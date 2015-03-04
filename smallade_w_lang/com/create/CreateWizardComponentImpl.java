/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * VidereWizardComponentImpl.java
 *
 * @author Andrew McGlathery and Gordon Briggs
 */
package com.create;

import static utilities.Util.Sleep;

import java.rmi.RemoteException;
import java.util.Random;

import javax.swing.*;
import java.awt.event.*;

import ade.ADEComponentImpl;
import ade.gui.ADEGuiVisualizationSpecs;

/** <code>VidereWizardComponentImpl</code>.  This is server used to wizard the nao
 * during experiments, if need be.
 */
public class CreateWizardComponentImpl extends CreateComponentImpl implements CreateWizardComponent {
    private static final long serialVersionUID = 1L;

    /** a prefix used to distinguish log entries that will be parsed 
     * by the updateFromLog(String logEntry) method. */
    private static final String STATE_LOG_INDICATOR = "COUNTER: ";
	
    // arguments that are populated by parseadditionalargs MUST
    //    be STATIC, otherwise setting them will have no effect.
    private static boolean verbose = false;

    private int currentCounter;
    private boolean alreadyAlertedUserOfWaiting = false;

    private enum Movement {STOP, LEFT, RIGHT, FORWARD, SLEFT, SRIGHT, SFORWARD, CRY};
    private static Movement currentCommand = Movement.STOP;

    private enum Head {CENTER, LEFT, RIGHT};
    private static Head headState = Head.CENTER;
    
    private static class NaoControls extends JPanel implements ActionListener {
      protected JButton stop, left, right, forward, headCenter, headLeft, headRight, sLeft, sRight, sForward;
      protected JButton cryButton;

      private boolean stopped = true;
      private boolean sat     = false;
      private boolean cry     = false;

      public NaoControls() {

         stop = new JButton("Stop");
         stop.setActionCommand("stop");

         left = new JButton("Left");
         left.setActionCommand("left");

         right = new JButton("Right");
         right.setActionCommand("right");

         forward = new JButton("Forward");
         forward.setActionCommand("forward");

	 sLeft = new JButton("Slow Left");
	 sLeft.setActionCommand("sLeft");
	
	 sRight = new JButton("Slow Right");
	 sRight.setActionCommand("sRight");

	 sForward = new JButton("Slow Forward");
	 sForward.setActionCommand("sForward");

	 headCenter = new JButton("Center Head");
	 headCenter.setActionCommand("headCenter");

	 headLeft = new JButton("Turn Head Left");
	 headLeft.setActionCommand("headLeft");

	 headRight = new JButton("Turn Head Right");
	 headRight.setActionCommand("headRight");

	 cryButton = new JButton("CRY");
	 cryButton.setActionCommand("cry");

         stop.addActionListener(this);
         left.addActionListener(this);
         right.addActionListener(this);
         forward.addActionListener(this);
	 sLeft.addActionListener(this);
	 sRight.addActionListener(this);
	 sForward.addActionListener(this);
	 headCenter.addActionListener(this);
	 headLeft.addActionListener(this);
	 headRight.addActionListener(this);
	 cryButton.addActionListener(this);

         add(stop);
         add(left);
         add(right);
         add(forward);
	 add(sLeft);
	 add(sRight);
	 add(sForward);
	 add(headCenter);
	 add(headLeft);
	 add(headRight);
	 add(cryButton);
      }

      public void actionPerformed(ActionEvent e) {
				 if("cry".equals(e.getActionCommand())) {
							 cry = true;
							 stopped = true;
							 currentCommand = Movement.CRY;
				 } else if (sat) {
            if ("stand".equals(e.getActionCommand())) {
               sat = false;
            }
         } else if (stopped) {
            if ("sit".equals(e.getActionCommand())) {
               sat = true;
            } else if ("left".equals(e.getActionCommand())) {
               stopped = false;
               currentCommand = Movement.LEFT;
            } else if ("right".equals(e.getActionCommand())) {
               stopped = false;
               currentCommand = Movement.RIGHT;
            } else if ("forward".equals(e.getActionCommand())) {
               stopped = false;
               currentCommand = Movement.FORWARD;
            } else if ("sLeft".equals(e.getActionCommand())) {
	       stopped = false;
	       currentCommand = Movement.SLEFT;
	    } else if ("sRight".equals(e.getActionCommand())) {
	       stopped = false;
	       currentCommand = Movement.SRIGHT;
	    } else if ("sForward".equals(e.getActionCommand())) {
	       stopped = false;
	       currentCommand = Movement.SFORWARD;					}
         } else {
            if ("stop".equals(e.getActionCommand())) {
               stopped = true;
               currentCommand = Movement.STOP;
            } else if ("left".equals(e.getActionCommand())) {
               stopped = false;
               currentCommand = Movement.LEFT;
            } else if ("right".equals(e.getActionCommand())) {
               stopped = false;
               currentCommand = Movement.RIGHT;
            } else if ("forward".equals(e.getActionCommand())) {
               stopped = false;
               currentCommand = Movement.FORWARD;
            } else if ("sLeft".equals(e.getActionCommand())) {
	       stopped = false;
	       currentCommand = Movement.SLEFT;
	    } else if ("sRight".equals(e.getActionCommand())) {
	       stopped = false;
	       currentCommand = Movement.SRIGHT;
	    } else if ("sForward".equals(e.getActionCommand())) {
	       stopped = false;
	       currentCommand = Movement.SFORWARD;
            }
         }
				 if ("headCenter".equals(e.getActionCommand())) {
						headState = Head.CENTER;
				 } else if ("headLeft".equals(e.getActionCommand())) {
						headState = Head.LEFT;
				 } else if ("headRight".equals(e.getActionCommand())) {
						headState = Head.RIGHT;
				 }
       }
    }
    
    // ********************************************************************
    // *** Local methods
    // ********************************************************************
    
   private static void createAndShowGUI() {
      JFrame frame = new JFrame("CreateWizardComponent");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      NaoControls newContentPane = new NaoControls();
      newContentPane.setOpaque(true);
      frame.setContentPane(newContentPane);

      frame.pack();
      frame.setVisible(true);
   }
  
    /** 
     * VidereWizardComponentImpl constructor.
     */
    public CreateWizardComponentImpl() throws RemoteException {
        super();
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
               createAndShowGUI();
            }
         });
    }
    
    /** update the server once */
    @Override
    protected void updateComponent() {
        switch(currentCommand) {
          case FORWARD:
             setVels(50, 0.0);
             break;
          case LEFT:
             setVels(0.0, -50);
             break;
          case RIGHT:
             setVels(0.0, 50);
             break;
          case STOP:
             setVels(0.0, 0.0);
             break;
   	  case SFORWARD:
             setVels(20, 0.0);
	     break;
	  case SLEFT:
             setVels(0.0, -10);
	     break;
	  case SRIGHT:
             setVels(0.0, 10);
	     break;
	  case CRY:
             setVels(0.0, 0.0);
	     break;
      }
   }
    
    /** common method, used both by updateComponent and updateFromLog */
    private void processCounterInfo() {
    	System.out.println(getNameFromID(myID) + ":  " + 
    			"My counter = " + this.currentCounter);
	}
}
