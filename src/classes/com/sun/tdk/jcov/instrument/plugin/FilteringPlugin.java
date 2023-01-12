/*
 * Copyright (c) 2023 Oracle and/or its affiliates. All rights reserved.
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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class FilteringPlugin extends ProxyInstrumentationPlugin {
    private final Predicate<String> filter;

    public FilteringPlugin(InstrumentationPlugin inner, Predicate<String> filter) {
        super(inner);
        this.filter = filter;
    }

    @Override
    public void instrument(Collection<String> resources, ClassLoader loader,
                           BiConsumer<String, byte[]> saver, InstrumentationParams parameters) throws Exception {
        List<String> accepted = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        resources.forEach(r -> {
            if (filter.test(r)) accepted.add(r);
            else rejected.add(r);
        });
        getInner().instrument(accepted, loader, saver, parameters);
        rejected.forEach(c -> {
            try (InputStream in = loader.getResourceAsStream(c)) {
                saver.accept(c, in.readAllBytes());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
}
