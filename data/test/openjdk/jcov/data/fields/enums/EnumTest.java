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
package openjdk.jcov.data.fields.enums;

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
import openjdk.jcov.data.runtime.serialization.EnumDeserializer;
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
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Listeners({TestStatusListener.class})
public class EnumTest {
    Path test_dir;
    Path template;
    Path coverage;
    List<Path> classes;
    Util util;
    Path data_dir;

    @BeforeClass
    public void clean() throws IOException {
        data_dir = Files.createTempDirectory("userdir");
        test_dir = data_dir.resolve("enum_field_test");
        Util.rfrm(test_dir);
        template = test_dir.resolve("template.lst");
        coverage = test_dir.resolve("coverage.lst");
        util = new Util(test_dir);
        Runtime.init();
        Runtime.clearExitHook();
    }
    @Test
    public void template() throws IOException, InterruptedException, ClassNotFoundException,
            InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        Collect.data().coverage().clear();
        TemplateGenerator tg = new TemplateGenerator();
        String vmEnumName = "L" + UserCode.ENum.class.getName().replace(".", "/") + ";";
        String vmClassName = UserCode.class.getName().replace(".", "/");
        tg.setFieldFilter(fm -> vmEnumName.equals(fm.fieldType().stringValue()) &&
                vmClassName.equals(fm.parent().get().thisClass().name().stringValue()));
        classes = util.copyBytecode(UserCode.class.getName(), UserCode.ENum.class.getName());
        tg.generate(classes);
        Coverage.write(Collect.data(), template);
        Coverage tmpl = Coverage.read(template);
        String cn = UserCode.class.getName().replace('.', '/');
        assertTrue(tmpl.coverage().containsKey(cn));
        assertEquals(tmpl.coverage().get(cn).size(), 2);
    }
    @Test(dependsOnMethods = "template")
    public void instrument() throws Exception {
        Collect.data(Coverage.read(template));
        new Instrumentation(new Plugin())
                .instrument(new PathSource(test_dir), new PathDestination(test_dir), new InstrumentationParams());
    }

    @Test(dependsOnMethods = "instrument")
    public void run() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException, IOException, InstantiationException {
        Collect.data(Coverage.read(template));
        util.runClass(UserCode.class, new String[0]);
        Coverage.write(Collect.data(), coverage);
        Coverage loaded = Coverage.read(coverage, new EnumDeserializer(UserCode.ENum.class));
        String cn = UserCode.class.getName().replace('.', '/');
        Map<String, List> fields = loaded.coverage().get(cn);
        assertEquals(fields.size(), 2);
        assertEquals(fields.get("eNum").get(0), UserCode.ENum.TWO);
        assertEquals(fields.get("staticENum").get(0), UserCode.ENum.ONE);
    }

    @AfterClass
    public void tearDown() throws IOException {
        if(TestStatusListener.status)
            Util.rfrm(data_dir);
    }
}
