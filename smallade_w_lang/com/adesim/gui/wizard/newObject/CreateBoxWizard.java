package com.adesim.gui.wizard.newObject;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;

import com.adesim.datastructures.SimShape;
import com.adesim.gui.ADESimMapVis;
import com.adesim.gui.images.GuiImageHelper;
import com.adesim.gui.wizard.components.ColorChooserPanel;
import com.adesim.gui.wizard.components.DisappearingStatusLabel;
import com.adesim.gui.wizard.components.NumberTextField;
import com.adesim.objects.Box;
import com.adesim.objects.SimEntity;

public class CreateBoxWizard extends AbstractSimpleDragRectangleWizardPanel {
	private static final long serialVersionUID = 1L;

	private JCheckBox checkBoxOpen;

	private JCheckBox checkboxPushable;	
	
	/**
	 * Create the frame.
	 */
	public CreateBoxWizard(ADESimMapVis vis) {
		super(vis);
	}
	

	@Override
	protected void initGUI() {
		setBounds(100, 100, 279, 474);
		this.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.setLayout(null);
		
		JLabel lblCreateShapeBased = new JLabel("Create box shape based on:");
		lblCreateShapeBased.setBounds(6, 6, 267, 16);
		this.add(lblCreateShapeBased);
		
		JLabel lblName = new JLabel("Name (optional):");
		lblName.setBounds(6, 197, 118, 16);
		this.add(lblName);
		
		textFieldName = new JTextField();
		textFieldName.setBounds(136, 191, 134, 28);
		this.add(textFieldName);
		textFieldName.setColumns(10);
		
		
		checkBoxOpen = new JCheckBox("Open");
		checkBoxOpen.setSelected(true);
		checkBoxOpen.setBounds(6, 329, 92, 23);
		this.add(checkBoxOpen);
		
		JButton btnCreate = new JButton("Create box!");
		btnCreate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				createObject();
			}
		});
		btnCreate.setFont(new Font("Lucida Grande", Font.BOLD, 14));
		btnCreate.setBounds(6, 411, 262, 29);
		this.add(btnCreate);
		
		colorPanel = new ColorChooserPanel(Box.DEFAULT_COLOR);
		colorPanel.setBounds(6, 246, 262, 57);
		this.add(colorPanel);
		
		JLabel lblZ = new JLabel("Z:");
		lblZ.setBounds(6, 138, 21, 16);
		this.add(lblZ);
		
		textFieldZ = new NumberTextField(false, null);
		textFieldZ.setText("0");
		textFieldZ.setColumns(10);
		textFieldZ.setBounds(27, 132, 51, 28);
		this.add(textFieldZ);
		
		JLabel lblOptionalZlength = new JLabel("Optional Z-Length:");
		lblOptionalZlength.setBounds(90, 138, 134, 16);
		this.add(lblOptionalZlength);
		
		textFieldZLength = new JTextField();
		textFieldZLength.setColumns(10);
		textFieldZLength.setBounds(217, 132, 51, 28);
		this.add(textFieldZLength);
		
		statusLabel = new DisappearingStatusLabel();
		statusLabel.setVisible(false);
		statusLabel.setBounds(6, 439, 262, 30);
		this.add(statusLabel);
		
		final JButton btnInfoHowTo = new JButton("Info:  how to place blocks into box");
		btnInfoHowTo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JOptionPane.showMessageDialog(meTheWizard, "To place objects inside the box, " +
						"first create the box, " +
						"then use the \"Create new blocks\" wizard to place blocks " +
						"in the environment, and drag them into the box.", 
						btnInfoHowTo.getText(), JOptionPane.INFORMATION_MESSAGE);
			}
		});
		btnInfoHowTo.setBounds(6, 359, 262, 29);
		this.add(btnInfoHowTo);
		
		toggleButtonDragShape = new JToggleButton(getDrawShapeToggleText(true));
		toggleButtonDragShape.setBounds(6, 26, 262, 39);
		toggleButtonDragShape.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				toggleButtonDragShapeToggledCallback();
			}
		});
		add(toggleButtonDragShape);
		
		
		JButton buttonHelpEditShape = new JButton();
		buttonHelpEditShape.addActionListener(createEditShapeHelpActionListener());
		buttonHelpEditShape.setBounds(236, 72, 32, 32);
		buttonHelpEditShape.setIcon(GuiImageHelper.helpIcon);
		add(buttonHelpEditShape);
		
		JLabel label = new JLabel("<html>You can also EDIT a drawn shape <br>(when the above toggle is off) </html>");
		label.setBounds(6, 72, 218, 31);
		add(label);
		
		checkboxPushable = new JCheckBox("Pushable");
		checkboxPushable.setBounds(143, 329, 118, 23);
		add(checkboxPushable);
	}
	

	@Override
	protected SimEntity createSimEntity(SimShape shape, String objectName) {
		if (checkboxPushable.isSelected()) {
			appendPushActionAndSetPushabilityDeterminer(shape);
		}
		return new Box(shape, objectName, colorPanel.getColor(), checkBoxOpen.isSelected(), null);
	}
	
	@Override
	protected String getTitle() {
		return "Create new box";
	}
	
	
	// for debugging
	public static void main(String[] args) {
		CreateBoxWizard wizard = new CreateBoxWizard(null);
		wizard.setVisible(true);
	}
}

