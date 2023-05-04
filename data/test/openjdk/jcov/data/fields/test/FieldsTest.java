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
package openjdk.jcov.data.fields.test;

import com.sun.tdk.jcov.instrument.InstrumentationParams;
import com.sun.tdk.jcov.instrument.plugin.Instrumentation;
import com.sun.tdk.jcov.instrument.plugin.PathDestination;
import com.sun.tdk.jcov.instrument.plugin.PathSource;
import openjdk.jcov.data.fields.instrument.Plugin;
import openjdk.jcov.data.fields.instrument.TemplateGenerator;
import openjdk.jcov.data.fields.runtime.Collect;
import openjdk.jcov.data.fields.runtime.Coverage;
import openjdk.jcov.data.fields.runtime.Runtime;
import openjdk.jcov.data.lib.TestStatusListener;
import openjdk.jcov.data.lib.Util;
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

import static org.testng.Assert.*;

@Listeners({TestStatusListener.class})
public class FieldsTest {
    Path test_dir;
    Path template;
    private List<Path> classes;
    Path data_dir;

    @BeforeClass
    public void clean() throws IOException {
        data_dir = Files.createTempDirectory("userdir");
        test_dir = data_dir.resolve("fields_test");
        Util.rfrm(test_dir);
        template = test_dir.resolve("template.lst");
        classes = new Util(test_dir).copyBytecode(UserCode.class.getName());
        Runtime.init();
        Runtime.clearExitHook();
    }
    @Test
    public void template() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException, IOException {
        new TemplateGenerator().generate(classes);
        Coverage.write(Collect.data(), template);
    }

    @Test(dependsOnMethods = "template")
    public void instrument() throws Exception {
        Collect.data(Coverage.read(template));
        new Instrumentation(new Plugin())
                .instrument(new PathSource(getClass().getClassLoader(), test_dir), new PathDestination(test_dir),
                        new InstrumentationParams());
        Coverage.write(Collect.data(), template);
        Coverage tmpl = Coverage.read(template);
        String cn = UserCode.class.getName().replace('.', '/');
        assertTrue(tmpl.coverage().containsKey(cn));
        assertTrue(tmpl.coverage().get(cn).containsKey("i"));
        assertTrue(tmpl.coverage().get(cn).get("i").isEmpty());
    }

    @Test(dependsOnMethods = "instrument")
    public void run() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException, IOException, InstantiationException {
        new Util(test_dir).runClass(UserCode.class, new String[0]);
        Coverage.write(Collect.data(), template);
        Coverage coverage = Coverage.read(template);
        String cn = UserCode.class.getName().replace('.', '/');
        List values = coverage.get(cn, "i");
        assertEquals(values.size(), 2);
        assertTrue(values.contains("0"));
        assertTrue(values.contains("7"));
        values = coverage.get(cn, "j");
        assertEquals(values.size(), 2);
        assertTrue(values.contains("1"));
        assertTrue(values.contains("8"));
        values = coverage.get(cn, "f");
        assertEquals(values.size(), 2);
        assertTrue(values.contains("2.0"));
        assertTrue(values.contains("9.0"));
        values = coverage.get(cn, "d");
        assertEquals(values.size(), 2);
        assertTrue(values.contains("3.0"));
        assertTrue(values.contains("10.0"));
        values = coverage.get(cn, "b");
        assertEquals(values.size(), 2);
        assertTrue(values.contains("5"));
        assertTrue(values.contains("12"));
        values = coverage.get(cn, "z");
        assertEquals(values.size(), 2);
        assertTrue(values.contains("true"));
        assertTrue(values.contains("false"));
        values = coverage.get(cn, "s");
        assertEquals(values.size(), 2);
        assertTrue(values.contains("6"));
        assertTrue(values.contains("13"));
    }
    @AfterClass
    public void tearDown() throws IOException {
        if(TestStatusListener.status)
            Util.rfrm(data_dir);
    }
}
