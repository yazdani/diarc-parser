package com.adesim.robot;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.Random;

import com.adesim.datastructures.Point3D;
import com.adesim.util.SimUtil;


public class SimLocationSpecifier implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private double x, y, z, theta; // theta is in radians.  this is global in terms of the sim.
	
	/// Accurate odometry (required for simulation to work properly)
	private double odoX, odoY, odoTheta; // this is relative to x,y, and theta at time 0.

	/// Noisy odometry
	private double noisy_odoX, noisy_odoY, noisy_odoTheta;
	
	/// Odometry noise on each update. Taken from the original (pre-2010) ADESim.
	double[] ododev = {0.05, 0.05, 0.0125}; // Vishesh's stddev
	Random rand;

	
	/** NOTE that when the SimLocationSpecifier is IN USE BY THE ROBOT,
	 * the X, Y, Z LOCATION IS RELATIVE TO THE ROBOT'S CENTER!!!
	 * When INITIALIZING the robot, however, the Z refers to the bottom of 
	 * the robot (makes more sense from the environment's point of view,
	 * which does not know which robot is to come), hence the 
	 * offsetZbyHalfOfRobotHeight(SimAbstractRobot robot) method. */
	public SimLocationSpecifier(Point3D centerPoint3D, double theta) {
		// want to set both the actual position and the odometry angle (the x and y 
		//    can stay at 0, unless there's a reason to set them to 
		//    centerPoint3D.x, centerPoint3D.y; someone who works with odometry
		//    should comment and make those changes if necessary).
		this(centerPoint3D.x, centerPoint3D.y, centerPoint3D.z,
				SimUtil.normalizeAngle(theta), 
			 0, 0, SimUtil.normalizeAngle(theta));
	}
	
	/** copy constructor */
	public SimLocationSpecifier(SimLocationSpecifier toCopy) {
		this(toCopy.x, toCopy.y, toCopy.z, toCopy.theta, toCopy.odoX, toCopy.odoY, toCopy.odoTheta);
	}
	
	private SimLocationSpecifier(double x, double y, double z, double theta, double odo_x, double odo_y, double odo_theta) {
		// in this super-explicit constructor, will NOT normalize theta, as presumably anyone who has
		//    such intimate knowledge of the odometry is just copying an old SimLocationSpecifier -- and so
		//    there's no need to do the extra processing.
		this.x = x;
		this.y = y;
		this.z = z;
		this.theta = theta;
		
		this.odoX = odo_x;
		this.odoY = odo_y;
		this.odoTheta = odo_theta;
	}
	
	/** NOTE that when the SimLocationSpecifier is IN USE BY THE ROBOT,
	 * the X, Y, Z LOCATION IS RELATIVE TO THE ROBOT'S CENTER!!!
	 * When INITIALIZING the robot, however, the Z refers to the bottom of 
	 * the robot (makes more sense from the environment's point of view,
	 * which does not know which robot is to come), so need to lower z by half 
	 * of the robot's height, hence the offsetZbyHalfOfRobotHeight(SimAbstractRobot robot) method. */
	public void offsetZbyHalfOfRobotHeight(SimAbstractRobot robot) {
		this.z -= (robot.getPlatonicShape().getZLength() / 2.0);
	}

	@Override
	public String toString() {
		return "x " + x + " y " + y + " z " + z + " theta " + theta + 
				" odoX " + odoX + " odoY " + odoY + " odoTheta " + odoTheta;
	}
	
	/** returns true if x, y, z, and theta are all 0s. */
	public boolean allZeros() {
		return ( (x==0) && (y==0) && (z==0) && (theta==0) );
	}

	public Point2D.Double getXYLocation() {
		return new Point2D.Double(x, y);
	}

	public void set(SimLocationSpecifier locationSpecifier) {
		odoX = odoX + (locationSpecifier.x - this.x);
		odoY = odoY + (locationSpecifier.y - this.y);
		odoTheta = SimUtil.normalizeAngle(odoTheta + locationSpecifier.theta - this.theta);
		
		this.x = locationSpecifier.x;
		this.y = locationSpecifier.y;
		this.theta = SimUtil.normalizeAngle(locationSpecifier.theta);
	}
	
	public void addOffsets(SimLocationSpecifier positionDifference) {
		addLocationOffset(positionDifference.getXYLocation());
		addThetaOffset(positionDifference.getTheta());
		addZOffset(positionDifference.getZ());
	}
	
	public void addLocationOffset(Point2D offset) {
		if (rand == null) {
			this.rand = new Random();
		}
		
		double noiseX = offset.getX() * rand.nextGaussian() * ododev[0];
		double noiseY = offset.getY() * rand.nextGaussian() * ododev[1];
		noisy_odoX += offset.getX() + noiseX;
		noisy_odoY += offset.getY() + noiseY;
		
		odoX += offset.getX();
		odoY += offset.getY();
		this.x = this.x+offset.getX();
		this.y = this.y+offset.getY();
	}
	public void addThetaOffset(double offset) {
		double noiseTheta = offset * rand.nextGaussian() * ododev[2];
		noisy_odoTheta = SimUtil.normalizeAngle(noisy_odoTheta + offset + noiseTheta);
		
		odoTheta = SimUtil.normalizeAngle(odoTheta + offset);
		this.theta = SimUtil.normalizeAngle(this.theta + offset);
	}
	public void addZOffset(double offset) {
		this.z += offset;
	}
	
	
	public AffineTransform getTransformation() {
		AffineTransform transformation = new AffineTransform();
		transformation.translate(x, y);
		transformation.rotate(theta);
		return transformation;
	}
	
	public double[] getPoseGlobal() {
		return new double[]{x, y, theta};
	}
	
	public double getX() {
		return x;
	}
	public double getY() {
		return y;
	}
	public double getZ() {
		return z;
	}
	public double getTheta() {
		return theta;
	}
	public double getOdoX() {
		return odoX;
	}
	public double getOdoY() {
		return odoY;
	}
	public double getOdoTheta() {
		return odoTheta;
	}

	public double[] getPoseEgoGroundTruth() {
		return new double[] {odoX, odoY, odoTheta};
	}

	public double[] getPoseEgoNoisy() {
		return new double[] { noisy_odoX, noisy_odoY, noisy_odoTheta };
	}

	public void resetPoseEgo() {
		odoX = 0.0;
                odoY = 0.0;
                odoTheta = 0.0;
	}

}
