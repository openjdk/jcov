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
package com.sun.tdk.jcov.processing;

import com.sun.tdk.jcov.instrument.DataRoot;

/**
 * Interface for data processors.
 * <p/>
 * When a format is not supported the corresponding
 * <code>process()</code> should throw
 * <code>UnsupportedOperationException</code>.
 *
 * @see DataProcessorSPI
 * @author Dmitry Fazunenko
 */
public interface DataProcessor {

    /**
     * Handles passed JcovFileImage object. It's up to implementator to return
     * either the same or new created object.
     *
     * @param root - data to process
     * @return modified data
     * @throws ProcessingException - if error occurred while processing
     * @throws UnsupportedOperationException - if format is not supported by the
     * data processor
     */
    public DataRoot process(DataRoot root) throws ProcessingException;
    /**
     * Implementation that returns input data unmodified.
     */
    public final static DataProcessor STUB = new DataProcessor() {
        public DataRoot process(DataRoot root) throws ProcessingException {
            return root;
        }
    };
}
