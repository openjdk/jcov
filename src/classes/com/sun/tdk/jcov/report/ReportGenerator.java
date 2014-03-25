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

import com.sun.tdk.jcov.instrument.InstrumentationOptions;
import com.sun.tdk.jcov.report.javap.JavapClass;
import com.sun.tdk.jcov.util.Utils;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Interface for creating custom reports
 *
 * @see ReportGeneratorSPI
 * @author Andrey Titov
 */
public interface ReportGenerator {

    /**
     * <p> JCov RepGen will call this method before reading XML datafile. </p>
     *
     * @param outputPath Value of "-output" option. "report" by default.
     * @throws IOException
     */
    public void init(String outputPath) throws IOException;

    /**
     * <p> This method should create report </p>
     *
     * @param coverage Coverage data
     * @param options Some useful data (testlist and paths to the sources)
     * @throws IOException
     * @see ProductCoverage
     */
    public void generateReport(ProductCoverage coverage, Options options) throws IOException;

    /**
     * <p> Class encapsulating some useful data </p> <p> It will be filled by
     * the RepGen and passed to each ReportGenerator </p>
     */
    public static class Options {

        /**
         * Non-parsed path to sources (eg "classes/:tests/" or "classes;tests"
         * for Win)
         */
        private String srcRootPath;
        /**
         * Parsed paths to sources
         */
        private String[] srcRootPaths;
        /**
         * TestList object.
         */
        private SmartTestService testListService;
        /**
         * Classes are results of the javap command
         */
        private List<JavapClass> classes;
        /**
         * show coverage for each test in result.xml (use when result.xml has
         * been gotten after merge operation with scales)
         */
        private boolean withTestsInfo;
        /**
         * true if RepGen has been run for several result.xml files (will give
         * different view of the result report)
         */
        private boolean mergeRepGenMode;
        private InstrumentationOptions.InstrumentationMode mode;
        private boolean anonym = false;

        /**
         * Creates empty Options
         */
        public Options() {
            srcRootPath = null;
            srcRootPaths = null;
            testListService = null;
        }

        /**
         * Creates Options instance
         *
         * @param srcRootPath Path to the sources (divided by
         * File.pathSeparator)
         * @param testListService STS object
         */
        public Options(String srcRootPath, SmartTestService testListService, List<JavapClass> classes, boolean withTestInfo, boolean mergeRepGenMode) {
            this.withTestsInfo = withTestInfo;
            this.mergeRepGenMode = mergeRepGenMode;
            this.classes = classes;
            this.srcRootPath = srcRootPath;
            this.testListService = testListService;
            if (srcRootPath != null) {
                this.srcRootPaths = Utils.getSourcePaths(srcRootPath);
            } else {
                this.srcRootPaths = null;
            }
        }

        public List<JavapClass> getJavapClasses() {
            return classes;
        }

        /**
         * Non-parsed path to sources (eg "classes/:tests/" or "classes;tests"
         * for Win)
         *
         * @return Non-parsed path to sources (eg "classes/:tests/" or
         * "classes;tests" for Win)
         */
        public String getSrcRootPath() {
            return srcRootPath;
        }

        /**
         * Parsed paths to sources
         *
         * @return Parsed paths to sources
         */
        public String[] getSrcRootPaths() {
            return srcRootPaths;
        }

        /**
         * Sets parsed paths to sources
         *
         * @param srcRootPaths Parsed paths to sources
         */
        public void setSrcRootPaths(String[] srcRootPaths) {
            this.srcRootPaths = srcRootPaths;
            srcRootPath = "";
            if (srcRootPaths.length > 0) {
                for (int i = 0; i < srcRootPaths.length - 1; ++i) {
                    srcRootPath += srcRootPaths[i] + File.pathSeparatorChar;
                }
                srcRootPath += srcRootPaths[srcRootPaths.length - 1];
            }
        }

        /**
         * TestList object
         *
         * @return TestList object
         */
        public SmartTestService getTestListService() {
            return testListService;
        }

        public void setSrcRootPath(String srcRootPath) {
            this.srcRootPath = srcRootPath;
            if (srcRootPath != null) {
                this.srcRootPaths = Utils.getSourcePaths(srcRootPath);
            }
        }

        public void setTestListService(SmartTestService testListService) {
            this.testListService = testListService;
        }

        public boolean isWithTestsInfo() {
            return withTestsInfo;
        }

        public boolean isMergeRepGenMode() {
            return mergeRepGenMode;
        }

        public void setInstrMode(InstrumentationOptions.InstrumentationMode mode) {
            this.mode = mode;
        }

        public InstrumentationOptions.InstrumentationMode getInstrMode() {
            return this.mode;
        }

        public void setAnonymOn(boolean anonym) {
            this.anonym = anonym;
        }

        public boolean isAnonymOn() {
            return anonym;
        }
    }
}