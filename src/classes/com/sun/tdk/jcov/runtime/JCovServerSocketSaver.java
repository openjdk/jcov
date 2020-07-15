/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
class JCovServerSocketSaver implements JCovSaver {

    protected static final int DEFAULT_PORT = 3335;
    protected static final String PORT = "server.port";
    protected int port;

    static int detectPort() {
        String p = null;
        try {
            p = PropertyFinder.findValue(PORT, String.valueOf(DEFAULT_PORT));
            return Integer.parseInt(p);
        } catch (NumberFormatException e) {
            System.err.println("JCovRT: Port parse error (not a number) " + p);
        } catch (Throwable ignore) {
        }
        return DEFAULT_PORT;
    }

    public JCovServerSocketSaver() {
        port = detectPort();
    }

    public JCovServerSocketSaver(int port) {
        this.port = port;
    }

    protected void startServer(Thread server) {
        server.start();
    }

    public void saveResults() {
        //throw new UnsupportedOperationException("Not supported yet.");
    }
}
