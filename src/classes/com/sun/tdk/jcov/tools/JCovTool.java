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
package com.sun.tdk.jcov.tools;

import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * @author Dmitry Fazunenko
 * @author Alexey Fedorchenko
 */
public abstract class JCovTool {

    public static class EnvHandlingException extends Exception {

        public EnvHandlingException(String message) {
            super(message);
        }

        public EnvHandlingException(String message, Throwable cause) {
            super(message, cause);
        }

        public EnvHandlingException() {
        }
    }
    public static final int SUCCESS_EXIT_CODE = 0;
    public static final int ERROR_CMDLINE_EXIT_CODE = 1;
    public static final int ERROR_EXEC_EXIT_CODE = 2;
    private static HashMap<String, Class> spis;
//    public static OptionDescr[] VALID_OPTIONS;
    protected boolean readPlugins = false;

    protected JCovTool() {
    }

    protected abstract EnvHandler defineHandler();

    protected abstract int handleEnv(EnvHandler envHandler) throws EnvHandlingException;

    protected abstract String getDescr();

    protected boolean isMainClassProvided() {
        return true;
    }

    protected abstract String usageString();

    protected abstract String exampleString();

    protected final void registerSPI(String envname, Class classname) {
        if (spis == null) {
            spis = new HashMap<String, Class>();
        }
        spis.put(envname, classname);
    }
    private PrintStream out = System.out;

    public void setOut(PrintStream out) {
        this.out = out;
    }

    public PrintStream getOut() {
        return out;
    }
    private static final String[] allTools = {
        "com.sun.tdk.jcov.Exec",
        "com.sun.tdk.jcov.Agent",
        "com.sun.tdk.jcov.Instr",
        "com.sun.tdk.jcov.JREInstr",
        "com.sun.tdk.jcov.ProductInstr",
        "com.sun.tdk.jcov.Instr2",
        "com.sun.tdk.jcov.TmplGen",
        "com.sun.tdk.jcov.Grabber",
        "com.sun.tdk.jcov.GrabberManager",
        "com.sun.tdk.jcov.Merger",
        "com.sun.tdk.jcov.RepMerge",
        "com.sun.tdk.jcov.Filter",
        "com.sun.tdk.jcov.DiffCoverage",
        "com.sun.tdk.jcov.RepGen",
        "com.sun.tdk.jcov.JCov",
        "com.sun.tdk.jcov.IssueCoverage"
    };
    public static final List<String> allToolsList = Collections.unmodifiableList(Arrays.asList(allTools));

    /**
     * Prints help about all registered tools in <b>allTools</b> to standard
     * output
     *
     * @see #allTools
     */
    public static void printHelp() {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(System.out, Charset.defaultCharset()), true);
        writer.println("Java Code Coverage Tool ver." + JcovVersion.getJcovVersion());
        writer.println("Usage: 'java -jar jcov.jar <Name>' or 'java -cp jcov.jar com.sun.tdk.jcov.<Name>'");
        writer.println("JCov includes the following components:");
        writer.println();
        for (String str : allTools) {
            Class c = null;
            JCovTool h = null;
            Object o = null;
            try {
                c = Class.forName(str);
                o = c.newInstance();
                h = (JCovTool) o;
            } catch (NoClassDefFoundError cndfe){
                if ("com.sun.tdk.jcov.IssueCoverage".equals(str)){
                    System.out.println("IssueCoverage command request jdk9 or javax.tools in classpath");
                    continue;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(1);
            }
//            h.printDescr(writer);
//            if (h.isMainClassProvided()) {
            String name = c.getName();
//            writer.println("\t" + str.substring(name.lastIndexOf(".") + 1) + "\t\t" + h.getDescr());
            writer.println(String.format("   %-20s%s", str.substring(name.lastIndexOf(".") + 1), h.getDescr()));
//            } else {
//                h.printDescr(writer);
//            }

        }
        writer.println();
        writer.println("Use \"java -jar jcov.jar <Name> -help\" for command-line help on each component, or \"java -jar jcov.jar <Name> -help-verbose\" for wider description");
    }

    /**
     * Prints help by tool`s classname
     *
     * @param toolClass tool to load
     * @param args checks whether -help-verbose was mentioned
     */
    public static void printHelp(JCovTool toolClassObject, String[] args) {
        try {
            JCovTool d = toolClassObject;

            for (int i = 0; i < args.length; i++) {
                if (args[i].endsWith(EnvHandler.HELP_VERBOSE.name)) {
                    d.defineHandler().usage(true);
                    return;
                }
            }

            d.defineHandler().usage(false);
        } catch (Exception e) {
            System.out.println("INTERNAL ERROR! " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
