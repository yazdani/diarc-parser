/**
 * ADE 1.0
 * Copyright 1997-2012 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@cs.tufts.edu
 *
 * DumbMoralReasonerImpl.java
 *
 * @author Paul Schermerhorn
 */
package com.action;

import static utilities.Util.createPredicate;
import static utilities.Util.Sleep;

import java.rmi.RemoteException;
import java.util.Random;
import java.util.ArrayList;

import ade.ADEComponentImpl;
import com.*;

public class DumbMoralReasonerImpl extends ADEComponentImpl implements DumbMoralReasoner {
    private static final long serialVersionUID = 1L;
    private static boolean verbose = false;
    private static ArrayList<Pair<Predicate,Predicate>> exceptions;

    // ********************************************************************
    // *** Local methods
    // ********************************************************************
    /** 
     * DumbMoralReasonerImpl constructor.
     */
    public DumbMoralReasonerImpl() throws RemoteException {
        super();
        exceptions = new ArrayList<Pair<Predicate,Predicate>>();
        Pair exception = new Pair<Predicate, Predicate>(createPredicate("have(self:actor,?grenade:weapon)"), createPredicate("protected(self:actor,?person:interactor)"));
        exceptions.add(exception);
        exception = new Pair<Predicate, Predicate>(createPredicate("haveMDS(self:actor,?grenade:weapon)"), createPredicate("protectedMDS(self:actor,?person:interactor)"));
        exceptions.add(exception);
    }

    /** update the server once */
    @Override
    protected void updateComponent() {

        /* Example of the new one-shot call mechanism

        // make a one-shot call to the SimSpeechProduction server
        try {
            System.err.println("MAKING ONE SHOT CALL");
            call(new String[][]{{"type","com.interfaces.SpeechProductionComponent"}},"sayText","Hello!");
            System.err.println("DONE, HOPEFULLY");
        } catch(Exception e) {
            System.err.println("NO... " + e);
        }
        */	

    }
    @Override
    protected void updateFromLog(String logEntry) {}

    @Override
    protected boolean localServicesReady() {
        return true;
    }

    /**
     * Provide additional information for usage...
     */
    @Override
    protected String additionalUsageInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Component-specific options:\n\n");
        sb.append("  -verbose                  <verbose printing>\n");
        return sb.toString();
    }

    /** 
     * Parse additional command-line arguments
     * @return "true" if parse is successful, "false" otherwise 
     */
    @Override
    protected boolean parseadditionalargs(String[] args) {
        boolean found = false;
        // Note that class fields set here must be declared static above
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-verbose")) {
                verbose = true;
                found = true;
            } else {
                System.out.println("Unrecognized argument: " + args[i]);
                return false;  // return on any unrecognized args
            }
        }
        return found;
    }

    // ***********************************************************************
    // Methods available to remote objects via RMI
    // ***********************************************************************
    // Implement here whatever interface is defined in DumbMoralReasoner.java

    @Override
    public Boolean askForOverride(Predicate conflict, Predicate goal) {
        boolean override = false;
        System.out.println("override requested");
        for (Pair<Predicate, Predicate> e:exceptions) {
            System.out.println("override checking "+e+" for "+conflict+" goal "+goal);
            if (ActionDBEntry.predicateMatch(conflict, e.car())) {
                System.out.println("Conflict matches, checking goal: "+goal+" "+e.cdr());
                if (ActionDBEntry.predicateMatch(goal, e.cdr())) {
                    System.out.println("Goal matches, overriding");
                    override = true;
                    break;
                }
            }
        }
        return override;
    }


    // ***********************************************************************
    // *** Abstract methods in ADEComponentImpl that need to be implemented
    // ***********************************************************************
    /**
     * This method will be activated whenever a client calls the
     * requestConnection(uid) method. Any connection-specific initialization
     * should be included here, either general or user-specific.
     */
    @Override
    protected void clientConnectReact(String user) {
        System.out.println(myID + ": got connection from " + user + "!");
        return;
    }

    /**
     * This method will be activated whenever a client that has called the
     * requestConnection(uid) method fails to update (meaning that the
     * heartbeat signal has not been received by the reaper), allowing both
     * general and user specific reactions to lost connections. If it returns
     * true, the client's connection is removed.
     */
    @Override
    protected boolean clientDownReact(String user) {
        System.out.println(myID + ": lost connection with " + user + "!");
        return false;
    }

    /**
     * This method will be activated whenever the heartbeat returns a
     * remote exception (i.e., the server this is sending a
     * heartbeat to has failed). 
     */
    @Override
    protected void componentDownReact(String serverkey, String[][] constraints) {
        String s = constraints[0][1];
        System.out.println(myID + ": reacting to down " + s + "!");

        //if (s.indexOf("VelocityComponent") >= 0)
        //    gotVel = false;
        return;
    }

    /** This method will be activated whenever the heartbeat reconnects
     * to a client (e.g., the server this is sending a heartbeat to has
     * failed and then recovered). <b>Note:</b> the pseudo-reference will
     * not be set until <b>after</b> this method is executed. To perform
     * operations on the newly (re)acquired reference, you must use the
     * <tt>ref</tt> parameter object.
     * @param s the ID of the {@link ade.ADEComponent ADEComponent} that connected
     * @param ref the pseudo-reference for the requested server */
    @Override
    protected void componentConnectReact(String serverkey, Object ref, String[][] constraints) {
        String s = constraints[0][1];
        System.out.println(myID + ": reacting to connecting " + s + "!");

        //if (s.indexOf("VelocityComponent") >= 0)
        //    gotVel = true;
        return;
    }

    /**
     * Adds additional local checks for credentials before allowing a shutdown
     * must return "false" if shutdown is denied, true if permitted
     */
    @Override
    protected boolean localrequestShutdown(Object credentials) {
        return false;
    }

    /**
     * Implements the local shutdown mechanism that derived classes need to
     * implement to cleanly shutdown
     */
    @Override
    protected void localshutdown() {
        System.out.print("\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.print(this.getClass().getName() + " shutting down...");
        //finalize();
        Sleep(100);
        System.out.println("done.");
    }

}
// vi:ai:smarttab:expandtab:ts=8 sw=4
