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

import com.sun.tdk.jcov.data.Scale;
import com.sun.tdk.jcov.runtime.Collect;
import com.sun.tdk.jcov.tools.OneElemIterator;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * DataMethodEntryOnly serves for storing information about only entering into
 * methods - eg Method coverage mode
 *
 * @author Robert Field
 */
public class DataMethodEntryOnly extends DataMethod implements Iterable<DataBlock> {

    /**
     * Method entry information
     */
    private final DataBlockMethEnter entryBlock;

    /**
     * Creates a new instance of DataMethodEntryOnly<br> ID is generated<br>
     * Warning, this constructor adds created object to <b>k</b> DataClass. Do
     * not use this constructor in iterators.
     *
     * @param k
     * @param access
     * @param name
     * @param desc
     * @param signature
     * @param exceptions
     */
    public DataMethodEntryOnly(final DataClass k,
            final int access,
            final String name,
            final String desc,
            final String signature,
            final String[] exceptions) {
        this(k, access, name, desc, signature, exceptions, -1);
    }

    /**
     * Creates a new instance of DataMethodEntryOnly<br> Warning, this
     * constructor adds created object to <b>k</b> DataClass. Do not use this
     * constructor in iterators.
     *
     * @param k
     * @param access
     * @param name
     * @param desc
     * @param signature
     * @param exceptions
     * @param id
     */
    public DataMethodEntryOnly(final DataClass k,
            final int access,
            final String name,
            final String desc,
            final String signature,
            final String[] exceptions,
            int id) {
        super(k, access, name, desc, signature, exceptions, false);

        boolean newData = (id == -1) ? true : false;
        int slot = (newData) ? Collect.newSlot() : id;
        entryBlock = new DataBlockMethEnter(rootId, slot, newData, 0) {
            @Override
            void xmlAttrs(XmlContext ctx) {
                ctx.attr(XmlNames.ID, getId());
                ctx.attr(XmlNames.COUNT, getCount());
                printScale(ctx);
            }
        };
        LocationConcrete loc = new LocationConcrete(0) {
            public String kind() {
                return "trash";
            }
        };
        entryBlock.setConcreteLocation(loc);
    }

    /**
     * Creates a new instance of DataMethodEntryOnly<br> Warning, this
     * constructor adds created object to <b>k</b> DataClass. Do not use this
     * constructor in iterators.
     *
     * @param k
     * @param access
     * @param name
     * @param desc
     * @param signature
     * @param exceptions
     * @param id
     * @param count
     */
    public DataMethodEntryOnly(final DataClass k,
            final int access,
            final String name,
            final String desc,
            final String signature,
            final String[] exceptions,
            int id, long count) {
        super(k, access, name, desc, signature, exceptions, false);

        boolean newData = (id == -1) ? true : false;
        int slot = (newData) ? Collect.newSlot() : id;
        entryBlock = new DataBlockMethEnter(rootId, slot, newData, count) {
            @Override
            void xmlAttrs(XmlContext ctx) {
                ctx.attr(XmlNames.ID, getId());
                ctx.attr(XmlNames.COUNT, getCount());
                printScale(ctx);
            }
        };
        LocationConcrete loc = new LocationConcrete(0) {
            public String kind() {
                return "trash";
            }
        };
        entryBlock.setConcreteLocation(loc);
    }

    /**
     * Creates a new instance of DataMethodEntryOnly<br> Warning, this
     * constructor adds created object to <b>k</b> DataClass. Do not use this
     * constructor in iterators.
     *
     * @param k
     * @param access
     * @param name
     * @param desc
     * @param signature
     * @param exceptions
     * @param id
     * @param count
     */
    public DataMethodEntryOnly(DataMethod other) {
        super(other);

        boolean newData = (other.getSlot() == -1) ? true : false;
        int slot = (newData) ? Collect.newSlot() : other.getSlot();
        entryBlock = new DataBlockMethEnter(rootId, slot, newData, other.getCount()) {
            @Override
            void xmlAttrs(XmlContext ctx) {
                ctx.attr(XmlNames.ID, getId());
                ctx.attr(XmlNames.COUNT, getCount());
                printScale(ctx);
            }
        };
        LocationConcrete loc = new LocationConcrete(0) {
            public String kind() {
                return "trash";
            }
        };
        entryBlock.setConcreteLocation(loc);
    }

    @Override
    public DataMethod clone(DataClass newClass, int newAccess, String newName) {
        return new DataMethodEntryOnly(newClass, newAccess, newName, this.getVmSignature(), this.getSignature(), this.getExceptions());
    }

    /**
     * Get the ID of this method entry
     *
     * @return
     */
    public int getId() {
        return entryBlock.getId();
    }

    @Override
    public boolean wasHit() {
        return entryBlock.wasHit();
    }

    @Override
    public long getCount() {
        return entryBlock.getCount();
    }

    @Override
    public void setCount(long count) {
        entryBlock.setCount(count);
    }

    /**
     * Set the scale information of this method
     *
     * @param s
     */
    public void setScale(String s) {
        entryBlock.readScale(s);
    }

    @Override
    public Scale getScale() {
        return entryBlock.scale;
    }

    @Override
    public int getSlot() {
        return entryBlock.slot;
    }

//    void readScale(String s) {
//        if (s != null && s.length() > 0) {
//            try {
//                DataRoot r = DataRoot.getInstance(rootId);
//                ScaleOptions opts = r.getScaleOpts();
//                entryBlock.scale = new Scale(s.toCharArray(), s.length(),
//                        opts.getScaleSize(), opts.getScaleCompressor(), opts.scalesCompressed());
//            } catch (JcovFileFormatException ex) {
//            }
//        }
//    }
    @Override
    void xmlGen(XmlContext ctx) {
        super.xmlGenBodiless(ctx);
    }

    @Override
    void xmlAttrs(XmlContext ctx) {
        super.xmlAttrs(ctx);
        entryBlock.xmlAttrs(ctx);

    }

    @Override
    public void checkCompatibility(DataMethod other, String trace) throws MergeException {
        if (!(other instanceof DataMethodEntryOnly)) {
            throw new MergeException("Method has other type than it's"
                    + " merging copy, expected DataMethodEntryOnly; found " + other.getClass().getSimpleName(),
                    trace, MergeException.CRITICAL);
        }

        DataMethodEntryOnly m = (DataMethodEntryOnly) other;
        if (!getDataRoot().getParams().isDynamicCollect() && !other.getDataRoot().getParams().isDynamicCollect()) {
            if (getId() != m.getId()) {
                throw new MergeException("Method has other id than it's"
                        + " merging copy, expected " + getId() + "; found " + m.getId(),
                        trace, MergeException.CRITICAL);
            }
        }
    }

    @Override
    public void merge(DataMethod other) {
        DataMethodEntryOnly m = (DataMethodEntryOnly) other;

        entryBlock.mergeScale(m.entryBlock);
        entryBlock.setCount(getCount() + m.getCount());
    }

    @Override
    public Iterator<DataBlock> iterator() {
        return new OneElemIterator(entryBlock);
    }

    @Override
    public List<DataBlock> getBlocks() {
        ArrayList<DataBlock> list = new ArrayList<DataBlock>(1);
        list.add(entryBlock);
        return list;
    }

    @Override
    public List<DataBranch> getBranches() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<DataBlockTarget> getBranchTargets() {
        return Collections.EMPTY_LIST;
    }

    void writeObject(DataOutput out) throws IOException {
        super.writeObject(out);
        entryBlock.writeObject(out);
    }

    DataMethodEntryOnly(DataClass parent, DataInput in) throws IOException {
        super(parent, in);
        entryBlock = new DataBlockMethEnter(parent.rootId, in) {
            @Override
            void xmlAttrs(XmlContext ctx) {
                ctx.attr(XmlNames.ID, getId());
                ctx.attr(XmlNames.COUNT, getCount());
                printScale(ctx);
            }
        };
    }
}