/*
 * Copyright (c) 2014,2024, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JavapClass object representing javap output for the specified class
 *
 * @author Alexey Fedorchenko
 */
public class JavapClass {

    private String packageName;
    private String className;
    private final ArrayList<JavapLine> lines = new ArrayList<>();
    // method in the class is list of lines numbers in the javap output
    private final HashMap<String, ArrayList<Integer>> methods = new HashMap<>();

    /**
     * Get a list of JavapLines associated with the method
     *
     * @param methodName      the name of the method to find
     * @param methodSignature the signature of the method to find
     * @return a list of JavapLines associated with the method
     */
    public List<JavapLine> getMethod(String methodName, String methodSignature) {
        List<Integer> list = methods.get(methodName + methodSignature);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return lines.subList(list.get(0), list.get(list.size() - 1) + 1);
    }

    public String getClassName() {
        return className == null ? "" : className;
    }

    /**
     * return a list of JavapLines associated with the class
     */
    public List<JavapLine> getLines() {
        return lines;
    }

    /**
     * parse specified class file and fill JavapClass object data
     *
     * @param filePath - path to the class file
     */
    void parseJavapFile(String filePath, String jarPath) {

        try {
            BufferedReader inStream;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, Charset.defaultCharset()));

            JavapClassReader.read(filePath, jarPath, pw);

            inStream = new BufferedReader(
                    new InputStreamReader(new ByteArrayInputStream(out.toByteArray()), Charset.defaultCharset()));

            String textLine;
            String lastMethodString = "";
            int lineNumber = 0;

            while ((textLine = inStream.readLine()) != null) {

                if (textLine.startsWith("Warning:")) {
                    //do not parse javap warnings
                    continue;
                }

                // try to find class in javap output
                if (textLine.contains("class") && textLine.contains("{")) {
                    parsePackageAndClassNames(textLine);
                }
                // try to find method in javap output
                if (textLine.contains("(") && (textLine.contains(");") || textLine.contains(") throws"))) {
                    if (!textLine.contains("." + className + "(")) {
                        lastMethodString = parseMethodString(textLine);
                    } else {
                        lastMethodString = parseClassString(textLine);
                    }
                    methods.put(lastMethodString, new ArrayList<>());
                } else {
                    if (textLine.trim().equals("static {};")) {
                        lastMethodString = parseStaticBlockString();
                        methods.put(lastMethodString, new ArrayList<>());
                    }
                }
                // try to find code lines which could be covered
                if (isCodeLine(textLine)) {
                    addCodeLine(lineNumber, textLine, lastMethodString);
                } else {
                    addLine(lineNumber, textLine);
                }
                lineNumber++;

            }
            inStream.close();
        } catch (Exception e) {
            System.err.println("Error in parsing javap file:");
            e.printStackTrace();
        }
    }

    final private static Pattern codeLinePattern = Pattern.compile("^\\s*\\d+:.*");
    final private static Pattern switchPattern = Pattern.compile("^\\s*\\d+:\\s+\\d+$");

    /**
     * Check whether the line is a code line
     *
     * @param textLine javap source line
     * @return true if the line is the code line
     */
    private boolean isCodeLine(String textLine) {
        if (codeLinePattern.matcher(textLine).find()) {
            int ind = textLine.indexOf("//");
            String line = ((ind != -1) ? textLine.substring(ind) : textLine).trim();
            return !switchPattern.matcher(line).find();
        }
        return false;
    }

    final private static String[] JavaClassTokens = new String[]{"implements", "extends", "{"};

    private void parsePackageAndClassNames(String textLine) {

        for (String s : JavaClassTokens) {
            if (textLine.contains(s)) {
                textLine = textLine.substring(0, textLine.indexOf(s));
            }
        }

        textLine = textLine.substring(textLine.indexOf("class") + 5).trim();

        int ind = textLine.indexOf('<');
        if (ind != -1) {
            textLine = textLine.substring(0, ind);
        }

        ind = textLine.lastIndexOf('.');
        if (ind > 0) {
            packageName = textLine.substring(0, ind);
            className = textLine.substring(ind + 1);
        } else {
            className = textLine;
            packageName = "";
        }
    }

    private String parseStaticBlockString() {
        return "<clinit>()V";
    }

    private String parseClassString(String textLine) {
        textLine = removeGenericsInfo(textLine);
        String vmSig = encodeVmSignature(substringBetween(textLine, "\\(", "\\)", false), null);
        return "<init>" + vmSig;
    }

    /**
     * Parses the method's name and signature from the Javap source.
     * Since the constructor in the Javap output shares the same name as the class,
     * the method substitutes it with &pt;init&gt; to ensure compatibility with the coverage report."
     *
     * @param textLine a line containing the method's name and signature
     * @Returns the method name concatenated with the method's signature
     */
    private String parseMethodString(String textLine) {
        textLine = removeGenericsInfo(textLine);
        String methodName = substringBetween(textLine, "\\ ", "\\(", true);
        String vmSig;
        // Checks whether it is a constructor, according to javap notation
        if (methodName.equals(className)) {
            methodName = "<init>";
            vmSig = encodeVmSignature(substringBetween(textLine, "\\(", "\\)", false), null);
        } else {
            String returnType = substringBetween(textLine, "\\ ", " " + methodName, true);
            vmSig = encodeVmSignature(substringBetween(textLine, "\\(", "\\)", false), returnType);
        }
        return methodName + vmSig;
    }

    private static String encodeVmType(String oneMethodParam) {
        String className = oneMethodParam.replaceAll("[\\,\\[\\]]", "");
        String s = className;
        if (className.lastIndexOf(".") > -1) {
            s = className.substring(className.lastIndexOf("."));
        }
        String dim = oneMethodParam.replaceAll("[^\\[\\]]", "");
        String newType;
        switch (s) {
            case "boolean":
            case "Boolean":
                newType = "Z";
                break;
            case "void":
            case "Void":
                newType = "V";
                break;
            case "int":
            case "Integer":
                newType = "I";
                break;
            case "long":
            case "Long":
                newType = "J";
                break;
            case "char":
            case "Character":
                newType = "C";
                break;
            case "byte":
            case "Byte":
                newType = "B";
                break;
            case "double":
            case "Double":
                newType = "D";
                break;
            case "short":
            case "Short":
                newType = "S";
                break;
            case "float":
            case "Number":
                newType = "F";
                break;
            default:
                newType = "L";
                break;
        }
        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < dim.length() / 2; i++) {
            prefix.append("[");
        }
        if (className.lastIndexOf(".") > -1) {
            return prefix + newType + className.replaceAll("\\.", "/") + ";";
        } else {
            return prefix + newType;
        }
    }

    private static String removeGenericsInfo(String textLine) {

        if (textLine != null) {
            textLine = textLine.replaceAll("<.*?>", "");
        }

        return textLine;
    }

    private static String encodeVmSignature(String params, String returnValue) {

        if (params == null && returnValue == null) {
            return "()V";
        }

        StringBuilder vmSig = new StringBuilder();
        if (params != null) {

            if (params.contains(" ")) {
                for (String p : params.split(" ")) {
                    vmSig.append(encodeVmType(p));
                }
            } else {
                vmSig.append(encodeVmType(params));
            }
        }

        if (returnValue == null || returnValue.equals("void")) {
            return "(" + vmSig + ")V";
        }

        return "(" + vmSig + ")" + encodeVmType(returnValue);
    }

    private void addCodeLine(int lineNumber, String textLine, String methodNameAndVMsig) {

        JavapCodeLine codeLine = new JavapCodeLine();

        try {
            codeLine.setCodeNumber(Integer.parseInt(textLine.substring(0, textLine.indexOf(":")).trim()));
        } catch (NumberFormatException nfe) {
            System.err.println(nfe + " in code line: " + textLine);
        }

        codeLine.setLineNumber(lineNumber);
        codeLine.setTextLine(textLine);

        if (methods.get(methodNameAndVMsig) != null) {
            methods.get(methodNameAndVMsig).add(lineNumber);
        }
        lines.add(codeLine);
    }

    private void addLine(int lineNumber, String textLine) {
        JavapLine codeLine = new JavapLine();
        codeLine.setLineNumber(lineNumber);
        codeLine.setTextLine(textLine);

        lines.add(codeLine);
    }

    private static String substringBetween(String str, String open, String close, boolean firstValue) {

        // does not allow any characters from the "close" string in the end
        String regexStringLast = "([^" + open + "]+)(?=" + close + "[^" + close + "]*$)";
        // just try to find string between open and close
        String regexString = "([^" + open + "]+)(?=" + close + ")";

        if (!firstValue) {
            regexString = regexStringLast;
        }

        Pattern p = Pattern.compile(regexString);
        str = str.trim();
        int i = str.indexOf("//");
        if (i > -1) {
            str = str.substring(0, i);
        }

        Matcher m = p.matcher(str);
        if (m.find()) {
            return m.group(1).trim();
        }

        return null;
    }
}
