/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * @author Michael Zlatkovsky (based on original ADESim by Paul Schermerhorn, et al)
 *
 */

package com.adesim;

import ade.gui.ADEGuiVisualizationSpecs;
import com.LaserScan;
import com.adesim.datastructures.MountingPoint;
import com.adesim.gui.vis3D.ADESim3DCameraVisualization;
import com.adesim.robot.interfaces.LaserCarrying;
import com.adesim.robot.laser.AbstractLaser;
import com.adesim.robot.laser.SICKLaser;
import com.adesim.robot.laser.URGLaser;
import com.adesim.robot.laser.UTMLaser;
import com.interfaces.LaserComponent;
import com.lrf.LRFComponentVis;
import com.lrf.LaserFeatureDetector;
import com.lrf.feature.Door;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.event.KeyEvent;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Generic class for a robot that has a camera and a laser (for example, the Pioneer or the Videre Era) (actually,
 * there's no particular mention of camera in this file, it gets instantiated by individual instances, except insofar as
 * the getVisualizationSpecs() creates a Camera visualization)
 */
public abstract class SimGenericRobotBaseComponentImpl extends ADESimActorComponentImpl implements LaserComponent {
    private static final long serialVersionUID = 1L;
    protected String laserName; // for specifying via commandline
    protected AbstractLaser laser; // for the laser, once it's been instantiated
    protected LaserFeatureDetector lfd;
    private Log log = LogFactory.getLog(getClass());

    /////////////////////////////////////////////////////////////////////////////
    //    ABSTRACT METHODS THAT NEED TO BE IMPLEMENTED BY PARTICULAR INSTANCES //
    /////////////////////////////////////////////////////////////////////////////

    public SimGenericRobotBaseComponentImpl(String defaultLaserName) throws RemoteException {
        super();
        laser = instantiateLaser(defaultLaserName);
        lfd = new LaserFeatureDetector(laser.getNumLaserReadings(), laser.getScanAngle(), laser.getScanOffset());
    }

    /////////////////////////////////////////////////////////////////////////////

    public abstract MountingPoint getLaserMountingPoint();

    private AbstractLaser instantiateLaser(String defaultLaserName) {
        // if no command-line laser name specified, instantiate default
        if (laserName == null) {
            laserName = defaultLaserName;
        }
        if (coordinatesRead)
            log.info("ADESIM: Using robot hull coordinates for obstacle avoidance");
        switch (laserName) {
            case "sick":
                return new SICKLaser(getLaserMountingPoint());
            case "urg":
                return new URGLaser(getLaserMountingPoint());
            case "utm":
                if (coordinatesRead)
                    return new UTMLaser(getLaserMountingPoint(), robotHullCoordinates);
                return new UTMLaser(getLaserMountingPoint());
            default:
                log.warn("Unrecognized laser name \"" + laserName + "\".  Resorting to default");
                return new SICKLaser(getLaserMountingPoint()); // default laser
        }
    }

    @Override
    protected void appendActorSpecificUsageInfo(StringBuilder actorSpecificInfo) {
        actorSpecificInfo
                .append("  -sick              <instantiate the SICK laser (180 degrees, long range)>\n")
                .append("  -urg               <instantiate the Hokuyo URG laser (240 degrees, 5m range)>\n")
                .append("  -utm               <instantiate the Hokuyo UTM laser (270 degrees, 60m range)>\n");
    }

    @Override
    protected int checkIfRecognizeArgument(String[] args, int i) {
        if (args[i].equalsIgnoreCase("-sick")) {
            this.laserName = "sick";
            return 0; // recognized, but not need to advance anything.
        } else if (args[i].equalsIgnoreCase("-urg")) {
            this.laserName = "urg";
            return 0; // recognized, but not need to advance anything.
        } else if (args[i].equalsIgnoreCase("-utm")) {
            this.laserName = "utm";
            return 0; // recognized, but not need to advance anything.
        } else {
            // did not recognize anything:
            return -1;
        }
    }

    @Override
    public ADEGuiVisualizationSpecs getVisualizationSpecs() throws RemoteException {
        ADEGuiVisualizationSpecs specs = super.getVisualizationSpecs();
        specs.add("Lasers", LRFComponentVis.class);
        specs.add("Camera", ADESim3DCameraVisualization.class);
//    specs.add("World Tick Frequency", FrequencyMonitor.class, simulationWorld.getBaseWorld().getNewTickCounter(), 10);
        return specs;
    }

    @Override
    public String getHelpText() throws RemoteException {
        return new StringBuilder()
                .append("* Up arrow:     move robot forward at default speed.\n")
                .append("* Down arrow:   move robot backwards at default speed.\n")
                .append("* Left arrow:   rotate robot to the left at default speed.\n")
                .append("* Right arrow:  rotate robot to the right at default speed.")
                .toString();
    }

    @Override
    public void keyPressed(int keyCode) throws RemoteException {
        if (keyCode == KeyEvent.VK_UP) {
            this.setTV(this.getDefaultTV());
        } else if (keyCode == KeyEvent.VK_DOWN) {
            this.setTV(-1 * this.getDefaultTV());
        } else if (keyCode == KeyEvent.VK_LEFT) {
            this.setRV(this.getDefaultRV());
        } else if (keyCode == KeyEvent.VK_RIGHT) {
            this.setRV(-1 * this.getDefaultRV());
        }
    }

    @Override
    public void keyReleased(int keyCode) throws RemoteException {
        if (keyCode == KeyEvent.VK_UP) {
            this.setTV(0);
        } else if (keyCode == KeyEvent.VK_DOWN) {
            this.setTV(0);
        } else if (keyCode == KeyEvent.VK_LEFT) {
            this.setRV(0);
        } else if (keyCode == KeyEvent.VK_RIGHT) {
            this.setRV(0);
        }
    }

    /**
     * Get the distance that MoPo takes to be the distance at which obstacle avoidance should engage.
     *
     * @return the critical distance.
     */
    public double getCritDist() throws RemoteException {
        return (this.isReady() ? ((LaserCarrying) model.robot).getCRITICALDIST() : 0);
    }

    /**
     * Set the distance that MoPo takes to be the distance at which obstacle avoidance should engage.
     *
     * @param dist the new critical distance
     */
    public void setCritDist(double dist) throws RemoteException {
        // no robot != null check here, as DO want it to fail.  Otherwise will give
        //     illusion of setting critical distance, when in reality have not!
        ((LaserCarrying) model.robot).setCRITICALDIST(dist);
    }

    /**
     * Get the distance that robot uses to calculate open space.
     *
     * @return the minopen distance.
     */
    public double getMinOpen() throws RemoteException {
        return (this.isReady() ? ((LaserCarrying) model.robot).getMINOPEN() : 0);
    }

    /**
     * Set the distance that robot uses to calculate open space.
     *
     * @param dist the new minopen distance
     */
    public void setMinOpen(double dist) throws RemoteException {
        // no robot != null check here, as DO want it to fail.  Otherwise will give
        //     illusion of setting minopen distance, when in reality have not!
        ((LaserCarrying) model.robot).setMINOPEN(dist);
    }

    /**
     * Check whether there is currently an obstacle in front.
     *
     * @return true if an obstacle is present, false otherwise.
     */
    public boolean checkObstacle() throws RemoteException {
        return (this.isReady() ? ((LaserCarrying) model.robot).getLaser().checkObstacle() : false);
    }

    /**
     * Get LRF readings.
     *
     * @return the most recent set of LRF readings.
     */
    @Override
    public double[] getLaserReadings() throws RemoteException {
        log.warn("This API is deprecated. Use getLaserScan() instead.");
        if (this.isReady()) {
            return laser.getCurrentLaserDistances();
        } else {
            return new double[0];
        }
    }

    /**
     * Check safe spaces.  A sector is safe if there's no obstacle detected within CRITICAL_DIST.
     *
     * @return an array of booleans indicating safe*
     */
    public boolean[] getSafeSpaces() throws RemoteException {
        if (this.isReady()) {
            return laser.getSafeSpaces();
        } else {
            return new boolean[3]; //return something.  that way gets at least
            //    some boolean array, hopefully preventing a crash
        }
    }

    /**
     * Check open spaces.  A sector is open if there's space there for the robot to move into.
     *
     * @return an array of booleans indicating open*
     */
    public boolean[] getOpenSpaces() throws RemoteException {
        if (this.isReady()) {
            return laser.getOpenSpaces();
        } else {
            return new boolean[3]; //return something.  that way gets at least
            //    some boolean array, hopefully preventing a crash
        }
    }

    /**
     * Check the angle of coverage of the LRF.
     *
     * @return the total angle a full scan covers (in radians).
     */
    public double getScanAngle() throws RemoteException {
        log.warn("This API is deprecated. Use getLaserScan() instead.");
        return laser.getScanAngle();
    }

    /**
     * Check the angle of coverage of the LRF.
     *
     * @return the total angle a full scan covers (in radians).
     */
    public double getScanOffset() throws RemoteException {
        log.warn("This API is deprecated. Use getLaserScan() instead.");
        return laser.getScanOffset();
    }

    /**
     * Check how many readings this LRF returns.
     *
     * @return the number of LRF readings.
     */
    public int getNumLaserReadings() throws RemoteException {
        log.warn("This API is deprecated. Use getLaserScan() instead.");
        if (this.isReady()) {
            return laser.getNumLaserReadings();
        } else {
            return 0;
        }
    }

    /**
     * Check whether there is currently a doorway detected
     *
     * @return true if a doorway is present, false otherwise.
     */
    public boolean checkDoorway() throws RemoteException {
        log.warn("This API is deprecated. Use LaserFeatureExtractorComponent.getDoors().isEmpty() instead.");
        lfd.updateFeatures(getLaserReadings());
        return !lfd.getDoors().isEmpty();
    }

    /**
     * Check whether there is currently a hallway detected
     *
     * @return true if a hallway is present, false otherwise.
     */
    public boolean checkHallway() throws RemoteException {
        log.warn("This API is deprecated. Use com.lrf.extractor.LaserFeatureExtractorComponent.inHallway() instead.");
        lfd.updateFeatures(getLaserReadings());
        return lfd.isInHallway();
    }

    /** @return an array of all detected doors */
    @Override
    public List<Door> getDoorways() throws RemoteException {
        log.warn("This API is deprecated. Use LaserFeatureExtractorComponent.getDoors() instead.");
        lfd.updateFeatures(getLaserReadings());
        return lfd.getDoors();
    }

    @Override
    public LaserScan getLaserScan() throws RemoteException {
        LaserScan scan = new LaserScan();

        scan.angleMin = laser.getAngleMin();
        scan.angleIncrement = laser.getAngleIncrement();
        scan.angleMax = laser.getAngleMax();

        scan.rangeMin = laser.getRangeMin();
        scan.rangeMax = laser.getRangeMax();
        scan.ranges = getLaserReadings();

        return scan;
    }
}
