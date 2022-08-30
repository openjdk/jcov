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

import com.sun.tdk.jcov.util.Utils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Label;

/**
 * OffsetRecordingMethodAdapter
 *
 * @author Robert Field
 */
class OffsetRecordingMethodAdapter extends MethodVisitor {

    private int currentOffset;
    private int currentInstructionIndex;
    protected int[] bcis;
    protected final DataMethodWithBlocks method;

    public OffsetRecordingMethodAdapter(final MethodVisitor mv,
            final DataMethodWithBlocks method) {
        super(ASMUtils.ASM_API_VERSION, mv);
        this.currentInstructionIndex = 0;
        this.bcis = new int[60];
        this.method = method;
    }

    DataMethodWithBlocks method() {
        return method;
    }

    private void recordInstructionOffset() {
        if (currentInstructionIndex >= bcis.length) {
            bcis = Utils.copyOf(bcis, bcis.length * 2);
        }
        bcis[currentInstructionIndex++] = currentOffset;
    }

    public void visitLabel(final Label label) {
        OffsetLabel ol = (OffsetLabel) label;
        currentOffset = ol.originalOffset;
        if (ol.realLabel) {
            super.visitLabel(label);
        }
    }

    public void visitInsn(final int opcode) {
        recordInstructionOffset();
        super.visitInsn(opcode);
    }

    public void visitIntInsn(final int opcode, final int operand) {
        recordInstructionOffset();
        super.visitIntInsn(opcode, operand);
    }

    public void visitVarInsn(final int opcode, final int var) {
        recordInstructionOffset();
        super.visitVarInsn(opcode, var);
    }

    public void visitTypeInsn(final int opcode, final String desc) {
        recordInstructionOffset();
        super.visitTypeInsn(opcode, desc);
    }

    public void visitFieldInsn(
            final int opcode,
            final String owner,
            final String name,
            final String desc) {
        recordInstructionOffset();
        super.visitFieldInsn(opcode, owner, name, desc);
    }

    public void visitJumpInsn(final int opcode, final Label label) {
        recordInstructionOffset();
        super.visitJumpInsn(opcode, label);
    }

    public void visitLdcInsn(final Object cst) {
        recordInstructionOffset();
        super.visitLdcInsn(cst);
    }

    public void visitIincInsn(final int var, final int increment) {
        recordInstructionOffset();
        super.visitIincInsn(var, increment);
    }

    public void visitTableSwitchInsn(
            final int min,
            final int max,
            final Label dflt,
            final Label labels[]) {
        recordInstructionOffset();
        super.visitTableSwitchInsn(min, max, dflt, labels);
    }

    public void visitLookupSwitchInsn(
            final Label dflt,
            final int keys[],
            final Label labels[]) {
        recordInstructionOffset();
        super.visitLookupSwitchInsn(dflt, keys, labels);
    }

    public void visitMultiANewArrayInsn(final String desc, final int dims) {
        recordInstructionOffset();
        super.visitMultiANewArrayInsn(desc, dims);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String anno, boolean b) {
        method().addAnnotation(anno);
        return super.visitAnnotation(anno, b);
    }

    @Override
    public void visitEnd() {
        recordInstructionOffset();  // record end
        method().setBytecodeLength(currentOffset); // and set as method length
        super.visitEnd();
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        recordInstructionOffset();
        super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        recordInstructionOffset();
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

}
