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
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * @author Dmitry Fazunenko
 * @author Alexey Fedorchenko
 */
class EntryCodeMethodAdapter extends MethodVisitor {

    private final DataMethodEntryOnly method;
    boolean isLineNumberVisited = false;
    private final InstrumentationParams params;

    EntryCodeMethodAdapter(final MethodVisitor mv,
            final DataMethodEntryOnly method,
            final InstrumentationParams params) {
        super(ASMUtils.ASM_API_VERSION, mv);
        this.method = method;
        this.params = params;
    }

    @Override
    public void visitCode() {
        super.visitCode();
        int id = 0;
        int fullId = 0;

        if (params.isCallerFilterOn()) {
            if (params.isDynamicCollect() || Utils.isAdvanceStaticInstrAllowed(method.getParent().getFullname(), method.getName())) {
                id = (method.getName() + method.getVmSignature()).hashCode();
                fullId = (method.getParent().getFullname() + method.getName() + method.getVmSignature()).hashCode();
            }
        }
        if (params.isInnerInvacationsOff() && Utils.isAdvanceStaticInstrAllowed(method.getParent().getFullname(), method.getName())) {
            id = -1;
            fullId = -1;
            if (method.getName().equals("<clinit>")) {
                id = 1; //do not count clinit methods like outinvocations
            }
        }

        Instrumenter.visitInstrumentation(mv, method.getId(), id, fullId, params.isDetectInternal());
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(maxStack < 3 ? 3 : maxStack, maxLocals);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        if (!isLineNumberVisited) {
            method.addLineEntry(0, line);
            isLineNumberVisited = true;
        }
        super.visitLineNumber(line, start);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String anno, boolean b) {
        method.addAnnotation(anno);
        return super.visitAnnotation(anno, b);
    }
}
