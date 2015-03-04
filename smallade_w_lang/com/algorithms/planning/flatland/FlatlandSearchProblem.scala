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
package com.algorithms.planning.flatland

import com.algorithms.planning.{MotionValidator, ProblemDefinition, StateContainer}

import FlatStateSpace.{FlatGoal, Point}

class FlatlandSearchProblem(val startState: Point, val stateSpace: FlatStateSpace, val goal: FlatGoal) extends ProblemDefinition[Point] {
  def shouldGrow(unused: StateContainer[Point]) = goal.currentSolution == None
}
