package com.adesim.robot;

import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import utilities.ColorConverter;

import com.ADEPercept;
import com.adesim.datastructures.CameraSpecs;
import com.adesim.datastructures.ColorPoint;
import com.adesim.datastructures.DoubleDimension;
import com.adesim.datastructures.ObjectLineIntersectionData;
import com.adesim.datastructures.PointCollection;
import com.adesim.datastructures.PushabilityDeterminer;
import com.adesim.datastructures.SimShape;
import com.adesim.objects.Door;
import com.adesim.objects.SimContainerEntity;
import com.adesim.objects.SimEntity;
import com.adesim.objects.Wall;
import com.adesim.objects.model.SimModel;
import com.adesim.util.SimUtil;

public class PerceptionHelper {
	
	private SimAbstractRobot robot;
	private CameraSpecs cameraSpecs;
	
	// cached center, heading, etc:
	private Point2D robotCenter;
	private double robotHeading;
	private SimShape robotPlatonicShape;
	private DoubleDimension robotDim;
	private SimModel model;	
	private AffineTransform robotTransform;
	
	
	public PerceptionHelper(SimAbstractRobot robot, CameraSpecs cameraSpecs) {
		this.robot = robot;
		this.cameraSpecs = cameraSpecs;

		// cache various multi-use robot info
		this.model = robot.getModel();
		this.robotCenter = robot.getLocation();
		this.robotHeading = robot.getLocationSpecifier().getTheta();
		this.robotPlatonicShape = robot.getPlatonicShape();
		this.robotDim = robotPlatonicShape.getBoundingDimension();
		this.robotTransform = robot.getLocationSpecifier().getTransformation();
	}


	public ArrayList<ADEPercept> getPercepts(int fieldOfVisionDegrees, String[] matchingCriteria) {
		ObjectLineIntersectionData intersectionData = 
				findAllPerceivedObjects(fieldOfVisionDegrees, matchingCriteria);

		
		ArrayList<ADEPercept> percepts = new ArrayList<ADEPercept>();
		
		
		// PERCEPTS FOR WORLD OBJECTS
		for (SimEntity object : intersectionData.perceivedObjectEntities) {
			ADEPercept percept = getBasicPerceptInfo(object.getShape(), object.getName(), object.getType());
			
			percept.id = object.getGUID();
			percept.time = object.getTime();


			if (object.getColorAssigned() != null) {
				percept.color = ColorConverter.getColorName(object.getColorAssigned());
				percept.colorObject = object.getColorAssigned();
			}

			Point2D pseudoXY = getPerceptPseudoXY(object);
			if (pseudoXY != null) {
				percept.sim_px = pseudoXY.getX();
				percept.sim_py = pseudoXY.getY();
			}

			if (object instanceof SimContainerEntity) {
				percept.open = ((SimContainerEntity) object).isOpen();
				percept.filled = (((SimContainerEntity) object).getObjectsHolder().size() > 0);
			} else if (object instanceof Door) {
				percept.open = ((Door) object).isFullyOpen();
			}

			if (object.getContainingObjectID() != null) {
				percept.inside = model.getObjectFromGUID(object.getContainingObjectID()).getName();
			}


			percepts.add(percept);
			//System.out.println(percept.toString());
		}

		
		// PERCEPTS FOR OTHER ROBOTS
		
		for (Entry<String, SimShape> eachRobot : intersectionData.perceivedRobotShapes.entrySet()) {
			ADEPercept percept = getBasicPerceptInfo(eachRobot.getValue(), eachRobot.getKey(), "robot");
			percepts.add(percept);
		}
		
		return percepts;
	}
	
	
	private ADEPercept getBasicPerceptInfo(SimShape shape, String name, String type) {
		Point2D objectCenter = shape.getCenter();
		double heading = SimUtil.heading(robotCenter, robotHeading, objectCenter);
		double distance = SimUtil.distance(robotCenter, objectCenter);
		DoubleDimension size = shape.getBoundingDimension();

		double x = objectCenter.getX();
		double y = objectCenter.getY();

		// basic Percept object:
		ADEPercept percept = new ADEPercept(name, type,
				heading, distance, x, y, x, y, size.width, size.height);
		// note:  initializing pseudo-x and pseudo-y to just x and y,
		//        will update later if relevant.

		return percept;
	}


	/** The idea of this method is to give the robot some point X,Y to aim towards so as to be
	 * able to ACCESS (i.e., pick up) an object.  Obviously, if the robot aimed at the center of
	 * the object, as it is returned by the Percept, it would collide.  Instead, try points
	 * on all four sides of the object, and see if any are free for the robot to stand in.
	 * @param object
	 * @return a good PseudoPoint to approach, or NULL if none is to be found.
	 */
	private Point2D getPerceptPseudoXY(SimEntity object) {
		// if object is contained within another object (i.e., box), skip it.  The robot will first
		//     need to approach the parent based on the parent's pseudo-x-y coordinates
		if (object.getContainingObjectID() != null) {
			return null;
		}

		ArrayList<Point2D> possiblePseudoPoints = getPerceptPseudoXYallPossiblePoints(object);

		// sort based on whichever one is closest:
		Collections.sort(possiblePseudoPoints, new Comparator<Point2D>() {
			@Override
			public int compare(Point2D pt1, Point2D pt2) {
				double pt1Dist = pt1.distance(robotCenter);
				double pt2Dist = pt2.distance(robotCenter);
				if (pt1Dist < pt2Dist) {
					return -1;
				} else if (pt1Dist == pt2Dist) {
					return 0;
				} else {
					return 1;
				}
			}
		});

		if (possiblePseudoPoints.size() >= 0) {
			return possiblePseudoPoints.get(0);
		} else {
			return null;
		}
	}


	private ArrayList<Point2D> getPerceptPseudoXYallPossiblePoints(SimEntity object) {
		ArrayList<Point2D> possiblePseudoPoints = new ArrayList<Point2D>();

		// generate points (they are all would-be center points for the robot):
		double safetyMargin = 0.05;  // just a few cm of safety
		double robotDistance = Math.sqrt(Math.pow(robotDim.width, 2) + Math.pow(robotDim.height, 2)) / 2.0
				+ safetyMargin;

		Point2D objectMinPt = object.getShape().getMin();
		Point2D objectMaxPt = object.getShape().getMax();
		double centerX = (objectMinPt.getX() + objectMaxPt.getX()) / 2.0;
		double centerY = (objectMinPt.getY() + objectMaxPt.getY()) / 2.0;

		// left:
		possiblePseudoPoints.add(new Point2D.Double(objectMinPt.getX() - robotDistance, centerY));
		// right
		possiblePseudoPoints.add(new Point2D.Double(objectMaxPt.getX() + robotDistance, centerY));
		// top
		possiblePseudoPoints.add(new Point2D.Double(centerX, objectMaxPt.getY() + robotDistance));
		// bottom
		possiblePseudoPoints.add(new Point2D.Double(centerX, objectMinPt.getY() - robotDistance));


		// for each of the points, see if putting a robot there would cause it to collide with stuff:
		Iterable<SimShape> simVisibleShapes = model.getLaserVisibleShapes(robot.getMinMaxZ());

		Iterator<Point2D> iterator = possiblePseudoPoints.iterator();
		while (iterator.hasNext()) {
			Point2D checkingPoint = iterator.next();
			AffineTransform transformer = new AffineTransform();
			transformer.translate(checkingPoint.getX(), checkingPoint.getY());
			SimShape hypotheticalRobotShape = robotPlatonicShape.createdTransformedShape(transformer);
			if (robot.locationIsObstacleFree(hypotheticalRobotShape, null)) {
				// of course, even if robot is obstacle-free in that location, it's possible that it's because
				//     the robot would end up *INSIDE* a large object (i.e., inside a box), with none of
				//     its sides touching the box.  That would hardly be a good point to go to!
				//     so, check if the line from the object to the possible point is intersected by anything
				Line2D centerToCenterLine = new Line2D.Double(object.getShape().getCenter(), checkingPoint);
				//   first find where it intersects the object:
				Point2D objectIntersection = object.getShape().getIntersectionPoint(centerToCenterLine);

				//   want the ObjectIntersection point to be just ever so slightly OUTSIDE the object:
				double dX = (checkingPoint.getX() - objectIntersection.getX()) * 0.95;
				double dY = (checkingPoint.getY() - objectIntersection.getY()) * 0.95;
				Point2D safeIntersectionPoint = new Point2D.Double(checkingPoint.getX() + dX,
						checkingPoint.getY() + dY);

				PointCollection pointCollection = new PointCollection();
				pointCollection.add(checkingPoint);
				pointCollection.add(safeIntersectionPoint);
				SimShape lineBetweenShape = new SimShape(pointCollection, null, 0, null,
						PushabilityDeterminer.alwaysFalse);

				for (SimShape eachLaserVisibleShape : simVisibleShapes) {
					if (eachLaserVisibleShape.intersectsShape(lineBetweenShape)) {
						iterator.remove(); // shape failed.
						break;
					}
				}
			} else { // if not obstacle free, then just remove object without a second thought
				iterator.remove();
			}
		}

		return possiblePseudoPoints;
	}


	public ObjectLineIntersectionData findAllPerceivedObjects(int fieldOfVisionDegrees, String... matchingCriteria) {
		// for now, unlike the laser readings, perceived objects essentially ignore the z parameter.
		//     this is because the laser just shoots out a straight line, whereas the perceived objects
		//     is actually a cone, much trickier computationally.  For most environments and objects,
		//     would probably be able to see a fair chunk of the environment anyway.  If want to do things
		//     more realistically, rely on the actual 3D camera view, and do real vision processing.

		HashSet<String> matchingCriteriaSet = SimUtil.toLowerCaseSet(matchingCriteria);

		List<Line2D> perceptualLinesToInfinity = new ArrayList<Line2D>();
		Point2D transformedCameraOrigin = robotTransform.transform(cameraSpecs.mountingPoint.point.point2D, null);


		// first generate perceptual lines to max perceptual distance
		int startingCameraDegree = 90 - fieldOfVisionDegrees / 2;
		for (int i = 0; i < fieldOfVisionDegrees; i++) {
			double angle = Math.toRadians(startingCameraDegree + i) + cameraSpecs.mountingPoint.degreeOffsetFromFront;
			Point2D platonicPointEnd = new Point2D.Double(
					cameraSpecs.maxPerceptualDistance * Math.sin(angle),
					cameraSpecs.maxPerceptualDistance * Math.sin(angle - (Math.PI/2.0)));
			Line2D transformedLine = new Line2D.Double(transformedCameraOrigin,
					robotTransform.transform(platonicPointEnd, null));
			perceptualLinesToInfinity.add(transformedLine);
		}


		// now trim them according to WALLS and DOORS.  Anything else would not block the view too much.

		List<Line2D> trimmedPerceptualLines = new ArrayList<Line2D>();
		HashSet<UUID> perceivedLookingForObjectIDs = new HashSet<UUID>();
		HashSet<SimEntity> perceivedLookingForObjectEntities = new HashSet<SimEntity>();
		HashMap<String, SimShape> perceivedRobotShapes = new HashMap<String, SimShape>();

		for (Line2D eachInfiniteLine : perceptualLinesToInfinity) {
			double maxUnobscuredDistance = cameraSpecs.maxPerceptualDistance; // init to full max distance
			Point2D obstructionPoint = null;

                  // first figure out the max distance that's not obscured by a wall or door,
                  //      updating the maxUnobscuredDistance variable as needed.

                  synchronized (model.worldObjects) {
                    for (SimEntity eachObject : model.worldObjects.getObjects()) {
                      if (eachObject instanceof Wall || eachObject instanceof Door) {
                        SimShape eachObjectShape = eachObject.getShape();
                        for (Line2D eachObjectLine : eachObjectShape.lines) {
                          Point2D inter = SimUtil.lineIntersection(eachInfiniteLine, eachObjectLine);
                          if (inter != null) {
                            double d = inter.distance(transformedCameraOrigin);
                            if (d < maxUnobscuredDistance) {
                              maxUnobscuredDistance = d;
                              obstructionPoint = inter;
                            }
                          }
                        }
                      }
                    }
                  }


			// now search for matching objects (within limits of the maxUnobscuredDistance):

			synchronized (model.worldObjects) {
                            int i = 0;
				for (SimEntity eachObject : model.worldObjects.getObjects()) {
					// have to check whether can see FIRST, before checking descriptions,
					//    because objects can be nested within each other, so don't want to exclude
					//    matching child if its parent is visible but does not match query.
					boolean canSeeIt = checkIfCanSeeObject(eachObject.getShape(),
							eachInfiniteLine, maxUnobscuredDistance, transformedCameraOrigin);

					if (canSeeIt) {
						if (eachObject.matchesDescription(matchingCriteriaSet)) {
							perceivedLookingForObjectIDs.add(eachObject.getGUID());
							perceivedLookingForObjectEntities.add(eachObject);
						}
						// regardless of whether or not matches description in its own right,
						//    check if has objects inside of it and if they match description:
						recursivelyCheckIfChildrenMatch(eachObject, matchingCriteriaSet,
								perceivedLookingForObjectIDs, perceivedLookingForObjectEntities);
					}
				}
			}



			// similarly, search for any non-blocked-by-a-wall robots:

			synchronized (model.otherRobotShapes) {
				for (Entry<String, SimShape> eachRobot : model.otherRobotShapes.entrySet()) {
					if (otherRobotMatchesDescription(eachRobot.getKey(), matchingCriteriaSet)) {
						boolean canSeeIt = checkIfCanSeeObject(eachRobot.getValue(),
								eachInfiniteLine, maxUnobscuredDistance, transformedCameraOrigin);
						if (canSeeIt) {
							perceivedRobotShapes.put(eachRobot.getKey(), eachRobot.getValue());
						}
					}
				}
			}

			if (obstructionPoint == null) {
				trimmedPerceptualLines.add(eachInfiniteLine);
			} else {
				trimmedPerceptualLines.add(new Line2D.Double(transformedCameraOrigin, obstructionPoint));
			}
		}



		List<ColorPoint> pseudoXYPoints = new ArrayList<ColorPoint>();
		for (SimEntity eachObject : perceivedLookingForObjectEntities) {
			Point2D eachPseudoPointIfAny = getPerceptPseudoXY(eachObject);
			if (eachPseudoPointIfAny != null) {
				pseudoXYPoints.add(new ColorPoint(eachObject.getColorAssignedOrDefault(),
						eachPseudoPointIfAny, "Pseudo-point for " + eachObject.getNameOrType(true)));
			}
		}


		ObjectLineIntersectionData intersectionData = new ObjectLineIntersectionData();
		intersectionData.lines = trimmedPerceptualLines;
		intersectionData.perceivedObjectIDs = perceivedLookingForObjectIDs;
		intersectionData.perceivedObjectEntities = perceivedLookingForObjectEntities;
		intersectionData.pseudoXYpoints = pseudoXYPoints;
		intersectionData.perceivedRobotShapes = perceivedRobotShapes;


		return intersectionData;
	}



	private boolean checkIfCanSeeObject(SimShape shape, Line2D eachInfiniteLine,
			double maxUnobscuredDistance, Point2D transformedCameraOrigin) {
		for (Line2D eachObjectLine: shape.lines) {
			Point2D inter = SimUtil.lineIntersection(eachInfiniteLine, eachObjectLine);
			if (inter != null) {
				double d = inter.distance(transformedCameraOrigin);
                if (d < maxUnobscuredDistance) {
                	return true;
                }
			}
		}
		// if haven't quit with true
		return false;
	}


	/** criteria must be all lower case!  can be "*", "robot", or robot's name */
	private boolean otherRobotMatchesDescription(String possibleRobotName, HashSet<String> criteria) {
		String[] possibleRobotDescriptions = {"*", "robot", possibleRobotName};
		for (String eachDescription : possibleRobotDescriptions) {
			if (criteria.contains(eachDescription.toLowerCase())) {
				return true;
			}
		}

		// if haven't quit yet:
		return false;
	}

	private void recursivelyCheckIfChildrenMatch(SimEntity parentObject,
			HashSet<String> matchingCriteriaSet,
			HashSet<UUID> perceivedLookingForObjectIDs,
			HashSet<SimEntity> perceivedLookingForObjectEntities) {

		if (parentObject instanceof SimContainerEntity) {
			SimContainerEntity container = (SimContainerEntity) parentObject;
			if (container.isOpen()) {
				for (SimEntity eachContainedObject : container.getObjectsHolder().getObjects()) {

					if (eachContainedObject.matchesDescription(matchingCriteriaSet)) {
						perceivedLookingForObjectIDs.add(eachContainedObject.getGUID());
						perceivedLookingForObjectEntities.add(eachContainedObject);
					}

					recursivelyCheckIfChildrenMatch(eachContainedObject, matchingCriteriaSet,
							perceivedLookingForObjectIDs, perceivedLookingForObjectEntities);
				}
			}
		}
	}



}
