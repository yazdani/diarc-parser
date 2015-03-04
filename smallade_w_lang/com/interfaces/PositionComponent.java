/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * PositionComponent.java
 *
 * Pos: position-related commands
 *
 * @author Paul Schermerhorn
 *
 **/
package com.interfaces;

import ade.ADEComponent;
import java.rmi.*;

/**
 * Minimal interface for position servers, such as localizers, GPS servers,
 * simulators.
 */
public interface PositionComponent extends ADEComponent {

    /**
     * Get current location.
     * @return the current location, as indicated by the Player localizer or
     * the GPS server; the pose is x,y (in meters) and theta (in radians)
     */
    public double[] getPoseGlobal() throws RemoteException;
}
// vi:ai:smarttab:expandtab:ts=8 sw=4

