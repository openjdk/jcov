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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipException;

/**
 * AddToProduct - is the class responsible for injecting content of a
 * jcov_xxx_server.jar into a user application jar file. This class will be
 * executed when the following command is given:
 * <pre>
 * java -jar jcov_xxx_saver.jar myProduct.jar
 * </pre>
 *
 * @author Alexey Fedorchenko
 */
public class AddToProduct {

    // inJars is set of jar files to modified
    private static Set inJars;
    // jarEntryNames is a set to check and inform user about entries duplication
    private static Set jarEntryNames;
    private static final String THIS_CLASS = AddToProduct.class.getName().replace('.', '/') + ".class";

    public static void main(String[] args) {

        CodeSource src = AddToProduct.class.getProtectionDomain().getCodeSource();

        if (src == null) {
            System.err.println("Can't find main file in specified source");
            return;
        }

        URL location = src.getLocation();
        JarFile currentJar = null;
        try {
            currentJar = new JarFile(location.getFile());
        } catch (ZipException ze) {
            System.err.println("Can't read jar file to add it in products jars");
            return;
        } catch (IOException ioe) {
            System.err.println("Can't use this functionality not from jar file");
            return;
        }

        if (args.length == 0) {
            System.err.println("Please specify products jars to add this jar in");
            return;
        }

        inJars = new HashSet<JarFile>();
        for (int i = 0; i < args.length; i++) {
            try {
                inJars.add(new JarFile(args[i]));
            } catch (SecurityException ze) {
                System.out.println("Access to the specified product jar: " + args[i] + " is denied");
            } catch (IOException ioe) {
                System.out.println("Product jar " + args[i] + " doesn't exist");
            }
        }

        if (inJars.isEmpty()) {
            System.err.println("Please specify products jars to add this jar in");
            return;
        }

        Iterator itr = inJars.iterator();
        while (itr.hasNext()) {

            JarFile product = (JarFile) itr.next();
            File archiveFile = new File(product.getName() + "_temp");
            try {
                FileOutputStream stream = new FileOutputStream(archiveFile);
                JarOutputStream fos = new JarOutputStream(stream, product.getManifest());

                jarEntryNames = new HashSet();
                writeEntriesToJar(product.entries(), fos, product);
                writeEntriesToJar(currentJar.entries(), fos, currentJar);

                fos.flush();
                fos.close();

                File origFile = new File(product.getName());
                if (!origFile.delete()) {
                    System.out.println("Can not delete product jar: " + product.getName() + " the result is in the " + product.getName() + "_temp");
                } else {
                    archiveFile.renameTo(origFile);
                }

            } catch (IOException ioe) {
                System.out.println("IOException while processing product jar file: " + product.getName() + " " + ioe);
            }
        }

    }

    private static void writeEntriesToJar(Enumeration<JarEntry> en, JarOutputStream fos, JarFile outJar) throws IOException {

        while (en.hasMoreElements()) {

            JarEntry element = en.nextElement();

            if (!element.getName().equals("META-INF/")
                    && !element.getName().equals("META-INF/MANIFEST.MF")
                    && !element.getName().equals(THIS_CLASS)) {

                if (!jarEntryNames.add(element.getName())) {
                    System.err.println("Duplicate jar entry [" + element.getName() + "]");
                    continue;
                }
                fos.putNextEntry(element);
                InputStream filereader = outJar.getInputStream(element);

                final int buffersize = 1024;
                byte buffer[] = new byte[buffersize];
                int readcount = 0;

                while ((readcount = filereader.read(buffer, 0, buffersize)) >= 0) {
                    if (readcount > 0) {
                        fos.write(buffer, 0, readcount);
                    }
                }

                fos.closeEntry();
                filereader.close();

            }

        }
    }
}
