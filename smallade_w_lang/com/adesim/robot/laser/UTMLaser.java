package com.adesim.robot.laser;

import com.adesim.datastructures.MountingPoint;

import java.awt.geom.Point2D;
import java.util.ArrayList;

public class UTMLaser extends AbstractLaser {
    private static final int NUM_LASER_READINGS = 270; //Actually 1080, but this assumes 1 per degree.
    private static final double LASER_OFFSET = 0.0;
    private static final double MAX_LASER_DISTANCE = 60.0;
    //     The URG has 5m max range

    public UTMLaser(MountingPoint mountingPoint) {
        super(mountingPoint, NUM_LASER_READINGS, LASER_OFFSET, MAX_LASER_DISTANCE);
    }

    public UTMLaser(MountingPoint mountingPoint, ArrayList<Point2D> coordinates) {
        super(mountingPoint, NUM_LASER_READINGS, LASER_OFFSET, MAX_LASER_DISTANCE, coordinates);
    }

    @Override
    public int[] getStartAndEndAngles(int sectorCounter, int totalSectors) {
        //hard code.
        //	int angleStart = sectorCounter * NUM_LASER_READINGS / totalSectors;
        //	int angleEnd = (sectorCounter+1) * NUM_LASER_READINGS / totalSectors;
        if (sectorCounter == 0)
            return new int[]{0, 160};
        else if (sectorCounter == 1)
            return new int[]{161, 240};
        else
            return new int[]{241, 270};
    }

    @Override
    public double getAngleMin() {
        return 0;
    }

    @Override
    public double getAngleMax() {
        return 270.0;
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
        return 60.0;
    }

}
