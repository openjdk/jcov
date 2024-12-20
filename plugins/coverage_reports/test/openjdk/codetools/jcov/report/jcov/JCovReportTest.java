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
package openjdk.codetools.jcov.report.jcov;

import openjdk.codetools.jcov.report.FileSet;
import openjdk.codetools.jcov.report.LineRange;
import openjdk.codetools.jcov.report.filter.GitDifFilterTest;
import openjdk.codetools.jcov.report.filter.GitDiffFilter;
import openjdk.codetools.jcov.report.filter.SourceFilter;
import openjdk.codetools.jcov.report.source.ContextFilter;
import openjdk.codetools.jcov.report.source.SourceHierarchy;
import openjdk.codetools.jcov.report.source.SourcePath;
import openjdk.codetools.jcov.report.view.MultiHTMLReport;
import openjdk.codetools.jcov.report.view.SingleHTMLReport;
import com.sun.tdk.jcov.data.FileFormatException;
import com.sun.tdk.jcov.instrument.DataRoot;
import openjdk.codetools.jcov.report.view.TextReport;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.regex.Pattern.matches;
import static org.testng.Assert.assertTrue;

public class JCovReportTest {

    private FileSet files;
    private JCovLineCoverage rawCoverage;
    private Path src;
    private SourceHierarchy source;

    @BeforeClass
    void init() throws IOException, FileFormatException {
        String pkg = "/" + GitDifFilterTest.class.getPackageName().replace('.', '/') + "/";
        src = GitDifFilterTest.cp("src", Map.of(
                pkg + "JavaObjectInputStreamAccess.java.txt", "java/io/JavaObjectInputStreamAccess.java",
                pkg + "ObjectInputStream.java.txt", "java/io/ObjectInputStream.java"));
        files = new FileSet(Set.of("src/java/io/JavaObjectInputStreamAccess.java", "src/java/io/ObjectInputStream.java"));
        var xmlName = JCovLoadTest.class.getName().replace('.', '/');
        xmlName = "/" + xmlName.substring(0, xmlName.lastIndexOf('/')) + "/ObjectInputStream.xml";
        source = new SourcePath(src, src.resolve("src"));
        rawCoverage = new JCovLineCoverage(DataRoot.read(GitDifFilterTest.cp(xmlName).toString()), source);
    }
    @Test
    void txtReport() throws Exception {
        String diffPkg = "/" + JCovReportTest.class.getPackageName().replace('.', '/') + "/";
        var filter = GitDiffFilter.parseDiff(GitDifFilterTest.cp(diffPkg + "negative_array_size.diff")/*, Set.of("src")*/);
        Path textReport = Files.createTempFile("report", ".txt");
        new TextReport.Builder()
                .source(new SourcePath(src, src.resolve("src")))
                .files(files)
                .coverage(rawCoverage).header("negative array size fix")
                .filter(new ContextFilter(filter, 10)).report()
                .report(textReport);
        List<String> reportLines = Files.readAllLines(textReport);
        assertTrue(reportLines.contains("1454:      * @throws StreamCorruptedException if arrayLength is negative"));
        assertTrue(reportLines.contains("1457:     private void checkArray(Class<?> arrayType, int arrayLength) throws ObjectStreamException {"));
        assertTrue(reportLines.contains("1463:+            throw new StreamCorruptedException(\"Array length is negative\");"));
        assertTrue(reportLines.contains("2141:+        if (len < 0) {"));
        assertTrue(reportLines.contains("2142:+            throw new StreamCorruptedException(\"Array length is negative\");"));
        assertTrue(reportLines.contains("2143:         }"));
        assertTrue(reportLines.contains("2142:+            throw new StreamCorruptedException(\"Array length is negative\");"));
    }
    @Test
    void singleReport() throws Exception {
        String diffPkg = "/" + JCovReportTest.class.getPackageName().replace('.', '/') + "/";
        var filter = GitDiffFilter.parseDiff(GitDifFilterTest.cp(diffPkg + "negative_array_size.diff")/*, Set.of("src")*/);
        Path htmlReport = Files.createTempFile("report", ".html");
        new SingleHTMLReport.Builder().source(new SourcePath(src, src.resolve("src"))).files(files)
                .coverage(rawCoverage).title("negative array size fix").header("negative array size fix")
                .highlight(filter).include(new ContextFilter(filter, 10)).report().report(htmlReport);
        System.out.println("Report: " + htmlReport);
        var reportLines = Files.readAllLines(htmlReport);
        assertTrue(reportLines.contains("<a class=\"highlight\">1454:      * @throws StreamCorruptedException if arrayLength is negative</a>"));
        assertTrue(reportLines.contains("<a class=\"highlight\">1457:     private void checkArray(Class<?> arrayType, int arrayLength) throws ObjectStreamException {</a>"));
        assertTrue(reportLines.contains("<a class=\"covered\">1463:             throw new StreamCorruptedException(\"Array length is negative\");</a>"));
        assertTrue(reportLines.contains("<a class=\"covered\">2141:         if (len < 0) {</a>"));
        assertTrue(reportLines.contains("<a class=\"covered\">2142:             throw new StreamCorruptedException(\"Array length is negative\");</a>"));
        assertTrue(reportLines.contains("<a class=\"highlight\">2143:         }</a>"));
    }
    @Test
    void multiReport() throws Exception {
        String diffPkg = "/" + JCovReportTest.class.getPackageName().replace('.', '/') + "/";
        var filter = GitDiffFilter.parseDiff(GitDifFilterTest.cp(diffPkg + "negative_array_size.diff")/*, Set.of("src")*/);
        Path multiHtmlReport = Files.createTempDirectory("report");
        new MultiHTMLReport.Builder().setSource(new SourcePath(src, src.resolve("src"))).setFiles(files)
                .setCoverage(rawCoverage).setTitle("negative array size fix")
                .setFolderHeader(x -> "negative array size fix")
                .setFileHeader(x -> "negative array size fix")
                .setHighlight(filter).setInclude(new ContextFilter(filter, 10)).report().report(multiHtmlReport);
        System.out.println("Report: " + multiHtmlReport);
        var reportLines = Files.readAllLines(multiHtmlReport.resolve("index.html"));
        reportLines = Files.readAllLines(multiHtmlReport.resolve("src_java_io_ObjectInputStream.java.html"));
        assertTrue(reportLines.stream().anyMatch(s -> matches(".*1454.*class=\"highlight\".*@throws StreamCorruptedException if arrayLength is negative.*", s)));
        assertTrue(reportLines.stream().anyMatch(s -> matches(".*1457.*class=\"highlight\".*private void checkArray.*", s)));
        assertTrue(reportLines.stream().anyMatch(s -> matches(".*1463.*class=\"covered\".*throw new StreamCorruptedException.*", s)));
        assertTrue(reportLines.stream().anyMatch(s -> matches(".*2141.*class=\"covered\".*", s)));
        assertTrue(reportLines.stream().anyMatch(s -> matches(".*2142.*class=\"covered\".*throw new StreamCorruptedException.*", s)));
        assertTrue(reportLines.stream().anyMatch(s -> matches(".*2143.*lass=\"highlight\".*", s)));
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
        new TextReport.Builder().source(source).files(files).coverage(rawCoverage).header("negative array size fix").filter(filter).report().report(textReport);
        List<String> reportLines = Files.readAllLines(textReport);
        assertTrue(reportLines.contains("3035:+            this.in = new PeekInputStream(in);"));
    }
}
