package com.lrf.feature;

import java.awt.geom.Point2D;

/** @author Jeremiah Via <jeremiah.via@gmail.com> */
public final class Pose {

    private final Point2D position;
    private final double rotation;

    public Pose(Point2D position, double rotation) {
        this.position = position;
        this.rotation = rotation;
    }

    public Pose(double x, double y, double rotation) {
        this.position = new Point2D.Double(x, y);
        this.rotation = rotation;
    }

    public Point2D getPosition() {
        return position;
    }

    public double getRotation() {
        return rotation;
    }

    public Pose setPosition(Point2D position) {
        return new Pose(position, rotation);
    }

    public Pose setRotation(double rotation) {
        return new Pose(position, rotation);
    }
}
