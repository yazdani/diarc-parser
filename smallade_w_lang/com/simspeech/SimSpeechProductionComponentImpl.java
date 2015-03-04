/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * SimSpeechProductionComponentImpl.java
 *
 * @author Paul Schermerhorn
 */
package com.simspeech;

import ade.*;
import ade.gui.ADEGuiVisualizationSpecs;
import java.rmi.*;
import static utilities.Util.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/** <code>SimSpeechProductionComponentImpl</code>.  Pops up a box for spoken text
 * instead of playing it to a sound device.
 */
public class SimSpeechProductionComponentImpl extends ADEComponentImpl implements SimSpeechProductionComponent {
    private static String prg = "SimSpeechProductionComponentImpl";
    private static boolean verbose = false;

    /* Component-specific fields */
    Text t;
    public static int tx = 200;
    public static int ty = 150;
    private boolean isSpeaking = false;
    public static Color textColor = Color.BLACK;
    private boolean useGui = false;

    private final Object syncObj = new Object();
    private String text;
    private Long textTs = 0L;

    // ***********************************************************************
    // *** Abstract methods in ADEComponentImpl that need to be implemented
    // ***********************************************************************
    /**
     * This method will be activated whenever a client calls the
     * requestConnection(uid) method. Any connection-specific initialization
     * should be included here, either general or user-specific.
     */
    @Override
    protected void clientConnectReact(String user) {
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
    protected boolean clientDownReact(String user) {
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

        //if (s.indexOf("VelocityComponent") >= 0)
        //    gotVel = false;
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

        //if (s.indexOf("VelocityComponent") >= 0)
        //    gotVel = true;
    }

    /**
     * Adds additional local checks for credentials before allowing a shutdown
     * must return "false" if shutdown is denied, true if permitted
     */
    @Override
    protected boolean localrequestShutdown(Object credentials) {
        return false;
    }

    /**
     * Implements the local shutdown mechanism that derived classes need to
     * implement to cleanly shutdown
     */
    @Override
    protected void localshutdown() {
        System.out.print("\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.print(prg + " shutting down...");
        //finalize();
        Sleep(100);
        System.out.println("done.");
    }

    /** The server is always ready to provide its service after it has come up */
    @Override
    protected boolean localServicesReady() {
        return true;
    }

    // ***********************************************************************
    // Methods available to remote objects via RMI
    // ***********************************************************************
    /**
     * Get the most recent text (if available).
     * @return the text to display, or null
     */
    @Override
    public ArrayList<Object> getText(long ts) {
        ArrayList<Object> ret = null;
        synchronized (syncObj) {
            if (textTs > ts) {
                ret = new ArrayList<Object>();
                ret.add(textTs);
                ret.add(text);
            }
        }
        return ret;
    }

    /**
     * Get the color specified for text (mostly useful for the visualizer).
     * @return the color text should be displayed in
     */
    @Override
    public Color getTextColor() {
        return textColor;
    }

    /**
     * Get the display geometry (mostly useful for the visualizer).
     * @return the x,y screen coordinates for the output frame
     */
    @Override
    public int[] getInitialLoc() {
        return new int[]{tx,ty};
    }

    /**
     * Register/deregister local GUI (mostly useful for the visualizer).  If useGui
     * is false, speech output will be sent to the terminal.
     * @param useGui true if local visualizer present, false otherwise
     */
    @Override
    public void setLocalGui(boolean useGui) {
        this.useGui = useGui;
    }

    /** Sends text to the speech production component. */
    @Override
    public boolean sayText(String text) {
        return sayText(text, true);
    }

    /** Sends text to the speech production component. */
    @Override
    public boolean sayText(String in, boolean wait) {
        canLogIt("Text: " + in);
        System.out.println("Saying Text: "+in);
        isSpeaking = true;
        String[] words = in.split(" ");
        long tmptime = 500 * words.length;
        if (tmptime < 1000) {
            tmptime = 1000;
        } else if (tmptime > 4000) {
            tmptime = 4000;
        }
        final long time = tmptime;
        if (! useGui || verbose) {
            System.out.println(in);
        }
        synchronized (syncObj) {
            textTs = System.currentTimeMillis();
            text = in;
        }
        if (wait) {
            // sleep, then destroy
            Sleep(time);
            if (t != null) {
                t.closeText();
                t = null;
            }
            isSpeaking = false;
        } else {
            new Thread() {
                @Override
                public void run() {
                    // sleep, then destroy
                    Sleep(time);
                    isSpeaking = false;
                }
            }.start();
        }
        return true;
    }

    /** Checks if Festival is producing speech.
     * @return <tt>true</tt> if speech is being produced, <tt>false</tt>
     * otherwise
     * @throws RemoteException if an error occurs */
    @Override
    public boolean isSpeaking() throws RemoteException{
        return isSpeaking;
    }

    /** Stops an ongoing utterance.
     * @return <tt>true</tt> if speech is interrupted, <tt>false</tt>
     * otherwise.
     * @throws RemoteException if an error occurs */
    @Override
    public boolean stopUtterance() throws RemoteException {
        if (t != null) {
            t.closeText();
        }
        return isSpeaking;
    }

    // ********************************************************************
    // *** Local methods
    // ********************************************************************
    private class Text {

        private JFrame IFrame;
        private JPanel ITextPanel;
        private JTextArea IText;
        private double ITextWidth = 40;

        public Text(String text) {
            IFrame = new JFrame(myID);
            //IFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            ITextPanel = new JPanel();

            IText = new JTextArea(text);
            IText.setColumns((int) ITextWidth);
            IText.setFont(IText.getFont().deriveFont(0, 16.0f));
            IText.setLineWrap(true);
            IText.setWrapStyleWord(true);
            IText.setEditable(false);
            IText.setBackground(IFrame.getBackground());
            IText.setForeground(textColor);
            ITextPanel.add(IText);

            IFrame.getContentPane().add(ITextPanel, BorderLayout.PAGE_START);
            IFrame.pack();
            IFrame.setSize(IFrame.getPreferredSize());
            IFrame.setLocation(tx, ty);
            IFrame.setVisible(true);
        }

        public void closeText() {
            IFrame.dispose();
        }
    }

    /**
     * Provide additional information for usage...
     */
    @Override
    protected String additionalUsageInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Component-specific options:\n\n");
        sb.append("  -verbose                  <verbose printing>\n");
        sb.append("  -textred                  <red button text>\n");
        sb.append("  -textblue                 <blue button text>\n");
        sb.append("  -geom x y                 <pop-up location>\n");
        return sb.toString();
    }

    /**
     * Parse additional command-line arguments
     * @return "true" if parse is successful, "false" otherwise
     */
    @Override
    protected boolean parseadditionalargs(String[] args) {
        boolean found = false;
        // Note that class fields set here must be declared static above
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-verbose")) {
                verbose = true;
                found = true;
            } else if (args[i].equalsIgnoreCase("-textred")) {
                textColor = Color.RED;
                found = true;
            } else if (args[i].equalsIgnoreCase("-textblue")) {
                textColor = Color.BLUE;
                found = true;
            } else if (args[i].equalsIgnoreCase("-geom")) {
                int x, y;
                try {
                    x = Integer.parseInt(args[i + 1]);
                    i++;
                    y = Integer.parseInt(args[i + 1]);
                    i++;
                    tx = x;
                    ty = y;
                } catch (NumberFormatException nfe) {
                    System.err.println(prg + ": error parsing geometry " + args[i + 1]);
                    System.err.println(prg + ": " + nfe);
                    System.err.println(prg + ": reverting to default geometry");
                }
                found = true;
            } else {
                System.out.println("Unrecognized argument: " + args[i]);
                return false;  // return on any unrecognized args
            }
        }
        return found;
    }

    @Override
    public ADEGuiVisualizationSpecs getVisualizationSpecs() throws RemoteException {
    	ADEGuiVisualizationSpecs specs = super.getVisualizationSpecs();
        specs.add("Speech Output", SimSpeechProductionComponentVis.class);
        return specs;
    }

    /**
     * Log a message using ADE Component Logging, if possible.  The ASL facility
     * takes care of adding timestamp, etc.
     * @param o the message to be logged
     */
    protected void logItASL(Object o) {
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
    protected void setASL(boolean state) {
        try {
            setADEComponentLogging(state);
        } catch (Exception e) {
            System.out.println("setASL: " + e);
        }
    }

    @Override
    protected void updateComponent() {
    }

    @Override
    protected void updateFromLog(String logEntry) {
    }

    /**
     * SimSpeechProductionComponentImpl constructor.
     */
    public SimSpeechProductionComponentImpl() throws RemoteException {
        super();
    }
}
