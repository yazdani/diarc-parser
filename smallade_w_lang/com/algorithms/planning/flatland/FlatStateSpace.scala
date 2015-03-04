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

import com.algorithms.astar.AStar
import com.algorithms.planning.{Goal, Path, Planner, StateContainer, StateSampler, StateSpace, StateValidityChecker}
import com.collections.KdTree
import scala.collection.JavaConversions._
import scala.collection.SeqLike
import scala.util.Random

import FlatStateSpace._
import KdTree.Dimensioned

class FlatStateSpace(@transient validityChecker: Option[StateValidityChecker[Point]] = None
                     , seed: Long = Long.MinValue) extends StateSpace[Point] {
  def this(validityChecker: StateValidityChecker[Point], seed: Long) = this(Some(validityChecker), seed)
  def this(validityChecker: StateValidityChecker[Point]) = this(Some(validityChecker), Long.MinValue)
  def this() = this(None, Long.MinValue)

  val dimensionality = pointDimensionality

  @transient val random = if (seed == Long.MinValue) new Random else new Random(seed)

  @transient val stateSampler = new StateSampler[Point] {
    def sample: Point = Point(random.nextDouble, random.nextDouble)
    def sampleValid: Point = validityChecker match {
      case Some(vc) => Iterator.continually(sample).find(vc.isValid(_)).get
      case _ => sample
    }
  }
  
  def distanceBetween(a: Point, b: Point): Double = a.distanceTo(b)

  def makePath(orig: Point, dest: Option[Point], container: StateContainer[Point]): Option[Path[Point]] = dest match {
    case None => None
    case Some(destination) => AStar[Point](orig
                                           , destination
                                           , distanceBetween(_, _)
                                           , container.getConnections(_)
                                           ).map(FlatPath(_))
  }

  def makeStateContainer: StateContainer[Point] = new KdContainer[Point](this)

  def nearestNeighbor(query: Point, container: StateContainer[Point]): Option[Point] = container.nearestNeighbor(query)

  def pointAlongPath(from: Point, to: Point, distance: Double): Point = {
    val p = (distance / from.distanceTo(to)).min(1d)
    Point((to.x - from.x) * p + from.x, (to.y - from.y) * p + from.y)
  }

  def projectToUnitDistance(d: Double): Double = d

  def validState(state: Point): Boolean = validityChecker match {
    case Some(vc) => vc.isValid(state)
    case _ => true
  }

  def projectToUnit2D(point: Point): (Double, Double) = (point.x, point.y)

  def projectToUnit3D(point: Point): (Double, Double, Double) = (point.x, point.y, 0d)

  def makeState(x: Double, y: Double): Point = Point(x, y)

  def makeState(coords: Seq[Double], p: Boolean): Point = coords match {
    case Seq(x, y) => Point(x, y)
    case _ => null 
  }

  def unmakeState(pt: Point): Seq[Double] = Seq(pt.x, pt.y)
}

object FlatStateSpace {
  val pointDimensionality = 2

  case class Point(x: Double, y: Double) extends Dimensioned {
    def getDimension(i: Int) = if (i == 0) x else y
    def distanceTo(other: Point): Double = math.sqrt(math.pow(other.x - x, 2) + math.pow(other.y - y, 2))
    val getDimensionality = pointDimensionality
  }

  class FlatGoal(center: Point, radius: Double) extends Goal[Point] {
    var currentSolution: Option[Point] = None
    def getSolution = currentSolution
    val goalSamples = Set(center)

    def offerSolution(state: Point): Boolean = {
      if (center.distanceTo(state) < radius) {
        currentSolution = Some(state)
        true
      }
      false
    }
  }

  case class FlatPath(waypoints: Seq[Point]) extends Path[Point] {
    val length = waypoints.sliding(2).map{ case Seq(a, b) => a.distanceTo(b) }.sum
    def stateAt(p: Double): Point = waypoints((p * (waypoints.length -1)).toInt)
  }
}

