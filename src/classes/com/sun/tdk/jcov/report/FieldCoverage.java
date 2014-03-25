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

import com.sun.tdk.jcov.instrument.DataField;
import java.util.Arrays;
import static com.sun.tdk.jcov.report.DataType.*;

/**
 * <p> This class provides access to field coverage information.
 * <code>getData(DataType.FIELD)</code> will return 1/1 if this field is
 * covered. </p> <p> To access specific method/field information use
 * <code>getMethodCoverageList()</code> and
 * <code>getFieldCoverageList()</code> methods </p>
 *
 * @author Dmitry Fazunenko
 */
public class FieldCoverage extends MemberCoverage {

    private DataType[] supportedColumns = {FIELD};

    /**
     * Creates new instance of FieldCoverage
     *
     * @param fld
     */
    public FieldCoverage(DataField fld) {
        count = fld.getCount();
        startLine = 0;
        modifiers = Arrays.deepToString(fld.getAccessFlags());
        name = fld.getName();
        signature = fld.getSignature();
        scale = fld.getScale();
        access = fld.getAccess();
    }

    /**
     * Coverage kind (used in HTML reports as header for label)
     *
     * @return DataType.FIELD
     */
    @Override
    public DataType getDataType() {
        return DataType.FIELD;
    }

    @Override
    /**
     * <p> Only FIELD type is allowed. </p>
     *
     * @param column Type to sum
     * @return CoverageData representing 2 fields - total number of members and
     * number of covered members
     * @see DataType
     * @see CoverageData
     * @see MemberCoverage#getHitCount()
     */
    public CoverageData getData(DataType column) {
        return getData(column, -1);
    }

    public CoverageData getData(DataType column, int testNumber) {
        switch (column) {
            case FIELD:
                return new CoverageData(count > 0 ? 1 : 0, 1);
            default:
                return new CoverageData();
        }
    }

    @Override
    protected DataType[] getDataTypes() {
        return supportedColumns;
    }
}