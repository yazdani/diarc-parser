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
package com.algorithms.pid;

/**
 * A speed manager for people who don't want a speed manager.
 *
 * @author M@ <matthew.dunlap+hrilab@gmail.com>
 */
public class ConstantMaxSpeed implements SpeedManager {
  double maxSpeed = Double.MAX_VALUE;

  public ConstantMaxSpeed(double max) {
    maxSpeed = max;
  }

  @Override
  public double getLimitedSpeed(double previousChange, double rawPid, double dt) {
    return maxSpeed * dt;
  }
}
