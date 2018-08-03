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

import com.sun.tdk.jcov.report.AncFilter;
import com.sun.tdk.jcov.report.AncFilterFactory;
import com.sun.tdk.jcov.report.ParameterizedAncFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.HashMap;

import static java.util.stream.Collectors.toList;

public class BuiltInAncFilters implements AncFilterFactory {
    private static final Map<String, Class<? extends AncFilter>> filterClasses;
    static {
        filterClasses = new HashMap();
        filterClasses.put("catch", CatchANCFilter.class);
        filterClasses.put("deprecated", DeprecatedANCFilter.class);
        filterClasses.put("empty", EmptyANCFilter.class);
        filterClasses.put("getter", GetterANCFilter.class);
        filterClasses.put("list", ListANCFilter.class);
        filterClasses.put("setter", SetterANCFilter.class);
        filterClasses.put("synthetic", SyntheticANCFilter.class);
        filterClasses.put("throw", ThrowANCFilter.class);
        filterClasses.put("toString", ToStringANCFilter.class);
    }
    @Override
    public AncFilter instantiate(String shortName) {
        try {
            return filterClasses.get(shortName).newInstance();
        } catch (InstantiationException|IllegalAccessException e) {
            throw new RuntimeException("Unable to instantiate filter " + shortName, e);
        }
    }

    @Override
    public Collection<AncFilter> instantiateAll() {
        List<AncFilter> filters = new ArrayList<>();
        for(Class<? extends AncFilter> cls : filterClasses.values()) {
            if(!ParameterizedAncFilter.class.isAssignableFrom(cls)) {
                try {
                    filters.add(cls.newInstance());
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException("Unable to instantiate filter " + cls.getName(), e);
                }
            }
        }
        return filters;
    }
}
