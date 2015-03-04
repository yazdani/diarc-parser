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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SimPioneer extends SimGenericRobotBase {
    private Log log = LogFactory.getLog(getClass());
    private static final String IMAGE_RELATIVE_FILENAME = "images/pioneer.gif";
    //       relative to SimAbstractRobot class

    // dimensions according to a website, but seem pretty plausible
    public static final double X_LENGTH_IN_METERS = 0.50;
    public static final double Y_LENGTH_IN_METERS = 0.49;
    public static final double Z_LENGTH_IN_METERS = 0.26;

    private static final double HALF_X_LENGTH_IN_METERS = X_LENGTH_IN_METERS / 2.0;
    private static final double HALF_Y_LENGTH_IN_METERS = Y_LENGTH_IN_METERS / 2.0;

    // the Pioneer base will not change its "platonic shape", so may as well
    //     create it once and then cache it.
    private SimShape cachedPlatonicShape;


    public SimPioneer(SimModel model, String serverIDName, boolean noisyOdometry, AbstractLaser laser, CameraSpecs cameraSpecs) {
        super(model, serverIDName, noisyOdometry, IMAGE_RELATIVE_FILENAME, laser, cameraSpecs);
        log.info("SimPioneer created");
    }

    /**
     * gets shape in the "platonic" "ideal" form, centered at 0,0 and
     * oriented to the right (i.e. 0 orientation).
     */
    @Override
    public SimShape getPlatonicShape() {
        if (this.cachedPlatonicShape == null) {
            // the Pioneer is a rectangle;
            PointCollection points = PointCollection.createRectangleFromCornerPoints(
                    new Point2D.Double(-1 * HALF_X_LENGTH_IN_METERS, -1 * HALF_Y_LENGTH_IN_METERS),
                    new Point2D.Double(HALF_X_LENGTH_IN_METERS, HALF_Y_LENGTH_IN_METERS));
            // PushabilityDeterminer.alwaysFalse = robot is not pushable.
            this.cachedPlatonicShape = new SimShape(points, null, 0, Z_LENGTH_IN_METERS, PushabilityDeterminer.alwaysFalse);
        }

        return this.cachedPlatonicShape;
    }

    @Override
    public List<Point2D> getPlatonicObstacleSensors() {
        // don't cache this, as *conceivably* could be changing critical distance.

        boolean[] safeSpaces = laser.getSafeSpaces(); // right = 0, middle = 1, left = 2

        List<Point2D> points = new ArrayList<>();
        if (!safeSpaces[0])
            points.add(new Point2D.Double(0, -0.5 * (HALF_X_LENGTH_IN_METERS + CRITICALDIST))); // right

        if (!safeSpaces[1])
            points.add(new Point2D.Double(0.5 * (HALF_X_LENGTH_IN_METERS + CRITICALDIST), 0)); // middle

        if (!safeSpaces[2])
            points.add(new Point2D.Double(0, 0.5 * (HALF_X_LENGTH_IN_METERS + CRITICALDIST))); // left

        return points;
    }

}
