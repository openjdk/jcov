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
import com.sun.tdk.jcov.util.Utils;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * <p> This class represents the so-called 'test scale'. Lets assume that N
 * tests have been run against the code under test and some tests have covered a
 * particular item (method, block, branch) and some - have not. It can be
 * written as a sequence of '0' and '1', where the position of '0' or '1' equals
 * the test number and the value '0' means that the test has *not* covered the
 * item, '1' - the test *has* covered the item. This sequence is the test scale
 * (TS) for this item. Jcov file format allows to keep TSs for every item of all
 * classes. Using only '1' and '0' would lead to too long TSs, so jcov file
 * format uses the following method for keeping *uncompressed* TSs: </p> <p> Let
 * TS be
 * <code>0100 1011 1001 101</code>. Then it is broken into 4-s of '0' and '1'-s
 * so that each quadruple represents a value between 0 and 15 (inclusive), and
 * therefore unambiguously corresponds to a hexadecimal digit with this value.
 * Thus given binary sequence is transformed into "4b95" hex character sequence
 * (last triple is left-padded with zero : 101 -> 0101). Such character forms of
 * TSs are kept in jcov data files. They can be either compressed or not. If the
 * TS is compressed, Jcov uses some compressor (now it is only
 * SimpleScaleCompressor) to decompress it and convert to the internal form
 * which is straightforward: test number N corresponds to N'th bit in a byte
 * array. Again, if Jcov is instructed to compress test scales it uses a
 * compressor, which converts a byte array into a sequence of characters (since
 * jcov data file must be a plain ascii file). </p> <p> For example, when
 * merging two data files (A.xml and B.xml), Jcov also merges TSs of the
 * corresponding items. Lets assume, that file A.xml has compressed TSs, file
 * B.xml - uncompressed TSs. Then to merge two test scales the following is done
 * : </p>
 * <pre>
 * 1. the sequence S1 of chars (corresponding to TS1) is read from A.xml
 * 2. S1 is decompressed by a compressor into a sequence of bits (byte array), e.g. :
 * <code>
 * byte1 |   byte0    byte index in the byte array
 *  __01 | 0100 1001  Test scale (as stored in memory)
 *    98 | 7654 3210  Test number (bit number)
 * </code> 3. other actions 4. the sequence S2 of chars (corresponding to TS2)
 * is read from B.xml 5. S2 is straightforwardly transformed into a sequence of
 * bits, e.g. :
 * <code>
 *    01 0110  Test scale (as stored in memory)
 *    54 3210  Test number (bit number)
 * </code> 6. other actions 7. TS1 and TS2 are merged :
 * <code>
 *                                 byte1    byte0    byte index in resulting byte array
 *                               ... ..98 7654 3210  test number (bit number)
 *  10 0100 1001 "+" 01 0110 "=" 0101 1010 0100 1001  resulting TS
 *                               |-TS2-||--- TS1---|
 * </code>
 * </pre>
 *
 * @see com.sun.tdk.jcov.tools.ScaleCompressor
 * @see com.sun.tdk.jcov.tools.SimpleScaleCompressor
 * @author Konstantin Bobrovsky
 */
public class Scale {

    public final static String sccsVersion = "%I% $LastChangedDate: 2012-06-20 12:45:52 +0400 (Wed, 20 Jun 2012) $";
    private static Scale buf_scale;

    static {
        buf_scale = new Scale();
        buf_scale.bytes = new byte[1024];
    }
    /**
     * keeps the internal (always uncompressed) form of the TS
     */
    protected byte[] bytes;
    /**
     * number of tests represented by this scale
     */
    protected int size;

    /**
     * default constructor
     */
    private Scale() {
    }

    /**
     * Constructs new Scale object
     *
     * @param scale TS in character form
     * @param len number of characters in the TS
     * @param size TS size (number of tests)
     * @param compressor scale compressor that can decompress the TS if it is
     * compressed
     * @param compressed whether the TS is compressed
     * @exception JcovFileFormatException if compressor is unable to decompress
     * the TS
     */
    public Scale(char[] scale, int len, int size, ScaleCompressor compressor, boolean compressed)
            throws FileFormatException {
        this.size = size;
        bytes = new byte[bytesRequiredFor(size)];
        if (compressed) {
            try {
                compressor.decompress(scale, len, bytes, size);
            } catch (Exception e) {
                throw new FileFormatException(e);
            }
        } else {
            int l = (size + 7) / 8;
            for (int i = 0; i < l; i++) {
                int ind = 2 * i;
                char val = scale[ind];
                byte b1 = val >= 'a' ? (byte) (10 + val - 'a') : (byte) (val - '0');
                if (ind + 1 < scale.length) {
                    val = scale[ind + 1];
                    byte b2 = val >= 'a' ? (byte) (10 + val - 'a') : (byte) (val - '0');
                    bytes[i] = (byte) (b1 | (b2 << 4));
                } else {
                    bytes[i] = b1;
                }
            }
            if (1 <= size % 8 && size % 8 <= 4) {
                int lastByte = bytes.length - 1;
                bytes[lastByte] = (byte) (bytes[lastByte] & 15);
            }

        }
    }

    /**
     * Returns size of this scale. Size of the scale equals the number of merged
     * tests
     *
     * @return size of this scale. Size of the scale equals the number of merged
     * tests
     */
    public int size() {
        return size;
    }

    /**
     * Returns count of "1" in this scale. This number equals the number of
     * tests that hit this block
     *
     * @return count of "1" in this scale. This number equals the number of
     * tests that hit this block
     */
    public int getSetBitsCount() {
        int res = 0;
        for (int i = 0; i < size; i++) {
            if (isBitSet(i)) {
                res++;
            }
        }
        return res;
    }

    /**
     * Checks whether pos's bit in the scale is set
     *
     * @return true if pos's bit in the scale is set, false otherwise. True
     * means that test #pos made hit on this block.
     */
    public boolean isBitSet(int pos) {
        if (pos < 0 || pos >= size) {
            return false;
        }
        int ind = pos / 8;
        int sft = pos % 8;
        byte val = bytes[ind];
        byte mask = (byte) (1 << sft);
        return (val & mask) != 0;
    }

    /**
     * Set scale value on test #pos
     *
     * @param pos bit to set
     * @param to_one value to set (1 or 0)
     */
    public void setBit(int pos, boolean to_one) {
        if (pos < 0 || pos >= size) {
            return;
        }
        int ind = pos / 8;
        int sft = pos % 8;
        byte val = bytes[ind];
        byte mask = (byte) (1 << sft);
        if (to_one) {
            bytes[ind] = (byte) ((val | mask) & 0xFF);
        } else {
            bytes[ind] = (byte) ((val & ~mask) & 0xFF);
        }
    }

    /**
     * @param scale_size TS size
     * @return number of bytes required to keep TS of given size
     */
    static int bytesRequiredFor(int scale_size) {
        return (scale_size + 8 - 1) / 8;
    }

    static Scale createZeroScale(int size) {
        Scale res = new Scale();
        res.size = size;
        res.bytes = new byte[bytesRequiredFor(size)];
        return res;
    }

    void addZeroes(int num, boolean add_before) {
        int new_size = num + size;
        int new_len = bytesRequiredFor(new_size);
        int bytes_extra = new_len - bytes.length;
        byte[] sav = bytes;
        if (bytes_extra > 0) {
            bytes = new byte[sav.length + bytes_extra];
        }
        if (add_before) {
            if (buf_scale.bytes.length < sav.length) {
                buf_scale.bytes = new byte[sav.length];
            }
            System.arraycopy(sav, 0, buf_scale.bytes, 0, sav.length);
            buf_scale.size = size;
            for (int i = 0; i < bytes.length; bytes[i++] = (byte) 0);
            this.size = num;
            merge(this, buf_scale);
        } else {
            if (bytes_extra > 0) {
                System.arraycopy(sav, 0, bytes, 0, sav.length);
            }
            int len = bytesRequiredFor(size);
            int rmndr = len * 8 - size;
            if (rmndr > 0) {
                byte mask = (byte) ((1 << (8 - rmndr)) - 1);
                bytes[len - 1] &= mask;
            }
            for (int i = len; i < new_len; bytes[i++] = (byte) 0);
            size = new_size;
        }
    }

    /**
     * Merges two scales and returns the result scale (new object)
     *
     * @param s1 scale information in block1. Can be null.
     * @param s2 scale information in block2. Can be null.
     * @param count1 count of hits in block1
     * @param count2 count of hits in block2
     * @return Merged (or created) Scale
     */
    public static Scale merge(Scale s1, Scale s2, long count1, long count2) {
        Scale res;
        if (s1 == null) {
            if (s2 == null) {
                res = Scale.merge(count1, count2);
            } else {
                res = Scale.merge(count1, s2);
            }
        } else {
            if (s2 == null) {
                Scale.merge(s1, count2);
            } else {
                Scale.merge(s1, s2);
            }
            res = s1;
        }

        return res;
    }

    /**
     * Given 2 exec counters of a jcov item (obtained from 2 testruns), creates
     * TS of size 2 for it.
     *
     * @param count0 execution counter of a jcov item obtained from the 1st
     * testrun
     * @param count1 execution counter of a jcov item obtained from the 2nd
     * testrun
     */
    static Scale merge(long count0, long count1) {
        Scale res = new Scale();
        res.bytes = new byte[1];
        res.size = 2;
        if (count0 > 0) {
            count0 = 1;
        }
        if (count1 > 0) {
            count1 = 1;
        }
        res.bytes[0] = (byte) (count0 | (count1 << 1));
        return res;
    }

    /**
     * Creates new test scale by merging a test scale of size 1 represented only
     * by an execution counter with another (normal) test scale
     */
    static Scale merge(long count, Scale scale) {
        Scale res = new Scale();
        res.bytes = new byte[bytesRequiredFor(scale.size + 1)];
        res.size = 1;
        if (count > 0) {
            res.bytes[0] = 1;
        }
        merge(res, scale);
        return res;
    }

    /**
     * Merges a test scale with another test scale of size 1 represented only by
     * an execution counter, and writes the result to the 1st test scale
     */
    static void merge(Scale scale, long count) {
        int i = scale.size / 8;
        int j = scale.size % 8;
        scale.size++;
        if (i >= scale.bytes.length) {
            byte[] sav = scale.bytes;
            scale.bytes = new byte[i + 1];
            System.arraycopy(sav, 0, scale.bytes, 0, i);
            sav = null;
        }
        if (count == 0) {
            return;
        }
        scale.bytes[i] |= (byte) (1 << j);
    }

    /**
     * Merges two test scales, writing the result to the 1st
     *
     * @see Scale
     */
    static void merge(Scale dst, Scale src) {
        // both dst.bytes and src.bytes can be longer than
        // it is necessary to hold actual scales
        int old_dst_len = bytesRequiredFor(dst.size);
        int new_dst_len = bytesRequiredFor(dst.size + src.size);
        int src_len = bytesRequiredFor(src.size);
        // does dst have enough space to accomodate both scales?
        if (dst.bytes.length * 8 < dst.size + src.size) {
            byte[] sav = dst.bytes;
            // no - create minimum space required
            dst.bytes = new byte[new_dst_len];
            System.arraycopy(sav, 0, dst.bytes, 0, old_dst_len);
            sav = null; // should now be gc'ed
        }
        byte[] dst_bytes = dst.bytes;
        byte[] src_bytes = src.bytes;
        if (dst.size % 8 == 0) {
            // dst scale occupied exactly old_dst_len * 8 bits
            System.arraycopy(src_bytes, 0, dst_bytes, old_dst_len, src_len);
            dst.size += src.size;
        } else {
            int dst_rmndr = dst.size % 8;
            int src_rmndr = src.size % 8;
            byte dst_mask = (byte) ((1 << dst_rmndr) - 1);
            // copy src scale to dst shifting it (8 - dst_rmndr)
            // bits right
            int i, j;
            for (i = old_dst_len - 1, j = 0; j < src_len - 1; i++, j++) {
                dst_bytes[i] = (byte) (((dst_bytes[i] & dst_mask) | (src_bytes[j] << dst_rmndr)) & 0xFF);
                dst_bytes[i + 1] = (byte) ((src_bytes[j] >>> (8 - dst_rmndr)) & 0xFF);
            }

            // copy last byte
            dst_bytes[i] = (byte) ((dst_bytes[i] & dst_mask) | (src_bytes[j] << dst_rmndr));
            if (i < dst_bytes.length - 1) {
                int src_mask = (int) ((src_rmndr == 0) ? 0xFF : (1 << src_rmndr) - 1);
                dst_bytes[i + 1] = (byte) (((src_bytes[j] & src_mask) >>> (8 - dst_rmndr)) & 0xFF);
            }
            dst.size += src.size;
        }
    }

    /**
     * <p> Converts this scale to character form </p> <p> Character
     * representation of a Scale is encoded 16-bit mask: 0123456789abcdef. </p>
     *
     * @param compress whether the character form will be compressed
     * @param buf a StringBuffer that will keep the character form
     * @param compressor compressor used to compress the TS
     * @return number of characters representing the TS
     * (compressed/uncompressed)
     */
    public int convertToChars(boolean compress,
            StringBuffer buf,
            ScaleCompressor compressor) {
        if (compress) {
            return compressor.compress(bytes, buf, size);
        } else {
            int res = Utils.halfBytesRequiredFor(size);
            for (int i = res - 1; i >= 0; i--) {
                buf.setCharAt(i, Utils.int2HexChar(Utils.getHalfByteAt(i, bytes)));
            }
            return res;
        }
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer(size + size / 4);
        for (int i = 0; i < size; i++) {
            if (i % 4 == 0 && i != 0 && i <= size - 1) {
                buf.append('_');
            }
            char ch = isBitSet(i) ? '1' : '0';
            buf.append(ch);
        }
        return buf.toString();
    }

    public static Scale illuminateDuplicates(Scale scale, int new_size, ArrayList pairs) {
        if (scale == null) {
            return null;
        }

        // Merge values for duplicates
        for (Iterator it = pairs.iterator(); it.hasNext();) {
            Utils.Pair p = (Utils.Pair) (it.next());
            scale.setBit(p.a, scale.isBitSet(p.a) | scale.isBitSet(p.b));
        }
        Scale old = scale;
        scale = Scale.createZeroScale(new_size);

        /**
         * Moving forward simultaneously in new and old scales, skipping
         * duplicated indexes into old (p.b) and writing data into new scale
         */
        Iterator<Utils.Pair> it = pairs.iterator();
        int iremove = it.hasNext() ? it.next().b : new_size;
        int old_size = old.size;
        for (int inew = 0, iold = 0; inew < new_size && iold < old_size; inew++, iold++) {
            while (iold == iremove) {
                if (it.hasNext()) {
                    iremove = it.next().b;
                } else {
                    iremove = old_size;
                }
                iold++;
            }
            scale.setBit(inew, old.isBitSet(iold));
        }

        return scale;
    }

    public static Scale expandScale(Scale scale, int new_size, boolean add_before, long dataCount) {
        if (scale == null) {
            scale = Scale.createZeroScale(new_size);
            if (dataCount > 0) {
                int pos = add_before ? new_size - 1 : 0;
                scale.setBit(pos, true);
            }
        } else if (new_size > scale.size) {
            scale.addZeroes(new_size - scale.size, add_before);
            if (dataCount > 0) {
                int pos = add_before ? 0 : new_size - 1;
                scale.setBit(pos, true);
            }
        }

        return scale;
    }

    /**
     * Writes this scale data to stream
     *
     * @param out
     * @throws IOException
     */
    public void writeObject(DataOutput out) throws IOException {
        out.writeShort(bytes.length);
        out.write(bytes);
    }

    /**
     * Creates Scale instance reading it from the stream
     *
     * @param in
     * @throws IOException
     */
    public Scale(DataInput in) throws IOException {
        int len = in.readShort();
        bytes = new byte[len];
        in.readFully(bytes);
    }
}