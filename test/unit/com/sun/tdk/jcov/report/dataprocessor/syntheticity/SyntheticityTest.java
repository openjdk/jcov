/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.jcov.report.dataprocessor.syntheticity;

import com.sun.tdk.jcov.data.FileFormatException;
import com.sun.tdk.jcov.instrument.DataClass;
import com.sun.tdk.jcov.instrument.DataPackage;
import com.sun.tdk.jcov.instrument.DataRoot;
import com.sun.tdk.jcov.io.Reader;
import com.sun.tdk.jcov.processing.DefaultDataProcessorSPI;
import com.sun.tdk.jcov.processing.ProcessingException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class SyntheticityTest {

    static DataRoot data;
    static String template;

    @BeforeClass
    public static void setup() {
        template = SyntheticityTest.class.getPackageName()
                .replace(".", "/") + "/synthetic_template.xml";
    }
    @Test
    public void load() throws FileFormatException {
        data = Reader.readXML(ClassLoader.getSystemResourceAsStream(template));
        DataPackage p = data.findPackage("package");
        DataClass sc1 = p.findClass("SyntheticityClass$SyntheticClass");
        assertTrue(sc1.findMethod("syntheticMethod").getModifiers().isSynthetic());
        assertFalse(sc1.findMethod("nonSyntheticMethod").getModifiers().isSynthetic());
        DataClass sc2 = p.findClass("SyntheticityClass$NonSyntheticClass");
        assertTrue(sc2.findMethod("syntheticMethod").getModifiers().isSynthetic());
        assertFalse(sc2.findMethod("nonSyntheticMethod").getModifiers().isSynthetic());
    }
    @Test(dependsOnMethods = "load")
   public void transform() throws ProcessingException {
        data = new DefaultDataProcessorSPI().getDataProcessor().process(data);
        DataPackage p = data.findPackage("package");
        DataClass sc = p.findClass("SyntheticityClass");
        assertTrue(sc.findMethod("$SyntheticClass.syntheticMethod").getModifiers().isSynthetic());
        assertFalse(sc.findMethod("$SyntheticClass.nonSyntheticMethod").getModifiers().isSynthetic());
        assertTrue(sc.findMethod("$NonSyntheticClass.syntheticMethod").getModifiers().isSynthetic());
        assertFalse(sc.findMethod("$NonSyntheticClass.nonSyntheticMethod").getModifiers().isSynthetic());
    }
}
