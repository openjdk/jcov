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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * DataExitSimple
 *
 * @author Robert Field
 */
public class DataExitSimple extends DataExit {

    int opcode;

    /**
     * Creates a new instance of DataExitSimple
     */
    public DataExitSimple(int rootId, int bciStart, int bciEnd, int opcode) {
        super(rootId, bciStart, bciEnd);
        this.opcode = opcode;
    }

    public String opcodeName() {
        return Constants.opcNames[opcode];
    }

    /**
     * XML Generation
     */
    public String kind() {
        return XmlNames.EXIT;
    }

    @Override
    void xmlGen(XmlContext cxt) {
        xmlGenBodiless(cxt);
    }

    @Override
    void xmlAttrs(XmlContext ctx) {
        super.xmlAttrs(ctx);
        ctx.attr(XmlNames.OPCODE, opcodeName());
    }

//    void readDataFrom(XMLStreamReader parser, Map<LocationCoords, List<LocationRef>> refs,
//            InstrumentationMode mode) throws XMLStreamException {
////        super.readDataFrom(parser, refs, mode);
//        parser.nextTag(); //end exit
//    }
    @Override
    public Iterator<DataBlock> getIterator() {
        return new Iterator<DataBlock>() {
            public boolean hasNext() {
                return false;
            }

            public DataBlock next() {
                throw new NoSuchElementException();
            }

            public void remove() {
            }
        };
    }

    void writeObject(DataOutput out) throws IOException {
        super.writeObject(out);
        out.write(opcode);
    }

    DataExitSimple(int rootId, DataInput in) throws IOException {
        super(rootId, in);
        opcode = in.readByte();
        if (opcode < 0) {
            opcode += 256;
        }
    }
}