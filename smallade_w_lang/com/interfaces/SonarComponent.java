/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 * Last update: April 2010
 *
 * SonarComponent.java
 */
package com.interfaces;

import ade.ADEComponent;
import java.rmi.*;

/**
SonarComponent.java

Defines a standard set of methods that must be defined in an
ADEComponent to use sonar.

Naturally, it is not required to conform or even to use the
interfaces. However, by explicitly declaring them, it acts
as a guarantee that robot control code will work mostly as
expected no matter what ADEComponent it is connected with.
 */
public interface SonarComponent extends ADEComponent {

    /** Returns a particular sonar reading.
     * @param w Which sonar
     * @return The distance reading
     * @throws RemoteException If an error occurs */
    public double getSonar(int w) throws RemoteException;

    /** Returns the sonar readings.
     * @return Array of distances array
     * @throws RemoteException If an error occurs */
    public double[] getSonars() throws RemoteException;

    /** Returns indicator of a particular new sonar reading.
     * @param w which sonar
     * @return boolean
     * @throws RemoteException If an error occurs */
    public boolean getSonarNew(int w) throws RemoteException;

    /** Returns indicators for new sonar readings.
     * @return boolean array
     * @throws RemoteException If an error occurs */
    public boolean[] getSonarNew() throws RemoteException;
}						
