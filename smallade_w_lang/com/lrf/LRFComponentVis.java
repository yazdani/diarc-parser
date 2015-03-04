/**
 * ADE 1.0
 * (c) copyright HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@gmail.com
 * Last update: April 2010
 *
 * LRFComponentVis.java
 */
package com.lrf;

import ade.gui.ADEGuiCallHelper;
import ade.gui.SuperADEGuiPanel;
import com.LaserScan;
import com.lrf.feature.Door;
import com.lrf.feature.IntersectionBranch;
import com.lrf.feature.Line;
import com.lrf.tracker.LaserFeatureTracker;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class LRFComponentVis extends SuperADEGuiPanel {

    private static final long serialVersionUID = 1L;
    protected double[] pose = new double[3];
    double[] laserReadings;
    int[] possibleDoor;
    int totalNumDoors = 0;
    List<Line> lines;
    List<Point2D> points;
    LaserFeatureDetector detector;      //to detect doors, etc..
    LaserFeatureTracker tracker;
    private double MAX_READING;
    // program data
    private int numReadings;//181;        // number of readings in a laser scan
    private double scanAngle;       //total angle that laser sweeps out
    private double angleIncrement;  // angle between adjacent beams
    private double criticalDist;    //dist to be considered an obstacle
    private LaserScan laserScan;
    // graphic data - note that the scale of the image is determined by
    // the combination of the baseRadius and robotSize; laser distances
    // display is limited by xSize and ySize
    private JLabel topLabel;
    private JLabel offlineLabel;
    private JLabel thresholdLabel;
    private LaserPanel laserPanel;
    private int xSize = 350;    // pixels
    private int ySize = 550;    // pixels
    private int baseRadius = 7;   // pixels
    private double robotSize = 0.42; // meters (radius)
    private int dispRobotWid = baseRadius * 2;
    private double mPerPixel = robotSize / dispRobotWid;
    private int xCenter = xSize / 2;
    private int yCenter = ySize / 2;//- 10;
    private int dispRobotLeft = xCenter - baseRadius;
    private int dispRobotBot = yCenter - baseRadius;
    private Point[] endRobot;
    private Point[] endBorder;
    private double[] xScalar;
    private double[] yScalar;
    private Point[] endBeam;
    private int skip = 1;
    private JPanel optionCheckboxPanel;
    private JCheckBox drawLasers = new JCheckBox("Lasers", true);
    private JCheckBox drawDiscontinuities = new JCheckBox("Discontinuities", false);
    private JCheckBox drawPossibleDoors = new JCheckBox("Possible doors", false);
    private JCheckBox drawContour = new JCheckBox("Contour", false);
    private JCheckBox drawEntry = new JCheckBox("Entry", false);
    private JCheckBox drawExit = new JCheckBox("Exit", false);
    private JCheckBox drawLines = new JCheckBox("Lines", true);
    private JCheckBox drawCritDist = new JCheckBox("Critical dist", false);
    private JCheckBox drawRightAngles = new JCheckBox("Right Angles", true);
    private JCheckBox drawIntersections = new JCheckBox("Intersections", true);
    private List<Door> doors;
    private List<IntersectionBranch> intersectionBranches;
    //    private List<IntersectionCenter> intersectionCenters;
    private double offset = 0.0;

    public LRFComponentVis(ADEGuiCallHelper guiCallHelper) {
        super(guiCallHelper, 100); // 100ms = update 10x/second

        //set number of laser lines in a single reading
        numReadings = call("getNumLaserReadings", Integer.class);
        offset = call("getScanOffset", Double.class);

        //set angle between adjacent beams
        scanAngle = call("getScanAngle", Double.class);
        angleIncrement = scanAngle / numReadings;

        //set angle of first laser reading (ie, reading[0])
        // zero is robot's right, increading ccw
        // currently assuming laser is pointing straight ahead
        double minAngle = (Math.PI - scanAngle) / 2.0 + offset;
        //set obstacle critical distance
        criticalDist = call("getCritDist", Double.class);

        //allocate arrays
        possibleDoor = new int[numReadings];
        doors = new ArrayList<>();
        intersectionBranches = new ArrayList<>();
        lines = new ArrayList<>();
        points = new ArrayList<>();

        //instantiate detector and tracker
        detector = new LaserFeatureDetector(numReadings, scanAngle, offset);
        tracker = new LaserFeatureTracker();

        // set up the laser end points, one at the robot one at the border
        endRobot = new Point[numReadings];
        endBorder = new Point[numReadings];
        endBeam = new Point[numReadings];
        xScalar = new double[numReadings];
        yScalar = new double[numReadings];
        double ang = minAngle;
        MAX_READING = detector.getMaxReading();
        //Point orig = new Point(xCenter, yCenter);
        for (int i = 0; i < numReadings; i++) {
            endRobot[i] = new Point(xCenter + rToX((robotSize / mPerPixel), ang),
                    yCenter - rToY((robotSize / mPerPixel), ang));
            endBorder[i] = new Point(xCenter + rToX(xCenter, ang), yCenter - rToY(yCenter, ang));
            endBeam[i] = new Point(endBorder[i].x, endBorder[i].y); // Arbitrary since it gets reset later?
            xScalar[i] = Math.cos(ang) / mPerPixel;  // ratio of x to hypot
            yScalar[i] = Math.sin(ang) / mPerPixel;  // ratio of y to hypot
            boolean debugPreprocess = false;
            if (debugPreprocess) {
                System.out.print("Beam " + i + ": ");
                System.out.print("endR=(" + endRobot[i].x + "," + endRobot[i].y + "); ");
                System.out.print("endB=(" + endBorder[i].x + "," + endBorder[i].y + "); ");
                System.out.print("scalar=(" + round(xScalar[i], 2));
                System.out.println("," + round(yScalar[i], 2) + ")");
            }
            ang += angleIncrement;
        }

        // set up the GUI
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        topLabel = new JLabel("Front Lasers", SwingConstants.LEFT);
        this.add(topLabel);
        laserPanel = new LaserPanel();
        this.add(laserPanel);
        this.add(createOptionCheckboxesPane());
        this.setTitle("Laser Component View Panel");

//        final JSlider requestOFFLINETOLERANCE = new JSlider(
//                JSlider.HORIZONTAL,
//                0 /* min */, 100 /* max */,
//                (int) (detector.OFFLINETOLERANCE * 100.0) /* initial */);
//        requestOFFLINETOLERANCE.setMajorTickSpacing(10);
//        requestOFFLINETOLERANCE.setMinorTickSpacing(5);
//        requestOFFLINETOLERANCE.setPaintTicks(true);
//        requestOFFLINETOLERANCE.setPaintTrack(true);
//        //requestOFFLINETOLERANCE.setValue( rampSpeed );
//        requestOFFLINETOLERANCE.setLabelTable(requestOFFLINETOLERANCE.createStandardLabels(10));
//        requestOFFLINETOLERANCE.setPaintLabels(true);
//
//        // user changed the speed request slider
//        ChangeListener requestOFFLINETOLERANCEListener = new ChangeListener() {
//            public void stateChanged(ChangeEvent event) {
//                detector.OFFLINETOLERANCE = requestOFFLINETOLERANCE.getValue() / 100.0;
//            }
//        };
//        requestOFFLINETOLERANCE.addChangeListener(requestOFFLINETOLERANCEListener);
//        offlineLabel = new JLabel("Off-line Tolerance", SwingConstants.LEFT);
//        this.add(offlineLabel);
//        this.add(requestOFFLINETOLERANCE);

//        final JSlider requestSPLITTHRESH = new JSlider(JSlider.HORIZONTAL, 0 /* min */, 100 /* max */, (int) (detector.SPLITTHRESH * 100.0) /* initial */);
//        requestSPLITTHRESH.setMajorTickSpacing(10);
//        requestSPLITTHRESH.setMinorTickSpacing(5);
//        requestSPLITTHRESH.setPaintTicks(true);
//        requestSPLITTHRESH.setPaintTrack(true);
//        //requestSPLITTHRESH.setValue( rampSpeed );
//        requestSPLITTHRESH.setLabelTable(requestSPLITTHRESH.createStandardLabels(10));
//        requestSPLITTHRESH.setPaintLabels(true);
//
//        // user changed the speed request slider
//        ChangeListener requestSPLITTHRESHListener = new
//
//                ChangeListener() {
//                    public void stateChanged(ChangeEvent event) {
//                        detector.SPLITTHRESH = requestSPLITTHRESH.getValue() / 100.0;
//                    }
//                };
//        requestSPLITTHRESH.addChangeListener(requestSPLITTHRESHListener);
//        thresholdLabel = new JLabel("Split Threshold", SwingConstants.LEFT);
//        this.add(thresholdLabel);
//        this.add(requestSPLITTHRESH);
    }

    private static double round(double v, int d) {
        return (double) ((int) (v * Math.pow(10, d))) / Math.pow(10, d);
    }

    private Component createOptionCheckboxesPane() {
        optionCheckboxPanel = new JPanel();
        optionCheckboxPanel.setLayout(new FlowLayout());

        optionCheckboxPanel.add(drawLasers);
        optionCheckboxPanel.add(drawDiscontinuities);
        optionCheckboxPanel.add(drawPossibleDoors);
        optionCheckboxPanel.add(drawContour);
        optionCheckboxPanel.add(drawEntry);
        optionCheckboxPanel.add(drawExit);
        optionCheckboxPanel.add(drawLines);
        optionCheckboxPanel.add(drawRightAngles);
        optionCheckboxPanel.add(drawIntersections);
        optionCheckboxPanel.add(drawCritDist);

        return optionCheckboxPanel;
    }

    private int rToX(double r, double theta) {
        return (int) (r * Math.cos(theta));
    }

    private int rToY(double r, double theta) {
        return (int) (r * Math.sin(theta));
    }

    @Override
    public void refreshGui() {
        try {
            laserScan = call("getLaserScan", LaserScan.class);
            laserReadings = (double[]) callComponent("getLaserReadings");
//            laserReadings = call("getLaserReadings", double[].class);


            // The 'detector' object needs to be a copy/have identical values as the one in SimGenericCompImpl
            // if we want to track using odometry
            // Currently, vis can't work with odom (can't call getPoseGlobal() to get pose)
            detector.updateFeatures(laserScan);
            tracker.track(detector.getDoors(), detector.getIntersections());

            intersectionBranches = detector.getIntersections();
            possibleDoor = new int[endRobot.length];
            totalNumDoors = detector.getDoors().size();
            doors = tracker.doors();
            lines = detector.getLines();
            points = detector.getPoints();

            setLaserVals(laserScan.ranges);
            laserPanel.repaint();
        } catch (Exception ex) {
            System.err.println(this.getClass().getSimpleName()
                    + ":  Could not obtain visualization data! \n" + ex);
            ex.printStackTrace();
        }
    }

    /**
     * Sets the laser display.
     *
     * @param lasers an array of laser readings
     */
    public void setLaserVals(double[] lasers) {
        //double scalar;
        //int xScalar, yScalar;
        int newX, newY;
        for (int i = 0; i < lasers.length; i++) {
            //check for bad range values
            if (lasers[i] > 0) {
                newX = xCenter + (int) (lasers[i] * xScalar[i]);
                newY = yCenter - (int) (lasers[i] * yScalar[i]);
            } else {
                // Creating a laser length of 0, effectively
                newX = endRobot[i].x;
                newY = endRobot[i].y;
            }
            endBeam[i].setLocation(newX, newY);
        }
    }

    // panel display class
    class LaserPanel extends JPanel {

        public LaserPanel() {
            super();
            setSize(xSize, ySize);
            setBorder(BorderFactory.createLineBorder(Color.black));
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(xSize, ySize);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(xSize, ySize);
        }

        @Override
        public void paintComponent(Graphics comp) {
            Graphics2D comp2D = (Graphics2D) comp;

            // draw the background
            //comp2D.setColor(getBackground());
            comp2D.setColor(Color.white);
            comp2D.fillRect(0, 0, getSize().width, getSize().height);
            // draw the robot
            comp2D.setColor(Color.black);
            comp2D.drawOval(dispRobotLeft, dispRobotBot, dispRobotWid, dispRobotWid);
            comp2D.drawLine(xCenter, yCenter, xCenter, yCenter - baseRadius);
            boolean debugDispVals = false;
            if (debugDispVals) {
                System.out.println("Drawing beams (mPerPixel=" + mPerPixel + "):");
            }
            // draw the lasers
            if (drawLasers.isSelected()) {
                double dX;
                double dY;

                for (int i = 0; i < endRobot.length; i += skip) {
                    comp2D.setColor(Color.green);
                    if (possibleDoor[i] == 0) {
                        // 1000 pixels
                        if (Math.abs(endBeam[i].x) < 1000 && Math.abs(endBeam[i].y) < 1000) {
                            dX = endBeam[i].x - endRobot[i].x;
                            dY = endBeam[i].y - endRobot[i].y;
                            if (Math.sqrt(dX * dX + dY * dY) > (int) (MAX_READING * 0.99) / mPerPixel) {
                                // Color maxed out lasers differently
                                comp2D.setColor(Color.orange);
                            }

                            comp2D.drawLine(endRobot[i].x, endRobot[i].y, endBeam[i].x, endBeam[i].y);
                        }
                    }
                }
            }
            if (drawDiscontinuities.isSelected()) {
                comp2D.setColor(Color.red);  // draw possible doors
                for (int i = 0; i < endRobot.length; i += skip) {
                    if (i >= endRobot.length) {
                        break;
                    }
                    if (possibleDoor[i] == 1) {
                        comp2D.drawLine(endRobot[i].x, endRobot[i].y, endBeam[i].x, endBeam[i].y);
                    }
                }
            }
            if (drawPossibleDoors.isSelected()) {
                comp2D.setColor(Color.blue);  // draw possible doors
                for (int i = 0; i < endRobot.length; i += skip) {
                    if (i >= endRobot.length) {
                        break;
                    }
                    if (possibleDoor[i] == 2) {
                        comp2D.drawLine(endRobot[i].x, endRobot[i].y, endBeam[i].x, endBeam[i].y);
                    }
                }
            }
            if (drawContour.isSelected()) {
                comp2D.setColor(Color.gray);
                for (int i = 1; i < numReadings; i++) {
                    comp2D.drawLine(
                            (int) (xCenter + points.get(i - 1).getX() / mPerPixel),
                            (int) (yCenter - points.get(i - 1).getY() / mPerPixel),
                            (int) (xCenter + points.get(i).getX() / mPerPixel),
                            (int) (yCenter - points.get(i).getY() / mPerPixel));
                }
            }

            if (drawCritDist.isSelected()) {
                double radiusPix = criticalDist / mPerPixel;
                comp2D.setColor(Color.red);
                comp2D.drawOval((int) (xCenter - radiusPix),
                        (int) (yCenter - radiusPix),
                        (int) (2 * radiusPix),
                        (int) (2 * radiusPix));
            }
            if (drawRightAngles.isSelected()) {
                List<Point2D> ras = detector.getRightAngles();
                double px, py, xOvalCenter, yOvalCenter;
                double radiusPix = 0.25 / mPerPixel; // Arbitrarily sized
                for (Point2D p : ras) {
                    px = p.getX();
                    py = p.getY();
                    xOvalCenter = (xCenter + px / mPerPixel) - (radiusPix / 2);
                    yOvalCenter = (yCenter + -1 * py / mPerPixel) - (radiusPix / 2);
                    comp2D.setColor(Color.blue);
                    comp2D.drawOval((int) xOvalCenter,
                            (int) yOvalCenter,
                            (int) radiusPix,
                            (int) radiusPix);
                }
            }
            if (drawLines.isSelected()) {
                comp2D.setColor(Color.black);
                for (Line line : lines) {
                    comp2D.setColor(Color.black);
                    if (line.getConfidence() < 0.5) {
                        comp2D.setColor(Color.red);
                    } else {
                        comp2D.setColor(Color.black);
                    }
                    comp2D.drawLine(
                            (int) (xCenter + line.getStart().getX() / mPerPixel),
                            (int) (yCenter - line.getStart().getY() / mPerPixel),
                            (int) (xCenter + line.getEnd().getX() / mPerPixel),
                            (int) (yCenter - line.getEnd().getY() / mPerPixel));
                }
            }
            if (drawEntry.isSelected() || drawExit.isSelected()) {
                // draw door entry points
                comp2D.setColor(Color.red);
                Stroke origi = comp2D.getStroke();
                comp2D.setStroke(new BasicStroke(3.0f));
                for (Door door : doors) {
                    // draw the door threshold
                    comp2D.drawLine(
                            (int) (xCenter + points.get(door.getLowerIndex()).getX() / mPerPixel),
                            (int) (yCenter - points.get(door.getLowerIndex()).getY() / mPerPixel),
                            (int) (xCenter + points.get(door.getUpperIndex()).getX() / mPerPixel),
                            (int) (yCenter - points.get(door.getUpperIndex()).getY() / mPerPixel));

                    // draw the entry point
                    if (drawEntry.isSelected()) {
                        Point2D entry = door.getEntry();
                        comp2D.setColor(Color.red);  // draw door entry points
                        comp2D.drawOval(
                                (int) (xCenter + entry.getX() / mPerPixel),
                                (int) (yCenter - entry.getY() / mPerPixel),
                                3, 3);

                        Point2D center = door.getCenter();
                        comp2D.setColor(Color.blue);  // draw door entry points
                        comp2D.drawOval(
                                (int) (xCenter + center.getX() / mPerPixel),
                                (int) (yCenter - center.getY() / mPerPixel),
                                3, 3);

                        Point2D approach = door.getApproach();
                        comp2D.setColor(Color.orange);  // draw door entry points
                        comp2D.drawOval(
                                (int) (xCenter + approach.getX() / mPerPixel),
                                (int) (yCenter - approach.getY() / mPerPixel),
                                3, 3);
                    }
                    // draw the exit point
                    if (drawExit.isSelected()) {
                        Point2D exit = door.getExit();
                        comp2D.drawOval(
                                (int) (xCenter + exit.getX() / mPerPixel),
                                (int) (yCenter - exit.getY() / mPerPixel),
                                3, 3);
                    }
                }
                comp2D.setStroke(origi);
            }
            if (drawIntersections.isSelected()) {
                double radiusPix = 0.125 / mPerPixel; // Arbitrarily sized
                // Draw all intersection branches tracked
                for (IntersectionBranch ib : intersectionBranches) {
                    // Draw center
                    if (!ib.visible()) {
                        comp2D.setColor(Color.darkGray);
                    } else {
                        comp2D.setColor(Color.red);
                    }
                    comp2D.drawOval(
                            (int) (xCenter + ib.entryx() / mPerPixel - (radiusPix / 2)),
                            (int) (yCenter - ib.entryy() / mPerPixel - (radiusPix / 2)),
                            (int) radiusPix, (int) radiusPix);

                    // Draw corners
                    if (!ib.visible()) {
                        comp2D.setColor(Color.darkGray);
                    } else {
                        comp2D.setColor(Color.magenta);
                    }
                    for (Point2D corner : ib.corners()) {
                        comp2D.drawOval(
                                (int) (xCenter + corner.getX() / mPerPixel - (radiusPix / 2)),
                                (int) (yCenter - corner.getY() / mPerPixel - (radiusPix / 2)),
                                (int) radiusPix, (int) radiusPix);
                    }
                }
            }
        }
    }
}
