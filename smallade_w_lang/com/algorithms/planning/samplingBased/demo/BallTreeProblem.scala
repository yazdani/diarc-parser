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
package com.algorithms.planning.samplingBased.demo

import com.algorithms.planning.{Goal, ProblemDefinition, StateContainer, StateSpace}
import com.algorithms.planning.samplingBased.BallTree.BallForest

case class BallTreeProblem[T](startState: T, stateSpace: StateSpace[T], goal: Goal[T]) extends ProblemDefinition[T] {
  var i = 0
  def shouldGrow(container: StateContainer[T]): Boolean = container match {
    case forest: BallForest[T] => {
      !forest.trees.exists(tree => tree.containsStart && tree.containsEnd)// && false
    }
    case _ => goal.getSolution.isEmpty
  }
}

import com.algorithms.planning.StateValidityChecker
import com.algorithms.planning.flatland.FlatStateSpace.Point

class TestValidityChecker extends StateValidityChecker[Point] {
  def isValid(point: Point): Boolean = !(point.x > .4 && .6 > point.x && (point.y < .85 || point.y > .86))
}
