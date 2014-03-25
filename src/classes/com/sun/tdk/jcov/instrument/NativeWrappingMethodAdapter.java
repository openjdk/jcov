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

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ClassVisitor;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.tree.*;

/**
 * NativeWrappingMethodAdapter
 *
 * Write the native method (renamed with a prefix) by simply passing it on to
 * the forking adapter. The forking adapter is also handed a MethodNode so that
 * it builds a tree representation of the method. Add instrumentation and a call
 * to the native to make the wrapper.
 *
 * @author Robert Field
 */
class NativeWrappingMethodAdapter extends ForkingMethodAdapter {

    private final ClassVisitor cv;
    private final MethodNode methodNode;
    private final DataMethodEntryOnly dataMethod;
    private final InstrumentationParams params;

    NativeWrappingMethodAdapter(final MethodVisitor mv,
            final MethodNode methodNode, final ClassVisitor cv,
            final DataMethodEntryOnly dataMethod, final InstrumentationParams params) {
        super(mv, methodNode);
        this.cv = cv;
        this.dataMethod = dataMethod;
        this.methodNode = methodNode;
        this.params = params;
    }

    private int computeMaxLocals(String descriptor, int accessFlags) {
        int index = 1;
        int slot = 0;

        if ((accessFlags & ACC_STATIC) == 0) {
            ++slot;
        }
        char type;
        while ((type = descriptor.charAt(index)) != ')') {
            switch (type) {
                case 'B': // byte
                case 'C': // char
                case 'I': // int
                case 'S': // short
                case 'Z': // boolean
                case 'F': // float
                case 'L': // object
                case '[': // array
                    ++slot;
                    break;
                case 'D': // double
                case 'J': // long
                    slot += 2;
                    break;
                default:
                    break;
            }
            index = nextDescriptorIndex(descriptor, index);
        }

        return slot;
    }

    private int nextDescriptorIndex(String descriptor, int index) {
        switch (descriptor.charAt(index)) {
            case 'B': // byte
            case 'C': // char
            case 'I': // int
            case 'S': // short
            case 'Z': // boolean
            case 'F': // float
            case 'D': // double
            case 'J': // long
                return index + 1;
            case 'L': // object
                int i = index + 1;
                while (descriptor.charAt(i) != ';') {
                    ++i;
                }
                return i + 1;
            case '[': // array
                return nextDescriptorIndex(descriptor, index + 1);
            default:
                break;
        }
        throw new InternalError("should not reach here");
    }

    @Override
    public void visitEnd() {
        super.visitEnd();

        InsnList instructions = methodNode.instructions;
        String descriptor = methodNode.desc;

        // set max locals and max stack
        int maxLocals = computeMaxLocals(descriptor, methodNode.access);
        methodNode.maxLocals = maxLocals;
        methodNode.maxStack = maxLocals + 4;

        // set-up args

        int slot = 0;
        if ((methodNode.access & ACC_STATIC) == 0) {
            // "this"
            instructions.add(new VarInsnNode(ALOAD, slot));
            ++slot;
        }
        char type;
        int index;
        for (index = 1;
                (type = descriptor.charAt(index)) != ')';
                index = nextDescriptorIndex(descriptor, index)) {
            switch (type) {
                case 'B': // byte
                case 'C': // char
                case 'I': // int
                case 'S': // short
                case 'Z': // boolean
                    instructions.add(new VarInsnNode(ILOAD, slot));
                    ++slot;
                    break;
                case 'F': // float
                    instructions.add(new VarInsnNode(FLOAD, slot));
                    ++slot;
                    break;
                case 'D': // double
                    instructions.add(new VarInsnNode(DLOAD, slot));
                    slot += 2;
                    break;
                case 'J': // long
                    instructions.add(new VarInsnNode(LLOAD, slot));
                    slot += 2;
                    break;
                case 'L': // object
                case '[': // array
                    instructions.add(new VarInsnNode(ALOAD, slot));
                    ++slot;
                    break;
                default:
                    break;
            }
        }

        // call the wrapped version
        int invokeOp;
        if ((methodNode.access & ACC_STATIC) == 0) {
            invokeOp = INVOKEVIRTUAL;
        } else {
            invokeOp = INVOKESTATIC;
        }
        instructions.add(new MethodInsnNode(invokeOp, dataMethod.getParent().getFullname(), InstrumentationOptions.nativePrefix + dataMethod.getName(), dataMethod.getVmSignature()));

        // return correct type
        switch (descriptor.charAt(index + 1)) {
            case 'B': // byte
            case 'C': // char
            case 'I': // int
            case 'S': // short
            case 'Z': // boolean
                instructions.add(new InsnNode(IRETURN));
                break;
            case 'F': // float
                instructions.add(new InsnNode(FRETURN));
                break;
            case 'D': // double
                instructions.add(new InsnNode(DRETURN));
                break;
            case 'J': // long
                instructions.add(new InsnNode(LRETURN));
                break;
            case 'L': // object
            case '[': // array
                instructions.add(new InsnNode(ARETURN));
                break;
            case 'V': // void
                instructions.add(new InsnNode(RETURN));
                break;
            default:
                break;
        }

        String[] exceptions = (String[]) methodNode.exceptions.toArray(new String[0]);
        MethodVisitor mvn = cv.visitMethod(
                methodNode.access,
                methodNode.name,
                methodNode.desc,
                methodNode.signature,
                exceptions);

        //instrument wrapper
        EntryCodeMethodAdapter ecma = new EntryCodeMethodAdapter(mvn, dataMethod, params);
        methodNode.accept(ecma);
    }
}
