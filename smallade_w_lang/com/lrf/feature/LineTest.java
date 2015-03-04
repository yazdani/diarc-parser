package com.lrf.feature;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.awt.geom.Point2D;

/**
 * @author Jeremiah M. Via <jeremiah.via@gmail.com>
 */
public class LineTest {

    Line line1 = new Line(new Point2D.Double(0.0, 0.0), new Point2D.Double(0.0, 1.0));
    Line line2 = new Line(new Point2D.Double(0.0, 0.0), new Point2D.Double(1.0, 0.0));

    Point2D point1 = new Point2D.Double(0.0, 0.0);
    Point2D point2 = new Point2D.Double(0.5, 0.0);
    Point2D point3 = new Point2D.Double(3.0, 4.0);

    @Test
    public void testGetLength() throws Exception {
        assertThat(line1.getLength(), is(1.0));
        assertThat(line2.getLength(), is(1.0));
    }

    @Test
    public void testAngleTo() throws Exception {


    }

    @Test
    public void testNormal() throws Exception {

    }

    @Test
    public void testPerpindicularDistanceTo() throws Exception {
        assertEquals(0.0, line1.perpindicularDistanceTo(point1), 0.0001);
        assertEquals(0.5, line1.perpindicularDistanceTo(point2), 0.0001);
        assertEquals(3.0, line1.perpindicularDistanceTo(point3), 0.0001);

        assertEquals(0.0, line2.perpindicularDistanceTo(point1), 0.0001);
        assertEquals(0.0, line2.perpindicularDistanceTo(point2), 0.0001);
        assertEquals(4.0, line2.perpindicularDistanceTo(point3), 0.0001);
    }
}
