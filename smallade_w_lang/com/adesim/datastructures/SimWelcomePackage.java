package com.adesim.datastructures;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

public class SimWelcomePackage implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public int tickCounter;
	public SimShape worldBounds;
	public ObjectsHolder worldObjects;
    public ConcurrentHashMap<String, SimShape> otherRobotShapes;
    public ConfigStartupActorProperties startUpPropertiesIfAny;
    
    
    public SimWelcomePackage(int tickCounter,
    				SimShape worldBounds, 
    				ObjectsHolder worldObjects,
    				ConcurrentHashMap<String, SimShape> otherRobotShapes,
    				ConfigStartupActorProperties startUpPropertiesIfAny) {
    	this.tickCounter = tickCounter;
    	this.worldBounds = worldBounds;
    	this.worldObjects = worldObjects;
    	this.otherRobotShapes = otherRobotShapes;
    	this.startUpPropertiesIfAny = startUpPropertiesIfAny;
    }
}
