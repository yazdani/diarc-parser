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

/**
 * IMPORTANT NOTE: I'm being lazy and have lots of memory, so I'm storing everything twice!  TODO: If RAM becomes more important than effort store everything only once!
 */
class PrioritySet[T]()(implicit ord: Ordering[T]) extends mutable.Set[T] with mutable.SetLike[T, PrioritySet[T]] with Serializable {
  var pq = new mutable.PriorityQueue[T]
  val set = mutable.Set[T]()

  def dequeue: Option[T] = set.size == 0 match {
    case true => None
    case false => {
      set -= pq.head
      Some(pq.dequeue)
    }
  }
  def contains(key: T) = set.contains(key)
  def iterator = set.iterator
  def +=(elem: T): this.type = {
    if (!set.contains(elem)) {
      set += elem
      pq += elem
    }
    this
  }
  def -=(elem: T): this.type = {
    if (set.contains(elem)) {
      set -= elem
      if (pq.head == elem) {
        pq.dequeue
      } else {
        pq = pq.filterNot(_ == elem)
      }
    }
    this
  }
  override def empty = new PrioritySet[T]
  override def foreach[U](f: T => U) = set.foreach(f)
  override def size = set.size
}

object PrioritySet {
  def apply[T](xs: T*)(implicit ord: Ordering[T]) = new PrioritySet[T]() ++ xs
}

