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

import com.sun.tdk.jcov.constants.MiscConstants;
import com.sun.tdk.jcov.instrument.*;
import com.sun.tdk.jcov.instrument.asm.ClassMorph;
import com.sun.tdk.jcov.runtime.AgentSocketSaver;
import com.sun.tdk.jcov.runtime.Collect;
import com.sun.tdk.jcov.runtime.CollectDetect;
import com.sun.tdk.jcov.runtime.FileSaver;
import com.sun.tdk.jcov.runtime.JCovSaver;
import com.sun.tdk.jcov.runtime.PropertyFinder;
import com.sun.tdk.jcov.runtime.SaverDecorator;
import com.sun.tdk.jcov.tools.EnvHandler;
import com.sun.tdk.jcov.tools.JCovTool;
import com.sun.tdk.jcov.tools.OptionDescr;
import com.sun.tdk.jcov.util.Utils;
import java.io.*;
import java.lang.instrument.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Dmitry Fazunenko
 * @author Alexey Fedorchenko
 */
public class Agent extends JCovTool {

    private boolean detectInternal;
    private boolean classesReload;
    private boolean instrumentField;
    private boolean instrumentAbstract;
    private boolean instrumentNative;
    private boolean instrumentAnonymous = true;
    private boolean instrumentSynthetic = true;
    private String[] include;
    private String[] exclude;
    private String[] m_include;
    private String[] m_exclude;
    private String[] callerInclude;
    private String[] callerExclude;
    private String[] fm;
    private String[] saveBegin;
    private String[] saveEnd;
    private String template;
    private String filename;
    private String flushPath;
    private InstrumentationOptions.InstrumentationMode mode;
    private InstrumentationOptions.MERGE merge;
    private boolean grabberSaver = false;

    static {
        Utils.initLogger();
        logger = Logger.getLogger(Agent.class.getName());
    }
    private final static Logger logger;
    private static ClassMorph classMorph;
    private String host;
    private int port;
    private static final Object LOCK = new Object();

    private static class SynchronizedSaverDecorator implements SaverDecorator {

        private JCovSaver wrap;

        public SynchronizedSaverDecorator(JCovSaver wrap) {
            init(wrap);
        }

        @Override
        public final void init(JCovSaver saver) {
            this.wrap = saver;
        }

        public void saveResults() {
            synchronized (LOCK) {
                wrap.saveResults();
            }
        }
    }

    /**
     * ClassFileTransformer implementation. Gets classfile binary data from VM
     * and runs ClassMorph.morph method.
     */
    private static class Tr implements ClassFileTransformer {

        /**
         * Path to flush instrumented classfiles to. Null means that
         * instrumented classfiles should not be flushed.
         */
        private final String flushpath;
        /**
         * Transformer name. Is not used.
         */
        private final String trname;
        /**
         * Can turn off agent instrumentation
         */
        private boolean ignoreLoads = true;

        /**
         * Creates new Tr instance
         *
         * @param trname Transformer name. Is not used.
         * @param flushpath Path to flush instrumented classfiles to. Null means
         * that instrumented classfiles should not be flushed.
         */
        public Tr(String trname, String flushpath) {
            this.trname = trname;
            this.flushpath = flushpath;
        }

        /**
         * transform method implementation
         *
         * @param loader
         * @param className
         * @param classBeingRedefined
         * @param protectionDomain
         * @param classfileBuffer
         * @return instrumented classfile binary data (if ignoreLoads is not set
         * to true, classfileBuffer will be returned otherwise). If collect is
         * not enabled - null is returned.
         */
        public byte[] transform(ClassLoader loader,
                String className,
                Class<?> classBeingRedefined,
                ProtectionDomain protectionDomain,
                byte[] classfileBuffer) {
            synchronized (LOCK) {
                if (Collect.enabled == false) {
                    return null; // signals to the VM that no changes were done
                }

                // no need to enter when Collect is disabled
                CollectDetect.enterInstrumentationCode(); // ensuring that instrumenting will not influence on coverage data

                try {
                    if (ignoreLoads) {
                        logger.log(Level.INFO, "Ignore for now {0}", className);
                    } else {
                        logger.log(Level.INFO, "Try to transform {0}", className);

                        byte[] newBuff = classMorph.morph(classfileBuffer, loader, flushpath);
                        return newBuff;
                    }
                } catch (Throwable e) {
                    logger.log(Level.SEVERE, "Adaption failed for {0} with :{1}", new Object[]{className, e});
                    e.printStackTrace();
                } finally {
                    CollectDetect.leaveInstrumentationCode(); // release instrumentation lock
                }
                return null;
            }
        }
    }

    /**
     * Class for listening agent commands
     */
    private static class CommandThread extends Thread {

        /**
         * Agent commands
         */
        public static enum COMMAND {

            SAVE {
                String cmd() {
                    return "save";
                }
            },
            SAVED {
                String cmd() {
                    return "saved";
                }
            },
            EXIT {
                String cmd() {
                    return "exit";
                }
            },
            EXIT_WITHOUT_SAVE {
                String cmd() {
                    return "exitWithoutSave".toLowerCase();
                }
            },
            AUTOSAVE_DISABLED {
                String cmd() {
                    return "autosave disabled";
                }
            };

            abstract String cmd();
        }
        /**
         * Port to listen incoming messages
         */
        private int port;
        /**
         * Instrumentation params
         */
        private InstrumentationParams params;

        /**
         * Creates CommandThread instance
         *
         * @param port Port to listen incoming messages
         * @param params Instrumentation params
         */
        public CommandThread(int port, InstrumentationParams params) {
            this.port = port;
            this.params = params;
            setDaemon(true);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    ServerSocket sock = new ServerSocket(port);
                    Socket s = sock.accept();
//                    System.out.println("Accepted");
                    InputStream is = s.getInputStream();
                    byte[] buff = new byte[1024];
                    int l;
                    String rest = "";
                    while ((l = is.read(buff)) > 0) {
                        String msg = rest + new String(buff, 0, l, Charset.defaultCharset());
//                        System.out.println("Message: " + msg);
                        rest = performTask(msg, s);
                    }
                    sock.close();
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, "Network IOException", ex);
                }
            }
        }

        /**
         * Parse and execute incoming message
         *
         * @param msg message
         * @param sock socket
         * @return exit code
         * @throws IOException
         */
        private String performTask(String msg, Socket sock) throws IOException {
            Pattern p = Pattern.compile("\\p{Space}*(\\p{Digit}+).*");
            msg = msg.toLowerCase(Locale.getDefault());
            PrintStream ps = new PrintStream(sock.getOutputStream(), false, "UTF-8");
            while (msg.length() > 0) {
                msg = msg.trim();
                COMMAND cmd = nextCommand(msg);
                if (cmd == null) {
                    break;
                } else {
                    switch (cmd) {
                        case SAVE:
                            msg = msg.substring(cmd.cmd().length());
                            if (Collect.enabled) {
                                Collect.disable();
                                Collect.saveResults();
                                params.enable();
                            }
                            ps.print(COMMAND.SAVED.cmd());
                            ps.flush();
                            break;
                        case EXIT:
                            msg = msg.substring(cmd.cmd().length());
                            Matcher m = p.matcher(msg);
                            int exitCode = 0;
                            if (m.matches()) {
                                exitCode = Integer.parseInt(m.group(1));
                            }
                            System.exit(exitCode);
                            break;
                        case EXIT_WITHOUT_SAVE:
                            msg = msg.substring(cmd.cmd().length());
                            m = p.matcher(msg);
                            exitCode = 0;
                            if (m.matches()) {
                                exitCode = Integer.parseInt(m.group(1));
                            }
                            FileSaver.setDisableAutoSave(true);
                            ps.print(COMMAND.AUTOSAVE_DISABLED.cmd());
                            ps.flush();
                            System.exit(exitCode);
                            break;
                    }
                }
            }

            return msg;
        }

        /**
         * Parse incomming message and return COMMAND value
         *
         * @param msg message to parse
         * @return associated COMMAND value
         */
        private COMMAND nextCommand(String msg) {
            String foundPref = "";
            COMMAND found = null;

            for (COMMAND c : COMMAND.values()) {
                if (msg.startsWith(c.cmd()) && foundPref.length() < c.cmd().length()) {
                    found = c;
                    foundPref = c.cmd();
                }
            }

            return found;
        }
    }

    /**
     * javaagent entry point
     *
     * @param agentArgs
     * @param instArg
     * @throws Exception
     */
    public static void premain(String agentArgs, Instrumentation instArg) {

        // handling JCovTool

        // This method manages CLI handling for Agent tool.
        // If any change is performed here - check JCovCMDTool CLI handling logic.

        Agent tool = new Agent();

        EnvHandler handler = tool.defineHandler();

        try {
            // proccess cmd options
            if (agentArgs == null) {
                agentArgs = "";
            }
            handler.parseCLIArgs(EnvHandler.parseAgentString(agentArgs));
            tool.handleEnv(handler);
            if (handler.isSet(EnvHandler.PRINT_ENV)) {
                handler.printEnv();
            }
        } catch (EnvHandler.CLParsingException ex) {
            if (handler.isSet(EnvHandler.HELP)) {
                handler.usage();
                handler.getOut().println("\n JCov Agent command line error: " + ex.getMessage() + "\n");
                System.exit(ERROR_CMDLINE_EXIT_CODE);
            }

            if (handler.isSet(EnvHandler.HELP_VERBOSE)) {
                handler.usage(true);
                handler.getOut().println("\n JCov Agent command line error: " + ex.getMessage() + "\n");
                System.exit(ERROR_CMDLINE_EXIT_CODE);
            }

            handler.getOut().println(" JCov Agent command line error: " + ex.getMessage() + "\n");
            handler.getOut().println("Use \"java -jar jcov.jar Agent -h\" for command-line help or \"java -jar jcov.jar Agent -hv\" for wider description");
            System.exit(ERROR_CMDLINE_EXIT_CODE);
        } catch (EnvHandlingException ex) {
            handler.getOut().println("JCov Agent command line error: " + ex.getMessage() + "\n");
            handler.getOut().println("Use \"java -jar jcov.jar Agent -h\" for command-line help or \"java -jar jcov.jar Agent -hv\" for wider description");
            if (handler.isSet(EnvHandler.PRINT_ENV)) {
                handler.printEnv();
            }
            System.exit(ERROR_CMDLINE_EXIT_CODE);
        } catch (Throwable ex) {
            handler.getOut().println("JCov Agent command line error: " + ex.getMessage());
            System.exit(ERROR_CMDLINE_EXIT_CODE);
        }
        if (handler.isSet(EnvHandler.PRINT_ENV)) {
            handler.printEnv();
            System.exit(SUCCESS_EXIT_CODE);
        }

        try {
            if (Utils.getJavaVersion() >= Utils.VER1_6) {
                tool.premainV50(agentArgs, instArg);
            } else {
                tool.premainV49(agentArgs, instArg);
            }
        } catch (Exception ex) {
            System.out.println("Agent execution error: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(ERROR_EXEC_EXIT_CODE);
        }
    }

    /**
     * premain chain for classfiles V50+
     *
     * @param agentArgs
     * @param inst
     * @throws Exception
     */
    public void premainV50(String agentArgs, Instrumentation inst) throws Exception {
        InstrumentationParams params =
                new InstrumentationParams(true, classesReload, true, instrumentNative, instrumentField,
                detectInternal, instrumentAbstract ? InstrumentationOptions.ABSTRACTMODE.DIRECT : InstrumentationOptions.ABSTRACTMODE.NONE,
                include, exclude, callerInclude, callerExclude, m_include, m_exclude, mode, saveBegin, saveEnd)
                .setInstrumentAnonymous(instrumentAnonymous)
                .setInstrumentSynthetic(instrumentSynthetic);

        params.enable();
        CollectDetect.enterInstrumentationCode();

        Tr transformer = new Tr("RetransformApp", flushPath);
        inst.addTransformer(transformer, true);
        if (params.isInstrumentNative()) {
            inst.setNativeMethodPrefix(transformer, InstrumentationOptions.nativePrefix);
        }

        DataRoot root = new DataRoot(agentArgs, params);
        classMorph = new ClassMorph(filename, root, params);
        Class[] classes = inst.getAllLoadedClasses();
        Set<Class> examinedClasses = new HashSet<Class>(Arrays.asList(classes));
        int keep = 0;
        for (Class c : classes) {
            if (inst.isModifiableClass(c)
                    && classMorph.shouldTransform(c.getName().replace('.', '/'))
                    && !c.getName().replace('.', '/').equals("jdk/internal/reflect/Reflection")
                    && !c.getName().replace('.', '/').equals("sun/reflect/Reflection")) {
                classes[keep++] = c;
            }
        }
        transformer.ignoreLoads = false;
        if (keep > 0) {
            classes = Utils.copyOf(classes, keep);
            logger.log(Level.INFO, "About to retransform {0} classes {1}", new Object[]{keep, classes[0]});
            try {
                inst.retransformClasses(classes);
            } catch (UnmodifiableClassException e) {
                System.err.println("Should not happen: " + e);
                e.printStackTrace(System.err);
            } catch (Throwable e) {
                System.err.println("During retransform: " + e);
                e.printStackTrace(System.err);
            }
        }
        logger.log(Level.INFO, "Retransformed {0} classes", keep);
        Class[] allClasses = inst.getAllLoadedClasses();
        keep = 0;
        for (Class c : allClasses) {
            if (!examinedClasses.contains(c)
                    && inst.isModifiableClass(c)
                    && classMorph.shouldTransform(c.getName().replace('.', '/'))) {
                allClasses[keep++] = c;
            }
        }
        if (keep > 0) {
            logger.log(Level.INFO, "New not transformed: {0} classes {1}", new Object[]{keep, allClasses[0]});
            classes = Utils.copyOf(classes, keep);
            try {
                inst.retransformClasses(classes);
            } catch (UnmodifiableClassException e) {
                logger.log(Level.SEVERE, "retransformClasses: Should not happen: ", e);
                //log.log(.printStackTrace(System.err);
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "Error during retransform: ", e);
            }
        }

        if (!grabberSaver) {
            // File saver should perform full merge here, not only insert new classes.
            JCovSaver saver = FileSaver.getFileSaver(root, filename, template, merge, true);
            loadFileSaverClasses();
            Collect.setSaver(Collect.decorateSaver(new SynchronizedSaverDecorator(saver)));
        } else {
            AgentSocketSaver saver = new AgentSocketSaver(root, filename, host, port);
            Collect.setSaver(Collect.decorateSaver(new SynchronizedSaverDecorator(saver)));
        }
        CollectDetect.leaveInstrumentationCode();
        PropertyFinder.addAutoShutdownSave();

    }

    private void loadFileSaverClasses() throws IOException{
        File file = new File(filename + "_load");
        new FileOutputStream(file).close();
        file.delete();
    }

    /**
     * premain chain for classfiles V49+
     *
     * @param agentArgs
     * @param inst
     * @throws Exception
     */
    public void premainV49(String agentArgs, Instrumentation inst) throws Exception {
        InstrumentationParams params =
                new InstrumentationParams(true, instrumentNative, instrumentField,
                detectInternal, instrumentAbstract ? InstrumentationOptions.ABSTRACTMODE.DIRECT : InstrumentationOptions.ABSTRACTMODE.NONE,
                include, exclude, callerInclude, callerExclude, mode, saveBegin, saveEnd)
                .setInstrumentAnonymous(instrumentAnonymous)
                .setInstrumentSynthetic(instrumentSynthetic);

        params.enable();
        CollectDetect.enterInstrumentationCode();
        Tr transformer = new Tr("RetransformApp", flushPath);
        inst.addTransformer(transformer);
        /* if (Options.isInstrumentNative()) {
         inst.setNativeMethodPrefix(transformer, Options.nativePrefix);
         }
         */
        DataRoot root = new DataRoot(agentArgs, params);
        classMorph = new ClassMorph(filename, root, params);
        Class[] classes = inst.getAllLoadedClasses();
        Set<Class> examinedClasses = new HashSet<Class>(Arrays.asList(classes));
        int keep = 0;
        for (Class c : classes) {
            if (/*inst.isModifiableClass(c) &&*/classMorph.shouldTransform(c.getName().replace('.', '/'))) {
                classes[keep++] = c;
            }
        }
        if (keep > 0) {
            classes = Utils.copyOf(classes, keep);
            logger.log(Level.INFO, "About to retransform {0} classes {1}", new Object[]{keep, classes[0]});
        }
        logger.log(Level.INFO, "Retransformed {0} classes", keep);
        transformer.ignoreLoads = false;
        Class[] allClasses = inst.getAllLoadedClasses();
        keep = 0;
        for (Class c : allClasses) {
            if (!examinedClasses.contains(c)
                    && //  inst.isModifiableClass(c) &&
                    classMorph.shouldTransform(c.getName().replace('.', '/'))) {
                allClasses[keep++] = c;
            }
        }
        if (keep > 0) {
            classes = Utils.copyOf(allClasses, keep);
        }

        if (!grabberSaver) {
            // File saver should perform full merge here, not only insert new classes.
            JCovSaver saver = FileSaver.getFileSaver(root, filename, template, merge, true);
            loadFileSaverClasses();
            Collect.setSaver(Collect.decorateSaver(new SynchronizedSaverDecorator(saver)));
        } else {
            AgentSocketSaver saver = new AgentSocketSaver(root, filename, host, port);
            Collect.setSaver(Collect.decorateSaver(new SynchronizedSaverDecorator(saver)));
        }
        CollectDetect.leaveInstrumentationCode();
        PropertyFinder.addAutoShutdownSave();
    }

    public String usageString() {
        return "java -javaagent:jcov.jar=[=option=value[,option=value]*] ...";
    }

    public String exampleString() {
        return "java -javaagent:jcov.jar=include=java\\.lang\\.String,native=on,type=branch,abstract=off -jar MyApp.jar";
    }

    public String getDescr() {
        return "print help on usage jcov in dynamic mode";
    }

    @Override
    public boolean isMainClassProvided() {
        return false;
    }

///////// JCovTool implementation /////////
    @Override
    public EnvHandler defineHandler() {
        return new EnvHandler(new OptionDescr[]{
                    DSC_OUTPUT,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_MERGE,
                    // Verbosity
                    DSC_VERBOSE,
                    DSC_TIMEOUT,
                    DSC_PORT,
                    // Instrumentation parameters.
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_TEMPLATE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_TYPE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_INCLUDE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_EXCLUDE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_CALLER_INCLUDE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_CALLER_EXCLUDE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_MINCLUDE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_MEXCLUDE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_INCLUDE_LIST,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_EXCLUDE_LIST,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_MINCLUDE_LIST,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_MEXCLUDE_LIST,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_ABSTRACT,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_NATIVE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_FIELD,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_SYNTHETIC,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_ANONYM,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_CLASSESRELOAD,
                    // Data save points
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_SAVE_BEGIN,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_SAVE_AT_END,
                    ClassMorph.DSC_FLUSH_CLASSES,
                    DSC_GRABBER,
                    DSC_PORT_GRABBER,
                    DSC_HOST_GRABBER,
                    DSC_LOG
                }, this);
    }

    @Override
    public int handleEnv(EnvHandler opts) throws EnvHandlingException {
        String internal = "default";
        if (internal.equals("detect")) {
            detectInternal = true;
        } else if (internal.equals("show")) {
            detectInternal = true;
        } else if (internal.equals("include")) {
            detectInternal = false;
        } else if (internal.equals("default")) {
            detectInternal = true;
        } else {
            throw new Error("Parameter error");
        }

        mode = InstrumentationOptions.InstrumentationMode.fromString(opts.getValue(InstrumentationOptions.DSC_TYPE));
        if (opts.isSet(InstrumentationOptions.DSC_TEMPLATE)) {
            template = opts.getValue(InstrumentationOptions.DSC_TEMPLATE);
        }

        include = InstrumentationOptions.handleInclude(opts);
        exclude = InstrumentationOptions.handleExclude(opts);

        m_include = InstrumentationOptions.handleMInclude(opts);
        m_exclude = InstrumentationOptions.handleMExclude(opts);

        fm = InstrumentationOptions.handleFM(opts);

        callerInclude = opts.getValues(InstrumentationOptions.DSC_CALLER_INCLUDE);
//        System.out.println("Setup callerInclude " + Arrays.toString(callerInclude));
        callerExclude = opts.getValues(InstrumentationOptions.DSC_CALLER_EXCLUDE);
//        System.out.println("Setup callerExclude " + Arrays.toString(callerExclude));

        String abstractValue = opts.getValue(InstrumentationOptions.DSC_ABSTRACT);
        if (abstractValue.equals("off")) {
            instrumentAbstract = false;
        } else if (abstractValue.equals("on")) {
            instrumentAbstract = true;
        } else {
            // will not happen - checking inside EnvHandler
            throw new EnvHandlingException("'" + InstrumentationOptions.DSC_ABSTRACT.name + "' parameter value error: expected 'on' or 'off'; found: '" + abstractValue + "'");
        }

        String classesReloadValue = opts.getValue(InstrumentationOptions.DSC_CLASSESRELOAD);
        if (classesReloadValue.equals("on")) {
            classesReload = true;
        } else {
            classesReload = false;
        }

        String nativeValue = opts.getValue(InstrumentationOptions.DSC_NATIVE);
        if (nativeValue.equals("on")) {
            instrumentNative = true;
        } else if (nativeValue.equals("off")) {
            instrumentNative = false;
        } else {
            // will not happen - checking inside EnvHandler
            throw new EnvHandlingException("'" + InstrumentationOptions.DSC_NATIVE.name + "' parameter value error: expected 'on' or 'off'; found: '" + nativeValue + "'");
        }

        String fieldValue = opts.getValue(InstrumentationOptions.DSC_FIELD);
        if (fieldValue.equals("on")) {
            instrumentField = true;
        } else if (fieldValue.equals("off")) {
            instrumentField = false;
        } else {
            // will not happen - checking inside EnvHandler
            throw new EnvHandlingException("'" + InstrumentationOptions.DSC_FIELD.name + "' parameter value error: expected 'on' or 'off'; found: '" + fieldValue + "'");
        }

        String anonym = opts.getValue(InstrumentationOptions.DSC_ANONYM);
        if (anonym.equals("on")) {
            instrumentAnonymous = true;
        } else { // off
            instrumentAnonymous = false;
        }

        String synthetic = opts.getValue(InstrumentationOptions.DSC_SYNTHETIC);
        if (synthetic.equals("on")) {
            instrumentSynthetic = true;
        } else { // off
            instrumentSynthetic = false;
        }

        String mergeValue = opts.getValue(InstrumentationOptions.DSC_MERGE);
        if (mergeValue.equals("merge")) {
            merge = InstrumentationOptions.MERGE.MERGE;
        } else if (mergeValue.equals("scale")) {
            merge = InstrumentationOptions.MERGE.SCALE;
        } else if (mergeValue.equals("overwrite")) {
            merge = InstrumentationOptions.MERGE.OVERWRITE;
        } else if (mergeValue.equals("gensuff")) {
            merge = InstrumentationOptions.MERGE.GEN_SUFF;
        } else {
            // will never happen as this is checked in EnvHandler
            throw new EnvHandlingException("'" + InstrumentationOptions.DSC_MERGE.name + "' parameter value error: expected 'merge', 'scale', 'overwrite' or 'gensuff'; found: '" + mergeValue + "'");
        }

        saveBegin = opts.getValues(InstrumentationOptions.DSC_SAVE_BEGIN);
        saveEnd = opts.getValues(InstrumentationOptions.DSC_SAVE_AT_END);

        flushPath = opts.getValue(ClassMorph.DSC_FLUSH_CLASSES);
        if ("none".equals(flushPath)) {
            flushPath = null;
        }

        String logfile = opts.getValue(EnvHandler.LOGFILE);
        if (opts.isSet(DSC_LOG) || logfile != null) {
            if (logfile == null) {
                logfile = "jcov.log";
            }
            try {
                Utils.setLoggerHandler(new FileHandler(logfile));
            } catch (Exception ex) {
                throw new EnvHandlingException("Can't open file '" + logfile + "' for writing the log", ex);
            }
            if (opts.isSet(EnvHandler.LOGLEVEL)) {
                Utils.setLoggingLevel(opts.getValue(EnvHandler.LOGLEVEL));
            } else if (opts.isSet(DSC_VERBOSE)) {
                int verbositylevel = Utils.checkedToInt(opts.getValue(DSC_VERBOSE), "verbosity level", Utils.CheckOptions.INT_NONNEGATIVE);
                switch (verbositylevel) {
                    case 0:
                        logger.setLevel(Level.SEVERE);
                        Utils.setLoggingLevel(Level.SEVERE);
                        break;
                    case 1:
                        logger.setLevel(Level.CONFIG);
                        Utils.setLoggingLevel(Level.CONFIG);
                        break;
                    case 2:
                        logger.setLevel(Level.INFO);
                        Utils.setLoggingLevel(Level.INFO);
                        break;
                    case 3:
                        logger.setLevel(Level.ALL);
                        Utils.setLoggingLevel(Level.ALL);
                        break;
                    default:
                        throw new EnvHandlingException("Incorrect verbosity level (" + opts.getValue(DSC_VERBOSE) + ") - should be 0..3");
                }
            }
        } else {
            Utils.setLoggingLevel(Level.OFF);
        }

        if (opts.isSet(DSC_TIMEOUT)) {
            long timeout = Utils.checkedToInt(opts.getValue(DSC_TIMEOUT), "timeout value");
            if (timeout > 0) {
                Timer timer = new Timer(true);
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        logger.log(Level.INFO, "Agent has been timed out.");
                        if (Collect.enabled) {
                            Collect.disable();
                            Collect.saveResults();
                        }
                        Runtime.getRuntime().halt(0);
                    }
                }, timeout);
            }
        }

        grabberSaver = opts.isSet(DSC_GRABBER);
        if (grabberSaver) {
            host = opts.getValue(DSC_HOST_GRABBER);
            Utils.checkHostCanBeNull(host, "grabber host");

            this.port = Utils.checkedToInt(opts.getValue(DSC_PORT_GRABBER), "grabber port number", Utils.CheckOptions.INT_POSITIVE);
        }

        filename = opts.getValue(DSC_OUTPUT);
        if (!grabberSaver) {
            Utils.checkFileNotNull(filename, "output filename", Utils.CheckOptions.FILE_NOTISDIR, Utils.CheckOptions.FILE_PARENTEXISTS);
        }

        if (opts.isSet(DSC_PORT)) {
            CommandThread cmdThread = new CommandThread(Utils.checkedToInt(opts.getValue(DSC_PORT), "command listener port number", Utils.CheckOptions.INT_POSITIVE),
                    new InstrumentationParams(true, instrumentNative, instrumentField, detectInternal,
                    instrumentAbstract ? InstrumentationOptions.ABSTRACTMODE.DIRECT : InstrumentationOptions.ABSTRACTMODE.NONE,
                    include, exclude, callerInclude, callerExclude, mode, saveBegin, saveEnd)
                    .setInstrumentAnonymous(instrumentAnonymous)
                    .setInstrumentSynthetic(instrumentSynthetic));
            cmdThread.start();
        }

        return SUCCESS_EXIT_CODE;
    }
    public static final OptionDescr DSC_OUTPUT =
            new OptionDescr("file", new String[]{"url", "o"}, "Output path definition.",
            OptionDescr.VAL_SINGLE, "Specifies output data file. \n"
            + "If specified file already exists, collected data will be merged with data from file",
            "result.xml");
    public final static OptionDescr DSC_VERBOSE =
            new OptionDescr("verbose", "Verbosity level.",
            new String[][]{
                {"0", "minimal, only fatal failure diagnostic is printed"},
                {"1", "moderate, non-fatal errors are included in log"},
                {"2", "high, all warnings are included in log"},
                {"3", "highest, all messages are included in log"}
            },
            "Set verbosity level.", "0");
    public static final OptionDescr DSC_TIMEOUT =
            new OptionDescr("timeout", "Agent process timeout.",
            OptionDescr.VAL_SINGLE, "Specifies timeout for agent process in milliseconds.\n"
            + "0 means there is no timeout specified. Default is 0.\n",
            "0");
    // port now can be set only as "agent.port" via VM properties and env variables. "Port" is used only for grabber and socket saver.
    public static final OptionDescr DSC_PORT = new OptionDescr("agent.port", new String[]{"portcl"}, "Agent command listening port",
            OptionDescr.VAL_SINGLE, "Specifies port number to listen for driving commands.\n"
            + "Commands are executed sequentially, some may send messages in response. "
            + "Valid commands to send are: \n"
            + "   \"save\" - to save already collected data. It will respond with \"saved\" message\n"
            + "   \"exit\" - to perform System.exit() immediately. Exit code number may be sent with this command.\n"
            + "              It's chars should follow \"exit\"");
    public static final OptionDescr DSC_PORT_GRABBER = new OptionDescr("port", new String[]{"grabberport"}, "",
            OptionDescr.VAL_SINGLE, "Specifies port number to send data to the grabber", MiscConstants.JcovPortNumber + "");
    public static final OptionDescr DSC_HOST_GRABBER = new OptionDescr("host", new String[]{"grabberhost"}, "",
            OptionDescr.VAL_SINGLE, "Specifies host name to send data to the grabber", "localhost");
    public final static OptionDescr DSC_LOG =
            new OptionDescr("log", "logging", OptionDescr.VAL_NONE, "Turns on JCov's agent logging.\n"
            + "Log records saved in jcov.log file");
    public final static OptionDescr DSC_GRABBER =
            new OptionDescr("grabber", "use grabber saver", OptionDescr.VAL_NONE, "Use grabber saver instead of file saver. jcov.port "
            + "and jcov.host VM properties could be used to control the saver as well as JCOV_PORT and JCOV_HOST env variable");
}