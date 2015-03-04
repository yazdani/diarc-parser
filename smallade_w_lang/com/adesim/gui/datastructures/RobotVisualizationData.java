package com.adesim.gui.datastructures;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import com.adesim.datastructures.ColorPoint;
import com.adesim.datastructures.SimShape;
import com.adesim.gui.PopupObjectAction;
import com.adesim.gui.SimPanel;
import com.adesim.robot.SimAbstractRobot;
import com.adesim.robot.SimLocationSpecifier;
import com.adesim.util.SimUtil;

public class RobotVisualizationData implements Serializable{
	private static final long serialVersionUID = 1L;
	
	private static final int VISUALIZE_1_IN_X_LINES = 5;
	
	public static final Color PERCEPTUAL_OUTLINE_COLOR = Color.ORANGE;
	
	public SimShape platonicShape;
	public String imageRelativeFilename;
	public SimLocationSpecifier modelLocationSpecifier;
	public List<Line2D> laserLines;
	public List<Point2D> platonicActivatedObstacleSensors;
	public double[] laserDistances;
	
	public List<Line2D> perceptualLines;
	public HashSet<UUID> perceivedObjects;
	public List<ColorPoint> perceivedPseudoXYPoints;
	public HashMap<String, SimShape> perceivedRobotShapes;
	
	
	public ArrayList<PopupObjectAction> popupRobotActions;
	public String ID;
	
	private String tooltipDescription;

	
	
	public RobotVisualizationData(SimAbstractRobot robot) {
		this.ID = robot.getName();
		this.popupRobotActions = robot.getPopupRobotActions();
		this.tooltipDescription = robot.getTooltipDescription();
	}


	public void paint(SimPanel simPanel, Graphics g) {
		AffineTransform transformation = modelLocationSpecifier.getTransformation(); 
		
		paintRobotImageOrPolygon(transformation, simPanel, g);
		
		paintBumpers(transformation, simPanel, g);
		
		if (laserLines != null) {
			paintLaserLines(simPanel, g);
		}
		
		if (perceptualLines != null) {
			paintPerceptualLines(simPanel, g);
		}
		if (perceivedObjects != null) {
			for (UUID eachObjectID : perceivedObjects) {
				simPanel.outlineObject(eachObjectID, (Graphics2D) g);
			}
		}
		if (perceivedPseudoXYPoints != null) {
			Stroke formerStroke = ((Graphics2D)g).getStroke();
			// draw the points so that they're 10 pixels across
			((Graphics2D)g).setStroke(new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			
			for (ColorPoint eachPt : perceivedPseudoXYPoints) {
				g.setColor(SimUtil.createAlphaColor(eachPt.color, 0.75));
				simPanel.paintLine(new Line2D.Double(eachPt.pt, eachPt.pt), g, eachPt.tooltip);
			}
			
			// reset stroke
			((Graphics2D)g).setStroke(formerStroke);
		}
		
		if (perceivedRobotShapes != null) {
			for (SimShape each : perceivedRobotShapes.values()) {
				simPanel.outlineBounds(simPanel.getPolygonFromSimShape(each).getBounds(), (Graphics2D) g);
			}
		}
	}


	private void paintBumpers(AffineTransform transformation, SimPanel simPanel, Graphics g) {
		if (platonicActivatedObstacleSensors != null) {
			for (Point2D pt : platonicActivatedObstacleSensors) {
				simPanel.paintCircleAroundWorldPoint(Color.BLUE, transformation.transform(pt, null), 3, g);
			}
		}
	}


	private void paintLaserLines(SimPanel simPanel, Graphics g) {
		g.setColor(Color.GREEN);
		for (int i = 0; i < laserLines.size(); i = i + VISUALIZE_1_IN_X_LINES) {
			String tooltipText = "laser ray # " + i + SimUtil.DEGREE_SYMBOL + ".  distance = " + 
								 SimUtil.formatDecimal(laserDistances[i]);
			simPanel.paintLine(laserLines.get(i), g, tooltipText);
		}
	}
	
	private void paintPerceptualLines(SimPanel simPanel, Graphics g) {
		g.setColor(PERCEPTUAL_OUTLINE_COLOR);
		for (int i = 0; i < perceptualLines.size(); i = i + VISUALIZE_1_IN_X_LINES) {
			simPanel.paintLine(perceptualLines.get(i), g, null);
		}
	}


	private void paintRobotImageOrPolygon(AffineTransform transformation, SimPanel simPanel, Graphics g) {
		// create polygon for tooltip-ing, and for backup painting.
		Polygon robotPoly = simPanel.getPolygonFromSimShape(platonicShape, transformation);
		simPanel.addTooltipDescriptionIfRelevant(robotPoly, tooltipDescription);
		
		
		// if there is an image, paint an image in the bounding box of the robot
		//     otherwise, just paint a polygon:
		// start by trying to paint image.  Resort to polygon only if failed
		boolean imagePainted = false;
		if (imageRelativeFilename != null) { // no need to try painting image if no image specified.
			Point2D platonicMin = platonicShape.getMin();
			Point2D platonicMax = platonicShape.getMax();
			
			Point2D topLeftCornerModelPlatonic = new Point2D.Double(platonicMin.getX(), platonicMax.getY());
			Point2D topLeftCornerModelTransformed = transformation.transform(topLeftCornerModelPlatonic, null);
			Point2D topLeftCornerVis = simPanel.visCoordinatesFromWorldPoint(topLeftCornerModelTransformed);
			
			Graphics2D g2d = (Graphics2D)g;
			BufferedImage image = simPanel.paintUtil.getCachedImageOrLoadIfNecessary(imageRelativeFilename);
			if (image != null) {
				AffineTransform imageTransformation = new AffineTransform();
				imageTransformation.translate(topLeftCornerVis.getX(), topLeftCornerVis.getY());
				imageTransformation.rotate(-1*modelLocationSpecifier.getTheta()); // since screen coordinate system is different
				//   then model system, turning is the EXACT OPPOSITE, and hence need to multiply by -1
				Dimension2D visDimension = simPanel.visDimensionFromWorldDimension(
						platonicShape.getBoundingDimension());
				double scaleX = visDimension.getWidth() / image.getWidth();
			    double scaleY = visDimension.getHeight() / image.getHeight();
			    imageTransformation.scale(scaleX, scaleY);
				g2d.drawImage(image, imageTransformation, null);
				imagePainted = true;
			}
		}
		
		if (!imagePainted) {
			g.setColor(Color.ORANGE);
			simPanel.paintPolygonFromSimShape(robotPoly, g, tooltipDescription);
		}
	}


	public boolean containsSimVisualizationPoint(Point simPoint, SimPanel simPanel) {
		List<Point2D> allPoints = platonicShape.getAllPoints();
		Polygon transformedVisPolygon = new Polygon();
        for (Point2D modelPoint : allPoints) {
        	Point2D transformedModelPoint = modelLocationSpecifier
        					.getTransformation().transform(modelPoint, null);
        	Point2D realPoint = simPanel.visCoordinatesFromWorldPoint(transformedModelPoint);
            
        	transformedVisPolygon.addPoint((int)realPoint.getX(), (int)realPoint.getY());
        }
        
        return transformedVisPolygon.contains(simPoint);
	}
	
}
