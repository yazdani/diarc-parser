package com.engine3D.my3DShapes;

import com.engine3D.my3DCore.Material;

public class Grid3D extends Object3D {

	private static final long serialVersionUID = 92347860006L;

	public Grid3D(double[][] array, double spacing, Material material, boolean twoSided) {
		
		// Add points.
		for(int x=0;x<array.length;x++) {
			for(int y=0;y<array[x].length;y++) {
				double[] newPoint = {	x * spacing - spacing * array.length / 2.0,
										y * spacing - spacing * array[x].length / 2.0,
										array[x][y]};
				points.add(newPoint);
				
				if(x>0&&x<array.length-1&&y>0&&y<array[x].length-1) {
					
				}
				ptNormals.add(null);
			}
		}
		
		// Add triangles.
		for(int x=0;x<array.length-1;x++) {
			for(int y=0;y<array[x].length-1;y++) {
				int p11 = x * array[x].length + y;
				int p12 = x * array[x].length + (y + 1);
				int p21 = (x + 1) * array[x].length + y;
				int p22 = (x + 1) * array[x].length + (y + 1);
				int[] t1 = {p11,p12,p21};
				int[] t2 = {p22,p21,p12};
				
				triangles.add(t1);
				triangleMaterials.add(material);
				
				triangles.add(t2);
				triangleMaterials.add(material);
				
				if(twoSided) {
					int[] t3 = {p11,p21,p12};
					int[] t4 = {p22,p12,p21};
					
					triangles.add(t3);
					triangleMaterials.add(material);
					
					triangles.add(t4);
					triangleMaterials.add(material);
					
				}
			}
		}
//		recalculateCentroid();
	}
}
