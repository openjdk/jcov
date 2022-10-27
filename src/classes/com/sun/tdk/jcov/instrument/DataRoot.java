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
package com.sun.tdk.jcov.instrument;

import com.sun.tdk.jcov.data.FileFormatException;
import com.sun.tdk.jcov.data.ScaleOptions;
import com.sun.tdk.jcov.filter.MemberFilter;
import com.sun.tdk.jcov.instrument.reader.ReaderFactory;
import com.sun.tdk.jcov.instrument.reader.RootReader;
import com.sun.tdk.jcov.io.Reader;
import com.sun.tdk.jcov.runtime.Collect;
import com.sun.tdk.jcov.runtime.JCovXMLFileSaver;
import com.sun.tdk.jcov.tools.JcovVersion;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p> DataRoot is the root for all coverage data. It contains information about
 * covered product hierarchy - all packages, classes, methods and blocks could
 * be retrieved from DataRoot. </p> <p> It's also used for writing and reading
 * XML files. Use DataRoot.read() method to read JCov data from XML file and
 * DataRoot.write() method to write an XML file. </p>
 *
 * @see DataRoot#read(java.lang.String)
 * @see DataRoot#read(java.lang.String, boolean,
 * com.sun.tdk.jcov.filter.MemberFilter)
 * @see DataRoot#write(java.lang.String,
 * com.sun.tdk.jcov.instrument.InstrumentationOptions.MERGE)
 * @author Robert Field
 * @author Sergey Borodin
 * @author Dmitry Fazunenko
 * @author Andrey Titov
 */
public class DataRoot extends DataAbstract {

    /**
     * Total slot count
     */
    private int count;
    /**
     * Coverage generator args (used for Agent mostly)
     */
    private String args;
    /**
     * XML file containing this DataRoot
     */
    private String storageFileName;
    /**
     * True when this DataRoot is synchronized with Collect class. <br/> This
     * means that this DataRoot is getting hit counts from Collect class and not
     * from inside storage.
     */
    private boolean attached = false;
    /**
     * Packages registered in this DataRoot
     */
    private final Map<String, DataPackage> packages;
    /**
     * Scales information
     */
    protected ScaleOptions scaleOpts = new ScaleOptions();
    /**
     * Instrumentation information
     */
    protected InstrumentationParams params;
    /**
     * All instances of DataRoots (for attaching&merging capabilities)
     */
    private static Map<Integer, DataRoot> instances = new HashMap();
    private static volatile int instanceCount = 0;
    private List<Integer> secondaryIDs = new LinkedList<Integer>();
    /**
     * Acceptor API
     */
    protected MemberFilter acceptor;
    /**
     * Reading API
     */
    protected ReaderFactory rf;
    private TreeMap<String, String> props;
    private static final Logger logger;

    static {
        logger = Logger.getLogger(DataRoot.class.getName());
    }

    /**
     * Every DataRoot has it's own ID and is stored in static library.
     * <br/><br/>
     *
     * To remove DataRoot use destroy()
     *
     * @param i DataRoot ID
     * @return Find the DataRoot by ID
     * @see #destroy()
     */
    public static DataRoot getInstance(int i) {
        return instances.get(i);
    }

    public DataRoot() {
        this("", true, null);
    }

    /**
     * <p> Create empty DataRoot. </p> <p> Every DataRoot has it's own ID and is
     * stored in static library. </p> <p> To remove DataRoot use destroy() </p>
     * <p> To load DataRoot from a file use read() method </p>
     *
     * @param params Parameters this coverage DataRoot was collected with
     * @see #destroy()
     * @see #read(java.lang.String)
     * @see #read(java.lang.String, boolean,
     * com.sun.tdk.jcov.filter.MemberFilter)
     */
    public DataRoot(InstrumentationParams params) {
        this("", true, params);
    }

    /**
     * Creates a new instance of DataRoot<br/><br/>
     *
     * Every DataRoot has it's own ID and is stored in static library.
     * <br/><br/>
     *
     * To remove DataRoot use destroy() <p> To load DataRoot from a file use
     * read() method </p>
     *
     * @param args Command line arguments used to collect this coverage. Mostly
     * used with Agent
     * @param params Parameters this coverage DataRoot was collected with
     * @see #destroy()
     * @see #read(java.lang.String)
     * @see #read(java.lang.String, boolean,
     * com.sun.tdk.jcov.filter.MemberFilter)
     */
    public DataRoot(String args, InstrumentationParams params) {
        this(args, true, params);
    }

    /**
     * Creates empty DataRoot object. Take care of initializing
     * InstrumentationParams (e.g. in XML reading)<br/><br/>
     *
     * Every DataRoot has it's own ID and is stored in static library.
     * <br/><br/>
     *
     * To remove DataRoot use destroy() <p> To load DataRoot from a file use
     * read() method </p>
     *
     * @param args Command line arguments used to collect this coverage. Mostly
     * used with Agent
     * @param attached An attached DataRoot is synchronized with Collect class
     * so that it gets (and stores) hit count directly from Collect
     * @see #destroy()
     * @see #read(java.lang.String)
     * @see #read(java.lang.String, boolean,
     * com.sun.tdk.jcov.filter.MemberFilter)
     */
    public DataRoot(String args, boolean attached) {
        super(instanceCount++);
        instances.put(rootId, this);

        this.args = args;
        this.packages = new HashMap<String, DataPackage>();
        this.attached = attached;
        this.props = new TreeMap<String, String>();
    }

    /**
     * Creates empty DataRoot object.<br/><br/>
     *
     * Every DataRoot has it's own ID and is stored in static library.
     * <br/><br/>
     *
     * To remove DataRoot use destroy()
     *
     * @param args Command line arguments used to collect this coverage. Mostly
     * used with Agent
     * @param attached An attached DataRoot is synchronized with Collect class
     * so that it gets (and stores) hit count directly from Collect
     * @param params Parameters this coverage DataRoot was collected with
     * @see #destroy()
     */
    public DataRoot(String args, boolean attached, InstrumentationParams params) {
        this(args, attached);
        this.params = params;
    }

    /**
     * Removes this DataRoots from the instances<br/><br/>
     *
     * Every DataRoot has it's own ID and is stored in static library.
     */
    public void destroy() {
        if (rootId == -1) {
            return; // already removed
        }
        instances.remove(rootId);
        for (int i : secondaryIDs) {
            instances.remove(i);
        }
        rootId = -1;
    }

    /**
     * @return Command line arguments used to collect this coverage. Mostly used
     * with Agent
     */
    public String getArgs() {
        return args;
    }

    /**
     * Set command line arguments used to collect this coverage. Mostly used
     * with Agent
     *
     * @param args command line arguments used to collect this coverage
     */
    public void setArgs(String args) {
        this.args = args;
    }

    /**
     * Get slot count. If this DataRoot is attached - data is taken directly
     * from Collect
     *
     * @return the count of slots
     */
    public int getCount() {
        if (!attached) {
            return count;
        } else {
            return Collect.slotCount();
        }
    }

    /**
     * Set slot count. <br/> Doesn't care about attached status.
     *
     * @param count
     */
    public void setCount(int count) {
        this.count = count;
    }

    /**
     * Set the options of scales
     *
     * @param scaleOpts new options of scales
     */
    public void setScaleOpts(ScaleOptions scaleOpts) {
        this.scaleOpts = scaleOpts;
    }

    /**
     * @return the options of scales
     */
    public ScaleOptions getScaleOpts() {
        return scaleOpts;
    }

    /**
     * Set the filename of XML file storing this DataRoot
     *
     * @param fileName XML file storing this DataRoot
     */
    public void setStorageFileName(String fileName) {
        storageFileName = fileName;
    }

    /**
     * @return the filename of XML file storing this DataRoot
     */
    public String getStorageFileName() {
        return storageFileName;
    }

    /**
     * Set the parameters of this DataRoot
     *
     * @param params Parameters this coverage DataRoot was collected with
     */
    public void setParams(InstrumentationParams params) {
        this.params = params;
    }

    /**
     * Get the parameters of this DataRoot
     *
     * @return Parameters this coverage DataRoot was collected with
     */
    public InstrumentationParams getParams() {
        return params;
    }

    /**
     * Attached means that this DataRoot is synchronized with Collect object.
     * <br/> This means that hit counts are read from Collect and not from
     * DataRoot hierarchy
     *
     * @return true when attached
     */
    public boolean isAttached() {
        return attached;
    }

    /**
     * Acceptor is used when adding a class to the DataRoot
     *
     * @param acceptor acceptor for this DataRoot
     */
    public void setAcceptor(MemberFilter acceptor) {
        this.acceptor = acceptor;
    }

    /**
     * Acceptor is used when adding a class to the DataRoot
     *
     * @return acceptor for this DataRoot
     */
    public MemberFilter getAcceptor() {
        return acceptor;
    }

    /**
     * Set reader factory used to read the XML. Not supposed to be used outside.
     *
     * @param rf reader factory (StAX or JAXB)
     */
    public void setReaderFactory(ReaderFactory rf) {
        this.rf = rf;
    }

    /**
     * Get reader factory used to read the XML. Not supposed to be used outside.
     *
     * @return reader factory (StAX or JAXB)
     */
    public ReaderFactory getReaderFactory() {
        return rf;
    }

    /**
     * Get packages registered in this DataRoot. Packages are not stored
     * hierarchically
     *
     * @return All packages map
     * @see #packages
     */
    public List<DataPackage> getPackages() {
        ArrayList<DataPackage> packs = new ArrayList<DataPackage>(packages.size());
        packs.addAll(packages.values());
        return packs;
    }

    /**
     * Find package by name. Creates new DataPackage instance if the package
     * doesn't exist
     *
     * @param name
     * @return package
     */
    public DataPackage findPackage(String name) {
        return findPackage(name, XmlNames.NO_MODULE);
    }

    /**
     * Find package by name and module name. Creates new DataPackage instance if the package
     * doesn't exist
     *
     * @param name
     * @return package
     */
    public DataPackage findPackage(String name, String moduleName) {
        DataPackage pack = packages.get(name);
        if (pack == null) {
            pack = new DataPackage(rootId, name, moduleName);
            packages.put(pack.getName(), pack);
        }
        return pack;
    }

    /**
     * Find class by full classname (eg "foo/bar/MyClass")
     *
     * @param classname
     * @return DataClass representing <code>classname</code> or null if not
     * found
     */
    public DataClass findClass(String classname) {
        int i = classname.lastIndexOf('/');
        String packname;
        if (i > 0) {
            packname = classname.substring(0, i);
            classname = classname.substring(i);
        } else {
            packname = ROOT_PACKAGE;
        }

        DataPackage pack = packages.get(packname);
        if (pack == null) {
            return null;
        }

        return pack.findClass(classname);
    }

    /**
     * Find class by full classname (eg "foo/bar/MyClass")
     *
     * @param classname
     * @return package containing <code>classname</code> or null if not found
     */
    public DataPackage findPackageForClass(String classname) {
        int i = classname.lastIndexOf('/');
        if (i > 0) {
            classname = classname.substring(0, i);
        } else {
            classname = ROOT_PACKAGE;
        }
        return packages.get(classname);
    }

    /**
     * Add a class to the DataRoot.
     *
     * @param clazz
     */
    public void addClass(DataClass clazz) {
        if (acceptor != null && !acceptor.accept(clazz)) {
            return;
        }

        int slash = clazz.getFullname().lastIndexOf('/');
        String pname =
                (slash < 0)
                ? ""
                : clazz.getFullname().substring(0, slash);
        this.findPackage(pname, clazz.getModuleName()).addClass(clazz);
    }

    /**
     * Add package info to this JCov data. It's also possible to add package
     * with
     * <code>findPackage()</code> method
     *
     * @param pack
     * @see #findPackage(java.lang.String, java.lang.String)
     */
    public void addPackage(DataPackage pack) {
        packages.put(pack.getName(), pack);
    }

    /**
     * @return list of all classes registered in this JCov data
     */
    public List<DataClass> getClasses() {
        LinkedList<DataClass> classes = new LinkedList<DataClass>();
        for (DataPackage dp : packages.values()) {
            classes.addAll(dp.getClasses());
        }
        return classes;
    }

    /**
     * XML Generation. Not supposed to use outside.
     */
    public String kind() {
        return XmlNames.COVERAGE;
    }

    /**
     * XML Generation
     */
    @Override
    public void xmlGen(XmlContext ctx) {
        ctx.println("<?xml version='1.0' encoding='UTF-8'?>");
        ctx.println();
        super.xmlGen(ctx);
    }

    /**
     * XML Generation
     */
    @Override
    void xmlAttrs(XmlContext ctx) {
        ctx.println();
        ctx.println("        xmlns='http://java.sun.com/jcov/namespace'");
        ctx.println("        xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'");
        ctx.print("        xsi:schemaLocation='http://java.sun.com/jcov/namespace coverage.xsd'");
    }

    /**
     * XML Generation
     */
    @Override
    void xmlBody(XmlContext ctx) {
        xmlHead(ctx);
        List<DataPackage> packList = new ArrayList<DataPackage>(packages.values());
        Collections.sort(packList);
        for (DataPackage pack : packList) {
            pack.xmlGen(ctx);
        }
    }

    /**
     * XML Generation
     */
    private void xmlHead(XmlContext ctx) {
        if (attached) {
            updateHead();
        }

        TimeZone gmt = TimeZone.getTimeZone("GMT");
        Calendar calendar = new GregorianCalendar(gmt);
        Date now = new Date();
        calendar.setTime(now);

        ctx.println();
        ctx.indentPrintln("<" + XmlNames.HEAD + ">");
        ctx.incIndent();
        props.put("coverage.created.date", String.format(LOCALE_ROOT, "%tF", calendar));
        props.put("coverage.created.time", String.format(LOCALE_ROOT, "%tT", calendar));
        props.put("coverage.generator.name", "jcov");
        props.put("coverage.generator.version", JcovVersion.jcovVersion);
        props.put("coverage.generator.fullversion", JcovVersion.getJcovVersion());
        props.put("coverage.spec.version", "1.3");
        ctx.indentPrintln("<property " + XmlNames.NAME + "='coverage.generator.args' " + XmlNames.VALUE + "='" + args + "'/>");
        ctx.indentPrintln("<property " + XmlNames.NAME + "='coverage.generator.mode' " + XmlNames.VALUE + "='" + params.getMode() + "'/>");
        ctx.indentPrintln("<property " + XmlNames.NAME + "='coverage.generator.internal' " + XmlNames.VALUE + "='" + (params.isDetectInternal() ? "detect" : "include") + "'/>");
        ctx.indentPrintln("<property " + XmlNames.NAME + "='coverage.generator.include' " + XmlNames.VALUE + "='" + InstrumentationOptions.concatRegexps(params.getIncludes()) + "'/>");
        ctx.indentPrintln("<property " + XmlNames.NAME + "='coverage.generator.exclude' " + XmlNames.VALUE + "='" + InstrumentationOptions.concatRegexps(params.getExcludes()) + "'/>");
        ctx.indentPrintln("<property " + XmlNames.NAME + "='coverage.generator.caller_include' " + XmlNames.VALUE + "='" + InstrumentationOptions.concatRegexps(params.getCallerIncludes()) + "'/>");
        ctx.indentPrintln("<property " + XmlNames.NAME + "='coverage.generator.caller_exclude' " + XmlNames.VALUE + "='" + InstrumentationOptions.concatRegexps(params.getCallerExcludes()) + "'/>");

        for (String key : props.keySet()) {
            String value = props.get(key);
            ctx.indentPrintln("<property " + XmlNames.NAME + "='" + key + "' " + XmlNames.VALUE + "='" + value + "'/>");
        }
        ctx.indentPrintln("<property " + XmlNames.NAME + "='dynamic.collected' " + XmlNames.VALUE + "='" + params.isDynamicCollect() + "'/>");
        ctx.indentPrintln("<property " + XmlNames.NAME + "='id.count' " + XmlNames.VALUE + "='" + count + "'/>");
        if (scaleOpts.getScaleSize() > 1) {
            ctx.indentPrintln("<property " + XmlNames.NAME + "='scale.size' " + XmlNames.VALUE + "='" + scaleOpts.getScaleSize() + "'/>");
            ctx.indentPrintln("<property " + XmlNames.NAME + "='scales.compressed' " + XmlNames.VALUE + "='" + scaleOpts.scalesCompressed() + "'/>");
        }

        ctx.decIndent();
        ctx.indentPrintln("</" + XmlNames.HEAD + ">");
    }

    /**
     * XML reading
     *
     * @throws FileFormatException
     */
    public void readHeader() throws FileFormatException {
        RootReader r = (RootReader) rf.getReaderFor(this);
        r.readHeader(this);
    }

    /**
     * Check whether this DataRoot is compatible with <b>other</b> one.
     *
     * @param other
     * @param severity a level of error ignorance
     * @param boe true means that checkCompatibility will stop on the first
     * occurred critical (see <b>severity</b>) error
     * @return Number of errors (if <b>boe</b> is set to true it can be only 1)
     */
    public CompatibilityCheckResult checkCompatibility(DataRoot other, int severity, boolean boe) {
        if (!checkHeaderCompatibility(other)) {
            logger.log(Level.SEVERE, "Attempt to merge data of different data types: \n"
                    + "expected {0}; found {1}  (in XML header, property name \"coverage.generator.mode\")",
                    new Object[]{params.getMode().name(), params.getMode().name()});
            return new CompatibilityCheckResult(1, 0);
        }
        int errors = 0;
        int warnings = 0;

        // checking java version after all checks as java mismatch is warning
        String ver = other.props.get("java.runtime.version");
        if (ver != null && !ver.equals(props.get("java.runtime.version"))) {
            logger.log(Level.WARNING, "Java version differs in file {0}: {1}", new Object[]{other.storageFileName, ver});
            warnings += 1;
        }

        for (DataPackage pOther : other.packages.values()) {
            if (packages.containsKey(pOther.getName())) {
                DataPackage p = packages.get(pOther.getName());
                for (DataClass cl : pOther.getClasses()) {
                    DataClass c = p.findClass(cl.getName());
                    if (c != null) {
                        try {
                            errors += c.checkCompatibility(cl, other.getStorageFileName(), severity, boe);
                        } catch (MergeException e) {
                            logger.log(Level.SEVERE, "Error while merging class " + cl.getFullname(), e);
                            errors++;
                            if (boe) {
                                return new CompatibilityCheckResult(errors, warnings);
                            }
                        }
                    }
                }
            }
        }

        return new CompatibilityCheckResult(errors, warnings);
    }

    /**
     * Creates scale information
     */
    public void createScales() {
        for (DataPackage p : getPackages()) {
            for (DataClass c : p.getClasses()) {
                for (DataMethod m : c.getMethods()) {
                    for (DataBlock b : m) {
                        b.expandScales(1, false);
                    }
                }
                for (DataField f : c.getFields()) {
                    for (DataBlock b : f) {
                        b.expandScales(1, false);
                    }
                }
            }
        }
        scaleOpts.setScaleSize(1);
    }

    /**
     * Adds one new scale. Use this method when DataRoot receives several
     * testruns during one VM run
     */
    public void addScales() {
        int newScaleSize = scaleOpts.getScaleSize() + 1;
        for (DataPackage p : getPackages()) {
            for (DataClass c : p.getClasses()) {
                for (DataMethod m : c.getMethods()) {
                    for (DataBlock b : m) {
                        b.expandScales(newScaleSize, false, b.collectCount() - b.count);
                    }
                }
                for (DataField f : c.getFields()) {
                    for (DataBlock b : f) {
                        b.expandScales(newScaleSize, false, b.collectCount() - b.count);
                    }
                }
            }
        }
        scaleOpts.setScaleSize(newScaleSize);
    }

    /**
     * Remove all scale information
     */
    public void cleanScales() {
        for (DataPackage p : getPackages()) {
            for (DataClass c : p.getClasses()) {
                for (DataMethod m : c.getMethods()) {
                    for (DataBlock b : m) {
                        b.cleanScale();
                    }
                }
                for (DataField f : c.getFields()) {
                    for (DataBlock b : f) {
                        b.cleanScale();
                    }
                }
            }
        }
        scaleOpts.setScaleSize(0);
    }

    /**
     * Simple class containing error and warning number
     */
    public static class CompatibilityCheckResult {

        public int errors;
        public int warnings;

        public CompatibilityCheckResult(int errors, int warnings) {
            this.errors = errors;
            this.warnings = warnings;
        }
    }

    public void mergeSorted(DataRoot other, boolean fullmerge) {
        mergeHeader(other, fullmerge);

        for (DataPackage otherPackage : other.packages.values()) {
            if (!packages.containsKey(otherPackage.getName())) {
                if (fullmerge) {
                    for (DataClass classOther : otherPackage.getClasses()) {
                        if (scaleOpts.needReadScales()) {
                            classOther.expandScales(scaleOpts.getScaleSize(), true);
                            addClass(classOther);
                        }
                    }
                }
            } else {
                ListIterator<DataClass> thisClasses = packages.get(otherPackage.getName()).getClasses().listIterator();
                ListIterator<DataClass> otherClasses = otherPackage.getClasses().listIterator();

                DataClass thisClass = null, otherClass = null;

                if (!thisClasses.hasNext()) {
                    while (otherClasses.hasNext()) {
                        otherClass = otherClasses.next();
                        if (fullmerge) {
                            if (scaleOpts.needReadScales()) {
                                otherClass.expandScales(scaleOpts.getScaleSize(), true);
                            }
                            thisClasses.add(otherClass);
                            thisClasses.next(); // skipping added class - preserving sorted state
                        }
                    }
                } else {

                    while (otherClasses.hasNext()) {
                        otherClass = otherClasses.next();
                        thisClass = thisClasses.next();

                        int comp = thisClass.compareTo(otherClass);
                        while (thisClasses.hasNext() && comp < 0) {
                            thisClass = thisClasses.next();
                            comp = thisClass.compareTo(otherClass);
                        }
                        if (comp == 0) {
                            // found - merging class
                            thisClass.mergeSorted(otherClass);
                        } else if (comp > 0) {
                            // no such class in thisClasses
                            if (fullmerge) {
                                if (scaleOpts.needReadScales()) {
                                    otherClass.expandScales(scaleOpts.getScaleSize(), true);
                                }
                                thisClasses.previous();
                                thisClasses.add(otherClass); // adding _before_ current. No need to skip new class as it was added before current and currect is still thisClass
                            }
                        } else {
                            // comp < 0 - thisClasses has no more elements - all that left are missing in thisClasses
                            if (fullmerge) {
                                if (scaleOpts.needReadScales()) {
                                    otherClass.expandScales(scaleOpts.getScaleSize(), true);
                                }
                                thisClasses.add(otherClass);
                                thisClasses.next(); // skipping added class - preserving sorted state
                                while (otherClasses.hasNext()) {
                                    otherClass = otherClasses.next();
                                    if (scaleOpts.needReadScales()) {
                                        otherClass.expandScales(scaleOpts.getScaleSize(), true);
                                    }
                                    thisClasses.add(otherClass);
                                    thisClasses.next(); // skipping added class - preserving sorted state
                                }
                            }
                            break; // need to break as next iteration will call next() which will fail
                        }
                    }
                }

            }
        }
    }

    /**
     * Merges all information from <b>other</b> to this DataRoot<br/><br/>
     *
     * All hits are summed. If <b>fullmerge</b> is true all missing classes are
     * copied as well<br/><br/>
     *
     * Note that in current implementation "other" DataRoot becomes invalid
     * during the merge and destroyed at the end of method. This is done for
     * efficiency, when we add classes into "this" from "other", to avoid clone
     * operations.
     *
     * @param other
     * @param fullmerge whether merge should add classes missing in this and
     * existing in other. Should be false when merging with template - in this
     * case filters and instrumentation method in the header will not be merged
     * @throws MergeException
     */
    public void merge(DataRoot other, boolean fullmerge) {
        mergeHeader(other, fullmerge);

        for (DataPackage pOther : other.packages.values()) {
            if (!packages.containsKey(pOther.getName())) {
                if (fullmerge) {
                    for (DataClass cl : pOther.getClasses()) {
                        if (scaleOpts.needReadScales()) {
                            cl.expandScales(scaleOpts.getScaleSize(), true, 0); // as now scales are created always in readDataRoot - no need to set last bit ON
                        }
                        addClass(cl);
                    }
                }
            } else {
                DataPackage p = packages.get(pOther.getName());
                if (p.getModuleName() == null || p.getModuleName().equals(XmlNames.NO_MODULE)) {
                    p.setModuleName(pOther.getModuleName());
                }
                for (DataClass cl : pOther.getClasses()) {
                    DataClass c = p.findClass(cl.getName());
                    if (c == null) {
                        if (fullmerge) {
                            if (scaleOpts.needReadScales()) {
                                cl.expandScales(scaleOpts.getScaleSize(), true, 0); // as now scales are created always in readDataRoot - no need to set last bit ON
                            }
                            p.addClass(cl);
                        }
                    } else {
                        c.merge(cl);
                        c.setModuleName(cl.getModuleName());
                    }
                }
            }
        }

        //To adjust scales for untouched classes
        if (scaleOpts.needReadScales()) {
            for (DataPackage p : packages.values()) {
                for (DataClass cl : p.getClasses()) {
                    cl.expandScales(scaleOpts.getScaleSize(), false, 0); // untouched classes were not hit
                }
            }
        }

        instances.remove(other.rootId);
        instances.put(other.rootId, this);
        secondaryIDs.add(other.rootId);
        other.rootId = -1; // to avoid remove problems (DataRoot.merge(dr); dr.destroy();)
    }

    /**
     * Remove all blocks structure from this DataRoot (convert to method
     * coverage)
     */
    public void truncateToMethods() {
        if (params.getMode() != InstrumentationOptions.InstrumentationMode.METHOD) {
            for (DataPackage pack : packages.values()) {
                for (DataClass clazz : pack.getClasses()) {
                    ListIterator<DataMethod> it = clazz.getMethods().listIterator();
                    while (it.hasNext()) {
                        DataMethod meth = it.next();
                        if (!(meth instanceof DataMethodEntryOnly)) {
                            it.set(new DataMethodEntryOnly(meth)); // should care of copying scales from blocks to methods
                        }
                    }
                }
            }
            params = InstrumentationParams.setMode(params, InstrumentationOptions.InstrumentationMode.METHOD);
        }
    }

    /**
     * Merge JCov data with another one without looking into blocks structure
     *
     * @param other
     * @param addmissing
     */
    public void mergeOnSignatures(DataRoot other, boolean addmissing) {
        mergeHeader(other, addmissing);

        for (DataPackage otherPackage : other.packages.values()) {
            DataPackage thisPackage = packages.get(otherPackage.getName());
            if (thisPackage == null) { // other's package doesn't exist here - if fullmerge mode - need to add
                if (addmissing) {
                    for (DataClass otherClass : otherPackage.getClasses()) {
                        if (scaleOpts.needReadScales()) {
                            otherClass.expandScales(scaleOpts.getScaleSize(), true, 0);
                        }
                        addClass(otherClass);
                    }
                }
            } else {
                // package merging logic is in DataRoot as merging can affect scales
                outer:
                for (DataClass otherClass : otherPackage.getClasses()) {
                    for (DataClass thisClass : thisPackage.getClasses()) {
                        if (otherClass.getName().equals(thisClass.getName())) {
                            thisClass.mergeOnSignatures(otherClass);

                            continue outer;
                        }
                    }

                    // "continue outer" didn't happen - class not found
                    if (addmissing) {
                        if (scaleOpts.needReadScales()) {
                            otherClass.expandScales(scaleOpts.getScaleSize(), true, 0);
                        }
                        addClass(otherClass);
                    }
                }
            }
        }

        //To adjust scales for untouched classes
        if (scaleOpts.needReadScales()) {
            for (DataPackage p : packages.values()) {
                for (DataClass cl : p.getClasses()) {
                    cl.expandScales(scaleOpts.getScaleSize(), false, 0); // untouched classes were not hit
                }
            }
        }

        instances.remove(other.rootId);
        instances.put(other.rootId, this);
        secondaryIDs.add(other.rootId);
    }

    private boolean checkHeaderCompatibility(DataRoot other) {
        return params.getMode().equals(other.params.getMode());
    }

    private void mergeHeader(DataRoot other, boolean fullmerge) {
        args = mergeArgs(args, other.args);

        count = count > other.count ? count : other.count;

        if (fullmerge) {
            params = InstrumentationParams.mergeParams(params, other.params);
        } else {
            params = InstrumentationParams.mergeDetectInternalOnly(params, other.params);
        }

        if (scaleOpts.needReadScales()) {
            scaleOpts.setScaleSize(scaleOpts.getScaleSize() + other.scaleOpts.getScaleSize());
        }

    }

    private String mergeArgs(String args1, String args2) {
        TreeSet<String> resSet = new TreeSet();

        List<String> excludes = new LinkedList();
        excludes.add(InstrumentationOptions.DSC_INCLUDE.name);
        excludes.addAll(Arrays.asList(InstrumentationOptions.DSC_INCLUDE.aliases));
        excludes.add(InstrumentationOptions.DSC_EXCLUDE.name);
        excludes.addAll(Arrays.asList(InstrumentationOptions.DSC_EXCLUDE.aliases));
        excludes.add(InstrumentationOptions.DSC_CALLER_INCLUDE.name);
        excludes.addAll(Arrays.asList(InstrumentationOptions.DSC_CALLER_INCLUDE.aliases));
        excludes.add(InstrumentationOptions.DSC_CALLER_EXCLUDE.name);
        excludes.addAll(Arrays.asList(InstrumentationOptions.DSC_CALLER_EXCLUDE.aliases));
        for (String s : excludes) {
            s += "=";
        }

        String[][] arrays = {args1.split(","), args2.split(",")};
        for (String[] arr : arrays) {
            for (String s : arr) {
                boolean toExclude = false;
                for (String ex : excludes) {
                    if (s.startsWith(ex)) {
                        toExclude = true;
                        break;
                    }
                }
                if (!toExclude) {
                    resSet.add(s);
                }
            }
            arr = args2.split(",");
        }

        String res = "";
        for (String s : resSet) {
            res += s + ",";
        }

        return res;
    }

    /**
     * Synchronizes this DataRoot to Collect.slots - sets data from Collect to
     * each element of DataRoot
     */
    public void makeAttached() {
        Collect.setSlot(count);

        for (DataPackage p : packages.values()) {
            for (DataClass clazz : p.getClasses()) {
                for (DataMethod m : clazz.getMethods()) {
                    for (DataBlock b : m) {
                        b.attached = true;
                        b.setCount(Collect.countFor(b.slot) + b.count);
                    }
                }
                for (DataField fld : clazz.getFields()) {
                    for (DataBlock b : fld) {
                        b.attached = true;
                        b.setCount(Collect.countFor(b.slot) + b.count);
                    }
                }
            }
        }

        attached = true;
    }

    /**
     * Make this DataRoot synchronized with Collect class. <br/><br/>
     *
     * This means that hit counts are read from Collect and not from DataRoot
     * hierarchy
     */
    public void attach() {

        Collect.setSlot(count);

        for (DataPackage p : packages.values()) {
            for (DataClass clazz : p.getClasses()) {
                for (DataMethod m : clazz.getMethods()) {
                    for (DataBlock b : m) {
                        b.attach();
                    }
                }
                for (DataField fld : clazz.getFields()) {
                    for (DataBlock b : fld) {
                        b.attach();
                    }
                }
            }
        }

        attached = true;
    }

    /**
     * Make this DataRoot unsynchronized with Collect class.<br/><br/>
     *
     * This will copy all actual data from Collect to this DataRoot object
     */
    public void detach() {
        updateHead();
        for (DataPackage p : packages.values()) {
            for (DataClass clazz : p.getClasses()) {
                for (DataMethod m : clazz.getMethods()) {
                    for (DataBlock b : m) {
                        b.detach();
                    }
                }
                for (DataField fld : clazz.getFields()) {
                    for (DataBlock b : fld) {
                        b.detach();
                    }
                }
            }
        }

        attached = false;
    }

    /**
     * Copy actual hit data from Collect to this DataRoot object
     */
    public void update() {
        updateHead();
        for (DataPackage p : packages.values()) {
            for (DataClass clazz : p.getClasses()) {
                for (DataMethod m : clazz.getMethods()) {
                    for (DataBlock b : m) {
                        b.update();
                    }
                }
                for (DataField fld : clazz.getFields()) {
                    for (DataBlock b : fld) {
                        b.update();
                    }
                }
            }
        }
    }

    /**
     * Update slot count. Not needed to do this now as count now is taken from
     * Collect when attached
     */
    private void updateHead() {
        count = Collect.slotCount();
    }

    /**
     * Removes classes rejected by the filter. For each accepted class applies
     * the same filter to eliminate unwanted members.
     *
     * @param filter
     */
    public void applyFilter(MemberFilter filter) {
        if (filter == null) {
            return;
        }

        for (String pack : packages.keySet()) {
            DataPackage p = packages.get(pack);
            p.applyFilter(filter);
        }

        List<String> toRemove = new LinkedList();
        for (String pName : packages.keySet()) {
            if (packages.get(pName).getClasses().isEmpty()) {
                toRemove.add(pName);
            }
        }
        for (String pack : toRemove) {
            packages.remove(pack);
        }

    }

    /**
     * Remove duplicates in scales
     *
     * @param pairs
     */
    public void illuminateDuplicatesInScales(ArrayList pairs) {
        scaleOpts.setScaleSize(scaleOpts.getScaleSize() - pairs.size());
        int newSize = scaleOpts.getScaleSize();
        for (DataPackage p : packages.values()) {
            for (DataClass clazz : p.getClasses()) {
                for (DataMethod m : clazz.getMethods()) {
                    for (DataBlock b : m) {
                        b.illuminateDuplicatesInScales(newSize, pairs);
                    }
                }
                for (DataField fld : clazz.getFields()) {
                    for (DataBlock b : fld) {
                        b.illuminateDuplicatesInScales(newSize, pairs);
                    }
                }
            }
        }
    }

    /**
     * Check whether this DataRoot is configured to differ elements. E.g.
     * constructors vs simple methods, classes vs interfaces<br/><br/> False
     * always
     *
     * @return False always
     */
    public boolean isDifferentiateElements() {
        return false;
    }

    /**
     * <p> Reads XML and retrieves DataRoot from it. Scales are read from the
     * file or created if file doesn't contain scale information </p> <p> Uses
     * Reader.readXML() </p>
     *
     * @param filename file to read
     * @return DataRoot read from XML
     * @throws FileFormatException if any problem reading XML occurs.
     * @see com.sun.tdk.jcov.io.Reader
     */
    public static DataRoot read(String filename) throws FileFormatException {
        return Reader.readXML(filename);
    }

    /**
     * <p> Reads XML and retrieves DataRoot from it. </p> <p> Uses
     * Reader.readXML() </p>
     *
     * @param filename file to read
     * @param readScales if true - JCov will read or create scales for this
     * DataRoot
     * @return DataRoot read from XML
     * @throws FileFormatException if any problem reading XML occurs.
     * @see com.sun.tdk.jcov.io.Reader
     */
    public static DataRoot read(String filename, boolean readScales) throws FileFormatException {
        return Reader.readXML(filename, readScales, null);
    }

    /**
     * <p> Reads XML and retrieves DataRoot from it. </p> <p> Uses
     * Reader.readXML() </p>
     *
     * @param filename file to read
     * @param read_scales if true - JCov will read or create scales for this
     * DataRoot
     * @param filter allows to filter out some classes or specific methods and
     * fields
     * @return DataRoot read from XML
     * @throws FileFormatException
     * @see com.sun.tdk.jcov.io.Reader
     * @see com.sun.tdk.jcov.filter.MemberFilter
     */
    public static DataRoot read(String filename, boolean read_scales, MemberFilter filter) throws FileFormatException {
        return Reader.readXML(filename, read_scales, filter);
    }

    /**
     * Writes DataRoot to XML in <b>filename</b>
     *
     * @param filename file to write XML to
     * @param mergeMode defines behavior when file exists (allows to merge data)
     * @see com.sun.tdk.jcov.instrument.InstrumentationOptions.MERGE
     */
    public void write(String filename, InstrumentationOptions.MERGE mergeMode) throws Exception {
        JCovXMLFileSaver saver = new JCovXMLFileSaver(this, filename, null, mergeMode, false);
        saver.saveResults();
    }

    /**
     * Sort out all data
     */
    public void sort() {
        for (DataPackage p : packages.values()) {
            p.sort();
        }
    }

    /**
     * Nonstandard options are stored in TreeMap. All it's containing would be
     * saved to XML and binary
     *
     * @param props
     */
    public void setXMLHeadProperties(TreeMap<String, String> props) {
        this.props = props;
    }

    /**
     * @return Nonstandard options tree
     */
    public Map<String, String> getXMLHeadProperties() {
        return props;
    }

    /**
     * Writes DataRoot and all it's hierarchy to the stream
     *
     * @param out
     * @throws IOException
     */
    public void writeObject(DataOutput out) throws IOException {
        params.writeObject(out);
        scaleOpts.writeObject(out);
        writeString(out, args);
        out.writeShort(packages.size());
        for (DataPackage p : packages.values()) {
            p.writeObject(out);
        }
        out.write(props.size());
        for (Map.Entry<String, String> e : props.entrySet()) {
            out.writeUTF(e.getKey());
            out.writeUTF(e.getValue());
        }
    }

    /**
     * Creates an instance of DataRoot reading it from the stream
     *
     * @param in
     * @throws IOException
     */
    public DataRoot(DataInput in) throws IOException {
        super(instanceCount++);
        try {
            params = new InstrumentationParams(in);
            scaleOpts = new ScaleOptions(in);
            args = readString(in);
            int packs = in.readShort();
            packages = new HashMap<String, DataPackage>(packs);
            for (int i = 0; i < packs; ++i) {
                DataPackage p = new DataPackage(rootId, in);
                packages.put(p.getName(), p);
            }
            int propsCount = in.readByte();
            props = new TreeMap<String, String>();
            for (int i = 0; i < propsCount; ++i) {
                props.put(in.readUTF(), in.readUTF());
            }
            instances.put(rootId, this);
        } catch (IOException e) {
            --instanceCount;
            throw e;
        }
    }

    private static DataRoot mapXML(InputStream is, long[] counts) throws Exception {
        DataRoot root = Reader.readXML(is);
        root = applyCounts(root, counts, true);
        return root;
    }

    public static DataRoot setCounts(DataRoot root, long[] counts) throws Exception {
        return applyCounts(root, counts, false);
    }

    public static DataRoot mergeCounts(DataRoot root, long[] counts) throws Exception {
        return applyCounts(root, counts, true);
    }

    private static DataRoot applyCounts(DataRoot root, long[] counts, boolean merge)
            throws Exception {

        for (DataPackage pack : root.getPackages()) {
            for (DataClass clazz : pack.getClasses()) {
                for (DataField fld : clazz.getFields()) {
                    long count = merge ? fld.getCount() : 0;
                    fld.setCount(count + counts[fld.getId()]);
                }

                for (DataMethod meth : clazz.getMethods()) {
                    if (meth instanceof DataMethodInvoked) {
                        DataMethodInvoked mi = (DataMethodInvoked) meth;
                        long count = merge ? mi.getCount() : 0;
                        mi.setCount(count + counts[mi.getId()]);
                    } else if (meth instanceof DataMethodEntryOnly) {
                        DataMethodEntryOnly me = (DataMethodEntryOnly) meth;
                        long count = merge ? me.getCount() : 0;
                        me.setCount(count + counts[me.getId()]);
                    } else {
                        DataMethodWithBlocks mb = (DataMethodWithBlocks) meth;
                        for (BasicBlock bb : mb.getBasicBlocks()) {
                            for (DataBlock db : bb.blocks()) {
                                long count = merge ? db.getCount() : 0;
                                db.setCount(count + counts[db.getId()]);
                            }
                            if (bb.exit instanceof DataBranch) {
                                DataBranch ba = (DataBranch) bb.exit;
                                for (DataBlockTarget tg : ba.branchTargets) {
                                    long count = merge ? tg.getCount() : 0;
                                    tg.setCount(count + counts[tg.getId()]);
                                }
                            }
                        }
                    }
                }
            }
        }

        return root;
    }
    private static transient Locale LOCALE_ROOT = new Locale("__", "", ""); // for jdk1.5 support
    private static final transient String ROOT_PACKAGE = "";
}