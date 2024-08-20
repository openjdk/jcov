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

import com.sun.tdk.jcov.data.FileFormatException;
import com.sun.tdk.jcov.data.Result;
import com.sun.tdk.jcov.data.ScaleOptions;
import com.sun.tdk.jcov.instrument.DataRoot;
import com.sun.tdk.jcov.instrument.InstrumentationOptions;
import com.sun.tdk.jcov.instrument.InstrumentationOptions.MERGE;
import com.sun.tdk.jcov.io.ClassSignatureFilter;
import com.sun.tdk.jcov.io.Reader;
import com.sun.tdk.jcov.runtime.FileSaver;
import com.sun.tdk.jcov.tools.EnvHandler;
import com.sun.tdk.jcov.tools.JCovCMDTool;
import com.sun.tdk.jcov.tools.OptionDescr;
import com.sun.tdk.jcov.tools.ScaleCompressor;
import com.sun.tdk.jcov.tools.SimpleScaleCompressor;
import com.sun.tdk.jcov.util.Utils;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p> This tool allows to create one merged XML file from many ones </p> <p>
 * Merger can create "scaled" coverage. </p>
 *
 * @author Andrey Titov
 */
public class Merger extends JCovCMDTool {

    /**
     * Do not ignore any error
     */
    public final static String LOOSE_0 = "0";
    /**
     * Ignore access and fields size errors
     */
    public final static String LOOSE_1 = "1";
    /**
     * Ignore signature and methods size errors (+ LOOSE_1)
     */
    public final static String LOOSE_2 = "2";
    /**
     * Ignore checksum mismatch (+ LOOSE_2)
     */
    public final static String LOOSE_3 = "3";
    public final static String LOOSE_BLOCKS = "blocks";
    private String outTestList;
    private String output;
    private String skippedPath;
    private String template;
    private String[] srcs;
    private BreakOnError boe = BreakOnError.NONE;
    private boolean read_scales = false;
    private ClassSignatureFilter readFilter = null;
    private int loose_lvl = 0;
    private String[] include = new String[]{".*"};
    private String[] exclude = new String[]{""};
    private String[] m_include = new String[]{".*"};
    private String[] m_exclude = new String[]{""};
    private String[] fm = null;
    private boolean compress;
    private boolean sigmerge = false;
    private boolean addMissing = true;
    private boolean warningCritical = false;
    private static ScaleCompressor compressor = new SimpleScaleCompressor();
    private final static Logger logger;

    static {
        Utils.initLogger();
        logger = Logger.getLogger(Merger.class.getName());
    }

    /**
     * <p> There could be a number of errors in XML which JCov Merger can not
     * resolve:
     *
     * <ul> <li>empty, malformed (non-XML) or damaged (not full) file </li>
     * <li>coverage type in a file is different from previous file (e.g. in
     * first it was Method coverage and in the second - Block) </li> <li>a file
     * contains coverage data of another product version or another product
     * </li> </ul> </p> <p> BreakOnError.FILE means than Merger will find all
     * the problems in the first malformed file and then will stop merging. </p>
     * <p> BreakOnError.ERROR means that Merger will stop at the very first
     * occurred error. </p> <p> BreakOnError.NONE means that Merger will stop
     * only when all files would be processed regardless on errors. </p> <p>
     * BreakOnError.SKIP means that merger will not stop on malformed data but
     * will skip such files. The list of skipped files would be saved in Merge
     * object. </p>
     *
     * @see Merge
     * @see #setBreakOnError(com.sun.tdk.jcov.Merger.BreakOnError)
     */
    public static enum BreakOnError {

        FILE("file"), ERROR("error"), TEST("test"), NONE("none", "default"), SKIP("skip");
        private String[] aliases;

        private BreakOnError(String... str) {
            this.aliases = str;
        }

        public static BreakOnError fromString(String name) {
            for (BreakOnError boe : values()) {
                for (String s : boe.aliases) {
                    if (s.equalsIgnoreCase(name)) {
                        return boe;
                    }
                }
            }
            return null;
        }

        public static BreakOnError getDefault() {
            return NONE;
        }
    }

    /**
     * Merging info. Contains JCov results and template (if needed). After
     * merging contains result, skipped files list, error number and testlist
     */
    public static class Merge {

        private final Result[] jcovFiles;
        private final String template;
        private DataRoot result;
        private List<String> skippedFiles;
        private int errors;
        private int warnings;
        private String[] resultTestList;

        public Merge(Result[] files, String template) {
            this.jcovFiles = files;
            this.template = template;
        }

        /**
         * <p> Creates Merge object from a number of XML files, their testlists
         * filenames and template. </p>
         *
         * @param files Files to merge
         * @param testlists Testlists assigned to each file. Can be null. Some
         * elements of the array can be null. Length of <b>testlists</b> can be
         * lesser than <b>files</b>
         * @param template Template to merge these files with. If template is
         * set - Merger will merge only those elements which exist in the
         * template. <p>Can be null.</p>
         * @throws IOException
         */
        public Merge(String[] files, String[] testlists, String template) throws IOException {
            this.jcovFiles = new Result[files.length];
            for (int i = 0; i < files.length; ++i) {
                if (testlists.length > i && testlists[i] != null) {
                    jcovFiles[i] = new Result(files[i], testlists[i]);
                } else {
                    jcovFiles[i] = new Result(files[i]);
                }
            }
            this.template = template;
        }

        /**
         * <p> When Merger finds a problem during merge (e.g. empty or damaged
         * file) it can skip it if BreakOnError is set to SKIP. Names of skipped
         * files are stored in a list and can be accessed after the merge
         * finishes. </p>
         *
         * @return List of files which were skipped during the merge routine.
         * Returns null if merge was not started yet or no files were skipped
         * @see BreakOnError
         * @see Merger#setBreakOnError(com.sun.tdk.jcov.Merger.BreakOnError)
         *
         */
        public List<String> getSkippedFiles() {
            return skippedFiles;
        }

        /**
         * @return Result test list of merged data. Note that outTestList should
         * be passed to Merger in order to generate testlist.
         */
        public String[] getResultTestList() {
            return resultTestList;
        }

        /**
         * <p> When Merger finds a problem during merge (e.g. empty or damaged
         * file) it can skip it if BreakOnError is set to SKIP. Names of skipped
         * files are stored in a list and can be accessed after the merge
         * finishes. </p>
         *
         * @return Count of files skipped during merge routine.
         * @see BreakOnError
         * @see Merger#setBreakOnError(com.sun.tdk.jcov.Merger.BreakOnError)
         * @see #getSkippedFiles()
         */
        public int getSkippedCount() {
            if (skippedFiles == null) {
                return 0;
            }
            return skippedFiles.size();
        }

        /**
         * <p> There could be a number of errors in XML which JCov Merger can
         * not resolve:
         *
         * <ul> <li>empty, malformed (non-XML) or damaged (not full) file </li>
         * <li>coverage type in a file is different from previous file (e.g. in
         * first it was Method coverage and in the second - Block) </li> <li>a
         * file contains coverage data of another product version or another
         * product </li> </ul>
         *
         * To control Merger behavior on error occurrence use BreakOnError </p>
         *
         * @return Number of critical errors found during the merge.
         * @see BreakOnError
         * @see Merger#setBreakOnError(com.sun.tdk.jcov.Merger.BreakOnError)
         */
        public int getErrors() {
            return errors;
        }

        /**
         * <p> When Merger founds that a file contains coverage data collected
         * on another Java version it prints a warning. This warning can be
         * turned to error with <b>setWarningCritical()</b> method to prevent
         * JCov merge coverage collected on different Java platforms. </p>
         *
         * @return Number of warnings occurred during merge.
         * @see Merger#setWarningCritical(boolean)
         * @see BreakOnError
         * @see Merger#setBreakOnError(com.sun.tdk.jcov.Merger.BreakOnError)
         */
        public int getWarnings() {
            return warnings;
        }

        void addSkippedFile(String file) {
            if (skippedFiles == null) {
                skippedFiles = new LinkedList();
            }
            skippedFiles.add(file);
        }

        /**
         * @return Merge result as DataRoot object or null if Merger was not
         * started or failed (e.g. only one file found)
         */
        public DataRoot getResult() {
            return result;
        }
    }

    /**
     * legacy entry point
     *
     * @param args
     * @param logStream
     * @throws Exception
     */
    @Deprecated
    public static void innerMain(String args[], PrintStream logStream) throws Exception {
        Merger merger = new Merger();
        merger.run(args);
    }

    /**
     * CLI entry point. Do not use it as API entry point - System.exit is called
     * here
     *
     * @param args
     */
    public static void main(String args[]) {
        Merger merger = new Merger();
        try {
            int res = merger.run(args);
            System.exit(res);
        } catch (Exception ex) {
            System.exit(1);
        }
    }

///////// BODY /////////
    /**
     * Write merged data to a file
     *
     * @param merge merged data
     * @param outputPath path to write
     * @param outputTestList path to write testlist to
     * @param skippedPath path to write skipped files list to
     */
    public void write(Merge merge, String outputPath, String outputTestList, String skippedPath) throws IOException {
        FileSaver saver = FileSaver.getFileSaver(merge.result, outputPath, template, MERGE.MERGE, false, read_scales);
        if (compress) {
            merge.result.getScaleOpts().setScalesCompressed(true);
        }

        try {
            logger.log(Level.INFO, "- Writing result to {0}", outputPath);
            saver.saveResults(outputPath);
        } catch (Exception ex) {
            throw new IOException("Can't write result file", ex);
        }

        try {
            if (merge.resultTestList != null) {
                Utils.writeLines(outputTestList, merge.resultTestList);
            }
        } catch (IOException ex) {
            throw new IOException("Cannot create resulting test list: " + outputTestList + ": ", ex);
        }

        try {
            if (skippedPath != null && merge.getSkippedCount() > 0) {
                Utils.writeLines(skippedPath, merge.getSkippedFiles().toArray(new String[merge.getSkippedFiles().size()]));
            }
        } catch (IOException ex) {
            throw new IOException("Cannot create skipped files list: " + skippedPath + ": ", ex);
        }
    }

    /**
     * Merge and write data (api entry point)
     *
     * @param jcovFiles files to merge
     * @param outTestList path to write testlist to
     * @param output path to write
     * @param template template to use for merging (golden data)
     * @param skippedPath path to write skipped files list to
     * @return merged and written data in Merge object
     * @throws IOException
     */
    public Merge mergeAndWrite(String[] jcovFiles, String outTestList, String output, String template, String skippedPath) throws IOException {
        this.output = output;
        this.outTestList = outTestList;
        this.template = template;
        this.srcs = jcovFiles;
        this.skippedPath = skippedPath;

        logger.log(Level.INFO, "- Reading test lists");
        Result results[];
        try {
            results = initResults(jcovFiles, read_scales);
        } catch (IOException ex) {
            throw new IOException("Can't read test lists", ex);
        }
        Merge merge = new Merge(results, template);

        return mergeAndWrite(merge, outTestList, output, skippedPath);
    }

    /**
     * Merge and write data
     *
     * @param merge merging data
     * @param outTestList path to write testlist to
     * @param output to write
     * @param skippedPath path to write skipped files list to
     * @return merged data
     */
    public Merge mergeAndWrite(Merge merge, String outTestList, String output, String skippedPath) throws IOException {
        merge(merge, outTestList, false);

        // merge.result == null when errors occurred or BOE.TEST is set
        if (merge.result != null && output != null && !"".equals(output)) {
            write(merge, output, outTestList, skippedPath);
        }
        return merge;
    }

    /**
     * Merge data
     *
     * @param merge merging data
     * @param outTestList path to write testlist to (testlist data will not be
     * collected if null, this method does NOT write the file)
     */
    public void merge(Merge merge, String outTestList) {
        merge(merge, outTestList, false);
    }

    /**
     * Merge data
     *
     * @param merge merging data
     * @param outTestList path to write testlist to (testlist data will not be
     * collected if null, this method does NOT write the file)
     * @param ingoreOriginalScales files will be merged ignoring scales from
     * these files
     */
    public void merge(Merge merge, String outTestList, boolean ignoreOriginalScales) {

        readFilter = new ClassSignatureFilter(include, exclude, m_include, m_exclude, fm);
        DataRoot merged = null;
        DataRoot rNext = null;
        logger.log(Level.INFO, "- Merging started");
        int filesMerged = 0;
        String mergingRes;
        for (int i = 0; i < merge.jcovFiles.length; i++) {
            mergingRes = merge.jcovFiles[i].getResultPath();
            try {
                if (i == 0 && merge.template != null) {
                    logger.log(Level.INFO, "-- Reading jcov template {0}", merge.template);
                    merged = Reader.readXML(merge.template, read_scales, readFilter); // template should not contain scales
                    merged.getScaleOpts().setScaleSize(0); // template should not be counted in scales
                    if (sigmerge) {
                        merged.truncateToMethods(); // leaving only methods information in source XML
                    }

                    // scales are set to "0" 4 lines before
//                    if (merged.getScaleOpts().getScaleSize() > 1) {
//                        logger.log(Level.SEVERE, "Template {0} has not null scale size: found {1}; expected 1", new Object[]{merge.template, merged.getScaleOpts().getScaleSize()});
//                        merge.errors++;
//                        break;
//                    }
                    if (merged.getParams().isDynamicCollect()) {
                        logger.log(Level.SEVERE, "File {0} is dynamic collected coverage data and can't be used as template", merge.template);
                        merge.errors++;
                        break;
                    }
                    if (outTestList != null && read_scales && merged.getScaleOpts().getScaleSize() == 0) {
                        merged.createScales();
                    }

                    logger.log(Level.INFO, "-- Merging all with template {0}. Java version: {1}, generated {2}", new Object[]{merge.template, merged.getXMLHeadProperties().get("java.runtime.version"), merged.getXMLHeadProperties().get("coverage.created.date")});
                    filesMerged++; // allowing to merge 1 data file with template
                }

                logger.log(Level.FINE, "-- Reading jcov file {0}", mergingRes);

                rNext = Reader.readXML(mergingRes, read_scales, readFilter);

                if (outTestList != null) {
                    logger.log(Level.FINE, "-- Reading testlist for jcov file {0}", mergingRes);

                    if (ignoreOriginalScales) {
                        rNext.cleanScales();
                        rNext.createScales();
                    }

                    ScaleOptions scaleOpts = rNext.getScaleOpts();
                    String[] tlist = merge.jcovFiles[i].getTestList();

                    if (scaleOpts.getScaleSize() != tlist.length) {
                        logger.log(Level.SEVERE, "Inconsistent scale sizes: in file {0}: {1}; expected: {2}", new Object[]{mergingRes, scaleOpts.getScaleSize(), tlist.length});
                        if (boe == BreakOnError.SKIP) {
                            merge.addSkippedFile(mergingRes);
                            continue;
                        }
                        merge.errors++;
                        if (boe == BreakOnError.ERROR || boe == BreakOnError.FILE) {
                            break;
                        }
                    }
                    scaleOpts.setTestList(tlist);
                    scaleOpts.setOutTestList(outTestList);
                }

                if (merged == null) {
                    merged = rNext;
                    logger.log(Level.INFO, "-- Merging all with {0}. Java version: {1}, generated {2}", new Object[]{mergingRes, merged.getXMLHeadProperties().get("java.runtime.version"), merged.getXMLHeadProperties().get("coverage.created.date")});
                    if (sigmerge) {
                        merged.truncateToMethods(); // leaving only methods information in source XML
                    }

                    // we will lost fist test scales without creating them manually after reading file
                    if (read_scales && merged.getScaleOpts().getScaleSize() == 0) {
                        merged.createScales();
                    }

                    filesMerged++;
                    continue;
                }

                logger.log(Level.INFO, "-- Merging {0}", mergingRes);

                if (sigmerge) {
                    merged.mergeOnSignatures(rNext, addMissing);
                } else {
                    DataRoot.CompatibilityCheckResult localErrors;
                    localErrors = merged.checkCompatibility(rNext, loose_lvl, boe == BreakOnError.ERROR);
                    merge.errors += localErrors.errors;
                    merge.warnings += localErrors.warnings;
                    if (warningCritical) {
                        localErrors.errors += localErrors.warnings;
                    }

                    if (localErrors.errors != 0) {
                        if (boe == BreakOnError.SKIP) {
                            logger.log(Level.INFO, "-- File {0} has {1} critical error(s) and will be skipped", new Object[]{mergingRes, localErrors.errors});
                            merge.addSkippedFile(mergingRes);
                            continue; // just skip file without merging
                        }

                        if (boe == BreakOnError.FILE || boe == BreakOnError.ERROR) {
                            logger.log(Level.SEVERE, "-- File {0} has {1} critical error(s). Stopping merging process (break on error set)", new Object[]{mergingRes, localErrors.errors});
                            break;
                        }

                        logger.log(Level.INFO, "-- File {0} has {1} critical errors", new Object[]{mergingRes, localErrors.errors});
                    }

                    // all OK - merging
                    merged.merge(rNext, addMissing);
                }
                filesMerged++;
            } catch (FileFormatException ex) {
                merge.errors++;
                if (boe == BreakOnError.SKIP) {
                    logger.log(Level.SEVERE, "Skipping malformed xml file {0}: " + ex.getMessage(), new Object[]{mergingRes});
                    merge.addSkippedFile(mergingRes);
                    continue;
                }

                if (boe == BreakOnError.FILE || boe == BreakOnError.ERROR) {
                    if (i == 0 && merge.template != null) {
                        logger.log(Level.SEVERE, "Stopping on malformed xml template {0}: {1}", new Object[]{merge.template, ex.getMessage()});
                        break;
                    } else {
                        logger.log(Level.SEVERE, "Stopping on malformed xml file {0}: {1}", new Object[]{mergingRes, ex.getMessage()});
                        break;
                    }
                }

                logger.log(Level.SEVERE, "Malformed xml file: " + ex.getMessage(), mergingRes);
            } catch (Throwable th) {
                logger.log(Level.SEVERE, "Critical error while merging file " + mergingRes, th);
                th.printStackTrace();
                merge.errors++;
                return;
            }
        }


        if (outTestList != null) {
            if (merge.errors == 0 || boe == BreakOnError.SKIP) {
                logger.log(Level.INFO, "- Generating result testlist");
                if (merge.getSkippedCount() > 0) {
                    Result[] newres = new Result[merge.jcovFiles.length - merge.getSkippedCount()];
                    int newresI = 0;
                    outer:
                    for (int i = 0; i < merge.jcovFiles.length; ++i) {
                        for (int j = 0; j < merge.getSkippedCount(); ++j) {
                            if (merge.jcovFiles[i].getResultPath().equals(merge.getSkippedFiles().get(j))) {
                                continue outer;
                            }
                        }
                        newres[newresI++] = merge.jcovFiles[i];
                    }
                    merge.resultTestList = generateTestList(outTestList, newres, merged, merge.template != null);
                } else {
                    merge.resultTestList = generateTestList(outTestList, merge.jcovFiles, merged, merge.template != null);
                }
            } else {
                logger.log(Level.SEVERE, "Don't generating result testlist - errors occurred");
            }
        }

        if ((merge.errors > 0 && boe != BreakOnError.SKIP) || boe == BreakOnError.TEST) {
            logger.log(Level.SEVERE, "- Merging failed. Use \"-boe skip\" option to ignore bad files.");
            return; // DataRoot is not set to Merge
        }

        logger.log(Level.INFO, "- Merging complete");
        if (merge.getSkippedCount() > 0) {
            logger.log(Level.SEVERE, "- {0} files were skipped: ", merge.getSkippedCount());
            int i = 1;
            for (String skipped : merge.getSkippedFiles()) {
                logger.log(Level.SEVERE, "-- {0}", skipped);
            }
        }

        if (filesMerged < 2) {
            if (merge.getSkippedCount() > 0) {
                logger.log(Level.SEVERE, "- Not enough correct files to perform merging. Found {0} correct files and {1} were skipped due to errors while needed at least 2 correct files", new Object[]{filesMerged, merge.getSkippedCount()});
            } else {
                logger.log(Level.SEVERE, "- Not enough correct files to perform merging. Found {0} correct files while needed at least 2", filesMerged);
            }
            return;
        }

        merge.result = merged;
    }

    protected int run() throws Exception {
        Result results[];
        try {
            results = initResults(srcs, read_scales);
        } catch (IOException ex) {
            throw new IOException("Can't read test lists", ex);
        }

        Merge merge = new Merge(results, template);
        mergeAndWrite(merge, outTestList, output, skippedPath);

        if (boe == BreakOnError.TEST || warningCritical) {
            return merge.errors + merge.warnings;
        } else {
            return merge.errors;
        }
    }

    protected String usageString() {
        return "java com.sun.tdk.jcov.Merger [options] <filenames>";
    }

    protected String exampleString() {
        return "java -cp jcov.jar com.sun.tdk.jcov.Merger -include java.lang.* -scale -output merged.xml test1.xml test2.xml";
    }

    protected String getDescr() {
        return "merges several jcov data files";
    }

    private String[] generateTestList(String outTestList, Result[] results,
            Object root, boolean cleanTmpl) {

        // list of unique test names to be written to output file
        ArrayList testList = new ArrayList();

        // pairs of duplicate tests
        ArrayList pairs = new ArrayList();

        int cur = 0;
        int length = cleanTmpl ? results.length + 1 : results.length;
        for (int i = 0; i < length; i++) {

            String[] tlist;
            if (cleanTmpl) {
                if (i == 0) {
                    tlist = new String[]{results[0].getTestList()[0]};
                } else {
                    tlist = results[i - 1].getTestList();
                }
            } else {
                tlist = results[i].getTestList();
            }
            for (int j = 0; j < tlist.length; j++) {
                int found = testList.indexOf(tlist[j]);
                if (found < 0) {
                    testList.add(tlist[j]);
                } else {
                    pairs.add(new Utils.Pair(found, cur));
                }
                cur++;
            }
        }
        if (root instanceof DataRoot) {
            ((DataRoot) root).illuminateDuplicatesInScales(pairs);
        }
        return (String[]) testList.toArray(new String[testList.size()]);
    }

    /**
     * Decodes list of jcov file names. File name may be in one of three forms:
     * <ul> <li> a.jcov#test_a - jcov for the test "test_a" </li> <li>
     * b.jcov%test.lst - jcov for the tests listed in the file "test.lst"</li>
     * <li> c.jcov - jcov for the test "c.jcov"</li> </ul> Method removes "#..."
     * and "%..." from the names of jcov files. Returned structure will contain
     * test list for each passed jcov file.<br> Example: <br>
     * <code>
     * initTestList({"a.jcov#test_a", "b.jcov%test.lst", "c.jcov"})
     * will return:
     * {
     *   {"test_a"},
     *   {"t1, t2, t3, t4"}, // lines of test.lst file
     *   {"c.jcov"},
     * }
     * passed array will be transformed to {"a.jcov", "b.jcov", "c.jcov"}
     * </code>
     *
     * @param jcov_files - jcov file lists
     * @return array of String arrays, where i-th row is comprised of tests for
     * jcov_files[i]
     */
    public static Result[] initResults(String[] jcov_files, boolean initTestlists) throws IOException {

        Result[] results = new Result[jcov_files.length];
        for (int i = 0; i < jcov_files.length; i++) {
            results[i] = new Result();
            String str = jcov_files[i];
            int k = str.indexOf('%');
            if (k < 0) {
                k = str.indexOf('#');
                if (k < 0) {
                    results[i].setResultPath(str);
                    if (initTestlists) {
                        results[i].setDefaultName();
                    }
                } else {
                    results[i].setResultPath(str.substring(0, k));
                    if (initTestlists) {
                        results[i].setTestName(str.substring(k + 1));
                    }
                }
            } else {
                results[i].setResultPath(str.substring(0, k));
                if (initTestlists) {
                    results[i].readTestList(str.substring(k + 1));
                }
            }
        }
        return results;
    }

    /**
     *
     * @param path Read testlist from file
     * @return read lines as array
     * @throws IOException
     */
    public static String[] initTestList(String path) throws IOException {
        return Utils.readLines(path);
    }

    /**
     * Read testlist from file using first and last as borders in the files
     *
     * @param path file to read
     * @param first the first line to read
     * @param last the last line to read
     * @return read lines as array
     * @throws IOException
     */
    public static String[] initTestList(String path, int first, int last) throws IOException {
        return Utils.readLines(path, first, last);
    }

    /**
     * JCov Merger allows to specify source files among with testlist (eg
     * result.xml%testlist) or testname (eg result.xml#test1).
     *
     * @param jcov_file filename to parse
     * @return Initialized Result object
     * @throws IOException
     * @see Result
     */
    public static Result parseResultFromString(String jcov_file) throws IOException {
        Result res;
        int k = jcov_file.indexOf('%'); // testlist file separator
        if (k < 0) {
            k = jcov_file.indexOf('#'); // testname separator
            if (k > 0) {
                res = new Result(jcov_file.substring(0, k), new String[]{jcov_file.substring(k + 1)});
            } else {
                res = new Result(jcov_file);
            }
        } else {
            res = new Result(jcov_file.substring(0, k), jcov_file.substring(k + 1));
        }

        return res;
    }

///////// public API getters & setters /////////
    /**
     * Sets default (ClassSignatureAcceptor) acceptor using include, exclude and
     * fm.
     *
     * @param include
     * @param exclude
     * @param fm
     */
    public void setDefaultReadingFilter(String[] include, String[] exclude, String[] fm) {
        setDefaultReadingFilter(include, exclude, m_include, m_exclude, fm);
    }

    public void setDefaultReadingFilter(String[] include, String[] exclude, String[] m_include, String[] m_exclude, String[] fm) {
        this.include = include;
        this.exclude = exclude;
        this.m_include = m_include;
        this.m_exclude = m_exclude;
        this.fm = fm;
        this.readFilter = new ClassSignatureFilter(include, exclude, m_include, m_exclude, fm);
    }

    /**
     * Set ClassSignatureFilter used by this Merger to read the DataRoot
     *
     * @param filter
     */
    public void setReadingFilter(ClassSignatureFilter filter) {
        this.readFilter = filter;
    }

    public BreakOnError getBreakOnError() {
        return boe;
    }

    public void setBreakOnError(BreakOnError boe) {
        this.boe = boe;
    }

    public static ScaleCompressor getCompressor() {
        return compressor;
    }

    public static void setCompressor(ScaleCompressor compressor) {
        Merger.compressor = compressor;
    }

    public int getLoose_lvl() {
        return loose_lvl;
    }

    public void setLoose_lvl(int loose_lvl) {
        this.loose_lvl = loose_lvl;
    }

    public boolean isRead_scales() {
        return read_scales;
    }

    public void setRead_scales(boolean read_scales) {
        this.read_scales = read_scales;
    }

    public boolean isCompress() {
        return compress;
    }

    public void setCompress(boolean compress) {
        this.compress = compress;
    }

    public void setFilters(String[] include, String[] exclude, String[] classModifiers) {
        if (include == null) {
            include = new String[]{".*"};
        }
        this.include = include;
        if (exclude == null) {
            exclude = new String[]{""};
        }
        this.exclude = exclude;
        this.fm = classModifiers;
    }

    public void setClassModifiers(String[] fm) {
        this.fm = fm;
    }

    public void resetDefaults() {
        try {
            handleEnv_(defineHandler());
            setBreakOnError(BreakOnError.NONE);
            setRead_scales(false);
            setReadingFilter(null);
            setLoose_lvl(0);
            setCompress(false);
            warningCritical = false;
        } catch (EnvHandlingException ex) {
            // should not happen
        }
    }

    public String[] getExclude() {
        return exclude;
    }

    public String[] getInclude() {
        return include;
    }

    public String[] getFm() {
        return fm;
    }

    public boolean isAddMissing() {
        return addMissing;
    }

    public void setAddMissing(boolean addMissing) {
        this.addMissing = addMissing;
    }

    public void setSigmerge(boolean sigmerge) {
        this.sigmerge = sigmerge;
    }

    public boolean isSigmerge() {
        return sigmerge;
    }

    public boolean isWarningCritical() {
        return warningCritical;
    }

    public void setWarningCritical(boolean warningCritical) {
        this.warningCritical = warningCritical;
    }

///////// JCovTool implementation /////////
    @Override
    protected EnvHandler defineHandler() {
        return new EnvHandler(new OptionDescr[]{
                    DSC_OUTPUT,
                    DSC_FILELIST,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_INCLUDE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_EXCLUDE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_FM,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_INCLUDE_LIST,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_EXCLUDE_LIST,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_FM_LIST,
                    DSC_SCALE,
                    DSC_OUTPUT_TEST_LIST,
                    DSC_VERBOSE,
                    DSC_LOOSE,
                    DSC_COMPRESS,
                    DSC_BREAKERR,
                    DSC_WARNINGS,
                    DSC_TEMPLATE,
                    DSC_SKIPPED
                }, this);
    }

    private int handleEnv_(EnvHandler opts) throws EnvHandlingException {
        if (opts.isSet(DSC_FILELIST)) {
            String val = opts.getValue(DSC_FILELIST);
            int start = -1;
            int end = -1;
            String file = val;
            int ind = val.indexOf(",");
            if (ind != -1) {
                file = val.substring(0, ind);
                String rest = val.substring(ind + 1);
                int second_ind = rest.indexOf(",");
                if (second_ind == -1) {
                    start = Integer.parseInt(rest);
                } else {
                    start = Integer.parseInt(rest.substring(0, second_ind));
                    end = Integer.parseInt(rest.substring(second_ind + 1, rest.length()));
                }
            }
            Utils.checkFileNotNull(file, "filelist filename",
                    Utils.CheckOptions.FILE_EXISTS, Utils.CheckOptions.FILE_CANREAD, Utils.CheckOptions.FILE_ISFILE);
            try {
                srcs = Utils.readLines(file, start, end);
            } catch (IOException ex) {
                throw new EnvHandlingException("Can't read filelist " + val, ex);
            }
        }

        include = InstrumentationOptions.handleInclude(opts);
        exclude = InstrumentationOptions.handleExclude(opts);
        m_include = InstrumentationOptions.handleMInclude(opts);
        m_exclude = InstrumentationOptions.handleMExclude(opts);
        fm = InstrumentationOptions.handleFM(opts);

        read_scales = opts.isSet(DSC_SCALE);
        if (opts.isSet(DSC_LOOSE)) {
            String loose = opts.getValue(DSC_LOOSE);
            if (LOOSE_BLOCKS.equals(loose)) {
                sigmerge = true;
            } else {
                try {
                    loose_lvl = Utils.checkedToInt(loose, "loose level", Utils.CheckOptions.INT_POSITIVE);
                } catch (NumberFormatException nfe) {
                    throw new EnvHandlingException("Can't parse loose level " + loose, nfe);
                }
            }
        }

        outTestList = null;
        if (opts.isSet(DSC_OUTPUT_TEST_LIST)) {
            outTestList = opts.getValue(DSC_OUTPUT_TEST_LIST);
            Utils.checkFileNotNull(outTestList, "output testlist filename",
                    Utils.CheckOptions.FILE_CANWRITE, Utils.CheckOptions.FILE_NOTISDIR, Utils.CheckOptions.FILE_PARENTEXISTS);
            read_scales = true;
        }
        skippedPath = opts.getValue(DSC_SKIPPED);

        String boestr = opts.getValue(DSC_BREAKERR);
        if ("error".equalsIgnoreCase(boestr)) {
            boe = BreakOnError.ERROR;
        } else if ("test".equalsIgnoreCase(boestr)) {
            boe = BreakOnError.TEST;
        } else if ("file".equalsIgnoreCase(boestr)) {
            boe = BreakOnError.FILE;
        } else if ("skip".equalsIgnoreCase(boestr)) {
            boe = BreakOnError.SKIP;
        } else {
            boe = BreakOnError.NONE;
        }

        template = opts.getValue(DSC_TEMPLATE);
        Utils.checkFileCanBeNull(template, "template filename",
                Utils.CheckOptions.FILE_EXISTS, Utils.CheckOptions.FILE_ISFILE, Utils.CheckOptions.FILE_CANREAD);

        addMissing = template == null;

        output = opts.getValue(DSC_OUTPUT);
        Utils.checkFileNotNull(output, "output filename",
                Utils.CheckOptions.FILE_NOTISDIR, Utils.CheckOptions.FILE_PARENTEXISTS, Utils.CheckOptions.FILE_CANWRITE);

        compress = opts.isSet(DSC_COMPRESS);
        if (opts.isSet(DSC_VERBOSE)) {
            logger.setLevel(Level.INFO);
        }

        warningCritical = opts.isSet(DSC_WARNINGS);

        return SUCCESS_EXIT_CODE;
    }

    @Override
    protected int handleEnv(EnvHandler opts) throws EnvHandlingException {
        srcs = opts.getTail();

        int code = handleEnv_(opts);

        if (srcs == null || srcs.length == 0) { // srcs could be set from filelist
            throw new EnvHandlingException("No input files specified");
        }
        return code;
    }
    final static OptionDescr DSC_SKIPPED =
            new OptionDescr("outSkipped", "path for list of skipped files", OptionDescr.VAL_SINGLE,
            "Sets path to the list of files that were skipped during the merge by '-boe skip' option");
    final static OptionDescr DSC_TEMPLATE =
            new OptionDescr("template", new String[]{"tmpl", "t"}, "template path", OptionDescr.VAL_SINGLE,
            "Sets path to the template used for merging. Only data in template will be merged");
    final static OptionDescr DSC_BREAKERR =
            new OptionDescr("breakonerror", new String[]{"boe", "onerror"}, "break on error",
            new String[][]{{"file", "break when finished merging first file with errors"},
                {"error", "break on the first occurred error"},
                {"test", "don't break on any error and don't write result file"},
                {"skip", "merge all correct files. Attention: first jcov file is always considered to be correct. Use '-template' option to ensure correct results."},
                {"none", "don't break on any error and write result file if all passed. Neither result file neither testlist would be written if any error will occur."}},
            "Sets type of handling errors in merging process. Can be 'none', 'test', 'error', 'skip' and 'file'", "file");
    final static OptionDescr DSC_OUTPUT =
            new OptionDescr("merger.output", new String[]{"output", "o"}, "output file", OptionDescr.VAL_SINGLE,
            "Output file for generating new profiler data file.", "merged.xml");
    final static OptionDescr DSC_FILELIST =
            new OptionDescr("filelist", "file to read jcov input files from", OptionDescr.VAL_SINGLE,
            "Text file to read jcov data files for merge from. One file name per line.\n"
            + "The option allows to specify a range of lines to be read:\n"
            + "-filelist=<file>,first_line,last_line");
    final static OptionDescr DSC_SCALE =
            new OptionDescr("scale", "process/generate test scales",
            "Process/generate test scale that lets find tests covering specific\n"
            + "code sections (no scale by default)");
    final static OptionDescr DSC_COMPRESS =
            new OptionDescr("compress", "compress test scales",
            "Compress test scales.");
    final static OptionDescr DSC_VERBOSE =
            new OptionDescr("verbose", new String[]{"v"}, "verbose mode", "Enables verbose mode");
    final static OptionDescr DSC_OUTPUT_TEST_LIST =
            new OptionDescr("outTestList", "", OptionDescr.VAL_SINGLE,
            "Generate summary test list. Test names will be extracted from the filename\n"
            + "operands, which may be specified in one of the following ways:\n"
            + "    a.jcov#test_a    coverage file for 'test_a'\n"
            + "    b.jcov%test.lst  coverage file for tests listed in 'test.lst'\n"
            + "    testC/c.jcov     coverage file for 'testC/c.jcov'");
    final static OptionDescr DSC_LOOSE =
            new OptionDescr("loose",
            "looseness level",
            new String[][]{
                {LOOSE_0, "none (strict)"},
                {LOOSE_1, "moderate"},
                {LOOSE_2, "high"},
                {LOOSE_3, "highest"},
                {LOOSE_BLOCKS, "drop blocks"}
            },
            "Sets the \"looseness\" level of merger operation.\n"
            + "0 - default strict mode. All errors are treated as fatal.\n"
            + "1 - warning instead of an error when merging two classes "
            + "with the same name and common timestamp, whose coverage "
            + "item counts don't match.\n"
            + "2 - warning instead of an error when merging two classes "
            + "with the same name and the same arbitrary timestamp, "
            + "whose coverage item counts don't match.\n"
            + "Also allows to merge without warning DATA: B and DATA: C classes\n"
            + "3 - warning instead of any error\n"
            + "blocks - all blocks information would be dropped. JCov data will be "
            + "truncated to method coverage without any checks in block structure and then merged by signatures",
            LOOSE_0);
    final static OptionDescr DSC_WARNINGS =
            new OptionDescr("critwarn", "", OptionDescr.VAL_NONE, "Count warnings as errors",
            "When set JCov will process warnings (e.g. java version missmatch) just as errors");
}
