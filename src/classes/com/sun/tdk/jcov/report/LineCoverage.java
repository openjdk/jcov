/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.jcov.report;

import com.sun.tdk.jcov.instrument.DataMethod.LineEntry;
import java.util.HashMap;
import java.util.List;

/**
 * <p> Class providing information about line coverage - it contains numbers of
 * covered and uncovered lines. </p>
 *
 * @author Dmitry Fazunenko
 */
public class LineCoverage extends CoverageData {

    final HashMap<Long, Boolean> lines_hits = new HashMap<Long, Boolean>();

    public LineCoverage() {
    }

    /**
     * Return true if passed line is covered, false otherwise.
     *
     * @param lineNum line number
     * @return true if passed line is covered, false otherwise.
     */
    public boolean isLineCovered(long lineNum) {
        Boolean isHit = lines_hits.get(lineNum);
        return isHit != null && isHit.booleanValue();

    }

    /**
     * Return true if the line with the given number contains java code
     *
     * @param lineNum line number
     * @return true if the line with the given number contains java code
     */
    public boolean isCode(long lineNum) {
        return lines_hits.get(lineNum) != null;
    }

    /**
     * Merges given lineTable with the own one
     *
     * @param lineTable
     */
    void processLineTable(final List<LineEntry> lineTable) {
        if (lineTable == null) {
            return;
        }
        for (LineEntry le : lineTable) {
            ++total;
            lines_hits.put((long) le.line, false);
        }
    }

    /**
     * Marks the given line as covered. Does nothing if the line is not in the
     * coverage
     *
     * @param line - line number
     */
    public void hitLine(long line) {
        Boolean wasHit = lines_hits.get(line);
        if (wasHit != null) {
            boolean was = lines_hits.put(line, true);
            if (!was) {
                ++covered;
            }
        }
    }

    private void hitLine(long line, boolean isHit) {
        Boolean wasHit = lines_hits.get(line);
        if (wasHit != null) {
            if (!wasHit && isHit) {
                ++covered;
            } else if (wasHit && !isHit) {
                --covered;
            }
            lines_hits.put(line, isHit || wasHit);
        } else {
            if (isHit) {
                ++covered;
            } // no else as this line was not hit
            ++total;
            lines_hits.put(line, isHit);
        }
    }

    /**
     * Merges the given LineCoverage data with the own ones.
     *
     * @param lineCov - coverage data to merge
     */
    void processLineCoverage(LineCoverage lineCov) {
        for (long line : lineCov.lines_hits.keySet()) {
            hitLine(line, lineCov.lines_hits.get(line));
        }
    }

    /**
     * @return line having the smallest number
     */
    public long firstLine() {
        long firstLine = -1;
        for (long lineNum : lines_hits.keySet()) {
            if (firstLine < 0 || lineNum < firstLine) {
                firstLine = lineNum;
            }
        }
        return firstLine < 0 ? 1 : firstLine;
    }

    /**
     * @return line having the largest number
     */
    public long lastLine() {
        long lastLine = -1;
        for (long lineNum : lines_hits.keySet()) {
            if (lastLine < 0 || lineNum > lastLine) {
                lastLine = lineNum;
            }
        }
        return lastLine < 0 ? 1 : lastLine;
    }
}
