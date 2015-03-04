/**
 * ADE 1.0
 * Copyright 1997-2012 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@cs.tufts.edu
 *
 * GoalManagerGoal.java
 *
 * Last update: December 2012
 *
 * @author Paul Schermerhorn
 *
 */

package com.action;

import com.ActionStatus;
import com.Predicate;
import java.util.ArrayList;

/** 
 * <code>GoalManagerGoal</code> encapsulates a goal Predicate to include
 * some additional information.
 */
public class GoalManagerGoal {
    private Predicate goal;
    private long gid;
    private ArrayList<Predicate> waitConds;
    private ArrayList<Predicate> failConds;
    private ActionInterpreter ai;
    private ActionStatus status;
    private GoalManagerGoal parent;
    private long start;
    private long end;
    private boolean terminated;

    protected Predicate getGoal() {
        return goal;
    }

    protected long getGoalId() {
        return gid;
    }

    protected ArrayList<Predicate> getGoalWaitConds() {
        return new ArrayList<Predicate>(waitConds);
    }

    protected ArrayList<Predicate> getGoalFailConds() {
        return new ArrayList<Predicate>(failConds);
    }

    protected void setGoalStatus(ActionStatus as) {
        status = as;
        if (! terminated && ai != null) {
            ai.rootStatus = status;
        }
    }

    protected ActionStatus getGoalStatus() {
        if (! terminated && ai != null) {
            status = ai.rootStatus;
        }
        return status;
    }

    protected GoalManagerGoal getGoalParent() {
        return parent;
    }

    protected long getGoalStartTime() {
        return start;
    }

    protected long getGoalEndTime() {
        return end;
    }

    protected void terminate(ActionStatus s) {
        terminated = true;
        status = s;
        end = System.currentTimeMillis();
        ai = null;
    }

    protected void addGoalWaitCond(Predicate cond) {
        waitConds.add(cond);
    }

    protected void addGoalFailCond(Predicate cond) {
        failConds.add(cond);
    }

    protected void setActionInterpreter(ActionInterpreter ai) {
        this.ai = ai;
    }

    /** 
     * Constructs the GoalManagerGoal.
     * @param g the goal state
     * @param p the parent goal (if one exists, null otherwise)
     */
    public GoalManagerGoal(Predicate g, GoalManagerGoal p) {
        gid = System.currentTimeMillis();
        start = gid;
        goal = g;
        waitConds = new ArrayList<Predicate>();
        failConds = new ArrayList<Predicate>();
        status = ActionStatus.UNKNOWN;
        parent = p;
        terminated = false;
        ai = null;
    }

    /** 
     * Constructs the GoalManagerGoal.
     * @param g the goal state
     */
    public GoalManagerGoal(Predicate g) {
        this(g, null);
    }
}
// vi:ai:smarttab:expandtab:ts=8 sw=4
