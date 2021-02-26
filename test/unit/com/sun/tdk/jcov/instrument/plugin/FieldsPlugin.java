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
package com.sun.tdk.jcov.instrument.plugin;

import com.sun.tdk.jcov.instrument.InstrumentationPlugin;
import com.sun.tdk.jcov.runtime.JCovSaver;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;
import static org.objectweb.asm.Opcodes.*;

/**
 * An instrumentation plugin which saves some information about fields and methods used during execution.
 * This plugin inserts calls to specific mathod after every PUTFIELD bytecode instruction.
 * This plugin only supports Object and int data types.
 * This class also defines the logic to be used at runtime to save the collected data by printing it into the output.
 */
public class FieldsPlugin implements InstrumentationPlugin, JCovSaver {

    public static final Map<String, Set<Object>> values = new HashMap<>();
    public static final String INSTRUMENTATION_COMPLETE = "Instrumentation complete: ";

    public static void recordFieldValue(Object value, String field) {
        Set<Object> fieldValues = values.getOrDefault(field, new HashSet<>());
        if(values.containsKey(field)) {
            fieldValues = values.get(field);
        } else {
            fieldValues = new HashSet<>();
            values.put(field, fieldValues);
        }
        fieldValues.add(value);
    }

//    @Override
    public MethodVisitor methodVisitor(int access, String owner, String name, String desc, MethodVisitor visitor) {
        return new MethodVisitor(ASM6, visitor) {
            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                if(opcode == PUTFIELD) {
                    super.visitInsn(DUP);
                    if(!descriptor.startsWith("L")) {
                        switch (descriptor) {
                            case "I":
                                super.visitMethodInsn(INVOKESTATIC, Integer.class.getName().replace(".", "/"),
                                        "valueOf", "(I)Ljava/lang/Integer;", false);
                                break;
                        }
                    }
                    super.visitLdcInsn(format("%s.%s", owner, name));
                    super.visitMethodInsn(INVOKESTATIC, FieldsPlugin.class.getName().replace('.', '/'),
                            "recordFieldValue", "(Ljava/lang/Object;Ljava/lang/String;)V", false);
                }
                super.visitFieldInsn(opcode, owner, name, descriptor);
            }
        };
    }

    final static AtomicInteger completeCount = new AtomicInteger(0);

    @Override
    public void instrumentationComplete() throws Exception {
        completeCount.incrementAndGet();
        System.out.println(INSTRUMENTATION_COMPLETE + completeCount);
    }

    @Override
    public void saveResults() {
        values.entrySet().forEach(e ->
                e.getValue().forEach(v -> System.out.println(e.getKey() + "=" + v)));
    }
}
