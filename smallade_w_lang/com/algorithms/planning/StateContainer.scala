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

trait StateContainer[T] extends Serializable {
  def addState(state: T)
  def connectStates(orig: T, dest: T)
  def getConnections(query: T): Iterable[T]
  def states: Iterable[T]
  def stateSpace: StateSpace[T]
  def threadsafe: Boolean

  // it's probably a good idea to override this with something faster if you can
  def nearestNeighbor(query: T): Option[T] = {
    if (states.isEmpty) {
      None
    } else {
      Some(states.minBy(stateSpace.distanceBetween(query, _)))
    }
  }
}
