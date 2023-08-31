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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * When the source code is available locally.
 */
public class SourcePath implements SourceHierarchy {
    private final Map<Path, List<Path>> roots;

    /**
     * Single source repository, Single class hierarchy.
     */
    public SourcePath(Path srcRoot, Path classRoot) {
        this(srcRoot, List.of(classRoot));
    }

    /**
     * Single source repository, multiple class hierarchies.
     */
    public SourcePath(Path srcRoot, List<Path> classRoots) {
        this(Map.of(srcRoot, classRoots));
    }

    /**
     * There could be multiple repositories which multiple class hierarchies within each.
     */
    public SourcePath(Map<Path, List<Path>> roots) {
        this.roots = roots;
    }

    protected Path resolveFile(Path root, String file) {
        return root.resolve(file);
    }

    @Override
    public List<String> readFile(String file) throws IOException {
        Path res;
        for (var root : roots.keySet()) {
            res = resolveFile(root, file);
            if (Files.exists(res)) return Files.readAllLines(res);
        }
        return null;
    }

    public Path resolveClass(String file) {
        Path res;
        for (var sourceRoot : roots.keySet()) {
            for (var classRoot : roots.get(sourceRoot)) {
                res = resolveFile(classRoot, file);
                if (Files.exists(res)) return res;
            }
        }
        return null;
    }

    @Override
    public String toClass(String file) {
        for (var sourceRoot : roots.keySet()) {
            var path = sourceRoot.resolve(file);
            if (Files.exists(path))  {
                for (var classRoot : roots.get(sourceRoot))
                    if (path.startsWith(classRoot))
                        return classRoot.relativize(path).toString();
            }
        }
        return null;
    }
}
