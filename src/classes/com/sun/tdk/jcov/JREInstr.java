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

import com.sun.tdk.jcov.runtime.JCovSESocketSaver;
import com.sun.tdk.jcov.tools.EnvHandler;
import com.sun.tdk.jcov.tools.JCovCMDTool;
import com.sun.tdk.jcov.tools.OptionDescr;
import com.sun.tdk.jcov.util.Utils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p> A tool to statically instrument JRE. </p> <p> There are 2 coverage
 * collection modes: static and dynamic. In static mode JCov reads and modifies
 * classes bytecode inserting there some instructions which will use JCov RT
 * libraries. In dynamic mode (aka Agent mode) a VM agent is used ("java
 * -javaagent") that instruments bytecode just at loadtime. </p>
 *
 * @author Andrey Titov
 * @author Dmitry Fazunenko
 * @author Alexey Fedorchenko
 */
public class JREInstr extends JCovCMDTool {

    private Instr instr;
    private File toInstrument;
    private File[] addJars;
    private File[] addJimages;
    private File[] addTests;
    private File implant;
    private File javac;
    private String[] callerInclude;
    private String[] callerExclude;
    private static final Logger logger;
    private String host = null;
    private Integer port = null;

    static {
        Utils.initLogger();
        logger = Logger.getLogger(Instr.class.getName());
    }

    /**
     * tries to find class in the specified jars
     */
    public class StaticJREInstrClassLoader extends URLClassLoader {

        StaticJREInstrClassLoader(URL[] urls) {
            super(urls);
        }

        @Override
        public InputStream getResourceAsStream(String s) {
            InputStream in = null;
            try {
                in = findResource(s).openStream();
            } catch (IOException ex) {
                //nothing to do
            }
            if (in != null) {
                return in;
            }
            return super.getResourceAsStream(s);
        }
    }

    @Override
    protected int run() throws Exception {
        final String[] toInstr = new String[]{toInstrument.getAbsolutePath()};
        Utils.addToClasspath(toInstr);
        instr.startWorking();

        StaticJREInstrClassLoader cl = new StaticJREInstrClassLoader(new URL[]{toInstrument.toURI().toURL()});
        instr.setClassLoader(cl);

        if (toInstrument.getAbsolutePath().endsWith("bootmodules.jimage")){

            ArrayList<File> jdkImages = new ArrayList<File>();
            jdkImages.add(toInstrument);
            if (addJimages != null) {
                Collections.addAll(jdkImages, addJimages);
            }

            for (File jimageInstr: jdkImages) {
                String tempDirName = jimageInstr.getName().substring(0, jimageInstr.getName().indexOf(".jimage"));

                expandJimage(jimageInstr, tempDirName);

                File dirtoInstrument = new File(jimageInstr.getParent(), tempDirName);
                Utils.addToClasspath(new String[]{dirtoInstrument.getAbsolutePath()});
                if (jimageInstr.equals(toInstrument)) {
                    instr.instrumentFile(dirtoInstrument.getAbsolutePath(), null, implant.getAbsolutePath());
                }
                else{
                    instr.instrumentFile(dirtoInstrument.getAbsolutePath(), null, null);
                }
                createJimage(dirtoInstrument, jimageInstr.getAbsolutePath() + "i");

            }
            for (File jimageInstr: jdkImages) {

                String tempDirName = jimageInstr.getName().substring(0, jimageInstr.getName().indexOf(".jimage"));
                File dirtoInstrument = new File(jimageInstr.getParent(), tempDirName);
                if (!Utils.deleteDirectory(dirtoInstrument)) {
                    logger.log(Level.SEVERE, "please, delete " + tempDirName + " jimage dir manually");
                }

                Utils.copyFile(jimageInstr, new File(jimageInstr.getParent(), jimageInstr.getName() + ".bak"));

                if(!jimageInstr.delete()){
                    logger.log(Level.SEVERE, "please, delete original jimage manually: "+jimageInstr);
                }
                else{
                    Utils.copyFile(new File(jimageInstr.getAbsolutePath()+"i"), jimageInstr);
                    new File(jimageInstr.getAbsolutePath()+"i").delete();
                }

            }

        }
        else {
            instr.instrumentFile(toInstrument.getAbsolutePath(), null, implant.getAbsolutePath());
        }

        ArrayList<String> srcs = null;
        if (addJars != null) {
            srcs = new ArrayList<String>();
            for (int i = 0; i < addJars.length; ++i) {
                srcs.add(addJars[i].getAbsolutePath());
            }
        }

        if (addTests != null) {
            if (srcs == null) {
                srcs = new ArrayList<String>();
            }
            for (int i = 0; i < addTests.length; ++i) {
                srcs.add(addTests[i].getAbsolutePath());
            }
        }

        if (srcs != null) {
            Utils.addToClasspath(srcs.toArray(new String[0]));
            instr.instrumentFiles(srcs.toArray(new String[0]), null, null);
        }

        instr.finishWork();
        return SUCCESS_EXIT_CODE;
    }


    private boolean expandJimage(File jimage, String tempDirName){
        try {
            String command = jimage.getParentFile().getParentFile().getParent()+File.separator+"bin"+File.separator+"jimage extract --dir "+
                    jimage.getParent()+File.separator+tempDirName+" "+jimage.getAbsolutePath();
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
            if (process.exitValue() != 0) {
                logger.log(Level.SEVERE, "wrong command for expand jimage: "+command);
                return false;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "exception in process(expanding jimage)", e);
            return false;
        }
        return true;
    }

    private boolean createJimage(File dir, String new_jimage_path){
        try {
            String command = dir.getParentFile().getParentFile().getParent()+File.separator+"bin"+File.separator+"jimage recreate --dir "+
                    dir + " "+ new_jimage_path;
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
            if (process.exitValue() != 0) {
                logger.log(Level.SEVERE, "wrong command for create jimage: "+command);
                return false;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "exception in process(expanding jimage)", e);
            return false;
        }
        return true;
    }

    @Override
    protected EnvHandler defineHandler() {
        Instr.DSC_INCLUDE_RT.usage = "To run instrumented JRE you should implant JCov runtime library both into rt.jar and into 'lib/endorsed' directory.\nWhen instrumenting whole JRE dir with jreinstr tool - these 2 actions will be done automatically.";
        return new EnvHandler(new OptionDescr[]{
                    Instr.DSC_INCLUDE_RT,
                    Instr.DSC_VERBOSE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_TYPE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_INCLUDE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_INCLUDE_LIST,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_EXCLUDE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_EXCLUDE_LIST,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_CALLER_INCLUDE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_CALLER_EXCLUDE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_TEMPLATE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_ABSTRACT,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_NATIVE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_FIELD,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_SYNTHETIC,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_ANONYM,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_INNERINVOCATION,
                    Instr.DSC_SUBSEQUENT,
                    DSC_JAVAC_HACK,
                    DCS_ADD_JAR,
                    DCS_ADD_JIMAGE,
                    DCS_ADD_TESTS,
                    DSC_HOST,
                    DSC_PORT
                }, this);
    }

    @Override
    protected int handleEnv(EnvHandler envHandler) throws EnvHandlingException {
        instr = new Instr();

        String[] tail = envHandler.getTail();
        if (tail == null || tail.length == 0) {
            throw new EnvHandlingException("JRE dir is not specified");
        }
        if (tail.length > 1) {
            logger.log(Level.WARNING, "Only first argument ({0}) will be used", tail[0]);
        }

        if (!envHandler.isSet(Instr.DSC_INCLUDE_RT)) {
            throw new EnvHandlingException("Runtime should be always implanted when instrumenting rt.jar (e.g. '-rt jcov_j2se_rt.jar')");
        }

        implant = new File(envHandler.getValue(Instr.DSC_INCLUDE_RT));
        Utils.checkFile(implant, "JCovRT library jarfile", Utils.CheckOptions.FILE_ISFILE, Utils.CheckOptions.FILE_EXISTS, Utils.CheckOptions.FILE_CANREAD);

        if (envHandler.isSet(DCS_ADD_JAR)) {
            String[] jars = envHandler.getValues(DCS_ADD_JAR);
            addJars = new File[jars.length];
            for (int i = 0; i < addJars.length; ++i) {
                addJars[i] = new File(jars[i]);
                if (!addJars[i].exists()) {
                    throw new EnvHandlingException("Additional jar " + jars[i] + " doesn't exist");
                }
                if (!addJars[i].canRead()) {
                    throw new EnvHandlingException("Can't read additional jar " + jars[i]);
                }
            }
        }
        if (envHandler.isSet(DCS_ADD_JIMAGE)) {
            String[] images = envHandler.getValues(DCS_ADD_JIMAGE);
            addJimages = new File[images.length];
            for (int i = 0; i < addJimages.length; ++i) {
                addJimages[i] = new File(images[i]);
                if (!addJimages[i].exists()) {
                    throw new EnvHandlingException("Additional jimage " + images[i] + " doesn't exist");
                }
                if (!addJimages[i].canRead()) {
                    throw new EnvHandlingException("Can't read additional jimage " + images[i]);
                }
            }
        }

        if (envHandler.isSet(DCS_ADD_TESTS)) {
            String[] files = envHandler.getValues(DCS_ADD_TESTS);
            addTests = new File[files.length];
            for (int i = 0; i < addTests.length; ++i) {
                addTests[i] = new File(files[i]);
                if (!addTests[i].exists()) {
                    throw new EnvHandlingException("Test file " + files[i] + " doesn't exist");
                }
                if (!addTests[i].canRead()) {
                    throw new EnvHandlingException("Can't read test file " + files[i]);
                }
            }
        }

        File f = new File(tail[0]);
        Utils.checkFile(f, "JRE directory", Utils.CheckOptions.FILE_EXISTS, Utils.CheckOptions.FILE_ISDIR, Utils.CheckOptions.FILE_CANREAD);

        if (envHandler.isSet(DSC_HOST)) {
            host = envHandler.getValue(DSC_HOST);
        }

        if (envHandler.isSet(DSC_PORT)) {
            try {
                port = Integer.valueOf(envHandler.getValue(DSC_PORT));
            } catch (NumberFormatException nfe) {
                throw new EnvHandlingException("Specify correct port number");
            }
        }

        if (envHandler.isSet(DSC_JAVAC_HACK)) {
            String javacPath = envHandler.getValue(DSC_JAVAC_HACK);
            javac = new File(javacPath);

            File newJavac = null;

            boolean isWin = false;
            if (javacPath.endsWith(".exe") && System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
                newJavac = new File(javac.getParent() + File.separator + "javac_real.exe");
                isWin = true;
            } else {
                newJavac = new File(javac.getParent() + File.separator + "javac_real");
            }


            if (newJavac.exists()) {
                if (javac.exists()) {
                    logger.log(Level.INFO, "javac seems to be already hacked: {0} exists", newJavac.getPath());
                } else {
                    try {
                        if (!isWin) {
                            File newFile = new File(javacPath);
                            OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(newFile), Charset.forName("UTF-8"));
                            out.write(newJavac.getAbsolutePath() + " -J-Xms30m \"$@\"");
                            out.flush();
                            out.close();
                            try {
                                newFile.setExecutable(true);
                            } catch (Exception e) {
                                logger.log(Level.WARNING, "Can't make new hacked javac file executable: {0}", e.getMessage());
                            }
                        } else {
                            File newFile = new File(javacPath.replaceAll(".exe", ".bat"));
                            OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(newFile), Charset.forName("UTF-8"));
                            out.write(newJavac.getAbsolutePath() + " -J-Xms30m %*");
                            out.flush();
                            out.close();
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(JREInstr.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else {
                if (!javac.exists()) {
                    throw new EnvHandlingException("Specified javac doesn't exist (" + javacPath + ")");
                }

                if (!javac.isFile()) {
                    throw new EnvHandlingException("Specified javac is not a file (" + javacPath + ")");
                }

                if (!javac.canWrite()) {
                    throw new EnvHandlingException("Can't modify specified javac (" + javacPath + ")");
                }

                if (!javac.renameTo(newJavac)) {
                    throw new EnvHandlingException("Can't move specified javac to new location (" + javacPath + " to " + newJavac.getPath() + ")");
                }
                try {
                    if (!isWin) {
                        File newFile = new File(javacPath);
                        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(newFile), Charset.forName("UTF-8"));
                        out.write(newJavac.getAbsolutePath() + " $@ -J-Xms30m");
                        out.flush();
                        out.close();
                        try {
                            newFile.setExecutable(true);
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Can't make new hacked javac file executable: {0}", e.getMessage());
                        }
                    } else {
                        File newFile = new File(javacPath.replaceAll(".exe", ".bat"));
                        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(newFile), Charset.forName("UTF-8"));
                        out.write(newJavac.getAbsolutePath() + " -J-Xms30m %*");
                        out.flush();
                        out.close();
                    }
                } catch (IOException ex) {
                    Logger.getLogger(JREInstr.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        if (!f.isDirectory()) {
            throw new EnvHandlingException("Specified JRE is not a directory: " + f.getPath());
        } else {
            // instrumenting JRE dir - check ${JRE}/lib/rt.jar, move ${JRE}/lib/rt.jar to ${JRE}/lib/rt.jar.bak (if not exist), add copy ${JRE}/lib/endorsed/${implant.jar}
            File lib = new File(f, "lib");
            if (!lib.exists()) {
                throw new EnvHandlingException("lib directory was not found in JRE directory");
            }

            toInstrument = new File(lib, "modules");
            if (!toInstrument.exists()) {
                toInstrument = new File(lib, "rt.jar");
                if (!toInstrument.exists()) {
                    throw new EnvHandlingException("rt.jar directory was not found in lib directory");
                }
                if (!toInstrument.isFile() || !toInstrument.canRead() || !toInstrument.canWrite()) {
                    throw new EnvHandlingException("Can't read/write rt.jar (or not a file)");
                }
                File bak = new File(lib, "rt.jar.bak");
                if (!bak.exists()) {
                    try {
                        Utils.copyFile(toInstrument, bak);
                    } catch (FileNotFoundException ex) {
                        throw new EnvHandlingException("Error while backuping rt.jar: file not found", ex);
                    } catch (IOException ex) {
                        throw new EnvHandlingException("Error while backuping rt.jar", ex);
                    }
                } else {
                    if (!envHandler.isSet(Instr.DSC_SUBSEQUENT)) {
                        throw new EnvHandlingException("Backup rt.jar.bak file exisit. It can mean that JRE is already instrumented - nothing to do. Restore initial rt.jar or delete bak file.");
                    }
                }

                File endorsed = new File(lib, "endorsed");
                if (!endorsed.exists()) {
                    endorsed.mkdir();
                } else {
                    if (!endorsed.isDirectory()) {
                        throw new EnvHandlingException("JRE/lib/endorsed is not a directory");
                    }
                }
                File implantcopy = new File(endorsed, implant.getName());
                try {
                    // copy rt to endorsed dir
                    Utils.copyFile(implant, implantcopy);

                    if (host != null || port != null) {
                        Properties prop = new Properties();
                        try {
                            if (host != null) {
                                prop.setProperty(JCovSESocketSaver.HOST_PROPERTIES_NAME, host);
                            }
                            if (port != null) {
                                prop.setProperty(JCovSESocketSaver.PORT_PROPERTIES_NAME, Integer.toString(port));
                            }
                            prop.store(new FileOutputStream(endorsed.getAbsolutePath() + File.separator + JCovSESocketSaver.NETWORK_DEF_PROPERTIES_FILENAME), null);

                        } catch (IOException ex) {
                            logger.log(Level.WARNING, "Cannot create property file to save host and port: {0}", ex);
                        }
                    }


                } catch (FileNotFoundException ex) {
                    throw new EnvHandlingException("Error while copying implant file to endorsed dir: file not found", ex);
                } catch (IOException ex) {
                    throw new EnvHandlingException("Error while copying implant file to endorsed dir", ex);
                }
            }
            else{
                toInstrument = new File(toInstrument, "bootmodules.jimage");
                if (!toInstrument.exists()) {
                    throw new EnvHandlingException("bootmodules.jimage was not found in modules directory");
                }
                if (!toInstrument.isFile() || !toInstrument.canRead() || !toInstrument.canWrite()) {
                    throw new EnvHandlingException("Can't read/write bootmodules.jimage");
                }
                File bak = new File(toInstrument.getParent(), "bootmodules.jimage.bak");
                if (!bak.exists()) {
                    try {
                        Utils.copyFile(toInstrument, bak);
                    } catch (FileNotFoundException ex) {
                        throw new EnvHandlingException("Error while backuping bootmodules.jimage: file not found", ex);
                    } catch (IOException ex) {
                        throw new EnvHandlingException("Error while backuping bootmodules.jimage", ex);
                    }
                } else {
                    if (!envHandler.isSet(Instr.DSC_SUBSEQUENT)) {
                        throw new EnvHandlingException("Backup bootmodules.jimage.bak file exisit. It can mean that JRE is already instrumented - nothing to do. Restore initial bootmodules.jimage or delete bak file.");
                    }
                }
            }
        }

        // check that java/lang/Shutdown is not excluded
        String[] excludes = com.sun.tdk.jcov.instrument.InstrumentationOptions.handleExclude(envHandler);
        String[] includes = com.sun.tdk.jcov.instrument.InstrumentationOptions.handleInclude(envHandler);
        Utils.Pattern pats[] = Utils.concatFilters(includes, excludes);

        callerInclude = envHandler.getValues(com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_CALLER_INCLUDE);
        callerExclude = envHandler.getValues(com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_CALLER_EXCLUDE);

        if (!Utils.accept(pats, null, "/java/lang/Shutdown", null)) {
            // Shutdown was excluded with some filtering mechanism. No need to remove it from excludes as inclusion has more priority
            logger.log(Level.WARNING, "java.lang.Shutdown automatically included to instrumentation (it can't be excluded in jreinstr)");
            if (includes.length > 0) { // if something else is already included - just including Shutdown
                includes = Utils.copyOf(includes, includes.length + 1);
                includes[includes.length - 1] = "/java/lang/Shutdown";
            } else {
                includes = new String[]{"/java/lang/Shutdown", "/*"};
            }
        }

        int ret = instr.handleEnv(envHandler);
        instr.setSave_end(new String[]{"java/lang/Shutdown.runHooks"});
        instr.setInclude(includes);
        instr.setExclude(excludes);

        instr.setCallerInclude(callerInclude);
        instr.setCallerExclude(callerExclude);

        return ret;
    }

    @Override
    protected String getDescr() {
        return "instrumenter designed for instumenting rt.jar";
    }

    @Override
    protected String usageString() {
        return "java -jar jcov.jar jreinstr -implantrt <runtime_to_implant> <jre_dir>";
    }

    @Override
    protected String exampleString() {
        return "To instrument JRE: \"java -jar jcov.jar jreinstr -implantrt jcov_j2se_rt.jar JDK1.7.0/jre\"";
    }
    public static final OptionDescr DSC_JAVAC_HACK = new OptionDescr("javac", "hack javac", OptionDescr.VAL_SINGLE, "Hack javac to increase minimum VM memory used on initialization. Should be used on Solaris platform or should be done manually. ");
    public static final OptionDescr DCS_ADD_JAR = new OptionDescr("addjar", new String[]{"add"}, "instrument additional jars", OptionDescr.VAL_MULTI, "Instrument additional jars within JRE or JDK. Only jar files are allowed.");
    public static final OptionDescr DCS_ADD_JIMAGE = new OptionDescr("addjimage", new String[]{"addjimage"}, "instrument additional jimages", OptionDescr.VAL_MULTI, "Instrument additional jimages within JRE or JDK. Only jimage files are allowed.");
    public static final OptionDescr DCS_ADD_TESTS = new OptionDescr("addtests", new String[]{"tests"}, "instrument tests", OptionDescr.VAL_MULTI, "Instrument tests files (classes and jars).");
    final static OptionDescr DSC_HOST =
            new OptionDescr("host", new String[]{"host"}, "sets default host", OptionDescr.VAL_SINGLE, "set the default host for sending jcov data. needed only in network mode");
    final static OptionDescr DSC_PORT =
            new OptionDescr("port", new String[]{"port"}, "sets default port", OptionDescr.VAL_SINGLE, "set the default port for sending jcov data. needed only in network mode");
}
