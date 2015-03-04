/**
 * ADE 1.0
 * Copyright 1997-2012 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@cs.tufts.edu
 *
 * ActionLearner.java
 *
 * Last update: March 2013
 *
 * @author Tom Williams
 * @author Gordon Briggs
 *
 */

package com.action;

import com.*;
import java.util.ArrayList;

public class ActionLearner{


    private ActionDBEntry script;
    private ArrayList<String> roles = new ArrayList<String>();
    private ArrayList<String> eventSpects = new ArrayList<String>();

    protected ActionLearner(String name){
        this.script = new ActionDBEntry(name, "script");
        script.addRoles("self actor");
        this.roles.add("self actor");
    }

    protected ActionLearner(Term t) {
        String name = t.getName();
        this.script = new ActionDBEntry(name, "script");
        script.addRoles("?mover self");
        //script.addRoles("!mover self");
        script.addRoles("!hand string");

        this.roles.add("?mover self");
        this.roles.add("!hand string");

        for(Symbol s: t.getArgs()) {
            if(s instanceof Variable) {
                Variable v = (Variable)s;
                script.addRole(v.getName(),v.getType(),null,false);

                String newRole = v.getName() + " " + v.getType();
                this.roles.add(newRole);
            }
        }

        this.addStep("set ?mover self");
        this.addStep("set !hand testhand");

        this.eventSpects.add("set ?mover self");
        this.eventSpects.add("set !hand testhand");

        System.out.println("ActionLearner term constructor : " + this.script.toString());
    }

    protected void addRole(String role) {
        this.script.addRoles(role);
    }

    protected void addRole(String name, String type, boolean isReturnVar) {
        this.script.addRole(name, type, null, isReturnVar);
    }
    
    protected void addStep(ActionDBEntry subscript){
        System.out.println("### ActionLearner.addStep() number subEvents = " + subscript.getEventSpecs());
        /*for(ArrayList<String> subevent : subscript.getEventSpecs())
            script.addEventSpec(subevent);*/
        String str = subscript.getType() + " ";
        for(int i=0; i<subscript.getNumRoles(); i++) {
            String roleName = subscript.getRoleName(i);
            if(roleName.equals("?hand")) {
                str += "!hand";
            } else {
                str += subscript.getRoleName(i) + " ";
            }
        }
        System.out.println("### ActionLearner.addStep() str = " + str);
        this.script.addEventSpec(str);
        System.out.println("ActionLearner script : " + this.script.toString());
        
    }

    protected void addStep(ArrayList<String> step){
        script.addEventSpec(step);
    }

    protected void addStep(String step){
        script.addEventSpec(step);
    }
    
    protected ActionDBEntry getScript(){
        return script;
    }

    protected void addPostCondition(Predicate cond, boolean override){
        script.addPostcondToDB(cond, override);
        script.addPostcond(cond);
    }

    protected void addRole(String rname, String type, String defValue, boolean ret) {
        script.addRole(rname,type,defValue,ret);
    }

    public ArrayList<String> getRoles(){
        return this.roles;
    }

    public ArrayList<String> getEventSpects(){
        return this.eventSpects;
    }

}
