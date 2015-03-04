/**
 * ADE 1.0 
 * Copyright 1997-2012 HRILab (http://hrilab.org/) 
 *
 * All rights reserved.  Do not copy and use without permission. 
 * For questions contact Matthias Scheutz at mscheutz@cs.tufts.edu
 *
 * PlannerElement.java
 *
 * Last update: October 2012
 *
 * @author Paul Schermerhorn
 *
 */
package com.action;

import ade.ADEException;
import com.Predicate;
import com.Variable;
import com.google.common.base.Joiner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;

import static java.lang.String.format;
import static utilities.Util.createPredicate;

/** Stores the information needed to instantiate elements of the Sapa planner domain and problem. */
class PlannerElement {
    private static Log log = LogFactory.getLog(PlannerElement.class);
    public static final int TYPE = 0;
    public static final int ACTION = 1;
    public static final int PREDICATE = 2;
    public static final int CONSTANT = 3;
    public static final int STATE = 4;
    public static final int GOAL = 5;
    public static final int OPEN = 6;
    private String name;
    private String type;
    private int plannerType = -1;
    private String supertype = null;
    private float utility = 0.0f;
    private float time = Float.POSITIVE_INFINITY;
    private boolean hardGoal = true;
    private ArrayList<Variable> vars;
    private ArrayList<Predicate> atStarts;
    private ArrayList<Predicate> overAlls;
    private ArrayList<Predicate> startEffects;
    private ArrayList<Predicate> endEffects;
    private ArrayList<Variable> openVars;
    private Variable senseVar = null;
    private ArrayList<Predicate> senseFacts;
    private Predicate senseEffect = null;
    private Predicate openGoal = null;

    public PlannerElement(String n, int t) {
        name = n;
        plannerType = t;
        if (plannerType == ACTION || plannerType == PREDICATE)
            vars = new ArrayList<Variable>();
        if (plannerType == ACTION) {
            atStarts = new ArrayList<Predicate>();
            overAlls = new ArrayList<Predicate>();
            startEffects = new ArrayList<Predicate>();
            endEffects = new ArrayList<Predicate>();
        }
        if (plannerType == OPEN)
            openVars = new ArrayList<Variable>();
        senseFacts = new ArrayList<Predicate>();
    }

    public String getName() {
        return name;
    }

    public String getSupertype() {
        return supertype;
    }

    public void setSupertype(String s) {
        supertype = s;
    }

    public int getPlannerType() {
        return plannerType;
    }

    public String getType() {
        return type;
    }

    public void setPlannerType(String t) {
        type = t;
    }

    public void setSoftGoal() {
        hardGoal = false;
    }

    public void setBenefit(float b) {
        utility = b;
    }

    public void setCost(float c) {
        utility = c;
    }

    public void setDuration(float d) {
        time = d;
    }

    public void setDeadline(float d) {
        time = d;
    }

    public void setOpenGoal(Predicate g) {
        if (openGoal != null)
            log.warn("WARNING: overwriting open goal " + name);
        openGoal = g;
    }

    public void setSenseVar(String n, String t) {
        if (senseVar != null)
            log.warn("WARNING: overwriting sense variable for open goal " + name);
        senseVar = new Variable(n, t);
    }

    public void addSenseFact(Predicate f) {
        senseFacts.add(f);
    }

    public void setSenseEffect(Predicate e) {
        if (senseEffect != null)
            log.warn("WARNING: overwriting sense effect for open goal " + name);
        senseEffect = e;
    }

    public void addOpenVar(String name, String type) {
        openVars.add(new Variable(name, type));
    }

    public void addVar(String name, String type) {
        vars.add(new Variable(name, type));
    }

    public void addAtStart(Predicate p) {
        atStarts.add(p);
    }

    public void addOverAll(Predicate p) {
        overAlls.add(p);
    }

    public void addStartEffect(Predicate p) {
        startEffects.add(p);
    }

    public void addEndEffect(Predicate p) {
        endEffects.add(p);
    }

    public boolean submit(GoalManagerImpl ami) {
        log.debug(format("[submit] :: %s", this));
        try {
            switch (plannerType) {
                case TYPE:
                    addPlannerType(ami);
                    break;
                case ACTION:
                    addPlannerAction(ami);
                    break;
                case PREDICATE:
                    addPlannerPredicate(ami);
                    break;
                case CONSTANT:
                    newPlannerObject(ami);
                    break;
                case STATE:
                    updatePlannerState(ami);
                    break;
                case GOAL:
                    submitPlannerGoal(ami);
                    break;
                case OPEN:
                    submitQuantifiedPlannerGoal(ami);
                    break;
                default:
                    log.warn(format("[submit] :: Unrecognized planner type: %s", plannerType));
                    return false;
            }
        } catch (ADEException e) {
            log.error("[submit] :: Error submitting to planner. ", e);
            return false;
        }
        return true;
    }


    private void submitQuantifiedPlannerGoal(GoalManagerImpl ami) throws ADEException {
        if (time == -1) time = Float.POSITIVE_INFINITY;
        ami.callMethod("submitQuantifiedPlannerGoal", openVars, senseVar, senseFacts, senseEffect, openGoal,
                hardGoal, (double) utility, (double) time);
    }

    private void submitPlannerGoal(GoalManagerImpl ami) throws ADEException {
        ami.callMethod("submitPlannerGoal", createPredicate(name), hardGoal, (double) utility, (double) time);
    }

    private void updatePlannerState(GoalManagerImpl ami) throws ADEException {
        ami.callMethod("updatePlannerState", createPredicate(name), false);
    }

    private void newPlannerObject(GoalManagerImpl ami) throws ADEException {
        GoalManagerImpl.nameIdMap.put(name.toLowerCase(), -1);
        ami.callMethod("newPlannerObject", name, type);
    }

    private void addPlannerPredicate(GoalManagerImpl ami) throws ADEException {
        ami.callMethod("addPlannerPredicate", name, vars);
    }

    private void addPlannerType(GoalManagerImpl ami) throws ADEException {
        ami.callMethod("addPlannerType", name, supertype);
    }

    private void addPlannerAction(GoalManagerImpl ami) throws ADEException {
        ami.callMethod("addPlannerAction", name, utility, time, vars, atStarts, overAlls, startEffects, endEffects);
    }

    @Override
    public String toString() {
        Joiner joiner = Joiner.on(", ");
        switch (plannerType) {
            case TYPE:
                return format("Type %s, supertype %s", name, supertype);
            case ACTION:
                return format("Action %s, Vars: [%s], At Starts: [%s], Overalls: [%s], Start Effects: [%s], End Effects: [%s]",
                        name, joiner.join(vars), joiner.join(atStarts), joiner.join(overAlls), joiner.join(startEffects), joiner.join(endEffects));
            case PREDICATE:
                return format("Predicate %s, Vars: [%s]", name, joiner.join(vars));
            case CONSTANT:
                return format("Constant %s, type: %s", name, type);
            case STATE:
                return format("State: %s", createPredicate(name));
            case GOAL:
                return format("Goal: %s, Hard: %s, Utility: %f, Time: %f", name, String.valueOf(hardGoal), utility, time);
            case OPEN:
                return format("Open goal: %s, Open Vars: [%s], Sense var: %s, Sense facts: [%s], Sense effect: %s, Hard: %s, Utility: %f, Time: %s",
                        openGoal, joiner.join(openVars), senseVar, joiner.join(senseFacts), senseEffect, String.valueOf(hardGoal), utility, time);
            default:
                log.warn(format("[submit] :: Unrecognized planner type: %s", plannerType));
                return "Unknown planner type";
        }
    }
}

