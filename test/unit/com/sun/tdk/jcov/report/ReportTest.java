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
package com.sun.tdk.jcov.report;

import com.sun.tdk.jcov.Instr;
import com.sun.tdk.jcov.instrument.DataRoot;
import com.sun.tdk.jcov.instrument.InstrumentationOptions;
import com.sun.tdk.jcov.instrument.Util;
import org.testng.annotations.BeforeClass;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReportTest {
    protected Path test_dir;
    protected Path template;
    protected Path result;

    public void setup(Class runClass, String... copyClasses) throws Exception {
        System.setProperty("jcov.selftest", "true");

        Path data_dir = Paths.get(System.getProperty("user.dir"));

        //clear
        test_dir = data_dir.resolve("instr_test");
        System.out.println("test dir = " + test_dir);
        Util.rmRF(test_dir);

        //prepare bytecode
        List<Path> classFiles = new Util(test_dir).copyBytecode(copyClasses);

        //instrument
        template = test_dir.resolve("template.xml");
        List<String> params = new ArrayList<>();
        params.add("-t");
        params.add(template.toString());
        params.addAll(classFiles.stream().map(Path::toString).collect(Collectors.toSet()));
        new Instr().run(params.toArray(new String[0]));

        //run
        new Util(test_dir).runClass(runClass, new String[] {});

        //save coverage
        result = test_dir.resolve("result.xml");
        DataRoot.getInstance(0).write(result.toString(), InstrumentationOptions.MERGE.OVERWRITE);
    }
    public void tearDown() throws IOException {
        Util.rmRF(test_dir);
    }
}
