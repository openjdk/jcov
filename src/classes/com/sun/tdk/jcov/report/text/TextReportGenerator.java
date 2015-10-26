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
package com.sun.tdk.jcov.report.text;

import com.sun.tdk.jcov.report.ClassCoverage;
import com.sun.tdk.jcov.report.DataType;
import com.sun.tdk.jcov.report.FieldCoverage;
import com.sun.tdk.jcov.report.MethodCoverage;
import com.sun.tdk.jcov.report.PackageCoverage;
import com.sun.tdk.jcov.report.ProductCoverage;
import com.sun.tdk.jcov.report.ReportGenerator;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;

import com.sun.tdk.jcov.tools.JcovStats;
import com.sun.tdk.jcov.util.Utils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used to generate text-form reports based on the contents of a JcovFileImage
 * object. Html-form report generators are also available.
 *
 * @see com.sun.tdk.jcov.filedata.JcovFileImage
 * @see HtmlReportGenerator
 * @author Konstantin Bobrovsky
 */
public class TextReportGenerator implements ReportGenerator {

    public final static String sccsVersion = "%I% $LastChangedDate: 2013-10-14 18:13:10 +0400 (Mon, 14 Oct 2013) $";
    /**
     * format specifier, as used in command-line option
     */
    public final static String FMT_TEXT = "text";
    /**
     * default report file name
     */
    public static final String DEFAULT_REPORT_NAME = "report.txt";
    /**
     * where to write the report
     */
    protected PrintWriter out;
    /**
     * Error/message log
     */
    private static String[] spaces = new String[]{"", " ", "  ", "   ", "    ", "     ", "      "};
    private static String[] dashes = new String[]{"", " ", ". ", ".. ", "... ", ".... ", "..... "};
    private static final Logger logger;
    private boolean generateShortFormat;
    private boolean showMethods;
    private boolean showBlocks;
    private boolean showBranches;
    private boolean showLines;
    private boolean showFields;

    static {
        Utils.initLogger();
        logger = Logger.getLogger(TextReportGenerator.class.getName());
        logger.setLevel(Level.WARNING);
    }

    @Override
    public void init(String outputFile) throws IOException {
        outputFile = detectOutputTextFileName(outputFile);
        if (outputFile == null) {
            out = new PrintWriter(new OutputStreamWriter(System.out, Charset.defaultCharset()));
        } else {
            out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFile), Charset.defaultCharset()));
        }
    }
    Options options;

    @Override
    public void generateReport(ProductCoverage coverage, Options options) {
        this.options = options;

        out.println("Coverage Report: ");
        String sumFormat = "ALL: classes:%1$s;";
        String pkgFormat = "PKG%1s: %2s classes:%3$s;";
        String clsFormat = "CLS%1s: %2$s ";
        String mthFormat = "MTH%1s: %2$s hits: %3$s";
        String fldFormat = "FLD%1s: %2$s hits: %3$s";

        if (showMethods) {
            sumFormat += " methods:%2$s;";
            pkgFormat += " methods:%4$s;";
            clsFormat += " methods:%3$s;";
        }

        if (showBlocks) {
            sumFormat += " blocks:%3$s;";
            pkgFormat += " blocks:%5$s;";
            clsFormat += " blocks:%4$s;";
            mthFormat += " blocks:%4$s;";
        }

        if (showBranches) {
            sumFormat += " branches:%4$s;";
            pkgFormat += " branches:%6$s;";
            clsFormat += " branches:%5$s;";
            mthFormat += " branches:%5$s;";
        }

        if (showLines) {
            sumFormat += " lines:%5$s;";
            pkgFormat += " lines:%7$s;";
            clsFormat += " lines:%6$s;";
            mthFormat += " lines:%6$s;";
        }

        out.println(String.format(sumFormat,
                coverage.getCoverageString(DataType.CLASS, coverage.isAncFiltersSet()),
                coverage.getData(DataType.METHOD).add(coverage.getData(DataType.FIELD)).toString(),
                coverage.getCoverageString(DataType.BLOCK, coverage.isAncFiltersSet()),
                coverage.getCoverageString(DataType.BRANCH, coverage.isAncFiltersSet()),
                coverage.getCoverageString(DataType.LINE, coverage.isAncFiltersSet())));
        for (PackageCoverage pkgCov : coverage) {
            out.println(String.format(pkgFormat,
                    pkgCov.isCovered() ? "+" : "-",
                    pkgCov.getName(),
                    pkgCov.getCoverageString(DataType.CLASS, coverage.isAncFiltersSet()),
                    pkgCov.getData(DataType.METHOD).add(pkgCov.getData(DataType.FIELD)).toString(),
                    pkgCov.getCoverageString(DataType.BLOCK, coverage.isAncFiltersSet()),
                    pkgCov.getCoverageString(DataType.BRANCH, coverage.isAncFiltersSet()),
                    pkgCov.getCoverageString(DataType.LINE, coverage.isAncFiltersSet())));
            for (ClassCoverage clsCov : pkgCov.getClasses()) {
                out.println(String.format(clsFormat,
                        clsCov.isCovered() ? "+" : "-",
                        clsCov.getName(),
                        clsCov.getData(DataType.METHOD).add(clsCov.getData(DataType.FIELD)).toString(),
                        clsCov.getCoverageString(DataType.BLOCK, coverage.isAncFiltersSet()),
                        clsCov.getCoverageString(DataType.BRANCH, coverage.isAncFiltersSet()),
                        clsCov.getCoverageString(DataType.LINE, coverage.isAncFiltersSet())));
                if (!generateShortFormat && showMethods) {
                    for (MethodCoverage mthCov : clsCov.getMethods()) {
                        out.println(String.format(mthFormat,
                                mthCov.isCovered() ? "+" : "-",
                                mthCov.getName() + mthCov.getSignature(),
                                mthCov.getHitCount(),
                                mthCov.getCoverageString(DataType.BLOCK, coverage.isAncFiltersSet()),
                                mthCov.getCoverageString(DataType.BRANCH, coverage.isAncFiltersSet()),
                                mthCov.getCoverageString(DataType.LINE, coverage.isAncFiltersSet())));
                    }
                }
                if (!generateShortFormat && showFields) {
                    for (FieldCoverage fldCov : clsCov.getFields()) {
                        out.println(String.format(fldFormat,
                                fldCov.isCovered() ? "+" : "-",
                                fldCov.getName(),
                                fldCov.getHitCount()));
                    }
                }
                out.println();
            }
        }


        out.flush();
        out.close();
    }

    /**
     * prints given three numbers
     */
    private static void printTrio(PrintWriter out, long num1, long num2, float prcnt, int align) {
        boolean use_dashes = false;
        if (align < 0) {
            align = -align;
            use_dashes = true;
        }
        String s1 = Long.toString(num1);
        String s2 = Long.toString(num2);
        String s3 = num1 == 0 ? "N/A" : (prcnt < 0.01 ? "0.0" : Float.toString(prcnt * 100.0f));
        String[] fill = use_dashes ? dashes : spaces;

        out.print(fill[align - s1.length()]);
        out.print(s1 + " ");
        out.print(fill[align - s2.length()]);
        out.print(s2 + " ");

        if (num1 > 0) {
            int dot_ind = s3.indexOf((int) '.');
            out.print(fill[3 - dot_ind]);
            out.print(s3.substring(0, dot_ind + 2) + "% ");
        } else {
            out.print(fill[6 - s3.length()]);
            out.print(s3 + " ");
        }
    }

    public static void printStats(PrintWriter out, JcovStats stats) {
        printStats(out, stats, false);
    }

    /**
     * prints given coverage statistics
     */
    public static void printStats(PrintWriter out, JcovStats stats, boolean use_dashes) {
        int align = use_dashes ? -7 : 7;
        out.print(" ");
        printTrio(out, stats.methods_tot, stats.methods_cov, stats.method_cvg, align);
        printTrio(out, stats.blocks_tot, stats.blocks_cov, stats.block_cvg, align);
        printTrio(out, stats.branches_tot, stats.branches_cov, stats.branch_cvg, align);
    }

    /**
     * prints class/method modifiers
     */
    private void printModifiers(String[] modifiers) {
        if (modifiers == null || modifiers.length == 0) {
            return;
        }
        out.print("[");
        for (int i = 0; i < modifiers.length; i++) {
            out.print(modifiers[i]);
            if (i < modifiers.length - 1) {
                out.print(" ");
            }
        }
        out.print("]");
    }

    /**
     * Detects filename of the text report. Creates all necessary directies.
     *
     * @param value - value specified via "-output" option
     * @return
     */
    private static String detectOutputTextFileName(String value) {
        if (value == null) {
            return null;
        }
        File f = new File(value);
        if (f.exists()) {
            if (f.isDirectory()) {
                return value + File.separator + DEFAULT_REPORT_NAME;
            } else {
                return value;
            }
        }
        File dir = f;
        String result = value;
        if (value.endsWith(File.separator) || value.endsWith("/")) {
            result = dir.getPath() + File.separator + DEFAULT_REPORT_NAME;
        } else {
            dir = f.getParentFile();
        }
        if (dir != null) {
            dir.mkdirs();
        }
        return result;
    }

    public void setGenerateShortFormat(boolean generateShortFormat) {
        this.generateShortFormat = generateShortFormat;
    }

    public void setShowMethods(boolean showMethods) {
        this.showMethods = showMethods;
    }

    public void setShowBlocks(boolean showBlocks) {
        this.showBlocks = showBlocks;
    }

    public void setShowBranches(boolean showBranches) {
        this.showBranches = showBranches;
    }

    public void setShowFields(boolean showFields) {
        this.showFields = showFields;
    }

    public void setShowLines(boolean showLines) {
        this.showLines = showLines;
    }
}