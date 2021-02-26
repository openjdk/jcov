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
package com.sun.tdk.jcov.instrument.plugin;

import com.sun.tdk.jcov.lib.InstrProxy;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.sun.tdk.jcov.instrument.plugin.FieldsPlugin.INSTRUMENTATION_COMPLETE;
import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.joining;
import static org.testng.Assert.*;

/**
 * Tests that it is possible to use instrumentation plugin and a corresponding data saver.
 */
public class FieldsTest {
    Path test_dir;
    InstrProxy instr;

    /**
     * Perform the instrumentation.
     */
    @BeforeClass
    public void instrument() throws IOException, InterruptedException {
        test_dir = Paths.get(System.getProperty("user.dir")).resolve("plugin_test");
        instr = new InstrProxy(test_dir);
        instr.copyBytecode(FieldsClass.class.getName());
        System.getProperties().setProperty("jcov.selftest", "true");
        int[] instrumentationCompleteTimes = new int[1];
        instr.instr(new String[]{"-instr_plugin", "com.sun.tdk.jcov.instrument.plugin.FieldsPlugin"},
                line -> {
                    if(line.startsWith(INSTRUMENTATION_COMPLETE))
                        instrumentationCompleteTimes[0] =
                                parseInt(line.substring(INSTRUMENTATION_COMPLETE.length()));
                }, null,
                FieldsClass.class.getName());
        assertEquals(instrumentationCompleteTimes[0], 1);
        //this does not work because
        //Warning: Add input source(s) to the classpath: -cp jcov.jar:...
        //see InstrProxy class for more info
        //assertEquals(FieldsPlugin.completeCount.intValue(), 1);
    }

    /**
     * Check collected field values at runtime in the same VM.
     */
    @Test
    public void fields() throws IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        instr.runClass(FieldsClass.class.getName(), new String[0]);
        testFieldValuesSameVM("field1", Set.of(1, 2));
        testFieldValuesSameVM("field2", Set.of("", "two", "one"));
    }
    private void testFieldValuesSameVM(String field, Set<Object> values) {
        String fullName = FieldsClass.class.getName().replace('.','/') + "." + field;
        testFieldValues(fullName, field, values, FieldsPlugin.values.get(fullName));
    }

    /**
     * Test that data saver is called.
     */
    @Test
    public void testSaver() throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(System.getProperty("java.home") + "/bin/java");
        command.add("-Djcov.data-saver="+FieldsPlugin.class.getName());
        command.add("-cp");
        command.add(test_dir
                + File.pathSeparator + System.getProperty("java.class.path"));
        command.add(FieldsClass.class.getName());
        System.out.println(command.stream().collect(joining(" ")));
        Process p = new ProcessBuilder().command(command.toArray(new String[0]))
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            List<String> lines = in.lines().collect(Collectors.toList());
            testFieldValuesOtherVM("field1", Set.of(1, 2), lines);
            testFieldValuesOtherVM("field2", Set.of("", "two", "one"), lines);
        }
        assertEquals(p.waitFor(), 0);
    }
    private void testFieldValuesOtherVM(String field, Set<Object> values, List<String> lines) {
        String fullName = FieldsClass.class.getName().replace('.','/') + "." + field;
        testFieldValues(fullName, field,
                values.stream().map(Object::toString).collect(Collectors.toSet()),
                lines.stream().filter(l -> l.startsWith(fullName + "="))
                        .map(l -> l.substring(fullName.length() + 1))
                        .collect(Collectors.toSet()));
    }
    private void testFieldValues(String fullName, String field, Set<Object> values, Set<Object> actual) {
        if(values.size() == 0) {
            assertFalse(FieldsPlugin.values.containsKey(fullName), "No values for field " + fullName);
        } else {
            assertNotNull(actual);
            System.out.printf("Comparing [%s] with [%s]\n",
                    values.stream().map(Object::toString).collect(joining(",")),
                    actual.stream().map(Object::toString).collect(joining(",")));
            assertEquals(values.size(), actual.size(), "size");
            assertTrue(values.stream().allMatch(actual::contains), "content is the same");
        }
    }
}
