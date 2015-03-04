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

import ade.ADEComponent;
import com.interfaces.BumperSensorComponent;
import com.interfaces.VelocityComponent;
import java.rmi.RemoteException;

/**
 * CreateComponent is a low level control component for the iRobot Create.
 *
 * Full documentation of the Create is available at:
 * http://www.irobot.com/filelibrary/pdfs/hrd/create/Create%20Manual_Final.pdf
 * http://www.irobot.com/filelibrary/pdfs/hrd/create/Create%20Open%20Interface_v2.pdf
 * and mirrored on hrilab in /home/hrilab.
 *
 * @author M@ <matthew.dunlap+hrilab@gmail.com>
 */
public interface CreateComponent extends ADEComponent,
                                         BumperSensorComponent,
                                         VelocityComponent {
  public void playSound(String filename) throws RemoteException;
  public void stationarySpin(double speed) throws RemoteException;
}