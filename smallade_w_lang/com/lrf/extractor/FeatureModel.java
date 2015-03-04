package com.lrf.extractor;

import com.google.common.base.Objects;
import com.google.common.base.Optional;

import java.io.Serializable;

import static com.google.common.base.Preconditions.checkState;

/**
 * This class contains the model parameters for all feature extractors. It is a fluent API for ease of modification.
 *
 * @author Jeremiah Via <jeremiah.via@gmail.com>
 */
public class FeatureModel implements Serializable {
    private final DoorModel door;
    private final LineModel line;
    private final PointModel point;
    private final RightAngleModel rightAngleModel;
    private final ParallelModel parallelModel;
    private final IntersectionModel intersectionModel;

    /**
     * Create a new empty feature model. Note: you must set parameters in the model before you use them otherwise an
     * {@link IllegalStateException} will be thrown.
     */
    public FeatureModel() {
        door = new DoorModel(this);
        line = new LineModel(this);
        point = new PointModel(this);
        rightAngleModel = new RightAngleModel(this);
        parallelModel = new ParallelModel(this);
        intersectionModel = new IntersectionModel(this);
    }

    /**
     * Access the door model.
     *
     * @return the door model
     */
    public DoorModel door() {
        return door;
    }

    /**
     * Access the line model.
     *
     * @return the line model0
     */
    public LineModel line() {
        return line;
    }

    /**
     * Access the point model.
     *
     * @return the point model
     */
    public PointModel point() {
        return point;
    }

    /**
     * Access the parallel lines model.
     *
     * @return the parallel lines model
     */
    public ParallelModel parallel() {
        return parallelModel;
    }

    /**
     * Access the right angle model.
     *
     * @return the right angle model
     */
    public RightAngleModel rightAngle() {
        return rightAngleModel;
    }

    /**
     * Access the intersection model.
     *
     * @return the intersection model
     */
    public IntersectionModel intersection() {
        return intersectionModel;
    }

    public class DoorModel implements Serializable {
        private final FeatureModel model;
        private final Open open;
        private Optional<Double> maxWidth;
        private Optional<Double> minWidth;
        private Optional<Double> minLineLength;
        private Optional<Integer> minNumBeams;

        public DoorModel(FeatureModel model) {
            this.model = model;
            open = new Open(model);
            minWidth = Optional.absent();
            maxWidth = Optional.absent();
            minLineLength = Optional.absent();
            minNumBeams = Optional.absent();
        }

        /**
         * Get the maximum width of a potential doorway.
         *
         * @return the width in meters
         */
        public double getMaxWidth() {
            checkState(maxWidth.isPresent());
            return maxWidth.get();
        }

        /**
         * Set the maximum width of a potential doorway.
         *
         * @param doorWidthMax the door width in meters
         * @return the feature model
         */
        public FeatureModel setMaxWidth(double doorWidthMax) {
            this.maxWidth = Optional.of(doorWidthMax);
            return model;
        }

        /**
         * Get the minimum width of a potential doorway.
         *
         * @return the width in meters
         */
        public double getMinWidth() {
            checkState(minWidth.isPresent());
            return minWidth.get();
        }

        /**
         * Set the minimum width of a potential doorway.
         *
         * @param doorWidthMin the width in meters
         * @return the feature model
         */
        public FeatureModel setMinWidth(double doorWidthMin) {
            this.minWidth = Optional.of(doorWidthMin);
            return model;
        }

        /**
         * Get minimum line length in meters necessary for a line segment to be used in the doorway extraction
         * algorithm.
         *
         * @return the minimum line length in meters
         */
        public double getMinLineLength() {
            return minLineLength.get();
        }

        /**
         * Set minimum line length in meters necessary for a line segment to be used in the doorway extraction
         * algorithm.
         *
         * @return the feature model
         */
        public FeatureModel setMinLineLength(double minLineLengthDoor) {
            this.minLineLength = Optional.of(minLineLengthDoor);
            return model;
        }

        /**
         * Get the minimum number of laser beams required for a door.
         *
         * @return the minimum number of laser beams
         */
        public int getMinNumBeams() {
            return minNumBeams.get();
        }

        /**
         * Set the minimum number of laser beams required for a door.
         *
         * @return the feature model
         */
        public FeatureModel setMinNumBeams(int minNumBeams) {
            this.minNumBeams = Optional.of(minNumBeams);
            return model;
        }

        /**
         * Access the parameters specific to an open door.
         */
        public Open open() {
            return open;
        }

        public class Open implements Serializable{
            private final FeatureModel model;
            private Optional<Double> minDepth;

            public Open(FeatureModel model) {
                this.model = model;
                minDepth = Optional.absent();
            }

            /**
             * Get the minimum depth in meters required an a door to be considered open.
             *
             * @return the minimum depth in meters
             */
            public double getMinDepth() {
                return minDepth.get();
            }

            /**
             * Set the minimum depth in meters required an a door to be considered open.
             *
             * @return the minimum depth in meters
             */
            public FeatureModel setMinDepth(double minDepth) {
                this.minDepth = Optional.of(minDepth);
                return model;
            }
        }
    }

    public class LineModel implements Serializable {
        // parameter for determining whether two lines are parallel
        private final FeatureModel model;
        private Optional<Double> splitThreshold;
        private Optional<Double> offLineTolerance;
        private Optional<Double> confidenceThreshold;

        public LineModel(FeatureModel model) {
            this.model = model;
            splitThreshold = Optional.absent();
            offLineTolerance = Optional.absent();
            confidenceThreshold = Optional.absent();
        }

        /**
         * Get the threshold for the distance between a point and a line for the Ramer-Douglas-Peucker algorithm.
         *
         * @return the split threshold in meters
         */
        public double getSplitThreshold() {
            return splitThreshold.get();
        }

        /**
         * Set the threshold for the distance between a point and a line for the Ramer-Douglas-Peucker algorithm.
         *
         * @return the feature model
         */
        public FeatureModel setSplitThreshold(double splitThreshold) {
            this.splitThreshold = Optional.of(splitThreshold);
            return model;
        }

        /**
         * Get the amount of tolerance we are willing to have when considering lines as part of a doorway.
         *
         * @return the amount of tolerance in meters
         */
        public double getOffLineTolerance() {
            return offLineTolerance.get();
        }

        /**
         * Set the amount of tolerance we are willing to have when considering lines as part of a doorway.
         *
         * @param offLineTolerance the off line tolerance in meters
         * @return the feature model
         */
        public FeatureModel setOffLineTolerance(double offLineTolerance) {
            this.offLineTolerance = Optional.of(offLineTolerance);
            return model;
        }

        /**
         * Get the threshold confidence value for line.
         *
         * @return a number in the range [0.0, 1.0]
         */
        public double getConfidenceThreshold() {
            return confidenceThreshold.get();
        }

        /**
         * Set the threshold confidence value for a line.
         *
         * @param confidenceThreshold a number in the range [0.0, 1.0]
         * @return the feature model
         */
        public FeatureModel setConfidenceThreshold(double confidenceThreshold) {
            this.confidenceThreshold = Optional.of(confidenceThreshold);
            return model;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("splitThreshold", getSplitThreshold())
                    .add("offLineTolerance", getOffLineTolerance())
                    .add("confidenceThreshold", getConfidenceThreshold())
                    .toString();
        }

    }

    public class RightAngleModel implements Serializable {
        private final FeatureModel model;
        private Optional<Double> rightAngleLower;
        private Optional<Double> rightAngleUpper;

        public RightAngleModel(FeatureModel model) {
            this.model = model;
            rightAngleLower = Optional.absent();
            rightAngleUpper = Optional.absent();

        }

        /**
         * Get the upper bound for fuzzy right angle detection. This number is in radians.
         *
         * @return an angle in radians
         */
        public double getUpperBound() {
            return rightAngleUpper.get();
        }

        /**
         * Set the upper bound for fuzzy right angle detection. This number is in radians.
         *
         * @param rightAngleUpper an angle in radians
         * @return the feature model
         */
        public FeatureModel setUpperBound(double rightAngleUpper) {
            this.rightAngleUpper = Optional.of(rightAngleUpper);
            return model;
        }

        /**
         * Get the lower bound for fuzzy right angle detection. This number is in radians.
         *
         * @return an angle in radians
         */
        public double getLowerBound() {
            return rightAngleLower.get();
        }

        /**
         * Get the lower bound for fuzzy right angle detection. This number is in radians.
         *
         * @param rightAngleLower an angle in radians
         * @return the feature model
         */
        public FeatureModel setLowerBound(double rightAngleLower) {
            this.rightAngleLower = Optional.of(rightAngleLower);
            return model;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("right angle lower", getLowerBound())
                    .add("right angle upper", getUpperBound()).toString();
        }
    }

    public class ParallelModel implements Serializable {
        private final FeatureModel model;
        private Optional<Double> parallelThreshold;
        private Optional<Double> hallwayLength;
        private Optional<Double> minLineLength;

        public ParallelModel(FeatureModel model) {
            this.model = model;
            parallelThreshold = Optional.absent();
            hallwayLength = Optional.absent();
            minLineLength = Optional.absent();
        }

        /**
         * Get the required length for the combined parallel lines, in meters.
         *
         * @return the required length in meters
         */
        public double getHallwayLength() {
            return hallwayLength.get();
        }

        /**
         * Set the required length for the combined parallel lines, in meters.
         *
         * @param hallwayLength the required length in meters
         * @return the feature model
         */
        public FeatureModel setHallwayLength(double hallwayLength) {
            this.hallwayLength = Optional.of(hallwayLength);
            return model;
        }

        /**
         * Get the minimum line length to be considered for hallway detection.
         *
         * @return the minimum line length in meters
         */
        public double getMinLineLength() {
            return minLineLength.get();
        }

        /**
         * Set the minimum line length to be considered for hallway detection.
         *
         * @param minLineLength the minimum line length in meters
         * @return the feature model
         */
        public FeatureModel setMinLength(double minLineLength) {
            this.minLineLength = Optional.of(minLineLength);
            return model;
        }

        /**
         * Get the tolerance we are willing to give the angle between lines when considering them to be parallel, in
         * radians.
         *
         * @return an angle tolerance in radians
         */
        public double getThreshold() {
            return parallelThreshold.get();
        }

        /**
         * Set the tolerance we are willing to give the angle between lines when considering them to be parallel, in
         * radians.
         *
         * @param parallelThreshold an angle tolerance in radians
         * @return the feature model
         */
        public FeatureModel setThreshold(double parallelThreshold) {
            this.parallelThreshold = Optional.of(parallelThreshold);
            return model;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("parallelThreshold", getThreshold())
                    .add("hallwayLength", getHallwayLength())
                    .add("minLineLength", getMinLineLength()).toString();
        }
    }

    public class PointModel implements Serializable {
        private final FeatureModel model;
        private Optional<Double> maxOffset;

        public PointModel(FeatureModel model) {
            this.model = model;
            maxOffset = Optional.absent();
        }

        /**
         * Get the maximum amount by which two consecutive laser beams can differ before being a candidate for
         * smoothing.
         *
         * @return the max offset
         */
        public double getMaxOffset() {
            return maxOffset.get();
        }

        /**
         * Set the maximum amount by which two consecutive laser beams can differ before being a candidate for
         * smoothing.
         *
         * @param maxOffset the max offset as a "percentage" difference
         * @return the max offset
         */
        public FeatureModel setMaxOffset(double maxOffset) {
            this.maxOffset = Optional.of(maxOffset);
            return model;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("maxOffset", getMaxOffset()).toString();
        }
    }

    public class IntersectionModel implements Serializable {
        private final FeatureModel model;
        private Optional<Double> minWidth;
        private Optional<Double> maxWidth;
        private Optional<Double> minDepth;
        private Optional<Double> minLength;
        private Optional<Double> detectionThreshold;
        private Optional<Double> trackingThreshold;

        public IntersectionModel(FeatureModel model) {
            this.model = model;
            minWidth = Optional.absent();
            maxWidth = Optional.absent();
            minDepth = Optional.absent();
            minLength = Optional.absent();
            detectionThreshold = Optional.absent();
            trackingThreshold = Optional.absent();
        }

        /**
         * Get the minimum width an intersection must have.
         *
         * @return the minimum width in meters
         */
        public double getMinWidth() {
            return minWidth.get();
        }

        /**
         * Set the minimum width an intersection must have.
         *
         * @param intersectionWidthMin the minimum width in meters
         * @return the feature model
         */
        public FeatureModel setMinWidth(double intersectionWidthMin) {
            this.minWidth = Optional.of(intersectionWidthMin);
            return model;
        }

        /**
         * Get the maximum width an intersection must have.
         *
         * @return the maximum width in meters
         */
        public double getMaxWidth() {
            return maxWidth.get();
        }

        /**
         * Set the maximum width an intersection must have.
         *
         * @param intersectionWidthMax the maximum width in meters
         * @return the feature model
         */
        public FeatureModel setMaxWidth(double intersectionWidthMax) {
            this.maxWidth = Optional.of(intersectionWidthMax);
            return model;
        }

        /**
         * Get the minimum depth an intersection must have.
         *
         * @return the minimum depth in meters
         */
        public double getMinDepth() {
            return minDepth.get();
        }

        /**
         * Set the minimum depth an intersection must have.
         *
         * @param intersectionDepthMin the minimum depth in meters
         * @return the feature model
         */
        public FeatureModel setIntersectionDepthMin(double intersectionDepthMin) {
            this.minDepth = Optional.of(intersectionDepthMin);
            return model;
        }

        /**
         * Get the minimum length a line must have to be considered for an intersection.
         *
         * @return the minimum length in meters
         */
        public double getMinLength() {
            return minLength.get();
        }

        /**
         * Set the minimum length a line must have to be considered for an intersection.
         *
         * @param intersectionLengthMin the length in meters
         * @return the feature model
         */
        public FeatureModel setMinLength(double intersectionLengthMin) {
            this.minLength = Optional.of(intersectionLengthMin);
            return model;
        }

        /**
         * Get the confidence threshold for detection.
         *
         * @return a number in the range of [0.0, 1.0]
         */
        public double getDetectionThreshold() {
            return detectionThreshold.get();
        }

        /**
         * Set the confidence threshold for detection.
         *
         * @param intersectionDetectionThreshold a number in the range of [0.0, 1.0].
         * @return the feature model
         */
        public FeatureModel setDetectionThreshold(
                double intersectionDetectionThreshold) {
            this.detectionThreshold = Optional
                    .of(intersectionDetectionThreshold);
            return model;
        }

        /**
         * Get the confidence threshold for tracking.
         *
         * @return a number in the range of [0.0, 1.0]
         */
        public double getTrackingThreshold() {
            return trackingThreshold.get();
        }

        /**
         * Set the confidence threshold for tracking.
         *
         * @param trackingThreshold a number in the range of [0.0, 1.0].
         * @return the feature model
         */
        public FeatureModel setTrackingThreshold(double trackingThreshold) {
            this.trackingThreshold = Optional.of(trackingThreshold);
            return model;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).add("minWidth", getMinWidth())
                    .add("maxWidth", getMaxWidth())
                    .add("minDepth", getMinDepth())
                    .add("minLength", getMinLength())
                    .add("detectionThreshold", getDetectionThreshold())
                    .add("trackingThreshold", getTrackingThreshold())
                    .toString();
        }
    }
}
