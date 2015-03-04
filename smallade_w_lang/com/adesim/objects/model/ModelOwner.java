package com.adesim.objects.model;


public interface ModelOwner {
    public enum OwnerType {
    	ENVIRONMENT, ACTOR, GUI
    }
    
    public OwnerType getOwnerType();
    
    public SimModel getModel();
}
