package com.adesim.gui.wizard.newObject;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;

import com.adesim.datastructures.SimShape;
import com.adesim.gui.ADESimMapVis;
import com.adesim.gui.images.GuiImageHelper;
import com.adesim.gui.wizard.components.ColorChooserPanel;
import com.adesim.gui.wizard.components.DisappearingStatusLabel;
import com.adesim.gui.wizard.components.NumberTextField;
import com.adesim.objects.Block;
import com.adesim.objects.SimEntity;

public class CreateBlockWizard extends AbstractSimpleDragRectangleWizardPanel {
	private static final long serialVersionUID = 1L;

	private JCheckBox checkBoxLaserVisible;

	private JCheckBox checkboxPushable;

	public CreateBlockWizard(ADESimMapVis vis) {
		super(vis);
	}

	@Override
	protected void initGUI() {
		setBounds(100, 100, 275, 422);
		this.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.setLayout(null);
		
		JLabel lblName = new JLabel("Name (optional):");
		lblName.setBounds(6, 191, 118, 16);
		this.add(lblName);
		
		textFieldName = new JTextField();
		textFieldName.setBounds(136, 185, 134, 28);
		this.add(textFieldName);
		textFieldName.setColumns(10);
		
		
		checkBoxLaserVisible = new JCheckBox("Laser-visible");
		checkBoxLaserVisible.setSelected(true);
		checkBoxLaserVisible.setBounds(6, 323, 134, 23);
		this.add(checkBoxLaserVisible);
		
		JButton btnCreate = new JButton("Create block!");
		btnCreate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				createObject();
			}
		});
		btnCreate.setFont(new Font("Lucida Grande", Font.BOLD, 14));
		btnCreate.setBounds(6, 358, 262, 29);
		this.add(btnCreate);
		
		colorPanel = new ColorChooserPanel(Block.DEFAULT_COLOR);
		colorPanel.setBounds(6, 240, 262, 57);
		this.add(colorPanel);
		
		JLabel lblZ = new JLabel("Z:");
		lblZ.setBounds(6, 132, 21, 16);
		this.add(lblZ);
		
		textFieldZ = new NumberTextField(false, null);
		textFieldZ.setText("0");
		textFieldZ.setColumns(10);
		textFieldZ.setBounds(27, 126, 51, 28);
		this.add(textFieldZ);
		
		JLabel lblOptionalZlength = new JLabel("Optional Z-Length:");
		lblOptionalZlength.setBounds(90, 132, 134, 16);
		this.add(lblOptionalZlength);
		
		textFieldZLength = new JTextField();
		textFieldZLength.setColumns(10);
		textFieldZLength.setBounds(217, 126, 51, 28);
		this.add(textFieldZLength);
		
		statusLabel = new DisappearingStatusLabel();
		statusLabel.setVisible(false);
		statusLabel.setBounds(6, 386, 262, 30);
		this.add(statusLabel);
		
		JLabel lblCreateBlockShape = new JLabel("Create block shape based on:");
		lblCreateBlockShape.setBounds(3, 6, 267, 16);
		add(lblCreateBlockShape);
		
		toggleButtonDragShape = new JToggleButton(getDrawShapeToggleText(true));
		toggleButtonDragShape.setBounds(3, 26, 262, 39);
		toggleButtonDragShape.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				toggleButtonDragShapeToggledCallback();
			}
		});
		add(toggleButtonDragShape);
		
		JLabel label_1 = new JLabel("<html>You can also EDIT a drawn shape <br>(when the above toggle is off) </html>");
		label_1.setBounds(3, 72, 218, 31);
		add(label_1);
		
		JButton buttonHelpEditShape = new JButton();
		buttonHelpEditShape.setIcon(GuiImageHelper.helpIcon);
		buttonHelpEditShape.addActionListener(createEditShapeHelpActionListener());
		buttonHelpEditShape.setBounds(233, 72, 32, 32);
		add(buttonHelpEditShape);
		
		checkboxPushable = new JCheckBox("Pushable");
		checkboxPushable.setBounds(134, 323, 134, 23);
		add(checkboxPushable);
	}
	
	
	@Override
	protected SimEntity createSimEntity(SimShape shape, String objectName) {
		if (checkboxPushable.isSelected()) {
			appendPushActionAndSetPushabilityDeterminer(shape);
		}
		return new Block(shape, objectName, checkBoxLaserVisible.isSelected(), colorPanel.getColor());
	}

	@Override
	protected String getTitle() {
		return "Create new block";
	}
	

	// for debugging
	public static void main(String[] args) {
		CreateBlockWizard wizard = new CreateBlockWizard(null);
		wizard.setVisible(true);
	}

}

