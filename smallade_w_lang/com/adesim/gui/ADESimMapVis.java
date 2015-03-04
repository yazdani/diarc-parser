package com.adesim.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import ade.gui.ADEGuiCallHelper;
import ade.gui.ADEGuiPanel;
import ade.gui.Util;

import com.adesim.commands.ActorCommand;
import com.adesim.datastructures.DoubleDimension;
import com.adesim.gui.datastructures.RobotVisualizationData;
import com.adesim.gui.datastructures.SimMapVisUpdatePackage;
import com.adesim.gui.images.GuiImageHelper;
import com.adesim.gui.vis3D.ADESim3DView;
import com.adesim.objects.model.ModelOwner;
import com.adesim.objects.model.SimModel;
import com.adesim.util.SimUtil;


public class ADESimMapVis extends ADEGuiPanel {
	private static final long serialVersionUID = 1L;
	
	private static final Dimension EXTERNAL_WINDOW_STARTUP_DIMENSION = new Dimension(1000, 700);
	private static final Dimension INTERNAL_WINDOW_STARTUP_DIMENSION = 
			SimUtil.scaleDimension(EXTERNAL_WINDOW_STARTUP_DIMENSION, 0.5);
	
	public boolean readyToRefreshAgain = true; // a flag by which the visualization's panel notifies the vis
	//    that it's done painting; otherwise, might try to get new data while still painting, causing 
	//    inconsistencies, or an impossible-to-catch-up loop, or both...
	
	public SimModel model;
	public int tickCount = -1; // start off with an invalid tick count to ensure that 
	//     immediately receive data for a brand new model.
	
    public ArrayList<RobotVisualizationData> robotsVisualizationDataFromComponent; // note that for a "full" environmental
    //     simulation, this will be all the robots; for a simulation of a single robot, it will be just 
    //     the SINGLE robot in the array.
    public ADESimMapVisualizationType visType;
    
    private SimPanel simPanel;
    private JPanel bottomPanel;
    private JLabel statusLabel;
    
    public SimMenuBar menuBar; // public so that components can view menu options
    
    public RobotKeyListener robotKeyListener;
    
    // hashset of items that are taking control AWAY from the standard
    //     robot key listener
    public HashSet<Object> robotKeyboardListenerOverrides = new HashSet<Object>();
    public HashSet<Object> mouseListenerOverriders = new HashSet<Object>();
    
    public HashSet<ADESim3DView> threeDviews = new HashSet<ADESim3DView>();
    
    
    private RobotSelector robotSelector;
    private String selectedRobotID; // Separate from robotSelector.  RobotSelector can UPDATE this field,
    //   but this is something the visualization keeps cached for itself (that way, for single-robot
    //   visualizations, don't need a robot selector at all!)
    
    // count other keyboard listeners that take precedence over the RobotKeyListener (for example,
    //    keyboard listeners for wall-creation, block-creation, etc wizards).  The RobotKeyListener
    //    will only respond if that number is 0!

    
    public ADESimMapVis(ADEGuiCallHelper guiCallHelper, ADESimMapVisualizationType visType)   {
    	super(guiCallHelper, 10); // 100ms = update 10x/second
		initContentPane();
		
		refreshGui(); // refresh gui here in the constructor, so that can create model,
		//   and consequently size the window proportionally properly.
		
		robotKeyListener = new RobotKeyListener(this);    
    }

	public SimPanel getSimPanel() {
		return simPanel;
	}
    
    public SimMenuBar getMenuBarSim() {
    	return menuBar;
    }


	private void initContentPane() {
        this.setLayout(new BorderLayout());
        JMenuBar menuBar = createMenuBar();
        this.add(menuBar, BorderLayout.NORTH);
        simPanel = new SimPanel(this);

        this.add(simPanel, BorderLayout.CENTER);
        this.add(createBottomPanel(), BorderLayout.SOUTH);
    }


	private JMenuBar createMenuBar() {
		menuBar = new SimMenuBar(this);
		return menuBar;
	}


	private JPanel createBottomPanel() {
    	bottomPanel = new JPanel();
    	bottomPanel.setLayout(new BorderLayout());

    	statusLabel = new JLabel();
    	setStatusLabelText("Welcome to ADESim!"); // need to set it to some text so it shows up
        //    and does not mess up the layout later.
    	bottomPanel.add(statusLabel, BorderLayout.WEST);
    	
    	JPanel bottomRightPanel = new JPanel(new BorderLayout());
    	
    	if (visType == ADESimMapVisualizationType.FULL_ENVIRONMENT) {
	    	robotSelector = new RobotSelector(this);
	    	bottomRightPanel.add(robotSelector, BorderLayout.WEST);
	    	// set minimum height of the panel to that of the robot selector
	    	//   that way, won't flicker when the robot selector becomes visible when 
	    	//   the first robot joins.
	    	bottomPanel.setPreferredSize(new Dimension(400, robotSelector.getPreferredSize().height));
    	}
    	
    	JButton helpButton = new JButton(GuiImageHelper.helpIcon);
    	if (robotSelector != null) {
    		helpButton.setPreferredSize(new Dimension(
    				robotSelector.getPreferredSize().height, robotSelector.getPreferredSize().height));
    	}
    	helpButton.setFocusable(false); // for reasons known only to Swing, if the help button
    	//    is focusable, it steals the ability to control the robots via the keyboard!!!
    	bottomRightPanel.add(helpButton, BorderLayout.EAST);
    	helpButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String selectedRobot = getSelectedRobot();
		    	if (selectedRobot == null) {
		    		return;
		    	}
		    	
		    	String helpText;
				try {
					helpText = (String) callComponent("getHelpText", selectedRobot);
			    	showInfoMessage(helpText, "Instructions for controlling robot " + 
							Util.quotational(selectedRobot));
				} catch (Exception e1) {
					showErrorMessage("No help available -- you're on your own!",
							"Could not contact server for help");
				}
			}
		});
    	
    	bottomPanel.add(bottomRightPanel, BorderLayout.EAST);
    	
    	return bottomPanel;
	}


	@Override
    public void refreshGui() {
		if (!readyToRefreshAgain) {
			return;
		}
			
		try {
			SimMapVisUpdatePackage updatePackage = (SimMapVisUpdatePackage) 
			callComponent("getSimMapVisUpdatePackage",	tickCount,
					menuBar.viewMenuShowLasers.isSelected(), 
					menuBar.viewMenuShowActivatedObstacleSensors.isSelected(), 
					menuBar.viewMenuShowPerceivedObjects.isSelected());

			
			this.robotsVisualizationDataFromComponent = updatePackage.robotVisData;
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
						//System.out.println("command " + eachCommand.toString()); // (good for debugging) 
						eachCommand.execute(model);
					}
				}
			}
			
			
			// having gotten the new data, repaint:
			simPanel.repaint();
			
			synchronized (threeDviews) {
				for (ADESim3DView each : threeDviews) {
					each.updateView();
				}
			}


			// if the combo box for selecting robots is null (ie: single robot visualization),
			//     then just select the first (and only) robot.
			if (robotSelector == null) {
				try {
					setSelectedRobot(robotsVisualizationDataFromComponent.get(0).ID);
				} catch (Exception e) {
					// if robotsVisualizationDataFromComponent is null or contains no elements, oh well.
					//    I guess there's no selection
				}
			} else { // otherwise, if robot selector does exist, update its listing:
				robotSelector.updateRobotListings(robotsVisualizationDataFromComponent);
			}

		} catch (Exception e) {
			System.err.println("Could not communicate with the server " +
					"to get updated GUI information.  " +
			"\nVisualization is essentially frozen for the moment.");
			//System.out.println("Printing stack trace:");
			//e.printStackTrace();
		}
    }

	public void setStatusLabelText(String text) {
		statusLabel.setText(text);
	}
	
    public void setStatusLabelMousePoint(Point mousePoint) {
        Point2D worldPoint = worldPointFromVisCoordinates (mousePoint);
        setStatusLabelWorldPoint(worldPoint);
    }

    public void setStatusLabelWorldPoint(Point2D worldPoint) {
        String coordinateText;
        if (worldPoint == null) {// invalid (out of bounds) point
        	coordinateText = "N/A"; // not null, otherwise label will disappear
        } else { 
        	coordinateText = SimUtil.getPointCoordinatePair(worldPoint);
        }
        
        statusLabel.setText("Mouse world coordinates = " + coordinateText);
	}   
   


	public Point2D worldPointFromVisCoordinates (Point2D visPoint) {
    	Dimension proportionInvariantDimension = simPanel.visualizationDimension;
    	
    	if (   (proportionInvariantDimension == null) || (visPoint == null)   )
    		return null;
    	
    	if (    (visPoint.getX() > proportionInvariantDimension.width) ||
    			(visPoint.getY() > proportionInvariantDimension.height)     )
    		return null;
    	
    	
    	// if haven't quit, proceed:
    	
        double visPointProportionalXFromBottomLeft = visPoint.getX() / 
        						((double)proportionInvariantDimension.width);
        
        // y is a little trickier, because in screen it starts at top left
        double visPointProportionalYFromBottomLeft =
                    (proportionInvariantDimension.height - visPoint.getY()) / 
                    		((double)proportionInvariantDimension.height);

        Point2D worldMin = model.worldBounds.getMin();
        Dimension2D worldDim = model.worldBounds.getBoundingDimension();

        return new Point2D.Double (
                worldMin.getX() + worldDim.getWidth() * visPointProportionalXFromBottomLeft,
                worldMin.getY() + worldDim.getHeight() * visPointProportionalYFromBottomLeft);
    }
    
    public double worldSizeFromVisSize(double dimension) {
    	Dimension proportionInvariantDimension = simPanel.visualizationDimension;
    	
    	if (proportionInvariantDimension == null) {
    		System.err.println("Could not obtain panel dimension");
    		return 0;
    	}
    	
    	// using x axis (width) -- but doesn't matter which one, since units are same size both
    	//     in x and y direction.
    	double visProportionalSize = dimension / proportionInvariantDimension.getWidth();
    	
    	return model.worldBounds.getBoundingDimension().getWidth() * visProportionalSize;
    }

	public void setSelectedRobot(String robotID) {
		selectedRobotID = robotID;
		
		// if combo box is initialized (it wouldn't be in single-robot simulation)
		if (robotSelector != null) {
			robotSelector.setSelectedItem(robotID);
		}
	}

	public String getSelectedRobot() {
		return selectedRobotID;
	}

	@Override
	public Dimension getInitSize(boolean isInternalWindow) {
		if (isInternalWindow) {
			return getProportionateSize(INTERNAL_WINDOW_STARTUP_DIMENSION);
		} else {
			return getProportionateSize(EXTERNAL_WINDOW_STARTUP_DIMENSION);
		}
	}
	
	private Dimension getProportionateSize(Dimension windowSize) {
		int topAndBottomOutsidePanelHeight = menuBar.getPreferredSize().height + 
												bottomPanel.getPreferredSize().height;
		
		Dimension panelSize = new Dimension(windowSize.width, 
					windowSize.height - topAndBottomOutsidePanelHeight);
		
		DoubleDimension worldDim = model.worldBounds.getBoundingDimension();
		
		double windowYtoXproportion = panelSize.height / (double)(panelSize.width);
		double worldYtoXproportion = (double)(worldDim.height / worldDim.width);
		
		int newWidth;
		int newHeight;
		if (windowYtoXproportion > worldYtoXproportion) {
			newWidth = panelSize.width;
			newHeight = (int)(newWidth * worldYtoXproportion) + topAndBottomOutsidePanelHeight;
		} else {
			newHeight = panelSize.height;
			newWidth = (int) ( (newHeight-topAndBottomOutsidePanelHeight) / (double)worldYtoXproportion );
		}

		return new Dimension(newWidth, newHeight);
	}
	
	public double getModelPointDiff(double diff) {
		return worldPointFromVisCoordinates(new Point2D.Double(diff,0)).getX() - 
			   worldPointFromVisCoordinates(new Point(0,0)).getX();
	}

	
	public void showErrorMessage(Object message, String title) {
		JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
	}

	public void showInfoMessage(String message, String title) {
    	message = Util.breakStringIntoMultilineString(message);
		JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);

	}
}
