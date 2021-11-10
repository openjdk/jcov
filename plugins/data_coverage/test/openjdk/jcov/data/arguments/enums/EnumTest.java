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
package openjdk.jcov.data.arguments.enums;

import openjdk.jcov.data.Instrument;
import openjdk.jcov.data.arguments.runtime.Collect;
import openjdk.jcov.data.arguments.runtime.Coverage;
import openjdk.jcov.data.arguments.instrument.Plugin;
import openjdk.jcov.data.arguments.runtime.Saver;
import openjdk.jcov.data.Env;
import openjdk.jcov.data.lib.TestStatusListener;
import openjdk.jcov.data.lib.Util;
import openjdk.jcov.data.serialization.EnumDeserializer;
import openjdk.jcov.data.serialization.EnumSerializer;
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

import static openjdk.jcov.data.Env.JCOV_DATA_ENV_PREFIX;
import static openjdk.jcov.data.Instrument.JCOV_TEMPLATE;
import static openjdk.jcov.data.arguments.instrument.Plugin.*;
import static openjdk.jcov.data.arguments.runtime.Collect.SERIALIZER;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

@Listeners({openjdk.jcov.data.lib.TestStatusListener.class})
public class EnumTest {
    private Path test_dir;
    private Path template;
    private EnumDeserializer deserializer = new EnumDeserializer(UserCode.ENum.class);

    @BeforeClass
    public void clean() throws IOException {
        Path data_dir = Paths.get(System.getProperty("user.dir"));
        test_dir = data_dir.resolve("enum_test");
        Util.rfrm(test_dir);
        Files.createDirectories(test_dir);
        template = test_dir.resolve("template.lst");
    }
    @Test
    public void serialization() {
        assertSame(UserCode.ENum.THREE,
                new EnumDeserializer(UserCode.ENum.class).apply(new EnumSerializer().apply(UserCode.ENum.THREE)));
    }
    @Test
    public void instrument() throws IOException, InterruptedException {
        Env.clear(JCOV_DATA_ENV_PREFIX);
        Env.setSystemProperties(Map.of(
                Collect.COVERAGE_OUT, template.toString(),
                JCOV_TEMPLATE, test_dir.resolve("template.xml").toString(),
                METHOD_FILTER, EnumMethodsFilter.class.getName()));
        new Instrument().pluginClass(Plugin.class.getName())
                .instrument(new Util(test_dir).
                        copyBytecode(openjdk.jcov.data.arguments.enums.UserCode.class.getName()));
        Coverage tmplt = Coverage.read(template);
        assertEquals(tmplt.coverage().get(UserCode.class.getName().replace('.', '/')).size(), 2);
    }

    @Test(dependsOnMethods = "instrument")
    public void run() throws
            ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException,
            IOException, InstantiationException {
        Env.clear(JCOV_DATA_ENV_PREFIX);
        Env.setSystemProperties(Map.of(
                Collect.COVERAGE_IN, template.toString(),
                Collect.COVERAGE_OUT, template.toString(),
                SERIALIZER, EnumSerializer.class.getName()));
        new Util(test_dir).runClass(UserCode.class, new String[0], new Saver());
        Coverage res = Coverage.read(template, deserializer);
        List<List<?>> method =
                res.get(UserCode.class.getName().replace('.', '/'),
                        "method(Lopenjdk/jcov/data/arguments/enums/UserCode$ENum;)V");
        assertEquals(method.size(), 1);
        assertEquals(method.get(0).size(), 1);
        assertEquals(method.get(0).get(0), UserCode.ENum.ONE);
        List<List<?>> staticMethod =
                res.get(UserCode.class.getName().replace('.', '/'),
                        "staticMethod(Lopenjdk/jcov/data/arguments/enums/UserCode$ENum;)V");
        assertEquals(staticMethod.size(), 1);
        assertEquals(staticMethod.get(0).size(), 1);
        assertEquals(staticMethod.get(0).get(0), UserCode.ENum.TWO);
    }
    @AfterClass
    public void tearDown() throws IOException {
        if(TestStatusListener.status) {
            Util.rfrm(test_dir);
        }
    }
}
