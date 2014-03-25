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

import org.objectweb.asm.tree.*;

import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.*;

/**
 * @author Dmitry Fazunenko
 * @author Alexey Fedorchenko
 */
class Instrumenter {

    private static InsnList instrumentation(int id, boolean detectInternal) {
        return instrumentation(id, 0, 0, detectInternal);
    }

    private static InsnList instrumentation(int id, int hash, int fullHash, boolean detectInternal) {
        InsnList il = new InsnList();
        if (hash != 0 || fullHash != 0) { // caller filter ON (hash & fullHash == 0 otherwise)
            il.add(new LdcInsnNode(id));
            il.add(new LdcInsnNode(hash));
            il.add(new LdcInsnNode(fullHash));
            il.add(new MethodInsnNode(INVOKESTATIC,
                    "com/sun/tdk/jcov/runtime/CollectDetect", "hit", "(III)V"));
        } else if (detectInternal) { // agent (hardcoded by default) or loaded, false otherwise
            il.add(new LdcInsnNode(id));
            il.add(new MethodInsnNode(INVOKESTATIC,
                    "com/sun/tdk/jcov/runtime/CollectDetect", "hit", "(I)V"));
        } else { // static
            il.add(new LdcInsnNode(id));
            il.add(new MethodInsnNode(INVOKESTATIC,
                    "com/sun/tdk/jcov/runtime/Collect", "hit", "(I)V"));
        }
        return il;
    }

    static InsnList instrumentation(DataBlock block, boolean detectInternal) {
        return instrumentation(block.getId(), detectInternal);
    }

    static InsnList instrumentation(SimpleBasicBlock block, boolean detectInternal) {
        return instrumentation(block.getId(), detectInternal);
    }

    static void visitInstrumentation(final MethodVisitor mv, int id, int hash, int fullHash, boolean detectInternal) {
        instrumentation(id, hash, fullHash, detectInternal).accept(mv);
    }
    /*
     static InsnList insertSavePoint() {
     InsnList il = new InsnList();
     il.add(new MethodInsnNode(INVOKESTATIC, "com/sun/tdk/jcov/runtime/Collect","saveResults", "(V)V"));
     return il;
     }
     */
}
