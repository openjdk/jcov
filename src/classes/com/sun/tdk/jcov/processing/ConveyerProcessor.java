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
import java.util.ArrayList;
import java.util.List;

/**
 * A processor that applies other processors in series.
 *
 * @author Dmitry Fazunenko
 */
public class ConveyerProcessor implements DataProcessor {

    private final List<DataProcessor> processors;

    /**
     * Constructs a processor with an empty set of data processors
     */
    public ConveyerProcessor() {
        processors = new ArrayList<DataProcessor>();
    }

    /**
     * Adds processor to the list of data processors to be applied.
     *
     * @param p - processors, if null - nothing will happen
     */
    public void add(DataProcessor p) {
        if (p != null && !processors.contains(p)) {
            processors.add(p);
        }
    }

    /**
     * Removes processor from the list of processors to be applied.
     *
     * @param p - processor, if null - nothing will happen
     */
    public void remove(DataProcessor p) {
        if (p != null) {
            processors.remove(p);
        }
    }

    /**
     * Applies all registered processors to the image in order they were added.
     *
     * @param root - data to process
     * @return modified data
     * @throws ProcessingException - if one of added processor fail to handle
     * data
     * @throws UnsupportedOperationException - if format is not supported by any
     * of registred data processor
     */
    public DataRoot process(DataRoot root) throws ProcessingException {
        DataRoot result = root;
        for (DataProcessor p : processors) {
            result = p.process(result);
        }
        return result;
    }
}
