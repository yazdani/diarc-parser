/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 * Last update: April 2010
 *
 * PioneerComponent.java
 */
package com.pioneer;

import ade.ADEComponent;
import java.rmi.*;
import com.interfaces.*;
//import com.HardwareInterface;

/**
PioneerComponent.java

The interface for the Pioneer robot server implementation.
These methods are in addition to other interfaces implemented
by the PioneerComponentImpl class.
@author Jim Kramer
*/
public interface PioneerComponent extends ADEComponent,
				BumperSensorComponent,
				SonarComponent,
                                VelocityComponent {
	/** Toggles the sonar printout to the console. */
	public void toggleShowSonar() throws RemoteException;
	
	/** Toggles the bumper printout to the console. */
	public void toggleShowBumper() throws RemoteException;
	
	/** Toggles the digital in (LSB are IR sensors) printout to the console. */
	public void toggleShowDigin() throws RemoteException;
	
	/** Return the status of the motors; that is, whether they are
	 * engaged. */
        public boolean[] getMotors() throws RemoteException;

	/** Used to turn sonar on/off. */
	public void setSonar(int arg) throws RemoteException;

        // BatterySensorComponent

	/** Returns voltage as a float value */
	public float getVoltage() throws RemoteException;

	/** Returns the status of the battery alarm (i.e., low battery). */
	public boolean getBatteryAlarm() throws RemoteException;

	/** Sets the value of the "low battery" indicator. */
	public void setLowBatteryValue(float low) throws RemoteException;

        // EncoderSensorComponent

	/** Returns encoder counts as an array of ints */
	public int[] getEncoders() throws RemoteException;

        // IRSensorComponent

	/** Returns a boolean for a specific IR (0 is left, 1 is right).
	 * @param w which IR */
	public boolean getIR(int w) throws RemoteException;

	/** Returns a boolean array for all IRs. */
	public boolean[] getIRs() throws RemoteException;
	
        // MovementEgocentricEffectorComponent

	/** Stop movement.
	 * @throws RemoteException If an error occurs */
	public void stop() throws RemoteException;
	
	/** Turn a relative orientation (in radians).
	 * @param t The number of radians to turn
	 * @throws RemoteException If an error occurs */
	public void turn(double t) throws RemoteException;
	
	/** Turn a relative orientation (in radians) at the specified speed.
	 * @param t The number of radians to turn
	 * @param spd The speed at which to rotate
	 * @throws RemoteException If an error occurs */
	public void turn(double t, double spd) throws RemoteException;
	
	/** Turn a relative orientation (in degrees).
	 * @param t The number of degrees to turn
	 * @throws RemoteException If an error occurs */
	public void turnDeg(int t) throws RemoteException;
	
	/** Turn a relative orientation (in degrees) at the specified speed.
	 * @param t The number of degrees to turn
	 * @param spd The speed at which to rotate
	 * @throws RemoteException If an error occurs */
	public void turnDeg(int t, double spd) throws RemoteException;
	
	/** Move a specified distance (in meters) straight ahead at the
	 * default speed.
	 * @param dist The distance to move forward
	 * @throws RemoteException If an error occurs */
	public void go(double dist) throws RemoteException;
	
	/** Move a specified distance (in meters) straight ahead at the
	 * speed specified.
	 * @param dist The distance to move forward
	 * @param spd The speed at which to move
	 * @throws RemoteException If an error occurs */
	public void go(double dist, int spd) throws RemoteException;
	
	/** Move forward (positive) or backward (negativein the direction of the current heading in meters
	 * @param dx the distance, in m, to move forward/backward
	 * @throws RemoteException If an error occurs */
        public void move(double distance) throws RemoteException;
	
	/** Move to a location relative to the current location. Note that the
	 * parameters are perhaps better thought of as <i>delta</i> distances
	 * (or change in location). The trajectory travelled is implementation
	 * dependent (i.e., not necessarily a straight line).
	 * @param dx the distance, in m, to move forward/backward
	 * @param dy the distance, in m, to move left/right
	 * @throws RemoteException If an error occurs */
	public void move(double dx, double dy) throws RemoteException;
	
	/** Move to a location relative to the current location at the specified
	 * speeds. Note that the first two parameters are perhaps better thought
	 * of as <i>delta</i> distances (or change in location). The trajectory
	 * travelled is implementation dependent (i.e., not necessarily a
	 * straight line).
	 * @param dx The distance, in m, to move forward/backward
	 * @param dy The distance, in m, to move left/right
	 * @param tv The translational velocity at which to move
	 * @param rv The rotational velocity at which to move
	 * @throws RemoteException If an error occurs */
	public void move(double dx, double dy, double tv, double rv)
				throws RemoteException;
	
        // PositionEgocentricComponent

	/** Return a three-element array of (x, y, theta) position.
	 * @return A three-element <tt>double</tt> array (x,y,t)
	 * @throws RemoteException If an error occurs */
	public double[] getPoseEgo() throws RemoteException;
	
	/** Return an x coordinate position.
	 * @return A <tt>double</tt> value of the x-coordinate
	 * @throws RemoteException If an error occurs */
	public double getXPosEgo() throws RemoteException;
	
	/** Return a y coordinate position.
	 * @return A <tt>double</tt> value of the y-coordinate
	 * @throws RemoteException If an error occurs */
	public double getYPosEgo() throws RemoteException;
	
	/** Return orientation (in rads).
	 * @return A <tt>double</tt> value of the orientation
	 * @throws RemoteException If an error occurs */
	public double getTPosEgo() throws RemoteException;
	
	/** Return orientation (in degrees).
	 * @return A <tt>double</tt> value of the orientation
	 * @throws RemoteException If an error occurs */
	public double getTPosEgoDeg() throws RemoteException;
	
	/** Reset the current pose to the given (x, y, t) coordinates
	 * (in m, m, rads).
	 * @param x The <tt>x</tt> coordinate
	 * @param y The <tt>y</tt> coordinate
	 * @param t The desired orientation
	 * @throws RemoteException If an error occurs */
	public void setPoseEgo(double x, double y, double t) throws RemoteException;
	
	/** Reset the current location to the given (x, y, t) coordinates
	 * (in m, m, degs).
	 * @param x The <tt>x</tt> coordinate
	 * @param y The <tt>y</tt> coordinate
	 * @param t The desired orientation
	 * @throws RemoteException If an error occurs */
	public void setPoseEgoDeg(double x, double y, int t)
				throws RemoteException;

	// VelocitySensorComponent

	/** The velocity of all the motors. */
	public int[] getVelocity() throws RemoteException;
	
	/** Return the left motor velocity. */
	public int getLeftVelocity() throws RemoteException;
	
	/** Return the right motor velocity. */
	public int getRightVelocity() throws RemoteException;

	// VelocityEffectorComponent

	/** Immediately stop the motors. */
	public void emergencyStop() throws RemoteException;
	
	/** Set the maximum translational velocity. */
	public void setMaxVelocityTranslational(int v) throws RemoteException;
	
	/** Set the maximum rotational velocity. */
	public void setMaxVelocityRotational(int v) throws RemoteException;
	
	/** Set the translational velocity (in millimeters per second). */
	public void setTranslationalVelocity(int v) throws RemoteException;
	
	/** Set the rotational velocity (in degrees per second). */
	public void setRotationalVelocity(int v) throws RemoteException;
	
	/** Set the rotational velocity (in radians per second). */
	public void setRotationalVelocity(double v) throws RemoteException;
	
	/** Set forward velocity.
	 * @param v velocity value */
	public void setVelocity(int v) throws RemoteException;
	
	/** Set velocity of left/right motors (or the first two
	 * if there are more than two).
	 * @param left left wheel velocity
	 * @param right right wheel velocity */
	public void setVelocities(int left, int right) throws RemoteException;
	
	/** Set velocity of all motors.
	 * @param arg array of velocity values */
	public void setVelocities(int arg[]) throws RemoteException;
	
	/** Set the left wheel velocity.
	 * @param v velocity value */
	public void setLeftVelocity(int v) throws RemoteException;
	
	/** Set the right wheel velocity.
	 * @param v velocity value */
	public void setRightVelocity(int v) throws RemoteException;

	// DigitalInSensorComponent

	/** Returns the digital in value as a short */
	public short getDigin() throws RemoteException;

	// DigitalOutEffectorComponent

	/** Sets the digital out effector.
	 * @param port number between 0-7
	 * @param arg 1 (set) or 0 (reset) */
	public void setDigitalOut(int port, int arg) throws RemoteException;
}
