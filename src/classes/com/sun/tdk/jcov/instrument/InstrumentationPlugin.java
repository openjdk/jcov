/*
 * Copyright (c) 2022 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.jcov.instrument;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A plugin's {@link #instrument(Collection, ClassLoader, BiConsumer, InstrumentationParams)} could be used
 * multiple times to instrument additional bytecode. A plugin is supposed to accumulate the the, which later
 * is supposed to be used through {@link #complete()} method.
 */
public interface InstrumentationPlugin {

    /**
     * An identifier for template artifact.
     */
    String TEMPLATE_ARTIFACT = "template.xml";
    String CLASS_EXTENTION = ".class";
    String MODULE_INFO_CLASS = "module-info.class";

    static InstrumentationPlugin getPlugin() {
        return Services.getPlugin();
    }

    /**
     *
     * @param resources A collection of resource paths relative to root of the class hierarchy. '/' is supposed to be
     *                  used as a file separator.
     * @param loader
     * @param saver
     * @param parameters
     * @throws Exception
     */
    void instrument(Collection<String> resources, ClassLoader loader,
                    BiConsumer<String, byte[]> saver,
                    InstrumentationParams parameters) throws Exception;

    /**
     * Completes the instrumentation proccess and returns a map of instrumentation artifacts.
     * @see #TEMPLATE_ARTIFACT
     *
     * @return the artifact map. The artifacts are identifiable by a string. The artifacts are consumers of
     * OutputStream's.
     * @throws Exception
     */
    Map<String, Consumer<OutputStream>> complete() throws Exception;

    default boolean isClass(String resource) {
        return resource.endsWith(CLASS_EXTENTION) && !resource.endsWith(MODULE_INFO_CLASS);
    }
}
