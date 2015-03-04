package com.adesim.sample;


import java.rmi.RemoteException;
import java.util.Vector;

import ade.gui.ADEGuiVisualizationSpecs;

import com.ADEPercept;
import com.action.ActionComponentImpl;

public class SampleActionComponentImpl extends ActionComponentImpl implements SampleActionComponent {	
	private static final long serialVersionUID = 1L;
	

	boolean verbose = false;
	boolean getPercepts = false;
	boolean getBeacons = false;
	
	boolean readyToRunArchitecture = false;
	
	
	/** 
	 * <code>runArchitecture</code> is called periodically to perform
	 * whatever sensing and acting required by the architecture.
	 */
	public void runArchitecture()
	{
		if (!readyToRunArchitecture) {
			return;
		}
		
		
		// here's an example of fetching percepts: (if getPercepts is set to true above)
		if (getPercepts) {
			try {
				System.out.println("Getting box and robot percepts");
				String[] query = {"box", "robot"};
				@SuppressWarnings("unchecked")
				Vector<ADEPercept> percepts = (Vector<ADEPercept>) callMethod("getPercepts", (Object)query);
				for (ADEPercept each : percepts) {
					System.out.println(each);
				}
			} catch (Exception e) {
				System.out.println("Could not get percepts");
			}
		}
		
		
		boolean[] obsts = checkObstacles();
		
		if (verbose)
			System.out.println ("Sensor readings:  right = " + obsts[0] + 
							", front = " + obsts[1] + 
							", left = " + obsts[2]);
		
		if (getBeacons) {
			Double[][] beaconData = getBeaconData();
			if (verbose) {
				System.out.println("ORIENTATIONS for the " + beaconData.length + " beacons:");
				for (int row = 0; row < beaconData.length; row++) {
					System.out.println("angle = " + beaconData[row][0] + ".  dist = " + beaconData[row][1]);
				}
			}
		}

		
		// My initial idea with the navigation was fairly simple:  
		//   if the robot saw that it is clear on the front and there's a wall 
		//   on the right, the robot should would straight; otherwise, it would
		//   turn left.  The problem with this scheme was that up until
		//   the robot was close enough to something, it saw nothing.  
		//   More troubling still was the robot's tendency -- upon turning 
		//   some 30-40 degrees, to STOP seeing that there is an obstacle on
		//   the front, yet to bump into it if it were told to go straight.
		

		// So really, the only TRUE check that the robot can forge ahead
		//    is to see whether ALL sensors show no obstacle.  
		if (! (obsts[0] || obsts[1] || obsts[2]))
		{
			goStraight();
			if (verbose) System.out.println("...... all clear, go straight!");
		}
		
		// if all is NOT clear:
		else
		{
			if (obsts[0]) // if obstacle on the right, turn left
						  //    this is the default action even if
						  //    both the left and the right have obstacles
			{
				if (verbose) System.out.println("...... ostacle on the right, turning left");
				turnLeft();
			}
			else // else, must be on the left
			{
				if (verbose) System.out.println("...... ostacle on the left, turning right");
				turnRight();
			}
		}
	}

	/** 
	 * Constructs the ActionComponentArch.
	 */
	public SampleActionComponentImpl() throws RemoteException
	{
		super();
		
		new Thread() {
			public void run() {
				
				// wait until local services are ready, and until gets default velocities below 
				//    (At which point, will set the readyTorunArchitecture flag).
				while (!localServicesReady() || (!readyToRunArchitecture)) {
					try {
						sleep(100);
					} catch (InterruptedException e) {
						// can't sleep, busy-wait
					}
					
					try {
						double[] defVels = (double[]) callMethod("getDefaultVels");
						defTV = defVels[0];
						defRV = defVels[1];
						readyToRunArchitecture = true;
					} catch (Exception e) {
						// do nothing, just wait
					}
				}
				
				System.out.println("Default velocities obtained, SampleActionComponent starting to run!");
				
			};
		}.start();
		
	}

	
    @Override
    public ADEGuiVisualizationSpecs getVisualizationSpecs()	throws RemoteException {
    	ADEGuiVisualizationSpecs specs = super.getVisualizationSpecs();
    	specs.add("Direction_&_Orientation", ActionComponentVis.class);
    	return specs;
    }
    
    @Override
    public ActionComponentVisData getVisualizationData() throws RemoteException {
    	ActionComponentVisData data = new ActionComponentVisData();
    	
    	data.obstacles = checkObstacles();
    	data.orientation = getOrientation();
    	
    	return data;
    }
}
