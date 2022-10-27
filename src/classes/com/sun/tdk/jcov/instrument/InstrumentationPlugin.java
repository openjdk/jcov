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

import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * TODO describe the lifecycle
 */
public interface InstrumentationPlugin {

    void instrument(Collection<String> classes, Function<String, byte[]> loader, BiConsumer<String, byte[]> saver,
                    InstrumentationParams parameters) throws Exception;

    void complete(Supplier<OutputStream> templateStreamSupplier) throws Exception;

    //TODO properly relocate the inner classes

    abstract class FilteringPlugin implements InstrumentationPlugin {
        private final InstrumentationPlugin inner;

        public FilteringPlugin(InstrumentationPlugin inner) {
            this.inner = inner;
        }

        protected abstract boolean filter(String cls);
        @Override
        public void instrument(Collection<String> classes, Function<String, byte[]> loader,
                               BiConsumer<String, byte[]> saver, InstrumentationParams parameters) throws Exception {
            inner.instrument(classes.stream().filter(this::filter).collect(Collectors.toList()),
                    loader, saver, parameters);
        }

        @Override
        public void complete(Supplier<OutputStream> templateStreamSupplier) throws Exception {
            inner.complete(templateStreamSupplier);
        }
    }

    interface ModuleImplant {
        //TODO qualified exports?
        List<String> exports();
        Collection<String> classes();
        Function<String, byte[]> loader();
    }

    abstract class ModuleImplantingPlugin implements InstrumentationPlugin {

        public static final String MODULE_INFO_CLASS = "module-info.class";

        public interface ModuleInstrumentationPlugin extends InstrumentationPlugin {
            String getModuleName(byte[] moduleInfo);
            byte[] addExports(List<String> exports, byte[] moduleInfo);
        }

        private final ModuleInstrumentationPlugin inner;
        private final Function<String, ModuleImplant> implants;

        public ModuleImplantingPlugin(ModuleInstrumentationPlugin inner, Function<String, ModuleImplant> implants) {
            this.inner = inner;
            this.implants = implants;
        }

        @Override
        public void instrument(Collection<String> classes, Function<String, byte[]> loader,
                               BiConsumer<String, byte[]> saver, InstrumentationParams parameters) throws Exception {
            inner.instrument(classes, loader, saver, parameters);
            String moduleName = inner.getModuleName(loader.apply(MODULE_INFO_CLASS));
            if(moduleName != null) {
                ModuleImplant implant = implants.apply(moduleName);
                if(implant != null) {
                    saver.accept(MODULE_INFO_CLASS, loader.apply(MODULE_INFO_CLASS));
                    for(String c : implant.classes()) saver.accept(c, implant.loader().apply(c));
                }
            }
        }

        @Override
        public void complete(Supplier<OutputStream> template) throws Exception {
            inner.complete(template);
        }
    }

    class ImplantingPlugin implements InstrumentationPlugin {
        private final Collection<String> implant;
        private final Function<String, byte[]> implantLoader;
        private final InstrumentationPlugin inner;

        //TODO similar to ModuleImplantingPlugin have different implants for different locations somehow?
        public ImplantingPlugin(InstrumentationPlugin inner,
                                Collection<String> implant, Function<String, byte[]> loader) {
            this.implant = implant;
            this.implantLoader = loader;
            this.inner = inner;
        }

        @Override
        public void instrument(Collection<String> classes, Function<String, byte[]> loader,
                               BiConsumer<String, byte[]> saver, InstrumentationParams parameters) throws Exception {
            inner.instrument(classes, loader, saver, parameters);
            implant.forEach(c -> saver.accept(c, implantLoader.apply(c)));
        }

        @Override
        public void complete(Supplier<OutputStream> templateStreamSupplier) throws Exception {
            inner.complete(templateStreamSupplier);
        }

        protected InstrumentationPlugin inner() {
            return inner;
        }
    }
}
