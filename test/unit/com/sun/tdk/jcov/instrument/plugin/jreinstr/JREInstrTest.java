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
package com.sun.tdk.jcov.instrument.plugin.jreinstr;

import com.sun.tdk.jcov.JREInstr;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class JREInstrTest {

    public static final String TIMES_SAVED = "TIMES SAVED ";
    public static final String TIMES_CALLED = "TIMES CALLED ";

    Path rtJar;
    Path jre;

    private Path copyJRE(Path src) throws IOException, InterruptedException {
        Path dest = Files.createTempDirectory("JDK");
        System.out.println("Copying " + src + " to " + dest);
        Files.walk(src).forEach(s -> {
            try {
                Files.copy(s, dest.resolve(src.relativize(s)), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        return dest;
        //TODO make this to work, as it would be a whole lot faster
        //a JDK created by jimage can not be used, it fails with
        //Command line error: bootmodules.jimage was not found in modules directory
//        Path dest = Files.createTempDirectory("test_data");
//        Path res = dest.resolve("jdk");
//        List<String> command = List.of(
//                src.toString() + File.separator + "bin" + File.separator + "jlink",
//                "--add-modules",
//                "java.base,java.compiler",
//                "--output",
//                res.toString()
//        );
//        System.out.println(command.stream().collect(Collectors.joining(" ")));
//        assertEquals(new ProcessBuilder(command)
//                .redirectError(ProcessBuilder.Redirect.INHERIT)
//                .redirectInput(ProcessBuilder.Redirect.INHERIT)
//                .start().waitFor(), 0);
//        return res;
    }

    private void removeJRE(Path jre) throws IOException {
        System.out.println("Removing " + jre);
        Files.walkFileTree(jre, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    static Path createRtJar(String prefix, Class collect) throws IOException {
        Path dest = Files.createTempFile(prefix, ".jar");
        System.out.println(prefix + " jar: " + dest);
        try(JarOutputStream jar = new JarOutputStream(Files.newOutputStream(dest))) {
            jar.putNextEntry(new JarEntry(collect.getName().replace(".", File.separator) + ".class"));
            try (InputStream ci = collect.getClassLoader()
                    .getResourceAsStream(collect.getName().replace('.', '/') + ".class")) {
                byte[] buffer = new byte[1024];
                int read;
                while((read = ci.read(buffer)) > 0) {
                    jar.write(buffer, 0, read);
                }
            }
        }
        return dest;
    }

    @BeforeClass
    public void setup() throws IOException, InterruptedException {
        String testJRE = System.getProperty("test.jre");
        if(testJRE == null) {
            testJRE = System.getProperty("java.home");
        }
        rtJar = createRtJar("jcov-rt-", Collect.class);
        jre = copyJRE(Paths.get(testJRE));
    }

    @Test
    public void testJREInstr() throws IOException, InterruptedException {
        TestPlugin.reset();
        String runtime = Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                .peek(System.out::println)
                .filter(s -> s.endsWith("jcov_network_saver.jar")).findAny().get();
        String[] params = new String[] {
                "-implantrt", runtime,
                "-instr_plugin", TestPlugin.class.getName(),
                jre.toString()};
        System.out.println("Running JREInstr with " + Arrays.stream(params).collect(Collectors.joining(" ")));
        assertEquals(new JREInstr().run(params), 0);
        assertEquals(TestPlugin.savedTimes.intValue(), 1);
        assertTrue(TestPlugin.calledTimes.get() > 0);
    }

    @Test(dependsOnMethods = "testJREInstr")
    public void testInstrumentation() throws IOException, InterruptedException {
        //no classpath necessary for the next call because the class is implanted
        List<String> command = List.of(
                jre.toString() + File.separator + "bin" + File.separator + "java",
                Collect.class.getName());
        System.out.println(command.stream().collect(Collectors.joining(" ")));
        Process p = new ProcessBuilder()
                .command(command)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();
        boolean jcovExported = false;
        boolean pluginExported = false;
        try(BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while((line = in.readLine()) != null && !(jcovExported && pluginExported)) {
                System.out.println(line);
                if(line.equals(Collect.class.getPackage().getName())) pluginExported = true;
                if(line.equals(com.sun.tdk.jcov.runtime.Collect.class.getPackage().getName())) jcovExported = true;
            }
        }
        p.waitFor();
        assertTrue(pluginExported && jcovExported);
    }

    @AfterClass
    public void tearDown() throws IOException {
        if(jre != null && Files.exists(jre)) removeJRE(jre);
        if(rtJar != null && Files.exists(rtJar)) Files.delete(rtJar);
    }

}
