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
 * <p> This class represents coverage data. It contains number of covered
 * members (getCovered()) and total member number (getTotal()). </p> <p>
 * getFormattedCoverage() allows to get coverage in form ready for output </p>
 *
 * @author Andrey Titov
 * @see AbstractCoverage#getData(com.sun.tdk.jcov.report.DataType)
 */
public class CoverageData {

    protected int covered;
    protected int total;
    /**
     * default formatter to use in getFormattedCoverage
     *
     * @see #getFormattedCoverage()
     * @see AbstractCoverage.CoverageFormatter
     * @see AbstractCoverage.PercentFormatter
     */
    public static final AbstractCoverage.CoverageFormatter defaultFormatter = new AbstractCoverage.PercentFormatter();

    /**
     * Creates zero-filled coverage data object
     */
    public CoverageData() {
        this.covered = this.total = 0;
    }

    /**
     * Creates coverage data object
     *
     * @param covered member hit count
     * @param total total member count
     */
    public CoverageData(int covered, int total) {
        this.covered = covered;
        this.total = total;
    }

    /**
     * @return member hit count
     */
    public int getCovered() {
        return covered;
    }

    /**
     * @param covered member hit count
     */
    public void setCovered(int covered) {
        this.covered = covered;
    }

    /**
     * @return total member count
     */
    public int getTotal() {
        return total;
    }

    /**
     * @param total total member count
     */
    public void setTotal(int total) {
        this.total = total;
    }

    /**
     * sums two coverage data objects
     *
     * @param data data object to add
     * @return this object
     */
    public CoverageData add(CoverageData data) {
        covered += data.covered;
        total += data.total;
        return this;
    }

    /**
     * <p> Formats coverage using default formatter. By default it's
     * PercentFormatter that formats coverage in form of "percent%
     * (covered/total)" or " -" if total is null </p>
     *
     * @return formatted coverage string
     * @see AbstractCoverage.CoverageFormatter
     * @see AbstractCoverage.PercentFormatter
     */
    public String getFormattedCoverage() {
        return defaultFormatter.format(this);
    }

    /**
     * Formats coverage using specified formatter
     *
     * @param f formatter to use
     * @return formatted coverage string
     * @see AbstractCoverage.CoverageFormatter
     * @see AbstractCoverage.PercentFormatter
     */
    public String getFormattedCoverage(AbstractCoverage.CoverageFormatter f) {
        return f.format(this);
    }

    /**
     * @return coverage string in form of "covered/total"
     */
    @Override
    public String toString() {
        if (total == 0) {
            return "-";
        }
        return String.format("%d/%d", covered, total);
    }
}
