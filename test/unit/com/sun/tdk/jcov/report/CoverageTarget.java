/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.jcov.report;

public class CoverageTarget {

    public static void main(String[] args) {
        CoverageTarget target = new CoverageTarget();

        target.testBranching(5);
        target.testLoop(3);
        target.testSwitch("c");
        target.testExceptionHandling(true);
        target.testTryWithResources();
        target.useLambda(() -> "Lambda result");
        System.out.println("Inner: " + target.new InnerClass().compute());
    }

    public int testBranching(int x) {
        if (x > 10) {
            return x * 2;
        } else if (x > 5) {
            return x + 10;
        } else {
            return x - 1;
        }
    }

    public int testLoop(int n) {
        int result = 0;
        for (int i = 0; i < n; i++) {
            if (i % 2 == 0) {
                result += i;
            } else {
                result -= i;
            }
        }
        int i = 0;
        while (i < 2) {
            result += i;
            i++;
        }
        return result;
    }

    public String testSwitch(String input) {
        switch (input) {
            case "a":
                return "A";
            case "b":
                return "B";
            case "c":
                return "C";
            default:
                return "Unknown";
        }
    }

    public String testExceptionHandling(boolean shouldThrow) {
        try {
            if (shouldThrow) {
                throw new IllegalArgumentException("Test exception");
            }
            return "No exception";
        } catch (IllegalArgumentException e) {
            return "Caught: " + e.getMessage();
        } finally {
            System.out.println("In finally block");
        }
    }

    public String testTryWithResources() {
        try (DummyResource res = new DummyResource()) {
            res.use();
            return "Resource used";
        } catch (Exception e) {
            return "Exception during resource use";
        }
    }

    public String useLambda(Supplier<String> supplier) {
        return supplier.get();
    }

    public class InnerClass {
        public int compute() {
            return 42;
        }
    }

    static class DummyResource implements AutoCloseable {
        public void use() {
            System.out.println("Using resource");
        }

        @Override
        public void close() {
            System.out.println("Closing resource");
        }
    }

    @FunctionalInterface
    public interface Supplier<T> {
        T get();
    }
}
