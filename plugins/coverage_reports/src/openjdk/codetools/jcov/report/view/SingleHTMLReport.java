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
import openjdk.codetools.jcov.report.view.CoverageHierarchy;
import openjdk.codetools.jcov.report.view.HightlightFilteredReport;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SingleHTMLReport extends HightlightFilteredReport {

    private String title;
    private String header;

    public SingleHTMLReport(SourceHierarchy source, FileSet files, FileCoverage coverage,
                            String title, String header,
                            SourceFilter highlight, SourceFilter include) {
        super(source, files, new CoverageHierarchy(files.files(), coverage, highlight), highlight, include);
        this.title = title;
        this.header = header;
    }

    public void report(Path dest) throws Exception {
        try (BufferedWriter out = Files.newBufferedWriter(dest)) {
            var rout = new HtmlOut(out);
            out.write("<html><head>"); out.newLine();
            out.write("<title>" + title + "</title>"); out.newLine();
            out.write("<style>\n" +
                    ".highlight {\n" +
                    "  font-weight: bold;\n" +
                    "}\n" +
                    ".covered {\n" +
                    "  font-weight: bold;\n" +
                    "  background-color: palegreen;\n" +
                    "}\n" +
                    ".uncovered {\n" +
                    "  font-weight: bold;\n" +
                    "  background-color: salmon;\n" +
                    "}\n" +
                    ".filename {\n" +
                    "  font-weight: bold;\n" +
                    "  font-size: larger;\n" +
                    "}\n" +
                    "</style>"); out.newLine();
            out.write("</head><body>\n"); out.newLine();
            out.write(header + "\n"); out.newLine();
            out.write("<table><tbody>"); out.newLine();
            toc(rout, "");
            out.write("</tbody></table>"); out.newLine();
            out.write("<hr>"); out.newLine();
            code(rout, "");
            out.write("<body></html>");out.newLine();
        }
    }

    private class HtmlOut implements TOCOut, FileOut {
        private final BufferedWriter out;

        private HtmlOut(BufferedWriter out) {
            this.out = out;
        }

        @Override
        public void printFileLine(String s) throws IOException {
            out.write("<tr><td><a href=\"#" + s.replace('/', '_') + "\">" + s + "</a></td><td>" +
                    coverage().get(s) + "</td></tr>");
            out.newLine();
        }

        @Override
        public void printFolderLine(String s) throws IOException {
            Coverage cov = coverage().get(s);
            if (s.isEmpty()) s = "total";
            out.write("<tr><td><a href=\"#" + s.replace('/', '_') + "\">" + s + "</a></td><td>" +
                    cov + "</td></tr>");
            out.newLine();
        }

        @Override
        public void startFile(String file) throws IOException {
            out.write("<hr/>"); out.newLine();
            out.write("<a class=\"filename\" id=\"" +
                    file.replace('/', '_') + "\">" + file + ":" +
                    coverage().get(file) + "</a></br>"); out.newLine();
        }

        @Override
        public void startLineRange(LineRange range) throws IOException {
            out.write("<pre>"); out.newLine();
        }

        @Override
        public void printSourceLine(int lineNo, String line, boolean highlight, Coverage coverage) throws IOException {
            out.write("<a");
            if (coverage != null) {
                if (coverage.covered() > 0)
                    out.write(" class=\"covered\"");
                else
                    out.write(" class=\"uncovered\"");
            } else if (highlight) {
                out.write(" class=\"highlight\"");
            }
            out.write(">");
            out.write((lineNo + 1) + ": ");
            out.write(line.replaceAll("</?\\s*pre\\s*>", ""));
            out.write("</a>");
            out.newLine();
        }

        @Override
        public void endLineRange(LineRange range) throws IOException {
            out.write("</pre>"); out.newLine();
            out.write("<hr/>"); out.newLine();
        }

        @Override
        public void endFile(String s) throws IOException {
        }

        @Override
        public void startDir(String s) throws IOException {
            if (s.isEmpty()) s = "total";
            out.write("<a id=\"" + s.replace('/', '_') + "\"/>");
        }
    }
}
