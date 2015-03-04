/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * OrientationComponent.java
 *
 * Mot: motion-related commands (stateful)
 *
 * @author Paul Schermerhorn
 *
 **/
package com.interfaces;

import ade.ADEComponent;
import com.ActionStatus;
import java.rmi.*;

/**
 * Interface for complex/stateful motion commands (e.g., move to a location,
 * turn a given amount).
 */
public interface OrientationComponent extends ADEComponent {
    /** 
     * Turn to a global heading.
     * @param tdest the global heading (in radians) to which to turn
     * @return an identifying timestamp for the turn action
     */
    public long turnTo(double tdest) throws RemoteException;

    /** 
     * Turn a specified distance.
     * @param dist the distance (in radians) to turn
     * @return an identifying timestamp for the turn action
     */
    public long turnDist(double dist) throws RemoteException;

    /** 
     * Check status of current Motion.  Soon this will be obsoleted, as the
     * motion commands will notify Action of their completions.
     * @param aid the identifying timestamp of the action to check
     * @return the status found for the indicated action
     */
    public ActionStatus checkMotion(long aid) throws RemoteException;

    /**
     * Cancel current Motion.
     * @param aid the identifying timestamp of the action to cancel
     * @return true if action was canceled, false otherwise (i.e., if that
     * action ID was not active)
     */
    public boolean cancelMotion(long aid) throws RemoteException;
}
// vi:ai:smarttab:expandtab:ts=8 sw=4
