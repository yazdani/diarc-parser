/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 * Last update: April 2012
 *
 */
package com.lrf;

import com.LaserScan;
import com.lrf.extractor.*;
import com.lrf.feature.Door;
import com.lrf.feature.IntersectionBranch;
import com.lrf.feature.Line;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Door, hallway and intersection detection module.
 * Input: Laser Readings
 * Output: Detected getDoors, detected intersection branches, detected right angles (for visualization purposes)
 * Call detectDoors to run the Door and Hallway detection routines.
 * Call detectIntersections to run the IntersectionBranch detection routine.
 * Check totalNumDoors(), inhallway and totalnumintersectionbranches().
 */
public class LaserFeatureDetector {
    private Log log = LogFactory.getLog(getClass());
    private LaserModel laserModel;
    // Feature model
    FeatureModel model = new FeatureModel()
            .point().setMaxOffset(1.2)
            .line().setOffLineTolerance(0.30)
            .line().setSplitThreshold(0.12)
            .line().setConfidenceThreshold(0.95)
            .door().setMinWidth(0.75)
            .door().setMaxWidth(1.1)
            .door().open().setMinDepth(0.7)
            .door().setMinLineLength(0.15)
            .door().setMinNumBeams(8)
            .rightAngle().setLowerBound(Math.toRadians(80))
            .rightAngle().setUpperBound(Math.toRadians(100))
            .parallel().setThreshold(Math.toRadians(0.17453))
            .parallel().setHallwayLength(17.0)
            .parallel().setMinLength(2.8)
            .intersection().setMinWidth(1.0)
            .intersection().setMaxWidth(3.0)
            .intersection().setIntersectionDepthMin(1.0)
            .intersection().setMinLength(0.3)
            .intersection().setDetectionThreshold(0.5)
            .intersection().setTrackingThreshold(0.5);
    // Feature extractors
    private LineExtractor lineExtractor = new LineExtractor(model);
    private PointExtractor pointExtractor = new PointExtractor(model);
    private DoorExtractor doorExtractor = new DoorExtractor(model);
    private IntersectionExtractor intersectionExtractor = new IntersectionExtractor(model);
    private RightAngleExtractor rightAngleExtractor = new RightAngleExtractor(model);
    private HallwayDetector hallwayDetector = new HallwayDetector(model);
    // Features
    private List<IntersectionBranch> intersectionBranches;
    private List<Line> lines;
    private List<Point2D> points;
    private List<Door> doors;
    private List<Point2D> rightAngles;
    private boolean inHallway;

    // initialize door and line data structures
    public LaserFeatureDetector(int numLaserVals, double laserScanAngle, double toOffset) {
        this(numLaserVals, laserScanAngle, toOffset, 4.0);
    }

    // initialize door and line data structures
    private LaserFeatureDetector(int numLaserVals, double laserScanAngle, double toOffset, double maxReading) {
        laserModel = new LaserModel(numLaserVals, toOffset, laserScanAngle, 0.0, maxReading);
        doors = new ArrayList<>();
        intersectionBranches = new ArrayList<>();
        lines = new ArrayList<>();
        rightAngles = new ArrayList<>();
        points = new ArrayList<>();
    }

    /**
     * Give a new laser scan, perform all of the feature detection.
     *
     * @param distances a double array of laser readings
     */
    public void updateFeatures(double[] distances) {
        LaserScan scan = distancesToLaserScan(distances);
        updateFeatures(scan);
    }

    public void updateFeatures(LaserScan scan) {
        points = pointExtractor.extract(scan);
        lines = lineExtractor.extract(scan);
        doors = doorExtractor.extract(scan);
        intersectionBranches = intersectionExtractor.extract(scan);
        rightAngles = rightAngleExtractor.extract(scan);
        inHallway = hallwayDetector.isHallwayDetected(scan);
    }

    public boolean isInHallway() {
        return inHallway;
    }

    public List<Point2D> getRightAngles() {
        return rightAngles;
    }

    public List<Door> getDoors() {
        return doors;
    }

    public List<IntersectionBranch> getIntersections() {
        return intersectionBranches;
    }

    public double getMaxReading() {
        return laserModel.getRangeMax();
    }

    public List<Point2D> getPoints() {
        return points;
    }

    public List<Line> getLines() {
        return lines;
    }

    private LaserScan distancesToLaserScan(double[] distances) {
        LaserScan scan = new LaserScan();
        scan.angleMin = laserModel.getAngleMin();
        scan.angleMax = laserModel.getAngleMax();
        scan.angleIncrement = laserModel.getAngleIncrement();

        scan.rangeMin = laserModel.getRangeMin();
        scan.rangeMax = laserModel.getRangeMax();

        scan.ranges = distances;

        return scan;
    }

    private class LaserModel {
        private final double angleMin;
        private final double angleIncrement;
        private final double angleMax;
        private final double rangeMin;
        private final double rangeMax;

        private LaserModel(int numLaserRanges, double angleMin, double angleMax, double rangeMin, double rangeMax) {
            this.angleMin = angleMin;
            this.angleIncrement = (angleMax - angleMin) / numLaserRanges;
            this.angleMax = angleMax;
            this.rangeMin = rangeMin;
            this.rangeMax = rangeMax;
        }

        private double getAngleMin() {
            return angleMin;
        }

        private double getAngleMax() {
            return angleMax;
        }

        private double getRangeMin() {
            return rangeMin;
        }

        private double getRangeMax() {
            return rangeMax;
        }

        private double getAngleIncrement() {
            return angleIncrement;
        }
    }
}
