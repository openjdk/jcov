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
package jcov;

import com.sun.javatest.Harness;
import com.sun.javatest.Parameters;
import com.sun.javatest.TestResult;
import com.sun.javatest.TestResult.Section;
import java.io.DataOutputStream;
import java.net.Socket;

/**
 *
 * @author Andrey Titov
 */
public class JTObserver implements Harness.Observer {

    private static int port = 3337;
    private static String host = "localhost";
    public static final int SAVE = 0;
    public static final int NAME = 1;
    public static final int EXIT = 2;
    public static final int WAIT = 3; // should be run in same thread (Thread.run())

    public JTObserver() {
    }

    public void startingTestRun(Parameters prmtrs) {
        log("Starting test run");
    }

    public void startingTest(TestResult tr) {
        new Signal(tr.getTestName(), NAME).start();
        tr.addObserver(new TestResult.Observer() {
            public void createdSection(TestResult tr, Section sctn) {
            }

            public void completedSection(TestResult tr, Section sctn) {
            }

            public void createdOutput(TestResult tr, Section sctn, String string) {
            }

            public void completedOutput(TestResult tr, Section sctn, String string) {
            }

            public void updatedOutput(TestResult tr, Section sctn, String string, int i, int i1, String string1) {
            }

            public void updatedProperty(TestResult tr, String string, String string1) {
            }

            public void completed(TestResult tr) {
                new Signal(tr.getTestName(), SAVE).start();
            }
        });
        log("Starting test " + tr.getTestName());
    }

    public void finishedTest(TestResult tr) {
        //new Signal(tr.getTestName(), SAVE).start();
        log("Finished test " + tr.getTestName());
    }

    public void stoppingTestRun() {
        log("Stopping testrun");
    }

    public void finishedTesting() {
        Signal sig = new Signal(null, EXIT);
        sig.setDaemon(true);
        sig.start();
        log("Finished testing");
    }

    public void finishedTestRun(boolean bln) {
        log("Finished testrun");
    }

    public void error(String string) {
        log("error");
    }

    public static class Signal extends Thread {

        private String message;
        private int command;

        public Signal(String name, int command) {
            this.message = name;
            this.command = command;
        }

        @Override
        public void run() {
            for (int i = 0; i < 100; ++i) {
                try {
                    Socket s = new Socket(host, port);
                    DataOutputStream out = new DataOutputStream(s.getOutputStream());
                    out.writeBytes("JCOV");
                    out.write(command);
                    if (message != null) {
                        out.writeUTF(message);
                    }
                    s.getInputStream().read();
                    s.close();
                    log("DONE! " + message + " " + command);
                    break;
                } catch (Exception ex) {
                    try {
                        log("waiting " + message + " " + command);
                        Thread.sleep(100);
                    } catch (InterruptedException ex1) {
                    }
                }
            }
        }
    }

    private static void log(String str) {
//        System.out.println(str);
    }
}
