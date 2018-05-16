/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;

import static org.objectweb.asm.Opcodes.*;

public class Utils {
    private final static int[] SIMPLE_INSTRUCTIONS = new int[]{DUP, LDC,
            BALOAD, CALOAD, AALOAD, DALOAD, FALOAD, IALOAD, SALOAD,
            ACONST_NULL,
            ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5, ICONST_M1,
            LCONST_0, LCONST_1,
            FCONST_0, FCONST_1, FCONST_2,
            DCONST_0, DCONST_1,
            ALOAD, ILOAD, FLOAD, LLOAD, DLOAD,
            GETFIELD, GETSTATIC,
            BIPUSH, SIPUSH};
    private final static int[] INVOKE_INSTRUCTIONS = new int[]{INVOKEVIRTUAL, INVOKEINTERFACE, INVOKESTATIC,
            INVOKEDYNAMIC, INVOKESPECIAL};
    private final static int[] RETURN_INSTRUCTIONS = new int[]{RETURN, ARETURN, IRETURN, FRETURN, LRETURN, DRETURN};

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
    public static boolean isSimpleInstruction(int opCode) {
        return Arrays.binarySearch(SIMPLE_INSTRUCTIONS, opCode) >= 0;
    }
    public static boolean isReturnInstruction(int opCode) {
        return Arrays.binarySearch(RETURN_INSTRUCTIONS, opCode) >= 0;
    }
    public static boolean isInvokeInstruction(int opCode) {
        return Arrays.binarySearch(INVOKE_INSTRUCTIONS, opCode) >= 0;
    }
}
