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
package com.sun.tdk.jcov.instrument.reader;

import com.sun.tdk.jcov.data.FileFormatException;
import com.sun.tdk.jcov.instrument.BasicBlock;
import com.sun.tdk.jcov.instrument.CharacterRangeTable;
import com.sun.tdk.jcov.instrument.DataAbstract.LocationCoords;
import com.sun.tdk.jcov.instrument.DataMethodWithBlocks;
import com.sun.tdk.jcov.instrument.DataRoot;
import com.sun.tdk.jcov.instrument.LocationRef;
import com.sun.tdk.jcov.instrument.InstrumentationOptions.InstrumentationMode;
import com.sun.tdk.jcov.instrument.SimpleBasicBlock;
import com.sun.tdk.jcov.instrument.XmlNames;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 *
 * @author Sergey Borodin
 */
public class DataMethodWithBlocksStAX implements Reader {

    private DataMethodWithBlocks meth;
    private XMLStreamReader parser;
    private ReaderFactory rf;

    public void readData(Object dest) throws FileFormatException {
        meth = (DataMethodWithBlocks) dest;
        try {
            readData();
        } catch (XMLStreamException ex) {
            throw new FileFormatException(ex);
        }
    }

    void readData() throws XMLStreamException, FileFormatException {
        List<BasicBlock> blocks = new LinkedList();
        Map<LocationCoords, List<LocationRef>> refs = new HashMap();//TreeMap();//HashTable!!!
        rf.setProcessingData(refs);
        while (!(parser.getEventType() == XMLStreamConstants.END_ELEMENT
                && XmlNames.METHOD.equals(parser.getLocalName()))) {
            String elem = parser.getLocalName();
            if (XmlNames.BLOCK.equals(elem)) {
                BasicBlock bb;
                InstrumentationMode mode = DataRoot.getInstance(meth.rootId()).getParams().getMode();
                if (mode.equals(InstrumentationMode.BLOCK)) {
                    int start = Integer.parseInt(parser.getAttributeValue(null, XmlNames.START));
                    int end = Integer.parseInt(parser.getAttributeValue(null, XmlNames.END));
                    bb = new SimpleBasicBlock(meth.rootId(), start, end, false);
                } else {
                    bb = new BasicBlock(meth.rootId());
                }
                bb.readDataFrom();
                blocks.add(bb);
            } else if (XmlNames.METHENTER.equals(elem)) { // occures when instrmode == block
                int start = 0;
                String s = parser.getAttributeValue(null, XmlNames.START);
                if (s != null) {
                    start = Integer.parseInt(s);
                }
                int end = 0;
                s = parser.getAttributeValue(null, XmlNames.END);
                if (s != null) {
                    end = Integer.parseInt(s);
                }
                BasicBlock bb = new SimpleBasicBlock(meth.rootId(), start, end, false);
                bb.readDataFrom();
                blocks.add(bb);
            } else if (XmlNames.CRT.equals(elem)) {
                meth.setCharacterRangeTable(new CharacterRangeTable(meth.rootId()));
                meth.getCharacterRangeTable().readDataFrom();
            } else {
                Reader r = rf.getSuperReaderFor(DataMethodWithBlocks.class);
                r.readData(meth);
            }
            parser.nextTag();

        }
        for (LocationCoords c : refs.keySet()) {
            for (BasicBlock b : blocks) {
                if (b.startBCI() == c.start && b.endBCI() == c.end) {
                    for (LocationRef ref : refs.get(c)) {
                        ref.setConcreteLocation(b);
                    }
                    break;
                }
            }
        }

        meth.setBasicBlocks(blocks.toArray(new BasicBlock[blocks.size()]));
    }

    public void setReaderFactory(ReaderFactory r) {
        rf = r;
        parser = ((ReaderFactoryStAX) r).parser;
    }
}
