package com.adesim.datastructures.action;

import utilities.xml.Xml;

import com.adesim.objects.SimEntity;
import com.adesim.objects.model.ActorModelOwner;
import com.adesim.robot.SimLocationSpecifier;

public class RemoveAction extends SimAction {
	private static final long serialVersionUID = 1L;

	@Override
	/* Receives model owner so that can route requests to environment, if necessary. 
	 * Note that can assume that the owner is an ActorModelOwner because 
	 * the actions only get performed on collision with robot, and the environment
	 * and the GUI own no robot! */
	public void perform(int tickCounter, SimEntity simEntity, 
			ActorModelOwner actorModelOwner, SimLocationSpecifier robotDisplacement) {
		// request environment to remove this object from the environment:
		actorModelOwner.callEnvironment("requestRemoveObject", simEntity.getGUID());
	}

	@Override
	public Xml generateXMLinner() {
		return new Xml("remove");
	}
}
