package com.adesim.gui;

import com.adesim.gui.customKeyListener.CustomKeyListener;
import com.adesim.gui.customKeyListener.CustomKeyProcessor;

public class RobotKeyListener implements CustomKeyListener {
	ADESimMapVis vis;
	
	/** creates key listener and adds itself to visualization's listeners */
    public RobotKeyListener(ADESimMapVis vis) {
    	this.vis = vis;
    	new CustomKeyProcessor(vis, this);
    }
    
	@Override
	public void keyPressed(int keyCode) {
		if (vis.robotKeyboardListenerOverrides.size() > 0) {
    		return;
    	}
    	
    	String selectedRobot = vis.getSelectedRobot();
    	if (selectedRobot == null) {
    		return;
    	}
    	
    	try {
			vis.callComponent("keyPressed", keyCode, selectedRobot);
		} catch (Exception e) {
			vis.showErrorMessage("Could not communicate with the server to inform it " + 
					"of the appropriate action for the key press!", "Could not communicate with server!");
		}
	}

	@Override
	public void keyReleased(int keyCode) {
		if (vis.robotKeyboardListenerOverrides.size() > 0) {
    		return;
    	}
    	
    	String selectedRobot = vis.getSelectedRobot();
    	if (selectedRobot == null)
    		return;
    	
    	try {
			vis.callComponent("keyReleased", keyCode, selectedRobot);
		} catch (Exception e) {
			vis.showErrorMessage("Could not communicate with the server to inform it " + 
					"of the appropriate action for the key release!", "Could not communicate with server!");
		}
	}
}
