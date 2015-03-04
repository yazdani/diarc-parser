/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 * Last update: April 2010
 *
 * PioneerComponentImpl.java
 */
package com.pioneer;

import utilities.DataCopy;
import com.*;
//import com.laser.*;
//import com.logger.*;
import com.pioneer.gui.*; // for the panels
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.rmi.*;
import java.util.*;
import java.util.zip.DataFormatException;
import java.io.*;
//import javax.comm.*;
import ade.ADEComponent;
import ade.ADEComponentImpl;

/**
 * The implementation for a Pioneer robot server.
 */
public class PioneerComponentImpl extends SerialPortComponentImpl implements
        PioneerComponent, Runnable {
    // Note: this is inherited code; I've tried to clean it up some...you
    // should have seen it before...
    // Note: bumpers 1, 4, and 7 are not considered (have been commented
    // out of the packet parsing) due to flaky return values.

    public PioneerComponent robot;
    static Thread t = null;
    private static String prg = "PioneerComponentImpl";
    private static String serverType = "Pioneer";
    private static long DEF_READ_TIMEOUT = 500; // in ms
    // logger
    static public boolean useLogger = false;
    static public String logFileName = "pioneerserver.log";
    private boolean gotLogger = false;
    private Object logger;
    private String myID = "unknown";
    static public boolean usePeoplebot = true;  // command-line option
    //static int NUM_SONARS  = 16;
    static int NUM_SONARS = 24;   // if !usePeoplebot, will be reset to 16
    static int NUM_BUMPERS = 10;
    static boolean USE_LASER = false;
    /* convenient constants */
    public static final int MAX_PACKET_LEN = 200;
    byte[] readBuffer = new byte[MAX_PACKET_LEN];
    // convert from integer reading in range 0-4096 to degrees
    public static final float angleConvFactor = (float) 0.08789;
    // this will print all kinds of information
    static boolean debug = false;
    static boolean verbose = true;   // some info
    static boolean verbose2 = false; // more info
    private boolean showSonar = false;
    private boolean showBumpers = false;
    private boolean showDigin = false;
    private boolean debugPacket = false;
    private int numberBytes;
    byte status;
    boolean readingBuffer = false, writingBuffer = false;
    Integer outputStreamGuard = new Integer(0);
    // local sensor storage
    // motor
    //short velocity = 10;
    //byte[] dataBuffer = null;
    boolean[] motorStatus = new boolean[1];
    boolean[] motorStall = new boolean[2];
    short lVel, rVel, Control;
    int leftEncoder, rightEncoder;
    static public boolean useSpeedRamp = true; //command-line option
    static public boolean batteryCheck = false; //command-line option
    int curTV = 0, targetTV = 0;
    int curRV = 0, targetRV = 0;
    int MAX_DELTA_TV_A = 15; // acceleration (DCB - increased from 5)
    int MAX_DELTA_TV_D = 40; // deceleration (DCB - increased from 10)
    int MAX_DELTA_RV = 10; // (DCB - increased from 1)
    // handle the go command
    long goT = System.currentTimeMillis();
    long goUntilT = 0;
    boolean handlingGo = false;
    // sonar
    static public boolean movingSonarOnly = true;
    static public boolean leaveSonarOff = true;
    static public boolean SONAR_BURST_FLAG = false; // true=burst; false=always on
    private int SONAR_BURST_TIME = 5000;     // milliseconds
    private boolean sonarOn = true;
    private short[] shortSonars;
    private double[] sonars;
    private boolean[] sonarNew;
    private short sonarReadings;
    private long sonarT = System.currentTimeMillis();
    // bumpers
    private boolean[] bumpers = new boolean[NUM_BUMPERS];
    private boolean useBumpAlarm = false;
    private boolean bumpAlarm = false;
    private boolean handlingBumpAlarm = false;
    private static short BUMP_ALARM_TV = 50;
    private static long BUMP_ALARM_TIME = 2000;
    private long bumpT = System.currentTimeMillis();
    private short[] effectLR = new short[]{-5, -3, 0, 3, 5, 5, 3, 0, -3, -5};
    // position
    short posX, posY, PTU, Timer;
    short SB;
    short thPos;
    double thReturn;
    // battery
    float battery;
    float lowbattery = (float) 0.0; // unless explicitly set, we'll leave it at zero
    short LowBattery;     // from P2OS; no way to set it?
    //private boolean useBatteryAlarm = true;
    private boolean batteryAlarm = false;
    // IR
    private boolean[] ireds = new boolean[2];
    private byte leftIRBitMask = 0x01;
    private byte rightIRBitMask = 0x02;
    // compass
    short compass;
    short Analog;
    short Digin, Digout;
    short Checksum;
    String Type = "", Subtype = "", serial = "", name = "";
    short fourMotors, rotVelTop, transVelTop, rotAccTop, transAccTop, pwmMax, encoder;
    short sInfoCycle, HostBaud, AuxBaud, HasGripper, FrontSonar, RearSonar, RevCount;
    short WatchDog, P2Mpacs, StallVal, StallCount, CompassT, CompX, CompY, rotVelMax, transVelMax, rotAcc;
    short rotDecel, rotKp, rotKv, rotKi, transAcc, transDecel, transKp, transKv, transKi;
    short JoyVelMax, JoyRVelMax;
    static boolean mainRun = true, endRun = false;

    // ********************************************************************
    // *** abstract methods in ADEComponentImpl that need to be implemented
    // ********************************************************************
    /** This method will be activated whenever a client calls the
     * requestConnection(uid) method. Any connection-specific initialization
     * should be included here, either general or user-specific.
     * @param user the ID of the user/client that gained a connection */
    protected void clientConnectReact(String user) {
        if (dbg > 0 || debug) {
            System.out.println(prg + ": got connection from " + user + "!");
        }
    }

    /** This method will be activated whenever a client that has called the
     * requestConnection(uid) method fails to update (meaning that the
     * heartbeat signal has not been received by the reaper), allowing both
     * general and user specific reactions to lost connections. If it returns
     * true, the client's connection is removed.
     * @param user the ID of the user/client that lost a connection */
    protected boolean clientDownReact(String user) {
        if (dbg > 0 || debug) {
            System.out.println(prg + ": lost connection with " + user + "!");
        }
        return false;
    }

    /** This method will be activated whenever the heartbeat returns a
     * remote exception (i.e., the server this is sending a
     * heartbeat to has failed).
     * @param s the type of {@link ade.ADEComponent ADEComponent} that failed */
    protected void componentDownReact(String serverkey, String[][] constraints) {
        String s = constraints[0][1];

        if (dbg > 0 || debug) {
            System.out.println(prg + ": reacting to down " + s + " server...");
        }
        if (s.indexOf("LoggerComponent") >= 0) {
            gotLogger = false;
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

        if (dbg > 0 || debug) {
            System.out.println(myID + ": reacting to connected " + s + " server...");
        }
        if (s.indexOf("LoggerComponent") >= 0) {
            gotLogger = true;
            try {
                call(logger, "openOutputFile", myID, logFileName);
                if (dbg > 0) {
                    System.out.println(prg + ": opened log file " + logFileName + "...");
                }
            } catch (Exception re) {
                System.err.println(prg + ": exception opening log file...");
                System.err.println("\t" + re);
            }
        }
        return;
    }

    /** Adds additional local checks for credentials before allowing a shutdown.
     * @param credentials the Object presented for verification
     * @return must return "false" if shutdown is denied, true if permitted */
    protected boolean localrequestShutdown(Object credentials) {
        return false;
    }

    // local shutdown info
    protected void localshutdown() {
        System.out.print("Shutting Down " + prg + "...");
        try {
            sendStop();
            System.out.print("stop...");
        } catch (RemoteException ignore) {
            System.out.println("exception sending stop...");
        }
        sendClose();
        System.out.print("close...");
        mainRun = false;
        if (gotLogger) {
            System.out.print("closing log...");
            try {
                call(logger, "closeOutputFile", myID);
            } catch (Exception ignore) {
                System.err.println("Exception closing log:\n" + ignore);
            }
        }
        System.out.print("killing threads...");
        while ((t != null) && t.isAlive());
        System.out.print("post-killing...");
        serialPort.close();
        try {
            Thread.sleep(DEF_READ_TIMEOUT);
        } catch (Exception ignore) {
        }
        System.out.println("done.");
        System.out.flush();
    }

    private void logString(String desc, String s) {
        if (gotLogger) {
            try {
                call(logger, "writeString", myID, logFileName, desc, System.currentTimeMillis(), s);
            } catch (Exception re) {
                gotLogger = false;
            }
        }
    }

    // TODO: MS: called this updateComponent, but this should really not stay
    // in an infinite loop, but let the ADEComponentImpl take care of the timing...
    protected void updateComponent() {
        long nowT;
        boolean sonarQuiet = false;
        // ready to register
        // finishedInitialization();
        if (useLogger) {
            if (verbose) {
                System.out.println(prg + ": Setting alias logger");
            }
            try {
                myID = getID();
                getClient("com.logger.LoggerComponent");
            } catch (Exception re) {
                System.err.println("Exception getting logger " + re);
            }
        }
        if (batteryCheck) {
            try {
                float v = getVoltage();
                System.out.println("Pioneer battery voltage is: " + v);
            } catch (Exception e) {
                System.out.println("Error getting voltage");
            }
        }
        if (verbose) {
            System.out.println(prg + ": starting main run loop...");
        }
        //startExecution();

        while (mainRun) {
            updateFromSerialPort();
            nowT = System.currentTimeMillis();
            if (SONAR_BURST_FLAG) {
                if (sonarQuiet) {
                    if ((nowT - sonarT) > SONAR_BURST_TIME) {
                        // turn sonar on temporarily
                        try {
                            sendSonar((short) 1);
                        } catch (Exception ignore) {
                        }
                        sonarQuiet = false;
                        sonarT = nowT;
                    }
                } else {
                    if ((nowT - sonarT) > 1000) {
                        // turn sonar off again
                        try {
                            sendSonar((short) 0);
                        } catch (Exception ignore) {
                        }
                        sonarQuiet = true;
                        sonarT = nowT;
                    }
                }
            }
            // set when the packet is parsed; this is
            // the only place it it unset
            if (useBumpAlarm) {
                if (bumpAlarm) {
                    if ((nowT - bumpT) < BUMP_ALARM_TIME) {
                        try {
                            sendVel((short) 0);
                            sendRVel((short) 0);
                            bumpAlarm = false;
                            handlingBumpAlarm = false;
                        } catch (RemoteException ignore) {
                        }
                    }
                }
            }
            // the only time handlingGo, goT, and goUntilT are set is when a "go"
            // command is issued; goT-nowT will otherwise never be less than goUntilT
            if (handlingGo) {
                if ((nowT - goT) > goUntilT) {
                    try {
                        System.out.println("Stopping go");
                        //sendStop();
                        stop();
                    } catch (Exception e) {
                        if (verbose) {
                            System.err.println("Exception attempting to stop robot");
                        }
                    }
                    handlingGo = false;
                }
            }
            try {
                // DCB - I see nothing about this in the
                //       pioneer manual, *and* it causes a
                //       large amount of lag (up to 3 seconds).
                //       Reduced from 50 but probably should
                //       get rid of altogether.
                Thread.sleep(5);  // sleep the minimum update time of the Pioneer
            } catch (Exception ignore) {
            }
        }
    }

    // ********************************************************************
    // *** PioneerComponent interface available for remote execution
    // ********************************************************************
    /** Toggles the sonar printout to the console.
     * @throws RemoteException If an error occurs */
    public void toggleShowSonar() throws RemoteException {
        showSonar = !showSonar;
    }

    /** Toggles the bumper printout to the console.
     * @throws RemoteException If an error occurs */
    public void toggleShowBumper() throws RemoteException {
        showBumpers = !showBumpers;
    }

    /** Toggles the digital in (LSB are IR sensors) printout to the console.
     * @throws RemoteException If an error occurs */
    public void toggleShowDigin() throws RemoteException {
        showDigin = !showDigin;
    }

    // ********************************************************************
    // *** BumperSensorComponent interface
    // ********************************************************************
    /** Return a specific bumper value. Bumper values are stored in an
     * array where index 0 is the left/front bumper, going clockwise
     * to the left/back bumper.
     * @param w The bumper value to return
     * @return <tt>true</tt> if activated, <tt>false</tt> otherwise
     * @throws RemoteException If an error occurs */
    public boolean getBumper(int w) throws RemoteException {
        return bumpers[w];
    }

    /** Return all the bumper values. Bumper values are stored in an
     * array where index 0 is the left/front bumper, going clockwise
     * to the left/back bumper.
     * @return <tt>true</tt> if activated, <tt>false</tt> otherwise
     * @throws RemoteException If an error occurs */
    public boolean[] getBumperReadings() throws RemoteException {
        return bumpers;
    }

    // ********************************************************************
    // *** BatterySensorComponent interface
    // ********************************************************************
    /** Returns voltage as a float value */
    public float getVoltage() throws RemoteException {
        return battery;
    }

    //private float getBattery() throws RemoteException {
    //	return battery;
    //}
    /** Returns the status of the battery alarm (i.e., low battery). */
    public boolean getBatteryAlarm() throws RemoteException {
        return batteryAlarm;
    }

    /** Sets the value of the "low battery" indicator. */
    public void setLowBatteryValue(float low) throws RemoteException {
        if (low >= 0.0 && low <= 15.0) {
            lowbattery = low;
        }
    }

    // ********************************************************************
    // *** CompassSensorComponent interface
    // ********************************************************************
    public short getCompass() throws RemoteException {
        return compass;
    }

    // ********************************************************************
    // *** DigitalInSensorComponent interface
    // ********************************************************************
    public short getDigin() throws RemoteException {
        return Digin;
    }

    // ********************************************************************
    // *** DigitalOutEffectorComponent interface
    // ********************************************************************
    public short getDigout() throws RemoteException {
        return Digout;
    }

    /** Selects and sets a selected output port. Convert the integer
     * arguments to a single short (as required by the P2OS), where:
     * <ul>
     * <li>the most significant 8 bits are a byte mask that selects output ports</li>
     * <li>the least significant 8 bits set (1) or reset (0) the selected port(s)</li>
     * </ul>
     * There are 8 possible ports, identified by numbers 0-7.
     * @param port which port to set
     * @param arg 1 (set) or 0 (reset) */
    public void setDigitalOut(int port, int arg) throws RemoteException {
        // create the bit mask
        short set = (short) (Math.pow(2, (port & 0x000000FF)));
        // shift it to most significant bits and set/reset
        set = (short) ((set << 8) | (byte) arg);
        sendDigout(set);
    }

    // ********************************************************************
    // *** EncoderSensorComponent interface
    // ********************************************************************
    public int[] getEncoders() throws RemoteException {
        //System.out.println("In getEncoders(); returning ("+ leftEncoder +","+ rightEncoder +")");
        return new int[]{leftEncoder, rightEncoder};
    }

    // ********************************************************************
    // *** IRSensorComponent interface
    // ********************************************************************
    /** Returns a boolean for a specific IR (0 is left, 1 is right).
     * @param w which IR */
    public boolean getIR(int w) throws RemoteException {
        return ireds[w];
    }

    /** Returns a boolean array for all IRs. */
    public boolean[] getIRs() throws RemoteException {
        return new boolean[]{ireds[0], ireds[1]};
    }

    // ********************************************************************
    // *** MotorSensorComponent interface
    // ********************************************************************
    /** Return an array of booleans that are <tt>true</tt> if the
     * motor is operating, <tt>false</tt> otherwise. Index 0 is
     * the left wheel motor, index 1 is the right wheel motor. */
    public boolean[] getMotors() throws RemoteException {
        return motorStatus;
    }

    private short getMotor() throws RemoteException {
        return status;
    }

    private boolean[] motorStatus() throws RemoteException {
        return motorStatus;
    }

    // ********************************************************************
    // *** MovementEgocentricEffectorComponent
    // ********************************************************************
    /** Turn a relative orientation (in radians).
     * @param t The number of radians to turn
     * @throws RemoteException If an error occurs */
    public void turn(double t) throws RemoteException {
        turnDeg((int) Math.toDegrees(t));
    }

    /** Turn a relative orientation (in radians) at the specified speed.
     * @param t The number of radians to turn
     * @param spd The speed at which to rotate
     * @throws RemoteException If an error occurs */
    public void turn(double t, double spd) throws RemoteException {
    }

    /** Turn a relative orientation (in degrees). Note that this method
     * relies on the Pioneer's internal compass.
     * @param t The number of degrees to turn
     * @throws RemoteException If an error occurs */
    public void turnDeg(int t) throws RemoteException {
        if (movingSonarOnly && !leaveSonarOff) {
            sendSonar((short) 1);
            sonarOn = true;
        }
        sendDhead((short) t);
    }

    /** Turn a relative orientation (in degrees) at the specified speed.
     * Note that this method relies on the Pioneer's internal compass.
     * @param t The number of degrees to turn
     * @param spd The speed at which to rotate
     * @throws RemoteException If an error occurs */
    public void turnDeg(int t, double spd) throws RemoteException {
    }

    public void turnTo(int position) throws RemoteException {
        if (movingSonarOnly && !leaveSonarOff) {
            sendSonar((short) 1);
            sonarOn = true;
        }
        sendHead((short) position);
    }

    /** Move a specified distance (in meters) straight ahead at the
     * default speed.
     * @param dist The distance to move forward
     * @throws RemoteException If an error occurs */
    public void go(double dist) throws RemoteException {
        go(dist, 100);
    }

    /** Move a specified distance (in meters) straight ahead at the
     * specified speed.
     * @param dist The distance to move forward
     * @throws RemoteException If an error occurs */
    public void go(double dist, int spd) throws RemoteException {
        //long time2, time1 = System.currentTimeMillis();
        if (movingSonarOnly && !leaveSonarOff) {
            sendSonar((short) 1);
            sonarOn = true;
        }
        goT = System.currentTimeMillis();
        //goUntilT = (long)(distance * 1000/spd);
        goUntilT = (long) (dist * 1000000 / spd);
        handlingGo = true;
        if (useSpeedRamp) {
            if (dbg > 4) {
                System.out.print("Set targetTV from " + targetTV);
            }
            if (dist > 0) {
                targetTV = spd;
            } else {
                targetTV = -spd;
            }
            if (dbg > 4) {
                System.out.println(" to " + targetTV);
            }
        } else {
            if (dist > 0) {
                sendVel((short) spd);
            } else {
                sendVel((short) (-spd));
            }
            //	// TODO: this can't work...won't let the thread run
            //	while (((time2 = System.currentTimeMillis()) - time1) < (distance * 1000/(float)spd));
        }
        //sendStop(); // now done with handlingGo in run()
    }

    /** Move forward (positive) or backward (negativein the direction of the current heading in meters
     * @param dx the distance, in m, to move forward/backward
     * @param dy the distance, in m, to move left/right
     * @throws RemoteException If an error occurs */
    public void move(double distance) throws RemoteException {
        sendMove((short) (distance * 1000));
    }

    /** Move to a location relative to the current location. Note that the
     * parameters are perhaps better thought of as <i>delta</i> distances
     * (or change in location). The trajectory travelled is implementation
     * dependent (i.e., not necessarily a straight line).
     * @param dx the distance, in m, to move forward/backward
     * @param dy the distance, in m, to move left/right
     * @throws RemoteException If an error occurs */
    public void move(double dx, double dy) throws RemoteException {
    }

    /** Move to a location relative to the current location at the specified
     * speeds. Note that the first two parameters are perhaps better thought
     * of as <i>delta</i> distances (or change in location). The trajectory
     * travelled is implementation dependent (i.e., not necessarily a
     * straight line).
     * @param dx The distance, in m, to move forward/backward
     * @param dy The distance, in m, to move left/right
     * @param tv The translational velocity at which to move
     * @param rv The rotational velocity at which to move
     * @throws RemoteException If an error occurs */
    public void move(double dx, double dy, double tv, double rv)
            throws RemoteException {
    }

    // ********************************************************************
    // *** PositionEgocentricComponent interface
    // ********************************************************************
    /** Return a three-element array of (x, y, theta) position in m, m, rads.
     * @return A three-element <tt>double</tt> array (x,y,t)
     * @throws RemoteException If an error occurs */
    public double[] getPoseEgo() throws RemoteException {
        // DCB- was returning x,y in mm not m and theta in 0-2pi not -pi-pi
        double th = Math.toRadians(thReturn);
        if (th > Math.PI) {
            th -= 2 * Math.PI;
        }
        return new double[]{(double) posX / 1000., (double) posY / 1000., th};
    }

    /** Return an x coordinate position.
     * @return A <tt>double</tt> value of the x-coordinate
     * @throws RemoteException If an error occurs */
    public double getXPosEgo() throws RemoteException {
        // DCB- was returning x in mm not m
        return (double) posX / 1000.;
    }

    /** Return a y coordinate position.
     * @return A <tt>double</tt> value of the y-coordinate
     * @throws RemoteException If an error occurs */
    public double getYPosEgo() throws RemoteException {
        // DCB- was returning x in mm not m
        return (double) posY / 1000.;
    }

    /** Return orientation (in rads).
     * @return A <tt>double</tt> value of the orientation
     * @throws RemoteException If an error occurs */
    public double getTPosEgo() throws RemoteException {
        // DCB- was returning theta in 0-2pi not -pi-pi
        double th = Math.toRadians(thReturn);
        if (th > Math.PI) {
            th -= 2 * Math.PI;
        }
        return th;
    }

    /** Return orientation (in degrees).
     * @return A <tt>double</tt> value of the orientation
     * @throws RemoteException If an error occurs */
    public double getTPosEgoDeg() throws RemoteException {
        return thReturn;
    }

    /** Reset the current pose to the given (x, y, t) coordinates
     * (in m, m, rads).
     * @param x The <tt>x</tt> coordinate
     * @param y The <tt>y</tt> coordinate
     * @param t The desired orientation
     * @throws RemoteException If an error occurs */
    public void setPoseEgo(double x, double y, double t)
            throws RemoteException {
        // DCB- this is probably wrong (if x,y are really meters)
        // TODO: set in P2OS
        posX = (short) x;
        posY = (short) y;
        thReturn = Math.toDegrees(t);
    }

    /** Reset the current location to the given (x, y, t) coordinates
     * (in m, m, degs).
     * @param x The <tt>x</tt> coordinate
     * @param y The <tt>y</tt> coordinate
     * @param t The desired orientation
     * @throws RemoteException If an error occurs */
    public void setPoseEgoDeg(double x, double y, int t)
            throws RemoteException {
        // DCB- this is probably wrong (if x,y are really meters)
        // TODO: set in P2OS
        posX = (short) x;
        posY = (short) y;
        thReturn = t;
    }

    // ********************************************************************
    // *** SonarComponent interface
    // ********************************************************************
    /** Returns a particular sonar reading.
     * @param w Which sonar
     * @return The distance reading
     * @throws RemoteException If an error occurs */
    public double getSonar(int w) throws RemoteException {
        return sonars[w];
    }

    /** Returns the sonar readings.
     * @return Array of distances array
     * @throws RemoteException If an error occurs */
    public double[] getSonars() throws RemoteException {
        return sonars;
    }

    /** Returns indicator of a particular new sonar reading.
     * @param w which sonar
     * @return boolean
     * @throws RemoteException If an error occurs */
    public boolean getSonarNew(int w) throws RemoteException {
        return sonarNew[w];
    }

    /** Returns indicators for new sonar readings.
     * @return boolean array
     * @throws RemoteException If an error occurs */
    public boolean[] getSonarNew() throws RemoteException {
        return sonarNew;
    }

    // ********************************************************************
    // *** VelocitySensorComponent interface
    // ********************************************************************
    /** Return the left wheel velocity. */
    public int getLeftVelocity() throws RemoteException {
        // DCB- this may be wrong (mm instead of m)
        return (int) lVel;
    }

    /** Return the right wheel velocity. */
    public int getRightVelocity() throws RemoteException {
        // DCB- this may be wrong (mm instead of m)
        return (int) rVel;
    }

    public int[] getVelocity() throws RemoteException {
        // DCB- this may be wrong (mm instead of m)
        return (new int[]{lVel, rVel});
    }

    // ********************************************************************
    // *** VelocityComponent interface
    // ********************************************************************
    public boolean setVels(double tv, double rv) throws RemoteException {
        setTranslationalVelocity((int) (tv * 1000));
        setRotationalVelocity((int) Math.toDegrees(rv));
        return true;
    }

    public boolean setTV(double tv) throws RemoteException {
        setTranslationalVelocity((int) (tv * 1000));
        return true;
    }

    public boolean setRV(double v) throws RemoteException {
        setRotationalVelocity((int) Math.toDegrees(v));
        return true;
    }

    public double[] getDefaultVels() throws RemoteException {
        return new double[]{0.1, 0.1};
    }

    public double getTV() throws RemoteException {
        // DCB- was returning mm not m
        return curTV / 1000.;
    }

    public double getRV() throws RemoteException {
        // DCB- was returning mm not m
        return curRV / 1000.;
    }

    public double[] getVels() throws RemoteException {
        // DCB- was returning mm not m and degrees not rads
        return new double[]{curTV / 1000., Math.toRadians(curRV)};
    }

    // ********************************************************************
    // *** VelocityEffectorComponent interface
    // ********************************************************************
    /** Stop the motors. */
    public void stop() throws RemoteException {
        if (movingSonarOnly) {
            sendSonar((short) 0);
            sonarOn = false;
        }
        if (useSpeedRamp) {
            targetTV = 0;
            targetRV = 0;
        } else {
            sendStop();
        }
    }

    /** Emergency stop the motors. */
    public void emergencyStop() throws RemoteException {
        if (movingSonarOnly) {
            sendSonar((short) 0);
            sonarOn = false;
        }
        if (useSpeedRamp) {
            targetTV = 0;
            targetRV = 0;
            curTV = 0;
            curRV = 0;
        }
        sendEstop();
    }

    //	public void setVelocity(int vel) {
    //		velocity = (short)vel;
    //		sendVel((short)vel);
    //	}
    // deprecated
    //public void setWheelVelocity(int[] vel) throws RemoteException {
    //	sendVel2((short)(vel[1]*256 +vel[0]));
    //}
    /** Set the translational velocity (in millimeters per second). */
    public void setTranslationalVelocity(int spd) throws RemoteException {
        //System.out.println("Setting TRANSLATIONAL velocity to "+ spd);
        if (movingSonarOnly) {
            if (useSpeedRamp && spd == 0) {
                sendSonar((short) 0);
                sonarOn = false;
            } else if (!leaveSonarOff) {
                sendSonar((short) 1);
                sonarOn = true;
            }
        }
        if (useSpeedRamp) {
            targetTV = spd;
        } else {
            sendVel((short) spd);
        }
    }

    /** Set the rotational velocity (in degrees per second). */
    public void setRotationalVelocity(int v) throws RemoteException {
        //System.out.println("Setting ROTATIONAL velocity to "+ v);
        if (movingSonarOnly) {
            if (useSpeedRamp && v == 0) {
                sendSonar((short) 0);
                sonarOn = false;
            } else if (!leaveSonarOff) {
                sendSonar((short) 1);
                sonarOn = true;
            }
        }
        if (useSpeedRamp) {
            targetRV = v;
        } else {
            sendRVel((short) v);
        }
    }

    /** Set the rotational velocity (in radians per second). */
    public void setRotationalVelocity(double v) throws RemoteException {
        setRotationalVelocity((int) Math.toDegrees(v));
    }

    /** Set the left wheel velocity (mm/s), leaving the right
     * wheel velocity alone.
     * @param left left wheel velocity */
    public void setLeftVelocity(int left) throws RemoteException {
        if (movingSonarOnly && !leaveSonarOff) {
            sendSonar((short) 1);
            sonarOn = true;
        }
        sendVel2((short) (((left & 0x0000FFFF) << 8) | rVel));
    }

    /** Set the right wheel velocity (mm/s), leaving the left
     * wheel velocity alone.
     * @param right right wheel velocity */
    public void setRightVelocity(int right) throws RemoteException {
        if (movingSonarOnly && !leaveSonarOff) {
            sendSonar((short) 1);
            sonarOn = true;
        }
        sendVel2((short) ((lVel << 8) | (right & 0x0000FFFF)));
    }

    /** Set the wheel velocities (mm/s) using a single value.
     * @param vel velocity */
    public void setVelocity(int vel) throws RemoteException {
        if (movingSonarOnly && !leaveSonarOff) {
            sendSonar((short) 1);
            sonarOn = true;
        }
        sendVel2((short) (((vel & 0x0000FFFF) << 8) | (vel & 0x000000FF)));
    }

    /** Set the wheel velocities (mm/s) given left and right values.
     * @param left left wheel velocity
     * @param right right wheel velocity */
    public void setVelocities(int left, int right) throws RemoteException {
        if (movingSonarOnly && !leaveSonarOff) {
            sendSonar((short) 1);
            sonarOn = true;
        }
        sendVel2((short) (((left & 0x000000FF) << 8) | (right & 0x000000FF)));
    }

    /** Set the wheel velocities (mm/s) given an array of values.
     * @param vels array of wheel velocities */
    public void setVelocities(int[] vels) throws RemoteException {
        short vel;
        if (movingSonarOnly && !leaveSonarOff) {
            sendSonar((short) 1);
            sonarOn = true;
        }
        if (vels == null) {
            vel = 0;
        } else if (vels.length > 1) {
            vel = (short) (((vels[0] & 0x000000FF) << 8) | (vels[1] & 0x000000FF));
        } else {
            vel = (short) ((vels[0] & 0x000000FF) << 8);
        }
        sendVel2(vel);
    }

    /** Set the maximum translational velocity (in millimeters per second). */
    public void setMaxVelocityTranslational(int v) throws RemoteException {
        sendSetv((short) (v & 0x000000FF));
    }

    /** Set the maximum rotational velocity (in degrees per second). */
    public void setMaxVelocityRotational(int v) throws RemoteException {
        sendSetrv((short) (v & 0x000000FF));
    }

    // ********************************************************************
    // *** not entirely sure what these are...
    // ********************************************************************
    public short getSB() throws RemoteException {
        return SB;
    }

    public short getControl() throws RemoteException {
        return Control;
    }

    public short getPTU() throws RemoteException {
        return PTU;
    }

    public short getTimer() throws RemoteException {
        return Timer;
    }

    public short getAnalog() throws RemoteException {
        return Analog;
    }

    public short getChecksum() throws RemoteException {
        return Checksum;
    }

    public void sendReset() {
    }

    /** When we bump something, take immediate action to attempt avoidance.
     * Hard-coded, as this is the only place these things will be used. */
    private void handleBumpAlarm() {
        short lr = 0;
        int lrcnt = 0;
        int front = 0; // positive is front; negative is back

        //System.out.print("Bumpers triggered: ");
        handlingBumpAlarm = true;
        for (int i = 0; i < NUM_BUMPERS; i++) {
            if (bumpers[i]) {
                //System.out.print(" "+ i);
                lr += effectLR[i];
                lrcnt++;
                if (i < (NUM_BUMPERS / 2)) {
                    front++;
                } else {
                    front--;
                }
            }
        }
        //System.out.println();
        if (lrcnt != 0) {
            lr /= lrcnt;
        }
        //System.out.println("front="+ front +"; left/right="+ lr);
        if (useSpeedRamp) {
            if (front > 0) {
                targetTV = -BUMP_ALARM_TV;
            } else {
                targetTV = BUMP_ALARM_TV;           // go forwards
            }
            targetRV = lr;
            bumpT = System.currentTimeMillis();  // only set if commands are successful
        } else {
            try {
                if (front > 0) {
                    sendVel(BUMP_ALARM_TV);           // go forwards
                } else {
                    sendVel((short) (-BUMP_ALARM_TV)); // go backwards
                }
                sendRVel(lr);
                bumpT = System.currentTimeMillis();  // only set if commands are successful
            } catch (RemoteException ignore) {
            }
        }
    }

    /** Retrieve a packet and run it throuupdateSergh the appropriate parser. */
    protected void updateFromSerialPort() {
        byte[] p2osPacket;
        int dTV = 0, dRV = 0;
        if (useSpeedRamp) {
            // a velocity cap hack; try to get to the target velocities, but only in
            // MAX_DELTA increments; note that the "setRotationalVelocity" and
            // "setTranslationalVelocity" methods are pre-empted and only set the
            // target velocities...
            //System.out.println("---> Checking trans vel: cur="+ curTV +", target="+ targetTV);
            if (curTV != targetTV) {
                try {
                    if (targetTV < curTV) {
                        dTV = Math.min(MAX_DELTA_TV_D, (curTV - targetTV));
                        curTV -= dTV;

//System.out.println("\tSetting trans vel to "+ curTV);
                        sendVel((short) curTV);
                    } else {
                        dTV = Math.min(MAX_DELTA_TV_A, (targetTV - curTV));
                        curTV += dTV;
//System.out.println("\tSetting trans vel to "+ curTV);
                        sendVel((short) curTV);
                    }
                    logString("RAMPTV", "" + curTV);
                } catch (RemoteException re) {
                    if (verbose) {
                        System.err.println("Exception setting rotational velocity:");
                        System.err.println(re);
                    }
                }
            }
            //System.out.println("---> Checking rot vel: cur="+ curRV +", target="+ targetRV);
            if (curRV != targetRV) {
                try {
                    if (targetRV < curRV) {
                        dRV = Math.min(MAX_DELTA_RV, (curRV - targetRV));
                        curRV -= dRV;
                        //System.out.println("\tSetting rot vel to "+ curRV);
                        sendRVel((short) curRV);
                    } else {
                        dRV = Math.min(MAX_DELTA_RV, (targetRV - curRV));
                        curRV += dRV;
                        //System.out.println("\tSetting rot vel to "+ curRV);
                        sendRVel((short) curRV);
                    }
                    logString("RAMPRV", "" + curRV);
                } catch (RemoteException re) {
                    if (verbose) {
                        System.err.println("Exception setting rotational velocity:");
                        System.err.println(re);
                    }
                }
            }
        }
        try {
            p2osPacket = receive();
        } catch (Exception e) {
            System.err.println("Receive Exception: " + e);
            return;
        }
        sendPulse();

        if (p2osPacket == null) {
            return;
        }
        if (p2osPacket.length < 4) {
            return;
        }
        try {
            int typeOfPacket = p2osPacket[3] & 0xFF;
            //if (debugPacket && typeOfPacket == 0x20) {
            if (debugPacket) {
                StringBuffer sb = new StringBuffer();
                sb.append("first 5 bytes of packet: [");
                for (int j = 0; j < 5; j++) {
                    sb.append(" ");
                    sb.append(Integer.toHexString(p2osPacket[j] & 0xff));
                }
                sb.append("]");
                System.out.println(prg + sb.toString());
                logString("INFO", sb.toString());
            }

            switch (typeOfPacket) { // demux into standard vs. aux packets
                case 0x32: // p2opman10.pdf page 37
                case 0x33:
                    parseStandardPacket(p2osPacket);
                    break;
                case 0x90:
                    parseEncoderPacket(p2osPacket);
                    break;
                default:
                    throw new DataFormatException("Unknown Packet Type: 0x" + Integer.toHexString(typeOfPacket));
            }
        } catch (Exception e) {
            System.err.println("Exception in UpdateRobot: " + e);
        }
    }

    /**
     * This takes a standard p2os aux packet and pulls out the
     * embedded data (hopefully, a packet).
     * @param input an array of <code>byte</code>s (a p2os packet)
     * @return an array of the <code>byte</code>s embedded in the aux packet
     */
    protected byte[] stripInput(byte[] input) {
        byte[] stripped = new byte[input[2] - 3];
        DataCopy.copy_byte_array(input, stripped, 4);
        return stripped;
    }

    /**
     * This takes an encoder packet and parses it
     *
     * @param readBuffer an array of <code>byte</code>s (a p2os
     * packet)
     */
    protected void parseEncoderPacket(byte[] readBuffer) throws DataFormatException {
        short temp1, temp2;
        boolean changed = false;

        temp1 = toShortLocal(readBuffer[5], readBuffer[4]);
        temp2 = toShortLocal(readBuffer[7], readBuffer[6]);
        if (leftEncoder != (int) (temp2 * 256 + temp1)) {
            changed = true;
        }
        leftEncoder = temp2 * 256 + temp1;
        temp1 = toShortLocal(readBuffer[9], readBuffer[8]);
        temp2 = toShortLocal(readBuffer[11], readBuffer[10]);
        if (rightEncoder != (int) (temp2 * 256 + temp1)) {
            changed = true;
        }
        rightEncoder = temp2 * 256 + temp1;
        //System.out.println("("+ leftEncoder +","+ rightEncoder +")");
        if (changed) {
            logString("ENCODERS-L-V", "" + leftEncoder + " " + rightEncoder);
        }
    }

    /**
     * This takes a standard packet and parses it
     * @param readBuffer an array of <code>byte</code>s (a p2os packet)
     */
    protected void parseStandardPacket(byte[] readBuffer) throws DataFormatException {
        int i, index, compChecksum;
        short temp;
        boolean changed = false;

        // The Status byte is of the form 0x3S, where S = 2
        // means stopped, S = 3 means moving.
        // I am returning just the relevant value.
        status = (byte) (readBuffer[3] - (byte) 0x30);
        if (status == 2) {
            motorStatus[0] = false;
        } else {
            motorStatus[0] = true;
        }

        // Both Xpos and Ypos are defined to use only the least significant
        // 15 bits. However, bit shifts transform everything to integers...so
        // one byte becomes four. Thus the bit shifts for readBuffer[1] and [3].
        posX = toShortLocal(readBuffer[5], readBuffer[4]);
        posY = toShortLocal(readBuffer[7], readBuffer[6]);

        // The next three values are signed two byte values.
        // The following code places in each the same bit
        // pattern as was received from the robot
        thPos = toShortLocal(readBuffer[9], readBuffer[8]);
        thReturn = (double) (thPos * angleConvFactor);
        if (lVel != toShortLocal(readBuffer[11], readBuffer[10])) {
            changed = true;
        }
        lVel = toShortLocal(readBuffer[11], readBuffer[10]);
        if (rVel != toShortLocal(readBuffer[13], readBuffer[12])) {
            changed = true;
        }
        rVel = toShortLocal(readBuffer[13], readBuffer[12]);
        if (changed) {
            logString("WHEELVELS-LR", "" + lVel + " " + rVel);
        }

        battery = ((float) (readBuffer[14] & 0xFF)) / 10;
        if (battery <= lowbattery) {
            batteryAlarm = true;
        }
        //System.err.println(battery);

        // note that some of the bumpers are commented out; this is
        // because they're "sticky" and not functioning correctly
        SB = toShortLocal(readBuffer[16], readBuffer[15]);
        // the right stall and front bumpers
        motorStall[1] = ((SB & 0x1) > 0); // 1
        bumpers[0] = ((SB & 0x2) > 0);    // 2
        //bumpers[1] = ((SB & 0x4) > 0);    // 4
        bumpers[2] = ((SB & 0x8) > 0);    // 8
        bumpers[3] = ((SB & 0x10) > 0);   // 16
        //bumpers[4] = ((SB & 0x20) > 0);   // 32
        // the left stall and rear bumpers
        motorStall[0] = ((SB & 0x100) > 0); // 256
        bumpers[5] = ((SB & 0x200) > 0);  // 512
        bumpers[6] = ((SB & 0x400) > 0);  // 1024
        //bumpers[7] = ((SB & 0x800) > 0);  // 2048
        bumpers[8] = ((SB & 0x1000) > 0); // 4096
        bumpers[9] = ((SB & 0x2000) > 0); // 8192
        //System.err.println(SB);

        // debug the bumpers -- they're stuck?
        if (showBumpers) {
            System.out.print("Bumper settings:");
            for (int j = 0; j < NUM_BUMPERS; j++) {
                System.out.print(" " + j + ":" + bumpers[j]);
            }
            System.out.println();
        }

        //System.out.print("Setting bumpAlarm: ");
        if (useBumpAlarm) {
            for (int j = 0; j < NUM_BUMPERS; j++) {
                if (bumpers[j] == true) {
                    bumpAlarm = true; // handled at end of method
                }
            }
        }

        Control = toShortLocal(readBuffer[18], readBuffer[17]);
        //System.err.println(Control);
        // PWS: I wonder if that's supposed to be "PTU" not "Control"
        Control = toShortLocal(readBuffer[20], readBuffer[19]);
        //System.err.println(PTU);

        compass = (short) (readBuffer[21] & 0xFF);
        //System.err.println(compass);

        sonarReadings = readBuffer[22];
        if (sonarReadings > NUM_SONARS) {
            throw new DataFormatException("Sonar readings error: 0x" + Integer.toHexString(sonarReadings));
        }

        for (int j = 0; j < NUM_SONARS; j++) {
            sonarNew[j] = false;
        }

        if (showSonar) {
            System.out.print("Sonar (" + sonarReadings + " reads):");
        }
        for (i = 0; i < sonarReadings; i++) {
            index = readBuffer[23 + 3 * i] & 0xFF;
            if ((index < NUM_SONARS) && (index > -1)) {
                shortSonars[index] = toShortLocal(readBuffer[23 + 3 * i + 2], readBuffer[23 + 3 * i + 1]);
                sonars[index] = ((double) shortSonars[index]) * (double) 0.268;
                //sonar[index] = ((float)shortSonar[index])*(float)0.268/10;
                //sonar[index] = ((float)shortSonar[index])*(float)0.268/1000;
                sonarNew[index] = true;
                if (showSonar) {
                    System.out.print(" " + index + ":" + sonars[index]);
                }
            }
        }
        if (showSonar) {
            System.out.println();
            System.out.flush();
        }
        //Timer = readBuffer[20+3*i,20+3*i+2];

        Analog = (short) (readBuffer[23 + 3 * i + 2] & 0xFF);
        Digin = (short) (readBuffer[23 + 3 * i + 3] & 0xFF);
        if (usePeoplebot) {
            if ((int) (readBuffer[23 + 3 * i + 3] & leftIRBitMask) > 0) {
                ireds[0] = false;
            } else {
                ireds[0] = true;
            }
            if ((int) (readBuffer[23 + 3 * i + 3] & rightIRBitMask) > 0) {
                ireds[1] = false;
            } else {
                ireds[1] = true;
            }
        } else {
            ireds[0] = false;
            ireds[1] = false;
        }
        if (showDigin) {
            System.out.print("Digital-in bits:" + Integer.toBinaryString((int) Digin));
            System.out.println("; left IR=" + ireds[0] + ", right IR=" + ireds[1]);
        }
        Digout = (short) (readBuffer[23 + 3 * i + 4] & 0xFF);

        //Checksum = toShortLocal(readBuffer[23+3*i+5], readBuffer[23+3*i+6]);
        //compChecksum = calcCheckSum(readBuffer);
        //System.out.println("checksum " + (short) compChecksum + " " + Checksum);
        //if (Checksum != (short) compChecksum)
        //	System.out.println("Error receiving package (bad checksum)");
        if (useBumpAlarm) {
            if (bumpAlarm && !handlingBumpAlarm) {
                handleBumpAlarm();
            }
        }
    }

    /**
     * calculates the checksum for the given buffer
     *
     * @param readBuffer an array of bytes to calculate the checksum of
     * @return the checksum
     */
    public int calcCheckSum(byte readBuffer[]) {
        int n, i = 3;
        int c = 0;
        n = readBuffer[2] & 0xFF;
        n -= 2;
        while (n > 1) {
            c += toShortLocal(readBuffer[i], readBuffer[i + 1]);
            c = (c & 0xffff);
            n -= 2;
            i += 2;
        }
        if (n > 0) {
            c = (c ^ (readBuffer[i] & 0xFF));
        }
        return c;
    }

    // TODO: why is this even here? we don't handle the laser...
    /**
     * calculates the LRF checksum for the given buffer
     * (most ugliness here is either java's fault or SICK's fault)
     *
     * @param buffer an array of bytes to calculate the LRF checksum of
     * @return the checksum
     */
    public static short calcLRFCheckSum(byte[] buffer) {
        short uCrc16;
        byte[] abData = new byte[2];
        int uLen = buffer[0];
        int bufferCounter = 1;

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
            uCrc16 ^= toShortLocal(abData[0], abData[1]);
        }
        return uCrc16;
    }

    /*
    The following are functions that send commands to the Pioneer robot.
    Except for the start-up commands, the robot does not acknowledge any
    other commands.  Errors are expected in communication, so each of
    these functions may fail.  For purposes of sending information, each
    package has the following format:
    0xfa
    0xfb
    Command number
    Argument type
    Argument
    Checksum

    Good luck
     */
    /**
     * This is the general command that everything uses to send buffers of bytes
     * to the robot.
     *
     * @param buf the buffer of <code>byte</code>'s to send
     */
    protected void sendBuffer(byte[] buf) {
        if (portOpenOk) {
            synchronized (outputStreamGuard) {
                try {
                    long t1 = System.currentTimeMillis();
                    outputStream.write(buf);
                    outputStream.flush();
//					if (buf[3] != 0)
//						System.out.println("ROBOT command " + (System.currentTimeMillis()-t1));
                } catch (IOException e) {
                    System.err.println("Exception occurred while sending bytes: " + e);
                }
            }
        }
    }

    /**
     * sends the three commands required to initiate work with the robot;
     * it sends the three commands individually and waits for reply after each one
     */
    public int startUp() {
        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, 3, 0, 0, 0};
        byte[] checkBuffer = new byte[7];
        int check;
        int i, numBytes;
        //    boolean error1 = false, error2 = false, error3 = false;

        check = calcCheckSum(commandBuffer);
        commandBuffer[4] = (byte) (check >>> 8);
        commandBuffer[5] = (byte) (check & 0xFF);
        if (verbose2) {
            System.out.println("Pioneer: portOpenOk=" + portOpenOk);
        }
        if (portOpenOk) {
            sendBuffer(commandBuffer);

            // listen for echo
            numBytes = 0;
            for (i = 0; i < 6; i++) {
                try {
                    while (inputStream.available() == 0);
                    numBytes += inputStream.read(checkBuffer, numBytes, 1);
                } catch (IOException e) {
                    System.err.println("IO ERROR" + e);
                }
            }
            // Check for packet integrity - to be added

            commandBuffer[3] = 1;
            check = calcCheckSum(commandBuffer);
            commandBuffer[4] = (byte) (check >>> 8);
            commandBuffer[5] = (byte) (check & 0xFF);
            sendBuffer(commandBuffer);

            // listen for echo

            numBytes = 0;
            for (i = 0; i < 6; i++) {
                try {
                    while (inputStream.available() == 0);
                    numBytes += inputStream.read(checkBuffer, numBytes, 1);
                } catch (IOException e) {
                    System.err.println("IO ERROR" + e);
                }
            }
            // insert check for packet integrity
            commandBuffer[3] = 2;
            check = calcCheckSum(commandBuffer);
            commandBuffer[4] = (byte) (check >>> 8);
            commandBuffer[5] = (byte) (check & 0xFF);
            sendBuffer(commandBuffer);

            // listen for echo

            numBytes = 0;
            for (i = 0; i < 6; i++) {
                try {
                    while (inputStream.available() == 0);
                    numBytes += inputStream.read(checkBuffer, numBytes, 1);
                } catch (IOException e) {
                    System.err.println("IO ERROR" + e);
                }
            }
            // insert check for packet integrity
        }
        return 0;
    }

    // Commands that take no arguments in the manual
    /**
     * Sends the PULSE command to reset the server watchdog (in essence,
     * a nop). THIS COMMAND SHOULD BE USED OFTEN!!! (especially if
     * commands to the robot are sent relatively rarely) */
    public void sendPulse() {
        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, 3, 0, 0, 0};
        int check = calcCheckSum(commandBuffer);

        commandBuffer[4] = (byte) (check >>> 8);
        commandBuffer[5] = (byte) (check & 0xFF);
        sendBuffer(commandBuffer);
    }

    /**
     * Send the OPEN command,
     * which starts the controller.
     */
    public void sendOpen() {
        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, 3, 1, 0, 0};
        int check;
        check = calcCheckSum(commandBuffer);
        commandBuffer[4] = (byte) (check >>> 8);
        commandBuffer[5] = (byte) (check & 0xFF);
        sendBuffer(commandBuffer);
    }

    /**
     * Send the CLOSE command,
     * which closes the server and client connections.
     */
    public void sendClose() {
        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, 3, 2, 0, 0};
        int check;
        check = calcCheckSum(commandBuffer);
        commandBuffer[4] = (byte) (check >>> 8);
        commandBuffer[5] = (byte) (check & 0xFF);
        sendBuffer(commandBuffer);
    }

    /**
     * Send the SETO command,
     * which resets the robot's origin.
     */
    public void sendSeto() throws RemoteException {

        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, 3, 7, 0, 0};
        int check;
        check = calcCheckSum(commandBuffer);
        commandBuffer[4] = (byte) (check >>> 8);
        commandBuffer[5] = (byte) (check & 0xFF);
        sendBuffer(commandBuffer);
    }

    /**
     * sends the STOP command,
     * which stops the motors (the motors remain enabled).
     */
    private void sendStop() throws RemoteException {

        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, 3, 29, 0, 0};
        int check;
        check = calcCheckSum(commandBuffer);
        commandBuffer[4] = (byte) (check >>> 8);
        commandBuffer[5] = (byte) (check & 0xFF);
        sendBuffer(commandBuffer);
    }

    /**
     * sends the E_STOP command,
     * which overrides deceleration and makes an emergency stop.
     */
    private void sendEstop() throws RemoteException {

        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, 3, 55, 0, 0};
        int check;
        check = calcCheckSum(commandBuffer);
        commandBuffer[4] = (byte) (check >>> 8);
        commandBuffer[5] = (byte) (check & 0xFF);
        sendBuffer(commandBuffer);
    }

    /**
     * sends the STEP command,
     * which puts the robot in single-step mode (simulator only)
     */
    public void sendStep() throws RemoteException {

        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, 3, 64, 0, 0};
        int check;
        check = calcCheckSum(commandBuffer);
        commandBuffer[4] = (byte) (check >>> 8);
        commandBuffer[5] = (byte) (check & 0xFF);
        sendBuffer(commandBuffer);
    }

    /**
     * sends the PLAYLIST command,
     * which requests the AmigoBot sound playlist
     */
    public void sendPlaylist() throws RemoteException {

        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, 3, 90, 0, 0};
        int check;
        check = calcCheckSum(commandBuffer);
        commandBuffer[4] = (byte) (check >>> 8);
        commandBuffer[5] = (byte) (check & 0xFF);
        sendBuffer(commandBuffer);
    }

    // Commands that take a string in the manual
    /**
     * sends the POLLING command,
     * which sets the sonar polling sequence
     *
     * @param stringBuffer the sequence
     */
    public void sendPolling(byte stringBuffer[]) throws RemoteException {

        byte[] commandBuffer = new byte[stringBuffer[0] + 7];
        int check;
        int i;
        commandBuffer[0] = (byte) 0xfa;
        commandBuffer[1] = (byte) 0xfb;
        commandBuffer[2] = (byte) (stringBuffer[0] + 4);
        commandBuffer[3] = 3;
        commandBuffer[4] = (byte) 0x2b;
        for (i = 1; i < stringBuffer[0]; i++) {
            commandBuffer[i + 4] = stringBuffer[i];
        }
        check = calcCheckSum(commandBuffer);
        commandBuffer[i + 4] = (byte) (check >>> 8);
        commandBuffer[i + 5] = (byte) (check & 0xFF);
        sendBuffer(commandBuffer);
    }

    /**
     * send SAY command,
     * which makes the robot "speak" (beep)
     *
     * @param stringBuffer as many as 20 pairs of duration/tone pairs
     */
    public void sendSay(byte stringBuffer[]) throws RemoteException {

        byte[] commandBuffer = new byte[stringBuffer[0] + 7];
        int check;
        int i;
        commandBuffer[0] = (byte) 0xfa;
        commandBuffer[1] = (byte) 0xfb;
        commandBuffer[2] = (byte) (stringBuffer[0] + 4);
        commandBuffer[3] = 15;
        commandBuffer[4] = (byte) 0x2b;
        for (i = 0; i < stringBuffer[0]; i++) {
            commandBuffer[i + 5] = stringBuffer[i];
        }
        check = calcCheckSum(commandBuffer);
        commandBuffer[i + 5] = (byte) (check >>> 8);
        commandBuffer[i + 6] = (byte) (check & 0xFF);
        sendBuffer(commandBuffer);
    }

    /**
     * sends the TTY2 command,
     * which sends a string to the AUX port
     *
     * @param stringBuffer the string to send
     * (the first byte must be the length of the string)
     */
    public void sendTty2(byte stringBuffer[]) throws RemoteException {
        byte[] commandBuffer = new byte[stringBuffer[0] + 8];
        int check;
        int i;
        commandBuffer[0] = (byte) 0xfa;
        commandBuffer[1] = (byte) 0xfb;
        commandBuffer[2] = (byte) (stringBuffer[0] + 5);
        commandBuffer[3] = 42;
        commandBuffer[4] = (byte) 0x2b;
        for (i = 0; i <= stringBuffer[0]; i++) {
            commandBuffer[i + 5] = stringBuffer[i];
        }
        check = calcCheckSum(commandBuffer);
        commandBuffer[i + 5] = (byte) (check >>> 8);
        commandBuffer[i + 6] = (byte) (check & 0xFF);
        //		System.out.print("sendtty2: ");
        //		for (int j=0; j<stringBuffer[0]+8; j++)
        //			System.out.print(Integer.toHexString(commandBuffer[j]&0xFF)+" ");
        //		System.out.println();
        sendBuffer(commandBuffer);
    }

    // Commands that take an int in the manual
    /**
     * sends the SETA command,
     * which sets the translational acceleration parameter if positive
     * or deceleration if negative; in millimeters per second squared
     *
     * @param arg the translational acceleration parameter
     */
    public void sendSeta(short arg) throws RemoteException {

        // MS: fixed
        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, (byte) 6, (byte) 5, 0x3b, 0, 0, 0, 0};

        if (arg < 0) {
            commandBuffer[4] = 0x1b;
            arg = (short) ((-1) * arg);
        }

        int check;
        commandBuffer[6] = (byte) (arg >>> 8);
        commandBuffer[5] = (byte) (arg & 0xFF);
        check = calcCheckSum(commandBuffer);
        commandBuffer[7] = (byte) (check >>> 8);
        commandBuffer[8] = (byte) (check & 0xFF);
        sendBuffer(commandBuffer);
    }

    /**
     * sends the SETV command,
     * which sets the maximum translational velocity, in millimeters per second
     *
     * @param arg the maximum translational velocity
     */
    public void sendSetv(short arg) throws RemoteException {

        // MS: fixed
        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, (byte) 6, (byte) 6, 0x3b, 0, 0, 0, 0};

        if (arg < 0) {
            commandBuffer[4] = 0x1b;
            arg = (short) ((-1) * arg);
        }

        int check;
        commandBuffer[6] = (byte) (arg >>> 8);
        commandBuffer[5] = (byte) (arg & 0xFF);
        check = calcCheckSum(commandBuffer);
        commandBuffer[7] = (byte) (check >>> 8);
        commandBuffer[8] = (byte) (check & 0xFF);
        sendBuffer(commandBuffer);
    }

    /** Convert individual wheel speeds to translational velocity. */
    private short lrwvToTV(int l, int r) {
        // average the two speeds
        int tv = ((l * 10) + (r * 10)) / 2;
        return (short) tv;
    }

    /** Convert individual wheel speeds to rotational velocity. */
    private short lrwvToRV(int l, int r) {
        //	//
        //	double tv = ((l * 1000.0) + (r * 1000.0)) / 2;
        //	rotVel = forVel * ((r - l) / (l * wheelBase));
        //	return (short)tv;
        return (short) 0;
    }

    /**
     * sends the SETRV command,
     * which sets the maximum rotational velocity in degrees per second
     *
     * @param arg the maximum rotational velocity
     */
    public void sendSetrv(short arg) throws RemoteException {

        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, (byte) 6, (byte) 10, 0x3b, 0, 0, 0, 0};

        if (arg < 0) {
            commandBuffer[4] = 0x1b;
            arg = (short) ((-1) * arg);
        }

        int check;
        commandBuffer[6] = (byte) (arg >>> 8);
        commandBuffer[5] = (byte) (arg & 0xFF);
        check = calcCheckSum(commandBuffer);
        commandBuffer[7] = (byte) (check >>> 8);
        commandBuffer[8] = (byte) (check & 0xFF);
        sendBuffer(commandBuffer);
    }

    /**
     * sends the VEL command,
     * which makes the robot move forward (positive) or backward (negative)
     *
     * @param arg velocity in millimeters per second
     */
    private void sendVel(short arg) throws RemoteException {
        if (useBumpAlarm && bumpAlarm) {
            return;
        }
        // MS: fixed, to check for positive and negative values...
        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, (byte) 6, (byte) 11, 0x3b, 0, 0, 0, 0};

        if (arg < 0) {
            commandBuffer[4] = 0x1b;
            arg = (short) ((-1) * arg);
        }

        int check;
        short ch1;
        commandBuffer[6] = (byte) (arg >>> 8);
        commandBuffer[5] = (byte) (arg & 0xFF);
        //System.out.println ("Sending Velocity: " + arg + "  commandBuffer[6] " + commandBuffer[6] + "  commandBuffer[5] " + commandBuffer[5]);
        check = calcCheckSum(commandBuffer);
        ch1 = (short) check;
        //System.out.println("Checksum " + ch1 + " " + arg);
        commandBuffer[7] = (byte) (ch1 >>> 8);
        commandBuffer[8] = (byte) (ch1 & 0xFF);
        sendBuffer(commandBuffer);
    }

    /**
     * sends the HEAD command,
     * which turns the robot to an absolute heading, positive being counterclockwise
     *
     * @param arg the heading, in degrees
     */
    public void sendHead(short arg) throws RemoteException {
        if (useBumpAlarm && bumpAlarm) {
            return;
        }
        // MS: fixed
        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, (byte) 6, (byte) 12, 0x3b, 0, 0, 0, 0};

        if (arg < 0) {
            commandBuffer[4] = 0x1b;
            arg = (short) ((-1) * arg);
        }

        int check;
        commandBuffer[6] = (byte) (arg >>> 8);
        commandBuffer[5] = (byte) (arg & 0xFF);
        check = calcCheckSum(commandBuffer);
        commandBuffer[7] = (byte) (check >>> 8);
        commandBuffer[8] = (byte) (check & 0xFF);
        sendBuffer(commandBuffer);
    }

    /**
     * sends the MOVE command,
     * which moves the robot forward or backward a distance in millimeters in the direction
     * of the current heading, positive being forward, negative being backward
     *
     * @param arg the relative heading, in degrees
     */
    public void sendMove(short arg) throws RemoteException {
        if (useBumpAlarm && bumpAlarm) {
            return;
        }
        // MS: fixed
        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, (byte) 6, (byte) 8, 0x3b, 0, 0, 0, 0};

        if (arg < 0) {
            commandBuffer[4] = 0x1b;
            arg = (short) ((-1) * arg);
        }

        int check;
        commandBuffer[6] = (byte) (arg >>> 8);
        commandBuffer[5] = (byte) (arg & 0xFF);
        check = calcCheckSum(commandBuffer);
        commandBuffer[7] = (byte) (check >>> 8);
        commandBuffer[8] = (byte) (check & 0xFF);
        sendBuffer(commandBuffer);
    }

    /**
     * sends the DHEAD command,
     * which turns the robot to a heading relative to
     * the current heading, positive being counterclockwise.
     *
     * @param arg the relative heading, in degrees
     */
    public void sendDhead(short arg) throws RemoteException {
        if (useBumpAlarm && bumpAlarm) {
            return;
        }
        // MS: fixed
        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, (byte) 6, (byte) 13, 0x3b, 0, 0, 0, 0};

        if (arg < 0) {
            commandBuffer[4] = 0x1b;
            arg = (short) ((-1) * arg);
        }

        int check;
        commandBuffer[6] = (byte) (arg >>> 8);
        commandBuffer[5] = (byte) (arg & 0xFF);
        check = calcCheckSum(commandBuffer);
        commandBuffer[7] = (byte) (check >>> 8);
        commandBuffer[8] = (byte) (check & 0xFF);
        sendBuffer(commandBuffer);
    }

    /**
     * converts two unsigned bytes into a short
     *
     * @param a an unsigned <code>byte</code>
     * @param b an unsigned <code>byte</code>
     * @return a <code>short</code>
     * @exception RemoteException if the remote invocation fails
     */
    public short toShort(byte a, byte b) throws RemoteException {
        return (short) (((a & 0xFF) << 8) | (b & 0xFF));
    }

    /**
     * converts two unsigned bytes into a short
     *
     * @param a an unsigned <code>byte</code>
     * @param b an unsigned <code>byte</code>
     * @return a <code>short</code>
     */
    public static short toShortLocal(byte a, byte b) {
        return (short) (((a & 0xFF) << 8) | (b & 0xFF));
    }

    /**
     * sends a CONFIG command,
     * which requests a configuration SIP
     *
     * @param arg send 1 to request configuration SIP
     */
    public boolean sendConfig(short arg) throws RemoteException {

        // MS: IS THIS RIGHTS??? WHAT ABOUT SIGNS?
        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, 6, 18, (byte) 0x1b, 0, 0, 0, 0};
        byte[] config = null;
        int check, index, i, count = 5;
        short temp, length, strLen;

        commandBuffer[6] = (byte) (arg >>> 8);
        commandBuffer[5] = (byte) (arg & 0xFF);
        check = calcCheckSum(commandBuffer);
        commandBuffer[7] = (byte) (check >>> 8);
        commandBuffer[8] = (byte) (check & 0xFF);
        if (portOpenOk) {
            while (count > 0 && (config == null || config[3] != (short) 32)) {
                if (debug) {
                    System.out.print("sendConfig(): sending [");
                    for (int j = 0; j < commandBuffer.length; j++) {
                        System.out.print(Integer.toHexString(commandBuffer[j]) + " ");
                    }
                    System.out.println("]");
                }
                sendBuffer(commandBuffer);
                //config[2] = (byte) 0;
                //while (config[3] != (short) 32) {
                try {
                    config = receive();
                    if (config == null || config[3] != (short) 32) {
                        count--;
                        try {
                            Thread.sleep(20);
                        } catch (Exception ignore) {
                        }
                    }
                } catch (DataFormatException e) {
                }
                //}
            }
            if (count < 1) {
                System.out.println("sendConfig timed out!");
                sendClose();
                return false;
            }

            // Read in the fixed strings
            strLen = (short) (config[3] & 0xFF);
            index = 0;
            //  	for (i = 0; i < 110; i++)
            //  	    System.out.print((char)config[i]);
            //  	System.out.println(" ");
            //  	for (i = 0; i < 105; i++)
            //  	    System.out.print((short)config[i] + " " );
            //  	System.out.println(" ");

            Type = "";
            for (i = 0; i < 7; i++) {
                Type += (char) config[4 + i];
            }
            index = i + 4;

            strLen = config[index++];
            //	System.out.println("String " + (config[index] & 0xFF));

            Subtype = "";
            for (i = 0; i < 4; i++) {
                Subtype += (char) config[index + i];
            }
            //	System.out.println("Subtype " + Subtype);

            index += i;
            strLen = config[index++];
            serial = "";
            for (i = 0; i < 8; i++) {
                serial += (char) config[index + i];
            }
            index += i;
            index++;

            //	System.out.println("serial " + serial);

            // Read in the fixed numerical values

            fourMotors = (short) (config[index] & 0xFF);
            index++;

            rotVelTop = toShortLocal(config[index + 1], config[index]);
            index += 2;

            transVelTop = toShortLocal(config[index + 1], config[index]);
            index += 2;

            rotAccTop = toShortLocal(config[index + 1], config[index]);
            index += 2;

            transAccTop = toShortLocal(config[index + 1], config[index]);
            index += 2;

            pwmMax = toShortLocal(config[index + 1], config[index]);
            index += 2;

            encoder = (short) (config[index] & 0xFF);
            index++;

            //encoder = toShortLocal(config[index+1], config[index]);
            //index += 2;

            // Read in variable string

            //	index = 36;
            //	strLen = config[index];
            name = "";
            while ((char) config[index] != '\0') {
                name += (char) config[index];
                index++;
            }
            index++;

            // Read in three bytes

            sInfoCycle = (short) (config[index++] & 0xFF);
            HostBaud = (short) (config[index++] & 0xFF);
            AuxBaud = (short) (config[index++] & 0xFF);

            // Read in two integers
            HasGripper = toShortLocal(config[index + 1], config[index]);
            index += 2;
            //System.out.println("HasGripper " + HasGripper);

            FrontSonar = toShortLocal(config[index + 1], config[index]);
            index += 2;

            // Read byte

            RearSonar = (short) (config[index] & 0xFF);
            index++;

            // Read three integers

            LowBattery = toShortLocal(config[index + 1], config[index]);
            index += 2;

            RevCount = toShortLocal(config[index + 1], config[index]);
            index += 2;

            WatchDog = toShortLocal(config[index + 1], config[index]);
            index += 2;

            // Read byte

            P2Mpacs = (short) (config[index] & 0xFF);
            index++;

            // Read remaining integers

            StallVal = toShortLocal(config[index + 1], config[index]);
            index += 2;

            StallCount = toShortLocal(config[index + 1], config[index]);
            index += 2;

            CompassT = toShortLocal(config[index + 1], config[index]);
            index += 2;

            //  	CompX = toShortLocal(config[index+1], config[index]);
            //  	index += 2;

            CompX = (short) (config[index] & 0xFF);
            index++;

            //  	CompY = toShortLocal(config[index+1], config[index]);
            //  	index += 2;

            CompY = (short) (config[index] & 0xFF);
            index++;

            rotVelMax = toShortLocal(config[index + 1], config[index]);
            index += 2;

            transVelMax = toShortLocal(config[index + 1], config[index]);
            index += 2;

            rotAcc = toShortLocal(config[index + 1], config[index]);
            index += 2;

            rotDecel = toShortLocal(config[index + 1], config[index]);
            index += 2;

            rotKp = toShortLocal(config[index + 1], config[index]);
            index += 2;

            rotKv = toShortLocal(config[index + 1], config[index]);
            index += 2;

            rotKi = toShortLocal(config[index + 1], config[index]);
            index += 2;

            transAcc = toShortLocal(config[index + 1], config[index]);
            index += 2;

            //	System.out.println("transAcc " + transAcc);

            transDecel = toShortLocal(config[index + 1], config[index]);
            index += 2;

            transKp = toShortLocal(config[index + 1], config[index]);
            index += 2;

            transKv = toShortLocal(config[index + 1], config[index]);
            index += 2;

            transKi = toShortLocal(config[index + 1], config[index]);
            index += 2;

            //  	JoyVelMax = toShortLocal(config[index+1], config[index]);
            //  	index += 2;

            JoyVelMax = (short) (config[index] & 0xFF);
            index++;

            JoyRVelMax = (short) (config[index] & 0xFF);


            //  	JoyRVelMax = toShortLocal(config[index+1], config[index]);
            //  	index += 2;
        }
        return true;
    }

    /**
     * sends an IOREQUEST command,
     * which requests one (1) or a continuous stream (>1), or turns
     * off the ongoing stream (0) of IO SIPs
     *
     * @param arg which thing to do (0, 1, >1)
     */
    public void sendIORequest(short arg) throws RemoteException {

        // MS: fixed
        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, (byte) 6, (byte) 40, 0x3b, 0, 0, 0, 0};

        if (arg < 0) {
            commandBuffer[4] = 0x1b;
            arg = (short) ((-1) * arg);
        }

        int check;
        commandBuffer[6] = (byte) (arg >>> 8);
        commandBuffer[5] = (byte) (arg & 0xFF);
        check = calcCheckSum(commandBuffer);
        commandBuffer[7] = (byte) (check >>> 8);
        commandBuffer[8] = (byte) (check & 0xFF);
        sendBuffer(commandBuffer);
    }

    /**
     * sends an ENCODER command,
     * which requests one (1) or a continuous stream (>1), or turns
     * off the ongoing stream (0) of Encoder SIPs
     *
     * @param arg which thing to do (0, 1, >1)
     */
    public void sendEncoder(short arg) throws RemoteException {

        // MS: fixed
        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, (byte) 6, (byte) 19, 0x3b, 0, 0, 0, 0};

        if (arg < 0) {
            commandBuffer[4] = 0x1b;
            arg = (short) ((-1) * arg);
        }

        int check;
        commandBuffer[6] = (byte) (arg >>> 8);
        commandBuffer[5] = (byte) (arg & 0xFF);
        check = calcCheckSum(commandBuffer);
        commandBuffer[7] = (byte) (check >>> 8);
        commandBuffer[8] = (byte) (check & 0xFF);
        sendBuffer(commandBuffer);
    }

    /**
     * sends the RVEL command,
     * which sets the rotational velocity
     *
     * @param arg the rotational velocity, in +/- degrees per second
     */
    public void sendRVel(short arg) throws RemoteException {
        if (useBumpAlarm && bumpAlarm) {
            return;
        }
        // MS: fixed
        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, (byte) 6, (byte) 21, 0x3b, 0, 0, 0, 0};

        if (arg < 0) {
            commandBuffer[4] = 0x1b;
            arg = (short) ((-1) * arg);
        }

        int check;
        commandBuffer[6] = (byte) (arg >>> 8);
        commandBuffer[5] = (byte) (arg & 0xFF);
        check = calcCheckSum(commandBuffer);
        commandBuffer[7] = (byte) (check >>> 8);
        commandBuffer[8] = (byte) (check & 0xFF);
        sendBuffer(commandBuffer);
    }

    /**
     * sends the DCHEAD command,
     * which sets the heading setpoint relative to the last setpoint
     *
     * @param arg +/- degrees (positive being counterclockwise)
     */
    public void sendDchead(short arg) throws RemoteException {
        if (useBumpAlarm && bumpAlarm) {
            return;
        }
        // MS: fixed
        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, (byte) 6, (byte) 22, 0x3b, 0, 0, 0, 0};

        if (arg < 0) {
            commandBuffer[4] = 0x1b;
            arg = (short) ((-1) * arg);
        }

        int check;
        commandBuffer[6] = (byte) (arg >>> 8);
        commandBuffer[5] = (byte) (arg & 0xFF);
        check = calcCheckSum(commandBuffer);
        commandBuffer[7] = (byte) (check >>> 8);
        commandBuffer[8] = (byte) (check & 0xFF);
        sendBuffer(commandBuffer);
    }

    /**
     * sends the SETRA command,
     * which sets the rotational acceleration (positive parameter) or deceleration
     * (negative parameter) in degrees per second per second
     *
     * @param arg the rotational acc/dec -eleration
     */
    public void sendSetra(short arg) throws RemoteException {

        // Send
        // MS: fixed
        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, (byte) 6, (byte) 23, 0x3b, 0, 0, 0, 0};

        if (arg < 0) {
            commandBuffer[4] = 0x1b;
            arg = (short) ((-1) * arg);
        }

        int check;
        commandBuffer[6] = (byte) (arg >>> 8);
        commandBuffer[5] = (byte) (arg & 0xFF);
        check = calcCheckSum(commandBuffer);
        commandBuffer[7] = (byte) (check >>> 8);
        commandBuffer[8] = (byte) (check & 0xFF);
        sendBuffer(commandBuffer);
    }

    /** Enable/disable the sonar sensors.
     * @param arg 1 (enable) or 0 (disable) */
    public void setSonar(int arg) throws RemoteException {
        sendSonar((short) arg);
    }

    /**
     * sends the SONAR command,
     * which enables or disables the sonar sensors
     *
     * @param arg 1 (enable) or 0 (disable)
     */
    private void sendSonar(short arg) throws RemoteException {

        // MS: fixed
        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, (byte) 6, (byte) 28, 0x3b, 0, 0, 0, 0};

        if (arg < 0) {
            commandBuffer[4] = 0x1b;
            arg = (short) ((-1) * arg);
        }

        int check;
        commandBuffer[6] = (byte) (arg >>> 8);
        commandBuffer[5] = (byte) (arg & 0xFF);
        check = calcCheckSum(commandBuffer);
        commandBuffer[7] = (byte) (check >>> 8);
        commandBuffer[8] = (byte) (check & 0xFF);
        sendBuffer(commandBuffer);
    }

    /**
     * sends the DIGOUT command,
     * which selects output ports for changes and sets or resets the selected port
     *
     * the most significant bits is a byte mask that selects output ports
     * the least significant bits set (1) or reset (0) the selected port(s)
     *
     * @param arg bits
     */
    private void sendDigout(short arg) throws RemoteException {

        // MS: fixed
        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, (byte) 6, (byte) 30, 0x3b, 0, 0, 0, 0};

        if (arg < 0) {
            commandBuffer[4] = 0x1b;
            arg = (short) ((-1) * arg);
        }
        int check;
        commandBuffer[6] = (byte) (arg >>> 8);
        commandBuffer[5] = (byte) (arg & 0xFF);
        check = calcCheckSum(commandBuffer);
        commandBuffer[7] = (byte) (check >>> 8);
        commandBuffer[8] = (byte) (check & 0xFF);
        sendBuffer(commandBuffer);
    }

    /**
     * sends the VEL2 command,
     * which sets the independent wheel velocities
     *
     * most significant bits are for the left wheel
     * least significant bits are for the right wheel
     * the values are in +/- 2 centimeters per second increments
     *
     * @param arg bits
     */
    private void sendVel2(short arg) throws RemoteException {
        if (useBumpAlarm && bumpAlarm) {
            return;
        }
        // MS: fixed
        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, (byte) 6, (byte) 32, 0x3b, 0, 0, 0, 0};

        if (arg < 0) {
            commandBuffer[4] = 0x1b;
            arg = (short) ((-1) * arg);
        }
        int check;
        commandBuffer[6] = (byte) (arg >>> 8);
        commandBuffer[5] = (byte) (arg & 0xFF);
        check = calcCheckSum(commandBuffer);
        commandBuffer[7] = (byte) (check >>> 8);
        commandBuffer[8] = (byte) (check & 0xFF);
        sendBuffer(commandBuffer);
    }

    /**
     * sends the GRIPPER command,
     * which is described in the Pioneer Gripper manuals
     *
     * @param arg argument
     */
    public void sendGripper(short arg) throws RemoteException {

        // MS: fixed
        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, (byte) 6, (byte) 33, 0x3b, 0, 0, 0, 0};

        if (arg < 0) {
            commandBuffer[4] = 0x1b;
            arg = (short) ((-1) * arg);
        }
        int check;
        commandBuffer[6] = (byte) (arg >>> 8);
        commandBuffer[5] = (byte) (arg & 0xFF);
        check = calcCheckSum(commandBuffer);
        commandBuffer[7] = (byte) (check >>> 8);
        commandBuffer[8] = (byte) (check & 0xFF);
        sendBuffer(commandBuffer);
    }

    /**
     * sends the ADSEL command,
     * which selects the A/D port number for analog value in the SIP;
     * the selected port is reported in the SIP Timer value
     *
     * @param arg the port
     */
    public void sendAdsel(short arg) throws RemoteException {

        // MS: fixed
        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, (byte) 6, (byte) 35, 0x3b, 0, 0, 0, 0};

        if (arg < 0) {
            commandBuffer[4] = 0x1b;
            arg = (short) ((-1) * arg);
        }

        int check;
        commandBuffer[6] = (byte) (arg >>> 8);
        commandBuffer[5] = (byte) (arg & 0xFF);
        check = calcCheckSum(commandBuffer);
        commandBuffer[7] = (byte) (check >>> 8);
        commandBuffer[8] = (byte) (check & 0xFF);
        sendBuffer(commandBuffer);
    }

    /**
     * sends the GRIPPERVAL command,
     * which is explained in the P2 Gripper Manual
     *
     * @param arg argument
     */
    public void sendGripperval(short arg) throws RemoteException {

        // MS: fixed
        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, (byte) 6, (byte) 36, 0x3b, 0, 0, 0, 0};

        if (arg < 0) {
            commandBuffer[4] = 0x1b;
            arg = (short) ((-1) * arg);
        }
        int check;
        commandBuffer[6] = (byte) (arg >>> 8);
        commandBuffer[5] = (byte) (arg & 0xFF);
        check = calcCheckSum(commandBuffer);
        commandBuffer[7] = (byte) (check >>> 8);
        commandBuffer[8] = (byte) (check & 0xFF);
        sendBuffer(commandBuffer);
    }

    /**
     * sends the GRIPREQUEST command,
     * which requests one (1) or a continuous stream (>1),
     * or stops the continuous stream (0) of Gripper SIPs
     *
     * @param arg command - 0, 1, or >1
     */
    public void sendGriprequest(short arg) throws RemoteException {

        // MS: fixed
        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, (byte) 6, (byte) 37, 0x3b, 0, 0, 0, 0};

        if (arg < 0) {
            commandBuffer[4] = 0x1b;
            arg = (short) ((-1) * arg);
        }
        int check;
        commandBuffer[6] = (byte) (arg >>> 8);
        commandBuffer[5] = (byte) (arg & 0xFF);
        check = calcCheckSum(commandBuffer);
        commandBuffer[7] = (byte) (check >>> 8);
        commandBuffer[8] = (byte) (check & 0xFF);
        sendBuffer(commandBuffer);
    }

    /**
     * sends the PTUPOS command,
     * which doesn't have a good description - here's what it says:
     *
     * msb is the port number 1-4 and lsb is the pulse width in
     * 10 mu-second units; Version 1J (which we are using) uses RC-servo
     * 40ms duty cycle
     *
     * @param arg argument
     */
    public void sendPtupos(short arg) throws RemoteException {

        // MS: fixed
        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, (byte) 6, (byte) 41, 0x3b, 0, 0, 0, 0};

        if (arg < 0) {
            commandBuffer[4] = 0x1b;
            arg = (short) ((-1) * arg);
        }
        int check;
        commandBuffer[6] = (byte) (arg >>> 8);
        commandBuffer[5] = (byte) (arg & 0xFF);
        check = calcCheckSum(commandBuffer);
        commandBuffer[7] = (byte) (check >>> 8);
        commandBuffer[8] = (byte) (check & 0xFF);
        sendBuffer(commandBuffer);
    }

    /**
     * sends a GETAUX command,
     * which makes the robot retrieve 1-200 bytes from the AUX serial
     * channel (0 flushes the aux serial input buffer)
     *
     * @param arg number of bytes to retrieve, or 0 to flush
     */
    public void sendGetaux(short arg) throws RemoteException {
        // MS: fixed
        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, (byte) 6, (byte) 43, 0x3b, 0, 0, 0, 0};

        if (arg < 0) {
            commandBuffer[4] = 0x1b;
            arg = (short) ((-1) * arg);
        }
        int check;
        commandBuffer[6] = (byte) (arg >>> 8);
        commandBuffer[5] = (byte) (arg & 0xFF);
        check = calcCheckSum(commandBuffer);
        commandBuffer[7] = (byte) (check >>> 8);
        commandBuffer[8] = (byte) (check & 0xFF);
        //		System.out.print("getaux: ");
        //		for (int j=0; j<9; j++)
        //			System.out.print(Integer.toHexString(commandBuffer[j]&0xFF)+" ");
        //		System.out.println();
        sendBuffer(commandBuffer);
    }

    /**
     * sends the BUMP_STALL command,
     * which stops and registers a stall if the front (1), rear (2)
     * or either (3) bump-ring is contacted; off (the default) is 0
     *
     * @param arg off (0), front (1), rear (2), either (3)
     */
    public void sendBump_stall(short arg) throws RemoteException {

        // MS: fixed
        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, (byte) 6, (byte) 44, 0x3b, 0, 0, 0, 0};

        if (arg < 0) {
            commandBuffer[4] = 0x1b;
            arg = (short) ((-1) * arg);
        }
        int check;
        commandBuffer[6] = (byte) (arg >>> 8);
        commandBuffer[5] = (byte) (arg & 0xFF);
        check = calcCheckSum(commandBuffer);
        commandBuffer[7] = (byte) (check >>> 8);
        commandBuffer[8] = (byte) (check & 0xFF);
        sendBuffer(commandBuffer);
    }

    /**
     * sends the TCM2 command,
     * which sends TCM2 module commands (see TCM2 Manual for details)
     *
     * @param arg argument
     */
    public void sendTcm2(short arg) {
        // MS: fixed
        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, (byte) 6, (byte) 45, 0x3b, 0, 0, 0, 0};

        if (arg < 0) {
            commandBuffer[4] = 0x1b;
            arg = (short) ((-1) * arg);
        }
        int check;
        commandBuffer[6] = (byte) (arg >>> 8);
        commandBuffer[5] = (byte) (arg & 0xFF);
        check = calcCheckSum(commandBuffer);
        commandBuffer[7] = (byte) (check >>> 8);
        commandBuffer[8] = (byte) (check & 0xFF & 0xFF & 0xFF & 0xFF & 0xFF & 0xFF & 0xFF & 0xFF & 0xFF);
        sendBuffer(commandBuffer);
    }

    /**
     * sends the E_STALL command,
     * which causes a stall on the pioneer only
     *
     * @param arg argument
     */
    public void sendE_stall(short arg) throws RemoteException {

        // MS: fixed
        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, (byte) 6, (byte) 56, 0x3b, 0, 0, 0, 0};

        if (arg < 0) {
            commandBuffer[4] = 0x1b;
            arg = (short) ((-1) * arg);
        }
        int check;
        commandBuffer[6] = (byte) (arg >>> 8);
        commandBuffer[5] = (byte) (arg & 0xFF);
        check = calcCheckSum(commandBuffer);
        commandBuffer[7] = (byte) (check >>> 8);
        commandBuffer[8] = (byte) (check & 0xFF);
        sendBuffer(commandBuffer);
    }

    /**
     * sends the ENABLE command,
     * which enables (1) or disables (0) the motors
     *
     * @param arg enable (1) or disable (0)
     */
    public void sendEnable(short arg) throws RemoteException {
        // MS: fixed
        byte[] commandBuffer = {(byte) 0xfa, (byte) 0xfb, (byte) 6, (byte) 4, 0x3b, 0, 0, 0, 0};

        if (arg < 0) {
            commandBuffer[4] = 0x1b;
            arg = (short) ((-1) * arg);
        }
        int check;
        short ch1;
        commandBuffer[6] = (byte) (arg >>> 8);
        commandBuffer[5] = (byte) (arg & 0xFF);
        check = calcCheckSum(commandBuffer);
        ch1 = (short) check;
        commandBuffer[7] = (byte) (ch1 >>> 8);
        commandBuffer[8] = (byte) (ch1 & 0xFF);
        sendBuffer(commandBuffer);
    }

    /**
     * pulls up to 200 bytes from the serial port into a local buffer
     * for parsing by another function.
     *
     * @return an array of <code>byte</code>s that were read in
     */
    public synchronized byte[] receive() throws DataFormatException {
        // This may not need to be synchronized
        byte[] readBuffer = new byte[3]; // this will get resized later
        //byte[] junkBuffer = new byte[200];
        boolean coproc = false;
        int bytesread = 0;
        int numBytes = 0;
        int i;
        long start, now;

        if (portOpenOk) {
            if (debug) {
                System.out.println("In receive()...");
            }
            start = now = System.currentTimeMillis();
            //      try {
            //	numberBytes =(Integer.valueOf( messageString.substring(0,2), 16)).intValue();
            //	outputStream.write((messageString.substring(2)).getBytes());
            //      } catch (IOException e) { System.err.println("Exception " + e + " occurred"); }
            //      bytesread = 0;
            //      numBytes = 0;
            try {
                // Receive and check the first byte
                // (aka: dispose of garbage until you find 0xFA)
                while (readBuffer[0] != (byte) (0xFA) && (now - start) < DEF_READ_TIMEOUT) {
                    numBytes = 0;
                    //				  while ( inputStream.available() == 0);
                    numBytes += inputStream.read(readBuffer, numBytes, 1);
                    //	    if (readBuffer[0] != (byte) 0xFA)
                    //		System.out.println("ERROR 1 " + readBuffer[0] + " " + numBytes);
                    now = System.currentTimeMillis();
                }
                if (readBuffer[0] != (byte) (0xFA)) {
                    System.out.print("Invalid first byte; need 0xFA, ");
                    System.out.println("got 0x" + Integer.toHexString(readBuffer[0]));
                    return null;
                }
                // Receive and check the second byte
                //			  while ( inputStream.available() == 0);
                numBytes += inputStream.read(readBuffer, numBytes, 1);
                if (readBuffer[1] != (byte) 0xFB) {
                    System.out.print("Second byte: expected 0xfb, got ");
                    System.out.println(Integer.toHexString(readBuffer[1]));
                    throw new DataFormatException("Incoming packet has bad startbyte: 0x" + Integer.toHexString(readBuffer[1] & 0xFF));
                }

                // Receive the number of bytes remaining in the communication

                //			  while ( inputStream.available() == 0);
                numBytes += inputStream.read(readBuffer, numBytes, 1);
                numberBytes = readBuffer[2] & 0xFF; // stupid signed stuff
                if (debug) {
                    System.out.println("Expecting " + (numberBytes + 3) + " bytes (" + Integer.toHexString(readBuffer[2]) + ")...");
                }
                if (numberBytes > 200) {
                    System.out.println("Cannot receive > 200 bytes");
                    throw new DataFormatException("Illogical number of bytes: " + numberBytes);
                }
                readBuffer = new byte[numberBytes + 3];
                readBuffer[0] = (byte) 0xFA;
                readBuffer[1] = (byte) 0xFB;
                readBuffer[2] = (byte) numberBytes;
                //while ((numBytes-3) < numberBytes) {
                while (numBytes < (numberBytes + 3)) {
                    //				  while ( inputStream.available() == 0);
                    numBytes += inputStream.read(readBuffer, numBytes, 1);
                }
            } catch (IOException e) {
                System.err.println("receive: IO exception -- method error");
                readBuffer[0] = 0;
            }
            Checksum = toShortLocal(readBuffer[numBytes - 2], readBuffer[numBytes - 1]);
            int compChecksum = calcCheckSum(readBuffer);
            //System.out.println("checksum " + (short) compChecksum + " " + Checksum);
            if (Checksum != (short) compChecksum) {
                System.out.print("Bad checksum; got: ");
                System.out.print("0x" + Integer.toHexString(Checksum & 0xFFFF));
                System.out.println(" expected: 0x" + Integer.toHexString(compChecksum & 0xFFFF) + ")");
                throw new DataFormatException("Bad Checksum (got: 0x" + Integer.toHexString(Checksum & 0xFFFF) + " expected: 0x" + Integer.toHexString(compChecksum & 0xFFFF) + ")");
            }
            //      returnMessage = (new String(readBuffer));
        }
        if (debug) {
            System.out.print("received " + numBytes + " [");
            for (int j = 0; j < numBytes; j++) {
                System.out.print(Integer.toHexString(readBuffer[j] & 0xFF) + " ");
            }
            System.out.println("]");
        }
        return readBuffer;
    }

    // add all the graphical components
    @SuppressWarnings("unchecked")
    public static ArrayList extendInit(ADEComponent boi) {
        ArrayList components = new ArrayList();
        JButton controlPanel = new JButton("Control");
        JButton sonarButton = new JButton("Sonar");
        //JButton laserButton = new JButton("Laser");
        JButton bumperButton = new JButton("Bumper");
        JButton motorButton = new JButton("Motors");
        components.add(sonarButton);
        //components.add(laserButton);
        components.add(bumperButton);
        components.add(motorButton);
        components.add(controlPanel);
        // create action listener for panels
        myActionListener mal = new myActionListener(boi, controlPanel, sonarButton, bumperButton, motorButton);
        controlPanel.addActionListener(mal);
        //laserButton.addActionListener(mal);
        sonarButton.addActionListener(mal);
        bumperButton.addActionListener(mal);
        motorButton.addActionListener(mal);
        return components;
    }

    // ===================== GRAPHICAL INTERFACE ============================================================
    static class myActionListener implements ActionListener {

        ADEComponent boi;
        JButton laserButton, controlPanel, sonarButton, bumperButton, motorButton;
        PioneerComponent robot;
        public PioneerSonarPanel sonarPanel;
        public PioneerRangePanel laserPanel;
        public PioneerBumperPanel bumperPanel;
        public PioneerMotorPanel motorPanel;

        public myActionListener(ADEComponent boi, JButton controlPanel, JButton sonarButton, JButton bumperButton, JButton motorButton) {
            this.boi = boi;
            this.controlPanel = controlPanel;
            this.sonarButton = sonarButton;
            this.bumperButton = bumperButton;
            this.motorButton = motorButton;
            /* PWS: Not sure what this is for, but it no longer works...
            try {
            robot = (PioneerComponent)boi.getValue("robot");
            } catch (Exception e) {
            System.out.println("ERROR GETTING ROBOT: " + e);
            }
             */
            if (robot == null) {
                System.out.println("Null robot");
            }
        }

        public void update() {
        }

        public void actionPerformed(ActionEvent e) {
            Object o = e.getSource();
            if (o == controlPanel) {
                displayPanel();
            } else if (o == sonarButton) {
                if (sonarPanel == null) {
                    sonarPanel = new PioneerSonarPanel(robot);
                } else {
                    sonarPanel.setVisible(true);
                }
            } else if (o == bumperButton) {
                if (bumperPanel == null) {
                    bumperPanel = new PioneerBumperPanel(robot);
                } else {
                    bumperPanel.setVisible(true);
                }
            } else if (o == motorButton) {
                if (motorPanel == null) {
                    motorPanel = new PioneerMotorPanel(robot);
                } else {
                    motorPanel.setVisible(true);
                }
            }
        }

        void displayPanel() {
            final Dialog f = new Dialog(new Frame(), "Control Panel", false);
            Button left, right, forward, back, stop;
            BorderLayout blah = new BorderLayout();
            f.setLayout(blah);
            left = new Button("Left");
            left.addActionListener(
                    new ActionListener() {

                        public void actionPerformed(ActionEvent e) {
                            try {
                                robot.turn(15.0);
                            } catch (Exception e1) {
                                System.err.println("Exception in turn 15");
                            }
                        }
                    });

            forward = new Button("Forward");
            forward.addActionListener(
                    new ActionListener() {

                        public void actionPerformed(ActionEvent e) {
                            System.out.println(robot);
                            if (robot == null) {
                                System.out.println("NULL ROBOT");
                            } else {
                                try {
                                    robot.setVelocity(30);
                                } catch (Exception e1) {
                                    System.err.println("Exception in set velocity 30");
                                }
                            }
                        }
                    });

            right = new Button("Right");
            right.addActionListener(
                    new ActionListener() {

                        public void actionPerformed(ActionEvent e) {
                            try {
                                robot.turn(-15.0);
                            } catch (Exception e1) {
                                System.err.println("Exception in turn -15");
                            }
                        }
                    });

            back = new Button("Back");
            back.addActionListener(
                    new ActionListener() {

                        public void actionPerformed(ActionEvent e) {
                            try {
                                robot.setVelocity(-30);
                            } catch (Exception e1) {
                                System.err.println("Exception in set velocity -30");
                            }
                        }
                    });

            stop = new Button("Stop");
            stop.addActionListener(
                    new ActionListener() {

                        public void actionPerformed(ActionEvent e) {
                            try {
                                robot.stop();
                            } catch (Exception e1) {
                                System.err.println("Exception in stop");
                            }

                        }
                    });
            f.add(left, "West");
            f.add(forward, "North");
            f.add(right, "East");
            f.add(back, "South");
            f.add(stop, "Center");
            f.addWindowListener(
                    new WindowAdapter() {

                        public void windowClosing(WindowEvent e) {
                            f.setVisible(false);
                            f.dispose();
                        }
                    });
            f.pack();
            f.setVisible(true);
        }
    }
    // TODO: FIX THIS!!!

    public void componentUpdate() {
        if (robot == null) {
            System.out.println("NULL ROBOT");
        } else {
            try {
                robot.getSonars();
            } catch (Exception e) {
                System.out.println("ERROR GETTING DISTANCES");
            }
        }
    }

    // not sure whether we need this
    public void extendActionPerformed(Component c, ADEComponent boi) {
        System.out.println("Component " + c + " was activated");
    }

    /** The server is always ready to provide its service after it has come up */
    protected boolean localServicesReady() {
        return true;
    }

    /** Constructor. Goes throught the requisite setup for operating
     * an ActivMedia robot.
     * @throws RemoteException if the server cannot be instantiated
     * @see #additionalUsageInfo for command line arguments */
    public PioneerComponentImpl() throws RemoteException {
        super();
        if (verbose) {
            System.out.println("PioneerComponent finished super() call, continuing...");
        }
        robot = this;

        int numBytes = 0, i = 0;
        byte[] junkBuffer = new byte[100];
        if (!usePeoplebot) {
            if (verbose) {
                System.out.println("Not using Peoplebot, resetting NUM_SONAR to 16");
            }
            NUM_SONARS = 16;
        }
        shortSonars = new short[NUM_SONARS];
        sonars = new double[NUM_SONARS];
        sonarNew = new boolean[NUM_SONARS];

        // Initialization commands
        if (verbose) {
            System.out.println("PioneerComponent starting connection process...");
        }
        boolean notyetconnected = true;
        while (notyetconnected) {
            if (verbose2) {
                System.out.println(prg + ": Calling startUp()");
            }
            startUp();
            // Start the controller
            if (verbose2) {
                System.out.println(prg + ": Calling sendOpen()");
            }
            sendOpen();
            // Reset the watchdog
            if (verbose2) {
                System.out.println(prg + ": Calling sendPulse()");
            }
            sendPulse();
            // Enable the motors
            if (verbose2) {
                System.out.println(prg + ": Calling sendEnable()");
            }
            sendEnable((short) 1);
            // Enable encoder packets
            if (verbose2) {
                System.out.println(prg + ": Calling sendEncoder()");
            }
            sendEncoder((short) 2);
            // Request a configuration packet and fill data fields
            if (verbose2) {
                System.out.println(prg + ": Calling sendConfig()");
            }
            notyetconnected = !sendConfig((short) 1);
        }
        // Clear the serial buffer
        if (verbose2) {
            System.out.println(prg + ": Calling sendGetaux()");
        }
        sendGetaux((short) 0);
        if (verbose2) {
            System.out.println(prg + ": Calling sendEstop()");
        }
        sendEstop();
        // PWS: changing these to 200
        if (verbose2) {
            System.out.println(prg + ": Setting max trans accel to 30");
        }
        sendSeta((short) 200);
        if (verbose2) {
            System.out.println(prg + ": Setting max trans decel to -30");
        }
        sendSeta((short) -200);
        if (verbose2) {
            System.out.println(prg + ": Setting max rot accel to 30");
        }
        sendSetra((short) 30);
        if (verbose2) {
            System.out.println(prg + ": Setting max rot decel to -30");
        }
        sendSetra((short) -30);
        if (movingSonarOnly || leaveSonarOff) {
            if (verbose2) {
                System.out.println(prg + ": Turning sonar off");
            }
            sendSonar((short) 0);
            sonarOn = false;
        }
        // note we set our state to RUN in the run() method
        if (verbose) {
            System.out.println(prg + ": Starting thread");
        }
        t = new Thread(this, prg);
        t.start();
    }

    // provide additional information for usage...
    protected String additionalUsageInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("     -pb   --usepeoplebot <set use peoplebot to " + !usePeoplebot + ">\n");
        sb.append("     -ramp --usespeedramp <set use speed ramping to " + !useSpeedRamp + ">\n");
		sb.append("     -batt --batterycheck <print battery voltage and exit>\n");
        sb.append("     -t -T --turnsonar <set turn sonar off permanently to " + !leaveSonarOff + ">\n");
        sb.append("     -s -S --sonarburst <turns on sonar burst mode>\n");
        sb.append("     -log --uselogger [logname] <set useLogger to " + !useLogger + ">\n");
        return sb.toString();
    }

    protected boolean parseadditionalargs(String[] args) {
        // MS: moved this from main, please check
        // should be integrated below
        int port;
        serialPortName = "/dev/ttyUSB1";
        if ((port = preparseargs(args)) > 0) {
            serialPortName = args[port];
        }
        System.out.println("Setting port to " + serialPortName);
        System.setProperty("gnu.io.rxtx.SerialPorts", serialPortName);

        boolean found = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-s") || args[i].equals("--sonarburst")) {
                SONAR_BURST_FLAG = true;
                found = true;
            } else if (args[i].equalsIgnoreCase("-t") || args[i].equals("--turnsonar")) {
                leaveSonarOff = !leaveSonarOff;
                found = true;
            } else if (args[i].equalsIgnoreCase("-pb") || args[i].equals("--usepeoplebot")) {
                usePeoplebot = !usePeoplebot;
                found = true;
            } else if (args[i].equalsIgnoreCase("-ramp") || args[i].equals("--usespeedramp")) {
                useSpeedRamp = !useSpeedRamp;
                System.out.println("SpeedRamp now == " + useSpeedRamp);
                found = true;
            } else if  (args[i].equalsIgnoreCase("-batt") || args[i].equals("--batteryCheck")) {
                batteryCheck = true;
                found = true;
            } else if (args[i].equalsIgnoreCase("-log") || args[i].equals("--uselogger")) {
                useLogger = !useLogger;
                try {
                    // try to get a filename; if arg starts with "-", assume
                    // it's a switch and no filename was given
                    String tmp = args[++i];
                    if (!tmp.startsWith("-")) {
                        logFileName = tmp;
                    } else {
                        i--;
                    }
                } catch (Exception e) {
                    System.err.println("No log filename supplied; using " + logFileName);
                }
                found = true;
            } else if (args[i].equalsIgnoreCase("-port")) {
                // already took care of this in preparseargs
                ++i;
                found = true;
            } else {
                return false;  // return on any unrecognized args
            }
        }
        return found;
    }

    static protected int preparseargs(String[] args) {
        int port = -1;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-port")) {
                port = ++i;
                break;
            }
        }
        return port;
    }

    // TODO: the main loop needs to use these...
    // nothing special for now
    protected void updateFromLog(String logEntry) {
    }

    /*
    final public static void main(String[] args) throws Exception {
        // if non-standard port is desired, set "serialPortName = ... " here
        int port;
        serialPortName = "/dev/ttyUSB1";
        if ((port = preparseargs(args)) > 0) {
            serialPortName = args[port];
        }
        System.out.println("Setting port to " + serialPortName);
        System.setProperty("gnu.io.rxtx.SerialPorts", serialPortName);
        ADEComponentImpl.main(args);
    }
     * 
     */
}
