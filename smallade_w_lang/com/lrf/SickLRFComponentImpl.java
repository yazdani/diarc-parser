/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 * Last update: April 2010
 *
 * SickLRFComponentImpl.java
 *
 * @author Paul Schermerhorn
 */
package com.lrf;

import com.LaserScan;
import gnu.io.*; // if java can't find it, make sure to include RXTXcomm.jar, which you can obtain by unzipping one of the tar.gz files in the rxtx folder in the ADE home folder.
import java.io.*;
import java.rmi.*;
import static utilities.Util.*;

/** 
 * <code>SickLRFComponentImpl</code> interface for Sick LRF.
 */
public class SickLRFComponentImpl extends LRFComponentImpl implements SickLRFComponent {

    private CommPortIdentifier portIdentifier;
    private SerialPort port;
    private InputStream in;
    private OutputStream out;
    private Reader r = null;
    private byte[] buf;

    /* Command Packets */
    // Set range/resolution
    private byte[] CMD_BAUD = {(byte) 0x02, (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x20, (byte) 0x40, (byte) 0x50, (byte) 0x08};
    private byte[] CMD_MODE = {(byte) 0x02, (byte) 0x00, (byte) 0x0a, (byte) 0x00, (byte) 0x20, (byte) 0x00, (byte) 0x53, (byte) 0x49, (byte) 0x43, (byte) 0x4b, (byte) 0x5f, (byte) 0x4c, (byte) 0x4d, (byte) 0x53, (byte) 0xbe, (byte) 0xc5};
    private byte[] CMD_RES = {(byte) 0x02, (byte) 0x00, (byte) 0x05, (byte) 0x00, (byte) 0x3b, (byte) 0xb4, (byte) 0x00, (byte) 0x64, (byte) 0x00, (byte) 0x97, (byte) 0x49};
    private byte[] CMD_START = {(byte) 0x02, (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x20, (byte) 0x24, (byte) 0x34, (byte) 0x08};
    private byte[] CMD_STOP = {(byte) 0x02, (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x20, (byte) 0x25, (byte) 0x35, (byte) 0x08};
    private byte[] CMD_RESET = {(byte) 0x02, (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x20, (byte) 0x42, (byte) 0x52, (byte) 0x08};
    private byte[] CMD_INIT = {(byte) 0x02, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x10, (byte) 0x34, (byte) 0x12};
    private byte[] ACK_CMD = {(byte) 0x02, (byte) 0x80, (byte) 0x03, (byte) 0x00, (byte) 0xa0, (byte) 0x00, (byte) 0x10, (byte) 0x16, (byte) 0x0a};
    private byte[] ACK_RES = {(byte) 0x02, (byte) 0x80, (byte) 0x07, (byte) 0x00, (byte) 0xbb, (byte) 0x01, (byte) 0x64, (byte) 0x00, (byte) 0x64, (byte) 0x00, (byte) 0x10, (byte) 0x5b, (byte) 0x30};

    /** The server is always ready to provide its service after it has come up */
    @Override
    protected boolean localServicesReady() {
        return true;
    }

    /**
     * Constructor for SickLRFComponentImpl.
     */
    public SickLRFComponentImpl() throws RemoteException {
        super(181, Math.PI);
        
        //numReadings = 181; set in super class constructor now
        readings = new double[numReadings];
        history = new int[numReadings];
        
        if (isLive()) {
        	initLive();
        } else {
        	// nothing.
        }
    }

    private void initLive() {
    	// MS: moved this from main, please check
        try {
            Class.forName("gnu.io.NoSuchPortException");
        } catch (ClassNotFoundException e) {
            System.out.println("\nERROR: Can't find gnu.io classes; is RXTXcomm.jar in the cpasspath?");
            System.exit(0);
        }
        System.setProperty("gnu.io.rxtx.SerialPorts", portName);

        buf = new byte[4096];

        try {
            portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
        } catch (Exception e) {
            System.err.println(prg + ": can't find " + portName + ": " + e);
        }
        if (portIdentifier.isCurrentlyOwned()) {
            System.err.println(prg + ": port " + portName + " already in use!");
            System.exit(1);
        }
        try {
            port = (SerialPort) portIdentifier.open(prg, 2000);
        } catch (Exception e) {
            System.err.println(prg + ": Error opening port: " + e);
            System.exit(1);
        }
        if (verbose) {
            System.out.println(prg + ": setting port to 9600 baud");
        }
        try {
            port.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
            port.setInputBufferSize(1024);
        } catch (Exception ucoe) {
            System.err.println(prg + ": " + ucoe);
            System.exit(1);
        }
        if (verbose) {
            System.out.println(prg + ": setting up streams");
        }
        try {
            in = port.getInputStream();
            //inBufSize = port.getInputBufferSize();
            out = port.getOutputStream();
            //port.notifyOnDataAvailable(true);
        } catch (IOException ioe) {
            System.err.println(prg + ": unable to set up streams: " + ioe);
            System.exit(1);
        }
        // INIT not a good idea...
        //command(CMD_INIT, ACK_CMD);
        //Sleep(10000);
        // SICK: 38400 baud
        if (verbose) {
            System.out.println(prg + ": setting Sick to 38400 baud");
        }
        if (command(CMD_BAUD, ACK_CMD) == -1) {
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println(prg + ": attempting to reset LRF, please wait.");
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            try {
                port.setSerialPortParams(38400, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
                port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
                port.setInputBufferSize(1024);
            } catch (UnsupportedCommOperationException ucoe) {
                System.err.println(prg + ": " + ucoe);
                System.exit(1);
            }
            if (verbose) {
                System.out.println(prg + ": sending INIT.");
            }
            command(CMD_INIT, ACK_CMD);
            if (verbose) {
                System.out.println(prg + ": waiting for INIT to complete.");
            }
            Sleep(20000);
            if (verbose) {
                System.out.println(prg + ": setting port to 9600 baud.");
            }
            try {
                port.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
                port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
                port.setInputBufferSize(1024);
            } catch (Exception ucoe) {
                System.err.println(prg + ": " + ucoe);
                System.exit(1);
            }
            if (verbose) {
                System.out.println(prg + ": setting Sick to 38400 baud");
            }
            command(CMD_BAUD, ACK_CMD);
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println(prg + ": LRF should be reset, continuing.");
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        }
        // PORT: 38400 baud
        if (verbose) {
            System.out.println(prg + ": setting port to 38400 baud");
        }
        try {
            port.setSerialPortParams(38400, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
            port.setInputBufferSize(1024);
        } catch (UnsupportedCommOperationException ucoe) {
            System.err.println(prg + ": " + ucoe);
            System.exit(1);
        }
        // Set range and resolution
        if (verbose) {
            System.out.println(prg + ": setting range/resolution");
        }
        command(CMD_RES, ACK_RES);
        // Send password
        if (verbose) {
            System.out.println(prg + ": sending password");
        }
        command(CMD_MODE, ACK_CMD);
        // Set units?
        // Startup
        if (verbose) {
            System.out.println(prg + ": starting");
        }
        command(CMD_START, ACK_CMD);
        r = new Reader();
        r.start();
	}

	private short checksum(byte[] buffer, int length) {
        short uCrc16;
        byte[] abData = new byte[2];
        int uLen = length - 2;
        int bufferCounter = 0;

        uCrc16 = 0;
        abData[0] = 0;

        while (uLen-- > 0) {
            abData[1] = abData[0];
            abData[0] = buffer[bufferCounter++];
            if ((uCrc16 & 0x8000) != 0) {
                uCrc16 = (short) ((uCrc16 & 0x7FFF) << 1);
                uCrc16 ^= 0x8005;
            } else {
                uCrc16 <<= 1;
            }
            uCrc16 ^= toShort(abData[1], abData[0]);
        }
        if (vverbose) {
            System.out.format("CRC: %02X %02X %d\n", (byte) (uCrc16 & 0xFF), (byte) (uCrc16 << 16 >>> 24), uCrc16 & 0xFFFF);
        }
        return uCrc16;
    }

    protected static short toShort(byte a, byte b) {
        return (short) (((a & 0xFF) << 8) | (b & 0xFF));
    }

    private short command(byte[] msg, byte[] ack) {
        short t = 0;

        // Calculated checksums statically
        //short cs = checksum(msg, msg.length);
        //msg[msg.length - 2] = (byte)(cs & 0xFF);
        //msg[msg.length - 1] = (byte)(cs << 16 >>> 24);
        try {
            if (vverbose) {
                System.out.print("Writing: ");
                for (int i = 0; i < msg.length; i++) {
                    System.out.format("%02X ", (short) (msg[i] & 0xff));
                }
                System.out.println();
            }
            out.write(msg, 0, msg.length);
            //while ((byte)(t & 0xff) != ack[4])
            t = read(true);
        } catch (IOException ioe) {
            System.err.println(prg + ": exception sending to receiver: " + ioe);
            return 0;
        }
        return t;
    }

    private short read(boolean ack) {
        int a = -1;
        int b, len = 0;
        short f, s, t;
        long start = System.currentTimeMillis();
        int j = 0;

        //Sleep(100);
        try {
            if (ack) {
                while ((a = in.available()) < 2) {
                    Sleep(10);
                    j++;
                    if (j > 100) {
                        j = 0;
                        if ((System.currentTimeMillis() - start) > 3000) {
                            return -1;
                        }
                    }
                }
                f = (short) (in.read() & 0xFF);
                s = (short) (in.read() & 0xFF);
                while ((f != 0x02) || (s != 0x80)) {
                    f = s;
                    while ((a = in.available()) < 1) {
                        Sleep(10);
                    }
                    s = (short) (in.read() & 0xFF);
                    j++;
                    if (j > 100) {
                        j = 0;
                        if ((System.currentTimeMillis() - start) > 3000) {
                            return -1;
                        }
                    }
                }
            } else {
                while ((a = in.available()) < 3) {
                    Sleep(10);
                }
                f = (short) (in.read() & 0xFF);
                s = (short) (in.read() & 0xFF);
                t = (short) (in.read() & 0xFF);
                while ((f != 0x06) || (s != 0x02) || (t != 0x80)) {
                    f = s;
                    s = t;
                    while ((a = in.available()) < 1) {
                        Sleep(10);
                    }
                    t = (short) (in.read() & 0xFF);
                }
                f = s;
                s = t;
            }
            buf[0] = (byte) (f & 0xff);
            buf[1] = (byte) (s & 0xff);
            while ((a = in.available()) < 2) {
                Sleep(10);
            }
            f = (short) (in.read() & 0xFF);
            s = (short) (in.read() & 0xFF);
            buf[2] = (byte) (f & 0xff);
            buf[3] = (byte) (s & 0xff);
            len = f + (s << 8) + 2;
            while ((a = in.available()) < len) {
                Sleep(10);
            }
            //System.out.format("Read byte: %02X\n", s);
            a = in.read(buf, 4, len);
            len += 4;
            if (vverbose) {
                System.out.print("Reading: ");
                for (int i = 0; i < len; i++) {
                    System.out.format("%02X ", (short) (buf[i] & 0xff));
                }
                System.out.println();
                System.out.print("Read ");
                checksum(buf, len);
            }
            t = buf[4];
        } catch (IOException ioe) {
            System.err.println(prg + ": error reading: " + ioe);
            t = 0;
        }
        return t;
    }

    @Override
    public LaserScan getLaserScan() throws RemoteException {
        // TODO
        return null;
    }

    /**
     * The <code>Reader</code> is the main loop that queries the Sick.
     */
    private class Reader extends Thread {

        private boolean shouldRead;

        public Reader() {
            shouldRead = true;
        }

        @Override
        public void run() {
            int num, val, o;

            while (shouldRead) {
                read(true);
                num = (buf[5] & 0xff) + ((buf[6] & 0x1f) << 8);
                if (vverbose) {
                    System.out.println(prg + ": " + num + " readings");
                }
                for (int i = 0; i < num; i++) {
                    o = 2 * i;
                    val = (buf[7 + o] & 0xff) + ((buf[8 + o] & 0x3f) << 8);
                    readings[i] = (double) val / 1000;
                    if (vverbose) {
                        System.out.print(readings[i] + " ");
                    }
                }
                if (vverbose) {
                    System.out.println();
                }
                
                //set indices of right/front/left regions and update laser info
                int oneThird = 2*numReadings/6;
                int twoThird = 4*numReadings/6;
                updateLRF(0, oneThird-1, oneThird, twoThird-1, twoThird, numReadings-1, 0, -1);
                
                Sleep(50);
            }
            System.out.println(prg + ": Exiting Reader thread...");
        }

        public void halt() {
            System.out.println(prg + ": Halting read thread...");
            shouldRead = false;
        }
    }

    /**
     * Shut down the LRF.
     */
    @Override
    public void shutdown() {
        int a = 0;
        if (r != null) {
            r.halt();
        }
        Sleep(1000);
        // try to drain the input stream
        try {
            while ((a = in.available()) > 0) {
                in.read();
            }
        } catch (IOException ioe) {
        }
        if (verbose) {
            System.out.println(prg + ": stopping");
        }
        command(CMD_STOP, ACK_CMD);
        Sleep(1000);
        if (verbose) {
            System.out.println(prg + ": resetting");
        }
        command(CMD_RESET, ACK_CMD);
        try {
            out.flush();
        } catch (IOException ioe) {
            System.err.println(prg + ": Error flushing output stream: " + ioe);
        }
        Sleep(1000);
        try {
            out.close();
        } catch (IOException ioe) {
            System.err.println(prg + ": Error closing output stream: " + ioe);
        }
    }

    // TODO: the main loop needs to use these...
    // nothing special for now
    @Override
    protected void updateComponent() {
    }
    // nothing special for now

    @Override
    protected void updateFromLog(String logEntry) {
    }

    /**
     * <code>main</code> passes the arguments up to the ADEComponentImpl
     * parent.  The parent does some magic and gets the system going.
     *
    public static void main(String[] args) throws Exception {
        // Check for RXTX here so we're able to give a hint before the
        // problems manifest.
        try {
            Class.forName("gnu.io.NoSuchPortException");
        } catch (ClassNotFoundException e) {
            System.out.println("\nERROR: Can't find gnu.io classes; is RXTXcomm.jar in the cpasspath?");
            System.exit(0);
        }
        System.setProperty("gnu.io.rxtx.SerialPorts", portName);
        LRFComponentImpl.main(args);
    }
     */
}

