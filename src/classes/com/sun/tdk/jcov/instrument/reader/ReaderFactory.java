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
import java.io.InputStream;
import java.util.HashMap;

/**
 *
 * @author Sergey Borodin
 */
public abstract class ReaderFactory {

    protected int javaVersion;
    protected HashMap<Class, Reader> readers;
    protected Object processingData;

    public static ReaderFactory newInstance(int javaVersion, InputStream is) {
        String rName = ReaderFactory.class.getName();
        rName += "StAX";
        try {
            ReaderFactory r = (ReaderFactory) Class.forName(rName).newInstance();
            r.javaVersion = javaVersion;
            r.setDataSource(is);
            r.readers = new HashMap();
            return r;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public void setProcessingData(Object data) {
        processingData = data;
    }

    public Object getProcessingData() {
        return processingData;
    }

    abstract void setDataSource(InputStream is) throws FileFormatException;

    abstract String getReadersSuffix();

    public Reader getReaderFor(Object o) throws FileFormatException {
        Class c = o.getClass();
        return getReaderForClass(c);
    }

    public Reader getSuperReaderFor(Class c) throws FileFormatException {
        Class superC = c.getSuperclass();
        return getReaderForClass(superC);
    }

    private Reader getReaderForClass(Class c) throws FileFormatException {
        if (readers.get(c) != null) {
            return readers.get(c);
        }

        try {
            String rName = c.getPackage().getName() + ".reader." + c.getSimpleName() + getReadersSuffix();
            Reader r = (Reader) Class.forName(rName).newInstance();
            r.setReaderFactory(this);
            readers.put(c, r);
            return r;
        } catch (ClassNotFoundException ex) {
            Class superClass = c.getSuperclass();
            if (superClass != null && !Object.class.equals(superClass)) {
                return getReaderForClass(superClass);
            }
            return null;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

    }
}
