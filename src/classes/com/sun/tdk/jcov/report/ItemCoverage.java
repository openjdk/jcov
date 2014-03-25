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
package com.sun.tdk.jcov.report;

import com.sun.tdk.jcov.data.Scale;
import com.sun.tdk.jcov.instrument.DataBlock;

/**
 * ItemCoverage has 2 inheritors: BlockCoverage and BranchCoverage. Use
 * isBlock() method to differ them if needed
 *
 * @author Dmitry Fazunenko
 * @author Alexey Fedorchenko
 */
public abstract class ItemCoverage extends AbstractCoverage {

    /**
     * Creates new block coverage data
     *
     * @param b
     * @return new block coverage data
     */
    public static ItemCoverage createBlockCoverageItem(DataBlock b) {
        return createBlockCoverageItem(b.startBCI(), b.endBCI(), b.getCount(), b.getScale());
    }

    /**
     * Creates new block coverage data
     *
     * @param startBCI
     * @param endBCI
     * @param cnt
     * @return new block coverage data
     */
    public static ItemCoverage createBlockCoverageItem(int startBCI, int endBCI, long cnt) {
        return new BlockCoverage(startBCI, endBCI, cnt, null);

    }

    /**
     * Creates new block coverage data
     *
     * @param startBCI
     * @param endBCI
     * @param cnt
     * @return new block coverage data
     */
    public static ItemCoverage createBlockCoverageItem(int startBCI, int endBCI, long cnt, Scale scale) {
        return new BlockCoverage(startBCI, endBCI, cnt, scale);

    }

    /**
     * Creates new branch coverage data
     *
     * @param b
     * @return new block coverage data
     */
    public static ItemCoverage createBranchCoverageItem(DataBlock b) {
        return createBranchCoverageItem(b.startBCI(), b.endBCI(), b.getCount(), b.getScale());
    }

    /**
     * Creates new branch coverage data
     *
     * @param startBCI
     * @param endBCI
     * @param cnt
     * @return new block coverage data
     */
    public static ItemCoverage createBranchCoverageItem(int startBCI, int endBCI, long cnt) {
        return new BranchCoverage(startBCI, endBCI, cnt, null);

    }

    /**
     * Creates new branch coverage data
     *
     * @param startBCI
     * @param endBCI
     * @param cnt
     * @return new block coverage data
     */
    public static ItemCoverage createBranchCoverageItem(int startBCI, int endBCI, long cnt, Scale scale) {
        return new BranchCoverage(startBCI, endBCI, cnt, scale);

    }
    protected final int startLine;
    protected final int endLine;
    protected long count;
    private int srcLine = -1;
    protected Scale scale;

    protected ItemCoverage(int startLine, int endLine, long count, Scale scale) {
        this.startLine = startLine;
        this.endLine = endLine;
        this.count = count;
        this.scale = scale;
    }

    protected void setSrcLine(int srcLine) {
        this.srcLine = srcLine;
    }

    /**
     * @return line position in source file
     */
    public int getSourceLine() {
        return srcLine;
    }

    /**
     * @return hit count
     */
    public long getCount() {
        return count;
    }

    /**
     * @return last line position in source file
     */
    public int getEndLine() {
        return endLine;
    }

    /**
     * @return first line position in source file
     */
    public int getStartLine() {
        return startLine;
    }

    /**
     * @return true if this object is instance of BlockCoverage
     */
    public abstract boolean isBlock();

    /**
     * @return true when any hit was collected
     */
    public boolean isCovered() {
        return count > 0;
    }

    @Override
    public boolean isCoveredByTest(int testnum) {
        return scale != null && scale.isBitSet(testnum);
    }

    @Override
    public String toString() {
        String b = isBlock() ? "BLOCK  " : "BRANCH ";
        return b + startLine + ":" + endLine + "   " + count;
    }

    /**
     * @return { DataType.BLOCK, DataType.BRANCH }
     * @see DataType
     */
    public static DataType[] getAllPossibleTypes() {
        return new DataType[]{DataType.BLOCK, DataType.BRANCH};
    }

    /**
     * Data covering blocks
     */
    private static class BlockCoverage extends ItemCoverage {

        private BlockCoverage(DataBlock dataBlock) {
            this(dataBlock.startBCI(), dataBlock.endBCI(), dataBlock.getCount(), dataBlock.getScale());
        }

        private BlockCoverage(int startBCI, int endBCI, long cnt, Scale scale) {
            super(startBCI, endBCI, cnt, scale);
        }

        /**
         * Coverage kind (used in HTML reports as header for label)
         *
         * @return DataType.BLOCK
         */
        public DataType getDataType() {
            return DataType.BLOCK;
        }

        /**
         * @return {DataType.BLOCK}
         */
        protected DataType[] getDataTypes() {
            return new DataType[]{DataType.BLOCK};
        }

        public CoverageData getData(DataType column) {
            return getData(column, -1);
        }

        public CoverageData getData(DataType column, int testNumber) {
            switch (column) {
                case BLOCK:
                    return new CoverageData(count == 0 ? 0 : 1, 1);
                default:
                    return new CoverageData();
            }
        }

        @Override
        public String getName() {
            return "block";
        }

        @Override
        public boolean isBlock() {
            return true;
        }
    }

    /**
     * Data covering branches
     */
    private static class BranchCoverage extends ItemCoverage {

        private BranchCoverage(int startBCI, int endBCI, long cnt, Scale scale) {
            super(startBCI, endBCI, cnt, scale);
        }

        /**
         * Coverage kind (used in HTML reports as header for label)
         *
         * @return DataType.BRANCH
         */
        public DataType getDataType() {
            return DataType.BRANCH;
        }

        /**
         * @return {DataType.BRANCH}
         */
        protected DataType[] getDataTypes() {
            return new DataType[]{DataType.BRANCH};
        }

        public CoverageData getData(DataType column) {
            return getData(column, -1);
        }

        public CoverageData getData(DataType column, int testNumber) {
            switch (column) {
                case BRANCH:
                    return new CoverageData(count == 0 ? 0 : 1, 1);
                default:
                    return new CoverageData();
            }
        }

        @Override
        public String getName() {
            return "branch";
        }

        @Override
        public boolean isBlock() {
            return false;
        }
    }
}
