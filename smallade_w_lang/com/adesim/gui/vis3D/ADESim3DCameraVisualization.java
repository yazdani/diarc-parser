package com.adesim.gui.vis3D;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import ade.gui.ADEGuiCallHelper;
import ade.gui.ADEGuiPanel;
import ade.gui.Util;

import com.adesim.commands.ActorCommand;
import com.adesim.gui.customKeyListener.CustomKeyListener;
import com.adesim.gui.customKeyListener.CustomKeyProcessor;
import com.adesim.gui.datastructures.Sim3DVisUpdatePackage;
import com.adesim.objects.model.ModelOwner;
import com.adesim.objects.model.SimModel;
import java.awt.event.KeyEvent;


public class ADESim3DCameraVisualization extends ADEGuiPanel implements CustomKeyListener {
	private static final long serialVersionUID = 1L;
	private static final Dimension DEFAULT_WINDOW_SIZE = new Dimension(640, 480);

	public SimModel model;
	public int tickCount = -1; // initialize to invalid tick count to ensure that model
	//     gets created on startup

	private ADESim3DVisInnerPanel visPanel;

	private String imgTemp;
	private String imgFinal;
	private String timeTemp;
	private String timeFinal;



    public ADESim3DCameraVisualization(ADEGuiCallHelper guiCallHelper) {
		super(guiCallHelper, 33); // 100ms = update 10x/second
		visPanel = new ADESim3DVisInnerPanel(false);

		imgTemp = "/dev/shm/tmp.jpg";
		imgFinal = "/dev/shm/final.jpg";
		timeTemp = "/dev/shm/tmp.time";
		timeFinal = "/dev/shm/final.time";

		if(!(new File("/dev/shm")).exists()) {
			imgTemp = imgFinal = timeTemp = timeFinal = null;
		}

		// Build content.
		initContentPane();

		new CustomKeyProcessor(visPanel.panel3D, this);
	}

	private void initContentPane() {
		this.setLayout(new BorderLayout());
		this.add(visPanel, BorderLayout.CENTER);
	}




	@Override
	public void refreshGui() {
		Sim3DVisUpdatePackage updatePackage;
		try {
			updatePackage = (Sim3DVisUpdatePackage) callComponent("getSim3DVisUpdatePackage", tickCount);
		} catch (Exception e) {
			System.err.println("Could not communicate with the server " +
					"to get updated GUI information.  " +
			"\nVisualization is essentially frozen for the moment.  ");
			//System.out.println("Printing stack trace:");
			//e.printStackTrace();
			return;
		}



		// STEP 1:  update the world objects
		this.setTitle(updatePackage.title);
		this.tickCount = updatePackage.tickCount;


		// if the update requires a full re-creation of the model:
		if (updatePackage.recreateModel) {
			//System.out.println("Visualization re-creating model");
			ModelOwner guiModelOwner = new ModelOwner() {
				@Override
				public OwnerType getOwnerType() {
					return OwnerType.GUI;
				}
				@Override
				public SimModel getModel() {
					return model;
				}
			};

			this.model = new SimModel(guiModelOwner,
					updatePackage.worldBounds, updatePackage.worldObjects,
					updatePackage.otherRobotShapes);
		} else {
			// catch up on "ticking" the model for any ticks between the "next one after the old tick count"
			//     and the "new tick count (inclusive)"
			for (int tempTick = this.tickCount + 1; tempTick <= updatePackage.tickCount; tempTick++) {
				model.tick(tempTick, false); // allowRobotMotion flag is irrelevant, as the GUI model
				//    has no robot.  But may as well pass it "false"
			}

			// the ACTUAL history and the updates that need to apply:
			ArrayList<ArrayList<ActorCommand>> commandsToCatchUpOn = updatePackage.updateHistory;
			for (ArrayList<ActorCommand> eachSetOfCommands : commandsToCatchUpOn) {
				for (ActorCommand eachCommand : eachSetOfCommands) {
					// good for debugging:  System.out.println("command " + eachCommand.toString());
					eachCommand.execute(model);
				}
			}
		}


		// having gotten the new data, reload map:
		visPanel.reloadObjects(model, this.getComponentName());


		//STEP 2:  update camera location

		visPanel.setCameraLocation(updatePackage.cameraData.location.x,
				updatePackage.cameraData.location.y,
				updatePackage.cameraData.location.z,
				updatePackage.cameraData.theta);
		// note that not touching panel3D's tx, so that can let user drag mouse to look up/down
		// TODO:  take into account camera field of view?



		// STEP 3:  repaint
		visPanel.setView();


		if(imgTemp != null) {
			try {
				if(imgTemp != null) {
					BufferedImage writeImg = visPanel.getImage(320,240);
					ImageIO.write(writeImg,"jpg",new File(imgTemp));
					(new File(imgTemp)).renameTo(new File(imgFinal));


					Calendar now = Calendar.getInstance();

					BufferedWriter writeTime = new BufferedWriter(new FileWriter(new File(timeTemp)));
					writeTime.write(now.getTimeInMillis() + "");
					writeTime.close();
					(new File(timeTemp)).renameTo(new File(timeFinal));

				}
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}


	@Override
	public Dimension getInitSize(boolean isInternalWindow) {
		return DEFAULT_WINDOW_SIZE;
	}

	@Override
	public void keyPressed(int keyCode) {
		// PWS: disabling keyboard robot motion; this is a camera visualization...
		if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT)
			return;
		try {
			callComponent("keyPressed", keyCode, this.getComponentName());
		} catch (Exception e) {
			showErrorMessage("Could not communicate with the server to inform it " +
					"of the appropriate action for the key press!", "Could not communicate with server!");
		}
	}

	@Override
	public void keyReleased(int keyCode) {
		try {
			callComponent("keyReleased", keyCode, this.getComponentName());
		} catch (Exception e) {
			showErrorMessage("Could not communicate with the server to inform it " +
					"of the appropriate action for the key release!", "Could not communicate with server!");
		}
	}



	public void showErrorMessage(String message, String title) {
		message = Util.breakStringIntoMultilineString(message);
		System.err.println(title);
		System.err.println(message);
		JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
	}

	public void showInfoMessage(String message, String title) {
    	message = Util.breakStringIntoMultilineString(message);
		JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);

	}

}
