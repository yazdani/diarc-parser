package com.lrf.context;

import com.LaserScan;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;
import com.lrf.extractor.FeatureModel;
import com.lrf.extractor.LineExtractor;
import com.lrf.feature.Line;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import weka.classifiers.lazy.KStar;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;

public class KStarClassifier implements RoomTypeClassifier<Double> {
    private Log log = LogFactory.getLog(getClass());
    private LineExtractor lineExtractor;
    private KStar kStar;
    private Instances dataSet;
    private boolean trained;
    private SummaryStatistics stats;


    public KStarClassifier(String arffFile, FeatureModel featureModel) {
        trained = false;
        lineExtractor = new LineExtractor(featureModel);
        stats = new SummaryStatistics();
        // Load arff file
        ArffLoader loader = new ArffLoader();
        try {
            loader.setFile(new File(arffFile));
            Instances structure = loader.getStructure();
            structure.setClassIndex(structure.numAttributes() - 1);
            dataSet = loader.getDataSet();
            log.debug("ARFF file loaded");

            // Train K*

            kStar = new KStar();
//            kStar.buildClassifier(structure);
            dataSet.setClassIndex(dataSet.numAttributes() - 1);
            kStar.buildClassifier(dataSet);
//            Instance current;
//            while ((current = loader.getNextInstance(structure)) != null)
//                kStar.updateClassifier(current);
            log.debug("K* instance trained");

        } catch (Exception e) {
            log.error(format("Could not train K* using %s", arffFile), e);
        }
        trained = true;
        log.info(format("Finished training K* with the following options %s", Arrays.toString(kStar.getOptions())));
    }

//    public static void main(String[] args) {
//        System.out.println("starting");
//        KStarClassifier kStar = new KStarClassifier("com/lrf/data/stats.arff");
//        System.out.println("trained");
//
//        // ROOM
//        System.out.println(kStar.classify(Arrays.asList(42.0, 0.11359116692301771, 7.8416137459249855, 0.652163935866629, 0.360219795878262, 1.6237065810712485)));
//        System.out.println(kStar.classify(Arrays.asList(86.0, 0.06864279691182998, 5.609305856166893, 0.5266052607310927, 0.31478191398059596, 0.5730650264643898)));
//        System.out.println(kStar.classify(Arrays.asList(68.0, 0.14193661229644255, 5.57244152385471, 0.7305883761736862, 0.483620457073611, 0.8026885745734105)));
//
//        // HALL
//        System.out.println(kStar.classify(Arrays.asList(6.0, 0.8131888324594434, 9.825049247732943, 3.628433867304503, 2.659345984741525, 10.980201232471039)));
//        System.out.println(kStar.classify(Arrays.asList(8.0, 0.3304777796956805, 16.145597177085666, 4.200303048799741, 1.9832011413340631, 29.671453220064546)));
//        System.out.println(kStar.classify(Arrays.asList(10.0, 0.299629541580634, 77.01673605275514, 17.478432450851813, 3.9976665153631132, 958.6143958483614)));
//    }

    @Override
    public LaserContext.RoomType classify(List<Double> features) {
        if (!trained) {
            log.warn("K* model is untrained, returning unknown");
            return LaserContext.RoomType.UNKNOWN;
        }

        Instance instance = new Instance(1.0, Doubles.toArray(features));
        instance.setDataset(dataSet);
        try {
            int label = (int) kStar.classifyInstance(instance);
            switch (label) {
                case 0:
                    return LaserContext.RoomType.ROOM;
                case 1:
                    return LaserContext.RoomType.HALL;
                default:
                    return LaserContext.RoomType.UNKNOWN;
            }
        } catch (Exception e) {
            log.error(format("Could not classify %s", Joiner.on(", ").join(features)), e);
        }
        return LaserContext.RoomType.UNKNOWN;
    }

    @Override
    public LaserContext classify(LaserScan scan) {
        LaserContext laserContext = new LaserContext();
        laserContext.setRoomType(classify(getStatisticalFeatures(scan)));
        log.debug("Context: " + laserContext);
        return laserContext;
    }

    private List<Double> getStatisticalFeatures(LaserScan scan) {
        stats.clear();
        for (Line line : lineExtractor.extract(scan)) {
            stats.addValue(line.getLength());
        }
        return Lists.newArrayList(
                (double) stats.getN(),
                stats.getMin(),
                stats.getMax(),
                stats.getMean(),
                stats.getGeometricMean(),
                stats.getVariance());

    }
}
