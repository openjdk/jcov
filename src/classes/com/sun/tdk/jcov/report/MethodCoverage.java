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
import com.sun.tdk.jcov.instrument.DataMethod.LineEntry;
import java.util.Map;
import java.util.HashMap;
import com.sun.tdk.jcov.instrument.DataBlockTarget;
import com.sun.tdk.jcov.instrument.DataBlockTargetCond;
import com.sun.tdk.jcov.instrument.XmlNames;
import com.sun.tdk.jcov.instrument.DataBlock;
import com.sun.tdk.jcov.instrument.DataMethod;
import com.sun.tdk.jcov.util.Utils;
import org.objectweb.asm.Opcodes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import static com.sun.tdk.jcov.report.DataType.*;

/**
 * <p> This class provides access to method coverage information. Sums of
 * underlying coverage information can be obtained by
 * <code>getData(DataType)</code> method using DataType.METHOD, DataType.BLOCK,
 * DataType.BRANCH, DataType.LINE. </p> <p> To access specific method/field
 * information use
 * <code>getMethodCoverageList()</code> and
 * <code>getFieldCoverageList()</code> methods </p>
 *
 * @author Leonid Mesnik
 */
public class MethodCoverage extends MemberCoverage implements Iterable<ItemCoverage> {

    private LineCoverage lineCoverage = new LineCoverage();
    private List<ItemCoverage> items = new ArrayList<ItemCoverage>();
    private boolean inAnonymClass = false;
    private boolean lambdaMethod = false;
    private boolean anonymon = false;
    private DataType[] supportedColumns = {METHOD, BLOCK, BRANCH, LINE};
    private boolean isInAnc = false;
    protected String ancInfo;

    /**
     * <p> Creates new MethodCoverage instance without counting blocks </p>
     *
     * @param method DataMethod to read data from
     */
    public MethodCoverage(DataMethod method, AncFilter[] ancFilters, String ancReason) {
        this(method, false, ancFilters, ancReason);
    }

    public MethodCoverage(DataMethod method, boolean countBlocks) {
        this(method, countBlocks, null, null);
    }

    /**
     * <p> Creates new MethodCoverage instance </p>
     *
     * @param method
     * @param countBlocks not used
     */
    public MethodCoverage(DataMethod method, boolean countBlocks,  AncFilter[] ancFilters, String ancReason) {
        modifiers = Arrays.deepToString(method.getAccessFlags());
        name = method.getName();
        scale = method.getScale();
        signature = method.getVmSignature();
        access = method.getAccess();
        isInAnc = ancReason != null;
        ancInfo = ancReason;

        if (signature == null) {
            signature = "";
        }
        count = method.getCount();

        detectItems(method, items, isInAnc, ancFilters);

        List<LineEntry> lineTable = method.getLineTable();
        if (lineTable != null) {
            if (lineTable.size() > 0) {
                super.startLine = lineTable.get(0).line;
            }
            lineCoverage.processLineTable(lineTable);
            for (ItemCoverage item : items) {
                for (LineEntry le : lineTable) {
                    if (le.bci >= item.startLine && le.bci <= item.endLine) {
                        if (item.count > 0) {
                            lineCoverage.hitLine(le.line);
                        }
                        else {
                            if (isInAnc || item.isInAnc()) {
                                lineCoverage.markLineAnc(le.line);
                            }
                        }
                        if (item.getSourceLine() < 0) { // not set yet
                            item.setSrcLine(le.line);
                        }
                    }
                }
            }
        }
    }

    private static String isBlockInAnc(DataMethod m, DataBlock b , AncFilter[] filters, List<String> ancBlockReasons){
        if (filters == null){
            return null;
        }
        for (AncFilter filter : filters){
            if (filter.accept(m , b)){
                String ancReason = filter.getAncReason();
                if (ancBlockReasons.size() == 0) {
                    ancBlockReasons.add("All blocks are filtered:");
                }
                if (!ancBlockReasons.contains(ancReason)) {
                    ancBlockReasons.add(ancReason);
                }
                return ancReason;
            }
        }
        return null;
    }

    public void setAnonymOn(boolean anonym) {
        this.anonymon = anonym;
    }

    public void setInAnonymClass(boolean inAnonymClass) {
        this.inAnonymClass = inAnonymClass;
    }

    public boolean isInAnonymClass() {
        return inAnonymClass;
    }

    public void setLambdaMethod(boolean lambdaMethod) {
        this.lambdaMethod = lambdaMethod;
    }

    public boolean isLambdaMethod() {
        return lambdaMethod;
    }

    /**
     * Finds coverage items in terms of legacy jcov (blocks and branches)
     */
     void detectItems(DataMethod m, List<ItemCoverage> list, boolean isInAnc, AncFilter[] ancFilters) {
        Map<DataBlock, ItemCoverage> added = new HashMap<DataBlock, ItemCoverage>();
        List<String> ancBlockReasons = new ArrayList<String>();

        for (DataBlock db : m.getBlocks()) {
            if (db instanceof DataBlockTarget /* db.isNested()*/) {
                continue;
            }
            int type = type(db);
            ItemCoverage item = null;
            if (type == CT_BLOCK) {
                item = ItemCoverage.createBlockCoverageItem(db.startBCI(), db.endBCI(), db.getCount(), db.getScale());
            } else {
                item = ItemCoverage.createBranchCoverageItem(db.startBCI(), db.endBCI(), db.getCount(), db.getScale());
            }

            String ancReason = isBlockInAnc(m, db, ancFilters, ancBlockReasons);
            if (isInAnc || ancReason != null){
                item.setAncInfo(ancInfo != null ? ancInfo : ancReason);
            }

            boolean isNew = true;
            for (DataBlock d : added.keySet()) {
                if (d.startBCI() == db.startBCI() && type(d) == type(db) && added.get(d).isBlock()) {
                    added.get(d).count += db.getCount();

                    if (added.get(d).scale != null && db.getScale() != null) {
                        for (int i = 0; i < added.get(d).scale.size(); i++) {
                            if (db.getScale().isBitSet(i)) {
                                added.get(d).scale.setBit(i, true);
                            }
                        }
                    }
                    isNew = false;
                    break;
                }
            }
            if (isNew) {
                added.put(db, item);
                if (type != CT_METHOD) {
                    list.add(item);
                }
            }
        }

        for (DataBlockTarget db : m.getBranchTargets()) {
            int type = type(db);
            ItemCoverage item = null;
            if (type == CT_BLOCK) {
                item = ItemCoverage.createBlockCoverageItem(db.startBCI(), db.endBCI(), db.getCount(), db.getScale());
            } else {
                item = ItemCoverage.createBranchCoverageItem(db.startBCI(), db.endBCI(), db.getCount(), db.getScale());
            }

            String ancReason = isBlockInAnc(m, db, ancFilters, ancBlockReasons);
            if (isInAnc || ancReason != null){
                item.setAncInfo(ancInfo != null ? ancInfo : ancReason);
            }

            boolean isNew = true;
            for (DataBlock d : added.keySet()) {
                if (d.startBCI() == db.startBCI() && type(d) == type(db) && added.get(d).isBlock()) {
                    added.get(d).count += db.getCount();

                    if (added.get(d).scale != null && db.getScale() != null) {
                        for (int i = 0; i < added.get(d).scale.size(); i++) {
                            if (db.getScale().isBitSet(i)) {
                                added.get(d).scale.setBit(i, true);
                            }
                        }
                    }
                    isNew = false;
                    break;
                }
            }
            if (isNew) {
                added.put(db, item);
                list.add(item);
            }
        }

        for (DataBlock db : added.keySet()) {
            ItemCoverage i = added.get(db);
            if (!i.isBlock()) {
                ItemCoverage i2 = ItemCoverage.createBlockCoverageItem(i.startLine, i.endLine, i.count, db.getScale());

                String ancReason = isBlockInAnc(m, db, ancFilters, ancBlockReasons);
                if (isInAnc || ancReason != null){
                    i2.setAncInfo(ancInfo != null ? ancInfo : ancReason);
                }

                boolean isNew = true;
                for (DataBlock d : added.keySet()) {
                    if (d.startBCI() == db.startBCI() && added.get(d).isBlock()) {
                        added.get(d).count += db.getCount();

                        if (added.get(d).scale != null && db.getScale() != null){
                            Scale s =  Scale.createZeroScale(added.get(d).scale.size());
                            for (int j = 0; j < added.get(d).scale.size(); j++) {
                                if (added.get(d).scale.isBitSet(j) || db.getScale().isBitSet(j)) {
                                    s.setBit(j, true);
                                }
                            }
                            added.get(d).scale = s;
                        }

                        isNew = false;
                        break;
                    }
                }
                if (isNew) {
                    added.put(db, i2);
                    list.add(i2);
                }
            }
        }

        if (!isInAnc && ancBlockReasons.size() - 1 == list.size()) {
            StringBuilder methodAncReason = new StringBuilder();
            for (String ancBlock : ancBlockReasons) {
                methodAncReason.append(" ").append(ancBlock);
            }
            setAncInfo(methodAncReason.toString());
        }
    }

    public void setAncInfo(String ancInfo){
        isInAnc = (ancInfo != null && !ancInfo.isEmpty());
        this.ancInfo = ancInfo;
    }

    public boolean isMethodInAnc(){
        return isInAnc;
    }

    public String getAncInfo(){
        return ancInfo;
    }

    /**
     * Detects type of the DataBlock
     *
     * @param db
     * @return code
     * @see #CT_METHOD
     * @see #CT_BRANCH_TRUE
     * @see #CT_BRANCH_FALSE
     * @see #CT_CASE
     * @see #CT_SWITCH_WO_DEF
     * @see #CT_BLOCK
     */
    public static int type(DataBlock db) {
        String type = db.kind();
        if (type.equals(XmlNames.METHENTER)) {
            return CT_METHOD;
        }
        if (type.equals(XmlNames.COND)) {
            DataBlockTargetCond cond = (DataBlockTargetCond) db;
            return cond.side() ? CT_BRANCH_TRUE : CT_BRANCH_FALSE;
        }

        if (type.equals(XmlNames.CASE)) {
            return CT_CASE;
        }

        if (type.equals(XmlNames.DEFAULT)) {
            return CT_SWITCH_WO_DEF;
        }
        return CT_BLOCK;
    }

    /**
     * Returns list of blocks+branches
     *
     * @return all items that are included into this method
     */
    public List<ItemCoverage> getItems() {
        return items;
    }

    public Iterator<ItemCoverage> iterator() {
        return items.iterator();
    }

    /**
     * @return line coverage
     */
    public LineCoverage getLineCoverage() {
        return lineCoverage;
    }

    /**
     * Coverage kind (used in HTML reports as header for label)
     *
     * @return DataType.MEHTOD
     */
    public DataType getDataType() {
        return DataType.METHOD;
    }

    /**
     * <p> Allows to get sums over the coverage statistics of this method. E.g.
     * getData(DataType.BRANCH) will return coverage data containing the total
     * number of branches in this method and number of covered branches in this
     * method. getData(DataType.BLOCK) will return block coverage of this
     * method. </p> <p> Allows to sum though METHOD, BLOCK, BRANCH and LINE
     * types </p>
     *
     * @param column Type to sum
     * @return CoverageData representing 2 fields - total number of members and
     * number of covered members
     * @see DataType
     * @see CoverageData
     * @see MemberCoverage#getHitCount()
     */
    public CoverageData getData(DataType column) {
        return getData(column, -1);
    }

    @Override
    public CoverageData getData(DataType column, int testNumber) {

        switch (column) {
            case METHOD:

                if (inAnonymClass && !anonymon) {
                    return new CoverageData(0, 0, 0);
                }

                if (name.startsWith("lambda$")){
                    return new CoverageData(0, 0, 0);
                }

                if (testNumber > -1) {
                    int c = (count > 0 && isCoveredByTest(testNumber)) ? 1 : 0;
                    if (isInAnc) {
                        return new CoverageData(c, 1 - c, 1);
                    }
                    return new CoverageData(c, 0, 1);
                }
                int c = count > 0 ? 1 : 0;
                if (isInAnc) {
                    return new CoverageData(c, 1 - c, 1);
                }
                return new CoverageData(c, 0, 1);
            case BLOCK:
            case BRANCH:
                if (inAnonymClass && !anonymon && (access & Opcodes.ACC_SYNTHETIC) != 0) {
                    return new CoverageData(0, 0, 0);
                }
                CoverageData result = new CoverageData(0, 0, 0);
                for (ItemCoverage item : items) {
                    if (testNumber < 0 || item.isCoveredByTest(testNumber)) {
                        result.add(item.getData(column));
                    } else {
                        CoverageData icov = item.getData(column);
                        if (isInAnc){
                            result.add(new CoverageData(0, 1, icov.getTotal()));
                        }
                        else {
                            result.add(new CoverageData(0, icov.getAnc(), icov.getTotal()));
                        }
                    }
                }
                return result;
            case LINE:
                return lineCoverage;
            default:
                return new CoverageData();
        }
    }

    protected DataType[] getDataTypes() {
        return supportedColumns;
    }

    /**
     * Returns method name and signature in JLS format
     *
     * @return Method signature in readable form (JLS)
     */
    public String getReadableSignature() {
        return Utils.convertVMtoJLS(name, signature);
    }

    /**
     * @param lineNumber line number
     * @return true if passed line is covered, false otherwise.
     */
    public boolean isLineCovered(int lineNumber) {
        return lineCoverage.isLineCovered(lineNumber);
    }
    // Types of Jcov items
    public static final int CT_FIRST_KIND = 1;
    public static final int CT_METHOD = 1;
    public static final int CT_FIKT_METHOD = 2;
    public static final int CT_BLOCK = 3;
    public static final int CT_FIKT_RET = 4;
    public static final int CT_CASE = 5;
    public static final int CT_SWITCH_WO_DEF = 6;
    public static final int CT_BRANCH_TRUE = 7;
    public static final int CT_BRANCH_FALSE = 8;
    public static final int CT_LINE = 9;
    public static final int CT_LAST_KIND = 9;
}
