/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package openjdk.jcov.data.arguments.runtime;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class Coverage {

    public static final String DATA_PREFIX = " -> ";
    private final Map<String, Map<String, List<List<? extends Object>>>> data;

    public static Coverage readTemplate(Path path) throws IOException {
        return readImpl(path, s -> null);
    }

    private static List<? extends Object> parse(String s, Function<String, ? extends Object> deserialize) {
        return Arrays.stream(s.split(","))
                .map(v -> v.isEmpty() ? null : deserialize.apply(v))
                .collect(toList());
    }

    //TODO move to an SPI class
    public static Coverage read(Path path, Function<String, ? extends Object> deserializer) throws IOException {
        return readImpl(path, deserializer);
    }
    private static Coverage readImpl(Path path, Function<String, ? extends Object> deserializer) throws IOException {
        Coverage result = new Coverage();
        List<List<? extends Object>> lastData = null;
        for (String l : Files.readAllLines(path)) {
            if (!l.startsWith(DATA_PREFIX)) {
                int descStart = l.indexOf('(');
                int classEnd = l.lastIndexOf('#', descStart);
                String owner = l.substring(0, classEnd);
                String name = l.substring(classEnd + 1, descStart);
                String desc = l.substring(descStart);
                lastData = result.get(owner, name + desc);
            } else {
                lastData.add(parse(l.substring(DATA_PREFIX.length()), deserializer));
            }
        }
        return result;
    }

    //TODO move to an SPI class
    public static final void write(Coverage coverage, Path path, Function<Object, String> serializer)
            throws IOException {
        try(BufferedWriter out = Files.newBufferedWriter(path)) {
            coverage.data.entrySet().forEach(ce -> {
                    ce.getValue().entrySet().forEach(me -> {
                        try {
                            out.write(ce.getKey() + "#" + me.getKey());
                            out.newLine();
                            me.getValue().forEach(dl -> {
                                try {
                                    out.write(DATA_PREFIX +
                                            dl.stream().map(d -> serializer.apply(d))
                                            .collect(Collectors.joining(",")));
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
        }
    }

    public Coverage() {
        data = new HashMap<>();
    }

    public List<List<? extends Object>> get(String owner, String method) {
        Map<String, List<List<? extends Object>>> methods = data.get(owner);
        if(methods == null) {
            methods = new HashMap<>();
            data.put(owner, methods);
        }
        List<List<? extends Object>> result = methods.get(method);
        if(result == null) {
            result = new ArrayList<>();
            methods.put(method, result);
        }
        return result;
    }

    public Map<String, Map<String, List<List<? extends Object>>>> coverage() {
        return data;
    }
}
