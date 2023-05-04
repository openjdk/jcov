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
package openjdk.jcov.data.instrument;

import com.sun.tdk.jcov.instrument.InstrumentationParams;
import com.sun.tdk.jcov.instrument.InstrumentationPlugin;
import com.sun.tdk.jcov.instrument.ModuleInstrumentationPlugin;
import jdk.internal.classfile.Attributes;
import jdk.internal.classfile.ClassBuilder;
import jdk.internal.classfile.ClassModel;
import jdk.internal.classfile.Classfile;
import jdk.internal.classfile.MethodModel;
import jdk.internal.classfile.attribute.ModuleAttribute;
import jdk.internal.classfile.attribute.ModuleExportInfo;
import jdk.internal.classfile.attribute.ModuleHashesAttribute;
import jdk.internal.classfile.java.lang.constant.PackageDesc;
import openjdk.jcov.data.Env;
import openjdk.jcov.data.arguments.runtime.Collect;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static openjdk.jcov.data.Env.JCOV_DATA_ENV_PREFIX;


public abstract class Plugin extends ModulePlugin implements InstrumentationPlugin {
    /**
     * Name of a property which contains class name for the method filter.
     */
    public static final String METHOD_FILTER =
            JCOV_DATA_ENV_PREFIX + Collect.ARGUMENTS_PREFIX + "method.filter";
    protected MethodFilter methodFilter;

    public Plugin() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException,
            IllegalAccessException, InstantiationException {
        methodFilter = Env.getSPIEnv(METHOD_FILTER, (a, o, m, d) -> true);
    }

    private final Set<String> JCOV_FORCED_CLASSES = Set.of("java/lang/Shutdown", "java/lang/invoke/LambdaForm");

    @Override
    public void instrument(Collection<String> collection, ClassLoader loader, BiConsumer<String, byte[]> consumer,
                           InstrumentationParams params) throws Exception {
        for (String r: collection) {
            if (isClass(r)) {
                ClassModel model = Classfile.parse(loader.getResourceAsStream(r).readAllBytes());
                if (!JCOV_FORCED_CLASSES.stream().anyMatch(c -> model.thisClass().name().stringValue().startsWith(c))) {
//                byte[] oldContent =  model.transform((classBuilder, ce) -> {
//                    classBuilder.with(ce);
//                });
                    byte[] newContent = model.transform((classBuilder, ce) -> {
                        if (ce instanceof MethodModel) {
                            MethodModel mm = (MethodModel) ce;
                            try {
                                instrument(classBuilder, mm);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        } else classBuilder.with(ce);
                    });
                    consumer.accept(r, newContent);
                }
            }
        }
    }

    public void methodFilter(MethodFilter methodFilter) {
        this.methodFilter = methodFilter;
    }

    abstract public void instrument(ClassBuilder clb, MethodModel mm) throws Exception;
    abstract public Set<Class> runtime();

    @Override
    public String getModuleName(byte[] bytes) {
        ClassModel cls = Classfile.parse(bytes);
        if (cls.isModuleInfo()) {
            ModuleAttribute attr = ((ModuleAttribute) cls.attributes().stream()
                    .filter(a -> a.attributeMapper() == Attributes.MODULE)
                    .findFirst()
                    .orElseThrow());
            return attr.moduleName().name().stringValue();
        } else throw new IllegalStateException("Not a module!");
    }
}
