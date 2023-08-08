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
package openjdk.codetools.jcov.report.source;

import openjdk.codetools.jcov.report.LineRange;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class SourceReaderTest {
    Path sourceRoot;
    String[] files = {"1.java", "2.java"};
    @BeforeClass
    void createFiles() throws IOException {
        sourceRoot = Files.createTempDirectory("source.root");
        Files.createDirectories(sourceRoot);
        for (var file : files) {
            try (BufferedWriter out = Files.newBufferedWriter(sourceRoot.resolve(file))) {
                for (int i = 1; i <= 100; i++) {
                    out.write(i + ""); out.newLine();
                }
                out.flush();
            }
        }
    }
    @Test
    void reader() throws IOException {
        var read = new SourceReader(new SourcePath(List.of(sourceRoot)), file -> List.of(new LineRange(1,2)))
                .read(files[0]);
        assertEquals(read.size(), 2);
        for (int i = 0; i <= 1; i++) {
            assertEquals(read.get(i), i + 1 +"");
        }
    }
}
