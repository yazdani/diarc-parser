package com.lrf.extractor;

import com.LaserScan;
import com.lrf.feature.Door;
import com.lrf.feature.Line;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jeremiah Via <jeremiah.via@gmail.com>
 */
public class DoorExtractor extends AbstractExtractor<Door> {

    /**
     * The lower tolerance for right angles in radians.
     */
    private final int[] sideThresholds = {60, 120};
    public int[] possibleDoor; // Each index corresponds to a laser reading
    LineExtractor lineExtractor;
    PointExtractor pointExtractor;
    private Log log = LogFactory.getLog(getClass());

    public DoorExtractor(FeatureModel model) {
        super(model);
        lineExtractor = new LineExtractor(model);
        pointExtractor = new PointExtractor(model);
    }

    @Override
    public List<Door> extract(LaserScan scan) {
        List<Door> doors = detectDoors(scan, lineExtractor.extract(scan), pointExtractor.extract(scan));
        log.debug(String.format("Extracted %d doors", doors.size()));
        return detectDoors(scan, lineExtractor.extract(scan), pointExtractor.extract(scan));
    }

    List<Door> detectDoors(LaserScan scan, List<Line> lines, List<Point2D> points) {
        possibleDoor = new int[scan.ranges.length];


        // this is the open door model:
        //
        //          OPEN_DOOR_DEPTH_MIN
        // ____________|          |_______   _
        //    l-1      l  l+1   l+2  l+3
        // where either at least the point l+2 needs to be close to the line or i needs to be close to the line l+3
        //
        List<Door> doors = new ArrayList<>();
        for (int l = 1; l < lines.size(); l++) {
            double alpha = Math.abs(lines.get(l - 1).angleTo(lines.get(l)));
            // check if the lines are *not* parallel, then we have a discontinuity
            if (model.parallel().getThreshold() < alpha && alpha < Math.PI - model.parallel().getThreshold()) {
                Line prev = lines.get(l - 1);
                Line curr = lines.get(l);

                double x2minusx1 = prev.getEnd().getX() - prev.getStart().getX();
                double y2minusy1 = prev.getEnd().getY() - prev.getStart().getY();
                double root = prev.getLength();
                //now find the next starting point of a line that is roughly one door width away from the end point in l-1
                // and not by more than OFFLINETOLERANCE away from the line
                for (int m = l + 1; m < lines.size(); m++) {
                    // minimum length of a line
                    if (lines.get(m).getLength() < model.door().getMinLineLength())
                        continue;
                    double x1minusxi = prev.getEnd().getX() - lines.get(m).getStart().getX(); // X[k];
                    double y1minusyi = prev.getEnd().getY() - lines.get(m).getStart().getY(); // Y[k];
                    double d = Math.abs(x2minusx1 * y1minusyi - x1minusxi * y2minusy1) / root;
                    // compute it in the reversed direction too
                    double x2minusx1R = lines.get(m).getLength();
                    double y2minusy1R = lines.get(m).getLength();
                    double rootR = Math.sqrt(x2minusx1 * x2minusx1 + y2minusy1 * y2minusy1);
                    double dR = Math.abs(-x2minusx1R * y1minusyi + x1minusxi * y2minusy1R) / rootR;
                    double beta = Math.abs(prev.angleTo(lines.get(m)));
                    // MS: make this a bit more sophisticated: lines on both sides of the doorway are parallel, or
                    //     one side is perpendicular (but do not allow for both sides to be perpendicular)
                    if (((beta < model.parallel().getThreshold()
                            || beta > Math.PI - model.parallel().getThreshold())
                            && d < model.line().getOffLineTolerance() && dR < model.line().getOffLineTolerance()) ||
                            (model.rightAngle().getLowerBound() < beta
                                    && beta < model.rightAngle().getUpperBound())
                                    && (d < model.line().getOffLineTolerance() || dR < model.line().getOffLineTolerance())) {
//                        log.debug("D: " + d);
                        // check if the distances between it and the endpoint of the is within the door width
                        double opening = prev.getEnd().distance(lines.get(m).getStart());
                        if (model.door().getMinWidth() < opening && opening < model.door().getMaxWidth()) {
//                            log.debug("OPENING: " + opening + " l " + l + "  m " + m);

                            // MS: took this out to allow for matching of open door from inside rooms
                            // mark the startBeam as a possible door
                            possibleDoor[curr.getStartBeam()] = 1;
                            // get the start index of the new line
                            int upperIndex = lines.get(m).getStartBeam();
                            // now check if the lines on the other side continue either in parallel to the first line
                            // or at some angle
                            int lowerIndex = prev.getEndBeam();
                            // now check that the beam's end points longer than what a straight line would predict
                            int numbeams = upperIndex - lowerIndex;
                            if (numbeams < model.door().getMinNumBeams() || m == l + 1) {
                                // MS: this was break before, but that makes the algorithm skip lines...
                                continue; // not enough data points, not enough lines
                            }
                            boolean distancesOK = true;
                            double mindist = 0;

                            // check that all lines in doors are "behind the door threshold"
                            if (scan.ranges[lowerIndex] <= scan.ranges[upperIndex]) {
                                for (int c = l + 1; c < m; c++) {
                                    // if (scan.ranges[lines.get(c).startBeam] - OPEN_DOOR_DEPTH_MIN <= (scan.ranges[i] +scan.ranges[j])/2) {
                                    double dist = scan.ranges[lines.get(c).getStartBeam()] - (scan.ranges[lowerIndex] + scan.ranges[upperIndex]) / 2;
                                    if (dist <= 0) {
                                        distancesOK = false;
                                        break;
                                    } else
                                        mindist += dist;
                                }
                            } else {
                                for (int c = l + 1; c < m; c++) {
                                    // if (scan.ranges[lines.get(c).startBeam] - OPEN_DOOR_DEPTH_MIN <= (scan.ranges[j] + scan.ranges[i])/2) {
                                    double dist = scan.ranges[lines.get(c).getStartBeam()] - (scan.ranges[upperIndex] + scan.ranges[lowerIndex]) / 2;
                                    if (dist <= 0) {
                                        distancesOK = false;
                                        break;
                                    } else
                                        mindist += dist;
                                }
                            }
                            // MS: another "break" but, needs to be continue here
                            if (!distancesOK || mindist / Math.abs(m - l - 1) < model.door().open().getMinDepth()) {
                                continue;
                            }

                            log.debug("FOUND ONE between " + prev.getEndBeam() + " and " + upperIndex);
                            int centerindex = lowerIndex + (int) ((upperIndex - lowerIndex) / 2 + 0.5);
                            double centerdist = (scan.ranges[centerindex] + scan.ranges[centerindex + 1]) / 2;
                            possibleDoor[upperIndex] = 2;
                            possibleDoor[lowerIndex] = 2;
                            double depth = doorDepth(lowerIndex, upperIndex, scan);
                            // compute the entry point, which is 1.5 times the door width in front of the door
                            // also check if the door is left or right
                            double doorvecx = (points.get(upperIndex).getX() - points.get(lowerIndex).getX()) / 2;
                            double doorvecy = (points.get(upperIndex).getY() - points.get(lowerIndex).getY()) / 2;
                            Point2D centerPoint = new Point2D.Double(points.get(lowerIndex).getX() + doorvecx, points.get(lowerIndex).getY() + doorvecy);

                            // PWS: 120 and 60 don't work with nLaserReadings != 180/181
                            Point2D entryPoint;
                            if (centerindex > sideThresholds[1]) {
                                entryPoint = new Point2D.Double(points.get(lowerIndex).getX() + doorvecx * 6 / 6 - doorvecy * 1.0,
                                        points.get(lowerIndex).getY() + doorvecy * 4 / 6 + doorvecx * 1.0);
                            } else if (centerindex < sideThresholds[0]) {
                                entryPoint = new Point2D.Double(points.get(lowerIndex).getX() + doorvecx * 6 / 6 - doorvecy * 1.0,
                                        points.get(lowerIndex).getY() + doorvecy * 8 / 6 + doorvecx * 1.0);
                            } else {
                                entryPoint = new Point2D.Double(points.get(lowerIndex).getX() + doorvecx - doorvecy * 1.0,
                                        points.get(lowerIndex).getY() + doorvecy + doorvecx * 1.0);
                            }

                            Point2D exitPoint = new Point2D.Double(points.get(lowerIndex).getX() + doorvecx + doorvecy * 2.0, points.get(lowerIndex).getY() + doorvecy - doorvecx * 2.0);
                            Point2D approachPoint = new Point2D.Double(points.get(lowerIndex).getX() + doorvecx - doorvecy * 2.00, points.get(lowerIndex).getY() + doorvecy + doorvecx * 2.00);

                            Door nd = new Door(
                                    lowerIndex, centerindex, upperIndex, depth, centerdist,
                                    centerPoint, entryPoint, exitPoint, approachPoint, 0.9, Door.State.OPEN);
                            doors.add(nd);
                            // advance the line counter
                            l = m;
                            break;
                        }
                    }
                }
            }
        }

        return doors;
    }

    // Returns average laser length (average door depth) for a given range
    private double doorDepth(int indexS, int indexE, LaserScan scan) {
        double sum = 0;
        for (int i = indexS; i < indexE; i++) {
            sum += scan.ranges[i];
        }
        return (sum / (indexE - indexS));
    }


}
