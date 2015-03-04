package com.lrf.extractor;

import com.LaserScan;
import com.lrf.feature.Line;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * A class which extracts right angle features from a laser scan.
 *
 * @author Steven Lessard <sr.lessard@gmail.com>
 * @author Jeremiah Via <jeremiah.via@gmail.com>
 * @since 2013-10-25
 */
public class RightAngleExtractor extends AbstractExtractor<Point2D> {
    private LineExtractor lineExtractor;

    public RightAngleExtractor(FeatureModel model) {
        super(model);
        lineExtractor = new LineExtractor(model);
    }

    /**
     * Extracts the right angles from the laser scan.
     *
     * @param scan the laser scan
     * @return a potentially empty list of extracted right angles represented by points
     */
    public List<Point2D> extract(LaserScan scan) {
        return extractRightAngles(lineExtractor.extract(scan));
    }

    List<Point2D> extractRightAngles(List<Line> lines) {
        List<Point2D> rightAngles = new ArrayList<>();

        for (int i = 0; i < lines.size() - 1; i++) {
            Line curr = lines.get(i);
            Line next = lines.get(i + 1);

            if (isRightAngle(curr.angleTo(next)))
                rightAngles.add(next.getStart());
        }
        return rightAngles;

    }

    private boolean isRightAngle(double angle) {
        return ((angle > model.rightAngle().getLowerBound()) && (angle < model.rightAngle().getUpperBound()));
    }
}
