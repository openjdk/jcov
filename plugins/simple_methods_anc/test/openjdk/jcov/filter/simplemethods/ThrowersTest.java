/*
 * Copyright (c) 2018, 2024 Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.classfile.ClassModel;

import static openjdk.jcov.filter.simplemethods.TestUtils.findTestMethod;
import static org.testng.Assert.assertEquals;

public class ThrowersTest {

    Throwers tested;
    ClassModel cls;

    @BeforeTest
    public void init() throws IOException {
        tested = new Throwers();
        cls = TestUtils.findTestClass(this.getClass());
    }

    @DataProvider(name = "cases")
    public Object[][] cases() {
        return new Object[][] {
                {"notImplemented()V", true, "A thrower"},
                {"compoundMessage()V", false, "A thrower with a computed message"},
                {"empty()V", false, "Empty"}
        };
    }
    @Test(dataProvider = "cases")
    public void test(String method, boolean result, String description) {
        assertEquals(tested.test(cls, findTestMethod(cls, method)), result, description);
    }

    //test data
    String message = "not ";
    void empty() {}
    void notImplemented(){throw new RuntimeException(message);}
    void compoundMessage(){throw new RuntimeException(message + "implemented");}
}
