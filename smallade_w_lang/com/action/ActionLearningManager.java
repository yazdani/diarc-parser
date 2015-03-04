/**
 * ADE 1.0
 * Copyright 1997-2012 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@cs.tufts.edu
 *
 * ActionLearningManager.java
 *
 * Last update: March 2013
 *
 * @author Gordon Briggs
 *
 */

package com.action;

import ade.ADETimeoutException;
import ade.ADEReferenceException;
import ade.ADEException;
import com.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class ActionLearningManager{

  private Map<Predicate,ActionLearner> als;
  private GoalManagerImpl gmi;

  public ActionLearningManager(GoalManagerImpl gmi) {
    als = new HashMap<Predicate,ActionLearner>();
    this.gmi = gmi;
  }

  public void handleStep(Predicate parentAction, Predicate stepAction) {
    if(!als.keySet().contains(parentAction)) {
      //ActionLearner al = new ActionLearner(parentAction.getName());
      ActionLearner al = new ActionLearner(parentAction);
      als.put(parentAction,al);
    }
  }

  public void handleStep(Predicate parentAction, ActionDBEntry stepAction) {
    if(!als.keySet().contains(parentAction)) {
      //ActionLearner al = new ActionLearner(parentAction.getName());
      ActionLearner al = new ActionLearner(parentAction);
      als.put(parentAction,al);
    }
    als.get(parentAction).addStep(stepAction);
  }

  public void handleStep(Predicate parentAction, String step) {
    if(!als.keySet().contains(parentAction)) {
      //ActionLearner al = new ActionLearner(parentAction.getName());
      ActionLearner al = new ActionLearner(parentAction);
      als.put(parentAction,al);
    }
    als.get(parentAction).addStep(step);
  }

  public void addRole(Predicate parentAction, String role) {
    als.get(parentAction).addRole(role);
  }

  public void addRole(Predicate parentAction, String name, String type, boolean isReturnVar) {
    als.get(parentAction).addRole(name,type,isReturnVar);
  }

  public void finalizeAction(Predicate action) {
    // needs to be more generalized...
    als.get(action).addPostCondition(action,false);
  }

  public ActionDBEntry getScript(Predicate action) {
      for(Predicate p : als.keySet()) {
          if(p.getName().equals(action.getName())) {
              return als.get(p).getScript();
          }
      }
      return null;
  }
  
}
