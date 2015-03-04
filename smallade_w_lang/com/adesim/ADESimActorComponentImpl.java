/**
 * ADE 1.0
 * Copyright 1997-2010 HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@indiana.edu
 */
package com.adesim;

import ade.ADEComponentImpl;
import ade.ADEException;
import ade.gui.ADEGuiVisualizationSpecs;
import com.ADEPercept;
import com.ActionStatus;
import com.Predicate;
import com.Variable;
import com.adesim.commands.ActorCommand;
import com.adesim.commands.HistoryHolder;
import com.adesim.datastructures.Point3D;
import com.adesim.datastructures.SimShape;
import com.adesim.datastructures.SimWelcomePackage;
import com.adesim.gui.ADESimMapVis;
import com.adesim.gui.ADESimMapVisualizationType;
import com.adesim.gui.datastructures.RobotVisualizationData;
import com.adesim.gui.datastructures.Sim3DVisUpdatePackage;
import com.adesim.gui.datastructures.SimMapVisUpdatePackage;
import com.adesim.objects.*;
import com.adesim.objects.Door.DoorUpdatingStatus;
import com.adesim.objects.model.ActorModelOwner;
import com.adesim.objects.model.ObjectMover;
import com.adesim.objects.model.SimModel;
import com.adesim.robot.CameraData;
import com.adesim.robot.SimAbstractRobot;
import com.adesim.robot.SimLocationSpecifier;
import com.adesim.robot.interfaces.LaserCarrying;
import com.adesim.robot.laser.AbstractLaser;
import com.google.common.base.Optional;
import com.vision.stm.MemoryObject;
import utilities.xml.ExpressionEvaluator;
import utilities.xml.Substitution;
import utilities.xml.Xml;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.FileReader;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * ADESimActorComponentImpl.java
 * <p/>
 * An abstract actor class that connects to the ADE simulation environment
 *
 * @author Michael Zlatkovsky (based on original ADESim by Paul Schermerhorn, et al)
 * @see ADESimEnvironmentComponentImpl
 */
public abstract class ADESimActorComponentImpl extends ADEComponentImpl implements ADESimActorComponent {
    private static final long serialVersionUID = 1L;
    protected static boolean coordinatesRead = false;
    protected static ArrayList<Point2D> robotHullCoordinates = new ArrayList<>();
    private static Double defTV = null;
    private static Double defRV = null;
    private static Double acceleration = null;
    private static SimLocationSpecifier startupLocationSpecifier;
    private static boolean noisyOdometry = false;
    private static Double criticalDistance;
    /** command line can specify which views to start off the sim with */
    private static String viewFlags;
    /**
     * command line can specify which edit permissions to start off the sim with to false in your methods, once it's
     * ready it should stay ready! Note that there should not ever be a need to worry about the "else" case of if
     * (ready) because if it's not initialized and you can't, say, add commands to it -- it's perfectly ok!  Why?
     * because once it received the model, it will already have updated objects there.  If the model and robot have not
     * been initialized, then there's nowhere to add commands too, and nothing to tick!
     */
    private static String editFlags = null;
    private static long visionTypeIdCount = -1;
    private final ADESimActorComponentImpl meTheActorComponent = this;
    private final HashMap<Long, ActionStatus> statusMap;
    private final Object visionLock = new Object();
    /**
     * contains only vision searches that have been explicitly named using getTypeId(final List<Predicate> descriptors,
     * final Predicate typeName)
     */
    private final HashMap<Long, Predicate> visionTypeNames_byId;
    private final HashMap<Predicate, List<Long>> visionTypeIds_byTypeName;
    /** contains all instantiated vision searches */
    private final HashMap<Long, List<Predicate>> visionTypeDescriptors;
    /**
     * An executor that handles the processing of tick commands and immediate updates (receiveImmediateUpdate). The
     * problem that the executor solves is the following: During a regular tick execution, the model iterates over its
     * world objects and checks if the robot intersects with them (if so, dispatching an action).  In the case of a
     * "remove" action, the remove action calls the sim environment and requests to have an object removed. if the
     * request is granted (no other conflicting requests from other robots), the environment turns around and calls
     * "receiveImmediateUpdate" on the actor, and that, in turn, will remove the object -- all the while the model was
     * trying to iterate.  This then, swiftly, causes a concurrent modification exception and all sorts of problems down
     * the road). By using an executor, on the other hand, immediate updates are placed on a queue that will wait for
     * the tick command -- and hence the iteration -- to complete, making the sequence safe. (Note that this probably
     * applies to other commands as well, such as adding of world objects; I just happened to notice it with object
     * removal).
     */
    private final ExecutorService executor;
    // for use when Environment's UpdateManager is set for immediate updates.
    final private ArrayList<ActorCommand> immediateCommandsToStoreForVisualizationHistoryHolder;
    public SimModel model; // public so that subclasses can access it
    protected Optional<Object> simEnvironmentReference; // protected so available to subclasses
    /** A flag for when the simulation is ready */
    private boolean ready;
    private String myActorName;
    private int tickCounter;
    /**
     * used by GUI when user picks up or puts down the robot.  While you are moving the robot by mouse, it's rather
     * frustrating when it tries desperately to escape. This flag temporarily "lifts the robot off the ground" if it is
     * set to false. However, robots are assumed to be allowed to move by default (on startup)
     */
    private boolean allowRobotMotion = true;
    private HistoryHolder historyHolderForVisualization;

    ////////////////////////////////////////////////////////////////////////
    // METHODS THAT MUST BE OVERWRITTEN BY SUB-CLASSES:
    ////////////////////////////////////////////////////////////////////////

    public ADESimActorComponentImpl() throws RemoteException {
        super();
        // initialize fields
        simEnvironmentReference = Optional.absent();
        historyHolderForVisualization = new HistoryHolder();
        immediateCommandsToStoreForVisualizationHistoryHolder = new ArrayList<>();
        visionTypeIds_byTypeName = new HashMap<>();
        visionTypeDescriptors = new HashMap<>();
        ready = false;
        visionTypeNames_byId = new HashMap<>();
        statusMap = new HashMap<>();
        executor = Executors.newSingleThreadExecutor();
        //        robotHullCoordinates = new ArrayList<>();

        alertSimEnvOfThisActorsExistence();
    }

    /**
     * creates robot that will be placed by the parent into the model.  For simulator name argument when creating a
     * robot, use "getActorName()". (for example, "return new SimPioneer(model, getActorName());" )
     */
    protected abstract SimAbstractRobot createRobot(boolean noisyOdometry);

    /** append any additional commandline arguments, if any */
    protected abstract void appendActorSpecificUsageInfo(StringBuilder sb);

    /**
     * called by ADESimActorComponentImpl to check if a command-line argument is recognized by the concrete
     * (non-abstract) sub-class.  Returns number by which to advance the parse if the argName is recognized (0 if the
     * argument is self-sufficient, but 1 or 2 or etc if needed to read the next word or two), and -1 if unrecognized
     * option altogether (and hence the abstract class should do the parsing instead, or report an error).
     *
     * @param args, the ENTIRE command-line argument array
     * @param i,    current index in the command-line array (so that can check next args, if needed)
     */
    protected abstract int checkIfRecognizeArgument(String[] args, int i);

    /** default acceleration, 0-1.  higher means quicker stopping */
    protected abstract double getDefaultAcceleration();

    /** default translational velocity, meters/sec */
    protected abstract double getDefaultTV();


    ////////////////////////////////////////////////////////////////////////

    /** default rotational velocity, rads/sec */
    protected abstract double getDefaultRV();

    private void alertSimEnvOfThisActorsExistence() {
        //        finishedInitialization(this);
        System.out.println("Attempting to connect to the simulation environment...");
        String environmentType = ADESimEnvironmentComponent.class.getName();
        boolean firstTry = true;


        // MS: TODO: this should be fixed
        while (!simEnvironmentReference.isPresent()) {
            try {
                simEnvironmentReference = Optional.fromNullable(getClient(environmentType));
            } catch (Exception e) {
                // if got an exception, then obviously could not connect.  So be it,
                //     the sim might be the first in a group, or, more likely than not,
                //     it's just a single-robot simulation to begin with!
                System.out.println("exception with cross-reg:  " + e);
            }

            if (firstTry && (!simEnvironmentReference.isPresent())) {
                firstTry = false;
                System.out.println("Could not obtain a reference to the simulation environment.  " +
                        "Waiting until an environment starts up...");

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    System.out.println("Could not sleep while trying to find a reference to the " +
                            "simulation environment.");
                }
            }
        } // end while

        // having gotten a simulator reference, request to join the club on next tick!
        try {
            call(simEnvironmentReference.get(), "requestToJoinSimulation", this.getID());
        } catch (Exception e) {
            System.out.println("Could not send a request to join the simulation.  Unexpected error, quitting! " + e);
            System.exit(1);
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void joinSimulation(SimWelcomePackage welcomePackage) throws RemoteException {
        // create a "bridge" by which the model can call the environment
        ActorModelOwner actorModelOwner = new ActorModelOwner() {
            @Override
            public void callEnvironment(String methodName, Object... args) {
                safeCallEnvironment(methodName, args);
            }

            @Override
            public SimModel getModel() {
                return model;
            }
        };

        tickCounter = welcomePackage.tickCounter;
        this.model = new SimModel(actorModelOwner,
                welcomePackage.worldBounds,
                welcomePackage.worldObjects,
                welcomePackage.otherRobotShapes);

        // startup properties (i.e., location and contained objects (actually, contained objects
        //    will be done lower, after location specifier is set))
        if (startupLocationSpecifier == null) {
            // if location was not already set by commandline, accept environment suggestion if any:
            if (welcomePackage.startUpPropertiesIfAny != null) {
                startupLocationSpecifier = welcomePackage.startUpPropertiesIfAny.startupLocation;
            }
        }

        // if neither command line nor welcome package specified initial position, just
        //     create new specifier (0,0,0)
        if (startupLocationSpecifier == null) {
            startupLocationSpecifier = new SimLocationSpecifier(new Point3D(0, 0, 0), 0);
        }


        initDefaultVelocitiesAndAccelerations();

        initializeRobot(createRobot(noisyOdometry));


        if (welcomePackage.startUpPropertiesIfAny != null) {
            if (welcomePackage.startUpPropertiesIfAny.containedObjects != null) {
                for (SimEntity entityToAdd : welcomePackage.startUpPropertiesIfAny.containedObjects) {
                    // move object so that it is in front of robot
                    Point2D locationInFrontOfRobot = this.model.robot.getLocationInFrontOfRobot(entityToAdd);
                    Point2D currentObjectCenter = entityToAdd.getShape().getCenter();
                    ObjectMover.translate(entityToAdd,
                            locationInFrontOfRobot.getX() - currentObjectCenter.getX(),
                            locationInFrontOfRobot.getY() - currentObjectCenter.getY(),
                            model.robot.getGroundHeight() + model.robot.getShape().getZLength());
                    this.model.itemsInRobotPossession.add(entityToAdd);
                }
            }
        }

        historyHolderForVisualization.add(tickCounter, new ArrayList<ActorCommand>());

        // at the completion of this method, the robot's ready!
        this.ready = true;
    }

    private void initDefaultVelocitiesAndAccelerations() {
        if (defTV == null) {
            defTV = getDefaultTV();
        }
        if (defRV == null) {
            defRV = getDefaultRV();
        }
        if (acceleration == null) {
            acceleration = getDefaultAcceleration();
        }
    }

    @Override
    public final void keyPressed(int keyCode, String simName) throws RemoteException {
        if (referringToMe(simName)) {
            keyPressed(keyCode);
        } else {
            // should never happen, since GUI would not allow to call actor's key
            //    events for non-owned robots.
            throw new RemoteException("KeyPressed call was for " + simName +
                    ", but somehow got picked up by " + getActorName());
        }
    }

    @Override
    public final void keyReleased(int keyCode, String simName) throws RemoteException {
        if (referringToMe(simName)) {
            keyReleased(keyCode);
        } else {
            // should never happen, since GUI would not allow to call actor's key
            //    events for non-owned robots.
            throw new RemoteException("keyReleased call was for " + simName +
                    ", but somehow got picked up by " + getActorName());
        }
    }

    @Override
    public final String getHelpText(String simName) throws RemoteException {
        if (referringToMe(simName)) {
            return getHelpText();
        } else {
            // should never happen, since GUI would not allow to call help
            //    on anything but the owned robot
            throw new RemoteException("getHelpText call was for " + simName +
                    ", but somehow got picked up by " + getActorName());
        }
    }

    @Override
    public Xml generateRobotPropertiesXML() throws RemoteException {
        Xml positionXml = new Xml("position");
        SimLocationSpecifier location = this.model.robot.getLocationSpecifier();
        positionXml.addAttribute("x", Double.toString(location.getX()));
        positionXml.addAttribute("y", Double.toString(location.getY()));
        positionXml.addAttribute("theta", Double.toString(location.getTheta()));

        if (this.model.itemsInRobotPossession.size() > 0) {
            Xml containsXml = positionXml.addChild(new Xml("contains"));
            containsXml.addChildren(this.model.itemsInRobotPossession.generateXMLs());
        }

        return positionXml;
    }

    @Override
    public ADEGuiVisualizationSpecs getVisualizationSpecs() throws RemoteException {
        ADEGuiVisualizationSpecs specs = super.getVisualizationSpecs();
        specs.add("Environment", ADESimMapVis.class,
                ADESimMapVisualizationType.SINGLE_ROBOT);
        return specs;
    }

    /** places robot into model */
    private void initializeRobot(SimAbstractRobot robot) {
        robot.setAcceleration(acceleration);
        robot.setLocationSpecifier(startupLocationSpecifier);
        robot.getLocationSpecifier().offsetZbyHalfOfRobotHeight(robot);
        this.model.placeRobotIntoModel(robot);

        if (criticalDistance != null) {
            if (model.robot instanceof LaserCarrying) {
                ((LaserCarrying) model.robot).setCRITICALDIST(criticalDistance);
            }
        }

        // having placed the robot:
        notifyEnvironmentOfChangeInActorShape();
    }

    /** {@inheritDoc} */
    @Override
    public void tick(final int tickCounter,
                     final ArrayList<ActorCommand> generalCommands,
                     final ArrayList<ActorCommand> actorSpecificCommands) throws RemoteException {
        if (isReady()) {

            Future<?> executionFuture = executor.submit(new Runnable() {
                @Override
                public void run() {
                    meTheActorComponent.tickCounter = tickCounter;
                    model.applyEnvironmentOrderedCommands(generalCommands);

                    // add to history holder for visualization -- note the special processing depending
                    //    on whether or not had any "immediate commands" that came separately from generalCommands,
                    //    and that need to be noted for the visualization
                    synchronized (immediateCommandsToStoreForVisualizationHistoryHolder) {
                        if (immediateCommandsToStoreForVisualizationHistoryHolder.size() == 0) {
                            // just add the general commands
                            historyHolderForVisualization.add(tickCounter, generalCommands);
                        } else {
                            ArrayList<ActorCommand> accumulatedGeneralCommands = new ArrayList<>(generalCommands);
                            accumulatedGeneralCommands.addAll(immediateCommandsToStoreForVisualizationHistoryHolder);
                            immediateCommandsToStoreForVisualizationHistoryHolder.clear();
                            historyHolderForVisualization.add(tickCounter, accumulatedGeneralCommands);
                        }
                    }

                    model.applyEnvironmentOrderedCommands(actorSpecificCommands);

                    boolean robotMoved = model.tick(tickCounter, meTheActorComponent.allowRobotMotion);
                    if (robotMoved) {
                        notifyEnvironmentOfChangeInActorShape();
                    }
                }
            });


            // wait on the task to complete by calling .get() on the executionFuture.
            //     That way, robots will not get out of sync with each other.
            try {
                if (executionFuture.get() != null) {
                    throw new Exception("Execution future.get() should have been null, but is not!");
                }
            } catch (Exception e) {
                System.out.println("An error occurred while executing the \"tick\" task!  The exception is " + e);
            }

        }
    }

    /** {@inheritDoc} */
    @Override
    public void receiveImmediateUpdate(final ActorCommand command, final boolean actorSpecific) throws RemoteException {
        if (isReady()) {
            // sometimes, the request for an immediate update comes in the middle of a tick,
            //      which can cause concurrent modification exceptions (in the case of, say,
            //      removing a beacon from a list of world objects).  To avoid this,
            //      I want to submit the task for eventual execution, but first finish
            //      off the currently executing task.
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    model.applyEnvironmentOrderedCommand(command);
                    if (!actorSpecific) {
                        synchronized (immediateCommandsToStoreForVisualizationHistoryHolder) {
                            immediateCommandsToStoreForVisualizationHistoryHolder.add(command);
                        }
                    }
                }
            });
            // Again, unlike with "tick" where I want to wait for completion, here I do not.
            //     the commands will get executed in due time, and before the next tick.
        }
    }

    protected void updateComponent() {
        // do nothing -- governed by tick!
    }

    /** {@inheritDoc} */
    @Override
    protected void updateFromLog(String logEntry) {
        // do nothing -- for now, anyway...
    }


    // *********************************************************************
    // abstract/interface methods that need to be implemented/overridden
    // *********************************************************************

    /**
     * This method will be activated whenever a client calls the requestConnection(uid) method. Any connection-specific
     * initialization should be included here, either general or user-specific.
     */
    protected void clientConnectReact(String user) {
        // do nothing
    }

    /**
     * This method will be activated whenever a client that has called the requestConnection(uid) method fails to update
     * (meaning that the heartbeat signal has not been received by the reaper), allowing both general and user specific
     * reactions to lost connections. If it returns true, the client's connection is removed.
     */
    protected boolean clientDownReact(String user) {
        System.out.println("Lost connection with " + user);

        if (isOfSimEnvironmentType(user)) {
            System.out.println("The lost connection was a connection to the environment!  Quitting simulation");
            System.exit(10);
        }

        // do nothing
        return false;
    }

    private boolean isOfSimEnvironmentType(String user) {
        try {
            Class<?> classInQuestion = Class.forName(getTypeFromID(user));
            if (classIsOrInheritsFromSomeTargetClass(classInQuestion, ADESimEnvironmentComponent.class)) {
                System.out.println("Checking server's class:  " +
                        user + " is an instance of " + ADESimEnvironmentComponent.class.getName());
                return true;
            }
        } catch (Exception e) {
            System.out.println("Could not get class info for " + user + ".  " +
                    "\nAssuming that this is not an instance of " + ADESimEnvironmentComponent.class.getName());
        }

        // if hasn't returned as true:
        return false;
    }

    private boolean classIsOrInheritsFromSomeTargetClass(
            Class<?> classInQuestion, Class<?> targetClass) {
        if (classInQuestion == targetClass) {
            return true;
        }
        // otherwise search interfaces:
        for (Class<?> eachInterface : classInQuestion.getInterfaces()) {
            if (eachInterface == targetClass) {
                return true;
            }
        }

        // if hasn't quit:
        return false;
    }

    /**
     * This method will be activated whenever the heartbeat returns a remote exception (i.e., the server this is sending
     * a heartbeat to has failed).
     */
    protected void componentDownReact(String serverKey, String[][] constraints) {
        // do nothing
    }

    /**
     * This method will be activated whenever the heartbeat reconnects to a client (e.g., the server this is sending a
     * heartbeat to has failed and then recovered). <b>Note:</b> the pseudo-reference will not be set until <b>after</b>
     * this method is executed. To perform operations on the newly (re)acquired reference, you must use the <tt>ref</tt>
     * parameter object.
     *
     * @param serverKey the ID of the {@link ade.ADEComponent ADEComponent} that connected
     * @param ref       the pseudo-reference for the requested server
     */
    protected void componentConnectReact(String serverKey, Object ref, String[][] constraints) {
        // do nothing
    }

    /**
     * Adds additional local checks for credentials before allowing a shutdown must return "false" if shutdown is
     * denied, true if permitted
     */
    protected boolean localrequestShutdown(Object credentials) {
        return false;
    }

    /** Implements the local shutdown mechanism that derived classes need to implement to cleanly shutdown */
    protected void localshutdown() {
        // do nothing
    }

    /** Provide additional information for usage... */
    protected String additionalUsageInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Component-specific options:\n\n");
        appendActorSpecificUsageInfo(sb);
        sb.append("\n");
        sb.append("  -deftv tv          <default translational velocity>\n");
        sb.append("  -defrv rv          <default rotational velocity>\n");
        sb.append("  -acceleration val  <acceleration [0, 1] determines robot's momentum.  Higher = quicker stopping.>\n");
        sb.append("  -critical dist     <set CRITICALDIST to dist>\n");
        sb.append("  -initpose x y t    <initial pose>\n");
        sb.append("  -noisy             <initiates noisy odometry (off, i.e., ground truth, by default)>\n");
        sb.append("  -view flags        <sets up the \"preset\" view options for the simulator environement.\n" +
                "                      flag is a string of digits, one character per checkbox in the View menu (\"Show tooltips\",\n" +
                "                      \"Show laser lines\", etc).  Each digit is in the range 0-3, with \n" +
                "                      0 corresponding to OFF and *NOT* VISIBLE (hence *NOT* changeable from the GUI),\n" +
                "                      1 = ON and *NOT* VISIBLE, 2 = OFF and VISIBLE (can be changed from the GUI),\n" +
                "                      and 3 = ON and VISIBLE");
        sb.append("  -edit flags        <sets up the \"preset\" edit permission options for the simulator environement.\n" +
                "                      flag is a string of digits, one character per checkbox in the Edit menu, " +
                "                      following the same specifications as the -view flag description above");
        return sb.toString();
    }

    /**
     * Parse additional command-line arguments
     *
     * @return "true" if parse is successful, "false" otherwise
     */
    protected boolean parseadditionalargs(String[] args) {
        boolean found = false;
        for (int i = 0; i < args.length; i++) {
            // first, check if subclass (pioneer, etc) knows about this argument.
            int recognitionResult = this.checkIfRecognizeArgument(args, i);

            if (recognitionResult >= 0) { // if recognized something
                found = true;
                i = i + recognitionResult; // advance i's count as necessary
            } else { // if subclass did not recognize the argument

                if (args[i].equalsIgnoreCase("-deftv")) {
                    double tv;
                    try {
                        tv = Double.parseDouble(args[i + 1]);
                        i++;
                        defTV = tv;
                    } catch (NumberFormatException nfe) {
                        System.err.println("Error setting deftv " + args[i + 1]);
                        System.err.println(nfe);
                        System.err.println("Reverting to defTV = " + defTV);
                    }
                    found = true;
                } else if (args[i].equalsIgnoreCase("-defrv")) {
                    double rv;
                    try {
                        rv = Double.parseDouble(args[i + 1]);
                        i++;
                        defRV = rv;
                    } catch (NumberFormatException nfe) {
                        System.err.println("Error setting defrv " + args[i + 1]);
                        System.err.println(nfe);
                        System.err.println("Reverting to defRV = " + defRV);
                    }
                    found = true;
                } else if (args[i].equalsIgnoreCase("-critical")) {
                    try {
                        criticalDistance = Double.parseDouble(args[i + 1]);
                        i++;
                        System.out.println("new CRITICALDIST " + criticalDistance);
                    } catch (NumberFormatException nfe) {
                        System.err.println("Could not set critical distance based on commandline!");
                        System.out.println(nfe);
                    }
                    found = true;
                } else if (args[i].equalsIgnoreCase("-initpose")) {
                    ExpressionEvaluator evaluator = new ExpressionEvaluator();
                    startupLocationSpecifier = new SimLocationSpecifier(
                            new Point3D(evaluator.evaluateDouble(args[i + 1]),
                                    evaluator.evaluateDouble(args[i + 2]),
                                    0),
                            evaluator.evaluateDouble(args[i + 3], Substitution.RANDOM_ANGLE_SUBSTITUTION));
                    i = i + 3;
                    found = true;
                } else if (args[i].equalsIgnoreCase("-acceleration")) {
                    acceleration = Double.parseDouble(args[++i]);
                    found = true;
                } else if (args[i].equalsIgnoreCase("-noisy")) {
                    noisyOdometry = true;
                    found = true;
                } else if (args[i].equalsIgnoreCase("-view")) {
                    viewFlags = args[++i];
                    found = true;
                } else if (args[i].equalsIgnoreCase("-edit")) {
                    editFlags = args[++i];
                    found = true;
                } else if (args[i].equalsIgnoreCase("-coordinates") && (++i < args.length)) {
                    readCoordinates(args[i]);
                    found = true;
                } else {
                    System.out.println("Unrecognized argument: " + args[i]);
                    return false;  // return on any unrecognized args
                }
            }
        }
        return found;
    }

    public String getActorName() { // public for subclasses to see.
        if (myActorName == null) {

            try {
                //	        	finishedInitialization(this);
                myActorName = this.getNameFromID(this.getID());
            } catch (RemoteException e) {
                System.out.println("Component could not get its own ID, this could not be good!  " +
                        "Expect things to crash!");
                myActorName = "unknown server name!";
            }
        }

        return myActorName;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean localServicesReady() {
        return this.isReady();
    }

    /** {@inheritDoc} */
    @Override
    public String getCurrentStateConfigXML() throws RemoteException {
        return (String) safeCallEnvironment("getCurrentStateConfigXML");
    }







    /* **** VelComponent interface **** */

    /**
     * Get translational velocity.
     *
     * @return the most recent TV reading.
     */
    public double getTV() throws RemoteException {
        return model.robot.getTargetTV();
    }

    /**
     * Get rotational velocity.
     *
     * @return the most recent RV reading.
     */
    public double getRV() throws RemoteException {
        return model.robot.getTargetRV();
    }

    /**
     * Get translational and rotational velocity.
     *
     * @return the most recent velocity readings.
     */
    public double[] getVels() throws RemoteException {
        return new double[]{getTV(), getRV()};
    }

    /**
     * Get the default velocities used by MoPo functions.
     *
     * @return the default velocities.
     */
    public double[] getDefaultVels() throws RemoteException {
        return new double[]{defTV, defRV};
    }

    /** Stop. */
    public void stop() throws RemoteException {
        canLogIt("Stopping.", true);
        model.robot.setTargetVels(0.0, 0.0);
    }

    /**
     * Set translational velocity.
     *
     * @param tv the new TV
     * @return true if there's nothing in front of the robot, false otherwise.
     */
    public boolean setTV(double tv) throws RemoteException {
        canLogIt("setTV: " + tv, true);
        model.robot.setTargetTV(tv);
        return true;
    }

    /**
     * Set rotational velocity.
     *
     * @param rv the new RV
     */
    public boolean setRV(double rv) throws RemoteException {
        canLogIt("setRV: " + rv, true);
        model.robot.setTargetRV(rv);

        if (model.robot instanceof LaserCarrying) {
            if (rv == 0) {
                return true; // straight ahead is presumably fine
            } else {
                AbstractLaser laser = ((LaserCarrying) model.robot).getLaser();
                boolean[] safeSpaces = laser.getSafeSpaces();
                if (rv < 0) {
                    return safeSpaces[0]; // on the right
                } else {
                    return safeSpaces[2]; // on the left
                }
            } // end if non-zero RV
        } else {
            return true; // if robot has no laser, return true (since what else could it return?)
        }
    }

    /**
     * Set both velocities.
     *
     * @param tv the new TV
     * @param rv the new RV
     * @return true if there's nothing in front of the robot, false otherwise.
     */
    public boolean setVels(double tv, double rv) throws RemoteException {
        canLogIt("setVels: " + tv + " " + rv, true);

        model.robot.setTargetVels(tv, rv);

        if (model.robot instanceof LaserCarrying) {
            AbstractLaser laser = ((LaserCarrying) model.robot).getLaser();
            boolean[] safeSpaces = laser.getSafeSpaces();
            return safeSpaces[1]; // in front
        } else {
            return true; // if robot has no laser, return true (since what else could it return?)
        }
    }




    /* **** PosComponent interface **** */

    /**
     * Get current location.
     *
     * @return the current location.
     */
    public double[] getPoseGlobal() throws RemoteException {
        return model.robot.getPoseGlobal();
    }

    /* **** Miscellaneous **** */

    /**
     * Return the stall value.  If the robot is in a collision, stall will be true (i.e., motors ostensibly on, but no
     * motion).
     *
     * @return true if robot is stalled, false otherwise
     */
    public boolean getStall() throws RemoteException {
        return model.robot.getStall();
    }

    /** {@inheritDoc} */
    @Override
    public double getOrientation() throws RemoteException {
        return Math.toDegrees(model.robot.getLocationSpecifier().getTheta());
    }

    /** {@inheritDoc} */
    @Override
    public RobotVisualizationData getRobotVisData(boolean drawLaserLines,
                                                  boolean drawActivatedObstacleSensors,
                                                  boolean drawPerceivedObjects) throws RemoteException {
        return model.getRobotVisData(drawLaserLines, drawActivatedObstacleSensors,
                drawPerceivedObjects);
    }

    /** helper method for calling "notifyOfChangeInActorShape" on environment */
    protected void notifyEnvironmentOfChangeInActorShape() {
        safeCallEnvironment("notifyOfChangeInActorShape", getActorName(), model.robot.getShape());
    }

    /** {@inheritDoc} */
    @Override
    public SimShape getRobotShape() throws RemoteException {
        return this.model.robot.getShape();
    }

    /** {@inheritDoc} */
    @Override
    public double[] getPoseEgo() throws RemoteException {
        if (isReady()) {
            return this.model.robot.getPoseEgoInternal();
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void resetOdometry() {
        this.model.robot.resetPoseEgoInternal();
    }

    /** {@inheritDoc} */
    @Override
    public Double[][] getObjectsAngleAndDistance(String[] lookingForObjects) throws RemoteException {
        return model.getObjectsAngleAndDistance(lookingForObjects);
    }

    /** protected so subclasses can use this convenient method too */
    protected Object safeCallEnvironment(String methodName, Object... args) {
        try {
            return call(simEnvironmentReference.get(), methodName, args);
        } catch (ADEException e1) {
            safeCallEnvironmentAnnounceError(methodName, e1);
        }
        return null;
    }

    private void safeCallEnvironmentAnnounceError(String methodName, Exception exception) {
        System.err.println("Could not call " + methodName + " on environment reference " +
                "due to \n" + exception);
        exception.printStackTrace();
    }

    /** {@inheritDoc} */
    @Override
    public ConcurrentHashMap<String, SimShape> getRobotShapes() throws RemoteException {
        return this.model.otherRobotShapes;
    }

    /** {@inheritDoc} */
    @Override
    public ArrayList<RobotVisualizationData> getRobotsVisData(boolean drawLaserLines,
                                                              boolean drawActivatedObstacleSensors, boolean drawPerceivedObjects) throws RemoteException {
        ArrayList<RobotVisualizationData> visData = new ArrayList<>();
        visData.add(getRobotVisData(drawLaserLines, drawActivatedObstacleSensors, drawPerceivedObjects));
        return visData;
    }

    /** {@inheritDoc} */
    @Override
    public String getViewPresetFlags() throws RemoteException {
        return viewFlags;
    }

    /** {@inheritDoc} */
    @Override
    public String getEditPresetFlags() throws RemoteException {
        return editFlags;
    }

    /** {@inheritDoc} */
    @Override
    public void updateDoorStatusOnMap(UUID doorGUID, DoorUpdatingStatus status) throws RemoteException {
        // broadcast request to environment:
        safeCallEnvironment("updateDoorStatusOnMap", doorGUID, status);
    }

    /** {@inheritDoc} */
    @Override
    public void updateBoxStatusOnMap(UUID boxGUID, boolean open) throws RemoteException {
        safeCallEnvironment("updateBoxStatusOnMap", boxGUID, open);
    }

    /** {@inheritDoc} */
    @Override
    public void updateObjectShapeOnMap(UUID objectID, SimShape newShape) throws RemoteException {
        safeCallEnvironment("updateObjectShapeOnMap", objectID, newShape);
    }

    /** {@inheritDoc} */
    @Override
    public void updateObjectNameOnMap(UUID objectID, String newName) throws RemoteException {
        safeCallEnvironment("updateObjectNameOnMap", objectID, newName);
    }

    /** {@inheritDoc} */
    @Override
    public void updateObjectParentOnMap(UUID objectID, UUID[] newContainerID) throws RemoteException {
        safeCallEnvironment("updateObjectParentOnMap", objectID, newContainerID);
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeObjectFromMap(UUID objectID) throws RemoteException {
        return (Boolean) safeCallEnvironment("removeObjectFromMap", objectID);
    }

    /** {@inheritDoc} */
    @Override
    public void addObjectToMap(SimEntity object) throws RemoteException {
        safeCallEnvironment("addObjectToMap", object);
    }

    /** {@inheritDoc} */
    @Override
    public void createNewEnvironment(SimShape worldBounds, boolean bounded) throws RemoteException {
        safeCallEnvironment("createNewEnvironment", worldBounds, bounded);
    }

    /** {@inheritDoc} */
    @Override
    public void moveRobotRelativeWithImmediateRefresh(Point2D.Double offset,
                                                      String simName) throws RemoteException {
        if (referringToMe(simName)) {
            model.robot.getLocationSpecifier().addLocationOffset(offset);
            model.displaceItemsInRobotPossessionBasedOnRobotMovement(offset.x, offset.y, 0, 0);
            model.robot.updateLaserReadingsIfAny();

            notifyEnvironmentOfChangeInActorShape();
        } else {
            System.out.println("moveRobotRelativeWithImmediateRefresh:  simName does not match my name!");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void allowRobotMotion(String simName, boolean allow) throws RemoteException {
        if (referringToMe(simName)) {
            this.allowRobotMotion = allow;
        } else {
            System.out.println("allowRobotMotion:  simName does not match my name!");
        }
    }

    private boolean referringToMe(String simName) throws RemoteException {
        if (simName.equals(getNameFromID(this.getID()))) {
            return true;
        } else {
            System.err.println("The request refers to some other simulator, " + simName +
                    ", not myself (" + getNameFromID(this.getID()) + ")!");
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean pickUpObject(String lookingForCriteria) {
        if (model.robot.getArmReach() == null) {
            // can't pick up anything.
            return false;
        }

        // check if the center of ANY item is within arm-distance away from the robot
        SimEntity objectToPickUp = model.getFirstMatchingObjectWithinDistance(
                lookingForCriteria, SimEntity.class,
                model.robot.getArmReach());
        if (objectToPickUp == null) {
            System.out.println("No matching object to pick up!");
            return false; // no-op
        } else {
            return (Boolean) safeCallEnvironment("requestPickUpObject", objectToPickUp.getGUID(), getActorName());
        }

    }

    /** {@inheritDoc} */
    @Override
    public boolean putDownObject(String matchingCriteria) throws RemoteException {
        List<SimEntity> matchingObjectsInPossession =
                model.itemsInRobotPossession.getMatchingObjects(
                        new String[]{matchingCriteria}, SimEntity.class);
        if (matchingObjectsInPossession.size() > 0) {
            return (Boolean) safeCallEnvironment("requestPutDownObject", matchingObjectsInPossession.get(0), getActorName());
        } else {
            System.out.println("No matching object to put down found!");
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getObjectsInPossesionCount() throws RemoteException {
        return this.model.itemsInRobotPossession.size();
    }

    /** {@inheritDoc} */
    @Override
    public boolean putObjectIntoContainer(String objectInPossessionCriteria,
                                          String containerNameCriteria) throws RemoteException {
        // first, object needs to be in possession.
        List<SimEntity> matchingObjectsInPossession =
                model.itemsInRobotPossession.getMatchingObjects(
                        new String[]{objectInPossessionCriteria}, SimEntity.class);
        if (matchingObjectsInPossession.size() == 0) {
            System.out.println("No matching objects in possession!");
            return false;
        }

        // secondly, the container needs to be within reach:
        SimEntity containerToPutInto = model.getContainerWithinReach(containerNameCriteria);
        if (containerToPutInto == null) {
            System.out.println("No matching container within reach!");
            return false;
        }

        return (Boolean) safeCallEnvironment("requestPutObjectIntoContainer",
                matchingObjectsInPossession.get(0), containerToPutInto.getGUID(), getActorName());
    }

    /** {@inheritDoc} */
    @Override
    public boolean getObjectFromContainer(String objectToPickUpCriteria,
                                          String containerNameCriteria) throws RemoteException {

        SimEntity matchingContainer = model.getContainerWithinReach(containerNameCriteria);
        if (matchingContainer == null) {
            return false;
        }

        List<SimEntity> matchingObjectsInContainer =
                ((SimContainerEntity) matchingContainer).getObjectsHolder().getMatchingObjects(
                        new String[]{objectToPickUpCriteria}, SimEntity.class);
        if (matchingObjectsInContainer.size() == 0) {
            return false;
        }

        return (Boolean) safeCallEnvironment("requestGetObjectFromContainer",
                matchingObjectsInContainer.get(0).getGUID(), matchingContainer.getGUID(), getActorName());
    }

    /** {@inheritDoc} */
    @Override
    public boolean openDoor(String matchingDescription) throws RemoteException {
        return openOrCloseDoorCommon(matchingDescription, DoorUpdatingStatus.OPEN);
    }

    /** {@inheritDoc} */
    @Override
    public boolean closeDoor(String matchingDescription) throws RemoteException {
        return openOrCloseDoorCommon(matchingDescription, DoorUpdatingStatus.CLOSE);
    }

    private boolean openOrCloseDoorCommon(String matchingDescription, DoorUpdatingStatus doorStatus) {
        if (model.robot.getDoorOpenerReach() == null) {
            // can't open or close anything.
            return false;
        }

        // Doors live in the Dynamic object holder (they move!)
        // check if the center of ANY item is within arm-distance away from the robot
        Door doorToOpen = (Door) model.getFirstMatchingObjectWithinDistance(
                matchingDescription, Door.class,
                model.robot.getDoorOpenerReach());
        if (doorToOpen == null) {
            System.out.println("No door to open within reach!");
            return false; // no-op
        } else {
            return (Boolean) safeCallEnvironment("requestDoorAction", doorToOpen.getGUID(), doorStatus);
        }
    }

    private String getNameFromId(Long tokenId) {
        String name = null;
        for (ADEPercept eachPercept : this.model.robot.getPercepts("*")) {
            if (tokenId == eachPercept.time) {
                name = eachPercept.name;
                break;
            }
        }
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public boolean openBox(Long tokenId) throws RemoteException {
        String name = getNameFromId(tokenId);
        System.out.println("openBox name is: " + name);
        SimEntity matchingContainer = model.getContainerWithinReach(name);
        if (matchingContainer == null) {
            return false;
        }
        return (Boolean) safeCallEnvironment("requestSetContainerStatus", matchingContainer.getGUID(), true);
    }

    /** {@inheritDoc} */
    @Override
    public boolean openBox(String name) throws RemoteException {
        SimEntity matchingContainer = model.getContainerWithinReach(name);
        if (matchingContainer == null) {
            return false;
        }
        return (Boolean) safeCallEnvironment("requestSetContainerStatus", matchingContainer.getGUID(), true);
    }

    /** {@inheritDoc} */
    @Override
    public boolean closeBox(Long tokenId) throws RemoteException {
        String name = getNameFromId(tokenId);
        SimEntity matchingContainer = model.getContainerWithinReach(name);
        if (matchingContainer == null) {
            return false;
        }
        return (Boolean) safeCallEnvironment("requestSetContainerStatus", matchingContainer.getGUID(), false);
    }

    /** {@inheritDoc} */
    @Override
    public boolean closeBox(String name) throws RemoteException {
        SimEntity matchingContainer = model.getContainerWithinReach(name);
        if (matchingContainer == null) {
            return false;
        }
        return (Boolean) safeCallEnvironment("requestSetContainerStatus", matchingContainer.getGUID(), false);
    }

    /** {@inheritDoc} */
    @Override
    public boolean getFromBox(Long boxId, Long blockId) throws RemoteException {
        String boxname = getNameFromId(boxId);
        String blockname = getNameFromId(blockId);
        return getObjectFromContainer(blockname, boxname);
    }

    /** {@inheritDoc} */
    @Override
    public boolean getFromBox(String boxname, String blockname) throws RemoteException {
        return getObjectFromContainer(blockname, boxname);
    }

    /** {@inheritDoc} */
    @Override
    public boolean putIntoBox(Long boxId, Long blockId) throws RemoteException {
        String boxname = getNameFromId(boxId);
        String blockname = getNameFromId(blockId);
        return putObjectIntoContainer(blockname, boxname);
    }

    /** {@inheritDoc} */
    @Override
    public boolean putIntoBox(String boxname, String blockname)
            throws RemoteException {
        return putObjectIntoContainer(blockname, boxname);
    }

    /** {@inheritDoc} */
    @Override
    public boolean pickUpBox(Long tokenId) throws RemoteException {
        String name = getNameFromId(tokenId);
        return pickUpObject(name);
    }

    /** {@inheritDoc} */
    @Override
    public boolean pickUpBox(String name) throws RemoteException {
        return pickUpObject(name);
    }

    /** {@inheritDoc} */
    @Override
    public boolean putDownBox(Long tokenId) throws RemoteException {
        String name = getNameFromId(tokenId);
        return putDownObject(name);
    }

    /** {@inheritDoc} */
    @Override
    public boolean putDownBox(String name) throws RemoteException {
        return putDownObject(name);
    }

    /**
     * get percepts within robot's camera's field of vision, matching specific criteria ("box", "block", etc, or actual
     * object names, i.e., "blue box"). Special cases: - if want to match ANY object, use the wildcard "*" - if you want
     * to match another robot, use "robot" or the robot's name.
     */
    @Override
    public ArrayList<ADEPercept> getPercepts(String[] matchingCriteria) throws RemoteException {
        return this.model.robot.getPercepts(matchingCriteria);
    }

    /** {@inheritDoc} */
    @Override
    public ArrayList<ADEPercept> getBoxes() throws RemoteException {
        return getPercepts(new String[]{Box.TYPE});
    }

    /** {@inheritDoc} */
    @Override
    public ArrayList<ADEPercept> getBlocks() throws RemoteException {
        return getPercepts(new String[]{Block.TYPE});
    }

    /** {@inheritDoc} */
    @Override
    public ArrayList<ADEPercept> getLandmarks() throws RemoteException {
        return getPercepts(new String[]{Landmark.TYPE});
    }

        /* PWS: LFD-based getDoorways overriding this
    @Override
	public ArrayList<ADEPercept> getDoorways() throws RemoteException {
		return getPercepts(new String[] {Door.TYPE});
	}
        */

    /**
     * Get visual percepts of the type in 360 degree radius.  This corresponds to a physical scan of the surroundings,
     * although no actions are performed here. Criteria can be the object's type ("box", "block", etc), or actual object
     * names, i.e., "blue box". Special cases: - if want to match ANY object, use the wildcard "*" - if you want to
     * match another robot, use "robot" or the robot's actual name.
     *
     * @param criteria the object type to scan for
     * @return a Vector of ADEPercepts corresponding to each object of the given type that are visible from the robot's
     *         current position
     */
    @Override
    public ArrayList<ADEPercept> lookFor(String criteria) throws RemoteException {
        // look around for object ("swivel" camera all 360 degrees).
        return this.model.robot.getPercepts(360, criteria);
    }

    /**
     * gets boxes and blocks by color.  Note that unlike lookFor(String type), getObjectsOfColor only returns objects
     * within its PERCEPTUAL FIELD, NOT a 360-degree span.
     *
     * @param color the object color to scan for
     * @return a Vector of ADEPercepts corresponding to each object of the given color in the visual field
     */
    @Override
    public ArrayList<ADEPercept> getObjectsOfColor(String color) throws RemoteException {
        ArrayList<ADEPercept> matchingColoredObjects = new ArrayList<>();
        for (ADEPercept each : this.model.robot.getPercepts(new String[]{Box.TYPE, Block.TYPE})) {
            // each.color can be null.  but hopefully the passed-in parameter color is not!
            if (color.equalsIgnoreCase(each.color)) {
                matchingColoredObjects.add(each);
            }
        }
        return matchingColoredObjects;
    }

        /* **** Manipulate objects by vision id **** */

    /**
     * picks up the object with the given vision token ID if it is within range of the actor's arm (if the actor HAS an
     * arm).  If not, just a no-op. "true" simply means that the request was accepted, though if more than one robot go
     * for the object at the same time, both will get "true", but only one will actually get the object.
     *
     * @param tokenId the vision token ID, as returned by one of the VisionComponent methods
     * @return an ID that can be used for subsequent status queries
     */
    @Override
    public long pickUpObject(Long tokenId) {
        String name = null;
        for (ADEPercept eachPercept : this.model.robot.getPercepts("*")) {
            System.out.println("checking " + eachPercept.time + " for " + tokenId);
            if (tokenId == eachPercept.time) {
                name = eachPercept.name;
                break;
            }
        }
        if (name == null) {
            return -1L;
        }
        long id = System.currentTimeMillis();
        ActionStatus st = ActionStatus.SUCCESS;
        if (!pickUpObject(name)) {
            st = ActionStatus.FAIL;
        }
        statusMap.put(id, st);
        return id;
    }

    /**
     * Check status of an extended action.  Soon this will be obsoleted, as the extended commands will notify Action of
     * their completions.
     *
     * @param aid the identifying timestamp of the action to check
     * @return the status found for the indicated action
     */
    @Override
    public ActionStatus checkAction(long aid) {
        ActionStatus st = statusMap.get(aid);

        if (st == null) {
            st = ActionStatus.UNKNOWN;
        }
        return st;
    }

    /**
     * puts down object if it is in the robot's possession.  In doesn't match anything (or if no objects exist), a
     * no-op.
     *
     * @param tokenId the vision token ID, as returned by one of the VisionComponent methods
     * @return false if the object is unknown or not currently held
     * @throws RemoteException an ADE error
     */
    @Override
    public boolean putDownObject(Long tokenId) throws RemoteException {
        String name = null;
        List<SimEntity> matchingObjectsInPossession =
                model.itemsInRobotPossession.getMatchingObjects(
                        new String[]{"*"}, SimEntity.class);
        for (SimEntity eachPercept : matchingObjectsInPossession) {
            if (tokenId == eachPercept.getTime()) {
                name = eachPercept.getName();
                break;
            }
        }
        return name != null && putDownObject(name);
    }
        

  /* **** From VisionComponent interface **** */

    /**
     * Find all MemoryObjectTypes containing at least one token in STM. MemoryObjectType must have been asked for in
     * order to "initialize detection" and must be currently perceivable in camera's FOV. One each, regardless of how
     * many tokens of a particular type are in STM.
     *
     * @param conf ignored in simulation
     */
    @Override
    public List<Long> getTypeIds(final double conf) {
        ArrayList<Long> ids = new ArrayList<>();
        synchronized (visionLock) {
            for (Long typeId : visionTypeDescriptors.keySet()) {
                //check if typeid is currently in FOV
                if (!getTokenIds(typeId, 0.0).isEmpty()) {
                    ids.add(typeId);
                }
            }
        }
        return ids;
    }

    /**
     * Get the MemoryObjectType ID (search ID) based on a Predicate description of the object (eg, type(X, door),
     * type(X, block)). NOTE: This is only a convenience method so a List doesn't need to be passed when only a single
     * Predicate is used.
     *
     * @param descriptor Predicate description of object
     * @return MemoryObjectType Id (-1 if there is no match)
     */
    @Override
    public Long getTypeId(final Predicate descriptor) {
        Long id = -1L;
        if (!descriptor.getName().equalsIgnoreCase("type")) {
            System.err.println("getTypeId expects descriptors of the form: type(X,__)");
            return id;
        }

        List<Predicate> descriptorList = new ArrayList<>();
        descriptorList.add(descriptor);
        return getTypeId(descriptorList);
    }

    /**
     * Get the MemoryObjectType ID (search ID) based on a Predicate description of the object.
     *
     * @param descriptors Predicate description of object
     * @return MemoryObjectType Id (-1 if there is no match)
     */
    @Override
    public Long getTypeId(final List<Predicate> descriptors) {
        Long id = -1L;

        synchronized (visionLock) {
            //look for existing descriptor match
            for (Long typeId : visionTypeDescriptors.keySet()) {
                List<Predicate> currDescriptors = visionTypeDescriptors.get(typeId);
                if (descriptorsMatch(currDescriptors, descriptors)) {
                    id = typeId;
                    break;
                }
            }

            //if existing match wasn't found, add a new entry if
            //request can be searched for
            if (id == -1L && canMeetSearchRequest(descriptors)) {
                visionTypeIdCount += 1;
                id = visionTypeIdCount;
                visionTypeDescriptors.put(id, descriptors);
            }
        }

        return id;
    }

    /**
     * Get the descriptors for a particular vision type.
     *
     * @param typeId get descriptors which match the typeId
     * @return a list of all matching descriptors
     */
    @Override
    public List<Predicate> getDescriptors(final Long typeId) throws RemoteException {
        return visionTypeDescriptors.get(typeId);
    }

    /**
     * Name the collection of Predicate descriptors referred to by the MemoryObjectType ID (search ID).
     *
     * @param typeId   MemoryObjectType ID (search ID)
     * @param typeName Predicate name of object (name will be bound to typeId in VisionComponent)
     * @return true if typeId exists, false otherwise
     * @throws RemoteException an ADE error
     */
    @Override
    public boolean nameDescriptors(final Long typeId, final Predicate typeName) throws RemoteException {
        boolean result = false;
        synchronized (visionLock) {
            //check if search is valid. if so, name it
            if (visionTypeDescriptors.containsKey(typeId)) {
                List<Long> ids = visionTypeIds_byTypeName.get(typeName);
                if (ids == null) {
                    ids = new ArrayList<>();
                    visionTypeIds_byTypeName.put(typeName, ids);
                }
                if (!ids.contains(typeId)) {
                    ids.add(typeId);
                    visionTypeNames_byId.put(typeId, typeName);
                }
            }
        }
        return result;
    }

    /**
     * Get MemoryObject IDs of perceivable objects in FOV.
     *
     * @param conf ignored in simulation.
     * @return List of STM MemoryObject IDs
     */
    @Override
    public List<Long> getTokenIds(final double conf) {
        List<Long> ids = new ArrayList<>();
        synchronized (visionLock) {
            //look through all "active MemoryObjectType" searches
            for (Long typeId : visionTypeDescriptors.keySet()) {
                ids.addAll(getTokenIds(typeId, conf));
            }
        }
        return ids;
    }

    /**
     * Get MemoryObject IDs of perceivable objects that have specified MemoryObjectType ID.
     *
     * @param typeId MemoryObjectType ID of tracked objects
     * @param conf   ignored in simulator
     * @return List of STM MemoryObject IDs
     */
    @Override
    public List<Long> getTokenIds(final long typeId, final double conf) {
        ArrayList<Long> ids = new ArrayList<>();
        List<Predicate> descriptors;
        synchronized (visionLock) {
            descriptors = visionTypeDescriptors.get(typeId);
        }

        if (descriptors != null && !descriptors.isEmpty()) {
            for (ADEPercept eachPercept : this.model.robot.getPercepts("*")) {
                //check that all descriptors match percept's info
                if (perceptMatchesDescriptors(descriptors, eachPercept)) {
                    ids.add(eachPercept.time);
                }
            }
        }
        return ids;
    }

    /**
     * Get MemoryObject IDs for all tokens *exactly* matching descriptors in STM with confidence greater than threshold.
     * If no match exists, a MemoryObjectType will attempt to be built. This is largely a convenience method and
     * <getTypeId> should be used whenever possible.
     *
     * @param descriptors list of Predicate descriptors
     * @param conf        ignored used in ADESIM
     * @return List of STM MemoryObject IDs
     */
    @Override
    public List<Long> getTokenIds(final List<Predicate> descriptors, final double conf) {
        synchronized (visionLock) {
            //look for existing descriptor match
            Long id = -1L;
            for (Long typeId : visionTypeDescriptors.keySet()) {
                List<Predicate> currDescriptors = visionTypeDescriptors.get(typeId);
                if (descriptorsMatch(currDescriptors, descriptors)) {
                    id = typeId;
                    break;
                }
            }

            //get results from for typeId
            if (id != -1L) {
                return getTokenIds(id, 1.0);
            } else if (canMeetSearchRequest(descriptors)) {
                //if existing match wasn't found, add a new entry if
                //request can be searched for
                visionTypeIdCount += 1;
                id = visionTypeIdCount;
                visionTypeDescriptors.put(id, descriptors);
                return getTokenIds(id, 1.0);
            } else {
                System.out.println("[getTokenIds] could not find match for descriptors: " + descriptors);
                return null;
            }
        }
    }

    /**
     * Get the MemoryObject with the specified id.
     *
     * @param tokenId MemoryObject ID in STM
     * @param conf    minimum confidence of tokens to include -- ignored in simulator
     * @return MemoryObject token (Null if doesn't exist)
     */
    @Override
    public MemoryObject getToken(final long tokenId, final double conf) {
        for (ADEPercept eachPercept : this.model.robot.getPercepts("*")) {
            if (tokenId == eachPercept.time) {
                return createMemoryObjectFromADEPercept(eachPercept);
            }
        }
        return null;
    }

    /**
     * Get perceivable MemoryObjects.
     *
     * @param conf ignored in simulator
     * @return List of MemoryObjects
     */
    @Override
    public List<MemoryObject> getTokens(final double conf) {
        List<MemoryObject> objs = new ArrayList<>();
        synchronized (visionLock) {
            //look through all "active MemoryObjectType" searches
            for (Long typeId : visionTypeDescriptors.keySet()) {
                objs.addAll(getTokens(typeId, conf));
            }
        }
        return objs;
    }

    /**
     * Get perceivable MemoryObjects of the specified MemoryObjectType ID.
     *
     * @param typeId MemoryObjectType ID of tracked objects
     * @param conf   ignored in simulator
     * @return List of MemoryObjects
     */
    @Override
    public List<MemoryObject> getTokens(final long typeId, final double conf) {
        ArrayList<MemoryObject> objs = new ArrayList<>();
        List<Predicate> descriptors;
        synchronized (visionLock) {
            descriptors = visionTypeDescriptors.get(typeId);
        }

        if (descriptors != null && !descriptors.isEmpty()) {
            for (ADEPercept eachPercept : this.model.robot.getPercepts("*")) {
                //check that all descriptors match percept's info
                if (perceptMatchesDescriptors(descriptors, eachPercept)) {
                    objs.add(createMemoryObjectFromADEPercept(eachPercept));
                }
            }
        }
        return objs;
    }

    /**
     * Get MemoryObjects *exactly* matching descriptors in STM with confidence greater than threshold. If no match
     * exists, a MemoryObjectType will attempt to be built. This is largely a convenience method and <getTypeId> should
     * be used whenever possible.
     *
     * @param descriptors list of Predicate descriptors
     * @param conf        ignored in ADESIM
     * @return List of STM MemoryObjects
     */
    @Override
    public List<MemoryObject> getTokens(final List<Predicate> descriptors, final double conf) {
        synchronized (visionLock) {
            //look for existing descriptor match
            Long id = -1L;
            for (Long typeId : visionTypeDescriptors.keySet()) {
                List<Predicate> currDescriptors = visionTypeDescriptors.get(typeId);
                if (descriptorsMatch(currDescriptors, descriptors)) {
                    id = typeId;
                    break;
                }
            }

            if (id != -1L) {
                return getTokens(id, 1.0);
            } else if (canMeetSearchRequest(descriptors)) {
                //if existing match wasn't found, add a new entry if
                //request can be searched for
                visionTypeIdCount += 1;
                id = visionTypeIdCount;
                visionTypeDescriptors.put(id, descriptors);
                return getTokens(id, 1.0);
            } else {
                System.out.println("[getTokens] could not find match for descriptors: " + descriptors);
                return null;
            }
        }
    }

    /**
     * Confirms that the object is still in STM.
     *
     * @param tokenId MemoryObject ID of the object to be confirmed
     * @return true if the MemoryObject is present in STM
     */
    @Override
    public boolean confirmToken(final long tokenId) {
        for (ADEPercept eachPercept : this.model.robot.getPercepts("*")) {
            if (tokenId == eachPercept.time) {
                return true;
            }
        }
        return false;
    }

    /**
     * Confirms that the object is still in STM.
     *
     * @param token MemoryObject to be confirmed
     * @return true if the object is present in STM
     */
    @Override
    public boolean confirmToken(final MemoryObject token) {
        for (ADEPercept eachPercept : this.model.robot.getPercepts("*")) {
            if (token.getTokenId() == eachPercept.time) {
                return true;
            }
        }
        return false;
    }

    // ============== START Incremental Search Methods ====================================

    /**
     * This instantiates a new MemoryObjecType (search manager) which can be incrementally configured via
     * <addDescriptor>.
     *
     * @return MemoryObjectType ID (search id)
     */
    @Override
    public Long createNewType() {
        Long id;

        synchronized (visionLock) {
            //add a new entry
            visionTypeIdCount += 1;
            id = visionTypeIdCount;
            List<Predicate> descriptors = new ArrayList<>();
            visionTypeDescriptors.put(id, descriptors);
        }

        return id;
    }

    /**
     * Add new search constraint (i.e., ImageProcessor) to an existing MemoryObjectType (specified by searchID).
     *
     * @param typeId     - unique MemoryObjectType ID returned by <createNewType>
     * @param descriptor - predicate describing visual search attribute (e.g., "color(X,red)")
     */
    @Override
    public boolean addDescriptor(final long typeId, final Predicate descriptor) {
        synchronized (visionLock) {
            //make sure typeId exists
            if (!visionTypeDescriptors.containsKey(typeId)) {
                return false;
            }

            //add descriptor
            List<Predicate> descriptors = visionTypeDescriptors.get(typeId);
            if (descriptors == null) {
                descriptors = new ArrayList<>();
                visionTypeDescriptors.put(typeId, descriptors);
            }
            descriptors.add(descriptor);
        }

        return true;
    }

    /**
     * Remove search constraint (i.e., processing descriptor and/or ImageProcessor) to an existing MemoryObjectType
     * (specified by searchID).
     *
     * @param typeId     - unique MemoryObjectType ID returned by <createNewType>
     * @param descriptor - to remove
     * @return - if removal was successful
     */
    @Override
    public boolean removeDescriptor(final long typeId, final Predicate descriptor) {
        synchronized (visionLock) {
            //make sure typeId exists
            if (!visionTypeDescriptors.containsKey(typeId)) {
                return false;
            }

            //remove descriptor if it exists
            List<Predicate> descriptors = visionTypeDescriptors.get(typeId);
            if (descriptors == null || !descriptors.contains(descriptor)) {
                return false;
            }
            descriptors.remove(descriptor);
        }

        return true;
    }

    /**
     * Signal the end of constraint addition. Descriptors/constraints can no longer be added to the MemoryObjectType
     * after this has been called. This doesn't do anything in ADESIM since there aren't actually any Detector/Trackers
     * to turn on/off. This method is only here so that the sim and real versions of vision can be used the same way.
     *
     * @param typeId unique MemoryObjectType ID returned by <createNewType>
     */
    @Override
    public void endDescriptorChanges(final long typeId) {
    }
    // ============== END Incremental Search Methods ====================================

    /**
     * Removes MemoryObjectType. There isn't actually anything to turn off in ADESIM.
     *
     * @param typeId remove the memory object matching with this type ID
     */
    @Override
    public void stopAndRemoveType(final long typeId) {
        synchronized (visionLock) {
            visionTypeDescriptors.remove(typeId);
            Predicate removedName = visionTypeNames_byId.remove(typeId);
            if (removedName != null) {
                visionTypeIds_byTypeName.remove(removedName);
            }
        }
    }

    /**
     * ADESIM Vision Component helper function that returns if lists of predicates match exactly (order doesn't
     * matter).
     *
     * @param first  - first list of Predicates
     * @param second - second list of Predicates
     * @return if lists match exactly
     */
    private boolean descriptorsMatch(List<Predicate> first, List<Predicate> second) {
        //System.out.println("descriptorsMatch this: " + description + " other: " + descriptors);
        return (first.size() == second.size() && first.containsAll(second));
    }

    /**
     * ADESIM Vision Component helper function that ensures requested visual search is possible. Checks that the
     * requested descriptors map to descriptions that ADESIM can handle. Currently these are limited to: color(X, __)
     * and type(X, __).
     *
     * @param descriptors - list of predicates to check
     * @return true i.f.f. ADESIM can handle the predicates
     */
    private boolean canMeetSearchRequest(List<Predicate> descriptors) {

        //TODO: a more thorough check of allowable types and colors
        // TODO: making it return true for the empty conditionals since that is what happens anyway
        for (Predicate descriptor : descriptors) {
            if (descriptor.getName().equalsIgnoreCase("type")) {
                return true;
//        if (!descriptor.get(1).getName().equalsIgnoreCase("")) {
//          return false;
//        }
            } else if (descriptor.getName().equalsIgnoreCase("color")) {
                return true;
//        if (!descriptor.get(1).getName().equalsIgnoreCase("")) {
//          return false;
//        }
            } else {
                System.err.println("[canMeetSearchRequest] unrecognized desciptor: " + descriptor);
                return false;
            }
        }

        return true;
    }

    /**
     * Vision Component helper function that checks if the ADEPercept matches the descriptors. Returns false if the
     * descriptors are empty or contain an unrecognized descriptor.
     *
     * @param descriptors - predicates describing percept
     * @param percept     - existing ADEPercept
     * @return true i.f.f. the descriptors are non-empty and all are recognized
     */
    private boolean perceptMatchesDescriptors(List<Predicate> descriptors, ADEPercept percept) {
        if (descriptors == null || descriptors.isEmpty()) {
            return false;
        }

        //check that all descriptors match percept's info
        for (Predicate descriptor : descriptors) {
            if (descriptor.getName().equalsIgnoreCase("type")) {
                if (!descriptor.get(1).getName().equalsIgnoreCase(percept.type)) {
                    //EAK: to be backwards compatible, allow any type to be considered a blob.
                    if (!descriptor.get(1).getName().equalsIgnoreCase("blob")) {
                        return false;
                    }
                }
            } else if (descriptor.getName().equalsIgnoreCase("color")) {
                if (!descriptor.get(1).getName().equalsIgnoreCase(percept.color)) {
                    return false;
                }
            } else {
                System.err.println("[perceptMatchesDescriptors] unrecognized desciptor: " + descriptor);
                return false;
            }
        }

        return true;
    }

    /**
     * ADESIM Vision Component helper function that builds a MemoryObject from an ADEPercept.
     *
     * @param percept an ADE percept
     * @return a memory object representing an ADEPercept
     */
    private MemoryObject createMemoryObjectFromADEPercept(ADEPercept percept) {
        MemoryObject memoryObject = new MemoryObject();
        memoryObject.addDescriptor(utilities.Util.createPredicate(percept.type, new Variable("x")));
        memoryObject.setFrameNum(tickCounter);

        if (percept.colorObject != null) {
            memoryObject.setColorLabel(percept.color);
            memoryObject.setColorR(percept.colorObject.getRed());
            memoryObject.setColorG(percept.colorObject.getGreen());
            memoryObject.setColorB(percept.colorObject.getBlue());
        }

        // not sure what should be XCG and YCG (center of gravity)
        //    in a one-dimensional plane, so not setting
        //    any of the xcg, ycg, oxcg, oycg, area parameters.

        // tilt doesn't make much sense, either, but will set pan:
        memoryObject.setPan(percept.heading);  // looks like the sign is correct for the pan:
        //    essentially, if an object is in front of the robot and to the left, we want
        //    it to be positive, by the counter-clock-wise positive convention of mathematics.

        memoryObject.setDist(percept.distance);
        memoryObject.setConfidence(1.0); // in the sim, see everything with "full confidence"
        memoryObject.setTokenId(percept.time);

        return memoryObject;
    }

    /**
     * THIS IS OBSOLETE AND HAS BEEN REMOVED FROM THE API
     * <p/>
     * Return the MemoryObject w/ the specified key (i.e. its unique id). gets the actual object.
     *
     * @param key  the STM key for the object to be tracked
     * @param conf minimum confidence of tokens to include (ignored)
     * @return reference to the STM object
     */
    @Deprecated
    public MemoryObject getToken(String key, double conf) throws RemoteException {
        ArrayList<ADEPercept> matchingPercepts = this.model.robot.getPercepts(key);
        if (matchingPercepts.size() == 0) {
            System.err.println("Could not find the desired token, return null");
            return null;
        } else {
            if (matchingPercepts.size() > 1) {
                System.out.println("More than one token matched, return first one.");
            }
            return createMemoryObjectFromADEPercept(matchingPercepts.get(0));
        }
    }

    /**
     * THIS IS OBSOLETE AND HAS BEEN REMOVED FROM THE API
     * <p/>
     * Return a vector of the keys for all objects of the specified type.
     *
     * @param type description of type to be returned (e.g., person, object)
     * @param conf minimum confidence of tokens to include (ignored)
     * @return a vector of STM keys
     */
    @Deprecated
    public ArrayList<MemoryObject> getAllByType(String type, double conf) throws RemoteException {
        ArrayList<MemoryObject> memoryObjects = new ArrayList<>();
        for (ADEPercept eachPercept : this.model.robot.getPercepts(type)) {
            memoryObjects.add(createMemoryObjectFromADEPercept(eachPercept));
        }
        return memoryObjects;
    }

    /**
     * THIS IS OBSOLETE AND HAS BEEN REMOVED FROM THE API
     * <p/>
     * Return a vector of the keys for all objects of the specified color.
     *
     * @param desc label of color to be returned (e.g., red, blue, green)
     * @param conf minimum confidence of tokens to include (ignored)
     * @return a vector of STM keys
     */
    @Deprecated
    public ArrayList<MemoryObject> getAllByColorDescriptor(String desc, double conf) throws RemoteException {
        ArrayList<MemoryObject> memoryObjectsMatchingColor = new ArrayList<>();
        for (ADEPercept eachPercept : this.model.robot.getPercepts()) {
            if (desc.equalsIgnoreCase(eachPercept.color)) {
                memoryObjectsMatchingColor.add(createMemoryObjectFromADEPercept(eachPercept));
            }
        }
        return memoryObjectsMatchingColor;
    }

    private void readCoordinates(String file) {
        if (file.isEmpty()) return;
        try (Scanner s = new Scanner(new BufferedReader(new FileReader(file)))) {
            double keep = 0.0;
            boolean even = true;
            while (s.hasNext()) {
                if (even)
                    keep = Double.parseDouble(s.next());
                else {
                    Point2D.Double p = new Point2D.Double(keep, Double.parseDouble(s.next()));
                    robotHullCoordinates.add(p);
                }
                even = !even;
            }
            coordinatesRead = true;
        } catch (Exception e) {
            System.out.println("Error reading Coordinates... ");
            e.printStackTrace();
        }
    }

    @Override
    public SimMapVisUpdatePackage getSimMapVisUpdatePackage(
            int lastUpdateTickCount,
            boolean drawLaserLines, boolean drawActivatedObstacleSensors,
            boolean drawPerceivedObjects) throws RemoteException {

        // vis data.  just create an array with only ones' self.
        //     Note that it is NOT cached, because different
        //     visualizations will probably request the data differently (one might want lasers,
        //     the other won't)
        ArrayList<RobotVisualizationData> visDataArray = new ArrayList<>();
        visDataArray.add(getRobotVisData(drawLaserLines, drawActivatedObstacleSensors, drawPerceivedObjects));

        HistoryHolder.UpdateData updateHistory = historyHolderForVisualization.getUpdateHistory(lastUpdateTickCount);
        String title = "ADESim from the perspective of \"Actor\" " + getActorName() + ".  " +
                ADESimEnvironmentComponentImpl.getElapsedTimeForTickCounter(tickCounter);
        if (updateHistory == null) {
            return new SimMapVisUpdatePackage(visDataArray,
                    this.tickCounter, title,
                    model.worldBounds, model.worldObjects, model.otherRobotShapes);
        } else {
            return new SimMapVisUpdatePackage(visDataArray,
                    updateHistory.tickCount, title,
                    updateHistory.commands);
            // note, passing it the history holder's tickCount, in case the current one
            //     has already changed.
        }
    }

    public Sim3DVisUpdatePackage getSim3DVisUpdatePackage(int lastUpdateTickCount) throws RemoteException {

        CameraData cameraData = model.robot.getCameraDataFor3DVis();

        if (cameraData == null) {
            String errorText = "Robot does not have a camera, cannot visualize its 3D view";
            System.err.println(errorText);
            throw new RemoteException(errorText);
        }

        // if still here, obtained camera data successfully.

        HistoryHolder.UpdateData updateHistory = historyHolderForVisualization.getUpdateHistory(lastUpdateTickCount);

        String title = getActorName() + "'s 3D view, " +
                ADESimEnvironmentComponentImpl.getElapsedTimeForTickCounter(tickCounter);

        if (updateHistory == null) {
            return new Sim3DVisUpdatePackage(cameraData,
                    this.tickCounter, title,
                    model.worldBounds, model.worldObjects, model.otherRobotShapes);
        } else {
            return new Sim3DVisUpdatePackage(cameraData,
                    updateHistory.tickCount, title,
                    updateHistory.commands);
            // note, passing it the history holder's tickCount, in case the current one
            //     has already changed.
        }
    }

    public boolean isReady() {
        return ready;
    }
}
