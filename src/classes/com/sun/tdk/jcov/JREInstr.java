/*
 * Copyright (c) 2014, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tdk.jcov.instrument.InstrumentationOptions;
import com.sun.tdk.jcov.instrument.InstrumentationParams;
import com.sun.tdk.jcov.instrument.InstrumentationPlugin;
import com.sun.tdk.jcov.runtime.JCovSESocketSaver;
import com.sun.tdk.jcov.tools.EnvHandler;
import com.sun.tdk.jcov.tools.JCovCMDTool;
import com.sun.tdk.jcov.tools.OptionDescr;
import com.sun.tdk.jcov.util.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sun.tdk.jcov.Instr.DSC_INCLUDE_RT;
import static com.sun.tdk.jcov.Instr.DSC_VERBOSE;
import static com.sun.tdk.jcov.instrument.InstrumentationOptions.*;
import static com.sun.tdk.jcov.instrument.InstrumentationPlugin.MODULE_INFO_CLASS;
import static com.sun.tdk.jcov.instrument.InstrumentationPlugin.TEMPLATE_ARTIFACT;
import static com.sun.tdk.jcov.util.Utils.CheckOptions.*;
import static com.sun.tdk.jcov.util.Utils.getListFiles;

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

    private File toInstrument;
//    private File[] addJars;
//    private File[] addJimages;
//    private File[] addTests;
    private File implant;
    private static final Logger logger;
    private String host = null;
    private Integer port = null;
    private InstrumentationPlugin plugin;
    private InstrumentationParams params;
    private String template = "template.xml";
    private String[] m_excludes;
    private String[] m_includes;
    private String include_rt;

    static {
        Utils.initLogger();
        logger = Logger.getLogger(Instr.class.getName());
    }



    @Override
    protected int run() throws Exception {
        final String[] toInstr = new String[]{toInstrument.getAbsolutePath()};
        Utils.addToClasspath(toInstr);

        if (toInstrument.getName().equals("jmods")) {
            logger.log(Level.INFO, "working with jmods");
            File jdk = new File(toInstrument.getParentFile(), "jdk");
            if (!jdk.exists()) {
                jdk = toInstrument.getParentFile();
            }
            File jmodsTemp = new File(toInstrument.getParentFile(), "jmod_temp");

            ArrayList<URL> urls = new ArrayList<>();
            for (File mod : getListFiles(toInstrument)) {
                File jmodDir = extractJMod(jdk, mod.getAbsoluteFile(), jmodsTemp);
                urls.add(new File(jmodDir, "classes").toURI().toURL());
            }
            urls.add(toInstrument.toURI().toURL());

            ClassLoader cl = new InstrumentationPlugin.OverridingClassLoader(urls.toArray(new URL[0]),
                    ClassLoader.getSystemClassLoader());

            if(plugin == null) plugin = InstrumentationPlugin.getPlugin();
            InstrumentationPlugin.ModuleInstrumentation mi = new InstrumentationPlugin.ModuleInstrumentation(
                    new InstrumentationPlugin.FilteringPlugin(plugin, InstrumentationPlugin.classNameFilter(params)),
                    (InstrumentationPlugin.ModuleInstrumentationPlugin) plugin) {
                public void proccessModule(byte[] moduleInfo, ClassLoader loader,
                                           BiConsumer<String, byte[]> destination) throws Exception {
                    InstrumentationPlugin.ModuleInstrumentationPlugin mip = getModulePluign();
                    if(mip.getModuleName(moduleInfo).equals("java.base")) {
                        moduleInfo = mip.addExports(List.of("com/sun/tdk/jcov/runtime"), moduleInfo, loader);
                        moduleInfo = mip.clearHashes(moduleInfo, loader);
                        InstrumentationPlugin.PathSource implantSource =
                                new InstrumentationPlugin.PathSource(cl, implant.toPath());
                        for (String resource : implantSource.resources()) {
                            destination.accept(resource, implantSource.loader().getResourceAsStream(resource).readAllBytes());
                        }
                    }
                    destination.accept(MODULE_INFO_CLASS, moduleInfo);
                }
            };

            try {
                for (File mod : getListFiles(jmodsTemp)) {
                    if (mod != null && mod.isDirectory()) {
                        String moduleName = mod.getName();
                        if (isModuleIncluded(moduleName)) {
                            logger.log(Level.INFO, "Instrumenting " + moduleName);
                            File modClasses = new File(mod, "classes");
                            mi.instrument(new InstrumentationPlugin.PathSource(cl, modClasses.toPath()),
                                    new InstrumentationPlugin.PathDestination(modClasses.toPath()), params);
                        }
                        createJMod(mod, jdk, implant.getAbsolutePath(), null);
                    }
                }
                File newJdkDir = runJLink(jmodsTemp, jdk);
                if (newJdkDir != null) {
                    String jimage_path = File.separator + "lib" + File.separator + "modules" + File.separator + "bootmodules.jimage";
                    File orig_jimage = new File(jdk.getCanonicalPath() + jimage_path);
                    File instr_jimage = new File(newJdkDir.getCanonicalPath() + jimage_path);

                    if (!orig_jimage.exists()) {
                        jimage_path = File.separator + "lib" + File.separator + "modules";
                        orig_jimage = new File(jdk.getCanonicalPath() + jimage_path);
                        instr_jimage = new File(newJdkDir.getCanonicalPath() + jimage_path);
                    }
                    Utils.copyFile(orig_jimage, new File(orig_jimage.getParent(), orig_jimage.getName() + ".bak"));

                    if (!orig_jimage.delete()) {
                        Utils.copyFile(instr_jimage, new File(orig_jimage.getParent(), orig_jimage.getName() + ".instr"));
                        logger.log(Level.SEVERE, "please, delete original jimage manually: " + orig_jimage);
                    } else {
                        Utils.copyFile(instr_jimage, orig_jimage);
                    }
                    if (!Utils.deleteDirectory(newJdkDir)) {
                        logger.log(Level.SEVERE, "please, delete " + newJdkDir + " new instumented jdk dir manually");
                    }
                    if (!Utils.deleteDirectory(jmodsTemp)) {
                        logger.log(Level.SEVERE, "please, delete " + jmodsTemp + " temp jmod dir manually");
                    }

                } else {
                    logger.log(Level.SEVERE, "failed to link modules. Please, run jlink for " + jmodsTemp + " temp jmod dir manually");
                }
            } catch (Exception e) {
                e.printStackTrace();
                logger.log(Level.SEVERE, "exception while creating mods, e = " + e);
            }
        } else if (toInstrument.getAbsolutePath().endsWith("bootmodules.jimage")) {
            throw new RuntimeException("This functionality has not yet been implemented");
        } else {
            throw new RuntimeException("This functionality has not yet been implemented");
        }

        //see a comment in handleEnv(EnvHandler)
//        ArrayList<String> srcs = null;
//        if (addJars != null) {
//            srcs = new ArrayList<>();
//            for (File addJar : addJars) {
//                srcs.add(addJar.getAbsolutePath());
//            }
//        }
//
//        if (srcs != null) {
//            Utils.addToClasspath(srcs.toArray(new String[0]));
//            instr.instrumentFiles(srcs.toArray(new String[0]), null, null);
//        }
//        if (addTests != null) {
//            ArrayList<String> tests = new ArrayList<>();
//            for (File addTest : addTests) {
//                tests.add(addTest.getAbsolutePath());
//            }
//            instr.instrumentTests(tests.toArray(new String[0]), null, null);
//        }

        Consumer<OutputStream> tmplGen = plugin.complete().get(TEMPLATE_ARTIFACT);
        if (tmplGen != null) tmplGen.accept(Files.newOutputStream(Path.of(template)));

        return SUCCESS_EXIT_CODE;
    }

    private boolean doCommand(String command, File where, String msg) throws IOException, InterruptedException {
        Objects.requireNonNull(command);
        Objects.requireNonNull(msg);
        boolean success = false;
        StringBuilder sb = new StringBuilder(msg + command);
        ProcessBuilder pb = new ProcessBuilder(command.split("\\s+")).directory(where).redirectErrorStream(true);
        Process p = null;
        try {
            p = pb.start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String s;
                while ((s = br.readLine()) != null) {
                    sb.append(System.lineSeparator()).append('\t').append(s);
                }
                success = p.waitFor() == 0;
            }
            if (!success) {
                logger.log(Level.SEVERE, sb.toString());
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
        return success;
    }

    private File extractJMod(File jdk, File from, File to) {
        try {
            String name = from.getName();
            if (name.contains(".jmod")) {
                name = name.substring(0, name.indexOf(".jmod"));
            }
            File modDir = new File(to, name);
            modDir.mkdirs();
            String command = jdk.getAbsolutePath() + File.separator + "bin" + File.separator + "jar xf " +
                    from.getAbsolutePath();
            if (!doCommand(command, modDir, "wrong command for unjar jmod: ")) {
                return null;
            }
            return modDir;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "exception in process(unjar jmod)", e);
            return null;
        }
    }

    private File runJLink(File jmodDir, File jdk) {
        try {

            String command = jdk.getAbsolutePath() + File.separator + "bin" + File.separator + "jlink --module-path " +
                    jmodDir.getCanonicalPath() + " --add-modules ";

            StringBuilder sb = new StringBuilder();
            for (File subDir : getListFiles(jmodDir)) {
                if (subDir.isDirectory()) {
                    sb.append(subDir.getName()).append(",");
                }
            }
            String mods = sb.substring(0, sb.toString().length() - 1);
            command += mods + " --output instr_jimage_dir";
            if (!doCommand(command, jmodDir.getParentFile(), "wrong command for jlink mods: ")) {
                return null;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "exception in process(jlink mods)", e);
            return null;
        }

        return new File(jmodDir.getParentFile(), "instr_jimage_dir");
    }

    private boolean isModuleIncluded(String moduleName) {
        return (m_includes.length == 0 || Arrays.stream(m_includes).anyMatch(i -> moduleName.matches(i))) &&
               Arrays.stream(m_excludes).noneMatch(i -> moduleName.matches(i));
    }
    private void createJMod(File jmodDir, File jdk, String rt_path, String pluginPath) {
        try {
            File modsDir = jmodDir.getParentFile();
            StringBuilder command = new StringBuilder();
            command.append(jdk.getAbsolutePath()).append(File.separator).append("bin").append(File.separator).
                    append("jmod create ").
                    append("--module-path ").append(modsDir.getCanonicalPath()).append(" ");

            for (File subDir : getListFiles(jmodDir)) {
                if (subDir.getName().equals("classes")) {
                    if ("java.base".equals(jmodDir.getName())) {
                        command.append("--class-path " + rt_path);
                        if(pluginPath != null) {
                            command.append(File.pathSeparator + pluginPath);
                        }
                        command.append(File.pathSeparator + jmodDir.getName() + File.separator + "classes ");
                    } else {
                        command.append("--class-path " + jmodDir.getName() + File.separator + "classes ");
                    }
                }
                if (subDir.getName().equals("bin")) {
                    command.append("--cmds " + jmodDir.getName() + File.separator + "bin ");
                }
                if (subDir.getName().equals("conf")) {
                    command.append("--config " + jmodDir.getName() + File.separator + "conf ");
                }
                if (subDir.getName().equals("lib")) {
                    command.append("--libs " + jmodDir.getName() + File.separator + "lib ");
                }
                if (subDir.getName().equals("include")) {
                    command.append("--header-files " + jmodDir.getName() + File.separator + "include ");
                }
                if (subDir.getName().equals("legal")) {
                    command.append("--legal-notices " + jmodDir.getName() + File.separator + "legal ");
                }
                if (subDir.getName().equals("man")) {
                    command.append("--man-pages " + jmodDir.getName() + File.separator + "man ");
                }
            }
            command.append(" " + jmodDir.getName() + ".jmod");
            doCommand(command.toString(),modsDir,"wrong command for create jmod: ");
        } catch (Exception e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "exception in process(create jmod)", e);
        }
    }

    private void expandJimage(File jimage, String tempDirName) {
        try {
            String command = jimage.getParentFile().getParentFile().getParent() + File.separator + "bin" + File.separator +
                    "jimage extract --dir " + jimage.getParent() + File.separator +
                    tempDirName + " " + jimage.getAbsolutePath();
            doCommand(command, null, "wrong command for expand jimage: ");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "exception in process(expanding jimage)", e);
        }
    }

    private boolean createJimage(File dir, String new_jimage_path) {
        try {
            String command = dir.getParentFile().getParentFile().getParent() + File.separator + "bin" + File.separator +
                    "jimage recreate --dir " + dir + " " + new_jimage_path;
            return doCommand(command, null, "wrong command for create jimage: ");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "exception in process(expanding jimage)", e);
            return false;
        }
    }

    @Override
    protected EnvHandler defineHandler() {
        DSC_INCLUDE_RT.usage = "To run instrumented JRE you should implant JCov runtime library both into rt.jar " +
                "and into 'lib/endorsed' directory.\nWhen instrumenting whole JRE dir with jreinstr tool - " +
                "these 2 actions will be done automatically.";

        return new EnvHandler(new OptionDescr[]{
                DSC_INCLUDE_RT,
                DSC_VERBOSE,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_TYPE,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_INCLUDE,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_INCLUDE_LIST,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_EXCLUDE,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_EXCLUDE_LIST,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_CALLER_INCLUDE,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_CALLER_EXCLUDE,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_MINCLUDE,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_MEXCLUDE,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_MINCLUDE_LIST,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_MEXCLUDE_LIST,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_TEMPLATE,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_ABSTRACT,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_NATIVE,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_FIELD,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_SYNTHETIC,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_ANONYM,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_INNERINVOCATION,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_INNER_INCLUDE,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_INNER_EXCLUDE,
                com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_INSTR_PLUGIN,
                Instr.DSC_SUBSEQUENT,
                DSC_JAVAC_HACK,
                //See comment in handleEnv(EnvHandler)
//                DCS_ADD_JAR,
//                DCS_ADD_JIMAGE,
//                DCS_ADD_TESTS,
                DSC_HOST,
                DSC_PORT
        }, this);
    }

    @Override
    protected int handleEnv(EnvHandler envHandler) throws EnvHandlingException {

        params = new InstrumentationParams();

        String[] tail = envHandler.getTail();
        if (tail == null || tail.length == 0) {
            throw new EnvHandlingException("JRE dir is not specified");
        }
        if (tail.length > 1) {
            logger.log(Level.WARNING, "Only first argument ({0}) will be used", tail[0]);
        }

        if (!envHandler.isSet(DSC_INCLUDE_RT)) {
            throw new EnvHandlingException("Runtime should be always implanted when instrumenting rt.jar (e.g. '-rt jcov_j2se_rt.jar')");
        }

        implant = new File(envHandler.getValue(DSC_INCLUDE_RT));
        Utils.checkFile(implant, "JCovRT library jarfile", FILE_ISFILE, FILE_EXISTS, FILE_CANREAD);

        //It is currently unclear if there are solid usecases for instrumenting any additional bytecode together
        //with the JDK. That applies to additional jars, sources and also test. If anything else need to be
        //instrumented, a separate instrumentation call could be used.
        if (envHandler.isSet(DCS_ADD_JAR)) {
            throw new UnsupportedOperationException("Instrumenting additional code together with JDK is not supported.");
//            String[] jars = envHandler.getValues(DCS_ADD_JAR);
//            addJars = new File[jars.length];
//            for (int i = 0; i < addJars.length; ++i) {
//                addJars[i] = new File(jars[i]);
//                if (!addJars[i].exists()) {
//                    throw new EnvHandlingException("Additional jar " + jars[i] + " doesn't exist");
//                }
//                if (!addJars[i].canRead()) {
//                    throw new EnvHandlingException("Can't read additional jar " + jars[i]);
//                }
//            }
        }
        if (envHandler.isSet(DCS_ADD_JIMAGE)) {
            throw new UnsupportedOperationException("Instrumenting additional code together with JDK is not supported.");
//            String[] images = envHandler.getValues(DCS_ADD_JIMAGE);
//            addJimages = new File[images.length];
//            for (int i = 0; i < addJimages.length; ++i) {
//                addJimages[i] = new File(images[i]);
//                if (!addJimages[i].exists()) {
//                    throw new EnvHandlingException("Additional jimage " + images[i] + " doesn't exist");
//                }
//                if (!addJimages[i].canRead()) {
//                    throw new EnvHandlingException("Can't read additional jimage " + images[i]);
//                }
//            }
        }

        if (envHandler.isSet(DCS_ADD_TESTS)) {
            throw new UnsupportedOperationException("Instrumenting additional code together with JDK is not supported.");
//            String[] files = envHandler.getValues(DCS_ADD_TESTS);
//            addTests = new File[files.length];
//            for (int i = 0; i < addTests.length; ++i) {
//                addTests[i] = new File(files[i]);
//                if (!addTests[i].exists()) {
//                    throw new EnvHandlingException("Test file " + files[i] + " doesn't exist");
//                }
//                if (!addTests[i].canRead()) {
//                    throw new EnvHandlingException("Can't read test file " + files[i]);
//                }
//            }
        }

        File f = new File(tail[0]);
        Utils.checkFile(f, "JRE directory", FILE_EXISTS, FILE_ISDIR, FILE_CANREAD);

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
            File javac = new File(javacPath);
            File newJavac;

            boolean isWin = false;
            if (javacPath.endsWith(".exe") && System.getProperty("os.name").toLowerCase().contains("win")) {
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
                            OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(newFile), StandardCharsets.UTF_8);
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
                            OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(newFile), StandardCharsets.UTF_8);
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
                    throw new EnvHandlingException("Can't move specified javac to new location" +
                            " (" + javacPath + " to " + newJavac.getPath() + ")");
                }
                try {
                    if (!isWin) {
                        File newFile = new File(javacPath);
                        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(newFile), StandardCharsets.UTF_8);
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
                        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(newFile), StandardCharsets.UTF_8);
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
            // instrumenting JRE dir - check ${JRE}/lib/rt.jar, move ${JRE}/lib/rt.jar to ${JRE}/lib/rt.jar.bak (if not exist),
            // add copy ${JRE}/lib/endorsed/${implant.jar}
            File lib = new File(f, "lib");
            if (!lib.exists()) {
                throw new EnvHandlingException("lib directory was not found in JRE directory");
            }

            String template = envHandler.getValue(InstrumentationOptions.DSC_TEMPLATE);
            if (template != null) {
                setTemplate(template);
            }

            File mods = new File(f.getParentFile(), "jmods");
            if (mods.exists()) {
                toInstrument = mods;
                createPropertyFile(f);
            }
            else if (new File(f, "jmods").exists()){
                toInstrument = new File(f, "jmods");
                createPropertyFile(toInstrument.getParentFile());
            }
            else {

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
                            throw new EnvHandlingException("Backup rt.jar.bak file exists.\n" +
                                    "It can mean that JRE is already instrumented - nothing to do.\n" +
                                    "Restore initial rt.jar or delete bak file.");
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
                        createPropertyFile(endorsed);

                    } catch (FileNotFoundException ex) {
                        throw new EnvHandlingException("Error while copying implant file to endorsed dir: file not found", ex);
                    } catch (IOException ex) {
                        throw new EnvHandlingException("Error while copying implant file to endorsed dir", ex);
                    }
                } else {
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
                            throw new EnvHandlingException("Error while backing up bootmodules.jimage: file not found", ex);
                        } catch (IOException ex) {
                            throw new EnvHandlingException("Error while backing up bootmodules.jimage", ex);
                        }
                    } else {
                        if (!envHandler.isSet(Instr.DSC_SUBSEQUENT)) {
                            throw new EnvHandlingException("Backup bootmodules.jimage.bak file exists.\n" +
                                    "It can mean that JRE is already instrumented - nothing to do.\n" +
                                    "Restore initial bootmodules.jimage or delete bak file.");
                        }
                    }
                }
            }
        }

        // check that java/lang/Shutdown is not excluded
        String[] excludes = com.sun.tdk.jcov.instrument.InstrumentationOptions.handleExclude(envHandler);
        String[] includes = com.sun.tdk.jcov.instrument.InstrumentationOptions.handleInclude(envHandler);
        Utils.Pattern pats[] = Utils.concatFilters(includes, excludes);

        m_excludes = com.sun.tdk.jcov.instrument.InstrumentationOptions.handleMExclude(envHandler);
        m_includes = com.sun.tdk.jcov.instrument.InstrumentationOptions.handleMInclude(envHandler);

        String[] callerInclude = envHandler.getValues(InstrumentationOptions.DSC_CALLER_INCLUDE);
        String[] callerExclude = envHandler.getValues(InstrumentationOptions.DSC_CALLER_EXCLUDE);

        String[] innerInclude = InstrumentationOptions.handleInnerInclude(envHandler);
        String[] innerExclude = InstrumentationOptions.handleInnerExclude(envHandler);

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
        if (!Utils.accept(pats, null, "/java/lang/invoke/LambdaForm", null)) {
            logger.log(Level.WARNING, "java.lang.invoke.LambdaForm automatically included to instrumentation (it can't be excluded in jreinstr)");
            if (includes.length > 0) { // if something else is already included - just including Shutdown
                includes = Utils.copyOf(includes, includes.length + 1);
                includes[includes.length - 1] = "/java/lang/invoke/LambdaForm";
            } else {
                includes = new String[]{"/java/lang/invoke/LambdaForm", "/*"};
            }
        }

        if (envHandler.isSet(DSC_VERBOSE)) {
            logger.setLevel(Level.INFO);
        }

        params.setSavesBegin(envHandler.getValues(DSC_SAVE_BEGIN));
        params.setSavesEnd(envHandler.getValues(DSC_SAVE_AT_END));

        String abstractValue = envHandler.getValue(DSC_ABSTRACT);
        if (abstractValue.equals("off")) {
            params.setInstrumentAbstract(ABSTRACTMODE.NONE);
        } else if (abstractValue.equals("on")) {
            params.setInstrumentAbstract(ABSTRACTMODE.DIRECT);
        } else {
            throw new EnvHandlingException("'" + DSC_ABSTRACT.name +
                    "' parameter value error: expected 'on' or 'off'; found: '" + abstractValue + "'");
        }

        String nativeValue = envHandler.getValue(DSC_NATIVE);
        if (nativeValue.equals("on")) {
            params.setInstrumentNative(true);
        } else if (nativeValue.equals("off")) {
            params.setInstrumentNative(false);
        } else {
            throw new EnvHandlingException("'" + DSC_NATIVE.name +
                    "' parameter value error: expected 'on' or 'off'; found: '" + nativeValue + "'");
        }

        String fieldValue = envHandler.getValue(DSC_FIELD);
        if (fieldValue.equals("on")) {
            params.setInstrumentFields(true);
        } else if (fieldValue.equals("off")) {
            params.setInstrumentFields(false);
        } else {
            // can't happen - check is in EnvHandler
            throw new EnvHandlingException("'" + DSC_FIELD.name +
                    "' parameter value error: expected 'on' or 'off'; found: '" + fieldValue + "'");
        }

        params.setInstrumentAnonymous(envHandler.getValue(DSC_ANONYM).equals("on"));

        params.setInstrumentSynthetic(envHandler.getValue(DSC_SYNTHETIC).equals("on"));

        params.setInnerInvocations(envHandler.getValue(DSC_INNERINVOCATION).equals("off"));

        params.setCallerIncludes(callerInclude);
        params.setCallerExcludes(callerExclude);

        params.setMode(InstrumentationMode.fromString(envHandler.getValue(DSC_TYPE)));

        template = envHandler.getValue(DSC_TEMPLATE);
        Utils.checkFileNotNull(template, "template filename", FILE_NOTISDIR, FILE_PARENTEXISTS);

        include_rt = envHandler.getValue(DSC_INCLUDE_RT);
        Utils.checkFileCanBeNull(include_rt, "JCovRT library jarfile", FILE_EXISTS, FILE_ISFILE, FILE_CANREAD);

        params.setSavesEnd(new String[]{"java/lang/Shutdown.runHooks"});

        params.setIncludes(includes);
        params.setExcludes(excludes);

        params.setInnerIncludes(innerInclude);
        params.setInnerExcludes(innerExclude);

        try {
            String pluginClass = envHandler.getValue(DSC_INSTR_PLUGIN);
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

    private void setTemplate(String template) {
        this.template = template;
    }

    private void createPropertyFile(File dir){
        if (host != null || port != null) {
            Properties prop = new Properties();
            try {
                if (host != null) {
                    prop.setProperty(JCovSESocketSaver.HOST_PROPERTIES_NAME, host);
                }
                if (port != null) {
                    prop.setProperty(JCovSESocketSaver.PORT_PROPERTIES_NAME, Integer.toString(port));
                }
                prop.store(new FileOutputStream(dir.getAbsolutePath() + File.separator + JCovSESocketSaver.NETWORK_DEF_PROPERTIES_FILENAME), null);

            } catch (IOException ex) {
                logger.log(Level.WARNING, "Cannot create property file to save host and port: {0}", ex);
            }
        }
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
