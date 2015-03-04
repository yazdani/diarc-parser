package com.adesim.commands;

import com.adesim.datastructures.SimShape;
import com.adesim.objects.model.SimModel;

public class CreateNewEnvironmentCommand implements ActorCommand {
	private static final long serialVersionUID = 1L;

	private SimShape newWorldBounds;
	private boolean bounded;
	
	public CreateNewEnvironmentCommand(SimShape newWorldBounds, boolean bounded) {
		this.newWorldBounds = newWorldBounds;
		this.bounded = bounded;
	}
	
	@Override
	public void execute(SimModel model) {
		model.createNewEnvironment(this.newWorldBounds, this.bounded);
	}

}
