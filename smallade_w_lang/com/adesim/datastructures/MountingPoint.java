package com.adesim.datastructures;


/**
 * A simple data structure that specifies a RELATIVE MOUNTING POINT on the robot (with the robot in its "platonic" ideal
 * form, at the origin and facing right), and also the mounting ANGLE offset RELATIVE TO THE FRONT OF THE ROBOT. Thus, a
 * laser that's at the front of a robot might have point (0.15, 0), and angle 0.
 */
public class MountingPoint {
    public Point3D point;
    public double degreeOffsetFromFront; // as a PLUS/MINUS radian offset


    /**
     * Point relative to robot platonic form, offset from front as a plus/minus radian offset
     *
     * @param point                 Point relative to robot platonic form
     * @param degreeOffsetFromFront offset from front as a plus/minus radian offset
     */
    public MountingPoint(Point3D point, double degreeOffsetFromFront) {
        this.point = point;
        this.degreeOffsetFromFront = degreeOffsetFromFront;
    }

}
