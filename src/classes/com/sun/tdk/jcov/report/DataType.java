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
package com.sun.tdk.jcov.report;

/**
 * <p> Enumeration of coverage data types. </p>
 *
 * @author Baechul Kim
 * @since 1.0
 */
public enum DataType {

    PRODUCT("Product"),
    /**
     * Value of java class package coverage kind.
     */
    PACKAGE("Package"),
    /**
     * Value of java class coverage kind .
     */
    CLASS("Class"),
    /**
     * Value of java method coverage kind.
     */
    METHOD("Method"),
    /**
     * Value of java field coverage kind.
     */
    FIELD("Field"),
    /**
     * Value of java block coverage kind.
     */
    BLOCK("Block"),
    /**
     * Value of java branch coverage kind.
     */
    BRANCH("Branch"),
    /**
     * Value of native(c/c++) source file coverage kin .
     */
    SOURCE("Source"),
    /**
     * Value of native(c/c++) line coverage kind.
     */
    LINE("Line"),
    ITEM("Item"),
    ITEM_START_LINE("Start Line"),
    ITEM_END_LINE("End Line"),
    ITEM_COVERAGE_FORMAT("Coverage Format"),
    ITEM_NATIVE_KIND("Native Kind"),
    ITEM_HIT_COUNT("Hit Count");
    private String title;

    private DataType(String title) {
        this.title = title;
    }

    /**
     * Return a short description of this enum value, which can be used as a
     * name or title in the representation.
     *
     * @return a short description of this enum value, which can be used as a
     * name or title in the representation.
     */
    public String getTitle() {
        return title;
    }

    void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return getTitle();
    }
}
