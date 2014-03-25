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

/**
 * Implements a scale compressor, which quite efficient in compressing test
 * scales containing long sequences of the same character. The basic algorithm
 * is the following : Sequence of N characters &lt;ch&gt; (hex digits actually)
 * is transformed into the sequence &lt;Nrep&gt;&lt;ch&gt;, where Nrep
 * represents value N written in a big-radix numeric system. Digits of this
 * system do not include possible &lt;ch&gt;'s (i.e. hex digits) providing an
 * unambiguous way of backward transformation.
 *
 * @see ScaleCompressor
 * @see com.sun.tdk.jcov.filedata.Scale
 * @author Konstantin Bobrovsky
 */
public class SimpleScaleCompressor implements ScaleCompressor {

    public final static String sccsVersion = "%I% $LastChangedDate: 2009-06-08 18:52:39 +0400 (Mon, 08 Jun 2009) $";
    char[] buf = new char[32];

    /**
     * @see ScaleCompressor#decompress(char[], int, byte[], int)
     */
    public void decompress(char[] src, int len, byte[] dst, int scale_size) throws Exception {

        int dst_ind = 0;
        int dst_len = Utils.halfBytesRequiredFor(scale_size);
        int src_ind = 0;
        do {
            char ch;
            int buf_cnt = 0;
            byte hex_val = -1;

            try {
                while (src_ind < len) {
                    ch = src[src_ind++];
                    hex_val = Utils.hexChar2Int(ch);
                    if (hex_val >= 0) {
                        break;
                    }
                    buf[buf_cnt++] = ch;
                }
                if (hex_val < 0) {
                    throw new Exception("malformed scale");
                }

                int i, expand_cnt = (buf_cnt == 0) ? 1 : 0;
                for (i = 0, buf_cnt--; buf_cnt >= 0; buf_cnt--, i++) {
                    expand_cnt += Utils.convert2Int(buf[buf_cnt]) * Utils.pow(Utils.radix, i);
                }
                if (dst_ind + expand_cnt > dst_len) {
                    throw new ArithmeticException();
                }
                for (i = 0; i < expand_cnt; i++) {
                    Utils.writeHalfByteAt(hex_val, dst_ind++, dst);
                }
            } catch (ArithmeticException e) {
                throw new Exception("invalid scale compression");
            } catch (IndexOutOfBoundsException e) {
                throw new Exception("invalid scale size");
            }
        } while (src_ind < len);
    }

    /**
     * @see ScaleCompressor#compress(byte[], StringBuffer, int)
     */
    public int compress(byte[] src, StringBuffer dst, int scale_size) {
        byte old_quad = Utils.getHalfByteAt(0, src);
        byte cur_quad;
        int digit_cnt = 1;
        int dst_ind = 0;

        if (scale_size <= 4) {
            dst.setCharAt(0, Utils.int2HexChar(old_quad));
            return 1;
        }
        int size = Utils.halfBytesRequiredFor(scale_size);
        for (int i = 1; i < size; i++) {
            cur_quad = Utils.getHalfByteAt(i, src);
            if (cur_quad != old_quad || i == size - 1) {
                if (cur_quad == old_quad) {
                    digit_cnt++;
                }
                if (digit_cnt > 2) {
                    // worth compression
                    dst_ind = Utils.convert2BigRadix(digit_cnt, dst, dst_ind);
                } else if (digit_cnt == 2) {
                    dst.setCharAt(dst_ind++, Utils.int2HexChar(old_quad));
                }
                dst.setCharAt(dst_ind++, Utils.int2HexChar(old_quad));
                if (i == size - 1 && cur_quad != old_quad) {
                    dst.setCharAt(dst_ind++, Utils.int2HexChar(cur_quad));
                }
                digit_cnt = 1;
            } else {
                digit_cnt++;
            }
            old_quad = cur_quad;
        }
        return dst_ind;
    }
}
