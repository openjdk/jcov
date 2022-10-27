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
import com.sun.tdk.jcov.instrument.InstrumentationOptions;
import com.sun.tdk.jcov.instrument.InstrumentationParams;
import com.sun.tdk.jcov.instrument.InstrumentationPlugin;
import com.sun.tdk.jcov.instrument.XmlContext;
import com.sun.tdk.jcov.runtime.FileSaver;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * SPI class which allows to do additional instrumentation, in addition to instrumentation performed by JCov by default.
 * @author Alexander (Shura) Ilin.
 */
public class ASMInstrumentationPlugin implements InstrumentationPlugin {

    private final DataRoot data = new DataRoot();
    //TODO prehaps support qualified exports
    private List<String> exports = new ArrayList<>();

    @Override
    public void instrument(Collection<String> classes, Function<String, byte[]> loader,
                           BiConsumer<String, byte[]> saver, InstrumentationParams parameters) {
        //TODO are paremeters used in serialization only?
        //for now we have to assume that the same parameters are used for every call
        data.setParams(parameters);
        ClassMorph morph = new ClassMorph(null, data, parameters);
        classes.forEach(cls -> {
            try {
                //TODO nulls
                byte[] instrumented = morph.morph(loader.apply(cls), null, null);
                //TODO shoul never be null
                if(instrumented != null) saver.accept(cls, instrumented);
            } catch (IOException e) {
                //todo should this even be thrown?
                throw new UncheckedIOException(e);
            }
        });
    }

    @Override
    public void complete(Supplier<OutputStream> templateStreamSupplier) throws IOException {
        try (XmlContext ctx = new XmlContext(templateStreamSupplier.get(), data.getParams())) {
            //TODO
            //ctx.setSkipNotCoveredClasses(agentdata);
            data.xmlGen(ctx);
        }
    }
}
