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
package com.sun.tdk.jcov.filter;

import com.sun.tdk.jcov.instrument.DataClass;
import com.sun.tdk.jcov.instrument.DataField;
import com.sun.tdk.jcov.instrument.DataMethod;
import java.util.ArrayList;
import java.util.List;

/**
 * A filter that applies other filters in series.
 *
 * @author Dmitry Fazunenko
 */
public class ConveyerFilter implements MemberFilter {

    private final List<MemberFilter> filters;

    /**
     * Constructs a filter with an empty set of filters
     */
    public ConveyerFilter() {
        filters = new ArrayList<MemberFilter>();
    }

    /**
     * Adds filter to the list of filters to be applied.
     *
     * @param f - filter, if null - nothing will happen
     */
    public void add(MemberFilter f) {
        if (f != null && !filters.contains(f)) {
            filters.add(f);
        }
    }

    /**
     * Removes filter from the list of filters to be applied.
     *
     * @param f - filter, if null - nothing will happen
     */
    public void remove(MemberFilter f) {
        if (f != null) {
            filters.remove(f);
        }
    }

    /**
     * Walks from the list of filters in the order they were added and applies
     * them one by one.
     *
     * @param clz - class to check
     * @return true if either the class is accepted by all filters or none of
     * filters were added.
     */
    public boolean accept(DataClass clz) {
        for (MemberFilter f : filters) {
            if (!f.accept(clz)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Walks from the list of filters in the order they were added and applies
     * them one by one.
     *
     * @param clz - class of method
     * @param m - method to check
     * @return true if either the method of the class is accepted by all filters
     * or none of filters were added.
     */
    public boolean accept(DataClass clz, DataMethod m) {
        for (MemberFilter f : filters) {
            if (!f.accept(clz, m)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Walks from the list of filters in the order they were added and applies
     * them one by one.
     *
     * @param clz - class of field
     * @param fld - field to check
     * @return true if either class the field of the class accepted by all
     * filters or none of filters were added.
     */
    public boolean accept(DataClass clz, DataField fld) {
        for (MemberFilter f : filters) {
            if (!f.accept(clz, fld)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Conveyer filter: ");
        for (MemberFilter f : filters) {
            sb.append(f.toString()).append(" ");
        }
        return sb.toString();
    }
}
