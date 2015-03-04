/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * SimSpeechRecognitionComponent.java
 *
 * @author Paul Schermerhorn
 */
package com.simspeech;

import ade.gui.ADEGuiVisualizationSpecs;
import ade.ADEException;
import ade.*;
import java.awt.Color;
import java.io.*;
import java.rmi.*;
import java.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static utilities.Util.*;

/**
 * <code>SimSpeech</code> provides a keyboard interface to replace speech
 * recognition.
 */
public class SimSpeechRecognitionComponentImpl extends ADEComponentImpl implements SimSpeechRecognitionComponent {
    private static String prg = "SimSpeechRecognitionComponentImpl";
    private Log log = LogFactory.getLog(SimSpeechRecognitionComponentImpl.class);
    protected BufferedReader sbr;
    private Reader reader;
    private static String SConfig = "com/simspeech/config/default";
    public static boolean useUnk = false;
    public static boolean useCommand = true;
    public static boolean toLower = false;
    public static Color textColor = Color.BLACK;
    private static boolean doLogging = false;
    private String input = null; // input from GUI visualizer, typically
    private String output = null; // output saved for getText
    private final Object syncObj = new Object();
    private static boolean autoInput = false; //if should automatically process utterances from file
    private static int repetitions = 1;  //# times to repeat each utterance during read-from-file mode

    // discourse
    protected boolean gotDiscourse = false;
    private static String nlpType = "com.interfaces.NLPComponent";
    private static String nlpName = null;
    public Object nlp;
    public static boolean addByUtterance = false;
    public static boolean dsoutput = false;
    
    	/**
	 * Do not call.  Public so that the registry can get at it.
	 * @param newserverkey
	 */
	@Override
	public void notifyComponentJoined(final String newserverkey) {
          log.debug("notifyComponentJoined: " + newserverkey);
          final String componentType = getTypeFromID(newserverkey);
          final String componentName = getNameFromID(newserverkey);

          new Thread() {
            @Override
            public void run() {
              if (componentType.equals(nlpType)) {
                //actual connection is done in componentConnectReact
                if (nlpName == null) {
                  getClient(nlpType, 0);
                } else if (componentName.equals(nlpName)) {
                  getClient(nlpType, nlpName, 0);
                }
              }
            }
          }.start();
	}

    // ** abstract methods in ADEComponentImpl that need to be implemented **/

    /**
     * This method will be activated whenever the heartbeat returns a
     * remote exception (i.e., the server this is sending a
     * heartbeat to has failed).
     */
    @Override
    protected void componentDownReact(String componentkey, String[][] constraints) {
      log.debug("reacting to down " + componentkey);
      String type = getTypeFromID(componentkey);
      if (nlp == null && nlpType.equals(type)) {
        nlp = ref;
        gotDiscourse = false;
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
    protected void componentConnectReact(String componentkey, Object ref, String[][] constraints) {
        log.debug("reacting to connecting " + componentkey);
        String type = getTypeFromID(componentkey);
        if (nlp == null && nlpType.equals(type)) {
            nlp = ref;
            gotDiscourse = true;
        }
    }
    
        /**
     * This method will be activated whenever a client calls the
     * requestConnection(uid) method. Any connection-specific initialization
     * should be included here, either general or user-specific.
     */
    @Override
    protected void clientConnectReact(String user) {
        log.debug("got connection from " + user + "!");
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
        log.debug("lost connection with " + user + "!");
        return false;
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
        log.debug("shutting down...");
        log.debug("closing buffered reader...");
        try {
            sbr.close();
        } catch (IOException ioe) {
            log.error("Error closing BufferedReader", ioe);
        }
        if (doLogging) {
            try {
                setADEComponentLogging(false);
            } catch (Exception e) {
                log.error("setASL", e);
            }
        }
        log.debug("done.");
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
	System.out.println("GetConfigFile");
        return SConfig;
    }

    /**
     * Get the flags relevant to visualization (mostly useful for the visualizer).
     * Examples include whether to display the command box/button and the "unk" button.
     * @return an array of boolean flags
     */
    @Override
    public boolean[] getVisFlags() {
        return new boolean[]{useCommand, useUnk, toLower};
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
            log.error("error getting input", ioe);
        }
        return returnVal;
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
	    System.out.println("METHOD: Reader in SimSpeechTCI");
          try {
            while (!gotDiscourse || !(Boolean)call(nlp, "servicesReady", myID)) {
                log.debug("Waiting on NLP connection...");
                Thread.sleep(100);
            }
          } catch (Exception e) {
            log.error("Reader waiting on nlp services", e);
          }
         
	  System.out.println("aa");
            ArrayList<String> result;
            StringTokenizer strTok;
            String prompt = "> ";
	    System.out.println("bb");
            while (shouldRead) {
		System.out.println("cc");
                String in = null;
                if (sbr != null) {
                    if (prompt != null)
                        System.out.print(prompt);
                    in = getUserInput();
		    System.out.println("HALLO INPUT");
		    System.out.println("in is "+in);
                    log.trace(String.format("input: %s", in));
                }
		System.out.println("dd");
                synchronized (syncObj) {
                    if (in == null) { // no terminal/file input
                        // grab what might have come in from the gui and reset that
                        in = input;
			System.out.println("HALLO INPUT2");
                        input = null;
			System.out.println("ff");
                    } else {
			System.out.println("HALLO INPUT3");
                        canLogIt("Text " + in);
			System.out.println("gg");
                    }
                    if (in != null) {
                        // new input, make it available to getText
			System.out.println("HALLO INPUT4");
			System.out.println("in is: "+in);
                        output = in;
                        prompt = "> ";
                    } else {
                        prompt = null;
                    }
                }
                if (gotDiscourse && in != null) {
                    log.trace(String.format("sending \"%s\" to Discourse.", in));
		    System.out.println("hh");
                    //System.out.println("dso?"+dsoutput);
                    if(dsoutput){
                        ArrayList<String> tokens = new ArrayList<>();
			System.out.println("ii");
                        ArrayList<Double> means = new ArrayList<>();
                        ArrayList<Double> sds = new ArrayList<>();
                        ArrayList<Double> oneModelScores = new ArrayList<>();
                        ArrayList<Double> twoModelScores = new ArrayList<>();
                        ArrayList<ArrayList<Double>> scores = new ArrayList<>();
                        
                        tokens.add(in);
                        tokens.add("NO NO NO!");
                        means.add(1.489);
                        means.add(1.187);
                        sds.add(0.201);
                        sds.add(0.196);

                        //RC1
                        // oneModelScores.add(1.9);
                        // oneModelScores.add(1.2);
                        // twoModelScores.add(1.8);
                        // twoModelScores.add(1.0);

                        //RC2
                        oneModelScores.add(1.8);
                        oneModelScores.add(1.489);
                        twoModelScores.add(1.4);
                        twoModelScores.add(1.187);
                        scores.add(oneModelScores);
                        scores.add(twoModelScores);

                        //RC3
                        // oneModelScores.add(1.990);
                        // oneModelScores.add(1.888);
                        // twoModelScores.add(1.66);
                        // twoModelScores.add(1.66);
                        // scores.add(oneModelScores);
                        // scores.add(twoModelScores);


                        log.debug("in dso");
			System.out.println("jj");
                        try{
			    System.out.println("kk");
                            call(nlp,"addSentenceToken",tokens,means,sds,scores);
                        } catch (ADEException ace){
                            log.error("couldn't send token to DSNLP");
                            ace.printStackTrace();
                        }
                    }else if (addByUtterance) {
                        try {
			    log.debug("ADDING UTTERANCE!");
                            call(nlp, "addUtterance", in);
                        } catch (ADEException ace) {
                            log.error("adding utterance", ace);
                        }
                    } else {
			System.out.println("ll");
                        result = new ArrayList<String>();
                        ArrayList<String> newresult = new ArrayList<String>();
			strTok = new StringTokenizer(in);
			while (strTok.hasMoreTokens()) {
			    String str = strTok.nextToken();
			    log.trace(String.format("addword: %s", str));
			    System.out.println("mm");
			    result.add(str);
			}
			result.add("0");
			/*simulate the input from recognizer*/
			System.out.println("SimSpeechRecognitionComponentImpl");
			for (int count=0; count<repetitions; count++) {
			    newresult = new ArrayList<String>();
			    for (int i = 0; i < result.size(); i++) {
				System.out.println("result is: "+result);
				newresult.add(result.get(i));
				try {
				    System.out.println("nn");
				    //HACK: EAK: infinite timeout
                                    log.debug("Calling nlp.");
				    System.out.println("----newresult: " +newresult); 
			       
				    call(0, nlp, "addWords", newresult);
				} catch (ADEException ace) {
				    log.error("callling addWords", ace);
				}
				Sleep(100); //between words
			    }
                          Sleep(1000);  //between repetitions
			}
                    }
                }
                Sleep(200);
            }
	    System.out.println("oo");
            log.trace("Exiting Reader thread...");
        }

        public void halt() {
            shouldRead = false;
        }
    }

    /** This server is ready as soon as it is initialized */
    @Override
    protected boolean localServicesReady() {
        return gotDiscourse;
    }

    /**
     * Provide additional information for usage...
     */
    @Override
    protected String additionalUsageInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Component-specific options:\n\n");
        sb.append("  -config <file>     <load buttons from <file>>\n");
        sb.append("  -nlp <type> <name>	<use specified NLP server>\n");
        sb.append("  -utterance 	<use NLP \"addUtterance\">\n");
        sb.append("  -dsoutput 	        <use DSNLP \"addSentenceToken\">\n");
        sb.append("  -nocommand         <do not include command entry field>\n");
        sb.append("  -unk               <include unk button>\n");
        sb.append("  -log               <use logging>\n");
        sb.append("  -textred           <red button text>\n");
        sb.append("  -textblue          <blue button text>\n");
        sb.append("  -autoInput         <automatically run utterances from file>\n");
        sb.append("  -reps              <# of times to repeat each utterance>\n");
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
            } else if (args[i].equalsIgnoreCase("-nlp")) {
                if (! checkNextArg(args, i)) {
                    nlpType = args[++i];
                    if (! checkNextArg(args, i)) {
                        nlpName = args[++i];
                    }
                }
            } else if (args[i].equalsIgnoreCase("-utterance")) {
                addByUtterance = true;
            } else if (args[i].equalsIgnoreCase("-nocommand")) {
                useCommand = false;
            } else if (args[i].equalsIgnoreCase("-unk")) {
                useUnk = true;
            } else if (args[i].equalsIgnoreCase("-log")) {
                // SimSpeech can start and stop logs itself if desired
                doLogging = true;
            } else if (args[i].equalsIgnoreCase("-textred")) {
                textColor = Color.RED;
            } else if (args[i].equalsIgnoreCase("-textblue")) {
                textColor = Color.BLUE;
            } else if (args[i].equalsIgnoreCase("-dsoutput")){
                dsoutput = true;
            } else if (args[i].equalsIgnoreCase("-auto") || args[i].equalsIgnoreCase("-autoInput")) {
                autoInput = true;
            } else if (args[i].equalsIgnoreCase("-reps") || args[i].equalsIgnoreCase("-repetitions")) {
                repetitions = Integer.parseInt(args[++i]);
            } else {
                log.error("Unrecognized argument: " + args[i]);
                return false;  // return on any unrecognized args
            }
        }
        return true;
    }

    @Override
    public ADEGuiVisualizationSpecs getVisualizationSpecs() throws RemoteException {
    	ADEGuiVisualizationSpecs specs = super.getVisualizationSpecs();
        specs.add("Speech Input", SimSpeechRecognitionComponentVis.class);
        return specs;
    }

    @Override
    protected void updateComponent() {
    }

    @Override
    protected void updateFromLog(String logEntry) {
    }

    /**
     * Constructs the SimSpeechRecognitionComponent
     */
    public SimSpeechRecognitionComponentImpl() throws RemoteException {
        super();
        
        try {
          if (autoInput) {
            sbr = new BufferedReader(new FileReader(SConfig));
	    System.out.println("BufferedReader in SimSpechReC");
          } else {
            sbr = new BufferedReader(new InputStreamReader(System.in));
	    System.out.println("BufferedReader in SimSpechReC2");
          }
        } catch (Exception e) {
          log.fatal("Error getting input stream. Shutting down!", e);
          System.exit(-1);
        }
        
        if (doLogging) {
            try {
                setADEComponentLogging(true);
            } catch (Exception e) {
                log.error("setADEComponentLogging", e);
            }
        }
        
        //EAK: register for all notifications since individual registrations will 
        //overwrite previous registrations. filtering is done in notify method.
        registerNewComponentNotification(new String[][]{{}}, true);
        if (nlpName == null) {
            getClient(nlpType);
        } else {
            getClient(nlpType, nlpName);
        }
        
        reader = new Reader();
        log.trace("Starting Reader");
        reader.start();
    }
}
