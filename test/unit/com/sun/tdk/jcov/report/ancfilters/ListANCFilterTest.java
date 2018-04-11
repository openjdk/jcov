/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tdk.jcov.report.ancfilters;

import com.sun.tdk.jcov.instrument.DataClass;
import com.sun.tdk.jcov.instrument.DataMethodEntryOnly;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertFalse;

public class ListANCFilterTest {
    private String createListFile(String[] lines) throws IOException {
        Path file = Files.createTempFile("ListANCFilterTest", ".lst");
        BufferedWriter out = Files.newBufferedWriter(file, Charset.defaultCharset());
        for(String ln : lines) {
            out.write(ln);out.newLine();
        }
        out.close();
        file.toFile().deleteOnExit();
        return file.toAbsolutePath().toString();
    }

    @Test
    public void testNormal() throws IOException {
        ListANCFilter filter = new ListANCFilter();
        String[] data = {
                "#normal",
                //a method
                "java/lang/String#indexOf(I)I",
                //a constructor
                "java/lang/Math#<init>()V",
        };
        filter.setParameter(createListFile(data));
        assertEquals(filter.getAncReason(), "normal");
        DataClass stringDataClass = new DataClass(0,
                "java/lang/String", "java.base", 0, false);
        assertFalse(filter.accept(stringDataClass));
        assertTrue(filter.accept(stringDataClass,
                new DataMethodEntryOnly(stringDataClass, 0, "indexOf", "(I)I", "", new String[0], 0)));
        DataClass mathDataClass = new DataClass(2,
                "java/lang/Math", "java.base", 0, false);
        assertTrue(filter.accept(mathDataClass,
                new DataMethodEntryOnly(mathDataClass, 0, "<init>", "()V", "", new String[0], 0)));
    }

    @Test
    public void testNested() throws IOException {
        ListANCFilter filter = new ListANCFilter();
        String[] data = {
                "#nested",
                //a nested class
                "java/lang/System$LoggerFinder#checkPermission()Ljava/lang/Void;"
        };
        filter.setParameter(createListFile(data));
        assertEquals(filter.getAncReason(), "nested");
        DataClass systemDataClass = new DataClass(2,
                "java/lang/System", "java.base", 0, false);
        assertTrue(filter.accept(systemDataClass,
                new DataMethodEntryOnly(systemDataClass, 0, "$LoggerFinder.checkPermission",
                        "()Ljava/lang/Void;", "", new String[0], 0)));
    }

    @DataProvider(name="unreadable")
    public Object[][] unreadableLists() {
        return new Object[][] {
                {new String[] {"java/lang/String#indexOf(I)I"}},
                {new String[] {"data", "java/lang/String#indexOf(I)I"}},
                {new String[] {"#data", "java/lang/String/indexOf(I)I"}},
                {null}
        };
    }

    @Test(dataProvider = "unreadable", expectedExceptions = {IllegalStateException.class, IllegalArgumentException.class})
    public void testNotRead(String[] data) throws IllegalStateException, IOException {
        String file;
        if(data == null)
            file = null;
        else
            file = createListFile(data);
        new ListANCFilter().setParameter(file);
    }

    @DataProvider(name="readable")
    public Object[][] readableLists() {
        String indexOfLine = "java/lang/String#indexOf(I)I";
        String[] indexOfElements = {"java/lang/String", "indexOf", "(I)I"};
        String constructorLine = "java/lang/Math#<init>()V";
        String[] constructorElements = {"java/lang/Math", "<init>", "()V"};
        return new Object[][] {
                {new String[] {"#data0", indexOfLine}, "data0", new String[][] {indexOfElements}},
                {new String[] {"#data1", "", constructorLine}, "data1", new String[][] {constructorElements}},
                {new String[] {"#data2", indexOfLine, constructorLine, ""}, "data2", new String[][] {indexOfElements, constructorElements}},
                {new String[] {"#data3"}, "data3", new String[][] {}}
        };
    }

    @Test(dataProvider = "readable")
    public void testRead(String[] data, String reason, String[][] elements) throws IllegalStateException, IOException {
        ListANCFilter filter = new ListANCFilter();
        filter.setParameter(createListFile(data));
        assertEquals(filter.getAncReason(), reason);
        for(String[] el : elements) {
            DataClass dataClass = new DataClass(2,
                    el[0], "java.base", 0, false);
            assertTrue(filter.accept(dataClass,
                    new DataMethodEntryOnly(dataClass, 0, el[1], el[2], "", new String[0], 0)));        }
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testUninitiated() {
        new ListANCFilter().accept(new DataClass(0,
                "java/lang/String", "java.base", 0, false));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testReasonUninitiated() {
        new ListANCFilter().getAncReason();
    }

    @Test
    public void testGetFilterName() {
        assertEquals(new ListANCFilter().getFilterName(), "list");
    }
}
