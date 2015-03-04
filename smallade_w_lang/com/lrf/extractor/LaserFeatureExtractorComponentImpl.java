package com.lrf.extractor;

import ade.Connection;
import ade.SuperADEComponentImpl;
import com.LaserScan;
import com.google.common.base.Optional;
import com.lrf.LRFUtil;
import com.lrf.feature.Door;
import com.lrf.feature.IntersectionBranch;
import com.lrf.feature.Line;

import java.awt.geom.Point2D;
import java.rmi.RemoteException;
import java.util.List;

/**
 * This component takes data from the laser component and makes available features extracted from a laser scan.
 *
 * @author Jeremiah Via <jeremiah.via@gmail.com>
 */
public class LaserFeatureExtractorComponentImpl extends SuperADEComponentImpl implements LaserFeatureExtractorComponent {

    private LaserFeatureExtractor laserFeatureExtractor;
    private HallwayDetector hallwayDetector;
    private FeatureModel model;
    private Connection laser;
    private boolean ready;

    public LaserFeatureExtractorComponentImpl() throws RemoteException {
        super();
        laserFeatureExtractor = new LaserFeatureExtractor(model);
        hallwayDetector = new HallwayDetector(model);

        // Connect to components
        laser = connectToComponent("com.interfaces.LaserComponent");

        // Good to go!
        ready = true;
        log.info("Laser Feature Extractor Initialized");
    }

    /** {@inheritDoc} */
    @Override
    protected void init() {
        laser = null;
        model = LRFUtil.makeFeatureModel();
        ready = false;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean localServicesReady() {
        return ready && requiredConnectionsPresent();
    }

    /** {@inheritDoc} */
    @Override
    protected String additionalUsageInfo() {
        return "No additional info.";
    }

    /** {@inheritDoc} */
    @Override
    protected void readyUpdate() {
        final Optional<LaserScan> scan = Optional.fromNullable(laser.call("getLaserScan", LaserScan.class));
        if (!scan.isPresent()) {
            log.warn("No laser scan available");
            return;
        }
        laserFeatureExtractor.updateLaserScan(scan.get());
    }

    /** {@inheritDoc} */
    @Override
    public List<Point2D> getPoints() throws RemoteException {
        return laserFeatureExtractor.getPoints();
    }

    /** {@inheritDoc} */
    @Override
    public List<Line> getLines() throws RemoteException {
        return laserFeatureExtractor.getLines();
    }

    /** {@inheritDoc} */
    @Override
    public List<Door> getDoors() throws RemoteException {
        return laserFeatureExtractor.getDoors();
    }

    /** {@inheritDoc} */
    @Override
    public List<IntersectionBranch> getIntersections() throws RemoteException {
        return laserFeatureExtractor.getIntersections();
    }

    /** {@inheritDoc} */
    @Override
    public List<Point2D> getRightAngles() throws RemoteException {
        return laserFeatureExtractor.getRightAngles();
    }

    /** {@inheritDoc} */
    @Override
    public boolean inHallway() throws RemoteException {
        return hallwayDetector.detectHallway(getLines());
    }

    @Override
    public void setModel(FeatureModel model) throws RemoteException {
        log.debug("Changing feature model");
        laserFeatureExtractor.setModel(model);
    }
}
