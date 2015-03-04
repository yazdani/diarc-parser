package com.lrf.extractor;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class FeatureModelTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testCreation() {
        FeatureModel model = new FeatureModel().point().setMaxOffset(1.2);
        assertThat("Sanity check", model.point().getMaxOffset(), is(1.2));
    }

    @Test
    public void testFailureCondition() {
        // Trying to use a model parameter without first setting it will cause an exception
        FeatureModel model = new FeatureModel();

        thrown.expect(IllegalStateException.class);
        model.point().getMaxOffset();
    }
}
