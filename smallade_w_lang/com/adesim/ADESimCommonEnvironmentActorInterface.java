package com.adesim;

import java.awt.geom.Point2D;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import ade.ADEComponent;

import com.adesim.datastructures.SimShape;
import com.adesim.gui.datastructures.RobotVisualizationData;
import com.adesim.gui.datastructures.SimMapVisUpdatePackage;
import com.adesim.objects.Door.DoorUpdatingStatus;
import com.adesim.objects.SimEntity;

/** This common interface allows the ADESimMapVis to be used both on the Environment 
 * and on an individual robot.  Some requests the individual robots (actors)
 * will forward directly to the environment (such as set and get is running).  
 * Some requests (such as ones that include a simName) will only make sense
 * in the context of the environment, and the sim name will simply be ignored
 * when directed at single robot! */

public interface ADESimCommonEnvironmentActorInterface extends ADEComponent{
	/** returns update data structure for map visualization */
	public SimMapVisUpdatePackage getSimMapVisUpdatePackage(
			int lastUpdateTickCount, 
			boolean drawLaserLines, boolean drawActivatedObstacleSensors,
			boolean drawPerceivedObjects) throws RemoteException;
	
	/** returns the current state XML configuration */
	public String getCurrentStateConfigXML() throws RemoteException;

    /** gets visualization data for all robots */
    public ArrayList<RobotVisualizationData> getRobotsVisData (boolean drawLaserLines, 
    		boolean drawActivatedObstacleSensors, boolean drawPerceivedObjects) throws RemoteException;

    /** allowRobotMotion: used by GUI when user picks up or puts down the robot.  While you are 
     * moving the robot by mouse, it's rather frustrating when it tries desperately to escape.
     * This method temporarily "lifts the robot off the ground" if allow is set to false */
    public void allowRobotMotion(String simName, boolean allow) throws RemoteException;
    
    /** moves robot by particular offset, and tells any [standalone] GUIs to immediately refresh 
     * (that way, it's possible to see the movement of the robot as it's being dragged) */
    public void moveRobotRelativeWithImmediateRefresh(Point2D.Double offset, String simName) throws RemoteException;
    
    /** returns view preset flags (or null, if not specified by command line) for use in initial creation of the environment's visualization*/
    public String getViewPresetFlags() throws RemoteException;
    
    /** returns edit preset flags (or null, if not specified by command line) for use in initial creation of the environment's visualization*/
    public String getEditPresetFlags() throws RemoteException;

    
    /** Update door status (open, closed, stay_put), on the map.
     * This command is directly to the map -- it is different from telling a robot to open a door
     * using its door-opening effector (and hence having to be within certain distance, for instance).
     * whether called on the robot's map or the environment map, it will forward the request to the environment. */
    public void updateDoorStatusOnMap(UUID doorGUID, DoorUpdatingStatus doorStatus) throws RemoteException;
    
    /** Update box status (open, closed,, on the map.
     * This command is directly to the map -- it is different from telling a robot to open a box
     * using its box-opening effector (and hence having to be within certain distance, for instance).
     * whether called on the robot's map or the environment map, it will forward the request to the environment. */
    public void updateBoxStatusOnMap(UUID boxGUID, boolean open) throws RemoteException;
    
    /** updates object shape on map (such as a result of dragging, rotation, scaling, etc) */
    public void updateObjectShapeOnMap(UUID objectID, SimShape newShape) throws RemoteException;
    
    /** updates object name (useful for duplicating objects, then changing their names) */
    public void updateObjectNameOnMap(UUID objectID, String newName) throws RemoteException;
    
    /** moves object from wherever it is now, to a new container ID.  Since (as of Nov 2010) ADE does NOT
     * allow null to be passed into functions, if passing into an environment, simply create an empty array.
     * otherwise, create a single-element array with the ID of the new container*/
    public void updateObjectParentOnMap(UUID objectID, UUID[] newContainerID) throws RemoteException;
    
    /** removes object.  returns true if succeeded (i.e., object has not been removed already) */
    public boolean removeObjectFromMap(UUID objectID) throws RemoteException;
    
    /** adds object */
    public void addObjectToMap(SimEntity object) throws RemoteException;
    
    /** create brand new environment (i.e., erase all objects, set new world bounds) */
    public void createNewEnvironment(SimShape worldBounds, boolean bounded) throws RemoteException;
    
    
    
    /** let each actor respond to key presses as it wishes */
    public void keyPressed(int keyCode, String simName) throws RemoteException;
    /** let each actor respond to key releases as it wishes */
    public void keyReleased(int keyCode, String simName) throws RemoteException;
    
    
    /** returns help for how to control the particular robot */
    public String getHelpText(String simName) throws RemoteException;
    
    
    /** getRobotShapes -- gets shapes as stored by the model.  The only model that is guaranteed
     * to be accurate is the environment's model.  When robots want to update their knowledge of
     * whether everyone else is, they can use this method (in fact, this method should be called 
     * every tick unless the actor can guarantee to him/herself that the robot
	 * is entirely out of sensory & interaction range).  
	 * The method is also used by the visualization for single robots, to show whether
	 * the robot thinks its neighbors are*/
	public ConcurrentHashMap<String, SimShape> getRobotShapes() throws RemoteException;
}
