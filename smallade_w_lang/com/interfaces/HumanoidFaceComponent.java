/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * HumanoidFaceComponent.java
 *
 * @author Paul Schermerhorn
 */
package com.interfaces;

import ade.*;
import com.*;
import com.interfaces.*;
import java.rmi.*;

/**
 * The <code>HumanoidFaceComponent</code> interface.
 */
public interface HumanoidFaceComponent extends ADEComponent {
    /**
     * Move eyes.
     * @param left left eye yaw (left = 26, straight = 45, right = 64)
     * @param right right eye yaw (left = 26, straight = 45, right = 64)
     * @param pitch eye pitch (down = 26, straight = 45, up = 64)
     * @param lSpeed speed of left eye yaw
     * @param rSpeed speed of right eye yaw
     * @param pSpeed speed of eye pitch
     * @return true
     */
    public boolean moveEyes(int left, int right, int pitch, int lSpeed, int rSpeed, int pSpeed) throws RemoteException;

    /**
     * Move eyes at default speed.
     * @param left left eye yaw (left = 26, straight = 45, right = 64)
     * @param right right eye yaw (left = 26, straight = 45, right = 64)
     * @param pitch eye pitch (down = 26, straight = 45, up = 64)
     * @return true
     */
    public boolean moveEyes(int left, int right, int pitch) throws RemoteException;

    /**
     * Move eyes at default speed, both eyes yaw to same position.
     * @param eyes eye yaw (left = 26, straight = 45, right = 64)
     * @param pitch eye pitch (down = 26, straight = 45, up = 64)
     * @return true
     */
    public boolean moveEyes(int eyes, int pitch) throws RemoteException;

    /**
     * Start "background" eye movements.
     */
    public int eyeFlickers(int cntPitch, int cntTilt, int RestTimeStep, int NumSteps) throws RemoteException;

    /**
     * Get the position of the eyes.
     * @return an array with the angles
     */
    public int[] getEyePosition() throws RemoteException;

    /**
     * Move eyebrows.
     * @param left left eyebrow angle (middle down = 25, middle up = 65)
     * @param right right eyebrow angle (middle down = 25, middle up = 65)
     * @param lspeed left eyebrow speed
     * @param rspeed right eyebrow speed
     * @return true
     */
    public boolean moveEyeBrows(int left, int right, int lspeed, int rspeed) throws RemoteException;

    /**
     * Move eyebrows at same speed.
     * @param left left eyebrow angle (middle down = 25, middle up = 65)
     * @param right right eyebrow angle (middle down = 25, middle up = 65)
     * @param speed eyebrow speed
     * @return true
     */
    public boolean moveEyeBrows(int left, int right, int speed) throws RemoteException;

    /**
     * Move eyebrows at default speed.
     * @param left left eyebrow angle (middle down = 25, middle up = 65)
     * @param right right eyebrow angle (middle down = 25, middle up = 65)
     * @return true
     */
    public boolean moveEyeBrows(int left, int right) throws RemoteException;

    /**
     * Move eyebrows to same position at default speed.
     * @param Both eyebrow angle (middle down = 25, middle up = 65)
     * @return true
     */
    public boolean moveEyeBrows(int Both) throws RemoteException;

    /**
     * Move mouth.
     * @param uLeft upper left lip servo
     * @param lLeft lower left lip servo
     * @param uRight upper right lip servo
     * @param lRight lower right lip servo
     * @param uLeftSpeed upper left lip speed
     * @param lLeftSpeed lower left lip speed
     * @param uRightSpeed upper right lip speed
     * @param lRightSpeed lower right lip speed
     * @return true
     */
    public boolean moveMouth(int uLeft, int lLeft, int uRight, int lRight, int uLeftSpeed, int lLeftSpeed, int uRightSpeed, int lRightSpeed) throws RemoteException;

    /**
     * Move mouth.
     * @param uLeft upper left lip servo
     * @param lLeft lower left lip servo
     * @param uRight upper right lip servo
     * @param lRight lower right lip servo
     * @return true
     */
    public boolean moveMouth(int uLeft, int lLeft, int uRight, int lRight) throws RemoteException;

    /**
     * Open mouth.
     */
    public void open() throws RemoteException;

    /**
     * Close mouth.
     */
    public void close() throws RemoteException;
}
