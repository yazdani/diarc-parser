package com.adesim.objects.model;

import com.adesim.commands.ActorCommand;

public abstract class EnvironmentModelOwner implements ModelOwner {

	@Override
	public OwnerType getOwnerType() {
		return OwnerType.ENVIRONMENT;
	}
	

	public abstract void addCommand(ActorCommand command);
}
