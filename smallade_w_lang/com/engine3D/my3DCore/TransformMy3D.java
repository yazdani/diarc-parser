package com.engine3D.my3DCore;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;

public class TransformMy3D implements Serializable {
	private static final long serialVersionUID = 92347860001L;
	private Matrix4d matrix;
	private Point3d temp = new Point3d();

	public TransformMy3D() {
		matrix = new Matrix4d();
		matrix.setIdentity();
	}

	public TransformMy3D(Matrix3d rotationMatrix) {
		matrix = new Matrix4d();
		matrix.setIdentity();
		matrix.setRotation(rotationMatrix);
	}

	public TransformMy3D(Matrix4d transformMatrix) {
		matrix = new Matrix4d(transformMatrix);
	}

	public TransformMy3D(TransformMy3D orig) {
		matrix = new Matrix4d(orig.matrix);
	}

	public boolean sameAs(TransformMy3D other) {
		if (other == null) {
			return false;
		}
		return matrix.equals(other.matrix);
	}

	public void combine(TransformMy3D transform) {
		matrix.mul(transform.matrix);
	}

	public TransformMy3D combineNew(TransformMy3D transform) {
		TransformMy3D result = new TransformMy3D();
		result.matrix.mul(matrix, transform.matrix);
		return result;
	}

	public Matrix4d getMatrix() {
		return matrix;
	}

	public void show() {
		System.out.println("Matrix.");
		System.out.println(matrix.toString());
	}

	public static TransformMy3D translate(double x, double y, double z) {
		TransformMy3D transform = new TransformMy3D();
		transform.matrix.m03 = x;
		transform.matrix.m13 = y;
		transform.matrix.m23 = z;
		return transform;
	}

	public static TransformMy3D translate(Tuple3d xyz) {
		return translate(xyz.x, xyz.y, xyz.z);
	}

	public static TransformMy3D stretch(double x, double y, double z) {
		TransformMy3D transform = new TransformMy3D();
		transform.matrix.m00 = x;
		transform.matrix.m11 = y;
		transform.matrix.m22 = z;
    transform.matrix.m33 = 1;
		return transform;
	}

	public static TransformMy3D scale(Vector3d xyz) {
		return stretch(xyz.x, xyz.y, xyz.z);
	}

	// TODO: delete this method after all is refactored
	public static TransformMy3D rotateX(double theta) {
		TransformMy3D transform = new TransformMy3D();
		transform.matrix.rotX(theta);
		return transform;
	}

	// TODO: delete this method after all is refactored
	public static TransformMy3D rotateY(double theta) {
		TransformMy3D transform = new TransformMy3D();
		transform.matrix.rotY(theta);
		return transform;
	}

	public static TransformMy3D rotateZ(double theta) {
		TransformMy3D transform = new TransformMy3D();
		transform.matrix.rotZ(theta);
		return transform;
	}

	public static TransformMy3D rotate(Tuple3d xyz) {
		TransformMy3D res = rotateX(xyz.x);
		res.combine(rotateY(xyz.y));
		res.combine(rotateZ(xyz.z));
		return res;
	}

	public void apply(List<double[]> points, Point3d[] outPoints) {
		for (int i = 0; i < points.size(); i++) {
			double[] point = points.get(i);
			temp.x = point[0];
			temp.y = point[1];
			temp.z = point[2];
			//			apply(temp, outPoints[i]);
			matrix.transform(temp, outPoints[i]);
		}
	}

	public void invert() {
		matrix.invert();
	}

	public void apply(Point3d point, Point3d outPoint) {
		matrix.transform(point, outPoint);
	}

	public List<double[]> applyNoShift(List<double[]> points) {
		List<double[]> ret = Collections.synchronizedList(new ArrayList<double[]>());
		for (double[] point : points) {
			if (point == null) {
				ret.add(null);
			} else {
				ret.add(applyNoShift(point));
			}
		}
		return ret;
	}

	public double[] applyNoShift(double[] point) {
		Point3d p = new Point3d(point);
		matrix.transform(p);
		double[] ret = new double[3];
		ret[0] = p.x - matrix.m03;
		ret[1] = p.y - matrix.m13;
		ret[2] = p.z - matrix.m23;
		return ret;
	}
}
