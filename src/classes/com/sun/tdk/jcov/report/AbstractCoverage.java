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

import com.sun.tdk.jcov.util.NaturalComparator;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for Product, Package, Class, Method, Field, Block, Branch and Line
 * Coverage
 *
 * @author Leonid Mesnik
 * @author Dmitry Fazunenko
 */
public abstract class AbstractCoverage implements Comparable {

    /**
     * @return DataType representing type of this coverage data (eg
     * DataType.CLASS for classes coverage information)
     */
    public abstract DataType getDataType();

    /**
     * <p> Sums coverage over selected {@link DataType}. Every coverage member
     * has it own list of supported DataTypes to sum. E.g. ClassCoverage can sum
     * coverage only for classes (itself), methods, fields, blocks, branches and
     * lines. </p> <p> getData(DataType) sums the underlying coverage and
     * returns a {@link CoverageData} object which encapsulates 2 numbers -
     * number of <b>all</b> underlying elements and number of <b>covered</b>
     * elements. E.g. ClassCoverage.getData(DataType.METHOD) will return number
     * of all methods in this class and number of covered methods in this class.
     * </p>
     *
     * @param type type coverage to sum
     * @return Summed coverage in CoverageData object
     */
    public abstract CoverageData getData(DataType type);

    public abstract CoverageData getData(DataType type, int testNumber);

    /**
     * @return member name (eg packagename of classname)
     */
    public abstract String getName();

    /**
     * @return true if this member is covered (at least 1 hit was made)
     */
    public abstract boolean isCovered();

    /**
     * @return true if this member is covered (at least 1 hit was made) by
     * specific test (by position in the testlist)
     */
    public abstract boolean isCoveredByTest(int testnum);

    protected abstract DataType[] getDataTypes();

    /**
     * @param kind
     * @return true if this coverage member has underlying <b>kind</b> member
     * (eg Class has Fields and Methods)
     */
    public final boolean isSupported(DataType kind) {
        for (DataType name : getDataTypes()) {
            if (name.equals(kind)) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p> Formats coverage using default formatter. By default it's
     * PercentFormatter that formats coverage in form of "percent%
     * (covered/total)" or " -" if total is null </p>
     *
     * @param type Data type
     * @return Formatted coverage data
     * @see AbstractCoverage#getData(com.sun.tdk.jcov.report.DataType)
     * @see CoverageData#getFormattedCoverage()
     */
    public final String getCoverageString(DataType type) {
        return getCoverageString(type, false);
    }

    public final String getCoverageString(DataType type, boolean withAnc) {
        CoverageData data = getData(type);
        return data.getFormattedCoverage(withAnc);
    }

    /**
     * <p> Formats coverage using specified formatter. </p>
     *
     * @param type Data type
     * @param f Formatter to format string
     * @return Formatted coverage data
     * @see AbstractCoverage#getData(com.sun.tdk.jcov.report.DataType)
     * @see
     * CoverageData#getFormattedCoverage(com.sun.tdk.jcov.report.AbstractCoverage.CoverageFormatter)
     */
    public final String getCoverageString(DataType type, CoverageFormatter f) {
        CoverageData data = getData(type);
        return data.getFormattedCoverage(f);
    }

    public final String getCoverageString(DataType type, CoverageANCFormatter f, boolean withAnc) {
        CoverageData data = getData(type);
        return data.getFormattedCoverage(f, withAnc);
    }

    public int compareTo(Object obj) {
        return NaturalComparator.INSTANCE.compare(this.getName(), ((AbstractCoverage) obj).getName());
    }

    /**
     * @param testlist Testlist in order of tests were run
     * @return List of testnames covering this member
     */
    public List<String> getCoveringTests(String testlist[]) {
        ArrayList<String> list = new ArrayList<String>(testlist.length / 10);
        for (int i = 0; i < testlist.length; ++i) {
            if (isCoveredByTest(i)) {
                list.add(testlist[i]);
            }
        }
        return list;
    }

    /**
     * CoverageFormatter serves to format CoverageData objects into strings
     */
    public static interface CoverageFormatter {

        /**
         *
         * @param data CoverageData object to format
         * @return formatted coverage data
         */
        public String format(CoverageData data);
    }

    public static interface CoverageANCFormatter {

        /**
         * @param data CoverageData object to format
         * @param withAnc Show acceptable not covered data
         * @return formatted coverage data
         */
        public String format(CoverageData data, boolean withAnc);

    }

    /**
     * Default CoverageFormatter that formats coverage in form of "percent%
     * (covered/total)" or " -" if total is null
     */
    public static class PercentFormatter implements CoverageFormatter, CoverageANCFormatter {

        public String format(CoverageData data){
            if (data.total == 0) {
                return " -";
            } else {
                return String.format("%0$4.0f%% (%d/%d)", (float) data.covered / data.total * 100., data.covered, data.total);
            }
        }

        public String format(CoverageData data, boolean withAnc){
            if (data.total == 0) {
                return " -";
            } else {
                if (!withAnc) {
                    return String.format("%0$4.0f%% (%d/%d)", (float) data.covered / data.total * 100., data.covered, data.total);
                }
                return String.format("%0$4.0f%% (%d/%d/%d)", (float) (data.covered + data.anc) / (data.total) * 100., data.covered, data.anc, data.total);
            }
        }
    }
}
