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
package com.sun.tdk.jcov.instrument;

import com.sun.tdk.jcov.data.FileFormatException;
import com.sun.tdk.jcov.io.Reader;
import com.sun.tdk.jcov.runtime.Collect;
import com.sun.tdk.jcov.runtime.FileSaver;
import com.sun.tdk.jcov.tools.OptionDescr;
import com.sun.tdk.jcov.util.DebugUtils;
import com.sun.tdk.jcov.util.Utils;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.Adler32;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.TraceClassVisitor;

/**
 * @author Dmitry Fazunenko
 * @author Alexey Fedorchenko
 */
public class ClassMorph {

    private DataRoot root;
    private final String outputFile;
    //    private List<String> instrumented = new ArrayList<String>();
//    private HashSet<String> instrumented = new HashSet<String>(); // Set should work better
    private HashMap<String, Long> instrumented = new HashMap<String, Long>(); // Set should work better
    private HashMap<String, byte[]> instrumentedValues = new HashMap<String, byte[]>(); // Set should work better
    private static final boolean IS_SELFTEST = System.getProperty("jcov.selftest") != null;
    private final InstrumentationParams params;
    private boolean rtClassesInstrumented = false;
    private static final Logger logger;
    private String currentModuleName = null;

    static {
        Utils.initLogger();
        logger = Logger.getLogger(ClassMorph.class.getName());
    }

    //For Agent
    public ClassMorph(String filename, DataRoot root, final InstrumentationParams params) {
        this.outputFile = filename;
        this.root = root;
        this.params = params; // can't use params from root - they can be different from instrumenting ones (? - but not at the moment)
        rtClassesInstrumented = params.isDataSaveSpecified();
        findAlreadyInstrumentedAndSetID();
    }

    /**
     * Default constructor for Instr and TemplGen. Uses specified template as
     * output file.
     */
    public ClassMorph(final InstrumentationParams params, String template) {
        this(template, new DataRoot(params), params);

        this.root.getXMLHeadProperties().put("os.name", System.getProperty("os.name"));
        this.root.getXMLHeadProperties().put("os.arch", System.getProperty("os.arch"));
        this.root.getXMLHeadProperties().put("os.version", System.getProperty("os.version"));
        this.root.getXMLHeadProperties().put("user.name", System.getProperty("user.name"));
        this.root.getXMLHeadProperties().put("java.version", System.getProperty("java.version"));
        this.root.getXMLHeadProperties().put("java.runtime.version", System.getProperty("java.runtime.version"));
    }

    public boolean isTransformable(String className) {
        if (className.equals("java/lang/Object") && !params.isDynamicCollect() && params.isIncluded(className)) {
            logger.log(Level.WARNING, "java.lang.Object can't be statically instrumented and was excluded");
            return false;
        }

        if (className.startsWith("com/sun/tdk/jcov") && !IS_SELFTEST || className.startsWith("org/objectweb/asm")) {
            logger.log(Level.INFO, "{0} - skipped (should not perform self-instrument)", className);
            return false;
        }

        if (className.startsWith("sun/reflect/Generated")) {
            logger.log(Level.WARNING, "{0} - skipped (should not instrument generated classes)");
            return false;
        }

        return true;
    }

    public boolean isAlreadyTransformed(String className) {
        return instrumented.containsKey(className);
    }

    public boolean shouldTransform(String className) {
        return isTransformable(className)
                && !isAlreadyTransformed(className)
                && params.isIncluded(className);
    }

    /**
     * <p> Instrument loaded class data. </p>
     *
     * @param classfileBuffer Class data
     * @param loader ClassLoader containing this class (used in agent)
     * @param flushPath
     * @return
     * @throws IOException
     */
    public byte[] morph(byte[] classfileBuffer, ClassLoader loader, String flushPath) throws IOException {
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }

        if (classfileBuffer[0] != -54 || classfileBuffer[1] != -2 || classfileBuffer[2] != -70 || classfileBuffer[3] != -66) {
            throw new IOException("Not a java classfile (0xCAFEBABE header not found)");
        }

        OffsetLabelingClassReader cr = new OffsetLabelingClassReader(classfileBuffer);

        String fullname = cr.getClassName();

        if (!isTransformable(fullname)) {
            return null;
        }

        boolean shouldFlush = (flushPath != null);

        if (isAlreadyTransformed(fullname)) {
            long cs = computeCheckSum(classfileBuffer);
            Long oldCs = instrumented.get(fullname);
            if (oldCs > 0) {
                if (cs == oldCs && flushPath != null) { // the same class - restore from flushed
                    logger.log(Level.FINE, "{0} - instrumented copy used", fullname);
                    return DebugUtils.readClass(fullname, flushPath);
                } else {
                    logger.log(Level.WARNING, "application has different classes with the same name: {0}", fullname);
                }
            }
        }

        if (params.isClassesReload()
                && !shouldTransform(fullname)
                && isAlreadyTransformed(fullname)
                && params.isDynamicCollect()) {
            return instrumentedValues.get(fullname);
        }

        if (!shouldTransform(fullname)) {
            if (!params.isDynamicCollect() || // isStatic
                    !params.isCallerFilterOn() && !params.isInstrumentFields()) {
                if (isAlreadyTransformed(fullname)) {
                    logger.log(Level.INFO, "{0} - skipped (already instrumented)", fullname);
                }
                if (!params.isIncluded(fullname)) {
                    logger.log(Level.INFO, "{0} - skipped (is not included or is excluded explicitly)", fullname);
                }
                //null tells to AbstractUniversalInstrumenter not to overwrite existing data
                return null;
            }

            // support of caller_include feature
            // even those classes which are out of scope require some minor
            // transformation

            ClassWriter cw = new OverriddenClassWriter(cr, ClassWriter.COMPUTE_MAXS, loader);
            ClassVisitor cv = shouldFlush ? new TraceClassVisitor(cw, DebugUtils.getPrintWriter(fullname, flushPath))
                    : cw;
            cv = new InvokeClassAdapter(cv, params);

            cr.accept(cv, 0);
            byte[] res = cw.toByteArray();
            if (shouldFlush) {
                DebugUtils.flushInstrumentedClass(flushPath, fullname, res);
            }

            if (!params.isDynamicCollect() && !rtClassesInstrumented && isPreVMLoadClass(fullname)) {
                rtClassesInstrumented = true;
                logger.log(Level.WARNING, "It's possible that you are instrumenting classes which are loaded before VM is loaded. It's recomended to add saveatend at java/lang/Shutdown.runHooks method. Data could be lost otherwise.");
            }

            return res;
        }

        String moduleName = null;
        if (params.isDynamicCollect()) {
            moduleName = updateClassModule(fullname);
        }
        else{
            if (currentModuleName != null) {
                moduleName = "module " + currentModuleName;
            }
        }
        if (moduleName == null){
            moduleName = "module "+XmlNames.NO_MODULE;
        }

        if (!params.isModuleIncluded(moduleName.substring(7, moduleName.length()))){
            return null;
        }

        // Checksum should be counted before changing content
        long checksum = params.isDynamicCollect() ? -1
                : computeCheckSum(classfileBuffer);

        // The stackmap should be calculated only when static instrumentation
        // is used and class version 50. In this case the classfiles should
        // be downgraded to v 49.

        int opt = ClassWriter.COMPUTE_MAXS;
        if (params.isStackMapShouldBeUpdated()) {
            if (params.isDynamicCollect() && classfileBuffer[7] == 50) {
                classfileBuffer[7] = 49;
            }
        }

        if (classfileBuffer[7] > 49) {
            opt = ClassWriter.COMPUTE_FRAMES;
        }

        ClassWriter cw = new OverriddenClassWriter(cr, opt, loader);
        DataClass k = new DataClass(root.rootId(), fullname, moduleName.substring(7), checksum, false);
//        ClassVisitor cv = shouldFlush ? new TraceClassVisitor
//                (cw, DebugUtils.getPrintWriter(fullname, Options.getFlushPath())) :
//                cw;
        ClassVisitor cv = cw;
        cv = new DeferringMethodClassAdapter(cv, k, params);

        cr.accept(cv, new Attribute[]{new CharacterRangeTableAttribute(root.rootId())}, 0);

        if (k.hasModifier(Opcodes.ACC_SYNTHETIC) && !params.isInstrumentSynthetic()) {
            return null;
        } else {
            root.addClass(k);
            instrumented.put(fullname, checksum);
            instrumentedValues.put(fullname, cw.toByteArray());

            byte[] res = cw.toByteArray();
            if (shouldFlush) {
                DebugUtils.flushInstrumentedClass(flushPath, fullname, res);
            }

            if (!params.isDynamicCollect() && !rtClassesInstrumented && isPreVMLoadClass(fullname)) {
                rtClassesInstrumented = true;
                logger.log(Level.WARNING, "It's possible that you are instrumenting classes which are loaded before VM is loaded. It's recomended to add saveatend at java/lang/Shutdown.runHooks method. Data could be lost otherwise.");
            }

            return res;
        }
    }

    public void setCurrentModuleName(String name){
        currentModuleName = name;
    }

    private String updateClassModule(String fullname){
        String result = null;
        try{
            if (fullname.contains("$")){
                fullname = fullname.substring(0, fullname.indexOf("$"));
            }
            Class cls = Class.forName(fullname.replaceAll("/","."));
            java.lang.reflect.Method getModuleMethod = Class.class.getDeclaredMethod("getModule", null);
            Object module = getModuleMethod.invoke(cls, null);
            result = module.toString();

        }catch(Throwable ignore){
        }
        return result;
    }

    public static long computeCheckSum(byte[] classfileBuffer) {
//        Adler32 adler = new Adler32();
//        adler.update(classfileBuffer, 0, classfileBuffer.length);
//        long checksum = adler.getValue();
//        return checksum;

        int i = 0;
        i += 4;//skip magic
        i += 4;//skip minor/major version
        int cp_count = ((classfileBuffer[i] & 0xFF) << 8) | (classfileBuffer[i + 1] & 0xFF);
        i += 2;//skip constant pool count

        // Need to cache UTF8 values and their indexes to be able to resolve
        // method and attribute names
//        TreeMap<Integer, String> cp_utf8_cache = new TreeMap();
        HashMap<Integer, String> cp_utf8_cache = new HashMap(); // faster get

        //Process constant pool
        for (int j = 0; j < cp_count - 1; j++) {
            int cp_type = classfileBuffer[i++];
            switch (cp_type) {
                case 1://utf8
                    int utf8_length = ((classfileBuffer[i] & 0xFF) << 8) | (classfileBuffer[i + 1] & 0xFF);
                    i += 2;
//                    byte[] value = Arrays.copyOfRange(classfileBuffer, i, i + utf8_length);
//                    byte[] value = new byte[utf8_length];
//                    System.arraycopy(classfileBuffer, i, value, 0, utf8_length); // jdk1.5 support
                    // String is immutable. No need to copy arrays
                    String sval = new String(classfileBuffer, i, utf8_length, Charset.forName("UTF-8"));
                    cp_utf8_cache.put(j + 1, sval);
                    i += utf8_length;
                    break;
                case 3://integer
                case 4://float
                    i += 4;
                    break;
                case 5://long
                case 6://double
                    j++;
                    i += 8;
                    break;
                case 7://class
                    i += 2;
                    break;
                case 8://string
                    i += 2;
                    break;
                case 9://fieldref
                case 10://methodref
                case 11://interfacemethodref
                    i += 4;
                    break;
                case 12:
                    i += 4;//name and type
                    break;
                case 15: // methodhandle
                    i += 3;
                    break;
                case 16: // methodtype
                    i += 2;
                    break;
                case 18: // invokedynamic
                    i += 4;
                    break;
                case 19: // moduleId
                case 20: // moduleQuery
                    i += 4;
                    break;
                default:
                    logger.log(Level.SEVERE, "SHOULD NOT OCCUR: unknown cp_type: {0}", cp_type);
                    break;
            }
        }

        i += 2;//skip access flags
        i += 2;//skip this class
        i += 2;//skip super class
        int i_count = ((classfileBuffer[i] & 0xFF) << 8) | (classfileBuffer[i + 1] & 0xFF);
        i += 2;//skip interfaces count
        i += 2 * i_count;//skip interfaces table

        // Process fields
        int fld_count = ((classfileBuffer[i] & 0xFF) << 8) | (classfileBuffer[i + 1] & 0xFF);
        i += 2;

        byte[] clone = new byte[classfileBuffer.length];
        int clone_ptr = i;
        // Copy all processed data to the clone
        System.arraycopy(classfileBuffer, 0, clone, 0, clone_ptr);

        for (int j = 0; j < fld_count; j++) {
            int f_start = i;
            i += 2 * 3;

            int attr_count = ((classfileBuffer[i] & 0xFF) << 8) | (classfileBuffer[i + 1] & 0xFF);
            i += 2;

            int[][] attr_ptrs = new int[attr_count][2];
            int clone_attr_count = 0;
            for (int k = 0; k < attr_count; k++) {
                int name_index = ((classfileBuffer[i] & 0xFF) << 8) | (classfileBuffer[i + 1] & 0xFF);
                i += 2;//skip attr name
                long fld_attr_length = ((classfileBuffer[i] & 0xFF) << 24) | ((classfileBuffer[i + 1] & 0xFF) << 16)
                        | ((classfileBuffer[i + 2] & 0xFF) << 8) | (classfileBuffer[i + 3] & 0xFF);
                i += 4;//skip attr length
                i += fld_attr_length;
                if (!cp_utf8_cache.get(name_index).contains("Deprecated") /*&&
                         !cp_utf8_cache.get(name_index).contains("ConstantValue")*/) {// skip deprecated attribute
                    attr_ptrs[clone_attr_count][1] = 2 + 4 + (int) fld_attr_length;
                    attr_ptrs[clone_attr_count][0] = i - attr_ptrs[clone_attr_count][1];
                    clone_attr_count++;
                }
            }

            System.arraycopy(classfileBuffer, f_start, clone, clone_ptr, 6);
            clone_ptr += 6;
            clone[clone_ptr++] = (byte) (clone_attr_count >> 8);
            clone[clone_ptr++] = (byte) clone_attr_count;
            // copy all required field attributes
            for (int l = 0; l < clone_attr_count; l++) {
                System.arraycopy(classfileBuffer, attr_ptrs[l][0], clone, clone_ptr, attr_ptrs[l][1]);
                clone_ptr += attr_ptrs[l][1];
            }
        }

        // Process methods. Methods are sorted by their name + description befor
        // being written to the clone
        int mth_count = ((classfileBuffer[i] & 0xFF) << 8) | (classfileBuffer[i + 1] & 0xFF);
        clone[clone_ptr] = classfileBuffer[i];
        clone_ptr++;
        i++;
        clone[clone_ptr] = classfileBuffer[i];
        clone_ptr++;
        i++;

        TreeMap<String, byte[]> methods = new TreeMap();

        for (int j = 0; j < mth_count; j++) {
            int m_start = i;
            i += 2;
            int name_index = ((classfileBuffer[i] & 0xFF) << 8) | (classfileBuffer[i + 1] & 0xFF);
            i += 2;
            int descriptor_index = ((classfileBuffer[i] & 0xFF) << 8) | (classfileBuffer[i + 1] & 0xFF);
            i += 2;
            int attr_count = ((classfileBuffer[i] & 0xFF) << 8) | (classfileBuffer[i + 1] & 0xFF);
            i += 2;//skip count;

            int[][] attr_ptrs = new int[attr_count][2];
            int clone_attr_count = 0;
            int whole_attr_length = 0;
            for (int k = 0; k < attr_count; k++) {
                int attr_name_index = ((classfileBuffer[i] & 0xFF) << 8) | (classfileBuffer[i + 1] & 0xFF);
                i += 2;//skip attr name
                long mth_attr_length = ((classfileBuffer[i] & 0xFF) << 24) | ((classfileBuffer[i + 1] & 0xFF) << 16)
                        | ((classfileBuffer[i + 2] & 0xFF) << 8) | (classfileBuffer[i + 3] & 0xFF);
                i += 4;//skip attr length
                i += mth_attr_length;
                if (!cp_utf8_cache.get(attr_name_index).contains("Deprecated")) {// skip deprecated attribute
                    attr_ptrs[clone_attr_count][1] = 2 + 4 + (int) mth_attr_length;
                    whole_attr_length += attr_ptrs[clone_attr_count][1];
                    attr_ptrs[clone_attr_count][0] = i - attr_ptrs[clone_attr_count][1];
                    clone_attr_count++;
                }
            }

            byte[] data = new byte[2 * 4 + whole_attr_length];
            System.arraycopy(classfileBuffer, m_start, data, 0, 6);
            data[6] = (byte) (clone_attr_count >> 8);
            data[7] = (byte) clone_attr_count;
            int data_ptr = 0;
            for (int l = 0; l < clone_attr_count; l++) {
                System.arraycopy(classfileBuffer, attr_ptrs[l][0], data, data_ptr + 8, attr_ptrs[l][1]);
                data_ptr += attr_ptrs[l][1];
            }

            methods.put(cp_utf8_cache.get(name_index) + cp_utf8_cache.get(descriptor_index), data);
        }

        // sorting methods
        for (byte data[] : methods.values()) {
            System.arraycopy(data, 0, clone, clone_ptr, data.length);
            clone_ptr += data.length;
        }
//        for (String key : methods.keySet()) {
//            int length = methods.get(key).length;
//            System.arraycopy(methods.get(key), 0, clone, clone_ptr, length);
//            clone_ptr += length;
//        }

        // Process class attributes. They are sorted by their names. Some are skipped.
        TreeMap<String, byte[]> attributes = new TreeMap();

        int attr_count = ((classfileBuffer[i] & 0xFF) << 8) | (classfileBuffer[i + 1] & 0xFF);
        i += 2;

        int clone_attr_count = 0;
        for (int k = 0; k < attr_count; k++) {
            int name_index = ((classfileBuffer[i] & 0xFF) << 8) | (classfileBuffer[i + 1] & 0xFF);
            i += 2;//skip attr name
            long class_attr_length = ((classfileBuffer[i] & 0xFF) << 24) | ((classfileBuffer[i + 1] & 0xFF) << 16)
                    | ((classfileBuffer[i + 2] & 0xFF) << 8) | (classfileBuffer[i + 3] & 0xFF);
            i += 4;//skip attr length
            i += class_attr_length;
            if (!cp_utf8_cache.get(name_index).contains("Deprecated")
                    && !cp_utf8_cache.get(name_index).contains("EnclosingMethod")) {
                clone_attr_count++;
                byte[] data = new byte[2 + 4 + (int) class_attr_length];

                System.arraycopy(classfileBuffer, i - data.length, data, 0, data.length);
                attributes.put(cp_utf8_cache.get(name_index), data);
            }
        }

        clone[clone_ptr++] = (byte) (clone_attr_count >> 8);
        clone[clone_ptr++] = (byte) clone_attr_count;
        // Sorting attributes
        for (byte data[] : attributes.values()) {
            System.arraycopy(data, 0, clone, clone_ptr, data.length);
            clone_ptr += data.length;
        }
//        for (String key : attributes.keySet()) {
//            int length = attributes.get(key).length;
//            System.arraycopy(attributes.get(key), 0, clone, clone_ptr, length);
//            clone_ptr += length;
//        }


        Adler32 adler = new Adler32();
        adler.update(clone, 0, clone_ptr);
        long checksum = adler.getValue();
        return checksum;
    }

    public void updateModuleInfo(HashMap<String, String> moduleInfo){
        if (root != null){
            for (DataPackage pack : root.getPackages()){
                String moduleName = moduleInfo.get(pack.getName());
                if (moduleName != null) {
                    pack.setModuleName(moduleName);
                }
            }
        }
    }

    private void findAlreadyInstrumentedAndSetID() {
        try {
            if (outputFile == null) {
                return;
            }

            File file = new File(outputFile);
            if (!file.exists()) {
                return;
            }

            DataRoot r;
            if (params.isDynamicCollect()) {
                r = Reader.readXMLHeader(outputFile);
            } else {
                r = Reader.readXML(outputFile, true, null);
                for (DataPackage pack : r.getPackages()) {
                    for (DataClass clazz : pack.getClasses()) {
                        instrumented.put(clazz.getFullname(), clazz.getChecksum());
                    }
                }
            }

            Collect.setSlot(r.getCount());
            r.destroy();
        } catch (FileFormatException ffe) {
            System.err.println("Wrong format of the output file: "+outputFile+". " +
                    "Delete output file to receive coverage data");
        } catch (Exception e) {
            throw new Error(e);
        }

    }

    public void saveData(InstrumentationOptions.MERGE merge) {
        FileSaver fileSaver = FileSaver.getFileSaver(root, outputFile, outputFile, merge, true, false);
        fileSaver.saveResults();
    }

    public void saveData(String outputFile, InstrumentationOptions.MERGE merge) {
        FileSaver fileSaver = FileSaver.getFileSaver(root, outputFile, outputFile, merge, true, false);
        fileSaver.saveResults();
    }

    /**
     * <p> Saves collected DataRoot (template) </p>
     *
     * @param outputTemplateFile File to save the template to
     * @param initialTemplatePath Template to use for saving the new template
     * (would be merged). Can be null if no merging with old template needed
     * @param merge Merging type used in case <code>outputTemplateFile</code>
     * file already exists
     */
    public void saveData(String outputTemplateFile, String initialTemplatePath, InstrumentationOptions.MERGE merge) {
        FileSaver fileSaver = FileSaver.getFileSaver(root, outputTemplateFile, initialTemplatePath, merge, true, false);
        fileSaver.saveResults();
    }
    public final static OptionDescr DSC_FLUSH_CLASSES =
            new OptionDescr("flush", null, "flush instrumented classes",
                    OptionDescr.VAL_SINGLE, null, "Specify path to directory, where to store instrumented classes.\n"
                    + "Directory should exist. Classes will be saved in respect to their package hierarchy.\n"
                    + "Default value is \"none\". Pushing it means you don't want to flush classes.", "none");

    private boolean isPreVMLoadClass(String fullname) {
        // classes (actually only certain packages needed) which are loaded before
        // VM is loaded - add classname (eg through new Exception().getStackTrace()[0])
        // saving to Collect.hit
        return fullname.startsWith("java/lang")
                || fullname.startsWith("sun")
                || fullname.startsWith("java/util");
    }
}
