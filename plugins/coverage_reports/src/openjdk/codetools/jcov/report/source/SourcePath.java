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
    private final List<Path> repositories;

    /**
     * Single source repository, Single class hierarchy.
     */
    public SourcePath(Path repositorY, Path root) {
        this(repositorY, List.of(root));
    }

    /**
     * Single source repository, multiple class hierarchies.
     */
    public SourcePath(Path repository, List<Path> roots) {
        this(List.of(repository), Map.of(repository, roots));
    }

    /**
     * There could be multiple repositories which multiple class hierarchies within each.
     * @param repositories List of source
     */
    public SourcePath(List<Path> repositories, Map<Path, List<Path>> roots) {
        this.roots = roots;
        this.repositories = repositories;
    }

    /**
     * Maps class-root-relative path to repository-root-relative.
     * @param classFileName
     * @return
     */
    @Override
    public String toFile(String classFileName) {
        for (var source : repositories)
            for (var root : roots.get(source)) {
                var file = root.resolve(classFileName);
                if (Files.exists(file)) return source.relativize(file).toString();
            }
        return null;
    }

    protected Path resolveFile(Path root, String file) {
        return root.resolve(file);
    }

    @Override
    public List<String> readFile(String file) throws IOException {
        Path res;
        for (var source : repositories) {
            res = source.resolve(file);
            if (Files.exists(res)) return Files.readAllLines(res);
        }
        return null;
    }

    public Path resolveClass(String file) {
        Path res;
        for (var repository : repositories) {
            for (var root : roots.get(repository)) {
                res = resolveFile(root, file);
                if (Files.exists(res)) return res;
            }
        }
        return null;
    }

    @Override
    public String toClassFile(String file) {
        for (var sourceHierarchy : repositories) {
            var path = sourceHierarchy.resolve(file);
            if (Files.exists(path)) {
                if (file.endsWith(".java")) {
                    for(var sourceRoot : roots.get(sourceHierarchy)) {
                        if (path.startsWith(sourceRoot)) {
                            var relPath = sourceRoot.relativize(path).toString();
                            return relPath.substring(0, relPath.length() - ".java".length());
                        }
                    }
                } else return file;
            }
        }
        return null;
    }

    //    public static class SourceRepository {
//        private final String name;
//        private final Path root;
//
//        public SourceRepository(String name, Path root) {
//            this.name = name;
//            this.root = root;
//        }
//
//        public Path relativize(Path file) {
//            var realPath = name == null ? file : name.relativize(file);
//            var res = root.relativize(realPath);
//            return name == null ? res : name.resolve(file);
//        }
//        public Path resolve(String file) {
//            return name == null ? root.resolve(file) : name.resolve(root.resolve(file));
//        }
//    }
}
