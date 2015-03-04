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
package com.algorithms.planning.referenceImplementations

import com.algorithms.planning.{MotionValidator, StateSpace, StateValidityChecker}

class MarleyMotionValidator[T] extends MotionValidator[T] {
  def stepTowards(orig: T, dest: T): T = dest  // don't worry, be happy, you're there mon
  def getOuterInvalidStatesBetween(orig: T, dest: T): Option[(T, T)] = None
}

class BabyStepsMotionValidator[T](stepSize: Double, stateSpace: StateSpace[T]) extends MotionValidator[T] {
  def stepTowards(orig: T, dest: T): T = stateSpace.pointAlongPath(orig, dest, stepSize)
  def getOuterInvalidStatesBetween(orig: T, dest: T): Option[(T, T)] = None
}

class CollisionCheckingMotionValidator[T](stepSize: Double, stateSpace: StateSpace[T], svc: StateValidityChecker[T]) extends MotionValidator[T] {
  def stepTowards(orig: T, dest: T): T = stateSpace.pointAlongPath(orig, dest, stepSize)

  def getOuterInvalidStatesBetween(orig: T, dest: T): Option[(T, T)] =
    Iterator.iterate(orig)(stepTowards(_, dest)).find{ a => !svc.isValid(a) || a == dest } match {
      case Some(a) if a == dest => None
      case Some(a) => Some((a, Iterator.iterate(dest)(stepTowards(_, orig)).find{ !svc.isValid(_) }.get))
  }
}
