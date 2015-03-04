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

import com.algorithms.planning.{Path, Planner, ProblemDefinition, StateContainer, StateSpace}
import org.apache.commons.logging.LogFactory
import scala.collection.JavaConversions.{asJavaIterable, mapAsJavaMap}
import scala.collection.parallel.ParSet

class BallTree[T](problem: ProblemDefinition[T]) extends Planner[T] {
  import BallTree._
  val forest = new BallForest(problem)
  forest.connectToTrees(problem.startState, true, false)
  problem.goal.goalSamples.foreach (forest.connectToTrees(_, false, true))
  var foundSolutions = problem.goal.goalSamples

  def plan: Option[Path[T]] = {
    while (problem.shouldGrow(forest)) {
      val sample = problem.stateSpace.stateSampler.sample
      (forest.containsPoint(sample), problem.stateSpace.validState(sample)) match {
        case (_, false) => forest.discontain(sample)
        case (true, _)  => log.trace("Rejecting boring sample " + sample)
        case (false, _) => {
          forest.connectToTrees(sample) 
          if (problem.goal.offerSolution(sample)) {
            foundSolutions = foundSolutions + sample 
            forest.flagGoal(sample)
          } 
        } 
      }
    }
    // search the forest for the tree that contains both beginning and end, search for the end state, and then plan and return a path
    forest.trees.find(t => t.containsStart && t.containsEnd).flatMap(
      t => t.balls.find{ case (pt, _) => foundSolutions.contains(pt) }
    ).flatMap{ case (goal, _) => problem.stateSpace.makePath(problem.startState, Some(goal), getStates) }
  }

  def getStates: StateContainer[T] = forest
}

object BallTree {
  val log = LogFactory.getLog("com.algorithms.planning.samplingBased.BallTree")

  class Tree[T](root: T, rootRad: Double, space: StateSpace[T], var containsStart: Boolean, var containsEnd: Boolean) extends Serializable {
    def this(root: T, space: StateSpace[T], containsStart: Boolean, containsEnd: Boolean) = this(root, Double.PositiveInfinity, space, containsStart, containsEnd)
    def this(root: T, space: StateSpace[T]) = this(root, space, false, false)
    import scala.collection.mutable

    val balls = mutable.Map.empty[T, (Double, Set[T])].withDefaultValue((Double.NaN, Set.empty[T]))
    balls.put(root, (rootRad, Set.empty[T]))

    def containsPoint(query: T): Boolean = balls.exists { case (point, (radius, _)) => space.distanceBetween(query, point) < radius }

    def nearestVolume(query: T): (T, Double) = balls.minBy {
      case (node, (rad, _)) => space.distanceBetween(query, node) - rad
    } match { case (node, (rad, _)) => (node, rad) } 

    def shrinkBall(center: T, newRad: Double): Unit = balls(center) match {
      case (oldRad, s) if newRad < oldRad => balls.put(center, (newRad, s))
      case _ => Unit
    }

    def shrinkBall(center: T, collision: T): Unit = shrinkBall(center, space.distanceBetween(center, collision))

    def discontain(point: T): Unit = balls.foreach { case (center, _) => shrinkBall(center, point) }

    def flagGoal(point: T): Unit = containsEnd |= balls.contains(point)

    def assimilate(other: Tree[T]): Tree[T] = {
      other.balls.foreach { case (k, (r, n)) =>
        if (balls.contains(k)) {
          val (curRad, neighbors) = balls(k)
          balls.put(k, (curRad.min(r), neighbors ++ n))
        } else {
          balls.put(k, (r, n))
        }
      }
      containsStart |= other.containsStart
      containsEnd   |= other.containsEnd
      this
    }

    def addBall(parent: T, center: T, radius: Double) {
      val (prad, pnei) = balls(parent)
      balls.put(parent, (prad, pnei + center))
      balls.put(center, (radius, Set(parent)))
    }

    def debugBalls: java.util.Map[T, Double] = mapAsJavaMap(balls.keys.map(k => k -> balls(k)._1).toMap)
  }

  class BallForest[T](@transient problem: ProblemDefinition[T]) extends StateContainer[T] {
    var trees: Set[Tree[T]] = Set.empty[Tree[T]]

    def connectToTrees(point: T, isStart: Boolean = false, isGoal: Boolean = false) = {
      // get the nearest ball in each tree
      val targetBalls = trees.map(tree => (tree, tree.nearestVolume(point)))
      // get the outer collision pairs on the edges from each nearest ball to the new ball
      val outerCollisionPairs = targetBalls.map{ case (tree, ball) => (tree, ball, problem.motionValidator.getOuterInvalidStatesBetween(ball._1, point)) }
      log.trace("Outer Collision Found: " + outerCollisionPairs.map(_._3))
      // split the new edges into those that had collisions and those that didn't
      val (connections, shrinks) = outerCollisionPairs.partition(_._3.isEmpty)
      // set the radius of the new ball to the minimum outgoing edge length
      val ptRad = if (shrinks.isEmpty) Double.PositiveInfinity else shrinks.map {
        case (_, _, Some((_, last))) => stateSpace.distanceBetween(point, last)  // TODO: consider adding a delta term here
        case _ => Double.PositiveInfinity  // NOTE: this can't happen, it's just here to prevent the warning
      }.min
      // shrink the balls that couldn't reach the new point
      shrinks.foreach { case (tree, (center, _), Some((first, _))) => tree.shrinkBall(center, first) }
      // add the new ball to all the trees to which it successfully connected
      connections.foreach { case (tree, (parent, _), _) => tree.addBall(parent, point, ptRad) }

      trees = shrinks.map(_._1) + joinTrees(connections.map(_._1) + new Tree[T](point, ptRad, problem.stateSpace, isStart, isGoal))
    }

    def containsPoint(query: T): Boolean = trees.exists(_.containsPoint(query))

    def discontain(point: T): Unit = trees.foreach(_.discontain(point))

    def flagGoal(point: T): Unit = trees.foreach(_.flagGoal(point))

    def joinTrees(ts: Iterable[Tree[T]]): Tree[T] =  ts.reduce(_.assimilate(_))

    def addState(state: T) {}
    def connectStates(orig: T, dest: T) {}
    def addTree(tree: Tree[T]) = trees = trees + tree
    def getConnections(query: T): Iterable[T] = trees.foldLeft(Iterable.empty[T])(_ ++ _.balls(query)._2)
    def states: Iterable[T] = trees.foldLeft(Iterable.empty[T])(_ ++ _.balls.keySet)
    def balls: Iterable[(T, Double)] = trees.flatMap(_.balls.map{ case (pt, (r, _)) => (pt, r) } )
    def threadsafe: Boolean = false
    def stateSpace = problem.stateSpace

    def debugStates: java.lang.Iterable[T] = states
    def debugTrees: java.lang.Iterable[Tree[T]] = trees.toIterable
  }
  
}

