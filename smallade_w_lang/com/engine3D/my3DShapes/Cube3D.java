package com.engine3D.my3DShapes;

import com.engine3D.my3DCore.Material;
import java.util.ArrayList;
import java.util.List;
import javax.vecmath.Vector3d;

/**
 * Draw a cube.
 * @author bkievitk
 */
public class Cube3D extends Object3D {
	List<int[]> topTriangleIndices = new ArrayList<int[]>();
	private static final long serialVersionUID = 92347860004L;

	public Cube3D(double[] center, double radius, Material material) {
		double[] size = {radius, radius, radius};
		addBox(center, size, material);
	}

	public Cube3D(double x, double y, double z, double length, double width, double height, Material material, boolean onEnd) {
		double[] center = {x - length / 2, y, z};
		double[] size = {length, width, height};
		addBox(center, size, material);
	}

	public Cube3D(double x, double y, double z, double length, double width, double height, Material material) {
		double[] center = {x, y, z};
		double[] size = {length, width, height};
		addBox(center, size, material);
	}

	public Cube3D(double[] center, double[] size, Material material) {
		addBox(center, size, material);
	}

	public void addBox(double[] center, double[] size, Material material) {
		int start = points.size();
		for (int x = 0; x <= 1; x++) {
			for (int y = 0; y <= 1; y++) {
				for (int z = 0; z <= 1; z++) {
					double[] newPoint = new double[3];
					newPoint[0] = center[0] + (x - .5) * size[0];
					newPoint[1] = center[1] + (y - .5) * size[1];
					newPoint[2] = center[2] + (z - .5) * size[2];
					points.add(newPoint);
					ptNormals.add(null);
				}
			}
		}

		addTriangle(start, 0, 6, 4, material);
		addTriangle(start, 2, 6, 0, material);
		addTriangle(start, 1, 5, 7, material);
		addTopTriangle(start, 1, 5, 7);
		addTriangle(start, 1, 7, 3, material);
		addTopTriangle(start, 1, 7, 3);
		addTriangle(start, 0, 3, 2, material);
		addTriangle(start, 0, 1, 3, material);
		addTriangle(start, 6, 7, 4, material);
		addTriangle(start, 4, 7, 5, material);
		addTriangle(start, 0, 5, 1, material);
		addTriangle(start, 0, 4, 5, material);
		addTriangle(start, 2, 3, 7, material);
		addTriangle(start, 2, 7, 6, material);
//		recalculateCentroid();
	}

	public Vector3d getTopNormal() {
		Vector3d normal = new Vector3d();
		List<double[]> transformedPoints = getPointsInSpace();
		for (int[] triangle : topTriangleIndices) {
			normal.add(new Vector3d(getNormal(transformedPoints.get(triangle[0]),
																				transformedPoints.get(triangle[1]),
																				transformedPoints.get(triangle[2]))));
		}
		normal.normalize();
		return normal;
	}

	void addTopTriangle(int start, int a, int b, int c) {
		int[] newT = new int[3];
		newT[0] = a + start;
		newT[1] = b + start;
		newT[2] = c + start;
		topTriangleIndices.add(newT);
	}
}
