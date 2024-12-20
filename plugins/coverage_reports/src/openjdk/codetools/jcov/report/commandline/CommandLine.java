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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static java.util.stream.Collectors.joining;

public class CommandLine {
    private final List<Option> options;
    private final List<Parameter> parameters;
    private final boolean checkValidity;

    public CommandLine(List<Option> options, List<Parameter> parameters, boolean validity) {
        this.options = options;
        this.parameters = parameters;
        checkValidity = validity;
    }

    public CommandLine(List<Option> options, List<Parameter> parameters) {
        this(options, parameters, true);
    }

    public Parsed parse(String[] argv) {
        int i = 0;
        var opts = new HashMap<Option, String>();
        while (i < argv.length) {
            var next = argv[i];
            var option = options.stream().filter(o -> o.option().equals(next)).findAny();
            if (option.isPresent()) {
                opts.put(option.get(), argv[i + 1]);
                i+=2;
            } else {
                //not an option - parameters start
                break;
            }
        }
        if (checkValidity) {
            for( Option o : options)
                if (!o.optional() && !opts.containsKey(o))
                    throw new IllegalArgumentException(
                            String.format("Option %s is missing in %s",
                                    o.option(), Arrays.stream(argv).collect(joining(" "))));
        }
        var params = Arrays.asList(Arrays.copyOfRange(argv, i, argv.length));
        if (checkValidity && params.size() != parameters.size())
            throw new IllegalArgumentException(
                    String.format("Insufficient number of parameters. Expected %d, found %d:\n %s",
                            parameters.size(), params.size(), params.stream().collect(joining(" "))));
        return new Parsed() {
            @Override
            public String get(Option option) {
                return opts.get(option);
            }

            @Override
            public List<String> parameters() {
                return params;
            }
        };
    }

    public String usageLine() {
        return options.stream().map(o -> {
            String res = o.option() + " " + o.name();
            if (!o.optional())
                return res;
            else
                return "[" + res + "]";
        }).collect(joining(" ")) + " " +
                parameters.stream().map(Parameter::name).collect(joining(" "));

    }

    public String usageList(String indent) {
        return options.stream().map(o -> indent + o.name() + ": " + o.description()).collect(joining("\n")) +
                "\n" +
                parameters.stream().map(p -> indent + p.name() + ": " + p.description()).collect(joining("\n"));
    }

    public interface Parsed {
        String get(Option option);
        List<String> parameters();
        default String getOrElse(Option title, String def) {
            String res = get(title);
            if (res != null)
                return res;
            else
                return def;
        }
    }

    public static class Builder {

        private List<Option> options = new ArrayList<>();
        private List<Parameter> parameters = new ArrayList<>();
        private boolean validity = true;

        public Builder option(Option option) {
            options.add(option);
            return this;
        }

        public Builder parameter(Parameter parameter) {
            parameters.add(parameter);
            return this;
        }

        public Builder checkValidity(boolean validity) {
            this.validity = validity;
            return this;
        }

        public CommandLine create() {
            return new CommandLine(options, parameters, validity);
        }
    }
}
