/**
 * ADE 1.0 
 * Copyright 1997-2012 HRILab (http://hrilab.org/) 
 * 
 * All rights reserved.  Do not copy and use without permission. 
 * For questions contact Matthias Scheutz at mscheutz@cs.tufts.edu
 *
 * GoalManager.java
 *
 * Last update: October 2012
 *
 * @author Paul Schermerhorn
 *
 */
package com.action;

import ade.*;
import com.*;
//import edu.asu.sapa.lifted.*;
import java.rmi.*;
import java.util.*;

/**
 * The <code>GoalManager</code> takes care of the details of action
 * selection, allowing its clients to add new goals and providing the next
 * action based on its own prioritization scheme.
 */
public interface GoalManager extends ADEComponent {


    public ActionDBEntry getAction(String type) throws RemoteException;
    public ActionDBEntry getAction(Predicate postCondition) throws RemoteException;
    public Set<String> getActionTypes() throws RemoteException;
    public ArrayList<Predicate> getPostconditions() throws RemoteException;
    public void testLearner() throws RemoteException;


    /**
     * Get the distance from the agent to the named target.
     * @param target the name ot the target
     * @return the distance in meters
     */
    @Deprecated
    public double distanceToTarget(String target) throws RemoteException;

    /**
     * Define an action sequence, with pre- and post-conditions.
     * @param entities typed variables appearing in the definition
     * @param actions a list of actions to be performed (subgoals?)
     * @param pre conditions that must hold for this action sequence to apply
     * @param post the effects of this action sequence
     */
    public void associateMeaning(ArrayList<Variable> entities, ArrayList<Symbol> actions, ArrayList<Symbol> pre, ArrayList<Symbol> post) throws RemoteException;

    /**
     * Submit state updates.
     * @param u a list of updates in Predicate form
     */
    public void submitUpdate(ArrayList<Predicate> u) throws RemoteException;

    

    /**
     * Submit a goal to be achieved.  May be handled directly by Action, if
     * possible, or passed to Sapa if a plan is needed.
     * @param g the new goal
     * @return the goal ID (for checking status)
     */
    public long submitGoal(Predicate g) throws RemoteException;

    /**
     * Submit a goal to be achieved.  May be handled directly by Action, if
     * possible, or passed to Sapa if a plan is needed.
     * @param g the new goal
     * @param m a list of modifiers for the goal (e.g., urgency, parent goal)
     * @return the goal ID (for checking status)
     */
    public long submitGoal(Predicate g, List<Predicate> m) throws RemoteException;

    /**
     * Submit an Open World Quantified Goal.  Basically, when certain world
     * states are detected, that will trigger the instantiation of a
     * hypothetical entity with a given set of properties.  If the entity's
     * existence is verified, then the given goal is instantiated.
     * @param openVars the entity types that trigger the OWQG if detected
     * @param senseVar the entity type to be sensed for
     * @param senseFacts expected attributes of senseVar instances
     * @param senseEffect effect that would verify senseVar's existence
     * @param g the goal associated with the senseVar
     * @param m a list of modifiers for the goal (e.g., urgency, parent goal)
     */
    public void submitQuantifiedGoal(ArrayList<Variable> openVars, Variable senseVar, 
            ArrayList<Predicate> senseFacts, Predicate senseEffect, 
            Predicate g, List<Predicate> m) throws RemoteException;

    /**
     * Query what goals are currently being pursued by the Goal Manager.
     * @return a list of predicates representing the goal
     */
    public ArrayList<Predicate> goalQuery() throws RemoteException;

    /**
     * Query the status of a particular goal.
     * @param g the goal to check on
     * @return a com.ActionStatus object indicating the status
     */
    public ActionStatus goalStatus(long g) throws RemoteException;

    /** 
     * Get failure conditions for a particular goal.
     * @param gid the goal to check on
     * @return a list of conditions that caused the failure
     */
    public ArrayList<Predicate> goalFailConds(long gid) throws RemoteException;

    /* "MemoryComponent" interface */
    /**
     * Assert fact.
     * @param fname the name of the fact
     * @return true if success
     */
    public boolean assertFact(String fname) throws RemoteException;

    /**
     * Assert fact.
     * @param fname the name of the fact
     * @param value the name of the fact
     * @return true if success
     */
    public boolean assertFact(String fname, Object value) throws RemoteException;

    /**
     * Retract fact.
     * @param fname the name of the fact
     * @return true if success
     */
    public boolean retractFact(String fname) throws RemoteException;

    /**
     * Check fact.
     * @param fname the name of the fact
     * @return true if fact present in KB
     */
    public boolean checkFact(String fname) throws RemoteException;

    /**
     * Query fact.
     * @param fname the name of the fact
     * @return the value of the fact if in KB, false otherwise
     */
    public Object queryFact(String fname) throws RemoteException;

    /**
     * Get summary information for goal manager.
     * @return information about the goal manager.
     */
    public GoalManagerInfo getInfo() throws RemoteException;

    /**
     * Get summary information for current goals.
     * @return a list of info structures for the current goals.
     */
    public ArrayList<ActionInterpreterInfo> getGoalInfo() throws RemoteException;

    public Boolean actionExists(Predicate postCondition) throws RemoteException;

    /**
     * Send an action to a goal manager
     * @param adbe the action database entry to send
     * @param groupID the group that the receiving GoalManager component belongs to, e.g. pr2
     * @throws RemoteException
     */
    public void sendAction(ActionDBEntry adbe, String groupID) throws RemoteException;

    public void testSendAction() throws RemoteException;

    /**
     * Receive an action with a goal manager
     * @param adbe the action being received
     * @throws RemoteException
     */
    public void receiveAction(ActionDBEntry adbe) throws RemoteException;
}
