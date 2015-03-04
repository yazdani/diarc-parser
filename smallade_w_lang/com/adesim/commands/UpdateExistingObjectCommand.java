package com.adesim.commands;

import java.util.UUID;

import com.adesim.datastructures.ObjectsHolder;
import com.adesim.objects.SimContainerEntity;
import com.adesim.objects.SimEntity;
import com.adesim.objects.model.SimModel;

/** command that essentially replaces an existing object with a new updated copy of the object*/
public class UpdateExistingObjectCommand implements ActorCommand {
	private static final long serialVersionUID = 1L;

	private SimEntity updatedObject;
	private ApplicableFor updateWithin ; // flag to indicate whether to add to the model
	//     or to robot's personal stash or to a container
	
	public UpdateExistingObjectCommand(SimEntity updatedObject, ApplicableFor updateWithin) {
		this.updatedObject = updatedObject;
		this.updateWithin = updateWithin;
	}

	@Override
	public void execute(SimModel model) {
		ObjectsHolder preliminaryHolder = updateWithin.getObjectHolder(model);
		
		// first find the object:
		SimEntity objectThatNeedsUpdating = preliminaryHolder.getObjectFromGUID(updatedObject.getGUID());
		if (objectThatNeedsUpdating == null) {
			System.err.println("Could not find the object that needed updating!!!");
			return;
		} else {
			ObjectsHolder realHolder = null;
			UUID possibleNestedHolderID = objectThatNeedsUpdating.getContainingObjectID();
			if (possibleNestedHolderID == null) {
				realHolder = preliminaryHolder;
			} else {
				realHolder = ((SimContainerEntity)preliminaryHolder.getObjectFromGUID(possibleNestedHolderID)).getObjectsHolder();
			}
			
			realHolder.remove(objectThatNeedsUpdating);
			realHolder.add(updatedObject);
		}
	}

}
