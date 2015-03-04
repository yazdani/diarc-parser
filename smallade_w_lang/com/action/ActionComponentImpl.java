/**
 * ADE 1.0 Beta 
 * (c) copyright HRILab (http://hri.cogs.indiana.edu/) 
 * 
 * All rights reserved.  Do not copy and use without permission. 
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * ActionComponentImpl.java
 *
 * Last update: April 2010
 *
 * @author Paul Schermerhorn
 *
 */

package com.action;

import static utilities.Util.Sleep;

import java.rmi.RemoteException;

import ade.ADEException;

/** <code>ActionComponent</code> periodically calls the method in the derived
 * ActionComponentArch class.  This is just a greatly simplified version of the
 * GoalManager, providing the user with the infrastructure (e.g., remote
 * reference setup) of the GoalManager but without starting any 
 * ActionInterpreter.  Instead, the user specifies the actions in the
 * runArchitecture() method.  Some actions are defined here (i.e., methods
 * wrapping remote calls to VelocityComponent and LRFComponent); see the default
 * architecture in DefaultActionComponentArch.java for examples of other remote
 * calls.
 */
abstract public class ActionComponentImpl extends GoalManagerImpl {
	private static final long serialVersionUID = 1L;
	
	/* ADE-related fields (pseudo-refs, etc.) */
    private static String prg = "ActionComponentServ";
    private static String type = "ActionComponentServ";
    private static boolean verbose = true;
    private static boolean debug = true;

    private Updater upd;

    public static double defTV = 0.5; // m/s
    public static double defRV = Math.PI / 8.00; // rad/s

    // Architectures need to implement this; it gets called every cycle
    abstract public void runArchitecture();

    // ********************************************************************
    // *** ActionComponent convenience methods (wrappers for use by archs)
    // ********************************************************************

    /**
     * Go forward.
     * @return true if successful, false otherwise
     */
    final public boolean goStraight() {
        Boolean r = false;
        if (!checkMethod("setVels")) {
            System.out.println(methodName() + ": no VelocityComponent!");
            return r;
        }
        try {
            r = (Boolean)callMethod("setVels", defTV, 0.0);
        } catch (ADEException ace) {
            System.err.println("goStraight: Error sending to robot: "+ ace);
        } 
        return r;
    }

    /**
     * Go backwards. This is a very bad idea on a robot without
     * rearward-facing sensors.
     * @return true if successful, false otherwise
     */
      final public boolean goBackwards() {
        Boolean r = false;
        if (!checkMethod("setVels")) {
            System.out.println(methodName() + ": no VelocityComponent!");
            return r;
        }
        try {
            r = (Boolean)callMethod("setVels", -defTV, 0.0);
        } catch (ADEException ace) {
            System.err.println("goBackwards: Error sending to robot: "+ ace);
        }
        return r;
    }

    /**
     * Turn left at current TV and default RV.
     * @return true if successful, false otherwise
     */
    final public boolean goLeft() {
        Boolean r = false;
        if (!checkMethod("setVels")) {
            System.out.println(methodName() + ": no VelocityComponent!");
            return r;
        }
        try {
            r = (Boolean)callMethod("setVels", defTV, defRV);
        } catch (ADEException ace) {
            System.err.println("goLeft: Error sending to robot: "+ ace);
        } 
        return r;
    }

    /**
     * Turn right at current TV and default RV. 
     * @return true if successful, false otherwise
     */
    final public boolean goRight() {
        Boolean r = false;
        if (!checkMethod("setVels")) {
            System.out.println(methodName() + ": no VelocityComponent!");
            return r;
        }
        try {
            r = (Boolean)callMethod("setVels", defTV, -defRV);
        } catch (ADEException ace) {
            System.err.println("goLeft: Error sending to robot: "+ ace);
        } 
        return r;
    }

    /**
     * Turn left at zero TV and default RV. 
     * @return true if successful, false otherwise
     */
    final public boolean turnLeft() {
        Boolean r = false;
        if (!checkMethod("setVels")) {
            System.out.println(methodName() + ": no VelocityComponent!");
            return r;
        }
        try {
            r = (Boolean)callMethod("setVels", 0.0, defRV);
        } catch (ADEException ace) {
            System.err.println("turnLeft: Error sending to robot: "+ ace);
        } 
        return r;
    }

    /**
     * Turn right at zero TV and default RV. 
     * @return true if successful, false otherwise
     */
    final public boolean turnRight() {
        Boolean r = false;
        if (!checkMethod("setVels")) {
            System.out.println(methodName() + ": no VelocityComponent!");
            return r;
        }
        try {
            r = (Boolean)callMethod("setVels", 0.0, -defRV);
        } catch (ADEException ace) {
            System.err.println("turnRight: Error sending to robot: "+ ace);
        } 
        return r;
    }

    /**
     * Stop.
     * @return true if successful, false otherwise
     */
    final public boolean stop() {
        Boolean r = false;
        if (!checkMethod("setVels")) {
            System.out.println(methodName() + ": no VelocityComponent!");
            return r;
        }
        try {
            r = (Boolean)callMethod("setVels", 0.0, 0.0);
        } catch (ADEException ace) {
            System.err.println("stop: Error sending to robot: "+ ace);
        } 
        return r;
    }

    /**
     * Set robot velocities.
     * @param tv translational (forward) velocity
     * @param rv rotational velocity 
     * @return true if successful, false otherwise
     */
    final public boolean setVels(double tv, double rv) {
        Boolean r = false;
        if (!checkMethod("setVels")) {
            System.out.println(methodName() + ": no VelocityComponent!");
            return r;
        }
        try {
            r = (Boolean)callMethod("setVels", tv, rv);
        } catch (ADEException ace) {
            System.err.println("setVels: Error sending to robot: "+ ace);
        } 
        return r;
    }

    /**
     * Check for obstacles. 
     * @return an array indicating whether there is an obstacle in eac hof
     * three zones: left, front, right
     */
    final public boolean[] checkObstacles() {
        boolean[] safes;
        boolean[] obsts = new boolean[3];
        if (!checkMethod("getSafeSpaces")) {
            System.out.println(methodName() + ": no LRFComponent!");
            return obsts;
        }

        try {
            safes = (boolean[])callMethod("getSafeSpaces");
            obsts[0] = ! safes[0];
            obsts[1] = ! safes[1];
            obsts[2] = ! safes[2];
        } catch (ADEException ace) {
            System.out.println("checkObstacles: error checking lasers");
            obsts[0] = false;
            obsts[1] = false;
            obsts[2] = false;
        }
        return obsts;
    }

    /**
     * Get current readings from the laser rangefinder. 
     * @return an array of distances starting with the leftmost
     */
    final public double[] getLaserReadings() {
        if (!checkMethod("getLaserReadings")) {
            System.out.println(methodName() + ": no LRFComponent!");
            return null;
        }
        try {
            return (double[])callMethod("getLaserReadings");
        } catch (ADEException ace) {
            System.out.println("getLaserReadings: error checking lasers");
        }
        return null;
    }

    /**
     * Get current readings from the bumpers. 
     * @return an array of bumper states starting with the leftmost
     */
    final public boolean[] getBumperReadings() {
        if (!checkMethod("getBumperReadings")) {
            System.out.println(methodName() + ": no LRFComponent!");
            return null;
        }
        try {
            return (boolean[])callMethod("getBumperReadings");
        } catch (ADEException ace) {
            System.out.println("getBumperReadings: error checking bumpers");
        }
        return null;
    }

    /**
     * Get current readings from the motors. 
     * @return true if successful, false otherwise
     */
    final public boolean getStall() {
        if (!checkMethod("getStall")) {
            System.out.println(methodName() + ": no LRFComponent!");
            return false;
        }
        try {
            return (Boolean)callMethod("getStall");
        } catch (ADEException ace) {
            System.out.println("getStall: error checking stall");
        }
        return false;
    }

    /**
     * Get the current orientation. Unsafe as presently implemented. Only
     * works with some simulators.
     * @return the orinentation n radians
     */
    final public double getOrientation()    {

        double X = -1.0;

        try {
            X =  (Double)callMethod("getOrientation");
        } catch (ADEException ace) {
            System.out.println("getOrientation: error checking orientation");
        }
        return X;
    }
    
    /**
     * Get the current beacon info. Unsafe as presently implemented. Only
     * works with some simulators (i.e., old (pre-2010) adesim, and ADESim2010). 
     * @return an array of beacon states
     */
    final public Double[][] getBeaconData()    {
    	// try with ADESim2010 first:
    	// in the case of ADESim2010, the first value in each "inner" double array is
    	// the ABSOLUTE angle of the beacon relative to the robot, and the second is the 
    	// distance from the robot to the center of the beacon.
    	try {
    		final String[] careAbout = {"block"};
    		final Object[] methodArgArray = {careAbout};
    		return (Double[][]) callMethod("getObjectsAngleAndDistance", methodArgArray);
    	} catch (ADEException ace) {
    		// couldn't do it?  ok, fine, see if it's a method the old adesim will take
    	}

    	
    	// try old adesim:
    	try {
    		return (Double[][])callMethod("getBeaconData");
    	} catch (ADEException ace) {
    		// ok, not so good.  but just wait till very end to take care of error:
    	}
    	
    	// if hasn't returned with data, must have had a problem:
    	System.out.println("getBeaconData: error checking for beacons");
    	return new Double[][] {};
    }

    /**
     * Turn off beacons. Unsafe as presently implemented. Only works with
     * some simulators (i.e., old (pre-2010) adesim).
     */
    final public void turnOffBeacon()    {
    	try {
             callMethod("turnOffBeacon");
        } catch (ADEException ace) {
            System.out.println("getBeacon: error acquiring beacons");
        }
    }
    
    

    // ********************************************************************
    // *** ActionComponent "internal" methods
    // ********************************************************************

    protected void setSleepTime(int millis) {
        upd.setSleepTime(millis);
    }

    private String methodName() {
        return Thread.currentThread().getStackTrace()[2].getMethodName();
    }

    /**
     * Implements the local shutdown mechanism that derived classes need to
     * implement to cleanly shutdown
     */
    @Override
    protected void localshutdown() {
        System.out.print("\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println(prg + " shutting down...");
        System.out.print("stopping thread...");
        upd.halt();
        System.out.println("thread stopped...");
        System.out.println("closing logs...");
        if (useLocalLogger)
            setASL(false);
        if (checkMethod("stop")) {
            try {
                callMethod("stop");
            } catch (ADEException ace) {
                System.err.println("Unable to send halt to MoPo!");
            }
        }
        System.out.println("done.");
    }

    @Override
    protected boolean localServicesReady() {
        return refsReady;
    }

    /** 
     * Constructs the ActionComponent.
     */
    public ActionComponentImpl() throws RemoteException {
        super();
        upd = new Updater(200);
	System.out.println("ActionConponentImpl");
        sliceTime = 0;
        cycleTime = 50;

        registerNewComponentNotification(new String[][]{{}},true);
        getInitialRefs();
        upd.start();
    }

    /**
     * Update priorities; does nothing in the current server, as there is no
     * goal management.
     */
    @Override
    public void updatePriorities() {
    }

    /**
     * The <code>Updater</code> is the main loop for the action
     * interpreter, performing a small amount of periodic housekeeping and
     * then calling the action interpreter method <code>runCycle</code>.
     */
    private class Updater extends Thread {
        int sleepTime; // Add cycleTime
        boolean shouldUpdate;

        public Updater(int st) {
            sleepTime = st;
            shouldUpdate = true;
        }

        @Override
        public void run() {
            while (! gotRefs)
                Sleep(500);
            System.out.println("Ready to start.");
            while (shouldUpdate) {
            	try {
                    //System.out.println(prg +": top of Updater thread...");
                    try {
                        // PWS: Need to change this so that sleepTime is
                        // cycleTime - elapsedTime
                        Thread.sleep(sleepTime);
                    } catch(Exception e) {
                        System.err.println(prg +": had sleep issues: "+ e);
                    }
                    runArchitecture();
                } catch (Exception e1) { 
                    System.out.println(prg +": got generic exception"); 
                    e1.printStackTrace(); 
                }
            }
            System.out.println(prg +": Exiting Updater thread...");
        }

        public void halt() {
            shouldUpdate = false;
        }

        public void setSleepTime(int millis) {
            sleepTime = millis;
        }
    }

    /**
     * <code>main</code> passes the arguments up to the ADEComponentImpl
     * parent.  The parent does some magic and gets the system going.
    public static void main (String[] args) throws Exception {
        ADEComponentImpl.main(args);
    }
     */
    
}

// vi:ai:smarttab:expandtab:ts=8 sw=4
