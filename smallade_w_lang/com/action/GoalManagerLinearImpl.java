/**
 * ADE 1.0 
 * Copyright 1997-2012 HRILab (http://hrilab.org/) 
 * 
 * All rights reserved.  Do not copy and use without permission. 
 * For questions contact Matthias Scheutz at mscheutz@cs.tufts.edu
 *
 * GoalManagerLinearImpl.java
 *
 * Last update: October 2012
 *
 * @author Paul Schermerhorn
 *
 */
package com.action;

import java.rmi.*;

/** 
 * <code>GoalManagerLinear</code> performs no reprioritization; new goals
 * are pushed onto a stack and action selection is linear, stepping through the
 * events in the provided script until complete.
 */
public class GoalManagerLinearImpl extends GoalManagerImpl implements GoalManagerLinear {

    /** 
     * <code>updatePriorities</code> is called periodically to perform whatever
     * reprioritization of goals is necessary (e.g., because of changes in
     * affective states).
     */
    @Override
    public void updatePriorities() {
        // No goal reprioritization in the linear goal manager
    }

    /** 
     * Constructs the GoalManagerLinearImpl.
     */
    public GoalManagerLinearImpl() throws RemoteException {
        super();
        // PWS: construct these here so we get the right kind for the
        // preemption model (in this case, none)
        faceLock = new ActionResourceLockLinear("faceLock");
        blinkLock = new ActionResourceLockLinear("blinkLock");
        headLock = new ActionResourceLockLinear("headLock");
        motionLock = new ActionResourceLockLinear("motionLock");
        transmitLock = new ActionResourceLockLinear("transmitLock");
        visionLock = new ActionResourceLockLinear("visionLock");
        speechLock = new ActionResourceLockLinear("speechLock");
        lightLock = new ActionResourceLockLinear("lightLock");

        startUpAction();
    }
}
// vi:ai:smarttab:expandtab:ts=8 sw=4
