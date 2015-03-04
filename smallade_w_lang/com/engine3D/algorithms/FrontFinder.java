/**
 * Agent Development Environment (ADE)
 *
 * @version 1.0
 * @author M@
 *
 * Copyright 1997-2013 M@ Dunlap and HRILab (hrilab.org)
 * All rights reserved. Do not copy and use without permission.
 * For questions contact M@ at <matthew.dunlap+hrilab@gmail.com>
 */
package com.engine3D.algorithms;

import com.engine3D.my3DCore.TransformMy3D;
import com.engine3D.my3DShapes.Object3D;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math3.analysis.DifferentiableMultivariateVectorFunction;
import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.optimization.PointVectorValuePair;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * This is part of a naive algorithm for finding the hole in a handle in a
 * pointcloud.
 *
 * @author M@ <matthew.dunlap+hrilab@gmail.com>
 */
public class FrontFinder implements DifferentiableMultivariateVectorFunction {
  static final double h = .001d;  // the name h comes from the standard form of the fundamental theorem of calc
  Object3D geometry;
  TransformMy3D originalTransform;

  FrontFinder(Object3D obj) {
    geometry = obj;
    originalTransform = obj.transform;
  }

  /**
   * Gets the angle between the XZ plane and the "front" of the given geometry.
   *
   * @param obj
   * @return
   */
  public static double getAngleFromXZ(Object3D obj) {
    TransformMy3D originalTransform = obj.transform;

    FrontFinder worker = new FrontFinder(obj);
    double[] target = {0};
    double[] weights = {1};
    double[] startPoint = {0};
    LevenbergMarquardtOptimizer optimizer = new LevenbergMarquardtOptimizer();
    PointVectorValuePair results = null;

    try {
      results = optimizer.optimize(10000, worker, target, weights, startPoint);
    } catch (IllegalArgumentException ex) {
      Logger.getLogger(FrontFinder.class.getName()).log(Level.SEVERE, null, ex);
    } catch (Exception ex) {
      Logger.getLogger(FrontFinder.class.getName()).log(Level.SEVERE, null, ex);
    }

    obj.transform = originalTransform;

    if (results != null) {
      return results.getPointRef()[0];
    }
    return 0;
  }

  double getCost(double rz) {
    geometry.transform = new TransformMy3D(originalTransform);
    geometry.transform.combine(TransformMy3D.rotateZ(rz));
    List<double[]> verts = geometry.getPointsInSpace();

    // the idea here is to get all the points as close as possible to the XZ
    // plane that contains the vertex with the smallest value of Y
    // rather than make two passes over the data to get the minimum Y and then the
    // sum based on distances behind that Y I'm pulling the subtraction out
    DescriptiveStatistics stats = new DescriptiveStatistics(verts.size());
    for (double[] vert : verts) {
      stats.addValue(vert[1]);
    }
    return stats.getSum() - (stats.getN() * stats.getMin()) + Math.abs(rz);
  }

  @Override
  public MultivariateMatrixFunction jacobian() {
    return new MultivariateMatrixFunction() {
      @Override
      public double[][] value(double[] doubles) throws IllegalArgumentException {
        double x1 = getCost(doubles[0] + h);
        double x2 = getCost(doubles[0] - h);
        double[][] results = {{(x1 - x2) / (2 * h)}};
        return results;
      }
    };
  }

  @Override
  public double[] value(double[] doubles) throws IllegalArgumentException {
    double[] results = {getCost(doubles[0])};
    return results;
  }
}
