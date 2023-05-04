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
package openjdk.jcov.data.arguments.instrument;

import com.sun.tdk.jcov.instrument.InstrumentationPlugin;
import jdk.internal.classfile.ClassBuilder;
import jdk.internal.classfile.CodeBuilder;
import jdk.internal.classfile.MethodModel;
import openjdk.jcov.data.arguments.runtime.Collect;
import openjdk.jcov.data.arguments.runtime.Coverage;
import openjdk.jcov.data.arguments.runtime.Runtime;
import openjdk.jcov.data.runtime.CoverageData;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Plugin extends openjdk.jcov.data.instrument.Plugin {
    /**
     * Classname of a collector class which will be called from every instrumented method.
     */
    public static final String COLLECTOR_CLASS = Collect.class.getName()
            .replace('.', '/');
    /**
     * Name of the methods which will be called from every instrumented method.
     */
    public static final String COLLECTOR_METHOD = "collect";
    /**
     * Signature of the method which will be called from every instrumented method.
     */
    public static final String COLLECTOR_DESC =
            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)V";

    final static Map<String, TypeDescriptor> primitiveTypes;

    static {
        primitiveTypes = new HashMap<>();
        primitiveTypes.put("S", new TypeDescriptor("S", Short.class, (cb, i) -> cb.iload(i), false, true));
        primitiveTypes.put("I", new TypeDescriptor("I", Integer.class, (cb, i) -> cb.iload(i), false, true));
        primitiveTypes.put("J", new TypeDescriptor("J", Long.class, (cb, i) -> cb.lload(i), true, true));
        primitiveTypes.put("F", new TypeDescriptor("F", Float.class, (cb, i) -> cb.fload(i), false, true));
        primitiveTypes.put("D", new TypeDescriptor("D", Double.class, (cb, i) -> cb.dload(i), true, true));
        primitiveTypes.put("Z", new TypeDescriptor("Z", Boolean.class, (cb, i) -> cb.iload(i), false, true));
        primitiveTypes.put("B", new TypeDescriptor("B", Byte.class, (cb, i) -> cb.iload(i), false, true));
        primitiveTypes.put("C", new TypeDescriptor("C", Character.class, (cb, i) -> cb.iload(i), false, true));
    }

    public Plugin() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException,
            IllegalAccessException, InstantiationException {
        Runtime.init();
    }

    @Override
    public Map<String, Consumer<OutputStream>> complete() throws Exception {
        Consumer<OutputStream> writer = o -> {
            try {
                Coverage.write(Collect.data(), new BufferedWriter(new OutputStreamWriter(o)));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
        return Map.of(CoverageData.COVERAGE_OUT, writer, InstrumentationPlugin.TEMPLATE_ARTIFACT, writer);
    }

    public static List<TypeDescriptor> parseDesc(String desc) throws ClassNotFoundException {
        if(!desc.startsWith("(")) throw new IllegalArgumentException("Not a method descriptor: " + desc);
        int pos = 1;
        List<TypeDescriptor> res = new ArrayList<>();
        while(desc.charAt(pos) != ')') {
            char next = desc.charAt(pos);
            if(next == 'L') {
                int l = pos;
                pos = desc.indexOf(";", pos) + 1;
                res.add(new TypeDescriptor("L", desc.substring(l + 1, pos - 1), (cb, i) -> cb.aload(i)));
            } else if(next == '[') {
                //TODO can we do better?
                res.add(new TypeDescriptor("[", "java/lang/Object", (cb, i) -> cb.aload(i)));
                if(desc.charAt(pos + 1) == 'L') pos = desc.indexOf(";", pos) + 1;
                else pos = pos + 2;
            } else {
                res.add(primitiveTypes.get(new String(new char[] {next})));
                pos++;
            }
        }
        return res;
    }

    /**
     * Injects necessary instructions to place all the arguments into an array which is then passed tp the collector's
     * method.
     */
    @Override
    public void instrument(ClassBuilder clb, MethodModel mm) throws Exception {
//        if (mm.code().isPresent()) System.out.println(mm.parent().get().thisClass().name().stringValue());
        if (mm.code().isPresent() &&
                methodFilter.accept(mm.flags().flagsMask(), mm.parent().get().thisClass().name().stringValue(),
                    mm.methodName().stringValue(), mm.methodType().stringValue())) {
            Collect.template(clb.original().get().thisClass().name().toString(),
                    mm.methodName().stringValue(), mm.methodTypeSymbol().descriptorString());
            clb.withMethod(mm.methodName().stringValue(), mm.methodTypeSymbol(), mm.flags().flagsMask(), mb -> {
                mb.withCode(cb -> {
                    try {
                        List<TypeDescriptor> params = parseDesc(mm.methodType().stringValue());
                        if (params.size() > 0) {
                            cb.constantInstruction(mm.parent().get().thisClass().name().stringValue());
                            cb.constantInstruction(mm.methodName().stringValue());
                            cb.constantInstruction(mm.methodType().stringValue());
                            cb.bipush(params.size());
                            cb.anewarray(ClassDesc.of(Object.class.getName()));
                            //TODO is this the correct STATIC?
                            int stackIndex = ((mm.flags().flagsMask() & Modifier.STATIC) > 0) ? 0 : 1;
                            for (int i = 0; i < params.size(); i++)
                                stackIndex = params.get(i).visit(i, stackIndex, cb);
                            cb.invokestatic(ClassDesc.of(COLLECTOR_CLASS.replace('/', '.')), COLLECTOR_METHOD,
                                    MethodTypeDesc.of(ClassDesc.ofDescriptor("V"),
                                            ClassDesc.of(String.class.getName()),
                                            ClassDesc.of(String.class.getName()),
                                            ClassDesc.of(String.class.getName()),
                                            ClassDesc.of(Object.class.getName()).arrayType()));
                        }
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    mm.code().get().elementList().forEach(element -> cb.with(element));
                });
            });
        } else {
            clb.accept(mm);
        }
    }

    private static final Set<Class> runtimeClasses = Set.of(
            Collect.class, Coverage.class, Runtime.class,
            Runtime.CompletionTask.class
    );

    @Override
    public Set<Class> runtime() {
        return runtimeClasses;
    }

    /**
     * Aux class responsible for code generation for different types.
     */
    public static class TypeDescriptor extends openjdk.jcov.data.instrument.TypeDescriptor {

        public TypeDescriptor(String id, String cls, BiConsumer<CodeBuilder, Integer> loadOpcode) {
            super(id, cls, loadOpcode);
        }

        public TypeDescriptor(String id, Class cls, BiConsumer<CodeBuilder, Integer> loadOpcode, boolean longOrDouble, boolean isPrimitive) {
            super(id, cls, loadOpcode, longOrDouble, isPrimitive);
        }

        public int visit(int paramIndex, int stackIndex, CodeBuilder codeBuilder) {
            codeBuilder.dup();
            codeBuilder.bipush(paramIndex);
            load(codeBuilder, stackIndex);
            if(isPrimitive())
                codeBuilder.invokestatic(ClassDesc.of(cls()),
                        "valueOf", MethodTypeDesc.ofDescriptor("(" + id() + ")L" + vmCls() + ";"));
            codeBuilder.aastore();
            return stackIndex + (isLongOrDouble() ? 2 : 1);
        }
    }

}
