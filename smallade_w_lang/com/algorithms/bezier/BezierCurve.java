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
package com.algorithms.bezier;

import org.apache.commons.math3.util.ArithmeticUtils;

/**
 * A group of utility functions for calculating Bezier curves between sets of points.
 *
 * @author M@ <matthew.dunlap+hrilab@gmail.com>
 */
public class BezierCurve {
  public static double[] linearInterpolationBetween(double[] from, double percent, double[] to) {
    double[] results = new double[from.length];
    for (int i = 0; i < results.length; i++) {
      results[i] = percent * (to[i] - from[i]) + from[i];
    }
    return results;
  }

  /**
   * Get the point that is t of the way along the Bezier curve defined by the control points.
   * @param t The percentage of the way through the curve that you'd like to know, the domain of t is [0,1]
   * @param points The points that define the curve.
   * @return The point t way along the curve.
   */
  public static double[] getPoint(double t, double[][] points) {
    return getPoint(t, points, points.length);
  }

  public static double[] getPoint(double t, double[][] points, int order) {
    double[] result = new double[points[0].length];
    double step = (points.length-1) / (double)order;  // -1 here is to ensure that the endpoint is always a control point
    for (int dim = 0; dim < result.length; dim++) {
      double denom = 0;
      for (int i = 0; i*step < points.length; i++) {
        double b = bint(i, order, t);
        denom += b;
        result[dim] += b * points[(int) (i * step)][dim];
      }
      result[dim] /= denom;
    }
    return result;
  }

  /**
   * Get the point that is t of the way along the Bezier curve defined by the control points.
   * @param t The percentage of the way through the curve that you'd like to know, the domain of t is [0,1]
   * @param points The weighted points that define the curve.
   * @return The point t way along the curve.
   */
  public static double[] getPoint(double t, WeightedPoint[] points) {  // TODO: memoize this!
    if (t <= 0) {
      return points[0].point;
    } else if (t >= 1) {
      return points[points.length-1].point;
    }
    double[] result = new double[points[0].point.length];
    for (int dim = 0; dim < result.length; dim++) {
      double denom = 0;
      for (int i = 0; i < points.length; i++) {
        double b = bint(i, points.length, t) * points[i].weight;
        denom += b;
        result[dim] += b * points[i].point[dim];
      }
      result[dim] /= denom;
    }
    return result;
  }

  static double bint(int i, int n, double t) {  // the name bint comes from the choice of variable names in the bezier curve wikipedia article
    return ArithmeticUtils.binomialCoefficient(n, i) * Math.pow(t, i) * Math.pow(1 - t, n - i);
  }

  public static double[] getPointWithRepulsors(double t, WeightedPoint[] points) {
    if (t <= 0) {
      return points[0].point;
    } else if (t >= 1) {
      return points[points.length-1].point;
    }
    double[] firstPass = new double[points[0].point.length];
    for (int dim = 0; dim < firstPass.length; dim++) {
      double denom = 0;
      for (int i = 0; i < points.length; i++) {
        if (points[i].weight >= 0) {
          double b = bint(i, points.length, t) * points[i].weight;
          denom += b;
          firstPass[dim] += b * points[i].point[dim];
        }
      }
      firstPass[dim] /= denom;
    }

    double[] result = new double[points[0].point.length];
    for (int dim = 0; dim < firstPass.length; dim++) {
      double denom = 0;
      for (int i = 0; i < points.length; i++) {
        if (points[i].weight >= 0) {
          double b = bint(i, points.length, t) * points[i].weight;
          denom += b;
          result[dim] += b * points[i].point[dim];
        } else {
          double virtualPoint = result[dim] + (result[dim] - points[i].point[dim]);
          double b = bint(i, points.length, t);// * points[i].weight;
          denom += b;
          result[dim] += b * virtualPoint;
        }
      }
      result[dim] /= denom;
    }
    return result;
  }
}
