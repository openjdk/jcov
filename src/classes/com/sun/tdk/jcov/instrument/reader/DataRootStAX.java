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
import com.sun.tdk.jcov.instrument.DataRoot;
import com.sun.tdk.jcov.instrument.InstrumentationOptions.ABSTRACTMODE;
import com.sun.tdk.jcov.instrument.InstrumentationOptions.InstrumentationMode;
import com.sun.tdk.jcov.instrument.InstrumentationParams;
import com.sun.tdk.jcov.instrument.XmlNames;
import java.util.TreeMap;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 *
 * @author Sergey Borodin
 */
public class DataRootStAX implements RootReader {

    XMLStreamReader parser;
    DataRoot root;

    public void readData(Object dest) throws FileFormatException {
        root = (DataRoot) dest;
        try {
            readData();
        } catch (XMLStreamException ex) {
            if ("ParseError at [row,col]:[1,1]\nMessage: Premature end of file.".equals(ex.getMessage())) {
                throw new FileFormatException("File is empty");
            } else {
                if (ex.getMessage() != null) {
                    int i = ex.getMessage().indexOf("Message: ");
                    if (i < 0) {
                        throw new FileFormatException("Malformed xml file {0} - " + ex.getMessage());
                    } else {
                        throw new FileFormatException("Malformed xml file {0} - " + ex.getMessage().substring(i + 9));
                    }
                }
            }

            throw new FileFormatException(ex);
        }
    }

    private void readData() throws XMLStreamException, FileFormatException {
        int event = -1;//before start
        while (event != XMLStreamConstants.END_ELEMENT) {
            event = parser.nextTag();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    String tagName = parser.getLocalName();
                    if (tagName.equals(XmlNames.COVERAGE)) {
                        break;
                    } else if (tagName.equals(XmlNames.HEAD)) {
                        readHeader0();
                        break;
                    } else if (tagName.equals(XmlNames.PACKAGE)) {
                        readPackage();
                        break;
                    }
                default:
                    break;
            }
        }
    }

    private void readPackage() throws XMLStreamException, FileFormatException {
        String packName = parser.getAttributeValue(null, XmlNames.NAME);
        int event = parser.getEventType();
        while (true) {
            event = parser.nextTag();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    readClass(packName);
                    break;
                case XMLStreamReader.END_ELEMENT://end of package
                    if (parser.getLocalName().equals(XmlNames.PACKAGE)) {//not necessary?!
                        return;
                    }
                    break;
            }
        }
    }

    private void readClass(String packName) throws FileFormatException {
        String clName = parser.getAttributeValue(null, XmlNames.NAME);
        String fullName = packName.isEmpty() ? clName
                : packName.replaceAll("\\.", "/") + "/" + clName;
        String checksum = parser.getAttributeValue(null, XmlNames.CHECKSUM);
        DataClass clazz = new DataClass(root.rootId(), fullName, checksum == null ? -1
                : Long.parseLong(checksum), root.isDifferentiateElements());

        clazz.setInfo(parser.getAttributeValue(null, XmlNames.FLAGS), parser.getAttributeValue(null, XmlNames.SIGNATURE),
                parser.getAttributeValue(null, XmlNames.SUPERNAME), parser.getAttributeValue(null, XmlNames.SUPER_INTERFACES));
        final String inner = parser.getAttributeValue(null, XmlNames.INNER_CLASS);
        if (inner != null) {
            if ("inner".equals(inner)) {
                clazz.setInner(true);
            } else if ("anon".equals(inner)) {
                clazz.setInner(true);
                clazz.setAnonym(true);
            }
        }

//        if (acceptor == null || acceptor.accepts(clazz)) {
        clazz.readDataFrom();
        root.addClass(clazz);
//        }

    }

    ;

    public void readHeader(Object dest) throws FileFormatException {
        root = (DataRoot) dest;
        try {
            readHeader();
        } catch (XMLStreamException ex) {
            throw new FileFormatException(ex);
        }
    }

    private void readHeader() throws XMLStreamException {
        int event = -1;//before start
        boolean readHeader = false;

        while (!readHeader) {
            event = parser.nextTag();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    String tagName = parser.getLocalName();
                    if (tagName.equals(XmlNames.COVERAGE)) {
                        break;
                    } else if (tagName.equals(XmlNames.HEAD)) {
                        readHeader0();
                        readHeader = true;
                        break;
                    } else if (tagName.equals(XmlNames.PACKAGE)) {
                        break;//should not occure
                    }
                default:
                    break;
            }
        }
    }

    private void readHeader0() throws XMLStreamException {
        TreeMap<String, String> props = new TreeMap();

        parser.nextTag();//begin
        InstrumentationMode mode = InstrumentationMode.BRANCH;
        boolean detectInternal = true;
        boolean dynamicCollect = false;
        String[] includes = null, excludes = null, callerIncludes = null, callerExcludes = null;
        while (parser.getEventType() != XMLStreamReader.END_ELEMENT) {
            String name = parser.getAttributeValue(null, XmlNames.NAME);
            String value = parser.getAttributeValue(null, XmlNames.VALUE);
            if (name.equals("coverage.generator.args")) {
                root.setArgs(value);
            } else if (name.equals("id.count")) {
                root.setCount(Integer.parseInt(value));
            } else if (name.equals("coverage.generator.mode")) {
                mode = InstrumentationMode.fromString(value);
            } else if (name.equals("coverage.generator.internal")) {
                detectInternal = "detect".equals(value);
            } else if (name.equals("coverage.generator.include")) {
                includes = value.split("\\|");
            } else if (name.equals("coverage.generator.exclude")) {
                excludes = value.split("\\|");
            } else if (name.equals("coverage.generator.caller_include")) {
                callerIncludes = value.split("\\|");
            } else if (name.equals("coverage.generator.caller_exclude")) {
                callerExcludes = value.split("\\|");
            } else if (name.equals("dynamic.collected")) {
                dynamicCollect = Boolean.parseBoolean(value);
            } else if (name.equals("scale.size")) {
                if (root.getScaleOpts().needReadScales()) {
                    root.getScaleOpts().setScaleSize(Integer.parseInt(value));
                }
            } else if (name.equals("scales.compressed")) {
                root.getScaleOpts().setScalesCompressed(Boolean.parseBoolean(value));
            } else {
                props.put(name, value);
            }
            parser.nextTag();
            parser.nextTag();
        }

        ABSTRACTMODE abstractmode = null;
        boolean instrumentNative = false, instrumentFields = false;
        root.setParams(new InstrumentationParams(dynamicCollect, instrumentNative, instrumentFields, detectInternal, abstractmode, includes, excludes, callerIncludes, callerExcludes, mode));

        root.setXMLHeadProperties(props);
    }

    public void setReaderFactory(ReaderFactory r) {
        parser = ((ReaderFactoryStAX) r).parser;
    }
}
