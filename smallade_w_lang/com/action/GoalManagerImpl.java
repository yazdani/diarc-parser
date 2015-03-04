/**
 * ADE 1.0
 * Copyright 1997-2013 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@cs.tufts.edu
 *
 * GoalManagerImpl.java
 *
 * @author Paul Schermerhorn
 * @author Cody Canning
 *
 *
 */

package com.action;

import ade.*;
import ade.ADEGlobals.RecoveryState;
import ade.gui.ADEGuiVisualizationSpecs;
import com.*;
import com.google.common.base.Joiner;
import com.lrf.feature.Door;
import com.slug.dialog.NLPacket;
import com.slug.nlp.Utterance;
import com.slug.nlp.Type;
import com.vision.stm.MemoryObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;

import static java.lang.String.format;
import static utilities.Util.*;


/** <code>GoalManager</code> performs no reprioritization; new goals
 * are pushed onto a stack and action selection is linear, stepping through the
 * events in the provided script until complete.
 */
abstract public class GoalManagerImpl extends ADEComponentImpl implements GoalManager {
    static Log log = LogFactory.getLog(GoalManagerImpl.class);

    /* ADE-related fields (pseudo-refs, etc.) */
    private static boolean logCPU = false;
    private static boolean stickAround = false;
    protected static Boolean initialized = false;
    private static boolean plannerInit = false;
    protected static boolean gotRefs = false;
    protected static boolean refsReady = false;
    private static final Object initLock = new Object();
    /* Action-specific fields */
    // clients/updaters for remote components
    private ActionInterpreter plannerAI = null;
    private static String initState = null;
    protected static boolean sleepAI = true;
    protected static boolean useLocalLogger = false;

    // active ActionInterpreter instances
    final ArrayList<ActionInterpreter> actions = new ArrayList<ActionInterpreter>();
    // active planner goals
    final ArrayList<GoalManagerGoal> plannerGoals = new ArrayList<GoalManagerGoal>();
    // the global database
    protected static ActionDBEntry adb;
    // persistent list of goals
    protected static HashMap<Long, GoalManagerGoal> goals = new HashMap<Long, GoalManagerGoal>();
    // scripts to instantiate on start-up
    protected static ArrayList<ArrayList<String>> initScripts = new ArrayList<ArrayList<String>>();
    // goals to instantiate on start-up
    protected static ArrayList<Predicate> initGoals = new ArrayList<Predicate>();
    // list of servers, hashed by type
    private static HashMap<String, List<Component>> servers = new HashMap<String, List<Component>>();
    // list of remote methods available, hashed by name and priority-ordered
    private static HashMap<String, PriorityQueue<Component>> globalMethods = new HashMap<String, PriorityQueue<Component>>();
    // list of remote methods that have yet to be associated with a server reference
    protected static final HashSet<String> unboundMethods = new HashSet<String>();
    // default database
    protected static String dbFilename = "com/action/db/actioncore.xml";
    // supplementary databases
    protected static ArrayList<String> dbFileSupp = new ArrayList<String>();
    protected static boolean printAPI = false;
    private static String agentname = "self";
    protected static String Subject = null;

    // TODO: This should be per-component
    private Updater u;
    protected int sliceTime = 0;
    protected int cycleTime = 50;
    static ActionResourceLock faceLock; // facial expression
    static ActionResourceLock blinkLock; // eyelid 
    static ActionResourceLock headLock; // head and eye pose
    static ActionResourceLock motionLock;
    static ActionResourceLock transmitLock;
    static ActionResourceLock visionLock;
    static ActionResourceLock speechLock;
    static ActionResourceLock lightLock;
    // Note: Add all locks to the list in the constructor
    private ArrayList<ActionResourceLock> resourceLocks = new ArrayList<ActionResourceLock>();
    private double positiveAffect = 0.0;
    private double negativeAffect = 0.0;
    protected int descriptive = 0;
    // list of percept types that should be attended to
    protected boolean[] attendPercepts;
    protected static int perceptTypes = 7;
    // Add percept types here and increment value of perceptTypes above
    protected static final int OBST = 0;
    protected static final int BLOB = 1;
    protected static final int DOOR = 2;
    protected static final int BOX = 3;
    protected static final int BLOCK = 4;
    protected static final int LAND = 5;
    protected static final int ROBOT = 6;
    protected double current_x = 0.0;
    protected double current_y = 0.0;
    protected double current_t = 0.0;
    protected long currentSpex = -1L;

    // keeping track of doors and rooms; should be phased out for spatial expert
    // currently-perceived entities
    protected static ArrayList<ADEPercept> currentPercepts;
    // previously-perceived entities
    protected static HashMap<String, ADEPercept> pastPercepts;
    protected static HashMap<Object, Object> nameIdMap;
    protected final static ArrayList<Predicate> stateUpdates = new ArrayList<Predicate>();
    protected static boolean stateUpdatesReplan = false;
    private int currentDoor = 0;
    private int currentDoorCount = 0;
    private long prevDoorTime = 0L;
    private boolean haveDoor = false;
    protected List<Door> doors = null;
    protected Random rangen;

    protected static final double beside_min = (Math.PI*.5) - ((Math.PI*.5)*.15);
    protected static final double beside_max = (Math.PI*.5) + ((Math.PI*.5)*.15);

    protected static boolean simulateFirst = false;
    private HashMap<Predicate, ArrayList<GoalManagerGoal>> postponedGoals = new HashMap<Predicate, ArrayList<GoalManagerGoal>>();

    protected String sphinxGrammar = null;

    private String location = null;

    protected static boolean spexTest = false;

    private boolean conversationState = false;

    private static ArrayList<Predicate> proscribedActions = new ArrayList<Predicate>();
    private static ArrayList<Predicate> proscribedStates = new ArrayList<Predicate>();
    private static ArrayList<ActionEvent> events = new ArrayList<ActionEvent>();

    private ActionLearningManager alm = new ActionLearningManager(this);
    private HashMap<String,String> typeInfo = new HashMap<String,String>();
    // *************************************************************************
    // The methods available to remote objects via RMI (appear in GoalManager)
    // *************************************************************************


    /**
     *@author TEW
     */
    public boolean retractGoal(Long id){
        try{
            goals.remove(id);
        }catch(Exception e){
            return false;
        }
        return true;
    }

    /**
     *@author TEW
     */
    public ActionDBEntry getAction(String type){
        return adb.lookup(type);
    }
    /**
     *@author TEW
     */
    public ActionDBEntry getAction(Predicate postCondition){
        return adb.lookupPost(postCondition);
    }
    /**
     *@author TEW
     */
    public Set<String> getActionTypes(){
            return  adb.getActionTypes();
    }

    public HashMap<String,String> getTypeInfo() {
        return this.typeInfo;
    }


    public ArrayList<Predicate> getPostconditions(){
        return adb.getPostconds();
    }

    /**
     *@author TEW
     */
    public void testLearner(){
        ActionLearner al = new ActionLearner("pickUpMedkit");
        al.addPostCondition(utilities.Util.createPredicate("pickedUpMedkit(self)"), false);
        al.addStep("closeHand self any");
        al.addStep("liftHand self any 1.0 1.0 1.0");
        log.debug(al.getScript());
        submitGoal(utilities.Util.createPredicate("pickedUpMedkit(self)"));
    }

    /**
     * @author GB
     */
    public Boolean actionExists(Predicate postCondition) {
        return (this.getAction(postCondition) != null);
    }

    /**
     * Get the distance from the agent to the named target.  This is mostly for
     * the convenience of servers that don't acquire the necessary references
     * to do it themselves, and (with the advent of the spatial expert) is
     * likely to disappear soon.
     * @param target the name to the target
     * @return the distance in meters
     */
    @Override
    @Deprecated
    public double distanceToTarget(String target) {
        log.warn("Deprecated method: distanceToTarget");
        log.debug("Checking distance to " + target + " " + target.toUpperCase());
        ADEPercept t = pastPercepts.get(target.toUpperCase());

        try {
            double[] position = (double[]) callMethod("getPoseGlobal");
            current_x = position[0];
            current_y = position[1];
            current_t = position[2];
        } catch (ADEException ace) {
            log.error("startTurnRel: problem getting pose: " + ace);
        }
        if (t == null) {
            log.debug("Don't know distance to unknown target: "+target);
            return Double.MAX_VALUE;
        }
        double dist = Math.sqrt((current_x - t.sim_x) * (current_x - t.sim_x) +
                                (current_y - t.sim_y) * (current_y - t.sim_y));
        return dist;
    }

    /**
     * Define an action sequence, with pre- and post-conditions.
     * @param entities typed variables appearing in the definition
     * @param actions a list of actions to be performed (subgoals?)
     * @param pre conditions that must hold for this action sequence to apply
     * @param post the effects of this action sequence
     */
    @ActionCodeNote("Not setting postcond; need to get the type/predicate/etc. mapping settled")
    @Override
    public void associateMeaning(ArrayList<Variable> entities, ArrayList<Symbol> actions, ArrayList<Symbol> pre, ArrayList<Symbol> post) {
        log.trace("enter associateMeaning");
        String name = "associateMeaning_" + System.currentTimeMillis();
        ActionDBEntry noo = new ActionDBEntry(name.toLowerCase(), "script");
        int c = 0;
        float cost = 0.0f;
        float timeout = 0.0f;
        Variable actor = null;
        Variable zone = null;
        Variable doorway = null;
        Variable hallway = null;

        PlannerElement plannerAction = new PlannerElement(name, PlannerElement.ACTION);
        sayText("understood");

        for (Variable v : entities) {
            String nm = v.getName().toLowerCase();
            String tp = v.getType().toLowerCase();

            //TODO: fix this hack
            //fixes mismatching types from discourse
            log.debug("entity: " + nm + " - " + tp);
            if (tp.equals("door")) {
                tp = "doorway";
                doorway = new Variable(nm, tp);
            } else if (tp.equals("listener")) {
                tp = "actor";
                actor = new Variable(nm, tp);
            } else if (tp.equals("room")) {
                tp = "zone";
                zone = new Variable(nm, tp);
            } else if (tp.equals("location")) {
                tp = "hallway";
                hallway = new Variable(nm, tp);
            }
            noo.addRole(nm, tp, null, false);
            plannerAction.addVar(nm, tp);
        }
        /* variable issues with types:
         * door -> doorway, listener -> actor, room -> zone, location -> hallway
         * (try those again as subtypes now that planner is fixed)
         * predicate issues with conditions:
         * open(?door) -> has_property(?door,open_pr)
         */
        for (Symbol p: pre) {
            log.debug("pre: " + p);
            if (p instanceof Predicate) {
                if (p.getName().equals("at")) {
                    p = createPredicate("at(" + actor.getName() + "," + hallway.getName() + ")");
                } else if (p.getName().equals("closed")) {
                    p = createPredicate("has_property(" + doorway.getName() + ",closed_pr)");
                } else if (p.getName().equals("connectedto")) {
                    p = createPredicate("door_connected(" + doorway.getName() + "," + hallway.getName() + "," + zone.getName() + ")");
                    //plannerAction.addOverAll((Predicate)p);
                }
                plannerAction.addAtStart((Predicate)p);
            } else {
                log.error("Error: invalid pre-condition: " + p);
            }
        }
        for (Symbol p: post) {
            ArrayList<Symbol> args = new ArrayList<Symbol>(); // will populate with Variable versions of args, if necessary
            log.debug("post: " + p);
            if (p instanceof Predicate) {
                if (p.getName().equals("in")) {
                    p = createPredicate("not(at(" + actor.getName() + "," + hallway.getName() + "))");
                    noo.addEffect((Predicate)p);
                    plannerAction.addStartEffect((Predicate)p);
                    p = createPredicate("at(" + actor.getName() + "," + zone.getName() + ")");
                } else if (p.getName().equals("open")) {
                    p = createPredicate("has_property(" + doorway.getName() + ",open_pr)");
                    noo.addEffect((Predicate)p);
                    plannerAction.addEndEffect((Predicate)p);
                    p = createPredicate("not(has_property(" + doorway.getName() + ",closed_pr))");
                }
                noo.addEffect((Predicate)p);
                plannerAction.addEndEffect((Predicate)p);
            } else {
                log.error("Error: invalid post-condition: " + p);
            }
        }
        for (Symbol p : actions) {
            log.debug("action: " + p);
            if (!(p instanceof Predicate)) {
                log.error("Error: invalid action description: " + p);
                continue;
            }
            Predicate a = (Predicate)p;
            // PWS: this is simplistic, and could duplicate predicates
            String aSpec = a.getName();
            // PWS: looking up by name -- assumes the "predicate" is actually an action;
            // should check by post-condition (i.e., treat it as a goal)
            ActionDBEntry act = ActionDBEntry.lookup(aSpec);
            cost += act.getCost();
            timeout += act.getTimeout();
            for (Symbol s : a.getArgs()) {
                if (a.getClass().isInstance(s)) {
                    c = addPredSpec((Predicate)s, noo, c);
                    aSpec = aSpec + " !pred" + c;
                } else {
                    aSpec = aSpec + " " + s;
                }
            }
            noo.addEventSpec(aSpec);
            //noo.addEventSpec("printText \"Completed " + aSpec + "\"");
        }
        timeout /= 1000;
        noo.addTimeout((long)timeout);
        plannerAction.setDuration(timeout);
        noo.addCost(cost + ""); // why is this a string???
        plannerAction.setCost(cost);
        // wait until the script is complete to add it to the postcond index
        //noo.addPostcondToDB(pstring);
        plannerAction.submit(this);
        log.debug("completed action: " + noo);
    }

    /**
     * Submit state updates.
     * @param u a list of updates in Predicate form
     */
    @Override
    public void submitUpdate(ArrayList<Predicate> u) {
        log.trace("enter submitUpdate");
        processStateUpdate(u, true);
    }

    /**
     * Submit a goal to be achieved.  May be handled directly by Action, if
     * possible, or passed to Planner if a plan is needed.
     * @param g the new goal
     * @return the goal ID (for checking status)
     */
    @Override
    public long submitGoal(Predicate g) {
        log.trace("enter submitGoal(Predicate g)");
        log.debug("Submitting goal: "+g.getName());
        return submitGoal(g, new ArrayList<Predicate>());
    }

    /**
     * Submit a goal to be achieved.  May be handled directly by Action, if
     * possible, or passed to Planner if a plan is needed.
     * @param g the new goal
     * @param m a list of modifiers for the goal (e.g., urgency, parent goal)
     * @return the goal ID (for checking status)
     */
    @Override
    public long submitGoal(Predicate g, List<Predicate> m) {
        log.trace("enter submitGoal(Predicate g, List<Predicate> m");
        // Check to see whether Action knows how to make this happen
        // already; if so, instantiate an AI to do it, otherwise, send it 
        // to the planner.
        canLogIt("submitGoal: " + g);
        // DEONTIC: check goal state against proscribedStates here
        if (badState(g)) {
            log.debug("NOT DOING ANYTHING WITH SUBMITTED GOALS YET@!");
        }
        log.debug("submitGoal: " + g);
        if (g.getName().equals("have")) {
            ArrayList<Symbol> args = new ArrayList<Symbol>();
            log.debug("     args: " + g.get(0).getClass() + " " + g.get(1).getClass());
            args.add(g.get(1));
            args.add(g.get(0));
            Predicate ng = new Predicate(g.getName(), args);
            g = ng;
        }

        //log.debug("          : " + g.get(0).getName() + " - " + ((Variable)g.get(0)).getType());
        //log.debug("          : " + g.get(1).getName() + " - " + ((Variable)g.get(1)).getType());
        // PROCESS THE LIST OF MODIFIERS!
        for (Predicate mod:m) {
            log.debug("       mod: " + mod);
        }

        /* ** NEED TO TRY SIMULATE FIRST ** */
        // see if this goal's already being pursued
        synchronized (actions) {
            // check current actions
            for (ActionInterpreter a : actions) {
                if (a.getGoal() == null) {
                    continue;
                }
                if (g.equals(a.getGoal())) {
                    log.debug("found match!");
                    return a.getGoalID();
                }
            }
            // check planner goals (can be multiple for one AI)
            for (GoalManagerGoal gmg:plannerGoals) {
                if (g.equals(gmg.getGoal())) {
                    return gmg.getGoalId();
                }
            }
        }
        GoalManagerGoal gmg = new GoalManagerGoal(g);
        submitGoal(gmg);
        return gmg.getGoalId();
    }

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
    @Override
    public void submitQuantifiedGoal(ArrayList<Variable> openVars, Variable senseVar,
                                     ArrayList<Predicate> senseFacts, Predicate senseEffect,
                                     Predicate g, List<Predicate> m) {

        log.trace("enter submitQuantifiedGoal(...)");
        HashMap<String,Variable> vars = new HashMap<String,Variable>();
        ArrayList<Symbol> args;
        boolean hardGoal = true;
        float utility = 1000.0F;
        float deadline = -1.0F;
        // TODO: check the open and sense arguments to see if anything's missing
        // TODO: apply modifiers
        // TODO: create a gmg for the goal, try to detect if/when it's achieved
        // check each var to see if it's a location, and if so, consult spex
        // check sense var to see if it's known, and if not and null facts, ask about appearance
        // make sure there's an action with the effect

        if (! checkMethod("submitQuantifiedPlannerGoal")) {
            return;
        }

        // the open variables need to be variables
        for (Variable v:openVars) {
            vars.put(v.getName(),v);
            if (!v.getName().startsWith("?")) {
                v.setName("?var"+v.getName());
                vars.put(v.getName(),v);
            }
        }
        vars.put(senseVar.getName(),senseVar);
        // the sense variable needs to be a variable
        if (!senseVar.getName().startsWith("?")) {
            senseVar.setName("?var"+senseVar.getName());
            vars.put(senseVar.getName(),senseVar);
        }
        for (Predicate p:senseFacts) {
            args = p.getArgsRef();
            for (int i = 0; i < args.size(); i++) {
                Variable v = null;
                if ((v = vars.get(args.get(i).getName())) != null) {
                    args.set(i, v);
                }
            }
        }


        args = senseEffect.getArgsRef();
        for (int i = 0; i < args.size(); i++) {
            Variable v = null;
            if ((v = vars.get(args.get(i).getName())) != null) {
                args.set(i, v);
            }
        }
        if (senseEffect.getName().equals("found")) {
            senseEffect.setName("looked_for");
        }
        if (g.getName().equals("have")) {
            args = new ArrayList<Symbol>();
            log.debug("     args: " + g.get(0).getClass() + " " + g.get(1).getClass());
            args.add(g.get(1));
            args.add(g.get(0));
            Predicate ng = new Predicate(g.getName(), args);
            g = ng;
        }
            // HACK
        String type = vars.get(args.get(0).getName()).getType();
        List<Predicate> descs = new ArrayList<Predicate>();
        Predicate name = createPredicate("type(x,"+type+")");
        try {
            Long descId = (Long) callMethod("getTypeId", name);
            descs = (List<Predicate>)callMethod("getDescriptors", descId);
        } catch (ADEException ace) {
            System.err.println(ace);
            ace.printStackTrace();
        }
        if (descs.isEmpty()) {
            Predicate q = createPredicate("itk(self,needsVisionType("+type+"))");
            log.debug("Sending NLG request for: " + q);
            try {
                callMethod("submitNLGRequest", new NLPacket(new Utterance(q)));
            } catch (ADEException ace) {
                System.err.println(myID + ": error submitting NLG request: " + ace);
                ace.printStackTrace();
            }
        }

        args = g.getArgsRef();
        for (int i = 0; i < args.size(); i++) {
            Variable v = null;
            if ((v = vars.get(args.get(i).getName())) != null) {
                args.set(i, v);
            } else {
                try {
                    if (args.get(i).getClass() == Class.forName("com.Symbol")) {
                        // must be a literal
                        Symbol s = new Symbol(args.get(i).getName().toLowerCase());
                        args.set(i,s);
                    }
                } catch (ClassNotFoundException cnfe) {
                    System.err.println("Couldn't find class for com.Symbol...");
                }
            }
        }
        if (deadline == -1) {
            deadline = Float.POSITIVE_INFINITY;
        }

        log.debug("Submitting open: " + g);
        for (Variable v:openVars)
            log.debug("    openvar: " + v);
        log.debug("    senseVar: " + senseVar);
        for (Predicate p:senseFacts)
            log.debug("    sensefact: " + p);
        log.debug("    senseeffect: " + senseEffect);
        log.debug("    hard: " + hardGoal);
        log.debug("    utility: " + utility);
        log.debug("    deadline: " + deadline);

        for (Predicate mod:m) {
            log.debug("    mod: " + mod);
        }
        try {
            callMethod("submitQuantifiedPlannerGoal", openVars, senseVar,
                    senseFacts, senseEffect, g, hardGoal, (double)utility, (double)deadline);
        } catch (ADEException ace) {
            log.error(myID + ": exception submitting quantified goal", ace);
        }
    }

    /**
     * Query what goals are currently being pursued by the Goal Manager.
     * @return a list of predicates representing the goals
     */
    @Override
    public ArrayList<Predicate> goalQuery() {
        log.trace("enter goalQuery");
        ArrayList<Predicate> goalList = new ArrayList<Predicate>();
        synchronized (actions) {
            for (ActionInterpreter ai : actions) {
                goalList.add(ai.getGoal());
            }
        }
        return goalList;
    }

    /**
     * Query what goals are currently being pursued by the Goal Manager.
     * @param g the goal to query
     * @return the goal's ID, if present, -1 otherwise
     */
    public long goalQuery(Predicate g) {
        log.trace("enter goalQuery(Predicate g");
        // this looks at current actions on purpose; past goals can be queried
        // by ID (see goalStatus, etc., below).
        synchronized (actions) {
            log.debug("checking for new goal: " + g);
            for (ActionInterpreter ai : actions) {
                log.debug("comparing with current goal: " + ai.getGoal());
                if ((ai.getGoal() != null) && ai.getGoal().equals(g)) {
                    return ai.getGoalID();
                }
            }
        }
        return -1;
    }

    /**
     * Query the status of a particular goal.
     * @param gid the goal to check on
     * @return a com.ActionStatus object indicating the status
     */
    @Override
    public ActionStatus goalStatus(long gid) {
        log.trace("enter goalStatus(long gid)");
        if (goals.containsKey(gid)) {
            return goals.get(gid).getGoalStatus();
        } else {
            return ActionStatus.UNKNOWN;
        }
    }

    /**
     * Get failure condition for a particular goal.
     * @param gid the goal to check on
     * @return a string indicating the cause of failure
     */
    @Override
    public ArrayList<Predicate> goalFailConds(long gid) {
        log.trace("enter goalFailConds(long gid)");
        if (goals.containsKey(gid)) {
            return goals.get(gid).getGoalFailConds();
        } else {
            ArrayList<Predicate> conds = new ArrayList<Predicate>();
            conds.add(createPredicate("reason(unknown)"));
            return conds;
        }
    }

    /* "MemoryComponent" interface */
    /**
     * Assert fact.
     * @param fname the name of the fact
     * @return true if success
     */
    @Override
    public boolean assertFact(String fname) {
        log.trace("enter assertFact(String fname)");
        return ActionDBEntry.assertFact(fname);
    }

    /**
     * Assert fact.
     * @param fname the name of the fact
     * @param value the name of the fact
     * @return true if success
     */
    @Override
    public boolean assertFact(String fname, Object value) {
        log.trace("enter assertFact(String fname, Object value");
        return ActionDBEntry.assertFact(fname, value);
    }

    /**
     * Retract fact.
     * @param fname the name of the fact
     * @return true if success
     */
    @Override
    public boolean retractFact(String fname) {
        log.trace("enter retractFact(String fname)");
        return ActionDBEntry.retractFact(fname);
    }

    /**
     * Check fact.
     * @param fname the name of the fact
     * @return true if fact present in KB
     */
    @Override
    public boolean checkFact(String fname) {
        log.trace("enter checkFact(String fname)");
        ActionDBEntry factEnt;
        factEnt = ActionDBEntry.lookup(fname.toLowerCase());
        return (factEnt != null);
    }

    /**
     * Query fact.
     * @param fname the name of the fact
     * @return the value of the fact if in KB, false otherwise
     */
    @Override
    public Object queryFact(String fname) {
        log.trace("enter queryFact(String fname)");
        Object value;
        ActionDBEntry factEnt;
        factEnt = ActionDBEntry.lookup(fname.toLowerCase());
        if (factEnt != null) {
            value = factEnt.getValue();
        } else {
            value = (Object) false;
        }
        return value;
    }

    /**
     * Get summary information for goal manager.
     * @return information about the goal manager.
     */
    @Override
    public GoalManagerInfo getInfo() {
        log.trace("enter getInfo()");
        GoalManagerInfo info = new GoalManagerInfo();

        info.numGoals = actions.size();
        info.agentname = agentname;
        info.cycleTime = cycleTime;
        info.positiveAffect = positiveAffect;
        info.negativeAffect = negativeAffect;

        return info;
    }

    /**
     * Get summary information for current goals.
     * @return a list of info structures for the current goals.
     */
    @Override
    public ArrayList<ActionInterpreterInfo> getGoalInfo() {
        log.trace("enter getGoalInfo()");
        ArrayList<ActionInterpreterInfo> info = new ArrayList<ActionInterpreterInfo>();
        for (ActionInterpreter ai : actions) {
            info.add(ai.getInfo());
        }
        return info;
    }

    // *************************************************************************
    // General ADE methods
    // *************************************************************************

    /**
     * This method will be activated whenever a client calls the
     * requestConnection(uid) method. Any connection-specific initialization
     * should be included here, either general or user-specific.
     * @param user the name of the client connecting
     */
    @Override
    protected void clientConnectReact(final String user) {
        log.trace("enter clientConnectReact(final String user)");
        log.debug(myID + ": client " + user + " connected.");
    }

    /**
     * This method will be activated whenever a client that has called the
     * requestConnection(uid) method fails to update (meaning that the
     * heartbeat signal has not been received by the reaper), allowing both
     * general and user specific reactions to lost connections. If it returns
     * true, the client's connection is removed.
     * @param user the name of the client that went down
     */
    @Override
    protected boolean clientDownReact(final String user) {
        log.trace("enter clientDownReact(final String user)");
        log.debug(myID + ": client " + user + " down.");
        return false;
    }

    /**
     * This method will be activated whenever the heartbeat returns a
     * remote exception (j.e., the server this is sending a
     * heartbeat to has failed).
     */
    @Override
    protected void componentDownReact(String serverkey, String[][] constraints) {
        log.trace("enter componentDownReact(String serverkey, String[][] constraints)");
        String s = constraints[0][1];

        log.debug(myID + ": connection to server " + serverkey + " down.");
        String sTypeTmp = getTypeFromID(serverkey);
        for (int i = 0; i < constraints.length; i++) {
            if (constraints[i][0].equals("type")) {
                sTypeTmp = constraints[i][1];
            }
        }
        final String sType = sTypeTmp;
        String sNameTmp = getNameFromID(serverkey);
        for (int i = 0; i < constraints.length; i++) {
            if (constraints[i][0].equals("name")) {
                sNameTmp = constraints[i][1];
            }
        }
        final String sName = sNameTmp;
        List<Component> sList = servers.get(sType);
        for (Component srv : sList) {
            if (srv.getName().equals(sName)) {
                srv.setStatus(false);
                break;
            }
        }
    }

    /**
     * This method will be activated whenever the heartbeat reconnects
     * to a client (e.g., the server this is sending a heartbeat to has
     * failed and then recovered). <b>Note:</b> the pseudo-reference will
     * not be set until <b>after</b> this method is executed. To perform
     * operations on the newly (re)acquired reference, you must use the
     * <tt>sref</tt> parameter object.
     * @param serverkey the ID of the {@link ade.ADEComponent ADEComponent} that connected
     * @param sref the pseudo-reference for the requested server
     * @param constraints the set of constraints associated with this server
     */
    @Override
    protected void componentConnectReact(String serverkey, final Object sref, String[][] constraints) {
        log.trace("enter componentConnectReact(String serverkey, final Object sref, String[][] constraints)");
        final String s = constraints[0][1];

        log.debug(myID + ": connection to server " + serverkey + " established.");
        if (s.indexOf("Sphinx4Component") >= 0) {
            if (sphinxGrammar != null) {
                log.debug("Restoring Sphinx grammar");
                try {
                    call(0, sref, "changeSphinx4Grammar", sphinxGrammar);
                } catch (ADEException ace) {
                    log.error("Error connecting to Sphinx: " + ace);
                }
            }
        }

        /* only add servers I've asked for...
        for (int j = 0; j < constraints.length; j++) {
            if (! constraints[j][0].equals("type"))
                continue;
            //s = getTypeFromID(serverkey);
            s = constraints[j][1];
            if (! refs.containsKey(s)) {
                System.out.println("Component type " + s + " not requested...");
            }
            r = refs.get(s);
            while (r == null) {
                Sleep(50);
                r = refs.get(s);
            }
            System.out.println("Adding " + s + " to the good ref list");
            goodRefs.put(s, r);
            addMethods(s, r);
        }
        */
        // default type is server interface
        String sTypeTmp = getTypeFromID(serverkey);
        // however, should check to see whether the server was requested via
        // another interface (e.g., com.interfaces....)
        for (int i = 0; i < constraints.length; i++) {
            log.debug("constraint " + i + ": " + Arrays.toString(constraints[i]));
            if (constraints[i][0].equals("type")) {
                sTypeTmp = constraints[i][1];
            }
        }
        final String sType = sTypeTmp;
        String sNameTmp = getNameFromID(serverkey);
        for (int i = 0; i < constraints.length; i++) {
            log.debug("constraint " + i + ": " + Arrays.toString(constraints[i]));
            if (constraints[i][0].equals("name")) {
                sNameTmp = constraints[i][1];
            }
        }
        final String sName = sNameTmp;
        List<Component> sList = servers.get(sType);
        Component srv = null;
        // step through priority list
        for (Iterator<Component> it = sList.iterator(); it.hasNext();) {
            srv  = it.next();
            if (srv.getReference() == sref) { // PWS: I'm a little worried about this...
                srv.setName(sName);
                srv.setStatus(true);
                break;
            }
            log.debug("server " + sType + "$" + sName + " doesn't match");
        }
        final Component server = srv;
        new Thread() {
            @Override
            public void run() {
                synchronized (initLock) {
                    addMethods(sType, server);
                }
            }
        }.start();
    }

    @Override
    protected void componentNotify(Object ref, ADEGlobals.RecoveryState recState) {
        log.trace("enter componentNotify(Object ref, ADEGlobals.RecoveryState recState)");
        log.debug("componentNotify: server state " + recState);
        if (getRefID(ref).indexOf("DiscourseComponent") >= 0) {
            if (recState.equals(RecoveryState.UNREC)) {
                // Here I need to kill the executing scripts and head home
                log.debug("Notified of UNRECOVERABLE failure!");
            } else {
                log.debug("Notified of (not UNRECOVERABLE) failure!");
            }
        }
    }

    /**
     * Adds additional local checks for credentials before allowing a shutdown
     * must return "false" if shutdown is denied, true if permitted
     */
    @Override
    protected boolean localrequestShutdown(Object credentials) {
        log.trace("enter localrequestShutdown(Object credentials)");
        return false;
    }

    /**
     * Implements the local shutdown mechanism that derived classes need to
     * implement to cleanly shutdown
     */
    @Override
    protected void localshutdown() {
        log.trace("enter localShutdown()");
        log.info("Shutting down Goal Manager ...");
        log.debug("closing buffered reader...");
        log.debug("waking all sleepers...");
        synchronized (actions) {
            for (ActionInterpreter a : actions) {
                a.interrupt();
            }
        }
        log.info("halting ActionInterpreters...");
        synchronized (actions) {
            for (ActionInterpreter a : actions) {
                a.cancel();
            }
        }
        log.debug("ActionInterpreters halted...");
        log.debug("stopping thread...");
        u.halt();
        log.debug("thread stopped...");
        log.debug("closing logs...");
        if (useLocalLogger) {
            setASL(false);
        }
        if (checkMethod("stop")) {
            try {
                log.debug("Stopping velocity servers");
                callMethod("stop");
            } catch (ADEException ace) {
                log.error("Unable to send halt to MoPo!", ace);
            }
        }
        log.info("... DONE!");
    }

    protected void dialogueAck(Symbol semantics, Type type){
        try{
            Utterance utt = new Utterance(semantics, type);
            utt.initializeSpeaker("self");
            utt = (Utterance)callMethod("fillListenerField",utt);
            callMethod("submitNLGRequest",new NLPacket(10,utt));
        }catch(Exception e){
            log.fatal("Problem generating ack ",e);
        }
    }
    protected void dialogueAck(String semantics, Type type){
        try{
            Utterance utt = new Utterance(new ArrayList<String>(Arrays.asList(semantics.split(" "))), type);
            utt.initializeSpeaker("self");
            utt = (Utterance)callMethod("fillListenerField",utt);
            callMethod("submitNLGRequest",new NLPacket(10,utt));
        }catch(Exception e){
            log.fatal("Problem generating ack ",e);
        }
    }
    
    /**
     * Log a message using ADE Component Logging, if possible.  The ASL facility
     * takes care of adding timestamp, etc.
     * @param o the message to be logged
     */
    protected void logItASL(Object o) {
        //log.trace("enter logItASL(Object o)");
        canLogIt(o);
    }

    /**
     * Set the state of ADE Component Logging.  When true and logging is not
     * started, this starts logging.  When false and logging is started, this
     * stops logging and causes the log files to be written to disk.  ADE server
     * logging is a global logging facility, so starting logging here enables
     * logging in a currently instantiated ADE servers.  Note: You want to stop
     * ADE server logging before quitting, or the files will not be complete.
     * @param state indicates whether to start (true) or stop (false) logging.
     */
    protected void setASL(boolean state) {
        log.trace("enter setASL(boolean state)");
        try {
            setADEComponentLogging(state);
        } catch (Exception e) {
            log.error("FAILURE: setASL", e);
        }
    }

    @Override
    protected boolean localServicesReady() {
        return initialized;
    }

    /**
     * Callback method; when this server registers to be notified of servers
     * and a new server matches the description given, the registry will call
     * this method with the new server's key.  Note that if you need to do any
     * work (e.g., beyond setting flags) in response to the new server, you
     * MUST spawn a thread and allow this method to return.
     * @param newserverkey the new server's key
     */
    @Override
    public void notifyComponentJoined(final String newserverkey) {
        log.trace("enter notifyComponentJoined(final String newserverkey)");
        final String sType = getTypeFromID(newserverkey);
        final String sName = getNameFromID(newserverkey);
        log.debug("Component " + newserverkey + " joined " + this.getClass().getName());
        //if (! getNameFromID(newserverkey).equals(getNameFromID(myID)))
        //    return;
        // policy assumption: pre-emptively get the ref for future
        new Thread() {
            @Override
            public void run() {
                List<Component> sList = servers.get(sType);
                // don't want to request reference for this server if we've 
                // already requested a reference
                if (sList != null) {
                    // requestInitialRefs will get it
                    return;
                }
                // see if the new server satisfies any of the missing methods
                // we'd like to be able to call
                if (checkUnboundMethods(sType)) {
                    // then connect to that server
                    Component s = addComponent(getTypeFromID(newserverkey),getNameFromID(newserverkey));
                    log.debug(myID + " requesting server: " + s);
                    requestInitialRef(s);
                }
            }
        }.start();
    }

    /**
     * Provide additional information for usage...
     */
    @Override
    protected String additionalUsageInfo() {
        log.trace("enter additionalUsageInfo()");
        StringBuilder sb = new StringBuilder();
        sb.append("Component-specific options:\n\n");
        //sb.append("  -fest               <request Festival server>\n");
        //sb.append("  -festversion v      <request specified Festival server>\n");
        //sb.append("  -mopo               <request MoPo server>\n");
        //sb.append("  -pioneer            <request Pioneer server>\n");
        //sb.append("  -segway             <request Segway server>\n");
        //sb.append("  -cart               <request Cart server>\n");
        //sb.append("  -simbad             <request Simbad server>\n");
        //sb.append("  -adesim             <request ADESim server>\n");
        //sb.append("  -simpio             <request com.adesim.SimPioneerComponent>\n");
        //sb.append("  -usarsim            <request USARSim server>\n");
        //sb.append("  -motion             <request Motion server>\n");
        //sb.append("  -motversion v       <request specified Motion server>\n");
        //sb.append("  -lrf                <request LRF server>\n");
        //sb.append("  -urg                <request URG lrf server>\n");
        //sb.append("  -sick               <request SICK lrf server>\n");
        //sb.append("  -gps                <request GPS server>\n");
        //sb.append("  -fld, -field        <request Fld server>\n");
        //sb.append("  -fldversion v       <request specified Fld server>\n");
        //sb.append("  -video              <request Video server>\n");
        //sb.append("  -vision             <request Vision server>\n");
        //sb.append("  -reddy              <request Reddy server>\n");
        //sb.append("  -torso              <request Reddy-type torso server>\n");
        //sb.append("  -survey             <request Survey server>\n");
        //sb.append("  -sonic              <request Sonic server>\n");
        //sb.append("  -sonic2             <request Sonic2 server>\n");
        //sb.append("  -manyears           <request ManyEars server>\n");
        //sb.append("  -sphinx             <request Sphinx4 server>\n");
        //sb.append("  -simspeech          <request SimSpeechGui server>\n");
        //sb.append("  -hand               <request Hand server>\n");
        //sb.append("  -joystick           <request Joystick server>\n");
        //sb.append("  -gogo               <request GoGo server>\n");
        //sb.append("  -sapa               <request Sapa server>\n");
        //sb.append("  -dialogue           <request Dialogue server>\n");
        //sb.append("  -discourse          <request Discourse server>\n");
        //sb.append("  -discversion v      <request specified Discourse server>\n");
        //sb.append("  -nlpserver          <request NLP server>\n");
        //sb.append("  -nosleep            <disable sleep in AI loop>\n");
        sb.append("  -agentname name     <set agent name>\n");
        sb.append("  -subject name       <set subject name>\n");
        sb.append("  -script s [a]       <run init script s with args [a]>\n");
        sb.append("  -dbfilesupp f       <parse supplemental db file f>\n");
        //sb.append("  -api                <print script api information>\n");
        //sb.append("  -aiverb n           <set AI verbosity to n>\n");
        //sb.append("  -logcpu             <log CPU usage>\n");
        sb.append("  -component name        <acquire reference to named component>\n");
        return sb.toString();
    }

    /**
     * Parse additional command-line arguments
     * @param args the list of command-line arguments
     * @return "true" if parse is successful, "false" otherwise
     */
    @Override
    protected boolean parseadditionalargs(String[] args) {
        log.trace("enter parseadditionalargs(String[] args)");
        for (int i = 0; i < args.length; i++) {
            // Named component
            if (args[i].equalsIgnoreCase("-component")) {
                i = addComponent(args[++i], args, i);
            // Components
            } else if (args[i].equalsIgnoreCase("-badaction")) {
                Predicate bad = createPredicate(args[++i]);
                proscribedActions.add(bad);
            } else if (args[i].equalsIgnoreCase("-badstate")) {
                Predicate bad = createPredicate(args[++i]);
                proscribedStates.add(bad);
            } else if (args[i].equalsIgnoreCase("-fest") ||
                    args[i].equalsIgnoreCase("-festival")) {
                i = addComponent("com.festival.FestivalComponent", args, i);
            } else if (args[i].equalsIgnoreCase("-festversion")) {
                log.debug(myID + ": using Festival component " + args[i + 1]);
                i = addComponent(args[++i], args, i);
            } else if (args[i].equalsIgnoreCase("-urg")) {
                i = addComponent("com.lrf.UrgLRFComponent", args, i);
            } else if (args[i].equalsIgnoreCase("-sick")) {
                i = addComponent("com.lrf.SickLRFComponent", args, i);
            } else if (args[i].equalsIgnoreCase("-motion")) {
                i = addComponent("com.motion.MotionComponent", args, i);
            } else if (args[i].equalsIgnoreCase("-motversion")) {
                log.debug(myID + ": using Motion component " + args[i + 1]);
                i = addComponent(args[++i], args, i);
            } else if (args[i].equalsIgnoreCase("-cart")) {
                i = addComponent("com.cart.CartComponent", args, i);
            } else if (args[i].equalsIgnoreCase("-pioneer")) {
                i = addComponent("com.pioneer.PioneerComponent", args, i);
            } else if (args[i].equalsIgnoreCase("-segway")) {
                i = addComponent("com.segway.SegwayComponent", args, i);
            } else if (args[i].equalsIgnoreCase("-adesim")) {
                i = addComponent("com.adesim.ADESimComponent", args, i);
            } else if (args[i].equalsIgnoreCase("-simpio")) {
                i = addComponent("com.adesim.SimPioneerComponent", args, i);
            } else if (args[i].equalsIgnoreCase("-usarsim")) {
                i = addComponent("com.usarsim.USARSimComponent", args, i);
            } else if (args[i].equalsIgnoreCase("-mopo")) {
                i = addComponent("com.mopo.MoPoComponent", args, i);
            } else if (args[i].equalsIgnoreCase("-gps")) {
                i = addComponent("com.gps.GPSComponent", args, i);
            } else if (args[i].equalsIgnoreCase("-fld")
                    || args[i].equalsIgnoreCase("-field")) {
                i = addComponent("com.mopo.FldComponent", args, i);
            } else if (args[i].equalsIgnoreCase("-fldversion")) {
                log.debug(myID + ": using Fld component " + args[i + 1]);
                i = addComponent(args[++i], args, i);
                i = addComponent("com.mopo.FldComponent", args, i);
            } else if (args[i].equalsIgnoreCase("-video")) {
                i = addComponent("com.video.VideoComponent", args, i);
            } else if (args[i].equalsIgnoreCase("-vision")) {
                i = addComponent("com.vision.VisionComponent", args, i);
            } else if (args[i].equalsIgnoreCase("-visionversion")) {
                log.debug(myID + ": using Vision component " + args[i + 1]);
                i = addComponent(args[++i], args, i);
            } else if (args[i].equalsIgnoreCase("-reddy")) {
                i = addComponent("com.reddy.ReddyComponent", args, i);
            } else if (args[i].equalsIgnoreCase("-reddyversion")) {
                i = addComponent(args[++i], args, i);
            } else if (args[i].equalsIgnoreCase("-torso")) {
                i = addComponent("com.reddy.Humanoid", args, i);
            } else if (args[i].equalsIgnoreCase("-survey")) {
                i = addComponent("com.survey.SurveyComponent", args, i);
            } else if (args[i].equalsIgnoreCase("-manyears")) {
                i = addComponent("com.manyears.ManyEarsComponent", args, i);
            } else if (args[i].equalsIgnoreCase("-sphinx")) {
                i = addComponent("com.sphinx4.Sphinx4Component", args, i);
            } else if (args[i].equalsIgnoreCase("-simspeech")) {
                i = addComponent("com.simspeech.SimSpeechGuiComponent", args, i);
            } else if (args[i].equalsIgnoreCase("-hand")) {
                i = addComponent("com.hand.HandComponent", args, i);
            } else if (args[i].equalsIgnoreCase("-dance")) {
                i = addComponent("com.reddy.DanceComponent", args, i);
            } else if (args[i].equalsIgnoreCase("-joystick")) {
                i = addComponent("com.joystick.JoystickComponent", args, i);
            } else if (args[i].equalsIgnoreCase("-gogo")) {
                i = addComponent("com.GoGoBoard.GoGoBoardComponent", args, i);
            } else if (args[i].equalsIgnoreCase("-sapa")) {
                i = addComponent("com.sapa.SapaComponent", args, i);
            } else if (args[i].equalsIgnoreCase("-discourse")
                    || args[i].equalsIgnoreCase("-disc")) {
                i = addComponent("com.discourse.DiscourseComponent", args, i);
            } else if (args[i].equalsIgnoreCase("-discversion")) {
                log.debug(myID + ": using Discourse component " + args[i + 1]);
                i = addComponent(args[++i], args, i);
                i = addComponent("com.interfaces.NLPComponent", args, i);
            // Interfaces
            } else if (args[i].equalsIgnoreCase("-lrf") || args[i].equalsIgnoreCase("-laser")) {
                i = addComponent("com.interfaces.LaserComponent", args, i);
            } else if (args[i].equalsIgnoreCase("-nlpserver")) {
                i = addComponent("com.interfaces.NLPComponent", args, i);
            // other parameters
            } else if (args[i].equalsIgnoreCase("-simfirst")) {
                simulateFirst = true;
            } else if (args[i].equalsIgnoreCase("-agentname")) {
                agentname = args[++i];
            } else if (args[i].equalsIgnoreCase("-subject")) {
                Subject = args[++i];
            } else if (args[i].equalsIgnoreCase("-script")) {
                ArrayList<String> taskSpec = new ArrayList<String>();
                // Consumes C\L until encounters prefix '--'
                while ((++i < args.length) && !args[i].startsWith("--")) {
                    String t = args[i];
                    if (t.equals("?subject") && (Subject != null)) {
                        taskSpec.add(Subject);
                    } else {
                        taskSpec.add(t);
                    }
                }
                log.debug("Adding action " + taskSpec);
                //i--;
                initScripts.add(taskSpec);
            } else if (args[i].equalsIgnoreCase("-spextest")) {
                spexTest = true;
            } else if (args[i].equalsIgnoreCase("-nosleep")) {
                sleepAI = false;
            } else if (args[i].equalsIgnoreCase("-dbfile")) {
                dbFilename = args[++i];
            } else if (args[i].equalsIgnoreCase("-dbfilesupp")) {
                log.info(format("Adding action db file to be parsed: %s", args[i+1]));
                dbFileSupp.add(args[++i]);
            } else if (args[i].equalsIgnoreCase("-api")) {
                printAPI = true;
            } else if (args[i].equalsIgnoreCase("-goal")) {
                Predicate g = createPredicate(args[++i]);
                initGoals.add(g);
            } else if (args[i].equalsIgnoreCase("-state")) {
                if (checkNextArg(args,i)) {
                    if (initState == null)
                        initState = "";
                    else
                        initState += ":";
                    initState += args[++i];
                }
            } else if (args[i].equalsIgnoreCase("-aiverb")) {
                int d;
                try {
                    d = Integer.parseInt(args[i + 1]);
                    i++;
                    descriptive = d;
                } catch (NumberFormatException nfe) {
                    System.err.println(myID + ": aiverb " + args[i + 1]);
                    System.err.println(myID + ": " + nfe);
                    System.err.println(myID + ": default aiverb is " + descriptive);
                }
            } else if (args[i].equalsIgnoreCase("-logcpu")) {
                logCPU = true;
            } else {
                log.debug("Unrecognized argument: " + args[i]);
                return false;  // return on any unrecognized args
            }
        }
        return true;
    }

    @Override
    public ADEGuiVisualizationSpecs getVisualizationSpecs() throws RemoteException {
        log.trace("enter getVisualizationSpecs()");
    	ADEGuiVisualizationSpecs specs = super.getVisualizationSpecs();
        specs.add("Action Manager", GoalManagerVis.class);
        return specs;
    }

    @Override
    protected void updateComponent() {}
    @Override
    protected void updateFromLog(String logEntry) {}

    // ********************************************************************
    // *** local Action Manager methods
    // ********************************************************************
    /**
     * Must be implemented by subclass.  <code>updatePriorities</code> is
     * called periodically to perform whatever reprioritization of goals is
     * necessary (e.g., because of changes in affective states).
     */
    abstract protected void updatePriorities();

    protected boolean checkMethod(String mn) {
        log.trace("enter checkMethod(String mn)");
        Object sref = getMethodRef(mn);
        return checkComponentStatus(sref, mn);
    }

    protected boolean checkComponentStatus(Object ref, String mn) {
        log.trace("enter checkComponentStatus(Object ref, String mn)");
        if (ref == null) {
           log.debug("ref for " + mn + " is null");
            // Add this one to the list of desired methods
            synchronized (unboundMethods) {
                unboundMethods.add(mn);
            }
            return false;
        }
        ADEGlobals.RecoveryState rs = getRecoveryState(ref);
        if (! rs.equals(RecoveryState.OK)) {
            // mn bound to ref at some point, don't interfere
            return false;
        }
        return true;
    }

    /**
     * Get the reference associated with the given method name.
     * @param mn the method name
     * @return the reference
     */
    Object getReference(String mn) {
        log.trace("enter getReference(String mn)");
        return getMethodRef(mn);
    }

    /**
     * Check whether a reference of this name has been requested.
     * @param rn reference name
     * @param onlyGood whether to check only in the good references list
     * @return true if found, false otherwise
     */
    protected Boolean checkReference(String rn, boolean onlyGood) {
        log.trace("enter checkReference("+rn+", "+onlyGood+")");
        String sType = rn;
        String sName = null;
        List<Component> sList = servers.get(sType);

        if (sList == null) {
            // not a recognized type, maybe a full ID
            try {
                sType = getTypeFromID(rn);
                sName = getNameFromID(rn);
                sList = servers.get(sType);
                if (sList == null) {
                    // not a recognized type
                    return false;
                }
            } catch (IndexOutOfBoundsException ioobe) {
                // couldn't get a type from rn
                //System.err.println(myID + ": unknown server type/name: " + rn);
                return false;
            }
        }
        if (sName == null) {
            if (! onlyGood) {
                // found the type in the server list, so it's requested
                return true;
            }
            for (Component srv : sList) {
                if (srv.getStatus()) {
                    // without a name, just checking to see if *any* sType is good
                    return true;
                }
            }
        } else {
            for (Component srv : sList) {
                if (srv.getName().equals(sName)) {
                    // found the server, check status if needed
                    return (! onlyGood) || srv.getStatus();
                }
            }
        }
        // no match, or false status
        return false;
    }

    /**
     * Check whether a reference of this name had been acquired.
     * @param rn reference name
     * @return true if found, false otherwise
     */
    protected Boolean checkReference(String rn) {
        return checkReference(rn, true);
    }

    /**
     * Retrieve the reference associated with the given method and call
     * the method in that server, using the default timeout.
     * @param mn method name
     * @param args method arguments
     * @return whatever the remote method returns
     * @throws ADEException
     * @throws ADETimeoutException
     */
    protected Object callMethod(String mn, Object... args) throws ADEException, ADETimeoutException, ADEReferenceException {
        log.trace("enter callMethod(String mn, Object ... args)");
        Object sref = getMethodRef(mn);
        /*
        if (mn.equals("updatePlannerState")) {
            StackTraceElement e = new Exception().getStackTrace()[1];
            System.out.println("Calling updatePlannerState from " + e.getFileName() + ":" + e.getLineNumber());
        }
        */
        log.trace("ref for " + mn + " is " + sref);
        if (! checkComponentStatus(sref, mn)) {
            // TODO: check for local method here
            throw new ADEReferenceException("No valid reference for method " + mn);
        }
        return call(sref, mn, args);
    }

    /**
     * Retrieve the reference associated with the given method and call
     * the method in that server, using the specified timeout.
     * @param timeout the call timeout duration
     * @param mn method name
     * @param args method arguments
     * @return whatever the remote method returns
     * @throws ADEException
     * @throws ADETimeoutException
     */
    protected Object callMethod(int timeout, String mn, Object... args) throws ADEException, ADETimeoutException, ADEReferenceException {
        log.trace("enter callMethod(int timeout, String mn, Object ... args)");
        Object sref = getMethodRef(mn);
        log.trace("ref for " + mn + " is " + sref);
        if (! checkComponentStatus(sref, mn)) {
            throw new ADEReferenceException("No valid reference for method " + mn);
        }
        return call(timeout, sref, mn, args);
    }

    /**
     * Retrieve the reference associated with the given server/method and call
     * the method in that server, using the default timeout..
     * @param sn server name
     * @param mn method name
     * @param args method arguments
     * @return whatever the remote method returns
     * @throws ADEException
     * @throws ADETimeoutException
     */
    protected Object callComponentMethod(String sn, String mn, Object... args) throws ADEException, ADETimeoutException, ADEReferenceException {
        log.trace("enter callComponentMethod(String sn, String mn, Object ... args)");
        Object sref = getMethodRef(mn, sn);
        /*
        if (mn.equals("updatePlannerState")) {
            StackTraceElement e = new Exception().getStackTrace()[1];
            System.out.println("Calling updatePlannerState from " + e.getFileName() + ":" + e.getLineNumber());
        }
        */
        log.trace("ref for " + mn + " is " + sref);
        if (! checkComponentStatus(sref, mn)) {
            // TODO: check for local method here
            throw new ADEReferenceException("No valid reference for method " + mn);
        }
        return call(sref, mn, args);
    }

    /**
     * Retrieve the reference associated with the given server/method and call
     * the method in that server, using the specified timeout.
     * @param timeout the call timeout duration
     * @param sn server name
     * @param mn method name
     * @param args method arguments
     * @return whatever the remote method returns
     * @throws ADEException
     * @throws ADETimeoutException
     */
    protected Object callComponentMethod(int timeout, String sn, String mn, Object... args) throws ADEException, ADETimeoutException, ADEReferenceException {
        log.trace("enter callComponentMethod(int timeout, String sn, String mn, Object ... args)");
        Object sref = getMethodRef(mn, sn);
        log.trace("ref for " + mn + " is " + sref);
        if (! checkComponentStatus(sref, mn)) {
            throw new ADEReferenceException("No valid reference for method " + mn);
        }
        return call(timeout, sref, mn, args);
    }

    /**
     * Simulate achievement of goals, if possible.
     * @param gmg a goal to simulate
     * @return true if it succeeds
     */
    private boolean simulateGoal(GoalManagerGoal gmg) {
        log.trace("enter simulateGoal(GoalManagerGoal gmg)");
        Predicate goal = gmg.getGoal();
        Long gid = null;
        ActionStatus gas = null;
        if (! checkMethod("submitGoal")) {
            sayText("I can't simulate goal achievement, trying anyway.");
            return true;
        }
        try {
            gid = (Long)callMethod("submitGoal", goal);
        } catch (ADEException ace) {
            System.err.println("Error submitting goals: " + ace);
            sayText("I can't simulate goal achievement, trying anyway.");
            return true;
        }
        if (gid == null) {
            sayText("I can't simulate goal achievement, trying anyway.");
            return true;
        }
        if (gid == 0) {
            // goal didn't get ID, metaGoal
            gas = ActionStatus.SUCCESS;
        } else {
            try {
                gas = (ActionStatus)callMethod("goalStatus", gid);
            } catch (Exception ace) {
                System.err.println("Failed to get status for " + gid + ": " + ace);
                sayText("I am unable to achieve the goals for unknown reasons");
                return false;
            }
            if (gas == ActionStatus.FAIL || gas == ActionStatus.SUCCESS) {
                log.debug("Goal " + gid + " complete: " + gas);
            }
        }
        if (gas == ActionStatus.FAIL) {
            ArrayList<GoalManagerGoal> newgoals = new ArrayList<GoalManagerGoal>();
            newgoals.add(gmg);
            try {
                ArrayList<Predicate> conds = (ArrayList<Predicate>)callMethod("goalFailConds", gid);
                // PWS: Only looking at the first fail condition for now
                Predicate cond = conds.get(0);
                log.debug("Goal " + gid + " failed because " + cond);
                sayText("I can't achieve the goals because " + cond);
                if (cond.getName().equals("unable")) {
                    // tag postponed goals with missing method name; whenever new
                    // servers connect, the postponed goals will be checked for
                    // any that are waiting for methods of the new server
                    log.debug("Saving with method: " + cond.get(0).toString());
                    postponedGoals.put(cond, newgoals);
                    synchronized (unboundMethods) {
                        unboundMethods.add(cond.get(0).toString());
                    }
                } else {
                    postponedGoals.put(createPredicate("reason(unknown)"), newgoals);
                }
            } catch (Exception ace) {
                System.err.println("Failed to get condition for " + gid + ": " + ace);
                sayText("I am unable to achieve the goals for unknown reasons");
            }
            // TODO: should cancel remaining goals in simulator
            return false;
        } else if (gas == ActionStatus.SUCCESS) {
            if (gid > 0) {
                log.debug("Goal " + gid + " succeeded.");
            }
        }
        Sleep(100);
        submitGoal(gmg); // PWS: should have gid before now
        return true;
    }

    /**
     * Generate action script statements and variables to create the passed-in
     * predicate at run-time.  The statements are added to the action script object
     * parameter.
     * @param p the predidate to be created at run-time
     * @param a the action to add the script statements to
     * @param c the current !pred variable counter (!pred0, !pred1, !pred2, etc.)
     * @return the updated !pred variable counter
     */
    int addPredSpec(Predicate p, ActionDBEntry a, int c) {
        log.trace("enter addPredSpec(Predicat p, ActionDBEntry a, int c");
        String pSpec = "makePredicate \"" + p.getName();
        for (Symbol s : p.getArgs()) {
            if (p.getClass().isInstance(s)) {
                c = addPredSpec((Predicate)s, a, c);
                pSpec = pSpec + " !pred" + c;
                a.addRole("!pred" + c, "predicate", "data", false);
            } else {
                pSpec = pSpec + " " + s;
            }
        }
        c++;
        pSpec = pSpec + "\" !pred" + c;
        a.addRole("!pred" + c, "predicate", "data", false);
        a.addEventSpec(pSpec);
        //a.addEventSpec("printText \"Completed " + pSpec + "\"");
        return c;
    }

    private void submitGoal(GoalManagerGoal gmg) {
        System.out.println("Submitted goal "+gmg);
        log.trace("enter submitGoal(GoalManagerGoal gmg");
        Predicate g = gmg.getGoal();
        log.debug("gmg: "+g);
        ActionInterpreter ai;
	System.out.println("1");
        log.trace("in submitGoal for: " + g.getName());
        ActionDBEntry task = ActionDBEntry.lookupPost(g);
	System.out.println("2");
	String type = "object";
        String name = g.get(0).getName();
        boolean actor = true;
	System.out.println("3");
        if (task != null) {

            if(task.getNumRoles() == 0){
                type = "actor";
            }
            else{
                type = task.getRoleType(0);
            }
	    
            actor = !ActionDBEntry.lookup(type).isA("agent") || (type.equals("actor") && name.equals(agentname));
        }
	System.out.println("4");
        log.debug("submitGoal: "+name+" "+type+" "+actor);
	System.out.println("5");
	if (task == null || !actor) {
            // check for the meta-goals
            if (checkMetaAction(gmg)) {
                return; // not sure if this is a good idea...
            }
            // Add a dummy AI to "execute plan" serving as AM's representation 
            // of the Planner goal; need to figure out how to detect Planner 
            // plan finished to complete this goal...
            // PWS: can do this in the same way we did goal simulation
	    System.out.println("Inside GoalManagerImpl");
            System.err.println("Didn't find " + g.getName().toLowerCase());
            if (checkReference("com.sapa.SapaComponent")) {
                gmg.setGoalStatus(ActionStatus.INITIALIZE);
                plannerGoal(gmg, 1000, -1); // default utility, timeout
            } else {
                System.err.println("No planner, can't " + g.getName().toLowerCase());
                gmg.terminate(ActionStatus.FAIL);
            }
        } else {
            ActionDBEntry adbe = ActionDBEntry.lookup("obey commands");
            ai = addAction(gmg, adbe);
        }
        if (gmg.getGoalStatus() == ActionStatus.FAIL) {
            gmg.terminate(ActionStatus.FAIL);
        }
	System.out.println("7");
        goals.put(gmg.getGoalId(), gmg);
        log.debug("Added goal "+ g.getName() +" with ID "+ gmg.getGoalId());
    }

    /**
     * Process a list of state updates (e.g., for the planner).
     * @param updates the updates
     * @param replan whether this update should trigger replanning
     */
    protected void processStateUpdate(List<Predicate> updates, boolean replan) {
        log.trace("enter processStatusUpdate(List<Predicate> updates, boolean replan");
        for (Predicate update : updates) {
            processStateUpdate(update, replan);
            boolean negated = false;
            while (update.getName().equals("not")) {
                negated = !negated;
                update = (Predicate)update.get(0);
            }
            ActionEvent event = new ActionEvent(update, negated, null, null);
            events.add(event);
        }
    }

    /**
     * Process a list of state updates (e.g., for the planner).
     * @param updates the updates
     * @param replan whether this update should trigger replanning
     * @param actSpec description of action responsible for the state update
     */
    protected void processStateUpdate(List<Predicate> updates, boolean replan, String actSpec) {
        log.trace("enter processStatusUpdate(List<Predicate> updates, boolean replan, String actspec");
        for (Predicate update : updates) {
            processStateUpdate(update, replan);
            boolean negated = false;
            while (update.getName().equals("not")) {
                negated = !negated;
                update = (Predicate)update.get(0);
            }
            ActionEvent event = new ActionEvent(update, negated, actSpec, null);
            events.add(event);
        }
    }

    /**
     * Process a list of state updates (e.g., for the planner).
     * @param updates the updates
     * @param replan whether this update should trigger replanning
     * @param actSpec description of action responsible for the state update
     * @param goal the goal this update is for
     */
    protected void processStateUpdate(List<Predicate> updates, boolean replan, String actSpec, GoalManagerGoal goal) {
        log.trace("enter processStatusUpdate(List<Predicate> updates, boolean replan, String actspec, GoalManagerGoal goal");
        for (Predicate update:updates) {
            processStateUpdate(update, replan);
            boolean negated = false;
            while (update.getName().equals("not")) {
                negated = !negated;
                update = (Predicate)update.get(0);
            }
            GoalManagerGoal gmg = null;
            if (ActionDBEntry.predicateMatch(update, goal.getGoal())) {
                gmg = goal;
            }
            ActionEvent event = new ActionEvent(update, negated, actSpec, gmg);
            events.add(event);
        }
    }

    /**
     * Process a state update (e.g., for the planner).  The update gets added to
     * the list of current updates and is checked against outstanding goals to see
     * if it achieves any of them.
     * @param update the updates
     * @param replan whether this update should trigger replanning
     */
    protected void processStateUpdate(Predicate update, boolean replan) {
        log.trace("enter processStatusUpdate(Predicate update, boolean replan");
        log.debug("updating state: " + update.getName());
        if (replan) log.debug("Need to replan");

        for (Symbol a:update.getArgsRef()) {
            a.setName(a.getName().toLowerCase());
        }
        Predicate newUpdate = update;
        Predicate newUpdate2 = null;
        Long id = -1L;
        String name;
        boolean connected = false;
        if (update.getName().equals("type") || update.getName().equals("vartype")) {
            String type = update.get(1).getName().toLowerCase();
            if (type.equals("room") || type.equals("endroom")) {
                try {
                    id = Long.parseLong(update.get(0).getName());
                    /*
                    try {
                        connected = (Boolean)callMethod("connectedToWorld", id);
                    } catch (ADEException ace) {
                        System.err.println(myID + ": error checking connected for " + id + ": " + ace);
                        ace.printStackTrace();
                    }
                     */ connected = true;
                    if (connected && nameIdMap.get(id) == null) {
                        name = "spex" + id;
                        nameIdMap.put(name, id);
                        nameIdMap.put(id, name);
                        newUpdate = createPredicate("isa("+name+","+type+")");
                        newUpdate2 = createPredicate("has_property(" + name + ",clear_pr)");
                    } else {
                        newUpdate = null;
                    }
                } catch (NumberFormatException nfe) {
                    // try passing it on as-is
                }
            } else if (type.equals("hallway")) {
                try {
                    id = Long.parseLong(update.get(0).getName());
                    /*
                    try {
                        connected = (Boolean)callMethod("connectedToWorld", id);
                    } catch (ADEException ace) {
                        System.err.println(myID + ": error checking connected for " + id + ": " + ace);
                        ace.printStackTrace();
                    }
                     */ connected = true;
                    if (connected && nameIdMap.get(id) == null) {
                        name = "spex" + id;
                        nameIdMap.put(name, id);
                        nameIdMap.put(id, name);
                        newUpdate = createPredicate("isa("+name+",hallway)");
                        newUpdate2 = createPredicate("has_property(" + name + ",clear_pr)");
                    } else {
                        newUpdate = null;
                    }
                } catch (NumberFormatException nfe) {
                    // try passing it on as-is
                }
            } else if (type.equals("door")) {
                try {
                    id = Long.parseLong(update.get(0).getName());
                    /*
                    try {
                        connected = (Boolean)callMethod("connectedToWorld", id);
                    } catch (ADEException ace) {
                        System.err.println(myID + ": error checking connected for " + id + ": " + ace);
                        ace.printStackTrace();
                    }
                     */ connected = true;
                    if (connected && nameIdMap.get(id) == null) {
                        name = "spex" + id;
                        nameIdMap.put(name, id);
                        nameIdMap.put(id, name);
                        newUpdate = createPredicate("isa("+name+",doorway)");
                        newUpdate2 = createPredicate("has_property(" + name + ",open_pr)");
                    } else {
                        newUpdate = null;
                    }
                } catch (NumberFormatException nfe) {
                    // try passing it on as-is
                }
            }
        } else if (update.getName().equalsIgnoreCase("door_connected")) {
            try {
                long did = Long.parseLong(update.get(0).getName());
                long hid = Long.parseLong(update.get(1).getName());
                long rid = Long.parseLong(update.get(2).getName());
                String dname = (String)nameIdMap.get(did);
                String hname = (String)nameIdMap.get(hid);
                String rname = (String)nameIdMap.get(rid);
                if (dname != null && hname != null && rname != null) {
                    newUpdate = createPredicate("door_connected("+dname+","+hname+","+rname+")");
                } else {
                    newUpdate = null;
                }
            } catch (NumberFormatException nfe) {
                // try passing it on as-is
            }
        } else if (update.getName().equalsIgnoreCase("connected")) {
            try {
                long fid = Long.parseLong(update.get(0).getName());
                long tid = Long.parseLong(update.get(1).getName());
                String fname = (String)nameIdMap.get(fid);
                String tname = (String)nameIdMap.get(tid);
                if (fname != null && tname != null) {
                    newUpdate = createPredicate("connected("+fname+","+tname+")");
                } else {
                    newUpdate = null;
                }
            } catch (NumberFormatException nfe) {
                // try passing it on as-is
            }
        } else if (update.getName().equalsIgnoreCase("Consolidated")) {
            try {
                long fid = Long.parseLong(update.get(0).getName());
                long tid = Long.parseLong(update.get(1).getName());
                String fname = (String)nameIdMap.get(fid);
                String tname = (String)nameIdMap.get(tid);
                nameIdMap.put(tname, fid);
                nameIdMap.put(tid, fname);
                if (fname != null && tname != null) {
                    newUpdate = createPredicate("connected("+fname+","+tname+")");
                } else {
                    newUpdate = null;
                }
            } catch (NumberFormatException nfe) {
            }
            newUpdate = null;
        }
        log.debug(myID + ": state update: " + newUpdate + " (" + update + ")");
        // check here to see if this completes a gmg
        for (GoalManagerGoal gmg:plannerGoals) {

        }
        /*
        */
        if (newUpdate != null) {
            synchronized (stateUpdates) {
                stateUpdates.add(newUpdate);
                stateUpdatesReplan |= replan;
            }
        }
        if (newUpdate2 != null) {
            synchronized (stateUpdates) {
                stateUpdates.add(newUpdate2);
            }
        }
    }

    /**
     * Print all action priorities to stdout
     */
    protected void printPriorities() {
        synchronized (actions) {
            for (ActionInterpreter ai : actions) {
                System.out.println(ai.cmd + ": " + ai.getActionPriority());
            }
        }
    }

    /**
     * Add initial action. No parent.
     * @param taskSpec the invocation specification
     * @param mot the goal's motivation
     * @return the ActionInterpreter servicing the goal
     */
    ActionInterpreter addAction(GoalManagerGoal gmg, ArrayList<String> taskSpec, ActionDBEntry mot) {
        log.trace("enter addAction(GoalManagerGoal gmg, ArrayList<String> taskSpec, ActionDBEntry mot)");
        ActionInterpreter newAI = new ActionInterpreter(this, gmg, taskSpec, mot);
        log.debug("Adding " + taskSpec.get(0) + " to actions");
        synchronized (actions) {
            actions.add(newAI);
        }
        updatePriorities();
        if (gmg.getGoalStatus() == ActionStatus.FAIL) {
            log.error(taskSpec.get(0) + " unable to start (forbidden)");
            synchronized (actions) {
                actions.remove(newAI);
            }
            return null;
        }
        gmg.setActionInterpreter(newAI);
        log.debug("actions now contains " + actions.size() + " ActionInterpreters");
        sliceTime = cycleTime / actions.size();
        if (refsReady && !conversationState)
            newAI.start();
        logItASL(newAI.cmd + " START BY " + mot.getName());
        return newAI;
    }

    /**
     * Add Planner plan.
     * @param task an action script
     * @param mot the goal's motivation
     * @return the ActionInterpreter servicing the goal
     */
    ActionInterpreter addAction(GoalManagerGoal gmg, ActionDBEntry task, ActionDBEntry mot) {
        log.trace("enter addAction(GoalManagerGoal gmg, ActionDBEntry task, ActionDBEntry mot)");
        ActionInterpreter newAI = new ActionInterpreter(this, gmg, task, mot);
        task.setStartTime(System.currentTimeMillis());
        synchronized (actions) {
            actions.add(newAI);
        }
        updatePriorities();
        gmg.setActionInterpreter(newAI);
        log.debug("actions now contains " + actions.size() + " ActionInterpreters");
        sliceTime = cycleTime / actions.size();
        if (refsReady && !conversationState)
            newAI.start();
        logItASL(newAI.cmd + " START BY " + mot.getName());
        return newAI;
    }

    /**
     * Add submitGoal action.
     * @param gmg a Predicate embodying the task
     * @param mot the goal's motivation
     * @return the ActionInterpreter servicing the goal
     */
    ActionInterpreter addAction(GoalManagerGoal gmg, ActionDBEntry mot) {
        log.trace("enter addAction(GoalManagerGoal gmg, ActionDBEntry mot)");
        Predicate task = gmg.getGoal();
        ActionInterpreter newAI =
                new ActionInterpreter(this, gmg, mot);
        synchronized (actions) {
            actions.add(newAI);
        }
        updatePriorities();
        log.debug("WARNING: not checking for initial locks!");
        log.debug("status of "+task.getName()+": "+gmg.getGoalStatus());
        if (!newAI.acquireInitialLocks()) {
            log.error(task.getName() + " unable to start (locks)");
            String owner = newAI.currentAction.getLockOwnerDescription();
            sayText("I'm sorry, I can't do that, I need to " + owner);
            synchronized (actions) {
                actions.remove(newAI);
            }
            return null;
        } else if (gmg.getGoalStatus() == ActionStatus.FAIL) {
            log.error(task.getName() + " unable to start (forbidden)");
            synchronized (actions) {
                actions.remove(newAI);
            }
            return null;
        }
        gmg.setActionInterpreter(newAI);
        log.debug("actions now contains " + actions.size() + " ActionInterpreters");
        sliceTime = cycleTime / actions.size();
        if (refsReady && !conversationState)
            newAI.start();
        logItASL(newAI.cmd + " START BY " + mot.getName());
        return newAI;
    }

    /**
     * Remove an script interpreter from the goal manager.
     * @param action the AI to remove
     */
    void delAction(ActionInterpreter action) {
        log.trace("enter delAction(ActionInterpreter action)");
        synchronized (actions) {
            actions.remove(action);
        }
        log.warn("WARNING: deepRelease will not work for preempted AIs!");
        for (ActionResourceLock r : resourceLocks) {
            r.deepRelease(action);
        }
        if (actions.size() > 0) {
            sliceTime = cycleTime / actions.size();
        } else if (!stickAround) {
            System.exit(0);
        } else {
            sliceTime = cycleTime;
        }
    }

    /**
     * Get a resource lock.
     * @param lockName the name of the lock to get
     * @return the lock
     */
    static ActionResourceLock getLock(String lockName) {
        log.trace("enter getLock("+lockName+")");
        if (lockName.equalsIgnoreCase("motionLock")) {
            return motionLock;
        } else if (lockName.equalsIgnoreCase("transmitLock")) {
            return transmitLock;
        } else if (lockName.equalsIgnoreCase("visionLock")) {
            return visionLock;
        } else if (lockName.equalsIgnoreCase("speechLock")) {
            return speechLock;
        } else if (lockName.equalsIgnoreCase("lightLock")) {
            return lightLock;
        } else if (lockName.equalsIgnoreCase("blinkLock")) {
            return blinkLock;
        } else if (lockName.equalsIgnoreCase("headLock")) {
            return headLock;
        } else if (lockName.equalsIgnoreCase("faceLock")) {
            return faceLock;
        } else {
            return null;
        }
    }

    /**
     * Get the goal manager's "global" positive affect. This value can
     * influence goal priorities in some GoalManagers.
     * @return the value of positive affect
     */
    protected double getPositiveAffect() {
        log.trace("enter getPositiveAffect()");
        return positiveAffect;
    }

    /**
     * Decrement the goal manager's "global" positive affect. This value can
     * influence goal priorities in some GoalManagers.
     * @param dec the scale (or weight) of the decrement
     * @return the resultant value of positive affect
     */
    protected double decrementPositiveAffect(double dec) {
        log.trace("enter decrementPositiveAffect(double dec)");
        positiveAffect -= positiveAffect * dec;
        log.debug("Decremented positiveAffect: " + positiveAffect);
        return positiveAffect;
    }

    /**
     * Increment the goal manager's "global" positive affect. This value can
     * influence goal priorities in some GoalManagers.
     * @param w the scale (or weight) of the increment
     * @return the resultant value of positive affect
     */
    protected double incrementPositiveAffect(double w) {
        log.trace("enter incrementPositiveAffect(double w)");
        double inc = 0.0;
        int n = 0;

        synchronized (actions) {
            for (ActionInterpreter a : actions) {
                double pa = a.getAffect(true);
                double na = a.getAffect(false);
                if (pa > na) {
                    n++;
                    // inc += 1 + pa*pa - na*na;
                    // inc shouldn't be affected when pa & na are zero
                    inc += pa * pa - na * na;
                }
            }
        }
        if (n > 0) {
            // Assume for now equal weights...
            inc /= n;
            // Scale the inc (relative importance of AIs)
            inc *= w;
            positiveAffect += (1 - positiveAffect) * inc;
        }
        log.debug("Incremented positiveAffect: " + positiveAffect);
        return positiveAffect;
    }

    /**
     * Get the goal manager's "global" negative affect. This value can
     * influence goal priorities in some GoalManagers.
     * @return the value of negative affect
     */
    protected double getNegativeAffect() {
        log.trace("enter getNegativeAffect()");
        return negativeAffect;
    }

    /**
     * Decrement the goal manager's "global" negative affect. This value can
     * influence goal priorities in some GoalManagers.
     * @param dec the scale (or weight) of the decrement
     * @return the resultant value of negative affect
     */
    protected double decrementNegativeAffect(double dec) {
        log.trace("enter decrementNegativeAffect(double dec)");
        negativeAffect -= negativeAffect * dec;
        log.debug("Decremented negativeAffect: "+negativeAffect);
        return negativeAffect;
    }

    /**
     * Increment the goal manager's "global" negative affect. This value can
     * influence goal priorities in some GoalManagers.
     * @param w the scale (or weight) of the increment
     * @return the resultant value of negative affect
     */
    protected double incrementNegativeAffect(double w) {
        log.trace("enter incrementNegativeAffect(double w)");
        double inc = 0.0;
        int n = 0;

        synchronized (actions) {
            for (ActionInterpreter a : actions) {
                double pa = a.getAffect(true);
                double na = a.getAffect(false);
                if (pa <= na) {
                    n++;
                    // inc += 1 + na*na - pa*pa;
                    // inc shouldn't be affected when pa & na are zero
                    inc += na * na - pa * pa;
                }
            }
        }
        if (n > 0) {
            // Assume for now equal weights...
            inc /= n;
            // Scale the inc (relative importance of AIs)
            inc *= w;
            negativeAffect += (1 - negativeAffect) * inc;
        }
        log.debug("Incremented negativeAffect: "+negativeAffect);
        return negativeAffect;
    }

    /**
     * Set the goal manager's "global" negative affect. This is just here
     * for testing or demonstration purposes, not really any good reason to
     * use it in general.
     * @param w the new value
     * @return the resultant value of negative affect
     */
    protected double setNegativeAffect(double w) {
        log.trace("enter setNegativeAffect(double w)");
        negativeAffect = w;
        log.debug("set negativeAffect: " + negativeAffect);
        return negativeAffect;
    }

    private void updateAffect() {
        // TODO: determine how affect decay will operate...
    }

    protected boolean updateLaserFeatureDetector() {
        log.trace("enter updateLaserFeatureDetector()");
        try {
            doors = (List<Door>) callMethod("getDoorways");
        } catch (ADEException ace) {
            log.error((myID + ": error getting LRF readings"), ace);
            return false;
        }
        return true;
    }

    // Get the nearest door, and try to keep track of doors once detected
    protected Door detectNearestDoor() {
        log.trace("enter detectNearestDoor()");
        double d = 0.0;
        Door mydoor = null;
        double mydist = Double.POSITIVE_INFINITY;
        boolean found = false;

        // PWS: TODO: should get these from laser server instead of schlepping the
        // readings and doing it here...
        if (!checkMethod("getDoorways")) {
            log.debug(myID + ": no way to check for doorways");
            return mydoor;
        }
        if (! updateLaserFeatureDetector()) {
            return mydoor;
        }
        if (doors != null && ! doors.isEmpty()) {
            currentDoorCount++;
            // protect against errant door sightings by requiring some consistency
            //if (currentDoorCount > 4) {
                int i = 0;
                for (; i < doors.size(); i++) {
                    Door door = doors.get(i);
                    if (door.confidence == 1.0) {
                        haveDoor = true;
                        found = true;
                        mydist = door.getCenter().distance(0, 0); 
                        mydoor = door;
                    }
                }
                for (; i < doors.size(); i++) {
                    Door door = doors.get(i);
                    if (door.confidence == 1.0) {
                        d = door.getCenter().distance(0, 0);
                        if (d < mydist) {
                            mydist = d;
                            mydoor = door;
                        }
                    }
                }
                //mydoor.agentpose = pose;
            //}
        } else {
            //System.out.println("No doors found");
        }
        if (!found) {
            if (haveDoor == true) {
                currentDoor++; // lost currentDoor (e.g., maybe passed by it)
            }
            haveDoor = false;
            currentDoorCount = 0;
        }
        return mydoor;
    }

    /**
     * Try to determine whether this door has been seen before.
     * @param newdoor name of candidate door
     * @param x x-coordinate of door
     * @param y y-coordinate of door
     * @return true if we can match it with one we've seen before, false otherwise
     */
    private boolean checkDoor(String newdoor, double x, double y) {
        log.trace("enter checkDoor(String newdoor, double x, double y)");
        if (pastPercepts.containsKey(newdoor))
            return false;

        if (! checkMethod("getPoseGlobal")) {
            return true;
        }
        for (ADEPercept p : pastPercepts.values()) {
            //System.out.println("Checking " + p.name + " (" + p.sim_x + "," + p.sim_y + ") for " + newdoor + " (" + x + "," + y + ").");
            if (p.type.equalsIgnoreCase("door") && getDistanceFrom(x, y, p.sim_x, p.sim_y) < 0.5) {
                //System.out.println("Already knew of door: " + p.name);
                return false;
            }
        }
        return true;
    }

    /**
     * Set up the planner. Build the domain and problem from the action database.
     */
    private void initializePlanner() {
        log.trace("enter initializePlanner()");
        String mn = "startPlannerDomain";
        Object planner = getMethodRef(mn);
        try {
            if (! checkComponentStatus(planner, mn)) {
                throw new ADEReferenceException("No valid reference for method " + mn);
            }
            call(planner, mn);
        } catch (ADEException ace) {
           log.error("Error starting planner domain construction", ace);
        }
        for (PlannerElement p : adb.getPlannerDomainElements()) {
            if (p.getPlannerType() == PlannerElement.TYPE) {
                p.submit(this);
            }
        }
        // need to send constants now because some are used in action definitions
        for (PlannerElement p : adb.getPlannerProblemElements()) {
            if (p.getPlannerType() == PlannerElement.CONSTANT) {
                p.submit(this);
            }
        }
        for (PlannerElement p : adb.getPlannerDomainElements()) {
            if (p.getPlannerType() == PlannerElement.PREDICATE) {
                p.submit(this);
            }
        }
        for (PlannerElement p : adb.getPlannerDomainElements()) {
            if (p.getPlannerType() == PlannerElement.ACTION) {
                p.submit(this);
            }
        }
        //PlannerElement pe = buildPushDoor();
        //pe.submit(this);
        mn = "endPlannerDomain";
        try {
            if (! checkComponentStatus(planner, mn)) {
                throw new ADEReferenceException("No valid reference for method " + mn);
            }
            call(planner, mn);
        } catch (ADEException ace) {
            log.error("Error ending planner domain construction", ace);
        }
        mn = "startPlannerProblem";
        log.debug("Done submitting domain, ready to submit problem");
        try {
            if (! checkComponentStatus(planner, mn)) {
                throw new ADEReferenceException("No valid reference for method " + mn);
            }
            call(planner, mn);
        } catch (ADEException ace) {
            log.error("Error starting planner problem construction", ace);
        }
        for (PlannerElement p : adb.getPlannerProblemElements()) {
            if (p.getPlannerType() == PlannerElement.STATE) {
                p.submit(this);
            }
        }
        for (PlannerElement p : adb.getPlannerProblemElements()) {
            if (p.getPlannerType() == PlannerElement.GOAL) {
                p.submit(this);
            }
        }
        for (PlannerElement p : adb.getPlannerProblemElements()) {
            if (p.getPlannerType() == PlannerElement.OPEN) {
                p.submit(this);
            }
        }
        mn = "endPlannerProblem";
        try {
            if (! checkComponentStatus(planner, mn)) {
                throw new ADEReferenceException("No valid reference for method " + mn);
            }
            call(planner, mn);
        } catch (ADEException ace) {
            log.error("Error ending planner problem construction", ace);
        }
        log.debug("Done submitting problem");
        plannerInit = true;
    }

    /**
     * Get information from Spex.
     */
    private void updateSpex() {
        log.trace("enter updateSpex()");
        if (checkMethod("getCurrentPlaceID")) {
            try {
                Long at = (Long)callMethod("getCurrentPlaceID");
                String name;
                if ((name = (String)nameIdMap.get(at)) != null && currentSpex != at) {
                    currentSpex = at;
                    processStateUpdate(createPredicate("at("+agentname+"," + name + ")"), true);
                }
            } catch (ADEException ace) {
                System.err.println("Error getting current place ID from spex: " + ace);
                ace.printStackTrace();
            }
        }
    }

    /**
     * Get information from Belief.
     */
    private void updateBelief() {
        log.trace("enter updateBelief()");
        Set<Predicate> updates;
        if (checkMethod("getRecentUpdates")) {
            try {
                updates = (Set<Predicate>)callMethod("getRecentUpdates", myID, "self");

                // sort predicates -- type needs to come first
                List<Predicate> updateList = new ArrayList<Predicate>(updates);
                PredicateComparator pc = new PredicateComparator();
                Collections.sort(updateList,pc);

                for (Predicate b:updateList) {
                    log.debug("BELIEF UPDATE: " + b);
                    if (b.getName().equals("type") || b.getName().equals("vartype") ||
                            b.getName().equals("goal") || b.getName().equals("should")) {
                        // skip it
                        if(b.getName().equals("type") || b.getName().equals("vartype")) {
                            String nameStr = b.get(0).getName();
                            String typeStr = b.get(1).getName();
                            typeInfo.put(nameStr,typeStr);
                            log.debug("GoalManagerImpl.typeInfo = " + typeInfo);
                        }
                        else{log.debug("skipping it");}
                    } else if(b.getName().equals("procedure_step")) {
                        log.debug("procedure_step received");
                        Predicate parentEffect = new Predicate((Term)b.get(0));
                        Predicate subEffect = new Predicate((Term)b.get(1));
                        ActionDBEntry act = null;
                        if(actionExists(subEffect)) {
                            log.debug("procedure_step - found subaction!");
                            act = getAction(subEffect);
                            Predicate varParentEffect = varConvert(parentEffect);
                            log.debug("procedure_step varParentEffect = " + varParentEffect);
                            alm.handleStep(varParentEffect,act);
                        } else {
                            log.debug("procedure_step - can't find subaction!");
                        }
                        // check to see if
                        if(act != null && checkMethod("listActions")) {
                            List<String> actions = (List<String>)callMethod("listActions");
                            log.debug("procedure_step actions = " + actions);
                            log.debug("act.getName() = " + act.getName());
                            if(actions.contains(act.getName())) {
                                manActDoAction(act.getName(),true);
                            }
                        } else {
                            log.debug("procedure_step - listActions not available.");
                        }
                    } else if(b.getName().equals("procedure_step_vis")) {
                        log.debug("procedure_step_vis received");
                        Predicate parentEffect = new Predicate((Term)b.get(0));
                        //System.out.println("convertToVisionPredicate() - " + convertToVisionPredicate(b));
                        log.debug("manipulationActionPredicate() - " + manipulationActionPredicate(getAchieveTerm(b)));
                        Predicate manActPred = manipulationActionPredicate(getAchieveTerm(b));
                        List<Predicate> result = manActLearnAction(manActPred);
                        log.debug("result = " + result);
                        log.debug("listActions = " + manActListActions());

                        String methodname = null;
                        String var = null;
                        for(Predicate p: result) {
                            if(p.getName().equalsIgnoreCase("methodName")) {
                                methodname = p.getArgs().get(0).getName();
                                log.debug("methodname = " + methodname);
                            } else if(p.getName().equalsIgnoreCase("returnVar")) {
                                var = p.getArgs().get(0).getName();
                                log.debug("var = " + var);
                            }
                        }

                        String stepStr = "domanipulationaction ?mover " + methodname;
                        alm.handleStep(varConvert(parentEffect),stepStr);

                        if(var != null) {
                            ActionInterpreter.manipulationReturnValues.put(methodname,"?" + var);
                            //alm.addRole(varConvert(parentEffect),"?"+var,var,true);
                        }

                        if(result != null) {
                            submitAck();
                        }

                        // for testing purposes
                        String actName = getManActMethodName(result);
                        log.debug("actName = " +  actName);
                        manActDoAction(actName,true);
                        
                                              
                    } else if(b.getName().equals("done_teaching")) {
                        log.debug("done_teaching");
                        Predicate actionDesc = new Predicate((Term)b.get(2));
                        alm.finalizeAction(varConvert(actionDesc));

                        // test to see if that action is now accessible
                        if(actionExists(actionDesc)) {
                            log.debug("done_teaching action " + actionDesc + " now available!");
                        }
                    } else if(b.getName().equals("looks_like")){
                        String belief0 = (String)(b.get(0).toString());
                        Term belief1 = (Term)b.get(1);
                        String type0 =   getTypeInfo().get(belief0);
                        String type1P =   getTypeInfo().get(belief1);
                        Long type1 = -1L;
                        ArrayList<Term> vc = new ArrayList<Term>();
                        for(Symbol sym : belief1.getArgs()){
                            vc.add(convertDVtoVV((Term)sym));
                        }
                        try{
                            type1 = (Long)callMethod(0, "getTypeId", vc);
                            callMethod("nameDescriptors", type1, new Predicate("type", new Variable(((Term)(belief1.get(0))).get(0).toString().replace(" - ",""),""), new Symbol(type0)));
                     
                        }catch(ADEException ere){
                            log.error("Error sending to Vision: "+ere);
                            ere.printStackTrace();
                        }

                        
                    } else if(b.getName().equals("want_send_actions")) {
                        log.debug("want_send_actions");
                        Symbol predSym = b.get(1);
                        if(predSym instanceof Term) {
                            Predicate pred = new Predicate((Term)predSym);
                            ActionDBEntry adb = alm.getScript(pred);
                            if(adb != null) {
                                try {
                                    mdsMeditate();
                                    this.sendAction(adb, "pr2");
                                    this.manActSendActions("pr2");
                                    this.submitNLGStr("transfer complete",10);
                                    mdsNeutralFace();
                                } catch (RemoteException re) {
                                    log.error("Remote failure",re);
                                }
                            }
                        }
                    } else if(b.getName().equals("want_watch")) {
                        log.debug("want_watch");
                        Symbol whoToWatch = b.get(1);
                        //                        actionRecognizer(whoToWatch);
                    } else {
                        boolean update = true;
                        for (Symbol a:b.getArgs()) {
                            log.debug("Checking arg " + a.getName() + " for " + b.getName());
                            if (nameIdMap.get(a.getName().toLowerCase()) == null) {
                                update = false;
                            }
                        }
                        if (update) {
                            processStateUpdate(b, true);
                        }
                    }
                }
            } catch (ADEException ace) {
                log.error("Error getting belief updates",ace);
            }
        } else {
            log.debug("updateBelief - getRecentUpdates not found!");
        }
    }

    private void submitNLGStr(String text, int priority) {
        log.trace("enter submitNLGStr(String text, int priority)");
        try {
            if(checkMethod("submitNLGRequest")) {
                ArrayList<String> words = new ArrayList<String>();
                String[] splitStrs = text.split(" ");
                for(int i=0; i<splitStrs.length; i++) {
                    words.add(splitStrs[i]);
                }
                Utterance utterance = new Utterance(words);
                callMethod(0,"submitNLGRequest",new NLPacket(priority,utterance));
            }
        } catch (ADEException ace) {
            log.error("FAILURE: submitNLGStr", ace);
        }
    }

    private void submitAck() {
        log.trace("enter submitAck()");
        try {
            if(checkMethod("acceptCommand")) {
                callMethod("acceptCommand");
                log.trace("call method acceptCommand");
            } else {
                log.error("acceptCommand is not available");
            }
        } catch (ADEException ace) {
            log.error("FAILURE: submitAck", ace);
            ace.printStackTrace();
        }
    }

    private void manActDoAction(String str, boolean imaginary) {
        log.trace("enter manActDoAction(String str, boolean imaginary");
        try {
            if(checkMethod("doAction")) {
                ArrayList<Symbol> args = new ArrayList<Symbol>();
                Predicate actionPred = new Predicate(str,args);
                callMethod(0,"doAction",actionPred, imaginary);
            } else {
               log.error("manActDoAction - no doAction available!");
            }
        } catch (ADEException  ace) {
            log.error("FAILURE: manActDoAction", ace);
        }
    }

    private List<String> manActListActions() {
        log.trace("enter manActListActions()");
        try {
            if(checkMethod("listActions")) {
             List<String> result = (List<String>)callMethod(0,"listActions");
             return result;
          }
       } catch(ADEException ace) {
           log.error("FAILURE: manActListActions", ace);
       }
       return null;
    }

    private void manActSendActions(String groupName) {
        log.trace("enter manActSendActions(String groupName)");
        try {
           if(checkMethod("sendActions")) {
               callMethod(0,"sendActions", groupName);
           }
        } catch (ADEException ace) {
            log.error("FAILURE: manActSendActions", ace);
        }
    }

    private List<Predicate> manActLearnAction(Predicate specification) {
        log.trace("enter manActLearnAction(Predicate specification)");
       try {
           if(checkMethod("learnAction")) {
               log.debug("manActLearnAction -- start");
               List<Predicate> result = (List<Predicate>)callMethod(0,"learnAction",specification);
               return result;
           } else {
               log.error("no learnAction method available!");
           }
       } catch(ADEException ace) {
           log.error("FAILURE: manActLearnAction", ace);
       }
       return null;
    }

    private void mdsMeditate() {
        log.trace("enter mdsMeditate()");
       try {
          if(checkMethod("meditate")) {
             log.debug("mdsMeditate -- start");
             callMethod(0,"meditate");
          } else {
             log.error("no meditate method available!");
          }
       } catch(ADEException ace) {
           log.error("FAILURE: mdsMeditate", ace);
       }
    }

    private void mdsNeutralFace() {
        log.trace("enter mdsNeutralFace()");
      try {
         if(checkMethod("neutralFace")) {
             log.debug("mdsNeutralFace -- start");
            callMethod(0,"neutralFace");
         } else {
            log.error("no neutralFace method available");
         }
      } catch(ADEException ace) {
          log.error("FAILURE: mdsNeutralFace", ace);
      }
    }

    private String getManActMethodName(List<Predicate> preds) {
        log.trace("enter getManActMethodName(List<Predicate> preds)");
       for(Predicate p: preds) {
          if(p.getName().equalsIgnoreCase("methodname")) {
              return p.get(0).getName();
          }
       }
       return null;
    }

    private Term getAchieveTerm(Term p) {
        log.trace("enter getAchieveTerm(Term p)");
       for(Symbol s: p.getArgs()) {
          if(s instanceof Term) {
             if(s.getName().equals("achieve")) {
                return (Term)s;
             }
          }
       }
       return null;
    }

    //Dialogue Variables => Vision Variables
    private Predicate convertDVtoVV(Term inputTerm) {
        ArrayList<Symbol> allNewArgs = new ArrayList<Symbol>();
        for(Symbol s: inputTerm.getArgs()) {
            if(s instanceof Variable)
                allNewArgs.add(new Variable(((Variable)s).getType(),""));
            else allNewArgs.add(s);
        }
        Predicate retPred = new Predicate(inputTerm.getName(),allNewArgs);
        return retPred;
    }

    private Predicate varConvert(Term inputTerm) {
        log.trace("enter varConvert(Term inputTerm)");
       ArrayList<Symbol> allNewArgs = new ArrayList<Symbol>();
       for(Symbol s: inputTerm.getArgs()) {
           System.out.println("!! "+s);
           System.out.println("TYPESTR! "+this.typeInfo);
          String typeStr = this.typeInfo.get(s.getName());
          if(typeStr != null) {
              System.out.println("NOT NULL! "+new Variable(s.getName(),typeStr));
	     allNewArgs.add(new Variable(s.getName(),typeStr));
	  } else {
              System.out.println("NULL :( "+s);
             allNewArgs.add(s);
	  }
       }
       String newName = inputTerm.getName().equals("vartype") ? "type" : inputTerm.getName();
       Predicate retPred = new Predicate(newName,allNewArgs);
       return retPred;
    }

    private Predicate manipulationActionPredicate(Term achievePred) {

        log.trace("enter manipulateActionPredicates(Term achievePred)");
       ArrayList<Symbol> typePreds = new ArrayList<Symbol>();
       ArrayList<Symbol> allNewArgs = new ArrayList<Symbol>();
       for(Symbol s: achievePred.getArgs()) {
          if(s instanceof Term) {
             Term t = (Term)s;
             Term visT = convertToVisionPredicate(t);
             for(int i=0; i<t.size(); i++) {
                Symbol s2 = t.get(i);
                String typeStr = this.typeInfo.get(s2.getName());
                if(typeStr != null) {
                  ArrayList<Symbol> typeArgs = new ArrayList<Symbol>();
                  typeArgs.add(visT.get(i));
                  typeArgs.add(new Symbol(typeStr));
                  Predicate p = new Predicate("type",typeArgs);
                  allNewArgs.add(p);
                }
             }
             allNewArgs.add(visT);
          }
       }

       Predicate retPred = new Predicate("goto",allNewArgs);
       return retPred;
    }

    private boolean querySupport(Term query) {
        log.trace("enter querySuppoert(Term query)");
       try {
           if(checkMethod("querySupport")) {
              boolean result = (Boolean)callMethod("querySupport",query);
              return result;
	   }
       } catch (ADEException ace) {
           log.error("FAILURE: querySupport", ace);
       }
       return false;
    }

    private Predicate convertToVisionPredicate(Term p) {
        log.trace("enter convertToVisionPredicate (Term p)");
       Predicate returnPred = new Predicate(p);
       for(int i=0; i<p.getArgs().size(); i++) {
          if(returnPred.get(i) instanceof Term) {
             Predicate newPred = convertToVisionPredicate((Term)returnPred.get(i));
             returnPred.set(i,newPred);
          } else if(returnPred.get(i) instanceof Symbol) {
 	     ArrayList<Symbol> symList = new ArrayList<Symbol>();
             symList.add(p.get(i));
             symList.add(new Symbol("X"));
             Predicate queryPred = new Predicate("hasVisionID",symList);
             if(querySupport(queryPred)) {
                String vName = returnPred.get(i).getName().toUpperCase();
                Variable v = new Variable(vName,"");
                returnPred.set(i,v);
             }
          }
       }
       return returnPred;
    }

    /**
     * Send an update to the planner.
     */
    private void updatePlanner() {
        log.trace("enter updatePlanner()");
        double d = 0.0;
        Door mydoor;
        double x = 0.0;
        double y = 0.0;
        double ax = 0.0;
        double ay = 0.0;
        double tcurr = 0.0;
        double xdiff;
        double ydiff;
        boolean percept = false;
        ArrayList<ADEPercept> newPercepts = new ArrayList<ADEPercept>();
        ArrayList<Predicate> update = new ArrayList<Predicate>();

        if (attendPercepts[BOX]) {
            ArrayList<ADEPercept> boxes = null;
            if (checkMethod("getBoxes")) {
                try {
                    boxes = (ArrayList<ADEPercept>) callMethod("getBoxes");
                } catch (ADEException ace) {
                    log.error("Error getting boxes", ace);
                }
            }
            if (boxes != null) {
                for (ADEPercept b : boxes) {
                    if (pastPercepts.containsKey(b.name.toUpperCase())) {
                        continue;
                    }
                    update.add(new Predicate("isa", b.name, "box"));
                    update.add(new Predicate("color", b.name, b.color));
                    // not sure looked_for is right for attend
                    update.add(new Predicate("looked_for", b.name, "somewhere"));
                    update.add(new Predicate("found", b.name, "somewhere"));
                    update.add(new Predicate("in", b.name, "somewhere"));
                    newPercepts.add(b);
                    pastPercepts.put(b.name.toUpperCase(), b);

                    // Michael is't filling in the px,py so using x,y for now
                    b.sim_px = b.sim_x;
                    b.sim_py = b.sim_y;
                    log.debug("Adding box " + b.name + " color " + b.color + " at " + b.sim_px + "," + b.sim_py);
                }
            }
        }
        if (attendPercepts[BLOCK]) {
            ArrayList<ADEPercept> blocks = null;
            if (checkMethod("getBlocks")) {
                try {
                    blocks = (ArrayList<ADEPercept>)callMethod("getBlocks");
                } catch (ADEException ace) {
                    log.error("Error getting blocks",ace);
                }
            }
            if (blocks != null) {
                for (ADEPercept b : blocks) {
                    if (pastPercepts.containsKey(b.name.toUpperCase())) {
                        continue;
                    }
                    update.add(new Predicate("isa", b.name, "block"));
                    update.add(new Predicate("color", b.name, b.color));
                    // not sure looked_for is right for attend
                    update.add(new Predicate("looked_for", b.name, "somewhere"));
                    update.add(new Predicate("found", b.name, "somewhere"));
                    update.add(new Predicate("in", b.name, "somewhere"));
                    newPercepts.add(b);
                    pastPercepts.put(b.name.toUpperCase(), b);
                }
            }
        }
        if (attendPercepts[DOOR]) {
            if ((mydoor = detectNearestDoor()) != null) {
                x = mydoor.getCenter().getX();
                // PWS: 0.2 compensates for LRF offset from agent center; need to add call to robot base to get actual value
                y = mydoor.getCenter().getY() + 0.2;
                ax = mydoor.getApproach().getX();
                ay = mydoor.getApproach().getY() + 0.2;
                d = getDistanceFrom(0, 0, x, y);
                // make global, if possible
                if ((mydoor.getAgentPose() != null) && (d <= 1.75)) {
                    tcurr = mydoor.getAgentPose().getRotation() - Math.toRadians(90);
                    // adjust door position for my global pose
                    xdiff = Math.cos(tcurr) * x - Math.sin(tcurr) * y;
                    ydiff = Math.cos(tcurr) * y + Math.sin(tcurr) * x;
                    x = xdiff + mydoor.getAgentPose().getPosition().getX();
                    y = ydiff + mydoor.getAgentPose().getPosition().getY();

                    // adjust approach position for my global pose
                    xdiff = Math.cos(tcurr) * ax - Math.sin(tcurr) * ay;
                    ydiff = Math.cos(tcurr) * ay + Math.sin(tcurr) * ax;
                    ax = xdiff + mydoor.getAgentPose().getPosition().getX();
                    ay = ydiff + mydoor.getAgentPose().getPosition().getY();
                }
                log.debug("Got door at " + x + ", " + y + ", " + d);
                if ((d <= 2.00) && (checkDoor("DOOR" + currentDoor, x, y))) {
                    // Have a new nearby door, make a percept with some dummy values
                    ADEPercept o = new ADEPercept("door" + currentDoor, "door", Math.toRadians(90), d, x, y, ax, ay, 1.0, 1.0);
                    log.debug("new location " + o.name + " has location " + x + " " + y);
                    percept = true;
                    Predicate p = new Predicate("isa", o.name, "doorway");
                    update.add(p);
                    // we know it's open, because that's all this method detects
                    p = new Predicate("has_property", o.name, "open_pr");
                    update.add(p);
                    newPercepts.add(o);
                    pastPercepts.put(o.name.toUpperCase(), o);
                    // add the hallway approach point
                    d = getDistanceFrom(0, 0, ax, ay);
                    o = new ADEPercept("outside_room" + currentDoor, "location", Math.toRadians(0), d, ax, ay, ax, ay, 1.0, 1.0);
                    log.debug("new location " + o.name + " has location " + ax + " " + ay);
                    pastPercepts.put(o.name.toUpperCase(), o);
                }
            } else {
                // check "vision" for closed door
                ArrayList<ADEPercept> doorways = null;
                // getDoorways assumes forward-pointing camera
                if (checkMethod("lookFor")) {
                    try {
                        doorways = (ArrayList<ADEPercept>)callMethod("lookFor", "door");
                        for (int i = 0; i < doorways.size(); i++) {
                            if (doorways.get(i).open) {
                                doorways.remove(i);
                            }
                        }
                    } catch (ADEException ace) {
                        log.error("Error getting doorways",ace);
                    }
                } else if (checkMethod("getTypeId")) {
                    // get door from vision and create ADEPercept, as with LRF doors above
                    ArrayList<Long> tmpKeys = new ArrayList<Long>();
                    Predicate descriptor = utilities.Util.createPredicate("type(X, door)");
                    Long typeId = -1L;
                    try {
                        typeId = (Long)callMethod("getTypeId", descriptor);
                        tmpKeys = (ArrayList<Long>)callMethod("getTokenIds", typeId, 0.9);

                    } catch (ADEException ace) {
                        log.error("Error sending to Vision", ace);
                    }
                    if ((tmpKeys.size() > 0) /*&& !Long.class.isInstance(tmpKeys.get(0))*/) {
                        /* don't even need to check this when camera is side-mounted...
                        try {
                            MemoryObject r = (MemoryObject)callMethod("getToken",tmpKeys.get(0), 0.5);
                        } catch (ADEException ace) {
                            System.err.println("Error sending to Vision: " + ace);
                        }
                        */
                        log.debug("Saw a door!");
                        ADEPercept o = new ADEPercept("door" + currentDoor, "door", Math.toRadians(90), 0.5, x, y, ax, ay, 1.0, 1.0);
                        o.open = false;
                        doorways = new ArrayList<ADEPercept>();
                        doorways.add(o);
                    }
                }
                long currentTime = System.currentTimeMillis();
                long elapsed = currentTime - prevDoorTime;
                if (elapsed > 2500 && doorways != null && ! doorways.isEmpty()) {
                    double mydist = Double.POSITIVE_INFINITY;
                    boolean beside = false;
                    ADEPercept mydoorpercept = null;
                    for (ADEPercept door : doorways) {
                        if (door.distance < mydist) {
                            mydist = door.distance;
                            mydoorpercept = door;
                        }
                    }
                    if (mydoorpercept != null) {
                        beside = (mydoorpercept.heading > beside_min) && (mydoorpercept.heading < beside_max);
                        beside |= (mydoorpercept.heading < -beside_min) && (mydoorpercept.heading > -beside_max);
                    } else {
                        log.error("mydoorpercept is null!");
                    }
                    if (beside && (mydist <= 1.75) && (checkDoor(mydoorpercept.name, mydoorpercept.sim_x, mydoorpercept.sim_y))) {
                        log.debug(mydoorpercept.name + " is a " + mydoorpercept.type + " at heading " + mydoorpercept.heading);
                        percept = true;
                        prevDoorTime = currentTime;
                        Predicate p = new Predicate("isa", mydoorpercept.name, "doorway");
                        update.add(p);
                        // we know it's not open, because that's all this method detects
                        p = new Predicate("has_property", mydoorpercept.name, "closed_pr");
                        update.add(p);
                        newPercepts.add(mydoorpercept);
                        pastPercepts.put(mydoorpercept.name.toUpperCase(), mydoorpercept);
                        // add the hallway approach point
                        ADEPercept o = new ADEPercept("outside_room" + currentDoor, "location", Math.toRadians(0), mydist, mydoorpercept.sim_px, mydoorpercept.sim_py, mydoorpercept.sim_px, mydoorpercept.sim_py, 1.0, 1.0);
                        log.debug("new location " + o.name + " has location " + ax + " " + ay);
                        pastPercepts.put(o.name.toUpperCase(), o);
                    }
                }
            }
        }
        if (attendPercepts[ROBOT]) {
            ArrayList<ADEPercept> robots = null;
            percept = true;
            try {
                if (checkMethod("getPercepts")) {
                    robots = (ArrayList<ADEPercept>)callMethod("getPercepts", (Object)(new String[] {"robot"}));
                    for (ADEPercept o: robots) {
                        log.debug("seeing robot: " + o);
                        newPercepts.add(o);
                        pastPercepts.put(o.name.toUpperCase(), o);
                    }
                } else if (checkMethod("getTokenIds")) {
                    // get correct blob from vision and create ADEPercept, as with LRF doors above
                    ArrayList<Long> tmpKeys = new ArrayList<Long>();
                    ArrayList<Predicate> descriptors = new ArrayList<Predicate>();
                    descriptors.add(utilities.Util.createPredicate("type(X, blob)"));
                    descriptors.add(utilities.Util.createPredicate("color(X, blue)"));
                    Long typeId = -1L;
                    try {
                        typeId = (Long)callMethod("getTypeId", descriptors);
                        tmpKeys = (ArrayList<Long>)callMethod("getTokenIds", typeId, 0.9);

                    } catch (ADEException ace) {
                        log.error("Error sending to Vision",ace);
                    }
                    if (tmpKeys != null && !tmpKeys.isEmpty()) {
                        MemoryObject r = (MemoryObject)callMethod("getToken",tmpKeys.get(0), 0.5);
                        for (Object k : tmpKeys) {
                            String key = (String)k;
                            MemoryObject t = (MemoryObject)callMethod("getToken",key,0.5);
                            if (t.getArea() > r.getArea()) {
                                r = t;
                            }
                        }
                        // dummy object with default values
                        ADEPercept o = new ADEPercept("transport", "robot", r.getPan(),1.0,1.0, 0.0, 1.0, 0.0, 1.0, 1.0);
                        newPercepts.add(o);
                        pastPercepts.put(o.name.toUpperCase(), o);
                    }
                }
            } catch (ADEException ace) {
                log.error(myID + ": error getting robot info", ace);
            }
        }
        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        /*
        if (false && attendPercepts[LAND] && gotVision) {
        ArrayList<ADEPercept> landmarks;
        try {
        landmarks = (ArrayList<ADEPercept>)call(vsi, "getLandmarks");
        for (ADEPercept o: landmarks) {
        newPercepts.add(o);
        pastPercepts.put(o.type.toUpperCase(), o);
        }
        } catch (ADEException ace) {
        System.out.println(myID + ": error getting landmark info: " + ace);
        }
        }
         */
        currentPercepts = newPercepts;
        if (percept) {
            processStateUpdate(update, true);
        }

        // get all outstanding state updates and send them
        ArrayList<Predicate> toSend = new ArrayList<Predicate>();
        boolean toSendReplan;
        synchronized (stateUpdates) {
            toSend.addAll(stateUpdates);
            toSendReplan = stateUpdatesReplan;
            stateUpdates.clear();
            stateUpdatesReplan = false;
        }
        if (! toSend.isEmpty()) {
            if (checkMethod("updatePlannerState")) {
                try {
                    callMethod(0, "updatePlannerState", toSend, toSendReplan);
                } catch (ADEException ace) {
                    log.error("Error updating Planner", ace);
                }
            }
            // also send updates to belief, etc., here?
        }
        ActionDBEntry plan;
        /*
        if (plannerAI.rootStatus != ActionStatus.PROGRESS) {
        // Do something to update Planner...
        }
         */
        if (checkMethod("getPlan")) {
            try {
                plan = (ActionDBEntry) callMethod("getPlan");
            } catch (ADEException ace) {
                log.error("Error getting plan from Planner",ace);
                plan = null;
            }
            log.debug(format("[updatePlanner.getPlan] :: Plan %s", String.valueOf(plan)));
            if (plan != null) {
                //System.out.println("Got plan from Planner");
                // create new AI, etc.
                if (plannerAI != null) {
                    plannerAI.cancel();
                }
                GoalManagerGoal plannerGoal;
                if (plannerGoals.size() > 0) {
                    for (GoalManagerGoal gmg:plannerGoals) {
                        if (gmg.getGoalStatus() == ActionStatus.INITIALIZE) {
                            gmg.setGoalStatus(ActionStatus.PROGRESS);
                        }
                    }
                    plannerGoal = plannerGoals.get(0);
                } else {
                    //System.err.println(myID + ": something's wrong, no planner goal...");
                    plannerGoal = new GoalManagerGoal(createPredicate("something(todo)"));
                }
                plannerAI = addAction(plannerGoal, plan, ActionDBEntry.lookup("obey commands"));
            }
        }
    }

    // Handle some operations on goals in the goal manager
    private boolean checkMetaAction(final GoalManagerGoal gmg) {
        log.trace("enter checkMetaAction(final GoalManagerGoal gmg)");
        boolean retval = false;
        Predicate g = gmg.getGoal();
        String name = g.getName().toLowerCase();
        ArrayList<Symbol> args = g.getArgs();
        if (name.equalsIgnoreCase("reported") && (args.size() > 0)) {
            String rep = args.get(0).getName().toLowerCase();
            if (rep.equalsIgnoreCase("orders")
                    || rep.equalsIgnoreCase("goal")
                    || rep.equalsIgnoreCase("goals")) {
                new Thread() {
                    @Override
                    public void run() {
                        String output = "I have been ordered to";
                        //sayText("My orders are to");
                        //Sleep(1000);
                        int i = actions.size();
                        // sayText all the goal descriptions
                        synchronized (actions) {
                            for (ActionInterpreter ai : actions) {
                                i--;
                                // Need to have the Planner plans run in the AI created
                                // when goal submitted.  For now, ignore them.
                                if (ai.cmd.startsWith("PlannerGeneratedScript")) {
                                    continue;
                                }
                                log.debug("Report: " + actions.size() + " actions, i == " + i);
                                if ((actions.size() > 1) && (i == 0)) {
                                    output += ", and " + ai.description;
                                    //sayText("and " + ai.description);

                                } else if ((actions.size() > 1) && (i > 0)) {
                                    output += ", " + ai.description;
                                } else {
                                    output += " " + ai.description;
                                    //sayText(ai.description);
                                }
                                //Sleep(2500);
                            }
                            output += ".";
                            sayText(output);
                        }
                        gmg.terminate(ActionStatus.SUCCESS);
                    }
                }.start();
                retval = true;
            } else if (rep.equalsIgnoreCase("current-action")) {
                synchronized (actions) {
                    for (ActionInterpreter ai : actions) {
                        sayText("Executing " + ai.rootAction.getName());
                    }
                }
            }
        } else if (name.equalsIgnoreCase("ensure") || name.equalsIgnoreCase("maintain")) {
            // create a receivedOrders AI to hold the locks
            ArrayList<String> taskSpec = new ArrayList<String>();
            taskSpec.add("maintain");
            taskSpec.add("me");
            ActionInterpreter ai = addAction(gmg, taskSpec, ActionDBEntry.lookup("obey commands"));
            goals.put(gmg.getGoalId(), gmg);
            ai.description = g.getName();
            retval = true;
        } else if (name.equalsIgnoreCase("cancel")) {
            g = (Predicate) args.get(0);
            if (g.getName().equalsIgnoreCase("ensure") || g.getName().equalsIgnoreCase("maintain")) {
                g = (Predicate) g.getArgs().get(0);
            }
            synchronized (actions) {
                for (ActionInterpreter ai : actions) {
                    // This is a cheesy way to handle it
                    if ((ai.getGoal() != null) && g.toString().equalsIgnoreCase(ai.getGoal().toString())) {
                        // Also need to handle status?
                        ai.cancel();
                        gmg.terminate(ActionStatus.CANCEL);
                    }
                }
            }
            retval = true;
        }
        return retval;
    }

    // check postponed goals to see whether the start condition has been met
    private void checkPostponedGoals() {
        log.trace("enter checkPostponedGoals()");
        for(Predicate wait : postponedGoals.keySet()) {
            log.debug("Checking goal postponed for: " + wait);
            String name = wait.getName();
            String arg = wait.getName().toString();
            if (name.equals("unable") && checkMethod(arg)) {
                log.debug("found method goal was waiting for");
                ArrayList<GoalManagerGoal> newgoals = postponedGoals.remove(wait);
                sayText("I am now able to "+ arg +", resuming postponed goals");
                for (GoalManagerGoal g : newgoals) {
                    submitGoal(g);
                }
                log.debug("Resubmitted goals waiting on " + wait +
                        ", but not doing anything useful with gids");
            } else {
                log.debug("no new method found for " + arg);
            }
        }
    }

    // Create a goal and pass it off to Planner.
    private void plannerGoal(GoalManagerGoal gmg, int utility, int timeout) {
        log.trace("enter plannerGoal(GoalManagerGoal gmg, int utility, int timeout)");
        Predicate goal = gmg.getGoal();
        String goalString = goal.getName().toLowerCase();
        String goalType = "goal";
        // check for and (if necessary) extract inner soft goal
        if (goalString.equalsIgnoreCase("tried")) {
            goalType = "softgoal";
            goal = (Predicate) goal.getArgs().get(0);
        }
        goalString = applyFunction(goal);
        log.info("Sending goal to Planner: " + goalString);
        try {
            callMethod("submitPlannerGoal", createPredicate(goalString),
                    goalType.equals("goal"), (double)utility, (double)timeout);
            plannerGoals.add(gmg);
        } catch (ADEException ace) {
            log.error("Error sending to planner", ace);
            gmg.terminate(ActionStatus.FAIL);
        }
    }

    // submit plannerGoal to FODDComponent
    protected void plannerGoal(GoalManagerGoal gmg) {
        log.trace("enter plannerGoal(GoalManagerGoal gmg)");
        Predicate goal = gmg.getGoal();
        String goalString = applyFunction(goal);
        log.info("Sending goal to Planner: " + goalString);
        try {
            log.info(createPredicate(goalString));
            callMethod("submitPlannerGoal", createPredicate(goalString));
            plannerGoals.add(gmg);
        } catch (ADEException ace) {
            log.error("Error sending to planner", ace);
            gmg.terminate(ActionStatus.FAIL);
        }
    }

    // recursively apply functions from the ADBE FunctionDB
    private String applyFunction(Predicate p) {
        log.trace("enter applyFunction(Predicate p)");
        String s;
        //= adb.lookupFunction(p);
        s = p.getName().toLowerCase();
        ArrayList<Symbol> args = p.getArgs();
        for (int i = 0; i < args.size(); i++) {
            String a;
            if (Predicate.class.isInstance(args.get(i))) {
                a = adb.lookupFunction((Predicate) args.get(i)).toString().toLowerCase();
            } else {
                a = args.get(i).getName().toLowerCase();
            }
            if (!a.equalsIgnoreCase("cramer")) {
                s += " " + a;
            }
        }
        log.debug("Predicate is: " + s);
        return s;
    }

    protected boolean checkGoalConflict(Predicate p) {
        log.trace("enter checkGoalConflict(Predicate p)");
        boolean conflict = false;
        while (p.getName().equalsIgnoreCase("not")) {
            conflict = !conflict;
            p = (Predicate) p.getArgs().get(0);
        }
        String pname = p.getName();
        int pargs = p.getArgs().size();
        synchronized (actions) {
            for (ActionInterpreter ai : actions) {
                if (ai.getGoal() == null) {
                    continue;
                }
                Predicate g = ai.getGoal();
                while (g.getName().equalsIgnoreCase("not")) {
                    conflict = !conflict;
                    g = (Predicate) g.getArgs().get(0);
                }
                if (!pname.equalsIgnoreCase(g.getName())) {
                    continue;
                }
                if (pargs != g.getArgs().size()) {
                    continue;
                }
                boolean match = true;
                for (int i = 0; i < pargs; i++) {
                    String parg;
                    if (Predicate.class.isInstance(p.getArgs().get(i))) {
                        parg = adb.lookupFunction((Predicate) p.getArgs().get(i)).toString().toLowerCase();
                    } else {
                        parg = p.getArgs().get(i).getName().toLowerCase();
                    }
                    String garg;
                    if (Predicate.class.isInstance(g.getArgs().get(i))) {
                        garg = adb.lookupFunction((Predicate) g.getArgs().get(i)).toString().toLowerCase();
                    } else {
                        garg = g.getArgs().get(i).getName().toLowerCase();
                    }
                    if (!parg.equalsIgnoreCase(garg)) {
                        match = false;
                        break;
                    }
                }
                if (match) { // assumes no other match conflicting with this one...
                    return conflict;
                }
            }
        }
        return false;
    }

    /* cancel an action by name */
    protected void cancelAction(String aname) {
        log.trace("enter cancelAction(String aname)");
        synchronized (actions) {
            for (ActionInterpreter ai : actions) {
                if (ai.cmd == null) {
                    continue;
                }
                String cname = ai.cmd.substring(0, ai.cmd.indexOf("("));
                if (aname.equalsIgnoreCase(cname)) {
                    // Also need to handle status?
                    ai.cancel();
                }
            }
        }
    }

    private ArrayList<ActionEvent> getEvents() {
        log.trace("enter getEvents()");
        return events;
    }

    protected int getNumActions() {
        log.trace("enter getNumActions()");
        return actions.size();
    }

    final protected boolean getAttend(int a) {
        log.trace("enter getAttend(int a)");
        if (a < 0 || a >= perceptTypes) {
            return false;
        }
        return attendPercepts[a];
    }

    final protected void setAttend(int a, boolean v) {
        log.trace("enter setAttend(int a, boolean v)");
        attendPercepts[a] = v;
    }

    final protected void setSimulateFirst(boolean s) {
        log.trace("enter setSimulateFirst(boolean s)");
        simulateFirst = s;
    }

    final protected void setLocation(String l) {
        log.trace("enter setLocation(String l)");
        location = l;
    }

    final void setConversationState(boolean s, ActionInterpreter caller) {
        log.trace("enter setConversationState(boolean s, ActionInterpreter caller)");
        conversationState = s;
        for (ActionInterpreter ai:actions) {
            if (ai != caller) {
                ai.pause(s);
                if (!s && !ai.started) {
                    try {
                        ai.start();
                    } catch (IllegalThreadStateException itse) {
                        log.error(myID + ": error starting AI after conversation end", itse);
                    }
                }
                // anything else I need to do here?
            }
        }
    }



    final protected String getAgentName() {
        log.trace("enter getAgentName()");
        return agentname;
    }

    final protected void setSphinxGrammar(String g) {
        log.trace("enter getSphinxGrammar(String g)");
        sphinxGrammar = g;
    }

    final protected void setCurrentPosition(double[] position) {
        log.trace("enter getCurrentPosition(double[] position)");
        current_x = position[0];
        current_y = position[1];
        current_t = position[2];
    }

    /**
     * Check whether we're waiting for methods provided by this server type.
     * @param stype server type
     * @return true if we're waiting for such a method
     */
    final protected boolean checkUnboundMethods(String stype) {
        log.trace("enter checkUnboundMethods(String stype)");
        HashMap<String, ADEMethodConditions> methods;
        try {
            methods = getComponentMethods(stype);
        } catch (ADEException ace) {
            log.error(myID + ": failed to get methods for " + stype, ace);
            return false;
        }
        log.debug(format("[checkUnboundMethods] :: Component: %s, Methods: %s", stype, methods));
//        log.debug("NOT Checking for method conditions...");
        for(String method : methods.keySet()) {
            String mn = method.substring(0, method.indexOf("("));
            // TODO: check argument types
            log.debug("New method " + method + " (" + mn + ")");
            synchronized (unboundMethods) {
                if (unboundMethods.contains(mn))
                    return true;
            }
            /*
            MethodConditions mc;
            if ((mc = methods.get(method)) != null) {
                System.out.println("Got method conditions");
                System.out.println("  Precondition: ");
                for(String s : mc.Preconditions())
                    System.out.println("  " + s);
                System.out.println("  Postcondition: ");
                for(String s : mc.Postconditions())
                    System.out.println("  " + s);

            } else {
                System.out.println("No method conditions");
            }
            */
        }
        return false;
    }

    // Called when a server connects, adds the methods of its interface that
    // match methods in requested interfaces to the global methods table for
    // callMethod.
    final protected void addMethods(String stype, Component srv) {
        HashMap<String, ADEMethodConditions> methods;
        try {
            methods = getComponentMethods(stype);
        } catch (ADEException ace) {
            log.error(myID + ": failed to get methods for " + stype, ace);
            return;
        }
        if (methods == null) {
            log.error("Got no methods from " + stype);
            return;
        }
        log.debug(format("[addMethods] :: Component: %s, Methods: %s", stype, Joiner.on(", ").join(methods.keySet())));
        for (String method : methods.keySet()) {
            String mn = method.substring(0, method.indexOf("("));
            // TODO: check argument types
            synchronized (unboundMethods) {
                unboundMethods.remove(mn);
            }
            PriorityQueue<Component> sQueue = globalMethods.get(mn);
            if (sQueue == null) {
                sQueue = new PriorityQueue<Component>();
                globalMethods.put(mn, sQueue);
            }
            sQueue.add(srv);

        }
    }

    // Called when an interface is requested; adds the methods of that
    // interface to the unboundMethods table for matching when server
    // connects.
    final protected void addMethods(String stype) {
        HashMap<String, ADEMethodConditions> methods;
        log.debug("[addMethods] Getting methods for " + stype);
        try {
            methods = getComponentMethods(stype);
        } catch (ADEException ace) {
            log.error(format("[addMethods] Failed to get methods for %s", stype), ace);
            return;
        }

        log.debug(format("[addMethods] :: Component: %s, Methods: %s", stype, Joiner.on(", ").join(methods.keySet())));
        for(String method : methods.keySet()) {
            String mn = method.substring(0, method.indexOf("("));
            // TODO: check argument types
            // this is protected by getInitialRefs
            synchronized (unboundMethods) {
                unboundMethods.add(mn);
            }
        }
    }

    /**
     * Get the reference for the named method.
     * @param mn the method name
     * @return the highest-priority reference for that method
     */
    final protected Object getMethodRef(String mn) {
        PriorityQueue<Component> sQueue = globalMethods.get(mn);
        Object sref = null;

        if (sQueue != null) {
            Component srv = sQueue.peek();
            sref = srv.getReference();
        }   else {
            log.warn(format("[getMethodRef] :; No object reference with associated %s", mn));
        }
        return sref;
    }

    /**
     * Get the reference for the named method in the named server.
     * @param mn the method name
     * @param sn the server name
     * @return the highest-priority reference for that method
     */
    final protected Object getMethodRef(String mn, String sn) {
        log.trace("enter getMethodsRef(String mn, String sn)");
        PriorityQueue<Component> sQueue = globalMethods.get(mn);
        String sType = sn;
        String sName = null;
        int sPriority = Integer.MAX_VALUE;
        Object sReference = null;

        try {
            sType = getTypeFromID(sn);
            sName = getNameFromID(sn);
        } catch (IndexOutOfBoundsException ioobe) {
        }
        for (Component srv : sQueue) {
            if (srv.getType().equals(sType)) {
                if (sName == null) {
                    // have to find the highest-priority Component (less is higher)
                    if (srv.getPriority() < sPriority) {
                        sPriority = srv.getPriority();
                        sReference = srv.getReference();
                    }
                } else if (srv.getName().equals(sName)) {
                    sReference = srv.getReference();
                    break;
                }
            }
        }
        return sReference;
    }

    /**
     * Acquire the initial references and wait for them all to connect; take
     * care of some additional bookkeeping.
     */
    final protected void getInitialRefs() {
        log.trace("enter getInitialRefs()");
        new Thread() {
            @Override
            public void run() {
                boolean gotrefs = false;
                int missing = 0;
                Sleep(1000);
                requestInitialRefs();
                while (! gotrefs) {
                    synchronized (unboundMethods) {
                        missing = unboundMethods.size();
                        gotrefs = (missing == 0);
                        if (!gotrefs) {
                            log.debug(format("[getInitialRefs] :: Found methods: %s", Joiner.on(", ").join(unboundMethods)));
                        }
                    }
                    //for (String m: unboundMethods) {
                    //    System.out.println("missing: " + m);
                    //}
                    Sleep(500);
                }
                // for some reason, even after the reference is given
                // (componentConnectReact), some servers aren't ready yet;
                // calling servicesReady is not an option before the ref is
                // ready, so I check on it here.
                for (List<Component> sList : servers.values()) {
                    for (Component srv : sList) {
                        Object sref = srv.getReference();
                        while (! getRecoveryState(sref).equals(RecoveryState.OK)) {
                            Sleep(200);
                        }
                    }
                }
                if (checkMethod("setActor")) {
                    try {
                        callMethod("setActor", agentname);
                        log.debug("Set Discourse actor to " + agentname);
                    } catch (ADEException ace) {
                        log.error("FAILURE: setActor", ace);
                    }
                }
                if (checkMethod("setInteractor") && (Subject != null)) {
                    try {
                        callMethod("setInteractor", Subject);
                        log.debug("Set Discourse interactor to " + Subject);
                    } catch (ADEException ace) {
                        log.error("FAILURE: setInteractor", ace);
                    }
                }
                // at this point, should have all requested refs
                gotRefs = true;
				//quick hacky way to fix concurrent modification error
				List<List<Component>> sListList = new ArrayList(servers.values());
                for (List<Component> sList : sListList) {
				    List<Component> sListTmp = new ArrayList(sList);
                    for (Component srv : sListTmp) {
                        Object sref = srv.getReference();
                        try {
                            while (! (Boolean)call(sref, "servicesReady", myID)) {
                                Sleep(100);
                                log.debug("waiting on.... " + srv.getName());
                            }
                        } catch (ADEException ace) {
                            log.error(myID + ": error checking servicesReady for " + srv.getName(), ace);
                        }
                    }
                }
                refsReady = true;
            }
        }.start();
    }

    final protected void startGoalManagement() {
        new Thread() {
            @Override
            public void run() {
                while (!gotRefs) {
                    Sleep(100);
                }
                initialized = true;
                log.debug("...got refs");
                while (!refsReady) {
                    Sleep(100);
                }
                log.debug("[startGoalManagement] Component references ready.");
                u.start();
                if (checkReference("com.sapa.SapaComponent", false)) {
                    while (! plannerInit)
                        Sleep(50);
                }
                for (Object o:nameIdMap.keySet()) {
                    log.debug("nameIdMap contains: " + o + " - " + nameIdMap.get(o));
                }
                synchronized (actions) {
                    for (ActionInterpreter a : actions) {
                        log.trace(format("[startGoalManagement] Starting ActionInterpreter %s", a));
                        a.start();
                    }
                }
            }
        }.start();
    }

    /**
     * Request all references listed in the start-up parameters.
     */
    final protected void requestInitialRefs() {
        log.trace("enter requestInitialRefs()");
        synchronized (initLock) {
            PriorityQueue<Component> sQueue = new PriorityQueue<Component>();
            Component srv;
            for (List<Component> sList : servers.values()) {
                sQueue.addAll(sList);
            }
            while ((srv = sQueue.poll()) != null) {
                log.debug(myID + " requesting server: " + srv);
                requestInitialRef(srv);
            }
        }
    }

    /**
     * Request an individual reference.
     * @param server the server to request
     */
    final protected void requestInitialRef(Component server) { // soon add conds
        log.trace("enter requestInitialRef(Component server)");
        Object sref = null;
        String stype = server.getType();
        String sname = server.getName();
        // get the methods and put them in the unboundMethods hashmap
        addMethods(stype);
        log.debug("GoalManagerImpl.requestInitialRef - 1");
        while (sref == null) {
            if (sname != null) {
                server.setReference(sref = getClient(stype, sname));
            } else {
                server.setReference(sref = getClient(stype));
            }
            Sleep(100);
        }
        log.debug("GoalManagerImpl.requestInitialRef - 2");
        while (! server.getStatus()) {
            Sleep(100);
        }
        log.debug("GoalManagerImpl.requestInitialRef - 3");
    }

    final protected void startUpAction() {
        log.trace("enter startUpAction()");
        log.info("GM status: INITIALIZING...");
        resourceLocks.add(motionLock);
        resourceLocks.add(transmitLock);
        resourceLocks.add(speechLock);
        resourceLocks.add(visionLock);
        resourceLocks.add(lightLock);

        if (useLocalLogger) {
            log.debug("Starting ADE local logging...");
            setASL(true);
        }

        attendPercepts = new boolean[perceptTypes];
        for (int i = 0; i < perceptTypes; i++) {
            attendPercepts[i] = false;
        }
        currentPercepts = new ArrayList<ADEPercept>();
        pastPercepts = new HashMap<String, ADEPercept>();
        //ADEPercept o = new ADEPercept("hall_end", "landmark", Math.toRadians(0), 15.25, 15.25, -0.0, 15.25, -0.0, 1.0, 1.0);
        //pastPercepts.put("HALL_END", o);
        nameIdMap = new HashMap<Object, Object>();

        log.info("Using database: " + dbFilename);
        adb = new ActionDBEntry(dbFilename, printAPI);

        for (String name : dbFileSupp) {
            log.debug(format("Merging %s into action database", name));
            adb.mergeDB(name, printAPI);
        }
        // add all adb forbidden actions
        for (Predicate e : adb.getBadActions()) {
            proscribedActions.add(e);
        }
        // add all adb forbidden states
        for (Predicate e : adb.getBadStates()) {
            proscribedStates.add(e);
        }
        // now create tasks for all adb execs
        for (String e : adb.getExecs()) {
            ArrayList<String> taskSpec = new ArrayList<String>();
            StringTokenizer exec = new StringTokenizer(e);
            while (exec.hasMoreTokens()) {
                String t = exec.nextToken();
                if (t.equals("?subject") && (Subject != null)) {
                    taskSpec.add(Subject);
                } else {
                    taskSpec.add(t);
                }
            }
            initScripts.add(taskSpec);
        }
        // submit goals for all adb goals
        for (Predicate e : adb.getGoals()) {
            submitGoal(e);
        }
        // create tasks for all command line scripts
        for (ArrayList<String> taskSpec : initScripts) {
            ActionDBEntry task = ActionDBEntry.lookup(taskSpec.get(0).toLowerCase());
            Predicate g;
            if (task.getPostconds() != null && task.getPostconds().size() > 0) {
                g = task.getPostconds().get(0);
            } else {
                g = new Predicate(taskSpec.get(0));
            }
            GoalManagerGoal gmg = new GoalManagerGoal(g);
            // The motive here is "obey commands" because what
            // it's given in the startup file is a command, sort of
            addAction(gmg, taskSpec, ActionDBEntry.lookup("obey commands"));
            goals.put(gmg.getGoalId(), gmg);
        }
        // submit goals for all command line goals
        for (Predicate g : initGoals) {
            submitGoal(g);
        }
        // Notify of all joining servers
        log.debug("Registering new component notifications...");
        registerNewComponentNotification(new String[][]{{}},true);

        log.debug("Getting initial component refs...");
        getInitialRefs();

        log.debug("Starting goal management...");
        startGoalManagement();

        log.info("GM status: READY");
    }

    // need to call predicateMatch on state description
    protected static boolean badState(Predicate desc) {
        log.trace("enter badState(Predicate desc)");
        boolean bad = false;

        for (Predicate nono:proscribedStates) {
            if (ActionDBEntry.predicateMatch(desc, nono)) {
                bad = true;
                break;
            }
        }
        return bad;
    }

    // need to extract argument types from the action, construct a new predicate, and call predicateMatch on that
    protected static boolean badAction(ActionDBEntry action) {
        boolean bad = false;
        Predicate desc = action.getPredDesc();

        for (Predicate nono:proscribedActions) {
            if (ActionDBEntry.predicateMatch(desc, nono)) {
                bad = true;
                break;
            }
        }
        log.trace(format("[badAction] :: Action '%s' bad? %s", action, String.valueOf(bad)));
        return bad;
    }

    /**
     * Say some text, if possible.
     * @param text text to speak
     * @return true if successful, false otherwise
     */
    protected boolean sayText(String text) {
        log.trace("enter sayText(String text)");
        log.debug(myID + " sayText: " + text);
        if (checkMethod("sayText")) {
            try {
                callMethod(0, "sayText", text);
            } catch (ADEException ace) {
                log.error("sayText: Error sending to speech",ace);
                return false;
            }
            return true;
        } else {
            log.debug(myID + ": would say \"" + text + "\"");
        }
        return false;
    }

    /**
     * Get a script from Spex to find the location and start it.
     * @param id the Spex location id
     * @return -1L for failure, 1L for success (submitting, not finding)
     */
    long spexScript(long id, ActionInterpreter ai) {
        log.trace("enter spexScript(long id, ActionInterpreter ai)");
        long retval = -1L;
        log.debug("goal manager spexScript");
        if (checkMethod("getScript")) {
            try {
                ActionDBEntry script = (ActionDBEntry)callMethod("getScript", id);
                if (script != null) {
                    log.debug("got script from spex");
                    GoalManagerGoal spexGoal;
                    spexGoal = new GoalManagerGoal(createPredicate("findloc("+id+")"));
                    plannerAI = addAction(spexGoal, script, ActionDBEntry.lookup("obey commands"));
                    retval = 1L;
                }
            } catch (ADEException ace) {
                log.error(myID + ": error getting script from Spex", ace);

            }
        }
        return retval;
    }

    /**
     * The <code>Updater</code> is the main loop for the action
     * interpreter, performing a small amount of periodic housekeeping and
     * then calling the action interpreter method <code>runCycle</code>.
     */
    private class Updater extends Thread {
        int sleepTime; // Add cycleTime
        boolean shouldUpdate;

        public Updater(int st) {
            log.trace("enter Updater constructor");
            sleepTime = st;
            shouldUpdate = true;
        }

        BufferedReader procstat = null;
        String ps = null;
        StringTokenizer tps = null;
        long current = 0L, past = 0L, elapsed = 0L;
        double userp = 0.0;
        double ptotal = 0.0, pidle = 0.0, ctotal = 0.0, cidle = 0.0;

        @Override
        public void run() {
            log.trace("enter run()");
            log.debug("SHOULDRUN = " + shouldUpdate);
            System.out.flush();
            if (logCPU) {
                new Thread() {
                    @Override
                    public void run() {
                        long current = 0L, past = 0L, elapsed = 0L;
                        while (shouldUpdate) {
                            past = System.currentTimeMillis();
                            CPUStat();
                            current = System.currentTimeMillis();
                            elapsed = current - past;
                            if ((elapsed < 1000) && (elapsed >= 0))
                                Sleep(1000-elapsed);
                        }
                    }
                }.start();
            }
            if (checkReference("com.spex.SpexComponent", false)) {
                try {
                    log.debug(myID + ": getting initial location from Spex");
                    long id = (Long)callMethod("getCurrentPlaceID");
                    String name = (String)nameIdMap.get(id);
                    Predicate update;
                    if (name == null) {
                        // create name, add to map, send to planner
                        name = "spex" + id;
                        nameIdMap.put(name, id);
                        nameIdMap.put(id, name);
                        update = createPredicate("isa("+name+",room)");
                        log.debug(myID + ": adding initial location " + update);
                        synchronized (stateUpdates) {
                            stateUpdates.add(update);
                        }
                    }
                    update = createPredicate("at(" + agentname + "," + name + ")");
                    log.debug(myID + ": placing me at initial location " + update);
                    synchronized (stateUpdates) {
                        stateUpdates.add(update);
                    }
                } catch (ADEException ace) {
                    log.error(myID + ": error getting start location from Spex", ace);
                }
            }
            if (checkReference("com.sapa.SapaComponent", false)) {
                //attendPercepts[DOOR] = true;
                //attendPercepts[LAND] = true;

                log.debug("About to initialize planner ...");
                initializePlanner();
                log.debug("... done initializing planner! ");
            }
            if (checkReference("com.prolog.FODDComponent", false)) {
                log.debug("Got FODD, watching for doors.");
                attendPercepts[DOOR] = true;
                //attendPercepts[BOX] = true;
                //attendPercepts[BLOCK] = true;
            }
            if (spexTest && checkReference("com.spex.SpexComponent", false)) {
                ArrayList<Predicate> desc = new ArrayList<Predicate>();
                desc.add(createPredicate("vartype(R,endroom)"));
                desc.add(createPredicate("type(H,hallway)"));
                desc.add(createPredicate("connected(R,H)"));
                desc.add(createPredicate("to_left(R,C)"));
                Long id = -1L;
                try {
                    id = (Long)callMethod("provideLandmarkInfo", desc);
                } catch (ADEException ace) {
                    log.error(myID + ": error getting id from Spex", ace);
                }
                log.debug("*SPEX* location ID is: " + id);
                if (id >= 0) {
                    try {
                        ActionDBEntry script = (ActionDBEntry)callMethod("getScript", id);
                        if (script != null) {
                            GoalManagerGoal spexGoal;
                            spexGoal = new GoalManagerGoal(createPredicate("find(endroom)"));
                            plannerAI = addAction(spexGoal, script, ActionDBEntry.lookup("obey commands"));
                        }
                    } catch (ADEException ace) {
                        log.error(myID + ": error getting script from Spex", ace);
                    }
                }
            }
            if (false && checkReference("com.mds.simulator.MdsSimComponent", false)) {
                new Thread() {
                    @Override
                    public void run() {
                        /*
                        try {
                            callMethod("neutralFace");
                            //callMethod("setPose", "ARMS_TUCKED");
                            callMethod("setAngle", MDSJoint.LThumb, 0.6f);
                            callMethod("setAngle", MDSJoint.LIndex, 0.6f);
                            callMethod("setAngle", MDSJoint.LMiddle, 0.6f);
                            callMethod("setAngle", MDSJoint.LPinky, 0.6f);
                            callMethod("setAngle", MDSJoint.LWristFlex, 0.7f);
                            callMethod("setAngle", MDSJoint.LWristRoll, -0.7f);
                            callMethod("setAngle", MDSJoint.LShoulderPitch, -0.75f);
                            callMethod("setAngle", MDSJoint.LElbow, 0.3f);
                            callMethod("setAngle", MDSJoint.LUpperArmRoll, 0.1f);
                            callMethod("setAngle", MDSJoint.RThumb, 0.6f);
                            callMethod("setAngle", MDSJoint.RIndex, 0.6f);
                            callMethod("setAngle", MDSJoint.RMiddle, 0.6f);
                            callMethod("setAngle", MDSJoint.RPinky, 0.6f);
                            callMethod("setAngle", MDSJoint.RWristFlex, -0.7f);
                            callMethod("setAngle", MDSJoint.RWristRoll, 0.7f);
                            callMethod("setAngle", MDSJoint.RShoulderPitch, 0.75f);
                            callMethod("setAngle", MDSJoint.RElbow, -0.3f);
                            callMethod("setAngle", MDSJoint.RUpperArmRoll, -0.1f);
                            Sleep(5000);

                            callMethod("setAngle", MDSJoint.RShoulderAbduct, -1.15f);
                            callMethod("setAngle", MDSJoint.LShoulderAbduct, 1.15f);
                            Sleep(500);
                            callMethod("setAngle", MDSJoint.RShoulderAbduct, -1.15f);
                            Sleep(500);
                            callMethod("setAngle", MDSJoint.RShoulderAbduct, -1.15f);
                            Sleep(500);
                            callMethod("setAngle", MDSJoint.RShoulderAbduct, -1.15f);
                            Sleep(8000);
                        } catch (ADEException ace) {
                            System.err.println(myID + ": error relaxing MdsSimComponent: " + ace);
                            ace.printStackTrace();
                        }
		    */
                    }
                }.start();
            }

            while (shouldUpdate) {
                //myDetectNearestDoor();
                try {
                    past = System.currentTimeMillis();
                    updatePriorities();
                    updateAffect();
                    if (checkReference("com.sapa.SapaComponent") ||
                        checkReference("com.prolog.FODDComponent") ||
                        checkReference("com.interfaces.PlannerComponent")) {
                        updatePlanner();
                    }
                    if (checkReference("com.spex.SpexComponent")) {
                        updateSpex();
                    }
                    if (checkReference("com.dialogue.belief.BeliefComponent")) {
                        updateBelief();
                    }
                    current = System.currentTimeMillis();
                    elapsed = current - past;
                    if ((elapsed < sleepTime) && (elapsed >= 0))
                        Sleep(sleepTime-elapsed);
                } catch (Exception e1) {
                    log.error(myID + ": got generic exception", e1);
                }
            }
            log.trace(myID + ": Exiting Updater thread ...");
        }

        private void CPUStat() {
            log.trace("enter CPUStat()");
            try {
                procstat = new BufferedReader(new FileReader("/proc/stat"));
                ps = procstat.readLine();
                procstat.close();
                tps = new StringTokenizer(ps);
                // get rid of "cpu" tag
                tps.nextToken();
                // new values
                ctotal = Double.parseDouble(tps.nextToken());
                ctotal += Double.parseDouble(tps.nextToken());
                ctotal += Double.parseDouble(tps.nextToken());
                cidle = Double.parseDouble(tps.nextToken());
                ctotal += cidle;
                ctotal += Double.parseDouble(tps.nextToken());
                ctotal += Double.parseDouble(tps.nextToken());

                ptotal = ctotal - ptotal;
                pidle = cidle - pidle;
                pidle = pidle / ptotal;
                userp = 1 - pidle;
                canLogIt("CPU " + userp);
                log.debug(myID + ": CPU " + userp);
                ptotal = ctotal;
                pidle = cidle;
                /*
                procstat = new BufferedReader(new FileReader("/proc/uptime"));
                ps = procstat.readLine();
                procstat.close();
                tps = new StringTokenizer(ps);
                ctotal = Double.parseDouble(tps.nextToken());
                cidle = Double.parseDouble(tps.nextToken());
                ptotal = ctotal - ptotal;
                pidle = cidle - pidle;
                pidle = pidle / ptotal;
                userp = 1 - pidle;
                canLogIt("CPU " + userp);
                System.out.println("CPU " + userp);
                ptotal = ctotal;
                pidle = cidle;
                 */
            } catch (IOException ioe) {
                log.error("Error reading CPU times: ", ioe);
            }
        }

        public void halt() {
            shouldUpdate = false;
        }
    }

    /**
     * Component data structure to keep track of information about requested references.
     */
    protected class Component implements Comparable<Component> {
        private String type;
        private String name;
        private int priority;
        private Object reference;
        private boolean status;

        public String getType() {
            log.trace("enter getType()");
            return type;
        }

        public String getName() {
            log.trace("enter getName()");
            return name;
        }

        public void setName(String n) {
            log.trace("enter setName(String n)");
            name = n;
        }

        public int getPriority() {
            log.trace("enter getPriority()");
            return priority;
        }

        public Object getReference() {
            log.trace("enter getReference()");
            return reference;
        }

        public void setReference(Object r) {
            log.trace("enter setReference(Object r)");
            reference = r;
        }

        public boolean getStatus() {
            log.trace("enter getStatus()");
            return status;
        }

        public void setStatus(boolean s) {
            log.trace("enter setStatus(boolean s)");
            status = s;
        }

        @Override
        public String toString() {
            String out = type;
            if (name != null) {
                out += "$" + name;
            }
            return out;
        }

        @Override
        final public int compareTo(Component srv) {
            log.trace("enter compareTo(Component srv)");
            return priority - srv.getPriority();
        }

        public Component(String t, String n, int p) {
            log.trace("enter Component constructor");
            type = t;
            name = n;
            priority = p;
        }
    }

    /**
     * Send an action to a goal manager
     * @param adbe the action database entry to send
     * @param groupID the component group that the receiving Goal Manager component belongs to, e.g. pr2
     * @throws RemoteException
     */
    @Override
    public void sendAction(ActionDBEntry adbe, String groupID) throws RemoteException{
        log.trace("enter sendAction(ActionDBEntry adbe, String groupID)");
        try{
            log.info("[GoalManager::sendAction] Sending Action to another Goal Manager");
            // CC: should GoalManagerPriority, below, just be GoalManager?
            callBlocking(new String[][]{{"type", "com.action.GoalManagerPriority"}, {"group", groupID}}, "receiveAction", adbe);
        } catch (Exception e){
            log.error("one-shot call failure", e);
        }
    }

    @Override
    public void testSendAction() throws RemoteException{
        String filepath = "com/action/db/testMoveTo.xml";
        ActionDBEntry adbe = new ActionDBEntry(filepath,false);
        sendAction(adbe, "pr2");
    }

    /**
     * Receive an action with a goal manager
     * @param adbe the action being received
     * @throws java.rmi.RemoteException
     */
    @Override
    public void receiveAction(ActionDBEntry adbe) throws RemoteException{
        log.trace("enter receiveAction(ActionDBEntry adbe)");
        log.info("[GoalManager::receiveAction] Receiving action: " + adbe.getName());
        populateGlobalFields(adbe);
        log.debug("[GoalManager::receiveAction] Populated Global Fields");
    }

    /**
     * Adds all subtypes of an ADB entry to appropriate
     * static (global) fields and initializes transient fields.
     * @param adbe the action script
     */
    private void populateGlobalFields(ActionDBEntry adbe){
        log.trace("enter populateGlobalFields(ActionDBEntry adbe)");
      Stack<ActionDBEntry> frontier = new Stack();
      frontier.push(adbe);
      ActionDBEntry currEntry;
      while (!frontier.isEmpty()) {
        currEntry = frontier.pop();
        //add currEntry's children
        for (ActionDBEntry subType : adbe.getSubTypes()) {
          frontier.push(subType);
        }

        log.debug("[GoalManager::populateGlobalFields] received new action: " + currEntry.type);
        adb.actionDB.put(currEntry.type, currEntry); // add to action DB

        // add postcondition-adbe pairs to postconds database
        for (Pair<Predicate, ActionDBEntry> tp : currEntry.transferPostconds) {
          log.debug("[GoalManager::populateGlobalFields] adding postcond to DB after transfer: " + tp.car().getName());
          ActionDBEntry.addTransferredPostcondToDB(tp);
        }

        // initialize transient fields to empty
        currEntry.heldLocks = new ArrayList<ActionResourceLock>();
        currEntry.resourceLocks = new ArrayList<ActionResourceLock>();
        currEntry.plannerDomain = new ArrayList<PlannerElement>();
        currEntry.plannerProblem = new ArrayList<PlannerElement>();
      }
    }

    /**
     * Add a server from the list of start-up arguments.  This will look ahead in
     * the argument list to see if a name has also been given for this server.
     * @param type the server type
     * @param args the list of arguments
     * @param i the current position in the argument list
     * @return the resulting position in the argument list
     */
    private int addComponent(String type, String[] args, int i) {
        log.trace("enter addComponent(String type, String[] args, int i)");
        Component s;
        String sname = null;
        boolean name = ! checkNextArg(args, i);
        // check if want specific server by name  (ie: that next
        // argument is NOT another -something)
        if (name) {
            sname = args[++i];
        }

        s = new Component(type, sname, servers.size());
        List<Component> stype = servers.get(type);
        if (stype == null) {
            stype = new ArrayList<Component>();
            servers.put(type, stype);
        }
        stype.add(s);
        return i;
    }

    /**
     * Add a server by type and name.
     * @param type the server type
     * @param name the server name
     * @return the new Component object
     */
    private Component addComponent(String type, String name) {
        log.trace("enter addComponent(String type, String name)");
        Component s;
        // check if want specific server by name  (ie: that next
        // argument is NOT another -something)
        s = new Component(type, name, servers.size());
        List<Component> stype = servers.get(type);
        if (stype == null) {
            stype = new ArrayList<Component>();
            servers.put(type, stype);
        }
        stype.add(s);
        return s;
    }

    /**
     * Constructs the GoalManagerImpl.
     */
    public GoalManagerImpl() throws RemoteException {
        super();
        log.trace("enter GoalManagerImpl constructor");
        u = new Updater(200);
        rangen = new Random();

        /* KRAMER: shouldn't need the thread with the addition of the
         * startExecution() method...
        new Thread() {
        public void run() {
        startUpAction();
        }
        }.start();
         */
        // Calling startUpAction in subclass
        //startUpAction();
    }
}


/*
ACTION MANAGER EXTENSIONS:

-- make pre-conditions and post-conditions work in scripts:

explicit vs implicit post-conditions:

implicit: the action A as a predicate (with past-tense operator in front)
explicit: add logical expression that are made true if the action succeeded

explicit vs implicit pre-conditions:

implicit: the previous script line succeeded
explicit: explicit checks that the world is the way it is supposed to be
(e.g., frame axioms, etc., "pour beer, pickup glass, empty glass in mouth" may
not lead to beer drinking if the beer got spilled during pickup glass)


 ** all explicit pre-conditions need to be checked before a script command is
executed; if one condition is not met, the script command either fails
right away (or via special failure conditions of "precond not met") or
the AI starts problem solving on how to fix it


ADD local knowledge base to scripts:
in addition to having binding lists that dynamically get updated at various
points in the script, we now add facts or predicates that are true or
false or unknown to a database that gets dynamically updated
Note: the knowledge base has to be "local" to each script instance


GENERALIZE ISA-hierarchy on typed variables to allow for directed ISA graphs
can use ISA-hierarchy to do simple inferences for pre- and post-conditions'
based on types alone (checking for pre- and post-conditions needs to take
this hierarchy into account)

-- retain Simbad as a "mental simulator" for spatial worlds in the action
interpreter where objects can be placed, actions can be carried out,
and possible futures can be computed, etc. ...



GOAL MANAGER EXTENSIONS:

-- need to add pre- and post-conditions to goals

ARE THERE ANY PRE/POST CONDITIONS TO GOALS THAT ARE SEPARATE FROM
THOSE FOR SCRIPTS?


goals are post-conditions (j.e., sets of predicates that the agent
wants to be true), so how can we make them true?

==> generate scripts that start from the current state and achieve the
goal states (use temporal goal descriptions and link them to scripts that
will achieve them)





WANT: Through NLP a goal description phi such that we can generate an
associated action script A_phi that will (if all goes well) achieve phi.

Effectively, if the goal completes, we'll have "there was a time phi" true
(and if we look at the actual trace, then we'll have everything in the
knowledge base of the script instantiated by the goal; this will give us
additional facts, as well as more information, e.g., in the case of
disjunctive goals, or goals with existential quantifiers,...).  We also
may be able to derive individual conditions G={C1, C2, ..., Cn} that
obtain if the goal G is satisfied, where phi => G.


NEED: a reasoning mechanism that goes from goal descriptions to action
scripts, where it can chain existing scripts (based on pre- and post-conditions)
and be aware of utilities...  in other words, a utility-based planner at
the task level!
 */
/*

   A consistent scheme for monitors, some of which are associated with
   permanent goals (norms, whatever), and some with transient goals.  These
   will depend on the context, as will costs, etc.  We need a unified treatment
   of monitors and contexts.  Instead of special treatment for transient goal
   monitors (e.g., the one monitoring the exploration task), the robot should
   make its own decision; sometimes that will be wrong (e.g., the robot will
   choose to start a chat in the middle of the task rather than taking
   commands), in which case it will have to be corrected.  Eventually the robot
   will learn the correct priorities (costs/benefits/etc.).  Also need some
   concept of the cost of not pursuing some goal, or of failing to achieve it.

*/
