/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * ADEPerceptComponent.java
 *
 * ADEPercept: percepts.
 *
 * @author Paul Schermerhorn
 *
 **/
package com.interfaces;

import java.rmi.RemoteException;
import java.util.ArrayList;

import ade.ADEComponent;

import com.ADEPercept;

/**
 * Interface to simulated percepts.  These are primarily used for Icarus.
 */
public interface ADEPerceptComponent extends ADEComponent {

    /**
     * Get Box visual percepts.
     * @return a Vector of ADEPercepts corresponding to each Box in the visual field.
     */
    public ArrayList<ADEPercept> getBoxes() throws RemoteException;

    /**
     * Get Block visual percepts.
     * @return a Vector of ADEPercepts corresponding to each Block in the visual field.
     */
    public ArrayList<ADEPercept> getBlocks() throws RemoteException;

    /**
     * Get Landmark visual percepts.
     * @return a Vector of ADEPercepts corresponding to each Landmark in the visual field.
     */
    public ArrayList<ADEPercept> getLandmarks() throws RemoteException;

    /**
     * Get visual percepts of the given color in the current visual field.
     * @param color the object color to scan for
     * @return a Vector of ADEPercepts corresponding to each object of the given
     * color in the visual field
     */
    public ArrayList<ADEPercept> getObjectsOfColor(String color) throws RemoteException;

    /**
     * Get visual percepts of the type in 360 degree radius.  This corresponds to a 
     * physical scan of the surroundings, although no actions are performed here.
     * @param type the object type to scan for
     * @return a Vector of ADEPercepts corresponding to each object of the given 
     * type that are visible from the robot's current position
     */
    public ArrayList<ADEPercept> lookFor(String type) throws RemoteException;
}
// vi:ai:smarttab:expandtab:ts=8 sw=4

