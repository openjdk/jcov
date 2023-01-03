/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.jcov.instrument.plugin;

import com.sun.tdk.jcov.instrument.InstrumentationParams;
import com.sun.tdk.jcov.instrument.InstrumentationPlugin;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TestPlugin implements InstrumentationPlugin {
    private final static List<String> processed = new ArrayList<>();
    private final static AtomicBoolean completed = new AtomicBoolean(false);

    @Override
    public void instrument(Collection<String> resources, ClassLoader loader, BiConsumer<String, byte[]> saver,
                           InstrumentationParams parameters) throws IOException {
        for(String r : resources) {
            processed.add(r);
            saver.accept(r, loader.getResourceAsStream(r).readAllBytes());
        };
    }

    @Override
    public Map<String, Consumer<OutputStream>> complete() throws Exception {
        completed.set(true);
        return Map.of();
    }

    public static void install(InstrumentationPlugin plugin) {
//        InstrumentationPlugin.setPlugin(plugin);
        clear();
    }

    public static List<String> getProcessed() {
        return processed;
    }

    public static boolean isCompleted() {
        return completed.get();
    }

    public static void clear() {
        processed.clear();
        completed.set(false);
    }
}
