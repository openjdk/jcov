/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.jcov.instrument.asm;

import com.sun.tdk.jcov.instrument.DataRoot;
import com.sun.tdk.jcov.instrument.InstrumentationParams;
import com.sun.tdk.jcov.instrument.InstrumentationPlugin;
import com.sun.tdk.jcov.instrument.XmlContext;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ModuleVisitor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * SPI class which allows to do additional instrumentation, in addition to instrumentation performed by JCov by default.
 * @author Alexander (Shura) Ilin.
 */
public class ASMInstrumentationPlugin implements InstrumentationPlugin,
        InstrumentationPlugin.ModuleInstrumentationPlugin {

    private final DataRoot data = new DataRoot();
//    private String moduleName;

    @Override
    public void instrument(Collection<String> resources, ClassLoader loader,
                           BiConsumer<String, byte[]> saver, InstrumentationParams parameters) throws IOException {
        //TODO are paremeters used in serialization only?
        //for now we have to assume that the same parameters are used for every call
        data.setParams(parameters);
        URL miURL = loader.getResource(MODULE_INFO_CLASS);
        String moduleName = (miURL == null) ? null : getModuleName(miURL.openStream().readAllBytes());
        ClassMorph morph = new ClassMorph(null, data, parameters);
        morph.setCurrentModuleName(moduleName);
        for(String r : resources) {
            byte[] content = loader.getResourceAsStream(r).readAllBytes();
            if(isClass(r)) {
                byte[] instrumented = morph.morph(content, loader, null);
                //TODO should never be null
                if(instrumented != null) saver.accept(r, instrumented);
            } else saver.accept(r, content);
        }
        moduleName = null;
    }

    @Override
    public Map<String, Consumer<OutputStream>> complete() {
        return Map.of(TEMPLATE_ARTIFACT, out -> {
            try (XmlContext ctx = new XmlContext(out, data.getParams())) {
                //TODO
                //ctx.setSkipNotCoveredClasses(agentdata);
                data.xmlGen(ctx);
            } catch (FileNotFoundException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private String getModuleName(byte[] moduleInfo) {
        AtomicReference<String> moduleName = new AtomicReference<>(null);
        ClassReader cr = new ClassReader(moduleInfo);
        ClassWriter cw = new OverriddenClassWriter(cr, ClassWriter.COMPUTE_FRAMES, getClass().getClassLoader());
        cr.accept( new ClassVisitor(ASMUtils.ASM_API_VERSION, cw) {
            @Override
            public ModuleVisitor visitModule(String name, int access, String version) {
                moduleName.set(name);
                return null;
            }
        }, 0);
        return moduleName.get();
    }

    @Override
    public byte[] addExports(List<String> exports, byte[] moduleInfo, ClassLoader loader) {
        return ClassMorph.addExports(moduleInfo, exports, loader);
    }

    @Override
    public byte[] clearHashes(byte[] moduleInfo, ClassLoader loader) {
        return ClassMorph.clearHashes(moduleInfo, loader);
    }

//    @Override
//    public void instrumentModuleInfo(ClassLoader loader, BiConsumer<String, byte[]> saver, List<String> expports,
//                                     boolean clearHashes, InstrumentationParams parameters) throws IOException {
//        byte[] mi = loader.getResourceAsStream(MODULE_INFO_CLASS).readAllBytes();
//        moduleName = getModuleName(mi);
//        if(expports != null && !expports.isEmpty()) mi = addExports(expports, mi, loader);
//        if(clearHashes) mi = clearHashes(mi, loader);
//        saver.accept(MODULE_INFO_CLASS, mi);
//    }
}
