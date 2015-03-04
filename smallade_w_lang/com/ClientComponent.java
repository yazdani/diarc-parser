/**
 * ADE 1.0
 * (c) copyright HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@gmail.com
 * Last update: April 2010
 *
 * ClientComponent.java
 */
package com;

import java.io.*;
import java.net.*;

/** Class that listens for client requests and dispatches a
 * ClientComponentThread to service them. */
public class ClientComponent implements Runnable {

    private static String prg = "ClientComponent";
    private int timeout = 100; // milliseconds
    SocketComponentImpl ssi;
    ServerSocket serverSocket = null;
    Thread t;
    ThreadGroup tg = null;
    String host = null;
    boolean listening = true;
    int port;
    int maxClients;
    boolean debug = true;

    public ClientComponent(SocketComponentImpl uc, String host, int port, int count, ThreadGroup tg) throws IOException {
        this.ssi = uc;
        this.tg = tg;
        this.port = port;
        this.host = host;
        this.maxClients = count;
        serverSocket = new ServerSocket(port, count);
        //this.t = new Thread(this);
        //t.start();
        if (debug) {
            System.out.println(prg + ": leaving constructor");
        }
    }

    /** Serve the client requests */
    public void run() {
        if (debug) {
            System.out.print(prg + ": Waiting for client connect ");
            System.out.println("on " + host + ":" + port + "; listening=" + listening + "...");
        }
        // listen to port and start a new client server
        while (listening) {
            try {
                if (debug) {
                    System.out.println(prg + ": serverSocket accepting connections");
                }
                Socket s = serverSocket.accept();
                s.setSoTimeout(timeout);  // need to catch SocketTimeout at read
                if (debug) {
                    System.out.println(prg + ": someone connected");
                }
                (new ClientComponentThread(ssi, s, tg)).start();
            } catch (Exception e) {
                System.err.println(prg + ": Exception on connecting client:");
                System.err.println(e);
            }
            try {
                Thread.sleep(timeout);
            } catch (Exception ignore) {
            }
        }
    }

    public void start() {
        if (debug) {
            System.out.println(prg + ": in start");
        }
        listening = true;
        //this.run();
    }

    public void close() {
        try {
            // close the server socket
            serverSocket.close();
        } catch (IOException e) {
            System.err.println(prg + ": IO Exception on closing server socket:");
            System.err.println(e);
        }
    }

    /** Stop the server thread */
    public void stopListening() {
        listening = false;
    }
}
