/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * MotionComponent.java
 *
 * Motion: coordinate (stateful) motions using Vel and Position servers
 *
 * @author Paul Schermerhorn
 *
 **/

package com.motion;

import ade.ADEComponent;
import com.interfaces.*;
import java.rmi.*;

/**
 * Component to monitor and control extended motion commands.
 */
public interface MotionComponent extends ADEComponent, NavigationComponent, OrientationComponent, VelocityComponent {
    /** 
     * Navigate to a relative location.  Will attempt to navigate around
     * obstacles, unlike moveTo.
     * @param xdest the x-coordinate of the destination
     * @param ydest the y-coordinate of the destination
     * @return an identifying timestamp for the move action
     */
    public long moveToRel(double xdest, double ydest) throws RemoteException;

    /** 
     * Update destination of moveToRel navigation.
     * @param aid the identifying timestamp of the action to update
     * @param xdest the x-coordinate of the destination
     * @param ydest the y-coordinate of the destination
     * @return true if the update was successful, false otherwise
     */
    public boolean updateMoveToRel(long aid, double xdest, double ydest) throws RemoteException;

    /** 
     * Move a specified distance using time instead of global pose.
     * @param dist the distance (in meters) to move
     * @return an identifying timestamp for the move action
     */
    public long timeMove(double dist) throws RemoteException;

    /** 
     * Turn toward a point
     * @param xdest x coordinate of point to turn to
     * @param ydest y coordinate of point to turn to
     * @return an identifying timestamp for the turn action
     */
    public long turnToPoint(double xdest, double ydest) throws RemoteException;

    /** 
     * Turn a specified distance using time instead of global pose.
     * @param dist the distance (in radians) to turn
     * @return an identifying timestamp for the turn action
     */
    public long timeTurn(double dist) throws RemoteException;

    /**
     * Traverse (i.e., move forward until an obstacle is detected ahead, but
     * avoiding walls, etc.).
     * @return an identifying timestamp for the move action
     */
    public long traverse() throws RemoteException;

    /** 
     * Move through a doorway
     * @param xdest the x-coordinate of the doorway
     * @param ydest the y-coordinate of the doorway
     * @return an identifying timestamp for the move action
     */
    public long moveThrough(double xdest, double ydest, double xdest1, double ydest1) throws RemoteException;

    /** 
     * Approach visual referent
     * @param key the Vision reference key to approach
     * @return an identifying timestamp for the approach action
     */
    public long approachVisRef(Long key) throws RemoteException;

    /** 
     * Approach visual referent
     * @param key the Vision reference key to approach
     * @param server the Vision server to query
     * @return an identifying timestamp for the approach action
     */
    public long approachVisRef(Long key, String server) throws RemoteException;

    /** 
     * Approach visual color
     * @param color the Vision color to approach
     * @return an identifying timestamp for the approach action
     */
    public long approachVisColor(String color) throws RemoteException;

    /** 
     * Approach visual color
     * @param color the Vision color to approach
     * @param server the Vision server to query
     * @return an identifying timestamp for the approach action
     */
    public long approachVisColor(String color, String server) throws RemoteException;

    /**
     * Get approach tolerance (how closely the robot must approach)
     * @return the current tolerance distance
     */
    public double getTolerance() throws RemoteException;

    /**
     * Set approach tolerance (how closely the robot must approach)
     * @param dist the new tolerance distance
     */
    public void setTolerance(double dist) throws RemoteException;

    /**
     * Restore given Motion.
     * @param aid the identifying timestamp of the action to restore
     * @return true if action was restored, false otherwise (i.e., if that
     * action ID was unknown)
     */
    public boolean restoreMotion(long aid) throws RemoteException;

    /**
     * Get ID of current Motion.
     * @return action ID of current motion, or -1 if none
     */
    public long getCurrentMotion() throws RemoteException;

    /**
     * Return a three-element array of (x, y, theta) position.
     * @return A three-element <tt>double</tt> array (x,y,t)
     * @throws RemoteException If an error occurs
     */
    public double[] getPoseEgo() throws RemoteException;

    /**
     * Suspend current Motion.
     * @param aid the identifying timestamp of the action to suspend
     * @return true if action was suspended, false otherwise (i.e., if that
     * action ID was not active)
     */
    public boolean suspendMotion(long aid) throws RemoteException;
}
// vi:ai:smarttab:expandtab:ts=8 sw=4
