/**
 * Agent Development Environment (ADE)
 *
 * @version 1.0
 * @author M@
 *
 * Copyright 1997-2013 M@ Dunlap and HRILab (hrilab.org)
 * All rights reserved. Do not copy and use without permission.
 * For questions contact M@ at <matthew.dunlap+hrilab@gmail.com>
 */
package com.algorithms.planning.samplingBased

import com.algorithms.planning.{Path, Planner, ProblemDefinition}

class Rrt[T](problem: ProblemDefinition[T]) extends Planner[T] {
  val tree = problem.stateSpace.makeStateContainer
  tree.addState(problem.startState)

  def getStates = tree

  def plan: Option[Path[T]] = {
    // TODO: multithread me
    while (problem.shouldGrow(tree)) {
      val noob = singleStep
      problem.goal.offerSolution(noob)
    }
    problem.stateSpace.makePath(problem.startState, problem.goal.getSolution, tree)
  }

  protected def singleStep: T = stepTowards(problem.stateSpace.stateSampler.sample)

  protected def stepTowards(target: T): T = {
    val near = problem.stateSpace.nearestNeighbor(target, tree).get  // this get is safe because we know the tree will never be empty
    val noob = problem.motionValidator.stepTowards(near, target)
    tree.addState(noob)
    tree.connectStates(near, noob)
    noob
  }

}
