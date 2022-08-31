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

import com.sun.tdk.jcov.util.NaturalComparator;
import com.sun.tdk.jcov.data.Scale;
import com.sun.tdk.jcov.util.Utils;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.Opcodes;

/**
 * Parent for all method data classes. Keeps base information about method
 * itself.
 *
 * @author Dmitry Fazunenko
 * @author Alexey Fedorchenko
 */
public abstract class DataMethod extends DataAnnotated implements Comparable<DataMethod>,
        Iterable<DataBlock> {

    /**
     * Parent DataClass which is containing this object
     */
    protected final DataClass parent;
    /**
     * Container for method access code
     *
     * @see org.objectweb.asm.Opcodes
     */
    protected final DataModifiers access;
    /**
     * Method name
     */
    protected final String name;
    /**
     * VM-formed signature
     */
    protected final String vmSig;
    /**
     * Method signature
     */
    protected final String signature;
    /**
     * Exceptions which are thrown from this method
     */
    protected final String[] exceptions;
    /**
     * Information about position in source file
     */
    protected List<LineEntry> lineTable;
    /**
     * Check whether this DataMethod is configured to differ elements - classes
     * vs interfaces<br/> False always
     */
    private final boolean differentiateMethods;

    /**
     * Creates new DataMethod instance<br> Warning, this constructor adds
     * created object to <b>k</b> DataClass. Do not use this constructor in
     * iterators.
     *
     * @param k
     * @param access
     * @param name
     * @param desc
     * @param signature
     * @param exceptions
     * @param differentiateMethods
     */
    DataMethod(final DataClass k,
            final int access,
            final String name,
            final String desc,
            final String signature,
            final String[] exceptions,
            final boolean differentiateMethods) {
        super(k.rootId);

        this.parent = k;
        this.access = new DataModifiers(access);
        this.name = name;
        this.vmSig = desc;
        this.signature = signature;
        this.exceptions = exceptions;
        this.differentiateMethods = differentiateMethods; // always false at the moment
        k.addMethod(this);
    }

    /**
     * Creates new DataMethod instance<br> Warning, this constructor adds
     * created object to <b>k</b> DataClass. Do not use this constructor in
     * iterators.
     *
     * @param k
     * @param access
     * @param name
     * @param desc
     * @param signature
     * @param exceptions
     * @param differentiateMethods
     */
    protected DataMethod(DataMethod other) {
        super(other.rootId);

        this.parent = other.parent;
        this.access = other.access;
        this.name = other.name;
        this.vmSig = other.vmSig;
        this.signature = other.signature;
        this.exceptions = other.exceptions;
        this.differentiateMethods = other.differentiateMethods; // always false at the moment
    }

    /**
     * @return exceptions thrown by this method
     */
    public String[] getExceptions() {
        return exceptions;
    }

    /**
     * @return method name
     */
    public String getName() {
        return name;
    }

    /**
     * @return full method name in format /package1/package2/Class.method +
     * vmSig
     */
    public String getFullName() {
        return parent.getFullname() + "." + name + vmSig;
    }

    /**
     * @return class containing this method
     */
    public DataClass getParent() {
        return parent;
    }

    /**
     * @return method signature
     */
    public String getSignature() {
        return signature;
    }

    /**
     * @return method VM signature
     */
    public String getVmSignature() {
        return vmSig;
    }

    /**
     * @return human readable method signature with arguments and return type
     * (ret name(arg1,arg2,arg3))
     */
    public String getFormattedSignature() {
        return Utils.convertVMtoJLS(name, vmSig);
    }

    /**
     * Get methods access code
     *
     * @see org.objectweb.asm.Opcodes
     * @return methods access code
     */
    public int getAccess() {
        return access.access();
    }

    public Modifiers getModifiers() { return access; }

    /**
     * Get this method`s access flags as String array
     *
     * @return this method`s access flags as String array
     */
    public String[] getAccessFlags() {
        return accessFlags(access.access());
    }

    /**
     * Check whether <b>access</b> field has ACC_PUBLIC or ACC_PROTECTED flag
     *
     * @see #getAccess()
     * @see org.objectweb.asm.Opcodes
     * @return true if <b>access</b> field has ACC_PUBLIC or ACC_PROTECTED flag
     */
    @Deprecated
    public boolean isPublic() {
        return isPublicAPI();
    }

    /**
     * Check whether <b>access</b> field has ACC_PUBLIC or ACC_PROTECTED flag
     *
     * @see #getAccess()
     * @see org.objectweb.asm.Opcodes
     * @return true if <b>access</b> field has ACC_PUBLIC or ACC_PROTECTED flag
     */
    public boolean isPublicAPI() {
        return access.isPublic() || access.isProtected();
    }

    /**
     * Check whether <b>access</b> field has ACC_ABSTRACT flag
     *
     * @see #getAccess()
     * @see org.objectweb.asm.Opcodes
     * @return true if <b>access</b> field has ACC_ABSTRACT flag
     */
    @Deprecated
    public boolean isAbstract() {
        return access.isAbstract();
    }

    /**
     * Checks whether this method has 'private' modifier
     *
     * @return true if method is private
     */
    @Deprecated
    public boolean hasPrivateModifier() {
        return access.isPrivate();
    }

    /**
     * Checks whether this method has 'public ' modifier
     *
     * @return true if method is public
     */
    @Deprecated
    public boolean hasPublicModifier() {
        return access.isPublic();
    }

    /**
     * Checks whether this method has 'protected' modifier
     *
     * @return true if method is protected
     */
    @Deprecated
    public boolean hasProtectedModifier() {
        return access.isProtected();
    }

    /**
     * Checks whether this method has 'abstract' modifier
     *
     * @return true if method is abstract
     */
    @Deprecated
    public boolean hasAbstractModifier() {
        return access.isAbstract();
    }

    /**
     * Checks whether this method has 'static' modifier
     *
     * @return true if method is static
     */
    @Deprecated
    public boolean hasStaticModifier() {
        return access.isStatic();
    }

    /**
     * Checks whether this method has 'native' modifier
     *
     * @return true if method is native
     */
    @Deprecated
    public boolean hasNativeModifier() {
        return access.isNative();
    }

    /**
     * Checks whether this method has specified modifier (by Opcodes)
     *
     * @return true if method has specified modifier
     * @see Opcodes
     * @see DataMethod#getAccess()
     */
    @Deprecated
    public boolean hasModifier(int modifierCode) {
        return access.is(modifierCode);
    }

    /**
     * Check whether this method was hit. When DataMethod is attached - data is
     * directly gotten from Collect class<br/><br/>
     *
     * Don't use it directly with DataMethodWithBlocks - it contains several
     * block. Loop through it's blocks instead.
     *
     * @return true if this method was hit
     */
    public abstract boolean wasHit();

    /**
     * Check how many times this method was hit. When DataMethod is attached -
     * data is directly gotten from Collect class<br/><br/>
     *
     * Don't use it directly with DataMethodWithBlocks - it contains several
     * block. Loop through it's blocks instead. The first - MethEnter
     *
     * @return times this method was hit
     */
    public abstract long getCount();

    /**
     * Set count of hits. If DataMethod is attached - data is directly written
     * to Collect class<br/><br/>
     *
     * Don't use it directly with DataMethodWithBlocks - it contains several
     * block. Loop through it's blocks instead.
     *
     * @param count
     */
    public abstract void setCount(long count);

    /**
     * Get scales information of this method. It's a bit mask telling which
     * testrun hit this method<br/><br/>
     *
     * Don't use it directly with DataMethodWithBlocks - it contains several
     * block. Loop through it's blocks instead.
     *
     * @return scales information of this method
     */
    public abstract Scale getScale();

    /**
     * -1 means that there is no id in this method
     *
     * @return id assigned to this method (or to the first block of this method
     * - methenter)
     */
    public abstract int getSlot();

    public abstract DataMethod clone(DataClass newClass, int newAccess, String newName);

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DataMethod)) {
            return false;
        }
        DataMethod meth = (DataMethod) o;
        return parent.equals(meth.parent) && name.equals(meth.name) && vmSig.equals(meth.vmSig);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + (this.parent != null ? this.parent.hashCode() : 0);
        hash = 17 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 17 * hash + (this.vmSig != null ? this.vmSig.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return super.toString() + "-" + name;
    }

    /**
     * @return false
     */
    public boolean hasCRT() {
        return false;
    }

    /**
     * XML Generation. Not supposed to use outside.
     */
    @Override
    public String kind() {
        if (differentiateMethods) {
            if (name.equals("<init>")) {
                return XmlNames.CONSTRUCTOR;
            } else if (name.equals("<clinit>")) {
                return XmlNames.CLASS_INIT;
            }
        }
        return XmlNames.METHOD;
    }

    /**
     * XML Generation. Not supposed to use outside.
     */
    @Override
    void xmlGen(XmlContext ctx) {
        if (ctx.showAbstract || !access.isAbstract()) {
            super.xmlGen(ctx);
        }
    }

    /**
     * XML Generation. Not supposed to use outside.
     */
    @Override
    void xmlAttrs(XmlContext ctx) {
        ctx.attrNormalized(XmlNames.NAME, name);
        ctx.attr(XmlNames.VMSIG, vmSig);

        xmlAccessFlags(ctx, access.access());
        ctx.attr(XmlNames.ACCESS, access);

        if (!differentiateMethods) {
            if (name.equals("<init>")) {
                ctx.attr(XmlNames.CONSTRUCTOR, true);
            } else if (name.equals("<clinit>")) {
                ctx.attr(XmlNames.CLASS_INIT, true);
            }
        }

        if (signature != null) {
            ctx.attrNormalized(XmlNames.SIGNATURE, signature);
        }

    }

    /**
     * XML Generation. Not supposed to use outside.
     */
    @Override
    void xmlBody(XmlContext ctx) {
        if (ctx.showLineTable && lineTable != null) {
            xmlLineTable(ctx);
        }
    }

    /**
     * XML Generation. Not supposed to use outside.
     */
    void xmlLineTable(XmlContext ctx) {
        ctx.indent();
        ctx.print("<lt>");
        for (LineEntry pair : lineTable) {
            ctx.print(pair.bci + "=" + pair.line + ";");
        }
        ctx.println("</lt>");
    }

    /**
     * Add information about position in source file
     *
     * @param bci
     * @param line
     */
    public void addLineEntry(int bci, int line) {
        if (lineTable == null) {
            lineTable = new ArrayList<LineEntry>();
        }
        lineTable.add(new LineEntry(bci, line));
    }

    /**
     * Keeps information about ranges in source file
     */
    public static class LineEntry {

        /**
         * Instruction number (position) in bytecode
         */
        public int bci;
        /**
         * Line number in source code
         */
        public int line;

        LineEntry(int bci, int line) {
            this.bci = bci;
            this.line = line;
        }
    }

    /**
     * @return information about where this method is written in sources
     */
    public List<LineEntry> getLineTable() {
        return lineTable;
    }

    public int compareTo(DataMethod method) {
        return NaturalComparator.INSTANCE.compare(this.name, method.getName());
    }

    /**
     * Checks whether this method is compatible with <b>other</b>
     *
     * @param other
     * @param trace
     * @throws MergeException
     */
    public abstract void checkCompatibility(DataMethod other, String trace) throws MergeException;

    /**
     * Merges information from <b>other</b> to this DataMethod. <br/> This only
     * sums hit count and scales - any difference in method structure is an
     * error
     *
     * @param other
     */
    public abstract void merge(DataMethod other);

    /**
     * <p> Decode method`s access flags as String array </p> <p> Defender
     * methods (interface-default - Lambda project) have the same OpCode as
     * 'interface' so they should be marked as 'defender'. </p> <p> 'volatile'
     * and 'transient' flags are ignored </p> <p> Note that not all flags are
     * <i>written</i> or <i>read</i> into XML file </p>
     *
     * @param access
     * @return method`s access flags as String array
     */
    @Override
    String[] accessFlags(int access) {
        String[] as = super.accessFlags(access);
        List<String> lst = new ArrayList();
        for (String s : as) {
            if (s.equals(XmlNames.A_INTERFACE)) {
                lst.add(XmlNames.A_DEFENDER_METH);
            } else {
                if (!XmlNames.A_VOLATILE.equals(s) && !XmlNames.A_TRANSIENT.equals(s)) {
                    lst.add(s);
                }
            }
        }

        return lst.toArray(new String[lst.size()]);
    }

    /**
     * @return list of <b>all</b> blocks included into this method. It can
     * contain any block type.
     */
    public abstract List<DataBlock> getBlocks();

    /**
     * Returns a list of <b>all</b> branches included into this method
     * (DataExitSimple that represents just exit of the method is not included).
     * DataMethodsEntryOnly (as method coverage) and DataMethodInvoked (abstract
     * and native) will return Collections.EMPTY_LIST. <br/> <br/> Branch
     * doesn't contain counts itself. But branch contains a set of
     * DataBlockTargets which can contain counts. <br/>
     *
     * @return list of <b>all</b> branches included into this method.
     * @see #getBranchTargets()
     */
    public abstract List<DataBranch> getBranches();

    /**
     * Method can contain a number of branches. Each branch can contain any
     * number of DataBlockTargets that can be counted. <br/>
     *
     * @return all DataBlocks included in all branches of this method
     */
    public abstract List<DataBlockTarget> getBranchTargets();

    void writeObject(DataOutput out) throws IOException {
        super.writeObject(out);
        out.writeUTF(name);
        writeString(out, vmSig);
        writeString(out, signature);
        out.writeInt(access.access() & ACCESS_MASK); // we don't save ALL the codes in XML, we shouldn't save all codes in net
        out.writeBoolean(differentiateMethods);
        writeStrings(out, exceptions);
        if (lineTable != null) {
            out.writeShort(lineTable.size());
            for (LineEntry line : lineTable) {
                out.writeShort(line.bci);
                out.writeShort(line.line);
            }
        } else {
            out.writeShort(Short.MAX_VALUE);
        }
    }

    DataMethod(DataClass parent, DataInput in) throws IOException {
        super(parent.rootId, in);
        this.parent = parent;
        name = in.readUTF();
        vmSig = readString(in);
        signature = readString(in);
        access = new DataModifiers(in.readInt());
        differentiateMethods = in.readBoolean();
        exceptions = readStrings(in);
        int len = in.readShort();
        if (len != Short.MAX_VALUE) {
            lineTable = new ArrayList<LineEntry>(len);
            int bci, line;
            for (int i = 0; i < len; ++i) {
                bci = in.readShort();
                line = in.readShort();
                lineTable.add(new LineEntry(bci, line));
            }
        } else {
            lineTable = null;
        }
    }
}