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
package com.sun.tdk.jcov.ant;

import com.sun.tdk.jcov.constants.MiscConstants;
import com.sun.tdk.jcov.util.Utils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.Condition;

/**
 *
 * @author Andrey Titov
 * @author Alexey Fedorchenko
 */
public class GrabberManager extends Task implements Condition {

    private int port = MiscConstants.JcovGrabberCommandPort;
    private String host = "localhost";
    private String command;
    private File output;
    private File propertiesFile;

    @Override
    public void execute() throws BuildException {
        com.sun.tdk.jcov.GrabberManager ctrl = new com.sun.tdk.jcov.GrabberManager();
        if (propertiesFile != null) {
            try {
                ctrl.initPortFromFile(propertiesFile.getPath());
            } catch (FileNotFoundException ex) {
                throw new BuildException("Properties file not found", ex);
            } catch (IOException ex) {
                throw new BuildException(ex);
            }
        } else {
            ctrl.setPort(port);
        }
        ctrl.setHost(host);
        if (command != null) {
            try {
                if ("save".equalsIgnoreCase(command)) {
                    ctrl.sendSaveCommand();
                } else if ("kill".equalsIgnoreCase(command)) {
                    ctrl.sendKillCommand();
                } else if ("forcekill".equalsIgnoreCase(command)) {
                    ctrl.sendForceKillCommand();
                } else if ("status".equalsIgnoreCase(command)) {
                    String status = ctrl.sendStatusCommand();
                    if (output != null) {
                        String[] split = status.split(";");
                        Utils.writeLines(output.getPath(), split);
                    }
                } else {
                    throw new BuildException("Command " + command + " is not supported. Only 'save', 'kill' and 'forcekill' are supported.");
                }
            } catch (IOException ex) {
                if ("Connection refused".equals(ex.getMessage())) {
                    throw new BuildException("Connection refused on " + host + ":" + port);
                } else {
                    throw new BuildException(ex);
                }
            }
        }
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public boolean eval() throws BuildException {
        com.sun.tdk.jcov.GrabberManager ctrl = new com.sun.tdk.jcov.GrabberManager(port, host);
        boolean working = false;
        try {
            String gotstatus = ctrl.sendStatusCommand();
            String[] split = gotstatus.split(";");
            working = Boolean.parseBoolean(split[0]);
        } catch (IOException ex) {
            if ("Connection refused".equals(ex.getMessage())) {
                log("Connection refused on " + host + ":" + port);
                return false;
            } else {
                throw new BuildException(ex);
            }
        }
        return working;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setOutput(File output) {
        this.output = output;
    }

    public void setPropertiesFile(File propertiesFile) {
        this.propertiesFile = propertiesFile;
    }
}
