/*
 * Copyright (c) 2022 Oracle and/or its affiliates. All rights reserved.
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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * TODO describe the lifecycle
 */
public interface InstrumentationPlugin {

    /**
     * An identifier for template artifact.
     */
    String TEMPLATE_ARTIFACT = "template.xml";

    void instrument(Collection<String> classes, Function<String, byte[]> loader, BiConsumer<String, byte[]> saver,
                    InstrumentationParams parameters) throws Exception;

    /**
     * Completes the instrumentation proccess and returns a map of instrumentation artifacts.
     * @see #TEMPLATE_ARTIFACT
     *
     * @return the artifact map. The artifacts are identifiable by a string. The artifacts are consumers of
     * OutputStream's.
     * @throws Exception
     */
    Map<String, Consumer<OutputStream>> complete() throws Exception;

    //TODO properly relocate the inner classes

    class FilteringPlugin implements InstrumentationPlugin {
        private final InstrumentationPlugin inner;
        private final Predicate<String> filter;

        public FilteringPlugin(InstrumentationPlugin inner, Predicate<String> filter) {
            this.inner = inner;
            this.filter = filter;
        }

        @Override
        public void instrument(Collection<String> classes, Function<String, byte[]> loader,
                               BiConsumer<String, byte[]> saver, InstrumentationParams parameters) throws Exception {
            inner.instrument(classes.stream().filter(filter).collect(Collectors.toList()),
                    loader, saver, parameters);
            classes.stream().filter(filter.negate()).forEach(c -> saver.accept(c, loader.apply(c)));
        }

        @Override
        public Map<String, Consumer<OutputStream>> complete() throws Exception {
            return inner.complete();
        }

//        @Override
//        public void complete(Supplier<OutputStream> templateStreamSupplier) throws Exception {
//            inner.complete(templateStreamSupplier);
//        }
    }

//    interface ModuleImplant {
//        //TODO qualified exports?
//        List<String> exports();
//        Collection<String> classes();
//        Function<String, byte[]> loader();
//    }
//
//    abstract class ModuleImplantingPlugin implements InstrumentationPlugin {
//
//        public static final String MODULE_INFO_CLASS = "module-info.class";
//
//        public interface ModuleInstrumentationPlugin extends InstrumentationPlugin {
//            String getModuleName(byte[] moduleInfo);
//            byte[] addExports(List<String> exports, byte[] moduleInfo);
//        }
//
//        private final ModuleInstrumentationPlugin inner;
//        private final Function<String, ModuleImplant> implants;
//
//        public ModuleImplantingPlugin(ModuleInstrumentationPlugin inner, Function<String, ModuleImplant> implants) {
//            this.inner = inner;
//            this.implants = implants;
//        }
//
//        @Override
//        public void instrument(Collection<String> classes, Function<String, byte[]> loader,
//                               BiConsumer<String, byte[]> saver, InstrumentationParams parameters) throws Exception {
//            inner.instrument(classes, loader, saver, parameters);
//            String moduleName = inner.getModuleName(loader.apply(MODULE_INFO_CLASS));
//            if(moduleName != null) {
//                ModuleImplant implant = implants.apply(moduleName);
//                if(implant != null) {
//                    saver.accept(MODULE_INFO_CLASS, loader.apply(MODULE_INFO_CLASS));
//                    for(String c : implant.classes()) saver.accept(c, implant.loader().apply(c));
//                }
//            }
//        }
//
//        @Override
//        public void complete(Supplier<OutputStream> template) throws Exception {
//            inner.complete(template);
//        }
//    }
//
//    class ImplantingPlugin implements InstrumentationPlugin {
//        private final Collection<String> implant;
//        private final Function<String, byte[]> implantLoader;
//        private final InstrumentationPlugin inner;
//
//        //TODO similar to ModuleImplantingPlugin have different implants for different locations somehow?
//        public ImplantingPlugin(InstrumentationPlugin inner,
//                                Collection<String> implant, Function<String, byte[]> loader) {
//            this.implant = implant;
//            this.implantLoader = loader;
//            this.inner = inner;
//        }
//
//        @Override
//        public void instrument(Collection<String> classes, Function<String, byte[]> loader,
//                               BiConsumer<String, byte[]> saver, InstrumentationParams parameters) throws Exception {
//            inner.instrument(classes, loader, saver, parameters);
//            implant.forEach(c -> saver.accept(c, implantLoader.apply(c)));
//        }
//
//        @Override
//        public void complete(Supplier<OutputStream> templateStreamSupplier) throws Exception {
//            inner.complete(templateStreamSupplier);
//        }
//
//        protected InstrumentationPlugin inner() {
//            return inner;
//        }
//    }
//
//    interface ClassHierarchyReader {
//        Collection<String> getClasses() throws Exception;
//        Function<String, byte[]> getLoader() throws Exception;
//    }
//
//    class ClassHierarchyFileSystemReader implements ClassHierarchyReader {
//
//        private final FileSystem fs;
//        private final Path root;
//
//        public ClassHierarchyFileSystemReader(FileSystem fs, Path root) {
//            this.fs = fs;
//            this.root = root;
//        }
//
//        public ClassHierarchyFileSystemReader(Path root) {
//            this(FileSystems.getDefault(), root);
//        }
//
//        public ClassHierarchyFileSystemReader(FileSystem fs) {
//            this(fs, fs.getRootDirectories().iterator().next());
//        }
//
//        @Override
//        public Collection<String> getClasses() throws IOException {
//            return Files.find(root, Integer.MAX_VALUE,
//                            (f, a) -> f.toString().endsWith(".class"))
//                    .map(f -> root.relativize(f))
//                    .map(Path::toString)
//                    .map(s -> s.substring(0, s.length() - ".class".length()))
//                    .collect(Collectors.toList());
//        }
//
//        @Override
//        public Function<String, byte[]> getLoader() {
//            return f -> {
//                try {
//                    return Files.readAllBytes(root.resolve(f + ".class"));
//                } catch (IOException e) {
//                    throw new UncheckedIOException(e);
//                }
//            };
//        }
//    }
//
//    class SingleClassReader implements ClassHierarchyReader {
//
//        private final Path source;
//
//        public SingleClassReader(Path source) {
//            if(!source.toString().endsWith("class")) throw new IllegalArgumentException("Must be a class file: " + source);
//            this.source = source;
//        }
//
//        @Override
//        public Collection<String> getClasses() throws IOException {
//            return List.of(source.toString().substring(0, source.toString().length() - ".class".length()));
//        }
//
//        @Override
//        public Function<String, byte[]> getLoader() {
//            return f -> {
//                try {
//                    return Files.readAllBytes(source);
//                } catch (IOException e) {
//                    throw new UncheckedIOException(e);
//                }
//            };
//        }
//    }
//
//    class ClassHierarchyFileSystemWriter implements BiConsumer<String, byte[]> {
//        private final FileSystem fs;
//        private final Path target;
//
//        public ClassHierarchyFileSystemWriter(FileSystem fs, Path target) {
//            this.fs = fs;
//            this.target = target;
//        }
//
//        public ClassHierarchyFileSystemWriter(Path target) {
//            this(FileSystems.getDefault(), target);
//        }
//
//        public ClassHierarchyFileSystemWriter(FileSystem fs) {
//            this(fs, fs.getRootDirectories().iterator().next());
//        }
//
//        @Override
//        public void accept(String s, byte[] bytes) {
//            try {
//                Files.write(target.resolve(s + ".class"), bytes);
//            } catch (IOException e) {
//                throw new UncheckedIOException(e);
//            }
//        }
//    }
//
//    //TODO inherit ClassHierarchyFileSystemWriter
//    class ClassHierarchyJarWriter implements BiConsumer<String, byte[]> {
//        private final Path path;
//
//        public ClassHierarchyJarWriter(String rt) throws IOException {
//            this(Paths.get(rt));
//        }
//
//        public ClassHierarchyJarWriter(Path path) throws IOException {
//            this.path = path;
//        }
//
//        @Override
//        public void accept(String s, byte[] bytes) {
//            try (FileSystem fs = FileSystems.newFileSystem(path, null)) {
//                try(OutputStream out = Files.newOutputStream(fs.getPath(s + ".class"))) {
//                    out.write(bytes);
//                }
//            } catch (IOException e) {
//                throw new UncheckedIOException(e);
//            }
//        }
//    }
//
//    class JarFileReader extends ClassHierarchyFileSystemReader {
//        public JarFileReader(String jar) throws IOException {
//            this(Paths.get(jar));
//        }
//        public JarFileReader(Path jar) throws IOException {
//            super(FileSystems.newFileSystem(jar, null));
//        }
//    }
//
}
