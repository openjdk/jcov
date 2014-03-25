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
package com.sun.tdk.jcov.runtime;

import com.sun.tdk.jcov.constants.MiscConstants;
import com.sun.tdk.jcov.instrument.DataRoot;
import com.sun.tdk.jcov.instrument.InstrumentationOptions.MERGE;
import com.sun.tdk.jcov.util.RuntimeUtils;
import java.io.File;
import java.io.IOException;

/**
 * @author Dmitry Fazunenko
 * @author Alexey Fedorchenko
 */
public abstract class FileSaver implements JCovSaver {

    static final int LOCK_REPEAT = 50;
    static final int LOCK_SLEEP = 50000;
    protected String filename;    // filename to write data to
    protected String template;    // template
    protected MERGE mergeMode;    // GEN_SUFF - generate suffics to the file when exists; MERGE - merge if exists; SCALE - MERGE + write scales; OVERWRITE - remove and write if exists;
    protected boolean insertOnly; // do not change initial hit values when merging into
    protected DataRoot root;      // coverage data + product template
    protected boolean scales;
    private static boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

    protected static File createLockFile(String filename) {

        if (isWindows) {
            return null;
        }

        for (int i = LOCK_REPEAT; i != 0; i--) {
            String lockName = filename + ".lock";
            File fl = new File(lockName);
            try {
                if (fl.createNewFile()) {
                    return fl;
                }
                Thread.sleep(LOCK_SLEEP);
            } catch (InterruptedException e) {
                // should be exit??
                return null;
            } catch (IOException ioe) {
                return null;
            }
        }
        return null;
    }

    protected static void deleteLock(File lock) {
        if (isWindows) {
            return;
        }

        if (lock != null) {
            lock.delete();
        }
    }

    protected static String genUniqFileName(String filename) {
        for (int i = 1; i < Integer.MAX_VALUE; i++) {
            if (!new File(filename + "." + i).exists()) {
                return filename + "." + i;
            }
        }
        throw new Error("It\'s impossible...");
    }

    /**
     * This constructor configures file saver using Option's values
     *
     * @param root
     */
    protected FileSaver(DataRoot root, MERGE merge) {
        filename = MiscConstants.JcovSaveFileNameXML;
        template = MiscConstants.JcovTemplateFileNameXML;
        mergeMode = merge;
        this.root = root;
        this.scales = merge == MERGE.SCALE;
    }

    /**
     * This constructor configures file saver with passed parameters
     *
     * @param root
     * @param filename
     * @param template
     * @param merge
     */
    protected FileSaver(DataRoot root, String filename, String template, MERGE merge, boolean scales) {
        this.filename = filename;
        this.template = template;
        mergeMode = merge;
        this.root = root;
        this.scales = scales;
    }

    public abstract void saveResults(String filename) throws Exception;

    abstract void mergeResults(String dest, String src, boolean scale) throws Exception;

    public void saveResults() {
        if (root.getParams().isDynamicCollect() && Collect.enabled) {
            CollectDetect.enterInstrumentationCode();
        }
        try {
            if (mergeMode.equals(MERGE.GEN_SUFF)) {
                try2MergeAndSave(filename + RuntimeUtils.genSuffix(), template, scales || mergeMode == MERGE.SCALE);
                return;
            }

            if (new File(filename).exists()
                    && !mergeMode.equals(MERGE.OVERWRITE)) {
                File lock = createLockFile(filename);
                try {
                    try2MergeAndSave(filename, filename, scales || mergeMode == MERGE.SCALE);
                } finally {
                    deleteLock(lock);
                }
                return;
            } else {
                File lock = createLockFile(filename);
                try {
                    try2MergeAndSave(filename, template, scales || mergeMode == MERGE.SCALE);
                } finally {
                    deleteLock(lock);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (root.getParams().isDynamicCollect() && Collect.enabled) {
                CollectDetect.leaveInstrumentationCode();
            }
        }
    }

    private void try2MergeAndSave(String filename, String mergeSrc,
            boolean scales) throws Exception {
        boolean srcExists = mergeSrc != null && new File(mergeSrc).exists();

        if (srcExists) {
            mergeResults(filename, mergeSrc, scales);
        } else {
            saveResults(filename);
        }

    }

//    public static FileSaver getFileSaver(DataRoot root, MERGE merge, boolean insertOnly) {
//
//        return getFileSaver(root, MiscConstants.JcovSaveFileNameXML, MiscConstants.JcovTemplateFileNameXML, merge, insertOnly, false);
//    }
//
    public static FileSaver getFileSaver(DataRoot root, String filename, String template, MERGE merge) {

        return getFileSaver(root, filename, template, merge, false, false);
    }
    protected boolean agentdata = false;

    public void setAgentData(boolean agentdata) {
        this.agentdata = agentdata;
    }

    public static FileSaver getFileSaver(DataRoot root, String filename, String template, MERGE merge, boolean agentData) {
        FileSaver fileSaver = getFileSaver(root, filename, template, merge, false, false);
        fileSaver.agentdata = agentData;
        return fileSaver;
    }

    public static FileSaver getFileSaver(DataRoot root, boolean insertOnly) {

        return getFileSaver(root, MiscConstants.JcovSaveFileNameXML, MiscConstants.JcovTemplateFileNameXML, MERGE.MERGE, insertOnly, false);
    }

    public static FileSaver getFileSaver(DataRoot root, String filename, String template, MERGE merge, boolean insertOnly, boolean scales) {
        FileSaver saver = new JCovXMLFileSaver(root, filename, template, merge, scales);
        saver.insertOnly = insertOnly;
        return saver;
    }

    public static void setDisableAutoSave(boolean flag) {
        Collect.saveAtShutdownEnabled = !flag;
    }

    public static void addAutoShutdownSave() {
        PropertyFinder.addAutoShutdownSave();
    }
}
