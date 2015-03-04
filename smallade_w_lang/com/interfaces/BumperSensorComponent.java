/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 * Last update: April 2010
 *
 * BumperSensorComponent.java
 */
package com.interfaces;

//import com.AgeSUnrequestableComponentImpl;
import ade.ADEComponent;
import java.rmi.*;

/**
BumperSensorComponent.java

Defines a standard set of methods that must be defined in an
ADEComponent to get readings from the bumpers.

Naturally, it is not required to conform or even to use the
interfaces. However, by explicitly declaring them, it acts
as a guarantee that robot control code will work mostly as
expected no matter what ADEComponent it is connected with.
 */
public interface BumperSensorComponent extends ADEComponent {

    /** Return individual bumper value.
     * @param w which bumper */
    public boolean getBumper(int w) throws RemoteException;

    /** Return array of bumper values */
    public boolean[] getBumperReadings() throws RemoteException;
}						
