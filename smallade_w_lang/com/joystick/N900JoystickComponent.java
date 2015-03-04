/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * N900JoystickComponent.java
 *
 * @author Paul Schermerhorn
 */
package com.joystick;

import ade.*;
import com.interfaces.*;
import java.rmi.*;

/** <code>N900JoystickComponentImpl</code> takes input from the accelerometer
 * on a Nokia N900, translates and passes on velocity commands to servers
 * implementing the {@link com.interfaces.VelocityComponent VelocityComponent}
 * interface.
 */
public interface N900JoystickComponent extends ADEComponent {

    /**
     * Get the most recently-read values.
     * @return an array of the values
     */
    public short[] getValues() throws RemoteException;
}

