/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package openjdk.jcov.filter.simplemethods;

import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class MainTest {

    @Test
    public void testJRTFS() throws IOException, URISyntaxException {
        Path getters = Files.createTempFile("getters", ".lst");
        System.out.println("testJRTFS output file: " + getters.toAbsolutePath().toString());
        Scanner.main(new String[]{
                "--getters", getters.toAbsolutePath().toString(),
                "--include", "java.util",
                "jrt:/"
        });
        assertTrue(Files.lines(getters).anyMatch(l -> l.equals("java/util/ArrayList#size()I")));
        assertTrue(Files.lines(getters).noneMatch(l -> l.equals("java/io/File#getPath()Ljava/lang/String;")));
        Files.delete(getters);
    }

    @Test
    public void testJRTFSNoJavaUtil() throws IOException, URISyntaxException {
        Path getters = Files.createTempFile("throwers", ".lst");
        System.out.println("testJRTFSNoJavaUtil output file: " + getters.toAbsolutePath().toString());
        Scanner.main(new String[]{
                "--throwers", getters.toAbsolutePath().toString(),
                "--exclude", "java.util",
                "jrt:/"
        });
        assertTrue(Files.lines(getters).noneMatch(l -> l.equals("java/util/AbstractList#add(ILjava/lang/Object;)V")));
        assertTrue(Files.lines(getters).anyMatch("java/io/InputStream#reset()V"::equals));
        Files.delete(getters);
    }

    @Test
    public void testDir() throws IOException, URISyntaxException {
        String dir = System.getProperty("test.classes");
        Path delegators = Files.createTempFile("delegators", ".lst");
        System.out.println("testDir output file: " + delegators.toAbsolutePath().toString());
        Scanner.main(new String[]{
                "--delegators", delegators.toAbsolutePath().toString(),
                "file://" + dir
        });
        assertTrue(Files.lines(delegators).anyMatch(l ->
                l.equals(DelegatorsTest.class.getName().replace('.', '/') + "#foo(I)I")));
        Files.delete(delegators);
    }

    @Test
    public void testJAR() throws IOException, URISyntaxException {
        String jar = System.getProperty("test.jar");
        Path setters = Files.createTempFile("setters", ".lst");
        System.out.println("testJAR output file: " + setters.toAbsolutePath().toString());
        Scanner.main(new String[]{
                "--setters", setters.toAbsolutePath().toString(),
                "jar:file:" + jar});
        assertTrue(Files.lines(setters).anyMatch(l ->
                l.equals(SettersTest.class.getName().replace('.', '/') + "#setField(I)V")));
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "Unknown filter.*")
    public void testWrongFilter() throws IOException, URISyntaxException {
            Scanner.main(new String[]{
                    "--a-non-existing-filter", "a_file",
                    "file:///a/path"});
    }

    public void testNoFilter() throws IOException, URISyntaxException {
        try {
            Scanner.main(new String[]{
                    "file:///a/path"});
            fail("No RuntimeException when no filters specified");
        } catch (RuntimeException e) {
            assertTrue(Arrays.stream(Scanner.Filter.values()).map(f -> "--" + f.name())
                    .allMatch(f -> e.getMessage().contains(f)), "Incorrect error message: " + e.getMessage());
        }
    }
}
