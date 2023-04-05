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
package openjdk.jcov.data.instrument;

import com.sun.tdk.jcov.JREInstr;
import openjdk.jcov.data.Env;
import openjdk.jcov.data.runtime.EntryControl;
import openjdk.jcov.data.runtime.Runtime;
import openjdk.jcov.data.runtime.Serializer;
import openjdk.jcov.data.runtime.serialization.ToStringSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

public class InstrumentJDK {

    private final Plugin plugin;

    public InstrumentJDK(Plugin plugin) {
        this.plugin = plugin;
    }

    public int instrument(Path jdk, List<String> modules, Class<? extends Plugin> aClass,
                          List<String> exports,
                          String includes, Path template) throws IOException {
        List<String> params = new ArrayList<>();
        modules.forEach(m -> {
            params.add("-im"); params.add(m);
        });
        params.add("-i"); params.add(includes);
        params.add("-instr_plugin"); params.add(aClass.getName());
        params.add("-t"); params.add(template.toString());
        Path rt = createRtJar();
        params.add("-rt"); params.add(rt.toString());
        params.add(jdk.toString());
        System.setProperty("jcov.java.base.exports", exports.stream().collect(Collectors.joining(",")));
        System.out.println("Running JREInstr with: " + params.stream().collect(Collectors.joining(" ")));
        try {
            return new JREInstr().run(params.toArray(new String[0]));
        } finally {
            Files.deleteIfExists(rt);
        }
    }

    public Path createRtJar() throws IOException {
        Path dest = Files.createTempFile("rtjar", ".jar");
        List<Class> runtime = new ArrayList<>(plugin.runtime());
        //TODO move to Plugin
        runtime.addAll(List.of(Env.class, Runtime.class, Serializer.class, ToStringSerializer.class,
                EntryControl.class));
        try(JarOutputStream jar = new JarOutputStream(Files.newOutputStream(dest))) {
            for (Class cls : runtime) {
                String entryName = cls.getName().replace('.', '/') + ".class";
                jar.putNextEntry(new JarEntry(entryName));
                try (InputStream ci = cls.getClassLoader()
                        .getResourceAsStream(entryName)) {
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = ci.read(buffer)) > 0) {
                        jar.write(buffer, 0, read);
                    }
                }
            }
        }
        return dest;
    }
}
