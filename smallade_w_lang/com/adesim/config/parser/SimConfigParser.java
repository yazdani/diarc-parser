package com.adesim.config.parser;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ade.gui.Util;

import com.adesim.ADESimEnvironmentComponentImpl;
import com.adesim.datastructures.ConfigStartupActorProperties;
import com.adesim.datastructures.Point3D;
import com.adesim.datastructures.PointCollection;
import com.adesim.datastructures.PushabilityDeterminer;
import com.adesim.datastructures.SimShape;
import com.adesim.datastructures.action.DoorPushOpenAction;
import com.adesim.datastructures.action.ExitAction;
import com.adesim.datastructures.action.MessageAction;
import com.adesim.datastructures.action.PushAction;
import com.adesim.datastructures.action.RemoveAction;
import com.adesim.datastructures.action.SimAction;
import com.adesim.objects.Block;
import com.adesim.objects.Box;
import com.adesim.objects.Door;
import com.adesim.objects.Landmark;
import com.adesim.objects.SimEntity;
import com.adesim.objects.Wall;
import com.adesim.objects.model.ObjectMover;
import com.adesim.robot.SimLocationSpecifier;
import utilities.ColorConverter;
import utilities.xml.ExpressionEvaluator;
import utilities.xml.Substitution;
import utilities.xml.Xml;
import java.io.FileNotFoundException;

/**
 * An <code>SimConfigParser</code> will parse the xml config files containing
 * the structure and bounds of the Sim world
 */
public class SimConfigParser{
	private ADESimEnvironmentComponentImpl simEnvironment;
	private Xml config;
	private ExpressionEvaluator evaluator;

	/**
	 * Create a new parser for the config file.
	 * @param filename the config file to be parsed
	 */
	public SimConfigParser(ADESimEnvironmentComponentImpl simEnvironment, String filename)
	{
		this.simEnvironment = simEnvironment;

                try {
		    config = new Xml(filename, "config");
                } catch (FileNotFoundException e) {
                    System.err.println(e);
                }
		evaluator = new ExpressionEvaluator();

		// set environment:
		processWorld();

		// take note of initial position of robot(s)
		processRobotInitPositions();

		// add objects:  order may matter in that (at least until objects are added and removed,
		//     the last-to-be-added objects should take precedence when dragging objects in the
		//     GUI, hence want to add rarely-movable objects first (i.e., walls), and
		//     add blocks after boxes, for instance).
		processWalls();
		processLandmarks();
		processDoorways();
		processBoxes();
		processBlocks();
	}


	private Point2D.Double getPointData(Xml shape, boolean defaultTo0ForMissingAttributes) {
		double x, y;

		Double zeroDefaultForMissing = (defaultTo0ForMissingAttributes) ? 0.0 : null;
		x = getDoubleOrDefaultOrError(shape, "x", zeroDefaultForMissing);
		y = getDoubleOrDefaultOrError(shape, "y", zeroDefaultForMissing);

		return new Point2D.Double(x, y);
	}

	private double getDoubleOrDefaultOrError(Xml xml, String attributeName, Double defaultValueOrNullForError) {
		if (xml.containsAttribute(attributeName)) {
			return xml.numDouble(attributeName, evaluator);
		} else if (defaultValueOrNullForError == null) {
			throw new Error("Could not find attribute " + Util.quotational(attributeName) + ".");
		} else {
			return defaultValueOrNullForError;
		}
	}

	private SimShape getPolygonData(Xml shapeXml, List<SimAction> actions, double z, Double zLength) {
		Xml polyData = shapeXml.child("poly");
		List<Xml> ptNodes = polyData.children("pt");
		PointCollection points = new PointCollection();

		for (Xml eachPointNode: ptNodes) {
			points.add(getPointData(eachPointNode, false));
		}

		PushabilityDeterminer pushabilityDeterminer = createPushabilityDeterminer(actions);
		return new SimShape(points, actions, z, zLength, pushabilityDeterminer);
	}


	private SimShape getRectangleData(Xml shapeXml, List<SimAction> actions, double z, Double zLength) {
		Xml rectData = shapeXml.child("rect");
		Point2D.Double bottomLeft = getPointData(rectData, true);
		double xLength = rectData.numDouble("xLength", evaluator);
		double yLength = rectData.numDouble("yLength", evaluator);
		// check if had the center=True argument, for, if so, need to shift
		//  bottomLeft to the bottom and to the left by half of xLength and yLength
		if (  (rectData.containsAttribute("center")) && (rectData.boolValue("center"))  ) {
			bottomLeft = new Point2D.Double(bottomLeft.x - xLength/2.0, bottomLeft.y - yLength/2.0);
		}

		PointCollection points = new PointCollection();
		points.add(bottomLeft.x, bottomLeft.y);
		points.add(bottomLeft.x + xLength, bottomLeft.y);
		points.add(bottomLeft.x + xLength, bottomLeft.y + yLength);
		points.add(bottomLeft.x, bottomLeft.y + yLength);


		PushabilityDeterminer pushabilityDeterminer = createPushabilityDeterminer(actions);
		return new SimShape(points, actions, z, zLength, pushabilityDeterminer);
	}


	/** note:  this pushability determiner is only for non-door shapes, e.g., just
    for the actual "push" command.  Door-pushing is handled separately by the
     Door object. */
	private PushabilityDeterminer createPushabilityDeterminer(List<SimAction> actions) {
		if (actionsIncludePushAction(actions)) {
			return PushabilityDeterminer.pushableIfObjectWillStillBeObstacleFreeAfterMove;
		} else {
			return PushabilityDeterminer.alwaysFalse;
		}
	}


	private boolean actionsIncludePushAction(List<SimAction> actions) {
		for (SimAction eachAction : actions) {
			if (eachAction instanceof PushAction) {
				return true;
			}
		}
		// if found nothing:
		return false;
	}



	private ArrayList<SimAction> getActions(Xml shapeXml) {
		// for backwards compatibility, search for either a separate actions list,
		//      or just for "action" tags.
		Xml actionsXml = shapeXml.childIfAny("actions");
		if (actionsXml == null) {
			// then just use the shape as the action root.
			actionsXml = shapeXml;
		}

		ArrayList<SimAction> actions = new ArrayList<SimAction> ();

		for (Xml actionXml : actionsXml.children("action")) {
			Set<String> actionContents = actionXml.childNames();
			// expect EXACTLY one action:
			if (actionContents.size() == 1) {
				// only one is there, but easiest to iterate:
				for (String type : actionContents)
				{
					if (type.equals("exit")) {
						actions.add (new ExitAction());
					} else if (type.equals("message")) {
						String text = actionXml.child(type).string("text");
						actions.add (new MessageAction(text));
					} else if (type.equals("remove")) {
						actions.add(new RemoveAction()); // will get attached to possessing sim entity.
					} else if (type.equals("push")) {
						actions.add(new PushAction()); // will get attached to possessing sim entity.
					} else if (type.equals("pushDoor")) {
						actions.add(new DoorPushOpenAction()); // will get attached to possessing sim entity.
					} else {
						System.err.println("Could not recognize action type: " + type);
					}
				}
			} else {
				System.err.println("Invalid number of actions within: " + shapeXml.name());
			}
		}

		return actions;
	}

	private SimShape getShape(Xml overallNode) {
		List<SimAction> actions = getActions(overallNode);

		Xml shapeXml = overallNode.child("shape");
		Set<String> contents = shapeXml.childNames();


		// expect EXACTLY one shape
		if (contents.size() != 1)
		{
			System.err.println("Invalid number of shapes within " + overallNode.name());
			return null;
		}


		SimShape particularShape = null;
		double z = (shapeXml.containsAttribute("z"))  ?  shapeXml.numDouble("z", evaluator)  :  0;
		Double zLength = (shapeXml.containsAttribute("zLength"))  ?  shapeXml.numDouble("zLength", evaluator)  :  null;


		// only one is there, but easiest to iterate:
		for (String type : contents)
		{
			if (type.equals("rect"))
				particularShape = getRectangleData(shapeXml, actions, z, zLength);
			else if (type.equals("poly"))
				particularShape = getPolygonData(shapeXml, actions, z, zLength);
			else
				return null;
		}

		if (shapeXml.containsAttribute("rotation")) {
			ObjectMover.rotateShapeOnly(particularShape, shapeXml.numDouble("rotation", evaluator, Substitution.RANDOM_ANGLE_SUBSTITUTION));
		}


		return particularShape;
	}

	private void processBoxes() {
		List<Xml> boxes = config.children("box");
		for (Xml eachBoxXml : boxes) {
			SimShape shape = getShape(eachBoxXml);
			Color color = getColorIfAny(eachBoxXml);
			String name = getNameIfAny(eachBoxXml);

			boolean initialOpen = false;
			if (eachBoxXml.containsAttribute("open")) {
				initialOpen = eachBoxXml.boolValue("open");
			}

			List<SimEntity> containsEntities = getContainedObjectsInAny(eachBoxXml);

			Box box = new Box(shape, name, color, initialOpen, containsEntities);
			simEnvironment.model.worldObjects.add(box);
		}
	}

	private void processBlocks() {
		simEnvironment.model.worldObjects.add(getBlocks(config));
	}

	private List<SimEntity> getBlocks(Xml rootElement) {
		List<Xml> blocks = rootElement.children("block");
		ArrayList<SimEntity> blockObjects = new ArrayList<SimEntity>();
		for (Xml eachBlockXml : blocks) {
			SimShape shape = getShape(eachBlockXml);
			Color color = getColorIfAny(eachBlockXml);
			String name = getNameIfAny(eachBlockXml);
			boolean laserable = getLaserableIfAny(eachBlockXml, true);

			Block block = new Block(shape, name, laserable, color);
			blockObjects.add(block);
		}
		return blockObjects;
	}


	private String getNameIfAny(Xml xmlElement) {
		if (xmlElement.containsAttribute("name")) {
			return xmlElement.string("name");
		} else {
			return null;
		}
	}

	private boolean getLaserableIfAny(Xml xmlElement, boolean defaultValue) {
		if (xmlElement.containsAttribute("laserable")) {
			return xmlElement.boolValue("laserable");
		} else {
			return defaultValue;
		}
	}

	private Color getColorIfAny(Xml eachXmlElement) {
		// if contains color specs within the xml attribute itself (left here for backwards compatibility)
		Color possibleColorSpecsWithinXMLattributes = getColorIfAnyFromXmlAttributes(eachXmlElement);
		if (possibleColorSpecsWithinXMLattributes != null) {
			return possibleColorSpecsWithinXMLattributes;
		}


		// otherwise, look for a color node:
		Xml colorChild = eachXmlElement.childIfAny("color");
		if (colorChild != null) {
			// if the color node contains the name attribute (rather than RGB), generate the color
			if (colorChild.containsAttribute("name")) {
				String colorName = colorChild.string("name").toLowerCase(); // color names are all in lower case for consistency's sake
				if (ColorConverter.colorTable.containsKey(colorName)) {
					return ColorConverter.colorTable.get(colorName);
				} else {
					throw new RuntimeException("Unknown color name \"" + colorName + "\".");
				}
			}

			// otherwise, see if the child DOES specify RGB, which is essentially the one last hope:
			Color possibleRGBcolor = getColorIfAnyFromXmlAttributes(colorChild);
			if (possibleRGBcolor != null) {
				return possibleRGBcolor;
			}
		}


		// if still here, exhausted all possibilities.  no color, sorry.
		return null;
	}


	private Color getColorIfAnyFromXmlAttributes(Xml eachXmlElement) {
		if ( (eachXmlElement.containsAttribute("red")) && (eachXmlElement.containsAttribute("green"))
				&& (eachXmlElement.containsAttribute("blue")) ) {
			return new Color(eachXmlElement.numInt("red"), eachXmlElement.numInt("green"),
					eachXmlElement.numInt("blue"));
		} else {
			return null;
		}
	}


	private void processWalls() {
		List<Xml> walls = config.children("wall");
		for (Xml each : walls) {
			SimShape shape = getShape(each);
			Color color = getColorIfAny(each);
			Wall wall;
                        if (color == null)
                            wall = new Wall(shape);
                        else
                            wall = new Wall(shape, color);
			simEnvironment.model.worldObjects.add(wall);
		}
	}

	private void processDoorways() {
		// parse:  <door [*optional* name=""] openFraction="[0 through 1.0]"
		//              	x="" y="" width="" depth="" closed_angle="" pivot_angle="">
		//             [possible actions]
		//         </door>
		List<Xml> doors = config.children("door");
		for (Xml aDoorXML : doors) {
			String name = (   (aDoorXML.containsAttribute("name")) ? aDoorXML.string("name") : null   );
			double openFraction = aDoorXML.numDouble("openFraction", evaluator);
			Point2D pivot = new Point2D.Double(aDoorXML.numDouble("x", evaluator), aDoorXML.numDouble("y", evaluator));
			double width = aDoorXML.numDouble("width", evaluator);
			double thickness = aDoorXML.numDouble("thickness", evaluator);
			double closedAngle = aDoorXML.numDouble("closed_angle", evaluator);
			double pivotAngle = aDoorXML.numDouble("pivot_angle", evaluator);
                        Color color = getColorIfAny(aDoorXML);

			Door door;
                        
                        if (color == null) {
                            door = new Door(name, openFraction, pivot, width, thickness, closedAngle, pivotAngle, getActions(aDoorXML));
                        } else {
                            door = new Door(name, openFraction, pivot, width, thickness, closedAngle, pivotAngle, getActions(aDoorXML), color);
                        }
			simEnvironment.model.worldObjects.add(door);
		}
	}

	private void processLandmarks() {
		List<Xml> landmarks = config.children("landmark");
		for (Xml each : landmarks) {
			boolean laserable = getLaserableIfAny(each, false); // landmarks are NOT laserable by default
			addToLandmarks(each, laserable);
		}
	}

	private void addToLandmarks(Xml each, boolean laserVisible) {
		String name = null;
		if (each.containsAttribute("name"))
			name = each.string("name");
		SimShape shape = getShape(each);
		Landmark landmark;
                Color color = getColorIfAny(each);
                if (color == null)
                    landmark = new Landmark(shape, name, laserVisible);
                else
                    landmark = new Landmark(shape, name, laserVisible, color);
		simEnvironment.model.worldObjects.add(landmark);
	}


	private void processRobotInitPositions() {
		Xml positionsNode = config.childIfAny("init-robot-positions");
		if (positionsNode == null) {
			return;
		}

		for (Xml eachPosition : positionsNode.children("position")) {
			double x = eachPosition.numDouble("x", evaluator);
			double y = eachPosition.numDouble("y", evaluator);
			double z = 0;
			if (eachPosition.containsAttribute("z")) {
				z = eachPosition.numDouble("z", evaluator);
			}

			double theta = eachPosition.numDouble("theta", evaluator, Substitution.RANDOM_ANGLE_SUBSTITUTION);

			SimLocationSpecifier location = new SimLocationSpecifier(
					new Point3D(x,y,z), theta);


			List<SimEntity> containsEntities = getContainedObjectsInAny(eachPosition);

			simEnvironment.startupActorLocationsAndProperties.add(new ConfigStartupActorProperties(
					location, containsEntities));



		}
	}

	private List<SimEntity> getContainedObjectsInAny(Xml xmlParent) {
		// NOTE:  for now only assumes that blocks can be contained, nothing else
		Xml containsXml = xmlParent.childIfAny("contains");
		if (containsXml == null) {
			return null;
		} else {
			return getBlocks(containsXml);
		}
	}


	private void processWorld() {
		Xml world = config.child("world");

		boolean bounded = true;
		//Bounded refers to whether the simulation should automatically put in walls at the world's edge, so that
		//the robot does not drive off into nothingness.  It is true by default.
		if (world.containsAttribute("bounded")) {
			bounded = world.boolValue("bounded");
		}

		SimShape worldShape = getShape(world);
		simEnvironment.model.createNewEnvironment(worldShape, bounded);
	}

}

// vi:ai:smarttab:expandtab:ts=8 sw=4
