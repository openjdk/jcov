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
package openjdk.codetools.jcov.report;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

/**
 * Used to define for which files, within a hierarchy, to generate report for.
 */
public class FileSet {
    private final Set<String> files;
    private final Set<String> folders;
    public FileSet(Set<String> files) {
        this.files = files;
        folders = new HashSet<>();
        folders.add("");
        files.forEach(f -> {
            var parts = f.split("/");
            var path = "";
            for (int i = 0; i < parts.length - 1; i++) {
                path += (path.isEmpty() ? "" : "/") + parts[i];
                folders.add(path);
            }
        });
    }

    public Set<String> files() {
        return files;
    }

    public Set<String> files(String parent) {
        return files.stream().filter(f ->
<<<<<<< HEAD
                parent.isEmpty  () && f.indexOf('/') < 0 ||
                        f.startsWith(parent + "/") &&
                                f.substring(parent.length() + 1).indexOf('/') < 0)
                .collect(toSet());
=======
                parent.isEmpty() && f.indexOf('/') < 0 ||
                f.startsWith(parent + "/") &&
                f.substring(parent.length() + 1).indexOf('/') < 0).collect(toSet());
>>>>>>> 05fd4cae6a4651a07ecf85903355142573484a5a
    }

    public Set<String> folders(String parent) {
        if (parent.isEmpty()) return folders.stream().filter(f -> !f.isEmpty() && f.indexOf('/') < 0).collect(toSet());
        else return folders.stream().filter(f -> f.startsWith(parent + "/") &&
                f.substring(parent.length() + 1).indexOf('/') < 0).collect(toSet());
    }

    public boolean isFile(String file) {return files.contains(file);}
}
