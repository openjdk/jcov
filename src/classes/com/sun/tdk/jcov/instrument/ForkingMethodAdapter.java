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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

/**
 * ForkingMethodAdapter
 *
 * @author Robert Field
 */
class ForkingMethodAdapter extends MethodVisitor {

    static class DuplicatingAnnotationAdapter extends AnnotationVisitor {

        final AnnotationVisitor av1;
        final AnnotationVisitor av2;

        DuplicatingAnnotationAdapter(final AnnotationVisitor av1, final AnnotationVisitor av2) {
            super(Opcodes.ASM4);
            this.av1 = av1;
            this.av2 = av2;
        }

        public void visit(String name, Object value) {
            av1.visit(name, value);
            av2.visit(name, value);
        }

        public void visitEnum(String name, String desc, String value) {
            av1.visit(name, value);
            av2.visit(name, value);
        }

        public AnnotationVisitor visitAnnotation(String name, String desc) {
            AnnotationVisitor rav1 = av1.visitAnnotation(name, desc);
            AnnotationVisitor rav2 = av2.visitAnnotation(name, desc);
            return new DuplicatingAnnotationAdapter(rav1, rav2);
        }

        public AnnotationVisitor visitArray(String name) {
            AnnotationVisitor rav1 = av1.visitArray(name);
            AnnotationVisitor rav2 = av2.visitArray(name);
            return new DuplicatingAnnotationAdapter(rav1, rav2);
        }

        public void visitEnd() {
            av1.visitEnd();
            av2.visitEnd();
        }
    }
    /**
     * The {@link MethodVisitor} to which this adapter delegates calls.
     */
    protected MethodVisitor mv1;
    protected MethodVisitor mv2;

    /**
     * Constructs a new {@link MethodAdapter} object.
     *
     * @param mv the code visitor to which this adapter must delegate calls.
     */
    public ForkingMethodAdapter(final MethodVisitor mv1, final MethodVisitor mv2) {
        super(Opcodes.ASM4);
        this.mv1 = mv1;
        this.mv2 = mv2;
    }

    public AnnotationVisitor visitAnnotationDefault() {
        AnnotationVisitor av1 = mv1.visitAnnotationDefault();
        AnnotationVisitor av2 = mv2.visitAnnotationDefault();
        return new DuplicatingAnnotationAdapter(av1, av2);
    }

    public AnnotationVisitor visitAnnotation(
            final String desc,
            final boolean visible) {
        AnnotationVisitor av1 = mv1.visitAnnotation(desc, visible);
        AnnotationVisitor av2 = mv2.visitAnnotation(desc, visible);
        return new DuplicatingAnnotationAdapter(av1, av2);
    }

    public AnnotationVisitor visitParameterAnnotation(
            final int parameter,
            final String desc,
            final boolean visible) {
        AnnotationVisitor av1 = mv1.visitParameterAnnotation(parameter, desc, visible);
        AnnotationVisitor av2 = mv2.visitParameterAnnotation(parameter, desc, visible);
        return new DuplicatingAnnotationAdapter(av1, av2);
    }

    public void visitAttribute(final Attribute attr) {
        mv1.visitAttribute(attr);
        mv2.visitAttribute(attr);
    }

    public void visitCode() {
        mv1.visitCode();
        mv2.visitCode();
    }

    public void visitFrame(
            final int type,
            final int nLocal,
            final Object[] local,
            final int nStack,
            final Object[] stack) {
        mv1.visitFrame(type, nLocal, local, nStack, stack);
        mv2.visitFrame(type, nLocal, local, nStack, stack);
    }

    public void visitInsn(final int opcode) {
        mv1.visitInsn(opcode);
        mv2.visitInsn(opcode);
    }

    public void visitIntInsn(final int opcode, final int operand) {
        mv1.visitIntInsn(opcode, operand);
        mv2.visitIntInsn(opcode, operand);
    }

    public void visitVarInsn(final int opcode, final int var) {
        mv1.visitVarInsn(opcode, var);
        mv2.visitVarInsn(opcode, var);
    }

    public void visitTypeInsn(final int opcode, final String desc) {
        mv1.visitTypeInsn(opcode, desc);
        mv2.visitTypeInsn(opcode, desc);
    }

    public void visitFieldInsn(
            final int opcode,
            final String owner,
            final String name,
            final String desc) {
        mv1.visitFieldInsn(opcode, owner, name, desc);
        mv2.visitFieldInsn(opcode, owner, name, desc);
    }

    public void visitMethodInsn(
            final int opcode,
            final String owner,
            final String name,
            final String desc) {
        mv1.visitMethodInsn(opcode, owner, name, desc);
        mv2.visitMethodInsn(opcode, owner, name, desc);
    }

    public void visitJumpInsn(final int opcode, final Label label) {
        mv1.visitJumpInsn(opcode, label);
        mv2.visitJumpInsn(opcode, label);
    }

    public void visitLabel(final Label label) {
        mv1.visitLabel(label);
        mv2.visitLabel(label);
    }

    public void visitLdcInsn(final Object cst) {
        mv1.visitLdcInsn(cst);
        mv2.visitLdcInsn(cst);
    }

    public void visitIincInsn(final int var, final int increment) {
        mv1.visitIincInsn(var, increment);
        mv2.visitIincInsn(var, increment);
    }

    public void visitTableSwitchInsn(
            final int min,
            final int max,
            final Label dflt,
            final Label labels[]) {
        mv1.visitTableSwitchInsn(min, max, dflt, labels);
        mv2.visitTableSwitchInsn(min, max, dflt, labels);
    }

    public void visitLookupSwitchInsn(
            final Label dflt,
            final int keys[],
            final Label labels[]) {
        mv1.visitLookupSwitchInsn(dflt, keys, labels);
        mv2.visitLookupSwitchInsn(dflt, keys, labels);
    }

    public void visitMultiANewArrayInsn(final String desc, final int dims) {
        mv1.visitMultiANewArrayInsn(desc, dims);
        mv2.visitMultiANewArrayInsn(desc, dims);
    }

    public void visitTryCatchBlock(
            final Label start,
            final Label end,
            final Label handler,
            final String type) {
        mv1.visitTryCatchBlock(start, end, handler, type);
        mv2.visitTryCatchBlock(start, end, handler, type);
    }

    public void visitLocalVariable(
            final String name,
            final String desc,
            final String signature,
            final Label start,
            final Label end,
            final int index) {
        mv1.visitLocalVariable(name, desc, signature, start, end, index);
        mv2.visitLocalVariable(name, desc, signature, start, end, index);
    }

    public void visitLineNumber(final int line, final Label start) {
        mv1.visitLineNumber(line, start);
        mv2.visitLineNumber(line, start);
    }

    public void visitMaxs(final int maxStack, final int maxLocals) {
        mv1.visitMaxs(maxStack, maxLocals);
        mv2.visitMaxs(maxStack, maxLocals);
    }

    public void visitEnd() {
        mv1.visitEnd();
        mv2.visitEnd();
    }

    public void visitInvokeDynamicInsn(String name, String desc,
            Handle bsm, Object... bsmArgs) {
        mv1.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        mv2.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
    }
}
