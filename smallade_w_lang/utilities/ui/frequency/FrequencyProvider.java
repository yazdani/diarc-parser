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
package utilities.ui.frequency;

import java.rmi.RemoteException;

/**
 *
 * @author M@ <matthew.dunlap+hrilab@gmail.com>
 */
public interface FrequencyProvider {
  public int getCountAndReset() throws RemoteException;
  public String getDataName() throws RemoteException;
}
