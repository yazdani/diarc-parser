package com.adesim.assignments.a1;

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


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import ade.ADEComponentImpl;

/**
 * BootstrapADE is an example of how to start ADE from an executable jar
 * file.  The servers, etc., are all hard-coded; a general solution is
 * low priority right now...
 */
public class BootstrapADE {
	/* To make an ADESim executable jar:

	FROM THE ADE HOME DIRECTORY
	./mkade adesim
	ant -f com/adesim/assignments/a1/build.xml 

	This will compile all of the simulator, including the BootstrapADE file,
	and will then generate an appropriate jar, which it will place in the 
	home folder (as "toy1.jar", as specified in the build.xml file).
	
		
	See com/adesim/sample/ActionComponentArch.java for a sample action architecture.

	Students should compile with with:   
	      javac -cp toy1.jar ArchImpl.java Arch.java
	  run with:
	      java -jar toy1.jar ArchImpl
	      
	*/
	

	static final long PAUSE_DURATION_BETWEEN_STARTUPS = 3000;

	
	static Process registryProcess;
	static String arch;
	static boolean readProcs = true; // must always be set to true at start, this is what keeps the bootstrap running!
	static boolean verbose = true;
	static String jarfile;
	static boolean testing = false;                  //to use when grading or testing.
	static String statsFile = "./AssignmentResults.txt"; //default file to write and append to when testing.
	static long simStartTime;                           //to make sure doesn't go over maximum time limit

	
	// settings pertaining to the particular assignment:
	static final String BUILT_IN_CFG_PREFIX = "com" + File.separator + 
					"adesim" + File.separator + "config" + File.separator;
	static String cfgFile = BUILT_IN_CFG_PREFIX + "assignment1.xml";       //Default config file
	
	static boolean setInitPos = true;				 //By default, start at beginning of the hallway
	static String InitX = "1";                       //Default value
	static String InitY = "1";                       //Default value
	static String InitTheta = "0";                   //Default value

	static String acceleration = null;

	static ArrayList<String> externalArgs = new ArrayList<String>();

	// Environment view and edit options, see additionalUsageInfo() of ADESimEnvironmentComponent
	//     to see what those numbers mean.
	static String environmentViewOptions = "3030";
	static String environmentEditOptions = "00000";
	

	public static void main(String[] args){
		parseArgs(args);
		loadJar();

		startRegistry();
		startSimEnvironment();
		startSimPioneer();
		startActionArchitecture();


		// Keep parent process alive, waiting to kill children
		while (readProcs) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				System.err.println("Thread could not sleep, exception: " + e);
			}
		}

		// once out of readProcs
		System.out.println("=============================================");
		System.out.println("=============SIMULATION COMPLETE=============");
		System.out.println("=============================================\n");
	}

	private static void startActionArchitecture() {    	
		ArrayList<String> commandsBuilder = new ArrayList<String>();
		commandsBuilder.add("java");
		for (String eachExternalArg : externalArgs) {
			commandsBuilder.add(eachExternalArg);
		}
		String[] permanentCommands = {"-Dcomponent=" + arch, 
				"-Djava.rmi.server.hostname=127.0.0.1", "-cp", 
				"." + File.pathSeparator + jarfile, "ade.ADEComponentImpl", 
				"-l", "127.0.0.1", "-r", "127.0.0.1", 
				"-component", "com.adesim.SimPioneerComponent"};
		for (String each : permanentCommands) {
			commandsBuilder.add(each);
		}


		String[] commandsArray = new String[commandsBuilder.size()];
		for (int i = 0; i < commandsBuilder.size(); i++) {
			commandsArray[i] = commandsBuilder.get(i);
		}


		System.out.println("BootstrapADE starting " + arch);
		displayArguments(commandsArray);

		// Build process
		ProcessBuilder pb = new ProcessBuilder(commandsArray);
		// Redirect stderr
		pb.redirectErrorStream(true);
		// Execute
		Process architectureProcess = null;
		try {
			architectureProcess = pb.start();
		} catch (IOException e) {
			System.err.println("Error starting architecture process! " + e);

		}
		// Capture and print stdout
		final BufferedReader reader = new BufferedReader(
				new InputStreamReader(architectureProcess.getInputStream()));
		new Thread() {
			public void run() {
				String out;
				while (readProcs) {
					try {
						out = reader.readLine();
						if (verbose && (out != null)) {
							System.out.println(out);
						}
					} catch (IOException ioe) {
						System.err.println("Error reading from proc: " + ioe);
					}
				}
			}
		}.start();
	}

	private static void startSimEnvironment() {
		String[] envCommands = {"java", "-Dcomponent=com.adesim.ADESimEnvironmentComponentImpl",
				"-Djava.rmi.server.hostname=127.0.0.1", 
				"-cp", jarfile, "ade.ADEComponentImpl", 
				"-l", "127.0.0.1", "-r", "127.0.0.1", 
				"-cfg", cfgFile,
				"-g", "Environment", 
				"-view", environmentViewOptions, "-edit", environmentEditOptions};
		
		System.out.println("BootstrapADE starting ADESimEnvironmentComponentImpl");
		displayArguments(envCommands);


		// Build process
		ProcessBuilder pb = new ProcessBuilder(envCommands);
		// Redirect stderr
		pb.redirectErrorStream(true);

		// Execute
		try {
			pb.start();
		} catch (IOException e2) {
			System.err.println("Could not start the simulation environment process");
		}

		// Give the environment some time to start
		try {
			Thread.sleep(PAUSE_DURATION_BETWEEN_STARTUPS);
		} catch (InterruptedException e) {
			System.err.println("Could not sleep for a few seconds after starting ADESimEnvironmentComponent");
		}
	}
	
	
	private static void startSimPioneer() {
		ArrayList<String> commandsBuilder = new ArrayList<String>();
		String[] permanentCommands = {"java", "-Dcomponent=com.adesim.SimPioneerComponentImpl",
				"-Djava.rmi.server.hostname=127.0.0.1", 
				"-cp", jarfile, "ade.ADEComponentImpl", 
				"-l", "127.0.0.1", "-r", "127.0.0.1"};
		for (String each : permanentCommands) {
			commandsBuilder.add(each);
		}
		if (setInitPos) {
			commandsBuilder.add("-initpose");
			commandsBuilder.add(InitX);
			commandsBuilder.add(InitY);
			commandsBuilder.add(InitTheta);
		}
		if (acceleration != null) {
			commandsBuilder.add("-acceleration");
			commandsBuilder.add(acceleration);
		}

		String[] commandsArray = new String[commandsBuilder.size()];
		for (int i = 0; i < commandsBuilder.size(); i++) {
			commandsArray[i] = commandsBuilder.get(i);
		}

		System.out.println("BootstrapADE starting SimPioneerComponentImpl");
		displayArguments(commandsArray);


		// Build process
		ProcessBuilder pb = new ProcessBuilder(commandsArray);
		// Redirect stderr
		pb.redirectErrorStream(true);

		// Execute
		Process simProcess = null;
		try {
			simProcess = pb.start();
		} catch (IOException e2) {
			System.err.println("Could not start the sim pioneer process");
		}
		// Capture and consume stdout
		final BufferedReader reader = new BufferedReader(new InputStreamReader(simProcess.getInputStream()));
		if (testing) {
			simStartTime = System.currentTimeMillis();
		}

			new Thread() {
				BufferedWriter BWout = null;
				
				public void run() {
					if (testing) {
						// Create file
						try {
							BWout = new BufferedWriter(new FileWriter(statsFile, true));
						} catch (IOException e) {
							System.err.println("Could not create a file writer for the assignment results");
						}
					}
					
					while (readProcs) {
						try {
							String out = reader.readLine();
							String writeAndClose = null;

							if (testing) {
								// whatever writes out to terminal -- writes to file as well:
									BWout.write(out);

									if (out.startsWith("Stall!")) {
										writeAndClose = "Stall!  Robot is wrecked, killing simulator!";
									}
									if (((System.currentTimeMillis()-simStartTime) / 1000) > (60 * 60 * 5)) {
										writeAndClose = "Program Timed out after 5 minutes!!!";
									}
							}
							

							// if verbose, write everything to terminal:
							if (verbose && (out != null)) {
								System.out.println(out);
							}

							// and if write and close is not null, write it (if in testing mode), and close (regardless!)
							if (writeAndClose != null) {
								System.out.println(writeAndClose); // write the final statement to the terminal!

								if (testing) {
									BWout.write(writeAndClose);
									BWout.close();
								}

								shutdown();
							}
						} catch (IOException e) {
							System.err.println("IO exception writing to assignment results file:  " + e);
						}
					} // end while
				}
			}.start();


		// Give ADESim some time to start

		try {
			Thread.sleep(PAUSE_DURATION_BETWEEN_STARTUPS);
		} catch (InterruptedException e) {
			System.err.println("Could not sleep for a few seconds after starting ADESimEnvironmentComponent");
		}
	}


	private static void startRegistry() {
		// Registry
		String[] cmd = {"java", "-Dcomponent=ade.ADERegistryImpl",
				"-Djava.net.preferIPv4Stack", "-Djava.rmi.server.hostname=127.0.0.1", 
				"-cp", "." + File.pathSeparator + jarfile, "ade.ADEComponentImpl", 
				"-l", "127.0.0.1", "-o", "127.0.0.1"};
		System.out.println("BootstrapADE starting ADERegistryImpl");
		displayArguments(cmd);

		// Build process
		ProcessBuilder pb = new ProcessBuilder(cmd);
		// Redirect stderr
		pb.redirectErrorStream(true);
		// Execute
		try {
			registryProcess = pb.start();
		} catch (IOException e) {
			System.err.println("Exception starting ADE:\n" + e);
			shutdown();
		}

		// Give Registry some time to start
		try {
			Thread.sleep(PAUSE_DURATION_BETWEEN_STARTUPS);
		} catch (InterruptedException e) {
			System.err.println("Could not sleep for a few seconds after starting registry");
		}
	}

	private static void displayArguments(String[] cmd) {
		for (String each : cmd) {
			System.out.print(each + " ");
		}
		System.out.println();
		System.out.println();
	}

	private static void loadJar() {
		// Need the jarfile for the classpath
		jarfile = ADEComponentImpl.class.getProtectionDomain().getCodeSource().getLocation().getFile();
		// Don't want to worry about Windows path, so just getting the name
		jarfile = (new File(jarfile)).getName();

		// Add hook to kill the servers
		Runtime.getRuntime().addShutdownHook(
				new Thread() {
					public void run() {
						shutdown();
					}
				});
	}

	private static void parseArgs(String[] args) {
		// Look for optional architecture class name
		// and parsing other command line arguments
		if (args.length > 0) {
			arch = args[0];
			// Check for architecture class file
			File archfile = new File(arch + ".class");
			if (!archfile.exists()) {
				System.err.println("Can't find architecture file " + arch + ".class");
				System.exit(-1);
			}
			if (args.length > 1) {
				int argCounter = 1;
				// NOTE THAT YOU MUST MANUALLY INCREMENT THE ARG COUNTER WITHIN EACH "MATCHING" IF
				while (argCounter < args.length) {
					if (args[argCounter].equalsIgnoreCase("-initpose")) {
						setInitPos = true;
						InitX = args[argCounter + 1];
						InitY = args[argCounter + 2];
						InitTheta = args[argCounter + 3];
						argCounter = argCounter + 4;
					} else if (args[argCounter].equalsIgnoreCase("-acceleration")) {
						acceleration = args[argCounter + 1];
						argCounter = argCounter + 2;
					} else if (args[argCounter].equalsIgnoreCase("-cfg")) { // for files packaged in the JAR
						cfgFile = BUILT_IN_CFG_PREFIX + args[argCounter + 1];
						argCounter = argCounter + 2;
					} else if (args[argCounter].equalsIgnoreCase("-cfgEXTERN")) { // for files in system, don't append prefix
						cfgFile = args[argCounter + 1];
						argCounter = argCounter + 2;
					} else if (args[argCounter].equalsIgnoreCase("-test")) {
						testing = true;
						System.out.println("Running in Testing mode to grade assignment");
						argCounter = argCounter + 1;
					} else if (args[argCounter].equalsIgnoreCase("-testFile")) {
						statsFile = "./" + args[argCounter + 1] + ".txt";
						argCounter = argCounter + 2;
					} else if (args[argCounter].equalsIgnoreCase("-nov")) {
						verbose = false;
						System.out.println("no longer verbose");
						argCounter = argCounter + 1;
					} else if (args[argCounter].equalsIgnoreCase("-help")) {
						displayHelpInfoAndExit();
					} else {
						System.out.println("Unrecognized command-line argument \"" + args[argCounter] + 
						"\", adding it to list of external arguments to pass directly to java");
						externalArgs.add(args[argCounter]);
						argCounter = argCounter + 1;
					}
				}
			}
		}   
	}


	private static void displayHelpInfoAndExit() {
		StringBuilder out = new StringBuilder();
		out.append("\nUsage: java -jar toy1.jar [architecture impl] {optional parameters}");
		out.append("\n");
		out.append("\nOptional parameters include:");
		out.append("\n    -help     Shows this information.");
		out.append("\n    -initpose x y theta     Specifies initial position of robot, with theta in RADIANS or \"rand\".");
		out.append("\n    -acceleration     Sets robot's momentum (range 0 to 1).  0.1 = on ice; 1 = immediate stop and go.");
		out.append("\n    -cfg     Specifies config file embedded in the jar, for example \"assignment1.xml\"");
		out.append("\n    -cfgEXTERN     Specifies config file on the file system (external to jar).  Need full path.");
		out.append("\n    -test     Run in testing mode (ie: kill on stall, record to log file");
		out.append("\n    -testFile     Specifies log file to write to if in testing mode (see \"-test\").");
		out.append("\n    -nov     No verbose output (ie: will repress any console output)");
		out.append("\n    {anything else}     Will be passed to the java virtual machine.  For example, -ea to enable assertions.");
		out.append("\n");

		System.out.println(out.toString());
		System.exit(0);
	}

	public static void shutdown() {
		System.out.println("BootstrapADE: killing processes");
		readProcs = false;
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			System.err.println("Could not sleep for a moment while trying to shutdown...");
		}

		registryProcess.destroy(); // no need to destroy the other processes -- destroying the registry will destroy the rest.
	}
}
