/**
 * ADE 1.0 
 * Copyright 1997-2012 HRILab (http://hrilab.org/) 
 * 
 * All rights reserved.  Do not copy and use without permission. 
 * For questions contact Matthias Scheutz at mscheutz@cs.tufts.edu
 *
 * GoalManagerInfo.java
 *
 * Last update: October 2012
 *
 * @author Paul Schermerhorn
 *
 */

package com.action;

import java.io.*;

/** 
 * <code>GoalManagerInfo</code> provides information about the goal
 * manager that's primarily useful for the GUI visualizer.
 */
public class GoalManagerInfo implements Serializable {
    String agentname;
    int numGoals;
    int cycleTime;
    double positiveAffect = 0.0;
    double negativeAffect = 0.0;
    //boolean[] attendPercepts;
    // a list of references, perhaps?

    /** 
     * Constructs the GoalManagerInfo.
     */
    public GoalManagerInfo() {
    }

}
// vi:ai:smarttab:expandtab:ts=8 sw=4
