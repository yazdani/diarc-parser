package com.lrf.extractor;

import com.LaserScan;
import com.google.common.collect.Lists;
import com.lrf.feature.Line;

import java.util.List;

/**
 * This code was ripped out of the now deprecated LaserFeatureDetector.
 *
 * @author Jeremiah Via <jeremiah.via@gmail.com>
 */
public class HallwayDetector extends AbstractExtractor<Boolean> {
    private LineExtractor lineExtractor;
    private int[] numParallel;                 // to count the number of parallel lines
    private double[] lengthparallel;           // to count the length of parallel lines
    private int[] usedparallel;                // to keep track of which parallel lines have been found

    public HallwayDetector(FeatureModel model) {
        super(model);
        lineExtractor = new LineExtractor(model);
    }

    public boolean isHallwayDetected(LaserScan scan) {
        numParallel = new int[scan.ranges.length]; // to count the number of parallel lines
        lengthparallel = new double[scan.ranges.length]; // to count the length of parallel lines
        usedparallel = new int[scan.ranges.length]; // to keep track of which parallel lines have been found
        return detectHallway(lineExtractor.extract(scan));
    }

    /**
     * Finds parallel lines (heuristic -- a hallway has more line segments that are parallel to a given line segment
     * than a room).
     *
     * @param lines an array of lines detected
     */
    boolean detectHallway(List<Line> lines) {
        if (lines.isEmpty()) {
            return false;
        }
        // reset the parallel count
        for (int i = 0; i < lines.size(); i++) {
            usedparallel[i] = -1; // mark the lines as not parallel
        }
        for (int i = 0; i < lines.size(); i++) {
            if (usedparallel[i] < 0) {
                numParallel[i] = 0;
                lengthparallel[i] = lines.get(i).getLength();
                usedparallel[i] = i;
            }
            for (int j = i + 1; j < lines.size(); j++) {
                if ((Math.abs(lines.get(j).angleTo(lines.get(i))) < model.parallel().getThreshold())
                        || (Math.abs(lines.get(j).angleTo(lines.get(i))) > Math.PI - model.parallel().getThreshold())) {
                    numParallel[usedparallel[i]]++;
                    lengthparallel[usedparallel[i]] += (lines.get(j).getLength() < model.parallel().getMinLineLength() ? 0 : lines.get(j).getLength());
                    usedparallel[j] = usedparallel[i];
                }
            }
            if (numParallel[usedparallel[i]] > 0 && lengthparallel[usedparallel[i]] > model.parallel().getHallwayLength()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Boolean> extract(LaserScan scan) {
        return Lists.newArrayList(detectHallway(lineExtractor.extract(scan)));
    }
}
