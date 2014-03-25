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
package com.sun.tdk.jcov.constants;

/**
 * A storage for miscellaneous constants used in jcov classes.
 *
 * @author Konstantin Bobrovsky
 */
public interface MiscConstants {

    int versionMajor = 2;
    int versionMinor = 0;
    // Jcov data file constants
    String KWRD_FILE_VERSION = "JCOV-DATA-FILE-VERSION:";
    String KWRD_CLASS = "CLASS:";
    String KWRD_TIMESTAMP = "TIMESTAMP:";
    String KWRD_SRCFILE = "SRCFILE:";
    String KWRD_METHOD = "METHOD:";
    String KWRD_FIELD = "FIELD:";
    String KWRD_FILTER = "FILTER:";
    String KWRD_DATA = "DATA:";
    String KWRD_SUPERNAME = "SUPERNAME:";
    String KWRD_SUPER_INTERFACES = "SUPER_INTERFACES:";
    String KWRD_FX_ACCESS = "fxaccess:";
    char COMMENT_CHAR = '#';
    String SECTION_COMMENT2 = COMMENT_CHAR + "kind\tcount";
    String SECTION_COMMENT3 = COMMENT_CHAR + "kind\tcount\tposition";
    String SECTION_COMMENT4 = COMMENT_CHAR + "kind\tline\tposition\tcount";
    String SECTION_COMMENT4_NEW = COMMENT_CHAR + "kind\tstart\tend\t\tcount";
    String IDENT_LINE = KWRD_FILE_VERSION + " " + versionMajor + "." + versionMinor;
    char DATA_TYPE_A = 'A';
    char DATA_TYPE_B = 'B';
    char DATA_TYPE_M = 'M';
    char DATA_TYPE_C = 'C';
    String COMPRESSED_SPECIFIER = "C";
    int MAX_JCOV_LINE_LEN = 1024;
    // Types of Jcov items
    int CT_FIRST_KIND = 1;
    int CT_METHOD = 1;
    int CT_FIKT_METHOD = 2;
    int CT_BLOCK = 3;
    int CT_FIKT_RET = 4;
    int CT_CASE = 5;
    int CT_SWITCH_WO_DEF = 6;
    int CT_BRANCH_TRUE = 7;
    int CT_BRANCH_FALSE = 8;
    int CT_LINE = 9;
    int CT_LAST_KIND = 9;
    String[] STR_ITEM_KIND = {
        "ERROR!",
        "METHOD",
        "METHOD",
        "BLOCK",
        "BLOCK",
        "BRANCH",
        "BRANCH",
        "true",
        "false",
        "LINE"
    };
    String[] accessModifiers = {
        "public",
        "private",
        "protected",
        "static",
        "final",
        "synchronized",
        "volatile",
        "transient",
        "native",
        "interface",
        "abstract",
        "super",
        "strict",
        "explicit"
    };
    int CRT_ENTRY_PACK_BITS = 10;
    int CRT_ENTRY_POS_MASK = 0x3FF;
    char JVM_PACKAGE_DELIM = '/';
    String UniformSeparator = "|";
    // Other Jcov constants
    int JcovPortNumber = 3334;        // default number of jcov port
    int JcovOncePortNumber = 3335;    // default number of one-time jcov port
    int JcovGrabberCommandPort = 3336;
    String JcovProperty = "jcov.file";
    String JcovPropertyPort = "jcov.port";
    String JcovAutoSave = "jcov.autosave";
    String JcovServerPortProperty = "jcov.server_port";
    String JcovSaveFileName = "java.jcov";
    String JcovSaveFileNameXML = "result.xml";
    String JcovTemplateFileNameXML = "template.xml";
    String JcovDataStreamEndTag = "# END OF JCOV DATA";
    String JcovDataStreamStartTag = "# START OF JCOV DATA";
    String urlDelim = ",";
    public static final int GRABBER_KILL_COMMAND = 1;
    public static final int GRABBER_FORCE_KILL_COMMAND = 2;
    public static final int GRABBER_SAVE_COMMAND = 3;
    public static final int GRABBER_STATUS_COMMAND = 4;
    public static final int GRABBER_WAIT_COMMAND = 5;
}
