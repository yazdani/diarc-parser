package com.adesim.gui.wizard;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Robot;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

public class WizardUtil {
	public static final Color COLOR_INVALID_CALM = new Color(187, 198, 255);
	public static final Color COLOR_INVALID_ALERT = new Color(255, 92, 70);


	public static boolean canProceedBasicCheckAndAlert (Container parent) {
		List<Component> allComponents = getAllComponents(parent);
		for (Component each : allComponents) {
			if (!isValid(each))	{
				each.setBackground(COLOR_INVALID_ALERT);
				
				JOptionPane.showMessageDialog(parent, "Please fill in all required fields", 
						"One or more fields are invalid!", JOptionPane.ERROR_MESSAGE);
				
				// try to select the invalid component
				try {
					each.requestFocus();
					
					Point componentLocationOnScreen = each.getLocationOnScreen();
					Point componentCenter = new Point(
							componentLocationOnScreen.x + each.getWidth() / 2, 
							componentLocationOnScreen.y + each.getHeight() / 2);
					Robot mouseMover = new Robot();
					mouseMover.mouseMove(componentCenter.x, componentCenter.y);

				} catch (Exception e) { 
					// if can't select, oh well
				}
				
				return false;
			}
		}
		// if haven't quit with false:
		return true;
	}



	private static List<Component> getAllComponents(Container parent) {
		List<Component> components = new ArrayList<Component>();
		components.add(parent); // the original parent
		for (Component eachChild : parent.getComponents()) {
			components.add(eachChild);
			if (eachChild instanceof Container) {
				components.addAll(getAllComponents((Container) eachChild));
			}
		}
		return components;
	}

	
	public static boolean isValid(Component component) {
		if (   (COLOR_INVALID_CALM.equals(component.getBackground())) ||
		       (COLOR_INVALID_ALERT.equals(component.getBackground()))   ) {
			return false;
		} else {
			return true;
		}
	}

	public static boolean isNullOrEmpty(String string) {
		return (  (string == null) || (string.equals(""))  );
	}

	public static String getRealStringOrNull(String string) {
		if (isNullOrEmpty(string)) {
			return null;
		} else {
			return string;
		}
	}



}
