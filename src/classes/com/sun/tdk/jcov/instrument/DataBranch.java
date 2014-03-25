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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

import java.io.PrintStream;
import java.util.Iterator;

/**
 * <p> DataBranch class is abstract class representing branching classfile
 * structures. DataBranchCond, DataBranchGoto and DataBranchSwitch extend this
 * class. </p> <p> Each branch contain a number of blocks this branch is
 * targeting. These blocks can be accessed by getBranchTargets() or
 * getIterator(). </p>
 *
 * @author Robert Field
 * @see DataBlockTarget
 */
public abstract class DataBranch extends DataExit {

    public List<DataBlockTarget> branchTargets;

    /**
     * Creates a DataBranch object representing branch structure.
     *
     * @param rootId DataRoot id this branch belongs to
     * @param bciStart start position of this block in classfile
     * @param bciEnd end position of this block in classfile
     */
    DataBranch(int rootId, int bciStart, int bciEnd) {
        super(rootId, bciStart, bciEnd);
        branchTargets = new ArrayList<DataBlockTarget>();
    }

    /**
     * @return list of all targets this branch leads to
     */
    public List<DataBlockTarget> getBranchTargets() {
        return branchTargets;
    }

    /**
     * Not supposed to be used outside
     *
     * @param target target to add
     */
    public void addTarget(DataBlockTarget target) {
        branchTargets.add(target);
        target.setEnclosing(this);
    }

    void addBlocks(Collection<DataBlock> collection) {
        for (DataBlock block : branchTargets) {
            collection.add(block);
        }
    }

    /**
     * XML Generation
     */
    @Override
    void xmlBody(XmlContext ctx) {
//        ctx.incIndent();
        for (DataBlockTarget target : branchTargets) {
            if (ctx.showBodiesInExitSubBlocks) { //Always
                target.xmlGen(ctx);
            } else {
                target.xmlGenBodiless(ctx);
            }
        }
//        ctx.decIndent();
    }

    /**
     * @return iterator through all targets this branch leads to
     */
    @Override
    public Iterator<DataBlock> getIterator() {
        return new Iterator<DataBlock>() {
            private final Iterator<DataBlockTarget> delegate =
                    branchTargets.iterator();

            public boolean hasNext() {
                return delegate.hasNext();
            }

            public DataBlock next() {
                return delegate.next();
            }

            public void remove() {
            }
        };
    }

    /**
     * Debug printing
     */
    @Override
    void printDebug(PrintStream out, String indent) {
        super.printDebug(out, indent);
        String newIndent = indent + "    ";
        for (DataBlockTarget target : branchTargets) {
            target.printDebug(out, newIndent);
        }
    }

    void writeObject(DataOutput out) throws IOException {
        super.writeObject(out);
        out.writeShort(branchTargets.size());
        for (DataBlockTarget t : branchTargets) {
            if (t instanceof DataBlockTargetCase) {
                out.write(1);
            } else if (t instanceof DataBlockTargetCond) {
                out.write(2);
            } else if (t instanceof DataBlockTargetDefault) {
                out.write(3);
            } else if (t instanceof DataBlockTargetGoto) {
                out.write(4);
            } else {
                throw new IOException("DataBranch.writeObject: Unknown dataBlockTarget class " + t.getClass().getName() + ".");
            }
            t.writeObject(out);
        }
    }

    DataBranch(int rootId, DataInput in) throws IOException {
        super(rootId, in);
        int size = in.readShort();
        branchTargets = new ArrayList<DataBlockTarget>(size);
        for (int i = 0; i < size; ++i) {
            byte code = in.readByte();
            switch (code) {
                case 1:
                    branchTargets.add(new DataBlockTargetCase(rootId, in));
                    break;
                case 2:
                    branchTargets.add(new DataBlockTargetCond(rootId, in));
                    break;
                case 3:
                    branchTargets.add(new DataBlockTargetDefault(rootId, in));
                    break;
                case 4:
                    branchTargets.add(new DataBlockTargetGoto(rootId, in));
                    break;
                default:
                    throw new IOException("DataBlockTarget with unknown code in DataBranch " + code);
            }
        }
    }
}