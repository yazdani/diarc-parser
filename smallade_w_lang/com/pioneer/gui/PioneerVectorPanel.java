package com.pioneer.gui;

import com.pioneer.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;

public class PioneerVectorPanel extends JDialog implements Runnable {
	protected Thread t;
	myPanel topPanel;

	int SIZE = 500;
	final int WAITTIME = 50;
	boolean suppressCommunicationError = false;

	class myPanel extends JPanel {
		double vX = 0.0, vY = 10.0, sX = 0.0, sY = 0.0, cX = 0.0, cY = 0.0;
		
		public myPanel()
		{
			super();
			setSize(SIZE, SIZE);
		}

		public Dimension getMinimumSize()
		{
			return new Dimension(SIZE, SIZE);
		}

		public Dimension getPreferredSize()
		{
			return new Dimension(SIZE, SIZE);
		}

		public void setVector(double x, double y) {
			vX = x;
			vY = y;
		}

		public void setVectors(double sX, double sY, double cX, double cY, double vX, double vY) {
			this.sX = sX;
			this.sY = sY;
			this.cX = cX;
			this.cY = cY;
			this.vX = vX;
			this.vY = vY;
		}

		
		public void paintComponent(Graphics comp) {
			Graphics2D comp2D = (Graphics2D)comp;

			comp2D.setColor(getBackground());
			comp2D.fillRect(0, 0, getSize().width, getSize().height);
			comp2D.setColor(Color.black);

			if ((vX > 250) || (vY > 250)) {
				if (vX > vY) {
					vY = vY * 250 / vX;
					cX = cX * 250 / vX;
					cY = cY * 250 / vX;
					sX = sX * 250 / vX;
					sY = sY * 250 / vX;
					vX = 250;
				}
				else {
					vX = vX * 250 / vY;
					cX = cX * 250 / vY;
					cY = cY * 250 / vY;
					sX = sX * 250 / vY;
					sY = sY * 250 / vY;
					vY = 250;
				}
			}
				
			comp2D.setColor(Color.green);
			comp2D.drawLine(SIZE / 2, SIZE / 2, SIZE / 2 + (int)sX, SIZE / 2 - (int)sY);
			comp2D.setColor(Color.blue);
			comp2D.drawLine(SIZE / 2, SIZE / 2, SIZE / 2 + (int)cX, SIZE / 2 - (int)cY);
			comp2D.setColor(Color.red);
			comp2D.drawLine(SIZE / 2, SIZE / 2, SIZE / 2 + (int)vX, SIZE / 2 - (int)vY);

		}

	}

	public PioneerVectorPanel()
	{
		super(new Frame(), "Pioneer Vector Panel", false);
		JPanel pane = new JPanel();

		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		topPanel = new myPanel();

		pane.add(new JLabel("Upper Sonar Ring (8 - 15)", SwingConstants.LEFT));
		pane.add(topPanel);
		setContentPane(pane);
		final JDialog f = this;
		addWindowListener(
				new WindowAdapter()
				{
					public void windowClosing(WindowEvent e)
					{
						f.setVisible(false);
						f.dispose();
					}
				});
		pack();
		setResizable(false);
		setVisible(true);
		t = new Thread(this);
		t.start();

	}

	public void setVector(double x, double y) {
		topPanel.setVector(x,y);
	}

	public void setVectors(double sX, double sY, double cX, double cY, double vX, double vY) {
		topPanel.setVectors(sX,sY,cX,cY,vX,vY);		
	}

	
	public void run()
	{

		while(true)
		{
			try
			{
				//for(int i = 8; i < 16; i++)
				//	topPanel.vals[i - 8] = (double)sonarData[i] / 5;
				topPanel.repaint();

				suppressCommunicationError = false;
			}
			catch (Exception e)
			{
				if(!suppressCommunicationError)
				{
					System.out.println("PioneerSensorPanel - ERROR READING pioneer DATA");
					suppressCommunicationError = true;
				}
			}
			try {
				Thread.sleep(WAITTIME);
			}
			catch (Exception e) {
				System.out.println("PioneerSensorPanel - " + e.toString()); }
		}
	}
}
