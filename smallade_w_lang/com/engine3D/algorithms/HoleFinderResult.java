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
package com.engine3D.algorithms;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 * The results of finding a hole in a pointcloud.
 *
 * @author M@ <matthew.dunlap+hrilab@gmail.com>
 */
public class HoleFinderResult {
  public Point3d center;
  public Vector3d normal;
  public int pixelCount;
}
