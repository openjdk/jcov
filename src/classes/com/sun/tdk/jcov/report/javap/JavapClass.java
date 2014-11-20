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
package com.sun.tdk.jcov.report.javap;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
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
    // class as list of JavapLines
    private ArrayList<JavapLine> lines = new ArrayList<JavapLine>();
    // method in the class is list of lines numbers in the javap output
    private HashMap<String, ArrayList<Integer>> methods = new HashMap<String, ArrayList<Integer>>();

    /**
     * return method in the class like list of JavapLines
     *
     * @param nameAndVMSig name and VMsig string is needed to find method in the
     * class
     * @return method in the class like list of JavapLines
     */
    public List<JavapLine> getMethod(String nameAndVMSig) {

        List<Integer> numbers = methods.get(nameAndVMSig);

        if (numbers == null || numbers.isEmpty()) {
            return null;
        }

        return lines.subList(numbers.get(0), numbers.get(numbers.size() - 1) + 1);

    }

    public String getClassName() {
        return className;
    }

    public String getPackageName() {
        return packageName;
    }

    /**
     * return javap result for class like list of JavapLines
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
            BufferedReader inStream = null;
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
                    parseClassName(textLine);
                    parsePackageName(textLine);
                }
                // try to find method in javap output
                if (textLine.contains("(") && (textLine.contains(");") || textLine.contains(") throws"))) {
                    // check if it is constructor or not
                    if (!textLine.contains("." + className + "(")) {
                        lastMethodString = parseMethodString(textLine);
                    } else {
                        lastMethodString = parseClassString(textLine);
                    }
                    methods.put(lastMethodString, new ArrayList<Integer>());
                } else {
                    if (textLine.trim().equals("static {};")) {
                        lastMethodString = parseStaticBlockString();
                        methods.put(lastMethodString, new ArrayList<Integer>());
                    }
                }
                // try to find code lines which could be covered
                if (textLine.contains(":") && !textLine.contains("Code")
                        && !textLine.contains("case") && !textLine.contains("default")
                        && !textLine.contains("Exception table")) {
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

    private void parseClassName(String textLine) {

        if (textLine.contains("implements")) {
            textLine = textLine.substring(0, textLine.indexOf("implements"));
        }

        if (textLine.contains("extends")) {
            textLine = textLine.substring(0, textLine.indexOf("extends"));
        }
        textLine = textLine + "{";

        className = substringBetween(textLine, "\\.", "\\ ", false);
        if(className != null && className.contains("<") && className.contains(">")){
            className = className.substring(0, className.indexOf('<'));
        }

    }

    private void parsePackageName(String textLine) {

        if (textLine.contains("implements")) {
            textLine = textLine.substring(0, textLine.indexOf("implements"));
        }

        if (textLine.contains("extends")) {
            textLine = textLine.substring(0, textLine.indexOf("extends"));
        }
        textLine = textLine + "{";

        packageName = substringBetween(textLine, "\\ ", "\\.", false);
    }

    private String parseStaticBlockString() {
        String nameAndVMSig = "<clinit>()V";
        return nameAndVMSig;
    }

    private String parseClassString(String textLine) {
        textLine = removeGenericsInfo(textLine);
        String vmSig = encodeVmSignature(substringBetween(textLine, "\\(", "\\)", false), null);
        String nameAndVMSig = "<init>" + vmSig;
        return nameAndVMSig;
    }

    private String parseMethodString(String textLine) {
        textLine = removeGenericsInfo(textLine);
        String methodName = substringBetween(textLine, "\\ ", "\\(", true);
        String returnType = substringBetween(textLine, "\\ ", " " + methodName, true);

        String vmSig = encodeVmSignature(substringBetween(textLine, "\\(", "\\)", false), returnType);

        return methodName + vmSig;
    }

    private static String encodeVmType(String oneMethodParam) {
        String className = oneMethodParam.replaceAll("[\\,\\[\\]]", "");
        String s = className;
        if (className.lastIndexOf(".") > -1) {
            s = className.substring(className.lastIndexOf("."), className.length());
        }
        String dim = oneMethodParam.replaceAll("[^\\[\\]]", "");
        String newType = "";
        if ("boolean".equals(s) || "Boolean".equals(s)) {
            newType = "Z";
        } else if ("void".equals(s) || "Void".equals(s)) {
            newType = "V";
        } else if ("int".equals(s) || "Integer".equals(s)) {
            newType = "I";
        } else if ("long".equals(s) || "Long".equals(s)) {
            newType = "J";
        } else if ("char".equals(s) || "Character".equals(s)) {
            newType = "C";
        } else if ("byte".equals(s) || "Byte".equals(s)) {
            newType = "B";
        } else if ("double".equals(s) || "Double".equals(s)) {
            newType = "D";
        } else if ("short".equals(s) || "Short".equals(s)) {
            newType = "S";
        } else if ("float".equals(s) || "Number".equals(s)) {
            newType = "F";
        } else {
            newType = "L";
        }
        String prefix = "";
        for (int i = 0; i < dim.length() / 2; i++) {
            prefix += "[";
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

        String vmSig = "";
        if (params != null) {

            if (params.contains(" ")) {
                for (String p : params.split(" ")) {
                    vmSig += encodeVmType(p);
                }
            } else {
                vmSig += encodeVmType(params);
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
        String regexStringFirst = "([^" + open + "]+)(?=" + close + ")";
        String regexString = regexStringFirst;

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
