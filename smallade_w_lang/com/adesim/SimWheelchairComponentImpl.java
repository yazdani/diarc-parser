/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * SimWheelchairComponentImpl.java
 *
 * A simulated Vulcan Wheelchair robot that connects to the ADE simulation environment
 * @see ADESimEnvironmentComponentImpl and @see {@link ADESimActorComponentImpl}
 *
 * @author Tom Williams (based on original ADESim by Paul Schermerhorn, et al)
 *
 */

package com.adesim;

import java.rmi.RemoteException;

import com.adesim.datastructures.CameraSpecs;
import com.adesim.datastructures.MountingPoint;
import com.adesim.datastructures.Point3D;
import com.adesim.robot.SimAbstractRobot;
import com.adesim.robot.SimWheelchair;

public class SimWheelchairComponentImpl extends SimGenericRobotBaseComponentImpl implements SimWheelchairComponent {
    //?
    private static final long serialVersionUID = 1L;
    private static double offset = -.66;
    private static final MountingPoint laserMountingPoint = new MountingPoint(
									      //?
      new Point3D(SimWheelchair.X_LENGTH_IN_METERS/2+.10, -SimWheelchair.Y_LENGTH_IN_METERS/2 - .05, .15), offset); 
    //      5 cm from front, center, 15 cm from floor, facing forward
    
    private static final CameraSpecs cameraSpecs = new CameraSpecs(
      new MountingPoint(new Point3D(SimWheelchair.X_LENGTH_IN_METERS/2 - 0.05, 0, -2.30), 0), 50, 30.0);
    //      * mounting point 5 cm from front, center, 20 cm from floor, facing forward
    //           that may need to be adjusted experimentally, though in any "real" environment
    //           it shouldn't matter anyhow, as there will be obstacles in the way anyway).
    //           but this number specifies the maximum length for the perception rays, and corresponds
    //           roughly to how well a robot would be able to distinguish something at a great distance).
    
    private static final String defaultLaserName = "Utm";
    public SimWheelchairComponentImpl() throws RemoteException {
	super(defaultLaserName);
    }
    @Override
	public SimAbstractRobot createRobot(boolean noisyOdometry) {
	return new SimWheelchair(model, getActorName(), noisyOdometry, this.laser, cameraSpecs);
    }
    
    @Override
	public MountingPoint getLaserMountingPoint() {
	return laserMountingPoint;
    }
    //?
    @Override
	protected double getDefaultAcceleration() {
	return 0.5; // higher means quicker stopping
    }
    //?
    @Override
	protected double getDefaultTV() {
	return 0.1; // meters/sec
    }
    //?
    @Override
	protected double getDefaultRV() {
	return 0.2; // rads/sec
    }
}
