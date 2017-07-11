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
import com.sun.tdk.jcov.data.FileFormatException;
import com.sun.tdk.jcov.data.Result;
import com.sun.tdk.jcov.instrument.*;
import com.sun.tdk.jcov.instrument.DataRoot.CompatibilityCheckResult;
import com.sun.tdk.jcov.instrument.InstrumentationOptions.MERGE;
import com.sun.tdk.jcov.runtime.Collect;
import com.sun.tdk.jcov.runtime.FileSaver;
import com.sun.tdk.jcov.runtime.PropertyFinder;
import com.sun.tdk.jcov.tools.EnvHandler;
import com.sun.tdk.jcov.tools.JCovCMDTool;
import com.sun.tdk.jcov.tools.JcovVersion;
import com.sun.tdk.jcov.tools.LoggingFormatter;
import com.sun.tdk.jcov.tools.OptionDescr;
import com.sun.tdk.jcov.util.RuntimeUtils;
import com.sun.tdk.jcov.util.Utils;
import com.sun.tdk.jcov.util.Utils.Pair;
import org.objectweb.asm.Opcodes;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class Client serves to collect data from remote client. isWorking() should
 * return true until all data is processed by Server.
 *
 * @author Dmitry Fazunenko
 * @author Alexey Fedorchenko
 *
 * @see #isWorking()
 */
class Client extends Thread {

    private final Socket socket;                    // socket
    private final Server server;                    // server
    private final int clientNumber;                 // number of this client
    private boolean working = true;                 // set to false when data is read
    public static int unknownTestNumber = 0;       // number of unknown tests got from clients
    // runtime data - got from client side
    private String testName;                        // test name
    private String testerName;                      // tester name
    private String productName;                     // product name
    private int slotNumber;                         // slots number (in static mode)
    private static final int MAX_SLOTS = Collect.MAX_SLOTS;
    public static final String UNKNOWN = "<unknown>";

    /**
     * Constructor for Client class Sets "Client{clientNumber}Thread" as the
     * name of the thread
     *
     * @param server Server object this client is connected to
     * @param socket Remote clients socket
     * @param clientNumber ID of this client
     */
    public Client(Server server, Socket socket, int clientNumber) {
        super();

        setName("Client" + clientNumber + "Thread");
        setDaemon(false);
        this.socket = socket;
        this.server = server;
        this.clientNumber = clientNumber;
    }

    /**
     * <p> Main method. Use start() to launch in new thread. </p> <p> Reads data
     * as long[] or as DataRoot and forwards it to the Server </p> <p> Sets
     * accepted test name, tester name and product name. Use getTestName(),
     * getTesterName() and getProductName() to read them. </p>
     */
    @Override
    public void run() {
        executeClient();
    }

    public void executeClient() {
        DataInputStream IN = null;
        byte buff[];
        boolean legacy = false, dynamic = false;
        int version;

        long reserve = 0L;
        try {
            // synchronized (Server.STOP_THE_WORLD_LOCK) {
            //     server.checkFreeMemory(30000000L); // 30mb for a client seems to be OK
               /*     while (server.isDumping()) { // locks the thread when is dumping
             Server.STOP_THE_WORLD_LOCK.wait();
             }*/
            IN = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            buff = new byte[4];
            for (int i = 0; i < buff.length; ++i) {
                buff[i] = IN.readByte();
            }

            if (!new String(buff, "UTF-8").equals("JCOV")) {
                reserve = 10000000L; // 10mb
                legacy = true;
            } else {
                version = IN.read();
                testerName = IN.readUTF();
                testName = IN.readUTF();
                productName = IN.readUTF();
                if ("".equals(testName)) {
                    testName = testName + " " + testerName + " " + productName + "_test" + ++unknownTestNumber;
                }
                Grabber.logger.log(Level.FINE, "Got test data: name {0}, tester {1}, product {2}", new Object[]{testName, testerName, productName});
                dynamic = IN.readBoolean();
                if (dynamic) {
                    reserve = 50000000L; // 50mb
                } else {
                    reserve = Collect.SLOTS * 10;
                }
            }
            //server.increaseReserved(reserve);
            //} // release synchronized (Server.STOP_THE_WORLD_LOCK)

            if (legacy) {
                Grabber.logger.log(Level.FINE, "Header missmatch from client N{0}: '{1}'. Reading as legacy", new Object[]{clientNumber + "", new String(buff, "UTF-8")});
                // reading in old format - not to forget include first 8 bytes
                // in old format we can't get less or more than 1bill longs - so reading them all in long[1000000]
                testerName = UNKNOWN;
                testName = "test" + ++unknownTestNumber;
                productName = UNKNOWN;
                long ids[] = new long[1000000];
                // copying the first long to the result array
                for (int j = 0; j < 4; ++j) {
                    ids[0] += ((long) buff[j] & 0xffL) << (8 * j);
                }
                int i = 1;
                try {
                    for (; i < 1000000; ++i) {
                        ids[i] = IN.readLong();
                    }
                } catch (EOFException e) {
                    Grabber.logger.log(Level.SEVERE, "Got incorrect number of longs from legacy-format client N{0}: {1}, expected 1 000 000", new Object[]{clientNumber + "", i});
                    return;
                }
                IN.close();
                IN = null;

                Grabber.logger.log(Level.FINER, "Got legacy-format static data from client N{0}", clientNumber + "");
                saveResults(ids);

                return;
            } else {
                if (dynamic) {
                    DataRoot root = new DataRoot(IN);
                    slotNumber = root.getCount();
                    IN.close();
                    IN = null;
                    saveResults(root);
                } else {
                    String templateHash = IN.readUTF();
                    slotNumber = IN.readInt();
                    int lastIndex = IN.readInt();
                    long ids[] = new long[lastIndex + 1];
                    int i = 0;
                    try {
                        for (; i < slotNumber; ++i) {
                            int index = IN.readInt();
                            ids[index] = IN.readLong();
                        }
                    } catch (EOFException e) {
                        Grabber.logger.log(Level.SEVERE, "Got incorrect number of longs from static client N{0}: found {1}, expected {2}", new Object[]{clientNumber + "", i, slotNumber});
                    } catch (IOException ioe) {
                        Grabber.logger.log(Level.WARNING, "Got incorrect number of longs from static client N{0}: found {1}, expected {2}", new Object[]{clientNumber + "", i, slotNumber});
                    }
                    IN.close();
                    IN = null;
                    socket.close();

                    Grabber.logger.log(Level.FINER, "Got new-format static data from client N{0}", clientNumber + "");
                    saveResults(ids);
                }
            }
        } catch (Exception e) {
            Grabber.logger.log(Level.SEVERE, "e =" + e);
            Grabber.logger.log(Level.SEVERE, "Error while receiving data from client N" + clientNumber, e);
            //            e.printStackTrace();
        } finally {
            server.decreaseReserved(reserve);
            // java7+try-with-resources rules
            if (IN != null) {
                try {
                    IN.close();
                } catch (IOException ex) {
                }
            }
            if (!socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException ex) {
                }
            }
            --server.aliveClients;
            working = false; // all is done
            Grabber.logger.log(Level.FINE, "Client N{0} done", clientNumber + "");
        }
    }

    /**
     * Handle got data. Translates data to the Server.
     *
     * @param ids Data in long[] form transferred from client
     */
    private void saveResults(long[] ids) {
        server.handleData(ids, this);
    }

    /**
     * Handle got data. Translates data to the Server.
     *
     * @param root Data in DataRoot transferred from client
     */
    private void saveResults(DataRoot root) {
        server.handleData(root, this);
    }

    /**
     * Returns client address
     *
     * @return Client address formatted as 'host:port'
     * @see #Client(com.sun.tdk.jcov.Server, java.net.Socket, int)
     */
    public String getClientAddress() {
        return socket.getInetAddress().getHostName() + ":" + socket.getLocalPort();
    }

    /**
     * Returns ID assigned to this Client
     *
     * @return ID assigned to this Client
     * @see #Client(com.sun.tdk.jcov.Server, java.net.Socket, int)
     */
    public int getClientNumber() {
        return clientNumber;
    }

    /**
     * Answers whether Client have finished transactions.
     *
     * @return false when all transactions are finished (data received and
     * translated to Server or Exception occurred)
     */
    public boolean isWorking() {
        return working;
    }

    /**
     * @return test name accepted from client side or generated test name
     */
    public String getTestName() {
        return testName;
    }

    /**
     * @return tester name accepted from client side or &lt;unknown&gt;
     */
    public String getTesterName() {
        return testerName;
    }

    /**
     * @return product name accepted from client side or &lt;unknown&gt;
     */
    public String getProductName() {
        return productName;
    }
} // ############# Client

/**
 * Server class manages incoming client connections and handles coverage data
 * coming from them.
 *
 * @see #handleData(long[], com.sun.tdk.jcov.Client)
 */
class Server extends Thread {
    // config data

    private String fileName;                // output file
    private final String templateName;      // template file
    private final int port;                 // port to listen for client connections
    private int saveCount;                  // maximum connections to manage. Infinite if 0
    private boolean saveAtReceive;          // true = save data when it's coming from Client
    private final String hostName;          // host to connect in 'once' mode
    private final boolean once;             // enables 'once' mode
    private String outTestList;             // output testlist file
    private boolean genscale;               // allows to generate scales without outtestlist
    private String saveBadData;             // path to save bad agent data to
    // status data
    private boolean working = true;         // thread is working (not dead). Set to false at kill() method and when exiting main loop.
    private boolean listening = true;       // thread is listening for new connections. Set to false when exiting main loop.
    private boolean dataSaved = false;      // no notsaved data. Set to true when accepting new data, set to false when writing data.
    private boolean started = false;        // set to true when all initialization is done.
    // runtime data
    private ServerSocket ss = null;         // socket listener
    private long[] data = null;             // data got from clients. Not used in SaveAtReceive mode
    private DataRoot dataRoot = null;       // data got from clients. Not used in SaveAtReceive mode
    private int totalConnections;           // connections occurred
    int aliveClients = 0;                   // increased on client.start(), decreased in client
    private LinkedList<String> tests;       // accepted testnames
    static int MAX_TIMEOUT = 90000;         // maximum time to wait for Client working
    static boolean showMemoryChecks = false; // show memory checks
    final static Runtime rt = Runtime.getRuntime(); // runtime
    private long reservedMemory = 0;        // memory reserved by alive clients - not used yet
    private int dumpCount = 0;              // number of dumped files
    private boolean dumping = false;        // dumping proccess is going on
    static final Object STOP_THE_WORLD_LOCK = new Object(); // LOCK
    private boolean veryLowMemoryRun = false; // very low memory mode - minimal memory lock, no merge
    private boolean lowMemoryRun = false; // low memory mode - minimal memory lock, merge
    private boolean normalMemoryRun = false; // normal memory mode - 45% memory lock, merge
    private boolean mergeByTestNames = false;// generate scales based on test names (test name identifies test)
    private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    /**
     * <p> Constructor for Server class. Warning: ServerSocket will be opened in
     * constructor so port will be binded. </p> <p> Sets "ServerThread" as the
     * name of the thread. </p>
     *
     * @param port port to listen for client connections
     * @param once enables 'once' mode - Server connects to hname:port and
     * receives data from there
     * @param template template file
     * @param host host to connect in 'once' mode
     * @param saveAtReceive true = save data when it's coming from Client
     * @param genscale allows to generate scales without outtestlist
     * @throws BindException
     * @throws IOException
     */
    public Server(int port, boolean once, String template, String host, boolean saveAtRecieve, boolean genscale, boolean mergeByTestNames) throws BindException, IOException {
        super();

        setName("ServerThread");
        setDaemon(false);

        totalConnections = 0;

        this.once = once;
        this.templateName = template;
        this.hostName = host;
        this.saveAtReceive = saveAtRecieve;
        this.genscale = genscale;
        this.mergeByTestNames = mergeByTestNames;


        if (this.genscale) {
            tests = new LinkedList<String>();
        }

        if (this.once) {
            this.port = port;
            if (this.port == 0) {
                throw new IllegalArgumentException("Port should be specified in 'once' mode");
            }
        } else {
            try {
                initDataRoot();

                // checking available memory
                if (genscale) {
                    rt.gc();
                    if (rt.totalMemory() > rt.maxMemory() / 1.2) {
                        Grabber.logger.log(Level.WARNING, "Server started with very low memory: it''s recomended at least {0}M max memory for this template. Server will not write data dumps on low memory and can fail in OutOfMemoryError.", rt.totalMemory() * 3 / 1000000);
                        veryLowMemoryRun = true;
                    } else if (rt.totalMemory() > rt.maxMemory() / 2.5) {
                        if (showMemoryChecks) {
                            Grabber.logger.log(Level.WARNING, "Server started with low memory: it''s recomended at least {0}M max memory for this template. Server will write data dumps on low memory and will try to merge them on exit.", rt.totalMemory() * 2.5 / 1000000);
                        }
                        lowMemoryRun = true;
                    } else {
                        if (showMemoryChecks) {
                            Grabber.logger.log(Level.INFO, "Server will write data dumps on 45% free memory and will try to merge them on exit.");
                        }
                        normalMemoryRun = true;
                    }
                }
            } catch (FileFormatException ex) {
                throw new IllegalArgumentException("Bad template: " + templateName, ex);
            }
            this.ss = new ServerSocket(port);
            this.port = ss.getLocalPort(); // updating port number - when port == 0 ServerSocket will take any free port
        }
    }

    /**
     * <p> Constructor for Server class. Warning: ServerSocket will be opened in
     * constructor so port will be binded. </p> <p> Sets "ServerThread" as the
     * name of the thread </p>
     *
     * @param port port to listen for client connections
     * @param once enables 'once' mode - Server connects to hname:port and
     * receives data from there
     * @param template template file
     * @param output output file
     * @param outTestList file to write testlist to
     * @param host host to connect in 'once' mode
     * @param maxCount maximum connections to manage. Infinite if 0
     * @param saveAtReceive true = save data when it's coming from Client
     * @param genscale allows to generate scales without outtestlist
     * @throws BindException
     * @throws IOException
     */
    Server(int port, boolean once, String template, String output, String outTestList, String host, int maxCount,
            boolean saveAtReceive, boolean genscale, boolean mergeByTestNames) throws BindException, IOException {
        this(port, once, template, host, saveAtReceive, genscale || outTestList != null, mergeByTestNames);

        this.fileName = output;
        this.outTestList = outTestList;
        this.saveCount = maxCount;
    }

    @Override
    public synchronized void start() {
        super.start();
    }

    /**
     * Main method. Use start() to launch in new thread. 1. In 'once' mode
     * Server opens Socket to hostName:port, creates a Client object for this
     * Socket and receives data from it (all in one thread). Then it dies. 2. In
     * other case Server starts accepting client connections by ServerSocket.
     * For every connected client Server creates Client object and starts it in
     * new thread waiting for next connection. Client receives data and
     * translates it back to Server. Server will stop when maxCount connections
     * will be received or at system shutdown (eg Ctrl-C) or when kill() method
     * will be called (eg by CommandListener)
     */
    @Override
    public void run() {
        if (once) { // 'client' mode - single data connection to hostName:port
            started = true;
            InetAddress adr;
            try {
                if (hostName == null) {
                    adr = InetAddress.getLocalHost();
                } else {
                    adr = InetAddress.getByName(hostName);
                }

                Socket s = new Socket(adr, port);

                Grabber.logger.log(Level.INFO, "Connection established with {0}:{1}", new Object[]{adr, port});
                ++aliveClients;
                new Client(this, s, 0).executeClient(); // running client in local thread
                saveData();
                Grabber.logger.log(Level.FINE, "Server stopped");
            } catch (Exception e) {
                Grabber.logger.log(Level.SEVERE, "Jcov grabber failed", e);
            } finally {
                working = false;
            }
        } else { // server mode - receive data saveCount times (or any if 0)
            started = true;
            try {
                int n = 0;
                do {
                    Grabber.logger.log(Level.FINE, "Waiting for new connection");

                    Socket s = ss.accept();
                    Client c = new Client(this, s, ++n);
                    Grabber.logger.log(Level.INFO, "Connection N{0} received from {1}:{2}",
                            new Object[]{n + "", s.getInetAddress(), s.getLocalPort() + ""}); // s.getLocalPort() + "" - to avoid int formatting
                    Grabber.logger.log(Level.FINE, "Alive connections: {0}; total connections: {1}", new Object[]{aliveClients + 1 + "", totalConnections + 1 + ""});
                    ++aliveClients;
                    executor.execute(c);
                    //c.start();

                    ++totalConnections;
                } while (--saveCount != 0 && listening); // kill() sets listening to false to break the loop
                if (saveCount == 0 && listening) { // normal exit
                    kill(false); // ServerSocket will not produce exception on ss.close() as accept() is not waiting
                }
            } catch (Throwable e) {
                if (listening) { // loop was broken without kill() -> real exception. When kill() is called - ServerSocket.close() is called so IOException is thrown
                    LoggingFormatter.printStackTrace = true;
                    Grabber.logger.log(Level.SEVERE, "JCov grabber failed", e);
                    working = false;
                }
            } finally {
                int timeout = 0;
                while (working && timeout < MAX_TIMEOUT * 2) { // if working == true -> kill(false) was called when some client are still alive. Server waits for their ending.
                    try {
                        Thread.sleep(500);
                        timeout += 500;
                    } catch (InterruptedException ex) {
                    }
                }
                ss = null;
                working = false;
                listening = false;
                Grabber.logger.log(Level.FINE, "Server stopped");
            }
        }
    }

    /**
     * Stop listening socket for new client connections and close socket
     * listener. IOException will be thrown in run() method from ServerSocket as
     * it will be closed.
     *
     * @param force when false - server will wait till all alive clients will
     * end transmitting data
     */
    public void kill(boolean force) {
        listening = false;
        if (ss != null && !ss.isClosed()) {
            try {
                ss.close(); // killing socket listener
            } catch (Exception ex) {
                Grabber.logger.log(Level.SEVERE, "Error while closing socket", ex);
            }
        }

        if (!force && clientsAlive()) {
            Grabber.logger.log(Level.INFO, "Awaiting for finishing data transmission from {0} clients. Max timeout time: {1}ms", new Object[]{getAliveConnectionCount() + "", MAX_TIMEOUT});
            waitForAliveClients(); // getting data from alive connected clients
        }

        if (!force) {
            if (!dataSaved) {
                saveData();
                if (dataRoot != null) { // started without template
                    dataRoot.destroy();
                }
                if (dumpCount > 0) {
                    if (veryLowMemoryRun) { // not merging at very low - it's not possible
                    } else {
                        Grabber.logger.log(Level.WARNING, "Server is merging dumped data: {0} files should be merged", dumpCount + "");
                        // merge datafiles
                        Merger merger = new Merger();
                        Result res[] = new Result[dumpCount + 1];
                        try {
                            res[0] = new Result(fileName, outTestList);
                            for (int i = 0; i < dumpCount; ++i) {
                                if (outTestList != null) {
                                    res[i + 1] = new Result(fileName, outTestList + i);
                                }
                            }
                        } catch (IOException ignore) {
                        }
                        System.out.println("Server.this.templateName = " + Server.this.templateName);
                        Merger.Merge m = new Merger.Merge(res, Server.this.templateName);
                        try {
                            merger.mergeAndWrite(m, outTestList, fileName, null);
                        } catch (OutOfMemoryError e) {
                            Grabber.logger.log(Level.SEVERE, "OutOfMemoryError while merging dumped files. Please merge them manually: 'java -jar jcov.jar merger -o {0} -outTestList {1} {0}%{1} {0}0%{1}) {0}1%{1}1 ...'.", new Object[]{fileName, outTestList});
                        } catch (Throwable ignore) {
                        }

                        for (int i = 0; i < dumpCount; ++i) {
                            new File(fileName + dumpCount).delete();
                            new File(outTestList + dumpCount).delete();
                        }
                        Grabber.logger.log(Level.INFO, "Merging done");
                    }
                }
            }
        }

        executor.shutdown();
        working = false;
    }

    /**
     * Read template from templateName and initializes template
     *
     * @throws FileFormatException
     */
    private void initDataRoot() throws FileFormatException {
        if (templateName != null) {
            Grabber.logger.log(Level.FINE, "Server is reading template {0}", templateName);

            this.dataRoot = DataRoot.read(templateName, false, null);
            this.dataRoot.makeAttached();
            Collect.SLOTS = this.dataRoot.getCount();
            Collect.enableCounts();
            this.data = Collect.counts();

            if (genscale) { // creating empty scales
                this.dataRoot.getScaleOpts().setReadScales(true);
                if (outTestList != null) {
                    this.dataRoot.getScaleOpts().setOutTestList(outTestList);
                }
                this.dataRoot.cleanScales(); // making zero scales
            }

            Grabber.logger.log(Level.FINER, "Server finished reading template", templateName);
        } else {
            // DataRoot will be created at first data accept
        }
    }

    /**
     * Receives data from Client and handles it. In SaveAtReceive mode Server
     * saves data instantly by calling saveData(data) method. In SaveAtExit mode
     * Server stores clients data and merges it in memory. Real saving will
     * occur as saveData() method calls. Sets dataSaved to false
     *
     * @param data received clients data to handle
     * @param client client received the data (used for logging)
     * @see #saveData()
     */
    public synchronized void handleData(long[] data, Client client) {
        if (!working) {
            return;
        }
        if (templateName == null) {
            Grabber.logger.log(Level.SEVERE, "Server can't accept static data - started without template");
            return;
        }
        dataSaved = false;
        Grabber.logger.log(Level.INFO, "Server got data from client N{0}", client.getClientNumber() + "");

        if (saveAtReceive) {
            Grabber.logger.log(Level.FINE, "Server is saving data from client N{0}", client.getClientNumber() + "");

            for (int i = 0; i < data.length && i < dataRoot.getCount(); ++i) {
                this.data[i] += data[i];
            }

            saveData(data);
        } else {
            if (this.data == null) {
                // no need for Collect.enableCounts as it's performed in Collect.clinit
                this.data = Collect.counts();
            }

            for (int i = 0; i < data.length && i < dataRoot.getCount(); ++i) {
                this.data[i] += data[i];
            }
            if (genscale) {
                dataRoot.addScales();
                dataRoot.update();

                boolean merged = false;
                if (mergeByTestNames) {
                    for (int i = 0; i < tests.size(); i++) {
                        if (tests.get(i).equals(client.getTestName())) {

                            ArrayList<Pair> list = new ArrayList<Pair>();
                            list.add(new Utils.Pair(i, tests.size()));
                            this.dataRoot.illuminateDuplicatesInScales(list);
                            merged = true;
                            break;
                        }
                    }
                }

                if (!merged) {
                    tests.add(client.getTestName());
                }

            }
        }

        Grabber.logger.log(Level.FINEST, "Data from client N{0} saved", client.getClientNumber() + "");
    }

    /**
     * Receives data from Client and handles it. In SaveAtReceive mode Server
     * saves data instantly by calling saveData(data) method. In SaveAtExit mode
     * Server stores clients data and merges it in memory. Real saving will
     * occur as saveData() method calls. Sets dataSaved to false
     *
     * @param data received clients data to handle
     * @param client client received the data (used for logging)
     * @see #saveData()
     */
    public synchronized void handleData(DataRoot root, Client client) {
        boolean merged = false;
        try {
            if (!working) {
                return;
            }
            dataSaved = false;
            Grabber.logger.log(Level.FINER, "Server got dynamic data from client N{0}", client.getClientNumber() + "");

            if (saveAtReceive) {
                try {
                    root.write(fileName, MERGE.MERGE);
                } catch (Exception ex) {
                    Grabber.logger.log(Level.SEVERE, "Server can't save data from client N{0} to file '{1}' on recieve: {2}", new Object[]{client.getClientNumber() + "", fileName, ex.getMessage()});
                }
            } else {
                Grabber.logger.log(Level.FINE, "Server is merging dynamic data from client N{0}", client.getClientNumber() + "");
                root.getScaleOpts().setScaleSize(root.getScaleOpts().getScaleSize() + 1);
                // dynamic data mode only - template == null. In this mode should save first data got as dataRoot
                if (templateName == null && dataRoot == null) {
                    dataRoot = root;
                    dataRoot.attach();
                    if (genscale && (dataRoot.getScaleOpts() == null || !dataRoot.getScaleOpts().needReadScales())) {
                        dataRoot.getScaleOpts().setReadScales(true);
                        dataRoot.getScaleOpts().setOutTestList(outTestList);
                        dataRoot.getScaleOpts().setScaleSize(1);
                        tests.add(client.getTestName());
                    }
                } else {
                    // merging. First of all - checking compatibility.
                    int severity = 0;
                    // merge with template - different data is allowed - only template data will be merged
                    if (templateName != null) {
                        severity = 3;
                    }
                    CompatibilityCheckResult cc = this.dataRoot.checkCompatibility(root, severity, true);
                    if (cc.errors == 0) {
                        // merge + scale if needed
                        if (genscale && mergeByTestNames) {
                            for (int i = 0; i < tests.size(); i++) {
                                if (tests.get(i).equals(client.getTestName())) {
                                    this.dataRoot.merge(root, templateName == null);

                                    ArrayList<Pair> list = new ArrayList<Pair>();
                                    list.add(new Utils.Pair(i, tests.size()));
                                    this.dataRoot.illuminateDuplicatesInScales(list);

                                    merged = true;
                                    break;
                                }
                            }

                        }
                        if (genscale && !merged) {
                            this.dataRoot.merge(root, templateName == null);
                            tests.add(client.getTestName());
                            merged = true;
                        }
                        if (!genscale) {
                            this.dataRoot.merge(root, templateName == null);
                            merged = true;
                        }

                        Grabber.logger.log(Level.FINER, "Server finished merging dynamic data from client N{0}", client.getClientNumber() + "");
                    } else {
                        // data not compatible - flushing to file if selected
                        if (saveBadData != null) {
                            String filename = saveBadData + "_" + client.getTestName() + ".xml";
                            Grabber.logger.log(Level.INFO, "Malformed data from client N{0}: saving data to '{1}'", new Object[]{client.getClientNumber() + "", filename});
                            try {
                                root.write(filename, MERGE.GEN_SUFF);
                            } catch (Exception ex) {
                                Grabber.logger.log(Level.SEVERE, "Can't save malformed data from client N{0} to file '{1}': {2}", new Object[]{client.getClientNumber() + "", filename, ex.getMessage()});
                            }
                        } else {
                            Grabber.logger.log(Level.SEVERE, "Malformed data from client N{0}: not saving", client.getClientNumber() + "");
                        }
                    }
                }
            }
        } finally {
            if (dataRoot != root && !merged) {
                // destroy not needed if already merged - "root" was destroyed in DataRoot.merge
                root.destroy();
            }
        }
    }

    /**
     * Save data to file if it's needed (if dataSaved == false)
     */
    public synchronized void saveData() {
        if (dataSaved == true) {
            Grabber.logger.log(Level.FINE, "No new data received - nothing to save");
            return; // nothing to do - received data is already saved
        }

        Grabber.logger.log(Level.INFO, "Server is saving cached data to {0}", fileName);
        saveData(data);
        Grabber.logger.log(Level.FINE, "Saving done");
    }

    /**
     * Save data to file. In SaveAtExit mode file will always be rewritten. Sets
     * dataSaved to true
     *
     * @param data data to save
     */
    private synchronized void saveData(long[] data) {
        try {
            if (!saveAtReceive) {
                File file = new File(fileName);
                if (file.exists()) {
                    if (!file.delete()) {
                        File nfile = getUnexistingFile(file);
                        Grabber.logger.log(Level.SEVERE, "Error: given file '{0}' exists, cannot overwrite it. Data saved to '{1}'", new Object[]{file.getPath(), nfile.getPath()});
                        file = nfile;
                        fileName = nfile.getPath();
                    }
                }
            }

            if (dataRoot != null) {
                if (templateName != null) {
                    //do not need it at all
                    dataRoot.update();
                    if (dataRoot.getParams().isInstrumentAbstract() || dataRoot.getParams().isInstrumentNative()) {
                        for (DataPackage dp : dataRoot.getPackages()) {
                            for (DataClass dc : dp.getClasses()) {
                                for (DataMethod dm : dc.getMethods()) {
                                    if ((dm.isAbstract() || (dm.getAccess() & Opcodes.ACC_NATIVE) != 0)
                                            && data.length > dm.getSlot() && dm.getCount() < data[dm.getSlot()]) {
                                        dm.setCount(data[dm.getSlot()]);
                                    }
                                }
                            }
                        }
                    }
                }
                FileSaver fs = FileSaver.getFileSaver(dataRoot, fileName, templateName, MERGE.OVERWRITE, false, true);
                fs.saveResults(fileName);
            } else if (templateName != null) {
                Utils.copyFile(new File(templateName), new File(fileName));
            }

            if (outTestList != null) {
                Utils.writeLines(outTestList, tests.toArray(new String[tests.size()]));
            }
            dataSaved = true;
        } catch (Exception ex) {
            Grabber.logger.log(Level.SEVERE, "Error while saving data", ex);
//            ex.printStackTrace();
        }
    }

    void increaseReserved(long reserve) {
        reservedMemory += reserve;
    }

    void decreaseReserved(long reserve) {
        reservedMemory -= reserve;
    }

    synchronized boolean checkFreeMemory(long minMemory) throws Exception {
        if (genscale) {
            long mem = rt.freeMemory() + (rt.maxMemory() - rt.totalMemory()); // mem - current total free memory (free + non-allocated)
            if (showMemoryChecks) {
                Grabber.logger.log(Level.FINER, "Memory check routine. Total: {0}; max: {1}; free: {2}; max-total {3}", new Object[]{rt.totalMemory(), rt.maxMemory(), rt.freeMemory(), rt.maxMemory() - rt.totalMemory()});
            }
            if (((veryLowMemoryRun || lowMemoryRun) && mem < minMemory) || (normalMemoryRun && (rt.totalMemory() - rt.freeMemory()) > rt.maxMemory() / 3)) {
                //        if (rt.totalMemory() > rt.maxMemory() / 2.1) {
                Grabber.logger.log(Level.INFO, "Server is at low memory: trying to clean memory...");
                rt.gc();
                Thread.yield();
                mem = rt.freeMemory() + (rt.maxMemory() - rt.totalMemory());
                if ((lowMemoryRun && mem < minMemory && !dumping) || (normalMemoryRun && (rt.totalMemory() - rt.freeMemory()) > rt.maxMemory() * 0.45)) {
//                if (rt.totalMemory() > rt.maxMemory() / 1.5 && totalConnections > 10 && !dumping) {
                    Grabber.logger.log(Level.WARNING, "Server is at low memory: cleaning memory didn''t help - {0}mb free memory left. Dumping data to the file...", mem);
                    dumpAndResetData();
                    return true;
                }
            }
        }
        return false;
    }

    boolean isDumping() {
        synchronized (STOP_THE_WORLD_LOCK) {
            return dumping;
        }
    }

    private synchronized void dumpAndResetData() throws Exception {
        dumping = true;
        Thread t = new Thread() {
            @Override
            public void run() {
                synchronized (STOP_THE_WORLD_LOCK) {
                    try {
                        dataRoot.update();
                        FileSaver fs = FileSaver.getFileSaver(dataRoot, fileName + dumpCount, templateName, MERGE.OVERWRITE, false, true);
                        fs.saveResults(fileName + dumpCount);
                        if (outTestList != null) {
                            Utils.writeLines(outTestList + dumpCount, tests.toArray(new String[tests.size()]));
                            tests.clear();
                        }
                        dataRoot.cleanScales();
                        for (int i = 0; i < data.length; ++i) {
                            data[i] = 0;
                        }
                        dataRoot.update(); // needed?
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    } finally {
                        ++dumpCount;
                        dumping = false;
                        STOP_THE_WORLD_LOCK.notifyAll();
                    }
                }
            }
        };
        t.setName("DumpingThread" + dumpCount);
        t.setPriority(Thread.MAX_PRIORITY);
        t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
            }
        });
        t.start();
    }

    private static File getUnexistingFile(File file) {
        File temp = null;
        do {
            String suffix = RuntimeUtils.genSuffix();
            temp = new File(file.getPath() + suffix);

        } while (temp.exists());
        return temp;
    }

    /**
     * Wait until all alive clients will finish working
     */
    private void waitForAliveClients() {
        int timeout = 0;
        while (timeout < MAX_TIMEOUT && clientsAlive()) {
            try {
                Thread.sleep(100);
                timeout += 100;
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    /**
     * Checks that all clients have finish working Removes dead Client objects
     * from clients list
     *
     * @return false when all clients have finish working
     */
    private boolean clientsAlive() {
        return aliveClients > 0;
    }

    /**
     * Server is working
     *
     * @return server is working
     */
    public boolean isWorking() {
        return working;
    }

    public boolean isStarted() {
        return started;
    }

    /**
     * Returns count of all connections occurred
     *
     * @return count of all connections occurred
     */
    public int getConnectionsCount() {
        return totalConnections;
    }

    /**
     * Returns count of connections that are alive at the moment The number can
     * change fastly
     *
     * @return count of connections that are alive at the moment
     */
    public int getAliveConnectionCount() {
        return aliveClients;
    }

    /**
     * Checks that there is no unsaved data
     *
     * @return true when all data is saved to file (or no data comed)
     */
    public boolean isDataSaved() {
        return dataSaved;
    }

    /**
     * Get output file name
     *
     * @return output file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Get data template file name
     *
     * @return data template file name
     */
    public String getTemplateName() {
        return templateName;
    }

    /**
     * Get listening port
     *
     * @return listening port, assigned to ServerSocket. If server was created
     * with port == 0, then real listening port will be returned. -1 is returned
     * if server is not working anymore of port is not binded.
     */
    public int getPort() {
        if (port == 0) {
            if (ss != null) {
                return ss.getLocalPort();
            } else {
                if (!working) {
                    return -1;
                }
            }
        }

        return port;
    }

    /**
     * Check whether server is saving data on receive
     *
     * @return true if server is saving data on receive
     */
    public boolean isSaveOnReceive() {
        return saveAtReceive;
    }

    /**
     * Get maximum connections count
     *
     * @return maximum connections count
     */
    public int getMaxCount() {
        return saveCount;
    }

    /**
     * Set the maximum connection count that this Server will collect
     *
     * @param saveCount
     */
    public void setMaxCount(int saveCount) {
        this.saveCount = saveCount;
    }

    /**
     * Set maximum time in milliseconds to wait for alive clients when closing
     * Server
     *
     * @param MAX_TIMEOUT
     */
    public static void setMaxTimeout(int MAX_TIMEOUT) {
        Server.MAX_TIMEOUT = MAX_TIMEOUT;
    }

    /**
     * Set output testlist file
     *
     * @param outTestList
     */
    public void setOutTestList(String outTestList) {
        this.outTestList = outTestList;
    }

    /**
     * Set output file
     *
     * @param fileName
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSaveBadData() {
        return saveBadData;
    }

    public void setSaveBadData(String saveBadData) {
        this.saveBadData = saveBadData;
    }
} // ############# Server

/**
 * Class for server command listener. Receives commands from commandPort and
 * controls the Server
 */
class CommandListener extends Thread {

    private final int commandPort;              // port to listen for commands
    private final Server server;                // server to control
    private String runCommand;                  // command the Server was started with
    private ServerSocket serverSocket = null;   // socket listener
    private String hostName;                    // name of the host running the Server
    // runtime data
    private boolean working = true;             // thread is working

    /**
     * Constructor for CommandListener class Sets "CommandListener" as the name
     * of the thread
     *
     * @param commandPort port to listen for commands
     * @param server Server to control
     */
    public CommandListener(int commandPort, Server server) throws BindException, IOException {
        this(commandPort, server, "<unknown>", "<localhost>");
    }

    /**
     * Constructor for CommandListener class Sets "CommandListener" as the name
     * of the thread
     *
     * @param commandPort port to listen for commands
     * @param server Server to control
     * @param runCommand command this Server was runned with
     */
    public CommandListener(int commandPort, Server server, String runCommand, String hostName) throws BindException, IOException {
        super();

        setDaemon(false);
        setName("CommandListener");
        this.server = server;
        this.runCommand = runCommand;

        serverSocket = new ServerSocket(commandPort);
        this.commandPort = serverSocket.getLocalPort();
        this.hostName = hostName;
    }

    /**
     * Main method. Use start() to launch in new thread. CommandListener listens
     * commandPort with ServerSocket and receives commands from it. Every 5
     * seconds ServerSocket is getting TimeoutException so ServerSocket.accept()
     * releases the thread to check whether Server is still alive.
     */
    @Override
    public void run() {
        try {
            Grabber.logger.log(Level.CONFIG, "Server is waiting for commands at port {0}", Integer.toString(commandPort));
            outer:
            while (working && server.isWorking() && serverSocket != null) {
                Socket socket = null;
                try {
                    serverSocket.setSoTimeout(5000);
                    socket = serverSocket.accept();
                    InputStream in = socket.getInputStream();
                    int command = in.read();
                    Grabber.logger.log(Level.FINE, "Server received command {0} from {1}", new Object[]{Integer.toString(command), socket.getInetAddress().getHostAddress()});
                    switch (command) {
                        case MiscConstants.GRABBER_KILL_COMMAND:
                            Grabber.logger.log(Level.INFO, "Server received kill command.");
                            BufferedReader inReader = new BufferedReader(new InputStreamReader(in, Charset.defaultCharset()));
                            int killtimeout = Integer.valueOf(inReader.readLine());
                            if (killtimeout > 0){
                                server.setMaxTimeout(killtimeout*1000);
                            }
                            server.kill(false);
//                            server.saveData();
                            socket.getOutputStream().write(1);
                            socket.getOutputStream().close();
                            inReader.close();
                            break outer;
                        case MiscConstants.GRABBER_FORCE_KILL_COMMAND:
                            socket.getOutputStream().write(1);
                            socket.getOutputStream().close();
                            in.close();
                            Grabber.logger.log(Level.SEVERE, "Server received forced kill command. Exiting.");
                            server.kill(true);
                            break outer;
                        case MiscConstants.GRABBER_SAVE_COMMAND:
                            socket.getOutputStream().write(1);
                            socket.getOutputStream().close();
                            in.close();
                            Grabber.logger.log(Level.FINE, "Server received save command.");
                            server.saveData();
                            break;
                        case MiscConstants.GRABBER_STATUS_COMMAND:
                            OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream(), Charset.forName("UTF-8"));
                            String message = String.format("%b;%d;%d;%b;%s;%s;%s;%s", server.isWorking(), server.getConnectionsCount(), server.getAliveConnectionCount(),
                                    server.isDataSaved(), runCommand, new File(".").getCanonicalPath(), server.getTemplateName(), server.getFileName());
                            Grabber.logger.log(Level.FINEST, "Sending status '{0}'", message);
                            out.write(message, 0, message.length());
                            out.flush();
                            in.close();
                            out.close();
                            break;
                        case MiscConstants.GRABBER_WAIT_COMMAND:
                            out = new OutputStreamWriter(socket.getOutputStream(), Charset.forName("UTF-8"));
                            if (server.isStarted()) {
                                message = String.format("%b;%s;%d;%s", true, hostName, server.getPort(), server.getTemplateName());
                                Grabber.logger.log(Level.FINEST, "Sending started status: {0}", message);
                            } else {
                                message = String.format("%b;%s;%d;%s", false, hostName, server.getPort(), server.getTemplateName());
                            }
                            out.write(message, 0, message.length());
                            out.flush();
                            in.close();
                            out.close();
                            break;
                        default:
                            Grabber.logger.log(Level.WARNING, "Unknown message '{0}' came from {0}", new Object[]{Integer.toString(command), socket.getInetAddress().getHostAddress()});
                            break;
                    }
                } catch (SocketTimeoutException ex) {
                    if (!server.isWorking()) {
                        break;
                    }
                } catch (IOException ex) {
                    if (serverSocket != null && !serverSocket.isClosed()) { // means that kill() was called
                        Grabber.logger.log(Level.SEVERE, "Exception occurred while processing command", ex);
                    }
                } finally {
                    if (socket != null && !socket.isClosed()) {
                        try {
                            socket.close();
                        } catch (IOException ex) {
                        }
                    }
                }

            }
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException ex) {
                }
            }
            serverSocket = null;
            working = false;
        }
    }

    /**
     * Stop listening port for commands IOException will be thrown in run()
     * method from ServerSocket as it will be closed.
     */
    public void kill() {
        working = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ex) {
            }
        }
        serverSocket = null;
    }

    /**
     * Sets the command used for running the Grabber
     *
     * @param runCommand the command used for running the Grabber
     */
    public void setRunCommand(String runCommand) {
        if (runCommand == null || runCommand.trim().equals("")) {
            this.runCommand = "<empty>";
        } else {
            this.runCommand = runCommand;
        }
    }

    /**
     * Get command the server was runned with
     *
     * @return command the server was runned with
     */
    public String getRunCommand() {
        return runCommand;
    }

    /**
     * Get port Command Listener is listening
     *
     * @return port Command Listener is listening
     */
    public int getPort() {
        if (commandPort == 0) {
            if (serverSocket != null) {
                return serverSocket.getLocalPort();
            } else {
                if (!working) {
                    return -1;

                }
            }
        }

        return commandPort;
    }
} // ############# CommandListener

//              ##############################################################################
//              ##############################################################################
//              ##############################################################################
//              ################################# MAIN CLASS #################################
//              ##############################################################################
//              ##############################################################################
//              ##############################################################################
/**
 * <p> Grabber is a class that collects coverage data in both static and dynamic
 * formats from remote clients. RT clients send their data through
 * JCovSocketSavers or Grabber connects to ServerSocketSavers set up by RT
 * clients. </p> <p> Grabber class manages 2 classes: Server and
 * CommandListener. First is listening for client connections and the second is
 * listening for commands sent from GrabberManager tool. </p>
 *
 * @see GrabberManager
 * @author andrey
 */
public class Grabber extends JCovCMDTool {

    static final Logger logger;

    static {
        Utils.initLogger();
        logger = Logger.getLogger(Grabber.class.getName());
    }
    public static final String COMMAND_PORT_PORPERTY = "jcov.grabber.commandPort";
    public static final String MAX_COUNT_PROPERTY = "jcov.grabber.maxCount";
    public static final String OUTPUTFILE_PROPERTY = "jcov.grabber.outputfile";
    public static final String PORT_PROPERTY = "jcov.grabber.port";
    public static final String RUN_LINE_PROPERTY = "jcov.grabber.runLine";
    public static final String SAVE_ON_RECEIVE_PROPERTY = "jcov.grabber.saveOnReceive";
    public static final String SAVE_IN_SHUTDOWN_HOOK_PROPERTY = "jcov.grabber.shutdownHookInstalled";
    public static final String TEMPLATE_PROPERTY = "jcov.grabber.template";
    public static final String SERVER_VERSION_PROPERTY = "jcov.grabber.version";
    public static final String SERVER_LOCAL_HOSTNAME_PROPERTY = "jcov.grabber.localhostname";
    private boolean saveInShutdownHook = false;     // true -> data would be automatically saved in shutdown hook
    private String propfile;                        // propfile to write
    private String hostName;                        // host running the Server
    private int maxCount;                           // maximum connections that would be processed by Server
    private Thread shutdownHook = null;             // shutdown hook which will save data if saveInShutdownHook is set to true
    // CommandListener configuration
    private boolean noCommand;                      // do not start CommandListener
    private CommandListener commandListener = null; // commandListener instance
    private int commandPort;                        // port to listen for commands from GrabberManager
    private String runCommand;                      // command this Grabber was started with
    // Server configuration
    private int port;                               // port to launch Server and listen for clients connections
    private Server server = null;                   // server instance
    private boolean once;                           // "once" mode when Server connects to the client
    private String template;                        // template to read (is passed to Server)
    private String filename;                        // output filename
    private boolean saveOnReceive;                  // save data on recieve (true) or on exit (false)
    private String onceHostToConnect;               // host to connect in "once" mode
    private String outTestList;                     // file to write testlist to
    private boolean genscale;                       // allows to generate scales without outtestlist
    private String baddata;                         // directory to write bad data to
    private String messageFormat;
    private boolean mergeByTestNames = false;       // generate scales based on test names (test name identifies test)

    /**
     * Get Properties object initialized with info about Server, Command
     * Listener and shutdown hook Properties object is initialized with:
     * jcov.grabber.port - Server's port jcov.grabber.commandPort - Command
     * Listener's port if set (otherwise this property is not set)
     * jcov.grabber.saveOnReceive - is the Server saving data on receive
     * jcov.grabber.shutdownHookInstalled - is the shutdown hook installed
     * jcov.grabber.maxCount - maximum Server connections allowed
     * jcov.grabber.template - template file jcov.grabber.outputfile - output
     * file jcov.grabber.runLine - line the Server is runned with (<unknown>
     * when not set and <empty> when there are no arguments)
     *
     * @return initialized Properties object
     */
    public Properties getProperties() {
        Properties ps = new Properties();

        ps.setProperty(PORT_PROPERTY, Integer.toString(server.getPort()));
        if (commandListener != null) {
            ps.setProperty(COMMAND_PORT_PORPERTY, Integer.toString(commandListener.getPort()));
            ps.setProperty(RUN_LINE_PROPERTY, commandListener.getRunCommand());
        }
        ps.setProperty(SAVE_ON_RECEIVE_PROPERTY, Boolean.toString(server.isSaveOnReceive()));
        ps.setProperty(SAVE_IN_SHUTDOWN_HOOK_PROPERTY, Boolean.toString(saveInShutdownHook));
        ps.setProperty(MAX_COUNT_PROPERTY, Integer.toString(server.getMaxCount()));
        if (server != null) {
            ps.setProperty(TEMPLATE_PROPERTY, server.getTemplateName());
            ps.setProperty(OUTPUTFILE_PROPERTY, server.getFileName());
        }
        ps.setProperty(SERVER_VERSION_PROPERTY, JcovVersion.jcovVersion + "-" + JcovVersion.jcovBuildNumber);
        try {
            ps.setProperty(SERVER_LOCAL_HOSTNAME_PROPERTY, InetAddress.getLocalHost().toString());
        } catch (UnknownHostException ex) {
        }

        return ps;
    }

    /**
     * Write properties to a file. Properties.store() is used so
     * Properties.load() can be used to retrieve data
     *
     * @param file file to write properites
     */
    public void writePropfile(String file) throws IOException {
        Properties ps = getProperties();

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            ps.store(out, null);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * Wait untill Server and CommandListener will stop
     *
     * @throws InterruptedException
     */
    public void waitForStopping() throws InterruptedException {
        if (server != null) {
            server.join();
        }
        if (commandListener != null) {
            commandListener.kill();
            commandListener.join();
        }
    }

    /**
     *
     * @throws BindException
     * @throws IOException
     */
    public void start(boolean startCommandListener) throws BindException, IOException {
        this.saveInShutdownHook = true;
        createServer();
        if (startCommandListener) {
            startCommandListener(commandPort);
        }

        installShutdownHook();
        startServer();
    }

    public void createServer() throws BindException, IOException {
        server = new Server(port, once, template, filename, outTestList, hostName, maxCount, saveOnReceive, genscale, mergeByTestNames);
        server.setSaveBadData(baddata);
    }

    /**
     * Start Server object
     */
    public void startServer() {
        server.start();
    }

    /**
     * Start CommandListener object
     *
     * @param commandPort
     */
    public void startCommandListener(int commandPort) throws BindException, IOException {
        if (server == null) {
            throw new IllegalStateException("Server is not created");
        }
        if (commandListener != null && commandListener.isAlive()) {
            return;
        }

        commandListener = new CommandListener(commandPort, server);
        commandListener.start();
    }

    /**
     * Start CommandListener object
     *
     * @param commandPort
     * @param runCommand command this Serever was runned with (used to send
     * 'info' command responce)
     */
    public void startCommandListener(int commandPort, String runCommand) throws BindException, IOException {
        if (server == null) {
            throw new IllegalStateException("Server is not created");
        }
        if (commandListener != null && commandListener.isAlive()) {
            return;
        }

        commandListener = new CommandListener(commandPort, server, runCommand, hostName);
        commandListener.start();
    }

    /**
     * Install shutdown hook that will save data and stop CommandListener and
     * Server
     */
    public void installShutdownHook() {
        if (shutdownHook == null) { // do this only when hook is not installed
            shutdownHook = new Thread() {
                @Override
                public void run() {
                    logger.log(Level.CONFIG, "Shutdownhook fired");
                    if (commandListener != null) {
                        commandListener.kill();
                    }
                    if (server != null && server.isWorking()) { // in normal exit server should return false
                        if (saveInShutdownHook) {
                            server.saveData();
                        }
                        server.kill(false);
                    }
                    logger.log(Level.FINE, "Shutdownhook done");
                }
            };
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            logger.log(Level.FINE, "Shutdownhook installed");
        }
    }

    /**
     * Stop the Server. CommandListener will also be stopped.
     *
     * @param force when false - server will wait till all alive clients will
     * end transmitting data
     */
    public void stopServer(boolean force) {
        if (server != null) {
            server.kill(force);
            server = null;
        }
        if (commandListener != null) {
            stopCommandListener();
        }
    }

    /**
     * Stop the CommandListener
     */
    public void stopCommandListener() {
        commandListener.kill();
        commandListener = null;
    }

    /**
     * Set run command to the Command Listener (wrapper method)
     *
     * @param command run command
     * @return true if runcommand was set, false otherwise (when Command
     * Listener is not created)
     */
    public boolean setRunCommand(String command) {
        if (commandListener != null) {
            commandListener.setRunCommand(command);
            return true;
        }
        return false;
    }

    /**
     * main
     */
    public static void main(String args[]) {
        Grabber tool = new Grabber();
        try {
            int res = tool.run(args);
            System.exit(res);
        } catch (Exception ex) {
            System.exit(1);
        }
    }

    public String usageString() {
        return "java com.sun.tdk.jcov.Grabber [-option value]";
    }

    public String exampleString() {
        return "java -cp jcov.jar com.sun.tdk.jcov.Grabber -output grabbed.xml -port 3000 -once -merge";
    }

    protected String getDescr() {
        return "gathers information from JCov runtime via sockets";
    }

    public int getServerPort() {
        if (server != null) {
            return server.getPort();
        }

        return -1;
    }

    public int getCommandListenerPort() {
        if (commandListener != null) {
            return commandListener.getPort();
        }

        return -1;
    }

    @Override
    public EnvHandler defineHandler() {
        return new EnvHandler(new OptionDescr[]{
                    DSC_OUTPUT,
                    DSC_VERBOSE,
                    DSC_VERBOSEST,
                    DSC_SHOW_MEMORY_CHECKS,
                    com.sun.tdk.jcov.instrument.InstrumentationOptions.DSC_TEMPLATE,
                    DSC_HOSTNAME,
                    DSC_ONCE,
                    DSC_PORT,
                    DSC_COUNT,
                    DSC_SAVE_MODE,
                    DSC_COMMAND_PORT,
                    DSC_PROPFILE,
                    Merger.DSC_OUTPUT_TEST_LIST,
                    Merger.DSC_SCALE,
                    DSC_BADDATA,
                    DSC_MESSAGE_FORMAT,
                    DSC_SCALE_BY_NAME
                }, this);
    }

    @Override
    public int handleEnv(EnvHandler opts) throws EnvHandlingException {
        template = opts.getValue(InstrumentationOptions.DSC_TEMPLATE);
        try {
            File t = Utils.checkFileNotNull(template, "template filename", Utils.CheckOptions.FILE_EXISTS, Utils.CheckOptions.FILE_ISFILE, Utils.CheckOptions.FILE_CANREAD);

            long recomendedMemory = Math.max(1000000000, t.length() * 16 + 150000000); // 12 = 4 (template in memory) * 4; 150000000 = 10mb (for each client) * 15 (number of concurrent/agent clients)
            long minMemory = Math.max(468000000, t.length() * 6 + 50000000); // 6 = 4 * 1.5; 50000000 = 10mb * 5
            if (Runtime.getRuntime().maxMemory() < minMemory) { // < 0.8gb
                logger.log(Level.WARNING, "Grabber started with {0}M max memory. Minimal requirement for this template is {1}M, recomended {2}M. ", new Object[]{Runtime.getRuntime().maxMemory() / 1000000, minMemory / 1000000, recomendedMemory / 1000000});
            } else if (Runtime.getRuntime().maxMemory() < recomendedMemory) {
                logger.log(Level.WARNING, "Grabber started with {0}M max memory. It''s recomended to have at least {1}M max memory. ", new Object[]{Runtime.getRuntime().maxMemory() / 1000000, recomendedMemory / 1000000});
            }
        } catch (EnvHandlingException ex) {
            if (opts.isSet(InstrumentationOptions.DSC_TEMPLATE)) {
                throw ex;
            }
            logger.log(Level.WARNING, "Grabber started without template, accepting only dynamic data");
            template = null;
        }

        try {
            InetAddress adr = InetAddress.getLocalHost();
            hostName = adr.getHostName();
        } catch (Exception ee) {
            logger.log(Level.WARNING, "Can't get real local host name, using '<localhost>'");
            hostName = "<localhost>";
        }

        if (opts.isSet(DSC_VERBOSE)) {
            Utils.setLoggingLevel(Level.INFO);
        }
        if (opts.isSet(DSC_VERBOSEST)) {
            Utils.setLoggingLevel(Level.ALL);
        }
        if (opts.isSet(DSC_SHOW_MEMORY_CHECKS)) {
            Server.showMemoryChecks = true;
        }

        once = opts.isSet(DSC_ONCE);

        port = opts.isSet(DSC_PORT)
                ? Utils.checkedToInt(opts.getValue(DSC_PORT), "port number", Utils.CheckOptions.INT_NONNEGATIVE)
                : (once ? MiscConstants.JcovOncePortNumber : MiscConstants.JcovPortNumber);

        maxCount = Utils.checkedToInt(opts.getValue(DSC_COUNT), "max connections count");

        filename = opts.isSet(DSC_OUTPUT) ? opts.getValue(DSC_OUTPUT) : MiscConstants.JcovSaveFileNameXML;
        Utils.checkFileNotNull(filename, "output filename", Utils.CheckOptions.FILE_PARENTEXISTS, Utils.CheckOptions.FILE_NOTISDIR);

        saveOnReceive = "receive".equalsIgnoreCase(opts.getValue(DSC_SAVE_MODE));

        if (once) {
            onceHostToConnect = opts.getValue(DSC_HOSTNAME);
            Utils.checkHostCanBeNull(onceHostToConnect, "testing host");
        }

        String commandPortStr = opts.getValue(DSC_COMMAND_PORT);
        if (once) {
            commandPort = 0;
        } else if (commandPortStr == null) {
            commandPort = MiscConstants.JcovGrabberCommandPort;
        } else {
            commandPort = Utils.checkedToInt(commandPortStr, "command port number");
        }
        noCommand = opts.isSet(DSC_NO_COMMAND);
        propfile = opts.getValue(DSC_PROPFILE);
        Utils.checkFileCanBeNull(propfile, "property filename", Utils.CheckOptions.FILE_NOTISDIR, Utils.CheckOptions.FILE_PARENTEXISTS);

        runCommand = opts.unParse();

        outTestList = opts.getValue(Merger.DSC_OUTPUT_TEST_LIST);
        Utils.checkFileCanBeNull(outTestList, "output testlist filename", Utils.CheckOptions.FILE_NOTISDIR, Utils.CheckOptions.FILE_PARENTEXISTS);

        genscale = opts.isSet(Merger.DSC_SCALE);

        baddata = opts.getValue(DSC_BADDATA);
        Utils.checkFileCanBeNull(baddata, "directory for bad datafiles", Utils.CheckOptions.FILE_EXISTS, Utils.CheckOptions.FILE_ISDIR);

        messageFormat = opts.getCleanValue(DSC_MESSAGE_FORMAT);

        mergeByTestNames = opts.isSet(DSC_SCALE_BY_NAME);

        return SUCCESS_EXIT_CODE;
    }

    @Override
    public int run() throws Exception {
        if (!saveOnReceive) {
            logger.log(Level.INFO, "Installing shutdown hook");
        }
        saveInShutdownHook = !saveOnReceive;

        // Trying to create server. It can fail in case port is incorrect or is already binded
        try {
            createServer();
            int returnValue = 0;

            boolean shouldStartCL = !(once) || noCommand;
            if (shouldStartCL) {
                try { // Trying to create and start command listener. It can fail in case port is incorrect or is already binded
                    startCommandListener(commandPort, runCommand);
                } catch (BindException ex) {
                    Grabber.logger.log(Level.SEVERE, "Cannot bind CommandListener to {0}: {1}", new Object[]{
                                commandPort == 0 ? "any free port" : "port " + String.valueOf(commandPort),
                                ex.getMessage()});
                    returnValue = 6;
                } catch (IOException ex) {
                    Grabber.logger.log(Level.SEVERE, "Cannot create CommandListener", ex);
                    returnValue = 6;
                }
            }

            if (!shouldStartCL || commandListener != null) { // run Server only if CommandListener was started or should not to be started
                installShutdownHook();
                startServer();

                char[] c = new char[]{'p', 'h', 'c', 't', 'C', 'o', 'O', 's', 'S'};
                String[] s = new String[]{Integer.toString(server.getPort()), hostName, Integer.toString(commandListener.getPort()), template, Integer.toString(maxCount), filename, outTestList, Boolean.toString(genscale), saveOnReceive ? "receive" : "exit"};
                System.out.println(PropertyFinder.processMacroString(messageFormat, c, s));

                String file = propfile;
                if (file != null) {
                    try {
                        writePropfile(file);
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, "Error while trying to store properties file: ", ex);
                    }
                }
            } else {
                server.kill(true); // CommandListener failed to start
            }

            logger.log(Level.INFO, "Waiting for closing all processes");
            waitForStopping();
            logger.log(Level.INFO, "Server is stopped");

            return returnValue;
        } catch (BindException ex) {
            Grabber.logger.log(Level.SEVERE, "Cannot bind Server to {0}: {1}", new Object[]{
                        port == 0 ? "any free port" : "port " + String.valueOf(commandPort),
                        ex.getMessage()});
            return 5;
        } catch (IOException ex) {
            Grabber.logger.log(Level.SEVERE, "Cannot create Server", ex);
            return 5;
        }
    }

    public int getCommandPort() {
        return commandPort;
    }

    public void setCommandPort(int commandPort) {
        this.commandPort = commandPort;
    }

    public String getOutputFilename() {
        return filename;
    }

    public void setOutputFilename(String filename) {
        this.filename = filename;
    }

    public boolean isGenscale() {
        return genscale;
    }

    public void setGenscale(boolean genscale) {
        this.genscale = genscale;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    public boolean isNoCommand() {
        return noCommand;
    }

    public void setNoCommand(boolean noCommand) {
        this.noCommand = noCommand;
    }

    public boolean isOnce() {
        return once;
    }

    public void setOnce(boolean once) {
        this.once = once;
    }

    public String getHostToConnect() {
        return onceHostToConnect;
    }

    public void setHostToConnect(String onceHostToConnect) {
        this.onceHostToConnect = onceHostToConnect;
    }

    public String getOutTestList() {
        return outTestList;
    }

    public void setOutTestList(String outTestList) {
        this.outTestList = outTestList;
    }

    public void setMergeByTestNames(boolean mergeByTestNames) {
        this.mergeByTestNames = mergeByTestNames;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isSaveInShutdownHook() {
        return saveInShutdownHook;
    }

    public void setSaveInShutdownHook(boolean saveInShutdownHook) {
        this.saveInShutdownHook = saveInShutdownHook;
    }

    public boolean isSaveOnReceive() {
        return saveOnReceive;
    }

    public void setSaveOnReceive(boolean saveOnReceive) {
        this.saveOnReceive = saveOnReceive;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getMessageString() {
        return messageFormat;
    }

    public void setMessageString(String message) {
        this.messageFormat = message;
    }
    public final static OptionDescr DSC_SAVE_MODE =
            new OptionDescr("save", "", OptionDescr.VAL_SINGLE, new String[][]{
                {"receive", "Save data to a file on receiving and then merge into it"},
                {"exit", "Save data on exit merging it in memory (faster but more memory used)"}
            }, "Specify when incoming data should be merged and saved.", "exit");
    public final static OptionDescr DSC_OUTPUT =
            new OptionDescr("grabber.output", new String[]{"output", "o"}, "Output parameters definition", OptionDescr.VAL_SINGLE,
            "Specifies output file.", "result.xml for xml format or java.jcov for legacy");
    public final static OptionDescr DSC_VERBOSE =
            new OptionDescr("verbose", new String[]{"v"}, "Verbosity level", "Show more messages.");
    public final static OptionDescr DSC_VERBOSEST =
            new OptionDescr("verbosemore", new String[]{"vv"}, "", "Show all messages.");
    public final static OptionDescr DSC_SHOW_MEMORY_CHECKS =
            new OptionDescr("showmemory", "", "Show memory checks.");
    public final static OptionDescr DSC_HOSTNAME =
            new OptionDescr("hostname", "Connection parameters", OptionDescr.VAL_SINGLE,
            "Specify host to connect when client mode is used.", "localhost");
    public final static OptionDescr DSC_ONCE =
            new OptionDescr("once", new String[]{"client"}, "",
            "Specify client mode.");
    //true - client, false - server
    public final static OptionDescr DSC_PORT =
            new OptionDescr("port", "", OptionDescr.VAL_SINGLE,
            "Specify port. Use -port 0 to use any free port for server (only for server mode)",
            MiscConstants.JcovPortNumber + " for server mode, or " + MiscConstants.JcovOncePortNumber + " for client mode.");
    public final static OptionDescr DSC_COUNT =
            new OptionDescr("count", "", OptionDescr.VAL_SINGLE,
            "Specify maximum times of receiving data. 0 corresponds to unlimited.", "0");
    public final static OptionDescr DSC_COMMAND_PORT =
            new OptionDescr("command_port", new String[]{"command"}, "", OptionDescr.VAL_SINGLE,
            "Set port to listen commands. Use -command 0 to use any free port for command listener.", Integer.toString(MiscConstants.JcovGrabberCommandPort));
    public final static OptionDescr DSC_NO_COMMAND =
            new OptionDescr("nocommand", new String[]{"nc"}, "", OptionDescr.VAL_NONE,
            "Use to not run command listener");
    public final static OptionDescr DSC_PROPFILE =
            new OptionDescr("grabber.props", "Properties file", OptionDescr.VAL_SINGLE,
            "Write properties to a file to use them in the manager.");
    public final static OptionDescr DSC_BADDATA =
            new OptionDescr("baddatato", "manage bad data", OptionDescr.VAL_SINGLE,
            "Directory to write data that can't be merged with the template.");
    public final static OptionDescr DSC_MESSAGE_FORMAT =
            new OptionDescr("message", "welcome message format", OptionDescr.VAL_SINGLE,
            "Specify format for output welcome message. %p% - port, %c% - command port, %h% - running host, %t% - used template, %C% - maximum connection count (0 == unlimited), %o% - output file, %O% - output testlist file, %s% - generate scales, %S% - save at receive or exit",
            "Server started on %h%:%p%. Command listener at port %c%. Used template '%t%'.");
    public final static OptionDescr DSC_SCALE_BY_NAME =
            new OptionDescr("mergebyname", "process/generate test scales",
            "test name identifies the test. tests with same name will be automatically merged");
}
