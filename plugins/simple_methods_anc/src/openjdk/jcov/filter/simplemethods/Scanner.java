/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package openjdk.jcov.filter.simplemethods;

import com.sun.tdk.jcov.util.Utils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiPredicate;

import static java.util.stream.Collectors.joining;

public class Scanner {
    private static String USAGE =
            "java -classpath jcov.jar:SimpleMethods.jar " + Scanner.class.getName() + " --usage\n" +
                    "\n" +
                    "java -classpath jcov.jar:SimpleMethods.jar " + Scanner.class.getName() +
                    " [--include|-i <include patern>] [--exclude|-e <exclude pattern>] \\\n" +
                    "[--getters <output file name>] " +
                    "[--setters <output file name>] " +
                    "[--delegators <output file name>] " +
                    "[--throwers <output file name>] " +
                    "[--empty <output file name>] \\\n" +
                    "jrt:/ | jar:file:/<jar file> | file:/<class hierarchy>\n" +
                    "\n" +
                    "    Options\n" +
                    "        --include - what classes to scan for simple methods.\n" +
                    "        --exclude - what classes to exclude from scanning.\n" +
                    "    Next options specify file names where to collect this or that type of methods. " +
                    "Only those which specified are detected. At least one kind of methods should be requested. " +
                    "Please consult the source code for exact details.\n" +
                    "        --getters - methods which are just returning a value.\n" +
                    "        --setters - methods which are just setting a field.\n" +
                    "        --delegators - methods which are just calling another method.\n" +
                    "        --throwers - methods which are just throwing an exception.\n" +
                    "        --empty - methods with an empty body.\n" +
                    "\n" +
                    "    Parameters define where to look for classes which are to be scanned.\n" +
                    "        jrt:/ - scan JDK classes\n" +
                    "        jar:file:/ - scan a jar file\n" +
                    "        file:/ - scan a directory containing compiled classes.";

    private Utils.Pattern[] includes;
    private Utils.Pattern[] excludes;
    private final List<Filter> filters = new ArrayList<>();
    private final List<URI> filesystems = new ArrayList<>();

    public static void main(String[] args) throws IOException, URISyntaxException {
        if (args.length == 1 && args[0].equals("--usage")) {
            usage();
            return;
        }
        Scanner scanner = new Scanner();
        final List<Utils.Pattern> class_includes = new ArrayList<>();
        final List<Utils.Pattern> class_excludes = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--include":
                case "-i":
                    i++;
                    class_includes.add(new Utils.Pattern(args[i], true, false));
                    break;
                case "--exclude":
                case "-e":
                    i++;
                    class_excludes.add(new Utils.Pattern(args[i], false, false));
                    break;
                default:
                    //the only other options allowed are -<filter name>
                    //see usage
                    if (args[i].startsWith("--")) {
                        Filter filter = Filter.get(args[i].substring(2));
                        scanner.filters.add(filter);
                        i++;
                        filter.setOutputFile(args[i]);
                    } else {
                        try {
                            scanner.filesystems.add(new URI(args[i]));
                        } catch (URISyntaxException e) {
                            usage();
                            throw e;
                        }
                    }
            }
        }
        if (scanner.filters.size() == 0) {
            usage();
            String filtersList =
                    Arrays.stream(Filter.values()).map(f -> "--" + f.name()).collect(joining(","));
            throw new IllegalArgumentException("One or more of " + filtersList + " options must be specified");
        }
        scanner.includes = class_includes.toArray(new Utils.Pattern[0]);
        scanner.excludes = class_excludes.toArray(new Utils.Pattern[0]);
        scanner.run();
    }

    private static void usage() {
        System.out.println(USAGE);
    }

    public void run() throws IOException {
        try {
            for (Filter f : filters) {
                f.openFile();
            }
            for (URI uri : filesystems) {
                FileSystem fs;
                Iterator<Path> roots;
                String scheme = uri.getScheme();
                if(scheme == null) {
                    throw new IllegalStateException("No scheme in " + uri.toString());
                }
                switch (scheme) {
                    case "jrt":
                        fs = FileSystems.getFileSystem(uri);
                        roots = Files.newDirectoryStream(fs.getPath("./modules")).iterator();
                        break;
                    case "jar":
                        fs = FileSystems.newFileSystem(uri, new HashMap<>());
                        roots = fs.getRootDirectories().iterator();
                        break;
                    case "file":
                        fs = FileSystems.getDefault();
                        roots = List.of(fs.getPath(uri.getPath())).iterator();
                        break;
                    default:
                        throw new RuntimeException("TRI not supported: " + uri.toString());
                }
                while (roots.hasNext()) {
                    Path root = roots.next();
                    Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            if (file.toString().endsWith(".class")) {
                                visitClass(root, file);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
                }
            }
        } finally {
            for (Filter f : filters) {
                f.closeFile();
            }
        }
    }

    private void visitClass(Path root, Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            ClassReader reader;
            reader = new ClassReader(in);
            if (included(reader.getClassName())) {
                ClassNode clazz = new ClassNode();
                reader.accept(clazz, 0);
                for (Object methodObject : clazz.methods) {
                    MethodNode method = (MethodNode) methodObject;
                    for (Filter f : filters) {
                        if (f.filter.test(clazz, method)) {
                            f.add(clazz.name + "#" + method.name + method.desc);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Exception while parsing file " + file + " from " + root, e);
        }
    }

    private boolean included(String clazz) {
        return  Utils.accept(includes, null, "/" + clazz, null) &&
                Utils.accept(excludes, null, "/" + clazz, null);
    }

    enum Filter {
        getters("simple getter", new Getters()),
        setters("simple setter", new Setters()),
        delegators("simple delegator", new Delegators()),
        throwers("simple thrower", new Throwers()),
        empty("empty methods", new EmptyMethods());
        private String description;
        private BiPredicate<ClassNode, MethodNode> filter;
        private String outputFile;
        private BufferedWriter output;

        Filter(String description, BiPredicate<ClassNode, MethodNode> filter) {
            this.description = description;
            this.filter = filter;
        }

        public void setOutputFile(String outputFile) {
            this.outputFile = outputFile;
        }

        public void openFile() throws IOException {
            output = Files.newBufferedWriter(Paths.get(outputFile));
            output.write("#" + description);
            output.newLine();
            output.flush();
        }

        public void closeFile() throws IOException {
            if (outputFile != null) {
                output.flush();
                output.close();
            }
        }

        public void add(String s) throws IOException {
            output.write(s);
            output.newLine();
            output.flush();
        }

        static Filter get(String name) {
            for(Filter f : values()) {
                if(f.name().equals(name)) {
                    return f;
                }
            }
            throw new RuntimeException("Unknown filter: " + name);
        }
    }
}
