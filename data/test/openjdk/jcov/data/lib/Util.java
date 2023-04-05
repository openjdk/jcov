/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package openjdk.jcov.data.lib;

import openjdk.jcov.data.arguments.runtime.Collect;
import openjdk.jcov.data.instrument.Instrument;
import openjdk.jcov.data.instrument.Plugin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

import static java.io.File.separator;
import static openjdk.jcov.data.instrument.Instrument.PLUGIN_CLASS;
import static openjdk.jcov.data.runtime.CoverageData.COVERAGE_OUT;

public class Util {
    private final Path outputDir;

    public Util(Path dir) {
        outputDir = dir;
    }

    public List<Path> copyBytecode(String... classes) throws IOException {
        byte[] buf = new byte[1024];
        List<Path> result = new ArrayList<>();
        for(String c : classes) {
            String classFile = classFile(c);
            try(InputStream in = getClass().getClassLoader().getResourceAsStream(classFile)) {
                Path o = outputDir.resolve(classFile);
                result.add(o);
                if(!Files.exists(o.getParent())) Files.createDirectories(o.getParent());
                try(OutputStream out = Files.newOutputStream(o)) {
                    int read;
                    while((read = in.read(buf)) > 0)
                        out.write(buf, 0, read);
                }
            }
        };
        return result;
    }

    public static Path copyJRE(Path src) throws IOException {
        Path dest = Files.createTempDirectory("JDK");
        System.out.println("Copying a JDK from " + src + " to " + dest);
        Files.walk(src).forEach(s -> {
            try {
                Files.copy(s, dest.resolve(src.relativize(s)), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        return dest;
    }

    public static Path createRtJar(String prefix, Collection<Class> implant) throws IOException {
        Path dest = Files.createTempFile(prefix, ".jar");
        try(JarOutputStream jar = new JarOutputStream(Files.newOutputStream(dest))) {
            for (Class cls : implant) {
                String entryName = cls.getName().replace('.', '/') + ".class";
                jar.putNextEntry(new JarEntry(entryName));
                try (InputStream ci = cls.getClassLoader()
                        .getResourceAsStream(entryName)) {
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = ci.read(buffer)) > 0) {
                        jar.write(buffer, 0, read);
                    }
                }
            }
        }
        return dest;
    }

    public static String classFile(String className) {
        return className.replace('.', '/') + ".class";
    }

    public Class runClass(Class clss, String[] argv)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return runClass(clss.getName(), argv);
    }
    public Class runClass(String className, String[] argv)
            throws ClassNotFoundException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException, InstantiationException {
        Collect.clearData();
        ClassLoader ofOutputDir = new InstrumentedClassLoader();
        Class cls = ofOutputDir.loadClass(className);
        Method m = cls.getMethod("main", new String[0].getClass());
        System.out.println("invoking " + m);
        m.invoke(null, (Object)argv);
        return cls;
    }

    public static int runClassOnM(List<String> vmArgs, Class mainClass, List<String> params)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(System.getProperty("java.home") + separator + "bin" + separator + "java");
        command.addAll(vmArgs);
        command.addAll(addExports());
        command.add("-cp"); command.add(System.getProperty("java.class.path"));
        command.add("--module-path"); command.add(System.getProperty("jdk.module.path"));
        command.add("--module"); command.add("jcov.data/" + mainClass.getName());
        command.addAll(params);
        System.out.println("Running " + command.stream().collect(Collectors.joining(" ")));
        ProcessBuilder pb = new ProcessBuilder(command).inheritIO();
        return pb.start().waitFor();
    }

    public static int runAClassOnCP(List<String> vmArgs, List<Path> classPath, String mainClass,
                                    List<String> params)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(System.getProperty("java.home") + separator + "bin" + separator + "java");
        command.addAll(vmArgs);
        command.addAll(addExports());
        command.add("-cp"); command.add(classPath.stream().map(Path::toString).collect(Collectors.joining(":")));
        command.add(mainClass);
        command.addAll(params);
        System.out.println("Running " + command.stream().collect(Collectors.joining(" ")));
        ProcessBuilder pb = new ProcessBuilder(command).inheritIO();
        return pb.start().waitFor();
    }

    private static List<String> addExports() {
        return List.of(
                "--add-exports", "java.base/jdk.internal.classfile.instruction=jcov.data,ALL-UNNAMED",
                "--add-exports", "java.base/jdk.internal.classfile.constantpool=jcov.data,ALL-UNNAMED",
                "--add-exports", "java.base/jdk.internal.classfile=jcov.data,ALL-UNNAMED"
        );
    }

    private class InstrumentedClassLoader extends ClassLoader {
        protected InstrumentedClassLoader() {
            super(Util.class.getClassLoader());
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            Path classFile = outputDir.resolve(classFile(name));
            if(Files.exists(classFile)) {
                byte[] buf = new byte[1024];
                try(InputStream in = Files.newInputStream(classFile)) {
                    try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                        int read;
                        while((read = in.read(buf)) > 0)
                            out.write(buf, 0, read);
                        return defineClass(name, out.toByteArray(), 0, out.size());
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return super.loadClass(name);
        }
    }
    public static void rfrm(Path jre) throws IOException {
        System.out.println("Removing " + jre);
        if(Files.isRegularFile(jre))
            Files.deleteIfExists(jre);
        else
            Files.walkFileTree(jre, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
    }
}
