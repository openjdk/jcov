/*
 * Copyright (c) 2023 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.jcov.instrument.plugin;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiConsumer;

public class PathDestination implements Destination, Closeable {
    private final Path root;
    private final FileSystem fs;
    private final BiConsumer<String, byte[]> saver;

    public PathDestination(Path root) throws IOException {
        fs = Files.isDirectory(root) ? null : FileSystems.newFileSystem(root, (ClassLoader) null);
        this.root = Files.isDirectory(root) ? root : fs.getPath("/");
        saver = (s, bytes) -> {
            try {
                Path f = PathDestination.this.root.resolve(s);
                Files.createDirectories(f.getParent());
                Files.write(f, bytes);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    @Override
    public void close() throws IOException {
        if (fs != null) fs.close();
    }

    @Override
    public BiConsumer<String, byte[]> saver() {
        return saver;
    }
}
