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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Alexey Fedorchenko
 */
public class HashesAttribute extends Attribute {
    private String algorithm;
    private Map<String, String> nameToHash;


    HashesAttribute(String algorithm, Map<String, String> nameToHash) {
        super("Hashes");
        this.algorithm = algorithm;
        this.nameToHash = nameToHash;
    }

    public HashesAttribute() {
        this(null, null);
    }

    @Override
    protected Attribute read(ClassReader cr,
                             int off,
                             int len,
                             char[] buf,
                             int codeOff,
                             Label[] labels)
    {
        String algorithm = cr.readUTF8(off, buf);
        off += 2;

        int hash_count = cr.readUnsignedShort(off);
        off += 2;

        Map<String, String> map = new HashMap<String, String>();
        for (int i=0; i<hash_count; i++) {
            String dn = cr.readUTF8(off, buf);
            String hash = cr.readUTF8(off, buf);
            map.put(dn, hash);
            off += 2;
        }

        return new HashesAttribute(algorithm, map);
    }

    @Override
    protected ByteVector write(ClassWriter cw,
                               byte[] code,
                               int len,
                               int maxStack,
                               int maxLocals)
    {
        ByteVector attr = new ByteVector();

        int index = cw.newUTF8(algorithm);
        attr.putShort(index);

        Set<String> names = nameToHash.keySet();
        attr.putShort(names.size());

        for (String dn : names) {
            attr.putShort(cw.newUTF8(""));
            attr.putShort(cw.newUTF8(""));
        }

        return attr;
    }
}
