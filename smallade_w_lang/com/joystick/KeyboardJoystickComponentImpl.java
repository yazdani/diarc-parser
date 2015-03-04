/**
 * ADE 1.0 Beta
 * (c) copyright HRILab (http://hri.cogs.indiana.edu/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * KeyboardJoystickComponentImpl.java
 *
 * @author Paul Schermerhorn
 */
package com.joystick;

import ade.ADEException;
import ade.*;
import static utilities.Util.*;
import java.rmi.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/** 
 * <code>KeyboardJoystickComponent</code> takes input from the keyboard (arrow
 * keys) and passes on velocity commands to servers implementing the {@link
 * com.interfaces.VelocityComponent VelocityComponent} interface.  The default TV can be
 * increased and decreased using PAGE_UP and PAGE_DOWN keys.
 */
public class KeyboardJoystickComponentImpl extends ADEComponentImpl implements KeyboardJoystickComponent
{
    /* ADE-related fields (pseudo-refs, etc.) */
    private static String prg = "KeyboardJoystickComponentImpl";
    private static String type = "KeyboardJoystickComponent";
    private static boolean verbose = false;

    private static boolean useVel = false;
    private static boolean gotVel = false;
    private static String velVersion = "com.interfaces.VelocityComponent";
    private Object vel;

    /* Joystick-specific fields */
    private JFrame frame;
    private JLabel label;

    private double[] defaultVels;
    private boolean[] arrows = {false, false, false, false};

    private Writer w;

    private boolean shutdown = false;

    // ***********************************************************************
    // *** Abstract methods in ADEComponentImpl that need to be implemented
    // ***********************************************************************

    /**
     * This method will be activated whenever a client calls the
     * requestConnection(uid) method. Any connection-specific initialization
     * should be included here, either general or user-specific.
     */
    @Override
    protected void clientConnectReact(String user)
    {
        System.out.println(myID + ": got connection from " + user + "!");
    }

    /**
     * This method will be activated whenever a client that has called the
     * requestConnection(uid) method fails to update (meaning that the
     * heartbeat signal has not been received by the reaper), allowing both
     * general and user specific reactions to lost connections. If it returns
     * true, the client's connection is removed.
     */
    @Override
    protected boolean clientDownReact(String user)
    {
        System.out.println(myID + ": lost connection with " + user + "!");
        return false;
    }

    /**
     * This method will be activated whenever the heartbeat returns a
     * remote exception (i.e., the server this is sending a
     * heartbeat to has failed). 
     */
    @Override
    protected void componentDownReact(String serverkey, String[][] constraints) {
        String s = constraints[0][1];

        if (s.indexOf(velVersion) >= 0) {
            gotVel = false;
        }
    }

    /** This method will be activated whenever the heartbeat reconnects
     * to a client (e.g., the server this is sending a heartbeat to has
     * failed and then recovered). <b>Note:</b> the pseudo-reference will
     * not be set until <b>after</b> this method is executed. To perform
     * operations on the newly (re)acquired reference, you must use the
     * <tt>ref</tt> parameter object.
     * @param s the ID of the {@link ade.ADEComponent ADEComponent} that connected
     * @param ref the pseudo-reference for the requested server */
    @Override
    protected void componentConnectReact(String serverkey, Object ref, String[][] constraints) {
        String s = constraints[0][1];

        if (s.indexOf(velVersion) >= 0) {
            gotVel = true;
        }
    }


    /**
     * Adds additional local checks for credentials before allowing a shutdown
     * must return "false" if shutdown is denied, true if permitted
     */
    @Override
    protected boolean localrequestShutdown(Object credentials)
    {
        return false;
    }

    /**
     * Implements the local shutdown mechanism that derived classes need to
     * implement to cleanly shutdown
     */
    @Override
    protected void localshutdown()
    {
        System.out.print("\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.print(prg + " shutting down...");
        //finalize();
        shutdown = true;
        w.halt();
        Sleep(100);
        if (useVel && gotVel) {
            try {
                call(vel, "setVels", 0.0, 0.0);
            } catch (ADEException ace) {
                System.err.println(prg + ": Error setting vels: " + ace);
            }
        }
        System.out.println("done.");
    }

    /** This server is ready as soon as it is initialized */
    @Override
    protected boolean localServicesReady() {
	return true;
    }

    // ***********************************************************************
    // Methods available to remote objects via RMI
    // ***********************************************************************

    // ********************************************************************
    // *** Local methods
    // ********************************************************************

    /** 
     * KeyboardJoystickComponent constructor.
     */
    public KeyboardJoystickComponentImpl() throws RemoteException
    {
        super();

	setUpdateLoopTime(this,10);

        frame = new JFrame("KeyboardJoystick");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	frame.addKeyListener(this);
	label = new JLabel("tv: 0.0 rv: 0.0");
	label.setBackground(Color.BLUE);
        label.setForeground(Color.WHITE);
	label.setOpaque(true);
	label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setPreferredSize(new Dimension(150, 20));
	frame.getContentPane().add(label);
        frame.pack();
        frame.setLocation(0, 30);
        frame.setVisible(true);
        JFrame.setDefaultLookAndFeelDecorated(true);

        vel = getClient(velVersion);
        while (! gotVel)
            Sleep(100);
        try {
            defaultVels = (double[])call(vel, "getDefaultVels");
	    //            System.out.println("Doubling default vels");
	    //            defaultVels[1] *= 2;
        } catch (ADEException ace) {
            System.err.println(prg + ": Error getting def vels: " + ace);
            defaultVels = new double[2];
        }
        System.out.println("defaultVels: " + defaultVels[0] + " " + defaultVels[1]);
        // Thread to write to VelocityComponent
        w = new Writer();
        w.start();
    }

    /**
     * Provide additional information for usage...
     */
    @Override
    protected String additionalUsageInfo()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Component-specific options:\n\n");
        sb.append("  -verbose                   <verbose debug printing>\n");
        return sb.toString();       
    }

    /** 
     * Parse additional command-line arguments
     * @return "true" if parse is successful, "false" otherwise 
     */
    @Override
    protected boolean parseadditionalargs(String[] args)
    {
        boolean found = false;
        for (int i=0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-verbose")) { 
                verbose = true;
                found = true;
            } else {
                System.out.println("Unrecognized argument: " + args[i]);
                return false;  // return on any unrecognized args
            }
        }
        return found;
    }

    /**
     * Handler for key press events.
     * @param event the keyboard event
     */
    @Override
    public void keyPressed(KeyEvent event) {
	int kchar = event.getKeyCode();
	if (kchar == KeyEvent.VK_UP) {
            arrows[0] = true;
	} else if (kchar == KeyEvent.VK_DOWN) {
            arrows[1] = true;
	} else if (kchar == KeyEvent.VK_LEFT) {
            arrows[2] = true;
	} else if (kchar == KeyEvent.VK_RIGHT) {
            arrows[3] = true;
	} else if (kchar == KeyEvent.VK_PAGE_DOWN) {
            defaultVels[0] *= 0.8;
	} else if (kchar == KeyEvent.VK_PAGE_UP) {
            defaultVels[0] *= 1.25;
	} else if (kchar == KeyEvent.VK_C) {
            if (event.isControlDown()) {
                System.exit(0);
            }
	}
    }

    /**
     * Handler for key release events.
     * @param event the keyboard event
     */
    @Override
    public void keyReleased(KeyEvent event) {
	int kchar = event.getKeyCode();
	if (kchar == KeyEvent.VK_UP) {
            arrows[0] = false;
	} else if (kchar == KeyEvent.VK_DOWN) {
            arrows[1] = false;
	} else if (kchar == KeyEvent.VK_LEFT) {
            arrows[2] = false;
	} else if (kchar == KeyEvent.VK_RIGHT) {
            arrows[3] = false;
	}
    }

    /**
     * Handler for key type events.
     * @param event the keyboard event
     */
    @Override
    public void keyTyped(KeyEvent event) {}

    /**
     * The <code>Writer</code> is the main loop that "interprets" the
     * joystick and sends to Vel if requested.
     */
    private class Writer extends Thread
    {
        private boolean shouldWrite;

        public Writer()
        {
            shouldWrite = true;
        }

        @Override
        public void run()
        {
            int i = 0;
            double tv = 0.0, rv = 0.0;
            double newtv = 0.0, newrv = 0.0;
            while (shouldWrite) {
                Sleep(100);
                if (verbose) System.out.println("Arrows: " + arrows[0] + " " + arrows[1] + " " + arrows[2] + " " + arrows[3]);
                if (arrows[0] && !arrows[1])
                    newtv = defaultVels[0];
                else if (arrows[1] && !arrows[0])
                    newtv = -defaultVels[0];
                else if (!arrows[1] && !arrows[0])
                    newtv = 0.0;
                if (arrows[2] && !arrows[3])
                    newrv = defaultVels[1];
                else if (arrows[3] && !arrows[2])
                    newrv = -defaultVels[1];
                else if (!arrows[3] && !arrows[2])
                    newrv = 0.0;
                if (verbose) System.out.println("tv: " + newtv + " rv: " + newrv);
                if (gotVel && (tv != newtv || rv != newrv)) {
                    tv = newtv;
                    rv = newrv;
                    label.setText("tv: " + tv + " rv: " + rv);
                    try {
                        call(vel, "setVels", tv, rv);
                    } catch (ADEException ace) {
                        System.err.println(prg + ": Error setting vels: " + ace);
                    }
                }
            }
            System.out.println(prg +": Exiting Writer thread...");
        }

        public void halt() 
        {
            System.out.print("halting write thread...");
            shouldWrite = false;
        }
    }

    /**
     * Log a message using ADE Component Logging, if possible.  The ASL facility
     * takes care of adding timestamp, etc.
     * @param o the message to be logged
     */
    protected void logItASL(Object o)
    {
        canLogIt(o);
    }

    /**
     * Set the state of ADE Component Logging.  When true and logging is not
     * started, this starts logging.  When false and logging is started, this
     * stops logging and causes the log files to be written to disk.  ADE server
     * logging is a global logging facility, so starting logging here enables
     * logging in a currently instantiated ADE servers.  Note: You want to stop
     * ADE server logging before quitting, or the files will not be complete.
     * @param state indicates whether to start (true) or stop (false) logging.
     */
    protected void setASL(boolean state)
    {
        try {
            setADEComponentLogging(state);
        } catch (Exception e) {
            System.out.println("setASL: " + e);
        }
    }

    @Override
    protected void updateComponent() {}
    @Override
    protected void updateFromLog(String logEntry) {}
    
    /**
     * <code>main</code> passes the arguments up to the ADEComponentImpl
     * parent.  The parent does some magic and gets the system going.
    public static void main (String[] args) throws Exception 
    {
        ADEComponentImpl.main(args);
    } 		
     */
}

// vi:ai:smarttab:expandtab:ts=8 sw=4
