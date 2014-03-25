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

import com.sun.tdk.jcov.tools.JCovCMDTool;
import com.sun.tdk.jcov.tools.JCovTool;
import com.sun.tdk.jcov.tools.JcovVersion;

/**
 * @author Dmitry Fazunenko
 * @author Alexey Fedorchenko
 */
public class Helper {

    public static void main(String[] args) {
        if (args.length > 0) {
            String arg = args[0];
            if ("-version".equals(arg)) {
                System.out.println(String.format("JCov %s-%s", JcovVersion.jcovVersion, JcovVersion.jcovBuildNumber, JcovVersion.jcovMilestone) + ("beta".equals(JcovVersion.jcovMilestone) ? " beta" : ""));
                System.exit(0);
            } else {
                for (int i = 0; i < JCovTool.allToolsList.size(); i++) {
                    // 17 = "com.sun.tdk.jcov."
                    if (JCovTool.allToolsList.get(i).toLowerCase().substring(17).equals(arg.toLowerCase())) {
                        try {
                            Class c = Class.forName(JCovTool.allToolsList.get(i));
                            if (JCovCMDTool.class.isAssignableFrom(c)) {
                                JCovCMDTool tool = (JCovCMDTool) c.newInstance();
//                                String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
                                String[] newArgs = new String[args.length - 1];
                                System.arraycopy(args, 1, newArgs, 0, args.length - 1); // jdk1.5 support
                                System.exit(tool.run(newArgs));
                            } else if (JCovTool.class.isAssignableFrom(c)) {
                                JCovTool.printHelp((JCovTool) c.newInstance(), args);
                                System.exit(1);
                            } else {
                                System.out.println("INTERNAL ERROR! Specified tool ('" + arg + "') is not a jcovtool. ");
                                System.exit(1);
                            }
                        } catch (ClassNotFoundException e) {
                            System.out.println("INTERNAL ERROR! Specified tool was not found in classpath. ");
                            System.exit(1);
                        } catch (Exception e) {
                            System.out.println("INTERNAL ERROR! " + e.getMessage());
                            e.printStackTrace();
                            System.exit(1);
                        }
                    }
                }
            }
        }

        JCovTool.printHelp();
    }
}
