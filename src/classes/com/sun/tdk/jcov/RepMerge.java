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

import com.sun.tdk.jcov.tools.EnvHandler;
import com.sun.tdk.jcov.tools.OptionDescr;

/**
 *
 * @author Andrey Titov
 */
public class RepMerge extends Merger {

    @Override
    protected int run() throws Exception {
        System.out.println("Warning, this tool is depricated and would be removed soon");
        return super.run();
    }

    @Override
    protected EnvHandler defineHandler() {
        return new EnvHandler(new OptionDescr[]{DSC_OUTPUT, DSC_NONEW}, this);
    }

    @Override
    protected int handleEnv(EnvHandler opts) throws EnvHandlingException {
        int res = super.handleEnv(opts);

        if (opts.isSet(DSC_NONEW)) {
            setAddMissing(false);
        }
        setSigmerge(true);

        return res;
    }

    @Override
    protected String usageString() {
        return "java com.sun.tdk.jcov.RepMerge [-output <filename>] [-nonew] <filenames>";
    }

    @Override
    protected String exampleString() {
        return "java -cp jcov.jar com.sun.tdk.jcov.RepMerge -output merged.xml test1.xml test2.xml";
    }

    @Override
    protected String getDescr() {
        return "merges jcov data files at method level not caring of blocks <deprecated>";
    }

    public static void main(String[] args) {
        try {
            System.exit(new RepMerge().run(args));
        } catch (Exception e) {
            System.exit(ERROR_EXEC_EXIT_CODE);
        }
    }
    static OptionDescr DSC_NONEW = new OptionDescr("nonew", "", OptionDescr.VAL_NONE, "Don't include any information that doesn't exist in the first merged file.");
}
