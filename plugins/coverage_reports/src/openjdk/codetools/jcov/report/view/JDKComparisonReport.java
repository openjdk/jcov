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

import com.sun.tdk.jcov.data.FileFormatException;
import com.sun.tdk.jcov.instrument.DataRoot;
import openjdk.codetools.jcov.report.FileSet;
import openjdk.codetools.jcov.report.jcov.JCovCoverageComparison;
import openjdk.codetools.jcov.report.jcov.JCovMethodCoverageComparison;
import openjdk.codetools.jcov.report.source.SourceHierarchy;
import openjdk.codetools.jcov.report.source.SourceHierarchyUnion;
import openjdk.codetools.jcov.report.source.SourcePath;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static openjdk.codetools.jcov.report.view.MultiHTMLReport.toReport;

public class JDKComparisonReport {
    private final static String USAGE = """
        java ... openjdk.codetools.jcov.report.view.JDKComparisonReport \\
            <first coverage> <first source> \\
            <second coverage> <second source> \\
            <report directory>
        Where:
            (first|second)_coverage - JCov XML coverage files to compare
            (first|second)_source - JDK source hierarchies corresponding to the coverage files
            <report directory> - must not exist or be empty
    """;
    public static void main(String[] args) throws Exception {
        try {
            SourceHierarchy oldSourcePath = jdkSourceHierarchy(args[1]);
            SourceHierarchy newSourcePath = jdkSourceHierarchy(args[3]);
            var reportDir = Path.of(args[4]);
            long start = System.currentTimeMillis();
            System.out.println("reading coverage at " + (System.currentTimeMillis() - start));
            //data reading seems to be fundamentally thread unsafe
//            DataRoot[] coverage = new DataRoot[2];
//            var oldSourceThread = startFileLoading(args[0], d -> coverage[0] = d);
//            var newSourceThread = startFileLoading(args[2], d -> coverage[1] = d);
//            oldSourceThread.join(); newSourceThread.join();
            DataRoot[] coverage = new DataRoot[] {DataRoot.read(args[0]), DataRoot.read(args[2])};
            System.out.println("comparing method coverage at " + (System.currentTimeMillis() - start));
            var items = new JCovMethodCoverageComparison(coverage[0], coverage[1], f -> newSourcePath.toFile(f));
            System.out.println("comparing coverage at " + (System.currentTimeMillis() - start));
            JCovCoverageComparison comparison = new JCovCoverageComparison(
                    coverage[0], oldSourcePath,
                    coverage[1], newSourcePath);
            var noSourceClasses = new HashSet<String>();
            var classes = coverage[1].getClasses().stream()
                    .map(dc -> {
                        var cn = dc.getFullname();
                        cn = cn.contains("$") ? cn.substring(0, cn.indexOf("$")) : cn;
                        if (!noSourceClasses.contains(cn)) {
                            var res = newSourcePath.toFile(cn.replace('.', '/') + ".java");
                            if (res == null) noSourceClasses.add(cn);
                            return res;
                        } return null;
                    })
                    .filter(c -> c != null)
                    .collect(Collectors.toSet());
            noSourceClasses.stream().sorted().forEach(cn -> System.err.println("No source file for " + cn));
//            classes = Set.of("jdk/internal/reflect/Label.java");
            System.out.println("generating report at " + (System.currentTimeMillis() - start));
            var reportLegenLink = "<a style=\"float:right\" href=\"JDKComparisonReport.html\">What am I looking at?</a>";
            var report = new MultiHTMLReport.Builder().setItems(items)
                    .setCoverage(comparison)
                    .setFolderHeader(s -> "<h1>Lost/kept coverage "+ (!s.isEmpty() ? "for package " + s : "") + "</h1>" + reportLegenLink)
                    .setFileHeader(s -> "<h1>Lost/kept coverage in class " + s + "</h1>" + reportLegenLink)
                    .setTitle("Lost/kept method coverage")
                    .setSource(newSourcePath)
                    .setFiles(new FileSet(classes))
                    .report();
            report.report(reportDir);
            toReport("JDKComparisonReport.html", reportDir);
         } catch (Exception e) {
            System.err.println(USAGE);
            throw e;
        }
    }

    public static SourceHierarchy jdkSourceHierarchy(String description) {
        var roots = new ArrayList<SourceHierarchy>();
        var names = new HashMap<SourceHierarchy, Path>();
        Arrays.stream(description.split(":")).forEach(sp -> {
            String path;
            String name;
            int nameIndex = sp.indexOf("=");
            if(nameIndex > -1) {
                path = sp.substring(0, nameIndex);
                name = sp.substring(nameIndex + 1);
            } else {
                name = null;
                path = sp;
            }
            SourceHierarchy sh = JDKDiffCoverageReport.jdkSource(path);
            roots.add(sh);
            if (name != null) names.put(sh, Path.of(name));
        });
        return new SourceHierarchyUnion(roots, names);
    }
//    private static Thread startFileLoading(String file, Consumer<DataRoot> consumer) {
//        var result = new Thread(() -> {
//            try {
//                System.out.println("Reading " + file);
//                consumer.accept(DataRoot.read(file));
//            } catch (FileFormatException e) {
//                throw new RuntimeException(e);
//            }
//        });
//        result.start();
//        return result;
//    }
}
