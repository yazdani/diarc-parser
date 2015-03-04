package com.adesim.datastructures.action;

import java.io.Serializable;

import utilities.xml.Xml;

import com.adesim.objects.SimEntity;
import com.adesim.objects.model.ActorModelOwner;
import com.adesim.robot.SimLocationSpecifier;

public abstract class SimAction implements Serializable {
	private static final long serialVersionUID = 1L;

	/* Receives model owner so that can route requests to environment, if necessary. 
	 * Note that can assume that the owner is an ActorModelOwner because 
	 * the actions only get performed on collision with robot, and the environment
	 * and the GUI own no robot! */
	public abstract void perform(int tickCounter, SimEntity simEntity, 
			ActorModelOwner actorModelOwner, SimLocationSpecifier robotDisplacement);

	public abstract Xml generateXMLinner();
	
	public static String appendTimeStep(String elapsedTime) {
		return "  (" + elapsedTime + ")";
	}
	
	public Xml generateXML() {
		Xml actionXML = new Xml("action");
		actionXML.addChild(generateXMLinner());
		return actionXML;
	}
	
}
