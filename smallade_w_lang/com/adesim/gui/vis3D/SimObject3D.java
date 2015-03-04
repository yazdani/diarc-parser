package com.adesim.gui.vis3D;

import java.awt.image.BufferedImage;

import com.engine3D.my3DCore.Material;
import com.engine3D.my3DShapes.Object3D;

public class SimObject3D extends Object3D {
	private static final long serialVersionUID = 1L;

	public SimObject3D(double ax, double ay, double bx, double by, double startZ, double zHeight, int parts, Material material) {
		for(int i=0;i<parts;i++) {
			double newAX = ax + (bx - ax) * i / parts;
			double newBX = ax + (bx - ax) * (i + 1) / parts;
			
			double newAY = ay + (by - ay) * i / parts;
			double newBY = ay + (by - ay) * (i + 1) / parts;
			
			addSurface(newAX,newAY,newBX,newBY,startZ,zHeight,material);
		}
	}
	
	public SimObject3D(double ax, double ay, double bx, double by, double startZ, double zHeight, Material material) {
		addSurface(ax, ay, bx, by, startZ, zHeight, material);
	}
	
	public SimObject3D(double[] a, double[] b, double startZ, double zHeight, Material material, BufferedImage img) {
		addSurface(a[0],a[1],b[0],b[1],startZ,zHeight,material);
	}
	
	public void addSurface(double ax, double ay, double bx, double by, double startZ, double zHeight, Material material) {
		int start = points.size();
		
		double[] newPoint = new double[3];
		newPoint[0] = ay;
		newPoint[1] = -1*(startZ + zHeight);
		newPoint[2] = ax;
		points.add(newPoint);
		ptNormals.add(null);

		newPoint = new double[3];
		newPoint[0] = ay;
		newPoint[1] = -1*startZ;
		newPoint[2] = ax;
		points.add(newPoint);
		ptNormals.add(null);

		newPoint = new double[3];
		newPoint[0] = by;
		newPoint[1] = -1*(startZ + zHeight);
		newPoint[2] = bx;
		points.add(newPoint);
		ptNormals.add(null);

		newPoint = new double[3];
		newPoint[0] = by;
		newPoint[1] = -1*startZ;
		newPoint[2] = bx;
		points.add(newPoint);
		ptNormals.add(null);
		

		addTriangle(start,0,1,3,material.newImage(true,true,false,true));
		addTriangle(start,3,2,0,material.newImage(false,false,false,true));		
		addTriangle(start,3,1,0,material.newImage(false,true,true,true));
		addTriangle(start,0,2,3,material.newImage(true,false,true,true));
	}
	
}
