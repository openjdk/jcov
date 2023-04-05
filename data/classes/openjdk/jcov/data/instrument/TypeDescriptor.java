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
package openjdk.jcov.data.instrument;

import jdk.internal.classfile.CodeBuilder;

import java.util.function.BiConsumer;

/**
 * Contains necessary type information for code generation, etc. Should be extended as needed with the actual code
 * generation logic.
 */
public class TypeDescriptor {
    private final String id;
    private final String cls;
    private final BiConsumer<CodeBuilder, Integer> load;
    private final boolean longOrDouble;
    private final boolean isPrimitive;

    public static String toVMClassName(Class cls) {
        return toVMClassName(cls.getName());
    }
    public static String toVMClassName(String className) {
        return className.replace('.','/');
    }
    public TypeDescriptor(String id, Class cls, BiConsumer<CodeBuilder, Integer> load) {
        this(id, toVMClassName(cls), load);
    }
    public TypeDescriptor(String id, String cls, BiConsumer<CodeBuilder, Integer> load) {
        this(id, cls, load, false, false);
    }
    public TypeDescriptor(String id, Class cls, BiConsumer<CodeBuilder, Integer>load, boolean longOrDouble, boolean isPrimitive) {
        this(id, cls.getName(), load, longOrDouble, isPrimitive);
    }
    public TypeDescriptor(String id, String cls, BiConsumer<CodeBuilder, Integer> load, boolean longOrDouble, boolean isPrimitive) {
        this.id = id;
        this.cls = cls;
        this.load = load;
        this.longOrDouble = longOrDouble;
        this.isPrimitive = isPrimitive;
    }

    public String id() {
        return id;
    }

    public String cls() { return cls; }

    public String vmCls() { return toVMClassName(cls); }

    public void load(CodeBuilder code, int index) {
        load.accept(code, index);
    }

    public boolean isLongOrDouble() {
        return longOrDouble;
    }

    public boolean isPrimitive() {
        return isPrimitive;
    }
}
