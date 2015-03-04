package com.lrf.extractor;

import com.LaserScan;

import java.util.List;

public interface FeatureExtractor<E> {

    public List<E> extract(LaserScan scan);

    public void setModel(FeatureModel model);
}
