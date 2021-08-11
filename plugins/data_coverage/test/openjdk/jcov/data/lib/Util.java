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
package openjdk.jcov.data.lib;

import com.sun.tdk.jcov.runtime.JCovSaver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Util {
    private final Path outputDir;

    public Util(Path dir) {
        outputDir = dir;
    }

    public List<Path> copyBytecode(String... classes) throws IOException {
        byte[] buf = new byte[1024];
        List<Path> result = new ArrayList<>();
        for(String c : classes) {
            String classFile = classFile(c);
            try(InputStream in = getClass().getClassLoader().getResourceAsStream(classFile)) {
                Path o = outputDir.resolve(classFile);
                result.add(o);
                if(!Files.exists(o.getParent())) Files.createDirectories(o.getParent());
                try(OutputStream out = Files.newOutputStream(o)) {
                    int read;
                    while((read = in.read(buf)) > 0)
                        out.write(buf, 0, read);
                }
            }
        };
        return result;
    }

    public static String classFile(String className) {
        return className.replace('.', '/') + ".class";
    }
    public Class runClass(Class className, String[] argv, JCovSaver saver)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return runClass(className.getName(), argv, saver);
    }
    public Class runClass(String className, String[] argv, JCovSaver saver)
            throws ClassNotFoundException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        ClassLoader offOutputDir = new InstrumentedClassLoader();
        Class cls = offOutputDir.loadClass(className);
        Method m = cls.getMethod("main", new String[0].getClass());
        m.invoke(null, (Object)argv);
        //have to do this because normally it only works on system exit
        saver.saveResults();
        return cls;
    }

    private class InstrumentedClassLoader extends ClassLoader {
        protected InstrumentedClassLoader() {
            super(Util.class.getClassLoader());
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
