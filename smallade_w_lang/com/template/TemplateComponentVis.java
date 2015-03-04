package com.template;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JLabel;

import ade.gui.ADEGuiCallHelper;
import ade.gui.ADEGuiPanel;

/** A simple visualization that displays the last message that
 * this server received from another server.
 */

public class TemplateComponentVis extends ADEGuiPanel {
	private static final long serialVersionUID = 1L;
	
	private JLabel messageLabel;

	public TemplateComponentVis(ADEGuiCallHelper guiCallHelper) {
		super(guiCallHelper, 500); // update twice/second. 
		
		// initialize the GUI.  In this case, just a simple 
		//     label will do.  In more complicated cases, it is also
		//     possible to draw on the panel (as in com.adesim.gui.ADESimMapVis,
		//     or com.lrf.LRFComponentVis)
		this.setLayout(new BorderLayout());
		this.messageLabel = new JLabel("---");
		this.add(messageLabel, BorderLayout.CENTER);
	}

	
	/** The main visualization method, while will be called time and time again.
	 * It will also be called upon resizing the frame (or whenever else
	 * public void paint(Graphics g) is called. */
	@Override
	public void refreshGui() {
		String newMessage;
		
		try {
			newMessage = (String) callComponent("getComponentNameAndCounter");
		} catch (Exception e) {
			newMessage = "{could not obtain message data}";
		}
		
		this.messageLabel.setText(newMessage);
	}
	
	/** It is often best to override the initial size, so as not to rely on
	 * Swing's getPreferredSize(), which can be wrong! */
	@Override
	public Dimension getInitSize(boolean isInternalWindow) {
		return new Dimension(350, 50);
	}

}
