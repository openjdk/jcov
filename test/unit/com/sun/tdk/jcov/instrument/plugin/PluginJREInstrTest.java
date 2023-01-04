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

import com.sun.tdk.jcov.JREInstr;
import com.sun.tdk.jcov.instrument.util.Util;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class PluginJREInstrTest {

    Path jre;

    @BeforeClass
    public void setup() throws IOException {
        String testJRE = System.getProperty("test.jre");
        if(testJRE == null) {
            testJRE = System.getProperty("java.home");
        }
        jre = Util.copyJRE(Paths.get(testJRE));
        System.out.println("JRE: " + testJRE);
    }

    @Test
    public void testJREInstr() {
        String runtime = Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                .peek(System.out::println)
                .filter(s -> s.endsWith("jcov_file_saver.jar")).findAny().get();
        String[] params = new String[] {
                "-implantrt", runtime,
                "-im", "java.base",
                "-im", "java.desktop",
                "-instr_plugin", JRETestPlugin.class.getName(),
                jre.toString()};
        System.out.println("Running JREInstr with " + Arrays.stream(params).collect(Collectors.joining(" ")));
        TestPlugin.clear();
        assertEquals(new JREInstr().run(params), 0);
        Stream.of(ServiceLoader.class, Component.class).forEach(cls -> {
            String r = cls.getName().replace('.', '/') + ".class";
            assertTrue(TestPlugin.getProcessed().contains(r));
        });
        assertTrue(TestPlugin.isCompleted());
    }

    @AfterClass
    public void tearDown() throws IOException {
        if(jre != null && Files.exists(jre)) Util.rmRF(jre);
    }

}
