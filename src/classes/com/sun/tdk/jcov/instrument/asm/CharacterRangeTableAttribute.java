/*
 * Copyright (c) 2014, 2022 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tdk.jcov.instrument.CharacterRangeTable;
import com.sun.tdk.jcov.instrument.CharacterRangeTable.CRTEntry;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ByteVector;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;

/**
 * CharacterRangeTableAttribute
 *
 *
 *
 * @author Robert Field
 */
public class CharacterRangeTableAttribute extends Attribute {

    CharacterRangeTable crt;

    CharacterRangeTableAttribute(CharacterRangeTable crt) {
        super("CharacterRangeTable");
        this.crt = crt;
    }

    @Override
    public boolean isUnknown() {
        return false;
    }

    void put(CRTEntry entry, ByteVector bv) {
        bv.putShort(entry.startBCI());
        bv.putShort(entry.endBCI());
        bv.putInt(entry.char_start);
        bv.putInt(entry.char_end);
        bv.putShort(entry.flags);
    }

    @Override
    protected Attribute read(ClassReader cr, int off, int len,
            char[] buf, int codeOff, Label[] labels) {
        int length = cr.readShort(off);
        CRTEntry[] entries = new CRTEntry[length];
        for (int i = 0; i < length; ++i) {
            int eoff = off + 2 + (i * 14);
            int start_pc = cr.readShort(eoff + 0);
            int end_pc = cr.readShort(eoff + 2);
            int char_start = cr.readInt(eoff + 4);
            int char_end = cr.readInt(eoff + 8);
            int flags = cr.readShort(eoff + 12);
            entries[i] = new CRTEntry(crt.getRootId(), start_pc, end_pc, char_start, char_end, flags);
        }
        return new CharacterRangeTableAttribute(new CharacterRangeTable(crt.getRootId(), length, entries));
    }

    @Override
    protected ByteVector write(ClassWriter cw, byte[] code, int len,
            int maxStack, int maxLocals) {
        ByteVector bv = new ByteVector();
        bv.putShort(crt.length);
        for (CRTEntry entry : crt.entries) {
            put(entry, bv);
        }
        return bv;
    }

}
