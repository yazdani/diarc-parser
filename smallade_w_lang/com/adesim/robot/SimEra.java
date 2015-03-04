package com.adesim.robot;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import com.adesim.datastructures.CameraSpecs;
import com.adesim.datastructures.PointCollection;
import com.adesim.datastructures.PushabilityDeterminer;
import com.adesim.datastructures.SimShape;
import com.adesim.objects.model.SimModel;
import com.adesim.robot.laser.AbstractLaser;

public class SimEra extends SimGenericRobotBase {
	private static final String IMAGE_RELATIVE_FILENAME = "images/era.gif"; 
	//       relative to SimAbstractRobot class
	
	// dimensions according to a website, but seem pretty plausible
	public static final double X_LENGTH_IN_METERS = 0.4014;
	public static final double Y_LENGTH_IN_METERS = 0.3735;
	public static final double Z_LENGTH_IN_METERS = 0.2085;
	
	// the Era base is not completely rectangular, it's somewhere between a 
	//     rectangle and a stop sign.  The diagonal parts are at 45 degrees
	//     and take away 7 cm from each of the sides.
	private static final double DIAGONAL_SIDE_X_OR_Y_COMPONENT = 0.07;
	
	private static final double HALF_X_LENGTH_IN_METERS = X_LENGTH_IN_METERS / 2.0;
	private static final double HALF_Y_LENGTH_IN_METERS = Y_LENGTH_IN_METERS / 2.0;

	// the Era base will not change its "platonic shape", so may as well
	//     create it once and then cache it.
	private SimShape cachedPlatonicShape;
	
	public SimEra(SimModel model, String serverIDName, boolean noisyOdometry, 
				AbstractLaser laser, CameraSpecs cameraSpecs) {
		super(model, serverIDName, noisyOdometry, IMAGE_RELATIVE_FILENAME, laser, cameraSpecs);
	}
	
	/** gets shape in the "platonic" "ideal" form, centered at 0,0 and 
	 * oriented to the right (i.e. 0 orientation).
	 */
	@Override
	public SimShape getPlatonicShape() {
		if (this.cachedPlatonicShape == null) {
			// the Pioneer is a rectangle;
			PointCollection points = new PointCollection();
			
			// start at top left, and go around
			points.add(-1 * HALF_X_LENGTH_IN_METERS,
					HALF_Y_LENGTH_IN_METERS - DIAGONAL_SIDE_X_OR_Y_COMPONENT);
			points.add(-1 * HALF_X_LENGTH_IN_METERS + DIAGONAL_SIDE_X_OR_Y_COMPONENT,
					HALF_Y_LENGTH_IN_METERS);
			
			
			points.add(HALF_X_LENGTH_IN_METERS - DIAGONAL_SIDE_X_OR_Y_COMPONENT,
					HALF_Y_LENGTH_IN_METERS);
			points.add(HALF_X_LENGTH_IN_METERS,
					HALF_Y_LENGTH_IN_METERS - DIAGONAL_SIDE_X_OR_Y_COMPONENT);
			
	
			points.add(HALF_X_LENGTH_IN_METERS,
					-1 * HALF_Y_LENGTH_IN_METERS + DIAGONAL_SIDE_X_OR_Y_COMPONENT);
			points.add(HALF_X_LENGTH_IN_METERS - DIAGONAL_SIDE_X_OR_Y_COMPONENT,
					-1 * HALF_Y_LENGTH_IN_METERS);
			
			
			points.add(-1 * HALF_X_LENGTH_IN_METERS + DIAGONAL_SIDE_X_OR_Y_COMPONENT,
					-1 * HALF_Y_LENGTH_IN_METERS);
			points.add(-1 * HALF_X_LENGTH_IN_METERS,
					- 1 * HALF_Y_LENGTH_IN_METERS + DIAGONAL_SIDE_X_OR_Y_COMPONENT);
			
			
			this.cachedPlatonicShape = new SimShape(
					points, null, 0, Z_LENGTH_IN_METERS, PushabilityDeterminer.alwaysFalse); 
			// PushabilityDeterminer.alwaysFalse = robot is not pushable.
		}
		
		return this.cachedPlatonicShape;
	}
	
	@Override
	public List<Point2D> getPlatonicObstacleSensors() {
		// don't cache this, as *conceivably* could be changing critical distance.
		
		boolean[] safeSpaces = laser.getSafeSpaces(); // right = 0, middle = 1, left = 2
		
		List<Point2D> points = new ArrayList<Point2D>();
		if (!safeSpaces[0]) 
			points.add(new Point2D.Double(0, -0.5*(HALF_X_LENGTH_IN_METERS + CRITICALDIST))); // right
		
		if (!safeSpaces[1]) 
			points.add(new Point2D.Double(0.5*(HALF_X_LENGTH_IN_METERS + CRITICALDIST), 0)); // middle
		
		if (!safeSpaces[2]) 
			points.add(new Point2D.Double(0, 0.5*(HALF_X_LENGTH_IN_METERS + CRITICALDIST))); // left
		
		return points;
	}
}
