package com.adesim.objects;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;

import utilities.ColorConverter;
import utilities.xml.Xml;

import com.adesim.datastructures.ObjectsHolder;
import com.adesim.datastructures.SimShape;
import com.adesim.gui.PopupObjectAction;
import com.adesim.objects.model.SimModel;
import com.adesim.util.SimUtil;

public class Box extends SimEntity implements SimContainerEntity {
	private static final long serialVersionUID = 1L;
	
	public static final Color DEFAULT_COLOR = ColorConverter.getColorByName("Brown", true);
	public static final String TYPE = "Box";
	
	private ObjectsHolder containedObjects = new ObjectsHolder(this.getGUID()); 
	// THIS is the parent of the objects holder!
	
	private boolean open;

	
    public Box(SimShape shape, String name, Color color, boolean initialOpen, 
    		List<SimEntity> initialContainedObjects) {
    	super(shape, name, TYPE, true, color, DEFAULT_COLOR);
    	
        this.open = initialOpen;
        
        if (initialContainedObjects != null) {
	        for (SimEntity eachInitialObject : initialContainedObjects) {
	        	add(eachInitialObject);
	        }
        }
    }
    
    @Override
    public Xml generateXML() {
    	Xml boxXml = new Xml("box");
    	
    	addNameAttributeIfItExists(boxXml);
    	boxXml.addAttribute("open", Boolean.toString(open));
    	addColorChildIfItExists(boxXml);
    	boxXml.addChildren(this.getShape().generateXML());
    	
    	if (containedObjects.size() > 0) {
    		Xml containsXml = boxXml.addChild(new Xml("contains"));
    		containsXml.addChildren(this.containedObjects.generateXMLs());
    	}
    	
    	return boxXml;
    }
    

	@Override
	public String getToolTipIfAny() {
		String tempName = this.getNameOrType(true);
		
		if (getShape().getZ() > 0) { 
			tempName += ", z = " + SimUtil.formatDecimal(getShape().getZ());
		}
		
		tempName = tempName + " (";
		if (open) {
			tempName = tempName + "OPEN";
		} else {
			tempName = tempName + "CLOSED";
		}
		
		
		if (containedObjects.size() == 1) {
			tempName = tempName + ", containing 1 object)"; 
		} else {
			tempName = tempName + ", containing " + containedObjects.size() + " objects)";
		}
		
		if (this.getShape().isHypotheticallyPushable()) {
			tempName += "; pushable";
		}
		
		return tempName;
	}

	@Override
	public void tick(SimModel model) {
		// do nothing.
	}

	@Override
	public ArrayList<PopupObjectAction> getPopupObjectActions() {
		ArrayList<PopupObjectAction> actions = new ArrayList<PopupObjectAction>();
		if (open) {
			actions.add (new PopupObjectAction("Close box", "updateBoxStatusOnMap", this.getGUID(), false));
		} else {
			actions.add (new PopupObjectAction("Open box", "updateBoxStatusOnMap", this.getGUID(), true));
		}
		
		return actions;
	}

	@Override
	public void add(SimEntity object) {
		containedObjects.add(object);
		object.snapToCenter(this.getShape());
	}

	@Override
	public ObjectsHolder getObjectsHolder() {
		return containedObjects;
	}
	
	@Override
	public void setObjectsHolder(ObjectsHolder newHolder) {
		this.containedObjects = newHolder;
	}
	
	
	
	@Override
	public void setOpen(boolean flag) {
		this.open = flag;
	}
	
	@Override
	public boolean isOpen() {
		return this.open;
	}
	
	@Override
	public void performAnyPostPainting(Polygon shapeOnScreen, Graphics g) {
		if (this.open) {
			g.setColor(Color.WHITE);
		} else {
			g.setColor(Color.GRAY);
		}
		SimUtil.drawStringIntoRectangle(Integer.toString(containedObjects.size()), 
				shapeOnScreen.getBounds(), (Graphics2D) g);
	}

	@Override
	public double defaultZLength() {
		return 0.3;
	}
	

} // end Box
