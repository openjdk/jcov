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
import com.sun.tdk.jcov.instrument.Util;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class RecordContainerTest extends ReportTest {
    @BeforeClass
    void setup() throws Exception {
        String[] copyClasses = {RecordContainer.class.getName(), RecordContainer.class.getName() + "$Point"};
        setup(RecordContainer.class, copyClasses);
        //prepare original bytecode
        new Util(test_dir).copyBytecode(copyClasses);
    }

    @Test
    void javapReport() throws IOException {
        Path report = test_dir.resolve("report.javap");
        List<String> params = new ArrayList<>();
        params.add("-javap");
        params.add(test_dir.toString());
        params.add("-o");
        params.add(report.toString());
        params.add(result.toString());
        new RepGen().run(params.toArray(new String[0]));
        assertTrue(Files.isDirectory(report));
        Path classHtml = report.resolve(RecordContainer.class.getName().replace('.', '/') +
                "$Point.html");
        assertFalse(Files.readAllLines(classHtml).stream().anyMatch(l -> {
            if (l.matches(".*<b>-\\d*</b>%\\(-\\d*/\\d*\\).*")) {
                System.err.println("Found some negative coverage:");
                System.err.println(l);
                return true;
            } else return false;
        }));
    }

    @Test
    void plainReport() throws IOException {
        Path report = test_dir.resolve("report.plain");
        List<String> params = new ArrayList<>();
        params.add("-o");
        params.add(report.toString());
        params.add(result.toString());
        new RepGen().run(params.toArray(new String[0]));
        assertTrue(Files.isDirectory(report));
        Path classHtml = report.resolve(RecordContainer.class.getName().replace('.', '/') +
                ".html");
        assertFalse(Files.readAllLines(classHtml).stream().anyMatch(l -> {
            if (l.matches(".*<b>-\\d*</b>%\\(-\\d*/\\d*\\).*")) {
                System.err.println("Found some negative coverage:");
                System.err.println(l);
                return true;
            } else return false;
        }));
    }
}
