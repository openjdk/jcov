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

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

/**
 * TODO describe the lifecycle
 */
public interface InstrumentationPlugin {

    /**
     * An identifier for template artifact.
     */
    String TEMPLATE_ARTIFACT = "template.xml";
    String CLASS_EXTENTION = ".class";
    String MODULE_INFO_CLASS = "module-info.class";

    /**
     *
     * @param resources A collection of resource paths relative to root of the class hierarchy. '/' is supposed to be
     *                  used as a file separator.
     * @param loader
     * @param saver
     * @param parameters
     * @throws Exception
     */
    void instrument(Collection<String> resources, ClassLoader loader,
                    BiConsumer<String, byte[]> saver,
                    InstrumentationParams parameters) throws Exception;

    void instrumentModuleInfo(ClassLoader loader, BiConsumer<String, byte[]> saver,
                              List<String> expports, boolean clearHashes,
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

    default boolean isClass(String resource) {return resource.endsWith(CLASS_EXTENTION) && !resource.endsWith(MODULE_INFO_CLASS);}

//    default <T extends InstrumentationPlugin> T cast(Class<T> cls) {
//        if (cls.isInstance(this)) return cls.cast(this);
//        else throw new RuntimeException(getClass().getName() + " is not a " + cls.getName());
//    }
    //TODO properly relocate the inner classes

    abstract class ProxyInstrumentationPlugin implements InstrumentationPlugin {
        private final InstrumentationPlugin inner;

        protected ProxyInstrumentationPlugin(InstrumentationPlugin inner) {
            this.inner = inner;
        }

        public InstrumentationPlugin getInner() {
            return inner;
        }

//        @Override
//        public <T extends InstrumentationPlugin> T cast(Class<T> cls) {
//            if (cls.isInstance(this)) return cls.cast(this);
//            else return inner.cast(cls);
//        }

        @Override
        public final Map<String, Consumer<OutputStream>> complete() throws Exception {
            return inner.complete();
        }
    }

    class FilteringPlugin extends ProxyInstrumentationPlugin {
        private final Predicate<String> filter;

        public FilteringPlugin(InstrumentationPlugin inner, Predicate<String> filter) {
            super(inner);
            this.filter = filter;
        }

        @Override
        public void instrument(Collection<String> resources, ClassLoader loader,
                               BiConsumer<String, byte[]> saver, InstrumentationParams parameters) throws Exception {
            getInner().instrument(resources.stream().filter(filter).collect(Collectors.toList()),
                    loader, saver, parameters);
            resources.stream().filter(filter.negate()).forEach(c -> {
                try {
                    saver.accept(c,
                            loader.getResourceAsStream(c).readAllBytes());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }

        @Override
        public void instrumentModuleInfo(ClassLoader loader, BiConsumer<String, byte[]> saver,
                                         List<String> expports, boolean clearHashes, InstrumentationParams parameters) throws Exception {
            getInner().instrumentModuleInfo(loader, saver, expports, clearHashes, parameters);
        }
    }

    interface Source extends Closeable {
        //paths relative to the result root
        Collection<String> resources() throws Exception;
        ClassLoader loader();
    }

    class ImplantingPlugin extends ProxyInstrumentationPlugin {
        private final Source source;

        //TODO similar to ModuleImplantingPlugin have different implants for different locations somehow?
        public ImplantingPlugin(InstrumentationPlugin inner, Source source) {
            super(inner);
            this.source = source;
        }

        @Override
        public void instrument(Collection<String> classes, ClassLoader loader,
                               BiConsumer<String, byte[]> saver, InstrumentationParams parameters) throws Exception {
            getInner().instrument(classes, loader, saver, parameters);
            for(String r : source.resources()) saver.accept(r, source.loader().getResourceAsStream(r).readAllBytes());
        }

        @Override
        public void instrumentModuleInfo(ClassLoader loader, BiConsumer<String, byte[]> saver,
                                         List<String> expports, boolean clearHashes, InstrumentationParams parameters) throws Exception {
            getInner().instrumentModuleInfo(loader, saver, expports, clearHashes, parameters);
        }
    }

    class PluginImpl {
        private final InstrumentationPlugin inner;

        public PluginImpl(InstrumentationPlugin inner) {
            this.inner = inner;
        }

        public final void instrument(Source source, BiConsumer<String, byte[]> destination,
                                     List<String> exports, boolean clearHashes,
                                     InstrumentationParams parameters) throws Exception {
            inner.instrumentModuleInfo(source.loader(), destination, exports, clearHashes, parameters);
            inner.instrument(source.resources(), source.loader(), destination, parameters);
        }
    }

    class PathSource implements Source, Closeable {

        private final ClassLoader loader;
        private final Path root;

        public PathSource(ClassLoader loader, Path root) {
            this.loader = loader;
            this.root = root;
        }
        public PathSource(Path root) throws MalformedURLException {
            this(new URLClassLoader(new URL[]{root.toUri().toURL()}), root);
        }

        @Override
        public Collection<String> resources() throws Exception {
            if(Files.isDirectory(root))
                return Files.find(root, Integer.MAX_VALUE, (f, a) -> Files.isRegularFile(f))
                        .map(r -> root.relativize(r).toString())
                        .collect(Collectors.toList());
            else
                try (JarFile jar = new JarFile(root.toFile())) {
                    return jar.stream().filter(f -> !f.isDirectory())
                            .map(ZipEntry::getName).collect(Collectors.toList());
                }
        }

        @Override
        public ClassLoader loader() {
            return loader;
        }

        @Override
        public void close() throws IOException {
            if (loader instanceof Closeable) ((Closeable) loader).close();
        }

        public boolean isModule() throws IOException {
            if (Files.isDirectory(root)) {
                return Files.exists(root.resolve(MODULE_INFO_CLASS));
            } else {
                try (JarFile jar = new JarFile(root.toFile())) {
                    return jar.stream().map(ZipEntry::getName).anyMatch(MODULE_INFO_CLASS::equals);
                }
            }
        }
    }
}
