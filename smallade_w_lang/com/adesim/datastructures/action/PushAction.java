package com.adesim.datastructures.action;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Map.Entry;

import utilities.xml.Xml;

import com.adesim.datastructures.SimShape;
import com.adesim.objects.SimEntity;
import com.adesim.objects.model.ActorModelOwner;
import com.adesim.objects.model.SimModel;
import com.adesim.robot.SimLocationSpecifier;

public class PushAction extends SimAction {
	private static final long serialVersionUID = 1L;
	
	
	@Override
	/* Receives model owner so that can route requests to environment, if necessary. 
	 * Note that can assume that the owner is an ActorModelOwner because 
	 * the actions only get performed on collision with robot, and the environment
	 * and the GUI own no robot! */
	public void perform(int tickCounter, SimEntity simEntity, 
			ActorModelOwner actorModelOwner, SimLocationSpecifier robotDisplacement) {
		if (robotDisplacement == null) {
			// if robot didn't move, just ignore this whole pushing thing
			return;
		}
		
		Point2D offset = robotDisplacement.getXYLocation();
		SimModel model = actorModelOwner.getModel();
		String robotName = model.robot.getName();
		
		if (generatePushedShapeIfCanPushObject(simEntity.getShape(), offset, robotName, model) != null) {
			actorModelOwner.callEnvironment("requestPushObject", simEntity.getGUID(), offset, robotName);	
		}
	}
	
	/** generates hypothetical pushed shape, or returns null if that would intersect
	 *  other robots or laser-visible world shapes */
	public static SimShape generatePushedShapeIfCanPushObject(SimShape shape, Point2D offset, 
			String thisRobotName, SimModel model) {
		
		SimShape hypotheticalShape = shape.createdTransformedShape(
				AffineTransform.getTranslateInstance(offset.getX(), offset.getY()));
		
		// check if intersects other robot shapes.  The actor would not have its own name
		//     in the "other" robots anyway, but the environment would, hence the extra
		//     check against robot name.
		for (Entry<String, SimShape> eachOtherRobot : model.otherRobotShapes.entrySet()) {
			if (!eachOtherRobot.getKey().equals(thisRobotName)) {
				if (eachOtherRobot.getValue().intersectsShape(hypotheticalShape)) {
					return null;
				}
			}
		}
		
		// also check against other laser-visible shapes
		for (SimShape eachShape : model.getLaserVisibleShapes(shape.getMinMaxZ())) {
			if (eachShape != shape) {
				if (eachShape.intersectsShape(hypotheticalShape)) {
					return null;
				}
			}
		}
		
		// if still here:
		return hypotheticalShape;
	}

	@Override
	public Xml generateXMLinner() {
		return new Xml("push");
	}
}
