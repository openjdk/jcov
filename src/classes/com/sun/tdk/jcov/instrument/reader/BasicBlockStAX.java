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
package com.sun.tdk.jcov.instrument.reader;

import com.sun.tdk.jcov.data.FileFormatException;
import com.sun.tdk.jcov.instrument.BasicBlock;
import com.sun.tdk.jcov.instrument.Constants;
import com.sun.tdk.jcov.instrument.DataBlockCatch;
import com.sun.tdk.jcov.instrument.DataBlockFallThrough;
import com.sun.tdk.jcov.instrument.DataBlockMethEnter;
import com.sun.tdk.jcov.instrument.DataBranchCond;
import com.sun.tdk.jcov.instrument.DataBranchGoto;
import com.sun.tdk.jcov.instrument.DataBranchSwitch;
import com.sun.tdk.jcov.instrument.DataExit;
import com.sun.tdk.jcov.instrument.DataExitSimple;
import com.sun.tdk.jcov.instrument.XmlNames;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 *
 * @author Sergey Borodin
 */
public class BasicBlockStAX implements Reader {

    private BasicBlock block;
    private XMLStreamReader parser;
    private ReaderFactory rf;

    public void readData(Object dest) throws FileFormatException {
        block = (BasicBlock) dest;
        try {
            readData();
        } catch (XMLStreamException ex) {
            throw new FileFormatException(ex);
        }
    }

    void readData() throws XMLStreamException, FileFormatException {

        Reader r = rf.getSuperReaderFor(BasicBlock.class);
        r.readData(block);

        parser.nextTag();
        while (!(parser.getEventType() == XMLStreamConstants.END_ELEMENT
                && parser.getLocalName() == XmlNames.BLOCK)) {
            String elem = parser.getLocalName();
            if (isXMLDataExit(elem)) {
                block.setExit(instantiateExit());
            } else {
                if (elem == XmlNames.METHENTER) {
                    DataBlockMethEnter enter = new DataBlockMethEnter(block.rootId(), 0, false, 0);
                    enter.readDataFrom();
                    block.add(enter);
                } else if (elem == XmlNames.FALL) {
                    DataBlockFallThrough fall = new DataBlockFallThrough(block.rootId(), 0, false, 0);
                    fall.readDataFrom();
                    block.add(fall);
                } else if (elem == XmlNames.CATCH) {
                    DataBlockCatch ctch = new DataBlockCatch(block.rootId(), 0, false, 0);
                    ctch.readDataFrom();
                    block.add(ctch);
                }
                parser.nextTag();
            }
//            parser.nextTag();//closing internal elem
            parser.nextTag();//next elem, may be </bl>
        }
    }

    private boolean isXMLDataExit(String elem) {
        if (elem == XmlNames.EXIT
                || elem == XmlNames.SWITHCH
                || elem == XmlNames.GOTO
                || elem == XmlNames.BRANCH) {

            return true;
        }
        return false;
    }

    private DataExit instantiateExit() throws XMLStreamException, FileFormatException {

        DataExit dExit = null;
        String elem = parser.getLocalName();
        int start = Integer.parseInt(parser.getAttributeValue(null, XmlNames.START));
        int end = Integer.parseInt(parser.getAttributeValue(null, XmlNames.END));
        if (elem == XmlNames.EXIT) {
            int opCode = Constants.getOpCode(parser.getAttributeValue(null, XmlNames.OPCODE));
            dExit = new DataExitSimple(block.rootId(), start, end, opCode);
            parser.nextTag();
            return dExit;
        } else if (elem == XmlNames.SWITHCH) {
            dExit = new DataBranchSwitch(block.rootId(), start, end);
        } else if (elem == XmlNames.GOTO) {
            dExit = new DataBranchGoto(block.rootId(), start, end);
        } else if (elem == XmlNames.BRANCH) {
            dExit = new DataBranchCond(block.rootId(), start, end);
        }

        dExit.readDataFrom();

        return dExit;
    }

    public void setReaderFactory(ReaderFactory r) {
        rf = r;
        parser = ((ReaderFactoryStAX) r).parser;
    }
}
