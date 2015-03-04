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

import ade.SuperADEComponentImpl;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Scanner;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/**
 * @author M@ <matthew.dunlap+hrilab@gmail.com>
 */
public class CreateComponentImpl extends SuperADEComponentImpl implements CreateComponent {
  public static final int CREATE_UPDATE_PERIOD = 15; //ms
  public static final int MAX_BYTES_PER_UPDATE_PERIOD = 86;
  public static final int SERIAL_PORT_MAX_WAIT = 10000; //ms
  public static final int MAX_TRANSLATIONAL_SPEED = 500; //mm/s
  public static final int MAX_TURNING_RADIUS = 2000; //mm

  boolean talkative;
  boolean safeMode;
  SerialPort port;
  String serialPortName;
  String baudRate;
  String name;
  OutputStream toRobot;
  BufferedInputStream fromRobot;

  byte[] dataStreamQuery;
  byte[] readBuffer;
  int readBufferWriteHead;

  CreateState lastState;

  /////////////////////////////////
  // SuperADEComponent Overrides //
  /////////////////////////////////
  public CreateComponentImpl() throws RemoteException {
    super();
    setUpdateLoopTime(this, CREATE_UPDATE_PERIOD);
    openSerialConnection();
    log.trace("Done Constructing");

    sendCommand(Commands.START);
    sendCommand(Commands.LOAD_STATUS_SONGS);
    if (safeMode) {
      sendCommand(Commands.SAFE);
    } else {
      sendCommand(Commands.FULL);
    }
    setStreamQuery(new byte[]{0, 5});  // begin streaming telemetry
    if (talkative) {
      System.out.println("Singin' Ready");
      sendCommand(Commands.SING_READY);
    }
    log.info(name + " is good to go.");
  }

  @Override
  public void init() {
    talkative = true;
    safeMode = false;
    port = null;
    serialPortName = "/dev/ttyAMA0";
    baudRate = "57600";
    toRobot = null;
    fromRobot = null;
    readBuffer = new byte[MAX_BYTES_PER_UPDATE_PERIOD];
    readBufferWriteHead = 0;
    getRobotName();

    lastState = new CreateState();
  }
 
  @Override 
  protected void updateComponent() {
    if (fromRobot != null) {
      readSensorPacket();
    }
  } 

  @Override 
  protected boolean localServicesReady() {
    return toRobot != null && fromRobot != null && requiredConnectionsPresent();
  }

  @Override
  protected void localshutdown() {
    log.info("Create shutting down...");
    sendCommand(Commands.STOP);  // I will give you one guess what this does
    sendCommand(Commands.PAUSE_STREAM);  // stops the streaming telemetry
    sendCommand(Commands.PASSIVE);  // go into passive mode (disable control of the actuators)
    try {
      if (toRobot != null) {
        toRobot.close();
      }
      if(fromRobot != null) {
        fromRobot.close();
      }
    } catch (IOException io) {
      log.warn("IO problems while shutting down... meh.", io);
    }
    if (port != null) {
      port.close();
    }
    log.debug("done.");
  }

  @Override
  public String additionalUsageInfo() {
    return "-port=X -stfu -safe";
  }

  @Override
  public boolean parseArgs(String[] args) {
    String arg;
    for (String a : args) {
      arg = a.toLowerCase();
      if (arg.startsWith("-port=")) {
        serialPortName = a.substring(6);
      } else if ("-stfu".equals(arg)) {
        talkative = false;
      } else if ("-safe".equals(arg)) {
        safeMode = true;
      } else {
        log.fatal("Invalid Argument: " + a);
        return false;
      }
    }
    return true;
  }

  ////////////////////////////////
  // Create Component Overrides //
  ////////////////////////////////
  @Override
  public void playSound(final String filename) {
    new Thread() {
      @Override
      public void run() {
        try {
          Clip clip = AudioSystem.getClip();
          clip.open(AudioSystem.getAudioInputStream(new File(filename)));
          clip.start();
          long length = clip.getMicrosecondLength();
          Thread.sleep(length / 1000);
          clip.close();
        } catch (Exception e) {
          log.error("Couldn't play " + filename, e);
        }
      }
    }.start();
  }

  @Override
  public void stationarySpin(double speed) {
    int right = (int)speed / 2;
    int left = -right;
    sendCommand(new byte[]{Commands.DRIVE_DIRECT, highByte(right), lowByte(right), highByte(left), lowByte(left)});
  }

  /////////////////////////////////////
  // BumperSensorComponent Overrides //
  /////////////////////////////////////
  @Override
  public boolean getBumper(int whichBumper) {
    if (lastState == null || lastState.leftBumperPressed == null) {
      return false;
    }
    switch (whichBumper) {
      case 0: return lastState.leftBumperPressed.booleanValue();
      case 1: return lastState.rightBumperPressed.booleanValue();
    }
    return false;
  }

  @Override
  public boolean[] getBumperReadings() {
    if (lastState == null || lastState.leftBumperPressed == null) {
      return null;
    }
    return new boolean[] {lastState.leftBumperPressed.booleanValue()
                          , lastState.rightBumperPressed.booleanValue()};
  }

  /////////////////////////////////
  // VelocityComponent Overrides //
  /////////////////////////////////
  @Override
  public double getTV() {
    if (lastState != null && lastState.requestedVelocity != null) {
      return lastState.requestedVelocity.doubleValue();
    }
    log.warn("Translational velocity requested before any telemetry has become available, sending default.");
    return 0;
  }

  @Override
  public double getRV() {
    if (lastState != null && lastState.requestedRadius != null) {
      return lastState.requestedRadius.doubleValue();
    }
    log.warn("Rotational velocity requested before any telemetry has become available, sending default.");
    return 0;
  }

  @Override
  public double[] getVels() {
    if (lastState != null && lastState.requestedRadius != null) {
      return new double[] {lastState.requestedVelocity.doubleValue()
                           , lastState.requestedRadius.doubleValue()};
    }
    log.warn("Telemetry requested before any has arrived, sending null.");
    return null;
  }

  @Override
  public double[] getDefaultVels() {
    return new double[] {MAX_TRANSLATIONAL_SPEED / 2, MAX_TURNING_RADIUS / 2};
  }

  @Override
  public void stop() {
    sendCommand(Commands.STOP);
  }

  @Override
  public boolean setTV(double tv) {
    return setVels(tv, getRV());
  }

  @Override
  public boolean setRV(double rv) {
    return setVels(getTV(), rv);
  }

  @Override
  public boolean setVels(double tv, double rv) {
    if (Double.isNaN(tv) || Double.isNaN(rv)) {
      log.error("You can't set velocities to NaN, stop trying!!");
      return false;
    }
    // sanity bound the inputs
    tv = tv > MAX_TRANSLATIONAL_SPEED ? MAX_TRANSLATIONAL_SPEED : tv;
    tv = tv < -MAX_TRANSLATIONAL_SPEED ? -MAX_TRANSLATIONAL_SPEED : tv;
    rv = rv > MAX_TURNING_RADIUS ? MAX_TURNING_RADIUS : rv;
    rv = rv < -MAX_TURNING_RADIUS ? -MAX_TURNING_RADIUS : rv;

    // command the move based on the special cases listed in the Create spec
    if (tv == 0 && rv == 0) {
      stop();
    } else if (tv == 0) {
      stationarySpin(rv);
    } else if (rv == 0) {
      sendCommand(new byte[]{Commands.DRIVE, highByte((int)tv), lowByte((int)tv), highByte(Commands.STRAIGHT), lowByte(Commands.STRAIGHT)});
    } else {
      // flip the magnitude of rv to convert from rotational velocity to turning radius
      if (rv > 0) {
        rv = MAX_TURNING_RADIUS - rv;
      } else if (rv < 0) {
        rv = -MAX_TURNING_RADIUS - rv;
      }
      sendCommand(new byte[]{Commands.DRIVE, highByte((int)tv), lowByte((int)tv), highByte((int)rv), lowByte((int)rv)});
    }
    return true;
  }

  //////////////////////////////////
  // High Level Utility Functions //
  //////////////////////////////////
  void setStreamQuery(byte[] newQuery) {
    if (DataParser.streamSegmentSize(newQuery) <= MAX_BYTES_PER_UPDATE_PERIOD) {
      dataStreamQuery = newQuery;
      byte[] commandString = new byte[2 + newQuery.length];
      commandString[0] = Commands.STREAM;
      commandString[1] = (byte)newQuery.length;
      for (int i=0; i<newQuery.length; i++) {
        commandString[2 + i] = newQuery[i];
      }
      sendCommand(commandString);
      readBuffer = new byte[DataParser.streamSegmentSize(newQuery)];
    } else {
      log.warn(String.format("Querying for {0} would require more than the available bandwidth.  Ignoring request to change stream query.", newQuery));
    }
  }

  void appendToStreamQuery(byte packetTypeId) {
    byte[] nq = Arrays.copyOf(dataStreamQuery, dataStreamQuery.length + 1);
    nq[dataStreamQuery.length] = packetTypeId;
    setStreamQuery(nq);
  }

  void readSensorPacket() {
    try {
      int oldWriteHead;
      while (fromRobot.available() > 0) {
        oldWriteHead = readBufferWriteHead;
        readBufferWriteHead += fromRobot.read(readBuffer, readBufferWriteHead, readBuffer.length - readBufferWriteHead);
        if (DataParser.verifyChecksum(readBuffer, dataStreamQuery, oldWriteHead)) {
          lastState = DataParser.parsePacket(readBuffer, dataStreamQuery, oldWriteHead, lastState);
          readBufferWriteHead = 0;
          log.trace("Successful parse");
        } else if (readBuffer.length - readBufferWriteHead == 0) {
          log.debug("Something got out of sync in the telemetry stream, fast forwarding to the next packet.");
          // find the first header
          int firstHeader = 1;  // if we've run out of space but the first item is 19 we still want to fast forward, so we begin at the second element
          for (int i=firstHeader; i<readBufferWriteHead; i++) {
            if (readBuffer[i] == DataParser.HEADER) {
              break;
            } else {
              firstHeader++;
            }
          }
          // move it to the first position
          for (int i=firstHeader; i<readBuffer.length; i++) {
            readBuffer[i - firstHeader] = readBuffer[i];
          }
          readBufferWriteHead -= firstHeader;
        }
      }
    } catch (IOException io) {
      log.error("Error reading from the serial port!", io);
      if (talkative) {
        sendCommand(Commands.SING_DRAMATIC);
      }
    }
  }

  void getRobotName() {
    name = "Roompi";
    // I hate every design decision that went into making a language where the following isn't considered terrible.
    try {
      String lowerName = new Scanner(new File("/etc/hostname")).useDelimiter("\\Z").next();
      char[] asChars = lowerName.toCharArray();
      asChars[0] = Character.toUpperCase(asChars[0]);
      name = new String(asChars);
    } catch (Exception ex) {}
  }

  // TODO: add a function to convert the battery charge telemetry into a command for the power LED

  /////////////////////////////////
  // Low Level Utility Functions //
  /////////////////////////////////
  void openSerialConnection() {
    try {
      System.setProperty("gnu.io.rxtx.SerialPorts", serialPortName);
      CommPortIdentifier cpi = CommPortIdentifier.getPortIdentifier(serialPortName);
      port = (SerialPort) cpi.open("ADE Create Component", SERIAL_PORT_MAX_WAIT);
      port.setSerialPortParams(Integer.parseInt(baudRate), SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

      toRobot = port.getOutputStream();
      fromRobot = new BufferedInputStream(port.getInputStream());
    } catch (NoSuchPortException nsp) {
      log.fatal("This computer doesn't have a serial port called " + serialPortName, nsp);
      System.exit(1);
    } catch (PortInUseException piu) {
      log.fatal(String.format("{0} is already in use!  Sad panda.  Personally, I blame {1}.", serialPortName, piu.currentOwner), piu);
      System.exit(1);
    } catch (UnsupportedCommOperationException uco) {
      log.fatal(baudRate + " is not a valid baud rate (trust the default on this one).", uco);
      System.exit(1);
    } catch (IOException io) {
      log.error("IO has barfed.  Perhaps this'll help you figure out why?", io);
    }
    if (toRobot == null || fromRobot == null) {
      log.fatal("For some unknown reason we don't have a serial connection!");
      System.exit(2);
    }
  }

  void sendCommand(byte[] cmd) {
    try {
      toRobot.write(cmd);
    } catch (IOException io) {
      log.error(String.format("Couldn't send {0}", cmd), io);
    }
  }

  byte highByte(int val) {
    return (byte) ((val & 0xFF00) >> 8);
  }

  byte lowByte(int val) {
    return (byte) (val & 0xFF);
  }
}
