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
import openjdk.codetools.jcov.report.filter.SourceFilter;
import openjdk.codetools.jcov.report.source.SourceHierarchy;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;


public class CoverageHierarchy {
    private final Map<String, Coverage> data;
    private final Map<String, Map<Integer, CoveredLineRange>> lineCoverage;
    private final FileCoverage coverage;
    private final SourceFilter filter;
    private final SourceHierarchy source;

    public CoverageHierarchy(Collection<String> files, SourceHierarchy source, FileCoverage coverage, SourceFilter filter) {
        data = new HashMap<>();
        lineCoverage = new HashMap<>();
        this.source = source;
        data.put("", null);
        for (String file : files) {
            lineCoverage.put(file, null);
            var components = file.split("/");
            String path = "";
            for (var component : file.split("/")) {
                data.put(path += (path.isEmpty() ? "" : "/") + component, null);
            }
        }
        this.coverage = coverage;
        this.filter = filter;
    }

    public Map<Integer, CoveredLineRange> getLineRanges(String file) {
        if (lineCoverage.containsKey(file) && lineCoverage.get(file) != null)
            return lineCoverage.get(file);
        String className = source.toClass(file);
        if (className == null) return null;
        var coverage = this.coverage.ranges(className);
        var coverageIt = coverage.iterator();
        CoveredLineRange lastCoverageRange = null;
        var fileCoverage = new HashMap<Integer, CoveredLineRange>();
        var used = new HashSet<CoveredLineRange>();
        for (var range : filter.ranges(file)) {
            for (int line = range.first(); line <= range.last() ; line++) {
                if (lastCoverageRange == null || lastCoverageRange.last() < line) {
                    while (coverageIt.hasNext() && (lastCoverageRange == null || lastCoverageRange.last() < line))
                        lastCoverageRange = coverageIt.next();
                }
                if (lastCoverageRange != null && lastCoverageRange.last() >= line && lastCoverageRange.first() <= line) {
                    fileCoverage.put(line, lastCoverageRange);
                    used.add(lastCoverageRange);
                }
            }
        }
        data.put(file, Coverage.sum(used.stream().map(CoveredLineRange::coverage).collect(Collectors.toList())));
        lineCoverage.put(file, fileCoverage);
        return fileCoverage;
    }

    public Coverage get(String fileOrDir) {
        var res = data.get(fileOrDir);
        if (res != null) return res;
        if (lineCoverage.containsKey(fileOrDir)) {
            return getLineRanges(fileOrDir) == null ? null : data.get(fileOrDir);
        }
        int[] coverage = {0, 0};
        boolean[] someCoverageFound = {false};
        data.entrySet().stream()
                .filter(e -> e.getKey().startsWith(fileOrDir + "/") && e.getKey().substring(fileOrDir.length() + 1).indexOf('/') < 0 ||
                        fileOrDir.isEmpty() && !e.getKey().isEmpty() && e.getKey().indexOf('/') < 0)
                .forEach(e -> {
                    var c = (e.getValue() == null) ? get(e.getKey()) : e.getValue();
                    if (c != null) {
                        someCoverageFound[0] = true;
                        coverage[0] += c.covered();
                        coverage[1] += c.total();
                    }
                });
        if (someCoverageFound[0]) {
            res = new Coverage(coverage[0], coverage[1]);
            data.put(fileOrDir, res);
            return res;
        } else return null;
    }


}
