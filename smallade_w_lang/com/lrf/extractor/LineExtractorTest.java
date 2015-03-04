package com.lrf.extractor;

import com.lrf.feature.Line;
import org.junit.Before;
import org.junit.Test;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Jeremiah Via <jeremiah.via@gmail.com>
 */
public class LineExtractorTest {

    LineExtractor lineExtractor;
    FeatureModel model = new FeatureModel().line().setSplitThreshold(0.12);

    @Before
    public void setup() {
        lineExtractor = new LineExtractor(model);
    }

    @Test
    public void testSplitLines() throws Exception {
        List<Point2D> ptList = new ArrayList<>();
        List<Line> extracted;

        // Test when 0 points
        assertThat(lineExtractor.split(new ArrayList<Point2D>()).isEmpty(), is(true));

        // Test simple 2 point case
        ptList.add(new Point2D.Double(0.0, 0.0));
        ptList.add(new Point2D.Double(1.0, 0.0));
        extracted = lineExtractor.split(ptList);
        assertThat(extracted.size(), is(1));

        // Test a single split
        ptList.clear();
        ptList.add(new Point2D.Double(0.0, 0.0));
        ptList.add(new Point2D.Double(0.5, 1.0));
        ptList.add(new Point2D.Double(1.0, 0.0));
        extracted = lineExtractor.split(ptList);
        assertThat(extracted.size(), is(2));

        // Should disregard middle point
        // Test a single split
        ptList.clear();
        ptList.add(new Point2D.Double(0.0, 0.0));
        ptList.add(new Point2D.Double(0.5, 0.001));
        ptList.add(new Point2D.Double(1.0, 0.0));
        extracted = lineExtractor.split(ptList);
        assertThat(extracted.size(), is(1));
    }
}
