/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
import openjdk.jcov.data.Instrument;
import openjdk.jcov.data.arguments.runtime.Coverage;
import openjdk.jcov.data.Env;
import org.objectweb.asm.MethodVisitor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static openjdk.jcov.data.Instrument.JCOV_DATA_ENV_PREFIX;
import static org.objectweb.asm.Opcodes.*;

public class Plugin implements InstrumentationPlugin {
    public static final String COLLECTOR_CLASS = "openjdk.jcov.data.arguments.runtime.Collect"
            .replace('.', '/');
    public static final String COLLECTOR_METHOD = "collect";
    public static final String COLLECTOR_DESC =
            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)V";
    public static final String ARGUMENTS_PREFIX = "args.";
    public static final String SERIALIZER =
        Instrument.JCOV_DATA_ENV_PREFIX + ARGUMENTS_PREFIX + ".serializer";
    public static final String TEMPLATE_FILE =
            JCOV_DATA_ENV_PREFIX + "arguments.template";
    public static final String METHOD_FILTER =
        JCOV_DATA_ENV_PREFIX + ARGUMENTS_PREFIX + "method.filter";

    private static class TypeDescriptor extends openjdk.jcov.data.instrument.TypeDescriptor {

        public TypeDescriptor(String id, Class cls, int loadOpcode, boolean longOrDouble) {
            super(id, cls, loadOpcode, longOrDouble);
        }

        public TypeDescriptor(String id, Class cls, int loadOpcode, boolean longOrDouble, boolean isPrimitive) {
            super(id, cls, loadOpcode, longOrDouble, isPrimitive);
        }

        //returns new stack index increased by 1 or 2
        int visit(int paramIndex, int stackIndex, MethodVisitor visitor) {
            visitor.visitInsn(DUP);
            visitor.visitIntInsn(BIPUSH, paramIndex);
            visitor.visitIntInsn(loadOpcode(), stackIndex);
            if(isPrimitive())
                visitor.visitMethodInsn(INVOKESTATIC, cls(), "valueOf",
                        "(" + id() + ")L" + cls() + ";", false);
            visitor.visitInsn(AASTORE);
            return stackIndex + (isLongOrDouble() ? 2 : 1);
        }
    }

    final static Map<String, TypeDescriptor> primitiveTypes;

    static {
        primitiveTypes = new HashMap<>();
        primitiveTypes.put("I", new TypeDescriptor("I", Integer.class, ILOAD, false));
        primitiveTypes.put("J", new TypeDescriptor("J", Long.class, LLOAD, true));
        primitiveTypes.put("F", new TypeDescriptor("F", Float.class, FLOAD, false));
        primitiveTypes.put("D", new TypeDescriptor("D", Double.class, DLOAD, true));
        primitiveTypes.put("Z", new TypeDescriptor("Z", Boolean.class, ILOAD, false));
        primitiveTypes.put("B", new TypeDescriptor("B", Byte.class, ILOAD, false));
    }

    final static TypeDescriptor objectType = new TypeDescriptor("L", Object.class, ALOAD, false, false);

    private final Coverage template;
    private MethodFilter methodFilter;
    private Path templateFile;
    private Function<Object, String> serializer;

    public Plugin() throws
            ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException,
            IllegalAccessException {
        template = new Coverage();
        methodFilter = (a, o, m) -> true;
        templateFile = Env.getPathEnv(TEMPLATE_FILE, Paths.get("template.lst"));
        serializer = Env.getSPIEnv(SERIALIZER, Object::toString);
    }

    @Override
    public MethodVisitor methodVisitor(int access, String owner, String name, String desc, MethodVisitor visitor) {
        String method = name + desc;
        if(methodFilter.accept(access, owner, method)) {
            template.get(owner, method);
            return new MethodVisitor(ASM6, visitor) {
                @Override
                public void visitCode() {
                    try {
                        List<TypeDescriptor> params = parseDesc(desc);
                        if (params.size() > 0) {
                            super.visitLdcInsn(owner);
                            super.visitLdcInsn(name);
                            super.visitLdcInsn(desc);
                            super.visitIntInsn(BIPUSH, params.size());
                            super.visitTypeInsn(ANEWARRAY, "java/lang/Object");
                            int stackIndex = ((access & ACC_STATIC) > 0) ? 0 : 1;
                            for (int i = 0; i < params.size(); i++) {
                                stackIndex = params.get(i).visit(i, stackIndex, this);
                            }
                            visitor.visitMethodInsn(INVOKESTATIC, COLLECTOR_CLASS, COLLECTOR_METHOD,
                                    COLLECTOR_DESC, false);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    super.visitCode();
                }
            };
        } else return visitor;
    }

    private static List<TypeDescriptor> parseDesc(String desc) {
        if(!desc.startsWith("(")) throw new IllegalArgumentException("Not a method descriptor: " + desc);
        int pos = 1;
        List<TypeDescriptor> res = new ArrayList<>();
        while(desc.charAt(pos) != ')') {
            String next = desc.substring(pos, pos + 1);
            if(next.equals("L") || next.equals("[")) {
                res.add(objectType);
                pos = desc.indexOf(";", pos) + 1;
            } else {
                res.add(primitiveTypes.get(next));
                pos++;
            }
        }
        return res;
    }

    @Override
    public void instrumentationComplete() throws IOException {
        Coverage.write(template, templateFile, serializer);
    }
}
