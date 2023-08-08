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
package openjdk.codetools.jcov.report.source;

import openjdk.codetools.jcov.report.LineRange;
import openjdk.codetools.jcov.report.filter.SourceFilter;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.max;

public class ContextFilter implements SourceFilter {
    private final SourceFilter filter;
    private final int contextLines;

    public ContextFilter(SourceFilter filter, int context) {
        this.filter = filter;
        contextLines = context;
    }

    @Override
    public List<LineRange> ranges(String file) {
        var ranges = new ArrayList<LineRange>();
        var origRanges = filter.ranges(file);
        LineRange last = null;
        int lastEnded = -1;
        for (int i = 0; i < origRanges.size(); i++) {
            if (last == null)
                last = expandRange(origRanges, i);
            else {
                if (last.last() < origRanges.get(i).first() - contextLines) {
                    ranges.add(last);
                    last = expandRange(origRanges, i);
                } else
                    last = new LineRange(last.first(),
                            origRanges.get(i).last() + contextLines);
            }
        }
        ranges.add(last);
        return ranges;
    }

    private LineRange expandRange(List<LineRange> origRanges, int i) {
        return new LineRange(
                max(origRanges.get(i).first() - contextLines, 1),
                origRanges.get(i).last() + contextLines);
    }
}
