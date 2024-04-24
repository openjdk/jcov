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
package openjdk.codetools.jcov.report.view.jdk;

import com.sun.tdk.jcov.instrument.DataRoot;
import openjdk.codetools.jcov.report.FileSet;
import openjdk.codetools.jcov.report.commandline.CommandLine;
import openjdk.codetools.jcov.report.commandline.Option;
import openjdk.codetools.jcov.report.commandline.Parameter;
import openjdk.codetools.jcov.report.filter.GitDiffFilter;
import openjdk.codetools.jcov.report.jcov.JCovLineCoverage;
import openjdk.codetools.jcov.report.source.ContextFilter;
import openjdk.codetools.jcov.report.source.SourcePath;
import openjdk.codetools.jcov.report.view.MultiHTMLReport;
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
    private static final Option FORMAT = new Option.Builder().option("--format").name("format")
            .description("single|multi|text for single|multi html report or a text report").create();
    private static final Option SOURCE = new Option.Builder().option("--source").name("source")
            .description("JDK source root").create();
    public static final Option COVERAGE = new Option.Builder().option("--coverage").name("coverage")
            .description("JCov coverage file").create();
    public static final Option DIFF = new Option.Builder().option("--diff").name("diff")
            .description("Git diff file from the tip to a revision in the past produced with -U0 option").create();
    public static final Option REPORT = new Option.Builder().option("--report").name("report")
            .description("Report output").create();
    public static final Option TITLE = new Option.Builder().option("--title").name("title").optional(true)
            .description("Report title").create();
    public static final Option HEADER = new Option.Builder().option("--header").name("header").optional(true)
            .description("Report header").create();
    private static final CommandLine commandLine = new CommandLine.Builder()
            .option(FORMAT)
            .option(SOURCE)
            .option(COVERAGE)
            .option(DIFF)
            .option(REPORT)
            .option(TITLE)
            .option(HEADER)
            .create();
    public static void main(String[] args) throws Exception {
        try {
            var command = commandLine.parse(args);
            var source = jdkSource(List.of(Path.of(command.get(SOURCE))));
            var coverage = new JCovLineCoverage(DataRoot.read(command.get(COVERAGE)), source);
            var diff = GitDiffFilter.parseDiff(Path.of(command.get(DIFF))/*, source.roots(List.of(Path.of(args[1])))*/);
            String reportFile = command.get(REPORT);
            String title = command.getOrElse(TITLE, "");
            String header = command.getOrElse(HEADER, "");
            switch (command.get(FORMAT)) {
                case "single":
                    new SingleHTMLReport.Builder().setSource(source).setFiles(new FileSet(diff.files()))
                            .setCoverage(coverage).setTitle(title).setHeader(header).setHighlight(diff)
                            .setInclude(new ContextFilter(diff, 10)).report()
                            .report(Path.of(reportFile));
                    break;
                case "multi":
                    new MultiHTMLReport.Builder().setSource(source).setFiles(new FileSet(diff.files()))
                            .setCoverage(coverage).setTitle(title)
                            .setFolderHeader(s -> header).setFileHeader(s -> header)
                            .setHighlight(diff)
                            .setInclude(new ContextFilter(diff, 10)).report()
                            .report(Path.of(reportFile));
                    break;
                case "text":
                    new TextReport.Builder().setSource(source).setFiles(new FileSet(diff.files())).setCoverage(coverage)
                            .setHeader(header).setFilter(diff).report()
                            .report(Path.of(reportFile));
                    break;
            }
        } catch (Throwable e) {
            System.out.println("Usage: java ... openjdk.codetools.jcov.report.view.JDKReport " +
                    commandLine.usageLine() + "\n");
            System.out.println(commandLine.usageList("    "));
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
