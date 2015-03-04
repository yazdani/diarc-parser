/**
 * ADE 1.0 
 * Copyright 1997-2012 HRILab (http://hrilab.org/) 
 * 
 * All rights reserved.  Do not copy and use without permission. 
 * For questions contact Matthias Scheutz at mscheutz@cs.tufts.edu
 *
 * Last update: October 2012
 *
 * GoalManagerAffectiveImpl.java
 *
 * @author Paul Schermerhorn
 *
 */

package com.action;

import java.rmi.*;

/** 
 * <code>GoalManagerAffective</code> manages goals based on affect, time
 * remaining, etc.  Hence, action execution may not be linear.
 */
class GoalManagerAffectiveImpl extends GoalManagerImpl 
implements GoalManagerAffective {

    /** 
     * <code>updatePriorities</code> is called periodically to perform whatever
     * reprioritization of goals is necessary (e.g., because of changes in
     * affective states).
     */
    @Override
    public void updatePriorities() {
        long currentTime, elapsedTime, allowedTime;
        double urgency, utility, p, minUrg, maxUrg;
        // iPA and iNA each step through actions, as below--combine, perhaps?
        double pAffect = incrementPositiveAffect(0.05);
        double nAffect = incrementNegativeAffect(0.05);

        currentTime = System.currentTimeMillis();
        logItASL("POSITIVE AFFECT " + pAffect);
        logItASL("NEGATIVE AFFECT " + nAffect);
        synchronized (actions) {
            for (ActionInterpreter a : actions) {
                elapsedTime = currentTime - a.getActionStartTime();
                allowedTime = a.getActionMaxTime();
                if (elapsedTime <= allowedTime) {
                    minUrg = a.getActionMinUrg();
                    maxUrg = a.getActionMaxUrg();
                    urgency = (double)elapsedTime / (double)allowedTime;
                    urgency = urgency * (maxUrg - minUrg) + minUrg;
                    // Alternative 1: pos and neg both influence benefit
                    // p = 1 + pAffect - nAffect;
                    // Alternative 1a: pos and neg both influence benefit
                    p = 1 + pAffect*pAffect - nAffect*nAffect;
                    // Alternative 1b: pos and neg both influence benefit
                    // p = 0.5 + (pAffect*pAffect - nAffect*nAffect) * 0.5;
                    utility = p * a.getActionBenefit();
                    utility -= a.getActionCost();
                    // Alternative 2: pos influences benefit, neg influences cost
                    //utility = pAffect * a.getActionBenefit();
                    //utility -= nAffect * a.getActionCost();
                    a.setActionPriority(urgency * utility);
                    logItASL(a.cmd + " PRIORITY " + urgency * utility);
                    logItASL(a.cmd + " URGENCY " + urgency);
                    //System.out.println(a.cmd + " PRIORITY " + urgency * utility);
                }
                a.decayAffect(0.005, true);
                a.decayAffect(0.005, false);
            }
        }
        // The dec here has to be pretty small, as the updates are frequent
        decrementPositiveAffect(0.0005);
        decrementNegativeAffect(0.0005);
    }

    /** 
     * Constructs the GoalManagerAffectiveImpl.
     */
    public GoalManagerAffectiveImpl() throws RemoteException {
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
