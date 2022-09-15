/*
 * Copyright (c) 2014, 2022 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.jcov.instrument;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 *
 * @author Robert Field
 */
public class DataBlockTargetCase extends DataBlockTarget {

    int targetValue;

    /**
     * Creates a new instance of DataBlockTargetCase
     */
    public DataBlockTargetCase(int rootId, int value) {
        super(rootId);
        this.targetValue = value;
    }

    public DataBlockTargetCase(int rootId, int value, int slot, boolean attached, long count) {
        super(rootId, slot, attached, count);
        this.targetValue = value;
    }

    public boolean equals(Object o) {
        return super.equals(o) && targetValue == ((DataBlockTargetCase) o).targetValue;
    }

    public int hashCode() {
        return super.hashCode() + targetValue;
    }

    /**
     * XML Generation
     */
    public String kind() {
        return XmlNames.CASE;
    }

    void xmlAttrsValue(XmlContext cxt) {
        cxt.attr(XmlNames.VALUE, targetValue);
    }

    void writeObject(DataOutput out) throws IOException {
        super.writeObject(out);
        out.writeInt(targetValue);
    }

    DataBlockTargetCase(int rootId, DataInput in) throws IOException {
        super(rootId, in);
        targetValue = in.readInt();
    }
}