package com.adesim.objects;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.util.ArrayList;

import utilities.xml.Xml;

import com.adesim.datastructures.SimShape;
import com.adesim.gui.PopupObjectAction;
import com.adesim.objects.model.SimModel;
import com.adesim.util.SimUtil;

public class Landmark extends SimEntity{
	private static final long serialVersionUID = 1L;

	public static final Color LANDMARK_COLOR = new Color(128, 0, 255, 100);
	public static final String TYPE = "Landmark";

	public Landmark(SimShape shape, String name, boolean laserable) {
		super(shape, name, TYPE, laserable, null, LANDMARK_COLOR);
        }

         public Landmark(SimShape shape, String name, boolean laserable, Color color) {
                super(shape, name, TYPE, laserable, color, LANDMARK_COLOR);
        }

	@Override
	public Xml generateXML() {
		Xml landmarkXml = new Xml("landmark");
		this.addNameAttributeIfItExists(landmarkXml);
		landmarkXml.addAttribute("laserable", Boolean.toString(this.isLaserVisible()));
		landmarkXml.addChildren(this.getShape().generateXML());
		return landmarkXml;
	}

	@Override
	public String getToolTipIfAny() {
		return getNameOrType(true) +
			((getShape().getZ() > 0) ? ", z = " + SimUtil.formatDecimal(getShape().getZ()) : null);
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
		return 0.01; // just enough to be seen above the floor!
	}

}
