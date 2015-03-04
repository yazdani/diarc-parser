/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * TemplateComponentImpl.java
 *
 * @author Paul Schermerhorn
 */
package com.template;

import static utilities.Util.Sleep;

import java.rmi.RemoteException;
import java.util.Random;

import ade.ADEComponentImpl;
import ade.gui.ADEGuiVisualizationSpecs;

/** <code>TemplateComponentImpl</code>.  This is just an example server that
 * demonstrates some basic ADE functionality, such as constructing the
 * server, implementing a remote method and advertising it for other
 * servers, obtaining a client, and making an ADE call to that client.  If
 * you start two instances of TemplateComponentImpl, the first will obtain a
 * reference to the second and make remote calls to it.
 *
 * This server also can serve as a template from which new servers can be
 * constructed.  Copy this file and TemplateComponent.java to their new home,
 * change the package name, search and replace the class names, and fill in
 * the server-specific details (i.e., constructor, remote interface, etc.).
 * 
 * From the top-level ADE directory:
 * Compile with:    javac com/template/TemplateComponent*.java 
 * Run with:        ./runadeserver com/template/TemplateComponent
 *       or         ./runadeserver com.template.TemplateComponent
 * You can also run the servers via a config file:
 *      ./runaderegistry -g -f com/template/TwoTemplateComponents.config 
 * To see a visualization of the server, append "-g" or "--GUI" to the 
 *      command-line.  Or "-g ?" to see visualizaton possibilities.
 * To start with logging, append "--logging" to the command-line.
 * To play back a log, start with "-g Log", and choose a file in the
 *      visualization window that appears.  You can also choose a file
 *      from the command-line with "--playback <filename>".  
 *      To see the other visualization at the same time as the Log window,
 *      do "-g Log Message", or choose both the visualization from the 
 *      list that appears when you do "-g ?".
 */
public class TemplateComponentImpl extends ADEComponentImpl implements TemplateComponent {
	private static final long serialVersionUID = 1L;

	/** a prefix used to distinguish log entries that will be parsed 
	 * by the updateFromLog(String logEntry) method. */
	private static final String STATE_LOG_INDICATOR = "COUNTER: ";
	
	// arguments that are populated by parseadditionalargs MUST
	//    be STATIC, otherwise setting them will have no effect.
    private static boolean verbose = false;

    /* Component-specific fields */
    private Object otherComponentRef = null;
    
    private int currentCounter;
    private boolean alreadyAlertedUserOfWaiting = false;
    
    
        // ********************************************************************
    // *** Local methods
    // ********************************************************************
    /** 
     * TemplateComponentImpl constructor.
     */
    public TemplateComponentImpl() throws RemoteException {
        super();


        // Open devices, get references, etc., here
        
        // The server can also set its own loop time (the default is 10x/second). 
        //     For this example, will slow down the server to a 1-second pause:
        this.setUpdateLoopTime(this, 1000); // the amount is in milliseconds.
        
        // Typically, a server will run various initialization code here.
        //     In this example case, we will simply initialize a counter
        //     (which henceforth will be incremented by updateComponent())
        //     to some random number, so that two instances of the TemplateComponent
        //     have different counters:
        this.currentCounter = new Random().nextInt(1000);

	/*        
        // To connect to another server (in this case, just another server
        //      of the same type), do:
    	while (otherComponentRef == null) {
    		if (verbose || (!alreadyAlertedUserOfWaiting)) {
    			System.out.println("Attempting to establish a connection " +
    					"to another Template Component...");
    			alreadyAlertedUserOfWaiting = true;
    		}
    		
			otherComponentRef = getClient(TemplateComponent.class.getName());
			if (otherComponentRef == null) { // if didn't establish a connection
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					System.out.println("Could not sleep.");
				}
			}
		}
		System.out.println("Connection established!");
		*/   
 
    }
    
    /** update the server once */
    @Override
    protected void updateComponent() {

		/* Example of the new one-shot call mechanism
	
		// make a one-shot call to the SimSpeechProduction server
		try {
	   	 	System.err.println("MAKING ONE SHOT CALL");
		    call(new String[][]{{"type","com.interfaces.SpeechProductionComponent"}},"sayText","Hello!");
	   		System.err.println("DONE, HOPEFULLY");
		} catch(Exception e) {
	    	System.err.println("NO... " + e);
		}
		*/	

        // this method is called periodically, based on the updateLoopTime.
    	//     see "this.setUpdateLoopTime(this, 1000)" call in the constructor.  

    	// first, update the server's own counter.
    	this.currentCounter++;
    	
    	// then use a common method that does something with that information,
    	//     common both to updateComponent and updateFromLog, if relevant.
    	//     in this case, just write this information to the console:
    	processCounterInfo();
    	
    	// Logging example:  log the counter, so that can play back its 
    	//      values later.  In this case, a prefix is used to parse
    	//      state data from info-only messages in the Log:
    	canLogIt(STATE_LOG_INDICATOR + this.currentCounter, true);
        
    	
    	// Finally, if this server has obtained a reference to another 
    	//     server, obtain and print information about it as well:
    	if (otherComponentRef != null) {
	        try {
	            String otherComponentMessage = (String) call(otherComponentRef, 
	            		"getComponentNameAndCounter");
	            System.out.println(getNameFromID(myID) + ":  " + 
	            		otherComponentMessage);
	        } catch (Exception e) {
	        	System.err.println(myID + ":  error getting other " +
	        			"server's information" + ":  " + e);
	            // ADE exceptions can be fairly opaque, so a stack trace 
	        	//     can be helpful
	            e.printStackTrace();
	        }
    	}
    }
    
    /** common method, used both by updateComponent and updateFromLog */
    private void processCounterInfo() {
    	System.out.println(getNameFromID(myID) + ":  " + 
    			"My counter = " + this.currentCounter);
	}

	/** Reads in one update from the log at a time, and re-populates the
     * this.currentCounter datastructure (the only "state" information 
     * for this sever).  For some servers,
     * like a Laser Range Finder, logging is very easy and makes perfect
     * sense; for others, this method is impractical, and can be ignored. */
    @Override
    protected void updateFromLog(String logEntry) {
    	if (logEntry.startsWith(STATE_LOG_INDICATOR)) {
            String actualEntry = logEntry.substring(STATE_LOG_INDICATOR.length() + 1);
            this.currentCounter = Integer.parseInt(actualEntry);
            processCounterInfo();
        }
    }

    
    @Override
    protected boolean localServicesReady() {
    	return true;
    }
    
    
    /** A visualization-spec override, in order to include a new server  
     * visualization.  Note the initial call to super.getVisualizationSpecs(),
     * so that can get the superclasses' visualizations (including
     * the log-playback window).
     */
    @Override
    public ADEGuiVisualizationSpecs getVisualizationSpecs()
    		throws RemoteException {
    	ADEGuiVisualizationSpecs specs = super.getVisualizationSpecs();
    	specs.add("Message", TemplateComponentVis.class);
    	return specs;
    }


    /**
     * Provide additional information for usage...
     */
    @Override
    protected String additionalUsageInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Component-specific options:\n\n");
        sb.append("  -verbose                  <verbose printing>\n");
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
            } else {
                System.out.println("Unrecognized argument: " + args[i]);
                return false;  // return on any unrecognized args
            }
        }
        return found;
    }
    
    

    // ***********************************************************************
    // Methods available to remote objects via RMI
    // ***********************************************************************
    // Implement here whatever interface is defined in TemplateComponent.java

    /**
     * An example of a remote call that fetches data from the server.
     *     Note that this remote call is used both by other servers and the
     *     server visualization -- e.g., there is no special interface
     *     that needs to be implemented for GUI-related methods.  
     * @return server name, followed by an incrementing counter (initially
     *     seeded to a random value, so that the output of the two servers
     *     is different) 
     */
    @Override
    public String getComponentNameAndCounter() throws RemoteException {
    	return this.getNameFromID(this.getID()) + 
    			" reports a counter value of " + this.currentCounter;
    }
    

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
        return;
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
        System.out.println(myID + ": reacting to down " + s + "!");

        //if (s.indexOf("VelocityComponent") >= 0)
        //    gotVel = false;
        return;
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
        System.out.println(myID + ": reacting to connecting " + s + "!");

        //if (s.indexOf("VelocityComponent") >= 0)
        //    gotVel = true;
        return;
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
        System.out.print(this.getClass().getName() + " shutting down...");
        //finalize();
        Sleep(100);
        System.out.println("done.");
    }
   
}
