/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tdk.jcov.data.FileFormatException;
import com.sun.tdk.jcov.instrument.DataMethod;
import com.sun.tdk.jcov.instrument.DataRoot;
import com.sun.tdk.jcov.report.LineCoverage;
import com.sun.tdk.jcov.report.MethodCoverage;
import openjdk.codetools.jcov.report.Coverage;
import openjdk.codetools.jcov.report.CoveredLineRange;
import openjdk.codetools.jcov.report.FileCoverage;
import openjdk.codetools.jcov.report.FileItems;
import openjdk.codetools.jcov.report.source.SourceHierarchy;
import openjdk.codetools.jcov.report.view.jdk.JDKDiffCoverageReport;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toMap;
import static openjdk.codetools.jcov.report.jcov.JCovMethodCoverageComparison.DEFAULT_COLORING;

public class JCovCoverageComparison implements FileCoverage {
    private final Map<String, List<CoveredLineRange>> cache = new HashMap<>();
    private final SourceHierarchy newSource;

    public JCovCoverageComparison(DataRoot oldCoverage, SourceHierarchy oldSource,
                                  DataRoot newCoverage, SourceHierarchy newSource) throws IOException {
        this(oldCoverage, oldSource, newCoverage, newSource, DEFAULT_COLORING);
    }
    public JCovCoverageComparison(DataRoot oldCoverage, SourceHierarchy oldSource,
                                  DataRoot newCoverage, SourceHierarchy newSource,
                                  BiFunction<Boolean, Boolean, FileItems.Quality> coloring) throws IOException {
        this.newSource = newSource;
        Set<String> oldClasses =  oldCoverage.getClasses().stream().map(dc -> dc.getFullname())
                .collect(Collectors.toSet());
        Map<String, DataMethod> oldMethodsCache = oldCoverage.getClasses().stream().flatMap(c -> c.getMethods().stream())
                .collect(toMap(m -> methodID(m), m -> m));
        for (var newClass : newCoverage.getClasses()) {
            boolean isOld = oldClasses.contains(newClass.getFullname());
            var newClassName = newClass.getFullname();
            newClassName = newClassName.contains("$") ? newClassName.substring(0, newClassName.indexOf("$")) : newClassName;
            var newFile =newSource.toFile( newClassName + ".java");
            if(newFile != null) { //null would happen if no source for a class found. Nothing to do.
                var newFileSource = newSource.readFile(newFile);
                List<String> oldFileSource;
                if(isOld) {
                    oldFileSource = oldSource != null ? oldSource.readFile(newFile) : newFileSource;
                    if (oldFileSource == null) {
                        System.err.println("Warning: no reference source for " + newClass.getFullname());
                    }
                } else oldFileSource = null;
                for (var newMethod : newClass.getMethods()) {
                    //no code for synthetic methods - no way to compare the code
                    //TODO are there non-synthetic generated methods with no code? enum methods?
                    if (!newMethod.getModifiers().isSynthetic()) {
                        String className = newClass.getFullname();
                        String methodName = newMethod.getName() + newMethod.getVmSignature();
                        if (className.contains("$")) {
                            methodName = className.substring(className.indexOf('$') + 1) + "$" + methodName;
                            className = className.substring(0, className.indexOf('$'));
                        }
                        var ranges = cache.get(className);
                        if (ranges == null) {
                            ranges = new ArrayList<>();
                            cache.put(className, ranges);
                        }
                        String id = className + "#" + methodName;
                        var isOldMethod = oldSource == null || oldMethodsCache.containsKey(id);
                        DataMethod oldMethod = isOldMethod ? oldMethodsCache.get(id) : null;
                        var newLineCoverage = new MethodCoverage(newMethod, true).getLineCoverage();
                        var oldLineCoverage = isOld && isOldMethod ?
                                new MethodCoverage(oldMethod, true).getLineCoverage() : null;
                        if(newFileSource.size() < newLineCoverage.lastLine() - 1) {
                            System.err.println("Wrong source for method " + className + "." + methodName);
                            System.err.println("    " + newLineCoverage.lastLine() + " lines expected but only " +
                                    newFileSource.size() + " in file: " + newFile);
                        } else {
                            var newMethodSource = methodSource(newFileSource, newLineCoverage);
                            int[] repeatedLines;
                            if (oldSource == null) {
                                repeatedLines = IntStream.range(0, newMethodSource.size()).toArray();
                            } else if (oldFileSource != null) {
                                var oldMethodSource = isOld && isOldMethod ? methodSource(oldFileSource,
                                        new MethodCoverage(oldMethod, true).getLineCoverage()) : null;
                                repeatedLines = repeatedLines(oldMethodSource, newMethodSource, newLineCoverage);
                            } else repeatedLines = new int[0];
                            for (int ln : repeatedLines)
                                if (newLineCoverage.isCode(ln + newLineCoverage.firstLine())) {
                                    FileItems.Quality rangeCoverage;
                                    if (isOld && isOldMethod) {
                                        var oldLC = oldLineCoverage.isLineCovered(oldLineCoverage.firstLine() + ln);
                                        var newLC = newLineCoverage.isLineCovered(newLineCoverage.firstLine() + ln);
                                        rangeCoverage = coloring.apply(oldLC, newLC);
                                    } else rangeCoverage = FileItems.Quality.NONE;
                                    ranges.add(new CoveredLineRange(
                                            (int) (ln + newLineCoverage.firstLine()),
                                            (int) (ln + newLineCoverage.firstLine()),
                                            new Coverage(newLineCoverage.getCovered() > 0 ? 1 : 0, 1, rangeCoverage)));
                                }
                        }
                    }
                }
            }

        }
    }

    //TODO a better diff algorithm could be used, right now only returning a top portion
    //of the code which is unchanged
    //if so also need to return a map between new and old lines
    private int[] repeatedLines(List<String> oldSource, List<String> newSource, LineCoverage lineCoverage) {
        if(oldSource == null) return new int[0];
        int length = 0;
        int[] res = new int[newSource.size()];
        for (int i = 0; i < newSource.size(); i++) {
            if(lineCoverage.isCode(lineCoverage.firstLine() + i))
                if (oldSource.size() > i && newSource.get(i).equals(oldSource.get(i))) {
                    res[length] = i;
                    length++;
                } else break;
        }
        return Arrays.copyOf(res, length);
    }

    private List<String> methodSource(List<String> classSource, LineCoverage lineCoverage) {
        return classSource.subList((int)lineCoverage.firstLine() - 1, (int)lineCoverage.lastLine());
    }

    private static String methodID(DataMethod m) {
        String className = m.getParent().getFullname();
        String methodName = m.getName() + m.getVmSignature();
        if (className.contains("$")) {
            methodName = className.substring(className.indexOf('$') + 1) + "$" + methodName;
            className = className.substring(0, className.indexOf('$'));
        }
        return className + "#" + methodName;
    }

    @Override
    public List<CoveredLineRange> ranges(String file) {
        return cache.get(newSource.toClassFile(file));
    }

    public static void main(String[] args) throws FileFormatException, IOException {
        new JCovCoverageComparison(
                DataRoot.read(args[0]), JDKDiffCoverageReport.jdkSource(List.of(Path.of(args[1]))),
                DataRoot.read(args[2]), JDKDiffCoverageReport.jdkSource(List.of(Path.of(args[3]))));
    }
}
