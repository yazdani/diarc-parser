package com.adesim.datastructures.action;

import java.awt.geom.Point2D;

import utilities.xml.Xml;

import com.adesim.objects.Door;
import com.adesim.objects.Door.DoorUpdatingStatus;
import com.adesim.objects.SimEntity;
import com.adesim.objects.model.ActorModelOwner;
import com.adesim.objects.model.SimModel;
import com.adesim.robot.SimLocationSpecifier;
import com.adesim.util.SimUtil;

public class DoorPushOpenAction extends SimAction {
	private static final long serialVersionUID = 1L;
	
	
	/** checks whether the robot would be trying to open the door on collision (true)
	 * or close it (false).  There are really no other alternatives 
	 * (we'll assume there's no such thing as a perfectly dead-on collision).
	 * Note that trying to open is quite different from actually opening 
	 * (if there's door jam, door is already open/closed to max, etc).  This
	 * methods merely determines the intention. */
	public static boolean wouldBeTryingToOpenTheDoor(Door door, SimModel model) {
		Point2D robotCenter = model.robot.getShape().getCenter();
		
		double currentAngle = door.getCurrentAngle();
		double closedAngle = door.getClosedAngle();
		//System.out.println("Current angle = " + currentAngle + ";   closed = " + closedAngle);
		
		double doorPivotToRobotAngle = SimUtil.getAngle0to2PI(door.getPivot(), robotCenter);
		
		// check diff between current door angle and closed angle
		double currentToClosedDiff = currentAngle - closedAngle;
		
		// and the diff between robot and current door angle
		double robotToCurrentDiff = doorPivotToRobotAngle - currentAngle;
		
		
		// in so doing, I've established the RELATIVE differences which should be independent
		//     of rotation and pivot point.  Now just need to compare the sign, to check that,
		//     essentially, robot will be doing the same or the exact opposite of what
		//     the current door angle to closed/open angle difference is doing.
		
		// the only problem is the discontinuity between 0 and 2*PI.  The easiest approach
		//     is to take the SIN of the angle difference, that way both -1.7 and 4.8, for instance
		//     turn out to be equally negative in terms of being in the -Y plane of the circle,
		//     even though they don't look to be so.  Of course, this trick only works so long that
		//     a door's opening range is not greater than PI, but most doors only swing a little 
		//     over 90 degrees, or at least are bounded by walls and etc. to swing more than 180.
		
		boolean tryToOpen;
		
		if (currentToClosedDiff == 0) {
			double currentToOpenDiff = currentAngle - door.getOpenAngle();
			if (SimUtil.sameSign(Math.sin(currentToOpenDiff), Math.sin(robotToCurrentDiff))) {
				// same sign, so want to go to the open position.
				tryToOpen = true;
			} else {
				// opposite sign, so want to request closing:
				tryToOpen = false;
			}
		} else {
			if (SimUtil.sameSign(Math.sin(currentToClosedDiff), Math.sin(robotToCurrentDiff))) {
				// same sign, so want to go to the closed position.
				tryToOpen = false;
			} else {
				// opposite sign, so want to request opening:
				tryToOpen = true;
			}
		}
		
		return tryToOpen;
	}
	
	@Override
	/* Receives model owner so that can route requests to environment, if necessary. 
	 * Note that can assume that the owner is an ActorModelOwner because 
	 * the actions only get performed on collision with robot, and the environment
	 * and the GUI own no robot! */
	public void perform(int tickCounter, SimEntity simEntity, 
			ActorModelOwner actorModelOwner, SimLocationSpecifier robotDisplacement) {
		if (wouldBeTryingToOpenTheDoor((Door) simEntity, actorModelOwner.getModel())) {
			actorModelOwner.callEnvironment("requestDoorAction", simEntity.getGUID(), DoorUpdatingStatus.OPEN);
		} else {
			actorModelOwner.callEnvironment("requestDoorAction", simEntity.getGUID(), DoorUpdatingStatus.CLOSE);
		}
	}

	@Override
	public Xml generateXMLinner() {
		return new Xml("pushDoor");
	}
}
