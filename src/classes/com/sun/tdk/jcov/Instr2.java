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

import com.sun.tdk.jcov.insert.AbstractUniversalInstrumenter;
import com.sun.tdk.jcov.instrument.ClassMorph;
import com.sun.tdk.jcov.instrument.ClassMorph2;
import com.sun.tdk.jcov.instrument.InstrumentationOptions;
import com.sun.tdk.jcov.instrument.InstrumentationParams;
import com.sun.tdk.jcov.tools.EnvHandler;
import com.sun.tdk.jcov.tools.JCovCMDTool;
import com.sun.tdk.jcov.tools.OptionDescr;
import com.sun.tdk.jcov.util.Utils;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p> A tool to statically instrument classfiles to collect coverage. </p> <p>
 * There are 2 coverage collection modes: static and dynamic. In static mode
 * JCov reads and modifies classes bytecode inserting there some instructions
 * which will use JCov RT libraries. In dynamic mode (aka Agent mode) a VM agent
 * is used ("java -javaagent") that instruments bytecode just at loadtime. </p>
 *
 * @author Andrey Titov
 */
public class Instr2 extends JCovCMDTool {

    private boolean genabstract;
    private boolean genfield;
    private boolean gennative;
    private boolean gensynthetic;
    private boolean genanonymous;
    private String template;
    private String flushPath;
    private String[] include;
    private String[] exclude;
    private static final Logger logger;
    private String[] srcs;
    private File outDir;

    static {
        Utils.initLogger();
        logger = Logger.getLogger(Instr2.class.getName());
    }

    /**
     * entry point. Parses options, constructs appropriate context, class name
     * filter, then invoces UniversalInstrumenter to do the job
     */
    public static void main(String[] args) {
        Instr2 tool = new Instr2();
        try {
            int res = tool.run(args);
            System.exit(res);
        } catch (Exception ex) {
            System.exit(1);
        }
    }

    protected String usageString() {
        return "java com.sun.tdk.jcov.Instr2 [-option [value]]";
    }

    protected String exampleString() {
        return "java -cp jcov.jar com.sun.tdk.jcov.Instr2 -include java.lang.* -abstract on -native on -field on -template mytemplate.xml instrumented_classes";
    }

    protected String getDescr() {
        return "instrumenter designed for abstract, native methods and fields";
    }

    @Override
    protected int run() throws Exception {
        Utils.addToClasspath(srcs);

        AbstractUniversalInstrumenter instrumenter =
                new AbstractUniversalInstrumenter(true) {
                    ClassMorph2 morph = new ClassMorph2(
                            new InstrumentationParams(gennative, genfield, genabstract, include, exclude, InstrumentationOptions.InstrumentationMode.BLOCK)
                            .setInstrumentAnonymous(genanonymous)
                            .setInstrumentSynthetic(gensynthetic), template);

                    protected byte[] instrument(byte[] classData, int classLen) throws IOException {
                        return morph.morph(classData, flushPath);
                    }

                    public void finishWork() {
                    }
                };

        //instrumenter.setPrintStats(opts.isSet(DSC_STATS));
//        com.sun.tdk.jcov.instrument.Options.instrumentAbstract = com.sun.tdk.jcov.instrument.Options.instrumentAbstract.NONE;

        for (String root : srcs) {
            instrumenter.instrument(new File(root), outDir);
        }
        /*
         if ((opts.isSet(JcovInstrContext.OPT_SAVE_BEFORE) ||
         opts.isSet(JcovInstrContext.OPT_SAVE_AFTER)  ||
         opts.isSet(JcovInstrContext.OPT_SAVE_BEGIN)  ||
         opts.isSet(JcovInstrContext.OPT_SAVE_AT_END)) &&
         instrumenter.getSavePointCount() < 1) {

         log.warning("no coverage data savepoints have been inserted");
         }
         */
        instrumenter.finishWork();
        return SUCCESS_EXIT_CODE;
    }

    @Override
    protected EnvHandler defineHandler() {
        return new EnvHandler(new OptionDescr[]{
                    DSC_OUTPUT,
                    DSC_VERBOSE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_INCLUDE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_INCLUDE_LIST,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_EXCLUDE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_EXCLUDE_LIST,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_TEMPLATE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_ABSTRACT,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_NATIVE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_FIELD,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_SYNTHETIC,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_ANONYM,
                    ClassMorph.DSC_FLUSH_CLASSES
                }, this);
    }

    @Override
    protected int handleEnv(EnvHandler envHandler) throws EnvHandlingException {
        srcs = envHandler.getTail();
        if (srcs == null) {
            throw new EnvHandlingException("No input files specified");
        }
        if (envHandler.isSet(DSC_VERBOSE)) {
            logger.setLevel(Level.INFO);
        }

        outDir = null;
        if (envHandler.isSet(DSC_OUTPUT)) {
            outDir = new File(envHandler.getValue(DSC_OUTPUT));
            if (!outDir.exists()) {
                outDir.mkdirs();
                logger.log(Level.INFO, "The directory {0} was created.", outDir.getAbsolutePath());
            }
        }

        String abstractValue = envHandler.getValue(InstrumentationOptions.DSC_ABSTRACT);
        if (abstractValue.equals("off")) {
            genabstract = false;
        } else if (abstractValue.equals("on")) {
            genabstract = true;
        } else {
            throw new EnvHandlingException("'" + InstrumentationOptions.DSC_ABSTRACT.name + "' parameter value error: expected 'on' or 'off'; found: '" + abstractValue + "'");
        }

        String nativeValue = envHandler.getValue(InstrumentationOptions.DSC_NATIVE);
        if (nativeValue.equals("on")) {
            gennative = true;
        } else if (nativeValue.equals("off")) {
            gennative = false;
        } else {
            throw new EnvHandlingException("'" + InstrumentationOptions.DSC_NATIVE.name + "' parameter value error: expected 'on' or 'off'; found: '" + nativeValue + "'");
        }

        String fieldValue = envHandler.getValue(InstrumentationOptions.DSC_FIELD);
        if (fieldValue.equals("on")) {
            genfield = true;
        } else if (fieldValue.equals("off")) {
            genfield = false;
        } else {
            throw new EnvHandlingException("'" + InstrumentationOptions.DSC_FIELD.name + "' parameter value error: expected 'on' or 'off'; found: '" + fieldValue + "'");
        }

        String anonym = envHandler.getValue(InstrumentationOptions.DSC_ANONYM);
        if (anonym.equals("on")) {
            genanonymous = true;
        } else { // off
            genanonymous = false;
        }

        String syntheticField = envHandler.getValue(InstrumentationOptions.DSC_SYNTHETIC);
        if (syntheticField.equals("on")) {
            gensynthetic = true;
        } else { // if (fieldValue.equals("off"))
            gensynthetic = false;
        }

        template = envHandler.getValue(InstrumentationOptions.DSC_TEMPLATE);
        Utils.checkFileNotNull(template, "template filename", Utils.CheckOptions.FILE_NOTISDIR, Utils.CheckOptions.FILE_PARENTEXISTS);

        include = InstrumentationOptions.handleInclude(envHandler);
        exclude = InstrumentationOptions.handleExclude(envHandler);

        flushPath = envHandler.getValue(ClassMorph.DSC_FLUSH_CLASSES);
        if ("none".equals(flushPath)) {
            flushPath = null;
        }

        return SUCCESS_EXIT_CODE;
    }
    final static OptionDescr DSC_OUTPUT =
            new OptionDescr("instr2.output", new String[]{"output", "o"}, "Output directory", OptionDescr.VAL_SINGLE,
            "Specifies output file or directory, default directory is current.");
    final static OptionDescr DSC_VERBOSE =
            new OptionDescr("verbose", "Verbose mode", "Enable verbose mode.");
}
