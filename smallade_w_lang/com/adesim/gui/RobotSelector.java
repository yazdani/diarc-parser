package com.adesim.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import com.adesim.gui.datastructures.RobotVisualizationData;

public class RobotSelector extends JComboBox{
	private static final long serialVersionUID = 1L;

	private SortedComboBoxModel comboBoxModel;
	private RobotSelector meTheSelector = this;
	
	public RobotSelector(final ADESimMapVis parentVis) {
		comboBoxModel = new SortedComboBoxModel();
		this.setModel(comboBoxModel);
		this.setSelectedIndex(-1);
		
		this.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				parentVis.setSelectedRobot((String) meTheSelector.getSelectedItem());
			}
		});
		
		this.setFocusable(false); // that way doesn't take away from the panel's focus
	}

	public void updateRobotListings(
			ArrayList<RobotVisualizationData> otherRobots) {
		if (otherRobots == null) {
			return;
		}
		
		
		HashSet<String> listAsItShouldBe = new HashSet<String>();
		for (RobotVisualizationData eachOtherRobot : otherRobots) {
			listAsItShouldBe.add(eachOtherRobot.ID);
		}
		
		HashSet<String> itemsToRemove = new HashSet<String>();
		for (int i = 0; i < this.getItemCount(); i++) {
			String currentItem = this.getItemAt(i).toString();
			if (!listAsItShouldBe.contains(currentItem)) {
				itemsToRemove.add(currentItem);				
			}
		}
	
		
		for (String itemToRemove : itemsToRemove) {
			this.removeItem(itemToRemove);
		}
		
		for (String itemToPotentiallyAdd : listAsItShouldBe) {
			if (comboBoxModel.getIndexOf(itemToPotentiallyAdd) == -1) {
				comboBoxModel.addElement(itemToPotentiallyAdd);
			}
		}
		
		
		this.setVisible(comboBoxModel.getSize() > 0);
		this.setSize(this.getPreferredSize());
	}
	
	
	
	private class SortedComboBoxModel extends DefaultComboBoxModel {
		private static final long serialVersionUID = 1L;
		
		public void addElement(Object object) {
			for(int i=0;i<this.getSize();i++)
				if( ((String)getElementAt(i)).compareTo((String)object) > 0) {
					super.insertElementAt(object, i);
					return;
				}
			super.addElement(object);
		}
		
	}
	
}
