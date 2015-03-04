package utilities;

import java.io.Serializable;

public class Blob implements Serializable {
	int label;
	public int xcg;
	public int ycg;
	public int area;
	int top;
	int left;
	int right;
	int bottom;
	float alpha;
	float beta;
	float shape;
	int colorrange;   // the color range of the blob
	int rgbav;          // the average of the blob color

	public Blob(int l,int cr) {
		label = l;
		xcg = 0;
		ycg = 0;	    
		area = 0;
		// invalidate boundaries
		top = -1; 
		left = -1;
		right = -1;
		bottom = -1;

		alpha = -1;
		beta = -1;
		shape = -1;
		colorrange = cr;
	}
}

