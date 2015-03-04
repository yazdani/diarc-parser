package com.adesim.gui.wizard.components;

import java.awt.Color;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.adesim.gui.wizard.WizardUtil;

public class NumberTextField extends JTextField implements DocumentListener {
	private static final long serialVersionUID = 1L;
	
	public abstract interface ChangeObserver {
		public void update();
	}

	
	private boolean greaterThanZero;
	private ChangeObserver observerIfAny;
	public NumberTextField(boolean greaterThanZero, ChangeObserver changeObserver) {
		this.greaterThanZero = greaterThanZero;
		this.observerIfAny = changeObserver;
		
		this.getDocument().addDocumentListener(this);
		
		verifyAndNotify();
	}
	
	@Override
	public void changedUpdate(DocumentEvent e) { verifyAndNotify();  }
	@Override
	public void insertUpdate(DocumentEvent e) { verifyAndNotify();  }
	@Override
	public void removeUpdate(DocumentEvent e) { verifyAndNotify();  }
	
	private void verifyAndNotify() {
		if (validNumber()) {
			this.setBackground(Color.WHITE);
		} else {
			this.setBackground(WizardUtil.COLOR_INVALID_CALM);
		}
		
		if (observerIfAny != null) {
			observerIfAny.update();
		}
	}

	public boolean validNumber() {
		try {
			Double num = Double.parseDouble(this.getText());
			if (greaterThanZero) {
				return (num > 0);
			} else {
				return true; // as long as it parsed, all good.
			}
		} catch (Exception e) {
			// not valid:
			return false;
		}
	}
	
	public double getValue() {
		return Double.parseDouble(this.getText());
	}
	
}
