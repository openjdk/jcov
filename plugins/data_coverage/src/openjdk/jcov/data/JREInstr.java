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

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class JREInstr {
    private String pluginClass;
    private String jcovTemplate;
    private String jcovRuntime;

    public JREInstr() {
    }

    public JREInstr pluginClass(String pluginClass) {
        this.pluginClass = pluginClass;
        return this;
    }

    public JREInstr jcovTemplate(String jcovTemplate) {
        this.jcovTemplate = jcovTemplate;
        return this;
    }

    public JREInstr jcovRuntime(String jcovRuntime) {
        this.jcovRuntime = jcovRuntime;
        return this;
    }

    public int instrument(String jre) throws IOException, InterruptedException {
        String[] params = new String[] {
//                System.getProperty("java.home") + separator + "bin" + separator + "java",
//                "-cp", System.getProperty("java.class.path"),
//                "-jar", "/Users/shura/JDK/git/jcov-mine/JCOV_BUILD/jcov_3.0/jcov.jar",
//                "JREInstr",
                "-implantrt", jcovRuntime,
                "-instr_plugin", pluginClass,
                "-template", jcovTemplate,
                "-im", "java.base",
                jre};
        System.out.println("Instrumentation parameters: " +
                Arrays.stream(params).collect(Collectors.joining(" ")));
//        return new ProcessBuilder(params).redirectOutput(ProcessBuilder.Redirect.INHERIT)
//                .redirectError(ProcessBuilder.Redirect.INHERIT).start().waitFor();
        return new com.sun.tdk.jcov.JREInstr().run(params);
    }
}
