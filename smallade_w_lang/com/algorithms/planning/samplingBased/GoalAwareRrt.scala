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
import scala.util.Random

class GoalAwareRrt[T](problem: ProblemDefinition[T], goalProbability: Double, seed: Long) extends Rrt[T](problem) {
  def this(problem: ProblemDefinition[T], goalProbability: Double) = this(problem, goalProbability, Long.MinValue)
  def this(problem: ProblemDefinition[T]) = this(problem, .01)
  protected val rand = if (seed == Long.MinValue) new Random else new Random(seed)

  override protected def singleStep: T = rand.nextDouble < goalProbability match {
    case true => stepTowards(problem.goal.goalSamples.head)
    case false => super.singleStep
  }
}
