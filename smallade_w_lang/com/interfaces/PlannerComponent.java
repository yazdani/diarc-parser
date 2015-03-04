/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * PlannerComponent.java
 *
 * Planner: methods for submitting goals, updates, etc., to planners
 *
 * @author Paul Schermerhorn
 *
 **/
package com.interfaces;

import ade.ADEComponent;
import com.action.ActionDBEntry;
import java.rmi.*;
import java.util.*;

/**
 * Minimal interface for planner servers.
 */
public interface PlannerComponent extends ADEComponent {

    /**
     * Submit a goal without a deadline.  Individual planner servers may
     * also implement methods that can include deadlines.
     * @param goals a list of goals, encapsulated in Predicates
     */
    public void submitPlannerGoal(ArrayList<com.Predicate> goals) throws RemoteException;

    /**
     * Submit a goal without a deadline.  Individual planner servers may
     * also implement methods that can include deadlines.
     * @param goal the goal, encapsulated in a Predicate
     */
    public void submitPlannerGoal(com.Predicate goal) throws RemoteException;

    /**
     * Retrieve the most recent plan created by Planner.
     * @return an ActionInterpreter script embodying a plan
     */
    public ActionDBEntry getPlan() throws RemoteException;

    /**
     * Submit a list of state updates.  These are usually the result of
     * observations (e.g., detected landmarks, objects).
     * @param conds a list of Predicates containing updates
     * @param replan whether this update should trigger replanning
     */
    public void updatePlannerState(ArrayList<com.Predicate> conds, boolean replan) throws RemoteException;

    /**
     * Submit a single state update.  These are usually the result of
     * observations (e.g., detected landmarks, objects).
     * @param cond a Predicate containing an update
     * @param replan whether this update should trigger replanning
     */
    public void updatePlannerState(com.Predicate cond, boolean replan) throws RemoteException;
}
// vi:ai:smarttab:expandtab:ts=8 sw=4
