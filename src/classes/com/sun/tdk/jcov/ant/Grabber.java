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
import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 *
 * @author Andrey Titov
 * @author Alexey Fedorchenko
 */
public class Grabber extends Task {

    private int port = MiscConstants.JcovPortNumber;
    private File template;
    private File output;
    private int count = 0;
    private boolean saveonrecieve = false;
    private boolean startCommandListener = true;
    private int commandport = MiscConstants.JcovGrabberCommandPort;
    private File logFile;
    private File propFile;
    private String outTestList;
    private boolean mergeByTestNames = false;

    @Override
    public void execute() throws BuildException {
        if (template == null || !template.exists() || !template.isFile()) {
            throw new BuildException("Incorrect template: " + template);
        }

        if (output == null) {
            output = new File(getProject().getBaseDir(), "result.xml");
        }

        com.sun.tdk.jcov.Grabber grabber = new com.sun.tdk.jcov.Grabber();

        if (logFile != null) {
            try {
                Logger logger = LogManager.getLogManager().getLogger("");
                Handler[] handlers = logger.getHandlers();
                for (int i = 0; i < handlers.length; i++) {
                    logger.removeHandler(handlers[i]);
                }
                FileHandler fh = new FileHandler(logFile.getPath());
                logger.addHandler(fh);
                logger.setLevel(Level.FINE);
                LogManager.getLogManager().addLogger(logger);

            } catch (Exception ex) {
                throw new BuildException(ex);
            }

        } else {
            LogManager.getLogManager().getLogger("").setLevel(Level.OFF);
        }

        try {

            grabber.setPort(port);
            grabber.setOutputFilename(output.getPath());
            grabber.setTemplate(template.getPath());
            grabber.setMaxCount(count);
            grabber.setSaveOnReceive(saveonrecieve);
            grabber.setCommandPort(commandport);
            grabber.setOutTestList(outTestList);
            grabber.setMergeByTestNames(mergeByTestNames);
            if (startCommandListener) {
                grabber.start(true);
            } else {
                grabber.start(false);
            }

            // setting run command after starting the server to use real ports
            grabber.setRunCommand(String.format("ANT: grabber port: %d, command port: %d, template: %s, output: %s, save on: %s, max connections: %s",
                    grabber.getServerPort(), grabber.getCommandListenerPort(), template, output, (saveonrecieve ? "recieve" : "exit"),
                    (count > 0 ? Integer.toString(count) : "unlimited")));

        } catch (BindException ex) {
            throw new BuildException("Can't bind to specified port", ex);
        } catch (IOException ex) {
            throw new BuildException("Can't create server", ex);
        }

        if (propFile != null) {
            try {
                grabber.writePropfile(propFile.getPath());
            } catch (IOException ex) {
                log(ex, 2);
            }
        }

        log("Server started at port " + port + " listening commands on " + commandport);
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setCommandport(int commandport) {
        this.commandport = commandport;
    }

    public void setSaveonrecieve(boolean saveonrecieve) {
        this.saveonrecieve = saveonrecieve;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setOutput(File output) {
        this.output = output;
    }

    public void setTemplate(File template) {
        this.template = template;
    }

    public void setLogFile(File logFile) {
        this.logFile = logFile;
    }

    public void setPropertiesFile(File propFile) {
        this.propFile = propFile;
    }

    public void setOutTestList(String outTestList) {
        this.outTestList = outTestList;
    }

    public void setMergeByTestNames(boolean mergeByTestNames) {
        this.mergeByTestNames = mergeByTestNames;
    }

    public void setStartCommandListener(boolean startCommandListener) {
        this.startCommandListener = startCommandListener;
    }
}
