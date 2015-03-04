package com.adesim.robot;

import java.io.Serializable;

import com.adesim.datastructures.Point3D;

/* A very basic class for passing information to the 3D visualizer */
public class CameraData implements Serializable {
	private static final long serialVersionUID = 1L;
	
	
	public Point3D location;
	public double theta; // in radians
	public int fieldOfViewDegrees;
	
	
	public CameraData(Point3D location, double theta, int fieldOfViewDegrees) {
		this.location = location;
		this.theta = theta;
		this.fieldOfViewDegrees = fieldOfViewDegrees;
	}
}
