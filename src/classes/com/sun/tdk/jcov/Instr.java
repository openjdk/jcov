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

import com.sun.tdk.jcov.instrument.InstrumentationParams;
import com.sun.tdk.jcov.instrument.InstrumentationPlugin;
import com.sun.tdk.jcov.tools.EnvHandler;
import com.sun.tdk.jcov.tools.JCovCMDTool;
import com.sun.tdk.jcov.tools.OptionDescr;
import com.sun.tdk.jcov.util.Utils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.sun.tdk.jcov.instrument.InstrumentationOptions.*;
import static com.sun.tdk.jcov.instrument.InstrumentationPlugin.TEMPLATE_ARTIFACT;
import static com.sun.tdk.jcov.util.Utils.CheckOptions.*;

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
    private boolean genAbstract = false;
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
    private ClassLoader cl = ClassLoader.getSystemClassLoader();
    private static final Logger logger;
    //TODO do need both?
    private InstrumentationPlugin plugin;
    private InstrumentationParams params;

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
    public void instrumentAll(File[] files, File outDir, String includeRTJar) throws Exception {
        instrumentFiles(files, outDir, includeRTJar);
//        instrumenter.finishWork();
    }

    /**
     * <p> Instrument several files (classfiles or jars) to some directory.
     * Instrumenter should be set. Instrumented file will be written to
     * <code>outDir</code>. </p>
     *
     * @param files files to instrument
     * @param outDir can be null. Initial file will be overwritten in such case.
     * @throws IOException
     * @see #setup()
     */
    public void instrumentAll(File[] files, File outDir) throws Exception {
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
     * @see #setup()
     * @see #finishWork()
     */
    public void instrumentFile(File file, File outDir, String includeRTJar) throws Exception {
        instrumentFiles(new String[] {file.getAbsolutePath()}, outDir, includeRTJar);
        //instrumenter.instrument(file, outDir, includeRTJar, recurse);
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
     * @see #setup()
     * @see #finishWork()
     */
    public void instrumentFile(String file, File outDir, String includeRTJar) throws Exception {
        instrumentFile(new File(file), outDir, includeRTJar);
    }

    @Deprecated
    public void instrumentFile(String file, File outDir, String includeRTJar, String moduleName) throws Exception {
        instrumentFiles(new String[] {file}, outDir, includeRTJar);
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
    public void instrumentFiles(File[] files, File outDir, String implantRT) throws Exception {
        instrumentFiles(Stream.of(files).map(File::toString).collect(Collectors.toList()).toArray(new String[0]),
                outDir, implantRT);
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
    public void instrumentFiles(String[] files, File outDir, String implantRT) throws Exception {
        setup();
        if (plugin == null) plugin = InstrumentationPlugin.getPlugin();
        InstrumentationPlugin aPlugin = plugin;
        InstrumentationPlugin.Source source;
        if (implantRT != null) {
            source = new InstrumentationPlugin.PathSource(ClassLoader.getSystemClassLoader(), Path.of(implantRT));
            aPlugin = new InstrumentationPlugin.ImplantingPlugin(plugin, source);
        }
        aPlugin = new InstrumentationPlugin.FilteringPlugin(aPlugin, InstrumentationPlugin.classNameFilter(params));

        InstrumentationPlugin.Instrumentation fi =
                new InstrumentationPlugin.Instrumentation(aPlugin);
        for (String file : files) {
            InstrumentationPlugin.PathSource in;
//            FileSystem outFS = null;
            Path inPath = Path.of(file);
            if (Files.isDirectory(inPath) || file.endsWith(".jar") || file.endsWith(".jmod")) {
                in = new InstrumentationPlugin.PathSource(cl, inPath);
            } else if (Files.isRegularFile(inPath) && file.endsWith(".class")) {
                //TODO implement by directly calling the plugin
                //TODO deprecate in documentation: instead of providing specific files, ask the user to provide
                //a class hierarchy root and filters
                throw new RuntimeException();
            } else throw new IllegalStateException("Unknown input kind: " + file);
            InstrumentationPlugin.Destination out;
            out = getDestination(outDir, inPath);
            try (in) {
                fi.instrument(in, out, params);
            } finally {
                in.close();
                out.close();
            }
        }
    }

    protected InstrumentationPlugin.Destination getDestination(File outDir, Path inPath) throws IOException {
        InstrumentationPlugin.Destination out;
        Path outPath = (outDir != null) ? outDir.toPath().resolve(inPath.getFileName()) : inPath;
        if (Files.isDirectory(outPath) ||
                outPath.toString().endsWith(".jar") ||
                outPath.toString().endsWith(".jmod")) {
            out = new InstrumentationPlugin.PathDestination(outPath);
        } else if (Files.isRegularFile(outPath) && outPath.toString().endsWith(".class")) {
            //TODO as above
            throw new RuntimeException();
        } else throw new IllegalStateException("Unknown output kind: " + inPath);
        return out;
    }

//    See comments in JREInstr.handleEnv(EventHandler)
//    public void instrumentTests(String[] files, File outDir, String implantRT) throws IOException {
//
//        if (gennative || genAbstract) {
//            morph.fillIntrMethodsIDs(morph.getRoot());
//        }
//
//        setup();
//        for (String file : files) {
//            instrumenter.instrument(new File(file), outDir, implantRT, recurse);
//        }
//    }

    /**
     * Begin instrumentation in semi-automatic mode
     *
     * @see Instr#finishWork()
     */
    public void startWorking() {
        setup();
    }

    /**
     * Set default instrumenter
     */
    private void setup() {
        if(params == null) {
            params = new InstrumentationParams(innerinvocations,
                    false,
                    false,
                    gennative,
                    genfield,
                    false,
                    genAbstract ? ABSTRACTMODE.DIRECT : ABSTRACTMODE.NONE,
                    include,
                    exclude,
                    callerInclude,
                    callerExclude,
                    m_include,
                    m_exclude,
                    mode,
                    save_beg,
                    save_end)
                    .setInstrumentSynthetic(gensynthetic)
                    .setInstrumentAnonymous(genanonymous)
                    .setInnerInvocations(innerinvocations)
                    .setInnerIncludes(innerInclude)
                    .setInnerExcludes(innerExclude)
                    .setInstrumentationPlugin(plugin);
        }
    }

    /**
     * Finish instrumentation and write template. If template already exists -
     * it will be merged. <p> Template is written to the place Instrumenter was
     * created with
     */
    public void finishWork() throws Exception {
        plugin.complete().get(TEMPLATE_ARTIFACT).accept(Files.newOutputStream(Path.of(template)));
    }

    /**
     * Finish instrumentation and write template. If template already exists -
     * it will be merged.
     *
     * @param outTemplate template path
     */
    public void finishWork(String outTemplate) throws Exception {
        Consumer<OutputStream> tmplGen = plugin.complete().get(TEMPLATE_ARTIFACT);
        if (tmplGen != null) tmplGen.accept(Files.newOutputStream(Path.of(outTemplate)));
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
        return "java -cp jcov.jar:source1:sourceN com.sun.tdk.jcov.Instr [-option value] source1 sourceN";
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
        return "java -cp jcov.jar:source1:sourceN com.sun.tdk.jcov.Instr -include java.lang.* " +
                "-type block -output instrumented_classes source1 sourceN";
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
        return genAbstract;
    }

    public void setGenAbstract(boolean genAbstract) {
        this.genAbstract = genAbstract;
    }

    public InstrumentationParams getParams() { return params; }

    public String[] getExclude() { return exclude; }

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

    public String[] getInclude() {
        return include;
    }

    public String[] getMInclude() {return m_include;}

    public File getOutDir() { return outDir; }

    public String getInclude_rt() { return include_rt; }

    public void setInclude_rt(String include_rt) { this.include_rt = include_rt; }

    public void setOutDir(File outDir) { this.outDir = outDir; }

    public void setGenNative(boolean gennative) {
        this.gennative = gennative;
    }

    public void setInclude(String[] include) {
        this.include = include;
    }

    public void setMInclude(String[] m_include) {this.m_include = m_include;}

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

    public void setPlugin(InstrumentationPlugin plugin) {
        this.plugin = plugin;
    }

    public InstrumentationPlugin getPlugin() {
        return plugin;
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

    public String[] getSrcs() { return srcs; }

    public void setSrcs(String[] srcs) { this.srcs = srcs; }

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
            throw new EnvHandlingException("Critical exception: " + ex);
        }
        return SUCCESS_EXIT_CODE;
    }

    @Override
    protected EnvHandler defineHandler() {
        return new EnvHandler(new OptionDescr[]{
                DSC_OUTPUT,
                DSC_VERBOSE,
                DSC_TYPE,
                DSC_INCLUDE,
                DSC_INCLUDE_LIST,
                DSC_EXCLUDE,
                DSC_CALLER_INCLUDE,
                DSC_CALLER_EXCLUDE,
                DSC_EXCLUDE_LIST,
                DSC_MINCLUDE,
                DSC_MEXCLUDE,
                DSC_SAVE_BEGIN,
                DSC_SAVE_AT_END,
                DSC_TEMPLATE,
                DSC_SUBSEQUENT,
                DSC_ABSTRACT,
                DSC_NATIVE,
                DSC_FIELD,
                DSC_SYNTHETIC,
                DSC_ANONYM,
                DSC_INNERINVOCATION,
                DSC_INSTR_PLUGIN,
                DSC_FLUSH_CLASSES,
                DSC_INCLUDE_RT,
                DSC_RECURSE,}, this);
    }

    protected int handleEnv_(EnvHandler opts) throws EnvHandlingException {
        if (opts.isSet(DSC_VERBOSE)) {
            setVerbose(true);
        }

        outDir = null;
        if (opts.isSet(DSC_OUTPUT)) { // compatibility
            outDir = new File(opts.getValue(DSC_OUTPUT));
            Utils.checkFile(outDir, "output directory", FILE_NOTISFILE);
            if (!outDir.exists()) {
                outDir.mkdirs();
                logger.log(Level.INFO, "The directory {0} was created.", outDir.getAbsolutePath());
            }
        }

        save_beg = opts.getValues(DSC_SAVE_BEGIN);
        save_end = opts.getValues(DSC_SAVE_AT_END);

        String abstractValue = opts.getValue(DSC_ABSTRACT);
        if (abstractValue.equals("off")) {
            genAbstract = false;
        } else if (abstractValue.equals("on")) {
            genAbstract = true;
        } else {
            throw new EnvHandlingException("'" + DSC_ABSTRACT.name +
                    "' parameter value error: expected 'on' or 'off'; found: '" + abstractValue + "'");
        }

        String nativeValue = opts.getValue(DSC_NATIVE);
        if (nativeValue.equals("on")) {
            gennative = true;
        } else if (nativeValue.equals("off")) {
            gennative = false;
        } else {
            throw new EnvHandlingException("'" + DSC_NATIVE.name +
                    "' parameter value error: expected 'on' or 'off'; found: '" + nativeValue + "'");
        }

        String fieldValue = opts.getValue(DSC_FIELD);
        if (fieldValue.equals("on")) {
            genfield = true;
        } else if (fieldValue.equals("off")) {
            genfield = false;
        } else {
            // can't happen - check is in EnvHandler
            throw new EnvHandlingException("'" + DSC_FIELD.name +
                    "' parameter value error: expected 'on' or 'off'; found: '" + fieldValue + "'");
        }

        String anonym = opts.getValue(DSC_ANONYM);
        genanonymous = anonym.equals("on");

        String synthetic = opts.getValue(DSC_SYNTHETIC);
        gensynthetic = synthetic.equals("on");

        String innerInvocation = opts.getValue(DSC_INNERINVOCATION);
        innerinvocations = ! "off".equals(innerInvocation);

        callerInclude = opts.getValues(DSC_CALLER_INCLUDE);
        callerExclude = opts.getValues(DSC_CALLER_EXCLUDE);

        recurse = opts.isSet(DSC_RECURSE);

        mode = InstrumentationMode.fromString(opts.getValue(DSC_TYPE));
        template = opts.getValue(DSC_TEMPLATE);
        Utils.checkFileNotNull(template, "template filename", FILE_NOTISDIR, FILE_PARENTEXISTS);

        subsequentInstr = opts.isSet(DSC_SUBSEQUENT);

        include = handleInclude(opts);
        exclude = handleExclude(opts);

        m_include = handleMInclude(opts);
        m_exclude = handleMExclude(opts);

        flushPath = opts.getValue(DSC_FLUSH_CLASSES);
        if ("none".equals(flushPath)) {
            flushPath = null;
        }
        include_rt = opts.getValue(DSC_INCLUDE_RT);
        Utils.checkFileCanBeNull(include_rt, "JCovRT library jarfile", FILE_EXISTS, FILE_ISFILE, FILE_CANREAD);

        try {
            String pluginClass = opts.getValue(DSC_INSTR_PLUGIN);
            if(pluginClass != null && !pluginClass.isEmpty()) {
                plugin = (InstrumentationPlugin) Class.forName(pluginClass)
                        .getDeclaredConstructor().newInstance();
            }
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException |
                NoSuchMethodException | InvocationTargetException e) {
            throw new EnvHandlingException("'" + DSC_INSTR_PLUGIN.name + "' parameter error: '" + e + "'");
        }

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
                    "Specifies output directory, default directory is current. " +
                            "Instr command could process different dirs and different jars: \n " +
                            "all classes from input dirs and all jars will be placed in output directory.");
    final static OptionDescr DSC_VERBOSE =
            new OptionDescr("verbose", "Verbose mode", "Enable verbose mode.");
    final static OptionDescr DSC_INCLUDE_RT =
            new OptionDescr("implantrt", new String[]{"rt"}, "Runtime management", OptionDescr.VAL_SINGLE,
                    "Allows to implant needed for runtime files into instrumented data: -includert jcov_rt.jar");
    final static OptionDescr DSC_SUBSEQUENT =
            new OptionDescr("subsequent", "", OptionDescr.VAL_NONE,
                    "Template would be used to decide what should not be instrumented - " +
                            "all existing in template would be treated as already instrumented");
    final static OptionDescr DSC_RECURSE =
            new OptionDescr("recursive", "", OptionDescr.VAL_NONE,
                    "Recurse through specified directories instrumenting everything inside. " +
                            "With -flush option it will be able to instrument duplicate classes");

}
