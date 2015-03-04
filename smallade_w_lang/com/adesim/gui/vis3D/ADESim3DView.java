package com.adesim.gui.vis3D;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import com.adesim.datastructures.Point3D;
import com.adesim.gui.ADESimMapVis;
import com.adesim.gui.SimPanel;
import com.adesim.gui.customKeyListener.CustomKeyListener;
import com.adesim.gui.customKeyListener.CustomKeyProcessor;
import com.adesim.util.SimUtil;

public class ADESim3DView extends JPanel implements CustomKeyListener, MouseListener, MouseMotionListener {
	private static final long serialVersionUID = 1L;

	private static final Dimension DEFAULT_WINDOW_SIZE = new Dimension(400, 200);
	private static final double DEFAULT_Z = 0.5;

	private double UP_DOWN_MOVEMENT_AMOUNT; // like a constant, though determined by world size
	private double Z_MOVEMENT_AMOUNT = 0.03;
	private double THETA_MOVEMENT_AMOUNT = Math.PI / 60;

	private ADESimMapVis vis;
	private SimPanel simPanel;
	private ADESim3DVisInnerPanel visPanel3D;
	public Point3D cameraLocation;
	private Point draggingStartPoint;
	
	
	public ADESim3DView(ADESimMapVis vis, Point2D cameraLocation2D, double initialTheta) {
		this.vis = vis;
		this.simPanel = vis.getSimPanel();
		this.cameraLocation = new Point3D(cameraLocation2D, DEFAULT_Z);
		

		UP_DOWN_MOVEMENT_AMOUNT = SimUtil.getMaxDimension(vis.model.worldBounds.getBoundingDimension()) / 200.0;
		
		
		createWindowAndHookUpClosingListener();
		
		
		// create main 3D panel
		this.setLayout(new BorderLayout());
		this.visPanel3D = new ADESim3DVisInnerPanel(true);
		this.add(visPanel3D, BorderLayout.CENTER);
		

		// add key listener to 3D panel, after it's been created.
		new CustomKeyProcessor(visPanel3D.panel3D, this);
		
		
		// once done loading everything, add to vis's list:
		synchronized (vis.threeDviews) {
			vis.threeDviews.add(this);			
		}
		

		// and set camera location with theta.
		visPanel3D.setCameraLocation(
				cameraLocation.x, cameraLocation.y, cameraLocation.z, initialTheta);
		
		
		simPanel.addMouseListener(this);
		simPanel.addMouseMotionListener(this);
	}
	

	private void createWindowAndHookUpClosingListener() {
		// create Frame or JInternal frame:
		Container containingWindow = vis.createWindowForHelperPanel(
				this, DEFAULT_WINDOW_SIZE, false, true, 
				"3D view for " + vis.getComponentName(), JFrame.DISPOSE_ON_CLOSE);
		
		if (containingWindow instanceof JFrame) {
			((JFrame)containingWindow).addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosed(WindowEvent e) {
					windowClosingCallback();
				}
			});
		} else if (containingWindow instanceof JInternalFrame) {
			((JInternalFrame) containingWindow).addInternalFrameListener(new InternalFrameAdapter() {
				@Override
				public void internalFrameClosed(InternalFrameEvent e) {
					windowClosingCallback();
				}
			});
		} else {
			System.out.println("Containing window is neither a JFrame nor a JInternalFrame, " +
					"can't create a hook into a window-closing callback.");
		}
	}


	protected void windowClosingCallback() {
		synchronized (vis.threeDviews) {
			vis.threeDviews.remove(this);
		}
		
		// de-register mouse listeners.
		simPanel.removeMouseListener(this);
		simPanel.removeMouseMotionListener(this);
	}

	public void updateView() {
		// having gotten the new data, reload map:
		visPanel3D.reloadObjects(vis.model, null);
		

		//STEP 2:  update camera location

		visPanel3D.setCameraLocation(
				cameraLocation.x, cameraLocation.y, cameraLocation.z, null);
		// note that not touching panel3D's tilt or pan, so that can let user drag mouse to look up/down
		
		
		// STEP 3:  repaint
		visPanel3D.setView();		
	}
	
	public double getCameraTheta() {
		return visPanel3D.getTheta();
	}

	@Override
	public void keyPressed(int keyCode) {
		if (keyCode == KeyEvent.VK_UP) {
			Point2D movementOffset = AffineTransform.getRotateInstance(getCameraTheta())
					.transform(new Point2D.Double(UP_DOWN_MOVEMENT_AMOUNT, 0), null);
			cameraLocation.addOffset(movementOffset);
			visPanel3D.setView();
		} else if (keyCode == KeyEvent.VK_DOWN) {
			Point2D movementOffset = AffineTransform.getRotateInstance(getCameraTheta())
					.transform(new Point2D.Double(-1 * UP_DOWN_MOVEMENT_AMOUNT, 0), null);
			cameraLocation.addOffset(movementOffset);
			visPanel3D.setView();
		} else if (keyCode == KeyEvent.VK_LEFT) {
			visPanel3D.offsetTheta(THETA_MOVEMENT_AMOUNT);
			visPanel3D.setView();
	    } else if (keyCode == KeyEvent.VK_RIGHT) {
	    	visPanel3D.offsetTheta(-1 * THETA_MOVEMENT_AMOUNT);
			visPanel3D.setView();
	    } else if (keyCode == KeyEvent.VK_PAGE_UP) {
	    	cameraLocation.z += Z_MOVEMENT_AMOUNT;
	    } else if (keyCode == KeyEvent.VK_PAGE_DOWN) {
	    	cameraLocation.z = Math.max(0, cameraLocation.z + -1 * Z_MOVEMENT_AMOUNT);
	    } else {
        	// ignore anything else, it's probably none of my business anyway!
	    }
	}

	@Override
	public void keyReleased(int keyCode) {
		// don't care
	}
	
	

	@Override
	public void mousePressed(MouseEvent e) {
		Point pt = e.getPoint();
		Rectangle2D mouseSurroundRectangle = SimPanel.createMouseSurroundRectangle(pt);
		if (simPanel.threeDViewShapes.get(this).intersects(mouseSurroundRectangle)) {
			this.draggingStartPoint = pt;
		}
	}
	
	@Override
	public void mouseDragged(MouseEvent e) {
		if (this.draggingStartPoint != null) {
			Point point = e.getPoint();
			double visDx = point.getX() - draggingStartPoint.getX();
			double visDy = -1 * (point.getY() - draggingStartPoint.getY());
			// -1 since screen y grows down, Eucledian grows up

			this.cameraLocation.addOffset(vis.getModelPointDiff(visDx), vis.getModelPointDiff(visDy));
			
			this.draggingStartPoint = point;
		}
	}
	
	@Override
	public void mouseReleased(MouseEvent e) {
		this.draggingStartPoint = null;
	}
	
	
	@Override
	public void mouseClicked(MouseEvent e) {}
	@Override
	public void mouseEntered(MouseEvent e) {}
	@Override
	public void mouseExited(MouseEvent e) {}

	
	@Override
	public void mouseMoved(MouseEvent e) {}
	
}
