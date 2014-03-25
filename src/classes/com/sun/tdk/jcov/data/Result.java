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
package com.sun.tdk.jcov.data;

import com.sun.tdk.jcov.util.Utils;
import java.io.IOException;

/**
 * Result class contains path to JCov datafile and it's testlist
 *
 * @author Andrey Titov
 */
public class Result {

    private String resultPath;
    private String[] testList;

    /**
     * Default empty constructor
     */
    public Result() {
    }

    /**
     * Constructor for Result without testname
     *
     * @param resultPath path to the result file
     */
    public Result(String resultPath) {
        this.resultPath = resultPath;
    }

    /**
     * Constructor for Result
     *
     * @param resultPath path to the result file
     * @param testList tests associated with this result file
     */
    public Result(String resultPath, String[] testList) {
        this.resultPath = resultPath;
        this.testList = testList;
    }

    /**
     * Constructor for Result that reads testlist from the file
     *
     * @param resultPath path to the result file
     * @param testListPath path to the test list file associated with this
     * result file
     */
    public Result(String resultPath, String testListPath) throws IOException {
        this.resultPath = resultPath;
        if (testListPath != null) {
            testList = Utils.readLines(testListPath);
        } else {
            testList = null;
        }
    }

    /**
     * Constructor for Result that reads a part of testlist from the file
     *
     * @param resultPath path to the result file
     * @param testListPath path to the test list file associated with this
     * result file
     * @param start first line to be read
     * @param end last line to be read
     */
    public Result(String resultPath, String testListPath, int start, int end) throws IOException {
        this.resultPath = resultPath;
        testList = Utils.readLines(testListPath, start, end);
    }

    /**
     * Get path of result file
     *
     * @return path of result file
     */
    public String getResultPath() {
        return resultPath;
    }

    /**
     * Set path of result file
     *
     * @param resultPath
     */
    public void setResultPath(String resultPath) {
        this.resultPath = resultPath;
    }

    /**
     * Get tests list
     *
     * @return Testlist array of test names associated with this result file
     */
    public String[] getTestList() {
        return testList;
    }

    /**
     * Set tests list
     *
     * @param testList array of test names associated with this result file
     */
    public void setTestList(String[] testList) {
        this.testList = testList;
    }

    /**
     * Set tests list reading it from a file
     *
     * @param testListPath path to the test list file associated with this
     * result file
     */
    public void readTestList(String testListPath) throws IOException {
        testList = Utils.readLines(testListPath);
    }

    /**
     * Set tests list reading it from a file
     *
     * @param testListPath path to the test list file associated with this
     * result file
     * @param start first line to be read
     * @param end last line to be read
     */
    public void readTestList(String testListPath, int start, int end) throws IOException {
        testList = Utils.readLines(testListPath, start, end);
    }

    /**
     * Set single testname to the test list
     *
     * @param testname name of the test associated with this result file
     */
    public void setTestName(String testname) {
        testList = new String[]{testname};
    }

    /**
     * Set testname equal to result file name
     */
    public void setDefaultName() {
        testList = new String[]{resultPath};
    }

    public boolean isTestListSet() {
        return testList != null;
    }
}
