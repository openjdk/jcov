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
package openjdk.jcov.data.jdk;

import openjdk.jcov.data.fields.instrument.Plugin;
import openjdk.jcov.data.fields.instrument.TemplateGenerator;
import openjdk.jcov.data.fields.runtime.Collect;
import openjdk.jcov.data.fields.runtime.Coverage;
import openjdk.jcov.data.fields.runtime.Runtime;
import openjdk.jcov.data.instrument.InstrumentJDK;
import openjdk.jcov.data.lib.Util;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static openjdk.jcov.data.runtime.CoverageData.COVERAGE_OUT;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class JDKTestFields {
    static Path instrJDK;
    static Path template;
    @BeforeClass
    public static void prepare() throws IOException {
        instrJDK = Util.copyJRE(Path.of(System.getProperty("java.home")));
        template = instrJDK.resolve("template.lst");
//        System.setProperty(COVERAGE_IN, template.toString());
    }
    @Test
    public void instrument() throws IOException, InterruptedException, ClassNotFoundException,
            InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
//        Path template = Files.createTempFile("template", ".lst");
        Runtime.init();
        System.out.println(template);
        System.setProperty(COVERAGE_OUT, template.toString());
        TemplateGenerator generator = new TemplateGenerator();
        generator.setFieldFilter(fm -> {
            if (fm.parent().isPresent()) {
                String typeName = fm.parent().get().thisClass().name().stringValue();
                if (typeName.equals("java/time/LocalDateTime")) {
                    String fieldName = fm.fieldName().stringValue();
                    return fieldName.equals("date") || fieldName.equals("time");
                }
            }
            return false;
        });
        generator.generate(FileSystems.getFileSystem(URI.create("jrt:/")).getPath("/modules/java.base/java/time"));
        //this is only needed for the manual file review.
        //since running in the same VM, Collect.data() will just keep holding the data
//        Coverage.write(Collect.data(), template);
        Plugin plugin = new Plugin();
        assertEquals(new InstrumentJDK(plugin).instrument(instrJDK, List.of("java.base"), Plugin.class,
                        List.of(Collect.class.getPackageName()), "java.time.LocalDateTime", template),
                0);
    }
    @Test(dependsOnMethods = "instrument")
    public void run() throws IOException, InterruptedException {
        Path output = instrJDK.resolve("coverage.lst");
        String[] command = new String[] {
                instrJDK.resolve("bin").resolve("java").toString(),
                "-D" + COVERAGE_OUT + "=" + output.toString(),
                "-cp", System.getProperty("java.class.path"),
                UserCode.class.getName()
        };
        new ProcessBuilder(command).inheritIO().start().waitFor();
        System.out.println("Expecting output in " + output);
        assertTrue(Files.exists(output));
        Coverage coverage = Coverage.read(output);
        assertTrue(coverage.get("java/time/LocalDateTime", "date").contains("0001-01-01"));
        assertTrue(coverage.get("java/time/LocalDateTime", "time").contains("01:01"));
    }
    @AfterClass
    public static void cleanup() throws IOException {
        Util.rfrm(instrJDK);
    }
}
