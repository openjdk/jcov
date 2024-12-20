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

import com.sun.tdk.jcov.data.FileFormatException;
import com.sun.tdk.jcov.instrument.DataRoot;
import openjdk.codetools.jcov.report.filter.GitDifFilterTest;
import openjdk.codetools.jcov.report.source.SourceHierarchy;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.*;

public class SameLineMethods {
    static JCovLineCoverage coverage;

    @BeforeClass
    static void init() throws FileFormatException, IOException {
        var xmlName = SameLineMethods.class.getName().replace('.', '/');
        xmlName = "/" + xmlName.substring(0, xmlName.lastIndexOf('/')) + "/Fake.xml";
        coverage = new JCovLineCoverage(DataRoot.read(GitDifFilterTest.cp(xmlName).toString()), new SourceHierarchy() {
            @Override
            public List<String> readFile(String file) throws IOException {
                return null;
            }

            @Override
            public String toClassFile(String file) {
                return file.substring(0, file.indexOf(".java"));
            }
            @Override
            public String toFile(String classFileName) { return classFileName; }
        });
    }

    @Test
    void basic() {
        var ranges = coverage.ranges("my/package/AB.java");
        assertTrue(ranges.stream().filter(clr -> clr.first() == 1).findAny().get().coverage().covered() > 0);
    }
}
