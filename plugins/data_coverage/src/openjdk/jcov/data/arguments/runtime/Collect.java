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

import openjdk.jcov.data.Env;
import openjdk.jcov.data.arguments.instrument.Plugin;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.ALOAD;

/**
 * Calls to this class' collect(...) methods are injected in the beginning of every instrumented method.
 */
public class Collect {
    /**
     * Property name prefix for all properties used by this plugin. The property names are started with
     * <code>Instrument.JCOV_DATA_ENV_PREFIX + ARGUMENTS_PREFIX</code>
     */
    public static final String ARGUMENTS_PREFIX = "args.";
    /**
     * Name of a property which contains path of the template file.
     */
    public static final String COVERAGE_FILE = Env.JCOV_DATA_ENV_PREFIX + ARGUMENTS_PREFIX +
            "coverage";

    /**
     * Name of a property containing a class name of a class of type <code>Function<Object, String></code> which will
     * be used during the serialization. <code>Object::toString</code> is used by default.
     */
    public static final String SERIALIZER = Env.JCOV_DATA_ENV_PREFIX +
            Collect.ARGUMENTS_PREFIX + "serializer";

    static final Coverage data;
    private final static Serializer serializer;

    static{
        try {
            Path coverageFile = Env.getPathEnv(COVERAGE_FILE, Paths.get("template.lst"));
            data = Coverage.read(coverageFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        try {
            serializer = wrap(Env.getSPIEnv(SERIALIZER, Object::toString));
        } catch (ClassNotFoundException|NoSuchMethodException|IllegalAccessException|InvocationTargetException|InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    private static Serializer wrap(Function<Object, String> function) {
        if(function instanceof Serializer)
            return (Serializer) function;
        else
            return new Saver.NoRuntimeSerializer(function);
    }

    public static synchronized void collect(String owner, String name, String desc, Object... params) {
//        keep these lines, it is useful for debugging in hard cases
//        System.out.printf("%s.%s%s: %s\n", owner, name, desc, (params == null) ? "null" :
//                Arrays.stream(params).map(Object::getClass).map(Class::getName)
//                        .collect(java.util.stream.Collectors.joining(",")));
//        System.out.println(Arrays.stream(params).map(Objects::toString)
//                .collect(Collectors.joining(",")));
        data.add(owner, name + desc, Arrays.stream(params).map(serializer::apply)
                .collect(Collectors.toList()));
    }

    static int countParams(String desc) {
        if(!desc.startsWith("(")) throw new IllegalArgumentException("Not a method descriptor: " + desc);
        int pos = 1;
        int count = 0;
        while(desc.charAt(pos) != ')') {
            char next = desc.charAt(pos);
            if(next == 'L') {
                int l = pos;
                pos = desc.indexOf(";", pos) + 1;
                count++;
            } else if(next == '[') {
                //TODO can we do better?
                count++;
                if(desc.charAt(pos + 1) == 'L') pos = desc.indexOf(";", pos) + 1;
                else pos = pos + 2;
            } else {
                count++;
                pos++;
            }
        }
        return count;
    }

    public static void clearData() {
        data.coverage().clear();
    }
}
