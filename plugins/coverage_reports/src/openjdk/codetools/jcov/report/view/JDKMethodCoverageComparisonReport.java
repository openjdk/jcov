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

import com.sun.tdk.jcov.instrument.DataRoot;
import openjdk.codetools.jcov.report.FileSet;
import openjdk.codetools.jcov.report.jcov.JCovLineCoverage;
import openjdk.codetools.jcov.report.jcov.JCovMethodCoverageComparison;
import openjdk.codetools.jcov.report.source.SourcePath;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JDKMethodCoverageComparisonReport {
    public static void main(String[] args) throws Exception {
        var source = System.getProperty("jdk.src");
        long start = System.currentTimeMillis();
        System.out.println("reading old " + (System.currentTimeMillis() - start));
        var oldCoverage = DataRoot.read(args[0]);
        System.out.println("reading new " + (System.currentTimeMillis() - start));
        var coverage = DataRoot.read(args[1]);
        System.out.println("computing difference " + (System.currentTimeMillis() - start));
        var roots = Arrays.stream(source.split(":"))
                .map(Path::of).collect(Collectors.toList());
        SourcePath sourcePath = JDKDiffCoverageReport.jdkSource(roots);
        var items = new JCovMethodCoverageComparison(
                oldCoverage,
                coverage,
                f -> {
                    if (f == null) return null;
                    var res = sourcePath.toFile(f);
                    return res != null ? res.toString() : null;
                });
        var classes = coverage.getClasses().stream()
                .map(dc -> dc.getFullname().replace('.','/') + ".java")
                .map(c -> sourcePath.toFile(c))
                .filter(c -> c != null)
                .map(c -> c.toString()).collect(Collectors.toSet());
        var reportDir = Path.of(args[2]);
        System.out.println("report " + (System.currentTimeMillis() - start));
        String infoLink =
                "<a style=\"float: right\" href=\"MethodCoverageComparisonReport.html\">Report information</a>";
        Function<String, String> headerProvider = s -> infoLink + "<h2>Method coverage comparison</h2><h2>"+ s +"</h2>";
        new MultiHTMLReport.Builder().setItems(items).setCoverage(new JCovLineCoverage(coverage, sourcePath))
                .setFolderHeader(headerProvider)
                .setFileHeader(headerProvider)
                .setTitle("Method coverage comparison")
                .setSource(sourcePath)
                .setFiles(new FileSet(classes))
                .report().report(reportDir);
        MultiHTMLReport.toReport("MethodCoverageComparisonReport.html", reportDir);
        System.out.println("done " + (System.currentTimeMillis() - start));
//        new ProcessBuilder("open", reportDir.resolve("index.html").toString()).start().waitFor();
    }
}
