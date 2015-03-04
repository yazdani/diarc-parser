package com.adesim.datastructures;

/** simple data structure to encompass camera mounting point and field of view */
public class CameraSpecs {
	
	public MountingPoint mountingPoint;
	public int fieldOfVisionDegrees;
	public double maxPerceptualDistance;
	
	
	public CameraSpecs(MountingPoint mountingPoint, int fieldOfVisionDegrees, double maxPerceptualDistance) {
		super();
		this.mountingPoint = mountingPoint;
		this.fieldOfVisionDegrees = fieldOfVisionDegrees;
		this.maxPerceptualDistance = maxPerceptualDistance;
	}
}
