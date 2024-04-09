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

import static org.testng.Assert.assertTrue;

public class MultiFilesReportTest {
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
        coverage = new FileCoverage() {
            @Override
            public List<CoveredLineRange> ranges(String file) {
                return file.equals(FILE_11) ? List.of(
                        new CoveredLineRange(1, 1, Coverage.COVERED),
                        new CoveredLineRange(3, 4, Coverage.UNCOVERED),
                        new CoveredLineRange(6, 8, Coverage.COVERED)
                ) : List.of();
            }
        };

        items = new FileItems() {
            @Override
            public List<FileItem> items(String file) {
                var res = new ArrayList<FileItems.FileItem>();
                res.add(new FileItems.FileItemImpl("item0", List.of(new LineRange(1, 2)), Quality.GOOD));
                if (file.equals(FILE_22)) return  res;
                res.add(new FileItems.FileItemImpl("item1", List.of(new LineRange(3, 4)), Quality.BAD));
                if (file.equals(FILE_21)) return  res;
                res.add(new FileItems.FileItemImpl("item2", List.of(new LineRange(5, 6)), Quality.SO_SO));
                if (file.equals(FILE_12)) return  res;
                res.add(new FileItems.FileItemImpl("item3", List.of(new LineRange(7, 8)), Quality.IGNORE));
                if (file.equals(FILE_11)) return  res;
                return null;
            }

            @Override
            public String kind() {
                return "Item";
            }

            @Override
            public Map<Quality, String> legend() {
                return Map.of(Quality.GOOD, "green", Quality.BAD, "red",
                        Quality.SO_SO, "yellow", Quality.IGNORE, "grey");
            }

        };
        reportFile = Files.createTempDirectory("report");
    }
    @Test
    void test() throws Exception {
        var fileSet = new FileSet(Set.of(FILE_11, FILE_12, FILE_21, FILE_22));
        SourceFilter filter = file -> List.of(
                new LineRange(1, 3),
                new LineRange(4, 4),
                new LineRange(6, 6),
                new LineRange(8, 8)
        );
        var report = new MultiHTMLReport.Builder().setSource(source).setFiles(fileSet)
                .setCoverage(coverage).setItems(items)
                .setTitle("TITLE").setFolderHeader(s -> s.isEmpty() ? "HEADER" : "FOLDER " + s)
                .setInclude(filter)
                .setFileHeader(f -> "FILE " + f).report();
        report.report(reportFile);
        System.out.println("Report: " + reportFile.toString());
        Path root = reportFile.resolve("index.html");
        List<String> content = Files.readAllLines(root);
        assertTrue(content.contains("<title>TITLE</title>"));
        assertTrue(content.contains("HEADER"));
        assertTrue(content.stream().anyMatch(s -> s.contains("Line coverage:") && s.contains("2/3")));
        content = Files.readAllLines(reportFile.resolve("dir1.html"));
        assertTrue(content.contains("FOLDER dir1"));
        assertTrue(content.contains("<p><a href=\"index.html\">root</a><a> / dir1</a></p>"));
        content = Files.readAllLines(reportFile.resolve(FILE_11.replace('/', '_') + ".html"));
        assertTrue(content.contains("FILE dir1/file1.java"));
        assertTrue(content.stream().anyMatch(s -> s.contains("class=\"uncovered\"") && s.contains("source line #4")));
        assertTrue(content.stream().anyMatch(s -> s.contains("class=\"covered\"") && s.contains("source line #6")));
        assertTrue(content.contains("<p><a href=\"index.html\">root</a> / <a href=\"dir1.html\">dir1</a><a> / file1.java</a></p>"));
    }
}
