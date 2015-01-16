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
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;

/**
 *
 * @author Andrey Titov
 */
public class JTObserver implements Harness.Observer {

    private static int port = 3337;
    public static final int SAVE = 0;
    public static final int NAME = 1;

    private static final HashSet<ClientData> clientsData = new HashSet<ClientData>();
    private static volatile String currentname = null;
    private static volatile boolean saving = false;
    private static volatile boolean naming = false;

    public JTObserver() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket listener = new ServerSocket(port);
                    try {
                        while (true) {
                            new Handler(listener.accept()).start();
                        }
                    } finally {
                        listener.close();
                    }
                }
                catch (Exception e){
                    log("Socket server exception: "+e);
                }
            }
        }).start();
    }

    public void startingTestRun(Parameters prmtrs) {
        log("Starting test run");
    }

    public void startingTest(TestResult tr) {
        send(tr.getTestName(), NAME);
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
                send(tr.getTestName(), SAVE);
            }
        });
        log("Starting test " + tr.getTestName());
    }

    public void finishedTest(TestResult tr) {
        log("Finished test " + tr.getTestName());
    }

    public void stoppingTestRun() {
        log("Stopping testrun");
    }

    public void finishedTesting() {
        log("Finished testing");
    }

    public void finishedTestRun(boolean bln) {
        log("Finished testrun");
    }

    public void error(String string) {
        log("error");
    }

    private static void log(String str) {
        //System.out.println(str);
    }

    private void send(String name, int command) {

        log("send name="+name + " command = "+command);
        log("clientsData.size() ="+clientsData.size());

        if (command == JTObserver.NAME) {
            currentname = name;
            naming = true;

            while (!nameClientsData()){
                try {
                    Thread.currentThread().sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            synchronized (clientsData){
                for (ClientData clientData :clientsData){
                    clientData.setNamed(false);
                }
            }
        }

        if (command == JTObserver.SAVE) {
            saving = true;
            currentname = name;

            while (!saveClientsData()){
                try {
                    Thread.currentThread().sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            synchronized (clientsData){
                for (ClientData clientData :clientsData){
                    clientData.setSaved(false);
                }
            }
        }
        log("end send");
    }

    private boolean nameClientsData(){
        synchronized (clientsData){

            for (ClientData clientData : clientsData){
                if (!clientData.isNamed())
                    return false;
            }

            if (clientsData.size() != 0)
                naming = false;

            return true;
        }
    }

    private boolean saveClientsData(){
        synchronized (clientsData){

            for (ClientData clientData :clientsData){
                if (!clientData.isSaved())
                    return false;
            }

            if (clientsData.size() != 0)
                saving = false;

            return true;
        }
    }

    private class Handler extends Thread {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;


        public Handler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                ClientData clientData = null;

                synchronized (clientsData) {
                    clientData = new ClientData(out, in);
                    clientsData.add(clientData);
                }

                while (true) {

                    try {
                        Thread.currentThread().sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    synchronized (clientData) {

                        if (naming && !clientData.isNamed()){
                            try {
                                clientData.getWriter().println("NAME" + currentname);
                                String answer = clientData.getReader().readLine();
                                log("name answer = " + answer);
                            }
                            catch(Exception e){
                                //we do not close clients connections
                            }
                            clientData.setNamed(true);
                        }

                        if (saving && !clientData.isSaved()){
                            try {
                                clientData.getWriter().println("SAVE" + currentname);
                                String answer = clientData.getReader().readLine();
                                log("save answer = " + answer);
                            }
                            catch(Exception e){
                                //we do not close clients connections
                            }
                            clientData.setSaved(true);
                        }

                        if (currentname!=null && !naming && !saving && !clientData.isNamed()){
                            try {
                                clientData.getWriter().println("NAME" + currentname);
                                String answer = clientData.getReader().readLine();
                                log("name answer = " + answer);
                            }
                            catch(Exception e){
                                //we do not close clients connections
                            }
                            clientData.setNamed(true);
                        }
                    }

                }
            } catch (IOException e) {
                log("JTObserver: " + e);
            }
        }
    }

    private static class ClientData {
        private PrintWriter writer;
        private BufferedReader reader;
        private boolean named = false;
        private boolean saved = false;

        public ClientData(PrintWriter writer, BufferedReader reader){
            this.writer = writer;
            this.reader = reader;
        }

        public PrintWriter getWriter(){
            return writer;
        }

        public BufferedReader getReader(){
            return reader;
        }

        public boolean isNamed() {
            return named;
        }

        public void setNamed(boolean named) {
            this.named = named;
        }

        public boolean isSaved() {
            return saved;
        }

        public void setSaved(boolean saved) {
            this.saved = saved;
        }
    }
}