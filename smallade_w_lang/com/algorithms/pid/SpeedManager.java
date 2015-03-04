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
 * This is an interface used by PidControllers to limit the signals they output.
 *
 * @author M@ <matthew.dunlap+hrilab@gmail.com>
 */
public interface SpeedManager {
  public double getLimitedSpeed(double previousChange, double rawPid, double dt);
}
