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
import org.testng.annotations.Test;

import java.util.Collection;

import static org.testng.Assert.assertTrue;

public class BuiltInAncFiltersTest {
    static final AncFilterFactory factory = new BuiltInAncFilters();
    @Test
    public void testInstantiate() {
        assertTrue(factory.instantiate("setter") instanceof SetterANCFilter);
        assertTrue(factory.instantiate("getter") instanceof GetterANCFilter);
    }
    @Test
    public void testInstantiateAll() {
        Collection<AncFilter> filters = factory.instantiateAll();
        assertTrue(filters.stream().anyMatch(f -> f instanceof CatchANCFilter));
        assertTrue(filters.stream().anyMatch(f -> f instanceof DeprecatedANCFilter));
        assertTrue(filters.stream().noneMatch(f -> f instanceof ListANCFilter));
    }
}
