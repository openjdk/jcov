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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.lang.String.format;

public class MultiHTMLReport {
    private final String title;
    private final Function<String, String> folderHeader;
    private final Function<String, String> fileHeader;
    private final FilteredReport theReport;
    private final SourceFilter highlight;
    private final SourceHierarchy source;

    protected MultiHTMLReport(SourceHierarchy source, FileSet files, FileCoverage coverage,
                              FileItems items, String title,
                              Function<String, String> folderHeader, Function<String, String> fileHeader,
                              SourceFilter highlight, SourceFilter include) {
        theReport = new FilteredReport.Builder()
                .setSource(source)
                .setFiles(files)
                .setItems(items)
                .setCoverage(new CoverageHierarchy(files.files(), source, coverage, highlight))
                .setInclude(include)
                .report();
        this.source = source;
        this.title = title;
        this.folderHeader = folderHeader;
        this.fileHeader = fileHeader;
        this.highlight = highlight;
    }

    public void report(Path dest) throws Exception {
        if (Files.isDirectory(dest)) {
            if (Files.list(dest).findAny().isPresent()) {
                throw new IllegalStateException("Not empty: " + dest);
            }
        } else if (Files.isRegularFile(dest)) {
            throw new IllegalStateException("Is a file: " + dest);
        } else {
            Files.createDirectories(dest);
        }
        toReport("coverage.css", dest);
        toReport("sorttable.js", dest);
        try (HtmlOut out = new HtmlOut(dest)) {
            theReport.report(out);
        }
    }

    static void toReport(String resourceName, Path dest) throws IOException {
        toReport(resourceName, resourceName, dest);
    }
    static void toReport(String resourceName, String fileName, Path dest) throws IOException {
        var className = MultiHTMLReport.class.getName();
        className = className.substring(0, className.lastIndexOf('.')).replace('.', '/');
        className = "/" + className;
        className += "/" + resourceName;
        copyToReport(MultiHTMLReport.class, className, fileName, dest);
    }
    public static void copyToReport(Class loaderClass, String resourceName, String fileName, Path dest) throws IOException {
        try (var in = loaderClass.getResourceAsStream(resourceName)) {
            Files.write(dest.resolve(fileName), in.readAllBytes());
        } catch (Exception e) {
            System.err.printf("Failed to extract %s to %s/%s\n", resourceName, dest.toString(), fileName);
            throw e;
        }
    }

    final static Map<FileItems.Quality, String> HTML_COLOR_CLASSES = Map.of(
            FileItems.Quality.BAD, "item_not_so_good",
            FileItems.Quality.SO_SO, "item_so_so",
            FileItems.Quality.GOOD, "item_good",
            FileItems.Quality.VERY_GOOD, "item_very_good",
            FileItems.Quality.IGNORE, "item_ignore",
            FileItems.Quality.NONE, "item_none",
            FileItems.Quality.LEFT, "item_left",
            FileItems.Quality.RIGHT, "item_right",
            FileItems.Quality.BOTH, "item_both"
    );

    private class HtmlOut implements FilteredReport.FileOut, AutoCloseable {
        private final Path dest;
        private BufferedWriter folderOut = null;
        private BufferedWriter fileOut = null;
        private String file;
        private final FilteredReport.FilterHighlighter highlighter;

        private HtmlOut(Path dest) throws IOException {
            this.highlighter = new FilteredReport.FilterHighlighter(source, highlight);
            this.dest = dest;
        }

        private void init(String title, String header, BufferedWriter out/*, boolean folder*/) throws IOException {
            out.write("<html><head>"); out.newLine();
            out.write("<title>" + title + "</title>"); out.newLine();
            out.write("<link rel=\"stylesheet\" type=\"text/css\" href=\"coverage.css\" title=\"Style\">");
            out.newLine();
            out.write("<script type=\"text/javascript\" src=\"sorttable.js\"></script>");
            out.newLine();
            out.write("</head><body>\n"); out.newLine();
            out.write(header + "\n"); out.newLine();
        }

        private String navbarRef(String s) {
            StringBuilder line = new StringBuilder("<a href=\"");
            line.append(s.isEmpty() ? "index.html" : reportFile(s));
            line.append("\">");
            line.append(s.isEmpty() ? "root" : s.substring(s.lastIndexOf('/') + 1));
            line.append("</a>");
            return line.toString();
        }

        private String navbar(String s) {
            if (s.isEmpty()) return "";
            StringBuilder line = new StringBuilder("<p>");
            line.append(navbarRef(""));
            int slashInd = 0;
            while ((slashInd = s.indexOf('/', slashInd + 1)) > -1) {
                line.append(" / ");
                line.append(navbarRef(s.substring(0, slashInd)));
            }
            line.append("<a> / ");
            line.append(s.substring(s.lastIndexOf('/') + 1));
            line.append("</a></p>");
            return line.toString();
        }

        private String reportFile(String folderOrFile) {
            return folderOrFile.replace('/', '_') + ".html";
        }
        @Override
        public void startFolder(String s) throws IOException {
            String folderFile;
            if (!s.isEmpty()) folderFile = reportFile(s);
            else folderFile = "index.html";
            try(BufferedWriter out = Files.newBufferedWriter(dest.resolve(folderFile))) {
                init(title, folderHeader.apply(s), out);
                out.write(navbar(s)); out.newLine();
                var colors = theReport.itemsCache().count(s);
                if (!colors.isEmpty()) {
                    out.write("<table><tr><th>" + theReport.items().kind() + "</th><th>Count</th></tr>");
                    out.newLine();
                    for (var c : FileItems.Quality.values()) {
                        if (theReport.items().legend().containsKey(c)) {
                            out.write("<tr><td><a class=\"" + HTML_COLOR_CLASSES.get(c) + "\">" +
                                    theReport.items().legend().get(c) + "</a></td>");
                            out.write("<td>" + colors.get(c) + "</td></tr>");
                            out.newLine();
                        }
                    }
                    out.write("</table>");
                    out.newLine();
                }
                out.write("Line coverage: " + theReport.coverage().get(s).toString());
                out.newLine();
                Collection<String> folders = theReport.files().folders(s);
                if (!folders.isEmpty()) {
                    out.write("<table id=\"folders\" class=\"sortable\"><tr><th>Folder</th>");
                    out.newLine();
                    if (!colors.isEmpty()) {
                        for (var c : FileItems.Quality.values()) {
                            if (theReport.items().legend().containsKey(c)) {
                                out.write("<th>" + theReport.items().legend().get(c) + "</th>");
                            }
                        }
                    }
                    out.write("<th>Line coverage</th></tr>");
                    out.newLine();
                    for (String subFolder : folders) {
                        out.write("<tr><td><a href=\"" + reportFile(subFolder) + "\"</a>" + subFolder + "</a></td>");
                        colors = theReport.itemsCache().count(subFolder);
                        if (!colors.isEmpty()) {
                            for (var c : FileItems.Quality.values()) {
                                if (theReport.items().legend().containsKey(c)) {
                                    out.write("<td><a class=\"" + HTML_COLOR_CLASSES.get(c) + "\">" +
                                            colors.get(c) + "</a></td>");
                                }
                            }
                        }
                        out.write("<td>" + theReport.coverage().get(subFolder) + "</td></tr>");
                        out.newLine();
                    }
                    out.write("</table>");
                    out.newLine();
                }
                Collection<String> files = theReport.files().files(s);
                if (!files.isEmpty()) {
                    out.write("<table id=\"files\" class=\"sortable\"><tr><th>File</th>");
                    if (!colors.isEmpty()) {
                        for (var c : FileItems.Quality.values()) {
                            if (theReport.items().legend().containsKey(c)) {
                                out.write("<th>" + theReport.items().legend().get(c) + "</th>");
                            }
                        }
                    }
                    out.write("<th>Line coverage</th></tr>");
                    out.newLine();
                    for (String file : files) {
                        out.write("<tr><td><a href=\"" + reportFile(file) + "\"</a>" + file + "</a></td>");
                        colors = theReport.itemsCache().count(file);
                        if (!colors.isEmpty()) {
                            for (var c : FileItems.Quality.values()) {
                                if (theReport.items().legend().containsKey(c)) {
                                    out.write("<td><a class=\"" + HTML_COLOR_CLASSES.get(c) + "\">" +
                                            colors.get(c) + "</a></td>");
                                }
                            }
                        }
                        out.write("<td>" + theReport.coverage().get(file) + "</td></tr>");
                        out.newLine();
                    }
                    out.write("</table>");
                    out.newLine();
                }
            }
        }

        @Override
        public void startFile(String file) throws IOException {
            this.file = file;
            fileOut = Files.newBufferedWriter(dest.resolve(reportFile(file)));
            init(title, fileHeader.apply(file), fileOut);
            fileOut.write(navbar(file)); fileOut.newLine();
        }

        @Override
        public void startItems() throws Exception {
            fileOut.write("<table>"); fileOut.newLine();
            fileOut.write("<tr><th>"+theReport.items().kind()+"</th></tr>");
        }

        @Override
        public void printItem(FileItems.FileItem fi) throws IOException {
            String href;
            if (fi.ranges().isEmpty()) href = "";
            else href = "href=\"#line_" + fi.ranges().get(0).first() + "\"";
            fileOut.write(format("<tr><td><pre><a id=\"item_%s\" %s class=\"%s\">%s</a></pre></td>",
                    fi.item(), href,
                    HTML_COLOR_CLASSES.get(fi.quality()),
                    fi.item().replace("<", "&lt;").replace(">", "&gt;")));
            fileOut.write("</tr>");
            fileOut.newLine();
        }

        @Override
        public void endItems() throws Exception {
            fileOut.write("</table>"); fileOut.newLine();
            fileOut.write("Line coverage " + theReport.coverage().get(file)); fileOut.newLine();
        }

        @Override
        public void startLineRange(LineRange range) throws IOException {
            if (highlight != null)
                fileOut.write("<hr/>"); fileOut.newLine();
            fileOut.write("<table>"); fileOut.newLine();
        }

        private String coveredClass(Coverage coverage) {
            if (coverage != null)
                switch (coverage.quality()) {
                    case GOOD:
                        return "covered";
                    case BAD:
                        return "uncovered";
                    case SO_SO:
                        return "so_so";
                    case LEFT:
                        return "left";
                    case RIGHT:
                        return "right";
                    case BOTH:
                        return "both";
                    default:
                        return "context";
                }
            else return "context";
        }

        @Override
        public void printSourceLine(int lineNo, String line, Coverage coverage,
                                    List<FileItems.FileItem> items) throws IOException {
            fileOut.write("<tr>");
            if (items != null) {
                fileOut.write("<td><pre>");
                for (var item : items) {
                    //TODO should there be a separate method on what to print?
                    fileOut.write(format("<a id=\"line_%s\" href=\"#item_%s\" class=\"%s\" title=\"%s\">%s</a>&nbsp;",
                            (lineNo + 1), item.item(), HTML_COLOR_CLASSES.get(item.quality()), item.item(), " "));
                }
                fileOut.write("</pre></td>");
            }
            fileOut.write("<td><pre>" + (lineNo + 1) + "</pre></td>");
            fileOut.write("<td><pre><a class=\"" +
                    (coverage != null ? coveredClass(coverage) :
                        (highlight != null && highlighter.isHighlighted(file, lineNo + 1) ?
                                "highlight" : "context")) +
                    "\">");
            fileOut.write(line.replaceAll("</?\\s*pre\\s*>", ""));
            fileOut.write("</a></pre></td></tr>");
            fileOut.newLine();
        }

        @Override
        public void endLineRange(LineRange range) throws IOException {
            fileOut.write("</table>"); fileOut.newLine();
        }

        @Override
        public void endFile(String s) throws IOException {
            if (highlight != null)
                fileOut.write("<hr/>"); fileOut.newLine();
            fileOut.close();
        }

        public void endFolder(String s) {
        }

        @Override
        public void close() throws Exception {
        }
    }

    public static class Builder {
        private FileSet files;
        private SourceHierarchy source;
        private SourceFilter highlight;
        private SourceFilter include;
        private String title = "";
        private Function<String, String> folderHeader = s -> "";
        private Function<String, String> fileHeader = s -> "";
        private FileCoverage coverage;
        private FileItems items;

        public Builder setFiles(FileSet files) {
            this.files = files;
            return this;
        }

        public Builder setSource(SourceHierarchy source) {
            this.source = source;
            return this;
        }

        public Builder setInclude(SourceFilter include) {
            this.include = include;
            return this;
        }

        public Builder setHighlight(SourceFilter highlight) {
            this.highlight = highlight;
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setCoverage(FileCoverage coverage) {
            this.coverage = coverage;
            return this;
        }

        public MultiHTMLReport report() {
            return new MultiHTMLReport(source, files, coverage, items,
                    title,
                    folderHeader, fileHeader, highlight,include);
        }

        public Builder setFolderHeader(Function<String, String> prefix) {
            this.folderHeader = prefix;
            return this;
        }

        public Builder setFileHeader(Function<String, String> prefix) {
            this.fileHeader = prefix;
            return this;
        }

        public Builder setItems(FileItems items) {
            this.items = items;
            return this;
        }
    }
}
