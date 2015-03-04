package com.pioneer.gui;

import com.pioneer.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class PioneerMotorPanel extends JDialog implements Runnable {
	protected Thread t;
	public PioneerComponent robot;
	final JPanel panel;
	final JTextField lVel_T = new JTextField("0",5);
	final JTextField rVel_T = new JTextField("0",5);

	
	final int WAITTIME = 50;
	boolean suppressCommunicationError = false;

	public PioneerMotorPanel(PioneerComponent newBot)
	{
		super(new Frame(), "Pioneer Motor Panel", false);
		JPanel pane = new JPanel();
		robot = newBot;
		panel = new JPanel();
		if(robot == null)
		{
			System.out.println("PeoplebotViewPanel - NULL ROBOT");
			dispose();
			return;
		}

		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.add(new JLabel("Left wheel"));
		panel.add(lVel_T);
		panel.add(new JLabel("Right wheel"));
		panel.add(rVel_T);
		//panel.pack();
		//panel.setVisible(true);
		
		pane.add(new JLabel("Pioneer Motor Sensor"));
		pane.add(panel);

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
					System.out.println("PeoplebotViewPanel - NULL ROBOT");
					dispose();
					return;
				}

				int lVel = 	robot.getLeftVelocity();	
				int rVel = 	robot.getRightVelocity();
				lVel_T.setText(""+lVel);
				rVel_T.setText(""+rVel);
				panel.repaint();	

				suppressCommunicationError = false;
			}
			catch (Exception e)
			{
				if(!suppressCommunicationError)
				{
					System.out.println("PeoplebotSensorPanel - ERROR READING PEOPLEBOT DATA");
					suppressCommunicationError = true;
				}
			}
			try {
				Thread.sleep(WAITTIME);
			}
			catch (Exception e) {
				System.out.println("PeoplebotSensorPanel - " + e.toString()); }
		}
	}
}
