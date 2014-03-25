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
package com.sun.tdk.jcov.io;

import com.sun.tdk.jcov.filter.MemberFilter;
import com.sun.tdk.jcov.instrument.DataField;
import com.sun.tdk.jcov.instrument.DataMethod;
import com.sun.tdk.jcov.instrument.DataClass;
import com.sun.tdk.jcov.util.Utils;
import com.sun.tdk.jcov.util.Utils.Pattern;

/**
 * An acceptor able to filter classes by their names and modifiers. All filters
 * for this acceptor are specified in the format understandable by
 * WildCardStringFilter class
 *
 * @see com.sun.tdk.jcov.tools.Acceptor
 * @see com.sun.tdk.jcov.tools.WildCardStringFilter
 * @see com.sun.tdk.jcov.filedata.Clazz
 * @author Konstantin Bobrovsky
 */
public class ClassSignatureFilter implements MemberFilter {

    /**
     * class modifiers acceptable by this acceptor
     */
    private String[] modifs;
    private String[] includes;
    private String[] excludes;
    private Pattern[] alls;

    /**
     * Constructs new ClassSignatureAcceptor with the specified
     * inclusion/exclusion masks and acceptable modifiers
     *
     * @param incl_masks string masks specifying acceptable class names
     * @param excl_masks string masks specifying unacceptable class names
     * @param modifs acceptable modifiers
     */
    public ClassSignatureFilter(String[] incl_masks, String[] excl_masks, String[] modifs) {
        this.modifs = modifs;
        this.includes = incl_masks;
        this.excludes = excl_masks;
        this.alls = Utils.concatFilters(incl_masks, excl_masks);
    }

    public boolean accept(DataClass c) {
        return Utils.accept(alls, modifs, "/" + c.getFullname(), c.getSignature());
    }

    public boolean accept(DataClass clz, DataMethod m) {
        return true;
    }

    public boolean accept(DataClass clz, DataField f) {
        return true;
    }

    public String[] getModifs() {
        return modifs;
    }

    public String[] getExcludes() {
        return excludes;
    }

    public String[] getIncludes() {
        return includes;
    }
}
