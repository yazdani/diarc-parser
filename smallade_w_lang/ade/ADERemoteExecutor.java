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

/**
Utility to perform remote, external (to Java) process execution or file
copying. This is similar and scaled down version of the {@link
ade.ADELocalProcessStarter ADELocalProcessStarter} that is meant to run
terminating commands (i.e., that are expected to run for a relatively short
time) on a particular host, whereas the {@link ade.ADELocalProcessStarter
ADELocalProcessStarter} is meant to execute a single, sustained command on
any host, as specified by an {@link ade.ADEHostInfo ADEHostInfo} parameter.
<p>
Differences between this and {@link ade.ADELocalProcessStarter
ADELocalProcessStarter}:
<ul>
<li>Designed to work with Java 1.4.</li>
<li>Does <b>not</b> use a thread for process execution, thus will block.</li>
<li>Constructor parameters explicitly include the host, username, and secure
shell command rather than an {@link ade.ADEHostInfo ADEHostInfo}.</li>
<li>There are no {@link ade.ADELocalProcessStarter#startProcess} methods, but an {@link #execute}
method that takes the command to execute on the remote host.</li>
<li>The output stream is <b>cannot</b> be redirected, but is captured in
total and returned as the result of the {@link #execute} method.</li>
</ul>
<p>
<quote><tt>
ADERemoteExecutor ahostexe;<br>
String[] sshcmd = new String[]{"/usr/bin/ssh", "-X -A -n"}<br>
String exe = "ls ~/";
...<br>
try {</br>
<div style="margin-left: 40px;">
ahostexe = new ADERemoteExecutor("192.168.0.1", "userid", sshcmd);<br>
ahostexe.execute(exe);<br>
</div>
} catch (Exception e) {<br>
<div style="margin-left: 40px;">
System.err.println("Exception starting "+ exe +":");<br>
e.printStackTrace();<br>
System.exit(1);
</div>
}<br>
</tt>
</quote>
 */
final public class ADERemoteExecutor {
    // included so we can later read files even if the class changed...

    private static final long serialVersionUID = 7526472295622776147L;
    private static String prg = "ADERemoteExecutor";
    private boolean debug = false;
    //private boolean verbose = false;
    private String toHost;    // the remote host
    private String user;      // user login id
    private String cmdssh;    // local ssh command
    private String[] argssh;  // ssh arguments
    private String cmdscp;    // local scp command
    private String[] argscp;  // scp arguments
    private String[] ssharr;  // command line for ssh, persist for repetition
    private String[] scparr;  // command line for scp, persist for repetition
    private int sleeptime = 10;
    private boolean runExe = false;  // for process monitoring
    private int exit = 0;            // process exit value
    private Boolean runguard = new Boolean(true);
    private BufferedReader br1 = null, br2 = null;
    private StringBuffer tempbuf = new StringBuffer();
    private StringBuffer outstr = new StringBuffer();

    // disable construction with no parameters
    private ADERemoteExecutor() {
    }

    /** Constructor for executing external processes on a single remote host.
     * @param ip The IP address of the remote host
     * @param uid The user id for the remote host login
     * @param hc The remote shell command (usually <tt>ssh</tt>)
     * @param pc The remote copy command (usually <tt>scp</tt>)
     * @throws SecurityException
     * @throws IOException if a directory or command does not exist or
     * has improper permissions */
    public ADERemoteExecutor(String ip, String uid, String hc, String pc) {
        this(ip, uid, hc, "", pc, "");
    }

    /** Constructor for executing external processes on a single remote host.
     * @param ip The IP address of the remote host
     * @param uid The user id for the remote host login
     * @param hc The remote shell command (usually <tt>ssh</tt>)
     * @param ha The shell command's arguments (e.g., <tt>-X -A -n</tt>)
     * @param pc The remote copy command (usually <tt>scp</tt>)
     * @param pa The copy command's arguments (e.g., <tt>-2 -v</tt>)
     * @throws SecurityException
     * @throws IOException if a directory or command does not exist or
     * has improper permissions */
    public ADERemoteExecutor(String ip, String uid, String hc, String ha,
            String pc, String pa) {
        if (ip == null || uid == null || hc == null || pc == null) {
            throw new NullPointerException("Null argument found");
        }
        if (ip.equals("") || uid.equals("") || hc.equals("") || pc.equals("")) {
            throw new NullPointerException("Empty String argument found");
        }
        toHost = ip;
        user = uid;
        cmdssh = hc;
        if (ha == null) {
            argssh = new String[]{""};
        } else {
            argssh = ha.split(" ");
        }
        cmdscp = pc;
        if (pa == null) {
            argscp = new String[]{""};
        } else {
            argscp = pa.split(" ");
        }
        createSSHCmdArray();
        createSCPCmdArray();
    }

    /** Constructor for executing external processes on a single remote host.
     * @param ip The IP address of the remote host
     * @param uid The user id for the remote host login
     * @param hc The remote shell command (usually <tt>ssh</tt>)
     * @param ha The shell command's arguments (e.g., <tt>-X -A -n</tt>)
     * @param pc The remote copy command (usually <tt>scp</tt>)
     * @param pa The copy command's arguments (e.g., <tt>-2 -v</tt>)
     * @throws SecurityException
     * @throws IOException if a directory or command does not exist or
     * has improper permissions */
    public ADERemoteExecutor(String ip, String uid, String hc, String[] ha,
            String pc, String[] pa) {
        if (ip == null || uid == null || hc == null || pc == null) {
            throw new NullPointerException("Null argument found");
        }
        if (ip.equals("") || uid.equals("") || hc.equals("") || pc.equals("")) {
            throw new NullPointerException("Empty String argument found");
        }
        toHost = ip;
        user = uid;
        cmdssh = hc;
        if (ha == null) {
            argssh = new String[]{""};
        } else {
            argssh = ha;
        }
        cmdscp = pc;
        if (pa == null) {
            argscp = new String[]{""};
        } else {
            argscp = pa;
        }
        createSSHCmdArray();
        createSCPCmdArray();
    }

    private void createSSHCmdArray() {
        createSSHCmdArray(true);
    }

    /** Create the ssh array; the actual command is carried forward if it
     * exists, but can be changed at execution. The array persists over
     * repeated uses for two reasons: (1) To avoid repeated object creation
     * and (2) to allow the same command to be executed repeatedly.
     * @param copyArgs Whether to force argument replacement (always done
     * if the array hasn't been created or the number of arguments has
     * changed */
    private void createSSHCmdArray(boolean copyArgs) {
        String[] args;
        String tmpcmd = "";
        boolean needNewArr = false, haveArgs;

        haveArgs = (argssh.length > 1 || !argssh[0].equals(""));
        if (ssharr == null) {
            // construct in its entirety
            needNewArr = true;
            if (haveArgs) {
                copyArgs = true;
            }
        } else {
            // if the array isn't null, retain the command
            tmpcmd = ssharr[ssharr.length - 1];
            // if the number of arguments changed, need to create a new array
            // and copy the arguments; otherwise, rely on parameter for copying
            if (!haveArgs) {
                if (ssharr.length != 2) {
                    needNewArr = true;
                }
            } else if (ssharr.length != (argssh.length + 2)) {
                needNewArr = true;
                copyArgs = true;
            }
        }
        if (needNewArr) {
            if (haveArgs) {
                ssharr = new String[argssh.length + 3];
            } else {
                ssharr = new String[3];
            }
        }
        if (copyArgs) {
            System.arraycopy(argssh, 0, ssharr, 1, argssh.length);
        }
        ssharr[0] = cmdssh;
        tempbuf.setLength(0);
        tempbuf.append(user);
        tempbuf.append("@");
        tempbuf.append(toHost);
        ssharr[ssharr.length - 2] = tempbuf.toString();
        ssharr[ssharr.length - 1] = tmpcmd;
    }

    /** Set the ssh argument string.
     * @param args The ssh arguments */
    public void setSSHArgs(String args) {
        if (debug) {
            System.out.println(prg + ": setting sshargs to " + args);
        }
        argssh = args.split(" ");
        createSSHCmdArray(true);
    }

    private void createSCPCmdArray() {
        createSCPCmdArray(true);
    }

    /** Create the scp array; the file source and destination are carried
     * forward if already set. The array persists over repeated uses for two
     * reasons: (1) To avoid repeated object creation and (2) to allow the
     * same files to be copied repeatedly.
     * @param copyArgs Whether to force argument replacement (always done
     * if the array hasn't been created or the number of arguments has
     * changed */
    private void createSCPCmdArray(boolean copyArgs) {
        String[] args;
        String tmpsrc = "", tmpdst = "";
        boolean needNewArr = false, haveArgs;

        haveArgs = (argscp.length > 1 || !argscp[0].equals(""));
        if (scparr == null) {
            // construct in its entirety
            needNewArr = true;
            if (haveArgs) {
                copyArgs = true;
            }
        } else {
            // if the array isn't null, retain the command
            tmpsrc = scparr[scparr.length - 2];
            tmpdst = scparr[scparr.length - 1];
            // if the number of arguments changed, need to create a new array
            // and copy the arguments; otherwise, rely on parameter for copying
            if (!haveArgs) {
                if (scparr.length != 3) {
                    needNewArr = true;
                }
            } else if (scparr.length != (argscp.length + 3)) {
                needNewArr = true;
                copyArgs = true;
            }
        }
        if (needNewArr) {
            if (haveArgs) {
                scparr = new String[argssh.length + 3];
            } else {
                scparr = new String[3];
            }
        }
        if (copyArgs) {
            System.arraycopy(argscp, 0, scparr, 1, argscp.length);
        }
        scparr[0] = cmdscp;
        scparr[scparr.length - 2] = tmpsrc;
        scparr[scparr.length - 1] = tmpdst;
    }

    /** Set the scp argument string.
     * @param args The scp arguments */
    public void setSCPArgs(String args) {
        if (debug) {
            System.out.println(prg + ": setting scpargs to " + args);
        }
        argscp = args.split(" ");
        createSCPCmdArray(true);
    }

    /** Used to monitor the state of the local process.
     * @return <tt>true</tt> if the process is running, <tt>false</tt>
     * otherwise */
    public boolean isRunning() {
        return runExe;
    }

    /** Returns the process's exit code. Returns 0 if process is still running
     * or has successfully terminated.
     * @return exit code */
    public int exitCode() {
        return exit;
    }

    /** Stop the process. */
    public void stopProcess() {
        //synchronized(this) {
        runExe = false;
        // need to sleep for the amount of time the loop sleeps
        // so that the process is destroyed
        try {
            Thread.sleep(sleeptime);
        } catch (Exception ignore) {
        }
        //}
    }

    public String execute(String cmd) throws IOException {
        if (cmd == null) {
            throw new NullPointerException("Null command");
        }
        if (cmd.equals("")) {
            throw new NullPointerException("Empty command");
        }
        ssharr[ssharr.length - 1] = cmd;
        return execute();
    }

    public String execute() throws IOException {
        String retstr;

        if (ssharr[ssharr.length - 1] == null) {
            throw new NullPointerException("Null command");
        }
        if (ssharr[ssharr.length - 1].equals("")) {
            throw new NullPointerException("Empty command");
        }
        synchronized (this) {
            retstr = runcmd(ssharr);
        }
        return retstr;
    }

    public String copyFileFromRemote(String src, String dest)
            throws IOException {
        String retstr;

        if (src == null || dest == null) {
            throw new NullPointerException("Null file name");
        }
        if (src.equals("") || dest.equals("")) {
            throw new NullPointerException("Empty file name");
        }
        tempbuf.setLength(0);
        tempbuf.append(user);
        tempbuf.append("@");
        tempbuf.append(toHost);
        tempbuf.append(":");
        tempbuf.append(src);
        scparr[scparr.length - 2] = tempbuf.toString();
        //tempbuf.append(" ");
        //tempbuf.append(dest);
        scparr[scparr.length - 1] = dest;
        //String[] tmparr = new String[]{cmdscp, argscp +" "+ tempbuf.toString()};
        synchronized (this) {
            retstr = runcmd(scparr);
            //retstr = runcmd(tmparr);
        }
        if (debug) {
            System.out.println("Copied file; results: " + retstr);
        }
        return retstr;
    }

    public String copyFileToRemote(String src, String dest)
            throws IOException {
        String retstr;

        if (src == null || dest == null) {
            throw new NullPointerException("Null file name");
        }
        if (src.equals("") || dest.equals("")) {
            throw new NullPointerException("Empty file name");
        }
        //tempbuf.setLength(0);
        //tempbuf.append(src);
        scparr[scparr.length - 2] = src;
        //String[] tmparr = new String[]{cmdscp, argscp, tempbuf.toString(), ""};
        //tempbuf.append(" ");
        tempbuf.setLength(0);
        tempbuf.append(user);
        tempbuf.append("@");
        tempbuf.append(toHost);
        tempbuf.append(":");
        tempbuf.append(dest);
        scparr[scparr.length - 1] = tempbuf.toString();
        //tmparr[3] = tempbuf.toString();
        synchronized (this) {
            retstr = runcmd(scparr);
            //retstr = runcmd(tmparr);
        }
        if (debug) {
            System.out.println("Copied file; results: " + retstr);
        }
        return retstr;
    }

    /** Run the command set by one of the <tt>startProcess</tt> methods. */
    private String runcmd(final String[] cmd) throws IOException {
        boolean done = false;
        if (debug) {
            System.out.print("Running cmd:\n\t");
            for (int i = 0; i < cmd.length; i++) {
                System.out.print("[" + cmd[i] + "] ");
            }
            System.out.println();
        }
        runExe = true;
        outstr.setLength(0);
        Process P = Runtime.getRuntime().exec(cmd);
        if (debug) {
            System.err.println(prg + ": Started: " + P + ", runExe=" + runExe);
        }
        br1 = new BufferedReader(new InputStreamReader(P.getErrorStream()));
        br2 = new BufferedReader(new InputStreamReader(P.getInputStream()));
        // give the process some startup time to catch any output
        //try {
        //	Thread.sleep(sleeptime);
        //} catch (Exception ignore) { }
        while (runExe && !done) {
            //while (!done) {
            try {
                Thread.sleep(sleeptime);  // be nice and sleep a bit
                exit = P.exitValue();
                if (debug) {
                    System.out.println(prg + ": exiting with value " + exit);
                }
                done = true;
            } catch (IllegalThreadStateException e1) {
                // Process has not terminated yet; continue to get I/O
            } catch (InterruptedException ie) {
                //System.err.println(prg +": Caught command interruption!");
                outstr.append(prg);
                outstr.append(": Caught command interruption!");
                done = true;
            }
            while (br1.ready()) { // standard err of the process
                outstr.append(br1.readLine());
                outstr.append("\n");
            }
            while (br2.ready()) { // standard out of the process
                outstr.append(br2.readLine());
                outstr.append("\n");
            }
        }
        P.destroy();
        runExe = false;
        return outstr.toString();
    }
}
