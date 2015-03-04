/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * HumanoidHeadComponent.java
 *
 * @author Paul Schermerhorn
 */
package com.interfaces;

import ade.*;
import com.*;
import com.interfaces.*;
import java.rmi.*;

/**
 * The <code>HumanoidHeadComponent</code> interface.
 */
public interface HumanoidHeadComponent extends ADEComponent {
    /**
     * Move head.
     * @param pitch pitch (down = 70, straight = 90, up = 110)
     * @param yaw yaw (right = 50, straight = 90, left = 130)
     * @param pitchSpeed speed of pitch
     * @param yawSpeed speed of yaw
     * @return true
     */
    public boolean moveHead(int pitch, int yaw, int pitchSpeed, int yawSpeed) throws RemoteException;

    /**
     * Move head at default speed.
     * @param pitch pitch (down = 70, straight = 90, up = 110)
     * @param yaw yaw (right = 50, straight = 90, left = 130)
     * @return true
     */
    public boolean moveHead(int pitch, int yaw) throws RemoteException;

    /**
     * Nod the head, possibly waiting.
     * @param wait whether to wait or not
     * @return true
     */
    public boolean nod(boolean wait) throws RemoteException;

    /**
     * Nod the head, no waiting.
     * @return true
     */
    public boolean nod() throws RemoteException;

    /**
     * Shake the head, possibly waiting.
     * @param wait whether to wait or not
     * @return true
     */
    public boolean shake(boolean wait) throws RemoteException;

    /**
     * Shake the head, no waiting.
     * @return true
     */
    public boolean shake() throws RemoteException;

}
