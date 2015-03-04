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
package com.algorithms.planning.samplingBased.demo

/**
 * This is a simple wrapper class to tell the visualizer how to draw your RRT.
 *
 * pointSize is the size you'd like the vertices in the tree to be
 * edgeWidth is the width you'd like the lines between the vertices to be
 */
case class RrtVisualizationParams(pointSize: Int, drawLines: Boolean) {
  def this(pointSize: Int) = this(pointSize, true)
}
