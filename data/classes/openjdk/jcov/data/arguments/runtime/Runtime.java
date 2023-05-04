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

import openjdk.jcov.data.runtime.EntryControl;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;

public class Runtime extends openjdk.jcov.data.runtime.Runtime {

    public static void main(String[] args) {
        init();
    }

    public static class CompletionTask implements Runnable {
        public void run() {
            try {
                Path out = coverageOut();
                if(out != null) {
                    System.out.println("Saving collected coverage to " + out);
                    Coverage.write(Collect.data(), out, serializer());
                } else {
                    System.err.println("No output file specified!");
                }
            } catch (Exception e) {
                throw new RuntimeException("Exception during shutting down.", e);
            }
        }
    }
    static {
        try {
            Collect.data(initialData());
        } catch (Exception e) {
            throw new RuntimeException("Exception during runtime initialization", e);
        }
        addCompletionTask(new CompletionTask());
    }

    private static Coverage initialData() throws IOException, ClassNotFoundException, InvocationTargetException,
            NoSuchMethodException, IllegalAccessException, InstantiationException {
        Path inputFile = openjdk.jcov.data.runtime.Runtime.coverageIn();
        if (inputFile != null) {
            System.out.println("Loading data coverage from " + inputFile);
            return Coverage.read(inputFile, deserializer());
        } else return new Coverage();
    }

    public static void init() {
    }
}
