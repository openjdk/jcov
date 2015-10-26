/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.jcov.instrument;

import java.util.*;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ByteVector;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;

/**
 *
 * @author Alexey Fedorchenko
 */
public class ModuleAttribute extends Attribute{
    private Set<String> serviceDependences;
    private Set<ModuleExport> exports;
    private Map<String, Set<String>> services;

    public ModuleAttribute() {
        super("Module");
    }

    @Override
    protected Attribute read(ClassReader cr,
                             int off,
                             int len,
                             char[] buf,
                             int codeOff,
                             Label[] labels) {
        ModuleAttribute attr = new ModuleAttribute();


        // requires_count and requires[requires_count]
        int requires_count = cr.readUnsignedShort(off);
        off += 2;
        for (int i = 0; i < requires_count; i++) {
            cr.readUTF8(off, buf);
            cr.readUnsignedShort(off + 2);
            off += 4;
        }

        // exports_count and exports[exports_count]
        int exports_count = cr.readUnsignedShort(off);
        off += 2;
        if (exports_count > 0) {
            attr.exports = new HashSet<ModuleExport>();
            for (int i = 0; i < exports_count; i++) {
                String pkg = cr.readUTF8(off, buf).replace('/', '.');
                int exports_to_count = cr.readUnsignedShort(off + 2);
                off += 4;

                if (exports_to_count > 0) {
                    for (int j = 0; j < exports_to_count; j++) {
                        String who = cr.readUTF8(off, buf);
                        off += 2;
                        attr.exports.add(new ModuleExport(pkg, who));
                    }
                } else {
                    attr.exports.add(new ModuleExport(pkg));
                }
            }
        }

        // uses_count and uses_index[uses_count]
        int uses_count = cr.readUnsignedShort(off);
        off += 2;
        if (uses_count > 0) {
            attr.serviceDependences = new HashSet<String>();
            for (int i = 0; i < uses_count; i++) {
                String sn = cr.readClass(off, buf).replace('/', '.');
                attr.serviceDependences.add(sn);
                off += 2;
            }
        }

        // provides_count and provides[provides_count]
        int provides_count = cr.readUnsignedShort(off);
        off += 2;
        if (provides_count > 0) {
            attr.services = new HashMap<String, Set<String>>();
            for (int i = 0; i < provides_count; i++) {
                String sn = cr.readClass(off, buf).replace('/', '.');
                String cn = cr.readClass(off + 2, buf).replace('/', '.');

                if (attr.services.get(sn) == null){
                    attr.services.put(sn, new HashSet<String>());
                }
                attr.services.get(sn).add(cn);

                off += 4;
            }
        }

        return attr;
    }

    @Override
    protected ByteVector write(ClassWriter cw,
                               byte[] code,
                               int len,
                               int maxStack,
                               int maxLocals) {
        ByteVector attr = new ByteVector();

        attr.putShort(0);

        if (exports == null) {
            attr.putShort(0);
        } else {
            exports.add(new ModuleExport("com.sun.tdk.jcov.runtime"));
            // group by exported package
            Map<String, Set<String>> map = new HashMap<String, Set<String>>();
            for (ModuleExport export : exports) {
                String pkg = export.pkg();
                String permit = export.permit();
                if (permit == null) {

                    if (map.get(pkg) == null){
                        map.put(pkg, new HashSet<String>());
                    }

                } else {
                    if (map.get(pkg) == null){
                        map.put(pkg, new HashSet<String>());
                    }
                    map.get(pkg).add(permit);
                }
            }
            attr.putShort(map.size());

            for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
                String pkg = entry.getKey().replace('.', '/');
                int index = cw.newUTF8(pkg);
                attr.putShort(index);

                Set<String> permits = entry.getValue();
                attr.putShort(permits.size());
                for (String permit : permits) {
                    index = cw.newUTF8(permit);
                    attr.putShort(index);
                }
            }
        }

        // uses_count and uses_index[uses_count]
        if (serviceDependences == null) {
            attr.putShort(0);
        } else {
            attr.putShort(serviceDependences.size());
            for (String s : serviceDependences) {
                String service = s.replace('.', '/');
                int index = cw.newClass(service);
                attr.putShort(index);
            }
        }

        // provides_count and provides[provides_count]
        if (services == null) {
            attr.putShort(0);
        } else {
            int count = 0;
            for (Set<String> value : services.values()){
                count += value.size();
            }
            attr.putShort(count);
            for (Map.Entry<String, Set<String>> entry : services.entrySet()) {
                String service = entry.getKey().replace('.', '/');
                int index = cw.newClass(service);
                for (String provider : entry.getValue()) {
                    attr.putShort(index);
                    attr.putShort(cw.newClass(provider.replace('.', '/')));
                }
            }
        }

        return attr;
    }

    class ModuleExport {

        private final String pkg;
        private final String permit;

        public ModuleExport(String pkg, String who) {
            this.pkg = Objects.requireNonNull(pkg);
            this.permit = who;
        }

        public ModuleExport(String pkg) {
            this(pkg, null);
        }

        public String pkg() {
            return pkg;
        }

        public String permit() {
            return permit;
        }

        public int hashCode() {
            return Objects.hash(pkg, permit);
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof ModuleExport))
                return false;
            ModuleExport other = (ModuleExport)obj;
            return Objects.equals(this.pkg, other.pkg) &&
                    Objects.equals(this.permit, other.permit);
        }

    }
}

