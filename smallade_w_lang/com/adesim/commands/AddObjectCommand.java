package com.adesim.commands;

import com.adesim.objects.SimEntity;
import com.adesim.objects.model.SimModel;

public class AddObjectCommand implements ActorCommand {
	private static final long serialVersionUID = 1L;

	private SimEntity objectToAdd;
	private ApplicableFor addTo ; // flag to indicate whether to add to the model
	//     or to robot's personal stash or to a container
	
	public AddObjectCommand(SimEntity objectToAdd, ApplicableFor addTo) {
		this.objectToAdd = objectToAdd;
		this.addTo = addTo;
	}

	@Override
	public void execute(SimModel model) {
		addTo.getObjectHolder(model).add(objectToAdd);
	}

}
