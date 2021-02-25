/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tdk.jcov.Merger;
import com.sun.tdk.jcov.instrument.DataClass;
import com.sun.tdk.jcov.instrument.DataPackage;
import com.sun.tdk.jcov.instrument.DataRoot;
import com.sun.tdk.jcov.instrument.InstrumentationOptions;
import com.sun.tdk.jcov.instrument.XmlContext;
import com.sun.tdk.jcov.io.Reader;
import com.sun.tdk.jcov.util.RuntimeUtils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.List;

/**
 *
 * @author Sergey Borodin
 */
public class JCovXMLFileSaver extends FileSaver {

    // private final static boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

    public JCovXMLFileSaver(DataRoot root, InstrumentationOptions.MERGE mergeMode) {
        super(root, mergeMode);
    }

    public JCovXMLFileSaver(DataRoot root, String filename, String template,
            InstrumentationOptions.MERGE merge, boolean scales) {
        super(root, filename, template, merge, scales);
    }

    public void saveResults(String filename) throws Exception {
        //if (isWindows) {
            File file = new File(filename);
            FileChannel channel = new RandomAccessFile(file, "rw").getChannel();

            for (int i = LOCK_REPEAT; i != 0; i--) {
                try {
                    FileLock lock = channel.tryLock();
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    XmlContext ctx = new XmlContext(os, root.getParams());
                    ctx.setSkipNotCoveredClasses(agentdata);
                    root.xmlGen(ctx);
                    ctx.close();

                    channel.truncate(0);
                    channel.write(ByteBuffer.wrap(os.toByteArray()));
                    lock.release();
                    channel.close();
                    return;
                } catch (OverlappingFileLockException e) {
                    Thread.sleep(LOCK_SLEEP);
                }
            }
        /*} else {
            XmlContext ctx = new XmlContext(filename, root.getParams());
            ctx.setSkipNotCoveredClasses(agentdata);
            root.xmlGen(ctx);
            ctx.close();
        }*/
    }

    void mergeResults(String dest, String src, boolean scale) throws Exception {
        if (insertOnly) {
            String tmpname = dest + ".tmp";
            saveResults(tmpname);

            try {
                root = Reader.readXML(src, scale, null);
                DataRoot templRoot = Reader.readXML(tmpname, scale, null);
                List<DataPackage> packs = root.getPackages();

                for (DataPackage templPack : templRoot.getPackages()) {
                    boolean pInserted = false;
                    for (DataPackage pack : packs) {
                        if (!pack.getName().equals(templPack.getName())) {
                            continue;
                        }
                        for (DataClass clazzNew : templPack.getClasses()) {
                            boolean cInserted = false;
                            for (DataClass clazz : pack.getClasses()) {
                                if (clazz.getName().equals(clazzNew.getName())) {
                                    cInserted = true;
                                    break;
                                }
                            }
                            if (!cInserted) {
                                pack.getClasses().add(clazzNew);
                            }
                        }
                        pInserted = true;
                        break; // there can't be 2 similar packages
                    }
                    if (!pInserted) {
                        root.addPackage(templPack);
                    }
                }

                root.setCount(templRoot.getCount());

                templRoot.destroy();

                saveResults(dest);

            } catch (Exception e) {
                throw new Error(e);
            } finally {
                new File(tmpname).delete();
            }

        } else {
            String tmpFileName = dest + RuntimeUtils.genSuffix();
            try {
                this.saveResults(tmpFileName);
                if (scale) {
                    Merger.main(new String[]{"-output", dest, "-scale", "-compress", tmpFileName, src});
                } else {
                    Merger.main(new String[]{"-output", dest, tmpFileName, src});
                }
            } finally {
                new File(tmpFileName).delete();
            }
        }
    }
}
