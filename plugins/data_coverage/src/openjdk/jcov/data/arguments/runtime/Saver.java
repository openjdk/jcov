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
package openjdk.jcov.data.arguments.runtime;

import com.sun.tdk.jcov.runtime.JCovSaver;
import openjdk.jcov.data.Env;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.function.Function;

public class Saver implements JCovSaver {

    /**
     * Name of a property defining where to save the results.
     */
    public static final String RESULT_FILE = Collect.JCOV_DATA_ENV_PREFIX + Collect.ARGUMENTS_PREFIX + "result";
    /**
     * Name of a property containing a class name of a class of type <code>Function<Object, String></code> which will
     * be used during the serialization. <code>Object::toString</code> is used by default.
     */
    public static final String SERIALIZER = Collect.JCOV_DATA_ENV_PREFIX +
            Collect.ARGUMENTS_PREFIX + "serializer";

    private Path resultFile;
    private Serializer serializer;

    public Saver() throws
            ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException,
            IllegalAccessException {
        resultFile = Env.getPathEnv(RESULT_FILE, Paths.get("result.lst"));
        serializer = wrap(Env.getSPIEnv(SERIALIZER, Object::toString));
    }

    public Saver resultFile(Path resultFile) {
        this.resultFile = resultFile;
        return this;
    }

    public Saver serializer(Function<Object, String> function) {
        this.serializer = wrap(function);
        return this;
    }

    public Saver serializer(Serializer serializer) {
        this.serializer = serializer;
        return this;
    }

    private static Serializer wrap(Function<Object, String> function) {
        if(function instanceof Serializer)
            return (Serializer) function;
        else
            return new NoRuntimeSerializer(function);
    }

    public void saveResults() {
        try {
            Coverage.write(Collect.data, resultFile, serializer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class NoRuntimeSerializer implements Serializer {
        private final Function<Object, String> function;

        public NoRuntimeSerializer(Function<Object, String> function) {
            this.function = function;
        }

        @Override
        public String apply(Object o) {
            return function.apply(o);
        }

        @Override
        public Collection<Class> runtime() {
            return null;
        }
    }
}
