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

import com.engine3D.my3DShapes.Object3D;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import javax.vecmath.Point2i;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * Hole finder is an algorithm for finding the center point of the holes in a
 * given Object3D.
 *
 * At the high level the algorithm works by defining a plane segment, cutting
 * the projection of the object from the plane segment, and then
 * bucket-fill-cutting around the perimeter. This leaves only the visible holes.
 * The midpoint of the set of points around the edge of the hole is then the
 * center of the hole.
 *
 * @author M@ <matthew.dunlap+hrilab@gmail.com>
 */
public class HoleFinder {
  static final int IMAGE_WIDTH = 400;
  static final int IMAGE_HEIGHT = 300;

  /**
   * Get the center point of the first hole found in the geometry.
   *
   * @param geometry
   * @return
   */
  public static HoleFinderResult getFirstHoleIn(Object3D geometry, Vector3d angle) {
    List<HoleFinderResult> centers = getHolesIn(geometry, angle);
    if (centers != null && centers.size() > 0) {
      return centers.get(0);
    }
    return null;
  }

  /**
   * Get a list of the center of each distinct hole found in the geometry.
   *
   * @param geometry
   * @return
   */
  public static List<HoleFinderResult> getHolesIn(Object3D geometry, Vector3d angle) {
    double objAngle = FrontFinder.getAngleFromXZ(geometry);

    Point3d min = new Point3d();
    Point3d max = new Point3d();
    getBoundingBox(geometry.getPointsInSpace(), min, max);
    double scale = Math.min(IMAGE_WIDTH / (max.x - min.x), IMAGE_HEIGHT / (max.z - min.z));
    double xOffset = -(min.x * scale);
    double zOffset = -(min.z * scale);

    BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
    geometry.flipNormals();
    geometry.fillingParallelProject(image, Color.white, scale, xOffset, zOffset);
    geometry.flipNormals();
    geometry.fillingParallelProject(image, Color.white, scale, xOffset, zOffset);

    fillPerimeter(image);
//		ImageIcon icon = new ImageIcon(image);
//		JOptionPane.showMessageDialog(null, icon);

    RealMatrix blackGroup = getBlackGroup(image);
    List<HoleFinderResult> results = new ArrayList<HoleFinderResult>();
    while (blackGroup != null) {
      DescriptiveStatistics xStats = new DescriptiveStatistics(blackGroup.getColumn(0));
      DescriptiveStatistics zStats = new DescriptiveStatistics(blackGroup.getColumn(1));
      DescriptiveStatistics yStats = new DescriptiveStatistics();
      Point3d[] points = geometry.getPointsCache(0);
      for (Point3d pt : points) {
        if (pt.z * scale + zOffset > zStats.getMean()) {
          yStats.addValue(pt.y);
        }
      }
      HoleFinderResult res = new HoleFinderResult();
      res.center = new Point3d((xStats.getMean() - xOffset) / scale,
                               yStats.getMean(),
                               (zStats.getMean() - zOffset) / scale);
      res.normal = new Vector3d(Math.sin(objAngle), Math.cos(objAngle), 0);
      res.normal.normalize();
      res.pixelCount = blackGroup.getRowDimension();
      results.add(res);

      // get the next hole
      blackGroup = getBlackGroup(image);
    }
    Collections.sort(results, new Comparator<HoleFinderResult>() {
      @Override
      public int compare(HoleFinderResult t, HoleFinderResult t1) {
        return Integer.valueOf(t1.pixelCount).compareTo(t.pixelCount);
      }
    });
    return results;
  }

  static void getBoundingBox(List<double[]> points, Point3d minOut, Point3d maxOut) {
    minOut.x = minOut.y = minOut.z = Double.POSITIVE_INFINITY;
    maxOut.x = maxOut.y = maxOut.z = Double.NEGATIVE_INFINITY;

    for (double[] pt : points) {
      if (pt[0] < minOut.x) {
        minOut.x = pt[0];
      }
      if (pt[1] < minOut.y) {
        minOut.y = pt[1];
      }
      if (pt[2] < minOut.z) {
        minOut.z = pt[2];
      }
      if (pt[0] > maxOut.x) {
        maxOut.x = pt[0];
      }
      if (pt[1] > maxOut.y) {
        maxOut.y = pt[1];
      }
      if (pt[2] > maxOut.z) {
        maxOut.z = pt[2];
      }
    }
  }

  static void fillPerimeter(BufferedImage image) {
    for (int i = 0; i < image.getWidth(); i++) {
      bucketFill(image, i, 0, Color.black.getRGB(), Color.white.getRGB());
      bucketFill(image, i, image.getHeight() - 1, Color.black.getRGB(), Color.white.getRGB());
    }
    for (int i = 0; i < image.getHeight(); i++) {
      bucketFill(image, 0, i, Color.black.getRGB(), Color.white.getRGB());
      bucketFill(image, image.getWidth() - 1, i, Color.black.getRGB(), Color.white.getRGB());
    }
  }

  static List<Point2i> bucketFill(BufferedImage image, int x, int y,
                                  int fromColor, int toColor) {
    if (fromColor == toColor) {
      return null;
    }
    List<Point2i> results = new ArrayList<Point2i>();
    LinkedList<Point2i> openSet = new LinkedList<Point2i>();

    openSet.offer(new Point2i(x, y));
    while (!openSet.isEmpty()) {
      Point2i p = openSet.pop();
      if (image.getRGB(p.x, p.y) == fromColor) {
        results.add(p);
        image.setRGB(p.x, p.y, toColor);
        if (p.x > 0) {
          openSet.offer(new Point2i(p.x - 1, p.y));
        }
        if (p.y > 0) {
          openSet.offer(new Point2i(p.x, p.y - 1));
        }
        if (p.x < image.getWidth() - 1) {
          openSet.offer(new Point2i(p.x + 1, p.y));
        }
        if (p.y < image.getHeight() - 1) {
          openSet.offer(new Point2i(p.x, p.y + 1));
        }
      }
    }
    return results;
  }

  static RealMatrix getBlackGroupStartingAt(int x, int y, BufferedImage image) {
    List<Point2i> points = bucketFill(image, x, y, Color.black.getRGB(),
                                      Color.orange.getRGB());
    RealMatrix results = new BlockRealMatrix(points.size(), 2);
    for (int i = 0; i < points.size(); i++) {
      double[] doubles = {points.get(i).x, points.get(i).y};
      results.setRow(i, doubles);
    }
    return results;
  }

  static RealMatrix getBlackGroup(BufferedImage image) {
    for (int x = 0; x < image.getWidth(); x++) {
      for (int y = 0; y < image.getHeight(); y++) {
        if (image.getRGB(x, y) == Color.black.getRGB()) {
          return getBlackGroupStartingAt(x, y, image);
        }
      }
    }
    return null;
  }
}
