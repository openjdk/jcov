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

import static com.sun.tdk.jcov.report.DataType.*;

/**
 * Class that allows to accumulate coverage of package with it subpackages.
 *
 * @author Dmitry Fazunenko
 * @see ProductCoverage
 * @see DataType
 * @see CoverageData
 */
public class SubpackageCoverage extends AbstractCoverage {

    private int class_cov = 0;
    private int class_anc = 0;
    private int class_tot = 0;
    private int meth_cov = 0;
    private int meth_anc = 0;
    private int meth_tot = 0;
    private int field_cov = 0;
    private int field_anc = 0;
    private int field_tot = 0;
    private int block_cov = 0;
    private int block_anc = 0;
    private int block_tot = 0;
    private int branch_cov = 0;
    private int branch_anc = 0;
    private int branch_tot = 0;
    private int line_cov = 0;
    private int line_anc = 0;
    private int line_tot = 0;

    public SubpackageCoverage() {
    }

    /**
     * Appends given coverage data to the already collected ones.
     *
     * @param cov
     */
    public void add(AbstractCoverage cov) {
        CoverageData d = cov.getData(CLASS);
        class_cov += d.getCovered();
        class_anc += d.getAnc();
        class_tot += d.getTotal();
        d = cov.getData(METHOD);
        meth_cov += d.getCovered();
        meth_anc += d.getAnc();
        meth_tot += d.getTotal();
        d = cov.getData(FIELD);
        field_cov += d.getCovered();
        field_anc += d.getAnc();
        field_tot += d.getTotal();
        d = cov.getData(BLOCK);
        block_cov += d.getCovered();
        block_anc += d.getAnc();
        block_tot += d.getTotal();
        d = cov.getData(BRANCH);
        branch_cov += d.getCovered();
        branch_anc += d.getAnc();
        branch_tot += d.getTotal();
        d = cov.getData(LINE);
        line_cov += d.getCovered();
        line_anc += d.getAnc();
        line_tot += d.getTotal();
    }
    private DataType[] supportedColumns = {CLASS, METHOD, FIELD,
        BLOCK, BRANCH, LINE};

    @Override
    public DataType getDataType() {
        return DataType.PRODUCT;
    }

    public CoverageData getData(DataType column) {
        return getData(column, -1);
    }

    @Override
    public CoverageData getData(DataType column, int testNumber) {
        switch (column) {
            case CLASS:
                return new CoverageData(class_cov, class_anc, class_tot);
            case METHOD:
                return new CoverageData(meth_cov, meth_anc, meth_tot);
            case FIELD:
                return new CoverageData(field_cov, field_anc, field_tot);
            case BLOCK:
                return new CoverageData(block_cov, block_anc, block_tot);
            case BRANCH:
                return new CoverageData(branch_cov, branch_anc, branch_tot);
            case LINE:
                return new CoverageData(line_cov, line_anc, line_tot);
            default:
                return new CoverageData();
        }
    }

    @Override
    protected DataType[] getDataTypes() {
        return supportedColumns;
    }

    public String getName() {
        return "";
    }

    public boolean isCovered() {
        return getData(DataType.CLASS).getCovered() > 0;
    }

    @Override
    /**
     * throws UnsupportedOperationException
     */
    public boolean isCoveredByTest(int testnum) {
        throw new UnsupportedOperationException("Not supported here yet.");
    }
}
