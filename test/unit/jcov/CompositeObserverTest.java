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

import org.testng.annotations.Test;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * Runs a nested jtreg invocation with a {@link CompositeObserver} configured
 * with two {@link TestObserver} instances. The test verifies that both
 * observers record the same callback sequence.
 */
public class CompositeObserverTest {
    @Test
    void twoObserverTests() throws Exception {
        Path output = Files.createTempDirectory("TestObserver");
        Path jtreg = Path.of(System.getenv("JT_HOME"), "bin", "jtreg");
        Path observer = classPath(CompositeObserver.class);
        Path testObserver = classPath(TestObserver.class);
        Path workDir = Path.of(System.getProperty("test.classes")).getParent()
                .resolve(System.getProperty("test.name").replace(".java", ".d"));

        ProcessBuilder builder = new ProcessBuilder(jtreg.toString(), "-conc:1",
                "-cpa:" + System.getProperty("java.class.path"),
                "-workDir:" + workDir,
                "-reportDir:" + workDir.resolve("JTreport"),
                "-observer:jcov.CompositeObserver",
                "-observerDir:" + observer + java.io.File.pathSeparator + testObserver,
                "-J-Djtreg.observers.to.chain=jcov.TestObserver,jcov.TestObserver",
                "-J-Djcov.testObserver.files=" + output,
                Path.of(System.getProperty("test.src"), "jcov/VectorTest.java").toString());
        builder.environment().put("JAVA_HOME", System.getProperty("java.home"));
        Process process = builder.inheritIO().start();
        assertEquals(process.waitFor(), 0);

        List<String> callbacks1 = Files.readAllLines(output.resolve("1.log"));
        List<String> callbacks2 = Files.readAllLines(output.resolve("2.log"));
        List<String> expected = List.of(
                "startingTestRun(" + System.getProperty("test.src") + ")",
                "startingTest(jcov/VectorTest.java)",
                "finishedTest(jcov/VectorTest.java)",
                "finishedTesting()",
                "finishedTesting(4)",
                "finishedTestRun(true)");
        assertEquals(callbacks1, expected);
        assertEquals(callbacks1, callbacks2);
    }

    private Path classPath(Class<?> clazz) throws URISyntaxException {
        return Path.of(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
    }
}
