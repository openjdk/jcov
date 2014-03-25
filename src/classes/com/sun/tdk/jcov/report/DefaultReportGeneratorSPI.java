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
package com.sun.tdk.jcov.report;

import com.sun.tdk.jcov.ant.AntableSPI;
import com.sun.tdk.jcov.report.text.TextReportGenerator;
import com.sun.tdk.jcov.report.html.CoverageReport;
import com.sun.tdk.jcov.tools.EnvHandler;
import com.sun.tdk.jcov.tools.EnvServiceProvider;
import com.sun.tdk.jcov.tools.JCovTool.EnvHandlingException;
import com.sun.tdk.jcov.tools.OptionDescr;

/**
 *
 * @author Andrey Titov
 */
public class DefaultReportGeneratorSPI implements ReportGeneratorSPI, AntableSPI, EnvServiceProvider {

    protected CoverageReport html = null;
    protected TextReportGenerator txt = null;
    protected boolean showMethods = true;
    protected boolean showBlocks = true;
    protected boolean showBranches = true;
    protected boolean showLines = true;
    protected boolean generateShortFormat;
    protected boolean showFields;

    public ReportGenerator getReportGenerator(String name) {
        if ("txt".equalsIgnoreCase(name) || "text".equalsIgnoreCase(name)) {
            if (txt == null) {
                txt = new TextReportGenerator();
            }
            txt.setGenerateShortFormat(generateShortFormat);
            txt.setShowMethods(showMethods);
            txt.setShowBlocks(showBlocks);
            txt.setShowBranches(showBranches);
            txt.setShowFields(showFields);
            txt.setShowLines(showLines);
            return txt;
        } else if ("html".equalsIgnoreCase(name)) {
            if (html == null) {
                html = new CoverageReport();
            }
            html.setShowMethods(showMethods);
            html.setShowBlocks(showBlocks);
            html.setShowBranches(showBranches);
            html.setShowLines(showLines);
            return html;
        }

        return null;
    }

    public void extendEnvHandler(final EnvHandler handler) {
        handler.addOptions(new OptionDescr[]{
                    DSC_NO_BLOCK,
                    DSC_NO_BRANCH,
                    DSC_NO_LINE,
                    DSC_NO_METHOD,
                    DSC_NO_FIELD,
                    DSC_SHORT,});
    }

    public int handleEnv(EnvHandler opts) throws EnvHandlingException {
        showMethods = !opts.isSet(DSC_NO_METHOD);
        showFields = !opts.isSet(DSC_NO_FIELD);
        showBlocks = !opts.isSet(DSC_NO_BLOCK);
        showBranches = !opts.isSet(DSC_NO_BRANCH);
        showLines = !opts.isSet(DSC_NO_LINE);
        generateShortFormat = opts.isSet(DSC_SHORT);

        return 0;
    }

    /**
     * <p> Allows to use this SPI through ANT wrappers </p> <p>
     * DefaultReportGeneratorSPI allows following attributes: <ul>
     * <li>hideMethods</li> <li>hideFields</li> <li>hideLines</li>
     * <li>hideBlocks</li> <li>hideBranches</li> <li>short</li> </ul> </p>
     *
     * @param attrName ANT attribute name.
     * @param attrValue Value of attribute to set.
     * @return False if this attribute name is not supported. If SPI returns
     * FALSE here - JCov will try to resolve setter method (setXXX where XXX is
     * attrName). If neither setter method exists neither handleAttribute()
     * accepts the attrName - BuildException would thrown.
     */
    public boolean handleAttribute(String attrName, String attrValue) {
        if ("hideMethods".equalsIgnoreCase(attrName)) {
            showMethods = !Boolean.parseBoolean(attrValue);
            return true;
        } else if ("hideBlocks".equalsIgnoreCase(attrName)) {
            showBlocks = !Boolean.parseBoolean(attrValue);
            return true;
        } else if ("hideBranches".equalsIgnoreCase(attrName)) {
            showBranches = !Boolean.parseBoolean(attrValue);
            return true;
        } else if ("hideLines".equalsIgnoreCase(attrName)) {
            showLines = !Boolean.parseBoolean(attrValue);
            return true;
        } else if ("short".equalsIgnoreCase(attrName)) {
            generateShortFormat = Boolean.parseBoolean(attrValue);
            return true;
        } else if ("hideFields".equalsIgnoreCase(attrName)) {
            showFields = !Boolean.parseBoolean(attrValue);
            return true;
        }
        return false;
    }
    protected final static OptionDescr DSC_SHORT =
            new OptionDescr("short", "Additional filtering", "Generate short summary text report.");
    protected final static OptionDescr DSC_NO_METHOD =
            new OptionDescr("nomethod", "Additional filtering", "Hide method coverage from report");
    protected final static OptionDescr DSC_NO_FIELD =
            new OptionDescr("nofields", "Additional filtering", "Hide field coverage from report");
    protected final static OptionDescr DSC_NO_BRANCH =
            new OptionDescr("nobranch", "Additional filtering", "Hide branch coverage from report");
    protected final static OptionDescr DSC_NO_BLOCK =
            new OptionDescr("noblock", "Additional filtering", "Hide block coverage from report");
    protected final static OptionDescr DSC_NO_LINE =
            new OptionDescr("noline", "Additional filtering", "Hide line coverage from report");
}