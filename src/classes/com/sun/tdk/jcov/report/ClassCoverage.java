/*
 * Copyright (c) 2014, 2022 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tdk.jcov.data.Scale;
import com.sun.tdk.jcov.filter.MemberFilter;
import com.sun.tdk.jcov.instrument.DataClass;
import com.sun.tdk.jcov.instrument.DataField;
import com.sun.tdk.jcov.instrument.DataMethod;
import com.sun.tdk.jcov.instrument.Modifiers;
import com.sun.tdk.jcov.report.javap.JavapClass;
import com.sun.tdk.jcov.util.Utils;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p> This class provides access to class coverage information. Sums of
 * underlying coverage information can be obtained by
 * <code>getData(DataType)</code> method using DataType.CLASS, DataType.METHOD,
 * DataType.FIELD, DataType.BLOCK, DataType.BRANCH, DataType.LINE. </p> <p> To
 * access specific method/field information use
 * <code>getMethodCoverageList()</code> and
 * <code>getFieldCoverageList()</code> methods </p>
 *
 * @see ProductCoverage
 * @see DataType
 * @see CoverageData
 *
 * @author Dmitry Fazunenko
 * @author Alexey Fedorchenko
 */
public class ClassCoverage extends AbstractCoverage {

    private String source;
    private boolean javapSource = false;
    private JavapClass javapClass;
    private List<MethodCoverage> methods = new ArrayList<MethodCoverage>();
    private List<FieldCoverage> fields = new ArrayList<FieldCoverage>();
    private LineCoverage lineCoverage = new LineCoverage();
    private DataType[] supportedColumns = {DataType.CLASS, DataType.METHOD, DataType.FIELD,
        DataType.BLOCK, DataType.BRANCH, DataType.LINE};
    private final int access;
    private final String fullname;
    private final String name;
    private final String packagename;
    private final String modulename;
    private final Modifiers modifiers;
    private boolean isInAnc = false;
    protected String ancInfo;

    /**
     * <p> Creates new ClassCoverage instance. </p>
     *
     * @param clz DataClass to read data from
     * @param srcRootPaths Paths for sources
     * @param filter Allows to filter read data
     */
    public ClassCoverage(DataClass clz, String srcRootPaths[], MemberFilter filter) {
        this(clz, srcRootPaths, null, filter);
    }

    public ClassCoverage(DataClass clz, String srcRootPaths[], List<JavapClass> javapClasses, MemberFilter filter) {
        this(clz, srcRootPaths, null, filter, null, false);
    }

    public ClassCoverage(DataClass clz, String srcRootPaths[], List<JavapClass> javapClasses, MemberFilter filter, AncFilter[] ancFilters, boolean anonym) {
        access = clz.getAccess();
        fullname = clz.getFullname();
        name = clz.getName();
        packagename = clz.getPackageName();
        modulename = clz.getModuleName();
        modifiers = clz.getModifiers();

        if (ancFilters != null){
            for (AncFilter ancFilter : ancFilters){
                if (ancFilter.accept(clz)){
                    isInAnc = true;
                    setAncInfo(ancFilter.getAncReason());
                    break;
                }
            }
        }

        for (DataMethod method : clz.getMethods()) {
            if (filter != null && !filter.accept(clz, method)) {
                continue;
            }

            MethodCoverage methodCoverage = null;
            if (ancFilters != null){
                for (AncFilter ancFilter : ancFilters){
                    if (isInAnc || ancFilter.accept(clz, method)){
                        methodCoverage = new MethodCoverage(method, ancFilters, ancFilter.getAncReason());
                        methodCoverage.setAncInfo(ancFilter.getAncReason());
                        break;
                    }
                }
            }
            if (methodCoverage == null) {
                methodCoverage = new MethodCoverage(method, ancFilters, null);
            }
            methodCoverage.setAnonymOn(anonym);
            if (method.getName() != null && method.getName().matches("\\$\\d.*")) {
                methodCoverage.setInAnonymClass(true);
            }
            if (method.getModifiers().isSynthetic() && method.getName().startsWith("lambda$")){
                methodCoverage.setLambdaMethod(true);
            }

            methods.add(methodCoverage);
            lineCoverage.processLineCoverage(methodCoverage.getLineCoverage());
        }
        for (DataField field : clz.getFields()) {
            if (filter != null && !filter.accept(clz, field)) {
                continue;
            }

            FieldCoverage fieldCoverage = new FieldCoverage(field);
            fields.add(fieldCoverage);
        }

        if (javapClasses == null) {
            this.source = findBestSource(clz, srcRootPaths);
        } else {
            javapSource = true;

            for (JavapClass jpClass : javapClasses) {
                if (jpClass != null
                        && jpClass.getClassName() != null
                        && (jpClass.getClassName()).equals(name)) {

                    javapClass = jpClass;
                    break;
                }
            }
        }

        Collections.sort(methods);
        Collections.sort(fields);
    }

    /**
     * @return true if the class doesn't contain neither methods or fields
     */
    public boolean isEmpty() {
        return methods.isEmpty() && fields.isEmpty();
    }

    /**
     * <p> Use getAccess() method to check for more specific modifiers.
     * getAccess() method returns a bit-mask of org.objectweb.asm.Opcodes
     * constants. </p>
     *
     * @return true if class access modifiers are <b>public</b> or
     * <b>protected</b>
     * @see ClassCoverage#getAccess()
     */
    public boolean isPublicAPI() { return modifiers.isPublic() || modifiers.isProtected(); }

    /**
     * <p> Use getAccess() method to check for more specific modifiers.
     * getAccess() method returns a bit-mask of org.objectweb.asm.Opcodes
     * constants. </p>
     *
     * @return true if class is <b>public</b> or <b>protected</b>
     * @see ClassCoverage#getAccess()
     */
    public boolean isPublic() {
        return modifiers.isPublic();
    }

    /**
     * <p> Use getAccess() method to check for more specific modifiers.
     * getAccess() method returns a bit-mask of org.objectweb.asm.Opcodes
     * constants. </p>
     *
     * @return true if class is <b>public</b>
     * @see ClassCoverage#getAccess()
     */
    public boolean isPrivate() {
        return modifiers.isPrivate();
    }

    /**
     * <p> Use getAccess() method to check for more specific modifiers.
     * getAccess() method returns a bit-mask of org.objectweb.asm.Opcodes
     * constants. </p>
     *
     * @return true if class is <b>protected</b>
     * @see ClassCoverage#getAccess()
     */
    public boolean isProtected() {
        return modifiers.isProtected();
    }

    /**
     * <p> Use getAccess() method to check for more specific modifiers.
     * getAccess() method returns a bit-mask of org.objectweb.asm.Opcodes
     * constants. </p>
     *
     * @return true if class is <b>abstract</b>
     * @see ClassCoverage#getAccess()
     */
    public boolean isAbstract() {
        return modifiers.isAbstract();
    }

    /**
     * <p> Use getAccess() method to check for more specific modifiers.
     * getAccess() method returns a bit-mask of org.objectweb.asm.Opcodes
     * constants. </p>
     *
     * @return true if class is <b>final</b>
     * @see ClassCoverage#getAccess()
     */
    public boolean isFinal() {
        return modifiers.isFinal();
    }

    /**
     * <p> Use this method to check for specific modifiers. </p>
     *
     * @return Access bit-mask of org.objectweb.asm.Opcodes constants.
     */
    public int getAccess() {
        return access;
    }

    /**
     * @param test Number of a test in the testlist
     * @return true when any method in this class is marked as hit by the
     * <b>test</b> bit in scales
     * @see Scale
     */
    public boolean isCoveredByTest(int test) {
        for (MethodCoverage method : methods) {
            Scale s = method.getScale();
            if (s != null && s.isBitSet(test)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return classname in VM notation
     */
    public String getName() {
        return name;
    }

    /**
     * @return getData(DataType.CLASS_COVERED) > 0;
     */
    public boolean isCovered() {
        for (MethodCoverage method : methods) {
            if (method.count > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return methods in this class
     */
    public List<MethodCoverage> getMethods() {
        return methods;
    }

    /**
     * @return fields in this class
     */
    public List<FieldCoverage> getFields() {
        return fields;
    }

    /**
     * @return package name in VM notation
     */
    public String getPackageName() {
        return packagename;
    }

    public String getModuleName() {
        return modulename;
    }

    /**
     * @return canonical classname
     */
    public String getFullClassName() {
        return fullname.replace('/', '.');
    }

    /**
     * @return classname in VM notation ('/' as package separators)
     */
    public String getFullClassNameFilename() {
        return fullname;
    }

    /**
     * Get class sourcefile
     *
     * @return class sourcefile
     */
    public String getSource() {
        return source;
    }

    public boolean isJavapSource() {
        return javapSource;
    }

    public JavapClass getJavapClass() {
        return javapClass;
    }

    /**
     * Coverage kind (used in HTML reports as header for label)
     *
     * @return DataType.CLASS
     */
    public DataType getDataType() {
        return DataType.CLASS;
    }

    protected DataType[] getDataTypes() {
        return supportedColumns;
    }

    /**
     * Returns true if passed line is covered, false otherwise.
     *
     * @param lineNum line number
     * @return true if passed line is covered, false otherwise.
     */
    public boolean isLineCovered(int lineNum) {
        return lineCoverage.isLineCovered(lineNum);
    }

    public boolean isLineInAnc(int lineNum){
        return lineCoverage.isLineAnc(lineNum);
    }

    public void setAncInfo(String ancInfo){
        isInAnc = (ancInfo != null && !ancInfo.isEmpty());
        this.ancInfo = ancInfo;
    }

    public String getAncInfo(){
        return ancInfo;
    }

    /**
     * Returns true if the line with the given number contains java code
     *
     * @param lineNum line number
     * @return true if the line with the given number contains java code
     */
    public boolean isCode(long lineNum) {
        return lineCoverage.isCode(lineNum);
    }

    /**
     * <p> Allows to get sums over the coverage of this class. E.g.
     * getData(DataType.METHOD) will return coverage data containing the total
     * number of methods in this class and number of covered methods in this
     * class. </p> <p> Allows to sum though CLASS, METHOD, FIELD, BLOCK, BRANCH
     * and LINE types </p>
     *
     * @param column Type to sum
     * @return CoverageData representing 2 fields - total number of members and
     * number of covered members
     * @see DataType
     * @see CoverageData
     */
    public CoverageData getData(DataType column, int testNumber) {
        switch (column) {
            case CLASS:
                boolean allMethodsInANC = true;
                for (MethodCoverage method : methods) {
                    if (method.count > 0 && (testNumber < 0 || method.isCoveredByTest(testNumber))) {
                        if (isInAnc) {
                            return new CoverageData(1, 1, 1);
                        }
                        return new CoverageData(1, 0, 1);
                    }
                    if (method.count <= 0 && !method.isMethodInAnc()){
                        allMethodsInANC = false;
                    }
                }
                if (isInAnc || (allMethodsInANC && methods.size() > 0)) {
                    return new CoverageData(0, 1, 1);
                }
                return new CoverageData(0, 0, 1);
            case METHOD:
            case BLOCK:
            case BRANCH:
                CoverageData covered = new CoverageData(0, 0, 0);
                for (MethodCoverage method : methods) {
                    if (testNumber < 0 || method.isCoveredByTest(testNumber)) {
                        covered.add(method.getData(column, testNumber));
                    } else {
                        CoverageData mcov = method.getData(column, testNumber);
                        covered.add(new CoverageData(0, mcov.getAnc() ,mcov.getTotal()));
                    }
                }
                return covered;
            case FIELD:
                covered = new CoverageData(0, 0, 0);
                for (FieldCoverage field : fields) {
                    covered.add(field.getData(column));
                }
                return covered;
            case LINE:
                return new CoverageData(lineCoverage.getCovered(), lineCoverage.getAnc(), lineCoverage.getTotal());
            default:
                return new CoverageData();
        }
    }

    public CoverageData getData(DataType column) {
        return getData(column, -1);
    }

    /**
     * Finds a source that is the most acceptable for this DataClass
     *
     * @param clz
     * @param source_paths
     * @return
     */
    private static String findBestSource(DataClass clz, String[] source_paths) {

        if (source_paths == null) {
            return clz.getSource();
        }

        final char sep = File.separatorChar;

        String source_name = clz.getSource();
        boolean dummy = false;

        if (source_name == null) {
            String clzName = clz.getName();
            int indexOf = clzName.indexOf('$');
            if (indexOf > 0) {
                clzName = clzName.substring(0, indexOf);
            } else {
                clzName = clzName + ".java";
            }

            source_name = clzName;
            dummy = true;
        }

        if (source_name.startsWith(pref) && source_name.endsWith(suff)) {
            source_name = source_name.substring(0, source_name.length() - suff.length());
            source_name = source_name.substring(pref.length());
            dummy = true;
        }
        source_name = source_name.replace('/', sep);
        source_name = source_name.replace('\\', sep);
        source_name = Utils.basename(source_name);
        if (dummy) {
            int i = source_name.indexOf("$");
            if (i > 0) {
                source_name = source_name.substring(0, i) + ".java";
            }
        }
        String pckg = clz.getPackageName();
        source_name = pckg.replace('/', File.separatorChar) + sep + source_name;

        for (int i = 0; i < source_paths.length; i++) {
            File f = new File(source_paths[i] + source_name);
            if (f.exists()) {
                return f.getAbsolutePath();
            }
            else{
                if (clz.getModuleName() != null) {
                    if (source_paths[i].contains("#module")) {
                        f = new File(source_paths[i].replaceAll("\\#module", clz.getModuleName()) + source_name);
                        if (f.exists()) {
                            return f.getAbsolutePath();
                        }
                    }

                    f = new File(source_paths[i].concat(clz.getModuleName()).concat(String.valueOf(sep)).concat(source_name));
                    if (f.exists()) {
                        return f.getAbsolutePath();
                    }
                }
            }
        }
        return clz.getSource(); // not found
    }
    static final String pref = "<UNKNOWN_SOURCE/";
    static final String suff = ">";
}
