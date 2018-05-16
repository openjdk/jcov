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

public class EmptyMethodsTest {

    EmptyMethods tested;
    ClassNode cls;

    @BeforeTest
    public void init() throws IOException {
        tested = new EmptyMethods();
        cls = TestUtils.findTestClass(this.getClass());
    }

    @DataProvider(name = "cases")
    public Object[][] cases() {
        return new Object[][] {
                {"empty()V", true, "Empty"},
                {"get()I", false, "A getter"},
                {"init()V", false, "init()"}
        };
    }
    @Test(dataProvider = "cases")
    public void test(String method, boolean result, String description) {
        assertEquals(tested.test(cls, TestUtils.findTestMethod(cls, method)), result, description);
    }

    //test data
    void empty() {
        //doing nothing
    }
    int get(){return 0;}
}
