/**
 * ADE 1.0
 * Copyright 1997-2012 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@cs.tufts.edu
 *
 * ActionDBEntry.java
 *
 * Last update: December 2012
 *
 * @author Paul Schermerhorn
 *
 */

package com.action;

import com.*;
import com.google.common.base.Joiner;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.*;
import java.util.*;

import static java.lang.String.format;
import static utilities.Util.*;

/**
 * An <code>ActionDBEntry</code> is the general-purpose container for one unit
 * of knowledge; can be factual (e.g., "person") or procedural (e.g., "goHome").
 */
public class ActionDBEntry implements Cloneable, Serializable {
    private static Log log = LogFactory.getLog(ActionDBEntry.class);
    protected static final HashMap<String,ActionDBEntry> actionDB = new HashMap<String,ActionDBEntry>();
    // function name returns hashmap of name,value pairs
    protected static HashMap<String,HashMap<String,Object>> functionDB = new HashMap<String,HashMap<String,Object>>();
    protected static HashMap<String,ArrayList<Pair<Predicate,ActionDBEntry>>> postcondDB = new HashMap<String,ArrayList<Pair<Predicate,ActionDBEntry>>>();
    private static HashMap<String,Integer> dictionary = new HashMap<String,Integer>();
    protected static ActionDBParser adbp;
    private static long idCounter = 0;
    private static int funcIndex = 0; // a "gensym" value for new names

    protected ArrayList<Pair<Predicate, ActionDBEntry>> transferPostconds = new ArrayList<Pair<Predicate, ActionDBEntry>>();
    private HashMap<String,ActionBinding> bindingList = new HashMap<String,ActionBinding>();
    private ArrayList<Predicate> goals = new ArrayList<Predicate>();
    private ArrayList<String> execs = new ArrayList<String>();
    private ArrayList<Predicate> badStates = new ArrayList<Predicate>();
    private ArrayList<Predicate> badActions = new ArrayList<Predicate>();
    protected transient ArrayList<PlannerElement> plannerDomain = new ArrayList<PlannerElement>();
    protected transient ArrayList<PlannerElement> plannerProblem = new ArrayList<PlannerElement>();

    protected String type; // External type (for isA, etc.)
    private Object value; // Value stored if fact
    private String description = null; // description...

    // These are for the type hierarchy; more specific/special-purpose scripts
    // are subTypes, whereas the superType is the next level up in generality
    private ActionDBEntry superType; // Pointer to parent type
    protected ArrayList<ActionDBEntry> subTypes = new ArrayList<ActionDBEntry>();


    // Script events are listed here, and the calling script is filled in when
    // this script is called.
    protected ActionDBEntry caller; // Runtime ref to calling script
    private ActionDBEntry event; // Runtime ref to subscript events
    // eventStatus is the result of the most recently executed child event, not me
    private transient boolean eventStatus = true; // Assume child events succeed
    // status is *my* result (assumed to succeed)
    private transient boolean status = true;
    // Script events with args
    private ArrayList<ArrayList<String>> eventSpecs = new ArrayList<ArrayList<String>>();
    // conditions that need to hold at the start of the action
    private ArrayList<Predicate> startConditions = new ArrayList<Predicate>();
    // conditions that need to hold throughout the action
    private ArrayList<Predicate> overAllConditions = new ArrayList<Predicate>();
    // effects that are presumed to hold by virtue of having attempted this action
    private ArrayList<Predicate> effects = new ArrayList<Predicate>();
    // effects that are presumed to hold only if this action succeeds
    private ArrayList<Predicate> successEffects = new ArrayList<Predicate>();
    private int pc; // Program counter (0 <= pc <= eventSpecs.size())
    private Stack<Integer> jumppc = new Stack<Integer>();

    // arguments field is filled in when the action is invoked
    private HashMap<String,ActionBinding> arguments = new HashMap<String,ActionBinding>();
    protected ArrayList<ActionBinding> roles = new ArrayList<ActionBinding>();
    //private ArrayList<String> precond = new ArrayList<String>();
    // PWS: original postcond list, need to unify with effects lists?
    //private ArrayList<Predicate> postcond = new ArrayList<Predicate>();
    protected transient ArrayList<ActionResourceLock> resourceLocks;
    protected transient ArrayList<ActionResourceLock> heldLocks;
    private transient String lockOwnerDescription;

    private double posAff = 0.0;
    private double negAff = 0.0;
    private double cost = 0.0;
    private double benefit = 0.0;
    private long maxTime = -1;
    private long startTime;
    private double minUrg = 0.0;
    private double maxUrg = 1.0;
    protected long id;

    /**
     * Add a new child to supertype; should respect the isA hierarchy
     * @param type the name of this entity
     * @param supertype the entity to which this should be attached
     */
    public ActionDBEntry(String type, ActionDBEntry supertype) {
        this.type = type;
        superType = supertype;
        id = idCounter++;
        resourceLocks = new ArrayList<ActionResourceLock>();
        heldLocks = new ArrayList<ActionResourceLock>();

        // Not sure how to handle this, except not adding it to the DB
        if (superType == null)
            return;

        synchronized(actionDB) {
            actionDB.put(type, this);
        }
        superType.subTypes.add(this);
    }
    /**
     * @author TEW
     */
    public Set<String> getActionTypes(){
        //        synchronized(actionDB){
        //try{
        //                System.out.println("Really getting types...");                
                Set<String> keyset = new HashSet<String>();
                keyset.addAll(actionDB.keySet());
                return keyset;
                //  }catch(Exception e){
                //                e.printStackTrace();
                //                throw e;
                //            }
                //        }
    }


    /**
     * Add a new child to supertype; should respect the isA hierarchy
     * @param type the name of this entity
     * @param supertype the entity to which this should be attached
     */
    public ActionDBEntry(String type, String supertype) {
        this.type = type;
        resourceLocks = new ArrayList<ActionResourceLock>();
        heldLocks = new ArrayList<ActionResourceLock>();

        synchronized(actionDB) {
            superType = actionDB.get(supertype);
        }
        id = idCounter++;

        // Not sure how to handle this, except not adding it to the DB
        if (superType == null)
            return;

        synchronized(actionDB) {
            actionDB.put(type, this);
        }
        superType.subTypes.add(this);
    }

    /**
     * Create a new top-level entity
     * @param type the name of this entity
     */
    /*
    public ActionDBEntry(String type) {
        Type = type;
        id = idCounter++;

        synchronized(actionDB) {
            actionDB.put(type, this);
        }
    }
    */

    /**
     * Construct DB hierarchy from a file
     * @param dbfilename the file containing the DB specification
     */
    protected ActionDBEntry(String dbfilename, boolean printAPI) {
        resourceLocks = new ArrayList<ActionResourceLock>();
        heldLocks = new ArrayList<ActionResourceLock>();

        adbp = new ActionDBParser(dbfilename);
        this.type = adbp.getRootName();
        id = idCounter++;
        //System.out.println("Adding " + Type + " (" + DBKind + ")");

        synchronized(actionDB) {
            actionDB.put(this.type, this);
        }
        adbp.ParseNode(this);
        for (String e:adbp.getExecs())
            execs.add(e);
        for (Predicate e:adbp.getGoals())
            goals.add(e);
        for (Predicate e:adbp.getBadActions()) {
            badActions.add(e);
        }
        for (Predicate e:adbp.getBadStates()) {
            badStates.add(e);
        }
        for (PlannerElement p:adbp.getPlannerDomainElements())
            plannerDomain.add(p);
        for (PlannerElement p:adbp.getPlannerProblemElements())
            plannerProblem.add(p);
        if (printAPI)
            System.out.print(adbp.getAPI());
        log.info("Done processing DB file " + dbfilename);
        log.info("Size of domain: "+plannerDomain.size()+", size of Problem: "+plannerProblem.size());;
    }

    /**
     * Merge another database file into the current DB
     * @param dbfilename the name of the file to be added
     */
    protected void mergeDB(String dbfilename, boolean printAPI) {
        ActionDBParser newadbp = new ActionDBParser(dbfilename);

        newadbp.ParseNode(this);
        for (String e:newadbp.getExecs())
            execs.add(e);
        for (Predicate e:newadbp.getGoals())
            goals.add(e);
        for (Predicate e:newadbp.getBadActions())
            badActions.add(e);
        for (Predicate e:newadbp.getBadStates()) {
            badStates.add(e);
        }
        for (PlannerElement p:newadbp.getPlannerDomainElements())
            plannerDomain.add(p);
        for (PlannerElement p:newadbp.getPlannerProblemElements())
            plannerProblem.add(p);
        if (printAPI)
            System.out.print(newadbp.getAPI());
        log.info(format("Processed DB file %s. Domain size: %d, Problem size: %d", dbfilename, plannerDomain.size(), plannerProblem.size()));
        log.debug(format("DB File %s. Domain: [%s], Problem: [%s], Goals: [%s]", dbfilename,
                Joiner.on(", ").join(plannerDomain), Joiner.on(", ").join(plannerProblem), Joiner.on(", ").join(goals)));
    }

    /**
     * Lookup by type
     * @param type the type of the entity to look up
     * @return the requested entry, if found, null otherwise
     */
    public static ActionDBEntry lookup(String type) {
        ActionDBEntry value = null;
        synchronized(actionDB) {
            value = actionDB.get(type);
        }

        return value;
    }

    /**
     * Lookup by postcondition
     * @param cond the postcondition of the entity to look up
     * @return the requested entry, if found, null otherwise
     */
    protected static boolean checkforPost(Predicate cond) {
        boolean post = false;
        String pname = cond.getName();
        ArrayList<Pair<Predicate,ActionDBEntry>> map = postcondDB.get(pname);

        if (map != null) {
            //log.debug("found "+map.size()+" entries for "+pname+" in postcondDB");
            // at least one predicate with that name exists, check them
            for (Pair<Predicate,ActionDBEntry> e:map) {
                //log.debug("Ready to check: "+e.car());
                if (predicateMatch(cond, e.car())) {
                    post = true;
                    break;
                }
            }
        } else {
            log.warn("found no entries for "+pname+" in postcondDB");
        }
        return post;
    }

    /**
     * Lookup by postcondition
     * @param cond the postcondition of the entity to look up
     * @return the requested entry, if found, null otherwise
     */
    @ActionCodeNote("lookup post needs to be updated when new postconds are implemented")
    protected static ActionDBEntry lookupPost(Predicate cond) {
        String pname = cond.getName();
        ArrayList<Pair<Predicate,ActionDBEntry>> map = postcondDB.get(pname);
        ActionDBEntry entry = null;
        ActionDBEntry tmpentry;
        double utility = -Double.MAX_VALUE;
        double tmputility;

        if (map != null) {
            log.debug("found "+map.size()+" entries for "+pname+" in postcondDB");
            //log.debug("entries: "+map);
            // at least one predicate with that name exists, check them
            for (Pair<Predicate,ActionDBEntry> e:map) {
                boolean badState = false;
                if (predicateMatch(cond, e.car())) {
                    tmpentry = e.cdr();
                    // check for badaction/badstate
                    if (GoalManagerImpl.badAction(tmpentry)) {
                        continue;
                    }
                    ArrayList<Predicate> postconds = tmpentry.getPostconds();
                    for (Predicate post : postconds) {
                        if (GoalManagerImpl.badState(post)) {
                            badState = true;
                            break;
                        }
                    }
                    if (badState) {
                        continue;
                    }
                    tmputility = tmpentry.getBenefit() - tmpentry.getCost();
                    if (tmputility > utility) {
                        utility = tmputility;
                        entry = tmpentry;
                    }
                } else {
                    log.debug(e.car()+" not matching "+cond);
                }
            }
            if (entry == null) {
                // when there's a moral reasoner, step through the actions again, 
                // find the best, send the offending states off to the MR so see 
                // whether it will override the constraint
                log.warn("no permissible action found to achieve "+cond);
            }
        } else {
            log.warn("found no entries for "+pname+" in postcondDB");
        }
        return entry;
    }

    /**
     * Test whether the test Predicate matches the template.  Predicate test matches
     * Predicate template if: they have the same  name; they have the same number of
     * arguments; and each argument of test is test match for the corresponding argument
     * of template, or the corresponding argument of template is untyped (also accepting if test is
     * untyped, until Variable use is more widespread).
     * @param test the test Predicate
     * @param template the template Predicate
     * @return true if test matches template
     */
    protected static boolean predicateMatch(Predicate test, Predicate template) {
        //log.debug("matching "+test+" and "+template);
        // need same names
        if (!test.getName().equalsIgnoreCase(template.getName())) {
            return false;
        }
        // need same number of arguments
        if (test.size() != template.size()) {
            return false;
        }
        // need same argument types -- when specified
        for (int i = 0; i < test.size(); i++) {
            Symbol testi = test.get(i);
            Symbol tempi = template.get(i);

            if (! (tempi instanceof Variable)) {
                //System.out.println("bi ain't a Variable: "+tempi+" of "+template+" vs "+test);
                // untyped template argument, assume it's of type "entity" and
                // the corresponding test argument is an instance of it
                continue;
            }
            if (! (testi instanceof Variable)) {
                //System.out.println("WARNING: argument "+testi+" of "+test+" is not a Variable.");
                /* Don't reject Symbols for now
                if (((Variable)tempi).getType().equals("entity")) {
                    continue;
                }
                return false;
                */
                continue;
            }
            // typed arguments, must match
            if (! variableMatch((Variable)testi, (Variable)tempi)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Test whether the test Variable matches the template.  Variable test matches
     * Variable template if the type of test is an instance of the type of template.
     * @param test the test Predicate
     * @param template the template Predicate
     * @return true if test matches template
     */
    protected static boolean variableMatch(Variable test, Variable template) {
        String testtype; // test type
        String temptype; // template type
        ActionDBEntry testentry; // test db entry
        ActionDBEntry tempentry; // template db entry

        //System.out.println("matching "+test+" and "+template);
        testtype = test.getType();
        temptype = template.getType();
        // get the db entry for these types
        testentry = lookup(testtype);
        tempentry = lookup(temptype);
        if (testentry == null || tempentry == null) {
            // unknown type specified, fall back on string comparison
            if (! testtype.equals(temptype)) {
                return false;
            }
        } else {
            if (! testentry.isA(temptype)) {
                // testtype is not an instance of temptype
                return false;
            }
        }
        if (! template.getName().startsWith("?")) {
            return test.getName().equalsIgnoreCase(template.getName());
        }
        return true;
    }

    /**
     * Insert a function map into the DB.
     * @param n function name
     * @param a function arg(s?)
     * @param v the value mapped to n(a)
     */
    protected void addFunction(String n, String a, String v) {
        HashMap<String,Object> funcvals = functionDB.get(n.toLowerCase());
        if (funcvals == null) {
            //System.out.println("No mapping found for " + n);
            funcvals = new HashMap<String,Object>();
            functionDB.put(n, funcvals);
        }
        funcvals.put(a.toLowerCase(), v.toLowerCase());
    }

    /**
     * Attempt to map function onto literal.  The function db is a hashmap
     * of hashmaps, where the outer HM is indexed by function (Predicate)
     * names, which return the inner HM, which is hashed by the full
     * function signature.
     * @param func the function to look up
     * @return a string literal, possibly newly generated
     */
    protected Object lookupFunction(Predicate func) {
        String arg = "";
        String fn = func.getName();
        ArrayList<Symbol> args = func.getArgs();
        HashMap<String,Object> funcvals = functionDB.get(fn.toLowerCase());
        if (funcvals == null) {
            log.warn("No mapping found for " + fn);
            return (fn + (funcIndex++));
        }
        // This is a little stupid (for now): instead of dealing with
        // variable numbers of args, I construct a single string with all
        // the args and look it up that way.
        for (int i = 0; i < args.size(); i++) {
            if (Predicate.class.isInstance(args.get(i))) {
                arg += lookupFunction((Predicate)args.get(i));
            } else {
                arg += args.get(i).getName().toLowerCase();
            }
            //System.out.println("Lookup adding arg " + i + ": " + arg);
        }
        Object value = funcvals.get(arg.toLowerCase());
        if (value == null) {
            log.warn("No mapping found for " + fn + " " + arg);
            return (fn + (funcIndex++));
        }
        return value;
    }

    /**
     * Add an argument to the list of arguments that were passed when the
     * script was invoked.
     * @param pos the position at which to add the argument
     * @param spec the value to be assigned to the argument
     * @return true if successful, false otherwise
     */
    protected boolean addArgument(int pos, String spec) {
        ActionBinding newArg = (ActionBinding)roles.get(pos).clone(), newBinding;
        //System.out.println("adding argument " + spec + " to " + this.Type + " for " + newArg.bName);
        Object value = null;
        String bname, defStr;
        ActionDBEntry tmp = lookup(newArg.bType);

        if (tmp == null)
            log.error("ERROR: type " + newArg.bType + " undefined.");
        if (spec.charAt(0) == '?' || spec.charAt(0) == '!') {
            // The variable at pos is bound to the variable "name" in the
            // caller's context (if available)
            //newBinding = caller.arguments.get(spec.substring(1).toLowerCase());
            newBinding = caller.arguments.get(spec.toLowerCase());
            if (newBinding != null) {
                value = newBinding.getBinding();
            } else if (tmp.isA("number")) {
                if (tmp.isA("long")) {
                    value = new Long(0);
                } else if (tmp.isA("integer")) {
                    value = new Integer(0);
                } else if (tmp.isA("double")) {
                    value = new Double(0.0);
                }
            } else {
                //value = spec.substring(1);
                value = spec;
                //value = new Variable(spec, newArg.bType);
            }
        } else if (spec.charAt(0) == '!') { // the old way, leaving for now
            if (tmp.isA("number")) {
                if (tmp.isA("long")) {
                    value = new Long(0);
                } else if (tmp.isA("integer")) {
                    value = new Integer(0);
                } else if (tmp.isA("double")) {
                    value = new Double(0.0);
                }
            } else {
                //value = spec.substring(1);
                value = spec;
            }
        } else if (tmp.isA("number")) {
            if (spec.charAt(0) == '$') {
                spec = jsEval(ActionInterpreter.bindText(this,spec.substring(2,spec.length()-1))).toString();
                // leaving the problem, error messages later on will be informative
            }
            try {
                if (tmp.isA("long")) {
                    value = new Long(spec);
                } else if (tmp.isA("integer")) {
                    value = new Integer(spec);
                } else if (tmp.isA("double")) {
                    value = new Double(spec);
                } else {
                    //System.out.println("Found generic number, creating Double");
                    value = new Double(spec);
                }
            } catch (NumberFormatException nfe) {
                value = spec;
            }
        } else if (spec.charAt(0) == '$') {
            spec = jsEval(ActionInterpreter.bindText(this,spec.substring(2,spec.length()-1))).toString();
            value = spec;
        // Other types with special characteristics?
        } else {
            //System.out.println("Didn't find type for: " + spec);
            //System.out.println(tmp);
            value = spec;
            //value = new Constant(spec, newArg.bType);
        }

        bname = ActionDBEntry.genSym(newArg.bName);
        if (newArg.bDefault == null)
            defStr = null;
        else
            defStr = newArg.bDefault.toString();
        if (value == null) {
            //System.out.println("addArgument found no value for " + spec);
            //System.out.println("tmp is: " + tmp);
            newBinding = new ActionBinding(bname, newArg.bType, defStr, newArg.bReturn);
        } else {
            //System.out.println("addArgument found value " + value + " for " + spec);
            newBinding = new ActionBinding(bname, newArg.bType, defStr, value, newArg.bReturn);
        }
        //System.out.println("adding binding for "+newBinding.bName+": "+newBinding);
        bindingList.put(newBinding.bName, newBinding);
        newArg.bind(newBinding);
        arguments.put(newArg.bName, newArg);
        return true;
    }

    /**
     * Specify the (deep) value of an argument
     * @param pos the position at which to add the argument
     * @param val the value to which the argument should be bound
     */
    protected void setArgument(int pos, Object val) {
        ActionBinding newArg = roles.get(pos);

        setArgument(newArg.bName, val);
    }

    /**
     * Specify the (deep) value of an argument
     * @param bname the argument's name
     * @param val the value to which the argument should be bound
     */
    protected void setArgument(String bname, Object val) {
        ActionBinding target = arguments.get(bname.toLowerCase());
        String type;
        ActionDBEntry typeEntry;

	if(bname.equalsIgnoreCase("?hand")) {
          log.debug("setting " + bname + " (" + this.type + ") to " + val);
          log.debug("!! setArgument got target of type: " + target.bType + ", " );
	}

        if (target != null) {
            type = target.getBindingTypeDeep();
            //System.out.println("!! setArgument got target of type: " + target.bType + ", " );
            typeEntry = lookup(type);
            if (typeEntry.isA("double")) {
                if (Double.class.isInstance(val))
                    target.bindDeep(val);
                else
                    target.bindDeep(new Double(val.toString()));
            } else if (typeEntry.isA("long")) {
                if (Double.class.isInstance(val))
                    target.bindDeep(new Long(((Double)val).longValue()));
                else if (Long.class.isInstance(val))
                    target.bindDeep(val);
                else
                    target.bindDeep(new Long(val.toString()));
            } else if (typeEntry.isA("integer")) {
                if (Double.class.isInstance(val))
                    target.bindDeep(new Integer(((Double)val).intValue()));
                else
                    target.bindDeep(new Integer(val.toString()));
            } else {
                target.bindDeep(val);
            }
        } else {
            log.error("ERROR: " + bname + " not in this context");
        }
    }

    /**
     * Inherit the argument list of the calling entity
     */
    protected void inheritArguments() {
        arguments = caller.arguments;
        roles = caller.roles;
    }

    /**
     * Retrieve the (deep) value of an argument
     * @param bname the argument's name
     * @return the deep binding associated with that name in the current
     * context
     */
    protected Object getArgument(String bname) {
        ActionBinding target;
        Object value;
        String type;
        ActionDBEntry typeEntry;

        target = arguments.get(bname.toLowerCase());
        if (target == null) {
            return null;
        }
        value = target.getBindingDeep();
        type  = target.getBindingTypeDeep();
        typeEntry = lookup(type);
        if (typeEntry.isA("double")) {
            if (! Double.class.isInstance(value))
                return new Double(value.toString());
        } else if (typeEntry.isA("long")) {
            if (! Long.class.isInstance(value))
                return new Long(value.toString());
        } else if (typeEntry.isA("integer")) {
            if (! Integer.class.isInstance(value))
                return new Integer(value.toString());
        }
        return value;
    }

    /**
     * Get String argument.
     * @param sname the argument name
     * @return the value
     */
    protected String getString(String sname) {
        ActionBinding target;
        String value;

        target = arguments.get(sname.toLowerCase());
        if (target == null) {
            return null;
        }
        value = (String)target.getBindingDeep();
        return value;
    }

    /**
     * Get Integer argument.
     * @param iname the argument name
     * @return the value
     */
    protected Integer getInteger(String iname) {
        Object value = getArgument(iname);
        if (! Integer.class.isInstance(value))
            return Integer.parseInt(value.toString());
        return (Integer)value;
    }

    /**
     * Get Long argument.
     * @param lname the argument name
     * @return the value
     */
    protected Long getLong(String lname) {
        Object value = getArgument(lname);
        if (! Long.class.isInstance(value))
            return Long.parseLong(value.toString());
        return (Long)value;
    }

    /**
     * Get Double argument.
     * @param dname the argument name
     * @return the value
     */
    protected Double getDouble(String dname) {
        Object value = getArgument(dname);
        if (! Double.class.isInstance(value))
            return Double.parseDouble(value.toString());
        return (Double)value;
    }

    /**
     * Get Boolean argument.
     * @param bname the argument name
     * @return the value
     */
    protected Boolean getBoolean(String bname) {
        Object value = getArgument(bname);
        if (! Boolean.class.isInstance(value))
            return Boolean.parseBoolean(value.toString());
        return (Boolean)value;
    }

    /**
     * List all the action's variables. This is only useful for debugging.
     */
    protected String listVariables() {
        String varlist = "";
        String delim = "";

        for (ActionBinding b : roles) {
            if (b.bName.startsWith("?") || b.bName.startsWith("!")) {
                varlist = varlist + delim + b.bName;
            } else {
                varlist = varlist + delim + "?" + b.bName;
            }
            delim = " ";
        }
        return varlist;
    }

    /**
     * List all the caller's variables. This is only useful for debugging.
     */
    protected String listCallerVariables() {
        String varlist = "";
        String delim = "";

        for (ActionBinding b : caller.roles) {
            if (b.bName.startsWith("?") || b.bName.startsWith("!")) {
                varlist = varlist + delim + b.bName;
            } else {
                varlist = varlist + delim + "?" + b.bName;
            }
            delim = " ";
        }
        return varlist;
    }

    /**
     * List all the action's arguments. This is only useful for debugging.
     */
    protected void listArguments() {
        Collection<ActionBinding> arglist = arguments.values();

        log.debug(this.type + " listing " + arglist.size() + " values");
        log.debug(this.type + " has " + roles.size() + " roles");
        for (ActionBinding b : arglist)
            log.debug(this.type + " argument: " + b);
    }

    /**
     * Retrieve the type of an argument
     * @param bname the argument's name
     * @return the type associated with that name in the current
     * context
     */
    protected String getArgumentType(String bname) {
        ActionBinding target;

        target = arguments.get(bname);
        if (target == null) {
            return null;
        }
        return target.getBindingTypeDeep();
    }

    /**
     * Retrieve the (deep) value of an argument in caller
     * @param bname the argument's name
     * @return the deep binding associated with that name in the current
     * context
     */
    protected Object getCallerArgument(String bname) {
        Object value = caller.getArgument(bname);

        return value;
    }

    /**
     * Get the Type of this action's caller.
     * @return the caller's Type
     */
    protected String getCallerType() {
        String value = caller.getType();

        return value;
    }

    /**
     * Add a description
     * @param desc the description string
     */
    protected void addDescription(String desc) {
        if (this.description == null)
            this.description = desc;
        else
            log.debug(this.type + " has a description: " + this.description);
    }

    /**
     * Get the description
     */
    protected String getDescription() {
        if (this.description == null)
            return null;
        return this.description;
    }

    /**
     * Add a typed role (variable)
     * @param rname the role name
     * @param type the role's type (person, beverage, etc.)
     * @param init an initial value
     * @param ret whether return values will be bound here
     */
    protected void addRole(String rname, String type, Object init, boolean ret) {
        ActionBinding newRole;

        newRole = new ActionBinding(rname, type, null, init, ret);

        roles.add(newRole);
        //arguments.put(rname, newRole);
    }

    /**
     * Add a typed role (variable)
     * @param rname the role name
     * @param type the role's type (person, beverage, etc.)
     * @param def a default value (or null)
     * @param ret whether return values will be bound here
     */
    protected void addRole(String rname, String type, String def, boolean ret) {
        ActionBinding newRole;

        /*
           if (rname.charAt(0) == '!')
           newRole = new ActionBinding(rname.substring(1), type, def);
           else
           newRole = new ActionBinding(rname, type, def);
           */
        newRole = new ActionBinding(rname, type, def, ret);

        roles.add(newRole);
    }

    /**
     * Add a typed role (variable).  This is a space-delimited sequence of
     * "name type" pairs (e.g., "name1 type1 name2 type2 ...").
     * @param rolestring a string of alternating names and types
     */
    public void addRoles(String rolestring) {
        StringTokenizer roleTok = new StringTokenizer(rolestring);
        String rname, type;
        ActionBinding newRole;

        while (roleTok.hasMoreTokens()) {
            rname = roleTok.nextToken();
            type = roleTok.nextToken();
            newRole = new ActionBinding(rname, type, null, false);

            roles.add(newRole);
        }
    }

    /**
     * Get a role's name
     * @param pos the argument position
     * @return that role's name
     */
    protected String getRoleName(int pos) {
        return roles.get(pos).bName;
    }

    /**
     * Get a role's type
     * @param pos the argument position
     * @return that role's type
     */
    protected String getRoleType(int pos) {
        return roles.get(pos).bType;
    }

    /**
     * Get the number of roles for this action.
     * @return the number of roles
     */
    protected int getNumRoles() {
        return roles.size();
    }

    /**
     * add a postcondition received from another GM to the global DB
     * @param p the pair specififying the postcond in predicate form and the scipt as an adbe
     */
    protected static void addTransferredPostcondToDB(Pair<Predicate,ActionDBEntry> p){
        String pname = p.car().getName();
        log.info("Adding transferred postcond to DB: "+ pname);
        ArrayList<Pair<Predicate,ActionDBEntry>> map = postcondDB.get(pname);

        if (map == null) {
            log.debug("never seen this effect before");
            // this effect hasn't been seen before, add it so this action
            // can be looked up by postcondition
            map = new ArrayList<Pair<Predicate,ActionDBEntry>>();
            postcondDB.put(pname, map);
        }
        log.debug("adding to map");
        map.add(p);
    }

    /**
     * Add a postcondition
     * @param post the postcondition to be added
     */
    protected void addPostcondToDB(Predicate post, boolean override) {
        String pname = post.getName();

        ArrayList<Pair<Predicate,ActionDBEntry>> map = postcondDB.get(pname);

        if (map == null) {
            // this effect hasn't been seen before, add it so this action
            // can be looked up by postcondition
            map = new ArrayList<Pair<Predicate,ActionDBEntry>>();
            log.debug("putting postcond into DB: "+pname);
            postcondDB.put(pname, map);
        } else {
            if (! checkforPost(post)) {
                if (override) {
                    // override the existing actions with this effect
                    ArrayList<Integer> rem = new ArrayList<Integer>();
                    // get indices for all matching pairs
                    for (int i = 0; i < map.size(); i++) {
                        if (predicateMatch(post, map.get(i).car())) {
                            rem.add(i);
                        }
                    }
                    // step backward through list of matches and remove them
                    for (int i = rem.size(); i > 0; i--) {
                        int p = rem.get(i-1);
                        map.remove(p);
                    }
                } else {
                    log.warn("Duplicate postcondition, not adding "+post);
                }
            }
        }
        map.add(new Pair<Predicate,ActionDBEntry>(post, this));

    }

    /**
     * Add a condition that must hold for this action to be possible.
     * @param cond the precondition to be added
     */
    protected void addStartCondition(Predicate cond) {
        startConditions.add(cond);
    }

    /**
     * Add a condition that must hold throughout for this action to be possible.
     * @param cond the condition to be added
     */
    protected void addOverAllCondition(Predicate cond) {
        overAllConditions.add(cond);
    }

    /**
     * Add an effect to be sent as a planner update whenever the action is executed.
     * @param post the postcondition to be added
     */
    protected void addEffect(Predicate post) {
        log.debug("addEffect adbe ID: "+id);
        effects.add(post);
    }

    /**
     * Add an effect to be sent as a planner update whenever the action successfully completes.
     * @param post the postcondition to be added
     */
    protected void addSuccessEffect(Predicate post) {
        successEffects.add(post);
    }

    /**
     * Get the preconditions for the current action.
     * @return a list of start and overAll conditions
     */
    protected ArrayList<Predicate> getPreconds() {
        ArrayList<Predicate> conds = new ArrayList<Predicate>(startConditions);
        conds.addAll(overAllConditions);
        return conds;
    }

    /**
     * Get the postconditions for the current action.
     * @return a list of the goals in Predicate form
     */
    protected ArrayList<Predicate> getPostconds() {
        ArrayList<Predicate> post = new ArrayList<Predicate>(effects);
        post.addAll(successEffects);
        return post;
    }

    /**
     * Set the cost
     * @param newCost the cost to be added
     */
    protected void addCost(String newCost) {
        cost = Double.parseDouble(newCost);
    }

    /**
     * Get the cost
     * @return the action's (nominal) cost
     */
    protected double getCost() {
        return cost;
    }

    /**
     * Set the benefit
     * @param newBenefit the benefit to be added
     */
    protected void addBenefit(String newBenefit) {
        benefit = Double.parseDouble(newBenefit);
    }

    /**
     * Get the benefit
     * @return the action's (nominal) benefit
     */
    protected double getBenefit() {
        return benefit;
    }

    /**
     * Add affect state
     * @param newAffect the new affect state
     * @param positive boolean indicating whether to add positive affect
     */
    protected void addAffect(String newAffect, boolean positive) {
        if (positive)
            posAff = Double.parseDouble(newAffect);
        else
            negAff = Double.parseDouble(newAffect);
    }

    /**
     * Update parent's affect state
     * @param inc the increment value for the update
     * @param positive boolean indicating whether to add positive affect
     */
    protected void updateParentAffect(double inc, boolean positive) {
        // Update affect states for the action prototype, not the instance
        ActionDBEntry.lookup(caller.type).updateAffect(inc, positive);
    }

    /**
     * Update affect state
     * @param inc the increment value for the update
     * @param positive boolean indicating whether to add positive affect
     */
    protected void updateAffect(double inc, boolean positive) {
        if (positive) {
            posAff += (1 - posAff) * inc;
            //log.debug("Action " + Type + " posAff: " + posAff);
        } else {
            negAff += (1 - negAff) * inc;
            //log.debug("Action " + Type + " negAff: " + negAff);
        }
    }

    /**
     * Get affect state
     * @param positive boolean indicating whether to add positive affect
     * @return the requested affect state value
     */
    protected double getAffect(boolean positive) {
        if (positive)
            return posAff;
        return negAff;
    }

    /**
     * Get affective evaluation of action
     * @return the evaluation
     */
    protected double affectEval() {
        //double a = 0.5 + (posAff*posAff - negAff*negAff) * 0.5;
        double a = 1 + posAff*posAff - negAff* negAff;

        return a;
    }

    /**
     * Set the maxUrg
     * @param newMaxUrg the maxUrg to be added
     */
    protected void addMaxUrg(String newMaxUrg) {
        maxUrg = Double.parseDouble(newMaxUrg);
    }

    /**
     * Get the maxUrg
     * @return the action's maxUrg
     */
    protected double getMaxUrg() {
        return maxUrg;
    }

    /**
     * Set the minUrg
     * @param newMinUrg the minUrg to be added
     */
    protected void addMinUrg(String newMinUrg) {
        minUrg = Double.parseDouble(newMinUrg);
    }

    /**
     * Get the minUrg
     * @return the action's minUrg
     */
    protected double getMinUrg() {
        return minUrg;
    }

    /**
     * Set the timeout
     * @param newTimeout the timeout to be added
     */
    protected void addTimeout(String newTimeout) {
        maxTime = Long.parseLong(newTimeout) * 1000;
    }

    /**
     * Set the timeout
     * @param newTimeout the timeout to be added
     */
    protected void addTimeout(Long newTimeout) {
        maxTime = newTimeout * 1000;
    }

    /**
     * Get the timeout
     * @return the action's (nominal) timeout
     */
    protected long getTimeout() {
        return maxTime;
    }

    /**
     * Get the start time
     * @return the action's start time
     */
    protected long getStartTime() {
        return startTime;
    }

    /**
     * Set the start time
     */
    protected void setStartTime(long stime) {
        startTime = stime;
    }

    /**
     * Get the elapsed time
     * @return the action's elapsed time
     */
    protected long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Add predicate, boolean pair to postconditions
     * (for learning new action from other GM)
     * @param p predicate to add
     */

    protected void addPostcond(Predicate p){
        this.transferPostconds.add(new Pair<Predicate, ActionDBEntry>(p, this));
    }

    /**
     * get the subtypes of this adbe
     * @return the subtypes
     */
    protected ArrayList<ActionDBEntry> getSubTypes(){
        return this.subTypes;
    }

    /**
     * Add locks
     * @param locks a list of locks to acquire before executing
     */
    protected void addLocks(String locks) {
        StringTokenizer lockTok = new StringTokenizer(locks);
        String lockName;
        ActionResourceLock lock = null;

        while (lockTok.hasMoreTokens()) {
            lockName = lockTok.nextToken();
            lock = GoalManagerImpl.getLock(lockName);
            if (lock != null)
                resourceLocks.add(lock);
            else
                log.warn("Unrecognized resource lock: " + lockName);
        }
    }

    boolean acquireLock(String lockName, ActionInterpreter actionInt) {
        ActionResourceLock lock = null;
        lock = GoalManagerImpl.getLock(lockName);
        if (lock != null) {
            if (lock.nonBlockingAcquire(actionInt)) {
                heldLocks.add(lock);
                // postpone any active actions?
                return true;
            } else {
                log.warn("Failed to acquire resource lock: " + lockName);
            }
        } else {
            log.warn("Unrecognized resource lock: " + lockName);
        }
        return false;
    }

    void releaseLock(String lockName, ActionInterpreter actionInt) {
        ActionResourceLock lock = null;
        lock = GoalManagerImpl.getLock(lockName);
        if (lock != null) {
            lock.release(actionInt);
        } else {
            log.warn("Unrecognized resource lock: " + lockName);
        }
    }

    /**
     * Acquire all locks registered for this action
     * @param actionInt The AI that needs the lock
     * @return true is all locks acquired, false otherwise
     */
    boolean acquireLocks(ActionInterpreter actionInt) {
        ActionInterpreter owner;
        log.trace(format("%s :: %s [%s] heldLocks (before acquire):", actionInt.cmd, type, id, heldLocks));
        if (resourceLocks == null) {
            log.warn("resourceLocks was null. Should this happen? Initializing for now...");
            resourceLocks = new ArrayList<ActionResourceLock>();
        }
        log.trace(format("Resource locks: Null? %s, %s", resourceLocks == null ? "true" : "false" ,resourceLocks));
        for (ActionResourceLock lock : resourceLocks) {
            //if (! lock.blockingAcquire(actionInt, primitive))
            //System.out.println(actionInt.cmd + " acquiring resource lock: " + lock);
            if (! lock.nonBlockingAcquire(actionInt)) {
                owner = lock.getOwner();
                if (owner != null) {
                    lockOwnerDescription = owner.cmd;
                } else {
                    lockOwnerDescription = "unknown";
                }
                //System.out.println(actionInt.cmd + "::" + type + "[" + id + "] heldLocks (fail " + lock + "): " + heldLocks);
                return false;
            }
            //System.out.println(actionInt.cmd + "::" + type + "[" + id + "] adding " + lock);
            heldLocks.add(lock);
        }
        //System.out.println(actionInt.cmd + "::" + type + "[" + id + "] heldLocks (after acquire): " + heldLocks);
        return true;
    }

    /**
     * Release all locks registered for this action
     * @param actionInt The AI that needs the lock
     */
    void releaseLocks(ActionInterpreter actionInt) {
        if (heldLocks == null) {
            log.warn("heldLocks is null. This should not be the case. Initializing and returning for now.");
            heldLocks = new ArrayList<ActionResourceLock>();
            return;
        }
        for (ActionResourceLock lock : heldLocks) {
            lock.release(actionInt);
        }
        heldLocks.clear();
    }

    /**
     * Get the description of the owner of a resource lock.
     * @return the description
     */
    protected String getLockOwnerDescription() {
        return lockOwnerDescription;
    }

    /**
     * Try to resolve the parameters of a script invocation.  Check what's
     * passed in, and the calling environment, etc.
     * @param act the parent action
     * @param spec the script invocation (task specification)
     * @return a binding list
     */
    protected ArrayList<String> resolveParams(ActionDBEntry act,
            ArrayList<String> spec) {
        String type, param;
        String bName, tmpName;
        ArrayList<String> newSpec = new ArrayList<String>();
        ActionDBEntry typeEntry;

        //System.out.println("Filling in params for " + this.type +
        //	" (called by " + act.type + ")");
        newSpec.add(spec.get(0));
        for (int i = 0, j = 1; i < roles.size(); i++, j++) {
            bName = null;
            tmpName = null;
            param = null;
            // Variable names starting with '!' in script definitions are local
            // variables that should not be filled in using variables in the
            // enclosing context, as that would overwrite those variables.
            if (roles.get(i).bLocal) {
                continue;
            }
            type = roles.get(i).bType;
            if (spec.size() > j)
                param = spec.get(j);
            typeEntry = lookup(type);
            if ((param == null) || (param.charAt(0) == '?')) {
                // If it matches one of the existing variables, use that
                if ((param != null) && (param.charAt(0) == '?')) {
                    //tmpName = param.substring(1);
                    tmpName = param;
                    for (ActionBinding b : act.roles) {
                        if (b.bName.equalsIgnoreCase(tmpName)) {
                            bName = b.bName;
                            break;
                        } else { // PWS: Should check for error here...
                        }
                    }
                }
                if (typeEntry == null)
                    log.warn("WARNING: did not find type " + type);
                // Look for something of this type in this context
                if (bName == null) {
                    for (int k = 1; k <= getDepth(); k++) {
                        for (ActionBinding b : act.roles) {
                            if ((b.bName.charAt(0) != '!') &&
                                    typeEntry.isA(b.bType, k)) {
                                bName = b.bName;
                                // PWS: A stupid way to break from both loops
                                k = getDepth() + 1;
                                break;
                            } else { // PWS: Should check for error here...
                            }
                        }
                    }
                }
                // Look for something of this type in parent context
                if (bName == null) {
                    for (int k = 1; k <= getDepth(); k++) {
                        for (ActionBinding b : roles) {
                            if ((b.bName.charAt(0) != '!') &&
                                    typeEntry.isA(b.bType, k)) {
                                bName = b.bName;
                                // PWS: A stupid way to break from both loops
                                k = getDepth() + 1;
                                break;
                            }
                        }
                    }
                }
                if (bName == null) {
                    bName = param;
                }
                //newSpec.add("?" + bName);
                newSpec.add(bName);
            } else {
                newSpec.add(param);
            }
            //System.out.println("Added parameter " + newSpec.get(j));
        }
        return newSpec;
    }

    /**
     * Instantiate the event specified in the event specification
     * @param eSpec the event to be executed
     */
    protected ActionDBEntry addEvent(ArrayList<String> eSpec) {
        // if(eSpec.get(0).equals("getWirelessStatus"))
        //     System.out.println("adding event: "+eSpec);
        log.trace(format("[addEvent] Adding event according to specification: %s", eSpec));
        String command = eSpec.get(0).toLowerCase(), bname;
        ActionDBEntry newEvent = ActionDBEntry.lookup(command);

        if (newEvent == null) {
            log.error("[addEvent] Error adding task " + eSpec.get(0));
            return null;
        }
        //System.out.println("addEvent adding: " + eSpec);

        // Clone a copy of the task prototype
        long remainingTime;

        newEvent = (ActionDBEntry)newEvent.clone();
        newEvent.caller = this;
        newEvent.bindingList = new HashMap<String,ActionBinding>(bindingList);
        // get timeouts right for autonomy
        if (newEvent.maxTime < 0) {
            newEvent.maxTime = maxTime;
            newEvent.startTime = startTime;
        } else {
            newEvent.startTime = System.currentTimeMillis();

            // Limit the timeout, if necessary
            if (this != GoalManagerImpl.adb) {
                remainingTime = maxTime - (newEvent.startTime - startTime);
                if (newEvent.maxTime > remainingTime)
                    newEvent.maxTime = remainingTime;
            }
        }
        // Now need to go through and bind the values in taskspec to
        // variables: step through roles and arguments, interpreting each
        // element according to its kind in roles
        for (int i = 1, j = 0; j < newEvent.roles.size(); i++, j++) {
            if (i < eSpec.size()) {
                newEvent.addArgument(j, eSpec.get(i));
            } else {
                //newEvent.addArgument(j, "?"+newEvent.roles.get(j).bName);
                newEvent.addArgument(j, newEvent.roles.get(j).bName);
            }
        }
        if (newEvent.arguments.values().size() != newEvent.roles.size()) {
            log.error("ERROR: event signature mismatch:");
            System.out.print("    " + eSpec.get(0) + " expects: ");
            for (ActionBinding b : newEvent.roles)
                System.out.print(" " + b.bName);
            System.out.print(",\n    but got: ");
            for (int i = 1; i < eSpec.size(); i++)
                System.out.print(" " + eSpec.get(i));
            log.debug(" instead");
        }

        event = newEvent;
        return newEvent;
    }

    /**
     * Add a new script event specification to a script
     * @param eventSpec the event to be added to the (ordered) eventSpecs list
     * (an event specification includes the name of the action to be executed
     * and its parameters)
     */
    protected void addEventSpec(ArrayList<String> eventSpec) {
        eventSpecs.add(eventSpec);
    }

    /**
     * Add a new script event specification to a script
     * @param eventSpec the event to be added to the (ordered) eventSpecs list
     * (an event specification includes the name of the action to be executed
     * and its parameters)
     */
    public void addEventSpec(String eventSpec) {
        ArrayList<String> newEventSpec = new ArrayList<String>();
        StringTokenizer ESTok = new StringTokenizer(eventSpec);
        String token, subToken;

        while (ESTok.hasMoreTokens()) {
            token = ESTok.nextToken();
            if (token.startsWith("\"")) {
                if (! token.endsWith("\"")) {
                    //token = token.substring(1);
                    while (ESTok.hasMoreTokens()) {
                        subToken = ESTok.nextToken();
                        if (subToken.endsWith("\"")) {
                            //subToken = subToken.substring(0, subToken.length() - 1);
                            token = token.concat(" " + subToken);
                            break;
                        } else {
                            token = token.concat(" " + subToken);
                        }
                    }
                }
            } else if (token.startsWith("$")) {
                if (! token.endsWith("}")) {
                    //token = token.substring(1);
                    while (ESTok.hasMoreTokens()) {
                        subToken = ESTok.nextToken();
                        token = token.concat(" "+subToken);
                        if (subToken.endsWith("}")) {
                            break;
                        }
                    }
                }
            }
            newEventSpec.add(token);
        }
        eventSpecs.add(newEventSpec);
    }

    /**
     * Get the next script event specification from a script that matches
     * @param enames a list of event names to search for (e.g., for else,
     * elseif)
     * @param start the opening name of a potentially nested statement (e.g.,
     * if, while)
     * @param end the closing name of a potentially nested statement (e.g.,
     * endif, endwhile)
     * @return the spec of the next event, or null if there is none (an event
     * specification includes the name of the action to be executed and its
     * parameters)
     */
    protected ArrayList<String> getEventSpec(String enames, String start,
            String end) {
        ArrayList<String> nextEvent;
        ArrayList<String> eNames = new ArrayList<String>();
        StringTokenizer eNameTok = new StringTokenizer(enames);

        while (eNameTok.hasMoreTokens()) {
            eNames.add(eNameTok.nextToken());
        }

        while (pc < eventSpecs.size()) {
            nextEvent = eventSpecs.get(pc++);
            // Skip start-end sequences (e.g., everything between IF and
            // ENDIF)
            if (nextEvent.get(0).equals(start)) {
                nextEvent = getEventSpec(end, start, end);
            } else {
                for (String ename : eNames) {
                    if (nextEvent.get(0).equals(ename)) {
                        return nextEvent;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Get the next script event specification from a script that matches
     * @param enames a list of event names to search for (e.g., for else,
     * elseif)
     * @return the spec of the next event, or null if there is none (an event
     * specification includes the name of the action to be executed and its
     * parameters)
     */
    protected ArrayList<String> getEventSpec(String enames) {
        ArrayList<String> nextEvent;
        ArrayList<String> eNames = new ArrayList<String>();
        StringTokenizer eNameTok = new StringTokenizer(enames);

        while (eNameTok.hasMoreTokens()) {
            eNames.add(eNameTok.nextToken());
        }

        while (pc < eventSpecs.size()) {
            nextEvent = eventSpecs.get(pc++);
            for (String ename : eNames) {
                if (nextEvent.get(0).equals(ename)) {
                    return nextEvent;
                }
            }
        }
        return null;
    }

    /**
     * Get the next script event specification from a script
     * @return the spec of the next event, or null if there is none (an event
     * specification includes the name of the action to be executed and its
     * parameters)
     */
    protected ArrayList<String> getEventSpec() {
        if (pc < eventSpecs.size()) {
            return eventSpecs.get(pc++);
        } else {
            return null;
        }
    }

    /**
     * Back up the pc one step
     */
    protected void ungetEventSpec() {
        pc--;
    }

    /**
     * Set the step to which the script should jump (e.g., from the bottom
     * of a while loop, the jumppc is the top of the loop).
     */
    protected void setJumpPC() {
        //log.debug("Setting jump pc to " + (pc - 1));
        jumppc.push(pc - 1);
    }

    /**
     * Restore the jumppc for any enclosing loop/conditional/etc.
     */
    protected void restoreJumpPC() {
        pc = jumppc.pop();
        //log.debug("Restoring pc to " + pc);
    }

    /**
     * Get the current script's program counter.
     * @return the pc
     */
    protected int getPC() {
        return pc;
    }

    /**
     * Discard the program counter for the curent script (e.g., when popping
     * off the stack to the parent script).
     */
    protected void discardJumpPC() {
        //log.debug("Discarding jump pc");
        jumppc.pop();
    }

    /**
     * Assert a fact in the DB
     * @param fname the name of the fact to be asserted
     * @param value a value to be associated with the fact
     * @return true if success
     */
    protected static boolean assertFact(String fname, Object value) {
        log.debug("Asserting fact");
        fname = fname.toLowerCase();
        ActionDBEntry fact = lookup(fname);

        if (fact == null) {
            fact = new ActionDBEntry(fname, "fact");
        }

        fact.value = value;
        return true;
    }

    /**
     * Assert a fact in the DB
     * @param fname the name of the fact to be asserted
     * @return true if success
     */
    protected static boolean assertFact(String fname) {
        log.debug("Asserting fact");
        fname = fname.toLowerCase();
        ActionDBEntry fact = lookup(fname);

        if (fact == null) {
            //log.debug("Asserting fact: " + fname);
            fact = new ActionDBEntry(fname, "fact");
        } else {
            return false;
        }
        return true;
    }

    /**
     * Retract an assertion from the DB
     * @param fname the name of the fact to be retracted
     * @return true if success
     */
    protected static boolean retractFact(String fname) {
        fname = fname.toLowerCase();
        ActionDBEntry fact = lookup(fname);
        if (fact == null) {
            //System.out.println("Fact not found: " + fname);
            return false;
        }
        if (fact.isA("fact")) {
            //System.out.println("Removing fact: " + fname);
            synchronized(actionDB) {
                actionDB.remove(fname);
            }
        }
        return true;
    }

    /**
     * Recursive check whether this can be categorized as a <type>
     * @param type the name of the type
     * @return true if it is, false if it isn't
     */
    protected boolean isA(String type) {
        if (this.type.equalsIgnoreCase(type))
            return(true);
        else if (superType != null)
            return(superType.isA(type));
        else
            return(false);
    }

    /**
     * Recursive check whether this can be categorized as a <type>, limited to
     * <levels> levels of recursion
     * @param type the name of the type
     * @param levels the number of levels to try
     * @return true if it is, false if it isn't
     */
    protected boolean isA(String type, int levels) {
        if (levels == 0)
            return false;
        else if (this.type.equalsIgnoreCase(type))
            return true;
        else if (superType != null)
            return(superType.isA(type, levels - 1));
        else
            return false;
    }

    /**
     * Find the depth at which the entry resides in the DB tree
     * @return the level, 0 if it's not found
     */
    protected int getDepth() {
        int depth = 0;
        ActionDBEntry ancestor = this;

        while (ancestor.superType != null) {
            depth++;
            ancestor = ancestor.superType;
        }
        return depth;
    }

    /**
     * Get the type of this entry
     * @return the type
     */
    public String getType() {
        return this.type;
    }

    /**
     * Get the super type of this entry
     * @return the type
     */
    public String getSupertype() {
        if (superType == null) {
            return null;
        }
        return superType.getType();
    }

    /**
     * Get the name of this entry (same as type)
     * @return the name
     */
    public String getName() {
        return this.type;
    }

    /**
     * Get the value of this fact
     * @return the value
     */
    public Object getValue() {
        return(this.value);
    }

    /**
     * Get the exit status for most recent event.  Note that this is intended
     * to be invoked at the event's caller's level, not the event's level;
     * eventStatus is always the exit status of my child event, not me.
     * @return the exit status
     */
    protected boolean getEventExitStatus() {
        return eventStatus;
    }

    /**
     * Get the exit status for this event.  Note that this is intended
     * to be invoked at the event's level, not the caller's level;
     * status is always *my* exit status.
     * @return the exit status
     */
    protected boolean getExitStatus() {
        return status;
    }

    /**
     * Set the exit status for current event.  Note that this is always called
     * from the child event, setting its exit status before it returns.
     * @param eValue the exit status of the currently running event
     */
    protected void setExitStatus(boolean eValue) {
        //System.out.println("Setting event status of " + caller.Type + " for " + Type + " to " + eValue);
        caller.eventStatus = eValue;
        //System.out.println("Setting exit status of " + Type + " to " + eValue);
        status = eValue;
    }

    /**
     * Called when the action completes, adds the (presumed) effects to parent's eventEffects
     */
    protected ArrayList<Predicate> updateEffects() {
        ArrayList<Predicate> update = new ArrayList<Predicate>();
        if (status) {
            // bind successEffects and add to caller eventEffects
            for (Predicate e:successEffects) {
                Predicate b = bindPredicate(e);
                log.debug("updateEffects for " + this.type + " returning predicate: "+b);
                update.add(b);
            }
        }
        // bind effects and add to caller eventEffects
        for (Predicate e:effects) {
            Predicate b = bindPredicate(e);
            log.debug("updateEffects for " + this.type + " returning predicate: "+b);
            update.add(b);
        }
        return update;
    }

    protected Predicate bindPredicate(Predicate p) {
        String pText = p.toString();
        pText = ActionInterpreter.bindText(this, pText);
        pText = pText.replaceAll(" - ", ":");
        Predicate b = createPredicate(pText);
        //System.out.println("bindPredicate in: "+p+", inter: "+pText+", out: "+b);
        return b;
    }

    /**
     * Exit the current event.  Note that this is always called
     * from the child event, setting its exit status before it returns.
     * @param eValue the exit status of the currently running event
     */
    protected void exit(boolean eValue) {
        caller.eventStatus = eValue;
        caller.status = eValue; // not sure why this is necessary...
        status = eValue;
        // set my PC past the end of my event list
        pc = eventSpecs.size() + 1;
        // same with my caller's PC (...)
        caller.pc = caller.eventSpecs.size() + 1;
        if (caller.isA("syntax")) {
            caller.exit(eValue);
        }
    }

    /**
     * Get list of goals found in DB file.
     * @return the list of goals
     */
    protected ArrayList<Predicate> getGoals() {
        return goals;
    }

    /**
     * Get list of script invocations found in DB file.
     * @return the list of taskspecs
     */
    protected ArrayList<String> getExecs() {
        return execs;
    }

    /**
     * Get list of forbidden states found in DB file.
     * @return the list of forbidden states
     */
    protected ArrayList<Predicate> getBadStates() {
        return badStates;
    }

    /**
     * Get list of forbidden actions found in DB file.
     * @return the list of forbidden actions
     */
    protected ArrayList<Predicate> getBadActions() {
        return badActions;
    }

    /**
     * Add a domain element to the list.
     * @param p the new domain element
     */
    void addPlannerDomainElement(PlannerElement p) {
        plannerDomain.add(p);
    }

    /**
     * Get list of domain elements to be sent to the planner
     * @return the list of plannerDomain elements
     */
    ArrayList<PlannerElement> getPlannerDomainElements() {
        return plannerDomain;
    }

    /**
     * Get list of problem elements to be sent to the planner
     * @return the list of plannerProblem elements
     */
    ArrayList<PlannerElement> getPlannerProblemElements() {
        return plannerProblem;
    }

    @Override
    public String toString() {

        String printname = this.type;
        //if (Type == null){ printname = ""; }

        if (superType == null) {
            printname = printname.concat(" ()");
        } else {
            printname = printname.concat(" (" + superType.type + ")");
        }
        for (ActionDBEntry a : subTypes) {
            printname = printname.concat("\n subType: " + a.type);
        }
        for (ArrayList<String> a : eventSpecs) {
            printname = printname.concat("\n eventSpec: " + a);
        }
        for (ActionBinding b : roles) {
            log.trace("Binding: " + b);
            printname = printname.concat("\n role: " + b.bName + " - " + b.bType);
        }
        return printname;
    }

    /**
     * Print the current database element, with indentation.
     */
    protected void printDB() {
        printDB("");
    }

    private void printDB(String indent) {
        if (superType == null) {
            log.debug(this.type + " ()");
        } else {
            log.debug(this.type + " (" + superType.type + ")");
        }
        indent = indent + " ";
        for (ActionDBEntry a : subTypes) {
            System.out.print(indent + "subType: ");
            a.printDB(indent);
        }
        for (ArrayList<String> a : eventSpecs) {
            log.debug(indent + "eventSpec: " + a);
        }
        for (ActionBinding b : roles) {
            log.debug(indent + "role: " + b.bName);
        }
    }

    @Override
    public Object clone() {
        try {
            ActionDBEntry cloned = (ActionDBEntry)super.clone();

            cloned.eventSpecs = new ArrayList<ArrayList<String>>(eventSpecs);
            cloned.jumppc = new Stack<Integer>();
            cloned.arguments = new HashMap<String,ActionBinding>();
            cloned.heldLocks = new ArrayList<ActionResourceLock>();

            return cloned;
        } catch (CloneNotSupportedException cnse) {
            return null;
        }
    }

    /**@author TEW*/
    public ArrayList<ArrayList<String>> getEventSpecs(){
        return new ArrayList<ArrayList<String>>(eventSpecs);
    }

    /**
     * Generate a new symbol (String) based on the provided prefix
     * @param prefix the new symbol's base name
     * @return the newly generated symbol
     */
    protected static String genSym(String prefix) {
        Integer index = dictionary.remove(prefix);
        String newSym;

        if (index == null) {
            index = new Integer(0);
        }
        newSym = prefix + (index.intValue() + 1);
        dictionary.put(prefix, new Integer(index.intValue() + 1));
        return newSym;
    }

    protected Predicate getPredDesc() {
        Predicate desc;
        ArrayList<Symbol> args = new ArrayList<Symbol>();
        for (ActionBinding b:roles) {
            String bname = b.bName;
            String btype = b.getBindingTypeDeep();
            //log.debug("getPredDesc "+Type+": looking at role "+b);
            ActionBinding arg = arguments.get(bname);
            if (arg == null) {
                arg = b;
            }
            Object v = arg.getBindingDeep();
            if (v instanceof Variable) {
                args.add((Symbol)v);
            } else {
                if (v != null) {
                    bname = v.toString();
                }
                if (bname.startsWith("?") || bname.startsWith("!")) {
                    args.add(new Variable(bname, btype));
                } else {
                    args.add(new Constant(bname, btype));
                }
            }
        }
        desc = new Predicate(this.type, args);
        return desc;
    }
}
// vi:ai:smarttab:expandtab:ts=8 sw=4
