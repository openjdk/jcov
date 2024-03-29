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

import com.sun.tdk.jcov.data.FileFormatException;
import com.sun.tdk.jcov.instrument.reader.Reader;
import com.sun.tdk.jcov.instrument.reader.ReaderFactory;

/**
 * CharacterRangeTableAttribute
 *
 *
 *
 * @author Robert Field
 */
public class CharacterRangeTable {

    public static class CRTEntry extends LocationConcrete {

        public final int char_start;
        public final int char_end;
        public final int flags;
        public static final int CRT_STATEMENT = 0x0001;
        public static final int CRT_BLOCK = 0x0002;
        public static final int CRT_ASSIGNMENT = 0x0004;
        public static final int CRT_FLOW_CONTROLLER = 0x0008;
        public static final int CRT_FLOW_TARGET = 0x0010;
        public static final int CRT_INVOKE = 0x0020;
        public static final int CRT_CREATE = 0x0040;
        public static final int CRT_BRANCH_TRUE = 0x0080;
        public static final int CRT_BRANCH_FALSE = 0x0100;

        public CRTEntry(final int rootId,
                final int start_pc,
                final int end_pc,
                final int char_start,
                final int char_end,
                final int flags) {
            super(rootId, start_pc, end_pc);
            this.char_start = char_start;
            this.char_end = char_end;
            this.flags = flags;
        }

        /**
         * XML Generation
         */
        public String kind() {
            return XmlNames.RANGE;
        }

        public void xmlAttrs(XmlContext ctx) {
            super.xmlAttrs(ctx);
            if ((flags & CRT_STATEMENT) != 0) {
                ctx.attr(XmlNames.A_STATEMENT, true);
            }
            if ((flags & CRT_BLOCK) != 0) {
                ctx.attr(XmlNames.A_BLOCK, true);
            }
            if ((flags & CRT_ASSIGNMENT) != 0) {
                ctx.attr(XmlNames.A_ASSIGNMENT, true);
            }
            if ((flags & CRT_FLOW_CONTROLLER) != 0) {
                ctx.attr(XmlNames.A_CONTROLLER, true);
            }
            if ((flags & CRT_FLOW_TARGET) != 0) {
                ctx.attr(XmlNames.A_TARGET, true);
            }
            if ((flags & CRT_INVOKE) != 0) {
                ctx.attr(XmlNames.A_INVOKE, true);
            }
            if ((flags & CRT_CREATE) != 0) {
                ctx.attr(XmlNames.A_CREATE, true);
            }
            if ((flags & CRT_BRANCH_TRUE) != 0) {
                ctx.attr(XmlNames.A_BRANCHTRUE, true);
            }
            if ((flags & CRT_BRANCH_FALSE) != 0) {
                ctx.attr(XmlNames.A_BRANCHFALSE, true);
            }
        }

        private void xmlPos(XmlContext ctx, int char_pos) {
            ctx.indent();
            ctx.format("<" + XmlNames.CRT_POS + " " + XmlNames.CRT_LINE + "='%d' "
                    + XmlNames.CRT_COL + "='%d'/>", char_pos >> 10, char_pos & 0x3FF);
            ctx.println();
        }

        void xmlBody(XmlContext ctx) {
            xmlPos(ctx, char_start);
            xmlPos(ctx, char_end);
        }
    }
    public int length;
    public CRTEntry[] entries;
    private int rootId;

    public int getRootId() {
        return rootId;
    }

    public void setRootId(int rootId) {
        this.rootId = rootId;
    }

    /**
     * Creates a new instance of CharacterRangeTableAttribute
     */
    public CharacterRangeTable(int rootId) {
        this(rootId, 0, new CRTEntry[0]);
    }

    public CharacterRangeTable(int rootId, int length, CRTEntry[] entries) {
        this.length = length;
        this.entries = entries;
        this.rootId = rootId;
    }

    public CRTEntry[] getEntries() {
        return entries;
    }

    public void setEntries(CRTEntry[] entries) {
        this.entries = entries;
    }

    /*
     * return entry with given pc and flag set,
     * if there are more then one found , return first in source code
     */
    CRTEntry getEntry(int pc, int flag) {
        CRTEntry result = null;
        for (CRTEntry entry : getEntries()) {
            if (entry.startBCI() == pc
                    && (entry.flags & flag) != 0) {
                result = (result == null) ? entry
                        : entry.char_start < result.char_start ? entry : result;
            }
        }
        return result;
    }

    /**
     * XML Generation
     *
     * Since there is no multiple inheritance, we aren't a DataAbstract, but
     * we'll fake it
     */
    public void xmlGen(XmlContext ctx) {
        ctx.indent();
        ctx.println("<" + XmlNames.CRT + ">");
        ctx.incIndent();
        for (CRTEntry entry : entries) {
            entry.xmlGen(ctx);
        }
        ctx.decIndent();
        ctx.indent();
        ctx.println("</" + XmlNames.CRT + ">");
    }

    public void readDataFrom() throws FileFormatException {
        ReaderFactory rf = DataRoot.getInstance(rootId).getReaderFactory();
        Reader r = rf.getReaderFor(this);
        r.readData(this);
    }


    public int getPos(int line, int col) {
        return line << 10 | col;
    }
}