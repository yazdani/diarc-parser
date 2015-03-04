package com.adesim.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.UUID;

import com.adesim.datastructures.DoubleDimension;
import com.adesim.datastructures.SimShape;

public class SimUtil {
	public static final String DEGREE_SYMBOL = "\u00b0";
	public static final AffineTransform nullTransformation = new AffineTransform();
    

    // Point of intersection between two lines
    // http://local.wasp.uwa.edu.au/~pbourke/geometry/lineline2d/
    public static Point2D lineIntersection(Line2D line1, Line2D line2)
    {
        Point2D p = null;
        double dx1 = line1.getX2() - line1.getX1();
        double dx2 = line2.getX2() - line2.getX1();
        double dy1 = line1.getY2() - line1.getY1();
        double dy2 = line2.getY2() - line2.getY1();
        double n1 = dx2 * (line1.getY1() - line2.getY1()) - dy2 * (line1.getX1() - line2.getX1());
        double n2 = dx1 * (line1.getY1() - line2.getY1()) - dy1 * (line1.getX1() - line2.getX1());
        double d = dy2 * dx1 - dx2 * dy1;
        double ua, ub;

        if (d == 0)
            return p; // parallel
        ua = n1 / d;
        ub = n2 / d;

        if ((ua < 0) || (ua > 1) || (ub < 0) || (ub > 1))
            return p; // intersect outside segments

        p = new Point2D.Double(line1.getX1() + ua * dx1, line1.getY1() + ua * dy1);
        return p;
    }
    
	public static double getRotationAngleFromAffineTransformation(
			AffineTransform transform) {
		double[] rotationMatrix = new double[4]; // 4 elements only since don't care about translation
		transform.getMatrix(rotationMatrix);
		Point2D rotationReferencePoint = new Point2D.Double(1, 0);
		AffineTransform rotationOnlyTransform = new AffineTransform(rotationMatrix);
		rotationOnlyTransform.transform(rotationReferencePoint, rotationReferencePoint);
		double arcTan = Math.atan2(rotationReferencePoint.getY(), rotationReferencePoint.getX());
		return arcTan;
	}
    
	
    private static final double TWO_PI = 2 * Math.PI;
    /** returns angle between 0 and 2*PI */
    public static double normalizeAngle(double aTheta) {
		if (aTheta > TWO_PI)
			return aTheta - getPIExcess(aTheta);
		else if (aTheta < 0) 
			return aTheta + TWO_PI + getPIExcess(aTheta);
		else // all good, no modifications to theta are needed:
			return aTheta;
	}
	/** A helper method for figuring out how much extra angle to add/subtract
	 * in order to get an angle between 0 and 2*PI
	 * @param angle
	 * @return
	 */
	private static double getPIExcess(double angle) {
		return TWO_PI*(int)Math.abs(angle/TWO_PI);
	}
	
	

	public static Point visCoordinatesFromWorldPoint(Point2D worldPoint,
			SimShape worldShape, Dimension visualizationDimension) {

        Dimension2D worldDim = worldShape.getBoundingDimension();
        Point2D worldMin = worldShape.getMin();
        return visCoordinatesFromWorldPoint(worldPoint, worldDim, worldMin, visualizationDimension);
	}
	
	public static Point visCoordinatesFromWorldPoint(Point2D worldPoint,
			Dimension2D worldDim, Point2D worldMin, Dimension visualizationDimension) {

		double worldPointProportionalXFromBottomLeft =
                (worldPoint.getX()-worldMin.getX()) / worldDim.getWidth();
        double worldPointProportionalYFromBottomLeft =
                (worldPoint.getY()-worldMin.getY()) / worldDim.getHeight();

        double visX = (visualizationDimension.width * worldPointProportionalXFromBottomLeft);

        // y is a little trickier, because on screen it starts at top left
        double visY = (visualizationDimension.height -
                  (visualizationDimension.height * worldPointProportionalYFromBottomLeft));

        return new Point((int)visX, (int)visY);

	}

	public static Dimension scaleDimension(Dimension dim, double scale) {
		return new Dimension((int)(dim.width*scale), (int)(dim.height*scale));
	}

	public static double distance(Point2D pt1, Point2D pt2) {
		return Point2D.distance(pt1.getX(), pt1.getY(), pt2.getX(), pt2.getY());
	}
	
	public static double heading(Point2D from, double fromAngle, Point2D to) {
		double globalAngle = getAngle0to2PI(from, to);
		double heading = globalAngle - fromAngle;
		// normalize so that the angle is no greater than PI and no less than -PI
		if (heading > Math.PI) {
			heading = heading - TWO_PI;
		} else if (heading < -1*Math.PI) {
			heading = heading + TWO_PI;
		}
		//System.out.println("heading degrees = " + Math.toDegrees(heading));
		return heading;
	}

	public static double getAngle0to2PI(Point2D from, Point2D to) {
		return getAngle0to2PI(from.getX(), from.getY(), to.getX(), to.getY());
	}

	public static double getAngle0to2PI(double fromX, double fromY, double toX, double toY) {
		double xDiff = toX - fromX;
		double yDiff = toY - fromY;
		double arcTan = Math.atan2(yDiff, xDiff);
		if (arcTan < 0) {
			arcTan = 2*Math.PI + arcTan;
		}
		return arcTan;
	}

	public static UUID generateGUID() {
		return UUID.randomUUID();
	}
	
	public static String getPointCoordinatePair(Point2D worldPoint) {
    	 return "(" + formatDecimal(worldPoint.getX()) + ", " +
         				formatDecimal(worldPoint.getY()) + ")";
	}
    public static String formatDecimal(double x) {
        BigDecimal bd = new BigDecimal(x);
    	final int ROUND_DECIMAL_PLACES = 2;
        bd = bd.setScale(ROUND_DECIMAL_PLACES, BigDecimal.ROUND_HALF_UP);
        x = bd.doubleValue();
        return Double.toString(x);
    }

	public static boolean allZeros(double[] valueArray) {
		for (double each : valueArray) {
			if (each != 0) {
				return false;
			}
		}
		// if didn't return with false, all must have been zeros
		return true;
	}
	
	
	public static void drawStringIntoRectangle(String passedString,
			Rectangle boundingBox, Graphics2D g) {
		Font originalFont = g.getFont();
		
		FontMetrics fontMetrics = g.getFontMetrics();
		String[] strings = passedString.split(" ");

		int maxStrWidth = 0;
		for (String str : strings) {
			maxStrWidth = Math.max(maxStrWidth, fontMetrics.stringWidth(str));
		}
		int strHeight = fontMetrics.getHeight();
		int totalStrHeight = strHeight * strings.length;

		AffineTransform transform = new AffineTransform();
		double minScale = Math.min(boundingBox.width / (double) maxStrWidth,
				boundingBox.height / (double) totalStrHeight);
		transform.scale(minScale, minScale);
		g.setFont(g.getFont().deriveFont(transform));

		final double NOT_QUITE_A_FULL_LINE_FRACTION = 0.8;
		int startingY = (int) (boundingBox.y
				+ (boundingBox.height - totalStrHeight * minScale) / 2.0 + NOT_QUITE_A_FULL_LINE_FRACTION
				* strHeight * minScale);
		for (int i = 0; i < strings.length; i++) {

			int variableX = (int) ((boundingBox.width - fontMetrics
					.stringWidth(strings[i])
					* minScale) / 2.0);
			g.drawString(strings[i], boundingBox.x + variableX, startingY
					+ (int) (strHeight * i * minScale));
		}
		
		g.setFont(originalFont);
	}
	
	public static Color createAlphaColor(Color color, double opacityFraction) {
		int alpha = (int) (opacityFraction * 255);
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}


	/** method to test that IDs are equal -- even if one or both of them are null */
	public static boolean IDsAreEqual(UUID id1, UUID id2) {
		if (id1 == null) {
			return (id1 == id2);
		} else {
			return id1.equals(id2);
		}
	}

	public static HashSet<String> toLowerCaseSet(String[] matchingCriteria) {
		HashSet<String> matchingCriteriaSet = new HashSet<String>(matchingCriteria.length);
		//   NOTE:  EVERYTHING IN SET WILL BE LOWER CASED, SO CAN EASILY COMPARE!
		for (String each : matchingCriteria) {
			matchingCriteriaSet.add(each.toLowerCase());
		}
		return matchingCriteriaSet;
	}
	
	public static boolean sameSign(double num1, double num2) {
		// to avoid overflow when multiplying (to check that product >= 0), break into ifs:
		if (num1 < 0) {
			return (num2 < 0); 
		} else { // num 1 is 0 or positive
			return (num2 >= 0);
		}
	}

	public static double getMaxDimension(Dimension size) {
		return Math.max(size.width, size.height);
	}
	
	public static double getMaxDimension(DoubleDimension size) {
		return Math.max(size.width, size.height);
	}
	
	public static Shape createArrowShape(Point fromPt, Point toPt) {
		Polygon arrowPolygon = new Polygon();
		arrowPolygon.addPoint(-6,1);
		arrowPolygon.addPoint(3,1);
		arrowPolygon.addPoint(3,3);
		arrowPolygon.addPoint(6,0);
		arrowPolygon.addPoint(3,-3);
		arrowPolygon.addPoint(3,-1);
		arrowPolygon.addPoint(-6,-1);
		
		
		Point midPoint = midpoint(fromPt, toPt);
		
		double rotate = Math.atan2(toPt.y - fromPt.y, toPt.x - fromPt.x);
		
		AffineTransform transform = new AffineTransform();
		transform.translate(midPoint.x, midPoint.y);
		double ptDistance = fromPt.distance(toPt);
		double scale = ptDistance / 12.0; // 12 because it's the length of the arrow polygon.
		transform.scale(scale, scale);
		transform.rotate(rotate);
		
		return transform.createTransformedShape(arrowPolygon);
	}

	private static Point midpoint(Point p1, Point p2) {
		return new Point((int)((p1.x + p2.x)/2.0), 
				         (int)((p1.y + p2.y)/2.0));
	}

}
