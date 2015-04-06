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
package com.sun.tdk.jcov.report.html;

import com.sun.tdk.jcov.instrument.InstrumentationOptions;
import com.sun.tdk.jcov.report.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.tdk.jcov.report.html.resources.CopyResources;
import com.sun.tdk.jcov.report.javap.JavapClass;
import com.sun.tdk.jcov.report.javap.JavapCodeLine;
import com.sun.tdk.jcov.report.javap.JavapLine;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/**
 * @author Dmitry Fazunenko
 * @author Alexey Fedorchenko
 */
public class CoverageReport implements ReportGenerator {

    private ProductCoverage coverage;
    private SmartTestService testService;
    private boolean isGenSrc4Zero = false;
    private boolean isGenHitTests = false;
    private boolean isAddTestsInfo = false;
    private boolean isMergeRepGenMode = false;
    private boolean isAnonymOn = false;
    private String title = "Coverage report";
    private String dir;
    private boolean showLines;
    private boolean showFields;
    private boolean showBranches;
    private boolean showBlocks;
    private boolean showMethods;
    private static final Logger logger;
    private InstrumentationOptions.InstrumentationMode mode;

    static {
        logger = Logger.getLogger(CoverageReport.class.getName());
        logger.setLevel(Level.WARNING);
    }

    @Override
    public void init(String dir) throws IOException {
        this.dir = dir;
        File d = new File(dir);
        if (d.exists() && !d.isDirectory()) {
            throw new IOException("Not a directory: " + dir);
        }
        if (!d.exists() && !d.mkdirs()) {
            throw new IOException("Failed to create directory: " + dir);
        }
    }

    @Override
    public void generateReport(ProductCoverage coverage, Options options) throws IOException {
        this.coverage = coverage;

        if (options.getTestListService() != null) {
            this.setTestService(options.getTestListService());
            this.setGenHitTests(true);
            this.setAddTestsInfo(options.isWithTestsInfo());
            this.setMergeRepGenMode(options.isMergeRepGenMode());
        }
        this.setInstrMode(options.getInstrMode());
        this.isAnonymOn = options.isAnonymOn();

        setGenSrc4Zero(true);
        generate();
    }

    public void generate() throws IOException {
        File directory = new File(dir);
        directory.mkdirs();
        CopyResources.copy(directory);
        generateSourceFiles(directory);
        generateFrameset(directory);
        generatePackageList(directory);
        generateClassList(directory);
        HashMap<String, CoverageData[]> modules = getModulesCoverage();
        if (modules == null || (modules.size() == 1 && "no_module".equals(modules.keySet().iterator().next()))){
            modules = null;
        }
        generateOverview(directory, modules);
        if (modules != null) {
            generateModulesList(directory, modules);
        }

    }

    private void generateModulesList(File directory, HashMap<String, CoverageData[]> modules) throws IOException{
        List<PackageCoverage> list = coverage.getPackages();

        for (String moduleName: modules.keySet()) {
            HashMap<String, CoverageData[]> module = new HashMap<String, CoverageData[]>();
            module.put(moduleName, modules.get(moduleName));
            generateModuleOverview(directory, list, module);
        }
    }

    private HashMap<String, CoverageData[]> getModulesCoverage(){
        HashMap<String, CoverageData[]> modules = null;
        List<PackageCoverage> list = coverage.getPackages();

        if (list == null || list.size() == 0 || list.get(0) == null
                || list.get(0).getClasses() == null || list.get(0).getClasses().get(0).getModuleName() == null){
            return modules;
        }

        modules = new HashMap<String, CoverageData[]>();
        for (PackageCoverage pkg : list) {
            String moduleName = pkg.getClasses().get(0).getModuleName();
            if (modules.get(moduleName) == null) {
                modules.put(moduleName, new CoverageData[]{pkg.getData(DataType.CLASS), pkg.getData(DataType.METHOD),
                        pkg.getData(DataType.BLOCK), pkg.getData(DataType.BRANCH), pkg.getData(DataType.LINE)});
            } else {
                CoverageData[] oldCD = modules.get(moduleName);
                modules.put(moduleName, new CoverageData[]{oldCD[0].add(pkg.getData(DataType.CLASS)), oldCD[1].add(pkg.getData(DataType.METHOD)),
                        oldCD[2].add(pkg.getData(DataType.BLOCK)), oldCD[3].add(pkg.getData(DataType.BRANCH)), oldCD[4].add(pkg.getData(DataType.LINE))});
            }
        }

        return modules;
    }

    public void setVerbose(boolean verbose) {
        logger.setLevel(verbose ? Level.CONFIG : Level.WARNING);
    }

    public void setTestService(SmartTestService testService) {
        this.testService = testService;
    }

    public void setGenSrc4Zero(boolean isGenSrc4Zero) {
        this.isGenSrc4Zero = isGenSrc4Zero;
    }

    public void setGenHitTests(boolean isGenHitTests) {
        this.isGenHitTests = isGenHitTests;
    }

    public void setAddTestsInfo(boolean isWithTestsInfo) {
        this.isAddTestsInfo = isWithTestsInfo;
    }

    public void setMergeRepGenMode(boolean isMergeRepGenMode) {
        this.isMergeRepGenMode = isMergeRepGenMode;
    }

    public void setInstrMode(InstrumentationOptions.InstrumentationMode mode) {
        this.mode = mode;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    private void generateFrameset(File dir) throws IOException {
        File fsFile = new File(dir, "index.html");
        PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                fsFile), Charset.defaultCharset())));
        pw.println("<html>");
        pw.println("<head>");
        pw.println("<title>" + title + " " /* + coverage.getData(ColumnName.PRODUCT)*/
                + "</title>");
        generateScriptsHeader(pw);
        pw.println("</head>");
        pw.println("<FRAMESET cols=\"20%,80%\" title=\"Documentation frame\" onclick=\"top.loadFrames()\">");
        pw.println("<FRAMESET rows=\"30%,70%\" title=\"Left frames\" onload=\"top.loadFrames()\">");
        pw
                .println("<FRAME src=\"overview-frame.html\" name=\"packageListFrame\" title=\"All Packages\">");
        pw
                .println("<FRAME src=\"allclasses-frame.html\" name=\"packageFrame\" title=\"All classes and interfaces (except non-static nested types)\">");
        pw.println("</FRAMESET>");
        pw
                .println("<FRAME src=\"overview-summary.html\" name=\"classFrame\" title=\"Package, class and interface descriptions\" scrolling=\"yes\">");
        pw.println("<NOFRAMES>");
        pw
                .println("This document is designed to be viewed using the frames feature. If you see this message, you are using a non-frame-capable web client.");
        pw.println("<BR>");
        pw
                .println("Link to<A HREF=\"overview-summary.html\">Non-frame version.</A>");
        pw.println("</NOFRAMES>");
        pw.println("</FRAMESET>");
        pw.println("</html>");
        pw.close();
    }

    private void generatePackageList(File dir) throws IOException {
        File fsFile = new File(dir, "overview-frame.html");
        logger.log(Level.INFO, "generatePackageList:{0}", fsFile.getAbsolutePath());
        PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                fsFile), Charset.defaultCharset())));
        pw.println("<html>");
        pw.println("<head>");
        pw.println("<title>coverage report</title>");
        pw
                .println("<link rel =\"stylesheet\" type=\"text/css\" href=\"style.css\" title=\"Style\">");
        pw.println("</head>");
        pw.println("<body>");
        pw.println("<span class=\"title\">" + title + "</span><br>");
        // pw.println("<span class=\"title2\">" + coverage.getData(ColumnName.PRODUCT)
        //    + "</span>");
        pw.println("<table>");
        pw.println("<tr>");
        pw.println("<td nowrap=\"nowrap\">");
        pw.println("<a href=\"overview-summary.html\" target=\"classFrame\">Overview</a><br>");
        pw.println("<a href=\"allclasses-frame.html\" target=\"packageFrame\">All classes</a>");
        pw.println("</td>");
        pw.println("</tr>");
        pw.println("</table>");
        pw.println("<p>");
        pw.println("<table>");
        pw.println("<tr>");
        pw.println("<td nowrap=\"nowrap\"><span class=\"title2\">All packages</span></td>");
        pw.println("</tr>");
        pw.println("<tr>");
        pw.println("<td nowrap=\"nowrap\">");
        List<PackageCoverage> list = coverage.getPackages();
        for (PackageCoverage pkg : list) {
            String pkgPath = pkg.getName().replace('.', '/');
            String url = pkgPath + "/package-frame.html";
            logger.log(Level.FINE, "generatePackageList:url:{0}", url);
            pw.println("<a href=\"" + url + "\" target=\"packageFrame\""
                    + " onClick=\"parent.frames[2].location.href='"
                    + pkgPath + "/package-summary.html" + "';\">"
                    + pkg.getName() + "</a><br>");
        }

        pw.println("</td>");
        pw.println("</tr>");
        pw.println("</table>");
        pw.println("</body>");
        pw.println("</html>");
        pw.close();

        for (PackageCoverage pkg : list) {
            generateClassList(dir, pkg);
            generateOverview(dir, pkg, null);
        }
    }

    private void generateClassList(File dir) throws IOException {
        generateClassList(dir, null);
    }

    private void generateClassList(File dir, PackageCoverage pkg)
            throws IOException {
        String filename;
        String rootRef;
        Collection<ClassCoverage> classes;
        String urlDirectory = "";
        if (pkg == null) {
            rootRef = "";
            filename = "allclasses-frame.html";
            java.util.List<PackageCoverage> list = coverage.getPackages();
            classes = new ArrayList<ClassCoverage>();
            for (PackageCoverage pcov : list) {
                classes.addAll(pcov.getClasses());
            }
            Collections.sort((List) classes);
        } else {
            rootRef = getRelativePath(pkg.getName());
            filename = pkg.getName().replace('.', '/') + "/package-frame.html";
            classes = pkg.getClasses();
            urlDirectory = ".";
        }
        logger.log(Level.INFO, "generateClassList:filename:{0}", filename);
        File fsFile = new File(dir, filename);
        if (!fsFile.getParentFile().exists()) {
            logger.log(Level.INFO, "mkdirs:{0}", fsFile.getParentFile());
            fsFile.getParentFile().mkdirs();
        }
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(
                fsFile), Charset.defaultCharset()));
        pw.println("<html>");
        pw.println("<head>");
        pw.println("<title>coverage report</title>");
        pw.println("<link rel =\"stylesheet\" type=\"text/css\" href=\"" + rootRef
                + "style.css\" title=\"Style\">");
        pw.println("</head>");
        pw.println("<body>");
        if (pkg != null) {
            pw.println("<a href=\"package-summary.html\" target=\"classFrame\">"
                    + pkg.getName() + "</a> "
                    + "<span class=\"text_italic\">&nbsp;" + pkg.getCoverageString(DataType.METHOD)
                    + "</span><br>");
            pw.println("<p>");
        }
        pw.println("<span class=\"title\">All classes</span>");
        pw.println("<table>");
        pw.println("<tr>");
        pw.println("<td nowrap=\"nowrap\">");

        int i = 0;
        for (ClassCoverage theClass : classes) {
            logger.log(Level.INFO, "{0} generateClassList:theClass:{1}", new Object[]{++i, theClass.getName()});
            String prc = showFields
                    ? theClass.getData(DataType.METHOD).add(theClass.getData(DataType.FIELD)).toString()
                    : theClass.getCoverageString(DataType.METHOD);

            if (pkg == null) {
                if (theClass.getFullClassName().lastIndexOf('.') > 0) {
                    urlDirectory = theClass.getFullClassName().substring(0,
                            theClass.getFullClassName().lastIndexOf('.')).replace('.', '/');
                } else {
                    urlDirectory = ".";
                }
            }
            String classFilename = theClass.getName() + ".html";
            pw.println("<a href=\"" + urlDirectory + "/" + classFilename
                    + "\" target=\"classFrame\">" + theClass.getName()
                    + "</a><span class=\"text_italic\">&nbsp;" + prc + "</span><br>");
        }

        pw.println("</td>");
        pw.println("</tr>");
        pw.println("</table>");
        pw.println("</body>");
        pw.println("</html>");
        pw.close();
    }

    private void generateOverview(File dir, HashMap<String, CoverageData[]> modules) throws IOException {
        generateOverview(dir, null, modules);
    }

    private void generateModuleOverview(File dir, List<PackageCoverage> allPkgList, HashMap<String, CoverageData[]> module) throws IOException {
        String filename = "overview-summary.html";
        String rootRef = "../";
        String moduleName = module.keySet().iterator().next();
        if (moduleName != null) {
            filename = moduleName
                    + "/module-summary.html";
            rootRef = "../";
        } else {
            rootRef = "";
        }

        File fsFile = new File(dir, filename);
        if (!fsFile.getParentFile().exists()) {
            logger.log(Level.INFO, "mkdirs:{0}", fsFile.getParentFile());
            fsFile.getParentFile().mkdirs();
        }
        PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                fsFile), Charset.defaultCharset())));
        pw.println("<html>");
        pw.println("<head>");
        pw.println("<title>coverage report</title>");
        pw.println("<link rel =\"stylesheet\" type=\"text/css\" href=\"" + rootRef
                + "style.css\" title=\"Style\">");
        pw.println("<script type=\"text/javascript\" src=\"" + rootRef
                + "sorttable.js\"></script>");
        pw.println("</head>");
        pw.println("<body>");
        pw.println("<span class=\"title\"> <b>" + title + " "
                + /*coverage.getData(ColumnName.PRODUCT) + */ "</b> </span>");
        pw.println("<p>");

        if (module != null) {
            generateModulesInfo(pw, module, true/*,""*/);
        }

        pw.println("</tr>");
        pw.println("</table>");
        pw.println("<p>");

        List<PackageCoverage> modulesPkgList = new ArrayList<PackageCoverage>();
        for (PackageCoverage pc : allPkgList){
            if (pc.getClasses().get(0).getModuleName().equals(moduleName)){
                modulesPkgList.add(pc);
            }
        }

        List<PackageCoverage> pkgList = modulesPkgList;
        List<ClassCoverage> classes = new ArrayList<ClassCoverage>();
        for (PackageCoverage thePackage : pkgList) {
            classes.addAll(thePackage.getClasses());
        }
            //PackageCoverage thePackage = pc;

            SubpackageCoverage subCov = new SubpackageCoverage();
           // if (thePackage != null) {

                //pkgList = getSubPackages(thePackage);
              //  if (pkgList.size() > 0) {
                    //subCov.add(thePackage);
                    pw.println("<span class=\"title2\">Packages</span><br>");
                    pw.println("<table class=\"report\" cellpadding=\"0\" cellspacing=\"0\" id=\"subpackages\">");
                    pw.println("<tr class=\"report\">");
                    pw.println("<th class=\"report\">Name</th>");
                    pw.println("<th class=\"report_number\">#classes</th>");
                    pw.println("<th class=\"report\">%class</th>");
                    printColumnHeaders(pw, "");
                    //pw.println("<th class=\"report\">%total</th>");
                    pw.println("</tr>");
                    for (PackageCoverage pkg : pkgList) {
                        subCov.add(pkg);
                        pw.println("<tr class=\"report\">");
                        String subPkgDir = "../"+pkg.getName()/*.substring(
                                thePackage.getName().length() + 1)*/.replace('.', '/');
                        pw
                                .println("<td class=\"reportText\"><a href=\"" + subPkgDir
                                        + "/package-summary.html\">" + pkg.getName()
                                        + "</a></td>");
                        pw.println("<td class=\"reportValue\">"
                                + pkg.getData(DataType.CLASS).getTotal() + "</td>");
                        pw.println("<td class=\"reportValue\">"
                                + decorate(pkg.getCoverageString(DataType.CLASS)) + "</td>");
                        printColumnCoverages(pw, pkg, true, "");
                    /*pw.println("<td class=\"reportValue\">"
                     + generatePercentResult(pkg.getTotalCoverageString()) + "</td>");*/
                        pw.println("</tr>");
                    }
                    pw.println("</table>");
                    pw.println("<p>");
              //  }


                logger.log(Level.INFO, "generateOverview:classes.size:{0}", classes.size());
                if (classes.size() > 0) {
                    pw.println("<span class=\"title2\">Classes</span><br>");
                    pw.println("<table class=\"report\" cellpadding=\"0\" cellspacing=\"0\" id=\"classes\">");
                    pw.println("<tr class=\"report\">");
                    pw.println("<th class=\"report\">Name</th>");
                    printColumnHeaders(pw, "");
                    // pw.println("<th class=\"report\">%total</th>");
                    pw.println("</tr>");
                    int i = 0;
                    for (ClassCoverage cc : classes) {
                        logger.log(Level.INFO, "{0} generateOverview():cl:{1}", new Object[]{++i, cc.getName()});
                        String classFilename = cc.getName();
                        pw.println("<tr class=\"report\">");
                        pw.println("<td class=\"reportText\"><a href=\"" +"../"+cc.getPackageName().replace('.', '/')+"/"+ classFilename
                                + ".html\">" + classFilename + "</a></td>");
                        printColumnCoverages(pw, cc, true, "");
                        // pw.println("<td class=\"reportValue\">"
                        //   + generatePercentResult(cc.getTotalCoverageString()) + "</td>");
                        pw.println("</tr>");
                    }
                }
            //}
            pw.println("</table>");

        pw.println(generateFooter());
        pw.println("</body>");
        pw.println("</html>");
        pw.close();
    }

    private void generateOverview(File dir, PackageCoverage thePackage, HashMap<String, CoverageData[]> modules)
            throws IOException {
        String filename = "overview-summary.html";
        String rootRef;
        if (thePackage != null) {
            filename = thePackage.getName().replace('.', '/')
                    + "/package-summary.html";
            rootRef = getRelativePath(thePackage.getName());
        } else {
            rootRef = "";
        }
        logger.log(Level.INFO, "generateOverview:filename:{0}", filename);
        File fsFile = new File(dir, filename);
        if (!fsFile.getParentFile().exists()) {
            logger.log(Level.INFO, "mkdirs:{0}", fsFile.getParentFile());
            fsFile.getParentFile().mkdirs();
        }
        PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                fsFile), Charset.defaultCharset())));
        pw.println("<html>");
        pw.println("<head>");
        pw.println("<title>coverage report</title>");
        pw.println("<link rel =\"stylesheet\" type=\"text/css\" href=\"" + rootRef
                + "style.css\" title=\"Style\">");
        pw.println("<script type=\"text/javascript\" src=\"" + rootRef
                + "sorttable.js\"></script>");
        pw.println("</head>");
        pw.println("<body>");
        pw.println("<span class=\"title\">" + title + " "
                + /*coverage.getData(ColumnName.PRODUCT) + */ "</span>");
        pw.println("<p>");
        pw.println("<table class=\"report\" cellpadding=\"0\" cellspacing=\"0\">");
        pw.println("<tr class=\"report\">");
        pw.println("<th class=\"report\">&nbsp;</th>");
        pw.println("<th class=\"report_number\">#classes</th>");
        printColumnHeaders(pw, "");
        // pw.println("<th class=\"report\">%total</th>");
        pw.println("</tr>");
        pw.println("<tr class=\"report\">");
        if (thePackage != null) {
            pw.println("<td class=\"reportText\"> <b>" + thePackage.getName()
                    + "</b></td>");
            pw.println("<td class=\"reportValue_number\">"
                    + thePackage.getData(DataType.CLASS).getTotal() + "</td>");
            printColumnCoverages(pw, thePackage, false, "");
            //  pw.println("<td class=\"reportValue\">"
            //      + generatePercentResult(thePackage.getTotalCoverageString())
            //      + "</td>");
        } else {
            pw.println("<td class=\"reportText\"> <b>"
                    + "Overall statistics" + "</b> </td>");
            pw.println("<td class=\"reportValue_number\">"
                    + coverage.getData(DataType.CLASS).getTotal() + "</td>");
            printColumnCoverages(pw, coverage, false, "");
            //  pw.println("<td class=\"reportValue\">"
            //    + generatePercentResult(coverage.getTotalCoverageString()) + "</td>");
        }
        pw.println("</tr>");
        pw.println("</table>");
        pw.println("<p>");

        List<PackageCoverage> pkgList = null;
        SubpackageCoverage subCov = new SubpackageCoverage();
        if (thePackage != null) {
            //generateModulesInfo(pw, modules, false, thePackage.getName());
            pkgList = getSubPackages(thePackage);
            if (pkgList.size() > 0) {
                subCov.add(thePackage);
                pw.println("<span class=\"title2\">Subpackages</span><br>");
                pw.println("<table class=\"report\" cellpadding=\"0\" cellspacing=\"0\" id=\"subpackages\">");
                pw.println("<tr class=\"report\">");
                pw.println("<th class=\"report\">Name</th>");
                pw.println("<th class=\"report_number\">#classes</th>");
                pw.println("<th class=\"report\">%class</th>");
                printColumnHeaders(pw, "");
                //pw.println("<th class=\"report\">%total</th>");
                pw.println("</tr>");
                for (PackageCoverage pkg : pkgList) {
                    subCov.add(pkg);
                    pw.println("<tr class=\"report\">");
                    String subPkgDir = pkg.getName().substring(
                            thePackage.getName().length() + 1).replace('.', '/');
                    pw
                            .println("<td class=\"reportText\"><a href=\"" + subPkgDir
                            + "/package-summary.html\">" + pkg.getName()
                            + "</a></td>");
                    pw.println("<td class=\"reportValue_number\">"
                            + pkg.getData(DataType.CLASS).getTotal() + "</td>");
                    pw.println("<td class=\"reportValue\">"
                            + decorate(pkg.getCoverageString(DataType.CLASS)) + "</td>");
                    printColumnCoverages(pw, pkg, true, "");
                    /*pw.println("<td class=\"reportValue\">"
                     + generatePercentResult(pkg.getTotalCoverageString()) + "</td>");*/
                    pw.println("</tr>");
                }
                pw.println("</table>");
                pw.println("<p>");
            }

            List<ClassCoverage> classes = thePackage.getClasses();
            logger.log(Level.INFO, "generateOverview:classes.size:{0}", classes.size());
            if (classes.size() > 0) {
                pw.println("<span class=\"title2\">Classes</span><br>");
                pw.println("<table class=\"report\" cellpadding=\"0\" cellspacing=\"0\" id=\"classes\">");
                pw.println("<tr class=\"report\">");
                pw.println("<th class=\"report\">Name</th>");
                printColumnHeaders(pw, "");
                // pw.println("<th class=\"report\">%total</th>");
                pw.println("</tr>");
                int i = 0;
                for (ClassCoverage cc : classes) {
                    logger.log(Level.INFO, "{0} generateOverview():cl:{1}", new Object[]{++i, cc.getName()});
                    String classFilename = cc.getName();
                    pw.println("<tr class=\"report\">");
                    pw.println("<td class=\"reportText\"><a href=\"" + classFilename
                            + ".html\">" + classFilename + "</a></td>");
                    printColumnCoverages(pw, cc, true, "");
                    // pw.println("<td class=\"reportValue\">"
                    //   + generatePercentResult(cc.getTotalCoverageString()) + "</td>");
                    pw.println("</tr>");
                }
            }
        } else {
            pkgList = coverage.getPackages();
            if (pkgList.size() > 0) {
                if (modules != null) {
                    generateModulesInfo(pw, modules, false);
                }
                pw.println("<span class=\"title2\">Packages</span><br>");
                pw.println("<table class=\"report\" cellpadding=\"0\" cellspacing=\"0\" id=\"packages\">");
                pw.println("<tr class=\"report\">");
                pw.println("<th class=\"report\">Name</th>");
                pw.println("<th class=\"report_number\">#classes</th>");
                printColumnHeaders(pw, "");
                // pw.println("<th class=\"report\">%total</th>");
                pw.println("</tr>");
                for (PackageCoverage pkg : pkgList) {
                    pw.println("<tr class=\"report\">");
                    pw
                            .println("<td class=\"reportText\"><a href=\""
                            + pkg.getName().replace('.', '/')
                            + "/package-summary.html\">" + pkg.getName()
                            + "</a></td>");
                    pw.println("<td class=\"reportValue_number\">"
                            + pkg.getData(DataType.CLASS).getTotal() + "</td>");
                    printColumnCoverages(pw, pkg, true, "");
                    //  pw.println("<td class=\"reportValue\">"
                    //    + generatePercentResult(pkg.getTotalCoverageString()) + "</td>");
                    pw.println("</tr>");
                }
            }
        }
        pw.println("</table>");
        if (thePackage != null && pkgList != null && pkgList.size() > 0) {
            // sub packages summary
            pw.println("<p>");
            pw.println("<span class=\"title2\">Total (including subpackages)</span><br>");
            pw.println("<table class=\"report\" cellpadding=\"0\" cellspacing=\"0\" id=\"subpackages\">");
            pw.println("<tr class=\"report\">");
            pw.println("<th class=\"report\">-</th>");
            pw.println("<th class=\"report_number\">#classes</th>");
            //pw.println("<th class=\"report\">%total</th>");
            printColumnHeaders(pw, "");
            pw.println("</tr>");
            pw.println("<tr class=\"report\">");
            pw.println("<td class=\"reportValue\"></td>");
            pw.println("<td class=\"reportValue_number\">"
                    + subCov.getData(DataType.CLASS).getTotal() + "</td>");
            //pw.println("<td class=\"reportValue\">"
            //    + decorate(subCov.getClassCoverageString()) + "</td>");
            printColumnCoverages(pw, subCov, true, "");
            pw.println("</table>");
            pw.println("<p>");

        }

        pw.println(generateFooter());
        pw.println("</body>");
        pw.println("</html>");
        pw.close();
    }

    private void generateModulesInfo(PrintWriter pw, HashMap<String, CoverageData[]> modules, boolean decorate){

        if (modules.keySet().size() > 1) {
            pw.println("<span class=\"title2\">Modules</span><br>");
        }
        else{
            pw.println("<span class=\"title2\">Module</span><br>");
        }
        pw.println("<table class=\"report\" cellpadding=\"0\" cellspacing=\"0\" id=\"modules\">");
        pw.println("<tr class=\"report\">");
        pw.println("<th class=\"report\">Name</th>");
        pw.println("<th class=\"report_number\">#classes</th>");
        printColumnHeaders(pw, "");
        pw.println("</tr>");

        for (String module : modules.keySet()) {
            pw.println("<tr class=\"report\">");
            if (!decorate) {
                pw.println("<td class=\"reportText\"><a href=\""
                        + module
                        + "/module-summary.html\">" + module
                        + "</a></td>");
            }
            else{
                pw.println("<td class=\"reportText\"> <b>" + module
                        + " </b> </a></td>");
            }

            pw.println("<td class=\"reportValue_number\">"
                    + modules.get(module)[0].getTotal() + "</td>");
            for (int i = 1; i< modules.get(module).length; i++) {
                CoverageData cd = modules.get(module)[i];
                if (!decorate) {
                    pw.println("<td class=\"reportValue\">"
                            + decorate(cd.getFormattedCoverage()) + "</td>");
                }
                else{
                    pw.println("<td class=\"reportValue\">"
                            +generatePercentResult(cd.getFormattedCoverage()) + "</td>");
                }
            }
            pw.println("</tr>");
        }
        pw.println("</table>");
        pw.println("<p>");
    }

    enum columns {

        method, field, block, branch, line
    };

    /**
     * Returns true if a column needs to be shown
     *
     * @param col
     * @return false, if the column should not appear in the report
     */
    boolean show(columns col) {
        switch (col) {
            case method:
                return showMethods;
            case field:
                return showFields;
            case block:
                return showBlocks;
            case branch:
                return showBranches;
            case line:
                return showLines;
        }
        return false; // should never occur
    }

    /**
     * Returns true if a column needs to be shown
     *
     * @param col
     * @return false, if the column should not appear in the report
     */
    String getColumnData(columns col, AbstractCoverage cc) {
        switch (col) {
            case method:
                return cc.getCoverageString(DataType.METHOD);
            case field:
                return cc.getCoverageString(DataType.FIELD);
            case block:
                return cc.getCoverageString(DataType.BLOCK);
            case branch:
                return cc.getCoverageString(DataType.BRANCH);
            case line:
                return cc.getCoverageString(DataType.LINE);
        }
        return "";
    }

    String getFormattedColumnData(columns col, AbstractCoverage cc, int testNumber) {
        switch (col) {
            case method:
                return cc.getData(DataType.METHOD, testNumber).getFormattedCoverage();
            case field:
                return cc.getData(DataType.FIELD, testNumber).getFormattedCoverage();
            case block:
                return cc.getData(DataType.BLOCK, testNumber).getFormattedCoverage();
            case branch:
                return cc.getData(DataType.BRANCH, testNumber).getFormattedCoverage();
            case line:
                return "";
        }
        return "";
    }

    void printColumnHeaders(PrintWriter pw, String pad) {
        for (columns col : columns.values()) {
            if (show(col)) {

                if (col != columns.line && testService != null && (isAddTestsInfo || isMergeRepGenMode)) {

                    int testNumber = 0;
                    Iterator<Test> iterator = testService.iterator();
                    while (iterator.hasNext()) {
                        Test test = iterator.next();
                        testNumber++;
                        pw.println("<th class=\"report\" title=\"" + test.getTestName() + "\">" + "#" + testNumber + "</th>");
                    }

                }

                pw.println(pad + "<th class=\"report\">%" + col + "</th>");
            }
        }
    }

    /**
     *
     * @param pw print writer
     * @param cov - class or package coverage
     * @param decorate - if true, <i>decorate()</i> method will be used,
     * <i>generatePercentResult()</i> - otherwise
     */
    void printColumnCoverages(PrintWriter pw, AbstractCoverage cov,
            boolean decorate, String pad) {

        for (columns col : columns.values()) {
            if (show(col)) {

                String testsData = "";
                if (testService != null && (isAddTestsInfo || isMergeRepGenMode)) {

                    for (int test = 0; test < testService.getTestCount(); test++) {

                        String columnData = getFormattedColumnData(col, cov, test);

                        if (mode != null
                                && mode.equals(InstrumentationOptions.InstrumentationMode.METHOD)
                                && col != columns.method
                                && !columnData.isEmpty()) {
                            columnData = "-";
                        }

                        if (!columnData.isEmpty()) {
                            if (!columnData.trim().equals("-")) {
                                testsData += "<td class=\"reportValue\">" + columnData + "</td>";
                            } else {
                                testsData += "<td class=\"reportValue\"><span class=\"text\"><center>" + columnData + "</center></span></td>";
                            }
                        }

                    }

                }

                String data = getColumnData(col, cov);

                if (mode != null
                        && mode.equals(InstrumentationOptions.InstrumentationMode.METHOD)
                        && col != columns.method) {
                    data = "-";
                }

                String dataToShow = decorate ? decorate(data)
                        : generatePercentResult(data);
                pw.println(pad + testsData + "<td class=\"reportValue\">"
                        + dataToShow + "</td>");
            }
        }
    }

    // temporarily workaround for CR 6523425
    private List<PackageCoverage> getSubPackages(PackageCoverage thePackage) {
        ArrayList<PackageCoverage> list = new ArrayList<PackageCoverage>();
        String name = thePackage.getName();
        for (PackageCoverage pn : coverage) {
            String pname = pn.getName();
            int idx = pname.indexOf(name);
            if (idx != -1 && !name.equals(pname)
                    && pname.replaceAll(name, "").startsWith(".")) {
                list.add(pn);
            }
        }
        return list;
    }

    private void generateSourceFiles(File dir) throws IOException {
        logger.info("generating source files...");
        List<PackageCoverage> pkglist = coverage.getPackages();
        int i = 0;
        for (PackageCoverage pkgcov : pkglist) {
            logger.log(Level.FINE, "package:{0}", pkgcov.getName());
            List<ClassCoverage> clslist = pkgcov.getClasses();
            for (ClassCoverage clscov : clslist) {
                ++i;
                if (clscov == null) {
                    logger.log(Level.SEVERE, "{0}clscov is NULL!", i);
                    continue;
                }
                logger.log(Level.INFO, "{0} {1}", new Object[]{i, clscov.getName()});
                generateSourceFile(dir, clscov);
            }
        }
    }

    private void generateSourceFile(File directory, ClassCoverage theClass)
            throws IOException {

        String srcOutputFilename = theClass.getFullClassName().replace('.', '/')
                + ".html";
        File srcOutputFile = new File(directory, srcOutputFilename);
        logger.log(Level.FINE, "srcOutputFile:{0}", srcOutputFile.getAbsolutePath());
        File dirOutputFile = srcOutputFile.getParentFile();
        if (dirOutputFile != null && !dirOutputFile.exists()) {
            logger.log(Level.INFO, "mkdirs:{0}", dirOutputFile);
            dirOutputFile.mkdirs();
        }

        PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                srcOutputFile), Charset.defaultCharset())));
        pw.println("<html>");
        pw.println("<head>");
        pw.println("<title>tests coverage</title>");
        String rootRef = getRelativePath(theClass.getPackageName());
        pw.println("<link rel =\"stylesheet\" type=\"text/css\" href=\"" + rootRef
                + "style.css\" title=\"Style\">");
        pw.println("<script type=\"text/javascript\" src=\"" + rootRef
                + "sorttable.js\"></script>");
        generateScriptsHeader(pw);
        pw.println("</head>");
        pw.println("<body>");
        if (!theClass.getPackageName().isEmpty()) {
            generateNavHeader(pw, theClass.getName() + ".html", "" + theClass.getPackageName().replaceAll("[a-zA-Z0-9]+", "..") + "/index.html?" + srcOutputFilename);
        }
        else{
            generateNavHeader(pw, theClass.getName() + ".html", "" + theClass.getName()+"/../index.html?" + srcOutputFilename);
        }
        //pw.println("<span class=\"title\">" + title + " "
        //  + coverage.getData(ColumnName.PRODUCT) + "</span>");
        if (isGenHitTests) {
            pw.println("<p>");
            pw.println("<a href=\"#hittests\">Hit Tests</a>");
        }
        pw.println("<p>");
        pw.println(" <table cellspacing=\"0\" cellpadding=\"0\"class=\"report\">");
        pw.println(" <tr class=\"report\">");
        pw.println(" <th class=\"report\">&nbsp;</th>");
        printColumnHeaders(pw, " ");
        // pw.println(" <th class=\"report\">%total</th>");
        pw.println(" </tr>");
        pw.println(" <tr class=\"report\">");
        pw.println(" <td class=\"reportText\"><span class=\"text\"> <b>"
                + theClass.getFullClassName() + "</b></span></td>");

        printColumnCoverages(pw, theClass, false, " ");
        //  pw.println(" <td class=\"reportValue\">"
        //      + generatePercentResult(theClass.getTotalCoverageString()) + "</td>");
        pw.println(" </tr>");
        pw.println(" </table>");
        pw.println(" <p>");


        String src = theClass.getSource();
        boolean isGenerate;
        File srcfile = null; // can be null
        if (src == null || theClass.isJavapSource()) {
            isGenerate = false;
        } else {
            srcfile = new File(src);
            isGenerate = srcfile.exists();
        }

        int mcount = theClass.getData(DataType.METHOD).getCovered();
        if (mcount == 0 && !isGenSrc4Zero) {
            isGenerate = false;
        }

        HashMap<Integer, MemberCoverage> methodsForLine = new HashMap<Integer, MemberCoverage>();
        HashMap<Integer, List<ItemCoverage>> itemsForLine = new HashMap<Integer, List<ItemCoverage>>();
        List<MethodCoverage> methodList = theClass.getMethods();

        //Collections.sort((List) methodList);

        for (MethodCoverage mcov : methodList) {
            methodsForLine.put(Integer.valueOf(mcov.getStartLine()), mcov);
            for (ItemCoverage icov : mcov.getItems()) {
                int srcLine = icov.getSourceLine();
                if (srcLine > 0) {
                    List<ItemCoverage> listIcov = itemsForLine.get(srcLine);
                    if (listIcov == null) {
                        listIcov = new LinkedList<ItemCoverage>();
                        itemsForLine.put(srcLine, listIcov);
                    }
                    listIcov.add(icov);
                }
            }
        }

        generateMemberTable(pw, "method", methodList, isGenerate, theClass.isJavapSource());

        List<FieldCoverage> fieldList = theClass.getFields();
        //Collections.sort((List) fieldList);

        if (showFields) {
            for (FieldCoverage fcov : fieldList) {
                int startLine = fcov.getStartLine();
                methodsForLine.put(new Integer(startLine), fcov);
                logger.log(Level.FINE, "{0}-{1}", new Object[]{fcov.getName(), startLine});
            }
            generateMemberTable(pw, "field", fieldList, isGenerate, theClass.isJavapSource());
        }

        if (isGenerate) {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(srcfile), Charset.defaultCharset()));
            String lineStr;
            int numLine = 1;

            pw.println(" <table cellspacing=\"0\" cellpadding=\"0\" class=\"src\">");
            while ((lineStr = br.readLine()) != null) {
                generateSourceLine(pw, lineStr, numLine, theClass, methodsForLine, itemsForLine, null);
                numLine++;
            }
            br.close();
        }

        if (theClass.isJavapSource()) {
            pw.println(" <table cellspacing=\"0\" cellpadding=\"0\" class=\"src\">");
            JavapClass javapClass = theClass.getJavapClass();

            if (javapClass != null) {
                String lineStr;
                int numLine = 1;

                methodsForLine = new HashMap<Integer, MemberCoverage>();
                for (MethodCoverage mcov : methodList) {
                    List<JavapLine> lines = javapClass.getMethod(mcov.getName() + mcov.getSignature());
                    if (lines != null) {
                        methodsForLine.put(lines.get(0).getLineNumber(), mcov);
                    }
                }

                for (JavapLine line : javapClass.getLines()) {
                    lineStr = line.getTextLine();
                    generateSourceLine(pw, lineStr, numLine, theClass, methodsForLine, itemsForLine, line);
                    numLine++;
                }
            }
        }

        pw.println(" </table>");
        pw.println("<p>");
        if (isGenHitTests) {
            pw.println(generateHitTests(theClass));
        }
        pw.println(generateFooter());
        pw.println("</body>");
        pw.println("</html>");
        pw.close();
    }

    private void generateSourceLine(PrintWriter pw, String lineStr, int numLine, ClassCoverage theClass,
            HashMap<Integer, MemberCoverage> methodsForLine, HashMap<Integer, List<ItemCoverage>> itemsForLine,
            JavapLine javapLine) {

        pw.println(" <tr>");
        if (javapLine == null) {
            MemberCoverage mcov = methodsForLine.get(numLine);
            List<ItemCoverage> items = itemsForLine.get(numLine);
            String lineCov = null;
            if (!theClass.isCode(numLine)) {
                lineCov = "numLine";
            } else {
                lineCov = theClass.isLineCovered(numLine) ? "numLineCover" : "numLineUnCover";
            }
            String link = "";

            if (mcov != null) {
                link = "<a name=\"src_" + numLine + "\"></a>";
            }

            if (items != null) {
                int allcovered = 0;
                Map<DataType, Integer> covered = new HashMap();
                Map<DataType, Integer> total = new HashMap();
                for (DataType kind : ItemCoverage.getAllPossibleTypes()) {
                    covered.put(kind, 0);
                    total.put(kind, 0);
                }
                for (ItemCoverage icov : items) {
                    DataType kind = icov.getDataType();
                    total.put(kind, total.get(kind) + 1);
                    if (icov.getCount() != 0) {
                        covered.put(kind, covered.get(kind) + 1);
                        allcovered++;
                    }
                }

                String shortInfo = "";
                for (DataType kind : ItemCoverage.getAllPossibleTypes()) {
                    if (total.get(kind) != 0) {
                        shortInfo += kind.getTitle() + ":&nbsp;" + covered.get(kind) + "/" + total.get(kind) + "&nbsp;";
                    }
                }
                boolean isGreen = items.size() == allcovered;

                String nbHitsCov = isGreen ? "nbHitsCovered" : "nbHitsUncovered";
                pw.println(" <td class=\"" + lineCov + "\">&nbsp;" + numLine + link
                        + "</td>");

                if (isMergeRepGenMode) {

                    StringBuilder totalInfo = new StringBuilder();
                    if (total.get(DataType.BLOCK) == null
                            || total.get(DataType.BLOCK) == 0) {
                        totalInfo.append("-:");
                    } else {
                        totalInfo.append(total.get(DataType.BLOCK)).append(":");
                    }
                    if (total.get(DataType.BRANCH) == null
                            || total.get(DataType.BRANCH) == 0) {
                        totalInfo.append("-");
                    } else {
                        totalInfo.append(total.get(DataType.BRANCH));
                    }

                    pw.println(" <td class=\"nbHits\" title=\"#blocks:#branches\" >&nbsp;" + totalInfo + " </td>");
                } else {
                    pw.println(" <td class=\"" + nbHitsCov + "\">&nbsp;" + shortInfo + "</td>");
                }

                if (testService != null && (isAddTestsInfo || isMergeRepGenMode)) {
                    int testNumber = 0;
                    Iterator<Test> iterator = testService.iterator();
                    while (iterator.hasNext()) {
                        Test test = iterator.next();
                        String testCovered = "numLineUnCover";
                        int block = 0;
                        int branch = 0;
                        boolean blocks = false;
                        boolean branches = false;
                        for (ItemCoverage item : itemsForLine.get(numLine)) {

                            if (!blocks && item.isBlock()) {
                                blocks = true;
                            }

                            if (!branches && !item.isBlock()) {
                                branches = true;
                            }

                            if (item.isCoveredByTest(testNumber)) {
                                testCovered = "numLineCover";
                                if (item.isBlock()) {
                                    block++;
                                } else {
                                    branch++;
                                }
                            }
                        }
                        testNumber++;

                        StringBuilder testInfo = new StringBuilder();
                        if (!blocks) {
                            testInfo.append("-:");
                        } else {
                            testInfo.append(block).append(":");
                        }
                        if (!branches) {
                            testInfo.append("-");
                        } else {
                            testInfo.append(branch);
                        }

                        pw.println(" <td " + "title=\"" + test.getTestName() + " (Block:Branch)\" class=\""
                                + testCovered + "\">&nbsp;" + testInfo + "</td>");
                    }
                }

                pw.println(" <td class=\"src\"><pre class=\"src\">&nbsp;"
                        + JavaToHtml.syntaxHighlight(lineStr) + "</span></pre></td>");

                // just string without any items
            } else {

                pw.println(" <td class=\"" + lineCov + "\">&nbsp;" + numLine + link + "</td>");
                pw.println(" <td class=\"nbHits\">&nbsp;</td>");
                if (testService != null && (isAddTestsInfo || isMergeRepGenMode)) {

                    for (int k = 0; k < testService.getTestCount(); k++) {

                        if (theClass.isCode(numLine)) {

                            String testCovered = "numLineUnCover";

                            if (theClass.isLineCovered(numLine)) {
                                for (int i = numLine; i >= 0; i--) {
                                    if (itemsForLine.get(i) != null) {
                                        for (ItemCoverage item : itemsForLine.get(i)) {
                                            if (item.isCoveredByTest(k)) {
                                                testCovered = "numLineCover";
                                            }
                                        }
                                        break;
                                    }
                                }

                            }

                            pw.println(" <td class=\"" + testCovered + "\">&nbsp;</td>");

                        } else {
                            pw.println(" <td class=\"nbHits\">&nbsp;</td>");
                        }

                    }

                }
                pw.println(" <td class=\"src\"><pre class=\"src\">&nbsp;"
                        + JavaToHtml.syntaxHighlight(lineStr) + "</pre></td>");
            }
        } else {

            //two lines are from the method string to first javap output (1 - method name and sig, 2 - "Code:")
            MemberCoverage mcov = methodsForLine.get(javapLine.getLineNumber() + 2);
            String link = "";

            if (mcov != null) {
                if (mcov.getName().equals("<clinit>")) {
                    link = "<a name=\"src_" + mcov.getStartLine() + "cl" + "\"></a>";
                } else {
                    link = "<a name=\"src_" + mcov.getStartLine() + "\"></a>";
                }
            }

            if (javapLine instanceof JavapCodeLine) {

                boolean isGreen = ((JavapCodeLine) javapLine).isVisited();

                String nbHitsCov = isGreen ? "nbHitsCovered" : "nbHitsUncovered";
                String htmlStr = javapLine.getTextLine().replaceAll("\\<", "&#60;").replaceAll("\\>", "&#62;");

                pw.println(" <td>" + numLine + link + "</td>");
                pw.println(" <td class=\"" + nbHitsCov + "\">&nbsp;" + "   " + "</td>");
                pw.println(" <td class=\"src\"><pre class=\"src\">&nbsp;"
                        + htmlStr + "</span></pre></td>");
            } else {
                pw.println(" <td>" + numLine + link + "</td>");
                pw.println(" <td class=\"nbHits\">&nbsp;</td>");
                pw.println(" <td class=\"src\"><pre class=\"src\">&nbsp;"
                        + JavaToHtml.syntaxHighlight(lineStr) + "</pre></td>");
            }

        }
        pw.println(" </tr>");

    }

    private void generateMemberTable(PrintWriter pw, String fieldOrMethod,
            List<? extends MemberCoverage> list, boolean isGenerate, boolean javapReport) {
        pw.println(" <p>");
        pw.println(" <table cellspacing=\"0\" cellpadding=\"0\"class=\"report\" id=\"mcoverage\">");
        pw.println(" <tr class=\"report\">");
        pw.println(" <th class=\"report\">hit count</th>");
        if (testService != null && (isAddTestsInfo || isMergeRepGenMode)) {
            for (int i = 0; i < testService.getTestCount(); i++) {
                pw.println(" <th class=\"report\"> #" + (i + 1) + "</th>");
            }
        }
        pw.println(" <th class=\"report\">" + fieldOrMethod + "  name</th>");
        pw.println(" <th class=\"report\">" + fieldOrMethod + " modifiers</th>");
        pw.println(" <th class=\"report\">" + fieldOrMethod + " signature</th>");
        pw.println(" </tr>");

        for (MemberCoverage mcov : list) {

            if (!isAnonymOn && mcov instanceof MethodCoverage) {
                if (((MethodCoverage) mcov).isInAnonymClass()) {
                    continue;
                }
            }

            pw.println(" <tr class=\"report\">");
            long c = mcov.getHitCount();

            if (c > 0) {
                pw.println(" <td class=\"reportValue_covered\"><span class=\"text\">"
                        + c + "</span></td>");
            } else {
                pw.println(" <td class=\"reportValue_uncovered\"><span class=\"text\">"
                        + c + "</span></td>");
            }

            if (testService != null && (isAddTestsInfo || isMergeRepGenMode)) {

                for (int i = 0; i < testService.getTestCount(); i++) {
                    if (mcov.getCoveringTests().contains(i)) {
                        pw.println(" <td class=\"numLineCover\"><span class=\"text\">"
                                + "<center>+</center>" + "</span></td>");
                    } else {
                        pw.println(" <td class=\"numLineUnCover\"><span class=\"text\">"
                                + "<center>-</center>" + "</span></td>");
                    }
                }
            }

            String mname = mcov.getName().replaceAll("<", "&lt;").replaceAll(
                    ">", "&gt;");

            if (isGenerate || javapReport) {
//fda            pw.println(" <td class=\"reportText\"><span class=\"text\"><a href=\"#"
//fda                + mcov.getSignature() + "\">" + mname + "</a></span></td>");

                if (javapReport && mcov.getName().equals("<clinit>")) {
                    pw.println(" <td class=\"reportText\"><span class=\"text\"><a href=\"#src_"
                            + mcov.getStartLine() + "cl" + "\">" + mname + "</a></span></td>");
                } else {
                    pw.println(" <td class=\"reportText\"><span class=\"text\"><a href=\"#src_"
                            + mcov.getStartLine() + "\">" + mname + "</a></span></td>");
                }

            } else {
                pw.println(" <td class=\"reportText\"><span class=\"text\">" + mname
                        + "</span></td>");
            }
            String mmodifiers = mcov.getModifiers();
            pw.println(" <td class=\"reportText\"><span class=\"text\">" + mmodifiers
                    + "</span></td>");
            pw.println(" <td class=\"reportText\"><span class=\"text\">"
                    + mcov.getReadableSignature().replaceAll("<", "&lt;").replaceAll(">",
                    "&gt;") + "</span></td>");
            pw.println(" </tr>");
        }

        pw.println(" </table>");
        pw.println(" <p>");

    }

    private static String constructShortDescr(List<ItemCoverage> items) {
        String str = "";
        return str;
    }
    // to compare two reports it's much more convenient to not include
    // date of generation
    private static final String REPORT_DATE = System.getProperty("test.mode") == null
            ? DateFormat.getInstance().format(new Date()) : "date";

    private void generateScriptsHeader(PrintWriter pw) {
        pw.println("<script type=\"text/javascript\">");
        pw.println("  targetPage = \"\" + window.location.search;");
        pw.println("  if (targetPage != \"\" && targetPage != \"undefined\")");
        pw.println("    targetPage = targetPage.substring(1);");
        pw.println("  if (targetPage.indexOf(\":\") != -1 || (targetPage != \"\" && !validURL(targetPage)))");
        pw.println("    targetPage = \"undefined\";");
        pw.println("  function validURL(url) {");
        pw.println("    var pos = url.indexOf(\".html\");");
        pw.println("    if (pos == -1 || pos != url.length - 5)");
        pw.println("      return false;");
        pw.println("    var allowNumber = false;");
        pw.println("    var allowSep = false;");
        pw.println("    var seenDot = false;");
        pw.println("    for (var i = 0; i < url.length - 5; i++) {");
        pw.println("      var ch = url.charAt(i);");
        pw.println("      if ('a' <= ch && ch <= 'z' ||");
        pw.println("          'A' <= ch && ch <= 'Z' ||");
        pw.println("          ch == '$' ||");
        pw.println("          ch == '_') {");
        pw.println("            allowNumber = true;");
        pw.println("            allowSep = true;");
        pw.println("      } else if ('0' <= ch && ch <= '9' ||");
        pw.println("                 ch == '-') {");
        pw.println("                   if (!allowNumber)");
        pw.println("                     return false;");
        pw.println("      } else if (ch == '/' || ch == '.') {");
        pw.println("        if (!allowSep)");
        pw.println("          return false;");
        pw.println("        allowNumber = false;");
        pw.println("        allowSep = false;");
        pw.println("        if (ch == '.')");
        pw.println("          seenDot = true;");
        pw.println("        if (ch == '/' && seenDot)");
        pw.println("          return false;");
        pw.println("      } else {");
        pw.println("        return false;");
        pw.println("      }");
        pw.println("    }");
        pw.println("    return true;");
        pw.println("  }");
        pw.println("  function loadFrames() {");
        pw.println("    if (targetPage != \"\" && targetPage != \"undefined\")");
        pw.println("      top.classFrame.location = top.targetPage;");
        pw.println("  }");
        pw.println("</script>");
    }

    private void generateNavHeader(PrintWriter pw, String noframesPath, String index_classPath) {
        pw.println("<table>");
        pw.println("<tr>");
        pw.println("<td>");
        pw.println("<a href=\"" + index_classPath + "\" target=\"_top\">Frames</a>");
        pw.println("<a href=\"" + noframesPath + "\" target=\"_top\">No Frames</a>");
        pw.println("</td>");
        pw.println("</tr>");
        pw.println("</table>");
    }

    private String generateFooter() {
        StringBuilder sb = new StringBuilder();
        sb.append("<p>");
        sb.append("<table cellpadding=\"0\" cellspacing=\"0\" class=\"report\">");
        sb.append("  <tr class=\"report\">");
        sb.append("    <td class=\"reportText\"><span class=\"text\">");
        sb.append("    Report generated ").append(REPORT_DATE);
        sb.append("    </span></td>");
        sb.append("  </tr>");
        sb.append("</table>");
        return sb.toString();
    }

    private String generateHitTests(ClassCoverage n) {
        StringBuilder sb = new StringBuilder();
        if (n != null) {
            try {
                sb
                        .append("<a name=\"hittests\"><span class=\"title\">Hit tests</span></a>");
                sb
                        .append("<table cellpadding=\"0\" cellspacing=\"0\" class=\"report\">");
                sb.append(" <tr class=\"report\">");
                sb.append(" <th class=\"report\">#</th>");
                // sb.append(" <th class=\"report\">owner</th>");
                sb.append(" <th class=\"report\">Test name</th>");
                sb.append(" </tr>");
                List<Test> hitlist = testService != null
                        ? testService.getHitTestByClasses(n)
                        : new ArrayList<Test>();
                if (isAddTestsInfo || isMergeRepGenMode){
                    hitlist = testService.getAllTests();
                }

                int i = 1;
                for (Test httest : hitlist) {
                    String owner = httest.getTestOwner();
                    String tname = httest.getTestName();
                    sb.append(" <tr class=\"report\">");
                    sb.append(" <td class=\"reportValue\">").append(i++).append("</td>");
                    // sb.append(" <td class=\"reportText\">" + owner + "</td>");
                    StringBuilder append = sb.append(" <td class=\"reportText\">").append(tname).append("</td>");

                    sb.append("  </tr>");
                }
                sb.append("</table>");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    private String generatePercentResult(String percentValue) {
        String value = percentValue;
        String cov_total = "";
        int idx = value.indexOf("%");
        if (idx != -1) {
            value = value.substring(0, idx);
            cov_total = percentValue.substring(idx + 1);
        }

        double rest = 0;
        boolean badNumber = false;
        try {
            rest = 100d - new Double(value.replace(',', '.')).doubleValue();
        } catch (NumberFormatException e) {
            badNumber = true;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<table cellpadding=\"0\" cellspacing=\"0\" align=\"center\">");
        sb.append("<tr>");
        // sb.append("<td><span class=\"text\">" + decorate(percentValue)
        sb.append("<td><span class=\"text\"><b>").append(value.trim()).append("</b>%").append(cov_total.trim()).append("</span></td>");
        if (!badNumber) {
            sb.append("<tr>");
            sb.append("<td>");
            sb.append("<table class=\"percentGraph\" cellpadding=\"0\" cellspacing=\"0\">");
            sb.append("<tr>");
            sb.append("<td class=\"percentCovered\" width=\"").append(value).append("\"></td>");
            sb.append("<td class=\"percentUnCovered\" width=\"").append(String.valueOf(rest)).append("\"></td>");
            sb.append("</tr>");
            sb.append("</table>");
            sb.append("</td>");
            sb.append("</tr>");
        }
        sb.append("</tr>");
        sb.append("</table>");
        return sb.toString();
    }

    private String getRelativePath(String path) {
        if (path != null && !path.equals("")) {
            // java.awt.event - ../../../
            return path.replace('.', '/').replaceAll("\\w+", "..") + "/";
        } else {
            return "";
        }
    }

    private String decorate(String coverageString) {
        int idx = coverageString.indexOf("%");
        if (idx != -1) {
            StringBuilder sb = new StringBuilder();
            sb.append("<table class=\"report\">");
                sb.append("<tr>");
                sb.append("<td class=\"coverage_left\">");
                    sb.append("<span class=\"text_bold\">");
                        sb.append(coverageString.substring(0, idx+1).trim());
                    sb.append("</span>");
                sb.append("</td>");
                sb.append("<td class=\"coverage_right\"> ");
                    sb.append("<span class=\"text\">");
                        sb.append(coverageString.substring(idx+1).trim());
                    sb.append("</span>");
                sb.append("</td>");
                sb.append("</tr>");
            sb.append("</table>");
            return sb.toString();
        }
        return coverageString;
    }

    public void setShowMethods(boolean showMethods) {
        this.showMethods = showMethods;
    }

    public void setShowBlocks(boolean showBlocks) {
        this.showBlocks = showBlocks;
    }

    public void setShowBranches(boolean showBranches) {
        this.showBranches = showBranches;
    }

    public void setShowFields(boolean showFields) {
        this.showFields = showFields;
    }

    public void setShowLines(boolean showLines) {
        this.showLines = showLines;
    }
}