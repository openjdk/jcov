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

import com.sun.tdk.jcov.util.NaturalComparator;
import com.sun.tdk.jcov.filter.MemberFilter;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Iterator;

/**
 * DataPackage contains information about package hierarchy. Includes a list of
 * classes.
 *
 * @see #classes
 * @author Robert Field
 */
public class DataPackage extends DataAbstract implements Comparable<DataPackage> {

    /**
     * Name of associated package
     */
    private final String name;

    private String moduleName;
    /**
     * Classes in this package
     */
    private final List<DataClass> classes;

    /**
     * Creates a new instance of DataPackage
     *
     * @param rootId
     * @param name Associated package name
     */
    public DataPackage(int rootId, String name, String modulename) {
        super(rootId);
        this.name = name;
        this.moduleName = modulename;
        this.classes = new LinkedList<DataClass>();
    }

    public void setModuleName(String moduleName){
        this.moduleName = moduleName;
    }

    public String getModuleName(){
        return moduleName;
    }

    /**
     * Get associated package name
     *
     * @return associated package name
     */
    public String getName() {
        return name;
    }

    /**
     * Add a class to this package. Doesn't check that class is unique in the
     * package.
     *
     * @param k
     */
    public void addClass(DataClass k) {
        classes.add(k);
    }

    /**
     *
     * @return all classes in this package
     */
    public List<DataClass> getClasses() {
        return classes;
    }

    /**
     * Get DataClass by name
     *
     * @param className
     * @return DataClass with name <b>className</b> or null of this DataPackage
     * doesn't contain this class
     */
    public DataClass findClass(String className) {
        for (DataClass cl : classes) {
            if (cl.getName().equals(className)) {
                return cl;
            }
        }

        return null;
    }

    public boolean removeClass(DataClass k) {
        return classes.remove(k);
    }

    public boolean removeClass(String k) {
        if (k == null) {
            return false;
        }
        Iterator<DataClass> it = classes.iterator();
        while (it.hasNext()) {
            if (it.next().getName().equals(k)) {
                it.remove();
                return true;
            }
        }

        return false;
    }

    /**
     * XML Generation. Not supposed to use outside.
     */
    @Override
    public String kind() {
        return XmlNames.PACKAGE;
    }

    /**
     * XML Generation
     */
    @Override
    void xmlAttrs(XmlContext ctx) {
        ctx.attr(XmlNames.NAME, name.replace('/', '.'));
        ctx.attr(XmlNames.MODULE_NAME, moduleName);
    }

    /**
     * XML Generation
     */
    @Override
    void xmlBody(XmlContext ctx) {
        Collections.sort(classes);
        for (DataClass k : classes) {
            k.xmlGen(ctx);
        }
    }

    public int compareTo(DataPackage pack) {
        return NaturalComparator.INSTANCE.compare(this.name, pack.getName());
    }

    /**
     * Removes classes rejected by the filter. For each accepted class applies
     * the same filter to eliminate unwanted members.
     *
     * @param filter
     */
    public boolean applyFilter(MemberFilter filter) {
        Iterator<DataClass> it = classes.iterator();
        while (it.hasNext()) {
            DataClass clazz = it.next();
            if (!filter.accept(clazz)) {
                it.remove();
            } else {
                clazz.applyFilter(filter);
            }
        }

        return classes.isEmpty();
    }

    /**
     * Sort out all classes in this package
     */
    public void sort() {
        Collections.sort(classes);
        for (DataClass c : classes) {
            c.sort();
        }
    }

    void writeObject(DataOutput out) throws IOException {
        out.writeUTF(name);
        out.writeUTF(moduleName);
        out.writeShort(classes.size());
        for (DataClass dc : classes) {
            dc.writeObject(out);
        }
    }

    DataPackage(int rootID, DataInput in) throws IOException {
        super(rootID);
        name = in.readUTF();
        moduleName = in.readUTF();
        int classNum = in.readUnsignedShort();
        classes = new ArrayList<DataClass>(classNum);
        for (int i = 0; i < classNum; ++i) {
            classes.add(new DataClass(rootID, in));
        }
    }
}