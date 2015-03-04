package com.adesim.gui.wizard.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Map.Entry;

import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;

import utilities.ColorConverter;


public class ColorChooserPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	
	public JComboBox comboBox;
	private ArrayList<String> colorNames = new ArrayList<String>();
	private ArrayList<Color> colors = new ArrayList<Color>();
	
	public JToggleButton toggleButton;
	private Color toggleColor;

	
	public ColorChooserPanel(Color defaultColor) {
		initComboBox(defaultColor);
		initToggle(defaultColor);
		
		setLayout(new BorderLayout(5, 5));
		
		JLabel lblColor = new JLabel("Color:");
		add(lblColor, BorderLayout.WEST);
		
		JPanel panel = new JPanel();
		add(panel);
		panel.setLayout(new GridLayout(2, 1, 5, 5));
		
		panel.add(comboBox);
		
		panel.add(toggleButton);
	}
	
	/** returns the chosen color of the combo box or custom color */ 
	public Color getColor() {
		if (toggleButton.isSelected()) {
			return toggleColor;
		} else {
			return colors.get(comboBox.getSelectedIndex());
		}
	}
	
	
	private void initToggle(Color defaultColor) {
		this.toggleButton = new JToggleButton("Or choose a custom color");
		
		// if color did not match
		if (this.comboBox.getSelectedIndex() < 0) {
			this.toggleButton.setSelected(true);
			this.toggleColor = defaultColor;
		}
		
		this.toggleButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// if just selected it, ask for a color:
				if (toggleButton.isSelected()) {
					// if no color selected, go off of combobox
					if (toggleColor == null) {
						toggleColor = colors.get(comboBox.getSelectedIndex());
					}
					toggleColor = JColorChooser.showDialog(toggleButton, 
							"Please choose a custom color", toggleColor);
					if (toggleColor == null) { // if cancelled
						toggleButton.setSelected(false);
					}
				}
			}
		});
	}
	

	private void initComboBox(Color defaultColor) {
		Integer[] intArray = new Integer[ColorConverter.colorTable.size()];
		for (int i = 0; i < intArray.length; i++) {
			intArray[i] = i;
		}
		
 		this.comboBox = new JComboBox(intArray);
		this.comboBox.setRenderer(new ComboBoxRenderer());
		
		int indexToSelect = 0; // 0 by default, so at least there's something...
		int counter = 0;
		for (Entry<String, Color> eachColor : ColorConverter.colorTable.entrySet()) {
			Color color = eachColor.getValue();
			if (color.equals(defaultColor)) {
				indexToSelect = counter;
			}
			
			this.colors.add(eachColor.getValue());
			this.colorNames.add(eachColor.getKey());
			counter++;
		}
		
		this.comboBox.setSelectedIndex(indexToSelect);
	}

	private class ComboBoxRenderer implements ListCellRenderer {
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			// Get the selected index. (The index param isn't
			// always valid, so just use the value.)
			int selectedIndex = ((Integer) value).intValue();
			
			JLabel labelName = new JLabel(colorNames.get(selectedIndex));
			labelName.setOpaque(true);
			JLabel labelColor = new JLabel();
			labelColor.setOpaque(true);
			labelColor.setBackground(colors.get(selectedIndex));
			labelColor.setPreferredSize(new Dimension(labelName.getHeight(), labelName.getHeight()));
			
			if (isSelected) {
				labelName.setBackground(list.getSelectionBackground());
				labelName.setForeground(list.getSelectionForeground());
			} else {
				labelName.setBackground(list.getBackground());
				labelName.setForeground(list.getForeground());
			}
			
			JPanel panel = new JPanel(new GridLayout(1,2));
			panel.add(labelName);
			panel.add(labelColor);
			panel.setOpaque(true);
			
			return panel;
		}
	}


}
