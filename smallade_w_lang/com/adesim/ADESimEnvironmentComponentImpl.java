/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 *
 * ADESimEnvironmentComponentImpl.java
 *
 * An ADE simulation environment.
 * 
 * @author Michael Zlatkovsky (based on original ADESim by Paul Schermerhorn, et al)
 * 
 */
package com.adesim;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import utilities.xml.Xml;
import ade.ADEComponentImpl;
import ade.gui.ADEGuiVisualizationSpecs;

import com.adesim.commands.ActorCommand;
import com.adesim.commands.ActorCommandsHolder;
import com.adesim.commands.AddObjectCommand;
import com.adesim.commands.ApplicableFor;
import com.adesim.commands.ChangeInActorShapeCommand;
import com.adesim.commands.CreateNewEnvironmentCommand;
import com.adesim.commands.HistoryHolder;
import com.adesim.commands.RemoveActorCommand;
import com.adesim.commands.RemoveObjectCommand;
import com.adesim.commands.UpdateExistingObjectCommand;
import com.adesim.config.parser.SimConfigParser;
import com.adesim.datastructures.ConfigStartupActorProperties;
import com.adesim.datastructures.NewArrivalsGuestbook;
import com.adesim.datastructures.SimShape;
import com.adesim.datastructures.SimWelcomePackage;
import com.adesim.gui.ADESimMapVis;
import com.adesim.gui.ADESimMapVisualizationType;
import com.adesim.gui.datastructures.RobotVisualizationData;
import com.adesim.gui.datastructures.SimMapVisUpdatePackage;
import com.adesim.objects.Door;
import com.adesim.objects.Door.DoorUpdatingStatus;
import com.adesim.objects.SimContainerEntity;
import com.adesim.objects.SimEntity;
import com.adesim.objects.Wall;
import com.adesim.objects.model.EnvironmentModelOwner;
import com.adesim.objects.model.ObjectMover;
import com.adesim.objects.model.SimModel;
import com.adesim.util.SimUtil;


public class ADESimEnvironmentComponentImpl extends ADEComponentImpl implements ADESimEnvironmentComponent {
	private static final long serialVersionUID = 1L;

	private static boolean useThreads = false;
    private int nextActorToUpdateFirst = 0;	
    private int nextActorToTickFirst = 0;	
	private static final DateFormat TIME_FORMAT = new SimpleDateFormat("mm:ss.SSS");
	
	final int MAX_TIMEOUT = 10000; // max timeout for trying to send an update to an actor.  
	//    if 10 seconds is not enough, NOTHING would be...
	

	public static final double SIM_TIME_RESOLUTION = 10; // 10 = refresh is 10x/sec.  This is different
	//     from loop time, because this will allow the whole simulation to be sped up,
	//     while still (internally) keeping the time resolution the same.

	private static boolean groupUpdates = false; // by default, DON'T group updates, send them off immediately to actors.
	//     Set this to TRUE (via command-line by including "-groupUpdates") when running a massively parallel simulation,
	//     where, efficiency-wise, updates should be grouped together.  Note THAT VISUALIZATION UPDATES *ARE* regardless
	//     grouped by "tick", so this only refers to whether an actor robot will immediately note that the box opened
	//     after he asked for it to get opened, or if it will take a tick (1/10 second by default) before the update
	//     will be broadcast back at him.
	
	private static String startupConfig = "com/adesim/config/eigenmann8.xml"; // command-line mention will override this variable
	private static String viewFlags = null; // command line can specify which views to start off the sim with
	private static String editFlags = null; // command line can specify which views to start off the sim with

	public SimModel model; // public so that subclasses and config parser can access it
	public Queue<ConfigStartupActorProperties> startupActorLocationsAndProperties = 
		new LinkedList<ConfigStartupActorProperties>(); // public so config parser can access it



	private boolean delayedSaveGridmap;
	private String delayedSaveGridmapFilename;
	private double delayedSaveGridmapResolution;

	private int tickCounter = 0;

	private ConcurrentHashMap<String, Object> simActorComponents;

	private UpdateManager updateManager;
	private NewArrivalsGuestbook newArrivals = new NewArrivalsGuestbook();


	public ADESimEnvironmentComponentImpl() throws RemoteException {
		super();

		this.setUpdateLoopTime(this, 1000);

		simActorComponents = new ConcurrentHashMap<String, Object>();

		updateManager = new UpdateManager(this, groupUpdates);
		
		model = new SimModel(new EnvironmentModelOwner() {
			@Override
			public void addCommand(ActorCommand command) {
				updateManager.addCommand(command);
			}
			@Override
			public SimModel getModel() {
				return model;
			}
		});

		new SimConfigParser(this, startupConfig);

		if (delayedSaveGridmap) {
			saveGridmap(delayedSaveGridmapFilename, 
					delayedSaveGridmapResolution);
			delayedSaveGridmap = false;
		}

		this.setUpdateLoopTime(this, 100);
	}

	/** will produce a JPEG/PNG of the simulation environment */
	private void getGridmap(double resolution) {
		// determine image size, given desired resolution and scale of
		// environment 
		Dimension2D worldDim = this.model.worldBounds.getBoundingDimension(); 
		int gm_x = (int)(worldDim.getWidth()*resolution);
		int gm_y = (int)(worldDim.getHeight()*resolution);
		BufferedImage bi = new BufferedImage(gm_x, gm_y,
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D ig2 = bi.createGraphics();
		ig2.setBackground(Color.white);
		ig2.fill(new Rectangle(0, 0, gm_x, gm_y));

		ig2.setPaint(Color.black);
		// paint all walls:
		for (SimEntity simEntity : this.model.worldObjects.getObjects()) {
			if (simEntity instanceof Wall) {
				SimShape wallShape = simEntity.getShape();
				Polygon poly = new Polygon();
				for (Point2D eachWallPt : wallShape.getAllPoints()) {
					Point2D pt = SimUtil.visCoordinatesFromWorldPoint(eachWallPt, this.model.worldBounds, new Dimension(gm_x, gm_y));
					poly.addPoint((int)(pt.getX()), (int)(pt.getY()));
				}
				ig2.fill(poly);
			} // if wall
		} // for each object
	}

	/** will produce a JPEG/PNG of the simulation environment */
	private void saveGridmap(String filename, double resolution) {
		// determine image size, given desired resolution and scale of
		// environment 
		Dimension2D worldDim = this.model.worldBounds.getBoundingDimension(); 
		int gm_x = (int)(worldDim.getWidth()*resolution);
		int gm_y = (int)(worldDim.getHeight()*resolution);
		BufferedImage bi = new BufferedImage(gm_x, gm_y,
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D ig2 = bi.createGraphics();
		ig2.setBackground(Color.white);
		ig2.fill(new Rectangle(0, 0, gm_x, gm_y));

		ig2.setPaint(Color.black);
		// paint all walls:
		for (SimEntity simEntity : this.model.worldObjects.getObjects()) {
			if (simEntity instanceof Wall) {
				SimShape wallShape = simEntity.getShape();
				Polygon poly = new Polygon();
				for (Point2D eachWallPt : wallShape.getAllPoints()) {
					Point2D pt = SimUtil.visCoordinatesFromWorldPoint(eachWallPt, this.model.worldBounds, new Dimension(gm_x, gm_y));
					poly.addPoint((int)(pt.getX()), (int)(pt.getY()));
				}
				ig2.fill(poly);
			} // if wall
		} // for each object

		String[] ext = filename.split("\\.");
		if (ext.length == 2) {
			if (ext[1].equalsIgnoreCase("jpg"))
				ext[1] = "JPEG";
			try {
				ImageIO.write(bi, ext[1].toUpperCase(), new File(filename));
			} catch (IOException ie) {
				ie.printStackTrace();
			}
		} else {
			System.out.println("Missing extension, using PNG");
			try {
				ImageIO.write(bi, "PNG", new File(filename));
			} catch (IOException ie) {
				ie.printStackTrace();
			}
		}
	}



	// *********************************************************************
	// abstract/interface methods that need to be implemented/overridden
	// *********************************************************************
	/**
	 * This method will be activated whenever a client calls the
	 * requestConnection(uid) method. Any connection-specific initialization
	 * should be included here, either general or user-specific.
	 */
	protected void clientConnectReact(String user) {
		System.out.println(myID + ": got connection from " + user + "!");
	}


	/**
	 * This method will be activated whenever a client that has called the
	 * requestConnection(uid) method fails to update (meaning that the
	 * heartbeat signal has not been received by the reaper), allowing both
	 * general and user specific reactions to lost connections. If it returns
	 * true, the client's connection is removed.
	 */
	protected boolean clientDownReact(String user) {
		System.out.println(myID + ": lost connection with " + user + "!");

		// If the lost connection was from a sim, remove from shapes and connected servers!
		String actorName = getNameFromID(user);
		if (this.simActorComponents.containsKey(actorName)) {
			this.simActorComponents.remove(actorName);
			this.updateManager.addCommand(new RemoveActorCommand(actorName));
			this.model.removeOtherRobotRegistration(actorName);
			System.out.println("Removing " + actorName + " from list of concurrent actor robots");
                        releaseClient(user);
		}

		return false;
	}
	

	/**
	 * This method will be activated whenever the heartbeat returns a
	 * remote exception (i.e., the server this is sending a
	 * heartbeat to has failed). 
	 */
	protected void componentDownReact(String serverkey, String[][] constraints) {
		String s = constraints[0][1];
                String key = null;

		System.out.println(this.getComponentName() + ": reacting to down " + s + "...");
                /*
                for (int i = 0; i < constraints.length; i++) {
                    if (constraints[i][0].equals("name")) {
                        key = constraints[i][1];
                        break;
                    }
                }
		if (key != null && this.simActorComponents.containsKey(key)) {
			this.updateManager.addCommand(new RemoveActorCommand(key));
			this.model.removeOtherRobotRegistration(key);
			System.out.println("Removing " + key + " from list of concurrent actor robots");
		}
                */
		return;
	}

	/** This method will be activated whenever the heartbeat reconnects
	 * to a client (e.g., the server this is sending a heartbeat to has
	 * failed and then recovered). <b>Note:</b> the pseudo-reference will
	 * not be set until <b>after</b> this method is executed. To perform
	 * operations on the newly (re)acquired reference, you must use the
	 * <tt>ref</tt> parameter object.
	 * @param s the ID of the {@link ade.ADEComponent ADEComponent} that connected
	 * @param ref the pseudo-reference for the requested server */
	protected void componentConnectReact(String serverkey, Object ref, String[][] constraints) {
		// do nothing.
	}

	/**
	 * Adds additional local checks for credentials before allowing a shutdown
	 * must return "false" if shutdown is denied, true if permitted
	 */
	protected boolean localrequestShutdown(Object credentials) {
		return false;
	}

	/**
	 * Implements the local shutdown mechanism that derived classes need to
	 * implement to cleanly shutdown
	 */
	protected void localshutdown() {
	}

	/**
	 * Provide additional information for usage...
	 */
	protected String additionalUsageInfo() {
		StringBuilder sb = new StringBuilder();
		sb.append("Component-specific options:\n\n");
		sb.append("  -cfg f             <use environment config file f>\n");
		sb.append("  -useThreads t/f    <Whether threads are used for issue actor update calls (DEFAULT False)>\n");
		sb.append("  -groupUpdates      <by DEFAULT, updates (box-opening, etc) are dispatched IMMEDIATELY to all actors, so they\n" +
				  "                      don't have to wait for the update to come through before proceeding with, say, taking\n" + 
				  "                      objects out of the said box.  However, while this update mode is convenient, it is\n" + 
				  "                      not particularly efficient when manu updates are being sent at once.\n" +
				  "                      Thus, for massively-parallel simulations, add the -groupUpdates flag to the\n" + 
				  "                      command-line, and updates will be dispatched in groups at the end of each \"tick\".");
		sb.append("  -save-gridmap f r  <save walls as gridmap file at resolution (cells/m)>\n");
		sb.append("  -view flags        <sets up the \"preset\" view options for the simulator environement.\n" + 
				  "                      flag is a string of digits, one character per checkbox in the View menu (\"Show tooltips\",\n" +
				  "                      \"Show laser lines\", etc).  Each digit is in the range 0-3, with \n" + 
				  "                      0 corresponding to OFF and *NOT* VISIBLE (hence *NOT* changeable from the GUI),\n" + 
				  "                      1 = ON and *NOT* VISIBLE, 2 = OFF and VISIBLE (can be changed from the GUI),\n" +
				  "                      and 3 = ON and VISIBLE");
		sb.append("  -edit flags        <sets up the \"preset\" edit permission options for the simulator environement.\n" + 
  		          "                      flag is a string of digits, one character per checkbox in the Edit menu,\n" + 
  		          "                      following the same specifications as the -view flag description above");
		return sb.toString();
	}

	/**
	 * Parse additional command-line arguments
	 * @return "true" if parse is successful, "false" otherwise 
	 */
	protected boolean parseadditionalargs(String[] args) {
		boolean found = false;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-cfg") ||
                            args[i].equalsIgnoreCase("-conf") ||
                            args[i].equalsIgnoreCase("-config")) {
				startupConfig = args[++i];
				found = true;
			} else if (args[i].equalsIgnoreCase("-useThreads")) {
				String value = args[++i];
				if (value.equalsIgnoreCase("true") || value.equals("1") || value.equalsIgnoreCase("t")) {
					useThreads = true;
				} else {
					useThreads = false;
				}
				found = true;
			} else if (args[i].equalsIgnoreCase("-groupUpdates")) {
				groupUpdates = true;
				found = true;
			} else if (args[i].equalsIgnoreCase("-save-gridmap")) {
				String filename;
				double resolution;
				try {
					filename = args[i + 1];
					i++;
					resolution = Double.parseDouble(args[i + 1]);
					i++;
					delayedSaveGridmap = true;
					delayedSaveGridmapFilename = filename;
					delayedSaveGridmapResolution = resolution;
				} catch (NumberFormatException nfe) {
					System.err.println(this.getComponentName() + ": save-gridmap " + args[i + 1]);
					System.err.println(this.getComponentName() + ": " + nfe);
					System.err.println(this.getComponentName() + ": ignoring");
				} catch (ArrayIndexOutOfBoundsException aobe) {
					System.err.println(this.getComponentName() + ": save-gridmap <file> <resolution>");
					System.err.println(this.getComponentName() + ": ignoring");
				}
				found = true;
			} else if (args[i].equalsIgnoreCase("-view")) {
				viewFlags = args[++i];
				found = true; 
			} else if (args[i].equalsIgnoreCase("-edit")) {
				editFlags = args[++i];
				found = true; 
			} else {
				System.out.println("Unrecognized argument: " + args[i]);
				return false;  // return on any unrecognized args
			}
		}
		return found;
	}

	private String getComponentName() {
		try {
			return this.getID();
		} catch (RemoteException e) {
			return "unknown server name!";
		}
	}

	@Override
	protected boolean localServicesReady() {
		return true;
	}

	@Override
	public ArrayList<RobotVisualizationData> getRobotsVisData(boolean drawLaserLines, 
		boolean drawActivatedObstacleSensors, boolean drawPerceivedObjects) throws RemoteException {
	
		ArrayList<RobotVisualizationData> newVisData = new ArrayList<RobotVisualizationData>();
		for (Map.Entry<String, Object> eachEntry : simActorComponents.entrySet()) {
			Object ref = eachEntry.getValue();
			try {
				if ((Boolean) call(ref, "servicesReady", this.getID())) {
					RobotVisualizationData tempVisData = (RobotVisualizationData) call(ref, "getRobotVisData", 
							drawLaserLines, drawActivatedObstacleSensors, drawPerceivedObjects);
					if (tempVisData != null) {
						newVisData.add(tempVisData);
					}
				}
			} catch (Exception e) {
				System.out.println("Could not obtain robot visualization data for " + eachEntry.getKey() + 
						"\ndue to " + e);
			}
		}
			
		return newVisData;
	}

	private String getMapVisTitle() throws RemoteException {
		String robotCount = model.otherRobotShapes.size() + " " + "robot" + 
		( (model.otherRobotShapes.size() == 1) ? "" : "s" );

		return "ADESim Full Environment View (for " + this.getNameFromID(getName()) + 
		", " + getElapsedTimeForTickCounter(tickCounter) + ")" + "  " + 
		"[" + robotCount + "]";
	}


	@Override
	public void moveRobotRelativeWithImmediateRefresh(Point2D.Double offset,
			String simName) throws RemoteException {
		try {
			call(simActorComponents.get(simName), "moveRobotRelativeWithImmediateRefresh", offset, simName);
			// yes, passing simName is slightly redundant, but it DOES prevent making another method...
		} catch (Exception e) {
			System.err.println("Could not move robot " + simName);
		}
	}

	@Override
	public void allowRobotMotion(String simName, boolean allow)
	throws RemoteException {
		try {
			call(simActorComponents.get(simName), "allowRobotMotion", simName, allow);
			// yes, passing simName is slightly redundant, but it DOES prevent making another method...
		} catch (Exception e) {
			System.err.println("Could not set allowRobotMotion to " + allow + " on " + simName);
		}
	}


	protected void updateComponent() {
				// first, process any new arrivals:
		ArrayList<String> accumulatedGuests = newArrivals.offloadAccumulatedGuests();
		for (String eachNewArrival : accumulatedGuests) {
			registerActor(eachNewArrival);
		}

		updateComponentHelper();
	}

	int updateCount=0;
	private void updateComponentHelper() {
		final ActorCommandsHolder accumulatedCommandHolder = 
			this.updateManager.offloadAccumulatedCommandsIfDispatchingUpdatesInGroup(tickCounter);
		
		// update whatever is necessary in current model
		//    This is how doors get updated, for instance.
		model.tick(tickCounter, false); // false = allow robot motion.  since no robot, obviously no motion.

		//for debugging: might be useful to print out commands that are being transmitted...
		//    of course, those commands only exist if updateManager is set to group updates (groupUpdates = true),
		//    otherwise this is guaranteed (or should be, anyway) to be empty.
		//for (ActorCommand each : accumulatedCommandHolder.getGeneralCommands()) {
		//    System.out.println(each.toString());
		//}
		if (useThreads){
			ArrayList<Thread> actorThreads = new ArrayList<Thread>();
			for (final Entry<String, Object> eachActor : simActorComponents.entrySet()) {
				Thread individualActorTickThread = new Thread() {
					@Override
					public void run() {
						try {
							// note that the general commands and actor specific commands might well be empty
							//   arrays, ESPECIALLY if update-dispatching is immediate, in which case they 
							//   certainly SHOULD be empty.
							call(MAX_TIMEOUT, eachActor.getValue(), "tick", tickCounter, 
									accumulatedCommandHolder.getGeneralCommands(),
									accumulatedCommandHolder.getActorSpecificCommands(eachActor.getKey()));
						} catch (Exception e) {
							System.err.println("Environment could not \"tick\" actor " + eachActor.getKey() + " due to " + e + 
									".  Skipping the actor, this could lead to some inconsistencies... ");
							//System.out.println("Printing stack trace:");
							//e.printStackTrace();
						}		
					}
				};
				actorThreads.add(individualActorTickThread);
				individualActorTickThread.start();
			}

			// wait for all threads to complete
			for (Thread eachActorThread : actorThreads) {
				try {
					eachActorThread.join();
				} catch (InterruptedException e) {
					System.err.println("Could not perform join on thread \"" + eachActorThread.getName() + "\".");
				}
			}
		} else {
			if (nextActorToTickFirst >= simActorComponents.entrySet().size()) nextActorToTickFirst = 0;
			//DEBUG: System.out.println("NextActorToTickFirst:" + nextActorToTickFirst );
			int actorIndex = 0;
			for (Entry<String, Object> eachActor : simActorComponents.entrySet()) {
					if (nextActorToTickFirst <= actorIndex) { 
						// DEBUG: System.out.println("\t\tUpdateing Actor:" + actorIndex + " max:" + (simActorComponents.entrySet().size() - 1) + " in RR1" );
						try {
							// note that the general commands and actor specific commands might well be empty
							//   arrays, ESPECIALLY if update-dispatching is immediate, in which case they 
							//   certainly SHOULD be empty.
							call(MAX_TIMEOUT, eachActor.getValue(), "tick", tickCounter, 
									accumulatedCommandHolder.getGeneralCommands(),
									accumulatedCommandHolder.getActorSpecificCommands(eachActor.getKey()));
						} catch (Exception e) {
							System.err.println("Environment could not \"tick\" actor " + eachActor.getKey() + " due to " + e + 
									".  Skipping the actor, this could lead to some inconsistencies... ");
							//System.out.println("Printing stack trace:");
							//e.printStackTrace();
						}
					}	
				actorIndex++;
			}	
			actorIndex = 0;
			for (Entry<String, Object> eachActor : simActorComponents.entrySet()) {
					if (nextActorToTickFirst > actorIndex) { 
						// DEBUG: System.out.println("\t\tUpdateing Actor:" + actorIndex + " max:" + (simActorComponents.entrySet().size() - 1) + " in RR2" );
						try {
							// note that the general commands and actor specific commands might well be empty
							//   arrays, ESPECIALLY if update-dispatching is immediate, in which case they 
							//   certainly SHOULD be empty.
							call(MAX_TIMEOUT, eachActor.getValue(), "tick", tickCounter, 
									accumulatedCommandHolder.getGeneralCommands(),
									accumulatedCommandHolder.getActorSpecificCommands(eachActor.getKey()));
						} catch (Exception e) {
							System.err.println("Environment could not \"tick\" actor " + eachActor.getKey() + " due to " + e + 
									".  Skipping the actor, this could lead to some inconsistencies... ");
							//System.out.println("Printing stack trace:");
							//e.printStackTrace();
						}
					}	
				actorIndex++;
			}
		
			nextActorToTickFirst++;
		}

		tickCounter = tickCounter + 1;
	}
	
	@Override
	protected void updateFromLog(String logEntry) {
		// do nothing
	}


	@Override
	public String getCurrentStateConfigXML() throws RemoteException {
		Xml configXml = new Xml("config");

		// don't worry abound boundedness, already have walls for the bounds.  So set as FALSE.
		Xml worldXml = configXml.addChild(new Xml("world"));
		worldXml.addAttribute("bounded", Boolean.toString(false));

		// add the world shape.
		worldXml.addChildren(this.model.worldBounds.generateXML());


		// next up:  init robot positions:
		if (simActorComponents.size() > 0) {
			Xml initRobotPositionsXml = configXml.addChild(new Xml("init-robot-positions"));

			// just store wherever robots are now, and whatever they contain:
			for (final Entry<String, Object> eachActor : simActorComponents.entrySet()) {
				try {
					initRobotPositionsXml.addChild(
							(Xml) call(eachActor.getValue(), "generateRobotPropertiesXML"));
				} catch (Exception e) {
					System.err.println("Environment could not call generateRobotPropertiesXML " + 
							"on actor " + eachActor.getKey() + " due to " + e);
					e.printStackTrace();
				}
			}
		}


		// next up still:  objects.  easy:
		configXml.addChildren(this.model.worldObjects.generateXMLs());


		return configXml.toString();
	}


	public static String getElapsedTimeForTickCounter(int counter) {
		long milliseconds = (long) (counter * 1000 / SIM_TIME_RESOLUTION);
		Date millisecondsDate = new Date(milliseconds);
		return TIME_FORMAT.format(millisecondsDate);
	}

	@Override
	public String getViewPresetFlags() throws RemoteException {
		return viewFlags;
	}
	@Override
	public String getEditPresetFlags() throws RemoteException {
		return editFlags;
	}
    
        @Override
	public ADEGuiVisualizationSpecs getVisualizationSpecs()	throws RemoteException {
	ADEGuiVisualizationSpecs specs = super.getVisualizationSpecs();
	specs.add("Environment", ADESimMapVis.class, 
		  ADESimMapVisualizationType.FULL_ENVIRONMENT);
	return specs;
	}


	@Override
	public void updateDoorStatusOnMap(UUID doorGUID,
			DoorUpdatingStatus status) throws RemoteException {
		Door door = (Door) this.model.worldObjects.getObjectFromGUID(doorGUID);
		door.updateStatus(status);
		// don't need to update actors about it, because door swings ONLY in the environment,
		//      and updates actors of changes in its shape.
	}

	private void registerActor(String actorName) {
		ConfigStartupActorProperties possibleStartupLocation = startupActorLocationsAndProperties.poll();
		// will return an element at the head of the queue, or null if empty.
		//     either is perfectly fine!

		SimWelcomePackage welcomePackage = new SimWelcomePackage(
				this.tickCounter,
				this.model.worldBounds,
				this.model.worldObjects, 
				this.model.otherRobotShapes,
				possibleStartupLocation);

		safeCallActor(actorName, "joinSimulation", new Object[] {welcomePackage}, 
				"Could not send WelcomePackage to newly arrived actor, this will not end well!", null);
	}

	@Override
	public void keyPressed(int keyCode, String simName) throws RemoteException {
		safeCallActor(simName, "keyPressed", new Object[] {keyCode}, 
				"Could not call keyPressed event for actor " + simName, false);
	}
	@Override
	public void keyReleased(int keyCode, String simName) throws RemoteException {
		safeCallActor(simName, "keyReleased", new Object[] {keyCode}, 
				"Could not call keyReleased event for actor " + simName, false);
	}
	
	
	@Override
	public String getHelpText(String simName) throws RemoteException {
		return (String) safeCallActor(simName, "getHelpText", new Object[] {}, 
				"Could not call getHelpText event for actor " + simName, 
				"No help available");
	}
	
	
	private Object safeCallActor(String simName, String methodName,
			Object[] args, String errorString, Object returnValueOnError) {
		try {
			return call(simActorComponents.get(simName), methodName, args);
		} catch (Exception e) {
			System.err.println(errorString + "  " + simName);
			e.printStackTrace();
			return returnValueOnError;
		}
	}

	@Override
	public void notifyOfChangeInActorShape(String actorName,
			SimShape newRobotShape) throws RemoteException {
		this.model.updateOtherRobotLocation(actorName, newRobotShape);
		this.updateManager.addCommand(new ChangeInActorShapeCommand(actorName, newRobotShape));
	}

	@Override
	public ConcurrentHashMap<String, SimShape> getRobotShapes() throws RemoteException {
		return this.model.otherRobotShapes;
	}

	@Override
	public void requestToJoinSimulation(String serverFullID) throws RemoteException {
		String type = getTypeFromID(serverFullID);
		String name = getNameFromID(serverFullID);
		Object ref = getClient(type, name);
		synchronized (simActorComponents) {
			newArrivals.add(name);
			simActorComponents.put(name, ref);	
		}
	}
	
	@Override
	public boolean requestRemoveObject(UUID objectID) throws RemoteException {
		return (removeObjectCommonHelper(objectID, "Object already removed!") != null);
	}

	/** common helper for removing objects -- used both by actual remove request, and request to 
	 * pick up an object (since that still involves removing it form the environment).
	 * Also (automatically) removes any contained objects, since those conveniently belong to the 
	 * deleted container, and are thus referenced within it rather than the model).
	 * @param objectID
	 * @param errorMessage in case the object has already been removed.
	 * @return the object that was removed.
	 */
	private SimEntity removeObjectCommonHelper(UUID objectID, String errorMessaage) {
		SimEntity objectToRemove = model.getObjectFromGUID(objectID);
		if (objectToRemove == null) {
			System.out.println(errorMessaage);
			return null;
		}

		UUID holderID = objectToRemove.getContainingObjectID();
		if (holderID == null) {
			model.worldObjects.remove(objectToRemove);
			this.updateManager.addCommand(new RemoveObjectCommand(objectID, new ApplicableFor.ActorEnvironment()));
		} else {
			SimEntity holderEntity = model.getObjectFromGUID(holderID);
			SimContainerEntity holderContainer = (SimContainerEntity) holderEntity;
			holderContainer.getObjectsHolder().remove(objectToRemove);
			this.updateManager.addCommand(new RemoveObjectCommand(objectID, new ApplicableFor.Container(holderEntity.getGUID())));
		}


		return objectToRemove;
	}

	@Override
	public boolean requestPickUpObject(UUID objectID, String actorName) throws RemoteException {
		SimEntity requestedObject = removeObjectCommonHelper(objectID, "Object already picked up!");
		if (requestedObject != null) {
			this.updateManager.addCommand(actorName, new AddObjectCommand(requestedObject, new ApplicableFor.ActorPersonalStash())); 
			//     pass it the SIM ENTITY rather than the ID, because the object might already have been removed!
			//     false indicates that add NOT to environment but to actor's personal stash.
		}
		return (requestedObject != null);
	}

	@Override
	public void executeRobotPopupAction(String simName, String methodName,
			Object[] args) throws RemoteException {
		safeCallActor(simName, methodName, args, "Could not pass on the request \"" + methodName +
				"\" on actor \"" + simName + "\".", null);
	}

	@Override
	public boolean requestPutDownObject(SimEntity object, String actorName) throws RemoteException {
		// there's actually not anything to verify (unless we want to later ensure that we don't drop
		//     things on top of each other...).  Just put it down:
		model.worldObjects.add(object);
		// tell all actors to add the item to their cached environment:
		this.updateManager.addCommand(new AddObjectCommand(object, new ApplicableFor.ActorEnvironment()));
		// and remove it from the one "actor" who provided the item:
		this.updateManager.addCommand(actorName, new RemoveObjectCommand(object.getGUID(), new ApplicableFor.ActorPersonalStash()));
		return true; // couldn't have gone wrong.  easy!
	}

	@Override
	public boolean requestDoorAction(UUID doorID, DoorUpdatingStatus doorStatus) throws RemoteException {
		// just set the door to opening, and it will, in turn, take care of broadcasting to everyone.
		Door door = (Door) this.model.worldObjects.getObjectFromGUID(doorID);
		door.updateStatus(doorStatus);
		return true; // nothing to have gone wrong in "asking" door to open.  whether it gets jammed later is another story.
	}
	
	@Override
	public boolean requestPushObject(UUID objectID, Point2D offset, String robotName) throws RemoteException {
		SimEntity entity = model.getObjectFromGUID(objectID);
		if (entity == null) {
			System.out.println("Could not find object to push!");
			return false;
		}
		
		ObjectMover.translate(entity, offset.getX(), offset.getY(), 0);
		// and add command to have others open update the object
		this.updateManager.addCommand(new UpdateExistingObjectCommand(
				entity, new ApplicableFor.ActorEnvironment()));
		return true;
	}

	@Override
	public boolean requestPutObjectIntoContainer(SimEntity object, UUID containerID, 
			String actorName) throws RemoteException {
		SimEntity containerSimEntity = model.getObjectFromGUID(containerID); 
		if (containerSimEntity == null) {
			System.err.println("Could not find container " + containerID);
			return false;
		}

		if (!((SimContainerEntity) containerSimEntity).isOpen()) {
			System.out.println("Container is closed!");
			return false;
		}


		SimContainerEntity container = (SimContainerEntity) containerSimEntity;
		// add locally:
		// Note THAT ADDING -- LOCALLY OR NOT -- WILL MOVE OBJECT TO CENTER OF BOX
		container.add(object); 

		// tell all actors to add the item to their cached environment containers:
		this.updateManager.addCommand(new AddObjectCommand(object, new ApplicableFor.Container(containerSimEntity.getGUID())));

		// and remove it from the one "actor" who provided the item:
		this.updateManager.addCommand(actorName, 
				new RemoveObjectCommand(object.getGUID(), new ApplicableFor.ActorPersonalStash()));
		return true;
	}

	@Override
	public boolean requestGetObjectFromContainer(UUID objectID, UUID containerID,
			String actorName) throws RemoteException {
		SimEntity containerSimEntity = model.getObjectFromGUID(containerID); 
		if (containerSimEntity == null) {
			System.err.println("Could not find container " + containerID);
			return false;
		}

		if (!((SimContainerEntity) containerSimEntity).isOpen()) {
			System.out.println("Container is closed!");
			return false;
		}

		SimEntity requestedObject = ((SimContainerEntity) containerSimEntity)
		.getObjectsHolder().getObjectFromGUID(objectID);
		if (requestedObject == null) {
			System.out.println("Could not find object within the container, maybe it's already been taken!");
			return false;
		}

		// remove from container:

		// first locally:
		((SimContainerEntity) containerSimEntity).getObjectsHolder().remove(requestedObject);

		// also from ALL actor local copies of container
		this.updateManager.addCommand(new RemoveObjectCommand(
				requestedObject.getGUID(), new ApplicableFor.Container(containerSimEntity.getGUID())));


		// and add command to add object to PARTICULAR actor robot:
		this.updateManager.addCommand(actorName, new AddObjectCommand(
				requestedObject, new ApplicableFor.ActorPersonalStash()));
		return true;
	}

	@Override
	public boolean requestSetContainerStatus(UUID containerID, boolean open) throws RemoteException {
		SimEntity containerSimEntity = model.getObjectFromGUID(containerID);

		// open locally:
		((SimContainerEntity) containerSimEntity).setOpen(open);

		// and add command to have others open update the object
		this.updateManager.addCommand(new UpdateExistingObjectCommand(
				containerSimEntity, new ApplicableFor.ActorEnvironment()));

		return true; // nothing to have gone wrong, call succeeds by default!
	}

	@Override
	public void updateBoxStatusOnMap(UUID boxGUID, boolean open) throws RemoteException {
		requestSetContainerStatus(boxGUID, open); // re-use an existing method :-)
		//   still need to keep updateBoxStatusOnMap because it is a "common" method in the ADESimCommonEnvironmentActorInterface,
		//     whereas the requestSetContainerStatus is local to the environment only.  It is also a more user-friendly name...
	}

	
	@Override
	public void updateObjectShapeOnMap(UUID objectID, SimShape newShape)
			throws RemoteException {
		SimEntity entity = model.getObjectFromGUID(objectID);
		if (entity == null) {
			System.out.println("Could not find object that needed updating!");
			return;
		}
		
		entity.setShape(newShape);

		// and add command to have others open update the object
		this.updateManager.addCommand(new UpdateExistingObjectCommand(
				entity, new ApplicableFor.ActorEnvironment()));
	}
	
	@Override
	public void updateObjectNameOnMap(UUID objectID, String newName)
			throws RemoteException {
		SimEntity entity = model.getObjectFromGUID(objectID);
		if (entity == null) {
			System.out.println("Could not find object that needed updating!");
			return;
		}
		
		entity.setName(newName);

		// and add command to have others open update the object
		this.updateManager.addCommand(new UpdateExistingObjectCommand(
				entity, new ApplicableFor.ActorEnvironment()));
	}
	

	@Override
	public void updateObjectParentOnMap(UUID objectID, UUID[] newContainerID) throws RemoteException {
		SimEntity entity = model.getObjectFromGUID(objectID);

		UUID realNewContainerID = null;
		if (newContainerID.length > 0) {
			realNewContainerID = newContainerID[0];
		}

		// System.out.println("Object is in " + entity.getContainingObjectID() + ";   Want to be in " + realNewContainerID);
		// if already is where wants to be, no-op
		if (SimUtil.IDsAreEqual(entity.getContainingObjectID(), realNewContainerID)) {
			return;
		}

		// first remove
		removeObjectCommonHelper(objectID, "Could not remove object from its whereabouts in the model");

		// now add.  
		if (realNewContainerID == null) { // if adding straight to environment:
			// locally:
			model.worldObjects.add(entity);
			// command for others:
			this.updateManager.addCommand(new AddObjectCommand(entity, new ApplicableFor.ActorEnvironment()));
		} else {
			SimEntity containerEntity = model.getObjectFromGUID(realNewContainerID);
			// locally:
			((SimContainerEntity)containerEntity).getObjectsHolder().add(entity);
			// command for others:
			this.updateManager.addCommand(new AddObjectCommand(entity, new ApplicableFor.Container(realNewContainerID)));
		}
	}

	@Override
	public boolean removeObjectFromMap(UUID objectID) throws RemoteException {
		SimEntity removedObject = removeObjectCommonHelper(objectID, "Could not remove object!");
		return (removedObject != null);
	}

	@Override
	public void addObjectToMap(SimEntity object) throws RemoteException {
		// locally:
		model.worldObjects.add(object);
		// command for others:
		this.updateManager.addCommand(new AddObjectCommand(object, new ApplicableFor.ActorEnvironment()));
	}

	
	
	@Override
	public void createNewEnvironment(SimShape worldBounds, boolean bounded) throws RemoteException {
		// locally:
		model.createNewEnvironment(worldBounds, bounded);
		// command for others:
		this.updateManager.addCommand(new CreateNewEnvironmentCommand(worldBounds, bounded));
	}
	
	@Override
	public SimMapVisUpdatePackage getSimMapVisUpdatePackage(
			int lastUpdateTickCount,
			boolean drawLaserLines, boolean drawActivatedObstacleSensors,
			boolean drawPerceivedObjects) throws RemoteException {

		// first prepare the robot vis data.  Note that it is NOT cached, because different
		//     visualizations will probably request the data differently (one might want lasers, 
		//     the other won't)
		ArrayList<RobotVisualizationData> newVisData = getRobotsVisData(
				drawLaserLines, drawActivatedObstacleSensors, drawPerceivedObjects);


		// history needs to be always re-generated, since it depends on the caller (how far)
		//    generating it below ("historyHolder.getUpdateHistory(lastUpdateTickCount)")
		HistoryHolder.UpdateData updateHistory = this.updateManager.getUpdateHistory(lastUpdateTickCount);
		
		if (updateHistory == null) {
			return new SimMapVisUpdatePackage(newVisData, 
						this.tickCounter, getMapVisTitle(),
						model.worldBounds, model.worldObjects, model.otherRobotShapes);
		} else {
			return new SimMapVisUpdatePackage(newVisData, 
						updateHistory.tickCount, getMapVisTitle(),
						updateHistory.commands);
			// note, passing it the history holder's tickCount, in case the current one
			//     has already changed.
		}
	}
	

	/** method called by update manager if want to send immediate update to all actors */
	public void sendImmediateUpdate(final ActorCommand command) {
		if (useThreads){
			ArrayList<Thread> actorThreads = new ArrayList<Thread>();
			for (final Entry<String, Object> eachActor : simActorComponents.entrySet()) {
				Thread individualActorTickThread = new Thread() {
					@Override
					public void run() {
						try {
							call(MAX_TIMEOUT, eachActor.getValue(), "receiveImmediateUpdate", command, false);
							// false = not actor specific.  that much is obvious anyway, since sending to all actors.
							//    the reason the actor needs to know is for the sake of a visualization's HistoryHolder
							//    since false (not actor specific), knows to add it to history.
						} catch (Exception e) {
							System.err.println("Environment could send an immediate update to actor " 
									+ eachActor.getKey() + " due to " + e + 
									".  Skipping the actor, this could lead to some inconsistencies... ");
							//System.out.println("Printing stack trace:");
							//e.printStackTrace();
						}
					}
				};
				actorThreads.add(individualActorTickThread);
				individualActorTickThread.start();
			}

			// wait for all threads to complete
			for (Thread eachActorThread : actorThreads) {
				try {
					eachActorThread.join();
				} catch (InterruptedException e) {
					System.err.println("Could not perform join on thread \"" + eachActorThread.getName() + "\".");
				}
			}
		} else {
			if (nextActorToUpdateFirst >= simActorComponents.entrySet().size()) nextActorToUpdateFirst = 0;
			int actorIndex = 0;
			for (Entry<String, Object> eachActor : simActorComponents.entrySet()) {
					if (nextActorToUpdateFirst <= actorIndex) { 
							try {
								call(MAX_TIMEOUT, eachActor.getValue(), "receiveImmediateUpdate", command, false);
								// false = not actor specific.  that much is obvious anyway, since sending to all actors.
								//    the reason the actor needs to know is for the sake of a visualization's HistoryHolder
								//    since false (not actor specific), knows to add it to history.
							} catch (Exception e) {
								System.err.println("Environment could send an immediate update to actor " 
										+ eachActor.getKey() + " due to " + e + 
										".  Skipping the actor, this could lead to some inconsistencies... ");
								//System.out.println("Printing stack trace:");
								//e.printStackTrace();
							}
					}
					actorIndex++;
			}
		 	actorIndex = 0;
			for (Entry<String, Object> eachActor : simActorComponents.entrySet()) {
				if (nextActorToUpdateFirst > actorIndex) { 
							try {
								call(MAX_TIMEOUT, eachActor.getValue(), "receiveImmediateUpdate", command, false);
								// false = not actor specific.  that much is obvious anyway, since sending to all actors.
								//    the reason the actor needs to know is for the sake of a visualization's HistoryHolder
								//    since false (not actor specific), knows to add it to history.
							} catch (Exception e) {
								System.err.println("Environment could send an immediate update to actor " 
										+ eachActor.getKey() + " due to " + e + 
										".  Skipping the actor, this could lead to some inconsistencies... ");
								//System.out.println("Printing stack trace:");
								//e.printStackTrace();
							}
					}
					actorIndex++;
			}
						
			nextActorToUpdateFirst++;
		}	
	}
	/** method called by update manager if want to send immediate update to a particular actor */
	public void sendImmediateUpdate(String actorName, ActorCommand command) {
		try {
			call(MAX_TIMEOUT, simActorComponents.get(actorName), "receiveImmediateUpdate", command, true);
			// true = sending to specific actor.  this is obviously true in this method, as that's
			//    the whole point of this method compared to the non-actor-name-specified one.
			//    the reason the actor needs to know is for the sake of a visualization's HistoryHolder
		} catch (Exception e) {
			System.err.println("Environment could send an immediate update to actor " 
					+ actorName + " due to " + e + 
					".  Skipping the actor, this could lead to some inconsistencies... ");
			//System.out.println("Printing stack trace:");
			//e.printStackTrace();
		}		
	}

}
