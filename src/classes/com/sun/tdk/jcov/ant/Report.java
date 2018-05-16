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
package com.sun.tdk.jcov.ant;

import com.sun.tdk.jcov.RepGen;
import com.sun.tdk.jcov.data.Result;
import com.sun.tdk.jcov.processing.ProcessingException;
import com.sun.tdk.jcov.report.DefaultReportGeneratorSPI;
import com.sun.tdk.jcov.report.ReportGeneratorSPI;
import com.sun.tdk.jcov.report.javap.JavapRepGen;
import com.sun.tdk.jcov.runtime.PropertyFinder;
import com.sun.tdk.jcov.util.Utils;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DynamicAttribute;
import org.apache.tools.ant.IntrospectionHelper;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.UnsupportedAttributeException;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.types.PatternSet;

/**
 *
 * @author Andrey Titov
 * @author Alexey Fedorchenko
 */
public class Report extends Task implements DynamicAttribute {

    private PatternSet filter;
    public File srcRootPath;
    public boolean noAbstract = false; // not used
    public boolean isPublicAPI = false;
    public boolean noline = false;
    public boolean noblock = false;
    public boolean nobranch = false;
    public boolean nomethod = false;
    public boolean nofields = false;
    public String customFilter;
    public String customReportSPI;
    public ReportGeneratorSPI customReportSPIInstance;
    public String format = "html";
    public File destdir = new File("report");
    public File propfile = null;
    public File filename;
    public File testlist;

    public String getJavap() {
        return javap;
    }

    public void setJavap(String javap) {
        this.javap = javap;
    }

    public String javap;
    public boolean rewrite;
    public File fmlist;
    public LinkedList<FM> fm = new LinkedList<FM>();
    private boolean customAttributes;

    @Override
    public void setDynamicAttribute(String attrName, String attrVal) throws BuildException {
        boolean ok = false;
        if (customReportSPIInstance != null && customReportSPIInstance instanceof AntableSPI) {
            ok = ((AntableSPI) customReportSPIInstance).handleAttribute(attrName, attrVal);
        }
        if (!ok) {
            try {
                IntrospectionHelper.getHelper(customReportSPIInstance.getClass()).setAttribute(getProject(), customReportSPIInstance, attrName, attrVal);
            } catch (Exception ex) {
                throw new UnsupportedAttributeException("report has no such attribute '" + attrName + "' neither in Report ServiceProvider '" + customReportSPIInstance.getClass() + "'", attrName);
            }
        } else {
            customAttributes = true;
        }
    }

    @Override
    public void maybeConfigure() throws BuildException {
        RuntimeConfigurable rt = getRuntimeConfigurableWrapper();
        boolean found = false;
        for (Object o : rt.getAttributeMap().entrySet()) {
            Entry e = (Entry) o;
            if ("ReportSPI".equalsIgnoreCase(e.getKey().toString())) {
                try {
                    customReportSPI = e.getValue().toString();
                    customReportSPIInstance = (ReportGeneratorSPI) Class.forName(customReportSPI).newInstance();
                    found = true;
                    break;

                } catch (ClassNotFoundException ex) {
                    throw new BuildException("Can't create report generator " + customReportSPI, ex);
                } catch (InstantiationException ex) {
                    throw new BuildException("Can't create report generator " + customReportSPI, ex);
                } catch (IllegalAccessException ex) {
                    throw new BuildException("Can't create report generator " + customReportSPI, ex);
                } catch (ClassCastException ex) {
                    throw new BuildException("Can't create report generator " + customReportSPI + " - not a ReportGeneratorSPI", ex);
                }
            }
        }
        if (!found) {
            customReportSPIInstance = new DefaultReportGeneratorSPI();
        }
        super.maybeConfigure();
    }

    @Override
    public void execute() throws BuildException {
        RepGen repGen = new RepGen();

        if (propfile != null) {
            PropertyFinder.setPropertiesFile(propfile.getPath());
        }

        if (customReportSPIInstance.getClass() == DefaultReportGeneratorSPI.class && !customAttributes) {

            nomethod = Boolean.valueOf(PropertyFinder.findValue("nomethod", String.valueOf(nomethod)));
            noblock = Boolean.valueOf(PropertyFinder.findValue("noblock", String.valueOf(noblock)));
            nobranch = Boolean.valueOf(PropertyFinder.findValue("nobranch", String.valueOf(nobranch)));
            noline = Boolean.valueOf(PropertyFinder.findValue("noline", String.valueOf(noline)));
            nofields = Boolean.valueOf(PropertyFinder.findValue("nofields", String.valueOf(nofields)));

            ((DefaultReportGeneratorSPI) customReportSPIInstance).handleAttribute("hideMethods", String.valueOf(nomethod));
            ((DefaultReportGeneratorSPI) customReportSPIInstance).handleAttribute("hideBlocks", String.valueOf(noblock));
            ((DefaultReportGeneratorSPI) customReportSPIInstance).handleAttribute("hideBranches", String.valueOf(nobranch));
            ((DefaultReportGeneratorSPI) customReportSPIInstance).handleAttribute("hideLines", String.valueOf(noline));
            ((DefaultReportGeneratorSPI) customReportSPIInstance).handleAttribute("hideFields", String.valueOf(nofields));

        }

        initRepGen(repGen);
        try {
            if (rewrite && destdir != null) {
                if (destdir.exists()) {
                    Delete d = new Delete();
                    d.setProject(getProject());
                    d.setDir(destdir);
                    d.setTaskName("rewrite");
                    d.perform();
                }
            }

            if (filename != null) {

                if (javap != null) {
                    repGen.setDataProcessorsSPIs(null);
                    new JavapRepGen(repGen).run(filename.getPath(), javap, destdir.getPath());
                    if (propfile != null) {
                        PropertyFinder.cleanProperties();
                    }
                    return;
                }

                repGen.generateReport(format, destdir.getPath(), new Result(filename.getPath(), testlist != null ? testlist.getPath() : null), srcRootPath != null ? srcRootPath.getPath() : repGen.getSrcRootPath());
            } else {
                throw new BuildException("Input jcov file needed to generate report");
            }
        } catch (ProcessingException ex) {
            Logger.getLogger(Report.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.getLogger(Report.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (propfile != null) {
            PropertyFinder.cleanProperties();
        }
    }

    public void initRepGen(RepGen repGen) {
        try {
            repGen.resetDefaults();
        } catch (Exception ex) {
            Logger.getLogger(Report.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (customFilter != null && !customFilter.equals(repGen.getFilter())) {
            repGen.setFilter(customFilter);
        }
        String fms[] = repGen.getFms();
        if (fm.size() > 0) {
            fms = new String[fm.size()];
            int i = 0;
            for (FM f : fm) {
                fms[i++] = f.value;
            }
            repGen.setFms(fms);
        }

        if (filter != null) {
            String[] includes = repGen.getInclude();
            String[] excludes = repGen.getExclude();
            String[] incl = filter.getIncludePatterns(getProject());
            if (incl != null) {
                includes = incl;
            }
            String[] excl = filter.getExcludePatterns(getProject());
            if (excl != null) {
                excludes = excl;
            }
            repGen.setFilters(includes, excludes, fms);
        }

        if (repGen.isIsPublicAPI() != isPublicAPI) {
            repGen.setIsPublicAPI(isPublicAPI);
        }
        if (repGen.isNoAbstract() != noAbstract) {
            repGen.setNoAbstract(noAbstract);
        }
        repGen.setReportGeneratorSPIs(new ReportGeneratorSPI[]{customReportSPIInstance});
    }

    public void setOutput(File path) {
        this.destdir = path;
    }

    public void setPropfile(File path) {
        this.propfile = path;
    }

    public void setNoline(boolean noline) {
        this.noline = noline;
    }

    public void setNoblock(boolean noblock) {
        this.noblock = noblock;
    }

    public void setNobranch(boolean nobranch) {
        this.nobranch = nobranch;
    }

    public void setNomethod(boolean nomethod) {
        this.nomethod = nomethod;
    }

    public void setNofields(boolean nofields) {
        this.nofields = nofields;
    }

    public void setJcovFile(File filename) {
        this.filename = filename;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setSource(File source) {
        this.srcRootPath = source;
    }

    public void setHideAbstract(Boolean value) {
        this.noAbstract = value;
    }

    public void setPublicApi(Boolean isPublicApi) {
        this.isPublicAPI = isPublicApi;
    }

    public void setFilter(String filter) {
        this.customFilter = filter;
    }

    public void setTestList(File testlist) {
        this.testlist = testlist;
    }

    public void setReportSPI(String spi) {
        this.customReportSPI = spi;
    }

    public void setRewrite(boolean rewrite) {
        this.rewrite = rewrite;
    }

    public PatternSet createFilter() {
        if (filter != null) {
            throw new BuildException("Filter should be only one");
        }
        PatternSet ps = new PatternSet();
        filter = ps;
        return ps;
    }

    public void setTest(boolean test) {
        if (test) {
            System.setProperty("test.mode", "true");
        }
    }

    public void setFmlist(File fmlist) throws IOException {
        String[] lines = Utils.readLines(fmlist.getPath());
        for (String s : lines) {
            fm.add(new FM(s));
        }
        this.fmlist = fmlist;
    }

    public FM createFM() {
        FM f = new FM();
        fm.add(f);
        return f;
    }

    public static class FM {

        private String value;

        public FM() {
        }

        public FM(String value) {
            this.value = value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
