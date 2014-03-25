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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author Sergey Borodin
 */
public class JCovSEServerSocketSaver extends JCovServerSocketSaver {

    public JCovSEServerSocketSaver() {
        super();
        SEServer server = new SEServer();
        server.setDaemon(true);
        startServer(server);
    }

    public JCovSEServerSocketSaver(int port) {
        super(port);
        SEServer server = new SEServer();
        server.setDaemon(true);
        startServer(server);
    }

    class SEServer extends Thread {

        public void run() {
            try {
                ServerSocket ssock = new ServerSocket(port);
                System.out.println("JCov server saver started on port: " + port);

                Socket sock = ssock.accept();
                System.out.println("Remote request accepted");

                ByteArrayOutputStream bo = new ByteArrayOutputStream(Collect.MAX_SLOTS * 4);
                DataOutputStream os = new DataOutputStream(bo);
                for (int j = 0; j < Collect.counts().length; j++) {
                    os.writeLong(Collect.counts()[j]);
                }
                OutputStream oss = sock.getOutputStream();
                oss.write(bo.toByteArray());
                oss.close();
                os.close();
                sock.close();
                ssock.close();

            } catch (Throwable t) {
                System.err.println("Problem during server saver run: " + t.getMessage());
                //throw new Error("Unexpected error during saving: " + e.getMessage());
            }
        }
    }
}
