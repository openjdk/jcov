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
package com.sun.tdk.jcov.lib;

import com.sun.tdk.jcov.Instr;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.io.File.pathSeparator;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class InstrProxy {
    private final Path outputDir;

    public InstrProxy(Path outputDir) {
        this.outputDir = outputDir;
    }

    public void copyBytecode(String... classes) throws IOException {
        byte[] buf = new byte[1024];
        for(String c : classes) {
            String classFile = classFile(c);
            try(InputStream in = InstrProxy.class.getClassLoader().getResourceAsStream(classFile)) {
                Path o = outputDir.resolve(classFile);
                if(!Files.exists(o.getParent())) Files.createDirectories(o.getParent());
                try(OutputStream out = Files.newOutputStream(o)) {
                    int read;
                    while((read = in.read(buf)) > 0)
                        out.write(buf, 0, read);
                }
            }
        };
    }

    public int instr(String[] options, Consumer<String> outConsumer, Consumer<String> errConsumer, String... classes) throws IOException, InterruptedException {
        if(!Files.exists(outputDir) || !Files.isDirectory(outputDir)) {
            Files.createDirectories(outputDir);
        }
        //this does not work because
        //Warning: Add input source(s) to the classpath: -cp jcov.jar:...
//        List<String> params = new ArrayList<>();
//        params.addAll(List.of(options));
//        params.addAll(Arrays.stream(classes).map(c -> outputDir.resolve(classFile(c)).toString()).collect(toList()));
//        System.out.println(params.stream().collect(Collectors.joining(" ")));
//        new Instr().run(params.toArray(new String[0]));
        List<String> files = Arrays.stream(classes).map(this::classFile)
                .map(outputDir::resolve)
                .map(Path::toString)
                .collect(toList());
        List<String> command = new ArrayList<>();
        command.add(System.getProperty("java.home") + "/bin/java");
        command.add("-Djcov.selftest=true");
        command.add("-Djcov.stacktrace=true");
        command.add("-cp");
        command.add(System.getProperty("java.class.path") +
                        pathSeparator + files.stream()
                        .collect(joining(pathSeparator)));
        command.add(Instr.class.getName());
        command.addAll(Arrays.asList(options));
        command.addAll(files);
        System.out.println(command.stream().collect(joining(" ")));
        ProcessBuilder pb = new ProcessBuilder().command(command.toArray(new String[0]));
        if(outConsumer == null)
            pb = pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        if(errConsumer == null)
            pb = pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        Process p = pb.start();
        if(outConsumer != null) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while((line = in.readLine()) != null)
                    outConsumer.accept(line);
            }
        }
        if(errConsumer != null) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                String line;
                while((line = in.readLine()) != null)
                    errConsumer.accept(line);
            }
        }
        return p.waitFor();
    }

    public String classFile(String className) {
        return className.replace('.', '/') + ".class";
    }

    public Class runClass(String className, String[] argv)
            throws ClassNotFoundException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        ClassLoader offOutputDir = new InstrumentedClassLoader();
        Class cls = offOutputDir.loadClass(className);
        Method m = cls.getMethod("main", new String[0].getClass());
        m.invoke(null, (Object)argv);
        return cls;
    }

    private class InstrumentedClassLoader extends ClassLoader {
        protected InstrumentedClassLoader() {
            super(InstrProxy.class.getClassLoader());
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            Path classFile = outputDir.resolve(classFile(name));
            if(Files.exists(classFile)) {
                byte[] buf = new byte[1024];
                try(InputStream in = Files.newInputStream(classFile)) {
                    try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                        int read;
                        while((read = in.read(buf)) > 0)
                            out.write(buf, 0, read);
                        return defineClass(name, out.toByteArray(), 0, out.size());
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return super.loadClass(name);
        }
    }
}
