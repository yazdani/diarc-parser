/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 * Last update: April 2010
 * 
 * HostMonitorComponentImpl.java
 */
package com.hostmonitor;

import ade.*;
import java.lang.reflect.Constructor;
import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.util.*;

/**
The implementation of an {@link ade.ADEComponentImpl ADEComponentImpl} that
will maintain information about hosts known to the ADE system using Java 1.5.

@author Jim Kramer
 */
public class HostMonitorComponentImpl extends ADEComponentImpl
        implements HostMonitorComponent {

    private static String prg = "HostMonitorComponentImpl";
    private static boolean debug = false;
    private static boolean verbose = false;
    /** Whether to log data (set via command line). */
    static public boolean useLogger = false;
    /** Filename in which to store log data (set via command line). */
    static public String logFileName = "hostmonitor.log";
    private Object logger;
    private boolean gotLogger = false;
    private String myID;
    /** Whether to automatically get the host list from the {@link
     * ade.ADERegistry ADERegistry} with which this monitor is registered.
     * Note that setting {@link #getHostsFromAllReg} to <tt>true</tt>
     * also sets this to <tt>true</tt>. */
    static public boolean getHostsFromMyReg = true;
    /** Whether to automatically get the host list from all {@link
     * ade.ADERegistry ADERegistry} in the system. Note that setting this to
     * <tt>true</tt> also sets {@link #getHostsFromAllReg} to <tt>true</tt>. */
    static public boolean getHostsFromAllReg = false;
    /** Whether to automatically start monitoring hosts. */
    static public boolean startMonitor = false;
    /** Remove scripts from remote hosts upon exit. */
    static public boolean removeScripts = true;
    /** A self-reference for the GUI panel. */
    public HostMonitorComponent myself;
    /** The hosts being monitored. These are kept separately from the
     * <tt>MonitorTask</tt> inner class (which contains a reference to
     * the {@link ade.ADEHostStatus ADEHostStatus} object) to provide
     * persistence if checking of a particular host is halted. */
    private Map<String, ADEHostStatus> hosts;  // key on host address
    /** The monitor tasks that are being executed. */
    private Map<ADEHostStatus, MonitorTask> tasks;
    /** The {@link java.util.Timer Timer} used to schedule <tt>MonitorTask</tt>s
     * that periodically obtain host status information (via an {@link
     * ade.ADEHostStatus ADEHostStatus} object). */
    private Timer monitorTimer;

    /** The enumeration of {@link ade.ADEHostStatus ADEHostStatus} subclasses
     * to instantiate that matches a host's operating system (must match the
     * {@link ade.ADEGlobals.HostOS HostOS} enumeration). */
    public static enum HostStatusClass {

        /** The Linux status class. */
        LINUX(ADEGlobals.HostOS.LINUX, "ade.ADEHostStatusLinux"),
        /** The enum constant for FreeBSD. */
        FREEBSD(ADEGlobals.HostOS.FREEBSD, "ade.ADEHostStatusLinux"),
        /** The enum constant for Sun's Solaris. */
        SOLARIS(ADEGlobals.HostOS.SOLARIS, "ade.ADEHostStatusLinux"),
        /** The enum constant for Apple's OS-X. */
        OSX(ADEGlobals.HostOS.MACOSX, "ade.ADEHostStatusLinux"),
        /** The enum constant for Cygwin on Windows. */
        CYGWIN(ADEGlobals.HostOS.CYGWIN, "ade.ADEHostStatusLinux"),
        /** The Windows XP status class. */
        WINDOWSXP(ADEGlobals.HostOS.WINDOWSXP, "ade.ADEHostStatusWindowsXP");
        private final ADEGlobals.HostOS os;
        private final String statusClass;

        HostStatusClass(ADEGlobals.HostOS o, String cn) {
            os = o;
            statusClass = cn;
        }

        /** Returns the operating system name as specified in {@link
         * ade.ADEGlobals.HostOS HostOS}. */
        public String getOSName() {
            return os.osname();
        }

        /** Returns the operating system enumeration constant as specified in
         * {@link ade.ADEGlobals.HostOS HostOS}. */
        public ADEGlobals.HostOS getOS() {
            return os;
        }

        /** Returns the name of the class to use for obtaining status info. */
        public String getStatusClassName() {
            return statusClass;
        }

        /** Returns a string representation. */
        public String toString() {
            return (os.osname() + ":" + statusClass);
        }

        /** Returns a newly instantiated object for getting host information. */
        public ADEHostStatus getStatusClass(String myTmpDir, ADEHostInfo ahi) {
            ADEHostStatus ahs = null;
            Constructor cons;
            try {
                Class c = Class.forName(statusClass);
                cons = c.getConstructor(myTmpDir.getClass(), ahi.getClass());
                ahs = (ADEHostStatus) cons.newInstance(myTmpDir, ahi);
            } catch (Exception e) {
                System.err.println("Exception creating " + this + " status class:");
                System.err.println(e);
            }
            return ahs;
        }
    }

    /** Return the status class enumeration constant associated with the
     * supplied operating system identifier.
     * @param get The operating system name
     * @return The host status class enumeration constant or <tt>null</tt>
     * if none found */
    public static final HostStatusClass getHostStatusClassByName(String get) {
        Set<HostStatusClass> set = EnumSet.allOf(HostStatusClass.class);
        for (HostStatusClass hsc : set) {
            if (get == hsc.getOSName()) {
                return hsc;
            }
        }
        return null;
    }

    /** Return the status class enumeration constant associated with the
     * supplied operating system enumeration constant.
     * @param get The operating system enumeration constant as specified
     * by {@link ade.ADEGlobals.HostOS HostOS}
     * @return The host status class enumeration constant or <tt>null</tt>
     * if none found */
    public static final HostStatusClass getHostStatusClassByOS(ADEGlobals.HostOS get) {
        Set<HostStatusClass> set = EnumSet.allOf(HostStatusClass.class);
        for (HostStatusClass hsc : set) {
            if (get == hsc.getOS()) {
                return hsc;
            }
        }
        return null;
    }

    // Should this be moved to (perhaps) ADEGlobals?
    /** The enumeration of CPU types and related information. */
    public static enum CPUPower {

        PPC7400("IBM", "7400e", 1000, 30.0, 1.0),
        XSCALE("Intel", "PX25A", 600, 0.5, 1.0),
        PI75("Intel", "Pentium", 75, 6.0, 3.3),
        PI90("Intel", "Pentium", 90, 7.3, 3.3),
        PI100("Intel", "Pentium", 100, 8.0, 3.3),
        PMMX166("Intel", "PentiumMMX", 166, 7.8, 2.8),
        PMMX200("Intel", "PentiumMMX", 200, 15.7, 2.8),
        //PPRO150   ("Intel", "PentiumPro", 150, ?, 3.1),
        //PPRO166   ("Intel", "PentiumPro", 166, ?, 3.3),
        PII233("Intel", "PentiumII", 233, 34.8, 2.8),
        PII266("Intel", "PentiumII", 266, 38.6, 2.8),
        PII450("Intel", "PentiumII", 450, 27.1, 2.8),
        PIII450("Intel", "PentiumIII", 450, 25.3, 2.1),
        PIII600("Intel", "PentiumIII", 600, 34.5, 2.1),
        PIII700("Intel", "PentiumIII", 700, 21.9, 1.7),
        PIII733("Intel", "PentiumIII", 733, 22.8, 1.7),
        PIII850("Intel", "PentiumIII", 850, 25.7, 1.7),
        PIII866("Intel", "PentiumIII", 866, 26.1, 1.7),
        PIII1000("Intel", "PentiumIII", 1000, 29.0, 1.7),
        PIII1266("Intel", "PentiumIII", 1266, 29.5, 1.7),
        P42400("Intel", "Pentium4", 2400, 67.6, 1.8),
        P43060("Intel", "Pentium4", 3060, 81.8, 1.8),
        P43600("Intel", "Pentium4", 3600, 115.0, 1.8),
        PM1200("Intel", "PentiumM", 1200, 21.0, 1.4),
        PM1500("Intel", "PentiumM", 1500, 21.0, 1.4),
        PM1600("Intel", "PentiumM", 1600, 27.0, 1.4),
        PM1700("Intel", "PentiumM", 1700, 21.0, 1.4),
        PM1730("Intel", "PentiumM", 1730, 27.0, 1.4),
        PM1800("Intel", "PentiumM", 1800, 21.0, 1.4),
        PM1860("Intel", "PentiumM", 1860, 27.0, 1.4),
        PM2000("Intel", "PentiumM", 2000, 27.0, 1.4),
        PM2100("Intel", "PentiumM", 2100, 21.0, 1.4),
        PM2130("Intel", "PentiumM", 2130, 27.0, 1.4),
        PM2160("Intel", "PentiumM", 2160, 27.0, 1.4),
        //AMDK5     ("AMD", "K5", ?, ?, 3.5),
        //AMDK6     ("AMD", "K6", ?, ?, 2.9),
        //AMDK62    ("AMD", "K6-2", ?, ?, 2.0),
        //AMDK63    ("AMD", "K6-3", ?, ?, 2.2),
        AMDTB750("AMD", "Athlon", 750, 43.8, 1.8),
        AMDTB800("AMD", "Athlon", 800, 45.5, 1.8),
        AMDTB850("AMD", "Athlon", 850, 48.0, 1.8),
        AMDTB900("AMD", "Athlon", 900, 50.7, 1.8),
        AMDTB950("AMD", "Athlon", 950, 52.5, 1.8),
        AMDTB1000("AMD", "Athlon", 1000, 54.3, 1.8),
        AMDTB1400("AMD", "Athlon", 1400, 73.5, 1.8),
        AMDXP1900("AMD", "AthlonXP", 1600, 68.1, 1.7),
        AMDXP2000("AMD", "AthlonXP", 1667, 70.5, 1.7),
        AMDXP2100("AMD", "AthlonXP", 1733, 72.0, 1.7),
        AMDXP2200("AMD", "AthlonXP", 1800, 67.0, 1.7),
        AMDXP2600("AMD", "AthlonXP", 1850, 68.3, 1.7),
        AMDXP3200("AMD", "AthlonXP", 1900, 76.8, 1.7);
        private final String vendor;
        private final String model;
        private final int mhz;
        private final double watts;
        private final double vcore;

        CPUPower(String v, String m, int s, double w, double vc) {
            vendor = v;
            model = m;
            mhz = s;
            watts = w;
            vcore = vc;
        }

        /** The CPU vendor. */
        public String vendor() {
            return vendor;
        }

        /** The CPU model. */
        public String model() {
            return model;
        }

        /** The CPU speed (in MHz). */
        public int mhz() {
            return mhz;
        }

        /** The CPU wattage at max load. */
        public double watts() {
            return watts;
        }

        /** The CPU core voltage. */
        public double vcore() {
            return vcore;
        }

        /** Return the power used (in mA), assuming 100% usage. */
        public double mAmps() {
            return ((watts / vcore) / 1000.0);
        }
    }

    class MonitorTask extends TimerTask {

        String id;           // for display/debug
        ADEHostStatus ahs;
        boolean init = false;
        long lastChecked = System.currentTimeMillis();

        public MonitorTask(ADEHostStatus a) {
            ahs = a;
            id = "MonitorTask:" + ahs.getIP();
        }

        public void run() {
            if (!init) {
                try {
                    init = ahs.createScripts();
                } catch (Exception e) {
                    System.err.println(id + ": " + e);
                }
            } else {
                // if the host isn't available, probe it
                if (!ahs.isAvailable()) {
                    try {
                        ahs.probeHost();
                    } catch (Exception e) {
                        System.err.println(id + ": " + e);
                    }
                }
                // if the host is now available, get the stats
                if (ahs.isAvailable()) {
                    try {
                        ahs.getHostStats();
                    } catch (Exception e) {
                        System.err.println(id + ": " + e);
                    }
                }
            }
            if (verbose || dbg > 6) {
                System.out.println(toString());
            }
            lastChecked = System.currentTimeMillis();
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            long last = System.currentTimeMillis() - lastChecked;
            sb.append(id);
            sb.append(", checked ");
            sb.append(last);
            sb.append(" ms ago\n");
            sb.append(ahs.toString());
            return sb.toString();
        }
        /* Not needed?
        public int getPeriod() {
        return ahs.getPeriod();
        }

        public void setPeriod(int p) {
        ahs.setPeriod(p);
        }
         */
    }

    // ********************************************************************
    // *** abstract methods in ADEComponentImpl that need to be implemented
    // ********************************************************************
    /** This method will be activated whenever a client calls the
     * requestConnection(uid) method. Any connection-specific initialization
     * should be included here, either general or user-specific.
     * @param user the ID of the user/client that gained a connection */
    protected void clientConnectReact(String user) {
        if (dbg > 0) {
            System.out.println(myID + ": got connection from " + user + "!");
        }
    }

    /** This method will be activated whenever a client that has called the
     * requestConnection(uid) method fails to update (meaning that the
     * heartbeat signal has not been received by the reaper), allowing both
     * general and user specific reactions to lost connections. If it returns
     * true, the client's connection is removed.
     * @param user the ID of the user/client that lost a connection */
    protected boolean clientDownReact(String user) {
        if (dbg > 0) {
            System.out.println(myID + ": lost connection with " + user + "!");
        }
        return false;
    }

    /** This method will be activated whenever the heartbeat returns a
     * remote exception (i.e., the server this is sending a
     * heartbeat to has failed).
     * @param s the type of {@link ade.ADEComponent ADEComponent} that failed */
    protected void componentDownReact(String serverkey, String[][] constraints) {
        String s = constraints[0][1];

        if (dbg > 0) {
            System.out.println(myID + ": reacting to down " + s + " server...");
        }
        if (s.indexOf("LoggerComponent") >= 0) {
            gotLogger = false;
        }
        return;
    }

    /** This method will be activated whenever the heartbeat reconnects
     * to a client (i.e., the server this is sending a heartbeat to has
     * failed and then recovered).
     * @param s the type of {@link ade.ADEComponent ADEComponent} that reconnected */
    protected void componentConnectReact(String serverkey, Object ref, String[][] constraints) {
        String s = constraints[0][1];

        if (dbg > 0) {
            System.out.println(myID + ": reacting to connected " + s + " server...");
        }
        if (s.indexOf("LoggerComponent") >= 0) {
            gotLogger = true;
            try {
                call(ref, "openOutputFile", myID, logFileName);
                if (dbg > 0) {
                    System.out.println(prg + ": opened log file " + logFileName + "...");
                }
            } catch (Exception re) {
                System.err.println(prg + ": exception opening log file...");
                System.err.println("\t" + re);
            }
        }
        return;
    }

    /** Adds additional local checks for credentials before allowing a shutdown.
     * @param credentials the Object presented for verification
     * @return must return "false" if shutdown is denied, true if permitted */
    protected boolean localrequestShutdown(Object credentials) {
        return false;
    }

    /** Implements the local shutdown mechanism that derived classes need to
     * implement to cleanly shutdown. */
    protected void localshutdown() {
        // since we're shutting down, we ignore exceptions; use multiple
        // try/catches so that if one thing bombs, we still do the others
        System.out.print("Shutting Down " + myID + "...");
        if (gotLogger) {
            System.out.print("closing log...");
            try {
                call(logger, "closeOutputFile", myID);
            } catch (Exception ignore) {
            }
        }
        System.out.print("cancelling timer...");
        monitorTimer.cancel();
        if (removeScripts) {
            System.out.print("removing scripts...");
            for (ADEHostStatus ahs : hosts.values()) {
                try {
                    ahs.removeScripts();
                } catch (Exception ignore) {
                }
            }
        }
        System.out.println("done.");
    }

    // **********************************************************************
    // *** Methods of the HostMonitorSensorComponent available for remote execution
    // **********************************************************************
    /** Parses a file containing host information and adds the hosts
     * to the host list.
     * @param file the name of a properly formatted hosts configuration file
     * @param credentials determines whether the caller is allowed
     * to access the host list
     * @throws AccessControlException if <tt>credentials</tt> are
     * insufficient
     * @throws RemoteException if the remote call fails */
    public int loadHostFile(String file, Object credentials)
            throws RemoteException, AccessControlException {
        // TODO: parsing of host file is found in ADEHostInfo
        return 0;
    }

    /** Returns the list of known hosts.
     * @param credentials determines whether the caller is allowed
     * to access the host list
     * @return an {@link java.util.ArrayList ArrayList} of hosts
     * @throws AccessControlException if <tt>credentials</tt> are
     * insufficient
     * @throws RemoteException if the remote call fails */
    public ArrayList<String> getHostList(Object credentials)
            throws RemoteException, AccessControlException {
        // TODO: check credentials
        return new ArrayList<String>(hosts.keySet());
    }

    /** Starts the update for all hosts.
     * @param credentials determines whether the caller is allowed
     * to access the host monitor
     * @throws AccessControlException if <tt>credentials</tt> are
     * insufficient
     * @throws RemoteException if the remote call fails */
    public void startHostChecking(Object credentials)
            throws RemoteException, AccessControlException {
        // TODO: check credentials
        for (String host : hosts.keySet()) {
            try {
                startHostChecking(host, credentials);
            } catch (Exception e) {
                System.err.println(myID + ": Exception starting monitor for " + host);
            }
        }
    }

    /** Starts the update for a host.
     * @param host the name or IP address of a host
     * @param credentials determines whether the caller is allowed
     * to access the host list
     * @throws AccessControlException if <tt>credentials</tt> are
     * insufficient
     * @throws RemoteException if the remote call fails */
    public void startHostChecking(String host, Object credentials)
            throws RemoteException, AccessControlException {
        ADEHostStatus ahs;

        // TODO: check credentials
        if ((ahs = hosts.get(host)) == null) {
            throw new RemoteException("Unknown host " + host);
        }
        MonitorTask mt = new MonitorTask(ahs);
        tasks.put(ahs, mt);
        monitorTimer.schedule(mt, 0, ahs.getPeriod());
        if (dbg > 4 || verbose) {
            System.out.println(prg + ": starting checking of " + host);
        }
    }

    /** Stops the update for all hosts.
     * @param credentials determines whether the caller is allowed
     * to access the host monitor
     * @throws AccessControlException if <tt>credentials</tt> are
     * insufficient
     * @throws RemoteException if the remote call fails */
    public void stopHostChecking(Object credentials)
            throws RemoteException, AccessControlException {
        // TODO: check credentials
        for (String host : hosts.keySet()) {
            stopHostChecking(host, credentials);
        }
    }

    /** Stops the update for a host.
     * @param host the name or IP address of a host
     * @param credentials determines whether the caller is allowed
     * to access the host list
     * @throws AccessControlException if <tt>credentials</tt> are
     * insufficient
     * @throws RemoteException if the remote call fails */
    public void stopHostChecking(String host, Object credentials)
            throws RemoteException, AccessControlException {
        // TODO: check credentials
        MonitorTask mt = tasks.get(hosts.get(host));
        if (mt != null) {
            mt.cancel();
            if (dbg > 4 || verbose) {
                System.out.println(prg + ": stopped checking " + host);
            }
        }
    }

    /** Changes the update rate (aka, the period) of host checking.
     * @param host the name or IP address of a host
     * @param p The new update rate
     * @param credentials determines whether the caller is allowed
     * to access the host list
     * @throws AccessControlException if <tt>credentials</tt> are
     * insufficient
     * @throws RemoteException if the remote call fails */
    public void setHostCheckingPeriod(String host, long p, Object credentials)
            throws RemoteException, AccessControlException {
        // TODO: check credentials
        ADEHostStatus ahs = hosts.get(host);
        if (ahs == null) {
            throw new RemoteException("Unknown host " + host);
        } else {
            MonitorTask mt = tasks.get(ahs);
            if (mt != null) {
                mt.cancel();
            }
            ahs.setPeriod(p);
            startHostChecking(host, credentials);
        }
    }

    // **********************************************************************
    // *** Methods of the HostMonitor available for remote execution
    // **********************************************************************
    /** Returns the statistics of the hosts being monitored.
     * @return An {@link java.util.ArrayList ArrayList} of {@link
     * ade.ADEMiniHostStats ADEMiniHostStats}
     * @throws AccessControlException if <tt>credentials</tt> are
     * insufficient
     * @throws RemoteException if the remote call fails */
    public ArrayList<ADEMiniHostStats> getHostStats(Object credentials)
            throws RemoteException, AccessControlException {
        // TODO: check credentials
        ArrayList<ADEMiniHostStats> retlist =
                new ArrayList<ADEMiniHostStats>(hosts.size());
        for (ADEHostStatus ahs : hosts.values()) {
            retlist.add(ahs.getADEMiniHostStats());
        }
        return retlist;
    }

    /** Adds a host to the list. Note that this method does <b>not</b> start
     * monitoring the host; to start monitoring, use the {@link
     * #startHostChecking} method.
     * @param host The {@link ade.ADEHostInfo ADEHostInfo} structure for the
     * remote host
     * @param credentials determines whether the caller is allowed
     * to access the host list
     * @return <tt>true</tt> if the host was not already in the list and
     * was successfully added, <tt>false</tt> otherwise
     * @throws AccessControlException if <tt>credentials</tt> are
     * insufficient
     * @throws RemoteException if the remote call fails */
    public boolean addHost(ADEHostInfo host, Object credentials)
            throws RemoteException, AccessControlException {
        ADEHostStatus ahs = (getHostStatusClassByOS(host.getOS())).getStatusClass(getTempDir(), host);

        // TODO: add credentials check
        if (ahs == null) {
            throw new RemoteException("Unknown host os: " + host.getOS());
        }
        hosts.put(host.hostip, ahs);
        if (dbg > 4 || verbose) {
            System.out.println(prg + ": added host " + host);
        }
        return true;
    }

    /** Removes a host from the list.
     * @param host the name or IP address of a host
     * @param credentials determines whether the caller is allowed
     * to access the host list
     * @return <tt>true</tt> if the host existed and was removed,
     * <tt>false</tt> otherwise
     * @throws AccessControlException if <tt>credentials</tt> are
     * insufficient
     * @throws RemoteException if the remote call fails */
    public boolean removeHost(String host, Object credentials)
            throws RemoteException, AccessControlException {
        boolean removed = false;

        // TODO: check credentials
        ADEHostStatus ahs = hosts.remove(host);
        if (ahs != null) {
            MonitorTask mt = tasks.remove(ahs);
            if (mt != null) {
                mt.cancel();
            }
            removed = true;
            if (dbg > 4 || verbose) {
                System.out.println(prg + ": removed host " + host);
            }
        }
        return removed;
    }

    // **********************************************************************
    // *** Methods for GUI creation handling
    // **********************************************************************
    /** Provides command line argument descriptions.
     * @return command line argument switches and descriptions */
    protected String additionalUsageInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("     -log  --uselogger [logname] <set useLogger to " + !useLogger + ">\n");
        sb.append("     -reg  --myreg <automatically get hosts known to registry>\n");
        sb.append("     -all  --allreg <automatically get hosts known to all registries>\n");
        sb.append("     -auto --automatic <automatically start monitoring hosts>\n");
        sb.append("     -rem  --removescrips <automatically remove the scripts upon exiting>\n");
        return sb.toString();
    }

    /** Parses command line arguments specific to this ADEComponent.
     * @param args the custom command line arguments
     * @return <tt>true</tt> if all <tt>args</tt> are recognized,
     * <tt>false</tt> otherwise */
    protected boolean parseadditionalargs(String[] args) {
        boolean found = false;

        for (int i = 0; i < args.length; i++) {
            if (verbose) {
                System.out.println(myID + ": got argument [" + args[i] + "]");
            }
            if (args[i].equalsIgnoreCase("-log")
                    || args[i].equals("--uselogger")) {
                useLogger = !useLogger;
                try {
                    // try to get a filename; if arg starts with "-", assume
                    // it's a switch and no filename was given
                    String tmp = args[++i];
                    if (!tmp.startsWith("-")) {
                        logFileName = tmp;
                        if (verbose || dbg > 0) {
                            System.out.println(prg + ": set log to " + logFileName);
                        }
                    } else {
                        --i;
                    }
                } catch (Exception e) {
                    if (verbose || dbg > 0) {
                        System.err.println(prg + ": No log filename supplied; using " + logFileName);
                    }
                }
                found = true;
            } else if (args[i].equalsIgnoreCase("-reg")
                    || args[i].equals("--myreg")) {
                getHostsFromMyReg = true;
                getHostsFromAllReg = false;
                found = true;
            } else if (args[i].equalsIgnoreCase("-all")
                    || args[i].equals("--allreg")) {
                getHostsFromMyReg = true;
                getHostsFromAllReg = true;
                found = true;
            } else if (args[i].equalsIgnoreCase("-auto")
                    || args[i].equals("--automatic")) {
                startMonitor = true;
                found = true;
            } else if (args[i].equalsIgnoreCase("-rem")
                    || args[i].equals("--removescripts")) {
                removeScripts = false;
                found = true;
            } else {
                System.err.println(myID + ": failed on argument [" + args[i] + "]");
                return false;  // return on any unrecognized args
            }
        }
        return found;
    }

    /** The server is always ready to provide its service after it has come up */
    protected boolean localServicesReady() {
        return true;
    }

    /** Constructor. Note that {@link #finishedInitialization} must be
     * called before an ADEComponent can register with an {@link
     * ade.ADERegistryImpl ADERegistry}.
     * @throws RemoteException if the server cannot be instantiated
     * @see #additionalUsageInfo for command line arguments */
    public HostMonitorComponentImpl() throws RemoteException {
        super();
        myself = this;
        // any startup initialization would go here
        hosts = Collections.synchronizedMap(new HashMap<String, ADEHostStatus>());
        tasks = Collections.synchronizedMap(new HashMap<ADEHostStatus, MonitorTask>());
        monitorTimer = new Timer(true);
        //finishedInitialization();
        // we can use "getHostsFromMyReg" because it is true if we
        // automatically start anything
        if (getHostsFromMyReg) {
            ArrayList<ADEHostInfo> hostlist;
            if (getHostsFromAllReg) {
                hostlist = getHostListAll();
            } else {
                hostlist = getHostList();
            }
            ADEHostStatus ahs;
            for (ADEHostInfo ahi : hostlist) {
                ahs = (getHostStatusClassByOS(ahi.getOS())).getStatusClass(getTempDir(), ahi);
                hosts.put(ahi.hostip, ahs);
            }
            if (startMonitor) {
                new Thread() {

                    public void run() {
                        try {
                            Thread.sleep(10);
                        } catch (Exception ignore) {
                        }
                        try {
                            startHostChecking(this);
                        } catch (Exception e) {
                            System.err.println(myID + ": Exception starting monitor");
                            System.err.println(e);
                        }
                    }
                }.start();
            }
        }
        //startExecution();
    }

    protected void updateComponent() {}
    protected void updateFromLog(String logEntry) {}

    /*
    final public static void main(String args[]) throws Exception {
        ADEComponentImpl.main(args);
    }
     * 
     */
}	
