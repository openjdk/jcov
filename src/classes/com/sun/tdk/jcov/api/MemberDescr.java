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
package com.sun.tdk.jcov.api;

/**
 * Class representing either class method or class field.
 *
 * @author Dmitry Fazunenko
 */
public class MemberDescr extends AbstractDescr implements Comparable {

    /**
     * Parent class
     */
    private ClassDescr parent;
    /**
     * Signature of a member in VM representation format
     */
    public final String signature;
    /**
     * Flag indicating whether the member is method or field.
     */
    public final boolean isMethod;

    /**
     * Create an instance of MemberDescr
     *
     * @param name
     * @param signature
     * @param access
     */
    public MemberDescr(String name, String signature) {
        super(name);
        this.signature = signature;
        this.isMethod = signature.indexOf('(') >= 0;
    }

    public void setParent(ClassDescr parent) {
        this.parent = parent;
    }

    public ClassDescr getParent() {
        return parent;
    }

    private String toCompareString() {
        return isMethod + name + signature;
    }

    public int compareTo(Object o) {
        MemberDescr mem = (MemberDescr) o;
        return toCompareString().compareTo(mem.toCompareString());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof MemberDescr)) {
            return false;
        }
        return toCompareString().equals(((MemberDescr) obj).toCompareString());
    }

    @Override
    public int hashCode() {
        return toCompareString().hashCode();
    }
}
