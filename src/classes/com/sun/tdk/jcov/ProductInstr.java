/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.tdk.jcov.util.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author Andrey Titov
 */
public class ProductInstr extends Instr {

    static final Logger logger;

    static {
        Utils.initLogger();
        logger = Logger.getLogger(ProductInstr.class.getName());
    }

    public final static OptionDescr DSC_INSTRUMENT =
            new OptionDescr("product", "", OptionDescr.VAL_SINGLE, "");
    public final static OptionDescr DSC_INSTRUMENT_TO =
            new OptionDescr("productOutput", "", OptionDescr.VAL_SINGLE, "");
    public final static OptionDescr DSC_RT_TO = new OptionDescr("rtTo", "", OptionDescr.VAL_MULTI, "");

    private static final List<String> SKIP_INSTR_OPTIONS = List.of(
            DSC_OUTPUT).stream().map(o -> o.name).collect(Collectors.toList());
    private static final List<OptionDescr> ADD_TO_INSTR_OPTIONS = List.of(
            DSC_INSTRUMENT, DSC_INSTRUMENT_TO, Instr.DSC_INCLUDE_RT);

    @Override
    protected EnvHandler defineHandler() {
        EnvHandler superHandler = super.defineHandler();
        List<OptionDescr> opts = superHandler.getValidOptions().stream()
                .filter(o -> !SKIP_INSTR_OPTIONS.contains(o.name)).collect(Collectors.toList());
        opts = new ArrayList<>(opts);
        opts.addAll(ADD_TO_INSTR_OPTIONS);
        return new EnvHandler(opts.toArray(new OptionDescr[0]), this);
    }

    @Override
    protected int handleEnv(EnvHandler envHandler) throws EnvHandlingException {
        int superRes = super.handleEnv_(envHandler);
        if(superRes != SUCCESS_EXIT_CODE) return superRes;
        File instrProductDir = Utils.checkFileCanBeNull(envHandler.getValue(ProductInstr.DSC_INSTRUMENT),
                "product directory",
                Utils.CheckOptions.FILE_ISDIR, Utils.CheckOptions.FILE_CANREAD);
        File instrOutputDir = Utils.checkFileCanBeNull(envHandler.getValue(ProductInstr.DSC_INSTRUMENT_TO),
                "directory for instrumented product",
                Utils.CheckOptions.FILE_NOTEXISTS, Utils.CheckOptions.FILE_CANWRITE);
        if (instrProductDir != null && instrOutputDir == null) {
            throw new EnvHandlingException("Output directory for instrumented files should be specified in '-instrOutput'");
        }
        setSrcs(new String[] {instrProductDir.getAbsolutePath()});
        setOutDir(instrOutputDir);
        File rtLibFile = Utils.checkFileCanBeNull(envHandler.getValue(Instr.DSC_INCLUDE_RT), "JCov RT Saver path", Utils.CheckOptions.FILE_EXISTS, Utils.CheckOptions.FILE_ISFILE, Utils.CheckOptions.FILE_CANREAD);
        if (rtLibFile == null) {
            throw new EnvHandlingException("Please specify saver location with '-rt' option");
        }
        setInclude_rt(rtLibFile.toString());
        return SUCCESS_EXIT_CODE;
    }

    @Override
    protected String getDescr() {
        return "";
    }

    @Override
    protected String usageString() {
        return "";
    }

    @Override
    protected String exampleString() {
        return "";
    }

    public void instrumentProduct() throws Exception {
        if (!getOutDir().exists()) getOutDir().mkdirs();
        instrumentFiles(getSrcs(), getOutDir(), getInclude_rt());
        finishWork(getTemplate());
    }
}