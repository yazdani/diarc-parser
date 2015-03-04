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
import java.awt.*;
import javax.swing.*;

/**
A GUI panel for Pioneer robots that is accessed through an {@link
ade.ADEGuiComponentImpl ADEGuiComponentImpl}. See the documentation in
{@link ade.ADEGuiPanel ADEGuiPanel} for more.

@author Jim Kramer
*/
public class PioneerJoystickPanel extends ADEGuiPanel {
	private static String prg = "PioneerJoystickPanel";
	private static boolean debug = false;
	
   private javax.swing.JButton jButton1;
   private javax.swing.JButton jButton2;
   private javax.swing.JButton jButton3;
   private javax.swing.JButton jButton4;
   private javax.swing.JButton jButton5;
   private javax.swing.JLabel jLabel1;
   private javax.swing.JTextField jTextField1;
   private javax.swing.JTextField jTextField2;
	
	// maintain local speed settings
	private boolean gotVels = false;
	private int TV_INC = 20;
	private int RV_INC = 5;
	private int targetTV = 0, targetRV = 0;
	//private int maxTV, maxRV;
	private int[] vels = new int[]{0, 0};
	
	/** The window dimensions (in pixels). In this example, these values
	 * are used for the minimum, preferred, maximum, and actual window
	 * size, but they could be set otherwise. */
	private Dimension dim;
	
	public PioneerJoystickPanel(ADEGuiCallHelper helper) {
		super(helper, 500);
		String title = prg +" GUI Panel";
		
		// create the components and put them in the display
		initComponents();
		setLayout(new GridLayout(5,3));
		
		add(jTextField2); // left vel
		add(jLabel1);     // label
		add(jTextField1); // right vel
		
		add(new JLabel());
		add(new JLabel());
		add(new JLabel());
		
		add(new JLabel());
		add(jButton1);    // go forward
		add(new JLabel());
		
		add(jButton4);    // go left
		add(jButton5);    // go stop
		add(jButton3);    // go right
		
		add(new JLabel());
		add(jButton2);    // go back
		add(new JLabel());
		
		// set some of the window traits
		setTitle(title);
//		pack();
		dim = getSize();
		setMinimumSize(dim);
		setPreferredSize(dim);
	}
	
	
	/** Updates the components in the panel periodically (according to
	 * the value set by {@link ade.ADEGuiPanel#setUpdateTime}). */
	protected void updatePanel() {
		try {
			vels = (int[])callComponent("getVelocities");
      	jTextField1.setText(""+ vels[1]); // right
	      jTextField2.setText(""+ vels[0]); // left
			if (!gotVels) gotVels = true;
		} catch (Exception e) {
			System.err.println(prg +": exception getting velocities:");
			System.err.println(e);
		}
	}
	
	private void left(java.awt.event.ActionEvent evt) {
		targetRV += RV_INC;
		try {
			callComponent("setRotationalVelocity", targetRV);
		} catch (Exception e) {
		}
	}
	
	private void right(java.awt.event.ActionEvent evt) {
		targetRV -= RV_INC;
		try {
			callComponent("setRotationalVelocity", targetRV);
		} catch (Exception e) {
		}
	}
	
	private void forward(java.awt.event.ActionEvent evt) {
		targetTV += TV_INC;
		try {
			callComponent("setTranslationalVelocity", targetTV);
		} catch (Exception e) {
		}
	}
	
	private void back(java.awt.event.ActionEvent evt) {
		targetTV -= TV_INC;
		try {
			callComponent("setTranslationalVelocity", targetTV);
		} catch (Exception e) {
		}
	}
	
	private void stop(java.awt.event.ActionEvent evt) {
		try {
			callComponent("setVelocities", new int[]{0,0});
			targetTV = 0;
			targetRV = 0;
		} catch (Exception e) {
		}
	}
	
	/** This method is called from within the constructor to
	 * initialize the form. */
   private void initComponents() {
      jButton1 = new javax.swing.JButton();
      jButton2 = new javax.swing.JButton();
      jButton3 = new javax.swing.JButton();
      jButton4 = new javax.swing.JButton();
      jButton5 = new javax.swing.JButton();
      jTextField1 = new javax.swing.JTextField();
      jTextField2 = new javax.swing.JTextField();
      jLabel1 = new javax.swing.JLabel();

      jButton1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
      jButton1.setDefaultCapable(false);
      jButton1.setDoubleBuffered(true);
      jButton1.setFocusPainted(false);
      jButton1.setFocusable(false);
      jButton1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
      jButton1.setLabel("forward");
      jButton1.setName("forwardButton");
      jButton1.setRequestFocusEnabled(false);
      jButton1.setRolloverEnabled(false);
      jButton1.setVerifyInputWhenFocusTarget(false);
      jButton1.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            forward(evt);
         }
      });

      jButton2.setText("back");
      jButton2.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
      jButton2.setDefaultCapable(false);
      jButton2.setDoubleBuffered(true);
      jButton2.setFocusPainted(false);
      jButton2.setFocusable(false);
      jButton2.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
      jButton2.setMaximumSize(new java.awt.Dimension(53, 19));
      jButton2.setMinimumSize(new java.awt.Dimension(53, 19));
      jButton2.setName("backButton");
      jButton2.setPreferredSize(new java.awt.Dimension(53, 19));
      jButton2.setRequestFocusEnabled(false);
      jButton2.setRolloverEnabled(false);
      jButton2.setVerifyInputWhenFocusTarget(false);
      jButton2.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            back(evt);
         }
      });

      //jButton2.getAccessibleContext().setAccessibleName("stop");

      jButton3.setText("right");
      jButton3.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
      jButton3.setDefaultCapable(false);
      jButton3.setDoubleBuffered(true);
      jButton3.setFocusPainted(false);
      jButton3.setFocusable(false);
      jButton3.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
      jButton3.setMaximumSize(new java.awt.Dimension(53, 19));
      jButton3.setMinimumSize(new java.awt.Dimension(53, 19));
      jButton3.setName("rightButton");
      jButton3.setPreferredSize(new java.awt.Dimension(53, 19));
      jButton3.setRequestFocusEnabled(false);
      jButton3.setRolloverEnabled(false);
      jButton3.setVerifyInputWhenFocusTarget(false);
      jButton3.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            right(evt);
         }
      });

      jButton4.setText("left");
      jButton4.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
      jButton4.setDefaultCapable(false);
      jButton4.setDoubleBuffered(true);
      jButton4.setFocusPainted(false);
      jButton4.setFocusable(false);
      jButton4.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
      jButton4.setMaximumSize(new java.awt.Dimension(53, 19));
      jButton4.setMinimumSize(new java.awt.Dimension(53, 19));
      jButton4.setName("leftButton");
      jButton4.setPreferredSize(new java.awt.Dimension(53, 19));
      jButton4.setRequestFocusEnabled(false);
      jButton4.setRolloverEnabled(false);
      jButton4.setVerifyInputWhenFocusTarget(false);
      jButton4.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            left(evt);
         }
      });

      jButton5.setBackground(new java.awt.Color(255, 0, 0));
      jButton5.setForeground(new java.awt.Color(255, 255, 255));
      jButton5.setText("STOP!");
      jButton5.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED, null, java.awt.Color.white, null, null));
      jButton5.setDefaultCapable(false);
      jButton5.setDoubleBuffered(true);
      jButton5.setFocusPainted(false);
      jButton5.setFocusable(false);
      jButton5.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
      jButton5.setRequestFocusEnabled(false);
      jButton5.setRolloverEnabled(false);
      jButton5.setVerifyInputWhenFocusTarget(false);
      jButton5.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            stop(evt);
         }
      });

      jTextField1.setColumns(3);
      jTextField1.setEditable(false);
      jTextField1.setFont(new java.awt.Font("Dialog", 1, 12));
      jTextField1.setHorizontalAlignment(javax.swing.JTextField.CENTER);
      jTextField1.setText("- - -");
      jTextField1.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Right", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.ABOVE_TOP));
      jTextField1.setDisabledTextColor(new java.awt.Color(51, 51, 51));
      jTextField1.setDoubleBuffered(true);
      jTextField1.setFocusable(false);
      jTextField1.setMaximumSize(new java.awt.Dimension(36, 19));
      jTextField1.setMinimumSize(new java.awt.Dimension(36, 19));
      jTextField1.setName("rVel");
      jTextField1.setRequestFocusEnabled(false);

      jTextField2.setColumns(3);
      jTextField2.setEditable(false);
      jTextField2.setFont(new java.awt.Font("Dialog", 1, 12));
      jTextField2.setHorizontalAlignment(javax.swing.JTextField.CENTER);
      jTextField2.setText("- - -");
      jTextField2.setAutoscrolls(false);
      jTextField2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Left", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.ABOVE_TOP));
      jTextField2.setDoubleBuffered(true);
      jTextField2.setFocusable(false);
      jTextField2.setMaximumSize(new java.awt.Dimension(40, 19));
      jTextField2.setMinimumSize(new java.awt.Dimension(40, 19));
      jTextField2.setName("lVel");
      jTextField2.setRequestFocusEnabled(false);

      jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
      jLabel1.setText("Velocities");
      jLabel1.setFocusable(false);
      jLabel1.setInheritsPopupMenu(false);
      jLabel1.setRequestFocusEnabled(false);
      jLabel1.setVerifyInputWhenFocusTarget(false);
	}

	@Override
	public void refreshGui() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
