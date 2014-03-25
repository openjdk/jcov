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

/**
 * @author Dmitry Fazunenko
 * @author Sergey Borodin
 */
public class MergeException extends Exception {

    String descr;
    String location;
    /**
     * How ctritical the situation:
     */
    int severity = 0;
    public static final int CRITICAL = 0;
    public static final int HIGH = 1;
    public static final int MEDIUM = 2;
    public static final int LOW = 3;

    MergeException() {
    }

    MergeException(String descr, String location, int severity) {
        this.descr = descr;
        this.location = location;
        checkSeverity(severity);
        this.severity = severity;
    }

    @Override
    public String getMessage() {
        return "Merge exception: " + descr + "\n"
                + "Exception occurred in: " + location;
    }

    public void setSeverity(int severity) {
        checkSeverity(severity);
        this.severity = severity;
    }

    public int getSeverity() {
        return severity;
    }

    private void checkSeverity(int sev) {
        if (sev < CRITICAL || sev > LOW) {
            throw new IllegalArgumentException("Internal error: Illegal severity value " + sev);
        }

    }
}
