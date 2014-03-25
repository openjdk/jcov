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
package com.sun.tdk.jcov.tools;

/**
 * This class is a centralized storage for 9 coverage statistics numbers. Also
 * offers some service methods.
 *
 * @author Konstantin Bobrovsky
 */
public class JcovStats {

    public final static String sccsVersion = "%I% $LastChangedDate: 2011-08-25 16:47:35 +0400 (Thu, 25 Aug 2011) $";
    public int methods_tot;
    public int methods_cov;
    public float method_cvg;
    public int blocks_tot;
    public int blocks_cov;
    public float block_cvg;
    public int branches_tot;
    public int branches_cov;
    public float branch_cvg;
    public final static int IND_MET_COV = 0;
    public final static int IND_MET_TOT = 1;
    public final static int IND_MET_CVG = 2;
    public final static int IND_BLO_COV = 3;
    public final static int IND_BLO_TOT = 4;
    public final static int IND_BLO_CVG = 5;
    public final static int IND_BRN_COV = 6;
    public final static int IND_BRN_TOT = 7;
    public final static int IND_BRN_CVG = 8;

    /**
     * Mergers &lt;this&gt; stats with another so that &lt;this&gt; contains sum
     * of both
     */
    public void merge(JcovStats src) {
        blocks_tot += src.blocks_tot;
        blocks_cov += src.blocks_cov;
        branches_tot += src.branches_tot;
        branches_cov += src.branches_cov;
        methods_tot += src.methods_tot;
        methods_cov += src.methods_cov;
    }

    /**
     * Calculates coverage percentage fields
     */
    public void calculate() {
        block_cvg = blocks_tot != 0 ? (float) blocks_cov / (float) blocks_tot : 1.0f;
        branch_cvg = branches_tot != 0 ? (float) branches_cov / (float) branches_tot : 1.0f;
        method_cvg = methods_tot != 0 ? (float) methods_cov / (float) methods_tot : 1.0f;
    }

    /**
     * @return number in the field with the specified index wrapping it in an
     * Object
     */
    public Object getNumber(int field_ind) {
        switch (field_ind) {
            case IND_MET_COV:
                return Integer.valueOf(methods_cov);
            case IND_MET_TOT:
                return Integer.valueOf(methods_tot);
            case IND_MET_CVG:
                if (methods_tot > 0) {
                    return Float.valueOf((float) ((int) (method_cvg * 100000.0)) / 1000);
                }
                return "N/A";

            case IND_BLO_COV:
                return Integer.valueOf(blocks_cov);
            case IND_BLO_TOT:
                return Integer.valueOf(blocks_tot);
            case IND_BLO_CVG:
                if (blocks_tot > 0) {
                    return Float.valueOf((float) ((int) (block_cvg * 100000.0)) / 1000);
                }
                return "N/A";

            case IND_BRN_COV:
                return Integer.valueOf(branches_cov);
            case IND_BRN_TOT:
                return Integer.valueOf(branches_tot);
            case IND_BRN_CVG:
                if (branches_tot > 0) {
                    return Float.valueOf((float) ((int) (branch_cvg * 100000.0)) / 1000);
                }
                return "N/A";

            default:
                return null;
        }
    }
}
