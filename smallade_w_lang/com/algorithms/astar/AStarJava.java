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
package com.algorithms.astar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The A* heuristically directed search algorithm.
 *
 * I didn't write this, I just inherited it. --M@
 */
public class AStarJava {
  static Log log = LogFactory.getLog(AStarJava.class);

  public static ArrayList<AStarStateJava> aStar(AStarStateJava start, AStarStateJava stop) {
    HashMap<AStarStateJava, Double> g_score = new HashMap<AStarStateJava, Double>();
    HashMap<AStarStateJava, Double> h_score = new HashMap<AStarStateJava, Double>();
    HashMap<AStarStateJava, Double> f_score = new HashMap<AStarStateJava, Double>();
    HashMap<AStarStateJava, AStarStateJava> came_from = new HashMap<AStarStateJava, AStarStateJava>();

    g_score.put(start, 0.0);
    h_score.put(start, start.distance(stop));
    f_score.put(start, h_score.get(start));

    HashSet<AStarStateJava> openSet = new HashSet<AStarStateJava>();
    HashSet<AStarStateJava> closedSet = new HashSet<AStarStateJava>();
    openSet.add(start);

    while (!openSet.isEmpty()) {
      log.debug("Starting round.");
      log.trace("Goal " + stop);

      // This search hurts the overall timebound, if you're going to actually use this algorithm please replace it with a heap
      double min = Double.MAX_VALUE;
      AStarStateJava x = null;
      for (AStarStateJava w : openSet) {
        Double fScore = f_score.get(w);
        if (fScore != null && fScore < min) {
          min = fScore;
          x = w;
        }
      }
      log.debug("Min f of " + min + " found for object " + x);

      if (x == null) {
        return null;
      }
      if (x.equals(stop)) {
        log.debug("At goal");
        return reconstruct_path(came_from, x);
      }
      openSet.remove(x);
      closedSet.add(x);

      for (AStarStateJava y : x.neighbors()) {
        if (closedSet.contains(y)) {
          continue;
        }

        double tentative_g_score = g_score.get(x) + .1;
        boolean tentative_is_better = false;
        if (!openSet.contains(y)) {
          openSet.add(y);
          tentative_is_better = true;
        } else if (tentative_g_score < g_score.get(y)) {
          tentative_is_better = true;
        }

        if (tentative_is_better) {
          came_from.put(y, x);
          g_score.put(y, tentative_g_score);
          h_score.put(y, y.distance(stop));
          f_score.put(y, g_score.get(y) + h_score.get(y));
        }
      }
    }
    return null;
  }

  private static ArrayList<AStarStateJava> reconstruct_path(HashMap<AStarStateJava, AStarStateJava> came_from, AStarStateJava current_node) {
    if (came_from.containsKey(current_node)) {
      ArrayList<AStarStateJava> p = reconstruct_path(came_from, came_from.get(current_node));
      p.add(current_node);
      return p;
    } else {
      ArrayList<AStarStateJava> p = new ArrayList<AStarStateJava>();
      p.add(current_node);
      return p;
    }
  }
}
