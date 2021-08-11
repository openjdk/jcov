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
import openjdk.jcov.data.Instrument;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import static openjdk.jcov.data.Instrument.JCOV_DATA_ENV_PREFIX;
import static openjdk.jcov.data.arguments.instrument.Plugin.ARGUMENTS_PREFIX;

public class Saver implements JCovSaver {

    public static final String RESULT_FILE = JCOV_DATA_ENV_PREFIX + ARGUMENTS_PREFIX + "result";
    public static final String SERIALIZER =
            Instrument.JCOV_DATA_ENV_PREFIX + ARGUMENTS_PREFIX + ".serializer";

    private Path resultFile;
    private Function<Object, String> serializer;

    public Saver() throws
            ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException,
            IllegalAccessException {
        resultFile = Env.getPathEnv(RESULT_FILE, Paths.get("result.lst"));
        serializer = Env.getSPIEnv(SERIALIZER, Object::toString);
    }

    public Saver resultFile(Path resultFile) {
        this.resultFile = resultFile;
        return this;
    }

    public Saver serializer(Function<Object, String> serializer) {
        this.serializer = serializer;
        return this;
    }

    public void saveResults() {
        try {
            Coverage.write(Collect.data, resultFile, serializer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
