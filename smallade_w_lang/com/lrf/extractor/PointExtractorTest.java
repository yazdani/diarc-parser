package com.lrf.extractor;

import com.LaserScan;
import org.junit.Test;

import java.awt.geom.Point2D;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Jeremiah Via <jeremiah.via@gmail.com>
 */
public class PointExtractorTest {
    FeatureModel model = new FeatureModel().point().setMaxOffset(1.2);
    PointExtractor pointExtractor = new PointExtractor(model);

    @Test
    public void testLaserScanToPoints() throws Exception {
        LaserScan scan = new LaserScan();
        scan.ranges = new double[0];
        assertThat(pointExtractor.laserScanToPoints(scan).isEmpty(), is(true));

        scan.angleMin = Math.toRadians(0.0);
        scan.angleMax = Math.toRadians(180.0);
        scan.angleIncrement = Math.toRadians(90);
        scan.rangeMin = 0.0;
        scan.rangeMax = 3.0;
        scan.ranges = new double[]{1.0, 1.0, 1.0};

        List<Point2D> points = pointExtractor.laserScanToPoints(scan);
        assertThat(points.size(), is(3));

        assertEquals(1.0, points.get(0).getX(), 0.0001);
        assertEquals(0.0, points.get(0).getY(), 0.0001);

        assertEquals(0.0, points.get(1).getX(), 0.0001);
        assertEquals(1.0, points.get(1).getY(), 0.0001);

        assertEquals(-1.0, points.get(2).getX(), 0.0001);
        assertEquals(0.0, points.get(2).getY(), 0.0001);


    }
}
