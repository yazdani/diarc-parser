package com.pioneer.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;

public class PioneerRangePanel extends JPanel {
	private static String prg = "PioneerRangePanel";
	protected double[] rads;
	protected double[] vals;
	protected double orientation;
	
	private int SIZE = 250;
	private int BASERADIUS = (int)(.025 * SIZE);
	private int XCENTER = SIZE/2;
	private int YCENTER = SIZE/2;
	private int dispRobotLeft = XCENTER-BASERADIUS;
	private int dispRobotBot  = YCENTER-BASERADIUS;

	public PioneerRangePanel(double[] rads) {
		super();
		this.rads = new double[rads.length];
		for(int i = 0; i < rads.length; i++)
			this.rads[i] = rads[i];
		this.vals = new double[rads.length];
		for(int i = 0; i < rads.length; i++)
			this.vals[i] = 0.5;
		orientation = 0.0;
		setSize(SIZE, SIZE);
	}
	
	public Dimension getMinimumSize() {
		return new Dimension(SIZE, SIZE);
	}
	
	public Dimension getPreferredSize() {
		return new Dimension(SIZE, SIZE);
	}
	
	public void setVals(double[] v) {
		for (int i=0; i<v.length; i++) {
			vals[i] = v[i];
		}
	}
	
	/* The one below is the original; use it for now...
	public void paintComponent(Graphics comp) {
		Graphics2D comp2D = (Graphics2D)comp;
		comp2D.setColor(getBackground());
		comp2D.fillRect(0, 0, getSize().width, getSize().height);
		comp2D.setColor(Color.black);
		
		comp2D.drawOval(SIZE / 2 - BASERADIUS, SIZE / 2 - BASERADIUS, 2 * BASERADIUS, 2 * BASERADIUS);
		comp2D.drawLine(SIZE / 2, SIZE / 2, pToCX(BASERADIUS, orientation), pToCY(BASERADIUS, orientation));
		
		comp2D.setColor(Color.blue);
		long tmp = (long)(((double)(SIZE / 2 -BASERADIUS)) + BASERADIUS);
		for(int i = 0; i < vals.length; i+=5) {
			comp2D.drawLine(pToCX(tmp, rads[i]),
								 pToCY(tmp, rads[i]),
								 pToCX((long)(tmp*vals[i]), rads[i]),
								 pToCY((long)(tmp*vals[i]), rads[i]));
			//comp2D.drawOval(pToCX((long)(((double)(SIZE / 2 - BASERADIUS)) * vals[i] + BASERADIUS), rads[i] + orientation) - 4,
			//                pToCY((long)(((double)(SIZE / 2 - BASERADIUS)) * vals[i] + BASERADIUS), rads[i] + orientation) - 4,
			//                8, 8);
		}
	}
	*/
	
	//* Try the one above...
	public void paintComponent(Graphics comp) {
		Graphics2D comp2D = (Graphics2D)comp;
		double rX, rY;
		int scrX, scrY;
		
		comp2D.setColor(getBackground());
		comp2D.fillRect(0, 0, getSize().width, getSize().height);
		//System.out.print(prg +": vals:");
		for(int i = 0; i < vals.length; i++) {
			rX = (double)dispRobotLeft * vals[i] + BASERADIUS;
			rY = (double)dispRobotBot * vals[i] + BASERADIUS;
			scrX = pToCX(rX, rads[i]);
			scrY = pToCY(rY, rads[i]);
			//System.out.print(" ("+ scrX +","+ scrY +")");
			comp2D.setColor(Color.green);
			comp2D.drawLine(XCENTER, YCENTER, scrX, scrY);
			comp2D.setColor(Color.blue);
			comp2D.drawOval(scrX-2, scrY-2, 4, 4);
		}
		//System.out.println();
		comp2D.setColor(Color.black);
		comp2D.drawOval(dispRobotLeft, dispRobotBot, 2 * BASERADIUS, 2 * BASERADIUS);
		comp2D.drawLine(XCENTER, YCENTER, XCENTER, dispRobotBot);
	}
	//*/
	
	public int pToCX(double r, double theta) {
		return((int)(r * Math.cos(theta - Math.PI / 2)) + XCENTER);
	}
	
	public int pToCY(double r, double theta) {
		return((int)(r * Math.sin(theta - Math.PI / 2)) + YCENTER);
	}
}

