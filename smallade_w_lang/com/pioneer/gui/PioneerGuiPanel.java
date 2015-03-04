/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 * Last update: April 2010
 * 
 * PioneerGuiPanel.java
 */
package com.pioneer.gui;

import ade.gui.ADEGuiCallHelper;
import ade.gui.ADEGuiPanel;
import com.pioneer.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;

/**
A GUI panel for Pioneer robots that is accessed through an {@link
ade.ADEGuiComponentImpl ADEGuiComponentImpl}. See the documentation in
{@link ade.ADEGuiPanel ADEGuiPanel} for more.

@author Jim Kramer
*/
public class PioneerGuiPanel extends ADEGuiPanel {
	private static String prg = "PioneerGuiPanel";
	private static boolean debug = false;
	
	/** The local storage for encoder display. */
	private PioneerData mydata;
	
	/** The label for the encoders. */
	private JLabel encoderLabel;
	/** The text for the left encoder. */
	private JLabel leftEncoderText;
	/** The text for the right encoder. */
	private JLabel rightEncoderText;
	
	/** The label for the battery. */
	private JLabel batteryLabel;
	/** The text for the battery. */
	private JLabel batteryText;
	
	/** The label for the wheel velocities. */
	private JLabel velLabel;
	/** The text for the left encoder. */
	private JLabel leftVelText;
	/** The text for the right encoder. */
	private JLabel rightVelText;
	
	/** The forward button. */
	private JButton forward;
	/** The backward button. */
	private JButton backward;
	/** The left turn button. */
	private JButton left;
	/** The right button. */
	private JButton right;
	
	/** The window dimensions (in pixels). In this example, these values
	 * are used for the minimum, preferred, maximum, and actual window
	 * size, but they could be set otherwise. */
	private Dimension dim = new Dimension(350, 100);
	
	public PioneerGuiPanel(ADEGuiCallHelper helper) {
		super(helper, 500);
		String title = prg +" GUI Panel";
		
		// create the components and put them in the display
		//clientText = new JLabel("Unknown");
		//clientLabel = new JLabel("Is this a client?");
		//contentPane.add(clientLabel);
		//contentPane.add(clientText);
//		contentPane.add(close);           // created in superclass
		
		// set some of the window traits
		setTitle(title);
		setMinimumSize(dim);
	}
	
	
	/** Updates the components in the panel periodically (according to
	 * the value set by {@link ade.ADEGuiPanel#setUpdateTime}). */
	protected void updatePanel() {
		String newText = "Unknown";
                /*
		try {
			isClient = (Boolean)callEx("isClient");
			newText = isClient ? "Yes" : "No";
			clientText.setText(newText);
		} catch (Exception e) {
			// unusual, but let's show any exceptions here also
			clientText.setText(e.toString());
		}
                */
	}

	@Override
	public void refreshGui() {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
}
