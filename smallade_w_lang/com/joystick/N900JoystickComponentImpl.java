/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * N900JoystickComponentImpl.java
 *
 * @author Paul Schermerhorn
 */
package com.joystick;

import ade.ADEException;
import ade.*;
import java.io.*;
import java.rmi.*;
import static utilities.Util.*;

/** <code>N900JoystickComponentImpl</code> takes input from the accelerometer
 * on a Nokia N900, translates and passes on velocity commands to servers
 * implementing the {@link com.interfaces.VelocityComponent VelocityComponent}
 * interface.
 */
public class N900JoystickComponentImpl extends ADEComponentImpl implements N900JoystickComponent {
    /* ADE-related fields (pseudo-refs, etc.) */

    private static String prg = "com.joystick.N900JoystickComponentImpl";
    private static String type = "com.joystick.N900JoystickComponent";
    private static boolean verbose = false;
    private static boolean useVel = true;
    private static boolean gotVel = false;
    private static String velVersion = "com.interfaces.VelocityComponent";
    private Object vel;

    /* Joystick-specific fields */
    private static String devName = "/sys/class/i2c-adapter/i2c-3/3-001d/coord";
    private RandomAccessFile dev;
    private short value = 0;
    private short event = 0;
    private short input = 0;
    private Boolean update = false;
    private short[] values = new short[3];
    private static double[] defaultVels;
    private Reader r;
    private Writer w;
    private boolean status = false;
    private static boolean altSteer = true;
    private static double defTV = 0.0;
    private static double defRV = 0.0;

    // ***********************************************************************
    // *** Abstract methods in ADEComponentImpl that need to be implemented
    // ***********************************************************************
    /**
     * This method will be activated whenever a client calls the
     * requestConnection(uid) method. Any connection-specific initialization
     * should be included here, either general or user-specific.
     */
    protected void clientConnectReact(String user) {
        System.out.println(myID + ": got connection from " + user + "!");
        return;
    }

    /**
     * This method will be activated whenever a client that has called the
     * requestConnection(uid) method fails to update (meaning that the
     * heartbeat signal has not been received by the reaper), allowing both
     * general and user specific reactions to lost connections. If it returns
     * true, the client's connection is removed.
     */
    protected boolean clientDownReact(String user) {
        System.out.println(myID + ": lost connection with " + user + "!");
        return false;
    }

    /**
     * This method will be activated whenever the heartbeat returns a
     * remote exception (i.e., the server this is sending a
     * heartbeat to has failed). 
     */
    protected void componentDownReact(String serverkey, String[][] constraints) {
        String s = constraints[0][1];

        if (s.indexOf(velVersion) >= 0) {
            gotVel = false;
        }
        return;
    }

    /** This method will be activated whenever the heartbeat reconnects
     * to a client (e.g., the server this is sending a heartbeat to has
     * failed and then recovered). <b>Note:</b> the pseudo-reference will
     * not be set until <b>after</b> this method is executed. To perform
     * operations on the newly (re)acquired reference, you must use the
     * <tt>ref</tt> parameter object.
     * @param s the ID of the {@link ade.ADEComponent ADEComponent} that connected
     * @param ref the pseudo-reference for the requested server */
    protected void componentConnectReact(String serverkey, Object ref, String[][] constraints) {
        String s = constraints[0][1];

        if (s.indexOf(velVersion) >= 0) {
            gotVel = true;
        }
        return;
    }

    /**
     * Adds additional local checks for credentials before allowing a shutdown
     * must return "false" if shutdown is denied, true if permitted
     */
    protected boolean localrequestShutdown(Object credentials) {
        return false;
    }

    /** The server is always ready to provide its service after it has come up */
    protected boolean localServicesReady() {
        return true;
    }

    /**
     * Implements the local shutdown mechanism that derived classes need to
     * implement to cleanly shutdown
     */
    protected void localshutdown() {
        System.out.print("\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.print(prg + " shutting down...");
        //finalize();
        r.halt();
        w.halt();
        Sleep(100);
        if (useVel) {
            try {
                call(vel, "setVels", 0.0, 0.0);
            } catch (ADEException ace) {
                System.err.println(prg + ": Error setting vels: " + ace);
            }
        }
        System.out.println("done.");
    }

    // ***********************************************************************
    // Methods available to remote objects via RMI
    // ***********************************************************************
    /**
     * Get the most recently-read values.
     * @return an array of the values
     */
    public short[] getValues() throws RemoteException {
        // PWS: might want to synchronize access to values
        return values;
    }

    // ********************************************************************
    // *** Local methods
    // ********************************************************************
    /** 
     * N900JoystickComponentImpl constructor.
     */
    public N900JoystickComponentImpl() throws RemoteException {
        super();

        // Get reference to VelocityComponent, if requested
        if (useVel) {
            vel = getClient(velVersion);
            while (!gotVel) {
                Sleep(100);
            }
            try {
                defaultVels = (double[]) call(vel, "getDefaultVels");
            } catch (ADEException ace) {
                System.err.println(prg + ": Error getting def vels: " + ace);
                defaultVels = new double[2];
            }
            if (defTV != 0.0) {
                defaultVels[0] = defTV;
            }
            if (defRV != 0.0) {
                defaultVels[1] = defRV;
            }
            System.out.println("defaultVels: " + defaultVels[0] + " " + defaultVels[1]);
        }
        // Thread to read from joystick device
        r = new Reader();
        r.start();
        // Thread to write to VelocityComponent
        w = new Writer();
        w.start();
    }

    /**
     * Provide additional information for usage...
     */
    protected String additionalUsageInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Component-specific options:\n\n");
        sb.append("  -dev <device name>         <override default (" + devName + ")>\n");
        sb.append("  -novel                     <don't request velocity server>\n");
        sb.append("  -pioneer                   <request Pioneer server>\n");
        sb.append("  -videre                    <request Videre server>\n");
        sb.append("  -motion                    <request Motion server>\n");
        sb.append("  -segway                    <request Segway server>\n");
        sb.append("  -adesim                    <request ADESim server>\n");
        sb.append("  -usarsim                   <request USARSim server>\n");
        sb.append("  -mopo                      <request MoPo server>\n");
        sb.append("  -deftv tv                  <default translational velocity>\n");
        sb.append("  -defrv rv                  <default rotational velocity>\n");
        return sb.toString();
    }

    /** 
     * Parse additional command-line arguments
     * @return "true" if parse is successful, "false" otherwise 
     */
    protected boolean parseadditionalargs(String[] args) {
        boolean found = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-dev") && (++i < args.length)) {
                devName = args[i];
                found = true;
            } else if (args[i].equalsIgnoreCase("-pioneer")) {
                velVersion = "com.pioneer.PioneerComponent";
                found = true;
            } else if (args[i].equalsIgnoreCase("-videre")) {
                velVersion = "com.videre.VidereComponent";
                found = true;
            } else if (args[i].equalsIgnoreCase("-motion")) {
                velVersion = "com.motion.MotionComponent";
                found = true;
            } else if (args[i].equalsIgnoreCase("-segway")) {
                velVersion = "com.segway.SegwayComponent";
                found = true;
            } else if (args[i].equalsIgnoreCase("-adesim")) {
                velVersion = "com.adesim.ADESimComponent";
                found = true;
            } else if (args[i].equalsIgnoreCase("-usarsim")) {
                velVersion = "com.usarsim.USARSimComponent";
                found = true;
            } else if (args[i].equalsIgnoreCase("-mopo")) {
                velVersion = "com.mopo.MoPoComponent";
                found = true;
            } else if (args[i].equalsIgnoreCase("-altsteer")) {
                altSteer = true;
                found = true;
            } else if (args[i].equalsIgnoreCase("-deftv")) {
                double tv;
                try {
                    tv = Double.parseDouble(args[i + 1]);
                    i++;
                    defTV = tv;
                } catch (NumberFormatException nfe) {
                    System.err.println(prg + ": deftv " + args[i + 1]);
                    System.err.println(prg + ": " + nfe);
                }
                found = true;
            } else if (args[i].equalsIgnoreCase("-defrv")) {
                double rv;
                try {
                    rv = Double.parseDouble(args[i + 1]);
                    i++;
                    defRV = rv;
                } catch (NumberFormatException nfe) {
                    System.err.println(prg + ": defrv " + args[i + 1]);
                    System.err.println(prg + ": " + nfe);
                }
                found = true;
            } else if (args[i].equalsIgnoreCase("-novel")) {
                useVel = false;
            } else {
                System.out.println("Unrecognized argument: " + args[i]);
                return false;  // return on any unrecognized args
            }
        }
        return found;
    }

    /**
     * The <code>Writer</code> is the main loop that "interprets" the
     * joystick and sends to Vel if requested.
     */
    private class Writer extends Thread {

        private boolean shouldWrite;

        public Writer() {
            shouldWrite = true;
        }

        public void run() {
            int i = 0;
            double tv, rv;
            while (shouldWrite) {
                Sleep(100);
                // Check for update
                synchronized (update) {
                    if (update) {
                        update = false;
                    } else {
                        continue;
                    }
                }
                if (useVel) {
                    if (verbose) {
                        System.out.println(prg + ": T: " + values[0] + " R: " + values[1]);
                    }
                    tv = defaultVels[0] * ((double) values[0] / (double) (1000));
                    // It's sometimes hard to turn in place...
                    if ((tv < (defaultVels[0] * 0.25)) && (tv > (defaultVels[0] * -0.25))) {
                        tv = 0;
                    }
                    if (tv >= 0) {
                        rv = defaultVels[1] * ((double) values[1] / (double) (1000));
                    } else {
                        rv = defaultVels[1] * ((double) values[1] / (double) (-1000));
                    }
                    if ((rv < (defaultVels[1] * 0.25)) && (rv > (defaultVels[1] * -0.25))) {
                        rv = 0;
                    }
                    if (verbose) {
                        System.out.println("tv: " + tv + " rv: " + rv);
                    }
                    if (gotVel) {
                        try {
                            call(vel, "setVels", tv, rv);
                        } catch (ADEException ace) {
                            System.err.println(prg + ": Error setting vels: " + ace);
                        }
                    }
                }
            }
            System.out.println(prg + ": Exiting Writer thread...");
        }

        public void halt() {
            System.out.print("halting write thread...");
            shouldWrite = false;
        }
    }

    /**
     * The <code>Reader</code> is the main loop that listens to the joystick.
     */
    private class Reader extends Thread {

        private boolean shouldRead;

        public Reader() {
            shouldRead = true;
        }

        public void run() {
            int i = 0;
            getJoystick();
            String acc = null;
            int tv = 0, rv = 0;
            while (shouldRead) {
                Sleep(100);
                try {
                    acc = dev.readLine();
                    dev.seek(0L);
                } catch (IOException ioe) {
                    //halt();
                    if (useVel) {
                        try {
                            call(vel, "setVels", 0.0, 0.0);
                        } catch (ADEException ace) {
                            System.err.println(prg + ": Error setting vels: " + ace);
                        }
                    }
                    System.err.println("Exception: " + ioe);
                    getJoystick();
                }
                if (acc == null)
                    continue;
                synchronized (update) {
                    update = true;
                }
                String[] tokens = acc.trim().split("\\s+");
                if (Integer.parseInt(tokens[2]) > 0) {
                    tv = 0;
                    rv = 0;
                } else {
                    tv = Integer.parseInt(tokens[1]);
                    rv = Integer.parseInt(tokens[0]);
                }
                values[0] = (short)tv;
                values[1] = (short)rv;
                //System.out.println("event: " + event + ", input: " + input + ", value: " + value);
            }
            System.out.println(prg + ": Exiting Reader thread...");
        }

        public void halt() {
            System.out.print("halting read thread...");
            shouldRead = false;
        }
    }

    private void getJoystick()
    {
        boolean found = false;
        System.err.println(prg + ": accessing joystick, please wait.");
        while (! found) {
            Sleep(1000);
            try {
                dev = new RandomAccessFile(devName, "r");
                found = true;
            } catch (FileNotFoundException fnfe) {
            }
        }
        System.err.println(prg + ": joystick ready.");

    }
    /**
     * Log a message using ADE Component Logging, if possible.  The ASL facility
     * takes care of adding timestamp, etc.
     * @param o the message to be logged
     */
    protected void logItASL(Object o) {
        canLogIt(o);
    }

    /**
     * Set the state of ADE Component Logging.  When true and logging is not
     * started, this starts logging.  When false and logging is started, this
     * stops logging and causes the log files to be written to disk.  ADE server
     * logging is a global logging facility, so starting logging here enables
     * logging in a currently instantiated ADE servers.  Note: You want to stop
     * ADE server logging before quitting, or the files will not be complete.
     * @param state indicates whether to start (true) or stop (false) logging.
     */
    protected void setASL(boolean state) {
        try {
            setADEComponentLogging(state);
        } catch (Exception e) {
            System.out.println("setASL: " + e);
        }
    }

    protected void updateComponent() {}
    protected void updateFromLog(String logEntry) {}

    /**
     * <code>main</code> passes the arguments up to the ADEComponentImpl
     * parent.  The parent does some magic and gets the system going.
     *
     public static void main(String[] args) throws Exception {
     ADEComponentImpl.main(args);
     }
     */
}
