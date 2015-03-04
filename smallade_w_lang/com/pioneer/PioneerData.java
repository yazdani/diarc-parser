/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 * Last update: April 2010
 *
 * PioneerData.java
 */
package com.pioneer;

import java.io.Serializable;

/**
Simple data structure so that all pioneer information can be transferred
with one method call.

@author Jim Kramer
*/
public class PioneerData implements Serializable, Cloneable {
	public int[]     encoders;  // 0 is left, 1 is right
	public int[]     curvels;   // 0 is left, 1 is right
	public int[]     tvrv;      // 0 is tv, 1 is rv (not implemented?)
	public int[]     maxvels;   // not captured?
	public double    battery;   //
	public boolean[] bumpers;   //
	public double[]  sonars;    //
	public boolean   sonaron = false; // true=on; false=off
	public boolean[] irs;       //
	
	/** Constructor that takes number of sonar as a parameter. */
	public PioneerData(int nson) {
		encoders = new int[2];
		curvels = new int[2];
		bumpers = new boolean[10];
		sonars = new double[nson];
		irs = new boolean[2];
	}

	public PioneerData duplicate() {
		try {
			return (PioneerData)this.clone();
		} catch(CloneNotSupportedException e) {
			System.err.println("Cloning of PioneerData not supported.");
			return null;
		}
	}
	
}
