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
package com.sun.tdk.jcov.tools;

import com.sun.tdk.jcov.runtime.PropertyFinder;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 *
 * @author Andrey Titov
 */
public class LoggingFormatter extends Formatter {

    public static boolean printStackTrace = Boolean.parseBoolean(PropertyFinder.findValue("stacktrace", "false"));

    @Override
    public String format(LogRecord record) {
        if (record.getThrown() != null) {
            StringBuilder ret = new StringBuilder(String.format("%-8s: %s", record.getLevel().getLocalizedName(), record.getMessage() != null ? formatMessage(record) + "\nException details: " : ""));

            if (printStackTrace) {
                StringWriter sw = new StringWriter();
                record.getThrown().printStackTrace(new PrintWriter(sw));
                ret.append(sw.toString()).append("\n");
            } else {
                if (record.getThrown() instanceof NullPointerException) {
                    StackTraceElement[] stackTrace = record.getThrown().getStackTrace();
                    if (stackTrace.length > 0) {
                        ret.append("NPE in ").append(stackTrace[0]).append("\n");
                    } else {
                        ret.append("NPE in unknown place");
                        StringWriter sw = new StringWriter();
                        record.getThrown().printStackTrace(new PrintWriter(sw));
                        ret.append(sw.toString()).append("\n");
                    }
                } else {
                    ret.append(record.getThrown().getMessage()).append("\n");
                }
            }

            return ret.toString();
        } else {
            return String.format("%-8s: %s\n", record.getLevel().getLocalizedName(), formatMessage(record));
        }
    }
}
