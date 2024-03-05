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
package openjdk.codetools.jcov.report.filter;

import openjdk.codetools.jcov.report.LineRange;
import openjdk.codetools.jcov.report.source.SourceHierarchy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Only include code which has not changed from last time.
 */
public class UnchangedCodeFilter implements SourceFilter {
    private final SourceHierarchy oldSource, newSource;
    private final Function<String, List<NamedLineRange>> oldFilter, newFilter;

    public UnchangedCodeFilter(SourceHierarchy oldSource, Function<String, List<NamedLineRange>> oldFilter,
                               SourceHierarchy newSource, Function<String, List<NamedLineRange>> newFilter) {
        this.oldSource = oldSource;
        this.newSource = newSource;
        this.oldFilter = oldFilter;
        this.newFilter = newFilter;
    }

    @Override
    public List<LineRange> ranges(String file) {
        try {
            var res = new ArrayList<LineRange>();
            var oldFileSource = oldSource.readFile(file);
            var newFileSource = newSource.readFile(file);
            var oldRanges = oldFilter.apply(file);
            for(var range : newFilter.apply(file)) {
                var oldRange = oldRanges.stream()
                        .filter(r -> r.name.equals(range.name))
                        .findAny();
                if (oldRange.isPresent()) {
                    int lastMatchingLine = -1;
                    for (int line = range.first(); line <= range.last(); line++) {
                        if (!oldFileSource.get(oldRange.get().first() + (line - range.first()) - 1)
                                .equals(newFileSource.get(line - 1))) break;
                        lastMatchingLine = line;
                    }
                    if(lastMatchingLine >= 0) res.add(new LineRange(range.first(), lastMatchingLine));
                }
            }
            return res;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class NamedLineRange extends LineRange {
        private final String name;
        public NamedLineRange(String name, int first, int last) {
            super(first, last);
            this.name = name;
        }

        public String name() {
            return name;
        }
    }
}
