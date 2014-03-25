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

import com.sun.tdk.jcov.tools.DelegateIterator;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Set;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.Collection;

import java.util.Iterator;
import org.objectweb.asm.tree.LabelNode;

/**
 * BasicBlock
 *
 * Depends on LabelNode (via code currently in MethodNode) using Label.info to
 * hold the mapping between Label and LabelNode.
 *
 * This class is used in DataMethodWithBlocks to store blocks&branches.
 * DataMethodWithBlocks contains an array of BasicBlocks. Each BasicBlock
 * contains a set of DataBlocks and DataExit (DataExitSimple for just exiting
 * methods; abstract DataBranch for branches). So any method consists of some
 * blocks (DataBlock) and some instructions moving (GOTO) for branching
 *
 * @author Robert Field
 */
public class BasicBlock extends LocationConcrete {

    private DataBlock fallenInto = null;
    public final Map<DataBlock, LabelNode> blockMap;
    public DataExit exit = null;

    /**
     * Creates a new instance of BasicBlock
     */
    BasicBlock(int rootId, int startBCI) {
        super(rootId, startBCI);
        blockMap = new IdentityHashMap<DataBlock, LabelNode>();
    }

    /**
     * Creates a new instance of BasicBlock
     */
    BasicBlock(int rootId, int startBCI, int endBCI) {
        super(rootId, startBCI, endBCI);
        blockMap = new IdentityHashMap<DataBlock, LabelNode>();
    }

    public BasicBlock(int rootId) {
        this(rootId, -1);
    }

    void add(DataBlock blk, LabelNode label) {
        blockMap.put(blk, label);
        if (blk.isFallenInto()) {
            fallenInto = blk;
        }
        blk.setConcreteLocation(this);
    }

    public void add(DataBlock blk) {
        add(blk, null);
    }

    LabelNode getLabel(DataBlock blk) {
        return blockMap.get(blk);
    }

    boolean contains(DataBlock blk) {
        return blockMap.containsKey(blk);
    }

    DataBlock fallenInto() {
        return fallenInto;
    }

    public void setExit(DataExit exit) {
        this.exit = exit;
    }

    Collection<DataBlock> blocks() {
        return blockMap.keySet();
    }

    Set<Map.Entry<DataBlock, LabelNode>> blockLabelSet() {
        return blockMap.entrySet();
    }

    boolean wasHit() {
        return fallenInto() == null ? false : fallenInto().wasHit();
    }

    /**
     * XML Generation
     */
    public String kind() {
        return XmlNames.BLOCK;
    }

    @Override
    void xmlBody(XmlContext ctx) {
//        xmlEntries(ctx);
        xmlDetailBody(ctx);
    }

    void xmlDetailBody(XmlContext ctx) {
        if (ctx.showNonNested && blockMap != null) {//BRANCH only
            for (DataBlock block : blockMap.keySet()) {
                if (!block.isNested()) {
                    block.xmlGen(ctx);
                }
            }
        }
        if (exit != null) {
            exit.xmlGen(ctx);
        }
    }

    public void checkCompatibility(BasicBlock other) throws MergeException {
        if (blockMap.keySet().size() != other.blocks().size()) {
            throw new MergeException("Block has other number of data blocks than "
                    + "it's merging copy, expected " + blockMap.keySet().size() + "; found " + other.blocks().size(),
                    "", MergeException.HIGH);
        }

        if (exit instanceof DataBranch) {
            if (!(other.exit instanceof DataBranch)) {
                throw new MergeException("Block exit has other type than it's"
                        + " merging copy, expected DataBranchAbstract; found " + other.exit,
                        "", MergeException.HIGH);
            }
            DataBranch branch = (DataBranch) exit;
            DataBranch obranch = (DataBranch) other.exit;

            if (branch.branchTargets.size() != obranch.branchTargets.size()) {
                throw new MergeException("Block has other number of data blocks (targets) than "
                        + "it's merging copy, expected " + blockMap.keySet().size() + "; found " + other.blocks().size(),
                        "", MergeException.HIGH);
            }
        }
    }

    public void merge(BasicBlock other) {
        boolean dynamicCollected = DataRoot.getInstance(rootId).getParams().isDynamicCollect() || DataRoot.getInstance(other.rootId).getParams().isDynamicCollect();

        mergeDataBlocks(blockMap.keySet(), other.blocks(), dynamicCollected);

        if (exit instanceof DataBranch) {
            DataBranch branch = (DataBranch) exit;
            DataBranch obranch = (DataBranch) other.exit;

            mergeDataBlocks(branch.branchTargets, obranch.branchTargets, dynamicCollected);
        }
    }

    private static void mergeDataBlocks(Collection<? extends DataBlock> blocks,
            Collection<? extends DataBlock> oblocks, boolean dynamicCollected) {
        for (DataBlock b : blocks) {
            for (DataBlock bo : oblocks) {
                if (dynamicCollected) {
                    if (b.startBCI() == bo.startBCI()
                            && b.endBCI() == bo.endBCI()
                            && (b.getClass() == bo.getClass())
                            && b.isFallenInto() == bo.isFallenInto()) {

                        b.mergeScale(bo);
                        b.setCount(b.getCount() + bo.getCount());
                        break;
                    }
                } else {
                    if (b.getId() == bo.getId()) {
                        b.mergeScale(bo);
                        b.setCount(b.getCount() + bo.getCount());
                        break;
                    }
                }
            }
        }
    }

    public Iterator<DataBlock> getIterator() {
        return new DelegateIterator<DataBlock>() {
            private int index;

            @Override
            protected Iterator<DataBlock> nextIterator() {
                if (index == 0) {
                    index++;
                    return blockMap.keySet().iterator();
                } else if (index == 1) {
                    index++;
                    if (exit != null) {
                        return exit.getIterator();
                    } else {
                        return nextIterator();
                    }
                } else {
                    return null;
                }
            }
        };
    }

    void writeObject(DataOutput out) throws IOException {
        super.writeObject(out);
        Collection<DataBlock> blocks = blocks();
        int blocksNum = 0;
        for (DataBlock b : blocks) {
            if (!(b instanceof DataBlockTarget)) {
                ++blocksNum;
            }
        }
        out.writeShort(blocksNum);
        for (DataBlock b : blocks) {
            if (b instanceof DataBlockTarget) {
                // for some reason we can _create_ data with DataBlockTarget in blockMap, but we do not _save_ and _read_ them in XML
                continue;
            }
            if (b instanceof DataBlockCatch) {
                out.write(0);
            } else if (b instanceof DataBlockFallThrough) {
                out.write(1);
            } else if (b instanceof DataBlockMethEnter) {
                out.write(2);
            } else {
                throw new IOException("BasicBlock.writeObject - Unknown dataBlock class " + b.getClass().getName() + ".");
            }
            b.writeObject(out);
        }
        if (exit != null) {
            out.writeBoolean(true);
            if (exit instanceof DataExitSimple) {
                out.write(1);
            } else if (exit instanceof DataBranchCond) {
                out.write(2);
            } else if (exit instanceof DataBranchGoto) {
                out.write(3);
            } else if (exit instanceof DataBranchSwitch) {
                out.write(4);
            } else {
                throw new IOException("BasicBlock.writeObject - Unknown dataExit class " + exit.getClass().getName() + ". Please contact jcov_dev_ww@oracle.com");
            }
            exit.writeObject(out);
        } else {
            out.writeBoolean(false);
        }
//        fallenInto.writeObject(out); not needed?
    }

    BasicBlock(int rootId, DataInput in) throws IOException {
        super(rootId, in);
        int blockNum = in.readShort();
        blockMap = new IdentityHashMap<DataBlock, LabelNode>(blockNum);
        int code;
        for (int i = 0; i < blockNum; ++i) {
            code = in.readByte();
            switch (code) {
                case 0:
                    blockMap.put(new DataBlockCatch(rootId, in), null);
                    break;
                case 1:
                    blockMap.put(new DataBlockFallThrough(rootId, in), null);
                    break;
                case 2:
                    blockMap.put(new DataBlockMethEnter(rootId, in), null);
                    break;
                // for some reason we can _create_ data with DataBlockTarget in blockMap, but we do not _save_ and _read_ them in XML
                default:
                    throw new IOException("DataBlock with unknown code in BasicBlock " + code);
            }
        }
        if (in.readBoolean()) {
            code = in.readByte();
            switch (code) {
                case 1:
                    exit = new DataExitSimple(rootId, in);
                    break;
                case 2:
                    exit = new DataBranchCond(rootId, in);
                    break;
                case 3:
                    exit = new DataBranchGoto(rootId, in);
                    break;
                case 4:
                    exit = new DataBranchSwitch(rootId, in);
                    break;
                default:
                    throw new IOException("DataExit with unknown code in BasicBlock " + code);
            }
        }
    }
}