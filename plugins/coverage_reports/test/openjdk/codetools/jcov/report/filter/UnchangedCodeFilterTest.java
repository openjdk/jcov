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
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import static org.testng.Assert.assertEquals;

public class UnchangedCodeFilterTest {
    public static final String FILE_1 = "file1";
    SourceHierarchy oldSource = new SourceHierarchy() {
        @Override
        public List<String> readFile(String file) throws IOException {
            return file.equals(FILE_1) ? List.of("1", "2", "3", "4", "5", "6") : List.of();
        }

        @Override
        public String toClassFile(String file) {
            return null;
        }

        @Override
        public String toFile(String classFileName) {return classFileName;}
    };
    Function<String, List<UnchangedCodeFilter.NamedLineRange>> oldParts = (file) -> {
        return file.equals(FILE_1) ? List.of(
                new UnchangedCodeFilter.NamedLineRange("12", 1, 2),
                new UnchangedCodeFilter.NamedLineRange("34", 3, 4),
                new UnchangedCodeFilter.NamedLineRange("56", 5, 6)
        ) : List.of();
    };
    SourceHierarchy newSource = new SourceHierarchy() {
        @Override
        public List<String> readFile(String file) throws IOException {
            return file.equals(FILE_1) ? List.of("", "1", "3", "4", "4", "5", "6") : List.of();
        }

        @Override
        public String toClassFile(String file) {
            return null;
        }

        @Override
        public String toFile(String classFileName) {return classFileName;}
    };
    Function<String, List<UnchangedCodeFilter.NamedLineRange>> newParts = (file) -> {
        return file.equals(FILE_1) ? List.of(
                new UnchangedCodeFilter.NamedLineRange("12", 2, 3),
                new UnchangedCodeFilter.NamedLineRange("34", 4, 5),
                new UnchangedCodeFilter.NamedLineRange("56", 6, 7)
        ) : List.of();
    };
    @Test
    void test() {
        var filter = new UnchangedCodeFilter(oldSource, oldParts, newSource, newParts);
        var ranges = filter.ranges(FILE_1);
        assertEquals(ranges.size(), 2);
        assertEquals(ranges.get(0).toString(), "[2,2]");
        assertEquals(ranges.get(1).toString(), "[6,7]");
    }
}
