/**
 * ADE 1.0
 * (c) copyright HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@gmail.com
 * Last update: April 2010
 *
 * SocketComponentImpl.java
 */
package com;

import java.io.*;
import java.net.*;
import java.rmi.*;
import ade.ADEComponentImpl;

/**
The superclass for ADEComponents that communicate over sockets with an external
process started from within this server.
 */
abstract public class SocketComponentImpl extends ADEComponentImpl {

    private static String prg = "SocketComponentImpl";
    // levels of runtime output
    protected boolean verbose = false;
    protected boolean verbose2 = false;
    private boolean debug = false;
    // the external process info and socket support for communicating with it
    protected Process myProc;
    protected ThreadGroup tg = new ThreadGroup(prg);
    protected ClientComponent cs;
    protected Socket mySocket;
    // provide default of failed updates for recovery and tracking hash
    protected static final int DEF_NUMFAIL = 2;
    //protected int failureMax;
    //protected Hashtable failedUpdates;
    //protected int failedUpdatesHB;
    // port on which to connect
    protected static int sockPort = -1;
    // whether we set up a server socket
    protected static boolean actAsComponent = false;
    // the arguments passed to "exec"
    protected static String[] cmd;
    // MS: added this a local argument because the one-arg constructor cannot be used...
    protected static boolean startit = true;
    // MS: added this as an option to use either localhost or the set host interface
    // for processes that connect from other hosts in server mode
    protected static boolean uselocalhost = true;
    
    // MUST BE IMPLEMENTED IN SUBCLASS!
    abstract public void update(Object o);

    abstract public SocketHandler getHandler(Socket s);
    //abstract public void createComponentSockets();  // on construction, start server sockets

    // ADE constructor (MS: removed constructor with one arg because it was useless)
    public SocketComponentImpl() throws RemoteException {
        super();
        // store external process info and start server socket

        if (actAsComponent) {
            // we're the server; start up a server thread, then start the
            // external process
            try {
                if (verbose) {
                    System.out.println("Creating ClientComponent...");
                }
		// MS: added this an as option
		if (uselocalhost)
		    cs = new ClientComponent(this, "127.0.0.1", sockPort, 1, tg);
		else
		    cs = new ClientComponent(this, getHostMe(), sockPort, 1, tg);
                if (verbose) {
                    System.out.println("Starting ClientComponent..." + getHostMe());
                }
                //cs.run();
                new Thread(cs).start();
                if (verbose) {
                    System.out.println("Done starting ClientComponent...");
                }
            } catch (IOException ioe) {
                System.err.println("Error starting ClientComponent:");
                System.err.println(ioe);
                System.exit(1);
            }
            if (verbose) {
                System.out.println("Starting process...");
            }
            if (startit) {
                myProc = startProcess();
            }
        } else {
            // we're a client, start the process; try to connect back in subclass
            if (startit) {
                //System.out.println("Starting process...");
                myProc = startProcess();
            }
        }
        // need storage for recording failed updates; key=uid, value=int
        //failedUpdates = new Hashtable();
        //failedUpdatesHB = 0;
        //failureMax = DEF_NUMFAIL;
        //updater = new Updater(50);
        //updater.start();
        if (verbose) {
            System.out.println(prg + ": leaving constructor...");
        }
    }

    public Process startProcess() {
        Process p = null;

        if (debug) {
            System.out.println(prg + ": starting " + cmd);
        }
        try {
            Runtime r = Runtime.getRuntime();
            p = r.exec(cmd);
        } catch (SecurityException se) {
            System.err.println("Not allowed to start process:");
            System.err.println(se);
            System.exit(1);
        } catch (IOException ioe) {
            System.err.println("IO error starting process:");
            System.err.println(ioe);
            System.exit(1);
        } catch (NullPointerException npe) {
            System.err.println("Null command starting process:");
            System.err.println(npe);
            System.exit(1);
        } catch (IllegalArgumentException iae) {
            System.err.println("Empty command starting process:");
            System.err.println(iae);
            System.exit(1);
        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }

        // allow some time for the process to start up
        try {
            Thread.sleep(500);
        } catch (Exception ignore) {
        }
        if (verbose) {
            System.out.println(prg + ": returning " + p);
        }
        return p;
    }

    /** Opens a socket connection to a program listening on the specified
     * host and port. Used when the external process acts as server to
     * which this needs to connect.
     * @param sHost the host on which the server is executing
     * @param sPort the port on which the server is listening
     * @return <tt>true</tt> on successful connection, <tt>false</tt>
     * otherwise */
    protected boolean initSocket(String sHost, int sPort) {
        boolean conn = false;
        int attempts = 10;
        while (attempts-- > 0) {
            try {
                // start by sleeping; let the process startup before connecting
                Thread.sleep(2000);
                if (debug) {
                    System.out.println("Opening socket to server");
                }
                mySocket = new Socket(sHost, sPort);
                mySocket.setSoTimeout(500);  // need to catch SocketTimeout at read
                //s.setKeepAlive(true);
                (new ClientComponentThread(this, mySocket, tg)).start();
                return true;
            } catch (IOException ioe) {
                StringBuilder sb = new StringBuilder(prg);
                sb.append(": IO Exception creating/opening socket:\n\t");
                sb.append(ioe);
                sb.append("\n\tCommonly caused by application refusing socket connections");
                System.err.println(sb.toString());
            } catch (Exception e) {
                System.err.println(prg + "Exception creating/opening socket:");
                System.err.println(e);
            }
        }
        return false;
    }
}
