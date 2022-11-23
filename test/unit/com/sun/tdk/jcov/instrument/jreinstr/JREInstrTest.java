/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.jcov.instrument.jreinstr;

import com.sun.tdk.jcov.JREInstr;
import com.sun.tdk.jcov.data.FileFormatException;
import com.sun.tdk.jcov.instrument.DataClass;
import com.sun.tdk.jcov.instrument.DataMethod;
import com.sun.tdk.jcov.instrument.DataPackage;
import com.sun.tdk.jcov.instrument.DataRoot;
import com.sun.tdk.jcov.instrument.Util;
import com.sun.tdk.jcov.io.Reader;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class JREInstrTest {

    Path jre;
    Path userCode;
    Path template;
    Path result;

    static void createUserCode(Path location, Class code) throws IOException {
        String fileName = code.getName().replace('.', '/') + ".class";
        Path classFile = location.resolve(fileName);
        Files.createDirectories(classFile.getParent());
        try (InputStream ci = code.getClassLoader().getResourceAsStream(fileName);
             OutputStream out = Files.newOutputStream(classFile)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = ci.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        }
    }

    @BeforeClass
    public void setup() throws IOException, InterruptedException {
        String testJRE = System.getProperty("test.jre");
        if(testJRE == null) {
            testJRE = System.getProperty("java.home");
        }
        jre = Util.copyJRE(Paths.get(testJRE));
        userCode = Paths.get("user_code");
        createUserCode(userCode, Code.class);
        System.out.println("JRE: " + testJRE);
        template = Path.of(System.getProperty("user.dir")).resolve("template.xml");
        result = Path.of(System.getProperty("user.dir")).resolve("result.xml");
        Files.deleteIfExists(template);
        Files.deleteIfExists(result);
        System.out.println("Template: " + template);
    }

    @Test
    public void testJREInstr() throws IOException, InterruptedException {
        String runtime = Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                .peek(System.out::println)
                .filter(s -> s.endsWith("jcov_file_saver.jar")).findAny().get();
        String[] params = new String[] {
                "-implantrt", runtime,
                "-im", "java.base",
                "-im", "java.desktop",
                jre.toString()};
        System.out.println("Running JREInstr with " + Arrays.stream(params).collect(Collectors.joining(" ")));
        long start = System.currentTimeMillis();
        assertEquals(new JREInstr().run(params), 0);
        assertEquals(Files.readAllLines(template)
                .stream()
                .filter(s -> s.trim().startsWith("<package"))
                .filter(s -> !s.contains("moduleName=\"java.base\""))
                .filter(s -> !s.contains("moduleName=\"java.desktop\""))
                .count(), 0);
        //track instrumentation time for the TODO in copyJRE
        System.out.println("Took " + (System.currentTimeMillis() - start) + " to instrument.");
    }

    @Test(dependsOnMethods = "testJREInstr")
    public void testInstrumentation() throws IOException, InterruptedException {
        List<String> command = List.of(
                jre.toString() + File.separator + "bin" + File.separator + "java",
                "-cp", userCode.toAbsolutePath().toString(), Code.class.getName());
        System.out.println(command.stream().collect(Collectors.joining(" ")));
        new ProcessBuilder()
                .command(command)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start().waitFor();
        assertTrue(Files.exists(Paths.get("result.xml")));
    }

    @Test(dependsOnMethods = "testInstrumentation")
    public void testCoverage() throws IOException, InterruptedException, FileFormatException {
        DataRoot data = Reader.readXML(Files.newInputStream(Paths.get("result.xml")));
        DataPackage pkg = data.getPackages().stream().filter(p -> p.getName().equals("javax/swing")).findAny().get();
        DataClass cls = pkg.getClasses().stream().filter(c -> c.getName().equals("JFrame"))
                .findAny().get();
        DataMethod method = cls.getMethods().stream().filter(m ->
                m.getName().equals("<init>") && m.getVmSignature().equals("()V")
        ).findFirst().get();
        assertEquals(method.getCount(), 1);
    }
    @AfterClass
    public void tearDown() throws IOException {
        if(jre != null && Files.exists(jre)) Util.rmRF(jre);
        if(userCode != null && Files.exists(userCode)) Util.rmRF(userCode);
        Files.deleteIfExists(template);
        Files.deleteIfExists(result);
    }

}
