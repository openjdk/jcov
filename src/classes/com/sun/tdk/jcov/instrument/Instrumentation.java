/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.jcov.instrument;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A utility class to implement the most common case of using the InstrumentationPlaugin: with the files in filesystems.
 * Suitable to be used for any supported filesystem type.
 */
public class Instrumentation {
    private final InstrumentationPlugin plugin;

    public Instrumentation(InstrumentationPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Instruments classes with the specified plugin, reading from one location and saving to another. Resources which
     * are not classfiles are copied derectly. Content of the implant is also copied into the output.
     *
     * @param from
     * @param to
     * @param params
     * @param implant
     * @throws Exception
     */
    //TODO should implant really even be a part of this? easily could be added after this call
    public void instrument(Path from, Path to, InstrumentationParams params, Implant implant)
            throws Exception {
        List<Path> resources = Files.find(from, Integer.MAX_VALUE,
                        (f, a) -> Files.isRegularFile(f))
                .collect(Collectors.toList());
        plugin.instrument(resources.stream()
                            .filter(r -> isClass(r))
                        .map(r -> toClassName(from, r))
                        .collect(Collectors.toList()),
                c -> {
                    try {
                        return Files.readAllBytes(toResource(from, c));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                },
                (c, b) -> {
                    try {
                        Files.write(toResource(to, c), b);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                },
                params);
        resources.stream().filter(r -> !isClass(r)).forEach(p -> {
            try {
                Files.write(to.resolve(from.relativize(p)), Files.readAllBytes(p));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        if(implant != null) {
            for (String r : implant.resources()) Files.write(to.resolve(r), implant.read(r));
        }
    }

    public void complete(Path outputDir, Map<String, Path> artifactMap) throws Exception {
        Map<String, Consumer<OutputStream>> pluginArtifacts = plugin.complete();
        for (String a : pluginArtifacts.keySet()) {
            Path to = artifactMap.containsKey(a) ? artifactMap.get(a) : outputDir.resolve(a);
            pluginArtifacts.get(a).accept(Files.newOutputStream(to));
        }
    }

    private static boolean isClass(Path p) {
        return p.getFileName().toString().endsWith(".class");
    }

    static String toClassName(Path root, Path classFile) {
        String shortFileName = root.relativize(classFile).toString();
        return shortFileName.substring(0, shortFileName.length() - 6)//".class"
                .replace(root.getFileSystem().getSeparator(), ".");
    }

    static Path toResource(Path root, String className) {
        return root.resolve(className.replace(".", root.getFileSystem().getSeparator()) + ".class");
    }

    static public class PathImplant implements Implant {

        private final Path source;

        public PathImplant(Path source) {
            this.source = source;
        }

        public Path getSource() {
            return source;
        }

        protected boolean accept(Path path, BasicFileAttributes fa) {return true;}

        @Override
        public Collection<String> resources() throws IOException {
            return Files.find(source, Integer.MAX_VALUE, (p,a) -> Files.isRegularFile(p) && accept(p, a))
                    .map(p -> source.relativize(p).toString())
                    .collect(Collectors.toList());
        }

        @Override
        public byte[] read(String path) {
            return new byte[0];
        }
    }

    public interface Implant {
        //paths relative to the result root
        Collection<String> resources() throws Exception;
        byte[] read(String path) throws Exception;
    }

    public static class FileSystemImplant extends PathImplant implements AutoCloseable {
        private final FileSystem fs;
        public FileSystemImplant(FileSystem fs) throws IOException {
            super(fs.getPath("/"));
            this.fs = fs;
        }

        @Override
        public void close() throws Exception {
            fs.close();
        }
    }

    public static class JarImplant extends FileSystemImplant {

        public JarImplant(Path jar) throws IOException {
            super(FileSystems.newFileSystem(jar, null));
        }

        @Override
        protected boolean accept(Path path, BasicFileAttributes fa) {
            return !path.toString().startsWith("/META-INF");
        }
    }
}
