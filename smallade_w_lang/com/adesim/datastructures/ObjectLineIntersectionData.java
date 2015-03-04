package com.adesim.datastructures;

import java.awt.geom.Line2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import com.adesim.objects.SimEntity;

/** Simple data structure for holding perceptual lines and, for each "hit", the object it hits */
public class ObjectLineIntersectionData {
	public List<Line2D> lines;
	public HashMap<String, SimShape> perceivedRobotShapes;
	public HashSet<SimEntity> perceivedObjectEntities;
	public HashSet<UUID> perceivedObjectIDs;
	public List<ColorPoint> pseudoXYpoints;
}
