package com.adesim.gui;

import java.awt.AWTException;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.swing.JPanel;

import com.adesim.datastructures.DoubleDimension;
import com.adesim.datastructures.ObjectsHolder;
import com.adesim.datastructures.SimShape;
import com.adesim.gui.datastructures.RobotVisualizationData;
import com.adesim.gui.vis3D.ADESim3DView;
import com.adesim.objects.SimContainerEntity;
import com.adesim.objects.SimEntity;
import com.adesim.util.SimUtil;


public class SimPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    
	private static final Color COLOR_HYPOTHETICAL_SHAPES = new Color(255, 48, 16, 150); // somewhat transparent
	
	private static final Stroke BASIC_MEDIUM_LINE_STROKE = new BasicStroke(5);

	// the publicly accessible vars:
    public ADESimMapVis vis;
    public RobotDraggingListener robotDraggingListener;
    public ObjectEditingListener objectEditingListener;
    public PaintUtils paintUtil = new PaintUtils();
    public Dimension visualizationDimension; // a unified scaling factor, set by setDrawingBoundsForProportionality()
    public BackgroundImageManager backgroundImageManager;
    
    public ArrayList<RobotVisualizationData> tempCachedRobotsVisData;
    
    // hashmaps below are LINKED so that order is the same as objects that were put into them, rather than chaotic.
    //    (that way, if draw box first and then its contained blocks, iteration will be guaranteed to be in order)
    public ArrayList<SimEntity> simEntityDrawnList = new ArrayList<SimEntity>();
    public LinkedHashMap<UUID, Shape> simEntityIDtoAWTshapeMap = new LinkedHashMap<UUID, Shape>();

    // list of hypothetical shapes that MIGHT add (walls that are being created, etc).
    //     for now just need to visualize them.
    // the key is object because, while it will probably be a wizard window, this could be a handy
    //     mechanism in general, and there's hardly a need to limit it.
    // accessed via addHypotheticalShape and removeHypotheticalShape
    private HashMap<Object, SimShape> hypotheticalShapes = new HashMap<Object, SimShape>();
    
    public HashMap<ADESim3DView, Shape> threeDViewShapes = new HashMap<ADESim3DView, Shape>();
    
    
    
    private Point mousePoint;
    private Map<Shape, String> mouseOverDescriptions = new HashMap<Shape, String>();
    
    
    public SimPanel(ADESimMapVis vis) {
        this.vis = vis;
        
        new MouseMotionTracker(this);
        
        this.robotDraggingListener = new RobotDraggingListener(this);
        this.objectEditingListener = new ObjectEditingListener(this);
        
        this.backgroundImageManager = new BackgroundImageManager(vis);
        
        new ActableObjectsPopupListener(this);
    }
    
    public Dimension getVisualizationDimension() {
		return visualizationDimension;
	}


	@Override
    /**The most important method:  the one that actually paints everything
     * onto the screen!
     */
    public void paint(Graphics g) {
		super.paint(g);
		
		tempCachedRobotsVisData = vis.robotsVisualizationDataFromComponent;
    	
    	if (   (vis.model.worldBounds == null) ||
    		   (tempCachedRobotsVisData == null)   ) {
    		return;
    	}
    	
    	
    	// otherwise:  repaint all:
    	setDrawingBoundsForProportionality();
    	setWorldToWhite(g);
    	mouseOverDescriptions.clear();
    	
    	
    	// first thing:  paint background, if relevant
    	backgroundImageManager.paintBackgroundIfRelevant(
    			g, visualizationDimension, true); // true = this is
    	//              happening before drawing anything else.
    	
    	
    	simEntityDrawnList.clear();
    	simEntityIDtoAWTshapeMap.clear();
    	
    
    	paintSimObjects(vis.model.worldObjects, g);
    	
    	

   		paintRobotShapes(g); // just the shape outlines -- more info in function description
        paintRobotsVisualizationData(g); // detailed robot data including lasers, perceptions, etc.
        
        
        paintHypotheticalShapes(g);
        
        paint3DviewLocations(g);
        
        // refresh tooltip display:
        displayMouseOverDescriptionIfAny();
        
        
        
        // again, paint background if relevant, and if set to paint it "on top"
        backgroundImageManager.paintBackgroundIfRelevant(
        		g, visualizationDimension, false); // false = 
    	//            this is not "before" painting; already have drawn everything else.
        
        
        vis.readyToRefreshAgain = true;
    }


	private void paintHypotheticalShapes(Graphics g) {
		synchronized (hypotheticalShapes) {
			for (SimShape eachShape : hypotheticalShapes.values()) {
				// make sure it's a valid (non-null) shape (if had 0 points, 
				//         SimShape.createShapeFromPoints(...) would return null):
				if (eachShape != null) { 
					g.setColor(COLOR_HYPOTHETICAL_SHAPES);
					Polygon poly = getPolygonFromSimShape(eachShape);
					paintPolygonFromSimShape(poly, g, null);
					// also paint all the points:  useful esp when starting to draw the shape, and have 1 or 2 points
					//     that don't look much like a shape!
					for (Point2D eachPt : eachShape.getAllPoints()) {
						paintCircleAroundWorldPoint(COLOR_HYPOTHETICAL_SHAPES, eachPt, 3, g);
					}
				}
			}			
		}
	}
	public void addHypotheticalShape(Object key, SimShape shape) {
		synchronized (hypotheticalShapes) {
			hypotheticalShapes.put(key, shape);
		}
	}
	public void removeHypotheticalShape(Object key) {
		synchronized (hypotheticalShapes) {
			hypotheticalShapes.remove(key);
		}
	}
	

	private void paintSimObjects(ObjectsHolder objects, Graphics g) {
		 paintSimObjects(objects, g, 1); // fully opaque
	}
    private void paintSimObjects(ObjectsHolder objects, Graphics g, double opacityFraction) {
    	if (objects == null) {
    		return;
    	}
    	
    	synchronized (objects) {
	    	for (SimEntity eachObject : objects.getObjects()) {
	    		Color colorToDraw = eachObject.getColorAssignedOrDefault();
	    		
	    		g.setColor(SimUtil.createAlphaColor(colorToDraw, opacityFraction));
				Polygon poly = getPolygonFromSimShape(eachObject.getShape());
				simEntityDrawnList.add(eachObject);
				simEntityIDtoAWTshapeMap.put(eachObject.getGUID(), poly);
				paintPolygonFromSimShape(poly, g, eachObject.getToolTipIfAny());
				eachObject.performAnyPostPainting(poly, g);
				
				if (eachObject instanceof SimContainerEntity) {
					paintSimObjects(((SimContainerEntity) eachObject).getObjectsHolder(), g, opacityFraction * 0.75);
				}
	    	}
		}
	}


	private void setWorldToWhite(Graphics g) {
    	g.setColor(Color.WHITE);
		g.fillRect(0,0, visualizationDimension.width, 
						visualizationDimension.height);
	}


	private void setDrawingBoundsForProportionality() {
		Dimension2D world = vis.model.worldBounds.getBoundingDimension();
        Dimension2D sim = this.getSize();
        double simToWorldX = sim.getWidth()/world.getWidth();
        double simToWorldY = sim.getHeight()/world.getHeight();
        double worldYtoXProprotion = world.getHeight()/world.getWidth();
        
        // if simToWorldX < simToWorldY, that means that X will be the constraint.
        //     so then we can let the width be the simWidth, but we'll have
        //     to make the height proportionally smaller
        if (simToWorldX < simToWorldY) {
        	visualizationDimension = new Dimension
        			((int)sim.getWidth(), (int)(sim.getWidth()*worldYtoXProprotion));
        } else {
        	visualizationDimension = new Dimension
					((int)(sim.getHeight()/worldYtoXProprotion), (int)sim.getHeight());
        }
    }
    
    
    public void paintPolygonFromSimShape(Polygon poly, 
    									 Graphics g, String tooltipDescription) {
    	g.drawPolygon(poly); // always DRAW the outline first:  that way it won't disappear if pixels are too small.
    	g.fillPolygon(poly); // now do the fill
    	addTooltipDescriptionIfRelevant(poly, tooltipDescription);
	}
    
    public void addTooltipDescriptionIfRelevant(Shape shape,
			String tooltipDescription) {
    	if (tooltipDescription != null) {
			mouseOverDescriptions.put(shape, tooltipDescription);
		}
	}

    /** get polygon in visualization, based on sim shape.  No transformation */
	public Polygon getPolygonFromSimShape(SimShape shape) {
		return getPolygonFromSimShape(shape, SimUtil.nullTransformation);
	}
	/** get polygon in visualization, based on sim shape, and applying custom transformation. */
	public Polygon getPolygonFromSimShape(SimShape shape,
			AffineTransform transformation) {
    	List<Point2D> allPoints = shape.getAllPoints();
    	Polygon poly = new Polygon();
        for (Point2D modelPoint : allPoints) {
        	Point2D transformedModelPoint = transformation.transform(modelPoint, null); 
            Point2D realPoint = visCoordinatesFromWorldPoint(transformedModelPoint);
            poly.addPoint((int)(realPoint.getX()),
            			  (int)(realPoint.getY()));
        }
        return poly;
	}

	public void paintLine(Line2D modelLine, Graphics g, String tooltipDescription) {
		Point2D visP1 = visCoordinatesFromWorldPoint(modelLine.getP1());
		Point2D visP2 = visCoordinatesFromWorldPoint(modelLine.getP2());
		
		addTooltipDescriptionIfRelevant(new Line2D.Double(visP1, visP2),
				tooltipDescription);
		
		g.drawLine((int)visP1.getX(), (int)visP1.getY(), 
				   (int)visP2.getX(), (int)visP2.getY());
    }

/** if visualization is a single-actor visualization, where visualization data will not be
 *  available for any of the other robots, show at least the other robot's shapes
 *  as shapes (more precisely, the CACHED shapes stored in the server's model, not
 *  necessarily an accurate reflection of where other robots really are, if this robot
 *  has not updated its other-robot perceptions in a while) */
	private void paintRobotShapes(Graphics g) {
    	if (vis.visType == ADESimMapVisualizationType.SINGLE_ROBOT) {	    		
			for (Entry<String, SimShape> eachEntry : vis.model.otherRobotShapes.entrySet()) {
				String eachName = eachEntry.getKey();
				
				// no point painting shape for itself:  the visualization data for it will look much better
				//    this "if" code works because we already know it's a single robot visualization, so it's
				//    bound to have a single robot visualization data
				boolean paintIt = true;
				
				// for some reason, then != null check is required to prevent occasional null-pointer-exception crashes
				if (  (vis.robotsVisualizationDataFromComponent.size() > 0)  &&
					  (vis.robotsVisualizationDataFromComponent.get(0) != null)   &&  
				      (vis.robotsVisualizationDataFromComponent.get(0).ID.equals(eachName))   ) {
					paintIt = false;
				}

				if (paintIt) {
					SimShape eachShape = eachEntry.getValue();
					g.setColor(PaintUtils.COLOR_ROBOT_SHAPES);
					Polygon poly = getPolygonFromSimShape(eachShape);
					paintPolygonFromSimShape(poly, g, "\"Cached\" " + eachName);
				}
				
			}
    	}
	}
	
	private void paintRobotsVisualizationData(Graphics g) {
    	for (RobotVisualizationData aRobot : tempCachedRobotsVisData) {
    		try {
    			aRobot.paint(this, g);	
			} catch (Exception e) {
				System.out.println("Error painting robot visualization, hopefully it's just a one-time glitch");
				//e.printStackTrace();
			}
    	}
    }


    
    public Point visCoordinatesFromWorldPoint(Point2D worldPoint) {
    	return SimUtil.visCoordinatesFromWorldPoint(worldPoint, vis.model.worldBounds, visualizationDimension);
    }
    
    public Dimension2D visDimensionFromWorldDimension(Dimension2D dimension) {
    	Dimension2D worldDim = vis.model.worldBounds.getBoundingDimension();
        
        double xDivTotalWorldX = dimension.getWidth() / worldDim.getWidth();
        double yDivTotalWorldY = dimension.getHeight() / worldDim.getHeight();
        
        return new DoubleDimension(xDivTotalWorldX*visualizationDimension.width, 
        							yDivTotalWorldY*visualizationDimension.height);
    }

	public void setCurrentMouseLocation(Point mousePoint) {
		this.mousePoint = mousePoint;
	}

	public void displayMouseOverDescriptionIfAny() {
		if (!vis.getMenuBarSim().viewMenuShowTooltips.isSelected()) {
			return;
		}
		if (mousePoint == null) {
			return;
		}
		
		Rectangle2D mouseSurroundRect = createMouseSurroundRectangle(mousePoint);
		
		List<String> texts = new ArrayList<String>();
		for (Shape each : mouseOverDescriptions.keySet()) {
			if (each.intersects(mouseSurroundRect)) {
				String shapeText = mouseOverDescriptions.get(each);
				texts.add(shapeText);
			}
		}
		
		if (texts.size() == 0) {
			// if still showing, though should not be:
			if (  (getToolTipText() != null)  &&
			      (!getToolTipText().equals(null))   ) {
				setToolTipText(null);
				updateTooltipDisplay(); // will make tooltip disappear.
			}
		} else {
			StringBuilder textStringBuilder = new StringBuilder();
			Collections.sort(texts);
			for (String each : texts) {
				textStringBuilder.append(each + "<br>");
			}
			String newText = "<html>" + textStringBuilder.toString().trim() + "</html>";
			if (!newText.equals(getToolTipText())) {
				setToolTipText(newText);
				updateTooltipDisplay();
			}
		}
	}

	public static Rectangle2D createMouseSurroundRectangle(Point pt) {
		int mouseSurroundRadius = 3;
		return new Rectangle2D.Double(
				pt.getX()-mouseSurroundRadius, 
				pt.getY()-mouseSurroundRadius,
				mouseSurroundRadius*2, mouseSurroundRadius*2);
	}

	private void updateTooltipDisplay() {
		// ok, this is pretty sad, but the only way I could find to update
		//    the tooltip (either update the text, or hide it) was to actually
		//    move the mouse.  The tooltip API isn't particularly nice.
		//    if someone finds a better way, by all means replace this!
		Robot jitterer;
		try {
			jitterer = new Robot();
			Point globalMousePoint = MouseInfo.getPointerInfo().getLocation();
			jitterer.mouseMove(globalMousePoint.x+1, globalMousePoint.y);
			jitterer.mouseMove(globalMousePoint.x, globalMousePoint.y);
		} catch (AWTException e) {
			// could not jitter mouse.  oh well.
		}
	}

	public void outlineObject(UUID eachObjectID, Graphics2D g) {
		Shape objectToOutline = simEntityIDtoAWTshapeMap.get(eachObjectID);
		
		if (objectToOutline == null) {
			// object must already have been picked up / removed.  So just don't worry about it!
			return;
		}
		
		outlineBounds(objectToOutline.getBounds(), g);
	}
	
	public void outlineBounds(Rectangle bounds, Graphics2D g) {
		final int SURROUND_MARGIN = 10;
		Shape surroundOval = new Ellipse2D.Double(bounds.x - SURROUND_MARGIN, bounds.y - SURROUND_MARGIN, 
				bounds.width + SURROUND_MARGIN * 2, bounds.height + SURROUND_MARGIN * 2);
		Stroke originalStroke = g.getStroke();
		g.setColor(RobotVisualizationData.PERCEPTUAL_OUTLINE_COLOR);
		g.setStroke(BASIC_MEDIUM_LINE_STROKE);
		g.draw(surroundOval);
		g.setStroke(originalStroke);
	}
	
	public Rectangle paintCircleAroundWorldPoint(Color color, Point2D point, int visRadius, Graphics g) {
		g.setColor(color);
		Point2D center = visCoordinatesFromWorldPoint(point);
		Rectangle rect = new Rectangle((int)(center.getX())-visRadius, 
				   (int)(center.getY())-visRadius,
				   visRadius * 2, visRadius * 2);
		g.fillOval(rect.x, rect.y, rect.width, rect.height);
		return rect;
	}

	public boolean clickedOnHypotheticalShape(Point point) {
		Rectangle mouseSurroundRect = getMouseSurroundRect(point);
		for (SimShape each : hypotheticalShapes.values()) {
			if (getPolygonFromSimShape(each).intersects(mouseSurroundRect)) {
				return true;
			}
		}
		// if no intersection:
		return false;
	}
	
	public static Rectangle getMouseSurroundRect(Point point) {
		final int mouseRadius = 3;
		Rectangle mouseSurroundRect = new Rectangle(point.x - mouseRadius, point.y - mouseRadius, 
				mouseRadius*2 + 1, mouseRadius*2 + 1);
		return mouseSurroundRect;
	}

	public boolean clickedOn3DviewShape(Point point) {
		Rectangle mouseSurroundRect = getMouseSurroundRect(point);
		synchronized (threeDViewShapes) {	
			for (Shape each : threeDViewShapes.values()) {
				if (each.intersects(mouseSurroundRect)) {
					return true;
				}
			}
		}
		// if no intersection:
		return false;
	}
	

	private void paint3DviewLocations(Graphics g) {
		synchronized (vis.threeDviews) {
			for (ADESim3DView each3Dview : vis.threeDviews) {
				Graphics2D g2d = ((Graphics2D) g);
				int circleRadius = (int)(SimUtil.getMaxDimension(this.getSize()) / 50.0);
				Point2D cameraLocationWorld = each3Dview.cameraLocation.point2D;
				Rectangle threeDdrawnShape = paintCircleAroundWorldPoint(
						PaintUtils.COLOR_3D_VIEW, cameraLocationWorld,
						circleRadius, g);
				Point visCameraPt = visCoordinatesFromWorldPoint(cameraLocationWorld);

				double angle = each3Dview.getCameraTheta();
				AffineTransform rotateTransform = AffineTransform.getRotateInstance(-1 * angle);
				Point2D lineEndPointPlatonicVis = rotateTransform.transform(new Point2D.Double(circleRadius * 3, 0), null);
				Point visCameraLineEnd = new Point(
						(int)(visCameraPt.x + lineEndPointPlatonicVis.getX()),
						(int)(visCameraPt.y + lineEndPointPlatonicVis.getY()));
				Shape arrowShape = SimUtil.createArrowShape(visCameraPt, visCameraLineEnd);
				g2d.fill(arrowShape);

				g.setColor(Color.ORANGE);
								
				g.setColor(Color.ORANGE);
				SimUtil.drawStringIntoRectangle("3D", threeDdrawnShape, g2d);
				synchronized (threeDViewShapes) {
					threeDViewShapes.put(each3Dview, threeDdrawnShape);
				}
			}
		}
	}
	
}
