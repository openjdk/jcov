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
import openjdk.codetools.jcov.report.FileSet;
import openjdk.codetools.jcov.report.LineRange;
import openjdk.codetools.jcov.report.filter.SourceFilter;
import openjdk.codetools.jcov.report.source.SourceHierarchy;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Implements a hierarchical report in a single text file.
 */
public class TextReport extends HightlightFilteredReport {

    private String header;

    public TextReport(SourceHierarchy source, FileSet files, FileCoverage coverage, String header, SourceFilter filter) {
        super(source, files, new CoverageHierarchy(files.files(), source, coverage, filter), filter, filter);
        this.header = header;
    }

    public void report(Path dest) throws Exception {
        try (BufferedWriter out = Files.newBufferedWriter(dest)) {
            out.write(header); out.newLine(); out.newLine();
            super.toc(new TOCOut() {
                @Override
                public void printFileLine(String file) throws Exception {
                    out.write(file + " " + coverage().get(file));
                    out.newLine();
                }

                @Override
                public void printFolderLine(String folder, Coverage cov) throws Exception {
                    out.write((folder.isEmpty() ? "total" : folder) + " " + cov);
                    out.newLine();
                }
            }, "");
            code(new FileOut() {
                @Override
                public void startFile(String file) throws Exception {
                    out.write("file:" + file + " " + coverage().get(file));
                    out.newLine();
                    out.write("--------------------------------------------------------------------------");
                    out.newLine();
                }

                @Override
                public void startLineRange(LineRange range) throws Exception {
                }

                @Override
                public void printSourceLine(int line, String s, boolean highlight, Coverage coverage) throws Exception {
                    out.write((line + 1) + ":" + (coverage == null ? " " : coverage.covered() > 0 ? "+" : "-") + s);
                    out.newLine();
                }

                @Override
                public void endLineRange(LineRange range) throws Exception {
                    out.write("--------------------------------------------------------------------------");
                    out.newLine();
                }

                @Override
                public void endFile(String s) throws Exception {

                }

                @Override
                public void startDir(String s, Coverage cov) throws Exception {

                }
            }, "");
        }
    }
}
