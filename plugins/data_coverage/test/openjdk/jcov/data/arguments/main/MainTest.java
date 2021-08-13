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
package openjdk.jcov.data.arguments.main;

import openjdk.jcov.data.Env;
import openjdk.jcov.data.Instrument;
import openjdk.jcov.data.arguments.instrument.MethodFilter;
import openjdk.jcov.data.arguments.instrument.Plugin;
import openjdk.jcov.data.arguments.runtime.Coverage;
import openjdk.jcov.data.arguments.runtime.Saver;
import openjdk.jcov.data.lib.Util;
import openjdk.jcov.data.serialization.EnumDeserializer;
import openjdk.jcov.data.serialization.EnumSerializer;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static openjdk.jcov.data.Instrument.JCOV_TEMPLATE;
import static openjdk.jcov.data.arguments.analysis.Reader.DESERIALIZER;
import static openjdk.jcov.data.arguments.instrument.Plugin.*;
import static openjdk.jcov.data.arguments.runtime.Saver.RESULT_FILE;
import static org.testng.Assert.assertEquals;

public class MainTest {
    private Path test_dir;
    private Path template;
    private Path coverage;
    private MethodFilter mainFilter;

    @BeforeClass
    public void clean() throws IOException {
        Path data_dir = Paths.get(System.getProperty("user.dir"));
        test_dir = data_dir.resolve("parameter_test");
        template = test_dir.resolve("template.lst");
        coverage = test_dir.resolve("coverage.lst");
        Files.deleteIfExists(template);
        mainFilter = new MainFilter();
        Env.properties(Map.of(
                TEMPLATE_FILE, template.toString(),
                JCOV_TEMPLATE, test_dir.resolve("template.xml").toString(),
                METHOD_FILTER, MainFilter.class.getName()));
    }
    private Coverage instrument(Class cls) throws IOException, InterruptedException {
        new Instrument().pluginClass(Plugin.class.getName())
                .instrument(new Util(test_dir).
                        copyBytecode(cls.getName()));
        return Coverage.readTemplate(template);
    }
    @Test
    public void instrumentStatic() throws IOException, InterruptedException {
        Coverage tmplt = instrument(UserCodeStatic.class);
        Map<String, List<List<?>>> userCode =  tmplt.coverage().get(UserCodeStatic.class.getName().replace('.', '/'));
        assertEquals(userCode.size(), 1);
        assertEquals(userCode.keySet().iterator().next(), "main([Ljava/lang/String;)V");
    }
    @Test
    public void instrument() throws IOException, InterruptedException {
        Coverage tmplt = instrument(UserCode.class);
        assertEquals(tmplt.coverage().size(), 0);
    }
    @Test(dependsOnMethods = "instrument")
    public void run() throws
            ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException,
            IOException, InstantiationException {
        Env.properties(Map.of(
                TEMPLATE_FILE, template.toString(),
                RESULT_FILE, coverage.toString(),
                SERIALIZER, StringArraySerializer.class.getName(),
                DESERIALIZER, StringArrayDeserializer.class.getName()));
        new Util(test_dir).runClass(UserCodeStatic.class, new String[] {"one", "two"}, new Saver());
        Coverage res = Coverage.read(coverage, Objects::toString);
        List<List<?>> method =
                res.get(UserCodeStatic.class.getName().replace('.', '/'),
                        "main([Ljava/lang/String;)V");
        assertEquals(method.size(), 1);
        assertEquals(method.get(0).size(), 2);
        assertEquals(method.get(0).get(0), "one");
        assertEquals(method.get(0).get(1), "two");
    }
}
