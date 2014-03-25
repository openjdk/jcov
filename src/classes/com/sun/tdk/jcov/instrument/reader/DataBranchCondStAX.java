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
import com.sun.tdk.jcov.instrument.DataBlockTargetCond;
import com.sun.tdk.jcov.instrument.DataBranchCond;
import com.sun.tdk.jcov.instrument.XmlNames;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 *
 * @author Sergey Borodin
 */
public class DataBranchCondStAX implements Reader {

    private DataBranchCond cond;
    private XMLStreamReader parser;
    private ReaderFactory rf;

    public void readData(Object dest) throws FileFormatException {
        cond = (DataBranchCond) dest;
        try {
            readData();
        } catch (XMLStreamException ex) {
            throw new FileFormatException(ex);
        }
    }

    void readData() throws XMLStreamException, FileFormatException {
//        super.readDataFrom(parser, refs, mode);

        parser.nextTag();

        boolean isVal = Boolean.parseBoolean(parser.getAttributeValue(null, XmlNames.VALUE));
        DataBlockTargetCond cond1 = new DataBlockTargetCond(cond.rootId(), isVal,
                0, false, 0);
        cond1.readDataFrom();
        parser.nextTag();//end cond

        parser.nextTag();
        isVal = Boolean.parseBoolean(parser.getAttributeValue(null, XmlNames.VALUE));
        DataBlockTargetCond cond2 = new DataBlockTargetCond(cond.rootId(), isVal,
                0, false, 0);
        cond2.readDataFrom();
        parser.nextTag();//end cond

        if (cond1.isFallenInto()) {
            cond.addTarget(cond2);
            cond.addTarget(cond1);
        } else {
            cond.addTarget(cond1);
            cond.addTarget(cond2);
        }

        parser.nextTag();//end br
    }

    public void setReaderFactory(ReaderFactory r) {
        rf = r;
        parser = ((ReaderFactoryStAX) r).parser;
    }
}
