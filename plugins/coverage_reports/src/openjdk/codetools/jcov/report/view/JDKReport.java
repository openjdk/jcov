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
import openjdk.codetools.jcov.report.filter.GitDiffFilter;
import openjdk.codetools.jcov.report.jcov.JCovLineCoverage;
import openjdk.codetools.jcov.report.source.ContextFilter;
import openjdk.codetools.jcov.report.source.JDKHierarchy;

import java.nio.file.Path;
import java.util.List;

public class JDKReport {
    public static void main(String[] args) throws Exception {
        try {
            var coverage = new JCovLineCoverage(DataRoot.read(args[0]));
            var source = new JDKHierarchy(List.of(Path.of(args[1])));
            var diff = GitDiffFilter.parseDiff(Path.of(args[2]), source.roots(List.of(Path.of(args[1]))));
            String reportFile = args[3];
            boolean isHTML = reportFile.endsWith("html");
            String title = args.length >= 5 ? args[4] : "";
            String header = args.length >= 6 ? args[5] : "";
            if (isHTML)
                new SingleHTMLReport(source, new FileSet(diff.files()), coverage,
                        title, header,
                        diff, new ContextFilter(diff, 10))
                        .report(Path.of(reportFile));
            else
                new TextReport(source, new FileSet(diff.files()), coverage, header, diff)
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
}
