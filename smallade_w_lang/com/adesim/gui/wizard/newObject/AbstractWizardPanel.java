package com.adesim.gui.wizard.newObject;

import java.awt.Container;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;

import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import ade.gui.Util;

import com.adesim.datastructures.PointCollection;
import com.adesim.datastructures.PushabilityDeterminer;
import com.adesim.datastructures.SimShape;
import com.adesim.datastructures.action.PushAction;
import com.adesim.gui.ADESimMapVis;
import com.adesim.gui.ObjectEditingListener;
import com.adesim.gui.SimPanel;
import com.adesim.gui.customKeyListener.CustomKeyListener;
import com.adesim.gui.customKeyListener.CustomKeyProcessor;
import com.adesim.gui.images.GuiImageHelper;
import com.adesim.gui.wizard.WizardUtil;
import com.adesim.gui.wizard.components.ColorChooserPanel;
import com.adesim.gui.wizard.components.DisappearingStatusLabel;
import com.adesim.gui.wizard.components.NumberTextField;
import com.adesim.objects.SimEntity;
import com.adesim.objects.model.ObjectMover;
import com.adesim.util.SimUtil;

public abstract class AbstractWizardPanel extends JPanel implements 
				MouseListener, MouseMotionListener, CustomKeyListener {
	private static final long serialVersionUID = 1L;

	protected ADESimMapVis vis;
	protected AbstractWizardPanel meTheWizard;

	// ultimately, all wizards will be dealing with a SimShape "currentShape".
	//    (once that is updated, let the visualization know via method "updateVisShapeFromCurrentShape")
	protected SimShape currentShape = null;

	// also a shared previousMousePoint (for dragging, etc).
	protected Point formerMousePoint;



	// common components.  it is ok for the name, t, and z length to be null (i.e., not present in wizard)

	protected Point dragRectangleStartingPoint;
	protected JToggleButton toggleButtonDragShape;


	protected JTextField textFieldName;

	protected ColorChooserPanel colorPanel;
	protected DisappearingStatusLabel statusLabel;

	protected NumberTextField textFieldZ;
	protected JTextField textFieldZLength;

	
	private CustomKeyProcessor customKeyProcessor;
	
	// used when stretching, to ensure that mouse stays on same quadrant.
	//    clear this anytime that mouse is released or shift is released.
	private Point2D.Double originalStretchingCenterDiff;



	protected ActionListener createEditShapeHelpActionListener() {
		return new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String message = "Once you have drawn a shape, you can edit it by ACTIVATING THE MAP WINDOW and then:" +
						"\n1) Dragging the shape with the mouse to a different location." +
						"\n2) Scaling the image by holding SHIFT and dragging a corner." +
						"\n3) Pressing up/down to SCALE the shape uniformly." +
						"\n4) Pressing left/right to ROTATE it." +
						"\n5) Pressing delete/backspace to delete the shape, so as to start from scratch.";
		
				JOptionPane.showMessageDialog(meTheWizard, 
						Util.breakStringIntoMultilineString(message), 
						"Help for shape editing",JOptionPane.INFORMATION_MESSAGE, GuiImageHelper.helpIcon);
			}
		};
	}



	protected abstract String getTitle();
	protected abstract void initGUI();
	
	protected abstract String getDrawShapeToggleText(boolean selected);
	protected abstract SimEntity createSimEntity(SimShape shape, String objectName);
	
	
	public AbstractWizardPanel(ADESimMapVis vis) {
		super();
		
		meTheWizard = this;
		
		this.vis = vis;

		initGUI();


		// the if statement below is only to appease WindowBuilder Pro 
		//     (http://code.google.com/javadevtools/download-wbpro.html, free add-in into Eclipse)
		//     and to allow me to design the GUIs via drag-and-drop.  In reality, vis better be NOT null!
		if (vis != null) {

			// create Frame or JInternal frame:
			Container containingWindow = vis.createWindowForHelperPanel(
					this, this.getSize(), false, false, getTitle(), JFrame.DISPOSE_ON_CLOSE);
			
			if (containingWindow instanceof JFrame) {
				((JFrame)containingWindow).addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosed(WindowEvent e) {
						windowClosingCallback();
					}
				});
			} else if (containingWindow instanceof JInternalFrame) {
				((JInternalFrame) containingWindow).addInternalFrameListener(new InternalFrameAdapter() {
					@Override
					public void internalFrameClosed(InternalFrameEvent e) {
						windowClosingCallback();
					}
				});
			} else {
				System.out.println("Containing window is neither a JFrame nor a JInternalFrame, " +
						"can't create a hook into a window-closing callback.");
			}
			
			
			// common startup, hooking up mouse and key listeners:
			vis.getSimPanel().addMouseListener(this);
			vis.getSimPanel().addMouseMotionListener(this);
			
			customKeyProcessor = new CustomKeyProcessor(vis, this); // key listener gets added to vis, not to panel.
			//   why?  because of deep and dangerous Swing mysteries, as far as I'm concerned.
			
			

			// mouse listening depends on the toggle, but the keyboard
			//     can be used essentially at any time, and so should take that away from the 
			//     robot immediately.
			vis.robotKeyboardListenerOverrides.add(this);



			toggleButtonDragShape.setSelected(true); // this is probably what user will want to do.
			toggleButtonDragShapeToggledCallback(); // thank you Swing for forcing me to put it in this call...
			
			
			statusLabel.setVisible(false);
		}
	}



	protected void toggleButtonDragShapeToggledCallback() {
		if (toggleButtonDragShape.isSelected()) {
			// clear any previous points:
			currentShape = null;
			vis.getSimPanel().removeHypotheticalShape(this);
			// disable "regular" map mouse listeners (robot dragging, etc)
			vis.mouseListenerOverriders.add(this);
		} else {
			vis.mouseListenerOverriders.remove(this);
		}
		
		toggleButtonDragShape.setText(getDrawShapeToggleText(toggleButtonDragShape.isSelected()));
	}

	protected void updateZandZLengthIfRelevant() {
		double z = (textFieldZ == null) ? 0 : Double.parseDouble(textFieldZ.getText());

		Double zLength;
		if (   (textFieldZLength == null) || (textFieldZLength.getText().length() == 0)   ) {
			zLength = null;
		} else {
			zLength = Double.parseDouble(textFieldZLength.getText()); 
		}

		currentShape.setZ(z);
		currentShape.setZLength(zLength);
	}


	protected void createObject() {
		if (!WizardUtil.canProceedBasicCheckAndAlert(this)) {
			return;
		}
		
		if (currentShape == null) {
			JOptionPane.showMessageDialog(this, "No shape specified", 
					"Please drag a shape first!", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		if (!canProceedExtraChecks()) {
			return;
		}
		

		// assuming can proceed:

		// can de-select now that the user is creating the wall, that way will have 
		//    the ability to move things around in the environment again (and anyway, you shouldn't 
		//    keep editing a wall if you've committed enough to it to add it).
		//    However, don't destroy the wall even after creating the wall object,
		//    as the user might want to create duplicated of it (i.e., drag off the first wall somewhere, 
		//    and create another in its place).


		toggleButtonDragShape.setSelected(false);
		toggleButtonDragShapeToggledCallback(); // swing events are pesky.   just call the toggling 
		//   callback so that it can set the text appropriately, and return control to visualization's 
		//   mouse listeners.


		updateZandZLengthIfRelevant();

		try {
			String name = (textFieldName == null) ? null : WizardUtil.getRealStringOrNull(textFieldName.getText());

			vis.callComponent("addObjectToMap", createSimEntity(currentShape.clone(), name));

			// even after adding block, don't close window, in case want to create a similar block
			statusLabel.setText("Object added.  Add another?");
		} catch (Exception e1) {
			e1.printStackTrace();
			vis.showErrorMessage("Could not add box due to " + e1, "Could not add box!");
		}

	}


	/** extra error-checking method, if subclasses want to override it */
	protected boolean canProceedExtraChecks() {
		return true;
	}



	/******* HELPFUL METHODS FOR SUB-CLASSES ********/

	protected SimShape generateShapeFromDraggingRectangle(Point startPoint, Point endPoint) {
		Point2D startPtModel = vis.worldPointFromVisCoordinates(startPoint);
		Point2D endPtModel = vis.worldPointFromVisCoordinates(endPoint);
		
		// ensure points were not out of bounds
		if (  (startPtModel == null) || (endPtModel == null)  ) {
			return null;
		}

		PointCollection points = PointCollection.createRectangleFromCornerPoints(startPtModel, endPtModel);
		
		return new SimShape(points, null, 0, null, PushabilityDeterminer.alwaysFalse);
		//   at this stage in the wizarding process process, don't care about z and z-length
		//     (those will be appended on object creation, if relevant).  
		//     Don't care about actions and pushability, either.
	}
	
	
	protected void appendPushActionAndSetPushabilityDeterminer(SimShape shape) {
		shape.actions.add(new PushAction());
		shape.pushabilityDeterminer = PushabilityDeterminer.pushableIfObjectWillStillBeObstacleFreeAfterMove;
	}


	
	protected void editingKeyPressCommon(int keyCode) {
		if (!vis.getSimPanel().objectEditingListener.isDragging()) {
			if (currentShape != null) {
				if (  (keyCode == KeyEvent.VK_DELETE) || (keyCode == KeyEvent.VK_BACK_SPACE)  ) {
					currentShape = null;
					updateVisShapeFromCurrentShape();
				} else if ( (keyCode == KeyEvent.VK_LEFT) || (keyCode == KeyEvent.VK_RIGHT) ) {
					int directionMultiplier = (  (keyCode == KeyEvent.VK_LEFT) ? 1 : -1 );
					ObjectMover.rotateShapeOnly(currentShape, directionMultiplier * ObjectEditingListener.ROTATION_OFFSET);
					updateVisShapeFromCurrentShape();
				} else if ( (keyCode == KeyEvent.VK_UP) || (keyCode == KeyEvent.VK_DOWN) ) {
					double sizeFraction = (  (keyCode == KeyEvent.VK_UP) ? 1.03 : 0.97 );
					ObjectMover.scaleShapeOnly(currentShape, sizeFraction, sizeFraction);
					updateVisShapeFromCurrentShape();
				}
			}
		}
	}


	protected void editingMousePressCommon(Point point) {
		if (currentShape != null) {
			// might be editing -- so store the clicked on point as a reference
			if (vis.getSimPanel().getPolygonFromSimShape(currentShape).intersects(
					SimPanel.createMouseSurroundRectangle(point))) {
				formerMousePoint = point;
			}
		}
	}

	protected void editingMouseDragCommon(Point point) {
		if (currentShape != null) {
			// might be editing existing shape
			if (formerMousePoint != null) {
				// check if regular motion, or if holding shift (indicating resize)
				boolean holdingShift = customKeyProcessor.checkKeyHeld(KeyEvent.VK_SHIFT);
				
				
				if (holdingShift) {
					mouseDraggedStretchingHelper(point);
				} else {
					mouseDraggedRegularHelper(point);
				}
				
				updateVisShapeFromCurrentShape();

				formerMousePoint = point;
			}
		}
	}


	private void mouseDraggedStretchingHelper(Point point) {
		Point2D objectCenter = currentShape.getCenter();
		
		Point2D currentMouseModelPt = vis.worldPointFromVisCoordinates(point);
		if (currentMouseModelPt == null) {
			// mouse beyond window bounds; ignore.
			return;
		}
		
		double currentCenterDx = currentMouseModelPt.getX() - objectCenter.getX();
		double currentCenterDy = currentMouseModelPt.getY() - objectCenter.getY();
		if (this.originalStretchingCenterDiff == null) {
			this.originalStretchingCenterDiff = new Point2D.Double(currentCenterDx, currentCenterDy);
		}
		
		Point2D formerMouseModelPt = vis.worldPointFromVisCoordinates(formerMousePoint);
		if (formerMouseModelPt == null) {
			// mouse beyond window bounds; ignore.
			return;
		}
		
		Point2D objectMaxPt = currentShape.getMax();
		
		
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
		
		
		ObjectMover.scaleShapeOnly(currentShape, xFraction, yFraction);
		
	}

	private void mouseDraggedRegularHelper(Point point) {
		double visDx = point.getX() - formerMousePoint.getX();
		double visDy = -1 * (point.getY() - formerMousePoint.getY());
		// -1 since screen y grows down, Eucledian grows up

		Point2D modelOffset = new Point2D.Double(
				vis.getModelPointDiff(visDx), 
				vis.getModelPointDiff(visDy));

		ObjectMover.translateShapeOnly(currentShape, modelOffset.getX(), modelOffset.getY(), 0);
	}



	protected void updateVisShapeFromCurrentShape() {
		vis.getSimPanel().addHypotheticalShape(this, currentShape);
	}




	private void windowClosingCallback() {
		vis.getSimPanel().removeHypotheticalShape(this);

		vis.robotKeyboardListenerOverrides.remove(this);
		vis.mouseListenerOverriders.remove(this);

		// no need to leave "dangling" pointers either (I do wonder how Java handles this, actually...
		//    but I'll just be good and clean up after myself):
		customKeyProcessor.removeKeyListener();
		vis.getSimPanel().removeMouseListener(this);
		vis.getSimPanel().removeMouseMotionListener(this);
	}
	
	
	@Override
	public void mouseReleased(MouseEvent e) {
		this.originalStretchingCenterDiff = null;
	}
	
	/** note this is a custom keyPressed, not a Swing one */
	@Override
	public void keyReleased(int keyCode) {
		if (keyCode == KeyEvent.VK_SHIFT) {
			this.originalStretchingCenterDiff = null;
		}
	}




	/**************************************************************************/
	// the required mouse, mouse motion, and keyboard events are listed here.  
	//     override whichever ones you need!
	/**************************************************************************/

	@Override
	public void mouseClicked(MouseEvent e) {}
	@Override
	public void mousePressed(MouseEvent e) {}
	@Override
	public void mouseEntered(MouseEvent e) {}
	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mouseDragged(MouseEvent e) {}
	@Override
	public void mouseMoved(MouseEvent e) {}
	
	/** note this is a custom keyPressed, not a Swing one */
	@Override
	public void keyPressed(int keyCode) {}


}
