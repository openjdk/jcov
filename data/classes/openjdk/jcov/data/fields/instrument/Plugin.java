/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
package openjdk.jcov.data.fields.instrument;

import jdk.internal.classfile.ClassBuilder;
import jdk.internal.classfile.CodeBuilder;
import jdk.internal.classfile.Instruction;
import jdk.internal.classfile.MethodModel;
import jdk.internal.classfile.Opcode;
import jdk.internal.classfile.instruction.FieldInstruction;
import openjdk.jcov.data.fields.runtime.Collect;
import openjdk.jcov.data.fields.runtime.Coverage;
import openjdk.jcov.data.fields.runtime.Runtime;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class Plugin extends openjdk.jcov.data.instrument.Plugin {
    public Plugin() throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        Runtime.init();
    }
    @Override
    public Map<String, Consumer<OutputStream>> complete() throws Exception {
        //this plugin does not generate a template during the instrumentation
        //the template needs to be generated before - such as by TemplateGenerator class
        return Map.of();
    }

    private static final Set<Class> runtime = Set.of(
            Collect.class, Coverage.class, Runtime.class, Runtime.CompletionTask.class
    );

    @Override
    public Set<Class> runtime() {
        return runtime;
    }

    @Override
    public void instrument(ClassBuilder clb, MethodModel mm) throws Exception {
        if (methodFilter.accept(mm.flags().flagsMask(), mm.parent().get().thisClass().name().stringValue(),
                mm.methodName().stringValue(), mm.methodType().stringValue())) {
            clb.withMethod(mm.methodName().stringValue(), mm.methodTypeSymbol(), mm.flags().flagsMask(), mb -> {
                mb.withCode(cb -> {
                    mm.code().get().elementList().forEach(element -> {
                        if(element instanceof FieldInstruction) {
                            FieldInstruction instruction = (FieldInstruction) element;
                            if (instruction.opcode().equals(Opcode.PUTFIELD) || instruction.opcode().equals(Opcode.PUTSTATIC)) {
                                try {
                                    if (Collect.data().contains(
                                            instruction.owner().name().stringValue(),
                                            instruction.name().stringValue())) {
                                        String vmType = instruction.type().stringValue();
                                        if (vmType.startsWith("L") || vmType.startsWith("[")) {
                                            new Plugin.
                                                    TypeDescriptor("L", instruction.owner().name()
                                                    .stringValue()).visit(cb, instruction);
                                        } else {
                                            primitiveTypes.get(vmType).visit(cb, instruction);
                                        }
                                    } else cb.with(element);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            } else cb.with(element);
                        } else cb.with(element);
                    });
                });
            });
        } else {
            clb.accept(mm);
        }
    }

    public static class TypeDescriptor extends openjdk.jcov.data.instrument.TypeDescriptor {

        public TypeDescriptor(String id, String cls) {
            super(id, cls, (a,b) -> {});
        }

        public TypeDescriptor(String id, Class cls, boolean longOrDouble, boolean isPrimitive) {
            super(id, cls, (a,b) -> {}, longOrDouble, isPrimitive);
        }

        public void visit(CodeBuilder codeBuilder, FieldInstruction fi) {
            if(isLongOrDouble()) codeBuilder.dup2();
            else codeBuilder.dup();
            if(isPrimitive())
                codeBuilder.invokestatic(ClassDesc.of(cls()),
                        "valueOf", MethodTypeDesc.ofDescriptor("(" + id() + ")L" + vmCls() + ";"));
            codeBuilder.constantInstruction(fi.owner().name().stringValue());
            codeBuilder.constantInstruction(fi.name().stringValue());
            codeBuilder.invokestatic(ClassDesc.of(Collect.class.getName().replace('/', '.')),
                    "collect",
                    MethodTypeDesc.of(ClassDesc.ofDescriptor("V"),
                            ClassDesc.of(Object.class.getName()),
                            ClassDesc.of(String.class.getName()),
                            ClassDesc.of(String.class.getName())));
            codeBuilder.with(fi);
        }
    }

    final static Map<String, TypeDescriptor> primitiveTypes;

    static {
        primitiveTypes = new HashMap<>();
        primitiveTypes.put("S", new TypeDescriptor("S", Short.class, false, true));
        primitiveTypes.put("I", new TypeDescriptor("I", Integer.class, false, true));
        primitiveTypes.put("J", new TypeDescriptor("J", Long.class, true, true));
        primitiveTypes.put("F", new TypeDescriptor("F", Float.class, false, true));
        primitiveTypes.put("D", new TypeDescriptor("D", Double.class, true, true));
        primitiveTypes.put("Z", new TypeDescriptor("Z", Boolean.class, false, true));
        primitiveTypes.put("B", new TypeDescriptor("B", Byte.class, false, true));
        primitiveTypes.put("C", new TypeDescriptor("C", Character.class, false, true));
    }
}
