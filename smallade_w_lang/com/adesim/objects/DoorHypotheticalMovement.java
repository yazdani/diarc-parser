package com.adesim.objects;

import java.util.HashSet;
import java.util.UUID;

import com.adesim.datastructures.SimShape;
import com.adesim.objects.Door.DoorUpdatingStatus;
import com.adesim.objects.model.SimModel;

public class DoorHypotheticalMovement {
	public DoorUpdatingStatus updatingStatus;
	public double openFraction;
	public boolean performMove = false;
	public SimShape resultingDoorShape = null; // set to something only if performMove = true
	public boolean collision = false; // only relevant if performMove = false
	
	
	public DoorHypotheticalMovement(
			DoorUpdatingStatus passedInUpdatingStatus, double passedInOpenFraction, 
			Door doorObject, SimModel model) {
		this.updatingStatus = passedInUpdatingStatus;
		this.openFraction = passedInOpenFraction;
		
		
		// if was in opening or closing mode, but has reached it's limit, switch to STAY_PUT status:
		if ((updatingStatus == DoorUpdatingStatus.OPEN) && (openFraction >= 1.0)) {
			updatingStatus = DoorUpdatingStatus.STAY_PUT;
		} else if ((updatingStatus == DoorUpdatingStatus.CLOSE) && (openFraction <= 0.0)) {
			updatingStatus = DoorUpdatingStatus.STAY_PUT;
		}
		
		
		// now that possibly updated updatingStatus, check if any movement is required:
		if (updatingStatus == DoorUpdatingStatus.STAY_PUT) { // no movement required.
			return;
		}
		
		
		// check what objects the doors is colliding with at the start.  
		//    For example, if door was already touching a robot (e.g., robot was trying
		//    to push it open, then can expect to still possibly be touching at the end, and hence
		//    should not worry the obstacle-free check).
		//    Essentially, so long as the before and after hashsets are the same, we're golden.
		HashSet<SimShape> originalCollisionShapes = 
				checkDoorCollisionsWithAllRobotsAndOtherRelevantRelevantObjects(
						doorObject.getGUID(), model, doorObject.getShape());
		
		double newOpenFraction;
		if (updatingStatus == DoorUpdatingStatus.OPEN) {
			newOpenFraction = Math.min(1.0 , openFraction + Door.DOOR_OPENING_FRACTION_PER_TICK);
		} else if (updatingStatus == DoorUpdatingStatus.CLOSE) {
			newOpenFraction = Math.max(0, openFraction - Door.DOOR_OPENING_FRACTION_PER_TICK);
		} else {
			System.out.println("Invalid door updating status " + updatingStatus);
			return;
		}
		
		resultingDoorShape = doorObject.generateDoorShape(newOpenFraction);
		
		HashSet<SimShape> newCollisionShapes = 
				checkDoorCollisionsWithAllRobotsAndOtherRelevantRelevantObjects(
						doorObject.getGUID(), model, resultingDoorShape);
		
		// it's ok to LOSE some objects from the collision set (e.g., stop colliding with the robot)
		//    but NOT ok to add them.
		if (originalCollisionShapes.containsAll(newCollisionShapes)) {
			this.openFraction = newOpenFraction;
			this.performMove = true;
		} else {
			this.collision = true;
			// stop trying to open/close
			this.updatingStatus = DoorUpdatingStatus.STAY_PUT;
		}
		
	}
	
	
	
	private HashSet<SimShape> checkDoorCollisionsWithAllRobotsAndOtherRelevantRelevantObjects(
			UUID doorGUID, SimModel model, SimShape newDoorShape) {
		HashSet<SimShape> collidingShapes = new HashSet<SimShape>();
		
		// note:  only need to look at otherRobotShapes, as the Environment does not have its own robot!
		for (SimShape otherRobotShape : model.otherRobotShapes.values()) {
			if (otherRobotShape.intersectsShape(newDoorShape)) {
				collidingShapes.add(otherRobotShape);
			}
		}
		
		for (SimEntity anotherObject : model.worldObjects.getObjects()) {
			// as far as the door is concerned, the only world object that it should worry about 
			//    jamming into is things that are often moved around, i.e., boxes, blocks, etc.
			//    something like landmarks are irrelevant for doors, and walls, while relevant
			//    are too hard to specify (doors ARE right next to walls, so any turning might cause the 
			//    pivoting shape to scrape against the wall, yet that's perfectly normal!)
			// in short, if the other object is either a wall or a landmark, ignore it.
			if (  (anotherObject instanceof Wall) || (anotherObject instanceof Landmark)  ) {
				continue;
			}
			
			// doors are often positioned next to each other (think entrances-to-large-buildings-doors)
			//    so the hinge points may touch without it being a door jam.  On the other hand,
			//    few doors (except in really old homes) are positioned such that they could 
			//    hit each other.  So ignore collisions with other doors as well
			if (anotherObject instanceof Door) {
				continue;
			}
			
			// also, makes no sense to check for intersection against one's own self:
			//     (actually, the "instanceof Door" check should already cover this case,  
			//      but in case that ever gets removed for being too "lenient",
			//      leave this check in place)
			if (anotherObject.getGUID().equals(doorGUID)) { 
				continue;
			}
			
			// finally, the actual check:
			SimShape anotherObjectShape = anotherObject.getShape();
			if (anotherObjectShape.intersectsShape(newDoorShape)) {
				collidingShapes.add(anotherObjectShape);
			}
		}
		
		
		return collidingShapes;
	}

}
