/*
 * Copyright (c) 2018, 2025 Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class MainTest {

    Path classesDir;
    Path classesJar;
    @BeforeClass
    void copyByteCode() throws IOException {
        List<Class> testClasses =
                List.of(DelegatorsTest.class, EmptyMethodsTest.class,
                        GettersTest.class, Scanner.class, SettersTest.class, ThrowersTest.class);
        classesDir = Files.createTempDirectory(MainTest.class.getName());
        testClasses.forEach(c -> {
            String fn = c.getName().replace('.', '/') + ".class";
            try (var in = c.getClassLoader()
                    .getResourceAsStream(fn)) {
                Path newFile = classesDir.resolve(fn);
                Files.createDirectories(newFile.getParent());
                Files.copy(in, newFile);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        classesJar = Files.createTempFile(MainTest.class.getName(), ".jar");
        try (var jar = new JarOutputStream(Files.newOutputStream(classesJar))) {
            testClasses.forEach(c -> {
                String fn = c.getName().replace('.', '/') + ".class";
                try {
                    jar.putNextEntry(new JarEntry(fn));
                    try (BufferedInputStream in = new BufferedInputStream(c.getClassLoader().getResourceAsStream(fn))) {
                        byte[] buffer = new byte[1024];
                        while (true) {
                            int count = in.read(buffer);
                            if (count == -1)
                                break;
                            jar.write(buffer, 0, count);
                        }
                        jar.closeEntry();
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

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
        Path delegators = Files.createTempFile("delegators", ".lst");
        System.out.println("testDir output file: " + delegators.toAbsolutePath().toString());
        Scanner.main(new String[]{
                "--delegators", delegators.toAbsolutePath().toString(),
                "file://" + classesDir
        });
        assertTrue(Files.lines(delegators).anyMatch(l ->
                l.equals(DelegatorsTest.class.getName().replace('.', '/') + "#foo(I)I")));
        Files.delete(delegators);
    }

    @Test
    public void testJAR() throws IOException, URISyntaxException {
        Path setters = Files.createTempFile("setters", ".lst");
        System.out.println("testJAR output file: " + setters.toAbsolutePath().toString());
        Scanner.main(new String[]{
                "--setters", setters.toAbsolutePath().toString(),
                "jar:file:" + classesJar});
        assertTrue(Files.lines(setters).anyMatch(l ->
                l.equals(SettersTest.class.getName().replace('.', '/') + "#setField(I)V")));
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "Unknown filter.*")
    public void testWrongFilter() throws IOException, URISyntaxException {
            Scanner.main(new String[]{
                    "--a-non-existing-filter", "a_file",
                    "file:///a/path"});
    }

    @Test
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

    @Test
    public void testTwoFilters() throws IOException, URISyntaxException {
        Path setters = Files.createTempFile("setters2_", ".lst");
        Path getters = Files.createTempFile("getters2_", ".lst");
        System.out.println("testDir output file: " + setters.toAbsolutePath().toString());
        System.out.println("testDir output file: " + getters.toAbsolutePath().toString());
        Scanner.main(new String[]{
                "--setters", setters.toAbsolutePath().toString(),
                "--getters", getters.toAbsolutePath().toString(),
                "file://" + classesDir
        });
        assertTrue(Files.lines(setters).anyMatch(l ->
                l.equals(SettersTest.class.getName().replace('.', '/') + "#setField(I)V")));
        assertTrue(Files.lines(getters).anyMatch(l ->
                l.equals(GettersTest.class.getName().replace('.', '/') + "#getField()I")));
        Files.delete(setters);
        Files.delete(getters);
    }

    @AfterClass
    void deleteByteCode() throws IOException {
        if (Files.exists(classesDir))
            Files.walkFileTree(classesDir, new FileVisitor<Path>() {
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
                    return FileVisitResult.TERMINATE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        if (Files.exists(classesJar))
            Files.delete(classesJar);
    }
}
