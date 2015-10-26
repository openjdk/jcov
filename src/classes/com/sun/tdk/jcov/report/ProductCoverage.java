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

import com.sun.tdk.jcov.data.FileFormatException;
import com.sun.tdk.jcov.filter.MemberFilter;
import com.sun.tdk.jcov.instrument.DataClass;
import com.sun.tdk.jcov.instrument.DataField;
import com.sun.tdk.jcov.instrument.DataMethod;
import com.sun.tdk.jcov.instrument.DataPackage;
import com.sun.tdk.jcov.instrument.DataRoot;
import com.sun.tdk.jcov.io.Reader;
import com.sun.tdk.jcov.report.javap.JavapClass;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <p> The product coverage container serves for accessing coverage information
 * retrieved from the XML files or any other sources. </p> <p> Following
 * coverage objects are structured hierarchically and can be browsed from the
 * <code>ProductCoverage</code>. </p> <p> <ol>
 * <li><code>{@link ProductCoverage}</code><br> A top level product
 * coverage</li> <li><code>{@link PackageCoverage}</code><br> A package
 * coverage</li> <li><code>{@link ClassCoverage}</code><br> A class
 * coverage</li> <li><code>{@link MethodCoverage}</code><br> A method
 * coverage</li> <li><code>{@link ItemCoverage}</code><br> Represents a block or
 * branch coverage (depends on the coverage format).</li>
 * <li><code>{@link FieldCoverage}</code><br> A field coverage</li>
 * <li><code>{@link LineCoverage}</code><br> Represents line coverage.</li>
 * </ol> </p> <p> <b>Accessing coverage data</b> </p> <p> The coverage data can
 * be accessed by
 * <code>getData(DataType)</code> method. This method sums coverage information
 * over all the children so
 * <code>getData(DataType.CLASS)</code> will return number of covered classes
 * and the number of all classes in the product. More information can be found
 * at {@link AbstractCoverage} interface. </p> <p> To get package information
 * use
 * <code>getPackages()</code> method. </p>
 *
 * @author Baechul Kim
 * @author Andrey Titov
 * @see PackageCoverage
 * @see ClassCoverage
 * @see MethodCoverage
 * @see ItemCoverage
 * @see LineCoverage
 * @see CoverageData
 * @see DataType
 * @see DataRoot
 * @since 1.0
 */
public class ProductCoverage extends AbstractCoverage implements Iterable<PackageCoverage> {

    private ArrayList<PackageCoverage> packages;
    private DataType[] supportedColumns = {DataType.CLASS,
        DataType.METHOD, DataType.FIELD,
        DataType.BLOCK, DataType.BRANCH, DataType.LINE};
    private ArrayList<ClassCoverage> classes;
    private String productName;
    private AncFilter[] ancfilters = null;

    /**
     * <p> Creates a new instance of ProductCoverage which is the top level
     * coverage object in the coverage objects tree. DataRoot is read from
     * <b>filename</b>. </p> <p> This constructor reads file with scales and
     * doesn't filter data. </p>
     *
     * @param filename A file to read data from.
     * @throws FileFormatException when any problem with file reading occurs.
     * @see CoverageFilter
     * @see ProductCoverage#ProductCoverage(java.lang.String, boolean,
     * java.lang.String[],
     * com.sun.tdk.jcov.report.ProductCoverage.CoverageFilter)
     */
    public ProductCoverage(String filename) throws FileFormatException {
        this(readFileImage(filename, true), null, null, null, true);
    }

    /**
     * <p> Creates a new instance of ProductCoverage which is the top level
     * coverage object in the coverage objects tree. DataRoot is read from
     * <b>filename</b>. </p> <p> This constructor reads file with scales and
     * doesn't filter data. </p>
     *
     * @param filename A file to read data from.
     * @param srcPaths Paths to sources
     * @throws FileFormatException when any problem with file reading occurs.
     * @see CoverageFilter
     * @see ProductCoverage#ProductCoverage(java.lang.String, boolean,
     * java.lang.String[],
     * com.sun.tdk.jcov.report.ProductCoverage.CoverageFilter)
     */
    public ProductCoverage(String filename, String srcPaths[]) throws FileFormatException {
        this(readFileImage(filename, true), srcPaths, null, null, true);
    }

    /**
     * <p> Creates a new instance of ProductCoverage which is the top level
     * coverage object in the coverage objects tree. DataRoot is read from
     * <b>filename</b>. </p> <p> This constructor doesn't filter data. </p>
     *
     * @param filename A file to read data from.
     * @param readScales JCov will read scales only when readScales is set to
     * true. If the file doesn't contain scales - they would be created.
     * @param srcPaths Paths to sources
     * @throws FileFormatException when any problem with file reading occurs.
     * @see CoverageFilter
     * @see ProductCoverage#ProductCoverage(java.lang.String, boolean,
     * java.lang.String[],
     * com.sun.tdk.jcov.report.ProductCoverage.CoverageFilter)
     */
    public ProductCoverage(String filename, boolean readScales, String srcPaths[]) throws FileFormatException {
        this(readFileImage(filename, readScales), srcPaths, null, null, true);
    }

    /**
     * <p> Creates a new instance of ProductCoverage which is the top level
     * coverage object in the coverage objects tree. DataRoot is read from
     * <b>filename</b>. </p> <p> This constructor doesn't filter data. </p>
     *
     * @param filename A file to read data from.
     * @param readScales JCov will read scales only when readScales is set to
     * true. If the file doesn't contain scales - they would be created.
     * @throws FileFormatException when any problem with file reading occurs.
     * @see CoverageFilter
     * @see ProductCoverage#ProductCoverage(java.lang.String, boolean,
     * java.lang.String[],
     * com.sun.tdk.jcov.report.ProductCoverage.CoverageFilter)
     */
    public ProductCoverage(String filename, boolean readScales) throws FileFormatException {
        this(readFileImage(filename, readScales), null, null, null, true);
    }

    /**
     * <p> Creates a new instance of ProductCoverage which is the top level
     * coverage object in the coverage objects tree. DataRoot is read from
     * <b>filename</b>. </p>
     *
     * @param filename A file to read data from.
     * @param readScales JCov will read scales only when readScales is set to
     * true. If the file doesn't contain scales - they would be created.
     * @param srcPaths Paths to sources
     * @param filter Allows to filter data
     * @throws FileFormatException when any problem with file reading occurs.
     * @see CoverageFilter
     */
    public ProductCoverage(String filename, boolean readScales, String srcPaths[], CoverageFilter filter) throws FileFormatException {
        this(Reader.readXML(filename, readScales, filter), srcPaths, null, filter, true);
    }

    /**
     * <p> Creates a new instance of ProductCoverage which is the top level
     * coverage object in the coverage objects tree. DataRoot is read from
     * <b>filename</b>. </p> <p> A default filter is used to control reading of
     * non-public and abstract members. </p>
     *
     * @param filename A file to read data from.
     * @param readScales JCov will read scales only when readScales is set to
     * true. If the file doesn't contain scales - they would be created.
     * @param srcPaths Paths to sources
     * @param isPublicAPI Read only public API (public or protected members).
     * Use CoverageFilter to control filtering more precisely.
     * @param noAbstract Do not read abstract members
     * @throws FileFormatException when any problem with file reading occurs.
     * @see CoverageFilter
     * @see ProductCoverage#ProductCoverage(java.lang.String, boolean,
     * java.lang.String[],
     * com.sun.tdk.jcov.report.ProductCoverage.CoverageFilter)
     */
    public ProductCoverage(String filename, boolean readScales, String srcPaths[], boolean isPublicAPI, boolean noAbstract) throws FileFormatException {
        this(readFileImage(filename, readScales), srcPaths, null, new DefaultFilter(isPublicAPI, noAbstract), true);
    }

    /**
     * <p> Creates a new instance of ProductCoverage which is the top level
     * coverage object in the coverage objects tree. <p> <p> A default filter is
     * used to control reading of non-public and abstract members. </p>
     *
     * @param fileImage DataRoot representing coverage information. DataRoot can
     * be read with DataRoot.read() method
     * @throws FileFormatException
     * @see CoverageFilter
     * @see DataRoot
     * @see DataRoot#read(java.lang.String)
     * @see
     * ProductCoverage#ProductCoverage(com.sun.tdk.jcov.instrument.DataRoot,
     * java.lang.String[],
     * com.sun.tdk.jcov.report.ProductCoverage.CoverageFilter)
     */
    public ProductCoverage(DataRoot fileImage) {
        this(fileImage, null, null, null, false);
    }

    /**
     * <p> Creates a new instance of ProductCoverage which is the top level
     * coverage object in the coverage objects tree. <p> <p> A default filter is
     * used to control reading of non-public and abstract members. </p>
     *
     * @param fileImage DataRoot representing coverage information. DataRoot can
     * be read with DataRoot.read() method
     * @param srcPaths Paths to sources
     * @throws FileFormatException
     * @see CoverageFilter
     * @see DataRoot
     * @see DataRoot#read(java.lang.String)
     * @see
     * ProductCoverage#ProductCoverage(com.sun.tdk.jcov.instrument.DataRoot,
     * java.lang.String[],
     * com.sun.tdk.jcov.report.ProductCoverage.CoverageFilter)
     */
    public ProductCoverage(DataRoot fileImage, String srcPaths[]) {
        this(fileImage, srcPaths, null, null, false);
    }

    /**
     * <p> Creates a new instance of ProductCoverage which is the top level
     * coverage object in the coverage objects tree. <p> <p> A default filter is
     * used to control reading of non-public and abstract members. </p>
     *
     * @param fileImage DataRoot representing coverage information. DataRoot can
     * be read with DataRoot.read() method
     * @param srcRootPaths Paths to sources
     * @param isPublicAPI Read only public API (public or protected members).
     * Use CoverageFilter to control filtering more precisely.
     * @param noAbstract Do not read abstract members
     * @throws FileFormatException
     * @see CoverageFilter
     * @see DataRoot
     * @see DataRoot#read(java.lang.String)
     * @see
     * ProductCoverage#ProductCoverage(com.sun.tdk.jcov.instrument.DataRoot,
     * java.lang.String[],
     * com.sun.tdk.jcov.report.ProductCoverage.CoverageFilter)
     */
    public ProductCoverage(DataRoot fileImage, String srcRootPaths[], List<JavapClass> javapClasses, boolean isPublicAPI, boolean noAbstract) {
        this(fileImage, srcRootPaths, javapClasses, new DefaultFilter(noAbstract, isPublicAPI), false, false);
    }

    public ProductCoverage(DataRoot fileImage, String srcRootPaths[], List<JavapClass> javapClasses, boolean isPublicAPI, boolean noAbstract, AncFilter[] ancfilters) {
        this(fileImage, srcRootPaths, javapClasses, new DefaultFilter(noAbstract, isPublicAPI), false, false, ancfilters);
    }

    public ProductCoverage(DataRoot fileImage, String srcRootPaths[], List<JavapClass> javapClasses, boolean isPublicAPI, boolean noAbstract, boolean anonym) {
        this(fileImage, srcRootPaths, javapClasses, new DefaultFilter(noAbstract, isPublicAPI), false, anonym, null);
    }

    public ProductCoverage(DataRoot fileImage, String srcRootPaths[], List<JavapClass> javapClasses, boolean isPublicAPI, boolean noAbstract, boolean anonym, AncFilter[] ancfilters) {
        this(fileImage, srcRootPaths, javapClasses, new DefaultFilter(noAbstract, isPublicAPI), false, anonym, ancfilters);
    }

    /**
     * <p> Creates a new instance of ProductCoverage which is the top level
     * coverage object in the coverage objects tree. </p> <p> Note that empty
     * packages/classes will not be stored. </p>
     *
     * @param fileImage DataRoot representing coverage information. DataRoot can
     * be read with DataRoot.read() method
     * @param srcRootPaths Paths to sources
     * @param filter Allows to filter data
     * @throws FileFormatException
     * @see CoverageFilter
     * @see DataRoot
     * @see DataRoot#read(java.lang.String)
     */
    public ProductCoverage(DataRoot fileImage, String srcRootPaths[], CoverageFilter filter) {
        this(fileImage, srcRootPaths, null, filter, false);
    }

    public ProductCoverage(DataRoot fileImage, String srcRootPaths[], List<JavapClass> javapClasses, CoverageFilter filter, boolean releaseAfter) {
        this(fileImage, srcRootPaths, null, filter, false, false);
    }

    public ProductCoverage(DataRoot fileImage, String srcRootPaths[], List<JavapClass> javapClasses, CoverageFilter filter, boolean releaseAfter, boolean anonym){
        this(fileImage, srcRootPaths, null, filter, false, false, null);
    }
    /**
     * <p> Creates a new instance of ProductCoverage which is the top level
     * coverage object in the coverage objects tree. </p> <p> Note that empty
     * packages/classes will not be stored. </p>
     *
     * @param fileImage DataRoot representing coverage information. DataRoot can
     * be read with DataRoot.read() method
     * @param srcRootPaths Paths to sources
     * @param filter Allows to filter data
     * @param releaseAfter Allows to automatically destroy DataRoot right after
     * ProductCoverage initialization
     * @throws FileFormatException
     * @see CoverageFilter
     * @see DataRoot
     * @see DataRoot#read(java.lang.String)
     */
    public ProductCoverage(DataRoot fileImage, String srcRootPaths[], List<JavapClass> javapClasses, CoverageFilter filter, boolean releaseAfter, boolean anonym, AncFilter[] ancfilters) {
        packages = new ArrayList<PackageCoverage>();
        classes = new ArrayList<ClassCoverage>();
        if (filter == null) {
            filter = new DefaultFilter(false, false);
        }
        this.ancfilters = ancfilters;

        List<DataPackage> packs = fileImage.getPackages();
        ArrayList<String> packageNames = new ArrayList<String>(packs.size());
        for (DataPackage p : packs) {
            packageNames.add(p.getName());
        }
        java.util.Collections.sort(packageNames);

        for (String pkg : packageNames) {
            PackageCoverage pc = new PackageCoverage(fileImage, pkg, srcRootPaths, javapClasses, filter, ancfilters, anonym);
            List<ClassCoverage> pkgClasses = pc.getClasses();
            classes.addAll(pkgClasses);
            if (filter.accept(pc)) {
                packages.add(pc);
            }
        }

        if (releaseAfter) {
            fileImage.destroy();
        }
    }

    /**
     * Determine if the ANC filters were set
     * @return
     */
    public boolean isAncFiltersSet(){
        return ancfilters != null;
    }

    private static DataRoot readFileImage(String filename, boolean useScales)
            throws FileFormatException {
        return Reader.readXML(filename, useScales, null);
    }

    /**
     * <p> Coverage kind (used in HTML reports as header for label). DataType is
     * used to get sums over the coverage through getData() method </p>
     *
     * @return DataType.PRODUCT
     * @see DataType
     * @see ProductCoverage#getData(com.sun.tdk.jcov.report.DataType)
     */
    public DataType getDataType() {
        return DataType.PRODUCT;
    }

    /**
     * @return packages in this product
     */
    public List<PackageCoverage> getPackages() {
        return packages;
    }

    /**
     * @return iterator for packages in this product
     */
    public Iterator<PackageCoverage> iterator() {
        return packages.iterator();
    }

    /**
     * <p> Allows to get sums over the coverage of this product. E.g.
     * getData(DataType.CLASS) will return coverage data containing the total
     * number of classes in the product and number of covered classes in the
     * product. </p> <p> Allows to sum though PACKAGE, CLASS, METHOD, FIELD,
     * BLOCK, BRANCH and LINE types </p>
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
        CoverageData covered = new CoverageData(0, 0, 0);
        switch (column) {
            case PACKAGE:
                for (PackageCoverage pCoverage : packages) {
                    covered.add(pCoverage.getData(column));
                }
                return covered;
            case CLASS:
            case METHOD:
            case FIELD:
            case BLOCK:
            case BRANCH:
            case LINE:
                for (ClassCoverage classCoverage : classes) {
                    if (testNumber < 0 || classCoverage.isCoveredByTest(testNumber)) {
                        covered.add(classCoverage.getData(column));
                    } else {
                        covered.add(new CoverageData(0, 0, classCoverage.getData(column).getTotal()));
                    }
                }
                return covered;
            default:
                return new CoverageData();
        }
    }

    protected DataType[] getDataTypes() {
        return supportedColumns;
    }

    /**
     * <p> Set the product name </p>
     *
     * @param name
     */
    public void setName(String name) {
        productName = name;
    }

    /**
     * @return product name
     */
    @Override
    public String getName() {
        return productName == null ? "" : productName;
    }

    /**
     * @return true when any class is covered (getData(DataType.CLASS_COVERED) >
     * 0)
     */
    public boolean isCovered() {
        for (ClassCoverage c : classes) {
            if (c.isCovered()) {
                return true;
            }
        }
        return false;
    }

    @Override
    /**
     * @return true if any class in this product is covered (at least 1 hit was
     * made) by specified test (by position in the testlist)
     */
    public boolean isCoveredByTest(int testnum) {
        for (ClassCoverage c : classes) {
            if (c.isCoveredByTest(testnum)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Default filter that allows to filter abstract and non-public members.
     * Also filters out empty classes and packages.
     */
    public static class DefaultFilter extends CoverageFilter {

        private boolean noAbstract;
        private boolean isPublicAPI;

        public DefaultFilter(boolean noAbstract, boolean isPublicAPI) {
            this.noAbstract = noAbstract;
            this.isPublicAPI = isPublicAPI;
        }

        public boolean accept(DataClass clz) {
            return true;
        }

        public boolean accept(DataClass clz, DataMethod m) {
            return !(noAbstract && m.isAbstract() || isPublicAPI && !m.isPublicAPI());
        }

        public boolean accept(DataClass clz, DataField f) {
            return !(isPublicAPI && !f.isPublicAPI());
        }

        @Override
        public boolean accept(ClassCoverage cc) {
            return !(!cc.isPublicAPI() && isPublicAPI || cc.isEmpty() && noAbstract);
        }

        @Override
        public boolean accept(PackageCoverage cc) {
            return !cc.isEmpty();
        }
    }

    /**
     * Filter for ProductCoverage. Allows to filter ClassCoverage and
     * PackageCoverage after their full construction
     */
    public static abstract class CoverageFilter implements MemberFilter {

        public abstract boolean accept(ClassCoverage cc);

        public abstract boolean accept(PackageCoverage cc);
    }
}
