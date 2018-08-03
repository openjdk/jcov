/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.jcov.report.ancfilters;

import com.sun.tdk.jcov.instrument.DataBlock;
import com.sun.tdk.jcov.instrument.DataClass;
import com.sun.tdk.jcov.instrument.DataMethod;
import com.sun.tdk.jcov.report.AncFilter;
import com.sun.tdk.jcov.report.ParameterizedAncFilter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * This class provides an ability to use externally generated lists of methods to be used
 * as an ANC filter. There are certain assumptions on the file format:
 * <ul>
 *     <li>Lines containing comments must start with "#" symbol.</li>
 *     <li>First line in the file should be a comment and must contain the "ANC reason"</li>
 *     <li>Every non-comment line should be empty or be in a form of
 *     &lt;class-name#&lt&gt;&lt;method-name-andsignature&gt;. Example:  java/lang/String#indexOf(I)I</li>
 * </ul>
 */
public class ListANCFilter implements ParameterizedAncFilter {

    private static final String COMMENT_PREFIX = "#";
    private static final String CLASS_METHOD_SEPARATOR = "#";

    private Map<String, Set<String>> excludes;
    private String ancReason;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean accept(DataClass dc) {
        assertInitialized();
        return false;
    }

    private void assertInitialized() {
        if(excludes == null) {
            throw new IllegalStateException("No ANC list was provided");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean accept(DataClass dc, DataMethod dm) {
        assertInitialized();
        String className = dc.getFullname();
        String methodName = dm.getName();
        int dot = methodName.indexOf(".");
        if(dot > -1) {
            className = className + methodName.substring(0, dot);
            methodName = methodName.substring(dot + 1);
        }
        Set<String> methods = excludes.get(className);
        return methods != null && methods.contains(methodName + dm.getVmSignature());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean accept(DataMethod dm, DataBlock db) {
        assertInitialized();
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAncReason() {
        assertInitialized();
        return ancReason;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setParameter(String parameter) throws IOException {
        if(parameter == null)
            throw new IllegalArgumentException("File must not be null for list filter.");
        excludes = new HashMap<>();
        try(BufferedReader in =
                    new BufferedReader(new FileReader(parameter))) {
            String line = in.readLine();
            if(line != null && line.startsWith(COMMENT_PREFIX)) {
                ancReason = line.substring(COMMENT_PREFIX.length());
            } else {
                throw new IllegalStateException("No ANC reason was provided.");
            }
            while((line = in.readLine()) != null) {
                if(line.startsWith(COMMENT_PREFIX)) {
                    continue;
                }
                int separator = line.indexOf(CLASS_METHOD_SEPARATOR);
                if (separator > -1) {
                    String clss = line.substring(0, separator);
                    Set<String> mthds = excludes.get(clss);
                    if (mthds == null) {
                        mthds = new HashSet<>();
                        excludes.put(clss, mthds);
                    }
                    mthds.add(line.substring(separator + 1));
                } else {
                    if (line.length() > 0) {
                        throw new IllegalStateException("Unidentifiable method " + line);
                    }
                }
            }
        }
    }
}
