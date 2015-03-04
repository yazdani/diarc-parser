package com.adesim.gui;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

import javax.swing.JOptionPane;

import com.adesim.gui.customKeyListener.CustomKeyListener;
import com.adesim.gui.customKeyListener.CustomKeyProcessor;
import com.adesim.objects.Block;
import com.adesim.objects.Box;
import com.adesim.objects.Door;
import com.adesim.objects.Landmark;
import com.adesim.objects.SimContainerEntity;
import com.adesim.objects.SimEntity;
import com.adesim.objects.Wall;
import com.adesim.objects.model.ObjectMover;
import com.adesim.util.SimUtil;

public class ObjectEditingListener implements MouseListener, MouseMotionListener, CustomKeyListener {

	public static final double ROTATION_OFFSET = 2 * Math.PI / 180;;
	
	private SimPanel simPanel;
	
	private SimEntity draggedObject;
	private Point formerMousePoint;
	
	private CustomKeyProcessor keyProcessor;
	
	private ObjectEditingListener meTheObjectEditingListener;

	// used when stretching, to ensure that mouse stays on same quadrant.
	//    clear this anytime that mouse is released or shift is released.
	private Point2D.Double originalStretchingCenterDiff;

	
	/** pass robotDraggingListener so that don't move object at same time as moving robot, same with RobotKeyListener */
	public ObjectEditingListener(SimPanel passedPanel) {
		this.simPanel = passedPanel;

		simPanel.addMouseListener(this);
		simPanel.addMouseMotionListener(this);
		
		keyProcessor = new CustomKeyProcessor(simPanel.vis, this); 
		// for some probably-clever Swing reason, key listening needs
		//       to be done on the Visualization window rather than the panel.
	}
	
	public boolean isDragging() {
		return (draggedObject != null);
	}


	@Override
	public void mousePressed(MouseEvent e) {
		if (simPanel.vis.mouseListenerOverriders.size() > 0) {
			return;
		}
		
		// if already dragging robot, don't move an object as well
		if (simPanel.robotDraggingListener.isMovingRobot()) {
			return;
		}
		
		// if it was a right click, ignore
		if (e.isPopupTrigger()) {
			return;
		}
		
		Point point = e.getPoint();
		// if clicked on top of one of the SimPanel's  hypotheticalShapes, those take precedence
		//   (that way, don't move wall sitting below a hypothetical wall, otherwise couldn't separate them)
		//   so:
		if (simPanel.clickedOnHypotheticalShape(e.getPoint())) {
			return;
		}
		
		// 3D viewpoints have their own listener.
		if (simPanel.clickedOn3DviewShape(point)) {
			return;
		}

		draggedObject = clickedOnDraggableObject(point);
		if (draggedObject != null) {
			formerMousePoint = e.getPoint();
			
			// if alt is held down, make a CLONE of the object
			if (e.isAltDown()) {
				final SimEntity clonedSimEntity = draggedObject.clone();
				draggedObject = null; // wait for it...
				try {
					simPanel.vis.callComponent("addObjectToMap", clonedSimEntity);
					
					// wait till object becomes available to the GUI (after env update):
					sleepAndExecuteAfterNextTick(new Runnable() {
						public void run() {
							draggedObject = clonedSimEntity; // there we go, now I'm dragging it.
						}
					});
					
				} catch (Exception e1) {
					simPanel.vis.showErrorMessage("Could not add cloned object to map...", "Cloning error");
				}
			}
		}
		
		// disable robotListeneing on part of the RobotKeyListener
		simPanel.vis.robotKeyboardListenerOverrides.add(meTheObjectEditingListener);
	}
	
	
	/** Can't just do Thread.sleep, as it blocks the whole process if this is run through ADEGUI.
		instead, set draggedObject to nothing, and set it back after sleeping, along with 
		calling the rest of the runnable code (if any; can be left blank)  **/
	private void sleepAndExecuteAfterNextTick(final Runnable whatToExecute) {
		final int startingTickCount = simPanel.vis.tickCount;
		final SimEntity previousDraggedObject = draggedObject;
		draggedObject = null;
		
		Thread delayedRunner = new Thread() {
			@Override
			public void run() {
				while (simPanel.vis.tickCount == startingTickCount) {
					try {
						Thread.sleep(100);
					} catch (Exception e2) {
						// can't sleep?  oh well, busy wait...
					}
				}
				
				draggedObject = previousDraggedObject;
				
				if (whatToExecute != null) {
					whatToExecute.run();
				}
			}			
		};
		delayedRunner.start();
	}

	private SimEntity clickedOnDraggableObject(Point point) {
		Rectangle mouseSurroundRect = SimPanel.getMouseSurroundRect(point);
		
		ListIterator<SimEntity> reverseIterator = simPanel.simEntityDrawnList.listIterator(simPanel.simEntityDrawnList.size());
		while (reverseIterator.hasPrevious()) {
			SimEntity eachEntity = reverseIterator.previous();
			
			boolean allowedToMove;
			if (eachEntity instanceof Block) {
				allowedToMove = simPanel.vis.menuBar.editMenuAllowEditBlocks.isSelected();
			} else if (eachEntity instanceof Box) {
				allowedToMove = simPanel.vis.menuBar.editMenuAllowEditBoxes.isSelected();
			} else if (eachEntity instanceof Door) {
				allowedToMove = simPanel.vis.menuBar.editMenuAllowEditDoors.isSelected();
			} else if (eachEntity instanceof Landmark) {
				allowedToMove = simPanel.vis.menuBar.editMenuAllowEditLandmarks.isSelected();
			} else if (eachEntity instanceof Wall) {
				allowedToMove = simPanel.vis.menuBar.editMenuAllowEditWalls.isSelected();
			} else {
				System.out.println("Could not determine if allowed to move entity of type " + 
						eachEntity.getType() + ".  Assuming FALSE.");
				allowedToMove = false;
			}
			
			
			if (allowedToMove) {
				if (simPanel.simEntityIDtoAWTshapeMap.get(eachEntity.getGUID()).intersects(mouseSurroundRect)) {
					simPanel.vis.setStatusLabelText(eachEntity.getNameOrType(true) + 
							".  Drag w/ mouse, shift+drag to stretch. Copy = alt BEFORE click. " +
							"Keyboard: ←/→ = rotate. Del = remove. Enter = rename");
					return eachEntity;
				}
			}
			// if was not allowed to move, then just keep searching.  maybe there's another object
			//     that IS movable in the vicinity.
		}
		
		// if found nothing
		return null;
	}


	@Override
	public void mouseReleased(MouseEvent e) {
		// if was dragging object, snap it into the center of it's container, if relevant.
		if (draggedObject != null) {
			UUID containingObjectID = simPanel.vis.model.worldObjects.getObjectFromGUID(
					draggedObject.getGUID()).getContainingObjectID();
			if (containingObjectID != null) {
				try {
					SimEntity containingObject = simPanel.vis.model.worldObjects.getObjectFromGUID(containingObjectID);
					draggedObject.snapToCenter(containingObject.getShape());
					simPanel.vis.callComponent("updateObjectShapeOnMap", draggedObject.getGUID(), draggedObject.getShape());
				} catch (Exception e1) {
					simPanel.vis.showErrorMessage("Failed to drop object in center of its container...", "Error moving object");
				}
			}
		}
		
		
		draggedObject = null;
		this.originalStretchingCenterDiff = null;
		
		// return control to robot key listener.
		simPanel.vis.robotKeyboardListenerOverrides.remove(meTheObjectEditingListener);
		
		// reset the usual status label:
		simPanel.vis.setStatusLabelMousePoint(e.getPoint());
	}


	@Override
	public void mouseDragged(MouseEvent e) {
		if (draggedObject == null) 
			return;

		if (formerMousePoint == null)
			formerMousePoint = e.getPoint();

		// check if regular motion, or if holding shift (indicating resize)
		boolean holdingShift = keyProcessor.checkKeyHeld(KeyEvent.VK_SHIFT);
		
		
		if (holdingShift) {
			mouseDraggedStretchingHelper(e.getPoint());
		} else {
			mouseDraggedRegularHelper(e.getPoint());
		}
		

		formerMousePoint = e.getPoint(); // update former point
	}

	private void mouseDraggedStretchingHelper(Point point) {
		Point2D objectCenter = draggedObject.getShape().getCenter();
		
		Point2D currentMouseModelPt = simPanel.vis.worldPointFromVisCoordinates(point);
		if (currentMouseModelPt == null) {
			// mouse beyond window bounds; ignore.
			return;
		}

		double currentCenterDx = currentMouseModelPt.getX() - objectCenter.getX();
		double currentCenterDy = currentMouseModelPt.getY() - objectCenter.getY();
		if (this.originalStretchingCenterDiff == null) {
			this.originalStretchingCenterDiff = new Point2D.Double(currentCenterDx, currentCenterDy);
		}
		
		Point2D formerMouseModelPt = simPanel.vis.worldPointFromVisCoordinates(formerMousePoint);		
		if (formerMouseModelPt == null) {
			// mouse beyond window bounds; ignore.
			return;
		}
		
		Point2D objectMaxPt = draggedObject.getShape().getMax();
		
		
		// set up fractions for scaling.  by default, the "identity" fraction, i.e., don't do anything.
		double xFraction = 1; 
		double yFraction = 1;
		
		
		// only allow dragging if stays in same quadrant, i.e., sign of pt - center is the same
		//     for former and current pt.
		// If do drag, I want the distance from max to remain the same.  I.e., expand/shrink so 
		//     that max will still be in same distance from mouse point.
		
		// if same sign and everything works out, set xFraction
		if (SimUtil.sameSign(this.originalStretchingCenterDiff.getX(), currentCenterDx)) {
			double formerCenterDx = (formerMouseModelPt.getX() - objectCenter.getX());
			double upperQuadrantPtX = objectCenter.getX() + Math.abs(formerCenterDx);
			
			double formerDistFromMaxX = objectMaxPt.getX() - upperQuadrantPtX; 
			
			double goalMaxPtX = objectCenter.getX() + Math.abs(currentCenterDx) + formerDistFromMaxX;
			
			double formerMaxFromCenterDiffX = objectMaxPt.getX() - objectCenter.getX();
			
			double goalMaxDromCenterDiffX = goalMaxPtX - objectCenter.getX();
			
			if (formerMaxFromCenterDiffX > 0) { // avoid division by 0
				xFraction = goalMaxDromCenterDiffX / formerMaxFromCenterDiffX;
			} 
		}
		
		
		// if same sign and everything works out, set xFraction
		if (SimUtil.sameSign(this.originalStretchingCenterDiff.getY(), currentCenterDy)) {
			double formerCenterDy = (formerMouseModelPt.getY() - objectCenter.getY());
			double upperQuadrantPtY = objectCenter.getY() + Math.abs(formerCenterDy);
			
			double formerDistFromMaxY = objectMaxPt.getY() - upperQuadrantPtY;
			
			double goalMaxPtY = objectCenter.getY() + Math.abs(currentCenterDy) + formerDistFromMaxY;
			
			double formerMaxFromCenterDiffY = objectMaxPt.getY() - objectCenter.getY();
			
			double goalMaxDromCenterDiffY = goalMaxPtY - objectCenter.getY();
			
			if (formerMaxFromCenterDiffY > 0) {
				yFraction = goalMaxDromCenterDiffY / formerMaxFromCenterDiffY;
			}
		}
		
		
		ObjectMover.scaleShapeOnly(draggedObject.getShape(), xFraction, yFraction);
		
		try {
			simPanel.vis.callComponent("updateObjectShapeOnMap", draggedObject.getGUID(), draggedObject.getShape());
		} catch (Exception e1) {
			simPanel.vis.showErrorMessage("Failed to scale object to follow mouse dragging...", "Error scaling object");
		}
	}

	private void mouseDraggedRegularHelper(Point point) {
		double visDx = point.getX() - formerMousePoint.getX();
		double visDy = -1 * (point.getY() - formerMousePoint.getY());
		// -1 since screen y grows down, Eucledian grows up
		Point2D modelOffset = new Point2D.Double(
				simPanel.vis.getModelPointDiff(visDx), simPanel.vis.getModelPointDiff(visDy));
		
		List<SimEntity> impactedShapes = ObjectMover.translate(draggedObject, modelOffset.getX(), modelOffset.getY(), 0);
		for (SimEntity eachImpactedShape : impactedShapes) {
			try {
				simPanel.vis.callComponent("updateObjectShapeOnMap", eachImpactedShape.getGUID(), eachImpactedShape.getShape());
			} catch (Exception e1) {
				simPanel.vis.showErrorMessage("Failed to move object to follow mouse dragging...", 
						"Error moving object");
			}
		}
		
		// check if the new position is over a container:
		SimEntity mousedOverContainer = findContainerBeneathMousePointIfAny(point);
		perfromActionsBasedOnMousingOverContainer(mousedOverContainer);
	}

	private void perfromActionsBasedOnMousingOverContainer(SimEntity mousedOverContainer) {
		if (  (mousedOverContainer != null) && (draggedObject.getGUID().equals(mousedOverContainer.getGUID()))  ) {
			return; // just moving the container itself.  It's all good.
		}
		
		
		UUID newParentGUID = (  (mousedOverContainer == null) ? null : mousedOverContainer.getGUID()  );
		
		// the call to updateObjectParentOnMap will take an array of UUID.  empty will symbolize moving to environment,
		//    singleton will symbolize moving to particular id.  The reason that have to pass an empty array is that
		//    (as on Nov 2010), ADE does not allow for null-parameter passing.
		UUID destinationUUIDarray[];
		if (newParentGUID == null) {
			destinationUUIDarray = new UUID[] {};
		} else {
			destinationUUIDarray = new UUID[] {newParentGUID};
		}
		
		
		// for efficiency's sake, try to see if don't even need to update anything (i.e., if moving to DIFFERENT object).
		//    If can't tell, just go with the update, no need to throw exceptions!
		try {
			// if actually moving, proceed, otherwise quit now
			//    note:   for some reason, need to do the "long" call to figure out the real containing object ID, 
			//    rather than just asking the draggedObject.  But fine, I'm willing to deal with that.
			if (SimUtil.IDsAreEqual(
					simPanel.vis.model.worldObjects.getObjectFromGUID(draggedObject.getGUID()).getContainingObjectID(),
					newParentGUID)) {
				return;
			}
		} catch (Exception e) {
			// ignore the exception
		}
		

		// finally, inform the environment:
		try {
			UUID thisObjectGUID = draggedObject.getGUID();
			simPanel.vis.callComponent("updateObjectParentOnMap", draggedObject.getGUID(), destinationUUIDarray);
			
			// will avoid occasional sporadic errors if block till GUI registers this change of ownership
			//    (i.e., till a tick passes).
			sleepAndExecuteAfterNextTick(null);
			
			draggedObject = simPanel.vis.model.worldObjects.getObjectFromGUID(thisObjectGUID);
		} catch (Exception e1) {
			simPanel.vis.showErrorMessage("Failed to update object's containing \"parent\".", 
					"Object-moving error");
		}	
	}


	private SimEntity findContainerBeneathMousePointIfAny(Point point) {
		ListIterator<SimEntity> reverseIterator = simPanel.simEntityDrawnList.listIterator(simPanel.simEntityDrawnList.size());
		while (reverseIterator.hasPrevious()) {
			SimEntity eachEntity = reverseIterator.previous();
			if (eachEntity instanceof SimContainerEntity) {
				if (simPanel.simEntityIDtoAWTshapeMap.get(eachEntity.getGUID()).contains(point)) {
					return eachEntity;
				}
			}
		}
		// if found nothing
		return null;
	}

	
	@Override
	public void keyPressed(int keyCode) {
		if (this.draggedObject == null) {
			return;
		}
		
		// check for delete key:
		if (  (keyCode == KeyEvent.VK_DELETE) || (keyCode == KeyEvent.VK_BACK_SPACE)  ) {
			try {
				if ((Boolean) simPanel.vis.callComponent("removeObjectFromMap", draggedObject.getGUID())) {
					this.draggedObject = null;
				} else {
					simPanel.vis.showErrorMessage("Failed to remove object.",
					"Could not remove object");
				}
			} catch (Exception e1) {
				simPanel.vis.showErrorMessage("Failed to remove object due to " + e1, 
						"Could not remove object");
			}
		} else if ( (keyCode == KeyEvent.VK_LEFT) || (keyCode == KeyEvent.VK_RIGHT) ) {
			int directionMultiplier = (  (keyCode == KeyEvent.VK_LEFT) ? 1 : -1 );
			List<SimEntity> impactedRotatedObjects = 
					ObjectMover.rotate(draggedObject, ROTATION_OFFSET * directionMultiplier);
			for (SimEntity eachRotatedObject : impactedRotatedObjects) {
				try {
					simPanel.vis.callComponent("updateObjectShapeOnMap", eachRotatedObject.getGUID(), 
							eachRotatedObject.getShape());
				} catch (Exception e1) {
					simPanel.vis.showErrorMessage("Failed to rotate object due to " + e1, 
							"Could not rotate object");
				}
			}
		} else if (keyCode == KeyEvent.VK_ENTER) {
			String newName = JOptionPane.showInputDialog (
					simPanel, "Please enter a new name for " +
					draggedObject.getNameOrType(true) + ".", 
					draggedObject.getName());
			if (newName != null) {
				try { 
					simPanel.vis.callComponent("updateObjectNameOnMap", draggedObject.getGUID(), newName);
				} catch (Exception e1) {
					simPanel.vis.showErrorMessage("Failed to rename object due to " + e1, 
							"Could not rename object");
				}
			}
		}		
	}
	
	@Override
	public void keyReleased(int keyCode) {
		if (keyCode == KeyEvent.VK_SHIFT) {
			this.originalStretchingCenterDiff = null;
		}
	}
	

	@Override
	public void mouseMoved(MouseEvent e) { 
		// don't care about moving, only dragging.				
	}

	@Override
	public void mouseExited(MouseEvent e) { /* don't care */ }
	@Override
	public void mouseEntered(MouseEvent e) { /* don't care */ }
	@Override
	public void mouseClicked(MouseEvent e) { /* don't care */ }


}
