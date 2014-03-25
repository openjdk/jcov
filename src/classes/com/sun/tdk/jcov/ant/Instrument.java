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

import com.sun.tdk.jcov.Instr;
import com.sun.tdk.jcov.instrument.InstrumentationOptions;
import com.sun.tdk.jcov.runtime.PropertyFinder;
import com.sun.tdk.jcov.tools.OptionDescr;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.resources.FileResource;

/**
 *
 * @author Andrey Titov
 */
public class Instrument extends Task {

    private File destdir;
    private File productDir;
    private List<BinPath> implantTo = new LinkedList<BinPath>();
    private List<FileSet> files = new LinkedList<FileSet>();
    private PatternSet filter;
    private LinkedList<SavePoint> saveBegin = new LinkedList<SavePoint>();
    private LinkedList<SavePoint> saveEnd = new LinkedList<SavePoint>();
    private boolean verbose;
    private File template;
    private File outtemplate;
    private boolean showabstract = false;
    private boolean shownative = false;
    private boolean showfields = false;
    public File propfile = null;

//    private String type; // not used (MERGE now)
//    private String flushClasses; // not wrapped
    public Instrument() {
    }

    @Override
    public void execute() throws BuildException {
        File oldBase = null;

        if (propfile != null) {
            PropertyFinder.setPropertiesFile(propfile.getPath());
        }

        if (destdir != null) {
            if (destdir.exists() && !destdir.isDirectory()) {
                throw new BuildException("Output path is a file");
            }
            if (this.productDir != null) {
                if (!this.productDir.exists()) {
                    throw new BuildException("Project directory + " + this.productDir + " doesn't exist");
                }
                if (!this.productDir.isDirectory()) {
                    throw new BuildException("Project path " + this.productDir + " is not a directory");
                }
                if (!destdir.exists()) {
                    throw new BuildException("Output directory " + destdir + " doesn't exist");
                }

                copyProduct();
                log("Product mode: working in " + destdir + "...", 3);
                oldBase = convertPaths();
            }
        } else if (this.productDir != null) {
            throw new BuildException("Destdir needed in project mode");
        }

        String[] paths = getUniquePaths();
        if (implantTo.isEmpty() && paths.length < 1) {
            log("Warning: no binaries found to instrument", 2);
        }

        log("Creating instrumentator", 4);
        Instr instr = new Instr();
        log("Reseting instrumentator to defaults", 3);
        instr.resetDefaults();

        log("Configuring insturumentator", 4);
        applyFilter(filter, instr);

        if (template != null) {
            instr.setTemplate(template.getPath());
        } else {
            instr.setTemplate("template.xml");
        }
        instr.setVerbose(verbose);

        String[] saveBeg = null;
        int size = saveBegin.size();
        int i = 0;
        if (size > 0) {
            saveBeg = new String[size];
            i = 0;
            for (SavePoint sp : saveBegin) {
                saveBeg[i] = sp.name;
            }
        }
        String[] saveE = null;
        size = saveEnd.size();
        if (size > 0) {
            saveE = new String[size];
            i = 0;
            for (SavePoint sp : saveEnd) {
                saveE[i] = sp.name;
            }
        }

        String defValue = showabstract ? OptionDescr.ON : OptionDescr.OFF;
        showabstract = OptionDescr.ON.equals(PropertyFinder.findValue(InstrumentationOptions.DSC_ABSTRACT.name, defValue));

        defValue = showfields ? OptionDescr.ON : OptionDescr.OFF;
        showfields = OptionDescr.ON.equals(PropertyFinder.findValue(InstrumentationOptions.DSC_FIELD.name, defValue));

        defValue = shownative ? OptionDescr.ON : OptionDescr.OFF;
        shownative = OptionDescr.ON.equals(PropertyFinder.findValue(InstrumentationOptions.DSC_NATIVE.name, defValue));

        instr.config(showabstract, showfields, shownative, saveBeg, saveE);
        log("Insturumentator configured", 4);

        try {
            instr.startWorking();
            log("Instrumentation started", 4);
            log(String.format("Parameters: filter incl %s excl %s; template %s; outtemplate %s; save_beg %s save_end %s; abstract %s fields %s native %s",
                    Arrays.toString(incl), Arrays.toString(excl), template, outtemplate, Arrays.toString(saveBeg), Arrays.toString(saveE), showabstract, showfields, shownative), 3);

            for (BinPath path : implantTo) {
                File output = (productDir == null ? destdir : null);

                if (path.implantRT == null) {
                    log("Implanting runtime file was not found for binary " + path.path + ". Please use <files> if you want to instrument single file without implanting runtime binaries.", 2);
                    throw new BuildException("Cannot implant runtime data");
                }

                log(path.getInstrumentingLog(output), 2);

                instr.instrumentFile(path.path.getPath(), output, path.implantRT.getPath());
            }
            log("<implantTo> instrumentation complete", 4);

            if (paths != null && paths.length > 0) {
                File output = (productDir == null ? destdir : null);
                log("Instrumenting paths " + Arrays.toString(paths), 3);
                for (FileSet fs : files) {
                    File root = fs.getDir();
                    Iterator it = fs.iterator();
                    while (it.hasNext()) {
                        FileResource file = (FileResource) it.next();
                        if (!implantToContains(file.getFile())) {
                            instr.instrumentFile(root.getPath() + File.separator + file.getName(), new File(output, file.getName()), null);
                        }
                    }
                }

//                for (String s : paths) {
//                    instr.instrumentFile(s, output, null);
//                }
            }
            log("<files> instrumentation complete", 4);

            if (productDir != null) {
                log("Resetting basedir", 3);
                getProject().setBaseDir(oldBase);
            }

            if (outtemplate != null) {
                log("Writing result template to " + outtemplate, 2);
                instr.finishWork(outtemplate.getPath());
            } else {
                log("Writing result template to template.xml", 2);
                instr.finishWork();
            }

            log("Instrumentation finished", 3);
        } catch (BuildException e) {
            throw e;
        } catch (Exception e) {
            log("Instrumentation task failed: ", e, 1);
            e.printStackTrace();
//            e.printStackTrace();
            throw new BuildException("Instrumentation task failed: " + e.getMessage(), e);
        }

        if (propfile != null) {
            PropertyFinder.cleanProperties();
        }
    }

    private void copyProduct() {
        Copy copy = new Copy();
        copy.setProject(getProject());
        copy.setTaskName("instrument");
        FileSet fileSet = new FileSet();
        fileSet.setDir(productDir);
        copy.addFileset(fileSet);
        copy.setTodir(destdir);
        copy.perform();
    }

    /**
     * Converts all paths that should be converted to destdir (enables product
     * mode)
     *
     * @return old BaseDir file object
     */
    private File convertPaths() {
        File oldBase = getProject().getBaseDir();
        getProject().setBaseDir(productDir);
        String proj = destdir.getPath();
        if (files != null) {
            for (FileSet fs : files) {
                fs.setDir(new File(proj + fs.getDir().getPath().substring(oldBase.getPath().length())));
            }
        }
        return oldBase;
    }

//    public void setType(String type) {
//        com.sun.tdk.jcov.instrument.Options.setInstrumentationType(type);
//    }
    public void setVerbose(Boolean verbose) throws Exception {
        this.verbose = verbose;
    }

    public void setTemplate(File template) {
        if (!template.exists()) {
            throw new BuildException("Template " + template + " doesn't exist");
        }
        if (!template.isFile()) {
            throw new BuildException("Template " + template + " is not a file");
        }
        this.template = template;
    }

    public void setOutTemplate(File outtemplate) {
        if (outtemplate.exists()) {
            if (outtemplate.isDirectory()) {
                this.outtemplate = new File(outtemplate, "template.xml");
                log("Warning, path for template " + outtemplate + " is directory, writing to " + this.outtemplate, 3);
                return;
            }
            throw new BuildException("Template " + outtemplate + " exist");
        } else {
            this.outtemplate = outtemplate;
        }
    }

    public void setDestdir(File destdir) {
        this.destdir = destdir;
    }

    public void setProductDir(File project) {
        this.productDir = project;
    }

    public PatternSet createFilter() throws Exception {
        if (filter != null) {
            throw new Exception("filter should be only one");
        }
        filter = new PatternSet();
        return filter;
    }

    public SavePoint createSaveBegin() throws Exception {
        SavePoint sp = new SavePoint();
        saveBegin.add(sp);
        return sp;
    }

    public SavePoint createSaveEnd() throws Exception {
        SavePoint sp = new SavePoint();
        saveEnd.add(sp);
        return sp;
    }

    public static class SavePoint {

        private String name;

        public void setName(String name) {
            this.name = name;
        }
    }

    public BinPath createImplantTo() {
        BinPath path = new BinPath();
        implantTo.add(path);
        return path;
    }

    public class BinPath {

        protected File path;
        protected File implantRT;

        public void setPath(File path) throws Exception {
            if (destdir != null && productDir != null) {
                String proj = destdir.getPath();
                path = new File(proj + path.getPath().substring(getProject().getBaseDir().getPath().length()));
            } else {
                if (!path.exists()) {
                    throw new BuildException("Binary root " + path + " doesn't exist");
                }
            }
            this.path = path;
        }

        public void setImplantRT(File implantRT) {
            if (!implantRT.exists()) {
                throw new BuildException("Runtime file " + implantRT + " doesn't exist");
            }
            if (!implantRT.isFile() || !implantRT.getName().endsWith("jar")) {
                throw new BuildException("Runtime file " + implantRT + " is not a jar file");
            }
            this.implantRT = implantRT;
        }

        public String getInstrumentingLog(File output) {
            if (output != null) {
                return String.format("Instrumenting classfiles dir %s to %s with implanted %s", path.getPath(), output, implantRT);
            } else {
                return String.format("Instrumenting classfiles dir %s with implanted %s", path.getPath(), implantRT);
            }
        }
    }

    public FileSet createFiles() {
        FileSet fs = new FileSet();
        files.add(fs);
        return fs;
    }

    /**
     * Filters out from "files" fileset all entries that were intruduced in
     * "inplantTo"
     *
     * @return filtered paths array
     */
    private String[] getUniquePaths() {
        if (files == null || files.isEmpty()) {
            return null;
        }
        HashSet<String> pathsSet = new HashSet<String>();
        for (FileSet fs : files) {
            fs.getDir();
            Iterator it = fs.iterator();
            while (it.hasNext()) {
                Object o = it.next();
                if (o instanceof FileResource) {
                    FileResource fr = (FileResource) o;
                    File file = fr.getFile();
                    if (!implantToContains(file)) {
                        pathsSet.add(file.getPath());
                    }
                }
            }
        }
        return pathsSet.toArray(new String[pathsSet.size()]);
    }

    private boolean implantToContains(File file) {
        for (BinPath bp : implantTo) {
            if (bp.path.equals(file)) {
                return true;
            }
        }
        return false;
    }
    String[] incl;
    String[] excl;

    private void applyFilter(PatternSet filter, Instr instr) {
        incl = instr.getInclude();
        excl = instr.getExclude();
        if (filter != null) {
            incl = filter.getIncludePatterns(getProject());
            excl = filter.getExcludePatterns(getProject());
            instr.setFilter(incl, excl);
        }
    }

    public void setNative(boolean shownative) {
        this.shownative = shownative;
    }

    public void setFields(boolean showfields) {
        this.showfields = showfields;
    }

    public void setAbstract(boolean showabstract) {
        this.showabstract = showabstract;
    }

    public void setPropfile(File path) {
        this.propfile = path;
    }
}
