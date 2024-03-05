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

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class SourceHierarchyUnionTest {
    private static class SimpleSourceHierarchy implements SourceHierarchy {
        private final String file;

        private SimpleSourceHierarchy(String file) {
            this.file = file;
        }

        @Override
        public List<String> readFile(String file) throws IOException {
            return file.equals(this.file) ? List.of(this.file) : null;
        }

        @Override
        public String toClassFile(String file) {
            return file.equals(this.file) ? file : null;
        }

        @Override
        public String toFile(String classFileName) {
            return classFileName.equals(file) ? file : null;
        }
    }
    SourceHierarchy one = new SimpleSourceHierarchy("one");
    SourceHierarchy two = new SimpleSourceHierarchy("two");
    SourceHierarchy three = new SimpleSourceHierarchy("three");
    SourceHierarchy testedObject = new SourceHierarchyUnion(List.of(one, two, three),
            Map.of(two, Path.of("a"),
                    three, Path.of("b/c")));
    @Test
    void testHierarchy() throws IOException {
        assertEquals(testedObject.readFile("one").get(0), "one");
        assertEquals(testedObject.readFile("a/two").get(0), "two");
        assertEquals(testedObject.readFile("b/c/three").get(0), "three");
        assertEquals(testedObject.toClassFile("one"), "one");
        assertEquals(testedObject.toClassFile("a/two"), "two");
        assertEquals(testedObject.toClassFile("b/c/three"), "three");
        assertEquals(testedObject.toFile("one"), "one");
        assertEquals(testedObject.toFile("two"), "a/two");
        assertEquals(testedObject.toFile("three"), "b/c/three");
    }
}
