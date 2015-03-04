package com.lrf;
import java.io.Serializable;
import java.lang.Cloneable;

public class polar_laser_scan implements Serializable, Cloneable{
    public int type = 0; //1 = sick laser
    public long timestamp;
    public int scanID;
    public float startAngle;
    public float angularResolution;
    public float[] ranges;
    public short numRanges;
    public short[] intensities;
    public float maxRange;
    public float offset_x;
    public float offset_y;
    public float offset_t;

    public polar_laser_scan(){
	this.timestamp = System.currentTimeMillis();
    }

    public polar_laser_scan copy(){
	try {
	    return (polar_laser_scan)this.clone();
	} catch (java.lang.CloneNotSupportedException cnse) {
	    System.out.println("Can't clone");
	    return null;
	}
    }

    public void print(){
	System.out.println("timestamp:        "+timestamp);
	System.out.println("scanID:           "+scanID);
	System.out.println("starting angle:   "+startAngle);
	System.out.println("resolution:       "+angularResolution);	
	System.out.println("first range:      "+ranges[0]);
	System.out.println("number of ranges: "+numRanges);
	System.out.println("first intensity:  "+intensities[0]);
	System.out.println("maximum range:    "+maxRange);
	System.out.println("offset:");
	System.out.println("     x:           "+offset_x);
	System.out.println("     y:           "+offset_y);
	System.out.println("     theta:       "+offset_t);
    }

}
