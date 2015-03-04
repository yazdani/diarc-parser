package com.engine3D.my3DShapes;

import com.engine3D.my3DCore.Material;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Any arbitrary 3D Mesh.
 * @author M@ <matthew.dunlap+hrilab@gmail.com>
 */
public class Mesh3D extends Object3D {
	public enum MeshFormat {
		STL,
		VRML,
		WRL
	}
	private Material material;
	private double scale;
	HashMap<double[], Integer> coordIndices;

	public Mesh3D(String objectName) {
		material = new Material(Color.yellow, objectName, null);
		scale = 1;
	}

	public Mesh3D(String filename, MeshFormat format, String scale, Material material) {
		this.material = material;
		if (scale.isEmpty()) {
			this.scale = 1;
		} else {
			this.scale = Double.parseDouble(scale);
		}
		
		switch (format) {
			case STL:
				loadFromStl(filename);
				break;
			case WRL:		// fallthrough here since ".wrl" is the extension for VRML format files
			case VRML:
				throw new UnsupportedOperationException("Not yet implemented");
			//break;
		}
	}

	public void addPoint(double x, double y, double z) {
		double[] pt = {x,y,z};
		addPoint(pt, pt);
	}

	public void addTriangle(int point0, int point1, int point2) {
		int[] tri = {point0, point1, point2};
		addTriangle(tri, material);
	}
	
	public void addPolygon(int[] points) {
		addPolygon(points, material);
	}

	private void loadFromStl(String filename) {
		Pattern p = Pattern.compile("vertex\\s+(-?[\\d\\.]+(e-?\\d+)?)\\s+(-?[\\d\\.]+(e-?\\d+)?)\\s+(-?[\\d\\.]+(e-?\\d+)?)");
		coordIndices = new HashMap<double[], Integer>();
		try {
			String contents = readFileAsString(filename);
			String[] facets = contents.split("endfacet");
			for (String f : facets) {
				loadFacet(f, p);
			}
		} catch (IOException ex) {
		}
	}

	private void loadFacet(String facet, Pattern vertexPattern) {
		Matcher m = vertexPattern.matcher(facet);
		int pIndeces[] = {0, 0, 0};
		int p = 0;
		while (m.find()) {
			double point[] = new double[3];
			point[0] = Double.parseDouble(m.group(1)) * scale;
			point[1] = Double.parseDouble(m.group(3)) * scale;
			point[2] = Double.parseDouble(m.group(5)) * scale;
			int index = points.size();
			if (coordIndices.containsKey(point)) {
				index = coordIndices.get(point);
			} else {
				addPoint(point, point);
				coordIndices.put(point, index);
			}
			pIndeces[p++] = index;
		}
		addTriangle(pIndeces, material);
	}

	private static String readFileAsString(String filePath)
					throws java.io.IOException {
		StringBuilder fileData = new StringBuilder(1000);
		BufferedReader reader = new BufferedReader(
						new FileReader(filePath));
		char[] buf = new char[1024];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
			buf = new char[1024];
		}
		reader.close();
		return fileData.toString();
	}
}
