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
package openjdk.codetools.jcov.report.filter;

import openjdk.codetools.jcov.report.Coverage;
import openjdk.codetools.jcov.report.CoveredLineRange;
import openjdk.codetools.jcov.report.FileSet;
import openjdk.codetools.jcov.report.jcov.JCovLoadTest;
import openjdk.codetools.jcov.report.source.SourcePath;
import openjdk.codetools.jcov.report.view.TextReport;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class GitDifFilterTest {
    public static Path cp(String resource) throws IOException {
        Path res = Files.createTempFile(resource.substring(resource.lastIndexOf('/') + 1),
                resource.substring(resource.lastIndexOf('.') + 1));
        Files.createDirectories(res.getParent());
        try (var in = new BufferedReader(new InputStreamReader(JCovLoadTest.class.getResourceAsStream(resource)));
             var out = Files.newBufferedWriter(res)) {
            String line;
            while ((line = in.readLine()) != null) {
                out.write(line); out.newLine(); out.flush();
            }
        }
        return res;
    }
    public static Path cp(String dir, Map<String, String> resources) throws IOException {
        var res = Files.createTempDirectory("");
        var root = res.resolve(dir);
        Files.createDirectories(root);
        for (var r : resources.keySet()) {
            Path file = root.resolve(resources.get(r));
            Files.createDirectories(file.getParent());
            try (var in = new BufferedReader(new InputStreamReader(JCovLoadTest.class.getResourceAsStream(r)));
                 var out = Files.newBufferedWriter(file)) {
                String line;
                while ((line = in.readLine()) != null) {
                    out.write(line); out.newLine(); out.flush();
                }
            }
        }
        return res;
    }
    static SourceFilter filter;
    @BeforeClass
    static void init() throws IOException {
        String pkg = "/" + GitDifFilterTest.class.getPackageName().replace('.', '/') + "/";
        filter = GitDiffFilter.parseDiff(cp(pkg + "negative_array_size.diff")/*, Set.of("src")*/);
    }
    @Test
    void basic() {
        assertTrue(filter.includes("src/JavaObjectInputStreamAccess.java", 37));
        var oisRanges = filter.ranges("src/ObjectInputStream.java");
        assertEquals(oisRanges.get(oisRanges.size() - 1).first(), 2141);
        assertEquals(oisRanges.get(oisRanges.size() - 1).last(), 2143);
    }
    @Test
    void report() throws Exception {
        String pkg = "/" + GitDifFilterTest.class.getPackageName().replace('.', '/') + "/";
        Path src = cp("src", Map.of(
                pkg + "JavaObjectInputStreamAccess.java.txt", "JavaObjectInputStreamAccess.java",
                pkg + "ObjectInputStream.java.txt", "ObjectInputStream.java"));
        var files = new FileSet(Set.of("src/JavaObjectInputStreamAccess.java", "src/ObjectInputStream.java"));
        Path report = Files.createTempFile("report", ".txt");
        new TextReport.Builder().setSource(new SourcePath(src, src.resolve("src"))).setFiles(files).setCoverage(file -> {
            var res = new ArrayList<CoveredLineRange>();
            var line = 0;
            var chunkSize = 1;
            while (line < 10000) {
                res.add(new CoveredLineRange(line, line + chunkSize - 1, Coverage.UNCOVERED));
                line += chunkSize;
                res.add(new CoveredLineRange(line, line + chunkSize - 1, Coverage.COVERED));
                line += chunkSize;
                line += chunkSize;
            }
            return res;
        }).setHeader("ObjectInputStream coverage").setFilter(filter).report()
                .report(report);
        List<String> reportLines = Files.readAllLines(report);
        assertTrue(reportLines.contains("src/ObjectInputStream.java 50.00%(1/2)"));
        assertTrue(reportLines.contains("2142:-            throw new StreamCorruptedException(\"Array length is negative\");"));
    }
}
