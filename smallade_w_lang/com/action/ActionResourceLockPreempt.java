/**
 * ADE 1.0 
 * Copyright 1997-2012 HRILab (http://hrilab.org/) 
 * 
 * All rights reserved.  Do not copy and use without permission. 
 * For questions contact Matthias Scheutz at mscheutz@cs.tufts.edu
 *
 * ActionResourceLockPreempt.java
 *
 * Last update: October 2012
 *
 * @author Paul Schermerhorn
 *
 */

package com.action;

/** 
 * <code>ActionResourceLockPreempt</code> implements priority-based locks.
 * When a higher-priority AI requests the lock, it preempts the
 * lower-priority AI, but the lower-priority AI blocks while a
 * higher-priority AI holds the lock.
 */
class ActionResourceLockPreempt extends ActionResourceLock {
    // PWS: need to figure out how to reinstate the entire stack of lock
    // acquisitions when the preempted AI gets the lock back.  Otherwise
    // enclosing contexts may operate under the assumption that
    // lower-priority AIs won't slip in actions, when in reality they can.

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
            return retval;
        }

        lock.lock();

        try {
            while (true) {
                // PWS: Should we allow an AI access if its spawning AI has the
                // lock?
                if (owner.empty() || (owner.peek() == actionInt)) {
                    owner.push(actionInt);
                    //System.out.println(cmd + " acquired " + lockName);
                    retval = true;
                    break;
                } else if (owner.peek().getActionPriority() < 
                        actionInt.getActionPriority()) {
                    //System.out.println(cmd + " acquired " + lockName + 
                    //	    ", preempting " + owner.peek().cmd);
                    owner.push(actionInt);
                    retval = true;
                    break;
                } else if (++i == count) {
                    //System.out.println(cmd + " !(acquired) " + lockName + 
                    //	    ", because of " + owner.peek().cmd);
                    break;
                }
                // PWS: This might not be the best choice for nonblocking
                // acquires!
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
    public ActionResourceLockPreempt(String name) {
        super(name);
    }
}

// vi:ai:smarttab:expandtab:ts=8 sw=4
