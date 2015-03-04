/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * UrgLRFComponentImpl.java
 *
 * @author Paul Schermerhorn
 */
package com.lrf;

import com.LaserScan;
import gnu.io.*;
import java.io.*;
import java.rmi.*;
import static utilities.Util.*;

/**
 * <code>UrgLRFComponentImpl</code> interface for Hokuyo Urg LRF.
 */
public class UrgLRFComponentImpl extends LRFComponentImpl implements UrgLRFComponent {

    private CommPortIdentifier portIdentifier;
    private SerialPort port;
    private InputStream in;
    private OutputStream out;
    private Reader r;
    private byte[] buf;
    private byte[] zero;
    private final int frontReadings; // number of readings in front 180 degrees
    private final double readingRadians; // radians per reading
    private int oneThird;
    private int twoThird;
    private int threeThird;
    private double MINDIST = 0.06;
    private double MAXDIST = 4.095;

    /* Command Packets */
    // SCIP 1.1 version info request
    private byte[] CMD_VERS = {(byte) 0x56, (byte) 0x0A};
    // SCIP 2.0 version info request
    private byte[] CMD2_VERS = {(byte) 0x56, (byte) 0x56, (byte) 0x0A};
    // SCIP 1.1 command to change to SCIP 2.0 mode
    private byte[] CMD_MODE = {(byte) 0x53, (byte) 0x43, (byte) 0x49, (byte) 0x50, (byte)0x32, (byte) 0x2E, (byte) 0x30, (byte) 0x0A};

    // SCIP 2.0 state request
    private byte[] CMD2_INFO = {(byte) 0x49, (byte) 0x49, (byte) 0x0A};
    //
    // SCIP 1.1 read (181 readings, actually about 190 degrees)
    private byte[] CMD_READ = {(byte) 0x47, (byte) 0x31, (byte) 0x31, (byte) 0x33, (byte) 0x36, (byte) 0x35, (byte) 0x35, (byte) 0x30, (byte) 0x33, (byte) 0x0A};
    // SCIP 2.0 read (512 readings, 180 degrees)
    private static byte[] CMD2_READ_180 = {(byte) 0x47, (byte) 0x53, (byte) 0x30, (byte) 0x31, (byte) 0x32, (byte) 0x39, (byte) 0x30, (byte) 0x36, (byte) 0x34, (byte) 0x30, (byte) 0x30, (byte) 0x31, (byte) 0x0A};
    // SCIP 2.0 read (682 readings, 240 degrees)
    private static byte[] CMD2_READ_240 = {(byte) 0x47, (byte) 0x53, (byte) 0x30, (byte) 0x30, (byte) 0x34, (byte) 0x34, (byte) 0x30, (byte) 0x37, (byte) 0x32, (byte) 0x35, (byte) 0x30, (byte) 0x31, (byte) 0x0A};
    // SCIP 2.0 laser enable command
    private byte[] CMD2_ENBL = {(byte) 0x42, (byte) 0x4D, (byte) 0x0A};
    // SCIP 2.0 laser disable command
    private byte[] CMD2_DSBL = {(byte) 0x51, (byte) 0x54, (byte) 0x0A};
    // SCIP 2.0 high sensitivity command
    private byte[] CMD2_SENS = {(byte) 0x48, (byte) 0x53, (byte) 0x31, (byte) 0x0A};
    // SCIP 2.0 reset command
    private byte[] CMD2_RSET = {(byte) 0x52, (byte) 0x53, (byte) 0x0A};

    private static byte[] cmd_read = CMD2_READ_240;
    private static int localNumReadings = 682;
    private static double localScanAngle = Math.toRadians(240.0);
    //private static byte[] cmd_read = CMD2_READ_180;
    //private static int localNumReadings = 512;
    //private static double localScanAngle = Math.toRadians(180.0);

    /** The server is always ready to provide its service after it has come up */
    @Override
    protected boolean localServicesReady() {
        return true;
    }

    /**
     * Constructor for UrgLRFComponentImpl.
     */
    public UrgLRFComponentImpl() throws RemoteException {
        super(localNumReadings, localScanAngle);
        // parseadditionalargs now called in ADEComponentImpl constructor, so the values
        // passed in to super above do not reflect command-line parameters
        if (localNumReadings != numReadings) {
            numReadings = localNumReadings;
            scanAngle = localScanAngle;
new Thread("starter") {
        @Override
            public void run(){
            detector = new LaserFeatureDetector(numReadings, scanAngle, offset);
    }
    }.start();
        }
        
        final int centerReading = numReadings / 2;
        readingRadians = scanAngle / numReadings;
        // get the number of readings for the front 180 degrees
        frontReadings = (int)Math.round(Math.PI / readingRadians);
new Thread("starter") {
        @Override
            public void run(){
        // get the index of the first front reading
        eastReading = centerReading - (int)Math.round(frontReadings / 2.0);
        if (CRITICAL_DIST > 0.75) {
            oneThird = eastReading + frontReadings/3;
            twoThird = eastReading + 2*frontReadings/3;
            threeThird = eastReading + frontReadings;
        } else {
            oneThird = eastReading + frontReadings/6;
            twoThird = eastReading + 5*frontReadings/6;
            threeThird = eastReading + frontReadings;
        }

        // Search for port if it hasn't been specified
        if (!userPort) {
            portName = "/dev/ttyACM";
            int devNum = 0;
            while (devNum < 10) {
                if (new File(portName + devNum).exists()) {
                    portName = portName + devNum;
                    break;
                }
                devNum++;
            }
        }
        // Check for RXTX here so we're able to give a hint before the
        // problems manifest.
        try {
            Class.forName("gnu.io.NoSuchPortException");
        } catch (ClassNotFoundException e) {
            System.out.println("\nERROR: Can't find gnu.io classes; is RXTXcomm.jar in the cpasspath?");
            System.exit(0);
        }

        readings = new double[numReadings];
        history = new int[numReadings];
        buf = new byte[4096];
        zero = new byte[4096];

        // ACM devices not normally isVisible to gnu.io, so setting here.
        System.setProperty("gnu.io.rxtx.SerialPorts", portName);
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
        try {
            in = port.getInputStream();
            //inBufSize = port.getInputBufferSize();
            out = port.getOutputStream();
            //port.notifyOnDataAvailable(true);
        } catch (IOException ioe) {
            System.err.println(prg + ": unable to set up streams: " + ioe);
            System.exit(1);
        }
        command(CMD_VERS);
        read();
        read();
        System.out.format("buf[1]: %02X\n", (short) (buf[1] & 0xff));
        if (buf[1] == (byte) 0x0A) {
            // looks like a SCIP 1.0 version response
            for (int i = 0; i < 7; i++) {
                read();
                System.out.println((new String(buf)).trim());
            }
            // switch mode to SCIP 2.0
            command(CMD_MODE);
            for (int i = 0; i < 3; i++) {
                read();
                System.out.println((new String(buf)).trim());
            }
        } else {
            // already in SCIP 2.0 mode
            read();
        }
        command(CMD2_VERS);
        for (int i = 0; i < 8; i++) {
            read();
            System.out.println((new String(buf)).trim());
        }
        /*
        command(CMD2_INFO);
        for (int i = 0; i < 9; i++) {
            read();
            System.out.println((new String(buf)).trim());
        }
        */
        command(CMD2_SENS);
        for (int i = 0; i < 3; i++) {
            read();
            System.out.println((new String(buf)).trim());
        }
        command(CMD2_ENBL);
        for (int i = 0; i < 3; i++) {
            read();
            System.out.println((new String(buf)).trim());
        }

        r = new Reader();
        r.start();
    }
    }.start();
    }

    private void command(byte[] msg) {
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
        } catch (IOException ioe) {
            System.err.println(prg + ": exception sending to receiver: " + ioe);
        }
    }

    private int read() {
        int a = -1;
        int i = 0;
        byte b = (byte) 0x00;

        //Sleep(100);
        System.arraycopy(zero, 0, buf, 0, 4096);
        while (b != (byte) 0x0a) {
            try {
                while ((a = in.available()) < 1) {
                    Sleep(10);
                }
                //System.out.format("Read byte: %02X\n", s);
                a = in.read(buf, i, 1);
                b = buf[i++];
            } catch (IOException ioe) {
                System.err.println(prg + ": error reading: " + ioe);
            }
        }
        if (i > 1) i--; // not checking checksum yet
        if (vverbose) {
            System.out.print("Reading: ");
            for (int j = 0; j < i; j++) {
                System.out.format("%02X ", (short) (buf[j] & 0xff));
            }
            System.out.println();
        }
        return i;
    }

    /**
     * The <code>Reader</code> is the main loop that queries the Urg.
     */
    private class Reader extends Thread {

        private boolean shouldRead;

        public Reader() {
            shouldRead = true;
        }

        @Override
        public void run() {
            int num = 0, val, r;

            while (shouldRead) {
                num = 0;
                command(cmd_read);
                read(); // echo
                if (vverbose) {
                    System.out.print("Echo: ");
                    System.out.println(new String(buf));
                }
                read(); // status
                if (vverbose) {
                    System.out.print("Status: ");
                    System.out.println(new String(buf));
                }
                r = read();
                if (r == 1) {
                    // error in previous status
                    Sleep(100);
                    continue;
                } else if (vverbose) {
                    System.out.print("Timestamp: ");
                    System.out.println(new String(buf));
                }
                while ((r = read()) > 1) {
                    for (int i = 0; i < (r - 1); i = i + 2) {
                        val = ((buf[i] - 0x30) << 6) + (buf[i + 1] - 0x30);
                        if (val < 20) {
                            //System.out.println("reading["+(num+1)+"]: "+val);
                            //System.out.print(val);
                            val = 4096;
                            if (num>0 && false) {
                                int x=num;
                                if (readings[x] == 0.0) {
                                    readings[num++] = (double) val * 0.001;
                                } else {
                                    readings[num++] = readings[x];
                                }
                            } else {
                                readings[num++] = (double) val * 0.001;
                            }
                        } else {
                            readings[num++] = (double) val * 0.001;
                        }
                    }
                }
                if (verbose) {
                    System.out.println();
                    System.out.println("Got " + num + " readings");
                    for (int i = 0; i < num; i++) {
                        System.out.format("%1.4f ", readings[i]);
                    }
                    System.out.println();
                }

                //set indices of right/front/left regions and update laser info
                updateLRF(eastReading, oneThird-1, oneThird, twoThird-1, twoThird, threeThird-1, 0, -1);

                //Sleep(1000);
                Sleep(100);
            }
            System.out.println(prg + ": Exiting Reader thread...");
        }

        public void halt() {
            System.out.println(prg + ": Halting read thread...");
            shouldRead = false;
        }
    }

    // TODO: the main loop needs to use these...
    // nothing special for now
    @Override
    protected void updateComponent() {
    }
    // nothing special for now

    //@Override
    //protected void updateFromLog(String logEntry) {
    //}

    /**
     * Shut down the LRF.
     */
    @Override
    public void shutdown() {
        r.halt();
        Sleep(500);
        command(CMD2_DSBL);
        for (int i = 0; i < 3; i++) {
            read();
            System.out.println((new String(buf)).trim());
        }
        command(CMD2_RSET);
        for (int i = 0; i < 3; i++) {
            read();
            System.out.println((new String(buf)).trim());
        }
    }

    /** 
     * Parse additional command-line arguments
     * @return "true" if parse is successful, "false" otherwise 
     */
    protected boolean parseadditionalargs(String[] args) {
        boolean found = true;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-180")) {
                args[i] = "";
                localNumReadings = 512;
                localScanAngle = Math.toRadians(180);
                cmd_read = CMD2_READ_180;
            } else {
                //System.out.println("Unrecognized argument: " + args[i]);
                found = false;  // return on any unrecognized args
            }
        }
        if (! found) {
            found = super.parseadditionalargs(args);
        }
        return found;
    }

    /**
     * Set the distance that robot takes to be the distance at which obstacle
     * avoidance should engage.
     * @param dist the new critical distance
     */
    public void setCritDist(double dist) throws RemoteException {
        CRITICAL_DIST = dist;
        System.out.println("LRF: setting crit dist to: " + CRITICAL_DIST);
        if (CRITICAL_DIST > 0.75) {
            oneThird = eastReading + frontReadings/3;
            twoThird = eastReading + 2*frontReadings/3;
            threeThird = eastReading + frontReadings;
        } else {
            oneThird = eastReading + frontReadings/6;
            twoThird = eastReading + 5*frontReadings/6;
            threeThird = eastReading + frontReadings;
        }
    }

    @Override
    public LaserScan getLaserScan() throws RemoteException {
        LaserScan ls = new LaserScan();
    	ls.angleMin = 0;
    	ls.angleMax = localScanAngle;
    	ls.angleIncrement = (ls.angleMax - ls.angleMin) / localNumReadings;
    	ls.rangeMin = MINDIST;
    	ls.rangeMax = MAXDIST;
    	ls.ranges = this.readings;
        return ls;
    }
}
