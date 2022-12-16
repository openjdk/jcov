/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.jcov;

import com.sun.tdk.jcov.util.Utils;

import java.util.logging.Logger;

/**
 * <p> A tool to statically instrument classfiles to collect coverage. </p> <p>
 * There are 2 coverage collection modes: static and dynamic. In static mode
 * JCov reads and modifies classes bytecode inserting there some instructions
 * which will use JCov RT libraries. In dynamic mode (aka Agent mode) a VM agent
 * is used ("java -javaagent") that instruments bytecode just at loadtime. </p>
 *
 * @author Andrey Titov
 * @deprecated Use Instr
 */
@Deprecated(forRemoval = true)
public class Instr2 extends Instr {

    private static final Logger logger;

    static {
        Utils.initLogger();
        logger = Logger.getLogger(Instr2.class.getName());
    }

    /**
     * entry point. Parses options, constructs appropriate context, class name
     * filter, then invoces UniversalInstrumenter to do the job
     */
    public static void main(String[] args) {
        Instr2 tool = new Instr2();
        try {
            int res = tool.run(args);
            System.exit(res);
        } catch (Exception ex) {
            System.exit(1);
        }
    }

    protected String usageString() {
        return "java com.sun.tdk.jcov.Instr2 [-option [value]]";
    }

    protected String exampleString() {
        return "java -cp jcov.jar com.sun.tdk.jcov.Instr2 -include java.lang.* -abstract on -native on -field on -template mytemplate.xml instrumented_classes";
    }

    protected String getDescr() {
        return "instrumenter designed for abstract, native methods and fields";
    }
}
