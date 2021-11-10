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
import openjdk.jcov.data.arguments.runtime.Collect;
import openjdk.jcov.data.arguments.runtime.Coverage;
import openjdk.jcov.data.Env;
import openjdk.jcov.data.arguments.runtime.Implantable;
import openjdk.jcov.data.arguments.runtime.Saver;
import openjdk.jcov.data.arguments.runtime.Serializer;
import org.objectweb.asm.MethodVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static openjdk.jcov.data.Env.JCOV_DATA_ENV_PREFIX;
import static openjdk.jcov.data.arguments.runtime.Collect.SERIALIZER;
import static org.objectweb.asm.Opcodes.*;

/**
 * An instrumention plugin responsible for adding necessary bytecode instructions to collect and pass argument values to
 * a specified collector.
 */
public class Plugin implements InstrumentationPlugin {
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
    /**
     * Name of a property which contains class name for the method filter.
     */
    public static final String METHOD_FILTER =
        JCOV_DATA_ENV_PREFIX + Collect.ARGUMENTS_PREFIX + "method.filter";

    /**
     * Aux class responsible for code generation for different types.
     */
    public static class TypeDescriptor extends openjdk.jcov.data.instrument.TypeDescriptor {

        public TypeDescriptor(String id, String cls, int loadOpcode) {
            super(id, cls, loadOpcode);
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
        primitiveTypes.put("S", new TypeDescriptor("S", Short.class, ILOAD, false, true));
        primitiveTypes.put("I", new TypeDescriptor("I", Integer.class, ILOAD, false, true));
        primitiveTypes.put("J", new TypeDescriptor("J", Long.class, LLOAD, true, true));
        primitiveTypes.put("F", new TypeDescriptor("F", Float.class, FLOAD, false, true));
        primitiveTypes.put("D", new TypeDescriptor("D", Double.class, DLOAD, true, true));
        primitiveTypes.put("Z", new TypeDescriptor("Z", Boolean.class, ILOAD, false, true));
        primitiveTypes.put("B", new TypeDescriptor("B", Byte.class, ILOAD, false, true));
        primitiveTypes.put("C", new TypeDescriptor("C", Character.class, ILOAD, false, true));
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
        methodFilter = Env.getSPIEnv(METHOD_FILTER, (a, o, m, d) -> true);
        templateFile = Env.getPathEnv(Collect.COVERAGE_OUT, Paths.get("template.lst"));
        serializer = Env.getSPIEnv(SERIALIZER, Object::toString);
    }

    /**
     * Injects necessary instructions to place all the arguments into an array which is then passed tp the collector's
     * method.
     */
    @Override
    public MethodVisitor methodVisitor(int access, String owner, String name, String desc, MethodVisitor visitor) {
        try {
            String method = name + desc;
            try {
                if (methodFilter.accept(access, owner, name, desc)) {
                    template.get(owner, method);
                    return new MethodVisitor(ASM6, visitor) {
                        @Override
                        public void visitCode() {
                            try {
                                List<TypeDescriptor> params = MethodFilter.parseDesc(desc);
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
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch(Throwable e) {
            //JCov is known for swallowing exceptions
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void instrumentationComplete() throws IOException {
        try {
            Coverage.write(template, templateFile);
        } catch(Throwable e) {
            //JCov is known for swallowing exceptions
            e.printStackTrace();
            throw e;
        }
    }

    private static final Set<Class> runtimeClasses = Set.of(
            Collect.class, Coverage.class, Saver.class, Saver.NoRuntimeSerializer.class, Env.class,
            Implantable.class, Serializer.class
    );

    protected List<Class> runtimeClasses() {
        return runtimeClasses();
    }

    @Override
    public Path runtime() throws Exception {
        try {
            Path dest = Files.createTempFile("jcov-data", ".jar");
            Properties toSave = new Properties();
            System.getProperties().forEach((k, v) -> {
                if(k.toString().startsWith(JCOV_DATA_ENV_PREFIX))
                    toSave.setProperty(k.toString(), v.toString());
            });
            Set<Class> allRuntime = runtimeClasses;
            Function<Object, String> serializer = Env.getSPIEnv(SERIALIZER, null);
            if(serializer != null && serializer instanceof Implantable) {
                allRuntime = new HashSet<>(runtimeClasses);
                allRuntime.addAll(((Implantable)serializer).runtime());
            }
            try(JarOutputStream jar = new JarOutputStream(Files.newOutputStream(dest))) {
                jar.putNextEntry(new JarEntry(Env.PROP_FILE));
                toSave.store(jar, "");
                jar.closeEntry();
                for(Class rc : allRuntime) {
                    String fileName = rc.getName().replace(".", "/") + ".class";
                    jar.putNextEntry(new JarEntry(fileName));
                    try (InputStream ci = rc.getClassLoader().getResourceAsStream(fileName)) {
                        byte[] buffer = new byte[1024];
                        int read;
                        while ((read = ci.read(buffer)) > 0) {
                            jar.write(buffer, 0, read);
                        }
                    }
                    jar.closeEntry();
                }
            }
            return dest;
        } catch(Throwable e) {
            //JCov is known for swallowing exceptions
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public String collectorPackage() {
        try {
            return Collect.class.getPackage().getName();
        } catch(Throwable e) {
            //JCov is known for swallowing exceptions
            e.printStackTrace();
            throw e;
        }
    }
}
