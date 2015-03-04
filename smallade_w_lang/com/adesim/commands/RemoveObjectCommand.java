package com.adesim.commands;

import java.util.UUID;

import com.adesim.datastructures.ObjectsHolder;
import com.adesim.objects.SimEntity;
import com.adesim.objects.model.SimModel;

public class RemoveObjectCommand implements ActorCommand {
	private static final long serialVersionUID = 1L;

	private UUID objectID;
	private ApplicableFor removeFrom; // flag to indicate whether to remove from model
	//     or to robot's personal stash
	
	public RemoveObjectCommand(UUID objectID, ApplicableFor removeFrom) {
		this.objectID = objectID;
		this.removeFrom = removeFrom;
	}

	@Override
	public void execute(SimModel model) {
		ObjectsHolder relevantHolder = removeFrom.getObjectHolder(model);
		if (relevantHolder == null) {
			System.out.println("Could not find the object holder that contains the object!");
			return;
		}
		
		SimEntity objectToRemove = relevantHolder.getObjectFromGUID(objectID);
		if (objectToRemove == null) {
			System.err.println("Mysteriously, object has already been removed!");
			return;
		}
		
		relevantHolder.remove(objectToRemove);
	}

}
