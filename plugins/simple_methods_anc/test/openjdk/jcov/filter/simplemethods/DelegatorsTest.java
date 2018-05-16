/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package openjdk.jcov.filter.simplemethods;

import org.objectweb.asm.tree.ClassNode;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.assertEquals;

public class DelegatorsTest {

    Delegators any_name_delegator;
    Delegators same_name_delegator;
    ClassNode cls;

    @BeforeTest
    public void init() throws IOException {
        any_name_delegator = new Delegators();
        same_name_delegator = new Delegators(true);
        cls = TestUtils.findTestClass(this.getClass());
    }

    @DataProvider(name = "cases")
    public Object[][] cases() {
        return new Object[][] {
                {same_name_delegator, "foo(Ljava/lang/String;I)I", false, "Simple getter"},
                {same_name_delegator, "foo(I)I", true, "Using constants or parameters"},
                {same_name_delegator, "foo(J)I", true, "Using fields"},
                {same_name_delegator, "foo(Z)I", false, "Having condition"},
                {same_name_delegator, "foo(F)I", false, "Calling other methods"},
                {same_name_delegator, "bar(I)I", false, "Different method"},
                {any_name_delegator, "bar(I)I", true, "Different method"},
                {any_name_delegator, "empty()V", false, "Empty"}
        };
    }
    @Test(dataProvider = "cases")
    public void test(Delegators delegator, String method, boolean result, String description) throws IOException {
        assertEquals(delegator.test(cls, TestUtils.findTestMethod(cls, method)), result, description);
    }

    //test data
    int aField = 0;
    static String aStaticField = null;

    int foo(String i, int j) {return j;}

    int foo(int j) {
        return foo("", j);
    }

    int foo(long s) {
        return foo(aStaticField, aField);
    }

    int foo(boolean s) {
        if(s)
            return foo(1);
        else
            return foo(0);
    }

    int foo(float s) {
        foo(null, 0);
        return foo(null, 1);
    }

    int bar(int j) {
        return foo(null, j);
    }

    void empty() {
    }
}
