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
package com.sun.tdk.jcov.processing;

import com.sun.tdk.jcov.instrument.DataClass;
import com.sun.tdk.jcov.instrument.DataField;
import com.sun.tdk.jcov.instrument.DataMethod;
import com.sun.tdk.jcov.instrument.DataMethodEntryOnly;
import com.sun.tdk.jcov.instrument.DataPackage;
import com.sun.tdk.jcov.instrument.DataRoot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import org.objectweb.asm.Opcodes;

/**
 * Processor, that combines classes derived from the same sources.
 *
 * @author Dmitry Fazunenko
 */
public class CombinerDataProcessor implements DataProcessor {

    public DataRoot process(DataRoot root) throws ProcessingException {
        HashMap<String, ArrayList<DataClass>> classesMap =
                new HashMap<String, ArrayList<DataClass>>();
        DataRoot result = new DataRoot(root.getParams());
        result.setScaleOpts(root.getScaleOpts());

        // building map: scr --> cls1, cls2, ...
        List<DataPackage> allPkg = root.getPackages();
        for (DataPackage pkg : allPkg) {
            for (Iterator<DataClass> it = pkg.getClasses().iterator(); it.hasNext();) {
                DataClass cls = it.next();
                String srcName = cls.getSource();
                if (srcName != null) {
                    String key = pkg.getName() + '/' + srcName;
                    ArrayList<DataClass> srcClasses = classesMap.get(key);
                    if (srcClasses == null) {
                        srcClasses = new ArrayList<DataClass>();
                        classesMap.put(key, srcClasses);
                    }
                    srcClasses.add(cls);
                } else {
                    result.addClass(cls); // src == null
                }
            }
        }

        // combining classes derived from the same source
        for (String key : classesMap.keySet()) {
            ArrayList<DataClass> toMerge = classesMap.get(key);
            if (toMerge.size() == 1) {
                // nothing to combine: one class in the source
                result.addClass(toMerge.get(0)); // just one class
            } else {
                int k = key.indexOf(".");
                String mainClassName = k > 0 ? key.substring(0, k) : key;
                DataClass cls = findMainClazz(toMerge, mainClassName);
                // it's expected, the main class is always the first in the list
                DataClass newClass = cls.clone(result.rootId());

                k = mainClassName.lastIndexOf("/");
                if (k > 0) {
                    mainClassName = mainClassName.substring(k + 1);
                }


                for (DataClass c : toMerge) {
                    if (c == cls) {
                        continue; // skip mainClass
                    }

                    boolean isPublic = isPublic(c, toMerge);
                    String clzName = c.getName();
                    int j = clzName.lastIndexOf("/");
                    if (j >= 0) {
                        clzName = clzName.substring(j + 1);
                    }
                    String prefix = createPrefix(clzName, mainClassName);
                    for (DataMethod m : c.getMethods()) {
                        int newAccess = isPublic ? m.getAccess() : makePrivate(m.getAccess());

                        DataMethod nm = m.clone(newClass, newAccess, prefix + m.getName());
                        // for -type=method methods exist witout blocks and branches
                        if (nm instanceof DataMethodEntryOnly) {
                            nm.setCount(m.getCount());
                        }
                        // new created method will be added to the newClass
                    }
                    for (DataField f : c.getFields()) {
                        int newAccess = isPublic ? f.getAccess() : makePrivate(f.getAccess());
                        f.clone(newClass, newAccess, prefix + f.getName());
                        // new created field will be added to the newClass
                    }
                }
                result.addClass(newClass);
            }
        }
        return result;

    }

    /**
     * Finds the main class for the sources which name equals to the source
     *
     * @param toMerge
     * @param mainClassName
     * @return
     */
    protected DataClass findMainClazz(ArrayList<DataClass> toMerge, String mainClassName) {
        for (DataClass c : toMerge) {
            if (mainClassName.equals(c.getFullname())) {
                return c;
            }
        }
        return toMerge.get(0);
    }

    /**
     * Forms the prefix for a given class to be used in the report. This
     * implementation uses empty prefix for the main class, and a class name
     * precided with the "!" for nested classes, and precided with "%" for
     * classes defined outside the main one.
     *
     * This method can be overrided to alter the rules above.
     *
     * @param className - class name to generate prefix for
     * @param mainClassName - the main class of the source
     * @return prefix
     */
    protected String createPrefix(String className, String mainClassName) {
        if (className.equals(mainClassName)) {
            return "";
        } else if (className.startsWith(mainClassName + "$")) {
            return className.substring(className.indexOf("$")) + ".";
        } else {
            return "~" + className + ".";
        }
    }

    /**
     * Returns true if the given class is public (contains "public" modifier)
     * and all its outers are public as well.
     *
     * @param cls - class to analyze
     * @param peers classes the could be potentionaly outer of the cls
     * @return true or false
     */
    private boolean isPublic(DataClass cls, ArrayList<DataClass> peers) {
        ArrayList<DataClass> outers = findOuters(cls, peers);
        DataClass outClass = outers.get(outers.size() - 1);
        for (DataClass c : outers) {
            if (!isPublic(c, outClass)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Scans given modifiers in attempt to find "public".
     *
     * @param modifiers - array to scan
     * @return true if given array is not null and contains "public"
     */
    private boolean isPublic(DataClass c, DataClass outClass) {
        if (isAnonymous(c.getName())) {
            return isPublicAnonymous(c, outClass);
        }

        return (c.getAccess() & Opcodes.ACC_PUBLIC) != 0;
    }

    private boolean isPublicAnonymous(DataClass c, DataClass outClass) {

        TreeSet<DataMethod> sortedMethods = new TreeSet<DataMethod>(new Comparator<DataMethod>() {
            @Override
            public int compare(DataMethod dm1, DataMethod dm2) {
                return dm1.getLineTable().get(dm1.getLineTable().size() - 1).line - dm2.getLineTable().get(dm2.getLineTable().size() - 1).line;
            }
        });

        for (DataMethod dm : outClass.getMethods()) {
            if (dm.getLineTable() != null) {
                sortedMethods.add(dm);
            }
        }

        DataMethod initMethod = c.findMethod("<init>");

        if (sortedMethods != null && initMethod != null) {
            for (DataMethod dataMethod : sortedMethods) {

                if (initMethod.getLineTable().get(0).line <= dataMethod.getLineTable().get(dataMethod.getLineTable().size() - 1).line) {

                    //Anonymous classes in init and clinit is not public
                    if (dataMethod.getName().equals("<init>") || dataMethod.getName().equals("<clinit>")) {
                        return false;
                    }

                    return dataMethod.isPublicAPI();
                }

            }
        }

        return false;
    }

    /**
     * Finds outer classes among classes obtained from the same source.
     *
     * @param cls - class to find outers
     * @param peers - class obtained from the same source
     * @return list of outers including cls itself.
     */
    private ArrayList<DataClass> findOuters(DataClass cls, ArrayList<DataClass> peers) {
        ArrayList<DataClass> result = new ArrayList<DataClass>();
        result.add(cls);
        String name = cls.getName();
        for (DataClass c : peers) {
            if (name.startsWith(c.getName() + "$")) {
                result.add(c);
            }
        }
        return result;
    }

    /**
     * Removes "public" and "protected" modifiers from the given list
     *
     * @param modifiers array of modifiers
     * @return modified array
     */
    private int makePrivate(int modifiers) {
        return modifiers & ~Opcodes.ACC_PUBLIC & ~Opcodes.ACC_PROTECTED;
    }

    /**
     * @param name - name of a class
     * @return true, if the given class name is anonyomous
     */
    private boolean isAnonymous(String name) {
        if (name == null) {
            return false;
        }
        int index = name.lastIndexOf("$");
        if (index < 0) {
            return false;
        }
        return Character.isDigit(name.charAt(index + 1));
    }
}