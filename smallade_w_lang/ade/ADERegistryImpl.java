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

import ade.gui.ADEGuiCallHelper;
import ade.gui.ADEGuiCreatorUtil;
import ade.gui.ADEGuiVisualizationSpecs;
import ade.gui.InfoRequestSpecs;
import ade.gui.SystemViewAccess;
import ade.gui.SystemViewComponentInfo;
import ade.gui.SystemViewStatusData;
import ade.gui.sysview.ADESystemView;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.InetAddress;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.security.AccessControlException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.lang.reflect.Method;

/**
 * The <tt>ADERegistry</tt> is used both as a repository for available
 * <tt>ADEComponents</tt> and as a means of connecting <tt>ADE</tt> software via
 * Java's remote method invocation (RMI) mechanism. It is important to note that
 * it is <b>not</b> an instantiation of a Java
 * {@link java.rmi.registry.Registry Registry}; instead, it <i>creates</i> a
 * Java {@link java.rmi.registry.Registry Registry} on the local host with only
 * itself as an entry, then provides methods to connect <tt>ADE</tt> software
 * contained in its "library" (i.e., a set of {@link
 * java.util.HashMap HashMap}s that store the locations and state of other
 * <tt>ADE</tt> components). <p> The <tt>ADERegistry</tt>'s basic function is to
 * perform the bookkeeping for maintaining the location/name mapping (and other
 * information) necessary for communication between
 * {@link ade.ADEComponent ADEComponent}s, each of which is registered with an
 * <tt>ADERegistry</tt> (on this or another host). To facilitate this
 * functionality, an <tt>ADERegistry</tt> extends the
 * {@link ade.ADEComponentImpl ADEComponentImpl} class, inheriting all the
 * functionality contained therein. <p> This class can only be instantiated as a
 * separate process via the command line and is final, for security reasons.
 *
 * @see ade.ADERegistry ADERegistry
 * @see ade.ADERegistryAdminTool ADERegistryAdminTool
 * @see ade.ADEComponentImpl ADEComponentImpl
 * @see ade.ADEComponentInfo ADEComponentInfo
 * @see ade.ADEHostInfo ADEHostInfo
 * @see ade.ADEUser ADEUser
 * @see ade.ADEPreferences ADEPreferences
 * @see ade.ADEGlobals ADEGlobals
 */
public final class ADERegistryImpl extends ADEComponentImpl implements ADERegistry {

    private static final long serialVersionUID = 7526472295622776147L;
    private static String prg = "ADERegistryImpl";
    private static boolean verbose = false;  // lots o' output
    private boolean debugging = false;  // toggle by AdminRegistry program
    private boolean debugReaper = false;  // ditto
    private boolean debugRegistration = false; // for debugging [de]registration
    private boolean debugRequestConnection = false; // for debugging the request of connections
    private boolean debugTimes = false;
    /**
     * The hosts known to this registry.
     */
    final private HashMap<String, ADEHostInfo> knownhosts = new HashMap<String, ADEHostInfo>();// key on IP
    /**
     * The map of components that are currently registered with this registry. Note
     * that components in this hash should also be in the componentTypes and
     * heartbeats maps. If the component is an <tt>ADERegistry</tt>, it will also
     * be in the registries hash. See the
     * {@link #registerComponent(ADEComponentInfo, String)} for details on the
     * registration process.
     */
    final private Map<String, ADEComponentInfo> components = Collections.synchronizedMap(new HashMap<String, ADEComponentInfo>()); // key on type+name
    /**
     * The types of component known to the this registry. The map's key is the
     * component type, while the value is a secondary map. Note that a component's
     * type refers not only to its actual class name, but also all the
     * <tt>Remote</tt> interfaces that it implements. The secondary map's key is
     * <b>not</b> the same as the key of the <tt>components</tt> hash (i.e., it is
     * <tt>name</tt>, not <tt>type$name</tt>), but the value is the component's
     * same <tt>ADEComponentInfo</tt> object.
     */
    final private Map<String, HashMap<String, ADEComponentInfo>> componentTypes = Collections.synchronizedMap(new HashMap<String, HashMap<String, ADEComponentInfo>>()); // key on type
    /**
     * The active processes that have started an ADEComponent.
     */
    final private Map<ADEComponentInfo, ADELocalProcessStarter> procs = Collections.synchronizedMap(new HashMap<ADEComponentInfo, ADELocalProcessStarter>()); // for cleanup
    /**
     * A map of the components from which this registry should be receiving
     * heartbeat signals (i.e., the components are registered).
     */
    final private Map<String, ADEComponentInfo> heartbeats = Collections.synchronizedMap(new HashMap<String, ADEComponentInfo>()); // key on type+name
    /**
     * A map of the components that were registered but have been "reaped".
     */
    final private Map<String, ADEComponentInfo> inrecovery = Collections.synchronizedMap(new HashMap<String, ADEComponentInfo>()); // key on type+name
    /**
     * The updates coming from a component (this method of updating will be removed
     * in the future).
     */
    final private HashMap<String, ADEMiniComponentInfo> pendingupdates = new HashMap<String, ADEMiniComponentInfo>(); //key on type+name
    /**
     * Map of all registries in the system for request forwarding, etc.
     */
    final private Map<ADERegistry, ADEComponentInfo> registries = new HashMap<ADERegistry, ADEComponentInfo>(); // key on registry RMI reference
    /**
     * The map that keeps track of what component to notify about another component
     * joining
     */
    final private HashMap<ADEComponent, String[][]> notifications = new HashMap<ADEComponent, String[][]>(); // key on component RMI reference
    // TODO: use the isAdmin field in ADEUser to eliminate the adminDB
    /**
     * The recognized administrators.
     */
    final private Map<String, ADEUser> adminDB = Collections.synchronizedMap(new HashMap<String, ADEUser>()); // key on uid
    private RegistryReaper myReaper;      // maintains connections
    private ADERemoteCallTimer rctReg;    // obtained from superclass
    private ADERemoteCallTimer rctServ;   // obtained from superclass
    private static Remote thisreg = null; // for checking RMI binding
    //  sundry variables for internal workings
    private boolean allowshutdownbyregistries = false; // shutdown by other regs
    private String hostsfile;
    final private String adminIDdefault = "admin";
    final private String adminpassdefault = "admin";
    private String adminID;
    private String adminpasswd;
    private boolean shuttingDown; // for graceful exits
    private Boolean loggingrequest = false;  // for logging

    /**
     * Constructor that starts the whole shebang.
     */
    public ADERegistryImpl() throws RemoteException {
        super();
        //dbg = 6;
        if (adminID == null) {
            adminID = adminIDdefault;
        }
        if (adminpasswd == null) {
            adminpasswd = adminpassdefault;
        }

        shuttingDown = false;
	
	try {
	    reglock = new ADELock("RegistryLock");
	} catch(IOException ioe) {
	    System.err.println("WARNING: could not create registry lock due to " + ioe);
	}


        if (verbose || dbg > 5) {
            System.out.println("Creating hashmaps...");
        }
        // confirm a local Java registry is running
        try {
            if (!confirmLocalRegistry()) {
                // ID already bound?!? Possibly another registry has already
                // restarted this registry...
                System.err.println(myID + " ******** ID already bound; aborting...");
                System.exit(ADEGlobals.ExitCode.JAVA_REG.code());
            }
        } catch (Exception e) {
            // no sense in running if we cannot create an RMI registry
            System.err.println("Cannot create RMI registry; aborting...\n" + e);
            System.exit(ADEGlobals.ExitCode.JAVA_REG.code());
        }
        // add the 1st admin, make them a user (since
        // they could just add themselves anyway), and
        // start the reaper.
        if (verbose || dbg > 5) {
            System.out.println("Creating registry administrator...");
        }
        HashSet<String> v = new HashSet<String>();
        v.add(ADEGlobals.ALL_ACCESS);
        ADEUser u = new ADEUser(adminID, adminpasswd, true, false, v);
        if (verbose || dbg > 5) {
            System.out.println("Adding admin to dbs...");
        }
        adminDB.put(adminID, u);
        userDB.put(adminID, u);
        // create the thread that will reclaim components on disconnection
        if (verbose || dbg > 5) {
            System.out.println("Creating reaper...");
        }
        myReaper = new RegistryReaper(ADEGlobals.DEF_RPPULSE, this);
        if (verbose || dbg > 5) {
            System.out.println("Starting reaper...");
        }
        myReaper.start();
        // put this host in just in case we did not get a hostlist as this
        // host must be OK (since the registry is running on it);
        // note, however, that this info will get overwritten by info about
        // the same host supplied in the hostsfile
        ADEHostInfo hi = requestHostInfo(this);
        knownhosts.put(hi.hostip, hi);
        checkHostsFile();
        // this will check if an ADE system needs to be started up based on the
        // optional configfile and start it in separate thread
        checkConfigFile();

	// create the call timers once and for all here
	ensureRCTServ();
	ensureRCTReg();
    }

    // sets up the remote call timer for the registry, which allows is to call any infrastructure method in components
    // TODO: this adds also non-remote methods, should probably be removed for efficiency...
    private void ensureRCTServ() {
        if (rctServ == null) {
	    try {
		Class adecomponentclass = Class.forName("ade.ADEComponent");
		rctServ = new ADERemoteCallTimer(ADEGlobals.DEF_HBPULSE,adecomponentclass);
		ArrayList<String> regmeths = new ArrayList<String>();
		for (Method m : adecomponentclass.getMethods()) {
		    regmeths.add(ADEGlobals.getMethodString(m));
		}
		rctServ.setAllMethods(regmeths);
	    } catch(Exception e) {
		System.err.println("Cannot set component remote call timer, exiting... \n");
		System.exit(ADEGlobals.ExitCode.RCT_CREATION.code());
	    }
	}
    }

    // sets up the remote call timer for the registry, which allows is to call any infrastructure method in registries and components
    // TODO: this adds also non-remote methods, should probably be removed for efficiency...
    private void ensureRCTReg() {
        if (rctReg == null) {
	    try {
		Class aderegistryclass = Class.forName("ade.ADERegistry");
		rctReg = new ADERemoteCallTimer(ADEGlobals.DEF_RPPULSE,aderegistryclass);
		ArrayList<String> regmeths = new ArrayList<String>();
		for (Method m : aderegistryclass.getMethods()) {
		    regmeths.add(ADEGlobals.getMethodString(m));
		}
		rctReg.setAllMethods(regmeths);
	    } catch(Exception e) {
		System.err.println("Cannot set registry remote call timer, exiting... \n");
		System.exit(ADEGlobals.ExitCode.RCT_CREATION.code());
	    }
	}
    }

    private class RecoveryThread extends Thread {

        ADERegistry mycomponent;
        ADEComponentInfo s0;
        ADEMiniComponentInfo amsi2;
        long startTime;
        StringBuilder sb;

        public RecoveryThread(ADERegistry m, ADEComponentInfo s) {
            mycomponent = m;
            s0 = s;
            s0.registryType = getTypeFromID(myID);
            s0.registryName = getNameFromID(myID);
            // TODO: ignoreconfig isn't used right now; it's supposed to treat
            // cases where if a registry, check if components *need* restarting
            // See also: the startComponent method in ADEHostInfo
            s0.ignoreconfig = true;
            sb = new StringBuilder();
        }

        private void showTime(String msg) {
            sb.setLength(0);
            sb.append(myID);
            sb.append(": recovery of ");
            sb.append(s0.getKey());
            sb.append("-");
            sb.append(msg);
            sb.append(" ");
            sb.append(System.currentTimeMillis() - startTime);
            sb.append("ms");
            System.out.println(sb.toString());
        }

        @Override
        public void run() {
            String key = s0.getKey();

            if (debugTimes) {
                startTime = System.currentTimeMillis();
            }
            if (debugReaper || dbg > 2) {
                System.out.println(myID + ": IN RECOVERY THREAD " + this + ".....for " + key);
            }
            // drat; the component is gone. deregister and attempt restart...
            if (debugReaper) {
                System.out.println("\t" + this + ": " + key + " disconnected! Deregistering...");
            }
            deregisterComponent(s0);
            if (debugTimes) {
                showTime("deregistered");
            }
            if (debugReaper || dbg > 3) {
                System.out.println("\t" + this + ": Removing heartbeat for " + key + "...");
            }
            // note: synchronized maps, no need to worry

            // MS: already done in RegistryReaper before this thread is started
            // inrecovery.put(key, heartbeats.remove(key));

	    // now remove the component from the servers
            removeComponent(s0);

            if (debugTimes) {
                showTime("hb removal");
            }
            if (debugReaper || dbg > 5) {
                System.out.println("\t" + this + ": heartbeats.size() now =" + heartbeats.size() + "...");
            }
            // MS: SHOULD LOCK BE REMOVED?
            while (!reglock.lock()) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                }
            }
                    
            if (s0.numrestarts > 0) {
                s0.setRecoveryState(ADEGlobals.RecoveryState.INREC);
                // if restarting, always print this
                System.out.println(myID + ": STARTING RECOVERY THREAD for " + key);
                try {
                    requestComponentInfo(mycomponent);
                } catch (Exception e5) {
                    // this should never happen; this object
                    System.err.println("HOW>??? " + e5);
                }
                // confirm our local JAVA registry
                boolean regconf = false;
                while (!regconf) {
                    try {
                        if (!confirmLocalRegistry()) {
                            // MS: SHOULD REGLOCK be removed
                            reglock.unlock();
                            System.err.println(myID + ": RecoveryThread: different ADERegistry bound to my ID, aborting...");
                            System.exit(ADEGlobals.ExitCode.JAVA_REG.code());
                        } else {
                            regconf = true;
                        }
                    } catch (Exception e) {
                        System.err.println(myID + ": Java registry unconfirmed:\n" + e);
                        //System.err.println(myID +": Java registry unconfirmed:");
                        //e.printStackTrace();
                        try {
                            Thread.sleep(ADEGlobals.DEF_RPPULSE);
                        } catch (Exception ignore) {
                        }
                    }
                }
                if (debugTimes) {
                    showTime("java reg check");
                }
                // attempt to restart the component
                // MS: only unbind if it is a registry, as normal components are not bound...
                if (s0.isregistry) {
                    unbindFromRegistry(key, false);
                }
                // MS: fixed the while loop problem here
                int attempts = 1;
                for (; attempts <= s0.numrestarts; attempts++) {
                    if (attemptRecoveryComponentDown(s0)) {
                        break;
                    }
                    // failed one attempt, wait a bit
                    if (debugReaper || debugging || dbg > 3) {
                        System.out.println("\t" + this + ": attempt " + attempts + " to restart " + s0.name + " failed.");
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                    }
                }
                // if we exhausted our number of restarts, then give up
                if (attempts == s0.numrestarts) {
                    if (debugReaper || debugging || dbg > 3) {
                        System.out.println("\t" + this + ": Failed to restart " + s0.name + " after " + attempts + " attempts .... SORRY!");
                    }
                    // done trying to restart, just quit
                    s0.setRecoveryState(ADEGlobals.RecoveryState.UNREC);
                    System.out.println(myID + ".............." + key + " STOPPED..." + s0.recState);
                    inrecovery.remove(key);
                    // MS: only unbind if it is a registry, as normal components are not bound...
                    if (s0.isregistry) {
                        unbindFromRegistry(key, false);
                    }
                    // MS: SHOULD REGLOCK be removed
                    reglock.unlock();
                    if (debugTimes) {
                        showTime("failed recovery");
                    }
                    return;
                }
                // if we made it here, component was restarted...
                s0.setRecoveryState(ADEGlobals.RecoveryState.OK);
                // adjust the number of remaining attempts
                s0.numrestarts -= attempts;
                // we don't want to unlock, however, until the just restarted
                // component registers with us; unlock in the registerComponent method
                if (debugTimes) {
                    showTime("recovery");
                }
                // MS: there is no REGLOCK here, why not???
            } else {
                // restarts == 0, let it go
                s0.setRecoveryState(ADEGlobals.RecoveryState.UNREC);
                inrecovery.remove(s0.getKey());
                // MS: only unbind if it is a registry, as normal components are not bound...
                if (s0.isregistry) {
                    unbindFromRegistry(s0.getKey(), false);
                }
                // MS: SHOULD REGLOCK be removed
                reglock.unlock();
                System.out.println(".............." + s0.name + " STOPPED; " + s0.recState);
                amsi2 = s0.getADEMiniComponentInfo();
                synchronized (pendingupdates) {
                    pendingupdates.put(amsi2.id, amsi2);
                }
            }
        } // end run()
    }

    /**
     * Confirm a Java registry is running (or start one if not) and bind this
     * component to its ID.
     *
     * @return <tt>true</tt> if this <tt>ADERegistry</tt> is now bound in the
     * Java registry; <tt>false</tt> if this <tt>ADERegistry</tt>'s ID was bound
     * to a different object (note that in either case, a Java registry will be
     * running)
     * @throws Exception A Java registry cannot be created or the lookup/binding
     * failed
     */
    private boolean confirmLocalRegistry() throws Exception {
        Registry r;
        int myport;
        boolean mebound = true;

        if (debugReaper || verbose || dbg > 8) {
            System.out.println(myID + ": checking for local registry...");
        }
        myport = getPortMe();
        // this has to be done in a rather roundabout way; *every* getRegistry()
        // call will succeed, whether a Java registry is running or not. To
        // confirm one is actually running, we have to attempt access, hence the
        // lookup(myID) call. Four exceptions can be thrown:
        // NotBoundException: our ID isn't bound, although a registry is running;
        //   we want to bind our name
        // RemoteException: should never happen, as we're operating locally
        // AccessException: problem, in that we've been denied access
        // NullPointerException: should never happen, as myID shouldn't be null
        //   (but at least there's a registry running)
        // For the NotBound, we'll bind our name; for all others, we'll try to
        // create a registry and bind our name.
        // TODO: there be weirdness here: originally, the getRegistry came
        // first; although a reference was being returned, it would throw
        // a ConnectException. The following seems to solve it, but I'm not
        // sure *why* it was happening...
        try {
            r = LocateRegistry.createRegistry(myport);
            if (verbose || dbg > 5) {
                System.out.println(myID + ": created Java registry on " + myport);
            }
        } catch (ExportException ee) {
            try {
                r = LocateRegistry.getRegistry(myport);
                if (verbose || dbg > 5) {
                    System.out.println(myID + ": got Java registry on " + myport);
                }
            } catch (Exception e) {
                System.err.println(myID + ": creating Java Registry failed:\n" + e);
                throw e;
            }
        } catch (RemoteException re) {
            // if it is not there, create it and register self; note that any
            // exceptions here are not caught, just propagated
            if (verbose || dbg > 5) {
                System.out.println(myID + ": NO JAVA REGISTRY, starting new one...");
            }
            try {
                r = LocateRegistry.createRegistry(myport);
            } catch (Exception e) {
                System.err.println(myID + ": creating Java Registry failed:\n" + e);
                throw e;
            }
        }
        try {
            Remote reg = r.lookup(myID);
            if (verbose || dbg > 5) {
                System.out.println(myID + ": found my ID in Java registry");
            }
            if (!reg.equals(thisreg)) {
                if (verbose || dbg > 5) {
                    System.out.println(myID + ": Remote Object not me?!?!");
                }
                mebound = false;
            }
        } catch (NotBoundException nbe) {
            // good! a registry is running, but we have to bind our name
            if (verbose || dbg > 5) {
                System.out.println(myID + ": my ID not bound...attempting to bind");
            }
            try {
                Naming.bind(getRMIStringMe(), this);
                // when bound, get the ref for later comparisons
                if (debugReaper || verbose || dbg > 5) {
                    System.out.println(myID + ": bound ID; confirming...");
                }
                thisreg = r.lookup(myID);
                if (debugReaper || verbose || dbg > 5) {
                    System.out.println(myID + ": confirmed; bound ID at " + System.currentTimeMillis());
                }
                return true;
            } catch (AlreadyBoundException abe) {
                if (debugReaper || verbose || dbg > 5) {
                    System.err.println(myID + ": got registry; ID already bound " + abe);
                }
            } catch (Exception e) {
                // useless if we can't bind our ID, so fail
                System.err.println(myID + ": bind in Registry failed:\n" + e);
                throw e;
            }
        } catch (java.rmi.ConnectException ce) {
            // problem connecting to the Java registry; we'll try again
            System.err.println(myID + ": connecting with Registry failed:\n" + ce);
            try {
                Naming.rebind(getRMIStringMe(), this);
                // when bound, get the ref for later comparisons
                thisreg = r.lookup(myID);
                if (debugReaper || verbose || dbg > 8) {
                    System.out.println(myID + ": bound ID at " + System.currentTimeMillis());
                }
                return true;
            } catch (Exception e) {
                // useless if we can't bind our ID, so fail
                System.err.println(myID + ": rebind in Registry failed:\n" + e);
                throw e;
            }
        } catch (Exception e) {
            // other exceptions are total failures
            System.err.println(myID + ": lookup with Registry failed:\n" + e);
            throw e;
        }
        if (debugReaper || verbose || dbg > 8) {
            System.out.println(myID + ": got local registry but did not bind ID");
        }
        return mebound;
    }

    /*
     * * * * * * * * * * * * * * * * * * * *
     * Interface methods * * * * * * * * * * * * * * * * * * *
     */
    /**
     * Provides the means for a client to check what components of a particular
     * type are registered with its registry (note that user access is not checked here)
     *
     * @param uid the name of the user
     * @param upw the password for the user
     * @param constraints the type of to request an Agent from
     * @return an {@link java.util.ArrayList ArrayList} of ADEComponent names that
     * meet the constraints
     * @throws RemoteException if the request fails
     * @exception AccessControlException if the password is bad
     */
    @Override
    public ArrayList<String> requestLocalComponentList(
            String uid, // The username
            String upw, // The password
            String[][] constraints // The constraints to be met by the component
            ) throws AccessControlException, RemoteException {

	// get the list of all local components
	ArrayList<String> componentnames = new ArrayList<String>();
	for(ADEComponentInfo asi : getAllApplicableComponents(uid,upw,constraints,this,null,false,false))
	    componentnames.add(asi.getName());
	return componentnames;
    }

    /**
     * Provides the means for a client to check what components of a particular
     * type are registered in the whole ADE system (note that user access is not checked here)
     *
     * @param uid the name of the user
     * @param upw the password for the user
     * @param constraints the type of to request an Agent from
     * @return an {@link java.util.ArrayList ArrayList} of ADEComponent names that
     * meet the constraints
     * @throws RemoteException if the request fails
     * @exception AccessControlException if the password is bad
     */
    @Override
    public ArrayList<String> requestComponentList(
            String uid, // The username
            String upw, // The password
            String[][] constraints // The constraints to be met by the component
            ) throws AccessControlException, RemoteException {

	// get the list of all local components
	ArrayList<String> componentnames = new ArrayList<String>();
	for(ADEComponentInfo asi : getAllApplicableComponents(uid,upw,constraints,this,null,false,true))
	    componentnames.add(asi.getName());
	return componentnames;
    }


    // check if the component meets the criteria specified in the constraints list
    // each entry in the constraints ArrayList is an ArrayList consisting of constraint type followed by values
    // for now, only pairs are implemented
    // lists are in disjunctive normal form [][]...[] or [][]...[] or...  where [] is either a tuple [. .] or a triple starting with [not . .]
    private boolean meetsCriteria(ADEComponentInfo asi, String[][] constraints) {
        boolean sofar = true;
        //System.out.println("Examining " + asi.getName());
        if (constraints == null) {
            return true;
        }
	// MS: added a catch for index out of bounds exception in case the constraint list is not formatted correctly
	try {
	    for (String[] constraint : constraints) {
		// if there is no constraint or the "or" constraints, skip it
		if (constraint.length == 0) {
		    continue;
		} else if (constraint.length == 1 && constraint[0].equals("or")) {
		    // check if everything up to now has been true, in which case we can return
		    if (sofar) {
			return true;
		    } // otherwise see if the next disjunction might be true
		    else {
			sofar = true;
		    }
		    // consider the first set met
		    continue;
		}
		String first = constraint[0];
		String second;
		boolean negated;
		if (first.equals("not")) {
		    negated = true;
		    first = constraint[1];
		    second = constraint[2];
		}
		else {
		    negated = false;
		    second = constraint[1];
		}
		//System.out.println("... checking contstraint: " + first + "=" + second);
		if (first.equals("type")) {
		    if (!asi.getInterfaces().contains(second)) {
			sofar = false || negated;
		    }
		} else if (first.equals("host")) {
		    if (!asi.getHostName().equals(second)) {
			sofar = false || negated;
		    }
		} else if (first.equals("group")) {
		    if (!asi.getGroups().contains(second)) {
			sofar = false || negated;
		    }
		} else if (first.equals("name")) {
		    if (!asi.getName().equals(second)) {
			sofar = false || negated;
		    }
		} /*
		   * TODO: add method access else if (first.equals("method")) { if
		   * (!asi.getMethods().contains(second)) { sofar = false; } }
		   *
		   */ /*
		       * TODO: this is not working yet because the ADEComponentInfo does not
		       * seem to properly keep track of which components are registered in a
		       * component... else if (first.equals("hasComponent")) { if
		       * (!asi.getComponents().contains(second)) return false; }
		       */ // always return when there is an illegal sequence
		else {
		    System.out.println("Unrecognized constraint in requestComponent list: " + first + "  Constraints not met.");
		    return false;
		}
	    }
	} catch (Exception e) {
	    System.err.println("Formatting problem in constraint list, returning 'false' for meetsCriteria!");
	    // do not consider this a match
	    sofar = false;
	}
        // return whatever we have gathered (must be the last disjunct)
        return sofar;
    }

    /**
     * generates a list of ADE component infos of all components meeting the criteria to which the given user has access
     *
     * @param uid the name of the user
     * @param upw the password for the user
     * @param constraints to be met by the component
     * @param onbehalf the {@link ade.ADERegistry ADERegistry} making the
     *        request on behalf of a registry (null if it is the current one)
     * @param  checkavailableforuser // whether the user is allowed and could get a connection
     * @return an {@link java.util.ArrayList ArrayList} of ADE component infos of components that 
     *         meet the constraints and are accessible to the user
     * @throws RemoteException if the request fails
     * @exception AccessControlException if the password is bad
     */
    @Override
    public ArrayList<ADEComponentInfo> getAllApplicableComponents(
	    String uid, // the username of the requesting component
            String upw, // the password
            String[][] constraints, // the constraints to be met by the remote component
	    ADERegistry onbehalf, // the requesting registry
	    ADEUser u, // if applicable the user info
	    boolean checkavailableforuser, // whether the user is allowed and could get a connection
	    boolean forward // whether the request should be forwarded 
            ) throws AccessControlException, RemoteException {

        // make sure this user is allowed to access this registry
        if (onbehalf.equals((ADERegistry)this)) {
	    // only verify if no user info is passed in
	    if (u == null) {
		if ((u = verifyUser(uid, upw)) == null) {
		    throw new AccessControlException("Bad username/password");
		}
	    }
        } else {
	    // this is a forwarded request
            try {
		// if no user info was provided, get it
		if (u == null) {		
		    // verify that the user is known to the remote registry and retrieve the user data
		    // as the user is not registered with this registry
		    // this also verifies that registry onbehalf is registered with this registry
		    u = (ADEUser) rctReg.remoteCall("verifyUser", onbehalf, uid, upw, this);
		    if (u == null)
			throw new RemoteException("Could not verify user " + uid);
		}
	    } catch(AccessControlException ace) {
		throw new RemoteException("Registry not authorized to get user information in verifying user " + uid);
            } catch (Exception e) {
                throw new RemoteException("Unknown problems during user verification for user " + uid + " " + e);
            }
        }

        ArrayList<ADEComponentInfo> eligible = new ArrayList<ADEComponentInfo>();
        // lock the components, so as to not allow changes during lookup...
        synchronized (components) {
            for (ADEComponentInfo asi : components.values()) {
                // MS: removed the synchronization on the ASI, components should be sufficient
                if (!(asi.as instanceof ADERegistry) &&  meetsCriteria(asi, constraints)) {
                    if (!checkavailableforuser) {
                        eligible.add(asi);
                    }
                    // check if the user has access
                    // MS: FIXME: TODO: looks like we are checking things twice here, user access needs to be fixed
                    else if (asi.hasRightsTo(u) && asi.userAccess(u) && asi.allowConnect()) {
                        // add the component info to the list
                        eligible.add(asi);
                    }
                }
            }
        }
	// if we have additional registries, then check them as well
        // skip if we are already satisfying a request by another registry
        // i.e., forward only if directly contacted by the component
        // MS: added check that registries are not empty to avoid null pointer exception
        if (forward && !registries.isEmpty()) {
            try {
                // note that each registry will return an ArrayList of component names
                Object[] retList = rctReg.remoteCallConcurrent(
                        "getAllApplicableComponents", registries.keySet().toArray(),
                        uid, upw, constraints, this, u, checkavailableforuser, false);
		for(Object o : retList) {
		    // check that we got something meaningful back, otherwise ignore 
		    if (o != null && o instanceof ArrayList) {
			for(ADEComponentInfo asi : (ArrayList<ADEComponentInfo>)o) {
			    eligible.add(asi);
			}
		    }
		}
            } catch (Exception e) {
                System.err.println(myID + ": Unknown exception while contacting other registries in getAllApplicableComponents " + e);
            }
        }
	// at this point we added all applicable component infos in the whole ADE system, so return the list
        return eligible;
    }


    /**
     * Provides the means for a client to receive all {@link ade.ADEComponent
     * ADEComponent}s of a particular type excluding itself.
     *
     * @param uid the name of the user
     * @param upw the password for the user
     * @param requester the {@link ade.ADEComponent ADEComponent} making the request
     * @param constraints the constraints to be met by the component
     * @return an {@link java.util.ArrayList ArrayList} of  {@link ade.ADEComponentInfo ADEComponentInfo} of the requested components
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate permissions
     */
    @Override
    public ArrayList<ADEComponentInfo> requestConnections(
            String uid,
            String upw,
            ADEComponent requester,
            String[][] constraints // The constraints to be met by the component
            ) throws AccessControlException, RemoteException {
        // this one checks if the type registered under UID is among the
        // requested ones and will exclude it in the final list

	ADEUser u;
	// verify the user locally and get the user info
	if ((u = verifyUser(uid, upw)) == null) {
	    throw new AccessControlException("Bad username/password");
	}

        // initialize the list to be returned
        ArrayList<ADEComponentInfo> retVec = new ArrayList<ADEComponentInfo>();
	// generate a list of all applicable components
	for(ADEComponentInfo asi : getAllApplicableComponents(uid,upw,constraints,this,u,true,true)) {
	    // ensure that the requester does not register with itself...
	    if (requester == null || !requester.equals(asi.as)) {
		try {
		    debugPrint("Requesting connection...");
		    rctServ.remoteCall("requestConnectionRegistry", asi.as, uid, this, constraints);
		    // MS: took this out, because the client information will come through the Heartbeat update
		    // from the component... and only then is it guaranteed that the component actually has the client
		    // asi.addClient(uid);
		    retVec.add(asi);
		} catch (Exception e) {
		    StringBuilder sb = new StringBuilder(myID);
		    sb.append(": could not obtain connection to ");
		    sb.append(asi.getKey());
		    sb.append(":\n");
		    sb.append(e);
		    System.err.println(sb.toString());
		}
	    }
	}
	// return the list of components where a connection request has been made successfully
	return retVec;
    }

    /**
     * Provides the means for a client to receive an {@link ade.ADEComponent
     * ADEComponent}s of a particular type excluding itself.
     *
     * @param uid the name of the user
     * @param upw the password for the user
     * @param requester the {@link ade.ADEComponent ADEComponent} making the request
     * @param constraints the constraints to be met by the component
     * @return the {@link ade.ADEComponentInfo ADEComponentInfo} of the requested component
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate permissions
     */
    @Override
    public ADEComponentInfo requestConnection(
            String uid,
            String upw,
            ADEComponent requester,
            String[][] constraints // The constraints to be met by the component
            ) throws AccessControlException, RemoteException {

	ADEUser u;
	// verify the user locally and get the user info
	if ((u = verifyUser(uid, upw)) == null) {
	    throw new AccessControlException("Bad username/password");
	}

        // initialize the list to be returned
        ArrayList<ADEComponentInfo> candidates = new ArrayList<ADEComponentInfo>();
	// MS: new policy: first try to get a local connection, then try system-wide (reduces system-wide overhead)
	for(ADEComponentInfo asi : getAllApplicableComponents(uid,upw,constraints,this,u,true,false)) {
	    // ensure that the requester does not register with itself...
	    if (requester == null || !requester.equals(asi.as)) {
		candidates.add(asi);
	    }
	}
	// if there is no candidate, try system-wide if possible
        if (candidates.isEmpty() && !registries.isEmpty()) {
	    System.err.println("No local eligible candidates, trying system-wide with other registries...");
            try {
                // note that each registry will return an ArrayList of component names
                Object[] retList = rctReg.remoteCallConcurrent(
                        "getAllApplicableComponents", registries.keySet().toArray(),
                        uid, upw, constraints, this, u, true, false);
		for(Object o : retList) {
		    // check that we got something meaningful back, otherwise ignore 
		    if (o != null && o instanceof ArrayList) {
			for(ADEComponentInfo asi : (ArrayList<ADEComponentInfo>)o) {
			    candidates.add(asi);
			}
		    }
		}
            } catch (Exception e) {
                System.err.println(myID + ": Unknown exception while contacting other registries in getAllApplicableComponents " + e);
            }
        }
	// if there are candidates left, pick one and try to register with it
	while (!candidates.isEmpty()) {
	    int pick = (int) (Math.random() * candidates.size());
	    ADEComponentInfo asi = candidates.get(pick);
	    try {
		debugPrint("Requesting connection...");
		rctServ.remoteCall("requestConnectionRegistry", asi.as, uid, this, constraints);
		// MS: took this out, because the client information will come through the Heartbeat update
		// from the component... and only then is it guaranteed that the component actually has the client
		// asi.addClient(uid);
		// return the component where a connection request has been made successfully
		return asi;
	    } catch (Exception e) {
		StringBuilder sb = new StringBuilder(myID);
		sb.append(": could not obtain connection to ");
		sb.append(asi.getKey());
		sb.append(":\n");
		sb.append(e);
		System.err.println(sb.toString());
		candidates.remove(pick);
	    }
	}
	// no connection was made
	return null;
    }

    // MS: this is the new direct call method without registration via the registry
    // it also returns a reference to the component so that locally the component ref can be kept
    // for future direct calls...
    /**
     * Provides the means for a client to directly make a call in a remote component
     *
     * @param uid the name of the user
     * @param upw the password for the user
     * @param requester the {@link ade.ADEComponent ADEComponent} making the request
     * @param constraints the constraints to be met by the component
     * @return an {@link java.util.ArrayList ArrayList} of ADEComponent references
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate permissions
     */
    @Override
    public Object callMethodInRemoteComponent(
            String uid,
            String upw,
            ADEComponent requester,
            String[][] constraints, // The constraints to be met by the component
	    String methodname,
	    Object[] args
            ) throws AccessControlException, RemoteException {

	ADEUser u;
	// verify the user locally and get the user info
	if ((u = verifyUser(uid, upw)) == null) {
	    throw new AccessControlException("Bad username/password");
	}

        // find a list of all candidate components
        ArrayList<ADEComponentInfo> candidates = new ArrayList<ADEComponentInfo>();
	// generate a list of all applicable components
	for(ADEComponentInfo asi : getAllApplicableComponents(uid,upw,constraints,this,u,true,true)) {
	    // ensure that the requester does not register with itself...
	    if (requester == null || !requester.equals(asi.as)) {
		candidates.add(asi);
	    }
	}

	// go through the list until  a random candidate works or the list is empty
	while (!candidates.isEmpty()) {
	    int pick = (int) (Math.random() * candidates.size());
	    ADEComponentInfo asi = candidates.get(pick);
	    try {
		return rctServ.remoteCall("requestMethodCall", asi.as, uid, this, methodname, args);
	    } catch(Exception e) {
		// System.err.println("The component had a problem  " + e);
		// the component had a problem, remove it
		candidates.remove(pick);
	    }
	}
	// one-shot call did not succeed
	throw new RemoteException("One-shot call was not successful for method " + methodname);
    }

    /**
     * Check whether to allow return of the registry's {@link
     * ade.ADEComponentInfo ADEComponentInfo} information. We only give info to other
     * <tt>ADERegistry</tt>s.
     *
     * @param credentials checks access for component info
     * @return <tt>true</tt> if allowed, <tt>false</tt> otherwise
     */
    @Override
    protected boolean localRequestComponentInfo(Object credentials) {
        if (heartbeats.containsKey((String) credentials)) {
            // we can't simply check the registries hash because this registry
            // may be in the process of registering with the other
            ADEComponentInfo si = heartbeats.get((String) credentials);
            if (si.isregistry) {
                return true;
            }
        } else if (credentials instanceof String) {
            String aid = getTypeFromID((String) credentials);
            String apw = getNameFromID((String) credentials);
            if (verifyAdmin(aid, apw)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether to allow return of the registry's {@link
     * ade.ADEHostInfo ADEHostInfo} information. Right now we only give info to
     * other <tt>ADERegistry</tt>s.
     *
     * @param credentials Checks access for host info
     * @return <tt>true</tt> if allowed, <tt>false</tt> otherwise
     */
    @Override
    protected boolean localRequestHostInfo(Object credentials) {
        // TODO: MS: why is this simply cast to a string??? if credentials are not strings, then this will fail!
        if (heartbeats.containsKey((String) credentials)) {
            // we can't simply check the registries hash because this registry
            // may be in the process of registering with the other
            ADEComponentInfo si = heartbeats.get((String) credentials);
            if (si.isregistry) {
                return true;
            }
        }
        return false;
    }

    /**
     * Provides the means for a client to receive all {@link ade.ADEComponent
     * ADEComponent}s of a particular type (whenever they register)
     *
     * @param uid the name of the user
     * @param upw the password for the user
     * @param reqType the type of {@link ade.ADEComponent ADEComponent}
     * @return an {@link java.util.ArrayList ArrayList} of ADEComponent references
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    @Override
    public void requestNewComponentNotification(
            String uid, // The username
            String upw, // The password
            ADEComponent requester,
            String[][] constraints, // The constraints to be met by the component
            boolean on // whether to turn notification on or off
            ) throws AccessControlException, RemoteException {
        if (on) {
            notifications.put(requester, constraints);
        } else {
            notifications.remove(requester);
        }
	// MS: for add this directly, should be done differently in the future to avoid cross-registries refs...
	// add the component to all other registries as well so it will be notified when a matching component
	// registers in another registry
	if (!registries.isEmpty()) {
	    try {
		rctReg.remoteCallConcurrent("requestNewComponentNotification", registries.keySet().toArray(),
					    uid, upw, requester, constraints, on);
	    } catch(Exception e) {
		System.err.println(myID + ": Problem occurred during notification registration... " + e);
	    }
	}
    }

    /**
     * Provides the means for an {@link ade.ADEComponent ADEComponent} to obtain the
     * execution status of another <tt>ADEComponent</tt> from this {@link
     * ade.ADERegistry ADERegistry}.
     *
     * @param uid the name of the user
     * @param upw the password for the user
     * @param reqType the type of component
     * @param reqName the name of a particular component
     * @return the {@link ade.ADEGlobals.RecoveryState RecoveryState}
     * @throws RemoteException if the request fails
     * @exception AccessControlException if the password is bad or the component is
     * not accepting any more connections
     */
    @Override
    public ADEGlobals.RecoveryState requestState(
            String uid, // The username
            String upw, // The password
            String reqType,
            String reqName // The constraints to be met by the component
            ) throws AccessControlException, RemoteException {
        return requestState(uid, upw, reqType, reqName, null);
    }

    /**
     * Provides the means for an {@link ade.ADERegistry ADERegistry} to obtain
     * the execution status of an <tt>ADEComponent</tt> from another {@link
     * ade.ADERegistry ADERegistry}.
     *
     * @param uid the name of the user
     * @param upw the password for the user
     * @param reqType the type of component
     * @param reqName the name of a particular component
     * @param requester the origin of a connection request
     * @return the {@link ade.ADEGlobals.RecoveryState RecoveryState}
     * @throws RemoteException if the request fails
     * @exception AccessControlException if the password is bad or the component is
     * not accepting any more connections
     */
    @Override
    public ADEGlobals.RecoveryState requestState(
            String uid, // The username
            String upw, // The password
            String reqType,
            String reqName, // The constraints to be met by the component
            ADERegistry requester // the requesting registry
            ) throws AccessControlException, RemoteException {
        ADEComponentInfo s;
        ADEUser u;
        String vuid;
        String key;

        if (verbose || debugging || dbg > 5) {
            System.out.println(myID + ": " + uid + " requested state of component " + reqType + " with name " + reqName);
        }

        // don't check user name and password if the request comes from another
        // registry, we always allow those, otherwise make sure this user is
        // allowed to access the registry
        if (requester == null) {
            if ((u = verifyUser(uid, upw)) == null) {
                throw new AccessControlException("Bad username/password");
            }
        } else {
            //u = requester.verifyUser(uid, upw, this);
            try {
                // check whether the requester is the same registry as the current one...
                if (requester.equals(this)) {
                    u = verifyUser(uid, upw);
                } else {
                    u = (ADEUser) rctReg.remoteCall("verifyUser", requester, uid, upw, this);
                }
            } catch (ADEException ace) {
                throw new RemoteException("Could not verify " + uid, ace);
            }
        }
        vuid = u.getUID();
        key = getKey(reqType, reqName);

        // from this point, don't allow reqType or reqName to be "any"
        if (reqType.equals("") || reqType.equals("any")
                || reqName.equals("") || reqName.equals("any")) {
            throw new RemoteException(myID + ": requestState: Must supply component type and name (got " + key + ")");
        }

        // check for the Component about which they're enquiring
        if (componentTypes.containsKey(reqType)) {
            s = components.get(key);
            // if the key isn't in the components hash, we need to check
            // the other possible types/interfaces
            if (s == null) {
                HashMap<String, ADEComponentInfo> m =
                        (HashMap<String, ADEComponentInfo>) componentTypes.get(reqType);
                synchronized (m) {
                    s = m.get(reqName);
                }
            }
            // if we don't have the component, check the other registries; will
            // throw an exception if no component of that name is available
            if (s == null) {
                if (dbg > 5) {
                    System.out.println(myID + ": ref to " + key + " (for " + vuid + ") is null!");
                }
                if (requester == null) {
                    try {
                        // see if the other registires have it and pass it
                        return forwardRequestState(uid, upw, reqType, reqName);
                    } catch (RemoteException re) {
                        // something else happened, pass on the Exception
                        throw new RemoteException("Error occurred looking up the component state in a remote registry: " + re);
                    }
                }
                // request endpoint, no components here, so set the state to non-existent
                return ADEGlobals.RecoveryState.NONEXISTENT;
            }
            if (dbg > 5) {
                System.out.println(myID + ": have ref to " + s.getKey());
            }
            // we found a local component by that name
            return s.recState;

        } else {
            if (requester == null) {
                return forwardRequestState(uid, upw, reqType, reqName);
            } else {
                // request endpoint, no components here, so set the state to non-existent
                return ADEGlobals.RecoveryState.NONEXISTENT;
            }
        }
    }

    private ADEGlobals.RecoveryState forwardRequestState(
            String uid, // The username
            String upw, // The password
            String reqType,
            String reqName) throws RemoteException {
        ADEGlobals.RecoveryState ss;
        ensureRCTReg();
        for (ADERegistry r : registries.keySet()) {
            try {
                //ss = r.requestState(uid, upw, reqType, reqName, this);
                ss = (ADEGlobals.RecoveryState) rctReg.remoteCall(
                        "requestState", r, uid, upw, reqType, reqName, this);
                if (ss != null) {
                    return ss;
                }
            } catch (AccessControlException ace) {
                if (verbose || dbg > 0) {
                    System.out.println(myID + ": User " + uid + " does not have permission to access registry " + registries.get(r).name);
                }
                /*
                 * KRAMER } catch (RemoteException re) { // either component is not
                 * registered or registry is down if (verbose || dbg > 0) {
                 * System.out.println(myID +": Exception forwarding state
                 * request, registry may be down"); } // TODO: start the
                 * recovery process?
                 */
            } catch (Exception e) {
                if (verbose || dbg > 0) {
                    System.out.println(myID + ": Unexpected exception forwarding state request:");
                    System.out.println(e);
                }
            }
        }
        // request endpoint, no components here, so set the state to non-existent
        return ADEGlobals.RecoveryState.NONEXISTENT;
    }

    /**
     * checks if the given component key in use by this registry
     */
    // TODO: for now, this can only be used by registries, but should probably also be allowed for registered users...
    @Override
    public boolean isUsed(String key, Object credentials)
            throws AccessControlException, RemoteException {
        if (!(credentials instanceof ADERegistry)) {
            throw new AccessControlException("Only registries can call this method");
        }
        return (components.containsKey(key) || inrecovery.containsKey(key));
    }

    // local method for check if the key is in use either here or in any of the other registries
    private boolean isUsed(String key) {
        if (components.containsKey(key) || inrecovery.containsKey(key)) {
            return true;
        }
        try {
            for (ADERegistry ar : registries.keySet()) {
		// MS: FIXED: should use remote call
		//                if (ar.isUsed(key, this)) {
                if ((Boolean)rctReg.remoteCall("isUsed", ar, key, this)) {
                    return true;
                }
            }
        } catch (Exception e) {
            // something wrong, be conservative and assume the key might be in use...
            return true;
        }
        return false;
    }

    /**
     * Register a new component.
     *
     * @param si An {@link ade.ADEComponentInfo ADEComponentInfo} structure
     * @param passwd The password the registering component will use
     * @param forward whether this request should be forwarded to other
     * registries too
     * @return The name for the component (if no name was supplied, the new name)
     * @throws RemoteException If registration fails
     */
    @Override
    public String registerComponent(final ADEComponentInfo si, String passwd, final boolean forward)
            throws RemoteException {
        // note that there is a very specific sequence used to register a
        // component. This is very important when deregistering a component or
        // shutting down.
        // 1. check that the type is non-empty
        // 2. check that no other component by a specified name exists OR assign
        //   a name if none is specified
        // 3. update the component's host information with an entry from the
        //   knownhosts map after confirming it *is* a known host
        // 4. Put the component into the components hash (prevents other components
        //   from registering with the same name)
        // 5. Put the component into the componentTypes hash, using its actual
        //   class name (allows connections to be requested by classname)
        // 6. Put the component's interfaces into the componentTypes hash (using
        //   the "handleInterfaces" method; allows connections to be
        //   requested by interface name)
        // 7. Add the user to the user hash *if* a password is given (?!?)
        // 8. If the component is an ADERegistry, wait for one reaper cycle,
        //   then mutually register; mutual registration first calls
        //   "registerWithRegistry", then attempts to register the new
        //   component with all the other known registries (contained in the
        //   registries hash).
        ADEHostInfo sihi;
        System.out.println(myID + ": COMPONENT " + si.getKey() + " REPORTED FROM " + si.host + "PORT IS: "+ si.port);
        // make sure we don't reap it before it's even registered
        si.setConnectsCurrent(0);
        String type = si.type;
        // check if we have a valid type
        if (type.equals("")) {
            System.out.println(myID + ": REJECTED (invalid component type)");
            throw new RemoteException("Invalid component type!");
        }
        String name;
        // check if we need to generate a name
        // TODO: use the uniqueCounter value in ADEPreferences gensym
        if (si.name == null) {
            // get only the simple class name for the automatically generated name
            String reducedtype = type.substring(type.lastIndexOf('.') + 1);
            do {
                // create a name for the component, keep generating until unique
                name = ADEPreferences.gensym(reducedtype);
            } while (isUsed(getKey(type, name)));
            // now set the name
            si.name = name;
        } // trying to register with a chosen name, check if that ID is available
        else {
            name = si.name;
	    String testkey = getKey(type, name); 
	    // MS: FIXED: this should only be done for components, registries will be registered multiple times!!!
	    if ((si.isregistry && (components.containsKey(testkey) || inrecovery.containsKey(testkey))) || 
		(!si.isregistry && isUsed(testkey))) {
		System.out.println(myID + ": REJECTED (component already registered/key already in use.)");
		throw new RemoteException(getKey(type, name) + " already exists!");
	    }
        }

        // since this is called by si's object, of course si.as is set,
        // so we can go ahead and make the remote call
        if ((sihi = knownhosts.get(si.host)) != null) {
            try {
                rctServ.remoteCall("setHostInfo", si.as, sihi.duplicate(), this);
            } catch (Exception e) {
                System.err.println(myID + ": Cannot set " + getKey(type, name) + "'s host:\n" + e);
                e.printStackTrace();
            }
        } else {
            System.err.println(myID + ": " + name + " Registering with an unknown host!");
            throw new RemoteException("Registering with an unknown host!");
        }
        String key = si.getKey();
        ADEComponent as = si.as;

        // MS: check if the component has at least one group specified, otherwise set its default group
        // which is simply its keys
        if (si.groups.isEmpty()) {
            si.groups.add(key);
        }

        // components is a synchronizedMap; no need to synchronize
        //synchronized(components) {
        if (debugRegistration) {
            System.out.println(myID + ": >>>>>>>>>> putting " + key + " in components hash...");
            System.out.println(myID + ": >>>>>>>>>> si.as=[" + si.as + "]");
        }
        components.put(key, si);
        //}

        // the component type lookup (storing the components in a nameMap
        // makes returning all components of a specific type easier, in
        // addition to allowing us to easily associate the interfaces
        // a component implements with the component); we add the actual type
        // outside the thread below to be sure it doesn't fail
        if (debugRegistration) {
            System.out.println(myID + ": >>>> putting " + key + " in type/name maps");
        }

        // add the implemented interfaces as types
        handleInterfaces(si, true);

        if (debugRegistration) {
            System.out.println(myID + ": >>>>>>>>> adding " + key + " to heartbeats");
        }
        heartbeats.put(key, si);

        // add a new user, if requested, mkuser contains the password
        if (passwd != null) {
            // components always register with their myIDs, so they are always new
            ADEUser newuser = new ADEUser(key, passwd, si.userAccess);
            synchronized (userDB) {
                userDB.put(key, newuser);
            }
            if (debugging || dbg > 5) {
                System.out.println(myID + ": Added " + key + " to user database");
            }
        }
        if (verbose || debugRegistration) {
            System.out.println(myID + ": >>>>>>>>> Registered: " + si.toString());
        }

        // if the component is an ADERegistry, register with it after
        // a short delay unless we are already registered
        if (as instanceof ADERegistry) {
            if (!(registries.containsKey((ADERegistry) as)) && !as.equals(this)) {
                try {
                    final ADEComponentInfo s = si;
                    final ADERegistry ars = (ADERegistry) as;
                    final boolean allowshutdownbyotherregistriesfinal = this.allowshutdownbyregistries;
                    final ADERegistry me = this;
                    new Thread() {
                        @Override
                        public void run() {
                            String othreg, newreg;
                            try {
                                // sleep for at least as long as one reaper cycle (to ensure that the calling registry's registration processes finishes...)
                                // MS: this should be done differently... (should not rely on reaper timing...
                                //System.out.println(System.currentTimeMillis() + ">>>>>>>>>>>>>>>>>>>>>>>THREAD sleep " + myID);
                                Thread.sleep(ADEGlobals.DEF_RPPULSE);
                                newreg = (String) rctReg.remoteCall("getID", ars);

                                // from the above check, we know that we have to
                                // mutually register; if we succeed, we then
                                synchronized (registries) {
                                    if (registries.containsKey(ars)) {
                                        System.err.println("AFTER SLEEP, REGISTRY WAS IN THE HASHMAP SURPRISINGLY...");
                                    } else {
                                        if (registerWithRegistry(ars)) {
                                            // mark whether registry is allowed to shut us down
                                            s.shutdownbyotherregistries = allowshutdownbyotherregistriesfinal;
                                            if (dbg > 5 || debugRegistration) {
                                                System.out.println(System.currentTimeMillis() + "######### " + myID + " REGISTERED WITH " + newreg + " #######");
                                            }
                                            //synchronized(registries) {
                                            // if this succeeds, add it to the list of registries
                                            registries.put(ars, s);
                                            if (forward) {
                                                for (ADERegistry r : registries.keySet()) {
                                                    othreg = (String) rctReg.remoteCall("getID", r);
                                                    try {
                                                        if (!r.equals(ars)) {
                                                            //System.err.println(System.currentTimeMillis() + "@@@@@@@@@@@ in LOOP checking " + othreg);
                                                            if (dbg > 5 || debugRegistration) {
                                                                System.out.println(System.currentTimeMillis() + "######### " + myID + ": REQUESTING from " + othreg + " REGISTRATION with " + newreg + " ####### ");
                                                            }
                                                            //r.registerWithRegistry(ars,me);
                                                            try {
                                                                rctReg.remoteCall(
                                                                        "registerWithRegistry", r, ars, me);
                                                                if (dbg > 5 || debugRegistration) {
                                                                    System.out.println(System.currentTimeMillis() + "######### " + myID + ": " + othreg + "SUCESSFULLY REGISTERED WITH " + newreg + " #######\n");
                                                                }
                                                            } catch (ADEException ace) {
                                                                //System.out.println("=======> GOING TO SLEEP...");
                                                                try {
                                                                    Thread.sleep(ADEGlobals.DEF_RPPULSE);
                                                                } catch (java.lang.InterruptedException ie) {
                                                                    System.out.println("Got interrupted in registerWithRegistry... who did it???");
                                                                }
                                                                // now try again...
                                                                rctReg.remoteCall(
                                                                        "registerWithRegistry", r, ars, me);
                                                                if (dbg > 5 || debugRegistration) {
                                                                    System.out.println(System.currentTimeMillis() + "######### " + myID + ": " + othreg + "SUCESSFULLY REGISTERED WITH " + newreg + " #######\n");
                                                                }
                                                            }
                                                        }
                                                        //} catch (ADEException ace) {
                                                        //	System.err.println(myID +": Problem registering registry with other registry " + registries.get(r).name + ":\n" + ace);
                                                        //	ace.printStackTrace();
                                                    } catch (ADEException re) {
                                                        if (re.getCause() != null) {
                                                            Throwable t = re.getCause();
                                                            while (t.getCause() != null) {
                                                                t = t.getCause();
                                                            }
                                                            if (t instanceof ADEDuplicateRegistrationException) {
                                                                // let it go...
                                                            } else {
                                                                System.err.println(myID + ": Problem registering " + newreg + " with " + othreg + ":\n" + re.getCause());
                                                            }
                                                        } else {
                                                            System.err.println(myID + ": Problem registering " + newreg + " with " + othreg + ":\n" + re);
                                                        }
                                                    }
                                                }
                                            }
                                            // MS: this part stops the recursive forwarding...
                                            //else
                                            //    System.out.println("FORWARDING NOT REQUESTED...");
                                            //}
                                        } // tried to register with registry that we originally
                                        // registered with on startup, add to registry list
                                        else {
                                            //synchronized(registries) {
                                            // MS: not sure we should put the registry is as something went wrong....
                                            registries.put(ars, s);
                                        }
                                    }
                                }
                                // this is thrown when the registry already was registered as the standard AR..., so just move on
                            } catch (AccessControlException ace) {
                                registries.put(ars, s);
                            } catch (RemoteException re) {
                                if (re.getCause() != null) {
                                    Throwable t = re.getCause();
                                    while (t.getCause() != null) {
                                        t = t.getCause();
                                    }
                                    if (t instanceof ADEDuplicateRegistrationException) {
                                        // let it go; we're already registered
                                    } else {
                                        System.err.println(myID + ": Could not register with " + s.toString() + ":\n" + re.getCause());
                                    }
                                } else {
                                    System.err.println(myID + ": Could not register with " + s.toString() + ":\n" + re);
                                }
                            } catch (Exception e) {
                                System.err.println(myID + ": Could not register with " + s.toString() + ":\n" + e);
                            }
			    // MS: SHOULD REGLOCK be removed
			    reglock.unlock();
                        }  // end run
                    }.start();
                } catch (Exception e1) {
                    System.err.println(myID + ": Problem during registration of " + si.toString() + " with other registries:\n" + e1);
                    // MS: SHOULD REGLOCK be removed
                    reglock.unlock();
                }
            }
            //else
            //	System.out.println(System.currentTimeMillis() + "------------------------ old registry in registerComponent in " + myID);
        } 
	// the component is not a registry, so check whether logging has to be turn on and notifications need to be sent
	else {
            // start logging in late arriving components
            if (loggingrequest) {
                // si.as.setLocalLogging(this,loggingrequest);
                try {
                    rctServ.remoteCall("setLocalLogging", si.as, this, loggingrequest);
                } catch (ADEException ace) {
                    System.err.println("Could not set local logging to " + loggingrequest + " due to " + ace);
                    ace.printStackTrace();
                }
            }
            // MS: should REGLOCK be removed
            reglock.unlock();
	    if (debugRegistration || dbg > 5) {
		System.out.println(myID + ": almost done with registration of " + key + " -- finalizing notifications\n");
	    }
	    // MS: the notifications should be quick because they will spawn a notification thread in each component 
	    // to not hold up the registry
	    synchronized (notifications) {
		final String newcomponentkey = si.getKey();
		for (final Map.Entry<ADEComponent, String[][]> entry : notifications.entrySet()) {
		    String[][] constraints = entry.getValue();
		    //System.out.println("CHECKING " + entry.getKey());
		    if (meetsCriteria(si, constraints)) {
			//System.out.println("Component meets constraints, great!");
			// MS: use non-blocking calls through the rct instead of using an additional thread
			try {
			    rctServ.remoteCallNonBlocking("notifyComponentJoinedRegistry", entry.getKey(), newcomponentkey, this);
			} catch (Exception e) {
			    System.err.println("Problem notifying component of new component " + newcomponentkey + " joining.");
			    if (e.getCause() != null) {
				e.getCause().printStackTrace();
			    } else {
				e.printStackTrace();
			    }
			}
		    } else {
			//System.out.println("Component does not meet constraints, sorry.");
		    }
		}
	    }
        }
        // done
	if (debugRegistration || dbg > 5) {
            System.out.println(myID + ": completed registration of " + key + "\n");
        }
        // return the name of the component
        return name;
    }

    /**
     * Add/remove the interfaces a component implements to/from the component
     * type/name hashes. We do this in a thread so as to not hold up other
     * processing due to the (potentially numerous) synchronization.
     *
     * @param si The {@link ade.ADEComponentInfo ADEComponentInfo} for the component
     * @param add If <tt>true</tt>, add the interfaces; if <tt>false</tt> remove
     * them
     */
    private final void handleInterfaces(final ADEComponentInfo s0, final boolean add) {
        if (dbg > 5 || debugRegistration) {
            System.out.println(myID + ": in handleInterfaces(" + s0.getType() + "," + add + ")");
        }
        // don't process interfaces for registry
        if (s0.as instanceof ADERegistry) {
            return;
        }
        // MS: this should NOT be done in a thread, because we want the component's interface
        // to be ready and all determined when registration has finished...
//        new Thread() {
//            @Override
//            public void run() {
        String skey = s0.getKey();
        String sname = s0.getName();
        HashSet<String> ifaces = s0.interfaces;
        HashMap<String, ADEComponentInfo> nameMap;

        // add/remove the interfaces to/from the type/name maps
        if (dbg > 5 || debugRegistration) {
            System.out.println(myID + ": starting handleInterfaces.run()");
        }
        for (String fullname : ifaces) {
            if (dbg > 5 || debugRegistration) {
                System.out.println(myID + ": checking for interface " + fullname);
            }
            if (!componentTypes.containsKey(fullname)) {
                if (debugRegistration) {
                    System.out.println(myID + ": did not find componentType " + fullname);
                }
                if (add) {
                    nameMap = new HashMap<String, ADEComponentInfo>();
                    //nameMap.put(skey, s0);
                    nameMap.put(sname, s0);
                    // componentTypes is a synchronizedMap; no need
                    //synchronized(componentTypes) {
                    componentTypes.put(fullname, nameMap);
                    //}
                    if (debugRegistration) {
                        System.out.println(myID + ": added componentType " + fullname + ", " + skey);
                    }
                } else {
                    // if we didn't find it, we don't have to do anything
                }
            } else {
                if (debugRegistration) {
                    System.out.println(myID + ": found componentType " + fullname + "; " + (add ? "adding to" : "removing from") + " nameMap");
                }
                nameMap = componentTypes.get(fullname);
                if (nameMap != null) {
                    synchronized (nameMap) {
                        if (add) {
                            //nameMap.put(skey, s0);
                            nameMap.put(sname, s0);
                        } else {
                            //nameMap.remove(skey);
                            nameMap.remove(sname);
                        }
                    }
                    if (debugRegistration) {
                        System.out.println(myID + ": " + (add ? "added" : "removed") + " componentType " + fullname + ", " + sname);
                    }
                }
            }
        }
//            } // end run()
//        }.start();
    }

    /**
     * Register this {@link ade.ADERegistry ADERegistry} with another at the
     * behest of an already registered registry. This method lets
     * {@link ade.ADERegistryImpl ADERegistryImpl}s maintain a fully connected
     * graph, propagating new registrations.
     *
     * @param newreg The new {@link ade.ADERegistry ADERegistry}
     * @param requester An already registered {@link ade.ADERegistry ADERegistry}
     * @throws RemoteException If registration fails
     * @throws AccessControlException If the requesting registry is not in the
     * registry hashmap
     */
    @Override
    public final void registerWithRegistry(ADERegistry newreg, ADERegistry requester)
            throws RemoteException, AccessControlException {
        if (dbg > 5) {
            System.out.println("############# REGISTRATION REQUESTED ##########");
        }
        if (!registries.containsKey(requester)) {
            if (dbg > 5) {
                System.out.println(myID + ": ######## REQUESTER IS NOT AUTHORIZED ########");
            }
            throw new AccessControlException("Requesting registry not authorized");
        } else {
            if (dbg > 5) {
                System.out.println(myID + ": ######## REQUESTER AUTHORIZED #########");
            }
            // check whether registry is already registered and just skip it in that case
            if (!(registries.containsKey(newreg))) {
                registerWithRegistry(newreg);
                try {
                    // need to add newreg to the local registries
                    registries.put(newreg, components.get((String) rctReg.remoteCall("getID", newreg)));
                } catch (ADEException ate) {
                    System.err.println(System.currentTimeMillis() + "--------------------> " + myID + " could not get ADEComponentInfo.  Not added to registries..." + ate);
                }
            }
            /*
             else
             try {
             System.err.println(System.currentTimeMillis() + "--------------------> " + myID + " already registered with " + (String)rctReg.remoteCall("getID", newreg));
             } catch(Exception e) {
             System.err.println(System.currentTimeMillis() + "--------------------> " + myID + " already registered with newreg (problem getting name...)");
             }
             */
        }
    }

    /**
     * A component is shutting down of its own accord and is informing the
     * registry. This will cleanly and completely remove the component from the
     * registry. NOTE: this can usually only be called by the component itself, as
     * nobody else has the component's unique, dynamically created password (except
     * this registry and, possibly, registry admins looking it up). If a component
     * disappears (i.e., stops sending a heartbeat), it has failed; the
     * RegistryReaper will then attempt to restart the component (given appropriate
     * settings).
     *
     * @param sID the component id
     * @param passwd the password
     * @exception RemoteException if the remote invocation fails
     */
    @Override
    public void deregisterComponent(String sID, String passwd)
            throws RemoteException {
        if (!components.containsKey(sID)) {
            System.out.println(myID + ": UNKNOWN " + sID + " trying to deregister");
            throw new RemoteException("Unknown (or disconnected) component:" + sID);
        }

        if (verifyUser(sID, passwd) != null) {
            if (debugRegistration || dbg > 5) {
                System.out.println(myID + ": COMPONENT " + sID + " DEREGISTERING");
            }
            ADEComponentInfo asi = components.get(sID);
            if (asi != null) {
                // this will make the component unavailable, both for connections
                // and for updates, setting the state and removing pending updates
                deregisterComponent(asi);
                // remove component in thread which takes time and would hold up the component
                final ADEComponentInfo s0 = asi;
                new Thread() {
                    @Override
                    public void run() {
                        removeComponent(s0);
                    }
                }.start();
            } else {
                System.err.println(myID + ": missing info for " + sID);
            }
        } else {
            // this is really an invalid password...
            throw new RemoteException("Invalid component name or password");
        }
    }

    /**
     * Perform a partial removal of the component from the registry, making the
     * component unavailable (i.e., unable to provide services). Note that this
     * method will <b>not</b> stop the heartbeat, allowing recovery to proceed.
     */
    private void deregisterComponent(ADEComponentInfo s) {
        // so as not to conflict with the "registerComponent", "requestConnection",
        // or "requestConnectionAll" methods, this needs to be done in a very
        // particular order. "registerComponent" will reject any component whose
        // type$name exists as a key, which should be preserved until the component
        // is totally removed. "requestConnection" uses the componentTypes map for
        // a lookup, which allows us to synchronize on a single component type at
        // a time and locate one component that will provide a connection.
        // "requestConnectionAll" also uses the componentTypes map, but requests
        // a connection for every component in the secondary map.
        // 1. Set the component's state
        // 2. Remove the component's entry from all of the secondary maps in
        //   the componentTypes map (making it unavailable for requests)
        // 3. Remove the user from the userDB map (thereby rejecting any updates
        //   sent; the component will have to re-register)
        // 4. Remove the component's pending updates
        // Note that the component is *not* removed from the components nor the
        // heartbeats maps.
        s.setState(ADEGlobals.ComponentState.DEREGISTER);
        s.setRecoveryState(ADEGlobals.RecoveryState.DOWN);
        // TODO: not necessary, but if a component is deregistering itself,
        // we could remove the heartbeat and notify any connected components
        // of the deregistration...
        debugPrint("COMPONENT " + s + " DEREGISTERING &&&&&&&&&&&&&&&&&&&&");
        // the component type lookup; little more complicated, since
        // the type keys to another map that has to be checked and
        // possibly removed.
        // I don't think it makes sense to remove the type entry
        // if all named instances are gone; the memory overhead
        // isn't much, the processing time would go up (admittedly,
        // not by much), but it shouldn't affect operation anyway.
        debugPrint("\tRemoving from componentTypes...");
        String sKey = s.getKey();
        String sName = s.getName();
        String sType = s.getType();
        if (componentTypes.containsKey(sType)) {
            Map m = (Map) componentTypes.get(sType);
            synchronized (m) {
                //if (m.containsKey(sKey)) {
                //	m.remove(sKey);
                if (m.containsKey(sName)) {
                    m.remove(sName);
                }
            }
        }
        // remove from notifications so that there will be no more attempts to notify the component
        notifications.remove(s.as);

        // we also need to remove the interfaces the component implements;
        // do it in a thread so as to not hold up processing...
        if (debugRegistration) {
            System.out.println(myID + ": removing interfaces of " + sType + " from type/name maps");
        }
        handleInterfaces(s, false);

        // remove it from the user database
        if (userDB.containsKey(sKey)) {
            synchronized (userDB) {
                userDB.remove(sKey);
            }
        }

        // rid ourselves of any pending updates
        synchronized (pendingupdates) {
            pendingupdates.remove(sKey);
        }
    }

    /**
     * Try to start an <tt>ADEComponent</tt>. This method can be called from
     * multiple places, depending on circumstances: <ul> <li>The
     * <tt>startupADE</tt> method, if a configuration file was specified</li>
     * <li>The <tt>startComponent</tt> method, if the component information is coming
     * from an external source</li> <li>A <tt>RecoveryThread</tt> (via
     * <tt>attemptRecoveryComponentDown</tt>), if the component failed and is supposed
     * to be restarted</li> </ul> A new operating system process (with a new
     * Java VM) will be started using an
     * {@link ade.ADELocalProcessStarter ADELocalProcessStarter}, which is then
     * stored as the value in the <tt>procs</tt> map (the key is the component's
     * ID, in <tt>type$name</tt> format). Note that if the component has <i>state
     * maintenance</i> activated and this registry has a non-null
     * {@link ade.ADEState ADEState} object associated with the component, the
     * {@link ade.ADEComponentImpl#loadState loadState} method will be called.
     *
     * @param myInfo This registry's information
     * @param si The component's information
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    private boolean attemptStartComponent(ADEComponentInfo myInfo, ADEComponentInfo si)
            throws Exception {

        ADEHostInfo hi, myHost = requestHostInfo(this);
        ADELocalProcessStarter alp;
        long startTime = 0;
        if (debugTimes) {
            startTime = System.currentTimeMillis();
        }
        if (verbose || debugging || dbg > 3) {
            System.out.print(myID + ": attempting to start " + si.type);
            System.out.println(" on " + si.host + " from " + myHost.hostip);
            //si.print();
        }

        // If running out of a JAR, will need to add the jar path (
        //     essentially just the registry's path) so that can run configuration
        //     files or run components via the GUI.
        addRegistryClassPathToComponent(myHost, si);



        /* this needs to be completely rewritten
         - generate list of available hosts that are reachable and in the onlyonhostlist based on knownhosts
         (this could be extended so that hosts have to meet additional criteria, could call the reasoner to generate the list)
         - select a host from the list according to some policy (could call the reasoner to do it)
         - without reasoner the default policy is to
         - start with the components hosts if it is in that list and attempt to move the component there
         - go through the remaining list of eligible hosts
         - give up only when no host remains
         the default policy could be extended to allow for load balancing
         */


        // remove the component from the host list of components on it
        if ((hi = knownhosts.get(si.host)) != null) {
            hi.removeComponent(si.getKey()); // if there, replace; if not, no worries
        }
        // check if the component's host is either not in knownhosts, or if it is, whether it is not reachable, then we need a new one
        if (verbose || debugging) {
            System.out.print(myID + ": Checking availability...");
        }
        if (hi == null || !myHost.reachable(si)) {
            if (verbose || debugging) {
                System.out.println("Current host of component is unreachable, looking for new host for " + si.name);
            }
            // need to find a different host for the component...
            // pick a  host from the components onlyonhost list, unless it is empty, then any host is fine
            // TODO: make sure that the registry's hostlist and that of the component are consistent if the component
            // was manually started...
            boolean foundone = false;
            for (String hn : si.onlyonhosts) {
                // replace the current host by the new one
                if ((hi = knownhosts.get(hn)) != null && myHost.reachable(hi.hostip)) {
                    si.host = hi.hostip;
                    foundone = true;
                    break;
                }
            }
            // no eligible host...
            if (!foundone) {
                System.err.println("Could not find any eligible host for component " + si.name);
                return false;
            }
        }

        // if we get here, we found a host
        if (debugTimes) {
            System.out.println(myID + ": recovery of " + si.name + "-availability recovery " + (System.currentTimeMillis() - startTime) + "ms");
        }
        if (verbose || debugging) {
            System.out.print("OK.\n\tChecking hosts/devices...");
        }
        //hi.print();
        //myHost.print();
        //si.print();
        if (hi.canStartComponent(si)) {
            if (debugTimes) {
                System.out.println(myID + ": recovery of " + si.name + "-canstart recovery " + (System.currentTimeMillis() - startTime) + "ms");
            }
            if (verbose || debugging) {
                System.out.println("OK. Calling start...");
            }
            si.registryIP = myHost.hostip;
            si.registryType = myInfo.type;
            si.registryName = myInfo.name;
            si.registryPort = myInfo.port;
            try {
                if ((alp = myHost.startComponent(hi, si)) != null) {
                    //System.out.println("JACK: Give Me your STDOUT AND ERR PLEASE");
                    procs.put(si, alp);
                    if (debugTimes) {
                        System.out.println(myID + ": recovery of " + si.name + "-started recovery " + (System.currentTimeMillis() - startTime) + "ms");
                    }

                    return true;
                } else {
                    System.err.println(myID + ": Could not start " + si + "; trying other hosts...");
                }
            } catch (IllegalThreadStateException itse) {
            }
        } else {
            System.err.println(myID + ": Not allowed or lacking devices on " + si.host + "; trying elsewhere...");
        }
        // if we got here, the component couldn't be started on its original
        return false;
    }

    private void addRegistryClassPathToComponent(ADEHostInfo myHost, ADEComponentInfo si) {
        // If running out of a JAR, will need to add the jar path so that can run 
        //     configuration files or run components via the GUI.  JAR-based component 
        //     launching does not work otherwise!
        String registryClassFile = "ade/ADERegistryImpl.class";
        String registryClassURL = getClass().getClassLoader().getResource(registryClassFile).toString();
        if (registryClassURL.startsWith("jar")) {
            String registryClassJar = registryClassURL.substring(0, registryClassURL.indexOf("!"));
            registryClassJar = registryClassJar.substring(registryClassJar.lastIndexOf(myHost.filesep) + 1);
            //System.out.println("Registry jar: "+registryClassJar);
            if ((si.userclasspath == null) || (si.userclasspath.trim().length() == 0)) {
                si.userclasspath = registryClassJar;
            } else {
                si.userclasspath = registryClassJar + myHost.pathsep + si.userclasspath;
            }
        }
    }

    /**
     * Start an {@link ade.ADEComponent ADEComponent}, based on the information in an
     * {@link ade.ADEComponentInfo ADEComponentInfo} structure.
     *
     * @param aid the administrator id
     * @param apw the administrator password
     * @param si information about the component
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     * @throws Exception
     */
    @Override
    public boolean startComponent(String aid, String apw, ADEComponentInfo si)
            throws RemoteException, AccessControlException, Exception {
        if (!verifyAdmin(aid, apw)) {
            throw new AccessControlException("Bad admin username/password.");
        }
        ADEComponentInfo myInfo = requestComponentInfo(this);
        return attemptStartComponent(myInfo, si);
    }

    /**
     * Update the heartbeat of an {@link ade.ADEComponent ADEComponent}.
     *
     * @param myID the ID string of the component (<tt>type$name</tt>)
     * @param p the new period
     * @throws RemoteException if the request fails
     */
    @Override
    public void updateHeartBeatPeriod(
            String myID,
            int p) throws RemoteException {
        String sType, sName;
        ADEComponentInfo s;

        // setup processing
        if (debugReaper) {
            System.out.println(myID + " updating...");
        }
        sType = myID.substring(0, myID.lastIndexOf('$'));
        sName = myID.substring(myID.lastIndexOf('$') + 1);
        if (debugReaper) {
            System.out.println("Type=" + sType + "; Name=" + sName);
        }

        // Make sure we have a component of that name and type
        if (!components.containsKey(myID)) {
            debugPrint("UNKNOWN component update: " + myID);
            throw new RemoteException("Unknown (or disconnected) component.");
        }
        s = components.get(myID);
        if (!heartbeats.containsKey(s.getKey())) {
            debugPrint("Component " + s.toString() + " missing from heartbeat hash");
            return;
        }
        debugPrint("Component " + s.toString() + " setting HB to " + p);
        s.setHeartBeatPeriod(p);
        if (myReaper.getPeriod() > p) {
            if (debugReaper) {
                System.out.println("Updating reaper's period to " + p);
            }
            myReaper.setPeriod(p);
        }
    }

    /**
     * Update an {@link ade.ADEComponent ADEComponent}'s status, including one of the
     * enumeration constants from {@link ade.ADEGlobals.ComponentState}, an
     * {@link java.util.ArrayList ArrayList} of component names to which the component
     * has connected, and an {@link java.util.ArrayList
     * ArrayList} of component names that have obtained connections to the component.
     *
     * @param amsi component information object
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    @Override
    public void updateStatus(ADEMiniComponentInfo amsi)
            throws RemoteException, AccessControlException {
        // this needs to be fast and just return, as it gets called by
        // every component; put any and all component bookkeeping in the reaper
        String uID = amsi.id, sType, sName;
        ADEComponentInfo aci;

        // TODO: should the componentservs be a Set (to correspond with
        // heartbeats.keySet())?
        int conns = amsi.clients.size();
        // TODO: want to adjust the reaper period to match the heartbeat...
        // setup processing
        if (dbg > 8 || debugReaper) {
            StringBuilder sb = new StringBuilder(myID);
            sb.append(": ");
            sb.append(uID);
            sb.append(" updating status: state=");
            sb.append(amsi.state);
            sb.append(", ");
            sb.append(conns);
            sb.append(" connects");
            System.out.println(sb.toString());
        }
        // Make sure we have a component of that name and type
        if ((aci = components.get(uID)) == null) {
            if (dbg > 6 || debugging) {
                System.out.println(myID + ": UNKNOWN component update: " + uID + "...");
            }
            if ((aci = inrecovery.get(uID)) != null) {
                if (dbg > 8 || debugging) {
                    System.out.println("\tInvalidated client attempting to reconnect; sending shutdown...");
                }
                try {
                    // aci.as.requestShutdown(this);
                    rctServ.remoteCall("requestShutdown", aci.as, this);
                    // } catch(RemoteException re) {
                } catch (ADEException ace) {
                    if (dbg > 8 || debugging) {
                        System.out.println("Problem shutting down component " + aci.name);
                    }
                }
            }
            throw new RemoteException("Unknown component " + uID);
        }
	// we hage the component
        if (!heartbeats.containsKey(aci.getKey())) {
            // TODO: possibly store this fact somewhere?
            if (dbg > 8 || debugging) {
                System.out.println(myID + ": Component " + aci.toString() + " missing from heartbeat hash");
            }
            return;
        }
        // immediately set the fields used during connection requests and state
        // notification (the #connections, userAcess set, and state)
        // this will also update the checkin time (for the reaper)
        if (dbg > 6 || debugging) {
            System.out.println(myID + ": got update from " + uID + "...");
        }
	// MS: added the update of the clients and the components in the ACI
	aci.clients = amsi.clients;
	aci.components = amsi.components;
        aci.setConnectsCurrent(conns);
        aci.setState(amsi.state); // set this right away (don't wait for reaper)
        pendingupdates.put(amsi.id, amsi);
    }

    /**
     * Change the period of time required to pass before the registry starts
     * recovery procedures for an {@link ade.ADEComponent ADEComponent} from the
     * standard one {@link ade.ADEGlobals#DEF_RPPULSE ReaperPeriod} to some
     * multiple of the reaper period. Note that the component will be unavailable
     * during this time, so careful consideration of the effects on the
     * application are required.
     *
     * @param sid the component's identification string
     * @param spw the component's password
     * @param mult the number of reaper periods that must pass before initiating
     * recovery
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    @Override
    public void setRecoveryMultiplier(String sid, String spw, int mult)
            throws RemoteException, AccessControlException {
        ADEComponentInfo s;

        if (!components.containsKey(sid)) {
            debugPrint("UNKNOWN component " + sid + " trying to modify recovery multiplier");
            throw new RemoteException("Unknown (or disconnected) component:" + sid);
        }
        if (verifyUser(sid, spw) == null) {
            throw new RemoteException("Invalid component name or password");
        }
        if (mult < 1) {
            throw new RemoteException("Multiplier must be greater than 0");
        }
        s = components.get(sid);
        if (!heartbeats.containsKey(s.getKey())) {
            if (dbg > 8 || debugging) {
                System.out.println(myID + ": Component " + s.toString() + " missing from heartbeat hash");
            }
            return;
        }
        s.setRecoveryMultiplier(mult);
    }

    /**
     * Add a user and the types of {@link ade.ADEComponent ADEComponent} to which the
     * user is allowed access to the user database.
     *
     * @param aid the administrator's ID
     * @param apw the administrator's password
     * @param uid the user's ID
     * @param upw the user's password
     * @param acc a {@link java.util.HashSet HashSet} of component types to which
     * the user is granted access
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    @Override
    public boolean addUser(
            String aid,
            String apw,
            String uid,
            String upw,
            HashSet<String> acc) throws RemoteException, AccessControlException {
        // confirm admin's rights
        if (!verifyAdmin(aid, apw)) {
            throw new AccessControlException("Admin rights not granted for " + aid);
        }
        // confirm no one with that id exists
        if (userDB.get(uid) != null) {
            throw new RemoteException(uid + " already taken");
        }

        // create the new user
        ADEUser newuser = new ADEUser(uid, upw, false, false, acc);
        synchronized (userDB) {
            userDB.put(uid, newuser);
        }
        return (userDB.get(uid) != null);
    }

    /**
     * Add an administrator to the user database.
     *
     * @param aid the administrator's ID who already has access
     * @param apw the administrator's password who already has access
     * @param uid the new administrator ID
     * @param upw the new administrator password
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    @Override
    public boolean addAdmin(
            String aid, // Administrator username
            String apw, // Administrator password
            String uid, // New username
            String upw // New password
            ) throws RemoteException, AccessControlException {
        // confirm admin's rights
        if (!verifyAdmin(aid, apw)) {
            throw new AccessControlException("Admin rights not granted for " + aid);
        }
        // confirm no one with that id exists
        if (userDB.get(uid) != null) {
            throw new RemoteException(uid + " already taken");
        }

        // add the new administrator
        ADEUser newadmin = new ADEUser(uid, upw, true, false);
        synchronized (adminDB) {
            adminDB.put(uid, newadmin);
        }
        return (adminDB.get(uid) != null);
    }

    /**
     * Delete a user from the user database.
     *
     * @param aid the administrator's ID
     * @param apw the administrator's password
     * @param uid the user ID to remove
     * @return <tt>true</tt> on success, <tt>false</tt> otherwise
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the administrator does not have
     * adequate permissions
     */
    @Override
    public boolean delUser(
            String aid,
            String apw,
            String uid) throws RemoteException, AccessControlException {
        if (!verifyAdmin(aid, apw)) {
            throw new AccessControlException("Admin rights not granted for " + aid);
        }
        // delete the user; if not existent, remove just returns null
        try {
            synchronized (userDB) {
                userDB.remove(uid);
            }
        } catch (NullPointerException e) {
            if (debugReaper) {
                System.out.println("NULL POINTER EXCEPTION IN REGISTRY ON LOOKUP");
            }
        }
        return (userDB.get(uid) == null);
    }

    // delete Self
    /**
     * Allow a user to remove themselves from the user database.
     *
     * @param uid the user's ID
     * @param upw the user's password
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    @Override
    public boolean delSelf(String uid, String upw) throws RemoteException, AccessControlException {
        if (verifyUser(uid, upw) == null) {
            throw new AccessControlException("User rights not granted for " + uid);
        }
        // delete the user; if not existent, remove just returns null
        try {
            synchronized (userDB) {
                userDB.remove(uid);
            }
        } catch (NullPointerException e) {
            if (debugging) {
                System.out.println("NULL POINTER EXCEPTION IN REGISTRY ON LOOKUP");
            }
        }
        return (userDB.get(uid) == null);
    }

    /**
     * Delete an administrator from the user database.
     *
     * @param aid the administrator's ID who already has access
     * @param apw the administrator's password who already has access
     * @param uid the administrator ID to remove
     * @return <tt>true</tt> on success, <tt>false</tt> otherwise
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    @Override
    public boolean delAdmin(
            String aid, // Administrator username
            String apw, // Administrator password
            String uid // Administrator to delete
            ) throws RemoteException, AccessControlException {
        // confirm admin's rights
        if (!verifyAdmin(aid, apw)) {
            throw new AccessControlException("Admin rights not granted for " + aid);
        }
        // delete the admin; if not existent, remove just returns null
        synchronized (adminDB) {
            adminDB.remove(uid);
        }
        return (adminDB.get(uid) != null);
    }

    /**
     * Modify a user's entry in the user database. Note that there is no
     * parameter to change the <tt>isComponent</tt> value, which can only be set
     * when an {@link ade.ADEComponent ADEComponent} registers.
     *
     * @param aid The administrator's username
     * @param apw The administrator's password
     * @param uid The username to modify
     * @param nad Whether the user has administration rights (null to leave
     * as-is)
     * @param nuid The new username of the user (null to leave as-is)
     * @param nupw The new password of the user (null to leave as-is)
     * @param nacclvl a {@link java.util.HashSet HashSet} containing the names
     * of components to which the user is granted access (null to leave as-is)
     * @return <tt>true</tt> on success, <tt>false</tt> otherwise
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    @Override
    public boolean modUser(
            String aid,
            String apw,
            String uid,
            boolean nad,
            String nuid,
            String nupw,
            HashSet<String> nacclvl) throws RemoteException, AccessControlException {
        ADEUser temp;

        // check admin's rights
        if (!verifyAdmin(aid, apw)) {
            throw new AccessControlException("Admin rights not granted for " + aid);
        }
        // check there is such a user
        if (!userDB.containsKey(uid)) {
            return false;
        }
        if (nuid == null) {
            // no need to lock, as hash won't change
            temp = (ADEUser) userDB.get(uid);
        } else {
            // lock the userDB and make the changes
            synchronized (userDB) {
                temp = (ADEUser) userDB.remove(uid);
                temp.setUID(nuid);
                userDB.put(nuid, temp);
            }
        }
        if (nupw != null) {
            temp.setPassword(nupw);
        }
        temp.setAdmin(nad);
        if (nacclvl != null) {
            temp.setUserAccess(nacclvl);
        }
        return true;
    }

    // NOTE: not in ADERegistry interface!
    /**
     * Write the user database out to the default file.
     *
     * @param aid the administrator's ID
     * @param apw the administrator's password
     * @return <tt>true</tt> on success, <tt>false</tt> otherwise
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    public boolean dumpUDB(
            String aid,
            String apw) throws RemoteException, AccessControlException {
        return dumpUDB(aid, apw, null);
    }

    /**
     * Write the user database out to a file.
     *
     * @param aid the administrator's ID
     * @param apw the administrator's password
     * @param file the name of the file
     * @return <tt>true</tt> on success, <tt>false</tt> otherwise
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    @Override
    public boolean dumpUDB(
            String aid,
            String apw,
            String file) throws RemoteException, AccessControlException {
        if (!shuttingDown) {
            if (!verifyAdmin(aid, apw)) {
                throw new AccessControlException("Bad username/password.");
            }
        }
        if (file == null) {
            file = new String("default.udb");
        }

        FileOutputStream ostream;
        ObjectOutputStream p;

        try {
            ostream = new FileOutputStream(file);
        } catch (Exception e) { // FileNotFoundException
            if (debugging) {
                System.err.println("Could not open file \"" + file + "\" to save the user database");
            }
            return false;
        }
        try {
            p = new ObjectOutputStream(ostream);
        } catch (Exception e) { // IOException
            if (debugging) {
                System.err.println("Could not write to file \"" + file + "\" to save the user database: " + e);
            }
            try {
                ostream.close();
            } catch (Exception e2) {
                if (debugging) {
                    System.err.println("Could not close the file \"" + file + "\" after failing to write to it: " + e2);
                }
            }
            return false;
        }
        try {
            //System.out.println("Attempting to write userDB");
            // well, this doesn't work
            // p.writeObject(userDB);
            // get users, write out number of them
            ADEUser u;
            Collection users = userDB.values();
            p.writeInt(users.size());
            // iterate over collection, writing each one
            Iterator uiter = users.iterator();
            while (uiter.hasNext()) {
                u = (ADEUser) uiter.next();
                p.writeObject(u.getUID());
                p.writeObject(u.getPwd());
                p.writeObject(u.getAllowances());
            }
        } catch (Exception e) {
            if (debugging) {
                System.err.println("Could not write objects to the opened stream to save the user database: " + e);
            }
            try {
                ostream.close();
            } catch (Exception e2) {
                if (debugging) {
                    System.err.println("Could not close the file \"" + file + "\" after failing to write to the stream: " + e2);
                }
            }
            return false;
        }
        try {
            p.flush();
        } catch (Exception e) {
            if (debugging) {
                System.err.println("Could not flush the opened file to disk to save the user database: " + e);
            }
            try {
                ostream.close();
            } catch (Exception e2) {
                if (debugging) {
                    System.err.println("Could not close the file \"" + file + "\" after failing to flush it to disk: " + e2);
                }
            }
            return false;
        }
        try {
            ostream.close();
        } catch (Exception e) {
            if (debugging) {
                System.err.println("Could not close the file \"" + file + "\" to save the user database: " + e);
            }
            return false;
        }
        return true;
    }

    /**
     * Read the user database in from a file.
     *
     * @param aid the administrator's ID
     * @param apw the administrator's password
     * @param file the name of the file (if null, uses <tt>default.udb</tt>)
     * @return <tt>true</tt> on success, <tt>false</tt> otherwise
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    @Override
    public boolean loadUDB(
            String aid,
            String apw,
            String file) throws RemoteException, AccessControlException {
        if (!verifyAdmin(aid, apw)) {
            throw new AccessControlException("Bad username/password");
        }
        if (file == null) {
            file = new String("default.udb");
        }

        FileInputStream istream;
        ObjectInputStream p;

        try {
            istream = new FileInputStream(file);
        } catch (Exception e) { // FileNotFoundException
            if (debugging) {
                System.err.println("Could not open file \"" + file + "\" to load the user database");
            }
            return false;
        }
        try {
            p = new ObjectInputStream(istream);
        } catch (Exception e) { // IOException
            if (debugging) {
                System.err.println("Could not get an input stream from \"" + file + "\" to load the user database: " + e);
            }
            try {
                istream.close();
            } catch (Exception e2) {
                if (debugging) {
                    System.err.println("Could not close the file \"" + file + "\" after failing to read from it: " + e2);
                }
            }
            return false;
        }
        try {
            // old (non-working) method; changed to match writeObject()
            //userDB = (Map) p.readObject();
            ADEUser u;
            String uid;
            String upw;
            HashSet<String> uallow;
            int numUsers = p.readInt();
            for (int i = 0; i < numUsers; i++) {
                uid = (String) p.readObject();
                upw = (String) p.readObject();
                uallow = (HashSet<String>) p.readObject();
                u = new ADEUser(uid, upw, uallow);
                userDB.put(uid, u);
            }
        } catch (Exception e) {
            if (debugging) {
                System.err.println("Could not read objects from the opened stream to load the user database: " + e);
            }
            try {
                istream.close();
            } catch (Exception e2) {
                if (debugging) {
                    System.err.println("Could not close the file \"" + file + "\" after failing to read from the stream: " + e2);
                }
            }
            return false;
        }
        try {
            istream.close();
        } catch (Exception e) {
            if (debugging) {
                System.err.println("Could not close the file \"" + file + "\" after reading in the user database: " + e);
            }
            return false;
        }
        return true;
    }

    /**
     * Get an {@link java.util.ArrayList ArrayList} of the users currently in
     * the user database.
     *
     * @param aid the administrator's ID
     * @param apw the administrator's password
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    @Override
    public ArrayList<ArrayList<String>> listUDB(String aid, String apw)
            throws RemoteException, AccessControlException {
        Collection users;
        Iterator uiter;
        ADEUser u;
        ArrayList<ArrayList<String>> ret;      // the return vector of all users
        ArrayList<String> user;     // the per-user vector, composed of uid, access string

        if (!verifyAdmin(aid, apw)) {
            throw new AccessControlException("Bad username/password.");
        } else {
            synchronized (userDB) {
                users = userDB.values();
            }
            ret = new ArrayList<ArrayList<String>>(users.size());
            uiter = users.iterator();
            StringBuffer sb;
            while (uiter.hasNext()) {
                u = (ADEUser) uiter.next();
                if (!u.isComponent()) {
                    user = new ArrayList<String>(3);
                    sb = new StringBuffer();
                    synchronized (u) {
                        user.add(u.getUID());
                        if (u.isAdmin()) {
                            user.add("*");
                        } else {
                            user.add(" ");
                        }
                        for (String allowed : u.getAllowances()) {
                            sb.append(allowed);
                            sb.append(",");
                        }
                        sb.deleteCharAt(sb.length() - 1);
                        user.add(sb.toString());
                    }
                    ret.add(user);
                }
            }
        }
        debugPrint("Exiting from listUDB with a " + ret.size() + " user list");
        return ret;
    }

    /**
     * Get an {@link java.util.ArrayList ArrayList} of the administrators
     * currently in the user database.
     *
     * @param aid the administrator's ID
     * @param apw the administrator's password
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    @Override
    public ArrayList<String> listAdmins(String aid, String apw)
            throws RemoteException, AccessControlException {
        Set<String> admins;
        Iterator<String> aiter;
        String admin;
        ArrayList<String> ret;      // the return vector of all users

        if (!verifyAdmin(aid, apw)) {
            throw new AccessControlException("Bad username/password.");
        } else {
            synchronized (adminDB) {
                admins = adminDB.keySet();
            }
            ret = new ArrayList<String>(admins.size());
            aiter = admins.iterator();
            while (aiter.hasNext()) {
                admin = aiter.next();
                ret.add(admin);
            }
        }
        return ret;
    }

    /**
     * Get an {@link java.util.ArrayList ArrayList} of the {@link
     * ade.ADEComponent ADEComponent}s currently registered.
     *
     * @param uid the administrator's ID
     * @param upw the administrator's password
     * @return the list of component names
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    @Override
    public ArrayList<String> listComponents(String uid, String upw)
            throws RemoteException, AccessControlException {
        Collection<ADEComponentInfo> servs;
        Iterator<ADEComponentInfo> siter;
        ArrayList<String> ret;

        if (!verifyAdmin(uid, upw) && (verifyUser(uid, upw) == null)) {
            throw new AccessControlException("Bad username/password.");
        } else {
            synchronized (components) {
                servs = components.values();
            }
            ret = new ArrayList<String>(servs.size());
            ADEComponentInfo s;
            siter = servs.iterator();
            while (siter.hasNext()) {
                s = siter.next();
                ret.add(s.toString());
            }
        }
        return ret;
    }

    // gets a list of component references
    /**
     * Obtain a {@link java.util.HashMap HashMap} of {@link ade.ADEComponentInfo
     * ADEComponentInfo} structures (keyed on the component reference).
     *
     * @param uid the administrator's ID
     * @param upw the administrator's password
     * @return the map of components
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    @Override
    public HashMap<ADEComponent, ADEComponentInfo> getComponentInfos(String uid, String upw)
            throws RemoteException, AccessControlException {
        if (!verifyAdmin(uid, upw)) {
            throw new AccessControlException("Bad admin username/password.");
        } else {
            HashMap<ADEComponent, ADEComponentInfo> ret = new HashMap<ADEComponent, ADEComponentInfo>();
            // first get an array to decouple the return items from the component array
            ADEComponentInfo[] componentsarray;
            synchronized (components) {
                componentsarray = (ADEComponentInfo[]) components.values().toArray(new ADEComponentInfo[0]);
            }
            // now put the component info into the hashmap
            for (ADEComponentInfo si : componentsarray) {
                ret.put(si.as, si.duplicate());
            }

            return ret;
        }
    }

    // gets a list of component references
    /**
     * Obtain an {@link java.util.ArrayList ArrayList} of references to the
     * currently registered {@link ade.ADEComponent ADEComponent}s.
     *
     * @param uid the administrator's ID
     * @param upw the administrator's password
     * @return the map of component references
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    @Override
    public ArrayList<ADEComponent> getComponentRefs(String uid, String upw)
            throws RemoteException, AccessControlException {

        if (!verifyAdmin(uid, upw)) {
            throw new AccessControlException("Bad admin username/password.");
        } else {
            Iterator<ADEComponentInfo> it;
            synchronized (components) {
                it = components.values().iterator();
            }
            ArrayList<ADEComponent> ret = new ArrayList<ADEComponent>();
            while (it.hasNext()) {
                ret.add(it.next().as);
            }
            return ret;
        }
    }

    /**
     * Obtain an {@link java.util.ArrayList ArrayList} of {@link
     * ade.ADEHostInfo ADEHostInfo} objects (the hosts known to this registry).
     *
     * @param credentials The component's credentials
     * @return The list of {@link ade.ADEHostInfo ADEHostInfo} objects
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    @Override
    public ArrayList<ADEHostInfo> requestHostList(Object credentials)
            throws RemoteException, AccessControlException {
        if (verbose) {
            System.out.println(myID + ": host list requested");
        }
        if (!(credentials instanceof ADERegistry
                || ((String) credentials).indexOf("HostMonitorComponent") >= 0)) {
            throw new AccessControlException("Insufficient rights for host list");
        }
        return new ArrayList(knownhosts.values());
    }

    /**
     * Obtain an {@link java.util.ArrayList ArrayList} of all {@link
     * ade.ADEHostInfo ADEHostInfo} objects known to all registries.
     *
     * @param credentials The component's credentials
     * @return The list of {@link ade.ADEHostInfo ADEHostInfo} objects
     * @throws RemoteException if the request fails
     * @throws AccessControlException if the user does not have adequate
     * permissions
     */
    @Override
    public ArrayList<ADEHostInfo> requestHostListAll(Object credentials)
            throws RemoteException, AccessControlException {
        ArrayList<ADEHostInfo> hostlist, tmplist;

        if (verbose) {
            System.out.println(myID + ": host list requested");
        }
        if (!(credentials instanceof ADERegistry
                || ((String) credentials).indexOf("HostMonitorComponent") >= 0)) {
            throw new AccessControlException("Insufficient rights for host list");
        }
        // put our own hosts in the list
        hostlist = new ArrayList<ADEHostInfo>();
        for (ADEHostInfo ahi : knownhosts.values()) {
            hostlist.add(ahi.duplicate());
        }
        // then get the lists from the other registries
        // TODO: this should use concurrent calls
        for (ADERegistry r : registries.keySet()) {
            try {
                tmplist = r.requestHostList(this);
                hostlist.addAll(tmplist);
            } catch (Exception e) {
                if (dbg > 4 || verbose) {
                    System.err.println(myID + ": Exception requesting host list");
                    System.err.println(e);
                }
            }
        }
        return hostlist;
    }

    // MS: added to enable or disable ADEComponent logging
    // this is passed on to all registered components and other registries
    /**
     * Turns infrastructure logging on/off.
     *
     * @param uid The user ID making the request
     * @param upw The user's password
     * @param state <tt>true</tt> to turn logging on, <tt>false</tt> to turn
     * logging off
     * @param requester The <tt>ADERegistry</tt> making the request, if there is
     * one
     * @throws AccessControlException If the user is unrecognized
     * @throws RemoteException If another error occurs
     */
    @Override
    public final void setADEComponentLogging(
            String uid, String upw, boolean state, ADEComponent requester)
            throws AccessControlException, RemoteException {
        // NOTE: even though this Boolean is not final, it only gets changed inside this
        // synchronized statement and only at the end, thus there won't be any problems with
        // multiple copies being locked at the same time...
        synchronized (loggingrequest) {
            boolean userrequest = (!(requester instanceof ADERegistry));

            // if the requester is not a registry there is a requesting
            // requistry, carry out the request
            if (userrequest) {
                if (verifyUser(uid, upw) == null) {
                    throw new AccessControlException("Bad username/password");
                } else {
                    // forward the request to the other registries
                    // changing this to use concurrent calls
                    ensureRCTReg();
                    /*
                     try {
                     retList = rctReg.remoteCallConcurrent("setADEComponentLogging",
                     registries.keySet().toArray(), uid, upw, state, this);
                     for (Object ret : retList) {
                     if (ret instanceof Exception) {
                     System.err.println(myID +": failed to set registry logging:\n"+ ret);
                     }
                     }
                     } catch (ADEException ace) {
                     System.err.println(myID +": set logging for registries failed:\n"+ ace);
                     }
                     */
                    // this is done directly because it needs to be fast!
                    for (ADERegistry r : registries.keySet()) {
                        r.setADEComponentLogging(uid, upw, state, this);
                    }
                }
            }
            Iterator<ADEComponentInfo> siter;
            synchronized (components) {
                siter = components.values().iterator();
            }
            while (siter.hasNext()) {
                ADEComponentInfo s = (ADEComponentInfo) siter.next();
                // s.as.setLocalLogging(this,state);
                try {
                    rctServ.remoteCall("setLocalLogging", s.as, this, state);
                } catch (ADEException ace) {
                    System.err.println("Could not set local logging to " + state + " due to " + ace);
                    ace.printStackTrace();
                }
            }
            // create a new Boolean
            loggingrequest = new Boolean(state);
        }
    }

    /**
     * Stores an <tt>ADEComponent</tt>'s state.
     *
     * @param sid The ID of the component making the request
     * @param spw The component's password
     * @param state An instance of {@link ade.ADEState ADEState}
     * @throws AccessControlException If the user is unrecognized
     * @throws RemoteException If another error occurs
     */
    @Override
    public void setComponentState(String sid, String spw, ADEState state)
            throws AccessControlException, RemoteException {
        if (!components.containsKey(sid)) {
            System.out.println(myID + ": UNKNOWN " + sid + " trying to set state");
            throw new RemoteException("Unknown (or disconnected) component:" + sid);
        }

        if (verifyUser(sid, spw) != null) {
            if (dbg > 5) {
                System.out.println(myID + ": COMPONENT " + sid + " setting state");
            }
            ADEComponentInfo asi = components.get(sid);
            if (asi != null) {
                asi.adestate = state;
            } else {
                System.err.println(myID + ": missing state info for " + sid);
            }
        } else {
            throw new AccessControlException("Invalid component name or password");
        }
    }

    /**
     * Retrieves an <tt>ADEComponent</tt>'s state.
     *
     * @param sid The ID of the component making the request
     * @param spw The component's password
     * @return An instance of {@link ade.ADEState ADEState} (if one exists)
     * @throws AccessControlException If the user is unrecognized
     * @throws RemoteException If another error occurs
     */
    @Override
    public ADEState getComponentState(String sid, String spw)
            throws AccessControlException, RemoteException {
        if (!components.containsKey(sid)) {
            System.out.println(myID + ": UNKNOWN " + sid + " trying to get state");
            throw new RemoteException("Unknown (or disconnected) component:" + sid);
        }

        if (verifyUser(sid, spw) != null) {
            if (dbg > 5) {
                System.out.println(myID + ": COMPONENT " + sid + " getting state");
            }
            ADEComponentInfo asi = components.get(sid);
            if (asi != null) {
                // might be null, we don't care
                return asi.adestate;
            } else {
                System.err.println(myID + ": missing info for " + sid);
            }
        } else {
            // this is really an invalid password...
            throw new AccessControlException("Invalid component name or password");
        }
        return null;
    }

    // local shutdown due to errors, etc.
    // TODO: this needs to be better delineated/defined; there's more than
    // simply shutting down the components. Preliminary list:
    // - stop reaper
    // - stop hbs to other regs
    // - halt any external processes
    // - shutdown (registered) components
    // - write out files (config, host, user, ?)
    @Override
    final protected void localshutdown() {
        shuttingDown = true;
        if (dbg > 5) {
            System.out.println(myID + ": terminating reaper...");
        }
        myReaper.terminate();
        if (dbg > 5) {
            System.out.println("\t" + myID + " reaper terminated.");
        }
        ensureRCTServ();
        // even if we don't have the rctServ, we still want to finish...
        if (rctServ != null) {
            // shut down all components
            ADEComponentInfo[] vals;
            ArrayList<ADEComponent> servs = null;
            if (!components.isEmpty()) {
                //if (!heartbeats.isEmpty()) {
                if (dbg > 5) {
                    System.out.println(myID + ": halting components...");
                }
                synchronized (components) {
                    vals = components.values().toArray(new ADEComponentInfo[0]);
                    //synchronized(heartbeats) {
                    //	vals = heartbeats.values().toArray(new ADEComponentInfo[0]);
                }
                servs = new ArrayList<ADEComponent>();
                for (ADEComponentInfo asi : vals) {
                    if (asi.as != null) {
                        servs.add(asi.as);
                    }
                }
                // note that we don't check for individual exceptions
                try {
                    rctServ.remoteCallConcurrent("requestShutdown", servs.toArray(), this);
                    if (dbg > 5) {
                        System.out.println("\t" + myID + " halted components");
                    }
                } catch (Exception e) {
                    System.err.println(myID + ": Problem shutting down components:\n" + e);
                }
            }
        }
        // deregister from other registries
        if (!registries.isEmpty()) {
            System.out.println(myID + ": deregistering from " + registries.size() + " registries");
            ADERegistry[] regs = null;
            synchronized (registries) {
                regs = registries.keySet().toArray(new ADERegistry[registries.size()]);
                System.out.println(myID + ": have " + regs.length + " registries");
                deregisterFromRegistries(regs, this);
                registries.clear();
            }
        }

        // might want to take some more action here for shutdown
        if (procs != null && !procs.isEmpty()) {
            for (ADELocalProcessStarter alp : procs.values()) {
                alp.stopProcess();
            }
        }
        // nullify our remote call timers; note that because they are
        // created in the ADEComponentImpl superclass, we just nullify and
        // do not call terminate()
        rctServ = null;
        rctReg = null;
    }

    /**
     * Unbind the <tt>ADERegistry</tt> with the specified ID from the local Java
     * registry. Note that this method is accessible by classes in the
     * <tt>ade</tt> package so that it can be called from within the
     * <tt>ADEComponentImpl</tt> class in the <tt>shutdownADEComponent</tt> method.
     *
     * @param id The ID of the <tt>ADERegistry</tt> to unbind
     * @param me <tt>true</tt> indicates this registry is unbinding itself;
     * <tt>false</tt> indicates unbinding of a different registry.
     */
    final void unbindFromRegistry(String id, boolean me) {
        try {
            Registry r = LocateRegistry.getRegistry();
            if (me) {
                if (r.lookup(id).equals(thisreg)) {
                    r.unbind(id);
                    if (dbg > 3) {
                        System.out.println("\t" + myID + " unbound from JAVA registry.");
                    }
                }
            } else {
                if (!r.lookup(id).equals(thisreg)) {
                    r.unbind(id);
                    if (dbg > 3) {
                        System.out.println("\t" + id + " unbound from JAVA registry.");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(myID + ": error unbinding from JAVA registry:\n" + e);
        }
    }

    /**
     * Adds additional local checks for credentials before allowing a shutdown
     * must return "false" if shutdown is denied, true if permitted
     */
    @Override
    final protected boolean localrequestShutdown(Object credentials) {
        // allow for shutdown by other registries
        if (allowshutdownbyregistries && (credentials instanceof ADERegistry)) {
            return true;
        }
        // TODO: for now don't allow anybody to shut down the registry
        // has to go via the admin tool or via a Ctr-C on the terminal
        return false;
    }

    /**
     * This method will be activated whenever a client calls the
     * requestConnection(uid) method. Any connection-specific initialization
     * should be included here, either general or user-specific.
     */
    @Override
    protected void clientConnectReact(String user) {
        if (dbg > 3) {
            System.err.println(myID + ": connection from " + user);
        }
    }

    /**
     * This method will be activated whenever a client that has called the
     * requestConnection(uid) method fails to update (meaning that the heartbeat
     * signal has not been received by the reaper), allowing both general and
     * user specific reactions to lost connections. If it returns true, the
     * client's connection is removed.
     */
    @Override
    final protected boolean clientDownReact(String user) {
        if (dbg > 3) {
            System.err.println(myID + ": connection lost with " + user);
        }
        //protected boolean attemptRecoveryClientDown(String user) {
	/*
         * TODO: not clear what that was supposed to do... if
         * (user.indexOf("ADEType") != -1) return true;
         */
        return false;
    }

    /**
     * Causes the Registry to save the UDB and quit. Actual system exit happens
     * in the <tt>reaper</tt> thread.
     *
     * @param aid The administrator's username
     * @param apw The administrator's password
     * @return true or false (success)
     * @exception RemoteException if the remote invocation fails
     * @exception AccessControlException if the password is bad
     */
    @Override
    final public boolean shutdownRegistry(
            String aid,
            String apw) throws RemoteException, AccessControlException {
        boolean retval = true;
        if (!verifyAdmin(aid, apw)) {
            throw new AccessControlException("Bad username/password.");
        }
        if (dbg > 2) {
            System.out.println(myID + ": received shutdown from " + aid);
        }
        // shuts down the registry
        shutdownADEComponent();
        return retval;
    }

    /**
     * Causes the Registry to shutdown an {@link ade.ADEComponent ADEComponent}.
     *
     * @param aid The administrator's username
     * @param apw The administrator's password
     * @param sType The type of the ADEComponent
     * @param sName The name of the ADEComponent
     * @param lines Number of lines to return from the end (0 == all)
     * @return ArrayList with log data
     * @exception RemoteException if the remote invocation fails
     * @exception AccessControlException if the password is bad
     */
    @Override
    final public ArrayList reportLogs(
            String aid,
            String apw,
            String sType,
            String sName,
            int lines) throws RemoteException, AccessControlException {
        debugPrint(myID + ": " + aid + " is shutting down " + sType + "$" + sName + "...");
        ADEComponentInfo si;
        if (!verifyAdmin(aid, apw)) {
            throw new AccessControlException("Bad username/password.");
        }
        synchronized (components) {
            si = components.get(getKey(sType, sName));
        }
        if (si == null) {
            ArrayList tmp = new ArrayList();
            tmp.add("Component was not started with the --uilogging option");
            return tmp;
        } else {
            try {
                // return si.as.reportLogs(lines);
                // MS: changed to use remote call timer -- is this the right call???
                return (ArrayList) rctServ.remoteCall("reportLogs", si.as, lines);
                // } catch(RemoteException re) {
            } catch (ADEException ace) {
                ArrayList tmp = new ArrayList();
                tmp.add("Problem shutting down component " + si.name);
                return tmp;
            }
        }
    }

    /**
     * Causes the Registry to shutdown an {@link ade.ADEComponent ADEComponent}.
     *
     * @param aid The administrator's username
     * @param apw The administrator's password
     * @param sType The type of the ADEComponent
     * @param sName The name of the ADEComponent
     * @return true or false (success)
     * @exception RemoteException if the remote invocation fails
     * @exception AccessControlException if the password is bad
     */
    @Override
    final public boolean shutdownComponents(
            String aid,
            String apw,
            String[][] constraints // The constraints to be met by the component
            ) throws RemoteException, AccessControlException {
        debugPrint(myID + ": " + aid + " is shutting down all components meeting constraints " + constraints);
        boolean retval = true;
        if (!verifyAdmin(aid, apw)) {
            throw new AccessControlException("Bad username/password.");
        }
        synchronized (components) {
            for (Map.Entry<String, ADEComponentInfo> e : components.entrySet()) {
                ADEComponentInfo si = e.getValue();
                synchronized (si) {
                    if (meetsCriteria(si, constraints)) {
                        try {
                            rctServ.remoteCall("requestShutdown", si.as, this);
                        } catch (ADEException ace) {
                            if (debugging) {
                                System.err.println("Problem shutting down component " + si.name);
                            }
                            retval = false; // indicate that not all components shut down properly
                        }
                    }
                }
            }
        }
        return retval;
    }

    /**
     * Toggles registry debugging output.
     *
     * @param aid The administrator's username
     * @param apw The administrator's password
     * @return true or false (success)
     * @exception RemoteException if the remote invocation fails
     * @exception AccessControlException if the password is bad
     */
    @Override
    final public boolean toggleDebug(
            String aid,
            String apw) throws RemoteException, AccessControlException {
        if (!verifyAdmin(aid, apw)) {
            throw new AccessControlException("Bad username/password.");
        }
        debugging = !debugging;
        if (verbose) {
            System.out.println("Toggling debugging " + (debugging ? "on" : "off"));
        }
        return true;
    }

    /* * * * * * * * * * * * * * * * * * * *
     *           Utility methods           *
     * * * * * * * * * * * * * * * * * * * */

    /**
     * Check to see if a user is in the user table (remote version).
     *
     * @param uid The user id
     * @param upw The user password given
     * @exception AccessControlException if id or password is incorrect
     */
    @Override
    final public void checkUser(String uid, String upw)
            throws RemoteException, AccessControlException {
        // see if the uid is in the admin DB
        if (!userDB.containsKey(uid)) {
            throw new AccessControlException("No user " + uid + " recognized");
        }
        // check the password
        if (!((ADEUser) userDB.get(uid)).validPassword(upw)) {
            throw new AccessControlException("Incorrect password for " + uid);
        }
        return;
    }

    /**
     * This checks a user's authorization to access the registry.
     *
     * @param uid The username to check
     * @param upw The password (unencoded) to check.
     * @return true or false (true meaning "authorized")
     */
    private ADEUser verifyUser(String uid, String upw) {
        if (!userDB.containsKey(uid)) {
            return null;
        }
        // check the password
        ADEUser user = (ADEUser) userDB.get(uid);
        if (!user.validPassword(upw)) {
            return null;
        }
        return user;
    }

    @Override
    final public ADEUser verifyUser(String uid, String upw, ADERegistry ar)
            throws RemoteException, AccessControlException {
        // check if the registry is registered, if not, throw an exception
        if (registries.keySet().contains(ar)) {
	    // if the user isn't in the database, they're not authorized
            if (!userDB.containsKey(uid)) {
                return null;
            }
            // check the password
            ADEUser user = (ADEUser) userDB.get(uid);
            if (!user.validPassword(upw)) {
                return null;
            }
            return user;
	}
	else throw new AccessControlException("Registry not authorized to get information");
    }

    /**
     * Check to see if a user is in the administrator table (remote version).
     *
     * @param uid The user id
     * @param upw The user password given
     * @exception AccessControlException if id or password is incorrect
     */
    @Override
    final public void checkAdmin(String uid, String upw)
            throws RemoteException, AccessControlException {
        // see if the uid is in the admin DB
        if (!adminDB.containsKey(uid)) {
            throw new AccessControlException("No administrator " + uid + " recognized");
        }
        // check the password
        if (!((ADEUser) adminDB.get(uid)).validPassword(upw)) {
            throw new AccessControlException("Incorrect password for " + uid);
        }
        return;
    }

    /**
     * Check to see if a user is in the administrator table.
     *
     * @param uid The user id
     * @param upw The user password given
     * @return Whether the user qualifies as an administrator
     */
    final protected boolean verifyAdmin(String uid, String upw) {
        // see if the uid is in the admin DB
        if (!adminDB.containsKey(uid)) {
            return false;
        }
        // check the password
        if (!((ADEUser) adminDB.get(uid)).validPassword(upw)) {
            return false;
        }
        return true;
    }

    /**
     * Implement the abstract method as required by the {@link
     * ade.ADEComponentImpl ADEComponentImpl} class; registry does not use this
     * because it has its own special <tt>RegistryReaper</tt>, which calls
     * {@link #attemptRecoveryComponentDown} instead.
     *
     * @param as the component type
     */
    @Override
    final protected void componentDownReact(String as, String[][] constraints) {
        return;
    }

    /**
     * Implement the abstract method as required by the {@link
     * ade.ADEComponentImpl ADEComponentImpl} class; this method will be activated
     * whenever the heartbeat reconnects to a client (i.e., the component this is
     * sending a heartbeat to has failed and then recovered). <b>NOTE:</b> the
     * pseudo-reference obtained by the {@link #getClient} method will not be
     * set until <b>after</b> this method is executed. To perform operations on
     * the newly (re)acquired reference, you must use the <tt>ref</tt> parameter
     * object.
     *
     * @param as a string representing the {@link ade.ADEComponent ADEComponent} that
     * has established a connection
     * @param ref the pseudo-reference that was requested
     */
    @Override
    final protected void componentConnectReact(String as, Object ref, String[][] constraints) {
        return;
    }

    /**
     * The registry is always ready to provide its service after it has come up
     */
    @Override
    final protected boolean localServicesReady() {
        return true;
    }

    /**
     * Attempt to recover a failed component. This method calls {@link
     * #attemptStartComponent} after checking the component is not in the
     * <tt>inrecovery</tt> map (i.e., it is being recovered elsewhere), then
     * checking and decrementing the <tt>numrestarts</tt> variable.
     *
     * @param s The {@link ade.ADEComponentInfo ADEComponentInfo} object that contains
     * the information about the component to be restarted
     * @return <tt>true</tt> if the component was restarted, <tt>false</tt>
     * otherwise
     */
    private boolean attemptRecoveryComponentDown(ADEComponentInfo s) {
        // we use the inrecovery map to make sure we don't try to start
        // a component that has already been started (potentially, by another
        // registry, which will broadcast its attempt).
        if (verbose || debugReaper || debugging || dbg > 2) {
            System.out.println("ATTEMPTING recovery for component " + s.getKey());
        }
        if (s.numrestarts > 0) {
            try {
                if (debugReaper || debugging || dbg > 5) {
                    System.out.println(myID + ": Getting my info for restart...");
                }
                ADEComponentInfo myInfo = requestComponentInfo(this);
                // if failed component is a registry, we need to check if it is
                // already being restarted by another registry (not us)
                if (s.isregistry) {
                    if (debugReaper || debugging || dbg > 2) {
                        System.out.println(myID + ": checking for reg restart...");
                    }
                    String sKey = s.getKey();
                    String rKey = getKey(s.registryType, s.registryName);
                    if (!rKey.equals(myID)
                            && inrecovery.containsKey(sKey)) {
                        System.out.println(myID + ": " + rKey + " recovering " + sKey);
                        // TODO: throw exception instead?
                        return false;
                    }
                }
                // decrement the number of restart attempts
                if (debugReaper || debugging || dbg > 5) {
                    System.out.println(myID + ": Decrementing numrestarts...");
                }
                s.numrestarts--;
                if (debugReaper || debugging || dbg > 5) {
                    System.out.println(myID + ": Attempting restart...");
                }
                attemptStartComponent(myInfo, s);
                if (debugReaper || debugging || dbg > 2) {
                    System.out.println(myID + ": " + s.getKey() + " restarted!");
                }
                return true;
            } catch (Exception e) {
                System.err.println(myID + ": Failed restart " + s.getKey() + ":\n" + e);
                //e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Removes a component from the ADERegistry permanently. This should
     * <b>only</b> be called from the {@link #deregisterComponent(ADEComponentInfo)}
     * method, which performs partial deregistration.
     *
     * @param s The component to remove
     */
    private void removeComponent(ADEComponentInfo s) {
        // much of this method will perform the reverse steps of those found
        // in "registerComponent", minus those already done in "deregisterComponent".
        // . Remove the entry from the components map
        String sKey = s.getKey();

        debugPrint("\tIn removeComponent(" + s + ")");
        if (debugRegistration) {
            System.out.println(myID + ": <<<<<<< in removeComponent " + sKey);
        }
        ADEComponent as;
        ADEComponentInfo asi;

        // get our reference to the component that is to be removed
        synchronized (components) {
            asi = components.get(sKey);
        }

        if (debugRegistration) {
            System.out.println(myID + ": removing from heartbeats");
        }
        // note: synchronized maps; return null if no entry exists
        heartbeats.remove(sKey);        // stops reaper from seeing the component
        inrecovery.remove(sKey);

        // make sure we actually have a component info object
        if (asi != null) {
            if (debugRegistration) {
                System.out.println(myID + ": <<<<<<< got " + sKey + " info");
            }
            as = asi.as;
            if (as == null) {
                // whoops! lost the remote reference
                if (debugRegistration) {
                    System.out.println(myID + ": <<<<<<< missing " + sKey + " ref!");
                }
            } // it is a registry, remove it from the registry list
            // and stop it's heartbeat (done in superclass)
            else if (as instanceof ADERegistry) {
                if (debugRegistration) {
                    System.out.println(myID + ": <<<<<<< removing registry " + (registries.containsKey((ADERegistry) as)));
                }
                removeHeartbeat(sKey, this);
                synchronized (registries) {
                    registries.remove((ADERegistry) as);
                }
            }
        }

        // the component lookup; if we get here, the component has been removed
        // from the other data structures as promised in registerComponent()
        // components is a synchronizedMap; no need to synchronize
        //synchronized(components) {
        components.remove(sKey);
        //}
        if (debugRegistration) {
            System.out.println(myID + ": <<<<<<< removed " + sKey + " from components");
        }

        // make sure the reaper will run in an appropriate time
        debugPrint("\tResetting reaper time...");
        adjustReaperPeriod();

        System.out.println(myID + ": COMPONENT " + sKey + " REMOVED");
        return;
    }

    private void adjustReaperPeriod() {
        synchronized (components) {
            Collection ss = components.values();
            if (ss != null && ss.size() > 0) {
                int servPeriod;
                int maxServPeriod = 0;
                Iterator it = ss.iterator();
                ADEComponentInfo nextS;
                // find the maximum heartbeat time
                do {
                    nextS = (ADEComponentInfo) it.next();
                    servPeriod = nextS.getHeartBeatPeriod();
                    if (servPeriod > maxServPeriod) {
                        maxServPeriod = servPeriod;
                    }
                } while (it.hasNext());
                // setPeriod will pad reaping time a little
                myReaper.setPeriod(maxServPeriod);
            } else {
                myReaper.setPeriod(ADEGlobals.DEF_RPPULSE);
            }
        }
    }

    /**
     * If debugging is enabled, it prints the message.
     *
     * @param message the message to print
     */
    protected void debugPrint(String message) {
        if (debugging) {
            System.out.println(message);
        }
    }

    /* * * * * * * * * * * * * * * * * * * *
     *       Internally used objects       *
     * * * * * * * * * * * * * * * * * * * */
    /**
     * This keeps track of and removes {@link ade.ADEComponentImpl
     * ADEComponentImpl}s that haven't checked in in a while (in addition to
     * performing component related bookkeeping).
     *
     * @param timediff How many milliseconds the reaper should sleep between
     * reaping runs. The list of components using heartbeats should really be put
     * into a priority queue, in which case only those components using a heartbeat
     * would need to be checked, and the sleep value could be calculated more
     * precisely. Perhaps add a ArrayList of components that should be checked?
     */
    private class RegistryReaper extends Thread {

        int period;           // how often reaper will sleep
        //int timeBuffer = 750; // ms greater than shortest component period
        boolean shouldReap;
        ADERegistryImpl mycomponent; // because "this" is the reaper, not the reg
        boolean callChecker = false;
        private final ExecutorService singExeService = Executors.newCachedThreadPool();

        public RegistryReaper(int timediff, ADERegistryImpl ar) {
            mycomponent = ar;
            //period = timediff + timeBuffer;
            period = timediff;
            shouldReap = true;
        }

        public int getPeriod() {
            return period;
        }

        public void setPeriod(int ms) {
            // the hope is that twice the period plus a time buffer
            // will account for any network latency that exists
            //period = (2*ms) + timeBuffer;
            period = (2 * ms);
        }

        public void terminate() {
            shouldReap = false;
            if (dbg > 5) {
                System.out.println("\t" + myID + " set shouldReap to " + shouldReap);
            }
        }

        @Override
        public void run() {
            ADEComponentInfo s, actualsi = null;
            //LinkedList<ADEComponentInfo> ss = new LinkedList<ADEComponentInfo>();
            HashMap<String, ADEComponentInfo> ss = new HashMap<String, ADEComponentInfo>();
            Iterator<ADEComponentInfo> it;
            ADEMiniComponentInfo amsi; // for processing pendingupdates
            final Map<ADEComponentInfo, Thread> recoveryStarted = Collections.synchronizedMap(new HashMap<ADEComponentInfo, Thread>());
            Thread recover;
            long startTime,startTime2;
            int workTime, workTime2;

            // the registry does the bookkeeping required to maintain data
            // integrity. it is also responsible for restarting failed components
            // (that is, failures aren't detected until the reaper notices). This
            // carries implications concerning keeping data fresh; we'll assume
            // that the most current data is stored in the ADEComponentInfo objects,
            // even though it might be 1 reaper period stale
            // Make sure and do the recovery prior to the bookkeeping
            while (shouldReap) {
                if (debugReaper || debugging || dbg > 5) {
                    System.out.println(myID + ": registry reaping...");
                }
                if (shuttingDown) {
                    try {
                        if (debugReaper || debugging || dbg > 5) {
                            System.out.println("reaper caught shutdown");
                        }
                        //sleep(5); // make sure the shutdown command exits
                        shouldReap = false;
                        continue;
                    } catch (Exception e) {
                        if (debugReaper || debugging) {
                            System.err.println("Reaper shut down too fast");
                        }
                        shouldReap = false;
                    }
                }
                // set the start time, which is subtracted from the sleep
                startTime = System.currentTimeMillis();
                // check for the local JAVA registry, as another registry might
                // have brought it down on exit/failure. If so, bring it up again
                // so other components can register with this registry. During normal
                // operation, this is quick (just a LocateRegistry.getRegistry
                // and lookup call); if the Java registry is down, then there is
                // also registry creation and binding of the name.
                try {
                    if (!confirmLocalRegistry()) {
                        // ID is bound to a different ADERegistry?!? How?
                        System.err.println(myID + ": RegistryReaper.run(): ID already bound; aborting...");
                        System.exit(ADEGlobals.ExitCode.JAVA_REG.code());
                    }
                    //} catch (ConnectException ce) {
                    //	// some weirdness; registry exists, but we can't connect?
                    //	System.err.println(myID +": failed connect to registry...\n"+ e);
                    //	skipreap = true;
                } catch (Exception e) {
                    // no sense in running if we cannot create an RMI registry
                    System.err.println(myID + ": No RMI registry; not reaping...\n" + e);
                    // MS: put this back in --
                    // TODO: if we should get here, we might have to figure out how to deal with it...
                    System.err.println("System exit due to problems with JAVA RMI registry (added in 040710 by MS), aborting...");
                    System.exit(ADEGlobals.ExitCode.JAVA_REG.code());
                }
                // get a new list of components; note that we duplicate the
                // contents of the heartbeats map to avoid synch holdups
                if (debugReaper) {
                    System.out.println(myID + ": getting component list");
                }
                ss.clear();
                // set the start time, which is subtracted from the sleep
                // MS: this is without the confirming of the registry, because that can take a variable amount of time and
                //     we should not penalize the component lookup for it...
                startTime2 = System.currentTimeMillis();
                synchronized (heartbeats) {
                    if (!heartbeats.isEmpty()) {
                        it = heartbeats.values().iterator();
                        while (it.hasNext()) {
                            // do we *really* want to duplicate? Then we can't
                            // update the information therein...
                            //ss.add(it.next().duplicate());
                            //s = it.next().duplicate();
                            //ss.put(s.getKey(), s);
                            s = it.next();
                            ss.put(s.getKey(), s);
                        }
                    }
                }
                if (ss != null && ss.size() > 0) {
                    if (debugReaper || debugging || dbg > 7) {
                        System.out.println(myID + " RegistryReaper: have " + ss.size() + " components in list");
                    }
                    it = ss.values().iterator();
                    do {
                        //s = (ADEComponentInfo)it.next();
                        s = it.next();
                        if (debugReaper || debugging) {
                            System.out.println(myID + ": Checking " + s.toString());
                        }
                        /*
                         // component is in the heartbeats hash, but also in the
                         // recovery started hash! Interrupt the recovery thread
                         // and allow the reaper to continue as normal
                         if ((recover = recoveryStarted.get(s)) != null) {
                         System.out.println(myID +": Interrupting recovery of "+ s.toString());
                         //s.as = null;
                         recover.interrupt();
                         if (!it.hasNext()) break;
                         else continue;
                         }
                         */
                        // if it's overdue for checkin AND checkStartRecovery
                        // returned true (the previous case)...
                        if (!s.checkStatus((long) period)) {
                            if (debugReaper || dbg > 2) {
                                System.out.println(myID + ": Possible disconnection of " + s.getKey() + "!");
                            }
                            // Note that since both depend on period,
                            // checkStartRecovery can never return true unless
                            // checkStatus will return false
                            if (!s.checkStartRecovery((long) period)) {
                                if (debugReaper) {
                                    System.out.println(myID + ": Delaying recovery of " + s.getKey());
                                }
                                //System.out.println("\t\tSetting state to DELAYRECOVERY");
                                s.setRecoveryState(ADEGlobals.RecoveryState.DELAY);
                                continue;
                            }
                            // the component might be gone, so investigate and possibly
                            // attempt a restart...
                            // first test the remote reference...if it's gone, this
                            // will cause an exception; if it's there, give the component
                            // another chance to send the update
                            boolean stillUp = false;
                            try {
                                if (debugReaper) {
                                    System.out.println("\tCalling isUp()...");
                                }
                                stillUp = (Boolean) rctReg.remoteCall(ADEGlobals.DEF_RECOVERYTIMEOUT, "isUp", s.as);
                            } catch (Exception e) {
                                // if there's an exception, recovery will begin, so no action
                            }
                            if (stillUp) {
                                // component is there; set recovery state to OK
                                s.setRecoveryState(ADEGlobals.RecoveryState.OK);
                                if (debugReaper || dbg > 2) {
                                    System.out.println(myID + ": " + s.getKey() + " still there");
                                }
                                continue;
                            }
                            s.setRecoveryState(ADEGlobals.RecoveryState.DOWN);
                            if (!inrecovery.containsKey(s.getKey())) {
                                // TODO: keep a reference to thread for interruption?
                                recover = new RecoveryThread(mycomponent, s);
                                //recoveryStarted.put(s, recover);
                                inrecovery.put(s.getKey(), s);
                                heartbeats.remove(s.getKey());
                                recover.start();
                                if (debugReaper || debugging || dbg > 2) {
                                    System.out.println(myID + ": Started " + recover
                                            + " to recover " + s.getKey() + "...");
                                }
                            } else {
                                if (debugReaper || debugging || dbg > 2) {
                                    System.out.println(myID + ": Recovery of "
                                            + s.getKey() + " already started");
                                }
                            }
                            if (debugTimes) {
                                System.out.println(myID + ": recovery of " + s.name + "-thread started for recovery " + (System.currentTimeMillis() - startTime) + "ms");
                            }
                        } else {
                            // component is good
                            s.setRecoveryState(ADEGlobals.RecoveryState.OK);
                            if (debugReaper || debugging) {
                                System.out.println("\t\tRestarts=" + s.numrestarts + "; " + s.getConnectsCurrent() + " user still connected...");
                            }
                        }
                    } while (it.hasNext());

                    // that's the recovery; now do the bookkeeping
                    LinkedList<ADEMiniComponentInfo> pending;
                    String[] srvconns;
                    if (pendingupdates.size() > 0) {
                        // copy the current pendingupdates to a list so as to cap
                        // the number processed (which are constantly being added)
                        // and lock pendingupdates as little as possible
                        synchronized (pendingupdates) {
                            pending = new LinkedList(pendingupdates.values());
                            pendingupdates.clear();
                        }
                        if (dbg > 8) {
                            System.out.println(myID + ": doing " + pending.size() + " updates");
                        }
                        while (!pending.isEmpty()) {
                            srvconns = new String[0]; // need to reset
                            // create a new list and make it final, so we can pass it on to a thread later
                            final LinkedList<ADEMiniComponentInfo> updating = new LinkedList<ADEMiniComponentInfo>();
                            amsi = pending.removeFirst();
                            if (debugReaper || debugging || dbg > 8) {
                                System.out.println(myID + ": updating " + amsi.id);
                            }
                            if (!ss.containsKey(amsi.id)) {
                                // this might happen if the component has shutdown or
                                // deregistered and there are still updates
                                //System.out.println(myID +": missing "+ amsi.id +"!");
                                continue;
                            }
                            s = ss.get(amsi.id);
                            s.update(amsi);
                            srvconns = amsi.components.toArray(srvconns);
                            if (dbg > 9) {
                                System.out.println("\t" + amsi.id + " has " + srvconns.length + " pseudorefs");
                                for (int j = 0; j < srvconns.length; j++) {
                                    System.out.println("\t\t " + srvconns[j]);
                                }
                            }
                            // this is where we supplement the updating component's
                            // information with info about its clients' recovery
                            // states (if necessary)
                            for (int j = 0; j < srvconns.length; j++) {
                                // get the client's known state as it was last sent
                                // by its heartbeat; client may be registered here,
                                // may be registered here but invalidated, or may be
                                // registered elsewhere...
                                //System.out.println(myID +": looking for "+ srvconns[j] +" in heartbeats...");
                                if (heartbeats.containsKey(srvconns[j])) {
                                    // good, it's local
                                    actualsi = heartbeats.get(srvconns[j]);
                                    updating.add(new ADEMiniComponentInfo(srvconns[j], actualsi.state, actualsi.recState));
                                } else if (inrecovery.containsKey(srvconns[j])) {
                                    actualsi = inrecovery.get(srvconns[j]);
                                    updating.add(new ADEMiniComponentInfo(srvconns[j], ADEGlobals.ComponentState.DEREGISTER, ADEGlobals.RecoveryState.UNREC));
                                    //updating.add(new ADEMiniComponentInfo(srvconns[j], ADEGlobals.ComponentState.DEREGISTER, ADEGlobals.RecoveryState.OK));
                                    //updating.add(new ADEMiniComponentInfo(srvconns[j], ADEGlobals.ComponentState.DEREGISTER, actualsi.recState));

                                } else {
                                    // this is an issue because it might interfere with
                                    // the reaper's timing...
                                    //actualsi = null;
                                    //System.err.println("\tWhaaah?!?!?!?!?! components hash missing "+ srvconns[j] +" key!");
                                    //System.err.println("\tObtaining remote state information not implemented yet!");
                                    updating.add(new ADEMiniComponentInfo(srvconns[j], ADEGlobals.ComponentState.RUN, ADEGlobals.RecoveryState.DOWN));
                                }
                            }
                            // send the reconciled list to the component
                            if (updating.size() > 0) {
                                // use non-blocking remote call timer so that this will all be submitted quickly
                                try {
				    // MS: don't do this for registries ...  (TODO: the info needs to be transferred in a different way)
				    if (! (s.as instanceof ADERegistry)) {
					// make this call non-blocking
					mycomponent.rctServ.remoteCallNonBlocking("updateComponentInfo", s.as, updating, mycomponent);
				    }
                                } catch (ADEException ace) {
                                    // we're gonna see recovery messages, so only show
                                    // this if dbg is above a certain level...
                                    if (debugging || dbg > 5) {
                                        System.err.println(myID + ": failed to update "
                                                + amsi.id + " due to:\n" + ace);
                                    }
                                }
                            } else {
                                //System.out.println("\t++++++++++ no info to send!");
                            }
                        }
                    }
                } else {
                    if (debugReaper || debugging) {
                        System.out.println(myID + ": registry reaper - no components registered");
                    }
                }
                // done; get the end time so we can sleep appropriately
                workTime = (int) (System.currentTimeMillis() - startTime);
                workTime2 = (int) (System.currentTimeMillis() - startTime2);
                if (debugReaper || debugging) {
                    System.out.println(myID + ": overall reaper cycle took " + workTime + "ms");
                    System.out.println(myID + ": reaper cycle for components took " + workTime2 + "ms");
                }
                if (workTime2 > period) {
                    System.err.println(myID + ": too much component load; reaper falling behind!");
                }
                else if (workTime > period) {
                    System.err.println(myID + ": locating Java registry taking up too time; reaper cannot meet loop requirements!");
                }
                else {
                    try {
                        // even death sleeps sometimes
                        if (debugReaper || debugging) {
                            System.out.println(myID + ": registry reaper sleeping " + (period - workTime));
                        }
                        sleep(period - workTime);
                    } catch (Exception e) {
                        if (verbose || debugReaper) {
                            System.err.println("Reaper had insomnia: " + e);
                        }
                        shouldReap = false;
                    }
                }
            } // end while(shouldReap)
            if (debugReaper || debugging || dbg > 5) {
                System.out.println(myID + ": exiting RegistryReaper thread");
            }
        }
    }

    // Note that unlike the checkConfigFile() method, we do not use a separate
    // thread, as the hosts are necessary for the components
    private void checkHostsFile() {
        File f;
        FileReader fr;
        BufferedReader br = null;
        int lineno;

        if (verbose || debugging) {
            System.out.println(prg + ": Checking hosts file...");
        }
        if (hostsfile != null) {
            //ADEComponentInfo si = null;
            f = ADEGlobals.getADEConfigFile(hostsfile);
            if (f == null) {
                System.err.println("Hosts file " + hostsfile + " not found");
                System.exit(ADEGlobals.ExitCode.HOST_FILE.code());
            }
            hostsfile = f.getPath();

            /*
             try {
             si = requestComponentInfo(this);
             } catch (AccessControlException ex) {
             System.err.println(prg +": Could not get component information");
             return;
             } catch (RemoteException ex) {
             System.err.println(prg +": Could not get component information");
             return;
             }
             */
            try {
                fr = new FileReader(f);
                br = new BufferedReader(fr);
            } catch (FileNotFoundException fnfe) {
                if (dbg > 2 || verbose) {
                    System.out.println("File not found " + f.getName());
                }
                System.exit(ADEGlobals.ExitCode.HOST_FILE.code());
            }
            lineno = 0;
            while (true) {
                ADEHostInfo hi = null;
                String str = null;
                try {
                    str = br.readLine();
                    lineno++;
                } catch (IOException ioe) {
                    continue;
                } catch (Exception e) {
                    System.err.println("Could not parse configuration file due to " + e + "\naborting...");
                    System.exit(ADEGlobals.ExitCode.FILE_PARSE.code());
                }
                if (str != null && str.length() > 0) {
                    if (str.startsWith("STARTHOST")) {
                        hi = new ADEHostInfo();
                        try {
                            hi.parseHost(br, lineno);
                        } catch (ParseException pe) {
                            System.err.println(prg + ": Parse Exception parsing host");
                        } catch (IOException ioe) {
                            System.err.println(prg + ": IOException parsing host");
                        } catch (NumberFormatException ex) {
                            System.err.println(prg + ": NumberFormatException parsing host");
                        }
                        if (hi != null) {
                            knownhosts.put(hi.hostip, hi);
                        } else {
                            System.out.println(prg + ": null host!");
                            //br.reset(); // try to move it back...
                        }
                        // check for comments and simply skip
                    } else if (str.startsWith("#")) {
                        //} else if (str.startsWith("LOADHOSTS")) {
                        //  not implemented yet; want to be able to embed other files
                        //  something like calling checkConfigFile recursively
                    }
                } else {
                    break;
                }
            }

            // MS: fix the lack of parameter updates on the registry host... (e.g., ssh params)
            // check the host to see if we got new information about our host, then reset our info
            try {
                ADEHostInfo myHost = requestHostInfo(this);
                if (debugging) {
                    System.out.println(prg + ": checking my host info...");
                }
                if ((myHost = knownhosts.get(myHost.hostip)) != null) {
                    if (debugging) {
                        System.out.println("Replaced host information with:");
                        myHost.print();
                    }
                    setHostInfo(myHost, this);
                }
            } catch (RemoteException re) {
                System.err.println("Could not check for Registry's host info... (host updates not checked)");
            }
            // perform various checks on the host file, not completley implemented yet
			/*
             if (knownhosts.size() > 0) {
             for (ADEHostInfo hi : knownhosts.values()) {
             //System.out.println("\tChecking data for "+ hi.hostip);
             try {
             if (hi.hostip.equals(si.host)) hi.checkMembers();
             else hi.checkMembers(myHost);
             } catch (SecurityException ex) {
             // do we want to remove the host ?
             System.err.println(ex);
             } catch (IOException ex) {
             // do we want to remove the host?
             System.err.println(ex);
             }
             }
             }
             */
        }
    }

    // =======================================================================
    // this function effectively reads in an ADE configuration file and starts
    // up the components in sequence
    private void checkConfigFile() {
        try {
            final ADEComponentInfo myInfo = requestComponentInfo(this);
            // TODO: ignoreconfig isn't used right now; it's supposed to treat
            // cases where if a registry, check if components *need* restarting.
            // See also: the startComponent method in ADEHostInfo
            //if (myInfo.configfile != null && !myInfo.ignoreconfig) {
            if (myInfo.configfile != null) {
                final File cfgfile = ADEGlobals.getADEConfigFile(myInfo.configfile);
                if (cfgfile != null) {
                    new Thread() {
                        @Override
                        public void run() {
                            yield();
			    // delay the start-up until at least one registry has checked in and is registered
			    if (delaystartup) {
				while(registries.isEmpty()) {
				    try {
					sleep(1000);
				    } catch(InterruptedException ie) {}
				}
			    }
			    // now start up the system
                            try {
                                startUpADE(myInfo, cfgfile);
                            } catch (Exception e) {
                                System.err.println("Problem parsing file: " + e);
                            }
                        }
                    }.start();
                } else {
                    System.out.println(prg + ": Cannot find " + myInfo.configfile);
                }
                //myInfo.ignoreconfig = false;
            }
        } catch (RemoteException re) {
            System.err.println("HOW???");// this should never happen
        }
    }

    private void startUpADE(ADEComponentInfo myInfo, File configurationfile)
            throws ParseException {
        // want to sleep for a couple ms to guarantee the constructor finishes
        try {
            Thread.sleep(50);
        } catch (Exception ignore) {
        }
        if (debugging) {
            System.out.println("Starting ADE configuration...");
        }
        // now load the initialization data
        try {
            FileReader fr = new FileReader(configurationfile);
            BufferedReader br = new BufferedReader(fr);
            int lineno = 0;
            while (true) {
                try {
                    String str = br.readLine();
                    lineno++;
                    //if (str != null && str.length() > 0) {
                    if (debugging || dbg > 6) {
                        System.out.println(prg + ": parsing " + str);
                    }
                    if (str != null) {
                        if (str.startsWith("STARTCOMPONENT")) {
                            //readingcomponent = true;
                            ADEComponentInfo regComponent = new ADEComponentInfo();
                            try {
                                regComponent.parseComponent(br, lineno);
                                ADEHostInfo hi = null;
                                // Checking for known hosts is also done in
                                // attemptStartComponent; we repeat it here because that
                                // method will attempt to relocate the component, but if
                                // the initially specified host is invalid, we don't
                                // ever want to allow that
                                if ((hi = knownhosts.get(regComponent.host)) == null) {
                                    System.err.println("..........CANNOT START COMPONENT; unknown host " + regComponent.host);
                                } else {
                                    if (verbose || debugging) {
                                        System.out.println("...........CREATED COMPONENTINFO " + regComponent);
                                        //regComponent.print();
                                    }
                                    if (dbg > 1 || verbose) {
                                        System.out.println("Starting " + regComponent);
                                    }
                                    if (!attemptStartComponent(myInfo, regComponent)) {
                                        if (verbose || debugging) {
                                            System.out.println("...........Could not start component " + regComponent);
                                        }
                                    } else {
                                        if (verbose || debugging) {
                                            System.out.println("...........Started " + regComponent);
                                        }
                                    }
                                }
                            } catch (Exception pe) {
                                System.err.println("Exception caught while trying to parse config file.  Exception: " + pe);
                                pe.printStackTrace();
                                br.reset(); // try to move it back...
                            }
                        } // check for comments and simply skip
                        else if (str.startsWith("#")) {
                        } // check for pause and execute
                        else if (str.startsWith("pause")) {
                            StringTokenizer s = new StringTokenizer(str);
                            s.nextToken();
                            while (s.hasMoreTokens()) {
                                try {
                                    int p = Integer.parseInt(s.nextToken());
                                    Thread.sleep(p);
                                } catch (NumberFormatException nfe) {
                                    if (debugging) {
                                        System.err.println("Parsing format exception");
                                    }
                                }
                            }
                        } // MS: this will recursively load component configuration files
                        // seperated by spaces
                        else if (str.startsWith("LOADCOMPONENTCONFIGS")) {
                            StringTokenizer s = new StringTokenizer(str);
                            s.nextToken();
                            while (s.hasMoreTokens()) {
                                startUpADE(myInfo, new File(s.nextToken()));
                            }
                        }
                    } else {
                        break;
                    }
                } catch (IOException ioe) {
                    // the reset() failed...
                } catch (Exception e) {
                    System.err.println("Could not parse configuration file due to " + e + "\naborting...");
                    System.exit(ADEGlobals.ExitCode.FILE_PARSE.code());
                }
            }
        } catch (FileNotFoundException fnfe) {
            if (dbg > 2 || verbose) {
                System.out.println("File not found " + configurationfile.getName());
            }
            System.out.println("FILENOTFOUND: " + fnfe);
        }
    }

    // find an available IP address for the given host, using "ping" to check availability
    public String processAddress(String hostname) {
        InetAddress[] ip = null;
        Runtime R = Runtime.getRuntime();
        try {
            ip = InetAddress.getAllByName(hostname);
        } catch (java.net.UnknownHostException e) {
            if (debugging) {
                System.err.println(e + " Could not determine localhost");
            }
        }
        if (ip != null) {
            for (int i = 0; i < ip.length; i++) {
                try {
                    String addr = ip[i].getHostAddress();
                    String[] cmd = {"ping", "-w", "2", addr};
                    Process P = R.exec(cmd);
                    try {
                        P.waitFor();
                        String procOut = loadStream(P.getInputStream());
                        String procErr = loadStream(P.getErrorStream());
                        if (procOut.indexOf("0 received") != -1) {
                            if (debugging) {
                                System.out.println("Address " + addr + " is currently invalid for host " + hostname + ".  This is address " + i + " out of " + ip.length);
                            } else {
                                return addr;
                            }
                        }
                    } catch (InterruptedException ie) {
                        if (debugging) {
                            System.out.println("Interrupted");
                        }
                    }
                } catch (IOException ioe) {
                    if (debugging) {
                        System.err.println("ADEstartup: IO Error pinging host " + ioe);
                    }
                }
            }
        }
        return ip[0].getHostAddress();
    }

    private String loadStream(InputStream in) throws IOException {
        int ptr = 0;
        in = new BufferedInputStream(in);
        StringBuffer buffer = new StringBuffer();
        while ((ptr = in.read()) != -1) {
            buffer.append((char) ptr);
        }
        return buffer.toString();
    }

    // Currently not used
    /**
     * This function either loads a user database file or reads it from a
     * regular text file
     *
     * @param admin The username of the registry administrator
     * @param passwd The password of the registry administrator
     * @param file The permissions file
     */
    void registrySetUp(String admin, String passwd, String file, ArrayList users) {
        if (file.indexOf(".udb") != -1) {
            try {
                loadUDB(admin, passwd, file);
            } catch (RemoteException re) {
                if (verbose) {
                    System.out.println("Could not load user database file " + file + " into registry");
                }
            }
        } else {
            File f = new File(file);
            if (f.exists()) {
                try {
                    FileReader fr = new FileReader(f);
                    BufferedReader br = new BufferedReader(fr);
                    while (true) {
                        try {
                            String str = br.readLine();
                            if (str != null) {
                                StringTokenizer s = new StringTokenizer(str);
                                String uname = s.nextToken();
                                String upasswd = s.nextToken();
                                HashSet<String> perms = new HashSet<String>();
                                while (s.hasMoreTokens()) {
                                    String data = s.nextToken();
                                    perms.add(data);
                                }
                                try {
                                    addUser(admin, passwd, uname, upasswd, perms);
                                } catch (RemoteException re) {
                                    if (debugging) {
                                        System.out.println("Caught exception adding user to registry " + re);
                                    }
                                }
                            } else {
                                break;
                            }
                        } catch (IOException ioe) {
                            break;
                        }
                    }
                } catch (FileNotFoundException fnfe) {
                    if (debugging) {
                        System.out.println("File not found " + f.getName());
                    }
                }
            }
        }
    }

    /**
     * Provide additional information concerning command-line switches. Any
     * additional switches added should be parsed in the {@link
     * #parseadditionalargs} method.
     *
     * @return The text to display when given a <tt>-h</tt> switch
     */
    @Override
    protected String additionalUsageInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("     -s -S --shutdown       <shutdown by other registries allowed>\n");
        sb.append("     -p -P --permission     <user name and password for registry admin>\n");
        sb.append("     -k -K --knownhosts     <[file] that contains information about available hosts>\n");
        return sb.toString();
    }

    /**
     * Parse additional command-line arguments. Needs to return <tt>true</tt> if
     * parse is successful, <tt>false</tt> otherwise. If additional arguments
     * are enabled, please make sure to add them to the {@link
     * #additionalUsageInfo} method to display them from the command-line help.
     *
     * @param args The additional arguments for this component
     * @return <tt>true</tt> if additional arguments exist and were parsed
     * correctly, <tt>false</tt> otherwise
     */
    @Override
    protected boolean parseadditionalargs(String[] args) {
        boolean found = false;
        // note that each switch that takes additional parameters is in a
        // try/catch to prevent failure due to missing values
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-p")
                    || args[i].equals("--permission")) {
                String newadminID, newadminpw;
                try {
                    newadminID = args[++i];
                    // TODO: (masked) query for password to avoid plain text
                    newadminpw = args[++i];
                    adminID = newadminID;
                    adminpasswd = newadminpw;
                    found = true;
                } catch (Exception e) {
                    System.err.println(prg + ": Exception with permission parameter");
                    System.err.println(e);
                    System.err.println(prg + ": Using default permission");
                }
            } else if (args[i].equalsIgnoreCase("-k")
                    || args[i].equals("--knownhosts")) {
                String newhostsfile;
                try {
                    newhostsfile = args[++i];
                    hostsfile = newhostsfile;
                    found = true;
                } catch (Exception e) {
                    System.err.println(prg + ": Exception with hosts file parameter");
                    System.err.println(e);
                    System.err.println(prg + ": Using default host(s)");
                }
            } else if (args[i].equalsIgnoreCase("-s")
                    || args[i].equals("--shutdown")) {
                allowshutdownbyregistries = true;
            } else {
                return false;
            }
        }
        return found;
    }

    // for now, the registry does not updateComponent or updateFromLog
    @Override
    final protected void updateComponent() {
    }

    final protected void updateFromLog(String logEntry) {
    }

    /**
     * *********************************************************************
     */
    /**
     * ************ ACCESS METHODS FOR THE ADE SYSTEM VIEW GUI *************
     */
    /**
     * *********************************************************************
     */
    /**
     * NOTE that ALL gui-related methods accept a SystemViewAccess *
     */
    /**
     * accessKey as the first parameter, for authentication purposes. *
     */
    /**
     * *********************************************************************
     */
    @Override
    public ADEGuiVisualizationSpecs getVisualizationSpecs() throws RemoteException {
        ADEGuiVisualizationSpecs specs = super.getVisualizationSpecs();

        specs.add("System", ADESystemView.class,
                this.getName(), new SystemViewAccess(adminIDdefault, adminpassdefault));
        // TODO:  NOTE:  giving the system view full admin access.  If later want to limit this,
        //     could make the SystemViewAccess take no parameters, and instead have 
        //     a "setter" method that the GUI would populate based on, say, 
        //     a username-password dialog. 

        return specs;
    }

    @Override
    public SystemViewStatusData guiGetRegistryStatus(
            SystemViewAccess accessKey, InfoRequestSpecs requestSpecs,
            boolean entireSystem) throws RemoteException {
        // TODO:  may want to check access key, if want to ensure that the it's 
        //        the GUI (and with appropriate rights) that's calling this method

        SystemViewStatusData data = new SystemViewStatusData();

        if (entireSystem) {
            // Add the registry itself.
            data.componentIDtoInfoMap.put(this.getID(), guiConvertToSystemViewComponentInfo(
                    this.requestComponentInfo(this), requestSpecs));
        }

        // any local components and registries (registries are kept both in the 
        //      registries hashmap and the component hashmap).
        for (ADEComponentInfo eachMassiveComponentInfo : components.values()) {
            data.componentIDtoInfoMap.put(getKey(eachMassiveComponentInfo.type, eachMassiveComponentInfo.name),
                    guiConvertToSystemViewComponentInfo(eachMassiveComponentInfo, requestSpecs));
        }

        if (entireSystem) {
            // ask any registries for THEIR components:
            for (Entry<ADERegistry, ADEComponentInfo> registryEntrySet : registries.entrySet()) {
                try {
                    SystemViewStatusData otherRegLocalInfo =
                            (SystemViewStatusData) rctReg.remoteCall(
                            "guiGetRegistryStatus", registryEntrySet.getKey(),
                            accessKey, requestSpecs, false);
                    // false = no longer entire system, so as to prevent infinite recursion.

                    for (SystemViewComponentInfo otherComponentInfo : otherRegLocalInfo.componentIDtoInfoMap.values()) {
                        data.componentIDtoInfoMap.put(
                                getKey(otherComponentInfo.type, otherComponentInfo.name), otherComponentInfo);
                    }
                } catch (Exception e) {
                    System.out.println("Could not get GUI information from registry " + registryEntrySet.getValue().name);
                }
            }
        }

        return data;
    }

    @Override
    public SystemViewComponentInfo guiGetComponentStatus(SystemViewAccess accessKey,
            String componentID, InfoRequestSpecs requestSpecs, boolean searchEntireSystem)
            throws RemoteException {
        // TODO:  may want to check access key, if want to ensure that the it's 
        //        the GUI (and with appropriate rights) that's calling this method

        if (componentID.equals(this.getID())) {
            return guiConvertToSystemViewComponentInfo(
                    this.requestComponentInfo(this), requestSpecs);
        } else if (components.containsKey(componentID)) {
            return guiConvertToSystemViewComponentInfo(
                    components.get(componentID), requestSpecs);
        } else if (searchEntireSystem) {
            // try to search amongst other registries:
            for (Entry<ADERegistry, ADEComponentInfo> registryEntrySet : registries.entrySet()) {
                try {
                    SystemViewComponentInfo componentInfoOnOtherRegistry =
                            (SystemViewComponentInfo) rctReg.remoteCall(
                            "guiGetComponentStatus", registryEntrySet.getKey(),
                            accessKey, componentID, requestSpecs, false);
                    // false = no longer entire system, so as to prevent infinite recursion.
                    if (componentInfoOnOtherRegistry != null) {
                        return componentInfoOnOtherRegistry;
                    }
                } catch (Exception e) {
                    System.out.println("Could not search for GUI component status on registry "
                            + registryEntrySet.getValue().name);
                }
            }

            // if still here, found nothing:
            throw new RemoteException("Component " + componentID + " not found on ANY registry!");
        } else {
            return null; // (hopefully the component will be found on another registry!)
        }
    }

    private SystemViewComponentInfo guiConvertToSystemViewComponentInfo(
            ADEComponentInfo massiveComponentInfo, InfoRequestSpecs requestSpecs) {
        SystemViewComponentInfo componentGuiInfo = new SystemViewComponentInfo();

        // ALWAYS include name and type info.
        componentGuiInfo.name = massiveComponentInfo.name;
        componentGuiInfo.type = massiveComponentInfo.type;

        if (requestSpecs.host) {
            componentGuiInfo.host = massiveComponentInfo.host;
        }
        if (requestSpecs.registryName) {
            componentGuiInfo.registryName = massiveComponentInfo.registryName;
        }
        if (requestSpecs.startDirectory) {
            componentGuiInfo.startDirectory = massiveComponentInfo.startdirectory;
        }
        if (requestSpecs.clients) {
            componentGuiInfo.clients = massiveComponentInfo.clients;
        }

        return componentGuiInfo;
    }

    @Override
    public ADEGuiVisualizationSpecs guiGetComponentVisualizationSpecs(
            SystemViewAccess accessKey, String componentID,
            boolean searchEntireSystem) throws RemoteException {
        // TODO:  may want to check access key, if want to ensure that the it's 
        //        the GUI (and with appropriate rights) that's calling this method

        if (componentID.equals(this.getID())) {
            return this.getVisualizationSpecs();
        } else if (components.containsKey(componentID)) {
            return (ADEGuiVisualizationSpecs) tryCallingComponent(
                    componentID, "getVisualizationSpecs");
        } else if (searchEntireSystem) {
            // try to search amongst other registries:
            for (Entry<ADERegistry, ADEComponentInfo> registryEntrySet : registries.entrySet()) {
                try {
                    ADEGuiVisualizationSpecs visSpecsOnOtherRegistry =
                            (ADEGuiVisualizationSpecs) rctReg.remoteCall(
                            "guiGetComponentVisualizationSpecs", registryEntrySet.getKey(),
                            accessKey, componentID, false);
                    // false = no longer entire system, so as to prevent infinite recursion.
                    if (visSpecsOnOtherRegistry != null) {
                        return visSpecsOnOtherRegistry;
                    }
                } catch (Exception e) {
                    System.out.println("Could not search for GUI visualization specs on registry "
                            + registryEntrySet.getValue().name);
                }
            }

            // if still here, found nothing:
            throw new RemoteException("Component " + componentID + " not found on ANY registry!");
        } else {
            return null; // (hopefully the component will be found on another registry!)
        }
    }

    private Object tryCallingComponent(String componentID,
            String methodName, Object... args) throws RemoteException {
        try {
            return rctServ.remoteCall(methodName, components.get(componentID).as, args);
        } catch (Exception e) {
            String message = this.getID() + "Could not call " + methodName
                    + " on " + componentID;
            System.err.println(message);
            e.printStackTrace();
            throw new RemoteException(message);
        }
    }

    @Override
    public ADEGuiCallHelper guiCreateGUICallHelper(SystemViewAccess accessKey,
            String componentID, boolean searchEntireSystem) throws RemoteException {
        // TODO:  may want to check access key, if want to ensure that the it's 
        //        the GUI (and with appropriate rights) that's calling this method

        // NOTE:  the registry can only create a visualization for itself 
        //     if it is registered with another registry, or with itself.
        //     Note that doing a check for componentID equaling to this.getID() is
        //     *NOT* desirable in this case, because calling 
        //     "return ADEGuiCreatorUtil.createCallHelper(this.requestComponentInfo(this).as);",
        //     would return an ADEComponentImpl rather than the proxy, hence 
        //     causing a java.io.NotSerializableException to be thrown.

        // Thus, for now, removing the check for componentID equaling to registry's own ID.
        //     as that is bound to fail, whereas searching might lead to an indirect 
        //     (and successful) reference to self.
        if (components.containsKey(componentID)) {
            return ADEGuiCreatorUtil.createCallHelper(components.get(componentID).as);

        } else if (searchEntireSystem) {
            // try to search amongst other registries:
            for (Entry<ADERegistry, ADEComponentInfo> registryEntrySet : registries.entrySet()) {
                try {
                    ADEGuiCallHelper callhelperOnOtherRegistry =
                            (ADEGuiCallHelper) rctReg.remoteCall(
                            "guiCreateGUICallHelper", registryEntrySet.getKey(),
                            accessKey, componentID, false);
                    // false = no longer entire system, so as to prevent infinite recursion.
                    if (callhelperOnOtherRegistry != null) {
                        return callhelperOnOtherRegistry;
                    }
                } catch (Exception e) {
                    System.out.println("Could not create GUI Call Helper on registry "
                            + registryEntrySet.getValue().name);
                }
            }

            // if still here, found nothing:
            throw new RemoteException("Component " + componentID + " not found on ANY registry!");
        } else {
            return null; // (hopefully the component will be found on another registry!)
        }
    }

    @Override
    public boolean guiShutdownComponent(SystemViewAccess accessKey,
            String componentID, boolean searchEntireSystem) throws RemoteException {
        // don't need to check the accessKey here, as the shutdownComponents
        //     method will perform a similar check anyway.

        if (componentID.equals(this.getID())) {
            return requestShutdown(this); // shut down the registry itself.
        } else if (components.containsKey(componentID)) {
            return shutdownComponents(accessKey.getUserID(), accessKey.getPassword(),
                    new String[][]{{"type", getTypeFromID(componentID)},
                        {"name", getNameFromID(componentID)}});
        } else if (searchEntireSystem) {
            // try to search amongst other registries:
            for (Entry<ADERegistry, ADEComponentInfo> registryEntrySet : registries.entrySet()) {
                try {
                    boolean shutdownResult =
                            (Boolean) rctReg.remoteCall(
                            "guiShutdownComponent", registryEntrySet.getKey(),
                            accessKey, componentID, false);
                    // false = no longer entire system, so as to prevent infinite recursion.
                    if (shutdownResult) { // if succeeded, and the component was found
                        return true;
                    }
                } catch (Exception e) {
                    System.out.println("Could not ask to possibly shutdown component on registry "
                            + registryEntrySet.getValue().name);
                }
            }

            // if still here, found nothing:
            throw new RemoteException("Component " + componentID + " could not be shut down, "
                    + "or was not found on ANY registry!");
        } else {
            return false; // (hopefully the component will be found on another registry!)
        }
    }

    @Override
    public String guiGetComponentInfoPrintText(SystemViewAccess accessKey,
            String componentID, boolean searchEntireSystem) throws RemoteException {
        // TODO:  may want to check access key, if want to ensure that the it's 
        //        the GUI (and with appropriate rights) that's calling this method

        if (componentID.equals(this.getID())) {
            return this.requestComponentInfo(this).toPrintString();
        } else if (components.containsKey(componentID)) {
            return components.get(componentID).toPrintString();
        } else if (searchEntireSystem) {
            // try to search amongst other registries:
            for (Entry<ADERegistry, ADEComponentInfo> registryEntrySet : registries.entrySet()) {
                try {
                    String printString = (String) rctReg.remoteCall(
                            "guiGetComponentInfoPrintText", registryEntrySet.getKey(),
                            accessKey, componentID, false);
                    // false = no longer entire system, so as to prevent infinite recursion.
                    if (printString != null) {
                        return printString;
                    }
                } catch (Exception e) {
                    System.out.println("Could not get possible component info on registry "
                            + registryEntrySet.getValue().name);
                }
            }

            // if still here, found nothing:
            throw new RemoteException("Component " + componentID + " not found on ANY registry!");
        } else {
            return null; // (hopefully the component will be found on another registry!)
        }
    }

    @Override
    public String guiGetComponentConfigText(SystemViewAccess accessKey,
            String componentID, boolean searchEntireSystem) throws RemoteException {
        // TODO:  may want to check access key, if want to ensure that the it's 
        //        the GUI (and with appropriate rights) that's calling this method

        if (componentID.equals(this.getID())) {
            return guiGetComponentConfigTextHelper(this.requestComponentInfo(this));
        } else if (components.containsKey(componentID)) {
            return guiGetComponentConfigTextHelper(components.get(componentID));
        } else if (searchEntireSystem) {
            // try to search amongst other registries:
            for (Entry<ADERegistry, ADEComponentInfo> registryEntrySet : registries.entrySet()) {
                try {
                    String configText = (String) rctReg.remoteCall(
                            "guiGetComponentConfigText", registryEntrySet.getKey(),
                            accessKey, componentID, false);
                    // false = no longer entire system, so as to prevent infinite recursion.
                    if (configText != null) {
                        return configText;
                    }
                } catch (Exception e) {
                    System.out.println("Could not get possible component config text on registry "
                            + registryEntrySet.getValue().name);
                }
            }

            // if still here, found nothing:
            throw new RemoteException("Component " + componentID + " not found on ANY registry!");
        } else {
            return null; // (hopefully the component will be found on another registry!)
        }
    }

    private String guiGetComponentConfigTextHelper(ADEComponentInfo componentInfo) {
        try {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);

            componentInfo.writeConfigStartComponentToEndComponentSection(printWriter);

            printWriter.flush();
            printWriter.close();
            stringWriter.flush();

            String result = stringWriter.toString();
            stringWriter.close();
            return result;
        } catch (IOException e) {
            return "{Could not obtain component info for " + getKey(componentInfo.type, componentInfo.name)
                    + " due to Exception " + e + "}";
        }
    }

    @Override
    public void guiLoadConfig(SystemViewAccess accessKey, String configFileName)
            throws RemoteException {
        // TODO:  may want to check access key, if want to ensure that the it's 
        //        the GUI (and with appropriate rights) that's calling this method

        // NOTE:  this code is very similar to "private void checkConfigFile()",
        //     (similar but not quite indentical enough),
        //     except the config file is not internal to the registry
        final File cfgfile = ADEGlobals.getADEConfigFile(configFileName);
        if (cfgfile != null) {
            final ADEComponentInfo myInfo = requestComponentInfo(this);
            new Thread() {
                @Override
                public void run() {
                    yield();
                    try {
                        startUpADE(myInfo, cfgfile);
                    } catch (Exception e) {
                        System.err.println("Problem parsing file: " + e);
                    }
                }
            }.start();
        } else {
            System.out.println(prg + ": Cannot find " + configFileName);
        }
    }

    @Override
    public void guiRunComponent(SystemViewAccess accessKey,
            String componentConfigString) throws RemoteException, Exception {
        // TODO:  may want to check access key, if want to ensure that the it's 
        //        the GUI (and with appropriate rights) that's calling this method

        ADEComponentInfo componentInfo = new ADEComponentInfo();

        // will want to rely on ADEComponentInfo.parseComponent() method.  But that 
        //    methods expects to have received a buffered reader where the initial
        //    "STARTCOMPONENT" has already been encountered.
        BufferedReader reader = new BufferedReader(new StringReader(componentConfigString));
        if (!(reader.readLine().equals("STARTCOMPONENT"))) {
            throw new ParseException("Component configuration must start with \"STARTCOMPONENT\".", 0);
        }
        componentInfo.parseComponent(reader, 0); // 0 = last read line, e.g., first "STARTCOMPONENT" line.

        // assuming did not throw an exception already, try to start the component:
        startComponent(accessKey.getUserID(), accessKey.getPassword(), componentInfo);
    }

    @Override
    public String guiGetComponentHelpText(SystemViewAccess accessKey,
            String componentType) throws RemoteException, Exception {
        // TODO:  could conceivably check access key, though surely this is an innocent
        //        enough request that it does not merit access control, right?!

        String minimalistHelpPleaConfig =
                "STARTCOMPONENT"
                + "\n" + "type " + componentType
                + "\n" + "componentargs --help"
                + "\n" + "ENDCOMPONENT";

        ADEComponentInfo componentInfo = new ADEComponentInfo();
        BufferedReader reader = new BufferedReader(new StringReader(minimalistHelpPleaConfig));
        // skip the very first line, of "STARTCOMPONENT". Doing it like this for consistency.
        reader.readLine();
        componentInfo.parseComponent(reader, 0); // 0 = last read line, e.g., first "STARTCOMPONENT" line.


        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);


        ADEHostInfo thisHost = this.requestHostInfo(this);
        addRegistryClassPathToComponent(thisHost, componentInfo);

        String cmd = componentInfo.buildStartCommand(thisHost);
        ADELocalProcessStarter helpStarter = new ADELocalProcessStarter(thisHost,
                this.requestComponentInfo(this).startdirectory, cmd);
        helpStarter.setOutputStream(printStream);
        helpStarter.startProcess();

        // wait for the help text to display.  hopefully it will display reasonably
        //      soon, because otherwise a remote exception will occur!  thankfully,
        //      seems to only take it a second or so, so no problem.
        while (helpStarter.isRunning()) {
            Thread.sleep(100);
        }

        // once the process has completed, return everything that has piled up
        //      on the output stream -- namely, the contents of the help!
        return outputStream.toString("UTF8");
    }

    @Override
    public UUID guiRegisterOutputRedirectionConsumer(
            SystemViewAccess accessKey, String componentID,
            boolean searchEntireSystem) throws RemoteException, IOException {
        // TODO:  may want to check access key, if want to ensure that the it's 
        //        the GUI (and with appropriate rights) that's calling this method

        if (componentID.equals(this.getID())) {
            return this.registerOutputRedirectionConsumer(this);
        } else if (components.containsKey(componentID)) {
            return (UUID) tryCallingComponent(componentID,
                    "registerOutputRedirectionConsumer", this);
        } else if (searchEntireSystem) {
            // try to search amongst other registries:
            for (Entry<ADERegistry, ADEComponentInfo> registryEntrySet : registries.entrySet()) {
                try {
                    UUID outputRegistrationResult =
                            (UUID) rctReg.remoteCall(
                            "guiRegisterOutputRedirectionConsumer",
                            registryEntrySet.getKey(),
                            accessKey, componentID, false);
                    // false = no longer entire system, so as to prevent infinite recursion.
                    if (outputRegistrationResult != null) { // if succeeded, return the ID:
                        return outputRegistrationResult;
                    }
                } catch (Exception e) {
                    System.out.println("Could not ask to register output consumer on registry "
                            + registryEntrySet.getValue().name);
                }
            }

            // if still here, found nothing:
            throw new RemoteException("Component " + componentID + " could not initialize "
                    + "output redirection, or was not found on ANY registry!");
        } else {
            return null; // (hopefully the component will be found on another registry!)
        }
    }

    @Override
    public boolean guiDeregisterOutputRedirectionConsumer(
            SystemViewAccess accessKey, String componentID, UUID consumerID,
            boolean searchEntireSystem) throws RemoteException {
        // TODO:  may want to check access key (though de-registering 
        //     should actually be a pretty innocent method, especially since it requires 
        //     a consumer ID that could only have been obtained through honest registration).

        if (componentID.equals(this.getID())) {
            this.deregisterOutputRedirectionConsumer(consumerID);
            return true; // found the component, done.
        } else if (components.containsKey(componentID)) {
            tryCallingComponent(componentID, "deregisterOutputRedirectionConsumer", consumerID);
            return true;
        } else if (searchEntireSystem) {
            // try to search amongst other registries:
            for (Entry<ADERegistry, ADEComponentInfo> registryEntrySet : registries.entrySet()) {
                try {
                    boolean foundAndDeRegisteredComponent =
                            (Boolean) rctReg.remoteCall(
                            "guiDeregisterOutputRedirectionConsumer",
                            registryEntrySet.getKey(),
                            accessKey, componentID, consumerID, false);
                    // false = no longer entire system, so as to prevent infinite recursion.
                    if (foundAndDeRegisteredComponent) { // if succeeded, return the ID:
                        return true; // done!
                    }
                } catch (Exception e) {
                    System.out.println("Could not ask to de-register output consumer on registry "
                            + registryEntrySet.getValue().name);
                }
            }

            // if still here, found nothing:
            throw new RemoteException("Component " + componentID + " could not de-register "
                    + "output redirection, or was not found on ANY registry!");
        } else {
            return false; // (hopefully the component will be found on another registry!)
        }
    }

    @Override
    public String guiGetAccumulatedRedirectedOutput(SystemViewAccess accessKey,
            String componentID, UUID consumerID, boolean searchEntireSystem)
            throws RemoteException {
        // TODO:  may want to check access key (though since output-fetching
        //     requires the possesion of a valid unique consumerID, the access
        //     check is probably not necessary).

        if (componentID.equals(this.getID())) {
            return this.getAccumulatedRedirectedOutput(consumerID);
        } else if (components.containsKey(componentID)) {
            return (String) tryCallingComponent(componentID,
                    "getAccumulatedRedirectedOutput", consumerID);
        } else if (searchEntireSystem) {
            // try to search amongst other registries:
            for (Entry<ADERegistry, ADEComponentInfo> registryEntrySet : registries.entrySet()) {
                try {
                    String foundAccumulatedOutput =
                            (String) rctReg.remoteCall(
                            "guiGetAccumulatedRedirectedOutput",
                            registryEntrySet.getKey(),
                            accessKey, componentID, consumerID, false);
                    // false = no longer entire system, so as to prevent infinite recursion.
                    if (foundAccumulatedOutput != null) { // if succeeded, return the ID:
                        return foundAccumulatedOutput;
                    }
                } catch (Exception e) {
                    System.out.println("Could not ask to fetch redirected output on registry "
                            + registryEntrySet.getValue().name);
                }
            }

            // if still here, found nothing:
            throw new RemoteException("Component " + componentID + " could not fetch"
                    + "redirected output, or was not found on ANY registry!");
        } else {
            return null; // (hopefully the component will be found on another registry!)
        }
    }
}
