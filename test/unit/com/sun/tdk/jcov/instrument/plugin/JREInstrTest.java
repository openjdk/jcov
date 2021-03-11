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

import com.sun.tdk.jcov.JREInstr;
import com.sun.tdk.jcov.instrument.InstrumentationPlugin;
import com.sun.tdk.jcov.runtime.Collect;
import org.objectweb.asm.MethodVisitor;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class JREInstrTest {

    public static final String TIMES_SAVED = "TIMES SAVED ";
    public static final String TIMES_CALLED = "TIMES CALLED ";

    Path rtJar;
    Path jre;

    private Path copyJRE(Path src) throws IOException {
        Path dest = Files.createTempDirectory("JDK");
        Files.walk(src).forEach(s -> {
            try {
                Files.copy(s, dest.resolve(src.relativize(s)), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        return dest;
    }

    private void removeJRE(Path jre) throws IOException {
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

    private Path createJCovRtJar() throws IOException {
        Path dest = Files.createTempFile("jcov-rt-", ".jar");
        System.out.println("rt jar: " + dest);
        try(JarOutputStream jar = new JarOutputStream(Files.newOutputStream(dest))) {
            jar.putNextEntry(new JarEntry(Collect.class.getName().replace(".", File.separator) + ".class"));
        }
        return dest;
    }

    @BeforeClass
    public void setup() throws IOException {
        String testJRE = System.getProperty("test.jre");
        if(testJRE == null) {
            testJRE = System.getProperty("java.home");
        }
        rtJar = createJCovRtJar();
        jre = copyJRE(Paths.get(testJRE));
    }

    @Test
    public void testJREInstr() throws IOException, InterruptedException {
        VoidPlugin.reset();
        String[] params = new String[] {
                "-implantrt", rtJar.toString(),
                "-instr_plugin", VoidPlugin.class.getName(), jre.toString()};
        System.out.println(params);
        new JREInstr().run(params);
        assertEquals(VoidPlugin.savedTimes.intValue(), 1);
        assertTrue(VoidPlugin.calledTimes.get() > 0);
    }

    @AfterClass
    public void tearDown() throws IOException {
        if(jre != null && Files.exists(jre)) removeJRE(jre);
        if(rtJar != null && Files.exists(rtJar)) Files.delete(rtJar);
    }

    public static class VoidPlugin implements InstrumentationPlugin {

        public static AtomicInteger calledTimes = new AtomicInteger(0);
        public static AtomicInteger savedTimes = new AtomicInteger(0);

        public static void reset() {
            calledTimes.set(0);
            savedTimes.set(0);
        }

        @Override
        public MethodVisitor methodVisitor(int access, String owner, String name, String desc, MethodVisitor visitor) {
            calledTimes.incrementAndGet();
            return null;
        }

        @Override
        public void instrumentationComplete() throws Exception {
            savedTimes.incrementAndGet();
        }
    }
}
