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
 * Utility to start and stop processes on both local and remote machines. Once
 * created, the process can be started using the {@link #startProcess}, stopped
 * using the {@link #stopProcess}, and monitored using the {@link
 * #isRunning} methods, while the exit code is returned by {@link #exitCode}.
 * <p> Some items to note: <ul> <li>An {@link ade.ADEHostInfo ADEHostInfo}
 * object is required, which contains information about the (local) host.</li>
 * <li>There are two {@link #startProcess} methods, one with no parameters the
 * other that takes an {@link ade.ADEHostInfo ADEHostInfo} object as a
 * parameter. The unparameterized method will execute the process on the local
 * host; the other will determine if the host parameter is local or remote to
 * start the process.</li> <li>The directory in which to start the process (set
 * either at creation or by using the {@link #changeStartDirectory} method)
 * refers to a directory <b>on the execution host</b>.</li> <li>The arguments of
 * the start command (either the local shell or <tt>ssh</tt> command) can be
 * changed by modifying the appropriate fields in the {@link
 * ade.ADEHostInfo ADEHostInfo} structure.</li> <li>The arguments of the process
 * can be changed at run-time (so long as the process is not running) using the
 * {@link #changeArgs} method.</li> <li>The output stream (defaults to
 * <tt>System.out</tt>) can be redirected at run-time and turned on/off
 * (defaults to on) using the {@link #setOutputStream} and {@link #showOutput}
 * methods.</li> <li>The output stream is captured periodically, using a
 * <tt>sleep</tt> of length set by {@link #setSleepTime} (default is 1000
 * ms).</li> <li>There is an internal private <tt>cleanup</tt> method for making
 * sure the process is terminated; at this point, it simply destroys the local
 * process, although in the future functionality may be added to better handle
 * remote process cleanup.</li> </ul> <p> While this class is used by an
 * {@link ade.ADERegistryImpl ADERegistryImpl} to start
 * {@link ade.ADEComponentImpl ADEComponentImpl}s, it can also be used to start local,
 * external programs that are "wrapped" inside an {@link ade.ADEComponentImpl
 * ADEComponentImpl}. For instance, this is useful for programs that communicate
 * over a socket to transfer data between the component and the executable. Code to
 * do this for executable <tt>prg</tt>, with an array of arguments <tt>args</tt>
 * that is started in directory <tt>dir</tt>, will often look like this:<p>
 * <quote><tt> ADELocalProcessStarter exe;<br> ADEHostInfo ahi =
 * requestHostInfo(this);<br> try {</br> <div style="margin-left: 40px;"> exe =
 * new ADELocalProcessStarter(ahi, dir, prg, args);<br> exe.startProcess();<br>
 * Thread.sleep(ADEGlobals.DEF_RPPULSE); </div> } catch (Exception e) {<br> <div
 * style="margin-left: 40px;"> System.err.println("Exception starting "+ prg
 * +":");<br> e.printStackTrace();<br> System.exit(1); </div> }<br> </tt>
 * </quote>
 */
final public class ADELocalProcessStarter {
    // included so we can later read files even if the class changed...

    private static final long serialVersionUID = 7526472295622776147L;
    final static String prg = "ADELocalProcessStarter";
    final static boolean debug = false;
    final static boolean verbose = false;
    final static boolean showCommand = false;
    private ADEHostInfo myHost;         // the local host
    private ADEHostInfo toHost;         // the remote host, if needed
    private boolean showExitCode = false;// display non-0 exit code to console
    private boolean showOutput = true;  // redirect output to console
    private PrintStream outputStream = System.out;
    private String[] cmdexe;            // the command to execute
    private String exedir;              // the directory in which to start
    private int sleeptime = 1000;       // sleep a bit between stream checks
    private boolean runExe = false;     // for process monitoring
    private int exit = 0;               // process exit value
    private Boolean runguard = new Boolean(true);

    // disable construction with no parameters
    private ADELocalProcessStarter() {
    }

    /**
     * Constructor for a process that takes no arguments. Takes an {@link
     * ade.ADEHostInfo ADEHostInfo} object (from which the shell command and its
     * arguments are extracted) and executable name as a parameter; it is
     * assumed that no path needs to be specified (that is, if executing
     * locally, the program can be started in the current working directory; if
     * remote, the program can be started in the login directory of the remote
     * host).
     *
     * @param ahi The {@link ade.ADEHostInfo ADEHostInfo} object
     * @param exe The executable name
     * @throws SecurityException
     * @throws IOException if a directory or command does not exist or is has
     * improper permissions
     */
    public ADELocalProcessStarter(ADEHostInfo ahi, String exe)
            throws SecurityException, IOException {
        this(ahi, null, exe, null);
    }

    /**
     * Constructor to start a process that takes no arguments, but should be
     * started in a specific directory. Takes an {@link ade.ADEHostInfo
     * ADEHostInfo} object (from which the shell command and its arguments are
     * extracted), a start directory, and executable name as parameters. Note
     * that <tt>stdir</tt> can be <tt>null</tt>, effectively using the
     * <tt>ADELocalProcessStarter(ahi, exe)</tt> constructor.
     *
     * @param ahi The {@link ade.ADEHostInfo ADEHostInfo} object
     * @param stdir The start directory
     * @param exe The executable name
     * @throws SecurityException
     * @throws IOException if a directory or command does not exist or is has
     * improper permissions
     */
    public ADELocalProcessStarter(ADEHostInfo ahi, String stdir, String exe)
            throws SecurityException, IOException {
        this(ahi, stdir, exe, null);
    }

    /**
     * Constructor to start a process that takes arguments. Takes an {@link
     * ade.ADEHostInfo ADEHostInfo} object (from which the shell command and its
     * arguments are extracted), a start directory, the executable name, and the
     * executable's arguments as parameters. Note that <tt>args</tt> can be
     * <tt>null</tt>, effectively using the <tt>ADELocalProcessStarter(ahi,
     * exe)</tt> constructor.
     *
     * @param ahi The {@link ade.ADEHostInfo ADEHostInfo} object
     * @param exe The executable name
     * @param args The executable arguments
     * @throws SecurityException
     * @throws IOException if a directory or command does not exist or is has
     * improper permissions
     */
    public ADELocalProcessStarter(ADEHostInfo ahi, String exe, String[] args)
            throws SecurityException, IOException {
        this(ahi, null, exe, args);
    }

    /**
     * Constructor to start a process. Takes an {@link ade.ADEHostInfo
     * ADEHostInfo} object (from which the shell command and its arguments are
     * extracted), in addition to the directory in which to start, executable
     * name, and the executable's arguments as parameters. Note that
     * <tt>stdir</tt> and/or <tt>args</tt> can be <tt>null</tt>, effectively
     * using one of the other constructors.
     *
     * @param ahi The {@link ade.ADEHostInfo ADEHostInfo} object
     * @param stdir The directory in which the executable is started
     * @param exe The executable name (can include a path)
     * @param args The executable's arguments
     * @throws SecurityException
     * @throws IOException if a directory or command does not exist or is has
     * improper permissions
     */
    public ADELocalProcessStarter(ADEHostInfo ahi,
            String stdir, String exe, String[] args)
            throws SecurityException, IOException {
        if (debug) {
            System.out.println(prg + ":\n\tstdir [" + stdir + "]\n\texe  [" + exe + "]");
            System.out.print("\targs [");
            if (args != null) {
                for (String arg : args) {
                    System.out.print(" " + arg);
                }
            } else {
                System.out.print("none");
            }
            System.out.println("]");
        }
        // store the reference to the host and the executable command-line
        // startup separately; this decouples them, allowing either the shell
        // arguments or executable arguments to be changed between individual
        // process executions
        if (stdir != null) {
            exedir = stdir;
        }
        myHost = ahi;
        if (args != null) {
            cmdexe = new String[1 + args.length];
            cmdexe[0] = exe;
            System.arraycopy(args, 0, cmdexe, 1, args.length);
        } else {
            cmdexe = new String[]{exe};
        }
    }

    /**
     * Change the start directory. The start directory can only be modified if
     * the process is not currently running.
     *
     * @param newstdir The new start directory
     * @throws IllegalStateException if the process is currently running
     */
    public void changeStartDirectory(String newstdir) {
        if (isRunning()) {
            throw new IllegalStateException("Process currently exectuing");
        }
        exedir = newstdir;
    }

    /**
     * Change the executable arguments. Arguments can only be modified if the
     * process is not currently running.
     *
     * @param newargs The new arguments
     * @throws IllegalStateException if the process is currently running
     */
    public void changeArgs(String[] newargs) {
        // might we want to halt and restart the process?
        if (isRunning()) {
            throw new IllegalStateException("Process currently exectuing");
        }
        String fullexe = cmdexe[0];
        cmdexe = new String[1 + newargs.length];
        cmdexe[0] = fullexe;
        System.arraycopy(newargs, 0, cmdexe, 1, newargs.length);
    }

    /**
     * Set the stream for output redirection.
     *
     * @param out the stream for output redirection
     */
    public void setOutputStream(PrintStream out) {
        outputStream = out;
    }

    /**
     * Turns the process output redirection on or off.
     *
     * @param b on if <tt>true</tt>, off if <tt>false</tt>
     */
    public void showOutput(boolean b) {
        showOutput = b;
    }

    /**
     * Toggles the non-normal exit code display (default is on).
     *
     * @param b On if <tt>true</tt>, off if <tt>false</tt>
     */
    public void showExitCode(boolean b) {
        showExitCode = b;
    }

    /**
     * Set the sleep time of output monitoring.
     *
     * @param t The sleep time (in ms)
     */
    public void setSleepTime(int t) {
        sleeptime = t;
    }

    /**
     * Used to monitor the state of the local process.
     *
     * @return <tt>true</tt> if the process is running, <tt>false</tt> otherwise
     */
    public boolean isRunning() {
        boolean retb;
        synchronized (runguard) {
            retb = runExe;
        }
        return retb;
    }

    /**
     * Returns the process's exit code. Returns 0 if process is still running or
     * has successfully terminated.
     *
     * @return exit code
     */
    public int exitCode() {
        return exit;
    }

    /**
     * Stop the process.
     */
    public void stopProcess() {
        setRunExe(false);
        // need to sleep for the amount of time the loop sleeps
        // so that the process is destroyed
        try {
            Thread.sleep(sleeptime);
        } catch (Exception ignore) {
        }
    }

    /**
     * Start the stored process locally as a foreground process, redirecting its
     * output to the console. On startup, set the <tt>runExe</tt> variable to
     * <tt>true</tt>; upon termination, set it to <tt>false</tt>, allowing
     * monitoring of state using the {@link #isRunning isRunning} method.
     */
    public void startProcess() {
        startProcess(false);
    }

    /**
     * Start the stored process locally, redirecting its output to the console.
     * On startup, set the <tt>runExe</tt> variable to <tt>true</tt>; upon
     * termination, set it to <tt>false</tt>, allowing monitoring of state using
     * the {@link #isRunning isRunning} method.
     *
     * @param backgroundit Start the process in the background (detaching it
     * from the parent process)
     */
    public void startProcess(boolean backgroundit) {
        String[] cmd;
        StringBuilder sb;

        /*
         // since it's local, we can check directory/file existence
         if (exedir != null) {
         ADEGlobals.checkDir(exedir, true, false);
         }
         ADEGlobals.checkCommand(cmdexe[0]);
         */
        // set up the array to execute
        if (backgroundit && myHost.hostos.equals(ADEGlobals.HostOS.WINDOWSXP)) {
            // need to make adjustment to background the process; UNIX is
            // handled below...
            // replace the "/C" with "/K" if it's there
            cmd = new String[myHost.shell.length];
            System.arraycopy(myHost.shell, 0, cmd, 0, myHost.shell.length);
            for (int i = 0; i < cmd.length; i++) {
                if (cmd[i].equals("/C")) {
                    cmd[i] = myHost.bkgarg;
                }
            }
        } else {
            cmd = new String[myHost.shell.length];
            System.arraycopy(myHost.shell, 0, cmd, 0, myHost.shell.length);
        }
        sb = new StringBuilder();
        //if (myHost.hostos.equals(ADEGlobals.HostOS.WINDOWSXP))
        //	sb.append("\"");
        if (exedir != null) {
            sb.append("cd ");
            sb.append(exedir);
            sb.append(myHost.hostos.csep());
        }
        for (String str : cmdexe) {
            sb.append(str);
            sb.append(" ");
        }
        if (backgroundit && !myHost.hostos.equals(ADEGlobals.HostOS.WINDOWSXP)) {
            // add an "&" to the end
            sb.append(myHost.bkgarg);
        } else {
            sb.setLength(sb.length() - 1); // remove last space
        }
        //if (myHost.hostos.equals(ADEGlobals.HostOS.WINDOWSXP))
        //	sb.append("\"");
        cmd[cmd.length - 1] = sb.toString();
        if (debug) {
            System.err.print(prg + ": start command=[");
            for (String part : cmd) {
                System.err.print(part + " ");
            }
            System.err.println("]");
        }
        toHost = null;
        startUp(cmd);
    }

    /**
     * Start the stored process on a remote machine in the foreground,
     * redirecting its output to the console. On startup, set the
     * <tt>runExe</tt> variable to <tt>true</tt>; upon termination, set it to
     * <tt>false</tt>, allowing monitoring of state using the {@link
     * #isRunning isRunning} method.
     */
    public void startProcess(ADEHostInfo tohost) {
        startProcess(tohost, false);
    }

    /**
     * Start the stored process on a remote machine in the foreground,
     * redirecting its output to the console. On startup, set the
     * <tt>runExe</tt> variable to <tt>true</tt>; upon termination, set it to
     * <tt>false</tt>, allowing monitoring of state using the {@link
     * #isRunning isRunning} method.
     *
     * @param backgroundit Start the process in the background (detaching it
     * from the parent process)
     */
    public void startProcess(ADEHostInfo tohost, boolean backgroundit) {
        String[] cmd;
        StringBuilder sb;

        // set up the array to execute, still want to check if it's local
        if (tohost.hostip.equals("127.0.0.1")
                || tohost.hostip.equals("localhost")
                || tohost.hostip.equals(myHost.hostip)) {
            startProcess(backgroundit);
        } else {
            sb = new StringBuilder();
            cmd = new String[myHost.ssh.length];
            System.arraycopy(myHost.ssh, 0, cmd, 0, myHost.ssh.length);
            // fill in username@host
            sb.append(tohost.sshusername);
            sb.append("@");
            sb.append(tohost.hostip);
            // fill in command
            cmd[cmd.length - 2] = sb.toString();
            sb.setLength(0);
            if (myHost.hostos.equals(ADEGlobals.HostOS.WINDOWSXP)) {
                sb.append("\"");
            }
            if (exedir != null) {
                sb.append("cd ");
                sb.append(exedir);
                sb.append(myHost.hostos.csep());
            }
            for (String str : cmdexe) {
                sb.append(str);
                sb.append(" ");
            }
            sb.setLength(sb.length() - 1); // remove last space
            if (myHost.hostos.equals(ADEGlobals.HostOS.WINDOWSXP)) {
                sb.append("\"");
            }
            cmd[cmd.length - 1] = sb.toString();
            if (debug) {
                System.err.print(prg + ": start command=[");
                for (String part : cmd) {
                    System.err.print(part + " ");
                }
                System.err.println("]");
            }
            toHost = tohost;
            startUp(cmd);
        }
    }

    /**
     * Run the command set by one of the <tt>startProcess</tt> methods.
     */
    private void startUp(final String[] cmd) {
        if (showCommand || debug) {
            System.out.print(prg + ": Startcommand=[");
            for (String st : cmd) {
                System.out.print(" " + st);
            }
            System.out.println("]");
            System.out.flush();
        }
        // spawn a thread for execution
        setRunExe(true);
        try {
            new Thread() {
                BufferedReader br1 = null, br2 = null;
                PrintWriter out = null;
                String outstr;

                public void run() {
                    try {
                        Process P = Runtime.getRuntime().exec(cmd);
                        if (debug) {
                            System.err.print("\t" + this + " Started: " + P);
                            System.err.println(", runExe=" + runExe);
                        }
                        br1 = new BufferedReader(
                                new InputStreamReader(P.getErrorStream()));
                        br2 = new BufferedReader(
                                new InputStreamReader(P.getInputStream()));
                        out = new PrintWriter(P.getOutputStream());
                        // give the process some startup time to catch any output
                        try {
                            Thread.sleep(sleeptime);
                        } catch (Exception ignore) {
                        }
                        while (runExe) {
                            if (showOutput) {
                                // standard err of the process
                                while (br1.ready()) {
                                    // do something with it...
                                    outstr = br1.readLine();
                                    outputStream.println(outstr);
                                }
                                // standard out of the process
                                while (br2.ready()) {
                                    // do something with it...
                                    outstr = br2.readLine();
                                    outputStream.println(outstr);
                                }
                                outputStream.flush();
                            }
                            try {
                                Thread.sleep(sleeptime);  // be nice and sleep a bit
                                exit = P.exitValue();
                                break;
                            } catch (IllegalThreadStateException e1) {
                                // Process has not terminated yet; continue to get I/O
                            } catch (InterruptedException ignore) {
                                System.err.println(this + ": Caught interruption!");
                                break;
                            }
                        }
                        cleanup(P);
                    } catch (Exception e) {
                        System.err.print(prg + " " + this + ": ");
                        System.err.println("Exception starting " + cmd[cmd.length - 1]);
                        e.printStackTrace();
                    }
                    if (exit != 0 && showOutput && showExitCode) {
                        System.err.print(prg + " " + this + ": ");
                        System.err.println("exited improperly (" + exit + ")");
                    }
                    setRunExe(false);
                }
            }.start();
        } catch (IllegalThreadStateException e) {
            System.err.println(prg + " " + this + ": Caught improper cmd[] exit!");
        }
        // If necessary, higher-level programs should take care of sleeping
        //try {
        //	Thread.sleep(5000);
        //} catch (Exception ignore) { }
    }

    /**
     * Internal private method that sets the endless loop control.
     *
     * @param b if true, run the process
     */
    private void setRunExe(boolean b) {
        if (b) {
            exit = 0;
        }
        synchronized (runguard) {
            runExe = b;
        }
    }

    /**
     * Internal private method that cleans up after a started process. At this
     * point, the process is simply destroyed.
     *
     * @param p The (now-terminal) process
     */
    private void cleanup(Process p) {
        if (showCommand) {
            System.out.println(prg + ": killing process " + cmdexe[0]);
            //} else {
            //	System.out.println(prg +": killing process");
        }
        p.destroy();
        toHost = null;
    }
}
