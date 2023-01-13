/*
 * Copyright (c) 2023 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.jcov.instrument.plugin;

import com.sun.tdk.jcov.instrument.InstrumentationParams;
import com.sun.tdk.jcov.instrument.InstrumentationPlugin;
import com.sun.tdk.jcov.instrument.ModuleInstrumentationPlugin;

import java.io.InputStream;
import java.util.function.BiConsumer;

/**
 * Helps to instrument modules.
 *
 * @see #Instrumentation
 */
public class ModuleInstrumentation extends Instrumentation {
    private final ModuleInstrumentationPlugin modulePlugin;

    public ModuleInstrumentation(InstrumentationPlugin inner, ModuleInstrumentationPlugin modulePlugin) {
        super(inner);
        this.modulePlugin = modulePlugin;
    }

    public ModuleInstrumentationPlugin getModulePluign() {
        return modulePlugin;
    }

    /**
     * Take any required action needed to instrument a module. This implementation does not do anything.
     *
     * @param moduleInfo
     * @param loader
     * @param destination
     * @throws Exception
     * @see ModuleInstrumentationPlugin
     */
    protected void proccessModule(byte[] moduleInfo, ClassLoader loader, BiConsumer<String, byte[]> destination)
            throws Exception {
    }

    @Override
    public void instrument(Source source, Destination destination,
                           InstrumentationParams parameters) throws Exception {
        super.instrument(source, destination, parameters);
        try (InputStream in = source.loader().getResourceAsStream(InstrumentationPlugin.MODULE_INFO_CLASS)) {
            byte[] moduleInfo = in.readAllBytes();
            proccessModule(moduleInfo, source.loader(), destination.saver());
        }
    }
}
