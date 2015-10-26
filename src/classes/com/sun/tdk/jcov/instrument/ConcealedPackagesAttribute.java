/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import org.objectweb.asm.*;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Alexey Fedorchenko
 */
public class ConcealedPackagesAttribute extends Attribute {
    private final Set<String> packages;

    ConcealedPackagesAttribute(Set<String> packages) {
        super("ConcealedPackages");
        this.packages = packages;
    }

    public ConcealedPackagesAttribute() {
        this(null);
    }

    @Override
    protected Attribute read(ClassReader cr,
                             int off,
                             int len,
                             char[] buf,
                             int codeOff,
                             Label[] labels)
    {
        // package count
        int package_count = cr.readUnsignedShort(off);
        off += 2;

        // packages
        Set<String> packages = new HashSet<String>();
        for (int i=0; i<package_count; i++) {
            String pkg = cr.readUTF8(off, buf).replace('/', '.');
            packages.add(pkg);
            off += 2;
        }

        return new ConcealedPackagesAttribute(packages);
    }

    @Override
    protected ByteVector write(ClassWriter cw,
                               byte[] code,
                               int len,
                               int maxStack,
                               int maxLocals)
    {
        ByteVector attr = new ByteVector();

        // package_count
        attr.putShort(0);

        // packages
        /*for (String pak : packages){
            String rp = pak.replace('.', '/');
            attr.putShort(cw.newUTF8(""));
        }*/

        return attr;
    }

}
