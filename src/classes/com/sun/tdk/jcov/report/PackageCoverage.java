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

import com.sun.tdk.jcov.instrument.DataClass;
import com.sun.tdk.jcov.instrument.DataPackage;
import com.sun.tdk.jcov.instrument.DataRoot;
import com.sun.tdk.jcov.report.javap.JavapClass;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <p> This class provides access to package coverage information. Sums of
 * underlying coverage information can be obtained by
 * <code>getData(DataType)</code> method using DataType.CLASS, DataType.METHOD,
 * DataType.FIELD, DataType.BLOCK, DataType.BRANCH, DataType.LINE. </p>
 *
 * @author Leonid Mesnik
 * @see ProductCoverage
 * @see ClassCoverage
 * @see DataType
 * @see CoverageData
 */
public class PackageCoverage extends AbstractCoverage implements Iterable<ClassCoverage> {

    private String name;
    private List<ClassCoverage> classCoverageList;
    private DataType[] supportedColumns = {DataType.CLASS, DataType.METHOD,
        DataType.BLOCK, DataType.BRANCH, DataType.LINE};

    /**
     * <p> Creates new PackageCoverage instance. </p>
     *
     * @param fileImage DataRoot to read data from
     * @param name Name of the package
     * @param srcRoots Paths for sources
     * @param filter Allows to filter read data
     */
    public PackageCoverage(DataRoot fileImage, String name, String srcRoots[], ProductCoverage.CoverageFilter filter) {
        this(fileImage, name, srcRoots, null, filter);
    }

    public PackageCoverage(DataRoot fileImage, String name, String srcRoots[], List<JavapClass> javapClasses, ProductCoverage.CoverageFilter filter) {
        this(fileImage, name, srcRoots, null, filter, false);
    }

    public PackageCoverage(DataRoot fileImage, String name, String srcRoots[], List<JavapClass> javapClasses, ProductCoverage.CoverageFilter filter, boolean anonym) {
        this.name = name;
        classCoverageList = _getClassCoverageList(fileImage, srcRoots, javapClasses, filter, anonym);
        if (classCoverageList != null) {
            java.util.Collections.sort(classCoverageList);
        }
    }

    /**
     * @return all classes in this package
     */
    public List<ClassCoverage> getClasses() {
        return classCoverageList;
    }

    private List<ClassCoverage> _getClassCoverageList(DataRoot fileImage, String srcRoots[], List<JavapClass> javapClasses, ProductCoverage.CoverageFilter filter, boolean anonym) {
        List<ClassCoverage> result = new ArrayList<ClassCoverage>();
        DataPackage pkg = fileImage.findPackage(name, "");
        for (DataClass cls : pkg.getClasses()) {
            ClassCoverage cc = new ClassCoverage(cls, srcRoots, javapClasses, filter, anonym);
            if (filter == null || filter.accept(cc)) {
                result.add(cc);
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PackageCoverage other = (PackageCoverage) obj;
        if (this.name == null || !this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }

    /**
     * <p> Allows to get sums over the coverage of this package. E.g.
     * getData(DataType.METHOD) will return coverage data containing the total
     * number of methods in this package and number of covered methods in this
     * package. </p> <p> Allows to sum though CLASS, METHOD, FIELD, BLOCK,
     * BRANCH and LINE types </p>
     *
     * @param column Type to sum
     * @return CoverageData representing 2 fields - total number of members and
     * number of covered members
     * @see DataType
     * @see CoverageData
     */
    public CoverageData getData(DataType column) {
        return getData(column, -1);
    }

    public CoverageData getData(DataType column, int testNumber) {
        switch (column) {
            case CLASS:
            case METHOD:
            case FIELD:
            case BLOCK:
            case BRANCH:
            case LINE:
                CoverageData covered = new CoverageData();
                for (ClassCoverage classCoverage : getClasses()) {
                    if (testNumber < 0 || classCoverage.isCoveredByTest(testNumber)) {
                        covered.add(classCoverage.getData(column, testNumber));
                    } else {
                        covered.add(new CoverageData(0, classCoverage.getData(column, testNumber).getTotal()));
                    }
                }
                return covered;
            default:
                return new CoverageData();
        }
    }

    /**
     * <p> Coverage kind (used in HTML reports as header for label) </p>
     *
     * @return DataType.PACKAGE
     */
    public DataType getDataType() {
        return DataType.PACKAGE;
    }

    protected DataType[] getDataTypes() {
        return supportedColumns;
    }

    /**
     * @return name of the package
     */
    @Override
    public String getName() {
        return name.replace('/', '.');
    }

    /**
     * @return true when any class is covered (getData(DataType.CLASS_COVERED) >
     * 0)
     */
    public boolean isCovered() {
        return getData(DataType.CLASS).getCovered() > 0;
    }

    /**
     * @return true if this any class of this package is covered (at least 1 hit
     * was made) by specified test (by position in the testlist)
     */
    @Override
    public boolean isCoveredByTest(int testnum) {
        for (ClassCoverage c : classCoverageList) {
            if (c.isCoveredByTest(testnum)) {
                return true;
            }
        }
        return false;
    }

    public Iterator<ClassCoverage> iterator() {
        return classCoverageList.iterator();
    }

    /**
     * @return false if package doesn't contain any classes
     */
    public boolean isEmpty() {
        return classCoverageList.isEmpty();
    }
}