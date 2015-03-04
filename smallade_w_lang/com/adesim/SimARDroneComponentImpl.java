package com.adesim;

import java.awt.event.KeyEvent;
import java.rmi.RemoteException;

import ade.gui.ADEGuiVisualizationSpecs;

import com.adesim.datastructures.CameraSpecs;
import com.adesim.datastructures.MountingPoint;
import com.adesim.datastructures.Point3D;
import com.adesim.gui.vis3D.ADESim3DCameraVisualization;
import com.adesim.robot.SimARDroneRobot;
import com.adesim.robot.SimAbstractRobot;

public class SimARDroneComponentImpl extends ADESimActorComponentImpl implements SimARDroneComponent {
	private static final long serialVersionUID = 1L;

	// for now, supports only one camera, facing forward.
	private static final CameraSpecs cameraSpecs = new CameraSpecs(
			new MountingPoint(new Point3D(0.20, 0, 0.05), 0),
			93, 10.0);
	//      * mounting point 5 cm above ground, and 20 cm off of center (since can have different
	//        shell size, but camera stays in place, measure from center rather from external offset)
        //      * PWS: height is broken, see com/adesim/robot/SimAbstractRobot.java
	//      * field of vision 93 degrees, as per a review I found online
	//        (http://www.lecun.org/blog/index.php?entry=entry100107-025557), FYI 640x480.
	//      * FYI, down-facing camera (when add one) is 176x144 resolution, 63 degree field of view,
	//        as per same website)
	//      * maximum distance that can see anything at all, 10 meters (essentially a made-up number
	//           that may need to be adjusted experimentally, though in any "real" environment
	//           it shouldn't matter anyhow, as there will be obstacles in the way anyway).
	//           I figured that, given the resolution, it's probably reasonably true...
	//           This number specifies the maximum length for the perception rays, and corresponds
	//           roughly to how well a robot would be able to distinguish something at a great distance.


	private boolean wearingExternalShell = true; // flag if should use shell dimensions
	//    true by default, can be turned off with command-line "-noShell"

	public SimARDroneComponentImpl() throws RemoteException {
		super();
	}

	@Override
	public SimAbstractRobot createRobot(boolean noisyOdometry) {
		return new SimARDroneRobot(model, getActorName(), noisyOdometry, cameraSpecs, wearingExternalShell);
	}

	@Override
    public ADEGuiVisualizationSpecs getVisualizationSpecs()	throws RemoteException {
    	ADEGuiVisualizationSpecs specs = super.getVisualizationSpecs();
		specs.add("Camera", ADESim3DCameraVisualization.class);
		return specs;
	}

	@Override
	protected void appendActorSpecificUsageInfo(StringBuilder sb) {
		sb.append("  -noShell           <set dimensions as if NOT wearing the protective external shell>\n");
	}

	@Override
	protected int checkIfRecognizeArgument(String[] args, int i) {
		if (args[i].equalsIgnoreCase("-noShell")) {
			this.wearingExternalShell = false;
			return 0; // recognized, but not need to advance anything.
		} else {
			// did not recognize anything:
			return -1;
		}
	}


	@Override
	protected double getDefaultAcceleration() {
		return 0.75; // higher means quicker stopping
	}

	@Override
	protected double getDefaultTV() {
		return 1.0; // meters/sec
	}

	@Override
	protected double getDefaultRV() {
		return Math.PI / 4.0; // rads/sec
	}




	@Override
	public String getHelpText() throws RemoteException {
		return "* Up arrow: move robot forward at default speed." +
		       "\n* Down arrow:  move robot backwards at default speed." +
		       "\n* Left arrow:  rotate robot to the left at default speed." +
		       "\n* Right arrow:  rotate robot to the right at default speed." +
		       "\n* Page up:  increase the quadrotor's goal hover height." +
		       "\n* Page down:  decrease the quadrotor's goal hover height.";
	}


	@Override
	public void keyPressed(int keyCode) throws RemoteException {
		if (keyCode == KeyEvent.VK_UP) {
			this.setTV(this.getDefaultTV());
		} else if (keyCode == KeyEvent.VK_DOWN) {
			this.setTV(-1 * this.getDefaultTV());
		} else if (keyCode == KeyEvent.VK_LEFT) {
			this.setRV(this.getDefaultRV());
	    } else if (keyCode == KeyEvent.VK_RIGHT) {
	    	this.setRV(-1 * this.getDefaultRV());
	    } else if (keyCode == KeyEvent.VK_PAGE_UP) {
	    	SimARDroneRobot arDrone = ((SimARDroneRobot) this.model.robot);
	    	arDrone.setGoalHoverHeight(arDrone.getGoalHoverHeight() + 0.02);
	    			// this will happen fast, so make small increments!
	    } else if (keyCode == KeyEvent.VK_PAGE_DOWN) {
	    	SimARDroneRobot arDrone = ((SimARDroneRobot) this.model.robot);
	    	arDrone.setGoalHoverHeight(arDrone.getGoalHoverHeight()- 0.02);
	    			// this will happen fast, so make small increments!)
	    } else {
        	// if it's none of those, there's a good chance it has nothing to do with the RobotKeyListener,
	    	//     and consequently with this call.  do nothing.
	    }
	}

	@Override
	public void keyReleased(int keyCode) throws RemoteException {
		if (keyCode == KeyEvent.VK_UP) {
			this.setTV(0);
		} else if (keyCode == KeyEvent.VK_DOWN) {
			this.setTV(0);
		} else if (keyCode == KeyEvent.VK_LEFT) {
			this.setRV(0);
	    } else if (keyCode == KeyEvent.VK_RIGHT) {
	    	this.setRV(0);
	    } else {
        	// if it's none of those, there's a good chance it has nothing to do with the RobotKeyListener,
	    	//     and consequently with this call.  do nothing.
	    }
	}

	@Override
	public void setHoverHeight(double height) throws RemoteException {
		SimARDroneRobot arDrone = ((SimARDroneRobot) this.model.robot);
    	arDrone.setGoalHoverHeight(height);
	}

	@Override
	public double getHoverHeight() throws RemoteException {
		SimARDroneRobot arDrone = ((SimARDroneRobot) this.model.robot);
    	return arDrone.getGoalHoverHeight();
	}
}
