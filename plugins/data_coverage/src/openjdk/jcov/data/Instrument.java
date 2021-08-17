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
package openjdk.jcov.data;

import com.sun.tdk.jcov.Instr;
import openjdk.jcov.data.arguments.instrument.Plugin;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Some API which makes it easier to run JCov instrumentation while setting specific options to use an instrumentation
 * plugin. Could be used in tests or other java code.<br/>
 * Also provides a main method so it is easier to run the instrumentation from the command line.<br/>
 * Current implementaion runs JCov code in the same VM.
 */
public class Instrument {
    /**
     * Prefix for all system property names which will be passed to the VM running JCov calls.
     */
    public static final String JCOV_DATA_ENV_PREFIX = "jcov.data.";
    /**
     * Name of a system property which contains class name of an instrumentation plugin.
     */
    public static final String PLUGIN_CLASS = JCOV_DATA_ENV_PREFIX + "plugin";
    public static final String JCOV_TEMPLATE = JCOV_DATA_ENV_PREFIX + "jcov.template";

    private String pluginClass;
    private Path jcovTemplate;

    public Instrument() {
        pluginClass = Env.getStringEnv(PLUGIN_CLASS, Plugin.class.getName());
        jcovTemplate = Env.getPathEnv(JCOV_TEMPLATE, Paths.get("template.xml"));
    }

    public Instrument pluginClass(String pluginClass) {
        this.pluginClass = pluginClass;
        return this;
    }

    public Instrument jcovTemplate(Path jcovTemplate) {
        this.jcovTemplate = jcovTemplate;
        return this;
    }

    public boolean instrument(List<Path> classes) throws IOException, InterruptedException {
        List<String> params = new ArrayList<>();
        params.add("-instr_plugin");
        params.add(pluginClass);
        params.add("-t");
        params.add(jcovTemplate.toString());
        params.addAll(classes.stream().map(Path::toString).collect(toList()));
        return new Instr().run(params.toArray(new String[0])) == 0;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        new Instrument().instrument(Arrays.stream(args).map(Paths::get).collect(toList()));
    }
}
