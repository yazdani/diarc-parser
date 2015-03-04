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

/**
 * Holds information about a door.  The actual Door Detection and Hallway Detection has been moved to
 * LaserFeatureDetector.java
 */
public class Door implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;
    public double confidence;
    private State state;
    private double centerDistance;       // the distance to the center of the door
    private int centerIndex;
    private double depth;
    private int lowerIndex;      // index into the laser array
    private int upperIndex;      // index into the laser array
    private long ID = -1;
    private Point2D center;
    private Point2D approach; // Further from door than entry
    private Point2D entry;
    private Point2D exit;
    private Pose agentPose = null; // could store pose associated with this door
    private boolean matched = false; // used to keep track of whether the doors was isMatched to a previous door
    private boolean visible = true;

    public Door(int lowerIndex, int centerindex, int upperIndex, double depth, double centerdist,
                Point2D centerPoint, Point2D entryPoint, Point2D exitPoint, Point2D approachPoint, double confidence, State state) {
        this.lowerIndex = lowerIndex;
        this.centerIndex = centerindex;
        this.upperIndex = upperIndex;
        this.depth = depth;
        this.centerDistance = centerdist;
        this.center = centerPoint;
        this.entry = entryPoint;
        this.exit = exitPoint;
        this.approach = approachPoint;
        this.confidence = confidence;
        this.state = state;
    }

    /**
     * Door constructor.
     */
    public Door() {
    }

    /**
     * Door constructor.
     */
    public Door(int id, double d, int a, int w, double c) {
        ID = id;
        centerDistance = d;
        centerIndex = a;
        confidence = c;
        state = State.OPEN;
    }

    /**
     * Copy a Door object.
     *
     * @return the new object
     */
    public Door copy() {
        try {
            return (Door) this.clone();
        } catch (java.lang.CloneNotSupportedException cnse) {
            return null;
        }
    }

    /**
     * Fill the passed in door object with the data from this door.
     *
     * @param that door to fill
     */
    // TODO: do we need this?
    public void fill(Door that) {
        that.setState(this.getState());
        that.setCenterDistance(this.getCenterDistance());
        that.setCenterIndex(this.getCenterIndex());
        that.setDepth(this.getDepth());
        that.setLowerIndex(this.getLowerIndex());
        that.setUpperIndex(this.getUpperIndex());
        that.setConfidence(this.getConfidence());
        that.setID(this.getID());
        that.setCenter(this.getCenter());
        that.setApproach(this.getApproach());
        that.setEntry(this.getEntry());
        that.setExit(this.getExit());
        that.setAgentPose(this.getAgentPose());
        that.setMatched(this.isMatched());
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Point2D getEntry() {
        return entry;
    }

    public void setEntry(Point2D p) {
        entry = p;
    }

    public boolean isOpen() {
        return state == State.OPEN;
    }

    public void setOpen() {
        state = State.OPEN;
    }

    public void setClosed() {
        state = State.CLOSED;
    }

    public double getCenterDistance() {
        return centerDistance;
    }

    public void setCenterDistance(double d) {
        centerDistance = d;
    }

    public int getCenterIndex() {
        return centerIndex;
    }

    public void setCenterIndex(int a) {
        centerIndex = a;
    }

    // in beams
    public int getWidth() {
        return upperIndex - lowerIndex;
    }

    public double getDepth() {
        return depth;
    }

    public void setDepth(double d) {
        depth = d;
    }

    public int getLowerIndex() {
        return lowerIndex;
    }

    public void setLowerIndex(int li) {
        lowerIndex = li;
    }

    public int getUpperIndex() {
        return upperIndex;
    }

    public void setUpperIndex(int upperIndex) {
        this.upperIndex = upperIndex;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double c) {
        confidence = c;
    }

    public long getID() {
        return ID;
    }

    public void setID(long id) {
        ID = id;
    }

    public Point2D getCenter() {
        return center;
    }

    public void setCenter(Point2D p) {
        center = p;
    }

    public Point2D getApproach() {
        return approach;
    }

    public void setApproach(Point2D p) {
        approach = p;
    }

    public Point2D entry() {
        return entry;
    }

    public Point2D getExit() {
        return exit;
    }

    public void setExit(Point2D p) {
        exit = p;
    }

    public Pose getAgentPose() {
        return agentPose;
    }

    public void setAgentPose(Pose pose) {
        this.agentPose = pose;
    }

    public boolean isMatched() {
        return matched;
    }

    public void setMatched(boolean m) {
        matched = m;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean v) {
        visible = v;
    }

    public enum State {OPEN, CLOSED}
}
