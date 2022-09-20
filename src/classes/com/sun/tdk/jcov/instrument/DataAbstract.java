/*
 * Copyright (c) 2014, 2022 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tdk.jcov.data.FileFormatException;
import com.sun.tdk.jcov.instrument.reader.Reader;
import com.sun.tdk.jcov.instrument.reader.ReaderFactory;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

/**
 * DataAbstract
 *
 *
 * @author Robert Field
 */
public abstract class DataAbstract {

    final private static Map<String, Integer> map =
            Collections.synchronizedMap(new HashMap<String, Integer>());
    static volatile int invokeCount = 0;

    public static int getInvokeID(String owner, String name, String descr) {
        String sig = owner + "." + name + descr;
        synchronized (map) {
            Integer id = map.get(sig);
            if (id != null) {
                return id;
            }
            //return 0;
            id = invokeCount++;
            map.put(sig, id);
            return id;
        }
    }

    //never used
    public static void addID(String className, String name, String descr, int id) {
        String sig = className + "." + name + descr;
        map.put(sig, id);
    }

    protected int rootId;

    DataAbstract(int rootId) {
        this.rootId = rootId;
    }

    /**
     * @return DataRoot ID this Data object belongs to
     */
    public int rootId() {
        return rootId;
    }

    /**
     * XML Generation. Not supposed to use outside.
     */
    public abstract String kind();

    void xmlTagOpen(XmlContext ctx, String tag) {
        ctx.indent();
        ctx.print("<");
        ctx.print(tag);
        xmlAttrs(ctx);
        ctx.println(">");
    }

    void xmlTagClose(XmlContext ctx, String tag) {
        ctx.indent();
        ctx.print("</");
        ctx.print(tag);
        ctx.println(">");
    }

    void xmlTagSingle(XmlContext ctx, String tag) {
        ctx.indent();
        ctx.print("<");
        ctx.print(tag);
        xmlAttrs(ctx);
        ctx.println("/>");
    }

    void xmlAttrs(XmlContext ctx) {
    }

    void xmlBody(XmlContext ctx) {
    }

    public static class LocationCoords {

        public int start;
        public int end;
    }

    public void xmlGen(XmlContext ctx) {
        xmlTagOpen(ctx, kind());
        ctx.incIndent();
        xmlBody(ctx);
        ctx.decIndent();
        xmlTagClose(ctx, kind());
    }

    void xmlGenBodiless(XmlContext ctx) {
        xmlTagSingle(ctx, kind());
    }

    void xmlAccessFlags(XmlContext ctx, int access) {
        ctx.print(" " + XmlNames.FLAGS + "='");
        String[] flags = accessFlags(access);
        for (String fl : flags) {
            ctx.print(" " + fl);
        }
        ctx.print("'");
    }

    /**
     * XML reading. Not supposed to use outside.
     */
    public void readDataFrom() throws FileFormatException {
        ReaderFactory rf = DataRoot.getInstance(rootId).getReaderFactory();
        Reader r = rf.getReaderFor(this);
        r.readData(this);
    }

    String[] accessFlags(int access) {
        List<String> flags = new ArrayList<String>();
        if ((access & ACC_PUBLIC) != 0) {
            flags.add(XmlNames.A_PUBLIC);
        }
        if ((access & ACC_PRIVATE) != 0) {
            flags.add(XmlNames.A_PRIVATE);
        }
        if ((access & ACC_PROTECTED) != 0) {
            flags.add(XmlNames.A_PROTECTED);
        }
        if ((access & ACC_STATIC) != 0) {
            flags.add(XmlNames.A_STATIC);
        }
        if ((access & ACC_FINAL) != 0) {
            flags.add(XmlNames.A_FINAL);
        }
        if ((access & ACC_SYNCHRONIZED) != 0) {
            flags.add(XmlNames.A_SYNCHRONIZED);
        }
        if ((access & ACC_VOLATILE) != 0) {
            flags.add(XmlNames.A_VOLATILE);
        }
        if ((access & ACC_BRIDGE) != 0) {
            flags.add(XmlNames.A_BRIDGE);
        }
        if ((access & ACC_VARARGS) != 0) {
            flags.add(XmlNames.A_VARARGS);
        }
        if ((access & ACC_TRANSIENT) != 0) {
            flags.add(XmlNames.A_TRANSIENT);
        }
        if ((access & ACC_NATIVE) != 0) {
            flags.add(XmlNames.A_NATIVE);
        }
        if ((access & ACC_INTERFACE) != 0) {
            flags.add(XmlNames.A_INTERFACE);
        }
        if ((access & ACC_ABSTRACT) != 0) {
            flags.add(XmlNames.A_ABSTRACT);
        }
        if ((access & ACC_STRICT) != 0) {
            flags.add(XmlNames.A_STRICT);
        }
        if ((access & ACC_ANNOTATION) != 0) {
            flags.add(XmlNames.A_ANNOTATION);
        }
        if ((access & ACC_ENUM) != 0) {
            flags.add(XmlNames.A_ENUM);
        }
        if ((access & ACC_SYNTHETIC) != 0) {
            flags.add(XmlNames.A_SYNTHETIC);
        }
        return flags.isEmpty() ? new String[0] : flags.toArray(new String[flags.size()]);
    }

    public int access(String[] accessFlags) {
        int access = 0;
        for (String flag : accessFlags) {
            if (flag.contains(XmlNames.A_PUBLIC)) {
                access |= ACC_PUBLIC;
            }
            if (flag.contains(XmlNames.A_PRIVATE)) {
                access |= ACC_PRIVATE;
            }
            if (flag.contains(XmlNames.A_PROTECTED)) {
                access |= ACC_PROTECTED;
            }
            if (flag.contains(XmlNames.A_STATIC)) {
                access |= ACC_STATIC;
            }
            if (flag.contains(XmlNames.A_FINAL)) {
                access |= ACC_FINAL;
            }
            if (flag.contains(XmlNames.A_VOLATILE)) {
                access |= ACC_VOLATILE;
            }
            if (flag.contains(XmlNames.A_BRIDGE)) {
                access |= ACC_BRIDGE;
            }
            if (flag.contains(XmlNames.A_VARARGS)) {
                access |= ACC_VARARGS;
            }
            if (flag.contains(XmlNames.A_TRANSIENT)) {
                access |= ACC_TRANSIENT;
            }
            if (flag.contains(XmlNames.A_NATIVE)) {
                access |= ACC_NATIVE;
            }
            if (flag.contains(XmlNames.A_INTERFACE) || flag.contains(XmlNames.A_DEFENDER_METH)) {
                access |= ACC_INTERFACE;
            }
            if (flag.contains(XmlNames.A_ABSTRACT)) {
                access |= ACC_ABSTRACT;
            }
            if (flag.contains(XmlNames.A_STRICT)) {
                access |= ACC_STRICT;
            }
            if (flag.contains(XmlNames.A_ANNOTATION)) {
                access |= ACC_ANNOTATION;
            }
            if (flag.contains(XmlNames.A_ENUM)) {
                access |= ACC_ENUM;
            }
            if (flag.contains(XmlNames.A_SYNTHETIC)) {
                access |= ACC_SYNTHETIC;
            }
            if (flag.contains(XmlNames.A_SYNCHRONIZED)) {
                access |= ACC_SYNCHRONIZED;
            }

        }
        return access;
    }

    public String access(int access) {
        String res = "";
        for (String s : accessFlags(access)) {
            res += " " + s;
        }
        res = res.length() > 0 ? res.substring(1) : res;

        return res;
    }

    protected DataRoot getDataRoot() {
        return DataRoot.getInstance(rootId);
    }

    static void writeString(DataOutput out, String s) throws IOException {
        if (s != null) {
            out.writeBoolean(true);
            out.writeUTF(s);
        } else {
            out.writeBoolean(false);
        }
    }

    static String readString(DataInput in) throws IOException {
        if (in.readBoolean()) {
            return in.readUTF();
        } else {
            return null;
        }
    }

    static void writeStrings(DataOutput out, String[] strs) throws IOException {
        if (strs != null) {
            out.writeShort(strs.length);
            for (int i = 0; i < strs.length; ++i) {
                out.writeUTF(strs[i]);
            }
        } else {
            out.writeShort(Short.MAX_VALUE);
        }
    }

    static String[] readStrings(DataInput in) throws IOException {
        int len = in.readShort();
        if (len != Short.MAX_VALUE) {
            String res[] = new String[len];
            for (int i = 0; i < len; ++i) {
                res[i] = in.readUTF();
            }
            return res;
        } else {
            return null;
        }
    }
    public static final int ACCESS_MASK = ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED | ACC_FINAL | ACC_STATIC | ACC_SYNCHRONIZED
            | ACC_VOLATILE | ACC_BRIDGE | ACC_VARARGS | ACC_TRANSIENT | ACC_NATIVE | ACC_ABSTRACT | ACC_INTERFACE
            | ACC_STRICT | ACC_ANNOTATION | ACC_ENUM | ACC_SYNTHETIC;
}