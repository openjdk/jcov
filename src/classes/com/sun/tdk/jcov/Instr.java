/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.tdk.jcov.instrument.InstrumentationOptions.MERGE;
import com.sun.tdk.jcov.instrument.InstrumentationParams;
import com.sun.tdk.jcov.tools.EnvHandler;
import com.sun.tdk.jcov.tools.JCovCMDTool;
import com.sun.tdk.jcov.tools.LoggingFormatter;
import com.sun.tdk.jcov.tools.OptionDescr;
import com.sun.tdk.jcov.util.Utils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p> A tool to statically instrument class files to collect coverage. </p> <p>
 * There are 2 coverage collection modes: static and dynamic. In static mode
 * JCov reads and modifies classes bytecode inserting there some instructions
 * which will use JCov RT libraries. In dynamic mode (aka Agent mode) a VM agent
 * is used ("java -javaagent") that instruments bytecode just at loadtime. </p>
 *
 * @author Andrey Titov
 * @author Dmitry Fazunenko
 * @author Alexey Fedorchenko
 */
public class Instr extends JCovCMDTool {

    private String[] include = new String[]{".*"};
    private String[] exclude = new String[]{""};
    private String[] m_include = new String[]{".*"};
    private String[] m_exclude = new String[]{""};
    private String[] callerInclude;
    private String[] callerExclude;
    private String[] innerInclude;
    private String[] innerExclude;
    private String[] save_beg = null;
    private String[] save_end = null;
    private boolean genabstract = false;
    private boolean gennative = false;
    private boolean genfield = false;
    private boolean gensynthetic = true;
    private boolean genanonymous = true;
    private boolean innerinvocations = true;
    private String[] srcs;
    private File outDir;
    private String include_rt;
    private String template;
    private String flushPath;
    private boolean subsequentInstr = false;
    private boolean recurse;
    private InstrumentationMode mode = InstrumentationMode.BRANCH;
    private AbstractUniversalInstrumenter instrumenter;
    private ClassMorph morph;
    private ClassLoader cl = null;
    private static final Logger logger;

    static {
        Utils.initLogger();
        logger = Logger.getLogger(Instr.class.getName());
    }

    /**
     * <p> Instrument specified files (directories with classfiles and jars) and
     * write template. Output template will be merged with input template (if
     * any) or will be written to specified path. Instrumented file will be
     * written to
     * <code>outDir</code>. </p>
     *
     * @param files Files to be instrumented. Can contain both directoried with
     * classfiles and jars
     * @param outDir Directory to write instrumented data to. Be careful - all
     * instrumented data will be written to the root of outDir. When null -
     * instrumented data will overwrite source binaries
     * @param includeRTJar Runtime jar path to be implanted into instrumented
     * data. For jar files - rt will be integrated into result jar. For
     * directories - rt will be unpacked into outDir. Null if nothing should be
     * implanted
     * @throws IOException
     * @see #setTemplate(java.lang.String)
     */
    public void instrumentAll(File[] files, File outDir, String includeRTJar) throws IOException {
        instrumentFiles(files, outDir, includeRTJar);
        instrumenter.finishWork();
    }

    /**
     * <p> Instrument several files (classfiles or jars) to some directory.
     * Instrumenter should be set. Instrumented file will be written to
     * <code>outDir</code>. </p>
     *
     * @param files files to instrument
     * @param outDir can be null. Initial file will be overwritten in such case.
     * @throws IOException
     * @see
     * #setInstrumenter(com.sun.tdk.jcov.insert.AbstractUniversalInstrumenter)
     * @see #setDefaultInstrumenter()
     */
    public void instrumentAll(File[] files, File outDir) throws IOException {
        instrumentAll(files, outDir, null);
    }

    /**
     * <p> Instrument one file using default instrumenter. This method doesn't
     * write output template.xml - use
     * <code>finishWork()</code> method. Instrumented file will be written to
     * <code>outDir</code>. </p>
     *
     * @param file
     * @param outDir can be null. Initial file will be overwritten in such case.
     * @param includeRTJar
     * @throws IOException
     * @see
     * #setInstrumenter(com.sun.tdk.jcov.insert.AbstractUniversalInstrumenter)
     * @see #setDefaultInstrumenter()
     * @see #finishWork()
     */
    public void instrumentFile(File file, File outDir, String includeRTJar) throws IOException {
        setDefaultInstrumenter();
        instrumenter.instrument(file, outDir, includeRTJar, recurse);
    }

    /**
     * <p> Instrument one file using default instrumenter. This method doesn't
     * write output template.xml - use
     * <code>finishWork()</code> method. Instrumented file will be written to
     * <code>outDir</code>. </p>
     *
     * @param file
     * @param outDir can be null. Initial file will be overwritten in such case.
     * @param includeRTJar
     * @throws IOException
     * @see
     * #setInstrumenter(com.sun.tdk.jcov.insert.AbstractUniversalInstrumenter)
     * @see #setDefaultInstrumenter()
     * @see #finishWork()
     */
    public void instrumentFile(String file, File outDir, String includeRTJar) throws IOException {
        instrumentFile(new File(file), outDir, includeRTJar);
    }

    public void instrumentFile(String file, File outDir, String includeRTJar, String moduleName) throws IOException {
        if (morph != null){
            morph.setCurrentModuleName(moduleName);
            instrumentFile(new File(file), outDir, includeRTJar);
        }
    }

    /**
     * <p> This method instruments a bunch of files using default instrumenter.
     * This method doesn't write output template.xml - use
     * <code>finishWork()</code> method. Instrumented file will be written to
     * <code>outDir</code>. </p>
     *
     * @param files
     * @param outDir can be null. Initial file will be overwritten in such case.
     * @param implantRT
     */
    public void instrumentFiles(File[] files, File outDir, String implantRT) throws IOException {
        setDefaultInstrumenter();
        for (File file : files) {
            instrumenter.instrument(file, outDir, implantRT, recurse);
        }
    }

    /**
     * <p> This method instruments a bunch of files using default instrumenter.
     * This method doesn't write output template.xml - use
     * <code>finishWork()</code> method. Instrumented file will be written to
     * <code>outDir</code>. </p>
     *
     * @param files
     * @param outDir can be null. Initial file will be overwritten in such case.
     * @param implantRT
     */
    public void instrumentFiles(String[] files, File outDir, String implantRT) throws IOException {
        setDefaultInstrumenter();
        for (String file : files) {
            instrumenter.instrument(new File(file), outDir, implantRT, recurse);
        }
    }

    public void instrumentTests(String[] files, File outDir, String implantRT) throws IOException {

        if (gennative || genabstract) {
            morph.fillIntrMethodsIDs(morph.getRoot());
        }

        setDefaultInstrumenter();
        for (String file : files) {
            instrumenter.instrument(new File(file), outDir, implantRT, recurse);
        }
    }

    /**
     * Begin instrumentation in semi-automatic mode
     *
     * @see Instr#finishWork()
     */
    public void startWorking() {
        setDefaultInstrumenter();
    }

    /**
     * Set default instrumenter
     */
    private void setDefaultInstrumenter() {

        if (morph == null) {
            InstrumentationParams params = new InstrumentationParams(innerinvocations, false, false, gennative, genfield, false, genabstract ? InstrumentationOptions.ABSTRACTMODE.DIRECT : InstrumentationOptions.ABSTRACTMODE.NONE, include, exclude, callerInclude, callerExclude, m_include, m_exclude, mode, save_beg, save_end)
                    .setInstrumentSynthetic(gensynthetic)
                    .setInstrumentAnonymous(genanonymous)
                    .setInnerInvocations(innerinvocations)
                    .setInnerIncludes(innerInclude)
                    .setInnerExcludes(innerExclude);
            if (subsequentInstr) {
                morph = new ClassMorph(params, template);
            } else {
                morph = new ClassMorph(params, null);
            }
        }
        if (instrumenter == null) {
            instrumenter = new AbstractUniversalInstrumenter(true) {
                protected byte[] instrument(byte[] classData, int classLen) throws IOException {
                    return morph.morph(classData, cl, flushPath);
                }

                public void finishWork() {
                    if (subsequentInstr) {
                        morph.saveData(MERGE.MERGE); // template should be initialized
                    } else {
                        morph.saveData(template, null, MERGE.OVERWRITE); // template should be initialized
                    }
                }

                public void processClassFileInModules(Path filePath, File outDir){
                    if (morph != null){
                        if (filePath != null){
                            String mpath = filePath.toAbsolutePath().toString();
                            mpath = mpath.substring("/modules/".length());
                            if (mpath.indexOf("/") != -1){
                                String module_name = mpath.substring(0, mpath.indexOf("/"));
                                morph.setCurrentModuleName(module_name);
                            }
                        }
                        super.processClassFileInModules(filePath, outDir);
                    }
                }

            };
        }
    }

    /**
     * Set instrumenter
     *
     * @param instrumenter instrumenter used to instrument data
     */
    public void setInstrumenter(AbstractUniversalInstrumenter instrumenter) {
        this.instrumenter = instrumenter;
    }

    /**
     * Finish instrumentation and write template. If template already exists -
     * it will be merged. <p> Template is written to the place Instrumenter was
     * created with
     */
    public void finishWork() {
        if (instrumenter != null) {
            instrumenter.finishWork();
            // destroy instrumenter & morph?
        }
    }

    /**
     * Finish instrumentation and write template. If template already exists -
     * it will be merged.
     *
     * @param outTemplate template path
     */
    public void finishWork(String outTemplate) {
        if (instrumenter != null) {
            if (subsequentInstr) {
                morph.saveData(outTemplate, MERGE.MERGE); // template should be initialized
            } else {
                morph.saveData(outTemplate, null, MERGE.OVERWRITE); // template should be initialized
            }
        }
    }

    /**
     * Legacy CLI entry point.
     */
    public static void main(String[] args) {
        Instr tool = new Instr();
        try {
            int res = tool.run(args);
            System.exit(res);
        } catch (Exception ex) {
            System.exit(1);
        }
    }


    protected String usageString() {
        return "java com.sun.tdk.jcov.Instr [-option value] source1 sourceN";
    }

    /**
     * @return Command note
     * @see com.sun.tdk.jcov.tools.JCovTool#noteString()
     */
    @Override
    protected String noteString() {
        return "  Note: Starting from JDK 9, the sources: source1,sourceN should be added to the class path of the command line.";
    }


    protected String exampleString() {
        return "java -cp jcov.jar:source1:source2 com.sun.tdk.jcov.Instr -include java.lang.* " +
                "-type block -output instrumented_classes source1 source2";
    }

    protected String getDescr() {
        return "instruments class files and creates template.xml";
    }

    /**
     * public configuration interface
     *
     * @param b true when logger should be verbose
     */
    public void setVerbose(boolean b) {
        if (b) {
            logger.setLevel(Level.INFO);
        } else {
            logger.setLevel(Level.WARNING);
        }
    }

    public void resetDefaults() {
        try {
            handleEnv_(defineHandler());
        } catch (EnvHandlingException ex) {
            // should not happen
        }
    }

    public boolean isGenAbstract() {
        return genabstract;
    }

    public void setGenAbstract(boolean abstact) {
        this.genabstract = abstact;
    }

    public String[] getExclude() {
        return exclude;
    }

    public void setExclude(String[] exclude) {
        this.exclude = exclude;
    }

    public String[] getMExclude() {
        return m_exclude;
    }

    public void setMExclude(String[] m_exclude) {
        this.m_exclude = m_exclude;
    }

    public boolean isGenField() {
        return genfield;
    }

    public void setGenField(boolean field) {
        this.genfield = field;
    }

    public boolean isGenNative() {
        return gennative;
    }

    public void setGenNative(boolean gennative) {
        this.gennative = gennative;
    }

    public String[] getInclude() {
        return include;
    }

    public void setInclude(String[] include) {
        this.include = include;
    }

    public String[] getMInclude() {
        return m_include;
    }

    public void setMInclude(String[] m_include) {
        this.m_include = m_include;
    }

    public void setCallerInclude(String[] callerInclude) {
        this.callerInclude = callerInclude;
    }

    public void setCallerExclude(String[] callerExclude) {
        this.callerExclude = callerExclude;
    }

    public void setInnerInclude(String[] include) {
        this.innerInclude = include;
    }

    public void setInnerExclude(String[] exclude) {
        this.innerExclude = exclude;
    }

    public void setFilter(String[] include, String exclude[]) {
        this.include = include;
        this.exclude = exclude;
    }

    public String[] getSave_beg() {
        return save_beg;
    }

    public void setSave_beg(String[] save_beg) {
        this.save_beg = save_beg;
    }

    public String[] getSave_end() {
        return save_end;
    }

    public void setSave_end(String[] save_end) {
        this.save_end = save_end;
    }

    public void setMode(InstrumentationMode mode) {
        this.mode = mode;
    }

    public InstrumentationMode getMode() {
        return mode;
    }

    public void config(boolean genabstract, boolean genfield, boolean gennative, String[] saveBegin, String saveEnd[]) {
        setGenNative(gennative);
        setGenAbstract(genabstract);
        setGenField(genfield);
        setSave_beg(save_beg);
        setSave_end(save_end);
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getTemplate() {
        return template;
    }

    public boolean isSubsequentInstr() {
        return subsequentInstr;
    }

    public void setSubsequentInstr(boolean subsequentInstr) {
        this.subsequentInstr = subsequentInstr;
    }

    public void setFlushPath(String flushPath) {
        this.flushPath = flushPath;
    }

    public String getFlushPath() {
        return flushPath;
    }

    public void setClassLoader(ClassLoader cl) {
        this.cl = cl;
    }

    @Override
    protected int run() throws Exception {
        Utils.addToClasspath(srcs);
        try {
            instrumentFiles(srcs, outDir, include_rt);
            finishWork(template);
        } catch (IOException ex) {
            LoggingFormatter.printStackTrace = true;
            throw new Exception("Critical exception: ", ex);
        }
        return SUCCESS_EXIT_CODE;
    }

    @Override
    protected EnvHandler defineHandler() {
        return new EnvHandler(new OptionDescr[]{
                DSC_OUTPUT,
                DSC_VERBOSE,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_TYPE,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_INCLUDE,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_INCLUDE_LIST,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_EXCLUDE,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_CALLER_INCLUDE,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_CALLER_EXCLUDE,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_EXCLUDE_LIST,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_MINCLUDE,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_MEXCLUDE,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_SAVE_BEGIN,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_SAVE_AT_END,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_TEMPLATE,
                DSC_SUBSEQUENT,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_ABSTRACT,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_NATIVE,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_FIELD,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_SYNTHETIC,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_ANONYM,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_INNERINVOCATION,
                ClassMorph.DSC_FLUSH_CLASSES,
                DSC_INCLUDE_RT,
                DSC_RECURSE,}, this);
    }

    private int handleEnv_(EnvHandler opts) throws EnvHandlingException {
        if (opts.isSet(DSC_VERBOSE)) {
            setVerbose(true);
        }

        outDir = null;
        if (opts.isSet(DSC_OUTPUT)) { // compatibility
            outDir = new File(opts.getValue(DSC_OUTPUT));
            Utils.checkFile(outDir, "output directory", Utils.CheckOptions.FILE_NOTISFILE);
            if (!outDir.exists()) {
                outDir.mkdirs();
                logger.log(Level.INFO, "The directory {0} was created.", outDir.getAbsolutePath());
            }
        }

        save_beg = opts.getValues(InstrumentationOptions.DSC_SAVE_BEGIN);
        save_end = opts.getValues(InstrumentationOptions.DSC_SAVE_AT_END);

        String abstractValue = opts.getValue(InstrumentationOptions.DSC_ABSTRACT);
        if (abstractValue.equals("off")) {
            genabstract = false;
        } else if (abstractValue.equals("on")) {
            genabstract = true;
        } else {
            throw new EnvHandlingException("'" + InstrumentationOptions.DSC_ABSTRACT.name + "' parameter value error: expected 'on' or 'off'; found: '" + abstractValue + "'");
        }

        String nativeValue = opts.getValue(InstrumentationOptions.DSC_NATIVE);
        if (nativeValue.equals("on")) {
            gennative = true;
        } else if (nativeValue.equals("off")) {
            gennative = false;
        } else {
            throw new EnvHandlingException("'" + InstrumentationOptions.DSC_NATIVE.name + "' parameter value error: expected 'on' or 'off'; found: '" + nativeValue + "'");
        }

        String fieldValue = opts.getValue(InstrumentationOptions.DSC_FIELD);
        if (fieldValue.equals("on")) {
            genfield = true;
        } else if (fieldValue.equals("off")) {
            genfield = false;
        } else {
            // can't happen - check is in EnvHandler
            throw new EnvHandlingException("'" + InstrumentationOptions.DSC_FIELD.name + "' parameter value error: expected 'on' or 'off'; found: '" + fieldValue + "'");
        }

        String anonym = opts.getValue(InstrumentationOptions.DSC_ANONYM);
        if (anonym.equals("on")) {
            genanonymous = true;
        } else { // off
            genanonymous = false;
        }

        String synthetic = opts.getValue(InstrumentationOptions.DSC_SYNTHETIC);
        if (synthetic.equals("on")) {
            gensynthetic = true;
        } else { // if (fieldValue.equals("off"))
            gensynthetic = false;
        }

        String innerInvocation = opts.getValue(InstrumentationOptions.DSC_INNERINVOCATION);
        if ("off".equals(innerInvocation)) {
            innerinvocations = false;
        } else {
            innerinvocations = true;
        }

        callerInclude = opts.getValues(InstrumentationOptions.DSC_CALLER_INCLUDE);
        callerExclude = opts.getValues(InstrumentationOptions.DSC_CALLER_EXCLUDE);

        recurse = opts.isSet(DSC_RECURSE);

        mode = InstrumentationOptions.InstrumentationMode.fromString(opts.getValue(InstrumentationOptions.DSC_TYPE));
        template = opts.getValue(InstrumentationOptions.DSC_TEMPLATE);
        Utils.checkFileNotNull(template, "template filename", Utils.CheckOptions.FILE_NOTISDIR, Utils.CheckOptions.FILE_PARENTEXISTS);

        subsequentInstr = opts.isSet(DSC_SUBSEQUENT);

        include = InstrumentationOptions.handleInclude(opts);
        exclude = InstrumentationOptions.handleExclude(opts);

        m_include = InstrumentationOptions.handleMInclude(opts);
        m_exclude = InstrumentationOptions.handleMExclude(opts);

        flushPath = opts.getValue(ClassMorph.DSC_FLUSH_CLASSES);
        if ("none".equals(flushPath)) {
            flushPath = null;
        }
        include_rt = opts.getValue(DSC_INCLUDE_RT);
        Utils.checkFileCanBeNull(include_rt, "JCovRT library jarfile", Utils.CheckOptions.FILE_EXISTS, Utils.CheckOptions.FILE_ISFILE, Utils.CheckOptions.FILE_CANREAD);

        return SUCCESS_EXIT_CODE;
    }

    @Override
    protected int handleEnv(EnvHandler opts) throws EnvHandlingException {
        srcs = opts.getTail();
        if (srcs == null || srcs.length == 0) {
            throw new EnvHandlingException("No sources specified");
        }

        return handleEnv_(opts);
    }
    final static OptionDescr DSC_OUTPUT =
            new OptionDescr("instr.output", new String[]{"output", "o"}, "Output directory for instrumented classes",
                    OptionDescr.VAL_SINGLE,
                    "Specifies output directory, default directory is current. Instr command could process different dirs and different jars: \n "
                            + "all classes from input dirs and all jars will be placed in output directory.");
    final static OptionDescr DSC_VERBOSE =
            new OptionDescr("verbose", "Verbose mode", "Enable verbose mode.");
    final static OptionDescr DSC_INCLUDE_RT =
            new OptionDescr("implantrt", new String[]{"rt"}, "Runtime management", OptionDescr.VAL_SINGLE, "Allows to implant needed for runtime files into instrumented data: -includert jcov_rt.jar");
    final static OptionDescr DSC_SUBSEQUENT =
            new OptionDescr("subsequent", "", OptionDescr.VAL_NONE, "Template would be used to decide what should not be instrumented - all existing in template would be treated as already instrumented");
    final static OptionDescr DSC_RECURSE =
            new OptionDescr("recursive", "", OptionDescr.VAL_NONE, "Recurse through specified directories instrumenting everything inside. With -flush option it will be able to instrument duplicate classes. ");
}
