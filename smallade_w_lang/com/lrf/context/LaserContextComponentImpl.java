package com.lrf.context;

import ade.Connection;
import ade.SuperADEComponentImpl;
import com.LaserScan;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.lrf.LRFUtil;
import com.lrf.extractor.FeatureModel;

import java.rmi.RemoteException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

/**
 * TODO: document
 *
 * @author Jeremiah Via <jeremiah.via@gmail.com>
 * @since 2013-10-30
 */
public class LaserContextComponentImpl extends SuperADEComponentImpl implements LaserContextComponent {
    private FeatureModel featureModel;
    private ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2));
    private Connection laser;
    private Connection featureExtractor;
    private Optional<String> arffFile;
    private boolean constructed;
    private KStarClassifier roomTypeClassifier;
    private Optional<ListenableFuture<LaserContext>> laserContext;
    private ListenableFuture<LaserScan> laserScan;
    private Callable<LaserContext> laserContextCallable;

    public LaserContextComponentImpl() throws RemoteException {
        super();

        featureModel = LRFUtil.makeFeatureModel();
        arffFile = Optional.of("com/lrf/config/stats.arff");
        roomTypeClassifier = new KStarClassifier(arffFile.get(), featureModel);

        laser = connectToComponent("com.interfaces.LaserComponent");
        featureExtractor = connectToComponent("com.lrf.extractor.LaserFeatureExtractorComponent");

        laserContextCallable = new Callable<LaserContext>() {
            @Override
            public LaserContext call() throws Exception {
                return roomTypeClassifier.classify(laserScan.get());
            }
        };


        constructed = true;
    }

    @Override
    public LaserContext getLaserContext() throws RemoteException {
        if (!laserContext.isPresent()) return new LaserContext();
        try {
            return laserContext.get().get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error acquiring context", e);
        }
        return new LaserContext();
    }

    @Override
    protected void init() {
        constructed = false;
        arffFile = Optional.absent();
    }

    @Override
    protected String additionalUsageInfo() {
        return "";
    }

    @Override
    protected void readyUpdate() {
        // Determine context
        final Optional<LaserScan> scan = Optional.fromNullable(laser.call("getLaserScan", LaserScan.class));
        if (!scan.isPresent()) {
            log.warn("No laser scan available");
            return;
        }
        laserScan = Futures.immediateFuture(scan.get());
        log.info("Received scan: " + scan.get().ranges.length + " readings");
        laserContext = Optional.fromNullable(service.submit(laserContextCallable));


        // Adjust relevant components based on context
        try {
            switch (getLaserContext().getRoomType()) {
                case HALL:
                    featureModel
                            .line().setSplitThreshold(0.05)
                            .door().setMinWidth(0.35)
                            .door().setMaxWidth(1.1)
                            .door().setMinLineLength(0.15)
                            .door().setMinNumBeams(6)
                            .door().open().setMinDepth(0.1);
                    break;
                case ROOM:
                    featureModel
                            .line().setSplitThreshold(0.12)
                            .door().setMinWidth(0.9)
                            .door().setMaxWidth(1.2)
                            .door().setMinLineLength(0.1)
                            .door().setMinNumBeams(8)
                            .door().open().setMinDepth(0.7);
                case UNKNOWN:
                    break;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        featureExtractor.call("setModel", void.class, featureModel);

    }

    @Override
    protected boolean localServicesReady() {
        return constructed && requiredConnectionsPresent();
    }
}
