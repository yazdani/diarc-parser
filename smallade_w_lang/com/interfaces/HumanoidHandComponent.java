/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * HumanoidHandComponent.java
 *
 * @author Paul Schermerhorn
 */
package com.interfaces;

import ade.*;
import com.*;
import com.interfaces.*;
import java.rmi.*;

/**
 * The <code>HumanoidHandComponent</code> interface.
 */
public interface HumanoidHandComponent extends ADEComponent {
    
    /** Open the hand.
     * @param right if true, open right hand, otherwise open left
     * @return the task ID
     */
    public int openHand(boolean right) throws RemoteException;

    /** Close the hand.
     * @param right if true, close right hand, otherwise close left
     * @return the task ID
     */
    public int closeHand(boolean right) throws RemoteException;
}
