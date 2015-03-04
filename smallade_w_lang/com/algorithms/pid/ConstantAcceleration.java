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
 * This speed manager will accelerate with constant acceleration until it hits
 * its specified max, at which point it will stay constant.
 *
 * @author M@ <matthew.dunlap+hrilab@gmail.com>
 */
public class ConstantAcceleration implements SpeedManager {
  double maxSpeed;
  double acceleration;

  public ConstantAcceleration(double max, double acc) {
    maxSpeed = max;
    acceleration = acc;
  }

  @Override
  public double getLimitedSpeed(double previousChange, double rawPid, double dt) {
    double maxChange = previousChange + acceleration * dt;
    double minChange = previousChange - acceleration * dt;
    double limited = rawPid;

    if (limited > maxChange) {
      limited = maxChange;
    }
    if (limited < minChange) {
      limited = minChange;
    }

    if (limited > maxSpeed) {
      return maxSpeed;
    }
    if (limited < -maxSpeed) {
      return -maxSpeed;
    }
    return limited;
  }
}
