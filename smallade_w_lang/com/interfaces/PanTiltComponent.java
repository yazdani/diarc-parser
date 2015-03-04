/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * PanTiltComponent.java
 *
 * PanTilt: PTU-related commands
 *
 * @author Paul Schermerhorn
 *
 **/
package com.interfaces;

import ade.ADEComponent;
import java.rmi.*;

/**
 * Minimal interface for pan-tilt servers.
 */
public interface PanTiltComponent extends ADEComponent {

    /** Moves the unit in the horizontal plane to an absolute position.
     * @param deg the pan (horizontal) position in degrees */
    public void pan(int deg) throws RemoteException;

    /** Moves the unit in the horizontal plane to a relative position.
     * @param deg the amount to pan (horizontal) in degrees. */
    public void panRelative(int deg) throws RemoteException;

    /** Moves the unit in the vertical plane to an absolute position.
     * @param deg the tilt (vertical) position in degrees */
    public void tilt(int deg) throws RemoteException;

    /** Moves the unit in the vertical plane to a relative position.
     * @param deg the amount to tilt (vertical) in degrees. */
    public void tiltRelative(int deg) throws RemoteException;

    /** Combines pan and tilt operations to an absolute position.
     * @param pan the pan (horizontal) position in degrees
     * @param tilt the tilt (vertical) position in degrees */
    public void panTilt(int pan, int tilt) throws RemoteException;

    /** Combines pan and tilt operations to a relative position.
     * @param pan the amount to pan (horizontal) in degrees
     * @param tilt the amount to tilt (vertical) in degrees */
    public void panTiltRelative(int pan, int tilt) throws RemoteException;

    /** Returns the current pan (horizontal) position.
     * @return the current pan */
    public int getPan() throws RemoteException;

    /** Returns the current tilt (vertical) position.
     * @return the current tilt */
    public int getTilt() throws RemoteException;

    /**
     * set pan speed.
     * @param speed the new pan speed
     */
    public void setPanSpeed(int speed) throws RemoteException;

    /**
     * set tilt speed.
     * @param speed the new tilt speed
     */
    public void setTiltSpeed(int speed) throws RemoteException;

    /**
     * get the current pan speed.
     * @return the current pan speed
     */
    public int getPanSpeed() throws RemoteException;

    /**
     * get the current tilt speed.
     * @return the current tilt speed
     */
    public int getTiltSpeed() throws RemoteException;
}
// vi:ai:smarttab:expandtab:ts=8 sw=4

