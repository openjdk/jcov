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

import com.sun.tdk.jcov.instrument.InstrumentationParams;
import com.sun.tdk.jcov.instrument.plugin.ClassListSource;
import com.sun.tdk.jcov.instrument.plugin.ImplantingPlugin;
import com.sun.tdk.jcov.instrument.plugin.Instrumentation;
import com.sun.tdk.jcov.instrument.plugin.PathDestination;
import com.sun.tdk.jcov.instrument.plugin.PathSource;
import jdk.internal.classfile.ClassBuilder;
import jdk.internal.classfile.MethodModel;
import openjdk.jcov.data.Env;
import openjdk.jcov.data.runtime.CoverageData;
import openjdk.jcov.data.runtime.Deserializer;
import openjdk.jcov.data.runtime.Runtime;
import openjdk.jcov.data.runtime.Serializer;

import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static openjdk.jcov.data.Env.JCOV_DATA_ENV_PREFIX;

public class Instrument {
    /**
     * Name of a system property which contains class name of an instrumentation plugin.
     */
    public static final String PLUGIN_CLASS = JCOV_DATA_ENV_PREFIX + "plugin";

    public static void main(String[] args) throws Exception {
        Path src = Paths.get(args[0]);
        Path dest;
        if(args.length > 2)
            dest = Paths.get(args[1]);
        else
            dest = src;
        Plugin plugin = Env.getSPIEnv(PLUGIN_CLASS, new DoNothingPlugin());
        Set<Class> runtime = new HashSet<>(plugin.runtime());
        runtime.addAll(Set.of(CoverageData.class, Deserializer.class, Runtime.class, Serializer.class, Env.class));
        Set.of(Runtime.serializer().getClass(), Runtime.deserializer().getClass()).forEach(cls -> {
            if (!runtime.contains(cls)) runtime.add(cls);
        });
        com.sun.tdk.jcov.instrument.InstrumentationPlugin imlanting =
                new ImplantingPlugin(plugin, new ClassListSource(Instrument.class.getClassLoader(),
                runtime));
        Instrumentation instrumentation = new Instrumentation(imlanting);
        instrumentation.instrument(new PathSource(ClassLoader.getSystemClassLoader(), src),
                new PathDestination(dest), new InstrumentationParams());
    }

    private static class DoNothingPlugin extends Plugin {

        public DoNothingPlugin() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException,
                IllegalAccessException, InstantiationException {
        }

        @Override
        public Map<String, Consumer<OutputStream>> complete() throws Exception {
            return Map.of();
        }

        @Override
        public Set<Class> runtime() {
            return Set.of();
        }

        @Override
        public void instrument(ClassBuilder clb, MethodModel mm) throws Exception {

        }

    }
}
