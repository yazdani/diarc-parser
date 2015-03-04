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

import com.algorithms.planning.{StateContainer, StateSpace}
import com.collections.{KdTree, KdTreeMap}

import KdTree._

class KdContainer[T <: Dimensioned](val stateSpace: StateSpace[T])
    extends KdTreeMap[T, List[T]](stateSpace.dimensionality) with StateContainer[T] {

  // TODO: replace this with something faster (no need for multiple searches)
  def addState(state: T) = if (!contains(state)) this += ((state, Nil))

  def connectStates(orig: T, dest: T) = {
    // TODO: replace this with something faster
    put(orig, dest :: this(orig))
    put(dest, orig :: this(dest))
  }

  def states: Iterable[T] = keys

  def getConnections(query: T): Iterable[T] = get(query).getOrElse(Nil)

  // TODO: parameterize KdTreeMap.nearestNeighbor to accept a distance metric
  override def nearestNeighbor(query: T): Option[T] = super[KdTreeMap].nearestNeighbor(query)

  val threadsafe = false
}
