/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * SimPioneerComponentImpl.java
 *
 * A simulated Pioneer robot that connects to the ADE simulation environment
 * @see ADESimEnvironmentComponentImpl and @see {@link ADESimActorComponentImpl}
 *
 * @author Michael Zlatkovsky (based on original ADESim by Paul Schermerhorn, et al)
 *
 */

package com.adesim;

import com.adesim.datastructures.CameraSpecs;
import com.adesim.datastructures.MountingPoint;
import com.adesim.datastructures.Point3D;
import com.adesim.robot.SimAbstractRobot;
import com.adesim.robot.SimPioneer;

import java.rmi.RemoteException;

public class SimPioneerComponentImpl extends SimGenericRobotBaseComponentImpl implements SimPioneerComponent {
    private static final long serialVersionUID = 1L;
    private static final MountingPoint laserMountingPoint = new MountingPoint(
            new Point3D(SimPioneer.X_LENGTH_IN_METERS / 2 - 0.05, 0, 0.15), 0);
    //      5 cm from front, center, 15 cm from floor, facing forward
    private static final CameraSpecs cameraSpecs = new CameraSpecs(
            new MountingPoint(new Point3D(SimPioneer.X_LENGTH_IN_METERS / 2 - 0.05, 0, -2.30), 0),
            50, 30.0);
    //      * mounting point 5 cm from front, center, 20 cm from floor, facing forward
    //      * PWS: height is broken, see com/adesim/robot/SimAbstractRobot.java
    //      * field of vision 50 degrees (approximate number; turns out even Logitech didn't
    //           know what their field of vision is!
    //      * maximum distance that can see anything at all, 30 meters (essentially a made-up number
    //           that may need to be adjusted experimentally, though in any "real" environment
    //           it shouldn't matter anyhow, as there will be obstacles in the way anyway).
    //           but this number specifies the maximum length for the perception rays, and corresponds
    //           roughly to how well a robot would be able to distinguish something at a great distance).
    private static final String defaultLaserName = "sick";

    public SimPioneerComponentImpl() throws RemoteException {
        super(defaultLaserName);
    }

    /** {@inheritDoc} */
    @Override
    public SimAbstractRobot createRobot(boolean noisyOdometry) {
        return new SimPioneer(model, getActorName(), noisyOdometry, laser, cameraSpecs);
    }

    /** {@inheritDoc} */
    @Override
    public MountingPoint getLaserMountingPoint() {
        return laserMountingPoint;
    }

    /** {@inheritDoc} */
    @Override
    protected double getDefaultAcceleration() {
        return 0.5; // higher means quicker stopping
    }

    /** {@inheritDoc} */
    @Override
    protected double getDefaultTV() {
        return 0.25; // meters/sec
    }

    /** {@inheritDoc} */
    @Override
    protected double getDefaultRV() {
        return Math.PI / 8.0; // rads/sec
    }

}
