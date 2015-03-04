package com.lrf.extractor;

import com.LaserScan;
import com.google.common.base.Optional;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.*;
import com.lrf.feature.Door;
import com.lrf.feature.IntersectionBranch;
import com.lrf.feature.Line;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.geom.Point2D;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

/** @author Jeremiah Via <jeremiah.via@gmail.com> */
public class LaserFeatureExtractor {
    private Log log = LogFactory.getLog(LaserFeatureExtractor.class);

    private ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2));
    private Table<FeatureExtractor, Callable, Optional<ListenableFuture>> featureExtractor;
    private FeatureExtractor<Door> doorExtractor;
    private FeatureExtractor<Point2D> pointExtractor;
    private FeatureExtractor<Line> lineExtractor;
    private FeatureExtractor<Point2D> rightAngleExtractor;
    private FeatureExtractor<IntersectionBranch> intersectionExtractor;
    private ListenableFuture<LaserScan> laserScanFuture;
    private FeatureModel model;


    public LaserFeatureExtractor(FeatureModel model) {
        this.model = model;
        featureExtractor = HashBasedTable.create();

        // Add point extractor
        pointExtractor = new PointExtractor(model);
        featureExtractor.put(pointExtractor, makeFeatureExtractorCallable(pointExtractor), Optional.<ListenableFuture>absent());

        // Add line extractor
        lineExtractor = new LineExtractor(model);
        featureExtractor.put(lineExtractor, makeFeatureExtractorCallable(lineExtractor), Optional.<ListenableFuture>absent());

        // Add right angle extractor
        rightAngleExtractor = new RightAngleExtractor(model);
        featureExtractor.put(rightAngleExtractor, makeFeatureExtractorCallable(rightAngleExtractor), Optional.<ListenableFuture>absent());

        // Add door extractor
        doorExtractor = new DoorExtractor(model);
        featureExtractor.put(doorExtractor, makeFeatureExtractorCallable(doorExtractor), Optional.<ListenableFuture>absent());

        // Add intersection extractor
        intersectionExtractor = new IntersectionExtractor(model);
        featureExtractor.put(intersectionExtractor, makeFeatureExtractorCallable(intersectionExtractor), Optional.<ListenableFuture>absent());

    }

    public void updateLaserScan(LaserScan scan) {
        laserScanFuture = Futures.immediateFuture(scan);

        log.info("Received scan: " + scan.ranges.length + " readings");
        for (Table.Cell<FeatureExtractor, Callable, Optional<ListenableFuture>> cell : featureExtractor.cellSet()) {
            Callable callable = cell.getColumnKey();
            Optional<ListenableFuture> value = Optional.fromNullable(service.submit(callable));
            featureExtractor.put(cell.getRowKey(), cell.getColumnKey(), value);
            Futures.addCallback(value.get(), new FutureCallback<Object>() {
                @Override
                public void onSuccess(Object o) {
                    log.debug(String.format("Feature extraction successful (%s)", o.getClass()));
                }

                @Override
                public void onFailure(Throwable throwable) {
                    log.warn("A failure occurred during feature extraction.", throwable);
                }
            });
        }
    }

    public List<Point2D> getPoints() throws RemoteException {
        return getFeature(pointExtractor);
    }

    public List<Line> getLines() throws RemoteException {
        return getFeature(lineExtractor);
    }

    public List<Door> getDoors() throws RemoteException {
        return getFeature(doorExtractor);
    }

    public List<IntersectionBranch> getIntersections() throws RemoteException {
        return getFeature(intersectionExtractor);
    }

    public List<Point2D> getRightAngles() throws RemoteException {
        return getFeature(rightAngleExtractor);
    }

    private <E> List<E> getFeature(FeatureExtractor<E> fe) {
        for (Optional<ListenableFuture> v : featureExtractor.row(fe).values())
            try {
                if (!v.isPresent())
                    return new ArrayList<>();
                else
                    return (List<E>) v.get().get();
            } catch (InterruptedException | ExecutionException e) {
                log.error(e);
            }
        return new ArrayList<>();
    }

    private <E> Callable<List<E>> makeFeatureExtractorCallable(final FeatureExtractor<E> fe) {
        return new Callable<List<E>>() {
            @Override
            public List<E> call() throws Exception {
                return fe.extract(laserScanFuture.get());
            }
        };
    }

    public void setModel(FeatureModel model) {
        for (FeatureExtractor fe : featureExtractor.rowKeySet())
            fe.setModel(model);
    }
}
