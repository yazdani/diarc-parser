/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * ADESimEnvironmentComponent.java
 *
 * An ADE simulation environment.
 * 
 * @author Michael Zlatkovsky (based on original ADESim by Paul Schermerhorn, et al)
 * 
 */
package com.adesim;

import java.awt.geom.Point2D;
import java.rmi.RemoteException;
import java.util.UUID;

import ade.ADEComponent;

import com.adesim.datastructures.SimShape;
import com.adesim.objects.SimEntity;
import com.adesim.objects.Door.DoorUpdatingStatus;

public interface ADESimEnvironmentComponent extends ADEComponent, ADESimCommonEnvironmentActorInterface {
	
	/** An "actor" will call this to ask to join the simulation.  The Environment will add it on the
	 * next "tick".
	 * @param serverFullID
	 * @throws RemoteException
	 */
	public void requestToJoinSimulation(String serverFullID) throws RemoteException;
	
	
		/** execute robot popup action (see ActableObjectsPopupListener)
	 *  by redirecting it from the environment to the individual actor */
    public void executeRobotPopupAction(String simName, String methodName, Object[] args) throws RemoteException;
    
	
	/** An actor will call this to notify the environment that it's in a new location.
	 * The name uses "shape" instead of "location" because it is a SimShape that's
	 * being passed around, and it is that shapes that represents the locational 
	 * information of the robot.  
	 * 
	 * Actors must be DILIGENT about notifying the environment any time they move.
	 * This is a bit of a headache to remember, but will SERIOUSLY CUT DOWN ON NETWORK CHATTER
	 * when robots do not move (ie: if robot knows that it only moves through external dragging 
	 * or internal translational/rotational velocities, it can only call this method when
	 * one of those cases is true, rather than having the environment constantly poll
	 * the robots for their locations, even if that is unchanged!)
	 * */
	public void notifyOfChangeInActorShape(String actorName, SimShape newRobotShape) throws RemoteException;
	
	
	/** removes object from environment, and informs all actors */
	public boolean requestRemoveObject(UUID objectID) throws RemoteException;

	/** requests that actor have object removed from the environment and placed into its "personal stash" */
	public boolean requestPickUpObject(UUID objectID, String actorName) throws RemoteException;
	
	public boolean requestPutDownObject(SimEntity object, String actorName) throws RemoteException;
	
	public boolean requestDoorAction(UUID doorID, DoorUpdatingStatus doorStatus) throws RemoteException;
	
	/** requests that the object be pushed a certain direction.  note that the CHECKING for whether
	 * the push is valid must have already happened on the Actor side, this command merely 
	 * copies the instruction into its own environment, dispatches the request to all other actors. */
	public boolean requestPushObject(UUID objectID, Point2D offset, String robotName) throws RemoteException;
		
	public boolean requestPutObjectIntoContainer(SimEntity object, 
					UUID containerID, String actorName) throws RemoteException;
	
	public boolean requestGetObjectFromContainer(UUID objectID, 
					UUID containerID, String actorName) throws RemoteException;
	
	public boolean requestSetContainerStatus(UUID containerID, boolean open) throws RemoteException;
}

