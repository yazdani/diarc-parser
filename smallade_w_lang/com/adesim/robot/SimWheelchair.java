
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

public class SimWheelchair extends SimGenericRobotBase {
	private static final String IMAGE_RELATIVE_FILENAME = "images/era.gif"; 
	//       relative to SimAbstractRobot class
	
	// dimensions according to a website, but seem pretty plausible
	public static final double Y_LENGTH_IN_METERS = 0.66;
	public static final double X_LENGTH_IN_METERS = 1.04;
	public static final double Z_LENGTH_IN_METERS = 0.26;
	
	private static final double HALF_X_LENGTH_IN_METERS = X_LENGTH_IN_METERS / 2.0;
	private static final double HALF_Y_LENGTH_IN_METERS = Y_LENGTH_IN_METERS / 2.0;
	
	// the Wheelchair base will not change its "platonic shape", so may as well
	//     create it once and then cache it.
	private SimShape cachedPlatonicShape;
	
	
    public SimWheelchair(SimModel model, String serverIDName, boolean noisyOdometry, AbstractLaser laser, CameraSpecs cameraSpecs) {
	super(model, serverIDName, noisyOdometry, IMAGE_RELATIVE_FILENAME, laser, cameraSpecs);
    }
    
    
    /**
       To disallow left turns, uncomment this.
     */
    /*    @Override
        public void setTargetRV(double rv) {
	//ze wheelchair is not ambiturning.
	//	this.targetRV = ((rv > 0) ? 0 : rv);
	}*/


	/** gets shape in the "platonic" "ideal" form, centered at 0,0 and 
	 * oriented to the right (i.e. 0 orientation).
	 */
	@Override
	public SimShape getPlatonicShape() {
		if (this.cachedPlatonicShape == null) {
			// the Wheelchair is a rectangle;
			PointCollection points = PointCollection.createRectangleFromCornerPoints(
					new Point2D.Double(-1 * HALF_X_LENGTH_IN_METERS,
							           -1 * HALF_Y_LENGTH_IN_METERS),
					new Point2D.Double(HALF_X_LENGTH_IN_METERS,
									   HALF_Y_LENGTH_IN_METERS));
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
