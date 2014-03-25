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
import com.sun.tdk.jcov.instrument.InstrumentationOptions;
import com.sun.tdk.jcov.instrument.InstrumentationOptions.InstrumentationMode;
import com.sun.tdk.jcov.instrument.InstrumentationParams;
import com.sun.tdk.jcov.tools.EnvHandler;
import com.sun.tdk.jcov.tools.JCovCMDTool;
import com.sun.tdk.jcov.tools.OptionDescr;
import com.sun.tdk.jcov.util.Utils;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * <p> Template generation. </p> <p> JCov Template file should be used to merge
 * dynamic data and to save static data. Template could be created while
 * statically instrumenting classes or with TmplGen tool. </p>
 *
 * @author Andrey Titov
 */
public class TmplGen extends JCovCMDTool {

    private String[] files;
    private AbstractUniversalInstrumenter instrumenter;
    private String flushPath;
    private String template;
    private String[] include;
    private String[] exclude;
    private boolean instrumentAbstract = false;
    private boolean instrumentNative = true;
    private boolean instrumentField = false;
    private boolean instrumentAnonymous = true;
    private boolean instrumentSynthetic = true;
    private InstrumentationOptions.InstrumentationMode mode;

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

    protected String usageString() {
        return "java com.sun.tdk.jcov.TmplGen [options] filename";
    }

    protected String exampleString() {
        return "java -cp jcov.jar com.sun.tdk.jcov.TmplGen -include java.lang.* -type method -template template.xml classes";
    }

    protected String getDescr() {
        return "generates the jcov template.xml";
    }

    @Override
    protected int run() throws Exception {
        try {
            generateAndSave(files);
        } catch (IOException ioe) {
            throw new Exception("Unexpected error during instrumentation", ioe);
        }
        return SUCCESS_EXIT_CODE;
    }

    public void generateAndSave(String[] files) throws IOException {
        setDefaultInstrumenter();
        for (String root : files) {
            instrumenter.instrument(new File(root), null);
        }
        instrumenter.finishWork();
        instrumenter = null;
    }

    public void generateTemplate(String[] files) throws IOException {
        if (instrumenter == null) {
            setDefaultInstrumenter();
        }
        for (String root : files) {
            instrumenter.instrument(new File(root), null);
        }
    }

    public void finishWork() {
        if (instrumenter == null) {
            throw new IllegalStateException("Instrumenter is not ready");
        }
        instrumenter.finishWork();
        instrumenter = null;
    }

    public void setDefaultInstrumenter() {
        if (instrumenter == null) {
            instrumenter = new AbstractUniversalInstrumenter(true, true) {
                ClassMorph morph = new ClassMorph(
                        new InstrumentationParams(instrumentNative, instrumentField, instrumentAbstract, include, exclude, mode)
                        .setInstrumentAnonymous(instrumentAnonymous)
                        .setInstrumentSynthetic(instrumentSynthetic), template);

                protected byte[] instrument(byte[] classData, int classLen) throws IOException {
//                    byte[] res = Arrays.copyOf(classData, classLen);
                    byte[] res = new byte[classLen];
                    System.arraycopy(classData, 0, res, 0, classLen);
                    morph.morph(res, null, flushPath); // jdk1.5 support
                    return res;
                }

                public void finishWork() {
                    morph.saveData(template, InstrumentationOptions.MERGE.MERGE);
                }
            };
        }
    }

    @Override
    protected EnvHandler defineHandler() {
        return new EnvHandler(new OptionDescr[]{
                    //        DSC_OUTPUT,
                    DSC_VERBOSE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_TEMPLATE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_TYPE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_INCLUDE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_EXCLUDE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_INCLUDE_LIST,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_EXCLUDE_LIST,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_ABSTRACT,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_NATIVE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_FIELD,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_SYNTHETIC,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_ANONYM,
                    ClassMorph.DSC_FLUSH_CLASSES,}, this);
    }

    @Override
    protected int handleEnv(EnvHandler opts) throws EnvHandlingException {
        files = opts.getTail();
        if (files == null) {
            throw new EnvHandlingException("No input files specified");
        }

        if (opts.isSet(DSC_VERBOSE)) {
            Utils.setLoggingLevel(Level.INFO);
        }

        Utils.addToClasspath(files);

        flushPath = opts.getValue(ClassMorph.DSC_FLUSH_CLASSES);
        if ("none".equals(flushPath)) {
            flushPath = null;
        }
        String abstractValue = opts.getValue(InstrumentationOptions.DSC_ABSTRACT);
        if (abstractValue.equals("off")) {
            instrumentAbstract = false;
        } else if (abstractValue.equals("on")) {
            instrumentAbstract = true;
        } else {
            throw new EnvHandlingException("'" + InstrumentationOptions.DSC_ABSTRACT.name + "' parameter value error: expected 'on' or 'off'; found: '" + abstractValue + "'");
        }

        String nativeValue = opts.getValue(InstrumentationOptions.DSC_NATIVE);
        if (nativeValue.equals("on")) {
            instrumentNative = true;
        } else if (nativeValue.equals("off")) {
            instrumentNative = false;
        } else {
            throw new EnvHandlingException("'" + InstrumentationOptions.DSC_NATIVE.name + "' parameter value error: expected 'on' or 'off'; found: '" + nativeValue + "'");
        }

        String fieldValue = opts.getValue(InstrumentationOptions.DSC_FIELD);
        if (fieldValue.equals("on")) {
            instrumentField = true;
        } else if (fieldValue.equals("off")) {
            instrumentField = false;
        } else {
            throw new EnvHandlingException("'" + InstrumentationOptions.DSC_FIELD.name + "' parameter value error: expected 'on' or 'off'; found: '" + fieldValue + "'");
        }

        String anonym = opts.getValue(InstrumentationOptions.DSC_ANONYM);
        if (anonym.equals("on")) {
            instrumentAnonymous = true;
        } else { // off
            instrumentAnonymous = false;
        }

        String synth = opts.getValue(InstrumentationOptions.DSC_SYNTHETIC);
        if (synth.equals("on")) {
            instrumentSynthetic = true;
        } else { // off
            instrumentSynthetic = false;
        }

        mode = InstrumentationOptions.InstrumentationMode.fromString(opts.getValue(InstrumentationOptions.DSC_TYPE));
        template = opts.getValue(InstrumentationOptions.DSC_TEMPLATE);
        File tmpl = new File(template);
        if (tmpl.isDirectory()) {
            throw new EnvHandlingException("'" + template + "' is a directory while expected template filename");
        }
        if (tmpl.getParentFile() != null && !tmpl.getParentFile().exists()) {
            throw new EnvHandlingException("Template parent directory '" + tmpl.getParentFile() + "' doesn't exits");
        }

        include = InstrumentationOptions.handleInclude(opts);
        exclude = InstrumentationOptions.handleExclude(opts);

        com.sun.tdk.jcov.runtime.CollectDetect.enableInvokeCounts();

        return SUCCESS_EXIT_CODE;
    }

    public String[] getExclude() {
        return exclude;
    }

    public void setExclude(String[] exclude) {
        this.exclude = exclude;
    }

    public String getFlushPath() {
        return flushPath;
    }

    public void setFlushPath(String flushPath) {
        this.flushPath = flushPath;
    }

    public String[] getInclude() {
        return include;
    }

    public void setInclude(String[] include) {
        this.include = include;
    }

    public boolean isInstrumentAbstract() {
        return instrumentAbstract;
    }

    public void setInstrumentAbstract(boolean instrumentAbstract) {
        this.instrumentAbstract = instrumentAbstract;
    }

    public boolean isInstrumentField() {
        return instrumentField;
    }

    public void setInstrumentField(boolean instrumentField) {
        this.instrumentField = instrumentField;
    }

    public boolean isInstrumentNative() {
        return instrumentNative;
    }

    public void setInstrumentNative(boolean instrumentNative) {
        this.instrumentNative = instrumentNative;
    }

    public InstrumentationMode getMode() {
        return mode;
    }

    public void setMode(InstrumentationMode mode) {
        this.mode = mode;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    final static OptionDescr DSC_VERBOSE =
            new OptionDescr("verbose", "Verbosity", "Enables verbose mode.");
}