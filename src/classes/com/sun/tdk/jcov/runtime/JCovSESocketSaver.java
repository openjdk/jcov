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
package com.sun.tdk.jcov.runtime;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Properties;

/**
 *
 * @author Sergey Borodin
 */
public class JCovSESocketSaver extends JCovSocketSaver {

    public static final String NETWORK_DEF_PROPERTIES_FILENAME = "jcov_network_default.properties";
    public static final String PORT_PROPERTIES_NAME = "port";
    public static final String HOST_PROPERTIES_NAME = "host";

    static {

        File file = null;
        String urlString = "";
        try {
            urlString = ClassLoader.getSystemClassLoader().getResource(JCovSESocketSaver.class.getCanonicalName().replaceAll("\\.", "/") + ".class").toString();
            urlString = urlString.substring(urlString.indexOf("file:"), urlString.indexOf('!'));
            URL url = new URL(urlString);
            file = new File(url.toURI());
        } catch (Exception e) {
            System.err.println("Error while finding " + urlString + " file: " + e);
        }

        if (file != null && file.exists()) {

            File defProperties = new File(file.getParent() + File.separator + NETWORK_DEF_PROPERTIES_FILENAME);

            if (defProperties.exists()) {

                Properties prop = new Properties();

                try {
                    prop.load(new FileInputStream(defProperties));
                    if (prop.getProperty(PORT_PROPERTIES_NAME) != null) {
                        setDefaultPort(Integer.valueOf(prop.getProperty(PORT_PROPERTIES_NAME)));
                    }
                    if (prop.getProperty(HOST_PROPERTIES_NAME) != null) {
                        setDefaultHost(prop.getProperty(HOST_PROPERTIES_NAME));
                    }
                } catch (Exception e) {
                    System.err.println("Error while reading " + defProperties.getAbsolutePath() + " file: " + e);
                }
            }
        }

    }

    public synchronized void saveResults() {
        try {
            host = detectHost();
            port = detectPort();
            String testname = PropertyFinder.findValue("testname", null);
            if (testname == null) {
                testname = PropertyFinder.findValue("file", "result.xml");
            } else {
                if ("<jcov.ignore>".equals(testname)) {
                    return; // ignoring this test data
                }
            }

            int count = 0;
            final long[] data = Collect.counts();
            final long[] dataVal = new long[data.length];
            final int[] dataIdx = new int[data.length];
            int lastIndex = 0;
            for (int i = 0; i < Collect.MAX_SLOTS; i++) {
                if (data[i] != 0) {
                    dataIdx[count] = i;
                    dataVal[count] = data[i];
                    lastIndex = i;
                    count++;
                }
            }

            Socket s = null;

            /* Make 3 attempts to connect with JCOV server */
            for (int i = 0; i < 3; i++) {
                try {
                    s = new Socket(host, port);
                } catch (UnknownHostException e) {
                    System.err.println("JCovRT: Can't resolve hostname " + host
                            + " - unknown host. Exiting. ");
                    return;
                } catch (Throwable e) {
                    System.err.println("Attempt to connect to " + host + ":"
                            + port + " failed: ");
                    System.err.println(e.getMessage());
                }

                if (s != null) {
                    break;
                }
                Thread.sleep(3000);
            }

            if (s == null) {
                return;
            }
            //System.out.println("Connected to " + host + ":" + port);

            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            out.write(new byte[]{'J', 'C', 'O', 'V'});              // magicword    - 8bytes
            out.write(SOCKET_SAVER_VERSION);                        // version      - 1byte
            out.writeUTF(System.getProperty("user.name"));          // testername   - 1+?bytes
            out.writeUTF(testname); // testname     - 1+?bytes
            out.writeUTF(PropertyFinder.findValue("product", ""));  // productname  - 1+?bytes
            out.writeBoolean(false);                                // static       - 1byte
            out.writeUTF("NIY");
            out.writeInt(count);
            out.writeInt(lastIndex);
            for (int j = 0; j < count; ++j) {
                out.writeInt(dataIdx[j]);
                out.writeLong(dataVal[j]);
            }
            out.close();
            s.close();
        } catch (InterruptedException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
