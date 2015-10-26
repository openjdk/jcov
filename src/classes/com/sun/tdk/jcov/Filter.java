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

import com.sun.tdk.jcov.filter.ConveyerFilter;
import com.sun.tdk.jcov.filter.FilterSpi;
import com.sun.tdk.jcov.filter.MemberFilter;
import com.sun.tdk.jcov.instrument.DataRoot;
import com.sun.tdk.jcov.instrument.InstrumentationOptions;
import com.sun.tdk.jcov.instrument.InstrumentationOptions.MERGE;
import com.sun.tdk.jcov.tools.EnvHandler;
import com.sun.tdk.jcov.io.ClassSignatureFilter;
import com.sun.tdk.jcov.io.Reader;
import com.sun.tdk.jcov.runtime.JCovXMLFileSaver;
import com.sun.tdk.jcov.tools.JCovCMDTool;
import com.sun.tdk.jcov.tools.OptionDescr;
import com.sun.tdk.jcov.tools.SPIDescr;
import com.sun.tdk.jcov.util.Utils;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * The class that applies a custom filter to coverage data file. It has a
 * command line entry point. It also can be used from other tools.
 *
 * Usage examples:
 * <pre>
 *   Filter filter = new Filter("plugin.myFilterSpi");
 *   filter.applyFilter("result.xml", "new.xml");
 * </pre> or from command line:
 * <pre>
 * # java com.sun.tdk.jcov.Filter -i com.sun.* -filter plugin.myFilterSpi result.xml new.xml
 * </pre>
 *
 * @author Dmitry Fazunenko
 * @author Sergey Borodin
 */
public class Filter extends JCovCMDTool {

    private String inFile = null;
    private String outFile = null;
    // primitive filter by class name include/exclude
    private ClassSignatureFilter readFilter;
    private MemberFilter filter;
    private String[] filterSpi = null;
    private boolean synthetic = false;

    /**
     *
     * Reads inFile, applies filter and writes result down to outFile.
     *
     * @param inFile - file to read
     * @param outFile - file to create (if null, System.out will be used)
     * @throws Exception
     */
    public void applyFilter(String inFile, String outFile)
            throws Exception {
        DataRoot root = Reader.readXML(inFile, true, readFilter);

        if (synthetic) {
            root.applyFilter(new RepGen.ANC_FILTER());
        }

        if (filter != null) {
            root.applyFilter(filter);
        }
        root.getParams().setIncludes(readFilter.getIncludes());
        root.getParams().setExcludes(readFilter.getExcludes());
        if (readFilter.getModifs() != null && readFilter.getModifs().length > 0) {
            root.getXMLHeadProperties().put("coverage.generator.modif", Arrays.toString(readFilter.getModifs()));
        } else {
            root.getXMLHeadProperties().remove("coverage.generator.modif");
        }
        if (filterSpi != null) {
            String get = root.getXMLHeadProperties().get("coverage.filter.plugin");
            root.getXMLHeadProperties().put("coverage.filter.plugin", Arrays.toString(filterSpi));
        } else {
            root.getXMLHeadProperties().remove("coverage.filter.plugin");
        }
        new JCovXMLFileSaver(root, MERGE.OVERWRITE).saveResults(outFile);
        root.destroy();
    }

    /**
     * Entry point for command line.
     *
     * @param args
     */
    public static void main(String args[]) {
        Filter tool = new Filter();
        try {
            int res = tool.run(args);
            System.exit(res);
        } catch (Exception ex) {
            System.exit(1);
        }
    }

    protected String usageString() {
        return "java " + Filter.class.getCanonicalName() + " [options] inFile outFile";
    }

    protected String exampleString() {
        return "java -cp jcov.jar:filter_classes "
                + Filter.class.getCanonicalName() + " -include java.lang.* "
                + "-filter java.xml lang.xml";
    }

    protected String getDescr() {
        return "filters out result data";
    }

///////// JCovTool implementation /////////
    @Override
    protected EnvHandler defineHandler() {
        EnvHandler eh = new EnvHandler(new OptionDescr[]{
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_INCLUDE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_INCLUDE_LIST,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_EXCLUDE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_EXCLUDE_LIST,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_FM,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_FM_LIST,
                    DSC_FILTER_PLUGIN,
                    DSC_SYNTHETIC
                }, this);
        eh.registerSPI(new SPIDescr("filter", FilterSpi.class));
        return eh;
    }

    @Override
    protected int handleEnv(EnvHandler opts) throws EnvHandlingException {
        String[] files = opts.getTail();
        if (files == null || files.length == 0) {
            throw new EnvHandlingException("no input file specified");
        } else if (files.length == 1) {
            throw new EnvHandlingException("no output file specified");
        } else if (files.length == 2) {
            inFile = files[0];
            Utils.checkFileNotNull(inFile, "input JCov datafile", Utils.CheckOptions.FILE_ISFILE, Utils.CheckOptions.FILE_EXISTS, Utils.CheckOptions.FILE_CANREAD);
            outFile = files[1];
            Utils.checkFileNotNull(outFile, "output JCov datafile", Utils.CheckOptions.FILE_NOTEXISTS, Utils.CheckOptions.FILE_CANWRITE);
        } else if (files.length > 2) {
            throw new EnvHandlingException("too many files specified");
        }

        synthetic = opts.isSet(DSC_SYNTHETIC);

        String[] exclude = InstrumentationOptions.handleExclude(opts);
        String[] include = InstrumentationOptions.handleInclude(opts);
        String[] fm = InstrumentationOptions.handleFM(opts);

        String[] m_exclude = InstrumentationOptions.handleMExclude(opts);
        String[] m_include = InstrumentationOptions.handleMInclude(opts);

        readFilter = new ClassSignatureFilter(include, exclude, m_include, m_exclude, fm);

        ArrayList<FilterSpi> filters = opts.getSPIs(FilterSpi.class);
        if (filters == null || filters.isEmpty()) {
            filter = null;
        } else if (filters.size() == 1) {
            filter = filters.get(0).getFilter();
        } else {
            ConveyerFilter cf = new ConveyerFilter();
            for (FilterSpi spi : filters) {
                cf.add(spi.getFilter());
            }
            filter = cf;
        }

        return SUCCESS_EXIT_CODE;
    }

    @Override
    protected int run() throws Exception {
        try {
            applyFilter(inFile, outFile);
        } catch (Exception e) {
            if (filter != null) {
                if (e instanceof NullPointerException) {
                    e.printStackTrace();
                }
                throw new Exception("Cannot apply filter '" + filter + "' to " + inFile + " - exception occured: " + e.getMessage(), e);
            } else {
                throw new Exception("Mishap:", e);
            }
        }
        return SUCCESS_EXIT_CODE;
    }

    /**
     * @return ClassSignatureFilter used by this Filter to read the DataRoot
     */
    public ClassSignatureFilter getReadingFilter() {
        return readFilter;
    }

    /**
     * Set the ClassSignatureFilter used by this Filter to read the DataRoot
     *
     * @param acceptor
     */
    public void setReadingFilter(ClassSignatureFilter acceptor) {
        this.readFilter = acceptor;
    }

    /**
     * @return MemberFilter used by this Filter
     */
    public MemberFilter getFilter() {
        return filter;
    }

    /**
     * Set MemberFilter used by this Filter
     *
     * @param filter
     */
    public void setFilter(MemberFilter filter) {
        this.filter = filter;
    }
    final static OptionDescr DSC_FILTER_PLUGIN =
            new OptionDescr("filter", "", OptionDescr.VAL_MULTI,
            "Custom filtering plugin class");
    public final static OptionDescr DSC_SYNTHETIC =
            new OptionDescr("synthetic", "Additional filtering", "Remove coverage for synthetic methods");
}