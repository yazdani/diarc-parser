package com.adesim;

import java.rmi.RemoteException;

import com.adesim.datastructures.CameraSpecs;
import com.adesim.datastructures.MountingPoint;
import com.adesim.datastructures.Point3D;
import com.adesim.robot.SimAbstractRobot;
import com.adesim.robot.SimEra;


public class SimEraComponentImpl extends SimGenericRobotBaseComponentImpl implements SimEraComponent {
	private static final long serialVersionUID = 1L;
    
	private static final MountingPoint laserMountingPoint = new MountingPoint(
			new Point3D(SimEra.X_LENGTH_IN_METERS/2 - 0.05, 0, 0.15), 0);
	//      5 cm from front, center, 15 cm from floor, facing forward

	private static final CameraSpecs cameraSpecs = new CameraSpecs(
			new MountingPoint(new Point3D(SimEra.X_LENGTH_IN_METERS/2 - 0.05, 0, -2.30), 0),
			50, 30.0);
	//      * mounting point 5 cm from front, center, 30 cm from floor, facing forward
        //      * PWS: height is broken, see com/adesim/robot/SimAbstractRobot.java
	//      * field of vision 50 degrees (approximate number; turns out even Logitech didn't
	//           know what their field of vision is!
	//      * maximum distance that can see anything at all, 30 meters (essentially a made-up number
	//           that may need to be adjusted experimentally, though in any "real" environment
	//           it shouldn't matter anyhow, as there will be obstacles in the way anyway).
	//           but this number specifies the maximum length for the perception rays, and corresponds
	//           roughly to how well a robot would be able to distinguish something at a great distance).

	private static final String defaultLaserName = "urg";

	public SimEraComponentImpl() throws RemoteException {
		super(defaultLaserName);
	}

	@Override
	public SimAbstractRobot createRobot(boolean noisyOdometry) {
		return new SimEra(model, getActorName(), noisyOdometry, this.laser, cameraSpecs);
	}

	@Override
	public MountingPoint getLaserMountingPoint() {
		return laserMountingPoint;
	}

	@Override
	protected double getDefaultAcceleration() {
            return 1.0; // higher means quicker stopping
            // return 0.5;
	}

	@Override
	protected double getDefaultTV() {
            return 0.25; // meters/sec
            // return 0.33;
	}

	@Override
	protected double getDefaultRV() {
            return Math.PI / 8.0; // rads/sec
            // return 0.75;    
	}

}
