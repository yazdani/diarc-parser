package com.pioneer.gui;

import com.pioneer.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class PioneerBumperPanel extends JDialog implements Runnable
{
	protected Thread t;
	public PioneerComponent robot;
	public PioneerRangePanel topPanel;
	
	final int WAITTIME = 50;
	boolean suppressCommunicationError = false;

	public PioneerBumperPanel(PioneerComponent newBot)
	{
		super(new Frame(), "Pioneer Bumper Panel", false);
		JPanel pane = new JPanel();
		robot = newBot;
		double[] rad2 = {-40, -20, 0, 20, 40, 140, 160, 180, 200, 220};
		if(robot == null)
		{
			System.out.println("PioneerViewPanel - NULL ROBOT");
			dispose();
			return;
		}

		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
				
		for(int i = 0; i < rad2.length; i++)
			rad2[i] = Math.toRadians(rad2[i]);
		topPanel = new PioneerRangePanel(rad2);

		pane.add(new JLabel("Bumper Status", SwingConstants.LEFT));
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

	public void run()
	{	
		while(true)
		{
			try
			{
				if(robot == null)
				{
					System.out.println("PioneerViewPanel - NULL ROBOT");
					dispose();
					return;
				}

				//double orientation = robot.getOrientationRad();
				double orientation = 0.0; // PWS: can't find getOrientationRad()

				boolean[] bumperData = 	robot.getBumperReadings();		
				topPanel.orientation = orientation;
				if (bumperData.length == 10)
					for(int i = 0; i < 10; i++)
						if (bumperData[i] == false)
							topPanel.vals[i] = 0;
						else
							topPanel.vals[i] = 1;
				else {
					for (int i = 0; i < 5; i++)
						topPanel.vals[i] = 0;
					for(int i = 5; i < 10; i++)
						if (bumperData[i-5] == false)
							topPanel.vals[i] = 0;
						else
							topPanel.vals[i] = 1;
				}
				topPanel.repaint();

				suppressCommunicationError = false;
			}
			catch (Exception e)
			{
				if(!suppressCommunicationError)
				{
					System.out.println("PioneerSensorPanel - ERROR READING Pioneer DATA");
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
