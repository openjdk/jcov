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

import com.sun.tdk.jcov.instrument.InstrumentationParams;
import com.sun.tdk.jcov.instrument.plugin.FilteringPlugin;
import com.sun.tdk.jcov.instrument.plugin.Instrumentation;
import com.sun.tdk.jcov.instrument.plugin.PathDestination;
import com.sun.tdk.jcov.instrument.plugin.PathSource;
import openjdk.jcov.data.arguments.instrument.Plugin;
import openjdk.jcov.data.arguments.runtime.Collect;
import openjdk.jcov.data.arguments.runtime.Coverage;
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
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static org.testng.Assert.assertEquals;

@Listeners({TestStatusListener.class})
public class MainTest {
    private Path test_dir;
    private Path coverage;
    private Path data_dir;

    @BeforeClass
    public void cleanClass() throws IOException {
        data_dir = Files.createTempDirectory("userdir");
        test_dir = data_dir.resolve("main_test");
        coverage = test_dir.resolve("coverage.lst");
        Util.rfrm(test_dir);
        Files.createDirectories(test_dir);
    }
    private Coverage instrument(Class cls, Function<com.sun.tdk.jcov.instrument.InstrumentationPlugin,
            com.sun.tdk.jcov.instrument.InstrumentationPlugin> transform) throws Exception {
        new Util(test_dir).copyBytecode(cls.getName());
        Plugin plugin = new Plugin();
        plugin.methodFilter(new MainFilter());
        new Instrumentation(plugin).instrument(
                new PathSource(getClass().getClassLoader(), test_dir),
                new PathDestination(test_dir),
                new InstrumentationParams());
        Instrumentation instr = new Instrumentation(transform.apply(plugin));
        Collect.clearData();
        instr.instrument(new PathSource(ClassLoader.getSystemClassLoader(), test_dir),
                new PathDestination(test_dir), new InstrumentationParams());
        Collect.outputFile(coverage);
        Collect.save();
        return Coverage.read(coverage);
    }
    @Test
    public void instrument() throws Exception {
        Coverage tmplt = instrument(UserCode.class, p -> p);
        assertEquals(tmplt.coverage().size(), 0);
    }
    @Test(dependsOnMethods = "instrument")
    public void instrumentStatic() throws Exception {
        Coverage tmplt = instrument(UserCodeStatic.class, p -> new FilteringPlugin(p, s -> s.contains("UserCodeStatic")));
        Map<String, List<List<?>>> userCode =  tmplt.coverage().get(UserCodeStatic.class.getName().replace('.', '/'));
        assertEquals(userCode.size(), 1);
        assertEquals(userCode.keySet().iterator().next(), "main([Ljava/lang/String;)V");
    }
    @Test(dependsOnMethods = "instrumentStatic")
    public void run() throws
                ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException,
                IOException, InstantiationException {
            new Util(test_dir).runClass(UserCodeStatic.class, new String[] {"one", "two"});
            Collect.outputFile(coverage);
            Collect.serializer(new StringArraySerializer());
            Collect.save();
            Coverage res = Coverage.read(coverage, Objects::toString);
            List<List<?>> method =
                    res.get(UserCodeStatic.class.getName().replace('.', '/'),
                            "main([Ljava/lang/String;)V");
            assertEquals(method.size(), 1);
            assertEquals(method.get(0).size(), 2);
            assertEquals(method.get(0).get(0), "one");
            assertEquals(method.get(0).get(1), "two");
    }
    @AfterClass
    public void tearDown() throws IOException {
        if(TestStatusListener.status) Util.rfrm(data_dir);
    }
}
