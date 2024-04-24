/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates. All rights reserved.
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.lang.String.format;

/**
 * Implements a hierarchical report in a single html file.
 */
public class SingleHTMLReport {

    static final String CSS = """
                    .sortable {
                    }
                    .context {
                      font-weight: lighter;
                    }
                    .highlight {
                      font-weight: bold;
                    }
                    .covered {
                      font-weight: bold;
                      background-color: palegreen;
                    }
                    .uncovered {
                      font-weight: bold;
                      background-color: salmon;
                    }
                    .filename {
                      font-weight: bold;
                      font-size: larger;
                    }
                    .item_good {
                      background-color: palegreen;
                    }
                    .item_so_so {
                      background-color: yellow;
                    }
                    .item_not_so_good {
                      background-color: salmon;
                    }
                    .item_ignore {
                      background-color: lightgrey;
                    }
                    .item_none {
                    }
                    """;
    private final String title;
    private final String header;
    private final FilteredReport theReport;
    private final SourceFilter highlight;
    private final SourceHierarchy source;

    public SingleHTMLReport(SourceHierarchy source, FileSet files, FileCoverage coverage,
                            FileItems items, String title, String header,
                            SourceFilter highlight, SourceFilter include) {
        theReport = new FilteredReport.Builder()
                .setSource(source)
                .setFiles(files)
                .setItems(items)
                .setCoverage(new CoverageHierarchy(files.files(), source, coverage, highlight))
                .setInclude(include)
                .report();
        this.title = title;
        this.header = header;
        this.highlight = highlight;
        this.source = source;
    }

    public void report(Path dest) throws Exception {
        try (BufferedWriter out = Files.newBufferedWriter(dest)) {
            out.write("<html><head>"); out.newLine();
            out.write("<title>" + title + "</title>"); out.newLine();
            out.write("<style>\n" +
                    CSS +
                    "</style>"); out.newLine();
            out.write("</head><body>\n"); out.newLine();
            out.write(header + "\n"); out.newLine();
            out.write("<table><tbody>"); out.newLine();

            theReport.report(new HtmlTOCOut(out));
            out.write("</tbody></table>"); out.newLine();
            out.write("<hr>"); out.newLine();
            theReport.report(new HtmlOut(out));
            out.write("<body></html>");out.newLine();
        }
    }

    private class HtmlTOCOut implements FilteredReport.FileOut {
        private final BufferedWriter out;

        private HtmlTOCOut(BufferedWriter out) {
            this.out = out;
        }

        @Override
        public void startFile(String s) throws IOException {
            var cov = theReport.coverage().get(s);
            out.write("<tr><td><a href=\"#" + s.replace('/', '_') + "\">" + s + "</a></td><td>" +
                    cov + "</td></tr>");
            out.newLine();
        }

        @Override
        public void startItems() throws Exception {}

        @Override
        public void printItem(FileItems.FileItem fi) throws Exception {}

        @Override
        public void endItems() throws Exception {}

        @Override
        public void startLineRange(LineRange range) throws Exception {}

        @Override
        public void printSourceLine(int line, String s, Coverage coverage, List<FileItems.FileItem> items) throws Exception {}

        @Override
        public void endLineRange(LineRange range) throws Exception {}

        @Override
        public void endFile(String s) throws Exception {}

        @Override
        public void endFolder(String s) {}

        @Override
        public void startFolder(String s) throws IOException {
            Coverage cov = theReport.coverage().get(s);
            if (s.isEmpty()) s = "total";
            out.write("<tr><td><a href=\"#" + s.replace('/', '_') + "\">" + s + "</a></td><td>" +
                    cov + "</td></tr>");
            out.newLine();
        }
    }
    private class HtmlOut implements FilteredReport.FileOut {
        private final BufferedWriter out;
        private final FilteredReport.FilterHighlighter highlighter;
        private String lastFile;

        private HtmlOut(BufferedWriter out) {
            this.highlighter = new FilteredReport.FilterHighlighter(source, highlight);
            this.out = out;
        }

        @Override
        public void startFile(String file) throws IOException {
            out.write("<hr/>"); out.newLine();
            out.write("<a class=\"filename\" id=\"" +
                    file.replace('/', '_') + "\">" + file + ":" +
                    theReport.coverage().get(file) + "</a></br>"); out.newLine();
            lastFile = file;
        }

        @Override
        public void startLineRange(LineRange range) throws IOException {
            out.write("<pre>"); out.newLine();
        }

        @Override
        public void printSourceLine(int lineNo, String line, Coverage coverage,
                                    List<FileItems.FileItem> items) throws IOException {
            out.write("<a");
            if (coverage != null) {
                if (coverage.covered() > 0)
                    out.write(" class=\"covered\"");
                else
                    out.write(" class=\"uncovered\"");
            } else if (highlighter.isHighlighted(lastFile, lineNo + 1)) {
                out.write(" class=\"highlight\"");
            } else
                out.write(" class=\"context\"");
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
        public void endFolder(String s) {

        }

        @Override
        public void startFolder(String s) throws IOException {
            if (s.isEmpty()) s = "total";
            out.write("<a id=\"" + s.replace('/', '_') + "\"/>");
        }

        @Override
        public void startItems() throws Exception {
            out.write("<table>"); out.newLine();
        }

        @Override
        public void printItem(FileItems.FileItem fi) throws IOException, Exception {
//            out.write(format("<tr><td>%s</td></tr>", fi.item())); out.newLine();
            out.write(format("<tr><td><pre><a id=\"item_%s\" class=\"%s\">%s</a></pre></td>",
                    fi.item(), MultiHTMLReport.HTML_COLOR_CLASSES.get(fi.quality()), fi.item()));
            out.write("</tr>");
            out.newLine();
        }

        @Override
        public void endItems() throws Exception {
            out.write("</table>"); out.newLine();
        }
    }

    public static class Builder {
        private SourceHierarchy source;
        private FileSet files;
        private FileCoverage coverage;
        private String title;
        private String header;
        private SourceFilter highlight;
        private SourceFilter include;
        private FileItems items;

        public Builder setSource(SourceHierarchy source) {
            this.source = source;
            return this;
        }

        public Builder setFiles(FileSet files) {
            this.files = files;
            return this;
        }

        public Builder setCoverage(FileCoverage coverage) {
            this.coverage = coverage;
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setHeader(String header) {
            this.header = header;
            return this;
        }

        public Builder setHighlight(SourceFilter highlight) {
            this.highlight = highlight;
            return this;
        }

        public Builder setInclude(SourceFilter include) {
            this.include = include;
            return this;
        }

        public Builder setItems(FileItems items) {
            this.items = items;
            return this;
        }

        public SingleHTMLReport report() {
            return new SingleHTMLReport(source, files, coverage, items, title, header, highlight, include);
        }
    }
}
