/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
package openjdk.codetools.jcov.report.source;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface SourceHierarchy {

    /**
     * Delivers the file source code.
     * @param file - file name within the source hierarchy.
     */
    List<String> readFile(String file) throws IOException;

    /**
     * Maps a file name (as present in source) to a class file name.
     * Example: <code>src/main/java/my/company/product/Main.hava</code> to <code>my/company/product/Main.hava</code>.
     * @param file - file name within the source hierarchy.
     */
    String toClassFile(String file);

    /**
     * Finds a file by a class file name. There could be multiple source with the same class file names,
     * the implemenation is responsible to decide which one is to be selected.
     * Example: <code>my/company/product/Main.hava</code> to <code>src/main/java/my/company/product/Main.hava</code>.
     * @param classFileName - a class file name relative to the root of a class hierarchy.
     */
    String toFile(String classFileName);

    String JAVA_EXTENSION = ".java";

    static String toClassName(String classFileName) {
        return classFileName.substring(0, classFileName.lastIndexOf(JAVA_EXTENSION));
    }

    static String toClassFileName(String className) {
        return className + JAVA_EXTENSION;
    }

}


