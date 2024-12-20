/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
package openjdk.codetools.jcov.report.view;

import openjdk.codetools.jcov.report.Coverage;
import openjdk.codetools.jcov.report.FileCoverage;
import openjdk.codetools.jcov.report.FileItems;
import openjdk.codetools.jcov.report.FileSet;
import openjdk.codetools.jcov.report.LineRange;
import openjdk.codetools.jcov.report.filter.SourceFilter;
import openjdk.codetools.jcov.report.source.SourceHierarchy;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Implements a hierarchical report in a single text file.
 */
public class TextReport {
    private static final String SEPARATOR_LINE = "-".repeat(80);
    private final String header;
    private final FilteredReport theReport;

    public TextReport(SourceHierarchy source, FileSet files,
                      FileCoverage coverage,
                      String header, SourceFilter filter) {
        theReport = new FilteredReport.Builder()
                .setSource(source)
                .setFiles(files)
                .setItems(null)
                .setCoverage(new CoverageHierarchy(files.files(), source, coverage, filter))
                .setInclude(filter)
                .report();
        this.header = header;
    }

    public void report(Path dest) throws Exception {
        try (BufferedWriter out = Files.newBufferedWriter(dest)) {
            out.write(header); out.newLine(); out.newLine();
            theReport.report(new FilteredReport.FileOut() {
                @Override
                public void startFile(String file) throws Exception {
                    out.write(file + " " + theReport.coverage().get(file));
                    out.newLine();
                }

                @Override
                public void startItems() throws Exception {

                }

                @Override
                public void printItem(FileItems.FileItem fi) throws Exception {

                }

                @Override
                public void endItems() throws Exception {

                }

                @Override
                public void startLineRange(LineRange range) throws Exception {

                }

                @Override
                public void printSourceLine(int line, String s, Coverage coverage, List<FileItems.FileItem> items) throws Exception {

                }

                @Override
                public void endLineRange(LineRange range) throws Exception {

                }

                @Override
                public void endFile(String s) throws Exception {

                }

                @Override
                public void endFolder(String s) {

                }

                @Override
                public void startFolder(String folder) throws Exception {
                    out.write((folder.isEmpty() ? "total" : folder) + " " + theReport.coverage().get(folder));
                    out.newLine();
                }
            });
            theReport.report(new FilteredReport.FileOut() {
                @Override
                public void startFile(String file) throws Exception {
                    out.write("file:" + file + " " + theReport.coverage().get(file));
                    out.newLine();
                    out.write(SEPARATOR_LINE);
                    out.newLine();
                }

                @Override
                public void startLineRange(LineRange range) throws Exception {
                }

                @Override
                public void printSourceLine(int line, String s, Coverage coverage,
                                            List<FileItems.FileItem> items) throws Exception {
                    out.write((line + 1) + ":" + (coverage == null ? " " : coverage.covered() > 0 ? "+" : "-") + s);
                    out.newLine();
                }

                @Override
                public void endLineRange(LineRange range) throws Exception {
                    out.write(SEPARATOR_LINE);
                    out.newLine();
                }

                @Override
                public void endFile(String s) throws Exception {

                }

                @Override
                public void endFolder(String s) {

                }

                @Override
                public void startFolder(String s) throws Exception {

                }

                @Override
                public void startItems() {
                    throw new RuntimeException("This shoudl not happen");
                }

                @Override
                public void printItem(FileItems.FileItem fi) {
                    throw new RuntimeException("This shoudl not happen");
                }

                @Override
                public void endItems() {
                    throw new RuntimeException("This shoudl not happen");
                }
            });
        }
    }

    public static class Builder {
        private SourceHierarchy source;
        private FileSet files;
        private FileCoverage coverage;
        private String header;
        private SourceFilter filter;

        public Builder source(SourceHierarchy source) {
            this.source = source;
            return this;
        }

        public Builder files(FileSet files) {
            this.files = files;
            return this;
        }

        public Builder coverage(FileCoverage coverage) {
            this.coverage = coverage;
            return this;
        }

        public Builder header(String header) {
            this.header = header;
            return this;
        }

        public Builder filter(SourceFilter filter) {
            this.filter = filter;
            return this;
        }

        public TextReport report() {
            return new TextReport(source, files, coverage, header, filter);
        }
    }
}
