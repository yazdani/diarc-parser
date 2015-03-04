/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@gmail.com
 * Last update: April 2010
 *
 * SerialPortComponentImpl.java
 */
package com;

import java.io.*;
import java.rmi.*;
import java.util.*;
//import javax.comm.*;
import gnu.io.*;
import ade.ADEComponentImpl;

/**
The <tt>SerialPortComponentImpl</tt> class extends the <tt>ADEComponentImpl</tt>
class, providing actual hardware interface (i.e., SerialPort)
functionality. In addition, it provides a <tt>heartbeat</tt> internal
class that maintains a "keep-alive" signal with the
<tt>ADERegistry</tt>.

Note that besides the abstract <tt>requestConnection</tt> method
inherited from the <tt>ADEComponentImpl</tt> class, four more abstract
methods are declared: <tt>startUp()</tt>, <tt>updateRobot()</tt>,
<tt>sendReset()</tt>, and <tt>sendOpen()</tt>.
@author Virgil Andronache, Matthias Scheutz, Jim Kramer
 */
abstract public class SerialPortComponentImpl extends ADEComponentImpl {
    // levels of runtime output

    protected boolean verbose = true;
    protected boolean verbose2 = true;
    // following are serial port setting default constants
    protected static final String DEF_WIN_PORTNAME = "COM1";
    protected static final String DEF_UNIX_PORTNAME = "/dev/ttyS0";
    //protected static final String DEF_PORTNAME = DEF_WIN_PORTNAME;
    protected static final String DEF_PORTNAME = DEF_UNIX_PORTNAME;
    protected static final int DEF_WAIT_FOR_PORT_TIMEOUT = 2000;
    protected static final int DEF_BAUD_RATE = 9600;
    // provide default of failed updates for recovery and tracking hash
    protected static final int DEF_NUMFAIL = 2;
    protected int failureMax;
    protected Hashtable failedUpdates;
    protected int failedUpdatesHB;
    // vars to open port
    // static var 'cuz server runs on a single robot
    protected Enumeration portList = CommPortIdentifier.getPortIdentifiers();
    protected CommPortIdentifier portId;
    protected SerialPort serialPort;
    // non-static vars for serial port availability
    protected boolean portOpenOk = false;
    protected OutputStream outputStream;
    protected InputStream inputStream1;
    protected BufferedInputStream inputStream;

    // require the following methods for SerialPortComponent
    abstract public int startUp();

    abstract protected void updateFromSerialPort();

    abstract public void sendReset();

    abstract public void sendOpen();
    // set this in the derived class
    protected static String serialPortName;

    /*
    public Object requestConnection(String uid) throws RemoteException {
    super.requestConnection(uid);

    // store the uid for failure recovery
    failedUpdates.put(uid, new Integer(0));
    return null;
    }
     */
    /**
     * Constructor that uses all defaults (assigned a server name by
     * the ADERegistry, uses the default portname, allow a single user
     * connection, default heartbeat pulse, and no user added to the
     * ADERegistry. */
    public SerialPortComponentImpl() throws RemoteException {
        super();

        if (serialPortName == null) {
            serialPortName = DEF_UNIX_PORTNAME;
        }

        if (verbose2) {
            System.out.println("are there any ports? "
                    + portList.hasMoreElements());
        }

        while (portList.hasMoreElements()) {
            portId = (CommPortIdentifier) portList.nextElement();
            if (verbose2) {
                System.out.println("Checking " + portId.getName());
            }
            if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                //if (portId.getName().equals("COM1") || portId.getName().equals("/dev/ttyS0")) {
                if (portId.getName().equals(serialPortName)) {
                    if (verbose2) {
                        System.out.println("Finding serial port on " + serialPortName);
                    }
                    //open port for I/O
                    try {
                        serialPort = (SerialPort) portId.open("SerialPortComponentApp", DEF_WAIT_FOR_PORT_TIMEOUT);
                        /* if unsuccessful try then this will not execute */
                        if (verbose || verbose2) {
                            System.out.println(portId.getName() + " opened successfully");
                        }
                        portOpenOk = true;
                    } catch (PortInUseException e) {
                        System.err.println("Port in Use -- failure");
                    } catch (Exception e) {
                        System.err.println("Something went wrong with the port...");
                    }

                    try {
                        serialPort.setSerialPortParams(DEF_BAUD_RATE,
                                SerialPort.DATABITS_8,
                                SerialPort.STOPBITS_1,
                                SerialPort.PARITY_NONE);
                        if (verbose || verbose2) {
                            System.out.println(portId.getName() + " parameters set successfully");
                        }
                    } catch (UnsupportedCommOperationException e) {
                        System.err.println("Could not set port parameters -- failure");
                    }
                    try {
                        //write the message to the output
                        outputStream = serialPort.getOutputStream();
                        inputStream1 = serialPort.getInputStream();
                        inputStream = new BufferedInputStream(inputStream1);
                        serialPort.notifyOnDataAvailable(true);
                        if (verbose || verbose2) {
                            System.out.println(portId.getName() + " got streams successfully");
                        }
                    } catch (IOException e) {
                        System.err.println("Could not get streams -- failure");
                    }
                }
            }
        }
        // need storage for recording failed updates; key=uid, value=int
        failedUpdates = new Hashtable();
        failedUpdatesHB = 0;
        failureMax = DEF_NUMFAIL;
    }
}
