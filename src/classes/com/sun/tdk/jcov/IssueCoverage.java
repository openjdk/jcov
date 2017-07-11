/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.source.tree.*;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import com.sun.tdk.jcov.filter.MemberFilter;
import com.sun.tdk.jcov.instrument.*;
import com.sun.tdk.jcov.processing.DataProcessorSPI;
import com.sun.tdk.jcov.processing.DefaultDataProcessorSPI;
import com.sun.tdk.jcov.processing.StubSpi;
import com.sun.tdk.jcov.report.*;
import com.sun.tdk.jcov.tools.*;
import com.sun.tdk.jcov.util.Utils;

import javax.tools.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * @author Alexey Fedorchenko
 */
public class IssueCoverage extends JCovCMDTool {

    final static String DATA_PROCESSOR_SPI = "dataprocessor.spi";

    private String hg_path = "hg";
    private String hg_repo_dir = "jdk";
    private String output = "report";
    private String hg_comment = "";
    private DataProcessorSPI dataProcessorSPIs[];
    private static final Logger logger;
    private String resultFile;
    private String replaceDiff = "";
    private HashMap<String, DiffCoverage.SourceLine[]> sources;


    static {
        Utils.initLogger();
        logger = Logger.getLogger(IssueCoverage.class.getName());
    }


    @Override
    protected int run() throws Exception {

        String[] command = new String[]{hg_path, "log", "-k", hg_comment};

        ProcessBuilder pb =
                new ProcessBuilder(command)
                        .directory(new File(hg_repo_dir));

        Process proc = pb.start();

        InputStream inputStream = proc.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        String logline;
        ArrayList<Integer> changesets = new ArrayList<Integer>();
        while ((logline = br.readLine()) != null) {
            if (logline.contains("changeset:")) {
                try {
                    changesets.add(Integer.parseInt(logline.split(":")[1].trim()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (changesets.size() == 0) {
            logger.log(Level.SEVERE, "No changes found in the repository for " + hg_comment);
            return ERROR_EXEC_EXIT_CODE;
        }

        String[] change_command = new String[changesets.size() + 2];
        change_command[0] = hg_path;
        change_command[1] = "diff";
        for (int i = 0; i < changesets.size(); i++) {
            change_command[i + 2] = "-c " + changesets.get(i);
        }

        ProcessBuilder pb1 =
                new ProcessBuilder(change_command)
                        .directory(new File(hg_repo_dir));

        Process proc1 = pb1.start();
        InputStream patchInputStream = proc1.getInputStream();

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(patchInputStream, "UTF-8"));
            DiffCoverage.DiffHandler handler = new DiffCoverage.HGDiffHandler(in);
            sources = new HashMap<String, DiffCoverage.SourceLine[]>();
            String sourceName;
            while ((sourceName = handler.getNextSource()) != null) {
                LinkedList<DiffCoverage.SourceLine> lines = new LinkedList<DiffCoverage.SourceLine>();
                DiffCoverage.SourceLine line;
                while ((line = handler.getNextSourceLine()) != null) {
                    lines.add(line);
                }
                if (lines.size() > 0) {
                    logger.log(Level.INFO, "File {0} has {1} new lines", new Object[]{sourceName, lines.size()});

                    sources.put(sourceName, lines.toArray(new DiffCoverage.SourceLine[lines.size()]));
                } else {
                    logger.log(Level.INFO, "File {0} doesn't have new lines", sourceName);
                }
            }

        }
        catch (Exception ex) {
            logger.log(Level.SEVERE, "Error while parsing diff", ex);
            return ERROR_EXEC_EXIT_CODE;
        }

        final HashMap<String, ArrayList<MethodWithParams>> changed = new HashMap<String, ArrayList<MethodWithParams>>();
        processChangedClasses(changesets, changed);

        DataRoot data = com.sun.tdk.jcov.io.Reader.readXML(resultFile, false, new MemberFilter() {
            public boolean accept(DataClass clz) {

                String className;
                if (replaceDiff != null && replaceDiff.contains("#module")) {
                    className = replaceDiff.replaceAll("\\#module", clz.getModuleName()) + clz.getFullname().substring(0, clz.getFullname().lastIndexOf('/') + 1) + clz.getSource();
                } else {
                    className = replaceDiff + clz.getFullname().substring(0, clz.getFullname().lastIndexOf('/') + 1) + clz.getSource();
                }
                return (changed.get(className) != null);
            }

            public boolean accept(DataClass clz, DataMethod m) {
                return true;
            }

            public boolean accept(DataClass clz, DataField f) {
                return true;
            }
        });


        if (dataProcessorSPIs != null) {
            for (DataProcessorSPI spi : dataProcessorSPIs) {
                data = spi.getDataProcessor().process(data);
            }
        }

        AncFilter notChanged = new AncFilter() {
            @Override
            public boolean accept(DataClass clz) {
                return false;
            }

            @Override
            public boolean accept(DataClass clz, DataMethod m) {

                String className;
                if (replaceDiff != null && replaceDiff.contains("#module")) {
                    className = replaceDiff.replaceAll("\\#module", clz.getModuleName()) + clz.getFullname().substring(0, clz.getFullname().lastIndexOf('/') + 1) + clz.getSource();

                } else {
                    className = replaceDiff + clz.getFullname().substring(0, clz.getFullname().lastIndexOf('/') + 1) + clz.getSource();
                }


                if (changed.get(className) != null) {

                    for (MethodWithParams method : changed.get(className)) {

                        if (m.getName().equals(method.getMethodName()) ||
                                (m.getName().contains("$") && m.getName().endsWith(method.getMethodName()))) {

                            if (method.getMethodParams().size() == 0 || m.getFormattedSignature().split(",").length == method.getMethodParams().size()
                                    && m.getFormattedSignature().matches(".*" + method.getParamsRegex())) {

                                method.setFound(true);
                                return false;
                            }
                        }
                    }
                }

                return true;

            }

            @Override
            public boolean accept(DataMethod m, DataBlock b) {
                return false;
            }

            @Override
            public String getAncReason() {
                return "No changes";
            }
        };

        RepGen rr = new RepGen();
        ReportGenerator rg = rr.getDefaultReportGenerator();
        try {
            ReportGenerator.Options options = new ReportGenerator.Options(new File(hg_repo_dir + "/" + replaceDiff).getAbsolutePath(), null, null, false,
                    false);

            ProductCoverage coverage = new ProductCoverage(data, options.getSrcRootPaths(), options.getJavapClasses(), false, false, false, new AncFilter[]{notChanged});

            for (String classFile : changed.keySet()) {
                for (MethodWithParams method : changed.get(classFile)) {
                    if (!method.isFound()) {
                        logger.log(Level.WARNING, "Could not find changed method {0} from classfile {1}", new String[]{method.toString(), classFile});
                    }
                }
            }

            rg.init(output);
            rg.generateReport(coverage, options);


        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in report generation:", e);
        }

        return 0;
    }

    private void processChangedClasses(ArrayList<Integer> changesets, HashMap<String, ArrayList<MethodWithParams>> changed) {
        try {
            ArrayList<String> files = new ArrayList<String>();

            String[] command = new String[]{hg_path, "log", "-k", hg_comment, "--stat"};

            ProcessBuilder pb =
                    new ProcessBuilder(command)
                            .directory(new File(hg_repo_dir));

            Process proc = pb.start();

            InputStream inputStream = proc.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            String classline;
            while ((classline = br.readLine()) != null) {
                if (classline.contains("|") && (classline.contains("+") || classline.contains("-"))) {
                    try {
                        files.add(classline.split(" ")[1]);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            parseClassFiles(files, changesets.get(0), changed);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in parsing changed files:", e);
        }
    }

    private void parseClassFiles(ArrayList<String> files, int changeset, HashMap<String, ArrayList<MethodWithParams>> changed) {
        try {

            for (String file : files) {

                String[] command = new String[]{hg_path, "cat", file, "-r", String.valueOf(changeset)};

                ProcessBuilder pb =
                        new ProcessBuilder(command)
                                .directory(new File(hg_repo_dir));

                Process proc = pb.start();

                InputStream inputStream = proc.getInputStream();

                final Path destination = Paths.get(new File(hg_repo_dir).getAbsolutePath() + File.pathSeparator + "temp" + File.pathSeparator + ".java");
                Files.copy(inputStream, destination);

                DiffCoverage.SourceLine lines[] = sources.get(file);

                parseClassFile(destination.toFile(), lines, file, changed);
                destination.toFile().delete();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in parsing changed files:", e);
        }
    }

    private void parseClassFile(File c, DiffCoverage.SourceLine lines[], String classFile, HashMap<String, ArrayList<MethodWithParams>> changed) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnosticsCollector = new DiagnosticCollector<JavaFileObject>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnosticsCollector, null, null);
        Iterable<? extends JavaFileObject> fileObjects = fileManager.getJavaFileObjects(c);
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnosticsCollector, null, null, fileObjects);

        JavacTask javacTask = (JavacTask) task;
        SourcePositions sourcePositions = Trees.instance(javacTask).getSourcePositions();
        Iterable<? extends CompilationUnitTree> parseResult = null;
        try {
            parseResult = javacTask.parse();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error in parsing source file:", e);
        }

        if (parseResult != null) {
            for (CompilationUnitTree compilationUnitTree : parseResult) {
                compilationUnitTree.accept(new MethodParser(compilationUnitTree, sourcePositions, lines, classFile, changed), null);
            }
        }
    }

    private class MethodParser extends TreeScanner<Void, Void> {
        private final CompilationUnitTree compilationUnitTree;
        private final SourcePositions sourcePositions;
        private final LineMap lineMap;
        private final DiffCoverage.SourceLine lines[];
        private HashMap<String, ArrayList<MethodWithParams>> changed;
        private String classFile;
        private List<ClassTree> classes;

        private MethodParser(CompilationUnitTree compilationUnitTree, SourcePositions sourcePositions, DiffCoverage.SourceLine lines[], String classFile, HashMap<String, ArrayList<MethodWithParams>> changed) {
            this.compilationUnitTree = compilationUnitTree;
            this.sourcePositions = sourcePositions;
            this.lineMap = compilationUnitTree.getLineMap();
            this.lines = lines;
            this.changed = changed;
            this.classFile = classFile;
            classes = new ArrayList<ClassTree>();
        }

        @Override
        public Void visitClass(ClassTree paramClassTree, Void paramP) {
            classes.add(paramClassTree);
            return super.visitClass(paramClassTree, paramP);
        }

        private String findClassName(MethodTree method) {
            String simpleName = "";
            String className = "";
            HashMap<String, Integer> annonym = new HashMap<String, Integer>();
            for (ClassTree classTree : classes) {

                simpleName = classTree.getSimpleName().toString();

                if (simpleName.isEmpty()) {
                    if (annonym.get(className) == null) {
                        annonym.put(className, 1);
                    } else {
                        annonym.put(className, annonym.get(className) + 1);
                    }
                } else {
                    className = String.valueOf(simpleName);
                }
                for (Tree tree : classTree.getMembers()) {
                    if (method.equals(tree)) {
                        if (simpleName.isEmpty()) {
                            simpleName = className + "$" + annonym.get(className);
                        }
                        return simpleName;
                    }
                }
            }
            return simpleName;
        }

        @Override
        public Void visitMethod(final MethodTree arg0, Void arg1) {
            long startPosition = sourcePositions.getStartPosition(compilationUnitTree, arg0);
            long startLine = lineMap.getLineNumber(startPosition);
            long endPosition = sourcePositions.getEndPosition(compilationUnitTree, arg0);
            long endLine = lineMap.getLineNumber(endPosition);

            if (lines != null) {
                for (DiffCoverage.SourceLine line : lines) {
                    if (line.line >= startLine && line.line <= endLine) {

                        if (changed.get(classFile) == null) {
                            changed.put(classFile, new ArrayList<MethodWithParams>());
                        }

                        String simpleClassName = findClassName(arg0);

                        String methodName = classFile.endsWith(simpleClassName + ".java") ? arg0.getName().toString() : "$" + simpleClassName + "." + arg0.getName();
                        List<String> params = new ArrayList<String>();

                        for (VariableTree vt : arg0.getParameters()) {
                            params.add(vt.getType().toString());
                        }
                        changed.get(classFile).add(new MethodWithParams(methodName, params));

                        return super.visitMethod(arg0, arg1);
                    }
                }
            }

            return super.visitMethod(arg0, arg1);
        }
    }


    @Override
    protected EnvHandler defineHandler() {
        EnvHandler envHandler = new EnvHandler(new OptionDescr[]{
                DSC_REPLACE_DIFF,
                DSC_ISSUE_TO_FIND,
                DSC_REPO_DIR,
                DSC_OUTPUT,
                DSC_HG_PATH
        }, this);

        SPIDescr spiDescr = new SPIDescr(DATA_PROCESSOR_SPI, DataProcessorSPI.class);
        spiDescr.addPreset("none", new StubSpi());
        spiDescr.setDefaultSPI(new DefaultDataProcessorSPI());
        envHandler.registerSPI(spiDescr);
        return envHandler;
    }

    @Override
    protected int handleEnv(EnvHandler envHandler) throws EnvHandlingException {
        String[] tail = envHandler.getTail();

        if (tail == null) {
            throw new EnvHandlingException("No input files. Please specify JCov data file and diff (mercurial) file.");
        }

        ArrayList<DataProcessorSPI> dataProcessors = envHandler.getSPIs(DataProcessorSPI.class);
        if (dataProcessors != null) {
            dataProcessorSPIs = dataProcessors.toArray(new DataProcessorSPI[dataProcessors.size()]);
        }

        replaceDiff = envHandler.getValue(DSC_REPLACE_DIFF);
        hg_path = envHandler.getValue(DSC_HG_PATH);
        hg_comment = envHandler.getValue(DSC_ISSUE_TO_FIND);
        hg_repo_dir = envHandler.getValue(DSC_REPO_DIR);
        output = envHandler.getValue(DSC_OUTPUT);

        resultFile = tail[0];
        Utils.checkFileNotNull(tail[0], "JCov datafile", Utils.CheckOptions.FILE_EXISTS, Utils.CheckOptions.FILE_ISFILE, Utils.CheckOptions.FILE_CANREAD);


        return SUCCESS_EXIT_CODE;
    }

    @Override
    protected String getDescr() {
        return null;
    }

    @Override
    protected String usageString() {
        return null;
    }

    @Override
    protected String exampleString() {
        return null;
    }

    class MethodWithParams {

        private String methodName;
        private List<String> methodParams;
        private boolean found = false;

        MethodWithParams(String methodName, List<String> methodParams) {
            this.methodName = methodName;
            this.methodParams = methodParams;
        }

        public List<String> getMethodParams() {
            return methodParams;
        }

        public void setMethodParams(List<String> methodParams) {
            this.methodParams = methodParams;
        }

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        public String getParamsRegex() {

            if (methodParams.size() == 0) {
                return "\\(\\)";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("\\(");

            for (int i = 0; i < methodParams.size(); i++) {

                String param = methodParams.get(i);
                if (param.equals("K") || param.equals("V")) {
                    param = "Object";
                }
                if (param.contains("<")) {
                    param = param.substring(0, param.indexOf("<"));
                }
                param = Pattern.quote(param);

                if (i < methodParams.size() - 1) {
                    sb.append(".*").append(param).append(",");
                } else {
                    sb.append(".*").append(param).append("\\)");
                }
            }

            return sb.toString();

        }

        public boolean isFound() {
            return found;
        }

        public void setFound(boolean found) {
            this.found = found;
        }

        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append(methodName).append("( ");
            for (String param : methodParams) {
                result.append(param).append(" ");
            }
            result.append(")");

            return result.toString();
        }
    }

    static OptionDescr DSC_HG_PATH = new OptionDescr("hgPath", new String[]{"hgPath", "hg"}, "", OptionDescr.VAL_SINGLE, "Path to the hg", "hg");
    static OptionDescr DSC_REPLACE_DIFF = new OptionDescr("replaceDiff", "Manage replacing", OptionDescr.VAL_SINGLE, "Set replacement pattern for diff filenames (e.g. to cut out \"src/classes\" you can specify -replaceDiff src/classes:)");
    static OptionDescr DSC_ISSUE_TO_FIND = new OptionDescr("issueToFind", new String[]{"issueToFind","if"}, "", OptionDescr.VAL_SINGLE, "Set issue identifier to find in repository history");
    static OptionDescr DSC_REPO_DIR = new OptionDescr("localRepo", new String[]{"localRepo", "lr"}, "", OptionDescr.VAL_SINGLE, "Path to the local repository");
    static OptionDescr DSC_OUTPUT = new OptionDescr("output", new String[]{"output", "o"}, "", OptionDescr.VAL_SINGLE,
                    "Output directory for generating HTML report.", "report");
}