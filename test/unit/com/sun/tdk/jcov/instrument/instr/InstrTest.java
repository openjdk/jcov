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
package com.sun.tdk.jcov.instrument.instr;

import com.sun.tdk.jcov.Instr;
import com.sun.tdk.jcov.data.FileFormatException;
import com.sun.tdk.jcov.instrument.DataMethod;
import com.sun.tdk.jcov.instrument.DataRoot;
import com.sun.tdk.jcov.instrument.Util;
import com.sun.tdk.jcov.io.Reader;
import com.sun.tdk.jcov.runtime.Collect;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class InstrTest {
    Path implant_dir;
    Path implant_jar;
    Path test_dir;
    Path test_zip;
    Path template;
    int method_slot = -1;
    @BeforeClass
    public void clean() throws IOException {
        System.setProperty("jcov.selftest", "true");
        Path data_dir = Paths.get(System.getProperty("user.dir"));
        implant_dir = data_dir.resolve("instr_implant");
        implant_jar = data_dir.resolve("instr_implant.jar");
        test_dir = data_dir.resolve("instr_test");
        test_zip = data_dir.resolve("instr_test.jar");
        System.out.println("test dir = " + test_dir);
        template = test_dir.resolve("template.lst");
    }
    @BeforeMethod
    public void rm() throws IOException {
        Util.rmRF(test_dir);
    }
    //@Test
    public void instrumentClass() throws IOException, InterruptedException, FileFormatException,
            ClassNotFoundException, InvocationTargetException, NoSuchMethodException,
            IllegalAccessException, InstantiationException {
        List<String> params = new ArrayList<>();
        params.add("-t");
        params.add(template.toString());
        params.add(new Util(test_dir).copyBytecode(UserCode.class.getName()).get(0).toString());
        new Instr().run(params.toArray(new String[0]));
        testInstrumentation();
        run(test_dir);
    }
    @Test
    public void instrumentDir() throws IOException, InterruptedException, FileFormatException,
            ClassNotFoundException, InvocationTargetException, NoSuchMethodException,
            IllegalAccessException, InstantiationException {
        List<String> params = new ArrayList<>();
        params.add("-t");
        params.add(template.toString());
        params.add("-i");
        params.add("UserCode");
        new Util(test_dir).copyBytecode(UserCode.class.getName(), InstrTest.class.getName());
        params.add(test_dir.toString());
        new Instr().run(params.toArray(new String[0]));
        testInstrumentation();
        run(test_dir);
    }
    @Test
    public void instrumentJar() throws IOException, InterruptedException, FileFormatException,
            ClassNotFoundException, InvocationTargetException, NoSuchMethodException,
            IllegalAccessException, InstantiationException {
        Path classes = test_dir.resolve("classes");
        Files.createDirectories(classes);
        new Util(classes).copyBytecode(UserCode.class.getName(), InstrTest.class.getName());
        Util.jar(classes, test_zip, p -> true);
        Util.rmRF(classes);
        List<String> params = new ArrayList<>();
        params.add("-t");
        params.add(template.toString());
        params.add("-i");
        params.add("UserCode");
        params.add(test_zip.toString());
        new Instr().run(params.toArray(new String[0]));
        Util.unjar(test_zip, classes);
        testInstrumentation();
        run(classes);
    }
    @Test
    public void implantTest() throws IOException, FileFormatException, ClassNotFoundException,
            InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        new Util(test_dir).copyBytecode(UserCode.class.getName());
        new Util(implant_dir).copyBytecode(InstrTest.class.getName());
        Files.write(implant_dir.resolve("some.properties"), "some.property=value\n".getBytes());
        Util.jar(implant_dir, implant_jar, p -> true);
        List<String> params = new ArrayList<>();
        params.add("-t");
        params.add(template.toString());
        params.add("-implantrt");
        params.add(implant_jar.toString());
        params.add("-i");
        params.add("UserCode");
        params.add(test_dir.toString());
        new Instr().run(params.toArray(new String[0]));
        testInstrumentation();
        assertTrue(Files.exists(test_dir.
                resolve(InstrTest.class.getName().replace('.', File.separatorChar) + ".class")));
        assertTrue(Files.exists(test_dir.resolve("some.properties")));
        assertFalse(Files.exists(test_dir.resolve("META-INF").resolve("MANIFEST.MF")));
        run(test_dir);
    }
    private void testInstrumentation() throws FileFormatException {
        DataRoot data = Reader.readXML(template.toString());
        DataMethod dm =
                data.getPackages().stream().filter(p -> p.getName().equals("com/sun/tdk/jcov/instrument/instr")).findAny().get()
                        .getClasses().stream().filter(c -> c.getName().equals("UserCode")).findAny().get()
                        .getMethods().stream().filter(m -> m.getName().equals("main")).findAny().get();
        method_slot = dm.getSlot();
        assertTrue(method_slot > 0);
    }

    public void run(Path test_dir) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException, IOException, InstantiationException {
        new Util(test_dir).runClass(UserCode.class, new String[] {"+"});
        assertTrue(Collect.wasHit(method_slot));
    }

    @AfterClass
    public void tearDown() throws IOException {
        Util.rmRF(test_dir);
        Util.rmRF(test_zip);
        Util.rmRF(implant_dir);
        Util.rmRF(implant_jar);
    }
}
