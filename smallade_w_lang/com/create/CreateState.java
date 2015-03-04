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
package com.create;

/**
 * @author M@ <matthew.dunlap+hrilab@gmail.com>
 */
class CreateState {
  // 7
  Boolean casterDropped = null;
  Boolean leftWheelDropped = null;
  Boolean rightWheelDropped = null;
  Boolean leftBumperPressed = null;
  Boolean rightBumperPressed = null;

  // 8
  Boolean wallDetected = null;

  // 9
  Boolean cliffDetectedLeft = null;

  // 10
  Boolean cliffDetectedFrontLeft = null;

  // 11
  Boolean cliffDetectedFrontRight = null;

  // 12
  Boolean cliffDetectedRight = null;

  // 13
  Boolean virtualWallDetected = null;  // ??!?!?

  // 14
  Boolean leftWheelOverCurrent = null;
  Boolean rightWheelOverCurrent = null;
  Boolean ld2 = null;  // 1.5A
  Boolean ld1 = null;  // 0.5A
  Boolean ld0 = null;  // 0.5A

  // 17  (15 and 16 are NO-OPs)
  Integer byteFromIR = null;

  // 18
  Boolean playPressed = null;
  Boolean nextPressed = null;

  // 19
  Integer distanceIncrement = null;  //mm

  // 20
  Integer angleIncrement = null;  //deg

  // 21
  ChargingState chargingState = ChargingState.UNKNOWN;

  enum ChargingState {
    NOT_CHARGING
    , RECONDITIONING
    , FULL
    , TRICKLE
    , WAITING
    , CHARGING_FAULT
    , UNKNOWN
  }

  // 22
  Integer voltage = null;  //mV

  // 23
  Integer current = null;  //mA

  // 24
  Integer batteryTemperature = null;  //C

  // 25
  Integer batteryCharge = null;  //mAh

  // 26
  Integer batteryCapacity = null;  //mAh

  // 27
  Integer wallSensorSignalStrength = null;

  // 28
  Integer leftCliffDetectorSignalStrength = null;

  // 29
  Integer leftFrontCliffDetectorSignalStrength = null;

  // 30
  Integer rightFrontCliffDetectorSignalStrength = null;

  // 31
  Integer rightCliffDetectorSignalStrength = null;

  // 32
  Boolean deviceDetectHigh = null;
  Boolean digitalInput3High = null;
  Boolean digitalInput2High = null;
  Boolean digitalInput1High = null;
  Boolean digitalInput0High = null;

  // 33
  Integer analogInputSignal = null;

  // 34
  Boolean chargingFromBase = null;
  Boolean chargingFromCable = null;

  // 35
  RobotMode currentMode = RobotMode.UNKNOWN;

  enum RobotMode {
    OFF
    , PASSIVE
    , SAFE
    , FULL_CONTROL
    , UNKNOWN
  }

  // 36
  Integer currentSong = null;

  // 37
  Boolean songPlaying = null;

  // 38
  Integer streamPacketsCount = null;

  // 39
  Integer requestedVelocity = null;  //mm/s

  // 40
  Integer requestedRadius = null;  //mm

  // 41
  Integer requestedRightVelocity = null;  //mm/s

  // 42
  Integer requestedLeftVelocity = null;  //mm/s
}