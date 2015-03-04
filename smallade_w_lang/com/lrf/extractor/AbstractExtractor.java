package com.lrf.extractor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractExtractor<E> implements FeatureExtractor<E> {
    Log log = LogFactory.getLog(getClass());
    FeatureModel model;

    public AbstractExtractor(FeatureModel model) {
        this.model = model;
    }

    @Override
    public void setModel(FeatureModel model) {
        this.model = model;
    }
}
