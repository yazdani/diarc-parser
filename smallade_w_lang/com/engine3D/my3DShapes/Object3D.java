package com.engine3D.my3DShapes;

import com.engine3D.my3DCore.Material;
import com.engine3D.my3DCore.MatrixMath;
import com.engine3D.my3DCore.TransformMy3D;
import com.engine3D.my3DCore.Triangle3D;
import com.engine3D.my3DCore.TriangleCollision;
import com.engine3D.my3DCore.Universe;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.Point;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;

public abstract class Object3D implements Serializable {
	private static final long serialVersionUID = 1300466461134542262L;
	// Distance from the user to the screen.
	// This is used for perspective warp.
	public static final double SCREEN_DISTANCE = -50.0;
	// Axis to normalize to.
	private static final Matrix3d AXIS = new Matrix3d(0, 0, 1, 1, 0, 0, 1, 1, 1);
	// Transform for this object.
	public TransformMy3D transform = new TransformMy3D();
	private TransformMy3D lastTransform = null;
	// Children objects take this transform and then their own.
	private List<Object3D> children = Collections.synchronizedList(new ArrayList<Object3D>());
	private Object3D parent = null;
	// The points defining each triangle.
	protected List<int[]> triangles = Collections.synchronizedList(new ArrayList<int[]>());
	protected List<Material> triangleMaterials = Collections.synchronizedList(new ArrayList<Material>());
	// Points and their norms.
	protected List<double[]> points = new ArrayList<double[]>();
	protected List<double[]> ptNormals = new ArrayList<double[]>();
	protected Point3d[] pointsCache;
	protected Point3d[] pointsCache1;// = new ArrayList<Point3d>();
	protected Point3d[] pointsCache2;// = new ArrayList<Point3d>();
	protected Point3d min = new Point3d();
	protected Point3d max = new Point3d();
	protected Point3d centroid = new Point3d();
	// Define light.
	private double[] distLight_World = {.5, -.4, .5};
	private double[] distLight = normalize(distLight_World);
	boolean useFastFill = false;

	/**
	 * Flip the direction of every triangle.
	 * This is good to do after a transform that involves negative scalling.
	 */
	public void flipNormals() {
		for (int[] triangle : triangles) {
			int tmp = triangle[0];
			triangle[0] = triangle[1];
			triangle[1] = tmp;
		}
	}

//	public void recalculateCentroid() {
//		double x = 0;
//		double y = 0;
//		double z = 0;
//		float count = 0;
//		List<double[]> transformedPoints = getPointsInSpace();
//		for (double[] point : transformedPoints) {
//			x += point[0];
//			y += point[1];
//			z += point[2];
//			count++;
//		}
//		if (count > 0) {
//			centroid.set(x / count, y / count, z / count);
//		}
//		Point3d p = new Point3d();
//		for (double[] point : transformedPoints) {
//			p.set(point[0], point[1], point[2]);
//			double dist = p.distance(centroid);
//			if (dist > boundingRadius) {
//				boundingRadius = dist;
//			}
//		}
//		setUpPointsCaches();
//	}

	public Point3d getCentroid() {
		calculatePoints();
		return centroid;
	}

	public void resetPointCache() {
		lastTransform = null;
	}

//	boolean pointCacheValid(TransformMy3D cur) {
//		return cur.sameAs(lastTransform);
//	}

	void calculatePoints() {
		TransformMy3D curTransform  = getTransform();
		if (!curTransform.sameAs(lastTransform)) {
			if (pointsCache == null || pointsCache.length < points.size()) {
				pointsCache = new Point3d[points.size()];
				for (int i = 0; i < pointsCache.length; i++) {
					pointsCache[i] = new Point3d();
				}
			}
			lastTransform = curTransform;
			lastTransform.apply(points, pointsCache);

			min = min == null ? new Point3d() : min;
			max = max == null ? new Point3d() : max;

			min.x = Double.POSITIVE_INFINITY;
			min.y = Double.POSITIVE_INFINITY;
			min.z = Double.POSITIVE_INFINITY;
			max.x = Double.NEGATIVE_INFINITY;
			max.y = Double.NEGATIVE_INFINITY;
			max.z = Double.NEGATIVE_INFINITY;
			centroid = new Point3d();

			for (Point3d pt : pointsCache) {
				if (pt.x < min.x) {
					min.x = pt.x;
				}
				if (pt.y < min.y) {
					min.y = pt.y;
				}
				if (pt.z < min.z) {
					min.z = pt.z;
				}
				if (pt.x > max.x) {
					max.x = pt.x;
				}
				if (pt.y > max.y) {
					max.y = pt.y;
				}
				if (pt.z > max.z) {
					max.z = pt.z;
				}
				centroid.x += pt.x;
				centroid.y += pt.y;
				centroid.z += pt.z;
			}
			centroid.scale(1d / pointsCache.length);
		}
	}

	/**
	 * Recursively show structure.
	 */
	@Override
	public String toString() {
		return toString(0);
	}

	/**
	 * Remove this object from the scene.
	 * This removes from its parent.
	 */
	public void removeObject() {
		if (parent != null) {
			parent.children.remove(this);
		}
	}

	/**
	 * Get this object's parent object, if any.
	 * @return
	 */
	public Object3D getParent() {
		return parent;
	}

	/**
	 * Show the text for all of the points and triangles of this object.
	 * @return
	 */
	public String showPoints() {
		String ret = "points (" + points.size() + "): ";
		for (double[] point : points) {
			ret += "[";
			for (double pt : point) {
				ret += "(" + pt + ")";
			}
			ret += "]";
		}
		ret += "\ntriangles (" + triangles.size() + "):";
		for (int[] triangle : triangles) {
			ret += "[";
			for (int tri : triangle) {
				ret += "(" + tri + ")";
			}
			ret += "]";
		}
		ret += "\n";
		return ret;
	}

	/**
	 * Show the object and it's descendents.
	 * @param depth
	 * @return
	 */
	private String toString(int depth) {
		String ret = "";
		for (int i = 0; i < depth; i++) {
			ret += " ";
		}
		ret += "object\r\n";
		for (Object3D child : children) {
			ret += child.toString(depth + 1);
		}
		return ret;
	}

	private void setUpPointsCaches() {
		setUpPointsCaches(points.size());
	}

	private void setUpPointsCaches(int minLength) {
		if (pointsCache1 == null || pointsCache1.length < minLength) {
			pointsCache1 = new Point3d[minLength];
			for (int i = 0; i < minLength; i++) {
				pointsCache1[i] = new Point3d();
			}
		}
		if (pointsCache2 == null || pointsCache2.length < minLength) {
			pointsCache2 = new Point3d[minLength];
			for (int i = 0; i < minLength; i++) {
				pointsCache2[i] = new Point3d();
			}
		}
	}

	/**
	 * Get the set of all points given the current transform.
	 * @return
	 */
	public List<double[]> getPointsInSpace() {
		calculatePoints();
		List<double[]> results = new ArrayList<double[]>(pointsCache.length);
		for (int i = 0; i < points.size(); i++) {  // not a bug, if points shrinks pointsCache will not be reallocated
			Point3d pt = pointsCache[i];
			double[] point = {pt.x, pt.y, pt.z};
			results.add(point);
		}
		return results;
	}

	/**
	 * Apply a transform to the actual point set.
	 * @param transform
	 */
	//M@: permApply considered harmful
//	public void permApplyTransform(TransformMy3D transform) {
//		calculatePoints();
//		for (int i = 0; i < points.size(); i++) {
//			points.get(i)[0] = pointsCache[i].x;
//			points.get(i)[1] = pointsCache[i].y;
//			points.get(i)[2] = pointsCache[i].z;
//		}
//		ptNormals = transform.applyNoShift(ptNormals);
//	}

	/**
	 * Add a new point to the 3D object.
	 * @param point
	 * @param ptNormal
	 */
	public void addPoint(double[] point, double[] ptNormal) {
		points.add(point);
		ptNormals.add(ptNormal);
	}

	/**
	 * Add a new triangle to the 3D object.
	 * @param triangle
	 * @param material
	 */
	public void addTriangle(int[] triangle, Material material) {
		triangles.add(triangle);
		triangleMaterials.add(material);
	}

	/**
	 * Add a polygon to the 3D object.
	 * This will only parse appropriately for flat, convex hulls.
	 * @param points
	 * @param material
	 */
	public void addPolygon(int[] points, Material material) {
		for (int i = 1; i < points.length - 1; i++) {
			int[] tri = {points[0], points[i], points[i + 1]};
			triangles.add(tri);
			triangleMaterials.add(material);
		}
	}

	/**
	 * Test if this point is on the inside of the triangle.
	 * @param pt
	 * @param triangle
	 * @return
	 */
	public boolean inside(double[] pt, int triangle) {
		return inside(pt, triangles.get(triangle));
	}

	/**
	 * Test if this point is on the inside of the triangle.
	 * @param pt
	 * @param triangle
	 * @return
	 */
	public boolean inside(double[] pt, int[] triangle) {

		for (int triID : triangle) {
			if (MatrixMath.equals(pt, points.get(triID))) {
				return true;
			}
		}

		double[] normal = getNormal(points.get(triangle[0]), points.get(triangle[1]), points.get(triangle[2]));
		double[] toPoint = MatrixMath.sub(pt, points.get(triangle[0]));
		if (MatrixMath.dotProduct(normal, toPoint) > 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Get the linear object inscribed by moving this object from the first to the second transform.
	 * @param t1
	 * @param t2
	 * @return
	 */
	public Object3D getTraversal(TransformMy3D t1, TransformMy3D t2) {
		setUpPointsCaches();

		t1.apply(points, pointsCache1);
		List<double[]> ptNormals1 = t1.applyNoShift(ptNormals);

		t2.apply(points, pointsCache2);
		List<double[]> ptNormals2 = t2.applyNoShift(ptNormals);

		Object3D newObject = new NullObject3D();
		for (int i = 0; i < points.size(); i++) {
			Point3d pt = pointsCache1[i];
			double[] dpt = {pt.x, pt.y, pt.z};
			newObject.points.add(dpt);
			pt = pointsCache2[i];
			double[] dpt2 = {pt.x, pt.y, pt.z};
			newObject.points.add(dpt2);
		}
//		newObject.points.addAll(points1);
//		newObject.points.addAll(points2);
		newObject.ptNormals.addAll(ptNormals1);
		newObject.ptNormals.addAll(ptNormals2);
//		recalculateCentroid();

		// Add first triangles.
		for (int i = 0; i < triangles.size(); i++) {
			newObject.triangles.add(MatrixMath.copy(triangles.get(i)));
			newObject.triangleMaterials.add(triangleMaterials.get(i).copy());
		}

		// Add second triangles.
		for (int i = 0; i < triangles.size(); i++) {
			// Use this size offset.
			newObject.triangles.add(MatrixMath.add(triangles.get(i), pointsCache1.length));
			newObject.triangleMaterials.add(triangleMaterials.get(i).copy());
		}

		// Then add the connections between.
		for (int i = 0; i < triangles.size(); i++) {
			int[] tri1 = MatrixMath.copy(triangles.get(i));
			int[] tri2 = MatrixMath.add(triangles.get(i), pointsCache1.length);

			for (int j = 0; j < tri1.length; j++) {
				int[] newTri1 = {tri1[j], tri1[(j + 1) % tri1.length], tri2[j]};
				newObject.triangles.add(newTri1);
				newObject.triangleMaterials.add(triangleMaterials.get(i).copy());

				int[] newTri2 = {tri2[j], tri1[(j + 1) % tri1.length], tri2[(j + 1) % tri1.length]};
				newObject.triangles.add(newTri2);
				newObject.triangleMaterials.add(triangleMaterials.get(i).copy());
			}
		}

		// Get list of internal points to remove.

		/*
		// For each point.
		for(int i=0;i<newObject.points.size();i++) {
		// For each shell triangle.
		boolean keep = false;
		for(int j=triangles.size()*2;j<newObject.triangles.size();j++) {
		if(!newObject.inside(newObject.points.get(i), j)) {
		keep = true;
		System.out.println("Here");
		break;
		}
		}
		if(!keep) {
		System.out.println("Remove " + i);
		}
		}
		 */
		return newObject;
	}

	/**
	 * Remove these points from the object.
	 * Removes all triangles containing any of these points.
	 * Adjusts the triangle point values.
	 * @param toRemove
	 */
	public void removePoints(HashSet<Integer> toRemove) {
		// Build transfer table first.
		int[] transferTable = new int[points.size()];
		int index = 0;
		for (int i = 0; i < transferTable.length; i++) {
			transferTable[i] = index;
			if (!toRemove.contains(i)) {
				index++;
			}
		}

		// Remove points and normals.
		List<double[]> newPoints = Collections.synchronizedList(new ArrayList<double[]>());
		List<double[]> newNormals = Collections.synchronizedList(new ArrayList<double[]>());
		for (int i = 0; i < points.size(); i++) {
			if (!toRemove.contains(i)) {
				newPoints.add(points.get(i));
				newNormals.add(ptNormals.get(i));
			}
		}
		points = newPoints;
		ptNormals = newNormals;
//		recalculateCentroid();

		// Remove and relabel triangles.
		List<int[]> newTriangles = Collections.synchronizedList(new ArrayList<int[]>());
		List<Material> newTriangleMaterial = Collections.synchronizedList(new ArrayList<Material>());
		for (int i = 0; i < triangles.size(); i++) {
			int[] triangle = triangles.get(i);
			boolean contains = false;
			for (int j : triangle) {
				if (toRemove.contains(j)) {
					contains = true;
					break;
				}
			}
			// Only add triangle if it does not contain a deleted point.
			if (!contains) {
				int[] newTriangle = new int[triangle.length];
				for (int j = 0; j < newTriangle.length; j++) {
					newTriangle[j] = transferTable[triangle[j]];
				}
				newTriangles.add(triangle);
				newTriangleMaterial.add(triangleMaterials.get(i));
			}
		}
		triangles = newTriangles;
		triangleMaterials = newTriangleMaterial;
	}

	/**
	 * Find the average of the points.
	 * @return
	 */
	public double[] findCenter() {
		double[] pointSum = new double[3];
		for (double[] point : points) {
			for (int i = 0; i < point.length; i++) {
				pointSum[i] += point[i];
			}
		}
		for (int i = 0; i < pointSum.length; i++) {
			pointSum[i] /= points.size();
		}
		return pointSum;
	}

	/**
	 * Add an object as a child.
	 * @param child
	 */
	public void addChild(Object3D child) {
		children.add(child);
		child.parent = this;
	}

	/**
	 * Set the material for all triangles in the object.
	 * @param m
	 */
	public void setAllMaterial(Material m) {
		for (int i = 0; i < triangleMaterials.size(); i++) {
			triangleMaterials.set(i, m);
		}
	}

	/**
	 * Just change the color for all triangles in the object.
	 * @param color
	 */
	public void setAllMaterialColor(Color color) {
		for (Material m : triangleMaterials) {
			m.color = color;
		}
	}

	/**
	 * Set the color of all triangles for this object and all descendants.
	 * @param color
	 */
	public void setAllMaterialColorRecursive(Color color) {
		setAllMaterialColorRecursive(color, this);
	}

	/**
	 * Actually does the recursive work.
	 * @param color
	 * @param thisObject
	 */
	private void setAllMaterialColorRecursive(Color color, Object3D thisObject) {
		if (thisObject != null) {
			for (Material m : triangleMaterials) {
				m.color = color;
			}
			for (Object3D child : children) {
				setAllMaterialColorRecursive(color, child);
			}
		}
	}

	/**
	 * List of all children objects.
	 * @return
	 */
	public List<Object3D> getChildren() {
		return children;
	}

	/**
	 * Apply the full transform heiarchy of parents to find the world relative transform of this object.
	 * @return
	 */
	public TransformMy3D getTransform() {
		if (parent == null) {
			return transform;
		}
		return parent.getTransform().combineNew(transform);
	}

	/**
	 * Apply perspective transform to all of these points.
	 * This also centers the points in the window frame and scales them appropriately.
	 * @param points
	 * @param width
	 * @param height
	 * @return
	 */
	private List<double[]> applyPerspective(Point3d[] points, int width, int height) {
		List<double[]> newPoints = Collections.synchronizedList(new ArrayList<double[]>());
		for (Point3d point : points) {
			newPoints.add(applyPerspective(point, width, height));
		}
		return newPoints;
	}

	/**
	 * Test of this object or its descendants collides with the given object.
	 * @param object
	 * @return
	 */
	public boolean collisionRecursive(Object3D object) {
		return collisionRecursive(object, this);
	}

	/**
	 * Test the collisions.
	 * @param object
	 * @param thisObject
	 * @return
	 */
	private boolean collisionRecursive(Object3D object, Object3D thisObject) {
		if (thisObject == null) {
			return false;
		}
		if (thisObject.collision(object)) {
			return true;
		}
		for (Object3D child : children) {
			if (collisionRecursive(object, child)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Test if this object is colliding with the given object.
	 * @param object
	 * @return
	 */
	public boolean collision(Object3D object) {
		return collision(this, object);
	}

	/**
	 * Test if this object is colliding with the given object.
	 * @param object2
	 * @return
	 */
	public static boolean collision(Object3D object1, Object3D object2) {
		object1.calculatePoints();
		object2.calculatePoints();

		// Try bounding box collision.
		if (!boundingBoxesCollide(object1.min, object1.max, object2.min, object2.max)) {
			return false;
		}

		// Try each triangle for each shape.
		for (int[] triangle1 : object1.triangles) {
			for (int[] triangle2 : object2.triangles) {
				if (TriangleCollision.tri_tri_intersect(object1.pointsCache[triangle1[0]],
																								object1.pointsCache[triangle1[1]],
																								object1.pointsCache[triangle1[2]],
																								object2.pointsCache[triangle2[0]],
																								object2.pointsCache[triangle2[1]],
																								object2.pointsCache[triangle2[2]])) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Test if the bounding boxes of two objects collide.
	 * @param b1
	 * @param b2
	 * @return
	 */
	private static boolean boundingBoxesCollide(Point3d aMin, Point3d aMax,
																							Point3d bMin, Point3d bMax) {
		return !(aMin.x > bMax.x || aMin.y > bMax.y || aMin.z > bMax.z
						 || bMin.x > aMax.x || bMin.y > aMax.y || bMin.z > aMax.z);
	}

	/**
	 * Get the bounding box for a set of points.
	 * @param points
	 * @param outMin A return variable for the minimum corner
	 * @param outMax A return variable for the maximum corner
	 */
	private static void getBoundingBox(Point3d[] points,
																		 Point3d outMin,
																		 Point3d outMax) {
		outMin.x = Double.POSITIVE_INFINITY;
		outMin.y = Double.POSITIVE_INFINITY;
		outMin.z = Double.POSITIVE_INFINITY;
		outMax.x = Double.NEGATIVE_INFINITY;
		outMax.y = Double.NEGATIVE_INFINITY;
		outMax.z = Double.NEGATIVE_INFINITY;

		for (Point3d pt : points) {
			if (pt.x < outMin.x) {
				outMin.x = pt.x;
			}
			if (pt.y < outMin.y) {
				outMin.y = pt.y;
			}
			if (pt.z < outMin.z) {
				outMin.z = pt.z;
			}
			if (pt.x > outMax.x) {
				outMax.x = pt.x;
			}
			if (pt.y > outMax.y) {
				outMax.y = pt.y;
			}
			if (pt.z > outMax.z) {
				outMax.z = pt.z;
			}
		}
	}

	/**
	 * Apply perspective transform.
	 * @param point
	 * @param width
	 * @param height
	 * @return
	 */
	private double[] applyPerspective(double[] point, int width, int height) {
		double x = point[0];
		double y = point[1];
		double z = point[2];

		double multiplier = SCREEN_DISTANCE / (z + SCREEN_DISTANCE);
		double dx = multiplier * x;
		double dy = multiplier * y;

		double[] newPoint = new double[3];
		newPoint[0] = dx * width / 80 + width / 2;
		newPoint[1] = dy * width / 80 + height / 2;
		newPoint[2] = z * width / 80;

		return newPoint;
	}

	private double[] applyPerspective(Point3d point, int width, int height) {
		double multiplier = SCREEN_DISTANCE / (point.z + SCREEN_DISTANCE);
		double dx = multiplier * point.x;
		double dy = multiplier * point.y;

		double[] newPoint = new double[3];
		newPoint[0] = dx * width / 80 + width / 2;
		newPoint[1] = dy * width / 80 + height / 2;
		newPoint[2] = point.z * width / 80;

		return newPoint;
	}

	/**
	 * Draw a single point in three space.
	 * @param zBuffer
	 * @param x
	 * @param y
	 * @param z
	 * @param g
	 * @param color
	 */
	protected void drawZPoint(double[][] zBuffer, int x, int y, double z, Graphics g, Color color) {
		if (x >= 0 && y >= 0 && x < zBuffer.length && y < zBuffer[0].length) {
			zBuffer[x][y] = z;
			g.setColor(color);
			g.drawLine(x, y, x, y);
		}
	}

	/**
	 * http://www.cs.unc.edu/~mcmillan/comp136/Lecture6/Lines.html
	 * @param zBuffer
	 * @param x0
	 * @param y0
	 * @param x1
	 * @param y1
	 * @param z
	 */
	protected void drawZLine(double[][] zBuffer, int x0, int y0, int x1, int y1, double z, Graphics g, Color color) {
		int dy = y1 - y0;
		int dx = x1 - x0;
		int stepx, stepy;

		if (dy < 0) {
			dy = -dy;
			stepy = -1;
		} else {
			stepy = 1;
		}
		if (dx < 0) {
			dx = -dx;
			stepx = -1;
		} else {
			stepx = 1;
		}

		drawZPoint(zBuffer, x0, y0, z, g, color);
		drawZPoint(zBuffer, x1, y1, z, g, color);

		if (dx > dy) {
			int length = (dx - 1) >> 2;
			int extras = (dx - 1) & 3;
			int incr2 = (dy << 2) - (dx << 1);
			if (incr2 < 0) {
				int c = dy << 1;
				int incr1 = c << 1;
				int d = incr1 - dx;
				for (int i = 0; i < length; i++) {
					x0 += stepx;
					x1 -= stepx;
					if (d < 0) {														// Pattern:
						drawZPoint(zBuffer, x0, y0, z, g, color);						//
						drawZPoint(zBuffer, x0 += stepx, y0, z, g, color);				//  x o o
						drawZPoint(zBuffer, x1, y1, z, g, color);						//
						drawZPoint(zBuffer, x1 -= stepx, y1, z, g, color);
						d += incr1;
					} else {
						if (d < c) {													// Pattern:
							drawZPoint(zBuffer, x0, y0, z, g, color);					//      o
							drawZPoint(zBuffer, x0 += stepx, y0 += stepy, z, g, color);	//  x o
							drawZPoint(zBuffer, x1, y1, z, g, color);					//
							drawZPoint(zBuffer, x1 -= stepx, y1 -= stepy, z, g, color);
						} else {
							drawZPoint(zBuffer, x0, y0 += stepy, z, g, color);			// Pattern:
							drawZPoint(zBuffer, x0 += stepx, y0, z, g, color);			//    o o
							drawZPoint(zBuffer, x1, y1 -= stepy, z, g, color);			//  x
							drawZPoint(zBuffer, x1 -= stepx, y1, z, g, color);			//
						}
						d += incr2;
					}
				}
				if (extras > 0) {
					if (d < 0) {
						drawZPoint(zBuffer, x0 += stepx, y0, z, g, color);
						if (extras > 1) {
							drawZPoint(zBuffer, x0 += stepx, y0, z, g, color);
						}
						if (extras > 2) {
							drawZPoint(zBuffer, x1 -= stepx, y1, z, g, color);
						}
					} else if (d < c) {
						drawZPoint(zBuffer, x0 += stepx, y0, z, g, color);
						if (extras > 1) {
							drawZPoint(zBuffer, x0 += stepx, y0 += stepy, z, g, color);
						}
						if (extras > 2) {
							drawZPoint(zBuffer, x1 -= stepx, y1, z, g, color);
						}
					} else {
						drawZPoint(zBuffer, x0 += stepx, y0 += stepy, z, g, color);
						if (extras > 1) {
							drawZPoint(zBuffer, x0 += stepx, y0, z, g, color);
						}
						if (extras > 2) {
							drawZPoint(zBuffer, x1 -= stepx, y1 -= stepy, z, g, color);
						}
					}
				}
			} else {
				int c = (dy - dx) << 1;
				int incr1 = c << 1;
				int d = incr1 + dx;
				for (int i = 0; i < length; i++) {
					x0 += stepx;
					x1 -= stepx;
					if (d > 0) {
						drawZPoint(zBuffer, x0, y0 += stepy, z, g, color);				// Pattern:
						drawZPoint(zBuffer, x0 += stepx, y0 += stepy, z, g, color);		//      o
						drawZPoint(zBuffer, x1, y1 -= stepy, z, g, color);				//    o
						drawZPoint(zBuffer, x1 -= stepx, y1 -= stepy, z, g, color);		//  x
						d += incr1;
					} else {
						if (d < c) {
							drawZPoint(zBuffer, x0, y0, z, g, color);					// Pattern:
							drawZPoint(zBuffer, x0 += stepx, y0 += stepy, z, g, color); //      o
							drawZPoint(zBuffer, x1, y1, z, g, color);                   //  x o
							drawZPoint(zBuffer, x1 -= stepx, y1 -= stepy, z, g, color); //
						} else {
							drawZPoint(zBuffer, x0, y0 += stepy, z, g, color);			// Pattern:
							drawZPoint(zBuffer, x0 += stepx, y0, z, g, color);			//    o o
							drawZPoint(zBuffer, x1, y1 -= stepy, z, g, color);			//  x
							drawZPoint(zBuffer, x1 -= stepx, y1, z, g, color);			//
						}
						d += incr2;
					}
				}
				if (extras > 0) {
					if (d > 0) {
						drawZPoint(zBuffer, x0 += stepx, y0 += stepy, z, g, color);
						if (extras > 1) {
							drawZPoint(zBuffer, x0 += stepx, y0 += stepy, z, g, color);
						}
						if (extras > 2) {
							drawZPoint(zBuffer, x1 -= stepx, y1 -= stepy, z, g, color);
						}
					} else if (d < c) {
						drawZPoint(zBuffer, x0 += stepx, y0, z, g, color);
						if (extras > 1) {
							drawZPoint(zBuffer, x0 += stepx, y0 += stepy, z, g, color);
						}
						if (extras > 2) {
							drawZPoint(zBuffer, x1 -= stepx, y1, z, g, color);
						}
					} else {
						drawZPoint(zBuffer, x0 += stepx, y0 += stepy, z, g, color);
						if (extras > 1) {
							drawZPoint(zBuffer, x0 += stepx, y0, z, g, color);
						}
						if (extras > 2) {
							if (d > c) {
								drawZPoint(zBuffer, x1 -= stepx, y1 -= stepy, z, g, color);
							} else {
								drawZPoint(zBuffer, x1 -= stepx, y1, z, g, color);
							}
						}
					}
				}
			}
		} else {
			int length = (dy - 1) >> 2;
			int extras = (dy - 1) & 3;
			int incr2 = (dx << 2) - (dy << 1);
			if (incr2 < 0) {
				int c = dx << 1;
				int incr1 = c << 1;
				int d = incr1 - dy;
				for (int i = 0; i < length; i++) {
					y0 += stepy;
					y1 -= stepy;
					if (d < 0) {
						drawZPoint(zBuffer, x0, y0, z, g, color);
						drawZPoint(zBuffer, x0, y0 += stepy, z, g, color);
						drawZPoint(zBuffer, x1, y1, z, g, color);
						drawZPoint(zBuffer, x1, y1 -= stepy, z, g, color);
						d += incr1;
					} else {
						if (d < c) {
							drawZPoint(zBuffer, x0, y0, z, g, color);
							drawZPoint(zBuffer, x0 += stepx, y0 += stepy, z, g, color);
							drawZPoint(zBuffer, x1, y1, z, g, color);
							drawZPoint(zBuffer, x1 -= stepx, y1 -= stepy, z, g, color);
						} else {
							drawZPoint(zBuffer, x0 += stepx, y0, z, g, color);
							drawZPoint(zBuffer, x0, y0 += stepy, z, g, color);
							drawZPoint(zBuffer, x1 -= stepx, y1, z, g, color);
							drawZPoint(zBuffer, x1, y1 -= stepy, z, g, color);
						}
						d += incr2;
					}
				}
				if (extras > 0) {
					if (d < 0) {
						drawZPoint(zBuffer, x0, y0 += stepy, z, g, color);
						if (extras > 1) {
							drawZPoint(zBuffer, x0, y0 += stepy, z, g, color);
						}
						if (extras > 2) {
							drawZPoint(zBuffer, x1, y1 -= stepy, z, g, color);
						}
					} else if (d < c) {
						drawZPoint(zBuffer, stepx, y0 += stepy, z, g, color);
						if (extras > 1) {
							drawZPoint(zBuffer, x0 += stepx, y0 += stepy, z, g, color);
						}
						if (extras > 2) {
							drawZPoint(zBuffer, x1, y1 -= stepy, z, g, color);
						}
					} else {
						drawZPoint(zBuffer, x0 += stepx, y0 += stepy, z, g, color);
						if (extras > 1) {
							drawZPoint(zBuffer, x0, y0 += stepy, z, g, color);
						}
						if (extras > 2) {
							drawZPoint(zBuffer, x1 -= stepx, y1 -= stepy, z, g, color);
						}
					}
				}
			} else {
				int c = (dx - dy) << 1;
				int incr1 = c << 1;
				int d = incr1 + dy;
				for (int i = 0; i < length; i++) {
					y0 += stepy;
					y1 -= stepy;
					if (d > 0) {
						drawZPoint(zBuffer, x0 += stepx, y0, z, g, color);
						drawZPoint(zBuffer, x0 += stepx, y0 += stepy, z, g, color);
						drawZPoint(zBuffer, x1 -= stepy, y1, z, g, color);
						drawZPoint(zBuffer, x1 -= stepx, y1 -= stepy, z, g, color);
						d += incr1;
					} else {
						if (d < c) {
							drawZPoint(zBuffer, x0, y0, z, g, color);
							drawZPoint(zBuffer, x0 += stepx, y0 += stepy, z, g, color);
							drawZPoint(zBuffer, x1, y1, z, g, color);
							drawZPoint(zBuffer, x1 -= stepx, y1 -= stepy, z, g, color);
						} else {
							drawZPoint(zBuffer, x0 += stepx, y0, z, g, color);
							drawZPoint(zBuffer, x0, y0 += stepy, z, g, color);
							drawZPoint(zBuffer, x1 -= stepx, y1, z, g, color);
							drawZPoint(zBuffer, x1, y1 -= stepy, z, g, color);
						}
						d += incr2;
					}
				}
				if (extras > 0) {
					if (d > 0) {
						drawZPoint(zBuffer, x0 += stepx, y0 += stepy, z, g, color);
						if (extras > 1) {
							drawZPoint(zBuffer, x0 += stepx, y0 += stepy, z, g, color);
						}
						if (extras > 2) {
							drawZPoint(zBuffer, x1 -= stepx, y1 -= stepy, z, g, color);
						}
					} else if (d < c) {
						drawZPoint(zBuffer, x0, y0 += stepy, z, g, color);
						if (extras > 1) {
							drawZPoint(zBuffer, x0 += stepx, y0 += stepy, z, g, color);
						}
						if (extras > 2) {
							drawZPoint(zBuffer, x1, y1 -= stepy, z, g, color);
						}
					} else {
						drawZPoint(zBuffer, x0 += stepx, y0 += stepy, z, g, color);
						if (extras > 1) {
							drawZPoint(zBuffer, x0, y0 += stepy, z, g, color);
						}
						if (extras > 2) {
							if (d > c) {
								drawZPoint(zBuffer, x1 -= stepx, y1 -= stepy, z, g, color);
							} else {
								drawZPoint(zBuffer, x1, y1 -= stepy, z, g, color);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Render a 3D triangle.
	 * @param img			Current image.
	 * @param zBuffer		Height of closes object.
	 * @param objBuffer		Object defined for closest object.
	 * @param n				Normals.
	 * @param t				Transformed points.
	 * @param p				Perspective.
	 * @param material		Material to render as.
	 * @param renderType	Type of rendering.
	 */
	private void renderTriangle(BufferedImage img, double[][] zBuffer, Object[][] objBuffer, double[][] n, double[][] t, double[][] p, Material material, int renderType) {

		Graphics g = img.getGraphics();

		if (renderType == Universe.RENDER_POINTS) {

			//g.setColor(triangleMaterial.get(t).color);
			int color;

			color = Math.min(Math.max(0, (int) (p[0][2] / 4)), 255);
			g.setColor(new Color(color, color, color));
			g.drawLine((int) p[0][0], (int) p[0][1], (int) p[0][0], (int) p[0][1]);

			color = Math.min(Math.max(0, (int) (p[1][2] / 4)), 255);
			g.setColor(new Color(color, color, color));
			g.drawLine((int) p[1][0], (int) p[1][1], (int) p[1][0], (int) p[1][1]);

			color = Math.min(Math.max(0, (int) (p[2][2] / 4)), 255);
			g.setColor(new Color(color, color, color));
			g.drawLine((int) p[2][0], (int) p[2][1], (int) p[2][0], (int) p[2][1]);

		} else if (renderType == Universe.RENDER_WIREFRAME) {

			g.setColor(material.color);
			//drawZLine(zBuffer, (int)p[0][0], (int)p[0][1], (int)p[1][0], (int)p[1][1], Double.MAX_VALUE,g,material.color);
			//drawZLine(zBuffer, (int)p[1][0], (int)p[1][1], (int)p[2][0], (int)p[2][1], Double.MAX_VALUE,g,material.color);
			//drawZLine(zBuffer, (int)p[2][0], (int)p[2][1], (int)p[0][0], (int)p[0][1], Double.MAX_VALUE,g,material.color);
			g.drawLine((int) p[0][0], (int) p[0][1], (int) p[1][0], (int) p[1][1]);
			g.drawLine((int) p[1][0], (int) p[1][1], (int) p[2][0], (int) p[2][1]);
			g.drawLine((int) p[2][0], (int) p[2][1], (int) p[0][0], (int) p[0][1]);

		} else {

			double[] normal = getNormal(t[0], t[1], t[2]);
			double theta = getTheta(getNormal(p[0], p[1], p[2]));

			if (theta <= Math.PI / 2) {
				Material triMaterial = material;
				double[] triNormal = normal;
				Triangle3D triangle = new Triangle3D(triMaterial, t, n, p, triNormal);
				if (useFastFill) {
					fasterFill(zBuffer, objBuffer, img, triangle, renderType);
				} else {
					slowerFill(zBuffer, objBuffer, img, triangle, renderType);
				}
			}
		}
	}

	/**
	 * Find the point at which this line crosses the plane.
	 * @param pt1
	 * @param pt2
	 * @return
	 */
	private double[] planeIntercept(double[] pt1, double[] pt2) {
		double zIntercept = -SCREEN_DISTANCE - .05;
		double a = (zIntercept - pt1[2]) / (pt2[2] - pt1[2]);
		double x = pt1[0] + a * (pt2[0] - pt1[0]);
		double y = pt1[1] + a * (pt2[1] - pt1[1]);
		double[] intercept = {x, y, zIntercept};
		return intercept;
	}

	/**
	 * Find the fraction of how close the point is to pt1 for the point that crosses the plane.
	 * @param pt1
	 * @param pt2
	 * @return
	 */
	private double planeInterceptFraction(double[] pt1, double[] pt2) {
		double zIntercept = -SCREEN_DISTANCE - .05;
		double a = (zIntercept - pt1[2]) / (pt2[2] - pt1[2]);
		return a;
	}

	/**
	 * The second and third point are behind the view scene.
	 * @param n
	 * @param t
	 * @param p
	 * @param img
	 * @param zBuffer
	 * @param objBuffer
	 * @param transformOld
	 * @param renderType
	 * @param material
	 */
	private void renderTwoBehind(double[][] n, double[][] t, double[][] p, BufferedImage img, double[][] zBuffer, Object[][] objBuffer, TransformMy3D transformOld, int renderType, Material material) {

		double[] t12 = planeIntercept(t[0], t[1]);
		double[] t13 = planeIntercept(t[0], t[2]);
		double[] p12 = applyPerspective(t12, img.getWidth(), img.getHeight());
		double[] p13 = applyPerspective(t13, img.getWidth(), img.getHeight());
		double a12 = planeInterceptFraction(t[0], t[1]);
		double a13 = planeInterceptFraction(t[0], t[2]);
		double[] n12;
		double[] n13;

		// Normals may be null.
		if (n[0] == null || n[2] == null) {
			n12 = null;
		} else {
			n12 = MatrixMath.add(MatrixMath.multiply(n[1], 1 - a12), MatrixMath.multiply(n[0], a12));
		}

		if (n[1] == null || n[2] == null) {
			n13 = null;
		} else {
			n13 = MatrixMath.add(MatrixMath.multiply(n[2], 1 - a13), MatrixMath.multiply(n[0], a13));
		}

		double[][] t1N = {n12, n13, n[0]};
		double[][] t1T = {t12, t13, t[0]};
		double[][] t1P = {p12, p13, p[0]};

		renderTriangle(img, zBuffer, objBuffer, t1N, t1T, t1P, material, renderType);
	}

	/**
	 * The third point is behind the view scene.
	 * @param n
	 * @param t
	 * @param p
	 * @param img
	 * @param zBuffer
	 * @param objBuffer
	 * @param transformOld
	 * @param renderType
	 * @param material
	 */
	private void renderOneBehind(double[][] n, double[][] t, double[][] p, BufferedImage img, double[][] zBuffer, Object[][] objBuffer, TransformMy3D transformOld, int renderType, Material material) {

		double[] t13 = planeIntercept(t[0], t[2]);
		double[] t23 = planeIntercept(t[1], t[2]);
		double[] p13 = applyPerspective(t13, img.getWidth(), img.getHeight());
		double[] p23 = applyPerspective(t23, img.getWidth(), img.getHeight());
		double a13 = planeInterceptFraction(t[0], t[2]);//(t13[2] - t[0][2]) / (t[2][2] - t[0][2]);
		double a23 = planeInterceptFraction(t[1], t[2]);//(t23[2] - t[1][2]) / (t[2][2] - t[1][2]);
		double[] n13;
		double[] n23;

		// Normals may be null.
		if (n[0] == null || n[2] == null) {
			n13 = null;
		} else {
			n13 = MatrixMath.add(MatrixMath.multiply(n[2], 1 - a13), MatrixMath.multiply(n[0], a13));
		}

		if (n[1] == null || n[2] == null) {
			n23 = null;
		} else {
			n23 = MatrixMath.add(MatrixMath.multiply(n[2], 1 - a23), MatrixMath.multiply(n[1], a23));
		}

		double[][] t1N = {n13, n[0], n23};
		double[][] t1T = {t13, t[0], t23};
		double[][] t1P = {p13, p[0], p23};

		renderTriangle(img, zBuffer, objBuffer, t1N, t1T, t1P, material, renderType);

		double[][] t2N = {n[0], n[1], n23};
		double[][] t2T = {t[0], t[1], t23};
		double[][] t2P = {p[0], p[1], p23};

		renderTriangle(img, zBuffer, objBuffer, t2N, t2T, t2P, material, renderType);
	}

	public void fillingParallelProject(BufferedImage img,
																		 Color color,
																		 double scale,
																		 double xOffset,
																		 double yOffset) {
//		lastTransform = null;
//		calculatePoints();
		Graphics g = img.getGraphics();
		g.setColor(color);

		// For all triangles.
		for (int tID = 0; tID < triangles.size(); tID++) {
			Point3d t1 = pointsCache[triangles.get(tID)[0]];
			Point3d t2 = pointsCache[triangles.get(tID)[1]];
			Point3d t3 = pointsCache[triangles.get(tID)[2]];
			int[] xs = {(int) (t1.x * scale + xOffset), (int) (t2.x * scale + xOffset), (int) (t3.x * scale + xOffset)};
			int[] zs = {(int) (t1.z * scale + yOffset), (int) (t2.z * scale + yOffset), (int) (t3.z * scale + yOffset)};
			g.fillPolygon(xs, zs, 3);
		}
	}

	public Point3d[] getPointsCache(int whichCache) {
		switch (whichCache) {
			case 0:
				return pointsCache;
			case 1:
				return pointsCache1;
			case 2:
				return pointsCache2;
		}
		return null;
	}

	/**
	 * First, perform transforms, then render all triangles.
	 * @param img
	 * @param zBuffer
	 * @param objBuffer
	 * @param transformOld
	 * @param renderType
	 */
	public void render(BufferedImage img, double[][] zBuffer, Object[][] objBuffer, TransformMy3D transformOld, int renderType) {
		setUpPointsCaches();
		TransformMy3D localTransform = transformOld.combineNew(this.transform);

		// Transformed points.
		localTransform.apply(points, pointsCache1);

		// Transformed from original normals.
		List<double[]> transformedNormals = localTransform.applyNoShift(ptNormals);

		// Perspective applied to points.
		List<double[]> perspective = applyPerspective(pointsCache1, img.getWidth(), img.getHeight());

		// Where to clip plane.
		double zCuttoff = SCREEN_DISTANCE;

		// For all triangles.
		for (int tID = 0; tID < triangles.size(); tID++) {

			Point3d t1 = pointsCache1[triangles.get(tID)[0]];
			Point3d t2 = pointsCache1[triangles.get(tID)[1]];
			Point3d t3 = pointsCache1[triangles.get(tID)[2]];

			double[] p1 = perspective.get(triangles.get(tID)[0]);
			double[] p2 = perspective.get(triangles.get(tID)[1]);
			double[] p3 = perspective.get(triangles.get(tID)[2]);

			double[] n1 = transformedNormals.get(triangles.get(tID)[0]);
			double[] n2 = transformedNormals.get(triangles.get(tID)[1]);
			double[] n3 = transformedNormals.get(triangles.get(tID)[2]);

			// Determine how many and which points are behind the viewer.
			// Then run the appropriate clipping function.

			if (t1.z < -zCuttoff && t2.z < -zCuttoff && t3.z < -zCuttoff) {
				// All in front of viewer.
				double[][] n = {n1, n2, n3};
				double[][] t = {{t1.x, t1.y, t1.z}, {t2.x, t2.y, t2.z}, {t3.x, t3.y, t3.z}};
				double[][] p = {p1, p2, p3};
				renderTriangle(img, zBuffer, objBuffer, n, t, p, triangleMaterials.get(tID), renderType);
			} else if (t1.z >= -zCuttoff && t2.z >= -zCuttoff && t3.z >= -zCuttoff) {
				// All behind viewer.
			} else {

				// Some in front and some behind.
				if (t1.z < -zCuttoff) {
					if (t2.z < -zCuttoff) {
						// 3 behind
						double[][] n = {n1, n2, n3};
						double[][] t = {{t1.x, t1.y, t1.z}, {t2.x, t2.y, t2.z}, {t3.x, t3.y, t3.z}};
//						double[][] t = {t1, t2, t3};
						double[][] p = {p1, p2, p3};
						renderOneBehind(n, t, p, img, zBuffer, objBuffer, transformOld, renderType, triangleMaterials.get(tID));
					} else if (t3.z < -zCuttoff) {
						// 2 behind
						double[][] n = {n3, n1, n2};
						double[][] t = {{t3.x, t3.y, t3.z}, {t1.x, t1.y, t1.z}, {t2.x, t2.y, t2.z}};
//						double[][] t = {t3, t1, t2};
						double[][] p = {p3, p1, p2};
						renderOneBehind(n, t, p, img, zBuffer, objBuffer, transformOld, renderType, triangleMaterials.get(tID));
					} else {
						// 2 and 3 behind.
						double[][] n = {n1, n2, n3};
						double[][] t = {{t1.x, t1.y, t1.z}, {t2.x, t2.y, t2.z}, {t3.x, t3.y, t3.z}};
//						double[][] t = {t1, t2, t3};
						double[][] p = {p1, p2, p3};
						renderTwoBehind(n, t, p, img, zBuffer, objBuffer, transformOld, renderType, triangleMaterials.get(tID));
					}
				} else if (t2.z < -zCuttoff) {
					if (t3.z < -zCuttoff) {
						// 1 behind
						double[][] n = {n2, n3, n1};
						double[][] t = {{t2.x, t2.y, t2.z}, {t3.x, t3.y, t3.z}, {t1.x, t1.y, t1.z}};
//						double[][] t = {t2, t3, t1};
						double[][] p = {p2, p3, p1};
						renderOneBehind(n, t, p, img, zBuffer, objBuffer, transformOld, renderType, triangleMaterials.get(tID));
					} else {
						// 1 and 3 behind
						double[][] n = {n2, n3, n1};
						double[][] t = {{t2.x, t2.y, t2.z}, {t3.x, t3.y, t3.z}, {t1.x, t1.y, t1.z}};
//						double[][] t = {t2, t3, t1};
						double[][] p = {p2, p3, p1};
						renderTwoBehind(n, t, p, img, zBuffer, objBuffer, transformOld, renderType, triangleMaterials.get(tID));
					}
				} else {
					// 1 and 2 behind
					double[][] n = {n3, n1, n2};
					double[][] t = {{t3.x, t3.y, t3.z}, {t1.x, t1.y, t1.z}, {t2.x, t2.y, t2.z}};
//					double[][] t = {t3, t1, t2};
					double[][] p = {p3, p1, p2};
					renderTwoBehind(n, t, p, img, zBuffer, objBuffer, transformOld, renderType, triangleMaterials.get(tID));
				}

			}
		}

		// Render each child next.
		for (Object3D p : children) {
			p.render(img, zBuffer, objBuffer, localTransform, renderType);
		}
	}

	/**
	 * Normal of the three points.
	 * @param p1
	 * @param p2
	 * @param p3
	 * @return
	 */
	public double[] getNormal(double[] p1, double[] p2, double[] p3) {

		double ax = p1[0] - p2[0];
		double ay = p1[1] - p2[1];
		double az = p1[2] - p2[2];

		double bx = p3[0] - p2[0];
		double by = p3[1] - p2[1];
		double bz = p3[2] - p2[2];

		// A is the cross product.
		double[] normal = {ay * bz - az * by,
											 az * bx - ax * bz,
											 ax * by - ay * bx};

		normal = normalize(normal);

		return normal;
	}

	/**
	 * Angle between two vectors.
	 * @param pt1
	 * @param pt2
	 * @return
	 */
	private double getAngle(double[] pt1, double[] pt2) {
		double len1 = Math.sqrt(pt1[0] * pt1[0] + pt1[1] * pt1[1] + pt1[2] * pt1[2]);
		double len2 = Math.sqrt(pt2[0] * pt2[0] + pt2[1] * pt2[1] + pt2[2] * pt2[2]);
		double dot = pt2[0] * pt1[0] + pt2[1] * pt1[1] + pt2[2] * pt1[2];
		double angle = dot / (len1 * len2);
		return Math.acos(angle);
	}

	/**
	 * Get the color transform of the material given the normal.
	 * @param normal
	 * @param material
	 * @return
	 */
	private double getColor(double[] normal, Material material) {
		double lAngleN = getAngle(normal, distLight);

		double LOnN = lAngleN;
		double LOnNx = normal[0] * LOnN;
		double LOnNy = normal[1] * LOnN;
		double LOnNz = normal[2] * LOnN;

		// Reflection.
		double Rx = LOnNx + LOnNx - distLight[0];
		double Ry = LOnNy + LOnNy - distLight[1];
		double Rz = LOnNz + LOnNz - distLight[2];

		double rDotV = -(Rx * 0 + Ry * 0 + Rz * 1);

		int s = material.s;  		// Is a shininess constant for this material, which decides how "evenly" light is reflected from a shiny spot.
		double ia = material.ia; 	// Ambiant intensity.
		double id = material.id;  	// Diffusion intensity of light.
		double is = material.is;  	// Specular intensity of light.

		return ia + lAngleN * id + Math.pow(rDotV, s) * is;
	}

	/**
	 * Fill in the triangle.
	 * @param zBuffer
	 * @param objBuffer
	 * @param img
	 * @param triangle3D
	 * @param renderType
	 */
	private void fasterFill(double[][] zBuffer, Object[][] objBuffer, BufferedImage img, Triangle3D triangle3D, int renderType) {

		double[] p1 = triangle3D.perspective[0];
		double[] p2 = triangle3D.perspective[1];
		double[] p3 = triangle3D.perspective[2];

		// Absolute bounds on triangle.
		int minX = (int) Math.floor(Math.min(Math.min(p1[0], p2[0]), p3[0]));
		int minY = (int) Math.floor(Math.min(Math.min(p1[1], p2[1]), p3[1]));
		int maxX = (int) Math.ceil(Math.max(Math.max(p1[0], p2[0]), p3[0]));
		int maxY = (int) Math.ceil(Math.max(Math.max(p1[1], p2[1]), p3[1]));

		minX = Math.min(Math.max(minX, 0), img.getWidth() - 1);
		minY = Math.min(Math.max(minY, 0), img.getHeight() - 1);
		maxX = Math.min(Math.max(maxX, 0), img.getWidth() - 1);
		maxY = Math.min(Math.max(maxY, 0), img.getHeight() - 1);

		// No distance, triangle is flat.
		if (maxX == minX || maxY == minY) {
			return;
		}

		// Calculate transform matrix.
		Matrix3d m3t = new Matrix3d(p1[0], p2[0], p3[0], p1[1], p2[1], p3[1], 1, 1, 1);
		m3t.invert();
		m3t.mul(AXIS, m3t);
//		m3t.mul(AXIS);
		m3t.transpose();
//		double[][] m3ti = MatrixMath.invert(m3t);
//		double[][] m2t = MatrixMath.multiply(m3ti, AXIS);
//		double[][] m2 = MatrixMath.transpose(m2t);

		Color faceColor = triangle3D.material.color;

		// Default color for this face.
		double col = 0;
		col = getColor(triangle3D.normal, triangle3D.material);

		// Used for checking if in triangle.
		Point.Double pt1 = new Point.Double(p1[0], p1[1]);
		Point.Double pt2 = new Point.Double(p2[0], p2[1]);
		Point.Double pt3 = new Point.Double(p3[0], p3[1]);

		/*
		for(int y=minY;y<=maxY;y++) {
		int startX = minX; for(; startX < maxX && !pointInTriangle(new Point.Double(startX,y),pt1,pt2,pt3); startX++);
		int stopX = maxX; for(; stopX > minX && !pointInTriangle(new Point.Double(stopX,y),pt1,pt2,pt3); stopX--);
		horizline(zBuffer, objBuffer, img, triangle3D, renderType, m2, col, faceColor, startX, stopX, y);
		}
		 */

		int lastStartX = minX;
		int lastStopX = maxX;
		for (int y = minY; y <= maxY; y++) {

			int startX = lastStartX;
			if (pointInTriangle(new Point.Double(startX, y), pt1, pt2, pt3)) {
				for (; startX > minX && pointInTriangle(new Point.Double(startX, y), pt1, pt2, pt3); startX--) {
				}
				//startX++;
			} else {
				for (; startX < maxX && !pointInTriangle(new Point.Double(startX, y), pt1, pt2, pt3); startX++) {
				}
				startX--;
			}

			int stopX = lastStopX;
			if (pointInTriangle(new Point.Double(stopX, y), pt1, pt2, pt3)) {
				for (; stopX < maxX && pointInTriangle(new Point.Double(stopX, y), pt1, pt2, pt3); stopX++) {
				}
				//stopX--;
			} else {
				for (; stopX > minX && !pointInTriangle(new Point.Double(stopX, y), pt1, pt2, pt3); stopX--) {
				}
				stopX++;
			}

			if (startX > stopX) {
				lastStartX = minX;
				lastStopX = maxX;
			} else {
				lastStartX = startX;
				lastStopX = stopX;
				horizline(zBuffer, objBuffer, img, triangle3D, renderType, m3t, col, faceColor, startX, stopX, y);
			}
		}

		/*
		Point pta,ptb,ptc;
		if(pt1.y < pt2.y && pt1.y < pt3.y) {
		if(pt2.y < pt3.y) {
		pta = ptInt(pt1); ptb = ptInt(pt2); ptc = ptInt(pt3);
		} else {
		pta = ptInt(pt1); ptb = ptInt(pt3); ptc = ptInt(pt2);
		}
		} else if(pt2.y < pt3.y) {
		if(pt1.y < pt3.y) {
		pta = ptInt(pt2); ptb = ptInt(pt1); ptc = ptInt(pt3);
		} else {
		pta = ptInt(pt2); ptb = ptInt(pt3); ptc = ptInt(pt1);
		}
		} else {
		if(pt1.y < pt2.y) {
		pta = ptInt(pt3); ptb = ptInt(pt1); ptc = ptInt(pt2);
		} else {
		pta =ptInt(pt3); ptb = ptInt(pt2); ptc = ptInt(pt1);
		}
		}

		int dx1,dx2,dx3;
		if (ptb.y-pta.y > 0) {dx1=(ptb.x-pta.x)/(ptb.y-pta.y);} else {dx1=0;}
		if (ptc.y-pta.y > 0) {dx2=(ptc.x-pta.x)/(ptc.y-pta.y);} else {dx2=0;}
		if (ptc.y-ptb.y > 0) {dx3=(ptc.x-ptb.x)/(ptc.y-ptb.y);} else {dx3=0;}

		Point pts = new Point(pta.x,pta.y);
		Point pte = new Point(pta.x,pta.y);
		if(dx1 > dx2) {
		for(;pts.y<=ptb.y;pts.y++,pte.y++,pts.x+=dx2,pte.x+=dx1) {
		if(pts.y >= minY && pts.y <= maxY) {
		horizline(zBuffer, objBuffer, img, triangle3D, renderType, m2, col, faceColor, pts.x, pte.x, pts.y);
		}
		}
		pte = new Point(ptb.x,ptb.y);
		for(;pts.y<=ptc.y;pts.y++,pte.y++,pts.x+=dx2,pte.x+=dx3) {
		if(pts.y >= minY && pts.y <= maxY) {
		horizline(zBuffer, objBuffer, img, triangle3D, renderType, m2, col, faceColor, pts.x, pte.x, pts.y);
		}
		}
		} else {
		for(;pts.y<=ptb.y;pts.y++,pte.y++,pts.x+=dx1,pte.x+=dx2) {
		if(pts.y >= minY && pts.y <= maxY) {
		horizline(zBuffer, objBuffer, img, triangle3D, renderType, m2, col, faceColor, pts.x, pte.x, pts.y);
		}
		}
		pts = new Point(ptb.x,ptb.y);
		for(;pts.y<=ptc.y;pts.y++,pte.y++,pts.x+=dx3,pte.x+=dx2) {
		if(pts.y >= minY && pts.y <= maxY) {
		horizline(zBuffer, objBuffer, img, triangle3D, renderType, m2, col, faceColor, pts.x, pte.x, pts.y);
		}
		}
		}
		 */
	}

	/**
	 * Render a line of the triangle.
	 * @param zBuffer
	 * @param objBuffer
	 * @param img
	 * @param triangle3D
	 * @param renderType
	 * @param m2
	 * @param col
	 * @param faceColor
	 * @param startX
	 * @param endX
	 * @param y
	 */
	private void horizline(double[][] zBuffer, Object[][] objBuffer, BufferedImage img, Triangle3D triangle3D, int renderType,
												 Matrix3d m2, double col, Color faceColor, int startX, int endX, int y) {

		double[] p1 = triangle3D.perspective[0];
		double[] p2 = triangle3D.perspective[1];
		double[] p3 = triangle3D.perspective[2];

		double[] n1 = triangle3D.transformedNormals[0];
		double[] n2 = triangle3D.transformedNormals[1];
		double[] n3 = triangle3D.transformedNormals[2];

		startX = Math.min(img.getWidth() - 1, Math.max(0, startX));
		endX = Math.min(img.getWidth() - 1, Math.max(0, endX));

		for (int x = startX; x <= endX; x++) {
			// Calculate a,b and c from the transform matrix.
			double c = x * m2.m00 + y * m2.m10 + m2.m20;
			double a = x * m2.m01 + y * m2.m11 + m2.m21;
			double b = 1 - c - a;

			// Calculate z.
			double z = a * p1[2] + b * p2[2] + c * p3[2];

			// Closer than closest point.
			if (zBuffer[x][y] < z) {

				if (renderType == Universe.RENDER_DEPTH) {

					// Color is a function of depth.
					int color = (int) (z / 2);
					color = Math.min(Math.max(0, color), 255);
					int rgb = new Color(color, color, color).getRGB();

					img.setRGB(x, y, rgb);
					objBuffer[x][y] = triangle3D.material.object;
					zBuffer[x][y] = z;


				} else {
					Color base;

					if (renderType == Universe.RENDER_FLAT) {

						// Use flat color.
						base = triangle3D.material.color;
					} else if (renderType == Universe.RENDER_NORMAL) {

						// Get light from new normal.
						if (n1 != null && n2 != null && n3 != null) {

							double[] newNormal = {-a * n1[0] - b * n2[0] - c * n3[0],
																		-a * n1[1] - b * n2[1] - c * n3[1],
																		-a * n1[2] - b * n2[2] - c * n3[2]};

							triangle3D.normal = normalize(newNormal);
							col = getColor(triangle3D.normal, triangle3D.material);
						}

						// Use face color or image color.
						base = triangle3D.material.getColor(a, c);
					} else {
						System.out.println("Unknown render type.");
						return;
					}

					int red = (int) (base.getRed() * col);
					int green = (int) (base.getGreen() * col);
					int blue = (int) (base.getBlue() * col);

					Color old = new Color(img.getRGB(x, y));
					int alpha = triangle3D.material.color.getAlpha();
					red = (red * alpha + old.getRed() * (255 - alpha)) / 255;
					green = (green * alpha + old.getGreen() * (255 - alpha)) / 255;
					blue = (blue * alpha + old.getBlue() * (255 - alpha)) / 255;

					red = Math.max(Math.min(red, 255), 0);
					green = Math.max(Math.min(green, 255), 0);
					blue = Math.max(Math.min(blue, 255), 0);
					faceColor = new Color(red, green, blue);

					img.setRGB(x, y, faceColor.getRGB());
					objBuffer[x][y] = triangle3D.material.object;
					zBuffer[x][y] = z;
				}
			}
		}
	}

	/**
	 * Render using, non-updated scanning.
	 * @param zBuffer
	 * @param objBuffer
	 * @param img
	 * @param triangle3D
	 * @param renderType
	 */
	private void slowerFill(double[][] zBuffer, Object[][] objBuffer, BufferedImage img, Triangle3D triangle3D, int renderType) {

		double[] p1 = triangle3D.perspective[0];
		double[] p2 = triangle3D.perspective[1];
		double[] p3 = triangle3D.perspective[2];

		double[] n1 = triangle3D.transformedNormals[0];
		double[] n2 = triangle3D.transformedNormals[1];
		double[] n3 = triangle3D.transformedNormals[2];

		// Absolute bounds on triangle.
		int minX = (int) Math.floor(Math.min(Math.min(p1[0], p2[0]), p3[0]));
		int minY = (int) Math.floor(Math.min(Math.min(p1[1], p2[1]), p3[1]));
		int maxX = (int) Math.ceil(Math.max(Math.max(p1[0], p2[0]), p3[0]));
		int maxY = (int) Math.ceil(Math.max(Math.max(p1[1], p2[1]), p3[1]));

		minX = Math.max(minX, 0);
		minY = Math.max(minY, 0);
		maxX = Math.min(maxX, img.getWidth());
		maxY = Math.min(maxY, img.getHeight());

		// No distance, triangle is flat.
		if (maxX == minX || maxY == minY) {
			return;
		}

		// Calculate transform matrix.
//		double[][] m3t = {{p1[0], p2[0], p3[0]}, {p1[1], p2[1], p3[1]}, {1, 1, 1}};
//		double[][] m3ti = MatrixMath.invert(m3t);
//		double[][] m2t = MatrixMath.multiply(m3ti, AXIS);
//		double[][] m2 = MatrixMath.transpose(m2t);
		Matrix3d m2 = new Matrix3d(p1[0], p2[0], p3[0], p1[1], p2[1], p3[1], 1, 1, 1);
		try {
			m2.invert();
		} catch (Exception e) {
			System.err.println("could not invert matrix in Object3D.slowerFill!  " + e);
		}
		m2.mul(AXIS, m2);
//		m2.mul(AXIS);
		m2.transpose();

		Color faceColor = triangle3D.material.color;

		// Default color for this face.
		double col = getColor(triangle3D.normal, triangle3D.material);

		// Used for checking if in triangle.
		Point.Double pt1 = new Point.Double(p1[0], p1[1]);
		Point.Double pt2 = new Point.Double(p2[0], p2[1]);
		Point.Double pt3 = new Point.Double(p3[0], p3[1]);

		// For every point within.
		for (int y = minY; y <= maxY; y++) {
			for (int x = minX; x <= maxX; x++) {

				// In triangle and in screen.
				if (x >= 0 && y >= 0 && x < zBuffer.length && y < zBuffer[0].length && pointInTriangle(new Point.Double(x, y), pt1, pt2, pt3)) {

					// Calculate a,b and c from the transform matrix.
					double c = x * m2.m00 + y * m2.m10 + m2.m20;
					double a = x * m2.m01 + y * m2.m11 + m2.m21;
					double b = 1 - c - a;

					// Calculate z.
					double z = a * p1[2] + b * p2[2] + c * p3[2];

					// Closer than closest point.
					if (zBuffer[x][y] < z) {

						if (renderType == Universe.RENDER_DEPTH) {

							// Color is a function of depth.
							int color = (int) (z / 2);
							color = Math.min(Math.max(0, color), 255);
							int rgb = new Color(color, color, color).getRGB();

							img.setRGB(x, y, rgb);
							objBuffer[x][y] = triangle3D.material.object;
							zBuffer[x][y] = z;


						} else {
							Color base;

							if (renderType == Universe.RENDER_FLAT) {

								// Use flat color.
								base = triangle3D.material.color;
							} else {

								// Get light from new normal.
								if (n1 != null && n2 != null && n3 != null) {

									double[] newNormal = {-a * n1[0] - b * n2[0] - c * n3[0],
																				-a * n1[1] - b * n2[1] - c * n3[1],
																				-a * n1[2] - b * n2[2] - c * n3[2]};

									triangle3D.normal = normalize(newNormal);
									col = getColor(triangle3D.normal, triangle3D.material);
								}

								// Use face color or image color.
								base = triangle3D.material.getColor(a, c);
							}

							int red = (int) (base.getRed() * col);
							int green = (int) (base.getGreen() * col);
							int blue = (int) (base.getBlue() * col);

							Color old = new Color(img.getRGB(x, y));
							int alpha = triangle3D.material.color.getAlpha();
							red = (red * alpha + old.getRed() * (255 - alpha)) / 255;
							green = (green * alpha + old.getGreen() * (255 - alpha)) / 255;
							blue = (blue * alpha + old.getBlue() * (255 - alpha)) / 255;

							red = Math.max(Math.min(red, 255), 0);
							green = Math.max(Math.min(green, 255), 0);
							blue = Math.max(Math.min(blue, 255), 0);
							faceColor = new Color(red, green, blue);

							img.setRGB(x, y, faceColor.getRGB());
							objBuffer[x][y] = triangle3D.material.object;
							zBuffer[x][y] = z;
						}
					}
				}
			}
		}
	}

	/**
	 * Apply the power function with an integer exponent.
	 * @param base
	 * @param power
	 * @return
	 */
	private double intPower(double base, int power) {
		double sum = 1;
		for (int i = 0; i < power; i++) {
			sum *= base;
		}
		return sum;
	}

	/**
	 * Subtract one point from another.
	 * @param p1
	 * @param p2
	 * @return
	 */
	private Point.Double sub(Point.Double p1, Point.Double p2) {
		return new Point.Double(p1.x - p2.x, p1.y - p2.y);
	}

	/**
	 * Cross product between two points.
	 * @param p1
	 * @param p2
	 * @return
	 */
	private double crossProduct(Point.Double p1, Point.Double p2) {
		return p1.x * p2.y - p1.y * p2.x;
	}

	/**
	 * Test if a and b are on the same side of the line p1,p2.
	 * @param p1
	 * @param p2
	 * @param a
	 * @param b
	 * @return
	 */
	private boolean sameSide(Point.Double p1, Point.Double p2, Point.Double a, Point.Double b) {
		double cp1 = crossProduct(sub(b, a), sub(p1, a));
		double cp2 = crossProduct(sub(b, a), sub(p2, a));
		return (cp1 * cp2 >= 0);
	}

	/**
	 * Test if p is inside of the triangle abc.
	 * @param p
	 * @param a
	 * @param b
	 * @param c
	 * @return
	 */
	private boolean pointInTriangle(Point.Double p, Point.Double a, Point.Double b, Point.Double c) {
		return sameSide(p, a, b, c) && sameSide(p, b, a, c) && sameSide(p, c, a, b);
	}

	/**
	 * Convert a double point to an int point.
	 * @param pt
	 * @return
	 */
	//private Point ptInt(Point.Double pt) {
	//	return new Point((int)pt.x,(int)pt.y);
	//}
	/**
	 * Calculate theta for this angle.
	 * @param p
	 * @return
	 */
	private double getTheta(double[] p) {
		double adb = -p[2];
		double lenB = getLen(p);
		return Math.acos(adb / lenB);
	}

	/**
	 * Line length.
	 * @param p
	 * @return
	 */
	private double getLen(double[] p) {
		return Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]);
	}

	/**
	 * Normalize to length 1.
	 * @param p
	 * @return
	 */
	private double[] normalize(double[] p) {
		double len = getLen(p);
		double[] norm = new double[3];
		norm[0] = p[0] / len;
		norm[1] = p[1] / len;
		norm[2] = p[2] / len;
		return norm;
	}

	/**
	 * Rotate an image by 90 degrees.
	 * @param img
	 * @return
	 */
	public static BufferedImage rotateImage(BufferedImage img) {
		BufferedImage ret = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_BGR);
		for (int x = 0; x < img.getWidth(); x++) {
			for (int y = 0; y < img.getHeight(); y++) {
				ret.setRGB(x, y, img.getRGB(img.getWidth() - x - 1, img.getHeight() - y - 1));
			}
		}
		return ret;
	}

	/**
	 * Flip an image.
	 * @param img
	 * @return
	 */
	public static BufferedImage flipImage(BufferedImage img) {
		BufferedImage ret = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_BGR);
		for (int x = 0; x < img.getWidth(); x++) {
			for (int y = 0; y < img.getHeight(); y++) {
				ret.setRGB(x, y, img.getRGB(img.getWidth() - x - 1, y));
			}
		}
		return ret;
	}

	/**
	 * Add a triangle to the object.
	 * @param start
	 * @param a
	 * @param b
	 * @param c
	 * @param material
	 */
	public void addTriangle(int start, int a, int b, int c, Material material) {
		int[] newT = new int[3];
		newT[0] = a + start;
		newT[1] = b + start;
		newT[2] = c + start;
		triangles.add(newT);
		triangleMaterials.add(material);
	}
}
