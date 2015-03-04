/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * JoystickComponent.java
 *
 * @author Paul Schermerhorn
 */
package com.joystick;

import ade.*;
import com.interfaces.*;
import java.rmi.*;

/** <code>JoystickComponent</code> takes input from a joystick (e.g., the
 * Logitech Dual Action) and passes on velocity commands to servers
 * implementing the {@link com.interfaces.VelocityComponent VelocityComponent} interface.
 * Although it has been tested with only the Logitech Dual Action gamepad,
 * it reads directly from the joystick event device, so it should work with
 * others, modulo button/axis differences.
 */
public interface JoystickComponent extends ADEComponent {

    /**
     * Get the most recently-read values.  A maping of values for the
     * Logitech Dual Action gamepad (default mode--LED off):<br>
     * values[0] - left stick x<br>
     * values[1] - left stick y<br>
     * values[2] - right stick x<br>
     * values[3] - right stick y<br>
     * values[4] - pad x<br>
     * values[5] - pad y<br>
     * values[6] - button 1<br>
     * values[7] - button 2<br>
     * values[8] - button 3<br>
     * values[9] - button 4<br>
     * values[10] - button 5 (L1)<br>
     * values[11] - button 6 (R1)<br>
     * @return an array of the values
     */
    public short[] getValues() throws RemoteException;
}

