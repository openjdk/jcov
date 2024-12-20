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
import openjdk.codetools.jcov.report.FileItems;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class SingleFileReportTest {
    public static final String FILE_11 = "dir1/file1.java";
    public static final String FILE_12 = "dir1/file2.java";
    public static final String FILE_21 = "dir2/file1.java";
    public static final String FILE_22 = "dir2/file2.java";
    static SourceHierarchy source;
    static FileCoverage coverage;
    static Path reportFile;
    private static FileItems items;

    public static Path createFiles() throws IOException {
        Path root = Files.createTempDirectory("source");
        Files.createDirectories(root);
        for (var dir : List.of("dir1", "dir2")) Files.createDirectories(root.resolve(dir));
        for (var file : List.of(FILE_11, FILE_12, FILE_21, FILE_22))
            Files.write(root.resolve(file),
                    List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").stream().map(s -> "source line #" + s)
                            .collect(Collectors.toList()));
        return root;
    }
    @BeforeClass
    static void init() throws IOException {
        Path root = createFiles();
        source = new SourcePath(root, root);
        coverage = file -> file.equals(FILE_11) ? List.of(
                new CoveredLineRange(1, 1, Coverage.COVERED),
                new CoveredLineRange(3, 4, Coverage.UNCOVERED),
                new CoveredLineRange(6, 8, Coverage.COVERED)
        ) : List.of();
        items = new FileItems() {
            @Override
            public List<FileItem> items(String file) {
                var res = new ArrayList<FileItems.FileItem>();
                res.add(new FileItems.FileItemImpl("item0", List.of(new LineRange(0, 1)), Quality.GOOD));
                if (file.equals(SingleFileReportTest.FILE_11)) return res;
                res.add(new FileItems.FileItemImpl("item1", List.of(new LineRange(2, 3)), Quality.BAD));
                if (file.equals(SingleFileReportTest.FILE_12)) return res;
                res.add(new FileItems.FileItemImpl("item2", List.of(new LineRange(4, 5)), Quality.SO_SO));
                if (file.equals(SingleFileReportTest.FILE_21)) return res;
                res.add(new FileItems.FileItemImpl("item3", List.of(new LineRange(6, 7)), Quality.IGNORE));
                if (file.equals(SingleFileReportTest.FILE_22)) return res;
                return null;
            }

            @Override
            public String kind() {
                return "Item";
            }

            @Override
            public Map<Quality, String> legend() {
                return null;
            }
        };
        reportFile = Files.createTempFile("report", ".html");
    }
    @Test
    void test() throws Exception {
        var fileSet = new FileSet(Set.of(FILE_11, FILE_12, FILE_21, FILE_22));
        SourceFilter filter = file -> List.of(
                new LineRange(2, 2),
                new LineRange(4, 4),
                new LineRange(6, 6),
                new LineRange(8, 8)
        );
        var report = new SingleHTMLReport.Builder().setSource(source).setFiles(fileSet)
                .setCoverage(coverage).setItems(items)
                .setTitle("TITLE").setHeader("<h1>HEADER</h1>").setHighlight(filter)
                .setInclude(filter).report();
        report.report(reportFile);
        System.out.println("Report: " + reportFile.toString());
        List<String> content = Files.readAllLines(reportFile);
        assertTrue(content.contains("<title>TITLE</title>"));
        assertTrue(content.contains("<h1>HEADER</h1>"));
        assertTrue(content.stream().anyMatch("<tr><td><a href=\"#total\">total</a></td><td>50.00%(1/2)</td></tr>"::equals));
        assertTrue(content.stream().anyMatch("<a class=\"uncovered\">4: source line #4</a>"::equals));
        assertTrue(content.stream().anyMatch("<a class=\"covered\">6: source line #6</a>"::equals));
        assertTrue(content.contains("<tr><td><pre><a id=\"item_item3\" class=\"item_ignore\">item3</a></pre></td></tr>"));
    }
}
