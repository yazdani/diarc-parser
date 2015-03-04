package com.adesim.robot;

import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import com.ADEPercept;
import com.adesim.ADESimEnvironmentComponentImpl;
import com.adesim.datastructures.CameraSpecs;
import com.adesim.datastructures.DoubleDimension;
import com.adesim.datastructures.ObjectLineIntersectionData;
import com.adesim.datastructures.Point3D;
import com.adesim.datastructures.SimShape;
import com.adesim.gui.PopupObjectAction;
import com.adesim.gui.datastructures.RobotVisualizationData;
import com.adesim.objects.Block;
import com.adesim.objects.Box;
import com.adesim.objects.Door;
import com.adesim.objects.Landmark;
import com.adesim.objects.SimEntity;
import com.adesim.objects.model.SimModel;
import com.adesim.robot.interfaces.CameraCarrying;
import com.adesim.robot.interfaces.LaserCarrying;
import com.adesim.robot.interfaces.ObstacleSensorEquipped;
import com.adesim.robot.laser.AbstractLaser;
import com.adesim.util.SimUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public abstract class SimAbstractRobot {
    private static final long serialVersionUID = 1L;
    private Log log = LogFactory.getLog(getClass());
    private SimLocationSpecifier locationSpecifier;
    //public so specific robots (i.e., the wheelchair) can override.
    public double targetTV = 0.0;
    public double targetRV = 0.0;
    //private double targetTV = 0.0;
    //private double targetRV = 0.0;
    private double acceleration; // passed in from server impl. 0-1, higher = quicker stopping
    private boolean isStalling;
    private SimShape robotTransformedShape; // updated after every move and at initialization.

    private double curTV, curRV;

    private SimModel model;
    private String serverName;
    private boolean noisyOdometry;
    private String imageRelativeFilename;


    ///////////////////////////////////////////////////////////////////
    /******** METHODS THAT MUST BE OVERRIDDEN BY SUBCLASSES **********/
    ///////////////////////////////////////////////////////////////////

    /**
     * gets shape in the "platonic" "ideal" form, centered at 0,0 and
     * oriented to the right.
     */
    protected abstract SimShape getPlatonicShape();

    public abstract String getTooltipDescription();

    public abstract ArrayList<PopupObjectAction> getPopupRobotActions();

    /**
     * the abstract robot class takes care of x, y, and theta.  but if
     * the Z position needs to be updated as well, do it here or just return 0.
     */
    public abstract double getZPositionDisplacementIfAny();


    ///////////////////////////////////////////////////////////////////


    public SimAbstractRobot(SimModel model, String serverIDName,
                            boolean noisyOdometry, String imageRelativeFilename) {
        this.model = model;
        this.serverName = serverIDName;
        this.noisyOdometry = noisyOdometry;
        this.imageRelativeFilename = imageRelativeFilename;
        log.info("Constructed abstract robot: " + serverIDName);
    }


    public String getName() {
        return serverName;
    }


    /**
     * Calculate next velocity according to target and acceleration
     */
    private double calculateNextVelocity(double target, double cur_v,
                                         double acceleration) {
        double tick = 1 / (double) ADESimEnvironmentComponentImpl.SIM_TIME_RESOLUTION;
        double vel_diff = target - cur_v;
        double time_to_target = Math.abs(vel_diff / acceleration);
        // If we can reach desired velocity in a single time step, do
        //  so, otherwise change velocity according to acceleration
        //  (per time tick)
        if (time_to_target > tick) {
            if (vel_diff < 0)
                cur_v -= acceleration * tick;
            else
                cur_v += acceleration * tick;
        } else {
            cur_v = target;
        }
        return cur_v;
    }

    /**
     * returns position offsets for one time step
     *
     * @return a double array of x, y, theta differences
     */
    private SimLocationSpecifier getPositionOffsets() {
        // Velocity threshold
        double thresh = .001;
        // Number of seconds in simulator tick
        double tick = 1 / ADESimEnvironmentComponentImpl.SIM_TIME_RESOLUTION;

        double zDifference = getZPositionDisplacementIfAny();

        // Update sim global positions according to acceleration and desired velocities
        // Calc new translational velocity
        curTV = calculateNextVelocity(targetTV, curTV, acceleration);
        // Calc new rotational velocity similarly
        double rot_acceleration = acceleration;
        curRV = calculateNextVelocity(targetRV, curRV, rot_acceleration);
        // Don't change if velocities are beneath threshold
        if (Math.abs(curTV) > thresh || Math.abs(curRV) > thresh) {
            // rotation but no translation
            if (Math.abs(curTV) < thresh && Math.abs(curRV) >= thresh) {
                double dt = curRV * tick;
                return new SimLocationSpecifier(new Point3D(0, 0, zDifference), dt);
                // translation but no rotation
            } else if (Math.abs(curTV) >= thresh && Math.abs(curRV) < thresh) {
                double dx = curTV * tick * Math.cos(this.locationSpecifier.getOdoTheta());
                double dy = curTV * tick * Math.sin(this.locationSpecifier.getOdoTheta());

                return new SimLocationSpecifier(new Point3D(dx, dy, zDifference), 0);
                // both translation and rotation
            } else {
                double dist = (curTV * tick);
                double radius = (curTV / curRV);
                double c_x = this.locationSpecifier.getX() + radius
                        * Math.cos(this.locationSpecifier.getOdoTheta() + Math.PI / 2.0);
                double c_y = this.locationSpecifier.getY() + radius
                        * Math.sin(this.locationSpecifier.getOdoTheta() + Math.PI / 2.0);

                double newTheta = SimUtil.normalizeAngle(this.locationSpecifier.getTheta()
                        + dist / radius);
                double newX = c_x + radius * Math.cos(newTheta - Math.PI / 2.0);
                double newY = c_y + radius * Math.sin(newTheta - Math.PI / 2.0);

                // Based on the new x, y, and theta, add the offsets to the location specifier
                //    (will, as above, influence both the global position and the odometry)
                double dx = newX - this.locationSpecifier.getX();
                double dy = newY - this.locationSpecifier.getY();
                double dt = newTheta - this.locationSpecifier.getTheta();

                return new SimLocationSpecifier(new Point3D(dx, dy, zDifference), dt);
            }
        } else { // if velocities are beneath threshold
            return new SimLocationSpecifier(new Point3D(0, 0, zDifference), 0);
        }
    }

    /**
     * returns a SimLocationSpecifier with the displacement if the robot moved,
     * and null otherwise
     */
    public SimLocationSpecifier performMove(int tickCounter) {
        SimLocationSpecifier formerLocationSpecifier = new SimLocationSpecifier(locationSpecifier);

        SimLocationSpecifier positionDifference = getPositionOffsets();
        this.locationSpecifier.addOffsets(positionDifference);
        robotTransformedShape = getShape();

        if (locationIsObstacleFree(robotTransformedShape, positionDifference.getXYLocation())) {
            isStalling = false;
            if (positionDifference.allZeros()) {
                return null;
            } else {
                return positionDifference;
            }
        } else {
            // error message:
            if (!isStalling) {
                System.out.println("STALL!" + "  (" +
                        ADESimEnvironmentComponentImpl.getElapsedTimeForTickCounter(tickCounter) + ")");
            }
            isStalling = true;

            // revert
            this.locationSpecifier = new SimLocationSpecifier(formerLocationSpecifier);
            robotTransformedShape = getShape();

            // hasn't moved (since obviously reverted), return null
            return null;
        }
    }


    /**
     * used to check if the location (hypothetical or current) is obstacle-free
     * protected so can be used by PerceptionHelper in the same package as well
     *
     */
    protected boolean locationIsObstacleFree(SimShape robotShape, Point2D pushingOffsetIfAny) {
        for (SimShape obstacle : model.getLaserVisibleShapes(this.getMinMaxZ())) {
            if (robotShape.intersectsShape(obstacle)) {
                if (!obstacle.isPushable(pushingOffsetIfAny, this.serverName, model)) {
                    return false;
                }
            }
        }

        // if hasn't intersected anything and returned false:
        return true;
    }

    public SimShape getShape() {
        AffineTransform transform = this.locationSpecifier.getTransformation();
        SimShape transformedGroundShape = this.getPlatonicShape().createdTransformedShape(transform);
        double groundHeight = this.getGroundHeight();
        transformedGroundShape.setZ(groundHeight);
        return transformedGroundShape;
    }

    public void updateLaserReadingsIfAny() {
        if (this instanceof LaserCarrying) {
            AbstractLaser laser = ((LaserCarrying) this).getLaser();
            laser.updateLaserReadings(this);
        }
    }


    // GETTERS AND SETTERS
    public Point2D.Double getLocation() {
        return locationSpecifier.getXYLocation();
    }

    public SimLocationSpecifier getLocationSpecifier() {
        return locationSpecifier;
    }

    public void setLocationSpecifier(SimLocationSpecifier locationSpecifier) {
        this.locationSpecifier = locationSpecifier;
    }

    public void setTargetTV(double tv) {
        this.targetTV = tv;
    }

    public double getTargetTV() {
        return targetTV;
    }

    public void setTargetRV(double rv) {
        this.targetRV = rv;
    }

    public double getTargetRV() {
        return targetRV;
    }

    public void setTargetVels(double tv, double rv) {
        setTargetTV(tv);
        setTargetRV(rv);
    }

    public double[] getPoseGlobal() {
        return locationSpecifier.getPoseGlobal();
    }

    public boolean getStall() {
        return isStalling;
    }

    public SimModel getModel() {
        return model;
    }

    public double[] getPoseEgoInternal() {
        if (noisyOdometry) {
            return this.locationSpecifier.getPoseEgoNoisy();
        } else {
            return this.locationSpecifier.getPoseEgoGroundTruth();
        }
    }

    public void resetPoseEgoInternal() {
        this.locationSpecifier.resetPoseEgo();
    }

    public void setAcceleration(double acceleration) {
        this.acceleration = acceleration;
    }

    /**
     * gets the robot's arm reach for picking up objects.  By default, null.
     * a robot WITH an arm will want to override this!
     */
    public Double getArmReach() {
        return null;
    }

    /**
     * gets the robot's door opener reach for opening/closing doors.  By default, null.
     * a robot WITH a door opener/closer will want to override this!
     */
    public Double getDoorOpenerReach() {
        return null;
    }


    /**
     * get percepts within robot's camera's field of vision,
     * matching specific criteria ("box", "block", etc, or actual object names, i.e., "blue box").
     * Special cases:
     * - if want to match ANY object, use the wildcard "*"
     * - if you want to match another robot, use "robot" or the robot's name.
     */
    public ArrayList<ADEPercept> getPercepts(String... matchingCriteria) {
        if (this instanceof CameraCarrying) {
            return getPercepts(((CameraCarrying) this).getCameraSpecs().fieldOfVisionDegrees, matchingCriteria);
        } else {
            return new ArrayList<ADEPercept>(); // empty vector, no perceptions.
        }
    }

    /**
     * get percepts within a particular field of vision (that way can do a
     * "look for" with a 360-degree sweep), matching specific criteria ("box", "block", etc,
     * or actual object names, i.e., "blue box").
     * Special cases:
     * - if want to match ANY object, use the wildcard "*"
     * - if you want to match another robot, use "robot" or the robot's name.
     */
    public ArrayList<ADEPercept> getPercepts(int fieldOfVisionDegrees, String... matchingCriteria) {
        if (this instanceof CameraCarrying) {
            PerceptionHelper perceptionHelper = new PerceptionHelper(this, ((CameraCarrying) this).getCameraSpecs());
            return perceptionHelper.getPercepts(fieldOfVisionDegrees, matchingCriteria);
        } else {
            return new ArrayList<ADEPercept>(); // empty vector, no perceptions.
        }
    }

    /**
     * generates a location in front of robot large enough for the object not to be touching the robot
     */
    public Point2D getLocationInFrontOfRobot(SimEntity object) {
        DoubleDimension dimension = object.getShape().getBoundingDimension();
        double maxRadius = Point.distance(0, 0, dimension.width / 2.0, dimension.height / 2.0);
        SimShape platonicShape = this.getPlatonicShape(); // robot points to right, so front is at the right.
        double platonicRobotGreatestX = platonicShape.getMax().getX();
        Point2D platonicSafePoint = new Point2D.Double(platonicRobotGreatestX + maxRadius + 0.05, 0);
        // 0.05 just for safety
        return this.getLocationSpecifier().getTransformation().transform(platonicSafePoint, null);
    }


    /**
     * gets camera data for 3D visualization -- if camera exists, i.e., if robot
     * overwrites getCameraSpecs()
     */
    public CameraData getCameraDataFor3DVis() {
        if (this instanceof CameraCarrying) {
            CameraSpecs cameraSpecs = ((CameraCarrying) this).getCameraSpecs();
            Point3D platonicCameraLocation = cameraSpecs.mountingPoint.point;
            Point2D cameraXY = this.getLocationSpecifier().getTransformation().transform(
                    platonicCameraLocation.point2D, null);
            Point3D worldCameraLocation = new Point3D(cameraXY,
                    cameraSpecs.mountingPoint.point.z + this.getGroundHeight());

            double worldCameraDirection = SimUtil.normalizeAngle(this.getLocationSpecifier().getTheta() +
                    cameraSpecs.mountingPoint.degreeOffsetFromFront);

            return new CameraData(worldCameraLocation,
                    worldCameraDirection, cameraSpecs.fieldOfVisionDegrees);
        } else {
            return null;
        }
    }


    /**
     * returns the height of the ROBOT'S BOTTOMMOST PART above ground (which is different from the
     * location specifier, which is in terms of the robot's center)
     */
    public double getGroundHeight() {
        // PWS: this looks wrong, to me (why *add* the length to the center of mass?),
        // but there are too many things depending on it to change it without fully
        // testing, so leaving for now
        return (this.getLocationSpecifier().getZ() + getPlatonicShape().getZLength() / 2.0);
    }


    public double[] getMinMaxZ() {
        double groundHeight = getGroundHeight();
        return new double[]{groundHeight, groundHeight + getPlatonicShape().getZLength()};
    }


    /**
     * basic "common" robot visualization data.  Individual robots can append if they want to.
     */
    public RobotVisualizationData getRobotVisData(
            boolean drawLaserLines, boolean drawActivatedObstacleSensors,
            boolean drawPerceivedObjects) {
        RobotVisualizationData data = new RobotVisualizationData(this);

        data.modelLocationSpecifier = this.getLocationSpecifier();
        data.platonicShape = getPlatonicShape();

        data.imageRelativeFilename = imageRelativeFilename;

        if (drawLaserLines) {
            if (this instanceof LaserCarrying) {
                AbstractLaser laser = ((LaserCarrying) this).getLaser();
                data.laserLines = laser.getCurrentLaserLines();
                data.laserDistances = laser.getCurrentLaserDistances();
            }
        }

        if (drawActivatedObstacleSensors) {
            if (this instanceof ObstacleSensorEquipped) {
                data.platonicActivatedObstacleSensors =
                        ((ObstacleSensorEquipped) this).getPlatonicObstacleSensors();
            }
        }

        if (drawPerceivedObjects) {
            if (this instanceof CameraCarrying) {
                CameraSpecs cameraSpecs = ((CameraCarrying) this).getCameraSpecs();

                PerceptionHelper perceptionHelper = new PerceptionHelper(this, cameraSpecs);
                ObjectLineIntersectionData perceptionData = perceptionHelper.
                        findAllPerceivedObjects(cameraSpecs.fieldOfVisionDegrees,
                                new String[]{Block.TYPE, Box.TYPE, Door.TYPE, Landmark.TYPE, "robot"});
                data.perceptualLines = perceptionData.lines;
                data.perceivedObjects = perceptionData.perceivedObjectIDs;
                data.perceivedPseudoXYPoints = perceptionData.pseudoXYpoints;
                data.perceivedRobotShapes = perceptionData.perceivedRobotShapes;
            }
        }

        return data;
    }


}
