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
package openjdk.jcov.data.runtime;

import openjdk.jcov.data.Env;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Runtime {

    private static volatile Thread exitHook = null;

    static {
        installExitHook();
    }

    private static List<Runnable> completionTasks = new ArrayList<>(1);

    protected static void addCompletionTask(Runnable task) {
        completionTasks.add(task);
    }

    public static synchronized void complete() {
        for(Runnable r : completionTasks) {
            r.run();
        }
    }

    public static Function<Object, String> serializer() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        return Env.getSPIEnv(Serializer.SERIALIZER, Serializer.TO_STRING);
    }

    public static Function<String, Object> deserializer() throws ClassNotFoundException,
            InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        return Env.getSPIEnv(Deserializer.DESERIALIZER, Deserializer.SELF);
    }

    public static Path coverageIn() {
        return Env.getPathEnv(CoverageData.COVERAGE_IN, null);
    }

    public static Path coverageOut() {
        return Env.getPathEnv(CoverageData.COVERAGE_OUT, null);
    }

    public static synchronized void installExitHook() {
        clearExitHook();
        exitHook = new Thread(Runtime::complete);
        java.lang.Runtime.getRuntime().addShutdownHook(exitHook);
    }

    public static synchronized void clearExitHook() {
        if (exitHook != null) java.lang.Runtime.getRuntime().removeShutdownHook(exitHook);
    }

    public static void init() {
    }
}
