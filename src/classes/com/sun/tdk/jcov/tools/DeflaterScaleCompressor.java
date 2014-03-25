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
package com.sun.tdk.jcov.tools;

import com.sun.tdk.jcov.util.Utils;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Implements a scale compressor which compresses/decompresses test scales using
 * standard zip-compression algorithms. Since zip-compressed data may contain
 * unprintable characters and jcov data file is a plain ASCII file, this
 * compressor (when doing compression) converts each 4-bytes of the compressed
 * data to a hex-digit character.
 *
 * @see ScaleCompressor
 * @see com.sun.tdk.jcov.filedata.Scale
 * @author Konstantin Bobrovsky
 */
public class DeflaterScaleCompressor implements ScaleCompressor {

    public final static String sccsVersion = "%I% $LastChangedDate: 2013-09-30 17:48:28 +0400 (Mon, 30 Sep 2013) $";
    protected static byte[] buf = new byte[2048];
    protected static final Deflater def = new Deflater(Deflater.BEST_COMPRESSION, true);
    protected static final ByteArrayOutputStream bos = new ByteArrayOutputStream(512);
    protected static final DeflaterOutputStream dos = new DeflaterOutputStream(bos, def);
    protected static final Inflater inf = new Inflater(true);
    protected static final ByteArrayInputStream bis = new ByteArrayInputStream(buf);
    protected static final InflaterInputStream ios = new InflaterInputStream(bis, inf);

    /**
     * @see ScaleCompressor#decompress(char[], int, byte[], int)
     */
    public void decompress(char[] src, int len, byte[] dst, int bits_total) throws Exception {
        for (int i = 0; i < len; i++) {
            Utils.writeHalfByteAt(Utils.hexChar2Int(src[i]), i, buf);
        }
        int buf_len = (bits_total + 7) / 8;
        try {
            ios.read(dst, 0, buf_len);
            inf.reset();
            bis.reset();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("invalid compression");
        }
    }

    /**
     * @see ScaleCompressor#compress(byte[], StringBuffer, int)
     */
    public int compress(byte[] src, StringBuffer dst, int bits_total) {
        try {
            dos.write(src, 0, (bits_total + 7) / 8);
            dos.finish();
            dos.flush();
            bos.flush();
        } catch (IOException e) {
            return -1;
        }
        byte[] res = bos.toByteArray();
        def.reset();
        bos.reset();
        if (res.length * 2 > dst.length()) {
            dst.setLength(res.length * 2);
        }
        for (int i = 0; i < res.length * 2; i++) {
            dst.setCharAt(i, Utils.int2HexChar(Utils.getHalfByteAt(i, res)));
        }
        return res.length * 2;
    }
}
