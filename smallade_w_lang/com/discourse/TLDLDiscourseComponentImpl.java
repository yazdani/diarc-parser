/**
 * ADE 1.0 
 * Copyright 1997-2010 HRILab (http://hrilab.org/) 
 * 
 * All rights reserved.  Do not copy and use without permission. 
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * TLDLDiscourseComponentImpl.java
 *
 * @author Paul Schermerhorn, Juraj Dzifcak
 *
 */
package com.discourse;

import java.io.*;
import java.net.*;
import java.net.InetAddress;
import ade.ADEException;
import ade.*;
import com.discourse.repair.Repairs;
import com.discourse.TL_DL_parser.core.Dictionary;
import com.discourse.TL_DL_parser.core.Tree;
import com.discourse.TL_DL_parser.lambda.Conversions;
import com.discourse.TL_DL_parser.nl_ltl_dl.Parser;
import com.*;

import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import static utilities.Util.*;

/**
 * A TLDLDiscourseComponentImpl object handles natural language interactions
 * with (presumably human) interactors.  It controls both language
 * interpretation and language production in a variety of contexts.
 */
public class TLDLDiscourseComponentImpl extends ADEComponentImpl implements TLDLDiscourseComponent {
    // Members

    public static String prg = "TLDLDiscourseComponent";
    protected static int DEBUG = 0;
    protected static boolean useFestival, useSphinx, useAction, gotAction, localLogging;
    private static String festVersion = "com.festival.FestivalComponent";
    private static String sphinxVersion = "com.sphinx4.Sphinx4Component";
    private static String actionVersion = "com.action.GoalManager";
    private String actionID = null;
    protected Object speaker, listener, actor;
    public static String actorName = "robot";
    public Dictionary dictionary;
    public static String dictFile = "com/discourse/TL_DL_parser/autonomy.dict";
    public static boolean useRepair = false;
    public Parser parser;

    private long goalID = 0L;

    //SOCKETCONNECTION FOR ROS
    Socket socket = null;  
    ServerSocket socket1 = null; 
    ServerSocket socket2 = null; 
    Socket con = null;
    Socket con1 = null;
    PrintWriter out = null;
    PrintWriter out1 = null;
    PrintWriter out2 = null;
    BufferedReader in = null;
    BufferedReader in1 = null;
    BufferedReader in2 = null;
    String message;
    String message1;
    String message2;
    BufferedReader stdIn = null;
    // ********************************************************************
    // *************** Section I: NLPComponent Functions ***************
    // ********************************************************************
    /** Accumulates text, word-at-a-time
     *  @param incoming an arraylist containing the sentence-to-date
     *    this will be matched against discourse's records to determine
     *    changes.  Note that, because TLDLDiscourse is non-incremental,
     *    this method actually falls back on addUtterance once the sentence
     *    is complete.
     *  @return true if sentence is understood. */
    @Override
    public boolean addWords(ArrayList<String> incoming) throws RemoteException {
	//System.out.println("addWords Methods");
        boolean matched = false;
        String word, inString, unnumbered;
        int i = 0;
        canLogIt("INCOMING: " + incoming);
        if (DEBUG > 2) {
            System.out.print("DSI Got word list: ");
            for (String s : incoming) {
                System.out.print(" " + s);
            }
            System.out.println();
        }
        inString = "";
        word = "";

        for (String aWord : incoming) {
            if (!(aWord.equals("0"))) {
                inString += " " + aWord;
		//	System.out.println("inString is: "+ inString);

            } else {
                matched = addUtterance(inString.trim());
            }
        }

        return matched;
    }

    /** Accumulates text, sentence-at-a-time
     *  @return true if sentence is understood. */
    @Override
    public boolean addUtterance(String input) throws RemoteException {


	Predicate p;

        try {
            if (useRepair) {
                if (DEBUG >= 5) System.out.println("Input before repairs: " + input);
                input = Repairs.repair(input).trim();
                if (DEBUG >= 5) System.out.println("Input after repairs: " + input);
            }
	
            String incoming = input;

            List<Tree> trees = parser.ParseLine(input, dictionary);
	    //System.out.println("We are asasashere2");
            if (trees.size() < 1) {
	
		System.out.println("it is smaller");
                return false;
            }
	     

            for (int i = 0; i < trees.size(); i++) {
                Tree t = trees.get(i);
                if (t != null) {
                    //System.out.println("t is "+t);
                    //System.out.println(t.getRoot().getCategory());
                    String s = Conversions.update(t.getRoot().getLambda());
                    String method;
                    // crude means of categorizing commands
                    boolean command = Pattern.matches("[?]actor", s);
                    command = true;
		    //System.out.println("s is "+s);
                    s = s.replaceAll("`", ",");
                    s = s.replaceAll("[?]actor",actorName);
                    String ss = Conversions.update(t.getRoot().getPar_lambda());
                    ss = ss.replaceAll("`", ",");
		    //System.out.println("Sending through socket");
		    //System.out.println("what is ss: " + ss);
		    out.println(ss);
		    // out1.println(s);
                    if (DEBUG >= 5) System.out.println("TL: " + s + " DL: " + ss);
                    System.out.println("TL: " + s + " DL: " + ss);
                    //parseDynamic(ss, script);
                    // For now only doing goals
                    p = parseTemporal(s);
                    if (command) {
                        method = "submitGoal";
                    } else {
                        method = "submitFact";
                    }

                    try {
                        //ActionStatus as = (ActionStatus)call(actor,"goalStatus", goalID);
                        //System.out.println("previous goal status: " + as);
                        // System.out.println("What is actor method and p " +actor+" : "+method+" : "+p);
			
			goalID = (Long)call(actor, method, p);
			System.out.println("What is goalID : "+goalID);
                    } catch (ADEException ace) {
                        // System.err.println(prg + ": " + method + " error for " + p + ": " + ace);
			 System.err.println("");
                        ace.printStackTrace();
                    }
                } else {
                    int j = i + 1;
                    System.out.println("HaHa-Cannot parse sentence number " + j + "!");
		    out.println("Parsing not possible");
		    
                }
            }
            return true;
        } catch (Exception ex) {
            System.out.println("addUtterance caught exception: " + ex);
            ex.printStackTrace();
        }
        return false;
    }

    public Predicate parseTemporal(String s) {
        Predicate p = null;
        p = createPredicate(s);

        return p;
    }

    public Predicate parseDynamic(String ss) {
        Predicate p = null;
        StringTokenizer t = new StringTokenizer(ss, ";");
        while (t.hasMoreElements()) {
            String aspec = t.nextToken();
            String spec = null;
            StringTokenizer s = new StringTokenizer(aspec, "(),");
            String action = s.nextToken();
            if (action.equalsIgnoreCase("go-to")) {
                String dest = s.nextToken();
                if (dest.equals("end-of")) {
                    spec = "traverse " + actorName;
                } else {
                    spec = "navTo " + actorName + " " + dest;
                }
            } else if (action.equals("go")) {
                String arg = s.nextToken();
                spec = "startmove";
                if (!(arg.equalsIgnoreCase("straight") || arg.equalsIgnoreCase("forward"))) {
		    System.out.println("what is spec: "+spec+" and what is arg: "+arg);
                    spec += arg;
                }else if (arg.equalsIgnoreCase("right") || arg.equalsIgnoreCase("left")) {
		    spec = "start" + arg + " " + actorName;
		    System.out.println("what is spec: "+spec+" and what is arg: "+arg+ " what is actorName: " + actorName);
		}
                spec += " " + actorName;
            } else if (action.equals("turn")) {
                String arg = s.nextToken();
                if (arg.equalsIgnoreCase("right") || arg.equalsIgnoreCase("left")) {

                    spec = "start" + arg + " " + actorName;
                } else {
                    spec = "timeTurn " + actorName + " " + arg;
                }
            } else if (action.equals("stop")) {
                spec = "stop " + actorName;
            } else if (action.equals("report")) {
                spec = "report " + actorName + " " + s.nextToken() + " " + s.nextToken();
            } else if (action.equals("timeout")) {
                String extent = s.nextToken();
                String unit = extent.substring(extent.indexOf('-') + 1);
                extent = extent.substring(0, extent.indexOf('-'));
                int time = 0;
                if (extent.equals("one")) {
                    time = 1;
                } else if (extent.equals("seven")) {
                    time = 7;
                }
                // etc.
                if (unit.startsWith("minute")) {
                    time *= 60;
                }
                spec = "timeout " + time;
            } else {
                System.out.println("Unknown action: " + action);
            }
            if (spec != null) {
                System.out.println("pd spec: " + spec);
                //script.addEventSpec(spec);
            }
        }
        return p;
    }

    @Override
    public boolean sayText(final String text) throws RemoteException {
        return sayText(text, true);
    }

    @Override
    synchronized public boolean sayText(final String text, final boolean wait) throws RemoteException {
        int timeout = ADEGlobals.DEF_TIMEOUT_CALL;
        if (wait) {
            timeout = 0;
        }
        if (speaker != null) {
            try {
                return (Boolean) call(timeout, speaker, "sayText", text, wait);
            } catch (ADEException ex) {
                System.out.println("Speaking error:");
                System.out.println(ex);
                return false;
            }
        } else {
            System.out.println("Would say: " + text);
            return true;
        }
    }

    /** Checks if speech is being produced.
     * @return <tt>true</tt> if speech is being produced, <tt>false</tt>
     * otherwise
     * @throws RemoteException if an error occurs */
    @Override
    public boolean isSpeaking() throws RemoteException {
        boolean isSpeaking = false;
        try {
            isSpeaking = (Boolean) call(speaker, "isSpeaking");
        } catch (ADEException ace) {
            System.err.println("Error checking isSpeaking: " + ace);
        }
        return isSpeaking;
    }

    /** Stops an ongoing utterance.
     * @return <tt>true</tt> if speech is interrupted, <tt>false</tt>
     * otherwise.
     * @throws RemoteException if an error occurs */
    @Override
    public boolean stopUtterance() throws RemoteException {
        System.out.println("Method: stopUtterance()");
	boolean stopped = false;
        try {
            stopped = (Boolean) call(speaker, "stopUtterance");
        } catch (ADEException ace) {
            System.err.println("Error stopping utterance: " + ace);
        }
        return stopped;
    }

    @Override
    public void setActor(String name) throws RemoteException {
	System.out.println("Method: setActor()");
	actorName = name;
    }

    // PWS: not relevant for TLDLDiscourse
    @Override
    public void setInteractor(String name) throws RemoteException {
     System.out.println("Method: setInteractor()");
    }

    // ********************************************************************
    // ************* Section II: Internally Useful Functions  *************
    // ********************************************************************
    // ********************************************************************
    // *************** Section III: ADEComponentImpl Functions ****************
    // ********************************************************************
    /** This method will be activated whenever a client calls the
     * requestConnection(uid) method. Any connection-specific initialization
     * should be included here, either general or user-specific.
     * @param user the ID of the user/client that gained a connection */
    @Override
    protected void clientConnectReact(String user) {
	System.out.println("Method: clientConnectReact()");
        System.out.println(myID + ": got connection from " + user + "!");
        if (user.indexOf("GoalManager") >= 0) {
            gotAction = true;
            // This can generate a superfluous getClient if it was just a time out
            if (useAction) {
		if (DEBUG > 5) {
                    System.out.print("Requesting connection to Action Manager.....");
		}
                actionID = user;
                new Thread() {
                    @Override
                    public void run() {
			// MS: note that we are making a blocking call here because we know the component is up and we should be able to get a client
			actor = getClient(actionVersion,0);
			if (DEBUG > 5) {
			    System.out.println("Done requesting " + actionVersion + " REF: " + actor);
			}
                    }
                }.start();
            } else {
                if (DEBUG > 5) {
                    System.out.println("Not using Action: useAction == " + useAction);
                }
            }
        }
    }

    /** This method will be activated whenever a client that has called the
     * requestConnection(uid) method fails to update (meaning that the
     * heartbeat signal has not been received by the reaper), allowing both
     * general and user specific reactions to lost connections. If it returns
     * true, the client's connection is removed.
     * @param user the ID of the user/client that lost a connection */
    @Override
    protected boolean clientDownReact(String user) {
	 System.out.println("Method: clientDownReact()");
        System.out.println(myID + ": lost connection with " + user + "!");
        if (user.indexOf("GoalManager") >= 0) {
            gotAction = false;
            releaseClient(actionID);
            actor = null;
        }
        return false;
    }

    /** This method will be activated whenever the heartbeat returns a
     * remote exception (i.e., the server this is sending a
     * heartbeat to has failed).
     * @param s the type of {@link ade.ADEComponent ADEComponent} that failed */
    @Override
    protected void componentDownReact(String serverkey, String[][] constraints) {
	 System.out.println("Method: componentDownReact()");
        String s = constraints[0][1];

        System.out.println(myID + ": reacting to down " + s + " server...");
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
         System.out.println("Method: componentConnectReact()");
	String s = constraints[0][1];

        System.out.println(myID + ": reacting to connected " + s + " server...");
    }

    /** Adds additional local checks for credentials before allowing a shutdown.
     * @param credentials the Object presented for verification
     * @return must return "false" if shutdown is denied, true if permitted */
    @Override
    protected boolean localrequestShutdown(Object credentials) {
	 System.out.println("Method: localrequestShutdown()");
        return false;
    }

    /** Implements the local shutdown mechanism that derived classes need to
     * implement to cleanly shutdown. */
    @Override
    protected void localshutdown() {
	 System.out.println("Method: localshutdown()");
        // since we're shutting down, we ignore exceptions; use multiple
        // try/catches so that if one thing bombs, we still do the others
        System.out.print("Shutting Down " + myID + "...");
        //        if (testThread != null)
        //            testThread.halt();    // stop TestThread
        System.out.println("done.");
    }

    /** Provides command line argument descriptions.
     * @return command line argument switches and descriptions */
    @Override
    protected String additionalUsageInfo() {
        StringBuilder output = new StringBuilder();
        output.append("-action       --useAction              <contact an action manager with action requests>\n");
        output.append("-actionversion <version>               <contact the specified action manager with action requests>\n");
        output.append("-fest         --useFestival            <contact a festival server for speech>\n");
        output.append("-festversion <version>                 <contact the specified festival server for speech>\n");
        output.append("-sphinx       --useSphinx              <use a sphinx4 server for speech>\n");
        output.append("-sphinxversion <version>               <use the specified sphinx4 server for speech>\n");
        output.append("-d            --DEBUG                  <print fairly large amounts of DEBUGging information>\n");
        output.append("-v            --verbose                <print copious amounts of DEBUGging information>\n");
        output.append("-log          --logging                <attempt to use local logging mechanisms>\n");
        output.append("-actor <name>                          <change default actor from ").append(actorName).append(">\n");
        output.append("-dict <file>                           <change default dictionary from ").append(dictFile).append(">\n");
        output.append("-repair                                <enable input repair>\n");
        return output.toString();
    }

    /** Parses command line arguments specific to this ADEComponent.
     * @param args the custom command line arguments
     * @return <tt>true</tt> if all <tt>args</tt> are recognized,
     * <tt>false</tt> otherwise */
    @Override
    protected boolean parseadditionalargs(String[] args) {
        boolean found = true;
        for (int i = 0; i < args.length; i++) {
            if (DEBUG > 5) {
                System.out.println("Parsing: " + args[i]);
            }
            if (args[i].equalsIgnoreCase("-action") || args[i].equalsIgnoreCase("--useaction")) {
                if (DEBUG > 5) {
                    System.out.println("Using Action Component.");
                }
                useAction = true;
                found = true;
            } else if (args[i].equalsIgnoreCase("-actionversion")) {
                actionVersion = args[++i];
                if (DEBUG > 5) {
                    System.out.println("Using Action Manager " + actionVersion + ".");
                }
                useAction = true;
                found = true;
            } else if (args[i].equalsIgnoreCase("-fest") || args[i].equalsIgnoreCase("--usefestival")) {
                if (DEBUG > 5) {
                    System.out.println("Using Festival Component.");
                }
                useFestival = true;
                found = true;
            } else if (args[i].equalsIgnoreCase("-festversion")) {
                festVersion = args[++i];
                if (DEBUG > 5) {
                    System.out.println("Using Festival Component " + festVersion + ".");
                }
                useFestival = true;
                found = true;
            } else if (args[i].equalsIgnoreCase("-sphinx") || args[i].equalsIgnoreCase("--usesphinx")) {
                if (DEBUG > 5) {
                    System.out.println("Using Sphinx Component.");
                }
                useSphinx = true;
                found = true;
            } else if (args[i].equalsIgnoreCase("-sphinxversion")) {
                sphinxVersion = args[++i];
                if (DEBUG > 5) {
                    System.out.println("Using Sphinx4 Component " + sphinxVersion + ".");
                }
                useSphinx = true;
                found = true;
            } else if (args[i].equalsIgnoreCase("-d") || args[i].equalsIgnoreCase("-debug") || args[i].equalsIgnoreCase("--debug")) {
                DEBUG = 6;
                if (DEBUG > 5) {
                    System.out.println("Debugging output: active");
                }
                found = true;
            } else if (args[i].equalsIgnoreCase("-v") || args[i].equalsIgnoreCase("-verbose") || args[i].equalsIgnoreCase("--verbose")) {
                DEBUG = 10;
                if (DEBUG > 5) {
                    System.out.println("Debugging output: verbose");
                }
                found = true;
            } else if (args[i].equalsIgnoreCase("-log") || args[i].equalsIgnoreCase("--logging")) {
                if (DEBUG > 5) {
                    System.out.println("Using local logging.");
                }
                localLogging = true;
                found = true;
            } else if (args[i].equalsIgnoreCase("-actor")) {
                i++;
                System.out.println("!!!!!!!!!!!!!!!! Setting rudyname to " + args[i]);
                actorName = args[i];
                found = true;
            } else if (args[i].equalsIgnoreCase("-dict")) {
                i++;
                dictFile = args[i];
                found = true;
            } else if (args[i].equalsIgnoreCase("-repair")) {
                if (DEBUG > 5) {
                    System.out.println("Enabling input repair.");
                }
                useRepair = true;
                found = true;
            } else {
                return false;  // return on any unrecognized args
            }
        }
        return found;
    }

    /** Update server
     */
    // nothing special for now
    @Override
    protected void updateComponent() {
    }

    @Override
    protected void updateFromLog(String logEntry) {
    }

    /*
    final public static void main(String args[]) throws Exception {
        ADEComponentImpl.main(args);
    }
     * 
     */

    /** The server is always ready when it has all its required references */
    @Override
    protected boolean localServicesReady() {
        return ((!useFestival || speaker != null) && (!useSphinx || speaker != null)
                && (!useAction || gotAction));
    }

    // ********************************************************************
    // ******* Section IV: Construction & Initialization Functions ********
    // ********************************************************************
    /** Constructor.
     * @throws RemoteException if the server cannot be instantiated
     * @see #additionalUsageInfo for command line arguments */
    public TLDLDiscourseComponentImpl() throws RemoteException {
        super();
        int i = 0;
        dictionary = new Dictionary();
        dictionary.parse(dictFile, this);
        parser = new Parser();
	boolean[] num = new boolean[50];
	int index = 0;
	try
	    {
		
		socket = new Socket("", 1234);
		out = new PrintWriter(socket.getOutputStream(), true);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

		//	out = new PrintWriter(socket.getOutputStream(), true);
		//in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

		//System.out.println("VERBUNDEN");
		// socket2 = new ServerSocket(1234, 10);
		// con = socket2.accept();
		// in2 = new BufferedReader(new InputStreamReader(con.getInputStream()));
		//	DataInputStream in3 = new DataInputStream(con.getInputStream());
	        // stdIn =new BufferedReader(new InputStreamReader(System.in));
		// out2 = new PrintWriter(con.getOutputStream(), true);
	
		while(true && (num.length > index) )
		     {		 
			 if(in.ready() == false)
			     {
				 num[index] = in.ready();
				 index++;
				 if(num.length == index)
				     {
					
					 break;
				     }
			     }
			
			 //	 System.out.println("VERBUNDEN1234");
			 
			 message = in.readLine();
			 // System.out.println(message);
			     if(message != null && message.trim().length() != 0)
				 { //true
				     //	     System.out.println("message is: "+message);
				     splitSentence(message);
				 }
			 
		     }
			 // socket.close(); //socket1
		 	 // socket = null;
		//System.out.println("close the socket connection\n");
		socket.close();
		return;
    
		     
	    }
	catch (Exception e) 
	    {
		 //socket1
		// socket = null;

	     	System.out.println("Read failed2");
        	System.exit(-1);
	    }  
	
	
	
        if (useFestival && !useSphinx) {
            if (DEBUG > 5) {
                System.out.print("Connecting to Festival Component.....");
            }
            try {
                speaker = getClient(festVersion);
            } catch (Exception ex) {
                System.err.println("Exception getting Festival Component: ");
                System.err.println(ex);
            }
            if (DEBUG > 5) {
                System.out.println("Connected.");
            }
        } else {
            if (DEBUG > 5) {
                System.out.println("Not using Festival: useFestival == " + useFestival);
            }
        }
        if (useSphinx) {
            if (DEBUG > 5) {
                System.out.print("Connecting to Sphinx Component.....");
            }
            try {
                speaker = getClient(sphinxVersion);
            } catch (Exception ex) {
                System.err.println("Exception getting Sphinx Component: ");
                System.err.println(ex);
            }
            if (DEBUG > 5) {
                System.out.println("Connected.");
            }
        } else {
            if (DEBUG > 5) {
                System.out.println("Not using Sphinx: useSphinx == " + useSphinx);
            }
        }
        if (useRepair) {
            String[] fargs = {"-0", "-r"};
            Repairs.handleArgs(fargs);
        }

        if (localLogging) {
            try {
                setADEComponentLogging(true);
            } catch (AccessControlException ex) {
                ex.printStackTrace();
            } catch (ADEException ex) {
                ex.printStackTrace();
            }

        }
   
    }
    public String startMyPhrase(String sen)
    {
	String[] inArray = sen.split(" ");
	int i = 0;
	while(i < inArray.length)
	    {
		if(i == (inArray.length -1))
		    {
			break;
		    }else{
		    if(inArray[i].equals("rock") || inArray[i].equals("rocks") ||
		       inArray[i].equals("tree") || inArray[i].equals("trees") ||
		       inArray[i].equals("river")|| inArray[i].equals("rivers") ||
		       inArray[i].equals("sea") || inArray[i].equals("seas") ||
		       inArray[i].equals("hill") || inArray[i].equals("hills") ||
		       inArray[i].equals("pylon") || inArray[i].equals("pylons") ||
		       inArray[i].equals("wood") || inArray[i].equals("woods") ||
		       inArray[i].equals("house") || inArray[i].equals("houses") ||
		       inArray[i].equals("mountain") || inArray[i].equals("mountains") ||
		       inArray[i].equals("picture") || inArray[i].equals("pictures"))
  			{
			    System.out.println("inArray[i] :"+inArray[i]);
			    int test = i +1;
			    if(test == (inArray.length - 1))
		  		break;
			    if(inArray[i+1].equals("next") || inArray[i+1].equals("between") ||
			       inArray[i+1].equals("inbetween") || inArray[i+1].equals("over") ||
			       inArray[i+1].equals("close") || inArray[i+1].equals("to") ||
			       inArray[i+1].equals("in") || inArray[i+1].equals("behind")||
			       inArray[i+1].equals("right")|| inArray[i+1].equals("around")||
			       inArray[i+1].equals("across")|| inArray[i+1].equals("under")||
			       inArray[i+1].equals("above")|| inArray[i+1].equals("left"))
				{
				    int len = inArray.length + 1;
				    String[] cpArray = new String[len];
				    int j= 0;
				    while (j <= i)		
					{
				            cpArray[j] = inArray[j];
				            j++;
				 	}
				    
				    cpArray[j] = "sequ";
				    int s = j +1;
				    int si = i +1;
				    for(int k = s; k < cpArray.length; k++, si++)
				 	{
					    cpArray[k] = inArray[si]; 	
				 	}
				    inArray = null; 
				    inArray = new String[len];
				    for(int z = 0; z < inArray.length; z++)
				 	{
					    inArray[z] = cpArray[z];
					    System.out.println("inArray[z] :"+inArray[z]);
									    
				 	}
				    i++;
				}
			}
		    i++;
		}
	    }
	
	StringBuffer sbf = new StringBuffer();
        
	if(inArray.length > 0){
	    
	    sbf.append(inArray[0]);
	    for(int l=1; l < inArray.length; l++){
		sbf.append(" ").append(inArray[l]);
	    }
                       
	}
	// System.out.println("Show me the new String " + sbf.toString());
        return sbf.toString();
      }

    public void splitSentence(String sentence) throws RemoteException
    {
	//System.out.println("splitSentence function");
	ArrayList<String> cpArray = new ArrayList<String>();
	String lastElem = sentence.substring(sentence.length()-1);// length()-1;
	if(lastElem.equals(".")|| lastElem.equals("!") || lastElem.equals("?"))
	    {
		sentence = sentence.substring(0, sentence.length()-1);
		// System.out.println(" What is the sentence: " + sentence);
	    }
	//	 System.out.println("Sentence is: " + sentence);
	sentence = startMyPhrase(sentence);
	//System.out.println("New Sentence is: " + sentence);
	//ADD A ZERO TO THE END OF THE OLD SENTENCE
	sentence = sentence + " 0";
	//System.out.println("New Sentence is2: " + sentence);
	String[] inArray = sentence.split(" ");
	int index = 0;
	while(inArray.length-1 >= index)
	    {
		cpArray.add(inArray[index++]);
		addWords(cpArray);
	    }
    }

    public String checkOrder(String word) throws RemoteException
    {
	String checker = null;
	switch(word) {
	case "Are":
	case "Do":
	case "Does":
	case "Have":
	case "Has":
	case "How":
	case "What":
	case "Which":
	case "Where":
	case "Who":
	case "Whose":
	case "Why":
	    checker = "question";
	    return checker;
	default:
	    checker = "order";
	    return checker;	
	}
    }
}
