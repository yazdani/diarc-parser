package com.adesim.robot;

import java.util.ArrayList;
import java.util.List;

import com.adesim.datastructures.CameraSpecs;
import com.adesim.gui.PopupObjectAction;
import com.adesim.objects.Block;
import com.adesim.objects.Box;
import com.adesim.objects.Door;
import com.adesim.objects.SimContainerEntity;
import com.adesim.objects.SimEntity;
import com.adesim.objects.model.SimModel;
import com.adesim.robot.interfaces.CameraCarrying;
import com.adesim.robot.interfaces.LaserCarrying;
import com.adesim.robot.interfaces.ObstacleSensorEquipped;
import com.adesim.robot.laser.AbstractLaser;
import com.adesim.util.SimUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Generic class for a robot base that has a camera and a laser (for example, the Pioneer or the Videre Era)
 */
public abstract class SimGenericRobotBase extends SimAbstractRobot implements LaserCarrying, CameraCarrying, ObstacleSensorEquipped {

    private Log log = LogFactory.getLog(getClass());

    protected AbstractLaser laser;

    protected double CRITICALDIST = 0.65;
    protected double MINOPEN = 50; // 60 beams * 1/2 m

    /**
     * To pick up blocks (or other small items, which admittedly we don't have yet, but we might someday), distance
     * between robot center and items' center(s) must be less than or equal to ARM_REACH
     */
    protected static double ARM_REACH = 1.0;

    /**
     * To open door, distance between robot center and door center must be less than or equal to DOOR_OPENER_REACH
     */
    protected double DOOR_OPENER_REACH = 1.0;

    protected CameraSpecs cameraSpecs; // camera specs (mount point, angle, and field of vision).
    //     keep local rather than pass to super, because not all robots might have a camera,
    //     and perhaps the robot might modify its camera placement at runtime 
    //     (not implemented yet, but could easily be if the need arises...)	


    /**
     *
     */
    public SimGenericRobotBase(SimModel model, String serverIDName, boolean noisyOdometry,
                               String imageRelativeFilename, AbstractLaser laser, CameraSpecs cameraSpecs) {
        super(model, serverIDName, noisyOdometry, imageRelativeFilename);
        this.laser = laser;
        this.cameraSpecs = cameraSpecs;
    }


    @Override
    /** overwrite getCameraSpecs to show that yes, do have a camera */
    public CameraSpecs getCameraSpecs() {
        return this.cameraSpecs;
    }


    public void setCRITICALDIST(double dist) {
        log.debug("Changing critical distance from " + CRITICALDIST + " to " + dist);
        CRITICALDIST = dist;
    }

    public double getCRITICALDIST() {
        return CRITICALDIST;
    }

    public void setMINOPEN(double dist) {
        log.debug("Changing min open from " + MINOPEN + " to " + dist);
        MINOPEN = dist;
    }

    public double getMINOPEN() {
        return MINOPEN;
    }


    /**
     * this robot will have an arm -- hence override getArmReach()
     */
    @Override
    public Double getArmReach() {
        return ARM_REACH;
    }

    /**
     * this robot will have a door opening grasper -- hence override getDoorOpenerReach()
     */
    @Override
    public Double getDoorOpenerReach() {
        return DOOR_OPENER_REACH;
    }

    @Override
    public String getTooltipDescription() {
        return "robot id = " + this.getName() + ", " +
                "location = " + SimUtil.getPointCoordinatePair(getLocation()) + ", <br> orienation = " +
                SimUtil.formatDecimal(Math.toDegrees(this.getLocationSpecifier().getTheta()))
                + SimUtil.DEGREE_SYMBOL +
                ", <br> items in possession = " + this.getModel().itemsInRobotPossession.size();
    }

    @Override
    public ArrayList<PopupObjectAction> getPopupRobotActions() {
        ArrayList<PopupObjectAction> popupActions = new ArrayList<PopupObjectAction>();

        // generate the popup actions based on the context:
        List<SimEntity> blocksOnFloorWithinReach = this.getModel().getAllMatchingObjectsWithinDistance(
                Block.TYPE, SimEntity.class,
                this.getArmReach());
        for (SimEntity eachBlockOnFloorWithinReach : blocksOnFloorWithinReach) {
            popupActions.add(new PopupObjectAction("Pick up " +
                    eachBlockOnFloorWithinReach.getNameOrType(true), "pickUpObject",
                    eachBlockOnFloorWithinReach.getNameOrType(false)));
        }


        List<SimEntity> blocksInPossession = this.getModel().itemsInRobotPossession
                .getMatchingObjects(new String[]{Block.TYPE}, SimEntity.class);
        for (SimEntity eachBlockInPossession : blocksInPossession) {
            popupActions.add(new PopupObjectAction("Put down " +
                    eachBlockInPossession.getNameOrType(true), "putDownObject",
                    eachBlockInPossession.getNameOrType(false)));
        }


        List<SimEntity> boxesWithinReach = this.getModel().getAllMatchingObjectsWithinDistance(
                Box.TYPE, SimContainerEntity.class, this.getArmReach());

        for (SimEntity eachContainerSimEntity : boxesWithinReach) {
            SimContainerEntity eachContainer = (SimContainerEntity) eachContainerSimEntity;

            if (eachContainer.isOpen()) {
                for (SimEntity eachBlockInPossession : blocksInPossession) {
                    popupActions.add(new PopupObjectAction(
                            "Put " + eachBlockInPossession.getNameOrType(true) +
                                    " into " + eachContainerSimEntity.getNameOrType(true),
                            "putIntoBox",
                            eachContainerSimEntity.getNameOrType(false), eachBlockInPossession.getNameOrType(false)));
                }

                List<SimEntity> blocksWithinBox = eachContainer.getObjectsHolder()
                        .getMatchingObjects(new String[]{Block.TYPE}, SimEntity.class);
                for (SimEntity eachBlockInBox : blocksWithinBox) {
                    popupActions.add(new PopupObjectAction(
                            "Get " + eachBlockInBox.getNameOrType(true) +
                                    " from " + eachContainerSimEntity.getNameOrType(true),
                            "getFromBox", eachContainerSimEntity.getNameOrType(false),
                            eachBlockInBox.getNameOrType(false)));
                }


                popupActions.add(new PopupObjectAction("Close " +
                        eachContainerSimEntity.getNameOrType(true), "closeBox",
                        eachContainerSimEntity.getNameOrType(false)));
            } else {
                popupActions.add(new PopupObjectAction("Open " +
                        eachContainerSimEntity.getNameOrType(true), "openBox",
                        eachContainerSimEntity.getNameOrType(false)));
            }
        }


        // door:
        if (getDoorOpenerReach() != null) {
            // check if the center of ANY item is within arm-distance away from the robot
            Door doorToOpen = (Door) getModel().getFirstMatchingObjectWithinDistance(
                    Door.TYPE, Door.class, getDoorOpenerReach());
            if (doorToOpen != null) {
                if (!doorToOpen.isFullyOpen()) {
                    popupActions.add(new PopupObjectAction("Open nearby door", "openDoor", Door.TYPE));
                }

                if (!doorToOpen.isFullyClosed()) {
                    popupActions.add(new PopupObjectAction("Close nearby door", "closeDoor", Door.TYPE));
                }
            }
        }


        return popupActions;
    }

    @Override
    public AbstractLaser getLaser() {
        return this.laser;
    }

    @Override
    public double getZPositionDisplacementIfAny() {
        return 0; // never changes Z, the generic robot (i.e., Pioneer, Era) does not fly!
    }

}
