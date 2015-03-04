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
import com.adesim.gui.wizard.components.NumberTextField;
import com.adesim.objects.SimEntity;
import com.adesim.objects.Wall;

public class CreateWallWizard extends AbstractComplexShapeWizardPanel {
	private static final long serialVersionUID = 1L;
	
	/**
	 * Create the frame.
	 */
	public CreateWallWizard(ADESimMapVis vis) {
		super(vis, true, true, 10000);
	}


	@Override
	protected void initGUI() {
		setBounds(100, 100, 276, 306);
		this.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.setLayout(null);

		JButton btnCreate = new JButton("Create wall");
		btnCreate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				createObject();
			}
		});
		btnCreate.setFont(new Font("Lucida Grande", Font.BOLD, 14));
		btnCreate.setBounds(6, 244, 262, 29);
		this.add(btnCreate);

		JLabel lblZ = new JLabel("Z:");
		lblZ.setBounds(6, 198, 21, 16);
		this.add(lblZ);

		textFieldZ = new NumberTextField(false, null);
		textFieldZ.setText("0");
		textFieldZ.setColumns(10);
		textFieldZ.setBounds(27, 192, 51, 28);
		this.add(textFieldZ);

		JLabel lblOptionalZlength = new JLabel("Optional Z-Length:");
		lblOptionalZlength.setBounds(90, 198, 134, 16);
		this.add(lblOptionalZlength);

		textFieldZLength = new JTextField();
		textFieldZLength.setColumns(10);
		textFieldZLength.setBounds(217, 192, 51, 28);
		this.add(textFieldZLength);

		statusLabel = new DisappearingStatusLabel();
		statusLabel.setVisible(false);
		statusLabel.setBounds(6, 270, 262, 30);
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

		JLabel lblWallShape = new JLabel("Wall Shape:");
		lblWallShape.setBounds(6, 9, 134, 16);
		add(lblWallShape);

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
		return new Wall(shape);
	}
	
	@Override
	protected String getTitle() {
		return "Create new wall";
	}


	// for debugging
	public static void main(String[] args) {
		CreateWallWizard wizard = new CreateWallWizard(null);
		wizard.setVisible(true);
	}
}

