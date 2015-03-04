/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * HumanoidArmComponent.java
 *
 * @author Paul Schermerhorn
 */
package com.interfaces;

import ade.*;
import com.*;
import com.interfaces.*;
import java.rmi.*;

/**
 * The <code>HumanoidArmComponent</code> interface.
 */
public interface HumanoidArmComponent extends ADEComponent {
    /**
     * Move arm.
     * @param Right move right arm if true, else left arm
     * @param ePitch elbow pitch (0 = straight, bent = 90)
     * @param sYaw shoulder yaw (i.e., to side; 0 = down, 90 = out)
     * @param sPitch shoulder pitch (i.e., to front; 0 = down, 180 = up)
     * @param ePitSpeed elbow pitch speed
     * @param sYawSpeed shoulder yaw speed
     * @param sPitSpeed shoulder pitch speed
     * @return true
     */
    public boolean moveArm(boolean Right, int ePitch, int sYaw, int sPitch, int ePitSpeed, int sYawSpeed, int sPitSpeed) throws RemoteException;

    /**
     * Move arm at single given speed.
     * @param Right move right arm if true, else left arm
     * @param ePitch elbow pitch (0 = straight, bent = 90)
     * @param sYaw shoulder yaw (i.e., to side; 0 = down, 90 = out)
     * @param sPitch shoulder pitch (i.e., to front; 0 = down, 180 = up)
     * @param Speed speed
     * @return true
     */
    public boolean moveArm(boolean Right, int ePitch, int sYaw, int sPitch, int Speed) throws RemoteException;

    /**
     * Move arm at default speed.
     * @param Right move right arm if true, else left arm
     * @param ePitch elbow pitch (0 = straight, bent = 90)
     * @param sYaw shoulder yaw (i.e., to side; 0 = down, 90 = out)
     * @param sPitch shoulder pitch (i.e., to front; 0 = down, 180 = up)
     * @return true
     */
    public boolean moveArm(boolean Right, int ePitch, int sYaw, int sPitch) throws RemoteException;

    /**
     * Move left arm at default speed.
     * @param ePitch elbow pitch (0 = straight, bent = 90)
     * @param sYaw shoulder yaw (i.e., to side; 0 = down, 90 = out)
     * @param sPitch shoulder pitch (i.e., to front; 0 = down, 180 = up)
     * @return true
     */
    public boolean moveLeftArm( int ePitch, int sYaw, int sPitch) throws RemoteException;

    /**
     * Move right arm at default speed.
     * @param ePitch elbow pitch (0 = straight, bent = 90)
     * @param sYaw shoulder yaw (i.e., to side; 0 = down, 90 = out)
     * @param sPitch shoulder pitch (i.e., to front; 0 = down, 180 = up)
     * @return true
     */
    public boolean moveRightArm(int ePitch, int sYaw, int sPitch) throws RemoteException;

    /**
     * Point at a location.
     * @param theta (right-left)
     * @param phi (up-down)
     * @return true
     */
    public boolean Pnt2Obj(double theta, double phi) throws RemoteException;

    /**
     * Wave left hand.
     * @param n the number of waves.
     */
    public void waveLeftHand (int n) throws RemoteException;

    /**
     * Wave right hand.
     * @param n the number of waves.
     */
    public void waveRightHand (int n) throws RemoteException;
}
