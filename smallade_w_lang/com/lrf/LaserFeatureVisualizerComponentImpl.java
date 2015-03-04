package com.lrf;

import ade.Connection;
import ade.SuperADEComponent;
import ade.SuperADEComponentImpl;

import java.rmi.RemoteException;

/** @author Jeremiah Via <jeremiah.via@gmail.com> */
public class LaserFeatureVisualizerComponentImpl extends SuperADEComponentImpl implements LaserFeatureVisualizerComponent, SuperADEComponent {

    private Connection laser;
    private Connection featureExtractor;
    private Connection featureTracker;

    private boolean initialized;
    public LaserFeatureVisualizerComponentImpl() throws RemoteException {
        super();
        laser = connectToComponent("com.interfaces.LaserComponent");
        featureExtractor = connectToComponent("com.lrf.extractor.LaserFeatureExtractorComponent");
        featureExtractor.required = false;
        featureTracker = connectToComponent("com.lrf.tracker.LaserFeatureTrackerComponent");
        featureTracker.required = false;



        initialized = true;
    }

    @Override
    protected void init() {
        initialized = false;
    }

    @Override
    protected String additionalUsageInfo() {
        return "";
    }
}
