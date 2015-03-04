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
 * A generic PID Controller.
 *
 * @author M@ <matthew.dunlap+hrilab@gmail.com>
 */
public class PidController {
  double kp = 0;
  double ki = 0;
  double kd = 0;
  double integralAccumulator = 0;
  double prevError = 0;
  double prevResult = 0;
  SpeedManager speedManager;

  public PidController() {
  }

  public PidController(double p, double i, double d) {
    kp = p;
    ki = i;
    kd = d;
  }

  /**
   * Calculate the control signal and update the controller's state.
   *
   * @param setPoint The goal for whatever you're controlling.
   * @param position The current location of what you're controlling.
   * @param dt       The amount of time that has passed since the last call to
   *                 update.  Measured in seconds.
   * @return The control signal to pass to whatever you're controlling.
   */
  public double update(double setPoint, double position, double dt) {
    if (dt <= 0) {
      // if no time has elapsed the control signal can't change
      return prevResult;
    }
    if (Double.isNaN(setPoint) || Double.isNaN(position)) {
      return 0;
    }

    double error = setPoint - position;
    double dErrorDt = (error - prevError) / dt;
    integralAccumulator += ki * error * dt;  // using ki here instead of on the next line since these constants may change over the course of a run
    double result = dt * (kp * error + integralAccumulator + kd * dErrorDt);

    prevError = error;
    prevResult = speedManager == null ? result : speedManager.getLimitedSpeed(prevResult, result, dt);
    return prevResult;
  }

  public void setKp(double p) {
    kp = p;
  }

  public void setKi(double i) {
    ki = i;
  }

  public void setKd(double d) {
    kd = d;
  }

  /**
   * FOR EMERGENCIES (COLLISIONS) ONLY
   */
  public void hardReset() {
    integralAccumulator = 0;
    prevError = 0;
    prevResult = 0;
  }

  public void setManipulatedVariableMax(double speed) {
    speedManager = new ConstantMaxSpeed(speed);
  }

  public void setSpeedManager(SpeedManager manager) {
    speedManager = manager;
  }
}
