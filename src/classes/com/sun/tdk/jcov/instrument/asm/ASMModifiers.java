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
package com.sun.tdk.jcov.instrument.asm;

import com.sun.tdk.jcov.instrument.Modifiers;
import com.sun.tdk.jcov.instrument.XmlNames;

import static org.objectweb.asm.Opcodes.*;

public class ASMModifiers implements Modifiers {
    private final int access;

    public ASMModifiers(int access) {
        this.access = access;
    }

    @Override
    public int access() { return access & ACCESS_MASK; }

    @Override
    public boolean is(int flag) { return (access & flag) != 0; }

    @Override
    public boolean isPublic() { return is(ACC_PUBLIC); }

    @Override
    public boolean isPrivate() { return is(ACC_PRIVATE); }

    @Override
    public boolean isProtected() { return is(ACC_PROTECTED); }

    @Override
    public boolean isAbstract() { return is(ACC_ABSTRACT); }

    @Override
    public boolean isFinal() { return is(ACC_FINAL); }

    @Override
    public boolean isSynthetic() { return is(ACC_SYNTHETIC); }

    @Override
    public boolean isStatic() { return is(ACC_STATIC); }

    @Override
    public boolean isInterface() { return is(ACC_INTERFACE); }

    @Override
    public boolean isSuper() { return is(ACC_SUPER); }

    @Override
    public boolean isNative() { return is(ACC_NATIVE); }

    @Override
    public boolean isDeprecated() { return is(ACC_DEPRECATED); }

    @Override
    public boolean isSynchronized() { return is(ACC_SYNCHRONIZED); }

    @Override
    public boolean isVolatile() { return is(ACC_VOLATILE); }

    @Override
    public boolean isBridge() { return is(ACC_BRIDGE); }

    @Override
    public boolean isVarargs() { return is(ACC_VARARGS); }

    @Override
    public boolean isTransient() { return is(ACC_TRANSIENT); }

    @Override
    public boolean isStrict() { return is(ACC_STRICT); }

    @Override
    public boolean isAnnotation() { return is(ACC_ANNOTATION); }

    @Override
    public boolean isEnum() { return is(ACC_ENUM); }

    public static final int ACCESS_MASK = ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED | ACC_FINAL | ACC_STATIC | ACC_SYNCHRONIZED
            | ACC_VOLATILE | ACC_BRIDGE | ACC_VARARGS | ACC_TRANSIENT | ACC_NATIVE | ACC_ABSTRACT | ACC_INTERFACE
            | ACC_STRICT | ACC_ANNOTATION | ACC_ENUM | ACC_SYNTHETIC | ACC_SUPER | ACC_DEPRECATED;

    public static ASMModifiers parse(String[] modifiers) {
        int access = 0;
        for (String flag : modifiers) {
            if (flag.contains(XmlNames.A_PUBLIC)) access |= ACC_PUBLIC;
            if (flag.contains(XmlNames.A_PRIVATE)) access |= ACC_PRIVATE;
            if (flag.contains(XmlNames.A_PROTECTED)) access |= ACC_PROTECTED;
            if (flag.contains(XmlNames.A_STATIC)) access |= ACC_STATIC;
            if (flag.contains(XmlNames.A_FINAL)) access |= ACC_FINAL;
            if (flag.contains(XmlNames.A_VOLATILE)) access |= ACC_VOLATILE;
            if (flag.contains(XmlNames.A_BRIDGE)) access |= ACC_BRIDGE;
            if (flag.contains(XmlNames.A_VARARGS)) access |= ACC_VARARGS;
            if (flag.contains(XmlNames.A_TRANSIENT)) access |= ACC_TRANSIENT;
            if (flag.contains(XmlNames.A_NATIVE)) access |= ACC_NATIVE;
            if (flag.contains(XmlNames.A_INTERFACE) || flag.contains(XmlNames.A_DEFENDER_METH)) access |= ACC_INTERFACE;
            if (flag.contains(XmlNames.A_ABSTRACT)) access |= ACC_ABSTRACT;
            if (flag.contains(XmlNames.A_STRICT)) access |= ACC_STRICT;
            if (flag.contains(XmlNames.A_ANNOTATION)) access |= ACC_ANNOTATION;
            if (flag.contains(XmlNames.A_ENUM)) access |= ACC_ENUM;
            if (flag.contains(XmlNames.A_SYNTHETIC)) access |= ACC_SYNTHETIC;
            if (flag.contains(XmlNames.A_SYNCHRONIZED)) access |= ACC_SYNCHRONIZED;
        }
        return new ASMModifiers(access);
    }
}

