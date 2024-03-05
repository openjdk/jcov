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
import java.util.Map;

public class SourceHierarchyUnion implements SourceHierarchy {
    private final List<SourceHierarchy> subHierarchies;
    private final Map<SourceHierarchy, Path> locations;

    public SourceHierarchyUnion(List<SourceHierarchy> paths, Map<SourceHierarchy, Path> locations) {
        subHierarchies = paths;
        this.locations = locations;
    }

    @Override
    public List<String> readFile(String file) throws IOException {
        for (var sp : subHierarchies) {
            var path = Path.of(file);
            if (locations != null && locations.containsKey(sp))
                path = locations.get(sp).relativize(path);
            List<String> result = sp.readFile(path.toString());
            if (result != null) return result;
        }
        return null;
    }

    @Override
    public String toClassFile(String file) {
        for (var sp : subHierarchies) {
            var path = Path.of(file);
            if (locations != null && locations.containsKey(sp))
                path = locations.get(sp).relativize(path);
            var res = sp.toClassFile(path.toString());
            if (res != null) return res;
        }
        return null;
    }

    @Override
    public String toFile(String classFileName) {
        for (var sp : subHierarchies) {
            var res = sp.toFile(classFileName);
            if (res != null) {
                if (locations != null && locations.containsKey(sp))
                    res = locations.get(sp).resolve(res).toString();
                return res;
            }
        }
        return null;
    }
}
