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
import openjdk.codetools.jcov.report.FileItems;
import openjdk.codetools.jcov.report.FileSet;
import openjdk.codetools.jcov.report.jcov.JCovCoverageComparison;
import openjdk.codetools.jcov.report.jcov.JCovMethodCoverageComparison;
import openjdk.codetools.jcov.report.source.SourceHierarchy;
import openjdk.codetools.jcov.report.source.SourceHierarchyUnion;
import openjdk.codetools.jcov.report.view.MultiHTMLReport;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class JDKLostKeptReport {
    private final static String USAGE = """
        java ... %s \\
            <first coverage> <first source> \\
            <second coverage> <second source> \\
            <report directory>
        Where:
            (first|second)_coverage - JCov XML coverage files to compare
            (first|second)_source - JDK source hierarchies corresponding to the coverage files
            <report directory> - must not exist or be empty
    """.formatted(JDKLostKeptReport.class.getName());
    public static final String DESCRIPTION_HTML = "report-description.html";
    private final DataRoot oldCov;
    private final DataRoot newCov;
    private final SourceHierarchy oldSource;
    private final SourceHierarchy newSource;
    private final Path reportDir;
    private final BiFunction<Boolean, Boolean, FileItems.Quality> coloring;
    private final JCovMethodCoverageComparison items;
    private final JCovCoverageComparison comparison;

    public JDKLostKeptReport(DataRoot oldCov, DataRoot newCov,
                             SourceHierarchy oldSource, SourceHierarchy newSource,
                             Path dir, BiFunction<Boolean, Boolean, FileItems.Quality> coloring,
                             JCovMethodCoverageComparison items, JCovCoverageComparison comparison) {
        this.oldCov = oldCov;
        this.newCov = newCov;
        this.oldSource = oldSource;
        this.newSource = newSource;
        reportDir = dir;
        this.coloring = coloring;
        this.items = items;
        this.comparison = comparison;
    }

    public void report() throws Exception {
        try {
            DataRoot[] coverage = new DataRoot[] {oldCov, newCov};
            var noSourceClasses = new HashSet<String>();
            var classes = coverage[1].getClasses().stream()
                    .map(dc -> {
                        var cn = dc.getFullname();
                        cn = cn.contains("$") ? cn.substring(0, cn.indexOf("$")) : cn;
                        if (!noSourceClasses.contains(cn)) {
                            var res = newSource.toFile(cn.replace('.', '/') + ".java");
                            if (res == null) noSourceClasses.add(cn);
                            return res;
                        } return null;
                    })
                    .filter(c -> c != null)
                    .collect(Collectors.toSet());
            noSourceClasses.stream().sorted().forEach(cn -> System.err.println("No source file for " + cn));
            var reportLegenLink = "<a style=\"float:right\" href=\""+DESCRIPTION_HTML +"\">What am I looking at?</a>";
            var report = new MultiHTMLReport.Builder().setItems(items)
                    .setCoverage(comparison)
                    .setFolderHeader(s -> "<h1>Lost/kept coverage</h1>" + reportLegenLink)
                    .setFileHeader(s -> "<h1>Lost/kept coverage</h1>" + reportLegenLink)
                    .setTitle("Lost/kept method coverage")
                    .setSource(newSource)
                    .setFiles(new FileSet(classes))
                    .report();
            report.report(reportDir);
            MultiHTMLReport.copyToReport(JDKLostKeptReport.class, "JDKLostKeptReport.html",
                    DESCRIPTION_HTML, reportDir);
        } catch (Exception e) {
            System.err.println(USAGE);
            throw e;
        }
    }

    public static void main(String[] args) throws Exception {
        try {
            long start = System.currentTimeMillis();
            Builder builder = new Builder()
                    .oldSource(jdkSourceHierarchy(args[1]))
                    .newSource(jdkSourceHierarchy(args[3]))
                    .dir(Path.of(args[4]));
            builder.oldCov(DataRoot.read(args[0])).newCov(DataRoot.read(args[2]));
            builder.items(new JCovMethodCoverageComparison(
                    builder.oldCov, builder.newCov,
                    f -> builder.newSource.toFile(f)));
            builder.comparison(new JCovCoverageComparison(
                    builder.oldCov, builder.oldSource,
                    builder.newCov, builder.newSource));
            builder.report().report();
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

    public static class Builder {
        private DataRoot oldCov;
        private DataRoot newCov;
        private SourceHierarchy oldSource;
        private SourceHierarchy newSource;
        private Path dir;
        private JCovMethodCoverageComparison items;
        JCovCoverageComparison comparison;
        private BiFunction<Boolean, Boolean, FileItems.Quality> coloring;

        public DataRoot oldCov() {
            return oldCov;
        }

        public DataRoot newCov() {
            return newCov;
        }

        public SourceHierarchy oldSource() {
            return oldSource;
        }

        public SourceHierarchy newSource() {
            return newSource;
        }

        public Builder oldCov(DataRoot cov) {
            this.oldCov = cov;
            return this;
        }

        public Builder coloring(BiFunction<Boolean, Boolean, FileItems.Quality> coloring) {
            this.coloring = coloring;
            return this;
        }

        public Builder newCov(DataRoot cov) {
            this.newCov = cov;
            return this;
        }

        public Builder oldSource(SourceHierarchy source) {
            this.oldSource = source;
            return this;
        }

        public Builder newSource(SourceHierarchy source) {
            this.newSource = source;
            return this;
        }

        public Builder dir(Path dir) {
            this.dir = dir;
            return this;
        }

        public Builder items(JCovMethodCoverageComparison items) {
            this.items = items;
            return this;
        }

        public Builder comparison(JCovCoverageComparison comparison) {
            this.comparison = comparison;
            return this;
        }

        public JDKLostKeptReport report() {
            return new JDKLostKeptReport(oldCov, newCov, oldSource, newSource, dir, coloring, items, comparison);
        }
    }
}
