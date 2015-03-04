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
package utilities;

import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Quat4d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;

/**
 * A collection of static methods to help out with rotations.
 *
 * @author M@ <matthew.dunlap+hrilab@gmail.com>
 */
public class RotationHelpers {
  /**
   * Converts a Tuple containing a rotation about x, y, and z (to be
   * applied in that order) into an equivalent quaternion.
   *
   * @param xyz The three desired rotations
   * @return The same rotation as a quaternion.
   */
  public static Quat4d xyzRotationsToQuaternion(Tuple3d xyz) {
    Rotation rot = new Rotation(RotationOrder.XYZ, xyz.x, xyz.y, xyz.z);
    return new Quat4d(-rot.getQ1(), -rot.getQ2(), -rot.getQ3(), rot.getQ0());  // inverting the imaginary outputs, see Javadocs for Rotation for an explanation of why... or perhaps better, just take my word for it and leave these negatives
  }

  /**
   * Converts a Tuple containing a rotation about x, y, and z (to be
   * applied in that order) into a matrix that can rotate points by x,
   * then y, and then z.
   *
   * @param xyz The three desired rotations.
   * @return A matrix that achieves them.
   */
  public static Matrix3d xyzRotationsToRotationMatrix(Tuple3d xyz) {
    Matrix3d mat = new Matrix3d();
    mat.set(xyzRotationsToQuaternion(xyz));
    return mat;
  }

  /**
   * Converts a Tuple containing a rotation about x, y, and z (to be
   * applied in that order) into a matrix that can rotate points by x,
   * then y, and then z.
   *
   * The translate component of the matrix will always be zero.
   *
   * @param xyz The three desired rotations.
   * @return A matrix that achieves them.
   */
  public static Matrix4d xyzRotationsToTransformMatrix(Tuple3d xyz) {
    Matrix4d mat = new Matrix4d();
    mat.setIdentity();
    mat.setRotation(xyzRotationsToQuaternion(xyz));
    return mat;
  }

  /**
   * Converts a translation and a rotation into a transform matrix that
   * can be applied to points.
   *
   * @param translation How far the points should move.
   * @param rotation    How far the points should rotate.
   * @return
   */
  public static Matrix4d buildTransformMatrix(Vector3d translation, Tuple3d rotation) {
    return new Matrix4d(xyzRotationsToQuaternion(rotation), translation, 1);
  }
}
