package com.adesim.gui.wizard.newObject;

import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;

import com.adesim.datastructures.SimShape;
import com.adesim.gui.ADESimMapVis;
import com.adesim.gui.images.GuiImageHelper;
import com.adesim.gui.wizard.components.DisappearingStatusLabel;
import com.adesim.objects.Landmark;
import com.adesim.objects.SimEntity;

public class CreateLandmarkWizard extends AbstractComplexShapeWizardPanel {
	private static final long serialVersionUID = 1L;
	
	/**
	 * Create the frame.
	 */
	public CreateLandmarkWizard(ADESimMapVis vis) {
		super(vis, true, true, 10000);
	}

	
	/** init the GUI.  a lot like the wall wizard, except don't need 
	 * to create the z and z-length textboxes, those wouldn't make sense for a landmark */
	@Override
	protected void initGUI() {
		setBounds(100, 100, 276, 323);
		this.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.setLayout(null);

		JButton btnCreate = new JButton("Create landmark");
		btnCreate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				createObject();
			}
		});
		btnCreate.setFont(new Font("Lucida Grande", Font.BOLD, 14));
		btnCreate.setBounds(6, 267, 262, 29);
		this.add(btnCreate);
		
		

		JLabel lblName = new JLabel("Name (optional):");
		lblName.setBounds(6, 197, 118, 16);
		this.add(lblName);
		
		textFieldName = new JTextField();
		textFieldName.setBounds(136, 191, 134, 28);
		this.add(textFieldName);
		textFieldName.setColumns(10);
		

		
		statusLabel = new DisappearingStatusLabel();
		statusLabel.setBounds(6, 292, 262, 30);
		this.add(statusLabel);


		toggleButtonDragShape = new JToggleButton(getDrawShapeToggleText(true));
		toggleButtonDragShape.setMargin(new Insets(0, 0, 0, 0));
		toggleButtonDragShape.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				toggleButtonDragShapeToggledCallback();
			}
		});
		toggleButtonDragShape.setBounds(6, 37, 221, 67);
		add(toggleButtonDragShape);

		JLabel lblLandmarkShape = new JLabel("Landmark Shape:");
		lblLandmarkShape.setBounds(6, 9, 134, 16);
		add(lblLandmarkShape);

		JButton buttonHelpForToggleButtonDrawWallShape = new JButton();
		buttonHelpForToggleButtonDrawWallShape.setIcon(GuiImageHelper.helpIcon);
		buttonHelpForToggleButtonDrawWallShape.addActionListener(createDrawShapeHelpActionListener());
		buttonHelpForToggleButtonDrawWallShape.setBounds(233, 48, 32, 32);
		add(buttonHelpForToggleButtonDrawWallShape);

		JButton buttonHelpForToggleEditShape = new JButton();
		buttonHelpForToggleEditShape.addActionListener(createEditShapeHelpActionListener());
		buttonHelpForToggleEditShape.setIcon(GuiImageHelper.helpIcon);
		buttonHelpForToggleEditShape.setBounds(233, 123, 32, 32);
		add(buttonHelpForToggleEditShape);

		JLabel lblWhenYoureDone = new JLabel("<html>You can also EDIT a drawn shape <br>(when the above toggle is off) </html>");
		lblWhenYoureDone.setBounds(6, 124, 218, 31);
		add(lblWhenYoureDone);
	}



	@Override
	protected SimEntity createSimEntity(SimShape shape, String objectName) {
		return new Landmark(shape, objectName, false);
		// assuming landmarks are NOT laser-visible, because the wizard doesn't
		//    have a way to specify their heights, anyway.  And, really, the point
		//    of a landmark is NOT to be visible, if it were, it'd be a wall!
	}
	
	@Override
	protected String getTitle() {
		return "Create new landmark";
	}


	// for debugging
	public static void main(String[] args) {
		CreateLandmarkWizard wizard = new CreateLandmarkWizard(null);
		wizard.setVisible(true);
	}
}

