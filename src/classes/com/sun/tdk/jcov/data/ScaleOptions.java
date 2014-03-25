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
package com.sun.tdk.jcov.data;

import com.sun.tdk.jcov.tools.ScaleCompressor;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 *
 * @author Sergey Borodin
 */
public final class ScaleOptions {

    public ScaleOptions() {
    }

    public ScaleOptions(boolean read_scales, int scale_size, ScaleCompressor compressor) {
        setReadScales(read_scales);
        setScaleSize(scale_size);
        setScaleCompressor(compressor);
    }
    private boolean read_scales;
    private int scale_size;
    private boolean scales_compressed;
    private ScaleCompressor compressor;
    private String[] testIDs;
    private String outTestList;

    public void setReadScales(boolean read_scales) {
        this.read_scales = read_scales;
    }

    public boolean needReadScales() {
        return read_scales;
    }

    public void setScaleSize(int scale_size) {
        this.scale_size = scale_size;
    }

    public int getScaleSize() {
        return scale_size;
    }

    public void setScalesCompressed(boolean scales_compressed) {
        this.scales_compressed = scales_compressed;
    }

    public boolean scalesCompressed() {
        return scales_compressed;
    }

    public void setScaleCompressor(ScaleCompressor compressor) {
        this.compressor = compressor;
    }

    public ScaleCompressor getScaleCompressor() {
        return compressor;
    }

    public boolean hasCompressor() {
        return compressor != null;
    }

    public void setTestList(String[] testIDs) {
        this.testIDs = testIDs;
    }

    public String[] getTestList() {
        return testIDs;
    }

    public void setOutTestList(String outTestList) {
        this.outTestList = outTestList;
    }

    public String getOutTestList() {
        return outTestList;
    }

    public void writeObject(DataOutput out) throws IOException {
        out.writeBoolean(read_scales);
        out.writeShort(scale_size);
        out.writeBoolean(scales_compressed);

        if (compressor != null) {
            out.writeBoolean(true);
            out.writeUTF(compressor.getClass().getName());
        } else {
            out.writeBoolean(false);
        }

        if (testIDs != null) {
            out.writeShort(testIDs.length);
            for (int i = 0; i < testIDs.length; ++i) {
                out.writeUTF(testIDs[i]);
            }
        } else {
            out.writeShort(Short.MAX_VALUE);
        }

        if (outTestList != null) {
            out.writeBoolean(true);
            out.writeUTF(outTestList);
        } else {
            out.writeBoolean(false);
        }
    }

    public ScaleOptions(DataInput in) throws IOException {
        read_scales = in.readBoolean();
        scale_size = in.readShort();
        scales_compressed = in.readBoolean();

        if (in.readBoolean()) {
            in.readUTF(); // compressor name
        }

        int len = in.readShort();
        if (len != Short.MAX_VALUE) {
            testIDs = new String[len];
            for (int i = 0; i < len; ++i) {
                testIDs[i] = in.readUTF();
            }
        } else {
            testIDs = null;
        }

        if (in.readBoolean()) {
            outTestList = in.readUTF();
        }
    }
}