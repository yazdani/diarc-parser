package com.lrf.extractor;

import com.LaserScan;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * A class to extract {@link javax.vecmath.Point2d} from a laser scan.
 *
 * @author Jeremiah Via <jeremiah.via@gmail.com>
 */
public class PointExtractor extends AbstractExtractor<Point2D> {

    public PointExtractor(FeatureModel model) {
        super(model);
    }

    /**
     * Given a laser scan, extract a list of points that represent the laser scan.
     *
     * @param scan the laser scan
     * @return a list of points
     */
    public List<Point2D> extract(LaserScan scan) {
        scan = reduceLaserNoise(scan);
        return laserScanToPoints(scan);
    }

    /**
     * Smooths out readings that are too far from the norm.
     *
     * @param scan a laser scan
     * @return a smoothed laser scan
     */
    private LaserScan reduceLaserNoise(LaserScan scan) {
        for (int i = 0; i < scan.ranges.length; i++) {
            if (!trustworthyLaserReading(scan, i)) {
                if (i > 0 && i < scan.ranges.length - 1) {
                    scan.ranges[i] = (scan.ranges[i - 1] + scan.ranges[i + 1]) / 2;
                }
            }
        }
        return scan;
    }

    /**
     * Converts a laser scan to a list of {@link Point2D}.
     *
     * @param scan a laser scan
     * @return a list of points
     */
    List<Point2D> laserScanToPoints(LaserScan scan) {
        List<Point2D> points = new ArrayList<>();
        for (int i = 0; i < scan.ranges.length; i++) {
            double R = scan.ranges[i];
            //assumes zero is robot's right, and laser points straight ahead
            double minAngle = (Math.PI - scan.angleMax) / 2.0 + scan.angleMin;
            double Theta = i * scan.angleIncrement + minAngle;
            points.add(new Point2D.Double(R * Math.cos(Theta), R * Math.sin(Theta)));
        }

        return points;
    }

    /**
     * Determines if a beam {@code i} is trust worthy or too far outside the norm of its immediately surrounding beams.
     *
     * @param scan a laser scan
     * @param i    a beam index in the laser scan
     * @return
     */
    boolean trustworthyLaserReading(LaserScan scan, int i) {
        if ((i > 0) && (scan.ranges[i] > scan.ranges[i - 1] * model.point().getMaxOffset())) {
            return false;
        } else if ((i < scan.ranges.length - 1) && (scan.ranges[i] > scan.ranges[i + 1] * model.point().getMaxOffset())) {
            return false;
        }
        return true;
    }
}