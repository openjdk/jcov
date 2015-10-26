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
import com.sun.tdk.jcov.data.Result;
import com.sun.tdk.jcov.tools.EnvHandler;
import com.sun.tdk.jcov.tools.JCovCMDTool;
import com.sun.tdk.jcov.tools.OptionDescr;
import com.sun.tdk.jcov.util.Utils;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * One command to get coverage on product by running user command.
 *
 * @author Alexey Fedorchenko
 */
public class JCov extends JCovCMDTool {

    private String userOutput;
    private String srcRootPath;
    private String command;
    private String template = MiscConstants.JcovTemplateFileNameXML;
    private String result = "result.xml";
    private String reportDirName = "report";
    private int commandsPort = MiscConstants.JcovGrabberCommandPort;
    private int testsPort = MiscConstants.JcovPortNumber;
    private final String JCOV_NETWORK_JAR_NAME = "jcov_network_saver.jar";

    // logger initialization
    static {
        Utils.initLogger();
        logger = Logger.getLogger(JCov.class.getName());
    }
    private final static Logger logger;
    private final Object lock = new Object();

    @Override
    protected int run() throws Exception {

        //instr
        ProductInstr instr = new ProductInstr();
        File productDir = new File(srcRootPath);

        if (!productDir.exists()) {
            logger.log(Level.SEVERE, "No product to get coverage");
            return 1;
        }

        if (productDir.isFile()) {
            logger.log(Level.SEVERE, "Set product directory, not file");
            return 1;
        }

        File jcovJar = new File(JCov.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        File networkJar = new File(jcovJar.getParent() + File.separator + JCOV_NETWORK_JAR_NAME);
        if (!networkJar.exists()) {
            logger.log(Level.SEVERE, "Can not find " + JCOV_NETWORK_JAR_NAME + " in the jcov.jar location");
            logger.log(Level.SEVERE, networkJar.getAbsolutePath());
            return 1;
        }

        File parentProductDir = productDir.getCanonicalFile().getParentFile();

        //zip product
        try {
            Utils.zipFolder(productDir.getAbsolutePath(), parentProductDir.getAbsolutePath() + File.separator + productDir.getName() + ".zip");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Can not zip product", e);
            return 1;
        }

        Utils.addToClasspath(new String[]{srcRootPath});
        try {
            instr.run(new String[]{"-product", srcRootPath, "-productOutput", parentProductDir.getAbsolutePath() + File.separator + "instr",
                        "-rt", networkJar.getAbsolutePath()});
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error while instrument product", ex);
            return 1;
        }

        //reeplace product by instrumented one
        Utils.deleteDirectory(productDir);
        productDir = new File(productDir.getAbsolutePath());
        productDir.mkdir();


        File instrFiles = new File(parentProductDir.getAbsolutePath() + File.separator + "instr");
        for (File file : instrFiles.listFiles()) {
            if (file.isDirectory()) {
                Utils.copyDirectory(file, new File(productDir, file.getName()));
            } else {
                Utils.copyFile(file, new File(productDir, file.getName()));
            }

        }

        Utils.deleteDirectory(new File(parentProductDir.getAbsolutePath() + File.separator + "instr"));

        GrabberThread grabberThread = new GrabberThread();
        grabberThread.start();

        synchronized (lock) {
            while (!grabberThread.isStarted()) {
                lock.wait();
            }
        }

        //runcommand
        try {
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
            if (process.exitValue() != 0) {
                logger.log(Level.SEVERE, "wrong command for running tests.");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "exception in process", e);
        }

        //stop grabber
        GrabberManager grabberManager = new GrabberManager();
        grabberManager.setPort(commandsPort);
        grabberManager.sendKillCommand();

        //repgen
        RepGen rg = new RepGen();

        File outputFile = networkJar.getParentFile();
        if (userOutput != null && !userOutput.isEmpty()) {
            outputFile = new File(userOutput);
            if (!outputFile.exists()) {
                outputFile.mkdirs();
            }
        }

        try {
            Result res = new Result(result);
            rg.generateReport(rg.getDefaultReportGenerator(), outputFile.getAbsolutePath() + File.separator + reportDirName, res, srcRootPath);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error in report generation", e);
        }

        System.out.println("coverage report for product: " + outputFile.getAbsolutePath() + File.separator + reportDirName);

        return SUCCESS_EXIT_CODE;
    }

    public static void main(String args[]) {
        JCov tool = new JCov();
        try {
            int res = tool.run(args);
            System.exit(res);
        } catch (Exception ex) {
            System.exit(1);
        }
    }

    @Override
    protected EnvHandler defineHandler() {
        EnvHandler envHandler = new EnvHandler(new OptionDescr[]{
                    DSC_PRODUCT, DSC_RUN_COMMAND, DSC_OUTPUT,}, this);

        return envHandler;
    }

    @Override
    protected int handleEnv(EnvHandler opts) throws EnvHandlingException {

        if (opts.isSet(DSC_PRODUCT)) {
            srcRootPath = opts.getValue(DSC_PRODUCT);
        }

        if (opts.isSet(DSC_RUN_COMMAND)) {
            command = opts.getValue(DSC_RUN_COMMAND);
        } else {
            throw new EnvHandlingException("command to run tests is not specified");
        }

        if (opts.isSet(DSC_OUTPUT)) {
            userOutput = opts.getValue(DSC_OUTPUT);
        }

        return SUCCESS_EXIT_CODE;
    }

    @Override
    protected String getDescr() {
        return "gets product coverage with one command";
    }

    @Override
    protected String usageString() {
        return "java -jar jcov.jar JCov -pro productDirPath -command \"java -jar tests.jar\"";
    }

    @Override
    protected String exampleString() {
        return "java -jar jcov.jar JCov -pro productDirPath -command \"java -jar tests.jar\"";
    }
    public final static OptionDescr DSC_PRODUCT =
            new OptionDescr("product", new String[]{"product", "pro"}, "Product files.", OptionDescr.VAL_SINGLE, "");
    public final static OptionDescr DSC_RUN_COMMAND =
            new OptionDescr("command", new String[]{"command", "cmd"}, "Command to run on product and get coverage", OptionDescr.VAL_SINGLE, "");
    public final static OptionDescr DSC_OUTPUT =
            new OptionDescr("output", new String[]{"output", "out", "o"}, "Output dir to create the result report directory", OptionDescr.VAL_SINGLE, "");

    private class GrabberThread extends Thread {

        private boolean started = false;

        public GrabberThread() {
            super();
        }

        public boolean isStarted() {
            return started;
        }

        @Override
        public void run() {
            try {
                //grabber
                Grabber grabber = new Grabber();
                grabber.setCommandPort(commandsPort);
                grabber.setPort(testsPort);
                grabber.setSaveOnReceive(false);
                grabber.setTemplate(template);
                grabber.setOutputFilename(result);
                grabber.start(true);

                synchronized (lock) {
                    started = true;
                    lock.notifyAll();
                }

                grabber.waitForStopping();

            } catch (Exception e) {
                logger.log(Level.SEVERE, "grabber exception", e);
            }
        }
    }
}
