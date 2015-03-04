package com.adesim.datastructures;

import java.awt.geom.Point2D;
import java.util.ArrayList;

public class PointCollection {
    
	private ArrayList<Point2D> points;

	/** constructor for point collection.  if want to create a rectangle based on
	 * min-max points, see "createRectangleFromCornerPoints" */
	public PointCollection() {
		this.points = new ArrayList<Point2D> ();
	}
	
	/** factory for quick creation of rectangle based on two side points */
	public static PointCollection createRectangleFromCornerPoints(
			Point2D startPoint, Point2D endPoint) {
		PointCollection collection = new PointCollection();
		collection.add(startPoint);
		collection.add(startPoint.getX(), endPoint.getY());
		collection.add(endPoint);
		collection.add(endPoint.getX(), startPoint.getY());
		return collection;
	}
	
	
	public void add(Point2D pt) {
        points.add(pt);
    }

    public void add(double x, double y) {
        add(new Point2D.Double(x,y));
    }

    public int size() {
        return points.size();
    }

    public Point2D get(int i)
    {
        return points.get(i);
    }

	public boolean containsPoint(Point2D point) {
		return points.contains(point);
	}

	public void clear() {
		points.clear();
	}

	public void remove(int i) {
		points.remove(i);
	}

}
