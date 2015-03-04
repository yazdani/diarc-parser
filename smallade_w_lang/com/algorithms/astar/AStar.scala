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
package com.algorithms.astar

import com.collections.PrioritySet
import scala.annotation.tailrec
import scala.collection.mutable
import scala.math.Ordering

object AStar {
  trait AStarState[T] {
    def distanceTo(other: T): Double
    def getNeighbors: Iterable[T]
  }

  def apply[T <: AStarState[T]](origin: T, goal: T): Option[Seq[T]] =
    apply[T](origin, goal, (_:T).distanceTo(_:T), (_:T).getNeighbors)

  def apply[T](origin: T, goal: T, distanceBetween: (T, T) => Double, neighbors: T => Iterable[T]): Option[Seq[T]] =
    apply(origin, goal, distanceBetween, distanceBetween(_:T, goal), neighbors)

  def apply[T](origin: T, goal: T, distanceBetween: (T, T) => Double, heuristic: T => Double, neighbors: T => Iterable[T]): Option[Seq[T]] = {
    var costAlongBest = Map(origin -> 0d).withDefaultValue(Double.PositiveInfinity)
    var bestGuessCost = Map(origin -> (costAlongBest(origin) + heuristic(origin)))
    implicit val ord = new Ordering[T] {  // implicit arg for the PrioritySet constructor
      def compare(x: T, y: T) = -(bestGuessCost(x) compare bestGuessCost(y))  // The negative here is because PriorityQueue returns items of the highest priority
    }

    var open = new PrioritySet[T]
    var closed = Set[T]()
    var cameFrom = Map[T, T]()

    open += origin
    while (open.nonEmpty) {
      val Some(current) = open.dequeue
      if (current == goal) {
        return Some(reconstructPath(cameFrom, List(goal)))
      }
      closed = closed + current
      neighbors(current).foreach { n =>
        val tentativeCost = costAlongBest(current) + distanceBetween(current, n)
        if (tentativeCost < costAlongBest(n) && !closed.contains(n)) {
          cameFrom = cameFrom + (n -> current)
          costAlongBest = costAlongBest + (n -> tentativeCost)
          bestGuessCost = bestGuessCost + (n -> (tentativeCost + heuristic(n)))
          open += n
        }
      }
    }
    None
  }

  @tailrec
  private def reconstructPath[T](cameFrom: Map[T, T], current: List[T]): List[T] = cameFrom.contains(current.head) match {
    case true => reconstructPath(cameFrom, cameFrom(current.head) :: current)
    case false => current
  }
}
