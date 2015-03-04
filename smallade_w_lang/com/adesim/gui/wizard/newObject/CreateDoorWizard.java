package com.adesim.gui.wizard.newObject;

import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;

import com.adesim.datastructures.SimShape;
import com.adesim.datastructures.action.DoorPushOpenAction;
import com.adesim.datastructures.action.SimAction;
import com.adesim.gui.ADESimMapVis;
import com.adesim.gui.images.GuiImageHelper;
import com.adesim.gui.wizard.components.DisappearingStatusLabel;
import com.adesim.gui.wizard.components.NumberTextField;
import com.adesim.objects.Door;
import com.adesim.objects.SimEntity;
import com.adesim.util.SimUtil;
import javax.swing.JCheckBox;

public class CreateDoorWizard extends AbstractComplexShapeWizardPanel {
	private static final long serialVersionUID = 1L;
	private NumberTextField textFieldDoorThickness;
	private NumberTextField textFieldPercentOpen;
	private JCheckBox checkboxDoorCanBePushedOpen;
	
	/**
	 * Create the frame.
	 */
	public CreateDoorWizard(ADESimMapVis vis) {
		super(vis, false, false, 3);
	}


	@Override
	protected void initGUI() {
		setBounds(100, 100, 276, 468);
		this.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.setLayout(null);

		JButton btnCreate = new JButton("Create door");
		btnCreate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				createObject();
			}
		});
		btnCreate.setFont(new Font("Lucida Grande", Font.BOLD, 14));
		btnCreate.setBounds(6, 406, 262, 29);
		this.add(btnCreate);

		JLabel lblZ = new JLabel("Z:");
		lblZ.setBounds(6, 367, 21, 16);
		this.add(lblZ);

		textFieldZ = new NumberTextField(false, null);
		textFieldZ.setText("0");
		textFieldZ.setColumns(10);
		textFieldZ.setBounds(27, 355, 51, 28);
		this.add(textFieldZ);

		JLabel lblOptionalZlength = new JLabel("Optional Z-Length:");
		lblOptionalZlength.setBounds(90, 367, 134, 16);
		this.add(lblOptionalZlength);

		textFieldZLength = new JTextField();
		textFieldZLength.setColumns(10);
		textFieldZLength.setBounds(217, 355, 51, 28);
		this.add(textFieldZLength);

		statusLabel = new DisappearingStatusLabel();
		statusLabel.setVisible(false);
		statusLabel.setBounds(6, 432, 262, 30);
		this.add(statusLabel);


		toggleButtonDragShape = new JToggleButton(getDrawShapeToggleText(true));
		toggleButtonDragShape.setMargin(new Insets(0, 0, 0, 0));
		toggleButtonDragShape.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				toggleButtonDragShapeToggledCallback();
			}
		});
		toggleButtonDragShape.setBounds(6, 104, 262, 47);
		add(toggleButtonDragShape);

		JLabel lblWallShape = new JLabel("Door shape.  Specify three points:");
		lblWallShape.setBounds(6, 9, 262, 16);
		add(lblWallShape);

		JButton buttonHelpForToggleEditShape = new JButton();
		buttonHelpForToggleEditShape.addActionListener(createEditShapeHelpActionListener());
		buttonHelpForToggleEditShape.setIcon(GuiImageHelper.helpIcon);
		buttonHelpForToggleEditShape.setBounds(236, 185, 32, 32);
		add(buttonHelpForToggleEditShape);

		JLabel lblWhenYoureDone = new JLabel("<html>You can also EDIT a drawn shape <br>(when the above toggle is off) </html>");
		lblWhenYoureDone.setBounds(9, 186, 218, 31);
		add(lblWhenYoureDone);
		
		JLabel lblPivotPoint = new JLabel("<html>1) Pivot Point<br/>2) Door end point<br/>3) Point to determine max swing angle </html>");
		lblPivotPoint.setBounds(16, 28, 254, 50);
		add(lblPivotPoint);
		
		JLabel lblDoorWidth = new JLabel("Door thickness:");
		lblDoorWidth.setBounds(6, 245, 120, 16);
		add(lblDoorWidth);
		
		textFieldDoorThickness = new NumberTextField(false, null);
		textFieldDoorThickness.setText("0.04");
		textFieldDoorThickness.setColumns(10);
		textFieldDoorThickness.setBounds(129, 239, 51, 28);
		add(textFieldDoorThickness);
		
		JLabel lblNameoptional = new JLabel("Name (optional):");
		lblNameoptional.setBounds(6, 334, 120, 16);
		add(lblNameoptional);
		
		textFieldName = new JTextField();
		textFieldName.setColumns(10);
		textFieldName.setBounds(129, 325, 139, 28);
		add(textFieldName);
		
		JLabel lblPercentOpen = new JLabel("Initial percent open (0-100):");
		lblPercentOpen.setBounds(6, 272, 199, 16);
		add(lblPercentOpen);
		
		textFieldPercentOpen = new NumberTextField(false, null);
		textFieldPercentOpen.setText("0");
		textFieldPercentOpen.setColumns(10);
		textFieldPercentOpen.setBounds(206, 268, 51, 28);
		add(textFieldPercentOpen);
		
		checkboxDoorCanBePushedOpen = new JCheckBox("Door can be pushed open");
		checkboxDoorCanBePushedOpen.setBounds(6, 296, 248, 23);
		add(checkboxDoorCanBePushedOpen);
	}

	@Override
	protected String getDrawShapeToggleText(boolean selected) {
		if (selected) {
			return "<html>Tracking mouse ... <br> Toggle off when done.</html>";
		} else {
			return "Initiate mouse tracking";
		}
	}

	@Override
	protected boolean canProceedExtraChecks() {
		// need three points for door!
		if (currentShape.getAllPoints().size() != 3) {
			JOptionPane.showMessageDialog(this, "Door shape must consist of three points, " +
					"the pivot point, the door-width-and-closed-angle- determining point, " +
					"and the open-angle- determining point", 
					"Door shape does not contain the expected number of points",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		Double openFraction = Double.parseDouble(textFieldPercentOpen.getText()) / 100.0;
		if ((openFraction < 0) || (openFraction > 100)) {
			JOptionPane.showMessageDialog(this, "The door-open percentage must be between" +
					"0 and 100 (inclusive).", 
					"Invalid open percentage",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		// if still here:
		return true;
	}
	
	@Override
	protected SimEntity createSimEntity(SimShape shape, String objectName) {
		Double openFraction = Double.parseDouble(textFieldPercentOpen.getText()) / 100.0;
		
		// know that there will be exactly 3 points because of the "canProceedExtraChecks" check.
		List<Point2D> points = shape.getAllPoints();
		
		Point2D pivotPoint = points.get(0);
		Point2D widthDeterminingPoint = points.get(1);
		Point2D swingDeterminingPoint = points.get(2);
		
		double doorWidth = pivotPoint.distance(widthDeterminingPoint);
		
		double closedAngle = SimUtil.getAngle0to2PI(pivotPoint, widthDeterminingPoint);
		
		double openAngle = SimUtil.getAngle0to2PI(pivotPoint, swingDeterminingPoint);
		double pivotAngle = normalizeAngleMinutPItoPI(openAngle - closedAngle);
		
		ArrayList<SimAction> actions = null;
		if (checkboxDoorCanBePushedOpen.isSelected()) {
			actions = new ArrayList<SimAction>();
			actions.add(new DoorPushOpenAction());
		}
		
		return new Door(objectName, openFraction, pivotPoint, doorWidth, 
				Double.parseDouble(textFieldDoorThickness.getText()), closedAngle, pivotAngle, actions);
	}
	
	public static double normalizeAngleMinutPItoPI(double aTheta) {
		boolean keepGoing = true;
		while (keepGoing) {
			if (aTheta > Math.PI) {
				aTheta = aTheta - 2 * Math.PI;
			} else if (aTheta < -1 * Math.PI) {
				aTheta = aTheta + 2 * Math.PI;
			} else {
				keepGoing = false;
			}
		}
		return aTheta;
	}


	@Override
	protected String getTitle() {
		return "Create new door";
	}
	

	
	

	// for debugging
	public static void main(String[] args) {
		CreateDoorWizard wizard = new CreateDoorWizard(null);
		wizard.setVisible(true);
	}
}

