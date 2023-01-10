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

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.ServiceLoader;

import com.sun.tdk.jcov.instrument.Modifiers.ModifiersFactory;

class Services {
    private static volatile InstrumentationPlugin PLUGIN;

    private static volatile ModifiersFactory MODIFIERS_FACTORY;

    static synchronized InstrumentationPlugin getPlugin() {
        if (Services.PLUGIN == null) {
            PLUGIN = ServiceLoader.load(InstrumentationPlugin.class).findFirst().orElseGet(() -> {
                try {
                    //for backward compatibility for a non-modular jar
                    return (InstrumentationPlugin)
                            Class.forName("com.sun.tdk.jcov.instrument.asm.ASMInstrumentationPlugin")
                                    .getConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                        NoSuchMethodException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return Services.PLUGIN;
    }

    static synchronized ModifiersFactory getFactory() {
        if (MODIFIERS_FACTORY == null) {
            MODIFIERS_FACTORY = ServiceLoader.load(ModifiersFactory.class).findFirst().orElseGet(() -> {
                try {
                    //for backward compatibility for a non-modular jar
                    return (ModifiersFactory)
                            Class.forName("com.sun.tdk.jcov.instrument.asm.ASMModifiers$ASMModfiersFactory")
                            .getConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                        NoSuchMethodException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return MODIFIERS_FACTORY;
    }
}
