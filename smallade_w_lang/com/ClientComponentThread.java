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

/** Class to execute message handler (in response to client request) in a thread. */
class ClientComponentThread extends Thread {

    private static String prg = "ClientComponentThread";
    SocketComponentImpl ssi;
    Socket socket;
    SocketHandler handler;
    private boolean verbose = false;
    private boolean debug = false;

    ClientComponentThread(SocketComponentImpl uc, Socket sock, ThreadGroup tg) throws IOException {
        super(tg, (Runnable) null);
        if (verbose) {
            System.out.println(prg + ": Initializing connection");
        }
        this.ssi = uc;
        this.socket = sock;
        if (verbose) {
            System.out.println(prg + ": initializing ssi");
        }
        this.handler = ssi.getHandler(this.socket);
        if (debug) {
            System.out.println(prg + ": got " + handler + "; leaving constructor");
        }
    }

    void closeConnection() throws IOException {
        if (handler != null) {
            handler.close();
        }
        if (socket != null) {
            socket.close();
        }
    }

    public void run() {
        Object recvd = null, response = null;
        // make sure we have a handler
        if (handler == null) {
            return;
        }
        //if (verbose) System.out.println(prg +": starting, type="+ type);
        if (debug) {
            System.out.println(prg + ": starting; socket is " + socket);
        }
        for (;;) {
            try {
                // get the message from the client and analyze it
                if ((recvd = handler.receiveMessage()) == null) {
                    //handler.setOpen(false);
                    System.out.println(prg + ": broken socket connection");
                    break;
                    //continue;
                } else {
                    if (!recvd.equals("")) {
                        handler.parseMessage(recvd);
                    }
                    handler.sendMessage();
                }
                try {
                    Thread.sleep(100);
                } catch (Exception ignore) {
                }
            } catch (IOException ioe) {
                System.err.println(prg + ": run error:");
                System.err.println(ioe);
                ioe.printStackTrace();
                //temporary...
                System.exit(1);
            }
        }
        System.out.println(prg + ": leaving run method");
        try {
            closeConnection();
        } catch (Exception e) {
            System.err.println("Error closing socket:");
            System.err.println(e);
        }
    }
}
