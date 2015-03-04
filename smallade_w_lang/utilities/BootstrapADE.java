/**
 * ADE 1.0 
 * Copyright 1997-2010 HRILab (http://hrilab.org/) 
 * 
 * All rights reserved.  Do not copy and use without permission. 
 * For questions contact Matthias Scheutz at mscheutz@gmail.com
 *
 * BootstrapADE.java
 *
 * @author Paul Schermerhorn
 *
 */
// To make an ADESim executable jar:
// jar cvfe toy1_2d.jar ade.BootstrapADE `find ade -name \*.class ; find com/action -name \*.class ; find com/adesim -name \*.class ; find com/interfaces -name \*.class ` com/adesim/robo.gif com/adesim/nobo.gif com/vision/MemoryObject.class com/vision/ConfidenceLevel.class com/simbad/config/eigenmann8 com/simbad/config/default
// To make a Simbad executable jar:
// cp simbad.jar toy1.jar ; jar uvfe toy1.jar ade.BootstrapADE `find ade -name \*.class ; find com/action -name \*.class ; find com/simbad -name \*.class ; find com/interfaces -name \*.class ` com/vision/MemoryObject.class com/vision/ConfidenceLevel.class com/simbad/config/eigenmann8 com/simbad/config/default
// To run (requres architecture class file in current directory):
// java -jar toy1_2d.jar
// or:
// java -jar toy1_2d.jar MyActionComponentArch
// See com/action/ActionComponentArch.java for a simple architecture template.
package utilities;

import java.io.*;

/**
 * BootstrapADE is an example of how to start ADE from an executable jar
 * file.  The servers, etc., are all hard-coded; a general solution is
 * low priority right now...
 */
public class BootstrapADE {

    public static BufferedReader br, br2, br3;
    public static Process p, p2, p3;
    public static String arch = "com.action.DefaultActionComponentArch";
    public static boolean readProcs = true;
    public static boolean verbose = true;
    public static String jarfile;
    public static boolean Testing = false;                  //to use when grading or testing.
    public static String StatsFile = "./AssignmentResults.txt"; //default file to write and append to when testing.
    public static long SimStart;                           //added for testing
    public static double SimTotal;                          //added for testing
    public static String InitX = "-4";                       //Default value
    public static String InitY = "0";                      //Default value
    public static String cfgFile = "eigenmann8";            //Default config file
    public static int BotOrientation = 0;                   //need to set for every assignment
    public static String strBotOri = Integer.toString(BotOrientation);
    public static boolean setRand = false;
    public static boolean setInitPos = false;

    public static void main(String[] args) throws Exception {
        // Look for optional architecture class name
        // and parsing other command line arguments
        if (args.length > 0) {
            arch = args[0];
            // Check for architecture class file
            File archfile = new File(arch + ".class");
            if (!archfile.exists()) {
                System.out.println("Can't find architecture file " + arch + ".class");
                System.exit(-1);
            }
            if (args.length > 1) {
                for (int idx = 1; idx < args.length; idx++) {
                    if (args[idx].equalsIgnoreCase("-rand")) {
                        setRand = true;
                        BotOrientation = (int) (Math.random() * 360);
                        strBotOri = Integer.toString(BotOrientation);
                    }
                    if (args[idx].equalsIgnoreCase("-initpose")) {
                        setInitPos = true;
                        InitX = args[idx + 1];
                        InitY = args[idx + 2];
                        strBotOri = args[idx + 3];
                    }
                    if (args[idx].equalsIgnoreCase("-test")) {
                        Testing = true;
                        System.out.println("Running in Testing mode to grade assignment");
                    }
                    if (args[idx].equalsIgnoreCase("-ConfigFile")) {
                        cfgFile = args[idx + 1];
                    }
                    if (args[idx].equalsIgnoreCase("-nov")) {
                        verbose = false;
                        System.out.println("no longer verbose");
                    }
                    if (args[idx].equalsIgnoreCase("-testFile")) {
                        StatsFile = "./" + args[idx + 1] + ".txt";
                    }
                }
            }

        }        // Need the jarfile for the classpath
	//        jarfile = ade.BootstrapADE.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        jarfile = BootstrapADE.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        // Don't want to worry about Windows path, so just getting the name
        jarfile = (new File(jarfile)).getName();
        // Add hook to kill the servers
        Runtime.getRuntime().addShutdownHook(
                new Thread() {

                    public void run() {
                        shutdown();
                    }
                });

        // Registry
        String[] cmd = {"java", "-Djava.rmi.server.hostname=127.0.0.1", "-cp", jarfile, "ade.ADERegistryImpl", "-l", "127.0.0.1", "-o", "127.0.0.1"};
        System.out.println("BootstrapADE starting ADERegistryImpl");
        try {
            // Build process
            ProcessBuilder pb = new ProcessBuilder(cmd);
            // Redirect stderr
            pb.redirectErrorStream(true);
            // Execute
            p = pb.start();
            // Capture and consume stdout
            br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            new Thread() {

                public void run() {
                    String out;
                    while (readProcs) {
                        try {
                            out = br.readLine();
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
            System.err.println("Exception starting ADE:\n" + e);
            System.exit(1);
        }
        // Give Registry some time to start
        try {
            Thread.sleep(5000);
        } catch (Exception e) {
        }
        // Simbad

        //String[] cmd2 = {"java", "-Djava.rmi.server.hostname=127.0.0.1", "-cp", jarfile, "com.simbad.SimbadComponentImpl", "-l", "127.0.0.1", "-r", "127.0.0.1", "-cfg", "com/simbad/config/eigenmann8", "-deftv", "0.5", "-winsize", "500", "300"};

        // ADESim
        //String[] cmd2 = {"java", "-Djava.rmi.server.hostname=127.0.0.1", "-cp", jarfile, "com.adesim.ADESimComponentImpl", "-l", "127.0.0.1", "-r", "127.0.0.1", "-cfg", "com/adesim/config/Assignment1", "-initpose", "-2", "-2", strBotOri};
        //String[] cmd2 = {"java", "-Djava.rmi.server.hostname=127.0.0.1", "-cp", jarfile, "com.adesim.ADESimComponentImpl", "-l", "127.0.0.1", "-r", "127.0.0.1", "-cfg", "com/adesim/config/Assignment2", "-initpose", "-4", "-4", strBotOri};

        String[] cmd2 = {"java", "-Djava.rmi.server.hostname=127.0.0.1", "-cp", jarfile, "com.adesim.ADESimComponentImpl", "-l", "127.0.0.1", "-r", "127.0.0.1", "-cfg", "com/adesim/config/" + cfgFile, "-initpose", InitX, InitY, strBotOri};
        System.out.println(cmd2);
        System.out.println("BootstrapADE starting ADESimComponentImpl");
        try {
            // Build process
            ProcessBuilder pb = new ProcessBuilder(cmd2);
            // Redirect stderr
            pb.redirectErrorStream(true);
            // Execute
            p2 = pb.start();
            // Capture and consume stdout
            br2 = new BufferedReader(new InputStreamReader(p2.getInputStream()));
            if (Testing) {
                SimStart = System.currentTimeMillis();
            }
            new Thread() {

                public void run() {
                    String out;
                    while (readProcs) {
                        try {
                            out = br2.readLine();
                            if (Testing) {
                                // Create file
                                FileWriter fstream = new FileWriter(StatsFile, true);
                                BufferedWriter BWout = new BufferedWriter(fstream);
                                SimTotal = SimTotal + (double) (System.currentTimeMillis() - SimStart);
                                if (out.startsWith("->")) {
                                    BWout.write(out);
                                }
                                if (out.equalsIgnoreCase("You Win!!!!!!")) {
                                    try {
                                        BWout.write("Success!!!!");
                                        BWout.write("Sim Time = " + SimTotal + " " + "Orientation = " + strBotOri + "\n");
                                        //Close the output stream
                                        BWout.close();
                                        shutdown();
                                    } catch (Exception e) {//Catch exception if any
                                        System.err.println("Error: " + e.getMessage());
                                    }
                                    System.out.println(SimTotal);
                                }
                                if (out.equalsIgnoreCase("Stall!")) {
                                    try {
                                        BWout.write("Sim Time = " + SimTotal + " " + "Orientation = " + strBotOri + "\n");
                                        BWout.write("Program Stalled!!!!     @" + SimTotal + "\n");
                                        //Close the output stream
                                        BWout.close();
                                        shutdown();
                                    } catch (Exception e) {//Catch exception if any
                                        System.err.println("Error: " + e.getMessage());
                                    }
                                    System.out.println(SimTotal);
                                }
                                if ((SimTotal / 1000) > (60 * 60 * 5)) {
                                    try {
                                        BWout.write("Sim Time = " + SimTotal + " " + "Orientation = " + strBotOri + "\n");
                                        BWout.write("Program Timed out after 5 minutes!!!!     " + SimTotal + "\n");
                                        //Close the output stream
                                        BWout.close();
                                        shutdown();
                                    } catch (Exception e) {//Catch exception if any
                                        System.err.println("Error: " + e.getMessage());
                                    }
                                }
                            }
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
            System.err.println("Exception starting ADE:\n" + e);
            System.exit(1);
        }

        // Give ADESim some time to start

        try {
            Thread.sleep(5000);
        } catch (Exception e) {
        }
        // Action
        // Simbad
        //String[] cmd3 = {"java", "-Djava.rmi.server.hostname=127.0.0.1", "-cp", jarfile + File.pathSeparator + ".", arch, "-l", "127.0.0.1", "-r", "127.0.0.1", "-simbad"};
        // ADESim
        String[] cmd3 = {"java", "-Djava.rmi.server.hostname=127.0.0.1", "-cp", jarfile + File.pathSeparator + ".", arch, "-l", "127.0.0.1", "-r", "127.0.0.1", "-adesim"};
        System.out.println("BootstrapADE starting " + arch);
        try {
            // Build process
            ProcessBuilder pb = new ProcessBuilder(cmd3);
            // Redirect stderr
            pb.redirectErrorStream(true);
            // Execute
            p3 = pb.start();
            // Capture and print stdout
            br3 = new BufferedReader(new InputStreamReader(p3.getInputStream()));
            new Thread() {

                public void run() {
                    String out;
                    while (readProcs) {
                        try {
                            out = br3.readLine();
                            if (out != null) {
                                System.out.println(out);
                            }
                        } catch (IOException ioe) {
                            System.out.println("Error reading from proc: " + ioe);
                        }
                    }
                }
            }.start();
        } catch (Exception e) {
            System.err.println("Exception starting ADE:\n" + e);
            System.exit(1);
        }




        // Keep parent process alive, waiting to kill children
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
        }
    }

    public static void shutdown() {
        System.out.println("BootstrapADE: killing processes");
        readProcs = false;
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
        }
        p.destroy();
        p2.destroy();
        p3.destroy();
    }
}
// vi:ai:smarttab:expandtab:ts=8 sw=4

