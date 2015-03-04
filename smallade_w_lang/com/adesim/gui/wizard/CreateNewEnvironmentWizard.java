package com.adesim.gui.wizard;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.adesim.datastructures.PointCollection;
import com.adesim.datastructures.PushabilityDeterminer;
import com.adesim.datastructures.SimShape;
import com.adesim.gui.ADESimMapVis;
import com.adesim.gui.wizard.components.NumberTextField;

public class CreateNewEnvironmentWizard extends JPanel {
	private static final long serialVersionUID = 1L;

	public static final Color COLOR_INVALID = Color.RED;
	
	private NumberTextField minX;
	private NumberTextField minY;
	private NumberTextField maxX;
	private NumberTextField maxY;
	
	private ADESimMapVis vis;
	
	private Component myWindow;

	private JCheckBox createBoundsAround;
	
	public CreateNewEnvironmentWizard(ADESimMapVis vis) {
		this.vis = vis;
		
		
		setBounds(100, 100, 358, 214);
		this.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.setLayout(null);
		
		JLabel lblMinimumX = new JLabel("Minimum X");
		lblMinimumX.setBounds(6, 12, 89, 16);
		this.add(lblMinimumX);
		
		minX = new NumberTextField(false, null);
		minX.setBounds(96, 6, 107, 28);
		this.add(minX);
		minX.setColumns(10);
		
		JLabel lblY = new JLabel("Y");
		lblY.setBounds(226, 12, 89, 16);
		this.add(lblY);
		
		minY = new NumberTextField(false, null);
		minY.setColumns(10);
		minY.setBounds(246, 6, 107, 28);
		this.add(minY);
		
		JLabel lblMaximumX = new JLabel("Maximum X");
		lblMaximumX.setBounds(6, 46, 89, 16);
		this.add(lblMaximumX);
		
		maxX = new NumberTextField(false, null);
		maxX.setColumns(10);
		maxX.setBounds(96, 40, 107, 28);
		this.add(maxX);
		
		JLabel label_1 = new JLabel("Y");
		label_1.setBounds(226, 46, 89, 16);
		this.add(label_1);
		
		maxY = new NumberTextField(false, null);
		maxY.setColumns(10);
		maxY.setBounds(246, 40, 107, 28);
		this.add(maxY);
		
		createBoundsAround = new JCheckBox("Create bounds around world perimeter");
		createBoundsAround.setSelected(true);
		createBoundsAround.setBounds(6, 81, 347, 23);
		this.add(createBoundsAround);
		
		JButton btnCreateworld = new JButton("Create World");
		btnCreateworld.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				createWorld();
			}
		});
		btnCreateworld.setBounds(6, 179, 346, 29);
		this.add(btnCreateworld);
		
		JLabel lblIfYouHave = new JLabel("<html>If you have robots currently running, remember to move ther to move them in bounds before creating the world!</html>");
		lblIfYouHave.setFont(new Font("Lucida Grande", Font.ITALIC, 13));
		lblIfYouHave.setBounds(28, 115, 312, 52);
		add(lblIfYouHave);
		
		
		
		// the if statement below is only to appease WindowBuilder Pro 
		//     (http://code.google.com/javadevtools/download-wbpro.html, free add-in into Eclipse)
		//     and to allow me to design the GUIs via drag-and-drop.  In reality, vis better be NOT null!
		if (vis != null) {
			Point2D minPt = vis.model.worldBounds.getMin();
			Point2D maxPt = vis.model.worldBounds.getMax();
			
			minX.setText(Double.toString(minPt.getX()));
			minY.setText(Double.toString(minPt.getY()));
			maxX.setText(Double.toString(maxPt.getX()));
			maxY.setText(Double.toString(maxPt.getX()));
			
			
			// create Frame or JInternal frame:
			myWindow = vis.createWindowForHelperPanel(this, this.getSize(), true,
					false, "Create new Environment?", JFrame.DISPOSE_ON_CLOSE);
		}
	}


	private void createWorld() {
		if (!WizardUtil.canProceedBasicCheckAndAlert(this)) {
			return; // it would have already done the alert.
		}
		
		// if here, then text fields were all good, can just request their values
		if (  (maxX.getValue() <= minX.getValue()) ||
			  (maxY.getValue() <= minY.getValue())   ) {
			JOptionPane.showMessageDialog(this, "The maximum X and Y must be greater than the minimums!", 
					"One or more fields are invalid!", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		PointCollection worldPtCollection = PointCollection.createRectangleFromCornerPoints(
				new Point2D.Double(minX.getValue(), minY.getValue()),
				new Point2D.Double(maxX.getValue(), maxY.getValue()));
		SimShape worldShape = new SimShape(worldPtCollection, null, 0, null, PushabilityDeterminer.alwaysFalse); 
		//    PushabilityDeterminer.alwaysFalse = world bounds are NOT pushable!
		
		// if still here, go for it:
		try {
			vis.callComponent("createNewEnvironment", worldShape, createBoundsAround.isSelected());
			closeWindow();
		} catch (Exception e) {
			vis.showErrorMessage("Could not create new environment, due to " +
					"\n" + e.toString() + 
					"\nSee console for stack trace.",
					"Error creating environement");
			e.printStackTrace();
		}
		
	}


	private void closeWindow() {
		if (myWindow instanceof JFrame) {
			((JFrame)myWindow).dispose();
		} else if (myWindow instanceof JInternalFrame) {
			((JInternalFrame) myWindow).dispose();
		} else {
			System.out.println("Containing window is neither a JFrame nor a JInternalFrame, " +
					"can't dispose it.");
		}
	}

}
