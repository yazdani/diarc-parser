package com.lrf.extractor;

import com.LaserScan;
import com.google.common.collect.Range;
import com.lrf.feature.Line;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;

public class LineExtractor extends AbstractExtractor<Line> {
    private List<Line> lines;
    private PointExtractor pointExtractor;

    public LineExtractor(FeatureModel model) {
        super(model);
        lines = new ArrayList<>();
        pointExtractor = new PointExtractor(model);
    }

    public List<Line> extract(LaserScan scan) {
        List<Point2D> points = pointExtractor.extract(scan);
        lines = split(points);
        for (Line line : lines)
            line.setConfidence(lineConfidence(line, scan));
        return lines;
    }

    List<Line> split(List<Point2D> points) {
        lines = new ArrayList<>();
        if (points.isEmpty()) return lines;
        splitLines(0, points.size() - 1, 0, points);
        return lines;
    }

    int splitLines(int first, int last, int newlines, List<Point2D> points) {
        // need at least two points
        if (last - first == 0)
            return newlines;
        else if (last - first > 1) {
            Point2D firstPt = points.get(first);
            Point2D lastPt = points.get(last);
            // split between those two points
            int farthest = first;
            double farthestdistance = 0.0;
            double y2minusy1 = lastPt.getY() - firstPt.getY();
            double x2minusx1 = lastPt.getX() - firstPt.getX();
            double root = Math.sqrt(x2minusx1 * x2minusx1 + y2minusy1 * y2minusy1);
            // find the point with the largest distance
            for (int i = first + 1; i < last; i++) {
                Point2D iPt = points.get(i);
                double x1minusxi = firstPt.getX() - iPt.getX();
                double y1minusyi = firstPt.getY() - iPt.getY();
                double d = Math.abs(x2minusx1 * y1minusyi - x1minusxi * y2minusy1) / root;
                if (d > farthestdistance) {
                    farthest = i;
                    farthestdistance = d;
                }
            }
            // if we can split, let's split and update the newlines count
            if (farthestdistance > model.line().getSplitThreshold()) {
                newlines = splitLines(first, farthest, newlines, points);
                newlines = splitLines(farthest, last, newlines, points);
                return newlines;
            }
        }
        // if we get here there were either no splits or there were not points between first and last, so make a line
        Line line = new Line(first, points.get(first), last, points.get(last), 1.0);
        lines.add(line);
        return lines.size();
    }

    /**
     * Calculate the confidence in the value of a line.
     *
     * @param line a line
     * @param scan the scan from which the line was generated
     * @return a number in the range [0.0, 1.0]
     */
    double lineConfidence(Line line, LaserScan scan) {
        int maxedouts = 0;

        for (int i = line.getStartBeam(); i < line.getEndBeam() + 1; i++) {
            if (scan.ranges[i] > scan.rangeMax * 0.95) {
                maxedouts++;
            }
        }

        double beamWidth = line.getEndBeam() + 1 - line.getStartBeam();
        double maxPercentage = maxedouts / beamWidth;
        double confidence = 1.0 - maxPercentage;

        checkState(Range.closed(0.0, 1.0).contains(confidence), "confidence value must be between 0.0 and 1.0");
        return confidence;
    }

}
