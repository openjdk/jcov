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

import com.sun.tdk.jcov.instrument.DataMethod;
import com.sun.tdk.jcov.instrument.DataField;
import com.sun.tdk.jcov.instrument.DataClass;
import com.sun.tdk.jcov.processing.DataProcessorSPI;
import com.sun.tdk.jcov.report.AncFilter;
import com.sun.tdk.jcov.util.Utils;
import com.sun.tdk.jcov.data.FileFormatException;
import com.sun.tdk.jcov.data.Result;
import com.sun.tdk.jcov.instrument.DataRoot;
import com.sun.tdk.jcov.processing.ProcessingException;

import com.sun.tdk.jcov.tools.EnvHandler;
import com.sun.tdk.jcov.tools.OptionDescr;
import com.sun.tdk.jcov.io.Reader;
import com.sun.tdk.jcov.filter.ConveyerFilter;
import com.sun.tdk.jcov.filter.MemberFilter;
import com.sun.tdk.jcov.filter.FilterFactory;
import com.sun.tdk.jcov.instrument.InstrumentationOptions;
import com.sun.tdk.jcov.io.ClassSignatureFilter;
import com.sun.tdk.jcov.processing.DefaultDataProcessorSPI;
import com.sun.tdk.jcov.processing.StubSpi;
import com.sun.tdk.jcov.report.DefaultReportGeneratorSPI;
import com.sun.tdk.jcov.report.ProductCoverage;
import com.sun.tdk.jcov.report.ReportGenerator;
import com.sun.tdk.jcov.report.ReportGeneratorSPI;
import com.sun.tdk.jcov.report.SmartTestService;
import com.sun.tdk.jcov.report.javap.JavapClass;
import com.sun.tdk.jcov.report.javap.JavapRepGen;
import com.sun.tdk.jcov.tools.JCovCMDTool;
import com.sun.tdk.jcov.tools.SPIDescr;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sun.tdk.jcov.tools.OptionDescr.*;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.objectweb.asm.Opcodes;

/**
 * <p> Report generation. </p>
 *
 * @author Andrey Titov
 * @author Dmitry Fazunenko
 * @author Alexey Fedorchenko
 */
public class RepGen extends JCovCMDTool {

    final static String CUSTOM_REPORT_GENERATOR_SPI = "customreport.spi";
    final static String DATA_PROCESSOR_SPI = "dataprocessor.spi";

    // logger initialization
    static {
        Utils.initLogger();
        logger = Logger.getLogger(RepGen.class.getName());
    }
    private final static Logger logger;
    /**
     * Consider or not enums. true - means ignore enum classes.
     */
    private boolean noEnums = false;
    /**
     * Whether show or not field coverage in the report (false by default).
     */
    private boolean showFields = false;
    private ReportGeneratorSPI reportGeneratorSPIs[];
    private DataProcessorSPI dataProcessorSPIs[];
    private String[] include = new String[]{".*"};
    private String[] exclude = new String[]{""};
    private String[] m_include = new String[]{".*"};
    private String[] m_exclude = new String[]{""};
    private String[] fms = null;
    private String filter = null;
    private String[] ancfilters = null;
    private boolean noAbstract = false;
    private boolean syntheticOn = false;
    private boolean isPublicAPI = false;
    private String[] filenames;
    private String name;
    private String outputDir;
    private String testlist;
    private String srcRootPath;
    private boolean anonym = false;
    private boolean withTestsInfo = false;
    //path to the jar, dir or .class for javap repgen
    private String classesPath;
    private AncFilter[] ancfiltersClasses = null;
    private String mainReportTitle = null;
    private String overviewListTitle = null;
    private String entitiesTitle = null;

    public RepGen() {
        readPlugins = true;
    }

    /**
     * Generate report using default (html) report generator
     *
     * @param output
     * @param jcovResult
     * @throws ProcessingException
     * @throws FileFormatException
     * @throws Exception
     */
    public void generateReport(String output, Result jcovResult) throws ProcessingException, FileFormatException, Exception {
        generateReport(getDefaultReportGenerator(), output, jcovResult, null);
    }

    /**
     * Generate report using specified format
     *
     * @param format
     * @param output
     * @param jcovResult
     * @throws ProcessingException
     * @throws FileFormatException
     * @throws Exception
     */
    public void generateReport(String format, String output, Result jcovResult) throws ProcessingException, FileFormatException, Exception {
        generateReport(format, output, jcovResult, null);
    }

    /**
     * Generate report using specified format
     *
     * @param format
     * @param output
     * @param jcovResult
     * @param srcRootPath
     * @throws ProcessingException
     * @throws FileFormatException
     * @throws Exception
     */
    public void generateReport(String format, String output, Result jcovResult, String srcRootPath) throws ProcessingException, FileFormatException, Exception {
        ReportGenerator rg = null;
        if (format != null) {
            rg = findReportGenerator(format);
        } else {
            rg = getDefaultReportGenerator();
        }
        if (rg == null) {
            throw new Exception("Specified ReportGenerator name (" + format + ") was not found");
        }

        generateReport(rg, output, jcovResult, srcRootPath);
    }

    /**
     * Generate report using specified report generator
     *
     * @param rg
     * @param output
     * @param jcovResult
     * @param srcRootPath
     * @throws ProcessingException
     * @throws FileFormatException
     * @throws Exception
     */
    public void generateReport(ReportGenerator rg, String output, Result jcovResult, String srcRootPath) throws ProcessingException, FileFormatException, Exception {
        generateReport(rg, output, jcovResult, srcRootPath, null);
    }

    /**
     * Generate report using specified report generator
     *
     * @param rg
     * @param output
     * @param jcovResult
     * @param srcRootPath
     * @param classes parsed javap classes
     * @throws ProcessingException
     * @throws FileFormatException
     * @throws Exception
     */
    public void generateReport(ReportGenerator rg, String output, Result jcovResult, String srcRootPath, List<JavapClass> classes) throws ProcessingException, FileFormatException, Exception {
        try {
            logger.log(Level.INFO, "-- Writing report to {0}", output);
            rg.init(output);
            logger.fine("OK");
        } catch (Throwable ex) {
            logger.log(Level.SEVERE, "Error while reading output file by ReportGenerator " + rg.getClass().getName(), ex);
            return;
        }

        logger.log(Level.INFO, "-- Reading data from {0}", jcovResult.getResultPath());
        DataRoot file_image = readDataRootFile(jcovResult.getResultPath(), jcovResult.isTestListSet(), include, exclude, fms);

        if (!syntheticOn) {
            file_image.applyFilter(new ANC_FILTER());
        }

        MemberFilter customFilter = null;
        if (filter != null) {
            logger.fine("-- Initializing custom filter");
            customFilter = initCustomFilter(filter, null);
            logger.fine("OK");
        }
        if (customFilter != null) {
            logger.log(Level.INFO, "-- Applying filter {0}", customFilter.getClass().getName());
            file_image.applyFilter(customFilter);
            logger.fine("OK");
        }

        if (ancfilters != null){
            ancfiltersClasses = new AncFilter[ancfilters.length];
            for (int i = 0; i < ancfilters.length; i++) {
                try {
                    String ancfilter = ancfilters[i];
                    Class ancFilteClass = Class.forName(ancfilter);
                    ancfiltersClasses[i] = (AncFilter) ancFilteClass.newInstance();
                } catch (Exception e) {
                    throw new Error("Cannot create an instance of "
                            + "AncFilter: ", e);
                }
            }
        }

        if (dataProcessorSPIs != null) {
            for (DataProcessorSPI spi : dataProcessorSPIs) {
                logger.log(Level.INFO, "-- Applying data processor {0}", spi.getClass());
                file_image = spi.getDataProcessor().process(file_image);
            }
        }
        logger.fine("OK");

        SmartTestService sts = null;
        if (jcovResult.isTestListSet()) {
            logger.fine("-- Initializing test list");
            sts = new SmartTestService(jcovResult.getTestList());
            if (file_image.getScaleOpts().getScaleSize() != sts.getTestCount()) {
                logger.log(Level.SEVERE, "The sizes of tests in JCov file and in test list differ.\n"
                        + "Datafile {0} contains {1} item(s).\nThe test list contains {2} item(s).",
                        new Object[]{jcovResult.getResultPath(), file_image.getScaleOpts().getScaleSize(), sts.getTestCount()});
                throw new Exception("The sizes of tests in JCov file and in test list differ");
            }
            logger.fine("OK");
        }
        ReportGenerator.Options options = new ReportGenerator.Options(srcRootPath, sts, classes, withTestsInfo, false,
                mainReportTitle, overviewListTitle, entitiesTitle);
        options.setInstrMode(file_image.getParams().getMode());
        options.setAnonymOn(anonym);

        try {
            ProductCoverage coverage = new ProductCoverage(file_image, options.getSrcRootPaths(), options.getJavapClasses(), isPublicAPI, noAbstract, anonym, ancfiltersClasses);

            logger.log(Level.INFO, "- Starting ReportGenerator {0}", rg.getClass().getName());
            rg.generateReport(coverage, options);
        } catch (Throwable ex) {
            if (ex.getMessage() != null) {
                throw new Exception("ReportGenerator produced exception " + ex.getMessage(), ex);
            } else {
                throw new Exception("ReportGenerator produced exception " + ex, ex);
            }
        }

        logger.log(Level.INFO, "- Report generation done");
        return;
    }

    /**
     * Get default (html) report generator
     *
     * @return default (html) report generator
     */
    public ReportGenerator getDefaultReportGenerator() {
        return findReportGenerator("html");
    }

    private ReportGenerator findReportGenerator(String name) {
        ReportGenerator rg = null;
        if (reportGeneratorSPIs != null) {
            for (ReportGeneratorSPI reportGeneratorSPI : reportGeneratorSPIs) {
                rg = reportGeneratorSPI.getReportGenerator(name);
                if (rg != null) {
                    return rg;
                }
            }
        }
        return new DefaultReportGeneratorSPI().getReportGenerator(name); // can be null
    }

    protected DataRoot readDataRootFile(String filename, boolean readScales, String[] include, String[] exclude, String[] modif) throws FileFormatException {
        DataRoot file_image = null;
        ClassSignatureFilter acceptor = new ClassSignatureFilter(include, exclude, m_include, m_exclude, modif);
        file_image = Reader.readXML(filename, readScales, acceptor);
        return file_image;
    }

    /**
     * Legacy CMD line entry poiny (use 'java -jar jcov.jar Merger' from cmd
     * instead of 'java -cp jcov.jar com.sun.tdk.jcov.Merger')
     *
     * @param args
     */
    public static void main(String args[]) {
        RepGen tool = new RepGen();
        try {
            int res = tool.run(args);
            System.exit(res);
        } catch (Exception ex) {
            System.exit(1);
        }
    }

    protected String usageString() {
        return "java com.sun.tdk.jcov.RepGen [options] filename";
    }

    protected String exampleString() {
        return "java -cp jcov.jar com.sun.tdk.jcov.RepGen -include java.lang.* -format html -output out result.xml";
    }

    protected String getDescr() {
        return "generates text or HTML (or custom) reports";
    }

    private MemberFilter createCustomFilter(String spiName) {
        return FilterFactory.
                getInstance(spiName).getMemberFilter();

    }

    private MemberFilter initCustomFilter(String filter, String sig) {
        MemberFilter customPlugin = null;
        MemberFilter sigFilter = null;

        if (filter != null) {
            customPlugin = createCustomFilter(filter);
        }
        if (customPlugin == null && sigFilter == null) {
            return null;
        } else if (customPlugin != null && sigFilter != null) {
            ConveyerFilter f = new ConveyerFilter();
            f.add(sigFilter);
            f.add(customPlugin);
            return f;
        } else {
            return sigFilter != null ? sigFilter : customPlugin;
        }

    }

    public void setReportGeneratorSPIs(ReportGeneratorSPI reportGeneratorSPI[]) {
        this.reportGeneratorSPIs = reportGeneratorSPI;
    }

    public ReportGeneratorSPI[] getReportGeneratorSPIs() {
        return reportGeneratorSPIs;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String[] getFms() {
        return fms;
    }

    public void setFms(String[] fms) {
        this.fms = fms;
    }

    public String[] getInclude() {
        return include;
    }

    public void setInclude(String[] include) {
        this.include = include;
    }

    public boolean isIsPublicAPI() {
        return isPublicAPI;
    }

    public void setIsPublicAPI(boolean isPublicAPI) {
        this.isPublicAPI = isPublicAPI;
    }

    public boolean isNoAbstract() {
        return noAbstract;
    }

    public void setNoAbstract(boolean noAbstract) {
        this.noAbstract = noAbstract;
    }

    public boolean isSyntheticOn() {
        return syntheticOn;
    }

    public void setNoANC(boolean syntheticOn) {
        this.syntheticOn = syntheticOn;
    }

    public boolean isWithTestsInfo() {
        return withTestsInfo;
    }

    public void setWithTestsInfo(boolean withTestsInfo) {
        this.withTestsInfo = withTestsInfo;
    }

    public boolean isNoEnums() {
        return noEnums;
    }

    public void setNoEnums(boolean noEnums) {
        this.noEnums = noEnums;
    }

    public boolean isShowFields() {
        return showFields;
    }

    public void setShowFields(boolean showFields) {
//        this.showFields = showFields;
    }

    public String[] getExclude() {
        return exclude;
    }

    public void setExclude(String[] exclude) {
        this.exclude = exclude;
    }

    public String getSrcRootPath() {
        return srcRootPath;
    }

    /**
     * Reset all properties to defaults. reportGeneratorSPI = null; include =
     * new String[] {".*"}; exclude = new String[] {""}; fms = null; filter =
     * null; generateShortFormat = false; noAbstract = false; isPublicAPI =
     * false; showMethods = true; showBlocks = true; showBranches = true;
     * showLines = true;
     */
    public void resetDefaults() {
        try {
            handleEnv_(defineHandler());
            reportGeneratorSPIs = null;
            //setFilters(new String[]{".*"}, new String[]{""}, null);
            setNoAbstract(false);
            setIsPublicAPI(false);
        } catch (EnvHandlingException ex) {
            // should not happen
        }
    }

    /**
     * Set all properties (except custorm report service provider)
     *
     * @param include patterns for including data. Set null for default value -
     * {".*"} (include all)
     * @param exclude patterns for excluding data. Set null for default value -
     * {""} (exclude nothing)
     * @param classModifiers modifiers that should have a class to be included
     * @param filter custom filter classname
     * @param generateShortFormat should generate short format
     * @param publicAPI should generate only public API (public and protected)
     * @param hideAbstract should hide abstract data
     * @param hideMethods should hide methods
     * @param hideBlocks should hide blocks
     * @param hideBranches should hide branches
     * @param hideLines should hide lines
     */
    public void configure(String[] include, String[] exclude, String[] classModifiers,
            String filter, boolean generateShortFormat, boolean publicAPI, boolean hideAbstract,
            boolean hideMethods, boolean hideBlocks, boolean hideBranches, boolean hideLines,
            boolean hideFields) {
        setFilters(include, exclude, classModifiers);
        setFilter(filter);
        this.isPublicAPI = publicAPI;
//        this.showFields = !hideFields;
    }

    /**
     * Set filtering properties for the report
     *
     * @param include patterns for including data. Set null for default value -
     * {".*"} (include all)
     * @param exclude patterns for excluding data. Set null for default value -
     * {""} (exclude nothing)
     * @param classModifiers modifiers that should have a class to be included
     */
    public void setFilters(String[] include, String[] exclude, String[] classModifiers) {
        if (include == null) {
            include = new String[]{".*"};
        }
        this.include = include;
        if (exclude == null) {
            exclude = new String[]{""};
        }
        this.exclude = exclude;
        this.fms = classModifiers;
    }

    @Override
    protected int run() throws Exception {
        Result r;
        boolean srcZipped = false;
        if (srcRootPath != null) {
            File srcRootPathFile = new File(srcRootPath);

            if (srcRootPathFile.exists() && srcRootPathFile.isFile() && (srcRootPath.endsWith(".zip") || srcRootPath.endsWith(".jar"))) {
                srcZipped = true;
                srcRootPath = outputDir + File.separator + srcRootPathFile.getName().replace(".zip", "").replace(".jar", "");
                Utils.unzipFolder(srcRootPathFile, srcRootPath);
            }
        }

        try {

            logger.log(Level.INFO, "-- Reading test list");
            if (filenames.length == 1) {
                r = new Result(filenames[0], testlist);
            } else {
                Merger merger = new Merger();
                Result[] results = Merger.initResults(filenames, true);
                Merger.Merge merge = new Merger.Merge(results, null);
                merger.setAddMissing(true);
                merger.setRead_scales(true);
                merger.setDefaultReadingFilter(include, exclude, m_include, m_exclude, fms);
                merger.merge(merge, outputDir, true);

                ReportGenerator rg;
                if (name != null) {
                    rg = findReportGenerator(name);
                } else {
                    rg = getDefaultReportGenerator();
                }
                if (rg == null) {
                    throw new Exception("Specified ReportGenerator name (" + name + ") was not found");
                }
                rg.init(outputDir);
                String[] tl = testlist != null ? Utils.readLines(testlist) : merge.getResultTestList();
                SmartTestService sts = new SmartTestService(tl);
                ReportGenerator.Options options = new ReportGenerator.Options(srcRootPath, sts, null, true, true,
                        mainReportTitle, overviewListTitle, entitiesTitle);
                try {
                    DataRoot mergedResult = merge.getResult();
                    if (!syntheticOn) {
                        mergedResult.applyFilter(new ANC_FILTER());
                    }
                    ProductCoverage coverage = new ProductCoverage(mergedResult, options.getSrcRootPaths(), null, isPublicAPI, noAbstract, ancfiltersClasses);
                    rg.generateReport(coverage, options);

                    if (srcZipped) {
                        Utils.deleteDirectory(new File(srcRootPath));
                    }

                } catch (Throwable ex) {
                    if (ex.getMessage() != null) {
                        throw new Exception("ReportGenerator produced exception " + ex.getMessage(), ex);
                    } else {
                        throw new Exception("ReportGenerator produced exception " + ex, ex);
                    }
                }

                return 0;
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error while reading testlist", ex);
            return 1;
        }

        if (classesPath != null) {
            try {
                logger.log(Level.INFO, "-- Creating javap report");
                new JavapRepGen(include, exclude).run(filenames[0], classesPath, outputDir);
                return 0;
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Error while creating javap report", ex);
                return 1;
            }
        }

        try {
            generateReport(name, outputDir, r, srcRootPath);

            if (srcZipped) {
                Utils.deleteDirectory(new File(srcRootPath));
            }

            return 0;
        } catch (FileFormatException e) {
//            logger.log(Level.SEVERE, "malformed jcov file \"{0}", filename);
            logger.log(Level.SEVERE, e.getMessage(), Arrays.toString(filenames));
        } catch (ProcessingException ex) {
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return SUCCESS_EXIT_CODE;
    }

    @Override
    protected EnvHandler defineHandler() {
        EnvHandler envHandler = new EnvHandler(new OptionDescr[]{
                    DSC_FMT,
                    DSC_OUTPUT,
                    //            DSC_STDOUT,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_INCLUDE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_EXCLUDE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_INCLUDE_LIST,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_EXCLUDE_LIST,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_MINCLUDE_LIST,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_MEXCLUDE_LIST,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_MINCLUDE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_MEXCLUDE,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_FM,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_FM_LIST,
                    DSC_NO_ABSTRACT,
                    DSC_SYNTHETIC_ON,
                    DSC_PUBLIC_API,
                    DSC_SRC_ROOT,
                    DSC_VERBOSE,
                    DSC_FILTER_PLUGIN,
                    DSC_ANC_FILTER_PLUGINS,
                    DSC_TEST_LIST,
                    DSC_ANONYM,
                    DSC_JAVAP,
                    DSC_TESTS_INFO,
                    DSC_REPORT_TITLE_MAIN,
                    DSC_REPORT_TITLE_OVERVIEW,
                    DSC_REPORT_TITLE_ENTITIES,}, this);
        SPIDescr spiDescr = new SPIDescr(CUSTOM_REPORT_GENERATOR_SPI, ReportGeneratorSPI.class);
        spiDescr.setDefaultSPI(new DefaultReportGeneratorSPI());
        envHandler.registerSPI(spiDescr);

        spiDescr = new SPIDescr(DATA_PROCESSOR_SPI, DataProcessorSPI.class);
        spiDescr.addPreset("none", new StubSpi());
        spiDescr.setDefaultSPI(new DefaultDataProcessorSPI());
        envHandler.registerSPI(spiDescr);

        return envHandler;
    }

    private int handleEnv_(EnvHandler opts) throws EnvHandlingException {
        if (opts.isSet(DSC_VERBOSE)) {
//            LoggingFormatter.printStackTrace = true; // by default logger doesn't print stacktrace
            logger.setLevel(Level.INFO);
        } else {
            logger.setLevel(Level.SEVERE);
        }

        name = opts.getValue(DSC_FMT);
        outputDir = opts.getValue(DSC_OUTPUT);
        // no check for output

        filter = opts.getValue(DSC_FILTER_PLUGIN);
        ancfilters = opts.getValues(DSC_ANC_FILTER_PLUGINS);
        noAbstract = opts.isSet(DSC_NO_ABSTRACT);
        isPublicAPI = opts.isSet(DSC_PUBLIC_API);
        anonym = opts.isSet(DSC_ANONYM);
        syntheticOn = opts.isSet(DSC_SYNTHETIC_ON);

        include = InstrumentationOptions.handleInclude(opts);
        exclude = InstrumentationOptions.handleExclude(opts);
        fms = InstrumentationOptions.handleFM(opts);

        m_include = InstrumentationOptions.handleMInclude(opts);
        m_exclude = InstrumentationOptions.handleMExclude(opts);

        testlist = opts.getValue(DSC_TEST_LIST);
        Utils.checkFileCanBeNull(testlist, "testlist filename", Utils.CheckOptions.FILE_EXISTS, Utils.CheckOptions.FILE_CANREAD, Utils.CheckOptions.FILE_ISFILE);

        withTestsInfo = opts.isSet(DSC_TESTS_INFO);

        srcRootPath = null;
        if (opts.isSet(DSC_SRC_ROOT)) {
            srcRootPath = opts.getValue(DSC_SRC_ROOT);
        }

        if (opts.isSet(DSC_REPORT_TITLE_MAIN)){
            mainReportTitle = opts.getValue(DSC_REPORT_TITLE_MAIN);
        }
        if (opts.isSet(DSC_REPORT_TITLE_OVERVIEW)){
            overviewListTitle = opts.getValue(DSC_REPORT_TITLE_OVERVIEW);
        }
        if (opts.isSet(DSC_REPORT_TITLE_ENTITIES)){
            entitiesTitle = opts.getValue(DSC_REPORT_TITLE_ENTITIES);
        }

        ArrayList<ReportGeneratorSPI> reportGenerators = opts.getSPIs(ReportGeneratorSPI.class);
        if (reportGenerators != null) {
            reportGeneratorSPIs = reportGenerators.toArray(new ReportGeneratorSPI[reportGenerators.size()]);
        }
        ArrayList<DataProcessorSPI> dataProcessors = opts.getSPIs(DataProcessorSPI.class);
        if (dataProcessors != null) {
            dataProcessorSPIs = dataProcessors.toArray(new DataProcessorSPI[dataProcessors.size()]);
        }

        classesPath = opts.getValue(DSC_JAVAP);

        return SUCCESS_EXIT_CODE;
    }

    @Override
    protected int handleEnv(EnvHandler opts) throws EnvHandlingException {
        String[] srcs = opts.getTail();
        if (srcs == null) {
            throw new EnvHandlingException("no input files specified");
        }

        filenames = srcs;
        Utils.checkFileNotNull(filenames[0], "JCov datafile", Utils.CheckOptions.FILE_EXISTS, Utils.CheckOptions.FILE_CANREAD, Utils.CheckOptions.FILE_ISFILE);
        /*if (srcs.length > 1) {
         logger.log(Level.WARNING,"only \"{0}\" will be processed, the rest of the files will be ignored\n" +
         "\tto generate report for all files, merge them into one using the merger utility", filename);
         }*/

        return handleEnv_(opts);
    }

    public static class ANC_FILTER implements MemberFilter {

        @Override
        public boolean accept(DataClass clz) {
            return true;
        }

        @Override
        public boolean accept(DataClass clz, DataMethod m) {

            boolean ancMethod;

            //Synthetic method (and Bridge method)
            ancMethod = ((m.getAccess() & Opcodes.ACC_SYNTHETIC) != 0);

            //Enum method
            ancMethod = ancMethod
                    || (clz.getSuperName().equals("java/lang/Enum") && (m.getName().equals("valueOf") || m.getName().equals("values")));

            return !ancMethod;

        }

        @Override
        public boolean accept(DataClass clz, DataField f) {
            return true;
        }
    }
    final static OptionDescr DSC_FMT =
            new OptionDescr("format", new String[]{"fmt"},
            "Report generation output.", VAL_SINGLE,
            "Specifies the format of the report.\n"
            + "Use \"text\" for generate text report and \"html\" for generate HTML report\n"
            + "Text report in one file which contains method/block/branch coverage information.\n"
            + "HTML report contains coverage information with marked-up sources.\n\n"
            + "Custom reports can be specified with ReportGeneratorSPI interface.", "html");
    /**
     *
     */
    public final static OptionDescr DSC_OUTPUT =
            new OptionDescr("repgen.output", new String[]{"output", "o"}, "", OptionDescr.VAL_SINGLE,
            "Output directory for generating text and HTML reports.", "report");
    /**
     *
     */
    public final static OptionDescr DSC_TEST_LIST =
            new OptionDescr("tests", "Test list", OptionDescr.VAL_SINGLE,
            "Specify the path to the file containing test list. File should contain a list of tests\n"
            + "with one name per line.");
    final static OptionDescr DSC_FILTER_PLUGIN =
            new OptionDescr("filter", "", OptionDescr.VAL_SINGLE,
            "Custom filtering plugin class");

    final static OptionDescr DSC_ANC_FILTER_PLUGINS =
            new OptionDescr("ancfilter", new String[]{"ancf"}, "Custom anc filtering plugin classes", OptionDescr.VAL_MULTI,
                    "");

    /**
     *
     */
    public final static OptionDescr DSC_SRC_ROOT =
            new OptionDescr("sourcepath", new String[]{"source", "src"}, "The source files.", OptionDescr.VAL_SINGLE, "");
    final static OptionDescr DSC_VERBOSE =
            new OptionDescr("verbose", "Verbosity.", "Enable verbose mode.");
    /**
     *
     */
    public final static OptionDescr DSC_NO_ABSTRACT =
            new OptionDescr("noabstract", "Additional filtering", "Do not count abstract methods");
    public final static OptionDescr DSC_SYNTHETIC_ON =
            new OptionDescr("syntheticon", "Additional filtering", "Count coverage for synthetic methods");
    /**
     *
     */
    public final static OptionDescr DSC_PUBLIC_API =
            new OptionDescr("publicapi", "", "Count only public and protected members");
    public final static OptionDescr DSC_ANONYM =
            new OptionDescr("anonym", "", "include methods from anonymous classes into the report");
    public final static OptionDescr DSC_JAVAP =
            new OptionDescr("javap", new String[]{"javap"}, "Path to the class files of the product to use javap", OptionDescr.VAL_SINGLE, "");
    public final static OptionDescr DSC_TESTS_INFO =
            new OptionDescr("testsinfo", "Additional information about for specified tests' list", "Show covererage for all tests in test list");

    public final static OptionDescr DSC_REPORT_TITLE_MAIN =
            new OptionDescr("mainReportTitle", new String[]{"mainReportTitle", "mrtitle"}, "The main report title", OptionDescr.VAL_SINGLE, "");
    public final static OptionDescr DSC_REPORT_TITLE_OVERVIEW =
            new OptionDescr("overviewReportTitle", new String[]{"overviewReportTitle", "ortitle"}, "The overview list report title", OptionDescr.VAL_SINGLE, "");
    public final static OptionDescr DSC_REPORT_TITLE_ENTITIES =
            new OptionDescr("entitiesReportTitle", new String[]{"entitiesReportTitle", "ertitle"}, "Entities report title (for modules, packages, subpackages)", OptionDescr.VAL_SINGLE, "");
}