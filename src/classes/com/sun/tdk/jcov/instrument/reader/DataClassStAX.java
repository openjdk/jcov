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
import com.sun.tdk.jcov.instrument.DataClass;
import com.sun.tdk.jcov.instrument.DataField;
import com.sun.tdk.jcov.instrument.DataMethod;
import com.sun.tdk.jcov.instrument.DataMethodEntryOnly;
import com.sun.tdk.jcov.instrument.DataMethodInvoked;
import com.sun.tdk.jcov.instrument.DataMethodWithBlocks;
import com.sun.tdk.jcov.instrument.XmlNames;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 *
 * @author Sergey Borodin
 */
public class DataClassStAX implements Reader {

    private XMLStreamReader parser;
    private ReaderFactory rf;
    private DataClass clazz;

    public void readData(Object dest) throws FileFormatException {
        clazz = (DataClass) dest;
        try {
            readData();
        } catch (XMLStreamException ex) {
            throw new FileFormatException(ex);
        }
    }

    final void readData() throws XMLStreamException, FileFormatException {

        clazz.setSuperName(parser.getAttributeValue(null, XmlNames.SUPERNAME));
        clazz.setSource(parser.getAttributeValue(null, XmlNames.SOURCE));

        boolean end = false;
        while (!end) {
            int type = parser.nextTag();
            switch (type) {
                case XMLStreamConstants.START_ELEMENT:
                    if (parser.getLocalName() == XmlNames.METHOD) {
                        parseMethod(parser);
                    } else {
                        parseField(parser);
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    end = true;
                    break;
            }
        }

    }

    private void parseMethod(XMLStreamReader parser) throws XMLStreamException, FileFormatException {

        String name = parser.getAttributeValue(null, XmlNames.NAME);
        String vmSig = parser.getAttributeValue(null, XmlNames.VMSIG);
        String flags = parser.getAttributeValue(null, XmlNames.FLAGS);
        int access = Integer.parseInt(parser.getAttributeValue(null, XmlNames.ACCESS));

        String signature = parser.getAttributeValue(null, XmlNames.SIGNATURE);
        int id = 0;
        String s = parser.getAttributeValue(null, XmlNames.ID);
        if (s != null) {
            id = Integer.parseInt(s);
        }
        long count = 0;
        s = parser.getAttributeValue(null, XmlNames.COUNT);
        if (s != null) {
            count = Long.parseLong(s);
        }
        int length = 0;
        s = parser.getAttributeValue(null, XmlNames.LENGTH);
        if (s != null) {
            length = Integer.parseInt(s);
        }

        String scale = parser.getAttributeValue(null, XmlNames.SCALE);

        parser.nextTag();
        DataMethod mthd;
        if (parser.getEventType() == XMLStreamConstants.END_ELEMENT) {
            if (flags.contains("native")
                    || flags.contains("abstract")) {
                DataMethodInvoked m = new DataMethodInvoked(clazz, access,
                        name, vmSig, signature,
                        new String[0], id);
                m.setCount(count);
                m.setScale(scale);
                mthd = m;
            } else {
                DataMethodEntryOnly m = new DataMethodEntryOnly(clazz, access,
                        name, vmSig, signature,
                        new String[0], id);//method exceptions aren't stored in xml
                m.setCount(count);
                m.setScale(scale);
                mthd = m;
            }
        }
        else {
            DataMethodWithBlocks m = new DataMethodWithBlocks(clazz, access,
                    name, vmSig, signature,
                    new String[0]);//method exceptions aren't stored in xml
            m.setBytecodeLength(length);
            m.readDataFrom();
            mthd = m;
        }
    }

    private void parseField(XMLStreamReader parser) throws XMLStreamException, FileFormatException {
        String name = parser.getAttributeValue(null, XmlNames.NAME);
        String vmSig = parser.getAttributeValue(null, XmlNames.VMSIG);
        String flags = parser.getAttributeValue(null, XmlNames.FLAGS);
        int access = Integer.parseInt(parser.getAttributeValue(null, XmlNames.ACCESS));
        int id = Integer.parseInt(parser.getAttributeValue(null, XmlNames.ID));

        String val = parser.getAttributeValue(null, XmlNames.VALUE);
        DataField field = new DataField(clazz, access,
                name, vmSig, clazz.getSignature(),
                val, id);
        field.readDataFrom();
    }

    public void setReaderFactory(ReaderFactory r) {
        rf = r;
        parser = ((ReaderFactoryStAX) r).parser;
    }
}
