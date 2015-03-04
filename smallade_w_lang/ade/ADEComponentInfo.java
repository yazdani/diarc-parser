/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@gmail.com
 * Last update: May 2010
 *
 * ADEComponentInfo.java
 */
package ade;

import java.io.*;
import java.security.*;
import java.text.ParseException;
import java.util.*;

/**
 * The various data of an {@link ade.ADEComponentImpl ADEComponentImpl}.
 *
 */
final public class ADEComponentInfo implements Serializable, Cloneable {
    // for debugging

    final static String prg = "ADEComponentInfo";
    final static boolean debug = false;
    // included so we can later read files even if class might have changed...
    private static final long serialVersionUID = 7526472295622776147L;
    private boolean withComponentStub = true;  // NOTE: needs to be true in general,
    // else serialization won't work
    // instance variables--although select variables are public, most are only
    // accessible either via accessor methods or classes in the ade package
    public String host;                // host IP/name on which component is running
    public int port = -1;              // port on which the component is listening
    public String type;                // type (category) of component
    public String name;                // the name of the component
    public HashSet<String> groups;     // the (component) groups this component belongs to
    public HashSet<String> connecttogroups; // the groups this component will connect to unless explicitly specified
    String username;                   // user name for which component is running
    String registryIP;                 // registry IP/name used by this host
    String registryType;               // type of the registry
    String registryName;               // name of the registry used by this host
    int registryPort = -1;             // port of the registry used by this host
    HashSet<String> userAccess;        // allowed users with their access rights
    HashSet<String> onlyonhosts;       // restrict to hosts (empty=unrestricted)
    HashSet<String> requiredDevices;   // devices host must have for this component
    HashSet<String> requiredClients;   // client types this component needs to work properly
    int connectsAllowed;               // maximum # of client connections
    int connectsCurrent = 0;           // current # of client connections
    int heartBeatPeriod;               // period of heartbeat (in ms)
    long lastUpdate;                   // most recent time heartbeat checked in
    int recoveryMultiplier = 1;        // # of reaper periods before recovery
    ADEGlobals.ComponentState state;      // last self-reported state of the component
    ADEGlobals.RecoveryState recState; // component state from external perspective
    String startdirectory;             // directory in which to start the component
    String javavmargs;                 // the java virtual machine arguments
    String userclasspath;              // the additional classpath for the component
    String additionalargs;             // additional command line arguments
    ADEComponent as;                   // a reference to the component
    int numrestarts = 0;               // attempt to restart the component
    String configfile;                 // file name of a config file for component
    boolean isregistry = false;        // whether this component is a registry
    Object credentials;                // the credentials of this component
    boolean ignoreconfig = false;      // don't do configfile (eg, on restart)
    int dbg = 0;                       // debugging level
    boolean maintainState = false;     // whether to maintain state
    ADEState adestate = null;          // the state object
    boolean killForCtrlc = false;      // halt uncleanly
    boolean withlocallogging = false;  // whether to use local logging
    boolean withcalllogging = false;   // whether to use call logging
    public boolean guiRequested = false; // whether --GUI was requested on the commandline
    boolean persistentGUI = false;     // whether --GUI-requested windows can be closed 
    //                                         persistent = ignore close operations.
    public HashSet<String> guiParticularVisualizations = new HashSet<String>(); // particular visualizations
    //    requested on the commandline (e.g., --GUI Environment 3D)
    boolean shutdownbyotherregistries = false; // whether this component can be shut down by other registries
    HashSet<String> clients = new HashSet<String>(); // list of connections to this component
    HashSet<String> components = new HashSet<String>(); // list of connections to other components
    HashSet<String> interfaces;        // set of interfaces implemented

    /** Constructor that returns a blank structure to be filled in manually.
     * Note that the default values are <b>not</b> set, particularly due to
     * the fact that the <tt>registryName</tt> field controls whether an
     * {@link ade.ADERegistry ADERegistry} will attempt registration. */
    public ADEComponentInfo() {
        this(false);
    }

    public ADEComponentInfo(boolean isregistry) {
        setDefaults(isregistry);
    }

    /** Set the defaults for non-registry components. */
    void setDefaults() {
        setDefaults(false);
    }

    // TODO: finish this...
    /** Sets many variables (except type, name, and startup
     * and execution variables) to default values. Note that if the component
     * is an {@link ade.ADERegistry ADERegistry}, the registry-related fields
     * are not filled in.
     * @param isReg <tt>true</tt> if the component is an {@link ade.ADERegistry
     * ADERegistry}, <tt>false</tt> otherwise. */
    void setDefaults(boolean isReg) {
        host = ADEPreferences.HOSTIP;
        port = ADEGlobals.DEF_COMPONENTPORT;
        state = ADEGlobals.ComponentState.INIT;
        recState = ADEGlobals.RecoveryState.OK;
        username = System.getProperty("user.name");
        if (!isReg) {
            registryIP = ADEGlobals.DEF_REGIP;
            registryType = ADEGlobals.DEF_REGTYPE;
            registryName = ADEGlobals.DEF_REGNAME;
            registryPort = ADEGlobals.DEF_REGPORT;
        } else {
            isregistry = true;
        }
        // if a registry, make this "infinite"?
        connectsAllowed = ADEGlobals.DEF_MAXCONN;
        heartBeatPeriod = ADEGlobals.DEF_HBPULSE;
        startdirectory = System.getProperty("user.dir");
        numrestarts = ADEGlobals.DEF_RESTARTS;
        onlyonhosts = new HashSet<String>();
        // default to all access; can be overwritten if necessary
        userAccess = new HashSet<String>();
        userAccess.add(ADEGlobals.ALL_ACCESS);
        requiredDevices = new HashSet<String>();
        requiredClients = new HashSet<String>();
        interfaces = new HashSet<String>();
        groups = new HashSet<String>();
        connecttogroups = new HashSet<String>();
    }

    /** Sets the host IP for this component */
    public void setHostInfo(ADEHostInfo hi) {
        //hostinfo = hi;
        host = hi.hostip;
    }

    /** clones this component info object */
    public ADEComponentInfo duplicate() {
        try {
            return (ADEComponentInfo) this.clone();
        } catch (CloneNotSupportedException e) {
            System.err.println("Problem cloning of ADEComponentInfo: " + e + ", not cloned.");
            return null;
        }
    }

    /** Create an {@link ade.ADEMiniComponentInfo ADEMiniComponentInfo} object
     * (for minimal data transfer) with duplicated information.
     * @return An {@link ade.ADEMiniComponentInfo ADEMiniComponentInfo} object */
    public ADEMiniComponentInfo getADEMiniComponentInfo() {
        return new ADEMiniComponentInfo(getKey(), userAccess, onlyonhosts,
                recoveryMultiplier, state, recState, clients, components);
    }

    /** Update this component info with the information from an {@link
     * ade.ADEMiniComponentInfo ADEMiniComponentInfo} object.
     * @param amsi {@link ade.ADEMiniComponentInfo ADEMiniComponentInfo} object */
    public void update(ADEMiniComponentInfo amsi) {
        // need to check the id?
        //System.out.print(prg +" updating "+ amsi.toString());
        userAccess.retainAll(amsi.userAccess);
        onlyonhosts.retainAll(amsi.onlyonhosts);
        recoveryMultiplier = amsi.recoveryMultiplier;
        state = amsi.state;
        recState = amsi.recState;
        clients.retainAll(amsi.clients);
        components.retainAll(amsi.components);
    }

    /** Returns a String in Java's RMI format displaying the host, port,
     * component type, and component name. */
    @Override
    public String toString() {
        return (host + ":" + port + "/" + type + "$" + name);
    }

    /** Outputs the String produced by {@link #toPrintString} to
     * <tt>System.out</tt>. */
    public void print() {
        System.out.println(toPrintString());
    }

    /** A formatted String (with line breaks) of this component's information. */
    public String toPrintString() {
        StringBuilder sb = new StringBuilder(512); // 1/2K good to start?
        sb.append("COMPONENT INFO:\n");
        sb.append("==============================\n");
        sb.append("Name:      ");
        sb.append(name);            // name of component
        sb.append("\nType:      ");
        sb.append(type);            // type (category) of component
        sb.append("\nGroups:  ");
        if (!groups.isEmpty()) {
            for (String group : groups) {
                sb.append("\n           " + group);
            }
        } else {
            sb.append("<none>");
        }
        sb.append("\nConnect to groups:  ");
        if (!connecttogroups.isEmpty()) {
            for (String group : connecttogroups) {
                sb.append("\n           " + group);
            }
        } else {
            sb.append("<none>");
        }
        sb.append("\nRun State: ");
        sb.append(state);           // execution state of component
        sb.append("\nRec State: ");
        sb.append(recState);        // recovery state of component
        sb.append("\nRequests:  ");
        if (interfaces.size() > 0) {
            for (String iface : interfaces) {
                sb.append("\n           ");
                sb.append(iface);
            }
        } else {
            sb.append("\n           <none>");
        }
        sb.append("\nOnly on:   ");
        sb.append(onlyonhosts);     // host restrictions
        sb.append("\nRequired:  ");
        sb.append(requiredDevices); // list of required devices host must supply
        sb.append("\n");
        sb.append(requiredClients); // list of required clients this component needs
        sb.append("\nHost:      ");
        sb.append(host);            // the current host
        sb.append("\nPort:      ");
        sb.append(port);            // the port
        sb.append("\nDirectory: ");
        sb.append(startdirectory);  // the directory in which to start the component
        sb.append("\nUser CP:   ");
        sb.append(userclasspath);   //
        sb.append("\nJVM Args:  ");
        sb.append(javavmargs);      //
        sb.append("\nAdd. Args: ");
        sb.append(additionalargs);  // the additional command line arguments
        sb.append("\nRestarts:  ");
        sb.append(numrestarts);     // attempt to restart the component
        sb.append("\nFile:      ");
        sb.append(configfile);      // the file name of a config file for that component
        sb.append("\nAccess:    ");
        sb.append(userAccess);      // access list (by user)
        sb.append("\nAllowed:   ");
        sb.append(connectsAllowed); // maximum $ of client connections
        sb.append("\nCurrent:   ");
        sb.append(connectsCurrent); // current $ of client connections
        sb.append("\nHBPeriod:  ");
        sb.append(heartBeatPeriod); // period of heartbeat (in ms)
        sb.append("\nUpdated:   ");
        sb.append(lastUpdate);      // most recent time heartbeat checked in
        sb.append("\nRegistry?  ");
        sb.append(isregistry);      // whether this component is a registry
        sb.append("\nReg. Name: ");
        sb.append(registryName);    // the registry
        sb.append("\nCredent.   ");
        sb.append(credentials);     // credentials
        sb.append("\nProviding services to: "); // the components we gave refs to
        if (clients.size() > 0) {
            for (String cl : clients) {
                sb.append("\n           " + cl);
            }
        } else {
            sb.append("<none>");
        }
        sb.append("\nUsing services from:"); // the components we have references to
        if (components.size() > 0) {
            for (String srv : components) {
                sb.append("\n           " + srv);
            }
        } else {
            sb.append("<none>");
        }
        sb.append("\n");
        //System.out.println("ADESer-Ref:" + as);              // a reference to the component
        return sb.toString();
    }

    /** Confirms that the user signified by an {@link ade.ADEUser ADEUser}
     * object has permission to access this component.
     * @param u An <tt>ADEUser</tt> object
     * @return <tt>true</tt> if the user has permission, else <tt>false</tt> */
    public boolean hasRightsTo(ADEUser u) {
        for (String access : u.getAllowances()) {
            if ((access.indexOf(name) != -1 && access.indexOf(type) != -1) || access.equals("any")) {
                return true;
            }
        }
        return false;
    }

    /** Returns the value of the {@link #host} field. */
    public String getHostName() {
        return host;
    }

    /** Returns the value of the {@link #host} field. */
    public HashSet<String> getGroups() {
	// for(String s: groups) System.out.println("IN GROUP: " + s);
        return groups;
    }

    /** Returns the value of the {@link #host} field. */
    public HashSet<String> getConnectToGroups() {
	// for(String s: groups) System.out.println("CONNECT TO GROUP: " + s);
        return connecttogroups;
    }

    //public ADEHostInfo getHostInfo() {
    //	return hostinfo;
    //}
    /** Returns the value of the {@link #port} field. */
    public int getPort() {
        return port;
    }

    /** Returns the value of the {@link #type} field. */
    public String getType() {
        return type;
    }

    /** Returns the value of the {@link #name} field. */
    public String getName() {
        return name;
    }

    /** Returns the value used to identify an <tt>ADEComponent</tt> (a String
     * comprised of the {@link #type} and {@link #name} fields in the form
     * <tt>type$name</tt>). */
    public String getKey() {
        return type + "$" + name;
    }

    /** Returns the value of the {@link #heartBeatPeriod} field. Note that,
     * for security purposes, only the <tt>ADEComponent</tt> associated with the
     * instance of this class (i.e., the parameter must be the same as the
     * object stored in the <tt>as</tt> field) is permitted access.
     * @param s The <tt>ADEComponent</tt> associated with this information
     * @return The length of one heartbeat period (in ms)
     * @throws AccessControlException If called with a non-equal
     * <tt>ADEComponent</tt> object */
    public int getHeartBeatPeriod(ADEComponent s) throws AccessControlException {
        if (!s.equals(as)) {
            throw new AccessControlException("Access to " + getKey() + " denied");
        }
        return heartBeatPeriod;
    }

    /** Returns the component's heartbeat period */
    int getHeartBeatPeriod() {
        return heartBeatPeriod;
    }

    /** Returns the value of the {@link #configfile} field. Note that,
     * for security purposes, only the <tt>ADEComponent</tt> associated with the
     * instance of this class (i.e., the parameter must be the same as the
     * object stored in the <tt>as</tt> field) is permitted access.
     * @param s The <tt>ADEComponent</tt> associated with this information
     * @return The name of the configuration file
     * @throws AccessControlException If called with a non-equal
     *   <tt>ADEComponent</tt> object */
    public String getConfigfile(ADEComponent s) throws AccessControlException {
        if (!s.equals(as)) {
            throw new AccessControlException("Access to " + getKey() + " denied");
        }
        return configfile;
    }

    /** Returns the value of the {@link #heartBeatPeriod} field; accessible
     * by classes in the <tt>ade</tt> package. */
    // don't need this, as it's controlled by field declaration
    //int getHeartBeatPeriod() { return heartBeatPeriod; }
    /** Returns the value of the {@link #interfaces} field; accessible
     * by classes in the <tt>ade</tt> package. */
    HashSet<String> getInterfaces() {
        return interfaces;
    }

    /** Returns the value of the {@link #interfaces} field; accessible
     * from classes in the <tt>ade</tt> package. */
    // don't need this, as it's controlled by field declaration
    int getConnectsCurrent() {
        return connectsCurrent;
    }

    /** Returns the value of the {@link #interfaces} field; accessible
     * by classes in the <tt>ade</tt> package. */
    int getConnectsAllowed() {
        return connectsAllowed;
    }

    /** Returns a <tt>true</tt> or <tt>false</tt> value as to whether the
     * component is accepting connections; accessible by classes in the
     * <tt>ade</tt> package. */
    boolean allowConnect() {
        //System.out.println(prg +": current="+ connectsCurrent +"; allowed="+ connectsAllowed);
        return connectsCurrent < connectsAllowed;
    }

    /** Returns the value of the {@link #clients} field; accessible
     * by classes in the <tt>ade</tt> package. */
    HashSet<String> getClients() {
        return clients;
    }

    /** Bookkeeping for adding a <i>client</i> connection (i.e., an
     * <tt>ADEComponent</tt> that has obtained a remote reference); accessible
     * by classes in the <tt>ade</tt> package. */
    void addClient(String uid) {
        clients.add(uid);
        connectsCurrent++;
    }

    /** Bookkeeping for removing a <i>client</i> connection (i.e., an
     * <tt>ADEComponent</tt> that has obtained a remote reference); accessible
     * by classes in the <tt>ade</tt> package. */
    void removeClient(String uid) {
        clients.remove(uid);
        connectsCurrent--;
    }

    /** Returns the list of components that this component has acquired a connection to */
    HashSet<String> getComponents() {
        return components;
    }

    /** Adds a component to the list of components this client has connections to */
    void addComponent(String uid) {
        components.add(uid);
    }

    /** Remove a component from the list of components this client has connections to */
    void removeComponent(String uid) {
        components.remove(uid);
    }

    /** Removes those clients from the client set that are not in cls */
    void retainClients(HashSet<String> cls) {
        clients.retainAll(cls);
    }

    /** Removes those components from the component set that are not in srvs */
    void retainComponents(HashSet<String> srvs) {
        // TODO: set the registry's component list when a connection is made;
        // for the moment, we're relying solely on the heartbeat
        for (String srv : srvs) {
            if (!components.contains(srv)) {
                components.add(srv);
            }
        }
    }

    /** Checks if user uid has access to this component */
    boolean userAccess(String uid) {
        if (userAccess.contains(ADEGlobals.ALL_ACCESS)) {
            return true;
        }
        else if (userAccess.contains(uid)) {
            return true;
        }
        return false;
    }


    void addAdditionalarg(String arg) {
        if (debug) {
            System.out.println(prg + ": adding additionalargs " + arg);
        }
        if (additionalargs == null) {
            additionalargs = new String("");
        }
        additionalargs = additionalargs + " " + arg;
    }

    /** adds a new required device to the device list */
    void addRequiredDevice(String dev) {
        synchronized (requiredDevices) {
            requiredDevices.add(dev);
        }
    }

    /** Removes the device from the required device list */
    void removeRequiredDevice(String dev) {
        synchronized (requiredDevices) {
            if (requiredDevices.contains(dev)) {
                requiredDevices.remove(dev);
            }
        }
    }

    boolean hasRequiredDevices() {
        return (requiredDevices.size() > 0);
    }

    String[] getRequiredDevices() {
        return requiredDevices.toArray(new String[1]);
    }

    String getRequiredDeviceList() {
        return ADEGlobals.setToStr(requiredDevices);
    }

    void addRequiredClient(String cl) {
        synchronized (requiredClients) {
            requiredClients.add(cl);
        }
    }

    void removeRequiredClient(String cl) {
        synchronized (requiredClients) {
            if (requiredClients.contains(cl)) {
                requiredClients.remove(cl);
            }
        }
    }

    boolean hasRequiredClient() {
        return (requiredClients.size() > 0);
    }

    String[] getRequiredClients() {
        return requiredClients.toArray(new String[1]);
    }

    String getRequiredClientList() {
        return ADEGlobals.setToStr(requiredClients);
    }

    void addAllowedHost(String ip) {
        synchronized (onlyonhosts) {
            onlyonhosts.add(ip);
        }
    }

    void removeAllowedHost(String ip) {
        synchronized (onlyonhosts) {
            if (onlyonhosts.contains(ip)) {
                onlyonhosts.remove(ip);
            }
        }
    }

    void allowOnAnyHost() {
        synchronized (onlyonhosts) {
            onlyonhosts.clear();
        }
    }

    boolean allowedOnHost(String ip) {
        if (onlyonhosts.size() > 0) {
            return onlyonhosts.contains(ip);
        }
        return true;
    }

    String getAllowedHosts() {
        return ADEGlobals.setToStr(onlyonhosts);
    }

    /** Sets a new host name. */
    void setHostName(String hostip) {
        host = hostip;
    }

    String getAccessList() {
        return ADEGlobals.setToStr(userAccess);
    }

    boolean userAccess(ADEUser u) {
        if (userAccess.contains(ADEGlobals.ALL_ACCESS)) {
            return true;
        }
        for (String allowed : u.getAllowances()) {
            /*
            String allowed;
            Iterator<String> it = u.getUserAccess();
            while(it.hasNext()) {
            allowed = (String)it.next();
             */
            if (userAccess.contains(allowed)) {
                return true;
            }
        }
        return false;
    }

    /** Forms the rmi string for this component.
     * @return A string of the form <tt>rmi://IPaddress:port/type$name</tt> */
    public String rmiString() {
        return ("rmi://" + toString());
    }

    void setHeartBeatPeriod(int hb) {
        heartBeatPeriod = hb;
        //as.setHeart
    }

    void setConnectsCurrent(int n) {
        connectsCurrent = (n);
        lastUpdate = System.currentTimeMillis();
        //System.out.println(prg +": set time for "+ name +" to "+ lastUpdate);
    }

    void setState(ADEGlobals.ComponentState s) {
        state = s;
    }

    void setRecoveryState(ADEGlobals.RecoveryState s) {
        recState = s;
    }

    /** Reads a component configuration from a BufferedReader (until it encounters
     * the token "ENDCOMPONENT" on a new line).
     * <p>
     * The format of a component configuration entry is:<br>
     * <tt>STARTCOMPONENT<br>
     * host             IP address or hostname on which to start this component<br>
     * port             the RMI port (default=1099)<br>
     * type             component class (e.g., com.test.TestComponentImpl)<br>
     * name             assigned name (optional)<br>
     * groups           the list of groups this component belongs to<br>
     * connecttogroups  the list of groups this component will connect to if no groups are specified<br>
     * conn             number of connections supported (default=10)<br>
     * tmout            reaper period<br>
     * acc              access rights<br>
     * startdirectory   fully qualified ADE home directory<br>
     * userclasspath    command-line execution statement<br>
     * restarts         number of recovery attempts (default=0)<br>
     * configfile       configuration file (optional)<br>
     * onlyonhosts      list of hosts this component can/is allowed to run on<br>
     * devices          list of devices host must supply<br>
     * credentials      additional permissions<br>
     * ENDCOMPONENT<br></tt>
     * <p>
     * A configuration file passed to an {@link ade.ADERegistry ADERegistry}
     * may also contain lines that specify other configuration files. The
     * format is:<br>
     * <tt>LOADCOMPONENTCONFIGS file1 [file2...]<tt><br>
     * @param br The {@link java.io.BufferedReader BufferedReader}
     * @param line The current line
     * @return The line number of the last parsed line, which is expected to have been "STARTCOMPONENT" */
    int parseComponent(BufferedReader br, int line) throws ParseException {
        boolean waitcomponent = false;
        boolean gothost = false;
        boolean isWindows = ADEGlobals.isWindows();

        if (debug) {
            System.out.println(prg + ": in parseComponent...");
        }
        while (true) {
            try {
                br.mark(1024);  // 1K should be more than enough...
                String str = br.readLine().trim();
                line++;
                //		if (str != null && str.length() > 0) {
                if (str != null) {
                    StringTokenizer s = new StringTokenizer(str);
                    while (s.hasMoreTokens()) {
                        String data = s.nextToken();
                        if (debug) {
                            System.out.println("\t" + prg + " token [" + data + "]; total line = [" + str + "]");
                        }
                        if (str.startsWith("STARTCOMPONENT")) {
                            throw new ParseException("Encountered another STARTCOMPONENT!", line);
                        } else if (data.startsWith("#")) {
                            // comment, skip the rest of the line
                            break;
                        } else if (data.equals("host")) {
                            gothost = true;
                            host = s.nextToken();
                        } else if (data.equals("port")) {
                            port = Integer.parseInt(s.nextToken());
                        } else if (data.equals("type")) {
			    type = s.nextToken();
                        } else if (data.equals("name")) {
                            name = s.nextToken();
                        } else if (data.equals("groups")) {
                            while (s.hasMoreTokens()) {
                                groups.add(s.nextToken());
                            }
                        } else if (data.equals("connecttogroups")) {
                            while (s.hasMoreTokens()) {
                                connecttogroups.add(s.nextToken());
                            }
                        } else if (data.equals("conn")) {
                            connectsAllowed = Integer.parseInt(s.nextToken());
                        } else if (data.equals("tmout")) {
                            heartBeatPeriod = Integer.parseInt(s.nextToken());
                        } else if (data.equals("acc")) {
                            while (s.hasMoreTokens()) {
                                userAccess.add(s.nextToken());
                            }
                        } else if (data.equals("startdirectory")) {
                            startdirectory = s.nextToken();
                            if (isWindows) {
                                while (s.hasMoreTokens()) {
                                    startdirectory += " " + s.nextToken();
                                }
                            }
                        } else if (data.equals("componentargs")) {
                            // allow empty componentargs, e.g., don't require that there be a token, per se.
                        	//     that way, can read in auto-generated config.
                        	additionalargs = combineTokensSpaceSeparated(s);
                        } else if (data.equals("userclasspath")) {
                        	// allow empty userclasspath, e.g., don't require that there be a token, per se.
                        	//     that way, can read in auto-generated config.
                        	//     BTW:  the classpath carries ":"s, but it might carry spaces too.
                        	//     hence want to combine it all into one string.
                        	userclasspath = combineTokensSpaceSeparated(s);
                        } else if (data.equals("javavmargs")) {
                        	// allow empty javavmargs, e.g., don't require that there be a token, per se.
                        	//     that way, can read in auto-generated config.
                        	javavmargs = combineTokensSpaceSeparated(s);
                        } else if (data.equals("restarts")) {
                            numrestarts = Integer.parseInt(s.nextToken());
                        } else if (data.equals("configfile")) {
                        	if (s.hasMoreTokens()) {
	                            configfile = s.nextToken();
	                            if (isWindows) {
	                                while (s.hasMoreTokens()) {
	                                    configfile += " " + s.nextToken();
	                                }
	                            }
                        	}
                        } else if (data.equals("credentials")) {
                        	if (s.hasMoreTokens()) {
                        		credentials = s.nextToken();
                        	}
                        } else if (data.equals("onlyonhosts")) {
                            while (s.hasMoreTokens()) {
                                onlyonhosts.add(s.nextToken());
                            }
                        } else if (data.equals("devices")) {
                            while (s.hasMoreTokens()) {
                                requiredDevices.add(s.nextToken());
                            }
                        } else if (data.equals("debug")) {
                            dbg = Integer.parseInt(s.nextToken());
                        } else if (data.equals("ctrlc")) {
                            killForCtrlc = true;
                        } else if (data.equals("pause")) {
                            try {
                                int p = Integer.parseInt(s.nextToken());
                                Thread.sleep(p);
                            } catch (NumberFormatException nfe) {
                                System.err.println(prg + ": Parsing format exception");
                            }
                        } else if (data.equals("ENDCOMPONENT")) {
                            /* TODO: Just a thought - if we're not given a host, we can
                             * choose one; as a matter of fact, we can fill in a bunch of
                             * the above data from a chosen host...
                            ADEComponentInfo si = new ADEComponentInfo(host, onlyonhosts, port, type, name, conn, tmout, acc, devs, startcommand, startdirectory, null, numrestarts, configfile, credentials, false);
                            ArrayList<ADEHostInfo> hostlist;
                            if (!gothost) {
                            // we weren't given a host, so choose one from known hosts
                            hostlist = pickStartHosts(si);
                            }
                             */

                            // do a little guessing if it's needed
                            // TODO: prompt if ADEPreferences.ADEHOME is different than
                            // user.dir; need to confirm which to use...
                            if (startdirectory == null) {
                                // try to fill in from ADEPreferences
                                if (debug) {
                                    System.out.println("Start directory for component unspecified; trying setting from ADEPreferences");
                                }
                                StringBuilder sb = new StringBuilder();
                                String homedir = ADEPreferences.ADEHOME;
                                String sep = System.getProperty("file.separator");
                                if (!homedir.startsWith(sep)) {
                                    // relative path; try with user.home on front
                                    sb.append(System.getProperty("user.home"));
                                    sb.append(sep);
                                }
                                sb.append(homedir);
                                startdirectory = sb.toString();
                                if (debug) {
                                    System.out.println(prg + ": set startdirectory=" + startdirectory);
                                }
                            }
                            if (userAccess.size() == 0) {
                                userAccess.add(ADEGlobals.ALL_ACCESS);
                            }
                            if (debug) {
                                System.out.println(prg + ": leaving parseComponent...");
                                print();
                            }
                            return line;
                        }
                    } // end while (s.hasMoreTokens())
                } else {
                    throw new java.text.ParseException("Missing ENDCOMPONENT", line);
                }// end if (str != null)
            } catch (Exception e) {
                throw new java.text.ParseException("Could not parse configuration file due to: " + e, line);
            }
        } // end while (true)
    }

    private String combineTokensSpaceSeparated(StringTokenizer s) {
    	if (s.hasMoreTokens()) {
    		String tmp = "";
	    	while (s.hasMoreTokens()) {
	    		tmp += " " + s.nextToken();
	        }
	    	return tmp.trim(); // trim away extra space
    	} else {
    		return null;
    	}
	}

	/** Utility method for writing out the config format. Declared static so
     * that config files may be written out by the {@link #createConfigFile}
     * method without declaring an object.
     * @param pw A {@link java.io.PrintWriter PrintWriter} for output
     * @param filename The name of the file (written in the header). */
    public static void writeConfigFormat(PrintWriter pw, String filename) {
    	pw.print("# ADE config file ");
        if (filename != null) {
        	pw.print(filename);
        }
        pw.println();
        pw.print("# Automatically generated by ");
        pw.print(prg);
        pw.print(" on ");
        pw.println(Calendar.getInstance().getTime().toString());
        pw.println("# Most of these items are optional and will be filled with");
        pw.println("# default values. At a minimum, the 'host', 'type', and");
        pw.println("# 'startdirectory' items should be listed.");
        pw.println("# ");
        pw.println("# Format and valid options:");
        pw.println("# STARTCOMPONENT");
        pw.println("# host            IP address or hostname");
        pw.println("# port            the RMI port (default=1099)");
        pw.println("# type            component class (e.g., com.test.TestComponent.  *NO* Impl)");
        pw.println("# name            assigned name (optional)");
        pw.println("# groups          the groups this component belongs to (optional)");
        pw.println("# connecttogroups the groups this component connects to (optional)");
        pw.println("# conn            number of connections supported (default=10)");
        pw.println("# tmout           reaper period");
        pw.println("# acc             access rights");
        pw.println("# startdirectory  fully qualified directory");
        pw.println("# userclasspath   component-specific classpath (colen or semi-colen delimited)");
        pw.println("# javavmargs      component-specific java vm arguments (with leading dashes)");
        pw.println("# componentargs      component-specific command-line arguments");
        pw.println("# restarts        number of recovery attempts (default=0)");
        pw.println("# onlyonhosts     IPs on which component may execute (space delimited)");
        pw.println("# devices         devices required (space delimited; host must supply listed devices)");
        pw.println("# configfile      configuration file (optional)");
        pw.println("# credentials     additional permissions");
        pw.println("# debug           debugging level");
        pw.println("# ENDCOMPONENT");
        pw.println("# You may also add \"pause\" commands between components.  \"pause 1000\" = 1 second.");
        pw.println("# ");
        pw.flush();
    }

    
    /** Writes the component information of this object to a new file. The file
     * will be written to the <tt>cdir</tt> directory (expected to be the ADE
     * configuration file directory). If a file of the same name exists, an
     * attempt will be made to rename the file as <tt>name.#</tt>, where
     * <tt>#</tt> is cumulative count of files with that name.
     * @param cdir The ADE config directory
     * @param name The file name
     * @throws SecurityException
     * @throws IOException if an error occurs creating or writing to the file */
    public final String createConfigFile(String cdir, String name)
            throws SecurityException, IOException {
        // TODO: this should most likely either *not* have "public" in the
        // declaration (thereby making it accessible only to classes in the
        // ade package) or take an ADEComponent parameter (which then must be
        // equal to that stored in the "as" field) for access control
        StringBuilder path = new StringBuilder(cdir);
        String file, filesep = System.getProperty("file.separator");

        if (!cdir.endsWith(filesep)) {
            path.append(filesep);
        }
        // could print out a message about file being backed up
        file = ADEGlobals.backupFile(path.toString(), name);
        createConfigFile(cdir, name, true);
        return name;
    }

    /** Writes the component information of this object to a file. The file will
     * be written to the <tt>adeconfigs</tt> directory.
     * @param cdir The directory in which to place the file
     * @param name The file name
     * @param ow Whether to overwrite a file (if it exists)
     * @throws SecurityException
     * @throws IOException if an error occurs creating or writing to the file */
    public final void createConfigFile(String cdir, String name, boolean ow)
            throws SecurityException, IOException {
        // TODO: this should most likely either *not* have "public" in the
        // declaration (thereby making it accessible only to classes in the
        // ade package) or take an ADEComponent parameter (which then must be
        // equal to that stored in the "as" field) for access control
        StringBuilder path = new StringBuilder(cdir);
        String filesep = System.getProperty("file.separator");

        if (!cdir.endsWith(filesep)) {
            path.append(filesep);
        }
        ADEGlobals.checkDir(path.toString(), true, true);
        path.append(name);
        File file = new File(path.toString());
        if (ow && file.exists()) {
            file.delete();
            file.createNewFile();
        }
        PrintWriter pw = new PrintWriter(file);

        if (ow) {
            writeConfigFormat(pw, file.getName());
        }
        
        writeConfigStartComponentToEndComponentSection(pw);
        pw.flush();
    }
    
    /** Writes the component information of this object to a PrintWriter. */
    public void writeConfigStartComponentToEndComponentSection(PrintWriter pw) {
    	 // TODO: this should most likely either *not* have "public" in the
        // declaration (thereby making it accessible only to classes in the
        // ade package) or take an ADEComponent parameter (which then must be
        // equal to that stored in the "as" field) for access control
        trimAndPrintLine(pw, "#");
        trimAndPrintLine(pw, "STARTCOMPONENT");
        trimAndPrintLine(pw, "host " + host);
        trimAndPrintLine(pw, "port " + port);
        trimAndPrintLine(pw, "type " + type);
        trimAndPrintLine(pw, "name " + name);
        trimAndPrintLine(pw, "groups " + ADEGlobals.setToStr(groups));
        trimAndPrintLine(pw, "connecttogroups " + ADEGlobals.setToStr(connecttogroups));
        trimAndPrintLine(pw, "conn " + connectsAllowed);
        trimAndPrintLine(pw, "tmout " + heartBeatPeriod);
        trimAndPrintLine(pw, "acc " + ADEGlobals.setToStr(userAccess));
        trimAndPrintLine(pw, "startdirectory " + startdirectory);
        trimAndPrintLine(pw, "userclasspath " + userclasspath);
        trimAndPrintLine(pw, "javavmargs " + stringOrEmptyString(javavmargs));
        trimAndPrintLine(pw, "componentargs " + stringOrEmptyString(additionalargs));
        trimAndPrintLine(pw, "restarts " + numrestarts);
        trimAndPrintLine(pw, "onlyonhosts " + ADEGlobals.setToStr(onlyonhosts));
        trimAndPrintLine(pw, "devices " + ADEGlobals.setToStr(requiredDevices));
        trimAndPrintLine(pw, "configfile " + stringOrEmptyString(configfile));
        trimAndPrintLine(pw, "credentials " + stringOrEmptyString(credentials));
        trimAndPrintLine(pw, "debug " + dbg);
        trimAndPrintLine(pw, "ENDCOMPONENT");
        trimAndPrintLine(pw, "# ");
	}

    private void trimAndPrintLine(PrintWriter pw, String string) {
		pw.println(string.trim());
	}

	/** returns either the string or an empty string, but not "null" */
	private String stringOrEmptyString(Object toPrint) {
		if (toPrint == null) {
			return "";
		} else {
			return toPrint.toString();
		}
	}

	/** Builds the command used to start this component on a host.
     * @param hi An {@link ade.ADEHostInfo ADEHostInfo} object */
    public String buildStartCommand(ADEHostInfo hi) {
        // TODO: this should most likely either *not* have "public" in the
        // declaration (thereby making it accessible only to classes in the
        // ade package) or take an ADEComponent parameter (which then must be
        // equal to that stored in the "as" field) for access control
        //String hostip, String javabin, String adeclasspath) {
        // examine optional arguments
        StringBuilder args = new StringBuilder();
        StringBuilder cmd = new StringBuilder();
        String filesep = hi.hostos.fsep();
        String pathsep = hi.hostos.psep();
        boolean isWindows = false;

        // put debugging first; if active, it'll go into effect ASAP
        args.append(" -d ");
        args.append(dbg);
        // TODO: this should be stored as a field, not calculated each time
        if (ADEGlobals.isWindows()) {
            isWindows = true;
        }
        if (onlyonhosts != null && onlyonhosts.size() > 0) {
            args.append(" -o");
            for (String host : onlyonhosts) {
                args.append(" " + host);
            }
        }
        // TODO: ignoreconfig isn't used right now; it's supposed to treat
        // cases where if a registry, check if components *need* restarting.
        // See also: the startComponent method in ADEHostInfo
        if (configfile != null && !ignoreconfig) {
            args.append(" -f " + (isWindows ? ADEGlobals.windowsPath(configfile) : configfile));
            if (debug) {
                System.err.println("\tConfig file=" + configfile);
            }
        }
        if (numrestarts > 0) {
            if (debug) {
                System.err.println("\tRestarts=" + numrestarts);
            }
            args.append(" -e " + numrestarts);
        }
        if (credentials != null) {
            if (debug) {
                System.err.println("\tCredentials=" + credentials);
            }
            args.append(" -c " + credentials);
        }

        // always add the access privileges, this can never be empty
        args.append(" -a");
        if (debug) {
            System.err.print("\tAccess list=");
        }
        if (userAccess != null && userAccess.size() > 0) {
            for (String access : userAccess) {
                if (debug) {
                    System.err.print(access);
                }
                args.append(" " + access);
            }
        } else {
            args.append(" " + ADEGlobals.ALL_ACCESS);
        }
        if (requiredDevices != null && requiredDevices.size() > 0) {
            args.append(" --devices");
            for (String dev : requiredDevices) {
                args.append(" " + dev);
            }
        }
        if (groups != null && !groups.isEmpty()) {
            args.append(" --groups");
            for (String group : groups) {
                args.append(" " + group);
            }
        }
        if (connecttogroups != null && !connecttogroups.isEmpty()) {
            args.append(" --connecttogroups");
            for (String group : connecttogroups) {
                args.append(" " + group);
            }
        }
        if (killForCtrlc) {
            args.append(" --ctrlc");
        }
        if (additionalargs != null && !additionalargs.equals("")) {
            if (debug) {
                System.err.println("\tAdditional args=" + additionalargs);
            }
            args.append(" " + additionalargs);
        }

	// added the -g and --logging flags
	if (withlocallogging)
            args.append(" --logging");

	if (guiRequested) {
            args.append(" -g");
            for (String eachParticularVis : guiParticularVisualizations) {
            	args.append(" " + eachParticularVis);
            }
	}

        // build the command string
        if (isWindows && hi.javabin.indexOf(' ') >= 0) {
            cmd.append("\"");
            //cmd.append(ADEGlobals.windowsPath(hi.javabin));
            cmd.append(hi.javabin);
            cmd.append("\"");
        } else {
            cmd.append(hi.javabin);
        }
        cmd.append(" ");
        if ( (javavmargs != null) && (javavmargs.length() > 0) ) { 
            // MZ:  javavmargs are now just as you would 
            //    find them on the commandline, e.g., -arg1 -arg2, WITH leading dashes
            cmd.append(javavmargs);
            cmd.append(" ");
        }
	// MS: FIXED THE WRONG COMPONENT SUBSTITUTION
        cmd.append("-Djava.rmi.server.hostname=");
        cmd.append(hi.hostip);
        // MS: needed to be fixed to reflect the new way to startup components
        cmd.append(" -Dcomponent=");
        cmd.append(type);
        cmd.append(" -cp ");
        // PWS: only worried about spaces in Windows adehome?
        if (isWindows && hi.adehome.indexOf(' ') >= 0) {
            cmd.append("\"");
        }

        // PWS: I don't think the absolute path to adehome should be used here;
        // if a user wants to start a component in an alternative directory via the 
        // startdirectory directive, prepending adehome to the classpath elements 
        // will effectively negate that, whereas when startdirectory is not given,
        // adehome is the fallback.  Either way, these jar files are relative to 
        // the startdirectory, so there's no reason to make them absolute.  Leaving
        // them relative will facilitate portable config file generation.

        /*
        // Add core jars to default path before adehome
        //cmd.append(hi.adehome);
        //if (! hi.adehome.endsWith(hi.filesep)) cmd.append(filesep);
        cmd.append("core"+filesep+"ADEcore.jar");
        cmd.append(pathsep);
        //cmd.append(hi.adehome);
        //if (! hi.adehome.endsWith(hi.filesep)) cmd.append(filesep);
        cmd.append("core"+filesep+"Action.jar");
        cmd.append(pathsep);
        //cmd.append(hi.adehome);
        //if (! hi.adehome.endsWith(hi.filesep)) cmd.append(filesep);
        cmd.append("jars"+filesep+"visionJNI.jar");
        cmd.append(pathsep);
        * 
        */
	// MS: added the build subdirectory, just in case
        cmd.append("build");
        cmd.append(pathsep);
        cmd.append("jars"+filesep+"visionJNI.jar");
        cmd.append(pathsep);
        cmd.append(".");
        //cmd.append(hi.adehome);
        //if (hi.adehome.endsWith(hi.filesep)) {
        //    cmd.setLength(cmd.length() - hi.filesep.length());
        //}
        if (isWindows && hi.adehome.indexOf(' ') >= 0) {
            cmd.append("\"");
        }
        if (userclasspath != null) {
            cmd.append(pathsep);
            // accept either ":" (UNIX) or ";" (WINDOWS), replacing it with 
            //     the "local" pathsep variety.
            cmd.append(userclasspath.replace(":", pathsep).replace(";", pathsep));
        }
        cmd.append(" ");

	// MS: added the default extension for the name of implementations of component interfaces
        // MS: needed to be fixed to reflect the new way to startup components
        cmd.append("ade.ADEComponentImpl");

        if (  (javavmargs != null) && (javavmargs.length() > 0)  ) {
            // append a -v followed by the javavmargs to the component, that way the 
            //     component knows which arguments it had received (needed for recovery, I think; MZ)
            //     Remember to follow the vmargs with a "--" delimiter,
            //     to help the component tell javavmargs apart from ADE args.
            cmd.append(" -v ");
            cmd.append(javavmargs);
            cmd.append(" -- ");
        }
        cmd.append(" -l ");
        cmd.append(host);
        cmd.append(" ");
        cmd.append(port);
        if (name != null) {
            cmd.append(" ");
            cmd.append(name);
        }
        if (registryIP != null) {
            cmd.append(" -r ");
            cmd.append(registryIP);
            cmd.append(" ");
            cmd.append(registryPort);
            cmd.append(" ");
            cmd.append(registryName);
        }
        cmd.append(" ");
        cmd.append(args.toString());
        
        //System.out.println("Command = " + cmd.toString());
        
        return cmd.toString();
    }

    /** Sets the recovery multiplier, which dictates how many reaper
     * periods must elapse before an {@link ade.ADERegistry ADERegistry}
     * initiates recovery procedures (must be greater than 0). Note that
     * the multiplier will be in effect <b>whenever</b> the number of
     * restarts is greater than zero; if you want to change the delay
     * before shutting down (for instance, if shutting yourself down),
     * change the setting back to one (possibly in the {@link
     * ade.ADEComponentImpl#localshutdown localshutdown} method).
     * @param mult the number of reaper periods */
    public void setRecoveryMultiplier(int mult) {
        if (mult > 0) {
            recoveryMultiplier = mult;
        }
    }

    /** Checks the time elapsed since the last update occurred and compares
     * it to the parameter time.
     * @param reaptime the time to compare to the elapsed time
     * @return <tt>true</tt> if not as much time has elapsed, <tt>false</tt>
     * otherwise */
    public boolean checkStatus(long reaptime) {
        long elapsedTime = System.currentTimeMillis() - lastUpdate;
        //System.out.println("\tElapsed time since update for "+ name +" is "+ elapsedTime);
        if (elapsedTime > reaptime) {
            return false;
        }
        return true;
    }

    /** Checks to see if the time elapsed since the last update is greater
     * than the reaper period times the recovery multiplier.
     * @param reaptime the time to compare to the elapsed time
     * @return <tt>true</tt> if recovery procedures should be initiated
     * (i.e., too much time has elapsed), <tt>false</tt> otherwise */
    public boolean checkStartRecovery(long reaptime) {
        long elapsedTime = System.currentTimeMillis() - lastUpdate;
        if (elapsedTime > (reaptime * recoveryMultiplier)) {
            return true;
        }
        return false;
    }

    // intercept serialization method to write without stub
    private void writeObject(ObjectOutputStream out) throws IOException {
        // check if the component stub should be serialized
        if (withComponentStub) {
            out.defaultWriteObject();
        } else {
            // temporarily remove the stub
            ADEComponent astemp = as;
            as = null;
            out.defaultWriteObject();
            as = astemp;
        }
    }
}
