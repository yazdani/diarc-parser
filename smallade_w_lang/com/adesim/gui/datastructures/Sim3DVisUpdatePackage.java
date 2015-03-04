package com.adesim.gui.datastructures;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import com.adesim.commands.ActorCommand;
import com.adesim.datastructures.ObjectsHolder;
import com.adesim.datastructures.SimShape;
import com.adesim.robot.CameraData;

public class Sim3DVisUpdatePackage implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public String title;
	public CameraData cameraData;
	public int tickCount;
	
	/** indicator whether the update is actually a "re-create model" update
	 * because updateHistory doesn't go far enough (in which case updateHistory
	 * remains unused), or if it's a proper incremental update, in which case
	 * some of the other fields go null and unused).
	 */
	public boolean recreateModel; 
	
	/** used only in case of a proper incremental update */
	public ArrayList<ArrayList<ActorCommand>> updateHistory;
	
	/** used if need to recreate the model */
	public SimShape worldBounds;
	public ObjectsHolder worldObjects;
	public ConcurrentHashMap<String, SimShape> otherRobotShapes;
	
	
	
	
	/** constructor for incremental update */
	public Sim3DVisUpdatePackage(
			CameraData cameraData,
			int tickCount, String title,
			ArrayList<ArrayList<ActorCommand>> updateHistory) {
		this(cameraData, tickCount, title);
		
		this.recreateModel = false;
		this.updateHistory = updateHistory;
	}
	
	
	/** constructor for re-creation-of-model update */
	public Sim3DVisUpdatePackage(
			CameraData cameraData,
			int tickCount, String title,
			SimShape worldBounds,
			ObjectsHolder worldObjects,
			ConcurrentHashMap<String, SimShape> otherRobotShapes) {
		this(cameraData, tickCount, title);

		this.recreateModel = true;
		this.worldBounds = worldBounds;
		this.worldObjects = worldObjects;
		this.otherRobotShapes = otherRobotShapes;
	}
	
	
	/** common part of both of the above constructors */
	private Sim3DVisUpdatePackage(CameraData cameraData,
				int tickCount, String title) {
		this.title = title;
		this.cameraData = cameraData;
		this.tickCount = tickCount;
	}
	
}
