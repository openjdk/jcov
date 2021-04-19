/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.jcov.report.javap;

import com.sun.tools.javap.Main;
import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * This class is used to run javap on class files
 *
 * @author Alexey Fedorchenko
 */
public class JavapClassReader {

    private static URLClassLoader classLoader = null;
    private static Method method;
    private static Object instance;
    private static File toolsJar;

    public static void read(String filePath, String jarPath, PrintWriter pw) {

        // Note: if the RepGen is being started by JDK 9 and above then
        // the option "--add-exports jdk.jdeps/com.sun.tools.javap=ALL-UNNAMED" should be added to the JVM command-line.
        try {
            if (jarPath == null) {
                Main.run(new String[]{"-c", "-p", filePath}, pw);
            } else {
                Main.run(new String[]{"-c", "-p", "-classpath", jarPath, filePath}, pw);
            }
        } catch (NoClassDefFoundError error) {

            if (classLoader == null) {

                File javaHome = new File(System.getProperty("java.home"));

                if (javaHome.getName().equals("jre")) {
                    javaHome = javaHome.getParentFile();
                    toolsJar = new File(new File(javaHome, "lib"), "tools.jar");

                    if (toolsJar.exists()) {

                        try {
                            classLoader = new URLClassLoader(new URL[]{toolsJar.toURI().toURL()}, ClassLoader.getSystemClassLoader());
                            Class classToLoad = Class.forName("com.sun.tools.javap.Main", true, classLoader);
                            method = classToLoad.getDeclaredMethod("run", String[].class, PrintWriter.class);
                            instance = classToLoad.getDeclaredConstructor().newInstance();

                            String[] params;

                            if (jarPath == null) {
                                params = new String[]{"-c", "-p", filePath};
                            } else {
                                params = new String[]{"-c", "-p", "-classpath", jarPath, filePath};
                            }
                            try {
                                Object result = method.invoke(instance, params, pw);
                            } catch (Exception ex) {
                                printToolsJarError();
                            }

                        } catch (Exception e) {
                            printToolsJarError();
                        }


                    } else {
                        printToolsJarError();
                    }

                } else {
                    System.err.println("cannot execute javap, perhaps jdk8/lib/tools.jar is missing from the classpath");
                    System.err.println("example: java -cp jcov.jar:tools.jar com.sun.tdk.jcov.RepGen -javap path_to_classes -o path_to_javap_output path_to_result.xml");
                    return;
                }
            } else {

                String[] params;

                if (jarPath == null) {
                    params = new String[]{"-c", "-p", filePath};
                } else {
                    params = new String[]{"-c", "-p", "-classpath", jarPath, filePath};
                }
                try {
                    method.invoke(instance, params, pw);
                } catch (Exception ex) {
                    printToolsJarError();
                }
            }
        }
    }

    private static void printToolsJarError() {
        System.err.println("cannot execute javap, perhaps jdk8/lib/tools.jar is missing from the classpath and from java.home");
        System.err.println("example: java -cp jcov.jar:tools.jar com.sun.tdk.jcov.RepGen -javap path_to_classes -o path_to_javap_output path_to_result.xml");
        return;
    }
}
