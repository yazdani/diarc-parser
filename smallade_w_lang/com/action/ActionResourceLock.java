/**
 * ADE 1.0
 * Copyright 1997-2012 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@cs.tufts.edu
 *
 * ActionResourceLock.java
 *
 * Last update: October 2012
 *
 * @author Paul Schermerhorn
 *
 */

package com.action;

import java.util.concurrent.locks.*;
import java.util.Stack;

/**
 * <code>ActionResourceLock</code> is the base class for Action's resource
 * locks.
 */
abstract class ActionResourceLock {
    String lockName;
    protected Stack<ActionInterpreter> owner;
    final Lock lock = new ReentrantLock();
    final Condition released = lock.newCondition();

    /**
     * Must be implemented by subclass.
     **/
    abstract public boolean acquire(ActionInterpreter actionInt, int count);

    /**
     * Acquire the resource lock, waiting indefinitely for it to become
     * available.
     * @param actionInt the ActionInterpreter requesting the lock
     * @return true if the lock is acquired
     */
    public boolean blockingAcquire(ActionInterpreter actionInt) {
        return acquire(actionInt, -1);
    }

    /**
     * Attempt to acquire the resource lock, but do not block if it is
     * unavailable.
     * @param actionInt the ActionInterpreter requesting the lock
     * @return true if the lock is acquired, false otherwise
     */
    public boolean nonBlockingAcquire(ActionInterpreter actionInt) {
        String cmd = actionInt.cmd;
        boolean retval = acquire(actionInt, 1);

        return retval;
    }

    /**
     * Release the resource lock, ensuring that the current AI is the
     * current holder.  This version releases only one level of recursive
     * acquire; if an enclosing script also acquired the lock, actionInt will
     * still hold it.
     * @param actionInt the ActionInterpreter releasing the lock
     */
    public void release(ActionInterpreter actionInt) {
        Stack<ActionInterpreter> uncover = new Stack<ActionInterpreter>();
        String cmd = actionInt.cmd;
        lock.lock();

        try {
            if (!owner.empty() && (owner.peek() == actionInt)) {
                owner.pop();
                if(owner.empty()) {
                    //System.out.println(cmd + " released " + lockName);
                    released.signal();
                } else {
                    //System.out.println(cmd + " popped " + lockName);
                }
            } else if (!owner.empty()) {
                uncover = new Stack<ActionInterpreter>();
                while (!owner.empty() && (owner.peek() != actionInt)) {
                    uncover.push(owner.pop());
                }
                if (!owner.empty()) {
                    //System.out.println(cmd + " must've had " + lockName +
                    //	    " preempted");
                    owner.pop();
                }
                while (!uncover.empty()) {
                    owner.push(uncover.pop());
                }
            } else {
                //System.out.println(cmd + " must've had " + lockName +
                //	" preempted, but nobody owns it now!");
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Release the resource lock, ensuring that the current AI is the
     * current holder.  This version ensures the lock will be released,
     * regardless of how many recursive acquires the owner has made.
     * @param actionInt the ActionInterpreter releasing the lock
     */
    public void deepRelease(ActionInterpreter actionInt) {
        lock.lock();

        try {
            if (!owner.empty() && (owner.peek() == actionInt)) {
                while (!owner.empty())
                    owner.pop();
                //System.out.println(cmd + " (deeply) releasing " + lockName);
                released.signal();
            } else {
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get the interpreter currently holding the lock.
     * @return the owner
     */
    public ActionInterpreter getOwner() {
        ActionInterpreter o = null;
        if (!owner.empty()) {
            o = owner.peek();
        }
        return o;
    }

    @Override
    public String toString() {
        String o = "unowned";
        if (!owner.isEmpty()) {
            o = owner.peek().cmd;
        }
        return lockName + " (" + o + ")";
    }

    /**
     * Constructor for a resource lock.
     * @param name the lock's name
     */
    public ActionResourceLock(String name) {
        lockName = name;
        owner = new Stack<ActionInterpreter>();
    }
}

// vi:ai:smarttab:expandtab:ts=8 sw=4
