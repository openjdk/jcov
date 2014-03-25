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
 * @author Dmitry Fazunenko
 * @author Alexey Fedorchenko
 */
public class CollectDetect extends Collect {

    static int last = 0;
    static int prelast = 0;

    /**
     * Used when it's needed to control concurrency: if there is 2 threads both
     * instrumenting (agent mode only) and collecting hits - this class allows
     * to block only that thread which is actually instrumenting and the other
     * can continue to collect hits. Also used to concurrent control of
     * CallerInclude/CallerExclude
     */
    private static class ThreadInfo {

        public static int MAX_STACK = 1000; // not used
        long id; // thread id
        int instLevel; // not-zero instLevel means that this thread entered into instrumentation (agent) or saving code when it shouldn't collect hits
        int expected = 0; // used for CallerInclude/CallerExclude - caller() method is instrumented with setExpected() method
        int clinitValue = 0;
        /*
         * In comparison with expected, contains hash of full signature = object
         * runtime type + name + vmsig
         * */
        int expectedFull; // used for CallerInclude/CallerExclude - caller() method is instrumented with setExpected() method
        ThreadInfo next;

        ThreadInfo(long id) {
            this.id = id;
        }

        private boolean enabled() {
            return instLevel == 0;
        }

        private boolean enabled(int i) {
            return ((expected == i && i != -1) || (expected == 0 && i == -1 && Collect.isInitialized)) && instLevel == 0;
        }

        private boolean enabledFull(int i) {
            return (expectedFull == i) && instLevel == 0;
        }
    }
    static ThreadInfo[] info;
    static ThreadInfo prevInfo;
    static ThreadInfo underConstruction;
    static volatile boolean lock = false;

    static {
        if (info == null) {
            // do initialization
            underConstruction = new ThreadInfo(0L);
            underConstruction.instLevel++;
            if (Thread.currentThread() != null) {
                info = new ThreadInfo[100];
                long id = Thread.currentThread().getId();
                prevInfo = infoForThread(id);
            }
        }
    }

    public static void enableDetectInternal() {
        if (info == null) {
            // do initialization
            underConstruction = new ThreadInfo(0L);
            underConstruction.instLevel++;
            info = new ThreadInfo[100];
            long id = Thread.currentThread().getId();
            prevInfo = infoForThread(id);
        }
    }

    private static ThreadInfo infoForThread(long id) {
        ThreadInfo ti;
        int hash = (int) (id % info.length);
        for (ti = info[hash]; ti != null; ti = ti.next) {
            if (ti.id == id) {
                prevInfo = ti;
                return ti;
            }
        }
        // this is a new thread, create a new ThreadInfo
        synchronized (underConstruction) {
            // set up a place holder to protect us
            underConstruction.id = id;
            underConstruction.next = info[hash];
            info[hash] = prevInfo = underConstruction;

            // we are now protected, safe to create the real one
            ti = new ThreadInfo(id); // the new will trigger a track
            ti.next = underConstruction.next;
            info[hash] = prevInfo = ti;
        }
        return ti;
    }

    public static void hit(int slot) {
        //lock = true;
        long id = Thread.currentThread().getId();
        ThreadInfo ti = prevInfo;

        if (ti.id != id) {
            ti = infoForThread(id);
        }
        if (ti.enabled()) {
            Collect.hit(slot);
        }
    }

    public static void hit(int slot, int hash, int fullHash) {

        long id = Thread.currentThread().getId();
        ThreadInfo ti = prevInfo;

        if (ti.id != id) {
            ti = infoForThread(id);
        }
        if (ti.enabled(hash)) {
            ti.expected = 0;
            Collect.hit(slot);
        }
        if (ti.enabledFull(fullHash)) {
            ti.expectedFull = 0;
            Collect.hit(slot);
        }
    }

    public static void enterInstrumentationCode() {
        if (prevInfo != null) {
            long id = Thread.currentThread().getId();
            ThreadInfo ti = prevInfo;

            if (ti.id != id) {
                ti = infoForThread(id);
            }
            ti.instLevel++;
        }
    }

    public static void setExpected(int hash) {
        if (prevInfo != null) {
            long id = Thread.currentThread().getId();
            ThreadInfo ti = prevInfo;

            if (ti.id != id) {
                ti = infoForThread(id);
            }
            ti.expected = hash;
        }
    }

    public static void enterClinit() {
        if (prevInfo != null) {
            long id = Thread.currentThread().getId();
            ThreadInfo ti = prevInfo;

            if (ti.id != id) {
                ti = infoForThread(id);
            }
            ti.clinitValue = ti.expected;
        }
    }

    public static void leaveClinit() {
        if (prevInfo != null) {
            long id = Thread.currentThread().getId();
            ThreadInfo ti = prevInfo;

            if (ti.id != id) {
                ti = infoForThread(id);
            }
            ti.expected = ti.clinitValue;
        }
    }

    public static void setExpectedFull(int fullHash) {
        if (prevInfo != null) {
            long id = Thread.currentThread().getId();
            ThreadInfo ti = prevInfo;

            if (ti.id != id) {
                ti = infoForThread(id);
            }
            ti.expectedFull = fullHash;
        }
    }

    public static void leaveInstrumentationCode() {
        if (prevInfo != null) {
            long id = Thread.currentThread().getId();
            ThreadInfo ti = prevInfo;

            if (ti.id != id) {
                ti = infoForThread(id);
            }
            ti.instLevel--;
        }
    }
    private static long[] invokeCounts;

    public static void enableInvokeCounts() {
        invokeCounts = new long[MAX_SLOTS];
    }

    public static void invokeHit(int id) {
        invokeCounts[id]++;
    }

    public static boolean wasInvokeHit(int id) {
        return invokeCounts[id] != 0;
    }

    public static long invokeCountFor(int id) {
        return invokeCounts[id];
    }

    public static void setInvokeCountFor(int id, long count) {
        invokeCounts[id] = count;
    }

    static {
        enableInvokeCounts();
    }
}
