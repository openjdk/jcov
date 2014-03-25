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

/**
 * Interface for a test scale compressor.
 *
 * @author Konstantin Bobrovsky
 */
public interface ScaleCompressor {

    /**
     * Takes &lt;src&gt; as a symbol representation of the test scale and
     * transforms (decompresses) it into canonical representation (as a sequence
     * of bits) writing result to &lt;dst&gt;
     *
     * @param src symbol representation of the test scale
     * @param len number of symbols in &lt;src&gt; (deduced from
     * &lt;scale_size&gt;)
     * @param dst storage for canonical representation
     * @param scale_size test scale size (deduced from &lt;len&gt;)
     * @exception Exception if a decompression error occurs
     * @see com.sun.tdk.jcov.filedata.Scale
     */
    void decompress(char[] src, int len, byte[] dst, int scale_size) throws Exception;

    /**
     * Takes &lt;src&gt; as a canonical representation of the test scale and
     * transforms (compresses) it into a symbol representation writing the
     * result to &lt;dst&gt;.
     *
     * @param src canonical representation of the test scale
     * @param dst storage for symbol (compressed) representation
     * @param scale_size test scale size
     * @see com.sun.tdk.jcov.filedata.Scale
     */
    int compress(byte[] src, StringBuffer dst, int scale_size);
}
