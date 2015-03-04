/**
 * ADE 1.0 
 * Copyright 1997-2012 HRILab (http://hrilab.org/) 
 * 
 * All rights reserved.  Do not copy and use without permission. 
 * For questions contact Matthias Scheutz at mscheutz@cs.tufts.edu
 *
 * ActionInterpreterInfo.java
 *
 * Last update: December 2011
 *
 * @author Paul Schermerhorn
 *
 */

package com.action;

import com.*;
import java.io.*;
import java.util.*;

/** <code>ActionInterpreterInfo</code> is the primary action execution module for
 * the robot.
 */
public class ActionInterpreterInfo implements Serializable {
    Stack<ActionDBEntry> ActionStack = new Stack<ActionDBEntry>();
    String agentname;
    String cmd;
    String currentAction;
    ActionStatus status;
    Predicate goal = null;
    String description;
    long goalID;
    double priority = 0;
    double cost;
    double benefit;
    double maxUrgency = 1.0;
    double minUrgency = 0.0;

    double posAff = 0.0;
    double negAff = 0.0;


    public ActionInterpreterInfo() {
    }
}

// vi:ai:smarttab:expandtab:ts=8 sw=4
