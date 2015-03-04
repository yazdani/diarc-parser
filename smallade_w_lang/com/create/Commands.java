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
class Commands {
  static final int STRAIGHT = 0x7FFF;

  static final byte[] START  = {(byte)128};
  static final byte[] PASSIVE = START;
  static final byte[] CONTROL = {(byte)130};
  static final byte[] SAFE = {(byte)131};
  static final byte[] FULL = {(byte)132};
  static final byte[] SPOT = {(byte)134};
  static final byte[] COVER = {(byte)135};
  static final byte DEMO = (byte)136;  // 136 demoNumber
  static final byte DRIVE = (byte)137;  // 137 velHigh velLow radHigh radLow
  static final byte LOW_SIDE_DRIVERS = (byte)138;  // 138 bits
  static final byte LEDS = (byte)139;  // 139 ledBits powerColor powerIntensity
  static final byte SONG = (byte)140;  // 140 songNumber songLength note1 note1dur note2...
  static final byte PLAY = (byte)141;  // 141 songNumber
  static final byte SENSORS = (byte)142;  // 142 packetId
  static final byte[] COVER_AND_DOCK = {(byte)143};
  static final byte PWM_LD = (byte)144; // 144 ld2DutyCycle ld1DutyCycle ld0DutyCycle
  static final byte DRIVE_DIRECT = (byte)145; // 145 rightHigh rightLow leftHigh leftLow
  static final byte STREAM = (byte)148; // 148 numIds id id id id...
  static final byte QUERY_LIST = (byte)149; // 149 numIds id id id id...
  static final byte[] PAUSE_STREAM = {(byte)150, (byte)0};
  static final byte[] RESUME_STREAM = {(byte)150, (byte)1};
  // NOTE: 151 assumes special hardware connected to LD1, we don't currently have that hardware.  
  // static final byte SEND_IR = (byte)151;  // 151 message 
  static final byte SCRIPT = (byte)152; // 152 scriptLength opcode1 data1 opcode2 data2 ...
  static final byte[] PLAY_SCRIPT = {(byte)153};
  static final byte[] SHOW_SCRIPT = {(byte)154};
  static final byte WAIT_TIME = (byte)155;  // 151 duration
  static final byte WAIT_DISTANCE = (byte)156; // 156 distanceHigh distanceLow
  static final byte WAIT_ANGLE = (byte)157; // 157 angleHigh angleLow
  static final byte WAIT_EVENT = (byte)158;  // 151 eventId

  static final byte[] STOP = { FULL[0]
                              , DRIVE_DIRECT, 0, 0, 0, 0
                              , LOW_SIDE_DRIVERS, 0
                              , SAFE[0]
                            };
  static final byte[] SPIN_LEFT = {DRIVE, 0, 0, (byte)0xFF, (byte)0xFF};
  static final byte[] SPIN_RIGHT = {DRIVE, 0, 0, 0, 1};

  static final byte[] LOAD_STATUS_SONGS = {SONG, 0, 6, 79, 8, 84, 8, 88, 8, 91, 12, 88, 4, 91, 32
                                            , SONG, 1, 3, 69, 24, 61, 24, 52, 64
                                          };

  static final byte[] SING_READY = {PLAY, 0};
  static final byte[] SING_DRAMATIC = {PLAY, 1};
}
