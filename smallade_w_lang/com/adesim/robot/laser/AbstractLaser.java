package com.adesim.robot.laser;

import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import com.adesim.datastructures.MountingPoint;
import com.adesim.datastructures.SimShape;
import com.adesim.robot.SimAbstractRobot;
import com.adesim.robot.interfaces.LaserCarrying;
import com.adesim.util.SimUtil;

/**
 * an abstract class for lasers used in ADESim.  The one "required" method that needs to be
 * called before the laser is used (i.e., during the construction of a robot, for instance) is
 * "mount" which will place and orient the laser appropriately
 */
public abstract class AbstractLaser {
    private MountingPoint mountingPoint;

    private static final int NUM_SECTORS = 3;

    private int numLaserReadings;
    private double laserOffset;
    private double maxLaserDistance;
    private boolean usingCoords = false;
    private Point2D.Double transOrigin;

    private ArrayList<Point2D> robotCoords;
    private List<Line2D> currentLaserLines;
    private double[] currentLaserDistances;
    private boolean[] safeSpaces;
    private boolean[] openSpaces;


    ////////////////////////////////////////////////
    //   ABSTRACT METHODS THAT NEED OVERRIDING    //
    ////////////////////////////////////////////////

    public abstract double getAngleMin();

    public abstract double getAngleMax();

    public abstract double getAngleIncrement();

    public abstract double getRangeMin();

    public abstract double getRangeMax();


    ////////////////////////////////////////////////
    ////////////////////////////////////////////////


    /**
     * construct laser based on number of readings and max laser distance.
     *
     * @param mountingPointOnRobot the laser is mounted on the "ideal platonic form" of the robot
     * @param numLaserReadings:    number of laser "lines", one greater than the
     *                             number of degrees.  thus, for a 180-degree laser, there's 181 lines.
     * @param maxLaserDistance:    maximum distance that the laser can detect.
     */
    public AbstractLaser(MountingPoint mountingPointOnRobot, int numLaserReadings, double laserOffset, double maxLaserDistance) {
        this.mountingPoint = mountingPointOnRobot;
        this.numLaserReadings = numLaserReadings;
        this.laserOffset = mountingPointOnRobot.degreeOffsetFromFront;
        //laserOffset;
        this.maxLaserDistance = maxLaserDistance;
    }

    public AbstractLaser(MountingPoint mountingPointOnRobot, int numLaserReadings, double laserOffset, double maxLaserDistance, ArrayList<Point2D> robotCoords) {
        this.mountingPoint = mountingPointOnRobot;
        this.numLaserReadings = numLaserReadings;
        this.laserOffset = mountingPointOnRobot.degreeOffsetFromFront;
        //laserOffset;
        this.maxLaserDistance = maxLaserDistance;
        this.robotCoords = robotCoords;
        usingCoords = true;
    }


    /**
     * the most important method -- the one that updates the laser readings
     */
    public void updateLaserReadings(SimAbstractRobot robot) {
        List<Line2D> realLaserLines = new ArrayList<Line2D>();

        AffineTransform robotTransform = robot.getLocationSpecifier().getTransformation();
        Point2D transformedLaserOrigin = robotTransform.transform(
                mountingPoint.point.point2D, null);
        //	this.transOrigin = new Point2D.Double((Double) transformedLaserOrigin.getX(), (Double) transformedLaserOrigin.getY());

        List<Line2D> maxRangeLaserLines = this.generateLaserLinesToMaxRange(robotTransform);

        double laserLineHeight = mountingPoint.point.z + robot.getGroundHeight();
        // the laser line is just a line -- the min and max matches are the same:
        Iterable<SimShape> laserables = robot.getModel().getLaserVisibleShapes(
                new double[]{laserLineHeight, laserLineHeight});

        double[] newDistances = new double[maxRangeLaserLines.size()];
        // Initialize laser distances to max
        for (int j = 0; j < maxRangeLaserLines.size(); j++) {
            newDistances[j] = maxLaserDistance;
        }

        int laserCounter = 0;
        // for each beam of lasers
        for (Line2D laserLine : maxRangeLaserLines) {
            // Step through each laserable object:
            Point2D farLaserPoint = laserLine.getP2();

            for (SimShape anObject : laserables) {
                // Look at each side of each laser-visible object
                for (Line2D objectLine : anObject.lines) {
                    Point2D inter = SimUtil.lineIntersection(laserLine, objectLine);
                    if (inter != null) {
                        // Found intersection, see if it's closest
                        double d = inter.distance(transformedLaserOrigin);
                        if (d < newDistances[laserCounter]) {
                            newDistances[laserCounter] = d;
                            farLaserPoint = inter;
                        }
                    }
                }
            }

            realLaserLines.add(new Line2D.Double(transformedLaserOrigin, farLaserPoint));
            laserCounter++;
        }

        currentLaserLines = realLaserLines;
        currentLaserDistances = newDistances;

        calculateSafeAndOpenSpaces((LaserCarrying) robot);
    }


    private List<Line2D> generateLaserLinesToMaxRange(AffineTransform robotTransform) {
        List<Line2D> lines = new ArrayList<Line2D>();
        Point2D transformedLaserOrigin = robotTransform.transform(mountingPoint.point.point2D, null);
        //		int firstReadingDegree = ((int)Math.toDegrees(this.laserOffset) + 90 - numLaserReadings/2);
        int firstReadingDegree = (int) (90 - numLaserReadings / 2);
        // for calculation
        for (int i = 0; i < numLaserReadings; i++) {
            double angle = Math.toRadians(firstReadingDegree + i) + mountingPoint.degreeOffsetFromFront;
            Point2D platonicPointEnd = new Point2D.Double(
                    maxLaserDistance * Math.sin(angle),
                    maxLaserDistance * Math.sin(angle - (Math.PI / 2.0))); // this is NOT cosine
            // NOTE: the sine notion above is opposite of what you might expect,
            //    since the "platonic" position of the robot is on its side, facing right
            //    (both for historic reasons, and so that it matches a 0 orientation),
            //    whereas I want the first ray to go south, be east-facing for the middle
            //    ray, and finally point north at the end.
            //    it's ok, just go with the math...
            //    also, while it might look like the second sin equation should just
            //    reduce to cosine, it doesn't; it's the opposite phase shift.

            Line2D transformedLine = new Line2D.Double(transformedLaserOrigin,
                    robotTransform.transform(platonicPointEnd, null));
            lines.add(transformedLine);
        }

        return lines;
    }

    /**
     * @author TEW
     */
    private double[] adjustDistancesAroundHull(double[] distances) {
        double bearingOfThisLaser;
        double thisLaserLength;
        double numLs = new Double(distances.length);
        Point2D.Double pointRepresentation = new Point2D.Double(0.0, 0.0);
        for (int index = 0; index < distances.length; index++) {
            thisLaserLength = distances[index];
            bearingOfThisLaser = ((Math.toRadians(numLs) / 2.0) + Math.PI + laserOffset - Math.PI / 2) % (2 * Math.PI); //Angle of first reading accounting for offset;
            bearingOfThisLaser = (bearingOfThisLaser + index * (Math.toRadians(numLs) / numLs)) % (2 * Math.PI); //Angle of THIS reading.
            pointRepresentation.setLocation(
                    thisLaserLength * Math.cos(bearingOfThisLaser) + mountingPoint.point.point2D.getX(),
                    thisLaserLength * Math.sin(bearingOfThisLaser) + mountingPoint.point.point2D.getY());
            //	    System.out.println("Before transform: "+distances[index]);
            distances[index] = shortestDistanceToBot(pointRepresentation);
            //  System.out.println("After transform: "+distances[index]);
        }
        //	System.out.println("\n\n************\n\n");
        return distances;
    }

    private double shortestDistanceToBot(Point2D.Double laserEndPoint) {
        Point2D robotCornerA;
        Point2D robotCornerB;
        double distanceToObstacle = 9999999999.0;
        for (int segmentIndex = 0; segmentIndex < robotCoords.size() - 1; segmentIndex++) {
            robotCornerA = robotCoords.get(segmentIndex);
            robotCornerB = robotCoords.get((segmentIndex + 1) % robotCoords.size()); //so the last iteration can wrap around
            distanceToObstacle = Math.min(distanceToObstacle, Line2D.ptSegDist(robotCornerA.getX(), robotCornerA.getY(),
                    robotCornerB.getX(), robotCornerB.getY(),
                    laserEndPoint.getX(), laserEndPoint.getY()));
        }
        return distanceToObstacle;
    }

    private void calculateSafeAndOpenSpaces(LaserCarrying robot) {
        double[] distances = Arrays.copyOf(getCurrentLaserDistances(), currentLaserDistances.length);
        if (usingCoords)
            distances = adjustDistancesAroundHull(distances);
        boolean[] newSafe = new boolean[NUM_SECTORS];
        for (int k = 0; k < NUM_SECTORS; k++) {
            newSafe[k] = getSectorSafe(robot, distances, k, NUM_SECTORS);
        }

        boolean[] newOpen = new boolean[NUM_SECTORS];
        for (int k = 0; k < NUM_SECTORS; k++) {
            newOpen[k] = (newSafe[k] && (getSectorSumOfDistances(distances, k, NUM_SECTORS) >=
                    ((LaserCarrying) robot).getMINOPEN()));
        }

        safeSpaces = newSafe;
        openSpaces = newOpen;
    }


    private boolean getSectorSafe(LaserCarrying robot, double[] distances, int sectorCounter, int totalSectors) {
        int[] startAndEndAngles = getStartAndEndAngles(sectorCounter, totalSectors);
        for (int i = startAndEndAngles[0]; i < startAndEndAngles[1]; i++) {
            if (distances[i] < ((LaserCarrying) robot).getCRITICALDIST()) // there's an obstacle close by
                return false;
        }
        // if hasn't quit with false
        return true;
    }

    public int[] getStartAndEndAngles(int sectorCounter, int totalSectors) {
        int angleStart = sectorCounter * numLaserReadings / totalSectors;
        int angleEnd = (sectorCounter + 1) * numLaserReadings / totalSectors;
        return new int[]{angleStart, angleEnd};
    }

    private double getSectorSumOfDistances(double[] laserReadings, int sectorCounter, int totalSectors) {
        double sumOfDistances = 0;
        int[] startAndEndAngles = getStartAndEndAngles(sectorCounter, totalSectors);
        for (int i = startAndEndAngles[0]; i < startAndEndAngles[1]; i++) {
            sumOfDistances = sumOfDistances + laserReadings[i];
        }
        return sumOfDistances;
    }


    public List<Line2D> getCurrentLaserLines() {
        return currentLaserLines;
    }

    public double[] getCurrentLaserDistances() {
        return currentLaserDistances;
    }

    public boolean checkObstacle() {
        return !safeSpaces[1]; // middle laser sector
    }


    /**
     * returns an array of three booleans for open on right (0), front(1), and left(2)
     */
    public boolean[] getOpenSpaces() {
        return openSpaces;
    }

    public boolean[] getSafeSpaces() {
        return safeSpaces;
    }

    /**
     * Total angle of coverage by a laser reading.
     * Currently assumes that there is 1 laser reading per degree.
     * (ie, scanAngle = numreadings-1)
     *
     * @return
     * @author EAK
     */
    public double getScanAngle() {
        return Math.toRadians(numLaserReadings - 1);
    }

    /**
     * Angle of offset in laser readings.
     *
     * @return
     * @author PWS
     */
    public double getScanOffset() {
        return laserOffset;
    }

    public int getNumLaserReadings() {
        return numLaserReadings;
    }

}
