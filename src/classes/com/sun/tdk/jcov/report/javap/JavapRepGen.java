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

import com.sun.tdk.jcov.RepGen;
import com.sun.tdk.jcov.data.FileFormatException;
import com.sun.tdk.jcov.data.Result;
import com.sun.tdk.jcov.instrument.DataBlock;
import com.sun.tdk.jcov.instrument.DataBlockTarget;
import com.sun.tdk.jcov.instrument.DataBranch;
import com.sun.tdk.jcov.instrument.DataClass;
import com.sun.tdk.jcov.instrument.DataMethod;
import com.sun.tdk.jcov.instrument.DataRoot;
import com.sun.tdk.jcov.io.ClassSignatureFilter;
import com.sun.tdk.jcov.io.Reader;
import com.sun.tdk.jcov.util.Utils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;

/**
 * Report generation for classfiles (javap output will be used as source).
 *
 * @author Alexey Fedorchenko
 */
public class JavapRepGen {

    private final RepGen repGen;

    private static String strMsg = "";

    public JavapRepGen(RepGen repGen){
        this.repGen = repGen;
    }

    /**
     * find the list of classfiles in the specified root
     *
     * @param root - directory to find classfiles
     * @param result - result list of classfiles
     */
    private void finder(File root, List<File> result) {
        File[] files = root.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    if (file.getName().endsWith(".class")) {
                        result.add(file);
                    }
                } else if (file.isDirectory()) {
                    finder(file, result);
                }
            }
        }
    }

    /**
     * main method to create report with javap output for classes
     *
     * @param templatePath - path to the result.xml (xml file for creating report)
     * @param classesPath - path to the product classes
     * @param outPath - path where report should be saved
     */
    public void run(String templatePath, String classesPath, String outPath) {

        DataRoot file_image;
        ClassSignatureFilter acceptor = new ClassSignatureFilter(null, null, null);
        try {
            file_image = Reader.readXML(templatePath, false, acceptor);
        } catch (FileFormatException ex) {
            Logger.getLogger(JavapRepGen.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            return;
        }

        if (classesPath == null) {
            JavapRepGen.printErrorMsg("no input classes specified");
            return;
        }

        ArrayList<File> classFiles = new ArrayList<>();
        ArrayList<String> classFilesInJar = new ArrayList<>();
        File rootFile = new File(classesPath);
        HashMap<String, JavapClass> classes = new HashMap<>();

        if (rootFile.isDirectory()) {
            finder(rootFile, classFiles);
        } else if (rootFile.isFile()) {
            String extension = "";
            int i = rootFile.getName().lastIndexOf('.');
            if (i > 0) {
                extension = rootFile.getName().substring(i + 1);
            }
            if ("class".equals(extension)) {
                classFiles.add(rootFile);
            } else {
                try {
                    JarFile jarFile = new JarFile(rootFile);
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {

                        JarEntry entry = entries.nextElement();
                        if (entry.getName().endsWith(".class")) {
                            classFilesInJar.add(entry.getName().replaceAll("/", ".")
                                    .replace("\\", ".").substring(0, entry.getName().lastIndexOf(".class")));
                        }
                    }
                } catch (IOException ioe) {
                    // if there is no class files user will have the message
                }
            }
        }

        if (classFiles.isEmpty() && classFilesInJar.isEmpty()) {
            JavapRepGen.printErrorMsg("no .class files found at the specified path: " + classesPath);
            return;
        }

        filterClasses(classFiles, classesPath);
        filterClassesInJar(classFilesInJar);

        for (File classFile : classFiles) {
            JavapClass javapClass = new JavapClass();
            javapClass.parseJavapFile(classFile.getAbsolutePath(), null);
            classes.put(javapClass.getClassName(), javapClass);
        }

        for (String classFileInJar : classFilesInJar) {
            JavapClass javapClass = new JavapClass();
            javapClass.parseJavapFile(classFileInJar, rootFile.getAbsolutePath());
            classes.put(javapClass.getClassName(), javapClass);
        }

        //reading block and branch coverage and mark lines in javap classes.
        for (DataClass dataClass : file_image.getClasses()) {
            for (DataMethod dataMethod : dataClass.getMethods()) {

                ArrayList<Integer> visitedBlocksNumbers = new ArrayList<>();
                for (DataBlock dataBlock : dataMethod.getBlocks()) {
                    if (dataBlock.getCount() > 0) {
                        for (int index = dataBlock.startBCI(); index <= dataBlock.endBCI(); index++) {
                            visitedBlocksNumbers.add(index);
                        }
                    }
                }
                for (DataBranch dataBranch : dataMethod.getBranches()) {
                    for (DataBlockTarget dataBranchTarget : dataBranch.getBranchTargets()) {
                        if (dataBranchTarget.getCount() > 0) {
                            for (int index = dataBranchTarget.startBCI(); index <= dataBranchTarget.endBCI(); index++) {
                                visitedBlocksNumbers.add(index);
                            }
                        }
                    }
                }

                List<JavapLine> methodLines;
                JavapClass javapClass = classes.get(dataClass.getName());
                if (javapClass != null) {
                    methodLines = javapClass.getMethod(dataMethod.getName() + dataMethod.getVmSignature());
                if (methodLines != null) {
                    for (JavapLine javapLine : methodLines) {
                        if ((javapLine instanceof JavapCodeLine) && visitedBlocksNumbers.contains(((JavapCodeLine) javapLine).getCodeNumber())) {
                            ((JavapCodeLine) javapLine).setVisited(true);
                        }
                    }
                }
                } else {
                    JavapRepGen.printErrorMsg(format("Can't get javap output for %s.class (%s) at the specified path: %s",
                            dataClass.getName(),
                            templatePath,
                            classesPath));
                }
            }
        }

        try {
            Result res = new Result(templatePath);
            repGen.generateReport(repGen.getDefaultReportGenerator(), outPath, res, null, new ArrayList(classes.values()));
        } catch (Exception e) {
            JavapRepGen.printErrorMsg("error in report generation: " + e);
        }

    }

    private static void printErrorMsg(String msg) {
        if ( strMsg.hashCode() != msg.hashCode() ) {
            strMsg = msg;
            System.err.println(msg);
        }
    }

    private void filterClasses(ArrayList<File> files, String classesPath) {

        ArrayList<File> newFiles = new ArrayList<>();
        if (files.size() > 1) {

            classesPath = new File(classesPath+"/").getAbsolutePath().replaceAll("\\\\","/");
            for (File classFile : files) {

                String className = classFile.getAbsolutePath().replaceAll("\\\\","/");

                if (className.startsWith(classesPath + "/")){
                    className = className.substring(classesPath.length()+1);
                }
                if (className.endsWith(".class")) {
                    className = className.substring(0, className.lastIndexOf(".class"));
                }
                if (Utils.accept(Utils.concatFilters(repGen.getInclude(), repGen.getExclude()), null, "/" + className, null)) {
                    newFiles.add(classFile);
                }

            }

            files.clear();
            files.addAll(newFiles);
        }

    }

    private void filterClassesInJar(ArrayList<String> filesInJar) {

        ArrayList<String> newFilesInJar = new ArrayList<>();

        for (String classFile : filesInJar) {
            if (Utils.accept(Utils.concatFilters(repGen.getInclude(), repGen.getExclude()), null, "/" + classFile.replaceAll("\\.", "/"), null)) {
                newFilesInJar.add(classFile);
            }
        }
        filesInJar.clear();
        filesInJar.addAll(newFilesInJar);
    }
}
