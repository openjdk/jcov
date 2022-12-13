/*
 * Copyright (c) 2014, 2022 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.jcov;

import com.sun.tdk.jcov.instrument.InstrumentationPlugin;
import com.sun.tdk.jcov.tools.EnvHandler;
import com.sun.tdk.jcov.tools.OptionDescr;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static com.sun.tdk.jcov.instrument.InstrumentationOptions.*;

/**
 * <p> Template generation. </p> <p> JCov Template file should be used to merge
 * dynamic data and to save static data. Template could be created while
 * statically instrumenting classes or with TmplGen tool. </p>
 *
 * @author Andrey Titov
 */
public class TmplGen extends Instr {

    /**
     * Legacy CMD line entry poiny (use 'java -jar jcov.jar TmplGen' from cmd
     * instead of 'java -cp jcov.jar com.sun.tdk.jcov.TmplGen')
     *
     * @param args
     */
    public static void main(String args[]) {
        TmplGen tool = new TmplGen();
        try {
            int res = tool.run(args);
            System.exit(res);
        } catch (Exception ex) {
            System.exit(1);
        }
    }

    @Override
    protected InstrumentationPlugin.Destination getDestination(File outDir, Path inPath) {
        return new InstrumentationPlugin.Destination() {
            @Override public BiConsumer<String, byte[]> saver() {return (n, c) -> {};}
            @Override public void close() {}
        };
    }

    protected String usageString() {
        return "java com.sun.tdk.jcov.TmplGen [options] filename";
    }

    protected String exampleString() {
        return "java -cp jcov.jar com.sun.tdk.jcov.TmplGen -include java.lang.* -type method -template template.xml classes";
    }

    protected String getDescr() {
        return "generates the jcov template.xml";
    }

    private static final List<String> SKIP_INSTR_OPTIONS = List.of(
            DSC_OUTPUT,
            DSC_CALLER_INCLUDE,
            DSC_CALLER_EXCLUDE,
            DSC_SAVE_BEGIN,
            DSC_SAVE_AT_END,
            DSC_FLUSH_CLASSES).stream().map(o -> o.name).collect(Collectors.toList());

    @Override
    protected EnvHandler defineHandler() {
        EnvHandler superHandler = super.defineHandler();
        List<OptionDescr> opts = superHandler.getValidOptions().stream()
                .filter(o -> !SKIP_INSTR_OPTIONS.contains(o.name)).collect(Collectors.toList());
        return new EnvHandler(opts.toArray(new OptionDescr[0]), this);
    }
}