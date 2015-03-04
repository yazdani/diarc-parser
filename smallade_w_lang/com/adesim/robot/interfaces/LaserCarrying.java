package com.adesim.robot.interfaces;

import com.adesim.robot.laser.AbstractLaser;

public interface LaserCarrying {
	public AbstractLaser getLaser();

	public void setCRITICALDIST(double dist);
    public double getCRITICALDIST();
	public double getMINOPEN();
	public void setMINOPEN(double dist);
}
