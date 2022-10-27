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
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertTrue;

public class InstrTest {
    Path test_dir;
    Path template;
    int method_slot = -1;
    @BeforeClass
    public void clean() throws IOException {
        System.setProperty("jcov.selftest", "true");
        Path data_dir = Paths.get(System.getProperty("user.dir"));
        test_dir = data_dir.resolve("instr_test");
        System.out.println("test dir = " + test_dir);
        Util.rmRF(test_dir);
        template = test_dir.resolve("template.lst");
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
        run();
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
        run();
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

//    @Test(dependsOnMethods = "instrumentDir")
    public void run() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException, IOException, InstantiationException {
        new Util(test_dir).runClass(UserCode.class, new String[] {"+"});
        assertTrue(Collect.wasHit(method_slot));
    }

    @AfterClass
    public void tearDown() throws IOException {
        Util.rmRF(test_dir);
    }
}
