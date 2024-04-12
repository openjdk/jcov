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

import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.InvokeInstruction;
import java.util.function.BiPredicate;

import static openjdk.jcov.filter.simplemethods.Utils.isSimpleInstruction;

public class Throwers implements BiPredicate<ClassModel, MethodModel> {
    @Override
    public boolean test(ClassModel cnode, MethodModel m) {
        if (m.code().isPresent()) {
            var iter = new InstructionIterator(m.code().get());
            //first should be NEW
            if(iter.next(i -> true).opcode() != Opcode.NEW) return false;
            //next is DUP
            if(iter.next(i -> true).opcode() != Opcode.DUP) return false;
            //next is a constructor call
            var next = iter.next(i -> !isSimpleInstruction(i.opcode()));
            if (next.opcode() != Opcode.INVOKESPECIAL) return false;
            if (next instanceof InvokeInstruction call) {
                if (!call.name().toString().equals("<init>")) return false;
            } else return false;
            //finally a throw
            return iter.next(i -> true).opcode() == Opcode.ATHROW;
        } else return false;
    }
}
