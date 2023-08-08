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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JDKHierarchy extends SourceHierarchy {
    public JDKHierarchy(List<Path> repos) {
        super(rootPaths(repos));
    }

    public static Set<String> roots(List<Path> repos) {
        //TODO add platform specific
        //one platform?
        return repos.stream().flatMap(repo -> {
            try {
                return Files.list(repo.resolve("src")).filter(module -> {
                    return Files.exists(module.resolve("share/classes"));
                        })
                        .map(module -> "src/" + module.getFileName().toString() + "/share/classes");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }).collect(Collectors.toSet());
    }

    private static List<Path> rootPaths(List<Path> repos) {
        //TODO add platform specific
        //one platform?
        return repos.stream().flatMap(repo -> {
                    try {
                        return Files.list(repo.resolve("src")).map(module -> module.resolve("share/classes")).filter(Files::exists);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }).collect(Collectors.toList());
    }

    @Override
    protected Path resolve(Path root, String file) {
        return root.resolve(file);
    }
}
