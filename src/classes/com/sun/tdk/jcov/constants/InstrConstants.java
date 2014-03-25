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

/*
 * @author Dmitry Fazunenko
 */
public interface InstrConstants {

    String sccsVersion = "%I% $LastChangedDate: 2008-08-08 19:46:37 +0400 (Fri, 08 Aug 2008) $";
// types of jcov instrumentation
    int instrByMethod = 0x1;
    int instrByBlock = 0x2;
    int instrByBranch = 0x4;
    int instrByAll = 0x7;
// types of additional instrumentation
    int addSaveBefore = 1;
    int addSaveAfter = 2;
    int addSaveBegin = 3;
    int addSaveAtEnd = 4;
// flags for others jcov features
    int synchGathFlag = 0x1;
    int autoCollectFlag = 0x2;
    int commonTimeStampFlag = 0x4;
    int noStartSatellite = 0x8;
// index in class description array
    int indexClassName = 0;
    int indexTimestamp = 1;
    int indexScrFile = 2;
    int indexModifiers = 3;
//
    int commonTimeStamp = 1;
//
    int codeGathering = 0;
    int codeReRegistration = 1;
    int codeNotGathering = 2;
//
    int scaleAutoGathering = 0x1;
    int scaleCollectSattelite = 0x2;
    String INSTR_FILE_SUFF = "i";
}
