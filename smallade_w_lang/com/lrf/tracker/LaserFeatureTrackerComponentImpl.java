package com.lrf.tracker;

import ade.Connection;
import ade.SuperADEComponentImpl;
import com.lrf.feature.Door;
import com.lrf.feature.IntersectionBranch;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/** @author Jeremiah Via <jeremiah.via@gmail.com> */
public class LaserFeatureTrackerComponentImpl extends SuperADEComponentImpl implements LaserFeatureTrackerComponent {

    private boolean initialized;
    private Connection featureExtractor;
    private LaserFeatureTracker featureTracker;

    public LaserFeatureTrackerComponentImpl() throws RemoteException {
        super();
        featureTracker = new LaserFeatureTracker();
        featureExtractor = connectToComponent("com.lrf.extractor.LaserFeatureExtractorComponent");
        initialized = true;
    }

    @Override
    public List<Door> getTrackedDoors() throws RemoteException {
        List<Door> doors = featureTracker.doors();
        if (doors == null)
            return new ArrayList<>();
        return doors;
    }

    @Override
    public List<IntersectionBranch> getTrackedIntersections() throws RemoteException {
        List<IntersectionBranch> intersections = featureTracker.intersectionbranches();
        if (intersections == null)
            return new ArrayList<>();
        return intersections;
    }

    @Override
    protected void init() {
        initialized = false;
    }

    @Override
    protected String additionalUsageInfo() {
        return "";
    }

    @Override
    protected boolean localServicesReady() {
        return initialized && super.localServicesReady();
    }

    @Override
    protected void readyUpdate() {
        List<Door> doors = featureExtractor.call("getDoors", List.class);
        List<IntersectionBranch> intersections = featureExtractor.call("getIntersections", List.class);

        featureTracker.track(doors, intersections);
    }
}
