/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 * Last update: April 2012
 *
 */
package com.lrf.feature;

import java.io.Serializable;
import java.lang.Cloneable;
import java.awt.geom.Point2D;
import java.util.Set;
import java.util.HashSet;

/**
 * Holds information about an intersection center. 
 * The actual Intersection Detection is in LaserFeatureDetector.java
 */
public class IntersectionCenter implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;
    private double width;
    private double depth;
    private long ID = -1;
    private Point2D.Double center;
    private Set<IntersectionBranch> branches;

    /**
     * IntersectionCenter constructor.
     */
    public IntersectionCenter(IntersectionBranch ib1, IntersectionBranch ib2, long id) {
        branches = new HashSet();
        branches.add(ib1);
        branches.add(ib2);
        ID = id;
        width = ib1.width();
        depth = ib2.width();
        center = centerPoint(ib1, ib2);
    }
    
    /**
     * Copy a IntersectionCenter object.
     * @return the new object
     */
    public IntersectionCenter copy() {
        try {
            return (IntersectionCenter)this.clone();
        } catch (java.lang.CloneNotSupportedException cnse) {
            return null;
        }
    }

    // fill the passed in intersection object with the data from this intersection
    public void fill(IntersectionCenter l) {
        l.width  = this.width;
        l.depth  = this.depth;
        l.ID     = this.ID;
        l.center = (Point2D.Double) this.center.clone();
    }

    // Returns a copy of the intersection branches
    public HashSet<IntersectionBranch> branches() {
        HashSet<IntersectionBranch> allbranches = new HashSet<IntersectionBranch>();
        for (IntersectionBranch b : branches) {
            allbranches.add((IntersectionBranch) b.copy());
        }
        return allbranches;
    }

    public boolean containsBranch(IntersectionBranch query) {
        for (IntersectionBranch thisb : branches) {
                if (cornerInCommon(query, thisb)) {
                    return true;
                }
        }
        return false;
    }

    public double width() {
        return width;
    }

    public double depth() {
        return depth;
    }

    public long ID() {
        return ID;
    }

    public Point2D.Double center() {
        return (Point2D.Double) center.clone();
    }

    public double centery() {
        return center.getY();
    }
    
    public double centerx() {
        return center.getX();
    }
    
    public void setwidth(double w) {
        width = w;
    }

    public void setdepth(double d) {
        depth = d;
    }

    public void setID(long id) {
        ID = id;
    }
    
    public void setcenter(Point2D.Double p) {
        center = (Point2D.Double) p.clone();
    }

    public void addBranch(IntersectionBranch ib) {
        branches.add(ib);
    }

    // Find where the IC center point should be located, given two composing IBs
    private Point2D.Double centerPoint(IntersectionBranch ib1, IntersectionBranch ib2) {
        double tolerance = 0.1; // in m
        double xavg = 0;
        double yavg = 0;

        for (Point2D c1 : ib1.corners()) {
            for (Point2D c2 : ib2.corners()) {
                if (closeProximity(c1, c2, tolerance)) {
                    xavg = (c1.getX() + c2.getX())/2;
                    yavg = (c1.getY() + c2.getY())/2;

                    System.out.println("IBC 1: " + ib1.entryx() + ", " + ib1.entryy());
                    System.out.println("IBC 2: " + ib2.entryx() + ", " + ib2.entryy());
                    System.out.println("Corner 1: " + c1.getX() + ", " + c1.getY());
                    System.out.println("Corner 2: " + c2.getX() + ", " + c2.getY());

                    return mirrorCorner(new Point2D.Double(xavg, yavg), ib1.entry(), ib2.entry());
                }
            }
        }
        System.err.println("Error: Cannot find IntersectionCenter's center");
        return null;
    }

    private boolean closeProximity(Point2D pt1, Point2D pt2, double toleratedDist) {
        return (distanceApart(pt1, pt2) < toleratedDist);
    }

    // Distance apart in meters
    private double distanceApart(Point2D pt1, Point2D pt2) {
        double deltaX = pt1.getX() - pt2.getX();
        double deltaY = pt1.getY() - pt2.getY();
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }


    // Returns the point mirroring the corner over the axis drawn between entry1 and entry2
    private Point2D.Double mirrorCorner(Point2D corner, Point2D entry1, Point2D entry2) {
        double deltaX = corner.getX() - entry1.getX();
        double deltaY = corner.getY() - entry2.getY();
        
        return new Point2D.Double(entry2.getX() - deltaX, entry1.getY() - deltaY);
    }

    private boolean cornerInCommon(IntersectionBranch ib1, IntersectionBranch ib2) {
        double tolerance = 0.1; // in m

        for (Point2D cib1 : ib1.corners()) {
            for (Point2D cib2 : ib2.corners()) {
                if (closeProximity(cib1, cib2, tolerance)) {
                    return true;
                }
            }
        }
        return false;
    }
}
