# SmallADE + Lang #

## What is smallADE? ##

The Agent Development Architecture (ADE) is an implementation of the
  DIARC     Cognitive Architecture. ADE consists, mainly, of a set of
  *components* that provide useful functionality. For example, the 
  LRFComponent provides an interface to a Laser Range Finder, and the
  WheelchairComponent provides an interface to the robot
  wheelchair. An instance of ADE consists of a set of components whose
  connections and status are managed by one or more *Registries*. 

This document details how to use a subset of ADE referred to as
  "smallADE + Lang" which includes a limited subset of ADE Components,
  and details how to use a specific *config file*, which
  simultaneously starts a selection of the architecture's
  components. The code for all components used can be found by
  navigating to `[ade]/com`. 

## smallADE Quickstart ##
In this section, I'll detail how to get some simple smallADE setups up and running.

### Acquiring the Code ###

Pending public release, the SmallADE + Lang code is currently stored
in a **private** repository. This means that to access it, you'll need
to contact Tom Williams at <williams@cs.tufts.edu>. Once Tom has added
you to the access list, you will be able to clone the repository. 

To clone the repository using ssh, add your ssh key to your bitbucket account and then run

`git clone git@bitbucket.org:tom_williams/smallade_w_lang.git`

To clone the repository using https, run

`git clone https://[your-bitbucket-username]@bitbucket.org/tom_williams/smallade_w_lang.git`

This will create a new directory entitled `smallade_w_lang` containing
the smallADE + Lang code. This directory will be henceforth refered to
as `[ade]`.

### Compiling ###

To compile, just run `./ant`. To compile a particular component, run
`./ant [name of component]`. Try tab-completing out ./ant and see what
options come up for you.  

### Starting Components in Parallel ###

To start components in parallel, you should use a **config file**. 

The config file we will be running can be found at `[ade]/config/robots/weheelchair/wheelsim.config`.

This config file starts seven components encapsulated in
STARTCOMPONENT-STOPCOMPONENT blocks.

1. `com.adesim.ADESimEnvironmentComponent`
    * This starts the simulated robot environment.
    * Examine the component's "componentargs" line. We can see two arguments:
        * `-cfg com/adesim/config/200BostonReverse.xml` : This specifies which map to use.
        * `-g` : This starts the visualization, so that we can see what is going on.
2. `com.adesim.SimWheelchairComponent`
    * This starts the simulated wheelchair, and uses the following arguments:
        * `-initpose -2.5 0 4.7` : specifies the initial X,Y,T pose of the robot.
        * `-coordinates config/robots/wheelchair/wheelsimCoords.conf` :
          specifies the robot's geometry, for obstacle avoidance. 
3. `com.motion.MotionComponent`
    * provides simple movement functionality (e.g., the ability to
      traverse forward, travel to a particular point, or turn a certain
      amount), and uses the following arguments: 
        * `-deadreck` : specifies that the robot uses dead reckoning to localize. 
        * `-critical 0.6` : specifies the critical distance at which the robot must stop for fear of hitting an obstacle.
        * `-deftv 0.35` : specifies the robot's default translational velocity
        * `-defrv 0.2` : specifies the robot's default rotational velocity
        * `-slowtv 0.25` : specifies the robot's slow translational velocity
        * `-slowrv 0.2` : specifies the robot's slow rotational velocity
        * `-tol 0.8` : specifies the robot's travel tolerance
        * `-teps 0.1` : specifies the robot's turning tolerance
4. `com.action.GoalManagerPriority`
    * The ADE Goal Manager, which, well, manages goals, and which uses the following arguments:
        * `-component com.interfaces.NLPComponent` : indicates that the GM needs to find an NLPComponent to connect to.
        * `-component com.motion.MotionComponent` : indicates that the GM needs to find a MotionComponent to connect to.
        * `-component com.adesim.SimWheelchairComponent` : indicates
          that the GM needs to find a SimWheelchairComponent to connect
          to. 
        * `-dbfilesupp com/action/db/muri-nofunnybusiness.xml` : specifies
          the set of supplemental actions the GM can use to pursue goals
          beyond those actions available to it by default. 
        * `-script listen self --` : specifies the robot's default behavior.
        * `-agentname self` : specifies the name of the agent.
5. `com.discourse.TLDLDiscourseComponent`
    * The NLP component used for syntactic and semantic parsing, which uses the following arguments:
        * `-action` : indicates that this component needs to find an action manager to connect to.
        * `-actor cbot` : specifies the name of the agent to use in discourse.
        * `-dict com/discourse/TL_DL_parser/newautonomy.dict` : specifies the parser's lexicon / grammar.
6. `com.simspeech.SimSpeechRecognitionComponent`
    * The simulated speech recognizer, which uses the following arguments:
        * `-g` : starts the interface used to input commands.
        * `-cfg com/simspeech/config/newautonomy` : specifies the set of utterances to display in the interface
        * `-nlp com.discourse.TLDLDiscourseComponent` : tells the simulated speech recognizer to look for a
          TLDLDiscourseComponent to connect to.
7. `com.simspeech.SimSpeechProductionComponent`
    * Simulated speech output, using the following argument:
        * `-g` : causes simulated speech output to be displayed in popup
          windows (in addition to being displayed in the command line. 


### Running the Demo ###
To run this config file, execute:  

`./ant run-registry -Df=config/robots/wheelchair/wheelsim.config -Dargs=-g`

You will see the following three visualization windows pop up:

* The Registry:
    * This is the central hub where all the ADE components connect.
    * Mousing over a component shows what other components that
      component has connections to. 
    * Right-clicking on a component provides a variety of options. 
      By Right-clicking on the Goal Manager and selecting the "Action
      Manager" visualization, you will be able to see the various goals
      currently being pursued by the Goal Manager.
    * This visualization appears because of the `-g` provided in the
      `-Dargs` list above. 
* The ADESim Environment
    * This shows the simulated wheelchair positioned in our lab.
    * The robot appears as it does because, in the config file specified above
      (i.e., `config/robots/wheelchair/wheelsim.config`), two adesim
      components are started: 
        1. An ADESimEnvironmentComponent with a particular specified map.
        2. A SimWheelChairComponent with a particular initial pose
    * You can drag the robot around in the environment. This is
      particularly useful for seeing in what positions the robot can and
      cannot maneuver. 
    * Click "View" and check all of the unchecked boxes. You will now
      see some green lines and some yellow lines. These are,
      respectively, the robot's laser and camera fields of view.  Try
      dragging the robot over so that the red box is in its field of
      view. You will see a circle appear around it, signifying that the
      robot's simulated camera has "recognized" the box.
* Simulated Speech Recognition
    * Using this interface, you can tell the robot to stop, go straight,
      turn left, and turn right.  If there's not enough room to
      maneuver, the robot won't respond, but you should see a message in
      the terminal specifying why, and, if all of the "View" boxes are
      checked, a small blue dot should be apparent on the side of the
      robot where an obstacle is preventing movement.

If the wheelchair worked, simply replacing the simulated robot and
environment with the physical robot and its sensors would
(theoretically) result in identical behavior in the real world.

### Starting Components Individually ###

Let's try running a few components by starting them individually.

First, run the registry: `/ant run-registry`

Next, start the simulator: `./ant run-adesimenvironmentcomponent -Dargs='-g'`

Finally, add a simulated robot: `./ant run-simpioneercompnent`

To drive the simulated robot around the simulated environment, use the arrow keys. You can also drag the robot around with your mouse.
To see the laser readings, go to `view->show laser lines`. To see the world from a 3D perspective, go to `3D->Create 3D View` To use a different map, add to the sim environment's args string `-config com/adesim/config/[name-of-map-you-want.xml]`

### Running the Robot###
To run the robot, the process is similar:

First, run the registry: `./ant run-registry`

Next, start the create: `./ant run-createcomponent -Dargs="-port=/dev/ttyACM0"`

Then, start the LRF Component: `./ant run-urglrfcomponent -Dargs="-port /dev/ttyAMA0 -g"`

Finally, start the keyboard joystick: `./ant run-keyboardjoystickcomponent`

### Running Distributed smallADE ###
Now let's look at running parts of the system on the robot and parts on another computer. Here's how we do that:

On the remote computer:
Run the registry: `./ant run-registry -Dk=config/hosts/[yourname].hosts -Dl=[your ip]`

For this to work, the robot computer needs to appear in your hosts file ( (e.g., config/hosts/matt.hosts) on the remote computer, and vice versa. 

On the robot:

Start the create: `./ant run-createcomponent -Dargs="-port=/dev/ttyACM0" -Dr=[your ip] -Dl=[the robot's ip]`

**If this doesn't work, try ttyAMA0 instead.**

Start the LRF Component: `/ant run-urglrfcomponent -Dr=[your ip] -Dl=[the robot's ip]`

Back on the remote computer: 

Run the keyboard component: `/ant run-keyboardjoystickcomponent -Dr=[your ip] -Dl=[your ip]`


## Writing your own smallADE Component ##
In order to actually interact with the create and lrf, you'll want to write your own ADE Component.

### Writing the Component ###
Let's say you want to write a new component, JimsEKFComponent. To do so, you'll want to do three things:

1. Create a new folder in `/com` to house your component (e.g., `/com/ekf`)
2. Create your component interface (JimsEKFComponent.java)
3. Create your component implementation (JimsEKFComponentImpl.java)

The interface file should contain something like:

```java
 package com.ekf;
 
 import ade.ADEComponent;
 import java.rmi.RemoteException;
 
 public interface JimsEKFComponent extends ADEComponent{
   public void doThatEKFThang() throws RemoteException;
 }
```

Note that each function you expose in your component needs to throw RemoteException, since ADE uses RMI.

The implementation file should contain something like:

```java
 package com.ekf;
 
 import ade.Connection;
 import ade.SuperADEComponentImpl;
 import java.rmi.RemoteException;
 import com.LaserScan; 
 
 public class JimsEKFComponentImpl extends SuperADEComponentImpl implements JimsEKFComponent{
    Connection lrfConn;
    boolean doneConstructing;
 
    public JimsEKFComponentImpl() throws RemoteException {
        super();
        lrfConn = connectToComponent("com.lrf.LRFComponent");
        doneConstructing = true;
        System.out.println("Ready to go.");
    }
 
    @Override
    protected void init() {
        lrfConn = null;
        doneConstructing = false;
    }
 
    @Override
    protected boolean localServicesReady() {
         return doneConstructing && requiredConnectionsPresent();
    }
    
    @Override
    protected void readyUpdate() {
         LaserScan ls = (LaserScan)lrfConn.call("getLaserScan", LaserScan.class);
         System.out.println("Scan "+ls+" received.");
    }
    
    @Override
    protected String additionalUsageInfo() {
        return "The component has no arguments.";
    }
 
    @Override
    public void doThatEKFThang() throws RemoteException {
        System.out.println("Doin' that EKF Thang");
    }
  
 }
```

Important things to note in here:

* init() is required, and is called before any code in the constructor is called, and after additional arguments (if any) are parsed. This is where you should set the default values of any class variables, unless you want terrible things to happen.
* readyUpdate is called every update cycle once the component is ready (i.e., once the criteria in `localServicesReady` are met), which is by default once every 100 ms. This time can be changed by calling `this.setUpdateLoopTime(this, SOME\_TIME\_IN\_MS)`
* To call a method advertised by another component, you must have a connection to that component, and you can then do `THATCONNECTION.call("METHODNAME", TYPERETURNEDBYMETHOD, ANYARGSTOPASSTOMETHOD`, making sure to cast the returned object to the appropriate type.


## Building the Component ##
Examine build targets like `create` in `build.xml` for examples for creating ant build targets for building your component. Perhaps yours will look something like this:

```xml
 <target name='ekf' depends='init, core' extensionOf='java-only' description='Builds Jims terrible, terrible ekf.'>
   <build-java package.dir='com/ekf'/>
 </target>
```

Examine run targets like `run-createcomponent` in `build.xml` for examples for creating ant run targets for running your component. Perhaps yours will look something like this:

```xml
<target name='run-ekfcomponent' depends='ekf' description='Runs Jims terrible, terrible ekf'>
   <run-component component='com.ekf.JimsEKFComponent'/>
 </target>
```

Following the above templates would allow you to compile your code by running `./ant ekf` and to run your code by running `./ant run-ekfcomponent`. Note that running code compiles all code that depends on it, as specified by the `depends` field.