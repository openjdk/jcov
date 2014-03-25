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

import java.io.PrintStream;

/**
 *
 *
 * @author Robert Field
 */
public abstract class LocationAbstract extends DataAbstract implements Comparable<LocationAbstract> {

    public LocationAbstract(int rootId) {
        super(rootId);
    }

    public int compareTo(LocationAbstract loc) {
        int comp = startBCI() - loc.startBCI();
        if (comp == 0) {
            // make sure non-equal don't return zero
            comp = hashCode() - loc.hashCode();
        }
        return comp;
    }

    @Override
    public String toString() {
        return kind() + "(" + startBCI() + "," + endBCI() + ")@" + Integer.toHexString(hashCode());
    }

    abstract int startBCI();

    abstract int endBCI();

    /**
     * XML Generation
     */
    @Override
    void xmlAttrs(XmlContext ctx) {
        ctx.attr(XmlNames.START, startBCI());
        ctx.attr(XmlNames.END, endBCI());
    }

    /**
     * Debug printing
     */
    void printDebug(PrintStream out, String indent) {
        out.print(indent);
        out.println(kind() + "(" + startBCI() + "," + endBCI() + ")");
    }
}
