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
import openjdk.codetools.jcov.report.FileItems;
import openjdk.codetools.jcov.report.FileSet;
import openjdk.codetools.jcov.report.LineRange;
import openjdk.codetools.jcov.report.filter.SourceFilter;
import openjdk.codetools.jcov.report.source.SourceHierarchy;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

/**
 * This support class allows to create reports which only <b>include</b> code which is selected by a filter.
 * The implementation uses visitor pattern twice: once for creating a table of content and then
 * for the body of the report.
 * @see #report(FilteredReport.FileOut, String)
 * @see FileOut
 */
public class FilteredReport {
    protected final FileSet files;
    protected final FileItems items;
    protected final CoverageHierarchy coverage;
    protected final SourceHierarchy source;
    protected final SourceFilter include;
    private final FileItems.ItemsCache cache;

    public FilteredReport(SourceHierarchy source, FileSet files, FileItems items,
                          CoverageHierarchy coverage, SourceFilter include) {
        this.files = files;
        this.items = items;
        this.coverage = coverage;
        this.source = source;
        this.include = include;
        cache = new FileItems.ItemsCache(items, files);
    }

    protected FileItems items() {
        return items;
    }

    protected FileItems.ItemsCache itemsCache() {
        return cache;
    }

    public FileSet files() {
        return files;
    }

    public void report(FileOut out) throws Exception {
        report(out, "");
    }

    //TODO this code should be aware on what root the file is
    protected void report(FileOut out, String s) throws Exception {
        Coverage cov = coverage.get(s);
        if (cov != null) {
            List<String> subFolders = files.folders(s).stream().sorted().collect(Collectors.toList());
            List<String> files = this.files.files(s).stream().sorted().collect(Collectors.toList());
            out.startFolder(s);
            for (var f : subFolders) {
                report(out, f);
            }
            for (var file : files) {
                var fileCov = coverage.getLineRanges(file);
                if (fileCov != null) {
                    out.startFile(file);
                    if (this.items != null) {
                        List<FileItems.FileItem> itemss = this.items.items(file);
                        if(itemss != null) {
                            itemss = itemss.stream()
                                    .sorted((o, a) -> o.item().compareTo(a.item())).collect(Collectors.toList());
                            if (itemss != null && !itemss.isEmpty()) {
                                out.startItems();
                                for (var fi : itemss) out.printItem(fi);
                                out.endItems();
                            }
                        }
                    }
                    var source = this.source.readFile(file);
                    List<LineRange> ranges = include != null ?
                            include.ranges(file) : List.of(new LineRange(1, source.size() + 1));
                    for (var range : ranges) {
                        out.startLineRange(range);
                        for (int line = range.first() - 1; line < range.last() && line < source.size(); line++) {
                            //TODO a separate visitor with no highlight?
                            out.printSourceLine(line, source.get(line),
                                    fileCov.containsKey(line + 1) ? fileCov.get(line + 1).coverage() : null,
                                    findItem(file, line + 1));
                        }
                        out.endLineRange(range);
                    }
                    out.endFile(s);
                }
            }
            out.endFolder(s);
        }
    }

    protected List<FileItems.FileItem> findItem(String file, int line) {
        if (items == null) return null;
        var fileItems = items.items(file);
        if (fileItems == null) return null;
        return fileItems.stream().filter(i -> {
            List<LineRange> ranges = i.ranges();
            return ranges != null && ranges.stream().anyMatch(r -> r.compare(line) == 0);
        }).collect(Collectors.toList());
    }

    protected CoverageHierarchy coverage() {
        return coverage;
    }

    protected interface FileOut {

        void startFolder(String s) throws Exception;

        void startFile(String s) throws Exception;

        void startItems() throws Exception;

        void printItem(FileItems.FileItem fi) throws Exception;

        void endItems() throws Exception;

        void startLineRange(LineRange range) throws Exception;

        void printSourceLine(int line, String s, Coverage coverage,
                             List<FileItems.FileItem> items)
                throws Exception;

        void endLineRange(LineRange range) throws Exception;

        void endFile(String s) throws Exception;

        void endFolder(String s);

    }

    /**
     * This class allows to <b>highlight</b> some portion of the included source code, leaving the
     * non-highlighted code in the report for context.
     */
    public interface Highlighter {
        boolean isHighlighted(String file, int line);
    }

    public static class Builder {
        private SourceHierarchy source;
        private FileSet files;
        private FileItems items;
        private CoverageHierarchy coverage;
        private SourceFilter include;

        public Builder setItems(FileItems items) {
            this.items = items;
            return this;
        }

        public Builder setSource(SourceHierarchy source) {
            this.source = source;
            return this;
        }

        public Builder setFiles(FileSet files) {
            this.files = files;
            return this;
        }

        public Builder setCoverage(CoverageHierarchy coverage) {
            this.coverage = coverage;
            return this;
        }

        public Builder setInclude(SourceFilter include) {
            this.include = include;
            return this;
        }

        public FilteredReport report() {
            return new FilteredReport(source, files, items, coverage, include);
        }
    }

    public static class FilterHighlighter implements Highlighter {
        private final SourceFilter highlight;
        private Iterator<LineRange> lastFileRanges;
        private LineRange lastRange = null;
        private String lastFile;
        private final SourceHierarchy source;

        public FilterHighlighter(SourceHierarchy source, SourceFilter highlight) {
            this.source = source;
            this.highlight = highlight;
        }

        public boolean isHighlighted(String file, int line) {
            if (lastFile == null || !lastFile.equals(file)) {
                lastFile = file;
                lastFileRanges = highlight.ranges(file).iterator();
            }
            if (lastRange == null)
                if (lastFileRanges.hasNext()) lastRange = lastFileRanges.next();
                else return false;
            while (lastRange.compare(line) > 0 && lastFileRanges.hasNext()) lastRange = lastFileRanges.next();
            return lastRange.compare(line) == 0;
        }
    }
}
