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

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.GeneratorDrivenPropertyChecks

class KdTreeTest extends FunSpec with Matchers with GeneratorDrivenPropertyChecks {
  import KdTreeTest.DD

  describe("A k-d tree") {
    it ("should know its size") {
      val babyTree = KdTree[DD](2)
      babyTree.size should be (0)
      babyTree += DD(5d, 7.5)
      babyTree.size should be (1)
      babyTree += DD(5d, 7.5)
      babyTree.size should be (1)

      forAll { (xs: List[Double], ys: List[Double]) =>
        val tree = KdTree[DD](2, 10)
        val dSet = xs.zip(ys).map(p => DD(p._1, p._2)).toSet  // toSet removes the duplicates
        dSet.foreach(tree += _)
        tree.size should be (dSet.size)
      }
    }

    it ("should contain things that have been added") {
      val tree = KdTree[DD](2, 10)
      forAll { (x: Double, y: Double) =>
        val dd = DD(x, y)
        tree += dd
        tree.contains(dd) should be (true)
      }
    }

    it ("should not contain things that have not been added") {
      val tree = KdTree[DD](2)
      forAll { (x: Double, y: Double) =>
        tree.contains(DD(x, y)) should be (false)
      }
    }

    it ("should not contain things that have been removed") {
      val tree = KdTree[DD](2, 10)
      val pie = DD(math.Pi, math.Pi)
      tree += pie

      // add some junk to force the pie into a sub-tree instead of the bucket
      forAll { (x: Double, y: Double) =>
        tree += DD(x, y)
        tree.contains(pie) should be (true)
      }

      val size = tree.size
      tree -= pie
      tree.contains(pie) should be (false)
      tree.size should be (size - 1)
    }

    it ("should return all points as their own nearest neighbor") {
      val tree = KdTree[DD](2, 10)
      forAll { (x: Double, y: Double) =>
        val dd = DD(x, y)
        tree += dd
        tree.nearestNeighbor(dd) should be (Some(dd))
      }
    }

    it ("should only return None as a nearest neighbor if the tree is empty") {
      val tree = KdTree[DD](2)
      forAll { (x: Double, y: Double) =>
        tree.nearestNeighbor(DD(x, y)) should be (None)
      }
      tree += DD(math.Pi, math.Pi)
      forAll { (x: Double, y: Double) =>
        tree.nearestNeighbor(DD(x, y)) should not be (None)
      }
    }

    it ("should only return the neighbors that are actually nearest") {
      val tree = KdTree[DD](2, 10)
      val bound = math.sqrt(Double.MaxValue / 2)  // bonus points if you can figure out why I picked this value
      val g1 = for (d <- Gen.choose(-bound, bound)) yield d
      val g2 = for (d <- Gen.choose(-bound, bound)) yield d
      val g3 = for (d <- Gen.choose(-bound, bound)) yield d
      val g4 = for (d <- Gen.choose(-bound, bound)) yield d

      // populate the tree
      forAll (g1, g2) { (x: Double, y: Double) =>
        tree += DD(x, y)
        tree.isEmpty should be (false)
      }
      // query for lots of neighbors
      forAll (g3, g4) { (x: Double, y: Double) =>
        val q = DD(x, y)
        val nn = tree.nearestNeighbor(q)
        nn should be (Some(tree.minBy(_.distance(q))))
      }
    }
    // TODO: instrument getDimension below to allow for testing that the k-d tree is actually faster than linear search
    // TODO: test auto-balancing
  }

  describe("A k-d tree map") {
    it ("should know its size") {
      val babyTree = KdTreeMap[DD, Double](2)
      babyTree.size should be (0)
      babyTree += ((DD(5d, 7.5), 0d))
      babyTree.size should be (1)

      forAll { (xs: List[Double], ys: List[Double], value: Double) =>
        val treeMap = KdTreeMap[DD, Double](2, 10)
        val dSet = xs.zip(ys).map(p => DD(p._1, p._2)).toSet  // toSet removes the duplicates
        dSet.foreach(dd => treeMap += ((dd, value)))
        treeMap.size should be (dSet.size)
      }
    }

    it ("should contain things that have been added") {
      val treeMap = KdTreeMap[DD, Double](2, 10)
      forAll { (x: Double, y: Double, value: Double) =>
        val dd = DD(x, y)
        treeMap += ((dd, value))
        treeMap.contains(dd) should be (true)
        treeMap(dd) should be (value)
      }
    }

    it ("should not contain things that have not been added") {
      val treeMap = KdTreeMap[DD, Double](2)
      forAll { (x: Double, y: Double) =>
        treeMap.contains(DD(x, y)) should be (false)
      }
    }

    it ("should not contain things that have been removed") {
      val treeMap = KdTreeMap[DD, Double](2, 10)
      val pie = DD(math.Pi, math.Pi)
      treeMap += ((pie, math.Pi))

      // add some junk to force the pie into a sub-treeMap instead of the bucket
      forAll { (x: Double, y: Double, value: Double) =>
        treeMap += ((DD(x, y), value))
        treeMap.contains(pie) should be (true)
      }

      val size = treeMap.size
      treeMap -= pie
      treeMap.contains(pie) should be (false)
      treeMap.size should be (size - 1)
    }

    it ("should return all points as their own nearest neighbor") {
      val treeMap = KdTreeMap[DD, Double](2, 10)
      forAll { (x: Double, y: Double, value: Double) =>
        val dd = DD(x, y)
        treeMap += ((dd, value))
        treeMap.nearestNeighbor(dd) should be (Some(dd))
      }
    }

    it ("should only return None as a nearest neighbor if the treeMap is empty") {
      val treeMap = KdTreeMap[DD, Double](2)
      forAll { (x: Double, y: Double) =>
        treeMap.nearestNeighbor(DD(x, y)) should be (None)
      }
      treeMap += ((DD(math.Pi, math.Pi), math.Pi))
      forAll { (x: Double, y: Double) =>
        treeMap.nearestNeighbor(DD(x, y)) should not be (None)
      }
    }

    it ("should only return the neighbors that are actually nearest") {
      val treeMap = KdTreeMap[DD, Double](2, 10)
      val bound = math.sqrt(Double.MaxValue / 2)  // bonus points if you can figure out why I picked this value
      val g1 = for (d <- Gen.choose(-bound, bound)) yield d
      val g2 = for (d <- Gen.choose(-bound, bound)) yield d
      val g3 = for (d <- Gen.choose(-bound, bound)) yield d
      val g4 = for (d <- Gen.choose(-bound, bound)) yield d
      val g5 = for (d <- Gen.choose(-bound, bound)) yield d

      // populate the treeMap
      forAll (g1, g2, g5) { (x: Double, y: Double, value: Double) =>
        treeMap += ((DD(x, y), value))
        treeMap.isEmpty should be (false)
      }
      // query for lots of neighbors
      forAll (g3, g4) { (x: Double, y: Double) =>
        val q = DD(x, y)
        val nn = treeMap.nearestNeighbor(q)
        nn should be (Some(treeMap.minBy(_._1.distance(q))._1))
      }
    }

    it ("should follow standard adding to a map semantics") {
      val treeMap = KdTreeMap[DD, Double](2, 10)
      forAll { (x: Double, y: Double, v1: Double, v2: Double) =>
        val dd = DD(x, y)
        val expectedSize = treeMap.size + (if (treeMap.contains(dd)) 0 else 1)

        // set a value and make sure the size is updated
        treeMap += ((dd, v1))
        treeMap.size should be (expectedSize)
        treeMap(dd) should be (v1)

        // change the value, make sure the size is not updated and that the new value is returned
        treeMap += ((dd, v2))
        treeMap.size should be (expectedSize)
        treeMap(dd) should be (v2)
      }
    }
  }
}

object KdTreeTest {
  case class DD(x: Double, y: Double) extends KdTree.Dimensioned {
    val getDimensionality = 2
    def getDimension(i: Int) = if (i == 0) x else y

    def distance(other: DD) = math.pow(x - other.x, 2) + math.pow(y - other.y, 2)
  }
}
