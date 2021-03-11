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

import com.sun.tdk.jcov.tools.EnvHandler;
import com.sun.tdk.jcov.tools.JCovTool.EnvHandlingException;
import com.sun.tdk.jcov.tools.OptionDescr;
import com.sun.tdk.jcov.util.Utils;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Dmitry Fazunenko
 * @author Alexey Fedorchenko
 */
public class InstrumentationOptions {

    public final static OptionDescr DSC_TYPE =
            new OptionDescr("type", "Type of template.", new String[][]{
                {"all", "full information"},
                {"branch", "only branches"},
                {"block", "only block"},
                {"method", "only methods"},},
            "Create statistics only for the specified type", "all");
    public static final String XML_DEFAULT_TEMPLATE = "template.xml";
    public final static OptionDescr DSC_TEMPLATE =
            new OptionDescr("template", new String[]{"t"}, "Pre-instrumented template specification.", OptionDescr.VAL_SINGLE,
            "Specifies file with xml based template of pre-instrumented classes. Each classfile presented\n"
            + " in this template should be statically instrumented. The instrumentation parameters should be\n"
            + "compatible for static and dynamic instrumentation.", XML_DEFAULT_TEMPLATE);
    public final static OptionDescr DSC_MINCLUDE =
            new OptionDescr("include_module", new String[]{"im"}, "Filtering conditions.",
                    OptionDescr.VAL_MULTI, "Specify included classes by regular expression for modules.");
    public final static OptionDescr DSC_MEXCLUDE =
            new OptionDescr("exclude_module", new String[]{"em"}, "", OptionDescr.VAL_MULTI,
                    "Specify excluded classes by regular expression for modules.");
    public final static OptionDescr DSC_INCLUDE =
            new OptionDescr("include", new String[]{"i"}, "Filtering conditions.",
            OptionDescr.VAL_MULTI, "Specify included classes by regular expression.");
    public final static OptionDescr DSC_EXCLUDE =
            new OptionDescr("exclude", new String[]{"e"}, "", OptionDescr.VAL_MULTI,
            "Specify excluded classes by regular expression.");
    public final static OptionDescr DSC_MINCLUDE_LIST =
            new OptionDescr("include_module_list", "", OptionDescr.VAL_SINGLE,
            "Specify the path to the file containing module include list. The effect will be the same as\n"
            + "using multiple -include_module options. File should contain one module name mask per line.");
    public final static OptionDescr DSC_MEXCLUDE_LIST =
            new OptionDescr("exclude_module_list", "", OptionDescr.VAL_SINGLE,
            "Specify the path to the file containing module exclude list. The effect will be the same as\n"
            + "using multiple -exclude_module options. File should contain one module name mask per line.");
    public final static OptionDescr DSC_INCLUDE_LIST =
            new OptionDescr("include_list", "", OptionDescr.VAL_SINGLE,
            "Specify the path to the file containing include list. The effect will be the same as\n"
            + "using multiple -include options. File should contain one class name mask per line.");
    public final static OptionDescr DSC_EXCLUDE_LIST =
            new OptionDescr("exclude_list", "", OptionDescr.VAL_SINGLE,
            "Specify the path to the file containing exclude list. The effect will be the same as\n"
            + "using multiple -exclude options. File should contain one class name mask per line.");
    public final static OptionDescr DSC_FM =
            new OptionDescr("fm", "", OptionDescr.VAL_MULTI,
            "Class must have <modifier> to be included in report");
    public final static OptionDescr DSC_FM_LIST =
            new OptionDescr("fm_list", "", OptionDescr.VAL_SINGLE,
            "Specify the path to the file containing modifiers list. The \n"
            + "effect will be the same as using multiple -fm options.\n"
            + "File should contain one class name mask per line.");
    public final static OptionDescr DSC_CALLER_INCLUDE =
            new OptionDescr("caller_include", new String[]{"ci"}, "", OptionDescr.VAL_MULTI,
            "Specify caller filter classes by regular expression.", ".*");
    public final static OptionDescr DSC_CALLER_EXCLUDE =
            new OptionDescr("caller_exclude", new String[]{"ce"}, "", OptionDescr.VAL_MULTI,
            "Specify caller filter classes by regular expression.", "");
    public final static OptionDescr DSC_ABSTRACT =
            new OptionDescr("abstract", "Specify which items should be additionally included in template.",
            new String[][]{
                {"on", "abstract methods are counted by their direct invocation"},
                {"off", "abstract methods are not counted and not included int report"},}, "Specify should be abstract method included in template.", "off");
    public final static OptionDescr DSC_NATIVE =
            new OptionDescr("native", "", new String[][]{
                {"on", "native methods are counted and included in report"},
                {"off", "native methods are not counted and not included int report"}
            }, "Specify should be native method included in template.", "on");
    public final static OptionDescr DSC_FIELD =
            new OptionDescr("field", "", new String[][]{
                {"on", "field reading is counted"},
                {"off", "field coverage is not counted, fields are not included in report"}
            },
            "Specify should be fields included in template.", "off");
    public final static OptionDescr DSC_SYNTHETIC =
            new OptionDescr("synthetic", "", new String[][]{
                {"on", "synthetic methods are counted"},
                {"off", "synthetic methods are not counted and are not included in report"}
            },
            "Specify should be synthetic methods included in template.", "on");
    public final static OptionDescr DSC_ANONYM =
            new OptionDescr("anonym", "", new String[][]{
                {"on", "anonymous classes are counted"},
                {"off", "anonymous classes are not counted and are not included in report"}
            }, "Allows to filter anonymous classes", "on");
    public final static OptionDescr DSC_CLASSESRELOAD =
            new OptionDescr("classesreload", "", new String[][]{
                {"on", "classes could be reloaded. Hash is needed in dynamic mode"},
                {"off", "classes will not be reloaded"}
            }, "Allows to keep instrumented classes for reusing", "off");
    public final static OptionDescr DSC_MERGE =
            new OptionDescr("merge", "", new String[][]{
                {"merge", "standard merge (summary coverage)"},
                {"overwrite", "overwrite existing file"},
                {"gensuff", "generate suffix as current time in milliseconds"},
                {"scale", "scaled merge (coverage information for individual tests is available)"}
            },
            "Specify behaviour if output file already exists. Doesn't affects network-based\n"
            + "saving. The merge and scale methods could cause errors if existing file could not be read.", "merge");
    public final static OptionDescr DSC_SAVE_BEGIN =
            new OptionDescr("savebegin", "Save points.", OptionDescr.VAL_MULTI,
            "Initiate saving Jcov data at the beginning of each method  specified by this regexp ,\n"
            + "before the first instruction of the method is executed. The signature of method doen't used.");
    public final static OptionDescr DSC_SAVE_AT_END =
            new OptionDescr("saveatend", "", OptionDescr.VAL_MULTI,
            "Work similar savebegin, however save before last instruction in the method is executed (throw or return).\n"
            + "This method doesn't work if exception was thrown not directly in this method.");
    public final static OptionDescr DSC_INNERINVOCATION =
            new OptionDescr("innerinvocation", "Inner invocations", new String[][]{
                {"on", "count inner invocations in the product"},
                {"off", "count only invocations outside the instrumented product"}
            }, "Allows to filter inner invocations in the instrumented product", "on");
    public final static OptionDescr DSC_INNER_INCLUDE =
            new OptionDescr("inner_include", new String[]{"ii"}, "", OptionDescr.VAL_MULTI,
            "Specify included classes by regular expression for adding inner invocations instrumentaion\n" +
            "(only for innerinvocation off)");
    public final static OptionDescr DSC_INNER_EXCLUDE =
            new OptionDescr("inner_exclude", new String[]{"ie"}, "", OptionDescr.VAL_MULTI,
            "Specify excluded classes by regular expression, no inner invocations instrumentaion will be\n" +
            "added to the specified classes (only for innerinvocation off)");
    public final static OptionDescr DSC_INSTR_PLUGIN =
            new OptionDescr("instr_plugin", new String[0], "Instrumentation plugin", OptionDescr.VAL_SINGLE,
                    "Defines instrumentation to be performed additionaly to already performed by JCov");

    public static enum ABSTRACTMODE {

        NONE, IMPLEMENTATION, DIRECT
    };

    public static enum InstrumentationMode {

        METHOD, BLOCK, BRANCH;

        public static InstrumentationMode fromString(String mode) {
            if (mode.equalsIgnoreCase("method")) {
                return METHOD;
            } else if (mode.equalsIgnoreCase("block")) {
                return BLOCK;
            } else if (mode.equalsIgnoreCase("branch")) {
                return BRANCH;
            } else if (mode.equalsIgnoreCase("all")) {
                return BRANCH;
            } else {
                throw new Error("Unknown instrumentation type.");
            }
        }
    };

    public static enum MERGE {

        OVERWRITE, MERGE, SCALE, GEN_SUFF
    };
    public static final String nativePrefix = "$$generated$$_";

    public static String concatRegexps(String[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        String result = "";

        for (String arg : args) {
            result += "|" + arg;
        }
        return result.substring(1);
    }

    public static boolean isSkipped(String className, String methodName, int modifier) {
        if (className.equals("java/lang/Thread")
                && (methodName.equals("currentThread") || methodName.equals("getId"))) {
            return true;
        }

        return false;
    }

    public static String[] handleInclude(EnvHandler opts) throws EnvHandlingException {
        Set<String> includeSet = new TreeSet<String>();
        String[] s;
        if (opts.isSet(InstrumentationOptions.DSC_INCLUDE)) {
            s = opts.getValues(InstrumentationOptions.DSC_INCLUDE);
            if (s != null) {
                includeSet.addAll(Arrays.asList(s));
            }
        }
        if (opts.isSet(InstrumentationOptions.DSC_INCLUDE_LIST)) {
            try {
                s = Utils.readLines(opts.getValue(InstrumentationOptions.DSC_INCLUDE_LIST));
                if (s != null) {
                    includeSet.addAll(Arrays.asList(s));
                }
            } catch (IOException ex) {
                throw new EnvHandlingException("Error while reading include list " + opts.getValue(InstrumentationOptions.DSC_INCLUDE_LIST), ex);
            }
        }

        return includeSet.toArray(new String[includeSet.size()]);
    }

    public static String[] handleMInclude(EnvHandler opts) throws EnvHandlingException {
        Set<String> includeSet = new TreeSet<String>();
        String[] s;
        if (opts.isSet(InstrumentationOptions.DSC_MINCLUDE)) {
            s = opts.getValues(InstrumentationOptions.DSC_MINCLUDE);
            if (s != null) {
                includeSet.addAll(Arrays.asList(s));
            }
        }

        if (opts.isSet(InstrumentationOptions.DSC_MINCLUDE_LIST)) {
            try {
                s = Utils.readLines(opts.getValue(InstrumentationOptions.DSC_MINCLUDE_LIST));
                if (s != null) {
                    includeSet.addAll(Arrays.asList(s));
                }
            } catch (IOException ex) {
                throw new EnvHandlingException("Error while reading module include list " + opts.getValue(InstrumentationOptions.DSC_INCLUDE_LIST), ex);
            }
        }

        return includeSet.toArray(new String[includeSet.size()]);
    }

    public static String[] handleInnerInclude(EnvHandler opts) throws EnvHandlingException {
        Set<String> includeSet = new TreeSet<String>();
        String[] s;
        if (opts.isSet(InstrumentationOptions.DSC_INNER_INCLUDE)) {
            s = opts.getValues(InstrumentationOptions.DSC_INNER_INCLUDE);
            if (s != null) {
                includeSet.addAll(Arrays.asList(s));
            }
        }

        return includeSet.toArray(new String[includeSet.size()]);
    }

    public static String[] handleInnerExclude(EnvHandler opts) throws EnvHandlingException {
        Set<String> excludeSet = new TreeSet<String>();
        String[] s = opts.getValues(InstrumentationOptions.DSC_INNER_EXCLUDE);
        if (s != null) {
            excludeSet.addAll(Arrays.asList(s));
        }

        return excludeSet.toArray(new String[excludeSet.size()]);
    }

    public static String[] handleExclude(EnvHandler opts) throws EnvHandlingException {
        Set<String> excludeSet = new TreeSet<String>();
        String[] s = opts.getValues(InstrumentationOptions.DSC_EXCLUDE);
        if (s != null) {
            excludeSet.addAll(Arrays.asList(s));
        }
        if (opts.isSet(InstrumentationOptions.DSC_EXCLUDE_LIST)) {
            try {
                s = Utils.readLines(opts.getValue(InstrumentationOptions.DSC_EXCLUDE_LIST));
                if (s != null) {
                    excludeSet.addAll(Arrays.asList(s));
                }
            } catch (IOException ex) {
                throw new EnvHandlingException("Error while reading exclude list " + opts.getValue(InstrumentationOptions.DSC_EXCLUDE_LIST), ex);
            }
        }

        return excludeSet.toArray(new String[excludeSet.size()]);
    }

    public static String[] handleMExclude(EnvHandler opts) throws EnvHandlingException {
        Set<String> excludeSet = new TreeSet<String>();
        String[] s = opts.getValues(InstrumentationOptions.DSC_MEXCLUDE);
        if (s != null) {
            excludeSet.addAll(Arrays.asList(s));
        }

        if (opts.isSet(InstrumentationOptions.DSC_MEXCLUDE_LIST)) {
            try {
                s = Utils.readLines(opts.getValue(InstrumentationOptions.DSC_MEXCLUDE_LIST));
                if (s != null) {
                    excludeSet.addAll(Arrays.asList(s));
                }
            } catch (IOException ex) {
                throw new EnvHandlingException("Error while reading module exclude list " + opts.getValue(InstrumentationOptions.DSC_EXCLUDE_LIST), ex);
            }
        }

        return excludeSet.toArray(new String[excludeSet.size()]);
    }

    public static String[] handleFM(EnvHandler opts) throws EnvHandlingException {
        Set<String> fmSet = new TreeSet<String>();
        String[] s;
        if (opts.isSet(InstrumentationOptions.DSC_FM)) {
            s = opts.getValues(InstrumentationOptions.DSC_FM);
            if (s != null) {
                fmSet.addAll(Arrays.asList(s));
            }
        }
        if (opts.isSet(InstrumentationOptions.DSC_FM_LIST)) {
            try {
                s = Utils.readLines(opts.getValue(InstrumentationOptions.DSC_FM_LIST));
                if (s != null) {
                    fmSet.addAll(Arrays.asList(s));
                }
            } catch (IOException ex) {
                throw new EnvHandlingException("Error while reading exclude list " + opts.getValue(InstrumentationOptions.DSC_EXCLUDE_LIST), ex);
            }
        }
        return fmSet.toArray(new String[fmSet.size()]);
    }
}