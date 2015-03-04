package com.adesim.datastructures;

import java.io.Serializable;
import java.util.List;

import com.adesim.objects.SimEntity;
import com.adesim.robot.SimLocationSpecifier;

/** simple data structure to hold the location and "contains" tags of 
 * <init-robot-positions> elements from the config file */
public class ConfigStartupActorProperties implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public SimLocationSpecifier startupLocation;
	public List<SimEntity> containedObjects;
	
	public ConfigStartupActorProperties(SimLocationSpecifier startupLocation,
			List<SimEntity> containedObjects) {
		this.startupLocation = startupLocation;
		this.containedObjects = containedObjects;
	}
}
