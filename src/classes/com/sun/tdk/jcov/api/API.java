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
package com.sun.tdk.jcov.api;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class representing entire API set
 *
 * @author Dmitry Fazunenko
 */
public class API {

    private final HashMap<String, ClassDescr> classes =
            new HashMap<String, ClassDescr>();

    private API() {
        this(new ArrayList<ClassDescr>());
    }

    public API(List<ClassDescr> classList) {
        if (classList != null) {
            for (ClassDescr cls : classList) {
                addClass(cls);
            }
        }
    }

    public static API parseSigFile(String sigFileName, APIReader reader)
            throws IOException, FileFormatException {
        return parseSigFile(new File(sigFileName), reader);
    }

    public static API parseSigFile(File sigFile, APIReader reader)
            throws IOException, FileFormatException {
        API api = new API();
        for (ClassDescr cls : reader.read(sigFile)) {
            api.addClass(cls);
        }
        return api;
    }

    public ClassDescr getClass(String name) {
        return classes.get(name);
    }

    public void addClass(ClassDescr cls) {
        if (cls != null) {
            classes.put(cls.name, cls);
        }
    }

    public SortedSet<String> getAllClassNames() {
        TreeSet<String> treeSet = new TreeSet<String>();
        treeSet.addAll(classes.keySet());
        return treeSet;
    }
}
