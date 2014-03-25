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

import com.sun.tdk.jcov.instrument.DataRoot;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Andrey Titov
 */
public class AgentSocketSaver extends JCovSocketSaver {

    private DataRoot root;
    private String file;

    public AgentSocketSaver(DataRoot root, String file) {
        this.root = root;
        this.file = file;
        if (this.file == null) {
            this.file = "result.xml";
        }
    }

    public AgentSocketSaver(DataRoot root, String file, String host, int port) {
        super(host, port);
        this.root = root;
        this.file = file;
    }

    @Override
    public synchronized void saveResults() {
        try {
            if (host == null) {
                host = detectHost();
            }
            if (port == -1) {
                port = detectPort();
            }
            String testname = PropertyFinder.findValue("testname", null);
            if (testname == null) {
                testname = PropertyFinder.findValue("file", file);
            } else {
                if ("<jcov.ignore>".equals(testname)) {
                    return; // ignoring this test data
                }
            }

            Socket s = null;
            for (int i = 0; i < 3; ++i) {
                try {
                    s = new Socket(host, port);
                    break;
                } catch (UnknownHostException e) {
                    System.err.println("JCovRT: Can't resolve hostname " + host
                            + " - unknown host. Exiting. ");
                    return;
                } catch (IOException e) {
                    System.err.println("JCovRT: Attempt to connect to " + host + ":"
                            + port + " failed: ");
                    System.err.println(e.getMessage());
                }
                Thread.sleep(3000);
            }
            if (s == null) {
                return;
            }

            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
            out.writeBytes("JCOV");                                 // magicword    - 8bytes
            out.write(SOCKET_SAVER_VERSION);                        // version      - 1byte
            out.writeUTF(System.getProperty("user.name"));          // testername   - 1+?bytes
            out.writeUTF(testname); // testname     - 1+?bytes
            out.writeUTF(PropertyFinder.findValue("product", ""));  // productname  - 1+?bytes
            out.writeBoolean(root.getParams().isDynamicCollect());  // dynamic      - 1byte
            root.writeObject(out);
            out.close();
            s.close();
        } catch (InterruptedException ex) {
        } catch (IOException ex) {
            Logger.getLogger(FileSaver.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
