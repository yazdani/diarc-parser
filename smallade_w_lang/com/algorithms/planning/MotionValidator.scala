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

trait MotionValidator[T] {
  def stepTowards(orig: T, dest: T): T
  def getOuterInvalidStatesBetween(orig: T, dest: T): Option[(T, T)]
}
