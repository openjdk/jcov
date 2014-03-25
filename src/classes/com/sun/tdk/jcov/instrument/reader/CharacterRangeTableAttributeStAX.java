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
import com.sun.tdk.jcov.instrument.CharacterRangeTableAttribute;
import com.sun.tdk.jcov.instrument.CharacterRangeTableAttribute.CRTEntry;
import com.sun.tdk.jcov.instrument.XmlNames;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 *
 * @author Sergey Borodin
 */
public class CharacterRangeTableAttributeStAX implements Reader {

    CharacterRangeTableAttribute crt;
    private XMLStreamReader parser;

    public void readData(Object dest) throws FileFormatException {
        crt = (CharacterRangeTableAttribute) dest;
        try {
            readData();
        } catch (XMLStreamException ex) {
            throw new FileFormatException(ex);
        }
    }

    void readData() throws XMLStreamException {
        parser.nextTag();
        List<CRTEntry> entriesList = new ArrayList();
        while (!(parser.getEventType() == XMLStreamConstants.END_ELEMENT
                && parser.getLocalName() == XmlNames.CRT)) {
            String elem = parser.getLocalName();
            if (elem == XmlNames.RANGE) {
                int flags = getFlags(parser);
                int start = Integer.parseInt(parser.getAttributeValue(null, XmlNames.START));
                int end = Integer.parseInt(parser.getAttributeValue(null, XmlNames.END));

                parser.nextTag();//come into <pos
                int ch_start = crt.getPos(Integer.parseInt(parser.getAttributeValue(null, XmlNames.CRT_LINE)),
                        Integer.parseInt(parser.getAttributeValue(null, XmlNames.CRT_COL)));
                parser.nextTag();//end of <pos
                parser.nextTag();//second <pos
                int ch_end = crt.getPos(Integer.parseInt(parser.getAttributeValue(null, XmlNames.CRT_LINE)),
                        Integer.parseInt(parser.getAttributeValue(null, XmlNames.CRT_COL)));

                CRTEntry e = new CRTEntry(crt.getRootId(), start, end, ch_start, ch_end, flags);
                entriesList.add(e);
                parser.nextTag();//end of <pos
                parser.nextTag();//end of range
            }
            parser.nextTag();//next elem, may be </crt>
        }

        crt.setEntries(entriesList.toArray(new CRTEntry[entriesList.size()]));
    }

    public int getFlags(XMLStreamReader parser) {
        int flags = 0;
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String name = parser.getAttributeLocalName(i);
            if (name.equals(XmlNames.A_STATEMENT)) {
                flags |= CRTEntry.CRT_STATEMENT;
            } else if (name.equals(XmlNames.A_BLOCK)) {
                flags |= CRTEntry.CRT_BLOCK;
            } else if (name.equals(XmlNames.A_CONTROLLER)) {
                flags |= CRTEntry.CRT_FLOW_CONTROLLER;
            } else if (name.equals(XmlNames.A_TARGET)) {
                flags |= CRTEntry.CRT_FLOW_TARGET;
            } else if (name.equals(XmlNames.A_BRANCHTRUE)) {
                flags |= CRTEntry.CRT_BRANCH_TRUE;
            } else if (name.equals(XmlNames.A_BRANCHFALSE)) {
                flags |= CRTEntry.CRT_BRANCH_FALSE;
            }
        }
        return flags;
    }

    public void setReaderFactory(ReaderFactory r) {
        parser = ((ReaderFactoryStAX) r).parser;
    }
}
