/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.jcov.instrument.tmplgen;

import com.sun.tdk.jcov.Instr;
import com.sun.tdk.jcov.TmplGen;
import com.sun.tdk.jcov.data.FileFormatException;
import com.sun.tdk.jcov.instrument.DataMethod;
import com.sun.tdk.jcov.instrument.DataPackage;
import com.sun.tdk.jcov.instrument.DataRoot;
import com.sun.tdk.jcov.instrument.util.Util;
import com.sun.tdk.jcov.instrument.instr.UserCode;
import com.sun.tdk.jcov.io.Reader;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

public class TmplGenTest {
    Path test_dir;
    Path test_zip;
    Path template;
    @BeforeClass
    public void clean() throws IOException {
        System.setProperty("jcov.selftest", "true");
        Path data_dir = Files.createTempDirectory("instr_test");
        test_dir = data_dir.resolve("instr_test");
        test_zip = data_dir.resolve("instr_test.jar");
        System.out.println("test dir = " + test_dir);
        template = test_dir.resolve("template.xml");
    }
    @BeforeMethod
    public void rm() throws IOException {
        Util.rmRF(test_dir);
    }
//    @Test
    public void instrumentClass() {
    }
    @Test
    public void instrumentDir() throws IOException, FileFormatException {
        List<String> params = new ArrayList<>();
        params.add("-t");
        params.add(template.toString());
        params.add("-i");
        params.add(UserCode.class.getName());
        new Util(test_dir).copyBytecode(UserCode.class.getName(), TmplGenTest.class.getName());
        int classSize = classSize(UserCode.class.getName());
        params.add(test_dir.toString());
        new TmplGen().run(params.toArray(new String[0]));
        testInstrumentation();
        assertEquals(classSize(UserCode.class.getName()), classSize);
    }

    private int classSize(String name) throws IOException {
        return Files.readAllBytes(test_dir.resolve(name.replace('.', File.separatorChar) + ".class")).length;
    }

    @Test
    public void instrumentJar() throws IOException, FileFormatException {
        Path classes = test_dir.resolve("classes");
        Files.createDirectories(classes);
        new Util(classes).copyBytecode(UserCode.class.getName(), TmplGenTest.class.getName());
        Util.jar(classes, test_zip, p -> true);
        Util.rmRF(classes);
        List<String> params = new ArrayList<>();
        params.add("-t");
        params.add(template.toString());
        params.add("-i");
        params.add(UserCode.class.getName());
        params.add(test_zip.toString());
        new Instr().run(params.toArray(new String[0]));
        Util.unjar(test_zip, classes);
        testInstrumentation();
    }
    private void testInstrumentation() throws FileFormatException {
        DataRoot data = Reader.readXML(template.toString());
        DataPackage dp =
                data.getPackages().stream()
                        .filter(p -> p.getName().equals("com/sun/tdk/jcov/instrument/instr")).findAny().get();
        DataMethod dm = dp
                        .getClasses().stream().filter(c -> c.getName().equals("UserCode")).findAny().get()
                        .getMethods().stream().filter(m -> m.getName().equals("main")).findAny().get();
        int method_slot = dm.getSlot();
        assertTrue(dm.getSlot() > 0);
        assertFalse(dp
                .getClasses().stream().filter(c -> c.getName().equals("InstrTest")).findAny().isPresent());
    }

    @AfterClass
    public void tearDown() throws IOException {
        Util.rmRF(test_dir);
        Util.rmRF(test_zip);
    }
}
