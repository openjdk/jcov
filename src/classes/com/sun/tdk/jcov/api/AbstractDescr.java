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
 * Base class for class and class member description.
 *
 * @author Dmitry Fazunenko
 */
public abstract class AbstractDescr {

    /**
     * Name of the abstract element
     */
    public final String name;
    /**
     * Custom attributes
     */
    public final List<Attribute> attributes;

    public AbstractDescr(String name) {
        this.name = name;
        this.attributes = new ArrayList<Attribute>();
    }

    /**
     * Invokes addAttribute(new Attribute(name, value));
     */
    public void addAttribute(String name, String value) {
        addAttribute(new Attribute(name, value));
    }

    /**
     * Adds attribute to the list of element attributes. Does nothing if attr is
     * null, or element already contains such attribute.
     *
     * @param attr
     */
    public void addAttribute(Attribute attr) {
        if (attr != null && !attributes.contains(attr)) {
            attributes.add(attr);
        }
    }

    /**
     * Removes attribute from the list of element attributes. Does nothing if
     * attr is null.
     *
     * @param attr
     */
    public void removeAttribute(Attribute attr) {
        if (attr != null) {
            attributes.remove(attr);
        }
    }

    /**
     * Finds the first attribute with specified name.
     *
     * @param attrName - name of the attribute
     * @return value of the first found attribute or null, if not found
     */
    public String getAttribute(String attrName) {
        List<String> list = getAllAttributes(attrName);
        if (list != null && list.size() > 0) {
            return list.get(0);
        } else {
            return null;
        }
    }

    /**
     * Returns values of all found attributes with the specified name.
     *
     * @param attrName - name of the attribute
     * @return a list of values or an empty list, if no attributes found.
     */
    public List<String> getAllAttributes(String attrName) {
        ArrayList<String> list = new ArrayList<String>();
        for (Attribute attr : attributes) {
            if (attr.name.equals(attrName)) {
                list.add(attr.value);
            }
        }
        return list;
    }
}
