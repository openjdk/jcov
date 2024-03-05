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

import com.sun.tdk.jcov.instrument.DataMethod;
import com.sun.tdk.jcov.instrument.DataRoot;
import com.sun.tdk.jcov.report.MethodCoverage;
import openjdk.codetools.jcov.report.FileItems;
import openjdk.codetools.jcov.report.LineRange;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

public class JCovMethodCoverageComparison implements FileItems {
    private final Map<String, List<FileItem>> items = new HashMap<>();

    public JCovMethodCoverageComparison(DataRoot oldCoverage, DataRoot newCoverage, Function<String, String> fileResolver) {
        var oldCache = oldCoverage.getClasses().stream().flatMap(c -> c.getMethods().stream())
                .collect(toMap(m -> methodID(m), m -> m));
        for (var newClass : newCoverage.getClasses()) {
//            if(!newClass.getFullname().equals("jdk/internal/reflect/Label")) continue;
            for (var newMethod : newClass.getMethods()) {
                String className = newClass.getFullname();
                String methodName = newMethod.getName() + newMethod.getVmSignature();
                if (newClass.getFullname().contains("$")) {
                    methodName = className.substring(className.indexOf('$') + 1) + "$" + methodName;
                    className = className.substring(0, className.indexOf('$'));
                }
                String id = className + "#" + methodName;
                FileItem toAdd;
                if (oldCache.containsKey(id)) {
                    var oldMethod = oldCache.get(id);
                    Quality quality;
                    if (newMethod.wasHit()) quality = Quality.GOOD;
                    else if (oldMethod.wasHit()) quality = Quality.BAD;
                    else quality = Quality.SO_SO;
                    toAdd = new MethodItem(newMethod, methodName, quality);
                } else {
                    toAdd = new MethodItem(newMethod, methodName, Quality.IGNORE);
                }
                List<FileItem> classItems;
                String fileName = fileResolver.apply(className + ".java");
                if (items.containsKey(fileName)) classItems = items.get(fileName);
                else {
                    classItems = new ArrayList<>();
                    items.put(fileName, classItems);
                }
                classItems.add(toAdd);
            }
        }
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

    private static List<LineRange> methodRanges(DataMethod m) {
        Map<Integer, Boolean> result = new HashMap<>();
        var lc = new MethodCoverage(m, true).getLineCoverage();
        for (int i = (int) lc.firstLine(); i <= lc.lastLine(); i++) {
            if (lc.isCode(i)) {
                if (result.get(i) == null || !result.get(i).booleanValue())
                    result.put(i, lc.isLineCovered(i));
            }
        }
        return result.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .map(le -> new LineRange(le.getKey(), le.getKey()))
                .collect(Collectors.toList());
    }

    private static class MethodItem implements FileItem {
        private final String id;
        private final Quality quality;
        private final List<LineRange> ranges;

        private MethodItem(DataMethod method, String displayName, Quality quality) {
            this.id = displayName;
            this.quality = quality;
            var lt = method.getLineTable();
            var mc = new MethodCoverage(method, true);
            ranges = methodRanges(method);
//            range = lt != null ? new LineRange(lt.get(0).line, lt.get(lt.size() - 1).line) : null;
        }

        @Override
        public String item() {
            return id;
        }

        @Override
        public List<LineRange> ranges() {
            return ranges;
        }

        @Override
        public Quality quality() {
            return quality;
        }
    }

    @Override
    public List<FileItem> items(String file) {
        return items.get(file);
    }

    @Override
    public String kind() {
        return "Methods";
    }

    @Override
    public Map<Quality, String> legend() {
        return Map.of(
                Quality.GOOD, "Covered",
                Quality.BAD, "Lost",
                Quality.SO_SO, "Uncovered",
                Quality.IGNORE, "New"
        );
    }
}
