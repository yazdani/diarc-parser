package com.adesim.robot;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import com.adesim.ADESimEnvironmentComponentImpl;
import com.adesim.datastructures.CameraSpecs;
import com.adesim.datastructures.PointCollection;
import com.adesim.datastructures.PushabilityDeterminer;
import com.adesim.datastructures.SimShape;
import com.adesim.gui.PopupObjectAction;
import com.adesim.objects.model.SimModel;
import com.adesim.robot.interfaces.CameraCarrying;
import com.adesim.util.SimUtil;

public class SimARDroneRobot extends SimAbstractRobot implements CameraCarrying {
	private static final String IMAGE_RELATIVE_FILENAME = "images/ARDrone.png"; 
	//       relative to SimAbstractRobot class
	
	// measured dimensions
	//    the ARDrone is completely square.  It has two different dimensions, though, 
	//    depending on whether its outer shell is on or not.
	public static final double SIDE_LENGTH_MINIMALISTIC = 0.46;
	public static final double SIDE_LENGTH_WITH_EXTERNAL_SHELL = 0.53;
	public static final double Z_LENGTH_MINIMALISTIC = 0.075;
	public static final double Z_LENGTH_WITH_EXTERNAL_SHELL = 0.10;
	
	private static final double MAX_Z_CHANGE_PER_SECOND = 0.3; // no more than 
	//     a third of a meter per second.  "reasonable" value based on my 
	//     recollection of flying the quadrotor.
	
	// the Era base will not change its "platonic shape", so may as well
	//     create it once and then cache it.
	private SimShape cachedPlatonicShape;
	
	private CameraSpecs cameraSpecs;
	
	private boolean wearingExternalShell; // flag if should use shell dimensions
	

	private double goalHoverHeight = 0; // if in flying mode, need to know what height to be at.
	//    initially 0, i.e., at rest.
	
	
	public SimARDroneRobot(SimModel model, String serverIDName, 
			boolean noisyOdometry, CameraSpecs cameraSpecs, boolean wearingExternalShell) {
		super(model, serverIDName, noisyOdometry, IMAGE_RELATIVE_FILENAME);
		this.cameraSpecs = cameraSpecs;
		this.wearingExternalShell = wearingExternalShell;
	}

	/** gets shape in the "platonic" "ideal" form, centered at 0,0 and 
	 * oriented to the right (i.e. 0 orientation).
	 */
	@Override
	public SimShape getPlatonicShape() {
		if (this.cachedPlatonicShape == null) {
			double sideLength, zLength;
			if (wearingExternalShell) {
				sideLength = SIDE_LENGTH_WITH_EXTERNAL_SHELL;
				zLength = Z_LENGTH_WITH_EXTERNAL_SHELL;
			} else {
				sideLength = SIDE_LENGTH_MINIMALISTIC;
				zLength = Z_LENGTH_MINIMALISTIC;
			}

			double halfSideLength = sideLength/2.0;
			PointCollection points = PointCollection.createRectangleFromCornerPoints(
					new Point2D.Double(-1 * halfSideLength, -1 * halfSideLength),
					new Point2D.Double(halfSideLength, halfSideLength));
			this.cachedPlatonicShape = new SimShape(
					points, null, 0, zLength, PushabilityDeterminer.alwaysFalse); 
			// PushabilityDeterminer.alwaysFalse = robot is not pushable.
		}
		
		return this.cachedPlatonicShape;
	}
	

	@Override
	/** overwrite getCameraSpecs to show that yes, do have a camera */
	public CameraSpecs getCameraSpecs() {
		return this.cameraSpecs;
	}


	@Override
	public String getTooltipDescription() {
		return "robot id = " + this.getName() + ", " + 
					"location = " + SimUtil.getPointCoordinatePair(getLocation()) + ", <br> orienation = " + 
					SimUtil.formatDecimal(Math.toDegrees(this.getLocationSpecifier().getTheta()))
					+ SimUtil.DEGREE_SYMBOL + 
					", <br> sonar height reading = " + SimUtil.formatDecimal(getSonarHeight()) + 
					" (actual ground height = " + SimUtil.formatDecimal(this.getGroundHeight()) + ")"; 
					
	}
	
	private double getSonarHeight() {
		double heightAboveGround = this.getGroundHeight();
		SimShape robotCurrentShape = this.getShape();
		
		double highestObjectBelowMeZ = 0;
		
		// find all objects below me, and ensure I'm not landing on top of them!
		for (SimShape eachWorldShape : this.getModel().getLaserVisibleShapes(0, heightAboveGround)) {
			if (eachWorldShape.intersectsShape(robotCurrentShape)) {
				highestObjectBelowMeZ = Math.max(highestObjectBelowMeZ, eachWorldShape.getZ() + eachWorldShape.getZLength());
			}
		}
		
		return (heightAboveGround - highestObjectBelowMeZ);
	}

	@Override
	public ArrayList<PopupObjectAction> getPopupRobotActions() {
		return null;
	}

	@Override
	public double getZPositionDisplacementIfAny() {
        double maxDisplacementPerTick = MAX_Z_CHANGE_PER_SECOND / (double)ADESimEnvironmentComponentImpl.SIM_TIME_RESOLUTION;

        double sonarHeight = getSonarHeight();
                  
		if (sonarHeight == goalHoverHeight) {
			return 0; // golden
		} else if (sonarHeight > goalHoverHeight) {
			if (sonarHeight - maxDisplacementPerTick > goalHoverHeight) {
				return -1 * maxDisplacementPerTick; // full speed down
			} else {
				return goalHoverHeight - sonarHeight; // just the diff
			}
		} else { // if sonarHeight < goalHoverHeight) 
			if (sonarHeight + maxDisplacementPerTick < goalHoverHeight) {
				return maxDisplacementPerTick; // full speed up!
			} else {
				return goalHoverHeight - sonarHeight;
			}
		}
        
	}
	
	public double getGoalHoverHeight() {
		return goalHoverHeight;
	}
	
	public void setGoalHoverHeight(double goalHoverHeight) {
		this.goalHoverHeight = Math.max(0, goalHoverHeight);
	}

}
