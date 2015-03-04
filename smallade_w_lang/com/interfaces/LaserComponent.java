/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * LaserComponent.java
 *
 * Las: laser range-finder commands
 *
 * @author Paul Schermerhorn
 *
 **/
package com.interfaces;

import ade.ADEComponent;
import com.LaserScan;
import com.lrf.feature.Door;

import java.rmi.RemoteException;
import java.util.List;

/**
 * Minimal interface for laser rangefinder servers.
 */
public interface LaserComponent extends ADEComponent {

    /**
     * Get the distance (in meters) that robot takes to be the distance at which obstacle avoidance should engage.
     *
     * @return the critical distance.
     */
    public double getCritDist() throws RemoteException;

    /**
     * Set the distance (in meters) that robot takes to be the distance at which obstacle avoidance should engage.
     *
     * @param dist the new critical distance
     */
    public void setCritDist(double dist) throws RemoteException;

    /**
     * Get the distance that robot uses to calculate open space.
     *
     * @return the minopen distance.
     */
    public double getMinOpen() throws RemoteException;

    /**
     * Set the distance that robot uses to calculate open space.
     *
     * @param mo the new minopen distance
     */
    public void setMinOpen(double mo) throws RemoteException;

    /**
     * Check whether there is currently an obstacle in front.
     *
     * @return true if an obstacle is present, false otherwise.
     */
    public boolean checkObstacle() throws RemoteException;

    /**
     * Check safe spaces.  A sector is safe if there's no obstacle detected within CRITICAL_DIST.
     *
     * @return an array of booleans indicating safe*
     */
    public boolean[] getSafeSpaces() throws RemoteException;

    /**
     * Check open spaces.  A sector is open if there's space there for the robot to move into.
     *
     * @return an array of booleans indicating open*
     */
    public boolean[] getOpenSpaces() throws RemoteException;

    /**
     * Check the angle of coverage of the LRF.
     *
     * @return the total angle a full scan covers (in radians).
     * @deprecated Use {@link #getLaserScan()} instead
     */
    @Deprecated
    public double getScanAngle() throws RemoteException;

    /**
     * Check the offset LRF.
     *
     * @return the offset.
     * @deprecated Use {@link #getLaserScan()} instead
     */
    @Deprecated
    public double getScanOffset() throws RemoteException;

    /**
     * Check how many readings this LRF returns.
     *
     * @return the number of LRF readings.
     * @deprecated Use {@link #getLaserScan()} instead
     */
    @Deprecated
    public int getNumLaserReadings() throws RemoteException;

    /**
     * Get LRF readings.  Readings are in meters, at one degree intervals, going counter-clockwise as the index
     * increases.
     *
     * @return the most recent set of LRF readings.
     * @deprecated Use {@link #getLaserScan()} instead
     */
    @Deprecated
    public double[] getLaserReadings() throws RemoteException;

    /**
     * Check whether there is currently a hallway detected
     *
     * @return true if a hallway is present, false otherwise.
     * @deprecated Use {@link com.lrf.extractor.LaserFeatureExtractorComponent#inHallway()}
     */
    @Deprecated
    public boolean checkHallway() throws RemoteException;

    /**
     * Check whether there is currently a doorway detected
     *
     * @return true if a doorway is present, false otherwise.
     * @deprecated Use {@link com.lrf.extractor.LaserFeatureExtractor#getDoors()#isEmpty()} instead.
     */
    @Deprecated
    public boolean checkDoorway() throws RemoteException;

    /**
     * @return an array of all detected doors
     * @deprecated Use {@link com.lrf.extractor.LaserFeatureExtractorComponent#getDoors()} instead
     */
    @Deprecated
    public List<Door> getDoorways() throws RemoteException;

    /**
     * Get a laser scan from the laser component.
     * <p/>
     * A scan contains the range readings as well as useful metadata for working with these readings.
     *
     * @return the current laser scan
     * @throws RemoteException
     */
    public LaserScan getLaserScan() throws RemoteException;
}

