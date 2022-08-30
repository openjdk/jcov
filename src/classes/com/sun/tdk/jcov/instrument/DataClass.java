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
package com.sun.tdk.jcov.instrument;

import com.sun.tdk.jcov.util.NaturalComparator;
import com.sun.tdk.jcov.filter.MemberFilter;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import org.objectweb.asm.Opcodes;
import java.util.List;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DataClass contains information about one class. Can include methods and
 * fields.
 *
 *
 * @author Dmitry Fazunenko
 * @author Alexey Fedorchenko
 *
 * @see DataClass#methods
 * @see DataClass#fields
 * @see DataMethod
 * @see DataField
 * @see DataPackage
 */
public class DataClass extends DataAnnotated implements Comparable<DataClass> {

    /**
     * Full (VM) name of associated class
     */
    private final String fullname;
    /**
     * Module name of associated class
     */
    private String moduleName;
    /**
     * Short name of associated class
     */
    private final String name;
    /**
     * Optional checksum
     */
    private long checksum;
    /**
     * All methods registered in this class
     */
    private final List<DataMethod> methods;
    /**
     * All fields registered in this class
     */
    private final List<DataField> fields;
    /**
     * Container for access code of this class
     *
     * @see org.objectweb.asm.Opcodes
     */
    private DataModifiers access;
    /**
     * Class signature
     */
    private String signature;
    /**
     * Superclass full (VM) name (or java/lang/Object)
     */
    private String superName;
    /**
     * Associated source file
     */
    private String source;
    /**
     * Implementing interfaces divided with ';'. Can be null
     */
    private String superInterfaces;
    /**
     * Check whether this DataClass is configured to differ elements - classes
     * vs interfaces False always
     */
    private final boolean differentiateClass;
    private static final Logger logger;

    static {
        logger = Logger.getLogger(DataClass.class.getName());
    }
    private boolean inner = false;
    private boolean anonym = false;

    /**
     * Creates a new instance of DataClass. Use setInfo method to set up
     * additional data<br/><br/>
     *
     * Use setInfo to set up additional parameters
     *
     * @see #setInfo
     * @param rootId
     * @param fullname can't be null
     * @param checksum
     * @param differentiateClass
     */
    public DataClass(int rootId, String fullname, String moduleName, long checksum, boolean differentiateClass) {
        super(rootId);
        this.fullname = fullname;
        this.checksum = checksum;
        int slash = fullname.lastIndexOf('/');
        if (slash < 0) {
            this.name = fullname;
        } else {
            this.name = fullname.substring(slash + 1);
        }
        this.methods = new LinkedList<DataMethod>();
        this.fields = new LinkedList<DataField>();
        this.differentiateClass = differentiateClass; // is always false at the moment
        this.moduleName = moduleName;
      }

    public String getModuleName(){
        return moduleName;
    }

    public void setModuleName(String moduleName){
        this.moduleName = moduleName;
    }

    /**
     * Set up additional data
     *
     * @param flags
     * @param signature
     * @param superName
     * @param interfaces
     */
    public void setInfo(String flags, String signature, String superName, String interfaces) {
        String[] accessFlags = flags.split(" ");
        int acc = access(accessFlags);
        String[] sInterfaces = null;
        if (interfaces != null) {
            sInterfaces = interfaces.split(";");
        }

        setInfo(acc, signature, superName, sInterfaces);

    }

    /**
     * Set up additional data
     *
     * @param access
     * @param signature
     * @param superName
     * @param interfaces
     */
    public void setInfo(int access, String signature, String superName, String[] interfaces) {
        this.access = new DataModifiers(access);
        this.signature = signature;
        this.superName = superName;
        if (interfaces != null && interfaces.length > 0) {
            this.superInterfaces = interfaces[0];
            for (int i = 1; i < interfaces.length; i++) {
                this.superInterfaces += ";" + interfaces[i];
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DataClass)) {
            return false;
        }
        DataClass clazz = (DataClass) o;
        boolean eq = fullname.equals(clazz.fullname);
        if (checksum != -1 && clazz.checksum != -1) {
            eq = eq && (checksum == clazz.checksum);
        }
        return eq;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + (fullname != null ? fullname.hashCode() : 0);
//        hash = 17 * hash + (int)checksum;
        return hash;
    }

    /**
     * Set source file associated with this DataClass
     *
     * @param source
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Get source file associated with this DataClass
     *
     * @return source file associated with this DataClass
     */
    public String getSource() {
        return source;
    }

    /**
     * Set superclass fullname (VM)
     *
     * @param superName
     */
    public void setSuperName(String superName) {
        this.superName = superName;
    }

    /**
     * Get superclass fullname (VM)
     *
     * @return superclass fullname (VM)
     */
    public String getSuperName() {
        return superName;
    }

    /**
     * Set the signature of this class
     *
     * @param signature
     */
    public void setSignature(String signature) {
        this.signature = signature;
    }

    /**
     * Get the signature of this class
     *
     * @return the signature of this class
     */
    public String getSignature() {
        return signature;
    }

    /**
     * Set access code of this class
     *
     * @see org.objectweb.asm.Opcodes
     * @param access
     */
    public void setAccess(int access) {
        this.access = new DataModifiers(access);
    }

    /**
     * Get access code of this class
     *
     * @see org.objectweb.asm.Opcodes
     * @return access code of this class
     */
    public int getAccess() {
        return access.access();
    }

    public Modifiers getModifiers() { return access; }
    /**
     * Set checksum of this class. It's used in equals() method
     *
     * @param checksum
     */
    public void setChecksum(long checksum) {
        this.checksum = checksum;
    }

    /**
     * Get checksum of this class. It's used in equals() method
     *
     * @return checksum of this class. It's used in equals() method
     */
    public long getChecksum() {
        return checksum;
    }

    /**
     * Set interfaces this class is implementing (divided with ';' or null if no
     * one)
     *
     * @param superInterfaces
     */
    public void setSuperInterfaces(String superInterfaces) {
        this.superInterfaces = superInterfaces;
    }

    /**
     * Get interfaces this class is implementing (divided with ';' or null if no
     * one)
     *
     * @return interfaces this class is implementing (divided with ';' or null
     * if no one)
     */
    public String getSuperInterfaces() {
        return superInterfaces;
    }

    /**
     * Get simple class name
     *
     * @return simple class name
     */
    public String getName() {
        return name;
    }

    /**
     * Get full (VM) class name
     *
     * @return full (VM) class name
     */
    public String getFullname() {
        return fullname;
    }

    /**
     * Detects package from the full (VM) classname
     *
     * @return package from the full (VM) classname
     */
    public String getPackageName() {
        int slash = fullname.lastIndexOf('/');
        if (slash < 0) {
            return "";
        } else {
            return fullname.substring(0, slash);
        }
    }

    /**
     * Get this class`s access flags as String array
     *
     * @return this class`s access flags as String array
     */
    public String[] getAccessFlags() {
        return accessFlags(access.access());
    }

    /**
     * Check whether <b>access</b> field has ACC_PUBLIC or ACC_PROTECTED flag
     *
     * @see #getAccess()
     * @see org.objectweb.asm.Opcodes
     * @return true if <b>access</b> field has ACC_PUBLIC or ACC_PROTECTED flag
     */
    @Deprecated
    public boolean isPublic() { return isPublicAPI(); }

    /**
     * Check whether <b>access</b> field has ACC_PUBLIC or ACC_PROTECTED flag
     *
     * @see #getAccess()
     * @see org.objectweb.asm.Opcodes
     * @return true if <b>access</b> field has ACC_PUBLIC or ACC_PROTECTED flag
     */
    public boolean isPublicAPI() {
        return access.isPublic() || access.isProtected();
    }

    /**
     * Checks whether this class has 'private' modifier
     *
     * @return true if class is private
     */
    @Deprecated
    public boolean hasPrivateModifier() {
        return access.isPrivate();
    }

    /**
     * Checks whether this class has 'public ' modifier
     *
     * @return true if class is public
     */
    @Deprecated
    public boolean hasPublicModifier() {
        return access.isPublic();
    }

    /**
     * Checks whether this class has 'protected' modifier
     *
     * @return true if class is protected
     */
    @Deprecated
    public boolean hasProtectedModifier() {
        return access.isProtected();
    }

    /**
     * Checks whether this class has 'abstract' modifier
     *
     * @return true if class is abstract
     */
    @Deprecated
    public boolean hasAbstractModifier() {
        return access.isAbstract();
    }

    /**
     * Checks whether this class has 'static' modifier
     *
     * @return true if class is static
     */
    @Deprecated
    public boolean hasStaticModifier() {
        return access.isStatic();
    }

    /**
     * Checks whether this class has specified modifier (by Opcodes)
     *
     * @return true if class has specified modifier
     * @see Opcodes
     * @see DataClass#getAccess()
     */
    @Deprecated
    public boolean hasModifier(int modifierCode) { return access.is(modifierCode); }

    /**
     * Add method data to this class
     *
     * @see DataMethod
     * @param method
     */
    public void addMethod(DataMethod method) {
        methods.add(method);
    }

    /**
     * Finds method info in this class
     *
     * @param methname
     * @return method or null if not found
     */
    public DataMethod findMethod(String methname) {
        if (methname == null) {
            return null;
        }
        for (DataMethod dm : methods) {
            if (dm.getName().equals(methname)) {
                return dm;
            }
        }
        return null;
    }

    /**
     * Removes method info from this class
     *
     * @param methname
     * @return removed method or null if not found
     */
    public DataMethod removeMethod(String methname) {
        if (methname == null) {
            return null;
        }
        Iterator<DataMethod> it = methods.iterator();
        while (it.hasNext()) {
            DataMethod dm = it.next();
            if (dm.getName().equals(methname)) {
                it.remove();
                return dm;
            }
        }
        return null;
    }

    /**
     * Add field data to this class
     *
     * @see DataField
     * @param field
     */
    public void addField(DataField field) {
        fields.add(field);
    }

    /**
     * Find field info in this class
     *
     * @param fieldname
     * @return field or null if not found
     */
    public DataField findField(String fieldname) {
        if (fieldname == null) {
            return null;
        }
        for (DataField df : fields) {
            if (df.getName().equals(fieldname)) {
                return df;
            }
        }
        return null;
    }

    /**
     * Removes field info from this class
     *
     * @param fieldname
     * @return removed field or null if not found
     */
    public DataField removeField(String fieldname) {
        if (fieldname == null) {
            return null;
        }
        Iterator<DataField> it = fields.iterator();
        while (it.hasNext()) {
            DataField dm = it.next();
            if (dm.getName().equals(fieldname)) {
                it.remove();
                return dm;
            }
        }
        return null;
    }

    /**
     * Get list of all methods associated with this class
     *
     * @see DataMethod
     * @return list of all methods associated with this class
     */
    public List<DataMethod> getMethods() {
        return methods;
    }

    /**
     * Get list of all fields associated with this class
     *
     * @see DataField
     * @return list of all fields associated with this class
     */
    public List<DataField> getFields() {
        return fields;
    }

    /**
     * Check whether this class was hit. Checks one-by-one all methods and all
     * fields in this class. Class is hit only is any method or field is hit.
     *
     * @return true if this class was hit
     */
    public boolean wasHit() {
        for (DataMethod method : methods) {
            if (method.wasHit()) {
                return true;
            }
        }
        for (DataField field : fields) {
            if (field.wasHit()) {
                return true;
            }
        }
        return false;
    }

    /**
     * XML Generation. Not supposed to use outside.
     */
    @Override
    public String kind() {
        if (differentiateClass) {
            return access.isInterface() ? XmlNames.INTERFACE : XmlNames.CLASS;
        } else {
            return XmlNames.CLASS;
        }
    }

    /**
     * XML Generation. Not supposed to use outside.
     */
    @Override
    void xmlGen(XmlContext ctx) {

        if ((!ctx.skipNotCoveredClasses || wasHit() && methods.size() > 0)) {

            // check abstract on
            if (ctx.showAbstract) {
                super.xmlGen(ctx);
            } else if (!access.isInterface()) {
                super.xmlGen(ctx);
            } else {
                // cheking interface
                // find default methods even when abstract off
                List<DataMethod> onlyDefaultMethods = new ArrayList<DataMethod>();
                for (DataMethod method : methods) {
                    if ((method.getAccess() & Opcodes.ACC_ABSTRACT) == 0) {
                        onlyDefaultMethods.add(method);
                    }
                }
                this.methods.retainAll(onlyDefaultMethods);

                if (methods.size() > 0) {
                    super.xmlGen(ctx);
                }
            }
        }

    }

    /**
     * XML Generation. Not supposed to use outside.
     */
    @Override
    void xmlAttrs(XmlContext ctx) {
        ctx.attr(XmlNames.NAME, name);
        ctx.attr(XmlNames.SUPERNAME, superName == null ? "" : superName);
        if (checksum != -1) {
            ctx.attr(XmlNames.CHECKSUM, checksum);
        }
        if (!differentiateClass && !access.isInterface()) {
            ctx.attr(XmlNames.INTERFACE, true);
        }
        if (signature != null) {
            ctx.attrNormalized(XmlNames.SIGNATURE, signature);
        }
        if (source != null) {
            ctx.attrNormalized(XmlNames.SOURCE, source);
        }
        if (inner) {
            if (anonym) {
                ctx.attr(XmlNames.INNER_CLASS, "anon");
            } else {
                ctx.attr(XmlNames.INNER_CLASS, "inner");
            }
        }
        xmlAccessFlags(ctx, access.access());

        super.xmlAttrs(ctx);
    }

    /**
     * XML Generation. Not supposed to use outside.
     */
    @Override
    void xmlBody(XmlContext ctx) {
        // Collections.sort(methods);
        for (DataMethod method : methods) {
            method.xmlGen(ctx);
        }
        // Collections.sort(fields);
        for (DataField field : fields) {
            field.xmlGen(ctx);
        }
    }

    /**
     * Decode class`s access flags as String array
     *
     * @param access
     * @return class`s access flags as String array
     */
    @Override
    String[] accessFlags(int access) {
        String[] as = super.accessFlags(access);
        List<String> lst = new LinkedList();
        for (String s : as) {
            if (!XmlNames.A_SYNCHRONIZED.equals(s)) {
                lst.add(s);
            }
        }

        return lst.toArray(new String[lst.size()]);
    }

    /**
     * Cloneable interface
     *
     * @param rootId
     * @return clone
     */
    public DataClass clone(int rootId) {
        DataClass res = new DataClass(rootId, fullname, moduleName, -1, differentiateClass);
        res.access = access;
        res.signature = signature;
        res.superName = superName;
        res.superInterfaces = superInterfaces;
        res.source = source;
        res.methods.addAll(methods);
        res.fields.addAll(fields);
        return res;
    }

    @Override
    public int compareTo(DataClass clz) {
        return NaturalComparator.INSTANCE.compare(this.name, clz.getName());
    }

    /**
     * Checks whether this class is compatible with <b>other</b>
     *
     * @param other
     * @param traceString
     * @param severity a level of error ignorance
     * @param boe true means that checkCompatibility will stop on the first
     * occurred critical (see <b>severity</b>) error
     * @return Number of errors (if <b>boe</b> is set to true it can be only 1)
     * @throws MergeException
     */
    public int checkCompatibility(DataClass other, String traceString, int severity, boolean boe) throws MergeException {
        int errors = 0;
        try {
            checkEquals(other, traceString + ": " + other.fullname);
        } catch (MergeException me) {
            if (isCritical(me, severity)) {
                throw me;
            } else {
                logger.log(Level.INFO, me.getMessage());
            }
        }
        for (DataMethod meth : methods) {
            for (DataMethod ometh : other.methods) {
                if (meth.equals(ometh)) {
                    try {
                        meth.checkCompatibility(ometh, traceString + ": " + other.fullname + "." + ometh.getName() + ometh.getVmSignature());
                    } catch (MergeException e) {
                        if (isCritical(e, severity)) {
                            errors++;
                            logger.log(Level.SEVERE, "Error while merging method " + ometh.getName() + ometh.getVmSignature(), e);
                            if (boe) {
                                return errors;
                            }
                        } else {
                            logger.log(Level.WARNING, "Error while merging method " + ometh.getName() + ometh.getVmSignature() + " - skipped as not critical", e);
                        }
                    }
                    break;
                }
            }
        }

        for (DataField fld : fields) {
            for (DataField ofld : other.fields) {
                if (fld.equals(ofld)) {
                    try {
                        fld.checkCompatibility(ofld, traceString + ": " + other.fullname + "." + ofld.getName());
                    } catch (MergeException e) {
                        if (isCritical(e, severity)) {
                            errors++;
                            logger.log(Level.SEVERE, "Error while merging field " + ofld.getName(), e);
                            if (boe) {
                                return errors;
                            }
                        } else {
                            logger.log(Level.WARNING, "Error while merging field " + ofld.getName() + " - skipped as not critical", e);
                        }
                    }
                    break;
                }
            }
        }

        return errors;
    }

    public void mergeSorted(DataClass other) {

        ListIterator<? extends Comparable> thisIt = methods.listIterator();
        ListIterator<? extends Comparable> otherIt = other.methods.listIterator();

        Comparable thisC = null, otherC = null;

        if (!thisIt.hasNext()) {
            // should not happen - at least init() should exist
        } else {

            while (otherIt.hasNext()) {
                otherC = otherIt.next();
                thisC = thisIt.next();

                int comp = thisC.compareTo(otherC);
                while (thisIt.hasNext() && comp < 0) {
                    thisC = thisIt.next();
                    comp = thisC.compareTo(otherC);
                }
                if (comp == 0) {
                    // found - merging class
                    ((DataMethod) thisC).merge((DataMethod) otherC);
                } else if (comp > 0) {
                    // no such class in thisClasses
                } else {
                    // comp < 0 - thisClasses has no more elements - all that left are missing in thisClasses
                    break; // need to break as next iteration will call next() which will fail
                }
            }
        }

        thisIt = fields.listIterator();
        otherIt = other.fields.listIterator();

        thisC = null;
        otherC = null;

        if (!thisIt.hasNext()) {
        } else {

            while (otherIt.hasNext()) {
                otherC = otherIt.next();
                thisC = thisIt.next();

                int comp = thisC.compareTo(otherC);
                while (thisIt.hasNext() && comp < 0) {
                    thisC = thisIt.next();
                    comp = thisC.compareTo(otherC);
                }
                if (comp == 0) {
                    // found - merging class
                    ((DataField) thisC).merge((DataField) otherC);
                } else if (comp > 0) {
                    // no such class in thisClasses
                } else {
                    // comp < 0 - thisClasses has no more elements - all that left are missing in thisClasses
                    break; // need to break as next iteration will call next() which will fail
                }
            }
        }

        if (checksum == -1) {
            checksum = other.checksum;
        }
    }

    /**
     * Merges information from <b>other</b> to this DataClass. <br/><br/>
     *
     * This only sums hit count and scales - any difference in class structure
     * is an error
     *
     * @param other
     */
    public void merge(DataClass other) {
        for (DataMethod meth : methods) {
            for (DataMethod ometh : other.methods) {
                if (meth.equals(ometh)) {
                    meth.merge(ometh);
                    break;
                } // XXX meth not found
            }
        }

        for (DataField fld : fields) {
            for (DataField ofld : other.fields) {
                if (fld.equals(ofld)) {
                    fld.merge(ofld);
                    break;
                } // XXX field not found
            }
        }

        if (checksum == -1) {
            checksum = other.checksum;
        }
    }

    /**
     * Merge class data with another one without looking into blocks structure
     *
     * @param otherClass
     */
    public void mergeOnSignatures(DataClass otherClass) {
        ListIterator<DataMethod> other_it = otherClass.methods.listIterator();
        outer:
        while (other_it.hasNext()) {
            DataMethod otherMethod = other_it.next();
            ListIterator<DataMethod> it = methods.listIterator();
            while (it.hasNext()) {
                DataMethod thisMethod = it.next();
                if (otherMethod.getFullName().equals(thisMethod.getFullName())) { // signature checking only
                    if (otherMethod instanceof DataMethodEntryOnly) {
                        thisMethod.merge(otherMethod);
                    } else {
                        // it's terrible but better than creating new DataMethodEntryOnly from otherMethod
                        thisMethod.iterator().next().mergeScale(otherMethod.iterator().next());
                        thisMethod.setCount(thisMethod.getCount() + otherMethod.getCount());
                    }
                    continue outer;
                }
            }

            // means that this method is missing - it's an error, but ignoring - just writting warning
            System.out.println("Warning, class " + otherClass.getFullname() + " has method " + otherMethod.name + " that was not found in template");
        }

        // fields
        outer:
        for (DataField otherFields : otherClass.fields) {
            ListIterator<DataField> it = fields.listIterator();
            while (it.hasNext()) {
                DataField thisField = it.next();
                if (otherFields.equals(thisField)) { // parent && signature checking only
                    thisField.merge(otherFields);
                    continue outer;
                }
            }

            // means that this field is missing - it's an error, but ignoring - just writting warning
            System.out.println("Warning, class " + otherClass.getFullname() + " has field " + otherFields.getName() + " that was not found in template");
        }
    }

    /**
     * Return true, if merge error is critical, false for warnings
     *
     * @param errorSeverity - error severity
     * @param level - loose level set by user
     * @return true, if merge error is critical, false for warnings
     */
    private static boolean isCritical(MergeException me, int level) {
        int errorSeverity = me.getSeverity();
        if (level == 0 || errorSeverity == 0) {
            return true;
        }
        if (level == 1 && errorSeverity == 3) {
            return false;
        }
        if (level == 2 && errorSeverity >= 2) {
            return false;
        }
        if (level == 3) {
            return false;
        }
        return true;
    }

    /**
     * Check that this method and another one are identical. <br/><br/>
     *
     * Name mismatch is a critical one (can't be skipped - different classes
     * compared). <br/> Checksum or methods size mismatch is high priority
     * error.<br/> Signature, access and field size mismatch is low priority
     * error.
     *
     * @param other
     * @param trace
     * @throws MergeException with severity level set
     */
    private void checkEquals(DataClass other, String trace) throws MergeException {
        if (!fullname.equals(other.fullname)) {
            throw new MergeException(
                    "Fullname mismatch: expected '" + fullname + "'; found '" + other.fullname + "'",
                    trace, MergeException.CRITICAL);
        }
        if (checksum != -1 && other.checksum != -1 && checksum != other.checksum) {
            throw new MergeException(
                    "Checksum mismatch: expected '" + checksum + "'; found '" + other.checksum + "'",
                    trace, MergeException.HIGH);
        }
        if (methods.size() != other.methods.size()) {
            throw new MergeException(
                    "Method size mismatch: expected '" + methods.size() + "'; found '" + other.methods.size() + "'",
                    trace, MergeException.HIGH);
        }
        if (signature != null && !signature.equals(other.signature)) {
            throw new MergeException(
                    "Signature mismatch: expected '" + signature + "'; found '" + other.signature + "'",
                    trace, MergeException.MEDIUM);
        }
        // if any problems with access - make mask to filter out access codes in DataClass(int, DataInput)
        if (access.isSuper() != other.access.isSuper()) {
            throw new MergeException(
                    "Access mismatch: expected '" + access.access() + "'; found '" + other.access.access() + "'",
                    trace, MergeException.LOW);
        }
        if (fields.size() != other.fields.size()) {
            throw new MergeException(
                    "Field size mismatch: expected '" + fields.size() + "'; found '" + other.fields.size() + "'",
                    trace, MergeException.LOW);
        }
    }

    /**
     * Root scale_size should be expanded before invoking this
     *
     * @param add_before
     */
    void expandScales(int newSize, boolean add_before) {
        for (DataMethod m : methods) {
            for (DataBlock bl : m) {
                bl.expandScales(newSize, add_before);
            }
        }

        for (DataField fld : fields) {
            for (DataBlock bl : fld) {
                bl.expandScales(newSize, add_before);
            }
        }
    }

    /**
     * Root scale_size should be expanded before invoking this
     *
     * @param add_before
     */
    void expandScales(int newSize, boolean add_before, int newcount) {
        for (DataMethod m : methods) {
            for (DataBlock bl : m) {
                bl.expandScales(newSize, add_before, newcount);
            }
        }

        for (DataField fld : fields) {
            for (DataBlock bl : fld) {
                bl.expandScales(newSize, add_before, newcount);
            }
        }
    }

    /**
     * Removes members rejected by the filter from this class.
     *
     * @param filter
     */
    public void applyFilter(MemberFilter filter) {
        Iterator it = methods.iterator();
        while (it.hasNext()) {
            DataMethod next = (DataMethod) it.next();
            if (!filter.accept(this, next)) {
                it.remove();
            }
        }

        it = fields.iterator();
        while (it.hasNext()) {
            DataField next = (DataField) it.next();
            if (!filter.accept(this, next)) {
                it.remove();
            }
        }
    }

    /**
     * Sort out fields and methods in this class
     */
    public void sort() {
        Collections.sort(methods);
        Collections.sort(fields);
    }

    void writeObject(DataOutput out) throws IOException {
        super.writeObject(out);
        out.writeUTF(name);
        writeString(out, moduleName);
        writeString(out, fullname);
        writeString(out, signature);
        writeString(out, source);
        writeString(out, superName);
        writeString(out, superInterfaces);
        out.writeLong(checksum);
        out.writeInt(access.access() & ACCESS_MASK); // we don't save ALL the codes in XML, we shouldn't save all codes in net
        out.writeByte((differentiateClass ? 1 : 0) + (inner ? 2 : 0) + (anonym ? 4 : 0));

        out.writeShort(fields.size());
        for (DataField f : fields) {
            f.writeObject(out);
        }

        out.writeShort(methods.size());
        for (DataMethod m : methods) {
            if (m instanceof DataMethodEntryOnly) {
                if (m.access.isNative() || m.access.isAbstract()) {
                    out.write(2); // DMI
                } else {
                    out.write(1); // DMEO
                }
            } else if (m instanceof DataMethodInvoked) {
                if (m.access.isNative() || m.access.isAbstract()) {
                    out.write(2); // DMI
                } else {
                    out.write(1); // DMEO
                }
            } else if (m instanceof DataMethodWithBlocks) {
                out.write(3);
            } else {
                System.out.println("ERROR " + m.getFullName());
                out.write(4);
                throw new IOException("DataClass.writeObject - Unknown dataMethod class " + m.getClass().getName() + ".");
            }
            m.writeObject(out);
        }
    }

    DataClass(int rootID, DataInput in) throws IOException {
        super(rootID, in);
        //moduleName = "test.java.base";
        name = in.readUTF();
        moduleName = readString(in);
        fullname = readString(in);
        signature = readString(in);
        source = readString(in);
        superName = readString(in);
        superInterfaces = readString(in);
        checksum = in.readLong();
        access = new DataModifiers(in.readInt());
        byte b = in.readByte();
        differentiateClass = (b & 1) != 0;
        inner = (b & 2) != 0;
        anonym = (b & 4) != 0;

        int fieldsNum = in.readShort();
        fields = new ArrayList<DataField>(fieldsNum);
        for (int i = 0; i < fieldsNum; ++i) {
            fields.add(new DataField(this, in));
        }

        int methodsNum = in.readShort();
        methods = new ArrayList<DataMethod>(methodsNum);
        for (int i = 0; i < methodsNum; ++i) {
            byte code = in.readByte();
            switch (code) {
                case 1:
                    methods.add(new DataMethodEntryOnly(this, in));
                    break;
                case 2:
                    methods.add(new DataMethodInvoked(this, in));
                    break;
                case 3:
                    methods.add(new DataMethodWithBlocks(this, in));
                    break;
                default:
                    throw new IOException("DataMethod with unknown code in DataClass " + code);
            }
        }
    }

    public void setInner(boolean inner) {
        this.inner = inner;
    }

    public boolean isInner() {
        return inner;
    }

    public boolean isAnonymous() {
        return anonym;
    }

    public void setAnonym(boolean b) {
        this.anonym = b;
    }

}