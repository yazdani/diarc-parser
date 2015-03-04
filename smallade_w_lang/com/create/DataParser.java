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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author M@ <matthew.dunlap+hrilab@gmail.com>
 */
class DataParser {
  static final byte HEADER = 19;

  static Log log = LogFactory.getLog(DataParser.class);

  static int unsign(byte b) {
    return b & 0xFF;
  }

  static int signedJoin(byte high, byte low) {
    return high << 8 | unsign(low);
  }

  static int unsignedJoin(byte high, byte low) {
    return unsign(high) << 8 | unsign(low);
  }

  static boolean iff(int x) {
    return x == 0 ? false : true;
  }

  static CreateState parsePacket(byte[] packet, byte[] query, int startingIndex, CreateState result) {
    if (result == null) {
      result = new CreateState();
    }
    if (!verifyChecksum(packet, query, startingIndex)) {
      return null;
    }
    int i = startingIndex + 2;  // location of the first packetID in the compound packet
    for (byte q : query) {
      if (q == packet[i++]) {
        parsePacket(packet, q, i, result);
        i += packetDataSize(q);
      } else {
        log.error("The data is out of sync... perhaps the query just changed?");
        return null;
      }
    }
    return result;
  }

  static CreateState parsePacket(byte[] packet, int packetTypeId, int startingIndex) {
    return parsePacket(packet, packetTypeId, startingIndex, new CreateState());
  }

  static CreateState parsePacket(byte[] packet, int packetTypeId, int startingIndex, CreateState result) {
    if (packet.length < startingIndex + packetDataSize(packetTypeId)) {
      return null;
    }
    switch (packetTypeId) {
      case  0:  parsePacket0(packet, startingIndex, result);  return result;
      case  1:  parsePacket1(packet, startingIndex, result);  return result;
      case  2:  parsePacket2(packet, startingIndex, result);  return result;
      case  3:  parsePacket3(packet, startingIndex, result);  return result;
      case  4:  parsePacket4(packet, startingIndex, result);  return result;
      case  5:  parsePacket5(packet, startingIndex, result);  return result;
      case  6:  parsePacket6(packet, startingIndex, result);  return result;
      case  7:  parsePacket7(packet, startingIndex, result);  return result;
      case  8:  parsePacket8(packet, startingIndex, result);  return result;
      case  9:  parsePacket9(packet, startingIndex, result);  return result;

      case 10:  parsePacket10(packet, startingIndex, result);  return result;
      case 11:  parsePacket11(packet, startingIndex, result);  return result;
      case 12:  parsePacket12(packet, startingIndex, result);  return result;
      case 13:  parsePacket13(packet, startingIndex, result);  return result;
      case 14:  parsePacket14(packet, startingIndex, result);  return result;
      case 15:  return null;  // 15 and 16 are never used
      case 16:  return null;
      case 17:  parsePacket17(packet, startingIndex, result);  return result;
      case 18:  parsePacket18(packet, startingIndex, result);  return result;
      case 19:  parsePacket19(packet, startingIndex, result);  return result;

      case 20:  parsePacket20(packet, startingIndex, result);  return result;
      case 21:  parsePacket21(packet, startingIndex, result);  return result;
      case 22:  parsePacket22(packet, startingIndex, result);  return result;
      case 23:  parsePacket23(packet, startingIndex, result);  return result;
      case 24:  parsePacket24(packet, startingIndex, result);  return result;
      case 25:  parsePacket25(packet, startingIndex, result);  return result;
      case 26:  parsePacket26(packet, startingIndex, result);  return result;
      case 27:  parsePacket27(packet, startingIndex, result);  return result;
      case 28:  parsePacket28(packet, startingIndex, result);  return result;
      case 29:  parsePacket29(packet, startingIndex, result);  return result;

      case 30:  parsePacket30(packet, startingIndex, result);  return result;
      case 31:  parsePacket31(packet, startingIndex, result);  return result;
      case 32:  parsePacket32(packet, startingIndex, result);  return result;
      case 33:  parsePacket33(packet, startingIndex, result);  return result;
      case 34:  parsePacket34(packet, startingIndex, result);  return result;
      case 35:  parsePacket35(packet, startingIndex, result);  return result;
      case 36:  parsePacket36(packet, startingIndex, result);  return result;
      case 37:  parsePacket37(packet, startingIndex, result);  return result;
      case 38:  parsePacket38(packet, startingIndex, result);  return result;
      case 39:  parsePacket39(packet, startingIndex, result);  return result;

      case 40:  parsePacket40(packet, startingIndex, result);  return result;
      case 41:  parsePacket41(packet, startingIndex, result);  return result;
      case 42:  parsePacket42(packet, startingIndex, result);  return result;
    }
    return null;
  }

  static void parsePacket0(byte[] packet, int startingIndex, CreateState result) {
    parsePacket1(packet, startingIndex, result);
    startingIndex += packetDataSize(1);
    parsePacket2(packet, startingIndex, result);
    startingIndex += packetDataSize(2);
    parsePacket3(packet, startingIndex, result);
  }

  static void parsePacket1(byte[] packet, int startingIndex, CreateState result) {
    parsePacket7(packet, startingIndex, result);
    startingIndex++;
    parsePacket8(packet, startingIndex, result);
    startingIndex++;
    parsePacket9(packet, startingIndex, result);
    startingIndex++;
    parsePacket10(packet, startingIndex, result);
    startingIndex++;
    parsePacket11(packet, startingIndex, result);
    startingIndex++;
    parsePacket12(packet, startingIndex, result);
    startingIndex++;
    parsePacket13(packet, startingIndex, result);
    startingIndex++;
    parsePacket14(packet, startingIndex, result);
  }

  static void parsePacket2(byte[] packet, int startingIndex, CreateState result) {
    parsePacket17(packet, startingIndex, result);
    startingIndex += packetDataSize(17);
    parsePacket18(packet, startingIndex, result);
    startingIndex += packetDataSize(18);
    parsePacket19(packet, startingIndex, result);
    startingIndex += packetDataSize(19);
    parsePacket20(packet, startingIndex, result);
  }

  static void parsePacket3(byte[] packet, int startingIndex, CreateState result) {
    parsePacket21(packet, startingIndex, result);
    startingIndex += packetDataSize(21);
    parsePacket22(packet, startingIndex, result);
    startingIndex += packetDataSize(22);
    parsePacket23(packet, startingIndex, result);
    startingIndex += packetDataSize(23);
    parsePacket24(packet, startingIndex, result);
    startingIndex += packetDataSize(24);
    parsePacket25(packet, startingIndex, result);
    startingIndex += packetDataSize(25);
    parsePacket26(packet, startingIndex, result);
  }

  static void parsePacket4(byte[] packet, int startingIndex, CreateState result) {
    parsePacket27(packet, startingIndex, result);
    startingIndex += packetDataSize(27);
    parsePacket28(packet, startingIndex, result);
    startingIndex += packetDataSize(28);
    parsePacket29(packet, startingIndex, result);
    startingIndex += packetDataSize(29);
    parsePacket30(packet, startingIndex, result);
    startingIndex += packetDataSize(30);
    parsePacket31(packet, startingIndex, result);
    startingIndex += packetDataSize(31);
    parsePacket32(packet, startingIndex, result);
    startingIndex += packetDataSize(32);
    parsePacket33(packet, startingIndex, result);
    startingIndex += packetDataSize(33);
    parsePacket34(packet, startingIndex, result);
  }

  static void parsePacket5(byte[] packet, int startingIndex, CreateState result) {
    parsePacket35(packet, startingIndex, result);
    startingIndex += packetDataSize(35);
    parsePacket36(packet, startingIndex, result);
    startingIndex += packetDataSize(36);
    parsePacket37(packet, startingIndex, result);
    startingIndex += packetDataSize(37);
    parsePacket38(packet, startingIndex, result);
    startingIndex += packetDataSize(38);
    parsePacket39(packet, startingIndex, result);
    startingIndex += packetDataSize(39);
    parsePacket40(packet, startingIndex, result);
    startingIndex += packetDataSize(40);
    parsePacket41(packet, startingIndex, result);
    startingIndex += packetDataSize(41);
    parsePacket42(packet, startingIndex, result);
  }

  static void parsePacket6(byte[] packet, int startingIndex, CreateState result) {
    parsePacket1(packet, startingIndex, result);
    startingIndex += packetDataSize(1);
    parsePacket2(packet, startingIndex, result);
    startingIndex += packetDataSize(2);
    parsePacket3(packet, startingIndex, result);
    startingIndex += packetDataSize(3);
    parsePacket4(packet, startingIndex, result);
    startingIndex += packetDataSize(4);
    parsePacket5(packet, startingIndex, result);
    startingIndex += packetDataSize(5);
    parsePacket6(packet, startingIndex, result);
  }

  static void parsePacket7(byte[] packet, int startingIndex, CreateState result) {
    result.casterDropped      = iff(packet[startingIndex] & 0x10);
    result.leftWheelDropped   = iff(packet[startingIndex] & 0x08);
    result.rightWheelDropped  = iff(packet[startingIndex] & 0x04);
    result.leftBumperPressed  = iff(packet[startingIndex] & 0x02);
    result.rightBumperPressed = iff(packet[startingIndex] & 0x01);
  }

  static void parsePacket8(byte[] packet, int startingIndex, CreateState result) {
    result.wallDetected = iff(packet[startingIndex]);
  }

  static void parsePacket9(byte[] packet, int startingIndex, CreateState result) {
    result.cliffDetectedLeft = iff(packet[startingIndex]);
  }

  static void parsePacket10(byte[] packet, int startingIndex, CreateState result) {
    result.cliffDetectedFrontLeft = iff(packet[startingIndex]);
  }

  static void parsePacket11(byte[] packet, int startingIndex, CreateState result) {
    result.cliffDetectedFrontRight = iff(packet[startingIndex]);
  }

  static void parsePacket12(byte[] packet, int startingIndex, CreateState result) {
    result.cliffDetectedRight = iff(packet[startingIndex]);
  }

  static void parsePacket13(byte[] packet, int startingIndex, CreateState result) {
    result.virtualWallDetected = iff(packet[startingIndex]);
  }

  static void parsePacket14(byte[] packet, int startingIndex, CreateState result) {
    result.leftWheelOverCurrent = iff(packet[startingIndex] & 0x10);
    result.rightWheelOverCurrent = iff(packet[startingIndex] & 0x8);
    result.ld2 = iff(packet[startingIndex] & 0x4);
    result.ld0 = iff(packet[startingIndex] & 0x2);  // may need to switch these two, likely typo in the spec on page 17
    result.ld1 = iff(packet[startingIndex] & 0x1);
  }

  static void parsePacket17(byte[] packet, int startingIndex, CreateState result) {
    result.byteFromIR = unsign(packet[startingIndex]);
  }

  static void parsePacket18(byte[] packet, int startingIndex, CreateState result) {
    result.nextPressed = iff(packet[startingIndex] & 0x4);
    result.playPressed = iff(packet[startingIndex] & 0x1);
  }

  static void parsePacket19(byte[] packet, int startingIndex, CreateState result) {
    result.distanceIncrement = signedJoin(packet[startingIndex], packet[startingIndex+1]);
  }

  static void parsePacket20(byte[] packet, int startingIndex, CreateState result) {
    result.angleIncrement = signedJoin(packet[startingIndex], packet[startingIndex+1]);
  }

  static void parsePacket21(byte[] packet, int startingIndex, CreateState result) {
    result.chargingState = CreateState.ChargingState.values()[packet[startingIndex]];
  }

  static void parsePacket22(byte[] packet, int startingIndex, CreateState result) {
    result.voltage = unsignedJoin(packet[startingIndex], packet[startingIndex+1]);
  }

  static void parsePacket23(byte[] packet, int startingIndex, CreateState result) {
    result.current = unsignedJoin(packet[startingIndex], packet[startingIndex+1]);
  }

  static void parsePacket24(byte[] packet, int startingIndex, CreateState result) {
    result.batteryTemperature = (int)packet[startingIndex];
  }

  static void parsePacket25(byte[] packet, int startingIndex, CreateState result) {
    result.batteryCharge = unsignedJoin(packet[startingIndex], packet[startingIndex+1]);
  }

  static void parsePacket26(byte[] packet, int startingIndex, CreateState result) {
    result.batteryCapacity = unsignedJoin(packet[startingIndex], packet[startingIndex+1]);
  }

  static void parsePacket27(byte[] packet, int startingIndex, CreateState result) {
    result.wallSensorSignalStrength = unsignedJoin(packet[startingIndex], packet[startingIndex+1]);
  }

  static void parsePacket28(byte[] packet, int startingIndex, CreateState result) {
    result.leftCliffDetectorSignalStrength = unsignedJoin(packet[startingIndex], packet[startingIndex+1]);
  }

  static void parsePacket29(byte[] packet, int startingIndex, CreateState result) {
    result.leftFrontCliffDetectorSignalStrength = unsignedJoin(packet[startingIndex], packet[startingIndex+1]);
  }

  static void parsePacket30(byte[] packet, int startingIndex, CreateState result) {
    result.rightFrontCliffDetectorSignalStrength = unsignedJoin(packet[startingIndex], packet[startingIndex+1]);
  }

  static void parsePacket31(byte[] packet, int startingIndex, CreateState result) {
    result.rightCliffDetectorSignalStrength = unsignedJoin(packet[startingIndex], packet[startingIndex+1]);
  }

  static void parsePacket32(byte[] packet, int startingIndex, CreateState result) {
    result.deviceDetectHigh  = iff(packet[startingIndex] & 0x10);
    result.digitalInput3High = iff(packet[startingIndex] & 0x08);
    result.digitalInput2High = iff(packet[startingIndex] & 0x04);
    result.digitalInput1High = iff(packet[startingIndex] & 0x02);
    result.digitalInput0High = iff(packet[startingIndex] & 0x01);
  }

  static void parsePacket33(byte[] packet, int startingIndex, CreateState result) {
    result.analogInputSignal = unsignedJoin(packet[startingIndex], packet[startingIndex+1]);
  }

  static void parsePacket34(byte[] packet, int startingIndex, CreateState result) {
    result.chargingFromBase  = iff(packet[startingIndex] & 0x02);
    result.chargingFromCable = iff(packet[startingIndex] & 0x01);
  }

  static void parsePacket35(byte[] packet, int startingIndex, CreateState result) {
    result.currentMode = CreateState.RobotMode.values()[packet[startingIndex]];
  }

  static void parsePacket36(byte[] packet, int startingIndex, CreateState result) {
    result.currentSong = unsign(packet[startingIndex]);
  }

  static void parsePacket37(byte[] packet, int startingIndex, CreateState result) {
    result.songPlaying = iff(packet[startingIndex]);
  }

  static void parsePacket38(byte[] packet, int startingIndex, CreateState result) {
    result.streamPacketsCount = unsign(packet[startingIndex]);
  }

  static void parsePacket39(byte[] packet, int startingIndex, CreateState result) {
    result.requestedVelocity = signedJoin(packet[startingIndex], packet[startingIndex+1]);
  }

  static void parsePacket40(byte[] packet, int startingIndex, CreateState result) {
    result.requestedRadius = signedJoin(packet[startingIndex], packet[startingIndex+1]);
  }

  static void parsePacket41(byte[] packet, int startingIndex, CreateState result) {
    result.requestedRightVelocity = signedJoin(packet[startingIndex], packet[startingIndex+1]);
  }

  static void parsePacket42(byte[] packet, int startingIndex, CreateState result) {
    result.requestedLeftVelocity = signedJoin(packet[startingIndex], packet[startingIndex+1]);
  }

  static boolean verifyChecksum(byte[] segment, byte[] query, int startingIndex) {
    int segSize = streamSegmentSize(query);
    // sanity checks
    if (segment == null || segment.length < startingIndex+segSize || segment[startingIndex] != HEADER) {
      return false;
    }
    int sum = 0;
    for (int i=startingIndex; i<startingIndex+segSize; i++) {
      sum += unsign(segment[i]);
    }
    return (sum & 0xFF) == 0 ? true : false;
  }

  static int streamSegmentSize(byte[] query) {
    int sum = 3;  // header bytecount and checksum
    for (int q : query) {
      sum += 1 + packetDataSize(q);  // the 1 here is packet id
    }
    return sum;
  }

  static int packetDataSize(int packetTypeId) {
    switch (packetTypeId) {
      case  0: return 26;
      case  1: return 10;
      case  2: return 06;
      case  3: return 10;
      case  4: return 14;
      case  5: return 12;
      case  6: return 52;
      case  7: return 01;
      case  8: return 01;
      case  9: return 01;

      case 10: return 01;
      case 11: return 01;
      case 12: return 01;
      case 13: return 01;
      case 14: return 01;
      case 15: return 01;
      case 16: return 01;
      case 17: return 01;
      case 18: return 01;
      case 19: return 02;

      case 20: return 02;
      case 21: return 01;
      case 22: return 02;
      case 23: return 02;
      case 24: return 01;
      case 25: return 02;
      case 26: return 02;
      case 27: return 02;
      case 28: return 02;
      case 29: return 02;

      case 30: return 02;
      case 31: return 02;
      case 32: return 01;
      case 33: return 02;
      case 34: return 01;
      case 35: return 01;
      case 36: return 01;
      case 37: return 01;
      case 38: return 01;
      case 39: return 02;

      case 40: return 02;
      case 41: return 02;
      case 42: return 02;
    }
    return -1;
  }
}