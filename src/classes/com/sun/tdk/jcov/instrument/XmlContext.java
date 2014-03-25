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

import com.sun.tdk.jcov.instrument.InstrumentationOptions.InstrumentationMode;
import java.io.BufferedOutputStream;
import java.util.Stack;

import java.io.PrintWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * XmlContext
 *
 *
 * @author Robert Field
 */
public class XmlContext extends PrintWriter {

    final String indentDelta;
    String indent;
    Stack<String> indentHistory;
    boolean showAbstract = true;
    boolean showNonNested = true;
    boolean showBodiesInExitSubBlocks = true;
    boolean showBasicBlocks = false;
    boolean showDetailBlocks = true;
    boolean showLineTable = true;
    boolean showRangeTable = true;
    InstrumentationParams params;
    boolean skipNotCoveredClasses = false;

    /**
     * Creates a new instance of XmlContext
     */
    public XmlContext(String filename, InstrumentationParams params) throws FileNotFoundException {
        super(new BufferedOutputStream(new FileOutputStream(filename)));
        indent = "";
        indentDelta = "\t";
        indentHistory = new Stack<String>();
        configureContext(params);
    }

    public XmlContext(OutputStream out, InstrumentationParams params) throws FileNotFoundException {
        super(out);
        indent = "";
        indentDelta = "\t";
        indentHistory = new Stack<String>();
        configureContext(params);
    }

    // Debug needs (flushing to System.output)
    public XmlContext() throws FileNotFoundException {
        super(System.out);
        indent = "";
        indentDelta = "\t";
        indentHistory = new Stack<String>();
    }

    public XmlContext(OutputStream out) throws FileNotFoundException {
        super(out);
        indent = "";
        indentDelta = "\t";
        indentHistory = new Stack<String>();
    }

    public void configureContext(InstrumentationMode mode, boolean showAbstract) {
        if (mode.equals(InstrumentationMode.BLOCK)) {
            showNonNested = false;
            showBasicBlocks = true;
            showDetailBlocks = false;
        }

        this.showAbstract = showAbstract;
    }

    void incIndent() {
        indentHistory.push(indent);
        indent += indentDelta;
    }

    void decIndent() {
        indent = indentHistory.pop();
    }

    void indent() {
        print(indent);
    }

    void indentPrintln(String str) {
        indent();
        println(str);
    }

    void attr(String name, String value) {
        print(" " + name + "=\"");
        writeEscaped(value);
        print("\"");
    }

    void attr(String name, Object value) {
        print(" " + name + "=\"");
        writeEscaped(value.toString());
        print("\"");
    }

    void attr(String name, int value) {
        print(" " + name + "=\"" + value + "\"");
    }

    void attr(String name, long value) {
        print(" " + name + "=\"" + value + "\"");
    }

    void attr(String name, boolean value) {
        print(" " + name + "=\"" + value + "\"");
    }

    void attrNormalized(String name, String value) {
        print(" " + name + "=\"");
        writeEscaped(value);
        print("\"");
    }

    public final void configureContext(InstrumentationParams params) {
        this.params = params;
        if (params.getMode().equals(InstrumentationMode.BLOCK)) {
            showNonNested = false;
            showBasicBlocks = true;
            showDetailBlocks = false;
        }

        this.showAbstract = params.isInstrumentAbstract();
    }

    public void setSkipNotCoveredClasses(boolean skipNotCoveredClasses) {
        this.skipNotCoveredClasses = skipNotCoveredClasses;
    }

    private void writeEscaped(String str) {
        for (int i = 0, j = str.length(); i < j; i++) {
            int ch = str.charAt(i);
            switch (ch) {
                case '&':
                    write("&amp;");
                    break;
                case '<':
                    write("&lt;");
                    break;
                case '>':
                    write("&gt;");
                    break;
                case '\'':
                    write("&apos;");
                    break;
                case '"':
                    write("&quot;");
                    break;
                default:
                    write(ch);
            }
        }
    }
}
