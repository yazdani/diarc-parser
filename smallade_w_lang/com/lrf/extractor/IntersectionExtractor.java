package com.lrf.extractor;

import com.LaserScan;
import com.lrf.feature.IntersectionBranch;
import com.lrf.feature.Line;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class IntersectionExtractor extends AbstractExtractor<IntersectionBranch> {
    private LineExtractor lineExtractor;
    private List<Line> lines;
    private List<IntersectionBranch> intersectionBranches;

    public IntersectionExtractor(FeatureModel model) {
        super(model);
        lineExtractor = new LineExtractor(model);
    }

    public List<IntersectionBranch> extract(LaserScan scan) {
        intersectionBranches = new ArrayList<>();
        lines = lineExtractor.extract(scan);

        // this is the hallway opening intersection model:
        // Where any combination of the four possible branches forms hallway openings
        // These come in three varieties:
        //    L-branches (2-way openings), T-branches (3-way openings), and 4-way intersections
        //
        //           |         |
        //    _______|         |_______
        //
        //    ________         ________
        //           |         |
        //           |         |

        detectIBsFromOrthogonalLines();
        detectIBsFromParallelLines();

        return intersectionBranches;

    }

    // If there is an IB between lines.get(l) and a parallel match, it is added to intersectionBranches
    private void checkParallelPairs(int l) {
        int i = 2;
        Line lineS = lines.get(l);
        Line lineE = nextParallelLine(l + 1, lineS);

        while (lineE != null) {
            if (calculatePossibleParallelIB(lineS, lineE)) { // Has a viable IB been found between lineS and lineE?
                return;
            }
            lineE = nextParallelLine(l + i, lineE);
            i++;
        }
    }

    // Returns the point where lineS extends into lineE to form a would-be intersection
    private Point2D.Double intersectionPoint(Line lineS, Line lineE) {
        double x1 = lineS.getStart().getX();
        double x2 = lineS.getEnd().getX();
        double x3 = lineE.getStart().getX();
        double x4 = lineE.getEnd().getX();
        double y1 = lineS.getStart().getY();
        double y2 = lineS.getEnd().getY();
        double y3 = lineE.getStart().getY();
        double y4 = lineE.getEnd().getY();
        double px;
        double py;

        try {
            px = ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / ((x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4));
            py = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / ((x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4));
            return new Point2D.Double(px, py);
        } catch (Exception e) {
            log.error("Cannot divide by 0.", e);
        }

        return null;
    }

    // Returns true if a viable IB has been found between lineS and lineE, false otherwise
    // Side effect: Any viable IB found is added to intersectionBranches
    private boolean calculatePossibleParallelIB(Line lineS, Line lineE) {
        Point2D cornerS;
        Point2D cornerE;
        Line normal;

        if (lineS.getLength() < lineE.getLength()) {
            cornerS = lineS.getEnd();
            normal = lineS.normal(cornerS);
            cornerE = intersectionPoint(normal, lineE);
        } else {
            cornerS = lineE.getStart();
            normal = lineE.normal(cornerS);
            cornerE = intersectionPoint(normal, lineS);
        }

        double hallwayWidth = cornerS.distance(cornerE);
        if ((hallwayWidth > model.intersection().getMaxWidth()) || (hallwayWidth < model.intersection().getMinWidth())) {
            return false;
        }

        // IBE = Intersection Branch Entry (Point)
        double xIBE = (cornerS.getX() + cornerE.getX()) / 2;
        double yIBE = (cornerS.getY() + cornerE.getY()) / 2;
        Point2D possibleIBE = new Point2D.Double(xIBE, yIBE);
        double measuredDepth = lineS.getLength();
        double confidence = intersectionConfidence(measuredDepth);

        if (likelyIB(possibleIBE, lineS, confidence)) {
            intersectionBranches.add(new IntersectionBranch(xIBE, yIBE, confidence, measuredDepth, cornerS, cornerE));
            return true;
        }

        return checkOtherHallwayEnd(lineS, lineE);
    }

    // Returns true if a viable IB is found, false otherwise
    // Side effect: Adds viable IB if found
    private boolean checkOtherHallwayEnd(Line lineS, Line lineE) {
        Point2D cornerS;
        Point2D cornerE;
        Point2D possibleIBE;
        Line normal;
        double hallwayWidth;
        double xIBE;
        double yIBE;
        double measuredDepth;
        double confidence;

        if (lineS.getLength() < lineE.getLength()) {
            cornerS = lineS.getStart();
            normal = lineS.normal(cornerS);
            cornerE = intersectionPoint(normal, lineE);
        } else {
            cornerS = lineE.getEnd();
            normal = lineE.normal(cornerS);
            cornerE = intersectionPoint(normal, lineS);
        }

        hallwayWidth = cornerS.distance(cornerE);
        if ((hallwayWidth > model.intersection().getMaxWidth()) || (hallwayWidth < model.intersection().getMinWidth())) {
            return false;
        }

        xIBE = (cornerS.getX() + cornerE.getX()) / 2;
        yIBE = (cornerS.getY() + cornerE.getY()) / 2;
        possibleIBE = new Point2D.Double(xIBE, yIBE);
        measuredDepth = lineS.getLength();
        confidence = intersectionConfidence(measuredDepth);

        if (likelyIB(possibleIBE, lineS, confidence)) {
            intersectionBranches.add(new IntersectionBranch(xIBE, yIBE, confidence, measuredDepth, cornerS, cornerE));
//            log.debug("Found an alternative parallel IB");
            return true;
        }

        return false;
    }

    // Returns the next line found in lines[] that is || to lineS, starting at l+1
    // Returns null if there is no next parallel line
    private Line nextParallelLine(int l, Line lineS) {
        if (l < 1) {
            return null;
        }

        double alpha;
        Line lineE;
        double parallelDegree = model.parallel().getThreshold();
        for (int i = l; i < lines.size() - 1; i++) {
            lineE = lines.get(i + 1);
            alpha = lineS.angleTo(lineE);
            if (parallelDegree > alpha || alpha > Math.PI - parallelDegree) {
                return lineE;
            }
        }
        return null;
    }

    private void detectIBsFromParallelLines() {
        for (int l = 0; l < lines.size(); l++) {
            if (lines.get(l).getLength() < model.intersection().getMinLength())
                continue;
            checkParallelPairs(l);
        }
    }

    // Detect IBs that can be detected from orthogonal lines, iterating through the lines incrementally and the decrementally
    private void detectIBsFromOrthogonalLines() {
        for (int l = 0; l < lines.size(); l++) {
            if (lines.get(l).getLength() < model.intersection().getMinLength())
                continue;
            checkOrthogonalPairs(l);
        }
    }

    // If there is an IB between lines.get(l) and an orthogonal match, it is added to intersectionBranches
    private void checkOrthogonalPairs(int l) {
        int i = 2;
        Line lineS = lines.get(l);
        Line lineE = nextOrthogonalLine(l + 1, lineS);

        while (lineE != null) {
            if (calculatePossibleOrthogonalIB(lineS, lineE)) { // Has a viable IB been found between lineS and lineE?
                return;
            }
            lineE = nextOrthogonalLine(l + i, lineE);
            i++;
        }
    }

    // Returns the next line found in lines[] that is orthogonal to lineS, starting at l+1
    // Returns null if there is no next orthogonal line
    private Line nextOrthogonalLine(int l, Line lineS) {
        if (l < 1) {
            return null;
        }

        double alpha;
        Line lineE;

        for (int i = l; i < lines.size() - 1; i++) {
            lineE = lines.get(i + 1);
            alpha = lineS.angleTo(lineE);
            if (model.rightAngle().getUpperBound() > alpha &&
                    alpha > model.rightAngle().getLowerBound()) {
//                log.debug("Lines " + (l - 1) + " and " + (i + 1) + " are orthogonal");
                return lineE;
            }
        }
        return null;
    }

    private boolean calculatePossibleOrthogonalIB(Line lineS, Line lineE) {
        Point2D cornerS = intersectionPoint(lineS, lineE);
        Point2D cornerE;

        if (lineE.getLength() < lineS.getLength()) {
            cornerE = lineE.getStart();
        } else {
            cornerE = lineS.getEnd();
        }

        double hallwayWidth = cornerS.distance(cornerE);

        if ((hallwayWidth > model.intersection().getMaxWidth()) || (hallwayWidth < model.intersection().getMinWidth())) {
            return false;
        }

        // IBE = Intersection Branch Entry (Point)
        double xIBE = (cornerS.getX() + cornerE.getX()) / 2;
        double yIBE = (cornerS.getY() + cornerE.getY()) / 2;
        Point2D.Double possibleIBE = new Point2D.Double(xIBE, yIBE);
        double measuredDepth = Math.abs(lineS.getLength() - lineE.getLength());
        double confidence = intersectionConfidence(measuredDepth);

        if (likelyIB(possibleIBE, lineS, confidence)) {
            intersectionBranches.add(new IntersectionBranch(xIBE, yIBE, confidence, measuredDepth, cornerS, cornerE));
            return true;
        }

        return false;
    }

    private double intersectionConfidence(double depth) {
        double confidence = depth / model.intersection().getMinDepth();
        if (confidence > 1.0)
            confidence = 1.0;

        return confidence;
    }

    private boolean likelyIB(Point2D possibleIBE, Line lineS, double confidence) {
        return (confidence > model.intersection().getDetectionThreshold()) &&
                pointInsideRoom(possibleIBE) &&
                lineS.intersectsAny(lines) &&
                uniqueIBE(possibleIBE);
    }

    // Return a java Polygon formed from the points of each Line in lines
    private Polygon room(List<Line> lines, int precision) {
        int[] xs = new int[lines.size() + 1];
        int[] ys = new int[lines.size() + 1];

        for (int i = 0; i < lines.size(); i++) {
            xs[i] = (int) lines.get(i).getStart().getX() * precision;
            ys[i] = (int) lines.get(i).getStart().getY() * precision;
        }
        xs[lines.size()] = (int) lines.get(lines.size() - 1).getEnd().getX() * precision;
        ys[lines.size()] = (int) lines.get(lines.size() - 1).getEnd().getY() * precision;
        // xs and ys are now completely filled with the defining vertices of the polygon

        return new Polygon(xs, ys, lines.size() + 1);
    }

    // Check to see if a given IntersectionBranch is too close to an existing IntersectionBranch
    private boolean uniqueIBE(Point2D possibleIB) {
        Point2D IB;
        for (IntersectionBranch inter : intersectionBranches) {
            IB = inter.entry();
//            log.debug("dist apart: " + IB.distance(possibleIB));
            if (IB.distance(possibleIB) < model.intersection().getTrackingThreshold()) {
                return false;
            }
        }
        return true;
    }

    private boolean pointInsideRoom(Point2D p) {
        int precision = 10000;
        Polygon room = room(lines, precision);
        Point2D tp = new Point2D.Double(p.getX() * precision, p.getY() * precision);

        return room.contains(tp);
    }


}
