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
import com.sun.tdk.jcov.instrument.DataBlockTargetCase;
import com.sun.tdk.jcov.instrument.DataBlockTargetDefault;
import com.sun.tdk.jcov.instrument.DataBranchSwitch;
import com.sun.tdk.jcov.instrument.XmlNames;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 *
 * @author Sergey Borodin
 */
public class DataBranchSwitchStAX implements Reader {

    private DataBranchSwitch swtch;
    private XMLStreamReader parser;

    public void readData(Object dest) throws FileFormatException {
        swtch = (DataBranchSwitch) dest;
        try {
            readData();
        } catch (XMLStreamException ex) {
            throw new FileFormatException(ex);
        }
    }

    void readData() throws XMLStreamException, FileFormatException {
//        super.readDataFrom(parser, refs, mode);

        while (!(parser.getEventType() == XMLStreamConstants.END_ELEMENT
                && parser.getLocalName() == XmlNames.SWITHCH)) {
            parser.nextTag();
            String elem = parser.getLocalName();
            if (elem == XmlNames.DEFAULT) {
                DataBlockTargetDefault def = new DataBlockTargetDefault(swtch.rootId(), 0, false, 0);
                def.readDataFrom();
                swtch.setDefault(def);
                swtch.addTarget(def);
                parser.nextTag();
            } else if (elem == XmlNames.CASE) {
                int val = Integer.parseInt(parser.getAttributeValue(null, XmlNames.VALUE));
                DataBlockTargetCase cs = new DataBlockTargetCase(swtch.rootId(), val,
                        0, false, 0);
                cs.readDataFrom();
                swtch.addTarget(cs);
                parser.nextTag();
            }
        }

    }

    public void setReaderFactory(ReaderFactory r) {
        parser = ((ReaderFactoryStAX) r).parser;
    }
}
