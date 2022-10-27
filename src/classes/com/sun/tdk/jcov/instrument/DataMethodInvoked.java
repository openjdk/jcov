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

import com.sun.tdk.jcov.data.Scale;
import com.sun.tdk.jcov.runtime.Collect;
import com.sun.tdk.jcov.runtime.CollectDetect;
import com.sun.tdk.jcov.tools.OneElemIterator;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Leonid Mesnik
 *
 * This class represents information about abstract and native methods. For
 * static instrumentation, contains information about mapping whole method's
 * signature to collector's slot (class Collect), which used in static
 * instrumentation. This is done by invoking DataBlock's constructor, which
 * grabs new slot from collector. This number is stored in jcov file then as
 * method id (see getId() method) and used by StaticInvokeMethodAdapter later
 * during ClassMorph2 work. For dynamic instrumentation method invocations are
 * stored inside separate array (see CollectDetect), and jcov runtime knows
 * mapping of signatures to slot indexes (see InvokeMethodAdapter). Instructions
 * to hit CollectDetect are inserted by InvokeMethodAdapter later.
 *
 * To serve native methods in dynamic mode, another mechanism is used -
 * mechanism of native wrappers.
 */
public class DataMethodInvoked extends DataMethod {

    /**
     * Method block information
     */
    private final DataBlock entryBlock;

    /**
     * Creates a new instance of DataMethodInvoked
     *
     * @param k
     * @param access
     * @param name
     * @param desc
     * @param signature
     * @param exceptions
     */
    public DataMethodInvoked(final DataClass k,
            final int access,
            final String name,
            final String desc,
            final String signature,
            final String[] exceptions) {
        this(k, access, name, desc, signature, exceptions, -1);
    }

    /**
     * Creates a new instance of DataMethodInvoked
     *
     * @param k
     * @param access
     * @param name
     * @param desc
     * @param signature
     * @param exceptions
     * @param id
     */
    public DataMethodInvoked(final DataClass k,
            final int access,
            final String name,
            final String desc,
            final String signature,
            final String[] exceptions,
            int id) {
        super(k, access, name, desc, signature, exceptions, false);

        boolean newSlot = (id == -1) ? true : false;
        int slot = (newSlot) ? Collect.newSlot() : id;
        entryBlock = new DataBlock(rootId, slot, newSlot, 0) {
            @Override
            public String kind() {
                return "invoked"; //not used
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

            @Override
            protected void xmlAttrs(XmlContext ctx) {
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
        return new DataMethodInvoked(newClass, newAccess, newName, this.getVmSignature(), this.getSignature(), this.getExceptions());
    }

    /**
     * Get the ID of this method
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
    public void setCount(long cnt) {
        entryBlock.setCount(cnt);
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

    @Override
    public void xmlGen(XmlContext ctx) {
        super.xmlGenBodiless(ctx);
    }

    @Override
    void xmlAttrs(XmlContext ctx) {
        super.xmlAttrs(ctx);
        entryBlock.xmlAttrs(ctx);

    }

    @Override
    public void checkCompatibility(DataMethod other, String trace) throws MergeException {
        if (!(other instanceof DataMethodInvoked)) {
            throw new MergeException("Method has other type than it's"
                    + " merging copy, expected DataMethodInvoked; found " + other.getClass().getSimpleName(),
                    trace, MergeException.CRITICAL);
        }

        DataMethodInvoked m = (DataMethodInvoked) other;
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
        DataMethodInvoked m = (DataMethodInvoked) other;

        entryBlock.mergeScale(m.entryBlock);
        entryBlock.setCount(getCount() + m.getCount());
    }

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

    DataMethodInvoked(final DataClass parent, DataInput in) throws IOException {
        super(parent, in);
        entryBlock = new DataBlock(rootId, in) {
            @Override
            public String kind() {
                return "invoked"; //not used
            }

            @Override
            protected boolean wasCollectHit() {
                return CollectDetect.wasInvokeHit(DataAbstract.getInvokeID(parent.getFullname(), name, vmSig));
            }

            @Override
            protected long collectCount() {
                return CollectDetect.invokeCountFor(DataAbstract.getInvokeID(parent.getFullname(), name, vmSig));
            }

            @Override
            protected void setCollectCount(long count) {
                CollectDetect.setInvokeCountFor(DataAbstract.getInvokeID(parent.getFullname(), name, vmSig), count);
            }

            @Override
            protected void xmlAttrs(XmlContext ctx) {
                ctx.attr(XmlNames.ID, getId());
                ctx.attr(XmlNames.COUNT, getCount());
                printScale(ctx);
            }
        };
    }
}