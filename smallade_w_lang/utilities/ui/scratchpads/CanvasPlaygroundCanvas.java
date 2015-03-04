/*
 * Agent Development Environment (ADE)
 *
 * @version 1.0
 * @author M@
 *
 * Copyright 1997-2013 M@ Dunlap and HRILab (hrilab.org)
 * All rights reserved. Do not copy and use without permission.
 * For questions contact M@ at <matthew.dunlap+hrilab@gmail.com>
 */
package utilities.ui.scratchpads;


import com.algorithms.bezier.BezierCurve;
import com.algorithms.bezier.WeightedPoint;

import java.awt.*;

/**
 * This is a simple 2D plotter that I use for RRT debugging, but you could use
 * for pretty much anything.
 *
 * @author M@ <matthew.dunlap+hrilab@gmail.com>
 */
public class CanvasPlaygroundCanvas extends Canvas {
  int radius = 5;
  double[][] points = {
      {20, 20},
      {50, 20},
      {80, 90},
      {200, 400},
      {400, 400},
  };

  WeightedPoint[] weightedPoints;

  public CanvasPlaygroundCanvas() {
    weightedPoints = new WeightedPoint[points.length];
    for (int i=0; i<points.length; i++) {
      weightedPoints[i] = new WeightedPoint();
      weightedPoints[i].point = points[i];
    }
    weightedPoints[2].weight = -30;
//    weightedPoints[3].weight = 80;
  }

  @Override
  public void paint(Graphics graphics) {
    double divs = 400;
    int lastX = (int) points[0][0];
    int lastY = (int) points[0][1];
    for (int i=1; i < divs; i++) {
      double[] pt = BezierCurve.getPointWithRepulsors(i / divs, weightedPoints);
      graphics.drawLine(lastX, lastY,(int)pt[0], (int)pt[1]);
      lastX = (int)pt[0];
      lastY = (int)pt[1];
    }
    graphics.drawLine(lastX, lastY,(int)points[points.length-1][0], (int)points[points.length-1][1]);
    graphics.setColor(Color.RED);
    for (double[]pt : points) {
      graphics.fillOval((int)pt[0]-radius, (int)pt[1]-radius, 2*radius, 2*radius);
    }
  }
}
