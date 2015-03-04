package com.adesim.objects.model;

public abstract class ActorModelOwner implements ModelOwner {

	@Override
	public OwnerType getOwnerType() {
		return OwnerType.ACTOR;
	}

	public abstract void callEnvironment(String methodName, Object... args);

}
