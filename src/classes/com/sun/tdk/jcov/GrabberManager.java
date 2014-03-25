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
package com.sun.tdk.jcov;

import com.sun.tdk.jcov.constants.MiscConstants;
import com.sun.tdk.jcov.tools.EnvHandler;
import com.sun.tdk.jcov.tools.JCovCMDTool;
import com.sun.tdk.jcov.tools.OptionDescr;
import com.sun.tdk.jcov.util.Utils;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p> Tool to control Grabber through socket requests </p>
 *
 * @author Andrey Titov
 */
public class GrabberManager extends JCovCMDTool {

    private LinkedList<ServerCommand> commands;
    private int waittime = 5;

    @Override
    protected int run() throws Exception {
        if (commands == null || commands.size() == 0) {
            throw new Exception("No commands specified");
        }
        try {
            Iterator<ServerCommand> comm = commands.iterator();
            while (comm.hasNext()) {
                ServerCommand command = comm.next();
                if (command == COMM_WAIT) {
                    String gotstatus = sendWaitCommand();
                    if (gotstatus == null) {
                        throw new Exception("Server didn't respond");
                    }
                    String[] split = gotstatus.split(";", -1);
                    if (split.length != 4) {
                        throw new Exception("Server sent malformed status: " + gotstatus);
                    }
                    String status = "Server started on " + split[1] + ":" + split[2] + ". Command listener at port " + port + ". Used template " + split[3] + ".";
                    System.out.println(status);
                } else if (command == COMM_STATUS) {
                    String gotstatus = sendStatusCommand();
                    String[] split = gotstatus.split(";", -1);
                    if (split.length != 8) {
                        throw new Exception("Got malformed status from the server: " + gotstatus);
                    }
                    String status = "Server " + (Boolean.parseBoolean(split[0]) ? "is working. Got "
                            + Integer.parseInt(split[1]) + " connections, " + Integer.parseInt(split[2]) + " are alive. "
                            + (Boolean.parseBoolean(split[3]) ? "No unsaved data. " : "Data is not saved. ") : "is not working. ")
                            + "Server was started with options '" + split[4] + "' \n"
                            + "        working directory: " + split[5] + "\n"
                            + "        current template used: " + split[6] + "\n"
                            + "        output file to be created on exit: " + split[7] + "\n";
                    System.out.println("Status: " + status);
                } else if (command == COMM_SAVE) {
                    sendSaveCommand();
                    System.out.println("Save: OK");
                } else if (command == COMM_KILL_FORCE) {
                    sendForceKillCommand();
                    System.out.println("Forced kill: OK");
                } else if (command == COMM_KILL) {
                    sendKillCommand();
                    System.out.println("Kill: OK");
                }
            }
        } catch (UnknownHostException e) {
            throw new Exception("Can't resolve hostname '" + host + "'");
        } catch (IOException e) {
            if ("Connection refused".equals(e.getMessage())) {
                throw new Exception("Server not responding on command port " + port);
            } else {
                throw e;
            }
        }
        return SUCCESS_EXIT_CODE;
    }

    @Override
    protected EnvHandler defineHandler() {
        return new EnvHandler(new OptionDescr[]{
                    DSC_HOSTNAME,
                    DSC_PORT,
                    DSC_FILE,
                    DSC_WAITTIME,
                    COMM_KILL,
                    COMM_KILL_FORCE,
                    COMM_SAVE,
                    COMM_STATUS,
                    COMM_WAIT
                }, this);
    }

    @Override
    protected int handleEnv(EnvHandler opts) throws EnvHandlingException {
        String file = opts.getValue(DSC_FILE);
        Utils.checkFileCanBeNull(file, "properties filename", Utils.CheckOptions.FILE_EXISTS, Utils.CheckOptions.FILE_CANREAD);
        if (file != null) {
            try {
                initPortFromFile(file);
            } catch (IOException ex) {
                throw new EnvHandlingException("Error while reading properties file: ", ex);
            }
        } else {
            setPort(Utils.checkedToInt(opts.getValue(DSC_PORT), "port number"));
        }

        host = opts.getValue(DSC_HOSTNAME);
        Utils.checkHostCanBeNull(host, "grabber host");

        commands = new LinkedList<ServerCommand>();
        if (opts.isSet(COMM_WAIT)) {
            commands.add(COMM_WAIT);
        }
        if (opts.isSet(COMM_STATUS)) {
            commands.add(COMM_STATUS);
        }
        if (opts.isSet(COMM_SAVE)) {
            commands.add(COMM_SAVE);
        }
        if (opts.isSet(COMM_KILL_FORCE)) {
            commands.add(COMM_KILL_FORCE);
        }
        if (opts.isSet(COMM_KILL)) {
            commands.add(COMM_KILL);
        }
        if (commands.size() == 0) {
            throw new EnvHandlingException("Command was not specified");
        }

        if (opts.isSet(DSC_WAITTIME)) {
            waittime = Utils.checkedToInt(opts.getValue(DSC_WAITTIME), "time to wait value");
        }

        return SUCCESS_EXIT_CODE;
    }

    static class ServerCommand extends OptionDescr {

        private int commandCode;

        ServerCommand(String name, String[] aliases, String titile, int values, String usage, int code) {
            super(name, aliases, titile, values, usage);
            this.commandCode = code;
        }

        public int getCommandCode() {
            return commandCode;
        }
    }
    final static OptionDescr DSC_HOSTNAME =
            new OptionDescr("host", new String[]{"hostname"}, "Connection parameters", OptionDescr.VAL_SINGLE,
            "Specify servers host to connect.", "localhost");
    final static OptionDescr DSC_PORT =
            new OptionDescr("command_port", new String[]{"port"}, "", OptionDescr.VAL_SINGLE, "Specify servers command port.",
            Integer.toString(MiscConstants.JcovGrabberCommandPort));
    final static OptionDescr DSC_FILE =
            new OptionDescr("grabber.props", "", OptionDescr.VAL_SINGLE, "Read server properties from a file. Host should be specified explicitly.");
    final static OptionDescr DSC_WAITTIME =
            new OptionDescr("waittime", new String[]{"time", "t"}, "",
            OptionDescr.VAL_SINGLE, "Max time in seconds to wait for Grabber startup. Note that Manager will do 4 attempts to connect the Grabber");
    final static ServerCommand COMM_KILL =
            new ServerCommand("kill", new String[]{"stop"}, "Manage running server",
            OptionDescr.VAL_NONE, "Stop running server saving data and waining for all connections close.", MiscConstants.GRABBER_KILL_COMMAND);
    final static ServerCommand COMM_KILL_FORCE =
            new ServerCommand("fkill", new String[]{"fstop"}, "", OptionDescr.VAL_NONE,
            "Stop running server not saving data and not waining for all connections close.", MiscConstants.GRABBER_FORCE_KILL_COMMAND);
    final static ServerCommand COMM_SAVE =
            new ServerCommand("save", new String[]{"flush"}, "", OptionDescr.VAL_NONE, "Save data to file.", MiscConstants.GRABBER_SAVE_COMMAND);
    final static ServerCommand COMM_STATUS =
            new ServerCommand("status", null, "", OptionDescr.VAL_NONE, "Print server status.", MiscConstants.GRABBER_STATUS_COMMAND);
    final static ServerCommand COMM_WAIT =
            new ServerCommand("wait", null, "", OptionDescr.VAL_NONE, "Wait server for starting.", MiscConstants.GRABBER_WAIT_COMMAND);
    static final Logger logger;

    static {
        Utils.initLogger();
        logger = Logger.getLogger(GrabberManager.class.getName());
    }

    public static void main(String args[]) {
        GrabberManager tool = new GrabberManager();
        try {
            int res = tool.run(args);
            System.exit(res);
        } catch (Exception ex) {
            System.exit(1);
        }
    }
    private int port;
    private String host;

    public GrabberManager() {
        this(MiscConstants.JcovGrabberCommandPort, "localhost");
    }

    public GrabberManager(int port, String host) {
        this.port = port;
        this.host = host;
    }

    private void sendCode(int code) throws IOException {
        Socket socket = null;
        try {
            socket = new Socket(host, port);
            OutputStream out = socket.getOutputStream();
            out.write(code);
            socket.getInputStream().read();
            socket.getInputStream().close();
            out.close();
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    private String recieveCode(int code) throws IOException {
        String data = null;
        Socket socket = null;
        try {
            socket = new Socket(host, port);
            OutputStream out = socket.getOutputStream();
            out.write(code);

            InputStream in = socket.getInputStream();
            BufferedReader inReader = new BufferedReader(new InputStreamReader(in, Charset.defaultCharset()));
            data = inReader.readLine();

            out.close();
            in.close();
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
        return data;
    }

    /**
     * Send KILL command. Port and Host should be both set.
     *
     * @throws IOException
     */
    public void sendKillCommand() throws IOException {
        sendCode(COMM_KILL.getCommandCode());
    }

    /**
     * Send forced KILL command. Port and Host should be both set.
     *
     * @throws IOException
     */
    public void sendForceKillCommand() throws IOException {
        sendCode(COMM_KILL_FORCE.getCommandCode());
    }

    /**
     * Send SAVE command. Port and Host should be both set.
     *
     * @throws IOException
     */
    public void sendSaveCommand() throws IOException {
        sendCode(COMM_SAVE.getCommandCode());
    }

    /**
     * Send STATUS command and recieve responce. Port and Host should be both
     * set.
     *
     * @throws IOException
     */
    public String sendStatusCommand() throws IOException {
        return recieveCode(COMM_STATUS.getCommandCode());
    }

    /**
     * Send WAIT command and recieve responce. Port and Host should be both set.
     *
     * @throws IOException
     */
    public String sendWaitCommand() throws IOException {
        String ret = null;
        for (int i = 0; i < 4; ++i) {
            try {
                ret = recieveCode(COMM_WAIT.getCommandCode());
                String[] split = ret.split(";");
                if (Boolean.parseBoolean(split[0])) {
                    break;
                }
            } catch (IOException e) {
                try {
                    Thread.sleep(waittime * 1000);
                } catch (InterruptedException ex) {
                }
            }
        }
        return ret;
    }

    @Override
    protected String getDescr() {
        return "control commands to the Grabber server";
    }

    @Override
    protected String usageString() {
        return "java -jar jcov.jar GrabberManager [-option value]";
    }

    @Override
    protected String exampleString() {
        return "java -jar jcov.jar GrabberManager -port 3336 -status";
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int initPortFromFile(String file) throws IOException {
        Properties ps = new Properties();
        InputStream in = null;
        in = new FileInputStream(file);
        ps.load(in);
        in.close();

        String portStr = ps.getProperty(Grabber.COMMAND_PORT_PORPERTY);
        if (portStr == null) {
            logger.log(Level.SEVERE, "Command Listeners port is not set in properties file '{0}'. Cannot work.", file);
            return 1;
        }

        port = 0;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException ex) {
            logger.log(Level.SEVERE, "Malformed port number '{0}' in properties file '{1}'. Cannot work.", new Object[]{portStr, file});
            return 1;
        }

        if (port == 0) {
            String runLine = ps.getProperty(Grabber.RUN_LINE_PROPERTY);
            if (runLine != null) {
                logger.log(Level.SEVERE, "Command listener is not running on server (port = 0). Server was run with arguments '{0}'.", runLine);
            } else {
                logger.log(Level.SEVERE, "Command listener is not running on server (port = 0). Servers run line is unknown (not set in properties file).");
            }
            return 1;
        }

        checkVersion(ps.getProperty(Grabber.SERVER_VERSION_PROPERTY));
        return 0;
    }

    private void checkVersion(String version) {
    }
}
