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

import com.sun.tdk.jcov.io.Reader;
import com.sun.tdk.jcov.util.DebugUtils;
import org.objectweb.asm.*;
import org.objectweb.asm.util.TraceClassVisitor;

/**
 * @author Dmitry Fazunenko
 * @author Alexey Fedorchenko
 */
public class ClassMorph2 {

    private final InstrumentationParams params;

    public ClassMorph2(final InstrumentationParams params, String template) {
        this.params = params;
        try {

            DataRoot root = Reader.readXML(template, true, null);
            for (DataPackage pack : root.getPackages()) {
                for (DataClass clazz : pack.getClasses()) {
                    for (DataMethod meth : clazz.getMethods()) {
                        if (meth.access(meth.getAccess()).matches(".*abstract.*")
                                || meth.access(meth.getAccess()).matches(".*native.*")) {
                            int id = 0;
                            if (meth instanceof DataMethodInvoked) {
                                id = ((DataMethodInvoked) meth).getId();
                            } else if (meth instanceof DataMethodEntryOnly) {
                                id = ((DataMethodEntryOnly) meth).getId();
                            } else {
                                DataMethodWithBlocks mb = (DataMethodWithBlocks) meth;
                                for (BasicBlock bb : mb.getBasicBlocks()) {
                                    for (DataBlock db : bb.blocks()) {
                                        id = db.getId();
                                        break;
                                    }
                                    break;
                                }
                            }

                            String className = pack.getName().equals("") ? clazz.getName()
                                    : pack.getName().replace('.', '/') + "/" + clazz.getName();
                            StaticInvokeMethodAdapter.addID(
                                    className, meth.getName(), meth.getVmSignature(), id);

                        }
                    }
                    for (DataField fld : clazz.getFields()) {
                        int id = fld.getId();
                        String className = pack.getName().equals("") ? clazz.getName()
                                : pack.getName().replace('.', '/') + "/" + clazz.getName();
                        StaticInvokeMethodAdapter.addID(
                                className, fld.getName(), fld.getVmSig(), id);
                    }
                }
            }

        } catch (Throwable t) {
            throw new Error(t);
        }
    }

    public boolean isTransformed(String className) {
        return className != null
                && !className.startsWith("com/sun/tdk/jcov")
                && !className.startsWith("org/objectweb/asm");
    }

    public boolean shouldTransform(String className) {
        return isTransformed(className)
                && params.isIncluded(className);
    }

    public byte[] morph(byte[] classfileBuffer, String flushPath) {
        ClassReader cr = new ClassReader(classfileBuffer);
        String fullname = cr.getClassName();

        if (!isTransformed(fullname)) {
            return null;
        }
        if (!shouldTransform(fullname)) {
            return null;
        }

        // Consider manually optimizing out the ClassWriter.COMPUTE_FRAMES
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);

        boolean shouldFlush = (flushPath != null);
        ClassVisitor cv = shouldFlush ? new TraceClassVisitor(cw, DebugUtils.getPrintWriter(fullname, flushPath))
                : cw;
        cv = new InvokeClassAdapter(cv, params) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
                MethodVisitor instr = new StaticInvokeMethodAdapter(mv, className, name, access, params);
                return instr;
            }
        };

        cr.accept(cv, 0);

        return cw.toByteArray();
    }
}
