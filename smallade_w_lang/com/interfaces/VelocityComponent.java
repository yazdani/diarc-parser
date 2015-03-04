/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * VelocityComponent.java
 *
 * Vel: velocity-related commands (stateless)
 *
 * @author Paul Schermerhorn
 *
 **/
package com.interfaces;

import ade.ADEComponent;
import java.rmi.*;

/**
 * Minimal interface for setting and getting velocities.
 */
public interface VelocityComponent extends ADEComponent {

    /**
     * Get translational velocity.
     * @return the most recent TV reading (m/sec).
     */
    public double getTV() throws RemoteException;

    /**
     * Get rotational velocity.
     * @return the most recent RV reading (rad/sec).
     */
    public double getRV() throws RemoteException;

    /**
     * Get translational and rotational velocity.
     * @return the most recent velocity readings (m/sec and rad/sec).
     */
    public double[] getVels() throws RemoteException;

    /**
     * Get the default velocities used by VelocityComponent functions.
     * @return the default velocities (m/sec and rad/sec).
     */
    public double[] getDefaultVels() throws RemoteException;

    /**
     * Stop.
     */
    public void stop() throws RemoteException;

    /**
     * Set translational velocity.
     * @param tv the new TV (m/sec)
     * @return true if there's nothing in front of the robot, false
     * otherwise.
     */
    public boolean setTV(double tv) throws RemoteException;

    /**
     * Set rotational velocity.
     * @param rv the new RV (rad/sec)
     * @return true if there's nothing on that side, false otherwise.
     */
    public boolean setRV(double rv) throws RemoteException;

    /**
     * Set both velocities.
     * @param tv the new TV (m/sec)
     * @param rv the new RV (rad/sec)
     * @return true if there's nothing in front of the robot, false
     * otherwise.
     */
    public boolean setVels(double tv, double rv) throws RemoteException;
    /**
     *Returns left and right wheel velocities.
     *
    public double[] getVelocity() throws RemoteException;
     */

    /**
     * Return a three-element array of (x, y, theta) position.
     * @return A three-element <tt>double</tt> array (x,y,t)
     * @throws RemoteException If an error occurs
    public double[] getPoseEgo() throws RemoteException;
     */
}
// vi:ai:smarttab:expandtab:ts=8 sw=4

