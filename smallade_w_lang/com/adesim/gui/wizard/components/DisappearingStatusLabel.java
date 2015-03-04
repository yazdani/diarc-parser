package com.adesim.gui.wizard.components;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.Timer;

public class DisappearingStatusLabel extends JLabel {
	private static final long serialVersionUID = 1L;
	
	private DisappearingStatusLabel meTheLabel;
	
	private static final Color STATUS_COLOR = new Color(0, 128, 64);
	private static final int SLEEP_DURATION = 3000;
	
	private Timer disappearerTimer;
	
	public DisappearingStatusLabel() {
		this.meTheLabel = this;
		this.setForeground(STATUS_COLOR);
		this.setText("Status...");
	}
	
	@Override
	public void setText(String text) {
		// forget about existing timer.
		if (disappearerTimer != null) {
			disappearerTimer.stop();
		}
		
		super.setText(text);
		
		this.setVisible(true);
		disappearerTimer = new Timer(SLEEP_DURATION, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				meTheLabel.setVisible(false);
			}
		});
		disappearerTimer.setRepeats(false);
		disappearerTimer.start();
	}
	
	
}
