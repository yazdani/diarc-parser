package com.adesim.commands;

import java.io.Serializable;
import java.util.UUID;

import com.adesim.datastructures.ObjectsHolder;
import com.adesim.objects.SimContainerEntity;
import com.adesim.objects.model.SimModel;

/** An interface to make it VERY EXPLICIT for whom the command is intended 
 * (ie: remove item from the environment or remove it from the actual actor) */

public interface ApplicableFor extends Serializable {
	
	/** The one method the ApplicableFor must implement -- what is the object holder? 
	 * @param model */
	public ObjectsHolder getObjectHolder(SimModel model);
	
	
	
	/********************************
	 * SUBCLASSES OF APPLICABLE FOR:
	 ********************************/
	
	public class ActorEnvironment implements ApplicableFor {
		private static final long serialVersionUID = 1L;
		
		@Override
		public ObjectsHolder getObjectHolder(SimModel model) {
			return model.worldObjects;
		}		
	}
	
	
	public class ActorPersonalStash implements ApplicableFor {
		private static final long serialVersionUID = 1L;

		@Override
		public ObjectsHolder getObjectHolder(SimModel model) {
			return model.itemsInRobotPossession;
		}		
	}
	
	
	
	public class Container implements ApplicableFor {
		private static final long serialVersionUID = 1L;
		
		private UUID id;
		public Container(UUID id) {
			this.id = id;
		}
		@Override
		public ObjectsHolder getObjectHolder(SimModel model) {
			SimContainerEntity container = 	(SimContainerEntity) model.
					getObjectFromGUID(id);
			return container.getObjectsHolder();
		}
	}

	
	
}

