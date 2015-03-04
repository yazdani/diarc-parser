package com.adesim.gui.wizard.newObject;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import javax.swing.JOptionPane;

import ade.gui.Util;

import com.adesim.datastructures.PointCollection;
import com.adesim.datastructures.PushabilityDeterminer;
import com.adesim.datastructures.SimShape;
import com.adesim.gui.ADESimMapVis;
import com.adesim.gui.images.GuiImageHelper;


public abstract class AbstractComplexShapeWizardPanel extends AbstractWizardPanel {
	private static final long serialVersionUID = 1L;

	private static final double MOVE_INCREMENT = 0.02;

	private static final double SUFFICIENT_DISTANCE_BETWEEN_POINTS_FOR_AUTOGENERATION_WHILE_DRAGGING = 0.20;
	//    20 cm seems like a good balance between dragging intricate shapes, but not populating
	//    a bazillion points (remember, all of those will be slowing down 3D graphics and laser/perception calculations!)


	private PointCollection drawingPoints = new PointCollection(); // point collection WHILE DRAWING (not dragging)  
	//       once start editing, only currentShape will be of any consequence. 

	
	// point-drawing modes:
	private boolean allowDraggingRectangle;
	private boolean allowDragClick;
	private int maxPoints;
	

	protected ActionListener createDrawShapeHelpActionListener() {
		return new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String message = "Draw the shape perimeter by clicking or dragging points on the map. " +
						"\n\nIf you start off dragging immediately, you will drag a RECTANGLE.  " +
						"For a more interesting shape, begin by clicking a point, and THEN either click" +
						"or drag points.  Dragging will create many points on a straight line, which will" +
						"ultimately result in slightly slower laser- and perceptual- readings; thus, if " +
						"the environment you're creating is huge, you may want to minimize the number " +
						"of points and resort to clicking." +
						"\n\nAlso please note that all other clicking functions within the map -- " +
						"dragging robot, moving objects, etc -- " +
						"will be disabled while the perimeter-drawing button is toggled)." + 
						"\n\nTo erase erroniously-placed point, press delete; to adjust the " + 
						"most recently-placed point, press up/down/left/right.";
				
				JOptionPane.showMessageDialog(meTheWizard, 
						Util.breakStringIntoMultilineString(message), 
						"Help for shape editing",JOptionPane.INFORMATION_MESSAGE, GuiImageHelper.helpIcon);
	
			}
		};
	}


	/** abstract constructor.
	 * @param vis:  reference to map visualization
	 * @param allowRectangle:  whether to allow dragging of rectangle or not
	 * @param allowDragClick:  while dragging in point mode, add points automatically?
	 * @param maxPoints:  maximum number of points allowed
	 */
	public AbstractComplexShapeWizardPanel(ADESimMapVis vis, 
			boolean allowRectangle, boolean allowDragClick, int maxPoints) {
		super(vis);
		this.allowDraggingRectangle = allowRectangle;
		this.allowDragClick = allowDragClick;
		this.maxPoints = maxPoints;
	}
	

	@Override
	protected String getDrawShapeToggleText(boolean selected) {
		if (selected) {
			return "<html>Tracking mouse ... <br> Toggle off when done.</html>";
		} else {
			return "<html>Initiate mouse tracking <br>(for clicked " +
			"perimeter points <br> or dragged rectangle)</html>";
		}
	}
	

	/** in addition to common call, also clear drawingPoints on selecting */
	@Override
	protected void toggleButtonDragShapeToggledCallback() {
		super.toggleButtonDragShapeToggledCallback(); // do the common procedure
		
		if (toggleButtonDragShape.isSelected()) {
			if (drawingPoints != null) { // essentially, if not at constructor time:
				// clear any previous points:
				drawingPoints.clear();
			}
		}
	}


	private void updateShapeFromPoints() {
		currentShape = new SimShape(drawingPoints, null, 0, null, PushabilityDeterminer.alwaysFalse);
		//   at this stage in the wizarding process process, don't care about z and z-length
		//     (those will be appended on object creation, if relevant).  
		//     Don't care about actions and pushability, either.

		updateVisShapeFromCurrentShape();
	}


	@Override
	/** clicking is used for the point-by-point drawing of the shape */
	public void mouseClicked(MouseEvent e) {
		super.mouseClicked(e);
		if (e.getButton() == MouseEvent.BUTTON1) { // left-click only
			if (toggleButtonDragShape.isSelected()) {
				addDrawingPoint(vis.worldPointFromVisCoordinates(e.getPoint()));
			}
		}
	}

	private void addDrawingPoint(Point2D worldPoint) {
		if (drawingPoints.size() < maxPoints) {
			drawingPoints.add(worldPoint);
			updateShapeFromPoints();
		}
	}



	@Override
	public void mousePressed(MouseEvent e) {
		super.mousePressed(e);
		// if drawing wall shape, AND NO OTHER POINTS, this MIGHT be the start of a drag.  store possible drag point.
		if (toggleButtonDragShape.isSelected()) {
			if (drawingPoints.size() == 0) {
				dragRectangleStartingPoint = e.getPoint();
			}
		} else {
			this.editingMousePressCommon(e.getPoint());
		}
	}


	@Override
	public void mouseDragged(MouseEvent e) {
		super.mouseDragged(e);
		if (toggleButtonDragShape.isSelected()) {
			if (drawingPoints.size() == 0) {
				if (allowDraggingRectangle) {
					SimShape possibleShape = generateShapeFromDraggingRectangle(dragRectangleStartingPoint, e.getPoint());
					if (possibleShape != null) {
						currentShape = possibleShape;
						updateVisShapeFromCurrentShape();
					}
				}
			} else {
				// if sufficiently far from previous point (at this "else" part, already know it's a 
				//     different point), add it if in dragging mode to the collection of 
				//     previously-drawn points
				if (allowDragClick) {
					Point2D currentPt = vis.worldPointFromVisCoordinates(e.getPoint());
					if (currentPt != null) {
						if (currentPt.distance(drawingPoints.get(drawingPoints.size()-1)) >= 
							SUFFICIENT_DISTANCE_BETWEEN_POINTS_FOR_AUTOGENERATION_WHILE_DRAGGING) {
							// if at least 20 cm away, add it:
							//    (don't want a heapload of points, that would be processor-intensive!)
							addDrawingPoint(currentPt);
						}
					}
				}
			}
		} else {
			this.editingMouseDragCommon(e.getPoint());
		}
	}



	@Override
	public void mouseReleased(MouseEvent e) {
		super.mouseReleased(e);
		formerMousePoint = null; // clear formerMousePoint.
		dragRectangleStartingPoint = null;
	}

	
	/** note this is a custom keyPressed, not a Swing one */
	@Override
	public void keyPressed(int keyCode) {
		super.keyPressed(keyCode);
		
		// two keyboard editing modes:  
		//    one ONLY if toggleButtonDrawWallShape is selected and drawing using POINTS.
		//    the other is "common" editing
		if (  toggleButtonDragShape.isSelected() && (drawingPoints.size() > 0)  ) {
			Point2D pointInQuestion = drawingPoints.get(drawingPoints.size() - 1);

			if (keyCode == KeyEvent.VK_UP) {
				pointInQuestion.setLocation(pointInQuestion.getX(), pointInQuestion.getY() + MOVE_INCREMENT);
			} else if (keyCode == KeyEvent.VK_DOWN) {
				pointInQuestion.setLocation(pointInQuestion.getX(), pointInQuestion.getY() - MOVE_INCREMENT);
			} else if (keyCode == KeyEvent.VK_LEFT) {
				pointInQuestion.setLocation(pointInQuestion.getX() - MOVE_INCREMENT, pointInQuestion.getY());
			} else if (keyCode == KeyEvent.VK_RIGHT) {
				pointInQuestion.setLocation(pointInQuestion.getX() + MOVE_INCREMENT, pointInQuestion.getY());
			} else if (  (keyCode == KeyEvent.VK_DELETE) || (keyCode == KeyEvent.VK_BACK_SPACE)  ) {
				drawingPoints.remove(drawingPoints.size() - 1); // delete last point	
			}

			updateShapeFromPoints();
			
		} else {
			// common editing
			this.editingKeyPressCommon(keyCode);
		}
	}


	
}
