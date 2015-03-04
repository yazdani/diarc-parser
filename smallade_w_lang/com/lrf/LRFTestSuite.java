package com.lrf;

import com.lrf.extractor.FeatureModelTest;
import com.lrf.extractor.LineExtractorTest;
import com.lrf.extractor.PointExtractorTest;
import com.lrf.extractor.RightAngleExtractorTest;
import com.lrf.feature.LineTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        // Features
        LineTest.class,
        // Extractors
        LineExtractorTest.class,
        PointExtractorTest.class,
        RightAngleExtractorTest.class,
        FeatureModelTest.class
})


public class LRFTestSuite {
    // class is empty, is a holder for annotations
}
