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
package com.algorithms.planning

import com.algorithms.planning.referenceImplementations.MarleyMotionValidator
import scala.beans.BeanProperty

trait ProblemDefinition[T] {
  @BeanProperty var motionValidator: MotionValidator[T] = new MarleyMotionValidator[T]

  def startState: T
  def stateSpace: StateSpace[T]

  def goal: Goal[T]
  def shouldGrow(container: StateContainer[T]): Boolean
}
