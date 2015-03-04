/**
 * ADE 1.0 
 * Copyright 1997-2010 HRILab (http://hrilab.org/) 
 * 
 * All rights reserved.  Do not copy and use without permission. 
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * DefaultActionComponentArchImpl.java
 *
 * Last update: April 2010
 *
 * @author Paul Schermerhorn
 *
 */

/*******************************************************************************

  The following methods are available:

  boolean goStraight()
  boolean goLeft()
  boolean goRight()
  boolean turnLeft()
  boolean turnRight()
  boolean stop()

  boolean setVels(double tv, double rv)
  Set translational (tv) and rotational (rv) velocities.

  boolean[] checkObstacles()
  Check for obstacles in the three sectors, right (0), front (1), 
  and left (2).

  double[] getLaserReadings()
  Get all laser readings in meters from right (0) to left (180).
  
  boolean[] getBumperReadings()
  Get all bumper readings (true if activated) from right (0) to left (4).

*******************************************************************************/

package com.action;

import ade.*;
import com.lrf.feature.Door;

import java.rmi.*;

/** 
 * <code>DefaultActionComponentArch</code> implements the ActionComponent's
 * runArchitecture method, and whatever supporting code is necessary, for an
 * architecture specified in Java, rather than Action scripts.  This is the
 * one that's automatically chosen by {@link ade.BootstrapADE BootstrapADE}
 * if another isn't passed as an argument.
 */
public class DefaultActionComponentArchImpl extends ActionComponentImpl implements DefaultActionComponentArch {
    private boolean done = false;
    private int state = 0;
    private long aid = 0;
    private double x = 0.0;
    private double y = 0.0;
    private double ex = 0.0;
    private double ey = 0.0;

    /** 
     * <code>runArchitecture</code> is called periodically to perform
     * whatever sensing and acting required by the architecture.
     */
    @Override
    public void runArchitecture() {
        double[] readings;
        double d = 0.0, ed = 0.0;
        Door mydoor;
        double mydist;
        try {
            Thread.sleep(200);
        } catch (Exception e) {
        }
        if (done) {
            System.out.println("Reached first door, done.");
            return;
        }
        boolean[] obsts = checkObstacles();
        if (obsts[1])
            stop();
        else if (obsts[0])
            goLeft();
        else if (obsts[2])
            goRight();
        else
            goStraight();
    }

    /** 
     * Constructs the ActionComponentArch.
     */
    public DefaultActionComponentArchImpl() throws RemoteException {
        super();
    }

    /**
     * <code>main</code> passes the arguments up to the ADEComponentImpl
     * parent.  The parent does some magic and gets the system going.
    final public static void main (String[] args) throws Exception {
        ActionComponentImpl.main(args);
    }
     */
}

// vi:ai:smarttab:expandtab:ts=8 sw=4
