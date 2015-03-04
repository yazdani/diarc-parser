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

abstract class StateSpace[T] extends Serializable {
  def dimensionality: Int
  def distanceBetween(a: T, b: T): Double
  def makePath(orig: T, dest: Option[T], container: StateContainer[T]): Option[Path[T]]
  def makeState(coords: Seq[Double], percentages: Boolean): T
  def unmakeState(pt: T): Seq[Double]
  def makeStateContainer: StateContainer[T]
  def nearestNeighbor(query: T, container: StateContainer[T]): Option[T]
  def pointAlongPath(from: T, to: T, distance: Double): T
  def stateSampler: StateSampler[T]
  // def stepTowards(orig: T, dest: T): T

  def validState(state: T): Boolean
  def projectToUnitDistance(d: Double): Double
  def projectToUnit2D(state: T): (Double, Double)
  def projectToUnit3D(state: T): (Double, Double, Double)
}
