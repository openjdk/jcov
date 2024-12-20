/*
 * Copyright (c) 2024 Oracle and/or its affiliates. All rights reserved.
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
package openjdk.codetools.jcov.report.commandline;

import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class CommandLineTest {
    CommandLine commandLine;
    List<Option> options;
    List<Parameter> parameters;
    @Test
    void before() {
        options = List.of(
                new Option.Builder().option("-a").name("A").description("The A").optional(false).create(),
                new Option.Builder().option("-b").name("B").description("The B").optional(true).create()
        );
        parameters = List.of(
                new Parameter("first", "The first"),
                new Parameter("second", "The second")
        );
        commandLine = new CommandLine(options, parameters);
    }

    @Test
    void usage() {
        assertEquals(commandLine.usageLine(), "-a A [-b B] first second");
        assertEquals(commandLine.usageList("  "), "  A: The A\n  B: The B\n  first: The first\n  second: The second");
    }

    @Test
    void parseFull() {
        var parsed = commandLine.parse(new String[] {
                options.get(0).option(), "a",
                options.get(1).option(), "b",
                "c", "d"});
        assertEquals(parsed.get(options.get(0)), "a");
        assertEquals(parsed.get(options.get(1)), "b");
        assertEquals(parsed.parameters().size(), parameters.size());
        assertEquals(parsed.parameters().get(0), "c");
        assertEquals(parsed.parameters().get(1), "d");
    }

    @Test
    void missingOptOpt() {
        var parsed = commandLine.parse(new String[] {
                options.get(0).option(), "a",
                "c", "d"});
        assertEquals(parsed.get(options.get(0)), "a");
        assertEquals(parsed.get(options.get(1)), null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    void missingMandatoryOpt() {
        commandLine.parse(new String[] {
                options.get(1).option(), "b",
                "c", "d"});
    }
    @Test(expectedExceptions = IllegalArgumentException.class)
    void missingParams() {
        commandLine.parse(new String[] {
                options.get(0).option(), "a",
                "c"});
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    void noParams() {
        commandLine.parse(new String[] {options.get(0).option(), "a"});
    }

}
