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
package com.sun.tdk.jcov.report;

import java.util.Collection;

/**
 * Implementations of this interface are responsible for mapping short names provided with {@code ancdf} options to
 * AncFilter instances. Instances of this interface are discovered through {@code ServiceLoader} API.
 */
public interface AncFilterFactory {
    /**
     * Creates an instance of {@code AncFilter} identified by a short name.
     * @param shortName
     * @return {@code AncFilter} or null, if {@code shortName} does not correspond to any filter supported by this factory.
     */
    AncFilter instantiate(String shortName);

    /**
     * Instantiases all supported {@code AncFilter}s, which could be instanciated without additional information,
     * such as parameters of {@code ParameterizedAncFilter}
     * @return
     */
    Collection<AncFilter> instantiateAll();
}
