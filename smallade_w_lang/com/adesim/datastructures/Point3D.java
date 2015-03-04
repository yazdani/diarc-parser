package com.adesim.datastructures;

import java.awt.geom.Point2D;
import java.io.Serializable;

/* A VERY VERY basic point datastructure */
public class Point3D implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public double x, y, z;
	public Point2D point2D; // cached for efficiency, better correspond to x and y!
	
	public Point3D(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
		
		this.point2D = new Point2D.Double(x, y);
	}
	
	public Point3D(Point2D point2D, double z) {
		this.point2D = point2D;
		this.z = z;
		
		// reverse-engineer the x and y, so that x and y are in sync with the point2D
		this.x = point2D.getX();
		this.y = point2D.getY();
	}
	
	public void addOffset(double xOffset, double yOffset) {
		this.x += xOffset;
		this.y += yOffset;
		this.point2D = new Point2D.Double(x, y);
	}

	public void addOffset(Point2D offset) {
		addOffset(offset.getX(), offset.getY());
	}
	

	@Override
	public String toString() {
		return "(" + x + ", " + y + ", " + z + ")"; 
	}

}
