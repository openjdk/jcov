/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package openjdk.jcov.data.arguments;

import openjdk.jcov.data.arguments.runtime.Coverage;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class CoverageTest {
    public static final String CLASS1 = "class1";
    public static final String CLASS2 = "class2";
    public static final String METHOD11 = "method11(Ljava/lang/Object;Ljava/lang/Object;)";
    public static final String PARAM1 = "param1";
    public static final String PARAM2 = "param2";
    public static final String PARAM3 = "param3";
    public static final String PARAM4 = "param4";
    private Coverage cov = new Coverage();

    private void assertContent(Coverage cov) {
        assertEquals(cov.coverage().size(), 1);
        assertEquals(cov.coverage().keySet().iterator().next(), CLASS1);
        Map<String, List<List<?>>> class1Cov = cov.coverage().values().iterator().next();
        assertEquals(class1Cov.size(), 1);
        assertEquals(class1Cov.keySet().iterator().next(), METHOD11);
        List<List<?>> method11Cov = class1Cov.values().iterator().next();
        assertEquals(method11Cov.size(), 2);
        assertEquals(method11Cov.get(0).get(0), PARAM1);
        assertEquals(method11Cov.get(0).get(1), PARAM2);
        assertEquals(method11Cov.get(1).get(0), PARAM3);
        assertEquals(method11Cov.get(1).get(1), PARAM4);
    }

    @Test
    public void addThings() {
        cov.get(CLASS1, METHOD11)
                .add(List.of(PARAM1, PARAM2));
        cov.get(CLASS1, METHOD11)
                .add(List.of(PARAM3, PARAM4));
        assertContent(cov);
    }

    @Test(dependsOnMethods = "addThings")
    public void saveAndLoad() throws IOException {
        Path temp1 = Files.createTempFile("coverage1.", ".lst");
        Path temp2 = Files.createTempFile("coverage2.", ".lst");
        Path temp3 = Files.createTempFile("coverage2.", ".lst");
        Coverage.write(cov, temp1);
        Coverage loaded1 = Coverage.read(temp1);
        assertContent(loaded1);
        Coverage.write(loaded1, temp2);
        Coverage loaded2 = Coverage.read(temp2);
        assertContent(loaded2);
        Coverage.write(loaded2, temp3);
        List<String> lines1 = Files.readAllLines(temp1);
        List<String> lines2 = Files.readAllLines(temp2);
        List<String> lines3 = Files.readAllLines(temp3);
        assertEquals(lines1.toArray(), lines2.toArray());
        assertEquals(lines1.toArray(), lines3.toArray());
        Files.deleteIfExists(temp1);
        Files.deleteIfExists(temp2);
        Files.deleteIfExists(temp3);
    }

    @Test(dependsOnMethods = "saveAndLoad")
    public void add() {
        cov.add(CLASS1, METHOD11, List.of(PARAM1, PARAM2));
        assertContent(cov);
        Coverage newCov = new Coverage();
        newCov.add(CLASS1, METHOD11, List.of(PARAM1, PARAM2));
        newCov.add(CLASS1, METHOD11, List.of(PARAM3, PARAM4));
        assertContent(newCov);
        newCov.add(CLASS2, METHOD11, List.of(PARAM1));
        assertEquals(newCov.coverage().size(), 2);
        assertTrue(newCov.coverage().keySet().contains(CLASS2));
        Map<String, List<List<?>>> class1Cov = newCov.coverage().get(CLASS2);
        assertEquals(class1Cov.size(), 1);
        assertEquals(class1Cov.keySet().iterator().next(), METHOD11);
        List<List<?>> method11Cov = class1Cov.values().iterator().next();
        assertEquals(method11Cov.size(), 1);
        assertEquals(method11Cov.get(0).size(), 1);
        assertEquals(method11Cov.get(0).get(0), PARAM1);
    }
}
