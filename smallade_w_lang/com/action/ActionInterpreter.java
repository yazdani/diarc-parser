/**
 * ADE 1.0
 * Copyright 1997-2012 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@cs.tufts.edu
 *
 * ActionInterpreter.java
 *
 * Last update: December 2012
 *
 * @author Paul Schermerhorn
 *
 */

package com.action;

import ade.ADETimeoutException;
import ade.ADEReferenceException;
import ade.ADEException;
import com.*;
import com.lrf.feature.Door;
import com.vision.stm.MemoryObject;
import com.slug.dialog.NLPacket;
import com.slug.nlp.Utterance;
import com.slug.nlp.Type;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.awt.geom.Point2D;
import java.io.*;
import java.rmi.*;
import java.util.*;
import java.util.regex.*;

import static java.lang.String.format;
import static utilities.Util.*;
import utilities.NetworkingStatus;

/** <code>ActionInterpreter</code> is the primary action execution module for
 * the robot.
 */
public class ActionInterpreter extends Thread {
    private static Log log = LogFactory.getLog(ActionInterpreter.class);
    protected Stack<ActionDBEntry> ActionStack = new Stack<ActionDBEntry>();
    protected final String cmd;
    protected GoalManagerImpl ami;
    private ActionDBEntry adb;
    private ActionDBEntry motivation;
    protected ActionDBEntry currentAction;
    protected ActionDBEntry rootAction;
    protected ActionStatus rootStatus; // goal status
    protected Predicate failCond = null;
    private static boolean methodFail = false;
    private GoalManagerGoal goal = null;
    protected String description;
    private double priority = 0;
    private double cost;
    private double benefit;
    private double maxUrgency = 1.0;
    private double minUrgency = 0.0;
    private HashMap<String,Object> globalHash = new HashMap<String,Object>();

    private double posAff = 0.0;
    private double negAff = 0.0;

    static private double defTV = 0.25;
    static private double defRV = 0.2;
    static private Boolean gotDefs = false;
    static private final Object defLock = new Object();
    static protected double curTV = 0.0;
    static protected double curRV = 0.0;

    // This needs to be returned to the script, like discourse used to be
    private Long currentMotion = 0L;

    protected static String myID;
    protected static String agentname;

    boolean started = false;
    boolean shouldUpdate = false;
    boolean shouldPause = false;
    int iters = 5;

    protected int descriptive;

    protected static ArrayList<ADEPercept> currentPercepts;
    protected static HashMap<String, ADEPercept> pastPercepts;

    protected static Pattern variable = Pattern.compile("[?!]\\w+");

    protected static HashMap<String,String> manipulationReturnValues = new HashMap<String,String>();

    protected boolean initLocks = false;
    private long latestSphinx4Text = 0;

    private String instruction = null;

    /**
     * Log statement.
     * @param o the log statement
     */
    protected void logIt(Object o) {
        ami.logItASL(o);
    }

    private void setADEComponentLogging(boolean state) {
        ami.setASL(state);
    }

    // ********************************************************************
    // *** Action interpreter methods
    // ********************************************************************

    /**
     * Execute a motion primitive
     * @param action the action specification
     */
    void doMotionPrimitive(ActionDBEntry action) {
        log.debug(format("[doMotionPrimitive] Executing action %s", action));
        String mover = (String)action.getArgument("?mover");
        log.debug("in doMotion, mover is: " + mover);

        if ((mover == null) ||
            ((!mover.equalsIgnoreCase(agentname)) &&
             (!mover.equalsIgnoreCase("Rudy")) &&
             (!mover.equalsIgnoreCase("Mary")) &&
             (!mover.equalsIgnoreCase("cbot")) &&
             (!mover.equalsIgnoreCase("robot")) &&
             (!mover.equalsIgnoreCase("cramer")) &&
             (!mover.equalsIgnoreCase("jACT-R")) &&
             (!mover.equalsIgnoreCase("actr")) &&
             (!mover.equalsIgnoreCase("icarus")) &&
             (!mover.equalsIgnoreCase("self")) &&
             (!mover.equalsIgnoreCase("me")))) {
            log.error("doMotionPrimitive: WARNING! invalid actor (" +
                    mover + "); motion primitive " + action.getType() + " aborted!");
            return;
        }

        log.debug("action is " + action.getType());
        String method = null;

        if (action.isA("move-through")) {
            // move-through: move through a doorway
            Double xdest = 0.0;
            Double ydest = 0.0;
            Double xdest1 = 0.0;
            Double ydest1 = 0.0;
            try {
                xdest = action.getDouble("?xdest");
                ydest = action.getDouble("?ydest");
                xdest1 = action.getDouble("?xdest1");
                ydest1 = action.getDouble("?ydest1");
            } catch (NumberFormatException nfe) {
                String target = action.getString("?xdest");
                ADEPercept o;
                if ((o = pastPercepts.get(target.toUpperCase())) != null) {
                    xdest = o.sim_x;
                    ydest = o.sim_y;
                    xdest1 = o.sim_px;
                    ydest1 = o.sim_py;
                } else {
                    currentMotion = -1L;
                    log.debug("move-through: " + target + " unknown!");
                    action.setExitStatus(false);
                    return;
                }
            }


            try {
                currentMotion = (Long)callMethod("moveThrough", xdest, ydest, xdest1, ydest1);
                if (currentMotion < 0) {
                    log.error("startMoveThrough: moveThrough failed!");
                    action.setExitStatus(false);
                }
            } catch (ADEException ace) {
                log.error("startMoveThrough: Error sending to Motion: ", ace);
                action.setExitStatus(false);
                currentMotion = -1L;
            }
            action.setArgument("?actionID", currentMotion);
        } else if (action.isA("simpleGoToObj")){
            String loc = (String)action.getArgument("?loc");
            if(checkMethod("goTo")){
                try{
                    callMethod(0,"goTo",loc);
                }catch(Exception e){
                    System.out.println("can't goTo because... because.");
                }
            }else{
                System.out.println("I can't go to "+loc+"but I would like to find it's location and travel to it");
            }
        } else if (action.isA("doManipulationAction")) {
	    String cmd = (String)action.getArgument("?cmd");
            String hand = (String)action.getArgument("?hand");
	    log.debug("doManipulationAction cmd = "  + cmd);
	    log.debug("doManipulationAction hand(debug) = "  + hand);
	    Predicate cmdPred = new Predicate(cmd);
	    method = "doAction";
	    log.debug("### ActionInterpreter - doManipulationAction");
	    String retValue = null;
	    if (checkMethod(method)) {
		try {
		   retValue = (String) callMethod(0,method,cmdPred,false);
		   //retValue = (String) callMethod(0,method,cmdPred,true);
		} catch (ADEException ace) {
            retValue = "failurestring";
		}
	    }
	    log.debug("### ActionInterpreter - doManipulationAction retValue = " + retValue);
	    action.setArgument("?hand",retValue);
	    hand = (String)action.getArgument("?hand");
	    log.debug("doManipulationAction hand(debug) = "  + hand);
	} else if (action.isA("closehand")) {
            String hand = (String)action.getArgument("?hand");
            //method = "closeHand";
            method = "doAction";
	    ArrayList<Symbol> cmdArgs = new ArrayList<Symbol>();
            cmdArgs.add(new Symbol(hand));
	    Predicate cmd = new Predicate("closeHand",cmdArgs);
	    log.debug("### ActionInterpreter - closeHand, hand = " + hand);
	    if (checkMethod(method)) {
	        log.debug("### ActionInterpreter - closeHand - 2");
                try {
                    callMethod(0,method, cmd, false);
	            log.debug("### ActionInterpreter - closeHand - 3");
                } catch (ADEException ace) {
                    log.error("Error closing "+hand+" hand");
                }
            } else {
                log.debug("Would close "+hand+" hand");
	    }
            /*
	    if (checkMethod(method, action)) {
                try {
                    callMethod(method, hand);
                } catch (ADEException ace) {
                    log.error("Error closing "+hand+" hand");
                                    }
            } else {
                log.debug("Would close "+hand+" hand");
            }*/
        } else if (action.isA("liftup")) {
            String hand = (String)action.getArgument("?hand");
            method = "doAction";
	    ArrayList<Symbol> cmdArgs = new ArrayList<Symbol>();
            cmdArgs.add(new Symbol(hand));
	    Predicate cmd = new Predicate("liftup",cmdArgs);
	    log.debug("### ActionInterpreter - liftup");
            if (checkMethod(method)) {
	    	log.debug("### ActionInterpreter - liftup - 2");
                try {
                    callMethod(0,method, cmd, false);
	    	    log.debug("### ActionInterpreter - liftup - 3");
                } catch (ADEException ace) {
                    log.error("Error lifting "+hand+" hand");

                }
            } else {
                log.debug("Would lift "+hand+" hand ");
            }
        } else if (action.isA("approach-visref")) {
            // approach-visref: move to a vision object
            Long key = (Long)action.getArgument("?key");
            String server = (String)action.getArgument("?server");
            if (server.equals("?server"))
                server = "VisionComponentImpl2";
            try {
                currentMotion = (Long)callMethod("approachVisRef", key, server);
                if (currentMotion < 0) {
                    log.debug("approach-visref failed!");
                    action.setExitStatus(false);
                }
            } catch (ADEException re) {
                log.error("approach-visref: Error sending to Motion: " + re);
                action.setExitStatus(false);
                currentMotion = -1L;
            }
            action.setArgument("?actionID", currentMotion);
        } else if (action.isA("approach-viscolor")) {
            // approach-viscolor: move to a vision color
            String color = (String)action.getArgument("?color");
            String server = (String)action.getArgument("?server");
            if (server.equals("?server"))
                server = "VisionComponentImpl2";
            try {
                currentMotion = (Long)callMethod("approachVisColor", color, server);
                if (currentMotion < 0) {
                    log.error("approach-viscolor failed!");
                    action.setExitStatus(false);
                }
            } catch (ADEException re) {
                log.error("approach-viscolor: Error sending to Motion: " + re);
                action.setExitStatus(false);
                currentMotion = -1L;
            }
            action.setArgument("?actionID", currentMotion);
        } else if (action.isA("startMoveTo") || action.isA("move-to")) {
            // startMoveTo: initiate move to a global location
            method = "moveTo";
            Double xdest = 0.0;
            Double ydest = 0.0;
            Long spexDest = -1L;
            String nameDest = null;
            ADEPercept o;
            boolean found = false;

            try {
                // are they x,y coordinates?
                xdest = action.getDouble("?xdest");
                ydest = action.getDouble("?ydest");
            } catch (NumberFormatException nfe) {
                // maybe a Spex location?
                String name = (String)action.getArgument("?xdest");
                spexDest = (Long)GoalManagerImpl.nameIdMap.get(name);
		log.debug("ActionInterpreter spexDest = " + spexDest);
                if (spexDest == null) {
		    log.debug("ActionInterpreter spexDest");
		    try{
			spexDest = Long.parseLong(name);
		    }catch(NumberFormatException nfe2){
			spexDest = Long.parseLong(name.substring(4));
		    }
		}
                /*if (spexDest == null && name.startsWith("spex")) {
                    spexDest = Long.parseLong(name.substring(4));
		}*/
                if (spexDest != null && checkMethod("connectedToWorld",action)) {
                    try {
                        // is it connected?
                        boolean conn = (((Boolean)(callMethod("connectedToWorld", spexDest))) &&
					(! (Boolean)(callMethod("largeScalePlace", spexDest))));
                        // could also check whether it's connected to the current location and if not send to the planner...
                        if (spexDest == ami.currentSpex) {
                            // HACK
                            double position[] = (double[])callMethod("getPoseEgo");
                            xdest = position[0];
                            ydest = position[1];
                            log.debug("startMoveTo " + xdest + "," + ydest);
                            spexDest = -1L;
                        } else if (conn) {
                            // then I can get coordinates and moveTo
                            Point2D.Double coord = (Point2D.Double)callMethod("getCoordinates", spexDest);
                            xdest = coord.getX();
                            ydest = coord.getY();
                            log.debug("startMoveTo " + xdest + "," + ydest);
                            spexDest = -1L;
                        }
                    } catch (ADEException ace) {
                        log.error("startMoveTo: Error sending to Spex: " + ace);
                        action.setExitStatus(false);
                        action.setArgument("?actionID", -1);
                        currentMotion = -1L;
                    }
                } else {
                    spexDest = -1L;
                    // must be a named location
                    nameDest = (String)action.getArgument("?xdest");
                    // is it a stored location?
                    if ((o = pastPercepts.get(nameDest.toUpperCase())) != null) {
                        // then I can get coordinates and moveTo
                        log.debug("going to " + nameDest + " x: " + o.sim_x + " px: " + o.sim_px);
                        xdest = o.sim_px;
                        ydest = o.sim_py;
                        nameDest = null;
                    }
                }
            }
            if (spexDest == -1L && nameDest == null) {
                // normal moveTo
                if (checkMethod(method, action)) {
                    found = true;
                    try {
                        currentMotion = (Long)callMethod(method, xdest, ydest);
                        if (currentMotion < 0) {
                            log.error("startMoveTo: moveTo failed!");
                            action.setArgument("?actionID", -1);
                            action.setExitStatus(false);
                        }
                    } catch (ADEException re) {
                        log.error("startMoveTo: Error sending to Motion: " + re);
                        action.setExitStatus(false);
                        action.setArgument("?actionID", -1);
                        currentMotion = -1L;
                    }
                    //log.debug("startMoveTo actionID: " + currentMotion);
                    action.setArgument("?actionID", currentMotion);
                }
            } else if (spexDest > 0) {
                // Spex destination, not connected, need to get path from Spex
                long spexScript = ami.spexScript(spexDest, this);
                if (spexScript < 0) {
                    action.setExitStatus(false);
                } else {
                    found = true;
                }
                action.setArgument("?actionID", spexScript);
                currentMotion = spexScript;
            } else {
                // named location, not stored locally
                if (checkMethod(method, action)) {
                    found = true;
                    try {
                        // PWS: I guess the "moveTo <name> is for Carmen  nav.
                        currentMotion = (Long)callMethod(method, nameDest);
                        if (currentMotion >= 0) {
                            log.debug("startMoveTo: got ID " + currentMotion + " for " + nameDest);
                            //ami.setPlannerTarget(target);
                            action.setArgument("?actionID", currentMotion);
                            return;
                        }
                    } catch (ADEException re) {
                        // This is normally because Motion is not Carmen
                        log.error("startMoveTo: exception sending " + nameDest + " to Motion: " + re);
                        action.setExitStatus(false);
                        action.setArgument("?actionID", -1);
                        currentMotion = -1L;
                        return;
                    }
                    log.error("startMoveTo: " + nameDest + " unknown!");
                    action.setExitStatus(false);
                    action.setArgument("?actionID", -1);
                    currentMotion = -1L;
                    return;
                }
            }
            if (!found) {
                log.error(myID + ": no Motion, can't " + action.getType());
                action.setExitStatus(false);
                action.setArgument("?actionID", -1);
            }
        } else if (action.isA("startMoveToRel")) {
            // startMoveToRel: initiate move to a relative location
            method = "moveToRel";
            Double xdest = 0.0;
            Double ydest = 0.0;

            xdest = action.getDouble("?xdest");
            ydest = action.getDouble("?ydest");

            if (checkMethod(method, action)) {
                try {
                    currentMotion = (Long)callMethod(method, xdest, ydest);
                    if (currentMotion < 0) {
                        log.error("startMoveToRel: moveToRel failed!");
                        action.setExitStatus(false);
                    }
                } catch (ADEException re) {
                    log.error("startMoveToRel: Error sending to Motion: " + re);
                    action.setExitStatus(false);
                    currentMotion = -1L;
                }
                action.setArgument("?actionID", currentMotion);
            } else {
                log.error(myID + ": no Motion, can't " + action.getType());
                action.setExitStatus(false);
                action.setArgument("?actionID", -1);
            }
        } else if (action.isA("updateMoveToRel")) {
            // updateMoveToRel: update destination of moveToRel
            method = "updateMoveToRel";
            Long aid = 0L;
            Double xdest = 0.0;
            Double ydest = 0.0;
            boolean retval = true;

            aid = action.getLong("?actionID");
            xdest = action.getDouble("?xdest");
            ydest = action.getDouble("?ydest");

            if (checkMethod(method, action)) {
                try {
                    retval = (Boolean)callMethod(method, aid, xdest, ydest);
                    if (!retval) {
                        // not really a failure, just not the right ID
                        //log.debug("updateMoveToRel: updateMoveToRel failed!");
                    }
                } catch (ADEException re) {
                    log.error("updateMoveToRel: Error sending to Motion: " + re);
                    retval = false;
                }
            } else {
                log.error(myID + ": no Motion, can't " + action.getType());
                retval = false;
            }
            action.setExitStatus(retval);
        } else if (action.isA("startTraverse")) {
            // startTraverse: initiate room/hallway traversal

            method = "traverse";
            if (checkMethod(method, action)) {
                try {
                    currentMotion = (Long)callMethod(method);
                    if (currentMotion < 0) {
                        log.error("startTraverse: traverse failed!");
                        action.setExitStatus(false);
                    }
                } catch (ADEException re) {
                    log.error("startTraverse: Error sending to Motion: " + re);
                    action.setExitStatus(false);
                    currentMotion = -1L;
                }
                action.setArgument("?actionID", currentMotion);
            } else {
                log.error(myID + ": no Motion, can't " + action.getType());
                action.setExitStatus(false);
                action.setArgument("?actionID", -1);
            }
        } else if (action.isA("startMoveRel")) {
            // startMoveRel: initiate move to a relative location
            method = "moveTo";
            String method2 = "getPoseGlobal";
            Double xdest = action.getDouble("?xdest");
            Double ydest = action.getDouble("?ydest");
            double[] position;
            double dist;

            if (checkMethod(method, action) && checkMethod(method2, action)) {
                try {
                    // Convert the relative to global
                    dist = Math.sqrt(xdest * xdest + ydest * ydest);
                    position = (double[])callMethod(method2);
                    xdest = position[0] + dist * Math.cos(position[2]);
                    ydest = position[1] + dist * Math.sin(position[2]);
                    // And go there
                    currentMotion = (Long)callMethod(method, xdest, ydest);
                    if (currentMotion < 0) {
                        log.error("startMoveRel: Motion moveTo failed!");
                        action.setExitStatus(false);
                    }
                } catch (ADEException re) {
                    log.error("startMoveRel: Error sending to Motion: " + re);
                    action.setExitStatus(false);
                    currentMotion = -1L;
                }
                action.setArgument("?actionID", currentMotion);
            } else {
                log.error(myID + ": no Motion/Position, can't " + action.getType());
                action.setExitStatus(false);
                action.setArgument("?actionID", -1);
            }
        } else if (action.isA("startMoveDist")) {
            // startMoveDist: initiate move a specified distance
            method = "moveDist";
            Double dist = action.getDouble("?dist");

            if (checkMethod(method, action)) {
                //log.debug("calling moveDist for " + dist);
                try {
                    currentMotion = (Long)callMethod(method, dist);
                    if (currentMotion < 0) {
                        log.error("startMoveDist: Motion moveDist failed!");
                        action.setExitStatus(false);
                    }
                } catch (ADEException re) {
                    log.error("startMoveDist: Error sending to Motion: " + re);
                    action.setExitStatus(false);
                    currentMotion = -1L;
                }
                action.setArgument("?actionID", currentMotion);
            } else {
                log.error(myID + ": no Motion, can't " + action.getType());
                action.setExitStatus(false);
                action.setArgument("?actionID", -1);
            }
        } else if (action.isA("startTimeMove")) {
            // startTimeTurn: initiate turn specified distance
            method = "timeMove";
            Double dist = action.getDouble("?dist");
            //log.debug("Got startTimeTurn " + Math.toDegrees(heading));

            if (checkMethod(method, action)) {
                try {
                    currentMotion = (Long)callMethod(method, dist);
                    if (currentMotion < 0) {
                        log.error("startTimeMove: Motion " + method + " failed!");
                        action.setExitStatus(false);
                    }
                } catch (ADEException re) {
                    log.error(action.getType() + ": Error sending to Motion: " + re);
                    action.setExitStatus(false);
                    currentMotion = -1L;
                }
                action.setArgument("?actionID", currentMotion);
            } else {
                log.error(myID + ": no Motion, can't " + action.getType());
                action.setExitStatus(false);
                action.setArgument("?actionID", -1);
            }
        } else if (action.isA("startTimeTurn")) {
            // startTimeTurn: initiate turn specified distance
            method = "timeTurn";
            Double heading = action.getDouble("?heading");
            //log.debug("Got startTimeTurn " + Math.toDegrees(heading));

            if (checkMethod(method, action)) {
                try {
                    currentMotion = (Long)callMethod(method, heading);
                    if (currentMotion < 0) {
                        log.error("startTimeTurn: Motion " + method + " failed!");
                        action.setExitStatus(false);
                    }
                } catch (ADEException re) {
                    log.error("startTimeTurn: Error sending to Motion: " + re);
                    action.setExitStatus(false);
                    currentMotion = -1L;
                }
                action.setArgument("?actionID", currentMotion);
            } else {
                log.error(myID + ": no Motion, can't " + action.getType());
                action.setExitStatus(false);
                action.setArgument("?actionID", -1);
            }
        } else if (action.isA("startTurnRel") || action.isA("turn")) {
            // startTurnRel: initiate turn specified distance
            method = "turnDist";
            String method2 = "getPoseGlobal";
            Double heading = 0.0;
            String target;
            ADEPercept o;

            if (checkMethod(method, action)) {
                try {
                    heading = action.getDouble("?heading");
                } catch (NumberFormatException nfe) {
                    target = action.getString("?heading");
                    // Make sure target is in memory
                    if (!checkMethod(method2, action)) {
                        log.error(myID + ": no Motion, can't " + action.getType());
                        action.setExitStatus(false);
                        action.setArgument("?actionID", -1);
                        return;
                    }
                    if ((o = pastPercepts.get(target.toUpperCase())) != null) {
                        try {
                            double[] position = (double[])callMethod(method2);
                            ami.current_x = position[0];
                            ami.current_y = position[1];
                            ami.current_t = position[2];
                        } catch (ADEException ace) {
                            log.error("startTurnRel: problem getting pose: " + ace);
                        }
                        heading = getHeadingTo(o.sim_x, o.sim_y);
                    } else {
                        log.error("turn: " + target + " unknown!");
                        action.setExitStatus(false);
                        currentMotion = -1L;
                        return;
                    }
                }

                try {
                    currentMotion = (Long)callMethod(method, heading);
                    if (currentMotion < 0) {
                        log.error("startTurnRel: Motion " + method + " failed!");
                        action.setExitStatus(false);
                    }
                } catch (ADEException re) {
                    log.error("startTurnRel: Error sending to Motion: " + re);
                    action.setExitStatus(false);
                    currentMotion = -1L;
                }
                action.setArgument("?actionID", currentMotion);
            } else {
                log.error(myID + ": no Motion, can't " + action.getType());
                action.setExitStatus(false);
                action.setArgument("?actionID", -1);
            }
        } else if (action.isA("checkMotion")) {
            method = "checkMotion";
            Long actionID = action.getLong("?actionID");
            ActionStatus status;

            if (checkMethod(method, action)) {
                //log.debug("Checking status of motion: " + actionID);
                try {
                    status = (ActionStatus)callMethod(method, actionID);
                    // PWS: This ignores the failure state...
                    if (status == ActionStatus.SUCCESS)
                        action.setExitStatus(true);
                    else
                        action.setExitStatus(false);
                } catch (Exception re) {
                    log.error("checkMotion: Error sending to Motion.");
                }
            } else {
                log.error(myID + ": no Motion, can't " + action.getType());
                action.setExitStatus(false);
            }
        } else if (action.isA("checkMotionDetail")) {
            method = "checkMotion";
            Long actionID = action.getLong("?actionID");
            ActionStatus status;

            if (checkMethod(method, action)) {
                try {
                    status = (ActionStatus)callMethod(method, actionID);
                    //log.debug("Status of motion: " + actionID + ": " + status);
                    if (status == ActionStatus.PROGRESS || status == ActionStatus.SUSPEND) {
                        action.setExitStatus(false);
                    } else {
                        if (status == ActionStatus.SUCCESS) {
                            action.setArgument("?actionStatus", "success");
                        } else if (status == ActionStatus.CANCEL) {
                            action.setArgument("?actionStatus", "cancel");
                        } else {
                            action.setArgument("?actionStatus", "failure");
                        }
                        action.setExitStatus(true);
                    }
                } catch (Exception re) {
                    log.error("checkMotion: Error sending to Motion.");
                }
            } else {
                log.error(myID + ": no Motion, can't " + action.getType());
                action.setArgument("?actionStatus", "unknown");
                action.setExitStatus(false);
                action.setArgument("?actionStatus", "failure");
            }
        } else if (action.isA("cancelMotion")) {
            method = "cancelMotion";
            Long actionID = action.getLong("?actionID");
            Boolean status;

            if (checkMethod(method, action)) {
                try {
                    status = (Boolean)callMethod(method, actionID);
                    // PWS: This ignores the failure state...
                    action.setExitStatus(true);
                } catch (ADEException ace) {
                    log.error("cancelMotion: Error sending to Motion: " + ace);
                    action.setExitStatus(false);
                }
            } else {
                log.error(myID + ": no Motion, can't " + action.getType());
                action.setExitStatus(true);
            }
        } else if (action.isA("suspendMotion")) {
            method = "suspendMotion";
            Long actionID = action.getLong("?actionID");
            Boolean status;

            if (checkMethod(method, action)) {
                try {
                    status = (Boolean)callMethod(method, actionID);
                    // PWS: This ignores the failure state...
                    action.setExitStatus(true);
                } catch (ADEException ace) {
                    log.error("suspendMotion: Error sending to Motion: " + ace);

                    action.setExitStatus(false);
                }
            } else {
                log.error(myID + ": no Motion, can't " + action.getType());
                action.setExitStatus(true);
            }
        } else if (action.isA("restoreMotion")) {
            method = "restoreMotion";
            Long actionID = action.getLong("?actionID");
            Boolean status;

            if (checkMethod(method, action)) {
                try {
                    status = (Boolean)callMethod(method, actionID);
                    action.setExitStatus(status);
                    if (status)
                        currentMotion = actionID;
                } catch (ADEException ace) {
                    log.error("restoreMotion: Error sending to Motion.");
                    action.setExitStatus(false);
                }
            } else {
                log.error(myID + ": no Motion, can't " + action.getType());
                action.setExitStatus(false);
            }
        } else if (action.isA("getCurrentMotion")) {
            method = "getCurrentMotion";
            Long actionID = action.getLong("?actionID");

            if (checkMethod(method, action)) {
                try {
                    actionID = (Long)callMethod(method);
                    action.setArgument("?actionID", actionID);
                } catch (ADEException ace) {
                    log.error("getCurrentMotion: Error sending to Motion.");
                    action.setExitStatus(false);
                }
            } else {
                log.error(myID + ": no Motion, can't " + action.getType());
                action.setExitStatus(false);
            }
        } else if (action.isA("dostop")) {
            // stop: stop
            Boolean quiet = action.getBoolean("?quiet");
            try {
                double[] vels = null;
                if (!quiet) {
                    vels = (double[])callMethod("getVels");
                }
                callMethod("stop");
                if (!quiet) {
                    if ((vels[0] != 0) || (vels[1] != 0)) {
                        String text;
                        double p0 = Math.random();
                        if (p0 < 0.75) {
                            text = "stopping";
                        } else {
                            text = "halting";
                        }
                        String mood = null;
                        ActionDBEntry dbe = ActionDBEntry.lookup("changemood");
                        if (dbe != null)
                            mood = (String)dbe.getValue();
                        if ((mood == null) || mood.equalsIgnoreCase("normal")) {
                            double p1 = Math.random();
                            if (p1 < 0.33) {
                                text = "okay, " + text;
                            } else if (p1 < 0.5) {
                                text = "all right, " + text;
                            }
                        } else if (mood.equalsIgnoreCase("halffrightened")) {
                            text = "okay";
                        }
                        sayText(text, action);
                    }
                }
                // have to set these, since not calling setVels
                curTV = curRV = 0;
            } catch (ADEReferenceException adere) {
                log.error("Can't do stop", adere);
                action.setExitStatus(false);
            } catch (ADEException re) {
                log.error("Error sending to motion server: " + re);
                action.setExitStatus(false);
            }
        } else if (action.isA("setTV")) {
            // setVels: set velocities
            double tv = action.getDouble("?tv");
            method = "setTV";
            if (!checkMethod(method, action)) {
                log.error("Can't do setTV: no Vel!");
                action.setExitStatus(false);
                return;
            }

            // NOT CHECKING FOR OBSTS...
            setTV(tv);
        } else if (action.isA("setRV")) {
            // setVels: set velocities
            double rv = action.getDouble("?rv");
            method = "setRV";
            if (!checkMethod(method, action)) {
                log.error("Can't do setRV: no Vel!");
                action.setExitStatus(false);
                return;
            }

            // NOT CHECKING FOR OBSTS...
            setRV(rv);
        } else if (action.isA("setVels")) {
            // setVels: set velocities
            double tv = action.getDouble("?tv");
            double rv = action.getDouble("?rv");
            method = "setVels";
            if (!checkMethod(method, action)) {
                log.error("Can't do setVels: no Vel!");
                action.setExitStatus(false);
                return;
            }

            // NOT CHECKING FOR OBSTS...
            setVels(tv, rv);
        } else if (action.isA("getVels")) {
            // getVels: get (nominal) velocities
            action.setArgument("?tv", curTV);
            action.setArgument("?rv", curRV);
        } else if (action.isA("startmove")) {
            // startmove: move straight ahead
            Boolean quiet = action.getBoolean("?quiet");
            boolean obst = false;

            // No extent given
            method = "setVels";
            if (!checkMethod(method, action)) {
                log.error("Can't do startmove: no Vel!");
                action.setExitStatus(false);
                return;
            }
            if (checkMethod("checkObstacle")) {
                try {
                    obst = (Boolean)callMethod("checkObstacle");
                } catch (ADEException ace) {
                    log.error("Can't check for obst", ace);
                }
            }
            if (obst) {
                if (!quiet) {
                    String text;
                    double p0 = Math.random();
                    if (p0 < 0.33) {
                        text = "There is something in the way.";
                    } else if (p0 < 0.67) {
                        text = "Something's in front of me.";
                    } else {
                        text = "I cant.";
                    }
                    sayText(text, action);
                }
                action.setExitStatus(false);
            } else if (setVels(defTV, 0)) {
                if (!quiet) {
                    String text;
                    double p0 = Math.random();
                    if (p0 < 0.5) {
                        text = "straight";
                    } else {
                        text = "forward";
                    }
                    p0 = Math.random();
                    double p1 = Math.random();
                    if (p1 >= 0.5)
                        p0 *= 0.67;
                    if (p0 < 0.33) {
                        text = "moving " + text;
                    } else if (p0 < 0.67) {
                        text = "going " + text;
                    } 
                    String mood = null;
                    ActionDBEntry dbe = ActionDBEntry.lookup("changemood");
                    if (dbe != null)
                        mood = (String)dbe.getValue();
                    if ((mood == null) || mood.equalsIgnoreCase("normal")) {
                        if (p1 < 0.33) {
                            text = "okay, " + text;
                        } else if (p1 < 0.5) {
                            text = "all right, " + text;
                        }
                    } else if (mood.equalsIgnoreCase("halffrightened")) {
                        text = "okay";
                    }
                    sayText(text, action);
                }
            } else {
                if (!quiet) {
                    sayText("there is something in the way", action);
                }
                action.setExitStatus(false);
            }
        } else if (action.isA("startmoveback")) {
            // startmoveback: back up
            Boolean quiet = action.getBoolean("?quiet");
            method = "setVels";
            if (!checkMethod(method, action)) {
                log.error("Can't do startmoveback: no Vel!");
                action.setExitStatus(false);
                return;
            }

            if (setVels(-defTV, 0)) {
                if (!quiet) {
                    sayText("Backing up", action);
                }
            } else {
                action.setExitStatus(false);
            }
        } else if (action.isA("startmoveleft")) {
            // startmoveleft: move off to the left
            Boolean quiet = action.getBoolean("?quiet");
            method = "setVels";
            if (checkMethod(method, action)) {
                boolean v = setVels(defTV, defRV);
                if (!quiet) {
                    String text = "left";
                    double p0 = Math.random();
                    double p1 = Math.random();
                    if (p1 >= 0.5)
                        p0 *= 0.67;
                    if (!v) {
                        text = "turning " + text;
                    } else if (p0 < 0.33) {
                        text = "moving " + text;
                    } else if (p0 < 0.67) {
                        text = "going " + text;
		    }
                    String mood = null;
                    ActionDBEntry dbe = ActionDBEntry.lookup("changemood");
                    if (dbe != null)
                        mood = (String)dbe.getValue();
                    if ((mood == null) || mood.equalsIgnoreCase("normal")) {
                        if (p1 < 0.33) {
                            text = "okay, " + text;
                        } else if (p1 < 0.5) {
                            text = "all right, " + text;
                        }
                    } else if (mood.equalsIgnoreCase("halffrightened")) {
                        text = "okay";
                    }
                    sayText(text, action);
                }
                action.setExitStatus(v);
            } else {
                if (!quiet) {
                    sayText("I cant.", action);
                }
                log.error("Can't do startmoveleft: no Vel!");
                action.setExitStatus(false);
            }
        } else if (action.isA("startmoveright")) {
            // startmoveright: move off to the right
            Boolean quiet = action.getBoolean("?quiet");
            method = "setVels";
            if (checkMethod(method, action)) {
                boolean v = setVels(defTV, -defRV);
                if (!quiet) {
                    String text = "right";
                    double p0 = Math.random();
                    double p1 = Math.random();
                    if (p1 >= 0.5)
                        p0 *= 0.67;
                    if (!v) {
                        text = "turning " + text;
                    } else if (p0 < 0.33) {
                        text = "moving " + text;
                    } else if (p0 < 0.67) {
                        text = "going " + text;
		    }
                    String mood = null;
                    ActionDBEntry dbe = ActionDBEntry.lookup("changemood");
                    if (dbe != null)
                        mood = (String)dbe.getValue();
                    if ((mood == null) || mood.equalsIgnoreCase("normal")) {
                        if (p1 < 0.33) {
                            text = "okay, " + text;
                        } else if (p1 < 0.5) {
                            text = "all right, " + text;
                        }
                    } else if (mood.equalsIgnoreCase("halffrightened")) {
                        text = "okay";
                    }
                    sayText(text, action);
                }
                action.setExitStatus(v);
            } else {
                if (!quiet) {
                    sayText("I cant.", action);
                }
                log.error("Can't do startmoveright: no Vel!");
                action.setExitStatus(false);
            }
        } else if (action.isA("startleft")) {
            // startleft: start turning left
            Boolean quiet = action.getBoolean("?quiet");
            method = "setVels";
            if (!checkMethod(method, action)) {
                log.error("Can't do startleft: no Vel!");
                action.setExitStatus(false);
                return;
            }

            // No extent given
            if (setRV(defRV)) {
                if (!quiet) {
                    String text = "left";
                    double p0 = Math.random();
                    double p1 = Math.random();
                    if (p1 >= 0.5)
                        p0 *= 0.67;
                    if (p0 < 0.33) {
                        text = "turning " + text;
                    } else if (p0 < 0.67) {
                        text = text + " turn";
                    }
                    String mood = null;
                    ActionDBEntry dbe = ActionDBEntry.lookup("changemood");
                    if (dbe != null)
                        mood = (String)dbe.getValue();
                    if ((mood == null) || mood.equalsIgnoreCase("normal")) {
                        if (p1 < 0.33) {
                            text = "okay, " + text;
                        } else if (p1 < 0.5) {
                            text = "all right, " + text;
                        }
                    } else if (mood.equalsIgnoreCase("halffrightened")) {
                        text = "okay";
                    }
                    sayText(text, action);
                }
            } else {
                if (!quiet) {
                    String text;
                    double p0 = Math.random();
                    if (p0 < 0.25) {
                        text = "I cant turn left.";
                    } else if (p0 < 0.75) {
                        text = "There's something over there.";
                    } else {
                        text = "I cant.";
                    }
                    sayText(text, action);
                }
                action.setExitStatus(false);
            }
        } else if (action.isA("startright")) {
            // startright: start turning right
            Boolean quiet = action.getBoolean("?quiet");
            method = "setVels";
            if (!checkMethod(method, action)) {
                log.error("Can't do startright: no Vel!");
                action.setExitStatus(false);
                return;
            }

            if (setRV(-defRV)) {
                if (!quiet) {
                    String text = "right";
                    double p0 = Math.random();
                    double p1 = Math.random();
                    if (p1 >= 0.5)
                        p0 *= 0.67;
                    if (p0 < 0.33) {
                        text = "turning " + text;
                    } else if (p0 < 0.67) {
                        text = text + " turn";
                    }
                    String mood = null;
                    ActionDBEntry dbe = ActionDBEntry.lookup("changemood");
                    if (dbe != null)
                        mood = (String)dbe.getValue();
                    if ((mood == null) || mood.equalsIgnoreCase("normal")) {
                        if (p1 < 0.33) {
                            text = "okay, " + text;
                        } else if (p1 < 0.5) {
                            text = "all right, " + text;
                        }
                    } else if (mood.equalsIgnoreCase("halffrightened")) {
                        text = "okay";
                    }
                    sayText(text, action);
                }
            } else {
                if (!quiet) {
                    String text;
                    double p0 = Math.random();
                    if (p0 < 0.25) {
                        text = "I cant turn right.";
                    } else if (p0 < 0.75) {
                        text = "There's something over there.";
                    } else {
                        text = "I cant.";
                    }
                    sayText(text, action);
                }
                action.setExitStatus(false);
            }
        } else if (action.isA("followWallRight")) {
            try {
                currentMotion = (Long)callMethod("followWall", true);
                if (currentMotion < 0) {
                    log.error("startMoveDist: MoPo followWall failed!");
                    action.setExitStatus(false);
                    currentMotion = -1L;
                }
            } catch (ADEException re) {
                log.error("followWall: Error sending to MoPo: " + re);
                action.setExitStatus(false);
                currentMotion = -1L;
            }
            action.setArgument("?actionID", currentMotion);
        } else if (action.isA("followWallLeft")) {
            try {
                currentMotion = (Long)callMethod("followWall", false);
                if (currentMotion < 0) {
                    log.error("startMoveDist: MoPo followWall failed!");
                    action.setExitStatus(false);
                    currentMotion = -1L;
                }
            } catch (ADEException re) {
                log.error("followWall: Error sending to MoPo: " + re);
                action.setExitStatus(false);
                currentMotion = -1L;
            }
            action.setArgument("?actionID", currentMotion);
        } else if (action.isA("safeRight")) {
            boolean[] safes;
            boolean s = false;
            try {
                safes = (boolean[])callMethod("getSafeSpaces");
                s = safes[0];
            } catch (ADEException ace) {
                log.error("safeRight: error checking lasers", ace);
            }
            action.setExitStatus(s);
        } else if (action.isA("safeFront")) {
            boolean[] safes;
            boolean s = false;
            try {
                safes = (boolean[])callMethod("getSafeSpaces");
                s = safes[1];
            } catch (ADEException ace) {
                log.error("safeFront: error checking lasers", ace);
            }
            action.setExitStatus(s);
        } else if (action.isA("safeLeft")) {
            boolean[] safes;
            boolean s = false;
            try {
                safes = (boolean[])callMethod("getSafeSpaces");
                s = safes[2];
            } catch (ADEException ace) {
                log.error("safeLeft: error checking lasers", ace);
            }
            action.setExitStatus(s);
        } else if (action.isA("gpsready")) {
            // gpsready: check to see if the GPS receiver is doing fixes
            method = "GPSReady";
            boolean ready = false;
            if (checkMethod(method, action)) {
                try {
                    ready = (Boolean)callMethod(method);
                } catch (ADEException ace) {
                    log.error(myID + ": error checking GPS: " + ace);
                }
            } else {
                // PWS: This is dicey--here for testing, but could cause
                // confusion later on...
                log.debug(myID + ": not using GPS, setting ready to TRUE");
                ready = true;
            }
            action.setExitStatus(ready);
        } else if (action.isA("addFieldPoint")) {
            double x = action.getDouble("?x");
            double y = action.getDouble("?y");
            double g = action.getDouble("?g");
            //log.debug("adding field point at " + x + " " + y + " " + g);
            try {
                callMethod("setFP", x, y, g);
            } catch (ADEException re) {
                log.error("Error sending to Field: " + re);
                action.setExitStatus(false);
            }
        } else if (action.isA("removeFieldPoint")) {
            int i = action.getInteger("?i");
            try {
                callMethod("delFP", i);
            } catch (ADEException re) {
                log.error("Error sending to Field: " + re);
                action.setExitStatus(false);
            }
        } else if (action.isA("moveFieldPoint")) {
            method = "delFP";
            Double a = action.getDouble("?startx");
            double step = action.getDouble("?step");
            double x, y, r = 212.13;
            int dir = action.getInteger("?dir");

            if (checkMethod(method, action)) {
                try {
                    callMethod(method, 0);
                } catch (ADEException re) {
                    log.error("Error removing Field point: " + re);
                }
            }
            x = r * Math.cos(a);
            y = r * Math.sin(a);
            log.warn("WARNING: using default field strength in moved field point");
            method = "setFP";
            if (checkMethod(method, action)) {
                try {
                    callMethod(method, x, y, 5.0);
                } catch (ADEException re) {
                    log.error("Error adding Field point: " + re);
                }
            }
            log.debug("Moved field point to [" + x + ", " + y + "]");
            //log.debug("New FS: " + (Double)callMethod("getFieldStrength"));
            a = a + (step * dir);
            if (a > 360)
                a -= 360;
            else if (a < 0)
                a += 360;
            action.setArgument("?startx", a);
        } else if (action.isA("fieldReading")) {
            // reading: get a reading from the field server
            method = "getFS";
            Double reading = 0.0;
            if (checkMethod(method, action)) {
                try {
                    reading = (Double)callMethod(method);
                    reading *= 100;
                } catch (ADEException re) {
                    log.error("Error sending to Field: " + re);
                    action.setExitStatus(false);
                }
            } else {
                log.error("Can't do reading: no Field!");
                action.setExitStatus(false);
            }
            action.setArgument("?fieldStrength", new Double(Math.round(reading)));
        } else if (action.isA("doTransmit")) {
            // doTransmit: get a reading from the field server and transmit if
            // it's high enough
            method = "getFS";
            Double reading = 0.0;
            Integer threshold = action.getInteger("?threshold");

            if (checkMethod(method, action)) {
                try {
                    reading = (Double)callMethod(method);
                    reading *= 100;
                } catch (ADEException re) {
                    log.error("Error sending to Field: " + re);
                }
            } else {
                log.error("Can't do reading: no Field!");
            }
            // This will be cast as an Integer by setArgument as needed
            action.setArgument("?fieldStrength", reading);
            action.setExitStatus(reading >= threshold);
        } else if (action.isA("getMotionTolerance")) {
            method = "getTolerance";
            double dist = 0.0;
            if (checkMethod(method, action)) {
                try {
                    dist = (Double)callMethod(method);
                } catch (ADEException re) {
                    log.error(method + ": Error sending to motion: " + re);
                    action.setExitStatus(false);
                }
            } else {
                log.error(method + " not defined without Motion");
            }
            action.setArgument("?dist", dist);
        } else if (action.isA("setMotionTolerance")) {
            method = "setTolerance";
            Double dist = action.getDouble("?dist");

            if (checkMethod(method, action)) {
                try {
                    callMethod("setTolerance", dist);
                } catch (ADEException re) {
                    log.error(method + ": Error sending to Motion: " + re);
                    action.setExitStatus(false);
                }
            } else {
                log.error(method + " not defined without Motion");
            }
        } else if (action.isA("getCritDist")) {
            method = "getCritDist";
            double dist = 0.0;
            if (checkMethod(method, action)) {
                try {
                    dist = (Double)callMethod(method);
                } catch (ADEException re) {
                    log.error("getCritDist: Error sending to LRF: " + re);
                    action.setExitStatus(false);
                }
            } else {
                log.error("getCritDist not defined without LRF");
            }
            action.setArgument("?dist", dist);
        } else if (action.isA("setCritDist")) {
            method = "setCritDist";
            Double dist = action.getDouble("?dist");

            if (checkMethod(method, action)) {
                try {
                    callMethod("setCritDist", dist);
                } catch (ADEException re) {
                    log.error("setCritDist: Error sending to LRF: " + re);
                    action.setExitStatus(false);
                }
            } else {
                log.error("setCritDist not defined without LRF");
            }
        } else if (action.isA("getMinOpen")) {
            method = "getMinOpen";
            double dist = 0.0;
            if (checkMethod(method, action)) {
                try {
                    dist = (Double)callMethod(method);
                } catch (ADEException re) {
                    log.error("getMinOpen: Error sending to LRF: " + re);
                    action.setExitStatus(false);
                }
            } else {
                log.error("getMinOpen not defined without LRF");
            }
            action.setArgument("?dist", dist);
        } else if (action.isA("setMinOpen")) {
            method = "setMinOpen";
            Double dist = action.getDouble("?dist");

            if (checkMethod(method, action)) {
                try {
                    callMethod(method, dist);
                } catch (ADEException re) {
                    log.error("setMinOpen: Error sending to LRF: " + re);
                    action.setExitStatus(false);
                }
            } else {
                log.error("setMinOpen not defined without LRF");
            }
        } else if (action.isA("resetOdometry")) {
            method = "resetOdometry";

            if (checkMethod(method, action)) {
                try {
                    callMethod(method);
                } catch (ADEException re) {
                    log.error(method + ": Error sending to base: " + re);
                    action.setExitStatus(false);
                }
            } else {
                log.error("Unable to " + method);
            }
        } else if (action.isA("atLocation")) {
            // atLocation: check whether current location is within some
            // epsilon of a provided location
            method = "getPoseGlobal";
            Double xdest = action.getDouble("?xdest");
            Double ydest = action.getDouble("?ydest");
            Double epsilon = action.getDouble("?epsilon");
            Double xcurr, ycurr, dist;

            if (checkMethod(method, action)) {
                try {
                    double[] position = (double[])callMethod(method);
                    xcurr = position[0];
                    ycurr = position[1];
                    dist = (xdest - xcurr) * (xdest - xcurr);
                    dist += (ydest - ycurr) * (ydest - ycurr);
                    dist = Math.sqrt(dist);
                    if (dist <= epsilon)
                        action.setExitStatus(true);
                    else
                        action.setExitStatus(false);
                } catch (ADEException re) {
                    log.error("atLocation: Error sending to Pos: " + re);
                    action.setExitStatus(false);
                }
            } else {
                log.error("atLocation: can't determine current location");
                action.setExitStatus(false);
            }
        } else if (action.isA("getLocation")) {
            // getLocation: get location
            method = "getPoseGlobal";

            if (checkMethod(method, action)) {
                Double xcurr, ycurr;
                try {
                    double[] position = (double[])callMethod(method);
                    xcurr = position[0];
                    ycurr = position[1];
                    action.setArgument("?xcoord", xcurr);
                    action.setArgument("?ycoord", ycurr);
                } catch (ADEException re) {
                    log.error("getLocation: Error sending to Pos: " + re);
                    action.setExitStatus(false);
                }
            } else {
                log.error("getLocation not defined without Pos");
            }
        } else if (action.isA("getStall")) {
            // getLocation: get location
            method = "getStall";
            boolean stalled = false;

            if (checkMethod(method, action)) {
                try {
                    stalled = (Boolean)callMethod(method);
                } catch (ADEException re) {
                    log.error("getStall: Error sending to Vel: " + re);
                }
                action.setExitStatus(stalled);
            } else {
                log.error("getStall not defined (need simulator reference?)");
            }
        } else if (action.isA("getHeading")) {
            // Get current heading
            method = "getPoseGlobal";

            if (checkMethod(method, action)) {
                Double tcurr;
                try {
                    double[] position = (double[])callMethod(method);
                    tcurr = position[2];
                    action.setArgument("?heading", tcurr);
                } catch (ADEException re) {
                    log.error("getHeading: Error sending to Pos: " + re);
                    action.setExitStatus(false);
                }
            } else {
                log.error("getHeading not defined without Pos");

            }
        } else if (action.isA("getHeadingFrom")) {
            // Global heading from (x1,y1) to (x2,y2)
            Double x1 = action.getDouble("?x1");
            Double y1 = action.getDouble("?y1");
            Double x2 = action.getDouble("?x2");
            Double y2 = action.getDouble("?y2");
            Double heading;

            // From Jim
            heading = Math.atan2((y2 - y1), (x2 - x1));

            action.setArgument("?heading", heading);
        } else if (action.isA("getHeadingFromRel")) {
            // Relative heading from (x1,y1) to (x2,y2) (i.e., how far I need
            // to turn to face (x2,y2) from (x1,y1)
            log.debug("Not sure about getHeadingFromRel, need to check for correctness...");
            Double x1 = action.getDouble("?x1");
            Double y1 = action.getDouble("?y1");
            Double x2 = action.getDouble("?x2");
            Double y2 = action.getDouble("?y2");
            Double heading = action.getDouble("?heading");

            Double xtmp = x2 - x1;
            Double ytmp = y2 - y1;
            // First rotate the destination so heading is 0
            Double xdif = Math.cos(-heading) * xtmp - Math.sin(-heading) * ytmp;
            Double ydif = Math.cos(-heading) * ytmp + Math.sin(-heading) * xtmp;
            // Then calculate the angle
            heading = Math.atan2(ydif, xdif);

            action.setArgument("?newHeading", heading);
        } else if (action.isA("getHeadingTo")) {
            // Relative heading from theta1 to theta2
            Double t1 = action.getDouble("?t1");
            Double t2 = action.getDouble("?t2");
            Double heading;

            heading = t2 - t1;
            while (heading >= 2 * Math.PI)
                heading -= 2 * Math.PI;
            while (heading < 0)
                heading += 2 * Math.PI;
            if (heading >= Math.PI)
                heading -= 2 * Math.PI;

            action.setArgument("?heading", heading);
        } else if (action.isA("getDistanceFrom")) {
            Double dist;
            Double x1 = action.getDouble("?x1");
            Double y1 = action.getDouble("?y1");
            Double x2 = action.getDouble("?x2");
            Double y2 = action.getDouble("?y2");

            dist = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
            //log.debug("getDistanceFrom: " + x1 + ", " + y1 + " to " + x2 + ", " + y2 + " is " + dist);
            action.setArgument("?dist", dist);
        } else if (action.isA("moveRightArm")) {
            method = "moveArm";
            boolean hand = true;
            try{
                int ep = action.getInteger("?elbow");
                int sy = action.getInteger("?shoulderYaw");
                int sp = action.getInteger("?shoulderPitch");
                int eps = action.getInteger("?elbowSpeed");
                int sys = action.getInteger("?shoulderYawSpeed");
                int sps = action.getInteger("?shoulderPitchSpeed");
                if (checkMethod(method, action)) {
                    try {
                        if (!(Boolean)callMethod(method, hand, ep, sy, sp, eps, sys, sps)) {
                            log.error("Action: moveRightArm failed!");
                            action.setExitStatus(false);
                        }
                    } catch (ADEException ace) {
                        log.error("Action: Error moving right arm: " + ace);
                        action.setExitStatus(false);
                    }
                }
                else{
                    log.error("Can't move left arm!");
                }
            } catch(NumberFormatException nfe) {

                int ep = action.getInteger("?elbow");
                int sy = action.getInteger("?shoulderYaw");
                int sp = action.getInteger("?shoulderPitch");
                method = "moveRightArm";
                if (checkMethod(method, action)) {
                    try {
                        if (!(Boolean)callMethod(method, ep, sy, sp)) {
                            log.error("Action: moveRightArm failed!");
                            action.setExitStatus(false);
                        }
                    } catch (ADEException ace2) {
                        log.error("Action: Error moving right arm: " + ace2);
                        action.setExitStatus(false);
                    }
                } else {
                    log.error("Can't move right arm!");
                }
            }
        } else if (action.isA("pointTo")) {
            // Takes vision-centric coords and converts to reddy-centric
            method = "Pnt2Obj";
            int t = action.getInteger("?theta");
            int p = action.getInteger("?phi");
            log.debug("Pointing to " + t + " " + p);
            if (checkMethod(method, action)) {
                try {
                    if (!(Boolean)callMethod(method, (double)t, (double)p)) {
                        log.error("Action: pointTo failed!");
                        action.setExitStatus(false);
                    }
                } catch (ADEException ace) {
                    log.error("Action: Error pointing: " + ace);
                    action.setExitStatus(false);
                }
            } else {
                log.error("Can't point! (would point to " + t + ", " + p + ")");
            }
        } else if (action.isA("moveLeftArm")) {
            method = "moveArm";
            boolean hand = false;
            try{
                int ep = action.getInteger("?elbow");
                int sy = action.getInteger("?shoulderYaw");
                int sp = action.getInteger("?shoulderPitch");
                int eps = action.getInteger("?elbowSpeed");
                int sys = action.getInteger("?shoulderYawSpeed");
                int sps = action.getInteger("?shoulderPitchSpeed");
                if (checkMethod(method, action)) {
                    try {
                        if (!(Boolean)callMethod(method, hand, ep, sy, sp, eps, sys, sps)) {
                            log.error("Action: moveLeftArm failed!");
                            action.setExitStatus(false);
                        }
                    } catch (ADEException ace) {
                        log.error("Action: Error moving left arm: " + ace);
                        action.setExitStatus(false);
                    }
                }
                else{
                    log.error("Can't move left arm!");
                }
            } catch(NumberFormatException ace) {
                method = "moveLeftArm";
                int ep = action.getInteger("?elbow");
                int sy = action.getInteger("?shoulderYaw");
                int sp = action.getInteger("?shoulderPitch");

                if (checkMethod(method, action)) {
                    try {
                        if (!(Boolean)callMethod(method, ep, sy, sp)) {
                            log.error("Action: moveLeftArm failed!");
                            action.setExitStatus(false);
                        }
                    } catch (ADEException ace1) {
                        log.error("Action: Error moving left arm: " + ace1);
                        action.setExitStatus(false);
                    }
                } else {
                    log.error("Can't move left arm!");
                }
            }
        } else if (action.isA("moveEyes")) {
            method = "moveEyes";
            int left = action.getInteger("?left");
            int right = action.getInteger("?right");
            int pitch = action.getInteger("?pitch");
            if (checkMethod(method, action)) {
                try {
                    if (!(Boolean)callMethod(method, left, right, pitch)) {
                        log.error("Action: moveEyes failed!");
                        action.setExitStatus(false);
                    }
                } catch (ADEException ace) {
                    log.error("Action: Error moving eyes: " + ace);
                    action.setExitStatus(false);
                }
            } else {
                log.error("Can't move eyes!");
            }
        } else if (action.isA("moveEyeBrows")) {
            method = "moveEyeBrows";
            int left = action.getInteger("?left");
            int right = action.getInteger("?right");
            if (checkMethod(method, action)) {
                try {
                    if (!(Boolean)callMethod(method, left, right)) {
                        log.error("Action: moveEyeBrows failed!");
                        action.setExitStatus(false);
                    }
                } catch (ADEException ace) {
                    log.error("Action: Error moving eye brows: " + ace);
                    action.setExitStatus(false);
                }
            } else {
                log.error("Can't move eye brows!");
            }
        } else if (action.isA("eyeFlickers")) {
            method = "eyeFlickers";
            int cntPitch = action.getInteger("?cntPitch");
            int cntTilt = action.getInteger("?cntTilt");
            int RestTimeStep = action.getInteger("?RestTimeStep");
            int NumSteps = action.getInteger("?NumSteps");
            long Time = 0;
            if (checkMethod(method, action)) {
                try {
                    Time = (Long)callMethod(method, cntPitch,cntTilt,RestTimeStep,NumSteps);
                    if (Time == -1) {
                        log.error("Action: eyeFlickers failed!");
                        action.setExitStatus(false);
                    }
                } catch (ADEException ace) {
                    log.error("Action: Error performing eyeFlickers: " + ace);
                    action.setExitStatus(false);
                }
            } else {
                log.error("Can't do eyeFlickers!");
            }
            action.setArgument("?Time", Time);
        } else if (action.isA("moveMouth")) {
            method = "moveMouth";
            int ll = action.getInteger("?lLeft");
            int ul = action.getInteger("?uLeft");
            int lr = action.getInteger("?lRight");
            int ur = action.getInteger("?uRight");
            if (checkMethod(method, action)) {
                try {
                    if (!(Boolean)callMethod(method, ll, ul, lr, ur)) {
                        log.error("Action: moveMouth failed!");
                        action.setExitStatus(false);
                    }
                } catch (ADEException ace) {
                    log.error("Action: Error moving mouth: " + ace);
                    action.setExitStatus(false);
                }
            } else {
                log.error("Can't move mouth!");
            }
        } else if (action.isA("moveHead")) {
            method = "moveHead";
            int pitch = action.getInteger("?pitch");
            int yaw = action.getInteger("?yaw");
            if (checkMethod(method, action)) {
                try {
                    if (!(Boolean)callMethod(method, pitch, yaw)) {
                        log.error("Action: moveHead failed!");
                        action.setExitStatus(false);
                    }
                } catch (ADEException ace) {
                    log.error("Action: Error moving head: " + ace);
                    action.setExitStatus(false);
                }
            } else {
                log.error("Can't move head!");
            }
        } else if (action.isA("Scowl")) {
            method = "Scowl";
            if (checkMethod(method, action)) {
                try {
                    if (!(Boolean)callMethod(method)) {
                        log.error("Action: scowl failed!");
                        action.setExitStatus(false);
                    }
                } catch (ADEException ace) {
                    log.error("Action: Error scowling: " + ace);
                    action.setExitStatus(false);
                }
            } else {
                log.error("Can't scowl!");
            }
        } else if (action.isA("openMouth")) {
            method = "open";
            if (checkMethod(method, action)) {
                try {
                    /*
                       if (!(Boolean)callMethod(method)) {
                       log.error("Action: change expression failed!");
                       action.setExitStatus(false);
                       }
                       */
                    callMethod(method);
                } catch (ADEException ace) {
                    log.error("Action: Error changing expression: " + ace);
                    action.setExitStatus(false);
                }
            } else {
                log.error("Can't change expression!");
            }
        } else if (action.isA("closeMouth")) {
            method = "close";
            if (checkMethod(method, action)) {
                try {
                    /*
                       if (!(Boolean)callMethod(method)) {
                       log.error("Action: change expression failed!");
                       action.setExitStatus(false);
                       }
                       */
                    callMethod(method);
                } catch (ADEException ace) {
                    log.error("Action: Error changing expression: " + ace);
                    action.setExitStatus(false);
                }
            } else {
                log.error("Can't change expression!");
            }
        } else if (action.isA("Smile")) {
            method = "Smile";
            if (checkMethod(method, action)) {
                try {
                    if (!(Boolean)callMethod(method)) {
                        log.error("Action: change expression failed!");
                        action.setExitStatus(false);
                    }
                } catch (ADEException ace) {
                    log.error("Action: Error changing expression: " + ace);
                    action.setExitStatus(false);
                }
            } else {
                log.error("Can't change expression!");
            }
        } else if (action.isA("Frown")) {
            method = "Frown";
            if (checkMethod(method, action)) {
                try {
                    if (!(Boolean)callMethod(method)) {
                        log.error("Action: change expression failed!");
                        action.setExitStatus(false);
                    }
                } catch (ADEException ace) {
                    log.error("Action: Error changing expression: " + ace);
                    action.setExitStatus(false);
                }
            } else {
                log.error("Can't change expression!");
            }
        } else if (action.isA("neutralFace")) {
            method = "neutralFace";
            if (checkMethod(method, action)) {
                try {
                    callMethod(0, method);
                } catch (ADEException ace) {
                    log.error("Action: Error changing expression: " + ace);
                    action.setExitStatus(false);
                }
            } else {
                log.error("Can't change expression!");
            }
        } else if (action.isA("sadFace")) {
            method = "sadFace";
            if (checkMethod(method, action)) {
                try {
                    callMethod(0, method);
                } catch (ADEException ace) {
                    log.error("Action: Error changing expression: " + ace);
                    action.setExitStatus(false);
                }
            } else {
                log.error("Can't change expression!");
            }
        } else if (action.isA("shockedFace")) {
            method = "shockedFace";
            if (checkMethod(method, action)) {
                try {
                    callMethod(0, method, 1.0f);
                } catch (ADEException ace) {
                    log.error("Action: Error changing expression: " + ace);
                    action.setExitStatus(false);
                }
            } else {
                log.error("Can't change expression!");
            }
        } else if (action.isA("nodAgree")) {
            method = "nodAgree";
            if (checkMethod(method, action)) {
                try {
                    log.debug(myID + ": before " + method);
                    callMethod(method);
                    log.debug(myID + ": after " + method);
                } catch (ADEException ace) {
                    log.error("Action: Error changing expression", ace);
                    action.setExitStatus(false);
                }
            } else {
                log.error("Can't change expression!");
            }
        } else if (action.isA("nodUncertain")) {
            method = "nodUncertain";
            if (checkMethod(method, action)) {
                try {
                    callMethod(method);
                } catch (ADEException ace) {
                    log.error("Action: Error changing expression: " + ace);
                    action.setExitStatus(false);
                }
            } else {
                log.error("Can't change expression!");
            }
        } else if (action.isA("listenPose")) {
            method = "listen";
            if (checkMethod(method, action)) {
                try {
                    log.debug(myID + ": before " + method);
                    callMethod(0, method);
                    log.debug(myID + ": after " + method);
                } catch (ADEException ace) {
                    log.error("Action: Error changing expression: " + ace);
                    action.setExitStatus(false);
                }
            } else {
                log.error("Can't change expression!");
            }
        } else if (action.isA("focus")) {
            method = "focus";
            if (checkMethod(method, action)) {
                try {
                    log.debug(myID + ": before " + method);
                    callMethod(method);
                    log.debug(myID + ": after " + method);
                } catch (ADEException ace) {
                    log.error("Action: Error changing expression: " + ace);
                    action.setExitStatus(false);
                }
            } else {
                log.error("Can't change expression!");
            }
        } else if (action.isA("headSway")) {
            method = "headSway";
            if (checkMethod(method, action)) {
                try {
                    callMethod(method);
                } catch (ADEException ace) {
                    log.error("headSway: error: " + ace);
                }
            } else {
                //log.error("No way to check conversation, assuming false");
            }
        } else if (action.isA("blink")) {
            method = "blink";
            if (checkMethod(method, action)) {
                try {
                    callMethod(method);
                } catch (ADEException ace) {
                    log.error(method + ": error: " + ace);
                }
            } else {
                //log.error("No way to check conversation, assuming false");
            }
        } else if (action.isA("setPose")) {
            method = "setPose";
            String pose = (String)action.getArgument("?pose");
            if (checkMethod(method, action)) {
                try {
                    callMethod(method, pose);
                } catch (ADEException ace) {
                    log.error(method + ": error: " + ace);
                }
            } else {
                //log.error("No way to check conversation, assuming false");
            }
        } else {
            log.error("Unrecognized motion command: " + action.getType());
        }
    }

    /**
     * Execute a vision primitive
     * @param action the action specification
     */
    void doVisionPrimitive(ActionDBEntry action) {
        log.debug(format("[doVisionPrimitive] Executing action %s", action));
        String viewer = (String)action.getArgument("?viewer");
        if ((viewer == null) ||
            ((!viewer.equalsIgnoreCase(agentname)) &&
             (!viewer.equalsIgnoreCase("Rudy")) &&
             (!viewer.equalsIgnoreCase("Mary")) &&
             (!viewer.equalsIgnoreCase("cbot")) &&
             (!viewer.equalsIgnoreCase("cramer")) &&
             (!viewer.equalsIgnoreCase("icarus")) &&
             (!viewer.equalsIgnoreCase("actr")) &&
             (!viewer.equalsIgnoreCase("robot")) &&
             (!viewer.equalsIgnoreCase("self")) &&
             (!viewer.equalsIgnoreCase("me")))) {
            log.error("Unknown viewer: " + viewer);
            return;
        }

        // Right now, all the confs are constant; eventually, AIs will
        // need to say what conf they require...
        double conf = 0.5;
        //int i;
        String method = null;

        /* Vision API not fully implemented yet in Vision. */
        if (action.isA("getTypeIds")) {
            // getTypeIds: get the types of all objects in STM
            method = "getTypeIds";
            ArrayList<Long> types = new ArrayList<Long>();
            if (checkMethod(method, action)) {
                ArrayList<Object> tmpTypes;
                try {
                    tmpTypes = (ArrayList<Object>)callMethod(method, conf);
                    for (Object o : tmpTypes)
                        types.add((Long)o);
                    action.setArgument("?STMTypes", types);
                } catch (ADEException ere) {
                    log.error("Error sending to Vision: " + ere);
                }
            } else {
                log.error("Can't " + action.getType() + "!");
            }
        } else if (action.isA("getTypeId")) {
            // getTypeId: get the type for the given description
            method = "getTypeId";
            Predicate desc = createPredicate((String)action.getArgument("?desc"));
            if (checkMethod(method, action)) {
                Long vtype = -1L;
                try {
                    vtype = (Long)callMethod(method, desc);
                    action.setArgument("?vtype", vtype);
                } catch (ADEException ere) {
                    log.error("Error sending to Vision: " + ere);
                }
            } else {
                log.error("Can't " + action.getType() + "!");
            }
        } else if (action.isA("getAllTokenIds")) {
            // getAllTokenIds: get all objects in STM
            method = "getTokenIds";
            ArrayList<Long> keys = new ArrayList<Long>();
            if (checkMethod(method, action)) {
                List<Object> tmpKeys;
                try {
                    tmpKeys = (List<Object>)callMethod(method, conf);
                    for (Object o : tmpKeys)
                        keys.add((Long)o);
                    action.setArgument("?STMKeys", keys);
                } catch (ADEException ere) {
                    log.error("Error sending to Vision: " + ere);
                }
            } else {
                log.error("Can't " + action.getType() + "!");
            }
        } else if (action.isA("getTokenIdByTypeId")) {
            // getTokenIdByTypeId: get one <type> object in STM
            method = "getTokenIds";
            Long typeId = action.getLong("?vType");
            Long tokenId = -1L;
            if (checkMethod(method, action)) {
                ArrayList<Long> tmpKeys = new ArrayList<Long>();
                try {
                    tmpKeys = (ArrayList)callMethod(0, method, typeId, conf);
                } catch (ADEException ere) {
                    log.error("Error sending to Vision: " + ere);

                }
                if (tmpKeys.size() > 0) {
                    try {
                        MemoryObject r = (MemoryObject)callMethod("getToken",tmpKeys.get(0), conf);
                        //log.debug("Found a key for " + type);
                        if (r == null) {
                            action.setExitStatus(false);
                        } else {
                            tokenId = tmpKeys.get(0);
                        }
                    } catch (ADEException ace) {
                        log.error("Error sending to Vision: " + ace);
                    }
                }
            } else {
                log.error("Can't " + action.getType() + "!");
                action.setExitStatus(false);
            }
            action.setArgument("?STMKey", "" + tokenId);
        } else if (action.isA("getAllTokenIdsByTypeId")) {
            // getAllTokenIdsByTypeId: get all <type> objects in STM
            method = "getTokenIds";
            Long type = action.getLong("?vType");
            ArrayList<Long> keys = new ArrayList<Long>();
            if (checkMethod(method, action)) {
                List<Long> tmpKeys;
                try {
                    tmpKeys = (List<Long>)callMethod(method, type, conf);
                    for (Long o : tmpKeys)
                        keys.add(o);
                    action.setArgument("?STMKeys", keys);
                } catch (ADEException ere) {
                    log.error("Error sending to Vision: " + ere);
                }
            } else {
                log.error("Can't " + action.getType() + "!");
            }
        } else if (action.isA("find")) {
            Predicate type = (Predicate)action.getArgument("?type");
            // System.out.println(viewer+" ATTEMPTING TO FIND "+type);
        } else if (action.isA("getTokenIdByColor")) {
            // getTokenIdByColor: get one <color> object in STM
            method = "getTokenIds";
            String color = (String)action.getArgument("?vColor");
            Long tokenId = -1L;
            if (checkMethod(method, action)) {
                ArrayList<Long> tmpKeys = new ArrayList<Long>();
                Long typeId = -1L;
                try {
                    ArrayList<Predicate> descriptors = new ArrayList<Predicate>();
                    descriptors.add(utilities.Util.createPredicate("type(X, blob)"));
                    descriptors.add(utilities.Util.createPredicate("color(X, " + color + ")"));
                    log.trace(format("[getTokenIds] getTypeId %s", descriptors));
                    typeId = (Long)callMethod("getTypeId", descriptors);
                    tmpKeys = (ArrayList)callMethod(method, typeId, conf);
                } catch (ADEException ere) {
                    log.error("Error sending to Vision: " + ere);
                }
                if (tmpKeys.size() > 0) {
                    try {
                        MemoryObject r = (MemoryObject)callMethod("getToken",tmpKeys.get(0), conf);
                        //log.debug("Found a key for " + type);
                        if (r == null) {
                            action.setExitStatus(false);
                        } else {
                            tokenId = tmpKeys.get(0);
                        }
                    } catch (ADEException ace) {
                        log.error("Error sending to Vision: " + ace);
                    }
                } else {
                    //log.error("Found no key for " + type);
                    action.setExitStatus(false);
                }
            } else {
                log.error("Can't " + action.getType() + "!");
                action.setExitStatus(false);
            }
            log.trace(format("[getTokenIds] Found ID: %d", tokenId));
            action.setArgument("?STMKey", tokenId);
        } else if (action.isA("getAllTokenIdsByColor")) {
            // getAllTokenIdsByColor: get all <color> objects in STM
            method = "getTokenIds";
            ArrayList<Long> keys = new ArrayList<Long>();
            String color = (String)action.getArgument("?vColor");
            if (checkMethod(method, action)) {
                ArrayList<Long> tmpKeys = new ArrayList<Long>();
                Long typeId = -1L;
                try {
                    ArrayList<Predicate> descriptors = new ArrayList<Predicate>();
                    descriptors.add(utilities.Util.createPredicate("type(X, blob)"));
                    descriptors.add(utilities.Util.createPredicate("color(X, " + color + ")"));
                    typeId = (Long)callMethod("getTypeId", descriptors);
                    //log.debug("getAllTokenIdsByColor " + type + " got " + tmpKeys.size());
                    tmpKeys = (ArrayList<Long>)callMethod(method, typeId, conf);
                    if (tmpKeys.size() > 0) {
                        for (Long o : tmpKeys)
                            keys.add(o);
                    }
                    action.setArgument("?STMKeys", keys);
                } catch (ADEException ere) {
                    log.error("Error sending to Vision: " + ere);
                }
            } else {
                log.error("Can't " + action.getType() + "!");
                action.setExitStatus(false);
            }
        } else if (action.isA("confirmToken")) {
            // confirmToken: see if <key> is in visual short-term memory
            method = "confirmToken";
            Long key = action.getLong("?STMKey");
            boolean present = false;
            if (checkMethod(method, action)) {
                try {
                    present = (Boolean)callMethod(method, key);
                } catch (ADEException ere) {
                    log.error("Error sending to Vision: " + ere);
                }
            } else {
                log.error("Can't " + action.getType() + "!");
            }
            action.setExitStatus(present);
        } else if (action.isA("confirmType")) {
            // confirmType: see if <key> is a known memoryobject type
            method = "getAvailableTypeIds";
            Long key = action.getLong("?vType");
            boolean present = false;
            if (checkMethod(method, action)) {
                List<Long> types;
                try {
                    types = (List<Long>)callMethod(method);
                    if (types != null) {
                        for (Long t : types) {
                            // Setting present here for setExitStatus below
                            if (t == key) {
                                present = true;
                                break;
                            }
                        }
                    }
                } catch (ADEException ere) {
                    log.error("Error sending to Vision: " + ere);
                }
            } else {
                log.error("Can't " + action.getType() + "!");
            }
            action.setExitStatus(present);
        } else if (action.isA("simple-find")){
            log.trace("ISF!!");
            String belief = (String)action.getArgument("?bel");
            String type =   (String)ami.getTypeInfo().get(belief);
            Long vtype = -1L;
            //need to get type.
            try {
                log.debug("calling vision getTypeId.");
                vtype = (Long) callMethod("getTypeId", createPredicate("type(?X:,"+type+")"));
                log.debug("got vision getTypeId: " + vtype);
            } catch (ADEException ere) {
                log.error("Error sending to Vision: " + ere);
            }                        
            //if null, make nlg request to say that it doesn't know about that shit.
            if(vtype==-1L){
                Predicate q = createPredicate("itk(self,needsVisionType("+type+"))");
                log.debug("Sending NLG request for: " + q);
                try {
		    //HACK: "What does medkit looks like?" fix
                    //callMethod("submitNLGRequest", new NLPacket(new Utterance(q)));
		    Utterance utterance = new Utterance(q);
		    ArrayList<String> utteranceHack = new ArrayList();
		    utteranceHack.add("What does a ");
		    utteranceHack.add(type);
		    utteranceHack.add("look like?");
		    utterance.words = utteranceHack;
		    callMethod("submitNLGRequest", new NLPacket(utterance));
                } catch (ADEException ace) {
                    log.error(String.format("Trying to submit NLG request: %s.", q.toString()), ace);
                }
                ami.goals.get(getGoalID()).terminate(ActionStatus.FAIL);
            }else{
                try {
		    Sleep(6000);//HACK
		    log.debug("Getting tokenIds from Vision.");
                    List<Long> tmpKeys = (List<Long>)callMethod(0, "getTokenIds", vtype, 0.0);
		    log.debug(String.format("Got %s tokenIds from Vision (-1 means null).", (tmpKeys == null) ? -1 : tmpKeys.size()));
                    if (tmpKeys != null && tmpKeys.size() > 0){
                        ami.dialogueAck("Yes", Type.REPLYY);
                            //ami.sayText("Yes.");
                    } else {
                        ami.dialogueAck("No", Type.REPLYN);
                            //ami.sayText("No.");
                    }
                    
                } catch (ADEException ere) {
                    log.error("Error sending to Vision.", ere);
                }                                        
                ami.goals.get(getGoalID()).terminate(ActionStatus.SUCCESS);
            }            
            //if not null, need to get tokens.
            //if tokens null, make nlg request to say that it can't find nada
            //if not null, say you can see it or whatever.
        } else if (action.isA("simple-report-location")){
            log.trace("ISF2!!");
            String belief = (String)action.getArgument("?bel");
            String type =   (String)ami.getTypeInfo().get(belief);
            Predicate typePred = createPredicate("type(?X:,"+type+")");
            Long vtype = -1L;
            //need to get type.
            try {
                vtype = (Long)callMethod("getTypeId", typePred);
                
            } catch (ADEException ere) {
                log.error("Error sending to Vision: " + ere);
            }                        
            //if null, make nlg request to say that it doesn't know about that shit.
            if(vtype==-1L){
                Predicate q = createPredicate("itk(self,needsVisionType("+type+"))");
                log.debug("Sending NLG request for: " + q);
                try {
                    callMethod("submitNLGRequest", new NLPacket(new Utterance(q)));
                } catch (ADEException ace) {
                    log.error("Trying to submit NLG request.", ace);
                }
                ami.goals.get(getGoalID()).terminate(ActionStatus.FAIL);
            }else{
                try {
		    log.debug("Getting tokenIds from Vision.");
                    List<Long> tmpKeys = (List<Long>)callMethod("getTokenIds", vtype, 0.0);
		    log.debug(String.format("Got %s tokenIds from Vision (-1 means null).", (tmpKeys == null) ? -1 : tmpKeys.size()));
                    if (tmpKeys != null && tmpKeys.size() > 0){
                        Predicate pointToAction = createPredicate("pointTo("+type+")");
                        //HACKy try-catch: call goes through but still exception...no idea why
                        try {
                          callMethod(0, "doAction", pointToAction, false);
                        } catch (ADEException ere) {
                          log.error("doAction: " + pointToAction.toString(), ere);
                        }
                        ami.dialogueAck("It is over there", Type.REPLY);
                    }else{
                        ami.dialogueAck("I do not see it any more", Type.REPLY);
                    }
                    
                } catch (ADEException ere) {
                    log.error("simple-report-location.", ere);
                }                                        
                ami.goals.get(getGoalID()).terminate(ActionStatus.SUCCESS);
            }            
            //if not null, need to get tokens.
            //if tokens null, make nlg request to say that it can't find nada
            //if not null, say you can see it or whatever.
        } else if (action.isA("howManyOfType")) {
            // howManyOfType: see how many <key> are in visual short-term memory
            method = "getTokenIds";
            Long type = action.getLong("?vType");
            Integer size = 0;
            if (checkMethod(method, action)) {
                List<Long> tmpKeys;
                try {
                    tmpKeys = (List<Long>)callMethod(method,
                            type, conf);
                    if (tmpKeys != null)
                        size = tmpKeys.size();
                } catch (ADEException ere) {
                    log.error("Error sending to Vision: " + ere);
                }
            } else {
                log.error("Can't " + action.getType() + "!");
            }
            action.setArgument("?count", size);
        } else if (action.isA("getToken")) {
            method = "getToken";
            Long key = action.getLong("?vKey");
            MemoryObject token = null;
            if (checkMethod(method, action)) {
                try {
                    token = (MemoryObject)callMethod(method, key, conf);
                } catch (ADEException ere) {
                    log.error("Error sending to Vision: " + ere);
                }
            } else {
                log.error("Can't getToken!");
            }
            action.setExitStatus(token != null);
            action.setArgument("?vToken", token);
        } else if (action.isA("getTokenPanTilt")) {
            method = "getToken";
            boolean status = false;
            Long key = action.getLong("?vKey");
            MemoryObject vt;
            if (checkMethod(method, action)) {
                try {
                    vt = (MemoryObject)callMethod(method, key, conf);
                    if (vt != null) {
                        action.setArgument("?pan", vt.getPan());
                        action.setArgument("?tilt", vt.getTilt());
                        status = true;
                    }
                } catch (ADEException ace) {
                    log.error("Error sending to Vision: " + ace);
                }
            } else {
                log.error("Can't getToken!");
            }
            action.setExitStatus(status);
        } else if (action.isA("getTokenArea")) {
            method = "getToken";
            boolean status = false;
            Long key = action.getLong("?vKey");
            MemoryObject vt;
            if (checkMethod(method, action)) {
                try {
                    vt = (MemoryObject)callMethod(method, key, conf);
                    if (vt != null) {
                        action.setArgument("?area", vt.getArea());
                        status = true;
                    }
                } catch (ADEException ace) {
                    log.error("Error sending to Vision: " + ace);
                }
            } else {
                log.error("Can't getToken!");
            }
            action.setExitStatus(status);
        } else if (action.isA("getTokenName")) {
            boolean status = false;
            ADEPercept p = null;
            Object var = action.getArgument("?vKey");
            if (var instanceof ADEPercept) {
                p = (ADEPercept)action.getArgument("?vKey");
                action.setArgument("?name", p.name);
                status = true;
            //} else if ((p = pastPercepts.get(((String)var).toUpperCase())) != null) {
            //    action.setArgument("?name", p.name);
            //    status = true;
            } else {
                // key is the name...
                Long key = action.getLong("?vKey");
                action.setArgument("?name", key.toString());
                status = true;
            }
            action.setExitStatus(status);
        } else if (action.isA("getTokenColor")) {
            method = "getToken";
            boolean status = false;
            ADEPercept p = null;
            Object var = action.getArgument("?vKey");
            if (var instanceof ADEPercept) {
                p = (ADEPercept)action.getArgument("?vKey");
                action.setArgument("?color", p.color);
                status = true;
            //} else if ((p = pastPercepts.get(((String)var).toUpperCase())) != null) {
            //    action.setArgument("?color", p.color);
            //    status = true;
            } else {
                Long key = action.getLong("?vKey");
                MemoryObject vt;
                log.debug("Checking color of: " + key);
                if (checkMethod(method, action)) {
                    try {
                        vt = (MemoryObject)callMethod(method, key, conf);
                        if (vt != null) {
                            log.debug("Setting ?color to " + vt.getColorLabel());
                            action.setArgument("?color", vt.getColorLabel());
                            status = true;
                        } else {
                            log.debug("Vision did not have " + key + "!");
                            action.setArgument("?color", "Unknown");
                        }
                    } catch (ADEException ace) {
                        log.error("Error sending to Vision: " + ace);
                    }
                } else {
                    log.error("Can't getToken!");
                }
            }
            action.setExitStatus(status);
        } else if (action.isA("getTokenDistance")) {
            method = "getToken";
            boolean status = false;
            ADEPercept p = null;
            Object var = action.getArgument("?vKey");
            if (var instanceof ADEPercept) {
                p = (ADEPercept)action.getArgument("?vKey");
                action.setArgument("?dist", p.distance);
                status = true;
            //} else if ((p = pastPercepts.get((var + "").toUpperCase())) != null) {
            //    action.setArgument("?dist", p.distance);
            //    status = true;
            } else {
                // key is the token id...
                Long key = action.getLong("?vKey");
                MemoryObject vt;
                //log.debug("Checking distance of: " + key);
                if (checkMethod(method, action)) {
                    try {
                        vt = (MemoryObject)callMethod(method, key, conf);
                        if (vt != null) {
                            action.setArgument("?dist", vt.getDist());
                            status = true;
                        } else {
                            action.setArgument("?dist", 0.0);
                        }
                    } catch (ADEException ace) {
                        log.error("Error sending to Vision: " + ace);
                    }
                } else {
                    log.error("Can't getToken!");
                }
            }
            action.setExitStatus(status);
        } else if (action.isA("getTokenHeading")) {
            method = "getToken";
            boolean status = false;
            ADEPercept p = null;
            Object var = action.getArgument("?vKey");
            if (var instanceof ADEPercept) {
                p = (ADEPercept)action.getArgument("?vKey");
                action.setArgument("?heading", p.heading);
                status = true;
            //} else if ((p = pastPercepts.get((var + "").toUpperCase())) != null) {
            //    action.setArgument("?heading", p.heading);
            //    status = true;
            } else {
                // key is the name...
                Long key = action.getLong("?vKey");
                MemoryObject vt;
                //log.debug("Checking heading of: " + key);
                if (checkMethod(method, action)) {
                    try {
                        vt = (MemoryObject)callMethod(method, key, conf);
                        if (vt != null) {
                            action.setArgument("?heading", vt.getPan());
                            status = true;
                        } else {
                            action.setArgument("?heading", 0.0);
                        }
                    } catch (ADEException ace) {
                        log.error("Error sending to Vision: " + ace);
                    }
                } else {
                    log.error("Can't getToken!");
                }
            }
            action.setExitStatus(status);
        } else if (action.isA("getTokenLocation")) {
            Long key = action.getLong("?vKey");
            MemoryObject token = null;
            method = "getToken";
            if (checkMethod(method, action)) {
                try {
                    token = (MemoryObject)callMethod(method, key, conf);
                } catch (ADEException ace) {
                    log.error("Error sending to Vision: " + ace);
                }
            } else {
                log.error("Can't getToken!");
            }
            if (token == null) {
                action.setArgument("?xcoord", 0.0);
                action.setArgument("?ycoord", 0.0);
            } else {
                double t = action.getDouble("?heading");
                double d = token.getDist();
                double x = Math.cos(token.getPan()) * d;
                double y = Math.sin(token.getPan()) * d;
                double xtmp, ytmp;

                log.debug("Object " + token.getTokenId() + " is " + d + " m away at heading " + token.getPan());
                // Rotate the relative point to the robot's heading
                xtmp = Math.cos(t) * x - Math.sin(t) * y;
                ytmp = Math.cos(t) * y + Math.sin(t) * x;
                // Add that to robot's current position (which seems to be passed in -- should get it here...
                x = xtmp + action.getDouble("?xcoord");
                y = ytmp + action.getDouble("?ycoord");
                log.debug("Object " + token.getTokenId() + " is at " + x + ", " + y);
                // Round them off (in case Festival needs to speak them)
                x = (int)(x * 10.0) / 10.0;
                y = (int)(y * 10.0) / 10.0;
                // And return
                action.setArgument("?xcoord", x);
                action.setArgument("?ycoord", y);
            }
        } else if (action.isA("getTokenInside")) {
            boolean status = false;
            ADEPercept p = null;
            Object var = action.getArgument("?vKey");
            if (var instanceof ADEPercept) {
                p = (ADEPercept)action.getArgument("?vKey");
                status = (p.inside != null);
                if (status)
                    action.setArgument("?inside", p.inside);
                else
                    action.setArgument("?inside", "none");
            } else if ((p = pastPercepts.get(((String)var).toUpperCase())) != null) {
                status = (p.inside != null);
                if (status)
                    action.setArgument("?inside", p.inside);
                else
                    action.setArgument("?inside", "none");
            } else {
                log.error("getTokenInside not implemented for " + var);
                action.setArgument("?inside", "none");
            }
            action.setExitStatus(status);
        } else if (action.isA("getTokenFilled")) {
            boolean status = false;
            ADEPercept p = null;
            Object var = action.getArgument("?vKey");
            if (var instanceof ADEPercept) {
                p = (ADEPercept)action.getArgument("?vKey");
                status = p.filled;
            } else if ((p = pastPercepts.get(((String)var).toUpperCase())) != null) {
                status = p.filled;
            } else {
                log.error("getTokenFilled not implemented for " + var);
            }
            action.setExitStatus(status);
        } else if (action.isA("shiftFOA")) {
            // shiftFOA: turn to argv[2]
            method = "shiftFOA";
            Long key = action.getLong("?FOAKey");
            if (!checkMethod(method, action)) {
                log.error("No vision, can't " + action.getType());
            } else {
                try {
                    callMethod(method, key);
                } catch (ADEException re) {
                    log.error("Error sending to Vision: " + re);
                }
            }
        } else if (action.isA("nod")) {
            // nod: nod camera with intensity argv[2]
            method = "nod";
            Integer intensity = action.getInteger("?intensity");
            if (checkMethod("moveHead", action)) {
                try {
                    callMethod(method, false); // false -> don't wait
                } catch (ADEException ace) {
                    log.error("Error sending to Reddy: " + ace);
                }
            } else if (checkMethod(method, action)) {
                try {
                    callMethod(method, intensity);
                } catch (ADEException re) {
                    log.error("Error sending to Vision: " + re);
                }
            } else {
                log.error("No vision/reddy, can't " + action.getType());
            }
        } else if (action.isA("shake")) {
            // shake: shake camera with intensity argv[2]
            method = "shake";
            Integer intensity = action.getInteger("?intensity");
            if (checkMethod("moveHead", action)) {
                try {
                    callMethod(method, false); // false -> don't wait
                } catch (ADEException ace) {
                    log.error("Error sending to Reddy: " + ace);
                }
            } else if (checkMethod(method, action)) {
                try {
                    callMethod(method, intensity);
                } catch (ADEException re) {
                    log.error("Error sending to Vision: " + re);
                }
            } else {
                log.error("No vision/reddy, can't " + action.getType());
            }
        } else if (action.isA("setLookHome")) {
            int p[];
            try {
                // get pan/tilt
                // PWS: not sure about this any more...
                p = (int [])callMethod("getLookPosition");
                // Save the info
                log.debug("Setting LookHome to " + p[0] + " " + p[1]);
                ActionDBEntry.assertFact("lookhomepan", new Integer(p[0]));
                ActionDBEntry.assertFact("lookhometilt", new Integer(p[1]));
                ActionDBEntry t = ActionDBEntry.lookup("lookhomepan");
                if (t == null) {
                    log.debug("Already null!");
                } else {
                    log.debug("It's there!");
                    Integer u = (Integer)t.getValue();
                    log.debug("Value is: " + u);
                }
            } catch (ADEException ace) {
                log.error("Error getting Pan/Tilt to set look home: " + ace);
            }
        } else if (action.isA("lookAt")) {
            method = "LookAt";
            int t = action.getInteger("?theta");
            int p = action.getInteger("?phi");
            log.debug("Looking at " + t + ", " + p);
            if (checkMethod(method, action)) {
                try {
                    if (!(Boolean)callMethod(method, (double)t, (double)p)) {
                        log.error("Action: lookAt failed!");
                        action.setExitStatus(false);
                    }
                } catch (ADEException ace) {
                    log.error("Action: Error looking: " + ace);
                    action.setExitStatus(false);
                }
            } else {
                log.error("Can't look!");
            }
        } else if (action.isA("lookUp")) {
            // lookUp: tilt camera argv[2] up
            Integer intensity = action.getInteger("?intensity");
            int current, future;

            if (checkMethod("moveHead", action) && checkMethod("moveEyes", action)) {
                // Ignorning intensity
                try {
                    callMethod("moveHead", 110, 90);
                    callMethod("moveEyes", 45, 45, 45);
                } catch (ADEException ace) {
                    log.error("Error looking up: " + ace);
                }
            } else if (checkMethod("tilt", action)) {
                try {
                    current = (Integer)callMethod("getTilt");
                    if (intensity == null) {
                        // If not already looking higher, look up
                        if (current > 65) {
                            future = 65;
                            callMethod("tilt", future);
                        }
                    } else {
                        if (current > 65) {
                            if ((future = current - intensity) < 65)
                                future = 65;
                            callMethod("tilt", future);
                        } // else I'm already looking up as far as I should
                    }
                } catch (ADEException re) {
                    log.error("Error sending to Vision: " + re);
                }
            } else {
                log.error("No vision/reddy, can't " + action.getType());
            }
        } else if (action.isA("lookDown")) {
            // lookDown: tilt camera argv[2] down
            Integer intensity = action.getInteger("?intensity");
            int current, future;

            if (checkMethod("moveHead", action) && checkMethod("moveEyes", action)) {
                // Ignorning intensity
                try {
                    callMethod("moveHead", 70, 90);
                    callMethod("moveEyes", 45, 45, 45);
                } catch (ADEException ace) {
                    log.error("Error looking down: " + ace);
                }
            } else if (checkMethod("tilt", action)) {
                try {
                    current = (Integer)callMethod("getTilt");
                    if (intensity == null) {
                        // If not already looking lower, look down
                        if (current < 115) {
                            future = 115;
                            callMethod("tilt", future);
                        }
                    } else {
                        if (current < 115) {
                            if ((future = current + intensity) > 115)
                                future = 115;
                            callMethod("tilt", future);
                        } // else I'm already looking down as far as I should
                    }
                } catch (ADEException re) {
                    log.error("Error sending to Vision: " + re);
                }
            } else {
                log.error("No vision/reddy, can't " + action.getType());
            }
        } else if (action.isA("startTracking")) {
            method = "startTracking";
            if (!checkMethod(method, action)) {
                log.error(action.getType() + " not supported at this time!");
            } else {
                try {
                    callMethod(method);
                } catch (ADEException re) {
                    log.error("Error sending to Vision: " + re);
                }
            }
        } else if (action.isA("stopTracking")) {
            method = "stopTracking";
            if (!checkMethod(method, action)) {
                log.error(action.getType() + " not supported at this time!");
            } else {
                try {
                    callMethod(method);
                } catch (ADEException re) {
                    log.error("Error sending to Vision: " + re);
                }
            }
        } else if (action.isA("startBlobDetect")) {
            method = "startType";
            if (!checkMethod(method, action)) {
                log.error("No vision, can't " + action.getType());
            } else {
                try {
                    Long vtype = (Long)callMethod("getTypeId", createPredicate("type(X, blob)"));
                    callMethod(method, vtype);
                } catch (ADEException re) {
                    log.error("Error sending to Vision: " + re);
                }
            }
        } else if (action.isA("stopBlobDetect")) {
            method = "stopType";
            if (!checkMethod(method, action)) {
                log.error("No vision, can't " + action.getType());
            } else {
                try {
                    Long vtype = (Long)callMethod("getTypeId", createPredicate("type(X, blob)"));
                    callMethod(method, vtype);
                } catch (ADEException re) {
                    log.error("Error sending to Vision: " + re);
                }
            }
        } else if (action.isA("learnVisionType")) {
            method = "learnType";
            Long time = -1L;
            Predicate type = createPredicate((String)action.getArgument("?vtype"));
            log.debug("Learning " + type);
            if (checkMethod(method, action)) {
                try {
                    if ((Boolean)callMethod(method, type)) {
                        time = (Long)callMethod("getTypeId", type);
                    }
                    action.setArgument("?vtypeid", time);
                } catch (ADEException re) {
                    log.error("Error sending to Vision: " + re);
                }
            } else {
                log.error("No vision, can't " + action.getType());
            }
        } else if (action.isA("getPerspective")) {
                log.error(myID + ": can't do getPerspective! "
                        + "Needs to be re-implemented to work with new Vision API.");
        } else if (action.isA("getRelationship")) {
                log.error(myID + ": can't do getRelationship! "
                        + "Needs to be re-implemented to work with new Vision API.");
        } else if (action.isA("getRelationship2")) {
                log.error(myID + ": can't do getRelationship2! "
                        + "Needs to be re-implemented to work with new Vision API.");
        } else if (action.isA("lookFor")) {
            method = "getTokenIds";
            String type = (String)action.getArgument("?type");
            // Sapa will send names like box!0 instead of types
            int bi = type.indexOf('!');
            if (bi > 0) {
                type = type.substring(0, bi);
            }
            log.debug("LOOKING FOR ENTITY OF TYPE " + type);
            String col = (String)action.getArgument("?col");
            String loc = (String)action.getArgument("?loc");
            ArrayList<ADEPercept> asps;
            ArrayList<Long> keys;
            Long typeId = -1L;
            boolean percept = false;
            MemoryObject mo;
            String nName = null;
            ArrayList<Predicate> updates = new ArrayList<Predicate>();
            if (checkMethod(method, action)) {
                if (type.equals("box")) { // treating as blobs for the moment
                    log.debug("looking for boxes");
                    if (col == null) {
                        col = "red";
                    }
                    try {
                        ArrayList<Predicate> descriptors = new ArrayList<Predicate>();
                        descriptors.add(utilities.Util.createPredicate("type(X, blob)"));
                        descriptors.add(utilities.Util.createPredicate("color(X, " + col + ")"));
                        typeId = (Long)callMethod("getTypeId", descriptors);
                        log.debug("got typeid for blobs: " + typeId);
                        keys = (ArrayList)callMethod(method, typeId, conf);
                    } catch (ADEException ere) {
                        log.error("Error sending to Vision: " + ere);
                        keys = null;
                    }
                    if (keys != null && keys.size() > 0) {
                        log.debug("Found at least one " + type);
                        percept = true;
                        //plannerPosts.add(new Predicate("looked_for", type, loc));
                        for (int i = 0; i < keys.size(); i++) {
                            Long monum = keys.get(i);
                            String name = monum.toString();
                            name = "vision" + name;
                            nName = name; // returns the last name we get
                            updates.add(new Predicate("isa", name, type));
                            //updates.add(new Predicate("found", name, loc));
                            action.setArgument("?type", name);
                            updates.add(new Predicate("in", name, loc));
                            mo = null;
                            try {
                                mo = (MemoryObject)callMethod("getToken", monum, 0.5);
                                updates.add(new Predicate("color", name, mo.getColorLabel()));
                            } catch (ADEException ace) {
                                log.error("Error getting token for " + name + ": " + ace);
                                updates.add(new Predicate("color", name, "unknown"));
                            }
                        }
                    }
                }
                if (!percept) {
                    action.setExitStatus(false);
                } else {
                    ami.processStateUpdate(updates, false);
                    action.setArgument("?perc", nName);
                    action.setExitStatus(true);
                }
            } else {
                // Ought to do something here...
                log.error(myID + ": can't do lookFor!");
                action.setExitStatus(false);
            }
        } else {
            log.error("Unrecognized Vision command: " +
                    action.getType());
        }
    }

    /**
     * Execute a speech production primitive (i.e., say something, modify
     * voice parameters, etc.).
     * @param action the action specification
     */
    void doSpeechProdPrimitive(ActionDBEntry action) {
        log.debug(format("[doSpeechProdPrimitive] Executing action %s", action));
        String speaker = (String)action.getArgument("?speaker");
                
        System.out.println("SPEAKER: "+speaker);

        if ((speaker == null) ||
            ((!speaker.equalsIgnoreCase(agentname)) &&
             (!speaker.equalsIgnoreCase("Rudy")) &&
             (!speaker.equalsIgnoreCase("Mary")) &&
             (!speaker.equalsIgnoreCase("cbot")) &&
             (!speaker.equalsIgnoreCase("robot")) &&
             (!speaker.equalsIgnoreCase("cramer")) &&
             (!speaker.equalsIgnoreCase("icarus")) &&
             (!speaker.equalsIgnoreCase("actr")) &&
             (!speaker.equalsIgnoreCase("me")))){
            System.out.println("I don't know who's speaking!");
            return;
        }

        String method = null;
        if (action.isA("sayTextNow") || action.isA("sayText") || action.isA("report")) {
            String text = (String)action.getArgument("?statement");
            text = bindText(action, text);


            text = text.replaceAll("\"", "");
            //text = text.substring(1, text.length() - 1);
            try {
                if (action.isA("sayTextNow")) {
                    sayText(text, action);
                } else if (checkMethod("isSpeaking", action)
                    && !(Boolean)callMethod("isSpeaking")) {
                    sayText(text, action);
                }
                if (false && checkMethod("setRepeatText", action)) {
                    callMethod("setRepeatText", text);
                }
            } catch (ADEException re) {
                log.error("Error sending to text server: " + re);
            }
        } else if (action.isA("lipSync")) {
            method = "lipSync";
            String file = (String)action.getArgument("?file");
            String silent = (String)action.getArgument("?silent");
            try {
                if (checkMethod(method, action)) {
                    log.debug("Sending lipSync " + file);
                    callMethod("lipSync", file, silent.equalsIgnoreCase("true"));
                } else {
                    log.error("NO REDDY");
                }
            } catch (ADEException ace) {
                log.error("Error sending to Reddy server: " + ace);
            }
        } else if (action.isA("waveDance")) {
            method = "Dance";
            String method2 = "DanceSync";
            String file = (String)action.getArgument("?file");
            String silent = (String)action.getArgument("?silent");
            try {
                if (checkMethod(method, action)) {
                    callMethod(method, file);
                } else if (checkMethod(method2, action)) {
                    callMethod(method2, file, silent.equalsIgnoreCase("true"));
                }
            } catch (ADEException ace) {
                log.error("Error sending to Reddy server: " + ace);
            }
        } else if (action.isA("sapareport")) {
            // TODO: need to allow more than boxes, need to allow to specify
            // characteristics to report

            // TODO: need to add sapapost field to action.xml, so that
            // action can inform planner when postconditions are achieved--in
            // this case, "reported box1 green room0" or the like
            log.debug("PWS: do we want to look up characteristics about the subject?");
            //String clr = (String)action.getArgument("?pr");
            String type = action.getArgumentType("?bx");
            String loc = (String)action.getArgument("?loc");
            //String report = "There is a " + clr + " box in " + loc;
            String report = "There is a " + type + " in " + loc;
            log.debug("sapareport: " + report);
            sayText(report, action);
        } else if (action.isA("startChat")) {
            method = "startChat";
            String chatTopic = (String)action.getArgument("?topic");
            if (checkMethod(method, action)) {
                try {
                    callMethod(method, chatTopic);
                } catch (ADEException re) {
                    log.error("Error sending to Speech: " + re);
                }
            } else {
                log.error(myID + ": no Speech, can't startChat");
            }
        } else if (action.isA("endChat")) {
            method = "endChat";
            if (checkMethod(method, action)) {
                try {
                    callMethod(method);
                } catch (ADEException re) {
                    log.error("Error sending to Speech: " + re);
                }
            } else {
                log.error(myID + ": no Speech, can't endChat");
            }
        } else if (action.isA("changeVoice")) {
            method = "changeVoice";
            String newVoice = (String)action.getArgument("?voice");
            log.debug("Got changeVoice: " + newVoice);
            if (checkMethod(method, action)) {
                try {
                    callMethod(method, newVoice);
                    logIt("VOICE " + newVoice);
                } catch (ADEException re) {
                    log.error("Error sending to text server: " + re);
                }
            } else {
                log.error("No Speech/Festival, can't change voice");
            }
        } else if (action.isA("changeMood")) {
            method = "changeMood";
            String newMood = (String)action.getArgument("?mood");
            log.debug("Got changeMood: " + newMood);
            ActionDBEntry.assertFact("changemood", newMood.toLowerCase());
            if (checkMethod(method, action)) {
                try {
                    callMethod(method, newMood);
                    logIt("MOOD " + newMood);
                } catch (ADEException re) {
                    log.error("Error sending to text server: " + re);
                }
            } else {
                log.error("No Speech/Festival, can't change mood");
            }
        } else if (action.isA("changeSpeed")) {
            method = "changeSpeed";
            double newSpeed = action.getDouble("?speed");
            if (checkMethod(method, action)) {
                try {
                    callMethod(method, newSpeed);
                } catch (ADEException re) {
                    log.error("Error sending to text server: " + re);
                }
            } else {
                log.error("No Speech/Festival, can't change speed");
            }
        } else if (action.isA("stopUtterance")) {
            method = "stopUtterance";
            if (checkMethod(method, action)) {
                try {
                    callMethod(method);
                } catch (ADEException re) {
                    log.error("Error sending to text server: " + re);
                }
            } else {
                log.error("No Speech/Festival, can't stop utterance");
            }
        } else if (action.isA("playWav")) {
            String filename = (String)action.getArgument("?filename");
            Process p;
            try {
                p = Runtime.getRuntime().exec("./adeplay " + filename);
                try {
                    p.waitFor();
                } catch (Exception e) {
                }
            } catch (IOException ioe) {
                log.debug("playWav: " + ioe);
            }
        } else if (action.isA("beep")) {
            try {
                Runtime.getRuntime().exec("aplay com/action/db/tone.wav");
                Sleep(2000);
            } catch (IOException ioe) {
                log.debug("Beep: " + ioe);
            }
        } else {
            log.error("Unrecognized speech production primitive: " +
                    action.getType());
        }
    }

    /**
     * Execute a speech recognition primitive (i.e., get spoken text,
     * change language model, etc.).
     * @param action the action specification
     */
    void doSpeechRecPrimitive(ActionDBEntry action) {
        log.debug(format("[doSpeechRecPrimitive] Executing action %s", action));
        String method = null;

        if (action.isA("getText")) {
            method = "getSphinx4Text";
            String text;
            long ts;
            if (checkMethod(method, action)) {
                ArrayList<Object> speech;
                try {
                    speech = (ArrayList)callMethod(method);
                    if (speech == null) {
                        action.setExitStatus(false);
                    } else {
                        ts = (Long)speech.get(1);
                        if (ts <= latestSphinx4Text) {
                            action.setExitStatus(false);
                        } else {
                            text = (String)speech.get(0);
                            log.debug("Receive thread just got: " + text);
                            action.setArgument("?statement", text);
                            latestSphinx4Text = ts;
                            callMethod("cancelSphinx4Text", ts);
                        }
                    }
                } catch (Exception re) {
                    log.error("Error sending to Speech: " + re);
                    action.setExitStatus(false);
                }
            } else {
                // Ought to do something here...
                log.error(myID + ": no Speech, can't getText");
                action.setExitStatus(false);
            }
        } else if (action.isA("trackSpeechSource")) {
            method = "TrackingOn";
            boolean status = false;
            String track = (String)action.getArgument("?track");
            if (checkMethod(method, action)) {
                try {
                    callMethod("TrackingOn", track.equalsIgnoreCase("true"));
                    status = true;
                } catch (ADEException ace) {
                    log.error("Error setting speech tracking: " + ace);
                }
            }
            action.setExitStatus(status);
        } 
        /* else if (action.isA("getSpeechSource")) {
            method = "getMostRecent";
            LocalizedInfo li = null;
            int theta = 0;
            int phi = 0;
            boolean status = false;
            if (checkMethod(method, action)) {
                try {
                    li = (LocalizedInfo)callMethod(method);
                    if (li != null) {
                        theta = (int)Math.round(li.theta);
                        phi = (int)Math.round(li.phi);
                        status = true;
                        log.debug("got speech source at " + theta + ", " + phi);
                    }
                } catch (ADEException ace) {
                    log.error("Error getting speech source: " + ace);
                }
            }
            action.setArgument("?theta", theta);
            action.setArgument("?phi", phi);
            action.setExitStatus(status);

        } 
        */ else if (action.isA("changeSphinx4Configuration")) {
            method = "changeSphinx4Configuration";
            String config = (String)action.getArgument("?config");
            log.debug("Changing Sphinx4 config to " + config);
            if (checkMethod(method, action)) {
                try {
                    callMethod(method, config);
                } catch (Exception re) {
                    log.error("Error sending to Speech: " + re);
                }
            }
        } else if (action.isA("changeSphinx4Grammar")) {
            method = "changeSphinx4Grammar";
            String grammar = (String)action.getArgument("?grammar");
            log.debug("Changing Sphinx4 grammar to " + grammar);
            if (checkMethod(method, action)) {
                ami.setSphinxGrammar(grammar);
                try {
                    callMethod(method, grammar);
                } catch (Exception re) {
                    log.error("Error sending to Speech: " + re);
                }
            }
        } else if (action.isA("changeConfiguration")) {
            method = "changeConfiguration";
            if (checkMethod(method, action)) {
                String newConfig = (String)action.getArgument("?config");
                try {
                    callMethod(method, newConfig);
                    log.debug(myID +": changed speech config");
                } catch (ADEException re) {
                    log.error("Error sending to Speech: " + re);
                }
            } else {
                log.error(myID + ": no Speech, can't changeConfig");
            }
        } else {
            log.error("Unrecognized speech recognition primitive: " +
                    action.getType());
        }
    }

    /**
     * Execute a misc primitive (i.e., comparisons, etc.)
     * @param action the action specification
     */
    void doMiscPrimitive(ActionDBEntry action) {
        //System.out.println("DMP: "+action);
        log.trace(format("[doMiscPrimitive] Executing action %s", action));
        String method = null;

        // Get the script's children and push them on the stack.
        // PWS: Do I need to check the script's preconditions here?
        if (action.isA("putHash")) {
            String key = action.getString("?key");
            key = bindText(action, key);
            Object value = action.getArgument("?value");
            globalHash.put(key,value);
        } else if (action.isA("getHash")) {
            String key = action.getString("?key");
            key = bindText(action, key);
            Object value = globalHash.get(key);
            action.setArgument("?value", value);
        } else if (action.isA("getWirelessStatus")){
            System.out.println("TRYING TO GET WIRELESS STATUS");
            action.setArgument("!wstat", Boolean.toString(NetworkingStatus.isEthernetAvailable()));
        } else if(action.isA("setCamera")) {
            int cameraID = action.getInteger("?cameraID");

            try {
                log.debug("---------Setting view part 1 --------");
                callMethod("setCamera", cameraID);
            } catch (ADEException re) {
                log.error("Error sending USARSim: " + re);
            }
        } else if (action.isA("stringCompare")) {
            String text1 = (String)action.getArgument("?stringOne");
            String text2 = (String)action.getArgument("?stringTwo");
            //log.debug("stringCompare: post comparing " + text1 + " and " + text2 + ": " + text1.equalsIgnoreCase(text2));
            action.setExitStatus(text1.equalsIgnoreCase(text2));
        } else if (action.isA("stringContains")) {
            String text1 = (String)action.getArgument("?stringOne");
            String text2 = (String)action.getArgument("?stringTwo");
            text1 = text1.toLowerCase();
            text2 = text2.toLowerCase();
            //log.debug("stringCompare: post comparing " + text1 + " and " + text2 + ": " + text1.contains(text2));
            action.setExitStatus(text1.contains(text2));
        } else if (action.isA("XXXsetText")) {
            String text = (String)action.getArgument("?newString");
            action.setArgument("?oldString", text);
        } else if (action.isA("checkPostcond")) {
        } else if (action.isA("actionSucceed")) {
            action.setExitStatus(true);
            action.exit(true);
        } else if (action.isA("qsleep")) {
            long time = action.getLong("?sleepMillis");
            long stime = time;
            if (stime > (ami.sliceTime*1.5)) {
                stime = ami.sliceTime;
            } else {
                action.setExitStatus(false);
            }
            // PWS: don't actually have to sleep, as runCycle will
            //Sleep(stime);
            action.setArgument("?sleepMillis", time - stime);
        } else if (action.isA("actionFail")) {
            action.setExitStatus(false);
            action.exit(false);
        } else if (action.isA("assertFact")) {
            String fact = (String)action.getArgument("?fact");
            //log.debug("Asserting fact: " + fact);
            ActionDBEntry.assertFact(fact);
        } else if (action.isA("assertFactVal")) {
            String fact = (String)action.getArgument("?fact");
            Object val = action.getArgument("?value");
            ActionDBEntry.assertFact(fact, val);
            //log.debug("Asserting fact: " + fact + " with val: " + val);
        } else if (action.isA("retractFact")) {
            String name = (String)action.getArgument("?fact");
            ActionDBEntry.retractFact(name);
        } else if (action.isA("isFact")) {
            // See if a fact has been asserted
            ActionDBEntry factEnt;
            String fact = (String)action.getArgument("?fact");
            factEnt = ActionDBEntry.lookup(fact.toLowerCase());
            action.setExitStatus(factEnt != null);
        } else if (action.isA("isFactVal")) {
            // See if a fact has been asserted with a particular value
            ActionDBEntry factEnt;
            String fact = (String)action.getArgument("?fact");
            Object val = action.getArgument("?value");
            Object newVal;
            boolean status = false;
            factEnt = ActionDBEntry.lookup(fact.toLowerCase());
            if (factEnt != null) {
                newVal = factEnt.getValue();
                if ((val instanceof String) && (newVal instanceof String)) {
                    status = val.equals(newVal);
                } else {
                    status = (newVal == val);
                }
            }
            action.setExitStatus(status);
        } else if (action.isA("retrieveFactVal")) {
            ActionDBEntry factEnt;
            String fact = (String)action.getArgument("?fact");
            Object value;
            factEnt = ActionDBEntry.lookup(fact.toLowerCase());
            if (factEnt != null) {
                //log.debug("Found fact " + fact);
                value = factEnt.getValue();
                //log.debug(fact + " has value " + value);
                action.setArgument("?value", value);
                action.setExitStatus(true);
            } else {
                log.error("Found no fact " + fact);
                action.setExitStatus(false);
            }
        } else if (action.isA("getNewList")) {
            ArrayList<Object> nl = new ArrayList<Object>();
            action.setArgument("?list", nl);
        } else if (action.isA("getListSize")) {
            Object var = action.getArgument("?list");
            int size = 0;
            if (var instanceof java.util.AbstractList) {
                size = ((java.util.AbstractList)var).size();
            } else {
                log.error("Unrecognized list type: " + var);
            }
            action.setArgument("?val", size);
        } else if (action.isA("getListElement")) {
            Object var = action.getArgument("?list");
            int index = action.getInteger("?index");
            Object arg = null;
            if (var instanceof java.util.AbstractList) {
                int size = ((java.util.AbstractList)var).size();
                if (index >= 0 && index < size) {
                    arg = ((java.util.AbstractList)var).get(index);
                } else {
                    log.error("getListElement: index out of bounds: " + index);
                    action.setExitStatus(false);
                }
            } else {
                log.error("Invalid list type: " + var);
                action.setExitStatus(false);
            }
            if (arg != null) {
                action.setArgument("?val", arg);
            }
        } else if (action.isA("addListElement")) {
            Object var = action.getArgument("?list");
            Object arg = action.getArgument("?val");
            if (var instanceof java.util.AbstractList) {
                ((java.util.AbstractList)var).add(arg);
            } else {
                log.error("Invalid list type: " + var);
                action.setExitStatus(false);
            }
        } else if (action.isA("delListElement")) {
            Object var = action.getArgument("?list");
            int index = action.getInteger("?index");
            Object arg = null;
            if (var instanceof java.util.AbstractList) {
                int size = ((java.util.AbstractList)var).size();
                if (index >= 0 && index < size) {
                    arg = ((java.util.AbstractList)var).remove(index);
                } else {
                    log.error("getListElement: index out of bounds: " + index);
                    action.setExitStatus(false);
                }
            } else {
                log.error("Invalid list type: " + var);
                action.setExitStatus(false);
            }
            if (arg != null) {
                action.setArgument("?val", arg);
            }
        } else if (action.isA("getName")) {
            Object var = action.getArgument("?var");
            String name = null;
            if (var instanceof Predicate) {
                Predicate p = (Predicate)var;
                name = p.getName();
            } else if (var instanceof ActionDBEntry) {
                name = ((ActionDBEntry)var).getName();
            }
            if (name != null) {
                action.setArgument("?name", name);
            }
        } else if (action.isA("getArgument")) {
            Object var = action.getArgument("?var");
            int index = action.getInteger("?index");
            Object arg = null;
            if (var instanceof Predicate) {
                Predicate p = (Predicate)var;
                arg = p.getArgs().get(index);
            } else if (var instanceof ActionDBEntry) {
                // PWS: need to figure out whether this makes sense
            } else {
                log.error("Unrecognized var: " + var);
            }
            if (arg != null) {
                action.setArgument("?val", arg);
            }
        } else if (action.isA("isA")) {
            Object var = (String)action.getArgument("?var");
            String type = (String)action.getArgument("?type");
            String name = null;
            ActionDBEntry v;
            if (var instanceof Predicate) {
                name = ((Predicate)var).getName();
            } else if (var instanceof String) {
                name = (String)var;
                if (name.charAt(0) == '?') {
                    name = null;
                }
            }
            if ((name == null) ||
               ((v = ActionDBEntry.lookup(name)) == null) ||
               (!v.isA(type))) {
                action.setExitStatus(false);
                log.error("not of type " + type);
            }
        } else if (action.isA("isBound")) {
            Object var = action.getArgument("?var");
            if (var instanceof String) {
                String v = (String)var;
                if (v.charAt(0) == '?')
                    action.setExitStatus(false);
                else
                    action.setExitStatus(true);
            } else {
                action.setExitStatus(true);
            }
        } else if (action.isA("true")) {
            action.setExitStatus(true);
        } else if (action.isA("false")) {
            action.setExitStatus(false);
        } else if (action.isA("logText")) {
            String text = action.getArgument("?text").toString();
            text = bindText(action, text);
            logIt("logText: " + text);
            log.debug("logText: " + text);
        } else if (action.isA("printPriorities")) {
            ami.printPriorities();
        } else if (action.isA("set")) {
            Object value = action.getArgument("?value");
            String type = action.getArgumentType("?target");
            //log.debug("set: " + type + " " + value);
            if (ActionDBEntry.lookup(type).isA("double"))
                if (value  instanceof Double)
                    action.setArgument("?target", value);
                else
                    action.setArgument("?target", new Double(value.toString()));
            else if (ActionDBEntry.lookup(type).isA("long"))
                if (value  instanceof Long)
                    action.setArgument("?target", value);
                else
                    action.setArgument("?target", new Long(value.toString()));
            else if (ActionDBEntry.lookup(type).isA("integer"))
                if (value  instanceof Integer)
                    action.setArgument("?target", value);
                else
                    action.setArgument("?target", new Integer(value.toString()));
            else
                action.setArgument("?target", value);
            action.setArgument("?target", value);
        } else if (action.isA("setText")) {
            String text = action.getArgument("?text").toString();
            if (text.charAt(0) == '"')
                text = text.substring(1, text.length() - 1);
            text = bindText(action, text);
            //log.debug("setText: " + text);
            action.setArgument("?target", text);
        } else if (action.isA("catText")) {
            String text1 = action.getArgument("?text1").toString();
            if (text1.charAt(0) == '"')
                text1 = text1.substring(1, text1.length() - 1);
            text1 = bindText(action, text1);
            String text2 = action.getArgument("?text2").toString();
            if (text2.charAt(0) == '"')
                text2 = text2.substring(1, text2.length() - 1);
            text2 = bindText(action, text2);
            //log.debug("catText: " + text1 + text2);
            action.setArgument("?target", text1 + text2);
        } else if (action.isA("printText")) {
            String text = action.getArgument("?text").toString();
            text = bindText(action, text);
            if (text.charAt(0) == '"')
                text = text.substring(1, text.length() - 1);
            System.out.println("ActionInterpreter printText: " + text);
        } else if (action.isA("setRepeatText")) {
            String text = action.getArgument("?repeat").toString();
            text = bindText(action, text);
            if (text.charAt(0) == '"')
                text = text.substring(1, text.length() - 1);
            try {
                callMethod(0, "setRepeatText", text);
            } catch (ADEException re) {
                log.error("Error sending to SimSpeech: " + re);
            }
        } else if (action.isA("setInstructionText")) {
            instruction = action.getArgument("?inst").toString();
        } else if (action.isA("clearInstructionText")) {
            instruction = null;
        } else if (action.isA("clarify")) {
            if (instruction != null)
                sayText(instruction, action);
        } else if (action.isA("randomInteger")) {
            Integer l = action.getInteger("?lower");
            Integer u = action.getInteger("?upper");
            double p = Math.random();
            Integer v = (int)Math.floor((u - l) * p) + l;
            action.setArgument("?value", v);
        } else if (action.isA("randomDouble")) {
            Double l = action.getDouble("?lower");
            Double u = action.getDouble("?upper");
            double p = Math.random();
            Double v = ((u - l) * p) + l;
            action.setArgument("?value", v);
        } else if (action.isA("+")) {
            Double a1 = action.getDouble("?arg1");
            Double a2 = action.getDouble("?arg2");
            Double s = a1 + a2;

            //log.debug("Add " + a1 + " to " + a2 + ": " + s);
            action.setArgument("?sum", s);
        } else if (action.isA("-")) {
            Double a1 = action.getDouble("?arg1");
            Double a2 = action.getDouble("?arg2");
            Double d = a1 - a2;

            action.setArgument("?diff", d);
        } else if (action.isA("*")) {
            Double a1 = action.getDouble("?arg1");
            Double a2 = action.getDouble("?arg2");
            Double p = a1 * a2;

            //log.debug("Multiply " + a1 + " by " + a2 + ": " + p);
            action.setArgument("?prod", p);
        } else if (action.isA("/")) {
            Double a1 = action.getDouble("?arg1");
            Double a2 = action.getDouble("?arg2");
            Double q = a1 / a2;

            action.setArgument("?quot", q);
        } else if (action.isA("%")) {
            Integer a1 = action.getInteger("?arg1");
            Integer a2 = action.getInteger("?arg2");
            Integer q = a1 % a2;

            action.setArgument("?quot", q);
        } else if (action.isA("round")) {
            Double a = action.getDouble("?arg");
            Long r = Math.round(a);

            action.setArgument("?round", r);
        } else if (action.isA("gt")) {
            Double a1 = action.getDouble("?arg1");
            Double a2 = action.getDouble("?arg2");

            //log.debug("Comparing: " + a1 + " > " + a2 + " " + (a1 > a2) + " " + (a1.doubleValue() > a2.doubleValue()));
            action.setExitStatus(a1 > a2);
        } else if (action.isA("ge")) {
            Double a1 = action.getDouble("?arg1");
            Double a2 = action.getDouble("?arg2");

            //log.debug("Comparing: " + a1 + " >= " + a2 + " " + (a1 >= a2) + " " + (a1.doubleValue() >= a2.doubleValue()));
            action.setExitStatus(a1 >= a2);
        } else if (action.isA("lt")) {
            Double a1 = action.getDouble("?arg1");
            Double a2 = action.getDouble("?arg2");

            //log.debug("Comparing: " + a1 + " < " + a2 + " " + (a1 < a2) + " " + (a1.doubleValue() < a2.doubleValue()));
            action.setExitStatus(a1 < a2);
        } else if (action.isA("le")) {
            Double a1 = action.getDouble("?arg1");
            Double a2 = action.getDouble("?arg2");

            //log.debug("Comparing: " + a1 + " <= " + a2 + " " + (a1 <= a2) + " " + (a1.doubleValue() <= a2.doubleValue()));
            action.setExitStatus(a1 <= a2);
        } else if (action.isA("=")) {
            Double a1 = action.getDouble("?arg1");
            Double a2 = action.getDouble("?arg2");

            action.setExitStatus(a1.doubleValue() == a2.doubleValue());
        } else if (action.isA("getTimeOfDay")) {
            action.setArgument("?time", System.currentTimeMillis());
        } else if (action.isA("kill")) {
            String victim = (String)action.getArgument("?victim");
            try {
                if (victim.equals("dsi")) {
                    log.debug("Killing dsi");
                    callMethod("serverKill");
                } else if (victim.equals("ssi")) {
                    callMethod("serverKill");
                }
            } catch (ADEException ace) {
                log.error("Exception sending to Discourse", ace);
            }
        } else if (action.isA("startADEComponentLogging")) {
            log.debug("starting ADEComponentLogging");
            setADEComponentLogging(true);
        } else if (action.isA("stopADEComponentLogging")) {
            setADEComponentLogging(false);
        } else if (action.isA("incrementPositiveLocalAffect")) {
            Double inc = action.getDouble("?increment");
            action.updateParentAffect(inc, true);
            updateAffect(inc, true);
        } else if (action.isA("incrementNegativeLocalAffect")) {
            Double inc = action.getDouble("?increment");
            action.updateParentAffect(inc, false);
            updateAffect(inc, false);
        } else if (action.isA("incrementPositiveAffect")) {
            Double inc = action.getDouble("?increment");
            ami.incrementPositiveAffect(inc);
        } else if (action.isA("incrementNegativeAffect")) {
            Double inc = action.getDouble("?increment");
            ami.incrementNegativeAffect(inc);
        } else if (action.isA("setNegativeAffect")) {
            Double inc = action.getDouble("?increment");
            ami.setNegativeAffect(inc);
        } else if (action.isA("SurveySliderItem")) {
            // SurveySliderItem <question text> <lower bound> <upper bound>  <result>
            String q = (String)action.getArgument("?question");
            q = q.substring(1, q.length() - 1);
            Integer l = action.getInteger("?lower");
            Integer u = action.getInteger("?upper");
            String ll = (String)action.getArgument("?lowerLabel");
            ll = ll.substring(1, ll.length() - 1);
            ll = ll.replace('_', ' ');
            String ul = (String)action.getArgument("?upperLabel");
            ul = ul.substring(1, ul.length() - 1);
            ul = ul.replace('_', ' ');
            Integer v = 0;
            try {
                v = (Integer)callMethod(0, "SurveySliderItem", q, l, u, ll, ul);
            } catch (ADEException re) {
                log.error("Error sending to Survey: " + re);
            }
            action.setArgument("?response", v);
        } else if (action.isA("SurveySliderItem2")) {
            // SurveySliderItem <question text> <lower bound> <upper bound>  <result>
            String s = (String)action.getArgument("?statement");
            s = s.substring(1, s.length() - 1);
            String q = (String)action.getArgument("?question");
            q = q.substring(1, q.length() - 1);
            Integer l = action.getInteger("?lower");
            Integer u = action.getInteger("?upper");
            String ll = (String)action.getArgument("?lowerLabel");
            ll = ll.substring(1, ll.length() - 1);
            String ul = (String)action.getArgument("?upperLabel");
            ul = ul.substring(1, ul.length() - 1);
            Integer v = 0;
            try {
                v = (Integer)callMethod(0, "SurveySliderItem", s, q, l, u, ll, ul);
            } catch (ADEException re) {
                log.error("Error sending to Survey: " + re);
            }
            action.setArgument("?response", v);
        } else if (action.isA("SurveyRadioItem")) {
            // SurveyRadioItem <question text> <valid responses> <result>
            String q = (String)action.getArgument("?question");
            if (q.charAt(0) == '"')
                q = q.substring(1, q.length() - 1);
            q = bindText(action, q);

            String v = (String)action.getArgument("?buttons");
	    System.out.println("ICH KRIEG NE KRISE");
            v = v.substring(1, v.length() - 1);
            v = bindText(action, v);

            StringTokenizer s = new StringTokenizer(v);
            ArrayList<String> vs = new ArrayList<String>();
            while (s.hasMoreTokens())
                vs.add(s.nextToken().replace('_', ' '));
            try {
                v = (String)callMethod(0, "SurveyRadioItem", q, vs);
            } catch (ADEException re) {
                log.error("Error sending to Survey: " + re);
            }
            action.setArgument("?response", v);
        } else if (action.isA("SurveyRadioItemKeyed")) {

            // SurveyRadioItem <question text> <valid responses> <result>
            String q = (String)action.getArgument("?question");
            q = q.substring(1, q.length() - 1);
            q = bindText(action, q);

            String v = (String)action.getArgument("?buttons");
            if(v.charAt(0) == '"') {
                v = v.substring(1, v.length() - 1);
            }
            v = bindText(action, v);

            StringTokenizer s = new StringTokenizer(v);
            ArrayList<String> vs = new ArrayList<String>();
            while (s.hasMoreTokens())
                vs.add(s.nextToken().replace('_', ' '));

            String c = (String)action.getArgument("?keys");
            if(c.charAt(0) == '"') {
                c = c.substring(1, c.length() - 1);
            }
            ArrayList<Character> vc = new ArrayList<Character>();
            for(int i=0;i<c.length();i++) {
                vc.add(c.charAt(i));
            }

            try {
                v = (String)callMethod(0, "SurveyRadioItem", q, vs, vc);
            } catch (ADEException re) {
                log.error("Error sending to Survey: " + re);
            }
            action.setArgument("?response", v);
        } else if (action.isA("SurveyCheckboxItem")) {
            // SurveyRadioItem <question text> <valid responses> <result>
            String q = (String)action.getArgument("?question");
            q = q.substring(1, q.length() - 1);
            String v = (String)action.getArgument("?buttons");
            v = v.substring(1, v.length() - 1);
            StringTokenizer s = new StringTokenizer(v);
            ArrayList<String> vs = new ArrayList<String>();
            while (s.hasMoreTokens())
                vs.add(s.nextToken().replace('_', ' '));
            try {
                v = (String)callMethod(0, "SurveyCheckboxItem", q, vs);
                log.debug("Checkbox response: " + v);
            } catch (ADEException re) {
                log.error("Error sending to Survey: " + re);
            }
            action.setArgument("?response", v);
        } else if (action.isA("SurveyTextItem")) {
            // SurveyTextItem <question text>
            String q = (String)action.getArgument("?question");
            if (q.charAt(0) == '"')
                q = q.substring(1, q.length() - 1);
            String v = "";
            try {
                v = (String)callMethod(0, "SurveyTextItem", q);
            } catch (ADEException re) {
                log.error("Error sending to Survey: " + re);
            }
            action.setArgument("?response", v);
        } else if (action.isA("SurveyText")) {
            // SurveyText <display text> <button text>
            String text = action.getArgument("?text").toString();
            String button = action.getArgument("?button").toString();
            text = bindText(action, text);
            text = text.substring(1, text.length() - 1);
            button = button.substring(1, button.length() - 1);
            try {
                callMethod(0, "SurveyText", text, button);
            } catch (ADEException re) {
                log.error("Error sending to Survey: " + re);
            }
        } else if (action.isA("SurveyButton")) {
            // SurveyButton <button text>
            String button = action.getArgument("?button").toString();
            button = button.substring(1, button.length() - 1);
            try {
                callMethod(0, "SurveyButton", button);
            } catch (ADEException re) {
                log.error("Error sending to Survey: " + re);
            }
        } else if (action.isA("SurveyTextColor")) {
            // Change survey text color
            String color = action.getArgument("?color").toString();

            try {
                callMethod("setSurveyTextColor", color);
            } catch (ADEException re) {
                log.error("Error sending to Survey: " + re);
            }
        } else if (action.isA("SurveyGeometry")) {
            // Change survey text color
            int x = action.getInteger("?x");
            int y = action.getInteger("?y");

            try {
                callMethod("setSurveyGeometry", x, y);
            } catch (ADEException re) {
                log.error("Error sending to Survey: " + re);
            }
        } else if (action.isA("HandDemo")) {
            try {
                callMethod(0, "runDemo");
            } catch (ADEException ace) {
                log.error("Error sending to Hand: " + ace);
            }
        } else if (action.isA("getBlocks")) {
            Object blocks = null;
            boolean status = false;
            if (checkMethod("getBlocks")) {
                try {
                    blocks = callMethod("getBlocks");
                    status = true;
                } catch (ADEException ace) {
                    log.error("getBlocks: error sending to sim: " + ace);
                }
            }
            action.setArgument("?blocklist", blocks);
            action.setExitStatus(status);
        } else if (action.isA("getBoxes")) {
            Object boxes = null;
            boolean status = false;
            if (checkMethod("getBoxes")) {
                try {
                    boxes = callMethod("getBoxes");
                    status = true;
                } catch (ADEException ace) {
                    log.error("getBoxes: error sending to sim: " + ace);
                }
            }
            action.setArgument("?boxlist", boxes);
            action.setExitStatus(status);
        } else if (action.isA("openDoor")) {
            String d = (String)action.getArgument("?door");
            boolean s = false;
            try {
                s = (Boolean)callMethod("openDoor", d);
            } catch (ADEException re) {
                log.error("Error sending open to robot: " + re);
            }
            action.setExitStatus(s);
        } else if (action.isA("openBox")) {
            Object b = action.getArgument("?box");
            boolean s = false;
            try {
                s = (Boolean)callMethod("openBox", b);
            } catch (ADEException re) {
                log.error("Error sending open to robot: " + re);
            }
            log.debug("openBox status: "+s);
            action.setExitStatus(s);
        } else if (action.isA("closeBox")) {
            Object b = action.getArgument("?box");
            boolean s = false;
            try {
                s = (Boolean)callMethod("closeBox", b);
            } catch (ADEException re) {
                log.error("Error sending close to robot: " + re);
            }
            action.setExitStatus(s);
        } else if (action.isA("get-from")) {
            Object b = action.getArgument("?box");
            Object k = action.getArgument("?block");
            boolean s = false;
            try {
                s = (Boolean)callMethod("getFromBox", b, k);
            } catch (ADEException re) {
                log.error("Error sending get-from to robot: " + re);
            }
            action.setExitStatus(s);
        } else if (action.isA("put-into")) {
            Object b = action.getArgument("?box");
            Object k = action.getArgument("?block");
            boolean s = false;
            try {
                s = (Boolean)callMethod("putIntoBox", b, k);
            } catch (ADEException re) {
                log.error("Error sending put-into to robot: " + re);
            }
            action.setExitStatus(s);
        } else if (action.isA("giveObjectTo")) {
            // vision token ID-based method
            Long id = action.getLong("?id");
            boolean s = false;
            method = "giveObjectTo";
            if (checkMethod(method)) {
                try {
                    s = (Boolean)callMethod(method, id);
                } catch (ADEException re) {
                    log.error("Error sending " + method + " to robot: " + re);
                }
            } else {
                log.error("Unable to " + action.getType());
            }
            action.setExitStatus(s);
        } else if (action.isA("giveObjectToRequest")) {
            // vision token ID-based method
            Long id = action.getLong("?id");
            boolean s = false;
            log.debug("!!!!!!!!! SEND SEMANTICS FOR REQUEST TO TAKE !!!!!!!!!!");
            /*
            method = "giveObjectTo";
            if (checkMethod(method)) {
                try {
                    s = (Boolean)callMethod(method, id);
                } catch (ADEException re) {
                    log.error("Error sending " + method + " to robot: " + re);
                }
            } else {
                log.error("Unable to " + action.getType());
            }
            */
            action.setExitStatus(s);
        } else if (action.isA("lookAtWeapon")) {
            method = "lookAtWeapon";
            if (checkMethod(method)) {
                try {
                    callMethod(0, method);
                } catch (ADEException re) {
                    log.error("Error sending lookAtWeapon to robot: " + re);

                }
            } else {
                log.error("Unable to " + action.getType());
            }
        } else if (action.isA("relaxArms")) {
            method = "relaxArms";
            if (checkMethod(method)) {
                try {
                    callMethod(0, method);
                } catch (ADEException re) {
                    log.error("Error sending relaxArms to robot: " + re);

                }
            } else {
                log.error("Unable to " + action.getType());
            }
        } else if (action.isA("startPickUpWeapon")) {
            method = "pickUpWeapon";
            if (checkMethod(method)) {
                try {
                    callMethod(0, method);
                } catch (ADEException re) {
                    log.error("Error sending pickUpWeapon to robot: " + re);

                }
            } else {
                log.error("Unable to " + action.getType());
            }
        } else if (action.isA("startPickUpObject")) {
            // vision token ID-based method
            Long id = -1L;
            Long actionId = -1L;
            boolean s = false;
            method = "pickUpObject";
            String objName = (String)action.getArgument("?obj");
            if (objName.startsWith("visiontoken")) {
                id = Long.parseLong(objName.substring(11));
                log.debug("pickUpObject id: " + id);
            } else {
                log.error("Unrecognized object id: " + objName);
                action.setExitStatus(s);
                return;
            }
            if (checkMethod(method)) {
                try {
                    actionId = (Long)callMethod(method, id);
                    if (actionId >= 0) {
                        s = true;
                    }
                } catch (ADEException re) {
                    log.error("Error sending pickUpObject to robot: " + re);

                }
            } else {
                log.error("Unable to " + action.getType());
            }
            action.setArgument("?id", actionId);
            action.setExitStatus(s);
        } else if (action.isA("putDownObject")) {
            // vision token ID-based method
            Long id = action.getLong("?id");
            boolean s = false;
            method = "putDownObject";
            if (checkMethod(method)) {
                try {
                    s = (Boolean)callMethod(method, id);
                } catch (ADEException re) {
                    log.error("Error sending putDownObject to robot: " + re);
                }
            } else {
                log.error("Unable to " + action.getType());
            }
            action.setExitStatus(s);
        } else if (action.isA("checkAction")) {
            method = "checkAction";
            Long actionID = action.getLong("?actionID");
            ActionStatus status;

            if (checkMethod(method, action)) {
                try {
                    status = (ActionStatus)callMethod(method, actionID);
                    // PWS: This ignores the failure state...
                    if (status == ActionStatus.SUCCESS)
                        action.setExitStatus(true);
                    else
                        action.setExitStatus(false);
                } catch (ADEException ace) {
                    log.error("checkAction: Error querying action status:" + ace);
                }
            } else {
                log.error(myID + ": no reference, can't " + action.getType());
                action.setExitStatus(false);
            }
        } else if (action.isA("checkActionDetail")) {
            method = "checkAction";
            Long actionID = action.getLong("?actionID");
            ActionStatus status;

            if (checkMethod(method, action)) {
                try {
                    status = (ActionStatus)callMethod(method, actionID);
                    if (status == ActionStatus.PROGRESS || status == ActionStatus.SUSPEND) {
                        action.setExitStatus(false);
                    } else {
                        if (status == ActionStatus.SUCCESS) {
                            action.setArgument("?actionStatus", "success");
                        }
                        action.setExitStatus(true);
                    }
                } catch (ADEException ace) {
                    log.error("checkAction: Error querying action status:" + ace);
                }
            } else {
                log.error(myID + ": no reference, can't " + action.getType());
                action.setArgument("?actionStatus", "unknown");
                action.setExitStatus(false);
                action.setArgument("?actionStatus", "failure");
            }
        } else if (action.isA("loadFODDDomain")) { // change FODD domain, etc.
            String domain = (String)action.getArgument("?domain");
            String policy = (String)action.getArgument("?policy");
            String id = "error";

            try {
                id = (String)callMethod("loadDomain", domain, policy);
            } catch (ADEException re) {
                log.error("Error sending to planner", re);
            }
            action.setArgument("?id", id);
        } else if (action.isA("plannerUpdate")) { // send update to planner
            String cond = (String)action.getArgument("?cond");
            cond = bindText(action, cond.trim());
            String[] updates = cond.split(":");
            ArrayList<Predicate> p = new ArrayList<Predicate>();
            for (int i=0; i < updates.length; i++) {
                if (updates[i].length() < 1)
                    continue;
                Predicate u = createPredicate(updates[i]);
                p.add(u);
            }
            log.trace(format("[doMiscPrimitive::plannerUpdate] Updating state: %s", p));
            ami.processStateUpdate(p, true);
        } else if (action.isA("plannerGoal")) {
            String goals = (String)action.getArgument("?goals");
            goals = bindText(action, goals.trim()).replaceAll(":", ",");
            log.trace(format("[doMiscPrimitive::plannerGoal] Send goals: %s", goals));
            GoalManagerGoal gmGoal = new GoalManagerGoal(createPredicate(goals));
            ami.plannerGoal(gmGoal);
        } else if (action.isA("cancelAction")) {
            String a = (String)action.getArgument("?aname");
            a = bindText(action, a);
            ami.cancelAction(a);
        } else if (action.isA("getNearestDoor")) {
            double d = Double.MAX_VALUE;
            double x = 0.0, y = 0.0, ex = 0.0, ey = 0.0, ax = 0.0, ay = 0.0;
            Door mydoor;
            boolean found = false;
            mydoor = ami.detectNearestDoor();
            if (mydoor != null) {
                x = mydoor.getEntry().getX();
                y = mydoor.getEntry().getY();
                ex = mydoor.getExit().getX();
                ey = mydoor.getExit().getY();
                ax = mydoor.getApproach().getX();
                ay = mydoor.getApproach().getY();
                d = getDistanceFrom(0, 0, x, y);
                //if (d < 2.0) {
                    //log.debug("Got door at " + x + ", " + y + ", " + d);
                    //ed = getDistanceFrom(0, 0, ex, ey);
                    //log.debug("Got inside at " + ex + ", " + ey + ", " + ed);
                    // Only set these if a door's found
                    found = true;
                //}
            }
            // Set args regardless
            action.setArgument("?x", x);
            action.setArgument("?y", y);
            action.setArgument("?ex", ex);
            action.setArgument("?ey", ey);
            action.setArgument("?ax", ax);
            action.setArgument("?ay", ay);
            action.setArgument("?dist", d);
            action.setExitStatus(found);
        } else if (action.isA("activate-light")) {
            method = "TurnOnLight";
            if (checkMethod(method, action)) {
                try {
                    callMethod(method);
                } catch (ADEException ace) {
                    log.error("Failed to activate light", ace);
                }
            } else {
                log.warn("Would activate light");
            }
        } else if (action.isA("deactivate-light")) {
            method = "TurnOffLight";
            if (checkMethod(method, action)) {
                try {
                    callMethod(method);
                } catch (ADEException ace) {
                    log.error("Failed to deactivate light",ace);
                }
            } else {
                log.warn("Would deactivate light");
            }
        } else if (action.isA("activate-grabber")) {
            method = "MagnetOnSnatch";
            if (checkMethod(method, action)) {
                try {
                    /* old, awesome-scary grabber
                    callMethod("TurnOnChannel", 4, 7);
                    Sleep(150);
                    callMethod("TurnOffChannel", 4);
                    Sleep(500);
                    //callMethod("TurnOnChannel", 1, 4);
                    Sleep(1500);
                    callMethod("TurnOnChannel", 4, 7);
                    */
                    /* new, awesome-fancy grabber */
                    // drop magnet
                    callMethod("MagnetDown", 600);
                    // magnet on
                    callMethod("MagnetOnSnatch");
                    Sleep(5000);
                    // motor on
                    callMethod("ReelMagnetUp", 800, 6);
                    // reduce motor voltage
                    callMethod("MagnetUpHold", 3);
                    // reduce magnet voltage
                    callMethod("MagnetOnHold");
                } catch (ADEException ace) {
                    log.error("Failed to activate grabber", ace);
                }
            } else {
                log.warn("Would activate grabber");
            }
        } else if (action.isA("deactivate-grabber")) {
            method = "ReelMagnetUp";
            if (checkMethod(method, action)) {
                try {
                    /* old, awesome-scary grabber
                    callMethod("TurnOffChannel", 3);
                    */
                    /* new, awesome-fancy grabber */
                    // drop magnet
                    callMethod("MagnetDown", 600);
                    // magnet off
                    callMethod("MagnetOff");
                    Sleep(1000);
                    // motor on
                    callMethod("ReelMagnetUp", 800, 5);
                    // reduce motor voltage
                    callMethod("MagnetUpHold", 3);
                } catch (ADEException ace) {
                    log.error("Failed to deactivate grabber",ace);
                }
            } else {
                log.warn("Would deactivate grabber");
            }
        } else if (action.isA("getAttend")) {
            int a;
            String t = (String)action.getArgument("?type");
            Boolean v = false;

            if (t.equalsIgnoreCase("obst"))
                a = GoalManagerImpl.OBST;
            else if (t.equalsIgnoreCase("blob"))
                a = GoalManagerImpl.BLOB;
            else if (t.equalsIgnoreCase("door"))
                a = GoalManagerImpl.DOOR;
            else if (t.equalsIgnoreCase("box"))
                a = GoalManagerImpl.BOX;
            else if (t.equalsIgnoreCase("block"))
                a = GoalManagerImpl.BLOCK;
            else if (t.equalsIgnoreCase("land"))
                a = GoalManagerImpl.LAND;
            else
                a = -1;
            v = ami.getAttend(a);
            action.setArgument("?value",v.toString());
        } else if (action.isA("setAttend")) {
            int a;
            String t = (String)action.getArgument("?type");
            String v = (String)action.getArgument("?value");

            if (t.equalsIgnoreCase("obst"))
                a = GoalManagerImpl.OBST;
            else if (t.equalsIgnoreCase("blob"))
                a = GoalManagerImpl.BLOB;
            else if (t.equalsIgnoreCase("door"))
                a = GoalManagerImpl.DOOR;
            else if (t.equalsIgnoreCase("box"))
                a = GoalManagerImpl.BOX;
            else if (t.equalsIgnoreCase("block"))
                a = GoalManagerImpl.BLOCK;
            else if (t.equalsIgnoreCase("land"))
                a = GoalManagerImpl.LAND;
            else
                a = -1;
            if (a > 0)
                ami.setAttend(a, v.equalsIgnoreCase("true"));
            else
                action.setExitStatus(false);
        } else if (action.isA("setLandmark")) {
            String n = (String)action.getArgument("?name");
            String t = (String)action.getArgument("?type");
            Double x = action.getDouble("?x");
            Double y = action.getDouble("?y");
            ADEPercept a = new ADEPercept(n, t, 0.0, 0.0, x, y, x, y, 0.0, 0.0);

            pastPercepts.put(n.toUpperCase(), a);
        } else if (action.isA("checkMethods")) {
            methodFail = true; // strict method checking
        } else if (action.isA("simulateFirst")) {
            ami.setSimulateFirst(true); // strict method checking
        } else if (action.isA("makePredicate")) {
            String pSpec = (String)action.getArgument("?predspec");
            if (pSpec.charAt(0) == '"')
                pSpec = pSpec.substring(1, pSpec.length() - 1);

            ArrayList<Symbol> pargs = new ArrayList<Symbol>();
            StringTokenizer PSTok = new StringTokenizer(pSpec, " ");
            String token;

            String pName = PSTok.nextToken();
            Object pArg;
            while (PSTok.hasMoreTokens()) {
                token = PSTok.nextToken();
                if ((token.charAt(0) == '?') || (token.charAt(0) == '!')) {
                    pArg = action.getArgument(token);
                    if (pArg == null) {
                        pArg = action.getCallerArgument(token);
                    }
                    if (pArg  instanceof Symbol) {
                        pargs.add((Symbol)pArg);
                    } else {
                        pargs.add(new Symbol(pArg.toString()));
                    }
                } else {
                    pargs.add(new Symbol(token));
                }
            }
            Predicate p = new Predicate(pName, pargs);
            action.setArgument("?pred", p);
        } else if (action.isA("getTeammateLoad")) {
            Double[] l = null;
            Double load = 0.0;
            method = "getInputs";
            if (checkMethod(method, action)) {
                try {
                    l = (Double [])callMethod(method);
                    load = l[0];
                } catch (ADEException re) {
                    log.error("Error sending to FNIR", re);
                    action.setExitStatus(false);
                }
            } else {
                log.error("Can't do reading: no FNIR!");
                action.setExitStatus(false);
            }
            action.setArgument("?load", load);
        } else if (action.isA("writeFile")) {
            String filename = (String)action.getArgument("?filename");
            String message = (String)action.getArgument("?message");
            message = bindText(action, message);
            if (message.charAt(0) == '"')
                message = message.substring(1, message.length() - 1);
            log.debug("writing to file: " + message);
            try {
                PrintWriter out = new PrintWriter((new BufferedWriter(new FileWriter(filename))));
                out.println(message);
                out.close();
            } catch (IOException ioe) {
                log.debug("writeFile: " + ioe);
            }
        } else if (action.isA("informSpexStart")) {
            String id = (String)action.getArgument("?id");
            method = "holdUpdates";
            if (checkMethod(method, action)) {
                try {
                    callMethod(method);
                } catch (ADEException ace) {
                    log.error("Error informing Spex of " + id + " search start", ace);

                }
            } else {
                log.warn("Would notify spex: " + id);
            }
        } else if (action.isA("informSpexEnd")) {
            String id = (String)action.getArgument("?id");
            method = "notifySpexAtEndpoint";
            if (checkMethod(method, action)) {
                try {
                    callMethod(method);
                } catch (ADEException ace) {
                    log.error("Error informing Spex of " + id + " search end", ace);

                }
            } else {
                log.warn("Would notify spex: " + id);
            }
        } else if (action.isA("acquireLock")) {
            String lock = (String)action.getArgument("?lock");
            // acquire for the caller
            if (! action.caller.acquireLock(lock, this)) {
                log.error("Could not acquire " + lock);
                action.setExitStatus(false);
            }
        } else if (action.isA("releaseLock")) {
            String lock = (String)action.getArgument("?lock");
            action.caller.releaseLock(lock, this);
        } else if (action.isA("inConversation")) {
            boolean inConversation = false;
            method = "inConversation";
            if (checkMethod(method, action)) {
                try {
                    inConversation = (Boolean)callMethod(method);
                } catch (ADEException ace) {
                    log.error("Error checking conversation status",ace);
                }
            } else {
                //log.warn("No way to check conversation, assuming false");
            }
            ami.setConversationState(inConversation, this);
            action.setExitStatus(inConversation);
        } else if (checkMethod(action.getType(), action)) {
            try {
                callMethod(action.getType());
            } catch (ADEException ace) {
                log.error(action.getType() + ": error",ace);
            }
        } else {
            log.error("Unrecognized misc primitive: " + action.getType());
        }
    }

    private boolean setTV(double tv) {
        boolean r;
        try {
            r = (Boolean)callMethod("setTV", tv);
            curTV = tv;
        } catch (Exception e) {
            log.error("setTV: Error sending to Vel",e);
            return false;
        }
        return r;
    }

    private boolean setRV(double rv) {
        boolean r;
        try {
            r = (Boolean)callMethod("setRV", rv);
            curRV = rv;
        } catch (Exception e) {
            log.error("setRV: Error sending to Vel: ",e);
            return false;
        }
        return r;
    }

    private boolean setVels(double tv, double rv) {
        boolean r;
        //log.debug("setVels " + tv + " " + rv);
        try {
            r = (Boolean)callMethod("setVels", tv, rv);
            curTV = tv;
            curRV = rv;
        } catch (Exception e) {
            log.error("setVels: Error sending to Vel: "+ e);
            return false;
        }
        return r;
    }

    private boolean sayText(String text) {        
        return sayText(text, null);
    }

    private boolean sayText(String text, ActionDBEntry action) {
        if (checkMethod("sayText", action)) {
            log.debug("calling sayText in AM");
            System.out.println("SAYING: "+text);
            return ami.sayText(text);
        } else {
            System.out.println("saytext not available. text was: "+text);
            log.error("sayText not available for: " + text);
        }
        return false;
    }

    protected boolean checkMethod(String mn) {
        return checkMethod(mn, null);
    }

    protected boolean checkMethod(String mn, ActionDBEntry action) {
        Object ref = ami.getMethodRef(mn);
        boolean st = ami.checkComponentStatus(ref, mn);
        if (!st) {
            failCond = new Predicate("unable", mn+":action");
            if (methodFail && action != null) { // stringent method checking
                action.setExitStatus(false);
                action.exit(false);
                log.error("Goal " + goal.getGoalId() + " failed because of missing method " + mn);
            }
        } else {
            failCond = null;
        }
        return st;
    }

    protected Object callMethod(String mn, Object... args) throws ADEException, ADETimeoutException, ADEReferenceException {
        /*
        if (mn.equals("updatePlannerState")) {
            StackTraceElement e = new Exception().getStackTrace()[1];
            log.debug("Calling updatePlannerState from " + e.getFileName() + ":" + e.getLineNumber());
        }
        */
        return ami.callMethod(mn, args);
    }

    protected Object callMethod(int timeout, String mn, Object... args) throws ADEException, ADETimeoutException, ADEReferenceException {
        return ami.callMethod(timeout, mn, args);
    }

    /**
     * Execute one cycle of the action interpreter.  This is currently
     * equivalent to executing one primitive (i.e., progressing through the
     * script until a primitive is found).
     */
    protected ActionDBEntry runCycle(ActionDBEntry lastaction) {
        ActionDBEntry action = getNextAction(lastaction);
        //System.out.println("next action: "+action.getType());
        if (action == null) {
            log.warn("Current action is null. Exiting...");
            halt();
        } else if (action.isA("motionPrimitive")) {
            doMotionPrimitive(action);
        } else if (action.isA("visionPrimitive")) {
            doVisionPrimitive(action);
        } else if (action.isA("speechProdPrimitive")) {
            doSpeechProdPrimitive(action);
        } else if (action.isA("speechRecPrimitive")) {
            doSpeechRecPrimitive(action);
        } else if (action.isA("miscPrimitive")) {
            doMiscPrimitive(action);
        } else if (!action.isA("primitive")) {
            if (checkMethod(action.getType(), action)) {
                // POSSIBLE HACK!  what's "must've had to wait" mean?
                doMiscPrimitive(action);
            } else {
                //log.debug(action.getType() + ": not a primitive (must've had to wait)");
            }
        } else {
            log.warn("Unrecognized action: " + action.getType());
        }
        // PWS: should abort script when subscript fails; right now I'm only using
        //      eventStatus for most recent action (i.e., could be overwritten)
        if (action != null) {
            ArrayList<Predicate> upd = action.updateEffects(); // POSTCOND
            if (!upd.isEmpty()) {
                String tmpspec = action.getType() + " " + action.listVariables();
                ami.processStateUpdate(upd, false, tmpspec);
            }
        }
        return action;
    }

    /**
     * <code>getNextAction</code> selects the next action for execution by the
     * client.
     * @return an action for the client to execute
     */
    protected ActionDBEntry getNextAction(ActionDBEntry lastaction) {

        ActionDBEntry action, sub;
        ArrayList<String> spec;
        boolean exitStatus;

        if (lastaction != null) {
            lastaction.releaseLocks(this);
            // Too much noise to describe each primitive before and after
            // describe(lastaction, "end");
        }
        if (ActionStack.empty()) {
            return null;
        }
        action = ActionStack.pop();
        // While it's a script, if there's another event, push the script and
        // instantiate the sub event.
        while (!action.isA("primitive")) {
            currentAction = action;
            //log.debug("Ready to perform action: " + action.getType());
            // Check here for timeout
            if ((action.getTimeout() > 0) &&
                    (action.getElapsedTime() > action.getTimeout())) {
                log.error("Timeout: " + action.getType());
                System.out.append("    " + action.getElapsedTime() + " " + action.getTimeout());
                action.releaseLocks(this);
                action.setExitStatus(false);
                describe(action, "failed");
                if (ActionStack.empty()) {
                    return null;
                }
                action = ActionStack.pop();
                continue;
            }
            spec = action.getEventSpec();
            if (spec == null) {
                action.releaseLocks(this);
                describe(action, "end");
                logIt(cmd + " COMPLETE " + action.getName());
                if (ActionStack.empty()) {
                    return null;
                }
                // this is the end of the current script, need to accrue effects
                // PWS: should abort script when subscript fails; right now I'm only using
                //      eventStatus for most recent action (i.e., could be overwritten)
                ArrayList<Predicate> upd = action.updateEffects(); // POSTCOND
                if (!upd.isEmpty()) {
                    String tmpspec = action.getType() + " " + action.listVariables();
                    ami.processStateUpdate(upd, true, tmpspec);
                }
                action = ActionStack.pop();
            } else if (spec.get(0).equals("if")) {
                // add the IF statement to the calling script
                sub = addTask(spec, action);
                // inherit the caller's arguments
                sub.inheritArguments();
                // copy the condition's eventspecs to the IF statement; the
                // caller's pc is incremented, so it will not execute these
                // events--that's the responsibility of the IF statement now.
                spec = action.getEventSpec();
                // TODO: Check for empty condition (automatic failure?)
                while (!spec.get(0).equals("then")) {
                    sub.addEventSpec(spec);
                    spec = action.getEventSpec();
                }
                // Add the call to check the status once the condition has
                // been executed.
                sub.addEventSpec(spec);
                // Push caller onto stack.
                ActionStack.push(action);
                action = sub;
            } else if (spec.get(0).equals("then")) {
                exitStatus = action.getEventExitStatus();
                // Then pop the parent of the IF statement
                //log.debug("THEN: exitStatus is " + exitStatus);
                action = ActionStack.pop();
                if (!exitStatus) {
                    // TODO: check for both IF and WHILE to make sure they're
                    // properly enclosed
                    spec = action.getEventSpec("endif else elseif", "if",
                            "endif");
                    //log.debug("THEN: spec is " + spec);
                    if (spec == null) {
                        // Have to do something, or the system will crap out
                        log.error("Syntax error: expecting one of " +
                                "\"endif else elseif\"");
                    } else if (spec.get(0).equals("elseif")) {
                        sub = addTask(spec, action);
                        // inherit the caller's arguments
                        sub.inheritArguments();
                        // copy the condition's eventspecs to the ELSEIF
                        spec = action.getEventSpec();
                        // TODO: Check for empty condition (automatic failure?)
                        while (!spec.get(0).equals("then")) {
                            sub.addEventSpec(spec);
                            spec = action.getEventSpec();
                        }
                        // Add the call to check the status once the condition
                        // has been executed.
                        sub.addEventSpec(spec);
                        // Push caller onto stack.
                        ActionStack.push(action);
                        action = sub;
                    }
                } // else go ahead and execute consequent
            } else if (spec.get(0).equals("else") ||
                    spec.get(0).equals("elseif")) {
                // If I encounter an ELSE or an ELSEIF as part of a sequence,
                // I'm executing a THEN so I should skip to the ENDIF.
                // TODO: check for stray ELSEs and ELSEIFs, as below for ENDIFs
                spec = action.getEventSpec("endif", "if", "endif");
            } else if (spec.get(0).equals("endif")) {
                // Ignore the ENDIF when encountered as part of a sequence
                // TODO: this allows stray ENDIFs; can keep an IF statement
                // counter that's incremented whenever an IF is entered and
                // decremented whenever an ENDIF is encountered.  Then, if
                // the counter <= 0, this is a stray ENDIF.
                continue;
            } else if (spec.get(0).equals("while")) {
                action.setJumpPC();
                // add the WHILE statement to the calling script
                sub = addTask(spec, action);
                // inherit the caller's arguments
                sub.inheritArguments();
                // copy the condition's eventspecs to the WHILE statement
                spec = action.getEventSpec();
                // TODO: Check for null condition (automatic failure?)
                while (!spec.get(0).equals("do")) {
                    sub.addEventSpec(spec);
                    spec = action.getEventSpec();
                }
                // Add the call to check the status once the condition has
                // been executed.
                sub.addEventSpec(spec);
                ActionStack.push(action);
                action = sub;
            } else if (spec.get(0).equals("do")) {
                exitStatus = action.getEventExitStatus();
                // Then pop the parent of the WHILE statement
                action = ActionStack.pop();
                if (!exitStatus) {
                    // TODO: check for both IF and WHILE to make sure they're
                    // properly enclosed
                    // Won't need to jump back to the WHILE statement, so
                    // discard the jumppc
                    action.discardJumpPC();
                    spec = action.getEventSpec("endwhile", "while",
                            "endwhile");
                    if (spec == null) {
                        // Have to do something, or the system will crap out
                        log.error("Syntax error: expecting " +
                                "\"endwhile\"");
                    }
                }
            } else if (spec.get(0).equals("endwhile")) {
                // Ignore the ENDWHILE when encountered as part of a sequence
                // TODO: this allows stray ENDWHILEs; can keep a WHILE
                // statement counter that's incremented whenever a WHILE is
                // entered and decremented whenever an ENDWHILE is
                // encountered.  Then, while the counter <= 0, this is a stray
                // ENDWHILE.
                action.restoreJumpPC();
                continue;
            } else if (spec.get(0).equals("not")) {
                // add the NOT statement to the calling script
                sub = addTask(spec, action);
                // inherit the caller's arguments
                sub.inheritArguments();
                // copy the argument's eventspecs to the NOT statement
                spec = action.getEventSpec();
                // TODO: Check for null condition (automatic failure?)
                while (!spec.get(0).equals("endnot")) {
                    sub.addEventSpec(spec);
                    spec = action.getEventSpec();
                }
                // Add the call to check the status once the condition has
                // been executed--in this case, the ENDNOT
                sub.addEventSpec(spec);
                // Push caller onto stack
                ActionStack.push(action);
                action = sub;
            } else if (spec.get(0).equals("endnot")) {
                exitStatus = action.getEventExitStatus();
                if (exitStatus) {
                    action.setExitStatus(false);
                } else {
                    action.setExitStatus(true);
                }
                // Then pop the parent
                action = ActionStack.pop();
            } else if (spec.get(0).equals("with")) {
                // dlocal: with vision VisionComponentImpl22
                log.warn("WARNING: dlocal not updated, reference not changing");
                if (spec.get(1).equals("vision")) {
                    // save current vsi for endwith
                    //ami.vsiStack.push(ami.vsi);
                    // Should check this...
                    //ami.vsi = ami.vsiRefs.get(spec.get(2));
                } else if (spec.get(1).equals("reddy")) {
                    log.debug("Changing to reddy ref: " + spec.get(2));
                    //ami.rsiStack.push(ami.rsi);
                    //ami.rsi = ami.rsiRefs.get(spec.get(2));
                } else if (spec.get(1).equals("discourse")) {
                    //ami.dsiStack.push(ami.dsi);
                    //ami.dsi = ami.dsiRefs.get(spec.get(2));
                } else {
                    log.error("Unrecognized server type: " + spec.get(1));
                }
            } else if (spec.get(0).equals("endwith")) {
                // dlocal: endwith vision
                log.warn("WARNING: dlocal not updated, reference not changing");
                if (spec.get(1).equals("vision")) {
                    //ami.vsi = ami.vsiStack.pop();
                } else if (spec.get(1).equals("reddy")) {
                    //ami.rsi = ami.rsiStack.pop();
                } else if (spec.get(1).equals("discourse")) {
                    //ami.dsi = ami.dsiStack.pop();
                } else {
                    log.error("Unrecognized server type: " + spec.get(1));
                }
            } else if (spec.get(0).equals("choose")) {
                ArrayList<String> tmpspec;
                spec = action.getEventSpec();
                // Look up the action to get the relevant cost, etc.
                ActionDBEntry newAction = ActionDBEntry.lookup(spec.get(0).toLowerCase());
                double b = newAction.getBenefit();
                double c = newAction.getCost();
                double a = newAction.affectEval();
                double u = a * b - c;
                log.debug("spec: " + spec + " u == " + u);
                double v;
                tmpspec = action.getEventSpec();
                // Step through the remaining choices to find the best
                while (!tmpspec.get(0).equals("endchoose")) {
                    newAction = ActionDBEntry.lookup(tmpspec.get(0).toLowerCase());
                    b = newAction.getBenefit();
                    c = newAction.getCost();
                    a = newAction.affectEval();
                    v = a * b - c;
                    log.debug("tmpspec: " + tmpspec + " u == " + v);
                    if (v > u) {
                        u = v;
                        spec = tmpspec;
                    }
                    tmpspec = action.getEventSpec();
                }
                // Add the chosen action to the calling script
                sub = addTask(spec, action);
                // inherit the caller's arguments
                sub.inheritArguments();
                // Push caller onto stack
                ActionStack.push(action);
                action = sub;
            } else if (spec.get(0).equals("endchoose")) {
                // Ignoring the ENDCHOOSE, but *should* check for syntax
            } else if (spec.get(0).equals("achieve")) {
                // get the predicate, bind it, and find an action that will achieve it
                Predicate ach = createPredicate(spec.get(1).toLowerCase());
                ach = action.bindPredicate(ach);
                log.debug("Going to achieve "+ach);
                // get the script entry from the KB
                sub = ActionDBEntry.lookupPost(ach);
                if (sub == null) {
                    log.error("No valid action found to achieve "+ach);
                } else {
                    String arg, tstring = "";
                    tstring += sub.getName() + " " + sub.listVariables();
                    ArrayList<Symbol> aargs = ach.getArgs();
                    ArrayList<Symbol> pargs;
                    ArrayList<Predicate> postconds = sub.getPostconds();
                    Predicate postcond = null;
                    for (Predicate pc:postconds) {
                        if (ActionDBEntry.predicateMatch(ach, pc)) {
                            postcond = pc;
                            break;
                        }
                    }
                    pargs = postcond.getArgs();
                    // create a taskspec string for the chosen action with the arguments substituted
                    for (int i = 0; i < pargs.size(); i++) {
                        String parg = pargs.get(i).getName();
                        String aarg = aargs.get(i).getName();
                        if (Predicate.class.isInstance(aargs.get(i))) {
                            aarg = adb.lookupFunction((Predicate)aargs.get(i)).toString();
                        }
                        tstring = tstring.replace(parg, aarg);
                    }
                    ArrayList<String> tmpspec = new ArrayList<String>();
                    tmpspec.addAll(Arrays.asList(tstring.split(" ")));
                    // Add the chosen action to the calling script
                    sub = addTask(tmpspec, action);
                    // Push caller onto stack
                    ActionStack.push(action);
                    action = sub;
                }
            } else if (spec.get(0).equals("and")) {
                exitStatus = action.getEventExitStatus();
                //log.debug("AND: exitStatus is " + exitStatus);
                if (!exitStatus) {
                    if (action.isA("if") || action.isA("elseif")) {
                        spec = action.getEventSpec("then");
                    } else if (action.isA("while")) {
                        spec = action.getEventSpec("do");
                    } else if (action.isA("not")) {
                        spec = action.getEventSpec("endnot");
                    }
                    // Have to undo the last get to evaluate exit status
                    action.ungetEventSpec();
                    //log.debug("AND: spec is " + spec);
                }
            } else if (spec.get(0).equals("or")) {
                exitStatus = action.getEventExitStatus();
                if (exitStatus) {
                    if (action.isA("if") || action.isA("elseif")) {
                        spec = action.getEventSpec("then");
                    } else if (action.isA("while")) {
                        spec = action.getEventSpec("do");
                    } else if (action.isA("not")) {
                        spec = action.getEventSpec("endnot");
                    }
                    // Have to undo the last get to evaluate exit status
                    action.ungetEventSpec();
                }
            } else if (spec.get(0).equals("return")) {
                // add the RETURN statement to the calling script
                sub = addTask(spec, action);
                // inherit the caller's arguments
                sub.inheritArguments();
                // copy the argument's eventspecs to the RETURN statement
                spec = action.getEventSpec();
                // TODO: Check for null condition (automatic failure?)
                while (!spec.get(0).equals("endreturn")) {
                    sub.addEventSpec(spec);
                    spec = action.getEventSpec();
                }
                // Add the call to check the status once the condition has
                // been executed--in this case, the ENDRETURN
                sub.addEventSpec(spec);
                // Push caller onto stack
                ActionStack.push(action);
                action = sub;
            } else if (spec.get(0).equals("endreturn")) {
                exitStatus = action.getEventExitStatus();
                action.setExitStatus(exitStatus);
                // Then pop the parent
                action = ActionStack.pop();
                action.setExitStatus(exitStatus);
                // Then pop the parent
                action = ActionStack.pop();
            } else {
                String aName = spec.get(0);
                if ((aName.charAt(0) == '?') || (aName.charAt(0) == '!')) {
                    // need to bind the action name
                    String sName = aName;
                    aName = (String)action.getArgument(sName);
                    log.debug("Setting action " + sName + " to " + aName);
                    spec.set(0, aName);
                }
                sub = addTask(spec, action);
                //System.out.println("ActionInterpreter: adding Task: spec: "+spec+", action: "+action);
                Predicate desc = sub.getPredDesc();
                // check here for badaction, badstate
                ArrayList<Predicate> postconds = sub.getPostconds();
                boolean badState = false;
                boolean skipAction = false;
                Predicate badStatePred = null;
                for (Predicate post : postconds) {
                    Predicate bpost = sub.bindPredicate(post);
                    log.debug("Checking post: "+post);
                    if (GoalManagerImpl.badState(bpost)) {
                        log.error("ERROR: "+desc+" leads to a forbidden state: "+bpost+"!");
                        badState = true;
                        badStatePred = bpost;
                        break;
                    }
                }
                if (badState) {
                    // here's where we'd send off to the moral reasoner, if there were one
                    boolean override = false;
                    if (checkMethod("askForOverride")) {
                        log.info("Asking for override of "+badStatePred+" for "+goal.getGoal());
                        try {
                            override = (Boolean)callMethod("askForOverride", badStatePred, goal.getGoal());
                            if (! override) {
                                ActionDBEntry.assertFact("badstate", "nooverride");
                            } else {
                                ActionDBEntry.assertFact("badstate", "override");
                            }
                        } catch (ADEException ace) {
                            log.error("Error asking for override: " + ace);
                        }
                    } else {
                        ActionDBEntry.assertFact("badstate", "noreasoner");
                    }
                    if (! override) {
                        log.info("Override denied");
                        ActionDBEntry subcaller = ActionDBEntry.lookup(sub.getCallerType());
                        sub.setExitStatus(false);
                        if (! subcaller.isA("syntax")) {
                            action.exit(false);
                        }
                        skipAction = true;
                    } else {
                        log.info("Override granted");
                    }
                } else if (GoalManagerImpl.badAction(sub)) {
                    log.error("ERROR: "+desc+" is a forbidden action!");
                    // here's where we'd send off to the moral reasoner, if there were one
                    boolean override = false;
                    if (checkMethod("askForOverride")) {
                        try {
                            override = (Boolean)callMethod("askForOverride", sub, goal.getGoal());
                        } catch (ADEException ace) {
                            log.error("Error asking for override: " + ace);
                        }
                    }
                    if (! override) {
                        ActionDBEntry subcaller = ActionDBEntry.lookup(sub.getCallerType());
                        sub.setExitStatus(false);
                        if (! subcaller.isA("syntax")) {
                            action.exit(false);
                        }
                        skipAction = true;
                    }
                }
                if (skipAction) {
                    // anything to do here?
                } else if (sub.acquireLocks(this)) {
                    ActionStack.push(action);
                    if (sub.isA("primitive") && !sub.getName().equals("qsleep")) {
                        describe(sub, null);
                        //log.debug(cmd + " PRIMITIVE " + sub.getName() + " BY " + action.getName());
                        logIt(cmd + " PRIMITIVE " + sub.getName() + " BY " + action.getName());
                    } else if (!sub.getName().equals("qsleep")) {
                        describe(sub, "begin");
                        //log.debug(cmd + " EXEC " + sub.getName() + " BY " + action.getName());
                        logIt(cmd + " EXEC " + sub.getName() + " BY " + action.getName());
                    }
                    action = sub;
                } else {
                    //log.error("Failed to acquire locks!  " + spec.get(0) + " failed!");
                    // PWS: I'm going to leave this as it is (i.e., it'll
                    // keep trying to get the lock) for now.
                    //sub.setExitStatus(false);
                    String owner = sub.getLockOwnerDescription();
                    sub.releaseLocks(this);
                    action.ungetEventSpec();
                    logIt(cmd + " WAIT " + action.getName() + " FOR " + owner);
                    ActionStack.push(action);
                    return action;
                }
            }
        }

        return action;
    }

    /**
     * <code>addTask</code> is the entry point for script execution; the client
     * submits a task (i.e., script) for execution, and the goal manager takes
     * care of the details of choosing actions to accomplish the task--in
     * theory, at least.
     * @param taskspec is an event specification (i.e., an action name and
     * arguments)
     * @param base is the (instantiated) script that will call this task;
     * new tasks from the action server will use the DB root, or some
     * permanent goal
     * @param push specifies whether to push the task onto the stack
     * @return the newly added task
     */
    protected final ActionDBEntry addTask(GoalManagerGoal goal, ArrayList<String> taskspec,
            ActionDBEntry base, boolean push) {
        ActionDBEntry newTask = addTask(taskspec, base);
        if ((newTask != null) && push) {
            logIt(goal.getGoal() + " EXEC " + newTask.getName() + " BY GoalManager");
            ActionStack.push(newTask);
        }

        return newTask;
    }

    /**
     * <code>addTask</code> is the entry point for script execution; the client
     * submits a task (i.e., script) for execution, and the goal manager takes
     * care of the details of choosing actions to accomplish the task--in
     * theory, at least.
     * @param taskspec is an event specification (i.e., an action name and
     * arguments)
     * @param base is the (instantiated) script that will call this task;
     * new tasks from the action server will use the DB root, or some
     * permanent goal
     * @return the newly added task
     */
    protected ActionDBEntry addTask(ArrayList<String> taskspec, ActionDBEntry base) {
        // Acquire the task prototype
        // PWS: Ought to be smarter about this, not reconstruct the event
        // every time (e.g., when the locks aren't acquired).  That will
        // also make startTime more accurate..
        // System.out.print("THE TASKSPEC: "+taskspec);
        ActionDBEntry newTask = base.addEvent(taskspec);

        if (newTask == null) {
            log.error("AGM: error adding task " + taskspec.get(0));
            return null;
        }

        return newTask;
    }

    /**
     * Get the cost of the action.
     * @return the cost
     */
    protected double getActionCost() {
        return cost;
    }

    /**
     * Get the benefit of the action.
     * @return the benefit
     */
    protected double getActionBenefit() {
        return benefit;
    }

    /**
     * Get the maximum time allowed the action.
     * @return the max time
     */
    protected long getActionMaxTime() {
        return currentAction.getTimeout();
    }

    /**
     * Get the start time for the action.
     * @return the start time
     */
    protected long getActionStartTime() {
        return currentAction.getStartTime();
    }

    /**
     * Get the maximum urgency allowed the action.
     * @return the max urgency
     */
    protected double getActionMaxUrg() {
        return maxUrgency;
    }

    /**
     * Get the minimum urgency allowed the action.
     * @return the min urgency
     */
    protected double getActionMinUrg() {
        return minUrgency;
    }

    /**
     * Get the action's current priority.
     * @return the priority
     */
    protected double getActionPriority() {
        return priority;
    }

    /**
     * Set the action's current priority.
     * @param newpri the new priority
     */
    protected void setActionPriority(double newpri) {
        priority = newpri;
    }

    protected static String bindText(ActionDBEntry action, String inText) {
        StringBuilder outText = new StringBuilder();
        Matcher m = variable.matcher(inText);
        String tmpToken = null, token = null;
        Object tokenObj = null;
        int start = 0; // start of most recent match
        int end = 0;   // end of most recent match

        while (m.find()) {
            start = m.start();
            outText.append(inText.substring(end, start));
            tmpToken = m.group();
            // Have to get the arg from the caller because the text
            // was created in that context.
            tokenObj = action.getArgument(tmpToken);
            if (tokenObj == null) {
                tokenObj = action.getCallerArgument(tmpToken);
                if (tokenObj == null) {
                    log.error(action+": unable to find " + tmpToken);
                    tokenObj = tmpToken;
                }
            }
            token = tokenObj.toString();
            // token might be bound to another string, which needs to be
            // checked again
            if (token.startsWith("\""))
                token = bindText(action, token);
            outText.append(token);
            end = m.end();
        }
        outText.append(inText.substring(end, inText.length()));
        return outText.toString();
    }

    /**
     * Print a description of an action.
     * @param action the action to describe
     * @param prefix indentation prefix
     */
    protected void describe(ActionDBEntry action, String prefix) {
        String text = action.getDescription();

        if ((descriptive == 0))
            return;
        else if ((descriptive == 1) && action.isA("primitive"))
            return;

        if (text == null) {
            //log.debug("--> someone does " + action.getType() +
            //	    " (maybe with or to something or someone else)");
        } else if (prefix == null) {
            log.debug("--> " + bindText(action, text));
        } else {
            log.debug("--> " + prefix + " " + bindText(action, text));
        }
    }

    /**
     * Update affect state
     * @param newAffect the new affect state
     * @param positive boolean indicating whether to add positive affect
     */
    protected void setAffect(double newAffect, boolean positive) {
        if (positive)
            posAff = newAffect;
        else
            negAff = newAffect;
    }

    /**
     * Update affect state
     * @param inc the increment value for the update
     * @param positive boolean indicating whether to add positive affect
     */
    protected void updateAffect(double inc, boolean positive) {
        if (positive) {
            posAff += (1 - posAff) * inc;
            //log.debug("AI " + cmd + " posAff: " + posAff);
        } else {
            negAff += (1 - negAff) * inc;
            //log.debug("AI " + cmd + " negAff: " + negAff);
        }
    }

    /**
     * Decay affect state
     * @param dec the amount to decrement by
     * @param positive boolean indicating whether to add positive affect
     */
    protected void decayAffect(double dec, boolean positive) {
        if (positive) {
            //log.debug("AI " + cmd + " posAff: " + posAff);
            posAff -= posAff * dec;
        } else {
            negAff -= negAff * dec;
            //log.debug("AI " + cmd + " negAff: " + negAff);
        }
    }

    /**
     * Get affect state
     * @param positive boolean indicating whether to add positive affect
     * @return the action's (nominal) benefit
     */
    protected double getAffect(boolean positive) {
        if (positive)
            return posAff;
        return negAff;
    }

    protected Predicate getGoal() {
        return goal.getGoal();
    }

    protected long getGoalID() {
        return goal.getGoalId();
    }

    // Global heading from (angle between) one point to another
    /**
     * Global heading from (angle between) the current point to another.
     * @param x the x coordinate of the distant point
     * @param y the y coordinate of the distant point
     * @return the heading
     */
    protected double getHeadingTo(double x, double y) {
        double h = Math.atan2((y - ami.current_y), (x - ami.current_x)) - ami.current_t;
        return h;
    }

    /**
     * The main ActionInterpreter run method.
     */
    @Override
    public void run() {
        started = true;
        shouldUpdate = true;
        long start, elapsed, remaining;
        ActionDBEntry action = null;
        boolean gotvel = true;
        synchronized (defLock) {
            if (!gotDefs && !checkMethod("getDefaultVels")) {
                //log.debug("waiting for default vels...");
                gotvel = false;
                Sleep(500);
            }
            if (!gotDefs && (gotvel || checkMethod("getDefaultVels"))) {
                //log.debug("trying...");
                try {
                    double[] defVels = (double[])callMethod(0, "getDefaultVels");
                    defTV = defVels[0];
                    defRV = defVels[1];
                    //log.debug(myID + ": got defvels");
                    gotDefs = true;
                } catch (ADEException ace) {
                    log.error(myID + ": error getting default vels!");
                }
            }
        }
        while (!initLocks && !currentAction.acquireLocks(this)) {
            //log.error("Failed to acquire locks!  "
            //	    + taskspec.get(0) + " waiting!");
            currentAction.releaseLocks(this);
            Sleep(ami.sliceTime);
        }
        describe(currentAction, "begin");
        while (shouldUpdate) {
            while (shouldPause) {
                Sleep(ami.sliceTime);
            }
            if (GoalManagerImpl.sleepAI) {
                //log.debug(cmd + ": top of loop.");
                try {
                    start = System.currentTimeMillis();
                    action = runCycle(action);
                    elapsed = System.currentTimeMillis() - start;
                    if ((remaining = ami.sliceTime - elapsed) > 0) {
                        Sleep(remaining);
                    } // else yield?
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    log.error("NullPointerException running an action interpreter cycle");
                } catch (Exception e1) {
                    e1.printStackTrace();
                    log.error("Exception running an action interpreter cycle");
                    // TODO: fix empty stack exception
                    //log.error(myID +": got generic exception", e1);
                }
            } else {
                action = runCycle(action);
                // Need to Yield here?  Shouldn't be multiple AI threads if
                // we're using this for RT captures.
            }
        }
        //log.debug("Action: ActionInterpreter " + cmd + " out of update loop (i.e., exiting)");
        ami.delAction(this);
    }

    /**
     * Pause/resume the Action Interpreter's script.
     */
    protected void pause(boolean p) {
        shouldPause = p;
    }

    /**
     * Cancel the Action Interpreter's script.  This is the only option for external classes.
     */
    protected void cancel() {
        halt(false);
    }

    /**
     * Halt the Action Interpreter.  This is called when the script has no more events.
     */
    private void halt() {
        halt(true);
    }

    /**
     * Halt the Action Interpreter.  halt() and cancel() both call this.
     * @param restartPlanner include restart flag in planner state update
     */
    private void halt(boolean restartPlanner) {
        // TODO: restartPlanner isn't used...
        if (!shouldUpdate) {
            return;
        }
        shouldUpdate = false;
        //log.debug("Action: thread halt called on ActionInterpreter " + cmd);
        if (rootAction.getEventExitStatus()) {
            rootStatus = ActionStatus.SUCCESS;
        } else {
            rootStatus = ActionStatus.FAIL;
            goal.addGoalFailCond(failCond);
        }
        if (ami.getNumActions() == 1) {
            if (checkMethod("cancelMotion")) {
                try {
                    callMethod("cancelMotion", currentMotion);
                } catch (Exception re) {
                    log.error("cancelMotion: Error sending to Motion: " + re);
                }
            }
        }
        goal.terminate(goal.getGoalStatus());
    }

    ActionInterpreterInfo getInfo() {
        ActionInterpreterInfo aii = new ActionInterpreterInfo();

        aii.agentname = agentname;
        aii.cmd = cmd;
        aii.currentAction = currentAction.getType();
        aii.status = rootStatus;
        aii.goalID = goal.getGoalId();
        aii.priority = priority;
        aii.cost = cost;
        aii.benefit = benefit;
        aii.maxUrgency = maxUrgency;
        aii.minUrgency = minUrgency;
        aii.posAff = posAff;
        aii.negAff = negAff;

        return aii;
    }

    @Override
    public String toString() {
        return super.toString() + " (" + cmd + ")";
    }

    /**
     * Try to acquire whatever locks are needed to start the script.
     * @return the success state
     */
    protected boolean acquireInitialLocks() {
        return currentAction.acquireLocks(this);
    }

    // For submitGoal goals
    protected ActionInterpreter(GoalManagerImpl goalmgr,
            GoalManagerGoal g, ActionDBEntry mot) {
        int i;
        int addedme = 0;
        ami = goalmgr;
        adb = GoalManagerImpl.adb;
        descriptive = ami.descriptive;
        motivation = mot;
        agentname = goalmgr.getAgentName();
        try {
            myID = ami.getID();
        } catch (RemoteException re) {
            log.error("Error getting ID: " + re);
        }
        // get the script entry from the KB
        ActionDBEntry task = ActionDBEntry.lookupPost(g.getGoal());
        String arg, tstring = "";
        // clone it so we can manipulate the PC, etc.
        task = (ActionDBEntry)task.clone();
        task.caller = GoalManagerImpl.adb;
        tstring += task.getName() + " ";
        ArrayList<Symbol> args = g.getGoal().getArgs();
        for (i = 0; i < args.size(); i++) {
            if (Predicate.class.isInstance(args.get(i))) {
                //log.debug("Found Predicate " + ((Predicate)g.get(i)).getName() + ", looking up");
                arg = adb.lookupFunction((Predicate)args.get(i)).toString();
            } else {
                //log.debug("Found name " + args.get(i).getName());
                arg = args.get(i).getName();
            }
            // PWS: clearly this is a hack; probably all actions should take
            // "me" as arg 0, and then servers like Sapa will have to
            // include it.  There are lots of other names that could mess
            // this up...
            if ((i == 0) && !arg.equalsIgnoreCase(agentname)) {
                log.error(myID + " WARNING: " + arg + " not agent name (" + agentname + ")");
                task.addArgument(0,agentname); // hard-coded for now
                addedme = 1;
            }
            //log.debug("Adding arg " + (i + addedme) + ": " + arg);
            task.addArgument(i + addedme, arg);
            if (i > 0)
                tstring += " ";
            tstring += arg;
        }
        for (i=i + addedme; i < task.getNumRoles(); i++) {
            // PWS: not sure this is necessary, plus it's catching locals...
            //log.debug("There are " + task.getNumRoles() + " vars, but I've only seen " + i);
            //log.debug("Adding arg " + i + ": " + task.getRoleName(i));
            task.addArgument(i, task.getRoleName(i));
        }
        //log.debug("TSTRING: " + tstring);

        ActionStack.push(task);
        rootAction = task;
        rootStatus = ActionStatus.PROGRESS;
        currentAction = rootAction;
        //
        currentAction.setStartTime(System.currentTimeMillis());
        cmd = bindText(currentAction, g.getGoal().toString());
        cost = currentAction.getCost();
        benefit = currentAction.getBenefit();
        minUrgency = currentAction.getMinUrg();
        maxUrgency = currentAction.getMaxUrg();
        currentPercepts = GoalManagerImpl.currentPercepts;
        pastPercepts = GoalManagerImpl.pastPercepts;
        goal = g;
        description = goal.getGoal().toString();
        // Add taskspec here?
        // PWS: currentAction gets started by run()
    }

    // For Planner plans
    protected ActionInterpreter(GoalManagerImpl goalmgr, GoalManagerGoal g,
            ActionDBEntry task, ActionDBEntry mot) {
        ami = goalmgr;
        adb = GoalManagerImpl.adb;
        descriptive = ami.descriptive;
        motivation = mot;
        agentname = goalmgr.getAgentName();
        try {
            myID = ami.getID();
        } catch (RemoteException re) {
            log.error("Error getting ID: " + re);
        }
        ActionStack.push(task);
        rootAction = task;
        rootStatus = ActionStatus.PROGRESS;
        currentAction = rootAction;
        cmd = bindText(currentAction, g.getGoal().toString());
        cost = currentAction.getCost();
        benefit = currentAction.getBenefit();
        minUrgency = currentAction.getMinUrg();
        maxUrgency = currentAction.getMaxUrg();
        currentPercepts = GoalManagerImpl.currentPercepts;
        pastPercepts = GoalManagerImpl.pastPercepts;
        this.goal = g;
        // Add taskspec here
        // PWS: currentAction gets started by run()
    }

    // From performAction, delay
    protected ActionInterpreter(GoalManagerImpl goalmgr, GoalManagerGoal g,
            ArrayList<String> taskspec, ActionDBEntry mot, ActionDBEntry caller) {
        ami = goalmgr;
        adb = GoalManagerImpl.adb;
        descriptive = ami.descriptive;
        motivation = mot;
        agentname = goalmgr.getAgentName();
        try {
            myID = ami.getID();
        } catch (RemoteException re) {
            log.error("Error getting ID: " + re);
        }

        rootAction = addTask(g, taskspec, caller, true);
        rootStatus = ActionStatus.PROGRESS;
        currentAction = rootAction;
        cmd = bindText(currentAction, g.getGoal().toString());
        cost = currentAction.getCost();
        benefit = currentAction.getBenefit();
        minUrgency = currentAction.getMinUrg();
        maxUrgency = currentAction.getMaxUrg();
        currentPercepts = GoalManagerImpl.currentPercepts;
        pastPercepts = GoalManagerImpl.pastPercepts;
        // add Predicate here
        ArrayList<Symbol> args = new ArrayList<Symbol>();
        for (int i = 1; i < taskspec.size(); i++)
            args.add(new Symbol(taskspec.get(i)));
        this.goal = g;
        if (currentAction.getDescription() != null)
            description = bindText(currentAction, currentAction.getDescription());
        else
            description = "do something";
        Predicate desc = rootAction.getPredDesc();
        // if (! desc.getName().equals("true")) log.debug("predDesc2: " + desc + "(" + desc.getName() + ")");
        if (GoalManagerImpl.badAction(rootAction)) {
            log.error("ERROR: "+desc+" is a forbidden action!");
            // here's where we'd send off to the moral reasoner, if there were one
            rootStatus = ActionStatus.FAIL;
            g.terminate(rootStatus);
            shouldUpdate = false;
        }
        ArrayList<Predicate> postconds = rootAction.getPostconds();
        for (Predicate post : postconds) {
            if (GoalManagerImpl.badState(post)) {
                log.error("ERROR: "+desc+" leads to a forbidden state: "+post+"!");
                // here's where we'd send off to the moral reasoner, if there were one
                rootStatus = ActionStatus.FAIL;
                g.terminate(rootStatus);
                shouldUpdate = false;
                break;
            }
        }
        // PWS: currentAction gets started by run()
    }

    // For initial actions
    protected ActionInterpreter(GoalManagerImpl goalmgr, GoalManagerGoal g,
            ArrayList<String> taskspec, ActionDBEntry mot) {
        this(goalmgr, g, taskspec, mot, GoalManagerImpl.adb);
    }
}
