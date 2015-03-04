package com.adesim.datastructures;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import utilities.xml.Xml;

import com.adesim.objects.SimContainerEntity;
import com.adesim.objects.SimEntity;
import com.adesim.objects.model.SimModel;
import com.adesim.util.SimUtil;


/**
 * This class is used by the Model (and by SimContainerObjects) to hold information about objects contained within then.  
 * It can be used with any SimEntity, regardless of type.
 */
public class ObjectsHolder implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private UUID parentObjectID; // the parent of this objectsholder.  NULL if it's just the model/robot, otherwise
	//    it's the sim container object that holds this holder.
	
	private Vector<SimEntity> objects = new Vector<SimEntity>();
	private HashMap<UUID, SimEntity> myGUIDsToObjectsMap = new HashMap<UUID, SimEntity>();

	
	// parentObject represents the parent of this objectsholder.  NULL if it's just the model/robot, otherwise
	//    it's the sim container object that holds this holder.
	public ObjectsHolder(UUID parentObjectID) {
		this.parentObjectID = parentObjectID;
	}
	
	
	public Vector<SimEntity> getObjects() {
		return objects;
	}
	
	public SimEntity getObjectFromGUID(UUID id) {
		SimEntity preliminaryObject = myGUIDsToObjectsMap.get(id);
		// if found it simply, return it:
		if (preliminaryObject != null) {
			return preliminaryObject;
		} 
		
		// if preliminary object is null, perhaps looking for object inside of object?  need to check any containers:
		for (SimEntity eachEntity : objects) {
			if (eachEntity instanceof SimContainerEntity) {
				SimEntity possibleContainedObject = ((SimContainerEntity) eachEntity).getObjectsHolder().getObjectFromGUID(id);
				if (possibleContainedObject != null) {
					return possibleContainedObject; // yay, found it!
				}
			}
		}
		
		// if still found nothing even after recursive search:
		return null;
	}
	
	/** get objects matching one of the specified criterions.  for all objects, specify 
	 * the wildcard "*" 
	 */
	public List<SimEntity> getMatchingObjects(String[] matchingCriteriaArray,
			Class<SimEntity> particularType) {
		HashSet<String> lookingForObjectsSet = SimUtil.toLowerCaseSet(matchingCriteriaArray);

		List<SimEntity> matches = new ArrayList<SimEntity>();
		synchronized (objects) {
			for (SimEntity eachSimObject : objects) {
				if (particularType.isAssignableFrom(eachSimObject.getClass())) {
					if (eachSimObject.matchesDescription(lookingForObjectsSet)) {
						matches.add(eachSimObject);
					}
				}
			}
			return matches;
		}
	}
	

	public synchronized void remove(SimEntity object) {
		if (SimUtil.IDsAreEqual(parentObjectID, object.getContainingObjectID())) {
			object.setContainingObjectID(null); // I denounce you, object.  I no longer possess you!
		}
		
		boolean removed = objects.remove(object);
		myGUIDsToObjectsMap.remove(object.getGUID());
		
		if (!removed) {
			System.out.println("Error removing object " + object + " (guid = " + object.getGUID() + 
					")!  The object holder thinks it does not exist!!!");
		}
	}

	public synchronized void add(SimEntity object) {
		object.setContainingObjectID(parentObjectID); // Set the object's parent:  "Luke, I am your father!"
		
		objects.add(object);
		
		UUID guid = object.getGUID();
		if (myGUIDsToObjectsMap.put(guid, object) != null) {
			throw new UnsupportedOperationException(
					"This is unbelievable, but there seems to have been a GUID " +
					"overlap!  The universe might collapse soon!");
		}
	}
	
	public synchronized void add(Iterable<SimEntity> objects) {
		for (SimEntity eachObject : objects) {
			add(eachObject);
		}
	}
	
	public synchronized void tick(SimModel model) {
		for (SimEntity each : objects) {
			each.tick(model);
		}
	}
	
	public int size() {
		return objects.size();
	}


	public List<Xml> generateXMLs() {
		List<Xml> xmlChildren = new ArrayList<Xml>();
		for (SimEntity eachObject : objects) {
			xmlChildren.add(eachObject.generateXML());
		}
		return xmlChildren;
	}
}
