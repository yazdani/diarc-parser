package com.adesim.gui;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.adesim.gui.datastructures.RobotVisualizationData;
import com.adesim.objects.SimEntity;

public class ActableObjectsPopupListener extends MouseAdapter{
	private SimPanel simPanel;
	private ArrayList<PopupObjectAction> objectActions;
	private JPopupMenu popup;
	
	public ActableObjectsPopupListener(SimPanel simPanel) {
		this.simPanel = simPanel;
		
		simPanel.addMouseListener(this);
	}

    public void mousePressed(MouseEvent e) {
    	if (simPanel.vis.mouseListenerOverriders.size() > 0) {
			return;
		}
    	
        maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
        	popup = new JPopupMenu("Actions");
        	        	
        	objectActions = new ArrayList<PopupObjectAction>();
        	
        	// check which objects intersects:
        	Rectangle2D mouseSurroundRectangle = SimPanel.createMouseSurroundRectangle(e.getPoint());
        	
        	addObjectActions(mouseSurroundRectangle);
        	addRobotActions(e.getPoint());
        	
        	if (objectActions.size() > 0) {
        		popup.show(e.getComponent(),
                       e.getX(), e.getY());
        	}
        }
    }

	private void addRobotActions(Point point) {
		for (final RobotVisualizationData eachRobot : simPanel.vis.robotsVisualizationDataFromComponent) {
			if (eachRobot.containsSimVisualizationPoint(point, simPanel)) {
				ArrayList<PopupObjectAction> eachRobotActions = eachRobot.popupRobotActions;
				
				if (  (eachRobotActions != null) && (eachRobotActions.size() > 0)  ) { 
					// if there are actions that could be performed
					
					objectActions.addAll(eachRobotActions);
					
					JMenu eachEntityMenu = new JMenu("Robot");
					
					popup.add(eachEntityMenu);
					
					for (final PopupObjectAction eachPopupAction : eachRobotActions) {
						JMenuItem popupActionMenu = new JMenuItem(eachPopupAction.actionNameForPopup);
						popupActionMenu.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								try {
									if (simPanel.vis.visType == ADESimMapVisualizationType.SINGLE_ROBOT) {
										simPanel.vis.callComponent(eachPopupAction.methodName, eachPopupAction.args);
									} else { // redirect request via the environment:
										simPanel.vis.callComponent("executeRobotPopupAction", eachRobot.ID, 
																eachPopupAction.methodName, eachPopupAction.args);
									}
								} catch (Exception e1) {
									System.out.println("Could not call action \"" + eachPopupAction.methodName + 
											"\" for \"" + eachRobot.ID + "\"");
								}
							}
						});
						
						eachEntityMenu.add(popupActionMenu);
					}
				}
			}
		}
	}

	private void addObjectActions(Rectangle2D mousePt) {
		for (SimEntity eachEntity : simPanel.vis.model.worldObjects.getObjects()) {
    		if (simPanel.simEntityIDtoAWTshapeMap.containsKey(eachEntity.getGUID())) {
    			if (simPanel.simEntityIDtoAWTshapeMap.get(eachEntity.getGUID()).intersects(mousePt)) {
    				
    				ArrayList<PopupObjectAction> eachEntityActions = eachEntity.getPopupObjectActions();
    				
    				if (  (eachEntityActions != null) && (eachEntityActions.size() > 0)  ) { 
    					// if there are actions that could be performed
    					
    					objectActions.addAll(eachEntityActions);
    					
    					final String popupName = eachEntity.getNameOrType(true);
    					JMenu eachEntityMenu = new JMenu(popupName);
    					eachEntityMenu.setToolTipText(eachEntity.getToolTipIfAny());
    					
    					popup.add(eachEntityMenu);
    					
    					for (final PopupObjectAction eachPopupAction : eachEntityActions) {
    						JMenuItem popupActionMenu = new JMenuItem(eachPopupAction.actionNameForPopup);
    						popupActionMenu.addActionListener(new ActionListener() {
								@Override
								public void actionPerformed(ActionEvent e) {
									try {
										simPanel.vis.callComponent(eachPopupAction.methodName, eachPopupAction.args);
									} catch (Exception e1) {
										System.out.println("Could not call action \"" + eachPopupAction.methodName + 
												"\" for \"" + popupName + "\"");
									}
								}
							});
    						
    						eachEntityMenu.add(popupActionMenu);
    					}
    				}
    			}
    		}
    	}
	}
    
    

}
