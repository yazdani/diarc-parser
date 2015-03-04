package com.adesim.objects.model;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.adesim.commands.ActorCommand;
import com.adesim.datastructures.ObjectsHolder;
import com.adesim.datastructures.SimShape;
import com.adesim.datastructures.action.SimAction;
import com.adesim.gui.datastructures.RobotVisualizationData;
import com.adesim.objects.SimContainerEntity;
import com.adesim.objects.SimEntity;
import com.adesim.objects.Wall;
import com.adesim.robot.SimAbstractRobot;
import com.adesim.robot.SimLocationSpecifier;
import com.adesim.util.SimUtil;


public class SimModel {
	public ObjectsHolder worldObjects;
    
    public SimShape worldBounds;
    public SimAbstractRobot robot;
    public ObjectsHolder itemsInRobotPossession;
    public ConcurrentHashMap<String, SimShape> otherRobotShapes;
    public ModelOwner owner;
    
    
    /** constructor used by @link ADESimActorComponentImpl to join the simulation */
    public SimModel(ModelOwner owner, 
    				SimShape worldBounds, 
    				ObjectsHolder worldObjects,
    				ConcurrentHashMap<String, SimShape> otherRobotShapes) {
    	this.owner = owner;
    	this.worldBounds = worldBounds;
    	this.worldObjects = worldObjects;
    	this.otherRobotShapes = otherRobotShapes;
    	this.itemsInRobotPossession = new ObjectsHolder(null);
    }
    
    /** constructor used by the @link ADESimEnvironmentComponentImpl when it instantiates 
     * its model. */
	public SimModel(ModelOwner owner) {
		this.owner = owner;
		worldObjects = new ObjectsHolder(null);
		otherRobotShapes = new ConcurrentHashMap<String, SimShape>();
		itemsInRobotPossession = new ObjectsHolder(null);
	}


	public void createNewEnvironment(SimShape newWorldBounds, boolean bounded) {
		this.worldBounds = newWorldBounds;
		this.worldObjects = new ObjectsHolder(null); // new environment means parent is null!
		
		if (bounded) {
        	for (SimShape eachWorldBoundingShape : worldBounds.generatePerimiterShapes()) {
        		this.worldObjects.add(new Wall(eachWorldBoundingShape));
        	}
        }
	}
	
	public void placeRobotIntoModel(SimAbstractRobot robot) {
		this.robot = robot;
		
		this.robot.updateLaserReadingsIfAny(); // that way, immediately has laser readings,
        //   even if starts off paused
	}


	/** returns true if robot moved */
	public boolean tick(int tickCounter, boolean allowRobotMotion) {
		// regardless of whether or not possesses a robot (Environment does not, neither does the GUI), 
		//     tick objects.  do that first, so robot can update perceptions accordingly.
		worldObjects.tick(this);
		
		
		SimLocationSpecifier robotDisplacement = null;
		// for model-possessors (ie: actors) that have a robot, do robot processing:
		if (robot != null) {
			
			// if robot is allowed to move (i.e., it's not lifted off of the ground), perform the move:
			if (allowRobotMotion) {
				robotDisplacement = robot.performMove(tickCounter);
				// if robot moved, any items that it was carrying should be moved too:
				if (robotDisplacement != null) {
					displaceItemsInRobotPossessionBasedOnRobotMovement(robotDisplacement);
				}

				// actions only make sense in terms of the robot having control over what it's doing.
				//     so if it's being dragged across stuff, don't count that for a cause to 
				//     perform possible actions
				
				// based on robot's possible intersection with shapes:
				performPossibleActions(tickCounter, robotDisplacement); 
			}
			
			// need to update perceptions regardless of having moved (or being moved) or not.
			//     the robot might be standing stationary, but things might well be moving in its
			//     environment, so it will need to update its perceptions regardless!
			robot.updateLaserReadingsIfAny();
		}
		
		return (robotDisplacement != null);
	}

	public void displaceItemsInRobotPossessionBasedOnRobotMovement(SimLocationSpecifier displacement) {
		displaceItemsInRobotPossessionBasedOnRobotMovement(displacement.getX(), 
				displacement.getY(), displacement.getZ(), displacement.getTheta());
	}

	public void displaceItemsInRobotPossessionBasedOnRobotMovement(double x, double y, double z, double theta) {
		Point2D robotLocation = this.robot.getLocation();
		for (SimEntity eachItem : itemsInRobotPossession.getObjects()) {
			if (  (x != 0) || (y != 0) || (z != 0)  ) {
				ObjectMover.translate(eachItem, x, y, z);
			}
			if (theta != 0) {
				ObjectMover.rotate(eachItem, theta, robotLocation); // for realism (i.e., an outstreched arm), 
				//   rotate the block around the ROBOT's axis, not its own!				
			}
		}
	}

	private void performPossibleActions(int tickCounter, SimLocationSpecifier robotDisplacement) {
		for (SimEntity eachEntity : worldObjects.getObjects()) {
			performPossibleActions(tickCounter, eachEntity, robotDisplacement);
		}
	}

	private void performPossibleActions(int tickCounter, SimEntity entity, 
			SimLocationSpecifier robotDisplacement) {
		SimShape shape = entity.getShape();
	
		if (shape.actions.size() > 0) {
			if (robot.getShape().intersectsShape(shape)) {
				for (SimAction action : shape.actions) {
					action.perform(tickCounter, entity, ((ActorModelOwner) this.owner), robotDisplacement);
				}
			}
		}
	}


	public RobotVisualizationData getRobotVisData(boolean drawLaserLines, 
			boolean drawActivatedObstacleSensors, boolean drawPerceivedObjects) {
		try {
			return robot.getRobotVisData(drawLaserLines, drawActivatedObstacleSensors, drawPerceivedObjects);
		} catch (Exception e) {
			System.out.println("Could not get robot visualization data for robot " + robot.getName());
			//handy for debugging:  e.printStackTrace();
			return null;
		}	
	}



	public Iterable<SimShape> getLaserVisibleShapes(double[] minMaxZSpecs) {
		return getLaserVisibleShapes(minMaxZSpecs[0], minMaxZSpecs[1]);
	}
	
	public Iterable<SimShape> getLaserVisibleShapes(double betweenMinHeight, double andMaxHeight) {
		List <SimShape> laserable = new ArrayList<SimShape> ();
		
		//for debugging:  System.out.println("Search between " + betweenMinHeight + " & " + andMaxHeight);
		
		// search through world objects:
		for (SimEntity eachEntity : worldObjects.getObjects()) {
			if (eachEntity.isLaserVisible()) {
				double[] minMaxZ = eachEntity.getShape().getMinMaxZ();
				// only add object if its bottom is less then the specified max height,
				//    and its top is less than the specified min height.
				//    That way will catch any object that is fully inside or at least partially
				//    inside the range, and will ignore those that are too far up or down.
				if (   (andMaxHeight >= minMaxZ[0]) && (betweenMinHeight <= minMaxZ[1])   ) {
					//handy for debugging:  System.out.println("Matched object with min " + minMaxZ[0] + 
					//      ", max " + minMaxZ[1] + "   , " + eachEntity);
					laserable.add(eachEntity.getShape());
				}
			}
		}
		
		// search through robots matching height:
		for (SimShape eachRobotShape : this.otherRobotShapes.values()) {
			double robotZ = eachRobotShape.getZ();
			double[] minMaxZ = new double[] {robotZ, robotZ + eachRobotShape.getZLength()};
			// only add object if its bottom is less then the specified max height,
			//    and its top is less than the specified min height.
			//    That way will catch any object that is fully inside or at least partially
			//    inside the range, and will ignore those that are too far up or down.
			if (   (andMaxHeight >= minMaxZ[0]) && (betweenMinHeight <= minMaxZ[1])   ) {
				//handy for debugging:  System.out.println("Matched object with min " + minMaxZ[0] + 
				//      ", max " + minMaxZ[1] + "   , " + eachEntity);
				laserable.add(eachRobotShape);
			}
		}
		
		return laserable;
	}



	public void updateOtherRobotLocation(String otherSimName,
			SimShape otherRobotShape) {
		this.otherRobotShapes.put(otherSimName, otherRobotShape);
	}
	public void removeOtherRobotRegistration(String otherSimName) {
		this.otherRobotShapes.remove(otherSimName);
	}



	public Double[][] getObjectsAngleAndDistance(String[] lookingForObjectsArray) {
		List<SimEntity> matches = worldObjects.getMatchingObjects(lookingForObjectsArray, SimEntity.class);
		
		
		SimLocationSpecifier robotLocation = robot.getLocationSpecifier();
		
		Double[][] anglesAndDistances = new Double[matches.size()][2];
		int counter = 0;
		for (SimEntity eachMatch : matches) {
			Point2D objectCenter = eachMatch.getShape().getCenter();
			double angleRadians = SimUtil.getAngle0to2PI(robotLocation.getX(), robotLocation.getY(), 
									   			   		 objectCenter.getX(), objectCenter.getY());
			double distance = SimUtil.distance(robotLocation.getXYLocation(), objectCenter);
			
			anglesAndDistances[counter][0] = Math.toDegrees(angleRadians);
			anglesAndDistances[counter][1] = distance;
			counter++;
		}
		
		return anglesAndDistances;
	}


	/** gets the first of the matching objects whose center is within a particular distance
	 * from the robot's center.
	 * Type can either just be SimEntity.class, or something more specify (Door.class)
	 * Returns the first object the robot comes across, or null*/
	public SimEntity getFirstMatchingObjectWithinDistance(
			String lookingForCriteria, Class<?> particularType, double distance) {
		List<SimEntity> possibleObjects = worldObjects.getMatchingObjects(
				new String[] {lookingForCriteria}, SimEntity.class);
		
		Point2D robotCenter = robot.getShape().getCenter();
		
		for (SimEntity eachPossibleObject : possibleObjects) {
			Point2D objectCenter = eachPossibleObject.getShape().getCenter();
			if (SimUtil.distance(robotCenter, objectCenter) <= distance) {
				return(eachPossibleObject);
			}
		}

		// if hasn't found anything, just return null
		return null;
	}
	
	/** Gets all objects whose center is within a particular distance
	 * from the robot's center.
	 * Type can either just be SimEntity.class, or something more specify (Door.class)
	 * Returns a (possibly-empty) list of matching objects*/
	public List<SimEntity> getAllMatchingObjectsWithinDistance(
			String lookingForCriteria, Class<?> particularType, double distance) {
		List<SimEntity> possibleObjects = worldObjects.getMatchingObjects(
				new String[] {lookingForCriteria}, SimEntity.class);
		
		Point2D robotCenter = robot.getShape().getCenter();
		
		List<SimEntity> matchingObjectsToReturn = new ArrayList<SimEntity>();
		
		for (SimEntity eachPossibleObject : possibleObjects) {
			Point2D objectCenter = eachPossibleObject.getShape().getCenter();
			if (SimUtil.distance(robotCenter, objectCenter) <= distance) {
				matchingObjectsToReturn.add(eachPossibleObject);
			}
		}

		return matchingObjectsToReturn;
	}
	

	/** A function used by SimActors ONLY (@see ADESimActorComponent) to 
	 * execute the commands (both general and actor-specific) passed by the environment
	 * @param commands
	 */
	public void applyEnvironmentOrderedCommands(ArrayList<ActorCommand> commands) {
		if (commands != null) {
			for (ActorCommand command : commands) {
				applyEnvironmentOrderedCommand(command);
			}
		}
	}
	
	/** A function used by SimActors ONLY (@see ADESimActorComponent) and also
	 * applyEnvironmentOrderedCommands function above to
	 * execute a command passed by the environment.
	 * Essentially this is just the singular version of the above, for when
	 * an individual immediate update comes in.  But it also is used by the 
	 * plural version, just to keep things consistent. 
	 * */
	public void applyEnvironmentOrderedCommand(ActorCommand command) {
		command.execute(this);
	}

	public SimEntity getObjectFromGUID(UUID id) {
		return worldObjects.getObjectFromGUID(id);
	}

	
	public SimEntity getContainerWithinReach(String typeName) {
		return getFirstMatchingObjectWithinDistance(
				typeName, SimContainerEntity.class, robot.getArmReach());
	}


}
