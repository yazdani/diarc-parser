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
package com.collections

import collection.mutable
import org.apache.commons.logging.LogFactory
import scala.language.implicitConversions

import KdTree._

class KdTree[T <: Dimensioned] private (k: Int, dim: Int = 0, bucketLimit: Int = 50, initialBucket: Set[T] = Set[T]())
    extends mutable.Set[T] with mutable.SetLike[T, KdTree[T]] with Serializable {
  @transient protected val log = LogFactory.getLog(this.getClass)
  private val dimension = if (dim < k) dim else 0
  private var _size = initialBucket.size
  private var bucket: Set[T] = initialBucket
  private var leq: Option[KdTree[T]] = None
  private var gt: Option[KdTree[T]] = None
  private var median = Double.NaN;

  if (k > 9) {
    log.warn("K-D Trees are known to perform poorly in high dimensional spaces!  Consider a different data structure.  Perhaps I could interest you in a hybrid spill tree?")
  }

  def contains(elem: T): Boolean = bucket.contains(elem) || leqContains(elem) || gtContains(elem)

  def nearestNeighbor(query: T): Option[T] = {
    if (bucket.size > 0) {  // if we're a leaf node
      bucket.minBy(distance2(_, query))
    } else if (leq.nonEmpty) {  // else if we're an interior node
      if (query.getDimension(dimension) <= median) {  // if it ought to be to the left of us
        val res = leq.get.nearestNeighbor(query)
        if (distance2(res, query) < math.pow(median - query.getDimension(dimension), 2)) {  // if we can short circuit the search
          res
        } else {
          List(res, gt.get.nearestNeighbor(query)).minBy(distance2(_, query))
        }
      } else {  // if it ought to be to the right
        val res = gt.get.nearestNeighbor(query)
        if (distance2(res, query) < math.pow(query.getDimension(dimension) - median, 2)) {  // if we can short circuit the search
          res
        } else {
          List(res, leq.get.nearestNeighbor(query)).minBy(distance2(_, query))
        }
      }
    } else {  // if the bucket is empty and we have no left or right children
      None
    }
  }

  // def kNearestNeighbors(query: T
  //                       , neighborCount: Int
  //                       , soFar: List[T] = List[T]()
  //                       , exceptions: Set[T] = Set[T]()): List[T] = Nil  // TODO: write me

  def iterator: Iterator[T] = bucket.iterator ++ leq.getOrElse(Nil).iterator ++ gt.getOrElse(Nil).iterator

  def +=(elem: T): this.type = {
    if (leq.nonEmpty) {
      if (elem.getDimension(dimension) <= median) {
        leq.get += elem
      } else {
        gt.get += elem
      }
      _size = leq.get.size + gt.get.size
      if (rebalanceNeeded) rebalanace
    } else {
      bucket = bucket + elem
      _size = bucket.size
      split
    }
    this
  }

  def -=(elem: T): this.type = {
    if (bucket.contains(elem)) {
      bucket = bucket - elem
      _size -= 1
    } else if (leqContains(elem)) {
      leq.get -= elem
      _size -= 1
    } else if (gtContains(elem)) {
      gt.get -= elem
      _size -= 1
    }
    this
  }
  
  private def rebalanceNeeded: Boolean = leq.nonEmpty && {
    val bal = leq.get.size.toDouble / size
    bal > 1 - BALANCE_BOUNDARY || BALANCE_BOUNDARY > bal
  }

  private def rebalanace = (leq, gt) match {
    case (Some(l), Some(g)) => {
      log.debug("rebalancing " + size)
      bucket = bucket ++ l.iterator ++ g.iterator
      leq = None
      gt = None
      split
    }
    case _ => log.debug("No rebalanace needed.")
  }

  private def distance2(a: T, b: T): Double = (0 until a.getDimensionality).map(i => math.pow(b.getDimension(i) - a.getDimension(i), 2)).sum
  private def distance2(aa: Option[T], bb: Option[T]): Double = (aa, bb) match {
    case (Some(a), Some(b)) => distance2(a, b)
    case _ => Double.PositiveInfinity
  }

  private def split {
    if (size > bucketLimit) {
      val sordid = bucket.toList.sortWith(_.getDimension(dimension) < _.getDimension(dimension))
      median = sordid(bucket.size / 2).getDimension(dimension)
      val (a, b) = bucket.partition(_.getDimension(dimension) <= median)
      leq = new KdTree(k, dimension+1, bucketLimit, a)
      gt = new KdTree(k, dimension+1, bucketLimit, b)
      bucket = bucket.empty
      // split the children too, this can happen when the tree is rebalanced
      leq.get.split
      gt.get.split
    }
  }

  private def leqContains(elem: T): Boolean = elem.getDimension(dimension) <= median && leq.foldLeft(false)(_||_.contains(elem))
  private def  gtContains(elem: T): Boolean = elem.getDimension(dimension) >  median &&  gt.foldLeft(false)(_||_.contains(elem))

  override def empty = new KdTree[T](k, dim, bucketLimit)

  override def size = _size

  override def foreach[U](f: T => U): Unit = {
    bucket.foreach(f)
    leq.foreach(_.foreach(f))
    gt.foreach(_.foreach(f))
  }
}

object KdTree {
  val BALANCE_BOUNDARY = .25
  trait Dimensioned {
    def getDimension(i: Int): Double
    def getDimensionality: Int
  }

  def apply[T <: Dimensioned](k: Int, bucketLimit: Int = 50) = new KdTree[T](k, bucketLimit=bucketLimit)
  def apply[T <: Dimensioned](ts: T*) = ts match {
    case (exemplar :: tail) => new KdTree[T](exemplar.getDimensionality) ++ (exemplar :: tail)
  }

  implicit def optionifyT[T <: Dimensioned](t: T): Option[T] = Option(t)
  implicit def optionifyKdTree[T <: Dimensioned](k: KdTree[T]): Option[KdTree[T]] = Option(k)
}

