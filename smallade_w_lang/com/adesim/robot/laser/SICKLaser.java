package com.adesim.robot.laser;

import com.adesim.datastructures.MountingPoint;

public class SICKLaser extends AbstractLaser {

    private static final int NUM_LASER_READINGS = 181;
    //     180 degrees + one extra "0" ray
    private static final double LASER_OFFSET = 0.0;
    /**
     * theoretically (as per website info, anyway), the SICK laser is capable of detecting objects at up to 80 meters
     */
    private static final double MAX_LASER_DISTANCE = 80.0;


    public SICKLaser(MountingPoint mountingPoint) {
        super(mountingPoint, NUM_LASER_READINGS, LASER_OFFSET, MAX_LASER_DISTANCE);
    }

    @Override
    public double getAngleMin() {
        return 0.0;
    }

    @Override
    public double getAngleMax() {
        return Math.toRadians(NUM_LASER_READINGS);
    }

    @Override
    public double getAngleIncrement() {
        return Math.toRadians(1.0);
    }

    @Override
    public double getRangeMin() {
        return 0.0;
    }

    @Override
    public double getRangeMax() {
        return MAX_LASER_DISTANCE;
    }

}
