/**
 * Agent Development Environment (ADE)
 *
 * @version 1.0
 * @author Matthias Scheutz
 *
 * Copyright 1997-2013 Matthias Scheutz and the HRILab Development Team
 * All rights reserved.  For information or questions, please contact
 * the director of the HRILab, Matthias Scheutz, at mscheutz@gmail.com
 * 
 * Redistribution and use of all files of the ADE package, in source and
 * binary forms with or without modification, are permitted provided that
 * (1) they retain the above copyright notice, this list of conditions
 * and the following disclaimer, and (2) redistributions in binary form
 * reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT HOLDER OR ANY
 * OF THE CONTRIBUTORS TO THE ADE PROJECT BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 * IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.

 * Note: This license is equivalent to the FreeBSD license.
 */
package ade;

import ade.gui.ADEGuiExternalFrame;
import ade.gui.ADEGuiVisualizationSpecs;
import ade.gui.icons.IconFetcher;
import ade.gui.logview.ADELogPlaybackVis;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import utilities.Util;

/**
 * The implementation of the {@link ade.ADEComponent ADEComponent} interface. The
 * <tt>ADEComponentImpl</tt> provides the minimum functionality necessary for an
 * <tt>ADEComponent</tt>, a (possibly remote) component that supplies services to
 * or uses the services of other <tt>ADEComponents</tt> via Java's RMI facilities.
 * <p> Infrastructure mechanisms include: <ul> <li>Maintaining connection status
 * (via the heartbeat/reaper combination)</li> <li>Component recovery and
 * reconnection upon failure</li> <li>Controlling access by specifying the
 * number of connections allowed</li> <li>Controlling access via security
 * options (including username/password lists and various user access
 * levels)</li> </ul> The <tt>ADEComponentImpl</tt> uses static and final
 * attributes applied to various methods and fields for security purposes, in
 * addition to using <i>credentials</i> for certain methods to limit the actions
 * that can be performed in various situations.
 *
 * @see ade.ADEComponent ADEComponent
 * @see ade.ADEComponentInfo ADEComponentInfo
 * @see ade.ADEHostInfo ADEHostInfo
 * @see ade.ADEUser ADEUser
 * @see ade.ADEPreferences ADEPreferences
 * @see ade.ADEGlobals ADEGlobals
 * @see ade.ADERegistryImpl ADERegistryImpl
 */
abstract public class ADEComponentImpl extends UnicastRemoteObject implements
        ADEComponent, Runnable {
    /*
     * A note for adding infrastructure functionality, one should make sure to
     * use the "rct" objects (ADERemoteCallTimers) in lieu of direct RMI calls
     * (i.e., "component.rmiMethod()"), so as to maintain consistent timeout and
     * connection functionality.
     */

    final static private String prg = "ADEComponentImpl";
    static String jvmpid; // ID of JVM (process ID, if applicable)
    // MS: what is this needed for? REGLOCK removed
    static protected ADELock reglock = null;
    /**
     * Whether total memory should be printed on start-up
     */
    final static private boolean showMemory = false;
    /**
     * The name of the component-specific config file (-f or --file CL arg)
     */
    static protected String configFileName = null;
    // debugging
    /**
     * The debug level; 0-10, 10 being the most verbose. Levels 0-3 are reserved
     * for subclass implementations; 4-6 are used for output produced by
     * non-repeating tasks; values of 7-9 are used for output produced by
     * periodic tasks (e.g., within the heartbeat thread); values of 10+ are not
     * used by ADE.
     */
    protected int dbg = 0;
    // debug toggles to isolate certain aspects of operation
    private boolean debugging = false;
    private boolean debugHB = false;
    private boolean debugReconn = false;
    private boolean debugRP = false;
    private boolean debugCall = false;
    private boolean debugMeths = false;
    private boolean debugTimes = false;
    private boolean verbose = false;
    // private member fields
    /**
     * Whether to perform "local logging". Note that this is different than
     * maintaining state, as it simply logs arbitrary data with timestamps.
     */
    private boolean locallogging = false;
    /**
     * The Writer used when "local logging" is active.
     */
    private FileWriter fwrLog = null;
    /**
     * Rudimentary state handling (saving, reloading). Note that this differs
     * from logging in that data will be periodically sent to the
     * {@link ade.ADERegistryImpl ADERegistryImpl} with which this component is
     * registered, to be reloaded if the component fails and is subsequently
     * restarted. The component must provide its own subclass of the
     * {@link ade.ADEState ADEState} class, which defines the import and export
     * methods specific to the component.
     */
    private boolean maintainState = false;
    /**
     * The {@link ade.ADEState ADEState} subclass object used to save this
     * component's state.
     */
    private ADEState state = null;
    /**
     * Controls registration (i.e., joining the system; only set in
     * {@link #finishedInitialization}).
     */
    private boolean initialized = false;
    /**
     * Access control: prevents instantiation of derived classes that don't go
     * through this class' main function.
     */
    private static boolean entryright = false;
    /**
     * Whether the component is registered or not.
     */
    private boolean registered = false;
    /**
     * Whether the shutdown was locally determined or by the registry
     */
    private boolean registryenforced = false;
    /**
     * The {@link ade.ADERegistryImpl ADERegistryImpl} with which this component is
     * registered.
     */
    private ADERegistry ar;
    /**
     * Automatically generated password.
     */
    private String myPassword;
    /**
     * The {@link ade.ADEComponentInfo ADEComponentInfo} object that contains
     * information about this component.
     */
    private ADEComponentInfo myInfo;
    /**
     * The {@link ade.ADEHostInfo ADEHostInfo} object that contains information
     * about the host on which this component is executing.
     */
    private ADEHostInfo myHost;
    /**
     * The <tt>Heartbeat</tt> that updates the {@link ade.ADERegistryImpl
     * ADERegistryImpl} with which this component is registered.
     */
    private Heartbeat registryHB;
    /**
     * The list of client IDs that have connections to this component. The
     * {@link java.util.concurrent.ConcurrentHashMap ConcurrentHashMap} uses the
     * client id and {@link java.lang.System#currentTimeMillis currentTime} as
     * its key/value pair. While the {@link ade.ADEComponentInfo ADEComponentInfo}
     * object also stores the connected clients, it not only increases
     * efficiency to store them here (more direct access), but keeps the size of
     * the component info object a bit smaller.
     */
    final private ConcurrentHashMap<String, Long> userMap = new ConcurrentHashMap<String, Long>();
    /**
     * The <tt>Reaper</tt> period (in ms). Controls how often the component checks
     * to make sure a client is still connected.
     */
    private int rppulse;
    /**
     * The number of reaper cycles a client has to finish initialization and
     * send a heartbeat signal after first contact. In other words,
     * initialization must take no longer than <tt>reaperRemoveUser *
     * rppulse</tt>.
     */
    private long reaperRemoveUser = 10;
    /**
     * Maintains connections to this component by periodically confirming that
     * clients have updated their status.
     */
    private Reaper theReaper;
    /**
     * Stores the <tt>Heartbeat</tt> used to maintain connections with other
     * components (i.e., to which this component has a client connection). Key/value
     * pairs are the component ID (concatenation of <tt>type$name</tt>) and
     * <tt>Heartbeat</tt> objects, respectively.
     */
    final private HashMap<String, Heartbeat> myheartbeats = new HashMap<String, Heartbeat>();

    /** Stores the components that meet a given constraint
    */
    final private HashMap<String[][], ArrayList<Heartbeat>> myconstraints = new HashMap<String[][], ArrayList<Heartbeat>>();
   
    /**
     * A synchronization guard for creating <tt>Heartbeat</tt> objects.
     */
    final private Boolean heartbeatGuard = false;
    /**
     * a ADELogHelper class to facilitate parsing and playback of logged component
     * data
     */
    private ADELogHelper logHelper = new ADELogHelper();
    /**
     * Remote call timer that makes infrastructure calls to
     * {@link ade.ADERegistry ADERegistry}s.
     */
    private ADERemoteCallTimer rctReg;
    // shared within the "ade" package, but are not available to derived classes
    /**
     * The users allowed to obtain a connection to this component. Key/value pairs
     * are the user ID and an associated {@link ade.ADEUser ADEUser} object.
     */
    final HashMap<String, ADEUser> userDB = new HashMap<String, ADEUser>();
    // ===== static globals that are used for command line arguments ==========
    static private String useIP; // the IP address to be used
    static private String[] myargs; // command line args at component start
    // ========= we don't mind if subclasses can identify themselves ========
    /**
     * The component ID (concatenation of <tt>type$name</tt>).
     */
    protected String myID;
    /**
     * change to true to log all remote calls
     */
    private static boolean callLogging = false;
    /**
     * list of logged calls (returned by getLoggedCalls)
     */
    private ArrayList<String> loggedCalls = new ArrayList<String>();
    /**
     * the new life mode, which is by default true
     */
    private boolean liveMode = true;
    /**
     * the suspend feature, which will halt the main run loop
     */
    private boolean suspended = false;
    /**
     * the dynamic looptime feature
     */
    private boolean dynamicloop = false;
    /**
     * the dynamic increment for adjusting the main loop timing
     */
    private long dynamicincrement = 10;
    /**
     * the cycle time for the main loop in msec
     */
    private long looptime = 100;
    /**
     * indicates whether the component is operation (note that once this is set
     * to false, then component will never be able to continue; for temporary
     * suspension use "suspend")
     */
    private boolean operating = true;
    // data structures to allow output redirection (or, rather, output copying)
    // to ADE SystemView GUI:
    private Boolean isRedirectingOutput = false; // capital Boolean so that we can synchronize on it
    private HashMap<UUID, StringBuilder> redirectedOutputConsumers;

    /** used for the new notification mechanism
     *  stores for each calling component the component name as key plus
     */
    final private HashMap<ADEComponent, ArrayList<ADENotification>> notifications = new HashMap<ADEComponent, ArrayList<ADENotification>>(); // key on component RMI reference
    ConcurrentHashMap<String, Method> localMethodsFast = new ConcurrentHashMap<String, Method>();

    /** used to delay the start-up of the system until at least one other registry has checked in */
    protected boolean delaystartup = false;
    
    // ********************************************************************
    // **** abstract methods that need to be implemented by derived classes
    // ********************************************************************
    /**
     * Must be implemented by subclass, although it may be a dummy method. This
     * method will be activated whenever a client calls the
     * requestConnection(uid) method. Any connection-specific initialization
     * should be included here, either general or user-specific. Most often the
     * method simply prints a message to the console indicating that a user
     * connected.
     *
     * @param user the ID of the user/client that gained a connection
     */
    abstract protected void clientConnectReact(String user);

    /**
     * Must be implemented by subclass, although it may be a dummy method (that
     * should return <tt>false</tt>). This method will be activated whenever a
     * client that has called the requestConnection(uid) method fails to update
     * (meaning that the heartbeat signal has not been received by the reaper),
     * allowing both general and user specific reactions to lost connections. If
     * it returns true, the client's connection is removed. Most often the
     * method simply prints a message to the console indicating that a user was
     * disconnected and returns <tt>false</tt>.
     *
     * @param user the ID of the user/client that lost a connection
     * @return <tt>true</tt> if the connection with <tt>user</tt> should be
     * removed, <tt>false</tt> otherwise
     */
    abstract protected boolean clientDownReact(String user);

    /**
     * This method will be activated whenever the heartbeat returns a
     * {@link java.rmi.RemoteException RemoteException} (i.e., the component to
     * which this is sending a heartbeat has failed). Most often the method
     * simply prints a message to the console indicating that a connection with
     * a component has been lost. <p> In many components, a boolean variable is used
     * to maintain a more immediate status determination (usually called
     * <tt>gotX</tt>, where <tt>X</tt> is the type of component). While not
     * required, the use of such variables allow avoidance of remote method
     * calls that are guaranteed to fail due to an incomplete connection,
     * thereby improving performance. A code snippet example of this usage might
     * take the following form: <p> <tt> private Object exampleComponent;<br>
     * private boolean gotExampleComponent = false;<br> ...<br> protected void
     * componentDownReact(String sid) {<br> <div style="margin-left: 40px;"> if
     * (sid.indexOf("ExampleComponent") >= 0) {<br> <div style="margin-left:
     * 40px;"> gotExampleComponent = false;</div><br> }</div><br> }<br> ...</tt>
     * <p> Then, any method calls made to the <tt>ExampleComponent</tt> can be
     * surrounded by an <tt>if</tt> statement like so: <p> <tt>if
     * (gotExampleComponent) {<br> <div style="margin-left: 40px;">
     * call(exampleComponent, "exampleMethod");<br></div> }</tt> <p> If the above
     * is done, it is very important to also make use of the
     * {@link #componentConnectReact} method to set the boolean value to
     * <tt>true</tt> upon (re)connection.
     *
     * @param sid a string representing the {@link ade.ADEComponent ADEComponent} that
     * has lost a connection
     */
    abstract protected void componentDownReact(String componentkey,
            String[][] constraints);

    /**
     * This method will be activated whenever the heartbeat connects to a client
     * (i.e., the component to which this is sending a heartbeat has been
     * connected/reconnected). Most often the method simply prints a message to
     * the console indicating that a connection with a component has been gained.
     * <b>NOTE:</b> the pseudo-reference obtained by the {@link #getClient}
     * method will not be set until <b>after</b> this method is executed. To
     * perform operations on the newly (re)acquired reference, you must use the
     * <tt>ref</tt> parameter object. <p> In many components, a boolean variable is
     * used to maintain a more immediate status determination (usually called
     * <tt>gotX</tt>, where <tt>X</tt> is the type of component). While not
     * required, the use of such variables allow avoidance of remote method
     * calls that are guaranteed to fail due to an incomplete connection,
     * thereby improving performance. A code snippet example of this usage might
     * take the following form: <p> <tt> private Object exampleComponent;<br>
     * private boolean gotExampleComponent = false;<br> ...<br> protected void
     * componentConnectReact(String sid, Object ref) {<br> <div style="margin-left:
     * 40px;"> if (sid.indexOf("ExampleComponent") >= 0) {<br> <div
     * style="margin-left: 40px;"> // any reference-dependent handling</div><br>
     * gotExampleComponent = true;<br> }</div><br> }<br> ...</tt> <p> Then, any
     * method calls made to the <tt>ExampleComponent</tt> can be surrounded by an
     * <tt>if</tt> statement like so: <p> <tt>if (gotExampleComponent) {<br> <div
     * style="margin-left: 40px;"> call(exampleComponent,
     * "exampleMethod");<br></div> }</tt> <p> If the above is done, it is very
     * important to also make use of the
     * {@link #componentDownReact} method to set the boolean value to
     * <tt>false</tt> upon disconnection.
     *
     * @param sid a string representing the {@link ade.ADEComponent ADEComponent} that
     * has established a connection
     * @param ref the pseudo-reference that was requested
     */
    abstract protected void componentConnectReact(String componentkey, Object ref,
            String[][] constraints);

    /**
     * Return the component's "capabilities". In general, this amounts to a "device
     * list"; that is, a description of the sensors and effectors that a
     * returned agent has. However, it can be used to give a more detailed
     * description of the component's function.
     */
    /*
     * Do we want to use this? public String requestCapabilityString() throws
     * RemoteException { return null; }
     */
    /**
     * Adds additional local checks for credentials before allowing a shutdown.
     * Must return <tt>false</tt> if shutdown is denied, <tt>true</tt> if
     * permitted.
     *
     * @param credentials confirming information
     * @return <tt>true</tt> if approved, <tt>false</tt> otherwise
     */
    abstract protected boolean localrequestShutdown(Object credentials);

    /**
     * Local shutdown mechanism that derived classes need to implement for clean
     * shutdown.
     */
    abstract protected void localshutdown();

    /**
     * The update component method that is called by the new logging mechanism for
     * live updates -- this is where code in derived components should be placed;
     * NOTE: make sure that the function returns, otherwise the whole loop
     * mechanism will not work properly
     */
    abstract protected void updateComponent();

    /**
     * The update from log method that is called by the new logging mechanism
     * for playback from logs -- this is where code in derived components should be
     * placed that handles logged data; it needs to be able to take a string
     * containing the state description of the component and deserialize it into
     * the component state and populate fields and data structures with the
     * state information
     *
     * @param logEntry a string representation of one line from the log
     * representing the state of this component
     *
     * NOTE: make sure that the function returns, otherwise the whole loop
     * mechanism will not work properly
     */
    abstract protected void updateFromLog(String logEntry);

    /**
     * Returns whether a component is ready and able to provide its services. This
     * method needs to be implemented by each subclass to specify the criteria
     * that must be met before it can provide its services (e.g., it must have
     * connected with all it's clients, it's data structures have been fully
     * initialized, etc.).
     *
     * @return <tt>true</tt> if the component is ready, <tt>false</tt> otherwise
     */
    abstract protected boolean localServicesReady();

    /**
     * Provide additional information concerning command-line switches. Any
     * additional switches added should be parsed in the
     * {@link #parseadditionalargs} method.
     *
     * @return The text to display when given a <tt>-h</tt> switch
     */
    abstract protected String additionalUsageInfo();

    /**
     * Parse additional command-line arguments. Needs to return <tt>true</tt> if
     * parse is successful, <tt>false</tt> otherwise. If additional arguments
     * are enabled, please make sure to add them to the
     * {@link #additionalUsageInfo} method to display them from the command-line
     * help.
     *
     * @param args The additional arguments for this component
     * @return <tt>true</tt> if additional arguments exist and were parsed
     * correctly, <tt>false</tt> otherwise
     */
    abstract protected boolean parseadditionalargs(String[] args);

    /**
     * Handle the notification (change in state) of one of the components to which
     * this has a reference. If notification is turned on (using the
     * {@link #setNotify} method), this method will be called whenever
     * {@link ade.ADEGlobals.ComponentState#notify} returns <tt>true</tt> for the
     * {@link ade.ADEGlobals.ComponentState state} of a component in the list set by
     * the {@link ade.ADERegistryImpl ADERegistryImpl} in a call to
     * {@link #updateComponentInfo}.
     *
     * @param ref A <i>pseudo-reference</i>
     */
    protected void componentNotify(Object ref, ADEGlobals.RecoveryState recState) {
        // must be overridden by subclass for use
        if (dbg > 3) {
            System.out.println(myID + ": notified about " + getRefID(ref)
                    + " due to state " + getState(ref));
        }
    }

    // this is the function derived components can implement
    // MS: TODO: will be made abstract
    /**
     * Announces the presence of a new component in the ADE system
     *
     * @param newcomponentkey the component's ID in the ADE system
     */
    protected void notifyComponentJoined(String newcomponentkey) {
    }

    // ***************************************************************
    // ******* Implementation of the ADEComponent interface
    // ***************************************************************
    /**
     * Set the debugging output level for this ADEComponent. The debug level is an
     * integer that generally ranges from 0 to 10, where higher numbers increase
     * verbosity. Numbers greater than 7 are usually reserved for messages
     * placed within loops (e.g., heartbeats, reapers, etc.).
     *
     * @param i The debug level to set
     * @param credentials Confirming information
     * @throws RemoteException If an error occurs
     * @throws AccessControlException If the user does not have adequate
     * permissions
     */
    @Override
    final public void setDebugLevel(int i, Object credentials)
            throws RemoteException, AccessControlException {
        // only allow debug set from the own component, the own registry,
        // registered users, or entities that pass the local criteria
        if (credentials.equals(this)
                || credentials.equals(ar)
                || (credentials instanceof String && userMap.containsKey((String) credentials))
                || localRequestComponentInfo(credentials)) {
            System.out.println(myID + ": setting debug level to " + i);
            dbg = i;
        } else {
            throw new AccessControlException(
                    "Authorization to set debug level failed based on "
                    + credentials);
        }
    }

    /**
     * The registry will call this method if a new component is joining and if the
     * notification has been turned on
     *
     * @param newcomponentkey the key of the newly joined component
     */
    @Override
    final public void notifyComponentJoinedRegistry(final String newcomponentkey,
            Object credentials) throws RemoteException {
        // only allow the registry with which this component is registered to call
        if (credentials.equals(ar)) {
	    new Thread() {
		@Override
		 public void run() {
		    notifyComponentJoined(newcomponentkey);
		}
	    }.start();
        } else {
            throw new AccessControlException(
                    "Authorization for joining component notification failed based on "
                    + credentials);
        }
    }

    /**
     * Used by remote components to confirm that this component responds.
     *
     * @return Always returns <tt>true</tt>
     * @throws RemoteException If method fails
     */
    @Override
    public final boolean isUp() throws RemoteException {
        return true;
    }

    /**
     * Returns whether a component is ready and able to provide its services. This
     * method calls the {@link ade.ADEComponentImpl#localServicesReady
     * localServicesReady} method to determine its return value, which can be
     * overridden by subclass if there are criteria that must be met before it
     * can provide its services (e.g., it must have connected with all it's
     * clients, it's data structures have been fully initialized, etc.).
     *
     * @param credentials Confirming information
     * @return <tt>true</tt> if the component is ready, <tt>false</tt> otherwise;
     * unless overridden by subclass, returns <tt>true</tt>
     * @throws RemoteException If an error occurs
     * @throws AccessControlException If the user does not have adequate
     * permissions
     */
    @Override
    public final boolean servicesReady(Object credentials)
            throws RemoteException, AccessControlException {
        if (credentials.equals(this)
                || credentials.equals(ar)
                || (credentials instanceof String && userMap.containsKey((String) credentials))) {
            return localServicesReady();
        } else {
            throw new AccessControlException(
                    "Authorization to check services failed based on "
                    + credentials);
        }
    }

    /**
     * An {@link ade.ADERegistry ADERegistry} calls this method to gain a
     * connection for the user with the given <tt>uID</tt>. For authentication,
     * the registry sends its own stub, which is checked against the local stub
     * -- this method is <tt>final</tt> for security purposes; subclasses are
     * <b>not</b> allowed to override.
     *
     * @param uID the user ID
     * @param credentials confirming information
     * @param the constraints under which the component was requested
     * @return a reference to this component
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    @Override
    public void requestConnectionRegistry(final String uID,
					  Object credentials,
					  String[][] constraints
					  ) throws AccessControlException, RemoteException {
        // make sure that this is called from an ADERegistry
        // if (!(credentials.equals(ar))) {
        if (!(credentials instanceof ADERegistry)) {
            throw new AccessControlException("Must be called by an ADERegistry.");
        }
        if (dbg > 3) {
            System.out.println("\t" + myID + " got request from " + uID);
            if (dbg > 5) {
                System.out.println("\t\tconnectsCurrent: "
                        + myInfo.connectsCurrent);
            }
        }
        // see if the connections are maxxed out
        if (myInfo.connectsCurrent >= myInfo.connectsAllowed) {
            throw new AccessControlException(myID
                    + ": Maximum number of connections reached");
        } else {
            // allow another connection, but only if the user doesn't have
            // one yet; note that the time is intially stored as -1, so that
            // the connection will not be reaped until the client checks in
            // (which will be after it finishes the initialization process)
            // if the client attempts another connection prior to checking in
            // (i.e., they never finished initialization), allow another
            // connection but don't re-do the addClient or clientConnectReact
            // After each reaper cycle, the stored value will be decremented;
            // if the client still hasn't checked in after a set number of
            // cycles, they will be removed.
            Long last = userMap.putIfAbsent(uID, new Long(-1));
            if (last == null || (last < 0)) {
                if (last == null) {
                    myInfo.addClient(uID);
                    if (dbg > 5) {
                        System.out.println("\tAdded; ConnectsCurrent: "
                                + myInfo.connectsCurrent);
                    }
		    // MS: moved this to the first check-in of the client, because it was possible that the
		    // client did not get a remote reference to this component, even though this component
		    // assumes that the client has one... (this led to false alarms in the reaper about lost
		    // connections that were never established in the first place...)
                    //new Thread() {
                    //    @Override
                    //    public void run() {
                    //        clientConnectReact(uID);
                    //    }
                    //}.start();
                }
            } else {
                // MS: removed this to allow clients to request the same component
                // multiple times
                // this is important if different interfaces are requested
                // throw new AccessControlException(myID +": "+ uID
                // +" already connected");
                //
                // TOOD: the userMap should include the requested interface, so
                // that the component
                // can track what exactly was requested; this is also important
                // for recovery:
                // a user that requested multiple interfaces implemented by the
                // same component
                // will only be able to retain all of them through recovery if
                // the same component
                // can be recovered or another component with all the interfaces is
                // in the system,
                // otherwise since the same heartbeat is shared for all
                // interfaces ADE cannot
                // cannot recover all clients (because there is no one component
                // that has all
                // the interfaces) -- NEED TO keep track of the interface
                // request specifically,
                // and add one heartbeat for each interface, but allow
                // heartbeats to be shared
                // so that we still only have one heartbeat to the component that
                // implements all
                // the interfaces; when the component goes down and cannot be
                // recovered, the
                // various individual heartbeats can then be re-routed to the
                // components that are
                // available in the system and implement the requisite
                // interfaces
	    }
        }
    }

    /**
     * See if a particular user is connected. Cannot be overridden, but can be
     * called by subclasses.
     *
     * @param uid The user ID
     * @return <tt>true</tt> if <tt>uid</tt> is in the <tt>userMap</tt>,
     * <tt>false</tt> otherwise
     */
    final protected boolean isConnected(String uid) {
        return (userMap.containsKey(uid));
    }

    /**
     * Causes the Registry to return stdout/stderr information
     * {@link ade.ADEComponent ADEComponent}.
     *
     * @param lines Number of lines to return from the end (0 == all)
     * @return ArrayList of String with log data
     * @exception RemoteException if the remote invocation fails
     * @exception AccessControlException if the password is bad
     */
    @Override
    final public ArrayList reportLogs(int lines) throws RemoteException,
            AccessControlException {
        if (getUILoggingPath().equals("")) {
            ArrayList tmp = new ArrayList();
            tmp.add("Component was not started with the --uilogging option");
            return tmp;
        }
        return ADEFileHelper.tail(getUILoggingPath(), lines);
    }

    /**
     * Returns information about this component. All connected components, the
     * <tt>ADERegistry</tt> with which this component is registered, the connected
     * clients, and this component can obtain this information.
     *
     * @param credentials one of: <tt>this</tt>, the
     * {@link ade.ADERegistry ADERegistry} with which this component is registered,
     * any client that has a connection, or a locally defined security check
     * (for subclasses)
     * @return An {@link ade.ADEComponentInfo ADEComponentInfo} object
     * @throws RemoteException Thrown if the request fails
     * @throws AccessControlException Thrown if the credentials inadequate
     */
    @Override
    final public ADEComponentInfo requestComponentInfo(Object credentials)
            throws RemoteException, AccessControlException {
        // always follow requests from the own component, the own registry,
        // registered users, or entities that pass the local criteria
        if (credentials.equals(this)
                || credentials.equals(ar)
                || (credentials instanceof String && userMap.containsKey((String) credentials))
                || localRequestComponentInfo(credentials)) {
            if (dbg > 4 && !credentials.equals(this)) {
                System.out.println(myID + ": " + credentials
                        + " requested component info");
            }
            // TODO: need to set the type according to the smallest 
	    // implemented interface by the component that satisfies all the 
	    // constraints under which the component reference was obtained
            return myInfo.duplicate();
        } else {
            if (dbg > 4) {
                System.out.println(myID + ": " + credentials
                        + " request denied!");
            }
            throw new AccessControlException(
                    "Component info request failed based on " + credentials);
        }
    }

    /**
     * Returns host information about this component. All connected clients, all
     * registries, and requests that pass the overridden
     * {@link #localRequestHostInfo} method are allowed access. Can be called by
     * subclasses but cannot be overridden.
     *
     * @param credentials one of: <tt>this</tt>, any
     * {@link ade.ADERegistry ADERegistry} , or a connected client
     * @return an {@link ade.ADEHostInfo ADEHostInfo} object
     * @throws RemoteException
     * @throws AccessControlException if the credentials are unacceptable
     */
    @Override
    final public ADEHostInfo requestHostInfo(Object credentials)
            throws RemoteException, AccessControlException {
        // always follow requests from the own component, any registries,
        // registered users, or locally defined criteria
        if (credentials.equals(this)
                || (credentials.equals(ar))
                || (credentials instanceof String && userMap.containsKey((String) credentials))
                || localRequestHostInfo(credentials)) {
            return myHost.duplicate();
        } else {
            throw new AccessControlException(
                    "Authorization to obtain component info failed based on "
                    + credentials);
        }
    }

    
    /** MS: the new notification mechanism
     * this will schedule a notification with the component given by "ref" -- note that a heartbeat to the component has to be in place for this to work
     *
     * @param functionvalues a list of constant functions (with no args) in the remote component to get desired values
     * @param conditions a list of conditions that have to be meet for the delivery
     * @param callbackmethodstring the methodstring of the callback function (needs to be in the component's interface)
     *        -- the return values from funtionvalues have to match the types of the args for the callback functions
     * @return true if successfully scheduled, false otherwise
     * @throws AccessControlException Thrown if the credentials inadequate
     *
     * returns "true" if the request can be carried out by the component and is scheduled, "false" otherwise
     */
    final protected boolean scheduleComponentNotification(Object ref, String[] functionvalues,String[][] conditions, String callbackmethodstring) 
                            throws ADEReferenceException {
	try {
	    if (ref == null || !(ref instanceof Heartbeat)) {
		throw new ADEReferenceException("Wrong type for remote reference!");
	    }
            ADENotification note = new ADENotification(functionvalues,conditions,callbackmethodstring);
	    Heartbeat hb = (Heartbeat) ref;
            // TODO: we should do some checking of argument types that they match up or can be coerced...
            // i.e., for each functionvalue, get the return type and then check if in the order of the return values
            // matches up with the arguments of the callback function
            hb.rct.remoteCall(0,"requestNotification", hb.toComponent, myID, myID, this, note);
            // TODO: store the notification locally for future reference
            
	    return true;
        } catch (Exception e) {
	    System.err.println("Could not schedule component notification due to :" + e);
	    return false;
	}
    } 

    /** MS: the new notification mechanism
     * the idea is to support streaming data at regular frequencies as well as condition-based event-driven method calls
     * through a unified interface
     *
     *    [list-of-values-to-deliver-or-keywords condition-for-delivery function-name-to-call]
     *
     * where values-to-deliver-or-keywords can either be the empty list for no args, "component" for only component args, or a keyword
     * known to the component providing the information (the component probably should advertise those...) 
     *
     * some examples:
     * 
     *    [[laserreadings] [[every 10]] updatereadings] which will call the updatereadings method in the client at 10Hz (if possible) 
     *                                        with the calling component as the first and laserreadings as the second argument
     *    [[] [[obstacle-in-front true][open-space right]] turnright] which will call the turnright function with no args
     *    [component [[obstacle-in-front true][open-space right]] turnright] which will call the turnright function with only
     *                                                         the component args
     *
     */
     public void requestNotification(final String uid, Object credentials, final ADEComponent requester, final ADENotification note)
	throws AccessControlException, RemoteException {
	// check if the user is registered
        if (!userMap.containsKey(uid)) {
            throw new AccessControlException("Authorization to request notification failed, user unkown.");
	}
	else {
	    // then check if the user already has notifications scheduled...
            ArrayList<ADENotification> requests = notifications.get(requester);
	    if (requests == null) {
                // generate a new remote call timer to the component
                try {
                    note.rct = new ADERemoteCallTimer(ADEGlobals.DEF_HBPULSE, requester);
                    note.rct.setAllMethods(ADEGlobals.getADEComponentMethods());
                    HashMap<String, ADEMethodConditions> hm = 
                            (HashMap<String, ADEMethodConditions>) note.rct.remoteCall("requestMethods", requester, 
                                                                                     myID, myID,requester.getClass().getName());
                    // pass on only the method strings, the RCT will also set
                    // the method conditions
                    note.rct.setAllMethods(new ArrayList<String>(hm.keySet()));
                } catch (ADERequestMethodsException are) {
                    System.err.println("Error obtaining method information from remote component " + are);
                } catch (ADEException ace) {
                    System.err.println(myID + ": ADERemoteCallTimer failed:\n\t" + ace);
                    throw new RemoteException("Failure to produce call timer in requestNotification");
                }
                // create a new entry in the notification table    
                requests = new ArrayList<ADENotification>();
                requests.add(note);            
                notifications.put(requester, requests);
            }
            // there are already notifications scheduled
            else {
                // grab the rct from the first request so we can make calls
                note.rct = requests.get(0).rct;
                // check if this particular notification is already scheduled
                int i;
                if ((i = requests.indexOf(note)) >= 0) {
                    // override the existing request, i.e., stop the current one, and start the new one
                    ADENotification oldnote = requests.get(i);
                    // make it inactive
                    oldnote.active = false;
                    // set the new one instead
                    requests.set(i, note);                    
                }
                else {
                    // now add the request
                    requests.add(note);
                }
            }
            // assemble the necessary local methods
            final Method[] tocall = new Method[note.valuefunctions.length];
            for(int i=0; i< note.valuefunctions.length; i++) {
                try {
                    tocall[i] = getClass().getMethod(note.valuefunctions[i]);
                    // make sure the user has permissions to call the method
                    if (!isAllowableMethod(uid, ADEGlobals.getMethodString(tocall[i]))) {
                        throw new AccessControlException("Component is not allowed to call method " + tocall[i]);
                    }
                } catch(NoSuchMethodException nsme) {}
            }
            //
            final ADEComponentImpl thicomponent = this;
            // now parse the conditions and set up a timer thread
            for(String[] condition : note.conditions) {
                if (condition[0].equals("every")) {
                    // second argument is the loop time in milliseconds
                    final int sleeptime = Integer.parseInt(condition[1]);
                    final Object[] args = new Object[tocall.length];                                
                    Thread runner = new Thread() {
                        public void run() {
                            // keep running while the notification is active
                            while(note.active) {
                                // this should take the actual elapsed time into account
                                try {
                                    sleep(sleeptime);
                                } catch (InterruptedException ie) {}
                                // gather all the values by calling the local functions
                                for(int i=0; i< tocall.length; i++) {
                                    try {
                                        args[i] = tocall[i].invoke(thicomponent);
                                    } catch(Exception e) {
                                        System.err.println("Exception calling component functions in Notification: " + e);
                                        note.active = false;
                                    }
                                }
                                // now use direct one-shot calling to deliver the values
                                // use the
                                try {
                                    note.rct.remoteCall(note.callbackfunctionname,requester,args);
                                } catch(Exception e) {
                                    System.err.println("Problem requesting callback: " + e);
                                    note.active = false;
                                }
                            }
                        }
                    };
                    // set the request to active
                    note.active = true;
                    runner.start();
                    note.runner = runner;
                }
                else if (condition[0].equals("once")) {
                    final Object[] args = new Object[tocall.length];                                
                    Thread runner = new Thread() {
                        public void run() {
                            // gather all the values by calling the local functions
                            for(int i=0; i< tocall.length; i++) {
                                try {
                                    args[i] = tocall[i].invoke(thicomponent);
                                } catch(Exception e) {
                                    System.err.println("Exception calling component functions in Notification: " + e);
                                    note.active = false;
                                }
                            }
                            // now use direct one-shot calling to deliver the values
                            // use the
                            try {
                                note.rct.remoteCall(note.callbackfunctionname,requester,args);
                            } catch(Exception e) {
                                System.err.println("Problem requesting callback: " + e);
                                note.active = false;
                            }
                            // set to inactive when exiting
                            note.active = false;
                        }
                    };
                    runner.start();
                    note.runner = runner;
                }

                // else consider the various other options
            }
	}
    }

    /** removes a notification if it exists */
    public void cancelNotification(final String uid, Object credentials, final ADEComponent requester, final ADENotification note)
    	throws AccessControlException, RemoteException {
	// check if the user is registered
        if (!userMap.containsKey(uid)) {
            throw new AccessControlException("Authorization to request notification failed, user unkown.");
	}
	else {
	    // then check if the user already has notifications scheduled...
            ArrayList<ADENotification> requests = notifications.get(requester);
            if (requests == null) {
                requests.remove(note);
            }
        }            
    }

     
//***********************************************************************************************************************

    // MS: the interface for requesting accessible methods for a componenttype from
    // the registry
    // NOTE: it is essential that this go through the registry for the case that
    // the component
    // locally through its classloader does not have access to the component
    // interfaces
    // since the registry will have to have access to it
    final protected HashMap<String, ADEMethodConditions> getComponentMethods(
            String componenttype) throws ADETimeoutException, ADEException {
        return (HashMap<String, ADEMethodConditions>) rctReg.remoteCall(
                "requestMethods", ar, myID, myPassword, componenttype);
    }

    // this method can either be called on the remote component (e.g., from
    // within the
    // heartbeat object) or can be sent to the registry asking the registry to
    // provide the
    // information; in the latter case, the registry
    //
    /**
     * Return an {@link java.util.HashMap HashMap} of method string as key and
     * ADEMethodConditions (pre-conditions, post-conditions, and failure
     * conditions) associated with the method as values for all methods the user
     * is allowed to call. Unless otherwise specified, the default behavior is
     * to allow access to <b>all</b> the remote methods defined in all
     * implemented interfaces. This is enforced by the
     * {@link ade.ADEComponentImpl#call call} method, which uses an
     * {@link ade.ADERemoteCallTimer ADERemoteCallTimer} within a
     * <i>pseudoreference</i> to perform the actual method calls.
     *
     * @param uid The user's ID
     * @param credentials Confirmation information
     * @param componenttype the type of component (i.e., interface) for which the
     * methods are requested
     * @return The permitted method names
     * @throws RemoteException If the request fails
     * @throws AccessControlException If the user does not have adequate
     * permissions
     */
    @Override
    final public HashMap<String, ADEMethodConditions> requestMethods(String uid,
            Object credentials, String componenttype)
            throws ADERequestMethodsException, AccessControlException {
        if (debugMeths) {
            System.out.println(myID + ": " + uid + " requesting methods...");
        }
        if (componenttype.equals("ade.ADERegistry")) {
            throw new ADERequestMethodsException(
                    "Function not available for ADERegistry.");
        }

        // check access control -- if a component is directly called, then this
        // client needs to be
        // registered with it; if the registry is called, then the client needs
        // to be registered
        // with the registry...
        if (!(this instanceof ADERegistryImpl)
                && !(credentials instanceof String && userMap.containsKey((String) credentials))) {
            throw new AccessControlException(
                    "Not authorized to access methods.");
        }

        // check if this runs as a registry
        boolean asRegistry = (this instanceof ADERegistryImpl);

        try {
            // get the interface for the componenttype for which the user wants the
            // accessible methods
            // note that the requested componenttype must be in the interface
            // hierarchy of this component
            // otherwise the empty list will be returned...
            Class icomponent = Class.forName(componenttype); // get the particular
            // interface
            // build a list of all implemented interfaces and filter for user
            // permissions and component type
            // we always have exactly one top-level interface; but if run
            // through the registry
            // we don't want to get the registry interface, so get that of the
            // component instead
            Class toplevel = (asRegistry ? icomponent : getClass().getInterfaces()[0]);
            // build hierarchy list for traversal put the top-level in
            ArrayList<Class> iclasses = new ArrayList<Class>();
            iclasses.add(toplevel);
            HashSet<Method> allowablemethods = new HashSet<Method>();
            // get the basic ADEcomponent and Remote interfaces
            Class aderegistry = Class.forName("ade.ADERegistry");
            Class remote = Class.forName("java.rmi.Remote");
            // recursively step through the interface hierarchy and collect each
            // interface explicitly
            int i = 0;
            while (i < iclasses.size()) {
                Class c = iclasses.get(i);
                // check if it extends remote and is not a super class of the
                // given component type
                if (remote.isAssignableFrom(c) && c.isAssignableFrom(icomponent)) {
                    // System.out.println("Adding methods from " + c);
                    // add the allowable methods in
                    for (Method m : c.getMethods()) {
                        // TODO: need to apply access control to eligible method
                        // here
                        allowablemethods.add(m);
                    }
                } // otherwise remove the methods of this class
                else {
                    // System.out.println("Removing methods from" + c);
                    for (Method m : c.getMethods()) {
                        allowablemethods.remove(m);
                    }
                }
                // now expand this class if possible and add the super classes
                // to the list
                for (Class superc : iclasses.get(i).getInterfaces()) {
                    iclasses.add(superc);
                }
                i++;
            }
            // now remove ADERegistry methods, will automatically remove
            // ADEComponent methods
            for (Method m : aderegistry.getMethods()) {
                allowablemethods.remove(m);
            }

            // build the return table
            HashMap<String, ADEMethodConditions> retlist = new HashMap<String, ADEMethodConditions>();
            // now collect all methods from the remaining interfaces
            for (Method m : allowablemethods) {
                String mstring = ADEGlobals.getMethodString(m);
                // Note that the registry cannot do fine-grained access control
                // on general component types
                // only an instantiated component will be able to further constrain
                // access, hence the two
                // methods might not coincide if the component implements special
                // access restrictions for
                // the given user id
                // Hence, if this is a registry, the local isAllowableMethod (in
                // the registry) will
                // NOT be called
                if (asRegistry || isAllowableMethod(uid, mstring)) {
                    retlist.put(mstring, null);
                    // now get whatever annotations are available
                    // NOTE that if there are more than one (if that's possible,
                    // not sure)
                    // the last one will get stored only!
                    for (Annotation annotation : m.getAnnotations()) {
                        if (annotation instanceof ADEMethodConditions) {
                            retlist.put(mstring, (ADEMethodConditions) annotation);
                        }
                    }
                }
            }

            // check if we have any methods left and if this is not a registry
            // (then we called
            // it on setup of the remote connection, and that's not good...), so
            // we
            // signal that there is a problem... otherwise just return the empty
            // set...
            // (as this was on exploratory through the registry)
            if (!asRegistry && retlist.keySet().isEmpty()) {
                System.err.println("NOTHING LEFT...");
                throw new ADERequestMethodsException(
                        "No methods for user to call.");
            }
            return retlist;
        } catch (Exception e) {
            if (dbg > 7 || debugMeths) {
                System.out.println(myID + ": cannot get Remote");
            }
            throw new ADERequestMethodsException(
                    "Problem building method list: " + e);
        }
    }

    /** finds a method in this class with the given name and parameter signature, 
     *  possibly coerced parameters and returns the method (this is locally cached for
     *  faster access)
     */
    private Method getLocalMethod(String mn, Class[] remoteParamTypes) {
	String key = ADEGlobals.getMethodString(mn, remoteParamTypes);
        // check if the method is in the cache
        Method quick = localMethodsFast.get(key);
        if (quick != null) {
            //System.out.println("+++ FAST ACCESS to " + mn);
            return quick;
        }

	// get the method to invoke and its return type
	try {
	    Method m = getClass().getMethod(mn,remoteParamTypes);
            localMethodsFast.put(key,m);
            return m;
	} catch (Exception e) {
	    // limit the search to methods with the same name
	    ArrayList<Method> matchmns = new ArrayList<Method>();
	    Method[] ms = getClass().getMethods();
	    for (Method m : ms) {
		if (mn.equals(m.getName())) {
		    matchmns.add(m);
		}
	    }
	    // set up the data we'll use in the loop
	    Class[] ptypes;
	    for (Method m : matchmns) {
		//System.out.println("CHECKING " + m);
		// short-circuit if number of params is different
		ptypes = m.getParameterTypes();
		if (ptypes.length != remoteParamTypes.length) {
		    continue;
		}
		//System.out.println("==> " + ADEGlobals.getMethodString(m));
		// there is a function that produces the method strings directly from classes
		if (key.equals(ADEGlobals.getMethodString(m))) {
                    localMethodsFast.put(key,m);
		    return m;
		}
		// MS: fixed the conversion/coercion problem -- this has to be done together
		// as there can be any combination of "auto-boxed", i.e., converted and coerced parameters...
		boolean found = true;
		for (int i = 0; i < ptypes.length; i++) {
		    //System.out.println("  -- " + ADEGlobals.primitiveToObject(ptypes[i]));
		    if (!ADEGlobals.primitiveToObject(ptypes[i]).isAssignableFrom(remoteParamTypes[i])) {
			found = false;
			break;
		    }
		}
		if (found) {
		    localMethodsFast.put(key,m);
		    return m;
		}
	    }
	    return null;
	}
    }

    /**
     * a request on behalf of a registry for user uid to call this component's method given by methodname on the args
     *
     * @param uid ID of the client requesting the accessible methods of this
     * @param credentials the registry's ref
     * @param methodname the name of the method to be called
     * @param args the arguments for the method
     * component
     * @return the results from the method invokation
     */
    @Override
    final public Object requestMethodCall(String uid, Object credentials, String methodname, Object[] args) 
            throws RemoteException, AccessControlException {
        // check access control
        if (!(credentials instanceof ADERegistry)) {            
            throw new AccessControlException("Not authorized to request call.");
        }

	Class[] paramtypes = new Class[args.length];
	for(int i = 0; i < args.length; i++) {
	    paramtypes[i] = args[i].getClass();
        }

	// now find an applicable method -- 
        // TODO: allow for caching of methods if that's possible as done in the RCT
	Method m = getLocalMethod(methodname, paramtypes);
	if (m != null) {
	    // check that the method is allowable for the user
	    if (isAllowableMethod(uid, ADEGlobals.getMethodString(m))) {
		try {
		    // local invoke
		    return m.invoke(this,args);
		} catch(Exception e) {
		    throw new RemoteException("Problem invoking method " + e);
		}
	    }
	    else
		throw new AccessControlException("Access to requested method is not allowed for user " + uid);
	}
	else
	    throw new RemoteException("Requested method not found ");
    }

    /**
     * Each component can implement some user specific access control here this can
     * be superseded by an access control tabel to be implemented still...
     *
     * @param uid ID of the client requesting the accessible methods of this
     * component
     * @return
     */
    protected boolean isAllowableMethod(String uid, String methodstring) {
        // by default allow all remote methods implemented by the components that
        // met the
        // basic ADE inheritance criteria
        return true;
    }

    /**
     * This can be overridden if components want to volunteer their info based on
     * other credentials.
     *
     * @param credentials subclass defined security check
     * @return <tt>true</tt> if credentials pass, <tt>false</tt> otherwise
     */
    protected boolean localRequestComponentInfo(Object credentials) {
        return false;
    }

    /**
     * This can be overridden if components want to volunteer their host info based
     * on other credentials.
     *
     * @param credentials Subclass defined security check
     * @return <tt>true</tt> if credentials pass, <tt>false</tt> otherwise
     */
    protected boolean localRequestHostInfo(Object credentials) {
        return false;
    }

    /**
     * The method that will keep the client connection to this component alive.
     * Client code is required to call this method periodically in order to not
     * be disconnected. These calls are built into the {@link ade.ADEComponentImpl
     * ADEComponentImpl} object. For security purposes, this method is final and
     * cannot be overridden.
     *
     * @param uid The client's ID
     * @return The {@link ade.ADEGlobals.ComponentState ComponentState} of this
     * ADEComponent
     * @throws RemoteException If the request fails
     * @throws AccessControlException If the user does not have adequate
     * permissions
     */
    @Override
    public final ADEGlobals.ComponentState updateConnection(final String uid)
            throws RemoteException, AccessControlException {
        if (dbg > 8) {
            System.out.println(myInfo.name + ": in updateConnection(" + uid
                    + ")");
        }
        Long ms = new Long(System.currentTimeMillis());
	Long lasttime;

        if ((lasttime = userMap.get(uid)) != null) {
            synchronized (userMap) {
		// if the entry is negative, then the client is checking in for the first time, so
		// run clientConnectReact confirming that the client as established the heartbeat
		if (lasttime < 0) {
		    if (dbg > 8) {
			System.out.println("; client " + uid + " checking in for the first time");
		    }
                    // use a thread, so that the client is not held up
                    new Thread() {
                        @Override
                        public void run() {
                            clientConnectReact(uid);
                        }
                    }.start();
		}
                userMap.put(uid, ms);
                if (dbg > 8) {
                    System.out.println("; time=" + ms.longValue());
                }
            }
        } else {
            throw new AccessControlException("Unknown user " + uid
                    + " updating connection to " + myInfo.getKey());
        }
        return (myInfo.state);
    }

    /**
     * Update the component's information performed by the component's registry
     *
     * @param list The update information list
     * @param credentials Access control object
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the credentials are not sufficient
     */
    @Override
    public final void updateComponentInfo(LinkedList<ADEMiniComponentInfo> list,
            Object credentials) throws RemoteException, AccessControlException {
        ADEMiniComponentInfo amsi;
        Heartbeat hb;

        // TODO: do we want to only allow our registry to do this?
        if (!(credentials.equals(ar))) {
            throw new AccessControlException("Not authorized to update.");
        }
        if (myheartbeats.size() < 1) {
            // System.out.println(myID
            // +": ******** no clients! skipping update");
            return;
        }
        // System.out.println(myID +": ******** updating "+ list.size()
        // +" component; have "+ myheartbeats.size() +" hbs");
        while (list.size() > 0) {
            amsi = list.removeFirst();
            // System.out.println(myID +": ******** updating "+ amsi.id
            // +" STATE");
            if (myheartbeats.containsKey(amsi.id)) {
                hb = myheartbeats.get(amsi.id);
                // heck, we got the heartbeat, might as well set the state even
                // when we don't have to notify...
                hb.setRecState(amsi.recState);
                hb.toState = amsi.state;
                // System.out.println(myID +": ******** set "+ amsi.id
                // +" STATE to "+ hb.toState +"; checking notify...");
                if (amsi.recState.shouldNotify() && hb.notify) {
                    System.out.println(myID
                            + ": ******** CALLING componentNotify for " + amsi.id);
                    final Heartbeat hbfin = hb;
                    final ADEGlobals.RecoveryState rs = amsi.recState;
                    new Thread() {
                        @Override
                        public void run() {
                            componentNotify(hbfin, rs);
                        }
                    }.start();
                    /*
                     * this is just for debugging... } else if (!hb.notify) {
                     * //System.out.println(myID +": ******** notification not
                     * set"); } else if (!amsi.recState.shouldNotify()) {
                     * //System.out.println(myID +": ******** notification not
                     * required for "+ amsi.state);
                     */
                }
            }
        }
    }

    /**
     * Used to shut down the component. Can be called by any object that has a
     * reference, but will only be active if the credentials are appropriate.
     *
     * @param credentials at this point, only the component itself or the
     * {@link ade.ADERegistryImpl ADERegistry} this ADEComponent is registered with
     * is allowed shutdown capability.
     * @return <tt>true</tt> if credentials are acceptable, <tt>false</tt>
     * otherwise
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the credentials are unacceptable
     */
    @Override
    final public boolean requestShutdown(Object credentials)
            throws RemoteException, AccessControlException {
        boolean shutdown = false;
        if (dbg > 3) {
            System.out.println(myInfo.getKey() + ": SHUTDOWN REQUEST RECEIVED");
        }
        // check permission, always follow requests from own registry
        if (credentials.equals(ar) || credentials.equals(this)) {
            shutdown = true;
            registryenforced = true;
        } else {
            if (dbg > 4) {
                System.out.println(myID + ": CHECKING LOCALSHUTDOWN...");
            }
            // check for local shutdown
            if (localrequestShutdown(credentials)) {
                shutdown = true;
                registryenforced = false;
            } else {
                if (dbg > 4) {
                    System.out.println(myID + ": LOCALSHUTDOWN REJECTED");
                }
            }
        }
        if (shutdown) {
            new Thread() {
                @Override
                public void run() {
                    yield(); // allows the parent method to complete
                    // the normal exit process will call shutdownADEComponent
                    // due to the shutdownHook
                    // shutdownADEComponent();
                    Runtime.getRuntime().exit(ADEGlobals.ExitCode.OK.code());
                }
            }.start();
            if (dbg > 1) {
                System.out.println(myInfo.getKey() + ": SHUTTING DOWN");
            }
        }
        return shutdown;
    }

    /**
     * Demands (unclean) component shutdown; no cleanup, finalize, etc. actions are
     * performed, just a {@link java.lang.Runtime#halt} call. This is really
     * here only for testing (i.e., simulated failure). <p> Proper credentials
     * are required to succeed; acceptable credentials consist of one of the
     * following: <ol> <li>This component (i.e., a reference to <tt>this</tt>)</li>
     * <li>The {@link ade.ADERegistry ADERegistry} with which this component is
     * registered</li> <li>A value of <tt>true</tt> returned by overriding the
     * {@link #localrequestKill} method</li> </ol>
     *
     * @param credentials Security permissions
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    final public void killComponent(Object credentials) throws RemoteException,
            AccessControlException {
        // check the credentials and halt if acceptable; we accept self-kills,
        // registry kills, or something that passes the "localrequestKill"
        if (credentials.equals(ar) || credentials.equals(this)
                || localrequestKill(credentials)) {
            System.out.println(myID + ": UNCLEAN SHUTDOWN NOW");
            Runtime.getRuntime().halt(ADEGlobals.ExitCode.KILL.code());
        }
        throw new AccessControlException("Failed kill due to: " + credentials);
    }

    /**
     * Components must override this method to give other <tt>ADEComponent</tt>s
     * (i.e., non-ADERegistry or not self-initiated) permission to halt this
     * component. The default action is to only allow self requested kills.
     *
     * @param credentials Security permissions
     */
    protected boolean localrequestKill(Object credentials) {
        if (credentials.equals(this)) {
            return true;
        }
        return false;
    }

    /**
     * Constructor. An <tt>ADEComponent</tt> is not allowed to be instantiated by
     * another program unless <tt>entryright</tt> is <tt>true</tt>, which means
     * it <b>must</b> be started in a new Java virtual machine. Configuration
     * options that the user is given access to are passed using command line
     * switches (processed by the {@link #parseadditionalargs} method, with
     * display descriptions specified in {@link #additionalUsageInfo}). Note
     * that {@link #finishedInitialization} must be called to register an
     * <tt>ADEComponent</tt> with an {@link ade.ADERegistryImpl ADERegistry}.
     */
    protected ADEComponentImpl() throws RemoteException {
        super();
        if (!entryright) {
            StringBuilder sb = new StringBuilder();
            sb.append("ADEComponentImpl cannot be instantiated directly!\n");
            sb.append("This mechanisms is to ensure that they will run as\n");
            sb.append("independent OS processes for various ADE mechainsms");
            sb.append("such as automatic error recovery to work properly.\n");
            sb.append("\nCall 'ADEComponentImpl.main(String args[])' with the\n");
            sb.append("command line arguments from your derived class.\n\n");
            System.err.println(sb.toString());
            System.exit(ADEGlobals.ExitCode.ARG_PARSE.code());
        }
        // now set entryright to false to indicate that the constructor was
        // called
        // will be checked in main()
        entryright = false;
        StringBuilder sb = new StringBuilder(); // concatenating various strings
        Registry r;

        // set the unique ID for this JVM (if possible, use the process ID)
        // if we can't get a pid, the time returned by the OS should be
        // sufficient (as we're only concerned with this host)
        jvmpid = System.getProperty("process.id");
        if (jvmpid == null || jvmpid.equals("")) {
            jvmpid = "pid-" + System.currentTimeMillis();
        }

        // parse the command line arguments
        if (dbg > 5) {
            System.out.print(prg + ": in Constructor; args are:\n\t[");
            for (String arg : myargs) {
                System.out.print(" " + arg);
            }
            System.out.println("]");
        }
        myInfo = parsecommandline(myargs);
        myInfo.state = ADEGlobals.ComponentState.INIT;
        // set the type, i.e., the top-level interface implemented by this class
        // which is the class name without the last four letters "Impl"
        // this was already check in main()
        myInfo.type = getClass().getName();
        myInfo.type = myInfo.type.substring(0, myInfo.type.length()
                - ADEGlobals.componentimpl.length());

        // set various default values
        // mark it as registry
        if (this instanceof ADERegistryImpl) {
            myInfo.isregistry = true;
        }
        if (myInfo.registryIP == null) {
            myInfo.registryIP = ADEGlobals.DEF_REGIP;
        }
        if (myInfo.registryPort == -1) {
            myInfo.registryPort = ADEGlobals.DEF_REGPORT;
        }
        if (myInfo.registryType == null) {
            myInfo.registryType = ADEGlobals.DEF_REGTYPE;
        }
        // set the registry default name only if this is not a registry, as
        // otherwise it will connect to itself
        if (myInfo.registryName == null && (!(this instanceof ADERegistryImpl))) {
            myInfo.registryName = ADEGlobals.DEF_REGNAME;
        }

        // NOTE: for components this needs to remain null if the registry is
        // supposed to assign a component name
        if (myInfo.name == null && (this instanceof ADERegistryImpl)) {
            myInfo.name = ADEGlobals.DEF_REGNAME;
        }
        if (myInfo.port == -1) {
            myInfo.port = ADEGlobals.DEF_COMPONENTPORT;
        }
        if (myInfo.connectsAllowed == -1) {
            myInfo.connectsAllowed = ADEGlobals.DEF_MAXCONN;
        }
        myInfo.connectsCurrent = 0;
        if (myInfo.heartBeatPeriod == -1) {
            myInfo.heartBeatPeriod = ADEGlobals.DEF_HBPULSE;
        }
        // check the access rights, assume "any" by default (i.e., everybody
        // can connect to this component)
	// MS: TODO: FIX: this should not probably be *none* instead, so that access control has to be specified
        if (myInfo.userAccess == null) {
            myInfo.userAccess = new HashSet<String>();
            myInfo.userAccess.add(ADEGlobals.ALL_ACCESS);
        }
        // set my ID, this will be only temporary if the registry is supposed
        // to assign a name which in turn depends on whether "myName" is null
        myID = myInfo.getKey();

        // compare my IP address to "useIP" if set
        try {
            if (dbg > 3) {
                System.out.println("Establishing my IP address");
            }
            boolean gotit = false;
            Enumeration e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements() && !gotit) {
                if (dbg > 5) {
                    System.out.println("Got network interface");
                }
                NetworkInterface netface = (NetworkInterface) e.nextElement();
                Enumeration e2 = netface.getInetAddresses();

                // TODO: this needs to be made more sophisticated... (in line
                // with current startup shell scripts)
                while (e2.hasMoreElements()) {
                    if (dbg > 5) {
                        System.out.println("Got network interface's internet address");
                    }
                    InetAddress ip = (InetAddress) e2.nextElement();
                    if (dbg > 5) {
                        System.out.println("Found IP address: "
                                + ip.getHostAddress());
                    }
                    if (useIP != null && ip.getHostAddress().equals(useIP)) {
                        if (dbg > 3) {
                            System.out.println("Found IP address to be used: "
                                    + ip.getHostAddress());
                        }
                        myInfo.host = ip.getHostAddress();
                        gotit = true;
                        break;
                    }
                    // start setting the IP just in case
                    if (!ip.isLoopbackAddress()) {
                        myInfo.host = ip.getHostAddress();
                    } // set local address only if we don't have any yet...
                    else if (myInfo.host == null) {
                        myInfo.host = ip.getHostAddress();
                    }
                }
            }
            if (dbg > 3) {
                System.out.println(myID + " IP=" + myInfo.host);
            }
        } catch (Exception e) {
            System.err.println("Could not determine IP address:");
            System.err.println(e);
            System.exit(ADEGlobals.ExitCode.IP.code());
        }

        // check if the host IP is in the list of restricted hosts
        if (!myInfo.onlyonhosts.isEmpty()
                && !myInfo.onlyonhosts.contains(myInfo.host)) {
            System.err.println("The component is running on a host that is not in the list of allowed hosts... quitting.");
            System.exit(ADEGlobals.ExitCode.HOST_NOT_ALLOWED.code());
        }

        // Recreate command line invocation... assume no jar file or
        // classpath, could be added. Leave out command line arguments
        // for now, the registry can substitute its own host for args[0]
        Properties p = System.getProperties();
        String filesep = p.getProperty("file.separator");
        String adir, adepath = p.getProperty("user.dir");
        if (!adepath.endsWith(filesep)) {
            adir = adepath + filesep;
        } else {
            adir = adepath;
        }
        // property returns "jre" on the end; remove for root javadir
        myInfo.startdirectory = adir;
        myInfo.username = p.getProperty("user.name");

	// create the infrastructure remote call timer used for all infrastructure calls, in particular, 
	// the initial call to the registry
	if (rctReg == null) {
	    try {
		// TODO: should the timeout really be the heartbeat period?
		Class aderegistryclass = Class.forName("ade.ADERegistry");
		rctReg = new ADERemoteCallTimer(ADEGlobals.DEF_HBPULSE, aderegistryclass);
		rctReg.setName(myID + "->ADERegistry");		
		// registries have unlimited access to other registries
                // TODO: only get the remote methods as the other ones don't matter!!!
		if (this instanceof ADERegistryImpl) {
		    ArrayList<String> regmeths = new ArrayList<String>();
		    for (Method m : aderegistryclass.getMethods()) {
			regmeths.add(ADEGlobals.getMethodString(m));
		    }
		    rctReg.setAllMethods(regmeths);
		} // regular components have only limited infrastructure access to
		// other components
		else {
		    rctReg.setAllMethods(ADEGlobals.getADERegistryMethods());
		}
	    } catch (ADERequestMethodsException are) {
		System.err.println("Error obtaining method information for ADERegistry "
				   + are);
		System.exit(ADEGlobals.ExitCode.RCT_CREATION.code());
	    } catch (Exception e) {
		System.err.print(myID
				 + ": Exception creating remote call timer");
		System.err.println(" to ADERegistry " + myInfo.registryName);
		System.err.println(e);
		System.exit(ADEGlobals.ExitCode.RCT_CREATION.code());
	    }
	}

        // KRAMER: why treat the registry differently?
        if (!(this instanceof ADERegistryImpl)) {
            myInfo.interfaces = getInterfaces();
        }

        // now that we have as much component info as possible (particularly the
        // IP address to use), create the host information
        myHost = new ADEHostInfo(true);
        myHost.hostip = myInfo.host;
        myHost.adehome = myInfo.startdirectory;

        // having established the host, pause with the host-stuff for a moment
        // to
        // remove the startdirectory and any prefix thereof
        // (..../aderoot/jars/core.jar) from the classpath (needed host for path
        // sep).
        // the start directory (i.e., the host's adehome as set above) from the
        // classpath
        // will be added automatically later, and the references are easier read
        // and
        // are more portable to other computers if they are relative rather than
        // absolute.
        if (myInfo.userclasspath != null) {
            String adehomeWithFileSepOnEnd = myInfo.startdirectory;
            if (!adehomeWithFileSepOnEnd.endsWith(myHost.filesep)) {
                adehomeWithFileSepOnEnd = adehomeWithFileSepOnEnd
                        + myHost.filesep;
            }
            // for debugging:
            // System.out.println("ADEHOME with path sep on end:  " +
            // adehomeWithFileSepOnEnd);

            String[] classpathPieces = myInfo.userclasspath.split(myHost.pathsep);
            for (int i = 0; i < classpathPieces.length; i++) {
                // for debugging: System.out.println("former classpath = " +
                // classpathPieces[i]);
                // trim beginning adehome
                if (classpathPieces[i].startsWith(adehomeWithFileSepOnEnd)) {
                    classpathPieces[i] = classpathPieces[i].substring(
                            adehomeWithFileSepOnEnd.length()).trim();
                } // also check if the path is the same as adehome, save for a
                // missing "/".
                else if (adehomeWithFileSepOnEnd.equals(classpathPieces[i]
                        + myHost.filesep)) {
                    classpathPieces[i] = ""; // will be removed after the
                    // iteration.
                }
                // for debugging: System.out.println("new classpath = " +
                // classpathPieces[i]);
            }

            // now that cleared up the pieces, re-add them (still in order, but
            // avoiding duplicates):
            HashSet<String> myclasspathAlreadyAddedElements = new HashSet<String>();
            StringBuilder classpathReBuilder = new StringBuilder();
            for (String eachClasspathPiece : classpathPieces) {
                if (eachClasspathPiece.length() > 0) {
                    if (!myclasspathAlreadyAddedElements.contains(eachClasspathPiece)) {
                        classpathReBuilder.append(eachClasspathPiece);
                        classpathReBuilder.append(myHost.pathsep);
                        myclasspathAlreadyAddedElements.add(eachClasspathPiece); // so
                        // that
                        // don't
                        // repeat.
                    }
                }
            }
            if (classpathReBuilder.length() > 0) {
                classpathReBuilder.setLength(classpathReBuilder.length()
                        - myHost.pathsep.length());
                // remove trailing path separator
            }
            myInfo.userclasspath = classpathReBuilder.toString();
        }
        if (dbg > 5) {
            myInfo.print();
        }

        // after the classpath hietus, resume with setting up the host:...

        myHost.sshusername = myInfo.username;
        adir = p.getProperty("java.home");
        // java might be in a couple places
        if (adir.indexOf("jdk") >= 0 && adir.endsWith("jre")) {
            adir = adir.substring(0, adir.length() - 4);
        }
        adir = adir + filesep;
        if (myHost.hostos.osname().indexOf("Windows") >= 0) {
            // adir = adir.replace("\\", "\\\\");
            adir = adir.replace(" ", "\\ ");
            myHost.javahome = adir;
            myHost.javabin = adir + "bin\\java";
            myHost.compiler = adir + "bin\\javac";
        } else {
            myHost.javahome = adir;
            myHost.javabin = adir + "bin/java";
            myHost.compiler = adir + "bin/javac";
        }

        // for shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                shutdownADEComponent();
            }
        });
    }

    /**
     * Determine the interfaces this component implements and put them in a
     * {@link java.util.HashSet HashSet}.
     */
    private HashSet<String> getInterfaces() {
        if (dbg > 5) {
            System.out.println(myInfo.type + ": in getInterfaces()");
        }
        Class[] dec_ifaces = getClass().getInterfaces();
        ArrayList<Class> all_ifaces = new ArrayList<Class>();
        HashSet<String> ifaces = new HashSet<String>();
        //HashMap<String, ADEComponentInfo> nameMap;
        String iname;

        // we only want to include the RMI interfaces, so we make sure
        // each one we add implements Remote
        Class remote;
        try {
            remote = Class.forName("java.rmi.Remote");
        } catch (Exception e) {
            if (dbg > 5) {
                System.err.println(myInfo.type
                        + ": cannot get Remote; cannot add interfaces");
            }
            return null;
        }
        // get the declared interfaces (only Remote)
        if (dbg > 8) {
            System.out.println(myInfo.type + ": getting declared interfaces:");
        }
        for (Class c : dec_ifaces) {
            if (remote.isAssignableFrom(c)) {
                iname = c.getName();
                if (dbg > 8) {
                    System.out.println("\tFound declared " + iname);
                }
                // perhaps filter out some other classes...
                if (iname.equals(remote.getName())
                        || iname.equals("ade.ADEComponent")) {
                    // skip it
                } else {
                    all_ifaces.add(c);
                }
            }
        }
        // iterate over the declared interfaces, going up the hierarchy
        // to get other implemented interfaces (must be Remote)
        // System.out.println(myID +": getting other interfaces:");
        for (int i = 0; i < all_ifaces.size(); i++) {
            Class c = all_ifaces.get(i);
            if (dbg > 8) {
                System.out.println("\tExamining interface " + c.getName());
            }
            dec_ifaces = c.getInterfaces();
            for (Class ciface : dec_ifaces) {
                if (remote.isAssignableFrom(c)) {
                    if (!ciface.getName().equals(remote.getName())
                            && !ciface.getName().equals("ade.ADEComponent")) {
                        if (dbg > 8) {
                            System.out.println("\tFound hier interface "
                                    + ciface.getName());
                        }
                        all_ifaces.add(ciface);
                    }
                }
            }
        }
        // now put all the Remote interfaces into a HashSet, which
        // will guarantee uniqueness
        if (dbg > 8) {
            System.out.println(myInfo.type + ": creating interface HashSet:");
        }
        String cname;
        for (Class c : all_ifaces) {
            cname = c.getName();
            if (!ifaces.contains(c.getName())) {
                // perhaps filter out some other classes...
                // if (!cname.equals(remote.getName()) &&
                // !cname.equals("ade.ADEComponent")) {
                if (dbg > 8) {
                    System.out.println("\tStoring interface " + cname);
                }
                ifaces.add(cname);
            }
        }
        return ifaces;
    }

    /**
     * Creates the communication objects (i.e., the
     * {@link ade.ADERemoteCallTimer ADERemoteCallTimer}s), registers, and makes
     * this component available for connections. Note that this method must be
     * called prior to a component joining the system; it is called only once in
     * one of two places: (1) during a {@link #getClient} call, where a remote
     * reference is obtained or (2) in the {@link #main} method after
     * construction is complete. Because it can be called from <tt>main</tt>, it
     * must be <tt>public</tt>; thus, for security, this object must be passed
     * as the <tt>cred</tt> parameter (ensuring that only this class calls this
     * method). The {@link #initialized} boolean guarantees this method is only
     * executed once.
     *
     * @param cred A self-reference, ensuring only this class calls this method
     * @throws AccessControlException If <tt>cred</tt> is not <tt>this</tt>
     * @throws RuntimeException Another error occurred
     */
    private synchronized void finishedInitialization(Object cred)
            throws AccessControlException, RuntimeException {
        // only let this class call this method
        if (!cred.equals(this)) {
            throw new AccessControlException("Access denied due to: " + cred);
        }
        // if the component has already initialized, just return
        if (initialized) {
            return;
        }

        // generate a new password for this component, will be used in
        // registry together with component name
        myPassword = "p" + Math.random();
        if (dbg > 5) {
            System.out.println(myID + " registering...");
        }
        if (!registered) {
	    //System.out.println("*************************** REGISTER COMPONENT *********");
            registerComponent();
            // TODO: MS: FIXME: if this was a registry and no registry argument was passed, 
            // then it would not be registered, so this should probably not be set to true in that case...
            registered = true;
            myInfo.state = ADEGlobals.ComponentState.RUN;
        }
        initialized = true;

        // After initialized, create any applicable standalone GUIs (see
        // method explanation for "applicable").
        createAnyApplicableStandaloneGUIs();

        if (dbg > 3) {
            System.out.println(myID + ": finishedInitialization, state="
                    + myInfo.state);
        }

        callLogging = myInfo.withcalllogging;

        // if local logging was enabled on the command line, do it now
        if (myInfo.withlocallogging) {
            try {
                setADEComponentLogging(true);
            } catch (ADEException ace) {
                System.err.println("Error enabling logging: "
                        + ace);
            }
        }

        if (callLogging && !(this instanceof ADERegistryImpl)) {
            try {
                setADEComponentLogging(true);
            } catch (ADEException ace) {
                System.err.println("Error enabling logging for call logging: "
                        + ace);
            }
        }
	// not sure we need this
        // callLogging = myInfo.withcalllogging;

        // now start the thread for the main loop
        (new Thread(this, "MainLoop")).start();
    }

    /**
     * creates standalone GUI frames. Will show: 1) any frames that are
     * specified as ADEGuiVisualizationSpecs.Startup.ALWAYS_SHOW, REGARDLESS of
     * the --GUI flag on the commandline. 2) any frames that are specified as
     * ADEGuiVisualizationSpecs.Startup.SHOW_ON_GUI_FLAG BUT ONLY IF --GUI was
     * passed on the command line AND the visualization was either requested by
     * name, OR there were NO EXPLICIT REQUESTS and so should just show all
     * "default" GUIs. 3) any frames that are specified as
     * ADEGuiVisualizationSpecs.Startup.SHOW_BY_EXPLICIT_REQUEST_ONLY, BUT ONLY
     * if --GUI is passed to the command line, AND the visualization is
     * explicitly requested by name.
     *
     */
    protected void createAnyApplicableStandaloneGUIs() {
        // delegate the code to a static method within ADEGuiStandaloneFrame,
        // for encapsulation's sake.
        ADEGuiExternalFrame.createExternalGUIFrames(this, myInfo.guiRequested,
                myInfo.guiParticularVisualizations, myInfo.persistentGUI);
    }

    /**
     * Register the component when ready.
     */
    private void registerComponent() {
        // ADERegistry ar = null;
        ar = null;
        myInfo.as = this; // add the reference to this component
        myID = myInfo.getKey(); // reset below if registry assigns name

	// Contact the ADERegistry
        // connect if a registryName has been provided, this is always the
        // case for regular components and only for registries if the name has
        // been passed on the command line, so the registry will register
        // with another registry
        if (myInfo.registryName != null) {
            // obtain a reference to the registry
            try {
                if (dbg > 3) {
                    System.out.print(myInfo.getKey()
                            + ": contacting ADERegistry ");
                    System.out.println(getRMIStringReg() + "...");
                }
                // MS: made this a bit more sophisticated:
                // -- if a specific registry name is given, it will be used
                // -- if no name was given, then if the default registry is
                // there
                // it will be used, otherwise any available registry will be
                // contacted
                ar = (ADERegistry) Naming.lookup(getRMIStringReg());
            } catch (Exception e) {
                // MS: look up for the given name failed, so let's see if there
                // are other names
                try {
                    if (dbg > 3) {
                        System.out.println("Could not connect to specified registry "
                                + myInfo.registryName
                                + " -- looking for other available registries...");
                    }
                    Registry r = LocateRegistry.getRegistry(myInfo.port);
                    String[] regs = r.list();
                    boolean foundone = false;
                    for (String s : regs) {
                        // if a registered object "claims" to be a registry,
                        // connect to it...
                        // MS: added check that the name is not equal to this
                        // components name (if it is a registry) to prevent
                        // self-registration...
                        if (s.startsWith("ADERegistryImpl") && !s.equals(myID)) {
                            ar = (ADERegistry) r.lookup(s);
                            if (dbg > 3) {
                                System.out.println("Found a registry: " + s);
                            }
                            // set the new registry name
                            myInfo.registryName = s.substring(s.indexOf('$') + 1);
                            foundone = true;
                            break;
                        }
                    }
                    // TODO: Maybe I still need to set the name????
                    if (!foundone) {
                        throw new Exception(
                                "No ADE registry registered in JAVA RMI Registry");
                    }
                } catch (Exception lastone) {
                    System.err.println("Could not contact any registry due to:");
                    System.err.println(e);
                    System.exit(ADEGlobals.ExitCode.REG_FAIL.code());
                }
            }
        
            // now try to register the component
            if (this instanceof ADERegistryImpl) {
                // registration
                try {
                    rctReg.remoteCall("registerComponent", ar, myInfo.duplicate(),
                            myPassword, true);
                } catch (Exception e) {
                    System.err.println(myID + ": could not register:\n" + e);
                    e.printStackTrace();
                    System.exit(ADEGlobals.ExitCode.REG_FAIL.code());
                }
                // heartbeat creation
                try {
                    // connect to registry with reconnection, but not to any
                    // registry
                    getHeartbeat(myInfo.registryType, myInfo.registryName, ar,
                            true, false, null);
                } catch (Exception e) {
                    System.err.println("Heartbeat creation failed! Exiting.");
                    System.err.println(e);
                    System.exit(ADEGlobals.ExitCode.REG_CONTACT.code());
                }
            } else {
                // let registry assign a name if myName==null
                String tempName;
                try {
                    tempName = (String) rctReg.remoteCall("registerComponent", ar,
                            myInfo.duplicate(), myPassword, false);
                    // remove the old binding
                    if (myInfo.name == null) {
                        // update the name
                        myInfo.name = tempName;
                        if (dbg > 4) {
                            System.out.println("\tAssigned name is "
                                    + myInfo.name);
                        }
                    } else if (!myInfo.name.equals(tempName)) {
                        System.err.print("Could not register under desired name: ");
                        System.err.println(myInfo.name + "; Got instead: "
                                + tempName);
                        System.err.println("Error registering component");
                        System.exit(ADEGlobals.ExitCode.NAME_CONFLICT.code());
                        return;
                    }
                    myID = myInfo.getKey();
                    // also add the key as the default group
		    // MS: only do this if the group is still empty, as the registry added that key too in that case
		    if (myInfo.groups.isEmpty()) {
			myInfo.groups.add(myID);
		    }

                    rctReg.setName(myID + "->ADERegistry");
                    if (dbg > 3) {
                        System.out.println(myID + " registered with "
                                + myInfo.registryName);
                    }
                } catch (Exception e) {
                    // throw an exception??
                    System.err.println(prg + ": Component " + myID
                            + " not registered:");
                    System.err.println(e);
                    System.exit(ADEGlobals.ExitCode.REG_FAIL.code());
                    return;
                }

                if (dbg > 7) {
                    System.out.println("\tCreating reaper (pulse="
                            + ADEGlobals.DEF_RPPULSE + ")");
                }
                theReaper = new Reaper(ADEGlobals.DEF_RPPULSE);
                if (dbg > 4) {
                    System.out.println(prg + ": Starting reaper...");
                }
                theReaper.start();

                // start the heartbeat to the registry; do this last
                if (dbg > 7 || debugHB) {
                    System.out.println("\tCreating heartbeat to registry...");
                }
                // TODO: put a choice of period back
                try {
		    // create new heartbeat
		    registryHB = new Heartbeat(ADEGlobals.DEF_HBPULSE,myInfo.registryType, myInfo.registryName,
					       ar, true, true, null);
		    // initialize it directly
		    registryHB.initialize();

                } catch (Exception e) {
                    System.err.println("Heartbeat creation failed! Exiting.");
                    System.err.println(e);
                    System.exit(ADEGlobals.ExitCode.REG_CONTACT.code());
                }
                if (dbg > 7 || debugHB) {
                    System.out.println("\tStarting heartbeat with registry "
                            + registryHB + "...");
                }
            }
            // state restoration (if applicable)
            // TODO: will this hold up the works if the user puts funky stuff
            // in the "loadState" method? (e.g., dependence on other component
            // refs, etc.)
            if (myInfo.maintainState) {
                try {
                    myInfo.adestate = (ADEState) rctReg.remoteCall(
                            "getComponentState", ar, myID, myPassword);
                    if (myInfo.adestate != null) {
                        loadState(myInfo.adestate);
                    } else {
                        System.out.println(myID
                                + ": no state returned for restore");
                    }
                } catch (Exception e) {
                    System.err.println(myID + ": could not retrieve state:\n"
                            + e);
                }
            }
        }
    }

    /**
     * Register with another {@link ade.ADERegistryImpl ADERegistry}. Only
     * ADERegistrys can call this.
     *
     * @param as the {@link ade.ADERegistryImpl ADERegistryImpl} with which this
     * ADERegistry is registering
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     * @throws RemoteException if registration fails
     * @throws AccessControlException if disallowed
     */
    final protected boolean registerWithRegistry(ADERegistry as)
            throws RemoteException, AccessControlException {
        if (!(this instanceof ADERegistryImpl)) {
            throw new AccessControlException(
                    "This function can only be used by ADERegistries.");
        }
        // if this registry has the other registry as a registered registry,
        // then the other one has just registered, so it can be released. Do
        // not register again (this will only cause an exception)
        // NOTE: that we are setting ar=null so that if the other registry
        // goes down, this one will stay up (otherwise it will be shut down
        // automatically)
        if (ar != null && ar.equals(as)) {
            ar = null;
            throw new AccessControlException(
                    "Registry already used as AR, no need to register...");
            // MS: throw exception to distinguish this case from the case below
            // as this is not an error...
            // return false;
        }
        String ret = null, asid = null;
        if (dbg > 3) {
            System.out.println("======== " + myID
                    + " CONNECTING TO OTHER REGISTRY ========");
            // System.out.println("==== "+ myInfo.registryType +" "+
            // myInfo.registryName +" "+ myInfo.registryPort +" "+
            // myInfo.registryIP +" ====");
            // System.out.println("======== CONNECTING TO OTHER REGISTRY ========");
        }
        try {
            asid = (String) rctReg.remoteCall("getID", as);
            // System.out.println("\n" + System.currentTimeMillis() +
            // "==registerWithRegistry()== "+ myID
            // +" calling registerComponent for " + asid);
            ret = (String) rctReg.remoteCall("registerComponent", as,
                    myInfo.duplicate(), myPassword, false);
            // System.out.println(System.currentTimeMillis() +
            // "==registerWithRegistry()== "+ myID + " registered with "+ asid +
            // "\n");
        } catch (ADEException ace) {
            if (ace.getCause() != null) {
                System.err.println(myID + ": remote call failed:\n"
                        + ace.getCause());
                if (dbg > 5) {
                    ace.getCause().printStackTrace();
                }
            } else {
                System.err.println(myID + ": remote call failed:\n" + ace);
                if (dbg > 5) {
                    ace.getCause().printStackTrace();
                }
            }
            throw new RemoteException("Registration failed", ace);
        }
        myInfo.registryType = getTypeFromID(asid);
        myInfo.registryName = getNameFromID(asid);
        return (getHeartbeat(getTypeFromID(asid), getNameFromID(asid), as,
                true, false, null) != null);
    }

    final protected void deregisterFromRegistries(ADERegistry[] regs,
            Object credentials) throws AccessControlException {
        // if (!(credentials.equals(ar))) {
        if (!(credentials instanceof ADERegistry)) {
            throw new AccessControlException("Must be called by an ADERegistry");
        }
        if (!credentials.equals(this)) {
            throw new AccessControlException("Can only be self-initiated");
        }
        if (rctReg != null) {
            System.out.println(myID + ": have " + regs.length + " regs");
            System.out.println(myID + ": have " + myheartbeats.size() + " hbs");
            Object[] regnames;
            Heartbeat hb;
            try {
                // this returns a set of Strings in an array, hence cast
                // accordingly
                regnames = rctReg.remoteCallConcurrent("getID", regs);
                for (Object regname : regnames) {
                    try {
                        hb = myheartbeats.get((String) regname);
                        if (hb != null) {
                            System.out.println(myID + ": got " + regname
                                    + " hb");
                            hb.terminate();
                            System.out.println(myID + ": called " + regname
                                    + " terminate");
                            myheartbeats.remove((String) regname);
                            System.out.println(myID + ": removed " + regname
                                    + " hb");
                        } else {
                            System.out.println(myID + ": missing " + regname
                                    + " hb");
                        }
                    } catch (Exception e) {
                        // System.err.println(myID +": terminating "+ regname
                        // +":\n"+ e);
                        System.err.println(myID + ": error terminating "
                                + regname + " hb");
                        e.printStackTrace();
                    }
                }
            } catch (Exception e1) {
                System.err.println(myID + ": getting reg IDs:\n" + e1);
            }
            try {
                rctReg.remoteCallConcurrent("deregisterComponent", regs, myID,
                        myPassword);
            } catch (Exception e) {
                // System.err.println(myID +": problem deregistering:\n"+ e);
                System.err.println(myID + ": problem deregistering:");
                e.printStackTrace();
            }
        } else {
            System.out.println(myID + ": rctReg is null!");
        }
    }

    /**
     * Removes the connection to the component
     *
     * @param ref A pseudo-reference to the component
     * @return returns true if successful, false otherwise
     */
    final protected boolean releaseComponent(Object ref) {
        //
        try {
            if (ref instanceof Heartbeat) {
                Heartbeat hb = (Heartbeat) ref;
                final String id = getKey(hb.toServType, hb.toServName);
                removeHeartbeat(id, this);
                return true;
            } else {
                return false;
            }
        } catch (AccessControlException ace) {
            System.err.println("Not sure what happened..." + ace);
            return false;
        }
    }

    /**
     * Removes and terminates the heartbeat to the <tt>ADEComponent</tt> specified
     * by <tt>id</tt>. This method can only be self-called by an
     * {@link ade.ADERegistry ADERegistry} as part of its bookkeeping
     * procedures.
     *
     * @param id The ID of the <tt>ADEComponent</tt> heartbeat to remove
     * @param credentials Permission verification
     * @throws AccessControlException If not self-called by an ADERegistry
     */
    final protected void removeHeartbeat(String id, Object credentials)
            throws AccessControlException {
        // if (!(credentials.equals(ar))) {
        if (!(credentials instanceof ADERegistry)) {
            throw new AccessControlException("Must be called by an ADERegistry");
        }
        if (!credentials.equals(this)) {
            throw new AccessControlException("Can only be self-initiated");
        }
        synchronized (myheartbeats) {
            Heartbeat hb = myheartbeats.remove(id);
            if (hb != null) {
                hb.terminate();
            }
        }
    }

    /**
     * Sets the host information as known to the {@link ade.ADERegistryImpl
     * ADERegistryImpl}.
     *
     * @param hi the {@link ade.ADEHostInfo ADEHostInfo} structure
     * @param credentials access control (must be the registry with which this
     * component is registered)
     * @throws AccessControlException if credentials are unacceptable
     * @throws RemoteException if the information cannot be set
     */
    @Override
    public final void setHostInfo(ADEHostInfo hi, Object credentials)
            throws AccessControlException, RemoteException {
        // if (!(credentials.equals(this) || credentials.equals(ar)
        if (!(credentials instanceof ADERegistry)) {
            System.err.println(myID + ": Authorization FAILED!");
            throw new AccessControlException(
                    "Authorization to set host failed due to " + credentials);
        }
        if (dbg > 4) {
            System.out.println(myID + ": setting host info...");
            // System.out.println(myID +": ar="+ ar +", credentials="+
            // credentials);
        }
        myHost = hi;
        if (dbg > 4) {
            System.out.println(myID + ": set host to " + hi.hostip);
        }
    }

    /**
     * Request a list of {@link ade.ADEComponent}s with the given constraints
     *
     * @param a list of constraints
     * @return a list of {@link ade.ADEComponent ADEComponent} names
     * @throws ADEException Relays any thrown <tt>Exception</tt>
     */
    final protected ArrayList<String> getComponentList(String[][] constraints) {
        // ensure this component is registered in the system
        finishedInitialization(this);
        try {
            if (dbg > 3) {
                System.out.println(myID
                        + ": requesting component list with constraints "
                        + constraints + "...");
            }
            return (ArrayList<String>) rctReg.remoteCall("requestComponentList",
                    ar, myID, myPassword, constraints);
        } catch (ADEException ace) {
            if (dbg > 5) {
                System.err.println(myID + ": failed to get list of all components");
                System.err.println(ace);
            }
        }
        return null;
    }

    final protected void registerNewComponentNotification(String[][] constraints,
            boolean on) {
        // ensure this component is registered in the system
        finishedInitialization(this);
        try {
            if (dbg > 3) {
                System.out.println(myID
                        + ": requesting notification for components with constraints "
                        + constraints + "...");
            }
            rctReg.remoteCall("requestNewComponentNotification", ar, myID,
                    myPassword, myInfo.as, constraints, on);
        } catch (ADEException ace) {
            if (dbg > 5) {
                System.err.println(myID
                        + ": failed to register for new component notification");
                System.err.println(ace);
            }
        }
    }


    // remove an acquired client 
    final protected void releaseClient(String key) {
        Heartbeat oref = myheartbeats.remove(key);
        if (oref != null) {
            System.out.println("releasing " + key);
            oref.terminate();
        }
    }

    // MS: FIX description
    /**
     * Request connections from an {@link ade.ADERegistryImpl ADERegistryImpl}
     * to all {@link ade.ADEComponent ADEComponent}s of type <tt>type</tt>
     * <b>except</b> the self-reference. Note that the returned list will only
     * contain pseudo-references to components that are <b>confirmed</b>
     * operational and responsive. Note also that the <tt>type</tt> can be
     * either an {@link ade.ADEComponent ADEComponent} or <b>any</b> interface that a
     * registered component implements.
     *
     * @param constraints a list of constraints for the component
     * @return an {@link java.util.ArrayList ArrayList} of pseudo-references
     * that provide (guarded) access to the remote {@link ade.ADEComponent
     *         ADEComponent}s
     * @throws RemoteException if an error occurs
     */

    // helper function to convert a groups Hashset to a list of group constraints
    private String[][] toGroupConstraints(HashSet<String> groups,String type, String name) {
	ArrayList<String[]> ret = new ArrayList<String[]>();
	Iterator<String> it = groups.iterator();
	while (it.hasNext()) {
	    ret.add(new String[]{"type",type});
	    if (name != null) {
		ret.add(new String[]{"name",name});
	    }
	    ret.add(new String[]{"group",it.next()});
	    ret.add(new String[]{"or"}); // this will produce an extra "or" at the end, which should not have any effect
	}
	// now remove the last element which is a superfluous "or" that causes meetCriteria to malfunction...
	ret.remove(ret.size()-1);
	return ret.toArray(new String[0][0]);
    }

    final protected Object getClient(String type) {
	if (myInfo.connecttogroups.isEmpty()) {
	    return getClient(new String[][]{{"type", type}},ADEGlobals.DEF_TIMEOUT_REQUEST);
	}
	else {
	    return getClient(toGroupConstraints(myInfo.connecttogroups,type,null),ADEGlobals.DEF_TIMEOUT_REQUEST);
	}
    }

    final protected Object getClient(String type, int time) {
	if (myInfo.connecttogroups.isEmpty()) {
	    return getClient(new String[][]{{"type", type}}, time);
	}
	else {
	    return getClient(toGroupConstraints(myInfo.connecttogroups,type,null),time);
	}
    }

    final protected Object getClient(String type, String name) {
	if (myInfo.connecttogroups.isEmpty()) {
	    return getClient(new String[][]{{"type", type}, {"name", name}},ADEGlobals.DEF_TIMEOUT_REQUEST);
	}
	else {
	    return getClient(toGroupConstraints(myInfo.connecttogroups,type,name),ADEGlobals.DEF_TIMEOUT_REQUEST);                
	}
    }

    final protected Object getClient(String type, String name, int time) {
	if (myInfo.connecttogroups.isEmpty()) {
	    return getClient(new String[][]{{"type", type}, {"name", name}},time);
	}
	else {
	    return getClient(toGroupConstraints(myInfo.connecttogroups,type,name),time);
	}
    }

    // NOTE: this function does not use the "connectToGroups" constraints, any group constraint must be passed in explicitly!
    final protected Object getClient(String[][] constraints) {
        return getClient(constraints, ADEGlobals.DEF_TIMEOUT_REQUEST);
    }

    final protected Object getClient(final String[][] constraints, int time) {
        // MS: this should not be necessary at this point in the future
        finishedInitialization(this);

        if (dbg > 3) {
            System.out.println(myID + ": Requesting a component with constraints "
                    + constraints);
            for (int i = 0; i < constraints.length; i++) {
                System.out.print("\t");
                for (int j = 0; j < constraints[i].length; j++) {
                    System.out.print(constraints[i][j] + " ");
                }
                System.out.println();
            }
        }

        // make the remote request to the registry
        try {
	    long starttime = System.currentTimeMillis();

	    if (time <0) {
		return rctReg.remoteCall(-1,new Callable<Object>() {
			public Object call() {
			    try {
				ADEComponentInfo aci = (ADEComponentInfo) rctReg.remoteCall(-1, "requestConnection",
											ar, myID, myPassword, myInfo.as, constraints);
				// return null if no connection could be made, otherwise return the heartbeat
				if (aci == null) {
				    return null;
				}
				else {
				    return getHeartbeat(aci.getType(), aci.getName(), aci.as, true, false, constraints, -1);
				}
			    } catch (ADEException ace) {
				// should not occur
			    }
			    return null;
			}});
	    }
	    else {	    
		ADEComponentInfo aci = (ADEComponentInfo) rctReg.remoteCall(time, "requestConnection",
							      ar, myID, myPassword, myInfo.as, constraints);
		// return null if no connection could be made, otherwise return the heartbeat
		if (aci == null) {
		    return null;
		}
		else {
		    return getHeartbeat(aci.getType(), aci.getName(), aci.as, true, false, constraints, time);
		}
	    }
	} catch (ADEException ace) {
	    // MS: TODO: THIS CAN HAPPEN BECAUSE OF A TIMEOUT!!!!!!
	    // note that it cannot/should not happen for time<0 though...
	    if (ace instanceof ADETimeoutException) {
		System.err.println(myID + " WARNING!!! Could registration attempt with constraints " + ADEGlobals.constraintsToStr(constraints," "," ") + " timed out!");
	    }
            if (dbg > 5) {
                System.err.println(myID + ": did not get component with constraints " + ADEGlobals.constraintsToStr(constraints," "," "));
                System.err.println(ace);
            }
        }
        // return null if an error occurred
        return null;
    }

    // MS: this replaces the old getClientAll call!
    final protected ArrayList<Object> getClients(String type) {
	if (myInfo.connecttogroups.isEmpty()) {
	    return getClients(new String[][]{{"type", type}},ADEGlobals.DEF_TIMEOUT_REQUEST);
	}
	else {
	    return getClients(toGroupConstraints(myInfo.connecttogroups,type,null),ADEGlobals.DEF_TIMEOUT_REQUEST);
	}
    }

    final protected ArrayList<Object> getClients(String type, int time) {
	if (myInfo.connecttogroups.isEmpty()) {
	    return getClients(new String[][]{{"type", type}}, time);
	}
	else {
	    return getClients(toGroupConstraints(myInfo.connecttogroups,type,null),time);	    
	}
    }

    final protected ArrayList<Object> getClients(String[][] constraints) {
        return getClients(constraints, ADEGlobals.DEF_TIMEOUT_REQUEST);
    }

    // most general entry point for requesting a set of clients from the
    // registry
    final protected ArrayList<Object> getClients(String[][] constraints,
            int time) {
        if (time < 0) {
            throw new IllegalArgumentException("Negative timeout");
        }
        ArrayList<ADEComponentInfo> slist;
        ArrayList<Object> rlist = new ArrayList<Object>();

        // MS: this should not be necessary in the future once we disallow
        // component constructors
        // this will cause the component to register, making it visible in the
        // system (able to both request connections and provide services)
        finishedInitialization(this);

        if (dbg > 3) {
            System.out.println(myID
                    + ": Requesting all components with constraints "
                    + constraints);
        }

        // make the remote request to the registry
        try {
            slist = (ArrayList<ADEComponentInfo>) rctReg.remoteCall(
                    "requestConnections", ar, myID, myPassword, myInfo.as,
                    constraints);
        } catch (ADEException ace) {
            if (dbg > 5) {
                System.err.println(myID
                        + ": did not get all components with constraints "
                        + constraints);
                System.err.println(ace);
            }
            // indicate that there was a problem by returning null
            return null;
        }
        // we got something back, might be the empty list...
        if (slist != null && slist.size() > 0) {
            int eligible = slist.size();
            int acquired = 0;
            Object obj;
            for (ADEComponentInfo asi : slist) {
                try {
		    obj = getHeartbeat(asi.getType(), asi.getName(), asi.as,
				       true, false, constraints, time);
		    if (obj != null) {
			acquired++;
			rlist.add(obj);
		    } else {
			System.err.println("Problem getting heartbeat to "
					   + asi.getKey());
		    }
                } catch (Exception e) {
                    System.err.println("Problem establishing heartbeat to component due to "
				       + e);
                }
            }
            if (acquired < eligible) {
                System.err.println("Did not get heartbeats fro all eligible components...");
            }
        }
        return rlist;
    }

    // ==> TODO: implement "releaseConnection"... to stop a connection without
    // stopping the component...
    /**
     * Get the host list from the {@link ade.ADERegistry ADERegistry} with which
     * this component is registered.
     *
     * @return The list of {@link ade.ADEHostInfo ADEHostInfo} objects
     */
    final protected ArrayList<ADEHostInfo> getHostList() {
        ArrayList<ADEHostInfo> retlist = null;
        // this will cause the component to register, making it visible in the
        // system (able to both request connections and provide services)
        finishedInitialization(this);
        try {
            retlist = (ArrayList<ADEHostInfo>) rctReg.remoteCall(
                    "requestHostList", ar, myID);
        } catch (ADEException ace) {
            System.err.println(myID + ": Could not get host list:\n\t" + ace);
        } catch (Exception e) {
            System.err.println(myID + ": Could not get host list:\n\t" + e);
        }
        return retlist;
    }

    /**
     * Get the host list from all the {@link ade.ADERegistry ADERegistry}s in
     * the system.
     *
     * @return The list of {@link ade.ADEHostInfo ADEHostInfo} objects
     */
    final protected ArrayList<ADEHostInfo> getHostListAll() {
        ArrayList<ADEHostInfo> retlist = null;
        // this will cause the component to register, making it visible in the
        // system (able to both request connections and provide services)
        finishedInitialization(this);
        try {
            retlist = (ArrayList<ADEHostInfo>) rctReg.remoteCall(
                    "requestHostListAll", ar, myID);
        } catch (ADEException ace) {
            System.err.println(myID + ": Could not get host list:\n\t" + ace);
        } catch (Exception e) {
            System.err.println(myID + ": Could not get host list:\n\t" + e);
        }
        return retlist;
    }

    /**
     * Create a {@link #Heartbeat} (if necessary; or return an existing one),
     * using {@link ADEGlobals#DEF_TIMEOUT_REQUEST} as the timeout value.
     *
     * @param t The component type
     * @param n The component name (<tt>null</tt> if unassigned)
     * @param as The component reference (can be <tt>null</tt>)
     * @param rec Attempt reconnections
     * @param any Reconnect to any of type <tt>type</tt>
     * @return A {@link #Heartbeat} for component <tt>as</tt>; <tt>null</tt> if
     * <tt>as</tt> is initially <tt>null</tt> and is not successfully
     * initialized
     */
    private Heartbeat getHeartbeat(String t, String n, ADEComponent as,
            boolean rec, boolean any, String[][] constraints) {
        return getHeartbeat(t, n, as, rec, any, constraints,
                ADEGlobals.DEF_TIMEOUT_REQUEST);
    }

    /**
     * Create a {@link #Heartbeat} (if necessary; or return an existing one).
     *
     * @param t The component type
     * @param n The component name (<tt>null</tt> if unassigned)
     * @param as The component reference (can be <tt>null</tt>)
     * @param rec Attempt reconnections
     * @param any Reconnect to any of type <tt>type</tt>
     * @param to A timeout value (in ms), only used if the <tt>Heartbeat</tt>
     * must be created (under the assumption that retrieval of an existing
     * <tt>Heartbeat</tt> will always be fast enough)
     * @return A {@link #Heartbeat} for component <tt>as</tt>; <tt>null</tt> if
     * <tt>as</tt> is initially <tt>null</tt> and is not successfully
     * initialized
     */
    private Heartbeat getHeartbeat(String t, String n, ADEComponent as,
            boolean rec, boolean any, String[][] constraints, int to) {
        Heartbeat hb = null;
        if (!myheartbeats.containsKey(getKey(t, n))) {
            // A not very good, but acceptable-for-the-moment solution to the
            // issue that myheartbeats cannot be locked (ConcurrentHashMap)
            // but we want to make sure only one heartbeat gets created.
            // If no heartbeat exists, we synch on the guard. Check for
            // existence again in case a waiting thread was locked out while
            // another was creating; if the heartbeat is still null, create it.
            // This is acceptable because we don't expect to be here all that
            // much, essentially only at startup or when a component has migrated;
            // at startup, we pay the price, on migration, we expect only a
            // small
            // number of heartbeats to contend for access.
            // Note, however, that the "getRemoteCall" method is itself
            // synchronized; be aware that deadlock can occur if a thread
            // attempts a remote call during heartbeat creation.
            // Also note that there is a switcheroo with the name, replacing
            // ALL_ACCESS with the name assigned by a registry
            if (debugCall || debugReconn) {
                System.out.println("******** " + myID + "*** : synch on hb");
            }
            // MS: why can't we simply do synchronized(myheartbeats)???
            synchronized (heartbeatGuard) {
                if (!myheartbeats.containsKey(getKey(t, n))) {
                    // acquired lock, still no heartbeat, so create it
                    if (dbg > 4 || debugHB || debugReconn) {
                        System.out.println(myID + ": creating " + getKey(t, n)
                                + " hb");
                    }
                    try {
			// create new heartbeat
			hb = new Heartbeat(ADEGlobals.DEF_HBPULSE, t, n, as, rec, any, constraints);
			// initialize it directly
			hb.initialize();

                        // Note: the heartbeat is put into the myheartbeat hash
                        // during its initialization in the "replaceReference"
                        // method (and *only* after success); only after
                        // initialization do we react (and thereby avoid
                        // potential
                        // deadlock resulting from making a remote call from
                        // within
                        // the reaction)
                        if (dbg > 3 || debugReconn) {
                            System.out.println(myID + ": got hb to "
                                    + getKey(t, n) + " (actual key "
                                    + getRefID(hb) + ")");
                        }
                        // MS: call component connect react in a separate thread so
                        // as to allow Heartbeat
                        // to return the heartbeat independent of the processing
                        // done in componentConnectReact
                        final String finalkey = getKey(hb.toServType,hb.toServName);
                        final Heartbeat newheartbeat = hb;
                        final String[][] finalconstraints = constraints;
                        new Thread() {
                            @Override
                            public void run() {
                                componentConnectReact(finalkey, newheartbeat,
                                        finalconstraints);
                            }
                        }.start();
                    } catch (Exception e) {
                        System.err.println(myID
                                + ": failed to create Heartbeat to "
                                + getKey(t, n) + ":\n\t" + e);
                        // e.printStackTrace();
                    }
                } else {
                    // heartbeat was created while waiting for lock
                    hb = myheartbeats.get(getKey(t, n));
                    if (dbg > 3 || debugReconn) {
                        System.out.println(myID + ": got created hb to "
                                + getKey(t, n) + "(actual key " + getRefID(hb)
                                + ")");
                    }
                }
                if (debugCall || debugReconn) {
                    System.out.println("******** " + myID
                            + "*** : release synch");
                }
                return hb;
            }
        } else {
            // heartbeat exists, just return it
            hb = myheartbeats.get(getKey(t, n));
            if (dbg > 3 || debugReconn) {
                System.out.println(myID + ": got existing hb to "
                        + getKey(t, n) + "(actual key " + getRefID(hb) + ")");
            }
        }
        return hb;
    }

    /**
     * The standard shutdown procedure, triggered by a system halt signal (e.g.,
     * Ctrl-C), interruptions, the ShutdownHook, or errors. It calls
     * {@link #localshutdown} for derived classes.
     */
    final protected void shutdownADEComponent() {
        String name = (myID == null ? "ADEComponentImpl" : myID);
        if (myInfo.killForCtrlc) {
            try {
                killComponent(this);
            } catch (Exception ignore) {
                System.out.println(name + ": UNCLEAN SHUTDOWN FAILED:\n"
                        + ignore);
                System.out.println("Continuing with normal shutdown...");
            }
        }
        System.out.println(name + ": Shutting down:");
        // are there other things? clear data structs? other threads?
        // - call localsutdown()
        // - stop all client heartbeats
        // - deregister
        // - stop registry heartbeat
        // - stop reaper
        // - stop rctReg

        // set our state
        if (dbg > 5) {
            System.out.println(myID + ": Setting state to SHUTDOWN...");
        }
        myInfo.state = ADEGlobals.ComponentState.SHUTDOWN;
        if (dbg > 5) {
            System.out.println("\tSet state to " + myInfo.state);
        }

        // stop the state maintenance thread
        disableState();
        // call localshutdown
        if (dbg > 5) {
            System.out.println(myID + ": Calling local shutdown...");
        }
        try {
            localshutdown();
            if (dbg > 5) {
                System.out.println("\tLocal shutdown success");
            }
        } catch (RuntimeException re) {
            System.err.println(myID + ": exception in localshutdown:");
            re.printStackTrace();
        } catch (Exception e) {
            System.err.println(myID + ": exception in localshutdown:\n" + e);
        }

        // stop client heartbeats
        if (dbg > 5) {
            System.out.println(myID + ": stopping client hearbeats...");
        }
        for (Heartbeat hb : myheartbeats.values()) {
            try {
                hb.terminate();
            } catch (Exception e) {
                System.err.println(myID + ": problem stopping heartbeat:\n" + e);
            }
            if (dbg > 5) {
                System.out.println("\tFinished stopping heartbeats");
            }
        }
        myheartbeats.clear();

        // deregister
        if (dbg > 5) {
            System.out.println(myID + ": deregistering...");
        }
        try {
            if (ar != null) {
                // only let the registry know if not enforced by the registry
                if (!registryenforced) {
                    rctReg.remoteCall("deregisterComponent", ar, myID, myPassword);
                } // this is used to allow for different treament depending on
                // who
                // initiated the shutdown; for now, it's the same
                else {
                    rctReg.remoteCall("deregisterComponent", ar, myID, myPassword);
                }
                ar = null;
                if (dbg > 5) {
                    System.out.println("\t" + myID + " deregistered.");
                }
            } else {
                if (dbg > 5) {
                    System.out.println("\t" + myID + " no registry reference!");
                }
            }
        } catch (Exception e) {
            System.err.println(myID + ": problem deregistering:\n" + e);
        }
        if (this instanceof ADERegistryImpl) {
            ((ADERegistryImpl) this).unbindFromRegistry(myID, true);
        }

        // stop the registry heartbeat
        if (dbg > 5) {
            System.out.println(myID + ": stopping registry hearbeat...");
        }
        try {
            if (registryHB != null) {
                registryHB.terminate();
            }
            if (dbg > 5) {
                System.out.println("\t" + myID + " stopped registry heartbeat.");
            }
        } catch (Exception e) {
            System.err.println(myID + ": stopping registry heartbeat:\n" + e);
        }

        // stop the reaper
        if (dbg > 5) {
            System.out.println(myID + ": stopping reaper...");
        }
        try {
            if (theReaper != null) {
                theReaper.terminate();
            }
            if (dbg > 5) {
                System.out.println("\t" + myID + " stopped reaper.");
            }
        } catch (Exception e) {
            System.err.println(myID + ": problem stopping reaper:\n" + e);
        }

        // get log file to flush properly.
        if (fwrLog != null) {
            System.out.println("Closing log file...");
            try {
                fwrLog.close();
            } catch (IOException ioe) {
                if (dbg > 5) {
                    System.err.println("Error closing log file: " + ioe);
                }
            }
        }

        // discard the remote call timers
        if (dbg > 5) {
            System.out.println(myID + ": final cleanup...");
        }
        if (rctReg != null) {
            rctReg.terminate();
        }
        // always want to see this message
        System.out.println(myID + " is shut down.\n");
        // why is this hanging?
        // myInfo.print();
        System.out.flush();
        // Runtime.getRuntime().halt(ADEGlobals.ExitCode.OK.code());
    }


    // ***************************************************************
    // ***** Utility methods for use by subclasses; note that some are
    // ***** declared final for security purposes
    // ***************************************************************
    /**
     * Print out a component's info.
     */
    protected void printInfo() {
        myInfo.print();
    }

    /**
     * Convenience method that will return a properly formatted RMI string.
     *
     * @param ip the RMI IP address
     * @param port the RMI port
     * @param type the component type
     * @param name the component name
     * @return a properly formatted RMI string
     */
    final protected String makeRMIString(String ip, int port, String type,
            String name) {
        StringBuilder sb = new StringBuilder("rmi://");
        sb.append(ip);
        sb.append(":");
        sb.append(port);
        sb.append("/");
        sb.append(type);
        sb.append("$");
        sb.append(name);
        return sb.toString();
    }

    /**
     * Convenience method that will return a properly formatted RMI string for
     * contacting the primary {@link ade.ADERegistryImpl ADERegistryImpl}.
     *
     * @return a properly formatted RMI string to the primary registry
     */
    final protected String getRMIStringReg() {
        StringBuilder sb = new StringBuilder("rmi://");
        sb.append(myInfo.registryIP);
        sb.append(":");
        sb.append(myInfo.registryPort);
        sb.append("/");
        sb.append(myInfo.registryType);
        sb.append("$");
        sb.append(myInfo.registryName);
        return sb.toString();
    }

    /**
     * Convenience method that will return a properly formatted RMI string for
     * contacting this component.
     *
     * @return a properly formatted RMI string for contacting this component
     */
    final protected String getRMIStringMe() {
        return "rmi://" + toString();
    }

    /**
     * Convenience method for returning the host of this component.
     *
     * @return the host
     */
    final public String getHostMe() {
        return myInfo.host;
    }

    /**
     * Convenience method for returning the host of this component.
     *
     * @return the host
     */
    final public int getPortMe() {
        return myInfo.port;
    }

    /**
     * Convenience method for returning information about this component (a
     * concatenation of host, port, type, and name), formatted like an RMI
     * string.
     *
     * @return a string representation of this component
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(myInfo.host);
        sb.append(":");
        sb.append(myInfo.port);
        sb.append("/");
        sb.append(myInfo.type);
        sb.append("$");
        sb.append(myInfo.name);
        return sb.toString();
    }

    /**
     * Return the ID of this component; let anyone access it.
     *
     * @return the component's ID string (<tt>type$name</tt>)
     * @throws RemoteException if an error occurs
     */
    @Override
    final public String getID() throws RemoteException {
        return myID;
    }

    /**
     * Returns the name of the component. Essentially it is the same as getID(),
     * except only the name portion of the ID is returned.
     */
    @Override
    public String getName() throws RemoteException {
        return getNameFromID(getID());
    }

    /**
     * Convenience method that constructs an ID from type and name. Equivalent
     * to the {@link ade.ADEComponentInfo#getKey getKey} method in
     * {@link ade.ADEComponentInfo ADEComponentInfo}, for use when such an object does
     * not yet exist.
     *
     * @param type the component type
     * @param name the component name
     * @return string concatenation <tt>type$name</tt>
     */
    final protected String getKey(String type, String name) {
        StringBuilder sb = new StringBuilder(type);
        sb.append("$");
        sb.append(name);
        return sb.toString();
    }

    /**
     * Convenience method that extracts the component type from an ID (works for an
     * RMI string also).
     *
     * @param id the ID
     * @return the type
     */
    final protected String getTypeFromID(String id) {
        return (id.substring(0, id.indexOf("$")));
        // return "admin";
    }

    /**
     * Convenience method that extracts the component name from an ID (works for an
     * RMI string also).
     *
     * @param id the ID
     * @return the name
     */
    final protected String getNameFromID(String id) {
        return (id.substring(id.indexOf("$") + 1));
        // return "admin";
    }

    /**
     * Convenience method that returns the temporary directory used by this
     * component.
     *
     * @return String The temporary directory path
     */
    final protected String getTempDir() {
        return myHost.scratch;
    }

    /**
     * Convenience method that returns the number of connections this component
     * currently services.
     *
     * @return the number of current connections
     */
    final protected int numConn() {
        return myInfo.connectsCurrent;
    }

    /**
     * Returns the component's additional <i>credentials</i> (generally set using
     * the <tt>-c</tt> command-line switch).
     *
     * @return The component's additional <i>credentials</i> object
     */
    final protected Object getCredentials() {
        return myInfo.credentials;
    }

    /**
     * Sets both the <tt>Heartbeat</tt> and <tt>Reaper</tt> periods.
     *
     * @param p the time period (in ms)
     */
    final protected void setPulse(int p) {
        setHeartbeatPulse(p);
        setReaperPulse(p);
    }

    /**
     * Sets the <tt>Heartbeat</tt> period.
     *
     * @param p the time period (in ms)
     */
    final protected void setHeartbeatPulse(int p) {
        myInfo.heartBeatPeriod = p;
        registryHB.setPeriod(myInfo.heartBeatPeriod);
    }

    /**
     * Sets the <tt>Reaper</tt> period.
     *
     * @param p the time period (in ms)
     */
    final protected void setReaperPulse(int p) {
        rppulse = p;
        theReaper.setPeriod(rppulse);
    }

    /**
     * Returns the current {@link ade.ADEGlobals.ComponentState State} of this
     * component.
     *
     * @return the component state
     */
    final protected ADEGlobals.ComponentState getMyState() {
        return myInfo.state;
    }

    /**
     * Sets the current {@link ade.ADEGlobals.ComponentState State} of this component.
     *
     * @param st the component state
     */
    final protected void setState(ADEGlobals.ComponentState st) {
        myInfo.state = st;
    }

    /**
     * Change the "recovery multiplier", which delays recovery initiation for
     * the component by the given number of registry reaper periods. Note that the
     * value cannot be less than one. Also note thatthe multiplier will be in
     * effect <b>whenever</b> the number of restarts is greater than zero; if
     * you want to change the delay (for instance, if shutting yourself down),
     * change the setting back to one before shutdown, possibly in the
     * {@link #localshutdown localshutdown} method.
     *
     * @param mult the reaper period multiplier
     */
    final protected void setRecoveryMultiplier(int mult) {
        if (mult < 1) {
            mult = 1;
        }
        if (dbg > 3) {
            System.out.println(myID + " setting recovery multiplier to " + mult);
        }
        myInfo.setRecoveryMultiplier(mult);
    }

    /**
     * Make a remote method call to an {@link ade.ADEComponent ADEComponent} to which
     * this component is connected with the timeout specified by
     * {@link ade.ADEGlobals#DEF_TIMEOUT_CALL}.
     */
    final protected Object call(Object ref, String mn, Object... args)
            throws ADEException, ADETimeoutException,
            ADEReferenceException {
        return call(ADEGlobals.DEF_TIMEOUT_CALL, ref, mn, args);
    }

    /**
     * Make a blocking remote method call to an {@link ade.ADEComponent ADEComponent} to which
     * this component is connected.
     */
    final protected Object callBlocking(Object ref, String mn, Object... args)
            throws ADEException, ADETimeoutException,
            ADEReferenceException {
        return call(0, ref, mn, args);
    }

    /**
     * Make a non-blocking remote method call to an {@link ade.ADEComponent ADEComponent} to which
     * this component is connected.  This will return a "future".
     */
    final protected Object callNonBlocking(Object ref, String mn, Object... args)
            throws ADEException, ADETimeoutException,
            ADEReferenceException {
        return call(-1, ref, mn, args);
    }

    /**
     * Make a remote method call to an {@link ade.ADEComponent ADEComponent} to which
     * this component is connected. The returned item, if there is one, must be
     * cast by the calling program. For security reasons, this method is final.
     *
     * @param tout The number of milliseconds allowed for completion (a value of
     * 0 indicates an indefinite wait)
     * @param ref the object returned by the {@link #getClient}
     * @param mn the name of the method to call
     * @param args the parameters for the method call
     * @return the result of the remote call
     * @throws ADEException A Java exception was thrown while attempting
     * completion of the method call. The ADEException is simply a wrapper;
     * the actual exception can be retrieved using <tt>getCause</tt> .
     * @throws ADETimeoutException The remote call timed out; it cannot be
     * determined whether failure was due to actual failure or network latency
     * @throws ADEReferenceException The pseudoreference is <tt>null</tt>
     */
    final protected Object call(int tout, Object ref, String mn, Object... args)
            throws ADEException, ADETimeoutException,
            ADEReferenceException {
        if (debugCall) {
            System.out.println(myID + "::call("
                    + ADEGlobals.getMethodString(mn, args) + ")");
            System.out.println(myID + ": ref=[" + ref + "]");
        }
        if (ref == null) {
            throw new ADEReferenceException("Null remote reference!");
        }
	// check if constraints have been passed in for one-shot calling
	if (ref instanceof String[][]) {
	    String[][] constraints = (String[][]) ref;
	    // request a call through the registry (note that the registry needs to do method access control
	    // in addition to finding an appropriate component)
	    try {
		return rctReg.remoteCall(tout, "callMethodInRemoteComponent", ar, myID, myPassword,
					 myInfo.as, constraints, mn, args);
	    } catch (Exception e) {
		canLogIt("ADE one-shot call exception for method " + mn + ": " + e);
		throw new ADEException("ADE one-shot call exception for method " + mn + ": " + e);
	    }
	}
	// otherwise it must be a Heartbeat object
        else if (!(ref instanceof Heartbeat)) {
            throw new ADEReferenceException(
                    "Wrong type for remote reference!");
        }
        if (mn == null || mn.equals("")) {
            throw new ADEException("No method name supplied");
        }
        Heartbeat hb = (Heartbeat) ref;
        long startT = System.currentTimeMillis();
        if (callLogging && locallogging) {
            String rargs = new String();
            String cl;
            for (Object a : args) {
                rargs = rargs + " " + a.toString();
            }
            try {
                cl = logIt("CALL: " + hb.toServType + " " + mn + " " + rargs);
                loggedCalls.add(cl + " CALL: " + hb.toServType + " " + mn + " "
                        + rargs);
            } catch (IOException e) {
                System.err.println(myID + ": Exception logging:\n\t" + e);
            }
        }
        if (debugCall || dbg > 6) {
            System.out.println("In RemoteCall::call() for "
                    + ADEGlobals.getMethodString(mn, args));
        }
        Object returnobj = null;
        try {
            // make the call
            returnobj = hb.rct.remoteCall(tout, mn, hb.toComponent, args);
            if (debugCall) {
                System.out.println(myID + ": Leaving RemoteInfo::call("
                        + ADEGlobals.getMethodString(mn, args) + "): done");
                System.out.println(myID + ": RemoteInfo::call took "
                        + (System.currentTimeMillis() - startT) + "ms");
            }
        } catch (Exception e) {
            // TODO: notify the registry that the component is
            // unrechable, so that it does not have to wait for
            // the heartbeat timeout for that component and can
            // initiate recovery right away and at the same time
            // allow for "component swaping", which will let this
            // heartbeat connect to another availabl component of the
            // same type that meets all constraints (the parameter
            // allowComponentSwapping must be true)
			/*
             * ADEComponent as; if ((as =
             * (ADEComponent)notifyRegistryComponentDown(hb.toComponent
             * ,allowComponentSwapping)) != null) { // the component swapping
             * was enabled and succeeded, so set the new component hb.toComponent =
             * as; return call(); // this should not be recursive but simply sit
             * in a while-loop } // otherwise we failed, so free the caller and
             * throw an exception
             */
            // PWS: is there a reason this is failing silently? That can
            // make it really hard to track down problems.
            canLogIt("ADE call exception in " + hb.toServType + "." + mn + ": " + e);
            throw new ADEException("Default call exception catch", e);
        }
        if (callLogging && locallogging) {
            String rargs = new String();
            String cl;
            for (Object a : args) {
                rargs = rargs + " " + a.toString();
            }
            try {
                cl = logIt("COMPLETE: " + hb.toServType + " " + mn + " " + rargs);
                loggedCalls.add(cl + " COMPLETE: " + hb.toServType + " " + mn + " "
                        + rargs);
            } catch (IOException e) {
                System.err.println(myID + ": Exception logging:\n\t" + e);
            }
        }
        return returnobj;
    }

    /**
     * Return the ID of a pseudo-reference (generally, in <tt>type$name</tt>
     * format). A pseudo-reference's ID is useful for handling notification of
     * the component's state (in the {@link #componentNotify} method, triggered by
     * {@link ade.ADEComponentImpl#updateComponentInfo updateComponentInfo}).
     *
     * @param A <i>pseudo-reference</i> object
     * @return The ID
     */
    final protected String getRefID(Object ref) {
        if (ref != null) {
            return getKey(((Heartbeat) ref).toServType,
                    ((Heartbeat) ref).toServName);
        }
        return null;
    }

    /**
     * Set <i>failure notification</i> for a particular <i>pseudo-reference</i>
     * on or off. If set to <tt>true</tt>, this component will be notified if the
     * {@link ade.ADEComponentImpl#updateComponentInfo updateComponentInfo} receives
     * information from the {@link ade.ADERegistryImpl ADERegistryImpl} that
     * that component's {@link ade.ADEGlobals.ComponentState state} will return
     * <tt>true</tt> from a call to {@link ade.ADEGlobals.ComponentState#notify}.
     */
    final protected void setNotify(Object ref, boolean b) {
        try {
            Heartbeat hb = (Heartbeat) ref;
            if (dbg > 4 || debugHB) {
                System.out.println(myID + ": ******** setting notify for "
                        + getKey(hb.toServType, hb.toServName) + " to " + b);
            }
            hb.notify = b;
        } catch (ClassCastException e) {
            System.err.println("Not a valid reference object passed to setNotify");
        }
    }

    /**
     * Returns whether the pseudo-reference has an actual component reference.
     *
     * @param ref the pseudo-reference
     * @return <tt>true</tt> if the internal reference is set, <tt>false</tt>
     * otherwise
     * @throws ADEReferenceException Thrown if <tt>ref == null</tt>
     */
    final protected boolean gotComponent(Object ref)
            throws ADEReferenceException {
        if (ref == null) {
            throw new ADEReferenceException();
        }
        return ((Heartbeat) ref).gotPRef();
    }

    // MS: should this be removed for security reasons???
    /**
     * Checks whether the pseudo-reference implements the specified interface.
     *
     * @param ref The <i>pseudo-reference</i> to a component
     * @param iface The name of the interface
     * @return <tt>true</tt> if implemented, <tt>false</tt> otherwise
     */
    final protected boolean isType(Object ref, String iface) {
        Heartbeat hb = (Heartbeat) ref;
        for (String chk : hb.ifaces) {
            if (chk.indexOf(iface) >= 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the {@link ade.ADEGlobals.ComponentState state} of the
     * <i>pseudo-reference</i> parameter.
     *
     * @param ref The <i>pseudo-reference</i> to a component
     * @return The {@link ade.ADEGlobals.ComponentState state} of <tt>ref</tt>
     */
    final public ADEGlobals.ComponentState getState(Object ref) {
        if (ref != null) {
            return ((Heartbeat) ref).toState;
        }
        return ADEGlobals.ComponentState.INIT;
    }

    /**
     * Returns the {@link ade.ADEGlobals.RecoveryState RecoveryState} of the
     * <i>pseudo-reference</i> parameter.
     *
     * @param ref The <i>pseudo-reference</i> to a component
     * @return The {@link ade.ADEGlobals.RecoveryState RecoveryState} of
     * <tt>ref</tt>
     */
    final public ADEGlobals.RecoveryState getRecoveryState(Object ref) {
        if (ref != null) {
            return ((Heartbeat) ref).recState;
        }
        return ADEGlobals.RecoveryState.UNK;
    }

    // ***************************************************************
    // ***** The heartbeat, or pseudoreference class
    // ***************************************************************
    /**
     * The Heartbeat, responsible for maintaining periodic contact with remote
     * objects (ADEComponents) and acting as a gateway to communication with them.
     * In addition to periodically sending updates to connected components (i.e.,
     * performing the requirements of a client), the Heartbeat also takes care
     * of reconnection and, perhaps more importantly, controls all the remote
     * method invocations. <p> In support of the remote method invocations, when
     * created, the remote component is queried to supply the list of methods it
     * makes available to this component (as output by {@link ade.ADEGlobals#getMethodString}).
     * This allows the client to apply access rights/restrictions on a
     * per-method basis. <p> This class is private and final, such that
     * subclasses can have a reference to it, but cannot actually DO anything
     * with it--except use it as an entry point to the remote reference it
     * contains via the protected ADEComponent method calls that use the reference.
     */
    private class Heartbeat extends Thread { //implements Callable {

        private boolean shouldRun = true; // main loop control
        private int period; // sleep time
        private String myString; // for RMI
        private boolean attemptReconnect; // try reconnecting?
        private boolean anyReconnect; // to any component of a type?
        private AtomicBoolean reconnecting; // maintainance of internal status
        private boolean showStatus = true; // output control
        private Heartbeat thisHB; // self-reference
        private ADEGlobals.PRefState pRefState; // connection state
        private ADEGlobals.RecoveryState recState; // recovery state
        private String preface; // for display/debug
        // former RemoteInfo class
        private boolean toServ; // ADEComponent if true, else reg
        private ADEComponent toComponent; // the remote component reference
        private String toServType; // for recovery (re-request)
        private String toServName; // for recovery (re-request)
        private String toServHost; // the component's host
        private ADEGlobals.ComponentState toState; // state of the remote component
        private boolean notify = false; // notification if down?
        private HashSet<String> ifaces; // Interfaces implemented
        private ADERemoteCallTimer rct; // RMI timer for component
        private String[][] constraints; // the constraints with which the component was obtained

        /*
         * int period = ADEGlobals.DEF_HBPULSE; String type, name =
         * ADEGlobals.ALL_ACCESS; ADEComponent serv = null; boolean doReconn =
         * true, anyName = true; String[][] constraints = null;
         */
        /**
         * Heartbeat constructor.
         */
        private Heartbeat(int timediff, String t, String n, ADEComponent tocomponent,
                boolean doReconnect, boolean any, String[][] constr) {
            if (dbg > 2 || debugHB) {
                StringBuilder sb = new StringBuilder(myID);
                sb.append(": creating pseudoreference for ");
                sb.append(getKey(t, n));
                if (doReconnect) {
                    sb.append(" with reconnection");
                } else {
                    sb.append(" without reconnection");
                }
                System.out.println(sb.toString());
            }
            period = timediff;
            myString = getRMIStringMe();
            attemptReconnect = doReconnect;
            anyReconnect = any;
            thisHB = this;
            preface = "-------* " + myID + " " + thisHB + ":\n\t";
            reconnecting = new AtomicBoolean(false);
            pRefState = ADEGlobals.PRefState.INIT;
            recState = ADEGlobals.RecoveryState.UNK;
            // note: new RemoteInfo has to come after pRefState setting
            // for registries, as it'll be modified therein
            toComponent = tocomponent; // the remote component reference
            toServType = t; // for recovery (re-request)
            toServName = n; // for recovery (re-request)
            toServHost = ADEGlobals.DEF_REGIP;
            constraints = constr;
            if (toComponent != null) {
                if (dbg > 8 || debugHB) {
                    System.out.println(preface + "have a reference to "
                            + getKey(toServType, toServName) + "...");
                }
                if (toComponent instanceof ADERegistry) {
                    // note that we don't have to initialize() for a registry,
                    // since we use the rctReg for communication
                    toServ = false; // it is an ADERegistry
                    toState = ADEGlobals.ComponentState.RUN;
                    pRefState = ADEGlobals.PRefState.CONNECTED;
                } else {
                    toServ = true;
                    toState = ADEGlobals.ComponentState.INIT;
                }
            } else {
                // if null, can assume component (else we couldn't have contact)
                toServ = true;
                toState = ADEGlobals.ComponentState.INIT;
            }
        }

        /**
         * Initialize the Heartbeat, which consists of obtaining a remote
         * reference (via <tt>requestComponent</tt>), getting the allowed methods
         * (via <tt>setMethods</tt>), creating the remote call timers to component,
         * replacing the reference, and setting the <tt>pRefState</tt>. Note
         * that this method will never be called for <tt>ADERegistries</tt> --
         * the fact that this method is even being called requires a registry
         * connection.
         *
         * @param reapCycles The number of reaper cycles allowed for the
         * initialization to complete (after which, the remote component will
         * remove this user from its connections); we translate this into a time
         * (in ms) by multiplying it by the reaper time
         * @throws ADEReferenceException Thrown if initialization is
         * unsuccessful
         */
        private void initialize() {
            if (dbg > 3 || debugHB) {
                System.out.println(preface + "initializing heartbeat");
            }

	    // MS: TODO: this should be set more systematically, through ADEGlobals
	    // give up after trying three times -- this is important because it's a blocking call...
	    int maxattempt = 3;

	    for(int attempt = 0; attempt < maxattempt; attempt++) {
                // since this will almost always be called at startup (when
                // there
                // is almost no chance it will succeed), we sleep first to avoid
                // alarming the user
                try {
                    Thread.sleep(ADEGlobals.DEF_HEARTBEAT_CREATION_WAIT);
                } catch (Exception ignore) {
                }
		
		// check if this is the registry connection, then re-use the rctReg call timer
		if (toServType.equals("ade.ADERegistry")) {
		    rct = rctReg;
		}
		else {
		    // create a remote call timer for this component
		    try {
			rct = new ADERemoteCallTimer(ADEGlobals.DEF_HBPULSE, toComponent);
			rct.setName(myID + "->" + getKey(toServType, toServName));
			if (dbg > 5 || debugHB) {
			    System.out.println(preface + "created component rct...");
			}
		    } catch (ADEException ace) {
			System.err.println(myID + ": ADERemoteCallTimer failed:\n\t"
					   + ace);
		    }
		    
		    if (dbg > 5 || debugHB) {
			System.out.println(preface + "setting methods");
		    }
		    
		    // now set up the access rights for remote calls
		    try {
			// first get the common component methods, then specific ones for
			// the remote component
			rct.setAllMethods(ADEGlobals.getADEComponentMethods());
			// MS: NOTE: we need to use the userID as password because
			// this call goes directly to the
			// remote component (not mediated via the registry), and the
			// remote component does not store our password
			// the password is only available at the registry...
			HashMap<String, ADEMethodConditions> hm = (HashMap<String, ADEMethodConditions>) rct.remoteCall("requestMethods", toComponent, myID, myID,
														  toServType);
			// pass on only the method strings, the RCT will also set
			// the method conditions
			rct.setAllMethods(new ArrayList<String>(hm.keySet()));
		    } catch (ADERequestMethodsException are) {
			System.err.println("Error obtaining method information from remote component "
					   + toServType + " " + are);
		    } catch (ADEException ace) {
			System.err.println("Error in SetAllMethods "
					   + toServType + " " + ace);
		    }

		}
		if (dbg > 5 || debugHB) {
		    System.out.println(preface + "setMethods() complete...");
		}

		if (!replaceReference()) {
		    // something got bungled with bookkeeping for the new ref
		    // we're gonna have to try again...
		    System.out.println(preface + "Couldn't replace ref!");
		} else {
		    if (dbg > 5 || debugHB) {
			System.out.println(preface + "replaced reference");
		    }
		    if (dbg > 4 || debugHB) {
			System.out.println(preface + "starting heartbeat");
		    }
		    this.start();
		    if (dbg > 3 || debugHB) {
			System.out.print(preface + "leaving initialize; set ");
			System.out.println(getKey(toServType, toServName) + " to "
					   + toState);
		    }
		    return;
		}
	    }
        }

        /**
         * Got a new remote reference; adjust the information accordingly.
         *
         * @return <tt>true</tt> if successful, else <tt>false</tt>
         */
        private boolean replaceReference() {
            StringBuilder sb = new StringBuilder();
            ADEComponentInfo sinfo;

            if (dbg > 4 || debugHB || debugReconn) {
                System.out.println(preface + "replacing reference");
            }
            // only want to get the componentinfo if not a registry
            // TODO: why?
            if (!myInfo.isregistry && toServ) {
                // replacing direct calls with new timeout mechanism
                // should it be in a while() rather than an if?
                if (dbg > 5 || debugHB || debugReconn) {
                    System.out.println(preface + "getting component info...");
                }
                try {
                    sinfo = (ADEComponentInfo) rct.remoteCall(
                            "requestComponentInfo", toComponent, myID);
                    if (dbg > 5 || debugHB || debugReconn) {
                        System.out.println(preface + "got component info");
                    }
		    // MS: TODO: FXI THIS: this will get all the component interfaces and not the ones requested
		    // hence by calling isType it'll be possible to find out about all the interfaces a component implements
		    // regardless of how the reference was requested; this needs to be changed so that the hearbeat can only
		    // see "subinterfaces" of what was requested

                    ifaces = sinfo.getInterfaces();
                } catch (ADEException ace) {
                    sb.append(preface);
                    sb.append("Exception getting component info for ");
                    sb.append(toServType);
                    sb.append("\n\t");
                    sb.append(ace);
                    System.err.println(sb.toString());
                    if (debugReconn || debugHB) {
                        if (ace.getCause() != null) {
                            ace.getCause().printStackTrace();
                        } else {
                            ace.printStackTrace();
                        }
                    }
                    return false;
                }
                if (dbg > 5 || debugHB || debugReconn) {
                    System.out.println(preface + "getting host info...");
                }
                try {
                    toServHost = ((ADEHostInfo) rct.remoteCall(
                            "requestHostInfo", toComponent, myID)).hostip;
                    if (dbg > 5 || debugHB || debugReconn) {
                        System.out.println(preface + "got host info");
                    }
                } catch (ADEException ace) {
                    sb.setLength(0);
                    sb.append(preface);
                    sb.append("Exception getting host info for ");
                    sb.append(toServType);
                    sb.append("\n\t");
                    sb.append(ace);
                    System.err.println(sb.toString());
                    if (debugReconn || debugHB) {
                        if (ace.getCause() != null) {
                            ace.getCause().printStackTrace();
                        } else {
                            ace.printStackTrace();
                        }
                    }
                    return false;
                }
                // got here means we have a ref to the component and its
                // host information; need to see if the name or the host
                // we're storing for the component has changed...
                String newname = sinfo.getName();
                if (newname != null) {
                    if (dbg > 5 || debugHB || debugReconn) {
                        sb.setLength(0);
                        sb.append(preface);
                        sb.append("got ref name: ");
                        sb.append(newname);
                        sb.append(" on host ");
                        sb.append(toServHost);
                        System.out.println(sb.toString());
                    }
                    if (!toServName.equals(newname)) {
                        if (dbg > 7 || debugHB || debugReconn) {
                            sb.setLength(0);
                            sb.append(preface);
                            sb.append("different NAMES! ADJUSTING hb hash");
                            System.out.println(sb.toString());
                        }
                        // not only have to store the new name, also have
                        // to adjust the heartbeat hash...
                        // TODO: is this thread-safe? does it need to be?
                        // reconnectComponent() -> synch on reconnecting
                        myheartbeats.remove(getKey(toServType, toServName));
                        toServName = newname;
                        myheartbeats.put(getKey(toServType, toServName), this);
                        if (dbg > 8 || debugHB || debugReconn) {
                            sb.setLength(0);
                            sb.append(preface);
                            sb.append("DONE ADJUSTING");
                            System.out.println(sb.toString());
                        }
                    } else {
                        // MS: added to register the HB to a new component on the
                        // first time through
                        myheartbeats.put(getKey(toServType, toServName), this);
                    }
                    if (dbg > 3 || debugHB || debugReconn) {
                        sb.setLength(0);
                        sb.append(preface);
                        sb.append("connected to ");
                        sb.append(getKey(toServType, toServName));
                        sb.append(" on ");
                        sb.append(toServHost);
                        System.out.println(sb.toString());
                    }
                    // addComponent and set pRefState; note that componentConnectReact
                    // is not called here, as a remote call might be made there
                    // that would cause deadlock during heartbeat creation
                    if (dbg > 5 || debugHB || debugReconn) {
                        System.out.println(preface + "adding to myInfo");
                    }
                    myInfo.addComponent(getKey(toServType, toServName));
                    if (dbg > 5 || debugHB || debugReconn) {
                        System.out.println(preface + "setting pRefState");
                    }
                    pRefState = ADEGlobals.PRefState.CONNECTED;
                    /*
                     * if (debugTimes) { workTime =
                     * (int)(System.currentTimeMillis()-preReqTime);
                     * System.out.println(myID +": preReq reconn cycle took "+
                     * workTime +"ms"); workTime =
                     * (int)(System.currentTimeMillis()-postReqTime);
                     * System.out.println(myID +": postReq reconn cycle took "+
                     * workTime +"ms"); }
                     */
                } else {
                    // new name is null? how can this be? can't
                    // bookkeep, need to kill our connection...
                    sb.setLength(0);
                    sb.append(preface);
                    sb.append(toServName);
                    sb.append(" component has a null name?!");
                    System.out.println(sb.toString());
                    toComponent = null;
                    return false;
                }
            } else { // if (!myInfo.isregistry && toServ)
                // reg names don't change, so there's no need to remove
                myheartbeats.put(getKey(toServType, toServName), this);
                if (dbg > 3 || debugHB || debugReconn) {
                    sb.setLength(0);
                    sb.append(preface);
                    sb.append("connected to ");
                    sb.append(getKey(toServType, toServName));
                    sb.append(" on ");
                    sb.append(toServHost);
                    System.out.println(sb.toString());
                }
                myInfo.addComponent(getKey(toServType, toServName));
                pRefState = ADEGlobals.PRefState.CONNECTED;
            }
            if (dbg > 5 || debugHB || debugReconn) {
                System.out.println(preface
                        + "returning from replaceReference()");
            }
            return true;
        } // end of replaceReference()

        // TODO; only to registry for now, should work with other components as
        // well
        private void setPeriod(int p) {
            try {
                if (!toServ) {
                    rctReg.remoteCall("updateHeartBeatPeriod", ar, myString,
                            period);
                }
                period = p;
            } catch (ADEException e) {
                System.err.println(myID + ": Heartbeat period exception:\n\t"
                        + e);
            }
        }

        private boolean gotPRef() {
            return pRefState.equals(ADEGlobals.PRefState.CONNECTED);
        }

        private void setPRefState(ADEGlobals.PRefState s) {
            pRefState = s;
        }

        private void setRecState(ADEGlobals.RecoveryState s) {
            recState = s;
        }

        private void setServState(ADEGlobals.ComponentState s) {
            toState = s;
        }

        private synchronized void terminate() {
            if (dbg > 5) {
                System.out.println(myID + ": terminate HB to "
                        + getKey(toServType, toServName) + " called");
            }
            shouldRun = false;
            reconnecting.set(false);
            notify();
        }

        @Override
        public void run() {
            long startTime =0, oldstarttime = 0;
            int workTime = 0;

            if (dbg > 8 || debugHB) {
                System.out.println(preface + "hb to "
                        + getKey(toServType, toServName));
            }
            while (shouldRun) {
		oldstarttime = startTime;
		startTime = System.currentTimeMillis();
                if (dbg > 8 || debugHB) {
                    StringBuilder sb = new StringBuilder(preface);
                    sb.append(" to ");
                    sb.append(getKey(toServType, toServName));
                    sb.append("\n         mystate=");
                    sb.append(myInfo.state);
                    sb.append(", prefstate=");
                    sb.append(pRefState);
                    sb.append("; other component's state=");
                    sb.append(toState);
                    System.out.println(sb.toString());
                }
                switch (pRefState) {
                    case CONNECTED:
                        sendHeartbeat();
                        break;
                    case LOST:
                        reconnect();
                        break;
                    case CLOSE:
                        shouldRun = false;
                        break;
                    case SUSPEND:
                        break;
                    default:
                        StringBuilder sb = new StringBuilder(preface);
                        sb.append(" to ");
                        sb.append(getKey(toServType, toServName));
                        sb.append("Unknown PRefState: ");
                        sb.append(pRefState);
                        System.out.println(sb.toString());
                }
		workTime = (int) (System.currentTimeMillis() - startTime);
		if (debugTimes) {
		    System.out.println(myID + ": HB update took " + workTime + "ms (period=" + period + ")");
		    System.out.println(myID + ": HB overall cycle duration: " + (startTime - oldstarttime));		    
		}
                synchronized (this) {
                    try {
                        // MS: this should have been synchronized
                        // Thread.sleep(period);
			if (pRefState == ADEGlobals.PRefState.CONNECTED && period > workTime) {
			    // MS: added more accurate timing to the heartbeat loop when components are connected
			    wait(period - workTime);
			}
			// if the connection state is not CONNECTED, sleep for the usual time
			else {
			    wait(period);
			}
                    } catch (Exception e1) {
                        if (debugHB) {
                            System.out.println(myID + ": Heartbeat woke up.");
                        }
                    }
                }
            } // end while(mainrun)
            if (debugHB || debugReconn || dbg > 5) {
                StringBuilder sb = new StringBuilder(preface);
                sb.append(" exiting heartbeat for ");
                sb.append(getKey(toServType, toServName));
                System.out.println(sb.toString());
            }
            cleanup();
        } // end run()

        //
        private void cleanup() {
            String key = getKey(toServType, toServName);
            toComponent = null;
            //myheartbeats.remove(key);
        }

        private void sendHeartbeat() {
            if (dbg > 6 || debugHB || debugging) {
                System.out.println(preface + "sending heartbeat to "
                        + getKey(toServType, toServName));
            }
            if (toServ) {
                String key = getKey(toServType, toServName);
                if (dbg > 7 || debugHB) {
                    System.out.println(preface + "updating to " + key + "...");
                }
                try {
                    ADEGlobals.ComponentState tst = (ADEGlobals.ComponentState) rct.remoteCall("updateConnection", toComponent, myID);
                    setServState(tst);
                    if (dbg > 8 || debugHB) {
                        System.out.println(preface + "updated to " + toState
                                + "...");
                    }
                } catch (ADEException e) {
                    System.err.println(preface + "\n\t" + key
                            + " heartbeat failed!");
                    canLogIt(key + " heartbeat failed!");
                    if (debugHB || debugReconn) {
                        if (e.getCause() != null) {
                            e.getCause().printStackTrace();
                        } else {
                            e.printStackTrace();
                        }
                    } else {
                        ADEGlobals.printShortStackTrace(e);
                    }
                    pRefState = ADEGlobals.PRefState.LOST;
                    recState = ADEGlobals.RecoveryState.UNK;
                    componentDownReact(getKey(toServType, toServName), constraints);
                }
            } else {
                if (dbg > 7 || debugHB) {
                    System.out.println(preface + "updating status to "
                            + getKey(toServType, toServName) + "...");
                }
                try {
                    rctReg.remoteCall("updateStatus", toComponent,
                            myInfo.getADEMiniComponentInfo());
                    setServState(ADEGlobals.ComponentState.RUN);
                } catch (ADEException e) {
                    System.err.println(myID + ": heartbeat to "
                            + getKey(toServType, toServName)
                            + " failed!\n" + e);
                    if (debugHB || debugReconn) {
                        if (e.getCause() != null) {
                            e.getCause().printStackTrace();
                        } else {
                            e.printStackTrace();
                        }
                    }
                    pRefState = ADEGlobals.PRefState.LOST;
                    recState = ADEGlobals.RecoveryState.UNK;
                }
            }
            if (pRefState.equals(ADEGlobals.PRefState.LOST)) {
                // no remote reference or we've been removed from the user list!
                // component cannot be contacted; set it and then react
                if (dbg > 6 || debugHB || debugReconn) {
                    System.out.println(preface + "downed "
                            + getKey(toServType, toServName));
                }
            } else {
                if (dbg > 8 || debugHB) {
                    System.out.println(preface + "sent heartbeat to "
                            + getKey(toServType, toServName));
                }
            }
        }

        private void reconnect() {
            if (dbg > 4) {
                StringBuilder sb = new StringBuilder(preface);
                sb.append("checking reconnect to ");
                sb.append(getKey(toServType, toServName));
                sb.append("; reconnecting=");
                sb.append(reconnecting);
                System.out.println(sb.toString());
            }
            if (!attemptReconnect) {
                // no reconnection, set the state to CLOSE and shouldRun to
                // false;
                // cleanup will be done when exiting the thread...
                setPRefState(ADEGlobals.PRefState.CLOSE);
                shouldRun = false;
            }
            // if we don't attempt reconnect or already reconnecting, just
            // return
            // we generally expect reconnecting to be false here (to match the
            // first time we come through); subsequent times through will leave
            // it as is and just return
            if (reconnecting.compareAndSet(false, true)) {
                if (toServ) {
                    reconnectComponent();
                } else {
                    reconnectRegistry();
                }
            }
        }

        /**
         * Attempt to reconnect to a component (i.e., consult the registry if
         * necessary and re-get the remote reference if possible. The bulk of
         * this method is an internal thread that will run with the same period
         * as the heartbeat, but independently.
         */
        private void reconnectComponent() {
            final Heartbeat me = thisHB;

            //			if (debugHB && showStatus) {
            StringBuilder sb = new StringBuilder(preface);
            sb.append("Error contacting ");
            sb.append(getKey(toServType, toServName));
            sb.append("; attempting reconnection, states=");
            sb.append(pRefState);
            sb.append(", ");
            sb.append(recState);
            System.out.println(sb.toString());
            //			}
            /**
             * The reconnection thread, which is simply started.
             */
            new Thread() {
                String servtn = getKey(toServType, toServName);
                String recpref = "-------* " + myID + " " + this + ":\n\t";
                long startTime, preReqTime, reqTime, postReqTime;
                int workTime = 0;

                /**
                 * The reconnection thread's run method.
                 */
                @Override
                public void run() {
                    if (debugTimes) {
                        startTime = System.currentTimeMillis();
                    }
                    if (dbg > 2) {
                        System.out.println(recpref + "reconnecting to "
                                + servtn);
                    }
                    while (attemptReconnect && reconnecting.get()) {
                        if (debugTimes) {
                            preReqTime = System.currentTimeMillis();
                        }
                        // MS: added DOWN because this might still indicate that the component is gone...
                        if (recState.equals(ADEGlobals.RecoveryState.UNK) || recState.equals(ADEGlobals.RecoveryState.DOWN)) {
                            // if there was no response from a component, we
                            // consult
                            // the registry; note that if we cannot get the
                            // information from the registry, we have no way of
                            // knowing if the component is down, in recovery,
                            // unrecoverable, etc., so we leave it as UNK and
                            // try
                            // again. If we do get information, we return and go
                            // through the normal heartbeat process.
                            consultRegistry();
                        } else {
                            // we know the recovery state, so try to re-get
                            obtainReference();
                        }
                        try {
                            Thread.sleep(period);
                        } catch (Exception ignore) {
                        }
                    } // end while()
                    if (debugHB) {
                        System.out.print("*********** " + myInfo.name
                                + ": Exiting thread " + this);
                        System.out.println("; have " + myheartbeats.size()
                                + " heartbeats");
                    }
                    if (debugTimes) {
                        workTime = (int) (System.currentTimeMillis() - startTime);
                        System.out.println(myID + ": total reconn cycle took "
                                + workTime + "ms");
                    }
                } // end run()

                /**
                 * When a connection is lost (pRefState is LOST), attempt to
                 * obtain information about the failure from the registry. Note
                 * that this may change either pRefState or recState, which will
                 * potentially cause the thread to exit.
                 */
                private void consultRegistry() {
                    StringBuilder sb = new StringBuilder(recpref);

                    //try {
                    requestComponentState();
                    sb.append("got ");
                    sb.append(recState);
                    sb.append(" from registry for ");
                    sb.append(getKey(toServType, toServName));
                    //					} catch (Exception e) {
                    //	sb.append("failed state request for ");
                    //	sb.append(getKey(toServType, toServName));
                    //}
                    if ((dbg > 7 || debugHB || debugReconn) && showStatus) {
                        System.out.println(sb.toString());
                    }
                    if (recState != null) {
                        switch (recState) {
                            // when the component is not there, try to get a new one...
                            case NONEXISTENT:
                            case UNREC: // impossible to recover, so exit
                                // try to get a reference to a component of the same
                                // type this will do automatic component substitution by
                                // trying to get another component that fits the profile; if
                                // that doesn't work, continue, otherwise break;

                                // pass in the constraints stored for the original component here only with the name removed if its in there...
                                try {
				    String[][] newconstraints = ADEGlobals.removeKeyString("name",constraints);
                                    ADEComponentInfo asi = (ADEComponentInfo) rctReg.remoteCall("requestConnection", ar,
                                            myID, myPassword, myInfo.as, constraints);
				    toComponent = asi.as;
				    // now update the constraints
				    constraints = newconstraints;
				    // MS: TODO: do we need to replace name and type here in case we got something else?
                                    if (replaceReference()) {
                                        if (reconnecting.compareAndSet(true, false)) {
                                            // MS: run this in a separate thread to
                                            // not hold this thread up
                                            // also avoids errors from
                                            // componentConnectReact being caught here
                                            // by accident...
                                            new Thread() {

                                                @Override
                                                public void run() {
                                                    componentConnectReact(
                                                            getKey(toServType,
                                                            toServName),
                                                            me, constraints);
                                                }
                                            }.start();
                                            showStatus = true;
                                        }
                                    }
                                } catch (Exception e1) {
                                    // no component available
                                    setRecState(ADEGlobals.RecoveryState.UNK);
                                    if ((showStatus && dbg > 3) || dbg > 5
                                            || debugHB || debugReconn) {
                                        sb.setLength(0);
                                        sb.append(recpref);
                                        sb.append("failed request for any ");
                                        sb.append(toServType);
                                        sb.append("\n\t");
                                        sb.append(ADEGlobals.shortStackTrace(e1));
                                        System.err.println(sb.toString());
                                    }
                                    if (notify) {
                                        componentNotify(me, recState);
                                    }
                                    setPRefState(ADEGlobals.PRefState.CLOSE);
                                    attemptReconnect = false;
                                    shouldRun = false;
                                }
                                break;
                            case OK: // recovered, can reconnect
                            case INREC: // recovering
                            case DELAY: // delayed recovery
                            case DOWN: // no info from reg
                                // connection state still UNK; leave variables
                                break;
                            default:
                            // leave the pRefState LOST and recState UNK
                        }
                    }
                }

                /**
                 * The recovery state of the remote component has been determined
                 * and should be coming available; try to re-get the reference.
                 */
                private void obtainReference() {
                    StringBuilder sb = new StringBuilder(recpref);
                    if ((dbg > 8 || debugHB || debugReconn) && showStatus) {
                        sb.append("Still attempting reconnection to ");
                        sb.append(servtn);
                        sb.append(" component");
                        System.out.println(sb.toString());
                    }
                    // request the new reference
                    if (makeRequest()) {
                        // got a new remote reference; store (possibly adjusted)
                        // info
                        if (replaceReference()) {
                            if (reconnecting.compareAndSet(true, false)) {
                                // MS: run this in a separate thread to not hold
                                // this thread up
                                // also avoids errors from componentConnectReact
                                // being propagated here...
                                new Thread() {

                                    public void run() {
                                        componentConnectReact(
                                                getKey(toServType, toServName),
                                                me, constraints);
                                    }
                                }.start();
                                showStatus = true;
                            }
                            sb.setLength(0);
                            sb.append(recpref);
                            sb.append("Reconnected with ");
                            sb.append(getKey(toServType, toServName));
                            System.out.println(sb.toString());
                        }
                    } else {
                        if ((dbg > 8 || debugHB || debugReconn) && showStatus) {
                            sb.setLength(0);
                            sb.append(recpref);
                            sb.append("failed request");
                            System.err.println(sb.toString());
                        }
                    }
                }

                /**
                 * Make the request for a new remote reference.
                 *
                 * @return <tt>true</tt> if we got a ref, else <tt>false</tt>
                 */
                private boolean makeRequest() {
                    StringBuilder sb = new StringBuilder(recpref);
                    try {
                        // first, try to get connection to component of the same
                        // name
                        if (dbg > 7 || debugHB || debugReconn) {
                            sb.append("requesting ");
                            sb.append(servtn);
                            sb.append("...");
                            System.out.println(sb.toString());
                        }
                        if (debugTimes) {
                            reqTime = System.currentTimeMillis();
                        }
                        // requestComponent(toServType, toServName);
			// request the same name and type although the same name might not work...
			ADEComponentInfo asi =(ADEComponentInfo) rctReg.remoteCall("requestConnection", ar, myID, myPassword,
									     myInfo.as, new String[][]{
										 {"type", toServType},
										 {"name", toServName}});
			toComponent = asi.as;
                    } catch (Exception e) {
                        // if that didn't work AND we're requesting a specific
                        // component AND anyReconnect == true, try with name "any"
                        if (debugTimes) {
                            workTime = (int) (System.currentTimeMillis() - reqTime);
                            System.out.println(myID
                                    + ": specific request reconn cycle took "
                                    + workTime + "ms");
                        }
                        if (!toServName.equals(ADEGlobals.ALL_ACCESS)
                                && anyReconnect) {
                            if ((showStatus && dbg > 3) || dbg > 5 || debugHB
                                    || debugReconn) {
                                sb.setLength(0);
                                sb.append(recpref);
                                sb.append(ADEGlobals.shortStackTrace(e));
                                sb.append("\n\t");
                                sb.append(servtn);
                                sb.append(" not found; trying any type...");
                                System.out.println(sb.toString());
                            }
                            try {
                                // pass in the constraints stored for the original component here only with the name removed if its in there...
				String[][] newconstraints = ADEGlobals.removeKeyString("name", constraints);
				ADEComponentInfo asi = (ADEComponentInfo) rctReg.remoteCall("requestConnection", ar,
										      myID, myPassword, myInfo.as,newconstraints);
				toComponent = asi.as;
				// now update the constraints
				constraints = newconstraints;
                            } catch (Exception e1) {
                                setRecState(ADEGlobals.RecoveryState.UNK);
                                if ((showStatus && dbg > 3) || dbg > 5
                                        || debugHB || debugReconn) {
                                    sb.setLength(0);
                                    sb.append(recpref);
                                    sb.append("failed request for any ");
                                    sb.append(toServType);
                                    sb.append("\n\t");
                                    sb.append(ADEGlobals.shortStackTrace(e1));
                                    System.err.println(sb.toString());
                                }
                                return false;
                            }
                            if (debugTimes) {
                                workTime = (int) (System.currentTimeMillis() - reqTime);
                                System.out.println(myID
                                        + ": any request reconn cycle took "
                                        + workTime + "ms");
                            }
                        }
                    }
                    if (dbg > 7 || debugHB || debugReconn) {
                        sb.setLength(0);
                        sb.append(recpref);
                        sb.append("GOT A ");
                        sb.append(toServType);
                        System.out.println(sb.toString());
                    }
                    return true;
                }
            }.start();
        }

        /**
         * Consult the {@link ade.ADERegistry ADERegistry} for state information
         * about another {@link ade.ADEComponent ADEComponent}.
         *
         * @throws RemoteException if the request fails
         */
        private void requestComponentState() {
            ADEGlobals.RecoveryState ss;

            if (dbg > 7 || debugHB || debugReconn) {
                System.out.println(preface + "requestComponentState(" + toServType
                        + "," + toServName + ")");
            }
            try {
                setRecState((ADEGlobals.RecoveryState) rctReg.remoteCall(
                        "requestState", ar, myID, myPassword, toServType,
                        toServName));
            } catch (Exception e) {
                System.err.println("GOT EXCEPTION REQUESTING COMPONENT STATE " + e);
                if (dbg > 7 || debugHB || debugReconn) {
                    System.err.println(preface + "did not get state of "
                            + getKey(toServType, toServName));
                }
            }
        }

        private void reconnectRegistry() {
            // if this component is a registry, the heartbeat should stop;
            // reconnection is taken care of with registerWithRegistry
            if (myInfo.isregistry) {
                pRefState = ADEGlobals.PRefState.CLOSE;
                recState = ADEGlobals.RecoveryState.UNREC;
                showStatus = true;
                return;
            }
            if (dbg > 7 || debugHB || debugReconn) {
                System.err.print(preface + "Error contacting ");
                System.err.println("ADERegistry; attempting reconnection...");
            }
            // we keep trying for the main reg
	    while (ar == null || reconnecting.get()) {
		if (dbg > 8 || debugHB || debugReconn) {
		    System.out.println(preface + ": LOOKUP "
				       + getRMIStringReg());
		}
		try {
		    ar = (ADERegistry) Naming.lookup(getRMIStringReg());
		} catch (Exception e1) {
		    if (dbg > 8 || debugHB || debugReconn) {
			System.err.println(preface
					   + "Registry unavailable, waiting...");
		    }
		}
		toComponent = ar;
		try {
		    // TODO: could check if the returned name is the same
		    // as the current one
		    rctReg.remoteCall("registerComponent", ar,
				      myInfo.duplicate(), myPassword, false); // not
		    // sure
		    // "false"
		    // is
		    // right
		    if (reconnecting.compareAndSet(true, false)) {
			pRefState = ADEGlobals.PRefState.CONNECTED;
			showStatus = true;
			break;
		    }
		} catch (Exception e2) {
		    System.err.print(myID
				     + ": Could not re-register during ");
		    System.err.println("recovery processs; retrying...");
		}
		try {
		    sleep(period);
		} catch (Exception e1) {
		    if (verbose || debugHB || debugReconn) {
			System.out.println(myID
					   + ": heartbeat got woken up...");
		    }
		}
	    }
        }
    }

    // -------------------------- end heartbeat --------------------------------
    /**
     * returns all methods from the component with the given reference that are
     * accessible to the client
     */
    final protected HashMap<String, ADEMethodConditions> getComponentMethods(
            Object ref) throws ADERequestMethodsException {
        // cast to heartbeat
        try {
            Heartbeat hb = (Heartbeat) ref;
            return hb.rct.getAllMethods();
        } catch (ClassCastException e) {
            throw new ADERequestMethodsException(
                    "Wrong reference object used in getComponentMethods");
        }
    }

    /**
     * returns the pre-conditions associated with the method
     */
    final protected String[] getPreconditions(Object ref, String methodname,
            Object... args) throws ADERequestMethodsException {
        try {
            Heartbeat hb = (Heartbeat) ref;
            return hb.rct.getPreconditions(methodname, args);
        } catch (ClassCastException e) {
            throw new ADERequestMethodsException(
                    "Wrong reference object used in getComponentMethods");
        }
    }

    /**
     * returns the post-conditions associated with the method
     */
    final protected String[] getPostconditions(Object ref, String methodname,
            Object... args) throws ADERequestMethodsException {
        // cast to heartbeat
        try {
            Heartbeat hb = (Heartbeat) ref;
            return hb.rct.getPostconditions(methodname, args);
        } catch (ClassCastException e) {
            throw new ADERequestMethodsException(
                    "Wrong reference object used in getComponentMethods");
        }
    }

    /**
     * returns the failure-conditions associated with the method
     */
    final protected String[] getFailureconditions(Object ref,
            String methodname, Object... args)
            throws ADERequestMethodsException {
        try {
            Heartbeat hb = (Heartbeat) ref;
            return hb.rct.getFailureconditions(methodname, args);
        } catch (ClassCastException e) {
            throw new ADERequestMethodsException(
                    "Wrong reference object used in getComponentMethods");
        }
    }

    /**
     * Starts or stops local logging (can only be initiated by the
     * {@link ade.ADERegistry ADERegistry} with which this component is
     * registered). or the component itself
     *
     * @param credentials Access control permissions
     * @param state Start if <tt>true</tt>, stop if <tt>false</tt>
     * @throws AccessControlException If permission is not granted
     * @throws RemoteException If an exception occurs.
     */
    @Override
    final public void setLocalLogging(Object credentials, boolean state)
            throws RemoteException, AccessControlException {
        // follows only requests from the registry,
        if (dbg > 2) {
            System.out.println(myID + ": setLocalLogging " + state);
        }
        if (credentials.equals(ar) || credentials.equals(this)) {
            if (state && !locallogging) {
                // turn it on..
                // create file name with ADE Component name + time stamp
                // write initial time stamp into file
                try {
                    String ct = Long.toString(System.currentTimeMillis());
                    fwrLog = new FileWriter(myHost.getADELogs() + myInfo.type
                            + "_" + myInfo.name + "_" + ct);
                    locallogging = true;
                    logIt(ADELogHelper.LOG_START_TIME_FLAG);
                } catch (IOException e) {
                    throw new RemoteException(
                            "Problem creating log file on component "
                            + myInfo.name);
                }
            } else if (!state && locallogging) {
                // turn it off..
                try {
                    logIt(ADELogHelper.LOG_END_TIME_FLAG + "\n");
                    fwrLog.close();
                } catch (IOException e) {
                    throw new RemoteException(
                            "Problem closing log file on component " + myInfo.name);
                }
                locallogging = false;
            } else if (state && locallogging) {
                // another component started logging, so mark time to synchronize
                try {
                    logIt(ADELogHelper.LOG_SYNC_TIME_FLAG);
                } catch (IOException e) {
                    throw new RemoteException(
                            "Problem writing to log file on component "
                            + myInfo.name);
                }
            }
        } else {
            throw new AccessControlException(
                    "Only registries can initiate local logging.");
        }
    }

    /**
     * Gets the list of logged ADE calls since last invocation.
     *
     * @return an ArrayList with the calls that were logged
     */
    public ArrayList<String> getLoggedCalls() throws RemoteException {
        ArrayList<String> ret = (ArrayList<String>) loggedCalls.clone();
        loggedCalls.clear(); // possibility of losing a call, synchronize?
        return ret;
    }

    private boolean hasLogAccessCredentials(Object credentials,
            String messageIfNot) {
        if (credentials.equals(this) || credentials.equals(ar)
                || credentials.equals(logHelper.getCredentialsUUID())) {
            return true;
        } else {
            throw new AccessControlException(messageIfNot);
        }
    }

    @Override
    public void openPlaybackLogFile(Object credentials, String logFileName)
            throws IOException, AccessControlException, RemoteException {
        if (hasLogAccessCredentials(credentials,
                "Insufficient access rights for opening playback log file")) {
            logHelper.openPlaybackLogFile(logFileName);
        }
    }

    @Override
    public boolean hasPlaybackLogFile() throws RemoteException {
        return logHelper.hasPlaybackLogFile();
    }

    @Override
    public String getPlaybackLogFileName() throws RemoteException {
        return logHelper.getCurrentFileName();
    }

    @Override
    public int getPlaybackPosition() throws RemoteException {
        return logHelper.getPlaybackPosition();
    }

    @Override
    public int maxPlaybackPosition() throws RemoteException {
        return logHelper.maxPlaybackPosition();
    }

    @Override
    public void setPlaybackPosition(Object credentials, int position)
            throws RemoteException {
        if (hasLogAccessCredentials(credentials,
                "Insufficient access rights to set playback position")) {
            logHelper.setPlaybackPosition(position);
        }
    }

    @Override
    public void setPlaybackRunning(Object credentials, boolean run)
            throws RemoteException {
        if (hasLogAccessCredentials(credentials,
                "Insufficient access rights to set log playback running state")) {
            logHelper.setIsRunning(run);
        }
    }

    @Override
    public boolean getPlaybackRunning() throws RemoteException {
        return logHelper.getIsRunning();
    }

    /**
     * Start or stop system-wide logging. Calling this method contacts the
     * {@link ade.ADERegistry ADERegistry} with which this component is registered,
     * causing it to: (1) forward the request to other known registries and (2)
     * initiate local logging in the other components registered with it.
     *
     * @param state Start logging if <tt>true</tt>, stop if <tt>false</tt>
     * @throws AccessControlException If this component does not have permissions
     * to start/stop logging
     * @throws ADETimeoutException If contact with the registry times out
     * @throws ADEException If some other problem occurs when contacting the
     * registry
     */
    final protected void setADEComponentLogging(boolean state)
            throws ADETimeoutException, ADEException,
            AccessControlException {
        // if (locallogging != state)
        // rctReg.remoteCall("setADEComponentLogging", ar, myID, myPassword,
        // state,null);

        // this will cause the component to register, making it visible in the
        // system (able to both request connections and provide services)
        finishedInitialization(this);
        try {
            /*
             * if (ar == null) System.out.println("ar == null!"); while (ar ==
             * null) Thread.yield();
             */
            // MS: even though this needs to be fast, it should not be allowed
            // to permanently freeze...
            rctReg.remoteCall("setADEComponentLogging", ar, myID, myPassword,
                    state, this);
        } catch (ADEException ace) {
            System.err.println("Exception setting ADEComponentLogging: " + ace);
        }
    }

    /**
     * Log an object locally.
     *
     * @param o The object to log
     * @return The logging timestamp (as a <tt>String</tt>)
     * @throws IOException If writing failed or local logging is not enabled
     */
    final protected String logIt(Object o) throws IOException {
        if (locallogging) {
            String ct = Long.toString(System.currentTimeMillis());
            fwrLog.write(ct + ADELogHelper.TIME_STAMP_SEPARATOR + o.toString()
                    + System.getProperty("line.separator"));
            fwrLog.flush();
            return ct;
        } else {
            throw new IOException("Local logging not enabled.");
        }
    }

    /**
     * Logs an object if possible (does not throw exceptions).
     *
     * @param o The object to log
     * @param silent If true, will NOT print error if logging failed
     */
    final protected void canLogIt(Object o, boolean silent) {
        try {
            logIt(o);
        } catch (IOException e) {
            if (!silent) {
                System.err.println(myID + ": Exception logging:\n\t" + e);
            }
        }
    }

    /**
     * Logs an object if possible (does not throw exceptions). NOT silent. See
     * canLogIt(Object o, boolean silent)
     *
     * @param o
     */
    final protected void canLogIt(Object o) {
        canLogIt(o, true);
    }

    /**
     * Turns state maintenance on via an exterior source.
     *
     * @param period The amount of time (in ms) between saves
     * @param credentials Permissions check
     * @throws AccessControlException If <tt>credentials</tt> are insufficient
     */
    public final void enableState(int period, Object credentials)
            throws AccessControlException {
        // follows only requests from the registry,
        if (credentials.equals(ar)) {
            if (dbg > 2) {
                System.out.println(myID + ": enabling state maintenance");
            }
            enableState(period);
        }
    }

    /**
     * Turns state maintenance off via an exterior source.
     *
     * @param credentials Permissions check
     * @throws AccessControlException If <tt>credentials</tt> are insufficient
     */
    public final void disableState(Object credentials)
            throws AccessControlException {
        // follows only requests from the registry,
        if (credentials.equals(ar)) {
            if (dbg > 2) {
                System.out.println(myID + ": disabling state maintenance");
            }
            disableState();
        }
    }

    /**
     * Turns the state maintenance mechanism on. Note that <i>tags</i>, used to
     * access storage slots, must be either explicitly added to the created
     * {@link ade.ADEState ADEState} object (using the {@link #addTag} method)
     * or implicitly as part of the {@link #saveState} method.
     *
     * @param period The amount of time (in ms) between saves
     */
    protected final void enableState(int period) {
        maintainState = true;
        final int cycle = period;
        new Thread() {

            @Override
            public void run() {
                // we need to wait until the remote call timer to the registry
                // is created (only happens on startup)
                while (maintainState && rctReg == null) {
                    try {
                        Thread.sleep(cycle);
                    } catch (InterruptedException ie) {
                    }
                }
                if (state == null) {
                    state = new ADEState();
                }
                while (maintainState) {
                    saveState(state);
                    try {
                        rctReg.remoteCall("setComponentState", ar, myID,
                                myPassword, state);
                    } catch (ADEException ace) {
                        System.err.println(myID + ": could not send state:\n"
                                + ace);
                    }
                    try {
                        Thread.sleep(cycle);
                    } catch (InterruptedException ie) {
                    }
                }
            }
        }.start();
    }

    /**
     * Turns the state maintenance mechanism off.
     */
    protected final void disableState() {
        // TODO: do we want to make sure the registry also deletes its copy?
        maintainState = false;
    }

    /**
     * Add a <i>tag</i> (i.e., a storage slot) to the state. This is in case an
     * implementer wants to set up the <tt>ADEState</tt> object prior to
     * enabling. The value will be initialized as the string <tt>unset</tt>.
     *
     * @param tag The name of the slot
     */
    protected final void addTag(String tag) {
        if (state == null) {
            state = new ADEState();
        }
        state.setValue(tag, "unset");
    }

    /**
     * To be overridden by subclass if maintaining state is enabled. This method
     * should contain a series of {@link ade.ADEState#setValue} calls, with a
     * corresponding set of {@link ade.ADEState#getValue} calls in the
     * {@link #loadState} method. Note that enabling state mainenance is
     * performed with the {@link #enableState} method.
     *
     * @param s An <tt>ADEState</tt> object that will passed to the method to be
     * filled with string key and object value pairs
     */
    protected void saveState(ADEState s) {
    }

    /**
     * To be overridden by subclass if maintaining state is enabled. This method
     * should contain a series of {@link ade.ADEState#setValue} calls, with a
     * corresponding set of {@link ade.ADEState#getValue} calls in the
     * {@link #loadState} method. Note that enabling state mainenance is
     * performed with the {@link #enableState} method.
     *
     * @param s An <tt>ADEState</tt> object containing string keys and object
     * values that will passed to the method
     */
    protected void loadState(ADEState s) {
    }

    // ***************************************************************
    // ***** The reaper class
    // ***************************************************************
    /**
     * This keeps track of and removes users that haven't checked in.
     */
    private class Reaper extends Thread {

        int period; // how often reaper will sleep
        boolean shouldReap = true;

        /**
         * Reaper constructor.
         *
         * @param timediff How many milliseconds the reaper should sleep between
         * reaping runs.
         */
        Reaper(int timediff) {
            // The list of users using heartbeats should really be put into a
            // priority queue, in which case only those users using a heartbeat
            // would need to be checked, and the sleep value could be calculated
            // more precisely. Perhaps add an ArrayList of components that should
            // be checked?
            period = timediff;
        }

        public int getPeriod() {
            return period;
        }

        void setPeriod(int ms) {
            period = ms;
        }

        synchronized void terminate() {
            shouldReap = false;
            notify();
        }

        @Override
        public void run() {
            String user;
            Set users;
            Iterator it;
            long timecomp, lasttime;

	    long starttime = 0, oldstarttime = 0;
            while (shouldReap) {
		oldstarttime = starttime;
		starttime = System.currentTimeMillis();
                users = userMap.keySet();
                if (users != null) {
                    it = users.iterator();
                    while (it.hasNext()) {
                        user = (String) it.next();
                        if (debugRP) {
                            System.out.println(myID + ": reaper checking "
                                    + user);
                        }
                        lasttime = userMap.get(user).longValue();
                        timecomp =  System.currentTimeMillis() - lasttime;
                        // check for two conditions: (1) user never checked in
                        // and
                        // grace period passed and (2) user missed their
                        // check-in;
                        // remove in either case
                        // NOTE the negative of reaperRemoveUser, because the
                        // count
                        // is decremented but the setting is positive
                        if (lasttime < -reaperRemoveUser
                                || (period < timecomp && lasttime > 0)) {
                            if (dbg > 5 || debugReconn || debugRP) {
                                System.out.print(myID + " reaper:");
                                System.out.print(" removing " + user
                                        + ", span=");
                                System.out.print("" + timecomp);
                                System.out.println(", period=" + period);
                            }
			    System.err.println("**************************** REAPER DETECTED PROBLEM, lasttime: " + lasttime);
			    System.err.println("**************************** REAPER: lasttime < - reaperRemoveUser " + (lasttime < -reaperRemoveUser));
			    System.err.println("**************************** REAPER: (period < timecomp && lasttime > 0 " + (period < timecomp && lasttime > 0));
                            if (!clientDownReact(user)) {
                                // TODO: check if "user" has no other
                                // connections,
                                // then remove...
                                it.remove();
                                myInfo.removeClient(user);
                            }
                            if (dbg > 4) {
                                System.out.println(myID
                                        + ": "
                                        + user
                                        + " "
                                        + (userMap.containsKey(user) ? "still there!!!!"
                                        : "removed"));
                            }
                        } else if (lasttime < 0) {
                            // user never checked in but is still in grace
                            // period
                            if (debugRP || dbg > 6) {
                                System.out.println("\t" + user
                                        + " not yet initialized");
                            }
                            userMap.put(user, new Long(lasttime - 1));
                        }
                    }
                }
                synchronized (this) {
                    try {
			//TODO: should reduce REAPER wait to account for work load in the above while loop to make period as closely as possible
                        wait(period);
                    } catch (Exception e) {
                        if (debugRP) {
                            System.out.println("Reaper got woken up.");
                        }
                    }
                }
                if (dbg > 8 || debugRP) {
                    System.out.println(myID + ": exiting reaper");
                }
            }
        }
    }

    // -------------------------- end reaper -----------------------------------
    // returns the suspended status of this component
    final protected boolean isSuspended() {
        return suspended;
    }

    // returns the live mode status of this component
    @Override
    final public boolean isLive() throws RemoteException {
        return liveMode;
    }

    // new interface methods for registries
    /**
     * Suspends the main loop of the component. Note that this will only cause the
     * main loop to be suspended; any other processes need to be manually
     * suspended (e.g., based on monitoring the suspended status); only
     * registries can call this method for now
     */
    final public void suspendComponent(Object credentials) throws RemoteException,
            AccessControlException {
        if (credentials.equals(this) || credentials.equals(ar)) {
            suspended = true;
        } else {
            throw new AccessControlException(
                    "Insufficient credentials for suspending component");
        }
    }

    /**
     * Resumes a suspended component. Note that this will only cause the main loop
     * to resume; any other processes need to be manually resumed (e.g., based
     * on monitoring the suspended status); only registries can call this method
     * for now
     */
    @Override
    final public void resumeComponent(Object credentials) throws RemoteException,
            AccessControlException {
        if (credentials.equals(this) || credentials.equals(ar)) {
            suspended = false;
        } else {
            throw new AccessControlException(
                    "Insufficient credentials for resuming component");
        }
    }

    /**
     * Puts the component into live mode (which calls updateComponent) versus log mode
     * (which calls updateLog) in the main loop; note that this will only cause
     * the main loop to switch between modes, all other processes need to be
     * manually adjusted (e.g., based on monitoring the liveMode status); only
     * registries, the component itself, and the log-playback toolbar
     * (ADELogPlaybackVis) can call this method for now
     */
    @Override
    final public void setUpdateModeLive(Object credentials, boolean mode)
            throws RemoteException, AccessControlException {
        // note:  if the request is made by ADELogPlaybackVis, it will be
        //     passing back a UUID that can be checked against logHelper's
        //     credentials UUID.  For a more detailed explanation, see ADELogHelper.
        if (credentials.equals(this) || credentials.equals(ar)
                || credentials.equals(logHelper.getCredentialsUUID())) {
            liveMode = mode;
        } else {
            throw new AccessControlException(
                    "Insufficient credentials for setting update mode");
        }
    }

    /**
     * Allows/disallows dynamic adjustments of looptime when the component fails to
     * update within one cycle (only preliminary implementation so far)
     */
    @Override
    final public void setDynamicLoopTime(Object credentials, boolean on)
            throws RemoteException, AccessControlException {
        if (credentials.equals(this) || credentials.equals(ar)) {
            dynamicloop = on;
        } else {
            throw new AccessControlException(
                    "Insufficient credentials for setting dynamic loop time");
        }
    }

    /**
     * Sets the looptime of the component's main loop; the component will attempt to
     * stay within the loop time for each update cycle (by sleeping the
     * remaining time when the cycle finishes early); it will dynam
     */
    @Override
    final public void setUpdateLoopTime(Object credentials, long time)
            throws RemoteException, AccessControlException {
        if (credentials.equals(this) || credentials.equals(ar)) {
            looptime = time;
        } else {
            throw new AccessControlException(
                    "Insufficient credentials for updating loop time");
        }
    }

    final public long getLoopTime() {
        return looptime;
    }

    // TODO: THIS SHOULD NOT BE CALLED VIA THE STANDARD RUNNABLE INTERFACE...
    // for security reasons, make this all local...
    // =========================================================================
    // the run method, which will be called once the component is ready
    @Override
    final public void run() {
        long sleeptime = looptime; // sleep for 100 msec by default

        // System.out.println("Starting main loop...");
        while (operating) {
            // start by checking if this component is suspended
            while (suspended) {
                // we're suspended, so go to sleep until woken up
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException ie) {
                        // handle interruptions properly
                    }
                }
            }
            while (!suspended) {
                if (liveMode) {
                    long startT = System.currentTimeMillis();

                    updateComponent();

                    // here we will implement different timing mechanisms...
                    // discrete updates, ticks/time-unit, etc.
                    // this will include adaptive speed-up and slow-down
                    long endT = System.currentTimeMillis();
                    if ((sleeptime = (endT - startT - looptime)) < 0) {
                        try {
                            Thread.sleep(-sleeptime);
                        } catch (InterruptedException ie) {
                            // handle interruptions properly
                        }
                    } // need to notify the other components (e.g., via
                    // the registry)
                    // about the increment in loop time so that they can
                    // also decide whether
                    // they need dynamically adjust their loops
                    else if (dynamicloop) {
                        looptime += dynamicincrement;
                    } // otherwise just bite the bullet...
                    else {
                        if (dbg > 3) {
                            System.err.println(myID
                                    + ": did not make my loop time by "
                                    + (sleeptime) + " msec");
                        }
                    }
                } else { // if not live, update using the log:

                    // if playing is within bounds and should be running:
                    if (logHelper.playbackIndexWithinBounds()
                            && logHelper.getIsRunning()) {
                        // first, see where I'm at:
                        // determine the goal time. also make sure that it's not
                        // out of bounds
                        if (logHelper.playbackIndexWithinBounds(logHelper.getPlaybackLineIndex() + 1)) {
                            int diff;
                            while ((diff = (logHelper.getTime(logHelper.getPlaybackLineIndex() + 1) - logHelper.getPlaybackPosition())) > 5) {
                                // hopefully my current playback time is == the
                                // "goal" time as specified by the time stamp.
                                // if within small margin of error (say, a few
                                // milliseconds), call it good enough
                                if (Math.abs(diff) <= 5) {
                                    // good enough
                                } else if (diff < 0) {
                                    System.out.println("Late with log playback by "
                                            + (diff * -1)
                                            + " milliseconds!");
                                } else if (diff > 0) {
                                    try {
                                        int logSleepDuration = Math.min(
                                                diff - 1, 200); // sleep no more
                                        // than 200 ms
                                        // -- that
                                        // way, will update position in playback
                                        // display gui at least "roughly",
                                        // rather
                                        // than waiting 20 seconds before the
                                        // next entry!
                                        long startSleepTime = System.currentTimeMillis();
                                        Thread.sleep(logSleepDuration);
                                        long endSleepTime = System.currentTimeMillis();
                                        logHelper.advancePlaybackTime((int) ((endSleepTime - startSleepTime)));
                                    } catch (InterruptedException e) {
                                        System.out.println("Could not sleep for "
                                                + diff
                                                + "; might start "
                                                + "getting off time if the insomnia continues!");
                                    }
                                }
                            }

                            long startTimeBeforeUpdate = System.currentTimeMillis();
                            String contents = logHelper.getContents(logHelper.getPlaybackLineIndex());
                            updateFromLog(contents);

                            logHelper.advancePlaybackLineIndex();
                            // check how much time has elapsed, and add this to
                            // playback position
                            long endTimeAfterUpdate = System.currentTimeMillis();
                            long elapsedAlready = (endTimeAfterUpdate - startTimeBeforeUpdate);
                            logHelper.advancePlaybackTime((int) elapsedAlready);
                        } else { // else (if out of bounds for the "goal" time)
                            logHelper.advancePlaybackLineIndex(); // that way
                            // advances
                            // ahead so
                            // now
                            // even the current position is out of bounds.
                            System.out.println("Done playing back...");
                        }

                    } else { // ie: if out of bounds already, or supposed to be
                        // paused
                        // if paused, still want to update -- to PREVIOUS state,
                        // if any:
                        if (!logHelper.getIsRunning()) {
                            int earlierIndex = Math.max(0,
                                    logHelper.getPlaybackLineIndex() - 1);
                            if (logHelper.playbackIndexWithinBounds(earlierIndex)) {
                                String contents = logHelper.getContents(earlierIndex);
                                updateFromLog(contents);
                            }
                        }

                        Util.Sleep(100);
                        // sleep, waiting for next command, I guess...
                    }
                }
            }
        }
        // System.out.println("Ending main loop...");
    }

    // =========================================================================
    // command line options valid for all ADEComponents
    private void usage() {
        System.out.println("Command line options are:");
        System.out.println("     -h -H --help           <display command line switches / options>");
        System.out.println("     -v -V --vmargs         <<args> Java VM arguments (ending with a lone \"--\")");
        System.out.println("     -l -L --local          <<IP-address> [port] [name] of this component>");
        System.out.println("     -r -R --registry       <<IP-address> [port] [name] of registry>");
        System.out.println("     -f -F --file           <<file> with configuration info for this component>");
        System.out.println("     -e -E --error          <<#> of error restarts for this component>");
        System.out.println("     -c -C --credentials    <<string> representation of credentials>");
        System.out.println("     -a -A --access         <<strings> describing access rights to this component>");
        System.out.println("     -o -O --onlyonhosts    <<IP-address[es]> of host restrictions>");
        System.out.println("     --devices              <<strings> describing devices this component requires>");
        System.out.println("     --groups               <<strings> describing groups this component belongs to>");
        System.out.println("     --connecttogroups      <<strings> describing groups this component connects to>");
        System.out.println("     -d -D --debug          <<#> level of debugging outputfor this component>");
        System.out.println("     -g -G --gui            <displays all default standalone GUI visualizations for the component,");
        System.out.println("                               or ones specified by name (e.g., [vis_1] [vis_2] ...).");
        System.out.println("                               to select available visualizations from a prompt, ");
        System.out.println("                               pass \"-?\" into the GUI args.>");
        System.out.println("     --persistentGUI        <GUI windows are only closed on component shutdown,");
        System.out.println("                               can't be accidentally closed.>");
        System.out.println("     -u -U --userID         <set the userID for this component>");
        System.out.println("     --uilogging            <Redirected stdout/stderr to a file for access on-demand via GUI>");
        System.out.println("     --logging              <Start local logging>");
        System.out.println("     --calllogging          <Start local logging including ADE calls>");
        System.out.println("     --playback [filename]  <Start in log playback (ie: NOT live) mode>");
        System.out.println("");
        System.out.println(additionalUsageInfo());
    }

    /*
     * MS: not used anymore // function to retrieve the class from a static
     * context private static class CurrentClassGetter extends SecurityManager {
     *
     * String getClassName() { return getClassContext()[1].getName(); }
     *
     * Class getCurrentClass() { return getClassContext()[1]; }
     *
     * Class getFirstClass() { Class[] c = getClassContext(); return c[c.length
     * - 1]; }
     *
     * String getClassNameFirst() { Class[] c = getClassContext(); return
     * c[c.length - 1].getName(); } }
     */
    private String uiLoggingPath = "";

    public String getUILoggingPath() {
        return uiLoggingPath;
    }

    private void redirectLogs() {
        try {
            if (!uiLoggingPath.equals("")) {
                return;
            }
            String classname = getClass().getName();
            int r = new Random().nextInt();
            r = Math.abs(r);
            File tempFile = File.createTempFile("ade-" + classname
                    + "-stdouterr-" + r, ""); // , myInfo.startdirectory);
            uiLoggingPath = tempFile.getAbsolutePath();
            System.setErr(new PrintStream(new FileOutputStream(tempFile)));
            System.setOut(new PrintStream(new FileOutputStream(tempFile)));
        } catch (Throwable t) {
            System.err.println("Error overriding standard output to file.");
            t.printStackTrace(System.err);
        }
    }

    /**
     * Returns a data structure that hold GUI visualization information for the
     * component. Instances of the abstract ADEComponentImpl must call
     * super.getVisualizationSpecs() before appending their own (that way, will
     * inherit common ADEComponentInfo visualizations, such as the log-playback
     * window)
     */
    @Override
    public ADEGuiVisualizationSpecs getVisualizationSpecs()
            throws RemoteException {
        ADEGuiVisualizationSpecs visSpecs = new ADEGuiVisualizationSpecs();

        // add the log-playback visualization:
        visSpecs.add("LogView", ADELogPlaybackVis.class,
                ADEGuiVisualizationSpecs.StartupOption.SHOW_BY_EXPLICIT_REQUEST_ONLY,
                IconFetcher.get16x16icon("appointment-new.png"),
                logHelper.getCredentialsUUID());

        return visSpecs;
    }

    // this parses the commandline and fills in as much as possible into
    // the ADEComponentInfo data structure
    private ADEComponentInfo parsecommandline(String[] args) {
        // create the component info data structure
        ADEComponentInfo me = new ADEComponentInfo(this instanceof ADERegistryImpl);
        LinkedList<String> arglist = new LinkedList<String>();
        String arg = null, tmp;
        boolean needaddargs = false;
        boolean passthrough = false;
        ArrayList<String> addargs = new ArrayList<String>();
        String pathsep = System.getProperty("path.separator");
        boolean redirectLogsAtTheEnd = false;
        // parse command line args; first we set up a linked list for easy
        // removal of args as we handle them
        if (dbg > 5) {
            System.out.print(myID + ": parsing command line...");
        }
        for (String anarg : args) {
            arglist.add(anarg);
        }

        try {
            if (dbg > 5) {
                System.out.print("\t");
            }
            // for (int i=0; i<arglist.size(); i++) {
            while (arglist.size() > 0) {
                arg = arglist.remove();
                if (dbg > 5) {
                    System.out.print("got [" + arg + "]; ");
                }
                if (passthrough) {
                    if (arg.equals("--")) {
                        passthrough = false;
                    }
                    if (dbg > 6) {
                        System.out.print(" (add) ");
                    }
                    needaddargs = true;
                    addargs.add(arg);
                } else {
                    if (arg.equals("--xargs")) {
                        passthrough = true;
                        if (dbg > 6) {
                            System.out.print(" (add) ");
                        }
                        needaddargs = true;
                        addargs.add(arg);
                    } else if (arg.equalsIgnoreCase("-h")
                            || arg.equals("--help")) {
                        usage();
                        System.exit(ADEGlobals.ExitCode.OK.code());
                    } else if (arg.equalsIgnoreCase("-u")
                            || arg.equals("--uilogging")) {
                        redirectLogsAtTheEnd = true;
                    } else if (arg.equalsIgnoreCase("-d")
                            || arg.equals("--debug")) {
                        if ((tmp = arglist.peek()) != null
                                && !tmp.startsWith("-")) {
                            try {
                                dbg = Integer.parseInt(arglist.remove());
                                me.dbg = dbg;
                                if (verbose || dbg > 4) {
                                    System.out.println(prg
                                            + ": set debug level to " + dbg);
                                }
                            } catch (Exception e) {
                                System.err.println(prg
                                        + ": Exception setting debug level:");
                                System.err.println(e);
                            }
                        } else {
                            System.err.println(prg
                                    + ": missing debug level argument");
                        }
                    } else if (arg.equalsIgnoreCase("-v")
                            || arg.equals("--vmargs")) {
                        if (dbg > 6) {
                            System.out.print(myID + " got JVM arguments:");
                        }
                        StringBuilder vmargsBuilder = new StringBuilder();
                        boolean gotone = false;
                        while ((tmp = arglist.peek()) != null
                                && (!tmp.equals("--"))) {
                            if (dbg > 6) {
                                System.out.print(" " + tmp);
                            }
                            gotone = true;
                            vmargsBuilder.append(arglist.remove());
                            vmargsBuilder.append(" ");
                        }
                        // will have ended on a "--", so remove that
                        // end-of-vmargs-signifier as well:
                        arglist.remove();
                        if (dbg > 6) {
                            System.out.println();
                        }
                        if (gotone) {
                            me.javavmargs = vmargsBuilder.toString().trim();
                        }
                    } else if (arg.equalsIgnoreCase("-l")
                            || arg.equals("--local")) {
                        // the local information
                        useIP = new String(arglist.remove());
                        // me.setHostInfo(arglist.remove());
                        // check if a port was supplied as well
                        if ((tmp = arglist.peek()) != null
                                && !tmp.startsWith("-")) {
                            try {
                                me.port = Integer.parseInt(arglist.remove());
                                if (me.port < 1025 || me.port > 65535) {
                                    System.err.println("Port must be between 1025 and 65535");
                                    System.exit(ADEGlobals.ExitCode.PORT.code());
                                }
                                if ((tmp = arglist.peek()) != null
                                        && !tmp.startsWith("-")) {
                                    me.name = arglist.remove();
                                }
                            } catch (NumberFormatException e) {
                                me.name = arglist.remove();
                                if ((tmp = arglist.peek()) != null
                                        && !tmp.startsWith("-")) {
                                    try {
                                        me.port = Integer.parseInt(arglist.remove());
                                        if (me.port < 1025 || me.port > 65535) {
                                            System.err.println("Port must be between 1025 and 65535");
                                            System.exit(ADEGlobals.ExitCode.PORT.code());
                                        }
                                    } catch (NumberFormatException e1) {
                                        System.out.println("Bad port number: "
                                                + tmp);
                                        usage();
                                        System.exit(ADEGlobals.ExitCode.PORT.code());
                                    }
                                }
                            }
                        }
                    } else if (arg.equalsIgnoreCase("-r")
                            || arg.equals("--registry")) {
                        // the registry
                        me.registryIP = new String(arglist.remove());
                        // if this is itself a registry, the registryName will
                        // be
                        // null by default; if the "-r" switch is encountered,
                        // we
                        // should fill it in if it's null, otherwise this
                        // registry
                        // will not contact the other as it's supposed to...
                        if (me.registryName == null) {
                            me.registryType = ADEGlobals.DEF_REGTYPE;
                            me.registryName = ADEGlobals.DEF_REGNAME;
                            // Quick fix to allow registry to register with
                            // others
                            // me.registryIP = ADEGlobals.DEF_REGIP;
                            me.registryPort = ADEGlobals.DEF_REGPORT;
                        }
                        // check if a port was supplied as well
                        if ((tmp = arglist.peek()) != null
                                && !tmp.startsWith("-")) {
                            try {
                                me.registryPort = Integer.parseInt(arglist.remove());
                                if (me.registryPort < 1025
                                        || me.registryPort > 65535) {
                                    System.err.println("Port must be between 1025 and 65535");
                                    System.exit(ADEGlobals.ExitCode.PORT.code());
                                }
                                if ((tmp = arglist.peek()) != null
                                        && !tmp.startsWith("-")) {
                                    me.registryName = arglist.remove();
                                }
                            } catch (NumberFormatException e) {
                                me.registryName = arglist.remove();
                                if ((tmp = arglist.peek()) != null
                                        && !tmp.startsWith("-")) {
                                    try {
                                        me.registryPort = Integer.parseInt(arglist.remove());
                                        if (me.registryPort < 1025
                                                || me.registryPort > 65535) {
                                            System.err.println("Port must be between 1025 and 65535");
                                            System.exit(ADEGlobals.ExitCode.PORT.code());
                                        }
                                    } catch (NumberFormatException e1) {
                                        System.out.println("Expected port number for registry");
                                        usage();
                                        System.exit(ADEGlobals.ExitCode.PORT.code());
                                    }
                                }
                            }
                        }
			// delay start-ups of config until other registries have checked in
			// in case this is a registry; not used for regular components
		        delaystartup = true;
                    } else if (arg.equalsIgnoreCase("-f")
                            || arg.equals("--file")) {
                        configFileName = me.configfile = arglist.remove();
                        File f = ADEGlobals.getADEConfigFile(me.configfile);
                        if (f == null) {
                            System.err.println("Configuration file "
                                    + me.configfile + " not found");
                            System.exit(ADEGlobals.ExitCode.CONFIG_FILE.code());
                        }
                    } else if (arg.equalsIgnoreCase("-e")
                            || arg.equals("--error")) {
                        me.numrestarts = Integer.parseInt(arglist.remove());
                    } else if (arg.equalsIgnoreCase("-c")
                            || arg.equals("--credentials")) {
                        me.credentials = arglist.remove();
                    } else if (arg.equalsIgnoreCase("-a")
                            || arg.equals("--access")) {
                        if (dbg > 6) {
                            System.out.print(myID + " got access list:");
                        }
                        // me.userAccess = new HashSet<String>();
                        while ((tmp = arglist.peek()) != null
                                && !tmp.startsWith("-")) {
                            if (dbg > 6) {
                                System.out.print(" " + tmp);
                            }
                            me.userAccess.add(arglist.remove());
                        }
                        if (dbg > 6) {
                            System.out.println();
                        }
                    } else if (arg.equalsIgnoreCase("-o")
                            || arg.equals("--onlyonhosts")) {
                        if (dbg > 6) {
                            System.out.print(myID + " got host restrictions:");
                        }
                        // me.onlyonhosts = new HashSet<String>();
                        while ((tmp = arglist.peek()) != null
                                && !tmp.startsWith("-")) {
                            if (dbg > 6) {
                                System.out.print(" " + tmp);
                            }
                            me.onlyonhosts.add(arglist.remove());
                        }
                        if (dbg > 6) {
                            System.out.println();
                        }
                    } else if (arg.equals("--devices")) {
                        if (dbg > 6) {
                            System.out.print(myID + " got device list:");
                        }
                        // me.requiredDevices = new HashSet<String>();
                        while ((tmp = arglist.peek()) != null
                                && !tmp.startsWith("-")) {
                            if (dbg > 6) {
                                System.out.print(" " + tmp);
                            }
                            me.requiredDevices.add(arglist.remove());
                        }
                        if (dbg > 6) {
                            System.out.println();
                        }
                    } else if (arg.equals("--groups")) {
                        if (dbg > 6) {
                            System.out.print(myID + " got group list:");
                        }
                        // me.groups = new HashSet<String>();
                        while ((tmp = arglist.peek()) != null
                                && !tmp.startsWith("-")) {
                            if (dbg > 6) {
                                System.out.print(" " + tmp);
                            }
                            me.groups.add(arglist.remove());
                        }
                        if (dbg > 6) {
                            System.out.println();
                        }
                    } else if (arg.equals("--connecttogroups")) {
                        if (dbg > 6) {
                            System.out.print(myID + " got connecttogroup list:");
                        }
                        // me.groups = new HashSet<String>();
                        while ((tmp = arglist.peek()) != null
                                && !tmp.startsWith("-")) {
                            if (dbg > 6) {
                                System.out.print(" " + tmp);
                            }
                            me.connecttogroups.add(arglist.remove());
                        }
                        if (dbg > 6) {
                            System.out.println();
                        }
                    } else if (arg.equalsIgnoreCase("-g")
                            || arg.equalsIgnoreCase("--gui")) {
                        while ((tmp = arglist.peek()) != null
                                && (tmp.equals("-?") || (!tmp.startsWith("-")))) {
                            me.guiParticularVisualizations.add(arglist.remove());
                        }
                        me.guiRequested = true;
                    } else if (arg.equalsIgnoreCase("--persistentGUI")) {
                        me.persistentGUI = true;
                    } else if (arg.equalsIgnoreCase("--logging")) {
                        me.withlocallogging = true; // turn on local logging
                    } else if (arg.equalsIgnoreCase("--calllogging")) {
                        me.withcalllogging = true; // turn on call logging
                    } else if (arg.equalsIgnoreCase("--playback")) {
                        try {
                            this.openPlaybackLogFile(this, arglist.remove());
                            this.setUpdateModeLive(this, false);
                        } catch (Exception e) {
                            System.out.println("Could not open specified log file:  "
                                    + e);
                            System.exit(ADEGlobals.ExitCode.ARG_PARSE.code());
                        }
                    } else if (arg.equalsIgnoreCase("--ctrlc")) {
                        System.out.println("WARNING: ALLOWING KILL FOR CTRL-C");
                        me.killForCtrlc = true;
                    } else {
                        if (dbg > 6) {
                            System.out.print(" (add) ");
                        }
                        needaddargs = true;
                        addargs.add(arg);
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Error parsing arguments  (problem arg = \""
                    + arg + "\"\n");
            usage();
            System.exit(ADEGlobals.ExitCode.ARG_PARSE.code());
        }
        // just want to see what the system properties are...
        // System.out.println("\n"+ prg +": PROPERTIES OF THIS JVM ARE:");
        // Properties props = System.getProperties();
        // props.list(System.out);
        // System.out.println();

        // the classpath can contain duplicates, especially if a component
        // configuration
        // is saved and then run and then re-saved, etc. However, the best
        // place to clean this up is in the constructor, where additional
        // class-path
        // modifications (like stripping the startdirectory prefix takes place).
        // so for now, just note note the System classpath and move on
        me.userclasspath = System.getProperty("java.class.path");

        if (me.onlyonhosts == null) {
            me.onlyonhosts = new HashSet<String>();
        }
        if (me.requiredDevices == null) {
            me.requiredDevices = new HashSet<String>();
        }
        // TODO: make userAccess consistent
        // if (me.userAccess == null) me.userAccess = new HashSet<String>();
        if (dbg > 5) {
            System.out.println();
        }

        if (needaddargs) {
            if (dbg > 4) {
                System.out.println(prg + ": parsing additional arguments...");
            }
            // turn the arglist back into a String array
            String[] addargsarr = addargs.toArray(new String[0]);
            if (parseadditionalargs(addargsarr)) {
                for (String anarg : addargs) {
                    if (dbg > 4) {
                        System.out.println(prg + ": adding additionalargs "
                                + anarg);
                    }
                    me.addAdditionalarg(anarg);
                }
            } else {
                System.out.print("Unknown startup argument \"" + arg
                        + "\" encountered; ");
                usage();
                System.exit(ADEGlobals.ExitCode.ARG_PARSE.code());
            }
            if (dbg > 4) {
                System.out.println(prg + ": additionalargs is ["
                        + me.additionalargs + "]");
            }
        } else {
            me.addAdditionalarg("");
        }

        // check if the current host

        if (redirectLogsAtTheEnd) {
            redirectLogs();
        }
        return me;
    }

    /**
     * Starts an ADEComponent
     *
     * @param args command-line arguments which are ignored
     * @throws Exception if the component cannot be instantiated
     */
    final public static void main(String[] args) throws Exception {
        entryright = true;
        // CurrentClassGetter ccg = new CurrentClassGetter();
        myargs = args;
        // String myType = ccg.getClassNameFirst();
        String myInterfaceName = (String) System.getProperties().get(
                "component");
        // check for component naming violation
        if (myInterfaceName == null) {
            System.err.println("Error: must specify which ADE component to run (-Dcomponent=...)");
            System.exit(ADEGlobals.ExitCode.COMPONENT_NAMING_VIOLATION.code());
        }
        // check for component interface violation: only one interface and
        // interface must be the same as the class name minus the "Impl" part at
        // the end
        String myImplName = myInterfaceName + ADEGlobals.componentimpl;
        Class myclass = null;
        try {
            myclass = Class.forName(myImplName);
        } catch (Exception e) {
            System.err.println("Class definition for component " + myImplName
                    + " not found, aborting.");
            System.exit(-1);
        }

        // skip checks for ADERegistry
        if (!myInterfaceName.equals(ADERegistry.class.getName())) {

            // Class[] interfaces = ccg.getFirstClass().getInterfaces();
            Class[] interfaces = myclass.getInterfaces();
            if (interfaces.length != 1) {
                System.err.println("Component must implement only one top-level interface.");
                System.exit(ADEGlobals.ExitCode.COMPONENT_NAMING_VIOLATION.code());
            }
            if (!(interfaces[0].getName() + ADEGlobals.componentimpl).equals(myclass.getName())) {
                System.err.println("The component name must be the component's interface name followed by "
                        + ADEGlobals.componentimpl);
                System.exit(ADEGlobals.ExitCode.COMPONENT_NAMING_VIOLATION.code());
            }
            // naming convention satisfied, now check the constructors, should
            // either have
            // no constructor (i.e., use the default constructor) or use a
            // constructor with no args
            // that calls super() in the first line
            try {
                Constructor[] constructors = myclass.getConstructors();
                if (constructors.length > 1) {
                    throw new SecurityException(
                            "Only one constructor with no arguments allowed.");
                }
                if (constructors.length == 1
                        && constructors[0].getParameterTypes().length > 0) {
                    throw new SecurityException(
                            "Constructor cannot have any arguments.");
                }
            } catch (SecurityException se) {
                System.err.println("Cannot instrospect on constructors of "
                        + myImplName);
                System.exit(ADEGlobals.ExitCode.CONSTRUCTOR_VIOLATION.code());
            }
        }
        // all checks are OK
        ADEComponentImpl asi;

        System.out.println(ADEGlobals.ADE_VERSION_STRING
                + " Copyright Matthias Scheutz (mscheutz@gmail.com)");
        // start the new class with the default constructor with no arguments
        try {
            // MS: REGLOCK removed
            // reglock = ADEGlobals.getADELock("jreglock");
            // asi = (ADEComponentImpl) ccg.getFirstClass().newInstance();
            asi = (ADEComponentImpl) myclass.newInstance();
            // check that the instance was with a parameterless constructor that
            // called super
            // if entryright is still true, the constructor in this class was
            // not called...
            if (entryright) {
                System.exit(ADEGlobals.ExitCode.CONSTRUCTOR_VIOLATION.code());
            }
            // otherwise the constructor was called, so finish initialization
            asi.finishedInitialization(asi);
            if (showMemory) {
                System.out.println("Heap size: " + Runtime.getRuntime().totalMemory());
            }
            System.out.println(asi.getID() + " running...\n");
        } catch (Exception e) {
            System.err.println("Could not instantiate class " + myImplName
                    + " due to:");
            e.printStackTrace();
        }
    }

    /**
     * *********************************************************************
     */
    /**
     * ************** METHODS FOR OUTPUT REDIRECTION TO GUI ****************
     */
    /**
     * *********************************************************************
     */
    /**
     * registers a SystemView GUI visualization as a consumer of a component's
     * output.
     *
     * @param credentials the Registry to which the SystemView GUI belongs.
     * @return a unique ID that identifies this particular consumer (that way,
     * can have multiple consumers, and know exactly how much information is yet
     * to be updated for the consumer)
     */
    @Override
    public UUID registerOutputRedirectionConsumer(Object credentials)
            throws RemoteException, IOException {
        if ((credentials != this) && (!(credentials instanceof ADERegistry))) {
            throw new AccessControlException(
                    "output redirection consumer registration "
                    + "failed based on " + credentials);
        }

        // begin by initializing output redirection, if isn't already.
        synchronized (isRedirectingOutput) {
            if (!isRedirectingOutput) {
                initializeOutputRedirection();
            }
        }

        UUID outputID = UUID.randomUUID();
        synchronized (redirectedOutputConsumers) {
            redirectedOutputConsumers.put(outputID, new StringBuilder());
        }
        return outputID;
    }

    private void initializeOutputRedirection() throws IOException {
        this.redirectedOutputConsumers = new HashMap<UUID, StringBuilder>();

        final PrintStream outWriter = System.out;
        final PrintStream errWriter = System.err;

        PipedInputStream pipedSysOutInputStream = new PipedInputStream();
        PipedOutputStream pipedSysOutOutputStream = new PipedOutputStream(
                pipedSysOutInputStream);

        PipedInputStream pipedSysErrInputStream = new PipedInputStream();
        PipedOutputStream pipedSysErrOutputStream = new PipedOutputStream(
                pipedSysErrInputStream);

        final BufferedReader sysOutReader = new BufferedReader(
                new InputStreamReader(pipedSysOutInputStream));
        final BufferedReader sysErrReader = new BufferedReader(
                new InputStreamReader(pipedSysErrInputStream));

        System.setOut(new PrintStream(pipedSysOutOutputStream));
        System.setErr(new PrintStream(pipedSysErrOutputStream));

        Thread outReaderThread = new Thread() {
            public void run() {
                String temp;
                try {
                    while ((temp = sysOutReader.readLine()) != null) {
                        outWriter.println(temp);
                        synchronized (redirectedOutputConsumers) {
                            for (StringBuilder eachConsumerBuffer : redirectedOutputConsumers.values()) {
                                eachConsumerBuffer.append(temp + "\n");
                            }
                        }
                    }
                } catch (IOException e) {
                    // don't worry about exception, will only occur when component
                    // is being shut down, and that's perfectly ok. The stream
                    // *is* over then.
                }
            }
        ;
        };
		outReaderThread.start();

        Thread errReaderThread = new Thread() {
            public void run() {
                String temp;
                try {
                    while ((temp = sysErrReader.readLine()) != null) {
                        errWriter.println(temp);
                        synchronized (redirectedOutputConsumers) {
                            for (StringBuilder eachConsumerBuffer :
                                    redirectedOutputConsumers.values()) {
                                eachConsumerBuffer.append(temp + "\n");
                            }
                        }
                    }
                } catch (IOException e) {
                    // don't worry about exception, will only occur when component
                    // is being shut down, and that's perfectly ok. The stream
                    // *is* over then.
                }
            }
        ;
        };
		errReaderThread.start();

        isRedirectingOutput = true; // having finished this method, the component
        // is not
        // set up to redirect output.
    }

    /**
     * de-registers a SystemView GUI visualization as a consumer of a component's
     * output.
     *
     * @param consumerID : the UUID that the consumer was assigned by a former
     * call to registerOutputRedirectionConsumer()
     */
    @Override
    public void deregisterOutputRedirectionConsumer(UUID consumerID)
            throws RemoteException {
        synchronized (redirectedOutputConsumers) {
            redirectedOutputConsumers.remove(consumerID);
        }
    }

    /**
     * returns any output that has been accumulated for a particular consumer
     * since its last call to this method.
     *
     * @param consumerID : the UUID that the consumer was assigned by a former
     * call to registerOutputRedirectionConsumer()
     */
    @Override
    public String getAccumulatedRedirectedOutput(UUID consumerID)
            throws RemoteException {
        synchronized (redirectedOutputConsumers) {
            StringBuilder consumerBuffer = redirectedOutputConsumers.get(consumerID);
            String result = consumerBuffer.toString();
            consumerBuffer.setLength(0); // clear the buffer, now that have
            // fetched the
            // up-to-date result.
            return result;
        }
    }
}
