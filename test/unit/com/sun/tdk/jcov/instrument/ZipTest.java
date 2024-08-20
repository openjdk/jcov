/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tdk.jcov.Instr;
import com.sun.tdk.jcov.Merger;
import com.sun.tdk.jcov.data.FileFormatException;
import com.sun.tdk.jcov.instrument.DataRoot;
import com.sun.tdk.jcov.instrument.Util;
import com.sun.tdk.jcov.instrument.instr.UserCode;
import com.sun.tdk.jcov.io.Reader;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.testng.Assert.assertEquals;

public class ReadZipTest {
    Path test_dir;
    Path template;
    Path template_zip;
    int method_slot = -1;
    @BeforeClass
    public void clean() throws IOException {
        System.setProperty("jcov.selftest", "true");
        Path data_dir = Paths.get(System.getProperty("user.dir"));
        test_dir = data_dir.resolve("instr_test");
        System.out.println("test dir = " + test_dir);
        Util.rmRF(test_dir);
        template = test_dir.resolve("template.xml");
        template_zip = test_dir.resolve("template.xml.zip");
    }
    @Test
    public void instrument() throws IOException, InterruptedException, FileFormatException {
        List<String> params = new ArrayList<>();
        params.add("-t");
        params.add(template.toString());
        params.add(new Util(test_dir).copyBytecode(UserCode.class.getName()).get(0).toString());
        System.out.println("Running Instr with");
        params.forEach(System.out::println);
        new Instr().run(params.toArray(new String[0]));
        try (var out = new ZipOutputStream(Files.newOutputStream(template_zip))) {
            out.putNextEntry(new ZipEntry("template.xml"));
            out.write(Files.readAllBytes(template));
        }
        compare(Reader.readXML(template_zip.toString()), Reader.readXML(template.toString()));
    }

    @Test(dependsOnMethods = "instrument")
    void merge() throws FileFormatException {
        var template_merge = test_dir.resolve("template_merge.xml");
        List<String> params = new ArrayList<>();
        params.add("-o");
        params.add(template_merge.toString());
        params.add(template_zip.toString());
        params.add(template.toString());
        System.out.println("Running Merger with");
        params.forEach(System.out::println);
        new Merger().run(params.toArray(new String[0]));
        compare(Reader.readXML(template_merge.toString()), Reader.readXML(template.toString()));
    }

    private void compare(DataRoot one, DataRoot another) {
        var methods = one.getClasses().stream()
                .flatMap(c -> c.getMethods().stream().map(m -> c.getName() + "." + m.getName() + m.getVmSignature()))
                .sorted().collect(Collectors.toList());
        var otherMethods = another.getClasses().stream()
                .flatMap(c -> c.getMethods().stream().map(m -> c.getName() + "." + m.getName() + m.getVmSignature()))
                .sorted().collect(Collectors.toList());
        assertEquals(methods.size(), otherMethods.size());
        for (int i = 0; i < methods.size(); i++) {
            assertEquals(methods.get(i), otherMethods.get(i));
        }
    }

    @AfterClass
    public void tearDown() throws IOException {
        Util.rmRF(test_dir);
    }
}
