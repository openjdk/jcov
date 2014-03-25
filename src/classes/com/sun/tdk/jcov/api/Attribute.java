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
 * Class representing name-value pair.
 *
 * @author Dmitry Fazunenko
 */
public class Attribute {

    /**
     * Attribute name
     */
    public final String name;
    /**
     * Attribute value
     */
    public final String value;

    /**
     * Creates an instance of Attribute
     *
     * @param name - name of attribute
     * @param value - value of attribute
     */
    public Attribute(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * @return string as name=value
     */
    @Override
    public String toString() {
        return name + "=" + value;
    }

    public static Attribute parse(String token) {
        if (token == null) {
            return null;
        }
        int index = token.indexOf('=');
        if (index < 1) {
            return null;
        }

        return new Attribute(token.substring(0, index),
                token.substring(index + 1));
    }
}
