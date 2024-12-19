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

import com.sun.tdk.jcov.RepGen;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertTrue;

public class BasicReportTest extends ReportTest {

    @BeforeClass
    public void setup() throws Exception {
        setup(BasicUserCode.class, BasicUserCode.class.getName());
    }

    @Test
    void textReport() throws IOException {
        Path report = test_dir.resolve("report.txt");
        List<String> params = new ArrayList<>();
        params.add("-format");
        params.add("text");
        params.add("-o");
        params.add(report.toString());
        params.add(result.toString());
        new RepGen().run(params.toArray(new String[0]));
        assertTrue(Files.isRegularFile(report));
        assertTrue(Files.readAllLines(report).contains(
                "MTH+: main([Ljava/lang/String;)V hits: 1 blocks:  75% (3/4); branches:  50% (1/2); lines:  75% (3/4);"
        ));
    }

    @Test
    void htmlReport() throws IOException {
        Path report = test_dir.resolve("report.html");
        List<String> params = new ArrayList<>();
        params.add("-o");
        params.add(report.toString());
        params.add(result.toString());
        new RepGen().run(params.toArray(new String[0]));
        assertTrue(Files.isDirectory(report));
        Path classHtml = report.resolve(BasicUserCode.class.getName().replace('.', '/') + ".html");
        assertTrue(Files.readAllLines(classHtml).stream().anyMatch(l -> l.contains("<b>60</b>%(3/5)")));
    }

    @AfterClass
    public void tearDown() throws IOException {
        super.tearDown();
    }
}
