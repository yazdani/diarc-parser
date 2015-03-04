package com.adesim.commands;

import com.adesim.objects.model.SimModel;

public class RemoveActorCommand implements ActorCommand {
	private static final long serialVersionUID = 1L;
	
	private String actorName;

	public RemoveActorCommand(String actorName) {
		this.actorName = actorName;
	}

	@Override
	public void execute(SimModel model) {
		model.otherRobotShapes.remove(actorName);
	}

	@Override
	public String toString() {
		return "Remove actor " + actorName;
	}
}
