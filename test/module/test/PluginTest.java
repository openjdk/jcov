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
/* @test
 * @run main PluginTest
 */
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static java.io.File.pathSeparator;
import static java.io.File.separator;
import static java.lang.System.getProperty;
import static java.util.stream.Collectors.toList;

public class PluginTest {
    public static void main(String[] args) throws IOException, InterruptedException {
        Path moduleSource = Path.of(getProperty("test.src")).getParent().resolve("module");
        List<File> files = Files.find(moduleSource, Integer.MAX_VALUE,
                        (a, b) -> !Files.isDirectory(a) && a.toString().endsWith(".java"))
                .map(Path::toFile).collect(toList());
        Path out = Path.of(getProperty("test.classes")).resolve("module"); Files.createDirectories(out);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fileManager =
                     compiler.getStandardFileManager(null, null, null)) {
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager,
                    diagnostic -> {
                        System.out.println(diagnostic.getSource() + ":");
                        System.out.println(diagnostic.getMessage(Locale.getDefault()));
                    }, List.of(
                            "-d", out.toString(),
                            "-p", getProperty("jdk.module.path"),
                            "--add-modules", "jcov"
                    ), null,
                    fileManager.getJavaFileObjectsFromFiles(files));
            if (!task.call()) {
                throw new RuntimeException("Compilation failed");
            }
        }
        List<String> command = List.of(
                getProperty("java.home") + separator + "bin" + separator + "java",
                "-p", getProperty("jdk.module.path") + pathSeparator + out,
                "--add-modules", "jcov,jcov.plugin.test",
                "-m", "jcov.plugin.test/main.Main"
        );
        Process p = new ProcessBuilder().command(command).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        if (p.waitFor() != 0) throw new RuntimeException("Failed");
        String output = new String(p.getInputStream().readAllBytes());
        if(!output.startsWith("instrumentation.Plugin")) {
            System.err.println("Wrong plugin type:");
            System.err.println(output);
            throw new RuntimeException("Wrong plugin type");
        }
    }
}
