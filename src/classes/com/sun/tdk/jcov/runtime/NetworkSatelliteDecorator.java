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

import java.io.*;
import java.net.Socket;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Andrey Titov
 */
public class NetworkSatelliteDecorator implements SaverDecorator {

    private JCovSaver wrapped;
    private int port = 3337;
    private static String host = "localhost";
    private static Lock lock = new ReentrantLock();
    private Thread socketClientThread = null;
    private volatile String name = null;

    public void init(JCovSaver wrap) {
        this.wrapped = wrap;
        listenObserver();
    }

    private void listenObserver(){
        socketClientThread = new Thread(new Runnable() {
            @Override
            public void run() {
                BufferedReader in;
                PrintWriter out;
                try {
                    Socket socket = new Socket(host, port);
                    in = new BufferedReader(new InputStreamReader(
                            socket.getInputStream()));
                    out = new PrintWriter(socket.getOutputStream(), true);

                    while (true) {
                        String line = null;
                        try {
                            line = in.readLine();
                        } catch (Exception e) {
                            lock.lock();
                            try {
                                wrapped.saveResults();
                            }
                            finally {
                                lock.unlock();
                            }
                        }
                        if (line != null) {
                            if (line.startsWith("NAME")) {
                                name = line.substring(4, line.length());
                                System.setProperty("jcov.testname", name);
                                out.println("named " + name);
                                out.flush();
                            } else if (line.startsWith("SAVE")) {
                                name = line.substring(4, line.length());
                                System.setProperty("jcov.testname", name);

                                lock.lock();
                                try {
                                    wrapped.saveResults();
                                }
                                finally {
                                    lock.unlock();
                                }
                                out.println("saved " + name);
                                out.flush();
                                name = null;
                            }
                        }
                    }

                } catch (Exception e) {
                    Logger.getLogger(NetworkSatelliteDecorator.class.getName()).log(Level.SEVERE, "SocketClient: ", e);
                }
            }

        });
        socketClientThread.setDaemon(true);
        socketClientThread.start();

    }

    public void saveResults() {

        while (name == null){
            try {
                Thread.currentThread().sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (name != null) {
            System.setProperty("jcov.testname", name);
            lock.lock();
            try {
                wrapped.saveResults();
            } finally {
                lock.unlock();
            }
            name = null;
        }

    }
}
