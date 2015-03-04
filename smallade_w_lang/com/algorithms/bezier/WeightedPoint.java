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

package com.algorithms.bezier;

import java.io.Serializable;

/**
 * Created by
 * User: M@
 * Date: 2013-02-22
 */
public class WeightedPoint implements Serializable {
  public double[] point;
  public double weight = 1;

  public WeightedPoint() {}
  public WeightedPoint(double[] p) { point = p; }
}
