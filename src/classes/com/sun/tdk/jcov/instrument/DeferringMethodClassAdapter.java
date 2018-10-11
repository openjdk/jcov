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
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.*;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Dmitry Fazunenko
 * @author Alexey Fedorchenko
 */
class DeferringMethodClassAdapter extends ClassVisitor {

    private final DataClass k;
    private final InstrumentationParams params;

    public DeferringMethodClassAdapter(final ClassVisitor cv, DataClass k, InstrumentationParams params) {
        super(Utils.ASM_API_VERSION, cv);
        this.k = k;
        this.params = params;
    }

    @Override
    public void visit(
            final int version,
            final int access,
            final String name,
            final String signature,
            final String superName,
            final String[] interfaces) {
        k.setInfo(access, signature, superName, interfaces);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        // the (access & ACC_STATIC) == 0 || value ==null means
        // that class is not substitued by value  during comilation
        FieldVisitor fv = super.visitField(access, name, desc, signature, value);
        if (params.isInstrumentFields() && ((access & ACC_STATIC) == 0 || value == null)) {
            DataField fld = new DataField(k, access, name, desc, signature, value);
            fv = new FieldAnnotationVisitor(fv, fld);
            // this is needed, because DataField contains adding to DataClass
        }
        return fv;
    }

    @Override
    public void visitSource(String source, String debug) {
        k.setSource(source);
        super.visitSource(source, debug);
    }

    public MethodVisitor visitMethodCoverage(
            final int access,
            final String name,
            final String desc,
            final String signature,
            final String[] exceptions) {
        if (!InstrumentationOptions.isSkipped(k.getFullname(), name, access)
                && params.isDynamicCollect()
                && params.isInstrumentNative()
                && (access & ACC_NATIVE) != 0
                && !"java/lang/invoke/VarHandle".equals(k.getFullname())) {
            // Visit the native method, but change the access flags and rename it with a prefix
            int accessNative = access;
            if ((accessNative & ACC_STATIC) == 0) {
                // not static, then it needs to be final
                accessNative |= ACC_FINAL;
            }
            accessNative &= ~(ACC_PUBLIC | ACC_PROTECTED);
            accessNative |= ACC_PRIVATE;
            MethodVisitor mvNative = cv.visitMethod(
                    accessNative,
                    InstrumentationOptions.nativePrefix + name,
                    desc,
                    signature,
                    exceptions);
            if (mvNative == null) {
                throw new InternalError("Should not happen!");
            }

            // Write the native method, then visit and write the wrapper method
            MethodNode methodWrapper = new MethodNode(access & ~ACC_NATIVE, name, desc, signature, exceptions);
            DataMethodEntryOnly meth = new DataMethodEntryOnly(k, access, name, desc, signature, exceptions);
            return new NativeWrappingMethodAdapter(mvNative, methodWrapper, cv, meth, params);
        }
        MethodVisitor mv = cv.visitMethod(access,
                name,
                desc,
                signature,
                exceptions);
        //This code is executed both in dynamic and static mode
        if (InstrumentationOptions.isSkipped(k.getFullname(), name, access) || mv == null
                || (access & ACC_ABSTRACT) != 0
                || (access & ACC_NATIVE) != 0) {
            // build method with no content, and we are done
            if ((access & ACC_ABSTRACT) != 0 && params.isInstrumentAbstract()
                    || ((access & ACC_NATIVE) != 0 && params.isInstrumentNative())
                    || InstrumentationOptions.isSkipped(k.getFullname(), name, access)) {
                DataMethod meth = new DataMethodInvoked(k, access, name, desc, signature, exceptions);
                return new MethodAnnotationAdapter(mv, meth);
            }
            return mv;
        }

        // method could not be instrumented
        // java.lang.VerifyError: (class: sun/awt/X11/XWindowPeer, method: handleButtonPressRelease signature: (IJ)V) Mismatched stack types
        // temporary use only method coverage
        if (k.getFullname().equals("sun/awt/X11/XWindowPeer")
                && name.equals("handleButtonPressRelease")) {
            DataMethodEntryOnly meth = new DataMethodEntryOnly(k, access, name, desc, signature, exceptions);
            return new EntryCodeMethodAdapter(mv, meth, params);
        }


        switch (params.getMode()) {
            case METHOD: {
                DataMethodEntryOnly meth = new DataMethodEntryOnly(k, access, name, desc, signature, exceptions);
                return new EntryCodeMethodAdapter(mv, meth, params);
            }
            case BLOCK: {
                DataMethodWithBlocks meth = new DataMethodWithBlocks(k, access, name, desc, signature, exceptions);
                return new BlockCodeMethodAdapter(mv, meth, params);
            }
            case BRANCH: {
                DataMethodWithBlocks meth = new DataMethodWithBlocks(k, access, name, desc, signature, exceptions);
                return new BranchCodeMethodAdapter(mv, meth, params);
            }
            default:
                break;
        }

        return null;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (!params.isInstrumentSynthetic() && (access & ACC_SYNTHETIC) != 0) {
            return super.visitMethod(access, name, desc, signature, exceptions);
//            return null;
        }
        MethodVisitor mv = visitMethodCoverage(access, name, desc, signature, exceptions);

        if ("<clinit>".equals(name) && !params.isDynamicCollect() && (k.getPackageName().startsWith("java/lang/"))) {
            mv = new MethodVisitor(Utils.ASM_API_VERSION, mv) {
                public void visitCode() {
                    mv.visitMethodInsn(INVOKESTATIC, "com/sun/tdk/jcov/runtime/Collect", "init", "()V");
                    super.visitCode();
                }
            };
        }

        if (params.isCallerFilterOn()
                && params.isCallerFilterAccept(k.getFullname())
                && !params.isDynamicCollect()) {

            if (name.equals("<clinit>")) {
                int id = (name + desc).hashCode();
                mv.visitLdcInsn(id);
                mv.visitMethodInsn(INVOKESTATIC, "com/sun/tdk/jcov/runtime/CollectDetect", "setExpected", "(I)V");
            }

        }

        if (params.isInnerInvacationsOff() && Utils.isAdvanceStaticInstrAllowed(k.getFullname(), name)) {

            if (name.equals("<clinit>")) {
                mv.visitMethodInsn(INVOKESTATIC, "com/sun/tdk/jcov/runtime/CollectDetect", "enterClinit", "()V");
            }

        }

        // System.out.println("Seeing " + k.fullname + "." + name);
        if (params.isDataSaveFilterAccept(k.getFullname(), name, true)) {
            mv = new SavePointsMethodAdapter(mv, true);
        }

        if (params.isDataSaveFilterAccept(k.getFullname(), name, false)) {
            mv = new SavePointsMethodAdapter(mv, false);
        }
        mv = new MethodVisitor(Utils.ASM_API_VERSION, mv) {
            @Override
            public void visitLocalVariable(String arg0, String arg1, String arg2, Label arg3, Label arg4, int arg5) {
                //super.visitLocalVariable(arg0, arg1, arg2, arg3, arg4, arg5);
            }
        };
        if (params.isDynamicCollect()) {
            mv = new InvokeMethodAdapter(mv, k.getFullname(), params);
        } else {
            mv = new StaticInvokeMethodAdapter(mv, k.getFullname(), name, access, params);
        }
        return mv;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String anno, boolean b) {
        k.addAnnotation(anno);
        return super.visitAnnotation(anno, b);
    }

    @Override
    public void visitInnerClass(String fullClassName, String parentClass, String className, int i) {
        if (!params.isInstrumentAnonymous()) {
            return; // ignore
        }
        // fullClassName can't be null - it's generated if the class is anonym
        // className is individual name as written in the source - anonym classes have null there
        try {
            if (k.getFullname().equals(fullClassName)) {
                k.setInner(true);
                k.setAnonym(className == null);
            }
        } catch (Exception e) {
        }
        super.visitInnerClass(fullClassName, parentClass, className, i);
    }
}
