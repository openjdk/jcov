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
package openjdk.jcov.data.serialization;

import openjdk.jcov.data.arguments.runtime.Implantable;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Serializes an enum into its name.
 */
public class EnumSerializer implements Function<Object, String>, Implantable {

    private final String defaultValue;

    public EnumSerializer(String value) {
        defaultValue = value;
    }

    public EnumSerializer() {
        this("NOT_AN_ENUM");
    }

    @Override
    public String apply(Object anEnum) {
        if (anEnum instanceof Enum)
            return ((Enum) anEnum).name();
        else
            return defaultValue;
    }

    public Collection<Class> runtime() {
        return List.of(EnumSerializer.class);
    }
}
