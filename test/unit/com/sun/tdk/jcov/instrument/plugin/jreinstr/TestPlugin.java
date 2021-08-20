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
package com.sun.tdk.jcov.instrument.plugin.jreinstr;

import com.sun.tdk.jcov.instrument.InstrumentationPlugin;
import org.objectweb.asm.MethodVisitor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

public class TestPlugin implements InstrumentationPlugin {

    public static AtomicInteger calledTimes = new AtomicInteger(0);
    public static AtomicInteger savedTimes = new AtomicInteger(0);
    public static Path rt;

    static {
        try {
            rt = JREInstrTest.createRtJar("plugin-rt-", Collect.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void reset() {
        calledTimes.set(0);
        savedTimes.set(0);
    }

    @Override
    public MethodVisitor methodVisitor(int access, String owner, String name, String desc, MethodVisitor visitor) {
        calledTimes.incrementAndGet();
        return visitor;
    }

    @Override
    public void instrumentationComplete() throws Exception {
        savedTimes.incrementAndGet();
    }

    @Override
    public Path runtime() {
        return rt;
    }

    @Override
    public String collectorPackage() {
        return Collect.class.getPackage().getName();
    }
}