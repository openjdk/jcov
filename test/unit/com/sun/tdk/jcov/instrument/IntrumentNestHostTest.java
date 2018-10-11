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

package com.sun.tdk.jcov.instrument;

import java.io.OutputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import com.sun.tdk.jcov.Instr;
import com.sun.tdk.jcov.io.Reader;
import com.sun.tdk.jcov.runtime.Collect;
import org.testng.Assert;
import org.testng.annotations.Test;

public class IntrumentNestHostTest {
    @Test
    public void testNestHostMembers() throws Exception {
        Path temp = Files.createTempDirectory("jcov");
        Path template = temp.resolve("template.xml");
        Path classes = temp.resolve("classes");
        Files.createDirectories(classes);
        try (OutputStream out = Files.newOutputStream(classes.resolve("Simple.class"))) {
            out.write(SIMPLE_CLASS);
        }
        try (OutputStream out = Files.newOutputStream(classes.resolve("Simple$I.class"))) {
            out.write(SIMPLE$I_CLASS);
        }
        Path outDir = temp.resolve("out");
        Files.createDirectories(outDir);
        Instr instr = new Instr();
        instr.setTemplate(template.toFile().getAbsolutePath());
        instr.instrumentFile(classes.toFile(), outDir.toFile(), null);
        instr.finishWork();
        Assert.assertNotEquals(Files.size(outDir.resolve("Simple.class")),
                               Files.size(classes.resolve("Simple.class")),
                               "File size should differ.");
        Assert.assertNotEquals(Files.size(outDir.resolve("Simple$I.class")),
                               Files.size(classes.resolve("Simple$I.class")),
                               "File size should differ.");
        Collect.enableCounts(); //reset
        if (new BigDecimal(System.getProperty("java.class.version")).compareTo(new BigDecimal("55.0")) >= 0) {
            //run the code, and check coverage outcome:
            ClassLoader cl = new URLClassLoader(new URL[] {outDir.toUri().toURL()});
            Class<?> simple = Class.forName("Simple", false, cl);
            Method run = simple.getMethod("run");
            run.invoke(null);
            DataRoot root = Reader.readXML(template.toFile().getAbsolutePath(), true, null);
            int blocks = 0;
            for (DataClass dc : root.getClasses()) {
                for (DataMethod dm : dc.getMethods()) {
                    if ("<init>".equals(dm.name))
                        continue;
                    for (DataBlock db : dm.getBlocks()) {
                        Assert.assertEquals(Collect.countFor(db.slot), 1);
                        blocks++;
                    }
                }
            }
            Assert.assertEquals(blocks, 3);
        } else {
            System.err.println("Warning: skipping run of the test sample, as the runtime JDK cannot handle classfiles version 55.");
        }
    }

    //classfiles based on:
    //public class Simple {
    //    public static void run() {
    //        I.run();
    //    }
    //    private static class I {
    //        public static void run() {
    //            d();
    //        }
    //        private static void d() {}
    //    }
    //}
    private static final byte[] SIMPLE_CLASS = new byte[] {
        (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x37, (byte) 0x00, (byte) 0x15, (byte) 0x0A, (byte) 0x00, (byte) 0x04, (byte) 0x00,
        (byte) 0x10, (byte) 0x0A, (byte) 0x00, (byte) 0x05, (byte) 0x00, (byte) 0x11, (byte) 0x07,
        (byte) 0x00, (byte) 0x12, (byte) 0x07, (byte) 0x00, (byte) 0x13, (byte) 0x07, (byte) 0x00,
        (byte) 0x14, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x49, (byte) 0x01, (byte) 0x00,
        (byte) 0x0C, (byte) 0x49, (byte) 0x6E, (byte) 0x6E, (byte) 0x65, (byte) 0x72, (byte) 0x43,
        (byte) 0x6C, (byte) 0x61, (byte) 0x73, (byte) 0x73, (byte) 0x65, (byte) 0x73, (byte) 0x01,
        (byte) 0x00, (byte) 0x06, (byte) 0x3C, (byte) 0x69, (byte) 0x6E, (byte) 0x69, (byte) 0x74,
        (byte) 0x3E, (byte) 0x01, (byte) 0x00, (byte) 0x03, (byte) 0x28, (byte) 0x29, (byte) 0x56,
        (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x43, (byte) 0x6F, (byte) 0x64, (byte) 0x65,
        (byte) 0x01, (byte) 0x00, (byte) 0x0F, (byte) 0x4C, (byte) 0x69, (byte) 0x6E, (byte) 0x65,
        (byte) 0x4E, (byte) 0x75, (byte) 0x6D, (byte) 0x62, (byte) 0x65, (byte) 0x72, (byte) 0x54,
        (byte) 0x61, (byte) 0x62, (byte) 0x6C, (byte) 0x65, (byte) 0x01, (byte) 0x00, (byte) 0x03,
        (byte) 0x72, (byte) 0x75, (byte) 0x6E, (byte) 0x01, (byte) 0x00, (byte) 0x0A, (byte) 0x53,
        (byte) 0x6F, (byte) 0x75, (byte) 0x72, (byte) 0x63, (byte) 0x65, (byte) 0x46, (byte) 0x69,
        (byte) 0x6C, (byte) 0x65, (byte) 0x01, (byte) 0x00, (byte) 0x0B, (byte) 0x53, (byte) 0x69,
        (byte) 0x6D, (byte) 0x70, (byte) 0x6C, (byte) 0x65, (byte) 0x2E, (byte) 0x6A, (byte) 0x61,
        (byte) 0x76, (byte) 0x61, (byte) 0x01, (byte) 0x00, (byte) 0x0B, (byte) 0x4E, (byte) 0x65,
        (byte) 0x73, (byte) 0x74, (byte) 0x4D, (byte) 0x65, (byte) 0x6D, (byte) 0x62, (byte) 0x65,
        (byte) 0x72, (byte) 0x73, (byte) 0x0C, (byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x09,
        (byte) 0x0C, (byte) 0x00, (byte) 0x0C, (byte) 0x00, (byte) 0x09, (byte) 0x01, (byte) 0x00,
        (byte) 0x06, (byte) 0x53, (byte) 0x69, (byte) 0x6D, (byte) 0x70, (byte) 0x6C, (byte) 0x65,
        (byte) 0x01, (byte) 0x00, (byte) 0x10, (byte) 0x6A, (byte) 0x61, (byte) 0x76, (byte) 0x61,
        (byte) 0x2F, (byte) 0x6C, (byte) 0x61, (byte) 0x6E, (byte) 0x67, (byte) 0x2F, (byte) 0x4F,
        (byte) 0x62, (byte) 0x6A, (byte) 0x65, (byte) 0x63, (byte) 0x74, (byte) 0x01, (byte) 0x00,
        (byte) 0x08, (byte) 0x53, (byte) 0x69, (byte) 0x6D, (byte) 0x70, (byte) 0x6C, (byte) 0x65,
        (byte) 0x24, (byte) 0x49, (byte) 0x00, (byte) 0x21, (byte) 0x00, (byte) 0x03, (byte) 0x00,
        (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02,
        (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x09, (byte) 0x00,
        (byte) 0x01, (byte) 0x00, (byte) 0x0A, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x1D,
        (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x05, (byte) 0x2A, (byte) 0xB7, (byte) 0x00, (byte) 0x01, (byte) 0xB1, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x0B, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x01, (byte) 0x00, (byte) 0x09, (byte) 0x00, (byte) 0x0C, (byte) 0x00, (byte) 0x09,
        (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x0A, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x20, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x04, (byte) 0xB8, (byte) 0x00, (byte) 0x02, (byte) 0xB1, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x0B, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x0A, (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x03, (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x03,
        (byte) 0x00, (byte) 0x0D, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x00,
        (byte) 0x0E, (byte) 0x00, (byte) 0x0F, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04,
        (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x05, (byte) 0x00, (byte) 0x07, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x0A, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x05,
        (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x0A
    };

    private static final byte[] SIMPLE$I_CLASS = new byte[] {
        (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x37, (byte) 0x00, (byte) 0x16, (byte) 0x0A, (byte) 0x00, (byte) 0x04, (byte) 0x00,
        (byte) 0x0F, (byte) 0x0A, (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x10, (byte) 0x07,
        (byte) 0x00, (byte) 0x11, (byte) 0x07, (byte) 0x00, (byte) 0x14, (byte) 0x01, (byte) 0x00,
        (byte) 0x06, (byte) 0x3C, (byte) 0x69, (byte) 0x6E, (byte) 0x69, (byte) 0x74, (byte) 0x3E,
        (byte) 0x01, (byte) 0x00, (byte) 0x03, (byte) 0x28, (byte) 0x29, (byte) 0x56, (byte) 0x01,
        (byte) 0x00, (byte) 0x04, (byte) 0x43, (byte) 0x6F, (byte) 0x64, (byte) 0x65, (byte) 0x01,
        (byte) 0x00, (byte) 0x0F, (byte) 0x4C, (byte) 0x69, (byte) 0x6E, (byte) 0x65, (byte) 0x4E,
        (byte) 0x75, (byte) 0x6D, (byte) 0x62, (byte) 0x65, (byte) 0x72, (byte) 0x54, (byte) 0x61,
        (byte) 0x62, (byte) 0x6C, (byte) 0x65, (byte) 0x01, (byte) 0x00, (byte) 0x03, (byte) 0x72,
        (byte) 0x75, (byte) 0x6E, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x64, (byte) 0x01,
        (byte) 0x00, (byte) 0x0A, (byte) 0x53, (byte) 0x6F, (byte) 0x75, (byte) 0x72, (byte) 0x63,
        (byte) 0x65, (byte) 0x46, (byte) 0x69, (byte) 0x6C, (byte) 0x65, (byte) 0x01, (byte) 0x00,
        (byte) 0x0B, (byte) 0x53, (byte) 0x69, (byte) 0x6D, (byte) 0x70, (byte) 0x6C, (byte) 0x65,
        (byte) 0x2E, (byte) 0x6A, (byte) 0x61, (byte) 0x76, (byte) 0x61, (byte) 0x01, (byte) 0x00,
        (byte) 0x08, (byte) 0x4E, (byte) 0x65, (byte) 0x73, (byte) 0x74, (byte) 0x48, (byte) 0x6F,
        (byte) 0x73, (byte) 0x74, (byte) 0x07, (byte) 0x00, (byte) 0x15, (byte) 0x0C, (byte) 0x00,
        (byte) 0x05, (byte) 0x00, (byte) 0x06, (byte) 0x0C, (byte) 0x00, (byte) 0x0A, (byte) 0x00,
        (byte) 0x06, (byte) 0x01, (byte) 0x00, (byte) 0x08, (byte) 0x53, (byte) 0x69, (byte) 0x6D,
        (byte) 0x70, (byte) 0x6C, (byte) 0x65, (byte) 0x24, (byte) 0x49, (byte) 0x01, (byte) 0x00,
        (byte) 0x01, (byte) 0x49, (byte) 0x01, (byte) 0x00, (byte) 0x0C, (byte) 0x49, (byte) 0x6E,
        (byte) 0x6E, (byte) 0x65, (byte) 0x72, (byte) 0x43, (byte) 0x6C, (byte) 0x61, (byte) 0x73,
        (byte) 0x73, (byte) 0x65, (byte) 0x73, (byte) 0x01, (byte) 0x00, (byte) 0x10, (byte) 0x6A,
        (byte) 0x61, (byte) 0x76, (byte) 0x61, (byte) 0x2F, (byte) 0x6C, (byte) 0x61, (byte) 0x6E,
        (byte) 0x67, (byte) 0x2F, (byte) 0x4F, (byte) 0x62, (byte) 0x6A, (byte) 0x65, (byte) 0x63,
        (byte) 0x74, (byte) 0x01, (byte) 0x00, (byte) 0x06, (byte) 0x53, (byte) 0x69, (byte) 0x6D,
        (byte) 0x70, (byte) 0x6C, (byte) 0x65, (byte) 0x00, (byte) 0x20, (byte) 0x00, (byte) 0x03,
        (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x03, (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x05, (byte) 0x00, (byte) 0x06,
        (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x1D, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x05, (byte) 0x2A, (byte) 0xB7, (byte) 0x00, (byte) 0x01, (byte) 0xB1,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x08, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x05, (byte) 0x00, (byte) 0x09, (byte) 0x00, (byte) 0x09, (byte) 0x00,
        (byte) 0x06, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x20, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0xB8, (byte) 0x00, (byte) 0x02, (byte) 0xB1,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x08, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x0A, (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x08, (byte) 0x00,
        (byte) 0x0A, (byte) 0x00, (byte) 0x0A, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x01,
        (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x19, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0xB1, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x08,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x01, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x09, (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x0B,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x0C, (byte) 0x00,
        (byte) 0x0D, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x0E,
        (byte) 0x00, (byte) 0x13, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0A, (byte) 0x00,
        (byte) 0x01, (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x0E, (byte) 0x00, (byte) 0x12,
        (byte) 0x00, (byte) 0x0A
    };
}
