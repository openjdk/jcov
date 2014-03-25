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
package com.sun.tdk.jcov.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 *
 * @author Sergey Borodin
 */
public class DebugUtils {

    public static final java.util.logging.Logger log;

    static {
        Utils.initLogger();
        log = Logger.getLogger("com.sun.tdk.jcov");
    }

    public static void flushInstrumentedClass(String flushPath,
            String className, byte[] data) {
        File root = new File(flushPath);
        String path = root.getAbsolutePath() + File.separator + className + ".class";
        File classFile = prepareFile(path);
        try {
            FileOutputStream fos = new FileOutputStream(classFile);
            fos.write(data);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException fnfe) {
        } catch (IOException ioe) {
        }
    }

    public static byte[] readClass(String className, String flushPath) {
        File root = new File(flushPath);
        String path = root.getAbsolutePath() + File.separator + className + ".class";
        File f = new File(path);
        int classLength = (int) f.length();
        FileInputStream fis;
        try {
            fis = new FileInputStream(f);
        } catch (FileNotFoundException e) {
//            logger.log(Level.SEVERE, "File not found - skipped", e);
            return null;
        }
        try {
            byte classBuf[] = new byte[classLength];
            fis.read(classBuf, 0, classLength); // read in class data
            fis.close();
            return classBuf;
        } catch (IOException e) {
//            logger.log(Level.SEVERE, "Error reading '" + fname + "' - skipped", e);
            return null;
        }
    }

    public static PrintWriter getPrintWriter(String className, String flushPath) {
        File root = new File(flushPath);
        String path = root.getAbsolutePath() + File.separator + className + ".java";
        File classFile = prepareFile(path);
        try {
            return new PrintWriter(new OutputStreamWriter(new FileOutputStream(classFile), Charset.defaultCharset()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void attachLogger() {
        try {
            FileHandler fh = new FileHandler("jcov.log");
            fh.setFormatter(new SimpleFormatter());
            log.addHandler(fh);
            log.setLevel(Level.ALL);
        } catch (Exception e) {
            log.warning("File " + "jcov.log" + " could not be opened");
        }

    }

    private static File prepareFile(String path) {
        File classFile = new File(path);
        if (!classFile.exists()) {
            try {
                File parent = classFile.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }
                classFile.createNewFile();
            } catch (IOException e) {
                System.out.println("Can't create file: " + path);
                System.out.println(e.getMessage());
            }
        }
        return classFile;
    }
}
