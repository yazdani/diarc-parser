package com.lrf.extractor;

import com.google.common.collect.Lists;
import com.lrf.feature.Line;
import org.junit.Test;

import java.awt.geom.Point2D;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class RightAngleExtractorTest {

    FeatureModel model = new FeatureModel()
            .rightAngle().setLowerBound(Math.toRadians(80))
            .rightAngle().setUpperBound(Math.toRadians(100));
    RightAngleExtractor angleExtractor = new RightAngleExtractor(model);

    @Test
    public void testExtractRightAngles() throws Exception {
        List<Line> lines;
        List<Point2D> points;

        // Trivial true case
        lines = Lists.newArrayList(
                new Line(new Point2D.Double(0.0, 0.0), new Point2D.Double(1.0, 0.0)),
                new Line(new Point2D.Double(1.0, 0.0), new Point2D.Double(1.0, 1.0))
        );
        points = angleExtractor.extractRightAngles(lines);
        assertThat(points.size(), is(1));
        assertThat(points.get(0).getX(), is(1.0));
        assertThat(points.get(0).getY(), is(0.0));

        // Trivial false case
        lines = Lists.newArrayList(
                new Line(new Point2D.Double(0.0, 0.0), new Point2D.Double(1.0, 0.0)),
                new Line(new Point2D.Double(0.0, 0.0), new Point2D.Double(1.0, 1.0))
        );
        points = angleExtractor.extractRightAngles(lines);
        assertThat(points.isEmpty(), is(true));
    }
}
