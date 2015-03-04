package com;

import java.io.Serializable;

/** @author Jeremiah Via <jeremiah.via@gmail.com> */
public class LaserScan implements Serializable {
    public double angleMin;
    public double angleMax;
    public double angleIncrement;
    public double rangeMin;
    public double rangeMax;
    public double[] ranges;

    public String toString() {
        return String.format("Angles: [%f, %f, %f], Ranges: [%f, %f, %s]",
                angleMin, angleIncrement, angleMax, rangeMin, rangeMax, ranges);
    }
}
