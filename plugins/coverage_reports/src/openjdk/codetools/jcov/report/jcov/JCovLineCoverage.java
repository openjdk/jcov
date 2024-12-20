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

import com.sun.tdk.jcov.instrument.DataClass;
import com.sun.tdk.jcov.instrument.DataMethod;
import com.sun.tdk.jcov.instrument.DataMethodWithBlocks;
import com.sun.tdk.jcov.instrument.DataRoot;
import com.sun.tdk.jcov.report.MethodCoverage;
import openjdk.codetools.jcov.report.Coverage;
import openjdk.codetools.jcov.report.CoveredLineRange;
import openjdk.codetools.jcov.report.FileCoverage;
import openjdk.codetools.jcov.report.source.SourceHierarchy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.Math.max;

/**
 * This uses JCov output to determine line coverage of classes
 */
public class JCovLineCoverage implements FileCoverage {
    private final DataRoot root;
    private final SourceHierarchy source;

    public JCovLineCoverage(DataRoot root, SourceHierarchy source) {
        this.root = root;
        this.source = source;
    }

    @Override
    public List<CoveredLineRange> ranges(String file) {
        var result = new ArrayList<CoveredLineRange>();
        String className = source.toClassFile(file);//file.substring(0, file.length() - ".java".length());
        root.getClasses().stream()
                .filter(dc -> dc.getFullname().equals(className) || dc.getFullname().startsWith(className + "$"))
                .forEach(dc -> result.addAll(ranges(dc)));
        Collections.sort(result);
        return result;
    }

    private static List<CoveredLineRange> ranges(DataClass cls) {
        var result = new HashMap<Integer, Boolean>();
        for (DataMethod m : cls.getMethods()) if (m instanceof DataMethodWithBlocks) {
            //TODO there is a copy-paste in other classes
            var lc = new MethodCoverage(m, true).getLineCoverage();
            for (int i = (int)lc.firstLine(); i <= lc.lastLine(); i++) {
                if (lc.isCode(i)) {
                    if (result.get(i) == null || !result.get(i).booleanValue())
                        result.put(i, lc.isLineCovered(i));
                }
            }
            //mmm but also the method declaration
            //this is unreliable
            //var lineTable = m.getLineTable().stream().sorted(Comparator.comparingInt(l -> l.bci))
            //        .collect(toList());
            //int methodLine = getLine(lineTable, 0) - 1; //can not do better without code parsing
            //if (methodLine >= 0) result.put(methodLine, methodCovered);
        }
        //below is an unsuccessful attempt to replicate the content of getLineCovreage
        //keep it around, return to it later and figure out what is wrong
        //currently the whole code marked as covered because of fallthough in clinit which
        //starts in the beginning and ends in the end of the file
//        for (DataMethod m : cls.getMethods()) if (m instanceof DataMethodWithBlocks) {
//            var added = new HashMap<DataBlock, Item>();
//            var items = new ArrayList<Item>();
//            for (DataBlock db : m.getBlocks()) {
//                if (db instanceof DataBlockTarget) {
//                    continue;
//                }
//                Item item = new Item(db, db.getCount() > 0);
//
//                boolean isNew = true;
//                for (DataBlock d : added.keySet()) {
//                    if (d.startBCI() == db.startBCI()) {
//                        if (db.getCount() > 0) added.get(d).cover();
//                        isNew = false;
//                        break;
//                    }
//                }
//                if (isNew) {
//                    added.put(db, item);
//                    items.add(item);
//                }
//            }
//            for (DataBlock db : m.getBranchTargets()) {
//                Item item = new Item(db, db.getCount() > 0);
//
//                boolean isNew = true;
//                for (DataBlock d : added.keySet()) {
//                    if (d.startBCI() == db.startBCI()) {
//                        if (db.getCount() > 0) added.get(d).cover();
//                        isNew = false;
//                        break;
//                    }
//                }
//                if (isNew) {
//                    added.put(db, item);
//                    items.add(item);
//                }
//            }
//            var lineTable = m.getLineTable().stream().sorted(Comparator.comparingInt(l -> l.bci))
//                    .collect(Collectors.toList());
//            for (var item : items) {
//                int startLine = getLine(lineTable, item.startBCI());
//                int endLine = getLine(lineTable, min(item.endBCI(), ((DataMethodWithBlocks) m).getBytecodeLength()));
//                    for (int l = startLine; l <= endLine; l++) {
//                        var lineCovered = result.containsKey(l) && result.get(l);
//                        if (!lineCovered) result.put(l, item.covered);
//                    }
//            }
//        }
        return result.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .map(le -> new CoveredLineRange(le.getKey(), le.getKey(), le.getValue() ? Coverage.COVERED : Coverage.UNCOVERED))
                .collect(Collectors.toList());
    }

//    private static int getLine(List<DataMethod.LineEntry> lineTable, int bci) {
//        int maxLine = 0;
//        for (var le : lineTable) {
//            if (le.bci > bci) return maxLine;
//            else maxLine = max(maxLine, le.line);
//        }
//        return maxLine;
//    }
//    private static class Item {
//        private final LocationRef loc;
//        private volatile boolean covered;
//
//        private Item(LocationRef loc, boolean covered) {
//            this.loc = loc;
//            this.covered = covered;
//        }
//        public void cover() {this.covered = true;}
//
//        public int startBCI() {return loc.startBCI();}
//        public int endBCI() {return loc.endBCI();}
//    }
}
