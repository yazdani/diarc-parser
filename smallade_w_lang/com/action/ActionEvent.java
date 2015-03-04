/**
 * ADE 1.0 
 * Copyright 1997-2012 HRILab (http://hrilab.org/) 
 * 
 * All rights reserved.  Do not copy and use without permission. 
 * For questions contact Matthias Scheutz at mscheutz@cs.tufts.edu
 *
 * ActionEvent.java
 *
 * Last update: December 2012
 *
 * @author Paul Schermerhorn
 *
 */

package com.action;

import com.*;
import java.io.*;

/**
 * An <code>ActionEvent</code> is a record of an event or state change.
 */
class ActionEvent implements Serializable {
    /** The state this represents */
    private final Predicate state;
    /** The time of the event */
    private final long time;
    /** Negated? */
    private final boolean negated;
    /** The action spec */
    private final String spec;
    /** The goal this satisfies (if applicable) */
    private final GoalManagerGoal goal;

    public Predicate getState() {
        return state;
    }

    public long getTime() {
        return time;
    }

    public boolean getNegated() {
        return negated;
    }

    public String getSpec() {
        return spec;
    }

    public GoalManagerGoal getGoal() {
        return goal;
    }

    /**
     * A new event
     * @param s the state
     * @param n negated?
     * @param p action spec
     * @param g goal this event satisfies
     */
    public ActionEvent(Predicate s, boolean n, String p, GoalManagerGoal g) {
        state = s;
        time = System.currentTimeMillis();
        negated = n;
        spec = p;
        goal = g;
    }
}

// vi:ai:smarttab:expandtab:ts=8 sw=4
