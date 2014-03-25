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
import com.sun.tdk.jcov.data.Scale;
import com.sun.tdk.jcov.data.ScaleOptions;
import com.sun.tdk.jcov.instrument.DataField;
import com.sun.tdk.jcov.instrument.DataRoot;
import com.sun.tdk.jcov.instrument.XmlNames;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 *
 * @author Sergey Borodin
 */
public class DataFieldStAX implements Reader {

    private DataField fld;
    private XMLStreamReader parser;
    private ReaderFactory rf;

    public void readData(Object dest) throws FileFormatException {
        fld = (DataField) dest;
        try {
            readData();
        } catch (XMLStreamException ex) {
            throw new FileFormatException(ex.getMessage());
        }
    }

    void readData() throws XMLStreamException, FileFormatException {

        if (fld.getParent().getSource() != null && fld.getParent().getSource().endsWith(".fx")) {
            Reader r = rf.getSuperReaderFor(DataField.class);
            r.readData(fld);
        }

        long count = 0;
        String s = parser.getAttributeValue(null, XmlNames.COUNT);
        if (s != null) {
            count = Long.parseLong(s);
        }
        fld.setCount(count);

        readScale();

        fld.setSignature(parser.getAttributeValue(null, XmlNames.SIGNATURE));

        parser.nextTag();
    }

    private void readScale() {
        String s = parser.getAttributeValue(null, XmlNames.SCALE);
        if (s != null && s.length() > 0) {
            try {
                DataRoot r = DataRoot.getInstance(fld.rootId());
                ScaleOptions opts = r.getScaleOpts();
                if (opts.needReadScales()) {
                    fld.setScale(new Scale(s.toCharArray(), s.length(),
                            opts.getScaleSize(), opts.getScaleCompressor(), opts.scalesCompressed()));
                }
            } catch (FileFormatException ex) {
            }
        }
    }

    public void setReaderFactory(ReaderFactory r) {
        rf = r;
        parser = ((ReaderFactoryStAX) r).parser;
    }
}
