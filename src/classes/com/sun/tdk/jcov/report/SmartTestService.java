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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Leonid Mesnik
 */
public class SmartTestService implements Iterable<Test> {

    private List<Test> allTests;

    /**
     * <p> Creates empty STS </p>
     */
    public SmartTestService() {
        allTests = new ArrayList<Test>();
    }

    /**
     * <p> Creates a STS from the testlist </p>
     *
     * @param tests testlist as array of testnames
     */
    public SmartTestService(String[] tests) {
        allTests = new ArrayList<Test>(tests.length);
        for (String test : tests) {
            allTests.add(new SmartTest(test));
        }
    }

    /**
     * <p> Creates a STS from the input stream. Testnames should be divided by a
     * newline. </p>
     *
     * @param reader Input stream to read
     */
    public SmartTestService(BufferedReader reader) {
        try {
            allTests = new ArrayList<Test>();
            String name = null;
            while ((name = reader.readLine()) != null) {
                allTests.add(new SmartTest(name));
                //  System.out.println("Add " + name);
            }
            //System.out.println("Read " + allTests.size());
        } catch (IOException ioe) {
            throw new Error(ioe);
        }
    }

    /**
     * <p> Get all tests covering a certain class </p>
     *
     * @param clz Class to check
     * @return List of tests which are covering the class
     */
    public List<Test> getHitTestByClasses(ClassCoverage clz) {
        List<Test> result = new ArrayList<Test>();
        if (allTests.isEmpty()) {
            result.addAll(allTests);
        } else {
            for (int i = 0; i < allTests.size(); i++) {
                if (clz.isCoveredByTest(i)) {
                    result.add(allTests.get(i));
                }
            }
        }
        return result;
    }

    public List<Test> getAllTests(){
        return  allTests;
    }

    /**
     * <p> Get number of tests in the STS </p>
     *
     * @return Count of tests
     */
    public int getTestCount() {
        return allTests.size();
    }

    public Iterator<Test> iterator() {
        return allTests.iterator();
    }

    private static class SmartTest implements Test {

        private String name;
        private String testername;

        SmartTest(String name) {
            this.name = name;
        }

        SmartTest(String name, String testername) {
            this.name = name;
            this.testername = testername;
        }

        public String getTestName() {
            return name;
        }

        public String getTestOwner() {
            return testername;
        }
    }
}
