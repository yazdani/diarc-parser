package com.adesim.robot.laser;

import com.adesim.datastructures.MountingPoint;

public class URGLaser extends AbstractLaser {
	private static final int NUM_LASER_READINGS = 181; // if ever want to change it to the
	//     "real" spectrum of the URG, just replace it with 241 (240 degrees + one extra "0" ray).
	//     Using 181 for now just for consistency with the UrgLRFComponentImpl, which
	//     returns 181 for consistency with 181-only-compatible code.
        private static final double LASER_OFFSET = 0.0;

	private static final double MAX_LASER_DISTANCE = 5.0;
	//     The URG has 5m max range

	public URGLaser(MountingPoint mountingPoint) {
		super(mountingPoint, NUM_LASER_READINGS, LASER_OFFSET, MAX_LASER_DISTANCE);
	}

    @Override
    public double getAngleMin() {
        return 0;
    }

    @Override
    public double getAngleMax() {
        return 180.0;
    }

    @Override
    public double getAngleIncrement() {
        return 1.0;
    }

    @Override
    public double getRangeMin() {
        return 0;
    }

    @Override
    public double getRangeMax() {
        return MAX_LASER_DISTANCE;
    }

}
