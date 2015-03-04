package com.adesim.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ade.gui.icons.IconFetcher;

import com.adesim.gui.vis3D.ADESim3DView;
import com.adesim.gui.wizard.CreateNewEnvironmentWizard;
import com.adesim.gui.wizard.newObject.CreateBlockWizard;
import com.adesim.gui.wizard.newObject.CreateBoxWizard;
import com.adesim.gui.wizard.newObject.CreateDoorWizard;
import com.adesim.gui.wizard.newObject.CreateLandmarkWizard;
import com.adesim.gui.wizard.newObject.CreateWallWizard;

public class SimMenuBar extends JMenuBar {
	private static final long serialVersionUID = 1L;

	public JCheckBoxMenuItem viewMenuShowLasers;
	public JCheckBoxMenuItem viewMenuShowActivatedObstacleSensors;
	public JCheckBoxMenuItem viewMenuShowPerceivedObjects;
	public JCheckBoxMenuItem viewMenuShowTooltips;
	
	public JCheckBoxMenuItem editMenuAllowEditBlocks;
	public JCheckBoxMenuItem editMenuAllowEditBoxes;
	public JCheckBoxMenuItem editMenuAllowEditDoors;
	public JCheckBoxMenuItem editMenuAllowEditLandmarks;
	public JCheckBoxMenuItem editMenuAllowEditWalls;
	
	
	private ADESimMapVis vis;

	
	public SimMenuBar(ADESimMapVis vis) {
		this.vis = vis;
		createSimMenu();
		createViewMenu();
		create3DMenu();
		createEditMenu();
		createNewItemMenu();
	}

	private void create3DMenu() {
		JMenu threeDmenu = new JMenu("3D");
		this.add(threeDmenu);
		
		JMenuItem create3Dview = new JMenuItem("Create 3D view");
		create3Dview.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Point2D worldCenter = vis.model.worldBounds.getCenter();
				double randomAngle = new Random().nextDouble() * 2 * Math.PI;
				new ADESim3DView(vis, worldCenter, randomAngle).setVisible(true);
			}
		});
		
		threeDmenu.add(create3Dview);
	}

	private void createEditMenu() {
		CheckBoxMenu editMenu = new CheckBoxMenu("Edit", "getEditPresetFlags");
		this.add(editMenu);
		
		editMenuAllowEditBlocks = new JCheckBoxMenuItem("Allow editing of Blocks", true);
		editMenu.add(editMenuAllowEditBlocks);
		
		editMenuAllowEditBoxes = new JCheckBoxMenuItem("Allow editing of Boxes", true);
		editMenu.add(editMenuAllowEditBoxes);
		
		editMenuAllowEditDoors = new JCheckBoxMenuItem("Allow editing of Doors", false);
		editMenu.add(editMenuAllowEditDoors);
		
		editMenuAllowEditLandmarks = new JCheckBoxMenuItem("Allow editing of Landmarks", false);
		editMenu.add(editMenuAllowEditLandmarks);
		
		editMenuAllowEditWalls = new JCheckBoxMenuItem("Allow editing of Walls", false);
		editMenu.add(editMenuAllowEditWalls);
	}

	private void createNewItemMenu() {
		JMenu simMenu = new JMenu("New");
		this.add(simMenu);
		
		JMenuItem newBlockItem = new JMenuItem("New block");
		simMenu.add(newBlockItem);
		newBlockItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new CreateBlockWizard(vis);
			}
		});
		
		JMenuItem newBoxItem = new JMenuItem("New box");
		simMenu.add(newBoxItem);
		newBoxItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new CreateBoxWizard(vis).setVisible(true);
			}
		});

		JMenuItem newDoorItem = new JMenuItem("New door");
		simMenu.add(newDoorItem);
		newDoorItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new CreateDoorWizard(vis).setVisible(true);
			}
		});
		
		JMenuItem newLandmarkItem = new JMenuItem("New landmark");
		simMenu.add(newLandmarkItem);
		newLandmarkItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new CreateLandmarkWizard(vis).setVisible(true);
			}
		});
		
		JMenuItem newWallItem = new JMenuItem("New wall");
		simMenu.add(newWallItem);
		newWallItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new CreateWallWizard(vis).setVisible(true);
			}
		});
	}

	private void createSimMenu() {
		JMenu simMenu = new JMenu("Sim");
		this.add(simMenu);
		
		JMenuItem saveItem = new JMenuItem("Save current state", 
				IconFetcher.get16x16icon("media-floppy.png"));
		saveItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new ConfigSaver(vis);
			}
		});
		simMenu.add(saveItem);
		
		
		simMenu.addSeparator();
		
		
		JMenuItem createNewEnvironmentItem = new JMenuItem("Create new environment",
				IconFetcher.get16x16icon("window-new.png"));
		createNewEnvironmentItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new CreateNewEnvironmentWizard(vis).setVisible(true);
			}
		});
		simMenu.add(createNewEnvironmentItem);
		
	}

	private void createViewMenu() {
		CheckBoxMenu viewMenu = new CheckBoxMenu("View", "getViewPresetFlags");
		this.add(viewMenu);
		
		
		viewMenuShowTooltips = new JCheckBoxMenuItem("Show tooltips", true);
		viewMenuShowTooltips.setToolTipText("Will show tooltips when mousing over robot, laser lines, labels, etc");
		viewMenu.add(viewMenuShowTooltips);
		
		
		
		ActionListener refreshAllRobotsActionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				vis.refreshGui();
			}
		}; 
		
		
		viewMenuShowLasers = new JCheckBoxMenuItem("Show laser lines", false);
    	// on checking/unchecking, ensure immediate repaint (particularly even if paused)
		viewMenuShowLasers.addActionListener(refreshAllRobotsActionListener);
		viewMenu.add(viewMenuShowLasers);
		
		viewMenuShowActivatedObstacleSensors = new JCheckBoxMenuItem("Show activated obstacle sensors", false);
		viewMenuShowActivatedObstacleSensors.addActionListener(refreshAllRobotsActionListener);
		viewMenu.add(viewMenuShowActivatedObstacleSensors);
		
		viewMenuShowPerceivedObjects = new JCheckBoxMenuItem("Show perceived objects", false);
		viewMenuShowPerceivedObjects.addActionListener(refreshAllRobotsActionListener);
		viewMenu.add(viewMenuShowPerceivedObjects);
		
		
		viewMenu.addSeparator();
		viewMenu.add(createSetBackgroundImageSubMenu());
	}

	
	
	private JMenuItem createSetBackgroundImageSubMenu() {
		JMenu subMenu = new JMenu("Choose background image");
		
		
		JMenuItem backgroundMenuItem = new JMenuItem("Choose image..."); 
		backgroundMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser imageChooser = new JFileChooser(vis.getSimPanel().
						backgroundImageManager.getFileChooserInitDirectory());
				int returnVal = imageChooser.showOpenDialog(vis);

		        if (returnVal == JFileChooser.APPROVE_OPTION) {
	        		vis.getSimPanel().backgroundImageManager.setBackgroundImage(
	        				imageChooser.getSelectedFile());
		        }
			}
		});
		subMenu.add(backgroundMenuItem);
		
		
		JMenuItem clearBackgroundMenuItem = new JMenuItem("Clear image"); 
		clearBackgroundMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				vis.getSimPanel().backgroundImageManager.setBackgroundImage(null);
			}
		});
		subMenu.add(clearBackgroundMenuItem);
		
		subMenu.addSeparator();

		JPanel backgroundTransparencyMenuPanel = new JPanel(new BorderLayout());
		backgroundTransparencyMenuPanel.add(new JLabel("Transparency"), BorderLayout.WEST);
		backgroundTransparencyMenuPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 4));
		final JSlider transparencySlider = new JSlider(0, 100, 
				(int)(BackgroundImageManager.DEFAULT_BACKGROUND_TRANSPARENCY * 100));
		transparencySlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				vis.getSimPanel().backgroundImageManager.setBackgroundImageTransparency(
						(transparencySlider.getValue() / (float)transparencySlider.getMaximum()));
			}
		});
		backgroundTransparencyMenuPanel.add(transparencySlider, BorderLayout.CENTER);
		subMenu.add(backgroundTransparencyMenuPanel);
		
		subMenu.addSeparator();
		
		
		JPanel displayImageInBackOrInFrontPanel = new JPanel(new GridLayout(3, 1));
		displayImageInBackOrInFrontPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 4));
		displayImageInBackOrInFrontPanel.add(new JLabel("Paint image:"));
		ButtonGroup radioButtonGroup = new ButtonGroup();
		
		{
			final JRadioButton radioButtonInBack = new JRadioButton("Behind everything else", 
					BackgroundImageManager.DEFAULT_PAINT_BEFORE_THE_REST_OF_THE_PANEL);
			radioButtonInBack.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					vis.getSimPanel().backgroundImageManager.setPaintBeforeTheRestOfThePanel(
							radioButtonInBack.isSelected());
				}
			});
			radioButtonGroup.add(radioButtonInBack);
			displayImageInBackOrInFrontPanel.add(radioButtonInBack);
		}
		{
			final JRadioButton radioButtonInFront = new JRadioButton("In front of everything else", 
					(! (BackgroundImageManager.DEFAULT_PAINT_BEFORE_THE_REST_OF_THE_PANEL) ));
			radioButtonInFront.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					vis.getSimPanel().backgroundImageManager.setPaintBeforeTheRestOfThePanel(
							(! (radioButtonInFront.isSelected()) ));
				}
			});
			radioButtonGroup.add(radioButtonInFront);
			displayImageInBackOrInFrontPanel.add(radioButtonInFront);
		}
		subMenu.add(displayImageInBackOrInFrontPanel);
		
		
		return subMenu;
	}



	private class CheckBoxMenu extends JMenu {
		private static final long serialVersionUID = 1L;
		
		private String flags = null;
		private int counter = 0;
		
		private boolean atLeastOneItemShowing = false;
		
		public CheckBoxMenu(String name, String serverFlagsCallMethodName) {
			super(name);
			this.setVisible(false); // not visible by default.
			
			try {
				flags = (String) vis.callComponent(serverFlagsCallMethodName);
			} catch (Exception e1) {
				System.out.println("Could not obtain view preset flags, creating a view menu with default settings.");
			}
		}
		
		
		@Override
		public JMenuItem add(JMenuItem menuItem) {
			super.add(menuItem);
			
			if (!(menuItem instanceof JCheckBoxMenuItem)) {
				this.setVisible(true); // at least there's something to show!
				return menuItem;
			}
			
			
			if (flags != null) {
				try {
					char value = flags.charAt(counter);
					counter++;
					switch (value) {
					case '0':
						menuItem.setVisible(false);
						((JCheckBoxMenuItem)menuItem).setSelected(false);
						break;
					case '1':
						menuItem.setVisible(false);
						((JCheckBoxMenuItem)menuItem).setSelected(true);
						break;
					case '2':
						menuItem.setVisible(true);
						((JCheckBoxMenuItem)menuItem).setSelected(false);
						break;
					case '3':
						menuItem.setVisible(true);
						((JCheckBoxMenuItem)menuItem).setSelected(true);
						break;
					default:
						System.out.println(this.getText() +" flag \"" + value + "\" is an UNEXPECTED value!");
						break;
					}
				} catch (IndexOutOfBoundsException e) {
					System.out.println(this.getText() + " flag out of bounds for menu item " + 
							menuItem.getText() + ".");
				}
			}
			
			if (!atLeastOneItemShowing) {
				// check if this one helped:
				if (menuItem.isVisible()) {
					atLeastOneItemShowing = true;
					this.setVisible(true);
				}
			}
			
			return menuItem;
		}
	}
	
}
