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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * Data container for the values collected in runtime. Same class is used to store template as a file with no
 * values and just method descriptions.
 */
public class Coverage {

    public static final String DATA_PREFIX = " -> ";
    private final Map<String, Map<String, List<List<? extends Object>>>> data;

    public static Coverage read(Path path) throws IOException {
        return readImpl(path, s -> s);
    }

    private static List<String> split(String s) {
        List<String> result = new ArrayList<>();
        int lci = -1;
        for (int ci = 0; ci < s.length(); ci++) {
            if(s.charAt(ci) == ',') {
                result.add(s.substring(lci + 1, ci));
                lci = ci;
            }
        }
        result.add(s.substring(lci + 1));
        return result;
    }

    private static List<? extends Object> parse(String s, Function<String, ? extends Object> deserialize) {
        return split(s).stream()
                .map(v -> v.isEmpty() ? "" : deserialize.apply(v))
                .collect(toList());
    }

    //TODO move to an SPI class
    public static Coverage read(Path path, Function<String, ? extends Object> deserializer) throws IOException {
        return readImpl(path, deserializer);
    }

    /**
     * Loads the data from a file in a custom plain text format.
     */
    private static Coverage readImpl(Path path, Function<String, ? extends Object> deserializer) throws IOException {
        Coverage result = new Coverage();
        List<List<? extends Object>> lastData = null;
        String desc = null, name = null, owner = null;
        for (String l : Files.readAllLines(path)) {
            if (!l.startsWith(DATA_PREFIX)) {
                int descStart = l.indexOf('(');
                int classEnd = l.lastIndexOf('#', descStart);
                owner = l.substring(0, classEnd);
                name = l.substring(classEnd + 1, descStart);
                desc = l.substring(descStart);
                lastData = result.get(owner, name + desc);
            } else {
                List<? extends Object> values = parse(l.substring(DATA_PREFIX.length()), deserializer);
                //TODO this needs to be fixed for arrays
//                if(Collect.countParams(desc) != values.size()) {
//                    throw new IllegalStateException("Incorrect number of parameters for " +
//                            owner + "#" + name + desc + ": " + values.size());
//                }
                lastData.add(values);
            }
        }
        return result;
    }

    //TODO move to an SPI class

    /**
     * Saves the data into a file in a custom plain text format.
     */
    public static final void write(Coverage coverage, Path path/*, Function<Object, String> serializer*/)
            throws IOException {
        try(BufferedWriter out = Files.newBufferedWriter(path)) {
            coverage.data.entrySet().forEach(ce -> {
                    ce.getValue().entrySet().forEach(me -> {
                        try {
                            out.write(ce.getKey() + "#" + me.getKey());
                            out.newLine();
                            me.getValue().forEach(dl -> {
                                try {
                                    String desc = me.getKey().substring(me.getKey().indexOf("("));
                                    List<String> values = dl.stream().map(o -> (String)o)
                                            .collect(toList());
                                    if(Collect.countParams(desc) != values.size()) {
                                        System.err.println("Incorect number of params for " + me.getKey());
                                        System.out.println(values.stream().map(Objects::toString)
                                                .collect(Collectors.joining(",")));
                                        throw new IllegalStateException("Incorrect number of parameters for " +
                                                me.getKey() + ": " + values.size());
                                    }
                                    out.write(DATA_PREFIX +
                                            values.stream()//.map(d -> serializer.apply(d))
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

    private final boolean selfCompacting = true;

    public Coverage() {
        data = new HashMap<>();
    }

    /**
     * Obtains a structure for the data, adding an empty one, if necessary.
     */
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

    public void add(String owner, String method, List<? extends Object> params) {
        List<List<? extends Object>> methodCov = get(owner, method);
        if(methodCov.stream().noneMatch(call -> {
            if(call.size() != params.size()) return false;
            for (int i = 0; i < call.size(); i++)
                if(!Objects.equals(call.get(i), params.get(i))) return false;
            return true;
        })) methodCov.add(params);
    }

    public Map<String, Map<String, List<List<? extends Object>>>> coverage() {
        return data;
    }
}
