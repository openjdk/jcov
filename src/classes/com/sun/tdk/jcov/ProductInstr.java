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

import com.sun.tdk.jcov.constants.MiscConstants;
import com.sun.tdk.jcov.insert.AbstractUniversalInstrumenter;
import com.sun.tdk.jcov.instrument.ClassMorph;
import com.sun.tdk.jcov.instrument.InstrumentationOptions;
import com.sun.tdk.jcov.instrument.InstrumentationParams;
import com.sun.tdk.jcov.tools.EnvHandler;
import com.sun.tdk.jcov.tools.JCovCMDTool;
import com.sun.tdk.jcov.tools.OptionDescr;
import com.sun.tdk.jcov.util.RuntimeUtils;
import com.sun.tdk.jcov.util.Utils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Andrey Titov
 */
public class ProductInstr extends JCovCMDTool {
    // instrumentation filters (include/exclude, fields, abstract, ...); removing temporary directory

    static final Logger logger;

    static {
        Utils.initLogger();
        logger = Logger.getLogger(ProductInstr.class.getName());
    }
    private File instrProductDir;
    private File instrOutputDir;
    private String template = MiscConstants.JcovTemplateFileNameXML;
    private String tempPath;
    private File rtLibFile;
    private String[] rtClassDirTargets;

    public void instrumentProduct() throws IOException {
        logger.log(Level.INFO, " - Instrumenting product");
        logger.log(Level.CONFIG, "Product location: ''{0}'', target location: ''{1}''", new Object[]{instrProductDir.getPath(), instrOutputDir.getPath()});

        tempPath = instrProductDir.getPath() + RuntimeUtils.genSuffix();
        createTempDir();

        AbstractUniversalInstrumenter instrumenter = setupInstrumenter();

        instrOutputDir.mkdir();
        instrumenter.instrument(instrProductDir, instrOutputDir, rtLibFile.getAbsolutePath(), rtClassDirTargets == null ? null : new ArrayList(Arrays.asList(rtClassDirTargets)), true);
        instrumenter.finishWork();

        removeTempDir();
    }

    private AbstractUniversalInstrumenter setupInstrumenter() {
        InstrumentationParams params = new InstrumentationParams(false, false, true, null, null, InstrumentationOptions.InstrumentationMode.BRANCH)
                .setInstrumentAnonymous(true)
                .setInstrumentSynthetic(false);

        final ClassMorph morph = new ClassMorph(params, null);
        return new AbstractUniversalInstrumenter(true) {
            @Override
            protected byte[] instrument(byte[] classData, int classLength) throws IOException {
                return morph.morph(classBuf, null, tempPath);
            }

            @Override
            public void finishWork() {
                morph.saveData(template, null, InstrumentationOptions.MERGE.OVERWRITE);
            }
        };
    }

    private void createTempDir() {
        new File(tempPath).mkdir();
        logger.log(Level.INFO, "Temp directory for storing instrumented classes created: ''{0}''. Automatic removal is not implemented yet so please remove it manually after all is done.", tempPath);
    }

    private void removeTempDir() {
        File tempFile = new File(tempPath);
        if (tempFile.isDirectory()) {
            Utils.deleteDirectory(tempFile);
        } else {
            tempFile.delete();
        }
        logger.log(Level.INFO, "Temp directory for storing instrumented classes deleted: ''{0}''.", tempPath);
    }

    @Override
    protected int run() throws Exception {
        instrumentProduct();
        return SUCCESS_EXIT_CODE;
    }

    @Override
    protected EnvHandler defineHandler() {
        return new EnvHandler(new OptionDescr[]{
                    DSC_INSTRUMENT,
                    DSC_INSTRUMENT_TO,
                    Instr.DSC_INCLUDE_RT,
                    DSC_RT_TO,}, this);
    }

    @Override
    protected int handleEnv(EnvHandler envHandler) throws EnvHandlingException {
        instrProductDir = Utils.checkFileCanBeNull(envHandler.getValue(ProductInstr.DSC_INSTRUMENT), "product directory", Utils.CheckOptions.FILE_ISDIR, Utils.CheckOptions.FILE_CANREAD);
        instrOutputDir = Utils.checkFileCanBeNull(envHandler.getValue(ProductInstr.DSC_INSTRUMENT_TO), "directory for instrumented product", Utils.CheckOptions.FILE_NOTEXISTS, Utils.CheckOptions.FILE_CANWRITE);
        if (instrProductDir != null && instrOutputDir == null) {
            throw new EnvHandlingException("Output directory for instrumented files should be specified in '-instrOutput'");
        }
        rtLibFile = Utils.checkFileCanBeNull(envHandler.getValue(Instr.DSC_INCLUDE_RT), "JCov RT Saver path", Utils.CheckOptions.FILE_EXISTS, Utils.CheckOptions.FILE_ISFILE, Utils.CheckOptions.FILE_CANREAD);
        if (rtLibFile == null) {
            throw new EnvHandlingException("Please specify saver location with '-rt' option");
        }
        rtClassDirTargets = envHandler.getValues(ProductInstr.DSC_RT_TO);
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
    public final static OptionDescr DSC_INSTRUMENT =
            new OptionDescr("product", "", OptionDescr.VAL_SINGLE, "");
    public final static OptionDescr DSC_INSTRUMENT_TO =
            new OptionDescr("productOutput", "", OptionDescr.VAL_SINGLE, "");
    public final static OptionDescr DSC_RT_TO = new OptionDescr("rtTo", "", OptionDescr.VAL_MULTI, "");
}