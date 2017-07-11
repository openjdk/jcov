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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import static org.objectweb.asm.Opcodes.*;

/**
 * @author Leonid Mesnik
 * @author Sergey Borodin
 *
 * Used in dynamic instrumentation mode. It handles method invocations - sets
 * instructions to serve caller include options (setExpected[Refl] instructions)
 * and checks all virtual invocations. In contrast to StaticInvokeMethodAdapter,
 * it hits CollectDetect slots and does it for all such invocations, not
 * filtering abstract and native invocations (we do not see access flags in
 * meth. invoke instruction)
 *
 */
public class InvokeMethodAdapter extends MethodVisitor {

    static volatile int invokeCount = 0;
    private final String className;
    private final InstrumentationParams params;

    public static int getInvokeID(String owner, String name, String descr) {
        String sig = owner + "." + name + descr;
        synchronized (map) {
            Integer id = map.get(sig);
            if (id != null) {
                return id;
            }
            //return 0;
            id = invokeCount++;
            map.put(sig, id);
            return id;
        }
    }
    final private static Map<String, Integer> map =
            Collections.synchronizedMap(new HashMap<String, Integer>());

    public InvokeMethodAdapter(MethodVisitor mv, String className, final InstrumentationParams params) {
        super(ASM6, mv);
        this.className = className;
        this.params = params;
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        if ((opcode == GETFIELD || opcode == GETSTATIC)
                && params.isInstrumentFields() && params.isIncluded(owner)
                && params.isCallerFilterAccept(className)) {
            InsnList il = new InsnList();
            il.add(new LdcInsnNode(getInvokeID(owner, name, desc)));
            il.add(new MethodInsnNode(INVOKESTATIC, "com/sun/tdk/jcov/runtime/CollectDetect", "invokeHit", "(I)V"));
            il.accept(this);
        }
        super.visitFieldInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (params.isCallerFilterOn()
                && params.isCallerFilterAccept(className)) {

            if (ReflPair.contains(owner, name)) {
                //handle reflection invokations
                visitReflectionCI(ReflPair.valueOf(owner, name));
            } else {
                int id = (name + desc).hashCode();
                super.visitLdcInsn(id);
                super.visitMethodInsn(INVOKESTATIC, "com/sun/tdk/jcov/runtime/CollectDetect", "setExpected", "(I)V", false);
            }
        }

        if ((opcode == INVOKEVIRTUAL || opcode == INVOKEINTERFACE)
                && params.isInstrumentAbstract()
                && params.isIncluded(owner)
                && params.isCallerFilterAccept(className)) {
            super.visitLdcInsn(getInvokeID(owner, name, desc));
            super.visitMethodInsn(INVOKESTATIC, "com/sun/tdk/jcov/runtime/CollectDetect", "invokeHit", "(I)V", false);
        }
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    private enum ReflPair {

        CLASS("java/lang/Class", "newInstance"),
        METHOD("java/lang/reflect/Method", "invoke"),
        CONSTRUCTOR("java/lang/reflect/Constructor", "newInstance");
        private String className;
        private String methName;

        ReflPair(String className, String methName) {
            this.className = className;
            this.methName = methName;
        }

        private boolean isEqual(String clName, String mName) {
            return className.equals(clName) && methName.equals(mName);
        }

        public static boolean contains(String clName, String mName) {
            return valueOf(clName, mName) != null;
        }

        public static ReflPair valueOf(String clName, String mName) {
            for (ReflPair p : values()) {
                if (p.isEqual(clName, mName)) {
                    return p;
                }
            }
            return null;

        }
    }

    private void visitReflectionCI(ReflPair p) {
        if (p == null) {
            return;
        }

        switch (p) {
            case CLASS:
                super.visitInsn(DUP);

                super.visitMethodInsn(INVOKESTATIC, "com/sun/tdk/jcov/instrument/InvokeMethodAdapter",
                        "getMethodHash", "(Ljava/lang/Object;)I");
                super.visitMethodInsn(INVOKESTATIC, "com/sun/tdk/jcov/runtime/CollectDetect",
                        "setExpectedFull", "(I)V");
                break;
            case METHOD:
                super.visitInsn(DUP2_X1);
                super.visitInsn(POP);
                super.visitInsn(POP);
                super.visitInsn(DUP);

                super.visitMethodInsn(INVOKESTATIC, "com/sun/tdk/jcov/instrument/InvokeMethodAdapter",
                        "getMethodHash", "(Ljava/lang/Object;)I");
                super.visitMethodInsn(INVOKESTATIC, "com/sun/tdk/jcov/runtime/CollectDetect",
                        "setExpectedFull", "(I)V");
                super.visitInsn(DUP_X2);
                super.visitInsn(POP);
                break;
            case CONSTRUCTOR:
                super.visitInsn(DUP2);
                super.visitInsn(POP);

                super.visitMethodInsn(INVOKESTATIC, "com/sun/tdk/jcov/instrument/InvokeMethodAdapter",
                        "getMethodHash", "(Ljava/lang/Object;)I");
                super.visitMethodInsn(INVOKESTATIC, "com/sun/tdk/jcov/runtime/CollectDetect",
                        "setExpectedFull", "(I)V");
                break;
            default:
                break;
        }
    }

    public static int getMethodHash(Object obj) {
        String desc = "";
        if (obj instanceof Method) {
            Method m = (Method) obj;
            Class c = m.getDeclaringClass();
            desc = c.getName().replace(".", "/");

            desc += m.getName();

            Class[] types = m.getParameterTypes();
            desc += "(";
            for (Class t : types) {
                desc += vmType(t.getName());
            }
            desc += ")";
            desc += vmType(m.getReturnType().getName());
        } else if (obj instanceof Constructor) {
            Constructor c = (Constructor) obj;
            Class cl = c.getDeclaringClass();
            desc = cl.getName().replace(".", "/");

            desc += "<init>";
            Class[] types = c.getParameterTypes();
            desc += "(";
            for (Class t : types) {
                desc += vmType(t.getName());
            }
            desc += ")";
            desc += "V";
        } else if (obj instanceof Class) {
            Class c = (Class) obj;
            desc = c.getName().replace(".", "/");
            desc += "<init>" + "()V";
        }

        return desc.hashCode();
    }

    private static String vmType(String type) {
        //      [<s>  -> <s>[]      <s> is converted recursively
        //      L<s>; -> <s>        characters '/' are replaced by '.' in <s>
        //      B     -> byte
        //      C     -> char
        //      D     -> double
        //      F     -> float
        //      I     -> int
        //      J     -> long
        //      S     -> short
        //      Z     -> boolean
        //      V     -> void       valid only in method return type
        String res = "";
        if (type.equals("")) {
            return "V";
        }

        while (type.endsWith("[]")) {
            type = type.substring(0, type.length() - 2);
            res += "[";
        }

        while (type.startsWith("[")) {
            res += '[';
            type = type.substring(1);
        }
        if (type.equals("byte")) {
            res += "B";
        } else if (type.equals("char")) {
            res += "C";
        } else if (type.equals("double")) {
            res += "D";
        } else if (type.equals("float")) {
            res += "F";
        } else if (type.equals("int")) {
            res += "I";
        } else if (type.equals("long")) {
            res += "J";
        } else if (type.equals("short")) {
            res += "S";
        } else if (type.equals("boolean")) {
            res += "Z";
        } else if (type.equals("void")) {
            res += "V";
        } else {
            type = type.replace(".", "/");
            if (!type.startsWith("L")) {
                type = "L" + type;
            }
            res += type;
            if (!type.endsWith(";")) {
                res += ";";
            }
        }

        return res;
    }

    //never used
    public static void addID(String className, String name, String descr, int id) {
        String sig = className + "." + name + descr;
        map.put(sig, id);
    }
}
