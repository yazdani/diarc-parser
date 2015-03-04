package com.lrf;

import com.lrf.extractor.FeatureModel;

/**
 * TODO: document
 *
 * @author Jeremiah Via <jeremiah.via@gmail.com>
 * @since 2013-10-30
 */
public class LRFUtil {

    /**
     * This makes a default model containing the values that have previously been used.
     *
     * @return   a pre-configured feature model
     */
    public static FeatureModel makeFeatureModel() {
        return new FeatureModel()
                .point().setMaxOffset(1.2)
                .line().setOffLineTolerance(0.30)
                .line().setSplitThreshold(0.12)
                .line().setConfidenceThreshold(0.95)
                .door().setMinWidth(0.75)
                .door().setMaxWidth(1.1)
                .door().open().setMinDepth(0.7)
                .door().setMinLineLength(0.15)
                .door().setMinNumBeams(8)
                .rightAngle().setLowerBound(Math.toRadians(80))
                .rightAngle().setUpperBound(Math.toRadians(100))
                .parallel().setThreshold(Math.toRadians(0.17453))
                .parallel().setHallwayLength(17.0)
                .parallel().setMinLength(2.8)
                .intersection().setMinWidth(1.0)
                .intersection().setMaxWidth(3.0)
                .intersection().setIntersectionDepthMin(1.0)
                .intersection().setMinLength(0.3)
                .intersection().setDetectionThreshold(0.5)
                .intersection().setTrackingThreshold(0.5);
    }
}
