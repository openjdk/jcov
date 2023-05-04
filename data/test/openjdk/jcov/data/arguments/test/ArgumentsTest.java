/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package openjdk.jcov.data.arguments.test;

import com.sun.tdk.jcov.instrument.InstrumentationParams;
import com.sun.tdk.jcov.instrument.plugin.Instrumentation;
import com.sun.tdk.jcov.instrument.plugin.PathDestination;
import com.sun.tdk.jcov.instrument.plugin.PathSource;
import openjdk.jcov.data.arguments.instrument.Plugin;
import openjdk.jcov.data.arguments.runtime.Collect;
import openjdk.jcov.data.arguments.runtime.Coverage;
import openjdk.jcov.data.lib.TestStatusListener;
import openjdk.jcov.data.lib.Util;
import openjdk.jcov.data.runtime.Serializer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.util.stream.Collectors.joining;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

@Listeners({openjdk.jcov.data.lib.TestStatusListener.class})
public class ArgumentsTest {
    Path test_dir;
    Path template;
    private Path data_dir;

    @BeforeClass
    public void clean() throws IOException {
        data_dir = Files.createTempDirectory("userdir");
        test_dir = data_dir.resolve("arguments_test");
        Util.rfrm(test_dir);
        template = test_dir.resolve("template.lst");
    }
    @Test
    public void instrument() throws Exception {
        Collect.clearData();
        Collect.outputFile(template);
        new Util(test_dir).copyBytecode(UserCode.class.getName());
        openjdk.jcov.data.instrument.Plugin plugin = new Plugin();
        new Instrumentation(plugin)
                .instrument(new PathSource(getClass().getClassLoader(), test_dir), new PathDestination(test_dir),
                        new InstrumentationParams());
        Collect.save();
        Coverage tmpl = Coverage.read(template);
        assertNotNull(tmpl.coverage().get(UserCode.class.getName().replace('.', '/')).
                get("method(IJFDZBLjava/lang/String;)V"));
    }

    @Test(dependsOnMethods = "instrument")
    public void run() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException, IOException, InstantiationException {
        Collect.serializer(Serializer.TO_STRING);
        new Util(test_dir).runClass(UserCode.class, new String[0]);
        Collect.save();
        Coverage coverage = Coverage.read(template);
        List<List<?>> calls = coverage.get(UserCode.class.getName().replace('.', '/'),
                "method(IJFDZBLjava/lang/String;)V");
        assertEquals(calls.size(), 2);
        assertEquals(calls.get(0).get(6), "6");
        assertEquals(calls.get(1).get(0), "7");
    }

    @AfterClass
    public void tearDown() throws IOException {
        if(TestStatusListener.status)
            Util.rfrm(data_dir);
    }
}
