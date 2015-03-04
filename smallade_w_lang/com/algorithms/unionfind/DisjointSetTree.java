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
 * DisjointSetTrees are objects that can be union-find-ed.
 *
 * For details on Union Find check out http://en.wikipedia.org/wiki/Union_find
 *
 * Created by
 * User: M@
 * Date: 2013-02-27
 */
public interface DisjointSetTree {
  public DisjointSetTree getParent();
  public void setParent(DisjointSetTree newParent);
  public int getRank();
  public void setRank(int rank);
}
