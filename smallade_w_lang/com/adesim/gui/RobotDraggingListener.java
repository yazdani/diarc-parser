package com.adesim.gui;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;

import com.adesim.gui.datastructures.RobotVisualizationData;

public class RobotDraggingListener implements MouseListener, MouseMotionListener {

	private SimPanel simPanel;
	private String isMovingRobotID;
	private Point formerMousePoint;

	public RobotDraggingListener(SimPanel passedPanel) {
		this.simPanel = passedPanel;

		simPanel.addMouseListener(this);
		simPanel.addMouseMotionListener(this);
	}


	@Override
	public void mousePressed(MouseEvent e) {
		if (simPanel.vis.mouseListenerOverriders.size() > 0) {
			return;
		}
		
		// if it was a right click, ignore
		if (e.isPopupTrigger()) {
			return;
		}

		isMovingRobotID = clickedOnRobot(e.getPoint());
		if (isMovingRobotID != null) {
			try {
				simPanel.vis.callComponent("allowRobotMotion", isMovingRobotID, false);
			} catch (Exception e1) {
				System.err.println("Could not lift robot off of the ground to prevent it moving while YOU are moving it");
			}

			formerMousePoint = e.getPoint();

			simPanel.vis.setSelectedRobot(isMovingRobotID);
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (isMovingRobotID != null) { // only respond if was actually moving the robot
			try {
				simPanel.vis.callComponent("allowRobotMotion", isMovingRobotID, true);
			} catch (Exception e1) {
				System.err.println("Could not set robot back on ground and have its wheels be functional again.  " + 
						"\nThat could be problematic as far as its own locomotion goes...");
			}

			isMovingRobotID = null;
		}
	}


	@Override
	public void mouseDragged(MouseEvent e) {
		if (isMovingRobotID == null) 
			return;

		if (formerMousePoint == null)
			formerMousePoint = e.getPoint();

		double visDx = e.getPoint().getX() - formerMousePoint.getX();
		double visDy = -1 * (e.getPoint().getY() - formerMousePoint.getY());
		// -1 since screen y grows down, Eucledian grows up

		Point2D modelOffset = new Point2D.Double(
				getModelPointDiff(visDx), getModelPointDiff(visDy));

		try {
			simPanel.vis.callComponent("moveRobotRelativeWithImmediateRefresh", modelOffset, isMovingRobotID);
		} catch (Exception e1) {
			System.err.println("Failed to move robot to follow mouse dragging...");
		}

		formerMousePoint = e.getPoint(); // update former point
	}

	private double getModelPointDiff(double diff) {
		return simPanel.vis.worldPointFromVisCoordinates(new Point2D.Double(diff,0)).getX() - 
		simPanel.vis.worldPointFromVisCoordinates(new Point(0,0)).getX();

	}

	private String clickedOnRobot(Point point) {
		// see if clicked on one of the robots
		for (RobotVisualizationData eachRobot : simPanel.vis.robotsVisualizationDataFromComponent) {
			if (eachRobot.containsSimVisualizationPoint(point, simPanel)) {
				return eachRobot.ID;
			}
		}

		// if still hasn't quit
		return null;
	}

	
	public boolean isMovingRobot() {
		return (isMovingRobotID != null);
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
