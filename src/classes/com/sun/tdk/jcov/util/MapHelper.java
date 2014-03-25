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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;

/**
 * For now, there is no way to map counts on legacy data file, because ther is
 * no info about item IDs.
 *
 * @author Sergey Borodin
 */
public class MapHelper {

    public static void mapCounts(String outputFile, String templateFile,
            long[] counts) throws Exception {

        String template = new File(outputFile).exists()
                ? outputFile : templateFile;
        map(outputFile, template, counts);

    }

    private static void map(String output, String templ, long[] counts) throws Exception {
        if (output.equals(templ)) {
            if (counts == null) {
                return; // nothing to do - file already exists
            }
            String out_tmp = output + RuntimeUtils.genSuffix();

            mapXMLFast(out_tmp, templ, counts);

            // we need to close input stream and
            // delete file "output" before renaming another one into it.
            File f_out = new File(output);
            f_out.delete();
            new File(out_tmp).renameTo(f_out);
        } else {
            mapXMLFast(output, templ, counts);
        }
    }

    /**
     * The method assumes that templ is an XML with jcov data. This method
     * provides update of this file without parsing templ into XML model. This
     * methods just reads the given file line by line and modifies those lines
     * which contains "id=...".
     *
     * <b>Note</b> this method is very sensitive to changes of XML format.
     *
     * @param result - output file to be created
     * @param templ - file will be used as a template (it could be result.xml)
     * @param counts - array of counts
     *
     * @throws Exception
     */
    private static void mapXMLFast(String result, String templ, long[] counts) throws Exception {
        checkTemplate(templ);
        FileInputStream is = new FileInputStream(templ);
        File outputFile = new File(result);
        if (!outputFile.getAbsoluteFile().getParentFile().exists()) {
            throw new Exception("Specified directory for output file doesn't exist - " + outputFile.getParentFile().getPath());
        }
        if (outputFile.exists() && !outputFile.canWrite()) {
            throw new Exception("Can't write output file");
        }
        if (outputFile.exists() && !outputFile.canRead()) {
            throw new Exception("Can't read output file");
        }
        FileOutputStream os = new FileOutputStream(outputFile, false);

        BufferedReader r = new BufferedReader(new InputStreamReader(is, Charset.defaultCharset()));
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(os, Charset.defaultCharset()));
        try {
            String s = r.readLine();
            while (s != null) {
                int i = s.indexOf("id=");
                if (i != -1 && counts != null) {
                    int id = 0;
                    long count = 0;

                    i += 4; //id="
                    int id_end = s.indexOf("\"", i);
                    id = Integer.parseInt(s.substring(i, id_end));

                    if (counts[id] == 0) {
                        pw.println(s); // nothing changed
                    } else if (s.indexOf("count", i) > 0) {
                        i = id_end + 9; //"_count="
                        String res = s.substring(0, i);

                        int count_end = s.indexOf("\"", i);
                        count = Long.parseLong(s.substring(i, count_end));
                        count += counts[id];
                        res += count;

                        res += s.substring(count_end);
                        pw.println(res);
                    } else {
                        String res = s.substring(0, id_end + 1) + " count=\"" + counts[id] + "\"" + s.substring(id_end + 1);
                        pw.println(res);
                    }
                } else {
                    pw.println(s);
                }

                s = r.readLine();
            }
            pw.flush();
        } finally {
            pw.close();
            r.close();
            is.close();
            os.close();
        }
    }

    /**
     * Performs some basic checks that given template is a jcov file. Throws
     * exception in case of error found.
     *
     * @param templ - template file name
     */
    private static void checkTemplate(String templ) throws Exception {
        FileInputStream is = new FileInputStream(templ);
        BufferedReader r = new BufferedReader(new InputStreamReader(is, Charset.defaultCharset()));
        try {
            String s = r.readLine();
            if (!"<?xml version='1.0' encoding='UTF-8'?>".equalsIgnoreCase(s)) {
                throw new RuntimeException(templ + " doesn't seem to be an XML file");
            }
            s = r.readLine();
            while (s != null && s.trim().length() == 0) {
                s = r.readLine();
            }
            if (s == null) {
                throw new RuntimeException(templ + " an empty XML file");
            }
            if (!s.startsWith("<coverage")) {
                throw new RuntimeException(templ + " not a jcov coverage file");
            }
        } finally {
            r.close();
            is.close();
        }
    }
}
