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

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.function.BiPredicate;

import static java.util.Arrays.binarySearch;
import static openjdk.jcov.filter.simplemethods.Utils.isInvokeInstruction;
import static openjdk.jcov.filter.simplemethods.Utils.isReturnInstruction;
import static openjdk.jcov.filter.simplemethods.Utils.isSimpleInstruction;

public class Delegators implements BiPredicate<ClassNode, MethodNode> {

    private final boolean sameNameDelegationOnly;

    public Delegators(boolean sameNameDelegationOnly) {
        this.sameNameDelegationOnly = sameNameDelegationOnly;
    }

    public Delegators() {
        this(false);
    }

    /**
     * Identifies simple delegation. A simple delegator is a method obtaining any number of values with a "simple" code
     * and then calling a method with the obtained values.
     * @see Utils#isSimpleInstruction(int)
     */
    @Override
    public boolean test(ClassNode clazz, MethodNode m) {
        int index = 0;
        int opCode = -1;
        //skip all instructions allowed to get values
        for(; index < m.instructions.size(); index++) {
            opCode = m.instructions.get(index).getOpcode();
            if(opCode >=0) {
                if (!isSimpleInstruction(opCode)) {
                    break;
                }
            }
        }
        //that should be an invocation instruction
        if(!isInvokeInstruction(opCode)) {
            return false;
        }
        if(sameNameDelegationOnly) {
            //check name
            AbstractInsnNode node = m.instructions.get(index);
            String name;
            if (node instanceof MethodInsnNode) {
                name = ((MethodInsnNode) node).name;
            } else if (node instanceof InvokeDynamicInsnNode) {
                name = ((InvokeDynamicInsnNode) node).name;
            } else {
                throw new IllegalStateException("Unknown node type: " + node.getClass().getName());
            }
            if(!m.name.equals(name)) {
                return false;
            }
        }
        //scroll to next instruction
        for(index++; index < m.instructions.size(); index++) {
            opCode = m.instructions.get(index).getOpcode();
            if(opCode >=0) {
                break;
            }
        }
        //that should be a return instruction
        return isReturnInstruction(opCode);
    }
}
