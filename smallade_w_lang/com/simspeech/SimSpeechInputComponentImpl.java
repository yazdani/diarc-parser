/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * SimSpeechInputComponent.java
 *
 * @author Paul Schermerhorn
 */
package com.simspeech;

import ade.gui.ADEGuiVisualizationSpecs;
import ade.ADEException;
import ade.*;
import java.awt.Color;
import java.io.*;
import java.lang.reflect.Array;
import java.rmi.*;
import java.util.*;
import javax.sound.sampled.*;

//import text.util.FileIO;

import static utilities.Util.*;

/**
 * <code>SimSpeech</code> provides a keyboard interface to replace speech
 * recognition.
 */
public class SimSpeechInputComponentImpl extends ADEComponentImpl implements SimSpeechInputComponent {

    protected BufferedReader sbr;
    private boolean verbose = false;
    private Reader r;
    private static String SConfig = null;
    public static Color textColor = Color.BLACK;
    private static boolean doLogging = false;
    private String input = null; // input from GUI visualizer, typically
    private String output = null;
    private final Object syncObj = new Object();

    // discourse
    protected boolean gotSR = false;
    private static String srType = "com.nsim.speechrec.nsimSpeechrecComponent";
    private static String srName = null;
    public Object sr;
    //public FileIO file=null;

    private static boolean playAudio = true;
    protected static long sleepDec = 25;
    protected static int outDev = 0;
    protected SourceDataLine outLine = null;

    // ** abstract methods in ADEComponentImpl that need to be implemented **/
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

        System.out.println(myID + ": reacting to down " + s + "...");
        if (s.indexOf(srType) >= 0) {
            gotSR = false;
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

        System.out.println(myID + ": reacting to connecting " + s + " server...");
        if (s.indexOf(srType) >= 0) {
            gotSR = true;
        }
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
        System.out.println(myID + " shutting down...");
        System.out.println("closing buffered reader...");
        try {
            sbr.close();
        } catch (IOException ioe) {
            System.err.println("Error closing BufferedReader: " + ioe);
        }
        if (doLogging) {
            try {
                setADEComponentLogging(false);
            } catch (Exception e) {
                System.out.println("setASL: " + e);
            }
        }
        System.out.println("done.");
    }

    // ********************************************************************
    // The methods available to remote objects via RMI
    // ********************************************************************

    /** Gets the most recently recognized text.
     * @return The most recently recognized text */
    @Override
    public String getText() {
        String ret;
        synchronized (syncObj) {
            ret = output;
            output = null;
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
     * Get the configuration file for input buttons (mostly useful for the visualizer).
     * @return the config file to use
     */
    @Override
    public String getConfigFile() {
        return SConfig;
    }

    /**
     * Get the flags relevant to visualization (mostly useful for the visualizer).
     * Examples include whether to display the command box/button and the "unk" button.
     * @return an array of boolean flags
     */
    @Override
    public boolean[] getVisFlags() {
        return new boolean[]{false, false, false};
    }

    /**
     * Set input text (mostly useful for the visualizer).
     * @param in the new speech input
     */
    @Override
    public void setText(String in) {
        synchronized (syncObj) {
            input = in;
        }
    }

    /**
     * Send text to be logged (mostly useful for the visualizer).
     * @param log the text to be logged
     */
    @Override
    public void logText(String log) {
        canLogIt(log);
    }

    // ********************************************************************
    // *** SimSpeech methods
    // ********************************************************************
    /**
     * Get user input from terminal.
     */
    public String getUserInput() {
        String returnVal = null;
        try {
            if (sbr.ready()) {
                returnVal = sbr.readLine();
            }
        } catch (IOException ioe) {
            System.err.println(myID+": error getting input: "+ioe);
        }
        return returnVal;
    }

    /**
     * Play the wav file.
     */
    public void play(File speechFile) {
        // Open the generated wav file
        AudioInputStream ais = null;
        try {
            ais = AudioSystem.getAudioInputStream(speechFile);
        } catch (Exception e) {
            System.err.println("Error opening file: " + e);
            return;
        }
        // Play it and generate commands to move lips
        int bytes = 0;
        int defBytes = 3200;
        long defSleep = 1000 * defBytes / 32000 - sleepDec;
        byte[] data = new byte[defBytes];
        System.out.print("Playing audio...");
        while (bytes >= 0) {
            try {
                bytes = ais.read(data, 0, data.length);
            } catch (IOException ioe) {
                System.err.println("Error reading file: " + ioe);
            }
            if (bytes > 0) {
                Sleep(defSleep);
                bytes = outLine.write(data, 0, bytes);
            }
        }
        // Flush help avoid clipping at the end of the utterance
        outLine.flush();
        try {
            ais.close();
        } catch (IOException ioe) {
        }
        System.out.println("done");
    }

    /**
     * The <code>Reader</code> is the main loop for the
     * server when in non-gui mode.
     */
    private class Reader extends Thread {

        boolean shouldRead;

        public Reader() {
            shouldRead = true;
        }

        @Override
        public void run() {
            ArrayList<String> result;
            StringTokenizer strTok;
            String prompt = "> ";
            while (! gotSR) {
                if (srName == null)
                    sr = getClient(srType);
                else
                    sr = getClient(srType, srName);
                Sleep(1000);
            }
            while (shouldRead) {
                String in = null;
                if (sbr != null) {
                    if (prompt != null)
                        System.out.print(prompt);
                    in = getUserInput();
                }
                synchronized (syncObj) {
                    if (in == null) { // no terminal input
                        // grab what might have come in from the gui and reset that
                        in = input;
                        input = null;
                    }
                    if (in != null) {
                        // new input, make it available to getText
                        output = in;
                        prompt = "> ";
                    } else {
                        prompt = null;
                    }
                }
                if (gotSR && in != null) {
                    //System.out.println(myID + ": sending \"" + in + "\" to SR.");
                    final String nin = in;
                    new Thread() {
                        @Override
                        public void run() {
                            if (playAudio) {
                                play(new File(nin));
                            }
                        }
                    }.start();
                    try {
                        call(0,sr, "speechInputFromFile", in);
                    } catch (ADEException ace) {
                        System.out.println(myID + ": " + ace);
			ace.printStackTrace();
                    }
                }
                Sleep(200);
            }
            System.out.println(myID + ": Exiting Reader thread...");
        }

        public void halt() {
            shouldRead = false;
        }
    }

    /** This server is ready as soon as it is initialized */
    @Override
    protected boolean localServicesReady() {
        return gotSR;
    }

    /**
     * Provide additional information for usage...
     */
    @Override
    protected String additionalUsageInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Component-specific options:\n\n");
        sb.append("  -config <file>     <load buttons from <file>>\n");
        sb.append("  -sr <type> <name>	<use specified NLP server>\n");
        sb.append("  -log               <use logging>\n");
        sb.append("  -textred           <red button text>\n");
        sb.append("  -textblue          <blue button text>\n");
        sb.append("  -quiet             <suppress wav file playing>\n");
        sb.append("  -outdev <n>        <use device n for audio out>\n");
        sb.append("  -sleepdec <n>      <set sleepDec to n>\n");
        return sb.toString();
    }

    /**
     * Parse additional command-line arguments
     * @return "true" if parse is successful, "false" otherwise
     */
    @Override
    protected boolean parseadditionalargs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-cfg") ||
                args[i].equalsIgnoreCase("-conf") ||
                args[i].equalsIgnoreCase("-config")) {
                SConfig = args[++i];
		System.out.printf("SimSpeechInput: Will use config file [%s]\n", SConfig);
            } else if (args[i].equalsIgnoreCase("-sr")) {
                if (! checkNextArg(args, i)) {
                    srType = args[++i];
                    if (! checkNextArg(args, i)) {
                        srName = args[++i];
                    }
                }
                System.out.println("Using sr: " + srType + " " + srName);
            } else if (args[i].equalsIgnoreCase("-log")) {
                // SimSpeech can start and stop logs itself if desired
                doLogging = true;
            } else if (args[i].equalsIgnoreCase("-textred")) {
                textColor = Color.RED;
            } else if (args[i].equalsIgnoreCase("-textblue")) {
                textColor = Color.BLUE;
            } else if (args[i].equalsIgnoreCase("-quiet")) {
                playAudio = false;
            } else if (args[i].equalsIgnoreCase("-outdev")) {
                int dev;
                try {
                    dev = Integer.parseInt(args[i + 1]);
                    i++;
                    outDev = dev;
                } catch (NumberFormatException nfe) {
                    System.err.println(myID + ": outdev " + args[i + 1]);
                    System.err.println(myID + ": " + nfe);
                    System.err.println(myID + ": default outDev is " + outDev);
                }
            } else if (args[i].equalsIgnoreCase("-sleepdec")) {
                long dec;
                try {
                    dec = Long.parseLong(args[i + 1]);
                    i++;
                    sleepDec = dec;
                } catch (NumberFormatException nfe) {
                    System.err.println(myID + ": sleepdec " + args[i + 1]);
                    System.err.println(myID + ": " + nfe);
                    System.err.println(myID + ": default sleepDec is " + sleepDec);
                }
            } else {
                System.out.println("Unrecognized argument: " + args[i]);
                return false;  // return on any unrecognized args
            }
        }
        return true;
    }

    @Override
    public ADEGuiVisualizationSpecs getVisualizationSpecs() throws RemoteException {
    	ADEGuiVisualizationSpecs specs = super.getVisualizationSpecs();
        if (SConfig != null) {
            specs.add("Speech Input", SimSpeechRecognitionComponentVis.class);
        }
        return specs;
    }

    @Override
    protected void updateComponent() {
    }

    @Override
    protected void updateFromLog(String logEntry) {
    }

    /**
     * Constructs the SimSpeechInputComponent
     */
    public SimSpeechInputComponentImpl() throws RemoteException {
        super();

        ADEComponentInfo asi = requestComponentInfo(this);
        if (! asi.guiRequested) {
            try {
                sbr = new BufferedReader(new InputStreamReader(System.in));
            } catch (Exception e) {
                System.err.println("Error getting input stream:");
                System.err.println(e);
                System.exit(-1);
            }
        }
        if (playAudio) {
            AudioFormat audioFormat = new AudioFormat(16000, 16, 1, true, false);
            Mixer.Info[] mixInf = AudioSystem.getMixerInfo();
            System.out.println("Using output device: " + outDev);
            System.out.println("*MIXER INFO*: Use -outdev params to select from:");
            for (int i = 0; i < Array.getLength(mixInf); i++) {
                System.out.println("*****MIXER " + i + " INFO*****: " + mixInf[i].toString());
            }
            Mixer myMixer = AudioSystem.getMixer(mixInf[outDev]);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
            if (!AudioSystem.isLineSupported(info)) {
                System.err.println(myID + ": error, line is not supported by AudioSystem");
            }
            try {
                outLine = (SourceDataLine) myMixer.getLine(info);
                outLine.open(audioFormat);
            } catch (LineUnavailableException lue) {
                System.err.println(myID + ": error opening line: " + lue);
            }
            outLine.start();
        }
        if (verbose) {
            System.out.println("Attempting to get Discourse client...");
        }
        if (srName == null)
            sr = getClient(srType);
        else
            sr = getClient(srType, srName);
        (r = new Reader()).start();
        if (doLogging) {
            try {
                setADEComponentLogging(true);
            } catch (Exception e) {
                System.out.println("setASL: " + e);
            }
        }
    }
}
