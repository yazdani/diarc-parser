package com.adesim.config;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class OldSimbadFileFormatConverter {

	private static final String QUOTE = "\"";
	private static final double BORDER_THICKNESS_FACTOR = 0.01;

	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.out.println("Expecting only two arguments -- the filename of the old sim, and a new file name");
			System.exit(1);
		}
		new OldSimbadFileFormatConverter(args[0], args[1]);
	}
	
	
	
	private CustomReader reader;
	private boolean preliminaryMinMaxSet = false;
	private double minX, minY, maxX, maxY;
	
	public OldSimbadFileFormatConverter(String fileIn, String fileOut) throws IOException {
		reader = new CustomReader(fileIn);
		
		String mainOutput = getConvertSimbadInputXML(); 
		// main = the walls, etc.  Not the world size or the header or 
		//   config tags at top and bottom.
		
		String worldSizeAndBoundaryWalls = getWorldSizeAndBoundaryWallsXML();
		

		StringBuilder output = new StringBuilder();
		output.append(HeaderFetcher.getHeaderInfo());
		output.append("\n\n");
		output.append("<config>");
		output.append("\n\n");
		output.append(worldSizeAndBoundaryWalls);
		output.append("\n\n");
		output.append(mainOutput);
		output.append("\n\n");
		output.append("</config>");
		output.append("\n");
		
		writeOutFile(fileOut, output.toString());
	}


	
	private void writeOutFile(String fileOutName, String output) throws IOException {
		FileWriter writer = new FileWriter(fileOutName);
		writer.write(output);
		writer.flush();
		writer.close();
	}


	private String getWorldSizeAndBoundaryWallsXML() {
		StringBuilder out = new StringBuilder();
		out.append("\t<!-- WORLD BOUNDARIES AND SURROUNDING WALLS -->");
		out.append("\n\t<!--      (TO PREVENT ROBOT FROM FALLING OFF THE EDGE OF THE UNIVERSE) -->");
		
		
		double worldWidth = maxX - minX;
		double worldHeight = maxY - minY;
		double outerWallThickness = Math.max(worldWidth, worldHeight)*BORDER_THICKNESS_FACTOR;
		
		out.append("\n\t<world>");
		out.append(getShapeDataInXML(minX - outerWallThickness, minY - outerWallThickness, 
				                 worldWidth + 2 * outerWallThickness,
				                 worldHeight + 2 * outerWallThickness));
		out.append("\n\t</world>");
		
		out.append("\n\t<!-- LEFT WORLD-EDGE WALL -->");
		out.append("\n\t<wall>");
		out.append(getShapeDataInXML(minX - outerWallThickness, minY - outerWallThickness, 
				                 outerWallThickness,
				                 worldHeight + 2 * outerWallThickness));
		out.append("\n\t</wall>");

		out.append("\n\t<!-- RIGHT WORLD-EDGE WALL -->");
		out.append("\n\t<wall>");
		out.append(getShapeDataInXML(maxX, minY - outerWallThickness, 
				                 outerWallThickness,
				                 worldHeight + 2 * outerWallThickness));
		out.append("\n\t</wall>");
		
		out.append("\n\t<!-- BOTTOM WORLD-EDGE WALL -->");
		out.append("\n\t<wall>");
		out.append(getShapeDataInXML(minX - outerWallThickness, minY - outerWallThickness, 
                worldWidth + 2 * outerWallThickness, outerWallThickness));
		out.append("\n\t</wall>");
		
		out.append("\n\t<!-- TOP WORLD-EDGE WALL -->");
		out.append("\n\t<wall>");
		out.append(getShapeDataInXML(minX - outerWallThickness, maxY, 
                worldWidth + 2 * outerWallThickness, outerWallThickness));
		out.append("\n\t</wall>");
		
		
		return out.toString();
	}



	private String getConvertSimbadInputXML() throws IOException {
		StringBuilder out = new StringBuilder();
		
		String tmp;
		while ((tmp = reader.readLine()) != null) {
			if (tmp.length() > 0) {
				if (tmp.charAt(0) == '#') {
					out.append("\n\t" + "<!-- " + tmp.substring(1) + " -->");
				} else if (tmp.startsWith("wall")) {
					processWall(reader.readLine(), out); // wall info is contained on next line
				} else {
					System.out.println("Unsupported keyword \"" + tmp + 
							"\" on line " + reader.lineCounter + 
							".  Only walls are comments are supported.");
				}
			} else {
				out.append("\n\t");
			}
		}
		
		return out.toString();
	}



	private void processWall(String wallSpecs, StringBuilder out) {
		// wall will have six values
		wallSpecs = wallSpecs.trim();
		wallSpecs = wallSpecs.replace('\t', ' ');
		while (wallSpecs.contains("  ")) {
			wallSpecs = wallSpecs.replace("  ", " ");
		}
		String[] specArray = wallSpecs.split(" ");
		if (specArray.length != 6) {
			System.out.println("Invalid num of parameters for wall in line " + reader.lineCounter + 
					".  Wall specs = \"" + wallSpecs + "\".");
		}
		double xCenter = Double.parseDouble(specArray[0]);
		double yCenter = Double.parseDouble(specArray[1]);
		double length = Double.parseDouble(specArray[3]);
		double breadth = Double.parseDouble(specArray[4]);
		
		double xLow = xCenter - length/2.0;
		double yLow = yCenter - breadth/2.0;
		double xHigh = xLow + length;
		double yHigh = yLow + breadth;
		
		out.append("\n\t<wall>");
		out.append(getShapeDataInXML(xLow, yLow, length, breadth));
		out.append("\n\t</wall>");
		
		possiblyUpdateMinsAndMaxes(new Point2D[] { 
				new Point2D.Double(xLow, yLow),
				new Point2D.Double(xHigh, yHigh),});
	}



	private String getShapeDataInXML(double xStart, double yStart, 
								 double width, double height) {
		StringBuilder out = new StringBuilder();
		out.append("\n\t\t<shape>");
		out.append("\n\t\t\t<rect" +
				" x=" + QUOTE + xStart + QUOTE + 
				" y=" + QUOTE + yStart + QUOTE + 
				" xLength=" + QUOTE + width + QUOTE + 
				" yLength=" + QUOTE + height + QUOTE + "/>");
		out.append("\n\t\t</shape>");
		return out.toString();
	}



	private void possiblyUpdateMinsAndMaxes(Point2D[] points) {
		for (Point2D pt : points) {
			if (!preliminaryMinMaxSet) {
				// set the point to be both min and max!
				minX = pt.getX();
				maxX = pt.getX();
				minY = pt.getY();
				maxY = pt.getY();
				preliminaryMinMaxSet = true;
			} else {
				minX = Math.min(minX, pt.getX());
				maxX = Math.max(maxX, pt.getX());
				minY = Math.min(minY, pt.getY());
				maxY = Math.max(maxY, pt.getY());
			}
		}
	}



	private class CustomReader {
		private int lineCounter = 1;
		private BufferedReader bufferedReader;
		
		public CustomReader(String fileIn) throws FileNotFoundException {
			bufferedReader = new BufferedReader(new FileReader(fileIn));
		}
		
		public String readLine() throws IOException {
			lineCounter++;
			return bufferedReader.readLine();
		}
		
	}
	


}
