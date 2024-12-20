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
import openjdk.codetools.jcov.report.CoveredLineRange;
import openjdk.codetools.jcov.report.FileCoverage;
import openjdk.codetools.jcov.report.FileSet;
import openjdk.codetools.jcov.report.LineRange;
import openjdk.codetools.jcov.report.filter.SourceFilter;
import openjdk.codetools.jcov.report.source.SourceHierarchy;
import openjdk.codetools.jcov.report.source.SourcePath;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.assertTrue;

public class TextReportTest {
    static SourceHierarchy source;
    static FileCoverage coverage;
    static Path reportFile;
    @BeforeClass
    static void init() throws IOException {
        Path root = SingleFileReportTest.createFiles();
        source = new SourcePath(root, root);
        coverage = file -> file.equals(SingleFileReportTest.FILE_11) ? List.of(
                new CoveredLineRange(1, 1, Coverage.COVERED),
                new CoveredLineRange(3, 4, Coverage.UNCOVERED),
                new CoveredLineRange(6, 8, Coverage.COVERED)
        ) : List.of();
        reportFile = Files.createTempFile("report", "txt");
    }
    @Test
    void everyOdd() throws Exception {
        SourceFilter filter = file -> List.of(
                new LineRange(2, 2),
                new LineRange(4, 4),
                new LineRange(6, 6),
                new LineRange(8, 8)
        );
        var files = new FileSet(Set.of(SingleFileReportTest.FILE_11, SingleFileReportTest.FILE_12,
                SingleFileReportTest.FILE_21, SingleFileReportTest.FILE_22));
        var report = new TextReport.Builder().source(source).files(files).coverage(coverage)
                .header("HEADER").filter(filter).report();
        report.report(reportFile);
        var content = Files.readAllLines(reportFile);
        assertTrue(content.contains("HEADER"));
        assertTrue(content.stream().anyMatch("total 50.00%(1/2)"::equals));
        assertTrue(content.stream().anyMatch("4:-source line #4"::equals));
        assertTrue(content.stream().anyMatch("6:+source line #6"::equals));
    }
}
