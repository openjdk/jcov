/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.jcov.runtime;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * When tests are run with JTReg, the most reliable approach to obtain the tes name
 * is through the "test.name" system property. Same could be used in other environments.
 */
public class TestNameDecorator implements SaverDecorator {

    private static final String UNKNOWN_TEST_NAME = "UNKNOWN_TEST";
    private JCovSaver wrapped;
    private static final Lock lock = new ReentrantLock();
    //Note that additional enhancement needs to be implemented to allow
    //different properties to be used to specify the test name.
    //Depending on a test harness, it may be needed to set such value during
    //the instrumentation. JTReg's property is used as the default for now.
    //Additional enhancement yet needs to be implemented to use
    //"jcov.testname" as a property name, because it is right now
    //used to pass the test name down to saveResults()
    private static final String testNameProperty = "test.name";

    private String getTestName() {
        return System.getProperty(testNameProperty, UNKNOWN_TEST_NAME);
    }

    public void init(JCovSaver wrap) {
        this.wrapped = wrap;
    }

    public void saveResults() {
        lock.lock();
        try {
            System.setProperty("jcov.testname", getTestName());
            wrapped.saveResults();
        } finally {
            lock.unlock();
        }
    }
}
