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
 * LocationConcrete
 *
 * @author Robert Field
 */
public abstract class LocationConcrete extends LocationAbstract {

    private int startBCI;
    private int endBCI;

    /**
     * Creates a new instance of LocationAbstract
     */
    protected LocationConcrete(int rootId, int startBCI, int endBCI) {
        super(rootId);
        this.startBCI = startBCI;
        this.endBCI = endBCI;
    }

    LocationConcrete(int rootId, int bci) {
        this(rootId, bci, bci);
    }

    LocationConcrete(int rootId) {
        this(rootId, -1, -1);
    }

    public void setStartBCI(int startBCI) {
        this.startBCI = startBCI;
    }

    public void setEndBCI(int endBCI) {
        this.endBCI = endBCI;
    }

    public int startBCI() {
        return startBCI;
    }

    public int endBCI() {
        return endBCI;
    }

    public boolean isSameLocation(LocationConcrete other) {
        return startBCI == other.startBCI && endBCI == other.endBCI && kind().equals(other.kind());
    }

    void writeObject(DataOutput out) throws IOException {
        out.writeShort(startBCI);
        out.writeShort(endBCI);
    }

    LocationConcrete(int rootId, DataInput in) throws IOException {
        super(rootId);
        startBCI = in.readShort();
        endBCI = in.readShort();
    }
}