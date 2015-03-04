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

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds information about a intersection.
 * The actual Intersection Detection is in LaserFeatureDetector.java
 */
public class IntersectionBranch implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;
    private double distToCenter; // distance to the intersection center
    private double depth;
    private double confidence;
    private boolean matched;
    private boolean visible = true;
    private long ID = -1;
    private Point2D entry;
    private List<Point2D> corners; // Two points associated with this branch opening

    /** IntersectionBranch constructor. */
    public IntersectionBranch(Point2D pt, double c, double measuredDepth,
                              Point2D cornerS, Point2D cornerE) {
        entry = (Point2D) pt.clone();
        corners = new ArrayList<>();
        addCorner(cornerS);
        addCorner(cornerE);
        confidence = c;
        depth = measuredDepth;
        matched = false;
    }

    /** IntersectionBranch constructor. */
    public IntersectionBranch(double ex, double ey, double c, double measuredDepth,
                              Point2D cornerS, Point2D cornerE) {
        entry = new Point2D.Double(ex, ey);
        corners = new ArrayList<>();
        addCorner(cornerS);
        addCorner(cornerE);
        confidence = c;
        depth = measuredDepth;
        matched = false;
    }

    /**
     * Copy an IntersectionBranch object.
     *
     * @return the new object
     */
    public IntersectionBranch copy() {
        try {
            return (IntersectionBranch) this.clone();
        } catch (java.lang.CloneNotSupportedException cnse) {
            return null;
        }
    }

    // fill the passed in intersection object with the data from this intersection
    public void fill(IntersectionBranch i) {
        i.distToCenter = this.distToCenter;
        i.depth = this.depth;
        i.confidence = this.confidence;
        //i.getID           = this.getID;
        i.entry = (Point2D) this.entry.clone();
        i.clearCorners();
        for (Point2D c : this.corners) {
            i.addCorner(c);
        }
    }

    public double distToCenter() {
        return distToCenter;
    }

    public double width() {
        double xavg = 0;
        double yavg = 0;

        for (Point2D corner : corners) {
            xavg += corner.getX();
            yavg += corner.getY();
        }
        return Math.sqrt(xavg * xavg + yavg * yavg);
    }

    public double depth() {
        return depth;
    }

    public double confidence() {
        return confidence;
    }

    public boolean matched() {
        return matched;
    }

    public boolean visible() {
        return visible;
    }

    public long ID() {
        return ID;
    }

    public Point2D.Double entry() {
        return (Point2D.Double) entry.clone();
    }

    public double entryy() {
        return entry.getY();
    }

    public double entryx() {
        return entry.getX();
    }

    public ArrayList<Point2D> corners() {
        ArrayList<Point2D> allcorners = new ArrayList<>();
        for (Point2D c : corners) {
            allcorners.add((Point2D.Double) c.clone());
        }
        return allcorners;
    }

    public void setdistToCenter(double d) {
        distToCenter = d;
    }

    public void setdepth(double d) {
        depth = d;
    }

    public void setconfidence(double c) {
        confidence = c;
    }

    public void setmatched(boolean m) {
        matched = m;
    }

    public void setvisible(boolean v) {
        visible = v;
    }

    public void setID(long id) {
        ID = id;
    }

    public void setentry(Point2D p) {
        entry = (Point2D) p.clone();
    }

    public void clearCorners() {
        corners.clear();
    }

    public void addCorner(Point2D c) {
        corners.add(c);
    }
}
