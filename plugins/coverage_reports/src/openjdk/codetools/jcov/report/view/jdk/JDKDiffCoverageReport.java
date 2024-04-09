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
package openjdk.codetools.jcov.report.view.jdk;

import com.sun.tdk.jcov.instrument.DataRoot;
import openjdk.codetools.jcov.report.FileSet;
import openjdk.codetools.jcov.report.filter.GitDiffFilter;
import openjdk.codetools.jcov.report.jcov.JCovLineCoverage;
import openjdk.codetools.jcov.report.source.ContextFilter;
import openjdk.codetools.jcov.report.source.SourcePath;
import openjdk.codetools.jcov.report.view.SingleHTMLReport;
import openjdk.codetools.jcov.report.view.TextReport;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This is a utility class to generate report for openjdk.
 */
public class JDKDiffCoverageReport {
    public static void main(String[] args) throws Exception {
        try {
            var source = jdkSource(List.of(Path.of(args[1])));
            var coverage = new JCovLineCoverage(DataRoot.read(args[0]), source);
            var diff = GitDiffFilter.parseDiff(Path.of(args[2])/*, source.roots(List.of(Path.of(args[1])))*/);
            String reportFile = args[3];
            boolean isHTML = reportFile.endsWith("html");
            String title = args.length >= 5 ? args[4] : "";
            String header = args.length >= 6 ? args[5] : "";
            if (isHTML)
                new SingleHTMLReport.Builder().setSource(source).setFiles(new FileSet(diff.files()))
                        .setCoverage(coverage).setTitle(title).setHeader(header).setHighlight(diff)
                        .setInclude(new ContextFilter(diff, 10)).report()
                        .report(Path.of(reportFile));
//                new MultiHTMLReport.Builder().setSource(source).setFiles(new FileSet(diff.files()))
//                        .setCoverage(coverage).setTitle(title).setHeader(header).setHighlight(diff)
//                        .setInclude(new ContextFilter(diff, 10)).report()
//                        .report(Path.of(reportFile));
            else
                new TextReport.Builder().setSource(source).setFiles(new FileSet(diff.files())).setCoverage(coverage)
                        .setHeader(header).setFilter(diff).report()
                        .report(Path.of(reportFile));
        } catch (Throwable e) {
            System.out.println("Usage: java ... openjdk.codetools.jcov.report.view.JDKReport \\");
            System.out.println("    <JCov coderage file produced for the tip of the repository> \\");
            System.out.println("    <JDK source hierarchy> \\");
            System.out.println("    <git diff file from the tip to a revision in the past produced with -U0 option> \\");
            System.out.println("    <output file> \\");
            System.out.println("    <report title> <report header>");
            throw e;
        }
    }

    public static SourcePath jdkSource(String sourcePath) {
        var list = new ArrayList<Path>();
        for (var s : sourcePath.split(":")) list.add(Path.of(s));
        return jdkSource(list);
    }

    public static SourcePath jdkSource(List<Path> repos) {
        return new SourcePath(repos, repos.stream().collect(Collectors.toMap(
                repo -> repo,
                repo -> {
                    try {
                        return jdkSource(repo);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })));
    }

    public static List<Path> jdkSource(Path path) throws IOException {
        var srcRoot = path.resolve("src");
        var classesDirs = List.of("linux/classes", "unix/classes", "share/classes");
        var res = new ArrayList<Path>();
        for (var cd : classesDirs)
            Files.list(srcRoot).map(module -> module.resolve(cd)).
                    filter(Files::exists).forEach(res::add);
        return res;
    }
}
