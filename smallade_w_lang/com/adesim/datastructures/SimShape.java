package com.adesim.datastructures;

import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import utilities.xml.Xml;

import com.adesim.datastructures.action.SimAction;
import com.adesim.objects.model.SimModel;
import com.adesim.util.SimUtil;

/** All shapes in the simulator are comprised of a SimShape --
 * a collection of lines that identify the object's boundaries */
public class SimShape implements Serializable {
	
	/** An observer class, to allow another class (such as a door)
	 * to be notified when a SimShape is updated (for example, so that 
	 * the door can move its pivot point) 
	 * 
	 * NOTE that shapes can change over the lifetime of an object, so
	 * anytime after a setShape() is called in SimEntity, for instance, you'll
	 * want to re-add yourself as an observer*/
	public interface Observer extends Serializable {
		/** a call-back to be notified that a shape has just been transformed via
		 * a particular AffineTransformation */
		public void shapeTransformed(AffineTransform transform);
	}
	
	
    private static final long serialVersionUID = 1L;

	public List<Line2D> lines;
	public List<SimAction> actions;
	public PushabilityDeterminer pushabilityDeterminer; 
	
	private double z = 0;
    private Double zLength = null;

    // points stored for caching size:
    private Point2D minPt, maxPt;
    
    private HashSet<Observer> shapeUpdateObservers = new HashSet<Observer> ();

	
    /** creates a shape based on a collection of points.
     * @param points:  a PointCollection
     * @param actions:  a list of actions to perform upon robot's intersection
     *     with the shape (or null).
     * @param z:  the starting (bottom) z-height of the object (height above ground).
     * @param zLength:  the "height" of the object; distance that it extends above its
     *                  lowest "z".
     * @param pushabilityDeterminer:  a "determinor" to see if the object is "pushable", 
     * 		and hence it's ok for the robot to be intersecting with it.  
     *      Can return a constant value (walls are never pushable),
     *      or can adapt based on circumstances (Doors, for example, are pushable,
     *      if they are not beyond their opening/closing limits, and if they would
     *      not intersect with other objects).
     *      Note that the pushability determiner (if it is not vacuously false) 
     *      must work IN CONJUNCTION WITH THE ACTION LIST, both for internal consistency
     *      and so that, on saving a map, pushability will be preserved.
     * @return the constructed shape.
     */
    public SimShape (PointCollection points, 
    				List<SimAction> actions, double z, Double zLength, 
    				PushabilityDeterminer pushabilityDeterminer) {
    	this(createLinesFromPoints(points), actions, z, zLength, pushabilityDeterminer);
    }
    
	/** creates a shape based on a collection of lines.
     * @param lines:  Array<Line2D>
     * @param actions:  a list of actions to perform upon robot's intersection
     *     with the shape.  can be empty, but NOT null
     * @param z:  the starting (bottom) z-height of the object (height above ground).
     * @param zLength:  the "height" of the object; distance that it extends above its
     *                  lowest "z".
     * @param pushabilityDeterminer:  a "determinor" to see if the object is "pushable", 
     * 		and hence it's ok for the robot to be intersecting with it.  
     *      Can return a constant value (walls are never pushable),
     *      or can adapt based on circumstances (Doors, for example, are pushable,
     *      if they are not beyond their opening/closing limits, and if they would
     *      not intersect with other objects).
     *      Note that the pushability determiner (if it is not vacuously false) 
     *      must work IN CONJUNCTION WITH THE ACTION LIST, both for internal consistency
     *      and so that, on saving a map, pushability will be preserved.
     * @return the constructed shape.
     */
    public SimShape (List<Line2D> lines, 
    				List<SimAction> actions, double z, Double zLength, 
    				PushabilityDeterminer pushabilityDeterminer) {
    	this.lines = lines;
    	
    	if (actions == null) {
        	this.actions = new ArrayList<SimAction>();
        } else {
        	this.actions = actions;	
        }
        
        this.z = z;
        this.zLength = zLength;
        
        this.pushabilityDeterminer = pushabilityDeterminer;
    }
    
    @Override
    public SimShape clone() {
    	return new SimShape(this.lines, this.actions, 
    			this.z, this.zLength, this.pushabilityDeterminer);
		// Note that I'm NOT making a clone of the lines and actions, but that's
		//    ok, because those lists are (operationally) immutable -- you
		//    never add new lines or actions to shapes after they're formed.
    }
    
    private static List<Line2D> createLinesFromPoints(PointCollection points) {
    	List<Line2D> lines = new ArrayList<Line2D> ();
    	int numPoints = points.size();
        for (int startI = 0; startI < numPoints; startI++) {
            int endI = (startI + 1) % numPoints;
            lines.add(new Line2D.Double(points.get(startI), points.get(endI)));
        }
        return lines;
	}
    
	public void updateShape(AffineTransform transform) {
		minPt = null;
		maxPt = null;
		List<Line2D> newLines = new ArrayList<Line2D>();
		for (Line2D aLine : this.lines) {
			Point2D p1 = transform.transform(aLine.getP1(), null);
			Point2D p2 = transform.transform(aLine.getP2(), null);
			newLines.add(new Line2D.Double(p1, p2));
		}
		this.lines = newLines;
		
		for (Observer eachObserver : shapeUpdateObservers) {
			eachObserver.shapeTransformed(transform);
		}
	}
	
	public void addObserver(Observer observer) {
		shapeUpdateObservers.add(observer);
	}
	
	public SimShape createdTransformedShape(AffineTransform transform) {
		// first make clone:
		SimShape transformedClone = this.clone();		
		// then transform:
		transformedClone.updateShape(transform);
		return transformedClone;
	}
	

    public Point2D getMin() {
        recalculateMinMaxIfNecessary();
        return minPt;
    }
    
    public Point2D getMax() {
        recalculateMinMaxIfNecessary();
        return maxPt;
    }

    public DoubleDimension getBoundingDimension() {
        Point2D min = getMin();
        Point2D max = getMax();
        return new DoubleDimension(max.getX()-min.getX(),
                                    max.getY()-min.getY());
    }

    private void recalculateMinMaxIfNecessary() {
        // make sure that there ARE lines:
        if (lines.size() == 0) {
            minPt = null;
            maxPt = null;
            return;
        }

        // from now can assume that all is ok:

        // check if even need to recalculate (otherwise just do nothing)
        if (   (minPt == null) || (maxPt == null)   ) {
            // initialize to something:
            double minX = lines.get(0).getX1();
            double minY = lines.get(0).getY1();
            double maxX = minX;
            double maxY = minY;

            for (Line2D eachLine : lines) {
                Point2D[] points = {eachLine.getP1(), eachLine.getP2()};
                for (Point2D eachPoint : points) {
                    minX = Math.min(minX, eachPoint.getX());
                    minY = Math.min(minY, eachPoint.getY());
                    maxX = Math.max(maxX, eachPoint.getX());
                    maxY = Math.max(maxY, eachPoint.getY());
                }
            }

            minPt = new Point2D.Double(minX, minY);
            maxPt = new Point2D.Double(maxX, maxY);
        }
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        for (Line2D each : lines) {
            out.append(pointCoordinateString(each.getP1()) + " to " +
                       pointCoordinateString(each.getP2()) + "     ");
        }
        return out.toString().trim();
    }

    public static String pointCoordinateString(Point2D point) {
        return "(" + point.getX() + ", " + point.getY() + ")";
    }


	public List<Point2D> getAllPoints() {
		List<Point2D> points = new ArrayList<Point2D>();
		for (Line2D each : lines)
			points.add(each.getP1());
		return points;
	}


	public Point2D getCenter() {
		Point2D min = getMin();
		Point2D max = getMax();
		return new Point2D.Double((min.getX() + max.getX())/2.0,
								  (min.getY() + max.getY())/2.0);
	}


	public boolean intersectsShape(SimShape otherShape) {
		for (Line2D otherShapeLine : otherShape.lines) {
			for (Line2D myLine : this.lines) {
				if (myLine.intersectsLine(otherShapeLine)) {
					return true;
				}
			}
		}
		
		// if not quit with true intersection
		return false;
		
		// FIXME:  add in a check for the height of object
	}


	public List<SimShape> generatePerimiterShapes() {
		ArrayList<SimShape> perimiterShapes = new ArrayList<SimShape>();
		
		for (Line2D line : lines) {
			PointCollection pointCollection = new PointCollection();
			pointCollection.add((Point2D.Double) line.getP1());
			pointCollection.add((Point2D.Double) line.getP2());
			
			// perimiter shapes should presumably carry no actions,
			//    and not be pushable.
			perimiterShapes.add(new SimShape(pointCollection, null, 
					z, zLength, PushabilityDeterminer.alwaysFalse));
		}
		
		return perimiterShapes;
	}

	public double getZ() {
		return z;
	}
	public void setZ(double z) {
		this.z = z;
	}

	/* Null if not set, otherwise the height of the object*/
	public Double getZLength() {
		return zLength;
	}
	
	public void setZLength(Double zLength) {
		this.zLength = zLength;
	}

	
	/** generates XML for shape and action */
	public List<Xml> generateXML() {
		List<Xml> xmlList = new ArrayList<Xml>();
		
		Xml shapeXml = new Xml("shape");
		xmlList.add(shapeXml);
		
		// don't need to worry about rotation, as the polygon for current shape will
		//     already take care of that.
		
		// do, however, need to worry about the z coordinate and length:
		if (this.z != 0) {
			shapeXml.addAttribute("z", Double.toString(this.z));
		}
		if (this.zLength != null) {
			shapeXml.addAttribute("zLength", this.zLength.toString());
		}
		
		
		this.generateXMLparticularShapeHelper(shapeXml);
		
		
		
		for (SimAction action : this.actions) {
			xmlList.add(action.generateXML());
		}
		
		
		return xmlList;
	}
	

	private void generateXMLparticularShapeHelper(Xml shapeXml) {
		if (this.isRectangle()) {
			Point2D min = this.getMin();
			DoubleDimension dimension = this.getBoundingDimension();
			Xml rectXml = shapeXml.addChild(new Xml("rect"));
			rectXml.addAttribute("x", Double.toString(min.getX()));
			rectXml.addAttribute("y", Double.toString(min.getY()));
			rectXml.addAttribute("xLength", Double.toString(dimension.width));
			rectXml.addAttribute("yLength", Double.toString(dimension.height));
		} else {
			// generate shape in terms of polygon
			Xml polygonXml = shapeXml.addChild(new Xml("poly"));
			for (Line2D eachLine : this.lines) {
				Xml ptXml = polygonXml.addChild(new Xml("pt"));
				Point2D pt = eachLine.getP1();
				ptXml.addAttribute("x", Double.toString(pt.getX()));
				ptXml.addAttribute("y", Double.toString(pt.getY()));
			}
		}

	}

	private boolean isRectangle() {
		List<Point2D> actualPoints = this.getAllPoints();
		
		if (actualPoints.size() != 4) {
			return false;
		}
		
		// since still here, assume that COULD be a rectangle or any other 4-sided shape:
		Point2D min = this.getMin();
		Point2D max = this.getMax();
		PointCollection boundingRectPtCollection = new PointCollection();
		boundingRectPtCollection.add(min);
		boundingRectPtCollection.add(min.getX(), max.getY());
		boundingRectPtCollection.add(max);
		boundingRectPtCollection.add(max.getX(), min.getY());
		
		// check that each of the actual points matches the rectangle points:
		for (Point2D eachPt : actualPoints) {
			if (!boundingRectPtCollection.containsPoint(eachPt)) {
				return false;
			}
		}
		
		// if still here, by golly, it's a rectangle!
		return true;
	}

	public Point2D getIntersectionPoint(Line2D externalLine) {
		for (Line2D eachLine : lines) {
			if (eachLine.intersectsLine(externalLine)) {
				return SimUtil.lineIntersection(eachLine, externalLine);
			}
		}
		// if haven't found intersection
		return null; 
	}

	public void assignDefaultZLengthIfNonePresent(Double defaultZLength) {
		if (this.zLength == null) {
			this.zLength = defaultZLength;
		}
	}

	/** returns true if the object is "pushable", and hence it's ok for the robot
	    to be intersecting with it.  Doors, for example, CAN be pushable,
	    though the actual pushability is dependent on checking the door's position
	    against robots and other objects. 
	 * @param model */
	public boolean isPushable(Point2D offset, 
			String thisRobotName, SimModel model) {
		if (offset == null) { // if no offset, don't even try.
			return false;
		} else {
			return pushabilityDeterminer.isPushable(this, offset, thisRobotName, model);
		}
	}
	
	/** returns array with the min and max z values of the object */
	public double[] getMinMaxZ() {
		// zLength should always be assigned -- if not initially, then by the 
		//      SimEntity constructor.  If this somehow returns null, make sure that
		//      the shape is not being re-created somehow, somewhere, without Z being assigned!
		double objectMaxZ = this.z + this.zLength;
		return new double[] {this.z, objectMaxZ};
	}
	
	/** returns true if the object is at least hypothetically pushable, given the right circumstances.
	 * E.g., walls are NEVER hypothetically pushable, whereas a pushable block IS hypothetically
	 * movable, even if, at the moment, it is jammed against the wall */
	public boolean isHypotheticallyPushable() {
		return pushabilityDeterminer.isHypotheticallyPushable();
	}

}
