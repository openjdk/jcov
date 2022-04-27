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

import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.ALOAD;

/**
 * When used defines which methods to instrument for argument collection.
 */
public interface MethodFilter {
    static List<Plugin.TypeDescriptor> parseDesc(String desc) throws ClassNotFoundException {
        if(!desc.startsWith("(")) throw new IllegalArgumentException("Not a method descriptor: " + desc);
        int pos = 1;
        List<Plugin.TypeDescriptor> res = new ArrayList<>();
        while(desc.charAt(pos) != ')') {
            char next = desc.charAt(pos);
            if(next == 'L') {
                int l = pos;
                pos = desc.indexOf(";", pos) + 1;
                res.add(new Plugin.TypeDescriptor("L", desc.substring(l + 1, pos - 1), ALOAD));
            } else if(next == '[') {
                //TODO can we do better?
                res.add(new Plugin.TypeDescriptor("[", "java/lang/Object", ALOAD));
                if(desc.charAt(pos + 1) == 'L') pos = desc.indexOf(";", pos) + 1;
                else pos = pos + 2;
            } else {
                res.add(Plugin.primitiveTypes.get(new String(new char[] {next})));
                pos++;
            }
        }
        return res;
    }

    boolean accept(int access, String owner, String name, String desc) throws Exception;
}
