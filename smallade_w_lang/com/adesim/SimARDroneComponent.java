package com.adesim;

import java.rmi.RemoteException;

public interface SimARDroneComponent extends ADESimActorComponent {
	
	/** sets the goal hover height, in meters. */
	public void setHoverHeight(double height) throws RemoteException;
	
	/** gets the current goal hover height */
	public double getHoverHeight() throws RemoteException;
	
}
