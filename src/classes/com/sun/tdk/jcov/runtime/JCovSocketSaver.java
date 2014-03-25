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

/**
 *
 * @author Sergey Borodin
 */
public abstract class JCovSocketSaver implements JCovSaver {

    public static final int SOCKET_SAVER_VERSION = 0;
    protected static String defaultHost = "localhost";
    protected static int defaultPort = 3334;
    protected static final String HOST = "host";
    protected static final String PORT = "port";
    protected String host;
    protected int port = -1;

    static void setDefaultHost(String host) {
        defaultHost = host;
    }

    static void setDefaultPort(int port) {
        defaultPort = port;
    }

    static String detectHost() {
        return PropertyFinder.findValue(HOST, defaultHost);
    }

    static int detectPort() {
        String p = null;
        try {
            p = PropertyFinder.findValue(PORT, "" + defaultPort);
            return Integer.parseInt(p);
        } catch (NumberFormatException e) {
            System.err.println("JCovRT: Port parse error (not a number) " + p);
        } catch (Throwable ignore) {
        }
        return defaultPort;
    }

    public JCovSocketSaver() {
    }

    public JCovSocketSaver(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public abstract void saveResults();
}
