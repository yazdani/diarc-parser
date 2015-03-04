//package com.lrf.context;
//
//import com.google.common.base.Stopwatch;
//import com.google.common.collect.Iterators;
//import com.google.common.collect.Lists;
//import com.google.common.collect.PeekingIterator;
//import com.lrf.context.KStarClassifier;
//import com.lrf.context.RoomTypeClassifier;
//import com.lrf.feature.ContextFeature;
//import com.lrf.feature.Door;
//import com.lrf.feature.Line;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
//import com.google.common.base.*;
//import java.io.*;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//
//import static java.lang.String.format;
//
////import weka.clusterers.SimpleKMeans;
//
///** @author Jeremiah Via <jeremiah.via@gmail.com> */
//public class ContextualLaserFeatureDetector extends LaserFeatureDetector {
//    private Log log = LogFactory.getLog(getClass());
//    private double hallwayThreshold = 0.05;
//    private double roomThreshold = 0.12;//0.15;//0.05;
//    private double unknownThreshold = 0.10;
//    private final int minimumCodebookSize = 3;
//    private SummaryStatistics stats;
//    private RoomTypeClassifier classifier;
//    private boolean chatty = true;
//    private Stopwatch stopwatch;
//    private Stopwatch timer;
//
//    private PrintWriter contextWriter;
//    private PrintWriter statsWriter;
//
//    public ContextualLaserFeatureDetector(int numLaserVals, double laserScanAngle, double toOffset) {
//        this(numLaserVals, laserScanAngle, toOffset, 4.0);
//    }
//
//    public ContextualLaserFeatureDetector(int numLaserVals, double laserScanAngle, double toOffset, double maxreading) {
//        super(numLaserVals, laserScanAngle, toOffset, maxreading);
//        stats = new SummaryStatistics();
//        classifier = new KStarClassifier("com/lrf/data/stats.arff");
//        SPLITTHRESH = unknownThreshold;
//        stopwatch = new Stopwatch().start();
//        timer = new Stopwatch().start();
//
//       // try {
//       //     statsWriter = new PrintWriter(new FileWriter("HALL.csv"), true);
//       // } catch (IOException e) {
//       //     log.error("Could not open files for writing.", e);
//       // }
//    }
//
//    public RoomTypeClassifier.RoomType getRoomType() {
//        return roomType;
//    }
//
//    private RoomTypeClassifier.RoomType roomType;
//
//    @Override
//    public List<Door> getDoors(double[] distances) {
//        List<Door> doors = super.getDoors(distances);
//        List<Double> statisticalFeatures = getStatisticalFeatures(getLines());
//
//        roomType = RoomTypeClassifier.RoomType.UNKNOWN;
//        //roomType = classifier.classify(statisticalFeatures);
//
//        switch (roomType) {
//            case HALL:
//                SPLITTHRESH = hallwayThreshold;
//
//                // parameters for the door model
//                DOOR_WIDTH_MAX = 1.1;           // in m; door must be less wide than this
//                DOOR_WIDTH_MIN = 0.35;           // in m; door must be wider than this
//                OPEN_DOOR_DEPTH_MIN = 0.1;           // in m; door must be deeper than this
//                MINLINELENGTH = 0.15;           // in m; the minimum lenght of a line to be considered for door detection
//                MINNUMBEAMS = 6;               // the min number of beams for the doorway
//                log.debug(format("[getDoors] Switching to hallway model: %f", SPLITTHRESH));
//                break;
//            case ROOM:
//                SPLITTHRESH = roomThreshold;
//                // parameters for the door model
//                DOOR_WIDTH_MAX = 1.2;           // in m; door must be less wide than this
//                DOOR_WIDTH_MIN = 0.9;           // in m; door must be wider than this
//                OPEN_DOOR_DEPTH_MIN = 0.7;           // in m; door must be deeper than this
//                MINLINELENGTH = 0.1;           // in m; the minimum lenght of a line to be considered for door detection
//                MINNUMBEAMS = 8;               // the min number of beams for the doorway
//                log.debug(format("[getDoors] Switching to room model: %f", SPLITTHRESH));
//                break;
//            case UNKNOWN:
//                SPLITTHRESH = unknownThreshold;
//                log.debug(format("[getDoors] Switching to unknown model: %f", SPLITTHRESH));
//                break;
//            default:
//
//        }
//
//
//        // log.debug(format("[getDoors] Writing %d statistical elements to disk.", statisticalFeatures.size()));
//        // statsWriter.println(Joiner.on(",").join(statisticalFeatures) + ",HALL");
//        sampleClassificationStream(roomType, doors.size());
//        return doors;
//    }
//
//    private void sampleClassificationStream(RoomTypeClassifier.RoomType roomType, int size) {
//        if (stopwatch.elapsed(TimeUnit.SECONDS) >= 1) {
//            if (chatty) System.out.printf("%5d    %6s    %3d\n", timer.elapsed(TimeUnit.SECONDS), roomType, size);
//            stopwatch.reset().start();
//        }
//    }
//
//    private List<Line> getLines() {
//        List<Line> list = new ArrayList<>();
//        for (int i = 0; i < totalnumlines; i++) {
//            list.add(lines[i]);
//        }
//        return list;
//    }
//
//    /**
//     * Calculate contextual features for use in room type classification. These fetures
//     *
//     * @param lines
//     * @return
//     */
//    private List<ContextFeature> getContextualFeatures(List<Line> lines) {
//        List<ContextFeature> features = new ArrayList<ContextFeature>();
//        PeekingIterator<Line> iter = Iterators.peekingIterator(lines.iterator());
//        while (iter.hasNext()) {
//            Line current = iter.next();
//            if (iter.hasNext()) {
//                Line next = iter.peek();
//                ContextFeature feature = new ContextFeature(current.getLength(), current.angleTo(next));
//                features.add(feature);
//            } else {
//                ContextFeature feature = new ContextFeature(current.getLength(), 0.0);
//                features.add(feature);
//            }
//        }
//
//        // Pad the feature vector with dummy data.
//        if (features.size() < minimumCodebookSize)
//            log.debug(format("[getContextualFeatures] Feature vector of %d elements is lower than minimum of %d. Padding.",
//                    features.size(), minimumCodebookSize));
//        while (features.size() < minimumCodebookSize) {
//            features.add(new ContextFeature(0.0, 0.0));
//        }
//
//        return features;
//    }
//
//    /**
//     * @param lines
//     * @return
//     */
//    private List<Double> getStatisticalFeatures(List<Line> lines) {
//        stats.clear();
//        for (Line line : lines) {
//            stats.addValue(line.getLength());
//        }
//
//        return Lists.asList((double) stats.getN(),
//                new Double[]{stats.getMin(), stats.getMax(), stats.getMean(), stats.getGeometricMean(), stats.getVariance()});
//
//    }
//}
