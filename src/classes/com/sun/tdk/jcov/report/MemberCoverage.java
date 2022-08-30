/*
 * Copyright (c) 2014, 2022 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.jcov.report;

import com.sun.tdk.jcov.data.Scale;
import com.sun.tdk.jcov.instrument.Modifiers;
import com.sun.tdk.jcov.util.Utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for Method and Field Coverage.
 *
 * @author Dmitry Fazunenko
 */
public abstract class MemberCoverage extends AbstractCoverage {

    protected long count;
    protected int startLine;
    protected String name;
    protected String signature;
    protected String modifiersString;
    protected int access;
    protected Scale scale;
    protected Modifiers modifiers;

    /**
     * @return hit count.
     */
    public Long getHitCount() {
        return count;
    }

    /**
     * @return name of this member (field or method)
     */
    public String getName() {
        return name;
    }

    /**
     * @return true when at least 1 hit was collected
     */
    public boolean isCovered() {
        return getHitCount() > 0;
    }

    /**
     * @return signature of this member (field or method)
     */
    public String getSignature() {
        return signature;
    }

    /**
     * Returns member name and signature in JLS format
     *
     * @return Member signature in readable form (JLS)
     */
    public String getReadableSignature() {
        return Utils.convertVMtoJLS(name, signature);
    }

    /**
     * @return String-encoded modifiers
     */
    public String getModifiersString() {
        return modifiersString;
    }

    /**
     * @return start line in source file
     */
    public int getStartLine() {
        return startLine;
    }

    @Override
    public boolean isCoveredByTest(int testnum) {
        return scale != null && scale.isBitSet(testnum);
    }

    /**
     * <p> In contract from getCoveringTests(String[]) this method uses
     * information about how many tests were run from scale information itself.
     * </p>
     *
     * @return Numbers of tests which covered this method. Will return
     * Collections.EMPTY_LIST when scales were not read
     */
    public List<Integer> getCoveringTests() {
        if (scale == null) {
            return Collections.EMPTY_LIST;
        }
        ArrayList<Integer> list = new ArrayList<Integer>(scale.size() / 10);
        for (int i = 0; i < scale.size(); ++i) {
            if (scale.isBitSet(i)) {
                list.add(i);
            }
        }
        return list;
    }

    /**
     * <b> Use scales to find out which code was covered by some specific test.
     * Scale contains a bit-mask, first bit refers to the first test in the
     * testlist, second bit to the second test and so on. For example "1101"
     * scale means that the method was hit by 1, 2 and 4 tests in the testlist.
     * </b>
     *
     * @return Scale information of this method. Can be null if data was read
     * without scales.
     * @see Scale
     */
    public Scale getScale() {
        return scale;
    }

    /**
     * @return Number of tests that were run against this member. This number
     * should be similar for the entire product.
     */
    public int getScaleSize() {
        return scale.size();
    }

    /**
     * <p> Use getAccess() method to check for more specific modifiers.
     * getAccess() method returns a bit-mask of org.objectweb.asm.Opcodes
     * constants. </p>
     *
     * @return true if member access modifiers are <b>public</b> or
     * <b>protected</b>
     * @see ClassCoverage#getAccess()
     */
    public boolean isPublicAPI() {
        return modifiers.isPublic() || modifiers.isProtected();
    }

    /**
     * <p> Use getAccess() method to check for more specific modifiers.
     * getAccess() method returns a bit-mask of org.objectweb.asm.Opcodes
     * constants. </p>
     *
     * @return true if member is <b>public</b> or <b>protected</b>
     * @see ClassCoverage#getAccess()
     */
    public boolean isPublic() {
        return modifiers.isPublic();
    }

    /**
     * <p> Use getAccess() method to check for more specific modifiers.
     * getAccess() method returns a bit-mask of org.objectweb.asm.Opcodes
     * constants. </p>
     *
     * @return true if member is <b>public</b>
     * @see ClassCoverage#getAccess()
     */
    public boolean isPrivate() {
        return modifiers.isPrivate();
    }

    /**
     * <p> Use getAccess() method to check for more specific modifiers.
     * getAccess() method returns a bit-mask of org.objectweb.asm.Opcodes
     * constants. </p>
     *
     * @return true if member is <b>protected</b>
     * @see ClassCoverage#getAccess()
     */
    public boolean isProtected() {
        return modifiers.isProtected();
    }

    /**
     * <p> Use getAccess() method to check for more specific modifiers.
     * getAccess() method returns a bit-mask of org.objectweb.asm.Opcodes
     * constants. </p>
     *
     * @return true if member is <b>abstract</b>
     * @see ClassCoverage#getAccess()
     */
    public boolean isAbstract() {
        return modifiers.isAbstract();
    }

    /**
     * <p> Use getAccess() method to check for more specific modifiers.
     * getAccess() method returns a bit-mask of org.objectweb.asm.Opcodes
     * constants. </p>
     *
     * @return true if member is <b>final</b>
     * @see ClassCoverage#getAccess()
     */
    public boolean isFinal() {
        return modifiers.isFinal();
    }

    /**
     * <p> Use this method to check for specific modifiers. </p>
     *
     * @return Access bit-mask of org.objectweb.asm.Opcodes constants.
     */
    public int getAccess() {
        return access;
    }
}