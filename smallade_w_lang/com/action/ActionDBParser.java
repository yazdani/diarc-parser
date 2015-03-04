/**
 * ADE 1.0 
 * Copyright 1997-2012 HRILab (http://hrilab.org/) 
 * 
 * All rights reserved.  Do not copy and use without permission. 
 * For questions contact Matthias Scheutz at mscheutz@cs.tufts.edu
 *
 * ActionDBParser.java
 *
 * Last update: December 2012
 *
 * @author Paul Schermerhorn
 *
 */

package com.action;

import com.*;

import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory;  
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.xml.sax.SAXException;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.io.*;
import java.security.*;
import java.security.spec.*;
import java.util.*;

import org.w3c.dom.*;

import static utilities.Util.*;

/**
 * An <code>ActionDBParser</code> will parse the xml database files containing
 * scripts and other knowledge.
 */
final class ActionDBParser{
    private static Log log = LogFactory.getLog(ActionDBParser.class);

    private DocumentBuilderFactory dbf;
    private DocumentBuilder db;
    private Document dbdoc; 
    private Element dbroot;
    private boolean verbose = false;
    private boolean printActions = false;
    private boolean printTypes = false;
    private StringBuilder currentAction = null;
    private ArrayList<Predicate> goals = new ArrayList<Predicate>();
    private ArrayList<String> execs = new ArrayList<String>();
    private ArrayList<Predicate> badStates = new ArrayList<Predicate>();
    private ArrayList<Predicate> badActions = new ArrayList<Predicate>();
    private ArrayList<PlannerElement> plannerDomain = new ArrayList<PlannerElement>();
    private ArrayList<PlannerElement> plannerProblem = new ArrayList<PlannerElement>();
    private PlannerElement plannerType = null;
    private PlannerElement plannerAction = null;
    private PlannerElement plannerPredicate = null;
    private PlannerElement plannerConstant = null;
    private PlannerElement plannerState = null;
    private PlannerElement plannerGoal = null;
    private PlannerElement plannerOpen = null;
    private ArrayList<APIElement> api = new ArrayList<APIElement>();
    private APIElement apie = null;
    private static HashMap<String,Variable> varmap = new HashMap<String,Variable>();
    private static HashMap<String,Constant> constmap = new HashMap<String,Constant>();
    private long nodeTransferID;

    /**
     * Create a new parser and parse the file.  This is primarily for the use
     * of the <code>main</code> method.
     * @param filename the database file to be parsed
     * @param run this is ignored; it's only here to generate a distinct
     * method signature.
     */
    public ActionDBParser(String filename, boolean run) {
        this(filename);

        verbose = true;
        ActionDBEntry newNode = ParseNode(dbroot, null, "");
        ParsePrint("Root of new DB tree: " + newNode.getType());
    }

    /**
     * Create a new parser for the database file.
     * @param filename the database file to be parsed
     */
    public ActionDBParser(String filename) { // ActionDBEntry base
        this();

        String oldfile = filename;
        boolean parsed;
        if (filename.indexOf(".xml") > 0)
            parsed = parsePlain(filename);
        else
            parsed = parseCrypt(filename);
        if (parsed)
            dbroot = dbdoc.getDocumentElement();
        else
            System.err.println("---Didn't find " + oldfile);
    }

    public boolean parsePlain(String filename) {
        File file = new File(filename);
        InputStream is = null;
        String newfile;
        if (! file.exists()) {
            //log.debug("Trying " + filename);
            while ((is = getClass().getResourceAsStream(filename)) == null) {
                newfile = filename;
                filename = filename.substring(filename.indexOf('/') + 1);
                if (filename.equals(newfile)) {
                    return false;
                }
                //log.debug("Trying " + filename);
            }
        }
        try {
            if (file.exists())
                dbdoc = db.parse(file);
            else
                dbdoc = db.parse(is);
        } catch (SAXException sxe) {
            ParsePrint(sxe.getMessage());
            sxe.printStackTrace();
            return false;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean parseCrypt(String filename) {
        File file = new File(filename);
        InputStream is = null;
        String newfile;
        char[] kbuf = {(char)0x21,(char)0x65,(char)0x72,(char)0x6f,(char)0x6d,
            (char)0x6f,(char)0x6e,(char)0x73,(char)0x69,(char)0x62,(char)0x61,
            (char)0x6c,(char)0x6f,(char)0x72,(char)0x69,(char)0x61};
        String kstr = new String(kbuf);
        String alg = "PBEWithMD5AndDES";
        int b = 0;

        PBEKeySpec keySpec = new PBEKeySpec(kstr.toCharArray());
        SecretKeyFactory keyFactory = null;
        try {
            keyFactory = SecretKeyFactory.getInstance(alg);
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("nsae: " + nsae);
            return false;
        }
        SecretKey key = null;
        try {
            key = keyFactory.generateSecret(keySpec);
        } catch (InvalidKeySpecException ikse) {
            System.err.println("ikse: " + ikse);
            return false;
        }
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("nsae: " + nsae);
            return false;
        }
        md.update("input".getBytes());
        byte[] dig = md.digest();
        byte[] buf = new byte[8];
        System.arraycopy(dig, 0, buf, 0, 8);
        PBEParameterSpec paramSpec = new PBEParameterSpec(buf, 20);
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance(alg);
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("nsae: " + nsae);
            return false;
        } catch (NoSuchPaddingException nspe) {
            System.err.println("nspe: " + nspe);
            return false;
        }
        try {
            cipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
        } catch (InvalidKeyException ike) {
            System.err.println("ike: " + ike);
            return false;
        } catch (InvalidAlgorithmParameterException iape) {
            System.err.println("iape: " + iape);
            return false;
        }
        if (file.exists()) {
            try {
                is = new FileInputStream(filename);
            } catch (FileNotFoundException fnfe) {
                System.err.println("fnfe: " + fnfe);
                return false;
            }
        } else {
            log.debug("Trying " + filename);
            while ((is = getClass().getResourceAsStream(filename)) == null) {
                newfile = filename;
                filename = filename.substring(filename.indexOf('/') + 1);
                if (filename.equals(newfile)) {
                    return false;
                }
                log.debug("Trying " + filename);
            }
        }
        CipherInputStream in = null;
        in = new CipherInputStream(is, cipher);
        try {
            dbdoc = db.parse(in);
        } catch (SAXException sxe) {
            ParsePrint(sxe.getMessage());
            sxe.printStackTrace();
            return false;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Create a new parser.
     */
    public ActionDBParser() { // ActionDBEntry base
        dbf =  DocumentBuilderFactory.newInstance();

        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        }
    }

    /**
     * Point the parser to a new database file.
     * @param filename the database file to be parsed
     */
    public void NewParse(String filename) {
        try {
            dbdoc = db.parse(new File(filename));
        } catch (SAXException sxe) {
            ParsePrint(sxe.getMessage());
            sxe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        dbroot = dbdoc.getDocumentElement();
    }

    private Predicate typedPredicate(String desc)
    {
        int opar = desc.indexOf('(');     // open parenthesis
        int cpar = desc.lastIndexOf(')'); // close parenthesis
        String name = desc.substring(0, opar);
        String args = desc.substring(opar+1, cpar).replaceAll(" ","");
        ArrayList<com.Symbol> arglist = new ArrayList<com.Symbol>();
        Symbol nextArg = null;
        //log.debug("parsing "+desc+" into "+name+" and "+args);

	ArrayList<String> tokens = tokenizeArgs(args);

	for(int i=0; i<tokens.size(); i++) {
	    cpar = tokens.get(i).indexOf(')');
	    if (cpar > 0) {
		//System.err.println("functionPredicate() - " + tokens.get(i));
	        nextArg = typedPredicate(tokens.get(i));
		//System.err.println("functionPredicate() fp - " + nextArg.toString() + " : " + nextArg.getClass().toString());
	    } else {
		//System.err.println("functionPredicate() - " + tokens.get(i));
                String a = tokens.get(i);
                nextArg = varmap.get(a);
                if (nextArg == null) {
                    nextArg = constmap.get(a);
                }
                if (nextArg == null) {
                    log.debug("unknown argument to "+desc+": "+a);
                    nextArg = new Symbol(a);
                }
	    }
    	    arglist.add(nextArg);
 	}
        return new Predicate(name, arglist);
    }

    /**
     * Parse the current file as the child(ren) of the provided node.  This is
     * the public entry for parsing a database file.
     * @param rootNode the point in the database to which the newly parsed
     * entries should be attached.
     */
    public void ParseNode(ActionDBEntry rootNode) {
        for (Node child = dbroot.getFirstChild(); child != null; 
                child = child.getNextSibling()) {
            if (child.getNodeType() == Node.TEXT_NODE) {
                ParsePrint("  " + child.getNodeValue());
            } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                ParsePrint(child.getNodeName()+" = "+
                	child.getFirstChild().getNodeValue());
                ParseNode(child, rootNode, " ");
            }
        }
        /*
            for (APIElement a:api)
                log.debug(a+"\n");
        */
        if (plannerOpen != null) {
            log.debug("Adding open goal now");
            plannerProblem.add(plannerOpen);
        }
    }

    /**
     * Continue the parse of the current file.  This is called by
     * <code>ParseNode</code> above, and recursively by itself.
     * @param root the xml root of the current subtree
     * @param rootNode the corresponding database entry
     * @param indent an indent string used for printing
     * @return the new database entry created by this parse
     */
    @ActionCodeNote("let's add an 'attend' directive here to set the attends based on the task")
    private ActionDBEntry ParseNode(Node root, ActionDBEntry rootNode, 
            String indent) {
        ActionDBEntry newNode = null;
        String name = root.getNodeName().trim().toLowerCase();
        String value = root.getFirstChild().getNodeValue().trim().toLowerCase();
        String Value = root.getFirstChild().getNodeValue().trim();
        if (name.equals("name")) {
            ParsePrint(indent + name + ": " + value);
            if (currentAction != null) {
                // print previously parsed node
                if (currentAction.charAt(currentAction.length() - 1) == ' ') {
                    currentAction.deleteCharAt(currentAction.length() - 1);
                    currentAction.deleteCharAt(currentAction.length() - 1);
                }
                currentAction.append(")");
                String caname = currentAction.substring(0, currentAction.indexOf("("));
                ActionDBEntry caentry = ActionDBEntry.lookup(caname.toLowerCase());
                if (caentry != null) {
                    if (printActions && 
                            !caname.equalsIgnoreCase("motionPrimitive") &&
                            !caname.equalsIgnoreCase("speechRecPrimitive") &&
                            !caname.equalsIgnoreCase("speechProdPrimitive") &&
                            !caname.equalsIgnoreCase("visionPrimitive") &&
                            !caname.equalsIgnoreCase("script") &&
                            (caentry.isA("motionPrimitive") ||
                             caentry.isA("speechRecPrimitive") ||
                             caentry.isA("speechProdPrimitive") ||
                             caentry.isA("visionPrimitive") ||
                             caentry.isA("script")))
                        log.debug(currentAction);
                    if (printTypes && caentry.isA("object"))
                        log.debug(indent+caname);
                }
            }
            currentAction = new StringBuilder(Value);
            currentAction.append("(");
        } else if (name.equals("var")) { // vars used to be called roles
            NodeList children = root.getChildNodes();
            String rolename = getElementValue(root, "varname");
            String roletype = getElementValue(root, "vartype");
            String roledefault = getElementValue(root, "vardefault");
            String rolerole = getElementValue(root, "varrole");
            boolean rolereturn = false;
            String RoleName = getElementValue2(root, "varname");
            String RoleType = getElementValue2(root, "vartype");
            String RoleRole = getElementValue2(root, "varrole");
            if (rolerole != null && rolerole.equalsIgnoreCase("return"))
                rolereturn = true;
            ParsePrint(indent + name + ": " + rolename +" "+ roletype);
            varmap.put(rolename, new Variable(rolename, roletype));
            rootNode.addRole(rolename, roletype, roledefault, rolereturn);
            if ((rolename.charAt(0) != '!') && (currentAction != null)) {
                if ((RoleRole != null) && RoleRole.equals("return")) {
                    currentAction.append(RoleRole);
                    currentAction.append(" ");
                }
                currentAction.append(RoleType);
                currentAction.append(" ");
                currentAction.append(RoleName);
                currentAction.append(", ");
            }
            if ((rolename.charAt(0) != '!') && (apie != null)) {
                String newVar = RoleName+": "+RoleType;
                String RoleDesc;
                RoleDesc = getAttribute(root, "desc");
                boolean hiddenVar = false;
                if (RoleDesc != null) {
                    newVar = newVar+" - "+RoleDesc;
                    if (RoleDesc.equalsIgnoreCase("hidefromapi"))
                        hiddenVar = true;
                }
                if (! hiddenVar) {
                    if ((RoleRole != null) && RoleRole.equals("return")) {
                        //apie.addRet(RoleName+": "+RoleType);
                        apie.addRet(newVar);
                    } else {
                        apie.addVar(newVar);
                    }
                }
            }
            if (plannerPredicate != null) {
                plannerPredicate.addVar(rolename, roletype);
            } else if (plannerAction != null && (rolename.charAt(0) != '!')) {
                plannerAction.addVar(rolename, roletype);
            }
        } else if (name.equals("locks")) {
            ParsePrint(indent + name + ": " + value);
            rootNode.addLocks(value);
            if (apie != null) {
                apie.addLock(Value);
            }
        } else if (name.equals("cost")) {
            ParsePrint(indent + name + ": " + value);
            rootNode.addCost(value);
            // add cost to plannerDomain action
            if (plannerAction != null) {
                try {
                    plannerAction.setCost(Float.parseFloat(value));
                } catch (NumberFormatException nfe) {
                    System.err.println("Invalid cost ("+value+"), defaulting to 0.");
                    plannerAction.setCost(0.0f);
                }
            }
        } else if (name.equals("benefit")) {
            ParsePrint(indent + name + ": " + value);
            rootNode.addBenefit(value);
        } else if (name.equals("posaff")) {
            ParsePrint(indent + name + ": " + value);
            rootNode.addAffect(value, true);
        } else if (name.equals("negaff")) {
            ParsePrint(indent + name + ": " + value);
            rootNode.addAffect(value, false);
        } else if (name.equals("minurg")) {
            ParsePrint(indent + name + ": " + value);
            rootNode.addMinUrg(value);
        } else if (name.equals("maxurg")) {
            ParsePrint(indent + name + ": " + value);
            rootNode.addMaxUrg(value);
        } else if (name.equals("timeout")) {
            ParsePrint(indent + name + ": " + value);
            rootNode.addTimeout(value);
            // add duration to plannerDomain action
            if (plannerAction != null) {
                try {
                    plannerAction.setDuration(Float.parseFloat(value));
                } catch (NumberFormatException nfe) {
                    System.err.println("Invalid duration ("+value+"), defaulting to inf.");
                    plannerAction.setDuration(Float.POSITIVE_INFINITY);
                }
            }
        } /* else if (name.equals("precond")) {
            ParsePrint(indent + name + ": " + value);
            rootNode.addPrecond(value);
        } else if (name.equals("postcond")) {
            ParsePrint(indent + name + ": " + value);
            // using createPredicate as a shortcut to get the args out
            // (but need to have Variable instead of Symbol args)
            Predicate p = createPredicate(value);
            ArrayList<Symbol> args = new ArrayList<Symbol>();
            String override = getAttribute(root, "override");
            for (int j = 0; j < p.size(); j++) {
                Symbol s = p.get(j);
                String n = s.getName();
                String t = null;
                for (int i = 0; i < rootNode.getNumRoles(); i++) {
                    if (n.equals(rootNode.getRoleName(i))) {
                        t = rootNode.getRoleType(i);
                        break;
                    }
                }
                args.add(new Variable(n, t));
            }
            p = new Predicate(p.getName(), args);
            rootNode.addPostcondToDB(p, Boolean.parseBoolean(override));
        } */ else if (name.equals("conditions")) {
            Predicate cond;
            if (plannerAction != null) {
                for (Node child = root.getFirstChild(); child != null; 
                        child = child.getNextSibling()) {
                    if (child.getNodeType() == Node.TEXT_NODE) {
                        //ParsePrint(indent + child.getNodeValue());
                    } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                        cond = createPredicate(child.getFirstChild().getNodeValue());
                        if (child.getNodeName().trim().equals("atstart")) {
                            plannerAction.addAtStart(cond);
                            rootNode.addStartCondition(cond);
                        } else if (child.getNodeName().trim().equals("overall")) {
                            plannerAction.addOverAll(cond);
                            rootNode.addOverAllCondition(cond);
                        }
                    }
                }
            }
        } else if (name.equals("effects")) {
            Predicate cond;
            boolean postcond = checkAttribute(root, "postcond", "false");
            for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child.getNodeType() == Node.TEXT_NODE) {
                    //ParsePrint(indent + child.getNodeValue());
                } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                    boolean override = checkAttribute(child, "override", "true");
                    boolean negate = false;
                    String desc = child.getFirstChild().getNodeValue();
                    cond = typedPredicate(desc);
                    if (plannerAction != null) {
                        boolean always = checkAttribute(child, "always", "true");
                        if (child.getNodeName().trim().equals("atstart")) {
                            plannerAction.addStartEffect(cond);
                        } else if (child.getNodeName().trim().equals("atend")) {
                            plannerAction.addEndEffect(cond);
                        } else {
                            log.debug("Unknown effect type: "+child.getNodeName());
                        }
                        if (always) {
                            // add to effects list
                            rootNode.addEffect(cond);
                        } else {
                            // add to successEffects list
                            rootNode.addSuccessEffect(cond);
                        }
                    }
                    if (! cond.getName().equals("not") && !postcond) {
                        // add to static postconditions DB
                        rootNode.addPostcondToDB(cond, override);

                        // CC: add to local postconditions
                        rootNode.addPostcond(cond);
                    }
                }
            }
        } else if (name.equals("actspec") || name.equals("control")) {
            // PWS: could handle these differently (e.g., check control)
            ParsePrint(indent + name + ": " + Value);
            // PWS: passing along the uppercase version...
            rootNode.addEventSpec(Value);
        } else if (name.equals("achieve")) {
            // PWS: a state to be achieved
            ParsePrint(indent + name + ": " + Value);
            // PWS: passing along the uppercase version...
            rootNode.addEventSpec("achieve " + Value);
        } else if (name.equals("openvar")) {
            if (plannerOpen != null) {
                String varname = getElementValue(root, "varname");
                String vartype = getElementValue(root, "vartype");
                plannerOpen.addOpenVar(varname, vartype);
            }
        } else if (name.equals("sensevar")) {
            if (plannerOpen != null) {
                String varname = getElementValue(root, "varname");
                String vartype = getElementValue(root, "vartype");
                plannerOpen.setSenseVar(varname, vartype);
            }
        } else if (name.equals("sensefact")) {
            if (plannerOpen != null) {
                ArrayList<Symbol> args = new ArrayList<Symbol>();
                for (Node child = root.getFirstChild(); child != null; 
                        child = child.getNextSibling()) {
                    String childName = child.getNodeName().trim().toLowerCase();
                    if (childName.equals("var")) {
                        String n = getElementValue(child, "varname");
                        String t = getElementValue(child, "vartype");
                        args.add(new Variable(n, t));
                    } else if (childName.equals("const")) {
                        String n = getElementValue(child, "constname");
                        String t = getElementValue(child, "vartypconst");
                        args.add(new Constant(n, t));
                    }
                }
                plannerOpen.addSenseFact(new Predicate(getElementValue(root, "name"), args));
            }
        } else if (name.equals("senseeffect")) {
            if (plannerOpen != null) {
                ArrayList<Symbol> args = new ArrayList<Symbol>();
                for (Node child = root.getFirstChild(); child != null; 
                        child = child.getNextSibling()) {
                    String childName = child.getNodeName().trim().toLowerCase();
                    if (childName.equals("var")) {
                        String n = getElementValue(child, "varname");
                        String t = getElementValue(child, "vartype");
                        args.add(new Variable(n, t));
                    }
                }
                plannerOpen.setSenseEffect(new Predicate(getElementValue(root, "name"), args));
            }
        } else if (name.equals("type")) {
            NodeList children = root.getChildNodes();
            String rootName = getElementValue(root, "name");
            String rootDesc = getElementValue(root, "desc");
            // could use this to root element in supplemental db file
            String rootSuper = getElementValue(root, "supertype");

            // add any previous plannerDomain element to list
            // (this will break if subtypes are added before the type is finished)
            if (plannerType != null) {
                plannerDomain.add(plannerType);
                plannerType = null;
            }
            if (plannerAction != null) {
                plannerDomain.add(plannerAction);
                plannerAction = null;
            }
            if (plannerPredicate != null) {
                plannerDomain.add(plannerPredicate);
                plannerPredicate = null;
            }
            if (plannerOpen != null) {
                log.debug("Adding open goal now");
                plannerProblem.add(plannerOpen);
                plannerOpen = null;
            }
            varmap.clear();

            boolean reuse = checkAttribute(root, "override", "false");
            if (ActionDBEntry.lookup(rootName) != null && reuse) {
                // use existing node
                newNode = ActionDBEntry.lookup(rootName);
            } else {
                if (rootSuper == null) {
                    // if no explicitly-defined supertype, attach to the node passed in
                    rootSuper = rootNode.getType();
                }

                newNode = new ActionDBEntry(rootName, rootSuper);
                newNode.addDescription(rootDesc);
                reuse = false;
            }

            // check whether this should be sent to the planner
            boolean sendPlanner = checkAttribute(root, "planner", "true");
            if (! sendPlanner || reuse) {
                // nothing
            } else if (newNode.isA("constant")) {
                String pName = newNode.getType();
                String pType = getElementValue(root, "ctype");
                plannerConstant = new PlannerElement(pName, PlannerElement.CONSTANT);
                plannerConstant.setPlannerType(pType);
                plannerProblem.add(plannerConstant);
                plannerConstant = null;
                Constant c = new Constant(pName, pType);
                constmap.put(pName, c);
            } else if (newNode.isA("state")) {
                plannerState = new PlannerElement(newNode.getType(), PlannerElement.STATE);
                plannerProblem.add(plannerState);
                plannerState = null;
            } else if (newNode.isA("goal")) {
                String elem;
                plannerGoal = new PlannerElement(newNode.getType(), PlannerElement.GOAL);
                elem = getElementValue(root, "hard");
                if (! elem.equals("true"))
                    plannerGoal.setSoftGoal();
                elem = getElementValue(root, "benefit");
                plannerGoal.setBenefit(Float.parseFloat(elem));
                elem = getElementValue(root, "deadline");
                plannerGoal.setDeadline(Float.parseFloat(elem));
                plannerProblem.add(plannerGoal);
                plannerGoal = null;
            } else if (newNode.isA("opengoal")) {
                String elem;
                ArrayList<Symbol> openGoalVars = new ArrayList<Symbol>();
                plannerOpen = new PlannerElement(newNode.getType(), PlannerElement.OPEN);
                elem = getElementValue(root, "hard");
                if (! elem.equals("true"))
                    plannerOpen.setSoftGoal();
                elem = getElementValue(root, "benefit");
                plannerOpen.setBenefit(Float.parseFloat(elem));
                elem = getElementValue(root, "deadline");
                plannerOpen.setDeadline(Float.parseFloat(elem));
                // get all the constants and variables for the open goal
                for (Node child = root.getFirstChild(); child != null; 
                        child = child.getNextSibling()) {
                    String childName = child.getNodeName().trim().toLowerCase();
                    if (childName.equals("goalvar")) {
                        String n = getElementValue(child, "varname");
                        String t = getElementValue(child, "vartype");
                        openGoalVars.add(new Variable(n, t));
                    } else if (childName.equals("goalconst")) {
                        String n = getElementValue(child, "constname");
                        String t = getElementValue(child, "consttype");
                        openGoalVars.add(new Constant(n, t));
                    }
                }
                //log.debug("GOT OPENGOAL: "+new Predicate(newNode.getType(), openGoalVars));
                plannerOpen.setOpenGoal(new Predicate(newNode.getType(), openGoalVars));
            } else if (newNode.isA("predicate")) {
                plannerPredicate = new PlannerElement(newNode.getType(), PlannerElement.PREDICATE);
            } else if (newNode.isA("object")) {
                plannerType = new PlannerElement(newNode.getType(), PlannerElement.TYPE);
                plannerType.setSupertype(newNode.getSupertype());
            } else if (newNode.isA("action")) {
                plannerAction = new PlannerElement(newNode.getType(), PlannerElement.ACTION);
            } else {
                System.err.println("Unrecognized planner entity type ("+newNode.getType()+"), not sending to planner");
            }

            String rootApi = getElementValue2(root, "api");
            apie = null;
            if (rootApi != null) {
                String elementName = "";
                if (! newNode.isA("action"))
                    elementName = indent;
                elementName += getElementValue2(root, "name");
                apie = new APIElement(elementName, rootApi);
                api.add(apie);
            }
            for (Node child = root.getFirstChild(); child != null; 
                    child = child.getNextSibling()) {
                if (child.getNodeType() == Node.TEXT_NODE) {
                    //ParsePrint(indent + child.getNodeValue());
                } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                    //ParsePrint(child.getNodeName()+" = "+
                    //	child.getFirstChild().getNodeValue());
                    ParseNode(child, newNode, indent + " ");
                }
            }
        } else if (name.equals("subtypes")) {
            NodeList children = root.getChildNodes();
            for (Node child = root.getFirstChild(); child != null; 
                    child = child.getNextSibling()) {
                if (child.getNodeType() == Node.TEXT_NODE) {
                    //ParsePrint(indent + child.getNodeValue());
                } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                    //ParsePrint(child.getNodeName()+" = "+
                    //	child.getFirstChild().getNodeValue());
                    ParseNode(child, rootNode, indent);
                }
            }
        } else if (name.equals("goal")) {
            // add the value to the goals list
            goals.add(createPredicate(value));
        } else if (name.equals("exec")) {
            // add the value to the execs list
            execs.add(value);
        } else if (name.equals("badstate")) {
            // add the value to the forbidden states list
            badStates.add(createPredicate(value));
        } else if (name.equals("badaction")) {
            // add the value to the forbidden actions list
            badActions.add(createPredicate(value));
        } else if (name.equals("funcmap")) {
            NodeList children = root.getChildNodes();
            String fname = null;
            String farg = null;
            String fval = null;
            for (Node child = root.getFirstChild(); child != null; 
                    child = child.getNextSibling()) {
                if (child.getNodeType() == Node.TEXT_NODE) {
                    //ParsePrint(indent + child.getNodeValue());
                } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                    //ParsePrint(child.getNodeName()+" = "+
                    //	child.getFirstChild().getNodeValue());
                    if (child.getNodeName().trim().equals("funcname"))
                        fname = child.getFirstChild().getNodeValue();
                    else if (child.getNodeName().trim().equals("funcarg"))
                        farg = child.getFirstChild().getNodeValue();
                    else if (child.getNodeName().trim().equals("funcval"))
                        fval = child.getFirstChild().getNodeValue();
                }
            }
            if (fname != null && farg != null && fval != null) {
                rootNode.addFunction(fname, farg, fval);
            }
        } else {
            ParsePrint(indent + name);
        }
        return newNode;
    }

    private class APIElement {
        String name;
        String desc;
        ArrayList<String> vars = null;
        ArrayList<String> rets = null;
        ArrayList<String> lcks = null;

        public APIElement(String n, String d) {
            name = n;
            desc = d;
        }

        public void addVar(String var) {
            if (vars == null)
                vars = new ArrayList<String>();
            vars.add(var);
        }

        public void addRet(String ret) {
            if (rets == null)
                rets = new ArrayList<String>();
            rets.add(ret);
        }

        public void addLock(String lock) {
            if (lcks == null)
                lcks = new ArrayList<String>();
            lcks.add(lock);
        }

        @Override
        public String toString() {
            String newapi = name+": "+desc;
            String var = "\n  Parameter vars:\n";
            String ret = "\n  Return vars:\n";
            String lck = "\n  Resource locks:\n";
            if (vars != null) {
                for (String v:vars) {
                    newapi = newapi+var+"    "+v;
                    var = "\n";
                }
            }
            if (rets != null) {
                for (String r:rets) {
                    newapi = newapi+ret+"    "+r;
                    ret = "\n";
                }
            }
            if (lcks != null) {
                for (String l:lcks) {
                    newapi = newapi+lck+"    "+l;
                    lck = "\n";
                }
            }
            //newapi += "\n";
            return newapi;
        }
    }

    /**
     * Helper method to get a named element value (a child of the current xml
     * node).
     * @param node the parent node
     * @param name the name of the child for which a value is sought
     * @return the value found, toLowerCase
     */
    private String getElementValue(Node node, String name) {
        NodeList children = node.getChildNodes();
        String val = null;
        for (Node child = node.getFirstChild(); child != null; 
                child = child.getNextSibling()) {
            if ((child.getNodeType() == Node.ELEMENT_NODE) &&
                    child.getNodeName().equalsIgnoreCase(name)) {
                val = child.getFirstChild().getNodeValue().trim().toLowerCase();
            }
        }
        return val;
    }

    /**
     * Helper method to get a named element value (a child of the current xml
     * node).
     * @param node the parent node
     * @param name the name of the child for which a value is sought
     * @return the value found NOT toLowerCase
     */
    private String getElementValue2(Node node, String name) {
        NodeList children = node.getChildNodes();
        String val = null;
        for (Node child = node.getFirstChild(); child != null; 
                child = child.getNextSibling()) {
            if ((child.getNodeType() == Node.ELEMENT_NODE) &&
                    child.getNodeName().equalsIgnoreCase(name)) {
                val = child.getFirstChild().getNodeValue().trim();
            }
        }
        return val;
    }

    /**
     * Helper method to get a named attribute of the given node.
     * @param node the parent node
     * @param name the name of the attribute
     * @return the value found NOT toLowerCase, null if undefined
     */
    private String getAttribute(Node node, String name) {
        NamedNodeMap attribs;
        Node attrib = null;
        String val = null;
        
        if (node.hasAttributes()) {
            attribs = node.getAttributes();
            attrib = attribs.getNamedItem(name);
        }
        
        if (attrib != null)
            val = attrib.getNodeValue();
        return val;
    }

    /**
     * Helper method to check the named attribute against a given value
     * @param node the parent node
     * @param name the name of the attribute
     * @param check the value to check for
     * @return true if the attribute is defined and is equal to the check parameter
     */
    private boolean checkAttribute(Node node, String name, String check) {
        NamedNodeMap attribs;
        Node attrib = null;
        String val = null;
        boolean checkVal = false;

        if (node.hasAttributes()) {
            attribs = node.getAttributes();
            attrib = attribs.getNamedItem(name);
        }
        
        if (attrib != null) {
            val = attrib.getNodeValue();
            checkVal = val.equals(check);
        }
        return checkVal;
    }

    /**
     * Helper function for printing.
     * @param text the string to (potentially) print
     */
    private void ParsePrint(String text) {
        if (verbose) {
            System.out.println(text);
        }
    }

    /**
     * Get the name of the current xml tree's root node.  This is used
     * primarily when constructing the root database entry.
     * @return the name
     */
    public String getRootName() {
        return getElementValue(dbroot, "name");
    }

    /**
     * Get list of goals found in DB file. 
     * @return the list of goals
     */
    public ArrayList<Predicate> getGoals() {
        return goals;
    }

    /**
     * Get list of forbidden actions found in DB file. 
     * @return the list of forbidden actions
     */
    public ArrayList<Predicate> getBadActions() {
        return badActions;
    }

    /**
     * Get list of forbidden states found in DB file. 
     * @return the list of forbidden states
     */
    public ArrayList<Predicate> getBadStates() {
        return badStates;
    }

    /**
     * Get list of script invocations found in DB file. 
     * @return the list of taskspecs
     */
    public ArrayList<String> getExecs() {
        return execs;
    }

    /**
     * Get list of elements to be sent to the plannerDomain
     * @return the list of plannerDomain elements
     */
    public ArrayList<PlannerElement> getPlannerDomainElements() {
        return plannerDomain;
    }

    /**
     * Get list of elements to be sent to the plannerDomain
     * @return the list of plannerDomain elements
     */
    public ArrayList<PlannerElement> getPlannerProblemElements() {
        return plannerProblem;
    }

    public String getAPI() {
        StringBuilder sb = new StringBuilder();

        for (APIElement a:api) {
            sb.append(a).append("\n");
            // the following adds an extra newline between scripts and primitives, but not types
            if (!a.toString().startsWith(" "))
                sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Run the parser independently (for testing)
     * @param argv pass the filename on the command line
     */
    public static void main(String argv[]) {
        ActionDBParser adbp;

        if (argv.length != 1) {
            System.err.println("Usage: java ActionDBParser filename");
            System.exit(1);
        }

        adbp = new ActionDBParser(argv[0], true);
    }
}

// vi:ai:smarttab:expandtab:ts=8 sw=4
