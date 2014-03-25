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
/**
 * <p> This package contains classes for simplifying reports generation. </p>
 * <p> {@link com.sun.tdk.jcov.report.ProductCoverage} class and underlying
 * classes allow to get sums over coverage information for entire product or for
 * some part of it. For example it's possible to get number of covered classes
 * in all product, or number of covered classes in some package, or number of
 * covered methods in a package. See
 * {@link com.sun.tdk.jcov.report.AbstractCoverage#getData(com.sun.tdk.jcov.report.DataType)}
 * method for more details. </p> <p> ProductCoverage class is the top class in
 * coverage hierarchy. It provides access to the following classes: </p> <ul>
 * <li>PackageCoverage</li> <li>ClassCoverage</li> <li>MethodCoverage</li>
 * <li>FieldCoverage</li> <li>ItemCoverage</li> <li>LineCoverage</li> </ul> <p>
 * ProductCoverage contains a list of PackageCoverage classes, PackageCoverage
 * contains ClassCoverages, which contains MethodCoverages and FieldCoverages.
 * Each MethodCoverage contains a number of ItemCoverage classes which can be
 * BlockCoverage or BranchCoverage representing blocks and branches
 * respectively. </p> <p> LineCoverage contains information about which lines of
 * the code are covered and which are not. </p> <p> The following example shows
 * how to iterate through coverage data and how to get coverage information.
 * </p>
 * <pre>
 * <code>
 * import com.sun.tdk.jcov.report.CoverageData;
 * import com.sun.tdk.jcov.report.ClassCoverage;
 * import com.sun.tdk.jcov.report.DataType;
 * import com.sun.tdk.jcov.report.MethodCoverage;
 * import com.sun.tdk.jcov.report.PackageCoverage;
 * import com.sun.tdk.jcov.report.ProductCoverage;
 * import com.sun.tdk.jcov.util.Utils;
 * import java.util.List;
 *
 * public class Main {
 *     public static void main(String[] args) {
 *         String fileName = args[0];
 *         String testlistFileName = args.length > 1 ? args[1] : null;
 *
 *         try {
 *             ProductCoverage coverage = new ProductCoverage(fileName);
 *             String[] testlist = testlistFileName != null ? Utils.readLines(testlistFileName) : null;
 *             for (PackageCoverage pc: coverage.getPackages()) {
 *                 for (ClassCoverage cc: pc.getClasses()) {
 *                     System.out.println("# class: " + cc.getFullClassName() + " method coverage: " + cc.getCoverageString(DataType.METHOD));
 *                     for (MethodCoverage mc: cc.getMethods()) {
 *                         CoverageData blc = mc.getData(DataType.BLOCK);
 *                         CoverageData brc = mc.getData(DataType.BRANCH);
 *                         CoverageData lcc = mc.getData(DataType.LINE);
 *                         System.out.println("  meth: " + mc.getName() + mc.getSignature());
 *                         System.out.println("    blocks: " + blc + ";  branches: " + brc + "; lines: " + lcc.getCovered() + "/" + lcc.getTotal());
 *                         if (testlistFileName != null) {
 *                             List&lt;String&gt; coveringTests = mc.getCoveringTests(testlist);
 *                             System.out.println("       covered by tests: " + coveringTests.toString());
 *                         }
 *                     }
 *                 }
 *             }
 *         } catch (Throwable ex) {
 *             System.err.println("Exception " + ex);
 *         }
 *     }
 * }
 * </code>
 * </pre> <p>
 * <code>com.sun.tdk.jcov.report</code> package also contains a number of
 * classes that can be used to create custom reports. </p> <p>
 * {@link com.sun.tdk.jcov.report.ReportGenerator} class can be passed to the
 * RepGen tool through {@link com.sun.tdk.jcov.report.ReportGeneratorSPI}
 * Service Provider. Implementing these interfaces it's possible to generate
 * custom report right from JCov RepGen tool. See
 * {@link com.sun.tdk.jcov.report.DefaultReportGeneratorSPI} for an extended
 * example. </p>
 */
package com.sun.tdk.jcov.report;