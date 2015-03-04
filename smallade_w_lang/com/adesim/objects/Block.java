package com.adesim.objects;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.io.Serializable;
import java.util.ArrayList;

import utilities.ColorConverter;
import utilities.xml.Xml;

import com.adesim.datastructures.SimShape;
import com.adesim.gui.PopupObjectAction;
import com.adesim.objects.model.SimModel;
import com.adesim.util.SimUtil;

public class Block extends SimEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public static final Color DEFAULT_COLOR = ColorConverter.getColorByName("Orange", true);
				// default orange color, if none specified.
	
	public static final String TYPE = "Block";
	

    public Block(SimShape shape, String name, boolean laserable, Color color) {
    	super(shape, name, TYPE, laserable, color, DEFAULT_COLOR);
    }
    
    @Override
    public Xml generateXML() {
    	Xml blockXml = new Xml("block");
    	
    	addNameAttributeIfItExists(blockXml);
    	addColorChildIfItExists(blockXml);

    	blockXml.addAttribute("laserable", Boolean.toString(this.isLaserVisible()));
    	blockXml.addChildren(this.getShape().generateXML());
    	
    	return blockXml;
    }

	@Override
	public String getToolTipIfAny() {
		String tooltip = getNameOrType(true);
		
		if (getShape().getZ() > 0) { 
			tooltip += ", z = " + SimUtil.formatDecimal(getShape().getZ());
		}
		
		if (this.getContainingObjectID() != null) {
			tooltip += " (within container)";
		}
		
		if (this.getShape().isHypotheticallyPushable()) {
			tooltip += "; pushable";
		}
		
		return tooltip;
	}


	@Override
	public void tick(SimModel model) {
		// do nothing.
	}

	@Override
	public ArrayList<PopupObjectAction> getPopupObjectActions() {
		return null; // no actions
	}

	@Override
	public void performAnyPostPainting(Polygon shapeOnScreen, Graphics g) {
		// do nothing
	}

	@Override
	public double defaultZLength() {
		return 0.2;
	}
	

} // End Block
