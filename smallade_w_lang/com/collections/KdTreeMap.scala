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

class KdTreeMap[K <: Dimensioned, V] protected (k: Int
                                                , dim: Int = 0
                                                , bucketLimit: Int = 50
                                                , initialBucket: Map[K, V] = Map[K, V]())
    extends mutable.Map[K, V] with mutable.MapLike[K, V, KdTreeMap[K, V]] with Serializable { 
  @transient protected val log = LogFactory.getLog(this.getClass)
  private val dimension = if (dim < k) dim else 0
  private var _size = initialBucket.size
  private var bucket: Map[K, V] = initialBucket
  private var leq: Option[KdTreeMap[K, V]] = None
  private var gt: Option[KdTreeMap[K, V]] = None
  private var median = Double.NaN

  if (k > 9) {
    log.warn("K-D Trees are known to perform poorly in high dimensional spaces!  Consider a different data structure.  Perhaps I could interest you in a hybrid spill tree?")
  }

  def nearestNeighbor(query: K): Option[K] = {
    if (bucket.size > 0) {  // if we're a leaf node
      bucket.minBy{case(k, _) => distance2(k, query)}._1
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

  def get(key: K): Option[V] = {
    if (bucket.contains(key)) {
      bucket.get(key)
    } else if (leqContains(key)) {
      leq.get.get(key)
    } else if (gtContains(key)) {
      gt.get.get(key)
    } else {
      None
    }
  }

  def iterator: Iterator[(K, V)] = bucket.iterator ++ leq.getOrElse(Nil).iterator ++ gt.getOrElse(Nil).iterator

  def +=(kv: (K, V)): this.type = {
    val (key, _) = kv
    if (leq.nonEmpty) {
      if (key.getDimension(dimension) <= median) {
        leq.get += kv
      } else {
        gt.get += kv
      }
      _size = leq.get.size + gt.get.size
      if (rebalanceNeeded) rebalanace
    } else {
      bucket = bucket + kv
      _size = bucket.size
      split
    }
    this
  }

  def -=(key: K): this.type = {
    if (bucket.contains(key)) {
      bucket = bucket - key
      _size -= 1
    } else if (leqContains(key)) {
      leq.get -= key
      _size -= 1
    } else if (gtContains(key)) {
      gt.get -= key
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

  private def distance2(a: K, b: K): Double = (0 until a.getDimensionality).map(i => math.pow(b.getDimension(i) - a.getDimension(i), 2)).sum
  private def distance2(aa: Option[K], bb: Option[K]): Double = (aa, bb) match {
    case (Some(a), Some(b)) => distance2(a, b)
    case _ => Double.PositiveInfinity
  }

  private def split {
    if (size > bucketLimit) {
      val sordid = bucket.keys.toList.sortWith(_.getDimension(dimension) < _.getDimension(dimension))
      median = sordid(bucket.size / 2).getDimension(dimension)
      val (a, b) = bucket.partition(_._1.getDimension(dimension) <= median)
      leq = new KdTreeMap(k, dimension+1, bucketLimit, a)
      gt = new KdTreeMap(k, dimension+1, bucketLimit, b)
      bucket = bucket.empty
      // split the children too, this can happen when the tree is rebalanced
      leq.get.split
      gt.get.split
    }
  }

  private def leqContains(key: K): Boolean = key.getDimension(dimension) <= median && leq.foldLeft(false)(_||_.contains(key))
  private def  gtContains(key: K): Boolean = key.getDimension(dimension) >  median &&  gt.foldLeft(false)(_||_.contains(key))

  override def empty = new KdTreeMap[K, V](k, dim, bucketLimit)
  override def size = _size

  override def foreach[U](f: ((K, V)) => U): Unit = {
    bucket.foreach(f)
    leq.foreach(_.foreach(f))
    gt.foreach(_.foreach(f))
  }
}

object KdTreeMap {
  def apply[K <: Dimensioned, V](k: Int, bucketLimit: Int = 50) = new KdTreeMap[K, V](k, bucketLimit=bucketLimit)
  def apply[K <: Dimensioned, V](items: (K, V)*) = items match {
    case (exemplar :: tail) => new KdTreeMap[K, V](exemplar._1.getDimensionality) ++ (exemplar :: tail)
  }
  implicit def optionifyKdTreeMap[K <: Dimensioned, V](k: KdTreeMap[K, V]): Option[KdTreeMap[K, V]] = Option(k)
}

