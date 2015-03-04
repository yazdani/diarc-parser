/**
 * Agent Development Environment (ADE)
 *
 * @version 1.0
 * @author M@
 * @file com/algorithms/planning/rrt/demo/RrtDemoComponentImpl.java
 *
 * Copyright 1997-2013 M@ Dunlap and HRILab (hrilab.org)
 * All rights reserved. Do not copy and use without permission.
 * For questions contact M@ at <matthew.dunlap+hrilab@gmail.com>
 */
package com.algorithms.planning.samplingBased.demo;

import ade.Connection;
import ade.SuperADEComponentImpl;
import ade.gui.ADEGuiVisualizationSpecs;
import com.algorithms.planning.Path;
import com.algorithms.planning.Planner;
import com.algorithms.planning.StateContainer;
import com.algorithms.planning.flatland.FlatlandSearchProblem;
import com.algorithms.planning.flatland.FlatStateSpace;
import com.algorithms.planning.flatland.FlatStateSpace.FlatGoal;
import com.algorithms.planning.referenceImplementations.BabyStepsMotionValidator;
import com.algorithms.planning.referenceImplementations.CollisionCheckingMotionValidator;
import com.algorithms.planning.samplingBased.BallTree;
import com.algorithms.planning.samplingBased.GoalAwareRrt;
import com.algorithms.planning.samplingBased.Rrt;
import java.rmi.RemoteException;
import java.util.concurrent.Future;
import scala.Option;
import utilities.scala.JavaInterop;

/**
 * 
 *
 * @author M@ <matthew.dunlap+hrilab@gmail.com>
 */
public class RrtDemoComponentImpl extends SuperADEComponentImpl implements RrtDemoComponent {
  private boolean constructed;
  Planner planner;
  FlatStateSpace space;
  Future<Option<Path>> solution;

  public RrtDemoComponentImpl() throws RemoteException {
    super();

    TestValidityChecker tvc = new TestValidityChecker();
    space = new FlatStateSpace(tvc);
    FlatGoal goal = new FlatGoal(space.makeState(.91, .91), .003);
    BallTreeProblem problem = new BallTreeProblem(space.makeState(.01, .01), space, goal);
    // FlatlandSearchProblem problem = new FlatlandSearchProblem(space.makeState(.01, .01), space, goal);
    // problem.motionValidator_$eq(new BabyStepsMotionValidator(.02, space));  // sorry for the gnarly syntax, it's an interop thing
    problem.setMotionValidator(new CollisionCheckingMotionValidator(.02, space, tvc));
    // planner = new RRT(problem);
    // planner = new GoalAwareRrt(problem);
    planner = new BallTree(problem);
    solution = JavaInterop.JavaFuture(planner.startPlanning());
    
    constructed = true;
  }

  @Override
  protected void init() {
    constructed = false;
  }

  @Override
  protected void readyUpdate() {
  }

  @Override
  protected boolean localServicesReady() {
    return constructed && requiredConnectionsPresent();
  }

  @Override
  protected String additionalUsageInfo() {
    return "";
  }

  @Override
  public ADEGuiVisualizationSpecs getVisualizationSpecs() throws RemoteException {
    ADEGuiVisualizationSpecs specs = super.getVisualizationSpecs();
    specs.add("planner", Rrt2dGui.class, space);
    return specs;
  }

  @Override
  public RrtVisualizationParams getVisualizationParams() {
    return new RrtVisualizationParams(2, true);
  }

  @Override
  public StateContainer getData() {
    return planner.getStates();
  }

  @Override
  public Option<Path> getSolution() {
    if (solution.isDone()) {
      try {
        return solution.get();
      } catch (Exception e) {}
    }
    return Option.apply(null);
  }
}