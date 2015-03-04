package com.adesim;

import com.interfaces.LaserComponent;
import java.rmi.RemoteException;

public interface SimPioneerComponent extends ADESimActorComponent, LaserComponent {
	
    /**
     * Check whether there is currently a hallway detected
     * @return true if a hallway is present, false otherwise.
     */
    public boolean checkHallway() throws RemoteException;

    /**
     * Check whether there is currently a doorway detected
     * @return true if a doorway is present, false otherwise.
     */
    public boolean checkDoorway() throws RemoteException;
}
