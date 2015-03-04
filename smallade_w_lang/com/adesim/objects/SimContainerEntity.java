package com.adesim.objects;

import com.adesim.datastructures.ObjectsHolder;


/** an interface for SimEntity objects that can be used as containers for other objects 
 * (for example, Boxes are containers for blocks)*/
public interface SimContainerEntity {
	public void add(SimEntity object);
	
	public ObjectsHolder getObjectsHolder();
	
	/** to set to a brand-new object holder (such as when making clone of SimEntity) */
	public void setObjectsHolder(ObjectsHolder newHolder); 
	
	public void setOpen(boolean flag);
	public boolean isOpen();
}
