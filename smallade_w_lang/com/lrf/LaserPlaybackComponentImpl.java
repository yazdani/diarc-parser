/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * LaserPlaybackComponentImpl.java
 *
 * @author Matthias Scheutz
 */
package com.lrf;

import com.LaserScan;

import java.io.*;
import java.rmi.*;

import static utilities.Util.*;

/**
 * <code>LaserPlaybackComponentImpl</code> for playing back laser data from log files without the need for laser devices
 */
public class LaserPlaybackComponentImpl extends LRFComponentImpl implements LaserPlaybackComponent {
    private static int localNumReadings = 512; //682;
    private static double localScanAngle = Math.toRadians(180); //(240.0);

    /**
     * The server is always ready to provide its service after it has come up
     */
    @Override
    protected boolean localServicesReady() {
        return true;
    }

    /**
     * Constructor for LaserPlaybackComponentImpl.
     */
    public LaserPlaybackComponentImpl() throws RemoteException {
        super(localNumReadings, localScanAngle);

        // parseadditionalargs now called in ADEComponentImpl constructor, so the values
        // passed in to super above do not reflect command-line parameters
        if (localNumReadings != numReadings) {
            numReadings = localNumReadings;
            scanAngle = localScanAngle;
            detector = new LaserFeatureDetector(numReadings, scanAngle, offset);
        }
        readings = new double[numReadings];
        history = new int[numReadings];
    }

    // read laser values from the log
    @Override
    protected void updateComponent() {
        // System.out.println("Reading");
    }

    //@Override
    protected void updateFromLog(String logEntry) {
        String[] laserstrings = logEntry.split(",");
        double[] localreadings = new double[laserstrings.length];
        // skip the entry if the number of readings is not the expected number
        if (numReadings != laserstrings.length)
            return;
        for (int i = 0; i < laserstrings.length; i++) {
            try {
                localreadings[i] = Double.parseDouble(laserstrings[i]);
            } catch (NumberFormatException nfe) {
                System.err.println("LaserPlaybackComponent: Problem parsing " + i + "th laser value: " + laserstrings[i]);
                localreadings[i] = 0.0;
            }
        }
        readings = localreadings;
    }

    //@Override
    public void shutdown() {
    }

    @Override
    public LaserScan getLaserScan() throws RemoteException {
        // TODO
        return null;
    }
}
