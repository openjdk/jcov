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

/**
 * <b>FilterFactory</b> factory is used by to instantiate member filter. <p>
 * This class uses a provider-based architecture. To create a FilterFactory call
 * the static getInstance() methods. <p> Once a FilterFactory object is created,
 * filters can be obtained by calling the getMemberFilter() method
 *
 * @author Dmitry Fazunenko
 * @author Sergey Borodin
 */
public class FilterFactory {
    // candidate to remove

    private FilterSpi spi = null;

    private FilterFactory(String spiClassName) {
        if (spiClassName == null) {
            spi = null;
        } else {
            try {
                Class spiClass = Class.forName(spiClassName);
                spi = (FilterSpi) spiClass.newInstance();
            } catch (Exception e) {
                throw new Error("Cannot create an instance of "
                        + "FilterFactorySpi: " + e);
            }
        }
    }

    /**
     * Returns FilterFactory instance. FilterFactory uses FilterFactorySpi to
     * get class member filter which is initialized during the first call of
     * this method.
     *
     * @param spiClassName name of spi class
     * @return factory instance
     * @throws Error if instance of FilterFactory cannot be created.
     *
     */
    public static FilterFactory getInstance(String spiClassName) {
        return new FilterFactory(spiClassName);
    }

    /**
     * Returns member filter. Never returns null.
     */
    public MemberFilter getMemberFilter() {

        MemberFilter filter = spi == null ? MemberFilter.ACCEPT_ALL
                : new DelegateFilter(spi);

        return filter;
    }

    final class DelegateFilter implements MemberFilter {

        private String spiName;
        private MemberFilter delegate;

        DelegateFilter(FilterSpi spi) {
            spiName = spi.getClass().getName();
            delegate = spi.getFilter();
        }

        public boolean accept(DataClass clz) {
            if (delegate == null) {
                throw new UnsupportedOperationException("Trying to filter using "
                        + spiName);
            } else {
                return delegate.accept(clz);
            }
        }

        public boolean accept(DataClass clz, DataMethod m) {
            if (delegate == null) {
                throw new UnsupportedOperationException("Trying to filter"
                        + " modern data using " + spiName);
            } else {
                return delegate.accept(clz, m);
            }
        }

        public boolean accept(DataClass clz, DataField f) {
            if (delegate == null) {
                throw new UnsupportedOperationException("Trying to filter"
                        + " modern data using " + spiName);
            } else {
                return delegate.accept(clz, f);
            }
        }

        @Override
        public String toString() {
            return delegate == null ? spiName + " filter" : delegate.toString();
        }
    }
}
