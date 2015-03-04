/**
 * ADE 1.0 
 * Copyright 1997-2012 HRILab (http://hrilab.org/) 
 * 
 * All rights reserved.  Do not copy and use without permission. 
 * For questions contact Matthias Scheutz at mscheutz@cs.tufts.edu
 *
 * GoalManagerPriorityImpl.java
 *
 * Last update: October 2012
 *
 * @author Paul Schermerhorn
 *
 */

package com.action;

import java.rmi.*;

/** 
 * <code>GoalManagerPriorityImpl</code> reprioritizes based on utility and
 * urgency alone; the influence of affect is left out.
 */
public class GoalManagerPriorityImpl extends GoalManagerImpl implements GoalManagerPriority {

    /** 
     * <code>updatePriorities</code> is called periodically to perform whatever
     * reprioritization of goals is necessary (e.g., because of changes in
     * affective states).
     */
    @Override
    public void updatePriorities() {
        long elapsedTime, allowedTime;
        double urgency, utility, minUrg, maxUrg;
        synchronized (actions) {
            for (ActionInterpreter a : actions) {
                elapsedTime = System.currentTimeMillis() - a.getActionStartTime();
                allowedTime = a.getActionMaxTime();
                //System.out.println(a.cmd + " elapsed " + elapsedTime);
                if (elapsedTime <= allowedTime) {
                    minUrg = a.getActionMinUrg();
                    maxUrg = a.getActionMaxUrg();
                    urgency = (double)elapsedTime / (double)allowedTime;
                    urgency = urgency * (maxUrg - minUrg) + minUrg;
                    // PWS: Seems OK if B < C, it'll just get very low P
                    utility = a.getActionBenefit() - a.getActionCost();
                    a.setActionPriority(urgency * utility);
                    //System.out.println(a.cmd + " PRIORITY " + urgency * utility);
                    logItASL(a.cmd + " PRIORITY " + urgency * utility);
                    logItASL(a.cmd + " URGENCY " + urgency);
                }
            }
        }
    }

    /** 
     * Constructs the GoalManagerPriorityImpl.
     */
    public GoalManagerPriorityImpl() throws RemoteException {
        super();
        // PWS: construct these here so we get the right kind for the
        // preemption model (in this case, strict priority-based)
        faceLock = new ActionResourceLockPreempt("faceLock");
        blinkLock = new ActionResourceLockPreempt("blinkLock");
        headLock = new ActionResourceLockPreempt("headLock");
        motionLock = new ActionResourceLockPreempt("motionLock");
        transmitLock = new ActionResourceLockPreempt("transmitLock");
        visionLock = new ActionResourceLockPreempt("visionLock");
        speechLock = new ActionResourceLockPreempt("speechLock");
        lightLock = new ActionResourceLockPreempt("lightLock");

        startUpAction();
    }
}
// vi:ai:smarttab:expandtab:ts=8 sw=4
