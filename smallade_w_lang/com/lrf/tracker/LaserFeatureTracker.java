/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 * Last update: April 2012
 *
 */
package com.lrf.tracker;

import com.algorithms.icp.IterativeClosestPoint;
import com.lrf.feature.Door;
import com.lrf.feature.IntersectionBranch;
import com.lrf.feature.IntersectionCenter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Door and intersection tracking module.
 * Tracks given doors, given intersection branches, and inferred intersection centers
 * Produces maps based upon tracked data
 */
public class LaserFeatureTracker {
    private final double MIN_PROXIMITY = 1.5; // In m, how far apart detected landmarks must be to register as independent landmarks
    private final double RIGHTANGLE = 90 * 0.017453; // In radians
    private final double NNDA_SCORE_THRESH = 15.0; // Arbitrary - predicts whether a detected landmark is unique or not
    private final double INTERSECTIONTRACKINGDECAY = 0.1; // the decay value for old intersectionbranches that are not verified
    private final double INTERSECTIONTRACKINGTHRESH = 0.5; // the confidence treshold for dropping out old intersectionbranches
    private final double TRANSFORM_DIST_LIMIT = 0.03; // In m
    protected Log log = LogFactory.getLog(getClass());
    // Landmarks
    private List<Door> doors;
    private List<IntersectionBranch> intersectionbranches; // Contains all IntersectionBranches tracked, even those in ICs
    private List<IntersectionCenter> intersectioncenters;
    private long currentDoorID = 0;
    private long currentIntersectionBranchID = 0;
    private long currentIntersectionCenterID = 0;
    // To-be-Added Landmarks
    private List<Door> doorstobeadded;
    private List<IntersectionBranch> ibstobeadded;
    // Wiping
    private int WIPE_THRESH = 100; // How many updates without isVisible landmarks it takes to wipe the data structures clean
    private int noVisLandmarkCounter = 0;
    // Flags
    private boolean doortracking = false;
    private boolean intersectiontracking = true;
    private boolean wiping = true;

    /** LaserFeatureTracker Constructor */
    public LaserFeatureTracker() {
        doors = new ArrayList<Door>();
        intersectionbranches = new ArrayList<IntersectionBranch>();
        intersectioncenters = new ArrayList<IntersectionCenter>();
        doorstobeadded = new ArrayList<Door>();
        ibstobeadded = new ArrayList<IntersectionBranch>();
    }

    private static double round(double v, int d) {
        return (double) ((int) (v * Math.pow(10, d))) / Math.pow(10, d);
    }

    // Returns the transformed point of the original point given a transform matrix
    private static Point2D transformPoint(Matrix4d transformMat, Point2D originalPoint) {
        Point3d tempPoint = new Point3d();
        transformMat.transform(new Point3d(originalPoint.getX(), originalPoint.getY(), 0), tempPoint);

        return new Point2D.Double(tempPoint.x, tempPoint.y);
    }

    // computes the angle between two lines
    static double getAngle(Line l1, Line l2) {
        double v1x = l1.endx - l1.startx;
        double v1y = l1.endy - l1.starty;
        double v2x = l2.endx - l2.startx;
        double v2y = l2.endy - l2.starty;
        return Math.acos(((v1x * v2x) + (v1y * v2y)) / (l1.length * l2.length));
    }

    // Given a list of Doors and a list of IntersectionBranches, update old landmarks and track new landmarks
    public void track(List<Door> ds, List<IntersectionBranch> ibs) {
        // For creating the transform matrix
        List<Point2D> oldTransformPoints = new ArrayList<>();
        List<Point2D> newTransformPoints = new ArrayList<>();

        // Reset visibilities of tracked landmarks
        resetVisibilities();

        if (((ds == null) || ds.isEmpty()) && ((ibs == null) || ibs.isEmpty())) {
            noVisLandmarkCounter++;
            if (noVisLandmarkCounter >= WIPE_THRESH) {
                wipe();
            }
        } else {
            noVisLandmarkCounter = 0;
        }


        // Track detected landmarks
        trackDoors(ds, oldTransformPoints, newTransformPoints);
        trackIntersectionBranches(ibs, oldTransformPoints, newTransformPoints);

        // Transform invisible landmarks
        transformInvisLandmarks(oldTransformPoints, newTransformPoints);

        // Begin tracking newly detected landmarks
        addNewLandmarks();
    }

    public ArrayList<Door> doors() {
        ArrayList<Door> alldoors = new ArrayList<Door>();
        for (Door d : doors) {
            alldoors.add((Door) d.copy());
        }
        return alldoors;
    }

    public ArrayList<IntersectionBranch> intersectionbranches() {
        ArrayList<IntersectionBranch> allibs = new ArrayList<IntersectionBranch>();
        for (IntersectionBranch ib : intersectionbranches) {
            allibs.add((IntersectionBranch) ib.copy());
        }
        return allibs;
    }

    public ArrayList<IntersectionCenter> intersectioncenters() {
        ArrayList<IntersectionCenter> allics = new ArrayList<IntersectionCenter>();
        for (IntersectionCenter ic : intersectioncenters) {
            allics.add((IntersectionCenter) ic.copy());
        }
        return allics;
    }

    private void trackDoors(List<Door> nds, List<Point2D> oldTransformPoints, List<Point2D> newTransformPoints) {
        if (doortracking) {
            if (nds == null) {
                return;
            }

            Door od;
            Door nd;

            // Stores NNDA scores representing how far each existing Door is from each newly detected Door
            // old landmarks x new landmarks
            double NNDAscores[][] = matrixNNDADoors(nds); // Nearest Neighbor Data Association

            for (int i = 0; i < nds.size(); i++) {
                nd = nds.get(i);
                if (doors.isEmpty()) {
                    nd.setID(currentDoorID++);
                    doorstobeadded.add(nd);
                } else {
                    od = closestExistingDoor(i, NNDAscores);
                    if (uniqueDoor(od, nd)) {
                        nd.setID(currentDoorID++);
                        doorstobeadded.add(nd);
                    } else {
                        nd.fill(od);
                        od.setVisible(true);
                        addDoorToTransformPoints(oldTransformPoints, od);
                        addDoorToTransformPoints(newTransformPoints, nd);
                    }
                }
            }
        }
    }

    private void trackIntersectionBranches(List<IntersectionBranch> nibs, List<Point2D> oldTransformPoints,
                                           List<Point2D> newTransformPoints) {
        if (intersectiontracking) {
            if (nibs == null) {
                return;
            }

            IntersectionBranch oib;
            IntersectionBranch nib;

            // Stores NNDA scores representing how far each existing IB is from each newly detected IB
            // old landmarks x new landmarks
            double NNDAscores[][] = matrixNNDAIBs(nibs); // Nearest Neighbor Data Association

            for (int i = 0; i < nibs.size(); i++) {
                nib = nibs.get(i);
                if (intersectionbranches.isEmpty()) {
                    log.trace("Adding IB " + currentIntersectionBranchID);
                    nib.setID(currentIntersectionBranchID++);
                    ibstobeadded.add(nib);
                } else {
                    oib = closestExistingIB(i, NNDAscores);
                    if (uniqueIntersectionBranch(oib, nib)) {
                        log.trace("Adding IB " + currentIntersectionBranchID);
                        nib.setID(currentIntersectionBranchID++);
                        ibstobeadded.add(nib);
                    } else {
                        nib.fill(oib);
                        oib.setvisible(true);
                        addIBToTransformPoints(oldTransformPoints, oib);
                        addIBToTransformPoints(newTransformPoints, nib);
                    }
                    //testForIntersectionCenter(ib);
                }
            }
        }
    }

    // Returns whether or not nd has been detected before.
    // If nd has been detected before, add it to newTransformPoints and its match to oldTransformPoints
    private boolean uniqueDoor(Door od, Door nd) {
        if (closeProximity(nd.getCenter(), od.getCenter(), MIN_PROXIMITY) &&
                closeProximity(nd.getApproach(), od.getApproach(), MIN_PROXIMITY) &&
                closeProximity(nd.entry(), od.entry(), MIN_PROXIMITY) &&
                closeProximity(nd.getExit(), od.getExit(), MIN_PROXIMITY)) {
            return false;
        }
        return true;
    }

    // Returns whether or not nib has been detected before.
    // If nib has been detected before, add it to newTransformPoints and its match to oldTransformPoints
    private boolean uniqueIntersectionBranch(IntersectionBranch oib, IntersectionBranch nib) {
        if (NNDAScoreIB(oib, nib) > NNDA_SCORE_THRESH) {
            log.trace("NNDA score is above threshold - therefore unique intersection");
            log.trace("NNDA Score: " + NNDAScoreIB(oib, nib));
            return true;
        }
        return false;
    }

    // Add each point that composes a Door into the list of transformPoints
    private void addDoorToTransformPoints(List<Point2D> transformPoints, Door d) {
        transformPoints.add(d.getCenter());
        transformPoints.add(d.getApproach());
        transformPoints.add(d.entry());
        transformPoints.add(d.getExit());
    }

    // Add each point that composes an Intersection into the list of transformPoints
    private void addIBToTransformPoints(List<Point2D> transformPoints, IntersectionBranch ib) {
        transformPoints.add(ib.entry());
        for (Point2D c : ib.corners()) {
            transformPoints.add(c);
        }
    }

    // Test if that all corners are the same
    private boolean uniqueCorners(IntersectionBranch nib, IntersectionBranch oib) {
        int nMatchedCorners = 0;

        for (Point2D oc : oib.corners()) {
            for (Point2D nc : nib.corners()) {
                if (closeProximity(oc, nc, MIN_PROXIMITY)) {
                    nMatchedCorners++;
                }
            }
        }

        if ((nMatchedCorners == oib.corners().size()) && (nMatchedCorners == nib.corners().size())) {
            return false;
        }

        return true;
    }

    // Nearest Neighbor Data Association
    // Returns the 2D array with corresponding NNDA scores between existing and newly-detected Doors
    private double[][] matrixNNDADoors(List<Door> nds) {
        double NNDAscores[][] = new double[doors.size()][nds.size()];
        Door od;
        Door nd;

        for (int o = 0; o < doors.size(); o++) {
            od = doors.get(o);
            for (int n = 0; n < nds.size(); n++) {
                nd = nds.get(n);
                NNDAscores[o][n] = NNDAScoreDoor(od, nd);
            }
        }

        return NNDAscores;
    }

    // Returns the 2D array with corresponding NNDA scores between existing and newly-detected IBs
    private double[][] matrixNNDAIBs(List<IntersectionBranch> nibs) {
        double NNDAscores[][] = new double[intersectionbranches.size()][nibs.size()];
        IntersectionBranch oib;
        IntersectionBranch nib;

        for (int o = 0; o < intersectionbranches.size(); o++) {
            oib = intersectionbranches.get(o);
            for (int n = 0; n < nibs.size(); n++) {
                nib = nibs.get(n);
                NNDAscores[o][n] = NNDAScoreIB(oib, nib);
                //log.trace("d[" + o + "][" + n + "]: " + NNDAscores[o][n]);
            }
        }

        return NNDAscores;
    }

    // Returns a score corresponding to the magnitude of the transformation from od to nd
    private double NNDAScoreDoor(Door od, Door nd) {
        return distanceApart(nd.entry(), od.entry()) +
                distanceApart(nd.getApproach(), od.getApproach()) +
                distanceApart(nd.getCenter(), od.getCenter()) +
                distanceApart(nd.getExit(), od.getExit());
    }

    // Returns a score corresponding to the magnitude of the transformation from oib to nib
    private double NNDAScoreIB(IntersectionBranch oib, IntersectionBranch nib) {
        Line ol = lineFromIB(oib);
        Line nl = lineFromIB(nib);

        double theta = getAngle(ol, nl);
        double dCorners = differenceInEndPoints(ol, nl); // Return sum of distances between closest corner pairs of two IBs
        //log.trace("NNDA Score: " + (distanceApart(nib.entry(), oib.entry()) + dCorners) * (1 + (theta%RIGHTANGLE)*3));
        return (distanceApart(nib.entry(), oib.entry()) + dCorners) * (1 + (theta % RIGHTANGLE) * 3);
    }

    private Line lineFromIB(IntersectionBranch ib) {
        if (ib == null) {
            System.err.println("Given IB is null; cannot form line");
            return null;
        }

        Line l = new Line();
        l.startx = ib.corners().get(0).getX();
        l.endx = ib.corners().get(1).getX();
        l.starty = ib.corners().get(0).getY();
        l.endy = ib.corners().get(1).getY();
        l.length = distanceApart(new Point2D.Double(l.startx, l.starty), new Point2D.Double(l.endx, l.endy));

        return l;
    }

    // Assumes all appropriate arrays are populated
    // Find the closest existing Door to the provided one
    private Door closestExistingDoor(int i, double[][] NNDAscores) {
        double min = Double.MAX_VALUE;
        int minIndex = 0;
        int j = 0;

        for (; j < doors.size(); j++) {
            if (NNDAscores[j][i] < min) {
                min = NNDAscores[j][i];
                minIndex = j;
            }
        }

        return doors.get(minIndex);
    }

    // Assumes all appropriate arrays are populated
    // Find the closest existing IntersectionBranch to the provided one
    private IntersectionBranch closestExistingIB(int i, double[][] NNDAscores) {
        double min = Double.MAX_VALUE;
        int minIndex = 0;
        int j = 0;

        for (; j < intersectionbranches.size(); j++) {
            if (NNDAscores[j][i] < min) {
                min = NNDAscores[j][i];
                minIndex = j;
            }
        }

        return intersectionbranches.get(minIndex);
    }

    private boolean closeProximity(Point2D pt1, Point2D pt2, double r) {
        return (distanceApart(pt1, pt2) < r);
    }

    private double distanceApart(Point2D pt1, Point2D pt2) {
        double deltaX = pt1.getX() - pt2.getX();
        double deltaY = pt1.getY() - pt2.getY();
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    private double differenceInEndPoints(Line ol, Line nl) {
        double sum = 0;
        Point2D.Double ostart = new Point2D.Double(ol.startx, ol.starty);
        Point2D.Double nstart = new Point2D.Double(nl.startx, nl.starty);
        Point2D.Double oend = new Point2D.Double(ol.endx, ol.endy);
        Point2D.Double nend = new Point2D.Double(nl.endx, nl.endy);

        double dOsNs = distanceApart(ostart, nstart);
        double dOsNe = distanceApart(ostart, nend);
        double dOeNs = distanceApart(oend, nstart);
        double dOeNe = distanceApart(oend, nend);

        return minSumEndPoints(dOsNs, dOsNe, dOeNs, dOeNe);
    }

    // Return the smallest sum possible from the end points of one line (O) to the other (N)
    private double minSumEndPoints(double dOsNs, double dOsNe, double dOeNs, double dOeNe) {
        if (dOsNs < dOsNe) {
            if (dOeNs < dOeNe) {
                return dOsNs + dOeNs;
            }
            return dOsNs + dOeNe;
        } else {
            if (dOeNs < dOeNe) {
                return dOeNe + dOeNs;
            }
        }
        return dOsNe + dOeNe;
    }

    // Update invisible landmarks with transformMatrix
    private void transformInvisLandmarks(List<Point2D> oldTransformPoints, List<Point2D> newTransformPoints) {
        if (oldTransformPoints.isEmpty() || newTransformPoints.isEmpty()) {
            return;
        }

        // Convert ArrayList<Point2D.Double> to double[][]
        double[][] oldpointset = pointset(oldTransformPoints);
        double[][] newpointset = pointset(newTransformPoints);

        // Calculate transformMatrix from the isVisible landmarks' points
        Matrix4d transformMatrix = IterativeClosestPoint.transformMatFromCorrPoints(oldpointset, newpointset);

        // Update the positions of all landmarks
        updateInvisLandmarks(transformMatrix);
    }

    // Add newly-tracked landmarks to the list of old landmarks
    private void addNewLandmarks() {
        for (Door d : doorstobeadded) {
            doors.add(d);
        }

        for (IntersectionBranch ib : ibstobeadded) {
            intersectionbranches.add(ib);
        }

        doorstobeadded.clear();
        ibstobeadded.clear();
    }

    // Convert a List<Point2D.Double> into a double[][]
    // pointset[i][0] == pts.get(i).getX()
    // pointset[i][1] == pts.get(i).getY()
    // pointset[i][2] == 0     // Originally the 'z' coordinate value
    private double[][] pointset(List<Point2D> pts) {
        double[][] pointset = new double[pts.size()][3];
        //log.trace("pts.size(): " + pts.size());

        for (int i = 0; i < pts.size(); i++) {
            pointset[i][0] = pts.get(i).getX();
            pointset[i][1] = pts.get(i).getY();
            pointset[i][2] = 0;
        }

        /*for (int i=0; i < pointset.length; i++) {
            for (int j=0; j < pointset[i].length; j++) {
                log.trace("pointset["+i+"]["+j+"]: " + pointset[i][j]);
            }
        }*/
        return pointset;
    }

    // Convert a double[][] into a List<Point2D.Double>
    // pts.get(i).getX() == pointset[i][0]
    // pts.get(i).getY() == pointset[i][1]
    // Ignores z-coordinate (pointset[i][2])
    private ArrayList<Point2D.Double> fromPointset(double[][] pointset) {
        ArrayList<Point2D.Double> pts = new ArrayList<Point2D.Double>();

        for (int i = 0; i < pointset.length; i++) {
            pts.add(new Point2D.Double(pointset[i][0], pointset[i][1]));
        }

        return pts;
    }

    // Update isVisible landmarks according to detection readings, and apply the transformMatrix to invisible landmarks
    // Where a landmark is either a Door or an Intersection
    private void updateInvisLandmarks(Matrix4d tMat) {
        for (Door d : doors) {
            if (!d.isVisible()) {
                transformDoor(tMat, d);
            }
        }

        // TODO: ICs not included
        int ibInvis = 0;
        for (IntersectionBranch ib : intersectionbranches) {
            if (!ib.visible()) {
                ibInvis++;
                transformIB(tMat, ib);
            }
        }

        //log.trace(ibInvis + " out of " + intersectionbranches.size() + " IBs are invisible");
    }

    // Transform each point that composes a Door
    private void transformDoor(Matrix4d tMat, Door d) {
        d.setCenter(transformPoint(tMat, d.getCenter()));
        d.setApproach(transformPoint(tMat, d.getApproach()));
        d.setEntry(transformPoint(tMat, d.entry()));
        d.setExit(transformPoint(tMat, d.getExit()));
    }

    // Transform each point that composes an IntersectionBranch
    private void transformIB(Matrix4d tMat, IntersectionBranch ib) {
        Point2D testPt = transformPoint(tMat, ib.entry());
        // TEST: Compare testPt and ib.entry()
        /*log.trace("old point vs. transformed point");
        log.trace("dX: " + (testPt.getX() - ib.getEntryY()));
        log.trace("dY: " + (testPt.getY() - ib.getEntryY()));
         */

        if (distanceApart(testPt, ib.entry()) > TRANSFORM_DIST_LIMIT) {
            return;
        }

        ib.setentry(transformPoint(tMat, ib.entry()));

        // Transform the ib's corners
        Set<Point2D> transformedCorners = new HashSet<>();
        for (Point2D c : ib.corners()) {
            transformedCorners.add(transformPoint(tMat, c));
        }
        ib.clearCorners();
        for (Point2D c : transformedCorners) {
            ib.addCorner(c);
        }
    }

    // Determine how to classify the given, newly-tracked intersection branch (nib)
    // If nib and an intersection center's branch are connected, add nib to that intersection center
    // If nib and an orphaned branch are connected, form a new intersection center for them
    // If they are not connected, continue through the loop
    private void testForIntersectionCenter(IntersectionBranch nib) {
        // Check if nib can be placed into an existing intersection center
        for (IntersectionCenter ic : intersectioncenters) {
            if (ic.containsBranch(nib)) {
                ic.addBranch(nib);
                return;
            }
        }

        // Check if a new intersection center can be formed by pairing nib with another orphaned intersection branch
        for (IntersectionBranch oib : intersectionbranches) {
            if (cornerInCommon(nib, oib)) {
                intersectioncenters.add(new IntersectionCenter(nib, oib, currentIntersectionCenterID++));
                return;
            }
        }
    }

    private boolean cornerInCommon(IntersectionBranch ib1, IntersectionBranch ib2) {
        double tolerance = 0.1; // in m

        for (Point2D cib1 : ib1.corners()) {
            for (Point2D cib2 : ib2.corners()) {
                if (closeProximity(cib1, cib2, tolerance)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void resetVisibilities() {
        for (Door d : doors) {
            d.setVisible(false);
        }

        for (IntersectionBranch ib : intersectionbranches) {
            ib.setvisible(false);
        }
    }

    private ArrayList<Door> invisibleDoors() {
        ArrayList<Door> invisibleDoors = new ArrayList<Door>();

        for (Door d : doors) {
            if (!d.isVisible()) {
                invisibleDoors.add(d);
            }
        }

        return invisibleDoors;
    }

    private ArrayList<IntersectionBranch> invisibleIBs() {
        ArrayList<IntersectionBranch> invisibleIBs = new ArrayList<IntersectionBranch>();

        for (IntersectionBranch ib : intersectionbranches) {
            if (!ib.visible()) {
                invisibleIBs.add(ib);
            }
        }

        return invisibleIBs;
    }

    private void wipe() {
        doors = new ArrayList<Door>();
        intersectionbranches = new ArrayList<IntersectionBranch>();
        intersectioncenters = new ArrayList<IntersectionCenter>();
        doorstobeadded = new ArrayList<Door>();
        ibstobeadded = new ArrayList<IntersectionBranch>();
        currentDoorID = 0;
        currentIntersectionBranchID = 0;
        currentIntersectionCenterID = 0;
        noVisLandmarkCounter = 0;
    }

    static class Line {
        double startx;
        double starty;
        double endx;
        double endy;
        double length;
    }
}
