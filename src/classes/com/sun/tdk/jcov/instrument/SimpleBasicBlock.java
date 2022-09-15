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

import com.sun.tdk.jcov.data.Scale;
import com.sun.tdk.jcov.tools.OneElemIterator;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;

/**
 * @author Dmitry Fazunenko
 * @author Alexey Fedorchenko
 */
public class SimpleBasicBlock extends BasicBlock {

    private final DataBlock block;

    /**
     * Creates new instance of SimpleBasicBlock
     *
     * @param rootId
     */
    public SimpleBasicBlock(int rootId) {
        this(rootId, -1, -1, true);
    }

    /**
     * Creates new instance of SimpleBasicBlock
     *
     * @param rootId
     * @param startBCI
     */
    public SimpleBasicBlock(int rootId, int startBCI) {
        this(rootId, startBCI, startBCI, true);
    }

    /**
     * Creates new instance of SimpleBasicBlock
     *
     * @param rootId
     * @param startBCI
     * @param attached
     */
    public SimpleBasicBlock(int rootId, int startBCI, int endBCI, boolean attached) {
        super(rootId, startBCI, endBCI);
        if (attached) {
            block = startBCI == 0
                    ? new DataBlockMethEnter(rootId)
                    : new DataBlock(rootId) {
                public String kind() {
                    return XmlNames.BLOCK;
                }
            };
        } else {
            block = startBCI == 0
                    ? new DataBlockMethEnter(rootId, 0, false, 0)
                    : new DataBlock(rootId, 0, false, 0) {
                public String kind() {
                    return XmlNames.BLOCK;
                }
            };
        }

        block.setConcreteLocation(this);
        add(block);
    }

    /**
     * Get containing block`s ID
     *
     * @return
     */
    public int getId() {
        return block.getId();
    }

    /**
     * Get containing block. Usually it's not needed to work directly with
     * DataBlocks as all common information can be found from methods wasHit(),
     * getCount() and getScale(). DataBlock itself contains information about
     * it's type (DataBlockCond, DataBlockTargetGoto, ...)
     *
     * @return
     */
    public DataBlock getBlock() {
        return block;
    }

    /**
     * Get containing block`s scale
     *
     * @return
     */
    public Scale getScale() {
        return block.scale;
    }

    @Override
    public void xmlGen(XmlContext ctx) {
        block.xmlGen(ctx);
    }

    @Override
    boolean wasHit() {
        return block.wasHit();
    }

    /**
     * Get containing block`s hit count
     *
     * @return
     */
    public long getCount() {
        return block.getCount();
    }

    @Override
    public void checkCompatibility(BasicBlock other) throws MergeException {
        super.checkCompatibility(other);
        if (!(other instanceof SimpleBasicBlock)) {
            throw new MergeException("Block has other type than it's"
                    + " merging copy, type is SimpleBasicBlock",
                    "",
                    MergeException.HIGH);
        }

        SimpleBasicBlock so = (SimpleBasicBlock) other;
        if (!getDataRoot().getParams().isDynamicCollect() && !other.getDataRoot().getParams().isDynamicCollect()) {
            if (getId() != so.getId()) {
                throw new MergeException("Block has other id than it's"
                        + " merging copy, id is " + getId(),
                        "",
                        MergeException.HIGH);
            }
        }
    }

    @Override
    public void merge(BasicBlock other) {
        SimpleBasicBlock so = (SimpleBasicBlock) other;

        block.mergeScale(so.block);
        block.setCount(getCount() + so.getCount());
    }

    @Override
    public Iterator<DataBlock> getIterator() {
        return new OneElemIterator(block);
    }

    @Override
    void writeObject(DataOutput out) throws IOException {
        super.writeObject(out);
        if (block instanceof DataBlockMethEnter) {
            out.write(1);
        } else {
            out.write(2);
        }
        block.writeObject(out);
    }

    SimpleBasicBlock(int rootId, DataInput in) throws IOException {
        super(rootId, in);
        byte code = in.readByte();
        switch (code) {
            case 1:
                block = new DataBlockMethEnter(rootId, in);
                break;
            case 2:
                block = new DataBlock(rootId, in) {
                    @Override
                    public String kind() {
                        return XmlNames.BLOCK;
                    }
                };
            default:
                throw new IOException("DataBlock with unknown code in SimpleBasicBlock " + code);
        }
    }
}