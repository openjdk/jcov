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
package com.sun.tdk.jcov.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

/**
 * Class that implements comparison of String in natural order: a1, a2, ...,
 * a10, a11
 *
 * @author Dmitry Fazunenko
 */
public final class NaturalComparator implements Comparator<String> {

    private NaturalComparator() {
    }
    public static final NaturalComparator INSTANCE = new NaturalComparator();
    private final HashMap<String, NaturalComparator.CompoundString> hash = new HashMap<String, NaturalComparator.CompoundString>();

    /**
     * {@inheritDoc} <p> Null values are allowed. In which case returns a
     * positive integer, negative integer, or zero as the first argument is
     * null, second argument is null or both are null.
     */
    @Override
    public int compare(String s1, String s2) {
        if (s1 != null && s2 != null) {
            return getCS(s1).compareTo(getCS(s2));
        } else if (s1 != null) {
            return -1;
        } else if (s2 != null) {
            return 1;
        } else {
            return 0;
        }
    }

    private NaturalComparator.CompoundString getCS(String s) {
        NaturalComparator.CompoundString cs = hash.get(s);
        if (cs == null) {
            cs = new NaturalComparator.CompoundString(s);
            hash.put(s, cs);
        }
        return cs;
    }

    /**
     * class representing strings which are mix of digits and chars
     */
    static class CompoundString implements Comparable<NaturalComparator.CompoundString> {

        ArrayList<String> chars;
        ArrayList<Long> nums;

        CompoundString(String s) {
            chars = new ArrayList<String>();
            nums = new ArrayList<Long>();
            final int len = s.length();
            int prev = 0;
            int pos = 0;
            while (true) {
                // looking for chars
                while (pos < len && !Character.isDigit(s.charAt(pos))) {
                    pos++;
                }
                if (pos == len) {
                    chars.add(s.substring(prev));
                    break;
                }
                // digit found!
                chars.add(s.substring(prev, pos));
                prev = pos;
                pos++;
                while (pos < len && Character.isDigit(s.charAt(pos))) {
                    pos++;
                }
                if (pos == len) {
                    nums.add(Long.parseLong(s.substring(prev)));
                    break;
                }
                nums.add(Long.parseLong(s.substring(prev, pos)));
                prev = pos;
            }


        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < chars.size(); i++) {
                sb.append(chars.get(i));
                if (i < nums.size()) {
                    sb.append(nums.get(i));
                }
            }
            return sb.toString();
        }

        @Override
        public int compareTo(NaturalComparator.CompoundString s2) {
            int k = 0;
            while (true) {
                // compare char part k
                if (k < chars.size() && k < s2.chars.size()) {
                    int res = chars.get(k).compareTo(s2.chars.get(k));
                    if (res != 0) {
                        return res;
                    }
                } else {
                    // one str is shorter than another
                    return chars.size() - s2.chars.size();
                }
                // compare numbs part k
                if (k < nums.size() && k < s2.nums.size()) {
                    int res = nums.get(k).compareTo(s2.nums.get(k));
                    if (res != 0) {
                        return res;
                    }
                } else {
                    // one str is shorter than another
                    return nums.size() - s2.nums.size();
                }
                k++;
            }
        }
    }
}
