package com.pioneer.gui;

import com.pioneer.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;

public class PioneerSonarPanel extends JDialog implements Runnable{
	protected Thread t;
	public PioneerComponent robot;
	public PioneerRangePanel topPanel;
	public PioneerRangePanel bottomPanel;
	private boolean usePeoplebot = true;
	
	final int WAITTIME = 50;
	boolean suppressCommunicationError = false;
	private boolean debugReads = false;
	private int divisor = 5000;
	private static double SONAR_MAX_VAL = 5000.0;
	private int interval = 10;
	
	public PioneerSonarPanel(PioneerComponent newBot) {
		super(new Frame(), "Pioneer View Panel", false);
		JPanel pane = new JPanel();
		robot = newBot;
		double[] rad = {270, 310, 330, 350, 10, 30, 50, 90};
		double[] rad2 = {270, 310, 330, 350, 10, 30, 50, 90, 90, 130, 150, 170, 190, 210, 230, 270};
		if(robot == null) {
			System.out.println("PioneerSonarPanel - NULL ROBOT");
			dispose();
			return;
		}
		
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		// peoplebot has 24 sonar; needs an additional panel
		if (usePeoplebot) {
			for(int i = 0; i < rad.length; i++)
				rad[i] = Math.toRadians(rad[i]);
			topPanel = new PioneerRangePanel(rad);
			pane.add(new JLabel("Upper Sonar Ring (16 - 24)", SwingConstants.LEFT));
			pane.add(topPanel);
		}
		// the lower sonar ring
		for(int i = 0; i < rad2.length; i++)
			rad2[i] = Math.toRadians(rad2[i]);
		bottomPanel = new PioneerRangePanel(rad2);
		pane.add(new JLabel("Lower Sonar Ring (0 - 15)"));
		pane.add(bottomPanel);

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
	
	public void run() {
		int count = 0;
		double[] sonarData;
		
		while(true) {
			try {
				if(robot == null) {
					System.out.println("PioneerSonarPanel - NULL ROBOT");
					dispose();
					return;
				}
				
				sonarData = robot.getSonars();
				if (usePeoplebot) {
					//topPanel.orientation = robot.getOrientationRad();
					if (debugReads && count > interval) System.out.print("Top panel:");
					for(int i = 8; i < 16; i++) {
						if (debugReads && count > interval) System.out.print(" "+ (i+8) +":"+ (int)(sonarData[i] / divisor));
						topPanel.vals[i - 8] = Math.min(SONAR_MAX_VAL, (double)sonarData[i]) / divisor;
					}
					if (debugReads && count > interval) System.out.println();
					topPanel.repaint();
				}
				//bottomPanel.orientation = orientation;
				if (debugReads && count > interval) System.out.print("Bottom panel:");
				for(int i = 0; i < 8; i++) {
					bottomPanel.vals[i] = Math.min(SONAR_MAX_VAL, (double)sonarData[i]) / divisor;
					if (usePeoplebot) {
						bottomPanel.vals[i+8] = Math.min(SONAR_MAX_VAL, (double)sonarData[i+16]) / divisor;
					} else {
						bottomPanel.vals[i+8] = Math.min(SONAR_MAX_VAL, (double)sonarData[i+8]) / divisor;
					}
					if (debugReads && count > interval) {
						System.out.print(" "+ i +":"+ (int)(bottomPanel.vals[i]));
						System.out.print(" "+ (i+8) +":"+ (int)(bottomPanel.vals[i+8]));
					}
				}
				if (debugReads && count > interval) {
					System.out.println();
					count = 0;
				} else {
					count++;
				}
				bottomPanel.repaint();
				suppressCommunicationError = false;
			} catch (Exception e) {
				if(!suppressCommunicationError) {
					System.out.println("PioneerSonarrPanel - ERROR READING pioneer DATA");
					suppressCommunicationError = true;
				}
			}
			try {
				Thread.sleep(WAITTIME);
			} catch (Exception e) {
				System.out.println("PioneerSonarPanel - " + e.toString());
			}
		}
	}
}
