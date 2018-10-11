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

import com.sun.tdk.jcov.util.Utils;
import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import static org.objectweb.asm.Opcodes.*;

/*
 * This class serves abstract, native method and field access coverage
 * functionality for static instrumentation.
 * Works inside Instr2 process - at the beginning mapping of whole signatures
 * to collector's slots (not CollectDetect slots!) is filled from template - see
 * ClassMorph2 for details. Then, if this method adapter finds virtual invocations
 * of methods or fields from map, it hits respective slot.
 *
 * @author Dmitry Fazunenko
 * @author Alexey Fedorchenko
 **/
public class StaticInvokeMethodAdapter extends MethodVisitor {

    static int invokeCount = 0;
    private final String className;
    private final InstrumentationParams params;
    private String methName;

    public static int getInvokeID(String owner, String name, String descr) {
        String sig = owner + "." + name + descr;
        Integer id = map.get(sig);
        if (id != null) {
            return id;
        }

        sig = owner + "." + name;
        id = map.get(sig);
        if (id != null) {
            return id;
        }
        return -1;
    }
    public static final Map<String, Integer> map = new HashMap<String, Integer>();

    public StaticInvokeMethodAdapter(MethodVisitor mv, String className, String methName, int access, final InstrumentationParams params) {
        super(Utils.ASM_API_VERSION, mv);
        this.className = className;
        this.params = params;
        this.methName = methName;
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        if (params.isInstrumentFields() && params.isIncluded(owner) && params.isCallerFilterAccept(className)) {
            if (getInvokeID(owner, name, desc) != -1) {
                int id = getInvokeID(owner, name, desc);
                InsnList il = new InsnList();
                il.add(new LdcInsnNode(id));
                il.add(new MethodInsnNode(INVOKESTATIC,
                        "com/sun/tdk/jcov/runtime/Collect", "hit", "(I)V"));

                il.accept(this);
            }
        }
        super.visitFieldInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {

        if ((params.isInstrumentAbstract() || params.isInstrumentNative()) &&
                params.isIncluded(owner) || params.isCallerFilterAccept(owner)) {
            if (getInvokeID(owner, name, desc) != -1) {
                int id = getInvokeID(owner, name, desc);
                InsnList il = new InsnList();
                il.add(new LdcInsnNode(id));
                il.add(new MethodInsnNode(INVOKESTATIC,
                        "com/sun/tdk/jcov/runtime/Collect", "hit", "(I)V", false));
                il.accept(this);
            }
        }
        if (params.isCallerFilterOn()
                && params.isCallerFilterAccept(className)) {

            int id = (name + desc).hashCode();
            super.visitLdcInsn(id);
            super.visitMethodInsn(INVOKESTATIC, "com/sun/tdk/jcov/runtime/CollectDetect", "setExpected", "(I)V", false);

        }

        if (params.isInnerInvacationsOff() && Utils.isAdvanceStaticInstrAllowed(className, name)) {
            if (!owner.equals("java/lang/Object") && params.isInnerInstrumentationIncludes(className)) {
                int id = -1;
                super.visitLdcInsn(id);
                super.visitMethodInsn(INVOKESTATIC, "com/sun/tdk/jcov/runtime/CollectDetect", "setExpected", "(I)V", false);

            }
        }

        super.visitMethodInsn(opcode, owner, name, desc, itf);

    }

    @Override
    public void visitInsn(int opcode) {

        switch (opcode) {
            case Opcodes.IRETURN:
            case Opcodes.FRETURN:
            case Opcodes.ARETURN:
            case Opcodes.LRETURN:
            case Opcodes.DRETURN:
            case Opcodes.RETURN:
                if (params.isInnerInvacationsOff() && Utils.isAdvanceStaticInstrAllowed(className, methName/*"<init>"*/)) {
                    if (!methName.equals("<clinit>")) {
                        int id = 0;
                        super.visitLdcInsn(id);
                        super.visitMethodInsn(INVOKESTATIC, "com/sun/tdk/jcov/runtime/CollectDetect", "setExpected", "(I)V");
                    } else {
                        super.visitMethodInsn(INVOKESTATIC, "com/sun/tdk/jcov/runtime/CollectDetect", "leaveClinit", "()V");
                    }
                }
                break;
            default:
                break;
        }

        super.visitInsn(opcode);
    }

    public static void addID(String className, String name, String descr, int id) {
        String sig = className + "." + name + descr;
        map.put(sig, id);
    }
}
