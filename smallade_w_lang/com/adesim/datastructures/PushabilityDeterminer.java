package com.adesim.datastructures;

import java.awt.geom.Point2D;
import java.io.Serializable;

import com.adesim.datastructures.action.PushAction;
import com.adesim.objects.model.SimModel;

public interface PushabilityDeterminer extends Serializable {
	
	/** a static PushabilityDeterminer that just always returns false */
	public static PushabilityDeterminer alwaysFalse = new PushabilityDeterminer() {
		private static final long serialVersionUID = 1L;

		@Override
		public boolean isPushable(SimShape shape, Point2D offset, 
				String thisRobotName, SimModel model) {
			return false;
		}
		
		public boolean isHypotheticallyPushable() {
			return false;
		}
	};
	
	public static PushabilityDeterminer pushableIfObjectWillStillBeObstacleFreeAfterMove = 
		new PushabilityDeterminer() {
			private static final long serialVersionUID = 1L;
	
			@Override
			public boolean isPushable(SimShape shape, Point2D offset, 
					String thisRobotName, SimModel model) {
				return (PushAction.generatePushedShapeIfCanPushObject(shape, offset, thisRobotName, model) != null);
			}
			
			public boolean isHypotheticallyPushable() {
				return true;
			}
		};
	
	
	/** the actual method that determines if an object will be pushable or not at a given moment.
	 * @param model */
	public boolean isPushable(SimShape shape, Point2D offset, 
			String thisRobotName, SimModel model);
	
	/** returns true if the object is at least hypothetically pushable, given the right circumstances.
	 * E.g., walls are NEVER hypothetically pushable, whereas a pushable block IS hypothetically
	 * movable, even if, at the moment, it is jammed against the wall */
	public boolean isHypotheticallyPushable();

}
