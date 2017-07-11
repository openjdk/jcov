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

import com.sun.tdk.jcov.instrument.InstrumentationOptions.ABSTRACTMODE;
import com.sun.tdk.jcov.instrument.InstrumentationOptions.InstrumentationMode;
import com.sun.tdk.jcov.runtime.Collect;
import com.sun.tdk.jcov.runtime.CollectDetect;
import com.sun.tdk.jcov.util.Utils;
import com.sun.tdk.jcov.util.Utils.Pattern;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author Andrey Titov
 */
public class InstrumentationParams {

    private boolean detectInternal;
    private boolean dynamicCollect;
    private boolean classesReload;
    private boolean instrumentNative;
    private boolean instrumentSynthetic;
    private boolean instrumentAnonymous;
    private InstrumentationOptions.ABSTRACTMODE instrumentAbstract;
    private boolean instrumentFields;
    private String callerInclude;
    private String callerExclude;
    private String[] includes;
    private String[] excludes;
    private String[] callerIncludes;
    private String[] callerExcludes;
    private String[] inner_includes;
    private String[] inner_excludes;
    private InstrumentationOptions.InstrumentationMode mode;
    private String saveBegin;
    private String saveEnd;
    private String[] savesBegin;
    private String[] savesEnd;
    private Pattern[] alls;
    private Pattern[] all_modules;
    private Pattern[] inner_alls;
    private boolean innerInvocations = true;

    public InstrumentationParams(boolean dynamicCollect, boolean instrumentNative, boolean instrumentFields, boolean detectInternal, ABSTRACTMODE instrumentAbstract, String[] includes, String[] excludes, String[] callerIncludes, String[] callerExcludes, InstrumentationMode mode) {
        this(dynamicCollect, instrumentNative, instrumentFields, detectInternal, instrumentAbstract, includes, excludes, callerIncludes, callerExcludes, mode, null, null);
    }

    public InstrumentationParams(boolean instrumentNative, boolean instrumentFields, boolean instrumentAbstract, String[] includes, String[] excludes, InstrumentationMode mode) {
        this(false, instrumentNative, instrumentFields, false, instrumentAbstract ? ABSTRACTMODE.DIRECT : ABSTRACTMODE.NONE, includes, excludes, null, null, mode, null, null);
    }

    public InstrumentationParams(boolean instrumentNative, boolean instrumentFields, boolean instrumentAbstract,  String[] includes, String[] excludes, String[] m_includes, String[] m_excludes, InstrumentationMode mode) {
        this(false, false, false, instrumentNative, instrumentFields, false, instrumentAbstract ? ABSTRACTMODE.DIRECT : ABSTRACTMODE.NONE, includes, excludes, null, null, m_includes, m_excludes, mode, null, null);
    }

    public InstrumentationParams(boolean instrumentNative, boolean instrumentFields, boolean instrumentAbstract, String[] includes, String[] excludes, InstrumentationMode mode, String[] saveBegin, String[] saveEnd) {
        this(false, instrumentNative, instrumentFields, false, instrumentAbstract ? ABSTRACTMODE.DIRECT : ABSTRACTMODE.NONE, includes, excludes, null, null, mode, saveBegin, saveEnd);
    }

    public InstrumentationParams(boolean instrumentNative, boolean instrumentFields, boolean instrumentAbstract, String[] includes, String[] excludes, String[] callerincludes, String[] callersexcludes, InstrumentationMode mode, String[] saveBegin, String[] saveEnd) {
        this(false, instrumentNative, instrumentFields, false, instrumentAbstract ? ABSTRACTMODE.DIRECT : ABSTRACTMODE.NONE, includes, excludes, callerincludes, callersexcludes, mode, saveBegin, saveEnd);
    }

    public InstrumentationParams(boolean dynamicCollect, boolean instrumentNative, boolean instrumentFields, boolean detectInternal, ABSTRACTMODE instrumentAbstract, String[] includes, String[] excludes, String[] callerIncludes, String[] callerExcludes, InstrumentationMode mode, String[] saveBegin, String[] saveEnd) {
        this(false, dynamicCollect, instrumentNative, instrumentFields, detectInternal, instrumentAbstract, includes, excludes, callerIncludes, callerExcludes, mode, saveBegin, saveEnd);
    }

    public InstrumentationParams(boolean classesReload, boolean dynamicCollect, boolean instrumentNative, boolean instrumentFields, boolean detectInternal, ABSTRACTMODE instrumentAbstract, String[] includes, String[] excludes, String[] callerIncludes, String[] callerExcludes, InstrumentationMode mode, String[] saveBegin, String[] saveEnd) {
        this(true, classesReload, dynamicCollect, instrumentNative, instrumentFields, detectInternal, instrumentAbstract, includes, excludes, callerIncludes, callerExcludes, mode, saveBegin, saveEnd);
    }

    public InstrumentationParams(boolean innerInvocations, boolean classesReload, boolean dynamicCollect, boolean instrumentNative, boolean instrumentFields, boolean detectInternal, ABSTRACTMODE instrumentAbstract, String[] includes, String[] excludes, String[] callerIncludes, String[] callerExcludes, InstrumentationMode mode, String[] saveBegin, String[] saveEnd) {
        this(innerInvocations, classesReload, dynamicCollect, instrumentNative, instrumentFields, detectInternal, instrumentAbstract, includes, excludes, callerIncludes, callerExcludes, null, null, mode, saveBegin, saveEnd);
    }

    public InstrumentationParams(boolean innerInvocations, boolean classesReload, boolean dynamicCollect, boolean instrumentNative, boolean instrumentFields, boolean detectInternal, ABSTRACTMODE instrumentAbstract, String[] includes, String[] excludes, String[] callerIncludes, String[] callerExcludes, String[] m_includes, String[] m_excludes, InstrumentationMode mode, String[] saveBegin, String[] saveEnd) {

        this.innerInvocations = innerInvocations;
        this.detectInternal = detectInternal;
        this.dynamicCollect = dynamicCollect;
        this.classesReload = classesReload;
        this.instrumentNative = instrumentNative;
        this.instrumentAbstract = instrumentAbstract;
        this.instrumentFields = instrumentFields;
        if (includes == null) {
            includes = new String[]{""};
        }
        if (excludes == null) {
            excludes = new String[]{""};
        }
        if (m_includes == null) {
            m_includes = new String[]{""};
        }
        if (m_excludes == null) {
            m_excludes = new String[]{""};
        }
        this.includes = includes;
        this.excludes = excludes;
        if (callerIncludes == null) {
            callerIncludes = new String[0];
        }
        if (callerExcludes == null) {
            callerExcludes = new String[0];
        }
        this.callerIncludes = callerIncludes;
        this.callerExcludes = callerExcludes;
        this.savesBegin = saveBegin;
        this.savesEnd = saveEnd;
        this.mode = mode;
        // nulls will be changed to "" in concatRegexps
        this.saveBegin = InstrumentationOptions.concatRegexps(saveBegin);
        this.saveEnd = InstrumentationOptions.concatRegexps(saveEnd);
        this.callerInclude = InstrumentationOptions.concatRegexps(callerIncludes);
        this.callerExclude = InstrumentationOptions.concatRegexps(callerExcludes);
        this.alls = Utils.concatFilters(includes, excludes);
        this.all_modules = Utils.concatModuleFilters(m_includes, m_excludes);
        this.inner_alls = Utils.concatFilters(inner_includes, inner_excludes);
    }

    public boolean isDetectInternal() {
        return detectInternal;
    }

    public boolean isDynamicCollect() {
        return dynamicCollect;
    }

    public boolean isClassesReload() {
        return classesReload;
    }

    public boolean isInstrumentNative() {
        return instrumentNative;
    }

    public boolean isInstrumentFields() {
        return instrumentFields;
    }

    public boolean isInstrumentSynthetic() {
        return instrumentSynthetic;
    }

    public boolean isInstrumentAnonymous() {
        return instrumentAnonymous;
    }

//    public boolean skipNotCoveredClasses() {
//        return dynamicCollect;
//    }
//
    public boolean isInstrumentAbstract() {
        return instrumentAbstract != InstrumentationOptions.ABSTRACTMODE.NONE;
    }

    public void enable() {
        Collect.enableCounts();
        if (instrumentFields || instrumentAbstract != InstrumentationOptions.ABSTRACTMODE.NONE) {
            CollectDetect.enableInvokeCounts();
        }
        if (detectInternal) {
            CollectDetect.enableDetectInternal();
        }
        Collect.enabled = true;
    }

    public boolean isIncluded(String classname) {
        return Utils.accept(alls, null, "/" + classname, null);
    }

    public boolean isInnerInstrumentationIncludes(String classname) {
        return Utils.accept(inner_alls, null, "/" + classname, null);
    }

    public boolean isModuleIncluded(String modulename) {
        return Utils.accept(all_modules, null, modulename, null);
    }

    public boolean isCallerFilterOn() {
        return /*dynamicCollect &&*/ !(callerInclude.equals(".*") && callerExclude.equals(""));
    }

    public boolean isInnerInvacationsOff() {
        return !innerInvocations;
    }

    public boolean isCallerFilterAccept(String className) {
        boolean name = className.matches(callerInclude) && !className.matches(callerExclude);
        return name;
    }

    public boolean isStackMapShouldBeUpdated() {
        return mode.equals(InstrumentationMode.BRANCH);
    }

    public InstrumentationMode getMode() {
        return mode;
    }

    public boolean isDataSaveFilterAccept(String className, String methodName, boolean isBegin) {
        return (className + "." + methodName).matches(isBegin ? saveBegin : saveEnd);
    }

    public boolean isDataSaveSpecified() {
        return saveBegin != null || saveEnd != null;
    }

    public static InstrumentationParams mergeParams(InstrumentationParams first, InstrumentationParams other) {
        boolean detectInternal = first.detectInternal || other.detectInternal;

        String[] includes = mergeFilter(first.includes, other.includes, true);
        String[] excludes = mergeFilter(first.excludes, other.excludes, false);
        String[] callerIncludes = mergeFilter(first.callerIncludes, other.callerIncludes, true);
        String[] callerExcludes = mergeFilter(first.callerExcludes, other.callerExcludes, false);

        boolean dynamicCollected = first.dynamicCollect | other.dynamicCollect;
        return new InstrumentationParams(dynamicCollected, first.instrumentNative, first.instrumentFields, detectInternal, first.instrumentAbstract, includes, excludes, callerIncludes, callerExcludes, first.mode);
    }

    public static InstrumentationParams mergeDetectInternalOnly(InstrumentationParams first, InstrumentationParams other) {
        boolean detectInternal = first.detectInternal || other.detectInternal;

        return new InstrumentationParams(first.dynamicCollect, first.instrumentNative, first.instrumentFields, detectInternal, first.instrumentAbstract, first.includes, first.excludes, first.callerIncludes, first.callerExcludes, first.mode);
    }

    //moveto utils
    public static String[] mergeFilter(String[] filter1, String[] filter2, boolean union) {
        Set<String> res = new TreeSet<String>();

        if (union) {
            res.addAll(Arrays.asList(filter1));
            res.addAll(Arrays.asList(filter2));
            return res.toArray(new String[res.size()]);
        } else {//intersection
            for (String e1 : filter1) {
                for (String e2 : filter2) {
                    if (e1.equals(e2)) {
                        res.add(e2);
                    }
                }
            }
        }

        return res.toArray(new String[res.size()]);
    }

    public String[] getIncludes() {
        return includes;
    }

    public String[] getExcludes() {
        return excludes;
    }

    public String[] getCallerIncludes() {
        return callerIncludes;
    }

    public String[] getCallerExcludes() {
        return callerExcludes;
    }

    public static InstrumentationParams setMode(InstrumentationParams params, InstrumentationMode mode) {
        return new InstrumentationParams(params.dynamicCollect, params.instrumentNative, params.instrumentFields, params.detectInternal, params.instrumentAbstract, params.includes, params.excludes, params.callerIncludes, params.callerExcludes, mode, params.savesBegin, params.savesEnd);
    }

    public void setExcludes(String[] excludes) {
        this.excludes = excludes;
        this.alls = Utils.concatFilters(includes, excludes);
    }

    public void setIncludes(String[] includes) {
        this.includes = includes;
        this.alls = Utils.concatFilters(includes, excludes);
    }

    public InstrumentationParams setInnerExcludes(String[] excludes) {
        this.inner_excludes = excludes;
        this.inner_alls = Utils.concatFilters(inner_includes, excludes);
        return this;
    }

    public InstrumentationParams setInnerIncludes(String[] includes) {
        this.inner_includes = includes;
        this.inner_alls = Utils.concatFilters(includes, inner_excludes);
        return this;
    }

    public InstrumentationParams setInstrumentSynthetic(boolean synth) {
        instrumentSynthetic = synth;
        return this;
    }

    public InstrumentationParams setInstrumentAnonymous(boolean anonym) {
        instrumentAnonymous = anonym;
        return this;
    }

    public InstrumentationParams setInnerInvocations(boolean inner) {
        innerInvocations = inner;
        return this;
    }

    void writeObject(DataOutput out) throws IOException {
        DataAbstract.writeStrings(out, excludes);
        DataAbstract.writeStrings(out, includes);
        DataAbstract.writeStrings(out, callerExcludes);
        DataAbstract.writeStrings(out, callerIncludes);
        DataAbstract.writeStrings(out, savesBegin);
        DataAbstract.writeStrings(out, savesEnd);
        out.writeBoolean(classesReload);
        out.writeBoolean(detectInternal);
        out.writeBoolean(dynamicCollect);
        out.writeBoolean(instrumentFields);
        out.writeBoolean(instrumentNative);
        out.writeBoolean(innerInvocations);
        out.write(instrumentAbstract.ordinal());
        out.write(mode.ordinal());
    }

    InstrumentationParams(DataInput in) throws IOException {
        excludes = DataAbstract.readStrings(in);
        includes = DataAbstract.readStrings(in);
        callerExcludes = DataAbstract.readStrings(in);
        callerIncludes = DataAbstract.readStrings(in);
        savesBegin = DataAbstract.readStrings(in);
        savesEnd = DataAbstract.readStrings(in);
        classesReload = in.readBoolean();
        detectInternal = in.readBoolean();
        dynamicCollect = in.readBoolean();
        instrumentFields = in.readBoolean();
        instrumentNative = in.readBoolean();
        innerInvocations = in.readBoolean();
        instrumentAbstract = ABSTRACTMODE.values()[in.readByte()];
        mode = InstrumentationMode.values()[in.readByte()];
    }
}