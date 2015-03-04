/**
 * Agent Development Environment (ADE)
 *
 * @version 1.0
 * @author M@
 * @file com/82/SuperADEComponent.java
 *
 * Copyright 1997-2013 M@ Dunlap and HRILab (hrilab.org)
 * All rights reserved. Do not copy and use without permission.
 * For questions contact M@ at <matthew.dunlap+hrilab@gmail.com>
 */
package ade;

import ade.ADEComponent;
import java.rmi.RemoteException;
import java.util.UUID;

/**
 * A wrapper interface that adds a few nice to haves to core.
 *
 * @author M@ <matthew.dunlap+hrilab@gmail.com>
 */
public interface SuperADEComponent extends ADEComponent {
  public UUID getComponentInstanceId() throws RemoteException;
}