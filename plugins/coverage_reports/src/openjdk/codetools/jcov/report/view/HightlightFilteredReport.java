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
import openjdk.codetools.jcov.report.FileSet;
import openjdk.codetools.jcov.report.LineRange;
import openjdk.codetools.jcov.report.filter.SourceFilter;
import openjdk.codetools.jcov.report.source.SourceHierarchy;

import java.nio.file.Files;
import java.util.stream.Collectors;

abstract class HightlightFilteredReport {
    private final FileSet files;
    private final CoverageHierarchy coverage;
    private final SourceHierarchy source;
    private final SourceFilter highlight;
    private final SourceFilter include;

    protected HightlightFilteredReport(SourceHierarchy source, FileSet files, CoverageHierarchy coverage,
                                       SourceFilter highlight, SourceFilter include) {
        this.files = files;
        this.coverage = coverage;
        this.source = source;
        this.highlight = highlight;
        this.include = include;
    }

    protected void toc(TOCOut out, String s) throws Exception {
        out.printFolderLine(s.isEmpty() ? "" : s);
        for (var f : files.folders(s).stream().sorted().collect(Collectors.toList())) {
            toc(out, f);
        }
        for (var f : files.files(s).stream().sorted().collect(Collectors.toList())) {
            out.printFileLine(f);
        }
    }

    protected void code(FileOut out, String s) throws Exception {
        out.startDir(s);
        for (var f : files.folders(s).stream().sorted().collect(Collectors.toList())) {
            code(out, f);
        }
        for (var file : files.files(s).stream().sorted().collect(Collectors.toList())) {
            out.startFile(file);
            var source = Files.readAllLines(this.source.resolve(file));
            var fileCov = coverage.getLineRanges(file);
            var highlightRanges = highlight.ranges(file).iterator();
            var highlightRange = highlightRanges.next();
            for (var range : include.ranges(file)) {
                out.startLineRange(range);
                for (int line = range.first() - 1; line < range.last() && line < source.size(); line++) {
                    while (highlightRange != null && highlightRange.compare(line) > 0)
                        highlightRange = highlightRanges.hasNext() ? highlightRanges.next() : null;
                    boolean highlight = highlightRange != null && highlightRange.compare(line + 1) == 0;
                    out.printSourceLine(line, source.get(line), highlight,
                            fileCov.containsKey(line + 1) ? fileCov.get(line + 1).coverage() : null);
                }
                out.endLineRange(range);
            }
            out.endFile(s);
        }
    }

    protected CoverageHierarchy coverage() {
        return coverage;
    }

    protected interface TOCOut {
        void printFileLine(String f) throws Exception;
        void printFolderLine(String s) throws Exception;
    }
    protected interface FileOut {
        void startFile(String s) throws Exception;
        void startLineRange(LineRange range) throws Exception;
        void printSourceLine(int line, String s, boolean highlight, Coverage coverage) throws Exception;
        void endLineRange(LineRange range) throws Exception;
        void endFile(String s) throws Exception;
        void startDir(String s) throws Exception;
    }
}
