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
package openjdk.jcov.data.runtime;

import openjdk.jcov.data.arguments.runtime.Collect;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static openjdk.jcov.data.Env.JCOV_DATA_ENV_PREFIX;

public interface CoverageData {
    /**
     * Specifies where to save collected data or instrumentation information.
     */
    String COVERAGE_OUT = JCOV_DATA_ENV_PREFIX + Collect.ARGUMENTS_PREFIX +
            "coverage";
    /**
     * Specifies where to load previously collected data from. A non-empty value of this property will
     * make Collect class to load the data on class loading.
     */
    String COVERAGE_IN = JCOV_DATA_ENV_PREFIX + Collect.ARGUMENTS_PREFIX +
            "coverage.in";

    public static List<String> split(String s) {
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

    public static List<? extends Object> parse(String s, Function<String, ? extends Object> deserialize) {
                return split(s).stream()
                        .map(v -> v.isEmpty() ? "" : deserialize.apply(v))
                        .collect(toList());
            }

    void setResultFile(Path file);

    void saveResults();

    void clearData();
}
