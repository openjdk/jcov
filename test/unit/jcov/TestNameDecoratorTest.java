/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
package jcov;

import com.sun.tdk.jcov.JREInstr;
import com.sun.tdk.jcov.data.Scale;
import com.sun.tdk.jcov.instrument.DataClass;
import com.sun.tdk.jcov.instrument.DataMethod;
import com.sun.tdk.jcov.instrument.Util;
import com.sun.tdk.jcov.instrument.DataRoot;
import com.sun.tdk.jcov.io.Reader;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Runs two tests in jtreg through a JCov grabber to verify that JTRegs sets
 * "test.name" property iin the test VM.
 */
public class TestNameDecoratorTest {

    @Test
    void passesJtregTestNamesToGrabber() throws Exception {
        Path workDir = Path.of(System.getProperty("test.classes")).getParent()
                .resolve(System.getProperty("test.name")
                        .replace(".java", ".d"));
        Path result = workDir.resolve("result.xml");
        Path testList = workDir.resolve("testlist.txt");
        Path template = workDir.resolve("template.xml");
        Path jdk = Util.copyJRE(Path.of(System.getProperty("java.home")));
        String vectorTest = "jcov/VectorTest.java";
        String listTest = "jcov/ArrayListTest.java";

        try {
            instrument(jdk, template);
            startGrabber(template, result, testList);
            ProcessBuilder builder = new ProcessBuilder(
                    Path.of(System.getenv("JT_HOME"), "bin", "jtreg").toString(),
                    "-conc:1", "-othervm",
                    "-e:JAVA_TOOL_OPTIONS=-Xms64m -Xmx4g",
                    "-e:_JAVA_OPTIONS=-Xms64m -Xmx4g",
                    "-cpa:" + System.getProperty("java.class.path"),
                    "-jdk:" + jdk,
                    "-vmoption:-Djcov.extension=com.sun.tdk.jcov.runtime.TestNameDecorator",
                    "-workDir:" + workDir.resolve("JTwork"),
                    "-reportDir:" + workDir.resolve("JTreport"),
                    Path.of(System.getProperty("test.src"), vectorTest).toString(),
                    Path.of(System.getProperty("test.src"), listTest).toString());
            builder.environment().put("JAVA_HOME", System.getProperty("java.home"));
            assertEquals(builder.inheritIO().start().waitFor(), 0);
        } finally {
            stopGrabber();
            Util.rmRF(jdk);
        }

        List<String> tests = Files.readAllLines(testList);
        int vectorIndex = tests.indexOf(vectorTest);
        assertTrue(vectorIndex > -1);
        int listIndex = tests.indexOf(listTest);
        assertTrue(listIndex > -1);

        DataRoot data = Reader.readXML(Files.newInputStream(result));
        assertCoveredBy(data, "Vector", vectorIndex);
        assertCoveredBy(data, "ArrayList", listIndex);
    }

    private void assertCoveredBy(DataRoot data, String className, int testIndex) {
        Scale scale = data.getClasses().stream()
                .filter(cls -> cls.getFullname().equals("java/util/" + className)).findAny().get()
                .getMethods().stream()
                .filter(method -> method.getName().equals("<init>") &&
                        method.getVmSignature().equals("()V")).findAny().get()
                .getScale();
        assertTrue(scale != null && scale.isBitSet(testIndex));
    }

    private void instrument(Path jdk, Path template) throws Exception {
        assertEquals(new JREInstr().run(new String[] {
                "-implantrt", classPathEntry("jcov_network_saver.jar"),
                "-im", "java.base",
                "-template", template.toString(),
                jdk.toString()
        }), 0);
    }

    private void startGrabber(Path template, Path result, Path testList)
            throws IOException, InterruptedException {
        new ProcessBuilder(System.getProperty("java.home") + "/bin/java",
                "-cp", classPathEntry("jcov.jar"),
                "com.sun.tdk.jcov.Grabber", "-v", "-t", template.toString(),
                "-o", result.toString(), "-scale", "-mergebyname",
                "-outTestList", testList.toString())
                .inheritIO().start();
        var manager = new ProcessBuilder(System.getProperty("java.home") + "/bin/java",
                "-cp", classPathEntry("jcov.jar"),
                "com.sun.tdk.jcov.GrabberManager", "-t", "600", "-wait")
                .inheritIO().start();
        assertEquals(manager.waitFor(), 0);
    }

    private void stopGrabber() throws IOException, InterruptedException {
        Process manager = new ProcessBuilder(System.getProperty("java.home") + "/bin/java",
                "-cp", classPathEntry("jcov.jar"),
                "com.sun.tdk.jcov.GrabberManager", "-stop", "-stoptimeout", "3600")
                .inheritIO().start();
        assertEquals(manager.waitFor(), 0);
    }

    private String classPathEntry(String name) {
        return Arrays.stream(System.getProperty("java.class.path")
                        .split(File.pathSeparator))
                .filter(entry -> entry.endsWith(name))
                .findFirst()
                .get();
    }
}
