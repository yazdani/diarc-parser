package com.lrf.feature;

import com.google.common.base.Optional;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.Serializable;

/**
 * @author Jeremiah Via <jeremiah.via@gmail.com>
 */
public class Line implements Serializable {

    private static final long serialVersionUID = 1L;
    // start goes counter-clockwise towards end
    private Optional<Integer> startBeam;
    private Optional<Integer> endBeam;
    private Point2D start;
    private Point2D end;
    /**
     * The proportion of non-maxed out lines to total lines, range of confidence: [0,1]
     */
    private double confidence = 1.0;

    public Line() {
        start = new Point2D.Double();
        end = new Point2D.Double();
        startBeam = Optional.absent();
        endBeam = Optional.absent();
        confidence = 1.0;
    }

    public Line(Point2D start, Point2D end) {
        this.start = start;
        this.end = end;
        startBeam = Optional.absent();
        endBeam = Optional.absent();
        confidence= 1.0;
    }

    public Line(int startBeam, Point2D start, int endBeam, Point2D end, double confidence) {
        this.startBeam = Optional.of(startBeam);
        this.endBeam = Optional.of(endBeam);
        this.start = start;
        this.end = end;
        this.confidence = confidence;
    }

    public int getStartBeam() {
        return startBeam.or(-1);
    }

    public void setStartBeam(int startBeam) {
        this.startBeam = Optional.of(startBeam);
    }

    public int getEndBeam() {
        return endBeam.or(-1);
    }

    public void setEndBeam(int endBeam) {
        this.endBeam = Optional.of(endBeam);
    }

    public Point2D getStart() {
        return start;
    }

    public void setStart(Point2D start) {
        this.start = start;
    }

    public Point2D getEnd() {
        return end;
    }

    public void setEnd(Point2D end) {
        this.end = end;
    }

    @Deprecated
    public double getStartX() {
        return start.getX();
    }

    @Deprecated
    public void setStartX(double startX) {
        start.setLocation(startX, start.getY());
    }

    @Deprecated
    public double getStartY() {
        return start.getY();
    }

    @Deprecated
    public void setStartY(double startY) {
        start.setLocation(start.getX(), startY);
    }

    @Deprecated
    public double getEndX() {
        return end.getX();
    }

    @Deprecated
    public void setEndX(double endX) {
        end.setLocation(endX, end.getY());
    }

    @Deprecated
    public double getEndY() {
        return end.getY();
    }

    @Deprecated
    public void setEndY(double endY) {
        end.setLocation(end.getX(), endY);
    }

    public double getLength() {
        return start.distance(end);
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public double angleTo(Line that) {
        double v1x = this.getEndX() - this.getStartX();
        double v1y = this.getEndY() - this.getStartY();
        double v2x = that.getEndX() - that.getStartX();
        double v2y = that.getEndY() - that.getStartY();
        return Math.acos(((v1x * v2x) + (v1y * v2y)) / (this.getLength() * that.getLength()));
    }

    public boolean intersects(Line that) {
        Line2D a = new Line2D.Double(this.start, this.end);
        Line2D b = new Line2D.Double(that.start, that.end);
        return a.intersectsLine(b);
    }

    public boolean intersectsAny(Iterable<Line> lines) {
        for (Line l : lines) {
            if (this.intersects(l)) return true;
        }
        return false;
    }

    public Line normal(Point2D point) {
        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();

        return new Line(0, point, 0, new Point2D.Double(point.getX() + dx, point.getY() + dy), 1.0);

    }

    /**
     * https://en.wikipedia.org/wiki/Perpendicular_distance
     *
     * @param pt a point
     * @return the perpendicular distance between the point and the line
     */
    public double perpindicularDistanceTo(Point2D pt) {
        double x = pt.getX();
        double y = pt.getY();
        double p1 = start.getX();
        double q1 = start.getY();
        double p2 = end.getX();
        double q2 = end.getY();
        double dp = Math.abs(p1 - p2);
        double dq = Math.abs(q1 - q2);

        return Math.abs(dq * x - dp * y + p1 * q2 - p2 * q1) / Math.sqrt(Math.pow(dp, 2) + Math.pow(dq, 2));
    }
}
