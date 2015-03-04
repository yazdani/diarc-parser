package com.adesim.gui.wizard.newObject;

import java.awt.event.MouseEvent;

import com.adesim.datastructures.SimShape;
import com.adesim.gui.ADESimMapVis;

public abstract class AbstractSimpleDragRectangleWizardPanel extends AbstractWizardPanel {
	private static final long serialVersionUID = 1L;
	
	
	public AbstractSimpleDragRectangleWizardPanel(ADESimMapVis vis) {
		super(vis);
	}
	
	@Override
	protected String getDrawShapeToggleText(boolean selected) {
		if (selected) {
			return "Dragging rectangle.  Done?";
		} else {
			return "Drag rectangle on map";
		}
	}
	
	
	@Override
	public void mousePressed(MouseEvent e) {
		super.mousePressed(e);
		if (toggleButtonDragShape.isSelected()) {
			dragRectangleStartingPoint = e.getPoint();
		} else {
			this.editingMousePressCommon(e.getPoint());
		}
	}
	
	
	@Override
	public void mouseDragged(MouseEvent e) {
		super.mouseDragged(e);
		if (toggleButtonDragShape.isSelected()) {
			SimShape possibleShape = generateShapeFromDraggingRectangle(dragRectangleStartingPoint, e.getPoint());
			if (possibleShape != null) {
				currentShape = possibleShape;
				updateVisShapeFromCurrentShape();
			}
		} else {
			this.editingMouseDragCommon(e.getPoint());
		}
	}
	
	
	@Override
	public void mouseReleased(MouseEvent e) {
		super.mouseReleased(e);
		formerMousePoint = null; // clear formerMousePoint.
	}

	
	/** note this is a custom keyPressed, not a Swing one */
	@Override
	public void keyPressed(int keyCode) {
		super.keyPressed(keyCode);
		this.editingKeyPressCommon(keyCode);
	}
	


}
