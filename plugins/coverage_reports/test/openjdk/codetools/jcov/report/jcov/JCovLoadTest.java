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

import openjdk.codetools.jcov.report.Coverage;
import openjdk.codetools.jcov.report.CoveredLineRange;
import openjdk.codetools.jcov.report.LineRange;
import openjdk.codetools.jcov.report.filter.GitDifFilterTest;
import com.sun.tdk.jcov.data.FileFormatException;
import com.sun.tdk.jcov.instrument.DataRoot;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

public class JCovLoadTest {
    static JCovLineCoverage coverage;

    @BeforeClass
    static void init() throws FileFormatException, IOException {
        var xmlName = JCovLoadTest.class.getName().replace('.', '/');
        xmlName = "/" + xmlName.substring(0, xmlName.lastIndexOf('/')) + "/ObjectInputStream.xml";
        coverage = new JCovLineCoverage(DataRoot.read(GitDifFilterTest.cp(xmlName).toString()));
    }

    private static Boolean covered(List<CoveredLineRange> ranges, int line) {
        var range = ranges.stream()
                .filter(e -> e.compare(line) == 0).findAny();
        if (range.isEmpty()) {
            System.out.println("Nothing for line " + line);
            return null;
        }
        var e = range.get();
        System.out.printf("For line %d found (%d,%d) -> %s\n", line, e.first(), e.last(), e.coverage());
        return e.coverage().covered() > 0;
    }
    /*
1454:      * @throws StreamCorruptedException if arrayLength is negative
...
1458:         if (! arrayType.isArray()) {
1459:             throw new IllegalArgumentException("not an array type");
1460:         }
...
1462:         if (arrayLength < 0) {
1463:             throw new StreamCorruptedException("Array length is negative");
1464:         }
...
2141:         if (len < 0) {
2142:             throw new StreamCorruptedException("Array length is negative");
2143:         }
     */
    @Test
    void basic() {
        var ranges = coverage.ranges("java/io/ObjectInputStream.java");
        assertNull(covered(ranges, 1454));
        assertTrue(covered(ranges, 1458));
        assertFalse(covered(ranges, 1459));
        assertNull(covered(ranges, 1460));
        assertTrue(covered(ranges, 2141));
        assertTrue(covered(ranges, 2142));
        assertNull(covered(ranges, 2143));
    }

    @Test
    void innerClass() {
        var ranges = coverage.ranges("java/io/ObjectInputStream.java");
        assertTrue(covered(ranges, 3035));
        assertTrue(covered(ranges, 3036));
        assertNull(covered(ranges, 3038));
    }
}
