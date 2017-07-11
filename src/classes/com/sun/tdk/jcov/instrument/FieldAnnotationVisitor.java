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
import org.objectweb.asm.Attribute;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Field visitor collecting runtime annotations
 *
 * @author Dmitry Fazunenko
 */
class FieldAnnotationVisitor extends FieldVisitor {

    final DataField field;
    final FieldVisitor fv;

    FieldAnnotationVisitor(final FieldVisitor fv, final DataField field) {
        super(Opcodes.ASM6, fv);
        this.fv = fv;
        this.field = field;
    }

    public void visitAttribute(Attribute arg0) {
        fv.visitAttribute(arg0);
    }

    public void visitEnd() {
        fv.visitEnd();
    }

    public AnnotationVisitor visitAnnotation(String anno, boolean b) {
        field.addAnnotation(anno);
        return fv.visitAnnotation(anno, b);
    }
}
