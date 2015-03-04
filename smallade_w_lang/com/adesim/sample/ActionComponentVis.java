package com.adesim.sample;

import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JLabel;

import ade.gui.ADEGuiCallHelper;
import ade.gui.ADEGuiPanel;

public class ActionComponentVis extends ADEGuiPanel{
	JLabel labelObstaclesSpaces, labelOrientation;
	
	public ActionComponentVis(ADEGuiCallHelper guiCallHelper) {
		super(guiCallHelper, 500); // 500 = update 2x/second
		initGui();
	}

	private void initGui() {
		this.setLayout(new GridLayout(2, 1));
	
		labelObstaclesSpaces = new JLabel();
		this.add(labelObstaclesSpaces);
		
		labelOrientation = new JLabel();
		this.add(labelOrientation);
	}

	private static final long serialVersionUID = 1L;

	
	@Override
	public Dimension getInitSize(boolean isInternalWindow) {
		return new Dimension(200,100);
	}

	@Override
	public void refreshGui() {
		try {
			ActionComponentVisData visData = (ActionComponentVisData) callComponent("getVisualizationData");
			labelObstaclesSpaces.setText("Obstacles = " + getObstaclesString(visData.obstacles));
			labelOrientation.setText("Orientation = ~" + (int)visData.orientation + " degrees");
		} catch (Exception e) {
			System.out.println("could not obtain vis data");
		}
	}

	
	private String getObstaclesString(boolean[] obstacles) {
		if (obstacles == null) {
			return null;
		}
		
		StringBuilder obstaclesString = new StringBuilder();
		for (int i = 2; i >= 0; i--) {
			obstaclesString.append(wallOrBlankString(obstacles[i]));
		}
		return obstaclesString.toString();
	}

	private String wallOrBlankString(boolean b)
	{
		if (b)
			return "W";
		else
			return "_";
	}
}
