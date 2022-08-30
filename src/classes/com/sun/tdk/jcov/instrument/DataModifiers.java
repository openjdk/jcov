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

import org.objectweb.asm.Opcodes;

public class DataModifiers implements Modifiers {
    private final int access;

    public DataModifiers(int access) {
        this.access = access;
    }

    public int access() {
        return access;
    }

    public boolean is(int flag) {
        return (access & flag) != 0;
    }

    @Override
    public boolean isPublic() {
        return is(Opcodes.ACC_PUBLIC);
    }

    @Override
    public boolean isPrivate() {
        return is(Opcodes.ACC_PRIVATE);
    }

    @Override
    public boolean isProtected() { return is(Opcodes.ACC_PROTECTED); }

    @Override
    public boolean isAbstract() {
        return is(Opcodes.ACC_ABSTRACT);
    }

    @Override
    public boolean isFinal() {
        return is(Opcodes.ACC_FINAL);
    }

    @Override
    public boolean isSynthetic() {
        return is(Opcodes.ACC_SYNTHETIC);
    }

    @Override
    public boolean isStatic() {
        return is(Opcodes.ACC_STATIC);
    }

    @Override
    public boolean isInterface() {
        return is(Opcodes.ACC_INTERFACE);
    }

    @Override
    public boolean isSuper() {
        return is(Opcodes.ACC_SUPER);
    }

    @Override
    public boolean isNative() {
        return is(Opcodes.ACC_NATIVE);
    }

    @Override
    public boolean isDeprecated() { return is(Opcodes.ACC_DEPRECATED); }
}
