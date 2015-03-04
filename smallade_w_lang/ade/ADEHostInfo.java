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

import java.io.*;
import java.net.*;
import java.text.ParseException;
import java.util.*;

/**
 * The information about a computational host in ADE. This class is mostly a
 * data structure, for use by an {@link ade.ADERegistryImpl ADERegistryImpl},
 * that contains information about hosts in an ADE system. It also provides some
 * methods to use that information (e.g., start ADE components using the
 * {@link #startComponent} method or make a determination if a host is reachable
 * using the {@link #reachable} method). <p> Unless fully supplied to a
 * constructor, information is filled using one of the <tt>setDefaults</tt>
 * methods, which extracts settings from the {@link
 * ade.ADEGlobals.HostOS HostOS} and {@link HostDefaultInfo} enumerations (the
 * default operating system is stored in {@link ade.ADEPreferences#HOSTOS}).
 * This information can be modified using the {@link #parseHost} method, which
 * accepts input from a {@link java.io.BufferedReader BufferedReader} that uses
 * the following key/value format (all lines except <tt>STARTHOST</tt> and
 * <tt>ENDHOST</tt> are optional; blank lines and those starting with "#" are
 * ignored as comments): <p> <tt>STARTHOST<br> ip IP address or hostname<br> os
 * operating system name<br> cpus number of CPUs (default=1)<br> cpumhz CPU
 * speeds (default=850MHz)<br> memmb available memory (default=256MB)<br>
 * adehome ADE home directory<br> tempdir temporary directory (e.g., /tmp)<br>
 * javadir Java home directory<br> javabin Java executable<br> javacompiler Java
 * compiler<br> shell command-line shell (e.g., /bin/sh)<br> shellargs shell
 * arguments (e.g., -c)<br> ping ping command (e.g., /bin/ping)<br> pingargs
 * ping arguments (e.g., -c 2)<br> rsh remote shell command (e.g., /bin/rsh)<br>
 * rshargs remote shell arguments (e.g., -X -A -n)<br> rcp remote copy command
 * (e.g., /bin/rcp)<br> rcpargs remote copy arguments (e.g., -X -A -n)<br> ssh
 * remote secure shell command (e.g., /bin/ssh)<br> sshargs remote secure shell
 * arguments (e.g., -X -A -n)<br> scp remote secure copy command (e.g.,
 * /bin/rcp)<br> scpargs remote secure copy arguments (e.g., -X -A -n)<br>
 * sshlogin ssh login username<br> ps process information command (e.g.,
 * /bin/ps)<br> psargs process information command (e.g., -f)<br> devices
 * interfaces supported on host<br> gpslocation the GPS coordinates of the
 * host's location<br> gpsformat the GPS format used (WGS84,degminsec,GPS),
 * default GPS<br> locationname the name of the location of this host<br>
 * ENDHOST<br><tt>
 *
 * This information can be written to a file using the {@link #createHostsFile}
 * method or printed to the console using the {@link #print} method. <p> At this
 * point, neither the <tt>rsh</tt> nor the <tt>rcp</tt> settings are used, as
 * both present security risks not present with the usual <tt>ssh</tt> and
 * <tt>scp</tt> commands. Rather, they are included for completeness and
 * possible extension, but may be removed in the future. In addition, it may be
 * of benefit in the future to make this a Runnable object that resides on its
 * host, disseminating status information.
 */
final public class ADEHostInfo implements Serializable, Cloneable {
    // for debugging

    final static String prg = "ADEHostInfo";
    final static boolean debug = false;
    final static boolean verbose = false;
    // included so we can later read files even if the class changed...
    private static final long serialVersionUID = 7526472295622776147L;

    /**
     * The recognized operating systems and default settings. OS names are
     * stored in the {@link ade.ADEGlobals.HostOS HostOS} enumeration located in
     * {@link ade.ADEGlobals ADEGlobals}. Note that flavors of Windows are not
     * distinguished and that ADE has not necessarily been tested on the listed
     * OSes.
     */
    static public enum HostDefaultInfo {

        /**
         * Defaults for Linux.
         */
        LINUX(ADEGlobals.HostOS.LINUX, "/bin/sh", "-c", "&", "/bin/ping", "-c 1 -W 2", "/usr/bin/rsh", "-l", "/usr/bin/rcp", "", "/usr/bin/ssh", "-X -A -n", "/usr/bin/scp", "", "/bin/ps", "-f -A", "/bin/kill", "/bin/cp", "/bin/rm", "/bin/mkdir", "", "GPS", ""),
        /**
         * Defaults for FreeBSD.
         */
        FREEBSD(ADEGlobals.HostOS.FREEBSD, "/bin/sh", "-c", "&", "/bin/ping", "-c 1 -w 5", "/usr/bin/rsh", "-l", "/usr/bin/rcp", "", "/usr/bin/ssh", "-X -A -n", "/usr/bin/scp", "", "/bin/ps", "-f -U", "/bin/kill", "/bin/cp", "/bin/rm", "/bin/mkdir", "", "GPS", ""),
        /**
         * Defaults for Solaris.
         */
        SOLARIS(ADEGlobals.HostOS.SOLARIS, "/bin/sh", "-c", "&", "/usr/sbin/ping", "", "/usr/bin/rsh", "-l", "/usr/bin/rcp", "", "/bin/ssh", "-X -A -n", "/bin/scp", "", "/bin/ps", "-f -U", "/bin/kill", "/bin/cp", "/bin/rm", "/bin/mkdir", "", "GPS", ""),
        /**
         * Defaults for alternative Solaris.
         */
        SUNOS(ADEGlobals.HostOS.SUNOS, "/bin/sh", "-c", "&", "/usr/sbin/ping", "", "/usr/bin/rsh", "-l", "/usr/bin/rcp", "", "/bin/ssh", "-X -A -n", "/bin/scp", "", "/bin/ps", "-f -U", "/bin/kill", "/bin/cp", "/bin/rm", "/bin/mkdir", "", "GPS", ""),
        /**
         * Defaults for OS X.
         */
        MACOSX(ADEGlobals.HostOS.MACOSX, "/bin/sh", "-c", "&", "/bin/ping", "", "/usr/bin/rsh", "", "/usr/bin/rcp", "", "/usr/bin/ssh", "-X -A -n", "/usr/bin/scp", "", "/bin/ps", "-f -U", "/bin/kill", "/bin/cp", "/bin/rm", "/bin/mkdir", "", "GPS", ""),
        /**
         * Defaults for Cygwin on Windows.
         */
        CYGWIN(ADEGlobals.HostOS.CYGWIN, "/usr/bin/sh", "-c", "&", "/usr/bin/ping", "-c 1 -w 5", "/usr/bin/rsh", "-l", "/usr/bin/rcp", "", "/usr/bin/ssh", "-X -A -n", "/usr/bin/scp", "", "/usr/bin/ps", "-f -a", "/usr/bin/kill", "/usr/bin/cp", "/usr/bin/rm", "/usr/bin/mkdir", "", "GPS", ""),
        /**
         * Defaults for Windows XP.
         */
        WINDOWSXP(ADEGlobals.HostOS.WINDOWSXP, "cmd.exe", "/C", "/K", "ping", "-n 1 -w 4000", "", "", "", "", "", "", "", "", "tasklist", "", "taskkill", "copy", "del", "mkdir", "", "GPS", ""),
        /**
         * Defaults for Windows 2000 copied from Windows XP.
         */
        WINDOWS2000(ADEGlobals.HostOS.WINDOWS2000, "cmd.exe", "/C", "/K", "ping", "-n 1 -w 4000", "", "", "", "", "", "", "", "", "tasklist", "", "taskkill", "copy", "del", "mkdir", "", "GPS", ""),
        /**
         * Defaults for Windows Vista copied from Windows XP.
         */
        WINDOWSVISTA(ADEGlobals.HostOS.WINDOWSVISTA, "cmd.exe", "/C", "/K", "ping", "-n 1 -w 4000", "", "", "", "", "", "", "", "", "tasklist", "", "taskkill", "copy", "del", "mkdir", "", "GPS", ""),
        /**
         * Defaults for Windows Seven copied from Windows XP.
         */
        WINDOWS7(ADEGlobals.HostOS.WINDOWS7, "cmd.exe", "/C", "/K", "ping", "-n 1 -w 4000", "", "", "", "", "", "", "", "", "tasklist", "", "taskkill", "copy", "del", "mkdir", "", "GPS", ""),
        /**
         * Unknown OS.
         */
        UNKNOWN(ADEGlobals.HostOS.UNKNOWN, "", "", "", "ping", "", "rsh", "", "rcp", "", "ssh", "", "scp", "", "ps", "", "halt", "copy", "rm", "mkdir", "", "GPS", "");
        private final ADEGlobals.HostOS os;
        private final String shlc;
        private final String[] shla;
        private final String bkga;
        private final String pngc;
        private final String[] pnga;
        private final String rshc;
        private final String[] rsha;
        private final String rcpc;
        private final String[] rcpa;
        private final String sshc;
        private final String[] ssha;
        private final String scpc;
        private final String[] scpa;
        private final String psc;
        private final String[] psa;
        private final String kill;
        private final String cp;
        private final String rm;
        private final String md;
        private final String gpslocation;
        private final String gpsformat;    // WGS84,degminsec,GPS
        private final String locationname;

        HostDefaultInfo(ADEGlobals.HostOS n, String shc, String sha,
                String bka, String pgc, String pga, String rsc, String rsa,
                String rcc, String rca, String ssc, String ssa,
                String scc, String sca, String psc, String psa,
                String killc, String cpc, String rmc, String mdc) {
            this(n, shc, sha, bka, pgc, pga, rsc, rsa, rcc, rca, ssc, ssa, scc, sca, psc, psa, killc, cpc, rmc, mdc, "", "GPS", "");
        }

        HostDefaultInfo(ADEGlobals.HostOS n, String shc, String sha,
                String bka, String pgc, String pga, String rsc, String rsa,
                String rcc, String rca, String ssc, String ssa,
                String scc, String sca, String psc, String psa,
                String killc, String cpc, String rmc, String mdc,
                String gpslocationc, String gpsformatc, String locationnamec) {
            os = n;
            shlc = shc;
            shla = sha.split("\\s");
            bkga = bka;
            pngc = pgc;
            pnga = pga.split("\\s");
            rshc = rsc;
            rsha = rsa.split("\\s");
            rcpc = rcc;
            rcpa = rca.split("\\s");
            sshc = ssc;
            ssha = ssa.split("\\s");
            scpc = scc;
            scpa = sca.split("\\s");
            this.psc = psc;
            this.psa = psa.split("\\s");
            kill = killc;
            cp = cpc;
            rm = rmc;
            md = mdc;
            gpslocation = gpslocationc;
            gpsformat = gpsformatc;
            locationname = locationnamec;
        }

        /**
         * Return the enumeration constant from the {@link ade.ADEGlobals.HostOS
         * HostOS} enumeration.
         */
        public ADEGlobals.HostOS getOS() {
            return os;
        }

        /**
         * Return the OS name (convenience method that calls {@link
         * ade.ADEGlobals.HostOS#name}).
         */
        public String getName() {
            return os.osname();
        }

        /**
         * Return the OS file separator (convenience method that calls {@link
         * ade.ADEGlobals.HostOS#fsep}).
         */
        public String getFileSep() {
            return os.fsep();
        }

        /**
         * Return the OS path separator (convenience method that calls {@link
         * ade.ADEGlobals.HostOS#psep}).
         */
        public String getPathSep() {
            return os.psep();
        }

        /**
         * Return the OS command separator (convenience method that calls {@link
         * ade.ADEGlobals.HostOS#csep}).
         */
        public String getCommandSep() {
            return os.csep();
        }

        /**
         * Return the local copy command.
         */
        public String getCpCmd() {
            return cp;
        }

        /**
         * Return the local remove command.
         */
        public String getRmCmd() {
            return rm;
        }

        /**
         * Return the local make directory command.
         */
        public String getMkdirCmd() {
            return md;
        }

        /**
         * Return the local kill process command.
         */
        public String getKillCmd() {
            return kill;
        }

        /**
         * Return the local shell command.
         */
        public String getShlCmd() {
            return shlc;
        }

        /**
         * Return the local shell arguments.
         */
        public String[] getShlArgs() {
            return shla;
        }

        /**
         * Return the background process switch.
         */
        public String getBkgArg() {
            return bkga;
        }

        /**
         * Return the ping command.
         */
        public String getPingCmd() {
            return pngc;
        }

        /**
         * Return the ping arguments.
         */
        public String[] getPingArgs() {
            return pnga;
        }

        /**
         * Return the remote shell command.
         */
        public String getRshCmd() {
            return rshc;
        }

        /**
         * Return the remote shell arguments.
         */
        public String[] getRshArgs() {
            return rsha;
        }

        /**
         * Return the remote copy command.
         */
        public String getRcpCmd() {
            return rcpc;
        }

        /**
         * Return the remote copy arguments.
         */
        public String[] getRcpArgs() {
            return rcpa;
        }

        /**
         * Return the remote secure shell command.
         */
        public String getSshCmd() {
            return sshc;
        }

        /**
         * Return the remote secure shell arguments.
         */
        public String[] getSshArgs() {
            return ssha;
        }

        /**
         * Return the remote secure copy command.
         */
        public String getScpCmd() {
            return scpc;
        }

        /**
         * Return the remote secure copy arguments.
         */
        public String[] getScpArgs() {
            return scpa;
        }

        /**
         * Return the process status command.
         */
        public String getPsCmd() {
            return psc;
        }

        /**
         * Return the process status arguments.
         */
        public String[] getPsArgs() {
            return psa;
        }

        /**
         * Same as getName() (convenience method that calls {@link
         * ade.ADEGlobals.HostOS#name}).
         */
        @Override
        public String toString() {
            return os.osname();
        }

        /**
         * Return the GPS location of the host
         */
        public String getGPSLocation() {
            return gpslocation;
        }

        /**
         * Return the GPS format
         */
        public String getGPSFormat() {
            return gpsformat;
        }

        /**
         * Return the location name of this host
         */
        public String getLocationName() {
            return locationname;
        }
    }

    /**
     * Return the operating system {@link HostDefaultInfo} enum constant for the
     * specified {@link ade.ADEGlobals.HostOS HostOS}.
     *
     * @param get the {@link ade.ADEGlobals.HostOS HostOS} enum constant, as
     * defined in {@link ade.ADEGlobals ADEGlobals}
     * @return the {@link ade.ADEGlobals.HostOS HostOS} enum constant, or
     * <tt>null</tt> if none is found
     */
    final static public HostDefaultInfo getHostDefaultInfo(ADEGlobals.HostOS get) {
        Set<HostDefaultInfo> diset = EnumSet.allOf(HostDefaultInfo.class);
        for (HostDefaultInfo di : diset) {
            if (get.equals(di.getOS())) {
                return di;
            }
        }
        return null;
    }
    // instance variables--select variables are public
    public String hostip;           // the IP of the host
    public String hostname;         // the name of the host
    ADEGlobals.HostOS hostos;       // the operating system (default to linux)
    int cpus;                       // number of available CPUs (default to 1)
    int cpumhz;                     // the processor speed (in MHz)
    int memmb;                      // the host's memory (in MB)
    float hostLoad = (float) 0.0;    // last known CPU load
    // Java and ADE specific information
    String adehome;                 // the top-level ADE path
    String adeconfigs;              // the path to ADE config/hosts files
    String adelogs;                 // the path to ADE logs
    String scratch;                 // the path for temporary files
    String javahome;                // the java home directory
    String javabin;                 // the java executable
    String compiler;                // the java compiler executable
    HashSet<String> devices;        // set of supported interfaces (hardware)
    Set<String> components;            // the ADEComponents executing on this host
    // OS dependent
    String shellcmd;                // the command to execute shell commands
    String[] shellargs;             // switches for executing shell commands
    String bkgarg;                  // background the shell process
    String pingcmd;                 // the command for pinging a host
    String[] pingargs;              // the switches for pinging a host
    String rshcmd;                  // the rsh command
    String[] rshargs;               // the switches for the rsh command
    String rcpcmd;                  // the rcp command
    String[] rcpargs;               // the switches for the rcp command
    String sshcmd;                  // the ssh command
    String[] sshargs;               // the switches for the ssh command
    String scpcmd;                  // the scp command
    String[] scpargs;               // the switches for the scp command
    String sshusername;             // login name for ssh/scp command
    String pscmd;                   // the ps command
    String[] psargs;                // the switches for the ps command
    String cp;                      // local copy
    String rm;                      // local remove
    String kill;                    // local kill process
    String md;                      // local make directory
    String filesep;                 // file separator character ("/" or "\")
    String pathsep;                 // path separator character (":" or ";")
    String cmdsep;                  // command separator ("; " or " && ")
    // the final command arrays
    String[] ssh;
    String[] scp;
    String[] shell;
    String[] ping;
    // GPS information
    String gpslocation;
    String gpsformat;    // WGS84,degminsec,GPS
    String locationname;
    // some state tracking
    private int exit = -1;
    private boolean canping = false;
    //private HashMap<String,ADELocalProcessStarter> localProcs;
    //private HashMap<String,ADERemoteProcessStarter> remoteProcs;

    /**
     * Use all defaults.
     */
    void setDefaults() {
        //setDefaults(ADEPreferences.HOSTOS, ADEPreferences.HOSTIP);
        ADEGlobals.HostOS myos = ADEGlobals.getOSByName(System.getProperty("os.name"));
        setDefaults(myos, ADEPreferences.HOSTIP);
    }

    void setDefaults(String ip) {
        ADEGlobals.HostOS myos = ADEGlobals.getOSByName(System.getProperty("os.name"));
        setDefaults(myos, ip);
    }

    void setDefaults(ADEGlobals.HostOS os, String ip) {
        //System.out.println(prg +": setting defaults for "+ os +", "+ ip);
        HostDefaultInfo di = getHostDefaultInfo(os);

        // set the defaults
        shellcmd = di.getShlCmd();
        shellargs = di.getShlArgs();
        bkgarg = di.getBkgArg();
        pingcmd = di.getPingCmd();
        pingargs = di.getPingArgs();
        rshcmd = di.getRshCmd();
        rshargs = di.getRshArgs();
        rcpcmd = di.getRcpCmd();
        rcpargs = di.getRcpArgs();
        sshcmd = di.getSshCmd();
        sshargs = di.getSshArgs();
        scpcmd = di.getScpCmd();
        scpargs = di.getScpArgs();
        sshusername = System.getProperty("user.name");
        pscmd = di.getPsCmd();
        psargs = di.getPsArgs();
        cp = di.getCpCmd();
        rm = di.getRmCmd();
        kill = di.getKillCmd();
        md = di.getMkdirCmd();
        filesep = os.fsep();
        pathsep = os.psep();
        cmdsep = os.csep();
        hostip = ip;
        cpus = ADEPreferences.CPUS;
        cpumhz = ADEPreferences.CPUMHZ;
        memmb = ADEPreferences.MEMMB;
        /* I think using the current JVM settings might be a better idea...
         adehome = ADEPreferences.ADEHOME;
         adeconfigs = ADEPreferences.ADECONF;
         adelogs = ADEPreferences.ADELOGS;
         scratch = ADEPreferences.SCRATCH;
         javahome = ADEPreferences.JAVAHOME;
         javabin = ADEPreferences.JAVABIN;
         compiler = ADEPreferences.COMPILER;
         */
        adehome = System.getProperty("user.dir");
        if (!adehome.endsWith(filesep)) {
            adehome += filesep;
        }
        adeconfigs = ADEPreferences.ADECONF;
        adelogs = ADEPreferences.ADELOGS;
        if (!adelogs.endsWith(filesep)) {
            adelogs += filesep;
        }
        scratch = System.getProperty("java.io.tmpdir");
        if (!scratch.endsWith(filesep)) {
            scratch += filesep;
        }
        javahome = System.getProperty("java.home");
        // java might be in a couple places
        if (javahome.indexOf("jdk") >= 0 && javahome.endsWith("jre")) {
            javahome = javahome.substring(0, javahome.length() - 4);
        }

        javahome += filesep;
        if (os.osname().indexOf("Windows") >= 0) {
            javahome = javahome.replace(" ", "\\ ");
            javabin = javahome + "bin\\java";
            compiler = javahome + "bin\\javac";
        } else {
            javabin = javahome + "bin/java";
            compiler = javahome + "bin/javac";
        }
        // create requisite data structures
        devices = new HashSet<String>();
        components = new HashSet<String>();
        // note that some elements must be filled at execution time
        shell = new String[shellargs.length + 2];
        shell[0] = shellcmd;
        System.arraycopy(shellargs, 0, shell, 1, shellargs.length);
        ping = new String[pingargs.length + 2];
        ping[0] = pingcmd;
        System.arraycopy(pingargs, 0, ping, 1, pingargs.length);
        ssh = new String[sshargs.length + 3];
        ssh[0] = sshcmd;
        System.arraycopy(sshargs, 0, ssh, 1, sshargs.length);
        scp = new String[scpargs.length + 3];
        scp[0] = scpcmd;
        System.arraycopy(scpargs, 0, scp, 1, scpargs.length);
        // new GPS stuff
        gpslocation = di.getGPSLocation();
        gpsformat = di.getGPSFormat();
        locationname = di.getLocationName();
        //System.out.println(prg +": done setting defaults");
    }

    /**
     * Constructor using all defaults (as found in {@link ADEPreferences
     * ADEPreferences} and {@link HostDefaultInfo}).
     */
    public ADEHostInfo() {
        setDefaults();
    }

    /**
     * Constructor for local host that uses all defaults except the address
     * (defaults as found in {@link ADEPreferences ADEPreferences}).
     */
    public ADEHostInfo(String useIP) throws IOException, SecurityException {
        setDefaults(useIP);
        if (verbose) {
            System.out.println("Establishing my IP address");
        }
        boolean gotit = false;
        Enumeration e = NetworkInterface.getNetworkInterfaces();
        while (e.hasMoreElements() && !gotit) {
            if (verbose) {
                System.out.println("Got network interface");
            }
            NetworkInterface netface = (NetworkInterface) e.nextElement();
            Enumeration e2 = netface.getInetAddresses();

            while (e2.hasMoreElements()) {
                if (debug) {
                    System.out.println("Got network interface's internet address");
                }
                InetAddress ip = (InetAddress) e2.nextElement();
                if (debug) {
                    System.out.println("Found IP address: " + ip.getHostAddress());
                }
                if (useIP != null && ip.getHostAddress().equals(useIP)) {
                    if (debug) {
                        System.out.println("Found IP address to be used: " + ip.getHostAddress());
                    }
                    hostip = ip.getHostAddress();
                    hostname = ip.getCanonicalHostName();
                    gotit = true;
                    break;
                }
                // start setting the IP just in case
                if (!ip.isLoopbackAddress()) {
                    hostip = ip.getHostAddress();
                    hostname = ip.getCanonicalHostName();
                    // set local address only if we don't have any yet...
                } else if (useIP == null) {
                    hostip = ip.getHostAddress();
                    hostname = ip.getCanonicalHostName();
                }
            }
        }
        if (debug) {
            System.out.println("Using " + hostip + " as the local IP address");
        }
    }

    /**
     * Attempt to get the OS of this host and its defaults.
     *
     * @param thishost If <tt>true</tt>, determine the operating system for
     * defaults; otherwise use {@link ade.ADEPreferences#HOSTOS HOSTOS} for
     * defaults
     * @throws IllegalArgumentException If the operating system on which this is
     * being run is not supported (i.e., not found in the {@link
     * ade.ADEGlobals.HostOS HostOS} enumeration)
     */
    public ADEHostInfo(boolean thishost) throws IllegalArgumentException {
        ADEGlobals.HostOS os;
        if (thishost) {
            String osname = System.getProperty("os.name");
            // we need to do some extra stuff for the nastiness that is Windows

            if (osname.indexOf("Windows") >= 0) {
                if (checkCygwin()) {
                    // superb, we have unix capabilities
                    osname = "Cygwin";
                    //} else {
                }
            }
            os = ADEGlobals.getOSByName(osname);
            if (os == null) {
                throw new IllegalArgumentException("Undefined operating system: " + osname);
            }
        } else {
            os = ADEPreferences.HOSTOS;
        }
        hostos = os;
        setDefaults(os, ADEPreferences.HOSTIP);
    }

    /**
     * Constructor that probes remote host for information (if one exists).
     */
    /*
     public ADEHostInfo(ADEHostInfo hi, String addr) throws UnknownHostException, IOException {
     String myshell = ADEPreferences.shellcmd;
     String myping = ADEPreferences.pingcmd;
     String myssh = ADEPreferences.sshcmd;
     // first, we need to get our own info
     if (System.getProperty("os.name").equals("Linux")) {
     myshell = ADEPreferences.shellcmd;
     for (String arg : ADEPreferences.shellargs) {
     myshell += " "+ arg;
     }
     myping = ADEPreferences.pingcmd;
     for (String arg : ADEPreferences.pingargs) {
     myping += " "+ arg;
     }
     myssh = ADEPreferences.sshcmd;
     for (String arg : ADEPreferences.sshargs) {
     myssh += " "+ arg;
     }
     }
     probeHost(myshell, myping, myssh, addr);
     }
     */
    public ADEHostInfo(
            String ip,
            String name,
            ADEGlobals.HostOS os,
            int cpus,
            int cpukhz,
            int memkb,
            String adehm,
            String tmp,
            String jhome,
            String jbin,
            String comp,
            String shlcmd,
            String shlargstr,
            String pngcmd,
            String pngargstr,
            String sshlcmd,
            String sshlargstr,
            String sshlpr,
            String sshlun,
            String sshlto,
            String dev) throws IOException, SecurityException {
        this(ip, name, os, cpus, cpukhz, memkb, adehm, tmp, jhome, jbin, comp, shlcmd, shlargstr.split(" "), pngcmd, pngargstr.split(" "), sshlcmd, sshlargstr.split(" "), sshlpr, sshlun, sshlto, dev);
    }

    /**
     * Constructor given all information.
     */
    public ADEHostInfo(
            String ip,
            String name,
            ADEGlobals.HostOS os,
            int cpus,
            int cpumhz,
            int memmb,
            String adehm,
            String tmp,
            String jhome,
            String jbin,
            String comp,
            String shlcmd,
            String[] shlargs,
            String pngcmd,
            String[] pngargs,
            String sshlcmd,
            String[] sshlargs,
            String sshlpr,
            String sshlun,
            String sshlto,
            String dev) throws IOException, SecurityException {
        setDefaults();
        // TODO: confirm ip and name are consistent
        //if (ip == null || name == null) {
        //
        //} else {
        hostip = ip;
        //}
        hostos = os;
        this.cpus = cpus;
        this.cpumhz = cpumhz;
        this.memmb = memmb;
        adehome = adehm;
        scratch = tmp;
        javahome = jhome;
        javabin = jbin;
        compiler = comp;
        shellcmd = shlcmd;
        shellargs = shlargs;
        pingcmd = pngcmd;
        pingargs = pngargs;
        sshcmd = sshlcmd;
        sshargs = sshlargs;
        sshusername = sshlun;
        if (dev != null) {
            String[] devs = dev.split(" ");
            for (String d : devs) {
                devices.add(d);
            }
        }
    }

    /**
     * Check to see if Cygwin is available. We simply see if the cygwin
     * repository exists on the system drive.
     *
     * @return <tt>true</tt> if Cygwin is detected, <tt>false</tt> otherwise
     */
    private boolean checkCygwin() {
        boolean haveCygwin = false;
        String sysdrive = System.getProperty("SystemDrive");
        if (sysdrive == null) {
            sysdrive = new String("c:");
        }
        try {
            ADEGlobals.checkDir(sysdrive + "\\cygwin", true, false);
            // cygwin is there; see if it's running. Are there other checks?
            if (System.getProperty("CYGWIN") != null) {
                haveCygwin = true;
            } else if (System.getProperty("OSTYPE") != null) {
                if (System.getProperty("OSTYPE").equals("cygwin")) {
                    haveCygwin = true;
                }
            }
        } catch (Exception e) {
            System.err.println(prg + ": Unexpected exception in checkCygwin:\n" + e);
        }
        return haveCygwin;
    }

    public void checkMembers() throws SecurityException, IOException {
        ADEGlobals.checkDir(adehome, true, false);
        ADEGlobals.checkDir(scratch, true, true);
        ADEGlobals.checkDir(javahome, true, false);
        ADEGlobals.checkCommand(javabin);
        ADEGlobals.checkCommand(compiler);
        ADEGlobals.checkCommand(shellcmd);
        ADEGlobals.checkCommand(pingcmd);
        ADEGlobals.checkCommand(sshcmd);
    }

    public void checkMembers(ADEHostInfo local) throws SecurityException, IOException {
        boolean islocal = false;
        /* Obviously, not implemented yet...
         if ((InetAddress.getByName(local.hostip)).isSiteLocalAddress()) {
         islocal = true;
         checkDir(adehome, true, false);
         checkDir(scratch, true, true);
         checkDir(javahome, true, false);
         checkCommand(javabin);
         checkCommand(compiler);
         checkCommand(shellcmd);
         checkCommand(pingcmd);
         checkCommand(sshcmd);
         }*/
    }

    /* TODO: not implemented yet...
     private void probeHost(String shcmd, String pingcmd, String sshcmd, String host)
     throws UnknownHostException, IOException {
     hostip = pingHost(host);
     hostname = InetAddress.getByName(hostip).getHostName();
     }
     */
    /**
     * Return a duplicate of this object.
     *
     * @return A copy of this object
     */
    public ADEHostInfo duplicate() {
        try {
            return (ADEHostInfo) this.clone();
        } catch (CloneNotSupportedException e) {
            System.err.println(prg + ": Cloning of ADEHostInfo not supported.");
            return null;
        }
    }

    boolean removeComponent(String s) {
        return components.remove(s);
    }

    /**
     * Return the {@link ade.ADEGlobals.HostOS HostOs}.
     *
     * @return The OS enumeration constant
     */
    public ADEGlobals.HostOS getOS() {
        return hostos;
    }

    /**
     * Return the ADE home directory.
     *
     * @return The ADE home directory
     */
    public String getADEHome() {
        return adehome;
    }

    /**
     * Return the ADE configuration file directory.
     *
     * @return The ADE configuration file directory
     */
    public String getADEConf() {
        return adehome + adeconfigs;
    }

    /**
     * Return the ADE log file directory.
     *
     * @return The ADE log file directory
     */
    public String getADELogs() {
        return adehome + adelogs;
    }

    /**
     * For recording the exit value of the command used by {@link #reachable} so
     * that it can be checked outside the thread.
     */
    private void setExit(int ex) {
        exit = ex;
    }

    /**
     * Return whether the host in tosi ADEComponentInfo is reachable from the
     * current host.
     *
     * @return The ADE log file directory
     */
    public boolean reachable(ADEComponentInfo tosi) {
        //final String host = tosi.host;
        return reachable(tosi.host);
    }

    public boolean reachable(final String host) {
        if (debug) {
            System.out.println(prg + ": checking if " + host + " is available...");
            System.out.flush();
        }
        // assume that local calls can always be completed...
        if (host.equals("localhost")
                || host.equals("127.0.0.1")
                || host.equals(hostip)) {
            //if (debug) System.out.println("OK (local).");
            return true;
        }

        // use ping rather than ssh?
        final String[] remotecmd = new String[ping.length];
        System.arraycopy(ping, 0, remotecmd, 0, ping.length);
        remotecmd[ping.length - 1] = host;
        // need: "ssh -1 -o "ConnectTimeout x" user@host"
        //final String[] remotecmd = new String[5];
        //remotecmd[0] = sshcmd;
        //remotecmd[1] = sshprot;
        //remotecmd[2] = sshtimeout;
        //remotecmd[3] = tohi.sshusername + "@" + host;
        //remotecmd[4] = "echo now";
        //final int sleeptime = (sshto*1000)+500;
        //if (debug) {
        //	System.err.print(prg +": executing[");
        //	for (String part : remotecmd) {
        //		System.err.print(" "+ part);
        //	}
        //	System.err.println("]");
        //}
        setExit(-1);
        new Thread() {
            public void run() {
                try {
                    Process P = Runtime.getRuntime().exec(remotecmd);
                    for (;;) {
                        try {
                            Thread.sleep(10);  // wait for response
                            setExit(P.exitValue());
                            break;
                        } catch (IllegalThreadStateException e1) {
                            // Process has not terminated yet, so continue
                        } catch (InterruptedException ignore) {
                            //System.err.println(this +": Caught interruption!");
                            //break;
                        }
                    }
                    //System.out.println(hostip +"->"+ host +": thread recovery $$$$$$ exit value="+ P.exitValue());
                    P.destroy();
                } catch (Exception e) {
                    System.err.println(prg + ": Exception testing reachability to " + host + "\n" + e);
                }
            }
        }.start();
        // need this so that the ping thread can start
        try {
            Thread.sleep(11);
        } catch (Exception e) {
            System.out.println("Cannot YIELD!");
        }
        // limit the time waiting for ping response
        int timeleft = 1000; // ms
        while (timeleft > 0 && exit == -1) {
            try {
                Thread.sleep(50);
                timeleft -= 50;
            } catch (Exception ignore) {
            }
        }
        //System.out.println(hostip +"->"+ host +": recovery prg $$$$$$ exit value="+ exit);
        if (exit != 0) {
            System.err.println(prg + ": ping to " + host + " unsuccessful (exit value=" + exit + ")!");
            return false;
        }
        return true;
    }

    /**
     * Starts an {@link ade.ADEComponent ADEComponent} on a local or remote host. The
     * host structure for the (possibly) remote host and the component structure of
     * the component to start are passed, so that specific paths, etc. can be set
     * properly.
     *
     * @param tohi The host information where the component will be started
     * @param si The component information
     */
    // TODO: should throw an exception if component can't be started
    final ADELocalProcessStarter startComponent(ADEHostInfo tohi, ADEComponentInfo si) {
        ADELocalProcessStarter alp;
        // This is a hack for the case where one ADERegistry has to restart
        // another; on startup, unless the entire host went down, the other
        // ADEComponents will still be executing. But the config file causes
        // the ADERegistry to start new ones, which is causing problems.
        // So, I'm going to null the config file before restart, then
        // replace it directly afterwards.
        // TODO: when fixing this hack, see the "ignoreconfig" variable in
        // ADEComponentInfo and its use in ADERegistryImpl
        String hackRegConfig = null;
        String cmd = null;
        //boolean background = true;
        boolean background = false;

        if (debug) {
            System.out.print(prg + ": in startComponent(" + si.host + ", " + si.port);
            System.out.println(", " + si.name + ", " + tohi.sshusername + ")");
            si.print();
        }
        if (si.isregistry) {
            hackRegConfig = si.configfile;
            si.configfile = null;
            background = false;
        }
        cmd = si.buildStartCommand(tohi);
        if (debug) {
            System.out.println(prg + ": starting " + si.type + "; cmd=[" + cmd + "]");
        }
        try {
            alp = new ADELocalProcessStarter(this, si.startdirectory, cmd);
        } catch (Exception e) {
            System.err.println(prg + ": Exception creating process starter:");
            //System.err.println(prg +": "+ e);
            e.printStackTrace();
            return null;
        }
        if (hackRegConfig != null) {
            si.configfile = hackRegConfig;
        }

        if (debug) {
            System.err.println("\tDeciding local or remote command...");
        }
        // check if startup is local -- compare hostip (i.e., the ip of the
        // current host of the component to that of the new host
        if (hostip.equals("127.0.0.1")
                || hostip.equals("localhost")
                || hostip.equals(tohi.hostip)) {
            if (debug) {
                System.err.println("\tLocal startup...");
            }

            // check if the component is still bound locally, in which case we
            // need to unbind it to be able to use the name
			/*
             try {
             Registry r = LocateRegistry.getRegistry();
             r.unbind(si.getKey());
             } catch (Exception e) {
             // don't worry if that did not succeed...
             //System.out.println("Error unbinding "+ type + "$" + name);
             }
             */
        } else {     // or remote
            if (debug) {
                System.err.println("\tRemote startup...");
            }
        }
        alp.startProcess(tohi, background);
        si.setHostInfo(tohi);
        return alp;
    }

    /**
     * Check if the component can be started on this host.
     */
    public boolean canStartComponent(ADEComponentInfo si) {
        if (debug) {
            System.out.println(prg + ": checking if " + si.type + " is allowed on " + hostip);
        }
        if (!si.allowedOnHost(hostip)) {
            if (debug) {
                System.out.println(prg + ": NOT ALLOWED on " + hostip + "!");
            }
            return false;
        }
        if (debug) {
            System.out.println(prg + ": " + si.type + " allowed on " + hostip);
            System.out.print(prg + ": checking if " + hostip + " has required devices");
        }
        if (si.hasRequiredDevices()) {
            if (!hasDevices(si.getRequiredDevices())) {
                if (debug) {
                    System.out.println(prg + ": " + hostip + " MISSING DEVICES!");
                }
                return false;
            }
        }
        if (debug) {
            System.out.println(prg + ": found devices");
            System.out.println(prg + ": OK to start " + si.type + " on " + hostip);
        }
        return true;
    }

    public boolean hasDevices(String[] devs) {
        synchronized (devices) {
            for (String dev : devs) {
                if (!devices.contains(dev)) {
                    return false;
                }
            }
        }
        return true;
    }

    // *******************************************************************
    // ******* String utility methods (parsing, printing, etc.)
    // *******************************************************************
    /**
     * Brief String representation of this host (IP and operating system).
     */
    public String toString() {
        return (hostip + ":" + hostos);
    }

    /**
     * Fills in information from an {@link java.io.BufferedReader
     * BufferedReader} with a <tt>hostfile</tt> format (see this class's main
     * documentation).
     *
     * @param br The buffered reader
     * @param line The line number
     * @throws IOException if an error occurs reading
     * @throws NumberFormatException if a numerical value cannot be parsed
     * @throws ParseException if no <tt>ENDHOST</tt> line is found
     */
    public int parseHost(BufferedReader br, int line)
            throws IOException, NumberFormatException, ParseException {
        // need storage to distinguish between defaults set by OS
        ADEGlobals.HostOS newos = null;
        String jbin = null, jcomp = null;
        String shc = null, sha = null, pgc = null, pga = null;
        String rsc = null, rsa = null, rcc = null, rca = null;
        String ssc = null, ssa = null, scc = null, sca = null;
        String psc = null, psa = null;
        String gpslocationc = null, gpsformatc = null, locationnamec = null;

        while (true) {
            //try {
            br.mark(512);  // 1/2K should be more than enough...
            String str = br.readLine();
            line++;
            if (str != null) {
                //System.out.println(prg +": Read ["+ str +"]");
                StringTokenizer s = new StringTokenizer(str);
                while (s.hasMoreTokens()) {
                    String data = s.nextToken();
                    if (str.startsWith("STARTHOST")) {
                        throw new ParseException("Encountered another STARTHOST!", line);
                    } else if (data.startsWith("#")) {
                        // comment, skip the rest of the line
                        break;
                        //} else if (data.equals("address")) {
                        //	addr = s.nextToken();
                    } else if (data.equals("ip")) {
                        hostip = s.nextToken();
                        //} else if (data.equals("hostname")) {
                        //	name = s.nextToken();
                    } else if (data.equals("os")) {
                        String osstr = s.nextToken();
                        // need to be able to handle things like 'Windows XP'
                        while (s.hasMoreTokens()) {
                            osstr += " " + s.nextToken();
                        }
                        newos = ADEGlobals.getOSByName(osstr);
                    } else if (data.equals("cpus")) {
                        cpus = Integer.parseInt(s.nextToken());
                    } else if (data.equals("cpumhz")) {
                        cpumhz = Integer.parseInt(s.nextToken());
                    } else if (data.equals("memmb")) {
                        memmb = Integer.parseInt(s.nextToken());
                    } else if (data.equals("adehome")) {
                        String tmp = adehome;
                        adehome = s.nextToken();
                        if ((hostip.equals("127.0.0.1") || hostip.equals("localhost")) && !(new File(adehome).exists())) {
                            System.err.println("Error in hosts config file for the localhost: 127.0.0.1\n The adehome is set incorrectly since '" + adehome + "' does not exist on this system.");
                            if (new File(tmp).exists()) {
                                System.err.println("Defaulting back to " + tmp + " (which does exist");
                                adehome = tmp;
                            }
                        }
                        // it's just due to Windows paths
                        while (s.hasMoreTokens()) {
                            adehome += " " + s.nextToken();
                        }
                    } else if (data.equals("tempdir")) {
                        scratch = s.nextToken();
                    } else if (data.equals("javadir")) {
                        javahome = s.nextToken();
                        // TODO: for now, assume if there are spaces,
                        // it's just due to Windows paths
                        while (s.hasMoreTokens()) {
                            javahome += " " + s.nextToken();
                        }
                    } else if (data.equals("javabin")) {
                        jbin = s.nextToken();
                        // TODO: for now, assume if there are spaces,
                        // it's just due to Windows paths
                        while (s.hasMoreTokens()) {
                            jbin += " " + s.nextToken();
                        }
                    } else if (data.equals("javacompiler")) {
                        jcomp = s.nextToken();
                        // TODO: for now, assume if there are spaces,
                        // it's just due to Windows paths
                        while (s.hasMoreTokens()) {
                            jcomp += " " + s.nextToken();
                        }
                    } else if (data.equals("shell")) {
                        shc = s.nextToken();
                    } else if (data.equals("shellargs")) {
                        if (s.hasMoreTokens()) {
                            sha = s.nextToken();
                        }
                        while (s.hasMoreTokens()) {
                            sha += " " + s.nextToken();
                        }
                    } else if (data.equals("ping")) {
                        pgc = s.nextToken();
                    } else if (data.equals("pingargs")) {
                        if (s.hasMoreTokens()) {
                            pga = s.nextToken();
                        }
                        while (s.hasMoreTokens()) {
                            pga += " " + s.nextToken();
                        }
                    } else if (data.equals("rsh")) {
                        rsc = s.nextToken();
                    } else if (data.equals("rshargs")) {
                        if (s.hasMoreTokens()) {
                            rsa = s.nextToken();
                        }
                        while (s.hasMoreTokens()) {
                            rsa += " " + s.nextToken();
                        }
                    } else if (data.equals("rcp")) {
                        rcc = s.nextToken();
                    } else if (data.equals("rcpargs")) {
                        if (s.hasMoreTokens()) {
                            rca = s.nextToken();
                        }
                        while (s.hasMoreTokens()) {
                            rca += " " + s.nextToken();
                        }
                    } else if (data.equals("ssh")) {
                        ssc = s.nextToken();
                    } else if (data.equals("sshargs")) {
                        if (s.hasMoreTokens()) {
                            ssa = s.nextToken();
                        }
                        while (s.hasMoreTokens()) {
                            ssa += " " + s.nextToken();
                        }
                    } else if (data.equals("scp")) {
                        scc = s.nextToken();
                    } else if (data.equals("scpargs")) {
                        if (s.hasMoreTokens()) {
                            sca = s.nextToken();
                        }
                        while (s.hasMoreTokens()) {
                            sca += " " + s.nextToken();
                        }
                    } else if (data.equals("sshlogin")) {
                        sshusername = s.nextToken();
                    } else if (data.equals("ps")) {
                        psc = s.nextToken();
                    } else if (data.equals("psargs")) {
                        if (s.hasMoreTokens()) {
                            psa = s.nextToken();
                        }
                        while (s.hasMoreTokens()) {
                            psa += " " + s.nextToken();
                        }
                    } else if (data.equals("devices")) {
                        if (devices == null) {
                            devices = new HashSet<String>();
                        }
                        if (s.hasMoreTokens()) {
                            devices.add(s.nextToken());
                        }
                        while (s.hasMoreTokens()) {
                            devices.add(s.nextToken());
                        }
                    } else if (data.equals("gpslocation")) {
                        gpslocationc = s.nextToken();
                    } else if (data.equals("gpsformat")) {
                        gpsformatc = s.nextToken();
                    } else if (data.equals("locationname")) {
                        locationnamec = s.nextToken();
                    } else if (data.equals("ENDHOST")) {
                        // do any final checks
                        if (newos != null) {
                            // a different os was set; get defaults for items not
                            // explicitly set otherwise
                            hostos = newos;
                        }
                        // get the defaults and set every variable that is not explicitly set
                        HostDefaultInfo di = hostos.hostDefaultInfo();
                        // shell
                        if (shc != null) {
                            sshcmd = shc;
                        } else {
                            sshcmd = di.getShlCmd();
                        }
                        if (sha != null) {
                            sshargs = sha.split(" ");
                        } else {
                            sshargs = di.getShlArgs();
                        }
                        // ping
                        if (pgc != null) {
                            pingcmd = pgc;
                        } else {
                            pingcmd = di.getPingCmd();
                        }
                        if (pga != null) {
                            pingargs = pga.split(" ");
                        } else {
                            pingargs = di.getPingArgs();
                        }
                        // rsh
                        if (rsc != null) {
                            rshcmd = rsc;
                        } else {
                            rshcmd = di.getRshCmd();
                        }
                        if (rsa != null) {
                            rshargs = rsa.split(" ");
                        } else {
                            rshargs = di.getRshArgs();
                        }
                        // rcp
                        if (rcc != null) {
                            rcpcmd = rcc;
                        } else {
                            rcpcmd = di.getRcpCmd();
                        }
                        if (rca != null) {
                            rcpargs = rca.split(" ");
                        } else {
                            rcpargs = di.getRcpArgs();
                        }
                        // ssh
                        if (ssc != null) {
                            sshcmd = ssc;
                        } else {
                            sshcmd = di.getSshCmd();
                        }
                        if (ssa != null) {
                            sshargs = ssa.split(" ");
                        } else {
                            sshargs = di.getSshArgs();
                        }
                        // scp
                        if (scc != null) {
                            scpcmd = scc;
                        } else {
                            scpcmd = di.getScpCmd();
                        }
                        if (sca != null) {
                            scpargs = sca.split(" ");
                        } else {
                            scpargs = di.getScpArgs();
                        }
                        // ps
                        if (psc != null) {
                            pscmd = psc;
                        } else {
                            pscmd = di.getPsCmd();
                        }
                        if (psa != null) {
                            psargs = psa.split(" ");
                        } else {
                            psargs = di.getPsArgs();
                        }
                        // compute the new ssh and scp arguments
                        ssh = new String[sshargs.length + 3];
                        ssh[0] = sshcmd;
                        System.arraycopy(sshargs, 0, ssh, 1, sshargs.length);
                        scp = new String[scpargs.length + 3];
                        scp[0] = scpcmd;
                        System.arraycopy(scpargs, 0, scp, 1, scpargs.length);
                        // the GPS stuff
                        if (gpslocationc != null) {
                            gpslocation = gpslocationc;
                        } else {
                            gpslocation = di.getGPSLocation();
                        }
                        if (gpsformatc != null) {
                            gpsformat = gpsformatc;
                        } else {
                            gpsformat = di.getGPSFormat();
                        }
                        if (locationnamec != null) {
                            locationname = locationnamec;
                        } else {
                            locationname = di.getLocationName();
                        }

                        // javahome must have been filled in
                        if (!javahome.endsWith(filesep)) {
                            javahome += filesep;
                        }
                        if (jbin == null) {
                            String tmp = javabin;
                            javabin = javahome + "bin" + filesep + "java";
                            if ((hostip.equals("127.0.0.1") || hostip.equals("localhost")) && !(new File(javabin).exists())) {
                                System.err.println("Error in hosts config file for the localhost: 127.0.0.1\n The javahome is set incorrectly since '" + javabin + "' does not exist on this system.");
                                if (new File(tmp).exists()) {
                                    System.err.println("Defaulting back to " + tmp + " (which does exist");
                                    javabin = tmp;
                                }
                            }
                        } else {
                            javabin = jbin;
                        }
                        if (jcomp == null) {
                            compiler = javahome + "bin" + filesep + "javac";
                        } else {
                            compiler = jcomp;
                        }
                        //System.out.println(prg +": Finished parsing entry; info:");
                        //print();
                        return line;
                    }
                }
            } else {
                throw new java.text.ParseException("Missing ENDHOST", line);
            }
        }
    }

    /**
     * Utility method for writing out the host format. Declared static so that
     * host files may be written out by the {@link #createHostsFile} method
     * without declaring an object.
     *
     * @param pw A {@link java.io.PrintWriter PrintWriter} for output
     * @param filename The name of the file (written in the header)
     */
    private static void writeHostsFormat(PrintWriter pw, String filename) {
        pw.print("# ADE hosts file ");
        pw.println(filename);
        pw.print("# Automatically generated by ");
        pw.print(prg);
        pw.print(" on ");
        pw.println(Calendar.getInstance().getTime().toString());
        pw.println("# All of these entries are optional and will be filled");
        pw.println("# default values, as found in the ade.ADEPreferences class");
        pw.println("# and the ade.ADEHostInfo.HostDefaultInfo enumeration.");
        pw.println("# ");
        pw.println("# Format and valid options:");
        pw.println("# STARTHOST");
        pw.println("# ip           IP address or hostname");
        pw.println("# os           operating system name");
        pw.println("# cpus         number of CPUs (default=1)");
        pw.println("# cpumhz       CPU speeds (default=850)");
        pw.println("# memmb        available memory (default=256MB)");
        pw.println("# adehome      ADE home directory");
        pw.println("# tempdir      temporary directory (e.g., /tmp)");
        pw.println("# javadir      Java home directory");
        pw.println("# javabin      Java executable");
        pw.println("# javacompiler Java compiler");
        pw.println("# shell        command-line shell (e.g., /bin/sh)");
        pw.println("# shellargs    shell arguments (e.g., -c)");
        pw.println("# ping         ping command (e.g., /bin/ping)");
        pw.println("# pingargs     ping arguments (e.g., -c 2)");
        pw.println("# rsh          remote shell command (e.g., /bin/rsh)");
        pw.println("# rshargs      remote shell arguments (e.g., -X -A -n)");
        pw.println("# rcp          remote copy command (e.g., /bin/rcp)");
        pw.println("# rcpargs      remote copy arguments (e.g., -X -A -n)");
        pw.println("# ssh          ssh command (e.g., /bin/ssh)");
        pw.println("# sshargs      ssh arguments (e.g., -X)");
        pw.println("# scp          remote secure copy command (e.g., /bin/scp)");
        pw.println("# scpargs      remote secure copy arguments (e.g., -A -n)");
        pw.println("# sshlogin     ssh login username");
        pw.println("# ps           process information command (e.g., /bin/ps)");
        pw.println("# psargs       process information command (e.g., -f)");
        pw.println("# devices      interfaces supported on host");
        pw.println("# gpslocation  the GPS coordinates of the host's location");
        pw.println("# gpsformat    the GPS format used (WGS84,degminsec,GPS), default GPS");
        pw.println("# locationname the name of the location of this host");
        pw.println("# ENDHOST");
        pw.println("# ");
    }

    /**
     * Writes pre-formulated host information to a file. Note that this will
     * method will overwrite an exiting file.
     *
     * @param path The file name
     * @param entries An {@link java.util.ArrayList ArrayList} of {@link
     * java.util.HashMap HashMap}s, comprised of String keys and String[] values
     * @return The name of the file written
     * @throws SecurityException
     * @throws IOException if an error occurs creating or writing to the file
     */
    public static final String createHostsFile(String path,
            ArrayList<HashMap<String, String[]>> entries)
            throws SecurityException, IOException {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
        PrintWriter pw = new PrintWriter(file);
        String[] vals;

        writeHostsFormat(pw, path);
        for (HashMap<String, String[]> entry : entries) {
            pw.println("STARTHOST");
            for (Map.Entry<String, String[]> keyval : entry.entrySet()) {
                pw.print(keyval.getKey());
                vals = keyval.getValue();
                for (int i = 0; i < vals.length; i++) {
                    pw.print(" " + vals[i]);
                }
                pw.println();
            }
            pw.println("ENDHOST");
            pw.println("# ");
        }
        pw.println("# ");
        pw.flush();
        return file.getName();
    }

    /**
     * Writes the host information of this object to a new file. The file will
     * be written to the <tt>adeconfigs</tt> directory. If a file of the same
     * name exists, an attempt will be made to rename the file as
     * <tt>name.#</tt>, where <tt>#</tt> is cumulative count of files with that
     * name.
     *
     * @param name The file name
     * @throws SecurityException
     * @throws IOException if an error occurs creating or writing to the file
     */
    public final String createHostsFile(String name)
            throws SecurityException, IOException {
        StringBuilder path = new StringBuilder(adehome);
        String file;

        if (!adehome.endsWith(filesep)) {
            path.append(filesep);
        }
        path.append(adeconfigs);
        if (!adeconfigs.endsWith(filesep)) {
            path.append(filesep);
        }
        // could print out a message about file being backed up
        file = ADEGlobals.backupFile(path.toString(), name);
        createHostsFile(name, true);
        return name;
    }

    /**
     * Writes the host information of this object to a file. The file will be
     * written to the <tt>adeconfigs</tt> directory.
     *
     * @param name The file name
     * @param ow Whether to overwrite a file (if it exists)
     * @throws SecurityException
     * @throws IOException if an error occurs creating or writing to the file
     */
    public final void createHostsFile(String name, boolean ow)
            throws SecurityException, IOException {
        StringBuilder path = new StringBuilder(adehome);
        if (!adehome.endsWith(filesep)) {
            path.append(filesep);
        }
        path.append(adeconfigs);
        if (!adeconfigs.endsWith(filesep)) {
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
            writeHostsFormat(pw, file.getName());
        }
        pw.println("#");
        pw.println("STARTHOST");
        pw.println("ip " + hostip);
        pw.println("os " + hostos.osname());
        pw.println("cpus " + cpus);
        pw.println("cpumhz " + cpumhz);
        pw.println("memmb " + memmb);
        pw.println("adehome " + adehome);
        pw.println("tempdir " + scratch);
        pw.println("javadir " + javahome);
        pw.println("javabin " + javabin);
        pw.println("javacompiler " + compiler);
        pw.println("shell " + shellcmd);
        pw.println("shellargs " + ADEGlobals.arrToStr(shellargs));
        pw.println("ping " + pingcmd);
        pw.println("pingargs " + ADEGlobals.arrToStr(pingargs));
        pw.println("rsh " + rshcmd);
        pw.println("rshargs " + ADEGlobals.arrToStr(rshargs));
        pw.println("rcp " + rcpcmd);
        pw.println("rcpargs " + ADEGlobals.arrToStr(rcpargs));
        pw.println("ssh " + sshcmd);
        pw.println("sshargs " + ADEGlobals.arrToStr(sshargs));
        pw.println("scp " + scpcmd);
        pw.println("scpargs " + ADEGlobals.arrToStr(scpargs));
        pw.println("sshlogin " + sshusername);
        pw.println("ps " + pscmd);
        pw.println("psargs " + ADEGlobals.arrToStr(psargs));
        pw.println("devices " + ADEGlobals.setToStr(devices));
        pw.println("gpslocation" + gpslocation);
        pw.println("gpsformat" + gpsformat);
        pw.println("locationname" + locationname);
        pw.println("ENDHOST");
        pw.println("#");
        pw.flush();
    }

    /**
     * Print the (shortened) host information to <tt>System.out</tt>. This
     * method simply prints the string produced by the {@link #hostInfo} method.
     */
    public void print() {
        System.out.println(hostInfo());
    }

    /**
     * Create a formatted String of the host information. This method allows the
     * host information to be obtained and displayed by other objects. Note that
     * some information (e.g., <tt>rsh</tt>, separator characters, etc.) are not
     * included.
     *
     * @return The formatted host information
     */
    public String hostInfo() {
        StringBuilder sb = new StringBuilder("HOST INFO:");
        sb.ensureCapacity(1024); // 1K should be enough
        sb.append("==============================\n");
        sb.append("IP Address:    ");
        sb.append(hostip);     // ip address
        sb.append("\nName:          ");
        sb.append(hostname);   // host name
        sb.append("\nOS:            ");
        sb.append(hostos);     // operating system
        sb.append("\nCPUs:          ");
        sb.append(cpus);       // number of CPUs
        sb.append("\nCPU speed:     ");
        sb.append(cpumhz);     // the processor speed
        sb.append("\nMemory:        ");
        sb.append(memmb);      // memory
        sb.append("\nADE home:      ");
        sb.append(adehome);    // ADE home directory
        sb.append("\nADE conf:      ");
        sb.append(adeconfigs); // ADE configuration file directory
        sb.append("\nADE logs:      ");
        sb.append(adelogs);    // ADE log file directory
        sb.append("\nTemp. dir:     ");
        sb.append(scratch);    // temp file directory
        sb.append("\nJava home:     ");
        sb.append(javahome);   // java home directory
        sb.append("\nJava binary:   ");
        sb.append(javabin);    // java binary
        sb.append("\nJava compiler: ");
        sb.append(compiler);   // java compiler
        sb.append("\nShell:         ");
        sb.append(shellcmd);   // shell command
        sb.append("\nShell args:    ");
        sb.append(ADEGlobals.arrToStr(shellargs));  // shell arguments
        sb.append("\nPing:          ");
        sb.append(pingcmd);    // ping command
        sb.append("\nPing args:     ");
        sb.append(ADEGlobals.arrToStr(pingargs));   // ping arguments
        sb.append("\nSecure shell:  ");
        sb.append(sshcmd);     // secure shell command
        sb.append("\nSShell args:   ");
        sb.append(ADEGlobals.arrToStr(sshargs));    // secure shell arguments
        sb.append("\nUser name:     ");
        sb.append(sshusername);
        sb.append("\nDevices:       ");
        sb.append(ADEGlobals.setToStr(devices));    // device list
        sb.append("\n----------------------");
        sb.append("\nCPU load:      ");
        sb.append(hostLoad);   // CPU load
        sb.append("\nComponents:       ");
        sb.append(ADEGlobals.setToStr(components));    // components hosted
        sb.append("\n GPS location: ");
        sb.append(gpslocation);
        sb.append("\n GPS format:   ");
        sb.append(gpsformat);
        sb.append("Location name:   ");
        sb.append(locationname);
        sb.append("\n");
        return sb.toString();
    }

    // *******************************************************************
    // ******* Some private utility methods
    // *******************************************************************
    // find an available IP address for the given host, using "ping"
    // to check availability
    private String pingHost(String hostname) {
        InetAddress[] ips = null;
        Runtime R = Runtime.getRuntime();
        try {
            ips = InetAddress.getAllByName(hostname);
        } catch (java.net.UnknownHostException uhe) {
            System.err.println("Exception in pingHost:\n" + uhe);
            //throw new UnknownHostException(uhe.toString());
        }
        if (ips != null) {
            for (InetAddress ip : ips) {
                try {
                    String addr = ip.getHostAddress();
                    String[] cmd = new String[2 + pingargs.length];
                    cmd[0] = pingcmd;
                    for (int i = 1; i < pingargs.length; i++) {
                        cmd[i] = pingargs[i - 1];
                    }
                    cmd[pingargs.length + 1] = addr;
                    Process P = R.exec(cmd);
                    try {
                        P.waitFor();
                        String procOut = loadStream(P.getInputStream());
                        String procErr = loadStream(P.getErrorStream());
                        if (procOut.indexOf(" 0 received") != -1) {
                            if (debug) {
                                System.out.println("Address " + addr + " is currently invalid for host " + hostname + ".");
                            }
                        } else {
                            canping = true;
                            return addr;
                        }
                    } catch (InterruptedException ie) {
                        if (debug) {
                            System.out.println("Interrupted");
                        }
                    }
                } catch (IOException ioe) {
                    System.err.println(prg + ": IO Error pinging host:\n" + ioe);
                    //throw new IOException(ioe.toString());
                }
            }
        }
        return ips[0].getHostAddress();
    }

    private String loadStream(InputStream in) throws IOException {
        int ptr = 0;
        in = new BufferedInputStream(in);
        StringBuilder buffer = new StringBuilder();
        while ((ptr = in.read()) != -1) {
            buffer.append((char) ptr);
        }
        return buffer.toString();
    }
}
