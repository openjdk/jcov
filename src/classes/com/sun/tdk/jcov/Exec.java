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
import com.sun.tdk.jcov.data.FileFormatException;
import com.sun.tdk.jcov.data.Result;
import com.sun.tdk.jcov.instrument.InstrumentationOptions;
import com.sun.tdk.jcov.processing.ProcessingException;
import com.sun.tdk.jcov.tools.EnvHandler;
import com.sun.tdk.jcov.tools.JCovCMDTool;
import com.sun.tdk.jcov.tools.OptionDescr;
import com.sun.tdk.jcov.util.Utils;

import java.beans.EventHandler;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p> This tool is designed to simplify JCov Grabber usage. It starts a
 * grabber, then executes specified command and waits while command is active.
 * After finishing the command Exec will stop the grabber. </p> <p> Exec also
 * can instrument the product with -instr option. Instrumentation is performed
 * recursively so that entire product is copied to new location and every single
 * binary file (class or jar/zip/war) will be instrumented. </p>
 *
 * @author Andrey Titov
 */
public class Exec extends JCovCMDTool {

    static final Logger logger;

    static {
        Utils.initLogger();
        logger = Logger.getLogger(Exec.class.getName());
    }
    private String command[];
    private Grabber grabber;
    private String outLogFile;
    private String errLogFile;
    private String grabberLogFile;
    private String template = MiscConstants.JcovTemplateFileNameXML;
    private String outTestList;
    private String outputFile = MiscConstants.JcovSaveFileNameXML;
    private File commandDir = null;
    // Product instrumentation
    private ProductInstr pInstr;
    // Report generation
    private File reportDir;
    private EnvHandler envHandler;

    private void instrumentProduct() throws Exception {
        pInstr.instrumentProduct();
    }

    private void runCommand() throws IOException, Exception {
        if (pInstr != null || reportDir != null) {
            logger.log(Level.INFO, " - Starting command");
        }
        logger.log(Level.CONFIG, "Command to run: ''{0}''", Arrays.toString(command));

        OutputStream outStream = null;
        OutputStream errStream = null;
        Process proc = null;
        try {
            grabber.handleEnv(envHandler);
            grabber.createServer();
            logger.log(Level.INFO, "Starting the Grabber");
            grabber.startServer();

            if (outLogFile != null) {
                outStream = new FileOutputStream(outLogFile);
            } else {
                outStream = System.out;
            }

            if (errLogFile != null) {
                if (errLogFile.equals(outLogFile)) {
                    errStream = outStream;
                } else {
                    errStream = new FileOutputStream(errLogFile);
                }
            } else {
                if (outLogFile != null) {
                    errStream = outStream;
                } else {
                    errStream = System.err;
                }
            }

            ProcessBuilder pb =
                    new ProcessBuilder(command)
                    .redirectErrorStream(errStream == outStream)
                    .directory(commandDir);

            pb.environment().put("JCOV_PORT", Integer.toString(grabber.getServerPort()));
            logger.log(Level.INFO, "Starting the command");
            proc = pb.start();

            InputStream inputStream = proc.getInputStream();
            new Thread(new Redirector(inputStream, outStream)).start();
            if (errStream != outStream) {
                new Thread(new Redirector(proc.getErrorStream(), errStream)).start();
            }

            int exitStatus = proc.waitFor();

            if (exitStatus == 0) {
                logger.log(Level.FINE, "Command finished with 0");
            } else {
                logger.log(Level.WARNING, "Command finished with {0}", exitStatus);
            }

            proc = null;
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            if (proc != null) {
                logger.log(Level.INFO, "Destroying the process...");
                proc.destroy();
            }
            if (outLogFile != null) {
                outStream.close();
                if (outStream != errStream) {
                    errStream.close();
                }
            }
            try {
                logger.log(Level.INFO, "Stopping the grabber (not forcely)...");
                grabber.stopServer(false);
            } catch (Throwable e) {
                e.printStackTrace();
            }

            if (!new File(outputFile).exists()) {
                throw new Exception("Output file " + outputFile + " was not created after grabber stop." + (reportDir != null ? " Can't write the report." : ""));
            }
        }
    }

    private void createReport() throws ProcessingException, FileFormatException, Exception {
        logger.log(Level.INFO, " - Generating report");
        logger.log(Level.CONFIG, "Output report directory: ''{0}''", reportDir.getPath());
        RepGen rg = new RepGen();
        rg.configure(null, null, null, null, false, false, false, false, false, false, false, false);

        Result res;
        if (outTestList == null) {
            res = new Result(outputFile);
        } else {
            res = new Result(outputFile, outTestList);
        }
        rg.generateReport(reportDir.getAbsolutePath(), res);
    }

    @Override
    protected int run() throws Exception {
        if (pInstr != null) {
            instrumentProduct();
        }

        runCommand();

        if (reportDir != null) {
            createReport();
        }

        return SUCCESS_EXIT_CODE;
    }

    @Override
    protected EnvHandler defineHandler() {
        return new EnvHandler(new OptionDescr[]{
                    DSC_TEST_COMMAND,
                    DSC_TEST_COMMANDS,
                    DSC_COMMAND_DIR,
                    Grabber.DSC_PORT,
                    Grabber.DSC_OUTPUT,
                    Merger.DSC_OUTPUT_TEST_LIST,
                    DSC_REDIRECT_OUT,
                    DSC_REDIRECT_ERR,
                    DSC_GRABBER_REDIRECT,
                    ProductInstr.DSC_INSTRUMENT,
                    ProductInstr.DSC_INSTRUMENT_TO,
                    ProductInstr.DSC_RT_TO,
                    Instr.DSC_INCLUDE_RT,
                    DSC_REPORT,
                    InstrumentationOptions.DSC_TEMPLATE,}, this);
    }

    @Override
    protected int handleEnv(EnvHandler envHandler) throws EnvHandlingException {
        if (envHandler.isSet(DSC_TEST_COMMAND)) {
            command = splitNoQuotes(envHandler.getValue(DSC_TEST_COMMAND));
            if (envHandler.isSet(DSC_TEST_COMMANDS)) {
                logger.log(Level.CONFIG, "'-commands' option ignored as '-command' option specified");
            }
        } else if (envHandler.isSet(DSC_TEST_COMMANDS)) {
            command = envHandler.getValues(DSC_TEST_COMMANDS);
        } else {
            throw new EnvHandlingException("'-command' or '-commands' option needed");
        }

        if (envHandler.isSet(DSC_COMMAND_DIR)) {
            commandDir = Utils.checkFileNotNull(envHandler.getValue(DSC_COMMAND_DIR), "command dir");
        }

        if (envHandler.isSet(ProductInstr.DSC_INSTRUMENT)) {
            pInstr = new ProductInstr();
            pInstr.handleEnv(envHandler);
        }

        reportDir = Utils.checkFileCanBeNull(envHandler.getValue(DSC_REPORT), "report directory", Utils.CheckOptions.FILE_NOTEXISTS, Utils.CheckOptions.FILE_CANWRITE);

        if (envHandler.isSet(DSC_GRABBER_REDIRECT)) {
            grabberLogFile = envHandler.getValue(DSC_GRABBER_REDIRECT);
            Logger grLogger = Logger.getLogger(Grabber.class.getName());
            grLogger.setUseParentHandlers(false);
            try {
                grLogger.addHandler(new FileHandler(grabberLogFile));
            } catch (Exception ex) {
                throw new EnvHandlingException("Error opening file for logging grabber: " + ex.getMessage(), ex);
            }
        }

        grabber = new Grabber();
//        grabber.handleEnv(envHandler);
        this.envHandler = envHandler;

        if (envHandler.isSet(DSC_REDIRECT_ERR)) {
            errLogFile = envHandler.getValue(DSC_REDIRECT_ERR);
            Utils.checkFileNotNull(errLogFile, "error log file", Utils.CheckOptions.FILE_CANWRITE, Utils.CheckOptions.FILE_NOTISDIR);
        }
        if (envHandler.isSet(DSC_REDIRECT_OUT)) {
            outLogFile = envHandler.getValue(DSC_REDIRECT_OUT);
            Utils.checkFileNotNull(outLogFile, "output log file", Utils.CheckOptions.FILE_CANWRITE, Utils.CheckOptions.FILE_NOTISDIR);
        }

        if (envHandler.isSet(InstrumentationOptions.DSC_TEMPLATE)) {
            template = envHandler.getValue(InstrumentationOptions.DSC_TEMPLATE);
            Utils.checkFileNotNull(template, "template file", Utils.CheckOptions.FILE_PARENTEXISTS);
        }

        if (envHandler.isSet(Merger.DSC_OUTPUT_TEST_LIST)) {
            outTestList = envHandler.getValue(Merger.DSC_OUTPUT_TEST_LIST);
            Utils.checkFileNotNull(outTestList, "output testlist file", Utils.CheckOptions.FILE_CANWRITE, Utils.CheckOptions.FILE_NOTISDIR, Utils.CheckOptions.FILE_NOTEXISTS);
        }

        if (envHandler.isSet(Grabber.DSC_OUTPUT)) {
            outputFile = envHandler.getValue(Grabber.DSC_OUTPUT);
            Utils.checkFileNotNull(outputFile, "output file", Utils.CheckOptions.FILE_CANWRITE, Utils.CheckOptions.FILE_NOTISDIR);
        }

        return 0;
    }

    @Override
    protected String getDescr() {
        return "Executes a command collecting coverage data in a grabber";
    }

    @Override
    protected String usageString() {
        return "java -jar jcov.jar exec -command <command to run> [-option value]";
    }

    @Override
    protected String exampleString() {
        return "java -jar jcov.jar exec -command \"./runtests.sh -testoptions:\\\"-javaagent:jcov.jar=grabber=\\\"\"";
    }

    /**
     * <p> Splits a string containing quotes ' and " by spaces. Everything
     * quoted is not splitted. Non-quoted quotes are removed. </p>
     *
     * @param str
     * @return
     */
    public static String[] splitNoQuotes(String str) {
        Character quote = null;

        LinkedList<String> r = new LinkedList<String>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); ++i) {
            char charAt = str.charAt(i);
            if (quote == null) {
                switch (charAt) {
                    case '\'':
                    case '\"':
                        quote = charAt;
                        break;
                    case ' ':
                    case '\t':
                    case '\n':
                    case '\f':
                    case '\r':
                    case 0x0B:
                        if (sb.length() > 0) {
                            r.add(sb.toString());
                            sb = new StringBuilder();
                        }
                        break;
                    default:
                        sb.append(charAt);
                }
            } else {
                if (charAt == quote) {
                    quote = null;
                } else {
                    sb.append(charAt);
                }
            }
        }
        if (sb.length() > 0) {
            r.add(sb.toString());
        }

        return r.toArray(new String[r.size()]);
    }
    public static final OptionDescr DSC_TEST_COMMAND =
            new OptionDescr("command", new String[]{"comm", "cmd"}, "Exec commands",
            OptionDescr.VAL_SINGLE, "Command running tests over instrumented classes or using JCov agent. Use quotes for arguments: -command \"./tests.sh -arg1 -arg2 arg3\"");
    public static final OptionDescr DSC_COMMAND_DIR =
            new OptionDescr("command.dir", new String[]{"dir"}, "", OptionDescr.VAL_SINGLE, "Specify directory to run the command");
    public static final OptionDescr DSC_TEST_COMMANDS =
            new OptionDescr("commands", new String[]{"comms", "cmds"}, "",
            OptionDescr.VAL_ALL, "Command running tests over instrumented classes or using JCov agent. Use quotes for arguments: -command \"./tests.sh -arg1 -arg2 arg3\"");
    public final static OptionDescr DSC_REDIRECT_OUT =
            new OptionDescr("out.file", new String[]{"out", "log.command"}, "", OptionDescr.VAL_SINGLE, "Redirect command output to a file");
    public final static OptionDescr DSC_REDIRECT_ERR =
            new OptionDescr("err.file", new String[]{"err"}, "", OptionDescr.VAL_SINGLE, "Redirect command error output to a file");
    public final static OptionDescr DSC_GRABBER_REDIRECT =
            new OptionDescr("log.grabber", "", OptionDescr.VAL_SINGLE, "Redirect grabber output to a file");
    public final static OptionDescr DSC_REPORT =
            new OptionDescr("report", "", OptionDescr.VAL_SINGLE, "");

    public static class Redirector implements Runnable {

        private final OutputStream out;
        private final BufferedReader rin;
        private final BufferedWriter rout;

        public Redirector(InputStream in, OutputStream out) {
            rin = new BufferedReader(new InputStreamReader(in, Charset.defaultCharset()));
            this.out = out;
            rout = new BufferedWriter(new OutputStreamWriter(out, Charset.defaultCharset()));
        }

        public void run() {
            String s;

            try {
                while ((s = rin.readLine()) != null) {
                    // out stream can be used in 2 threads simultaneously
                    synchronized (out) {
                        rout.write(s);
                        rout.newLine();
                        rout.flush();
                    }
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }
    }
}
