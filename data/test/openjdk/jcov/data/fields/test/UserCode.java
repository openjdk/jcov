/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package openjdk.jcov.data.fields.test;

public class UserCode {
    int i;
    long j;
    float f;
    double d;
    boolean z;
    byte b;
    String s;

    public void setI(int i) {
        this.i = i;
    }

    public void setJ(long j) {
        this.j = j;
    }

    public void setF(float f) {
        this.f = f;
    }

    public void setD(double d) {
        this.d = d;
    }

    public void setZ(boolean z) {
        this.z = z;
    }

    public void setB(byte b) {
        this.b = b;
    }

    public void setS(String s) {
        this.s = s;
    }

    public static void main(String[] args) {
        UserCode o = new UserCode();
        o.setI(0); o.setJ(1); o.setF(2f); o.setD(3.); o.setZ(true); o.setB((byte)5); o.setS("6");
        o.setI(7); o.setJ(8); o.setF(9f); o.setD(10d); o.setZ(false); o.setB((byte)12); o.setS("13");
    }
}
