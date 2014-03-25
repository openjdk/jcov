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

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing an API class.
 *
 * @author Dmitry Fazunenko
 */
public class ClassDescr extends AbstractDescr {

    /**
     * List of class methods and fields
     */
    public final List<MemberDescr> members;

    /**
     * Creates an instance of ClassDescr
     *
     * @param name - full qualified class name
     */
    public ClassDescr(String name) {
        super(name);
        members = new ArrayList<MemberDescr>();
    }

    /**
     * Adds a new member to the class. Does nothing if mem is null or the class
     * already contains this member.
     *
     * @param mem
     */
    public void addMember(MemberDescr mem) {
        if (mem != null && !members.contains(mem)) {
            mem.setParent(this);
            members.add(mem);
        }
    }

    /**
     * Removes a member from the class
     *
     * @param mem
     */
    public void removeMember(MemberDescr mem) {
        if (mem != null) {
            members.remove(mem);
        }
    }

    /**
     * Finds a class method by its name and signature.
     *
     * @param name
     * @param signature
     * @return found member or null.
     */
    public MemberDescr findMethod(String name, String signature) {
        for (MemberDescr mem : members) {
            if (mem.isMethod && mem.name.equals(name) && mem.signature.equals(signature)) {
                return mem;
            }
        }
        return null;
    }

    /**
     * Finds a class method by its name and signature.
     *
     * @param name
     * @param signature
     * @return found member or null.
     */
    public MemberDescr findField(String name) {
        for (MemberDescr mem : members) {
            if (!mem.isMethod && mem.name.equals(name)) {
                return mem;
            }
        }
        return null;
    }
}
