package com.adesim.commands;

import java.awt.geom.Point2D;
import java.util.UUID;

import com.adesim.datastructures.SimShape;
import com.adesim.objects.Door;
import com.adesim.objects.model.SimModel;

public class UpdateDoorShapeCommand implements ActorCommand {
	private static final long serialVersionUID = 1L;
	
	private UUID doorID;
	private double openFraction;
	private Point2D pivot; // since can change if door is moved.
	private double closedAngle; // since can change if door is moved.
	private SimShape newDoorShape;
	
	public UpdateDoorShapeCommand(UUID doorID, 
			double openFraction, Point2D pivot, double closedAngle, SimShape newDoorShape) {
		this.doorID = doorID;
		this.openFraction = openFraction;
		this.pivot = pivot;
		this.closedAngle = closedAngle;
		this.newDoorShape = newDoorShape;
	}

	@Override
	public void execute(SimModel model) {
		Door door = (Door) model.worldObjects.getObjectFromGUID(doorID);
		door.updateDoorShape(openFraction, pivot, closedAngle, newDoorShape);
	}
	
}
