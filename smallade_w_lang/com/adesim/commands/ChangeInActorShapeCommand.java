package com.adesim.commands;

import com.adesim.datastructures.SimShape;
import com.adesim.objects.model.ActorModelOwner;
import com.adesim.objects.model.SimModel;

public class ChangeInActorShapeCommand implements ActorCommand {
	private static final long serialVersionUID = 1L;
	
	private String actorName;
	private SimShape newRobotShape;

	public ChangeInActorShapeCommand(String actorName, SimShape newRobotShape) {
		this.actorName = actorName;
		this.newRobotShape = newRobotShape;
	}

	@Override
	public void execute(SimModel model) {
		boolean safeToExecute = true; // in general, execute.
		//    the one time when it's NOT safe to execute this command is if the model owner is 
		//    an actor, and the updated shape is its own self.  In which case do **NOT** want to 
		//    add to otherRobotShapes list, as it would cause the robot to think that it's 
		//    colliding with itself!
		if (model.owner instanceof ActorModelOwner) {
			if (model.robot != null) {
				if (model.robot.getName().equals(actorName)) {
					safeToExecute = false;
				}
			}
		}
		
		if (safeToExecute) {
			model.otherRobotShapes.put(actorName, newRobotShape);
		}
	}

	@Override
	public String toString() {
		return "Change shape of " + actorName;
	}
}
