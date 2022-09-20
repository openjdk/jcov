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
import com.sun.tdk.jcov.data.ScaleOptions;
import com.sun.tdk.jcov.runtime.Collect;
import com.sun.tdk.jcov.runtime.CollectDetect;
import com.sun.tdk.jcov.tools.OneElemIterator;
import com.sun.tdk.jcov.util.Utils;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.objectweb.asm.Opcodes;

/**
 * Keeps base information about field
 *
 * @author Dmitry Fazunenko
 * @author Alexey Fedorchenko
 */
public class DataField extends DataAnnotated implements Comparable<DataField>,
        Iterable<DataBlock> {

    /**
     * DataClass containing this field
     */
    private final DataClass parent;
    /**
     * Container for field access code
     *
     * @see org.objectweb.asm.Opcodes
     */
    private final DataModifiers access;
    /**
     * Field name
     */
    private final String name;
    /**
     * VM-formed field signature
     */
    private final String vmSig;
    /**
     * Field signature
     */
    private String signature;
    /**
     * Field value
     */
    private final Object value;
    /**
     * Coverage data assigned to this field
     */
    private final DataBlock block;

    /**
     * Creates new DataField instance
     *
     * @param k
     * @param access
     * @param name
     * @param desc
     * @param signature
     * @param value
     */
    public DataField(final DataClass k,
            final int access,
            final String name,
            final String desc,
            final String signature,
            final Object value) {
        this(k, access, name, desc, signature, value, -1);
    }

    /**
     * Creates new DataField instance
     *
     * @param k
     * @param access
     * @param name
     * @param desc
     * @param signature
     * @param value
     * @param id
     */
    public DataField(final DataClass k,
            final int access,
            final String name,
            final String desc,
            final String signature,
            final Object value,
            int id) {
        super(k.rootId);

        this.parent = k;
        this.access = new DataModifiers(access);
        this.name = name;
        this.vmSig = desc;
        this.signature = signature;
        this.value = value;

        boolean newSlot = (id == -1) ? true : false;
        int slot = newSlot ? Collect.newSlot() : id;
        this.block = new DataBlock(rootId, slot, newSlot, 0) {
            public String kind() {
                return XmlNames.FIELD;
            }

            @Override
            protected boolean wasCollectHit() {
                return CollectDetect.wasInvokeHit(DataAbstract.getInvokeID(k.getFullname(), name, desc));
            }

            @Override
            protected long collectCount() {
                return CollectDetect.invokeCountFor(DataAbstract.getInvokeID(k.getFullname(), name, desc));
            }

            @Override
            protected void setCollectCount(long count) {
                CollectDetect.setInvokeCountFor(DataAbstract.getInvokeID(k.getFullname(), name, desc), count);
            }

        };
        k.addField(this);
    }

    public DataField clone(DataClass newClass, int newAccess, String newName) {
        return new DataField(newClass, newAccess, newName, vmSig, this.signature, this.value);
    }

    /**
     * Check whether this field was hit (read or written). When DataField is
     * attached - data is directly gotten from Collect class
     *
     * @return true if this field was hit (read or written)
     */
    boolean wasHit() {
        return block.wasHit();
    }

    /**
     * Get field ID
     *
     * @return field ID
     */
    public int getId() {
        return block.getId();
    }

    /**
     * Check how many times this field was hit (read or written). When DataField
     * is attached - data is directly gotten from Collect class
     *
     * @return times this field was hit
     */
    public long getCount() {
        return block.getCount();
    }

    /**
     * Set count of hits. If DataField is attached - data is directly written to
     * Collect class
     *
     * @param count
     */
    public void setCount(long count) {
        block.setCount(count);
    }

    /**
     * Get scales information of this field. It's a bit mask telling which
     * testrun hit this field
     *
     * @return scales information of this field
     */
    public Scale getScale() {
        return block.scale;
    }

    /**
     * Set scales information of this field. It's a bit mask telling which
     * testrun hit this field
     *
     * @param scale
     */
    public void setScale(Scale scale) {
        block.scale = scale;
    }

    /**
     * @return the parent
     */
    public DataClass getParent() {
        return parent;
    }

    /**
     * @return the access
     */
    public int getAccess() {
        return access.access();
    }

    public Modifiers getModifiers() { return access; }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the vmSig
     */
    public String getVmSig() {
        return vmSig;
    }

    /**
     * @return the value
     */
    public Object getValue() {
        return value;
    }

    /**
     * @return the block
     */
    public DataBlock getBlock() {
        return block;
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
     * Checks whether this field has 'private' modifier
     *
     * @return true if field is private
     */
    @Deprecated
    public boolean hasPrivateModifier() {
        return access.isPrivate();
    }

    /**
     * Checks whether this field has 'public ' modifier
     *
     * @return true if field is public
     */
    @Deprecated
    public boolean hasPublicModifier() {
        return access.isPublic();
    }

    /**
     * Checks whether this field has 'protected' modifier
     *
     * @return true if field is protected
     */
    @Deprecated
    public boolean hasProtectedModifier() {
        return access.isProtected();
    }

    /**
     * Checks whether this field has 'static' modifier
     *
     * @return true if field is static
     */
    @Deprecated
    public boolean hasStaticModifier() {
        return access.isStatic();
    }

    /**
     * Checks whether this field has specified modifier (by Opcodes)
     *
     * @return true if field has specified modifier
     * @see Opcodes
     * @see DataField#getAccess()
     */
    @Deprecated
    public boolean hasModifier(int modifierCode) {
        return access.is(modifierCode);
    }

    /**
     * Get this field`s access flags as String array
     *
     * @return this field`s access flags as String array
     */
    public String[] getAccessFlags() {
        return accessFlags(access.access());
    }

    /**
     * Get field signature
     *
     * @return field signature
     */
    public String getSignature() {
        return signature;
    }

    /**
     * Set field signature
     */
    public void setSignature(String signature) {
        this.signature = signature;
    }

    /**
     * XML Generation. Not supposed to use outside.
     */
    @Override
    public String kind() {
        return block.kind();
    }

    /**
     * XML Generation. Not supposed to use outside.
     */
    @Override
    public void xmlGen(XmlContext ctx) {
        super.xmlGenBodiless(ctx);
    }

//    @Override
//    void xmlBody(XmlContext ctx) {
//        block.xmlBody(ctx);
//    }
    /**
     * XML Generation. Not supposed to use outside.
     */
    @Override
    void xmlAttrs(XmlContext ctx) {
        ctx.attrNormalized(XmlNames.NAME, name);
        ctx.attr(XmlNames.VMSIG, vmSig);
        xmlAccessFlags(ctx, access.access());
        ctx.attr(XmlNames.ACCESS, access.access());
        ctx.attr(XmlNames.ID, block.getId());

        if (value != null) {
            ctx.attr(XmlNames.VALUE, value);
        }
        if (signature != null) {
            ctx.attrNormalized(XmlNames.SIGNATURE, signature);
        }
        if (block.getCount() > 0) {
            ctx.attr(XmlNames.COUNT, block.getCount());
        }

        DataRoot r = DataRoot.getInstance(rootId);
        if (block.scale != null) {
            ScaleOptions opts = r.getScaleOpts();
            StringBuffer sb = new StringBuffer(Utils.halfBytesRequiredFor(block.scale.size()));
            sb.setLength(sb.capacity());
            sb.setLength(block.scale.convertToChars(opts.scalesCompressed(), sb,
                    opts.getScaleCompressor()));
            ctx.attr(XmlNames.SCALE, sb);
        }
    }

    /**
     * XML Generation. Not supposed to use outside.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DataField)) {
            return false;
        }
        DataField fld = (DataField) o;
        return parent.equals(fld.parent) && name.equals(fld.name) && vmSig.equals(fld.vmSig);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + (this.parent != null ? this.parent.hashCode() : 0);
        hash = 17 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 17 * hash + (this.vmSig != null ? this.vmSig.hashCode() : 0);
        return hash;
    }

    public int compareTo(DataField field) {
        return NaturalComparator.INSTANCE.compare(this.name, field.getName());
    }

    /**
     * Checks whether this field is compatible with <b>other</b>
     *
     * @param other
     * @param trace
     * @throws MergeException
     */
    public void checkCompatibility(DataField other, String trace) throws MergeException {
        if (!getDataRoot().getParams().isDynamicCollect() && !other.getDataRoot().getParams().isDynamicCollect()) {
            if (block.getId() != other.block.getId()) {
                throw new MergeException("Field has other id than it's"
                        + " merging copy, expected " + block.getId() + "; found " + other.block.getId(),
                        trace, MergeException.CRITICAL);
            }
        }
    }

    /**
     * Merges information from <b>other</b> to this DataField. <br/><br/> This
     * only sums hit count and scales
     *
     * @param other
     */
    public void merge(DataField other) {
        block.mergeScale(other.block);
        block.setCount(getCount() + other.getCount());
    }

    /**
     * Get DataBlock iterator. DataBlock is the simplest hit and id storage.
     * <br/><br/>
     *
     * Is a "one-element" iterator as field can have only 1 block
     *
     * @return DataBlock iterator
     */
    public Iterator<DataBlock> iterator() {
        return new OneElemIterator(block);
    }

    /**
     * Decode fields`s access flags as String array
     *
     * @param access
     * @return fields`s access flags as String array
     */
    String[] accessFlags(int access) {
        String[] as = super.accessFlags(access);
        List<String> lst = new ArrayList();
        for (String s : as) {
            if (!s.equals(XmlNames.A_BRIDGE) && !s.equals(XmlNames.A_VARARGS)) {
                lst.add(s);
            }
        }

        return lst.toArray(new String[lst.size()]);
    }

    void writeObject(DataOutput out) throws IOException {
        super.writeObject(out);
        out.writeUTF(name);
        writeString(out, signature);
        writeString(out, vmSig);
        out.writeInt(access.access() & ACCESS_MASK); // we don't save ALL the codes in XML, we shouldn't save all codes in net
//        out.write(value); can't - object. Writing only name
        if (value != null) {
            out.writeBoolean(true);
            out.writeUTF(value.getClass().getName());
        } else {
            out.writeBoolean(false);
        }
        block.writeObject(out);
    }

    DataField(final DataClass c, DataInput in) throws IOException {
        super(c.rootId, in);
        parent = c;
        name = in.readUTF();
        signature = readString(in);
        vmSig = readString(in);
        access = new DataModifiers(in.readInt());
        if (in.readBoolean()) {
            value = in.readUTF(); // value
        } else {
            value = null;
        }
        block = new DataBlock(c.rootId) {
            public String kind() {
                return XmlNames.FIELD;
            }

            @Override
            protected boolean wasCollectHit() {
                return CollectDetect.wasInvokeHit(DataAbstract.getInvokeID(c.getFullname(), name, vmSig));
            }

            @Override
            protected long collectCount() {
                return CollectDetect.invokeCountFor(DataAbstract.getInvokeID(c.getFullname(), name, vmSig));
            }

            @Override
            protected void setCollectCount(long count) {
                CollectDetect.setInvokeCountFor(DataAbstract.getInvokeID(c.getFullname(), name, vmSig), count);
            }
        };
    }
}