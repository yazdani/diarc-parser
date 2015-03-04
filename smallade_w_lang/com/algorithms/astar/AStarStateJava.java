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

import java.util.List;

/**
 * A nice general A* interface.
 *
 * @author M@ <matthew.dunlap+hrilab@gmail.com>
 */
public interface AStarStateJava {
  public double distance(AStarStateJava target);

  public List<AStarStateJava> neighbors();
}
