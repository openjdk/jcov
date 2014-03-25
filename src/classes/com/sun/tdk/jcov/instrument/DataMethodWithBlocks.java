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
package com.sun.tdk.jcov.instrument;

import com.sun.tdk.jcov.data.Scale;
import com.sun.tdk.jcov.tools.DelegateIterator;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * DataMethodWithBlocks can contain any number of blocks inside of it.
 *
 * @author Robert Field
 */
public class DataMethodWithBlocks extends DataMethod {

    /**
     * Bytecode length
     */
    private int bytecodeLength;
    /**
     *
     */
    private CharacterRangeTableAttribute characterRangeTable = null;
    /**
     * Including blocks
     */
    private BasicBlock[] basicBlocks;

    /**
     * Creates new instance of DataMethodWithBlocks
     *
     * @param k
     * @param access
     * @param name
     * @param desc
     * @param signature
     * @param exceptions
     */
    public DataMethodWithBlocks(final DataClass k,
            final int access,
            final String name,
            final String desc,
            final String signature,
            final String[] exceptions) {
        super(k, access, name, desc, signature, exceptions, false);
    }

    @Override
    public DataMethod clone(DataClass newClass, int newAccess, String newName) {
        DataMethodWithBlocks dm = new DataMethodWithBlocks(newClass, newAccess, newName, this.getVmSignature(), this.getSignature(), this.getExceptions());
        dm.setBasicBlocks(getBasicBlocks());
        dm.setCharacterRangeTable(getCharacterRangeTable());
        dm.setBytecodeLength(getBytecodeLength());
        dm.lineTable = getLineTable();
        return dm;
    }

    /**
     * Set containing blocks
     *
     * @param basicBlocks
     */
    public void setBasicBlocks(BasicBlock[] basicBlocks) {
        this.basicBlocks = basicBlocks;
    }

    /**
     * @return containing blocks
     */
    public BasicBlock[] getBasicBlocks() {
        return basicBlocks;
    }

    /**
     * @return the bytecode length
     */
    public int getBytecodeLength() {
        return bytecodeLength;
    }

    /**
     * Set bytecode length
     *
     * @param bytecodeLength
     */
    public void setBytecodeLength(final int bytecodeLength) {
        this.bytecodeLength = bytecodeLength;
    }

    /**
     * Set CharacterRangeTable
     *
     * @param crt
     */
    public void setCharacterRangeTable(CharacterRangeTableAttribute crt) {
        this.characterRangeTable = crt;
    }

    /**
     * Get CharacterRangeTable
     *
     * @return
     */
    public CharacterRangeTableAttribute getCharacterRangeTable() {
        return characterRangeTable;
    }

    @Override
    public boolean hasCRT() {
        return this.getCharacterRangeTable() != null;
    }

    @Override
    public boolean wasHit() {
        return getBasicBlocks() == null ? false : getBasicBlocks()[0].wasHit();
    }

    @Override
    public long getCount() {
        return getBasicBlocks() == null ? 0 : getBasicBlocks()[0].blocks().isEmpty() ? -1 : getBasicBlocks()[0].blocks().iterator().next().getCount();
    }

    @Override
    public void setCount(long count) {
        if (getBasicBlocks() != null && !getBasicBlocks()[0].blocks().isEmpty()) {
            getBasicBlocks()[0].blocks().iterator().next().setCount(count);
        }
    }

    @Override
    public Scale getScale() {
        return getBasicBlocks() == null ? null : getBasicBlocks()[0].blocks().isEmpty() ? null : getBasicBlocks()[0].blocks().iterator().next().scale;
    }

    @Override
    public int getSlot() {
        return getBasicBlocks() == null ? -1 : getBasicBlocks()[0].blocks().isEmpty() ? -1 : getBasicBlocks()[0].blocks().iterator().next().slot;
    }

    @Override
    void xmlAttrs(XmlContext ctx) {
        super.xmlAttrs(ctx);
        ctx.attr(XmlNames.LENGTH, getBytecodeLength());
    }

    @Override
    void xmlBody(XmlContext ctx) {
        if (getBasicBlocks() != null) {
            for (BasicBlock bb : getBasicBlocks()) {
                bb.xmlGen(ctx);
            }
        }
        if (ctx.showLineTable && getLineTable() != null) {
            xmlLineTable(ctx);
        }
        if (ctx.showRangeTable && getCharacterRangeTable() != null) {
            getCharacterRangeTable().xmlGen(ctx);
        }
    }

    @Override
    public void checkCompatibility(DataMethod other, String trace) throws MergeException {
        if (!(other instanceof DataMethodWithBlocks)) {
            throw new MergeException("Method has other type than it's"
                    + " merging copy, expected DataMethodWithBlocks; found " + other.getClass().getSimpleName(),
                    trace, MergeException.CRITICAL);
        }

        DataMethodWithBlocks m = (DataMethodWithBlocks) other;
        if (getBasicBlocks().length != m.getBasicBlocks().length) {
            throw new MergeException("Method has other number of basic blocks than "
                    + "it's merging copy, the number is " + getBasicBlocks().length,
                    trace, MergeException.CRITICAL);
        }

        for (BasicBlock bb : getBasicBlocks()) {
            for (BasicBlock bbo : m.getBasicBlocks()) {
                if (bb.startBCI() == bbo.startBCI() || bb.endBCI() == bbo.endBCI()) {
                    if (bb.startBCI() == bbo.startBCI() && bb.endBCI() == bbo.endBCI()) {
                        try {
                            bb.checkCompatibility(bbo);
                        } catch (MergeException e) {
                            e.location = trace + ", block " + bb.startBCI() + "-" + bb.endBCI() + e.location;
                            throw e;
                        }
                        break;
                    } else {
                        System.err.println("* WARNING *: block ranges are invalid in " + trace + ": [" + bbo.startBCI() + "; " + bbo.endBCI() + "] (expected [" + bb.startBCI() + "; " + bb.endBCI() + "]). This can mean that you are merging results from different product versions.");
                    }
                }
            }
        }
    }

    @Override
    public void merge(DataMethod other) {
        DataMethodWithBlocks m = (DataMethodWithBlocks) other;
        for (BasicBlock bb : getBasicBlocks()) {
            for (BasicBlock bbo : m.getBasicBlocks()) {
                if (bb.startBCI() == bbo.startBCI()
                        && bb.endBCI() == bbo.endBCI()) {
                    bb.merge(bbo);
                    break;
                }
            }
        }
    }

    /**
     * Debug printing
     */
    void printDebug(PrintStream out, String indent) {
        out.print(indent);
        out.println("Method: " + getName());
        String newIndent = indent + "    ";
        for (BasicBlock bb : getBasicBlocks()) {
            bb.printDebug(out, newIndent);
        }
    }

    public Iterator<DataBlock> iterator() {
        return new DelegateIterator<DataBlock>() {
            private int i;

            @Override
            protected Iterator<DataBlock> nextIterator() {
                if (i < getBasicBlocks().length) {
                    return getBasicBlocks()[i++].getIterator();
                } else {
                    return null;
                }
            }
        };
    }

    @Override
    public List<DataBlock> getBlocks() {
        LinkedList<DataBlock> blocks = new LinkedList<DataBlock>();
        for (BasicBlock bb : basicBlocks) {
            for (DataBlock db : bb.blocks()) {
//                if (!(db instanceof DataBlockTarget))
                blocks.add(db);
            }
        }

        return blocks;
    }

    @Override
    public List<DataBranch> getBranches() {
        LinkedList<DataBranch> branches = new LinkedList<DataBranch>();
        for (BasicBlock bb : basicBlocks) {
            if (bb.exit instanceof DataBranch) {
                branches.add((DataBranch) bb.exit);
            }
        }

        return branches;
    }

    @Override
    public List<DataBlockTarget> getBranchTargets() {
        LinkedList<DataBlockTarget> targets = new LinkedList<DataBlockTarget>();
        for (BasicBlock bb : basicBlocks) {
            if (bb.exit instanceof DataBranch) {
                DataBranch br = (DataBranch) bb.exit;
                for (DataBlockTarget db : br.branchTargets) {
                    targets.add(db);
                }
            }
        }

        return targets;
    }

    void writeObject(DataOutput out) throws IOException {
        super.writeObject(out);
        out.writeShort(bytecodeLength);
        // characterRangeTable
        out.writeShort(basicBlocks.length);
        for (int i = 0; i < basicBlocks.length; ++i) {
            if (basicBlocks[i] instanceof SimpleBasicBlock) {
                out.write(1);
            } else {
                out.write(2);
            }
            basicBlocks[i].writeObject(out);
        }
    }

    DataMethodWithBlocks(DataClass parent, DataInput in) throws IOException {
        super(parent, in);
        bytecodeLength = in.readShort();
        // charRangeTable
        int bblen = in.readShort();
        basicBlocks = new BasicBlock[bblen];
        for (int i = 0; i < bblen; ++i) {
            byte code = in.readByte();
            switch (code) {
                case 1:
                    basicBlocks[i] = new SimpleBasicBlock(rootId, in);
                    break;
                case 2:
                    basicBlocks[i] = new BasicBlock(rootId, in);
                    break;
                default:
                    throw new IOException("BasicBlock with unknown code in DataMethodWithBlocks " + code);
            }
        }
    }
}