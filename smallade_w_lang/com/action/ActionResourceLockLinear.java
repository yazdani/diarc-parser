/**
 * ADE 1.0 
 * Copyright 1997-2012 HRILab (http://hrilab.org/) 
 * 
 * All rights reserved.  Do not copy and use without permission. 
 * For questions contact Matthias Scheutz at mscheutz@cs.tufts.edu
 *
 * ActionResourceLockLinear.java
 *
 * Last update: October 2012
 *
 * @author Paul Schermerhorn
 *
 */

package com.action;

/** 
 * <code>ActionResourceLockLinear</code> implements FCFS locks--no
 * preemption based on priority.
 */
class ActionResourceLockLinear extends ActionResourceLock {

    /** 
     * Make <code>count</code> attempts to acquire this resource lock.
     * @param actionInt the ActionInterpreter requesting the lock
     * @param count the maximum number of attempts to acquire the lock
     * @return true if the lock is successfully acquired, false otherwise
     */
    @Override
    public boolean acquire(ActionInterpreter actionInt, int count) {
        int i = 0;
        String cmd = actionInt.cmd;
        boolean retval = false;

        // This is stupid, but I might as well preserve the semantics...
        if (count == 0) {
            return false;
        }

        lock.lock();

        try {
            while (true) {
                // PWS: Should we allow AIs access if their spawners have the
                // lock?
                // TODO: Here's where the fancy priority checking will happen...
                // Right now this just checks to see if the lock is free or I
                // already have it; need to check if my priority is high
                // enough to override and take the lock away.
                if (owner.empty() || (owner.peek() == actionInt)) {
                    owner.push(actionInt);
                    //System.out.println(cmd + " acquired " + lockName);
                    retval = true;
                    break;
                } else if (++i == count) {
                    break;
                }
                try { 
                    //System.out.println(cmd + " awaiting " + lockName);
                    released.await(); 
                } catch (InterruptedException ie){
                    actionInt.cancel();
                    break;
                } 
            }
        } finally {
            lock.unlock();
        }
        return retval;
    }

    /**
     * Constructor for a resource lock.
     * @param name the lock's name
     */
    public ActionResourceLockLinear(String name) {
        super(name);
    }
}

// vi:ai:smarttab:expandtab:ts=8 sw=4
