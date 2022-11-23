/*
 * Copyright (c) 2014, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.jcov.insert;

import com.sun.tdk.jcov.instrument.InstrumentationParams;
import com.sun.tdk.jcov.instrument.InstrumentationPlugin;
import com.sun.tdk.jcov.instrument.asm.ASMInstrumentationPlugin;
import com.sun.tdk.jcov.instrument.asm.OverriddenClassWriter;
import com.sun.tdk.jcov.util.Utils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.sun.tdk.jcov.util.Utils.FILE_TYPE;
import static com.sun.tdk.jcov.util.Utils.FILE_TYPE.*;
import static com.sun.tdk.jcov.util.Utils.isClassFile;

/**
 * The class is resposible to deal with class files and hierarchies of files, such as directories, jars, zips, modules.
 * The actual logic of bytecode instrumentation is left for subclasses of this class.
 * @see #instrument(byte[], int)
 * @see #finishWork()
 * @author Dmitry Fazunenko
 * @author Alexey Fedorchenko
 */
public abstract class AbstractUniversalInstrumenter {

    private String INSTR_FILE_SUFF = "i";
    private int fileCount = 0; // counter of processed files
    private int classCount = 0; // counter of processed classes
    private int iClassCount = 0; // counter of successfully instrumented classes

    private static final Logger logger;

    private InstrumentationPlugin plugin = new ASMInstrumentationPlugin();
    private InstrumentationParams params;

    static {
        Utils.initLogger();
        logger = Logger.getLogger(AbstractUniversalInstrumenter.class.getName());
    }
    /**
     * buffer for reading class data (either from a class file or from an
     * archive)
     */
    protected byte[] classBuf = new byte[32 * 1024];
    /**
     * whether to overwrite files with their instrumented versions
     */
    protected boolean overwrite;
    /**
     * whether to save instrumentation results or not
     */
    protected boolean readOnly;

    public AbstractUniversalInstrumenter(boolean overwrite) {
        this(overwrite, false);
    }

    /**
     * <p> Creates AbstractUniversalInstrumenter instance </p>
     *
     * @param overwrite Overwrite existing binary
     * @param readOnly Do not produce instrumented binaries (template generation
     * purposes)
     */
    public AbstractUniversalInstrumenter(boolean overwrite, boolean readOnly) {
        this.overwrite = overwrite;
        this.readOnly = readOnly;
    }

    public void setParams(InstrumentationParams params) {
        this.params = params;
    }

    /**
     * constructs instrumented file name by inserting
     * InstrConstants.INSTR_FILE_SUFF string before the first character of the
     * extension
     *
     * @param name file name to construct new name for
     * @return constructed name
     */
    protected String makeInstrumentedFileName(String name) {
        int dotInd = name.lastIndexOf('.');
        if (dotInd <= 0) {
            logger.log(Level.SEVERE, "Invalid classfile name: ''{0}''", name);
        }
        return name.substring(0, dotInd) + "." + INSTR_FILE_SUFF + name.substring(dotInd + 1, name.length());
    }

    /**
     * ensures that the class data buffer is capable of storing the specified
     * number of bytes
     *
     * @param length number of bytes to be stored
     * @param copy_old_buf preserve contents of the buffer if it must be
     * reallocated
     */
    private void ensureClassBufLength(int length, boolean copy_old_buf) {
        if (classBuf.length >= length) {
            return;
        }
        byte[] tmp_buf = new byte[length + length / 2];
        if (copy_old_buf) {
            System.arraycopy(classBuf, 0, tmp_buf, 0, classBuf.length);
        }
        classBuf = tmp_buf;
    }

    /**
     * instruments specified class file
     *
     * @param f class file to be instrumented
     * @return whether the file represented by f has been instrumented
     */
    protected boolean processClassFile(File f, File outFile) throws IOException {
        String fname = f.getPath();
        // to suppress verbosity: logger.log(Level.INFO, "Instrumenting classfile ''{0}''...", fname);
        boolean instredFine = true;
        int classLength = (int) f.length();

        if (f.getName().equals("module-info.class")){
            return true;
        }

        ensureClassBufLength(classLength, false);
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(f);
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, "  File not found - skipped", e);
            return false;
        }
        try {
            fis.read(classBuf, 0, classLength); // read in class data
            fis.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "  Error reading '" + fname + "' - skipped", e);
            return false;
        }

        byte[] outBuf = null;
        try {
            if (f.length() < 4) {
                if (f.length() == 0) {
                    logger.log(Level.SEVERE, "  Error reading data from ''{0}'': File is empty\n - skipped",
                            fname);
                    instredFine = false;
                } else {
                    logger.log(Level.SEVERE, "  Error reading data from ''{0}'': File is too small ({1}) - skipped",
                            new Object[]{fname, f.length()});
                    instredFine = false;
                }
            } else {
                outBuf = instrument(classBuf, classLength); // instrument the class
//                System.out.println(f + " " + classBuf.length + " " + (outBuf == null ? "null" : outBuf.length));
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "  Error reading data from '" + fname + "' - skipped", e);
            instredFine = false;
        } catch (NullPointerException e) {
            logger.log(Level.SEVERE, "  Error reading data from '" + fname + "' - skipped", e);
            instredFine = false;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "  Error instrumenting '" + fname + "' - skipped", e);
            instredFine = false;
        }
        if (outBuf == null) {
            // class can't be/is already instrumented
            instredFine = false;
            outBuf = classBuf;
        } else {
            classLength = outBuf.length;
        }

        if (!readOnly) {
            // construct instrumented class file name
            if (outFile == null) {
                outFile = f;
                f.delete();
            }

            // create "super" directories if necessary
            String parentName = outFile.getParent();
            if (parentName != null) {
                File parent = new File(parentName);
                if (!parent.exists()) {
                    parent.mkdirs();
                }
            }
            // write instrumented classfile
            FileOutputStream fos = new FileOutputStream(outFile);
            fos.write(outBuf, 0, classLength);
            fos.close();
        }
        return instredFine;
    }

    /**
     * instruments all class files in the specified directory and its
     * subdirectories.
     *
     * @param dir directory to be instrumented
     */
    protected void processClassDir(File dir, File outDir) throws IOException {
        String[] entries = dir.list();
        Arrays.sort(entries);
        for (int i = 0; i < entries.length; i++) {
            File f = new File(dir.getPath() + File.separator + entries[i]);
            if (f.isDirectory()) {
                processClassDir(f, new File(outDir, entries[i]));
            } else {
                fileCount++;
                if (FILE_TYPE.hasExtension(f.getPath(), CLASS)) {
                    classCount++;
                    if (processClassFile(f, new File(outDir, entries[i]))) {
                        iClassCount++;
                    }
                }
            }
        }
    }

    /**
     * instruments classes in specified jar- or zip-archive
     *
     * @param arc archive to be instrumented
     * @param rtPath runtime jar to me implanted. Ignored when null
     */
    protected void processArc(File arc, File outArc, String rtPath) {
        logger.log(Level.INFO, " - Instrumenting archive ''{0}''...", arc);
        String outFilename = outArc == null ? makeInstrumentedFileName(arc.getPath()) :
                outArc.getPath() + File.separator + arc.getName();
        logger.log(Level.CONFIG, "  Output archive ''{0}''", outFilename);
        if (rtPath != null) {
            logger.log(Level.CONFIG, "  RT to implant: ''{0}''", rtPath);
        }
        CRC32 crc32 = new CRC32();
        ZipInputStream in;
        try {
            in = new ZipInputStream(new BufferedInputStream(new FileInputStream(arc)));
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "  Error opening archive '" + arc.getPath() + "'", ex);
            return;
        }
        ZipOutputStream out = null;
        // construct instrumented archive name and create the file
        File outFile = new File(outFilename);
        if (!readOnly) {
            try {
                out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)));
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "  Error creating output archive '" + outFilename + "'", ex);
            }
        }

        while (true) {
            // cycle by all entries in the archive
            ZipEntry e0 = null; // last read entry
            try {
                e0 = in.getNextEntry();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "  Error reading archive entry in '" + arc.getPath() + "'", ex);
            }
            if (e0 == null) {
                // are there any remaining entries?
                break;
            }
            String ename = e0.getName();
            boolean isDir = e0.isDirectory();
            if (!isDir) {
                fileCount++;
            }

            int fileLength = (int) e0.getSize();
            try {
                if (fileLength >= 0) {
                    // is length of the entry known?
                    ensureClassBufLength(fileLength, false);
                    // in.read(byte[],int,int) has a bug in JDK1.1.x - using in.read()...
                    for (int i = 0; i < fileLength; classBuf[i++] = (byte) in.read()) {
                    }
                    in.read(classBuf, 0, fileLength);
                } else {
                    // no - read until the end of stream is encountered
                    int i = 0;
                    for (;; i++) {
                        int b = in.read();
                        if (b < 0) {
                            break;
                        }
                        ensureClassBufLength(i + 1, true); // make sure classBuf is big enough
                        classBuf[i] = (byte) b;
                    }
                    fileLength = i;
                }
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "  Error reading archive entry '" + ename + "' in '" + arc.getPath() + "' - skipped", ex);
            }

            byte[] res;
            boolean isClass = ename.endsWith(CLASS.getExtension()) && !isDir;
            if (isClass) {
                // does the entry represent a class file?
                classCount++;
                logger.log(Level.INFO, "  Instrumenting ''{0}''...", ename);
                try {
                    res = instrument(classBuf, fileLength); // try to instrument it
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, "    Error reading archive entry '" + ename + "' in '" + arc.getPath() + "' - skipped", ex);
                    res = null;
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "    Error instrumenting archive entry '" + ename + "' in '" + arc.getPath() + "' - skipped", ex);
                    res = null;
                }
            } else {
                // no - will write the buffer unchanged
                res = classBuf;
                logger.log(Level.INFO, "  Storing ''{0}''...", ename);
            }

            if (!readOnly) {
                ZipEntry e1 = new ZipEntry(ename); // entry to be written to the resulting archive
                e1.setSize(fileLength);
                e1.setMethod(e0.getMethod());
                e1.setExtra(e0.getExtra());
                e1.setComment(e0.getComment());
                long crc = e0.getCrc();
                if (crc >= 0) {
                    e1.setCrc(crc);
                }
                if (res != null && isClass) {
                    // have the class been instrumented?
                    if (res.length > 1) {
                        iClassCount++;
                        if (e0.getCrc() != -1) {
                            // update CRC if needed
                            crc32.reset();
                            crc32.update(res);
                            e1.setCrc(crc32.getValue());
                        }
                        fileLength = res.length; // set new length
                        e1.setSize(fileLength);
                    } else {
                        if (res.length == 1 && res[0] == (byte) 'P') {
                            logger.log(Level.WARNING, "    skipped: first pass native preview");
                        }
                        if (res.length == 1 && res[0] == (byte) 'F') {
                            logger.log(Level.WARNING, "    skipped: filtered out");
                        }
                        res = classBuf;
                    }
                } else {
                    // no - will write the buffer unchanged
                    res = classBuf;
                    if (!isClass) {
                        logger.log(Level.FINE, "    ''{0}'' - not instrumented (not a class)", ename);
                    } else {
//                        logger.log(Level.WARNING, "    ''{0}'' - not instrumented", ename);
                    }
                }

                try {
                    out.putNextEntry(e1); // write the entry to the resulting archive
                    out.write(res, 0, fileLength); // write actual entry data as well
                    out.closeEntry();
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, "Error adding archive entry '" + ename + "' to '" + outFilename + "' - skipped", ex);
                }
            }
        }

        // all entries read/processed/written - close input stream
        try {
            in.close();
        } catch (IOException ex) {
        }

        try {
            // coping JCovRT classes to the output jar
            if (rtPath != null) {
                logger.log(Level.INFO, "  Adding saver library...");
                File rt = new File(rtPath);
                if (!rt.exists()) {
                    throw new IOException("Runtime file doesn't exist " + rt);
                }
                if (!rt.isFile() || !FILE_TYPE.hasExtension(rt.getName(), JAR, ZIP)) {
                    throw new IOException("Malformed runtime archive " + rt);
                }
                ZipInputStream rtIn = new ZipInputStream(new BufferedInputStream(new FileInputStream(rt)));

                outer:
                while (true) {
                    ZipEntry e0 = rtIn.getNextEntry();
                    if (e0 == null) {
                        // are there any remaining entries?
                        break;
                    }
                    String ename = e0.getName();
                    if (ename.startsWith("META-INF")) {
                        continue;
                    }

                    int fileLength = (int) e0.getSize();
                    if (fileLength >= 0) {
                        // is length of the entry known?
                        ensureClassBufLength(fileLength, false);
                        // in.read(byte[],int,int) has a bug in JDK1.1.x - using in.read()...
                        for (int i = 0; i < fileLength; classBuf[i++] = (byte) rtIn.read());
                        rtIn.read(classBuf, 0, fileLength);
                    } else {
                        // no - read until the end of stream is encountered
                        int i = 0;
                        for (;; i++) {
                            int b = rtIn.read();
                            if (b < 0) {
                                break;
                            }
                            ensureClassBufLength(i + 1, true); // make sure classBuf is big enough
                            classBuf[i] = (byte) b;
                        }
                        fileLength = i;
                    }
                    ZipEntry e1 = new ZipEntry(ename); // entry to be written to the resulting archive
                    e1.setSize(fileLength);
                    e1.setMethod(e0.getMethod());
                    e1.setExtra(e0.getExtra());
                    e1.setComment(e0.getComment());
                    long crc = e0.getCrc();
                    if (crc >= 0) {
                        e1.setCrc(crc);
                    }

                    ZipInputStream inJar = new ZipInputStream(new BufferedInputStream(new FileInputStream(arc)));
                    try {
                        ZipEntry checking;
                        while (true) {
                            checking = inJar.getNextEntry();
                            if (checking == null) {
                                break;
                            }
                            if (checking.getName().equals(e1.getName())) {
                                if (checking.getSize() != e1.getSize()) {
                                    // logger.log(Level.WARNING, "Output jar contains file {0} that was found in runtime jar but files sizes differ, skipping",
                                    // checking.getName());
                                } else {
                                    // logger.log(Level.INFO, "Output jar contains file {0} that was found in runtime jar, skipping",
                                    // checking.getName());
                                }
                                inJar.close();
                                continue outer;
                            }
                        }
                    } catch (EOFException e) {
                        // logger.log(Level.SEVERE);
                    } finally {
                        inJar.close();
                    }/**/

                    // logger.log(Level.INFO, "Adding runtime file {0} to output jar {1}", new Object[]{e1.getName, jar.getName()})
                    try {
                        out.putNextEntry(e1); // write the entry to the resulting archive
                    } catch (ZipException e) {
                        if (!e.getMessage().startsWith("duplicate entry")) {
                            throw e;
                        }
                    }
                    out.write(classBuf, 0, fileLength); // write actual entry data as well
                    out.closeEntry();
                }

                rtIn.close();
            }

            if (out != null) {
                out.finish();
                out.close();
            }
        } catch (Throwable ex) {
            logger.log(Level.SEVERE, "Error processing archive '" + arc.getPath() + "'", ex);
            outFile.delete();
            return;
        }
        if (!readOnly && outArc == null && overwrite) {
            if (!arc.delete()) {
                logger.log(Level.WARNING, " Can''t remove initial JAR file ''{0}''",
                        arc.getAbsolutePath());
            }
            if (!outFile.renameTo(arc)) {
                logger.log(Level.WARNING, " Can''t rename result JAR file ''{0}' to ''{1}''. Please move manually",
                        new Object[]{outFile.getAbsolutePath(), arc.getAbsolutePath()});
            }
        }
    }

    public void instrument(String arg) throws IOException {
        instrument(new File(arg), null, null, false);
    }

    /**
     * determines what kind of object is represented by given path and invokes
     * appropriate methods to instrument the object
     *
     * @param arg path to a class file, zip/jar archive or a directory
     */
    public void instrument(File arg, File outDir) throws IOException {
        instrument(arg, outDir, null, false);
    }

    public void instrument(File arg, File outDir, String rtPath) throws IOException {
        instrument(arg, outDir, rtPath, false);
    }

    public void instrument(File instrumentingPath, File destinationPath, String rtPath, boolean recursive) throws IOException {
        instrument(instrumentingPath, destinationPath, rtPath, null, recursive);
    }

    /**
     * <p> Determines what kind of object is represented by given path and
     * invokes appropriate methods to instrument the object </p>
     *
     * @param instrumentingPath path to a class file, zip/jar archive or a
     * directory
     * @param destinationPath output path. Initial file is replaced in case if
     * null
     * @param rtPath path to runtime jar. Ignored if null
     * @param recursive insrument method will recurse through initial directory
     * if true instrumenting each file in the tree
     */
    public void instrument(File instrumentingPath, File destinationPath,
                           String rtPath, ArrayList<String> rtClassDirTargets,
                           boolean recursive) throws IOException {
//        fileCount = 0;
//        classCount = 0;
//        iClassCount = 0;

        if (!instrumentingPath.exists()) {
            logger.log(Level.WARNING, "Path ''{0}'' doesn''t exist - skipped", instrumentingPath);
            return;
        }
        boolean isClassFile = false;
        if (instrumentingPath.isDirectory()) {
            if (destinationPath == null) {
                destinationPath = instrumentingPath;
            }
            Path in = Path.of(instrumentingPath.getAbsolutePath());
            Path out = Path.of(destinationPath.getAbsolutePath());
            List<String> classes = Files.find(in, Integer.MAX_VALUE,
                            (f, a) -> f.getFileName().toString().endsWith(".class"))
                    .map(f -> in.relativize(f))
                    .map(Path::toString)
                    .collect(Collectors.toList());
//            try {
//                plugin.instrument(classes, f -> {
//                    try {
//                        return Files.readAllBytes(in.resolve(f));
//                    } catch (IOException e) {
//                        throw new UncheckedIOException(e);
//                    }
//                }, (c, d) -> {
//                    try {
//                        Files.write(out.resolve(c), d);
//                    } catch (IOException e) {
//                        throw new UncheckedIOException(e);
//                    }
//                }, params);
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
            //TODO what about recursive?
//            if (recursive) {
//
//                Utils.addToClasspath(instrumentingPath);
//
//                logger.log(Level.FINE, "Scanning directory ''{0}''...", instrumentingPath);
//                File[] entries = instrumentingPath.listFiles();
//                Arrays.sort(entries);
//                for (int i = 0; i < entries.length; i++) {
//                    destinationPath.mkdir();
//                    instrument(entries[i], new File(destinationPath, entries[i].getName()), rtPath, rtClassDirTargets, recursive);
//                }
//            } else {
//                logger.log(Level.INFO, "Instrumenting directory ''{0}''...", instrumentingPath);
//                processClassDir(instrumentingPath, destinationPath);
//                if (rtPath != null) {
//                    if (destinationPath != null) {
//                        unjarRT(rtPath, destinationPath);
//                    } else {
//                        unjarRT(rtPath, instrumentingPath);
//                    }
//                }
//            }
        } else if ( FILE_TYPE.hasExtension(instrumentingPath.getName(), JAR, ZIP, WAR) ) {
            // initially instrument(jar, toJar) meant create instrumented "toJar/jar". But in recursive mode it should be just "toJar".
            if (rtClassDirTargets == null || rtClassDirTargets.contains(instrumentingPath.getPath())) {
                if (recursive) {
                    processArc(instrumentingPath, destinationPath.getParentFile(), rtPath);
                } else {
                    processArc(instrumentingPath, destinationPath, rtPath);
                }
            } else {
                if (recursive) {
                    processArc(instrumentingPath, destinationPath.getParentFile(), null);
                } else {
                    processArc(instrumentingPath, destinationPath, null);
                }
            }
        } else if (instrumentingPath.getName().equals("modules")) {
            instrumentModulesFile(instrumentingPath, destinationPath);
        } else if (isClassFile(instrumentingPath.getName())) {
            if (destinationPath == null) {
                destinationPath = instrumentingPath;
            }
            processClassFile(instrumentingPath, destinationPath);
            isClassFile = true;
        } else {
            if (destinationPath == null) {
                destinationPath = instrumentingPath;
            }
            Utils.copyFile(instrumentingPath, destinationPath);
            return;
        }
        if (!isClassFile) {
            logger.log(Level.INFO, "Summary for ''{0}'': files total={1}, classes total={2}, instrumented classes total={3}",
                    new Object[]{instrumentingPath, fileCount, classCount, iClassCount});
        }
    }

    public void processClassFileInModules(Path file, File destinationPath){
        String fname = file.toAbsolutePath().toString();
        try {
            final String distinationStr = destinationPath.toString();
            classBuf = Files.readAllBytes(file);
            int classLength = classBuf.length;
            byte[] outBuf = null;
            try {
                if (!"module-info.class".equals(file.getFileName().toString())) {
                    outBuf = instrument(classBuf, classLength);
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "  Error reading data from '" + fname + "' - skipped", e);
            } catch (NullPointerException e) {
                logger.log(Level.SEVERE, "  Error reading data from '" + fname + "' - skipped", e);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "  Error instrumenting '" + fname + "' - skipped", e);
            }
            if (outBuf != null) {

                classLength = outBuf.length;

                File outFile = new File(distinationStr + File.separator + fname);
                String parentName = outFile.getParent();
                if (parentName != null) {
                    File parent = new File(parentName);
                    if (!parent.exists()) {
                        parent.mkdirs();
                    }
                }
                // write instrumented classfile
                FileOutputStream fos = new FileOutputStream(outFile);
                fos.write(outBuf, 0, classLength);
                try {
                    fos.close();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "  Error writing data to '" + outFile.getAbsolutePath() + "' - skipped", e);
                }
            }
        }
        catch(IOException e){
            logger.log(Level.SEVERE, "  Error processing classFile by Path '" + fname + "' - skipped", e);
        }
    }

    private void instrumentModulesFile(File modulePath, final File destinationPath){

        try {
            Utils.addToClasspath(modulePath.getParentFile().getParentFile());
            FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
            final Path path = fs.getPath("/modules/");

            Files.walkFileTree(path, new SimpleFileVisitor<Path>(){
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if(!attrs.isDirectory() && isClassFile(file.getFileName().toString())){
                        OverriddenClassWriter.addClassInfo(Files.newInputStream(file));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            Files.walkFileTree(path, new SimpleFileVisitor<Path>(){
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if(!attrs.isDirectory() && isClassFile(file.getFileName().toString())){
                        processClassFileInModules(file, destinationPath);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Modules instrumentaion failed", e);
        }

    }

    /**
     * <p> Treats given byte array as a class data and instruments the class.
     * Any Exception thrown from this metod will be treated as error during
     * instrumentation, class will be skipped. </p> <p> It's called for
     * instrumenting each binary file </p>
     *
     * @param classData class data to be instrumented
     * @param classLength length of the data
     * @return bytes of instrumented class. null means class won't be changed.
     */
    protected abstract byte[] instrument(byte[] classData, int classLength) throws IOException;

    public abstract void finishWork();

    /**
     * Get classfiles from a jar file. Use to implant rt classfiles to unpacked
     * binaries.
     *
     * @param rtJar
     * @param output
     * @throws IOException
     */
    public void unjarRT(String rtJar, File output) throws IOException {
        if (output == null) {
            throw new IllegalArgumentException("Output path (jar file or classfile directory) needed to copy runtime jar file to");
        }
        File rt = new File(rtJar);
        if (!rt.exists()) {
            throw new IOException("Runtime file doesn't exist " + rt);
        }
        if (!rt.isFile() || !FILE_TYPE.hasExtension(rt.getName(), JAR, ZIP)) {
            throw new IOException("Malformed runtime archive " + rt);
        }

        ZipInputStream in = new ZipInputStream(new BufferedInputStream(new FileInputStream(rt)));
        while (true) {
            ZipEntry e0 = in.getNextEntry();
            if (e0 == null) {
                // are there any remaining entries?
                break;
            }
            String ename = e0.getName();
            if (ename.startsWith("META-INF")) {
                continue;
            }

            int fileLength = (int) e0.getSize();
            if (fileLength >= 0) {
                // is length of the entry known?
                ensureClassBufLength(fileLength, false);
                // in.read(byte[],int,int) has a bug in JDK1.1.x - using in.read()...
                for (int i = 0; i < fileLength; classBuf[i++] = (byte) in.read());
                in.read(classBuf, 0, fileLength);
            } else {
                // no - read until the end of stream is encountered
                int i = 0;
                for (;; i++) {
                    int b = in.read();
                    if (b < 0) {
                        break;
                    }
                    ensureClassBufLength(i + 1, true); // make sure classBuf is big enough
                    classBuf[i] = (byte) b;
                }
                fileLength = i;
            }

            File outFile = new File(output.getPath() + File.separator + e0.getName());
            if (!e0.isDirectory()) {
                if (outFile.exists()) {
                    // logger.log(Level.WARNING, "Output classfile directory contains file {0} that was found in runtime jar,
                    // skipping without contains checking", e0.getName());
                    continue;
                } else {
                    // logger.log(Level.INFO, "Adding runtime file {0} to output directory {1}",
                    // new Object[]{e1.getName, outDir.getName()})
                    String parentName = outFile.getAbsoluteFile().getParent();
                    if (parentName != null) {
                        File parent = new File(parentName);
                        if (!parent.exists()) {
                            parent.mkdirs();
                        }
                    }
                    FileOutputStream fos = new FileOutputStream(outFile);
                    fos.write(classBuf, 0, fileLength);
                    fos.close();
                }
            }
        }
        in.close();
        }
}
