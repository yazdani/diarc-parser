package com.adesim.datastructures;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.Serializable;

/** REALLY simple data structure for holding a point and its color */
public class ColorPoint implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public Color color;
	public Point2D pt;
	public String tooltip; // could be null
	
	public ColorPoint(Color color, Point2D pt, String tooltip) {
		this.color = color;
		this.pt = pt;
		this.tooltip = tooltip;
	}
}
