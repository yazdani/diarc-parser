package com.adesim.objects;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import utilities.xml.Xml;

import com.adesim.ADESimEnvironmentComponentImpl;
import com.adesim.commands.UpdateDoorShapeCommand;
import com.adesim.datastructures.PointCollection;
import com.adesim.datastructures.PushabilityDeterminer;
import com.adesim.datastructures.SimShape;
import com.adesim.datastructures.SimShape.Observer;
import com.adesim.datastructures.action.DoorPushOpenAction;
import com.adesim.datastructures.action.SimAction;
import com.adesim.gui.PopupObjectAction;
import com.adesim.objects.model.EnvironmentModelOwner;
import com.adesim.objects.model.ModelOwner;
import com.adesim.objects.model.SimModel;
import com.adesim.util.SimUtil;

public class Door extends SimEntity implements Observer, PushabilityDeterminer {
	private static final long serialVersionUID = 1L;
	
	public enum DoorUpdatingStatus {
		OPEN, STAY_PUT, CLOSE
	}
	
	public static final Color DOOR_COLOR = new Color(153, 102, 51); // brown 
	public static final String TYPE = "Door";
	
	public static final double DOOR_OPENING_DURATION_IN_SECONDS = 2;
	public static final double DOOR_OPENING_FRACTION_PER_TICK = 
			1.0 / (double)((DOOR_OPENING_DURATION_IN_SECONDS * ADESimEnvironmentComponentImpl.SIM_TIME_RESOLUTION));
	
	private DoorUpdatingStatus updatingStatus = DoorUpdatingStatus.STAY_PUT;
	
	// SOME OF THE PARAMETERS BELOW WILL STAY CONSTANT THROUGH THE "LIFETIME"
	//      OF A PARTICULAR DOOR, OTHERS (marked as "MAY CHANGE") may
	//      indeed CHANGE IF THE DOOR IS MOVED/ROTATED  (and as such must be passed
	//      to the UpdateDoorShapeCommand, to make sure that those values remain in sync
	//      across the environment and actor models).  Or, in the case of "openFraction",
	//      simply when the door is opened/closed.
	private double openFraction;  // MAY CHANGE
	private Point2D pivot;  // MAY CHANGE
	private double width; 
	private double halfThickness; // cached just for efficiency
	private double closedAngle;  // MAY CHANGE
	private double pivotAngle; // think of this as a displacement amount -- 
	//        a door may pivot +PI/2, for example.   So whereas the closed angle is a 
	//        "physical" angle in the environment, the pivot is "how much should you change by?"
	
	
	private ArrayList<SimAction> actions; // keep copy from constructor, so can re-add them to shape
	//     every time that the door shape is regenerated.
	private boolean isPushable; // indicates whether the door object even SUPPORTS pushability.
	//     Cached for efficiency, but obtained by looking for a DoorPushOpenAction 
	//     within the passed-in actions list.  If IS pushable, will use more complex logic
	//     in isPushable() to determine if actually pushable under present circumstances
	//     (not already open/closed to maximum extent, will not intersect with other objects, etc).
	
	
	//  In parser, door specs are as follows:
	//         <door [*optional* name=""] openFraction="[0 through 1.0]" 
	//              	x="" y="" width="" depth="" closed_angle="" pivot_angle="">
	//             [possible actions]
	//         </door>
    public Door(String name, double openFraction, Point2D pivot, 
    			double width, double thickness, double closedAngle, double pivotAngle, 
    			ArrayList<SimAction> actions) {
    	super(null, name, TYPE, true, DOOR_COLOR, DOOR_COLOR); // "assign" default color so it's returned in memory object
    	
        this.openFraction = openFraction;
        this.pivot = pivot;
        this.width = width;
        this.halfThickness = thickness / 2.0;
        
        this.closedAngle = closedAngle;
        this.pivotAngle = pivotAngle;
        
        this.actions = actions;
        this.isPushable = checkIfDoorIsPushable();
        
        updateDoorShape();
    }
   
    public Door(String name, double openFraction, Point2D pivot, 
    			double width, double thickness, double closedAngle, double pivotAngle, 
    			ArrayList<SimAction> actions, Color color) {
    	super(null, name, TYPE, true, color, DOOR_COLOR);
    	
        this.openFraction = openFraction;
        this.pivot = pivot;
        this.width = width;
        this.halfThickness = thickness / 2.0;
        
        this.closedAngle = closedAngle;
        this.pivotAngle = pivotAngle;
        
        this.actions = actions;
        this.isPushable = checkIfDoorIsPushable();
        
        updateDoorShape();
    }
   
    private boolean checkIfDoorIsPushable() {
		for (SimAction each : actions) {
			if (each instanceof DoorPushOpenAction) {
				return true;
			}
		}
		// if not quit with true:
		return false;
	}

	/** door will need to be notified when the door shape changes (i.e., it is moved),
     * so that the door can update its pivot angle. */
	@Override
	public void shapeTransformed(AffineTransform transform) {
		this.pivot = transform.transform(pivot, null);
		double rotationAngleOfTransform = SimUtil.getRotationAngleFromAffineTransformation(transform);
		if (rotationAngleOfTransform != 0) {
			// only the closed angle needs to be adjusted.  pivot is RELATIVE DISPLACEMENT, not a physical angle.  
			closedAngle = SimUtil.normalizeAngle(closedAngle + rotationAngleOfTransform);
		}
	}
    
    @Override
    public Xml generateXML() {
    	Xml doorXml = new Xml("door");
    	this.addNameAttributeIfItExists(doorXml);
    	doorXml.addAttribute("openFraction", Double.toString(openFraction));
    	doorXml.addAttribute("x", Double.toString(pivot.getX()));
    	doorXml.addAttribute("y", Double.toString(pivot.getY()));
    	doorXml.addAttribute("width", Double.toString(width));
    	doorXml.addAttribute("thickness", Double.toString(halfThickness * 2.0));
    	doorXml.addAttribute("closed_angle", Double.toString(closedAngle));
    	doorXml.addAttribute("pivot_angle", Double.toString(pivotAngle));
    	
    	
		for (SimAction action : this.actions) {
			doorXml.addChild(action.generateXML());
		}
		
    	return doorXml;
    }

    
    private void updateDoorShape() {
    	updateDoorShape(generateDoorShape(this.openFraction));
	}
    
    /** public so that can be updated externally via the (@see UpdateDoorShapeCommand),
     * though mostly used within this class */
	public void updateDoorShape(double openFraction, Point2D pivot, 
			double closedAngle, SimShape newDoorShape) {
		this.openFraction = openFraction;
		
		// set pivot and closed angle, in case they changed as a result of a move/rotate.
		this.pivot = pivot;
		this.closedAngle = closedAngle;
		
		updateDoorShape(newDoorShape);
	}
	
	private void updateDoorShape(SimShape newDoorShape) {
		// cache the shape:
    	setShape(newDoorShape);
    	
    	// when set a new shape, be sure to add itself as an observer for it again.
    	
    	// door will need to be notified when the door shape changes (i.e., it is moved),
    	//    so that the door can update its pivot angle.
    	this.getShape().addObserver(this);
	}

	protected SimShape generateDoorShape(double basedOnOpenFraction) {
    	// first, assume it's on x axis at 0,0.  Then want to do:
    	PointCollection points = new PointCollection();
    	points.add(0, halfThickness);
    	points.add(width, halfThickness);
    	points.add(width, -1*halfThickness);
    	points.add(0, -1*halfThickness);
    	SimShape platonicShape = new SimShape(points, actions, 0, null, this);
    	
    	// now transform:
    	double doorAngle = closedAngle + basedOnOpenFraction * pivotAngle;
    	AffineTransform transform = new AffineTransform();
    	transform.translate(pivot.getX(), pivot.getY()); // translate first, then rotate
    	transform.rotate(doorAngle);
    	
    	
    	SimShape shape = platonicShape.createdTransformedShape(transform);
    	shape.assignDefaultZLengthIfNonePresent(defaultZLength());
    	return shape;
    }

	public void updateStatus(DoorUpdatingStatus status) {
		updatingStatus = status;
	}
	
	@Override
	public void tick(SimModel model) {
		// for a door, ticking is only valid IF IT IS PART OF THE ENVIRONMENT, *NOT* the individual robot server
		//    and not for the GUI, either!
		if (model.owner.getOwnerType() != ModelOwner.OwnerType.ENVIRONMENT) {
			return;
		}
		
		
		DoorHypotheticalMovement hypothetical = new DoorHypotheticalMovement(
				updatingStatus, openFraction, this, model);
		updatingStatus = hypothetical.updatingStatus;
		openFraction = hypothetical.openFraction;
		
		if (hypothetical.performMove) {
			updateDoorShape(hypothetical.resultingDoorShape);
			((EnvironmentModelOwner) model.owner).addCommand(
					new UpdateDoorShapeCommand(this.getGUID(), openFraction,
							pivot, closedAngle, hypothetical.resultingDoorShape));
		
		} else if (hypothetical.collision) {
			System.out.println("Door jam!");
			
			// since the environment is the only once concerned with the status, 
			//     no command needs to be issued in this case...
		}
	}
	

	@Override
	public ArrayList<PopupObjectAction> getPopupObjectActions() {
		ArrayList<PopupObjectAction> actions = new ArrayList<PopupObjectAction>();
		// door actions are of the form:
		// updateDoorStatusOnMap(String doorGUID, DoorUpdatingStatus status, boolean broadcastToAllOtherParticipants);
		//     in the case of the popup on a map, DO want to broadcast to the rest of the concurrent simulations,
		//     so the last argument is true.
		actions.add (new PopupObjectAction("Open door", "updateDoorStatusOnMap", 
											this.getGUID(), DoorUpdatingStatus.OPEN));
		actions.add (new PopupObjectAction("Close door", "updateDoorStatusOnMap", 
											this.getGUID(), DoorUpdatingStatus.CLOSE));
			// in both open and closeDoorOnMap, true stands from "broadcast to all of the rest of the concurrent simulations
		return actions;
	}
	
	@Override
	public void performAnyPostPainting(Polygon shapeOnScreen, Graphics g) {
		// do nothing
	}

	public boolean isFullyOpen() {
		return (openFraction == 1.0);
	}
	
	public boolean isFullyClosed() {
		return (openFraction == 0);
	}
	
	@Override
	public String getToolTipIfAny() {
		
		return getNameOrType(true) + 
				(  (getShape().getZ() > 0) ? ", z = " + SimUtil.formatDecimal(getShape().getZ()) : null) + 
				(isPushable ? "; pushable" : null);
	}
	
	@Override
	public double defaultZLength() {
		return 2.0;
	}
	
	public Point2D getPivot() {
		return pivot;
	}
	
	public double getCurrentAngle() {
		return closedAngle + openFraction * pivotAngle;
	}

	public double getClosedAngle() {
		return closedAngle;
	}

	public double getOpenAngle() {
		return closedAngle + pivotAngle;
	}
	

	@Override
	public boolean isPushable(SimShape shape, Point2D offset,
			String thisRobotName, SimModel model) {
		if (this.isPushable) {
			// isPushable(model) is only called by a sim actor server (e.g. server w/ robot).
			//     So, try to determine if door would be pushable by the robot.
			
			// first, determine which way it would try to push the door:
			boolean wouldBeTryingToOpenTheDoor = 
				DoorPushOpenAction.wouldBeTryingToOpenTheDoor(this, model);
			
			DoorUpdatingStatus hypotheticalDoorUpdatingStatus;
			if (wouldBeTryingToOpenTheDoor) {
				hypotheticalDoorUpdatingStatus = DoorUpdatingStatus.OPEN;
			} else {
				hypotheticalDoorUpdatingStatus = DoorUpdatingStatus.CLOSE;
			}
			
			DoorHypotheticalMovement hypothetical = new DoorHypotheticalMovement(
					hypotheticalDoorUpdatingStatus, openFraction, this, model);
			// return the status of the hypothetical test.  In the sense that if 
			//     would NOT stay put, then IS pushable
			return (hypothetical.updatingStatus != DoorUpdatingStatus.STAY_PUT);
			
		} else { // if not even supposed to be pushable
			return false;
		}
	}
	
	@Override
	public boolean isHypotheticallyPushable() {
		return isPushable;
	}
	
	
}
