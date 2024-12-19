/*
 * Copyright (c) 2014, 2024 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.jcov.io;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.sun.tdk.jcov.data.FileFormatException;
import com.sun.tdk.jcov.data.ScaleOptions;
import com.sun.tdk.jcov.filter.MemberFilter;
import com.sun.tdk.jcov.instrument.DataRoot;
import com.sun.tdk.jcov.instrument.reader.ReaderFactory;
import com.sun.tdk.jcov.tools.SimpleScaleCompressor;
import com.sun.tdk.jcov.util.Utils;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.zip.ZipInputStream;

/**
 * @author Dmitry Fazunenko
 * @author Alexey Fedorchenko
 */
public class Reader {

    /**
     * buffer size for BufferedReaders used here
     */
    protected final static int IO_BUF_SIZE = 524288;

    /**
     * @param filename filename
     * @return BufferedReader associated with the file specified by the filename
     */
    public static BufferedReader openFile4Reading(String filename) throws IOException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(filename), Charset.forName("UTF-8")), IO_BUF_SIZE);
    }

    public static DataRoot readXML(String fileName) throws FileFormatException {
        return readXML(fileName, true, null);
    }

    public static DataRoot readXML(InputStream is) throws FileFormatException {
        return readXML(is, true, null);
    }

    private static final String ZIP_EXTENSION = ".xml.zip";
    private static final String XML_EXTENSION = ".xml";

    private static InputStream openZipOrXML(File zipOrXML) throws IOException {
        if (zipOrXML.toString().endsWith(ZIP_EXTENSION)) {
            //the assumption is that
            //1. the zip will be having a special extension: ".xml.zip"
            //2. the zip has one entry and that entry is the xml file
            if (!zipOrXML.getName().endsWith(ZIP_EXTENSION))
                throw new IllegalArgumentException("Unknown file format. Can be \".xml.zip\" or XML " + zipOrXML);
            ZipInputStream res = new ZipInputStream(new FileInputStream(zipOrXML));
            if (!res.getNextEntry().getName().endsWith(XML_EXTENSION))
                throw new IllegalArgumentException("No XML file in " + zipOrXML);
            return res;
        } else return new FileInputStream(zipOrXML);
    }

    public static DataRoot readXML(String fileName, boolean read_scales,
            MemberFilter filter) throws FileFormatException {
        File f = new File(fileName);
        if (!f.exists()) {
            throw new FileFormatException("File " + fileName + " doesn''t exist");
        }
        try  (InputStream in = openZipOrXML(f)) {
            DataRoot dataRoot = readXML(in, read_scales, filter);
            dataRoot.setStorageFileName(fileName);
            return dataRoot;
        } catch (Exception e) {
            if (!(e instanceof FileFormatException)) {
                throw new FileFormatException(e.getMessage(), e);
            } else {
                throw (FileFormatException) e;
            }
        }
    }

    public static DataRoot readXML(InputStream input, boolean read_scales,
            MemberFilter filter) throws FileFormatException {

        DataRoot root = new DataRoot("", false);
        ScaleOptions scaleOpts = new ScaleOptions(read_scales, 0, new SimpleScaleCompressor());
        root.setScaleOpts(scaleOpts);
        root.setAcceptor(filter);

        ReaderFactory rf = ReaderFactory.newInstance(Utils.getJavaVersion(), input);
        root.setReaderFactory(rf);
        root.readDataFrom();

        if (read_scales && scaleOpts.getScaleSize() == 0) {
            root.createScales();
        }

        return root;
    }

    public static DataRoot readXMLHeader(String fileName) throws FileFormatException {
        try {
            return readXMLHeader(new FileInputStream(fileName));
        } catch (FileNotFoundException ex) {
            throw new FileFormatException(ex.getMessage());
        }
    }

    public static DataRoot readXMLHeader(InputStream input) throws FileFormatException {
        DataRoot root = new DataRoot("", false);

        ReaderFactory rf = ReaderFactory.newInstance(Utils.getJavaVersion(), input);
        root.setReaderFactory(rf);
        root.readHeader();
        return root;
    }
}
