/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
package openjdk.jcov.data.arguments.othervm;

import openjdk.jcov.data.arguments.instrument.Plugin;
import openjdk.jcov.data.arguments.runtime.Coverage;
import openjdk.jcov.data.arguments.test.UserCode;
import openjdk.jcov.data.instrument.Instrument;
import openjdk.jcov.data.lib.TestStatusListener;
import openjdk.jcov.data.lib.Util;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static openjdk.jcov.data.instrument.Instrument.PLUGIN_CLASS;
import static openjdk.jcov.data.runtime.CoverageData.COVERAGE_IN;
import static openjdk.jcov.data.runtime.CoverageData.COVERAGE_OUT;
import static org.testng.Assert.assertEquals;

public class OtherVMTest {
    private Path test_dir;
    private Path template;
    private Path coverage;
    private Path data_dir;

    @BeforeClass
    public void clean() throws IOException {
        data_dir = Files.createTempDirectory("other-vm-test");
        Files.createDirectories(data_dir);
        test_dir = data_dir.resolve("othervm_args_test");
        Util.rfrm(test_dir);
        template = test_dir.resolve("template.lst");
        coverage = test_dir.resolve("coverage.lst");
        new Util(test_dir).copyBytecode(UserCode.class.getName());
    }
    @Test
    public void instrument() throws IOException, InterruptedException {
        assertEquals(Util.runClassOnM(List.of("-D" + PLUGIN_CLASS + "=" + Plugin.class.getName(), "-D" + COVERAGE_OUT + "=" + template),
                Instrument.class,
                List.of(test_dir.toString(), test_dir.toString())), 0);
        Coverage data = Coverage.read(template);
        assertEquals(data.coverage().get(UserCode.class.getName().replace(".", "/")).size(), 3);
    }
    @Test(dependsOnMethods = "instrument")
    public void run() throws IOException, InterruptedException {
        assertEquals(Util.runAClassOnCP(List.of("-D" + COVERAGE_IN + "=" + template,
                "-D" + COVERAGE_OUT + "=" + coverage), List.of(test_dir), UserCode.class.getName(),
                List.of()), 0);
        Coverage data = Coverage.read(coverage);
        List<List<?>> values = data.get(UserCode.class.getName().replace(".", "/"),
                "method(IJFDZBLjava/lang/String;)V");
        assertEquals(values.size(), 2);
        assertEquals(values.get(0).get(0), "0");
    }
    @AfterClass
    public void tearDown() throws IOException {
        Util.rfrm(data_dir);
    }
}
