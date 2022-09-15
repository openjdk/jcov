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
import java.io.IOException;

/**
 * <p> DataBlockTarget is an abstract class for all branch target blocks:
 * DataBlockTargetCase, DataBlockTargetDefault, DataBlockTargetCond and
 * DataBlockTargetGoto. </p>
 *
 * @author Robert Field
 * @see DataBlock
 * @see DataBranch
 */
public abstract class DataBlockTarget extends DataBlock {

    DataBranch enclosing;

    DataBranch enclosing() {
        return enclosing;
    }

    DataBlockTarget(int rootId) {
        super(rootId);
    }

    DataBlockTarget(int rootId, int slot, boolean attached, long count) {
        super(rootId, slot, attached, count);
    }

    void setEnclosing(DataBranch enclosing) {
        this.enclosing = enclosing;
    }

    /**
     * Yes, it is nested in a branch
     */
    @Override
    boolean isNested() {
        return true;
    }

    /**
     * XML Generation
     */
    void xmlAttrsValue(XmlContext ctx) {
        // for value printing, override this
    }

    @Override
    protected void xmlAttrs(XmlContext ctx) {
        super.xmlAttrs(ctx);
        xmlAttrsValue(ctx);
    }

    DataBlockTarget(int rootId, DataInput in) throws IOException {
        super(rootId, in);
    }
}