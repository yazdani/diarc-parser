/**
 * ADE 1.0 
 * Copyright 1997-2010 HRILab (http://hrilab.org/) 
 * 
 * All rights reserved.  Do not copy and use without permission. 
 * For questions contact Matthias Scheutz at mscheutz@gmail.com
 *
 * RunADE.java
 *
 * @author Paul Schermerhorn
 *
 */
// To make an executable jar:
// ant -f buildade.xml runade
// To run (requres architecture class file in current directory):
// java -jar RunADE.jar
package utilities;

import java.io.*;
import java.util.*;

/**
 * RunADE is an example of how to start ADE from an executable jar
 * file.  The servers, etc., are read from the runade.list file.
 */
public class RunADE {
    public static ArrayList<String[]> commands;
    public static BufferedReader br;
    public static ArrayList<Process> processes;
    public static Process registry = null;
    public static Process p;
    public static boolean readProcs = true;
    public static boolean verbose = true;
    public static String jarfile;
    public static String java;
    public static String ip = "127.0.0.1";
    public static String port = "1099";
    private static String filename = "runade.list";

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            parseArgs(args);
        }
        // Need the jarfile for the classpath
        jarfile = RunADE.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        // Don't want to worry about Windows path, so just getting the name
        jarfile = (new File(jarfile)).getName();
        // use same jre
        //java = (new File(System.getProperty("java.home")).getParent())+"/bin/java";
        //java = System.getProperty("java.home")+"/bin/java";
        java = "java";
        // Add hook to kill the servers
        Runtime.getRuntime().addShutdownHook(
                new Thread() {
                    @Override
                    public void run() {
                        if (readProcs) {
                            shutdown();
                        }
                    }
                });

        commands = new ArrayList<String[]>();
        processes = new ArrayList<Process>();
        commands.add(argArray(java, "-Djava.rmi.server.hostname="+ip, "-cp", jarfile, "-Dcomponent=ade.ADERegistry", "ade.ADEComponentImpl", "-l", ip, "-o", ip));
        parseComponentList(new File(filename));

        long wait = 7500;
        for (String[] cmd:commands) {
            String serverName = (cmd[4]).split("=")[1];
            System.out.println("ADE starting " + serverName);
            try {
                // Build process
                ProcessBuilder pb = new ProcessBuilder(cmd);
                // Redirect stderr
                pb.redirectErrorStream(true);
                // Execute
                p = pb.start();
                if (registry == null)
                    registry = p;
                processes.add(p);
                // Capture and consume stdout
                br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                new Thread() {

                    @Override
                    public void run() {
                        String out;
                        BufferedReader lbr = br;
                        while (readProcs) {
                            try {
                                out = lbr.readLine();
                                if (verbose && (out != null)) {
                                    System.out.println(out);
                                }
                            } catch (IOException ioe) {
                                System.out.println("Error reading from proc: " + ioe);
                            }
                        }
                    }
                }.start();
            } catch (Exception e) {
                System.err.println("Exception starting " + serverName + ":\n" + e);
                System.exit(1);
            }
            // Give some time to start
            try {
                Thread.sleep(wait);
            } catch (Exception e) {
            }
            wait = 3000;
        }
    }

    // a little trickeration
    public static String[] argArray(String... args) {
        return args;
    }

    private static void parseComponentList(File servers) {
        BufferedReader serverList = null;
        int line = 0;
        try {
            // First try to read it from the filesystem
            FileReader fr = new FileReader(servers);
            serverList = new BufferedReader(fr);
        } catch (FileNotFoundException fnfe) {
            System.out.println("File not found: " + servers.getPath());
            // Then try to get it from the jar file
            ClassLoader cl;
            try {
                cl = Class.forName("ade.RunADE").getClassLoader();
                InputStream in = cl.getResourceAsStream(servers.getPath().replace("\\","/"));
                InputStreamReader inr = new InputStreamReader(in);
                serverList = new BufferedReader(inr);
            } catch (ClassNotFoundException ex) {
                System.err.println("Unable to find server list: " + servers.getPath());
                System.err.println(ex);
                ex.printStackTrace();
                System.exit(-3);
            }
            System.out.println("Loading internal server list.");
        } 
        try {
            String server;
            String[] tmp = new String[1];
            while ((server = serverList.readLine()) != null) {
                line++;
                if (server.startsWith("#"))
                    continue;
                String[] tokens = server.trim().split("\\s+");
                ArrayList<String> command = new ArrayList<String>();
                command.add(java);
                command.add("-Djava.rmi.server.hostname="+ip);
                command.add("-cp");
                command.add(jarfile);
                command.add("-Dcomponent="+tokens[0]);
                command.add("ade.ADEComponentImpl");
                command.add("-l");
                command.add(ip);
                command.add("-r");
                command.add(ip);
                for (int i=1; i<tokens.length; i++) {
                    command.add(tokens[i]);
                }
                commands.add(command.toArray(tmp));
            }
        } catch (Exception e) {
            System.err.println("Exception parsing server list file " + line + ": " + e);
            System.exit(-4);
        }
    }

    public static void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-runlist")) {
                if (args.length == (i+1)) {
                    System.err.println("Missing server list file.");
                    usage();
                }
                filename = args[++i];
                System.out.println("Loading servers from "+filename);
            } else if (args[i].equalsIgnoreCase("-ip")) {
                if (args.length == (i+1)) {
                    System.err.println("Missing IP address.");
                    usage();
                }
                ip = args[++i];
            } else if (args[i].equalsIgnoreCase("-port")) {
                if (args.length == (i+1)) {
                    System.err.println("Missing port specification.");
                    usage();
                }
                port = args[++i];
            } else {
                System.err.println("Unknown argument: " + args[i]);
                usage();
            }
        }
    }

    public static void usage() {
        System.err.println();
        System.err.println("Valid arguments are:");
        System.err.println("    -ip <addr>         the IP address to use (default: 127.0.0.1)");
        System.err.println("    -port <port>       the port to use (default: 1099)");
        System.err.println("    -runlist <file>    the file to load server run list from");
        System.exit(1);
    }

    public static void shutdown() {
        if (!readProcs)
            return;
        System.out.println("RunADE: killing processes");
        readProcs = false;
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
        }
        registry.destroy();
        try {
            Thread.sleep(3000);
        } catch (Exception e) {
        }
        for (Process p:processes) {
            try {
                p.waitFor();
            } catch (InterruptedException ignore) {
            }
        }
    }
}
// vi:ai:smarttab:expandtab:ts=8 sw=4
