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
package openjdk.jcov.data.arguments.runtime;

import openjdk.jcov.data.runtime.CoverageData;
import openjdk.jcov.data.Env;
import openjdk.jcov.data.runtime.EntryControl;
import openjdk.jcov.data.runtime.Serializer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Calls to this class' collect(...) methods are injected in the beginning of every instrumented method.
 */
public class Collect {
    /**
     * Property name prefix for all properties used by this plugin. The property names are started with
     * <code>Instrument.JCOV_DATA_ENV_PREFIX + ARGUMENTS_PREFIX</code>
     */
    public static final String ARGUMENTS_PREFIX = "args.";

    static volatile Coverage data;
    private static volatile Path outputFile;
    private static volatile Function<Object, String> serializer;

    static {
            try {
                Path coverageFile = Env.getPathEnv(CoverageData.COVERAGE_IN, null);
                if(coverageFile != null)
                    data = Coverage.read(coverageFile);
                else
                    data = new Coverage();
                outputFile = Env.getPathEnv(CoverageData.COVERAGE_OUT, Paths.get("coverage.lst"));
                serializer = Env.getSPIEnv(Serializer.SERIALIZER, Serializer.TO_STRING);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                    InvocationTargetException | InstantiationException e) {
                throw new RuntimeException(e);
            }
    }

    public static Coverage data() {return data;}

    public static void data(Coverage data) {Collect.data = data;}

    public static void outputFile(Path outputFile) {
        Collect.outputFile = outputFile;
    }

    public static void serializer(Function<Object, String> serializer) {
        Collect.serializer = serializer;
    }

    public static synchronized void template(String owner, String name, String desc) {
        data.add(owner, name + desc, null);
    }

    private final static EntryControl entryControl = new EntryControl();

    public static synchronized void collect(String owner, String name, String desc, Object... params) {
//        keep these lines, it is useful for debugging in hard cases
//        System.out.println("Collect.collect has been called with");
//        System.out.printf("%s.%s%s: %s\n", owner, name, desc, (params == null) ? "null" :
//                Arrays.stream(params).map(Object::getClass).map(Class::getName)
//                        .collect(java.util.stream.Collectors.joining(",")));
//        System.out.println(Arrays.stream(params).map(java.util.Objects::toString)
//                .collect(Collectors.joining(",")));
        Runtime.init();
        if (!entryControl.enter()) return;
        try {
            if (!data.writing)
                data.add(owner, name + desc, Arrays.stream(params)
                    .collect(Collectors.toList()));
        } finally {
            entryControl.exit();
        }
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

    public static void save() throws IOException {
        System.out.println("Saving the data info " + outputFile);
        Coverage.write(data, outputFile, serializer);
    }
}
