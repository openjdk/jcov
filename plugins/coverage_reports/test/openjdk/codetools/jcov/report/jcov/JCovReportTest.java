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
package openjdk.codetools.jcov.report.jcov;

import openjdk.codetools.jcov.report.FileSet;
import openjdk.codetools.jcov.report.LineRange;
import openjdk.codetools.jcov.report.filter.GitDifFilterTest;
import openjdk.codetools.jcov.report.filter.GitDiffFilter;
import openjdk.codetools.jcov.report.filter.SourceFilter;
import openjdk.codetools.jcov.report.source.ContextFilter;
import openjdk.codetools.jcov.report.source.SourcePath;
import openjdk.codetools.jcov.report.view.CoverageHierarchy;
import openjdk.codetools.jcov.report.view.SingleHTMLReport;
import openjdk.codetools.jcov.report.view.TextReport;
import com.sun.tdk.jcov.data.FileFormatException;
import com.sun.tdk.jcov.instrument.DataRoot;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertTrue;

public class JCovReportTest {

    private FileSet files;
    private JCovLineCoverage rawCoverage;
    private Path src;

    @BeforeClass
    void init() throws IOException, FileFormatException {
        String pkg = "/" + GitDifFilterTest.class.getPackageName().replace('.', '/') + "/";
        src = GitDifFilterTest.cp("src, ", Map.of(
                pkg + "JavaObjectInputStreamAccess.java.txt", "java/io/JavaObjectInputStreamAccess.java",
                pkg + "ObjectInputStream.java.txt", "java/io/ObjectInputStream.java"));
        files = new FileSet(Set.of("java/io/JavaObjectInputStreamAccess.java", "java/io/ObjectInputStream.java"));
        var xmlName = JCovLoadTest.class.getName().replace('.', '/');
        xmlName = "/" + xmlName.substring(0, xmlName.lastIndexOf('/')) + "/ObjectInputStream.xml";
        rawCoverage = new JCovLineCoverage(DataRoot.read(GitDifFilterTest.cp(xmlName).toString()));
    }
    @Test
    void report() throws Exception {
//        String pkg = "/" + GitDifFilterTest.class.getPackageName().replace('.', '/') + "/";
//        Path src = GitDifFilterTest.cp("src, ", Map.of(
//                pkg + "JavaObjectInputStreamAccess.java.txt", "java/io/JavaObjectInputStreamAccess.java",
//                pkg + "ObjectInputStream.java.txt", "java/io/ObjectInputStream.java"));
//        var files = new FileSet(Set.of("java/io/JavaObjectInputStreamAccess.java", "java/io/ObjectInputStream.java"));
//        var xmlName = JCovLoadTest.class.getName().replace('.', '/');
//        xmlName = "/" + xmlName.substring(0, xmlName.lastIndexOf('/')) + "/ObjectInputStream.xml";
//        String diffPkg = "/" + JCovReportTest.class.getPackageName().replace('.', '/') + "/";
        String diffPkg = "/" + JCovReportTest.class.getPackageName().replace('.', '/') + "/";
        var filter = GitDiffFilter.parseDiff(GitDifFilterTest.cp(diffPkg + "negative_array_size.diff"), Set.of("src"));
//        var rawCoverage = new JCovLineCoverage(DataRoot.read(GitDifFilterTest.cp(xmlName).toString()));
        Path textReport = Files.createTempFile("report", ".txt");
        new TextReport(new SourcePath(List.of(src)),
                files,
                rawCoverage,
                "negative array size fix",
                new ContextFilter(filter, 10)).report(textReport);
        List<String> reportLines = Files.readAllLines(textReport);
        assertTrue(reportLines.contains("1454:      * @throws StreamCorruptedException if arrayLength is negative"));
        assertTrue(reportLines.contains("1457:     private void checkArray(Class<?> arrayType, int arrayLength) throws ObjectStreamException {"));
        assertTrue(reportLines.contains("1463:+            throw new StreamCorruptedException(\"Array length is negative\");"));
        assertTrue(reportLines.contains("2141:+        if (len < 0) {"));
        assertTrue(reportLines.contains("2142:+            throw new StreamCorruptedException(\"Array length is negative\");"));
        assertTrue(reportLines.contains("2143:         }"));
        assertTrue(reportLines.contains("2142:+            throw new StreamCorruptedException(\"Array length is negative\");"));
        Path htmlReport = Files.createTempFile("report", ".html");
        new SingleHTMLReport(new SourcePath(List.of(src)),
                files,
                rawCoverage,
                "negative array size fix",
                "negative array size fix",
                filter,
                new ContextFilter(filter, 10)).report(htmlReport);
        System.out.println("Report: " + htmlReport);
        reportLines = Files.readAllLines(htmlReport);
        assertTrue(reportLines.contains("<a class=\"highlight\">1454:      * @throws StreamCorruptedException if arrayLength is negative</a>"));
        assertTrue(reportLines.contains("<a class=\"highlight\">1457:     private void checkArray(Class<?> arrayType, int arrayLength) throws ObjectStreamException {</a>"));
        assertTrue(reportLines.contains("<a class=\"covered\">1463:             throw new StreamCorruptedException(\"Array length is negative\");</a>"));
        assertTrue(reportLines.contains("<a class=\"covered\">2141:         if (len < 0) {</a>"));
        assertTrue(reportLines.contains("<a class=\"covered\">2142:             throw new StreamCorruptedException(\"Array length is negative\");</a>"));
        assertTrue(reportLines.contains("<a class=\"highlight\">2143:         }</a>"));
        assertTrue(reportLines.contains("<a class=\"covered\">2142:             throw new StreamCorruptedException(\"Array length is negative\");</a>"));

    }
    @Test
    void innerClass() throws Exception {
        var filter = new SourceFilter() {
            @Override
            public List<LineRange> ranges(String file) {
                return List.of(new LineRange(2987,3838));
            }
        };
        Path textReport = Files.createTempFile("report", ".txt");
        new TextReport(new SourcePath(List.of(src)),
                files,
                rawCoverage,
                "negative array size fix",
                filter).report(textReport);
        List<String> reportLines = Files.readAllLines(textReport);
        assertTrue(reportLines.contains("3035:+            this.in = new PeekInputStream(in);"));
    }
    //2987,3838
}
