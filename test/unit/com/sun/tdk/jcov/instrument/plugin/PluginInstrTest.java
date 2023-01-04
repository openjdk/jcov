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
package com.sun.tdk.jcov.instrument.plugin;

import com.sun.tdk.jcov.Instr;
import com.sun.tdk.jcov.instrument.util.Util;
import com.sun.tdk.jcov.instrument.instr.UserCode;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

public class PluginInstrTest {
    Path data_dir;
    Path test_dir;
    int method_slot = -1;
    @BeforeClass
    public void clean() throws IOException {
        System.setProperty("jcov.selftest", "true");
        data_dir = Files.createTempDirectory("instr_test");
        test_dir = data_dir.resolve("instr_test");
    }
    @BeforeMethod
    public void rm() throws IOException {
        Util.rmRF(data_dir);
    }
    @Test
    public void instrumentDir() throws IOException {
        List<String> params = new ArrayList<>();
        new Util(test_dir).copyBytecode(UserCode.class.getName(), PluginInstrTest.class.getName());
        params.add("-instr_plugin");
        params.add(TestPlugin.class.getName());
        params.add(test_dir.toString());
        TestPlugin.clear();
        new Instr().run(params.toArray(new String[0]));
        String resource = UserCode.class.getName().replace('.', '/') + ".class";
        assertTrue(TestPlugin.getProcessed().contains(resource));
        assertTrue(TestPlugin.isCompleted());
        byte[] orig = UserCode.class.getClassLoader().getResourceAsStream(resource).readAllBytes();
        byte[] newr = Files.newInputStream(test_dir.resolve(resource)).readAllBytes();
        assertEquals(orig.length, newr.length);
    }
    @AfterClass
    public void tearDown() throws IOException {
        Util.rmRF(data_dir);
    }
}
