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

import com.sun.tdk.jcov.runtime.PropertyFinder;
import com.sun.tdk.jcov.util.Utils;
import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.FileHandler;

/**
 *
 * @author Andrey Titov
 */
public abstract class JCovCMDTool extends JCovTool {

    /**
     * CLI entry point
     *
     * @param args tool arguments (without tool name)
     * @return exit code
     */
    public final int run(String[] args) {

        // This method manages CLI handling for all the tools except Agent tool.
        // If any change is performed here - check Agent CLI handling logic.

        EnvHandler handler = defineHandler();

        if (readPlugins) {
            String pluginsDir = handler.getValue(EnvHandler.PLUGINDIR);
            File file = new File(pluginsDir);
            if (file.isDirectory() && file.canRead()) {
                File[] list = file.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".jar");
                    }
                });
                URL urls[] = new URL[list.length];
                for (int i = 0; i < list.length; ++i) {
                    try {
                        urls[i] = list[i].toURI().toURL();
                    } catch (MalformedURLException ignored) {
                    }
                }
                URLClassLoader urlCL = new URLClassLoader(urls, ClassLoader.getSystemClassLoader());
                handler.setClassLoader(urlCL);
            }
        }

        try {

            handler.parseCLIArgs(args);

            if (handler.isSet(EnvHandler.HELP)) {
                handler.usage();
                return SUCCESS_EXIT_CODE;
            }

            if (handler.isSet(EnvHandler.HELP_VERBOSE)) {
                handler.usage(true);
                return SUCCESS_EXIT_CODE;
            }
        } catch (EnvHandler.CLParsingException ex) {
            // printing help on error only with -h or -hv options (and their aliases)
            if (handler.isSet(EnvHandler.HELP)) {
                handler.usage();
                handler.getOut().println("\n Command line error: " + ex.getMessage() + "\n");
                return ERROR_CMDLINE_EXIT_CODE;
            }

            if (handler.isSet(EnvHandler.HELP_VERBOSE)) {
                handler.usage(true);
                handler.getOut().println("\n Command line error: " + ex.getMessage() + "\n");
                return ERROR_CMDLINE_EXIT_CODE;
            }

            handler.getOut().println(" Command line error: " + ex.getMessage() + "\n");
            String name = this.getClass().getName();
            handler.getOut().println("Use \"java -jar jcov.jar " + name.substring(name.lastIndexOf(".") + 1) + " -h\" for command-line help or \"java -jar jcov.jar " + name.substring(name.lastIndexOf(".") + 1) + " -hv\" for wider description");
            return ERROR_CMDLINE_EXIT_CODE;
        }

        if (handler.isSet(EnvHandler.LOGFILE)) {
            try {
                FileHandler fh = new FileHandler(handler.getValue(EnvHandler.LOGFILE));
                Utils.setLoggerHandler(fh);
            } catch (Exception ex) {
                handler.getOut().println("\n Error initializing logger: " + ex.getMessage() + "\n");
            }
        }

        if (handler.isSet(EnvHandler.LOGLEVEL)) {
            Utils.setLoggingLevel(handler.getValue(EnvHandler.LOGLEVEL));
        }

        // handle environment on SPIs
        try {
            handler.initializeSPIs();
        } catch (Exception ex) {
            handler.getOut().println("Service Provider initialization error: " + ex.getMessage() + "\n");
            if (handler.isSet(EnvHandler.PRINT_ENV)) {
                handler.printEnv();
            } else {
                handler.getOut().println("Use -print-env option to find where this Service Provider is set\n");
            }
            return ERROR_CMDLINE_EXIT_CODE;
        }

        // give environment to the tool
        int res = SUCCESS_EXIT_CODE;
        try {
            res = handleEnv(handler);
        } catch (EnvHandlingException ex) {
            handler.getOut().println("Command line error: " + ex.getMessage() + "\n");
            String name = this.getClass().getName();
            handler.getOut().println("Use \"java -jar jcov.jar " + name.substring(name.lastIndexOf(".") + 1) + " -h\" for command-line help or \"java -jar jcov.jar " + name.substring(name.lastIndexOf(".") + 1) + " -hv\" for wider description");
            if (handler.isSet(EnvHandler.PRINT_ENV)) {
                handler.printEnv();
            }
            return ERROR_CMDLINE_EXIT_CODE;
        }
        if (handler.isSet(EnvHandler.PRINT_ENV)) {
            handler.printEnv();
            return SUCCESS_EXIT_CODE;
        }
        if (res != SUCCESS_EXIT_CODE) {
            handler.usage();
            return res;
        }

        // run tool execution
        try {
            return run();
        } catch (Exception e) {
            System.out.println("Execution error: " + e.getMessage());
            if (PropertyFinder.findValue("stacktrace", "false").equals("true")) {
                e.printStackTrace(System.out);
            }
            return ERROR_EXEC_EXIT_CODE;
        }
    }

    protected abstract int run() throws Exception;
}
