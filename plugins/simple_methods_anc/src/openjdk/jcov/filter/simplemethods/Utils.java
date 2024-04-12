/*
 * Copyright (c) 2018, 2024 Oracle and/or its affiliates. All rights reserved.
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
package openjdk.jcov.filter.simplemethods;

import java.lang.classfile.Opcode;
import java.util.Arrays;

import static java.lang.classfile.Opcode.*;

public class Utils {
    private final static Opcode[] SIMPLE_INSTRUCTIONS = new Opcode[]{
            DUP, LDC,
            BALOAD, CALOAD, AALOAD, DALOAD, FALOAD, IALOAD, SALOAD,
            ACONST_NULL,
            ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5, ICONST_M1,
            LCONST_0, LCONST_1,
            FCONST_0, FCONST_1, FCONST_2,
            DCONST_0, DCONST_1,
            ALOAD, ALOAD_0, ALOAD_1, ALOAD_2, ALOAD_3, ALOAD_W,
            ILOAD, ILOAD_0, ILOAD_1, ILOAD_2, ILOAD_3, ILOAD_W,
            FLOAD, FLOAD_0, FLOAD_1, FLOAD_2, FLOAD_3, FLOAD_W,
            LLOAD, LLOAD_0, LLOAD_1, LLOAD_2, LLOAD_3, LLOAD_W,
            DLOAD, DLOAD_0, DLOAD_1, DLOAD_2, DLOAD_3, DLOAD_W,
            GETFIELD, GETSTATIC,
            BIPUSH, SIPUSH};
    private final static Opcode[] INVOKE_INSTRUCTIONS = new Opcode[]{INVOKEVIRTUAL, INVOKEINTERFACE, INVOKESTATIC,
            INVOKEDYNAMIC, INVOKESPECIAL};
    private final static Opcode[] RETURN_INSTRUCTIONS = new Opcode[]{RETURN, ARETURN, IRETURN, FRETURN, LRETURN, DRETURN};

    static {
        Arrays.sort(SIMPLE_INSTRUCTIONS);
        Arrays.sort(INVOKE_INSTRUCTIONS);
        Arrays.sort(RETURN_INSTRUCTIONS);
    }

    /**
     * An instruction is called "simple" if its only effect is to bring values onto the stack from stack, variables, fields, constants, etc.
     * @param opCode
     * @return
     */
    public static boolean isSimpleInstruction(Opcode opCode) {
        return Arrays.binarySearch(SIMPLE_INSTRUCTIONS, opCode) >= 0;
    }
    public static boolean isReturnInstruction(Opcode opCode) {
        return Arrays.binarySearch(RETURN_INSTRUCTIONS, opCode) >= 0;
    }
    public static boolean isInvokeInstruction(Opcode opCode) {
        return Arrays.binarySearch(INVOKE_INSTRUCTIONS, opCode) >= 0;
    }

}
