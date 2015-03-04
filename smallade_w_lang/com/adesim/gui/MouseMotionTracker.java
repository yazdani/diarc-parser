package com.adesim.gui;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.ToolTipManager;

public class MouseMotionTracker {
	
	private SimPanel simPanel;
	

	public MouseMotionTracker(SimPanel passedPanel) {
		this.simPanel = passedPanel;
		
		simPanel.addMouseMotionListener(new MouseMotionListener() {
			// update both during moving and dragging:
			
        	public void mouseMoved(MouseEvent e) {
                updateMouseLabelBasedOnPoint(e);
            }
        	public void mouseDragged(MouseEvent e) {
        		updateMouseLabelBasedOnPoint(e);
        	}
        });
    
    
		simPanel.addMouseListener(new MouseListener() {
    		@Override
			public void mouseExited(MouseEvent e) {
				simPanel.vis.setStatusLabelMousePoint(null); // exited
			}
        	
        	@Override
			public void mouseReleased(MouseEvent e) {}
			@Override
			public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseClicked(MouseEvent e) {};
		});
		
		
		// make sure tool tips get displayed immediately
		ToolTipManager.sharedInstance().setInitialDelay(0);
		ToolTipManager.sharedInstance().setReshowDelay(0);
	}

	private void updateMouseLabelBasedOnPoint(MouseEvent e) {
        Point mousePoint = e.getPoint();
        simPanel.vis.setStatusLabelMousePoint(mousePoint);
        
        simPanel.setCurrentMouseLocation(mousePoint);
        simPanel.displayMouseOverDescriptionIfAny();
	}

}
