/*
 * Agent Development Environment (ADE)
 *
 * @version 1.0
 * @author M@
 *
 * Copyright 1997-2013 M@ Dunlap and HRILab (hrilab.org)
 * All rights reserved. Do not copy and use without permission.
 * For questions contact M@ at <matthew.dunlap+hrilab@gmail.com>
 */

package com.algorithms.unionfind;

/**
 * An implementation of Tarjan's Union Find algorithm, for details check out http://en.wikipedia.org/wiki/Union_find .
 *
 * Created by
 * User: M@
 * Date: 2013-02-27
 */
public class UnionFind {
  /**
   * Find and return the root of whatever tree contains x.
   *
   * @param x The subtree in question.
   * @return Whichever top level disjoint set contains x.
   */
  public static DisjointSetTree find(DisjointSetTree x) {
    if (x.getParent() == null || x.getParent() == x) {
      return x;
    }
    x.setParent(find(x.getParent()));
    return x.getParent();
  }

  /**
   * Insert one tree as a subtree of the other to take the union of the contained sets.
   *
   * @param x One of the sets.
   * @param y The other set.
   */
  public static void union(DisjointSetTree x, DisjointSetTree y) {
    DisjointSetTree xRoot = find(x);
    DisjointSetTree yRoot = find(y);
    if (xRoot == yRoot) return;

    int xRank = xRoot.getRank();
    int yRank = yRoot.getRank();
    if (xRank < yRank) {
      xRoot.setParent(yRoot);
    } else if (xRank > yRank) {
      yRoot.setParent(xRoot);
    } else {
      yRoot.setParent(xRoot);
      xRoot.setRank(xRoot.getRank() + 1);
    }
  }
}
