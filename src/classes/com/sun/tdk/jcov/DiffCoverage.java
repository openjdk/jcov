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

import com.sun.tdk.jcov.filter.MemberFilter;
import com.sun.tdk.jcov.instrument.DataClass;
import com.sun.tdk.jcov.instrument.DataField;
import com.sun.tdk.jcov.instrument.DataMethod;
import com.sun.tdk.jcov.instrument.DataPackage;
import com.sun.tdk.jcov.instrument.DataRoot;
import com.sun.tdk.jcov.report.ClassCoverage;
import com.sun.tdk.jcov.report.LineCoverage;
import com.sun.tdk.jcov.report.MethodCoverage;
import com.sun.tdk.jcov.tools.EnvHandler;
import com.sun.tdk.jcov.tools.JCovCMDTool;
import com.sun.tdk.jcov.tools.OptionDescr;
import com.sun.tdk.jcov.util.Utils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p> Allows to get numbers of changed code lines that were not covered. </p>
 * <p> Uses a diff file and a JCov XML result file. </p> <p> Mercurial and
 * Subversion diff files formats are supported. </p>
 *
 * @author Andrey Titov
 */
public class DiffCoverage extends JCovCMDTool {

    /**
     * A line in the sources
     */
    public static class SourceLine {

        private int line;
        private String source;
        boolean checked;

        public SourceLine(int line, String source) {
            this.line = line;
            this.source = source;
        }

        public SourceLine() {
        }

        public boolean hits(int start, int end) {
            return line > start && line < end;
        }

        public String toString() {
            return "[line = " + line + ", source = " + source + "]";
        }
    }
    private String file;
    private File diffFile;
    private HashMap<String, SourceLine[]> sources;
    private HashMap<Integer, String> sourceLines;
    private static final Logger logger;
    private String replaceDiff;
    private String replaceClass;
    private boolean all;

    static {
        Utils.initLogger();
        logger = Logger.getLogger(DiffCoverage.class.getName());
    }

    @Override
    protected int run() throws Exception {
        final LinkedList<String> classNames = new LinkedList<String>();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(diffFile), "UTF-8"));
            DiffHandler handler = new HGDiffHandler(in);
            sources = new HashMap<String, SourceLine[]>();
            String sourceName;
            while ((sourceName = handler.getNextSource()) != null) {
                LinkedList<SourceLine> lines = new LinkedList<SourceLine>();
                SourceLine line;
                while ((line = handler.getNextSourceLine()) != null) {
                    lines.add(line);
                }
                if (lines.size() > 0) {
                    classNames.add(sourceName.substring(sourceName.lastIndexOf('/') + 1)); // rough estimation of needed classes
                    logger.log(Level.INFO, "File {0} has {1} new lines", new Object[]{sourceName, lines.size()});
                    if (replaceDiff != null) {
                        String[] split = replaceDiff.split(":");
                        String patt = split[0];
                        String with;
                        if (split.length == 1) {
                            with = "";
                        } else {
                            with = split[1];
                        }
                        sourceName = sourceName.replaceAll(patt, with);
                    }
                    sources.put(sourceName, lines.toArray(new SourceLine[lines.size()]));
                } else {
                    logger.log(Level.INFO, "File {0} doesn't have new lines", sourceName);
                }
            }

        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error while parsing diff file", ex);
            if (ex instanceof NullPointerException) {
                ex.printStackTrace();
            }
            return ERROR_EXEC_EXIT_CODE;
        }

        DataRoot data = DataRoot.read(file, false, new MemberFilter() {
            public boolean accept(DataClass clz) {
                if (classNames.contains(clz.getSource())) {
                    return true;
                }
                return false;
            }

            public boolean accept(DataClass clz, DataMethod m) {
                return true;
            }

            public boolean accept(DataClass clz, DataField f) {
                return true;
            }
        });

        int notCovered = 0, covered = 0, nonCode = 0;
        for (DataPackage p : data.getPackages()) {
            HashMap<String, ArrayList<ClassCoveragePair>> classesMap = new HashMap<String, ArrayList<ClassCoveragePair>>();
            for (DataClass c : p.getClasses()) {
                String packageName = c.getPackageName();
                String className = !packageName.isEmpty() ? packageName + "/" + c.getSource() : c.getSource();
                if (replaceClass != null) {
                    String[] split = replaceClass.split(":");
                    String patt = split[0];
                    String with;
                    if (split.length == 1) {
                        with = "";
                    } else {
                        with = split[1];
                    }
                    className = className.replaceAll(patt, with);
                }
                if (classesMap.get(className) == null){
                    classesMap.put(className, new ArrayList<ClassCoveragePair>());
                }
                classesMap.get(className).add(new ClassCoveragePair(c));
            }

            for (String cln:classesMap.keySet()) {
                SourceLine lines[] = sources.get(cln);
                if (lines != null) {
                    ArrayList<DataMethod> methods = new ArrayList<DataMethod>();
                    String sourceClassName = "";
                    for (int i=0; i<classesMap.get(cln).size(); i++){
                        DataClass dc = classesMap.get(cln).get(i).getDataClass();
                        methods.addAll(dc.getMethods());
                        if (!dc.getFullname().contains("$")){
                            sourceClassName = dc.getName();
                        }
                    }
                    for (DataMethod m : methods) {
                        boolean changed = false;
                        LineCoverage lc = new MethodCoverage(m, false).getLineCoverage(); // false is not used

                        for (SourceLine line : lines) {
                            if (line.checked) {
                                continue;
                            }
                            if (line.line >= lc.firstLine() && line.line <= lc.lastLine()) {
                                line.checked = true;
                                if (!changed && all) {
                                    System.out.println(String.format("   %s: %s.%s", cln, sourceClassName, m.getFormattedSignature()));
                                    changed = true;
                                }
                                if (isLineCovered(classesMap.get(cln), line.line)) {
                                    ++covered;
                                    if (all) {
                                        System.out.println(String.format("+ %6d |%s", line.line, line.source));
                                    }
                                } else {
                                    if (isCode(classesMap.get(cln), line.line)) {
                                        if (!changed && !all) {
                                            System.out.println(String.format("   %s> %s: %s", cln, sourceClassName, m.getFormattedSignature()));
                                            changed = true;
                                        }
                                        ++notCovered;
                                        System.out.println(String.format("- %6d |%s", line.line, line.source));
                                    } else {
                                        ++nonCode;
                                        if (all) {
                                            System.out.println(String.format("  %6d |%s", line.line, line.source));
                                        }
                                    }
                                }
                            }
                        }
                    }

                    for (SourceLine line : lines) {
                        if (!line.checked) {
                            ++nonCode;
                        }
                    }
                }
            }
        }
        System.out.println(String.format("lines: %d new; %d covered; %d not covered; %d not code", nonCode + notCovered + covered, covered, notCovered, nonCode));

        return SUCCESS_EXIT_CODE;
    }

    private boolean isLineCovered(ArrayList<ClassCoveragePair> classes, int line){
        for (int i=0; i<classes.size(); i++){
            ClassCoverage cc = classes.get(i).getClassCoverage();
            if (cc.isLineCovered(line)){
                return true;
            }
        }
        return false;
    }

    private boolean isCode(ArrayList<ClassCoveragePair> classes, int line){
        for (int i=0; i<classes.size(); i++){
            ClassCoverage cc = classes.get(i).getClassCoverage();
            if (cc.isCode(line)){
                return true;
            }
        }
        return false;
    }

    @Override
    protected EnvHandler defineHandler() {
        return new EnvHandler(new OptionDescr[]{DSC_REPLACE_DIFF, DSC_REPLACE_CLASS, DSC_ALL}, this);
    }

    @Override
    protected int handleEnv(EnvHandler envHandler) throws EnvHandlingException {
        String[] tail = envHandler.getTail();

        if (tail == null) {
            throw new EnvHandlingException("No input files. Please specify JCov data file and diff (mercurial) file.");
        }

        if (tail.length < 2) {
            throw new EnvHandlingException("Not enough input files. Please specify JCov data file and diff (mercurial) file.");
        }

        file = tail[0];
        Utils.checkFileNotNull(tail[0], "JCov datafile", Utils.CheckOptions.FILE_EXISTS, Utils.CheckOptions.FILE_ISFILE, Utils.CheckOptions.FILE_CANREAD);
        diffFile = new File(tail[1]);
        Utils.checkFile(diffFile, "diff file", Utils.CheckOptions.FILE_EXISTS, Utils.CheckOptions.FILE_ISFILE, Utils.CheckOptions.FILE_CANREAD);

        replaceDiff = envHandler.getValue(DSC_REPLACE_DIFF);
        replaceClass = envHandler.getValue(DSC_REPLACE_CLASS);
        all = envHandler.isSet(DSC_ALL);

        return SUCCESS_EXIT_CODE;
    }

    @Override
    protected String getDescr() {
        return "check whether changed lines were covered";
    }

    @Override
    protected String usageString() {
        return "java -jar jcov.jar diffcoverage result.xml diff";
    }

    @Override
    protected String exampleString() {
        return "java -jar jcov.jar diffcoverage -replace src/classes/: result.xml diff";
    }

    static interface DiffHandler {

        public String getNextSource() throws IOException;

        public SourceLine getNextSourceLine() throws IOException;
    }

    static class HGDiffHandler implements DiffHandler {

        BufferedReader in;
        String current;
        int startLines = -1;
        int endLines = -1;

        public HGDiffHandler(BufferedReader in) {
            this.in = in;
        }

        public String getNextSource() throws IOException {
            if (current == null || !current.startsWith("+++")) {
                current = in.readLine();
            }
            while (current != null && !(current.startsWith("+++") && current.contains(".java") && !current.contains("package-info.java") && !current.contains("module-info.java"))) {
                current = in.readLine();
            }
            if (current != null) {
                String next = in.readLine();
                if (next.startsWith("@@")) {
//                    String filepath = current.substring(4, current.indexOf(".java") + 5);
                    String filepath = current.substring(0, current.lastIndexOf(".java") + 5);
                    filepath = filepath.replaceAll("\\+\\+\\+ ([a-zA-Z]/)?", ""); // !!! deleting _one_ symbol from the begining of the filename if
                    current = next;
                    return filepath;
                }
                current = null;
            } // else - EOF

            return null;
        }
        private static String LINES_NUMBERS_PATTERN = "@@ [-+][\\d]+,[\\d]+ [-+][\\d]+,[\\d]+ @@(?s).*";

        public SourceLine getNextSourceLine() throws IOException {
            if (current != null && !current.startsWith("---")) {
                if (startLines == -1 && endLines == -1) {
                    String linesNumbersLine = current;
                    while (linesNumbersLine == null || !linesNumbersLine.matches(LINES_NUMBERS_PATTERN)) { // e.g. @@ +1,44 -4,8 @@
                        linesNumbersLine = in.readLine();
                        if (linesNumbersLine == null) {
                            startLines = -1;
                            endLines = -1;
                            current = null;
                            return null;
                        }
                        if (linesNumbersLine.matches("---")) { // end of file block
                            startLines = -1;
                            endLines = -1;
                            current = null;
                            return null;
                        }
                    }
                    linesNumbersLine = linesNumbersLine.substring(0, linesNumbersLine.lastIndexOf("@@") + 2);
                    linesNumbersLine = linesNumbersLine.replaceAll(" @@", "");
                    linesNumbersLine = linesNumbersLine.replaceAll("@@ .*\\+", "");
                    String[] split = linesNumbersLine.split(",");

                    startLines = Integer.parseInt(split[0]);
                    endLines = Integer.parseInt(split[1]) + startLines;
                }
            } else if (current != null) {
                return null;
            }

            String plusLine = null;
            while (plusLine == null || !(plusLine.startsWith("+"))) {
                plusLine = in.readLine(); // searching '+' statement
                if (plusLine == null) {
                    startLines = -1;
                    endLines = -1;
                    current = null;
                    return null;
                }
                if (!plusLine.startsWith("-")) {
                    ++startLines;
                }
                if (plusLine.startsWith("---")) {
                    startLines = -1;
                    endLines = -1;
                    current = plusLine;
                    return null;
                } else if (plusLine.matches(LINES_NUMBERS_PATTERN)) {
                    startLines = -1;
                    endLines = -1;
                    current = plusLine;

                    return getNextSourceLine();
                }
            }
            // plusLine is not null if we are here
            SourceLine line = new SourceLine(startLines - 1, plusLine.substring(1));
            if (startLines > endLines) {
                startLines = -1;
                endLines = -1;
                current = null;
            }
            return line;
        }
    }

    private class ClassCoveragePair{
        private DataClass dClass;
        private ClassCoverage cClass;
        public ClassCoveragePair(DataClass dClass){
            this.dClass = dClass;
            this.cClass = new ClassCoverage(dClass, null, MemberFilter.ACCEPT_ALL);
        }

        public DataClass getDataClass(){
            return dClass;
        }

        public ClassCoverage getClassCoverage(){
            return cClass;
        }
    }

    static OptionDescr DSC_REPLACE_DIFF = new OptionDescr("replaceDiff", "Manage replacing", OptionDescr.VAL_SINGLE, "Set replacement pattern for diff filenames (e.g. to cut out \"src/classes\" you can specify -replaceDiff src/classes:)");
    static OptionDescr DSC_REPLACE_CLASS = new OptionDescr("replaceClass", "", OptionDescr.VAL_SINGLE, "Set replacement pattern for class filenames (e.g. to cut out \"com/sun\" you can specify -replaceDiff com/sun:)");
    static OptionDescr DSC_ALL = new OptionDescr("all", "Manage output", OptionDescr.VAL_NONE, "Show covered and non-code lines as well as not covered");
}
