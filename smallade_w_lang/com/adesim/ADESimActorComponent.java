/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * ADESimComponent.java
 *
 * ADESim: ADE interface to ADESim simulator
 *
 * @author Paul Schermerhorn
 *
 **/
package com.adesim;

import java.rmi.RemoteException;
import java.util.ArrayList;

import utilities.xml.Xml;
import ade.ADEComponent;

import com.ADEPercept;
import com.ActionStatus;
import com.adesim.commands.ActorCommand;
import com.adesim.datastructures.SimShape;
import com.adesim.datastructures.SimWelcomePackage;
import com.adesim.gui.datastructures.RobotVisualizationData;
import com.adesim.gui.datastructures.Sim3DVisUpdatePackage;
import com.interfaces.ADEPerceptComponent;
import com.interfaces.PositionComponent;
import com.interfaces.VelocityComponent;
import com.interfaces.VisionComponent;

public interface ADESimActorComponent extends ADEComponent, ADESimCommonEnvironmentActorInterface,
					VelocityComponent, PositionComponent, ADEPerceptComponent,
                                        VisionComponent {

	/** tick:  method by which the sim environment has the actor move one step further in the simulated time.
	 * @param tickCount : which tick am I on (elapsedTime *COULD* be generated off of it,
	 * but easier to just copy the pre-made string, and that way it's consistent between environment and actors)
	 * @param elapsedTime : string representing the elapsed time (mostly just for the GUI display or system.out.printline-s)
	 * @param generalCommands : commands that all of the actors get passed (i.e. remove object x from environment)
	 * @param actorSpecificCommands : commands specific to the actor (i.e., put object x into YOUR private stash)
	 * @throws RemoteException
	 */
	public void tick(int tickCount, ArrayList<ActorCommand> generalCommands,
					 ArrayList<ActorCommand> actorSpecificCommands) throws RemoteException;

	/** a method by which the environment can send immediate updates to the actor, IF that's the
	 * way that the Environment's UpdateManager is configured.  the actorSpecific boolean flag
	 * is necessary so that the actor knows whether to add the command to the history holder
	 * for the visualization or not (the visualization does not own a robot, so it only needs
	 * to know about general commands, NOT actor-specific ones) */
	public void receiveImmediateUpdate(ActorCommand command, boolean actorSpecific) throws RemoteException;

	/** method for interfacing with sim environment:  request to join simulation */
	public void joinSimulation(SimWelcomePackage welcomePackage) throws RemoteException;

	/** returns robot position and contained objects*/
	public Xml generateRobotPropertiesXML() throws RemoteException;

	/** returns update data structure for 3D visualization (if actor has camera) */
	public Sim3DVisUpdatePackage getSim3DVisUpdatePackage(int lastUpdateTickCount) throws RemoteException;


	/** react to key pressed event */
	public void keyPressed(int keyCode) throws RemoteException;
	/** react to key released event */
	public void keyReleased(int keyCode) throws RemoteException;


    /** returns help for how to control the particular robot */
	public String getHelpText() throws RemoteException;


    /**
     * Return the stall value.  If the robot is in a collision, stall will
     * be true (i.e., motors ostensibly on, but no motion).
     * @return true if robot is stalled, false otherwise
     */
    public boolean getStall() throws RemoteException;

    /** Returns internal odometry readings.  There are noisy if the noise option is turned on! */
    public double[] getPoseEgo() throws RemoteException;

    /**
     * Return the Orientation, in degrees.  0 degrees is due east,
     * 90 degrees is due north (assuming north is at the top of
     * the map).
     * @return <tt>double</tt> degrees  0 = east; 180 = west.
     */
    public double getOrientation() throws RemoteException;


    /** gets the robot's current shape in the current location (as a SimShape object) */
    public SimShape getRobotShape() throws RemoteException;

    /** gets visualization data for the PARTICULAR robot */
    public RobotVisualizationData getRobotVisData(boolean drawLaserLines,
    		boolean drawActivatedObstacleSensors, boolean drawPerceivedObjects) throws RemoteException;


    /** returns a double array, with the inner array representing the angle and distance
     * (from center of the robot), of each matched object.
     * @param lookingForObjects.  Array of one or more parameters to look for, each of which will be
     * matched separately; that way, it's possible to find out both where the "red block" and
     * the "yellow block" are with just one call).
     * Can also use generic types, i.e., block.  Note that angle is relative to where robot is, not where
     * it is currently facing!  Finally, note that this returns objects regardless of whether or not
     * the robot is able to perceive them (they can be in a door-closed room, and they'd still be returned.
     * think "radio beacons").
     * @return
     * @throws RemoteException
     */
    public Double[][] getObjectsAngleAndDistance(String[] lookingForObjects) throws RemoteException;




	/** picks up an object that matches the criteria (name or type), and if it is within
	 * range of the actor's arm (if the actor HAS an arm).  If not, just a no-op.
	 * "true" simply means that the request was accepted, thought it more than one robot go for the object
	 * at the same time, both will get "true", but only one will actually get the object.
	 * @param lookingForCriteria.  Name of object or general type (i.e., "block"). If multiple
	 * objects are matched, will pick up the first one.
	 * @throws RemoteException
	 */
	public boolean pickUpObject(String lookingForCriteria) throws RemoteException;

	/** picks up the object with the given vision token ID if it is within
	 * range of the actor's arm (if the actor HAS an arm).  If not, just a no-op.
	 * "true" simply means that the request was accepted, though if more than one robot go for the object
	 * at the same time, both will get "true", but only one will actually get the object.
	 * @param tokenId the vision token ID, as returned by one of the VisionComponent methods
         * @return an ID that can be used for subsequent status queries
	 * @throws RemoteException
	 */
        public long pickUpObject(Long tokenId) throws RemoteException;


	/** puts down object if it is in the robot's possession.  In doesn't match anything (or if
	 * no objects exist), a no-op.
	 * @param lookingForCriteria.  Name of object or general type (i.e., "block"). If multiple
	 * objects are matched, will put down the first one.
	 * @throws RemoteException
	 */
	public boolean putDownObject(String matchingCriteria) throws RemoteException;

	/** puts down object if it is in the robot's possession.  In doesn't match anything (or if
	 * no objects exist), a no-op.
	 * @param tokenId the vision token ID, as returned by one of the VisionComponent methods
         * @return false if the object is unknown or not currently held
	 * @throws RemoteException
	 */
	public boolean putDownObject(Long tokenId) throws RemoteException;

        /** 
         * Check status of an extended action.  Soon this will be obsoleted, as the
         * extended commands will notify Action of their completions.
         * @param aid the identifying timestamp of the action to check
         * @return the status found for the indicated action
         */
        public ActionStatus checkAction(long aid) throws RemoteException;

	/** returns the number of objects (probably blocks) in possession of the actor.*/
	public int getObjectsInPossesionCount() throws RemoteException;

	/** returns false if can't find object or container is closed.  if "reasonable" request, return true. */
	public boolean putObjectIntoContainer(String objectInPossessionCriteria,
						String containerNameCriteria) throws RemoteException;

    /** returns false if can't find object or container is closed.  if "reasonable" request, return true.
     * "true" simply means that the request was accepted, thought it more than one robot go for the object
	 * at the same time, both will get "true", but only one will actually get the object. */
	public boolean getObjectFromContainer(String objectToPickUpCriteria,
			String containerNameCriteria) throws RemoteException;



	/** open the first door (name or just generic "Door", see Door.TYPE) that matches the
	 * door description, if it is within reach of the door reaching mechanism.  If door already
	 * open, a no-op (and returns false).  If nothing matched, definitely a no-op (and false).
	 * Returning true means that the door has been found and should be openable, though
	 * if there are obstacles to cause door jams, for instance, or if the door is already fully open,
	 * the call wouldn't know about it, and the return would still return true.
	 * @param lookingForCriteria.  Name of door or general "Door" type.  If multiple
	 * doors are matched, will open the first one.
	 * @throws RemoteException
	 */
	public boolean openDoor(String matchingDescription) throws RemoteException;


	/** close the first door (name or just generic "Door", see Door.TYPE) that matches the
	 * door description, if it is within reach of the door reaching mechanism.  If door already
	 * closed, a no-op (and returns false).  If nothing matched, definitely a no-op (and false).
	 * Returning true means that the door has been found and should be close-able, though
	 * if there are obstacles to cause door jams, for instance, or if the door is already fully closed,
	 * the call wouldn't know about it, and the return would still return true.
	 * @param lookingForCriteria.  Name of door or general "Door" type.  If multiple
	 * doors are matched, will close the first one.
	 * @throws RemoteException
	 */
	public boolean closeDoor(String matchingDescription) throws RemoteException;


	/** get percepts within robot's camera's field of vision.  matching criteria could be
	 * the wildcard "*" if you want any object within the field.
	 * If calling from GoalManager (or probably from any other server), note that you need
	 * to CAST the string array to an Object, otherwise Java (or RMI, or ADE) will
	 * assume that you're passing separate strings.  E.g., use the getPercepts method
	 * as follows:
	 * String[] query = {"box", "robot"};
	 * Vector<ADEPercept> percepts = (Vector<ADEPercept>) callMethod("getPercepts", (Object)query);
	 **/
	public ArrayList<ADEPercept> getPercepts(String[] matchingCriteria) throws RemoteException;


	/****** BOX API as in the old ADESim *******/


    /* **** Box effector **** */
    /**
     * Open the box with name.  Only works if the box is close enough
     * @param tokenId the token ID of the box to open
     * @return false if box can't be opened, true otherwise
     */
    public boolean openBox(Long tokenId) throws RemoteException;

    /**
     * Open the box with name.  Only works if the box is close enough
     * @param name the name of the box to open
     * @return false if box can't be opened, true otherwise
     */
    public boolean openBox(String name) throws RemoteException;

    /**
     * Close the box with name.  Only works if the box is close enough
     * @param tokenId the token ID of the box to close
     * @return false if box can't be closed, true otherwise
     */
    public boolean closeBox(Long tokenId) throws RemoteException;

    /**
     * Close the box with name.  Only works if the box is close enough
     * @param name the name of the box to close
     * @return false if box can't be closed, true otherwise
     */
    public boolean closeBox(String name) throws RemoteException;

    /**
     * Get the block blockname from the box boxname.  Only works if box is
     * close enough, is open, and contains block.
     * @param boxId the token ID of the box to get the block from
     * @param blockId the token ID of the block to remove
     * @return false if unable to get block from box, true otherwise
     */
    public boolean getFromBox(Long boxId, Long blockId) throws RemoteException;

    /**
     * Get the block blockname from the box boxname.  Only works if box is
     * close enough, is open, and contains block.
     * @param boxname the name of the box to get the block from
     * @param blockname the block to remove
     * @return false if unable to get block from box, true otherwise
     */
    public boolean getFromBox(String boxname, String blockname) throws RemoteException;

    /**
     * Put the block blockname into the box boxname.  Only works if box is
     * close enough, and is open, and robot is holding block.
     * @param boxId the token ID of the box to put the block into
     * @param blockId the token ID of the block to remove
     * @return false if unable to put block into box, true otherwise
     */
    public boolean putIntoBox(Long boxId, Long blockId) throws RemoteException;

    /**
     * Put the block blockname into the box boxname.  Only works if box is
     * close enough, and is open, and robot is holding block.
     * @param boxname the name of the box to put the block into
     * @param blockname the block to remove
     * @return false if unable to put block into box, true otherwise
     */
    public boolean putIntoBox(String boxname, String blockname) throws RemoteException;

    /**
     * Pick up the specified box.  Only works if the box is close enough.
     * @param tokenId the token ID of the box to pick up
     * @return false if unable to pick up box, true otherwise
     */
    public boolean pickUpBox(Long tokenId) throws RemoteException;

    /**
     * Pick up the specified box.  Only works if the box is close enough.
     * @param name the name of the box to pick up
     * @return false if unable to pick up box, true otherwise
     */
    public boolean pickUpBox(String name) throws RemoteException;

    /**
     * Put down the specified box next to the robot
     * @param tokenId the token ID of the box to pick up
     * @return false if unable to put down box, true otherwise
     */
    public boolean putDownBox(Long tokenId) throws RemoteException;

    /**
     * Put down the specified box next to the robot
     * @param name the name of the box to pick up
     * @return false if unable to put down box, true otherwise
     */
    public boolean putDownBox(String name) throws RemoteException;

    /**
     * Reset odometry.
     */
    public void resetOdometry() throws RemoteException;
}
