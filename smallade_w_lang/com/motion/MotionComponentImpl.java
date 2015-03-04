/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * MotionComponentImpl.java
 *
 * Motion: coordinate (stateful) motions using Velocity and Position servers
 *
 * @author Paul Schermerhorn
 *
 */
package com.motion;

import ade.Connection;
import ade.SuperADEComponentImpl;
import com.ActionStatus;
import com.Predicate;
import com.vision.stm.MemoryObject;

import java.awt.*;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.lang.String.format;
import static utilities.Util.*;

/** Component to monitor and control extended motion commands. */
public class MotionComponentImpl extends SuperADEComponentImpl implements MotionComponent {

    /* Laser and Position information */
    private boolean openFront = true;
    private boolean openLeft = true;
    private boolean openRight = true;
    private boolean safeFront = true;
    private boolean safeLeft = true;
    private boolean safeRight = true;
    private static boolean avoidObstacles = true; // saves the parameter
    private boolean avobst = true; // avoidObstacles modified by navTo
    private boolean imminentCollision = false;
    public static double CRITICALDIST = 0.35;
    private double[] position;
    public final short X = 0;
    public final short Y = 1;
    public final short X1 = 2;
    public final short Y1 = 3;
    public final short X2 = 4;
    public final short Y2 = 5;
    public final short THETA = 2;
    public final short STATUS = 3;
    public final short DIST = 2;
    public final short SIDE = 1;
    public final short STATE = 1;
    public final short DIFF = 0;
    public final short TV = 0;
    public final short RV = 2;
    private static double initx = 0.0;
    private static double inity = 0.0;
    private static double initt = 0.0;


    private double scanAngle = 0.0;
    private int numReadings = 0;
    private int rightSideReading = 0;
    private int leftSideReading = 0;
    private int sideScanReadings = 0;
    private double rightRangeHist = 0.0;
    private int rightRangeReadings = 0;
    private double leftRangeHist = 0.0;
    private int leftRangeReadings = 0;

    /* Motion-related fields */
    private double[] nominal; // Nominal velocity
    public final double MAX_TV = 1.0;
    public final double MAX_RV = 0.35;
    private static double defTV = 1.0;
    private static double defRV = 0.3;
    private static double slowTV = 0.15;
    private static double slowRV = 0.1;
    private static double P = 1.0;
    private static double I = 0.2;
    private static double D = 5.0;
    private Action action;
    private final Action newAction;
    private HashMap<Long, Action> actionList;
    private boolean doneConstructing;

    private static enum ActionType {

        NONE, MOVETHROUGH, MOVETO, MOVEDIST, TIMEMOVE, MOVETOREL, TURNTO, TURNTOPOINT, TURNDIST, TIMETURN, STOP, SETVELS, SETTV, SETRV, FOLLOWWALL, TRAVERSE, APPROACHVISREF, APPROACHVISCOLOR
    }

    private double[] origin; // The starting position for the current action
    private double[] target; // The goal for the current action
    private double[] doorway; // The goal for the current action
    private double[] history; // History vector for moveTo
    private Long targetRef; // target for vision-based movements
    private Predicate targetColor; // target for vision-based movements
    private int moveThroughState = 0;
    public final short APPROACH = 1;
    public final short APPROACHED = 2;
    public final short TURN = 3;
    public final short TURNED = 4;
    public final short THROUGH = 5;
    private static double dEpsilon = 0.4; // acceptable distance error
    private static double tEpsilon = 0.05; // acceptable theta error (from a turn command target)
    private static double hEpsilon = 0.05; // acceptable heading error (from a move command nav heading)
    private double histFactor = 0.0; // weight given to past force vector

    /* Vel & Position interfaces, etc. */
    public static boolean useCart = false;
    public static boolean useSegway = false;
    public static boolean usePioneer = false;
    public static boolean useVidere = false;
    public static boolean useWheelchair = false;
    public static boolean useUSARSim = false;
    public static boolean useGPS = false;
    public static boolean useGPSD = false;
    public static boolean useTrimble = false;
    public static boolean useCarmenLoc = false;
    public static boolean gotCarmenLoc = false;
    public static boolean reinitCarmenLoc = false;
    public static boolean useSick = false;
    public static boolean useUtm = false;
    public static boolean useUrg = false;

    Connection velocityComponent;
    public static boolean useVel = true;
    public static String velType = "com.interfaces.VelocityComponent";
    public static String velName = null;

    Connection positionComponent;
    public static boolean deadReckoning = false;
    public static boolean usePos = true;
    public static String posType = "com.interfaces.PositionComponent";
    public static String posName = null;

    Connection laserComponent;
    public static boolean useLrf = true;
    public static String lrfType = "com.interfaces.LaserComponent";
    public static String lrfName = null;

    Connection visionComponent;
    public static String visionVersion = "com.vision.VisionComponent";
    private static boolean gotVision = false;
    private int visWidth = 320;
    private int visWidth2 = 160;
    private int visHeight = 240;
    private boolean visFinal = true;
    private int visLost = 0;
    private Updater updater;

    private boolean doingMotion = false;
    private long phantomTime = 0;
    private boolean phantomLeft = true;


    @Override
    protected void init() {
        // Set all connections to null
        velocityComponent = null;
        positionComponent = null;
        laserComponent = null;
        visionComponent = null;


        position = new double[]{initx, inity, initt};
        nominal = new double[3];
        origin = new double[3];
        target = new double[3];
        doorway = new double[6];
        history = new double[3];
        action = new Action();
        actionList = new HashMap<Long, Action>();


        doneConstructing = false;
    }

    /** Constructs the MotionComponentImpl. */
    public MotionComponentImpl() throws RemoteException {
        super();
        newAction = new Action();


        // Get the Vel and Position servers
        if (useVel) {
            log.info(format("Connecting to velocity component %s", velType));
            if (velName == null) {
                velocityComponent = connectToComponent(velType);
            } else {
                log.warn(format("Using old style connection. Using %s, droping %s", velType, velName));
                velocityComponent = connectToComponent(velType);
            }
        }

        if (usePos) {
            log.info(format("Connecting to position component %s", posType));
            if (posName == null) {
                positionComponent = connectToComponent(posType);
            } else {
                log.warn(format("Using old style connection. Using %s, dropping %s", velType, velName));
                positionComponent = connectToComponent(posType);
            }
        }

        if (useLrf) {
            log.info(format("Connecting to laser component %s", lrfType));
            if (lrfName == null) {
                laserComponent = connectToComponent(lrfType);
            } else {
                log.warn(format("Using old style connection. Using %s, dropping %s", velType, velName));
                laserComponent = connectToComponent(lrfType);
            }
        }

        // TODO: are these used or can we simplify?
        if (useCart) {
            velocityComponent = connectToComponent("com.cart.CartComponent");
            positionComponent = connectToComponent("com.cart.CartComponent");
            defTV = 40;
            defRV = 0.15;
            slowTV = 40;
            slowRV = 0.1;
            // lrf = ...
        } else if (useSegway) {
            velocityComponent = connectToComponent("com.segway.SegwayComponent");
            P = 1.0;
            I = 0.0;
            D = 0.0;
        } else if (usePioneer) {
            velocityComponent = connectToComponent("com.pioneer.PioneerComponent");
        } else if (useVidere) {
            velocityComponent = connectToComponent("com.videre.VidereComponent");
        } else if (useWheelchair) {
            velocityComponent = connectToComponent("com.wheelchair.WheelchairComponent");
        } else if (useUSARSim) {
            velocityComponent = connectToComponent("com.usarsim.USARSimComponent");
            positionComponent = connectToComponent("com.usarsim.USARSimComponent");
            positionComponent = connectToComponent("com.usarsim.USARSimComponent");
        }
        if (useGPSD) {
            positionComponent = connectToComponent("com.gps.GPSDComponent");
        } else if (useTrimble) {
            positionComponent = connectToComponent("com.gps.TrimbleGPSComponent");
        } else if (useCarmenLoc) {
            positionComponent = connectToComponent("com.carmen.Localize");
        }
        if (useUrg) {
            positionComponent = connectToComponent("com.lrf.UrgLRFComponent");
        } else if (useSick) {
            positionComponent = connectToComponent("com.lrf.SickLRFComponent");
        } else if (useUtm) {
            positionComponent = connectToComponent("com.lrf.UtmLRFComponent");
        }

        Sleep(2000);
        while (!laserComponent.isReady()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log.error("Sleep interrupted", e);
            }
        }

        log.info("Motion setting LRF crit dist to: " + CRITICALDIST);
        laserComponent.call("setCritDist", void.class, CRITICALDIST);

        updater = new Updater(200, this);
        updater.start();
        doneConstructing = true;
        log.info("Done constructing");
    }

    /**
     * Implements the local shutdown mechanism that derived classes need to
     * implement to cleanly shutdown
     */
    @Override
    protected void localshutdown() {
        super.localshutdown();
        setSpeed(0.0, 0.0);
        updater.halt();
    }

    /** Provide additional information for usage... */
    @Override
    protected String additionalUsageInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Component-specific options:\n\n");
        sb.append("  -cart              <get reference to cart server>\n");
        sb.append("  -wheelchair        <get reference to Wheelchair server>\n");
        sb.append("  -videre            <get reference to Videre server>\n");
        sb.append("  -pioneer           <get reference to Pioneer server>\n");
        sb.append("  -segway            <get reference to segway server>\n");
        sb.append("  -usarsim           <get reference to usarsim server>\n");
        sb.append("  -gpsd              <get reference to gpsd server>\n");
        sb.append("  -trimble           <get reference to trimble gps server>\n");
        sb.append("  -urg               <get reference to URG lrf server>\n");
        sb.append("  -utm               <get reference to UTM lrf server>\n");
        sb.append("  -sick              <get reference to SICK lrf server>\n");
        sb.append("  -noobst            <disable obstacle avoidance>\n");
        sb.append("  -tol dist          <acceptable distance tolerance>\n");
        sb.append("  -initpose x y t    <initial pose>\n");
        sb.append("  -deftv tv		<default translational velocity>\n");
        sb.append("  -defrv rv		<default rotational velocity>\n");
        sb.append("  -critical dist	<set CRITICALDIST to dist>\n");
        return sb.toString();
    }

    /**
     * Parse additional command-line arguments
     *
     * @return "true" if parse is successful, "false" otherwise
     */
    @Override
    protected boolean parseArgs(String[] args) {
        // TODO: remove these hard coded values and make them config supplied
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-cart")) {
                useCart = true;
                useGPS = true;
                dEpsilon = 6.0;
                hEpsilon = 0.15;
            } else if (args[i].equalsIgnoreCase("-segway")) {
                useSegway = true;
                defTV = 0.5;
                defRV = 10.0;
                slowTV = 0.4;
                slowRV = 7.0;
            } else if (args[i].equalsIgnoreCase("-wheelchair")) {
                useWheelchair = true;
                defTV = 0.1;
                defRV = 0.2;
            } else if (args[i].equalsIgnoreCase("-pioneer")) {
                defTV = 0.15;
                usePioneer = true;
            } else if (args[i].equalsIgnoreCase("-videre")) {
                defTV = 0.25;
                slowTV = 0.20;
                defRV = 0.3;
                slowRV = 0.2;
                useVidere = true;
            } else if (args[i].equalsIgnoreCase("-usarsim")) {
                useUSARSim = true;
                defTV = 0.15;
                defRV = 0.1;
            } else if (args[i].equalsIgnoreCase("-gpsd")) {
                useGPS = true;
                useGPSD = true;
            } else if (args[i].equalsIgnoreCase("-trimble")) {
                useGPS = true;
                useTrimble = true;
            } else if (args[i].equalsIgnoreCase("-carmenloc")) {
                useCarmenLoc = true;
            } else if (args[i].equalsIgnoreCase("-utm")) {
                useUtm = true;
            } else if (args[i].equalsIgnoreCase("-sick")) {
                useSick = true;
            } else if (args[i].equalsIgnoreCase("-vel")) {
                useVel = true;
                if (!checkNextArg(args, i)) {
                    velType = args[++i];
                    if (!checkNextArg(args, i)) {
                        velName = args[++i];
                    }
                }
                System.out.println("Getting vel: " + velType + " " + velName);
            } else if (args[i].equalsIgnoreCase("-pos")) {
                usePos = true;
                if (!checkNextArg(args, i)) {
                    posType = args[++i];
                    if (!checkNextArg(args, i)) {
                        posName = args[++i];
                    }
                }
                System.out.println("Getting pos: " + posType + " " + posName);
            } else if (args[i].equalsIgnoreCase("-nolrf")) {
                useLrf = false;
            } else if (args[i].equalsIgnoreCase("-nopos")) {
                usePos = false;
            } else if (args[i].equalsIgnoreCase("-lrf")) {
                useLrf = true;
                if (!checkNextArg(args, i)) {
                    lrfType = args[++i];
                    if (!checkNextArg(args, i)) {
                        lrfName = args[++i];
                    }
                }
                log.info(format("Getting lrf: %s %s", lrfType, lrfName));
            } else if (args[i].equalsIgnoreCase("-noobst")) {
                avoidObstacles = false;
            } else if (args[i].equalsIgnoreCase("-tol")) {
                double tol = Double.parseDouble(args[i + 1]);
                i++;
                dEpsilon = tol;
            } else if (args[i].equalsIgnoreCase("-teps")) {
                double teps = Double.parseDouble(args[i + 1]);
                i++;
                tEpsilon = teps;
                log.info(format("Setting tEpsilon to: %f", teps));
            } else if (args[i].equalsIgnoreCase("-heps")) {
                double heps;
                heps = Double.parseDouble(args[i + 1]);
                i++;
                hEpsilon = heps;
                log.trace("Setting hEpsilon to: " + heps);

            } else if (args[i].equalsIgnoreCase("-initpose")) {
                double[] init = new double[3];
                init[X] = Double.parseDouble(args[i + 1]);
                i++;
                init[Y] = Double.parseDouble(args[i + 1]);
                i++;
                init[THETA] = Double.parseDouble(args[i + 1]);
                i++;
                initx = init[X];
                inity = init[Y];
                initt = init[THETA];

            } else if (args[i].equalsIgnoreCase("-deftv")) {
                double tv;
                tv = Double.parseDouble(args[i + 1]);
                i++;
                defTV = tv;
            } else if (args[i].equalsIgnoreCase("-defrv")) {
                double rv;
                rv = Double.parseDouble(args[i + 1]);
                i++;
                defRV = rv;

            } else if (args[i].equalsIgnoreCase("-slowtv")) {
                double tv;
                tv = Double.parseDouble(args[i + 1]);
                i++;
                slowTV = tv;

            } else if (args[i].equalsIgnoreCase("-slowrv")) {
                double rv;
                rv = Double.parseDouble(args[i + 1]);
                i++;
                slowRV = rv;

            } else if (args[i].equalsIgnoreCase("-critical")) {
                double crit;
                crit = Double.parseDouble(args[i + 1]);
                i++;
                CRITICALDIST = crit;
                log.trace("new CRITICALDIST " + crit);
            } else if (args[i].equalsIgnoreCase("-deadreck")) {
                deadReckoning = true;
                tEpsilon = 0.12;
                usePos = false;
            } else {
                log.trace("Unrecognized argument: " + args[i]);
                return false;  // return on any unrecognized args
            }
        }
        return true;
    }

    // *********************************************************************
    // The methods available to remote objects via RMI
    // *********************************************************************

    /**
     * Move through a doorway
     *
     * @param xdest the x-coordinate of the doorway
     * @param ydest the y-coordinate of the doorway
     * @return an identifying timestamp for the move action
     */
    @Override
    public long moveThrough(double xdest, double ydest, double xdest1, double ydest1) throws RemoteException {
        log.debug("moveThrough: " + xdest + " " + ydest);
        return newAction.initiateAction(ActionType.MOVETHROUGH, xdest, ydest, xdest1, ydest1);
    }
    /* **** NavigationComponent interface **** */

    /**
     * Navigate to a global location.  Will attempt to navigate around
     * obstacles.
     *
     * @param xdest the x-coordinate of the destination
     * @param ydest the y-coordinate of the destination
     * @return an identifying timestamp for the move action
     */
    @Override
    public long moveTo(double xdest, double ydest) throws RemoteException {
        log.debug("moveTo: " + xdest + " " + ydest);
        return newAction.initiateAction(ActionType.MOVETO, xdest, ydest);
    }

    /**
     * Move forward a specified distance.
     *
     * @param dist the distance (in meters) to move
     * @return an identifying timestamp for the move action
     */
    @Override
    public long moveDist(double dist) throws RemoteException {
        log.debug("moveDist: " + dist);
        return newAction.initiateAction(ActionType.MOVEDIST, dist);
    }

    /**
     * Move a specified distance using time instead of global pose.
     *
     * @param dist the distance (in meters) to move
     * @return an identifying timestamp for the move action
     */
    @Override
    public long timeMove(double dist) throws RemoteException {
        log.debug("timeMove: " + dist);
        return newAction.initiateAction(ActionType.TIMEMOVE, dist);
    }

    /**
     * Navigate to a relative location.  Will attempt to navigate around
     * obstacles.
     *
     * @param xdest the x-coordinate of the destination
     * @param ydest the y-coordinate of the destination
     * @return an identifying timestamp for the move action
     */
    @Override
    public long moveToRel(double xdest, double ydest) throws RemoteException {
        log.debug("moveToRel: " + xdest + " " + ydest);
        return newAction.initiateAction(ActionType.MOVETOREL, xdest, ydest);
    }

    /**
     * Update destination of moveToRel navigation.
     *
     * @param aid   the identifying timestamp of the action to update
     * @param xdest the x-coordinate of the destination
     * @param ydest the y-coordinate of the destination
     * @return true if the update was successful, false otherwise
     */
    @Override
    public boolean updateMoveToRel(long aid, double xdest, double ydest) throws RemoteException {
        log.debug("updateMoveToRel: " + xdest + " " + ydest);
        if ((action.getID() == aid) &&
                (action.getStatus() == ActionStatus.PROGRESS)) {
            updateMoveToRel(xdest, ydest);
            return true;
        }
        return false;
    }

    /**
     * Traverse (i.e., move forward until an obstacle is detected ahead, but
     * avoiding walls, etc.).
     *
     * @return an identifying timestamp for the move action
     */
    @Override
    public long traverse() throws RemoteException {
        log.debug("traverse");
        return newAction.initiateAction(ActionType.TRAVERSE);
    }

    /**
     * Turn to a global heading.
     *
     * @param tdest the global heading (in radians) to which to turn
     * @return an identifying timestamp for the turn action
     */
    @Override
    public long turnTo(double tdest) throws RemoteException {
        log.debug("turnTo: " + tdest);
        return newAction.initiateAction(ActionType.TURNTO, tdest);
    }

    /**
     * Turn to a relative heading.
     *
     * @param x x
     * @param y y
     * @return an identifying timestamp for the turn action
     */
    @Override
    public long turnToPoint(double x, double y) throws RemoteException {
        log.debug("turnToPoint: " + x + " " + y);
        return newAction.initiateAction(ActionType.TURNTOPOINT, x, y);
    }

    /**
     * Turn a specified distance.
     *
     * @param dist the distance (in radians) to turn
     * @return an identifying timestamp for the turn action
     */
    @Override
    public long turnDist(double dist) throws RemoteException {
        log.debug("turnDist: " + dist);
        return newAction.initiateAction(ActionType.TURNDIST, dist);
    }

    /**
     * Turn a specified distance using time instead of global pose.
     *
     * @param dist the distance (in radians) to turn
     * @return an identifying timestamp for the turn action
     */
    @Override
    public long timeTurn(double dist) throws RemoteException {
        log.debug("timeTurn: " + dist);
        return newAction.initiateAction(ActionType.TIMETURN, dist);
    }

    /**
     * Approach visual referent
     *
     * @param key the Vision reference key to approach
     * @return an identifying timestamp for the approach action
     */
    @Override
    public long approachVisRef(Long key) throws RemoteException {
        log.debug("approachVisRef: " + key);
        return newAction.initiateAction(ActionType.APPROACHVISREF, key);
    }

    /**
     * Approach visual referent
     *
     * @param key    the Vision reference key to approach
     * @param server the Vision server to query
     * @return an identifying timestamp for the approach action
     */
    @Override
    public long approachVisRef(Long key, String server) throws RemoteException {
        log.debug("approachVisRef: " + key + " from " + server);
        visionVersion = server;
        return newAction.initiateAction(ActionType.APPROACHVISREF, key);
    }

    /**
     * Approach visual color
     *
     * @param color the Vision color to approach
     * @return an identifying timestamp for the approach action
     */
    @Override
    public long approachVisColor(String color) throws RemoteException {
        log.debug("approachVisColor: " + color);
        return newAction.initiateAction(ActionType.APPROACHVISCOLOR, color);
    }

    /**
     * Approach visual color
     *
     * @param color  the Vision color to approach
     * @param server the Vision server to query
     * @return an identifying timestamp for the approach action
     */
    @Override
    public long approachVisColor(String color, String server) throws RemoteException {
        log.debug("approachVisColor: " + color + " from " + server);
        visionVersion = server;
        return newAction.initiateAction(ActionType.APPROACHVISCOLOR, color);
    }

    /**
     * Check status of current Motion.  Soon this will be obsoleted, as the
     * motion commands will notify Action of their completions.
     *
     * @param aid the identifying timestamp of the action to check
     * @return the status found for the indicated action
     */
    @Override
    public ActionStatus checkMotion(long aid) throws RemoteException {
        Action checkAction;

        if (actionList.containsKey(aid)) {
            checkAction = actionList.get(aid);
            return checkAction.getStatus();
        }
        return ActionStatus.UNKNOWN;
    }

    /**
     * Cancel current Motion.
     *
     * @param aid the identifying timestamp of the action to cancel
     * @return true if action was canceled, false otherwise (i.e., if that
     *         action ID was not active)
     */
    @Override
    public boolean cancelMotion(long aid) throws RemoteException {
        return action.cancelAction(aid);
    }

    /**
     * Suspend current Motion.
     *
     * @param aid the identifying timestamp of the action to suspend
     * @return true if action was canceled, false otherwise (i.e., if that
     *         action ID was not active)
     */
    @Override
    public boolean suspendMotion(long aid) throws RemoteException {
        return action.suspendAction(aid);
    }

    /**
     * Restore given Motion.
     *
     * @param aid the identifying timestamp of the action to restore
     * @return true if action was restored, false otherwise (i.e., if that
     *         action ID was unknown)
     */
    @Override
    public boolean restoreMotion(long aid) throws RemoteException {
        Action restore;

        if (!actionList.containsKey(aid)) {
            return false;
        }
        synchronized (newAction) {
            action.cancelAction();
            restore = actionList.get(aid);
            restore.stat = ActionStatus.PROGRESS;
            doInitiate(restore);
            action = restore;
        }
        return true;
    }

    /**
     * Get ID of current Motion.
     *
     * @return action ID of current motion, or -1 if none
     */
    @Override
    public long getCurrentMotion() throws RemoteException {
        if (action.getStatus() == ActionStatus.PROGRESS) {
            return action.getID();
        }
        return -1L;
    }

    /**
     * Get approach tolerance (how closely the robot must approach)
     *
     * @return the current tolerance distance
     */
    @Override
    public double getTolerance() throws RemoteException {
        return dEpsilon;
    }

    /**
     * Set approach tolerance (how closely the robot must approach)
     *
     * @param dist the new tolerance distance
     */
    @Override
    public void setTolerance(double dist) throws RemoteException {
        log.trace("Changing tolerance from " + dEpsilon + " to " + dist);
        dEpsilon = dist;
    }
    /* **** VelocityComponent interface **** */

    /**
     * Get translational velocity.
     *
     * @return the most recent TV reading.
     */
    @Override
    public double getTV() throws RemoteException {
        return nominal[TV];
    }

    /**
     * Get rotational velocity.
     *
     * @return the most recent RV reading.
     */
    @Override
    public double getRV() throws RemoteException {
        return nominal[RV];
    }

    /**
     * Get translational and rotational velocity.
     *
     * @return the most recent velocity readings.
     */
    @Override
    public double[] getVels() throws RemoteException {
        return new double[]{nominal[TV], nominal[RV]};
    }

    /**
     * Set the default velocities used by Motion functions.
     *
     * @param tv new default translational velocity
     * @param rv new default rotational velocity
     */
    //@Override // PWS: consider adding to API
    public void setDefaultVels(double tv, double rv) throws RemoteException {
        defTV = tv;
        defRV = rv;
    }

    /**
     * Get the default velocities used by Motion functions.
     *
     * @return the default velocities.
     */
    @Override
    public double[] getDefaultVels() throws RemoteException {
        return new double[]{defTV, defRV};
    }

    /** Stop. */
    @Override
    public void stop() throws RemoteException {
        log.debug("stop");
        newAction.initiateAction(ActionType.STOP);
    }

    /**
     * Set translational velocity.
     *
     * @param tv the new TV
     * @return true if there's nothing in front of the robot, false
     *         otherwise.
     */
    @Override
    public boolean setTV(double tv) throws RemoteException {
        log.debug("setTV: " + tv);
        newAction.initiateAction(ActionType.SETTV, tv);
        return safeFront;
    }

    /**
     * Set rotational velocity, if it's safe.
     *
     * @param rv the new RV
     * @return true if it's safe to make that turn, false otherwise
     */
    @Override
    public boolean setRV(double rv) throws RemoteException {
        log.debug("setRV: " + rv);
        newAction.initiateAction(ActionType.SETRV, rv);
        return true;
    }


    /**
     * Set both velocities.
     *
     * @param tv the new TV
     * @param rv the new RV
     * @return true if there's no obstacle preventing the motion, false
     *         otherwise.
     */
    @Override
    public boolean setVels(double tv, double rv) throws RemoteException {
        boolean retval = true;
        log.debug("setVels: " + tv + " " + rv);
        newAction.initiateAction(ActionType.SETVELS, tv, rv);
        if (tv > 0.0) {
            retval = safeFront;
        }
        return retval;
    }

    /**
     * Return a three-element array of (x, y, theta) position.
     *
     * @return A three-element <tt>double</tt> array (x,y,t)
     * @throws RemoteException If an error occurs
     */
    @Override
    public double[] getPoseEgo() {
        double[] pose = null;
        if (velocityComponent.isReady())
            pose = velocityComponent.call("getPoseEgo", double[].class);
        return pose;
    }
    // *********************************************************************
    // Methods for local use
    // *********************************************************************

    /**
     * Set the velocities.  This is just a wrapper for a call to the Vel server.
     *
     * @param tv the new translational velocity
     * @param rv the new rotational velocity
     */
    private boolean setSpeed(double tv, double rv) {
        Boolean r = false;
        log.trace(format("[setSpeed] :: Translational: %f Rotational: %f", tv, rv));
        if (reinitCarmenLoc)
            return false;

        if (velocityComponent.isReady()) {
            r = velocityComponent.call("setVels", Boolean.class, tv, rv);
        }

        if (r != null && r) {
            nominal[TV] = tv;
            nominal[RV] = rv;
        }

        return r;
    }

    /**
     * Perform initialization of new action.  This is called from the
     * {@link com.motion.MotionComponentImpl#updater} loop when a new action is created by a
     * remote call.
     */
    private void doInitiate(Action initAction) {
        avobst = avoidObstacles;
        doingMotion = false;
        switch (initAction.getType()) {
            case MOVETHROUGH:
                initiateMoveThrough(initAction.getArg(0), initAction.getArg(1), initAction.getArg(2), initAction.getArg(3));
                break;
            case MOVETO:
                initiateMoveTo(initAction.getArg(0), initAction.getArg(1));
                break;
            case MOVEDIST:
                initiateMoveDist(initAction.getArg(0));
                break;
            case TIMEMOVE:
                initiateTimeMove(initAction.getArg(0));
                break;
            case MOVETOREL:
                initiateMoveToRel(initAction.getArg(0), initAction.getArg(1));
                break;
            case TURNTO:
                initiateTurnTo(initAction.getArg(0));
                break;
            case TURNTOPOINT:
                initiateTurnToPoint(initAction.getArg(0), initAction.getArg(1));
                break;
            case TURNDIST:
                initiateTurnDist(initAction.getArg(0));
                break;
            case TIMETURN:
                initiateTimeTurn(initAction.getArg(0));
                break;
            case APPROACHVISREF:
                initiateApproachVisRef(initAction.getLarg(0));
                break;
            case APPROACHVISCOLOR:
                initiateApproachVisColor(initAction.getStarg(0));
                break;
            case TRAVERSE:
                initiateTraverse();
                break;
            case STOP:
                setSpeed(0.0, 0.0);
                break;
            case SETVELS:
                setSpeed(initAction.getArg(0), initAction.getArg(1));
                break;
            case SETTV:
                setSpeed(initAction.getArg(0), nominal[RV]);
                break;
            case SETRV:
                setSpeed(nominal[TV], initAction.getArg(0));
                break;
            default:
                log.error("Unknown action type");
                break;
        }
        initAction.doneInit();
    }

    private ArrayList<Double> getPoint(double x1, double y1, double x2, double y2, double d) {
        double k = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
        double x, y;
        ArrayList<Double> a = new ArrayList<Double>();

        k = d / k;
        x = x2 - k * (x2 - x1);
        y = y2 - k * (y2 - y1);
        a.add(x);
        a.add(y);
        return a;
    }

    /**
     * Initiate data structures for {@link com.motion.MotionComponentImpl#moveThrough(double, double, double, double)}.
     *
     * @param xdest global x-coordinate of doorway
     * @param ydest global y-coordinate of doorway
     */
    private void initiateMoveThrough(double xdest, double ydest, double xdest1, double ydest1) {
        double dist;
        ArrayList<Double> p;

        setSpeed(0.0, 0.0);

        origin[X] = position[X];
        origin[Y] = position[Y];
        doorway[X] = xdest;
        doorway[Y] = ydest;
        doorway[X1] = xdest1;
        doorway[Y1] = ydest1;
        log.trace("moveThrough " + xdest + "," + ydest);

        dist = getDistanceFrom(origin[X], origin[Y], doorway[X], doorway[Y]);
        if (dist > 0.6) {
            moveThroughState = APPROACH;
            initiateMoveTo(doorway[X1], doorway[Y1]);
        } else {
            moveThroughState = APPROACHED;
        }
    }

    /** Monitor progress of {@link com.motion.MotionComponentImpl#moveThrough(double, double, double, double)}. */
    private void monitorMoveThrough() {
        ArrayList<Double> p;
        if (moveThroughState == APPROACH) {
            // Let moveTo handle it
            monitorMoveTo();
        } else if (moveThroughState == APPROACHED) {
            // Prepare for turnDist
            double t = getHeadingFrom(position[X], position[Y], doorway[X], doorway[Y]);
            t = t - position[THETA];
            moveThroughState = TURN;
            initiateTurnDist(t);
        } else if (moveThroughState == TURN) {
            // Let turnDist handle it
            monitorTurn();
        } else if (moveThroughState == TURNED) {
            // Prepare for moveTo
            p = getPoint(position[X], position[Y], doorway[X], doorway[Y], -0.65);
            moveThroughState = THROUGH;
            initiateMoveTo(p.get(0), p.get(1));
        } else if (moveThroughState == THROUGH) {
            // let moveTo handle it
            monitorMoveTo();
        }
    }

    private void initiateTraverse() {
        log.debug("Traverse");
        setSpeed(slowTV, 0.0);
        rightRangeHist = 0.0;
        rightRangeReadings = 0;
        leftRangeHist = 0.0;
        leftRangeReadings = 0;
    }

    private void monitorTraverse() {
        double xcurr, ycurr, tcurr, xdiff, ydiff, tdiff;
        double xtemp, ytemp, dist, fobst = 0.7, expon = 2.5;
        double xcrit, ycrit, tcrit, dcrit = CRITICALDIST / 1.0;
        double tscale = 1.0, rscale = 1.0, t;

        xcurr = position[X];
        ycurr = position[Y];
        tcurr = position[THETA];

        if (!safeFront) {
            log.trace("Encountered obstacle, stopping traverse");
            setSpeed(0.0, 0.0);
            action.completeAction();
            return;
        }
        dist = 5;
        // Pick a point straight ahead
        xtemp = dist * Math.cos(tcurr);
        ytemp = dist * Math.sin(tcurr);

        double[] readings;
        boolean balance = false;
        if (scanAngle == 0.0) {
            scanAngle = laserComponent.call("getScanAngle", Double.class);
            numReadings = laserComponent.call("getNumLaserReadings", Integer.class);
            double readingsPerRad = numReadings / scanAngle;
            int sideReading = (int) ((Math.PI / 2) * readingsPerRad);
            rightSideReading = (numReadings / 2) - sideReading;
            leftSideReading = (numReadings / 2) + sideReading;
            sideScanReadings = (int) ((int) ((Math.PI / 6)) * readingsPerRad);
        }
        readings = laserComponent.call("getLaserReadings", double[].class);
        double right = readings[rightSideReading];
        for (int i = rightSideReading + 1; i < rightSideReading + sideScanReadings; i++) {
            if (readings[i] < right) {
                right = readings[i];
            }
        }
        double left = readings[leftSideReading];
        for (int i = leftSideReading - sideScanReadings; i < leftSideReading; i++) {
            if (readings[i] < left) {
                left = readings[i];
            }
        }
        double error = 0.25;
        double wide = 2.0;
        if (right < wide && left < wide) {
            balance = true;
            //log.trace("Balancing "+left+" "+right);
            if (right > (left + error)) {
                tcrit = tcurr + Math.toRadians(67.5);
                xcrit = xcurr + dcrit * Math.cos(tcrit);
                ycrit = ycurr + dcrit * Math.sin(tcrit);
                xtemp += (xcurr - xcrit) * fobst / Math.pow(left / right, 1.5);
                ytemp += (ycurr - ycrit) * fobst / Math.pow(left / right, 1.5);
                //log.trace("Setting a repulsive point to left");
            } else if ((right + error) < left) {
                tcrit = tcurr + Math.toRadians(-67.5);
                xcrit = xcurr + dcrit * Math.cos(tcrit);
                ycrit = ycurr + dcrit * Math.sin(tcrit);
                xtemp += (xcurr - xcrit) * fobst / Math.pow(right / left, 1.5);
                ytemp += (ycurr - ycrit) * fobst / Math.pow(right / left, 1.5);
                //log.trace("Setting a repulsive point to right");
            }
        } else if (right < wide) {
            //log.trace("Right "+right);
            if ((right + error) < rightRangeHist) {
                tcrit = tcurr + Math.toRadians(-67.5);
                xcrit = xcurr + dcrit * Math.cos(tcrit);
                ycrit = ycurr + dcrit * Math.sin(tcrit);
                xtemp += (xcurr - xcrit) * fobst / Math.pow(right / rightRangeHist, 1.5);
                ytemp += (ycurr - ycrit) * fobst / Math.pow(right / rightRangeHist, 1.5);
            } else if ((right - error) > rightRangeHist) {
                tcrit = tcurr + Math.toRadians(67.5);
                xcrit = xcurr + dcrit * Math.cos(tcrit);
                ycrit = ycurr + dcrit * Math.sin(tcrit);
                xtemp += (xcurr - xcrit) * fobst / Math.pow(rightRangeHist / right, 1.5);
                ytemp += (ycurr - ycrit) * fobst / Math.pow(rightRangeHist / right, 1.5);
            }
        } else if (left < wide) {
            //log.trace("Left "+left);
            if ((left + error) < leftRangeHist) {
                tcrit = tcurr + Math.toRadians(67.5);
                xcrit = xcurr + dcrit * Math.cos(tcrit);
                ycrit = ycurr + dcrit * Math.sin(tcrit);
                xtemp += (xcurr - xcrit) * fobst / Math.pow(left / leftRangeHist, 1.5);
                ytemp += (ycurr - ycrit) * fobst / Math.pow(left / leftRangeHist, 1.5);
            } else if ((left - error) > leftRangeHist) {
                tcrit = tcurr + Math.toRadians(-67.5);
                xcrit = xcurr + dcrit * Math.cos(tcrit);
                ycrit = ycurr + dcrit * Math.sin(tcrit);
                xtemp += (xcurr - xcrit) * fobst / Math.pow(leftRangeHist / left, 1.5);
                ytemp += (ycurr - ycrit) * fobst / Math.pow(leftRangeHist / left, 1.5);
            }
        }
        rightRangeHist = ((rightRangeHist * rightRangeReadings) + right) / ++rightRangeReadings;
        leftRangeHist = ((leftRangeHist * leftRangeReadings) + left) / ++leftRangeReadings;


        if (!balance) {
            // Add forces away from obstacles
            if (!openLeft) { // Left obstacle
                tcrit = tcurr + Math.toRadians(67.5);
                xcrit = xcurr + dcrit * Math.cos(tcrit);
                ycrit = ycurr + dcrit * Math.sin(tcrit);
                xtemp += (xcurr - xcrit) * fobst / Math.pow(dcrit, expon);
                ytemp += (ycurr - ycrit) * fobst / Math.pow(dcrit, expon);
                //log.trace("Setting a repulsive point to left");
            }
            if (!openRight) { // Right obstacle
                tcrit = tcurr + Math.toRadians(-67.5);
                xcrit = xcurr + dcrit * Math.cos(tcrit);
                ycrit = ycurr + dcrit * Math.sin(tcrit);
                xtemp += (xcurr - xcrit) * fobst / Math.pow(dcrit, expon);
                ytemp += (ycurr - ycrit) * fobst / Math.pow(dcrit, expon);
                //log.trace("Setting a repulsive point to right");
            }
        }

        // The new (relative) heading
        // First rotate the target so heading is 0
        xdiff = Math.cos(-tcurr) * xtemp - Math.sin(-tcurr) * ytemp;
        ydiff = Math.cos(-tcurr) * ytemp + Math.sin(-tcurr) * xtemp;
        // Then calculate the angle
        tdiff = Math.atan2(ydiff, xdiff);
        //log.trace("tdiff: " + Math.toDegrees(tdiff) + "\n");

        if (tdiff > hEpsilon) {
            if (safeFront && safeLeft) {
                setSpeed(slowTV * tscale, slowRV * rscale);
            } else {
                setSpeed(0.0, slowRV * rscale);
            }
        } else if (tdiff <= -hEpsilon) {
            if (safeFront && safeRight) {
                setSpeed(slowTV * tscale, -slowRV * rscale);
            } else {
                setSpeed(0.0, -slowRV * rscale);
            }
        } else {
            if (safeFront) {
                setSpeed(slowTV * tscale, 0.0);
            }
        }
    }

    /**
     * Initiate data structures for {@link com.motion.MotionComponentImpl#moveTo(double, double)}.
     *
     * @param xdest global x-coordinate of destination
     * @param ydest global y-coordinate of destination
     */
    private void initiateMoveTo(double xdest, double ydest) {
        double dist;

        setSpeed(0.0, 0.0);

        origin[X] = position[X];
        origin[Y] = position[Y];
        target[X] = xdest;
        target[Y] = ydest;
        // Calculate the initial force vector
        history[X] = xdest - position[X];
        history[Y] = ydest - position[Y];
        if (useGPS)
            dist = lldist(position[X], position[Y], xdest, ydest);
        else
            dist = Math.sqrt(history[X] * history[X] + history[Y] * history[Y]);
        log.trace("moveTo " + xdest + "," + ydest + " (" + dist + ")");
        // Might as well check...
        if (dist <= dEpsilon) {
            if (moveThroughState == APPROACH)
                moveThroughState = APPROACHED;
            else
                newAction.completeAction();
        } else {
            history[X] /= (dist * dist);
            history[Y] /= (dist * dist);
        }
        history[X] = 0;
        history[Y] = 0;
        history[DIST] = position[THETA];
    }

    /**
     * Initiate data structures for {@link com.motion.MotionComponentImpl#moveDist(double)}.
     *
     * @param dist distance to move
     */
    private void initiateMoveDist(double dist) {
        setSpeed(0.0, 0.0);
        // Might as well check...
        if (Math.abs(dist) <= dEpsilon) {
            action.completeAction();
        } else {
            origin[X] = position[X];
            origin[Y] = position[Y];
            target[DIST] = dist; // Relative move distance
        }
    }

    /** Monitor progress of {@link com.motion.MotionComponentImpl#moveDist(double)}. */
    private void monitorMoveDist() {
        double xtemp;
        double ytemp;
        double dist;
        double tscale = 1.0;

        if (useGPS) {
            dist = lldist(origin[X], origin[Y], position[X], position[Y]);
        } else {
            xtemp = (origin[X] - position[X]) * 100;
            ytemp = (origin[Y] - position[Y]) * 100;
            dist = Math.sqrt(xtemp * xtemp + ytemp * ytemp) / 100;
        }

        log.trace("Motion has moved " + dist + " of " + target[DIST] + " so far");
        if ((dist + dEpsilon) < Math.abs(target[DIST])) {
            if ((target[DIST] > 0) && safeFront) { // Go
                if (dist > 0.75) { // Not too close
                    tscale = 1.0;
                } else if (dist > 0.2) { // Pretty close
                    tscale = 0.75;
                } else { // Very close
                    tscale = 0.5;
                }
                setSpeed(slowTV * tscale, 0.0);
            } else if (target[DIST] < 0) { // Back up!
                setSpeed(-slowTV / 3, 0.0);
            } else { // Fail
                setSpeed(0.0, 0.0);
                log.warn("!!!!!!! PWS: Disabled failAction in moveDist for now !!!!!!!");
                //action.failAction();
                action.completeAction();
                log.debug("moveDist failed at " + position[X] + ", " + position[Y]);
            }
        } else {
            setSpeed(0.0, 0.0);
            action.completeAction();
            log.debug("moveDist completed at " + position[X] + ", " + position[Y]);
        }
        //log.trace("Motion moveDist complete; moved " + dcurr + " of " + distance);
    }

    /**
     * Initiate data structures for {@link com.motion.MotionComponentImpl#timeMove(double)}.
     *
     * @param dist distance to move
     */
    private void initiateTimeMove(double dist) {
        setSpeed(0.0, 0.0);
        //log.trace(prg + ": moveDist " + dist);
        target[Y] = (dist * 1000) / slowTV;
        //log.trace("Need to move for " + target[Y]);
        //log.trace(prg + ": moving " + target[Y] + " millis to move " + dist);
        origin[Y] = (double) System.currentTimeMillis();
    }

    /** Monitor progress of {@link com.motion.MotionComponentImpl#timeMove(double)} */
    private void monitorTimeMove() {
        double torig, tcurr, tdiff = 0.0, odiff = 0.0, dist;

        dist = (double) System.currentTimeMillis() - origin[Y];
        if (Math.abs(target[Y]) < dist) {
            setSpeed(0.0, 0.0);
            //dist = (dist/1000)*slowTV;
            log.debug("Motion timeMove complete.");
            action.completeAction();
        } else if (target[Y] >= 0) {
            setSpeed(slowTV, 0.0);
        } else {
            setSpeed(-slowTV, 0.0);
        }
    }

    /**
     * Initiate data structures for {@link com.motion.MotionComponentImpl#moveToRel(double, double)}.
     *
     * @param xdest relative x-coordinate of destination
     * @param ydest relative y-coordinate of destination
     */
    private void initiateMoveToRel(double xdest, double ydest) {
        double dist;

        setSpeed(0.0, 0.0);

        log.trace("iMTR Current: " + position[X] + " " + position[Y] + " " + position[THETA]);
        double tcurr = position[THETA] - Math.toRadians(90);
        double xdiff = Math.cos(tcurr) * xdest - Math.sin(tcurr) * ydest;
        double ydiff = Math.cos(tcurr) * ydest + Math.sin(tcurr) * xdest;

        origin[X] = position[X];
        origin[Y] = position[Y];
        target[X] = xdiff + position[X];
        target[Y] = ydiff + position[Y];
        // Calculate the initial force vector
        history[X] = xdiff;
        history[Y] = ydiff;
        dist = Math.sqrt(history[X] * history[X] + history[Y] * history[Y]);
        log.trace("moveToRel " + xdest + "," + ydest + " (" + dist + ")");
        log.trace("moveToRel " + xdiff + "," + ydiff + " (" + dist + ")");
        log.trace("moveToRel " + target[X] + "," + target[Y] + " (" + dist + ")");
        // Might as well check...
        if (dist <= dEpsilon) {
            newAction.completeAction();
        } else {
            history[X] /= (dist * dist);
            history[Y] /= (dist * dist);
            history[DIST] = position[THETA];
        }
    }

    /**
     * Update data structures for {@link com.motion.MotionComponentImpl#moveToRel(double, double)}.
     *
     * @param xdest relative x-coordinate of destination
     * @param ydest relative y-coordinate of destination
     */
    private void updateMoveToRel(double xdest, double ydest) {
        double dist;

        //log.trace("Current: " + position[X] + " " + position[Y] + " " + position[THETA]);
        double tcurr = position[THETA] - Math.toRadians(90);
        double xdiff = Math.cos(tcurr) * xdest - Math.sin(tcurr) * ydest;
        double ydiff = Math.cos(tcurr) * ydest + Math.sin(tcurr) * xdest;

        origin[X] = position[X];
        origin[Y] = position[Y];
        target[X] = xdiff + position[X];
        target[Y] = ydiff + position[Y];
        // Calculate the initial force vector
        history[X] = xdiff;
        history[Y] = ydiff;
        dist = Math.sqrt(history[X] * history[X] + history[Y] * history[Y]);
        //log.trace(prg + ": moveToRel " + xdest + "," + ydest + " (" + dist + ")");
        //log.trace(prg + ": moveToRel " + xdiff + "," + ydiff + " (" + dist + ")");
        //log.trace(prg + ": moveToRel " + target[X] + "," + target[Y] + " (" + dist + ")");
        // Might as well check...
        if (dist <= dEpsilon) {
            newAction.completeAction();
        } else {
            history[X] /= (dist * dist);
            history[Y] /= (dist * dist);
            history[DIST] = position[THETA];
        }
    }

    /**
     * Monitor progress of {@link com.motion.MotionComponentImpl#moveTo(double, double)}.  This version of
     * moveTo will try to go around obstacles in the path between the robot
     * and the goal.
     */
    private void monitorMoveTo() {
        double xcurr, ycurr, tcurr, xdiff, ydiff, tdiff;
        double xtemp, ytemp, dist, fobst = 0.2, expon = 2.5;
        double xcrit, ycrit, tcrit, dcrit = CRITICALDIST / 1.0;
        double tscale, rscale;

        xcurr = position[X];
        ycurr = position[Y];
        tcurr = position[THETA];

        // Calculate force toward target
        // Destination has factor 1.0; adjust obst factor (fobst)
        xtemp = target[X] - xcurr;
        ytemp = target[Y] - ycurr;
        if (useGPS)
            dist = lldist(position[X], position[Y], target[X], target[Y]);
        else
            dist = Math.sqrt(xtemp * xtemp + ytemp * ytemp);
        log.trace("Motion dist: " + dist + " (" + dEpsilon + ")");
        if (dist <= dEpsilon) {
            setSpeed(0.0, 0.0);
            if (moveThroughState == APPROACH) {
                moveThroughState = APPROACHED;
            } else {
                moveThroughState = 0;
                action.completeAction();
            }
            log.debug("Motion arrived at " + xcurr + ", " + ycurr + ", " + Math.toDegrees(tcurr));

            log.trace("Motion arrived at " + xcurr + ", " + ycurr + ", " + Math.toDegrees(tcurr));
            return;
        }
        //xtemp /= dist;
        //ytemp /= dist;
        //xtemp /= (dist * dist);
        //ytemp /= (dist * dist);


        //log.trace("opens: " + openLeft + " " + openFront + " " + openRight);
        xdiff = Math.cos(-tcurr) * xtemp - Math.sin(-tcurr) * ytemp;
        ydiff = Math.cos(-tcurr) * ytemp + Math.sin(-tcurr) * xtemp;
        // Then calculate the angle
        tdiff = Math.atan2(ydiff, xdiff);
        log.trace("Motion current " + xcurr + ", " + ycurr + ", " + tcurr);
        log.trace("Motion diff " + xdiff + ", " + ydiff + ", " + tdiff);
        if (Math.abs(tdiff) > 1.5707963268) {
            // it's behind us, turn to face it.
        } else if ((!openRight) && (!openLeft) && (!openFront)) {
            xdiff = Math.cos(-tcurr) * xtemp - Math.sin(-tcurr) * ytemp;
            ydiff = Math.cos(-tcurr) * ytemp + Math.sin(-tcurr) * xtemp;
            // Then calculate the angle
            tdiff = Math.atan2(ydiff, xdiff);

            if (tdiff < 0.0) { // Front left obstacle
                // Place the point ahead at 45 degrees
                tcrit = tcurr + Math.toRadians(45);
                xcrit = xcurr + dcrit * Math.cos(tcrit);
                ycrit = ycurr + dcrit * Math.sin(tcrit);
                // Add the repulsive force from the point
                xtemp += (xcurr - xcrit) * fobst / Math.pow(dcrit, expon);
                ytemp += (ycurr - ycrit) * fobst / Math.pow(dcrit, expon);
                log.trace("Setting a repulsive point to forward left");
            } else { // Front right obstacle
                tcrit = tcurr + Math.toRadians(-45);
                xcrit = xcurr + dcrit * Math.cos(tcrit);
                ycrit = ycurr + dcrit * Math.sin(tcrit);
                xtemp += (xcurr - xcrit) * fobst / Math.pow(dcrit, expon);
                ytemp += (ycurr - ycrit) * fobst / Math.pow(dcrit, expon);
                log.trace("Setting a repulsive point to forward right");
            }
        } else if ((!openLeft) && (!openFront)) { // Front left obstacle
            // Place the point ahead at 45 degrees
            tcrit = tcurr + Math.toRadians(45);
            xcrit = xcurr + dcrit * Math.cos(tcrit);
            ycrit = ycurr + dcrit * Math.sin(tcrit);
            // Add the repulsive force from the point
            xtemp += (xcurr - xcrit) * fobst / Math.pow(dcrit, expon);
            ytemp += (ycurr - ycrit) * fobst / Math.pow(dcrit, expon);
            log.trace("Setting a repulsive point to forward left");
        } else if ((!openRight) && (!openFront)) { // Front right obstacle
            tcrit = tcurr + Math.toRadians(-45);
            xcrit = xcurr + dcrit * Math.cos(tcrit);
            ycrit = ycurr + dcrit * Math.sin(tcrit);
            xtemp += (xcurr - xcrit) * fobst / Math.pow(dcrit, expon);
            ytemp += (ycurr - ycrit) * fobst / Math.pow(dcrit, expon);
            log.trace("Setting a repulsive point to forward right");
        } else if ((!openLeft) && openFront) { // Left obstacle
            tcrit = tcurr + Math.toRadians(67.5);
            xcrit = xcurr + dcrit * Math.cos(tcrit);
            ycrit = ycurr + dcrit * Math.sin(tcrit);
            xtemp += (xcurr - xcrit) * fobst / Math.pow(dcrit, expon);
            ytemp += (ycurr - ycrit) * fobst / Math.pow(dcrit, expon);
            log.trace("Setting a repulsive point to left");
        } else if ((!openRight) && openFront) { // Right obstacle
            tcrit = tcurr + Math.toRadians(-67.5);
            xcrit = xcurr + dcrit * Math.cos(tcrit);
            ycrit = ycurr + dcrit * Math.sin(tcrit);
            xtemp += (xcurr - xcrit) * fobst / Math.pow(dcrit, expon);
            ytemp += (ycurr - ycrit) * fobst / Math.pow(dcrit, expon);
            log.trace("Setting a repulsive point to right");
        } else if (openLeft && openRight && (!openFront)) { // Front obstacle
            if (System.currentTimeMillis() > phantomTime) {
                xdiff = Math.cos(-tcurr) * xtemp - Math.sin(-tcurr) * ytemp;
                ydiff = Math.cos(-tcurr) * ytemp + Math.sin(-tcurr) * xtemp;
                // Then calculate the angle
                tdiff = Math.atan2(ydiff, xdiff);
                if (tdiff < 0.0)
                    phantomLeft = true;
                else
                    phantomLeft = false;
                // give it 20 sec. to get around the obstacle
                phantomTime = System.currentTimeMillis() + 20000L;
            }

            if (phantomLeft) { // Front left obstacle
                // Place the point ahead at 45 degrees
                tcrit = tcurr + Math.toRadians(45);
                xcrit = xcurr + dcrit * Math.cos(tcrit);
                ycrit = ycurr + dcrit * Math.sin(tcrit);
                // Add the repulsive force from the point
                xtemp += (xcurr - xcrit) * fobst / Math.pow(dcrit, expon);
                ytemp += (ycurr - ycrit) * fobst / Math.pow(dcrit, expon);
                log.trace("Setting a repulsive point to forward left");
            } else { // Front right obstacle
                tcrit = tcurr + Math.toRadians(-45);
                xcrit = xcurr + dcrit * Math.cos(tcrit);
                ycrit = ycurr + dcrit * Math.sin(tcrit);
                xtemp += (xcurr - xcrit) * fobst / Math.pow(dcrit, expon);
                ytemp += (ycurr - ycrit) * fobst / Math.pow(dcrit, expon);
                log.trace("Setting a repulsive point to forward right");
            }
        }

        // Add in history
        xtemp = (1 - histFactor) * xtemp + histFactor * history[X];
        ytemp = (1 - histFactor) * ytemp + histFactor * history[Y];
        history[X] = xtemp;
        history[Y] = ytemp;

        // The new (relative) heading
        // First rotate the target so heading is 0
        xdiff = Math.cos(-tcurr) * xtemp - Math.sin(-tcurr) * ytemp;
        ydiff = Math.cos(-tcurr) * ytemp + Math.sin(-tcurr) * xtemp;
        // Then calculate the angle
        tdiff = Math.atan2(ydiff, xdiff);
        log.trace("Adjusted diff " + xdiff + ", " + ydiff + ", " + tdiff);

        // Scale the turn (should make this continuous)
        if (Math.abs(tdiff) > 1.5707963268) { // Need to turn a LOT
            rscale = 1.5;
            tscale = 0.1;
        } else if (Math.abs(tdiff) > 1.1780972451) { // Need to turn a LOT
            rscale = 1.5;
            if (dist > 0.75) { // Not too close
                tscale = 0.75;
            } else if (dist > 0.2) { // Pretty close
                tscale = 0.25;
            } else { // Very close, turn in place
                tscale = 0;
            }
        } else if (Math.abs(tdiff) > 0.7853981634) { // Need to turn a lot
            rscale = 1.5;
            if (dist > 0.75) { // Not too close
                tscale = 1.0;
            } else if (dist > 0.2) { // Pretty close
                tscale = 0.5;
            } else { // Very close, turn in place
                tscale = 0;
            }
        } else {
            rscale = 1.0;
            if (dist > 0.75) { // Not too close
                tscale = 1.0;
            } else if (dist > 0.2) { // Pretty close
                tscale = 0.5;
            } else { // Very close
                tscale = 0.25;
            }
        }
        if ((useSegway || useCart) && tscale < 1)
            tscale = 1;
        else if (useSegway && tscale < 0.5)
            tscale = 0.5;

        if (tdiff > hEpsilon) {
            if (safeFront) {
                setSpeed(slowTV * tscale, slowRV * rscale);
            } else {
                setSpeed(0.0, slowRV * rscale);
            }
        } else if (tdiff <= -hEpsilon) {
            if (safeFront) {
                setSpeed(slowTV * tscale, -slowRV * rscale);
            } else {
                setSpeed(0.0, -slowRV * rscale);
            }
        } else {
            if (safeFront) {
                setSpeed(slowTV * tscale, 0.0);
            } else {
                setSpeed(0.0, -slowRV * rscale);
            }
        }
    }

    /**
     * Initiate data structures for {@link com.motion.MotionComponentImpl#turnTo(double)}.
     *
     * @param tdest distance to move
     */
    private void initiateTurnTo(double tdest) {
        setSpeed(0.0, 0.0);
        log.trace("turnTo " + tdest);
        if (useSegway) {
            log.warn("turnTo is unreliable on Segway base without compass (GPS heading is reliable only when in motion)");
        }
        origin[THETA] = position[THETA];
        // Convert to relative theta
        target[THETA] = tdest - position[THETA];
        if (target[THETA] < -Math.PI) {
            target[THETA] += 2 * Math.PI;
        } else if (target[THETA] > Math.PI) {
            target[THETA] -= 2 * Math.PI;
        }
        // Might as well check...
        if (Math.abs(target[THETA]) <= tEpsilon) {
            if (moveThroughState == TURN) {
                moveThroughState = TURNED;
            } else {
                action.completeAction();
            }
        }
    }

    /**
     * Initiate data structures for {@link com.motion.MotionComponentImpl#turnToPoint(double, double)}}.
     *
     * @param x x
     * @param y y
     */
    private void initiateTurnToPoint(double x, double y) {
        setSpeed(0.0, 0.0);
        target[X] = x;
        target[Y] = y;
        target[THETA] = getHeadingFrom(0, 0, x, y);
        if (target[THETA] < -Math.PI) {
            target[THETA] += 2 * Math.PI;
        } else if (target[THETA] > Math.PI) {
            target[THETA] -= 2 * Math.PI;
        }
        log.trace("turnToPoint " + x + " " + y + " " + target[THETA]);
        // Might as well check...
        if (Math.abs(target[THETA]) <= tEpsilon) {
            if (moveThroughState == TURN) {
                moveThroughState = TURNED;
            } else {
                action.completeAction();
            }
        }
    }

    /**
     * Initiate data structures for {@link com.motion.MotionComponentImpl#turnDist(double)}.
     *
     * @param dist distance to turn
     */
    private void initiateTurnDist(double dist) {
        setSpeed(0.0, 0.0);
        //log.trace("ITD Position: " + position[X] + " " + position[Y] + " " + position[THETA]);
        //log.trace(prg + ": turnDist " + dist);
        if (dist < -Math.PI) {
            dist += 2 * Math.PI;
        } else if (dist > Math.PI) {
            dist -= 2 * Math.PI;
        }
        target[THETA] = dist;
        if (useSegway) {
            origin[THETA] = (double) System.currentTimeMillis();
            //target[THETA] = Math.toDegrees(dist) * 1000 / defRV;
            //log.trace(prg + ": turning " + target[THETA] + " millis to turn " + dist);
        } else {
            origin[THETA] = position[THETA];
        }
        // Might as well check...
        if (Math.abs(dist) <= tEpsilon) {
            //log.trace(prg + ": turnDist close enough to " + dist);
            if (moveThroughState == TURN) {
                moveThroughState = TURNED;
            } else {
                action.completeAction();
            }
        }
    }

    /**
     * Initiate data structures for {@link com.motion.MotionComponentImpl#timeTurn(double)}.
     *
     * @param dist distance to turn
     */
    private void initiateTimeTurn(double dist) {
        setSpeed(0.0, 0.0);
        //log.trace(prg + ": turnDist " + dist);
        if (dist < -Math.PI) {
            dist += 2 * Math.PI;
        } else if (dist > Math.PI) {
            dist -= 2 * Math.PI;
        }
        target[THETA] = (dist * 1000) / defRV;
        origin[THETA] = (double) System.currentTimeMillis();
        //log.trace(prg + ": turning " + target[THETA] + " millis to turn " + dist);
    }

    /**
     * Monitor progress of {@link com.motion.MotionComponentImpl#turnTo(double)} and {@link
     * com.motion.MotionComponentImpl#turnDist(double)}.
     */
    private void monitorTurn() {
        double torig, tcurr, tdiff = 0.0, odiff = 0.0, dist;

        if (useSegway) {
            dist = (double) System.currentTimeMillis() - origin[THETA];
            //log.trace(prg + ": turned " + (dist * defRV) / 1000);
            if ((Math.toDegrees(Math.abs(target[THETA])) * 1000 / defRV) < dist) {
                setSpeed(0.0, 0.0);
                if (moveThroughState == TURN) {
                    moveThroughState = TURNED;
                } else {
                    action.completeAction();
                }
            } else if (target[THETA] < 0) {
                setSpeed(0.0, -defRV);
            } else {
                setSpeed(0.0, defRV);
            }
        } else {
            dist = position[THETA] - origin[THETA];
            if (dist < -Math.PI) {
                dist = dist + 2 * Math.PI;
            } else if (dist > Math.PI) {
                dist = dist - 2 * Math.PI;
            }
            log.trace(format("target[THETA]: %5.4f, dist: %5.4f, ldist: %5.4f, curr: %5.4f\n", target[THETA], dist, position[THETA] - origin[THETA], position[THETA]));
            if ((Math.abs(dist) + tEpsilon) >= Math.abs(target[THETA])) {
                setSpeed(0.0, 0.0);
                log.trace("Turn " + target[THETA] + " complete.");
                if (moveThroughState == TURN) {
                    moveThroughState = TURNED;
                } else {
                    action.completeAction();
                }
            } else if (target[THETA] < 0) {
                setSpeed(0.0, -defRV * 1.5);
            } else {
                setSpeed(0.0, defRV * 1.5);
            }
        }
    }

    /** Monitor progress of {@link com.motion.MotionComponentImpl#turnToPoint(double, double)}. */
    private void monitorTurnToPoint() {
        double torig, tcurr, tdiff = 0.0, odiff = 0.0, dist;

        dist = getHeadingFrom(0, 0, target[X], target[Y]) - Math.toRadians(90);
        if (dist < -Math.PI) {
            dist = dist + 2 * Math.PI;
        } else if (dist > Math.PI) {
            dist = dist - 2 * Math.PI;
        }
        if (Math.abs(dist) < tEpsilon) {
            setSpeed(0.0, 0.0);
            log.trace("Turn complete.");
            if (moveThroughState == TURN) {
                moveThroughState = TURNED;
            } else {
                action.completeAction();
            }
        } else if (target[THETA] < 0) {
            setSpeed(0.0, -defRV * 0.25);
        } else {
            setSpeed(0.0, defRV * 0.25);
        }
    }

    /**
     * Monitor progress of {@link com.motion.MotionComponent#turnTo(double)} and {@link
     * com.motion.MotionComponentImpl#turnDist(double)}.
     */
    private void monitorTimeTurn() {
        double torig, tcurr, tdiff = 0.0, odiff = 0.0, dist;

        dist = (double) System.currentTimeMillis() - origin[THETA];
        if (Math.abs(target[THETA]) < dist) {
            //log.trace(prg + ": turned " + (dist * defRV) / 1000);
            setSpeed(0.0, 0.0);
            log.debug("Motion timeTurn complete.");
            if (moveThroughState == TURN) {
                moveThroughState = TURNED;
            } else {
                action.completeAction();
            }
        } else if (target[THETA] < 0) {
            setSpeed(0.0, -defRV);
        } else {
            setSpeed(0.0, defRV);
        }
    }

    /**
     * Initiate data structures for {@link com.motion.MotionComponentImpl#approachVisRef(Long)}.
     *
     * @param key the Vision ref to approach
     */
    private void initiateApproachVisRef(Long key) {
        setSpeed(0.0, 0.0);
        log.trace("approachVisRef " + key);
        targetRef = key;
        visionComponent = connectToComponent(visionVersion);
        Sleep(1000);
        Dimension d = visionComponent.call("getImageSize", Dimension.class);
        visWidth = (int) d.getWidth();
        visWidth2 = visWidth / 2;
        visHeight = (int) d.getHeight();
    }

    /**
     * Initiate data structures for {@link com.motion.MotionComponentImpl#approachVisColor(String)}.
     *
     * @param key the Vision color to approach
     */
    private void initiateApproachVisColor(String key) {
        setSpeed(0.0, 0.0);
        log.trace("approachVisColor " + key);
        visionComponent = connectToComponent(visionVersion);
        Sleep(1000);
        Dimension d = visionComponent.call("getImageSize", Dimension.class);
        if (d == null) {
            newAction.failAction();
        } else {
            // PWS: might need to wait a while for the reference
            visWidth = (int) d.getWidth();
            visWidth2 = visWidth / 2;
            visHeight = (int) d.getHeight();
        }
        targetColor = utilities.Util.createPredicate("color(X, " + key + ")");
    }

    /** Monitor progress of {@link com.motion.MotionComponentImpl#approachVisRef(Long)}. */
    private void monitorApproachVisRef() {
        MemoryObject visionToken;
        double rv, tv;
        int w = visWidth2;
        int h = visHeight;
        boolean adj = true;
        //log.trace("WH: " + w + " " + h);
        // import the functionality from LocalActionComponentArcH
        visionToken = visionComponent.call("getToken", MemoryObject.class, targetRef, 0.5);
        if (visionToken != null) {
            //log.trace("LOC: " + visionToken.xcg + " " + visionToken.ycg);
            if (visionToken.getXCG() < (w - 30)) {
                rv = 0.1;
            } else if (adj && (visionToken.getXCG() < (w - 2))) {
                rv = 0.05;
            } else if (visionToken.getXCG() > (w + 30)) {
                rv = -0.1;
            } else if (adj && (visionToken.getXCG() > (w + 2))) {
                rv = -0.05;
            } else {
                rv = 0.0;
            }
            if (adj && (visionToken.getYCG() > (h - 40))) {
                tv = 0.0;
            } else if (!adj && (visionToken.getYCG() > (h - 90))) {
                tv = 0.0;
            } else if (visionToken.getYCG() > (h - 90)) {
                tv = 0.025;
            } else {
                tv = 0.05;
            }
            //log.trace("TV: " + tv + " RV: " + rv);
            velocityComponent.call("setVels", Boolean.class, tv, rv);
            if (rv == 0.0 && tv == 0.0) {
                action.completeAction();
            }
        } else {
            tv = rv = 0.0;
            log.trace("Lost object " + targetRef);
            velocityComponent.call("setVels", Boolean.class, tv, rv);
            //action.failAction();
        }
    }

    /** Monitor progress of {@link com.motion.MotionComponentImpl#approachVisColor(String)}. */
    private void monitorApproachVisColor() {
        MemoryObject visionToken = null;
        double rv, tv;
        int w = visWidth2;
        int h = visHeight;
        boolean adj = true;
        ArrayList<Long> v = null;
        //log.trace("WH: " + w + " " + h);
        // import the functionality from LocalActionComponentArcH
        if (!visionComponent.isReady()) {
            log.trace("need to wait for Vision");
            return;
        }

        //EAK: build vision description to query for
        List<Predicate> visionTokenDescription = new ArrayList<Predicate>();
        visionTokenDescription.add(targetColor);
        visionTokenDescription.add(utilities.Util.createPredicate("type(X, blob)"));
        v = visionComponent.call("getTokenIds", ArrayList.class, visionTokenDescription, 0.5);

        if ((v != null) && (v.size() > 0)
                && !Long.class.isInstance(v.get(0))) {
            //log.trace("Requesting " + v.get(0) + " from " + visionVersion);
            visionToken = visionComponent.call("getToken", MemoryObject.class, v.get(0), 0.5);

        }
        if (visionToken != null) {
            log.trace("LOC: " + visionToken.getXCG() + " " + visionToken.getYCG());
            visLost = 0;
            if (visionToken.getXCG() < (w - 30)) {
                rv = 0.1;
            } else if (adj && (visionToken.getXCG() < (w - 2))) {
                rv = 0.05;
            } else if (visionToken.getXCG() > (w + 30)) {
                rv = -0.1;
            } else if (adj && (visionToken.getXCG() > (w + 2))) {
                rv = -0.05;
            } else {
                rv = 0.0;
            }
            if (adj && (visionToken.getYCG() > (h - 40))) {
                tv = 0.0;
            } else if (!adj && (visionToken.getYCG() > (h - 90))) {
                tv = 0.0;
            } else if (visionToken.getYCG() > (h - 90)) {
                tv = 0.025;
            } else {
                tv = 0.05;
            }
            //log.trace("TV: " + tv + " RV: " + rv);
            velocityComponent.call("setVels", Boolean.class, tv, rv);
            if (rv == 0.0 && tv == 0.0) {
                action.completeAction();
            }
        } else {
            tv = rv = 0.0;
            log.trace("Lost object " + targetColor);
            velocityComponent.call("setVels", Boolean.class, tv, rv);
            visLost++;
            if (visLost > 50) {
                action.completeAction();
            }
            //action.failAction();
        }
    }

    /** Get latest position information from Position server */
    private void updatePosition() {
        if (!localServicesReady()) return;
        double[] pose;
        // PWS: protect from bad pos reference
        if (useCarmenLoc && !gotCarmenLoc)
            return;

        if (usePos && positionComponent.isReady()) {
            pose = positionComponent.call("getPoseGlobal", double[].class);
        } else if (deadReckoning && velocityComponent.isReady()) {
            pose = velocityComponent.call("getPoseEgo", double[].class);
        } else {
            //log.trace("Can't update position!");
            return;
        }
        if (pose == null) {
            log.error("Error getting pose. Stopping.");
            setSpeed(0.0, 0.0);
            return;
        }
        position[X] = pose[X];
        position[Y] = pose[Y];
        position[THETA] = pose[THETA];
        if (position[THETA] < -Math.PI) {
            position[THETA] += 2 * Math.PI;
        } else if (position[THETA] > Math.PI) {
            position[THETA] -= 2 * Math.PI;
        }
        log.trace("Position: " + pose[X] + " " + pose[Y] + " " + position[THETA]);
        //log.trace("Position: " + pose[X] + " " + pose[Y] + " " + position[THETA]);
        log.debug("position: " + position[X] + " " + position[Y] + " " + position[THETA]);

    }

    /** Get latest readings from laser and update local data structures. */
    private void updateLRF() {
        boolean[] safes;
        boolean[] opens;
        double newTV = nominal[TV];
        double newRV = nominal[RV];
        boolean safety = false;

        safes = laserComponent.call("getSafeSpaces", boolean[].class);
        opens = laserComponent.call("getOpenSpaces", boolean[].class);
        if (safes == null || opens == null) return;

        safeRight = safes[0];
        safeFront = safes[1];
        safeLeft = safes[2];
        openRight = opens[0];
        openFront = opens[1];
        openLeft = opens[2];
        if (avobst && (nominal[TV] > 0) && (!safeFront)) {
            safety = true;
            newTV = 0.0;
        }
        /* need to allow the robot to turn in place, too many bad side effects otherwise
        if (!safeLeft && !safeRight && !safeFront) {
            // have to allow it to turn, then
        } else if (avobst && (nominal[RV] > 0) && !doingMotion && (! safeLeft)) {
            safety = true;
            newRV = 0.0;
        } else if (avobst && (nominal[RV] < 0) && !doingMotion && (! safeRight)) {
            safety = true;
            newRV = 0.0;
        }
        */
        if (safety) {
            log.trace("setting speed for safety: " + newTV + "," + newRV + "," + safeLeft + "," + safeFront + "," + safeRight);
            if (!imminentCollision) {
                imminentCollision = true;
                log.warn("IMMINENT COLLISION");
            }
            setSpeed(newTV, newRV);
        } else {
            imminentCollision = false;
        }
    }

    /**
     * The <code>Action</code> class keeps track of information specific to
     * instantiations of Motion actions and takes care of initiating actions.
     * We need an awkward two-stage action instantiation process so that RMI
     * calls can be dealt with consistently without locking the whole server
     * down so tightly that the maintenance thread can't proceed.
     */
    private class Action {
        private ActionType type;
        private ActionStatus stat;
        private boolean init;
        private long ID;
        private double[] args;
        private String[] stargs;

        private long[] largs;

        /**
         * Initiate an action requiring no parameters.
         *
         * @param t the type of action to be instantiated
         * @return the timestamp identifier
         */
        public synchronized long initiateAction(ActionType t) {
            action.cancelAction();
            switch (t) {
                case STOP:
                    init = true;
                    ID = 0;
                    break;
                case TRAVERSE:
                    init = true;
                    ID = System.currentTimeMillis();
                    stat = ActionStatus.PROGRESS;
                    break;
            }
            if (init) {
                type = t;
            }
            return ID;
        }

        /**
         * Initiate an action requiring a single <code>double</code>
         * parameter.
         *
         * @param t  the type of action to be instantiated
         * @param a0 the <code>double</code> parameter (e.g., distance)
         * @return the timestamp identifier
         */
        public synchronized long initiateAction(ActionType t, double a0) {
            action.cancelAction();
            switch (t) {
                case MOVEDIST:
                case TIMEMOVE:
                case TURNTO:
                case TURNDIST:
                case TIMETURN:
                case FOLLOWWALL:
                    init = true;
                    ID = System.currentTimeMillis();
                    stat = ActionStatus.PROGRESS;
                    break;
                case SETTV:
                case SETRV:
                    init = true;
                    ID = 0;
                    break;
            }
            if (init) {
                type = t;
                args[0] = a0;
            }
            return ID;
        }

        /**
         * Initiate an action requiring a single <code>Long</code>
         * parameter.
         *
         * @param t  the type of action to be instantiated
         * @param a0 the <code>Long</code> parameter (e.g., Vision key)
         * @return the timestamp identifier
         */
        public synchronized long initiateAction(ActionType t, Long a0) {
            action.cancelAction();
            switch (t) {
                case APPROACHVISREF:
                    init = true;
                    ID = System.currentTimeMillis();
                    stat = ActionStatus.PROGRESS;
                    break;
            }
            if (init) {
                type = t;
                largs[0] = a0;
            }
            return ID;
        }

        /**
         * Initiate an action requiring a single <code>String</code>
         * parameter.
         *
         * @param t  the type of action to be instantiated
         * @param a0 the <code>String</code> parameter (e.g., Vision key)
         * @return the timestamp identifier
         */
        public synchronized long initiateAction(ActionType t, String a0) {
            action.cancelAction();
            switch (t) {
                case APPROACHVISCOLOR:
                    init = true;
                    ID = System.currentTimeMillis();
                    stat = ActionStatus.PROGRESS;
                    break;
            }
            if (init) {
                type = t;
                stargs[0] = a0;
            }
            return ID;
        }

        /**
         * Initiate an action requiring six <code>double</code> parameters.
         *
         * @param t  the type of action to be instantiated
         * @param a0 the first <code>double</code> parameter (e.g., distance)
         * @param a1 the second <code>double</code> parameter (e.g., distance)
         * @return the timestamp identifier
         */
        public synchronized long initiateAction(ActionType t, double a0, double a1, double a2, double a3) {
            action.cancelAction();
            switch (t) {
                case MOVETHROUGH:
                    init = true;
                    ID = System.currentTimeMillis();
                    stat = ActionStatus.PROGRESS;
                    break;
            }
            if (init) {
                type = t;
                args[0] = a0;
                args[1] = a1;
                args[2] = a2;
                args[3] = a3;
            }
            return ID;
        }

        /**
         * Initiate an action requiring two <code>double</code> parameters.
         *
         * @param t  the type of action to be instantiated
         * @param a0 the first <code>double</code> parameter (e.g., distance)
         * @param a1 the second <code>double</code> parameter (e.g., distance)
         * @return the timestamp identifier
         */
        public synchronized long initiateAction(ActionType t, double a0, double a1) {
            action.cancelAction();
            switch (t) {
                case MOVETO:
                case MOVETOREL:
                case TURNTOPOINT:
                    init = true;
                    ID = System.currentTimeMillis();
                    stat = ActionStatus.PROGRESS;
                    break;
                case SETVELS:
                    init = true;
                    ID = 0;
                    break;
            }
            if (init) {
                type = t;
                args[0] = a0;
                args[1] = a1;
            }
            return ID;
        }

        /** Indicate that the action successfully completed. */
        public synchronized void completeAction() {
            stat = ActionStatus.SUCCESS;
            avobst = avoidObstacles;
            doingMotion = false;
        }

        /** Cancel the action, if it's currently in progress. */
        public synchronized void cancelAction() {
            log.debug("cancelAction: local method called");
            setSpeed(0.0, 0.0);
            if (stat == ActionStatus.PROGRESS || stat == ActionStatus.UNKNOWN) {
                stat = ActionStatus.CANCEL;
                log.debug("cancelAction: cancelling action " + type);
                //log.trace("Motion cancelling action: " + type);
            }
            moveThroughState = 0;
            avobst = avoidObstacles;
            doingMotion = false;
        }

        /**
         * Cancel the action, if it's currently in progress.
         *
         * @param i the timestamp identifier of the action to be cancelled
         * @return true if the action was in progress and its ID matched i,
         *         false otherwise
         */
        public synchronized boolean cancelAction(long i) {
            if ((stat == ActionStatus.PROGRESS || stat == ActionStatus.UNKNOWN) && (ID == i)) {
                stat = ActionStatus.CANCEL;
                setSpeed(0.0, 0.0);
                // cancelMotion is the only place this is called
                log.debug("cancelMotion: " + type);
                //log.trace("Motion cancelling action "+i+": " + type);
                moveThroughState = 0;
                avobst = avoidObstacles;
                doingMotion = false;
                return true;
            }
            return false;
        }

        /**
         * Suspend the action, if it's currently in progress.
         *
         * @param i the timestamp identifier of the action to be suspended
         * @return true if the action was in progress and its ID matched i,
         *         false otherwise
         */
        public synchronized boolean suspendAction(long i) {
            if ((stat == ActionStatus.PROGRESS || stat == ActionStatus.UNKNOWN) && (ID == i)) {
                stat = ActionStatus.SUSPEND;
                setSpeed(0.0, 0.0);
                // suspendMotion is the only place this is called
                log.debug("suspendMotion: " + type);
                //log.trace("Motion cancelling action "+i+": " + type);
                moveThroughState = 0;
                avobst = avoidObstacles;
                doingMotion = false;
                return true;
            }
            return false;
        }

        /** Indicate that the action failed. */
        public synchronized void failAction() {
            if (stat == ActionStatus.PROGRESS) {
                stat = ActionStatus.FAIL;
            }
            moveThroughState = 0;
            avobst = avoidObstacles;
            doingMotion = false;
        }

        /**
         * Get the status of this action.
         *
         * @return the status of this action.
         */
        public synchronized ActionStatus getStatus() {
            return stat;
        }

        /**
         * Get the type of this action.
         *
         * @return the type of this action.
         */
        public synchronized ActionType getType() {
            return type;
        }

        /**
         * Get the timestamp identifier of this action.
         *
         * @return the timestamp identifier of this action.
         */
        public synchronized long getID() {
            return ID;
        }

        /**
         * Get the arguments of this action.
         *
         * @return the arguments of this action.
         */
        public synchronized double getArg(int a) {
            return args[a];
        }

        /**
         * Get the arguments of this action.
         *
         * @return the arguments of this action.
         */
        public synchronized Long getLarg(int a) {
            return largs[a];
        }

        /**
         * Get the arguments of this action.
         *
         * @return the arguments of this action.
         */
        public synchronized String getStarg(int a) {
            return stargs[a];
        }

        /**
         * Get the initialization status of this action.
         *
         * @return the initialization status of this action.
         */
        public synchronized boolean getInit() {
            return init;
        }

        /** Indicate that initialization is complete. */
        public synchronized void doneInit() {
            init = false;
        }

        /** <code>Action</code> constructor. */
        public Action() {
            type = ActionType.NONE;
            stat = ActionStatus.UNKNOWN;
            init = false;
            ID = 0;
            args = new double[6];
            stargs = new String[6];
            largs = new long[6];
        }

        /**
         * <code>Action</code> constructor.
         *
         * @param prot a prototype <code>Action</code> on which to base this
         *             action.
         */
        public Action(Action prot) {
            type = prot.type;
            stat = prot.stat;
            init = prot.init;
            ID = prot.ID;
            args = new double[6];
            System.arraycopy(prot.args, 0, args, 0, 6);
        }

    }

    private class Updater extends Thread {
        long sleepTime, cycleTime = 50, elapsedTime;
        boolean shouldUpdate;

        MotionComponentImpl that;

        public Updater() {
            shouldUpdate = true;
        }

        public Updater(long ct, MotionComponentImpl t) {
            this();
            cycleTime = ct;
            that = t;
        }

        @Override
        public void run() {
            boolean test = false;
            int cycles = 0;
            while (shouldUpdate) {
                sleepTime = System.currentTimeMillis();

                synchronized (newAction) {
                    if (newAction.getInit()) {
                        doInitiate(newAction);
                        if (newAction.getID() != 0) {
                            action = new Action(newAction);
                            actionList.put(action.getID(), action);
                        }
                    }
                }

                // Get and process latest laser readings
                if (useLrf && laserComponent.isReady()) {
                    //log.trace("Updating LRF");
                    updateLRF();
                }
                // Update position
                updatePosition();

                // Perform maintenance on current action, if needed
                if (action.getStatus() == ActionStatus.PROGRESS) {
                    switch (action.getType()) {
                        case MOVETHROUGH:
                            //log.trace("Updating moveThrough");
                            monitorMoveThrough();
                            break;
                        case MOVETO:
                        case MOVETOREL:
                            //log.trace("Updating moveTo");
                            monitorMoveTo();
                            break;
                        case MOVEDIST:
                            //log.trace("Updating moveDist");
                            monitorMoveDist();
                            break;
                        case TIMEMOVE:
                            //log.trace("Updating move");
                            monitorTimeMove();
                            break;
                        case TURNTO:
                        case TURNDIST:
                            //log.trace("Updating turn");
                            monitorTurn();
                            break;
                        case TURNTOPOINT:
                            //log.trace("Updating turn");
                            monitorTurnToPoint();
                            break;
                        case TIMETURN:
                            //log.trace("Updating turn");
                            monitorTimeTurn();
                            break;
                        case TRAVERSE:
                            //log.trace("Updating traverse");
                            monitorTraverse();
                            break;
                        case APPROACHVISREF:
                            //log.trace("Updating approach vision ref");
                            monitorApproachVisRef();
                            break;
                        case APPROACHVISCOLOR:
                            //log.trace("Updating approach vision color");
                            monitorApproachVisColor();
                            break;
                        default:
                            break;
                    }
                } else {
                    //log.trace("Action "+action.getID()+" status "+action.getStatus());
                }
                elapsedTime = System.currentTimeMillis() - sleepTime;
                sleepTime = cycleTime - elapsedTime;
                if (sleepTime > 0) {
                    Sleep(sleepTime);
                }
            }
        }

        public void halt() {
            shouldUpdate = false;
        }

    }

    /** The server is always ready when it has all its required references */
    @Override
    protected boolean localServicesReady() {
        return doneConstructing && requiredConnectionsPresent();
    }
}
