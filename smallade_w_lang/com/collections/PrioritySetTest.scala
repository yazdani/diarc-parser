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

import org.scalatest.{FunSpec, Matchers}
import org.scalatest.prop.GeneratorDrivenPropertyChecks

class PrioritySetTest extends FunSpec with Matchers with GeneratorDrivenPropertyChecks {
  describe("A Priority Set") {
    it ("should know its size") {
      val ps = PrioritySet[Double]()
      ps.size should be (0)
      ps -= 5d
      ps.size should be (0)
      ps += 5d
      ps.size should be (1)
    }

    it ("should contain things that have been added") {
      val ps = PrioritySet[Double]()
      forAll { (x: Double) =>
        ps += x
        ps.contains(x) should be (true)
      }
    }

    it ("should not contain things that have not been added") {
      val ps = PrioritySet[String]()
      forAll { (x: String) =>
        ps.contains(x) should be (false)
      }
    }

    it ("should not contain things that have been removed") {
      val ps = PrioritySet[Int]()
      ps += 5
      forAll { (x: Int) =>
        ps += x
        ps.contains(5) should be (true)
      }
      ps -= 5
      ps.contains(5) should be (false)
    }

    it ("should not contain duplicates") {
      forAll { (xs: List[String]) =>
        val ps = PrioritySet[String]()
        ps ++= xs
        ps ++= xs  // guarantee some duplicate adding
        ps.size should be (xs.toSet.size)
      }
    }

    it ("should dequeue in priority order") {
      forAll { (xs: List[Int]) =>
        val ps = PrioritySet[Int]()
        ps ++= xs
        if (xs.length > 0) {
          ps.dequeue should be (Some(xs.max))
        } else {
          ps.dequeue should be (None)
        }
      }
    }
  }
}