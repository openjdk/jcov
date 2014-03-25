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

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Andrey Titov
 */
public class NetworkSatelliteDecorator implements SaverDecorator {

    private ServerSocket ss;
    private JCovSaver wrapped;
    private int port1 = 3337;
    private static Lock lock = new ReentrantLock();

    static {
        Collect.saveAtShutdownEnabled = false;
    }

    public NetworkSatelliteDecorator() {
        System.setProperty("jcov.autosave", "false");
    }

    public void init(JCovSaver wrap) {
        this.wrapped = wrap;

        ThreadGroup tg;
        if (Thread.currentThread().getThreadGroup() != null && Thread.currentThread().getThreadGroup().getParent() != null) {
            tg = new ThreadGroup(Thread.currentThread().getThreadGroup().getParent(), "JCov");
        } else {
            tg = new ThreadGroup("JCov");
        }
        tg.setDaemon(true);

        Thread t = new Thread(tg, "JCovSatellite") {
            @Override
            public void run() {
                try {
                    ss = new ServerSocket(port1);
                    outer:
                    while (true) {
                        Socket sock = null;
                        try {
                            sock = ss.accept();

                            DataInputStream in = new DataInputStream(sock.getInputStream());
                            byte buff[] = new byte[4];
                            for (int i = 0; i < buff.length; ++i) {
                                buff[i] = in.readByte();
                            }

                            if (!new String(buff, Charset.defaultCharset()).equals("JCOV")) {
                                continue;
                            }

                            int code = in.read();
                            switch (code) {
                                case 0: // SAVE
                                    String name = in.readUTF();
                                    System.setProperty("jcov.testname", name);
                                    lock.lock();
                                    try {
                                        wrapped.saveResults();
                                    } finally {
                                        lock.unlock();
                                    }
                                    sock.getOutputStream().write(0);
                                    break;
                                case 1: // NAME
                                    name = in.readUTF();
                                    System.setProperty("jcov.testname", name);
                                    sock.getOutputStream().write(0);
                                    break;
                                case 2: // EXIT
                                    sock.getOutputStream().write(0);
                                    break outer;
                                default:
                                    sock.getOutputStream().write(0);
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                if (sock != null) {
                                    sock.close();
                                }
                            } catch (Exception e) {
                            }
                        }
                    }
                } catch (IOException ex) {
                    Logger.getLogger(NetworkSatelliteDecorator.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    try {
                        ss.close();
                    } catch (Throwable e) {
                    }
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }

    public void saveResults() {
        if (!lock.tryLock()) {
            lock.lock();
        }
        lock.unlock();
        try {
            ss.close();
        } catch (Exception ex) {
        }
//        wrapped.saveResults();
    }
}
