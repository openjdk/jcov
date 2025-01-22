/*
 * Copyright (c) 2014, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.jcov.report.javap;

import java.io.PrintWriter;
import java.util.NoSuchElementException;
import java.util.spi.ToolProvider;

import static java.util.spi.ToolProvider.findFirst;

public class JavapClassReader {
    public static void read(String filePath, String jarPath, PrintWriter pw) {
        int rc = 0;
        try {
            ToolProvider javap = findFirst("javap").orElseThrow();
            if (jarPath == null) {
                rc = javap.run(pw, new PrintWriter(System.err), "-c", "-p", filePath);
            } else {
                rc = javap.run(pw, new PrintWriter(System.err), "-c", "-p", "-classpath", jarPath, filePath);
            }
        } catch (NoSuchElementException ex) {
            System.err.println("Cannot find the javap tool");
        }
        if (rc != 0) {
            System.err.println("Usage: java -cp jcov.jar com.sun.tdk.jcov.RepGen -javap path_to_classes " +
                    "-o path_to_javap_output path_to_result.xml");
        }
    }
}
