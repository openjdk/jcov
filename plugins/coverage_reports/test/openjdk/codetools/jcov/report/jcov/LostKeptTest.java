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

import com.sun.tdk.jcov.Instr;
import com.sun.tdk.jcov.data.FileFormatException;
import com.sun.tdk.jcov.instrument.DataRoot;
import openjdk.codetools.jcov.report.CoveredLineRange;
import openjdk.codetools.jcov.report.FileCoverage;
import openjdk.codetools.jcov.report.FileItems;
import openjdk.codetools.jcov.report.FileSet;
import openjdk.codetools.jcov.report.source.SourcePath;
import openjdk.codetools.jcov.report.view.MultiHTMLReport;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.testng.Assert.*;

public class LostKeptTest {
    static final String STILL_EXISTS_JAVA_FILE = "p/a/StillExists.java";
    List<Map<String, Map<String, String>>> classes = List.of(
        Map.of("src0",
            Map.of(
                STILL_EXISTS_JAVA_FILE, """
        package p.a;
        public class StillExists {
            public static void still_covered_same_code() {
                System.out.println("still_covered_same_code");
            }
            public static void still_covered_code_added() {
                System.out.println("still_covered_code_added");
            }
            public static void still_covered_code_changed() {
                System.out.println("still_covered_code_changed");
            }
            public static void still_uncovered_same_code() {
                System.out.println("still_uncovered_same_code");
            }
            public static void lost_coverage() {
                System.out.println("lost_coverage");
            }
            public static void got_coverage() {
                System.out.println("got_coverage");
            }
            public static void removed() {
                System.out.println("removed");
            }
            public static void main(String[] argv) {
                StillExists.still_covered_same_code();
                StillExists.still_covered_code_added();
                StillExists.still_covered_code_changed();
                StillExists.lost_coverage();
                StillExists.removed();
                StillExists.Inner.inner_lost_coverage();
                p.b.Removed.b();
            }
            public static interface Inner {
                public static void inner_lost_coverage() {
                    System.out.println("inner_lost_coverage");
                }
                public static void inner_got_coverage() {
                    System.out.println("inner_got_coverage");
                }
            }
        }
        """,
        "p/b/Removed.java", """
        package p.b;
        public class Removed {
            public static void b() {System.out.println("b");}
        }
        """),
            "src1",
            Map.of("p/d/EmptyOuter.java", """
        package p.d;
        public interface EmptyOuter {
            public static class FullInner {
                public void method() {};
            }
        }
        """)),
        Map.of("src0",
            Map.of(
                STILL_EXISTS_JAVA_FILE, """
        package p.a;
        public class StillExists {
            public enum TYPE {ELEM_1, ELEM_2};
            public static void still_covered_same_code() {
                System.out.println("still_covered_same_code");
            }
            public static void still_covered_code_added() {
                System.out.println("still_covered_code_added");
                System.out.println("added code");
            }
            public static void still_covered_code_changed() {
                System.out.println("changed code");
            }
            public static void still_uncovered_same_code() {
                System.out.println("still_uncovered_same_code");
            }
            public static void lost_coverage() {
                System.out.println("lost_coverage");
            }
            public static void got_coverage() {
                System.out.println("got_coverage");
            }
            public static void added() {
                System.out.println("added");
            }
            public static TYPE type = TYPE.ELEM_1;
            public static void main(String[] argv) {
                StillExists.still_covered_same_code();
                StillExists.still_covered_code_added();
                StillExists.still_covered_code_changed();
                StillExists.got_coverage();
                StillExists.added();
                StillExists.Inner.inner_got_coverage();
                p.c.Added.c();
            }
            public static class Inner {
                public static void inner_lost_coverage() {
                    System.out.println("inner_lost_coverage");
                }
                public static void inner_got_coverage() {
                    System.out.println("inner_got_coverage");
                }
            }
        }
        """,
        "p/c/Added.java", """
        package p.c;
        public class Added {
            public static void c() {System.out.println("c");}
        }
        """),
        "src1",
        Map.of("p/d/EmptyOuter.java", """
        package p.d;
        public interface EmptyOuter {
            public static class FullInner {
                public void method() {};
            }
        }
        """)));
    Path[] instrumentedDirs = new Path[2];
    Path wd;
    List<Path>[] sourceDirs = new List[2];
    Path[] repoDirs = new Path[2];
    JCovMethodCoverageComparison items;
    SourcePath newSource;
    DataRoot newCoverage, oldCoverage;
    private FileSet files;

    @BeforeMethod
    public void setup() throws IOException, InterruptedException, FileFormatException {
        wd = Files.createTempDirectory("lost_found_");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        for (int i = 0; i < 2; i++) {
            var repo = wd.resolve("source_" + i);
            repoDirs[i] = repo;
            sourceDirs[i] = new ArrayList<>();
            for (var src : classes.get(i).keySet()) {
                List<File> javaFiles = new ArrayList<>();
                var sourceDir = repo.resolve(src);
                sourceDirs[i].add(sourceDir);
                for (var f : classes.get(i).get(src).keySet()) {
                    var dirName = f.substring(0, f.lastIndexOf('/'));
                    var fileName = f.substring(f.lastIndexOf('/') + 1);
                    var packageDir = repo.resolve(src).resolve(dirName);
                    Files.createDirectories(packageDir);
                    var javaFile = packageDir.resolve(fileName);
                    Files.write(javaFile, List.of(classes.get(i).get(src).get(f)));
                    javaFiles.add(javaFile.toFile());
                }
                Iterable<? extends JavaFileObject> cu = fileManager.getJavaFileObjectsFromFiles(javaFiles);
                compiler.getTask(null, fileManager, null, null, null, cu).call();
            }
        }
        //instrument
        for (int i = 0; i < 2; i++) {
            var instrumented = wd.resolve("instrumented_" + i);
            instrumentedDirs[i] = instrumented;
            var command = new ArrayList<String>();
            command.addAll(List.of(Path.of(System.getProperty("java.home")).resolve("bin").resolve("java").toString(),
                    "-cp", System.getProperty("java.class.path"),
                    Instr.class.getName(),
                    "-rt", findRT(),
                    "-t", repoDirs[i].resolve("template.xml").toString(),
                    "-o", instrumented.toString()));
            command.addAll(sourceDirs[i].stream().map(Path::toString).collect(Collectors.toList()));
            assertEquals(0, new ProcessBuilder(command).inheritIO().start().waitFor());
        }
        //run
        for (int i = 0; i < 2; i++) {
            Files.move(repoDirs[i].resolve("template.xml"), instrumentedDirs[i].resolve("template.xml"));

            assertEquals(0, new ProcessBuilder(
                    Path.of(System.getProperty("java.home")).resolve("bin").resolve("java").toString(),
                    "-cp", instrumentedDirs[i].toString() + System.getProperty("path.separator") + System.getProperty("java.class.path"),
                    "p.a.StillExists").inheritIO().directory(instrumentedDirs[i].toFile()).start().waitFor());
        }
        newCoverage = DataRoot.read(instrumentedDirs[1].resolve("result.xml").toString());
        oldCoverage = DataRoot.read(instrumentedDirs[0].resolve("result.xml").toString());
        newSource = new SourcePath(repoDirs[1], sourceDirs[1]);
        items = new JCovMethodCoverageComparison(
                oldCoverage,
                newCoverage, f -> newSource.toFile(f));
        files = new FileSet(classes.get(1).entrySet().stream()
                .flatMap(e -> e.getValue().keySet().stream().map(f -> e.getKey() + "/" + f))
                .collect(Collectors.toSet()));
    }
    private String findRT() {
        return Arrays.stream(System.getProperty("java.class.path").split(":"))
                .filter(p -> p.endsWith("jcov_file_saver.jar")).findAny().get();
    }
    private FileItems.FileItem find(String clss, String method) {
        return items.items(newSource.toFile(clss)).stream().filter(i -> i.item().equals(method + "()V")).findAny().get();
    }
    @Test
    void testItems() {
        assertEquals(find(STILL_EXISTS_JAVA_FILE, "still_covered_same_code").quality(), FileItems.Quality.GOOD);
        assertEquals(find(STILL_EXISTS_JAVA_FILE, "still_covered_code_added").quality(), FileItems.Quality.GOOD);
        assertEquals(find(STILL_EXISTS_JAVA_FILE, "still_uncovered_same_code").quality(), FileItems.Quality.SO_SO);
        assertEquals(find(STILL_EXISTS_JAVA_FILE, "lost_coverage").quality(), FileItems.Quality.BAD);
        assertEquals(find(STILL_EXISTS_JAVA_FILE, "added").quality(), FileItems.Quality.IGNORE);
        assertEquals(find(STILL_EXISTS_JAVA_FILE, "Inner$inner_got_coverage").quality(), FileItems.Quality.GOOD);
        assertEquals(find(STILL_EXISTS_JAVA_FILE, "Inner$inner_lost_coverage").quality(), FileItems.Quality.BAD);
        assertEquals(1, items.items(newSource.toFile(STILL_EXISTS_JAVA_FILE)).stream().filter(i -> i.item().
                equals("added()V")).count());
        assertEquals(0, items.items(newSource.toFile(STILL_EXISTS_JAVA_FILE)).stream().filter(i -> i.item().
                equals("removed()V")).count());
        assertNotNull(items.items(newSource.toFile("p/c/Added.java")));
        assertNull(items.items(newSource.toFile("p/b/Removed.java")));
        assertEquals(find("p/c/Added.java", "c").quality(), FileItems.Quality.IGNORE);
    }
    @Test(dependsOnMethods = "testItems")
    void testReport() throws Exception {
        var reportDir = wd.resolve("report");
        new MultiHTMLReport.Builder()
                .setItems(items)
                .setCoverage(new JCovLineCoverage(newCoverage, newSource))
                .setFolderHeader(s -> "<h1>Lost/kept method coverage</h1>")
                .setFileHeader(s -> "<h1>Lost/kept method coverage</h1>")
                .setTitle("Lost/kept method coverage")
                .setSource(newSource)
                .setFiles(files)
                .report().report(reportDir);
        var content = Files.readAllLines(reportDir.resolve("index.html"));
        assertTrue(content.contains("Line coverage: 69.00%(24/35)"));
        assertTrue(content.stream().anyMatch(l ->
                Pattern.matches(".*class=\"item_not_so_good\".*Lost.*2.*", l)));
        content = Files.readAllLines(reportDir.resolve("src0_p_a_StillExists.java.html"));
        assertTrue(content.stream().anyMatch(l ->
                Pattern.matches(".*item_not_so_good.*lost_coverage\\(\\)V.*", l)));
        assertTrue(content.stream().anyMatch(l ->
                Pattern.matches(
                        ".*href=\"#item_added\\(\\)V\".*class=\"item_ignore\".*title=\"added\\(\\)" +
                                "V.*System.out.println\\(\"added\"\\);.*",
                        l)));
    }
    private List<CoveredLineRange> qualityAtLine(FileCoverage coverage, String file, int line) {
        var fileCov = coverage.ranges(file);
        return fileCov.stream().filter(lr -> lr.first() >= line && lr.last() <= line)
                .collect(Collectors.toList());
    }
    @Test(dependsOnMethods = "testItems")
    void testComparisonReport() throws Exception {
        JCovCoverageComparison comparison = new JCovCoverageComparison(
                oldCoverage, new SourcePath(repoDirs[0], sourceDirs[0]),
                newCoverage, newSource);
        testQuality(comparison, "src0/" + STILL_EXISTS_JAVA_FILE, 5, 1, FileItems.Quality.GOOD);
        testQuality(comparison, "src0/" + STILL_EXISTS_JAVA_FILE, 15, 1, FileItems.Quality.SO_SO);
        testQuality(comparison, "src0/" + STILL_EXISTS_JAVA_FILE, 18, 1, FileItems.Quality.BAD);
        assertNull(comparison.ranges("src0/p/a/Removed.java"));
        assertEquals(comparison.ranges("src0/p/c/Added.java").size(), 0);
        var reportDir = wd.resolve("report");
        var report = new MultiHTMLReport.Builder()
                .setItems(items)
                .setCoverage(comparison)
                .setFolderHeader(s -> "<h1>Lost/kept method coverage</h1>")
                .setFileHeader(s -> "<h1>Lost/kept method coverage</h1>")
                .setTitle("Lost/kept method coverage")
                .setSource(newSource)
                .setFiles(files)
                .report();
        report.report(reportDir);
        assertTrue(Files.exists(reportDir.resolve("src0_p_c_Added.java.html")));
    }

    void testQuality(FileCoverage coverage, String file, int line, int expectedSize,
                     FileItems.Quality... expected) {
        var lineCoverage = qualityAtLine(coverage, file, line);
        assertEquals(lineCoverage.size(), expectedSize);
        if(expectedSize > 0)
            assertTrue(lineCoverage.stream().anyMatch(lr -> lr.coverage().quality() == expected[0]));
    }
}
