/**
 * ADE 1.0 
 * Copyright 1997-2010 HRILab (http://hrilab.org/) 
 * 
 * All rights reserved.  Do not copy and use without permission. 
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * ActionComponentArchImpl.java
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
Get all laser readings in meters from right (0) to left (181).

 *******************************************************************************/
package com.action;

import ade.*;
import java.rmi.*;

/** 
 * <code>ActionComponentArch</code> implements the ActionComponent's
 * runArchitecture method, and whatever supporting code is necessary, for an
 * architecture specified in Java, rather than Action scripts.  Copy and
 * modify this one, then run it like a normal ADE server or pass it as an
 * argument to {@link ade.BootstrapADE BootstrapADE} to run from a JAR file.
 */
public class ActionComponentArchImpl extends ActionComponentImpl implements ActionComponentArch {

    /** 
     * <code>runArchitecture</code> is called periodically to perform
     * whatever sensing and acting required by the architecture.
     */
    public void runArchitecture() {
        // this example just goes until it finds an obstacle in front
        boolean[] obsts = checkObstacles();
        if (obsts[1]) {
            stop();
        } else {
            goStraight();
        }
        return;
    }

    /** 
     * Constructs the ActionComponentArch.
     */
    public ActionComponentArchImpl() throws RemoteException {
        super();
    }
}