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
package openjdk.jcov.data.fields.runtime;

import openjdk.jcov.data.runtime.Serializer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class Coverage {
    public static final String DATA_PREFIX = " -> ";
    private final Map<String, Map<String, List>> data;
    volatile boolean writing;

    public Coverage() {
        data = new HashMap<>();
    }

    public Map<String, Map<String, List>> coverage() {
        return data;
    }

    /**
     * Obtains a structure for the data, adding an empty one, if necessary.
     */
    public List<? extends Object> get(String owner, String field) {
        Map<String, List> fields = data.get(owner);
        if(fields == null) {
            fields = new HashMap<>();
            data.put(owner, fields);
        }
        List<? extends Object> fieldCov = fields.get(field);
        if(fieldCov == null) {
            fieldCov = new ArrayList<>();
            fields.put(field, fieldCov);
        }
        return fieldCov;
    }

    public boolean contains(String owner) {
        return data.containsKey(owner);
    }

    public boolean contains(String owner, String field) {
        return contains(owner) && data.get(owner).containsKey(field);
    }

    public void add(String owner, String field, Object value) {
        List fieldCov = get(owner, field);
        if(fieldCov.stream().noneMatch(val -> Objects.equals(val, value)))
            fieldCov.add(value);
    }

    public static Coverage read(Path file) throws IOException {
        return read(file, s -> s);
    }

    public static Coverage read(Path path, Function<String, ? extends Object> deserializer) throws IOException {
        Coverage result = new Coverage();
        List lastData = null;
        for (String l : Files.readAllLines(path)) {
            if (!l.startsWith(DATA_PREFIX)) {
                int classEnd = l.indexOf('#');
                String owner = l.substring(0, classEnd);
                String name = l.substring(classEnd + 1);
                lastData = result.get(owner, name);
            } else {
                Object value = deserializer.apply(l.substring(DATA_PREFIX.length()));
                lastData.add(value);
            }
        }
        return result;
    }
    /**
     * Saves the data into a file in a custom plain text format.
     */
    public static void write(Coverage coverage, Path path) throws IOException {
        write(coverage, path, Serializer.TO_STRING);
    }
    public static void write(Coverage coverage, Path path, Function<Object, String> serializer)
            throws IOException {
        write(coverage, Files.newOutputStream(path), serializer);
    }
    public static void write(Coverage coverage, OutputStream outStream) throws IOException {
        write(coverage, outStream, Serializer.TO_STRING);
    }
    public static void write(Coverage coverage, OutputStream outStream, Function<Object, String> serializer)
            throws IOException {
        coverage.writing = true;
        try(BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outStream))) {
            coverage.data.entrySet().forEach(ce -> {
                ce.getValue().entrySet().forEach(me -> {
                    try {
                        out.write(ce.getKey() + "#" + me.getKey());
                        out.newLine();
                        me.getValue().forEach(d -> {
                            try {
                                out.write(DATA_PREFIX + serializer.apply(d));
                                out.newLine();
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
                        out.flush();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        } finally {
            coverage.writing = false;
        }
    }
}
