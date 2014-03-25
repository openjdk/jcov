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
package com.sun.tdk.jcov.runtime;

import com.sun.tdk.jcov.util.MapHelper;

/**
 * @author Dmitry Fazunenko
 * @author Alexey Fedorchenko
 */
public class TemplateFileSaver implements JCovSaver {

    /**
     * Default value for output file
     */
    private static final String def_filename_xml = "result.xml";
    /**
     * Default value for template file
     */
    private static final String def_template = "template.xml";
    private String filename = def_filename_xml;
    private String template = def_template;

    public TemplateFileSaver() {
    }

    public TemplateFileSaver(String filename, String template) {
        this.filename = filename;
        this.template = template;
    }

    public void saveResults() {
        template = PropertyFinder.findValue("template", template);
        filename = PropertyFinder.findValue("file", filename);

        try {
            MapHelper.mapCounts(filename, template, Collect.counts());
        } catch (Exception e) {
            System.err.println(
                    "Exception occurred while saving result into " + filename + " file.\n"
                    + "Template file: " + template + "\n"
                    + "Exception details: " + e.getMessage());
            if (PropertyFinder.findValue("verbose", null) != null) {
                System.err.println("\nStack trace: ");
                e.printStackTrace(System.err);
            }
        }
    }
}
