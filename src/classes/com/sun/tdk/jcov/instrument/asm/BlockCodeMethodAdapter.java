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
package com.sun.tdk.jcov.instrument.asm;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;

import com.sun.tdk.jcov.instrument.DataBlock;
import com.sun.tdk.jcov.instrument.DataBlockFallThrough;
import com.sun.tdk.jcov.instrument.DataBlockTargetDefault;
import com.sun.tdk.jcov.instrument.DataBranchCond;
import com.sun.tdk.jcov.instrument.DataBranchGoto;
import com.sun.tdk.jcov.instrument.DataBranchSwitch;
import com.sun.tdk.jcov.instrument.DataExit;
import com.sun.tdk.jcov.instrument.DataExitSimple;
import com.sun.tdk.jcov.instrument.DataMethodWithBlocks;
import com.sun.tdk.jcov.instrument.InstrumentationParams;
import com.sun.tdk.jcov.instrument.SimpleBasicBlock;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Attribute;

import org.objectweb.asm.tree.*;

/**
 * @author Dmitry Fazunenko
 * @author Alexey Fedorchenko
 */
class BlockCodeMethodAdapter extends OffsetRecordingMethodAdapter {

    private final MethodVisitor nextVisitor;
    private final List<DataExit> exits;
    private final Map<AbstractInsnNode, SimpleBasicBlock> insnToBB;
    private final InstrumentationParams params;

    public BlockCodeMethodAdapter(final MethodVisitor mv,
            final DataMethodWithBlocks method,
            final InstrumentationParams params) {
        super(new MethodNode(method.getAccess(), method.getName(), method.getVmSignature(), method.getSignature(), method.getExceptions()),
                method);
        this.nextVisitor = mv;
        this.insnToBB = new IdentityHashMap<AbstractInsnNode, SimpleBasicBlock>();
        this.exits = new ArrayList<DataExit>();
        this.params = params;
    }

    private SimpleBasicBlock getBB(AbstractInsnNode insn, int startBCI) {
        SimpleBasicBlock bb = insnToBB.get(insn);
        if (bb == null) {
            bb = new SimpleBasicBlock(method.rootId(), startBCI);
            insnToBB.put(insn, bb);
        } else if (startBCI >= 0) {
            bb.setStartBCI(startBCI);
        }
        return bb;
    }

    private SimpleBasicBlock getBB(AbstractInsnNode insn) {
        return getBB(insn, -1);
    }

    private AbstractInsnNode peek(ListIterator iit) {
        // Do a next() to get the next instruction..
        // Then immediately do a previous to restore our position.
        if (iit.hasNext()) {
            AbstractInsnNode insn = (AbstractInsnNode) iit.next();
            iit.previous();
            return insn;
        }
        return null;
    }

    private SimpleBasicBlock[] completeComputationOfCodeLabelNodes() {
        MethodNode methodNode = (MethodNode) mv;
        InsnList instructions = methodNode.instructions;
        int[] allToReal = new int[instructions.size()];
        int allIdx = 0;
        int insnIdx = 0;
        ListIterator iit = instructions.iterator();

        // Create the method entry block and basic block
        AbstractInsnNode insnFirst = peek(iit);
        SimpleBasicBlock bbFirst = getBB(insnFirst, 0);
        // DataBlock blockFirst = new DataBlockMethEnter();
        // bbFirst.add(blockFirst);

        while (iit.hasNext()) {
            AbstractInsnNode insn = (AbstractInsnNode) iit.next();
            allToReal[allIdx++] = insnIdx;
            int bci = bcis[insnIdx];
            int opcode = insn.getOpcode();
            if (opcode < 0) {
                // a pseudo-instruction
                if (insn.getType() == AbstractInsnNode.LINE) {
                    LineNumberNode lineNode = (LineNumberNode) insn;
                    method().addLineEntry(bci, lineNode.line);
                }
            } else {
                // a real instruction
                ++insnIdx; // advance the real instruction index

                //System.out.println( "#" + (insnIdx - 1) +
                //        " bci: " + bci + "  " +
                //        instr.toString().replace("org.objectweb.asm.tree.", "").replace("@", " @ ") +
                //        " [" + (opcode>=0? Constants.opcNames[opcode] : " pseudo") +"]");
                switch (opcode) {
                    case IFEQ:
                    case IFNE:
                    case IFLT:
                    case IFGE:
                    case IFGT:
                    case IFLE:
                    case IF_ICMPEQ:
                    case IF_ICMPNE:
                    case IF_ICMPLT:
                    case IF_ICMPGE:
                    case IF_ICMPGT:
                    case IF_ICMPLE:
                    case IF_ACMPEQ:
                    case IF_ACMPNE:
                    case IFNULL:
                    case IFNONNULL:
                    case JSR: {
                        JumpInsnNode jumpInsn = (JumpInsnNode) insn;
                        LabelNode insnTrue = jumpInsn.label;
                        int bciFalse = bcis[insnIdx]; // fall-through

                        DataBranchCond branch = new DataBranchCond(method.rootId(), bci, bciFalse - 1);
                        /* DataBlockTarget blockTrue = new DataBlockTargetCond(true);
                         DataBlockTarget blockFalse = new DataBlockTargetCond(false);
                         branch.addTarget(blockTrue);
                         branch.addTarget(blockFalse);
                         */
                        AbstractInsnNode insnFalse = peek(iit);
                        assert (insnFalse != null); // must be fall-through code
                        SimpleBasicBlock bbTrue = getBB(insnTrue);
                        SimpleBasicBlock bbFalse = getBB(insnFalse, bciFalse);

                        exits.add(branch);

                        // assign a new label for branch counting
                        // LabelNode nlab = new LabelNode();
                        // jumpInsn.label = nlab;  // branch to new label
                        //  bbTrue.add(blockTrue, nlab);

                        //bbFalse.add(blockFalse);
                        break;
                    }

                    case TABLESWITCH: {
                        TableSwitchInsnNode switchInsn = (TableSwitchInsnNode) insn;

                        // Create a block and basic-block the "default:" case
                        LabelNode insnDflt = switchInsn.dflt;
                        SimpleBasicBlock bbDefault = getBB(insnDflt);
                        DataBlockTargetDefault blockDefault = new DataBlockTargetDefault(bbDefault.rootId());

                        // assign a new default label for branch counting
                        //LabelNode nlab = new LabelNode();
                        //switchInsn.dflt = nlab;  // branch to new label
                        //bbDefault.add(blockDefault, nlab);

                        // Create the branch information
                        int bciEnd = bcis[insnIdx] - 1; // end of the switch
                        DataBranchSwitch branch = new DataBranchSwitch(method.rootId(), bci, bciEnd, blockDefault);
                        // branch.addTarget(blockDefault);
                        exits.add(branch);

                        // Process the other cases
                        ListIterator lit = switchInsn.labels.listIterator();
                        int key = switchInsn.min;
                        while (lit.hasNext()) {
                            // Create a block and basic-block the case
                            LabelNode labCase = (LabelNode) lit.next();
                            SimpleBasicBlock bbCase = getBB(labCase);
                            //DataBlockTargetCase blockCase = new DataBlockTargetCase(key++);
                            //branch.addTarget(blockCase);

                            // assign a new label to the case for branch counting
                            //nlab = new LabelNode();
                            //  lit.set(nlab);
                            // bbCase.add(blockCase, nlab);
                        }
                        break;
                    }

                    case LOOKUPSWITCH: {
                        LookupSwitchInsnNode switchInsn = (LookupSwitchInsnNode) insn;

                        // Create a block and basic-block the "default:" case
                        LabelNode insnDflt = switchInsn.dflt;
                        SimpleBasicBlock bbDefault = getBB(insnDflt);
                        DataBlockTargetDefault blockDefault = new DataBlockTargetDefault(bbDefault.rootId());

                        // assign a new default label for branch counting
                        // LabelNode nlab = new LabelNode();
                        // switchInsn.dflt = nlab;  // branch to new label
                        // bbDefault.add(blockDefault, nlab);

                        // Create the branch information
                        int bciEnd = bcis[insnIdx] - 1; // end of the switch
                        DataBranchSwitch branch = new DataBranchSwitch(method.rootId(), bci, bciEnd, blockDefault);
                        // branch.addTarget(blockDefault);
                        exits.add(branch);

                        // Process the other cases
                        ListIterator kit = switchInsn.keys.listIterator();
                        ListIterator lit = switchInsn.labels.listIterator();
                        while (lit.hasNext()) {
                            // Create a block and basic-block the case
                            LabelNode labCase = (LabelNode) lit.next();
                            SimpleBasicBlock bbCase = getBB(labCase);
                            Integer key = (Integer) kit.next();
//                            DataBlockTargetCase blockCase = new DataBlockTargetCase(key.intValue());
                            // branch.addTarget(blockCase);

                            // assign a new label to the case for branch counting
                            //nlab = new LabelNode();
                            //lit.set(nlab);
                            //bbCase.add(blockCase, nlab);
                        }
                        break;
                    }

                    case GOTO: {
                        JumpInsnNode jumpInsn = (JumpInsnNode) insn;

                        // Create origin info, a branch
                        int bciEnd = bcis[insnIdx] - 1;
                        DataBranchGoto branch = new DataBranchGoto(method.rootId(), bci, bciEnd);
                        exits.add(branch);

                        // Create destination info, a block target
                        LabelNode insnTarget = jumpInsn.label;
                        SimpleBasicBlock bbTarget = getBB(insnTarget);
                        //DataBlockTarget blockTarget = new DataBlockTargetGoto();
                        //branch.addTarget(blockTarget);

                        // assign a new label for branch counting
                        //LabelNode nlab = new LabelNode();
                        //jumpInsn.label = nlab;  // branch to new label
                        //bbTarget.add(blockTarget, nlab);
                        break;
                    }
                    case ATHROW:
                    case RET:
                    case IRETURN:
                    case LRETURN:
                    case FRETURN:
                    case DRETURN:
                    case ARETURN:
                    case RETURN: {
                        int bciNext = bcis[insnIdx];
                        DataExit exit = new DataExitSimple(method.rootId(), bci, bciNext - 1, insn.getOpcode());
                        exits.add(exit);

                        AbstractInsnNode insnNext = peek(iit);
                        if (insnNext != null) {
                            // If there is code after this, it has to be the start of a
                            // new basic block
                            getBB(insnNext, bciNext);
                        }
                        break;
                    }
                    default:
                        break;
                }
                // try add src block
            }
        }

        // Now go through the try-catch blocks
        LabelNode previousHandler = null;
        for (Iterator tbit = methodNode.tryCatchBlocks.iterator(); tbit.hasNext();) {
            TryCatchBlockNode tcbn = (TryCatchBlockNode) tbit.next();
            LabelNode insnHandler = tcbn.handler;
            if (insnHandler != previousHandler) {
                previousHandler = insnHandler;

                // Create destination info, a block target
                SimpleBasicBlock bbCatch = getBB(insnHandler);

            }
        }

        if (method().getCharacterRangeTable() != null) {
            boolean newBlock = true;
            int skip = 0;
            iit = instructions.iterator();
            while (iit.hasNext()) {
                AbstractInsnNode insn = (AbstractInsnNode) iit.next();
                int index = instructions.indexOf(insn);
                int bci = bcis[allToReal[index]];
                if (bci == skip) {
                    continue;
                }

                if (insnToBB.get(insn) != null) {
                    skip = bcis[allToReal[ instructions.indexOf(insn)]];
                }

                if (insn.getOpcode() < 0) {
                    continue;
                }

                for (CharacterRangeTableAttribute.CRTEntry entry : method().getCharacterRangeTable().getEntries()) {
                    if (entry.startBCI() == bci) {

                        if ((entry.flags & CharacterRangeTableAttribute.CRTEntry.CRT_STATEMENT) != 0 /*& newBlock*/) {
                            newBlock = false;
                            if (insnToBB.get(insn) == null) {
                                //System.out.println("Should add block at: " + bci + " in " + method().name +
                                //       " for " + Constants.opcNames[insn.getOpcode()]);
                                getBB(insn);
                                break;
                            }
                        }
                    } else {
                        if (entry.endBCI() == index && (entry.flags & CharacterRangeTableAttribute.CRTEntry.CRT_FLOW_TARGET) != 0) {
                            newBlock = true;
                        }
                    }

                }
            }

        }

        // Compute the startBCI for any basic blocks that don't have it'
        SimpleBasicBlock[] basicBlocks = new SimpleBasicBlock[insnToBB.size()];
        int i = 0;
        for (Map.Entry<AbstractInsnNode, SimpleBasicBlock> entry : insnToBB.entrySet()) {
            SimpleBasicBlock bb = entry.getValue();
            if (bb.startBCI() < 0) {
                AbstractInsnNode insn = entry.getKey();
                int index = instructions.indexOf(insn);
                int bci = bcis[allToReal[index]];
                bb.setStartBCI(bci);
            }
            basicBlocks[i++] = bb;
        }
        Arrays.sort(basicBlocks);

        return basicBlocks;
    }

    /**
     * Compute end BCIs for basic blocks, then set this info into detail blocks.
     * Assumes the basic blocks are sorted
     */
    private void computeEndBCIsAndFoldInExits(SimpleBasicBlock[] basicBlocks) {
        int ei = 0;  // exit index
        SimpleBasicBlock prev = basicBlocks[0];
        for (int bi = 1; bi <= basicBlocks.length; ++bi) {
            SimpleBasicBlock curr = null;
            int start;
            if (bi == basicBlocks.length) {
                start = method().getBytecodeLength();
            } else {
                curr = basicBlocks[bi];
                start = curr.startBCI();
            }

            int prevStart = prev.startBCI();
            // Set the previous block to end just before the current starts
            int prevEnd = start - 1;
            prev.setEndBCI(prevEnd);

            // Now that we know the endBCI, we can determine if
            // any exits reside in this range
            DataExit exit = null;
            int exitStart;
            if (ei < exits.size()) {
                exit = exits.get(ei);
                exitStart = exit.startBCI();
            } else {
                exitStart = -1; // always go to the fall-into code
            }
            if (exitStart >= prevStart && exitStart <= prevEnd) {
                // The exit is in the prev block attach it
                prev.setExit(exit);
                // System.out.println("found " + ei + " BB: " + prev + " exit: " + exit);
                ++ei;  // set-up to handle the next exit
            } else {
                // There is no exit from the prev block, so we fall
                // into the curr block (if any)
                if (curr != null) {
                    DataBlock fall = new DataBlockFallThrough(curr.rootId());
                    curr.add(fall);
                }
            }

            prev = curr;
        }
        // System.out.println("ei: " + ei + " / " + exits.size());
        assert (ei == exits.size());
    }

    private void insertInstrumentation() {
        MethodNode methodNode = (MethodNode) mv;
        InsnList instructions = methodNode.instructions;

        for (Map.Entry<AbstractInsnNode, SimpleBasicBlock> entry : insnToBB.entrySet()) {

            // Basic block 'bb' starts at instruction 'insn'
            AbstractInsnNode insn = entry.getKey();
            SimpleBasicBlock bb = entry.getValue();
            //   System.out.println("insn = " + insn);
            instructions.insert(insn, Instrumenter.instrumentation(bb, params.isDetectInternal()));
        }
    }

    @Override
    public void visitAttribute(Attribute attr) {
        super.visitAttribute(attr);
        if (attr instanceof CharacterRangeTableAttribute) {
            method().setCharacterRangeTable((CharacterRangeTableAttribute) attr);
        }
    }

// the instruction list has been built, insert the instrumentation
    @Override
    public void visitEnd() {
        super.visitEnd();

        SimpleBasicBlock[] basicBlocks = completeComputationOfCodeLabelNodes();
        computeEndBCIsAndFoldInExits(basicBlocks);
        //debugDump();
        insertInstrumentation();
        method().setBasicBlocks(basicBlocks);

        // push the result to the writer
        MethodNode methodNode = (MethodNode) mv;
        methodNode.accept(nextVisitor);
    }
}
