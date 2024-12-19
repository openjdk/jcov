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
import openjdk.codetools.jcov.report.LineRange;
import openjdk.codetools.jcov.report.source.SourceHierarchy;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class CoverageCacheTest {
    static final Set<String> files = Set.of("dir1/file1", "dir1/file2", "dir2/file1", "dir2/file2");
    static FileCoverage coverage;
    static Path reportFile;
    @BeforeClass
    static void init() throws IOException {
        coverage = file -> List.of(
                new CoveredLineRange(1, 1, Coverage.COVERED),
                new CoveredLineRange(3, 4, Coverage.COVERED),
                new CoveredLineRange(6, 8, Coverage.COVERED)
        );
        reportFile = Files.createTempFile("report", "txt");
    }
    @Test
    void everyEven() throws IOException {
        var cache = new CoverageHierarchy(files,
                new SourceHierarchy() {
                    @Override
                    public List<String> readFile(String file) throws IOException {
                        return Files.readAllLines(Path.of(file));
                    }

                    @Override
<<<<<<< HEAD
                    public String toClassFile(String file) {
                        return file;
                    }

                    @Override
                    public String toFile(String classFileName) {
                        return classFileName;
                    }
=======
                    public String toClass(String file) {
                        return file;
                    }
>>>>>>> 05fd4cae6a4651a07ecf85903355142573484a5a
                },
                coverage,
                file -> file.endsWith("file1") ?
                        List.of(
                                new LineRange(0, 0),
                                new LineRange(2, 2),
                                new LineRange(4, 4),
                                new LineRange(6, 6),
                                new LineRange(8, 8)
                        ) :
                        List.of());
        assertTrue(cache.get("").equals(new Coverage(4, 4)));
        assertTrue(cache.get("dir1").equals(new Coverage(2, 2)));
        assertTrue(cache.get("dir1/file1").equals(new Coverage(2, 2)));
    }
    @Test
    void getLineCoverage() throws IOException {
        var cache = new CoverageHierarchy(files,
                new SourceHierarchy() {
                    @Override
                    public List<String> readFile(String file) throws IOException {
                        return Files.readAllLines(Path.of(file));
                    }

                    @Override
<<<<<<< HEAD
                    public String toClassFile(String file) {
                        return file;
                    }
                    @Override
                    public String toFile(String classFileName) { return classFileName; }
=======
                    public String toClass(String file) {
                        return file;
                    }
>>>>>>> 05fd4cae6a4651a07ecf85903355142573484a5a
                },
                coverage,
                file -> file.endsWith("file1") ?
                List.of(
                        new LineRange(0, 0),
                        new LineRange(2, 2),
                        new LineRange(4, 4),
                        new LineRange(6, 6),
                        new LineRange(8, 8)
                ) :
                List.of());

        var lc = cache.getLineRanges("dir1/file1");
        assertEquals(lc.size(), 3);
        assertEquals(lc.get(4).coverage(), Coverage.COVERED);
        assertEquals(lc.get(6).coverage(), Coverage.COVERED);
        assertEquals(lc.get(8).coverage(), Coverage.COVERED);
    }
}
